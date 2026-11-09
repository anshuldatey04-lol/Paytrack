package com.paytrack.expensetracker.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object ListenerAccess {
    /** Special-access: cannot use runtime dialog — deep-link to system settings (PRD §3.1). */
    fun isNotificationAccessEnabled(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun notificationAccessIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun overlayIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun batteryIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun isBatteryExempt(ctx: Context): Boolean {
        val pm = ctx.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    /** Android 13+ runtime permission — required for QuickLog + budget heads-up notifs. */
    fun hasPostPermission(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return areNotificationsEnabled(ctx)
        return ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Direct system dialog: "Allow PayTrack to always run in background?" Falls back to list on OEMs. */
    fun batteryRequestIntent(ctx: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${ctx.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Tries the direct request first; returns false only if nothing could be launched. */
    fun openBatteryExemption(ctx: Context): Boolean {
        if (isBatteryExempt(ctx)) return true
        return try {
            ctx.startActivity(batteryRequestIntent(ctx))
            true
        } catch (_: Exception) {
            try {
                ctx.startActivity(batteryIntent(ctx))
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun areNotificationsEnabled(ctx: Context): Boolean =
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()
}
