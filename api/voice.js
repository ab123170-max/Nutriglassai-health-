const MODELS = ["gemini-3.8-flash-tts", "gemini-3.8-flash-lite-tts"];
const API_URL = "https://generativelanguage.googleapis.com/v1beta/interactions";
const ALLOWED_VOICES = new Set([
  "Zephyr", "Puck", "Charon", "Kore", "Fenrir", "Leda", "Orus", "Aoede",
  "Callirrhoe", "Autonoe", "Enceladus", "Iapetus", "Umbriel", "Algieba",
  "Despina", "Erinome", "Algenib", "Rasalgethi", "Laomedeia", "Achernar",
  "Alnilam", "Schedar", "Gacrux", "Pulcherrima", "Achird", "Zubenelgenubi",
  "Vindemiatrix", "Sadachbia", "Sadaltager", "Sulafat"
]);

function sendJson(res, status, body) {
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Cache-Control", "no-store");
  return res.status(status).json(body);
}

module.exports = async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return sendJson(res, 405, { error: "Method not allowed." });
  }
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) return sendJson(res, 503, { error: "Server AI is not configured.", code: "AI_NOT_CONFIGURED" });

  const body = req.body || {};
  const text = typeof body.text === "string" ? body.text.trim() : "";
  const style = typeof body.style === "string" ? body.style.slice(0, 160) : "warm, natural audiobook narrator; clear pronunciation and expressive but restrained delivery";
  const voice = ALLOWED_VOICES.has(body.voice) ? body.voice : "Kore";
  if (!text) return sendJson(res, 400, { error: "Text is required.", code: "TEXT_REQUIRED" });
  if (text.length > 12000) return sendJson(res, 413, { error: "Passage is too long. Generate audio for a shorter passage.", code: "TEXT_TOO_LONG" });

  const input = {
    model: "",
    input: [{
      type: "user_input",
      content: [{
        type: "text",
        text: "Read the following exact text aloud as an audiobook. Do not summarize, translate, add introductions, or speak the direction. Text begins:\n" + text,
        annotations: [{ type: "speech_metadata", style }]
      }]
    }],
    response_format: { type: "audio" },
    generation_config: { speech_config: [{ voice }] }
  };

  let lastStatus = 502;
  let lastMessage = "TTS generation failed.";
  for (const model of MODELS) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 55000);
    try {
      input.model = model;
      const upstream = await fetch(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
        body: JSON.stringify(input),
        signal: controller.signal
      });
      const raw = await upstream.text();
      let parsed;
      try { parsed = JSON.parse(raw); } catch (_) { parsed = null; }
      if (upstream.ok && parsed) {
        let audio = parsed.output_audio && parsed.output_audio.data;
        if (!audio && Array.isArray(parsed.steps)) {
          for (const step of parsed.steps) {
            for (const part of (step.content || [])) {
              if (part.type === "audio" && part.data) audio = part.data;
            }
          }
        }
        if (audio) return sendJson(res, 200, { audioBase64: audio, mimeType: "audio/wav", model });
        lastMessage = "TTS response did not contain audio.";
        lastStatus = 502;
      } else {
        lastStatus = upstream.status || 502;
        lastMessage = parsed?.error?.message || "TTS provider returned an error.";
        if ([401, 403].includes(upstream.status)) {
          return sendJson(res, 502, { error: "Gemini API key was rejected. Check Vercel environment variables.", code: "UPSTREAM_AUTH_FAILED" });
        }
        if (![400, 404, 429, 500, 502, 503, 504].includes(upstream.status)) {
          return sendJson(res, 502, { error: lastMessage, code: "VOICE_REQUEST_FAILED" });
        }
      }
    } catch (error) {
      lastMessage = error && error.name === "AbortError" ? "Voice generation timed out." : "Could not connect to the voice service.";
    } finally {
      clearTimeout(timeout);
    }
  }
  console.error("Gemini voice generation failed", { status: lastStatus, message: lastMessage });
  return sendJson(res, 502, { error: "AI voice generation failed. Check model access, quota, and Vercel logs.", code: "VOICE_GENERATION_FAILED" });
};
