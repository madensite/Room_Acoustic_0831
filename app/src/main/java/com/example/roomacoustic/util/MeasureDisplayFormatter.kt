package com.example.roomacoustic.util

sealed interface LengthUnitDisplay {
    fun format(meters: Double): String
}

data class MetersDisplay(val cmStep: Int): LengthUnitDisplay {
    override fun format(meters: Double): String {
        val decimals = when {
            cmStep % 100 == 0 -> 0
            cmStep % 10 == 0 -> 1
            else -> 2
        }
        val rounded = kotlin.math.round(meters * 100 / cmStep) * cmStep / 100
        return "%.${decimals}f m".format(rounded)
    }
}

data class FeetInchDisplay(val inchStep: Int): LengthUnitDisplay {
    override fun format(meters: Double): String {
        val feet = meters / 0.3048
        val f = kotlin.math.floor(feet).toInt()
        val i = ((feet - f) * 12).toInt()
        return "${f}′${i}″"
    }
}