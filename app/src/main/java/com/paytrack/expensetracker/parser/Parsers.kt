package com.paytrack.expensetracker.parser

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Result of parsing one posted notification. */
data class ParsedPayment(
    val amount: Double?,
    val merchant: String?,
    /** true = money left the account (popup); false = credit (no popup); null = unknown */
    val isDebit: Boolean?,
    val appLabel: String,
    val rawTitle: String,
    val rawText: String,
)

/** PRD §3.2 — per-app strategy; all strategies are driven by parsers.json regex lists. */
interface PaymentParser {
    val packageName: String
    val appLabel: String
    fun parse(title: String, text: String): ParsedPayment
}

private fun containsAny(haystack: String, needles: List<String>): Boolean {
    val lower = haystack.lowercase()
    return needles.any { it.lowercase() in lower }
}

private fun firstNamedGroup(pattern: String, text: String, group: String): String? {
    return try {
        val m = Regex(pattern, RegexOption.IGNORE_CASE).find(text) ?: return null
        m.groups[group]?.value?.trim()?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) { null }
}

private fun parseAmount(raw: String?): Double? {
    if (raw == null) return null
    val cleaned = raw.replace(",", "").replace(" ", "")
    return cleaned.toDoubleOrNull()
}

/** Generic config-backed implementation used for every whitelisted app. */
class ConfigParser(
    override val packageName: String,
    override val appLabel: String,
    private val debitKeywords: List<String>,
    private val creditKeywords: List<String>,
    private val amountPatterns: List<String>,
    private val payeePatterns: List<String>,
    private val genericAmountPatterns: List<String>,
    private val genericDebit: List<String>,
    private val genericCredit: List<String>,
) : PaymentParser {
    override fun parse(title: String, text: String): ParsedPayment {
        val combined = "$title $text"
        var amount: Double? = null
        for (p in amountPatterns) {
            amount = parseAmount(firstNamedGroup(p, combined, "amount"))
            if (amount != null) break
        }
        if (amount == null) {
            for (p in genericAmountPatterns) {
                amount = parseAmount(firstNamedGroup(p, combined, "amount"))
                if (amount != null) break
            }
        }
        var merchant: String? = null
        for (p in payeePatterns) {
            merchant = firstNamedGroup(p, combined, "payee")
            if (merchant != null) break
        }
        if (merchant.isNullOrBlank()) merchant = title.trim().takeIf { it.isNotEmpty() }

        val debitHit = containsAny(combined, debitKeywords.ifEmpty { genericDebit })
        val creditHit = containsAny(combined, creditKeywords.ifEmpty { genericCredit })
        // Credit wins on conflict (never popup for money-in). Both-absent => unknown (Review queue).
        val isDebit = when {
            creditHit -> false
            debitHit -> true
            else -> null
        }
        return ParsedPayment(amount, merchant, isDebit, appLabel, title, text)
    }
}

/**
 * Registry loading parsers.json from assets. Unknown/added-by-user apps get the
 * generic fallback parser (currency-pattern scan). Easily extensible without code changes.
 */
@Singleton
class ParserRegistry @Inject constructor(@ApplicationContext private val ctx: Context) {
    @Volatile private var parsers: Map<String, PaymentParser> = emptyMap()
    @Volatile private var genericAmount: List<String> = emptyList()
    @Volatile private var genericDebit: List<String> = emptyList()
    @Volatile private var genericCredit: List<String> = emptyList()
    @Volatile var dedupWindowSec: Long = 8
        private set

    @Synchronized
    fun load() {
        if (parsers.isNotEmpty()) return
        val json = ctx.assets.open("parsers.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        genericAmount = root.getJSONArray("genericAmountPatterns").toStringList()
        genericDebit = root.getJSONArray("genericDebitKeywords").toStringList()
        genericCredit = root.getJSONArray("genericCreditKeywords").toStringList()
        dedupWindowSec = root.optLong("dedupWindowSec", 8)
        val map = mutableMapOf<String, PaymentParser>()
        val apps = root.getJSONArray("apps")
        for (i in 0 until apps.length()) {
            val a = apps.getJSONObject(i)
            val pkg = a.getString("packageName")
            map[pkg] = ConfigParser(
                packageName = pkg,
                appLabel = a.optString("label", pkg),
                debitKeywords = a.optJSONArray("debitKeywords")?.toStringList() ?: emptyList(),
                creditKeywords = a.optJSONArray("creditKeywords")?.toStringList() ?: emptyList(),
                amountPatterns = a.optJSONArray("amountPatterns")?.toStringList() ?: emptyList(),
                payeePatterns = a.optJSONArray("payeePatterns")?.toStringList() ?: emptyList(),
                genericAmountPatterns = genericAmount,
                genericDebit = genericDebit,
                genericCredit = genericCredit,
            )
        }
        parsers = map
    }

    fun parserFor(pkg: String): PaymentParser {
        load()
        return parsers[pkg] ?: ConfigParser(
            packageName = pkg, appLabel = pkg.substringAfterLast('.'),
            debitKeywords = emptyList(), creditKeywords = emptyList(),
            amountPatterns = emptyList(), payeePatterns = emptyList(),
            genericAmountPatterns = genericAmount,
            genericDebit = genericDebit, genericCredit = genericCredit,
        )
    }

    private fun org.json.JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }
}

/** PRD §3.2 — debounce repeat posts (processing → success) per package+amount. */
@Singleton
class DedupFilter @Inject constructor() {
    private val lastSeen = mutableMapOf<String, Long>()
    @Synchronized
    fun isDuplicate(pkg: String, amount: Double?, windowMs: Long): Boolean {
        if (amount == null) return false
        val key = "$pkg|$amount"
        val now = System.currentTimeMillis()
        val prev = lastSeen[key]
        lastSeen[key] = now
        return prev != null && (now - prev) < windowMs
    }
}
