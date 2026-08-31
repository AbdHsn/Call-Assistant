package com.callassistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PhoneBookListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
    showTrailingActions: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(bottom = 88.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        userScrollEnabled = false
    ) {
        items(itemCount) {
            PhoneBookListSkeletonItem(showTrailingActions = showTrailingActions)
            PhoneBookListDivider(leadingInset = 82.dp)
        }
    }
}

@Composable
fun MessageThreadListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
    contentPadding: PaddingValues = PaddingValues(bottom = 88.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        userScrollEnabled = false
    ) {
        items(itemCount) {
            MessageThreadSkeletonItem()
            PhoneBookListDivider(leadingInset = 82.dp)
        }
    }
}

@Composable
private fun PhoneBookListSkeletonItem(
    showTrailingActions: Boolean,
    avatarSize: Dp = 52.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonBox(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            if (showTrailingActions) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            }
        }
        if (showTrailingActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    SkeletonBox(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageThreadSkeletonItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonBox(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .width(140.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                SkeletonBox(
                    modifier = Modifier
                        .width(48.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            }
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        }
    }
}

@Composable
private fun SkeletonBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmerOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(shimmerOffset - 300f, 0f),
        end = Offset(shimmerOffset, 0f)
    )
    Box(modifier = modifier.background(brush))
}
