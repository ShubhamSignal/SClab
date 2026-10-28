package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.CallSession
import com.example.ui.components.SClabButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.PrimaryVioletLight
import com.example.ui.viewmodel.CallUiState
import com.example.ui.viewmodel.CallViewModel

@Composable
fun CallRootContainer(
    callViewModel: CallViewModel,
    onCallFinished: () -> Unit
) {
    val callState by callViewModel.uiState.collectAsState()

    // Request permissions for audio and video calls
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        )
    }

    when (val state = callState) {
        is CallUiState.Idle -> {
            // Nothing active
        }

        is CallUiState.Incoming -> {
            IncomingCallScreen(
                session = state.session,
                onAccept = { callViewModel.acceptIncomingCall() },
                onReject = { callViewModel.rejectIncomingCall() }
            )
        }

        is CallUiState.Outgoing -> {
            OutgoingCallScreen(
                session = state.session,
                onCancel = { callViewModel.endCurrentCall() }
            )
        }

        is CallUiState.Connected -> {
            if (state.session.callType == "VIDEO") {
                ActiveVideoCallScreen(
                    state = state,
                    onToggleMute = { callViewModel.toggleMute() },
                    onToggleSpeaker = { callViewModel.toggleSpeaker() },
                    onToggleVideo = { callViewModel.toggleVideo() },
                    onSwitchCamera = { callViewModel.switchCamera() },
                    onEndCall = { callViewModel.endCurrentCall() }
                )
            } else {
                ActiveAudioCallScreen(
                    state = state,
                    onToggleMute = { callViewModel.toggleMute() },
                    onToggleSpeaker = { callViewModel.toggleSpeaker() },
                    onEndCall = { callViewModel.endCurrentCall() }
                )
            }
        }

        is CallUiState.Ended -> {
            CallSummaryScreen(
                durationSeconds = state.durationSeconds,
                earnedPaise = state.earnedPaise,
                onDismiss = {
                    callViewModel.resetCallState()
                    onCallFinished()
                }
            )
        }
    }
}

@Composable
fun IncomingCallScreen(
    session: CallSession,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StatusBadge(
                text = "INCOMING ${session.callType} CALL",
                badgeColor = AccentEmerald
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(PrimaryViolet),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = session.callerName.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = session.callerName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Connecting via SClab WebRTC Secure Signaling...",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentCyan
            )

            Spacer(modifier = Modifier.height(64.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Reject Button
                IconButton(
                    onClick = onReject,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DangerRed)
                        .testTag("reject_call_button")
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                // Accept Button
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentEmerald)
                        .testTag("accept_call_button")
                ) {
                    Icon(
                        if (session.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Phone,
                        contentDescription = "Accept",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OutgoingCallScreen(
    session: CallSession,
    onCancel: () -> Unit
) {
    val transition = rememberInfiniteTransition()
    val pulseScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StatusBadge(
                text = "OUTGOING ${session.callType} CALL",
                badgeColor = AccentCyan
            )
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(PrimaryViolet.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryViolet),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = session.receiverName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = session.receiverName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ringing... Establishing WebRTC peer connection",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(60.dp))

            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DangerRed)
                    .testTag("cancel_call_button")
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun ActiveAudioCallScreen(
    state: CallUiState.Connected,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    val durationText = formatSeconds(state.durationSeconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatusBadge(text = "CONNECTED • WEBRTC SECURE", badgeColor = AccentEmerald)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.session.callerName.ifBlank { state.session.receiverName },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = AccentCyan
                )

                if (state.estimatedEarningsPaise > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current Call Earning: ₹${state.estimatedEarningsPaise / 100}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = AccentEmerald
                    )
                }
            }

            // Audio wave visualizer animation
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(PrimaryViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Wave",
                    tint = PrimaryVioletLight,
                    modifier = Modifier.size(68.dp)
                )
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (state.isMuted) PrimaryViolet else DarkSurfaceElevated)
                        .testTag("mute_audio_button")
                ) {
                    Icon(
                        if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = Color.White
                    )
                }

                // End call
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(DangerRed)
                        .testTag("end_audio_call_button")
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End", tint = Color.White, modifier = Modifier.size(30.dp))
                }

                // Speaker
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (state.isSpeakerOn) PrimaryViolet else DarkSurfaceElevated)
                        .testTag("speaker_toggle_button")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Speaker", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ActiveVideoCallScreen(
    state: CallUiState.Connected,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val durationText = formatSeconds(state.durationSeconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Remote Video Feed (Simulated full screen WebRTC peer stream)
        Box(
            modifier = Modifier.fillMaxSize().background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(PrimaryViolet),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.session.callerName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "WebRTC Video Stream Active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentCyan
                )
            }
        }

        // Local Camera Preview via CameraX in top-right PIP
        if (state.isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
                    .size(width = 110.dp, height = 150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, PrimaryVioletLight, RoundedCornerShape(14.dp))
                    .background(Color.DarkGray)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val cameraSelector = if (state.isFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            } catch (e: Exception) {
                                // Camera in use or unavailable
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top info bar
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(text = durationText, badgeColor = AccentEmerald)
            if (state.estimatedEarningsPaise > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(text = "₹${state.estimatedEarningsPaise / 100}", badgeColor = AccentCyan)
            }
        }

        // Bottom Call Controls
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleMute, modifier = Modifier.testTag("video_mute_button")) {
                    Icon(if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = null, tint = Color.White)
                }

                IconButton(onClick = onToggleVideo, modifier = Modifier.testTag("video_toggle_button")) {
                    Icon(if (state.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff, contentDescription = null, tint = Color.White)
                }

                IconButton(onClick = onSwitchCamera, modifier = Modifier.testTag("camera_switch_button")) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = null, tint = Color.White)
                }

                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(DangerRed)
                        .testTag("end_video_call_button")
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun CallSummaryScreen(
    durationSeconds: Long,
    earnedPaise: Long,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(PrimaryViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryVioletLight, modifier = Modifier.size(38.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Call Completed",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Duration: ${formatSeconds(durationSeconds)} (${durationSeconds / 15L} qualifying intervals)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (earnedPaise > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                StatusBadge(text = "+ ₹${earnedPaise / 100} Credited to Wallet", badgeColor = AccentEmerald)
            }

            Spacer(modifier = Modifier.height(36.dp))

            SClabButton(
                text = "Done",
                onClick = onDismiss,
                testTag = "call_summary_done_button"
            )
        }
    }
}

fun formatSeconds(totalSecs: Long): String {
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}
