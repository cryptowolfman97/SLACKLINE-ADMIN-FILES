package com.snaploader.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SnapLoaderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Download progress channel
            manager.createNotificationChannel(
                NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    "Downloads",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Download progress notifications"
                }
            )

            // Floating window persistent notification channel
            // Must be LOW importance — no sound, no heads-up, just keeps Service alive
            manager.createNotificationChannel(
                NotificationChannel(
                    OVERLAY_CHANNEL_ID,
                    "Floating Window",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps the floating download window running"
                    setShowBadge(false)
                }
            )
        }
    }

    companion object {
        const val DOWNLOAD_CHANNEL_ID = "snaploader_downloads"
        const val OVERLAY_CHANNEL_ID  = "snaploader_overlay"
    }
}