package com.hazardiqplus.data

data class User(
     val id: Int,
     val firebase_uid: String,
     val name: String,
     val email: String,
     val role: String,
     val location_lat: Double,
     val location_lng: Double,
     val fcm_token: String,
     val created_at: String
)
