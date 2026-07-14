package com.example.omnicortex.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import java.util.concurrent.TimeUnit

// ── Boot receiver — reschedules background work after reboot ──────────────────
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleBreachMonitor(ctx)
        }
    }
}

fun scheduleBreachMonitor(ctx: Context) {
    val req = PeriodicWorkRequestBuilder<BreachMonitorWorker>(24, TimeUnit.HOURS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()
    WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
        "breach_monitor",
        ExistingPeriodicWorkPolicy.KEEP,
        req
    )
}

// ── Breach monitor worker — runs background HIBP checks ───────────────────────
class BreachMonitorWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        // TODO Phase 2: read watchlist from AegisPreferences,
        // call BreachEngine.checkEmail() for each, persist new records,
        // fire local notification if new breaches found.
        return Result.success()
    }
}
