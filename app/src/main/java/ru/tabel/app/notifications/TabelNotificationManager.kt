package ru.tabel.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import ru.tabel.app.MainActivity
import ru.tabel.app.R
import ru.tabel.app.data.model.AppSettings
import ru.tabel.app.data.model.ShiftEntry
import ru.tabel.app.data.model.ShiftTime
import ru.tabel.app.data.model.ShiftType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

const val CHANNEL_ID   = "tabel_shifts"
const val CHANNEL_NAME = "Смены"

private const val ALARM_ID_START = 1000
private const val ALARM_ID_END   = 1300

@Singleton
class TabelNotificationManager @Inject constructor(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: String = "default"

    init { createChannel("default") }

    fun updateChannelSound(soundUri: String) {
        currentSound = soundUri
        createChannel(soundUri)
    }

    private fun createChannel(soundUri: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val sound = when (soundUri) {
                "silent" -> null
                "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else -> runCatching { Uri.parse(soundUri) }.getOrNull()
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о сменах"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 400)
                enableLights(true)
                lightColor = 0xFF4F6EF7.toInt()
                if (sound != null) setSound(sound, audioAttr) else setSound(null, null)
            }

            nm.deleteNotificationChannel(CHANNEL_ID)
            nm.createNotificationChannel(channel)
        }
    }

    fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }

    fun vibrate(ms: Long = 50) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator
                    .vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).let { v ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                    else @Suppress("DEPRECATION") v.vibrate(ms)
                }
            }
        }
    }

    fun previewSound(soundUri: String) {
        stopPreview()
        if (soundUri == "silent") return
        runCatching {
            val uri = if (soundUri == "default")
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else Uri.parse(soundUri)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
                start()
                setOnCompletionListener { it.release(); mediaPlayer = null }
            }
        }
    }

    fun stopPreview() {
        runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        mediaPlayer = null
    }

    fun scheduleNotifications(
        shifts: List<ShiftEntry>,
        settings: AppSettings,
        shiftTimesMap: Map<ShiftType, ShiftTime>
    ) {
        val am  = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        for (i in ALARM_ID_START..ALARM_ID_END) {
            PendingIntent.getBroadcast(
                context, i,
                Intent(context, NotificationReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { am.cancel(it) }
        }

        if (settings.notifHoursBefore <= 0) return

        // Обновляем канал с выбранным звуком
        updateChannelSound(settings.notifSound)

        var id = ALARM_ID_START
        shifts
            .filter { it.date >= today }
            .sortedBy { it.date }
            .take(60)
            .forEach { entry ->
                if (!entry.type.hasTime) return@forEach
                when (entry.type) {
                    ShiftType.OFF, ShiftType.SICK, ShiftType.VACATION -> return@forEach
                    else -> { /* продолжаем */ }
                }

                val st = shiftTimesMap[entry.type]
                val startTime = entry.customStartTime
                    ?: st?.startTime
                    ?: when (entry.type) {
                        ShiftType.DAY     -> "08:00"
                        ShiftType.NIGHT   -> "20:00"
                        ShiftType.SLEEP   -> st?.startTime
                        ShiftType.HOLIDAY -> "08:00"
                        else              -> return@forEach
                    }

                if (startTime == null) return@forEach
                val parts = startTime.split(":").map { it.toIntOrNull() ?: 0 }
                val hh = parts.getOrElse(0) { 8 }
                val mm = parts.getOrElse(1) { 0 }
                val shiftDate  = LocalDate.parse(entry.date)
                val shiftStart = shiftDate.atTime(hh, mm)

                val t = shiftStart.minusHours(settings.notifHoursBefore.toLong())
                if (t.isAfter(now) && id <= ALARM_ID_END) {
                    val h    = settings.notifHoursBefore
                    val word = when {
                        h == 1                                      -> "час"
                        h % 10 in 2..4 && h % 100 !in 12..14     -> "часа"
                        else                                        -> "часов"
                    }
                    val timeLabel = if (h == 0) "сейчас" else "через $h $word"
                    
                    val titleText = "${entry.type.icon} ${entry.type.label}"
                    val bodyText = when {
                        h == 0 -> "Начинается прямо сейчас!"
                        h == 1 -> "Через 1 час"
                        else -> "Через $h $word — начало в $startTime"
                    }
                    
                    scheduleAlarm(am, id++, t, titleText, bodyText, settings.notifSound)
                }
            }
    }

    fun scheduleFillingReminder(nextMonthIsEmpty: Boolean) {
        val am  = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDate.now()

        PendingIntent.getBroadcast(
            context, 1999,
            Intent(context, NotificationReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { am.cancel(it) }

        if (!nextMonthIsEmpty) return

        val target25 = now.withDayOfMonth(25)
        val targetDate = if (now.dayOfMonth <= 25) target25 else target25.plusMonths(1)
        val triggerTime = targetDate.atTime(19, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        val nextMonth = now.plusMonths(1)
        val months = listOf("январь","февраль","март","апрель","май","июнь",
            "июль","август","сентябрь","октябрь","ноябрь","декабрь")
        val monthName = months[nextMonth.monthValue - 1]

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id",    1999)
            putExtra("title", "📅 Заполни график на $monthName")
            putExtra("body",  "Не забудь внести смены в календарь")
            putExtra("sound", currentSound)
        }
        val pi = PendingIntent.getBroadcast(
            context, 1999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            }
        }
    }

    private fun scheduleAlarm(
        am: AlarmManager, id: Int, time: LocalDateTime,
        title: String, body: String, sound: String
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id",    id)
            putExtra("title", title)
            putExtra("body",  body)
            putExtra("sound", sound)
        }
        val pi = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val ms = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            }
        }.onFailure {
            runCatching { am.set(AlarmManager.RTC_WAKEUP, ms, pi) }
        }
    }

    fun showNotification(id: Int, title: String, body: String, soundUri: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val pi = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sound = when (soundUri) {
            "silent" -> null
            "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else -> runCatching { Uri.parse(soundUri) }.getOrNull()
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 300, 150, 300, 150, 400))
            .setLights(0xFF4F6EF7.toInt(), 500, 500)

        if (sound != null) {
            builder.setSound(sound)
        } else {
            builder.setSilent(true)
        }

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(id, builder.build())
    }
}
