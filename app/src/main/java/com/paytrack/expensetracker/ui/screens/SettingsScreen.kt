package com.paytrack.expensetracker.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.paytrack.expensetracker.ui.SettingsViewModel
import com.paytrack.expensetracker.ui.TxViewModel
import com.paytrack.expensetracker.util.CsvExport
import com.paytrack.expensetracker.util.ListenerAccess

data class AppEntry(val pkg: String, val label: String)

/**
 * PRD §8.8 — whitelisted payment apps (user-addable), access status, thresholds note,
 * cash-on-hand top-up, CSV export. Overlay permission deliberately NOT requested:
 * the full-screen-intent popup path needs no overlay grant (see QuickLogNotifier).
 */
@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel(),
    txVm: TxViewModel = hiltViewModel(),
    onManageCategories: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val apps by vm.apps.collectAsState()
    val txs by txVm.transactions.collectAsState()
    var installed by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var cashText by remember { mutableStateOf("") }
    var notifTick by remember { mutableStateOf(0) }
    val postLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notifTick++ }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val list = try {
                // Launcher-only query (~60-100 rows) instead of getInstalledApplications (~300+ incl. system).
                val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(main, 0)
                    .mapNotNull {
                        try {
                            val pkg = it.activityInfo.packageName
                            AppEntry(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString())
                        } catch (_: Exception) { null }
                    }
                    .distinctBy { it.pkg }
                    .sortedBy { it.label.lowercase() }
            } catch (_: Exception) { emptyList() }
            installed = list
        }
    }

    // Refresh permission statuses when returning to this screen.

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Text("Permissions", style = MaterialTheme.typography.titleMedium)
        // notifTick forces recomposition after the runtime permission dialog returns.
        notifTick.let {
            StatusRow(
                label = "Notification access",
                ok = ListenerAccess.isNotificationAccessEnabled(ctx),
                onFix = { ctx.startActivity(ListenerAccess.notificationAccessIntent()) },
            )
            StatusRow(
                label = "App notifications (budget alerts)",
                ok = ListenerAccess.hasPostPermission(ctx),
                onFix = {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        !ListenerAccess.hasPostPermission(ctx)
                    ) {
                        try {
                            postLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } catch (_: Exception) {
                            ctx.startActivity(
                                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName),
                            )
                        }
                    } else {
                        ctx.startActivity(
                            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName),
                        )
                    }
                },
            )
            StatusRow(
                label = "Ignore battery optimizations",
                ok = ListenerAccess.isBatteryExempt(ctx),
                onFix = { ListenerAccess.openBatteryExemption(ctx) },
            )
        }
        Text(
            "On Xiaomi / Oppo / Vivo / OnePlus: also enable Autostart and lock PayTrack in Recents, " +
                "or the listener may be killed.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))

        Text("Tracked payment apps (${apps.size})", style = MaterialTheme.typography.titleMedium)
        Text("Only these apps are scanned. Tick to add/remove.", style = MaterialTheme.typography.bodySmall)
        // Merge whitelisted pkgs missing from launcher query (e.g. background-only), selected first.
        // Cap at 250 rows so the inner list never janks inside the outer scroll.
        val merged = remember(installed, apps) {
            val known = installed.map { it.pkg }.toSet()
            installed + apps.filter { it !in known }.map { AppEntry(it, it) }
        }
        val selectedFirst = merged.sortedBy { it.pkg !in apps }.take(250)
        LazyColumn(modifier = Modifier.height(280.dp)) {
            items(selectedFirst, key = { it.pkg }) { app ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = app.pkg in apps,
                        onCheckedChange = { checked ->
                            vm.setApps(if (checked) apps + app.pkg else apps - app.pkg)
                        },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyMedium)
                        Text(app.pkg, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("Categories", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onManageCategories, modifier = Modifier.fillMaxWidth()) {
            Text("Manage categories")
        }
        Spacer(Modifier.height(16.dp))

        Text("Cash on hand", style = MaterialTheme.typography.titleMedium)
        Row {
            OutlinedTextField(
                value = cashText, onValueChange = { cashText = it }, label = { Text("Balance (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f), singleLine = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val v = cashText.replace(",", "").toDoubleOrNull() ?: return@Button
            vm.setCash(v)
            cashText = ""
        }) { Text("Set cash balance") }
        Spacer(Modifier.height(16.dp))

        Text("Backup", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = {
            scope.launch(Dispatchers.IO) {
                try {
                    val uri = CsvExport.export(ctx, txs)
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(Intent.createChooser(share, "Export CSV").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) { }
            }
        }) { Text("Export transactions (CSV)") }
        Spacer(Modifier.height(8.dp))
        Text(
            "v1 is fully offline — data never leaves this device except when you export it yourself.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, onFix: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text((if (ok) "✅ " else "❌ ") + label, modifier = Modifier.weight(1f))
        if (!ok) OutlinedButton(onClick = onFix) { Text("Fix") }
    }
}
