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
    DAY     ("Дневная",    0xFF3B82F6, "☀️"),   // синий (Daylight Blue)
    NIGHT   ("Ночная",     0xFF6D28D9, "🌙"),   // глубокий фиолетовый
    SLEEP   ("Отсыпной",   0xFFF59E0B, "😴"),   // янтарный (Amber) - контрастный
    OFF     ("Выходной",   0xFF16A34A, "🏠", hasTime = false),  // нейтральный зелёный
    HOLIDAY ("Праздник",   0xFFDC2626, "🎁"),   // сигнальный красный
    SICK    ("Больничный", 0xFFEA580C, "🤒", hasTime = false),  // оранжевый
    VACATION("Отпуск",     0xFFBC8F8F, "🌴", hasTime = false);  // пыльно-розовый

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
    val customEndTime: String? = null,
    val locked: Boolean = false
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
    val fontLocked: Boolean = false,   // блокировка шрифта
    val isLocked: Boolean = false,     // блокировка данных (удаление/редактирование)
    val hourlyRate: Float = 300f,
    val nightCoeff: Float = 1.5f,
    val holidayCoeff: Float = 2.0f,
    val sickCoeff: Float = 0.6f,
    val notifHoursBefore: Int = 0,
    val notifSound: String = "default",
    val dynamicColor: Boolean = false,
    val breakMinutes: Int = 0,          // перерыв/обед в минутах (0 = не учитывать)
    val cloudBackupEnabled: Boolean = false,
    val cloudBackupUri: String = "",
    val autoBackupEnabled: Boolean = false,
    val autoBackupFrequency: Int = 7   // дни между бекапами (1, 7, 30)
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
    val normHours: Float = 160f,
    val overtimeHours: Float = 0f
)

// ── Шаблон смены ───────────────────────────────────────────────
@Entity(tableName = "shift_templates")
data class ShiftTemplate(
    @PrimaryKey val id: String,
    val name: String,
    val pattern: String,        // JSON: [{"day": 1, "type": "DAY"}, {"day": 2, "type": "OFF"}, ...]
    val workDays: Int = 0,      // кол-во рабочих дней в шаблоне
    val restDays: Int = 0,      // кол-во выходных в шаблоне
    val isDefault: Boolean = false,
    val profileId: String = "default"
)

// ── Предустановленные шаблоны ─────────────────────────────────────
object DefaultTemplates {
    val templates = listOf(
        ShiftTemplate(
            id = "2_2",
            name = "2/2 (два через два)",
            pattern = """[{"day":1,"type":"DAY"},{"day":2,"type":"DAY"},{"day":3,"type":"OFF"},{"day":4,"type":"OFF"}]""",
            workDays = 2,
            restDays = 2,
            isDefault = true
        ),
        ShiftTemplate(
            id = "2_1_2",
            name = "2/1/2 (два, выходной, два)",
            pattern = """[{"day":1,"type":"DAY"},{"day":2,"type":"DAY"},{"day":3,"type":"OFF"},{"day":4,"type":"DAY"},{"day":5,"type":"DAY"},{"day":6,"type":"OFF"},{"day":7,"type":"OFF"}]""",
            workDays = 4,
            restDays = 3,
            isDefault = true
        ),
        ShiftTemplate(
            id = "3_1_2_2",
            name = "3/1/2/2",
            pattern = """[{"day":1,"type":"DAY"},{"day":2,"type":"DAY"},{"day":3,"type":"DAY"},{"day":4,"type":"OFF"},{"day":5,"type":"DAY"},{"day":6,"type":"DAY"},{"day":7,"type":"OFF"},{"day":8,"type":"OFF"}]""",
            workDays = 5,
            restDays = 3,
            isDefault = true
        ),
        ShiftTemplate(
            id = "5_2",
            name = "5/2 (пять через два)",
            pattern = """[{"day":1,"type":"DAY"},{"day":2,"type":"DAY"},{"day":3,"type":"DAY"},{"day":4,"type":"DAY"},{"day":5,"type":"DAY"},{"day":6,"type":"OFF"},{"day":7,"type":"OFF"}]""",
            workDays = 5,
            restDays = 2,
            isDefault = true
        ),
        ShiftTemplate(
            id = "1_1",
            name = "1/1 (сутки через сутки)",
            pattern = """[{"day":1,"type":"DAY"},{"day":2,"type":"OFF"}]""",
            workDays = 1,
            restDays = 1,
            isDefault = true
        ),
        ShiftTemplate(
            id = "night_2_2",
            name = "Ночные 2/2",
            pattern = """[{"day":1,"type":"NIGHT"},{"day":2,"type":"SLEEP"},{"day":3,"type":"OFF"},{"day":4,"type":"OFF"}]""",
            workDays = 2,
            restDays = 2,
            isDefault = true
        )
    )
}
