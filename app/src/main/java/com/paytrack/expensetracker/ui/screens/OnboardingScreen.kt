package com.paytrack.expensetracker.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.paytrack.expensetracker.util.ListenerAccess

/**
 * PRD §8.1 — explains the sensitive notification-access permission, deep-links
 * to system settings (it cannot be a runtime dialog).
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(ListenerAccess.isNotificationAccessEnabled(ctx)) }
    var postGranted by remember { mutableStateOf(ListenerAccess.hasPostPermission(ctx)) }
    var batteryExempt by remember { mutableStateOf(ListenerAccess.isBatteryExempt(ctx)) }
    val postLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { postGranted = ListenerAccess.hasPostPermission(ctx) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Welcome to PayTrack 💸", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "PayTrack reads payment notifications (GPay, PhonePe, Paytm, FamPay) " +
                "on this device only — nothing leaves your phone — to detect what you paid " +
                "and pop up a one-tap expense logger.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "1. Tap below to open system settings.\n" +
                "2. Find PayTrack and turn notification access ON.\n" +
                "3. Come back here and continue.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            ctx.startActivity(ListenerAccess.notificationAccessIntent())
        }) {
            Text(if (granted) "Re-open notification settings" else "Grant notification access")
        }
        Spacer(Modifier.height(8.dp))
        // Android 13+: QuickLog heads-up + budget alerts need POST_NOTIFICATIONS at runtime.
        if (Build.VERSION.SDK_INT >= 33 && !postGranted) {
            Button(onClick = {
                try {
                    postLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (_: Exception) { }
            }) {
                Text("Allow app notifications")
            }
            Spacer(Modifier.height(8.dp))
        }
        if (!batteryExempt) {
            OutlinedButton(onClick = {
                ListenerAccess.openBatteryExemption(ctx)
                batteryExempt = ListenerAccess.isBatteryExempt(ctx)
            }) {
                Text("Allow background running")
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = {
            granted = ListenerAccess.isNotificationAccessEnabled(ctx)
            postGranted = ListenerAccess.hasPostPermission(ctx)
            batteryExempt = ListenerAccess.isBatteryExempt(ctx)
            if (granted) onDone()
        }) {
            Text("I've enabled it — continue")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDone) {
            Text("Skip for now (manual cash logging only)")
        }
    }
}
