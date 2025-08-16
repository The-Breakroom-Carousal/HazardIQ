package com.hazardiqplus.data


data class PredictResponse(
    val success: Boolean? = null,
    val predictions: List<Prediction>? = null,
    val error: String? = null,
    val message: String? = null,
    val supportedCities: List<String> = emptyList()
){
    fun isUnsupportedLocation() = error == "unsupported_location"
}