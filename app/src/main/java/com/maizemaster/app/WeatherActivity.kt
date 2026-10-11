
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

    private lateinit var fields: List<SavedField>

    private val locations by lazy {
        getSharedPreferences("maizemaster_locations", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fields = FieldStorage(this).loadFields()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 40)
            setBackgroundColor(0xFFF1F8E9.toInt())
        }

        fun heading(textValue: String): TextView =
            TextView(this).apply {
                text = textValue
                textSize = 23f
                setTextColor(0xFF1B5E20.toInt())
                setPadding(0, 15, 0, 15)
            }

        fun button(textValue: String, action: () -> Unit): Button =
            Button(this).apply {
                text = textValue
                setOnClickListener { action() }
            }

        layout.addView(heading("🌽 MaizeMaster Weather"))
        layout.addView(heading("Select Your Maize Field"))

        fieldSelector = Spinner(this)

        fieldSelector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            fields.map { it.name }.ifEmpty {
                listOf("No saved fields")
            }
        )

        layout.addView(fieldSelector)

        latitudeInput = EditText(this).apply {
            hint = "Latitude (example: -26.85)"
            inputType = 12290
        }

        longitudeInput = EditText(this).apply {
            hint = "Longitude (example: 26.67)"
            inputType = 12290
        }

        layout.addView(heading("📍 Field Location"))
        layout.addView(latitudeInput)
        layout.addView(longitudeInput)

        result = TextView(this).apply {
            text = "Select a field and set its location."
            textSize = 17f
            setPadding(10, 20, 10, 20)
        }

        layout.addView(button("📍 USE PHONE GPS") {
            useGps()
        })

        layout.addView(button("💾 SAVE FIELD LOCATION") {
            saveLocation()
        })

        layout.addView(button("🌦️ GET 7-DAY FORECAST") {
            getForecast()
        })

        layout.addView(result)

        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)

        fieldSelector.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val field = selectedField() ?: return
                    latitudeInput.setText(
                        locations.getString("${field.id}_lat", "")
                    )
                    longitudeInput.setText(
                        locations.getString("${field.id}_lon", "")
                    )
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }
    }

    private fun selectedField(): SavedField? =
        fields.getOrNull(fieldSelector.selectedItemPosition)

    private fun coordinates(): Pair<Double, Double>? {
        val lat = latitudeInput.text.toString().toDoubleOrNull()
        val lon = longitudeInput.text.toString().toDoubleOrNull()

        if (
            lat == null || lon == null ||
            !lat.isFinite() || !lon.isFinite() ||
            lat !in -90.0..90.0 ||
            lon !in -180.0..180.0
        ) {
            result.text = "Enter valid latitude and longitude."
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
            .putString("${field.id}_lat", position.first.toString())
            .putString("${field.id}_lon", position.second.toString())
            .commit()

        result.text = if (saved) {
            "Location saved for ${field.name}."
        } else {
            "Could not save field location."
        }
    }

    private fun useGps() {
        if (
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
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
                getSystemService(LOCATION_SERVICE) as LocationManager

            val position = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )
                .filter { manager.isProviderEnabled(it) }
                .mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }

            if (position == null) {
                result.text =
                    "GPS position unavailable. Try again outdoors or enter coordinates."
                return
            }

            latitudeInput.setText(position.latitude.toString())
            longitudeInput.setText(position.longitude.toString())

            saveLocation()
            result.append(
                "\nGPS uses the phone's last known position. Verify it is at your field."
            )

        } catch (e: Exception) {
            result.text = "GPS unavailable: ${e.message}"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == 100) {
            if (grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }) {
                useGps()
            } else {
                result.text =
                    "Location permission denied. You can enter coordinates manually."
            }
        }
    }

    private fun getForecast() {
        val field = selectedField() ?: run {
            result.text = "Save a maize field first."
            return
        }

        val position = coordinates() ?: return
        result.text = "Loading forecast for ${field.name}..."

        Thread {
            var connection: HttpURLConnection? = null

            try {
                val address =
                    "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${position.first}" +
                    "&longitude=${position.second}" +
                    "&daily=temperature_2m_max," +
                    "temperature_2m_min," +
                    "precipitation_sum," +
                    "wind_speed_10m_max" +
                    "&timezone=auto&forecast_days=7"

                connection =
                    URL(address).openConnection() as HttpURLConnection

                connection.connectTimeout = 12000
                connection.readTimeout = 12000

                if (connection.responseCode != 200) {
                    error("Weather service unavailable")
                }

                val response = connection.inputStream
                    .bufferedReader().use { it.readText() }

                val daily = JSONObject(response)
                    .getJSONObject("daily")

                val dates = daily.getJSONArray("time")
                val highs = daily.getJSONArray("temperature_2m_max")
                val lows = daily.getJSONArray("temperature_2m_min")
                val rainfall = daily.getJSONArray("precipitation_sum")
                val wind = daily.getJSONArray("wind_speed_10m_max")

                val report = StringBuilder(
                    "🌽 ${field.name}\n7-Day Weather Forecast\n\n"
                )

                for (i in 0 until dates.length()) {
                    val high = highs.getDouble(i)
                    val low = lows.getDouble(i)
                    val rain = rainfall.getDouble(i)
                    val windSpeed = wind.getDouble(i)

                    report.append(
                        String.format(
                            Locale.US,
                            "📅 %s\n🌡️ %.0f°C - %.0f°C\n" +
                            "🌧️ Rain: %.1f mm\n💨 Wind: %.0f km/h\n",
                            dates.getString(i),
                            low, high, rain, windSpeed
                        )
                    )

                    if (rain >= 25) {
                        report.append(
                            "⚠️ Heavy rain: inspect drainage and waterlogging risk.\n"
                        )
                    } else if (rain >= 5) {
                        report.append(
                            "✅ Rain may improve soil moisture.\n"
                        )
                    } else if (rain < 1 && high >= 30) {
                        report.append(
                            "⚠️ Hot and dry: monitor maize water stress.\n"
                        )
                    }

                    if (high >= 35) {
                        report.append(
                            "⚠️ Extreme heat may stress maize.\n"
                        )
                    }

                    if (low <= 2) {
                        report.append(
                            "⚠️ Cold weather: check local frost risk.\n"
                        )
                    }

                    if (windSpeed >= 45) {
                        report.append(
                            "⚠️ Strong winds may damage maize.\n"
                        )
                    }

                    report.append("\n")
                }

                report.append(
                    "Forecasts are estimates. Check actual field conditions."
                )

                runOnUiThread {
                    result.text = report.toString()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    result.text =
                        "Could not load weather. Check internet and try again.\n${e.message}"
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
