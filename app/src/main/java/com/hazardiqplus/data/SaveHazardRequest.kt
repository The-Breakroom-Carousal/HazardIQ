package com.hazardiqplus.data

data class SaveHazardRequest(
    val rad: Int,
    val lat: Double,
    val lng: Double,
    val hazard: String
)
