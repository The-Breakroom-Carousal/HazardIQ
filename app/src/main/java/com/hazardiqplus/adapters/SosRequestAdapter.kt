package com.hazardiqplus.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.hazardiqplus.R
import com.hazardiqplus.data.SosEvent
import com.hazardiqplus.ui.responder.ReactSosActitvity
import com.hazardiqplus.ui.SosChatActivity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.core.graphics.toColorInt
import com.google.android.material.card.MaterialCardView

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
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvTimeAgo: TextView = view.findViewById(R.id.tvTimeAgo)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val imgType: ImageView = view.findViewById(R.id.imgType)
        val btnAccept: MaterialButton = view.findViewById(R.id.btnAccept)
        val btnDecline: MaterialButton = view.findViewById(R.id.btnDecline)
         val showMap: MaterialCardView = view.findViewById(R.id.showMap)
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
        holder.tvLocation.text = event.city
        event.timestamp.let {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(it)

                val now = System.currentTimeMillis()
                val diffMillis = now - (date?.time ?: now)

                val diffMinutes = diffMillis / (60 * 1000)
                val diffHours = diffMillis / (60 * 60 * 1000)
                val diffDays = diffMillis / (24 * 60 * 60 * 1000)

                holder.tvTimeAgo.text = when {
                    diffMinutes < 1 -> "Just now"
                    diffMinutes < 60 -> "$diffMinutes min ago"
                    diffHours < 24 -> "$diffHours hour${if (diffHours > 1) "s" else ""} ago"
                    else -> "$diffDays day${if (diffDays > 1) "s" else ""} ago"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                holder.tvTimeAgo.text = "--:--"
            }
        }
        holder.tvType.text = event.type

        if (event.progress == "pending") {
            holder.btnAccept.apply {
                text = "Accept"
                setBackgroundColor("#1ABC63".toColorInt())
                icon = ContextCompat.getDrawable(context, R.drawable.round_task_alt_24)
            }
            holder.btnDecline.apply {
                text = "Decline"
                setBackgroundColor("#0F131A".toColorInt())
                setTextColor("#FF4D4D".toColorInt())
                iconTint = ColorStateList.valueOf("#FF4D4D".toColorInt())
                strokeColor = ColorStateList.valueOf("#FF4D4D".toColorInt())
                icon = ContextCompat.getDrawable(context, R.drawable.outline_cancel_24)
            }
        } else if (event.progress == "acknowledged" && event.responder_uid == FirebaseAuth.getInstance().currentUser?.uid) {
            holder.btnAccept.apply {
                text = "Chat"
                icon = ContextCompat.getDrawable(context, R.drawable.round_chat_24)
                setBackgroundColor(resources.getColor(R.color.md_theme_primary))
            }
            holder.btnDecline.apply {
                text = "Mark Complete"
                icon = ContextCompat.getDrawable(context, R.drawable.round_task_alt_24)
                setTextColor(Color.WHITE)
                iconTint = ColorStateList.valueOf(Color.WHITE)
                setBackgroundColor("#1ABC63".toColorInt())
                strokeColor = ColorStateList.valueOf("#1ABC63".toColorInt())
            }
        }
        else {
            holder.btnAccept.visibility = View.GONE
            holder.btnDecline.visibility = View.GONE
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
                listener.onMarkAsCompleted(event)
            } else {
                listener.onDecline(event)
            }
        }
        holder.showMap.setOnClickListener {
            val intent = Intent(holder.itemView.context, ReactSosActitvity::class.java)
            intent.putExtra("lat", event.latitude.toString())
            intent.putExtra("lng", event.longitude.toString())
            intent.putExtra("type", event.type)
            intent.putExtra("requesterName", event.name)
            holder.itemView.context.startActivity(intent)
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