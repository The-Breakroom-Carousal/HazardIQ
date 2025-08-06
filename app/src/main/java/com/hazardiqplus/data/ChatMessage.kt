package com.hazardiqplus.data


data class ChatMessage(
    val message: String = "",
    val sender: String = "",
    val timestamp: Long = 0L
)
