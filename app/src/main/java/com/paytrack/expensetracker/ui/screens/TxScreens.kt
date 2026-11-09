package com.paytrack.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.paytrack.expensetracker.ui.CategoryViewModel
import com.paytrack.expensetracker.ui.TxViewModel

@Composable
fun TransactionsScreen(vm: TxViewModel = hiltViewModel(), catVm: CategoryViewModel = hiltViewModel()) {
    val txs by vm.transactions.collectAsState()
    var sourceFilter by remember { mutableStateOf("all") } // all | cash | digital
    var catFilter by remember { mutableStateOf("All") }
    val cats by catVm.all.collectAsState()
    val catNames = listOf("All") + cats.map { it.name }

    val shown = txs.filter {
        (sourceFilter == "all" || it.source == sourceFilter) &&
            (catFilter == "All" || it.category == catFilter)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "All", "cash" to "Cash", "digital" to "Digital").forEach { (v, l) ->
                FilterChip(selected = sourceFilter == v, onClick = { sourceFilter = v }, label = { Text(l) })
            }
        }
        Spacer(Modifier.height(4.dp))
        CategoryChips(catNames.take(9), catFilter) { catFilter = it }
        Spacer(Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(shown, key = { it.id }) { tx ->
                var editing by remember(tx.id) { mutableStateOf(false) }
                TxRow(tx, onClick = { editing = true })
                if (editing) {
                    TxEditDialog(
                        tx = tx,
                        cats = cats.map { it.name }.ifEmpty { listOf("Other") },
                        onSave = { updated -> vm.update(updated); editing = false },
                        onDelete = { vm.delete(tx); editing = false },
                        onClose = { editing = false },
                    )
                }
            }
            if (shown.isEmpty()) item { EmptyHint("No transactions match these filters.") }
        }
    }
}

@Composable
fun AddCashScreen(vm: TxViewModel = hiltViewModel(), catVm: CategoryViewModel = hiltViewModel(), onSaved: () -> Unit) {
    val cats by catVm.all.collectAsState()
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add cash expense", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = merchant, onValueChange = { merchant = it }, label = { Text("Merchant / place (optional)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = note, onValueChange = { note = it }, label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        CategoryChips(cats.map { it.name }.ifEmpty { listOf("Other") }, category) { category = it }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val a = amount.replace(",", "").toDoubleOrNull() ?: return@Button
                if (a <= 0) return@Button
                vm.saveCash(a, category, note.trim(), merchant.trim())
                onSaved()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save cash expense")
        }
    }
}

/** PRD §8.3 — edit/delete for every transaction. */
@Composable
fun TxEditDialog(
    tx: com.paytrack.expensetracker.data.TransactionEntity,
    cats: List<String>,
    onSave: (com.paytrack.expensetracker.data.TransactionEntity) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var amount by remember(tx.id) { mutableStateOf(tx.amount.toString()) }
    var merchant by remember(tx.id) { mutableStateOf(tx.merchant.orEmpty()) }
    var note by remember(tx.id) { mutableStateOf(tx.note) }
    var category by remember(tx.id) { mutableStateOf(tx.category) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Edit transaction") },
        text = {
            Column {
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
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = amount.replace(",", "").toDoubleOrNull() ?: return@Button
                onSave(tx.copy(amount = a, merchant = merchant.trim().ifBlank { null }, note = note.trim(), category = category))
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (confirmDelete) {
                    Button(onClick = onDelete) { Text("Confirm delete") }
                } else {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                }
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        },
    )
}
