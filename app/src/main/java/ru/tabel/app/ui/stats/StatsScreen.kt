package ru.tabel.app.ui.stats

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.tabel.app.ui.settings.NotifSheet
import ru.tabel.app.ui.theme.animatedInt
import ru.tabel.app.ui.theme.animatedFraction
import ru.tabel.app.ui.theme.SlideCounter
import ru.tabel.app.ui.theme.morphColor
import java.time.YearMonth
import ru.tabel.app.ui.stats.ExportDialog

private val MONTHS_SHORT = listOf("Янв","Фев","Мар","Апр","Май","Июн",
    "Июл","Авг","Сен","Окт","Ноя","Дек")
private val MONTHS_FULL  = listOf("Январь","Февраль","Март","Апрель","Май","Июнь",
    "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val haptic        = LocalHapticFeedback.current
    val context       = LocalContext.current
    val stats         by viewModel.stats.collectAsState()
    val history       by viewModel.monthHistory.collectAsState()
    val yearStats     by viewModel.yearStats.collectAsState()
    val typeCounts    by viewModel.typeCounts.collectAsState()
    val settings      by viewModel.settings.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val monthShifts   by viewModel.monthShifts.collectAsState()

    val isCurrentMonth = selectedMonth == YearMonth.now()

    var showNotif  by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    val allShifts  by viewModel.allShiftsForExport.collectAsState()
    val profileName by viewModel.profileName.collectAsState()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Хедер ─────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Статистика",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold)
                Text(
                    "${MONTHS_FULL[selectedMonth.monthValue - 1]} ${selectedMonth.year}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Кнопка экспорта
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showExport = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.FileDownload, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
                // Кнопка уведомлений
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showNotif = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Notifications, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── Навигатор месяца ──────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.previousMonth()
            }) {
                Icon(Icons.Rounded.ChevronLeft, "Предыдущий месяц")
            }
            AnimatedContent(
                targetState = selectedMonth,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it / 2 } + fadeIn() togetherWith
                        slideOutHorizontally { -it / 2 } + fadeOut()
                    else
                        slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                        slideOutHorizontally { it / 2 } + fadeOut()
                },
                label = "month_nav"
            ) { month ->
                Text(
                    "${MONTHS_FULL[month.monthValue - 1]} ${month.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isCurrentMonth) {
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Сбросить в текущий — через цикл nextMonth
                        val now = YearMonth.now()
                        var cur = selectedMonth
                        while (cur < now) { viewModel.nextMonth(); cur = cur.plusMonths(1) }
                        while (cur > now) { viewModel.previousMonth(); cur = cur.minusMonths(1) }
                    }) { Text("Сейчас", style = MaterialTheme.typography.labelMedium) }
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.nextMonth()
                    },
                    enabled = !isCurrentMonth
                ) {
                    Icon(Icons.Rounded.ChevronRight, "Следующий месяц",
                        tint = if (isCurrentMonth)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // ── СМЕНЫ ─────────────────────────────────────────
            SectionTitle("СМЕНЫ", Icons.Rounded.BarChart)

            AnimatedContent(targetState = stats, label = "stats_anim") { s ->
                Column {
                    Row(Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigCard(s.workShifts.toString(),   "Рабочих смен", Color(0xFF4F6EF7), Modifier.weight(1f))
                        BigCard(s.totalHours.toInt().toString(), "Раб. часов",  Color(0xFF8B5CF6), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigCard(s.offDays.toString(),      "Выходных",  Color(0xFF22c55e), Modifier.weight(1f))
                        BigCard(s.nightShifts.toString(),  "Ночных",    Color(0xFFa855f7), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))

                    // ── Норма и переработки ───────────────────
                    if (s.totalHours > 0f) {
                        Card(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    s.overtimeHours > 0  -> Color(0xFFEA580C).copy(alpha = 0.1f)
                                    s.overtimeHours < 0  -> Color(0xFFEAB308).copy(alpha = 0.1f)
                                    else                 -> MaterialTheme.colorScheme.surfaceVariant
                                }),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("НОРМА / ФАКТ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 1.sp)
                                        Text(
                                            "${s.totalHours.toInt()} ч  /  ${s.normHours.toInt()} ч",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    val overColor = when {
                                        s.overtimeHours > 0 -> Color(0xFFEA580C)
                                        s.overtimeHours < 0 -> Color(0xFFEAB308)
                                        else                -> Color(0xFF22c55e)
                                    }
                                    val overLabel = when {
                                        s.overtimeHours > 0 -> "+${s.overtimeHours.toInt()} ч переработка"
                                        s.overtimeHours < 0 -> "${s.overtimeHours.toInt()} ч недоработка"
                                        else                -> "✓ норма выполнена"
                                    }
                                    Text(overLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = overColor)
                                }
                                Spacer(Modifier.height(8.dp))
                                // Прогресс-бар с анимацией появления
                                val rawProgress = (s.totalHours / s.normHours).coerceIn(0f, 1.5f)
                                val normFrac    = (s.normHours / (s.normHours * 1.5f)).coerceIn(0f, 1f)
                                val progress    = animatedFraction(rawProgress / 1.5f, durationMs = 900, delayMs = 150)
                                val barColor    = morphColor(when {
                                    s.overtimeHours > 0 -> Color(0xFFEA580C)
                                    s.overtimeHours < 0 -> Color(0xFFEAB308)
                                    else                -> Color(0xFF22c55e)
                                })
                                Box(
                                    Modifier.fillMaxWidth().height(6.dp)
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxHeight()
                                            .background(barColor)
                                    )
                                    // Маркер нормы
                                    Box(
                                        Modifier
                                            .fillMaxWidth(normFrac)
                                            .fillMaxHeight()
                                            .wrapContentWidth(Alignment.End)
                                    ) {
                                        Box(
                                            Modifier.width(2.dp).fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── ЗАРПЛАТА ──────────────────────────────────────
            SectionTitle("ЗАРПЛАТА", Icons.Rounded.CurrencyRuble)

            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF22c55e).copy(alpha = 0.15f)),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("ЗАРПЛАТА",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF22c55e).copy(alpha = 0.7f),
                        letterSpacing = 1.sp)
                    if (settings.hourlyRate > 0) {
                        Text(
                            "%,d ₽".format(stats.estimatedSalary.toInt()).replace(",", " "),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold)
                        Text("${stats.totalHours.toInt()} ч × ${settings.hourlyRate.toInt()}₽/ч",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("0 ₽",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold)
                        Text("Укажите ставку в Настройках",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── ПО ТИПАМ ──────────────────────────────────────
            SectionTitle("ПО ТИПАМ", Icons.Rounded.PieChart)

            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val totalForBar = stats.totalShifts.coerceAtLeast(1)
                    typeCounts.filter { it.second > 0 }.forEach { (type, count) ->
                        TypeBar(
                            label = type.label, icon = type.icon,
                            color = Color(type.color), count = count, total = totalForBar
                        )
                    }
                    if (typeCounts.all { it.second == 0 }) {
                        Text("Нет данных за выбранный месяц",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp))
                    }
                }
            }

            // ── ИТОГО ЗА ГОД ──────────────────────────────────
            SectionTitle("ИТОГО ЗА ГОД", Icons.Rounded.CalendarViewMonth)
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    YearStatChip("${yearStats.workShifts}", "смен", Color(0xFF4F6EF7), Modifier.weight(1f))
                    YearStatChip("${yearStats.totalHours.toInt()}", "часов", Color(0xFF8B5CF6), Modifier.weight(1f))
                    if (settings.hourlyRate > 0) {
                        YearStatChip(
                            "%,d".format(yearStats.estimatedSalary.toInt()).replace(",", " "),
                            "₽ итого", Color(0xFF22c55e), Modifier.weight(1f)
                        )
                    } else {
                        YearStatChip("${yearStats.offDays}", "выходных", Color(0xFF22c55e), Modifier.weight(1f))
                    }
                }
            }

            // ── ИСТОРИЯ ───────────────────────────────────────
            SectionTitle("ИСТОРИЯ", Icons.Rounded.History)

            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (history.isEmpty()) {
                        Text("Нет данных",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp))
                    } else {
                        history.take(6).forEach { (monthKey, data) ->
                            val parts = monthKey.split("-")
                            val label = "${MONTHS_SHORT[parts[1].toInt()-1]} ${parts[0]}"
                            HistoryItem(
                                monthLabel = label,
                                shifts     = data.first,
                                hours      = data.second,
                                earn       = data.third,
                                showEarn   = settings.hourlyRate > 0,
                                isSelected = monthKey == "%04d-%02d".format(
                                    selectedMonth.year, selectedMonth.monthValue)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showExport) {
        ExportDialog(
            context       = context,
            allShifts     = allShifts,
            profileName   = profileName,
            selectedMonth = selectedMonth,
            onDismiss     = { showExport = false }
        )
    }

    if (showNotif) {
        NotifSheet(onDismiss = { showNotif = false })
    }
}

// ── Компоненты ────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp)
    }
}

@Composable
private fun BigCard(value: String, label: String, color: Color, modifier: Modifier) {
    val numValue = value.toIntOrNull()
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(16.dp)) {
            if (numValue != null) {
                SlideCounter(count = numValue) { v ->
                    Text(v.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold, color = color)
                }
            } else {
                Text(value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold, color = color)
            }
            Text(label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TypeBar(label: String, icon: String, color: Color, count: Int, total: Int) {
    val pct by animateFloatAsState(
        targetValue   = if (total > 0) count.toFloat() / total else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "barFill"
    )
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 16.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(pct.coerceIn(0f, 1f)).fillMaxHeight().background(color))
            }
        }
        Text(count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryItem(
    monthLabel: String, shifts: Int, hours: Float, earn: Float,
    showEarn: Boolean, isSelected: Boolean
) {
    val bg = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Box(Modifier.width(3.dp).height(16.dp)
                .background(MaterialTheme.colorScheme.primary,
                    MaterialTheme.shapes.small))
            Spacer(Modifier.width(8.dp))
        }
        Text(monthLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📅 $shifts", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("⏰ ${hours.toInt()}ч", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showEarn)
                Text("%,d₽".format(earn.toInt()).replace(",", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF22c55e), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun YearStatChip(value: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color)
        Text(label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
