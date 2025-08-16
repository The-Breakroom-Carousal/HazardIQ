package com.hazardiqplus.data

data class SosRequest(
    val idToken: String,
    val type: String,
    val city: String,
    val lat: Double,
    val lng: Double
)
