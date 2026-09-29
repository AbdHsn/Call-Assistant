package com.callassistant.incall

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.callassistant.R
import com.callassistant.receiver.CallReminderReceiver
import com.callassistant.data.entity.Contact
import com.callassistant.ui.phonebook.PhoneBookViewModel
import com.callassistant.ui.MainViewModel
import com.callassistant.ui.theme.AccentTealStart
import com.callassistant.ui.theme.AccentTealEnd
import com.callassistant.ui.theme.CallAssistantTheme
import com.callassistant.ui.theme.ConnectingAmber
import com.callassistant.ui.theme.ErrorRed
import com.callassistant.ui.theme.SuccessGreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.callassistant.service.CallRecordingService
import com.callassistant.util.CallNotificationManager
import com.callassistant.util.CallNotesStore
import com.callassistant.util.CallRecordingState
import com.callassistant.util.ProximityWakeLockHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@AndroidEntryPoint
class InCallActivity : ComponentActivity() {

    @Inject
    lateinit var session: CallSessionManager

    private val viewModel: MainViewModel by viewModels()
    private val phoneBookViewModel: PhoneBookViewModel by viewModels()
    private lateinit var proximityWakeLock: ProximityWakeLockHelper

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        proximityWakeLock = ProximityWakeLockHelper(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val contactsUiState by phoneBookViewModel.uiState.collectAsState()

            val callState by session.callState.collectAsState()
            val connectTimestamp by session.callConnectTimestamp.collectAsState()
            val isMuted by session.isMuted.collectAsState()
            val audioRoute by session.audioRoute.collectAsState()
            val supportedAudioRoutes by session.supportedAudioRoutes.collectAsState()
            val bluetoothDeviceName by session.bluetoothDeviceName.collectAsState()

            SideEffect {
                updateProximityScreenBehavior(callState, audioRoute)
            }

            var hasSeenLiveCall by remember { mutableStateOf(false) }
            LaunchedEffect(callState) {
                when (callState) {
                    is CallState.Incoming,
                    is CallState.Active,
                    is CallState.Connecting,
                    CallState.None -> hasSeenLiveCall = true
                    is CallState.Ended -> if (hasSeenLiveCall) finish()
                }
            }

            CallAssistantTheme(themeMode = themeMode) {
                InCallScreen(
                    contacts = contactsUiState.contacts,
                    callState = callState,
                    connectTimestamp = connectTimestamp,
                    isMuted = isMuted,
                    audioRoute = audioRoute,
                    supportedAudioRoutes = supportedAudioRoutes,
                    bluetoothDeviceName = bluetoothDeviceName,
                    onAnswer = { session.answer() },
                    onReject = { session.reject() },
                    onHangUp = { session.hangUp() },
                    onToggleMute = { session.setMuted(!isMuted) },
                    onSelectAudioRoute = { session.setAudioRoute(it) },
                    onSendDigit = { digit -> session.sendDtmf(digit) }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        publishOngoingCallNotificationIfNeeded()
    }

    override fun onDestroy() {
        proximityWakeLock.release()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    private fun publishOngoingCallNotificationIfNeeded() {
        when (val state = session.callState.value) {
            is CallState.Active -> CallNotificationManager.showOngoingCallNotification(
                this,
                state.number,
                state.displayName
            )
            is CallState.Connecting -> CallNotificationManager.showOngoingCallNotification(
                this,
                state.number,
                state.displayName
            )
            is CallState.Incoming -> CallNotificationManager.showIncomingCallNotification(
                this,
                state.number,
                state.displayName
            )
            CallState.None -> if (session.activeCall != null) {
                CallNotificationManager.showOngoingCallNotification(this, "", null)
            }
            else -> Unit
        }
    }

    private fun updateProximityScreenBehavior(callState: CallState, audioRoute: Int) {
        when {
            !isEarpieceAudioRoute(audioRoute) -> {
                proximityWakeLock.release()
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            callState is CallState.Active || callState is CallState.Connecting -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                proximityWakeLock.acquire()
            }
            callState is CallState.Incoming -> {
                proximityWakeLock.release()
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            else -> {
                proximityWakeLock.release()
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

@Composable
private fun InCallScreen(
    contacts: List<Contact>,
    callState: CallState,
    connectTimestamp: Long?,
    isMuted: Boolean,
    audioRoute: Int,
    supportedAudioRoutes: Int,
    bluetoothDeviceName: String?,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onSelectAudioRoute: (Int) -> Unit,
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
    // Strip USSD/MMI characters for contact matching
    val sanitizedNumber = number.filter { it.isDigit() || it == '+' }
    val contact = contacts.find { 
        it.phoneNumber == number || 
        (sanitizedNumber.isNotBlank() && it.phoneNumber.filter { c -> c.isDigit() || c == '+' } == sanitizedNumber)
    }
    val resolvedPhotoUri = contact?.photoUri
    val resolvedName = displayName ?: contact?.name ?: number.ifBlank { "Unknown" }
    var showDialpad by remember { mutableStateOf(false) }
    var showAudioRouteDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    var isRecording by remember { mutableStateOf(CallRecordingService.isRecording) }
    var isAutoRecording by remember { mutableStateOf(CallRecordingState.isAutoRecording) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var wasRecording by remember { mutableStateOf(false) }
    var lastFailureShown by remember { mutableStateOf<String?>(null) }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            CallRecordingService.start(
                context,
                number,
                isIncoming = callState is CallState.Incoming,
                manual = true
            )
        } else {
            Toast.makeText(context, "Microphone permission is required to record", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleRecording() {
        if (CallRecordingService.isRecording) {
            CallRecordingService.stop(context)
            Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            CallRecordingService.start(
                context,
                number,
                isIncoming = callState is CallState.Incoming,
                manual = true
            )
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(callState) {
        while (callState is CallState.Active ||
            callState is CallState.Connecting ||
            callState is CallState.Incoming
        ) {
            val recordingNow = CallRecordingService.isRecording
            val autoNow = CallRecordingState.isAutoRecording
            if (recordingNow && !wasRecording) {
                Toast.makeText(
                    context,
                    if (autoNow) "Auto-recording started" else "Recording started",
                    Toast.LENGTH_SHORT
                ).show()
            }
            CallRecordingState.lastFailureMessage?.let { message ->
                if (message != lastFailureShown) {
                    lastFailureShown = message
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
            isRecording = recordingNow
            isAutoRecording = autoNow
            recordingSeconds = if (recordingNow && CallRecordingState.startedAtMillis > 0L) {
                ((System.currentTimeMillis() - CallRecordingState.startedAtMillis) / 1000L).toInt()
            } else {
                0
            }
            wasRecording = recordingNow
            delay(500)
        }
    }

    LaunchedEffect(callState) {
        if (callState is CallState.Ended) {
            if (CallRecordingService.isRecording) {
                CallRecordingService.stop(context)
            }
            isRecording = false
            isAutoRecording = false
            wasRecording = false
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
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = "Call Assistant",
                modifier = Modifier.size(32.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CallerAvatar(name = resolvedName, photoUri = resolvedPhotoUri, pulsing = callState is CallState.Incoming)
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = resolvedName,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (resolvedName != number && number.isNotBlank()) number else "Mobile",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                StatusPill(callState = callState, elapsedSeconds = elapsedSeconds)
                if (isRecording) {
                    Spacer(modifier = Modifier.height(10.dp))
                    RecordingIndicator(
                        isAutoRecording = isAutoRecording,
                        recordingSeconds = recordingSeconds
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (callState) {
                    is CallState.Incoming -> {
                        CallToolsRow(
                            audioRoute = audioRoute,
                            bluetoothDeviceName = bluetoothDeviceName,
                            onAudioClick = { showAudioRouteDialog = true },
                            isRecording = isRecording,
                            isAutoRecording = isAutoRecording,
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
                                tint = ErrorRed,
                                onClick = onReject
                            )
                        }
                        SwipeAnswerRejectButton(onAnswer = onAnswer, onReject = onReject)
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
                            audioRoute = audioRoute,
                            bluetoothDeviceName = bluetoothDeviceName,
                            onAudioClick = { showAudioRouteDialog = true },
                            isRecording = isRecording,
                            isAutoRecording = isAutoRecording,
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
                        CallButton(Icons.Filled.CallEnd, ErrorRed, "End", onHangUp)
                    }
                    is CallState.Connecting, CallState.None -> {
                        CallToolsRow(
                            audioRoute = audioRoute,
                            bluetoothDeviceName = bluetoothDeviceName,
                            onAudioClick = { showAudioRouteDialog = true },
                            isRecording = isRecording,
                            isAutoRecording = isAutoRecording,
                            onToggleRecording = { toggleRecording() },
                            onNoteClick = { noteText = ""; showNoteDialog = true },
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        CallButton(Icons.Filled.CallEnd, ErrorRed, "Cancel", onHangUp)
                    }
                    is CallState.Ended -> {}
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

    if (showAudioRouteDialog) {
        AudioRoutePickerDialog(
            currentRoute = audioRoute,
            supportedRoutes = supportedAudioRoutes,
            bluetoothDeviceName = bluetoothDeviceName,
            onSelectRoute = {
                onSelectAudioRoute(it)
                showAudioRouteDialog = false
            },
            onDismiss = { showAudioRouteDialog = false }
        )
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
private fun CallerAvatar(name: String, photoUri: String?, pulsing: Boolean) {
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
    val context = LocalContext.current
    var bitmap by remember(photoUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photoUri) {
        bitmap = if (photoUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
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
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = "Contact photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(text = initial, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatusPill(callState: CallState, elapsedSeconds: Int) {
    val (dotColor, label) = when (callState) {
        is CallState.Incoming -> AccentTealStart to "Incoming call"
        is CallState.Active -> AccentTealEnd to formatDuration(elapsedSeconds)
        is CallState.Connecting, CallState.None -> ConnectingAmber to "Connecting..."
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
private fun AudioRoutePickerDialog(
    currentRoute: Int,
    supportedRoutes: Int,
    bluetoothDeviceName: String?,
    onSelectRoute: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = supportedCallAudioRoutes(supportedRoutes, bluetoothDeviceName)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio output") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (options.isEmpty()) {
                    Text("No audio outputs available")
                } else {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectRoute(option.route) }
                                .background(
                                    if (option.route == currentRoute) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = option.label,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                            if (option.route == currentRoute) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = AccentTealStart
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RecordingIndicator(
    isAutoRecording: Boolean,
    recordingSeconds: Int
) {
    Row(
        modifier = Modifier
            .background(ErrorRed.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ErrorRed)
        )
        Text(
            text = buildString {
                append("REC ")
                append(formatRecordingDuration(recordingSeconds))
                if (isAutoRecording) append(" · Auto")
            },
            color = ErrorRed,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

private fun formatRecordingDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun CallToolsRow(
    audioRoute: Int,
    bluetoothDeviceName: String?,
    onAudioClick: () -> Unit,
    isRecording: Boolean,
    isAutoRecording: Boolean,
    onToggleRecording: () -> Unit,
    onNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioLabel = callAudioRouteLabel(audioRoute, bluetoothDeviceName)
    val audioIcon = callAudioRouteIcon(audioRoute)

    Row(
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        modifier = modifier
    ) {
        SecondaryControlButton(
            icon = audioIcon,
            label = audioLabel,
            active = audioRoute != android.telecom.CallAudioState.ROUTE_EARPIECE,
            onClick = onAudioClick
        )
        SecondaryControlButton(
            icon = Icons.Filled.FiberManualRecord,
            label = when {
                isRecording && isAutoRecording -> "Stop auto"
                isRecording -> "Stop rec"
                else -> "Record"
            },
            active = isRecording,
            activeColor = ErrorRed,
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
private fun SwipeAnswerRejectButton(onAnswer: () -> Unit, onReject: () -> Unit) {
    val scope = rememberCoroutineScope()
    val handleSize = 52.dp
    val threshold = with(androidx.compose.ui.platform.LocalDensity.current) { 80.dp.toPx() }
    val maxOffset = with(androidx.compose.ui.platform.LocalDensity.current) { 140.dp.toPx() }
    val offsetX = remember { Animatable(0f) }

    val backgroundColor = when {
        offsetX.value > 0 -> SuccessGreen.copy(alpha = (offsetX.value / threshold).coerceIn(0f, 1f))
        offsetX.value < 0 -> ErrorRed.copy(alpha = (-offsetX.value / threshold).coerceIn(0f, 1f))
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(handleSize)
            .clip(RoundedCornerShape(handleSize / 2))
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value > threshold) {
                                offsetX.animateTo(maxOffset, tween(150))
                                onAnswer()
                            } else if (offsetX.value < -threshold) {
                                offsetX.animateTo(-maxOffset, tween(150))
                                onReject()
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
                    val newValue = (offsetX.value + dragAmount.x).coerceIn(-maxOffset, maxOffset)
                    scope.launch { offsetX.snapTo(newValue) }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Swipe to answer or reject",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .padding(3.dp)
                .size(handleSize - 6.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (offsetX.value >= 0) Icons.Filled.Call else Icons.Filled.CallEnd,
                contentDescription = if (offsetX.value >= 0) "Answer" else "Decline",
                tint = if (offsetX.value >= 0) SuccessGreen else ErrorRed,
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
