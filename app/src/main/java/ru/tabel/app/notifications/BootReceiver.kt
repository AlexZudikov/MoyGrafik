package ru.tabel.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.tabel.app.data.model.AppSettings
import ru.tabel.app.data.repository.TabelRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// После перезагрузки телефона AlarmManager сбрасывает все алармы.
// Этот receiver восстанавливает уведомления.
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repo:         TabelRepository
    @Inject lateinit var notifManager: TabelNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val settings   = repo.settings.first() ?: AppSettings()
                val times      = repo.allShiftTimes.first().associateBy { it.type }
                val profile    = repo.activeProfile.first() ?: return@launch
                val today      = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val shifts     = repo.getAllShiftsForProfile(profile.id).first()
                    .filter { it.date >= today }
                notifManager.scheduleNotifications(shifts, settings, times)

                // Напоминание заполнить следующий месяц
                val nextMonth  = LocalDate.now().plusMonths(1)
                val nextPrefix = "%04d-%02d".format(nextMonth.year, nextMonth.monthValue)
                val nextEmpty  = shifts.none { it.date.startsWith(nextPrefix) }
                notifManager.scheduleFillingReminder(nextEmpty)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
