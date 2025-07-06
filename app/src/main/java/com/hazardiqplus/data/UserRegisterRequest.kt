package com.hazardiqplus.data

data class UserRegisterRequest(
    val idToken: String,
    val name: String,
    val email: String,
    val role: String,
    val fcm_token: String,
    val location_lat: Double?,
    val location_lng: Double?
)
