package com.hazardiqplus.data

data class Prediction(
    val hourAhead: Int,
    val PM25: Double,
    val PM10: Double,
    val AQI: Int
)
