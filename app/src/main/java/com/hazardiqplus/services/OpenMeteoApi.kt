package com.hazardiqplus.services

import com.hazardiqplus.data.AQIResponse
import com.hazardiqplus.data.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("air-quality")
    suspend fun getCurrentAQI(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "pm10,pm2_5"
    ): Response<AQIResponse>

    @GET("forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
    ): Response<WeatherResponse>
}