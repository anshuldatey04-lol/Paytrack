package com.paytrack.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity): Long

    @Update
    suspend fun update(tx: TransactionEntity)

    @Delete
    suspend fun delete(tx: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'needs_review' ORDER BY timestamp DESC")
    fun observeReviewQueue(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE timestamp >= :since AND status = 'confirmed'")
    suspend fun sumSince(since: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE timestamp >= :since AND category = :category AND status = 'confirmed'")
    suspend fun sumSinceForCategory(since: Long, category: String): Double

    @Query("SELECT * FROM transactions WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun listSince(since: Long): List<TransactionEntity>
}

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(b: BudgetEntity): Long

    @Update
    suspend fun update(b: BudgetEntity)

    @Delete
    suspend fun delete(b: BudgetEntity)

    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun listAll(): List<BudgetEntity>
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(c: CategoryEntity): Long

    @Update
    suspend fun update(c: CategoryEntity)

    @Delete
    suspend fun delete(c: CategoryEntity)

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    suspend fun listAll(): List<CategoryEntity>
}
