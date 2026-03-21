package ru.tabel.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tabel.app.data.model.*
import ru.tabel.app.data.repository.TabelRepository
import ru.tabel.app.notifications.TabelNotificationManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: TabelRepository,
    val notifManager: TabelNotificationManager   // public — используется в NotifSheet
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val shiftTimes: StateFlow<List<ShiftTime>> = repo.allShiftTimes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(themeMode = mode.name))
    }

    fun setFontScale(scale: Float) = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(fontScale = scale))
    }

    // БАГ 3 FIXED: sickCoeff теперь сохраняется
    fun saveWageSettings(
        rate: Float, nightCoeff: Float, holidayCoeff: Float, sickCoeff: Float = 0.6f
    ) = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(
            hourlyRate    = rate,
            nightCoeff    = nightCoeff,
            holidayCoeff  = holidayCoeff,
            sickCoeff     = sickCoeff
        ))
    }

    // БАГ 2 FIXED: после сохранения настроек — перепланируем уведомления
    fun saveNotifSettings(hoursBefore: Int, sound: String) =
        viewModelScope.launch {
            val newSettings = settings.value.copy(
                notifHoursBefore = hoursBefore,
                notifSound       = sound
            )
            repo.saveSettings(newSettings)
            rescheduleNotifications(newSettings)
        }

    fun saveShiftTime(type: ShiftType, start: String, end: String) = viewModelScope.launch {
        repo.saveShiftTime(ShiftTime(type = type, startTime = start, endTime = end))
        // Время смен изменилось — перепланируем уведомления
        rescheduleNotifications(settings.value)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(dynamicColor = enabled))
    }

    fun setBreakMinutes(minutes: Int) = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(breakMinutes = minutes))
    }

    fun setCloudBackup(enabled: Boolean, uri: String = "") = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(cloudBackupEnabled = enabled, cloudBackupUri = uri))
    }

    // БАГ 1 FIXED: читаем данные из repo напрямую, не из StateFlow
    private suspend fun rescheduleNotifications(s: AppSettings) {
        runCatching {
            val times   = shiftTimes.value.associateBy { it.type }
            val profile = repo.activeProfile.first() ?: return
            val today   = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val shifts  = repo.getAllShiftsForProfile(profile.id).first()
                .filter { it.date >= today }
            notifManager.scheduleNotifications(shifts, s, times)
        }
    }
}
