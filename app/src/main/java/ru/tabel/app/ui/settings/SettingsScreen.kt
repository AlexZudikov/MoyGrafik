package ru.tabel.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import ru.tabel.app.data.model.ThemeMode
import androidx.compose.ui.platform.LocalHapticFeedback
import ru.tabel.app.ui.calendar.AutofillBottomSheet
import ru.tabel.app.ui.templates.TemplatePickerSheet
import java.time.LocalDate
import androidx.compose.ui.text.font.FontWeight
import ru.tabel.app.ui.theme.rememberAdaptiveDimens
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.tabel.app.SettingsViewModel
import ru.tabel.app.data.model.*
import ru.tabel.app.ui.profile.ProfileViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// ─────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    onNavigateToProfiles: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel(),
    profileVm: ProfileViewModel = hiltViewModel()
) {
    val context      = LocalContext.current
    val haptic       = LocalHapticFeedback.current

    val settings      by vm.settings.collectAsState()
    val shiftTimes    by vm.shiftTimes.collectAsState()
    val profiles      by profileVm.profiles.collectAsState()
    val activeProfile by profileVm.activeProfile.collectAsState()
    val dimens       = rememberAdaptiveDimens()

    // ── Локальное состояние полей ──────────────────────────────
    var wageText   by remember(settings.hourlyRate)      { mutableStateOf(if (settings.hourlyRate > 0) settings.hourlyRate.toInt().toString() else "") }
    var nightText  by remember(settings.nightCoeff)      { mutableStateOf(settings.nightCoeff.toString()) }
    var holText    by remember(settings.holidayCoeff)    { mutableStateOf(settings.holidayCoeff.toString()) }
    var sickText   by remember(settings.sickCoeff)        { mutableStateOf(settings.sickCoeff.toString()) }
    var fontScale  by remember(settings.fontScale)       { mutableStateOf((settings.fontScale * 100).toInt()) }

    // ── Диалоги ───────────────────────────────────────────────
    var timePickerFor     by remember { mutableStateOf<Triple<ShiftType, Boolean, String>?>(null) }
    // Triple(type, isStart, currentValue)
    var showAutofill by remember { mutableStateOf(false) }
    var showTemplate by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var clearMonthYear by remember { mutableStateOf(LocalDate.now().year) }
    var clearMonthMonth by remember { mutableStateOf(LocalDate.now().monthValue) }
    var isClearingYear by remember { mutableStateOf(false) }

    // ── Лаунчер для выбора файла восстановления ───────────────
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { doRestore(context, it, vm) }
    }


    // ── Карта время смен: type -> ShiftTime ───────────────────
    val timesMap = remember(shiftTimes) { shiftTimes.associateBy { it.type } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Хедер
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = dimens.horizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Настройки", style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.titleFontSize), fontWeight = FontWeight.ExtraBold)
            Box(Modifier.size(dimens.buttonHeight - 8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Settings, null, Modifier.size(dimens.iconSizeSmall), tint = MaterialTheme.colorScheme.primary)
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── ПРОФИЛИ ───────────────────────────────────────
            SettingsCard {
                SectionHeader("ПРОФИЛИ", Icons.Rounded.People, Color(0xFF4F6EF7))

                val context2 = LocalContext.current

                profiles.forEach { profile ->
                    val isActive  = profile.id == activeProfile?.id
                    val isLocked  = context2.getSharedPreferences("profile_locks", android.content.Context.MODE_PRIVATE)
                        .getBoolean("locked_${profile.id}", false)
                    var showRename by remember(profile.id) { mutableStateOf(false) }
                    var renameVal  by remember(profile.id) { mutableStateOf(profile.name) }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.large,
                        onClick = {
                            if (!isActive) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                profileVm.selectProfile(profile)
                            }
                        }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Аватар
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape)
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(profile.name.take(1).uppercase(),
                                        color = if (isActive) Color.White
                                                else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.ExtraBold)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(profile.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold)
                                    Text(
                                        if (isActive) "Активный профиль"
                                        else "Нажмите для переключения",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (isLocked) {
                                        Text("🔒 Защищён от удаления",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                // ✏️ Переименовать
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    renameVal = profile.name
                                    showRename = true
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Rounded.Edit, "Переименовать",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp))
                                }
                                // 🔒 Замок
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    context2.getSharedPreferences("profile_locks", android.content.Context.MODE_PRIVATE)
                                        .edit().putBoolean("locked_${profile.id}", !isLocked).apply()
                                    // Перерисовка через recomposition
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                        if (isLocked) "Разблокировать" else "Заблокировать",
                                        tint = if (isLocked) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                // 🗑️ Удалить
                                if (!isActive && !isLocked && profiles.size > 1) {
                                    IconButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        profileVm.deleteProfile(profile)
                                    }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Rounded.Delete, "Удалить",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Диалог переименования
                    if (showRename) {
                        AlertDialog(
                            onDismissRequest = { showRename = false },
                            shape = MaterialTheme.shapes.extraLarge,
                            title = { Text("Переименовать", fontWeight = FontWeight.Bold) },
                            text = {
                                OutlinedTextField(
                                    value = renameVal,
                                    onValueChange = { renameVal = it },
                                    label = { Text("Новое название") },
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (renameVal.isNotBlank())
                                            profileVm.renameProfile(profile, renameVal.trim())
                                        showRename = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large
                                ) { Text("Сохранить", fontWeight = FontWeight.Bold) }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { showRename = false },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large
                                ) { Text("Отмена") }
                            }
                        )
                    }
                }

                // Добавить профиль
                var showAddProfile by remember { mutableStateOf(false) }
                var newProfileName by remember { mutableStateOf("") }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        newProfileName = ""
                        showAddProfile = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить профиль")
                }

                if (showAddProfile) {
                    AlertDialog(
                        onDismissRequest = { showAddProfile = false },
                        shape = MaterialTheme.shapes.extraLarge,
                        title = { Text("Новый профиль", fontWeight = FontWeight.Bold) },
                        text = {
                            OutlinedTextField(
                                value = newProfileName,
                                onValueChange = { newProfileName = it },
                                label = { Text("Название") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newProfileName.isNotBlank()) {
                                        profileVm.createProfile(newProfileName.trim())
                                        showAddProfile = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                enabled = newProfileName.isNotBlank()
                            ) { Text("Создать", fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showAddProfile = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) { Text("Отмена") }
                        }
                    )
                }
            }

            // ── ОБОЗНАЧЕНИЯ СМЕН ──────────────────────────────────
            SettingsCard {
                SectionHeader("ОБОЗНАЧЕНИЯ СМЕН", Icons.Rounded.Info, Color(0xFF8B5CF6))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShiftType.entries.forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(type.color).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type.icon, fontSize = 14.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    type.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!type.hasTime) {
                                    Text(
                                        "Без времени",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(type.color))
                            )
                        }
                    }
                }
            }

            // ── ВРЕМЯ СМЕН (кликабельные строки → TimePicker) ─
            SettingsCard {
                SectionHeader("ВРЕМЯ СМЕН", Icons.Rounded.Schedule, Color(0xFF06b6d4))
                Text("Нажмите на время чтобы изменить",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp))

                val isLockedData = settings.isLocked
                val timeTypes = listOf(ShiftType.DAY, ShiftType.NIGHT, ShiftType.HOLIDAY, ShiftType.SLEEP, ShiftType.SICK)
                
                timeTypes.forEach { type ->
                    val t     = timesMap[type]
                    val start = t?.startTime ?: "08:00"
                    val end   = t?.endTime   ?: if (type == ShiftType.NIGHT) "08:00" else "20:00"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .then(if (isLockedData) Modifier.alpha(0.5f) else Modifier),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(type.color).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center) {
                            Text(type.icon, fontSize = 16.sp)
                        }
                        Text(type.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)

                        if (isLockedData) {
                            Icon(
                                Icons.Rounded.Lock, "Заблокировано",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            // Кнопка начала
                            TimeChip(start) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                timePickerFor = Triple(type, true, start)
                            }
                            Text("–", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            // Кнопка конца
                            TimeChip(end) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                timePickerFor = Triple(type, false, end)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))

                var breakMin by remember(settings.breakMinutes) {
                    mutableStateOf(settings.breakMinutes)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF22c55e).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.FreeBreakfast, null, tint = Color(0xFF22c55e), modifier = Modifier.size(18.dp))
                    }
                    Text("Перерыв / обед", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color(0xFF22c55e).copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (breakMin == 0) "Не учитывать"
                            else when {
                                breakMin < 60  -> "$breakMin мин"
                                breakMin % 60 == 0 -> "${breakMin / 60} ч"
                                else -> "${breakMin / 60}ч ${breakMin % 60}м"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22c55e)
                        )
                    }
                }
                Text(
                    "Вычитается из времени каждой смены",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 46.dp, top = 2.dp)
                )
                val breakSteps = listOf(0, 15, 30, 45, 60, 75, 90, 120)
                val breakIdx = (breakSteps.indexOfFirst { it == breakMin }.takeIf { it >= 0 } ?: 0).toFloat()
                Slider(
                    value = breakIdx,
                    onValueChange = { if (!isLockedData) breakMin = breakSteps[it.toInt()] },
                    valueRange = 0f..(breakSteps.size - 1).toFloat(),
                    steps = breakSteps.size - 2,
                    enabled = !isLockedData,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF22c55e), 
                        activeTrackColor = Color(0xFF22c55e),
                        disabledThumbColor = Color.Gray,
                        disabledActiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth().then(if (isLockedData) Modifier.alpha(0.5f) else Modifier), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("1ч", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("2ч", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.saveWageSettings(
                            wageText.toFloatOrNull() ?: 0f,
                            nightText.toFloatOrNull() ?: 1.5f,
                            holText.toFloatOrNull() ?: 2.0f,
                            sickText.toFloatOrNull() ?: 0.6f
                        )
                        vm.setBreakMinutes(breakMin)
                        Toast.makeText(context, "Сохранено ✓", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isLockedData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLockedData) Modifier.alpha(0.5f) else Modifier), 
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Save, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            }

            // ── ЗАРПЛАТА И РАБОЧЕЕ ВРЕМЯ ──────────────────────
            SettingsCard {
                SectionHeader("ЗАРПЛАТА И РАБОЧЕЕ ВРЕМЯ", Icons.Rounded.CurrencyRuble, Color(0xFF22c55e))
                WageField("Ставка (₽/час)", wageText)  { wageText  = it }
                Spacer(Modifier.height(8.dp))
                WageField("Коэф. ночных",   nightText) { nightText = it }
                Spacer(Modifier.height(8.dp))
                WageField("Коэф. праздников", holText) { holText   = it }
                Spacer(Modifier.height(8.dp))
                WageField("Коэф. больничных",  sickText) { sickText  = it }

                Spacer(Modifier.height(16.dp))
                
                val isLockedData = settings.isLocked
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.saveWageSettings(
                            wageText.toFloatOrNull() ?: 0f,
                            nightText.toFloatOrNull() ?: 1.5f,
                            holText.toFloatOrNull() ?: 2.0f,
                            sickText.toFloatOrNull() ?: 0.6f
                        )
                        Toast.makeText(context, "Сохранено ✓", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isLockedData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLockedData) Modifier.alpha(0.5f) else Modifier), 
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Save, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Сохранить", fontWeight = FontWeight.Bold)
                }
            }

            // ── УВЕДОМЛЕНИЯ ───────────────────────────────────
            var showNotifSheet by remember { mutableStateOf(false) }
            SettingsCard {
                SectionHeader("УВЕДОМЛЕНИЯ", Icons.Rounded.Notifications, Color(0xFFf97316))
                NotifPermissionBanner(context)

                // Краткое состояние
                val notifSummary = if (settings.notifHoursBefore > 0) {
                    val h = settings.notifHoursBefore
                    val w = when { h == 1 -> "час"; h in 2..4 -> "часа"; else -> "часов" }
                    "За $h $w до начала смены"
                } else "Уведомления отключены"

                Row(
                    Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showNotifSheet = true
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFf97316).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.NotificationsActive, null,
                            tint = Color(0xFFf97316), modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Настроить уведомления",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium)
                        Text(notifSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }
            if (showNotifSheet) {
                NotifSheet(onDismiss = { showNotifSheet = false })
            }

            // ── РЕЗЕРВНОЕ КОПИРОВАНИЕ ─────────────────────────
            SettingsCard {
                SectionHeader("РЕЗЕРВНОЕ КОПИРОВАНИЕ", Icons.Rounded.Backup, Color(0xFF06b6d4))

                BackupBtn(Icons.Rounded.Download, "Сохранить резервную копию") {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    doExportJson(context, vm)
                }
                Spacer(Modifier.height(8.dp))
                BackupBtn(Icons.Rounded.Upload, "Восстановить из файла") {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    restoreLauncher.launch("application/json")
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))

                // Автобекап
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Автобекап",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        Text(
                            if (settings.autoBackupEnabled) "Каждые ${settings.autoBackupFrequency} дней"
                            else "Отключён",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoBackupEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.setAutoBackup(it)
                        }
                    )
                }

                if (settings.autoBackupEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1 to "Каждый день", 7 to "Раз в неделю", 30 to "Раз в месяц").forEach { (days, label) ->
                            val selected = settings.autoBackupFrequency == days
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        vm.setAutoBackup(true, days)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }


            // ── ТЕМА ──────────────────────────────────────────
            SettingsCard {
                SectionHeader("ТЕМА ОФОРМЛЕНИЯ", Icons.Rounded.DarkMode, Color(0xFF8B5CF6))
                val currentTheme = runCatching { ThemeMode.valueOf(settings.themeMode) }
                    .getOrDefault(ThemeMode.SYSTEM)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to ("🌓" to "Авто"),
                        ThemeMode.LIGHT  to ("☀️" to "Светлая"),
                        ThemeMode.DARK   to ("🌙" to "Тёмная")
                    ).forEach { (mode, pair) ->
                        val (emoji, label) = pair
                        val selected = currentTheme == mode
                        Column(
                            modifier = Modifier.weight(1f)
                                .clip(MaterialTheme.shapes.large)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    2.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    MaterialTheme.shapes.large
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.setThemeMode(mode)
                                }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(emoji, style = MaterialTheme.typography.titleLarge)
                            Text(label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── ИНТЕРФЕЙС ─────────────────────────────────────
            SettingsCard {
                SectionHeader("ИНТЕРФЕЙС", Icons.Rounded.TextFields, Color(0xFF06b6d4))
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Размер шрифта", style = MaterialTheme.typography.bodyMedium)
                    Text("$fontScale%", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = fontScale.toFloat(),
                    onValueChange = { if (!settings.fontLocked) fontScale = it.toInt() },
                    onValueChangeFinished = { if (!settings.fontLocked) vm.setFontScale(fontScale / 100f) },
                    valueRange = 80f..130f, steps = 9,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !settings.fontLocked
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(85 to "Мелкий", 100 to "Обычный", 115 to "Крупный").forEach { (v, label) ->
                        val sel = fontScale == v
                        val isLocked = settings.fontLocked
                        Box(
                            Modifier.weight(1f).clip(MaterialTheme.shapes.medium)
                                .background(
                                    when {
                                        isLocked -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        sel -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .then(
                                    if (!isLocked) Modifier.clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); fontScale = v; vm.setFontScale(v / 100f) }
                                    else Modifier
                                )
                                .padding(vertical = 8.dp),
                            Alignment.Center
                        ) {
                            Text(
                                if (isLocked) "🔒" else label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isLocked -> MaterialTheme.colorScheme.onSurfaceVariant
                                    sel -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium) {
                    Text("Пример: Дневная смена 08:00–20:00",
                        Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Динамические цвета",
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium)
                            Text("Цветовая схема подстраивается под обои телефона (Material You)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked         = settings.dynamicColor,
                            onCheckedChange = { vm.setDynamicColor(it) }
                        )
                    }
                }

                // ── Блокировка шрифта ─────────────────────────
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, null,
                                tint = if (settings.fontLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Блокировка шрифта",
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium)
                        }
                        Text("Защитить настройки шрифта от случайного изменения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked         = settings.fontLocked,
                        onCheckedChange = { vm.setFontLocked(it) }
                    )
                }

                // ── Блокировка данных ─────────────────────────
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, null,
                                tint = if (settings.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Блокировка данных",
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium)
                        }
                        Text("Защитить удаление и редактирование смен",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked         = settings.isLocked,
                        onCheckedChange = { vm.setIsLocked(it) }
                    )
                }
            }


            // ── КОНФИГУРАЦИЯ ДАННЫХ ────────────────────────────
            SettingsCard {
                SectionHeader("КОНФИГУРАЦИЯ ДАННЫХ", Icons.Rounded.AutoAwesome, Color(0xFF06b6d4))
                
                val isLockedData = settings.isLocked
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Автозаполнение
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAutofill = true
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.AutoAwesome, null,
                                tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Автозаполнение",
                                color = Color.White, fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    
                    // Шаблоны
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .background(Color(0xFF8B5CF6))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showTemplate = true
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Pattern, null,
                                tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Шаблоны",
                                color = Color.White, fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Очистить месяц и год
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLockedData) Modifier.alpha(0.5f) else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Очистить месяц
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable(enabled = !isLockedData) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isClearingYear = false
                                showClearConfirm = true
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CalendarMonth, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Месяц",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // Очистить год
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable(enabled = !isLockedData) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isClearingYear = true
                                showClearConfirm = true
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.DateRange, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Год",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Text(
                    "Удалить все смены за выбранный период",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // ── ПОДДЕРЖКА ──────────────────────────────────────
            // ✏️ ПОМЕНЯЙ СВОЙ EMAIL ЗДЕСЬ ↓
            val developerEmail = "moygrafik@mail.ru"
            // ✏️ КОНЕЦ РЕДАКТИРУЕМОЙ ЗОНЫ

            SettingsCard {
                SectionHeader("ПОДДЕРЖКА", Icons.Rounded.HeadsetMic, Color(0xFF4F6EF7))

                // Написать разработчику
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:$developerEmail")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Мой График — обратная связь")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier.size(40.dp).clip(MaterialTheme.shapes.medium)
                            .background(Color(0xFF4F6EF7).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Email, null, tint = Color(0xFF4F6EF7), modifier = Modifier.size(20.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text("Написать разработчику",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium)
                        Text(developerEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── TimePicker диалог ─────────────────────────────────────
    timePickerFor?.let { (type, isStart, current) ->
        TimePickerDialog(
            title    = if (isStart) "Начало — ${type.label}" else "Конец — ${type.label}",
            initial  = current,
            onConfirm = { newTime ->
                val cur = timesMap[type]
                val updated = if (isStart)
                    ShiftTime(type, newTime, cur?.endTime ?: "20:00")
                else
                    ShiftTime(type, cur?.startTime ?: "08:00", newTime)
                vm.saveShiftTime(type, updated.startTime, updated.endTime)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                timePickerFor = null
            },
            onDismiss = { timePickerFor = null }
        )
    }

    // ── Автозаполнение ──────────────────────────────────────
    if (showAutofill) {
        AutofillBottomSheet(
            currentMonth  = java.time.YearMonth.now(),
            onConfirm     = { pattern, startDate, startIndex ->
                showAutofill = false
            },
            onConfirmYear = { pattern, startDate, startIndex ->
                showAutofill = false
            },
            onDismiss     = { showAutofill = false }
        )
    }

    // ── Шаблоны ─────────────────────────────────────────────
    if (showTemplate) {
        val currentMonth = java.time.YearMonth.now()
        TemplatePickerSheet(
            onSelect = { template, startDay ->
                vm.applyTemplate(template, currentMonth.year, currentMonth.monthValue, startDay)
                showTemplate = false
            },
            onDismiss = { showTemplate = false }
        )
    }

    // ── Подтверждение очистки месяца/года ────────────────────────
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            shape = MaterialTheme.shapes.extraLarge,
            icon = {
                Icon(
                    Icons.Rounded.Warning, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    if (isClearingYear) "Очистить год?" else "Очистить месяц?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        if (isClearingYear) 
                            "Все смены за ${clearMonthYear} год будут удалены."
                        else 
                            "Все смены за выбранный месяц будут удалены."
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Это действие нельзя отменить!",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isClearingYear) {
                            vm.clearYear(clearMonthYear)
                            Toast.makeText(context, "Год ${clearMonthYear} очищен", Toast.LENGTH_SHORT).show()
                        } else {
                            vm.clearMonth(clearMonthYear, clearMonthMonth)
                            Toast.makeText(context, "Месяц очищен", Toast.LENGTH_SHORT).show()
                        }
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.DeleteForever, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Очистить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirm = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

// ─── TimePicker диалог ────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = remember(initial) { initial.split(":").map { it.toIntOrNull() ?: 0 } }
    val state = rememberTimePickerState(
        initialHour   = parts.getOrElse(0) { 8 },
        initialMinute = parts.getOrElse(1) { 0 },
        is24Hour      = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large) { Text("Отмена") }
                    Button(
                        onClick = {
                            val h = "%02d".format(state.hour)
                            val m = "%02d".format(state.minute)
                            onConfirm("$h:$m")
                        },
                        modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large
                    ) { Text("Готово") }
                }
            }
        }
    }
}

// ─── Чип времени (кликабельный) ───────────────────────────────
@Composable
private fun TimeChip(time: String, onClick: () -> Unit) {
    val dimens = rememberAdaptiveDimens()
    Box(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = if (dimens.isCompact) 8.dp else 10.dp, vertical = 6.dp),
        Alignment.Center
    ) {
        Text(time, style = MaterialTheme.typography.titleSmall.copy(fontSize = dimens.bodyFontSize),
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

// ─── Баннер разрешения уведомлений ───────────────────────────
@Composable
private fun NotifPermissionBanner(context: Context) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
    val hasPermission = remember {
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    if (hasPermission) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFf97316).copy(alpha = 0.15f)),
        shape = MaterialTheme.shapes.large
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Warning, null, tint = Color(0xFFf97316), modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Разрешите уведомления", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, color = Color(0xFFf97316))
                Text("Без разрешения напоминания не придут",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    })
                }
            ) { Text("Открыть") }
        }
    }
}

// ─── Реальный экспорт JSON ────────────────────────────────────
private fun doExportJson(context: Context, vm: SettingsViewModel) {
    try {
        val dir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        val file = File(dir, "moygrafik_backup_$date.json")
        
        // Получаем все смены для активного профиля
        val profileId = vm.settings.value.activeProfileId
        val allShifts = runBlocking {
            vm.repository.getAllShiftsForProfile(profileId).first()
        }
        
        val data = mapOf(
            "version"  to 2,
            "exported" to date,
            "settings" to vm.settings.value,
            "times"    to vm.shiftTimes.value,
            "shifts"   to allShifts
        )
        file.writeText(Gson().toJson(data))
        Toast.makeText(context, "✓ Сохранено: ${file.name}", Toast.LENGTH_LONG).show()
        // Открыть файл-менеджер
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(dir), "resource/folder")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// ─── Восстановление из JSON ───────────────────────────────────
private fun doRestore(context: Context, uri: Uri, vm: SettingsViewModel) {
    try {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            ?: throw Exception("Не удалось прочитать файл")
        
        val gson = Gson()
        val map = gson.fromJson(json, Map::class.java)
        
        if (map["version"] == null) throw Exception("Неверный формат файла")
        
        // Восстановить настройки
        val settingsData = map["settings"]
        if (settingsData != null) {
            val settingsJson = gson.toJson(settingsData)
            val settings = gson.fromJson(settingsJson, AppSettings::class.java)
            vm.setThemeMode(ThemeMode.valueOf(settings.themeMode))
            vm.setFontScale(settings.fontScale)
            vm.setFontLocked(settings.fontLocked)
            vm.setIsLocked(settings.isLocked)
            vm.setDynamicColor(settings.dynamicColor)
            vm.setBreakMinutes(settings.breakMinutes)
            vm.setCloudBackup(settings.cloudBackupEnabled, settings.cloudBackupUri)
            vm.setAutoBackup(settings.autoBackupEnabled, settings.autoBackupFrequency)
            vm.saveWageSettings(settings.hourlyRate, settings.nightCoeff, settings.holidayCoeff, settings.sickCoeff)
        }
        
        // Восстановить время смен
        val timesData = map["times"]
        if (timesData != null) {
            val type = object : TypeToken<List<ShiftTime>>() {}.type
            val times: List<ShiftTime> = gson.fromJson(gson.toJson(timesData), type)
            vm.restoreTimes(times)
        }
        
        // Восстановить смены (график)
        val shiftsData = map["shifts"]
        if (shiftsData != null) {
            val type = object : TypeToken<List<ShiftEntry>>() {}.type
            val shifts: List<ShiftEntry> = gson.fromJson(gson.toJson(shiftsData), type)
            vm.restoreShifts(shifts)
        }
        
        Toast.makeText(context, "✓ График и настройки восстановлены", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// ─── Экспорт ICS для Google Календаря ───────────────────────
// ─── Переиспользуемые компоненты ──────────────────────────────
@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val dimens = rememberAdaptiveDimens()
    Card(
        Modifier.fillMaxWidth().padding(horizontal = dimens.horizontalPadding),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape  = MaterialTheme.shapes.extraLarge
    ) { Column(Modifier.padding(dimens.cardPadding), content = content) }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    val dimens = rememberAdaptiveDimens()
    Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(dimens.iconSizeSmall - 2.dp))
        Text(title, style = MaterialTheme.typography.labelMedium.copy(fontSize = dimens.labelFontSize),
            fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
    }
}

@Composable
private fun WageField(label: String, value: String, onValueChange: (String) -> Unit) {
    val dimens = rememberAdaptiveDimens()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.bodyFontSize), modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.width(if (dimens.isCompact) 80.dp else 100.dp), singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.bodyFontSize),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium
        )
    }
}

@Composable
private fun NotifToggleRow(
    icon: ImageVector, iconBg: Color,
    title: String, subtitle: String,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(iconBg.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconBg, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BackupBtn(icon: ImageVector, label: String, onClick: () -> Unit) {
    val dimens = rememberAdaptiveDimens()
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = if (dimens.isCompact) 12.dp else 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, Modifier.size(dimens.iconSizeSmall), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.bodyFontSize))
        Spacer(Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(dimens.iconSizeSmall - 2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
