package com.hazardiqplus.data

data class PredictRequest(
    val city: String,
    val state: String,
    val lat: Double,
    val lon: Double,
    val hours: Int = 1
)