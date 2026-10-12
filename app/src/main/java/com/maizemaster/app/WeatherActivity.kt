
package com.maizemaster.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class WeatherActivity : Activity() {

    private lateinit var fieldSelector: Spinner
    private lateinit var latitudeInput: EditText
    private lateinit var longitudeInput: EditText
    private lateinit var weatherBanner: WeatherBannerView
    private lateinit var temperatureText: TextView
    private lateinit var conditionText: TextView
    private lateinit var highLowText: TextView
    private lateinit var rainText: TextView
    private lateinit var humidityText: TextView
    private lateinit var windText: TextView
    private lateinit var stageInfo: TextView
    private lateinit var result: TextView

    private lateinit var fields: List<SavedField>

    private val green = Color.rgb(24, 85, 49)
    private val dark = Color.rgb(34, 54, 39)
    private val muted = Color.rgb(105, 120, 107)
    private val pageBackground = Color.rgb(245, 248, 243)

    private val locations by lazy {
        getSharedPreferences(
            "maizemaster_locations",
            MODE_PRIVATE
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun shape(
        color: Int,
        radius: Int = 18
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun label(
        value: String,
        size: Float = 15f,
        bold: Boolean = false,
        color: Int = dark
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) {
            setTypeface(null, Typeface.BOLD)
        }
    }

    private fun card(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(16), dp(16),
                dp(16), dp(16)
            )
            background = shape(Color.WHITE)
            elevation = dp(2).toFloat()
        }

    private fun addCard(
        parent: LinearLayout,
        child: View,
        top: Int = 12
    ) {
        parent.addView(
            child,
            LinearLayout.LayoutParams(
                -1, -2
            ).apply {
                topMargin = dp(top)
            }
        )
    }

    private fun button(
        title: String,
        action: () -> Unit
    ): Button = Button(this).apply {
        text = title
        isAllCaps = false
        setOnClickListener { action() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fields = FieldStorage(this).loadFields()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(16), dp(18),
                dp(16), dp(32)
            )
            setBackgroundColor(pageBackground)
        }

        root.addView(
            label("🌽 MaizeMaster", 26f, true, green)
        )

        root.addView(
            label(
                "Smart Weather Dashboard",
                15f, false, muted
            )
        )

        val fieldCard = card()

        fieldCard.addView(
            label("YOUR MAIZE FIELD", 12f, true, muted)
        )

        fieldSelector = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@WeatherActivity,
                android.R.layout.simple_spinner_dropdown_item,
                fields.map { it.name }.ifEmpty {
                    listOf("No saved fields")
                }
            )
        }

        fieldCard.addView(fieldSelector)
        addCard(root, fieldCard, 18)

        // Illustrated maize weather dashboard
        val hero = FrameLayout(this).apply {
            background = shape(green, 20)
            clipToOutline = true
        }

        weatherBanner = WeatherBannerView(this).apply {
            growthStage = "Vegetative growth"
            condition = "Partly cloudy"
        }

        hero.addView(
            weatherBanner,
            FrameLayout.LayoutParams(-1, -1)
        )

        val overlay = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.argb(195, 11, 46, 48),
                    Color.argb(85, 11, 46, 48),
                    Color.TRANSPARENT
                )
            )
        }

        hero.addView(
            overlay,
            FrameLayout.LayoutParams(-1, -1)
        )

        val heroContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(20), dp(18),
                dp(16), dp(16)
            )
        }

        heroContent.addView(
            label(
                "CURRENT WEATHER",
                12f, true, Color.WHITE
            )
        )

        temperatureText = label(
            "--°", 58f, true, Color.WHITE
        )
        heroContent.addView(temperatureText)

        conditionText = label(
            "Load your local forecast",
            18f, true, Color.WHITE
        )
        heroContent.addView(conditionText)

        highLowText = label(
            "High --°  •  Low --°",
            14f, false, Color.WHITE
        )
        heroContent.addView(highLowText)

        hero.addView(
            heroContent,
            FrameLayout.LayoutParams(
                -1, -2, Gravity.TOP
            )
        )

        root.addView(
            hero,
            LinearLayout.LayoutParams(
                -1, dp(230)
            ).apply {
                topMargin = dp(14)
            }
        )

        // Weather metric cards
        val metrics = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun metric(
            icon: String,
            title: String
        ): Pair<LinearLayout, TextView> {
            val box = card().apply {
                gravity = Gravity.CENTER
                setPadding(
                    dp(5), dp(13),
                    dp(5), dp(13)
                )
            }

            box.addView(
                label(icon, 24f).apply {
                    gravity = Gravity.CENTER
                }
            )

            val value = label(
                "--", 18f, true, green
            ).apply {
                gravity = Gravity.CENTER
            }

            box.addView(value)

            box.addView(
                label(
                    title, 11f, false, muted
                ).apply {
                    gravity = Gravity.CENTER
                }
            )

            return Pair(box, value)
        }

        val rainMetric = metric("🌧️", "Rain chance")
        val humidityMetric = metric("💧", "Humidity")
        val windMetric = metric("💨", "Wind")

        rainText = rainMetric.second
        humidityText = humidityMetric.second
        windText = windMetric.second

        listOf(
            rainMetric.first,
            humidityMetric.first,
            windMetric.first
        ).forEachIndexed { index, view ->
            metrics.addView(
                view,
                LinearLayout.LayoutParams(
                    0, -2, 1f
                ).apply {
                    if (index > 0) {
                        leftMargin = dp(7)
                    }
                }
            )
        }

        addCard(root, metrics)

        // Maize growth information
        val growthCard = card()

        growthCard.addView(
            label(
                "🌱 Maize Growth Conditions",
                19f, true, green
            )
        )

        stageInfo = label(
            "Select a saved maize field.", 14f
        )

        growthCard.addView(stageInfo)
        addCard(root, growthCard)

        // Location controls
        val locationCard = card()

        locationCard.addView(
            label(
                "📍 Field Weather Location",
                18f, true, green
            )
        )

        latitudeInput = EditText(this).apply {
            hint = "Latitude (example: -26.85)"
            inputType = 12290
        }

        longitudeInput = EditText(this).apply {
            hint = "Longitude (example: 26.67)"
            inputType = 12290
        }

        locationCard.addView(latitudeInput)
        locationCard.addView(longitudeInput)

        locationCard.addView(
            button("📍 Use Phone GPS") {
                useGps()
            }
        )

        locationCard.addView(
            button("💾 Save Field Location") {
                saveLocation()
            }
        )

        addCard(root, locationCard)

        addCard(
            root,
            button("🌤️ Update Weather Forecast") {
                getForecast()
            }
        )

        // Seven-day farming advice
        val forecastCard = card()

        forecastCard.addView(
            label(
                "📅 7-Day Maize Weather Outlook",
                19f, true, green
            )
        )

        result = label(
            "Choose a field and update the weather " +
                "to see its forecast, benefits, " +
                "risks and recommended actions.",
            14f
        )

        forecastCard.addView(result)
        addCard(root, forecastCard)

        addCard(
            root,
            button("🔬 Maize Disease Identification") {
                DiseaseGuide.show(this)
            }
        )

        root.addView(
            label(
                "ℹ️ Forecasts are estimates. Weather " +
                    "conditions can increase crop risks, " +
                    "but cannot confirm disease, " +
                    "flooding or crop damage. " +
                    "Always inspect the field.",
                12f, false, muted
            ).apply {
                setPadding(
                    dp(4), dp(18),
                    dp(4), dp(8)
                )
            }
        )

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        fieldSelector.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val field = selectedField() ?: return

                    latitudeInput.setText(
                        locations.getString(
                            "${field.id}_lat", ""
                        )
                    )

                    longitudeInput.setText(
                        locations.getString(
                            "${field.id}_lon", ""
                        )
                    )

                    val stage = growthStage(field)
                    weatherBanner.growthStage = stage
                    weatherBanner.condition = "Partly cloudy"

                    temperatureText.text = "--°"
                    conditionText.text = "Update local weather"
                    highLowText.text =
                        "High --°  •  Low --°"

                    rainText.text = "--"
                    humidityText.text = "--"
                    windText.text = "--"

                    stageInfo.text =
                        "Estimated stage: $stage\n\n" +
                            stageOverview(stage)

                    result.text =
                        "Update the forecast for ${field.name}."
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }
    }

    private fun selectedField(): SavedField? =
        fields.getOrNull(
            fieldSelector.selectedItemPosition
        )

    private fun growthStage(field: SavedField): String {
        return if (
            field.plantingDate.isNotBlank() &&
            field.maturityDays > 0
        ) {
            MaizeGrowthCalculator.calculate(
                field.plantingDate,
                field.maturityDays
            ).stage
        } else {
            field.growthStage.ifBlank {
                "Unknown"
            }
        }
    }

    private fun stageType(stage: String): Int {
        val s = stage.lowercase(Locale.US)

        return when {
            "harvest" in s || "maturity" in s -> 6
            "dent" in s || "dough" in s ||
                "grain filling" in s -> 5
            "milk" in s || "blister" in s -> 4
            "silk" in s || "tassel" in s -> 3
            "vegetative" in s -> 2
            "seedling" in s || "emergence" in s ||
                "germination" in s -> 1
            else -> 0
        }
    }

    private fun stageOverview(stage: String): String =
        when (stageType(stage)) {
            1 -> "Warm, well-drained soil supports " +
                "emergence. Waterlogging and cold " +
                "soil may increase seed rot risk."

            2 -> "Sunlight, moisture and moderate " +
                "temperatures support leaf growth. " +
                "Watch for drought, flooding and hail."

            3 -> "Adequate moisture supports " +
                "pollination. Heat and drought can " +
                "reduce successful kernel set."

            4 -> "Healthy leaves and adequate " +
                "water support young kernels. " +
                "Severe stress can cause abortion."

            5 -> "Sunlight and moisture support " +
                "grain filling. Heat, drought and " +
                "leaf damage may reduce kernel weight."

            6 -> "Dry weather helps grain dry-down. " +
                "Wet weather may delay harvesting " +
                "and increase quality risks."

            else -> "Set a planting date and maturity " +
                "duration to estimate growth stage."
        }

    private fun coordinates(): Pair<Double, Double>? {
        val lat = latitudeInput.text.toString()
            .toDoubleOrNull()

        val lon = longitudeInput.text.toString()
            .toDoubleOrNull()

        if (
            lat == null || lon == null ||
            !lat.isFinite() || !lon.isFinite() ||
            lat !in -90.0..90.0 ||
            lon !in -180.0..180.0
        ) {
            result.text =
                "Enter valid latitude and longitude."
            return null
        }

        return Pair(lat, lon)
    }

    private fun saveLocation() {
        val field = selectedField() ?: run {
            result.text = "Save a maize field first."
            return
        }

        val position = coordinates() ?: return

        val saved = locations.edit()
            .putString(
                "${field.id}_lat",
                position.first.toString()
            )
            .putString(
                "${field.id}_lon",
                position.second.toString()
            )
            .commit()

        result.text = if (saved) {
            "Location saved for ${field.name}."
        } else {
            "Could not save field location."
        }
    }

    private fun useGps() {
        if (
            checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )
            return
        }

        try {
            val manager =
                getSystemService(LOCATION_SERVICE)
                    as LocationManager

            val position = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )
                .filter {
                    manager.isProviderEnabled(it)
                }
                .mapNotNull {
                    manager.getLastKnownLocation(it)
                }
                .maxByOrNull {
                    it.time
                }

            if (position == null) {
                result.text =
                    "GPS unavailable. Try again outdoors " +
                    "or enter coordinates manually."
                return
            }

            latitudeInput.setText(
                position.latitude.toString()
            )

            longitudeInput.setText(
                position.longitude.toString()
            )

            saveLocation()

            result.append(
                "\nCheck that this is your actual field."
            )

        } catch (e: Exception) {
            result.text =
                "GPS unavailable: ${e.message}"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100) {
            if (
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                useGps()
            } else {
                result.text =
                    "Location permission denied. " +
                    "Enter coordinates manually."
            }
        }
    }

    private fun weatherDescription(code: Int): String =
        when (code) {
            0 -> "Clear sky"
            1, 2 -> "Partly cloudy"
            3 -> "Cloudy"
            45, 48 -> "Foggy"
            in 51..67 -> "Rain"
            in 71..77 -> "Snow"
            in 80..82 -> "Rain showers"
            in 95..99 -> "Storm"
            else -> "Variable conditions"
        }

    private fun dailyAdvice(
        stage: String,
        high: Double,
        low: Double,
        rain: Double,
        wind: Double
    ): String {
        val positives = mutableListOf<String>()
        val negatives = mutableListOf<String>()
        val actions = mutableListOf<String>()

        val phase = stageType(stage)

        if (rain in 5.0..20.0) {
            positives.add(
                "Rain may replenish soil moisture " +
                    "where drainage is adequate."
            )
        }

        if (rain >= 25) {
            negatives.add(
                "Heavy rain may cause waterlogging, " +
                    "runoff and nutrient losses."
            )
            actions.add(
                "Inspect drainage and standing water."
            )
        }

        if (
            high in 20.0..30.0 &&
            low > 5 &&
            phase in 1..5
        ) {
            positives.add(
                "Moderate warmth may support " +
                    "maize growth if moisture is adequate."
            )
        }

        if (high >= 35) {
            negatives.add(
                "Extreme heat may increase water " +
                    "stress and reduce photosynthesis."
            )
            actions.add(
                "Check soil moisture and leaf stress."
            )
        }

        if (rain < 1 && high >= 30) {
            negatives.add(
                "Hot, dry weather may increase " +
                    "water demand; one day does not " +
                    "establish drought."
            )
        }

        if (low <= 2) {
            negatives.add(
                "Cold weather may indicate frost risk."
            )
            actions.add(
                "Check local frost warnings."
            )
        }

        if (wind >= 45) {
            negatives.add(
                "Strong winds may damage or lodge plants."
            )
            actions.add(
                "Inspect stalks after strong winds."
            )
        }

        when (phase) {
            1 -> {
                if (rain >= 20) {
                    negatives.add(
                        "Wet seedbeds may reduce oxygen " +
                            "and increase seed rot risk."
                    )
                }

                if (high in 20.0..30.0) {
                    positives.add(
                        "Warm conditions may help emergence."
                    )
                }

                actions.add(
                    "Check seedling emergence and uniformity."
                )
            }

            2 -> {
                if (rain in 5.0..20.0) {
                    positives.add(
                        "Moisture may support roots " +
                            "and leaf development."
                    )
                }

                if (high >= 33) {
                    negatives.add(
                        "Heat may slow vegetative growth."
                    )
                }

                actions.add(
                    "Inspect leaf health and growth."
                )
            }

            3 -> {
                if (high in 20.0..30.0 && rain < 25) {
                    positives.add(
                        "Moderate temperatures may " +
                            "support pollen shed and silking."
                    )
                }

                if (high >= 32) {
                    negatives.add(
                        "Heat during pollination may " +
                            "reduce kernel set, especially " +
                            "under drought stress."
                    )

                    actions.add(
                        "Inspect silks and soil moisture."
                    )
                }

                if (rain >= 20) {
                    negatives.add(
                        "Prolonged wet conditions near " +
                            "silking may favor some ear rots."
                    )
                }
            }

            4 -> {
                if (rain in 5.0..20.0) {
                    positives.add(
                        "Moisture may support young kernels."
                    )
                }

                if (high >= 33) {
                    negatives.add(
                        "Heat and water stress may " +
                            "increase kernel abortion risk."
                    )
                }

                actions.add(
                    "Inspect developing maize cobs."
                )
            }

            5 -> {
                if (high in 20.0..30.0 && rain < 25) {
                    positives.add(
                        "Moderate conditions may support " +
                            "grain filling."
                    )
                }

                if (high >= 33) {
                    negatives.add(
                        "Heat may reduce kernel weight."
                    )
                }

                if (rain >= 20) {
                    negatives.add(
                        "Persistent wet conditions may " +
                            "increase some ear-rot risks."
                    )
                }

                actions.add(
                    "Inspect leaves and ears."
                )
            }

            6 -> {
                if (rain < 1 && high >= 20) {
                    positives.add(
                        "Dry conditions may help " +
                            "grain dry-down and field access."
                    )
                }

                if (rain >= 10) {
                    negatives.add(
                        "Rain may delay field access " +
                            "and grain drying."
                    )
                }

                actions.add(
                    "Measure grain moisture before harvest."
                )
            }
        }

        if (positives.isEmpty()) {
            positives.add(
                "No specific benefit can be inferred " +
                    "from these values alone."
            )
        }

        if (negatives.isEmpty()) {
            negatives.add(
                "No threshold warning identified. " +
                    "Continue field scouting."
            )
        }

        if (actions.isEmpty()) {
            actions.add(
                "Check actual field conditions."
            )
        }

        return buildString {
            append("✅ POSITIVE EFFECTS\n")

            positives.distinct().forEach {
                append("• $it\n")
            }

            append("\n⚠️ NEGATIVE EFFECTS\n")

            negatives.distinct().forEach {
                append("• $it\n")
            }

            append("\n👨‍🌾 FARMER ADVICE\n")

            actions.distinct().forEach {
                append("• $it\n")
            }
        }
    }

    private fun getForecast() {
        val field = selectedField() ?: run {
            result.text = "Save a maize field first."
            return
        }

        val position = coordinates() ?: return
        val stage = growthStage(field)

        result.text =
            "Loading weather for ${field.name}..."

        Thread {
            var connection: HttpURLConnection? = null

            try {
                val address =
                    "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${position.first}" +
                    "&longitude=${position.second}" +
                    "&current=temperature_2m," +
                    "relative_humidity_2m," +
                    "weather_code,wind_speed_10m" +
                    "&daily=temperature_2m_max," +
                    "temperature_2m_min," +
                    "precipitation_sum," +
                    "precipitation_probability_max," +
                    "wind_speed_10m_max,weather_code" +
                    "&timezone=auto&forecast_days=7"

                connection =
                    URL(address).openConnection()
                        as HttpURLConnection

                connection.connectTimeout = 12000
                connection.readTimeout = 12000

                if (connection.responseCode != 200) {
                    error(
                        "Weather service unavailable: " +
                            connection.responseCode
                    )
                }

                val response = connection.inputStream
                    .bufferedReader().use {
                        it.readText()
                    }

                val root = JSONObject(response)
                val current = root.getJSONObject("current")
                val daily = root.getJSONObject("daily")

                val temp = current.getDouble(
                    "temperature_2m"
                )

                val humidity = current.getDouble(
                    "relative_humidity_2m"
                )

                val currentWind = current.getDouble(
                    "wind_speed_10m"
                )

                val code = current.getInt(
                    "weather_code"
                )

                val condition = weatherDescription(code)

                val dates = daily.getJSONArray("time")

                val highs = daily.getJSONArray(
                    "temperature_2m_max"
                )

                val lows = daily.getJSONArray(
                    "temperature_2m_min"
                )

                val rainfall = daily.getJSONArray(
                    "precipitation_sum"
                )

                val winds = daily.getJSONArray(
                    "wind_speed_10m_max"
                )

                val chances = daily.getJSONArray(
                    "precipitation_probability_max"
                )

                val highToday = highs.getDouble(0)
                val lowToday = lows.getDouble(0)

                val rainChance = if (
                    chances.isNull(0)
                ) {
                    "N/A"
                } else {
                    "${chances.getDouble(0).toInt()}%"
                }

                val report = StringBuilder()

                report.append(
                    "🌽 ${field.name}\n" +
                        "🌱 Growth stage: $stage\n\n"
                )

                for (i in 0 until dates.length()) {
                    val high = highs.getDouble(i)
                    val low = lows.getDouble(i)
                    val rain = rainfall.getDouble(i)
                    val wind = winds.getDouble(i)

                    report.append(
                        String.format(
                            Locale.US,
                            "━━━━━━━━━━━━━━\n" +
                                "📅 %s\n" +
                                "🌡️ %.0f°C to %.0f°C\n" +
                                "🌧️ Rain: %.1f mm\n" +
                                "💨 Wind: %.0f km/h\n\n",
                            dates.getString(i),
                            low,
                            high,
                            rain,
                            wind
                        )
                    )

                    report.append(
                        dailyAdvice(
                            stage,
                            high,
                            low,
                            rain,
                            wind
                        )
                    )

                    report.append("\n\n")
                }

                report.append(
                    "⚠️ Forecast-based guidance only. " +
                        "Check actual soil moisture, " +
                        "plant health and field conditions."
                )

                runOnUiThread {
                    if (selectedField()?.id != field.id) {
                        return@runOnUiThread
                    }

                    weatherBanner.growthStage = stage
                    weatherBanner.condition = condition

                    temperatureText.text =
                        String.format(
                            Locale.US,
                            "%.0f°",
                            temp
                        )

                    conditionText.text = condition

                    highLowText.text =
                        String.format(
                            Locale.US,
                            "High %.0f°  •  Low %.0f°",
                            highToday,
                            lowToday
                        )

                    rainText.text = rainChance

                    humidityText.text =
                        String.format(
                            Locale.US,
                            "%.0f%%",
                            humidity
                        )

                    windText.text =
                        String.format(
                            Locale.US,
                            "%.0f km/h",
                            currentWind
                        )

                    result.text = report.toString()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    if (selectedField()?.id == field.id) {
                        result.text =
                            "Could not load forecast. " +
                                "Check internet and try again.\n" +
                                "${e.message}"
                    }
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
