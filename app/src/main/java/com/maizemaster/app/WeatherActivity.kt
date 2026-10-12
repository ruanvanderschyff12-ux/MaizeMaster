
package com.maizemaster.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
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
    private lateinit var result: TextView
    private lateinit var stageInfo: TextView
    private lateinit var weatherBanner: WeatherBannerView

    private lateinit var fields: List<SavedField>

    private val locations by lazy {
        getSharedPreferences(
            "maizemaster_locations",
            MODE_PRIVATE
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun text(
        value: String,
        size: Float = 16f
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(0xFF243D29.toInt())
        setPadding(dp(8), dp(10), dp(8), dp(10))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fields = FieldStorage(this).loadFields()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
            setBackgroundColor(0xFFF5F8F3.toInt())
        }

        fun button(
            title: String,
            action: () -> Unit
        ): Button = Button(this).apply {
            text = title
            isAllCaps = false
            setOnClickListener { action() }
        }

        layout.addView(
            text("🌽 MaizeMaster Weather", 24f)
        )

        layout.addView(
            text("Select Your Maize Field", 18f)
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

        layout.addView(fieldSelector)

        // Illustrated maize field weather banner
        weatherBanner = WeatherBannerView(this).apply {
            growthStage = "Vegetative growth"
            condition = "Partly cloudy"
        }

        layout.addView(
            weatherBanner,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
        )

        stageInfo = text("", 15f)
        layout.addView(stageInfo)

        layout.addView(
            text("📍 Field Location", 19f)
        )

        latitudeInput = EditText(this).apply {
            hint = "Latitude (example: -26.85)"
            inputType = 12290
        }

        longitudeInput = EditText(this).apply {
            hint = "Longitude (example: 26.67)"
            inputType = 12290
        }

        layout.addView(latitudeInput)
        layout.addView(longitudeInput)

        layout.addView(
            button("📍 USE PHONE GPS") {
                useGps()
            }
        )

        layout.addView(
            button("💾 SAVE FIELD LOCATION") {
                saveLocation()
            }
        )

        layout.addView(
            button("🌦️ GET 7-DAY WEATHER ADVICE") {
                getForecast()
            }
        )

        result = text(
            "Select a field, enter its location " +
                "and load the forecast."
        )

        layout.addView(result)

        layout.addView(
            button("🔬 MAIZE DISEASE IDENTIFICATION") {
                DiseaseGuide.show(this@WeatherActivity)
            }
        )

        layout.addView(
            text(
                "ℹ️ Weather forecasts are estimates. " +
                    "Positive and negative effects are " +
                    "possible outcomes, not confirmed " +
                    "crop conditions.",
                13f
            )
        )

        val scroll = ScrollView(this)
        scroll.addView(layout)
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
                            "${field.id}_lat",
                            ""
                        )
                    )

                    longitudeInput.setText(
                        locations.getString(
                            "${field.id}_lon",
                            ""
                        )
                    )

                    val stage = growthStage(field)

                    // Update maize illustration automatically
                    weatherBanner.growthStage = stage

                    stageInfo.text =
                        "🌱 Estimated growth stage: $stage\n\n" +
                        stageOverview(stage)

                    result.text =
                        "Load the forecast for ${field.name}."
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
            "harvest" in s ||
                "maturity" in s -> 6

            "dent" in s ||
                "dough" in s ||
                "grain filling" in s -> 5

            "milk" in s ||
                "blister" in s -> 4

            "silk" in s ||
                "tassel" in s -> 3

            "vegetative" in s -> 2

            "seedling" in s ||
                "emergence" in s ||
                "germination" in s -> 1

            else -> 0
        }
    }

    private fun stageOverview(stage: String): String =
        when (stageType(stage)) {

            1 -> "🌱 Germination / seedling\n" +
                "Positive: Warm, moist, well-drained " +
                "soil supports emergence.\n" +
                "Negative: Cold, waterlogged soil may " +
                "cause poor emergence and seed rot."

            2 -> "🌿 Vegetative growth\n" +
                "Positive: Sunlight, moisture and " +
                "moderate warmth support leaf growth.\n" +
                "Negative: Drought, flooding, hail " +
                "and frost can damage plants."

            3 -> "🌽 Tasseling / silking\n" +
                "Positive: Adequate water supports " +
                "pollination and kernel set.\n" +
                "Negative: Heat and drought can " +
                "reduce successful fertilization."

            4 -> "🌽 Early kernel development\n" +
                "Positive: Moisture and healthy " +
                "leaves support developing kernels.\n" +
                "Negative: Severe stress may cause " +
                "kernel abortion."

            5 -> "🌽 Grain filling\n" +
                "Positive: Sunlight and adequate " +
                "water support heavier kernels.\n" +
                "Negative: Heat, drought and leaf " +
                "damage may reduce kernel weight."

            6 -> "🚜 Maturity / harvest\n" +
                "Positive: Dry weather can support " +
                "grain dry-down and field access.\n" +
                "Negative: Rain, lodging and mold " +
                "can delay harvest or reduce quality."

            else -> "Set a planting date and maturity " +
                "duration for growth-stage advice."
        }

    private fun coordinates(): Pair<Double, Double>? {
        val lat = latitudeInput.text
            .toString().toDoubleOrNull()

        val lon = longitudeInput.text
            .toString().toDoubleOrNull()

        if (
            lat == null ||
            lon == null ||
            !lat.isFinite() ||
            !lon.isFinite() ||
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
            result.text = "Please save a field first."
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
                    "or enter coordinates."
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
                "\nVerify that this position " +
                    "is at your actual field."
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
                "Rainfall may replenish soil " +
                    "moisture if drainage is good."
            )
        }

        if (rain >= 25) {
            negatives.add(
                "Heavy rainfall may cause " +
                    "waterlogging, runoff and " +
                    "nutrient losses."
            )
            actions.add(
                "Inspect drainage and standing water."
            )
        }

        if (high in 20.0..30.0 && low > 5) {
            if (phase in 1..5) {
                positives.add(
                    "Moderate warmth may support " +
                        "maize growth when soil " +
                        "moisture is adequate."
                )
            }
        }

        if (high >= 35) {
            negatives.add(
                "Very hot weather can increase " +
                    "water stress and reduce " +
                    "photosynthesis."
            )
            actions.add(
                "Check soil moisture and leaf stress."
            )
        }

        if (rain < 1 && high >= 30) {
            negatives.add(
                "Hot, dry conditions can " +
                    "increase water demand. " +
                    "One dry day alone does " +
                    "not establish drought."
            )
            actions.add(
                "Inspect actual soil moisture."
            )
        }

        if (low <= 2) {
            negatives.add(
                "Cold weather may indicate " +
                    "frost risk in some locations."
            )
            actions.add(
                "Check local frost conditions."
            )
        }

        if (wind >= 45) {
            negatives.add(
                "Strong wind may cause " +
                    "lodging or broken stalks."
            )
            actions.add(
                "Inspect plants after storms."
            )
        }

        when (phase) {
            1 -> {
                if (rain >= 20) {
                    negatives.add(
                        "Saturated seedbeds may " +
                            "reduce oxygen and " +
                            "increase seed rot risk."
                    )
                }
                if (high in 20.0..30.0) {
                    positives.add(
                        "Warm conditions may help " +
                            "seedling establishment."
                    )
                }
                actions.add(
                    "Check emergence and seedling uniformity."
                )
            }

            2 -> {
                if (rain in 5.0..20.0) {
                    positives.add(
                        "Adequate moisture may " +
                            "support root and leaf development."
                    )
                }
                if (high >= 33) {
                    negatives.add(
                        "Heat stress may slow vegetative growth."
                    )
                }
                actions.add(
                    "Inspect leaf condition and plant growth."
                )
            }

            3 -> {
                if (
                    high in 20.0..30.0 &&
                    rain < 25
                ) {
                    positives.add(
                        "Moderate temperatures " +
                            "may support pollen shed and silk growth."
                    )
                }
                if (high >= 32) {
                    negatives.add(
                        "Heat around pollination " +
                            "may increase poor kernel-set risk, " +
                            "especially with drought."
                    )
                    actions.add(
                        "Check silk development and soil moisture."
                    )
                }
                if (rain >= 20) {
                    negatives.add(
                        "Prolonged wet weather " +
                            "around silking may favor certain ear rots."
                    )
                }
            }

            4 -> {
                if (rain in 5.0..20.0) {
                    positives.add(
                        "Moisture may support young kernel survival."
                    )
                }
                if (high >= 33) {
                    negatives.add(
                        "Heat and water stress " +
                            "may increase kernel abortion risk."
                    )
                }
                actions.add("Inspect developing cobs.")
            }

            5 -> {
                if (
                    high in 20.0..30.0 &&
                    rain < 25
                ) {
                    positives.add(
                        "Suitable temperatures and healthy leaves " +
                            "support grain filling."
                    )
                }
                if (high >= 33) {
                    negatives.add(
                        "Heat stress may reduce kernel weight."
                    )
                }
                if (rain >= 20) {
                    negatives.add(
                        "Persistent wet conditions " +
                            "may increase some ear-rot risks."
                    )
                }
                actions.add("Inspect leaves and ears.")
            }

            6 -> {
                if (rain < 1 && high >= 20) {
                    positives.add(
                        "Dry weather may help grain dry-down " +
                            "and harvest access."
                    )
                }
                if (rain >= 10) {
                    negatives.add(
                        "Rain may delay field access and grain drying."
                    )
                }
                actions.add(
                    "Measure grain moisture before harvesting."
                )
            }
        }

        if (positives.isEmpty()) {
            positives.add(
                "No specific benefit can be inferred " +
                    "from these daily forecast values alone."
            )
        }

        if (negatives.isEmpty()) {
            negatives.add(
                "No simple threshold warning today. " +
                    "Continue scouting."
            )
        }

        if (actions.isEmpty()) {
            actions.add("Check actual field conditions.")
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
            "Loading weather advice for ${field.name}..."

        Thread {
            var connection: HttpURLConnection? = null

            try {
                val address =
                    "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${position.first}" +
                    "&longitude=${position.second}" +
                    "&current=temperature_2m,weather_code" +
                    "&daily=temperature_2m_max," +
                    "temperature_2m_min," +
                    "precipitation_sum," +
                    "wind_speed_10m_max," +
                    "weather_code" +
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
                val daily = root.getJSONObject("daily")

                val dates =
                    daily.getJSONArray("time")
                val highs =
                    daily.getJSONArray("temperature_2m_max")
                val lows =
                    daily.getJSONArray("temperature_2m_min")
                val rainfall =
                    daily.getJSONArray("precipitation_sum")
                val wind =
                    daily.getJSONArray("wind_speed_10m_max")

                val current = root.getJSONObject("current")
                val currentTemp =
                    current.getDouble("temperature_2m")
                val weatherCode =
                    current.getInt("weather_code")

                val weatherCondition = when (weatherCode) {
                    0 -> "Clear sky"
                    1, 2 -> "Partly cloudy"
                    3 -> "Cloudy"
                    45, 48 -> "Foggy"
                    in 51..67 -> "Rain"
                    in 71..77 -> "Snow"
                    in 80..82 -> "Rain showers"
                    in 95..99 -> "Storm"
                    else -> "Partly cloudy"
                }

                val report = StringBuilder()

                report.append(
                    "🌽 ${field.name}\n" +
                        "🌱 Growth stage: $stage\n" +
                        "🌡️ Current temperature: " +
                        String.format(
                            Locale.US,
                            "%.0f°C",
                            currentTemp
                        ) +
                        "\n🌤️ $weatherCondition\n\n"
                )

                for (i in 0 until dates.length()) {
                    val high = highs.getDouble(i)
                    val low = lows.getDouble(i)
                    val rain = rainfall.getDouble(i)
                    val windSpeed = wind.getDouble(i)

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
                            windSpeed
                        )
                    )

                    report.append(
                        dailyAdvice(
                            stage,
                            high,
                            low,
                            rain,
                            windSpeed
                        )
                    )

                    report.append("\n")
                }

                report.append(
                    "⚠️ These are preliminary " +
                        "weather-based guidelines. " +
                        "Rainfall does not prove soil " +
                        "moisture or waterlogging. " +
                        "Forecasts cannot confirm " +
                        "disease, frost, hail or crop " +
                        "damage. Check the field."
                )

                runOnUiThread {
                    if (selectedField()?.id != field.id) {
                        return@runOnUiThread
                    }

                    weatherBanner.growthStage = stage
                    weatherBanner.condition = weatherCondition

                    result.text = report.toString()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    result.text =
                        "Could not load forecast. " +
                        "Check internet and try again.\n" +
                        "${e.message}"
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
