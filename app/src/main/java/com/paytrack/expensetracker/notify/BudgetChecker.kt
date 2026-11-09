package com.paytrack.expensetracker.notify

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import com.paytrack.expensetracker.PayTrackApp
import com.paytrack.expensetracker.data.BudgetRepository
import com.paytrack.expensetracker.data.ExpenseRepository
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRD §6 + open question: alerts are BOTH push-style local notifications AND a
 * persistent dashboard indicator (Home screen reads the same [BudgetStatus]).
 */
data class BudgetStatus(
    val budgetId: Long,
    val period: String,
    val scope: String,
    val limit: Double,
    val spent: Double,
    val threshold: Int,
) {
    val pct: Int get() = if (limit <= 0) 0 else ((spent / limit) * 100).toInt()
    val breached: Boolean get() = spent >= limit
    val nearLimit: Boolean get() = !breached && pct >= threshold
}

@Singleton
class BudgetChecker @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val budgets: BudgetRepository,
    private val expenses: ExpenseRepository,
) {
    suspend fun evaluateAll(): List<BudgetStatus> {
        val out = mutableListOf<BudgetStatus>()
        for (b in budgets.listAll()) {
            val since = periodStart(b.period)
            val spent = if (b.scope == "overall") expenses.sumSince(since)
            else expenses.sumSinceForCategory(since, b.scope)
            out += BudgetStatus(b.id, b.period, b.scope, b.amount, spent, b.warningThresholdPercent)
        }
        return out
    }

    /** Called after every confirmed save — real-time alerts (PRD §6). */
    suspend fun checkAfterSave() {
        for (s in evaluateAll()) {
            when {
                s.breached -> post(
                    "budget-${s.budgetId}-over", "Budget exceeded",
                    "%s %s budget: ₹%.2f of ₹%.2f spent".format(label(s), s.period, s.spent, s.limit),
                )
                s.nearLimit -> post(
                    "budget-${s.budgetId}-near", "Near budget limit (${s.pct}%)",
                    "%s %s budget: ₹%.2f of ₹%.2f".format(label(s), s.period, s.spent, s.limit),
                )
            }
        }
    }

    private fun label(s: BudgetStatus) =
        if (s.scope == "overall") "Overall" else s.scope

    private fun post(tag: String, title: String, text: String) {
        val n = NotificationCompat.Builder(ctx, PayTrackApp.CHANNEL_BUDGET)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify("budget", tag.hashCode(), n)
        } catch (_: SecurityException) { }
    }

    companion object {
        fun periodStart(period: String): Long {
            val cal = Calendar.getInstance()
            when (period) {
                "weekly" -> {
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                }
                "monthly" -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                }
                "yearly" -> {
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                }
            }
            return cal.timeInMillis
        }
    }
}
