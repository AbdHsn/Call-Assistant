package com.callassistant

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.telephony.SmsManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.callassistant.receiver.CallReminderReceiver
import com.callassistant.service.CallAssistantInCallService
import com.callassistant.service.CallState
import com.callassistant.ui.theme.CallAssistantTheme
import com.callassistant.util.CallNotesStore
import com.callassistant.util.CallRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class InCallActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val callState by CallAssistantInCallService.callState.collectAsState()
            val connectTimestamp by CallAssistantInCallService.callConnectTimestamp.collectAsState()
            val isMuted by CallAssistantInCallService.isMuted.collectAsState()
            val isSpeakerOn by CallAssistantInCallService.isSpeakerOn.collectAsState()

            LaunchedEffect(callState) {
                if (callState is CallState.Ended) finish()
            }

            CallAssistantTheme {
                InCallScreen(
                    callState = callState,
                    connectTimestamp = connectTimestamp,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    onAnswer = { CallAssistantInCallService.activeCall?.answer(0) },
                    onReject = { CallAssistantInCallService.activeCall?.reject(false, null) },
                    onHangUp = { CallAssistantInCallService.activeCall?.disconnect() },
                    onToggleMute = { CallAssistantInCallService.setMuted(!isMuted) },
                    onToggleSpeaker = { CallAssistantInCallService.setSpeakerOn(!isSpeakerOn) },
                    onSendDigit = { digit ->
                        CallAssistantInCallService.activeCall?.let {
                            it.playDtmfTone(digit)
                            it.stopDtmfTone()
                        }
                    }
                )
            }
        }
    }
}

private val AccentTealStart = Color(0xFF17A79B)
private val AccentTealEnd = Color(0xFF6FCF97)
private val DeclineRed = Color(0xFFE64C3C)
private val ConnectingAmber = Color(0xFFE0A030)

@Composable
private fun InCallScreen(
    callState: CallState,
    connectTimestamp: Long?,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSendDigit: (Char) -> Unit
) {
    val context = LocalContext.current
    val number = when (callState) {
        is CallState.Incoming -> callState.number
        is CallState.Active -> callState.number
        is CallState.Connecting -> callState.number
        else -> ""
    }
    val displayName = when (callState) {
        is CallState.Incoming -> callState.displayName
        is CallState.Active -> callState.displayName
        is CallState.Connecting -> callState.displayName
        else -> null
    }
    val initial = (displayName ?: number).firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    var showDialpad by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    val recorder = remember { CallRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isRecording = recorder.start(number)
            if (!isRecording) Toast.makeText(context, "Couldn't start recording", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required to record", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleRecording() {
        if (isRecording) {
            val path = recorder.stop()
            isRecording = false
            Toast.makeText(
                context,
                if (path != null) "Recording saved" else "Recording stopped",
                Toast.LENGTH_SHORT
            ).show()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            isRecording = recorder.start(number)
            if (!isRecording) Toast.makeText(context, "Couldn't start recording", Toast.LENGTH_SHORT).show()
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(callState) {
        if (callState is CallState.Ended && isRecording) {
            recorder.stop()
            isRecording = false
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun scheduleReminder(delayMillis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val reminderIntent = Intent(context, CallReminderReceiver::class.java).apply {
            putExtra(CallReminderReceiver.EXTRA_NUMBER, number)
            putExtra(CallReminderReceiver.EXTRA_NAME, displayName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            number.hashCode(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMillis, pendingIntent)
        Toast.makeText(context, "Reminder set", Toast.LENGTH_SHORT).show()
    }

    var nowMillis by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(connectTimestamp) {
        while (connectTimestamp != null) {
            nowMillis = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }
    val elapsedSeconds = if (connectTimestamp != null) {
        ((nowMillis - connectTimestamp).coerceAtLeast(0L) / 1000L).toInt()
    } else {
        0
    }

    fun sendQuickMessageAndDecline() {
        if (number.isNotBlank() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(number, null, "Can't talk right now, will call you back.", null, null)
                Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't send message", Toast.LENGTH_SHORT).show()
            }
        }
        onReject()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Call Assistant",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CallerAvatar(initial = initial, pulsing = callState is CallState.Incoming)
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = displayName ?: number.ifBlank { "Unknown" },
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (displayName != null && number.isNotBlank()) number else "Mobile",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                StatusPill(callState = callState, elapsedSeconds = elapsedSeconds)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (callState) {
                    is CallState.Incoming -> {
                        CallToolsRow(
                            isSpeakerOn = isSpeakerOn,
                            onToggleSpeaker = onToggleSpeaker,
                            isRecording = isRecording,
                            onToggleRecording = { toggleRecording() },
                            onNoteClick = { noteText = ""; showNoteDialog = true },
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(36.dp),
                            modifier = Modifier.padding(bottom = 32.dp)
                        ) {
                            RoundActionButton(
                                icon = Icons.Filled.Alarm,
                                label = "Reminder",
                                onClick = onReject
                            )
                            RoundActionButton(
                                icon = Icons.Filled.Sms,
                                label = "Message",
                                onClick = { sendQuickMessageAndDecline() }
                            )
                            RoundActionButton(
                                icon = Icons.Filled.CallEnd,
                                label = "Decline",
                                tint = DeclineRed,
                                onClick = onReject
                            )
                        }
                        SlideToAnswerButton(onAnswer = onAnswer)
                    }
                    is CallState.Active -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(40.dp),
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            SecondaryControlButton(
                                icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                label = if (isMuted) "Unmute" else "Mute",
                                active = isMuted,
                                onClick = onToggleMute
                            )
                            SecondaryControlButton(
                                icon = Icons.Filled.Dialpad,
                                label = "Keypad",
                                active = showDialpad,
                                onClick = { showDialpad = !showDialpad }
                            )
                        }
                        CallToolsRow(
                            isSpeakerOn = isSpeakerOn,
                            onToggleSpeaker = onToggleSpeaker,
                            isRecording = isRecording,
                            onToggleRecording = { toggleRecording() },
                            onNoteClick = { noteText = ""; showNoteDialog = true },
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(40.dp),
                            modifier = Modifier.padding(bottom = 28.dp)
                        ) {
                            SecondaryControlButton(
                                icon = Icons.Filled.Alarm,
                                label = "Reminder",
                                active = false,
                                onClick = { showReminderDialog = true }
                            )
                        }
                        CallButton(Icons.Filled.CallEnd, DeclineRed, "End", onHangUp)
                    }
                    is CallState.Connecting -> {
                        CallToolsRow(
                            isSpeakerOn = isSpeakerOn,
                            onToggleSpeaker = onToggleSpeaker,
                            isRecording = isRecording,
                            onToggleRecording = { toggleRecording() },
                            onNoteClick = { noteText = ""; showNoteDialog = true },
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        CallButton(Icons.Filled.CallEnd, DeclineRed, "Cancel", onHangUp)
                    }
                    else -> {}
                }
            }
        }

        if (showDialpad && callState is CallState.Active) {
            DialpadOverlay(
                onDigit = onSendDigit,
                onDismiss = { showDialpad = false }
            )
        }
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Call note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Type a quick note...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    CallNotesStore.addNote(context, number, noteText)
                    Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                    showNoteDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Call back reminder") },
            text = { Text("Remind me to call back ${displayName ?: number} in:") },
            confirmButton = {
                TextButton(onClick = {
                    scheduleReminder(60 * 60 * 1000L)
                    showReminderDialog = false
                }) { Text("1 hour") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        scheduleReminder(3 * 60 * 60 * 1000L)
                        showReminderDialog = false
                    }) { Text("3 hours") }
                    TextButton(onClick = { showReminderDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
private fun CallerAvatar(initial: String, pulsing: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) 1.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val ringBrush = Brush.linearGradient(listOf(AccentTealStart, AccentTealEnd))
    Box(contentAlignment = Alignment.Center) {
        if (pulsing) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(AccentTealStart.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(AccentTealStart.copy(alpha = 0.16f))
            )
        }
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(ringBrush),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusPill(callState: CallState, elapsedSeconds: Int) {
    val (dotColor, label) = when (callState) {
        is CallState.Incoming -> AccentTealStart to "Incoming call"
        is CallState.Active -> AccentTealEnd to formatDuration(elapsedSeconds)
        is CallState.Connecting -> ConnectingAmber to "Connecting..."
        else -> MaterialTheme.colorScheme.onSurfaceVariant to ""
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun RoundActionButton(
    icon: ImageVector,
    label: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun CallToolsRow(
    isSpeakerOn: Boolean,
    onToggleSpeaker: () -> Unit,
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    onNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        modifier = modifier
    ) {
        SecondaryControlButton(
            icon = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
            label = "Speaker",
            active = isSpeakerOn,
            onClick = onToggleSpeaker
        )
        SecondaryControlButton(
            icon = Icons.Filled.FiberManualRecord,
            label = if (isRecording) "Stop" else "Record",
            active = isRecording,
            activeColor = DeclineRed,
            onClick = onToggleRecording
        )
        SecondaryControlButton(
            icon = Icons.Filled.EditNote,
            label = "Note",
            active = false,
            onClick = onNoteClick
        )
    }
}

@Composable
private fun SecondaryControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color = AccentTealStart,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (active) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun CallButton(
    icon: ImageVector,
    background: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(background)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun SlideToAnswerButton(onAnswer: () -> Unit) {
    val scope = rememberCoroutineScope()
    val handleSize = 52.dp
    var containerWidthPx by remember { mutableStateOf(0f) }
    val handleSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { handleSize.toPx() }
    val offsetX = remember { Animatable(0f) }
    val maxOffset = (containerWidthPx - handleSizePx).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(handleSize)
            .onGloballyPositioned { containerWidthPx = it.size.width.toFloat() }
            .clip(RoundedCornerShape(handleSize / 2))
            .background(Brush.horizontalGradient(listOf(AccentTealStart, AccentTealEnd)))
    ) {
        Text(
            text = "Slide to answer",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .padding(3.dp)
                .size(handleSize - 6.dp)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(maxOffset) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value > maxOffset * 0.7f) {
                                    offsetX.animateTo(maxOffset, tween(150))
                                    onAnswer()
                                } else {
                                    offsetX.animateTo(0f, tween(150))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, tween(150)) }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val newValue = (offsetX.value + dragAmount.x).coerceIn(0f, maxOffset)
                        scope.launch { offsetX.snapTo(newValue) }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Answer",
                tint = AccentTealEnd,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private val dtmfKeys = listOf(
    '1', '2', '3',
    '4', '5', '6',
    '7', '8', '9',
    '*', '0', '#'
)

@Composable
private fun DialpadOverlay(
    onDigit: (Char) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dtmfKeys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { digit ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { onDigit(digit) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit.toString(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Close",
                color = AccentTealStart,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable(onClick = onDismiss)
            )
        }
    }
}
