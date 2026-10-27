# Nutriglass AI — Secure Gemini fallback backend

This repository is a native Kotlin Android app. `api/gemini.js` is a separate Vercel serverless endpoint for food-photo analysis; Vercel hosts the API, not the Android APK.

## Deploy
1. Import this GitHub repository into Vercel and set Root Directory to the repository root.
2. Use Node.js 20 or newer. The API lives at `api/gemini.js`.
3. In Vercel **Settings → Environment Variables**, add `GEMINI_API_KEY` with your private Google AI Studio API key for Production and Preview.
4. Deploy/redeploy after saving the variable.
5. Visit `https://YOUR-VERCEL-DOMAIN/api/gemini`. The health check should return JSON with `ok: true` and `configured: true`.

Never put the real Gemini key in Android, GitHub, a public `.env` file, or a `VITE_` variable. If a key was ever committed or packaged in an APK, revoke/rotate it in Google AI Studio.

## Model fallback order
The server tries `gemini-3.8-flash`, `gemini-3.7-flash`, `gemini-3.6-flash`, `gemini-3.5-flash`, then `gemini-3.5-flash-lite`. It falls back on model-unavailable, quota/rate-limit, transient server and network errors. Invalid API credentials fail immediately because trying other models will not fix the key.

## Android integration status
The existing Kotlin service still calls Google directly via `BuildConfig.GEMINI_API_KEY`. It must be changed to POST the same Gemini `contents` JSON to the deployed `/api/gemini` URL before the APK uses this backend. Keep current key config until the endpoint is deployed and client migration is tested. This is intentionally not deleted prematurely.

Before public release, add abuse protection (e.g. Firebase App Check or authenticated requests and rate limiting). Vercel environment variables keep the key server-side but do not prevent other people from calling a public endpoint.

## Local syntax check
Run `npm test` to syntax-check `api/gemini.js` with Node.js 20+.
