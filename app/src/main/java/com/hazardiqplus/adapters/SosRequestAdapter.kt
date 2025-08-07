package com.hazardiqplus.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.hazardiqplus.R
import com.hazardiqplus.data.SosEvent
import com.hazardiqplus.ui.responder.ReactSosActitvity

class SosRequestAdapter(
    private val listener: SosActionListener
) : RecyclerView.Adapter<SosRequestAdapter.SosViewHolder>() {

    private val events = mutableListOf<SosEvent>()

    interface SosActionListener {
        fun onAccept(event: SosEvent)
        fun onDecline(event: SosEvent)
    }

    inner class SosViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPhone: TextView = view.findViewById(R.id.tvPhone)
        val tvPriority: TextView = view.findViewById(R.id.tvPriority)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvTimeAgo: TextView = view.findViewById(R.id.tvTimeAgo)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val imgType: ImageView = view.findViewById(R.id.imgType)
        val btnAccept: Button = view.findViewById(R.id.btnAccept)
        val btnDecline: Button = view.findViewById(R.id.btnDecline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SosViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sos_request, parent, false)
        return SosViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SosViewHolder, position: Int) {
        val event = events[position]

        holder.tvName.text = event.name
        holder.tvPhone.text = event.email
        holder.tvPriority.text = event.progress.uppercase()
        holder.tvLocation.text = "${event.city} (${event.latitude}, ${event.longitude})"
        holder.tvTimeAgo.text = event.timestamp
        holder.tvDescription.text = event.type

        // Change button text based on progress
        if (event.progress == "acknowledged") {
            holder.btnAccept.text = "Chat"
            holder.btnDecline.text = "Map"
        } else {
            holder.btnAccept.text = "Accept"
            holder.btnDecline.text = "Decline"
        }

        val iconRes = when (event.type.lowercase()) {
            "fire" -> R.drawable.fire_truck
            "medical emergency" -> R.drawable.ambulance
            "police" -> R.drawable.police_car
            else -> R.drawable.warning_icon
        }
        holder.imgType.setImageResource(iconRes)

        holder.btnAccept.setOnClickListener {
            if (event.progress == "acknowledged") {
                Toast.makeText(holder.itemView.context, "Chat with ${event.name}", Toast.LENGTH_SHORT).show()
            } else {
                listener.onAccept(event)
            }
        }
        holder.btnDecline.setOnClickListener {
            if (event.progress == "acknowledged") {
                val intent = Intent(holder.itemView.context, ReactSosActitvity::class.java)
                intent.putExtra("lat", event.latitude.toString())
                intent.putExtra("lng", event.longitude.toString())
                intent.putExtra("type", event.type)
                intent.putExtra("requesterName", event.name)
                holder.itemView.context.startActivity(intent)
            } else {
                listener.onDecline(event)
            }
        }
    }

    override fun getItemCount() = events.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<SosEvent>) {
        events.clear()
        events.addAll(newList)
        notifyDataSetChanged()
    }
}