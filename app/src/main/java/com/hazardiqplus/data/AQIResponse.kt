package com.hazardiqplus.data

import com.google.gson.annotations.SerializedName

// In your data class file for AQIResponse/HourlyAQIData

data class AQIResponse(
    val hourly: HourlyAQIData?
)

data class HourlyAQIData(
    val time: List<String>,
    val pm10: List<Double>,

    // Explicitly map the JSON key "pm2_5" to the Kotlin property "pm25"
    @SerializedName("pm2_5")
    val pm25: List<Double>
)
data class WeatherResponse(
    val current: CurrentWeather
) {
    data class CurrentWeather(
        @SerializedName("temperature_2m") val temperature: Double,
        @SerializedName("relative_humidity_2m") val humidity: Double,
        @SerializedName("weather_code") val weatherCode: Int,
        @SerializedName("wind_speed_10m") val windSpeed: Double
    )
}
