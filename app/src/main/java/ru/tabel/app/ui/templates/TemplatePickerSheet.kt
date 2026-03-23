package ru.tabel.app.ui.templates

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tabel.app.data.model.DefaultTemplates
import ru.tabel.app.data.model.ShiftTemplate
import ru.tabel.app.data.model.ShiftType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePickerSheet(
    onSelect: (template: ShiftTemplate, startDay: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedTemplate by remember { mutableStateOf<ShiftTemplate?>(null) }
    var startDay by remember { mutableStateOf(1) }
    var showStartDayPicker by remember { mutableStateOf(false) }

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
        ) {
            Text(
                "Выберите шаблон",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Применится с выбранного дня месяца",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(DefaultTemplates.templates) { template ->
                    TemplateCard(
                        template = template,
                        isSelected = selectedTemplate == template,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedTemplate = template
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedTemplate != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))

                    // Превью шаблона
                    selectedTemplate?.let { template ->
                        TemplatePreview(template = template)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Выбор дня начала
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDayPicker = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.CalendarMonth,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Начать с дня",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "$startDay числа",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                Icons.Rounded.Edit,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Кнопка применить
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedTemplate?.let { onSelect(it, startDay) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Применить шаблон", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Диалог выбора дня
    if (showStartDayPicker) {
        StartDayPickerDialog(
            currentDay = startDay,
            onConfirm = {
                startDay = it
                showStartDayPicker = false
            },
            onDismiss = { showStartDayPicker = false }
        )
    }
}

@Composable
private fun TemplateCard(
    template: ShiftTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                  else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Иконка шаблона
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (template.workDays > 0) Color(0xFF4F6EF7).copy(alpha = 0.15f)
                        else Color(0xFF22c55e).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Pattern,
                    null,
                    tint = Color(0xFF4F6EF7),
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${template.workDays} рабочих",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4F6EF7)
                    )
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${template.restDays} выходных",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF22c55e)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TemplatePreview(template: ShiftTemplate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Превью шаблона",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            
            val pattern = parsePattern(template.pattern)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                pattern.take(14).forEach { type ->
                    val color = Color(type.color)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(type.icon, fontSize = 12.sp)
                    }
                }
                if (pattern.size > 14) {
                    Text("...", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                "Цикл: ${template.workDays + template.restDays} дней",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StartDayPickerDialog(
    currentDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(currentDay) }
    val today = LocalDate.now()
    val daysInMonth = today.lengthOfMonth()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Начать с дня", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Выберите день месяца, с которого начнётся шаблон",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(1, 10, 15, 20, 25).forEach { day ->
                        if (day <= daysInMonth) {
                            val isSelected = selectedDay == day
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedDay = day },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$day",
                                    color = if (isSelected) Color.White
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = if (selectedDay in 1..daysInMonth) selectedDay.toString() else "",
                    onValueChange = { 
                        val d = it.filter { c -> c.isDigit() }.toIntOrNull()
                        if (d != null && d in 1..daysInMonth) {
                            selectedDay = d
                        }
                    },
                    label = { Text("Или введите день") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDay) },
                enabled = selectedDay in 1..daysInMonth
            ) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun parsePattern(pattern: String): List<ShiftType> {
    return try {
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
        val days: List<Map<String, Any>> = gson.fromJson(pattern, type)
        days.mapNotNull { d ->
            val typeName = d["type"] as? String
            runCatching { ShiftType.valueOf(typeName ?: "") }.getOrNull()
        }
    } catch (e: Exception) {
        emptyList()
    }
}
