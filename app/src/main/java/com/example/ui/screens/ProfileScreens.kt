package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Membership
import com.example.data.model.UserData
import com.example.ui.components.GlassCard
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
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.HomeViewModel

@Composable
fun ProfileScreen(
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    onOpenLegalPolicy: (title: String, content: String) -> Unit,
    onSignOut: () -> Unit
) {
    val user by homeViewModel.currentUserData.collectAsState()
    val membership by homeViewModel.activeMembership.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isFounderFree = membership?.tier == "FOUNDER_FREE"

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Profile Header Card
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("profile_header_card"),
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(PrimaryViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.fullName ?: "U").take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = user?.fullName ?: "SClab User",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = user?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Membership Status Badge
                    if (isFounderFree) {
                        StatusBadge(
                            text = "Lifetime Free Membership Active",
                            badgeColor = AccentEmerald
                        )
                    } else if (membership != null && membership?.status == "ACTIVE") {
                        StatusBadge(
                            text = "${membership?.tier} Active",
                            badgeColor = PrimaryVioletLight
                        )
                    } else {
                        StatusBadge(
                            text = "Standard Account",
                            badgeColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account Details Card
            Text(
                text = "Account Details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard {
                Column {
                    DetailRow(label = "Gender", value = user?.gender ?: "Not specified")
                    DetailRow(label = "Date of Birth", value = user?.dob ?: "Not specified")
                    DetailRow(label = "Account Status", value = if (user?.isSuspended == true) "Suspended" else "Active & Verified")
                    DetailRow(label = "Call Earning Rate", value = "₹${(membership?.earningRatePaisePer15s ?: 400L) / 100} / 15-sec")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legal & Information
            Text(
                text = "Policies & Terms",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))

            ProfileMenuItem(
                title = "Terms of Service",
                icon = Icons.Default.Gavel,
                onClick = {
                    onOpenLegalPolicy(
                        "Terms of Service",
                        "SClab requires all users to be at least 18 years of age. All members agree to genuine voice and video communications. Zero tolerance for unauthorized commercial advertising, harassment, or unlawful content."
                    )
                }
            )
            ProfileMenuItem(
                title = "Earning & Ledger Policy",
                icon = Icons.Default.Star,
                onClick = {
                    onOpenLegalPolicy(
                        "Earning & Ledger Policy",
                        "Call-earning rates apply strictly to genuine, connected 15-second intervals. Unconnected calls, ring time, self-calling, and artificial call looping are prohibited and flagged by anti-fraud algorithms."
                    )
                }
            )
            ProfileMenuItem(
                title = "Withdrawal Policy & ₹2,000 Threshold",
                icon = Icons.Default.Policy,
                onClick = {
                    onOpenLegalPolicy(
                        "Withdrawal Policy",
                        "Automatic withdrawal eligibility begins when available wallet balance reaches ₹2,000 (200,000 paise). Withdrawals are subject to banking details verification and standard fraud checks."
                    )
                }
            )
            ProfileMenuItem(
                title = "Privacy Policy & Data Security",
                icon = Icons.Default.Security,
                onClick = {
                    onOpenLegalPolicy(
                        "Privacy Policy",
                        "SClab never displays personal email addresses or bank details publicly. Conversations and WebRTC calls use secure server-authorized signaling and encrypted media streams."
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Logout & Delete
            ProfileMenuItem(
                title = "Log Out",
                icon = Icons.Default.ExitToApp,
                textColor = Color.White,
                onClick = {
                    authViewModel.signOut { onSignOut() }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileMenuItem(
                title = "Delete Account",
                icon = Icons.Default.DeleteForever,
                textColor = DangerRed,
                onClick = { showDeleteDialog = true }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete your SClab account and remove all data?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.signOut { onSignOut() }
                    }
                ) {
                    Text("Delete", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

@Composable
fun ProfileMenuItem(
    title: String,
    icon: ImageVector,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .testTag("profile_item_${title.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LegalPoliciesViewerScreen(
    title: String,
    content: String,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
