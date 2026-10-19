# MaizeMate online crop analysis backend

This is the secure backend setup for the online AI Crop Scanner. The Android APK must NEVER contain a model provider API key.

## Deploy prerequisites

1. Install Firebase CLI in a trusted development environment and select the existing Firebase project `maizemate-b7f97`.
2. Enable Firebase Cloud Functions and billing as required by Firebase/Google Cloud.
3. In the `functions` directory run `npm install`.
4. Set the secret: `firebase functions:secrets:set OPENAI_API_KEY --project maizemate-b7f97`.
5. Deploy: `firebase deploy --only functions:analyzeMaizePhoto --project maizemate-b7f97`.

This function requires a signed-in Firebase user, rejects oversized images, and returns a structured advisory assessment. It is not a laboratory diagnosis or official grain grade. Costs depend on model use and Cloud Functions. Set budgets, quotas, and App Check before public launch.

**Not deployed yet.** The Android scanner currently only previews photos. The next Android integration will upload a resized photo through an authenticated Firebase callable function and display the returned results.
