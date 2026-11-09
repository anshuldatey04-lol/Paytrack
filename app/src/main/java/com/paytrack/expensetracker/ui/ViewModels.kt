package com.paytrack.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.paytrack.expensetracker.data.BudgetEntity
import com.paytrack.expensetracker.data.BudgetRepository
import com.paytrack.expensetracker.data.CategoryEntity
import com.paytrack.expensetracker.data.CategoryRepository
import com.paytrack.expensetracker.data.ExpenseRepository
import com.paytrack.expensetracker.data.PrefsManager
import com.paytrack.expensetracker.data.TransactionEntity
import com.paytrack.expensetracker.notify.BudgetChecker
import com.paytrack.expensetracker.notify.BudgetStatus
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val expenses: ExpenseRepository,
    private val budgets: BudgetRepository,
    private val checker: BudgetChecker,
    prefs: PrefsManager,
) : ViewModel() {
    val period = MutableStateFlow("monthly")
    val transactions: StateFlow<List<TransactionEntity>> =
        expenses.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val statuses: StateFlow<List<BudgetStatus>> =
        combine(budgets.observeAll(), period) { _, _ ->
            // Room SUM queries must not run on Main — Home jank otherwise.
            withContext(Dispatchers.IO) { checker.evaluateAll() }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cashOnHand: StateFlow<Double> =
        prefs.cashOnHand.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}

@HiltViewModel
class TxViewModel @Inject constructor(
    private val expenses: ExpenseRepository,
    private val prefs: PrefsManager,
    private val checker: BudgetChecker,
) : ViewModel() {
    val transactions: StateFlow<List<TransactionEntity>> =
        expenses.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveCash(amount: Double, category: String, note: String, merchant: String) {
        viewModelScope.launch {
            expenses.save(
                TransactionEntity(
                    amount = amount, category = category, note = note,
                    merchant = merchant.ifBlank { null }, source = "cash",
                    status = "confirmed",
                ),
            )
            prefs.setCashOnHand(prefs.cashOnHand.first() - amount)
            checker.checkAfterSave()
        }
    }

    fun confirm(tx: TransactionEntity) {
        viewModelScope.launch {
            expenses.update(tx.copy(status = "confirmed"))
            checker.checkAfterSave()
        }
    }

    fun update(tx: TransactionEntity) {
        viewModelScope.launch {
            expenses.update(tx)
            checker.checkAfterSave()
        }
    }

    fun delete(tx: TransactionEntity) {
        viewModelScope.launch { expenses.delete(tx) }
    }
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val expenses: ExpenseRepository,
    private val checker: BudgetChecker,
) : ViewModel() {
    val queue: StateFlow<List<TransactionEntity>> =
        expenses.observeReview().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolve(tx: TransactionEntity, amount: Double, category: String, note: String, merchant: String) {
        viewModelScope.launch {
            expenses.update(
                tx.copy(
                    amount = amount, category = category, note = note,
                    merchant = merchant.ifBlank { null }, status = "confirmed",
                ),
            )
            checker.checkAfterSave()
        }
    }

    fun discard(tx: TransactionEntity) {
        viewModelScope.launch { expenses.delete(tx) }
    }
}

@HiltViewModel
class BudgetViewModel @Inject constructor(private val budgets: BudgetRepository) : ViewModel() {
    val all: StateFlow<List<BudgetEntity>> =
        budgets.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun upsert(period: String, scope: String, amount: Double, threshold: Int) {
        viewModelScope.launch {
            val existing = budgets.listAll().firstOrNull { it.period == period && it.scope == scope }
            if (existing != null) budgets.upsert(existing.copy(amount = amount, warningThresholdPercent = threshold))
            else budgets.upsert(BudgetEntity(period = period, scope = scope, amount = amount, warningThresholdPercent = threshold))
        }
    }

    fun delete(b: BudgetEntity) {
        viewModelScope.launch { budgets.delete(b) }
    }
}

@HiltViewModel
class CategoryViewModel @Inject constructor(private val cats: CategoryRepository) : ViewModel() {
    val all: StateFlow<List<CategoryEntity>> =
        cats.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { cats.seedDefaultsIfEmpty() }
    }

    fun add(name: String, emoji: String) {
        viewModelScope.launch { cats.add(CategoryEntity(name = name, emoji = emoji, isCustom = true)) }
    }

    fun delete(c: CategoryEntity) {
        viewModelScope.launch { if (!c.isDefault) cats.delete(c) }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PrefsManager,
) : ViewModel() {
    val apps: StateFlow<Set<String>> =
        prefs.whitelistedApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setApps(apps: Set<String>) {
        viewModelScope.launch { prefs.setWhitelisted(apps) }
    }

    fun setCash(v: Double) {
        viewModelScope.launch { prefs.setCashOnHand(v) }
    }
}
