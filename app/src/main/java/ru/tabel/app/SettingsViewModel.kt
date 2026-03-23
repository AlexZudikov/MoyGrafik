package ru.tabel.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tabel.app.backup.AutoBackupWorker
import ru.tabel.app.data.model.*
import ru.tabel.app.data.repository.TabelRepository
import ru.tabel.app.notifications.TabelNotificationManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: TabelRepository,
    val notifManager: TabelNotificationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val repository: TabelRepository get() = repo

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

    fun saveNotifSettings(hoursBefore: Int, sound: String) =
        viewModelScope.launch {
            val newSettings = settings.value.copy(
                notifHoursBefore = hoursBefore,
                notifSound       = sound
            )
            repo.saveSettings(newSettings)
            notifManager.updateChannelSound(sound)
            rescheduleNotifications(newSettings)
        }

    fun saveShiftTime(type: ShiftType, start: String, end: String) = viewModelScope.launch {
        repo.saveShiftTime(ShiftTime(type = type, startTime = start, endTime = end))
        rescheduleNotifications(settings.value)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(dynamicColor = enabled))
    }

    fun setFontLocked(locked: Boolean) = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(fontLocked = locked))
    }

    fun setIsLocked(locked: Boolean) = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(isLocked = locked))
    }

    fun setBreakMinutes(minutes: Int) = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(breakMinutes = minutes))
    }

    fun setCloudBackup(enabled: Boolean, uri: String = "") = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(cloudBackupEnabled = enabled, cloudBackupUri = uri))
    }

    fun setAutoBackup(enabled: Boolean, frequencyDays: Int = 7) = viewModelScope.launch {
        repo.saveSettings(settings.value.copy(
            autoBackupEnabled = enabled,
            autoBackupFrequency = frequencyDays
        ))
        
        if (enabled) {
            AutoBackupWorker.schedule(context, frequencyDays)
        } else {
            AutoBackupWorker.cancel(context)
        }
    }

    fun applyTemplate(template: ShiftTemplate, year: Int, month: Int, startDay: Int = 1) {
        viewModelScope.launch {
            val profileId = repo.activeProfile.first()?.id ?: return@launch
            repo.applyTemplate(profileId, template, year, month, startDay)
            rescheduleNotifications(settings.value)
        }
    }

    fun applyTemplateToYear(template: ShiftTemplate, year: Int, month: Int, startDay: Int = 1) {
        viewModelScope.launch {
            val profileId = repo.activeProfile.first()?.id ?: return@launch
            repo.applyTemplateToYear(profileId, template, year, month, startDay)
            rescheduleNotifications(settings.value)
        }
    }

    fun clearMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val profileId = repo.activeProfile.first()?.id ?: return@launch
            repo.clearMonth(profileId, year, month)
            rescheduleNotifications(settings.value)
        }
    }

    fun clearYear(year: Int) {
        viewModelScope.launch {
            val profileId = repo.activeProfile.first()?.id ?: return@launch
            repo.clearYear(profileId, year)
            rescheduleNotifications(settings.value)
        }
    }

    fun restoreShifts(shifts: List<ShiftEntry>) {
        viewModelScope.launch {
            repo.restoreShifts(shifts)
            rescheduleNotifications(settings.value)
        }
    }

    fun restoreTimes(times: List<ShiftTime>) {
        viewModelScope.launch {
            repo.shiftTimeDao.insertTimes(times)
        }
    }

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
