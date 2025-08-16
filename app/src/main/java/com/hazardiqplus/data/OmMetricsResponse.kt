package com.hazardiqplus.data

data class OmMetricsResponse (val current: MetricsData)

data class MetricsData (
    val temperature_2m: Double,
    val relative_humidity_2m: Double,
    val apparent_temperature: Double,
    val pressure_msl: Double,
    val surface_pressure: Double,
    val wind_speed_10m: Double,
    val wind_direction_10m: Double,
    val visibility: Double,
    val uv_index: Double
)