package com.hazardiqplus.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.hazardiqplus.R

data class PredictedAqi(val hour: String, val aqi: Int)

class PredictedAqiAdapter(
    private val predictions: List<PredictedAqi>
) : RecyclerView.Adapter<PredictedAqiAdapter.PredictedAqiViewHolder>() {

    inner class PredictedAqiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHour: TextView = view.findViewById(R.id.tvHour)
        val tvPredictedAqi: TextView = view.findViewById(R.id.tvPredictedAqi)
        val aqiDot: View = view.findViewById(R.id.aqiDot)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val cardStatus: MaterialCardView = view.findViewById(R.id.cardStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictedAqiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_predicted_aqi, parent, false)
        return PredictedAqiViewHolder(view)
    }

    override fun onBindViewHolder(holder: PredictedAqiViewHolder, position: Int) {
        val item = predictions[position]
        holder.tvHour.text = item.hour
        holder.tvPredictedAqi.text = item.aqi.toString()

        // Set colors based on AQI
        val (statusText, statusColor, dotColor) = when (item.aqi) {
            in 0..50 -> Triple("Good", 0xFF4CAF50.toInt(), 0xFF4CAF50.toInt())
            in 51..100 -> Triple("Moderate", 0xFFFFC107.toInt(), 0xFFFFC107.toInt())
            in 101..150 -> Triple("Unhealthy", 0xFFFF9800.toInt(), 0xFFFF9800.toInt())
            in 151..200 -> Triple("Unhealthy\n(Sensitive)", 0xFFF44336.toInt(), 0xFFF44336.toInt())
            in 201..300 -> Triple("Very Unhealthy", 0xFF9C27B0.toInt(), 0xFF9C27B0.toInt())
            else -> Triple("Hazardous", 0xFF795548.toInt(), 0xFF795548.toInt())
        }

        // Apply status text & chip background
        holder.tvStatus.text = statusText
        holder.cardStatus.setCardBackgroundColor(statusColor)

        // Change AQI dot color (using GradientDrawable)
        val drawable = holder.aqiDot.background as GradientDrawable
        drawable.setColor(dotColor)
    }

    override fun getItemCount(): Int = predictions.size
}