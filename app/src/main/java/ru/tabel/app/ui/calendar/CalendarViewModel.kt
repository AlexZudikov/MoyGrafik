package ru.tabel.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tabel.app.data.model.*
import ru.tabel.app.data.repository.TabelRepository
import ru.tabel.app.notifications.TabelNotificationManager
import ru.tabel.app.widget.WidgetUpdater
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repo:          TabelRepository,
    private val notifManager:  TabelNotificationManager,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    val activeProfile: StateFlow<Profile?> = repo.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Все предстоящие смены — для NextShiftCard (не ограничены текущим месяцем)
    @OptIn(ExperimentalCoroutinesApi::class)
    val upcomingShifts: StateFlow<List<ShiftEntry>> = activeProfile
        .flatMapLatest { p ->
            if (p == null) flowOf(emptyList())
            else repo.getUpcomingShifts(p.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shiftTimes: StateFlow<List<ShiftTime>> = repo.allShiftTimes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val shiftsThisMonth: StateFlow<List<ShiftEntry>> = combine(
        _currentMonth, activeProfile
    ) { month, profile -> profile?.id to month }
        .flatMapLatest { (profileId, month) ->
            if (profileId == null) flowOf(emptyList())
            else repo.getShiftsForMonth(profileId, month.year, month.monthValue)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarDays: StateFlow<List<DayInfo>> = combine(
        _currentMonth, shiftsThisMonth
    ) { month, shifts -> buildCalendarGrid(month, shifts) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedShift: StateFlow<ShiftEntry?> = combine(
        _selectedDate, shiftsThisMonth
    ) { date, shifts ->
        if (date == null) null else shifts.find { it.date == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val monthStats: StateFlow<MonthStats> = combine(
        shiftsThisMonth, shiftTimes, settings
    ) { shifts, times, s -> repo.getMonthStats(shifts, times, s) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthStats(0, 0, 0f, 0, 0, 0, 0f))

    private val allShifts: StateFlow<List<ShiftEntry>> = activeProfile
        .flatMapLatest { profile ->
            if (profile?.id == null) flowOf(emptyList())
            else repo.getAllShiftsForProfile(profile.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayShift: StateFlow<ShiftEntry?> = allShifts
        .map { shifts ->
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            shifts.find { it.date == today }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Навигация ──────────────────────────────────────────────
    fun previousMonth() { _currentMonth.update { it.minusMonths(1) } }
    fun nextMonth()     { _currentMonth.update { it.plusMonths(1) } }
    fun goToToday() {
        _currentMonth.value = YearMonth.now()
        _selectedDate.value = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    fun selectDate(date: String) {
        _selectedDate.update { if (it == date) null else date }
    }

    // ── Сохранение смены + перепланирование уведомлений ───────
    fun saveShift(date: String, type: ShiftType?, note: String,
                  customStart: String? = null, customEnd: String? = null,
                  locked: Boolean = false) {
        val profileId = activeProfile.value?.id ?: return
        viewModelScope.launch {
            if (type == null) {
                shiftsThisMonth.value.find { it.date == date }?.let { repo.deleteShift(it) }
            } else {
                repo.saveShift(ShiftEntry(
                    date            = date,
                    profileId       = profileId,
                    type            = type,
                    note            = note,
                    customStartTime = customStart,
                    customEndTime   = customEnd,
                    locked          = locked
                ))
            }
            rescheduleNotifications()
        }
    }

    // ── Автозаполнение месяца ─────────────────────────────────
    fun autofillMonth(pattern: List<ShiftType>, startDate: LocalDate, startIndex: Int = 0) {
        val profileId = activeProfile.value?.id ?: return
        if (pattern.isEmpty()) return
        viewModelScope.launch {
            repo.autofillMonth(
                profileId  = profileId,
                year       = startDate.year,
                month      = startDate.monthValue,
                pattern    = pattern,
                startDay   = startDate.dayOfMonth,
                startIndex = startIndex
            )
            rescheduleNotifications()
        }
    }

    // ── Автозаполнение на год вперёд ──────────────────────────
    fun autofillYear(pattern: List<ShiftType>, startDate: LocalDate, startIndex: Int = 0) {
        val profileId = activeProfile.value?.id ?: return
        if (pattern.isEmpty()) return
        viewModelScope.launch {
            var idx = startIndex
            var current = startDate
            // Ограничиваем до конца текущего года
            val endDate = LocalDate.of(startDate.year, 12, 31)
            while (!current.isAfter(endDate)) {
                val ym = YearMonth.of(current.year, current.month)
                val startDay = if (current.year == startDate.year &&
                    current.month == startDate.month) current.dayOfMonth else 1
                val daysInMonth = ym.lengthOfMonth()
                val shifts = ArrayList<ShiftEntry>(daysInMonth - startDay + 1)
                for (day in startDay..daysInMonth) {
                    shifts.add(ShiftEntry(
                        date      = "%04d-%02d-%02d".format(ym.year, ym.monthValue, day),
                        profileId = profileId,
                        type      = pattern[idx % pattern.size]
                    ))
                    idx++
                }
                repo.shiftDao.deleteShiftsForMonth(
                    profileId, "%04d-%02d".format(ym.year, ym.monthValue)
                )
                repo.shiftDao.insertShifts(shifts)
                current = ym.plusMonths(1).atDay(1)
            }
            rescheduleNotifications()
        }
    }

    fun clearMonth() {
        val profileId = activeProfile.value?.id ?: return
        val month = _currentMonth.value
        viewModelScope.launch {
            repo.shiftDao.deleteShiftsForMonth(
                profileId, "%04d-%02d".format(month.year, month.monthValue)
            )
            rescheduleNotifications()
        }
    }

    fun clearYear() {
        val profileId = activeProfile.value?.id ?: return
        val month = _currentMonth.value
        viewModelScope.launch {
            repo.shiftDao.deleteShiftsForYear(
                profileId, "%04d".format(month.year)
            )
            rescheduleNotifications()
        }
    }

    // ── Применение шаблона ────────────────────────────────────
    fun applyTemplate(template: ShiftTemplate, year: Int, month: Int, startDay: Int = 1) {
        val profileId = activeProfile.value?.id ?: return
        viewModelScope.launch {
            repo.applyTemplate(profileId, template, year, month, startDay)
            rescheduleNotifications()
        }
    }

    fun applyTemplateToYear(template: ShiftTemplate, startYear: Int, startMonth: Int, startDay: Int = 1) {
        val profileId = activeProfile.value?.id ?: return
        viewModelScope.launch {
            repo.applyTemplateToYear(profileId, template, startYear, startMonth, startDay)
            rescheduleNotifications()
        }
    }

    // ── Перепланирование всех уведомлений ─────────────────────
    // БАГ 1 FIXED: читаем напрямую из repo после записи в БД,
    // потому что StateFlow обновляется асинхронно и .value может быть устаревшим
    private suspend fun rescheduleNotifications() {
        runCatching {
            val s       = settings.value
            val times   = shiftTimes.value.associateBy { it.type }
            val profile = activeProfile.value ?: return
            val today   = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val shifts  = repo.getAllShiftsForProfile(profile.id).first()
                .filter { it.date >= today }
            notifManager.scheduleNotifications(shifts, s, times)
            // Обновляем виджет при каждом изменении смен
            widgetUpdater.update(shifts, profile.name, shiftTimes.value)
        }
    }

    // ── Построение сетки ──────────────────────────────────────
    private fun buildCalendarGrid(month: YearMonth, shifts: List<ShiftEntry>): List<DayInfo> {
        val today    = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val shiftMap = shifts.associateBy { it.date }
        val startOff = month.atDay(1).dayOfWeek.value - 1
        val daysInM  = month.lengthOfMonth()
        val grid     = ArrayList<DayInfo>(42)

        val prevMonth = month.minusMonths(1)
        val prevDays  = prevMonth.lengthOfMonth()
        repeat(startOff) { i ->
            val d    = prevDays - startOff + i + 1
            val date = "%04d-%02d-%02d".format(prevMonth.year, prevMonth.monthValue, d)
            grid.add(DayInfo(date, d, false, false, shiftMap[date]))
        }
        repeat(daysInM) { i ->
            val d    = i + 1
            val date = "%04d-%02d-%02d".format(month.year, month.monthValue, d)
            grid.add(DayInfo(date, d, date == today, true, shiftMap[date]))
        }
        val nextMonth = month.plusMonths(1)
        var nd = 1
        while (grid.size < 42) {
            val date = "%04d-%02d-%02d".format(nextMonth.year, nextMonth.monthValue, nd)
            grid.add(DayInfo(date, nd, false, false, shiftMap[date]))
            nd++
        }
        return grid
    }
    // ── Все профили для быстрого переключения ────────────────
    val allProfiles: StateFlow<List<ru.tabel.app.data.model.Profile>> = repo.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            repo.profileDao.deactivateAll()
            repo.profileDao.activateProfile(profileId)
            repo.settingsDao.setActiveProfile(profileId)
        }
    }

}
