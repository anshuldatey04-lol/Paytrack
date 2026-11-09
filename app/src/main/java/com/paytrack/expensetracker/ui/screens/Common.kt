package com.paytrack.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paytrack.expensetracker.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun inr(v: Double): String = "₹%.2f".format(v)

fun fmtTime(ts: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(ts))

@Composable
fun TxRow(tx: TransactionEntity, onClick: (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = onClick ?: {}) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.merchant?.takeIf { it.isNotBlank() } ?: tx.category,
                    fontWeight = FontWeight.SemiBold,
                )
                if (tx.note.isNotBlank()) Text(tx.note, style = MaterialTheme.typography.bodySmall)
                Text(
                    "${if (tx.source == "cash") "💵 Cash" else "📱 ${tx.sourceApp ?: "Digital"}"} • ${fmtTime(tx.timestamp)}" +
                        if (tx.status == "needs_review") " • ⚠ needs review" else "",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(inr(tx.amount), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(cats: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        cats.forEach { c ->
            FilterChip(selected = selected == c, onClick = { onSelect(c) }, label = { Text(c) })
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
