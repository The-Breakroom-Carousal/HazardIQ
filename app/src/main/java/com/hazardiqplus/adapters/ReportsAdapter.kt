package com.hazardiqplus.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hazardiqplus.R
import com.hazardiqplus.data.Hazard
import java.text.SimpleDateFormat
import java.util.*

class ReportsAdapter(
    private val context: Context,
    private val reports: MutableList<Hazard>
) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        holder.tvHazardType.text = report.hazard
        holder.tvRadius.text = "Radius: ${report.rad} km"
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = geocoder.getFromLocation(report.latitude, report.longitude, 1)?.firstOrNull()
        val city = address?.locality ?: "Unknown"
        holder.tvLocation.text = "Location: $city"

        report.timestamp.let {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(it)

                val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                holder.tvReportTime.text = date?.let { d -> formatter.format(d) } ?: "--:--"
            } catch (e: Exception) {
                holder.tvReportTime.text = "--:--"
            }
        }
    }

    override fun getItemCount(): Int = reports.size

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHazardType: TextView = itemView.findViewById(R.id.tvHazardType)
        val tvRadius: TextView = itemView.findViewById(R.id.tvRadius)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvReportTime: TextView = itemView.findViewById(R.id.tvReportTime)
    }

    fun updateReports(newReports: List<Hazard>) {
        reports.clear()
        reports.addAll(newReports)
        notifyDataSetChanged()
    }
}
