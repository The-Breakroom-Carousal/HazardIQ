package com.hazardiqplus.utils

import com.hazardiqplus.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WeatherUtils {
    fun getWeatherInfo(code: Int): Pair<String, Int> {
        val currentTime = SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toInt()
        return when (code) {
            0 -> "Clear sky" to if (currentTime in 5..17) R.drawable.clear_day else R.drawable.clear_night
            1, 2, 3 -> "Partly cloudy" to if (currentTime in 5..17) R.drawable.partly_cloudy_day else R.drawable.partly_cloudy_night
            45, 48 -> "Fog" to R.drawable.fog
            51, 53, 55 -> "Drizzle" to R.drawable.drizzle
            56, 57 -> "Freezing drizzle" to R.drawable.drizzle
            61, 63, 65 -> "Rain" to R.drawable.raindrops
            66, 67 -> "Freezing rain" to R.drawable.raindrops
            71, 73, 75 -> "Snow" to R.drawable.snow
            77 -> "Snow grains" to R.drawable.snowflake
            80, 81, 82 -> "Rain showers" to R.drawable.rain
            85, 86 -> "Snow showers" to R.drawable.snow
            95 -> "Thunderstorm" to if (currentTime in 5..17) R.drawable.thunderstorms_day else R.drawable.thunderstorms_night
            96, 99 -> "Thunderstorm with hail" to if (currentTime in 5..17) R.drawable.thunderstorms_day_rain else R.drawable.thunderstorms_night_rain
            else -> "Unknown" to R.drawable.baseline_cloud_queue_24
        }
    }
}