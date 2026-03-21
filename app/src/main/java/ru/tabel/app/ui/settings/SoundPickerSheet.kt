package ru.tabel.app.ui.settings

import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.tabel.app.notifications.TabelNotificationManager

data class SoundOption(
    val id: String,
    val label: String,
    val uri: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPickerSheet(
    currentSound: String,
    notifManager: TabelNotificationManager,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current
    var playingId by remember { mutableStateOf<String?>(null) }

    // Загружаем системные звуки уведомлений
    val systemSounds = remember {
        val list = mutableListOf(
            SoundOption("silent",  "🔕 Без звука",       "silent"),
            SoundOption("default", "🔔 По умолчанию",    "default")
        )
        runCatching {
            val rm = RingtoneManager(context)
            rm.setType(RingtoneManager.TYPE_NOTIFICATION)
            val cursor = rm.cursor
            while (cursor.moveToNext() && list.size < 20) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri   = rm.getRingtoneUri(cursor.position).toString()
                val id    = "sys_${cursor.position}"
                list.add(SoundOption(id, title, uri))
            }
        }
        list
    }

    // Лаунчер для выбора своего звука из файлов
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Даём постоянный доступ
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onSelect(it.toString())
            notifManager.previewSound(it.toString())
            playingId = "custom"
        }
    }

    DisposableEffect(Unit) {
        onDispose { notifManager.stopPreview() }
    }

    ModalBottomSheet(
        onDismissRequest = { notifManager.stopPreview(); onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Звук уведомления",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 4.dp))
            Text("Нажмите ▶ для предпрослушивания",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp))

            // Кнопка выбора своего звука
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        fileLauncher.launch("audio/*")
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.FolderOpen, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Выбрать свой звук",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Из файлов телефона (MP3, OGG, WAV)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
            }

            // Показываем выбранный пользовательский звук если он есть
            if (currentSound != "silent" && currentSound != "default" &&
                systemSounds.none { it.uri == currentSound }) {
                Spacer(Modifier.height(8.dp))
                SoundRow(
                    sound     = SoundOption("custom", "✅ Выбранный файл", currentSound),
                    isSelected = true,
                    isPlaying  = playingId == "custom",
                    onClick   = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(currentSound)
                        playingId = "custom"
                        notifManager.previewSound(currentSound)
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            Text("Системные звуки",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                items(systemSounds, key = { it.id }) { sound ->
                    SoundRow(
                        sound      = sound,
                        isSelected = currentSound == sound.uri,
                        isPlaying  = playingId == sound.id,
                        onClick    = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(sound.uri)
                            if (sound.id == "silent") {
                                notifManager.stopPreview()
                                playingId = null
                            } else {
                                playingId = sound.id
                                notifManager.previewSound(sound.uri)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { notifManager.stopPreview(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Выбрать", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SoundRow(
    sound: SoundOption,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color  = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape  = MaterialTheme.shapes.large
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    sound.id == "silent" -> Icons.Rounded.NotificationsOff
                    isPlaying            -> Icons.Rounded.Stop
                    else                 -> Icons.Rounded.PlayArrow
                },
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            sound.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(Icons.Rounded.Check, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp))
        }
    }
}
