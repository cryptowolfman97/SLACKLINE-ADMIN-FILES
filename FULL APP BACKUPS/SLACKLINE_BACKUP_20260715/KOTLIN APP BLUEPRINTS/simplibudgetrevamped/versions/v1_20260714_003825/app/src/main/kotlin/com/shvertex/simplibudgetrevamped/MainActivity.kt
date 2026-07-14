package com.shvertex.simplibudgetrevamped

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shvertex.simplibudgetrevamped.data.AppViewModel
import com.shvertex.simplibudgetrevamped.data.loadPrefs
import com.shvertex.simplibudgetrevamped.data.savePrefs
import com.shvertex.simplibudgetrevamped.ui.components.*
import com.shvertex.simplibudgetrevamped.ui.screens.*
import com.shvertex.simplibudgetrevamped.ui.theme.*

object Routes {
    const val HOME         = "home"
    const val BUDGET       = "budget"
    const val TRANSACTIONS = "transactions"
    const val ACCOUNTS     = "accounts"
    const val GOALS        = "goals"
    const val BILLS        = "bills"
    const val DEBTS        = "debts"
    const val REPORTS      = "reports"
    const val SETTINGS     = "settings"
}

private val bottomNavItems = listOf(
    NavItem("Home",     Icons.Default.Home,        Routes.HOME),
    NavItem("Budget",   Icons.Default.AccountBox,  Routes.BUDGET),
    NavItem("Log",      Icons.Default.List,        Routes.TRANSACTIONS),
    NavItem("Accounts", Icons.Default.Star,        Routes.ACCOUNTS),
    NavItem("More",     Icons.Default.Menu,        "more")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpliBudgetTheme {
                AppRoot(onExit = { finish() })
            }
        }
    }
}

@Composable
fun AppRoot(onExit: () -> Unit) {
    val vm: AppViewModel = viewModel()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.HOME

    var showExitDialog by remember { mutableStateOf(false) }
    var showMore       by remember { mutableStateOf(false) }

    // Onboarding — read prefs once after load
    var showOnboarding by remember { mutableStateOf(false) }
    var onboardingReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.load()
        // Check if onboarding should show
        val prefs = loadPrefs()
        showOnboarding = !prefs.onboardingDone
        onboardingReady = true
    }

    val mainRoutes = listOf(Routes.HOME, Routes.BUDGET, Routes.TRANSACTIONS, Routes.ACCOUNTS)

    BackHandler {
        when {
            showOnboarding -> { /* swallow back during onboarding */ }
            showMore -> showMore = false
            currentRoute == Routes.HOME -> showExitDialog = true
            currentRoute in mainRoutes -> {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = false }
                    launchSingleTop = true
                }
            }
            else -> { if (!navController.popBackStack()) showExitDialog = true }
        }
    }

    fun navigate(route: String) {
        if (route == "more") { showMore = true; return }
        showMore = false
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBg)
            .systemBarsPadding()
    ) {
        // Main app content
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME)         { HomeScreen(vm = vm, onNavigate = { navigate(it) }) }
                    composable(Routes.BUDGET)       { BudgetScreen(vm = vm, onBack = { navigate(Routes.HOME) }) }
                    composable(Routes.TRANSACTIONS) { TransactionsScreen(vm = vm, onBack = { navigate(Routes.HOME) }) }
                    composable(Routes.ACCOUNTS)     { AccountsScreen(vm = vm, onBack = { navigate(Routes.HOME) }) }
                    composable(Routes.GOALS)        { GoalsScreen(vm = vm, onBack = { navigate(Routes.HOME) }) }
                    composable(Routes.BILLS)        { BillsScreen(vm = vm, onBack = { navigate(Routes.HOME) }) }
                    composable(Routes.DEBTS)        { DebtsScreen(vm = vm, onBack = { navigate(Routes.HOME) }) }
                    composable(Routes.REPORTS)      { ReportsScreen(vm = vm, onBack = { navigate(Routes.HOME) }) }
                    composable(Routes.SETTINGS)     { SettingsScreen(vm = vm, onBack = { navigate(Routes.HOME) }) }
                }
            }
            BottomNavPill(items = bottomNavItems, currentRoute = currentRoute, onNavigate = { navigate(it) })
        }

        // More menu
        if (showMore) {
            MoreMenu(onNavigate = { route -> navigate(route) }, onDismiss = { showMore = false })
        }

        // Onboarding overlay — sits on top of everything
        if (onboardingReady && showOnboarding) {
            OnboardingOverlay(
                onDismiss = { dontShowAgain ->
                    showOnboarding = false
                    if (dontShowAgain) {
                        val prefs = loadPrefs()
                        savePrefs(prefs.copy(onboardingDone = true))
                    }
                }
            )
        }
    }

    if (showExitDialog) {
        ExitDialog(onConfirm = { onExit() }, onDismiss = { showExitDialog = false })
    }
}

@Composable
private fun MoreMenu(onNavigate: (String) -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBg.copy(alpha = 0.88f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
                .background(AmoledCard, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("MORE FEATURES", color = AmoledSubtext, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 4.dp))
            val items = listOf(
                Triple(Icons.Default.Star,         "Goals & Funds",  Routes.GOALS),
                Triple(Icons.Default.List,         "Bills Tracker",  Routes.BILLS),
                Triple(Icons.Default.ShoppingCart, "Debt Tracker",   Routes.DEBTS),
                Triple(Icons.Default.Star,         "Reports",        Routes.REPORTS),
                Triple(Icons.Default.Settings,     "Settings",       Routes.SETTINGS)
            )
            items.forEach { (icon, label, route) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AmoledNavBtn)
                        .clickable { onNavigate(route) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(icon, label, tint = Accent2, modifier = Modifier.size(22.dp))
                    Text(label, color = AmoledText, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, null, tint = AmoledSubtext, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
