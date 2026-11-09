package com.paytrack.expensetracker.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.paytrack.expensetracker.data.TransactionEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExport {
    /** PRD §10 — CSV export for backup/portability. Streams to disk (no OOM on large DBs). */
    fun export(ctx: Context, txs: List<TransactionEntity>): Uri {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        // Prune old exports so cacheDir doesn't grow unbounded.
        try {
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach {
                try { it.delete() } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
        val file = File(dir, "paytrack-export-${System.currentTimeMillis()}.csv")
        file.bufferedWriter().use { w ->
            w.append("id,amount,category,note,merchant,source,sourceApp,timestamp,status\n")
            for (t in txs) {
                w.append(
                    listOf(
                        t.id.toString(), t.amount.toString(), csv(t.category), csv(t.note),
                        csv(t.merchant.orEmpty()), t.source, csv(t.sourceApp.orEmpty()),
                        sdf.format(Date(t.timestamp)), t.status,
                    ).joinToString(","),
                ).append('\n')
            }
        }
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    private fun csv(s: String): String {
        val needsQuotes = s.any { it == ',' || it == '"' || it == '\n' }
        return if (needsQuotes) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }
}
