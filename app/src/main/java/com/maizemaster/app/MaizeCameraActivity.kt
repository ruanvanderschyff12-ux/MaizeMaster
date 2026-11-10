package com.maizemaster.app

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.graphics.Rect
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Base64
import android.widget.*
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MaizeCameraActivity : Activity() {
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var analyzeButton: Button
    private lateinit var results: TextView
    private lateinit var farmerDescription: EditText
    private var photo: Bitmap? = null
    private val gradingPhotos = mutableListOf<Bitmap>()
    private var analyzing = false
    private var gradingMode = false
    private var pendingGradingPhoto: File? = null
    private val cameraRequest = 4101
    private val galleryRequest = 4102
    private val endpoint = "https://maizemate-api-production.up.railway.app/analyze-photo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gradingMode = intent.getBooleanExtra("gradingMode", false)
        pendingGradingPhoto = savedInstanceState?.getString("pendingGradingPhoto")?.let { File(cacheDir, it) }
        val green = MaizeUi.forest
        val padding = MaizeUi.dp(this, 16)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(MaizeUi.page)
        }
        fun heading(value: String, size: Float): TextView = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(green)
            setPadding(0, 10, 0, 16)
        }
        root.addView(if (gradingMode) {
            MaizeUi.header(this, "GRAIN QUALITY", "Photo grade estimate",
                "Spread a small sample in one layer on a plain surface. Take a sharp photo from above in good light. You can add up to three more photos of the same sample.")
        } else {
            MaizeUi.header(this, "CROP HEALTH", "AI Crop Scanner",
                "Use clear photos of maize plants, leaves, cobs or kernels for a visual assessment.")
        })
        preview = ImageView(this).apply {
            background = MaizeUi.surface(this@MaizeCameraActivity, Color.rgb(228, 241, 230))
            clipToOutline = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Selected maize photo preview"
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, (190 * resources.displayMetrics.density).toInt()))
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
        if (!gradingMode) {
            root.addView(heading("What are you seeing? (optional)", 16f))
            root.addView(farmerDescription)
            root.addView(analyzeButton)
            root.addView(Button(this).apply {
                text = "🔬 Deeper analysis (experimental)"
                setOnClickListener { analyzePhoto(detailed = true) }
            })
            root.addView(Button(this).apply {
                text = "🐛 Identify grain insect (close-up)"
                setOnClickListener { analyzePhoto(detailed = true, insect = true) }
            })
            root.addView(heading("For insect identification, photograph the insect close up, plus damaged kernels. Add its size, colour, location and any holes or grain dust in the description.", 13f))
            root.addView(Button(this).apply {
                text = "🍃 Check maize disease (experimental)"
                setOnClickListener { analyzePhoto(detailed = true, disease = true) }
            })
            root.addView(Button(this).apply {
                text = "🌽 Open separate Grain Grading page"
                setOnClickListener { startActivity(Intent(this@MaizeCameraActivity, MaizeGradingActivity::class.java)) }
            })
            root.addView(heading("For disease checks, photograph affected and healthy leaves and describe crop age and symptoms. For grain quality, photograph a representative kernel sample; a photo cannot measure moisture or establish an official grade.", 13f))
        }
        results = heading("", 16f)
        results.setTextColor(Color.rgb(25, 45, 31))
        if (!gradingMode) {
            root.addView(heading("📋 AI assessment", 19f))
            root.addView(results)
        }
        MaizeUi.primary(analyzeButton)
        root.addView(Button(this).apply {
            text = if (gradingMode) "📷 Photograph kernels" else "📷 Take maize photo"
            setOnClickListener {
                if (!canSelectPhoto()) return@setOnClickListener
                openCamera()
            }
        })
        root.addView(Button(this).apply {
            text = "🖼 Choose existing photo"
            setOnClickListener {
                if (!canSelectPhoto()) return@setOnClickListener
                startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, galleryRequest)
            }
        })
        if (gradingMode) {
            analyzeButton.text = "🌽 Estimate grade"
            root.addView(analyzeButton)
            results.textSize = 24f
            results.setTypeface(null, android.graphics.Typeface.BOLD)
            root.addView(results)
            val resetMeasurements = addOptionalMeasurements(root)
            root.addView(Button(this).apply {
                text = "Start new sample"
                setOnClickListener {
                    if (analyzing) return@setOnClickListener
                    gradingPhotos.clear()
                    resetMeasurements()
                    preview.setImageDrawable(null)
                    results.text = ""
                    status.text = "Choose a photo to begin."
                    analyzeButton.isEnabled = false
                }
            })
        }
        root.addView(Button(this).apply {
            text = "← Back"
            setOnClickListener { finish() }
        })
        val pageScroll = ScrollView(this).apply { isFillViewport = true }
        pageScroll.addView(root)
        setContentView(pageScroll)
    }

    private fun addOptionalMeasurements(root: LinearLayout): () -> Unit {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = MaizeUi.surface(this@MaizeCameraActivity)
            val padding = MaizeUi.dp(this@MaizeCameraActivity, 14)
            setPadding(padding, padding, padding, padding)
            visibility = android.view.View.GONE
        }
        panel.addView(TextView(this).apply {
            text = "Record measurements if you have them. The photo estimate uses the kernels in the image."
        })
        fun weightField(label: String) = EditText(this).apply {
            hint = label
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            panel.addView(this)
        }
        val sample = weightField("Sample weight (g)")
        val underSieve = weightField("Material below the sieve (g)")
        val summary = TextView(this).apply { textSize = 15f }
        panel.addView(Button(this).apply {
            text = "Review measurements"
            setOnClickListener {
                val sampleText = sample.text.toString().trim()
                val belowText = underSieve.text.toString().trim()
                val total = sampleText.replace(',', '.').toDoubleOrNull()
                val below = belowText.replace(',', '.').toDoubleOrNull()
                sample.error = if (sampleText.isNotEmpty() && (total == null || !total.isFinite() || total <= 0)) {
                    "Enter a sample weight above zero."
                } else null
                underSieve.error = when {
                    belowText.isNotEmpty() && (below == null || !below.isFinite() || below < 0) ->
                        "Enter a weight of zero or more."
                    total != null && below != null && below > total ->
                        "This weight exceeds the sample weight."
                    else -> null
                }
                summary.text = when {
                    sample.error != null || underSieve.error != null -> ""
                    total != null && below != null -> String.format(Locale.getDefault(), "%.2f%% of the sample passed through the sieve.", below / total * 100)
                    total != null -> String.format(Locale.getDefault(), "Sample weight · %.1f g", total)
                    below != null -> String.format(Locale.getDefault(), "Material below the sieve · %.1f g", below)
                    else -> ""
                }
            }
        })
        panel.addView(summary)
        root.addView(Button(this).apply {
            text = "Add optional measurements"
            setOnClickListener {
                val show = panel.visibility != android.view.View.VISIBLE
                panel.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
                text = if (show) "Hide optional measurements" else "Add optional measurements"
            }
        })
        root.addView(panel)
        return {
            sample.text.clear()
            underSieve.text.clear()
            sample.error = null
            underSieve.error = null
            summary.text = ""
        }
    }

    private fun openCamera() {
        try {
            val capture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (gradingMode) {
                val directory = File(cacheDir, "grain-photos").apply { mkdirs() }
                val file = File.createTempFile("sample-", ".jpg", directory)
                pendingGradingPhoto = file
                val uri = FileProvider.getUriForFile(this, "$packageName.grain-photos", file)
                capture.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                capture.clipData = ClipData.newRawUri("Maize sample", uri)
                capture.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(capture, cameraRequest)
        } catch (_: Exception) {
            pendingGradingPhoto?.delete()
            pendingGradingPhoto = null
            status.text = "Could not open the camera. Choose a photo from your gallery."
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingGradingPhoto?.let { outState.putString("pendingGradingPhoto", it.relativeTo(cacheDir).path) }
    }

    @Deprecated("Uses Android activity result callback for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val capture = if (requestCode == cameraRequest) pendingGradingPhoto else null
        if (requestCode == cameraRequest) pendingGradingPhoto = null
        try {
            if (resultCode != RESULT_OK || !canSelectPhoto()) return
            val bitmap = when (requestCode) {
                cameraRequest -> if (capture != null) {
                    decodePhoto(Uri.fromFile(capture))
                } else data?.extras?.get("data") as? Bitmap
                galleryRequest -> data?.data?.let(::decodePhoto)
                else -> null
            }
            if (bitmap == null) {
                status.text = "Could not read this photo."
                return
            }
            if (gradingMode) gradingPhotos.add(bitmap) else photo = bitmap
            preview.setImageBitmap(if (gradingMode) gradingImage() else bitmap)
            analyzeButton.isEnabled = true
            results.text = ""
            status.text = if (gradingMode) "${gradingPhotos.size} photo(s) ready. Tap Estimate grade."
                else "Photo ready. Tap Analyse photo online."
        } catch (_: Exception) {
            status.text = "Unable to display photo. Try another image."
        } finally {
            capture?.delete()
        }
    }

    private fun decodePhoto(uri: Uri): Bitmap? {
        if (android.os.Build.VERSION.SDK_INT >= 28 && gradingMode) {
            val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
            return android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = minOf(1f, 1600f / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1600) inSampleSize *= 2
        }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun canSelectPhoto(): Boolean {
        if (analyzing) return false
        if (gradingMode && gradingPhotos.size == 4) {
            status.text = "Four photos are ready. Estimate the grade or start a new sample."
            return false
        }
        return true
    }

    private fun gradingImage(): Bitmap {
        if (gradingPhotos.size == 1) return gradingPhotos.first()
        val tileSize = 700
        val rows = if (gradingPhotos.size == 2) 1 else 2
        val sheet = Bitmap.createBitmap(tileSize * 2, tileSize * rows, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        gradingPhotos.forEachIndexed { index, bitmap ->
            val available = tileSize - 16
            val scale = available.toFloat() / maxOf(bitmap.width, bitmap.height)
            val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val left = (index % 2) * tileSize + (tileSize - width) / 2
            val top = (index / 2) * tileSize + (tileSize - height) / 2
            canvas.drawBitmap(bitmap, null, Rect(left, top, left + width, top + height), paint)
        }
        return sheet
    }

    private fun analyzePhoto(detailed: Boolean = false, insect: Boolean = false, disease: Boolean = false) {
        if (analyzing) return
        val bitmap = if (gradingMode) {
            if (gradingPhotos.isEmpty()) return
            gradingImage()
        } else photo ?: return
        val wasInsectMode = insect
        val wasDiseaseMode = disease
        val wasGradingMode = gradingMode
        val description = farmerDescription.text.toString().trim().take(700)
        val insectInstructions = if (insect) "GRAIN INSECT IDENTIFICATION: Focus on visible insect morphology (snout, antennae, wings, body shape, colour, size), holes in maize kernels and powdery dust. Compare cautiously with maize weevil (Sitophilus zeamais), larger grain borer (Prostephanus truncatus), lesser grain borer (Rhyzopertha dominica), grain moths and other stored-grain insects. Give a likely common and scientific name only if visual evidence supports it; otherwise say unidentified insect, list distinguishing features and request a sharper close-up. Distinguish insects from damage alone. Provide practical non-chemical storage inspection and hygiene steps. Do not recommend unsafe insecticides or claim certainty." else ""
        val diseaseInstructions = if (disease) "MAIZE DISEASE ASSESSMENT: Describe observed lesion shape, colour, pattern and affected tissue. Compare cautiously with northern corn leaf blight, grey leaf spot, common rust, southern rust, maize streak virus, nutrient deficiency, drought or herbicide injury as appropriate. State one most likely cause only if evidence supports it, list alternatives and specific differentiating signs. Avoid definitive diagnosis; request field history and expert or laboratory confirmation where needed." else ""
        val combinedDescription = if (gradingMode) {
            """
            Estimate the visual quality grade of the maize kernels in this sample. This image contains ${gradingPhotos.size} photo(s) of the same sample, arranged left to right, then top to bottom for multiple photos. Assess the visible kernels across all filled photos together.
            Focus on kernel condition: sound kernels, visibly broken kernels, discolouration, insect holes, and mould-like damage on kernels. Ignore the tray, background, gaps between kernels, dust, loose debris, foreign matter, and material that would pass through a sieve. No sample weight, sieve measurements, moisture reading, or official certification is requested.
            For a usable photo, put exactly one label in suspectedCondition: "Estimated Grade 1" for predominantly sound kernels with very few visible defects, "Estimated Grade 2" for noticeable but moderate visible kernel defects, or "Estimated Grade 3" for widespread or severe visible kernel defects. These are visual screening tiers. Choose the closest tier even if measured grading inputs are unavailable. Use "Retake photo" only when the kernels are absent, too small, blurred, or otherwise unreadable. Do not judge food or feed safety.
            """.trimIndent()
        } else listOf(insectInstructions, diseaseInstructions, description).filter { it.isNotBlank() }.joinToString("\n").take(2400)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            status.text = "Sign in to MaizeMaster before using online analysis."
            return
        }
        analyzing = true
        analyzeButton.isEnabled = false
        status.text = if (gradingMode) "Estimating grade from the visible kernels…" else "Analysing photo online…"
        results.text = ""
        user.getIdToken(false).addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result?.token else null
            if (token.isNullOrEmpty()) {
                status.text = "Could not verify sign-in. Please sign in again."
                analyzing = false
                analyzeButton.isEnabled = true
                return@addOnCompleteListener
            }
            Thread {
                try {
                    val maxSide = if (wasGradingMode) 1600 else 1000
                    val scale = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
                    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(
                        bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1), true
                    ) else bitmap
                    val output = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, if (wasGradingMode) 85 else 75, output)
                    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 15000
                        readTimeout = 60000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                    val payload = JSONObject().put("imageBase64", encoded).put("mimeType", "image/jpeg").put("detailed", detailed || wasGradingMode).put("description", combinedDescription)
                    conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    conn.disconnect()
                    val json = JSONObject(body)
                    if (code !in 200..299) throw IllegalStateException(json.optString("error", "Service unavailable ($code)"))
                    val assessment = json.optJSONObject("assessment")
                    val formatted = if (wasGradingMode) {
                        val label = assessment?.optString("suspectedCondition").orEmpty().ifBlank { json.optString("reply") }.trim().trimStart('*', '_', '#', ' ')
                        val grade = Regex("(?i)^(?:AI[- ]?)?(?:estimated |visual )?grade ([1-3])\\b")
                            .find(label)?.groupValues?.get(1)
                        when {
                            grade != null -> "Estimated Grade $grade"
                            label.startsWith("Retake photo", ignoreCase = true) -> "Retake the photo closer to the kernels in good light."
                            else -> "No grade estimate returned. Try again with a sharp close-up of the kernels."
                        }
                    } else if (assessment != null) {
                        fun lines(key: String): String {
                            val arr = assessment.optJSONArray(key) ?: return ""
                            return (0 until arr.length()).joinToString("\n") { "• " + arr.optString(it) }
                        }
                        val suspected = assessment.optString("suspectedCondition", "").trim()
                        val candidates = suspected.split(Regex("(?i)\\s+or\\s+|\\s*;\\s*")).map { it.trim() }.filter { it.isNotEmpty() }
                        val primary = candidates.firstOrNull() ?: "Unidentified insect"
                        val others = candidates.drop(1)
                        val insectSummary = if (wasInsectMode) {
                            "🐛 Most likely insect (tentative): $primary\n\n" +
                            "Other possibilities: ${if (others.isEmpty()) "None specified" else others.joinToString("; ")}\n\n" +
                            "Confidence: ${assessment.optString("confidence", "unknown")} (AI assessment, not a measured probability)\n\n" +
                            "Visual evidence: ${assessment.optString("diagnosticEvidence", "Not provided")}\n\n" +
                            "Confirm with a sharper close-up or an expert if uncertain.\n\n"
                        } else ""
                        val focusedSummary = when {
                            wasDiseaseMode -> "🍃 Disease screening (tentative): ${assessment.optString("suspectedCondition", "Uncertain")}\nEvidence: ${assessment.optString("diagnosticEvidence", "Not provided")}\n\n"
                            else -> ""
                        }
                        insectSummary + focusedSummary + "Subject: ${assessment.optString("subject")}\n\n" +
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
                        analyzing = false
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        status.text = "Analysis unavailable: ${e.message ?: "Please try again."}"
                        analyzeButton.isEnabled = true
                        analyzing = false
                    }
                }
            }.start()
        }
    }
}
