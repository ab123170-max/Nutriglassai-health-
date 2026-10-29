// NutriGlass AI — Secure Gemini Serverless Proxy with Resilient Model Fallback
// For Vercel Serverless Function deployment (Node.js 20+)

const MODELS = [
  "gemini-2.5-flash",
  "gemini-1.5-flash",
  "gemini-2.0-flash",
  "gemini-1.5-pro",
  "gemini-3.8-flash",
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
  return (
    lower.includes("model") ||
    lower.includes("unsupported") ||
    lower.includes("not found") ||
    lower.includes("not supported") ||
    lower.includes("resource has been exhausted")
  );
}

/**
 * Normalizes request payload to ensure compatibility with Gemini generateContent API:
 * - Supports both inline_data / inlineData and mime_type / mimeType
 * - Strips data URL prefixes (data:image/jpeg;base64,) if present
 * - Strips whitespace/newlines from Base64
 * - Normalizes generationConfig (e.g. responseMimeType)
 */
function normalizeRequestPayload(body) {
  if (!body || typeof body !== "object") return null;

  const rawContents = Array.isArray(body.contents) ? body.contents : [];
  let hasImage = false;
  let hasText = false;
  let imageBytesCount = 0;
  let detectedMimeType = "image/jpeg";

  const normalizedContents = rawContents.map((content) => {
    if (!content || !Array.isArray(content.parts)) return content;
    const parts = content.parts.map((part) => {
      if (!part) return part;

      if (part.text !== undefined && part.text !== null) {
        hasText = true;
        return { text: String(part.text) };
      }

      const inline = part.inline_data || part.inlineData;
      if (inline && typeof inline.data === "string") {
        hasImage = true;
        let rawData = inline.data;
        // Strip data:*;base64, prefix if client included it
        if (rawData.includes(",")) {
          rawData = rawData.substring(rawData.indexOf(",") + 1);
        }
        // Remove line breaks and whitespace
        rawData = rawData.replace(/\s+/g, "");

        const mime = inline.mime_type || inline.mimeType || "image/jpeg";
        detectedMimeType = mime;
        imageBytesCount = Math.floor((rawData.length * 3) / 4);

        return {
          inlineData: {
            mimeType: mime,
            data: rawData,
          },
        };
      }

      return part;
    });

    return { ...content, parts };
  });

  const genConfig = body.generationConfig || body.generation_config;
  let normalizedGenConfig = undefined;
  if (genConfig && typeof genConfig === "object") {
    normalizedGenConfig = { ...genConfig };
    // Google REST API expects camelCase responseMimeType
    if (normalizedGenConfig.response_mime_type && !normalizedGenConfig.responseMimeType) {
      normalizedGenConfig.responseMimeType = normalizedGenConfig.response_mime_type;
      delete normalizedGenConfig.response_mime_type;
    }
  }

  return {
    normalizedPayload: {
      contents: normalizedContents,
      ...(normalizedGenConfig ? { generationConfig: normalizedGenConfig } : {}),
    },
    hasImage,
    hasText,
    imageBytesCount,
    detectedMimeType,
  };
}

module.exports = async function handler(req, res) {
  if (req.method === "GET") {
    return sendJson(res, 200, {
      ok: true,
      service: "Nutriglass AI Gemini proxy",
      configured: Boolean(process.env.GEMINI_API_KEY),
      supportedModels: MODELS,
    });
  }

  if (req.method !== "POST") {
    res.setHeader("Allow", "GET, POST");
    return sendJson(res, 405, { error: "Method not allowed.", code: "METHOD_NOT_ALLOWED" });
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    return sendJson(res, 503, {
      error: "Server AI is not configured. Add GEMINI_API_KEY in Vercel Environment Variables.",
      code: "AI_NOT_CONFIGURED",
    });
  }

  // Parse body if received as raw string or buffer
  let body = req.body;
  if (typeof body === "string") {
    try {
      body = JSON.parse(body);
    } catch (e) {
      return sendJson(res, 400, {
        error: "Invalid JSON request body.",
        code: "INVALID_JSON_BODY",
        details: e.message,
      });
    }
  }

  if (!body || typeof body !== "object" || !Array.isArray(body.contents) || body.contents.length === 0) {
    return sendJson(res, 400, {
      error: "Invalid request: contents array is required.",
      code: "INVALID_REQUEST",
    });
  }

  const parseResult = normalizeRequestPayload(body);
  if (!parseResult) {
    return sendJson(res, 400, { error: "Could not parse request payload.", code: "INVALID_PAYLOAD" });
  }

  const { normalizedPayload, hasImage, hasText, imageBytesCount, detectedMimeType } = parseResult;
  if (!hasImage && !hasText) {
    return sendJson(res, 400, {
      error: "Request must include at least one text prompt or inline image.",
      code: "EMPTY_PAYLOAD",
    });
  }

  const serialized = JSON.stringify(normalizedPayload);
  if (serialized.length > MAX_IMAGE_BASE64_CHARS + 200_000) {
    return sendJson(res, 413, {
      error: "Image request is too large. Please scan a smaller or clearer photo.",
      code: "PAYLOAD_TOO_LARGE",
      imageBytesCount,
    });
  }

  // Allow client to request a specific model first, then fallback
  const requestedModel = req.query?.model || body.model || req.headers["x-model"];
  const modelsToTry = requestedModel
    ? [requestedModel, ...MODELS.filter((m) => m !== requestedModel)]
    : MODELS;

  let lastStatus = 502;
  let lastMessage = "All configured Gemini models are temporarily unavailable.";

  for (const model of modelsToTry) {
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
        : `Gemini returned error HTTP ${upstream.status}`;

      console.warn(`[Proxy] Model ${model} returned HTTP ${lastStatus}: ${lastMessage}`);

      if ([401, 403].includes(upstream.status)) {
        return sendJson(res, 502, {
          error: "Gemini API key was rejected by Google. Verify GEMINI_API_KEY in Vercel.",
          code: "UPSTREAM_AUTH_FAILED",
        });
      }

      if (!shouldTryNextModel(upstream.status, lastMessage)) {
        return sendJson(res, lastStatus, {
          error: lastMessage,
          code: "GEMINI_REQUEST_FAILED",
          modelTried: model,
          status: lastStatus,
        });
      }
    } catch (error) {
      lastStatus = 502;
      lastMessage = error && error.name === "AbortError"
        ? "Gemini request timed out after 40 seconds."
        : `Could not connect to Gemini: ${error?.message || "Network failure"}`;
      console.warn(`[Proxy] Error calling ${model}: ${lastMessage}`);
    } finally {
      clearTimeout(timeout);
    }
  }

  console.error("[Proxy] All Gemini fallback models failed", { status: lastStatus, message: lastMessage });
  return sendJson(res, 502, {
    error: `All Gemini models failed. Last error (${lastStatus}): ${lastMessage}`,
    code: "ALL_MODELS_FAILED",
    modelsTried: modelsToTry,
    imageMimeType: detectedMimeType,
    imageApproxBytes: imageBytesCount,
  });
};
