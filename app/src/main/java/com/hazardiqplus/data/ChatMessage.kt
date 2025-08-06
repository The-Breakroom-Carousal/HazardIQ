package com.hazardiqplus.data


data class ChatMessage(
    val message: String = "",
    val senderName: String,
    val sender: String = "",
    val timestamp: Long = 0L,
    var isMine: Boolean = false
)
