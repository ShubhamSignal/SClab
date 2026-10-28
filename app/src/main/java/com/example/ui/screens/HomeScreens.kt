package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PublicProfile
import com.example.ui.components.GlassCard
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
import com.example.ui.viewmodel.HomeViewModel

@Composable
fun DiscoverScreen(
    homeViewModel: HomeViewModel,
    onStartChat: (targetUserId: String, targetUserName: String) -> Unit,
    onStartAudioCall: (targetUserId: String, targetUserName: String) -> Unit,
    onStartVideoCall: (targetUserId: String, targetUserName: String) -> Unit,
    onOpenMembership: () -> Unit
) {
    val profiles by homeViewModel.discoverProfiles.collectAsState()
    val currentUser by homeViewModel.currentUserData.collectAsState()
    val membership by homeViewModel.activeMembership.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()

    var selectedProfileForAction by remember { mutableStateOf<PublicProfile?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var reportDetails by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        homeViewModel.loadData()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Discover People",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = if (currentUser?.gender == "Male") "Matched with verified women" else "Matched with verified men",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentCyan
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (membership != null && membership?.tier == "FOUNDER_FREE") {
                        StatusBadge(text = "FOUNDER FREE", badgeColor = AccentEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = { homeViewModel.loadData() },
                        modifier = Modifier.testTag("refresh_discover_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && profiles.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = PrimaryVioletLight)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Finding eligible people...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (profiles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No New People Right Now",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check back soon or refresh. SClab strictly enforces verified adult accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    SClabButton(
                        text = "Refresh",
                        onClick = { homeViewModel.loadData() },
                        modifier = Modifier.width(180.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(profiles, key = { it.userId }) { profile ->
                        DiscoverProfileCard(
                            profile = profile,
                            onChat = { onStartChat(profile.userId, profile.displayName) },
                            onAudioCall = { onStartAudioCall(profile.userId, profile.displayName) },
                            onVideoCall = { onStartVideoCall(profile.userId, profile.displayName) },
                            onBlock = {
                                homeViewModel.blockUser(profile.userId) {}
                            },
                            onReport = {
                                selectedProfileForAction = profile
                                showReportDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog && selectedProfileForAction != null) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text("Report User: ${selectedProfileForAction?.displayName}")
            },
            text = {
                Column {
                    Text(
                        text = "Please select reason for report. SClab investigates all safety and policy violations.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Reason (e.g. Harassment, Spam, Fraud)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        label = { Text("Additional Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val prof = selectedProfileForAction ?: return@TextButton
                        homeViewModel.reportUser(prof.userId, reportReason.ifBlank { "Policy Violation" }, reportDetails) {
                            showReportDialog = false
                            reportReason = ""
                            reportDetails = ""
                        }
                    }
                ) {
                    Text("Submit Report", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DiscoverProfileCard(
    profile: PublicProfile,
    onChat: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discover_card_${profile.userId}"),
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar placeholder with initials
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryViolet, AccentCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${profile.age} yrs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (profile.isOnline) AccentEmerald else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (profile.isOnline) "Active Now" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (profile.isOnline) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${profile.gender}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyan
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp).testTag("profile_menu_button_${profile.userId}")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report User") },
                            onClick = {
                                showMenu = false
                                onReport()
                            },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = DangerRed) }
                        )
                        DropdownMenuItem(
                            text = { Text("Block User") },
                            onClick = {
                                showMenu = false
                                onBlock()
                            },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = DangerRed) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action row: Chat, Voice Call, Video Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionChipButton(
                    text = "Chat",
                    icon = Icons.Default.Chat,
                    containerColor = DarkSurfaceElevated,
                    contentColor = AccentCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onChat,
                    testTag = "chat_action_${profile.userId}"
                )
                ActionChipButton(
                    text = "Audio",
                    icon = Icons.Default.Phone,
                    containerColor = DarkSurfaceElevated,
                    contentColor = PrimaryVioletLight,
                    modifier = Modifier.weight(1f),
                    onClick = onAudioCall,
                    testTag = "audio_call_action_${profile.userId}"
                )
                ActionChipButton(
                    text = "Video",
                    icon = Icons.Default.Videocam,
                    containerColor = PrimaryViolet,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = onVideoCall,
                    testTag = "video_call_action_${profile.userId}"
                )
            }
        }
    }
}

@Composable
fun ActionChipButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor
            )
        }
    }
}
