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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paytrack.expensetracker.notify.BudgetChecker
import com.paytrack.expensetracker.ui.HomeViewModel

@Composable
fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    onAddCash: () -> Unit,
    onSeeAll: () -> Unit,
    onReview: () -> Unit,
) {
    val period by vm.period.collectAsState()
    val txs by vm.transactions.collectAsState()
    val statuses by vm.statuses.collectAsState()
    val cash by vm.cashOnHand.collectAsState()

    val since = BudgetChecker.periodStart(period)
    val spent = txs.filter { it.timestamp >= since && it.status == "confirmed" }.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("weekly" to "Week", "monthly" to "Month", "yearly" to "Year").forEach { (p, label) ->
                FilterChip(selected = period == p, onClick = { vm.period.value = p }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Spent this $period", style = MaterialTheme.typography.labelLarge)
                Text(inr(spent), style = MaterialTheme.typography.headlineLarge)
                Text("Cash on hand: ${inr(cash)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Persistent dashboard indicators (open-question: alerts are BOTH notifications + here).
        statuses.filter { it.spent > 0 && (it.nearLimit || it.breached) }.forEach { s ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (s.breached) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        if (s.breached) "⛔ ${s.scope} ${s.period} budget exceeded"
                        else "⚠️ ${s.scope} ${s.period} at ${s.pct}%",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    LinearProgressIndicator(
                        progress = { (s.spent / s.limit).toFloat().coerceAtMost(1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                    Text("${inr(s.spent)} of ${inr(s.limit)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddCash, modifier = Modifier.weight(1f)) { Text("+ Cash expense") }
            OutlinedButton(onClick = onReview, modifier = Modifier.weight(1f)) { Text("Review queue") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Recent", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(txs.take(15)) { TxRow(it) }
            if (txs.isEmpty()) item { EmptyHint("No expenses yet. Make a UPI payment or add a cash expense.") }
        }
        OutlinedButton(onClick = onSeeAll, modifier = Modifier.fillMaxWidth()) { Text("All transactions") }
    }
}
