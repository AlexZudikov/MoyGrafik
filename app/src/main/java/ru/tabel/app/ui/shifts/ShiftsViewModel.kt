package ru.tabel.app.ui.shifts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.tabel.app.data.model.*
import ru.tabel.app.data.repository.TabelRepository
import ru.tabel.app.notifications.TabelNotificationManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ShiftsViewModel @Inject constructor(
    private val repo: TabelRepository,
    private val notifManager: TabelNotificationManager
) : ViewModel() {

    private val _searchQuery  = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow<ShiftType?>(null)
    private val _sortDesc     = MutableStateFlow(false)

    val searchQuery:  StateFlow<String>     = _searchQuery.asStateFlow()
    val activeFilter: StateFlow<ShiftType?> = _activeFilter.asStateFlow()
    val sortDesc:     StateFlow<Boolean>    = _sortDesc.asStateFlow()

    // Времена смен — для отображения в строке
    val shiftTimes: StateFlow<List<ShiftTime>> = repo.allShiftTimes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeProfile = repo.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allShifts: StateFlow<List<ShiftEntry>> = activeProfile
        .flatMapLatest { p ->
            if (p == null) flowOf(emptyList())
            else repo.getAllShiftsForProfile(p.id)   // все смены: и прошлые, и будущие
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    val filteredShifts: StateFlow<List<ShiftEntry>> = combine(
        allShifts, _searchQuery.debounce(150), _activeFilter, _sortDesc
    ) { shifts, query, filter, desc ->
        shifts
            .filter { entry ->
                val matchType  = filter == null || entry.type == filter
                val matchQuery = query.isBlank() ||
                    entry.type.label.contains(query, ignoreCase = true) ||
                    entry.date.contains(query) ||
                    entry.note.contains(query, ignoreCase = true) ||
                    formatDateForSearch(entry.date).contains(query, ignoreCase = true)
                matchType && matchQuery
            }
            .let { if (desc) it.sortedByDescending { e -> e.date } else it.sortedBy { e -> e.date } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filterStats: StateFlow<Map<ShiftType?, Int>> = allShifts.map { shifts ->
        buildMap {
            put(null, shifts.size)
            ShiftType.entries.forEach { t -> put(t, shifts.count { it.type == t }) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setSearch(q: String)     { _searchQuery.value = q }
    fun setFilter(t: ShiftType?) { _activeFilter.value = t }
    fun toggleSort()             { _sortDesc.update { !it } }

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Редактирование смены прямо из вкладки «Смены»
    fun saveShift(date: String, type: ShiftType?, note: String,
                  customStart: String? = null, customEnd: String? = null) {
        val profileId = activeProfile.value?.id ?: return
        viewModelScope.launch {
            if (type == null) {
                allShifts.value.find { it.date == date && it.profileId == profileId }
                    ?.let { repo.deleteShift(it) }
            } else {
                repo.saveShift(ShiftEntry(
                    date            = date,
                    profileId       = profileId,
                    type            = type,
                    note            = note,
                    customStartTime = customStart,
                    customEndTime   = customEnd
                ))
            }
            rescheduleNotifications()
        }
    }

    private suspend fun rescheduleNotifications() {
        runCatching {
            val s       = settings.value
            val times   = shiftTimes.value.associateBy { it.type }
            val profile = activeProfile.value ?: return
            val today   = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val shifts  = repo.getAllShiftsForProfile(profile.id).first()
                .filter { it.date >= today }
            notifManager.scheduleNotifications(shifts, s, times)
        }
    }

    private fun formatDateForSearch(iso: String): String {
        return runCatching {
            val d = LocalDate.parse(iso)
            val months = listOf("янв","фев","мар","апр","май","июн",
                "июл","авг","сен","окт","ноя","дек")
            "${d.dayOfMonth} ${months[d.monthValue-1]} ${d.year}"
        }.getOrDefault(iso)
    }
}
