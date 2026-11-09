package com.paytrack.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.paytrack.expensetracker.data.PrefsManager
import com.paytrack.expensetracker.ui.screens.AddCashScreen
import com.paytrack.expensetracker.ui.screens.BudgetsScreen
import com.paytrack.expensetracker.ui.screens.CategoriesScreen
import com.paytrack.expensetracker.ui.screens.HomeScreen
import com.paytrack.expensetracker.ui.screens.OnboardingScreen
import com.paytrack.expensetracker.ui.screens.ReviewScreen
import com.paytrack.expensetracker.ui.screens.SettingsScreen
import com.paytrack.expensetracker.ui.screens.TransactionsScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: PrefsManager

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPostNotifsIfNeeded()
        val startTab = intent.getStringExtra(EXTRA_TAB)
        setContent {
            MaterialTheme {
                var onboarded by remember { mutableStateOf<Boolean?>(null) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) {
                    prefs.onboardingDone.collect { onboarded = it }
                }
                when (onboarded) {
                    null -> {}
                    false -> OnboardingScreen(onDone = {
                        scope.launch { prefs.setOnboardingDone() }
                    })
                    true -> AppNav(startTab = startTab)
                }
            }
        }
    }

    companion object {
        const val EXTRA_TAB = "tab"
    }

    private fun requestPostNotifsIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) return
        try {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (_: Exception) { }
    }
}

private data class Dest(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
private fun AppNav(startTab: String?) {
    val nav = rememberNavController()
    val dests = listOf(
        Dest("home", "Home") { Icon(Icons.Filled.Home, null) },
        Dest("tx", "Txns") { Icon(Icons.Filled.List, null) },
        Dest("review", "Review") { Icon(Icons.Filled.Warning, null) },
        Dest("budgets", "Budgets") { Icon(Icons.Filled.DateRange, null) },
        Dest("settings", "Settings") { Icon(Icons.Filled.AccountCircle, null) },
    )
    LaunchedEffect(startTab) {
        if (startTab == "review") nav.navigate("review")
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by nav.currentBackStackEntryAsState()
                val current = backStack?.destination?.route
                dests.forEach { d ->
                    NavigationBarItem(
                        selected = current == d.route,
                        onClick = { nav.navigate(d.route) { launchSingleTop = true } },
                        icon = d.icon,
                        label = { Text(d.label) },
                    )
                }
            }
        },
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(pad),
        ) {
            composable("home") {
                HomeScreen(
                    onAddCash = { nav.navigate("addCash") },
                    onSeeAll = { nav.navigate("tx") },
                    onReview = { nav.navigate("review") },
                )
            }
            composable("tx") { TransactionsScreen() }
            composable("review") { ReviewScreen() }
            composable("addCash") { AddCashScreen(onSaved = { nav.popBackStack() }) }
            composable("budgets") { BudgetsScreen() }
            composable("categories") { CategoriesScreen() }
            composable("settings") { SettingsScreen(onManageCategories = { nav.navigate("categories") }) }
        }
    }
}
