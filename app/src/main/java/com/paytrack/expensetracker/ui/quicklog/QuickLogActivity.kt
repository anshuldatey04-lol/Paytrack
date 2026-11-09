package com.paytrack.expensetracker.ui.quicklog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.paytrack.expensetracker.data.CategoryRepository
import com.paytrack.expensetracker.data.ExpenseRepository
import com.paytrack.expensetracker.notify.BudgetChecker
import com.paytrack.expensetracker.notify.QuickLogNotifier
import javax.inject.Inject

/**
 * PRD §4 — bottom-sheet popup over whatever app the user is in.
 * Auto-dismisses after 25s untouched; the pre-saved row stays as needs_review.
 */
@AndroidEntryPoint
class QuickLogActivity : ComponentActivity() {

    @Inject lateinit var repo: ExpenseRepository
    @Inject lateinit var categories: CategoryRepository
    @Inject lateinit var budgets: BudgetChecker
    @Inject lateinit var notifier: QuickLogNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val txId = intent.getLongExtra(EXTRA_TX_ID, -1)
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val merchant = intent.getStringExtra(EXTRA_MERCHANT).orEmpty()
        val app = intent.getStringExtra(EXTRA_APP).orEmpty()
        if (txId == -1L) {
            finish()
            return
        }
        setFinishOnTouchOutside(true)
        setContent {
            MaterialTheme {
                QuickLogSheet(
                    txId = txId,
                    initialAmount = amount,
                    initialMerchant = merchant,
                    appLabel = app,
                    onDone = { finish() },
                    onSave = { a, m, note, cat -> save(txId, a, m, note, cat) },
                    loadCategories = suspend { categories.listAll().map { it.name } },
                )
            }
        }
    }

    private fun save(txId: Long, amount: Double, merchant: String, note: String, category: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val existing = repo.get(txId) ?: return@launch
                repo.update(
                    existing.copy(
                        amount = amount,
                        merchant = merchant.ifBlank { null },
                        note = note,
                        category = category,
                        status = "confirmed",
                    ),
                )
                notifier.cancel(txId)
                budgets.checkAfterSave()
            } catch (_: Exception) { }
        }
    }

    override fun onPause() {
        super.onPause()
        // Dismiss/skip path: leave the row as needs_review (PRD §4) rather than deleting.
        if (isFinishing) {
            val txId = intent.getLongExtra(EXTRA_TX_ID, -1)
            if (txId != -1L) notifier.cancel(txId)
        }
    }

    companion object {
        const val EXTRA_TX_ID = "tx_id"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_MERCHANT = "merchant"
        const val EXTRA_APP = "app"
        /** PRD §4: 20–30s timeout. */
        const val TIMEOUT_SEC = 25
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickLogSheet(
    txId: Long,
    initialAmount: Double,
    initialMerchant: String,
    appLabel: String,
    onDone: () -> Unit,
    onSave: (Double, String, String, String) -> Unit,
    loadCategories: suspend () -> List<String>,
) {
    var amountText by remember { mutableStateOf(if (initialAmount > 0) "%.2f".format(initialAmount) else "") }
    var merchant by remember { mutableStateOf(initialMerchant) }
    var note by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("Other") }
    var cats by remember { mutableStateOf(listOf("Food", "Travel", "Books", "Entertainment", "Bills", "Shopping", "Other")) }
    var secondsLeft by remember { mutableStateOf(QuickLogActivity.TIMEOUT_SEC) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(txId) {
        try { cats = loadCategories().ifEmpty { cats } } catch (_: Exception) { }
    }
    // Auto-dismiss countdown; untouched → saved as uncategorized (stays needs_review).
    LaunchedEffect(txId) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        onDone()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Payment detected • $appLabel", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / payee") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (purpose)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.forEach { c ->
                        FilterChip(
                            selected = selected == c,
                            onClick = { selected = c },
                            label = { Text(c) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) {
                        Text("Skip ($secondsLeft s)")
                    }
                    Button(
                        onClick = {
                            val a = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
                            scope.launch {
                                onSave(a, merchant.trim(), note.trim(), selected)
                                onDone()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
