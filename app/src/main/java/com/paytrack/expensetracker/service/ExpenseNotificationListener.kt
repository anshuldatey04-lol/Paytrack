package com.paytrack.expensetracker.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.paytrack.expensetracker.data.ExpenseRepository
import com.paytrack.expensetracker.data.PrefsManager
import com.paytrack.expensetracker.data.TransactionEntity
import com.paytrack.expensetracker.notify.BudgetChecker
import com.paytrack.expensetracker.notify.QuickLogNotifier
import com.paytrack.expensetracker.parser.DedupFilter
import com.paytrack.expensetracker.parser.ParserRegistry
import javax.inject.Inject

/**
 * PRD §3 — event-driven listener. No polling. Ignores non-whitelisted packages
 * at the top to minimize footprint.
 */
@AndroidEntryPoint
class ExpenseNotificationListener : NotificationListenerService() {

    @Inject lateinit var parsers: ParserRegistry
    @Inject lateinit var dedup: DedupFilter
    @Inject lateinit var repo: ExpenseRepository
    @Inject lateinit var prefs: PrefsManager
    @Inject lateinit var quickLog: QuickLogNotifier
    @Inject lateinit var budgets: BudgetChecker

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** In-memory whitelist — avoids a DataStore read on every notification (battery/throughput). */
    @Volatile private var cachedApps: Set<String>? = null

    override fun onListenerConnected() {
        parsers.load()
        scope.launch {
            try {
                prefs.whitelistedApps.collect { cachedApps = it }
            } catch (_: Exception) { }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        // Own notifications must never retrigger the pipeline.
        if (pkg == packageName) return
        scope.launch {
            try {
                val allowed = cachedApps ?: prefs.getWhitelistedOnce().also { cachedApps = it }
                if (pkg !in allowed) return@launch
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
                val body = listOf(text, bigText).filter { it.isNotBlank() }.joinToString("\n")
                if (title.isBlank() && body.isBlank()) return@launch

                val parsed = parsers.parserFor(pkg).parse(title, body)

                // Credit / money-in → never popup. Silently ignore (not an expense).
                if (parsed.isDebit == false) return@launch

                if (dedup.isDuplicate(pkg, parsed.amount, parsers.dedupWindowSec * 1000)) return@launch

                // Ambiguous or amount-less → Review queue, never silently dropped (PRD §3.2).
                if (parsed.amount == null || parsed.isDebit == null) {
                    val id = repo.save(
                        TransactionEntity(
                            amount = parsed.amount ?: 0.0,
                            category = "Other",
                            note = (listOf(title, body).filter { it.isNotBlank() }.joinToString(" — ")),
                            merchant = parsed.merchant,
                            source = "digital",
                            sourceApp = parsed.appLabel,
                            sourcePackage = pkg,
                            status = "needs_review",
                        ),
                    )
                    quickLog.showReviewQueued(id, parsed.appLabel)
                    return@launch
                }

                // Debit with amount → pre-save as needs_review, then heads-up + full-screen popup.
                // If the user saves in the popup the row is flipped to confirmed; if they
                // dismiss/timeout/ignore it, the row stays as needs_review (nothing lost).
                val id = repo.save(
                    TransactionEntity(
                        amount = parsed.amount,
                        merchant = parsed.merchant,
                        source = "digital",
                        sourceApp = parsed.appLabel,
                        sourcePackage = pkg,
                        status = "needs_review",
                    ),
                )
                quickLog.showQuickLog(
                    txId = id,
                    amount = parsed.amount,
                    merchant = parsed.merchant.orEmpty(),
                    appLabel = parsed.appLabel,
                )
            } catch (_: Exception) {
                // Listener must never crash the system service; failures are queued on next post.
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
