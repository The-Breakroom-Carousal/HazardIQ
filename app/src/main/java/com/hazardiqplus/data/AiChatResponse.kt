package com.hazardiqplus.data

// In your data package
data class AiChatResponse(
    val success: Boolean,
    val model: ModelResponse? // Use a nested data class
)
data class AiChatRequest(val msg: String)

data class ChatHistoryItemDto(
    val role: String,
    val message: String,
    val ts: Long
)
data class ModelResponse(
    val response: String?
)
