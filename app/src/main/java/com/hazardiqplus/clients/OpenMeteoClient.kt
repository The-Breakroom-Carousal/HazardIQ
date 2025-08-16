package com.hazardiqplus.clients

import com.hazardiqplus.data.AQIResponse
import com.hazardiqplus.data.OmMetricsResponse
import com.hazardiqplus.data.OmWeatherDailyResponse
import com.hazardiqplus.data.OmWeatherResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object OpenMeteoClient {
    private const val AQI_BASE_URL = "https://air-quality-api.open-meteo.com/v1/"

    interface OpenMeteoAqiApi {
        @GET("air-quality")
        suspend fun getAQIHourly(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("hourly") hourly: String = "pm10,pm2_5,carbon_monoxide,ozone,nitrogen_dioxide,sulphur_dioxide",
            @Query("timezone") timezone: String = "auto"
        ): AQIResponse
    }

    val api: OpenMeteoAqiApi by lazy {
        Retrofit.Builder()
            .baseUrl(AQI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoAqiApi::class.java)
    }

    private const val WEATHER_BASE_URL = "https://api.open-meteo.com/v1/"

    interface OpenMeteoWeatherApi {
        @GET("forecast")
        suspend fun getHourlyForecast(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("hourly") hourly: String = "temperature_2m,weathercode",
            @Query("forecast_days") forecastDays: Int = 1,
            @Query("timezone") timezone: String = "auto"
        ): OmWeatherResponse

        @GET("forecast")
        suspend fun getDailyForecast(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weathercode",
            @Query("forecast_days") forecastDays: Int = 7,
            @Query("timezone") timezone: String = "auto"
        ): OmWeatherDailyResponse

        @GET("forecast")
        suspend fun getWeatherMetrics(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,visibility,uv_index",
            @Query("timezone") timezone: String = "auto"
        ): OmMetricsResponse
    }

    val weatherApi: OpenMeteoWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoWeatherApi::class.java)
    }
}