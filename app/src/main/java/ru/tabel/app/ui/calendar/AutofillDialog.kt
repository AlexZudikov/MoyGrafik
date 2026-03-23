package ru.tabel.app.ui.calendar

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tabel.app.data.model.ShiftType
import java.time.LocalDate

data class AutofillTemplate(
    val id: String,
    val name: String,
    val desc: String,
    val pattern: List<ShiftType>
)

val SHIFT_AUTOFILL_TEMPLATES = listOf(
    AutofillTemplate("dn_so",  "День/Ночь/Отс/Вых", "Классический 4-дневный цикл",
        listOf(ShiftType.DAY, ShiftType.NIGHT, ShiftType.SLEEP, ShiftType.OFF)),
    AutofillTemplate("1_1",    "1/1 день/выходной",  "1 рабочий + 1 выходной",
        listOf(ShiftType.DAY, ShiftType.OFF)),
    AutofillTemplate("2_2",    "2/2",                "2 рабочих, 2 выходных",
        listOf(ShiftType.DAY, ShiftType.DAY, ShiftType.OFF, ShiftType.OFF)),
    AutofillTemplate("3_3",    "3/3",                "3 рабочих, 3 выходных",
        listOf(ShiftType.DAY, ShiftType.DAY, ShiftType.DAY, ShiftType.OFF, ShiftType.OFF, ShiftType.OFF)),
    AutofillTemplate("suit3",  "Сутки/трое",         "Суточная, 3 выходных",
        listOf(ShiftType.DAY, ShiftType.OFF, ShiftType.OFF, ShiftType.OFF)),
    AutofillTemplate("5_2",    "5/2",                "Пн–Пт, Сб–Вс выходных",
        listOf(ShiftType.DAY, ShiftType.DAY, ShiftType.DAY, ShiftType.DAY, ShiftType.DAY, ShiftType.OFF, ShiftType.OFF)),
    AutofillTemplate("6_1",    "6/1 (вахта)",        "6 рабочих, 1 выходной",
        listOf(ShiftType.DAY, ShiftType.DAY, ShiftType.DAY, ShiftType.DAY, ShiftType.DAY, ShiftType.DAY, ShiftType.OFF)),
    AutofillTemplate("6_1n",   "6/1 ночной (вахта)", "6 ночных, 1 выходной",
        listOf(ShiftType.NIGHT, ShiftType.NIGHT, ShiftType.NIGHT, ShiftType.NIGHT, ShiftType.NIGHT, ShiftType.NIGHT, ShiftType.OFF)),
)

private val MONTHS = listOf("Январь","Февраль","Март","Апрель","Май","Июнь",
    "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь")
private val DOW = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutofillBottomSheet(
    currentMonth: java.time.YearMonth,
    onConfirm: (pattern: List<ShiftType>, startDate: LocalDate, startIndex: Int) -> Unit,
    onConfirmYear: (pattern: List<ShiftType>, startDate: LocalDate, startIndex: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTemplateId  by remember { mutableStateOf(SHIFT_AUTOFILL_TEMPLATES[0].id) }
    var startTypeIndex      by remember { mutableStateOf(0) }
    var showYearConfirm     by remember { mutableStateOf(false) }
    var pendingYearPattern  by remember { mutableStateOf<List<ru.tabel.app.data.model.ShiftType>>(emptyList()) }
    var pendingYearStart    by remember { mutableStateOf<java.time.LocalDate?>(null) }
    var pendingYearIndex    by remember { mutableStateOf(0) }

    // Выбор дня начала — по умолчанию 1-е число
    var selectedDay by remember { mutableStateOf(1) }
    val daysInMonth = currentMonth.lengthOfMonth()

    val selectedTemplate = SHIFT_AUTOFILL_TEMPLATES.find { it.id == selectedTemplateId } ?: SHIFT_AUTOFILL_TEMPLATES[0]
    LaunchedEffect(selectedTemplateId) { startTypeIndex = 0 }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Заголовок ─────────────────────────────────────
            Text("Автозаполнение",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 4.dp))
            Text(
                "${MONTHS[currentMonth.monthValue-1]} ${currentMonth.year}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ── Выбор шаблона (сетка 2 колонки) ───────────────
            Text("Шаблон графика",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp))

            SHIFT_AUTOFILL_TEMPLATES.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { tpl ->
                        val isSel = tpl.id == selectedTemplateId
                        Card(
                            modifier = Modifier.weight(1f).clickable { selectedTemplateId = tpl.id },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant),
                            border = if (isSel) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tpl.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface)
                                Text(tpl.desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    tpl.pattern.forEach { type ->
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape)
                                            .background(Color(type.color)))
                                    }
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // ── Выбор типа смены для начала цикла ─────────────
            Text("С какой смены начать цикл?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp))

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedTemplate.pattern.forEachIndexed { index, type ->
                    val isSel = startTypeIndex == index
                    val color = Color(type.color)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .background(if (isSel) color.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant)
                            .border(if (isSel) 2.dp else 0.dp,
                                    if (isSel) color else Color.Transparent,
                                    MaterialTheme.shapes.large)
                            .clickable { startTypeIndex = index }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Text(type.icon, fontSize = 20.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(type.label,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = if (isSel) color else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 9.sp, maxLines = 2)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // ── Выбор дня начала ───────────────────────────────
            Text("С какого числа начать?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp))

            // Мини-календарь с числами месяца
            val firstDow = currentMonth.atDay(1).dayOfWeek.value - 1 // 0=Пн
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Заголовок дней недели
                Row(modifier = Modifier.fillMaxWidth()) {
                    DOW.forEach { d ->
                        Text(d, modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp)
                    }
                }
                // Числа
                val cells = firstDow + daysInMonth
                val rows = (cells + 6) / 7
                var day = 1
                repeat(rows) { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(7) { col ->
                            val cellIndex = row * 7 + col
                            if (cellIndex < firstDow || day > daysInMonth) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                val d = day
                                val isSel = selectedDay == d
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) Color(selectedTemplate.pattern[startTypeIndex].color)
                                                    else Color.Transparent)
                                        .border(
                                            width = if (!isSel && d == 1) 1.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedDay = d },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(d.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (isSel) Color.White
                                                else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp)
                                }
                                day++
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Инфо
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.large) {
                Row(modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Info, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp))
                    Text(
                        "С $selectedDay-го числа, первая смена: «${selectedTemplate.pattern[startTypeIndex].label}»",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val startDate = LocalDate.of(currentMonth.year, currentMonth.month, selectedDay)
                    onConfirm(selectedTemplate.pattern, startDate, startTypeIndex)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Заполнить месяц", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val startDate = LocalDate.of(currentMonth.year, currentMonth.month, selectedDay)
                    pendingYearPattern = selectedTemplate.pattern
                    pendingYearStart   = startDate
                    pendingYearIndex   = startTypeIndex
                    showYearConfirm    = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("До конца года", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large) {
                Text("Отмена")
            }
        }
    }

    // ── Диалог подтверждения "на год" ──────────────────────────
    if (showYearConfirm) {
        AlertDialog(
            onDismissRequest = { showYearConfirm = false },
            icon  = { Text("📅", style = MaterialTheme.typography.displaySmall) },
            title = {
                Text("Заполнить на год?",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
            },
            text  = {
                val startYear = pendingYearStart?.year ?: LocalDate.now().year
                Text("Это перезапишет расписание с выбранной даты до 31 декабря $startYear года.\n\nВсе ручные правки в этом периоде будут удалены.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showYearConfirm = false
                        val start = pendingYearStart ?: return@Button
                        onConfirmYear(pendingYearPattern, start, pendingYearIndex)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Да, заполнить", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showYearConfirm = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large
                ) { Text("Отмена") }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}
