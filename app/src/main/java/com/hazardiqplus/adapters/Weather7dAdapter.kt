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

class Weather7dAdapter(
    private val dates: List<String>,
    private val tempsMax: List<Double>,
    private val tempsMin: List<Double>,
    private val weatherCodes: List<Int>
) : RecyclerView.Adapter<Weather7dAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivWeatherIcon: ImageView = view.findViewById(R.id.ivWeatherIcon)
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val tvWeatherType: TextView = view.findViewById(R.id.tvWeatherType)
        val tvMaxTemp: TextView = view.findViewById(R.id.textView6)
        val tvMinTemp: TextView = view.findViewById(R.id.tvWeatherTempature)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather_7d, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dates[position])
        holder.tvDay.text = if (position == 0) "Today" else SimpleDateFormat("EEE", Locale.getDefault()).format(date)

        val weatherInfo = WeatherUtils.getWeatherInfo(weatherCodes[position])
        holder.tvWeatherType.text = weatherInfo.first
        holder.ivWeatherIcon.setImageResource(weatherInfo.second)

        holder.tvMaxTemp.text = "${tempsMax[position].toInt()}°C"
        holder.tvMinTemp.text = "${tempsMin[position].toInt()}°C"
    }

    override fun getItemCount() = dates.size
}
