package com.hazardiqplus.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.Hazard
import com.hazardiqplus.data.RemoveHazardResponse
import com.hazardiqplus.utils.PrefsHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
    override fun onBindViewHolder(holder: ReportViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val report = reports[position]
        val role = PrefsHelper.getUserRole(context)

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
            } catch (_: Exception) {
                holder.tvReportTime.text = "--:--"
            }
        }

        holder.btnDelete.visibility = if (role == "responder") View.VISIBLE else View.GONE
        holder.btnDelete.setOnClickListener {
            RetrofitClient.backendInstance.removeHazard(report.id)
                .enqueue(object : Callback<RemoveHazardResponse> {
                override fun onResponse(call: Call<RemoveHazardResponse>, response: Response<RemoveHazardResponse>) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result?.success == true) {
                            reports.removeAt(position)
                            notifyItemRemoved(position)
                            Toast.makeText(context, "Hazard removed successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to remove hazard", Toast.LENGTH_SHORT).show()
                            Log.e("RemoveHazard", "Failed to remove hazard")
                        }
                    }
                }

                override fun onFailure(call: Call<RemoveHazardResponse>, t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RemoveHazard", "Error: ${t.message}")
                }
            })

        }
    }

    override fun getItemCount(): Int = reports.size

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHazardType: TextView = itemView.findViewById(R.id.tvHazardType)
        val tvRadius: TextView = itemView.findViewById(R.id.tvRadius)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvReportTime: TextView = itemView.findViewById(R.id.tvReportTime)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
    }

    fun updateReports(newReports: List<Hazard>) {
        reports.clear()
        reports.addAll(newReports)
        notifyDataSetChanged()
    }
}
