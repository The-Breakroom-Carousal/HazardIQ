package com.hazardiqplus.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.hazardiqplus.R
import com.hazardiqplus.data.SosEvent
import com.hazardiqplus.ui.responder.ReactSosActitvity
import com.hazardiqplus.ui.SosChatActivity

class SosRequestAdapter(
    private val listener: SosActionListener,
    private val context: Context
) : RecyclerView.Adapter<SosRequestAdapter.SosViewHolder>() {

    private val events = mutableListOf<SosEvent>()

    interface SosActionListener {
        fun onAccept(event: SosEvent)
        fun onDecline(event: SosEvent)
        fun onMarkAsCompleted(event: SosEvent)
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
        val btnMarkAsResolved: Button = view.findViewById(R.id.btnMarkAsResolved)
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

        if (event.progress == "pending") {
            holder.btnAccept.text = "Accept"
            holder.btnDecline.text = "Decline"
            holder.btnMarkAsResolved.visibility = View.GONE
        } else if (event.progress == "acknowledged" && event.responder_uid == FirebaseAuth.getInstance().currentUser?.uid) {
            holder.btnAccept.text = "Chat"
            holder.btnDecline.text = "Map"
            holder.btnMarkAsResolved.visibility = View.VISIBLE
        }
        else {
            holder.btnAccept.visibility = View.GONE
            holder.btnDecline.visibility = View.GONE
            holder.btnMarkAsResolved.visibility = View.GONE
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
                val intent = Intent(context, SosChatActivity::class.java)
                intent.putExtra("sosId", event.id.toString())
                context.startActivity(intent)
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
        holder.btnMarkAsResolved.setOnClickListener {
            listener.onMarkAsCompleted(event)
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