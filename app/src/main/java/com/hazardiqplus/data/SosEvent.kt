package com.hazardiqplus.data

data class SosEvent(
    val id: Int,
    val firebase_uid: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val progress: String,
    val timestamp: String,
    val responder_uid: String?,
    val name: String,
    val email: String
)