package com.hazardiqplus.data

data class NearbyAQIData(
    val city: String,
    val state: String,
    val latitude: Double,
    val longitude: Double,
    val pm25_prediction: Double,
    val pm10_prediction: Double,
    val timestamp: String
)
