package com.paytrack.expensetracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.paytrack.expensetracker.ui.BudgetViewModel
import com.paytrack.expensetracker.ui.CategoryViewModel

/** PRD §6/§8.6 — overall + per-category budgets at weekly/monthly/yearly cadence. */
@Composable
fun BudgetsScreen(vm: BudgetViewModel = hiltViewModel(), catVm: CategoryViewModel = hiltViewModel()) {
    val all by vm.all.collectAsState()
    val cats by catVm.all.collectAsState()
    var period by remember { mutableStateOf("monthly") }
    var scope by remember { mutableStateOf("overall") }
    var amount by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf("80") }
    var scopeMenu by remember { mutableStateOf(false) }

    val scopes = listOf("overall") + cats.map { it.name }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Budgets", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row {
            listOf("weekly", "monthly", "yearly").forEach { p ->
                androidx.compose.material3.FilterChip(
                    selected = period == p, onClick = { period = p }, label = { Text(p) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = { scopeMenu = true }) { Text("Scope: $scope ▾") }
            DropdownMenu(expanded = scopeMenu, onDismissRequest = { scopeMenu = false }) {
                scopes.forEach { s ->
                    DropdownMenuItem(text = { Text(s) }, onClick = { scope = s; scopeMenu = false })
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = amount, onValueChange = { amount = it }, label = { Text("Limit (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f), singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = threshold, onValueChange = { threshold = it }, label = { Text("Warn %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.6f), singleLine = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val a = amount.replace(",", "").toDoubleOrNull() ?: return@Button
                val t = threshold.toIntOrNull()?.coerceIn(1, 99) ?: 80
                if (a <= 0) return@Button
                vm.upsert(period, scope, a, t)
                amount = ""
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save budget") }
        Spacer(Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(all, key = { it.id }) { b ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${b.scope} • ${b.period}", style = MaterialTheme.typography.titleSmall)
                            Text("${inr(b.amount)} • warn at ${b.warningThresholdPercent}%", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { vm.delete(b) }) { Text("Delete") }
                    }
                }
            }
            if (all.isEmpty()) item { EmptyHint("No budgets yet — set one above to get near-limit alerts.") }
        }
    }
}

@Composable
fun CategoriesScreen(vm: CategoryViewModel = hiltViewModel()) {
    val all by vm.all.collectAsState()
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📦") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Categories", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = emoji, onValueChange = { emoji = it }, label = { Text("Emoji") },
                modifier = Modifier.weight(0.5f), singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("New category") },
                modifier = Modifier.weight(1f), singleLine = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (name.isBlank()) return@Button
                vm.add(name.trim(), emoji.ifBlank { "📦" })
                name = ""
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Add category") }
        Spacer(Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(all, key = { it.id }) { c ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Text("${c.emoji}  ${c.name}", modifier = Modifier.weight(1f))
                        if (c.isCustom) OutlinedButton(onClick = { vm.delete(c) }) { Text("Delete") }
                        else Text("default", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
