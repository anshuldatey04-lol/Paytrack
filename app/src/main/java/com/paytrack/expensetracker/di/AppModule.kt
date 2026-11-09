package com.paytrack.expensetracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.paytrack.expensetracker.data.AppDatabase
import com.paytrack.expensetracker.data.CategoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "paytrack.db")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTxDao(db: AppDatabase) = db.transactions()

    @Provides
    fun provideBudgetDao(db: AppDatabase) = db.budgets()

    @Provides
    fun provideCategoryDao(db: AppDatabase) = db.categories()
}
