"use strict";
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");

const openaiKey = defineSecret("OPENAI_API_KEY");

exports.analyzeMaizePhoto = onCall(
  {region: "us-central1", secrets: [openaiKey], enforceAppCheck: false, maxInstances: 10, timeoutSeconds: 60},
  async (request) => {
    if (!request.auth?.uid) throw new HttpsError("unauthenticated", "Sign in to analyze photos.");
    const image = request.data?.imageBase64;
    const type = request.data?.mimeType;
    if (typeof image !== "string" || !/^[A-Za-z0-9+/]+={0,2}$/.test(image) ||
        image.length > 5_000_000 || image.length < 100 ||
        !["image/jpeg", "image/png", "image/webp"].includes(type)) {
      throw new HttpsError("invalid-argument", "Upload a JPEG, PNG, or WebP image under 3.7 MB.");
    }
    const prompt = `You are MaizeMate, an agronomy photo-assessment assistant. Only discuss maize plants,
leaves, cobs, kernels, visible insects, grain damage and crop health. Describe visible evidence,
plausible causes, uncertainty and safe next steps. Never assert a definitive disease diagnosis
or official grain grade based on a photo. For grain grading, explain that moisture, foreign matter,
defects, sampling and applicable local standards must be measured. If photo quality is poor or
the subject is not maize, say so clearly. Do not suggest pesticide dosages or chemical mixing.
Respond as concise JSON with keys: subject, observations (array of strings),
possibleCauses (array of strings), confidence (low|medium|high),
recommendations (array of strings), disclaimer (string).`;
    let response;
    try {
      response = await fetch("https://api.openai.com/v1/chat/completions", {
        method: "POST",
        headers: {"Authorization": `Bearer ${openaiKey.value()}`, "Content-Type": "application/json"},
        body: JSON.stringify({
          model: "gpt-4o-mini",
          response_format: {type: "json_object"},
          max_tokens: 850,
          messages: [
            {role: "system", content: prompt},
            {role: "user", content: [
              {type: "text", text: "Assess this maize photo and return the specified JSON."},
              {type: "image_url", image_url: {url: `data:${type};base64,${image}`, detail: "low"}}
            ]}
          ]
        }),
        signal: AbortSignal.timeout(45000)
      });
    } catch (e) {
      throw new HttpsError("unavailable", "AI service temporarily unavailable.");
    }
    if (!response.ok) {
      console.error("AI provider HTTP status", response.status);
      throw new HttpsError("unavailable", "AI analysis could not be completed.");
    }
    const payload = await response.json();
    let result;
    try { result = JSON.parse(payload.choices[0].message.content); }
    catch (e) { throw new HttpsError("internal", "Unexpected AI response."); }
    return {assessment: result, advisoryOnly: true};
  }
);
