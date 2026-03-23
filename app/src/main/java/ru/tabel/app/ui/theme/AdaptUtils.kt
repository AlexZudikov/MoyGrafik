package ru.tabel.app.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ScreenSizeClass { COMPACT, MEDIUM, EXPANDED }

data class AdaptiveDimens(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val screenSizeClass: ScreenSizeClass,
    val calendarCellSize: Dp,
    val calendarGridMaxHeight: Dp,
    val cardPadding: Dp,
    val horizontalPadding: Dp,
    val buttonHeight: Dp,
    val iconSizeSmall: Dp,
    val iconSizeMedium: Dp,
    val titleFontSize: TextUnit,
    val bodyFontSize: TextUnit,
    val labelFontSize: TextUnit,
    val gridColumns: Int,
    val isCompact: Boolean,
    val isLandscape: Boolean
)

val LocalAdaptiveDimens = compositionLocalOf {
    AdaptiveDimens(
        screenWidthDp = 360,
        screenHeightDp = 640,
        screenSizeClass = ScreenSizeClass.COMPACT,
        calendarCellSize = 40.dp,
        calendarGridMaxHeight = 400.dp,
        cardPadding = 12.dp,
        horizontalPadding = 12.dp,
        buttonHeight = 48.dp,
        iconSizeSmall = 18.dp,
        iconSizeMedium = 24.dp,
        titleFontSize = 18.sp,
        bodyFontSize = 14.sp,
        labelFontSize = 11.sp,
        gridColumns = 2,
        isCompact = true,
        isLandscape = false
    )
}

@Composable
fun rememberAdaptiveDimens(): AdaptiveDimens {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    
    val widthDp = config.screenWidthDp
    val heightDp = config.screenHeightDp
    val isLandscape = widthDp > heightDp
    
    val screenClass = when {
        widthDp < 360 -> ScreenSizeClass.COMPACT
        widthDp < 600 -> ScreenSizeClass.MEDIUM
        else -> ScreenSizeClass.EXPANDED
    }
    
    val isCompact = screenClass == ScreenSizeClass.COMPACT
    
    val calendarCellSize = when {
        widthDp < 320 -> 36.dp
        widthDp < 360 -> 38.dp
        widthDp < 400 -> 42.dp
        else -> 46.dp
    }
    
    val calendarGridMaxHeight = when {
        widthDp < 320 -> 320.dp
        widthDp < 360 -> 360.dp
        widthDp < 400 -> 400.dp
        isLandscape -> 300.dp
        else -> 420.dp
    }
    
    val cardPadding = when {
        isCompact -> 10.dp
        widthDp < 400 -> 12.dp
        else -> 14.dp
    }
    
    val horizontalPadding = when {
        isCompact -> 10.dp
        widthDp < 400 -> 12.dp
        else -> 16.dp
    }
    
    val buttonHeight = when {
        isCompact -> 44.dp
        else -> 48.dp
    }
    
    val iconSizeSmall = when {
        isCompact -> 16.dp
        else -> 18.dp
    }
    
    val iconSizeMedium = when {
        isCompact -> 20.dp
        else -> 24.dp
    }
    
    val titleFontSize = when {
        isCompact -> 16.sp
        widthDp >= 400 -> 18.sp
        else -> 17.sp
    }
    
    val bodyFontSize = when {
        isCompact -> 13.sp
        else -> 14.sp
    }
    
    val labelFontSize = when {
        isCompact -> 10.sp
        widthDp >= 400 -> 12.sp
        else -> 11.sp
    }
    
    val gridColumns = when {
        widthDp >= 600 -> 3
        else -> 2
    }
    
    return AdaptiveDimens(
        screenWidthDp = widthDp,
        screenHeightDp = heightDp,
        screenSizeClass = screenClass,
        calendarCellSize = calendarCellSize,
        calendarGridMaxHeight = calendarGridMaxHeight,
        cardPadding = cardPadding,
        horizontalPadding = horizontalPadding,
        buttonHeight = buttonHeight,
        iconSizeSmall = iconSizeSmall,
        iconSizeMedium = iconSizeMedium,
        titleFontSize = titleFontSize,
        bodyFontSize = bodyFontSize,
        labelFontSize = labelFontSize,
        gridColumns = gridColumns,
        isCompact = isCompact,
        isLandscape = isLandscape
    )
}

@Composable
fun Modifier.adaptivePadding(horizontal: Dp? = null, vertical: Dp? = null): Modifier {
    val dimens = rememberAdaptiveDimens()
    return this.padding(
        horizontal = horizontal ?: dimens.horizontalPadding,
        vertical = vertical ?: dimens.cardPadding
    )
}
