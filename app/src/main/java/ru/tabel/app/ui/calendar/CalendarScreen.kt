package ru.tabel.app.ui.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.tabel.app.data.model.*
import ru.tabel.app.ui.components.ShiftBottomSheet
import ru.tabel.app.ui.theme.animatedInt
import ru.tabel.app.ui.theme.pulseScale
import ru.tabel.app.ui.theme.pressScale
import ru.tabel.app.ui.theme.bounceEnter
import ru.tabel.app.ui.theme.morphColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MONTHS   = listOf("Январь","Февраль","Март","Апрель","Май","Июнь","Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")
private val MONTHS_G = listOf("Января","Февраля","Марта","Апреля","Мая","Июня","Июля","Августа","Сентября","Октября","Ноября","Декабря")
private val DOW      = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
private val DOW_SH   = listOf("Вс","Пн","Вт","Ср","Чт","Пт","Сб")

// ── Читаемые аббревиатуры для ячеек ──────────────────────────
private val ShiftType.abbr get() = when (this) {
    ShiftType.DAY      -> "Д"     // Дневная
    ShiftType.NIGHT    -> "Н"     // Ночная
    ShiftType.SLEEP    -> "Отс"   // Отсыпной
    ShiftType.OFF      -> "В"     // Выходной
    ShiftType.HOLIDAY  -> "Пр"    // Праздник
    ShiftType.SICK     -> "Бол"   // Больничный
    ShiftType.VACATION -> "Отп"   // Отпуск
}

// ── Семантические цвета (яркие, не перепутаешь) ──────────────
private val ShiftType.semanticColor get() = when (this) {
    ShiftType.DAY      -> Color(0xFF2563EB)  // насыщенный синий  — рабочий день
    ShiftType.NIGHT    -> Color(0xFF7C3AED)  // фиолетовый        — ночь
    ShiftType.SLEEP    -> Color(0xFF9333EA)  // лиловый           — отсыпной
    ShiftType.OFF      -> Color(0xFF16A34A)  // зелёный           — выходной
    ShiftType.HOLIDAY  -> Color(0xFFDC2626)  // красный           — праздник
    ShiftType.SICK     -> Color(0xFFEA580C)  // оранжевый         — больничный
    ShiftType.VACATION -> Color(0xFF0891B2)  // голубой           — отпуск
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val haptic         = LocalHapticFeedback.current
    val currentMonth   by viewModel.currentMonth.collectAsState()
    val days           by viewModel.calendarDays.collectAsState()
    val selectedDate   by viewModel.selectedDate.collectAsState()
    val selectedShift  by viewModel.selectedShift.collectAsState()
    val stats          by viewModel.monthStats.collectAsState()
    val activeProfile  by viewModel.activeProfile.collectAsState()
    val upcomingShifts by viewModel.upcomingShifts.collectAsState()
    val allProfiles    by viewModel.allProfiles.collectAsState()
    val shiftTimes     by viewModel.shiftTimes.collectAsState()

    var showShiftSheet  by remember { mutableStateOf(false) }
    var editingDate     by remember { mutableStateOf<String?>(null) }
    var showAutofill    by remember { mutableStateOf(false) }
    var showNotifSheet  by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        CalendarHeader(
            month          = currentMonth,
            profileName    = activeProfile?.name,
            profileColor   = activeProfile?.color?.let { Color(it) },
            onNotifClick   = { showNotifSheet = true },
            onProfileClick = { if (allProfiles.size > 1) showProfileMenu = true }
        )

        // ── Быстрый переключатель профиля ─────────────────────
        if (showProfileMenu && allProfiles.size > 1) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showProfileMenu = false }) {
                Card(shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Выбери профиль",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                        allProfiles.forEach { profile ->
                            val isActive = profile.id == activeProfile?.id
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(MaterialTheme.shapes.large)
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.switchProfile(profile.id)
                                        showProfileMenu = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape)
                                        .background(Color(profile.color).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(profile.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(profile.color))
                                }
                                Text(profile.name, modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                                if (isActive) Icon(Icons.Rounded.Check, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // ── Следующая смена ───────────────────────────────
            NextShiftCard(upcoming = upcomingShifts, shiftTimes = shiftTimes)

            // ── Карточка с календарём ─────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(Modifier.padding(12.dp)) {

                    // Навигация месяца
                    var slideDir by remember { mutableIntStateOf(0) } // -1 назад, +1 вперёд
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pressScale {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    slideDir = -1
                                    viewModel.previousMonth()
                                },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.ChevronLeft, null, Modifier.size(20.dp)) }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedContent(
                                targetState = "${MONTHS[currentMonth.monthValue-1]} ${currentMonth.year}",
                                transitionSpec = {
                                    if (slideDir >= 0) {
                                        (slideInHorizontally { it / 2 } + fadeIn()) togetherWith
                                        (slideOutHorizontally { -it / 2 } + fadeOut())
                                    } else {
                                        (slideInHorizontally { -it / 2 } + fadeIn()) togetherWith
                                        (slideOutHorizontally { it / 2 } + fadeOut())
                                    }
                                },
                                label = "monthTitle"
                            ) { title ->
                                Text(title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.goToToday()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Сегодня", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pressScale {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    slideDir = 1
                                    viewModel.nextMonth()
                                },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp)) }
                    }

                    // Заголовки дней недели
                    Row(Modifier.fillMaxWidth()) {
                        DOW.forEachIndexed { i, d ->
                            Text(d,
                                modifier  = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style     = MaterialTheme.typography.labelSmall,
                                color     = if (i >= 5) Color(0xFFDC2626)
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    // Сетка календаря
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                        userScrollEnabled = false,
                        verticalArrangement   = Arrangement.spacedBy(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(days, key = { it.date }) { dayInfo ->
                            DayCell(
                                dayInfo  = dayInfo,
                                selected = dayInfo.date == selectedDate,
                                onTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.selectDate(dayInfo.date)
                                    // Если день текущего месяца — сразу открываем шторку
                                    if (dayInfo.isCurrentMonth) {
                                        editingDate    = dayInfo.date
                                        showShiftSheet = true
                                    }
                                },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    editingDate    = dayInfo.date
                                    showShiftSheet = true
                                }
                            )
                        }
                    }

                    // ── Легенда цветов ────────────────────────
                    Spacer(Modifier.height(12.dp))
                    // ShiftLegend перенесена в Настройки
                }
            }

            // ── Подсказка при пустом графике ──────────────────
            val hasAnyShifts = days.any { it.shift != null && it.isCurrentMonth }
            if (!hasAnyShifts) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Row(Modifier.padding(16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("💡", style = MaterialTheme.typography.titleLarge)
                        Column {
                            Text("График пустой",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text("Нажми на день чтобы добавить смену, или используй «Автозаполнение» ниже",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Статистика месяца ─────────────────────────────
            if (hasAnyShifts) {
                // StatsStrip убран — данные есть во вкладке Статистика
            }

            // ── Детали выбранного дня ─────────────────────────
            selectedDate?.let { date ->
                DayDetailCard(date = date, shift = selectedShift) {
                    editingDate = date
                    showShiftSheet = true
                }
            }

            // ── Кнопки автозаполнения и очистки ──────────────
            var showClearConfirm by remember { mutableStateOf(false) }
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── АВТОЗАПОЛНЕНИЕ — главная кнопка ──────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAutofill = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome, null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Автозаполнение",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                // ── ОЧИСТИТЬ — маленькая иконка-кнопка ───────────
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            MaterialTheme.shapes.large
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showClearConfirm = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DeleteSweep, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showClearConfirm) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { showClearConfirm = false }) {
                    Card(shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Очистить месяц?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold)
                            Text("Все смены за ${MONTHS[currentMonth.monthValue-1]} будут удалены.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showClearConfirm = false },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.large
                                ) { Text("Отмена") }
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.clearMonth()
                                        showClearConfirm = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.large,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error)
                                ) { Text("Удалить", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Шторки ────────────────────────────────────────────────
    if (showShiftSheet && editingDate != null) {
        val curShift   = days.find { it.date == editingDate }?.shift
        val defST      = shiftTimes.find { it.type == (curShift?.type ?: ShiftType.DAY) }
        ShiftBottomSheet(
            date             = editingDate!!,
            currentShift     = curShift,
            defaultStartTime = defST?.startTime ?: "08:00",
            defaultEndTime   = defST?.endTime   ?: "20:00",
            onSave = { type, note, start, end ->
                viewModel.saveShift(editingDate!!, type, note, start, end)
                showShiftSheet = false
            },
            onDismiss = { showShiftSheet = false }
        )
    }

    if (showAutofill) {
        AutofillBottomSheet(
            currentMonth  = currentMonth,
            onConfirm     = { pattern, startDate, startIndex ->
                viewModel.autofillMonth(pattern, startDate, startIndex)
                showAutofill = false
            },
            onConfirmYear = { pattern, startDate, startIndex ->
                viewModel.autofillYear(pattern, startDate, startIndex)
                showAutofill = false
            },
            onDismiss = { showAutofill = false }
        )
    }

    if (showNotifSheet) {
        ru.tabel.app.ui.settings.NotifSheet(onDismiss = { showNotifSheet = false })
    }
}

// ── Хедер ─────────────────────────────────────────────────────
@Composable
private fun CalendarHeader(
    month: YearMonth,
    profileName: String?,
    profileColor: Color?,
    onNotifClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onProfileClick)
                .padding(4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Цветная точка профиля
            if (profileColor != null && profileName != null) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(profileColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profileName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = profileColor)
                }
            }
            Column {
                Text("Мой График",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${MONTHS[month.monthValue-1]} ${month.year}" +
                            if (profileName != null) " · $profileName" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profileName != null && true) {
                        Icon(Icons.Rounded.ExpandMore, null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        HeaderIconBtn(Icons.Rounded.Notifications) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onNotifClick()
        }
    }
}

@Composable
private fun HeaderIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
    }
}

// ── Полоска 7 ближайших дней ──────────────────────────────────
@Composable
private fun WeekStrip(
    days: List<DayInfo>,
    selectedDate: String?,
    onDateClick: (String) -> Unit
) {
    val today = remember { LocalDate.now() }
    LazyRow(
        modifier       = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(7, key = { it }) { i ->
            val date    = today.plusDays(i.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val shift   = days.find { it.date == dateStr }?.shift
            val color   = shift?.type?.semanticColor
            val isToday = i == 0
            val isSel   = dateStr == selectedDate

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        width = if (isSel) 2.dp else if (color != null) 1.5.dp else 0.dp,
                        color = if (isSel) MaterialTheme.colorScheme.primary
                                else color ?: Color.Transparent,
                        shape = MaterialTheme.shapes.medium
                    )
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else Color.Transparent
                    )
                    .clickable { onDateClick(dateStr) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(DOW_SH[date.dayOfWeek.value % 7],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp)
                Text(date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color ?: if (isToday) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp)
                // Аббревиатура типа смены
                if (shift != null) {
                    Text(shift.type.abbr,
                        style = MaterialTheme.typography.labelSmall,
                        color = color ?: Color.Transparent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp)
                } else {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

// ── Статистика месяца ─────────────────────────────────────────
@Composable
private fun StatsStrip(stats: MonthStats) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(stats.workShifts.toString(),           "СМЕН",     Color(0xFF2563EB), Modifier.weight(1f))
        StatChip(stats.totalHours.toInt().toString(),   "ЧАСОВ",    Color(0xFF7C3AED), Modifier.weight(1f))
        StatChip(stats.offDays.toString(),              "ВЫХОДНЫХ", Color(0xFF16A34A), Modifier.weight(1f))
        StatChip(stats.estimatedSalary.toInt().toString(), "₽",     Color(0xFFEA580C), Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(value: String, label: String, color: Color, modifier: Modifier) {
    val numValue = value.toIntOrNull()
    val animated = if (numValue != null) animatedInt(numValue) else null
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(animated?.toString() ?: value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color)
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

// ── Следующая смена ───────────────────────────────────────────
@Composable
private fun NextShiftCard(upcoming: List<ShiftEntry>, shiftTimes: List<ShiftTime>) {
    val now by produceState(initialValue = java.time.LocalDateTime.now()) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            value = java.time.LocalDateTime.now()
        }
    }
    val today    = now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val tomorrow = now.toLocalDate().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
    val next = remember(upcoming) { upcoming.firstOrNull() } ?: return
    val color = next.type.semanticColor
    val date  = LocalDate.parse(next.date)
    val label = when (next.date) {
        today    -> "Сегодня"
        tomorrow -> "Завтра"
        else     -> "${date.dayOfMonth} ${MONTHS_G[date.monthValue-1]}"
    }

    val countdown = remember(now, next.date, shiftTimes) {
        val st = shiftTimes.find { it.type == next.type }
        val startTime = next.customStartTime
            ?: st?.startTime
            ?: when (next.type) {
                ShiftType.DAY -> "08:00"; ShiftType.NIGHT -> "20:00"; else -> null
            }
        if (startTime == null) return@remember null
        val parts = startTime.split(":").map { it.toIntOrNull() ?: 0 }
        val shiftStart = date.atTime(parts[0], parts[1])
        val diff = java.time.Duration.between(now, shiftStart)
        if (diff.isNegative) return@remember null
        val hours = diff.toHours(); val mins = diff.toMinutes() % 60
        when { hours > 0 -> "через ${hours}ч ${mins}мин"; mins > 0 -> "через ${mins} минут"; else -> "скоро начало" }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape    = MaterialTheme.shapes.extraLarge
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("СЛЕДУЮЩАЯ СМЕНА",
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f),
                    letterSpacing = 1.sp)
                // Если "Сегодня"/"Завтра" — показываем и точную дату рядом
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold)
                    if (next.date == today || next.date == tomorrow) {
                        Text(
                            "${date.dayOfMonth} ${MONTHS_G[date.monthValue-1]}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(color)
                    )
                    Text(next.type.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        fontWeight = FontWeight.Medium)
                }
                if (countdown != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Timer, null,
                            modifier = Modifier.size(12.dp),
                            tint = color.copy(alpha = 0.8f))
                        Text(countdown,
                            style = MaterialTheme.typography.labelMedium,
                            color = color.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
            // Большой значок смены
            Box(
                Modifier.size(56.dp).clip(MaterialTheme.shapes.large)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(next.type.icon, fontSize = 26.sp)
            }
        }
    }
}

// ── Ячейка дня — НОВЫЙ ДИЗАЙН с аббревиатурой ────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    dayInfo: DayInfo,
    selected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val shift     = dayInfo.shift
    val typeColor = shift?.type?.semanticColor
    val isToday   = dayInfo.isToday

    // Фон круга — сегодня без смены тоже прозрачный, кольцо снаружи
    val bg = when {
        !dayInfo.isCurrentMonth       -> Color.Transparent
        selected && typeColor != null -> typeColor
        selected                      -> MaterialTheme.colorScheme.primary
        typeColor != null             -> typeColor
        else                          -> Color.Transparent
    }

    // Цвет числа
    val numColor = when {
        !dayInfo.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
        typeColor != null       -> Color.White
        selected                -> Color.White
        isToday                 -> MaterialTheme.colorScheme.primary
        else                    -> MaterialTheme.colorScheme.onSurface
    }

    val pulse  = if (isToday && !selected) pulseScale() else 1f
    val animBg by animateColorAsState(bg, tween(180), label = "cellBg")

    val ringColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f)
    val glowColor = MaterialTheme.colorScheme.primary

    // Внешний Box — квадратная ячейка сетки
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        // Свечение (только сегодня) — три слоя мягкого ореола
        if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize(1.1f)
                    .scale(pulse)
                    .drawBehind {
                        val r = size.minDimension / 2f
                        drawCircle(color = glowColor.copy(alpha = 0.22f), radius = r * 1.40f)
                        drawCircle(color = glowColor.copy(alpha = 0.16f), radius = r * 1.20f)
                        drawCircle(color = glowColor.copy(alpha = 0.10f), radius = r * 1.05f)
                    }
            )
        }

        // Внешнее контрастное кольцо — толще и контрастнее
        if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.97f)
                    .scale(pulse)
                    .border(width = 3.5.dp, color = ringColor, shape = CircleShape)
            )
        }

        // Цветной круг смены (чуть меньше кольца)
        Box(
            modifier = Modifier
                .fillMaxSize(0.80f)
                .scale(pulse)
                .clip(CircleShape)
                .background(animBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                dayInfo.dayOfMonth.toString(),
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                color      = numColor,
                fontSize   = 13.sp
            )
        }

        // Маленькая метка типа смены — снизу под кругом (только для текущего месяца)
        if (shift != null && dayInfo.isCurrentMonth) {
            Text(
                shift.type.abbr,
                modifier   = Modifier.align(Alignment.BottomCenter),
                fontSize   = 7.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = typeColor?.copy(alpha = if (selected) 0.6f else 0.9f) ?: Color.Transparent,
                letterSpacing = 0.sp
            )
        }
    }
}

// ── Легенда под сеткой ────────────────────────────────────────
@Composable
private fun ShiftLegend() {
    Column(
        Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("ОБОЗНАЧЕНИЯ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp)
        // 2 колонки
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShiftType.entries.take(4).forEach { type ->
                    LegendItem(type)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShiftType.entries.drop(4).forEach { type ->
                    LegendItem(type)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(type: ShiftType) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Цветная плашка с аббревиатурой
        Box(
            Modifier.size(width = 26.dp, height = 20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(type.semanticColor.copy(alpha = 0.15f))
                .border(1.dp, type.semanticColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(type.abbr,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = type.semanticColor,
                fontSize = 8.sp)
        }
        Text(type.icon, fontSize = 12.sp)
        Text(type.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp)
    }
}

// ── Детали выбранного дня ─────────────────────────────────────
@Composable
private fun DayDetailCard(date: String, shift: ShiftEntry?, onEdit: () -> Unit) {
    val dateObj = remember(date) {
        runCatching { LocalDate.parse(date) }.getOrNull()
    } ?: return
    val color = shift?.type?.semanticColor ?: MaterialTheme.colorScheme.outline

    AnimatedContent(
        targetState = date,
        transitionSpec = {
            (fadeIn(tween(250)) + slideInVertically(
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                initialOffsetY = { it / 3 }
            )).togetherWith(fadeOut(tween(150)))
        },
        label = "dayDetail"
    ) { targetDate ->
        val tDateObj = remember(targetDate) {
            runCatching { LocalDate.parse(targetDate) }.getOrNull()
        } ?: return@AnimatedContent
        val animColor by animateColorAsState(color, tween(300), label = "detailColor")

        Card(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = animColor.copy(alpha = 0.08f)),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Цветная полоса слева
                Box(Modifier.width(4.dp).height(80.dp).background(animColor))

                // Иконка смены
                if (shift != null) {
                    Box(
                        Modifier.padding(start = 14.dp).size(44.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(animColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(shift.type.icon, fontSize = 20.sp)
                    }
                }

                Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 14.dp)) {
                    Text(
                        "${tDateObj.dayOfMonth} ${MONTHS_G[tDateObj.monthValue - 1]}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (shift != null) {
                        Text(shift.type.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = animColor,
                            fontWeight = FontWeight.Medium)
                        if (shift.note.isNotEmpty())
                            Text("📝 ${shift.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Нет смены",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                FilledTonalButton(
                    onClick  = onEdit,
                    modifier = Modifier.padding(end = 12.dp),
                    shape    = MaterialTheme.shapes.large,
                    colors   = ButtonDefaults.filledTonalButtonColors(
                        containerColor = animColor.copy(alpha = 0.15f),
                        contentColor   = animColor)
                ) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Изменить", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
