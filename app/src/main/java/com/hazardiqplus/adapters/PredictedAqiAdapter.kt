package com.hazardiqplus.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hazardiqplus.R

data class PredictedAqi(val hour: String, val aqi: Int)
class PredictedAqiAdapter(
    private val predictions: List<PredictedAqi>
) : RecyclerView.Adapter<PredictedAqiAdapter.PredictedAqiViewHolder>() {

    inner class PredictedAqiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHour: TextView = view.findViewById(R.id.tvHour)
        val tvPredictedAqi: TextView = view.findViewById(R.id.tvPredictedAqi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictedAqiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_predicted_aqi, parent, false)
        return PredictedAqiViewHolder(view)
    }

    override fun onBindViewHolder(holder: PredictedAqiViewHolder, position: Int) {
        val item = predictions[position]
        holder.tvHour.text = item.hour
        holder.tvPredictedAqi.text = "AQI ${item.aqi}"
    }

    override fun getItemCount(): Int = predictions.size
}