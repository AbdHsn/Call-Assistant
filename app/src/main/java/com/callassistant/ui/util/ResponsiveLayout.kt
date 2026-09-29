package com.callassistant.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callassistant.ui.theme.AppDimensions
import kotlin.math.min

enum class ScreenWidthClass {
    Compact,
    Medium,
    Expanded
}

enum class ScreenHeightClass {
    Compact,
    Medium,
    Expanded
}

data class ListLayoutMetrics(
    val avatarSize: Dp,
    val horizontalPadding: Dp,
    val itemVerticalPadding: Dp,
    val rowGap: Dp,
    val dividerInset: Dp,
    val fabClearance: Dp,
    val showInlineTrailingActions: Boolean
)

data class DialPadMetrics(
    val keyHeight: Dp,
    val keyGap: Dp,
    val rowGap: Dp,
    val actionButtonSize: Dp,
    val callButtonWidth: Dp,
    val callButtonHeight: Dp,
    val addContactButtonSize: Dp,
    val horizontalPadding: Dp,
    val suggestionsMaxHeight: Dp,
    val bottomPadding: Dp,
    val digitStyle: TextStyle,
    val letterStyle: TextStyle,
    val useVerticalScroll: Boolean
)

@Composable
fun rememberScreenWidthClass(): ScreenWidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp < 600 -> ScreenWidthClass.Compact
            widthDp < 840 -> ScreenWidthClass.Medium
            else -> ScreenWidthClass.Expanded
        }
    }
}

@Composable
fun rememberScreenHeightClass(): ScreenHeightClass {
    val heightDp = LocalConfiguration.current.screenHeightDp
    return remember(heightDp) {
        when {
            heightDp < 520 -> ScreenHeightClass.Compact
            heightDp < 900 -> ScreenHeightClass.Medium
            else -> ScreenHeightClass.Expanded
        }
    }
}

@Composable
fun rememberListLayoutMetrics(inSelectionMode: Boolean): ListLayoutMetrics {
    val widthClass = rememberScreenWidthClass()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val horizontalPadding = when (widthClass) {
        ScreenWidthClass.Compact -> AppDimensions.listHorizontalPaddingCompact
        else -> AppDimensions.listHorizontalPadding
    }
    val avatarSize = when (widthClass) {
        ScreenWidthClass.Compact -> {
            if (screenWidthDp < 360) AppDimensions.avatarCompact else AppDimensions.avatarDefault
        }
        ScreenWidthClass.Medium -> AppDimensions.avatarMedium
        ScreenWidthClass.Expanded -> AppDimensions.avatarExpanded
    }
    val rowGap = if (widthClass == ScreenWidthClass.Compact) 10.dp else AppDimensions.listRowGap
    val dividerInset = if (inSelectionMode) {
        horizontalPadding
    } else {
        horizontalPadding + avatarSize + rowGap
    }
    return remember(widthClass, screenWidthDp, inSelectionMode) {
        ListLayoutMetrics(
            avatarSize = avatarSize,
            horizontalPadding = horizontalPadding,
            itemVerticalPadding = AppDimensions.listItemVerticalPadding,
            rowGap = rowGap,
            dividerInset = dividerInset,
            fabClearance = AppDimensions.fabClearance,
            showInlineTrailingActions = screenWidthDp >= 380
        )
    }
}

@Composable
fun ResponsiveScreenContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = AppDimensions.screenContentMaxWidth,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val contentModifier = if (maxWidth < this.maxWidth) {
            Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
        } else {
            Modifier.fillMaxWidth()
        }
        Box(modifier = contentModifier, content = content)
    }
}

@Composable
fun rememberDialPadMetrics(
    maxWidth: Dp,
    maxHeight: Dp
): DialPadMetrics {
    val widthClass = rememberScreenWidthClass()
    val heightClass = rememberScreenHeightClass()
    val density = LocalDensity.current

    val horizontalPadding = when (widthClass) {
        ScreenWidthClass.Compact -> 12.dp
        ScreenWidthClass.Medium -> 20.dp
        ScreenWidthClass.Expanded -> 24.dp
    }
    val keyGap = when (widthClass) {
        ScreenWidthClass.Compact -> AppDimensions.dialKeyGapCompact
        else -> AppDimensions.dialKeyGapDefault
    }
    val rowGap = when (heightClass) {
        ScreenHeightClass.Compact -> 6.dp
        else -> 10.dp
    }
    val padAreaWidth = (maxWidth - horizontalPadding * 2).coerceAtLeast(0.dp)
    val keyWidthFromArea = ((padAreaWidth - keyGap * 2) / 3).coerceAtLeast(AppDimensions.dialKeyMin)
    val dialPadHeightBudget = when (heightClass) {
        ScreenHeightClass.Compact -> maxHeight * 0.42f
        ScreenHeightClass.Medium -> maxHeight * 0.38f
        ScreenHeightClass.Expanded -> maxHeight * 0.36f
    }
    val keyHeightFromBudget = (dialPadHeightBudget / 4.2f).coerceIn(
        AppDimensions.dialKeyMin,
        AppDimensions.dialKeyMax
    )
    // Keep keys visually balanced: never taller than they are wide on the current row.
    val keyHeight = min(keyWidthFromArea.value, keyHeightFromBudget.value).dp

    val actionButtonSize = (keyHeight * 0.88f).coerceIn(44.dp, 56.dp)
    val callButtonHeight = (keyHeight * 0.9f).coerceIn(44.dp, 56.dp)
    val callButtonWidth = (callButtonHeight * 2.1f).coerceIn(96.dp, 140.dp)
    val suggestionsMaxHeight = when (heightClass) {
        ScreenHeightClass.Compact -> (maxHeight * 0.22f).coerceIn(80.dp, 160.dp)
        ScreenHeightClass.Medium -> (maxHeight * 0.28f).coerceIn(120.dp, 220.dp)
        ScreenHeightClass.Expanded -> (maxHeight * 0.32f).coerceIn(140.dp, 280.dp)
    }
    val digitScale = with(density) { (keyHeight * 0.42f).toSp() }
    val letterScale = with(density) { (keyHeight * 0.14f).toSp() }

    return remember(maxWidth, maxHeight, widthClass, heightClass) {
        DialPadMetrics(
            keyHeight = keyHeight,
            keyGap = keyGap,
            rowGap = rowGap,
            actionButtonSize = actionButtonSize,
            callButtonWidth = callButtonWidth,
            callButtonHeight = callButtonHeight,
            addContactButtonSize = (keyHeight * 0.68f).coerceIn(40.dp, 48.dp),
            horizontalPadding = horizontalPadding,
            suggestionsMaxHeight = suggestionsMaxHeight,
            bottomPadding = if (heightClass == ScreenHeightClass.Compact) 12.dp else 20.dp,
            digitStyle = TextStyle(
                fontSize = digitScale,
                lineHeight = digitScale * 1.1f
            ),
            letterStyle = TextStyle(
                fontSize = letterScale,
                lineHeight = letterScale * 1.2f
            ),
            useVerticalScroll = heightClass == ScreenHeightClass.Compact
        )
    }
}
