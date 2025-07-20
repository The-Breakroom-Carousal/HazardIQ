package com.hazardiqplus.data

data class NearbyAQIEntry(
    val lat: Double,
    val lon: Double,
    val AQI: Double,
    val PM25: Double,
    val PM10: Double,
    val timestamp: String,

)
