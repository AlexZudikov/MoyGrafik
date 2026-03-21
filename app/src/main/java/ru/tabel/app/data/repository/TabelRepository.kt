package ru.tabel.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.tabel.app.data.db.*
import ru.tabel.app.data.model.*
import java.time.LocalDate
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabelRepository @Inject constructor(
    val shiftDao: ShiftDao,
    val profileDao: ProfileDao,
    val shiftTimeDao: ShiftTimeDao,
    val settingsDao: SettingsDao
) {
    // ── Настройки ──────────────────────────────────────────────
    val settings: Flow<AppSettings> = settingsDao.getSettings().map { it ?: AppSettings() }

    suspend fun saveSettings(s: AppSettings) = settingsDao.saveSettings(s)

    // ── Профили ────────────────────────────────────────────────
    val allProfiles: Flow<List<Profile>> = profileDao.getAllProfiles()
    val activeProfile: Flow<Profile?>    = profileDao.getActiveProfile()

    suspend fun addProfile(name: String, color: Long = 0xFF4F6EF7) {
        profileDao.insertProfile(
            Profile(id = "profile_${System.currentTimeMillis()}", name = name, color = color)
        )
    }

    suspend fun switchProfile(profileId: String) {
        profileDao.deactivateAll()
        profileDao.activateProfile(profileId)
        settingsDao.setActiveProfile(profileId)
    }

    suspend fun deleteProfile(profile: Profile) {
        shiftDao.deleteAllShiftsForProfile(profile.id)
        profileDao.deleteProfile(profile)
    }

    suspend fun initDefaultProfile() {
        profileDao.insertProfile(Profile(id = "default", name = "Основной", isActive = true))
    }

    // ── Смены ──────────────────────────────────────────────────
    fun getAllShiftsForProfile(profileId: String): Flow<List<ShiftEntry>> =
        shiftDao.getShiftsForProfile(profileId)

    fun getShiftsForMonth(profileId: String, year: Int, month: Int): Flow<List<ShiftEntry>> =
        shiftDao.getShiftsForMonth(profileId, "%04d-%02d".format(year, month))

    fun getUpcomingShifts(profileId: String): Flow<List<ShiftEntry>> {
        val today = LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        return shiftDao.getUpcomingShifts(profileId, today)
    }

    suspend fun saveShift(shift: ShiftEntry) = shiftDao.insertShift(shift)
    suspend fun deleteShift(shift: ShiftEntry) = shiftDao.deleteShift(shift)

    // Автозаполнение — один батч INSERT вместо N одиночных
    suspend fun autofillMonth(
        profileId: String, year: Int, month: Int,
        pattern: List<ShiftType>, startDay: Int = 1, startIndex: Int = 0
    ) {
        val daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth()
        val shifts = ArrayList<ShiftEntry>(daysInMonth - startDay + 1)
        var idx = startIndex
        for (day in startDay..daysInMonth) {
            shifts.add(ShiftEntry(
                date = "%04d-%02d-%02d".format(year, month, day),
                profileId = profileId,
                type = pattern[idx % pattern.size]
            ))
            idx++
        }
        shiftDao.deleteShiftsForMonth(profileId, "%04d-%02d".format(year, month))
        shiftDao.insertShifts(shifts)
    }

    // ── Время смен ─────────────────────────────────────────────
    val allShiftTimes: Flow<List<ShiftTime>> = shiftTimeDao.getAllTimes()

    suspend fun saveShiftTime(time: ShiftTime) = shiftTimeDao.insertTime(time)

    suspend fun initDefaultTimes() {
        shiftTimeDao.insertTimes(listOf(
            ShiftTime(ShiftType.DAY,     "08:00", "20:00"),
            ShiftTime(ShiftType.NIGHT,   "20:00", "08:00"),
            ShiftTime(ShiftType.SLEEP,   "08:00", "20:00"),
            ShiftTime(ShiftType.HOLIDAY, "08:00", "20:00")
        ))
    }

    // ── Статистика ─────────────────────────────────────────────
    fun getMonthStats(shifts: List<ShiftEntry>, times: List<ShiftTime>, s: AppSettings): MonthStats {
        val timeMap   = times.associateBy { it.type }
        var workHours = 0f
        var day = 0; var night = 0; var off = 0

        for (e in shifts) {
            when (e.type) {
                ShiftType.DAY   -> { day++;   workHours += calcHours(e, timeMap, s.breakMinutes) }
                ShiftType.NIGHT -> { night++; workHours += calcHours(e, timeMap, s.breakMinutes) }
                ShiftType.SLEEP, ShiftType.HOLIDAY,
                ShiftType.OFF, ShiftType.VACATION, ShiftType.SICK -> off++
            }
        }
        // Норма рабочих часов — считаем по рабочим дням месяца (пн-пт * 8ч)
        val normHours = if (shifts.isNotEmpty()) {
            val dateStr = shifts.first().date          // "2025-03-01"
            val ym = runCatching {
                java.time.YearMonth.parse(dateStr.substring(0, 7))
            }.getOrNull()
            if (ym != null) {
                var workDays = 0
                for (d in 1..ym.lengthOfMonth()) {
                    val dow = ym.atDay(d).dayOfWeek
                    if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY)
                        workDays++
                }
                workDays * 8f
            } else 160f
        } else 160f

        return MonthStats(
            totalShifts     = shifts.size,
            workShifts      = day + night,
            totalHours      = workHours,
            dayShifts       = day,
            nightShifts     = night,
            offDays         = off,
            estimatedSalary = calcSalary(shifts, timeMap, s),
            normHours       = normHours,
            overtimeHours   = workHours - normHours
        )
    }

    private fun calcHours(e: ShiftEntry, timeMap: Map<ShiftType, ShiftTime>, breakMinutes: Int = 0): Float {
        // Значения по умолчанию для каждого типа если в БД нет настроек
        val defaultStart = when (e.type) {
            ShiftType.DAY     -> "08:00"
            ShiftType.NIGHT   -> "20:00"
            ShiftType.SLEEP   -> "08:00"
            ShiftType.HOLIDAY -> "08:00"
            ShiftType.SICK    -> "08:00"
            else              -> return 0f   // OFF, VACATION — 0 рабочих часов
        }
        val defaultEnd = when (e.type) {
            ShiftType.DAY     -> "20:00"
            ShiftType.NIGHT   -> "08:00"
            ShiftType.SLEEP   -> "20:00"
            ShiftType.HOLIDAY -> "20:00"
            ShiftType.SICK    -> "20:00"
            else              -> return 0f
        }
        val t  = timeMap[e.type]
        val st = e.customStartTime ?: t?.startTime ?: defaultStart
        val en = e.customEndTime   ?: t?.endTime   ?: defaultEnd
        val (sh, sm) = st.split(":").map { it.toInt() }
        val (eh, em) = en.split(":").map { it.toInt() }
        var diff = (eh * 60 + em) - (sh * 60 + sm)
        if (diff <= 0) diff += 1440
        // Вычитаем перерыв/обед
        diff = (diff - breakMinutes).coerceAtLeast(0)
        return diff / 60f
    }

    private fun calcSalary(shifts: List<ShiftEntry>, timeMap: Map<ShiftType, ShiftTime>, s: AppSettings): Float {
        if (s.hourlyRate <= 0f) return 0f
        var total = 0f
        for (e in shifts) {
            val h = calcHours(e, timeMap)
            total += when (e.type) {
                ShiftType.DAY     -> h * s.hourlyRate
                ShiftType.NIGHT   -> h * s.hourlyRate * s.nightCoeff
                ShiftType.SLEEP   -> h * s.hourlyRate * 0.5f
                ShiftType.HOLIDAY  -> h * s.hourlyRate * s.holidayCoeff
                ShiftType.SICK     -> calcHours(e, timeMap) * s.hourlyRate * s.sickCoeff
                else -> 0f
            }
        }
        return total
    }
}
