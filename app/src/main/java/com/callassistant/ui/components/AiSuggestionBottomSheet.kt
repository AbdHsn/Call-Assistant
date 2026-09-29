package com.callassistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callassistant.ai.model.AiContextScope
import com.callassistant.ai.model.AiModelStatus
import com.callassistant.ai.model.AiSuggestion
import com.callassistant.ai.model.AiTone
import com.callassistant.ai.model.MessageAiContext
import com.callassistant.ai.model.SuggestionLanguage
import com.callassistant.ui.messageai.MessageAiSheetState
import com.callassistant.ui.screens.ContactAvatar
import com.callassistant.ui.theme.AccentTealEnd
import com.callassistant.ui.theme.AccentTealStart
import com.callassistant.ui.theme.MessageBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AiRecipientSummary(
    val number: String,
    val displayName: String,
    val photoUri: String? = null
)

private val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
private val InlineSheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val CardShape = RoundedCornerShape(16.dp)
private val CompactCardShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(999.dp)
private val InlinePanelMaxHeight = 272.dp

private fun AiContextScope.compactLabel(): String = when (this) {
    AiContextScope.LAST_5 -> "Last 5"
    AiContextScope.LAST_20 -> "Last 20"
    AiContextScope.WHOLE_THREAD -> "All"
    AiContextScope.THIS_MESSAGE -> "This msg"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AiSuggestionBottomSheet(
    sheetState: MessageAiSheetState,
    modelStatus: AiModelStatus,
    inferenceReady: Boolean,
    messageContext: MessageAiContext,
    onDismiss: () -> Unit,
    onContextScopeChange: (AiContextScope) -> Unit,
    onGenerate: (MessageAiContext) -> Unit,
    onSelectSuggestion: (AiSuggestion) -> Unit,
    onAskQueryChange: (String) -> Unit,
    onRefineTone: (AiTone) -> Unit,
    onDownloadModel: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    recipients: List<AiRecipientSummary> = emptyList(),
    modifier: Modifier = Modifier,
    bottomSheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!sheetState.visible) return

    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(sheetState.visible) {
        if (sheetState.visible) {
            bottomSheetState.expand()
        }
    }

    AiSuggestionAutoGenerate(
        visible = sheetState.visible,
        sheetState = sheetState,
        messageContext = messageContext,
        onGenerate = onGenerate
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        modifier = modifier,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
        ) {
            val density = LocalDensity.current
            val maxBodyHeight = with(density) {
                (maxHeight - 280.dp).coerceAtLeast(120.dp)
            }

            AiSuggestionSurface(
                isInline = false,
                showContextScope = true,
                sheetState = sheetState,
                modelStatus = modelStatus,
                inferenceReady = inferenceReady,
                messageContext = messageContext,
                onDismiss = onDismiss,
                onContextScopeChange = onContextScopeChange,
                onGenerate = onGenerate,
                onSelectSuggestion = onSelectSuggestion,
                onAskQueryChange = onAskQueryChange,
                onRefineTone = onRefineTone,
                onDownloadModel = onDownloadModel,
                onNavigateToAiSettings = onNavigateToAiSettings,
                recipients = recipients,
                maxBodyHeight = maxBodyHeight,
                bringIntoViewRequester = bringIntoViewRequester,
                onAskAiFocused = {
                    scope.launch {
                        bottomSheetState.expand()
                        delay(120)
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AiSuggestionInlinePanel(
    sheetState: MessageAiSheetState,
    modelStatus: AiModelStatus,
    inferenceReady: Boolean,
    messageContext: MessageAiContext,
    onDismiss: () -> Unit,
    onContextScopeChange: (AiContextScope) -> Unit,
    onGenerate: (MessageAiContext) -> Unit,
    onSelectSuggestion: (AiSuggestion) -> Unit,
    onAskQueryChange: (String) -> Unit,
    onRefineTone: (AiTone) -> Unit,
    onDownloadModel: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    recipients: List<AiRecipientSummary> = emptyList(),
    showContextScope: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!sheetState.visible) return

    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    AiSuggestionAutoGenerate(
        visible = sheetState.visible,
        sheetState = sheetState,
        messageContext = messageContext,
        onGenerate = onGenerate
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        AiSuggestionSurface(
            isInline = true,
            showContextScope = showContextScope,
            sheetState = sheetState,
            modelStatus = modelStatus,
            inferenceReady = inferenceReady,
            messageContext = messageContext,
            onDismiss = onDismiss,
            onContextScopeChange = onContextScopeChange,
            onGenerate = onGenerate,
            onSelectSuggestion = onSelectSuggestion,
            onAskQueryChange = onAskQueryChange,
            onRefineTone = onRefineTone,
            onDownloadModel = onDownloadModel,
            onNavigateToAiSettings = onNavigateToAiSettings,
            recipients = recipients,
            maxBodyHeight = maxHeight.coerceAtMost(InlinePanelMaxHeight),
            bringIntoViewRequester = bringIntoViewRequester,
            onAskAiFocused = {
                scope.launch {
                    delay(120)
                    bringIntoViewRequester.bringIntoView()
                }
            }
        )
    }
}

@Composable
private fun AiSuggestionAutoGenerate(
    visible: Boolean,
    sheetState: MessageAiSheetState,
    messageContext: MessageAiContext,
    onGenerate: (MessageAiContext) -> Unit
) {
    LaunchedEffect(visible, sheetState.contextScope, sheetState.anchorMessage?.id) {
        if (visible && sheetState.suggestions.isEmpty() && !sheetState.isGenerating) {
            onGenerate(messageContext)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun AiSuggestionSurface(
    isInline: Boolean,
    showContextScope: Boolean,
    sheetState: MessageAiSheetState,
    modelStatus: AiModelStatus,
    inferenceReady: Boolean,
    messageContext: MessageAiContext,
    onDismiss: () -> Unit,
    onContextScopeChange: (AiContextScope) -> Unit,
    onGenerate: (MessageAiContext) -> Unit,
    onSelectSuggestion: (AiSuggestion) -> Unit,
    onAskQueryChange: (String) -> Unit,
    onRefineTone: (AiTone) -> Unit,
    onDownloadModel: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    recipients: List<AiRecipientSummary>,
    maxBodyHeight: Dp,
    bringIntoViewRequester: BringIntoViewRequester,
    onAskAiFocused: () -> Unit
) {
    val suggestionsScroll = rememberScrollState()
    val scopeScroll = rememberScrollState()
    val toneScroll = rememberScrollState()
    val horizontalPad = if (isInline) 12.dp else 20.dp
    val columnModifier = if (isInline) {
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxBodyHeight)
    } else {
        Modifier.fillMaxWidth()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = if (isInline) InlineSheetShape else SheetShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isInline) 8.dp else 0.dp,
        tonalElevation = if (isInline) 1.dp else 0.dp,
        border = if (isInline) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        } else {
            null
        }
    ) {
        Column(modifier = columnModifier) {
            AiSheetHeader(
                isInline = isInline,
                onDismiss = onDismiss
            )

            if (!isInline && recipients.isNotEmpty()) {
                RecipientStrip(
                    recipients = recipients,
                    modifier = Modifier.padding(horizontal = horizontalPad)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!inferenceReady) {
                AiModelStatusCard(
                    modelStatus = modelStatus,
                    onDownload = onDownloadModel,
                    onOpenSettings = onNavigateToAiSettings,
                    compact = isInline,
                    modifier = Modifier.padding(horizontal = horizontalPad)
                )
                Spacer(modifier = Modifier.height(if (isInline) 6.dp else 12.dp))
            }

            if (showContextScope) {
                Column(modifier = Modifier.padding(horizontal = horizontalPad)) {
                    if (!isInline) {
                        Text(
                            text = "Conversation context",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scopeScroll),
                        horizontalArrangement = Arrangement.spacedBy(if (isInline) 6.dp else 8.dp)
                    ) {
                        AiContextScope.entries.forEach { scope ->
                            val selected = sheetState.contextScope == scope
                            FilterChip(
                                selected = selected,
                                onClick = { onContextScopeChange(scope) },
                                label = {
                                    Text(
                                        text = if (isInline) scope.compactLabel() else scope.label,
                                        style = if (isInline) {
                                            MaterialTheme.typography.labelSmall
                                        } else {
                                            MaterialTheme.typography.labelLarge
                                        },
                                        color = if (selected) {
                                            MessageBlue
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                leadingIcon = if (selected && !isInline) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    null
                                },
                                shape = PillShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MessageBlue.copy(alpha = 0.14f),
                                    selectedLabelColor = MessageBlue,
                                    selectedLeadingIconColor = MessageBlue
                                )
                            )
                        }
                    }
                }

                if (!isInline) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = horizontalPad)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Suggested replies",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = horizontalPad, vertical = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (!isInline) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = horizontalPad)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Suggested replies",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = horizontalPad, vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isInline) {
                            Modifier.weight(1f, fill = true)
                        } else {
                            Modifier.heightIn(max = maxBodyHeight)
                        }
                    )
                    .verticalScroll(suggestionsScroll)
                    .padding(horizontal = horizontalPad)
            ) {
                when {
                    sheetState.isGenerating -> {
                        SuggestionSkeletonList(compact = isInline)
                    }
                    sheetState.error != null -> {
                        ErrorStateCard(
                            message = sheetState.error,
                            onRetry = { onGenerate(messageContext) },
                            compact = isInline
                        )
                    }
                    else -> {
                        if (sheetState.suggestions.isEmpty()) {
                            EmptySuggestionsHint(compact = isInline)
                        } else {
                            sheetState.suggestions.forEachIndexed { index, suggestion ->
                                AiSuggestionCard(
                                    index = index + 1,
                                    suggestion = suggestion,
                                    selected = sheetState.selectedSuggestion == suggestion,
                                    compact = isInline,
                                    onClick = { onSelectSuggestion(suggestion) }
                                )
                                Spacer(modifier = Modifier.height(if (isInline) 6.dp else 10.dp))
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = if (isInline) 4.dp else 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .padding(
                            horizontal = horizontalPad,
                            vertical = if (isInline) 10.dp else 16.dp
                        )
                ) {
                    if (!isInline) {
                        Text(
                            text = "Custom prompt",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isInline) 8.dp else 10.dp)
                    ) {
                        OutlinedTextField(
                            value = sheetState.askQuery,
                            onValueChange = onAskQueryChange,
                            placeholder = {
                                Text(
                                    if (isInline) "Ask AI…" else "e.g. polite decline in Bangla",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            shape = PillShape,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MessageBlue,
                                focusedBorderColor = MessageBlue.copy(alpha = 0.55f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusEvent { event ->
                                    if (event.isFocused) {
                                        onAskAiFocused()
                                    }
                                }
                        )
                        Surface(
                            onClick = { onGenerate(messageContext) },
                            enabled = sheetState.askQuery.isNotBlank() && !sheetState.isGenerating,
                            shape = CircleShape,
                            color = if (sheetState.askQuery.isNotBlank() && !sheetState.isGenerating) {
                                MessageBlue
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(if (isInline) 40.dp else 48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Generate",
                                    tint = if (sheetState.askQuery.isNotBlank() && !sheetState.isGenerating) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    },
                                    modifier = Modifier.size(if (isInline) 18.dp else 20.dp)
                                )
                            }
                        }
                    }

                    if (sheetState.selectedSuggestion != null) {
                        Spacer(modifier = Modifier.height(if (isInline) 8.dp else 16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isInline) {
                                Text(
                                    text = "Refine tone",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (sheetState.isRefiningTone) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MessageBlue
                                )
                            }
                        }
                        if (!isInline) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(toneScroll),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AiTone.entries.forEach { tone ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onRefineTone(tone) },
                                    enabled = !sheetState.isRefiningTone,
                                    label = {
                                        Text(
                                            tone.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    shape = PillShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    if (!isInline) {
                        Text(
                            text = "Tap a suggestion to insert into your draft — you send manually",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSheetHeader(
    isInline: Boolean,
    onDismiss: () -> Unit
) {
    if (isInline) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MessageBlue.copy(alpha = 0.08f),
                            AccentTealStart.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MessageBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Write Assist",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MessageBlue.copy(alpha = 0.10f),
                        AccentTealStart.copy(alpha = 0.08f),
                        AccentTealEnd.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isInline) 10.dp else 12.dp, bottom = 16.dp)
        ) {
            if (isInline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f))
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MessageBlue.copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MessageBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Write Assist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AI-powered reply suggestions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    }
}

@Composable
private fun RecipientStrip(
    recipients: List<AiRecipientSummary>,
    modifier: Modifier = Modifier
) {
    val chipScroll = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "To",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(chipScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipients.forEach { recipient ->
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ContactAvatar(
                                name = recipient.displayName,
                                photoUri = recipient.photoUri,
                                size = 24.dp
                            )
                            Text(
                                text = recipient.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiModelStatusCard(
    modelStatus: AiModelStatus,
    onDownload: () -> Unit,
    onOpenSettings: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val message = when (modelStatus) {
        is AiModelStatus.NotDownloaded -> "On-device model not downloaded — using smart templates"
        is AiModelStatus.Downloading -> "Downloading model… ${(modelStatus.progress * 100).toInt()}%"
        is AiModelStatus.UnsupportedDevice -> "This device may not support on-device AI (6 GB+ RAM recommended)"
        is AiModelStatus.Error -> "Model error: ${modelStatus.message}"
        is AiModelStatus.Ready -> ""
    }
    if (message.isBlank()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) 10.dp else 14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(if (compact) 8.dp else 14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (modelStatus is AiModelStatus.Downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Outlined.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 16.dp else 18.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    text = message,
                    style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
                )
            }
            if (modelStatus is AiModelStatus.NotDownloaded && !compact) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    TextButton(onClick = onDownload, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Download")
                    }
                    TextButton(onClick = onOpenSettings, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSuggestionCard(
    index: Int,
    suggestion: AiSuggestion,
    selected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val accent = languageAccent(suggestion.language)
    val shape = if (compact) CompactCardShape else CardShape
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) {
            accent.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(if (compact) 22.dp else 28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                        )
                    } else {
                        Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                LanguageBadge(language = suggestion.language, compact = compact)
                Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
                Text(
                    text = suggestion.text,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LanguageBadge(language: SuggestionLanguage, compact: Boolean = false) {
    val color = languageAccent(language)
    Surface(
        shape = PillShape,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = language.badge,
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SuggestionSkeletonList(compact: Boolean = false) {
    repeat(if (compact) 2 else 3) {
        Surface(
            shape = if (compact) CompactCardShape else CardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(if (compact) 10.dp else 14.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier.size(if (compact) 22.dp else 28.dp),
                    shape = RoundedCornerShape(999.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(if (compact) 0.35f else 0.25f)
                            .height(if (compact) 12.dp else 18.dp),
                        shape = PillShape
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(if (compact) 12.dp else 14.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))
    }
}

@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.12f))
    )
}

@Composable
private fun EmptySuggestionsHint(compact: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 12.dp else 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!compact) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MessageBlue.copy(alpha = 0.45f),
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = "Suggestions will appear here",
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    compact: Boolean = false
) {
    Surface(
        shape = if (compact) CompactCardShape else CardShape,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(if (compact) 10.dp else 16.dp)) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                maxLines = if (compact) 2 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Try again", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun languageAccent(language: SuggestionLanguage): Color = when (language) {
    SuggestionLanguage.BN -> AccentTealStart
    SuggestionLanguage.EN -> MessageBlue
    SuggestionLanguage.BL -> AccentTealEnd
}
