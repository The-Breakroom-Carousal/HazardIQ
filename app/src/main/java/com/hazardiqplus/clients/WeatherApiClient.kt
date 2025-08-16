package com.hazardiqplus.clients

import com.hazardiqplus.data.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

object WeatherApiClient {
    private const val BASE_URL = "https://api.open-meteo.com/v1/"

    interface WeatherApi {
        @GET("forecast")
        suspend fun getWeather(
            @Query("latitude") lat: Double,
            @Query("longitude") lon: Double,
            @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
        ): Response<WeatherResponse>
    }

    val api: WeatherApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}