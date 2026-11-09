package com.paytrack.expensetracker.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import com.paytrack.expensetracker.MainActivity
import com.paytrack.expensetracker.PayTrackApp
import com.paytrack.expensetracker.R
import com.paytrack.expensetracker.ui.quicklog.QuickLogActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRD §4 + §12 open question resolution:
 * Primary popup = heads-up notification with FULL-SCREEN INTENT launching the
 * transparent QuickLogActivity. This works on Android 10–16 without the sensitive
 * SYSTEM_ALERT_WINDOW overlay permission and survives most OEM background limits,
 * because the tap/full-screen path is an OS-blessed background-activity-start.
 * Overlay (TYPE_APPLICATION_OVERLAY) was evaluated and rejected as default: it needs
 * a manual grant, is blocked on lockscreen by several OEMs, and is increasingly
 * restricted — but remains available as an opted-in fallback in Settings docs.
 */
@Singleton
class QuickLogNotifier @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun showQuickLog(txId: Long, amount: Double, merchant: String, appLabel: String) {
        val fullIntent = Intent(ctx, QuickLogActivity::class.java).apply {
            putExtra(QuickLogActivity.EXTRA_TX_ID, txId)
            putExtra(QuickLogActivity.EXTRA_AMOUNT, amount)
            putExtra(QuickLogActivity.EXTRA_MERCHANT, merchant)
            putExtra(QuickLogActivity.EXTRA_APP, appLabel)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullPi = PendingIntent.getActivity(
            ctx, txId.toInt(), fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val label = if (merchant.isBlank()) appLabel else "$appLabel • $merchant"
        val notif = NotificationCompat.Builder(ctx, PayTrackApp.CHANNEL_QUICKLOG)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("₹%.2f paid via %s".format(amount, appLabel))
            .setContentText("Tap to categorize • $label")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(fullPi)
            .setFullScreenIntent(fullPi, true)
            .addAction(
                android.R.drawable.ic_menu_save, "Log",
                fullPi,
            )
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(QUICKLOG_TAG, txId.toInt(), notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied — the Review queue inside the app still holds the row.
        }
    }

    fun showReviewQueued(txId: Long, appLabel: String) {
        val open = PendingIntent.getActivity(
            ctx, 900_000 + txId.toInt(),
            Intent(ctx, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TAB, "review")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(ctx, PayTrackApp.CHANNEL_QUICKLOG)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Payment needs review ($appLabel)")
            .setContentText("Couldn't parse the amount — tap to fix it.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(QUICKLOG_TAG, 800_000 + txId.toInt(), notif)
        } catch (_: SecurityException) { }
    }

    fun cancel(txId: Long) {
        NotificationManagerCompat.from(ctx).cancel(QUICKLOG_TAG, txId.toInt())
    }

    companion object {
        const val QUICKLOG_TAG = "quicklog"
    }
}
