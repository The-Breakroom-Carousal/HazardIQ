package com.hazardiqplus.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.SosEvent
import com.hazardiqplus.data.UserName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MySosAdapter(
    private val context: Context,
    private val sosList: MutableList<SosEvent>,
    private val listener: OnActionListener
) : RecyclerView.Adapter<MySosAdapter.SosViewHolder>() {

    interface OnActionListener {
        fun onDelete(event: SosEvent)
        fun onMarkAsResolved(event: SosEvent)
    }

    inner class SosViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvSosType)
        val tvCity: TextView = view.findViewById(R.id.tvSosCity)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvResponderDetails: TextView = view.findViewById(R.id.tvResponderDetails)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
        val btnMarkAsResolved: Button = view.findViewById(R.id.btnMarkAsResolvedC)
        val btnChat: Button = view.findViewById(R.id.btnChat)
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
        if (event.progress == "acknowledged" && event.responder_uid != null) {
            holder.btnMarkAsResolved.visibility = View.VISIBLE
            holder.btnChat.visibility = View.VISIBLE
            RetrofitClient.instance.getUserName(event.responder_uid)
                .enqueue(object : Callback<UserName> {
                    override fun onResponse(
                        call: Call<UserName>,
                        response: Response<UserName>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            holder.tvResponderDetails.text = "Responder: ${response.body()?.name}"
                            holder.tvResponderDetails.visibility = View.VISIBLE
                        }
                    }

                    override fun onFailure(call: Call<UserName>, t: Throwable) {
                        Log.d("MySosAdapter", "Failed to fetch user details")
                    }
                })
        } else {
            holder.tvResponderDetails.visibility = View.GONE
            holder.btnChat.visibility = View.GONE
            holder.btnMarkAsResolved.visibility = View.GONE
        }

        holder.btnCancel.setOnClickListener {
            listener.onDelete(event)
        }
        holder.btnMarkAsResolved.setOnClickListener {
            listener.onMarkAsResolved(event)
        }
        holder.btnChat.setOnClickListener {
            /*val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("sosId", event.id.toString())
            context?.startActivity(intent)*/
        }
    }

    override fun getItemCount() = sosList.size
}
