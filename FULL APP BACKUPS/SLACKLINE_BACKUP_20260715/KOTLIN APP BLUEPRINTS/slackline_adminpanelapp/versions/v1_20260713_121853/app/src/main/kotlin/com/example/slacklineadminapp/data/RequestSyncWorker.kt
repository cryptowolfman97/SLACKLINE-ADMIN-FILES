package com.example.slacklineadminapp.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class RequestSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sharedPrefs = applicationContext.getSharedPreferences("admin_prefs", Context.MODE_PRIVATE)
        val lastKnownCount = sharedPrefs.getInt("last_pending_count", 0)

        return try {
            val client = HttpClient(Android)
            val response = client.get("https://ovdxetyadfsxehwnbyuz.supabase.co/rest/v1/client_requests?status=eq.pending&select=id") {
                headers {
                    append("apikey", "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz")
                    append("Authorization", "Bearer sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz")
                }
            }
            
            val payload = response.bodyAsText()
            val currentCount = "\"id\"".toRegex().findAll(payload).count()

            if (currentCount > lastKnownCount) {
                triggerSystemNotification(currentCount - lastKnownCount)
            }

            sharedPrefs.edit().putInt("last_pending_count", currentCount).apply()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun triggerSystemNotification(newCount: Int) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "client_requests_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Client Requests", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat) // Swappable with app resource drawables
            .setContentTitle("New Service Request Received")
            .setContentText("You have $newCount new client requests pending review.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
