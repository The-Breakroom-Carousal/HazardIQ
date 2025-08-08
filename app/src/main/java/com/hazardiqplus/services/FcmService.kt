package com.hazardiqplus.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hazardiqplus.R
import com.hazardiqplus.clients.RetrofitClient
import com.hazardiqplus.data.FcmTokenUpdateRequest
import com.hazardiqplus.data.UserResponse
import com.hazardiqplus.ui.responder.ReactSosActitvity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FcmService : FirebaseMessagingService(){

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val channelId = "Sos_Notification"
        val notificationManager = NotificationManagerCompat.from(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SOS Requests",
                NotificationManager.IMPORTANCE_HIGH
            )
            val sysNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            sysNotificationManager.createNotificationChannel(channel)
        }

        val title = remoteMessage.notification?.title ?: "🚨 SOS Alert"
        val body = remoteMessage.notification?.body ?: "Someone needs help nearby!"

        val requesterName = remoteMessage.data["name"] ?: "Unknown"

        val tapIntent = Intent(this, ReactSosActitvity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("lat", remoteMessage.data["lat"])
            putExtra("lng", remoteMessage.data["lng"])
            putExtra("type", remoteMessage.data["type"])
            putExtra("requesterName", requesterName)
        }

        val pendingTapIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(tapIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.sos)
            .setContentTitle(title)
            .setContentText("Tap to respond")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingTapIntent)
            .build()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify((0..100000).random(), notification)
            } else {
                Log.w("FCMService", "POST_NOTIFICATIONS permission not granted")
            }
        } else {
            notificationManager.notify((0..100000).random(), notification)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "New token generated: $token")
        Firebase.auth.currentUser?.let {
            sendTokenToServer(token)
        }
    }

    private fun sendTokenToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val idToken = Firebase.auth.currentUser?.getIdToken(true)?.await()?.token
                if (idToken == null) {
                    Log.e("FCM_TOKEN", "User is not authenticated, cannot send token.")
                    return@launch
                }
                val request = FcmTokenUpdateRequest(fcmToken = token)
                val call = RetrofitClient.instance.updateUser(idToken, request)
                call.enqueue(object : Callback<UserResponse> {
                    override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                        if (response.isSuccessful) {
                            val userResponse = response.body()
                            Log.d("UpdateUser", "Success: ${userResponse}")
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
}

