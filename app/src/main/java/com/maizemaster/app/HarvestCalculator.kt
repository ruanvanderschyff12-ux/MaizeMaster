package com.maizemaster.app

import kotlin.math.PI

object HarvestCalculator {

    fun calculateHectares(
        width: Double,
        length: Double,
        shape: String
    ): Double {
        val area = when (shape) {
            "Circle" -> PI * width * width / 4.0
            "Oval" -> PI * width * length / 4.0
            else -> width * length
        }
        return area / 10000.0
    }

    fun estimateTons(
        hectares: Double,
        yieldPerHectare: Double
    ): Double {
        return hectares * yieldPerHectare
    }

    fun estimatePlants(
        hectares: Double,
        rowSpacing: Double,
        plantSpacing: Double
    ): Long {
        if (hectares <= 0.0 ||
            rowSpacing <= 0.0 ||
            plantSpacing <= 0.0
        ) return 0L

        val squareMeters = hectares * 10000.0

        return (squareMeters /
            (rowSpacing * plantSpacing)).toLong()
    }
}
