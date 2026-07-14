package com.example.omnicortex

import android.app.Application
import androidx.work.Configuration
import com.example.omnicortex.data.db.AegisStore
import com.example.omnicortex.shizuku.ShizukuManager

class OmniCortexApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // Load persisted data into memory on startup
        AegisStore.init(this)
        // Set up Shizuku listeners early so availability state is correct
        // by the time the user opens Shizuku Mode. Safe no-op if Shizuku
        // isn't installed — listeners just never fire.
        try {
            ShizukuManager.init()
        } catch (_: Throwable) {
            // Shizuku library not reachable on this device config — Shizuku
            // Mode will simply show its "not installed" gate when opened.
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
