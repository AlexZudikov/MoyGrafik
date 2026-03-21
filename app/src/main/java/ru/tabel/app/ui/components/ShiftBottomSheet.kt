package ru.tabel.app.ui.components

import androidx.compose.animation.*
import androidx.compose.material.icons.rounded.Save
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.tabel.app.data.model.ShiftEntry
import ru.tabel.app.data.model.ShiftType
import ru.tabel.app.ui.theme.pressScale
import ru.tabel.app.ui.theme.staggerDelay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftBottomSheet(
    date: String,
    currentShift: ShiftEntry?,
    defaultStartTime: String = "08:00",
    defaultEndTime: String   = "20:00",
    onSave: (ShiftType?, String, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedType    by remember { mutableStateOf(currentShift?.type) }
    var note            by remember { mutableStateOf(currentShift?.note ?: "") }
    var customStart     by remember { mutableStateOf(currentShift?.customStartTime ?: "") }
    var customEnd       by remember { mutableStateOf(currentShift?.customEndTime ?: "") }
    var showTimeEditor  by remember { mutableStateOf(false) }

    // Кнопка сохранения заметки — активна всегда когда есть смена ИЛИ текст заметки
    val canSaveNote = selectedType != null || note.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = MaterialTheme.shapes.extraLarge,
        dragHandle = {
            Box(
                Modifier.padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Смена — ${formatSheetDate(date)}",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier   = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Нажми на тип — сразу сохранится",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Заметка — доступна только если выбран тип смены
            val hasShift = selectedType != null
            OutlinedTextField(
                value         = note,
                onValueChange = { if (it.length <= 500 && hasShift) note = it },
                label         = {
                    Text(if (hasShift) "Заметка (необязательно)"
                         else "Сначала выберите тип смены")
                },
                modifier      = Modifier.fillMaxWidth(),
                shape         = MaterialTheme.shapes.large,
                maxLines      = 4,
                enabled       = hasShift,
                leadingIcon   = {
                    Icon(
                        if (hasShift) Icons.Rounded.Notes else Icons.Rounded.Lock,
                        null,
                        tint = if (hasShift) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                },
                trailingIcon  = if (note.isNotBlank() && hasShift) {
                    {
                        IconButton(onClick = { note = "" }) {
                            Icon(Icons.Rounded.Clear, "Очистить заметку",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else null,
                supportingText = if (!hasShift) {
                    {
                        Text("Выберите тип смены ниже чтобы добавить заметку",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (note.length > 400) {
                    { Text("${note.length}/500", color =
                        if (note.length >= 500) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant) }
                } else null
            )

            // Кнопка сохранения заметки — появляется при любом изменении (в т.ч. очистка)
            AnimatedVisibility(
                visible = note != (currentShift?.note ?: ""),
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick  = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val start = customStart.toValidTime()
                            val end   = customEnd.toValidTime()
                            onSave(selectedType, note, start, end)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Rounded.Save, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (note.isBlank()) "Удалить заметку" else "Сохранить заметку",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Кастомное время — разворачивается по тапу
            AnimatedVisibility(
                visible = selectedType?.hasTime == true || showTimeEditor
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showTimeEditor = !showTimeEditor }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                        val startLabel = customStart.ifEmpty { defaultStartTime }
                        val endLabel   = customEnd.ifEmpty { defaultEndTime }
                        Text(
                            "Время: $startLabel — $endLabel",
                            style    = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (showTimeEditor) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showTimeEditor) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TimeTextField(
                                value         = customStart,
                                placeholder   = defaultStartTime,
                                label         = "Начало",
                                onValueChange = { customStart = it },
                                modifier      = Modifier.weight(1f)
                            )
                            TimeTextField(
                                value         = customEnd,
                                placeholder   = defaultEndTime,
                                label         = "Конец",
                                onValueChange = { customEnd = it },
                                modifier      = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }

            Text(
                "ТИП СМЕНЫ",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            LazyVerticalGrid(
                columns               = GridCells.Fixed(2),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.height(280.dp)
            ) {
                items(ShiftType.entries.size) { idx ->
                    val type    = ShiftType.entries[idx]
                    val visible = staggerDelay(idx, baseMs = 40)
                    AnimatedVisibility(
                        visible = visible,
                        enter   = fadeIn(tween(200)) + scaleIn(
                            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                            initialScale = 0.85f
                        )
                    ) {
                        AnimatedShiftTypeCard(
                            type       = type,
                            isSelected = selectedType == type,
                            onClick    = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val newType = if (selectedType == type) null else type
                                selectedType = newType
                                if (newType?.hasTime == true) showTimeEditor = true
                                // Сохраняем тип ВМЕСТЕ с текущей заметкой
                                val start = customStart.toValidTime()
                                val end   = customEnd.toValidTime()
                                onSave(newType, note.trim(), start, end)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (currentShift != null) {
                OutlinedButton(
                    onClick  = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(null, "", null, null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape    = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Удалить смену")
                }
            }
        }
    }
}

// Возвращает время если оно валидно (HH:MM), иначе null
private fun String.toValidTime(): String? {
    if (isEmpty()) return null
    return if (matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) this else null
}

@Composable
private fun TimeTextField(
    value: String,
    placeholder: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = { new ->
            // автоформат: вставляем ':' после 2 цифр
            val digits = new.filter { it.isDigit() }.take(4)
            onValueChange(when {
                digits.length >= 3 -> digits.substring(0, 2) + ":" + digits.substring(2)
                else               -> digits
            })
        },
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier      = modifier,
        shape         = MaterialTheme.shapes.large,
        singleLine    = true,
        leadingIcon   = { Icon(Icons.Rounded.Schedule, null, Modifier.size(16.dp)) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        )
    )
}

@Composable
private fun AnimatedShiftTypeCard(type: ShiftType, isSelected: Boolean, onClick: () -> Unit) {
    val typeColor = Color(type.color)

    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.04f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "typeCardScale"
    )
    val bg by animateColorAsState(
        targetValue   = if (isSelected) typeColor.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label         = "typeCardBg"
    )
    val border by animateColorAsState(
        targetValue   = if (isSelected) typeColor else Color.Transparent,
        animationSpec = tween(200),
        label         = "typeCardBorder"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(MaterialTheme.shapes.large)
            .background(bg)
            .border(2.dp, border, MaterialTheme.shapes.large)
            .pressScale(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(type.icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Text(
                type.label,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color      = if (isSelected) typeColor
                             else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatSheetDate(iso: String): String {
    val months = listOf("янв","фев","мар","апр","май","июн",
        "июл","авг","сен","окт","ноя","дек")
    return try {
        val p = iso.split("-")
        "${p[2].toInt()} ${months[p[1].toInt()-1]}"
    } catch (e: Exception) { iso }
}
