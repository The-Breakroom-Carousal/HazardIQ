package com.hazardiqplus.data

data class OmWeatherResponse(val hourly: HourlyData)

data class OmWeatherDailyResponse(val daily: DailyData)

data class DailyData(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val weathercode: List<Int>
)

data class HourlyData(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val weathercode: List<Int>
)