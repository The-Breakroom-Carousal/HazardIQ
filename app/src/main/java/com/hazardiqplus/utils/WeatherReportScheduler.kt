package com.hazardiqplus.utils

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WeatherReportScheduler {
    fun scheduleWeatherReport(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("WeatherReport")

        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE,40)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(currentTime)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val delayMillis = targetTime.timeInMillis - currentTime.timeInMillis
        val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delayMillis)

        val workRequest = PeriodicWorkRequestBuilder<WeatherReportGenerator>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "WeatherReport",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        Log.d("WeatherReportScheduler", "Weather report worker scheduled.")
    }
}