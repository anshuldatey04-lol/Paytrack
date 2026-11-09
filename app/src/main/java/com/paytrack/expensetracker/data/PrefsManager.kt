package com.paytrack.expensetracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.prefs by preferencesDataStore("paytrack")

/** Small key-value store: whitelist, onboarding, cash-on-hand, thresholds. Backed by DataStore. */
@Singleton
class PrefsManager @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val store = ctx.prefs

    val defaultApps = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "com.phonepe.app",
        "net.one97.paytm",
        "com.fampay.app",
    )

    val whitelistedApps: Flow<Set<String>> =
        store.data.map { it[KEY_APPS] ?: defaultApps }
    val onboardingDone: Flow<Boolean> =
        store.data.map { it[KEY_ONBOARDING] == true }
    val cashOnHand: Flow<Double> =
        store.data.map { it[KEY_CASH] ?: 0.0 }

    suspend fun setWhitelisted(apps: Set<String>) {
        store.edit { it[KEY_APPS] = apps }
    }

    suspend fun setOnboardingDone() {
        store.edit { it[KEY_ONBOARDING] = true }
    }

    suspend fun setCashOnHand(v: Double) {
        store.edit { it[KEY_CASH] = v }
    }

    suspend fun getWhitelistedOnce(): Set<String> = whitelistedApps.first()

    companion object {
        private val KEY_APPS = stringSetPreferencesKey("whitelisted_apps")
        private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_done")
        private val KEY_CASH = doublePreferencesKey("cash_on_hand")
    }
}
