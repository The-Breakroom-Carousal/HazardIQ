package com.hazardiqplus.data

data class Hazard(
    val id: Int,
    val rad: Double,
    val hazard: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)