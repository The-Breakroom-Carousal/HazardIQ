package com.hazardiqplus.data

data class PredictionRequest(
    val data: List<Boolean>
)

data class PredictionEventResponse(
    val event_id: String
)