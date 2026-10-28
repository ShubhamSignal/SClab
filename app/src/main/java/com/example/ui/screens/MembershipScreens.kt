package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MembershipTier
import com.example.ui.components.GlassCard
import com.example.ui.components.LegalNoticeCard
import com.example.ui.components.SClabButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.PrimaryVioletLight
import com.example.ui.viewmodel.MembershipUiState
import com.example.ui.viewmodel.MembershipViewModel

@Composable
fun MembershipSelectionScreen(
    membershipViewModel: MembershipViewModel,
    onMembershipConfirmed: () -> Unit
) {
    val context = LocalContext.current
    val uiState by membershipViewModel.uiState.collectAsState()

    Scaffold(
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is MembershipUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryVioletLight)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Checking Authoritative Membership Status...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is MembershipUiState.FounderFreeActive -> {
                    // Confirmed First User with ₹0 Lifetime Free Membership!
                    FounderFreeSuccessView(
                        onProceedToHome = onMembershipConfirmed
                    )
                }

                is MembershipUiState.PaidTierActive -> {
                    PaidMembershipSuccessView(
                        tierName = state.membership.tier,
                        rateDisplay = "₹${state.membership.earningRatePaisePer15s / 100} / 15-sec",
                        onProceedToHome = onMembershipConfirmed
                    )
                }

                is MembershipUiState.SelectionRequired -> {
                    SelectionRequiredView(
                        plans = state.plans,
                        onSelectPlan = { tier ->
                            membershipViewModel.selectPaidPlan(context, tier)
                        }
                    )
                }

                is MembershipUiState.PaymentPending -> {
                    PaymentVerificationDialog(
                        order = state.order,
                        onVerifyPayment = { ref ->
                            val tier = try {
                                MembershipTier.valueOf(state.order.tier)
                            } catch (e: Exception) {
                                MembershipTier.PLAN_A
                            }
                            membershipViewModel.verifyAndActivatePayment(state.order.id, ref, tier)
                        },
                        onCancel = {
                            membershipViewModel.loadMembershipStatus()
                        }
                    )
                }

                is MembershipUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SClabButton(
                            text = "Retry Check",
                            onClick = { membershipViewModel.loadMembershipStatus() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FounderFreeSuccessView(
    onProceedToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(AccentEmerald.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = null,
                tint = AccentEmerald,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        StatusBadge(
            text = "Lifetime Free Membership Active",
            badgeColor = AccentEmerald
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Founder Free Membership — ₹0",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "First Member — Lifetime Free",
            style = MaterialTheme.typography.titleMedium,
            color = AccentCyan,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = AccentEmerald.copy(alpha = 0.5f)
        ) {
            Column {
                BenefitRow("Lifetime ₹0 Membership Fee (Never charged again)")
                BenefitRow("Eligible Call-Earning Rate: ₹5 per 15-second call")
                BenefitRow("Full Access to Audio & Video Communication")
                BenefitRow("Standard Wallet & Bank Account Withdrawal Eligibility")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LegalNoticeCard(
            noticeText = "This lifetime free membership exemption applies to membership fees only. Withdrawals remain subject to the standard ₹2,000 threshold, fraud-prevention checks, and banking verification."
        )

        Spacer(modifier = Modifier.height(32.dp))

        SClabButton(
            text = "Enter SClab Home",
            onClick = onProceedToHome,
            testTag = "founder_proceed_home_button"
        )
    }
}

@Composable
fun PaidMembershipSuccessView(
    tierName: String,
    rateDisplay: String,
    onProceedToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryViolet.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PrimaryVioletLight,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        StatusBadge(text = "Membership Active", badgeColor = AccentEmerald)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "$tierName Activated",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Earning Rate: $rateDisplay",
            style = MaterialTheme.typography.bodyMedium,
            color = AccentCyan
        )

        Spacer(modifier = Modifier.height(36.dp))

        SClabButton(
            text = "Proceed to Discover",
            onClick = onProceedToHome,
            testTag = "paid_proceed_home_button"
        )
    }
}

@Composable
fun SelectionRequiredView(
    plans: List<MembershipTier>,
    onSelectPlan: (MembershipTier) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Select SClab Membership",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "One-time membership activation. No recurring subscription.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        plans.forEach { tier ->
            PlanCard(
                tier = tier,
                onSelect = { onSelectPlan(tier) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        LegalNoticeCard(
            noticeText = "Membership payments are processed securely via Razorpay Payment Links in INR. Membership activates immediately upon server-side payment verification. Automatic withdrawal eligibility begins at ₹2,000 wallet balance."
        )
    }
}

@Composable
fun PlanCard(
    tier: MembershipTier,
    onSelect: () -> Unit
) {
    val isPlanB = tier == MembershipTier.PLAN_B
    val borderColor = if (isPlanB) PrimaryVioletLight else DarkBorder
    val tag = if (isPlanB) "MOST POPULAR" else "STARTER"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("plan_card_${tier.name}"),
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = tag,
                    badgeColor = if (isPlanB) PrimaryVioletLight else AccentCyan
                )
                Text(
                    text = tier.priceDisplay,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tier.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = tier.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            BenefitRow("Call-Earning Rate: ${tier.rateDisplay}")
            BenefitRow("Lifetime Access to Audio & Video Calls")
            BenefitRow("INR Wallet with Bank Payout Transfer")

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isPlanB) PrimaryViolet else DarkSurfaceElevated)
                    .clickable { onSelect() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pay ${tier.priceDisplay} via Razorpay",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BenefitRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = AccentEmerald,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
fun PaymentVerificationDialog(
    order: com.example.data.model.PaymentOrder,
    onVerifyPayment: (paymentReference: String) -> Unit,
    onCancel: () -> Unit
) {
    var paymentRef by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(PrimaryViolet.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Payment in Progress",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Complete your payment on the Razorpay page, then verify below to activate your membership.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = paymentRef,
            onValueChange = { paymentRef = it },
            label = { Text("Razorpay Payment ID / Reference") },
            placeholder = { Text("e.g. pay_N23abc123") },
            modifier = Modifier.fillMaxWidth().testTag("payment_ref_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryVioletLight,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        SClabButton(
            text = "Verify & Activate Membership",
            onClick = {
                val ref = paymentRef.ifBlank { "pay_mock_${System.currentTimeMillis()}" }
                onVerifyPayment(ref)
            },
            testTag = "verify_payment_button"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Cancel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onCancel() }
        )
    }
}
