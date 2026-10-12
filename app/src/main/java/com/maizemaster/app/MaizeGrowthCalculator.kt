
package com.maizemaster.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object MaizeGrowthCalculator {

    data class GrowthResult(
        val stage: String,
        val daysSincePlanting: Int,
        val daysUntilMaturity: Int,
        val progressPercent: Int,
        val advice: String
    )

    private val stages = listOf(
        "Not planted",
        "Germination (VE)",
        "Seedling (V1-V3)",
        "Vegetative growth (V4-V12)",
        "Late vegetative growth (V13-VT)",
        "Tasseling (VT)",
        "Silking (R1)",
        "Blister (R2)",
        "Milk (R3)",
        "Dough (R4)",
        "Dent (R5)",
        "Physiological maturity (R6)"
    )

    fun calculate(
        plantingDate: String,
        maturityDays: Int
    ): GrowthResult {

        val totalDays = maturityDays.coerceAtLeast(1)
        val days = calculateDays(plantingDate)

        if (days < 0) {
            return GrowthResult(
                stage = "Not planted",
                daysSincePlanting = 0,
                daysUntilMaturity = totalDays,
                progressPercent = 0,
                advice = "Enter a valid planting date."
            )
        }

        val progress = (
            days.toDouble() / totalDays * 100.0
        ).toInt().coerceIn(0, 100)

        val remaining = (totalDays - days).coerceAtLeast(0)

        val stage = when {
            progress >= 100 -> stages[11]
            progress >= 90 -> stages[10]
            progress >= 80 -> stages[9]
            progress >= 70 -> stages[8]
            progress >= 62 -> stages[7]
            progress >= 55 -> stages[6]
            progress >= 50 -> stages[5]
            progress >= 38 -> stages[4]
            progress >= 16 -> stages[3]
            progress >= 5 -> stages[2]
            else -> stages[1]
        }

        val advice = when {
            stage.contains("Germination") ->
                "Check soil moisture, emergence and seedling establishment."

            stage.contains("Seedling") ->
                "Inspect early weeds, pests and plant population."

            stage.contains("Vegetative") ||
            stage.contains("Late vegetative") ->
                "Monitor water availability, nutrients, weeds and leaf health."

            stage.contains("Tasseling") ||
            stage.contains("Silking") ->
                "Avoid moisture stress during flowering and pollination."

            stage.contains("Blister") ||
            stage.contains("Milk") ->
                "Monitor kernel development and maintain healthy leaves."

            stage.contains("Dough") ||
            stage.contains("Dent") ->
                "Monitor grain filling, lodging and crop condition."

            else ->
                "Monitor grain moisture and prepare for harvesting."
        }

        return GrowthResult(
            stage = stage,
            daysSincePlanting = days,
            daysUntilMaturity = remaining,
            progressPercent = progress,
            advice = advice
        )
    }

    private fun calculateDays(dateText: String): Int {
        return try {
            val formatter = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).apply {
                isLenient = false
                timeZone = TimeZone.getDefault()
            }

            val planted = formatter.parse(dateText)
                ?: return -1

            val todayText = formatter.format(Date())
            val today = formatter.parse(todayText)
                ?: return -1

            val difference = TimeUnit.MILLISECONDS.toDays(
                today.time - planted.time
            )

            if (difference < 0) -1
            else difference.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        } catch (e: Exception) {
            -1
        }
    }
}
