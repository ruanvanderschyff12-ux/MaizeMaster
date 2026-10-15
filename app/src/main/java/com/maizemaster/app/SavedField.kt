package com.maizemaster.app

data class SavedField(
    val id: String,
    var name: String,
    var shape: String,
    var width: Double,
    var length: Double,
    var rowSpacing: Double,
    var plantSpacing: Double,
    var maizeVariety: String,
    var expectedYield: Double,
    var harvestedHectares: Double = 0.0,
    var harvestedTons: Double = 0.0,
    var plantingDate: String = "",
    var maturityDays: Int = 0,
    var growthStage: String = "",
    var growthNotes: String = "",
    var growthObservedDate: String = "",
    var rowCount: Int = 0,
    var rowsHorizontal: Boolean = false,
    var gpsHectares: Double = 0.0
) {
    fun hectares(): Double {
        if (gpsHectares > 0.0 && gpsHectares.isFinite()) return gpsHectares
        return HarvestCalculator.calculateHectares(
            width,
            length,
            shape
        )
    }

    fun estimatedTons(): Double {
        return HarvestCalculator.estimateTons(
            hectares(),
            expectedYield
        )
    }

    fun remainingTons(): Double {
        return (estimatedTons() - harvestedTons)
            .coerceAtLeast(0.0)
    }

    fun harvestProgress(): Double {
        val total = hectares()
        if (total <= 0.0) return 0.0

        return (harvestedHectares / total * 100.0)
            .coerceIn(0.0, 100.0)
    }
}