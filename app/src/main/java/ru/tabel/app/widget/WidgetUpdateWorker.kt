package ru.tabel.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import ru.tabel.app.di.WidgetWorkerEntryPoint
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val ep   = EntryPointAccessors.fromApplication(
                applicationContext, WidgetWorkerEntryPoint::class.java
            )
            val repo    = ep.repository()
            val profile = repo.activeProfile.first() ?: return Result.success()
            val times   = repo.allShiftTimes.first()
            val today   = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val shifts  = repo.getAllShiftsForProfile(profile.id).first()
                .filter { it.date >= today }

            ep.widgetUpdater().update(shifts, profile.name, times)
            ShiftWidget().updateAll(applicationContext)
            Result.success()
        }.getOrDefault(Result.retry())
    }

    companion object {
        private const val WORK_NAME = "widget_hourly_update"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(Constraints.NONE)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
