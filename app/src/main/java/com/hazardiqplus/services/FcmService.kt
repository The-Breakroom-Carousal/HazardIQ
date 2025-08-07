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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hazardiqplus.R
import com.hazardiqplus.ui.responder.ReactSosActitvity
import com.hazardiqplus.utils.SosActionReceiver

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

        val sosId = remoteMessage.data["id"]?.toIntOrNull() ?: -1
        val requesterName = remoteMessage.data["name"] ?: "Unknown"

        // Accept action
        val acceptIntent = Intent(this, SosActionReceiver::class.java).apply {
            action = "ACTION_ACCEPT"
            putExtra("sos_id", sosId)
            putExtra("requesterName", requesterName)
        }

        val acceptPendingIntent = PendingIntent.getBroadcast(
            this,
            sosId, // unique requestCode
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline action
        val declineIntent = Intent(this, SosActionReceiver::class.java).apply {
            action = "ACTION_DECLINE"
            putExtra("sos_id", sosId)
            putExtra("requesterName", requesterName)
        }

        val declinePendingIntent = PendingIntent.getBroadcast(
            this,
            sosId + 1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap intent (opens ReactSosActivity)
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
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingTapIntent)
            .addAction(R.drawable.check_24px, "Accept", acceptPendingIntent)
            .addAction(R.drawable.round_remove_circle_outline_24, "Decline", declinePendingIntent)
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