package com.hazardiqplus.services

import android.Manifest
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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hazardiqplus.R
import com.hazardiqplus.ui.responder.ReactSosActitvity

class FcmService : FirebaseMessagingService(){

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        //TODO()
    }

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

            val sysNotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            sysNotificationManager.createNotificationChannel(channel)
        }

        val title = remoteMessage.notification?.title ?: "🚨 SOS Alert"
        val body = remoteMessage.notification?.body ?: "Someone needs help nearby!"

        val intent = Intent(this, ReactSosActitvity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("lat", remoteMessage.data["lat"])
            putExtra("lng", remoteMessage.data["lng"])
            putExtra("type", remoteMessage.data["type"])
            putExtra("requesterName", remoteMessage.data["name"])
        }

        val stackBuilder = TaskStackBuilder.create(this).apply {
            addNextIntentWithParentStack(intent)
        }

        val pendingIntent = stackBuilder.getPendingIntent(
            0,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.sos)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
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
}