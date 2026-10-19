# Gemini photo analysis

Use the existing Railway MaizeMate API with Firebase ID-token authentication. Add POST /analyze-photo accepting a compressed JPEG base64 image, forward it to Gemini using GEMINI_API_KEY on the server, and return advisory-only observations, possible causes, and recommended checks. Preserve /chat and /health. Do not embed the Gemini key in the Android application. Limit image sizes and add per-account quotas before public release.
