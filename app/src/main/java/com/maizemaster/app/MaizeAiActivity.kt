package com.maizemaster.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Built-in maize guidance. This is intentionally not presented as a live AI model.
 * A future secure backend can supply conversational AI without embedding API keys in the APK.
 */
class MaizeAiActivity : Activity() {
    private val dark = Color.rgb(12, 78, 49)
    private val green = Color.rgb(28, 137, 57)
    private val pale = Color.rgb(244, 248, 245)
    private val gold = Color.rgb(245, 189, 59)
    private lateinit var conversation: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var question: EditText
    private lateinit var connectionStatus: TextView
    private var waiting = false
    private val endpoint = "https://maizemate-api-production.up.railway.app/chat"

    private fun dp(n: Int) = (n * resources.displayMetrics.density + 0.5f).toInt()
    private fun background(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(15).toFloat()
    }
    private fun text(s: String, size: Float = 15f, bold: Boolean = false) = TextView(this).apply {
        this.text = s
        textSize = size
        setTextColor(dark)
        if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun addBubble(message: String, farmer: Boolean) {
        val bubble = text(message, 15f).apply {
            setTextColor(if (farmer) Color.WHITE else dark)
            background = background(if (farmer) green else Color.WHITE)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (farmer) Gravity.END else Gravity.START
        }
        row.addView(bubble, LinearLayout.LayoutParams(0, -2, 0.88f))
        conversation.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(12)
        })
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }
    private fun answer(raw: String): String {
        val q = raw.lowercase(Locale.ROOT)
        return when {
            listOf("hello", "hi", "hey").any { q.trim() == it } ->
                "Hello! 🌽 Ask about planting, watering, fertiliser, maize growth, pests or harvesting."
            listOf("weevil", "grain insect", "grain pest", "grain borer", "stored insect", "sitophilus", "prostephanus").any { q.contains(it) } ->
                "🐛 Stored-maize insects: Maize weevils and larger grain borers can tunnel into kernels and create powdery residues. Inspect several grain samples for live insects, holes and dust. Keep stores clean and dry, monitor grain temperature and moisture, and use only locally registered grain-storage treatments as directed. Never apply household insecticides to food grain."
            listOf("kernel", "kernels", "grain quality", "broken grain", "discoloured grain").any { q.contains(it) } ->
                "🌽 Kernel quality: Check a representative sample for broken kernels, insect holes, discolouration, mould and foreign material. Measure grain moisture with a calibrated meter before storage. Separate visibly damaged grain and investigate the cause; appearance alone cannot establish food or feed safety."
            listOf("mould", "mold", "mycotoxin", "aflatoxin", "fumonisin", "ear rot").any { q.contains(it) } ->
                "⚠️ Mould and mycotoxins: Maize can contain harmful toxins even when kernels look normal. Do not assume affected grain is safe after drying or removing visibly mouldy kernels. Segregate suspect lots, prevent further moisture exposure and arrange representative laboratory mycotoxin testing before food or feed use."
            listOf("stalk rot", "grey leaf spot", "gray leaf spot", "northern leaf blight", "maize rust").any { q.contains(it) } ->
                "🛡️ Maize disease: Note the affected plant parts, lesion shapes, growth stage and recent weather. Several maize diseases can resemble nutrient or weather damage. Photograph symptoms, inspect multiple plants and seek confirmation from a local crop adviser before selecting treatment."
            listOf("water", "irrigat", "drought", "dry soil").any { q.contains(it) } ->
                "💧 Water management: Check moisture in the root zone rather than relying only on surface appearance. Maize is especially sensitive to water stress around tasseling and silking. Adjust irrigation to soil type, rainfall and crop stage. Avoid waterlogging."
            listOf("fertili", "nitrogen", "yellow leaf", "yellow leaves", "yellowing", "nutrient", "leaves are turning yellow").any { q.contains(it) } ->
                "🌱 Nutrition: Yellowing can result from nitrogen shortage, water stress, disease or other causes. Check which leaves are affected, soil moisture and recent weather. Use a soil or leaf test before choosing fertiliser rates. Avoid applying products based on leaf colour alone."
            listOf("pest", "worm", "insect", "fall armyworm", "caterpillar").any { q.contains(it) } ->
                "🐛 Pest scouting: Inspect several places in the field, including the whorl and undersides of leaves. Note the pest, damage level and crop stage. Identify the pest before treatment and follow locally registered product labels and local extension guidance."
            listOf("disease", "spots", "rust", "blight", "fungus").any { q.contains(it) } ->
                "🛡️ Disease checks: Record which leaves show symptoms, how rapidly damage is spreading and whether recent weather was humid. Different diseases and nutrient problems can look similar. Confirm the diagnosis before using a fungicide; consult a local agronomist for severe outbreaks."
            listOf("plant", "seed", "spacing", "emergence", "germinat").any { q.contains(it) } ->
                "🌽 Planting: Choose an adapted maize variety, plant into suitable soil moisture and check seed depth and spacing for your equipment and local conditions. Count emerged plants in several rows to identify uneven establishment."
            listOf("growth", "stage", "tassel", "silk", "maturity").any { q.contains(it) } ->
                "📈 Growth stages: Maize progresses through emergence (VE), vegetative leaf stages (V), tasseling (VT), silking (R1), grain fill (R2–R5) and maturity (R6). Calendar estimates are approximate; inspect plants to confirm their actual stage."
            listOf("harvest", "yield", "ton", "moisture", "storage").any { q.contains(it) } ->
                "🚜 Harvesting: Check grain maturity and moisture, inspect losses behind the harvester and keep load weights by field. Dry and store grain under suitable conditions to reduce spoilage. MaizeMaster's Harvest Tracker can record progress."
            listOf("weather", "rain", "heat", "frost").any { q.contains(it) } ->
                "☀️ Weather: Use the Weather section for current forecasts. Pay particular attention to heat and moisture stress during flowering, strong winds and waterlogged soil. Forecasts and field conditions can differ."
            else ->
                "🌽 I can offer built-in guidance on planting, fertiliser, irrigation, growth stages, pests, diseases and harvesting. Try asking a question with one of those topics. This version does not yet connect to a live AI model."
        }
    }
    private fun submit() {
        if (waiting) return
        val prompt = question.text.toString().trim()
        if (prompt.isEmpty()) return
        question.setText("")
        addBubble(prompt, true)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            connectionStatus.text = "● Offline guidance — sign in for online AI"
            addBubble(answer(prompt), false)
            return
        }
        waiting = true
        connectionStatus.text = "● Connecting to online AI…"
        user.getIdToken(false).addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result?.token else null
            if (token.isNullOrBlank()) {
                showFallback(prompt, "Could not verify sign-in")
                return@addOnCompleteListener
            }
            Thread {
                var conn: HttpURLConnection? = null
                try {
                    conn = URL(endpoint).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 12000
                    conn.readTimeout = 30000
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.outputStream.use { it.write(JSONObject().put("message", prompt).toString().toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val result = JSONObject(body)
                    if (code !in 200..299) throw IllegalStateException(
                        when (code) {
                            401 -> "Session expired — sign in again"
                            429 -> "AI is busy — try again later"
                            else -> result.optString("error", "Online AI unavailable")
                        }
                    )
                    val reply = result.optString("reply").trim()
                    if (reply.isEmpty()) throw IllegalStateException("No AI answer received")
                    runOnUiThread {
                        waiting = false
                        connectionStatus.text = "● Online AI connected"
                        addBubble(reply, false)
                    }
                } catch (e: Exception) {
                    runOnUiThread { showFallback(prompt, e.message ?: "Connection failed") }
                } finally {
                    conn?.disconnect()
                }
            }.start()
        }
    }
    private fun showFallback(prompt: String, reason: String) {
        waiting = false
        connectionStatus.text = "● Offline guidance — online AI unavailable"
        addBubble("Online AI unavailable ($reason). Here's offline guidance:\\n\\n" + answer(prompt), false)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pale)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = background(dark)
            setPadding(dp(10), dp(10), dp(12), dp(10))
        }
        header.addView(MaizeMateMascot(this), LinearLayout.LayoutParams(dp(80), dp(80)))
        val branding = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        branding.addView(text("MaizeMate", 26f, true).apply { setTextColor(Color.WHITE) })
        branding.addView(text("YOUR MAIZE SPECIALIST", 11f, true).apply {
            setTextColor(gold)
            letterSpacing = 0.08f
        })
        connectionStatus = text("● Ready to connect to online AI", 12f).apply {
            setTextColor(Color.rgb(202, 237, 210))
            setPadding(0, dp(6), 0, 0)
        }
        branding.addView(connectionStatus)
        header.addView(branding, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        val section = text("MAIZE • KERNELS • DISEASES • GRAIN INSECTS", 10f, true).apply {
            setTextColor(green)
            letterSpacing = 0.04f
            gravity = Gravity.CENTER
            setPadding(0, dp(3), 0, dp(10))
        }
        root.addView(section)

        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        conversation = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), dp(6), dp(3), dp(6))
        }
        scroll.addView(conversation)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        addBubble("Hi! I'm MaizeMate 🌽🤖\\n\\nAsk me about maize plants, kernels, crop diseases or grain insects. I use online AI when available and built-in guidance when offline.", false)

        val suggestions = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(6), 0, dp(7))
        }
        val choices = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("🌽 Maize growth", "🍃 Yellow leaves", "🛡️ Maize diseases", "🐛 Grain insects", "🌾 Kernel quality").forEach { topic ->
            choices.addView(TextView(this).apply {
                text = topic
                textSize = 13f
                setTextColor(dark)
                background = background(Color.WHITE)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setOnClickListener {
                    question.setText(topic)
                    submit()
                }
            }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
        }
        suggestions.addView(choices)
        root.addView(suggestions)

        val entry = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = background(Color.WHITE)
            setPadding(dp(9), dp(5), dp(7), dp(5))
            elevation = dp(3).toFloat()
        }
        question = EditText(this).apply {
            hint = "Ask MaizeMate..."
            textSize = 15f
            setTextColor(dark)
            setHintTextColor(Color.rgb(100, 117, 107))
            setBackgroundColor(Color.TRANSPARENT)
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) { submit(); true } else false
            }
        }
        entry.addView(question, LinearLayout.LayoutParams(0, dp(48), 1f))
        entry.addView(TextView(this).apply {
            text = "➤"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = background(green)
            contentDescription = "Send message"
            setOnClickListener { submit() }
        }, LinearLayout.LayoutParams(dp(49), dp(46)))
        root.addView(entry)

        // Keep the composer above Samsung/Android keyboards, including edge-to-edge layouts.
        val baseBottom = dp(12)
        root.setOnApplyWindowInsetsListener { _, insets ->
            val keyboard = insets.getInsets(android.view.WindowInsets.Type.ime()).bottom
            val navigation = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
            val extra = (keyboard - navigation).coerceAtLeast(0)
            root.setPadding(dp(14), dp(12), dp(14), baseBottom + extra)
            insets
        }
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(root)
    }
}
