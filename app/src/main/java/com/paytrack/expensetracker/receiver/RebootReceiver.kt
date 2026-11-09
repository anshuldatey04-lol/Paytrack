package com.paytrack.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.paytrack.expensetracker.worker.BudgetCheckWorker

/** PRD §13 — persist across reboots: reschedule periodic budget sweeps (listener itself is re-bound by the OS). */
class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
            )
        ) return
        try {
            BudgetCheckWorker.schedule(context)
        } catch (_: Exception) { }
    }
}
