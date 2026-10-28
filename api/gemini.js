const MODELS = [
  "gemini-3.8-flash",
  "gemini-3.7-flash",
  "gemini-3.6-flash",
  "gemini-3.5-flash",
  "gemini-3.5-flash-lite",
];

const API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";
const MAX_IMAGE_BASE64_CHARS = 5_500_000;

function sendJson(res, status, body) {
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Cache-Control", "no-store");
  return res.status(status).json(body);
}

function shouldTryNextModel(status, message) {
  if ([404, 429, 500, 502, 503, 504].includes(status)) return true;
  const lower = String(message || "").toLowerCase();
  return status === 400 && (
    lower.includes("model") ||
    lower.includes("unsupported") ||
    lower.includes("not found") ||
    lower.includes("not supported")
  );
}

module.exports = async function handler(req, res) {
  if (req.method === "GET") {
    return sendJson(res, 200, {
      ok: true,
      service: "Nutriglass AI Gemini proxy",
      configured: Boolean(process.env.GEMINI_API_KEY),
      fallbackModels: MODELS,
    });
  }

  if (req.method !== "POST") {
    res.setHeader("Allow", "GET, POST");
    return sendJson(res, 405, { error: "Method not allowed." });
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    return sendJson(res, 503, {
      error: "Server AI is not configured. Add GEMINI_API_KEY in Vercel Environment Variables.",
      code: "AI_NOT_CONFIGURED",
    });
  }

  const body = req.body;
  if (!body || typeof body !== "object" || !Array.isArray(body.contents) || body.contents.length === 0) {
    return sendJson(res, 400, { error: "Invalid request: contents is required.", code: "INVALID_REQUEST" });
  }

  const serialized = JSON.stringify(body);
  if (serialized.length > MAX_IMAGE_BASE64_CHARS + 100_000) {
    return sendJson(res, 413, { error: "Image request is too large. Please scan a smaller or clearer photo.", code: "PAYLOAD_TOO_LARGE" });
  }

  let hasImage = false;
  let hasText = false;
  for (const content of body.contents) {
    for (const part of (content && Array.isArray(content.parts) ? content.parts : [])) {
      if (part && typeof part.text === "string" && part.text.trim()) hasText = true;
      const inline = part && (part.inline_data || part.inlineData);
      if (inline && typeof (inline.data) === "string") {
        hasImage = true;
        if (inline.data.length > MAX_IMAGE_BASE64_CHARS) {
          return sendJson(res, 413, { error: "Image is too large. Please scan a smaller photo.", code: "IMAGE_TOO_LARGE" });
        }
      }
    }
  }
  if (!hasImage && !hasText) {
    return sendJson(res, 400, { error: "Text or image content is required.", code: "CONTENT_REQUIRED" });
  }

  let lastStatus = 502;
  let lastMessage = "All configured Gemini models are temporarily unavailable.";

  for (const model of MODELS) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 40_000);
    try {
      const upstream = await fetch(`${API_BASE}/${model}:generateContent`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": apiKey,
        },
        body: serialized,
        signal: controller.signal,
      });

      const text = await upstream.text();
      let parsed;
      try {
        parsed = JSON.parse(text);
      } catch (_) {
        parsed = null;
      }

      if (upstream.ok && parsed && Array.isArray(parsed.candidates) && parsed.candidates.length > 0) {
        res.setHeader("X-Nutriglass-Model", model);
        return sendJson(res, 200, parsed);
      }

      lastStatus = upstream.status || 502;
      lastMessage = parsed && parsed.error && parsed.error.message
        ? parsed.error.message
        : "Gemini returned an invalid or empty response.";

      if ([401, 403].includes(upstream.status)) {
        return sendJson(res, 502, {
          error: "Gemini API key was rejected. Check the GEMINI_API_KEY saved in Vercel.",
          code: "UPSTREAM_AUTH_FAILED",
        });
      }

      if (!shouldTryNextModel(upstream.status, lastMessage)) {
        return sendJson(res, 502, {
          error: "Gemini could not analyze this photo. Try again with a clear, well-lit image.",
          code: "GEMINI_REQUEST_FAILED",
          modelTried: model,
        });
      }
    } catch (error) {
      lastStatus = 502;
      lastMessage = error && error.name === "AbortError"
        ? "Gemini request timed out."
        : "Could not connect to Gemini.";
    } finally {
      clearTimeout(timeout);
    }
  }

  console.error("Gemini fallback exhausted", { status: lastStatus, message: lastMessage });
  return sendJson(res, 502, {
    error: "All Gemini fallback models failed. Check Vercel logs and Google AI Studio API quota.",
    code: "ALL_MODELS_FAILED",
    modelsTried: MODELS,
  });
};
