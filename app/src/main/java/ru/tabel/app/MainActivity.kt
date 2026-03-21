package ru.tabel.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import ru.tabel.app.data.model.*
import ru.tabel.app.ui.calendar.CalendarScreen
import ru.tabel.app.ui.onboarding.OnboardingScreen
import ru.tabel.app.ui.settings.SettingsScreen
import ru.tabel.app.ui.profile.ProfileScreen
import ru.tabel.app.ui.shifts.ShiftsScreen
import ru.tabel.app.ui.stats.StatsScreen
import ru.tabel.app.ui.theme.TabelTheme

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "Календарь", Icons.Rounded.CalendarMonth)
    object Shifts   : Screen("shifts",   "Смены",     Icons.Rounded.List)
    object Stats    : Screen("stats",    "Статистика", Icons.Rounded.BarChart)
    object Settings : Screen("settings", "Настройки",  Icons.Rounded.Settings)
    object Profiles : Screen("profiles", "Профили",    Icons.Rounded.People)
}
val SCREENS = listOf(Screen.Calendar, Screen.Shifts, Screen.Stats, Screen.Settings)

// ── SharedPreferences helpers ─────────────────────────────────
private fun Context.getAppPrefs() = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
private fun Context.isOnboardingDone() = getAppPrefs().getBoolean("onboarding_done", false)
private fun Context.setOnboardingDone() = getAppPrefs().edit().putBoolean("onboarding_done", true).apply()
private fun Context.getInstallDay() = getAppPrefs().getLong("install_day", 0L)
private fun Context.setInstallDay(day: Long) = getAppPrefs().edit().putLong("install_day", day).apply()
private fun Context.isRatingDone() = getAppPrefs().getBoolean("rating_done", false)
private fun Context.setRatingDone() = getAppPrefs().edit().putBoolean("rating_done", true).apply()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // Держим сплэш пока ViewModel не загрузит настройки (избегаем мигания)
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Плавное исчезновение: масштаб + прозрачность
        splashScreen.setOnExitAnimationListener { splashProvider ->
            val splashView = splashProvider.view
            val iconView   = splashProvider.iconView

            // Иконка улетает вверх и уменьшается
            iconView.animate()
                .translationY(-60f)
                .scaleX(0.7f)
                .scaleY(0.7f)
                .alpha(0f)
                .setDuration(400)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .start()

            // Фон растворяется
            splashView.animate()
                .alpha(0f)
                .setStartDelay(100)
                .setDuration(350)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction { splashProvider.remove() }
                .start()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Запоминаем день установки
        if (getInstallDay() == 0L) {
            setInstallDay(System.currentTimeMillis())
        }

        setContent {
            val vm: SettingsViewModel = hiltViewModel()
            val settings by vm.settings.collectAsState(initial = AppSettings())

            // Снимаем блокировку сплэша как только получили настройки
            LaunchedEffect(settings) { isReady = true }
            val systemDark = isSystemInDarkTheme()
            val themeMode = runCatching { ThemeMode.valueOf(settings.themeMode) }.getOrDefault(ThemeMode.SYSTEM)
            val isDark = when (themeMode) {
                ThemeMode.DARK   -> true
                ThemeMode.LIGHT  -> false
                ThemeMode.SYSTEM -> systemDark
            }
            val currentDensity = LocalDensity.current
            val scaledDensity = remember(settings.fontScale) {
                Density(density = currentDensity.density, fontScale = settings.fontScale)
            }
            TabelTheme(darkTheme = isDark, dynamicColor = settings.dynamicColor) {
                CompositionLocalProvider(LocalDensity provides scaledDensity) {
                    // Показываем кастомный сплэш поверх приложения при первом запуске
                    var splashDone by remember { mutableStateOf(false) }
                    if (!splashDone) {
                        ru.tabel.app.ui.splash.SplashScreen(
                            onFinished = { splashDone = true }
                        )
                    } else {
                        AppRoot(
                            onExit             = { finish() },
                            onboardingDone     = isOnboardingDone(),
                            onFinishOnboarding = { setOnboardingDone() },
                            installDay         = getInstallDay(),
                            ratingDone         = isRatingDone(),
                            onRatingDone       = { setRatingDone() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    onExit: () -> Unit,
    onboardingDone: Boolean,
    onFinishOnboarding: () -> Unit,
    installDay: Long,
    ratingDone: Boolean,
    onRatingDone: () -> Unit
) {
    var showOnboarding by remember { mutableStateOf(!onboardingDone) }

    if (showOnboarding) {
        OnboardingScreen(onFinish = {
            onFinishOnboarding()
            showOnboarding = false
        })
        return
    }

    // Диалог оценки — через 7 дней после установки
    val context = LocalContext.current
    var showRatingDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!ratingDone && installDay > 0L) {
            val daysPassed = (System.currentTimeMillis() - installDay) / (1000L * 60 * 60 * 24)
            if (daysPassed >= 7) showRatingDialog = true
        }
    }

    if (showRatingDialog) {
        RatingDialog(
            onRate = {
                onRatingDone()
                showRatingDialog = false
                // Открываем RuStore / Play Store
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            },
            onLater = { showRatingDialog = false },
            onNever = { onRatingDone(); showRatingDialog = false }
        )
    }

    TabelApp(onExit = onExit)
}

@Composable
private fun RatingDialog(
    onRate: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        icon  = { Text("⭐", style = MaterialTheme.typography.displaySmall) },
        title = {
            Text("Нравится приложение?",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge)
        },
        text  = {
            Text("Оцени «Мой График» — это помогает другим найти приложение и нам его улучшать.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        confirmButton = {
            Button(
                onClick  = onRate,
                modifier = Modifier.fillMaxWidth(),
                shape    = MaterialTheme.shapes.large
            ) {
                Text("⭐ Оценить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = onLater,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large
                ) { Text("Напомнить позже") }
                TextButton(
                    onClick  = onNever,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Не показывать",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
fun TabelApp(onExit: () -> Unit) {
    val navController = rememberNavController()
    var showExitDialog by remember { mutableStateOf(false) }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    BackHandler(enabled = currentRoute == Screen.Calendar.route) { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon  = { Icon(Icons.Rounded.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Выйти из приложения?", fontWeight = FontWeight.Bold) },
            text  = { Text("Все данные сохранены.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick  = onExit,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape    = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.ExitToApp, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Выйти", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showExitDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = MaterialTheme.shapes.large
                ) { Text("Остаться") }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    Scaffold(
        modifier  = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(tonalElevation = 0.dp) {
                val currentDest = backStack?.destination
                SCREENS.forEach { screen ->
                    val selected = currentDest?.hierarchy?.any { it.route == screen.route } == true
                    val iconScale by animateFloatAsState(
                        targetValue   = if (selected) 1.18f else 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
                        label         = "tab_${screen.route}"
                    )
                    NavigationBarItem(
                        icon     = { Icon(screen.icon, screen.label, Modifier.scale(iconScale)) },
                        label    = { Text(screen.label) },
                        selected = selected,
                        onClick  = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Calendar.route,
            modifier         = Modifier.padding(padding),
            enterTransition    = { fadeIn(tween(350)) + scaleIn(tween(350, easing = FastOutSlowInEasing), initialScale = 0.95f) },
            exitTransition     = { fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.98f) },
            popEnterTransition = { fadeIn(tween(350)) + scaleIn(tween(350, easing = FastOutSlowInEasing), initialScale = 0.95f) },
            popExitTransition  = { fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.98f) }
        ) {
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Shifts.route)   { ShiftsScreen() }
            composable(Screen.Stats.route)    { StatsScreen() }
            composable(Screen.Settings.route) { SettingsScreen(onNavigateToProfiles = { navController.navigate(Screen.Profiles.route) }) }
            composable(Screen.Profiles.route) { ProfileScreen() }
        }
    }
}
