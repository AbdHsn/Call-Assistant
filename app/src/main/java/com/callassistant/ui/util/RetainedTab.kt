package com.callassistant.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

/**
 * Keeps tab content composed so list state, scroll position, and collectors survive tab switches.
 * Hidden tabs are not drawn and do not receive touch events.
 */
@Composable
fun RetainedTab(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val blockInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(if (visible) 1f else 0f)
            .graphicsLayer {
                alpha = if (visible) 1f else 0f
            }
            .then(
                if (visible) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = blockInteraction,
                        indication = null,
                        onClick = {}
                    )
                }
            )
    ) {
        content()
    }
}
