package ru.tabel.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingPage(
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val subtitle: String
)

private val PAGES = listOf(
    OnboardingPage(
        icon      = Icons.Rounded.CalendarMonth,
        iconColor = Color(0xFF4F6EF7),
        title     = "Твой график\nв одном месте",
        subtitle  = "Дневные, ночные, выходные — всё видно на одном экране. Никаких бумажек."
    ),
    OnboardingPage(
        icon      = Icons.Rounded.AutoAwesome,
        iconColor = Color(0xFF8B5CF6),
        title     = "Автозаполнение\nза секунду",
        subtitle  = "Выбери шаблон 2/2, сутки/трое или свой — и график заполнится автоматически на месяц или год."
    ),
    OnboardingPage(
        icon      = Icons.Rounded.Notifications,
        iconColor = Color(0xFFf97316),
        title     = "Никогда не\nпропусти смену",
        subtitle  = "Уведомления за день или за несколько часов до начала. Виджет на экране телефона."
    ),
    OnboardingPage(
        icon      = Icons.Rounded.CurrencyRuble,
        iconColor = Color(0xFF22c55e),
        title     = "Считает зарплату\nавтоматически",
        subtitle  = "Укажи ставку — приложение само посчитает дневные, ночные и праздничные часы."
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val current = PAGES[page]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Кнопка пропустить
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (page < PAGES.size - 1) {
                    TextButton(onClick = onFinish) {
                        Text("Пропустить", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.height(40.dp))
                }
            }

            // Иконка + текст с анимацией
            AnimatedContent(
                targetState  = page,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 })
                        .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 })
                },
                label = "onboarding_page"
            ) { p ->
                val pg = PAGES[p]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(pg.iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pg.icon,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = pg.iconColor
                        )
                    }
                    Spacer(Modifier.height(40.dp))
                    Text(
                        text       = pg.title,
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign  = TextAlign.Center,
                        lineHeight = 36.sp,
                        color      = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text      = pg.subtitle,
                        style     = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        lineHeight = 24.sp
                    )
                }
            }

            // Точки прогресса + кнопка
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    PAGES.indices.forEach { i ->
                        val selected = i == page
                        val width by animateDpAsState(
                            targetValue   = if (selected) 24.dp else 8.dp,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy),
                            label         = "dot_$i"
                        )
                        Box(
                            Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (selected) current.iconColor
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                Button(
                    onClick = { if (page < PAGES.size - 1) page++ else onFinish() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape  = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(containerColor = current.iconColor)
                ) {
                    Text(
                        text       = if (page < PAGES.size - 1) "Далее" else "Начать",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 18.sp
                    )
                    if (page < PAGES.size - 1) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
