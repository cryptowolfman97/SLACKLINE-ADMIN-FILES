package com.example.slacklineadminapp.data

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.slacklineadminapp.worker.DeadlineWorker
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object NotificationHelper {

    fun scheduleDeadline(context: Context, taskId: String, taskTitle: String, endDateIso: String) {
        try {
            if (endDateIso.isBlank()) return

            // Parse the end date and target 9:00 AM on that day
            val deadlineDate = LocalDate.parse(endDateIso, DateTimeFormatter.ISO_LOCAL_DATE)
            val triggerMillis = deadlineDate.atTime(9, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val currentMillis = System.currentTimeMillis()
            val delayMillis = triggerMillis - currentMillis

            // If the deadline is already in the past, don't schedule
            if (delayMillis <= 0) return

            val inputData = Data.Builder()
                .putString("TASK_ID", taskId)
                .putString("TASK_TITLE", taskTitle)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DeadlineWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(taskId) // Tag it so we can easily cancel it later if the task is deleted
                .build()

            // REPLACE policy ensures if you edit the task, the old notification is overwritten
            WorkManager.getInstance(context).enqueueUniqueWork(
                taskId,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelDeadline(context: Context, taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(taskId)
    }
}
