package ru.tabel.app.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// 1. PRESS SCALE — элемент «проваливается» при нажатии
//    Modifier.pressScale { doSomething() }
// ─────────────────────────────────────────────────────────────
fun Modifier.pressScale(
    scaleTo: Float = 0.92f,
    onClick: () -> Unit
): Modifier = composed {
    val source  = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale   by animateFloatAsState(
        targetValue   = if (pressed) scaleTo else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "pressScale"
    )
    this
        .scale(scale)
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

// ─────────────────────────────────────────────────────────────
// 2. RIPPLE SCALE — нажатие + круговая волна (без стандартного ripple)
//    Modifier.rippleScale(color) { doSomething() }
// ─────────────────────────────────────────────────────────────
fun Modifier.rippleScale(
    rippleColor: Color = Color(0x334F6EF7),
    scaleTo: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    var rippleAnim     by remember { mutableStateOf<Animatable<Float, AnimationVector1D>?>(null) }
    var rippleCenter   by remember { mutableStateOf(Offset.Zero) }
    val scale          by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "rippleScale"
    )

    this
        .scale(scale)
        .drawBehind {
            rippleAnim?.let { anim ->
                val radius = anim.value * maxOf(size.width, size.height) * 0.7f
                drawCircle(
                    color  = rippleColor.copy(alpha = (1f - anim.value) * 0.5f),
                    radius = radius,
                    center = rippleCenter
                )
            }
        }
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = { offset ->
                    rippleCenter = offset
                    coroutineScope.launch {
                        val anim = Animatable(0f)
                        rippleAnim = anim
                        anim.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                        rippleAnim = null
                    }
                    tryAwaitRelease()
                },
                onTap = { onClick() }
            )
        }
}

// ─────────────────────────────────────────────────────────────
// 3. PULSE — лёгкое биение (сегодняшняя ячейка)
// ─────────────────────────────────────────────────────────────
@Composable
fun pulseScale(): Float {
    val t = rememberInfiniteTransition(label = "pulse")
    val v by t.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.07f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulseFloat"
    )
    return v
}

// ─────────────────────────────────────────────────────────────
// 4. SHIMMER — скелетон-загрузка
//    Box(Modifier.shimmer())
// ─────────────────────────────────────────────────────────────
@Composable
fun Modifier.shimmer(
    baseColor:     Color = Color(0xFF1A1A2E),
    highlightColor: Color = Color(0xFF2A2A4E)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue  = -1f,
        targetValue   = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label         = "shimmerOffset"
    )
    this.drawBehind {
        drawShimmer(offset, baseColor, highlightColor)
    }
}

private fun DrawScope.drawShimmer(
    offset: Float,
    base: Color,
    highlight: Color
) {
    drawRect(base)
    val x  = size.width * offset
    val w  = size.width * 0.4f
    if (x + w >= 0 && x <= size.width) {
        drawRect(
            color     = highlight,
            topLeft   = Offset((x - w).coerceAtLeast(0f), 0f),
            size      = androidx.compose.ui.geometry.Size(
                w.coerceAtMost(size.width - (x - w).coerceAtLeast(0f)), size.height
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 5. COUNTER — число плавно анимируется при изменении
// ─────────────────────────────────────────────────────────────
@Composable
fun animatedInt(target: Int): Int {
    val v by animateIntAsState(
        targetValue   = target,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "animInt"
    )
    return v
}

// ─────────────────────────────────────────────────────────────
// 6. BOUNCE ENTRY — появление снизу с пружиной
// ─────────────────────────────────────────────────────────────
fun bounceEnter(delay: Int = 0): EnterTransition =
    fadeIn(tween(280, delay)) +
    slideInVertically(
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        initialOffsetY = { it / 4 }
    )

// ─────────────────────────────────────────────────────────────
// 7. STAGGER DELAY — задержка появления элементов списка
//    val visible = staggerDelay(index = idx)
//    AnimatedVisibility(visible) { ... }
// ─────────────────────────────────────────────────────────────
@Composable
fun staggerDelay(index: Int, baseMs: Int = 50): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * baseMs).toLong())
        visible = true
    }
    return visible
}

// ─────────────────────────────────────────────────────────────
// 8. MORPH COLOR — плавная смена цвета фона
// ─────────────────────────────────────────────────────────────
@Composable
fun morphColor(target: Color, durationMs: Int = 300): Color {
    val animated by animateColorAsState(
        targetValue   = target,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label         = "morphColor"
    )
    return animated
}

// ─────────────────────────────────────────────────────────────
// 9. BREATHING — бесконечное медленное дыхание (accent badge)
// ─────────────────────────────────────────────────────────────
@Composable
fun breathingAlpha(
    minAlpha: Float = 0.6f,
    maxAlpha: Float = 1.0f,
    durationMs: Int = 2000
): Float {
    val t = rememberInfiniteTransition(label = "breathing")
    val v by t.animateFloat(
        initialValue  = minAlpha,
        targetValue   = maxAlpha,
        animationSpec = infiniteRepeatable(
            tween(durationMs, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breathAlpha"
    )
    return v
}

// ─────────────────────────────────────────────────────────────
// 10. SLIDE COUNTER — число меняется со слайдом вверх/вниз
//     Лучший способ показать изменение счётчика (как в спортивном табло)
// ─────────────────────────────────────────────────────────────
@Composable
fun SlideCounter(
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    AnimatedContent(
        targetState   = count,
        modifier      = modifier,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { -it } + fadeIn()) togetherWith
                (slideOutVertically { it } + fadeOut())
            } else {
                (slideInVertically { it } + fadeIn()) togetherWith
                (slideOutVertically { -it } + fadeOut())
            }
        },
        label = "slideCounter"
    ) { value ->
        content(value)
    }
}

// ─────────────────────────────────────────────────────────────
// 11. EXPAND HORIZONTALLY с пружиной — для прогресс-баров
// ─────────────────────────────────────────────────────────────
@Composable
fun animatedFraction(
    target: Float,
    durationMs: Int = 800,
    delayMs: Int = 200
): Float {
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        triggered = true
    }
    val v by animateFloatAsState(
        targetValue   = if (triggered) target else 0f,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label         = "animFraction"
    )
    return v
}

// ─────────────────────────────────────────────────────────────
// 12. SHARED TRANSITION SPEC — единые спецификации для проекта
// ─────────────────────────────────────────────────────────────
object MotionSpec {
    // Быстрый feedback — кнопки, тапы
    val snappy   = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh)
    // Плавный переход — контент, карточки
    val smooth   = tween<Float>(350, easing = FastOutSlowInEasing)
    // Упругое появление — модалки, шторки
    val bouncy   = spring<Float>(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
    // Мгновенный — служебные изменения
    val instant  = snap<Float>()

    val enterFade  = fadeIn(tween(250)) + scaleIn(tween(250, easing = FastOutSlowInEasing), 0.95f)
    val exitFade   = fadeOut(tween(180)) + scaleOut(tween(180), 0.97f)
    val enterSlide = fadeIn(tween(280)) + slideInVertically(
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
    ) { it / 5 }
}
