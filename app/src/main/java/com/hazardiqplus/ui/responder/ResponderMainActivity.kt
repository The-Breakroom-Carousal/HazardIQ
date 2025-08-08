package com.hazardiqplus.ui.responder

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.FcmTokenUpdateRequest
import com.hazardiqplus.data.UserResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResponderMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_responder_main)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.responderFragmentContainer)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.responderFragmentContainer, ResponderHomeFragment())
            }
        }

        // Launch a single coroutine to handle token logic
        lifecycleScope.launch {
            try {
                // Step 1: Get the actual FCM token from Firebase Messaging
                val fcmToken = Firebase.messaging.token.await()
                Log.d("FCM_TOKEN", "Fetched FCM Token: $fcmToken")

                // Step 2: Call the suspend function to update the server
                updateFcmTokenOnServer(fcmToken)
            } catch (e: Exception) {
                Log.e("FCM_TOKEN", "Could not get FCM token", e)
            }
        }
    }

    private suspend fun updateFcmTokenOnServer(fcmToken: String?) {
        if (fcmToken == null) {
            Log.e("FCM_TOKEN", "FCM token is null, cannot update server.")
            return
        }

        try {

            val idToken = Firebase.auth.currentUser?.getIdToken(true)?.await()?.token
            if (idToken == null) {
                Log.e("FCM_TOKEN", "User is not authenticated, cannot send token.")
                return
            }

            val request = FcmTokenUpdateRequest(fcmToken = fcmToken)


            val call = RetrofitClient.instance.updateUser(idToken, request)

            call.enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        val userResponse = response.body()
                        // Do something with userResponse
                        Log.d("UpdateUser", "Success: $userResponse")
                    } else {
                        Log.e("UpdateUser", "Failed: ${response.code()} ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Log.e("UpdateUser", "Error: ${t.message}", t)
                }
            })

        } catch (e: Exception) {
            Log.e("FCM_TOKEN", "Error sending FCM token to server", e)
        }
    }
}