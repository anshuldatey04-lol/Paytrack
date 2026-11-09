package com.paytrack.expensetracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.paytrack.expensetracker.worker.BudgetCheckWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PayTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createChannels()
        // Periodic safety net must exist on fresh installs too, not just after reboot.
        try {
            BudgetCheckWorker.schedule(this)
        } catch (_: Exception) { }
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_QUICKLOG, "Payment detected", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Heads-up alert when a UPI payment is detected" },
            NotificationChannel(
                CHANNEL_BUDGET, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Near-limit and exceeded budget warnings" },
        )
        channels.forEach(nm::createNotificationChannel)
    }

    companion object {
        const val CHANNEL_QUICKLOG = "quicklog"
        const val CHANNEL_BUDGET = "budget"
    }
}
