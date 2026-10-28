package com.example

import com.example.data.model.BankAccountData
import com.example.data.model.FounderMembershipRecord
import com.example.data.model.MembershipTier
import com.example.data.model.WalletData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

class SClabAppUnitTest {

    @Test
    fun testMembershipTiersAndRates() {
        // Plan A: ₹100, ₹4/15s interval
        assertEquals(10000L, MembershipTier.PLAN_A.pricePaise)
        assertEquals(400L, MembershipTier.PLAN_A.ratePer15sPaise)

        // Plan B: ₹200, ₹10/15s interval
        assertEquals(20000L, MembershipTier.PLAN_B.pricePaise)
        assertEquals(1000L, MembershipTier.PLAN_B.ratePer15sPaise)

        // Founder Free: ₹0, ₹5/15s interval
        assertEquals(0L, MembershipTier.FOUNDER_FREE.pricePaise)
        assertEquals(500L, MembershipTier.FOUNDER_FREE.ratePer15sPaise)
    }

    @Test
    fun testFounderMembershipModel() {
        val founder = FounderMembershipRecord(
            id = "status",
            isAssigned = true,
            userId = "user_founder_001",
            userEmail = "founder@sclab.in",
            earningRatePaisePer15s = 500L,
            auditEventId = "AUDIT_123"
        )
        assertTrue(founder.isAssigned)
        assertEquals("user_founder_001", founder.userId)
        assertEquals(500L, founder.earningRatePaisePer15s)
    }

    @Test
    fun testWalletWithdrawalThreshold() {
        // ₹2,000 threshold = 200,000 paise
        val ineligibleWallet = WalletData(
            userId = "u1",
            availableBalancePaise = 199900L // ₹1,999
        )
        assertFalse(ineligibleWallet.isWithdrawalEligible)

        val eligibleWallet = WalletData(
            userId = "u2",
            availableBalancePaise = 200000L // ₹2,000
        )
        assertTrue(eligibleWallet.isWithdrawalEligible)
        assertEquals(2000.0, eligibleWallet.availableBalanceInr, 0.01)
    }

    @Test
    fun testBankIFSCValidation() {
        val ifscRegex = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
        assertTrue(ifscRegex.matches("SBIN0001234"))
        assertTrue(ifscRegex.matches("HDFC0000128"))
        assertFalse(ifscRegex.matches("sbin0001234")) // Lowercase invalid
        assertFalse(ifscRegex.matches("SBIN1234"))     // Too short
        assertFalse(ifscRegex.matches("SBIN1001234"))   // 5th character must be '0'
    }

    @Test
    fun testAccountNumberMasking() {
        val fullAcc = "123456789012"
        val masked = "••••••••" + fullAcc.takeLast(4)
        assertEquals("••••••••9012", masked)
    }

    @Test
    fun testCallDurationIntervalEarnings() {
        // 60-second connected call with Plan B (₹10 / 15s)
        val durationSeconds = 60L
        val intervals = durationSeconds / 15L
        val ratePaise = MembershipTier.PLAN_B.ratePer15sPaise
        val totalEarnedPaise = intervals * ratePaise
        assertEquals(4L, intervals)
        assertEquals(4000L, totalEarnedPaise) // ₹40
        assertEquals(40.0, totalEarnedPaise / 100.0, 0.01)
    }

    @Test
    fun testAgeCalculation() {
        val dob = "2000-01-01"
        val birthDate = LocalDate.parse(dob, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val now = LocalDate.now()
        val age = Period.between(birthDate, now).years
        assertTrue(age >= 18)
    }
}
