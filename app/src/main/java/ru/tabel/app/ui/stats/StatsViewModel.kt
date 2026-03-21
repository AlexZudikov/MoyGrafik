package ru.tabel.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import ru.tabel.app.data.model.*
import ru.tabel.app.data.repository.TabelRepository
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(private val repo: TabelRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val activeProfile = repo.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shiftTimes: StateFlow<List<ShiftTime>> = repo.allShiftTimes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    fun previousMonth() { _selectedMonth.update { it.minusMonths(1) } }
    fun nextMonth()     { _selectedMonth.update { it.plusMonths(1) } }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allShifts: StateFlow<List<ShiftEntry>> = activeProfile
        .flatMapLatest { p ->
            if (p == null) flowOf(emptyList())
            else repo.getAllShiftsForProfile(p.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthShifts: StateFlow<List<ShiftEntry>> = combine(
        allShifts, _selectedMonth
    ) { shifts, month ->
        val prefix = "%04d-%02d".format(month.year, month.monthValue)
        shifts.filter { it.date.startsWith(prefix) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<MonthStats> = combine(
        monthShifts, shiftTimes, settings
    ) { s, t, set ->
        repo.getMonthStats(s, t, set)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthStats(0, 0, 0f, 0, 0, 0, 0f))

    val typeCounts: StateFlow<List<Pair<ShiftType, Int>>> = monthShifts.map { shifts ->
        ShiftType.entries.map { type -> type to shifts.count { it.type == type } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthHistory: StateFlow<List<Pair<String, Triple<Int, Float, Float>>>> =
        combine(allShifts, shiftTimes, settings) { shifts, times, s ->
            shifts
                .groupBy { it.date.substring(0, 7) }
                .entries
                .sortedByDescending { it.key }
                .take(6)
                .map { (month, entries) ->
                    val ms = repo.getMonthStats(entries, times, s)
                    month to Triple(entries.size, ms.totalHours, ms.estimatedSalary)
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Итоговая статистика за последние 12 месяцев
    val yearStats: StateFlow<MonthStats> = combine(
        allShifts, shiftTimes, settings
    ) { shifts, times, s ->
        val cutoff = YearMonth.now().minusMonths(11)
            .let { "%04d-%02d".format(it.year, it.monthValue) }
        val yearShifts = shifts.filter { it.date.substring(0, 7) >= cutoff }
        repo.getMonthStats(yearShifts, times, s)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthStats(0, 0, 0f, 0, 0, 0, 0f))

    // Все смены для экспорта (не только текущий месяц)
    @OptIn(ExperimentalCoroutinesApi::class)
    val allShiftsForExport: StateFlow<List<ShiftEntry>> = allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profileName: StateFlow<String> = activeProfile
        .map { it?.name ?: "График" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "График")

    fun buildShareText(month: YearMonth, shifts: List<ShiftEntry>): String {
        val monthNames = listOf("Январь","Февраль","Март","Апрель","Май","Июнь",
            "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")
        val sb = StringBuilder()
        sb.appendLine("📅 График на ${monthNames[month.monthValue - 1]} ${month.year}")
        sb.appendLine()
        if (shifts.isEmpty()) {
            sb.appendLine("Нет смен")
        } else {
            shifts.sortedBy { it.date }.forEach { entry ->
                val day = entry.date.substring(8).trimStart('0')
                sb.appendLine("$day — ${entry.type.icon} ${entry.type.label}" +
                    if (entry.note.isNotEmpty()) " (${entry.note})" else "")
            }
        }
        sb.appendLine()
        sb.append("Мой График — приложение для учёта рабочих смен")
        return sb.toString()
    }
}
