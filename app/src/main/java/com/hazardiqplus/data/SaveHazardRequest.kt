package com.hazardiqplus.data

data class SaveHazardRequest(
    val rad: Double,
    val lat: Double,
    val lng: Double,
    val hazard: String
)
