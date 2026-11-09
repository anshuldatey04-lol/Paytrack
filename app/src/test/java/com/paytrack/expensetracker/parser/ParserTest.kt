package com.paytrack.expensetracker.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun gpayParser() = ConfigParser(
    packageName = "com.google.android.apps.nbu.paisa.user",
    appLabel = "GPay",
    debitKeywords = listOf("paid", "sent", "debited", "payment successful"),
    creditKeywords = listOf("received", "credited", "money received"),
    amountPatterns = listOf(
        "(?:Rs\\.?|₹|INR)\\s?(?<amount>[0-9,]+(?:\\.[0-9]{1,2})?)",
        "(?<amount>[0-9,]+(?:\\.[0-9]{1,2})?)\\s?(?:Rs\\.?|₹|INR)",
    ),
    payeePatterns = listOf(
        "(?:Paid|Sent)\\s+(?:Rs\\.?|₹)?\\s?[0-9,.]+\\s+to\\s+(?<payee>[A-Za-z0-9 .&'_-]{2,60})",
    ),
    genericAmountPatterns = listOf("(?:Rs\\.?|₹|INR)\\s?(?<amount>[0-9,]+(?:\\.[0-9]{1,2})?)"),
    genericDebit = listOf("paid", "sent", "debited"),
    genericCredit = listOf("received", "credited"),
)

/** PRD §13 — real-format parser checks for GPay / PhonePe / Paytm shapes + credit exclusion + dedup. */
class ParserTest {

    @Test
    fun gpayDebit_parsesAmountAndPayee() {
        val r = gpayParser().parse("Payment successful", "Paid ₹250.00 to Swiggy UPI transaction ID ...")
        assertEquals(250.0, r.amount!!, 0.001)
        assertEquals(true, r.isDebit)
        assertTrue(r.merchant!!.contains("Swiggy"))
    }

    @Test
    fun phonePeShape_fallsBackToGenericAmount() {
        val r = gpayParser().parse("PhonePe", "Transaction successful Rs. 1,499 paid to Amazon")
        assertEquals(1499.0, r.amount!!, 0.001)
        assertEquals(true, r.isDebit)
    }

    @Test
    fun credit_neverCountsAsDebit() {
        val r = gpayParser().parse("Money received", "₹500 received from Rahul")
        assertEquals(false, r.isDebit)
    }

    @Test
    fun ambiguous_goesToReview() {
        val r = gpayParser().parse("Offer", "50% cashback this weekend only!")
        assertNull(r.amount)
        assertNull(r.isDebit)
    }

    @Test
    fun dedup_suppressesRepeatWithinWindow() {
        val d = DedupFilter()
        assertFalse(d.isDuplicate("pkg", 100.0, 8000))
        assertTrue(d.isDuplicate("pkg", 100.0, 8000))
    }
}
