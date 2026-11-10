package com.maizemaster.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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
    private val gradingPhotos = mutableListOf<Bitmap>()
    private var analyzing = false
    private var gradingMode = false
    private val cameraRequest = 4101
    private val galleryRequest = 4102
    private val endpoint = "https://maizemate-api-production.up.railway.app/analyze-photo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gradingMode = intent.getBooleanExtra("gradingMode", false)
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
        root.addView(heading(if (gradingMode) "🌽 MaizeMate Grain Photo Assessment" else "🌽 MaizeMate Crop Scanner", 25f))
        root.addView(heading(if (gradingMode) {
            "Mix one representative sample, divide it into four portions, and spread each portion in a single layer. Add one clear photo per portion. You will receive one visual estimate."
        } else {
            "Photograph maize plants, leaves, cobs or kernels. Analysis is advisory."
        }, 14f))
        preview = ImageView(this).apply {
            setBackgroundColor(Color.rgb(225, 238, 225))
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Selected maize photo preview"
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, (190 * resources.displayMetrics.density).toInt()))
        status = heading(if (gradingMode) "0 of 4 portions photographed." else "Choose a photo to begin.", 14f)
        root.addView(status)
        analyzeButton = Button(this).apply {
            text = "🌽 Analyse photo online"
            isEnabled = false
            setOnClickListener { analyzePhoto() }
        }
        farmerDescription = EditText(this).apply {
            hint = if (gradingMode) "Sample weight, visible defects or storage conditions (optional)"
                else "Describe symptoms, when they started, and where you noticed them (optional)"
            minLines = 2
            maxLines = 4
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        root.addView(heading("What are you seeing? (optional)", 16f))
        root.addView(farmerDescription)
        root.addView(analyzeButton)
        if (gradingMode) analyzeButton.text = "🌽 Estimate visual grade (1 / 2 / 3)"
        root.addView(Button(this).apply {
            text = "🔬 Deeper analysis (experimental)"
            setOnClickListener { analyzePhoto(detailed = true) }
        })
        if (!gradingMode) {
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
        root.addView(heading("📋 AI assessment", 19f))
        root.addView(results)
        root.addView(Button(this).apply {
            text = if (gradingMode) "📷 Photograph next portion" else "📷 Take maize photo"
            setOnClickListener {
                if (!canSelectPhoto()) return@setOnClickListener
                try { startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), cameraRequest) }
                catch (_: Exception) { status.text = "No camera app available." }
            }
        })
        root.addView(Button(this).apply {
            text = "🖼 Choose existing photo"
            setOnClickListener {
                if (!canSelectPhoto()) return@setOnClickListener
                startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, galleryRequest)
            }
        })
        if (gradingMode) root.addView(Button(this).apply {
            text = "Start new four-photo sample"
            setOnClickListener {
                if (analyzing) return@setOnClickListener
                gradingPhotos.clear()
                preview.setImageDrawable(null)
                results.text = ""
                status.text = "0 of 4 portions photographed."
                analyzeButton.isEnabled = false
            }
        })
        root.addView(Button(this).apply {
            text = "← Back"
            setOnClickListener { finish() }
        })
        val pageScroll = ScrollView(this).apply { isFillViewport = true }
        pageScroll.addView(root)
        setContentView(pageScroll)
    }

    @Deprecated("Uses Android activity result callback for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null || !canSelectPhoto()) return
        try {
            val bitmap = when (requestCode) {
                cameraRequest -> data.extras?.get("data") as? Bitmap
                galleryRequest -> data.data?.let { uri ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1600) inSampleSize *= 2
                    }
                    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                }
                else -> null
            }
            if (bitmap != null) {
                if (gradingMode) gradingPhotos.add(bitmap) else photo = bitmap
                preview.setImageBitmap(if (gradingMode) gradingSheet(gradingPhotos) else bitmap)
                analyzeButton.isEnabled = !gradingMode || gradingPhotos.size == 4
                results.text = ""
                status.text = if (gradingMode) "${gradingPhotos.size} of 4 portions photographed."
                    else "Photo ready. Tap Analyse photo online."
            } else status.text = "Could not read this photo."
        } catch (_: Exception) { status.text = "Unable to display photo. Try another image." }
    }

    private fun canSelectPhoto(): Boolean {
        if (analyzing) return false
        if (gradingMode && gradingPhotos.size == 4) {
            status.text = "All four portions are ready. Analyse them or start a new sample."
            return false
        }
        return true
    }

    private fun gradingSheet(photos: List<Bitmap>): Bitmap {
        val tileSize = 500
        val sheet = Bitmap.createBitmap(tileSize * 2, tileSize * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        photos.forEachIndexed { index, bitmap ->
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
        if (gradingMode && gradingPhotos.size != 4) {
            status.text = "Photograph all four portions before analysing."
            return
        }
        val bitmap = if (gradingMode) gradingSheet(gradingPhotos) else photo ?: return
        val wasInsectMode = insect
        val wasDiseaseMode = disease
        val wasGradingMode = gradingMode
        val description = farmerDescription.text.toString().trim().take(700)
        val insectInstructions = if (insect) "GRAIN INSECT IDENTIFICATION: Focus on visible insect morphology (snout, antennae, wings, body shape, colour, size), holes in maize kernels and powdery dust. Compare cautiously with maize weevil (Sitophilus zeamais), larger grain borer (Prostephanus truncatus), lesser grain borer (Rhyzopertha dominica), grain moths and other stored-grain insects. Give a likely common and scientific name only if visual evidence supports it; otherwise say unidentified insect, list distinguishing features and request a sharper close-up. Distinguish insects from damage alone. Provide practical non-chemical storage inspection and hygiene steps. Do not recommend unsafe insecticides or claim certainty." else ""
        val diseaseInstructions = if (disease) "MAIZE DISEASE ASSESSMENT: Describe observed lesion shape, colour, pattern and affected tissue. Compare cautiously with northern corn leaf blight, grey leaf spot, common rust, southern rust, maize streak virus, nutrient deficiency, drought or herbicide injury as appropriate. State one most likely cause only if evidence supports it, list alternatives and specific differentiating signs. Avoid definitive diagnosis; request field history and expert or laboratory confirmation where needed." else ""
        val gradingInstructions = if (gradingMode) "GRAIN PHOTO SCREENING: This image contains a 2 by 2 grid of four representative portions from one maize sample. Inspect all four quadrants and give one combined visual assessment. Begin suspectedCondition with Visual Grade 1 estimate, Visual Grade 2 estimate, Visual Grade 3 estimate, or Cannot estimate. Use Cannot estimate when the four portions are unclear or the visible evidence is insufficient. Describe broken kernels, discolouration, insect damage, mould-like growth and foreign material. Never infer numerical defect percentages, moisture, food or feed safety, or official South African grade from the photos." else ""
        val combinedDescription = listOf(insectInstructions, diseaseInstructions, gradingInstructions, description).filter { it.isNotBlank() }.joinToString("\n").take(2400)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            status.text = "Sign in to MaizeMaster before using online analysis."
            return
        }
        analyzing = true
        analyzeButton.isEnabled = false
        status.text = if (gradingMode) "Analysing all four portions…" else "Analysing photo online…"
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
                    val payload = JSONObject().put("imageBase64", encoded).put("mimeType", "image/jpeg").put("detailed", detailed).put("description", combinedDescription)
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
                            "🐛 Most likely insect (tentative): $primary\n\n" +
                            "Other possibilities: ${if (others.isEmpty()) "None specified" else others.joinToString("; ")}\n\n" +
                            "Confidence: ${assessment.optString("confidence", "unknown")} (AI assessment, not a measured probability)\n\n" +
                            "Visual evidence: ${assessment.optString("diagnosticEvidence", "Not provided")}\n\n" +
                            "Confirm with a sharper close-up or an expert if uncertain.\n\n"
                        } else ""
                        val focusedSummary = when {
                            wasDiseaseMode -> "🍃 Disease screening (tentative): ${assessment.optString("suspectedCondition", "Uncertain")}\nEvidence: ${assessment.optString("diagnosticEvidence", "Not provided")}\n\n"
                            wasGradingMode -> "🌽 COMBINED VISUAL GRADE ESTIMATE\n${assessment.optString("suspectedCondition", "Cannot estimate")}\n\nNOT AN OFFICIAL MAIZE GRADE. Moisture, measured defects, representative sampling and applicable standards are required.\n\n"
                            else -> ""
                        }
                        insectSummary + focusedSummary + "Subject: ${assessment.optString("subject")}\n\n" +
                        "Observations:\n${lines("observations")}\n\n" +
                        "🔎 ${if (wasGradingMode) "Visual assessment" else "Suspected disease / pest"}: ${assessment.optString("suspectedCondition", "Not identifiable from this photo")}\n\n" +
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
