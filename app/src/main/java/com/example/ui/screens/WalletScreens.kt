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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccountData
import com.example.data.model.EarningLedgerEntry
import com.example.data.model.WalletData
import com.example.ui.components.GlassCard
import com.example.ui.components.LegalNoticeCard
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
import com.example.ui.viewmodel.WalletActionState
import com.example.ui.viewmodel.WalletViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WalletScreen(
    walletViewModel: WalletViewModel,
    onOpenBankDetails: () -> Unit,
    onOpenTransactionHistory: () -> Unit
) {
    val wallet by walletViewModel.wallet.collectAsState()
    val bankAccount by walletViewModel.bankAccount.collectAsState()
    val actionState by walletViewModel.actionState.collectAsState()

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("2000") }

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
                Text(
                    text = "SClab Wallet",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                IconButton(onClick = onOpenTransactionHistory, modifier = Modifier.testTag("wallet_history_button")) {
                    Icon(Icons.Default.History, contentDescription = "History", tint = AccentCyan)
                }
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
            // Main Balance Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_balance_card"),
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AVAILABLE BALANCE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AccentCyan)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val balanceInr = wallet?.availableBalanceInr ?: 0.0
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.2f", balanceInr)}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lifetime Earned",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.0f", wallet?.lifetimeEarningsInr ?: 0.0)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AccentEmerald
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Withdrawn",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val totalWithdrawn = (wallet?.totalWithdrawnPaise ?: 0L) / 100.0
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.0f", totalWithdrawn)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    SClabButton(
                        text = "Request Withdrawal",
                        onClick = { showWithdrawDialog = true },
                        enabled = (wallet?.availableBalancePaise ?: 0L) >= 200000L,
                        testTag = "request_withdrawal_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Mandatory Notice from Requirements
            LegalNoticeCard(
                noticeText = "Automatic withdrawal eligibility begins when your available wallet balance reaches ₹2,000. Withdrawals remain subject to account verification, fraud checks, and successful payout processing."
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bank Account Status Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBankDetails() }
                    .testTag("bank_details_card"),
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Linked Bank Account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (bankAccount != null) {
                            Text(
                                text = "${bankAccount?.bankName} (${bankAccount?.accountNumberMasked})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Tap to add bank account for payouts",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryVioletLight
                            )
                        }
                    }

                    StatusBadge(
                        text = bankAccount?.status ?: "NOT ADDED",
                        badgeColor = when (bankAccount?.status) {
                            "APPROVED", "PAYOUT_ELIGIBLE" -> AccentEmerald
                            "PENDING" -> AccentCyan
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Earning Policy Highlights",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard {
                Column {
                    PolicyBullet("Call-earning rates credit every 15 seconds of connected call time.")
                    PolicyBullet("Unconnected, missed, or rejected calls do not qualify for earnings.")
                    PolicyBullet("Self-calls and artificial cycling are blocked by anti-fraud algorithms.")
                    PolicyBullet("All transactions are immutably logged on the server ledger in paise.")
                }
            }
        }
    }

    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("Withdraw Funds") },
            text = {
                Column {
                    Text(
                        text = "Available Balance: ₹${wallet?.availableBalanceInr ?: 0.0}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("Amount (INR)") },
                        modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Minimum withdrawal amount is ₹2,000.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = withdrawAmount.toDoubleOrNull() ?: 2000.0
                        walletViewModel.requestWithdrawal(amt)
                        showWithdrawDialog = false
                    }
                ) {
                    Text("Confirm Withdrawal", color = AccentEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PolicyBullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AccentCyan))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

@Composable
fun BankDetailsScreen(
    walletViewModel: WalletViewModel,
    onBack: () -> Unit
) {
    val bankAccount by walletViewModel.bankAccount.collectAsState()
    val actionState by walletViewModel.actionState.collectAsState()

    var holderName by remember { mutableStateOf(bankAccount?.accountHolderName ?: "") }
    var bankName by remember { mutableStateOf(bankAccount?.bankName ?: "") }
    var accountNumber by remember { mutableStateOf("") }
    var confirmAccountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf(bankAccount?.ifscCode ?: "") }
    var confirmDetails by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    text = "Bank Details for Payouts",
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (bankAccount != null) {
                GlassCard(
                    borderColor = AccentCyan.copy(alpha = 0.5f),
                    backgroundColor = DarkSurface
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusBadge(text = bankAccount?.status ?: "PENDING", badgeColor = AccentEmerald)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${bankAccount?.bankName} • ${bankAccount?.accountNumberMasked}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "IFSC: ${bankAccount?.ifscCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = "Update / Register Bank Account",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = holderName,
                onValueChange = { holderName = it },
                label = { Text("Account Holder Name") },
                modifier = Modifier.fillMaxWidth().testTag("bank_holder_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryVioletLight,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank Name") },
                placeholder = { Text("e.g. State Bank of India, HDFC Bank") },
                modifier = Modifier.fillMaxWidth().testTag("bank_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryVioletLight,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Account Number") },
                modifier = Modifier.fillMaxWidth().testTag("bank_account_number_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryVioletLight,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = confirmAccountNumber,
                onValueChange = { confirmAccountNumber = it },
                label = { Text("Confirm Account Number") },
                modifier = Modifier.fillMaxWidth().testTag("bank_confirm_account_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryVioletLight,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = ifscCode,
                onValueChange = { ifscCode = it.uppercase() },
                label = { Text("IFSC Code") },
                placeholder = { Text("e.g. SBIN0001234") },
                modifier = Modifier.fillMaxWidth().testTag("bank_ifsc_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryVioletLight,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { confirmDetails = !confirmDetails },
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Checkbox(
                    checked = confirmDetails,
                    onCheckedChange = { confirmDetails = it }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "I confirm that the above bank details are accurate and belong to me.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall, color = DangerRed)
            }

            if (actionState is WalletActionState.Success) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (actionState as WalletActionState.Success).message,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentEmerald
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SClabButton(
                text = "Save Bank Details",
                onClick = {
                    if (holderName.isBlank() || bankName.isBlank() || accountNumber.isBlank() || ifscCode.isBlank()) {
                        errorMessage = "Please fill in all bank details."
                        return@SClabButton
                    }
                    if (accountNumber != confirmAccountNumber) {
                        errorMessage = "Account numbers do not match."
                        return@SClabButton
                    }
                    if (!confirmDetails) {
                        errorMessage = "Please confirm that your bank details are correct."
                        return@SClabButton
                    }
                    errorMessage = null
                    walletViewModel.submitBankDetails(holderName, bankName, accountNumber, ifscCode)
                },
                testTag = "submit_bank_details_button"
            )
        }
    }
}

@Composable
fun TransactionHistoryScreen(
    walletViewModel: WalletViewModel,
    onBack: () -> Unit
) {
    val ledger by walletViewModel.ledger.collectAsState()

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
                    text = "Transaction History & Ledger",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    ) { padding ->
        if (ledger.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No earning transactions recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ledger, key = { it.id }) { entry ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.note.ifBlank { "Call Earning" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "TXN: ${entry.id.take(16)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "+ ₹${String.format(Locale.getDefault(), "%.2f", entry.amountInr)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AccentEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}
