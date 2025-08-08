package com.hazardiqplus.data

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("firebase_uid")
val firebaseUid: String?,

@SerializedName("name")
val name: String?,

@SerializedName("email")
val email: String?,

@SerializedName("role")
val role: String?,

@SerializedName("location_lat")
val locationLat: Double?,

@SerializedName("location_lng")
val locationLng: Double?,

@SerializedName("fcm_token")
val fcmToken: String?
)

