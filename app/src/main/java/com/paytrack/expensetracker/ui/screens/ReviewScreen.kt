package com.paytrack.expensetracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paytrack.expensetracker.data.TransactionEntity
import com.paytrack.expensetracker.ui.CategoryViewModel
import com.paytrack.expensetracker.ui.ReviewViewModel

/**
 * PRD §8.4 — unparsed / uncategorized notifications awaiting user action.
 * Nothing is silently dropped: dismissing the popup lands rows here.
 */
@Composable
fun ReviewScreen(vm: ReviewViewModel = hiltViewModel(), catVm: CategoryViewModel = hiltViewModel()) {
    val queue by vm.queue.collectAsState()
    val cats by catVm.all.collectAsState()
    var expandedId by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Review queue (${queue.size})", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(queue, key = { it.id }) { tx ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        TxRow(tx)
                        if (expandedId == tx.id) {
                            ResolveEditor(
                                tx = tx,
                                cats = cats.map { it.name }.ifEmpty { listOf("Other") },
                                onResolve = { a, c, n, m ->
                                    vm.resolve(tx, a, c, n, m)
                                    expandedId = null
                                },
                                onDiscard = { vm.discard(tx) },
                            )
                        } else {
                            Row {
                                OutlinedButton(onClick = { expandedId = tx.id }) { Text("Fix & confirm") }
                            }
                        }
                    }
                }
            }
            if (queue.isEmpty()) item { EmptyHint("All clear — every payment is categorized. 🎉") }
        }
    }
}

@Composable
private fun ResolveEditor(
    tx: TransactionEntity,
    cats: List<String>,
    onResolve: (Double, String, String, String) -> Unit,
    onDiscard: () -> Unit,
) {
    var amount by remember(tx.id) { mutableStateOf(if (tx.amount > 0) tx.amount.toString() else "") }
    var merchant by remember(tx.id) { mutableStateOf(tx.merchant.orEmpty()) }
    var note by remember(tx.id) { mutableStateOf(tx.note) }
    var category by remember(tx.id) { mutableStateOf(tx.category) }

    Column {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = merchant, onValueChange = { merchant = it }, label = { Text("Merchant") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = note, onValueChange = { note = it }, label = { Text("Note") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        CategoryChips(cats, category) { category = it }
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = {
                val a = amount.replace(",", "").toDoubleOrNull() ?: return@Button
                onResolve(a, category, note.trim(), merchant.trim())
            }) { Text("Confirm") }
            Spacer(Modifier.padding(4.dp))
            OutlinedButton(onClick = onDiscard) { Text("Delete") }
        }
    }
}
