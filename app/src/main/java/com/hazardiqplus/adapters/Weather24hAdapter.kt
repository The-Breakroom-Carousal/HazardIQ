package com.hazardiqplus.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hazardiqplus.R
import com.hazardiqplus.utils.WeatherUtils
import java.text.SimpleDateFormat
import java.util.*

class Weather24hAdapter(
    private val hours: List<String>,
    private val temperatures: List<Double>,
    private val weatherCodes: List<Int>
) : RecyclerView.Adapter<Weather24hAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val ivWeatherIcon: ImageView = view.findViewById(R.id.ivWeatherIcon)
        val tvWeatherTempature: TextView = view.findViewById(R.id.tvWeatherTempature)
        val tvWeatherType: TextView = view.findViewById(R.id.tvWeatherType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather_24h, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            .parse(hours[position])
        holder.tvTime.text = SimpleDateFormat("h a", Locale.getDefault()).format(date)

        holder.tvWeatherTempature.text = "${temperatures[position].toInt()}°C"

        val weatherInfo = WeatherUtils.getWeatherInfo(weatherCodes[position])
        holder.tvWeatherType.text = weatherInfo.first
        holder.ivWeatherIcon.setImageResource(weatherInfo.second)
    }

    override fun getItemCount() = hours.size
}