package com.paytrack.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** PRD §7 — status: confirmed vs needs_review. Dismissed/timeout popups persist as needs_review. */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String = "Other",
    val note: String = "",
    val merchant: String? = null,
    /** "digital" | "cash" */
    val source: String = "digital",
    /** e.g. "GPay" — null for cash */
    val sourceApp: String? = null,
    val sourcePackage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    /** "confirmed" | "needs_review" */
    val status: String = "confirmed",
)

/** PRD §7 — period weekly/monthly/yearly; scope "overall" or a category name. */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: String,
    val scope: String = "overall",
    val amount: Double,
    val warningThresholdPercent: Int = 80,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "📦",
    val isDefault: Boolean = false,
    val isCustom: Boolean = false,
)
