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

/**
 * Built-in maize guidance. This is intentionally not presented as a live AI model.
 * A future secure backend can supply conversational AI without embedding API keys in the APK.
 */
class MaizeAiActivity : Activity() {
    private val dark = Color.rgb(12, 78, 49)
    private val green = Color.rgb(28, 137, 57)
    private val pale = Color.rgb(242, 249, 242)
    private lateinit var conversation: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var question: EditText

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
            listOf("water", "irrigat", "drought", "dry soil").any { q.contains(it) } ->
                "💧 Water management: Check moisture in the root zone rather than relying only on surface appearance. Maize is especially sensitive to water stress around tasseling and silking. Adjust irrigation to soil type, rainfall and crop stage. Avoid waterlogging."
            listOf("fertili", "nitrogen", "yellow leaf", "yellowing", "nutrient").any { q.contains(it) } ->
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
        val prompt = question.text.toString().trim()
        if (prompt.isEmpty()) return
        question.setText("")
        addBubble(prompt, true)
        addBubble(answer(prompt), false)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pale)
            setPadding(dp(14), dp(14), dp(14), dp(12))
        }
        root.addView(text("🌽 Maize AI Assistant", 23f, true))
        root.addView(text("Built-in maize guidance • Offline mode", 13f).apply {
            setPadding(0, dp(4), 0, dp(10))
        })
        scroll = ScrollView(this).apply { isFillViewport = true }
        conversation = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(conversation)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        addBubble("Welcome to MaizeMaster! Ask a maize farming question. These are general built-in tips, not live AI or a field diagnosis.", false)
        val suggestions = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val choices = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Maize growth stages", "Yellow leaves", "Irrigation advice", "Harvest tips").forEach { topic ->
            choices.addView(Button(this).apply {
                text = topic
                isAllCaps = false
                setOnClickListener {
                    question.setText(topic)
                    submit()
                }
            })
        }
        suggestions.addView(choices)
        root.addView(suggestions)
        val entry = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        question = EditText(this).apply {
            hint = "Ask about your maize..."
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) { submit(); true } else false
            }
        }
        entry.addView(question, LinearLayout.LayoutParams(0, -2, 1f))
        entry.addView(Button(this).apply {
            text = "Send"
            isAllCaps = false
            setOnClickListener { submit() }
        })
        root.addView(entry)
        setContentView(root)
    }
}
