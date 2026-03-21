package ru.tabel.app.ui.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import ru.tabel.app.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifSheet(
    onDismiss: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val context  = LocalContext.current
    val haptic   = LocalHapticFeedback.current
    val settings by vm.settings.collectAsState()

    // Локальное состояние
    var notifHrs by remember(settings.notifHoursBefore) { mutableStateOf(settings.notifHoursBefore) }
    var curSound by remember(settings.notifSound)       { mutableStateOf(settings.notifSound) }
    var showSound by remember { mutableStateOf(false) }

    val soundLabel = when (curSound) {
        "default" -> "По умолчанию"
        "silent"  -> "Без звука"
        else      -> "Пользовательский"
    }

    val isEnabled = notifHrs > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        tonalElevation   = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Заголовок ──────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(Color(0xFFf97316).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Notifications, null,
                        tint = Color(0xFFf97316), modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Уведомления о сменах",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold)
                    Text("Напомню перед началом смены",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Баннер разрешения Android 13+ ─────────────────
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                val granted = context.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFf97316).copy(alpha = 0.12f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Warning, null,
                                tint = Color(0xFFf97316), modifier = Modifier.size(16.dp))
                            Text("Уведомления отключены в системе",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }) { Text("Включить", color = Color(0xFFf97316)) }
                        }
                    }
                }
            }

            // ── Точные будильники Android 12+ ─────────────────
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val canExact = remember { vm.notifManager.canScheduleExact() }
                if (!canExact) {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Alarm, null,
                                tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                            Text("Для точных уведомлений нужно разрешение",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { vm.notifManager.openExactAlarmSettings() }) {
                                Text("Разрешить", color = Color(0xFF8B5CF6))
                            }
                        }
                    }
                }
            }

            // ── Главная карточка выбора времени ───────────────
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(Color(0xFF4F6EF7).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Schedule, null,
                                tint = Color(0xFF4F6EF7), modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("За сколько часов напомнить",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold)
                            Text(
                                if (notifHrs == 0) "Уведомления отключены"
                                else {
                                    val w = when {
                                        notifHrs == 1 -> "час"
                                        notifHrs in 2..4 -> "часа"
                                        else -> "часов"
                                    }
                                    "За $notifHrs $w до начала смены"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (notifHrs > 0) Color(0xFF4F6EF7)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Кнопки выбора
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Выкл", 1 to "1ч", 2 to "2ч",
                               3 to "3ч", 6 to "6ч", 12 to "12ч").forEach { (h, label) ->
                            val sel = notifHrs == h
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(
                                        if (sel) Color(0xFF4F6EF7)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = if (sel) 0.dp else 1.dp,
                                        color = if (sel) Color.Transparent
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = MaterialTheme.shapes.large
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        notifHrs = h
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Пример когда придёт уведомление
                    AnimatedVisibility(
                        visible = isEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit  = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(Color(0xFF4F6EF7).copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Info, null,
                                tint = Color(0xFF4F6EF7).copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp))
                            val w = when {
                                notifHrs == 1 -> "час"
                                notifHrs in 2..4 -> "часа"
                                else -> "часов"
                            }
                            Text(
                                "Например: смена в 08:00 → уведомление в ${
                                    "%02d:00".format((8 - notifHrs).let { if (it < 0) it + 24 else it })
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4F6EF7)
                            )
                        }
                    }
                }
            }

            // ── Звук ───────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSound = true
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(Color(0xFF22c55e).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, null,
                            tint = Color(0xFF22c55e), modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Звук уведомления",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold)
                        Text(soundLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF22c55e))
                    }
                    Icon(Icons.Rounded.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }

            // ── Кнопка сохранить ───────────────────────────────
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.saveNotifSettings(notifHrs, curSound)
                    val msg = if (notifHrs == 0) "Уведомления отключены"
                              else {
                                  val w = when {
                                      notifHrs == 1 -> "час"
                                      notifHrs in 2..4 -> "часа"
                                      else -> "часов"
                                  }
                                  "✓ Буду напоминать за $notifHrs $w"
                              }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Сохранить", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showSound) {
        SoundPickerSheet(
            currentSound = curSound,
            notifManager = vm.notifManager,
            onSelect     = { curSound = it },
            onDismiss    = { showSound = false }
        )
    }
}
