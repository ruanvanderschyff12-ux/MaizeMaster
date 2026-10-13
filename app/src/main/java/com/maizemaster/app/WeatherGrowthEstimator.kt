package com.maizemaster.app

import android.content.Context
import org.json.JSONArray
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Experimental temperature adjustment; rainfall is a risk indicator, not soil moisture. */
object WeatherGrowthEstimator {
    data class Estimate(val result: MaizeGrowthCalculator.GrowthResult, val details: String)

    fun calculate(context: Context, field: SavedField): Estimate {
        val baseline = MaizeGrowthCalculator.calculate(field.plantingDate, field.maturityDays)
        val cache = context.getSharedPreferences("maizemaster_weather_cache", Context.MODE_PRIVATE)
        val raw = cache.getString("${field.id}_daily_growth", null)
        val lastUpdated = cache.getLong("${field.id}_growth_updated", 0L)
        if (raw.isNullOrBlank() || lastUpdated <= 0 ||
            System.currentTimeMillis() - lastUpdated > 48L * 60 * 60 * 1000) {
            return Estimate(baseline, "Calendar-based estimate. Recent field weather is not available; open the dashboard with internet access and a saved field location.")
        }
        val planted = try { LocalDate.parse(field.plantingDate) } catch (_: Exception) { return Estimate(baseline, "Enter a valid planting date.") }
        val today = LocalDate.now()
        val observations = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        var days = 0
        var heatUnits = 0.0
        var rain7 = 0.0
        var heavyRainDays = 0
        var hotDays = 0
        for (i in 0 until observations.length()) {
            val item = observations.optJSONObject(i) ?: continue
            val date = try { LocalDate.parse(item.optString("date")) } catch (_: Exception) { continue }
            if (date.isBefore(planted) || date.isAfter(today)) continue
            val high = item.optDouble("high", Double.NaN)
            val low = item.optDouble("low", Double.NaN)
            val rain = item.optDouble("rain", Double.NaN)
            if (!high.isFinite() || !low.isFinite() || !rain.isFinite()) continue
            // Simplified maize GDD: base 10 C, capped daily maximum 30 C.
            val gdd = (((high.coerceAtMost(30.0) + low.coerceAtLeast(10.0)) / 2.0) - 10.0).coerceAtLeast(0.0)
            heatUnits += gdd
            days++
            if (!date.isBefore(today.minusDays(6))) {
                rain7 += rain
                if (rain >= 40.0) heavyRainDays++
                if (high >= 35.0) hotDays++
            }
        }
        if (days < 7) return Estimate(baseline, "Calendar-based estimate. At least seven days of observed temperature data are needed for a weather comparison.")
        // Approximate reference 12.5 GDD/day. Cap adjustment to avoid false precision.
        val adjustment = ((heatUnits / 12.5) - days).roundToInt().coerceIn(-7, 7)
        // Calculator uses a planting date, so instead shift the planting date backwards/forwards.
        val shiftedPlanting = planted.minusDays(adjustment.toLong()).toString()
        val result = MaizeGrowthCalculator.calculate(shiftedPlanting, field.maturityDays)
        val risk = when {
            heavyRainDays > 0 -> "Heavy rainfall recorded recently; inspect drainage and roots for waterlogging."
            rain7 < 5.0 -> "Little rain recorded in the last week; check actual soil moisture before irrigating."
            rain7 >= 50.0 -> "Substantial recent rainfall; inspect soil drainage and field conditions."
            else -> "Recent rainfall does not by itself confirm adequate soil moisture."
        }
        val heat = if (hotDays > 0) " High temperatures recorded; inspect for heat stress." else ""
        val direction = when {
            adjustment > 0 -> "about ${adjustment} days ahead"
            adjustment < 0 -> "about ${-adjustment} days behind"
            else -> "similar to calendar timing"
        }
        return Estimate(result, "Weather-adjusted experimental estimate: $direction (using $days observed days, max +/-7 days). $risk$heat Confirm the real stage by checking plants; this is not a field measurement.")
    }
}
