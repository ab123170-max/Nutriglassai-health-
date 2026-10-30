// Nutriglass AI — secure USDA FoodData Central proxy
// Set USDA_API_KEY in Vercel Project Settings > Environment Variables.
// The key is only used server-side and is never returned to clients.

const BASE = "https://api.nal.usda.gov/fdc/v1";
const TIMEOUT_MS = 15000;

function respond(res, status, body) {
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Cache-Control", "no-store");
  return res.status(status).json(body);
}

function nutrientMap(food) {
  const wanted = {
    208: "energyKcal",
    203: "proteinG",
    205: "carbohydratesG",
    204: "fatG",
    291: "fiberG",
    269: "sugarsG",
    307: "sodiumMg",
  };
  const result = {
    energyKcal: null, proteinG: null, carbohydratesG: null,
    fatG: null, fiberG: null, sugarsG: null, sodiumMg: null,
  };
  for (const n of (food.foodNutrients || [])) {
    const id = Number(n.nutrient?.number ?? n.nutrientNumber ?? n.number);
    const key = wanted[id];
    if (!key) continue;
    const value = Number(n.amount ?? n.value);
    if (Number.isFinite(value)) result[key] = value;
  }
  return result;
}

function normalizeFood(food) {
  return {
    fdcId: food.fdcId,
    description: food.description || "",
    dataType: food.dataType || "",
    brandOwner: food.brandOwner || null,
    ingredients: food.ingredients || null,
    servingSize: food.servingSize ?? null,
    servingSizeUnit: food.servingSizeUnit ?? null,
    nutrientsPer100g: nutrientMap(food),
    source: "USDA FoodData Central",
  };
}

module.exports = async function handler(req, res) {
  if (req.method === "GET") {
    return respond(res, 200, {
      ok: true,
      service: "Nutriglass USDA FoodData Central proxy",
      configured: Boolean(process.env.USDA_API_KEY),
      actions: ["search", "details"],
    });
  }

  if (req.method !== "POST") {
    res.setHeader("Allow", "GET, POST");
    return respond(res, 405, { ok: false, error: "Method not allowed." });
  }

  const apiKey = process.env.USDA_API_KEY;
  if (!apiKey) {
    return respond(res, 503, {
      ok: false,
      error: "USDA service is not configured. Add USDA_API_KEY to Vercel environment variables.",
      code: "USDA_NOT_CONFIGURED",
    });
  }

  const body = typeof req.body === "string" ? (() => {
    try { return JSON.parse(req.body); } catch (_) { return null; }
  })() : req.body;

  if (!body || typeof body !== "object") {
    return respond(res, 400, { ok: false, error: "A valid JSON body is required." });
  }

  const action = String(body.action || "");
  let url;

  if (action === "search") {
    const query = String(body.query || "").trim();
    if (!query || query.length > 120) {
      return respond(res, 400, { ok: false, error: "query must contain 1–120 characters." });
    }
    const pageSize = Math.min(25, Math.max(1, Number(body.pageSize) || 10));
    const params = new URLSearchParams({
      api_key: apiKey,
      query,
      pageSize: String(pageSize),
      pageNumber: String(Math.max(1, Number(body.pageNumber) || 1)),
    });
    if (Array.isArray(body.dataType) && body.dataType.length) {
      for (const type of body.dataType.slice(0, 4)) params.append("dataType", String(type));
    }
    url = `${BASE}/foods/search?${params.toString()}`;
  } else if (action === "details") {
    const fdcId = String(body.fdcId || "").trim();
    if (!/^\\d{1,12}$/.test(fdcId)) {
      return respond(res, 400, { ok: false, error: "A valid numeric fdcId is required." });
    }
    const params = new URLSearchParams({ api_key: apiKey });
    url = `${BASE}/food/${fdcId}?${params.toString()}`;
  } else {
    return respond(res, 400, { ok: false, error: "action must be 'search' or 'details'." });
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const upstream = await fetch(url, {
      method: "GET",
      headers: { Accept: "application/json" },
      signal: controller.signal,
    });
    const raw = await upstream.text();
    let data;
    try { data = JSON.parse(raw); } catch (_) { data = null; }

    if (!upstream.ok) {
      const status = upstream.status === 429 ? 429 : 502;
      return respond(res, status, {
        ok: false,
        code: upstream.status === 429 ? "USDA_RATE_LIMIT" : "USDA_UPSTREAM_ERROR",
        error: upstream.status === 429
          ? "USDA rate limit reached. Please try again shortly."
          : "USDA FoodData Central request failed.",
      });
    }

    if (action === "search") {
      const foods = Array.isArray(data?.foods) ? data.foods : [];
      return respond(res, 200, {
        ok: true,
        totalHits: Number(data?.totalHits) || 0,
        currentPage: Number(data?.currentPage) || 1,
        foods: foods.map(normalizeFood),
      });
    }

    return respond(res, 200, { ok: true, food: normalizeFood(data) });
  } catch (error) {
    return respond(res, 502, {
      ok: false,
      code: error?.name === "AbortError" ? "USDA_TIMEOUT" : "USDA_NETWORK_ERROR",
      error: error?.name === "AbortError"
        ? "USDA request timed out. Please try again."
        : "Could not connect to USDA FoodData Central.",
    });
  } finally {
    clearTimeout(timeout);
  }
};
