package ru.tabel.app.ui.shifts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.tabel.app.data.model.ShiftEntry
import ru.tabel.app.data.model.ShiftType
import ru.tabel.app.data.model.ShiftTime
import ru.tabel.app.ui.components.ShiftBottomSheet
import ru.tabel.app.ui.theme.pressScale
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val MONTHS_FULL = listOf("Январь","Февраль","Март","Апрель","Май","Июнь",
    "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")
private val MONTHS_G    = listOf("января","февраля","марта","апреля","мая","июня",
    "июля","августа","сентября","октября","ноября","декабря")
private val DOW         = listOf("вс","пн","вт","ср","чт","пт","сб")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShiftsScreen(viewModel: ShiftsViewModel = hiltViewModel()) {
    val haptic        = LocalHapticFeedback.current
    val focusMgr      = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val shifts       by viewModel.filteredShifts.collectAsState()
    val searchQuery  by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val sortDesc     by viewModel.sortDesc.collectAsState()
    val filterStats  by viewModel.filterStats.collectAsState()
    val shiftTimes   by viewModel.shiftTimes.collectAsState()

    var searchVisible  by remember { mutableStateOf(false) }

    // Редактирование смены
    var editingEntry   by remember { mutableStateOf<ShiftEntry?>(null) }
    val shiftTimesMap  = remember(shiftTimes) { shiftTimes.associateBy { it.type } }

    // Группируем по месяцу
    val grouped = remember(shifts) {
        shifts.groupBy { it.date.substring(0, 7) }
            .entries.let { if (sortDesc) it.sortedByDescending { e -> e.key } else it.sortedBy { e -> e.key } }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Хедер ─────────────────────────────────────────────
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .padding(bottom = 8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Мои смены", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (searchQuery.isNotBlank()) "Найдено: ${shifts.size}"
                        else "Всего смен: ${filterStats[null] ?: 0}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Сортировка
                    HeaderIconBtn(
                        icon = if (sortDesc) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        tint = MaterialTheme.colorScheme.primary
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleSort()
                    }
                    // Поиск
                    HeaderIconBtn(
                        icon = Icons.Rounded.Search,
                        tint = if (searchVisible) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        searchVisible = !searchVisible
                        if (!searchVisible) { viewModel.setSearch(""); focusMgr.clearFocus() }
                    }
                }
            }

            // ── Строка поиска (анимированная) ─────────────────
            AnimatedVisibility(
                visible = searchVisible,
                enter = fadeIn() + expandVertically(),
                exit  = fadeOut() + shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearch(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Поиск по типу, дате, заметке…") },
                    leadingIcon  = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearch("") }) {
                                Icon(Icons.Rounded.Close, null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusMgr.clearFocus() })
                )
                LaunchedEffect(searchVisible) {
                    if (searchVisible) focusRequester.requestFocus()
                }
            }

            // ── Фильтры по типу ───────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                // Кнопка "Все"
                item {
                    FilterChipItem(
                        label     = "Все",
                        count     = filterStats[null] ?: 0,
                        color     = MaterialTheme.colorScheme.primary,
                        selected  = activeFilter == null,
                        onClick   = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.setFilter(null) }
                    )
                }
                items(ShiftType.entries.filter { (filterStats[it] ?: 0) > 0 }) { type ->
                    FilterChipItem(
                        label    = type.label,
                        count    = filterStats[type] ?: 0,
                        color    = Color(type.color),
                        selected = activeFilter == type,
                        onClick  = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.setFilter(type) }
                    )
                }
            }
        }

        // ── Список смен ───────────────────────────────────────
        if (shifts.isEmpty()) {
            EmptyState(searchQuery, activeFilter)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                grouped.forEach { (monthKey, entries) ->
                    val parts = monthKey.split("-")
                    val mIdx  = parts[1].toInt() - 1
                    val year  = parts[0]
                    stickyHeader(key = monthKey) {
                        MonthHeader(
                            label = "${MONTHS_FULL[mIdx]} $year",
                            count = entries.size
                        )
                    }
                    items(entries, key = { "${it.date}_${it.profileId}" }) { entry ->
                        ShiftRow(
                            entry      = entry,
                            shiftTimes = shiftTimes,
                            onClick    = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                editingEntry = entry
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // ── Редактирование смены (BottomSheet) ────────────────────
    editingEntry?.let { entry ->
        val defST = shiftTimesMap[entry.type]
        ShiftBottomSheet(
            date             = entry.date,
            currentShift     = entry,
            defaultStartTime = defST?.startTime ?: "08:00",
            defaultEndTime   = defST?.endTime   ?: "20:00",
            onSave = { type, note, start, end ->
                viewModel.saveShift(entry.date, type, note, start, end)
                editingEntry = null
            },
            onDismiss = { editingEntry = null }
        )
    }
}

// ── Компоненты ────────────────────────────────────────────────

@Composable
private fun HeaderIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FilterChipItem(
    label: String, count: Int, color: Color, selected: Boolean, onClick: () -> Unit
) {
    val animBg     by animateColorAsState(
        if (selected) color else MaterialTheme.colorScheme.surfaceVariant,
        tween(200), label = "chipBg"
    )
    val animBorder by animateColorAsState(
        if (selected) Color.Transparent else color.copy(alpha = 0.4f),
        tween(200), label = "chipBorder"
    )
    val animBorderW by animateDpAsState(
        if (selected) 0.dp else 1.dp, tween(200), label = "chipBorderW"
    )
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(animBg)
            .border(width = animBorderW, color = animBorder, shape = MaterialTheme.shapes.extraLarge)
            .pressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else color)
        Box(
            Modifier.clip(CircleShape)
                .background(if (selected) Color.White.copy(alpha = 0.25f) else color.copy(alpha = 0.15f))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else color)
        }
    }
}

@Composable
private fun MonthHeader(label: String, count: Int) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground)
        Text("$count смен", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShiftRow(
    entry: ShiftEntry,
    shiftTimes: List<ru.tabel.app.data.model.ShiftTime>,
    onClick: (() -> Unit)? = null
) {
    val color = Color(entry.type.color)
    val date  = remember(entry.date) { runCatching { LocalDate.parse(entry.date) }.getOrNull() }
    val today = remember { LocalDate.now() }
    val isToday    = date == today
    val isTomorrow = date == today.plusDays(1)
    val isPast     = date?.isBefore(today) == true

    val animBg by animateColorAsState(
        if (isToday) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        tween(250), label = "shiftRowBg"
    )
    val animBorder by animateColorAsState(
        if (isToday) color.copy(alpha = 0.6f) else Color.Transparent,
        tween(250), label = "shiftRowBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(MaterialTheme.shapes.large)
            .background(animBg)
            .border(width = if (isToday) 1.5.dp else 0.dp, color = animBorder, shape = MaterialTheme.shapes.large)
            .then(if (onClick != null) Modifier.pressScale(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Цветная полоса слева — для сегодня шире и ярче
        Box(Modifier
            .width(if (isToday) 6.dp else 4.dp)
            .height(68.dp)
            .background(
                when {
                    isToday -> color
                    isPast  -> color.copy(alpha = 0.3f)
                    else    -> color
                }
            ))

        // Дата
        Column(
            Modifier.width(52.dp).padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (date != null) {
                val dow = DOW[date.dayOfWeek.value % 7]
                Text(dow.uppercase(), style = MaterialTheme.typography.labelSmall,
                    color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp)
                Text("${date.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else if (isToday) color else MaterialTheme.colorScheme.onSurface)
                Text(MONTHS_G[date.monthValue - 1].take(3),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isPast) 0.4f else 0.8f))
            }
        }

        // Иконка типа
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(color.copy(alpha = if (isPast) 0.1f else 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.type.icon, fontSize = 18.sp)
        }

        // Название + бейдж
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.type.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface)
                when {
                    isToday    -> Badge(containerColor = color) { Text("СЕГОДНЯ", fontSize = 8.sp) }
                    isTomorrow -> Badge(containerColor = MaterialTheme.colorScheme.secondary) { Text("ЗАВТРА", fontSize = 8.sp) }
                }
            }
            if (entry.note.isNotEmpty()) {
                Text("📝 ${entry.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1)
            }
            // Время смены
            if (entry.type.hasTime) {
                val st = shiftTimes.find { it.type == entry.type }
                val start = st?.startTime ?: when (entry.type) {
                    ru.tabel.app.data.model.ShiftType.DAY   -> "08:00"
                    ru.tabel.app.data.model.ShiftType.NIGHT -> "20:00"
                    else -> null
                }
                val end = st?.endTime ?: when (entry.type) {
                    ru.tabel.app.data.model.ShiftType.DAY   -> "20:00"
                    ru.tabel.app.data.model.ShiftType.NIGHT -> "08:00"
                    else -> null
                }
                if (start != null && end != null) {
                    Text("⏰ $start — $end",
                        style = MaterialTheme.typography.bodySmall,
                        color = color.copy(alpha = if (isPast) 0.35f else 0.75f),
                        fontSize = 11.sp)
                }
            }
        }

        // Прошедшая / будущая
        if (isPast) {
            Icon(Icons.Rounded.CheckCircle, null,
                tint = color.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp).padding(end = 12.dp))
        } else {
            Icon(Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp).size(18.dp))
        }
    }
}

@Composable
private fun EmptyState(query: String, filter: ShiftType?) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (query.isNotBlank() || filter != null) "🔍" else "📋", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            if (query.isNotBlank()) "По запросу «$query» ничего не найдено"
            else if (filter != null) "Смен типа «${filter.label}» нет"
            else "Нет предстоящих смен",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (query.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Попробуйте другой запрос или уберите фильтр",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
