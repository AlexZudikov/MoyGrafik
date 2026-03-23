package ru.tabel.app.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import ru.tabel.app.data.model.AppSettings
import ru.tabel.app.data.repository.TabelRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TabelRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "auto_backup"
        const val CHANNEL_ID = "backup_channel"
        const val NOTIFICATION_ID = 2001

        fun schedule(context: Context, intervalDays: Int) {
            val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                intervalDays.toLong(), TimeUnit.DAYS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            createNotificationChannel()
            
            val settings = repository.settings.first()
            if (!settings.autoBackupEnabled) {
                return Result.success()
            }

            setForeground(createForegroundInfo())

            val backupData = createBackupData()
            val fileName = "tabel_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            
            val saved = saveBackupFile(backupData, fileName)
            
            if (saved) {
                showSuccessNotification()
                Result.success()
            } else {
                showErrorNotification()
                Result.failure()
            }
        } catch (e: Exception) {
            showErrorNotification()
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Автобекап",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Автоматическое резервное копирование данных"
            }
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Создание резервной копии...")
            .setContentText("Пожалуйста, подождите")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private suspend fun createBackupData(): String {
        val settings = repository.settings.first()
        val profiles = repository.allProfiles.first()
        val profile = profiles.find { it.id == settings.activeProfileId } ?: profiles.firstOrNull()
        
        val shifts = if (profile != null) {
            repository.getAllShiftsForProfile(profile.id).first()
        } else emptyList()
        
        val shiftTimes = repository.allShiftTimes.first()
        
        val backup = mapOf(
            "version" to 2,
            "exported" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "settings" to settings,
            "profiles" to profiles,
            "shifts" to shifts,
            "shiftTimes" to shiftTimes
        )
        
        return Gson().toJson(backup)
    }

    private fun saveBackupFile(data: String, fileName: String): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MoyGrafik")
                }
                
                val uri = applicationContext.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                uri?.let {
                    applicationContext.contentResolver.openOutputStream(it)?.use { output ->
                        output.write(data.toByteArray())
                    }
                    true
                } ?: false
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MoyGrafik")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                file.writeText(data)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun showSuccessNotification() {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("✅ Резервная копия создана")
            .setContentText("Данные сохранены в папку Загрузки/MoyGrafik")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showErrorNotification() {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("❌ Ошибка бекапа")
            .setContentText("Не удалось создать резервную копию")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }
}
