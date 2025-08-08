package com.hazardiqplus.data

import com.google.gson.annotations.SerializedName

data class FcmTokenUpdateRequest(
    @SerializedName("fcm_token")
    val fcmToken: String?
)

