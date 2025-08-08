package com.hazardiqplus.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.SosEvent
import com.hazardiqplus.data.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
        if (event.progress == "acknowledged" && event.responder_uid != null) {
            Firebase.auth.currentUser?.getIdToken(true)
                ?.addOnSuccessListener { tokenResult ->
                    val idToken = tokenResult.token
                    RetrofitClient.instance.getUserDetails(idToken ?: "")
                        .enqueue(object : Callback<User> {
                            override fun onResponse(call: Call<User>, response: Response<User>) {
                                if (response.isSuccessful && response.body() != null) {
                                    holder.tvResponderDetails.text = "Responder: ${response.body()?.name}"
                                    holder.tvResponderDetails.visibility = View.VISIBLE
                                }
                            }

                            override fun onFailure(call: Call<User>, t: Throwable) {
                                Log.d("MySosAdapter", "Failed to fetch user details")
                            }
                        })
                }
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
