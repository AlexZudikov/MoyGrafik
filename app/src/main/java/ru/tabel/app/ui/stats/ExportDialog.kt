package ru.tabel.app.ui.stats

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.tabel.app.data.model.ShiftEntry
import java.time.YearMonth

// ── Диалог экспорта ───────────────────────────────────────────
@Composable
fun ExportDialog(
    context:      Context,
    allShifts:    List<ShiftEntry>,
    profileName:  String,
    selectedMonth: java.time.YearMonth = java.time.YearMonth.now(),
    onDismiss:    () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Период
    val months = buildList {
        val now = YearMonth.now()
        repeat(12) { i -> add(now.minusMonths(i.toLong())) }
    }
    val monthNames = listOf("Январь","Февраль","Март","Апрель","Май","Июнь",
        "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")

    var selectedPeriod by remember { mutableStateOf<ExportPeriod>(ExportPeriod.CurrentMonth) }
    var customFrom     by remember { mutableStateOf(YearMonth.now()) }
    var customTo       by remember { mutableStateOf(YearMonth.now()) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var isExporting    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            Box(
                Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.FileDownload, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp))
            }
        },
        title = { Text("Экспорт данных", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── Период ────────────────────────────────────
                Text("Период", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PeriodOption("Текущий месяц", Icons.Rounded.CalendarToday,
                        selectedPeriod == ExportPeriod.CurrentMonth,
                        { haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          selectedPeriod = ExportPeriod.CurrentMonth })
                    PeriodOption("Последние 3 месяца", Icons.Rounded.CalendarMonth,
                        selectedPeriod == ExportPeriod.Last3Months,
                        { haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          selectedPeriod = ExportPeriod.Last3Months })
                    PeriodOption("Последние 6 месяцев", Icons.Rounded.DateRange,
                        selectedPeriod == ExportPeriod.Last6Months,
                        { haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          selectedPeriod = ExportPeriod.Last6Months })
                    PeriodOption("Весь год", Icons.Rounded.CalendarViewMonth,
                        selectedPeriod == ExportPeriod.FullYear,
                        { haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          selectedPeriod = ExportPeriod.FullYear })
                }

                HorizontalDivider(thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant)

                // ── Формат ────────────────────────────────────
                Text("Формат файла", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip("PDF", Icons.Rounded.PictureAsPdf, Color(0xFFDC2626),
                        selectedFormat == ExportFormat.PDF, Modifier.weight(1f),
                        { haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          selectedFormat = ExportFormat.PDF })
                    FormatChip("Текст", Icons.Rounded.TextSnippet, Color(0xFF2563EB),
                        selectedFormat == ExportFormat.Text, Modifier.weight(1f),
                        { haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          selectedFormat = ExportFormat.Text })
                }

                // Инфо о количестве смен
                val filteredShifts = filterShifts(allShifts, selectedPeriod)
                Text(
                    "Будет экспортировано: ${filteredShifts.size} смен",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExporting = true
                        val shifts = filterShifts(allShifts, selectedPeriod)
                        val period = getPeriodLabel(selectedPeriod)
                        when (selectedFormat) {
                            ExportFormat.PDF -> {
                                runCatching {
                                    val uri = PdfExporter.buildPdf(
                                        context, shifts, profileName,
                                        selectedMonth.year, selectedMonth.monthValue
                                    )
                                    if (uri != null) {
                                        // Предлагаем сохранить или отправить
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            putExtra(android.content.Intent.EXTRA_SUBJECT,
                                                "График смен — $profileName")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            android.content.Intent.createChooser(intent, "Сохранить или отправить PDF")
                                        )
                                    } else {
                                        Toast.makeText(context, "Ошибка создания PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }.onFailure {
                                    Toast.makeText(context, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            ExportFormat.Text -> {
                                val text = buildShareText(shifts, profileName, period)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    putExtra(Intent.EXTRA_SUBJECT, "График смен — $profileName — $period")
                                }
                                runCatching {
                                    context.startActivity(
                                        Intent.createChooser(intent, "Поделиться графиком")
                                    )
                                }
                            }
                        }
                        isExporting = false
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(Modifier.size(16.dp),
                            color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Rounded.FileDownload, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Экспортировать", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDismiss, modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("Отмена") }
            }
        },
        dismissButton = {}
    )
}

// ── Вспомогалки ───────────────────────────────────────────────
enum class ExportPeriod { CurrentMonth, Last3Months, Last6Months, FullYear }
enum class ExportFormat  { PDF, Text }

private fun filterShifts(shifts: List<ShiftEntry>, period: ExportPeriod): List<ShiftEntry> {
    val now = YearMonth.now()
    val from = when (period) {
        ExportPeriod.CurrentMonth  -> now
        ExportPeriod.Last3Months   -> now.minusMonths(2)
        ExportPeriod.Last6Months   -> now.minusMonths(5)
        ExportPeriod.FullYear      -> now.minusMonths(11)
    }
    val fromStr = "%04d-%02d".format(from.year, from.monthValue)
    val toStr   = "%04d-%02d".format(now.year, now.monthValue)
    return shifts.filter { it.date.substring(0, 7) in fromStr..toStr }
        .sortedBy { it.date }
}

private fun getPeriodLabel(period: ExportPeriod) = when (period) {
    ExportPeriod.CurrentMonth -> "Текущий месяц"
    ExportPeriod.Last3Months  -> "3 месяца"
    ExportPeriod.Last6Months  -> "6 месяцев"
    ExportPeriod.FullYear     -> "Год"
}

private fun buildShareText(shifts: List<ShiftEntry>, profile: String, period: String): String {
    val sb = StringBuilder()
    sb.appendLine("📅 График смен — $profile")
    sb.appendLine("Период: $period")
    sb.appendLine("─────────────────")
    if (shifts.isEmpty()) {
        sb.appendLine("Нет смен за выбранный период")
    } else {
        var currentMonth = ""
        shifts.forEach { entry ->
            val month = entry.date.substring(0, 7)
            if (month != currentMonth) {
                currentMonth = month
                val parts = month.split("-")
                val mNames = listOf("Январь","Февраль","Март","Апрель","Май","Июнь",
                    "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")
                sb.appendLine()
                sb.appendLine("▸ ${mNames[parts[1].toInt()-1]} ${parts[0]}")
            }
            val day = entry.date.substring(8).trimStart('0')
            val note = if (entry.note.isNotEmpty()) "  💬 ${entry.note}" else ""
            sb.appendLine("  $day — ${entry.type.icon} ${entry.type.label}$note")
        }
    }
    sb.appendLine()
    sb.appendLine("─────────────────")
    sb.append("Мой График — приложение для учёта рабочих смен")
    return sb.toString()
}

@Composable
private fun PeriodOption(
    label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                if (selected) 1.5.dp else 0.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                MaterialTheme.shapes.large
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface)
        if (selected) Icon(Icons.Rounded.CheckCircle, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FormatChip(
    label: String, icon: ImageVector, color: Color,
    selected: Boolean, modifier: Modifier, onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(if (selected) color.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(if (selected) 1.5.dp else 0.dp,
                if (selected) color else Color.Transparent,
                MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = if (selected) color
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
