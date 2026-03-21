package ru.tabel.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.tabel.app.R

private val BG = Color(0xFF0D0D1C)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startRipples by remember { mutableStateOf(false) }
    var showIcon     by remember { mutableStateOf(false) }
    var showText     by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(80);   startRipples = true
        delay(180);  showIcon     = true
        delay(280);  showText     = true
        delay(1400); onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(BG),
        contentAlignment = Alignment.Center
    ) {
        // Расходящиеся круги
        if (startRipples) {
            RippleRing(0,   Color(0xFF4ADBA2), 5.5f)
            RippleRing(160, Color(0xFF2DD4BF), 3.8f)
            RippleRing(320, Color(0xFF4ADBA2), 2.4f)
        }

        // Иконка
        val iconScale by animateFloatAsState(
            targetValue   = if (showIcon) 1f else 0f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
            label = "iconScale"
        )
        val iconAlpha by animateFloatAsState(
            targetValue   = if (showIcon) 1f else 0f,
            animationSpec = tween(300),
            label = "iconAlpha"
        )

        Box(
            modifier = Modifier
                .size(130.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha  = iconAlpha
                }
                .clip(RoundedCornerShape(30.dp))
        ) {
            androidx.compose.foundation.Image(
                painter            = painterResource(id = R.drawable.splash_logo),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
        }

        // Текст снизу
        val textOffset by animateFloatAsState(
            targetValue   = if (showText) 0f else 50f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
            label = "textOffset"
        )
        val textAlpha by animateFloatAsState(
            targetValue   = if (showText) 1f else 0f,
            animationSpec = tween(400),
            label = "textAlpha"
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .graphicsLayer { translationY = textOffset; alpha = textAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text       = "Мой График",
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center
            )
            Text(
                text      = "расписание смен",
                fontSize  = 14.sp,
                color     = Color(0xFF9090AA),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RippleRing(delayMs: Int, color: Color, maxScale: Float) {
    val inf = rememberInfiniteTransition(label = "ripple_$delayMs")
    val scale by inf.animateFloat(
        initialValue  = 0.3f,
        targetValue   = maxScale,
        animationSpec = infiniteRepeatable(
            tween(1500, delayMillis = delayMs, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        ), label = "s"
    )
    val alpha by inf.animateFloat(
        initialValue  = 0.30f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            tween(1500, delayMillis = delayMs, easing = LinearEasing),
            RepeatMode.Restart
        ), label = "a"
    )
    Box(
        Modifier
            .size(100.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}
