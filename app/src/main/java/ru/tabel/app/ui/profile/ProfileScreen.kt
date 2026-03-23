package ru.tabel.app.ui.profile

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.tabel.app.ui.theme.rememberAdaptiveDimens

// ── Хранение состояния блокировки ─────────────────────────────
private fun saveLock(ctx: Context, profileId: String, locked: Boolean) {
    ctx.getSharedPreferences("profile_locks", Context.MODE_PRIVATE)
        .edit().putBoolean("locked_$profileId", locked).apply()
}
private fun isLocked(ctx: Context, profileId: String): Boolean =
    ctx.getSharedPreferences("profile_locks", Context.MODE_PRIVATE)
        .getBoolean("locked_$profileId", false)

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val context  = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val active   by viewModel.activeProfile.collectAsState()
    val dimens  = rememberAdaptiveDimens()

    // Состояние блокировок — загружается из SharedPreferences
    var lockedIds by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(profiles) {
        lockedIds = profiles.map { it.id }.filter { isLocked(context, it) }.toSet()
    }

    // Диалоги
    var showAdd      by remember { mutableStateOf(false) }
    var newName      by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ru.tabel.app.data.model.Profile?>(null) }
    var renameTarget by remember { mutableStateOf<ru.tabel.app.data.model.Profile?>(null) }
    var renameName   by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(dimens.horizontalPadding)) {
        // Заголовок
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Профили",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = dimens.titleFontSize),
                fontWeight = FontWeight.Black)
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, "Добавить", modifier = Modifier.size(dimens.iconSizeMedium))
            }
        }

        // Подсказка
        Row(
            Modifier.fillMaxWidth().padding(bottom = dimens.cardPadding)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = dimens.cardPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Info, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(dimens.iconSizeSmall - 4.dp))
            Text("✏️ переименовать  ·  🔒 защита от удаления",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = dimens.labelFontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(profiles) { profile ->
                val locked = profile.id in lockedIds
                ProfileCard(
                    dimens   = dimens,
                    profile   = profile,
                    isActive  = profile.id == active?.id,
                    isLocked  = locked,
                    onSelect  = { viewModel.selectProfile(profile) },
                    onToggleLock = {
                        val newLocked = !locked
                        saveLock(context, profile.id, newLocked)
                        lockedIds = if (newLocked) lockedIds + profile.id
                                    else           lockedIds - profile.id
                    },
                    onRename = {
                        renameTarget = profile
                        renameName   = profile.name
                    },
                    onDelete = {
                        if (profiles.size > 1 && !locked) deleteTarget = profile
                    }
                )
            }
        }
    }

    // ── Диалог переименования ─────────────────────────────────
    renameTarget?.let { profile ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            shape = MaterialTheme.shapes.extraLarge,
            icon = {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Edit, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                }
            },
            title = { Text("Переименовать профиль", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value         = renameName,
                    onValueChange = { renameName = it },
                    label         = { Text("Новое название") },
                    singleLine    = true,
                    shape         = MaterialTheme.shapes.large,
                    modifier      = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameName.isNotBlank()) {
                            viewModel.renameProfile(profile, renameName.trim())
                        }
                        renameTarget = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large,
                    enabled  = renameName.isNotBlank()
                ) { Text("Сохранить", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { renameTarget = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large
                ) { Text("Отмена") }
            }
        )
    }

    // ── Диалог подтверждения удаления ─────────────────────────
    deleteTarget?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            shape = MaterialTheme.shapes.extraLarge,
            icon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить профиль?", fontWeight = FontWeight.Bold) },
            text  = {
                Text("Профиль «${profile.name}» и все его смены будут удалены безвозвратно.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                Button(
                    onClick  = { viewModel.deleteProfile(profile); deleteTarget = null },
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Удалить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { deleteTarget = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large
                ) { Text("Отмена") }
            }
        )
    }

    // ── Диалог создания профиля ───────────────────────────────
    if (showAdd) {
        var lockOnCreate by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAdd = false; newName = "" },
            shape = MaterialTheme.shapes.extraLarge,
            icon = {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PersonAdd, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                }
            },
            title = { Text("Новый профиль", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value         = newName,
                        onValueChange = { newName = it },
                        label         = { Text("Название профиля") },
                        singleLine    = true,
                        shape         = MaterialTheme.shapes.large,
                        modifier      = Modifier.fillMaxWidth(),
                        leadingIcon   = { Icon(Icons.Rounded.Person, null) }
                    )
                    // Опция сразу заблокировать
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                if (lockOnCreate)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (lockOnCreate) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            null,
                            tint = if (lockOnCreate) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Защитить от удаления",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                            Text("Кнопка удаления будет скрыта",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = lockOnCreate, onCheckedChange = { lockOnCreate = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.createProfile(newName.trim())
                            if (lockOnCreate) {
                                // Ищем только что созданный профиль и блокируем
                                val newProfile = profiles.firstOrNull { it.name == newName.trim() }
                                if (newProfile != null) {
                                    saveLock(context, newProfile.id, true)
                                    lockedIds = lockedIds + newProfile.id
                                }
                            }
                            showAdd      = false
                            newName      = ""
                            lockOnCreate = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large,
                    enabled  = newName.isNotBlank()
                ) { Text("Создать", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAdd = false; newName = ""; lockOnCreate = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("Отмена") }
            }
        )
    }
}

// ── Карточка профиля ─────────────────────────────────────────
@Composable
private fun ProfileCard(
    dimens:      ru.tabel.app.ui.theme.AdaptiveDimens,
    profile:      ru.tabel.app.data.model.Profile,
    isActive:     Boolean,
    isLocked:     Boolean,
    onSelect:     () -> Unit,
    onToggleLock: () -> Unit,
    onRename:     () -> Unit,
    onDelete:     () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else Color.Transparent,
        tween(200), label = "border"
    )
    val lockScale by animateFloatAsState(
        if (isLocked) 1.2f else 1f,
        spring(Spring.DampingRatioMediumBouncy), label = "lock"
    )

    Card(
        onClick = onSelect,
        shape   = MaterialTheme.shapes.extraLarge,
        colors  = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.border(
            if (isActive) 2.dp else 0.dp,
            borderColor, MaterialTheme.shapes.extraLarge
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(dimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (dimens.isCompact) 6.dp else 8.dp)
        ) {
            // Аватар
            Box(
                Modifier.size(if (dimens.isCompact) 36.dp else 42.dp).clip(CircleShape).background(
                    if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(profile.name.take(1).uppercase(),
                    color = if (isActive) Color.White
                            else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold, fontSize = if (dimens.isCompact) 14.sp else 16.sp)
            }

            // Имя + статус
            Column(Modifier.weight(1f)) {
                Text(profile.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.titleFontSize),
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                if (isActive) {
                    Text("Активный",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.labelFontSize),
                        color = MaterialTheme.colorScheme.primary)
                }
                AnimatedVisibility(isLocked,
                    enter = fadeIn() + expandVertically(),
                    exit  = fadeOut() + shrinkVertically()
                ) {
                    Text("🔒 Защищён",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (dimens.labelFontSize.value - 1).sp),
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            // ✏️ Переименовать
            IconButton(onClick = onRename, modifier = Modifier.size(if (dimens.isCompact) 32.dp else 36.dp)) {
                Icon(Icons.Rounded.Edit, "Переименовать",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(dimens.iconSizeSmall - 2.dp))
            }

            // 🔒 Замок
            IconButton(onClick = onToggleLock, modifier = Modifier.size(if (dimens.isCompact) 32.dp else 36.dp)) {
                Icon(
                    if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    if (isLocked) "Разблокировать" else "Заблокировать",
                    tint = if (isLocked) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(dimens.iconSizeSmall).scale(lockScale)
                )
            }

            // 🗑️ Удалить — скрыта если заблокирован или активный
            AnimatedVisibility(
                !isActive && !isLocked,
                enter = fadeIn() + scaleIn(spring(Spring.DampingRatioMediumBouncy)),
                exit  = fadeOut() + scaleOut()
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(if (dimens.isCompact) 32.dp else 36.dp)) {
                    Icon(Icons.Rounded.Delete, "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(dimens.iconSizeSmall - 2.dp))
                }
            }
        }
    }
}
