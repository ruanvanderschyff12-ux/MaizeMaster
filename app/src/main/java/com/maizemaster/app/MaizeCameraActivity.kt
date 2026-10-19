package com.maizemaster.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MaizeCameraActivity : Activity() {
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var analyzeButton: Button
    private lateinit var results: TextView
    private lateinit var farmerDescription: EditText
    private var photo: Bitmap? = null
    private var detailedMode = false
    private var insectMode = false
    private val cameraRequest = 4101
    private val galleryRequest = 4102
    private val endpoint = "https://maizemate-api-production.up.railway.app/analyze-photo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val green = Color.rgb(18, 96, 51)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(245, 250, 246))
        }
        fun heading(value: String, size: Float): TextView = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(green)
            setPadding(0, 10, 0, 16)
        }
        root.addView(heading("🌽 MaizeMate Crop Scanner", 25f))
        root.addView(heading("Photograph maize plants, leaves, cobs or kernels. Analysis is advisory, not a laboratory diagnosis or official grain grade.", 14f))
        preview = ImageView(this).apply {
            setBackgroundColor(Color.rgb(225, 238, 225))
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Selected maize photo preview"
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
        status = heading("Choose a photo to begin.", 14f)
        root.addView(status)
        analyzeButton = Button(this).apply {
            text = "🌽 Analyse photo online"
            isEnabled = false
            setOnClickListener { analyzePhoto() }
        }
        farmerDescription = EditText(this).apply {
            hint = "Describe symptoms, when they started, and where you noticed them (optional)"
            minLines = 2
            maxLines = 4
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        root.addView(heading("What are you seeing? (optional)", 16f))
        root.addView(farmerDescription)
        root.addView(analyzeButton)
        root.addView(Button(this).apply {
            text = "🔬 Deeper analysis (experimental)"
            setOnClickListener { detailedMode = true; analyzePhoto() }
        })
        root.addView(Button(this).apply {
            text = "🐛 Identify grain insect (close-up)"
            setOnClickListener { insectMode = true; detailedMode = true; analyzePhoto() }
        })
        root.addView(heading("For insect identification, photograph the insect close up, plus damaged kernels. Add its size, colour, location and any holes or grain dust in the description.", 13f))
        val resultScroll = ScrollView(this)
        results = heading("", 14f)
        resultScroll.addView(results)
        root.addView(resultScroll, LinearLayout.LayoutParams(-1, 0, 0.75f))
        root.addView(Button(this).apply {
            text = "📷 Take maize photo"
            setOnClickListener {
                try { startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), cameraRequest) }
                catch (_: Exception) { status.text = "No camera app available." }
            }
        })
        root.addView(Button(this).apply {
            text = "🖼 Choose existing photo"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, galleryRequest)
            }
        })
        root.addView(Button(this).apply {
            text = "← Back to dashboard"
            setOnClickListener { finish() }
        })
        setContentView(root)
    }

    @Deprecated("Uses Android activity result callback for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        try {
            val bitmap = when (requestCode) {
                cameraRequest -> data.extras?.get("data") as? Bitmap
                galleryRequest -> data.data?.let { uri ->
                    contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream) }
                }
                else -> null
            }
            if (bitmap != null) {
                photo = bitmap
                preview.setImageBitmap(bitmap)
                analyzeButton.isEnabled = true
                results.text = ""
                status.text = "Photo ready. Tap Analyse photo online."
            } else status.text = "Could not read this photo."
        } catch (_: Exception) { status.text = "Unable to display photo. Try another image." }
    }

    private fun analyzePhoto() {
        val bitmap = photo ?: return
        val wasInsectMode = insectMode
        val description = farmerDescription.text.toString().trim().take(700)
        val insectInstructions = if (insectMode) "GRAIN INSECT IDENTIFICATION: Focus on visible insect morphology (snout, antennae, wings, body shape, colour, size), holes in maize kernels and powdery dust. Compare cautiously with maize weevil (Sitophilus zeamais), larger grain borer (Prostephanus truncatus), lesser grain borer (Rhyzopertha dominica), grain moths and other stored-grain insects. Give a likely common and scientific name only if visual evidence supports it; otherwise say unidentified insect, list distinguishing features and request a sharper close-up. Distinguish insects from damage alone. Provide practical non-chemical storage inspection and hygiene steps. Do not recommend unsafe insecticides or claim certainty." else ""
        val combinedDescription = listOf(insectInstructions, description).filter { it.isNotBlank() }.joinToString("\n").take(1900)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            status.text = "Sign in to MaizeMaster before using online analysis."
            return
        }
        analyzeButton.isEnabled = false
        status.text = "Analysing photo online…"
        results.text = ""
        user.getIdToken(false).addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result?.token else null
            if (token.isNullOrEmpty()) {
                status.text = "Could not verify sign-in. Please sign in again."
                analyzeButton.isEnabled = true
                return@addOnCompleteListener
            }
            Thread {
                try {
                    val maxSide = 1000
                    val scale = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
                    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(
                        bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1), true
                    ) else bitmap
                    val output = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, output)
                    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 15000
                        readTimeout = 60000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    val payload = JSONObject().put("imageBase64", encoded).put("mimeType", "image/jpeg").put("detailed", detailedMode).put("description", combinedDescription)
                    conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    conn.disconnect()
                    val json = JSONObject(body)
                    if (code !in 200..299) throw IllegalStateException(json.optString("error", "Service unavailable ($code)"))
                    val assessment = json.optJSONObject("assessment")
                    val formatted = if (assessment != null) {
                        fun lines(key: String): String {
                            val arr = assessment.optJSONArray(key) ?: return ""
                            return (0 until arr.length()).joinToString("\n") { "• " + arr.optString(it) }
                        }
                        val suspected = assessment.optString("suspectedCondition", "").trim()
                        val candidates = suspected.split(Regex("(?i)\\s+or\\s+|\\s*;\\s*")).map { it.trim() }.filter { it.isNotEmpty() }
                        val primary = candidates.firstOrNull() ?: "Unidentified insect"
                        val others = candidates.drop(1)
                        val insectSummary = if (wasInsectMode) {
                            "🐛 Most likely insect (tentative): $primary\\n\\n" +
                            "Other possibilities: ${if (others.isEmpty()) "None specified" else others.joinToString("; ")}\\n\\n" +
                            "Confidence: ${assessment.optString("confidence", "unknown")} (AI assessment, not a measured probability)\\n\\n" +
                            "Visual evidence: ${assessment.optString("diagnosticEvidence", "Not provided")}\\n\\n" +
                            "Confirm with a sharper close-up or an expert if uncertain.\\n\\n"
                        } else ""
                        insectSummary + "Subject: ${assessment.optString("subject")}\\n\\n" +
                        "Observations:\n${lines("observations")}\n\n" +
                        "🔎 Suspected disease / pest: ${assessment.optString("suspectedCondition", "Not identifiable from this photo")}\n\n" +
                        "Supporting evidence: ${assessment.optString("diagnosticEvidence", "Insufficient visual evidence.")}\n\n" +
                        "Possible causes:\n${lines("possibleCauses")}\n\n" +
                        "Confidence: ${assessment.optString("confidence")}\n\n" +
                        "Recommended checks:\n${lines("recommendations")}\n\n" +
                        assessment.optString("disclaimer", "Photo assessment only; seek local expert confirmation.")
                    } else json.optString("reply", "No analysis returned.")
                    runOnUiThread {
                        results.text = formatted
                        status.text = "Analysis complete."
                        analyzeButton.isEnabled = true
                        detailedMode = false
                        insectMode = false
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        status.text = "Analysis unavailable: ${e.message ?: "Please try again."}"
                        analyzeButton.isEnabled = true
                        detailedMode = false
                    }
                }
            }.start()
        }
    }
}
