package ru.tabel.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color

// ── Тип смены ─────────────────────────────────────────────────
enum class ShiftType(
    val label: String,
    val color: Long,
    val icon: String,
    val hasTime: Boolean = true
) {
    DAY     ("Дневная",    0xFF2563EB, "☀️"),   // насыщенный синий
    NIGHT   ("Ночная",     0xFF7C3AED, "🌙"),   // фиолетовый
    SLEEP   ("Отсыпной",   0xFF9333EA, "😴"),   // лиловый
    OFF     ("Выходной",   0xFF16A34A, "🏠", hasTime = false),  // зелёный
    HOLIDAY ("Праздник",   0xFFDC2626, "🎁"),   // красный
    SICK    ("Больничный", 0xFFEA580C, "🤒", hasTime = false),  // оранжевый
    VACATION("Отпуск",     0xFF0891B2, "🌴", hasTime = false);  // голубой

    val colorValue get() = Color(color)
}

// Режим темы
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// ── Профиль ───────────────────────────────────────────────────
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: String,
    val name: String,
    val color: Long = 0xFF4F6EF7,
    val isActive: Boolean = false
)

// ── Запись смены ──────────────────────────────────────────────
@Entity(tableName = "shifts", primaryKeys = ["date", "profileId"])
data class ShiftEntry(
    val date: String,
    val profileId: String,
    val type: ShiftType,
    val note: String = "",
    val customStartTime: String? = null,
    val customEndTime: String? = null
)

// ── Настройки времени смен ────────────────────────────────────
@Entity(tableName = "shift_times")
data class ShiftTime(
    @PrimaryKey val type: ShiftType,
    val startTime: String = "08:00",
    val endTime: String = "20:00"
)

// ── Настройки приложения ──────────────────────────────────────
@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val activeProfileId: String = "default",
    val themeMode: String = "SYSTEM",   // SYSTEM / LIGHT / DARK
    val fontScale: Float = 1.0f,
    val hourlyRate: Float = 300f,
    val nightCoeff: Float = 1.5f,
    val holidayCoeff: Float = 2.0f,
    val sickCoeff: Float = 0.6f,
    val notifHoursBefore: Int = 0,
    val notifSound: String = "default",
    val dynamicColor: Boolean = false,
    val breakMinutes: Int = 0,          // перерыв/обед в минутах (0 = не учитывать)
    val cloudBackupEnabled: Boolean = false,
    val cloudBackupUri: String = ""
) {
    val darkTheme: Boolean get() = themeMode == "DARK"
}

// ── UI модели ─────────────────────────────────────────────────
data class DayInfo(
    val date: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isCurrentMonth: Boolean,
    val shift: ShiftEntry?
)

data class MonthStats(
    val workShifts: Int = 0,
    val totalShifts: Int,
    val totalHours: Float,
    val dayShifts: Int,
    val nightShifts: Int,
    val offDays: Int,
    val estimatedSalary: Float,
    val normHours: Float = 160f,      // норма часов в месяце
    val overtimeHours: Float = 0f     // переработка (отрицательная = недоработка)
)
