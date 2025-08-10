package com.hazardiqplus.ui.citizen.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hazardiqplus.R
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.hazardiqplus.adapters.Weather24hAdapter
import com.hazardiqplus.adapters.Weather7dAdapter
import com.hazardiqplus.clients.OpenMeteoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CitizenWeatherFragment : Fragment(R.layout.fragment_citizen_weather) {

    private lateinit var tvWindSpeed: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvVisibility: TextView
    private lateinit var tvUvIndex: TextView
    private lateinit var tvFeelsLike: TextView
    private lateinit var tvPressure: TextView
    private lateinit var hv24h: RecyclerView
    private lateinit var progressCircular24h: CircularProgressIndicator
    private lateinit var rv7d: RecyclerView
    private lateinit var progressCircular7d: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_citizen_weather, container, false)
        tvWindSpeed = view.findViewById(R.id.tvWindSpeed)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvVisibility = view.findViewById(R.id.tvVisibility)
        tvUvIndex = view.findViewById(R.id.tvUvIndex)
        tvFeelsLike = view.findViewById(R.id.tvFeelsLike)
        tvPressure = view.findViewById(R.id.tvPressure)
        hv24h = view.findViewById(R.id.hv24hWeatherForecast)
        progressCircular24h = view.findViewById(R.id.progressCircular24h)
        progressCircular7d = view.findViewById(R.id.progressCircular7d)
        rv7d = view.findViewById(R.id.hv7dWeatherForecast)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressCircular24h.visibility = View.VISIBLE
        progressCircular7d.visibility = View.VISIBLE
        hv24h.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv7d.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            try {
                val metrics = OpenMeteoClient.weatherApi.getWeatherMetrics(23.7957, 86.4304)
                Log.d("Metrics", metrics.toString())
                withContext(Dispatchers.Main) {
                    tvWindSpeed.text = "${metrics.current.wind_speed_10m} km/h"
                    tvHumidity.text = "${metrics.current.relative_humidity_2m}%"
                    tvVisibility.text = "${metrics.current.visibility} km"
                    tvUvIndex.text = "${metrics.current.uv_index}"
                    tvFeelsLike.text = "${metrics.current.apparent_temperature}°C"
                    tvPressure.text = "${metrics.current.pressure_msl} hPa"
                }

                val hourly = OpenMeteoClient.weatherApi.getHourlyForecast(
                    latitude = 23.7957, // replace with dynamic location
                    longitude = 86.4304,
                    hourly = "temperature_2m,weathercode",
                    forecastDays = 1
                )
                val daily = OpenMeteoClient.weatherApi.getDailyForecast(
                    latitude = 23.7957,
                    longitude = 86.4304,
                    daily = "temperature_2m_max,temperature_2m_min,weathercode",
                    forecastDays = 7
                )
                hv24h.adapter = Weather24hAdapter(
                    hourly.hourly.time,
                    hourly.hourly.temperature_2m,
                    hourly.hourly.weathercode
                )
                rv7d.adapter = Weather7dAdapter(
                    daily.daily.time,
                    daily.daily.temperature_2m_max,
                    daily.daily.temperature_2m_min,
                    daily.daily.weathercode
                )

                withContext(Dispatchers.Main) {
                    progressCircular24h.visibility = View.GONE
                    progressCircular7d.visibility = View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}