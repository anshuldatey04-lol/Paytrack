package com.paytrack.expensetracker.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(private val db: AppDatabase) {
    fun observeAll(): Flow<List<TransactionEntity>> = db.transactions().observeAll()
    fun observeReview(): Flow<List<TransactionEntity>> = db.transactions().observeReviewQueue()

    suspend fun save(tx: TransactionEntity): Long = db.transactions().insert(tx)
    suspend fun update(tx: TransactionEntity) = db.transactions().update(tx)
    suspend fun delete(tx: TransactionEntity) = db.transactions().delete(tx)
    suspend fun get(id: Long) = db.transactions().getById(id)
    suspend fun sumSince(since: Long) = db.transactions().sumSince(since)
    suspend fun sumSinceForCategory(since: Long, category: String) =
        db.transactions().sumSinceForCategory(since, category)
}

@Singleton
class BudgetRepository @Inject constructor(private val db: AppDatabase) {
    fun observeAll(): Flow<List<BudgetEntity>> = db.budgets().observeAll()
    suspend fun listAll() = db.budgets().listAll()
    suspend fun upsert(b: BudgetEntity): Long = db.budgets().upsert(b)
    suspend fun delete(b: BudgetEntity) = db.budgets().delete(b)
}

@Singleton
class CategoryRepository @Inject constructor(private val db: AppDatabase) {
    fun observeAll(): Flow<List<CategoryEntity>> = db.categories().observeAll()
    suspend fun listAll() = db.categories().listAll()
    suspend fun add(c: CategoryEntity) = db.categories().insert(c)
    suspend fun update(c: CategoryEntity) = db.categories().update(c)
    suspend fun delete(c: CategoryEntity) = db.categories().delete(c)

    /** Seed PRD §4 shortcut chips on first launch. */
    suspend fun seedDefaultsIfEmpty() {
        if (db.categories().listAll().isNotEmpty()) return
        listOf(
            "Food" to "🍔", "Travel" to "✈️", "Books" to "📚",
            "Entertainment" to "🎬", "Bills" to "🧾", "Shopping" to "🛍️",
            "Other" to "📦",
        ).forEach { (name, emoji) ->
            db.categories().insert(CategoryEntity(name = name, emoji = emoji, isDefault = true))
        }
    }
}
