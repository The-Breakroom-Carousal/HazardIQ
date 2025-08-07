package com.hazardiqplus.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.UpdateProgressRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SosActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val sosId = intent.getIntExtra("sos_id", -1)
        val requesterName = intent.getStringExtra("requesterName")

        when (action) {
            "ACTION_ACCEPT" -> {
                if (sosId != -1) {
                    val request = UpdateProgressRequest(progress = "acknowledged")
                    RetrofitClient.instance.updateSosProgress(sosId, request)
                        .enqueue(object : Callback<com.hazardiqplus.data.UpdateProgressResponse> {
                            override fun onResponse(
                                call: Call<com.hazardiqplus.data.UpdateProgressResponse>,
                                response: Response<com.hazardiqplus.data.UpdateProgressResponse>
                            ) {
                                Toast.makeText(context, "Accepted request from $requesterName", Toast.LENGTH_SHORT).show()
                            }

                            override fun onFailure(call: Call<com.hazardiqplus.data.UpdateProgressResponse>, t: Throwable) {
                                Toast.makeText(context, "Failed to accept request", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
            }

            "ACTION_DECLINE" -> {
                Toast.makeText(context, "Declined request from $requesterName", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
