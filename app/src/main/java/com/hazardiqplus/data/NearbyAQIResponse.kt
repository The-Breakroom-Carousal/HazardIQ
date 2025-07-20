package com.hazardiqplus.data

data class NearbyAQIResponse(
    val success: Boolean,
    val data: List<NearbyAQIEntry>
)