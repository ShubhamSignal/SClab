package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminAuditLog
import com.example.data.model.BankAccountData
import com.example.data.model.UserData
import com.example.data.model.UserReport
import com.example.data.model.WithdrawalRequest
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
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminLoginScreen(
    adminViewModel: AdminViewModel,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            IconButton(onClick = onBack, modifier = Modifier.padding(12.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PrimaryViolet),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "SClab Administration",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Authorized Personnel Only • Server Controlled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = passcode,
                onValueChange = { passcode = it },
                label = { Text("Admin Passcode") },
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentCyan) },
                modifier = Modifier.fillMaxWidth().testTag("admin_passcode_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryVioletLight,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall, color = DangerRed)
            }

            Spacer(modifier = Modifier.height(24.dp))

            SClabButton(
                text = "Authenticate Admin",
                onClick = {
                    val ok = adminViewModel.loginAdmin(passcode)
                    if (ok) {
                        onLoginSuccess()
                    } else {
                        errorMessage = "Invalid administrative passcode."
                    }
                },
                testTag = "admin_auth_button"
            )
        }
    }
}

@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    onExitAdmin: () -> Unit
) {
    val summary by adminViewModel.summaryStats.collectAsState()
    val founderRecord by adminViewModel.founderRecord.collectAsState()
    val users by adminViewModel.usersList.collectAsState()
    val pendingBanks by adminViewModel.pendingBanks.collectAsState()
    val withdrawals by adminViewModel.withdrawals.collectAsState()
    val reports by adminViewModel.reports.collectAsState()
    val auditLogs by adminViewModel.auditLogs.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Users", "Banks", "Payouts", "Reports", "Audit")

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Panel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    TextButton(
                        onClick = {
                            adminViewModel.logoutAdmin()
                            onExitAdmin()
                        }
                    ) {
                        Text("Exit", color = AccentCyan)
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = PrimaryVioletLight,
                    edgePadding = 12.dp
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(tabTitle, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
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
            when (selectedTab) {
                0 -> AdminOverviewTab(summary = summary, founderRecord = founderRecord)
                1 -> AdminUsersTab(users = users, onToggleSuspend = { uid, susp -> adminViewModel.toggleUserSuspension(uid, susp) })
                2 -> AdminBanksTab(banks = pendingBanks, onApprove = { uid -> adminViewModel.updateBankStatus(uid, "APPROVED") }, onReject = { uid -> adminViewModel.updateBankStatus(uid, "REJECTED", "Details mismatch") })
                3 -> AdminPayoutsTab(withdrawals = withdrawals, onProcess = { reqId -> adminViewModel.updateWithdrawal(reqId, "PROCESSED", "PAYOUT_REF_${System.currentTimeMillis()}") })
                4 -> AdminReportsTab(reports = reports, onResolve = { repId -> adminViewModel.resolveReport(repId, "ACTIONED") })
                5 -> AdminAuditTab(auditLogs = auditLogs)
            }
        }
    }
}

@Composable
fun AdminOverviewTab(
    summary: com.example.data.repository.AdminSummaryStats?,
    founderRecord: com.example.data.model.FounderMembershipRecord?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        // Founder status banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (founderRecord?.isAssigned == true) AccentEmerald.copy(alpha = 0.12f) else DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (founderRecord?.isAssigned == true) AccentEmerald else DarkBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Diamond, contentDescription = null, tint = AccentEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "First Registered Member (Founder Status)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (founderRecord?.isAssigned == true) {
                    Text(
                        text = "Founder Assigned: ${founderRecord.userEmail} (ID: ${founderRecord.userId.take(12)}...)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentEmerald
                    )
                    Text(
                        text = "Plan: ₹0 Lifetime Free • Rate: ₹${founderRecord.earningRatePaisePer15s / 100}/15s",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Not Yet Claimed. The next verified registration will receive the ₹0 Lifetime Free Founder plan automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminStatBox("Total Users", "${summary?.totalUsers ?: 0}", Icons.Default.People, Modifier.weight(1f))
            AdminStatBox("Revenue", "₹${(summary?.totalRevenuePaise ?: 0L) / 100}", Icons.Default.MonetizationOn, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminStatBox("Pending Banks", "${summary?.pendingBankApprovals ?: 0}", Icons.Default.AccountBalance, Modifier.weight(1f))
            AdminStatBox("Pending Payouts", "${summary?.pendingWithdrawals ?: 0}", Icons.Default.History, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        AdminStatBox("Open Safety Reports", "${summary?.openReports ?: 0}", Icons.Default.Flag, Modifier.fillMaxWidth())
    }
}

@Composable
fun AdminStatBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdminUsersTab(
    users: List<UserData>,
    onToggleSuspend: (userId: String, suspend: Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(users, key = { it.userId }) { user ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = user.fullName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text(text = "${user.email} • ${user.gender}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusBadge(
                            text = if (user.isSuspended) "SUSPENDED" else "ACTIVE",
                            badgeColor = if (user.isSuspended) DangerRed else AccentEmerald
                        )
                    }

                    TextButton(
                        onClick = { onToggleSuspend(user.userId, !user.isSuspended) }
                    ) {
                        Text(if (user.isSuspended) "Reinstate" else "Suspend", color = if (user.isSuspended) AccentEmerald else DangerRed)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBanksTab(
    banks: List<BankAccountData>,
    onApprove: (userId: String) -> Unit,
    onReject: (userId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(banks, key = { it.userId }) { bank ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = bank.accountHolderName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text(text = "${bank.bankName} (${bank.accountNumberMasked}) • IFSC: ${bank.ifscCode}", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onReject(bank.userId) }) {
                            Text("Reject", color = DangerRed)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { onApprove(bank.userId) }) {
                            Text("Approve", color = AccentEmerald)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPayoutsTab(
    withdrawals: List<WithdrawalRequest>,
    onProcess: (requestId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(withdrawals, key = { it.id }) { req ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "₹${req.amountInr.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AccentEmerald)
                        StatusBadge(text = req.status, badgeColor = if (req.status == "PROCESSED") AccentEmerald else AccentCyan)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = req.userEmail, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text(text = req.bankDetailsSnapshot, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (req.status == "PENDING") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onProcess(req.id) }) {
                                Text("Mark as Processed / Paid", color = AccentEmerald)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminReportsTab(
    reports: List<UserReport>,
    onResolve: (reportId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(reports, key = { it.id }) { rep ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Report: ${rep.reason}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DangerRed)
                        StatusBadge(text = rep.status, badgeColor = if (rep.status == "OPEN") DangerRed else AccentEmerald)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Reported User ID: ${rep.reportedUserId}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text(text = "Details: ${rep.details.ifBlank { "None" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (rep.status == "OPEN") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onResolve(rep.id) }) {
                                Text("Action & Resolve", color = AccentEmerald)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAuditTab(auditLogs: List<AdminAuditLog>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(auditLogs, key = { it.id }) { log ->
            val dateStr = remember(log.timestamp) {
                val d = log.timestamp?.toDate()
                if (d != null) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(d) else "recently"
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = log.action, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = AccentCyan)
                        Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = log.details, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text(text = "By: ${log.adminEmail}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
