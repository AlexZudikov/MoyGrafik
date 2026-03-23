package ru.tabel.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.tabel.app.data.model.ShiftEntry
import ru.tabel.app.data.model.ShiftTime
import ru.tabel.app.data.model.ShiftType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun update(
        shifts: List<ShiftEntry>,
        profileName: String,
        shiftTimes: List<ShiftTime> = emptyList()
    ) {
        val today    = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timesMap = shiftTimes.associateBy { it.type }

        val upcoming = shifts
            .filter   { it.date >= today }
            .sortedBy { it.date }
            .take(ShiftWidget.MAX_SHIFTS)

        val prefs = context.getSharedPreferences(ShiftWidget.PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(ShiftWidget.KEY_PROFILE, profileName)
            upcoming.forEachIndexed { i, e ->
                putString(ShiftWidget.kDate(i), e.date)
                putString(ShiftWidget.kType(i), e.type.name)
                
                val showTime = when (e.type) {
                    ShiftType.DAY, ShiftType.NIGHT, ShiftType.HOLIDAY -> true
                    else -> false
                }
                
                val startTime = if (showTime) {
                    e.customStartTime
                        ?: timesMap[e.type]?.startTime
                        ?: when (e.type) {
                            ShiftType.DAY, ShiftType.HOLIDAY -> "08:00"
                            ShiftType.NIGHT -> "20:00"
                            else -> null
                        }
                } else null

                val endTime = if (showTime) {
                    e.customEndTime
                        ?: timesMap[e.type]?.endTime
                        ?: when (e.type) {
                            ShiftType.DAY, ShiftType.HOLIDAY -> "20:00"
                            ShiftType.NIGHT -> "08:00"
                            else -> null
                        }
                } else null

                if (startTime != null) putString(ShiftWidget.kTime(i), startTime)
                else                   remove(ShiftWidget.kTime(i))

                if (endTime != null) putString(ShiftWidget.kEnd(i), endTime)
                else                 remove(ShiftWidget.kEnd(i))
            }
            
            for (i in upcoming.size until ShiftWidget.MAX_SHIFTS) {
                remove(ShiftWidget.kDate(i))
                remove(ShiftWidget.kType(i))
                remove(ShiftWidget.kTime(i))
                remove(ShiftWidget.kEnd(i))
            }
            apply()
        }

        CoroutineScope(Dispatchers.Main).launch {
            runCatching { ShiftWidget().updateAll(context) }
        }
    }
}
