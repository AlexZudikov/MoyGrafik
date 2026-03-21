package ru.tabel.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ═════════════════════════════════════════════════════════════
// ОСНОВНАЯ ПАЛИТРА — Dark Theme (по умолчанию для проекта)
// ═════════════════════════════════════════════════════════════

object AppColors {

    // ── Фоны ──────────────────────────────────────────────────
    
    /** Основной фон приложения (глубокий тёмный) */
    val Background = Color(0xFF0E0E0E)
    
    /** Фон карточек, панелей, модальных окон */
    val Surface = Color(0xFF1A1A1A)
    
    /** Фон приподнятых элементов (elevated cards) */
    val SurfaceElevated = Color(0xFF252525)
    
    /** Фон выделенных/активных элементов */
    val SurfaceHighlight = Color(0xFF2A2A4E)
    
    /** Фон элементов с лёгким акцентом */
    val SurfaceTint = Color(0xFF1A2A6C)

    // ── Акценты ───────────────────────────────────────────────
    
    /** Основной акцентный цвет (яркий синий) */
    val Primary = Color(0xFF4F6EF7)
    
    /** Акцент для hover/focus состояний */
    val PrimaryLight = Color(0xFF6B8AFF)
    
    /** Акцент для pressed состояний */
    val PrimaryDark = Color(0xFF3A56D4)
    
    /** Контейнер для primary элементов */
    val PrimaryContainer = Color(0xFF1A2A6C)
    
    /** Вторичный акцент (фиолетовый) */
    val Secondary = Color(0xFF8B5CF6)
    
    /** Вторичный контейнер */
    val SecondaryContainer = Color(0xFF2D1B6E)
    
    /** Третичный акцент (оранжевый) */
    val Tertiary = Color(0xFFF97316)
    
    /** Третичный контейнер */
    val TertiaryContainer = Color(0xFF5C2D0E)

    // ── Текст ─────────────────────────────────────────────────
    
    /** Основной текст (максимальная контрастность) */
    val TextPrimary = Color(0xFFEEEEEE)
    
    /** Вторичный текст (labels, описания) */
    val TextSecondary = Color(0xFF9A9A9A)
    
    /** Отключенный/неактивный текст */
    val TextDisabled = Color(0xFF6A6A7A)
    
    /** Текст на акцентном фоне */
    val TextOnPrimary = Color(0xFFFFFFFF)
    
    /** Текст на вторичном фоне */
    val TextOnSecondary = Color(0xFFFFFFFF)
    
    /** Текст на третичном фоне */
    val TextOnTertiary = Color(0xFFFFFFFF)

    // ── Состояния ─────────────────────────────────────────────
    
    /** Успех (зелёный) */
    val Success = Color(0xFF22C55E)
    
    /** Контейнер для success элементов */
    val SuccessContainer = Color(0xFF0D4A2A)
    
    /** Предупреждение (янтарный) */
    val Warning = Color(0xFFF59E0B)
    
    /** Контейнер для warning элементов */
    val WarningContainer = Color(0xFF5C3D0A)
    
    /** Ошибка (красный) */
    val Error = Color(0xFFFF6B6B)
    
    /** Контейнер для error элементов */
    val ErrorContainer = Color(0xFF93000A)
    
    /** Информация (голубой) */
    val Info = Color(0xFF3B82F6)
    
    /** Контейнер для info элементов */
    val InfoContainer = Color(0xFF1A3A6C)

    // ── Границы и разделители ─────────────────────────────────
    
    /** Основные границы элементов */
    val Border = Color(0xFF333333)
    
    /** Тонкие разделители (dividers) */
    val Divider = Color(0xFF222222)
    
    /** Границы input-полей */
    val InputBorder = Color(0xFF3A3A5E)
    
    /** Границы в фокусе */
    val InputBorderFocused = Primary
    
    /** Outline для обводки */
    val Outline = Color(0xFF333333)
    
    /** Вариант outline (менее заметный) */
    val OutlineVariant = Color(0xFF222222)

    // ── Overlay (модалки, шторки) ─────────────────────────────
    
    /** Затемнение под модальными окнами */
    val Scrim = Color(0xCC000000) // 80% чёрного

    // ── Специальные цвета (для Табеля) ────────────────────────
    
    /** Цвет сегодняшней даты (золотистый) */
    val Today = Color(0xFFFFD700)
    
    /** Цвет выходных дней (красноватый) */
    val Weekend = Color(0xFFFF6B6B)
    
    /** Цвет статуса "присутствие" */
    val Present = Success
    
    /** Цвет статуса "отсутствие" */
    val Absent = Error
    
    /** Цвет статуса "больничный" */
    val Sick = Warning
    
    /** Цвет статуса "отпуск" */
    val Vacation = Info
    
    /** Цвет статуса "командировка" */
    val BusinessTrip = Tertiary

    // ── Семантические прозрачности ────────────────────────────
    
    /** Лёгкий hover-эффект */
    val HoverOverlay = Color(0x0AFFFFFF) // 4%
    
    /** Эффект нажатия */
    val PressedOverlay = Color(0x1AFFFFFF) // 10%
    
    /** Эффект выделения */
    val SelectedOverlay = Color(0x14FFFFFF) // 8%
    
    /** Эффект фокуса */
    val FocusOverlay = Color(0x1F4F6EF7) // Primary с 12% альфой
    
    /** Эффект drag-and-drop */
    val DragOverlay = Color(0x334F6EF7) // Primary с 20% альфой

    // ── Градиенты ─────────────────────────────────────────────
    
    /** Градиент для hero-секций */
    val HeroGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1A2E),
            Background
        )
    )
    
    /** Градиент для акцентных кнопок */
    val PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(
            Primary,
            PrimaryLight
        )
    )
    
    /** Градиент для shimmer-эффекта */
    val ShimmerGradient = Brush.linearGradient(
        colors = listOf(
            Surface,
            SurfaceHighlight,
            Surface
        )
    )
    
    /** Градиент для success элементов */
    val SuccessGradient = Brush.horizontalGradient(
        colors = listOf(
            Success,
            Color(0xFF34D399)
        )
    )
    
    /** Градиент для error элементов */
    val ErrorGradient = Brush.horizontalGradient(
        colors = listOf(
            Error,
            Color(0xFFFB7185)
        )
    )
}


// ═════════════════════════════════════════════════════════════
// СВЕТЛАЯ ТЕМА
// ═════════════════════════════════════════════════════════════

object AppColorsLight {

    // ── Фоны ──────────────────────────────────────────────────
    
    val Background = Color(0xFFF4F5FF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceElevated = Color(0xFFFAFAFA)
    val SurfaceHighlight = Color(0xFFEEEFF8)
    val SurfaceTint = Color(0xFFDDE1FF)

    // ── Акценты ───────────────────────────────────────────────
    
    val Primary = Color(0xFF4F6EF7)
    val PrimaryLight = Color(0xFF6B8AFF)
    val PrimaryDark = Color(0xFF3A56D4)
    val PrimaryContainer = Color(0xFFDDE1FF)
    
    val Secondary = Color(0xFF8B5CF6)
    val SecondaryContainer = Color(0xFFEDE9FE)
    
    val Tertiary = Color(0xFFF97316)
    val TertiaryContainer = Color(0xFFFFDCC2)

    // ── Текст ─────────────────────────────────────────────────
    
    val TextPrimary = Color(0xFF1A1C2E)
    val TextSecondary = Color(0xFF44475A)
    val TextDisabled = Color(0xFF9A9AAA)
    val TextOnPrimary = Color(0xFFFFFFFF)
    val TextOnSecondary = Color(0xFFFFFFFF)
    val TextOnTertiary = Color(0xFF000000)

    // ── Состояния ─────────────────────────────────────────────
    
    val Success = Color(0xFF16A34A)
    val SuccessContainer = Color(0xFFDCFCE7)
    val Warning = Color(0xFFEA580C)
    val WarningContainer = Color(0xFFFFEDD5)
    val Error = Color(0xFFEF4444)
    val ErrorContainer = Color(0xFFFFDAD6)
    val Info = Color(0xFF2563EB)
    val InfoContainer = Color(0xFFDBEAFE)

    // ── Границы и разделители ─────────────────────────────────
    
    val Border = Color(0xFFCCCEE0)
    val Divider = Color(0xFFE5E7F0)
    val InputBorder = Color(0xFFD0D2E0)
    val InputBorderFocused = Primary
    val Outline = Color(0xFFCCCEE0)
    val OutlineVariant = Color(0xFFE5E7F0)

    // ── Overlay ───────────────────────────────────────────────
    
    val Scrim = Color(0x80000000) // 50% чёрного

    // ── Специальные цвета ─────────────────────────────────────
    
    val Today = Color(0xFFFFB800)
    val Weekend = Color(0xFFEF4444)
    val Present = Success
    val Absent = Error
    val Sick = Warning
    val Vacation = Info
    val BusinessTrip = Tertiary

    // ── Семантические прозрачности ────────────────────────────
    
    val HoverOverlay = Color(0x0A000000)
    val PressedOverlay = Color(0x1A000000)
    val SelectedOverlay = Color(0x14000000)
    val FocusOverlay = Color(0x1F4F6EF7)
    val DragOverlay = Color(0x334F6EF7)

    // ── Градиенты ─────────────────────────────────────────────
    
    val HeroGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Background
        )
    )
    
    val PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(
            Primary,
            PrimaryLight
        )
    )
    
    val ShimmerGradient = Brush.linearGradient(
        colors = listOf(
            Surface,
            SurfaceElevated,
            Surface
        )
    )
    
    val SuccessGradient = Brush.horizontalGradient(
        colors = listOf(
            Success,
            Color(0xFF22C55E)
        )
    )
    
    val ErrorGradient = Brush.horizontalGradient(
        colors = listOf(
            Error,
            Color(0xFFF87171)
        )
    )
}


// ═════════════════════════════════════════════════════════════
// СЕМАНТИЧЕСКИЕ ЦВЕТА — для конкретных UI элементов
// ═════════════════════════════════════════════════════════════

object SemanticColors {

    // ── Кнопки ────────────────────────────────────────────────
    
    val ButtonPrimaryBackground = AppColors.Primary
    val ButtonPrimaryHover = AppColors.PrimaryLight
    val ButtonPrimaryPressed = AppColors.PrimaryDark
    val ButtonPrimaryText = AppColors.TextOnPrimary
    
    val ButtonSecondaryBackground = AppColors.Surface
    val ButtonSecondaryBorder = AppColors.Border
    val ButtonSecondaryText = AppColors.TextPrimary
    val ButtonSecondaryHover = AppColors.SurfaceElevated
    
    val ButtonTertiaryBackground = Color.Transparent
    val ButtonTertiaryText = AppColors.Primary
    val ButtonTertiaryHover = AppColors.HoverOverlay
    
    val ButtonDisabledBackground = AppColors.SurfaceHighlight
    val ButtonDisabledText = AppColors.TextDisabled

    // ── Input поля ────────────────────────────────────────────
    
    val InputBackground = AppColors.Surface
    val InputBorder = AppColors.InputBorder
    val InputBorderFocused = AppColors.InputBorderFocused
    val InputBorderError = AppColors.Error
    val InputText = AppColors.TextPrimary
    val InputPlaceholder = AppColors.TextSecondary
    val InputDisabled = AppColors.SurfaceHighlight

    // ── Карточки ──────────────────────────────────────────────
    
    val CardBackground = AppColors.Surface
    val CardBorder = AppColors.Border
    val CardHover = AppColors.SurfaceElevated
    val CardSelected = AppColors.SurfaceHighlight
    val CardPressed = AppColors.SurfaceHighlight

    // ── Прогресс-бары ─────────────────────────────────────────
    
    val ProgressBackground = AppColors.Surface
    val ProgressFill = AppColors.Primary
    val ProgressTrack = AppColors.SurfaceHighlight

    // ── Бейджи ────────────────────────────────────────────────
    
    val BadgeSuccessBackground = AppColors.Success.withAlpha(0.15f)
    val BadgeSuccessText = AppColors.Success
    val BadgeSuccessBorder = AppColors.Success.withAlpha(0.3f)
    
    val BadgeWarningBackground = AppColors.Warning.withAlpha(0.15f)
    val BadgeWarningText = AppColors.Warning
    val BadgeWarningBorder = AppColors.Warning.withAlpha(0.3f)
    
    val BadgeErrorBackground = AppColors.Error.withAlpha(0.15f)
    val BadgeErrorText = AppColors.Error
    val BadgeErrorBorder = AppColors.Error.withAlpha(0.3f)
    
    val BadgeInfoBackground = AppColors.Info.withAlpha(0.15f)
    val BadgeInfoText = AppColors.Info
    val BadgeInfoBorder = AppColors.Info.withAlpha(0.3f)
    
    val BadgeNeutralBackground = AppColors.SurfaceHighlight
    val BadgeNeutralText = AppColors.TextSecondary
    val BadgeNeutralBorder = AppColors.Border

    // ── Табель (специфичные для приложения) ───────────────────
    
    val TodayBackground = AppColors.Today.withAlpha(0.1f)
    val TodayBorder = AppColors.Today
    val TodayText = AppColors.Today
    
    val WeekendBackground = AppColors.Weekend.withAlpha(0.1f)
    val WeekendText = AppColors.Weekend
    
    val PresentBackground = AppColors.Present.withAlpha(0.15f)
    val PresentText = AppColors.Present
    val PresentBorder = AppColors.Present.withAlpha(0.3f)
    
    val AbsentBackground = AppColors.Absent.withAlpha(0.15f)
    val AbsentText = AppColors.Absent
    val AbsentBorder = AppColors.Absent.withAlpha(0.3f)
    
    val SickBackground = AppColors.Sick.withAlpha(0.15f)
    val SickText = AppColors.Sick
    val SickBorder = AppColors.Sick.withAlpha(0.3f)
}


// ═════════════════════════════════════════════════════════════
// EXTENSION — удобные модификаторы цвета
// ═════════════════════════════════════════════════════════════

/**
 * Возвращает цвет с заданной прозрачностью.
 *
 * ```
 * val semiTransparent = AppColors.Primary.withAlpha(0.5f)
 * ```
 */
fun Color.withAlpha(alpha: Float): Color = this.copy(alpha = alpha.coerceIn(0f, 1f))

/**
 * Затемняет цвет на заданный процент.
 *
 * ```
 * val darkerBlue = AppColors.Primary.darken(0.2f) // на 20% темнее
 * ```
 */
fun Color.darken(fraction: Float): Color {
    val factor = (1f - fraction.coerceIn(0f, 1f))
    return Color(
        red = (red * factor).coerceIn(0f, 1f),
        green = (green * factor).coerceIn(0f, 1f),
        blue = (blue * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

/**
 * Осветляет цвет на заданный процент.
 *
 * ```
 * val lighterBlue = AppColors.Primary.lighten(0.3f)
 * ```
 */
fun Color.lighten(fraction: Float): Color {
    val factor = fraction.coerceIn(0f, 1f)
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

/**
 * Возвращает контрастный цвет для текста (белый или чёрный).
 *
 * Использует формулу относительной яркости WCAG 2.0.
 *
 * ```
 * val textColor = backgroundColor.contrastText()
 * Text("Hello", color = textColor)
 * ```
 */
fun Color.contrastText(): Color {
    // Формула относительной яркости (WCAG)
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
    return if (luminance > 0.5) Color(0xFF000000) else Color(0xFFFFFFFF)
}

/**
 * Смешивает текущий цвет с другим в заданной пропорции.
 *
 * ```
 * val mixed = AppColors.Primary.blend(AppColors.Secondary, 0.5f)
 * ```
 *
 * @param other второй цвет для смешивания
 * @param ratio пропорция (0f = текущий цвет, 1f = другой цвет)
 */
fun Color.blend(other: Color, ratio: Float): Color {
    val t = ratio.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - t) + other.red * t,
        green = green * (1f - t) + other.green * t,
        blue = blue * (1f - t) + other.blue * t,
        alpha = alpha * (1f - t) + other.alpha * t
    )
}

/**
 * Возвращает инвертированный цвет.
 *
 * ```
 * val inverted = AppColors.Primary.invert()
 * ```
 */
fun Color.invert(): Color = Color(
    red = 1f - red,
    green = 1f - green,
    blue = 1f - blue,
    alpha = alpha
)

/**
 * Конвертирует цвет в grayscale.
 *
 * ```
 * val gray = AppColors.Primary.toGrayscale()
 * ```
 */
fun Color.toGrayscale(): Color {
    val gray = 0.299f * red + 0.587f * green + 0.114f * blue
    return Color(gray, gray, gray, alpha)
}


// ═════════════════════════════════════════════════════════════
// DEPRECATED (для обратной совместимости)
// Удалить после миграции на AppColors
// ═════════════════════════════════════════════════════════════

@Deprecated(
    message = "Use AppColors or AppTheme.colors instead",
    replaceWith = ReplaceWith("AppColors.Primary", "ru.tabel.app.ui.theme.AppColors"),
    level = DeprecationLevel.WARNING
)
val Purple80 = Color(0xFFD0BCFF)

@Deprecated(
    message = "Use AppColors or AppTheme.colors instead",
    replaceWith = ReplaceWith("AppColors.Surface", "ru.tabel.app.ui.theme.AppColors"),
    level = DeprecationLevel.WARNING
)
val PurpleGrey80 = Color(0xFFCCC2DC)

@Deprecated(
    message = "Use AppColors or AppTheme.colors instead",
    replaceWith = ReplaceWith("AppColors.Secondary", "ru.tabel.app.ui.theme.AppColors"),
    level = DeprecationLevel.WARNING
)
val Pink80 = Color(0xFFEFB8C8)

@Deprecated(
    message = "Use AppColors or AppTheme.colors instead",
    replaceWith = ReplaceWith("AppColors.Primary", "ru.tabel.app.ui.theme.AppColors"),
    level = DeprecationLevel.WARNING
)
val Purple40 = Color(0xFF6650a4)

@Deprecated(
    message = "Use AppColors or AppTheme.colors instead",
    replaceWith = ReplaceWith("AppColors.Surface", "ru.tabel.app.ui.theme.AppColors"),
    level = DeprecationLevel.WARNING
)
val PurpleGrey40 = Color(0xFF625b71)

@Deprecated(
    message = "Use AppColors or AppTheme.colors instead",
    replaceWith = ReplaceWith("AppColors.Secondary", "ru.tabel.app.ui.theme.AppColors"),
    level = DeprecationLevel.WARNING
)
val Pink40 = Color(0xFF7D5260)