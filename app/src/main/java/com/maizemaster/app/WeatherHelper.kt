
package com.maizemaster.app

object WeatherHelper {

    fun getWeatherAdvice(
        temperature: Double,
        rainfall: Double
    ): String {

        val advice = mutableListOf<String>()

        if (temperature >= 35) {
            advice.add("High heat: Maize may suffer heat stress.")
        } else if (temperature < 10) {
            advice.add("Cold weather: Maize growth may slow down.")
        } else {
            advice.add("Temperature is generally suitable for maize growth.")
        }

        if (rainfall >= 50) {
            advice.add("Heavy rain: Risk of waterlogging and root damage.")
        } else if (rainfall >= 10) {
            advice.add("Rainfall may help replenish soil moisture.")
        } else if (rainfall == 0.0) {
            advice.add("No rain: Monitor soil moisture and irrigation needs.")
        }

        return advice.joinToString("\n")
    }
}
