package com.hazardiqplus.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hazardiqplus.R
import com.hazardiqplus.data.SosEvent

class MySosAdapter(
    private val sosList: MutableList<SosEvent>,
    private val listener: OnActionListener
) : RecyclerView.Adapter<MySosAdapter.SosViewHolder>() {

    interface OnActionListener {
        fun onDelete(event: SosEvent)
    }

    inner class SosViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvSosType)
        val tvCity: TextView = view.findViewById(R.id.tvSosCity)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvResponderDetails: TextView = view.findViewById(R.id.tvResponderDetails)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SosViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_sos, parent, false)
        return SosViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SosViewHolder, position: Int) {
        val event = sosList[position]
        holder.tvType.text = event.type
        holder.tvCity.text = event.city
        holder.tvStatus.text = "Status: ${event.progress.replaceFirstChar { it.uppercase()}}"
        if (event.progress == "acknowledged") {
            holder.tvResponderDetails.text = "Responder: Sayantan Sen"
            holder.tvResponderDetails.visibility = View.VISIBLE
        } else {
            holder.tvResponderDetails.visibility = View.GONE
        }

        holder.btnDelete.setOnClickListener {
            listener.onDelete(event)
        }
    }

    override fun getItemCount() = sosList.size

    fun updateData(newList: List<SosEvent>) {
        sosList.clear()
        sosList.addAll(newList)
        notifyDataSetChanged()
    }
}
