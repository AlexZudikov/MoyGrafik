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
import ru.tabel.app.ui.theme.rememberAdaptiveDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftBottomSheet(
    date: String,
    currentShift: ShiftEntry?,
    defaultStartTime: String = "08:00",
    defaultEndTime: String   = "20:00",
    onSave: (ShiftType?, String, String?, String?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedType    by remember { mutableStateOf(currentShift?.type) }
    var note            by remember { mutableStateOf(currentShift?.note ?: "") }
    var customStart     by remember { mutableStateOf(currentShift?.customStartTime ?: "") }
    var customEnd       by remember { mutableStateOf(currentShift?.customEndTime ?: "") }
    var showTimeEditor  by remember { mutableStateOf(false) }
    var isLocked        by remember { mutableStateOf(currentShift?.locked ?: false) }
    val dimens          = rememberAdaptiveDimens()

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
                .padding(horizontal = if (dimens.isCompact) 16.dp else 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Смена — ${formatSheetDate(date)}",
                    style      = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.titleFontSize),
                    fontWeight = FontWeight.Black,
                    modifier   = Modifier.padding(bottom = 4.dp)
                )
                if (currentShift != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                if (isLocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isLocked = !isLocked
                                onSave(selectedType, note.trim(), customStart.toValidTime(), customEnd.toValidTime(), isLocked)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (isLocked) "Заблокировано" else "Разблокировано",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = dimens.labelFontSize),
                            color = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (isLocked) {
                Text(
                    "Время и тип смены защищены от изменений",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.labelFontSize),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = dimens.cardPadding)
                )
            } else {
                Text(
                    "Нажми на тип — сразу сохранится",
                    style    = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.labelFontSize),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = dimens.cardPadding)
                )
            }

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
                            onSave(selectedType, note, start, end, isLocked)
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

            // ── Время смены — компактная строка ────────────────
            AnimatedVisibility(
                visible = selectedType?.hasTime == true || showTimeEditor
            ) {
                val workMinutes = calculateWorkMinutes(
                    customStart.ifEmpty { defaultStartTime },
                    customEnd.ifEmpty { defaultEndTime }
                )
                
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                if (isLocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLocked) {
                            Icon(
                                Icons.Rounded.Lock,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp))
                            Text(
                                "Время заблокировано",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Начало
                            CompactTimeField(
                                value       = customStart,
                                placeholder = defaultStartTime,
                                label       = "Нач.",
                                onValueChange = { customStart = it },
                                modifier    = Modifier.weight(1f)
                            )
                            
                            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            // Конец
                            CompactTimeField(
                                value       = customEnd,
                                placeholder = defaultEndTime,
                                label       = "Кон.",
                                onValueChange = { customEnd = it },
                                modifier    = Modifier.weight(1f)
                            )
                            
                            Spacer(Modifier.width(4.dp))
                            
                            // Чистое время
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${workMinutes / 60}ч ${workMinutes % 60}м",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (workMinutes > 0) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "чистое",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                }
            }

            Text(
                "ТИП СМЕНЫ",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            LazyVerticalGrid(
                columns               = GridCells.Fixed(dimens.gridColumns),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.heightIn(max = if (dimens.isCompact) 240.dp else 280.dp)
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
                            locked     = isLocked,
                            onClick    = {
                                if (isLocked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    return@AnimatedShiftTypeCard
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val newType = if (selectedType == type) null else type
                                selectedType = newType
                                if (newType?.hasTime == true) showTimeEditor = true
                                val start = customStart.toValidTime()
                                val end   = customEnd.toValidTime()
                                onSave(newType, note.trim(), start, end, isLocked)
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
                        onSave(null, "", null, null, isLocked)
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

// Расчёт чистого рабочего времени в минутах
private fun calculateWorkMinutes(start: String, end: String): Int {
    try {
        val (sh, sm) = start.split(":").map { it.toIntOrNull() ?: 0 }
        val (eh, em) = end.split(":").map { it.toIntOrNull() ?: 0 }
        
        var startMinutes = sh * 60 + sm
        var endMinutes = eh * 60 + em
        
        // Ночная смена — переход через полночь
        if (endMinutes <= startMinutes) {
            endMinutes += 24 * 60
        }
        
        return (endMinutes - startMinutes).coerceAtLeast(0)
    } catch (e: Exception) {
        return 0
    }
}

@Composable
private fun CompactTimeField(
    value: String,
    placeholder: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = { new ->
            val digits = new.filter { it.isDigit() }.take(4)
            onValueChange(when {
                digits.length >= 3 -> digits.substring(0, 2) + ":" + digits.substring(2)
                else               -> digits
            })
        },
        label         = { Text(label, style = MaterialTheme.typography.labelSmall) },
        placeholder   = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) },
        modifier      = modifier.height(56.dp),
        shape         = MaterialTheme.shapes.medium,
        singleLine    = true,
        textStyle     = MaterialTheme.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        )
    )
}

@Composable
private fun AnimatedShiftTypeCard(type: ShiftType, isSelected: Boolean, locked: Boolean, onClick: () -> Unit) {
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
            .pressScale(onClick = if (locked) {{}} else onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(type.icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Text(
                type.label,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color      = if (isSelected) typeColor
                             else MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.weight(1f)
            )
            if (locked && isSelected) {
                Icon(
                    Icons.Rounded.Lock,
                    "Заблокировано",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
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
