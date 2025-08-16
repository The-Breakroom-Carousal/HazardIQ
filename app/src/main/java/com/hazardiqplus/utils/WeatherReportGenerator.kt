package com.hazardiqplus.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.hazardiqplus.R
import com.hazardiqplus.clients.OpenMeteoClient
import com.hazardiqplus.ui.LoginActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WeatherReportGenerator (
    context: Context,
    workerParameters: WorkerParameters
): CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d("WeatherReportGenerator", "No user logged in")
            return Result.failure()
        }

        var lat: Double?
        var lon: Double?
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        } else {
            val fusedClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            val location = getLastLocation(fusedClient) ?: return Result.retry()
            lat = location.latitude
            lon = location.longitude
        }

        return try {
            val response = OpenMeteoClient.api.getAQIHourly(lat, lon)
            val hourly = response.hourly
            if (hourly != null) {
                val pm25 = hourly.pm2_5.firstOrNull() ?: 0.0
                val pm10 = hourly.pm10.firstOrNull() ?: 0.0
                val aqi = calculateAQIforShow(pm25, pm10)
                showAQINotification(aqi)
                WeatherReportScheduler().scheduleWeatherReport(applicationContext)
                Result.success()
            } else {
                Log.d("WeatherReportGenerator", "No hourly data found")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.d("WeatherReportGenerator", "Error: ${e.message}")
            Result.retry()
        }
    }

    private fun calculateAQIforShow(pm25: Double, pm10: Double): Int {
        val aqi25 = when {
            pm25 <= 12.0 -> ((50.0/12.0) * pm25)
            pm25 <= 35.4 -> 50 + ((50.0/(35.4-12.0)) * (pm25 - 12.0))
            pm25 <= 55.4 -> 100 + ((50.0/(55.4-35.4)) * (pm25 - 35.4))
            pm25 <= 150.4 -> 150 + ((100.0/(150.4-55.4)) * (pm25 - 55.4))
            pm25 <= 250.4 -> 200 + ((100.0/(250.4-150.4)) * (pm25 - 150.4))
            pm25 <= 350.4 -> 300 + ((100.0/(350.4-250.4)) * (pm25 - 250.4))
            pm25 <= 500.4 -> 400 + ((100.0/(500.4-350.4)) * (pm25 - 350.4))
            else -> 500
        }

        val aqi10 = when {
            pm10 <= 54.0 -> pm10
            pm10 <= 154.0 -> 50 + ((50.0/(154.0-54.0)) * (pm10 - 54.0))
            pm10 <= 254.0 -> 100 + ((50.0/(254.0-154.0)) * (pm10 - 154.0))
            pm10 <= 354.0 -> 150 + ((100.0/(354.0-254.0)) * (pm10 - 254.0))
            pm10 <= 424.0 -> 200 + ((100.0/(424.0-354.0)) * (pm10 - 354.0))
            pm10 <= 504.0 -> 300 + ((100.0/(504.0-424.0)) * (pm10 - 424.0))
            pm10 <= 604.0 -> 400 + ((100.0/(604.0-504.0)) * (pm10 - 504.0))
            else -> 500
        }

        return maxOf(aqi25.toInt(), aqi10.toInt())
    }

    private fun showAQINotification(aqi: Int) {
        val channelId = "aqi_alert"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AQI Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val quality = when (aqi) {
            in 0..50 -> "Good 😊"
            in 51..100 -> "Moderate 😐"
            in 101..200 -> "Unhealthy for Sensitive Groups 😷"
            in 201..300 -> "Unhealthy 😷"
            in 301..400 -> "Very Unhealthy 😷"
            else -> "Hazardous ☠️"
        }

        val message = if (aqi > 100) {
            "AQI $aqi ($quality). Consider wearing a mask outdoors!"
        } else {
            "AQI $aqi ($quality). Air quality is okay today."
        }

        val intent = Intent(applicationContext, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Air Quality Alert")
            .setContentText(message)
            .setSmallIcon(R.drawable.warning_24px)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2001, notification)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getLastLocation(client: FusedLocationProviderClient): Location? =
        suspendCancellableCoroutine { cont ->
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
}