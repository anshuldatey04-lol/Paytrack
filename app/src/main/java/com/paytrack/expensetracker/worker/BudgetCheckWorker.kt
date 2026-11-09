package com.paytrack.expensetracker.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.paytrack.expensetracker.notify.BudgetChecker

/** Periodic safety net so budget alerts fire even if the app was force-stopped between saves. */
class BudgetCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun checker(): BudgetChecker
    }

    override suspend fun doWork(): Result {
        return try {
            val deps = EntryPointAccessors.fromApplication<Deps>(applicationContext)
            deps.checker().checkAfterSave()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NAME = "budget-sweep"

        /** Single entry point so fresh installs get sweeps without waiting for a reboot. */
        fun schedule(ctx: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresStorageNotLow(false)
                .build()
            val req = PeriodicWorkRequestBuilder<BudgetCheckWorker>(6, java.util.concurrent.TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.MINUTES)
                .addTag(NAME)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP, req,
            )
        }
    }
}
