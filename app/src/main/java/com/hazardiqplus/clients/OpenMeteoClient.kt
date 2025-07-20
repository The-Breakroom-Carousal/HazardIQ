package com.hazardiqplus.clients

import com.hazardiqplus.data.AQIResponse
import retrofit2.http.GET
import retrofit2.http.Query

object AirQualityApiClient {
    private const val BASE_URL = "https://air-quality-api.open-meteo.com/v1/"

    interface AirQualityApi {
        @GET("air-quality")
        suspend fun getAQIHourly(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("hourly") hourly: String = "pm10,pm2_5",
            @Query("timezone") timezone: String = "auto"
        ): AQIResponse
    }

    val api: AirQualityApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(AirQualityApi::class.java)
    }
}