package ru.tabel.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.tabel.app.data.model.AppSettings
import ru.tabel.app.data.model.Profile
import ru.tabel.app.data.repository.TabelRepository
import javax.inject.Inject

@HiltAndroidApp
class TabelApplication : Application() {

    @Inject lateinit var repository:    TabelRepository
    @Inject lateinit var notifManager:  ru.tabel.app.notifications.TabelNotificationManager
    @Inject lateinit var widgetUpdater: ru.tabel.app.widget.WidgetUpdater

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching { initDefaultData() }
            runCatching { rescheduleNotifications() }
        }
        // Запускаем фоновое обновление виджета каждый час
        ru.tabel.app.widget.WidgetUpdateWorker.schedule(this)
    }

    private suspend fun rescheduleNotifications() {
        val settings = repository.settings.first()
        val times    = repository.allShiftTimes.first()
        val timesMap = times.associateBy { it.type }
        val profile  = repository.activeProfile.first() ?: return
        val today    = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val shifts   = repository.getAllShiftsForProfile(profile.id).first()
            .filter { it.date >= today }
        notifManager.scheduleNotifications(shifts, settings, timesMap)

        // Напоминание заполнить следующий месяц (25-го числа в 19:00)
        val nextMonth  = java.time.LocalDate.now().plusMonths(1)
        val nextPrefix = "%04d-%02d".format(nextMonth.year, nextMonth.monthValue)
        notifManager.scheduleFillingReminder(shifts.none { it.date.startsWith(nextPrefix) })

        // Обновляем виджет при каждом старте приложения
        widgetUpdater.update(shifts, profile.name, times)
    }

    private suspend fun initDefaultData() {
        val profiles = repository.allProfiles.first()
        if (profiles.isEmpty()) {
            repository.profileDao.insertProfile(
                ru.tabel.app.data.model.Profile(
                    id = "default", name = "Основной", isActive = true)
            )
            repository.settingsDao.saveSettings(ru.tabel.app.data.model.AppSettings())
            repository.initDefaultTimes()
            return
        }
        // Защита: если нет активного профиля — активируем первый доступный
        val active = repository.activeProfile.first()
        if (active == null) {
            val first = profiles.first()
            repository.profileDao.deactivateAll()
            repository.profileDao.activateProfile(first.id)
            repository.settingsDao.setActiveProfile(first.id)
        }
    }
}
