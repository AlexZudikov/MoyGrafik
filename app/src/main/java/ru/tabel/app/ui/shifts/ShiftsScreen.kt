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
import ru.tabel.app.ui.theme.rememberAdaptiveDimens
import java.time.LocalDate
import java.time.YearMonth
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
    val dimens       = rememberAdaptiveDimens()
    val currentMonth by viewModel.currentMonth.collectAsState()

    var searchVisible  by remember { mutableStateOf(false) }

    // Редактирование смены
    var editingEntry   by remember { mutableStateOf<ShiftEntry?>(null) }
    val shiftTimesMap  = remember(shiftTimes) { shiftTimes.associateBy { it.type } }

    // Фильтруем смены только для текущего выбранного месяца
    val monthPrefix = remember(currentMonth) {
        "%04d-%02d".format(currentMonth.year, currentMonth.monthValue)
    }
    val monthShifts = remember(shifts, monthPrefix) {
        shifts.filter { it.date.startsWith(monthPrefix) }
            .sortedBy { it.date }
    }

    // Группируем по месяцу (всегда одна группа - текущий месяц)
    val grouped = remember(monthShifts, currentMonth) {
        if (monthShifts.isEmpty()) {
            emptyList()
        } else {
            val monthKey = "%04d-%02d".format(currentMonth.year, currentMonth.monthValue)
            listOf(monthKey to monthShifts)
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Хедер ─────────────────────────────────────────────
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .padding(bottom = 8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = dimens.horizontalPadding, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { viewModel.previousMonth() }) {
                            Icon(Icons.Rounded.ChevronLeft, null, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            "${MONTHS_FULL[currentMonth.monthValue - 1]} ${currentMonth.year}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.nextMonth() }) {
                            Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(20.dp))
                        }
                        if (currentMonth != YearMonth.now()) {
                            TextButton(onClick = { viewModel.goToCurrentMonth() }) {
                                Text("Сегодня", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Text(
                        if (searchQuery.isNotBlank()) "Найдено: ${shifts.size}"
                        else "Смен в этом месяце: ${filterStats[null] ?: 0}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.labelFontSize),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Сортировка
                    HeaderIconBtn(
                        dimens = dimens,
                        icon = if (sortDesc) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        tint = MaterialTheme.colorScheme.primary
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleSort()
                    }
                    // Поиск
                    HeaderIconBtn(
                        dimens = dimens,
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
                        .padding(horizontal = dimens.horizontalPadding, vertical = 4.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Поиск…", style = MaterialTheme.typography.bodySmall.copy(fontSize = dimens.labelFontSize)) },
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
                contentPadding = PaddingValues(horizontal = dimens.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                // Кнопка "Все"
                item {
                    FilterChipItem(
                        dimens   = dimens,
                        label     = "Все",
                        count     = filterStats[null] ?: 0,
                        color     = MaterialTheme.colorScheme.primary,
                        selected  = activeFilter == null,
                        onClick   = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.setFilter(null) }
                    )
                }
                items(ShiftType.entries.filter { (filterStats[it] ?: 0) > 0 }) { type ->
                    FilterChipItem(
                        dimens   = dimens,
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
            EmptyState(dimens = dimens, query = searchQuery, filter = activeFilter)
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                grouped.forEach { (monthKey, entries) ->
                    val parts = monthKey.split("-")
                    val mIdx  = parts[1].toInt() - 1
                    val year  = parts[0]
                    stickyHeader(key = monthKey) {
                        MonthHeader(
                            dimens = dimens,
                            label = "${MONTHS_FULL[mIdx]} $year",
                            count = entries.size
                        )
                    }
                    items(entries, key = { "${it.date}_${it.profileId}" }) { entry ->
                        ShiftRow(
                            dimens      = dimens,
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

        // ── Легенда цветов ─────────────────────────────────────
        ShiftColorLegend(dimens = dimens)
    }

    // ── Редактирование смены (BottomSheet) ────────────────────
    editingEntry?.let { entry ->
        val defST = shiftTimesMap[entry.type]
        ShiftBottomSheet(
            date             = entry.date,
            currentShift     = entry,
            defaultStartTime = defST?.startTime ?: "08:00",
            defaultEndTime   = defST?.endTime   ?: "20:00",
            onSave = { type, note, start, end, locked ->
                viewModel.saveShift(entry.date, type, note, start, end, locked)
                editingEntry = null
            },
            onDismiss = { editingEntry = null }
        )
    }
}

// ── Компоненты ────────────────────────────────────────────────

@Composable
private fun HeaderIconBtn(
    dimens: ru.tabel.app.ui.theme.AdaptiveDimens,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(dimens.buttonHeight - 8.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(dimens.iconSizeSmall))
    }
}

@Composable
private fun FilterChipItem(
    dimens: ru.tabel.app.ui.theme.AdaptiveDimens,
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
            .padding(horizontal = if (dimens.isCompact) 10.dp else 12.dp, vertical = if (dimens.isCompact) 6.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = dimens.labelFontSize),
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else color)
        Box(
            Modifier.clip(CircleShape)
                .background(if (selected) Color.White.copy(alpha = 0.25f) else color.copy(alpha = 0.15f))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text("$count", fontSize = if (dimens.isCompact) 9.sp else 10.sp, fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else color)
        }
    }
}

@Composable
private fun MonthHeader(
    dimens: ru.tabel.app.ui.theme.AdaptiveDimens,
    label: String, count: Int
) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = dimens.horizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontSize = (dimens.titleFontSize.value - 2).sp),
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground)
        Text("$count смен", style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.labelFontSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShiftRow(
    dimens: ru.tabel.app.ui.theme.AdaptiveDimens,
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
            .padding(horizontal = dimens.horizontalPadding, vertical = 3.dp)
            .clip(MaterialTheme.shapes.large)
            .background(animBg)
            .border(width = if (isToday) 1.5.dp else 0.dp, color = animBorder, shape = MaterialTheme.shapes.large)
            .then(if (onClick != null) Modifier.pressScale(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Цветная полоса слева — для сегодня шире и ярче
        Box(Modifier
            .width(if (isToday) 6.dp else 4.dp)
            .height(if (dimens.isCompact) 56.dp else 68.dp)
            .background(
                when {
                    isToday -> color
                    isPast  -> color.copy(alpha = 0.3f)
                    else    -> color
                }
            ))

        // Дата
        Column(
            Modifier.width(if (dimens.isCompact) 44.dp else 52.dp).padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (date != null) {
                val dow = DOW[date.dayOfWeek.value % 7]
                Text(dow.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = (dimens.labelFontSize.value - 2).sp),
                    color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${date.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = (dimens.titleFontSize.value - 2).sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else if (isToday) color else MaterialTheme.colorScheme.onSurface)
                Text(MONTHS_G[date.monthValue - 1].take(3),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = (dimens.labelFontSize.value - 2).sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isPast) 0.4f else 0.8f))
            }
        }

        // Иконка типа
        Box(
            Modifier.size(if (dimens.isCompact) 34.dp else 40.dp).clip(CircleShape)
                .background(color.copy(alpha = if (isPast) 0.1f else 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.type.icon, fontSize = if (dimens.isCompact) 15.sp else 18.sp)
        }

        // Название + бейдж
        Column(Modifier.weight(1f).padding(horizontal = if (dimens.isCompact) 8.dp else 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.type.label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = dimens.bodyFontSize),
                    fontWeight = FontWeight.Bold,
                    color = if (isPast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface)
                when {
                    isToday    -> Badge(containerColor = color) { Text("СЕГОДНЯ", fontSize = if (dimens.isCompact) 7.sp else 8.sp) }
                    isTomorrow -> Badge(containerColor = MaterialTheme.colorScheme.secondary) { Text("ЗАВТРА", fontSize = if (dimens.isCompact) 7.sp else 8.sp) }
                }
            }
            if (entry.note.isNotEmpty()) {
                Text("📝 ${entry.note}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = dimens.labelFontSize),
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
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = (dimens.labelFontSize.value - 1).sp),
                        color = color.copy(alpha = if (isPast) 0.35f else 0.75f))
                }
            }
        }

        // Прошедшая / будущая
        if (isPast) {
            Icon(Icons.Rounded.CheckCircle, null,
                tint = color.copy(alpha = 0.4f),
                modifier = Modifier.size(dimens.iconSizeSmall).padding(end = if (dimens.isCompact) 8.dp else 12.dp))
        } else {
            Icon(Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = if (dimens.isCompact) 4.dp else 8.dp).size(dimens.iconSizeSmall))
        }
    }
}

@Composable
private fun EmptyState(
    dimens: ru.tabel.app.ui.theme.AdaptiveDimens,
    query: String, filter: ShiftType?
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (query.isNotBlank() || filter != null) "🔍" else "📋", fontSize = if (dimens.isCompact) 40.sp else 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            if (query.isNotBlank()) "По запросу «$query» ничего не найдено"
            else if (filter != null) "Смен типа «${filter.label}» нет"
            else "Нет предстоящих смен",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.titleFontSize),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (query.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Попробуйте другой запрос или уберите фильтр",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = dimens.labelFontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ShiftColorLegend(dimens: ru.tabel.app.ui.theme.AdaptiveDimens) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Цвета смен",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = dimens.labelFontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShiftType.entries.take(4).forEach { type ->
                    LegendItem(type = type, dimens = dimens, modifier = Modifier.weight(1f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShiftType.entries.drop(4).forEach { type ->
                    LegendItem(type = type, dimens = dimens, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendItem(
    type: ShiftType,
    dimens: ru.tabel.app.ui.theme.AdaptiveDimens,
    modifier: Modifier = Modifier
) {
    val typeColor = Color(type.color)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(typeColor)
        )
        Text(
            "${type.icon} ${type.label}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
