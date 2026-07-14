package com.example.slacklineadminapp.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*

// ── Module entry point ────────────────────────────────────────────────────────
// Called from Slackline's NavGraph: SupabaseAdminScreen(onNavigateBack = { nav.popBackStack() })

@Composable
fun SupabaseAdminScreen(onNavigateBack: () -> Unit) {
    val vm: SupabaseViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.boot() }

    when {
        state.pinNeedsSetup -> PinSetupScreen(vm)
        !state.pinUnlocked  -> PinGateScreen(vm)
        else                -> MainShell(vm, state, onNavigateBack)
    }
}

// ── PIN screens (unchanged logic, refreshed look) ─────────────────────────────

@Composable
fun PinSetupScreen(vm: SupabaseViewModel) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("set") }
    var error by remember { mutableStateOf(false) }
    val digits = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")

    Box(modifier = Modifier.fillMaxSize().background(BgBlack), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(TealCol.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Lock, null, tint = TealCol, modifier = Modifier.size(32.dp)) }
            Text("Supabase Admin", color = TealCol, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                if (stage == "set") "Set a 4-digit PIN to protect your credentials" else "Confirm your PIN",
                color = SubText, fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val current = if (stage == "set") pin else confirm
                repeat(4) { i ->
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(if (i < current.length) TealCol else CardBg2))
                }
            }
            if (error) Text("PINs don't match. Try again.", color = RedCol, fontSize = 12.sp)
            // Numpad
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                digits.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp).clip(CircleShape)
                                    .background(if (digit.isBlank()) Color.Transparent else CardBg2)
                                    .clickable(enabled = digit.isNotBlank()) {
                                        val current = if (stage == "set") pin else confirm
                                        if (digit == "⌫") {
                                            error = false
                                            if (stage == "set" && pin.isNotEmpty()) pin = pin.dropLast(1)
                                            else if (stage == "confirm" && confirm.isNotEmpty()) confirm = confirm.dropLast(1)
                                        } else if (current.length < 4) {
                                            if (stage == "set") {
                                                pin += digit
                                                if (pin.length == 4) stage = "confirm"
                                            } else {
                                                confirm += digit
                                                if (confirm.length == 4) {
                                                    if (confirm == pin) vm.setupPin(pin)
                                                    else { error = true; pin = ""; confirm = ""; stage = "set" }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) { if (digit.isNotBlank()) Text(digit, color = TextCol, fontSize = 20.sp, fontWeight = FontWeight.Light) }
                        }
                    }
                }
            }
            Text("Your PIN protects access to all saved\nSupabase credentials and API keys.", color = SubText, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun PinGateScreen(vm: SupabaseViewModel) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val digits = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")

    Box(modifier = Modifier.fillMaxSize().background(BgBlack), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Lock, null, tint = TealCol, modifier = Modifier.size(52.dp))
            Text("SupaAdmin", color = TealCol, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Module", color = TealCol.copy(0.6f), fontSize = 13.sp)
            Text("Enter your PIN to continue", color = SubText, fontSize = 13.sp)
            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { i ->
                    Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(if (i < pin.length) TealCol else CardBg2))
                }
            }
            if (error) Text("Incorrect PIN", color = RedCol, fontSize = 12.sp)
            // Numpad
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                digits.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp).clip(CircleShape)
                                    .background(if (digit.isBlank()) Color.Transparent else CardBg2)
                                    .clickable(enabled = digit.isNotBlank()) {
                                        if (digit == "⌫") {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            error = false
                                        } else if (pin.length < 4) {
                                            pin += digit
                                            if (pin.length == 4) {
                                                if (!vm.unlockPin(pin)) { error = true; pin = "" }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) { if (digit.isNotBlank()) Text(digit, color = TextCol, fontSize = 20.sp, fontWeight = FontWeight.Light) }
                        }
                    }
                }
            }
        }
    }
}

// ── Main shell with bottom nav ────────────────────────────────────────────────

@Composable
fun MainShell(vm: SupabaseViewModel, state: SupabaseUiState, onNavigateBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Back: sub-screens collapse first → sections collapse to HOME → return to Slackline dashboard
    BackHandler(enabled = true) {
        when {
            state.activeTableForDetail != null                 -> vm.closeTableDetail()
            state.activeBucket != null                         -> vm.closeBucket()
            state.activeScreen == NavScreen.WEB_DASHBOARD      -> { /* handled inside WebDashboardScreen */ }
            state.activeSection != NavSection.HOME             -> vm.navigateToSection(NavSection.HOME)
            else                                               -> onNavigateBack()
        }
    }

    LaunchedEffect(state.snackMessage) {
        state.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { SupaBottomNav(state, vm) },
        containerColor = BgBlack
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            MainContent(vm, state)
            if (state.isLoading) LoadingOverlay(state.loadingMessage)
            if (state.showGlobalSearch) GlobalSearchDialog(vm, state)
            state.pendingAction?.let { ActionDialog(it, vm) }
            state.updateRowContext?.let { UpdateRowDialog(it, vm) }
            state.insertRowContext?.let { InsertRowDialog(it, vm) }
            if (state.showCreateUser) CreateUserDialog(vm)
            if (state.showAddSecretPayload) AddSecretDialog(vm)
            if (state.showFunctionInvoker) FunctionInvokerDialog(state, vm)
            if (state.showSaveSnippetDialog) SaveSnippetDialog(vm, state)
            if (state.showSaveConnectionDialog) SaveConnectionDialog(state, vm)
            if (state.showSetPinDialog) SetPinDialog(vm)
            if (state.showWebViewCredDialog) WebViewCredDialog(vm, state)
            if (state.showCreateCronJob) CreateCronJobDialog(vm)
        }
    }
}

// ── Bottom navigation ─────────────────────────────────────────────────────────

@Composable
fun SupaBottomNav(state: SupabaseUiState, vm: SupabaseViewModel) {
    val items = listOf(
        Triple(NavSection.HOME,     Icons.Default.Home,           "Home"),
        Triple(NavSection.DATABASE, Icons.Default.Storage,        "Database"),
        Triple(NavSection.AUTH,     Icons.Default.People,         "Auth"),
        Triple(NavSection.DEVTOOLS, Icons.Default.Cloud,          "DevTools"),
        Triple(NavSection.MORE,     Icons.Default.MoreHoriz,      "More")
    )
    NavigationBar(
        containerColor = CardBg,
        tonalElevation = 0.dp
    ) {
        items.forEach { (section, icon, label) ->
            NavigationBarItem(
                selected = state.activeSection == section,
                onClick = {
                    if (state.activeSection == section) {
                        // Tap active section again = open global search from HOME, else go to section default
                        if (section == NavSection.HOME) vm.showGlobalSearch()
                    } else {
                        vm.navigateToSection(section)
                    }
                },
                icon = { Icon(icon, null, modifier = Modifier.size(22.dp)) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealCol,
                    selectedTextColor = TealCol,
                    indicatorColor = TealCol.copy(0.12f),
                    unselectedIconColor = SubText,
                    unselectedTextColor = SubText
                )
            )
        }
    }
}

// ── Content router ────────────────────────────────────────────────────────────

@Composable
fun MainContent(vm: SupabaseViewModel, state: SupabaseUiState) {
    val sectionSubNavs = mapOf(
        NavSection.HOME to listOf(
            "Dashboard" to NavScreen.DASHBOARD,
            "Overview"  to NavScreen.OVERVIEW,
            "Projects"  to NavScreen.PROJECTS,
            "Usage"     to NavScreen.USAGE
        ),
        NavSection.DATABASE to listOf(
            "Tables"     to NavScreen.TABLES,
            "SQL Editor" to NavScreen.SQL,
            "Policies"   to NavScreen.POLICIES,
            "Migrations" to NavScreen.MIGRATIONS,
            "Cron Jobs"  to NavScreen.CRON,
            "Webhooks"   to NavScreen.WEBHOOKS
        ),
        NavSection.AUTH to listOf(
            "Users"   to NavScreen.USERS,
            "Secrets" to NavScreen.SECRETS
        ),
        NavSection.DEVTOOLS to listOf(
            "Storage"   to NavScreen.STORAGE,
            "Functions" to NavScreen.FUNCTIONS,
            "Logs"      to NavScreen.LOGS,
            "Realtime"  to NavScreen.REALTIME
        ),
        NavSection.MORE to listOf(
            "Connections"    to NavScreen.CONNECTIONS,
            "Credentials"    to NavScreen.CREDENTIALS,
            "Web Dashboard"  to NavScreen.WEB_DASHBOARD,
            "Settings"       to NavScreen.SETTINGS
        )
    )

    Column(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        // Top app bar with section title + search + refresh
        TopBar(state, vm)

        // Sub-nav chips for current section
        sectionSubNavs[state.activeSection]?.let { subs ->
            if (subs.size > 1) {
                SubNavRow(subs, state.activeScreen) { vm.navigateTo(it) }
            }
        }

        // Screen content
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.activeScreen) {
                NavScreen.DASHBOARD   -> DashboardScreen(vm, state)
                NavScreen.OVERVIEW    -> OverviewScreen(vm, state)
                NavScreen.PROJECTS    -> ProjectsScreen(vm, state)
                NavScreen.USAGE       -> UsageScreen(vm, state)
                NavScreen.TABLES      -> TablesScreen(vm, state)
                NavScreen.SQL         -> SqlScreen(vm, state)
                NavScreen.POLICIES    -> PoliciesScreen(vm, state)
                NavScreen.MIGRATIONS  -> MigrationsScreen(vm, state)
                NavScreen.CRON        -> CronScreen(vm, state)
                NavScreen.WEBHOOKS    -> WebhooksScreen(vm, state)
                NavScreen.USERS       -> UsersScreen(vm, state)
                NavScreen.SECRETS     -> SecretsScreen(vm, state)
                NavScreen.STORAGE     -> StorageScreen(vm, state)
                NavScreen.FUNCTIONS   -> FunctionsScreen(vm, state)
                NavScreen.LOGS        -> LogsScreen(vm, state)
                NavScreen.REALTIME    -> RealtimeScreen(vm, state)
                NavScreen.CONNECTIONS -> ConnectionsScreen(vm, state)
                NavScreen.CREDENTIALS -> CredentialsScreen(vm, state)
                NavScreen.WEB_DASHBOARD -> WebDashboardScreen(vm, state)
                NavScreen.SETTINGS    -> SettingsScreen(vm, state)
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
fun TopBar(state: SupabaseUiState, vm: SupabaseViewModel) {
    val screenLabel = when (state.activeScreen) {
        NavScreen.DASHBOARD -> "Dashboard"
        NavScreen.OVERVIEW -> "Overview"
        NavScreen.PROJECTS -> "Projects"
        NavScreen.USAGE -> "Usage"
        NavScreen.TABLES -> "Tables"
        NavScreen.SQL -> "SQL Editor"
        NavScreen.POLICIES -> "RLS Policies"
        NavScreen.MIGRATIONS -> "Migrations"
        NavScreen.CRON -> "Cron Jobs"
        NavScreen.WEBHOOKS -> "Webhooks"
        NavScreen.USERS -> "Users"
        NavScreen.SECRETS -> "Secrets"
        NavScreen.STORAGE -> "Storage"
        NavScreen.FUNCTIONS -> "Edge Functions"
        NavScreen.LOGS -> "Logs"
        NavScreen.REALTIME -> "Realtime"
        NavScreen.CONNECTIONS -> "Connections"
        NavScreen.CREDENTIALS -> "Credentials"
        NavScreen.WEB_DASHBOARD -> "Web Dashboard"
        NavScreen.SETTINGS -> "Settings"
    }
    val ref = state.cfg.currentRef.let { if (it.isNotBlank()) it else "No project set" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App logo + name
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(TealCol.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) { Text("S", color = TealCol, fontWeight = FontWeight.Black, fontSize = 18.sp) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("SupaAdmin", color = TealCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(4.dp))
                Text("Module", color = TealCol.copy(0.55f), fontSize = 10.sp, modifier = Modifier.padding(bottom = 1.dp))
                Spacer(Modifier.width(8.dp))
                Text("· $screenLabel", color = SubText, fontSize = 13.sp)
            }
            Text(ref, color = SubText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Global search
        IconButton(onClick = { vm.showGlobalSearch() }) {
            Icon(Icons.Default.Search, null, tint = SubText)
        }
        // Refresh (not shown on static screens or WebView)
        if (state.activeScreen !in listOf(NavScreen.DASHBOARD, NavScreen.CREDENTIALS, NavScreen.CONNECTIONS, NavScreen.SETTINGS, NavScreen.WEB_DASHBOARD, NavScreen.REALTIME)) {
            IconButton(onClick = { vm.forceRefresh() }) {
                Icon(Icons.Default.Refresh, null, tint = SubText)
            }
        }
    }
}

// ── DASHBOARD ─────────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val overview = state.overviewCache?.payload
    LaunchedEffect(Unit) { if (state.overviewCache == null) vm.loadOverview() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Project header
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(TealCol.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bolt, null, tint = TealCol, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            overview?.currentProject?.name?.ifBlank { state.cfg.currentRef } ?: state.cfg.currentRef.ifBlank { "Not connected" },
                            color = TextCol, fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val plan = overview?.currentProject?.planName ?: "--"
                            val region = overview?.currentProject?.displayRegion ?: "--"
                            StatusChip(plan, TealCol, small = true)
                            Text(region, color = SubText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            // Stat cards 2x2
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = overview?.tables?.size?.toString() ?: "--",
                        label = "Tables",
                        icon = Icons.Default.TableChart,
                        color = TealCol,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = overview?.users?.size?.toString() ?: "--",
                        label = "Users",
                        icon = Icons.Default.People,
                        color = BlueCol,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = overview?.functions?.size?.toString() ?: "--",
                        label = "Functions",
                        icon = Icons.Default.Bolt,
                        color = AmberCol,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = overview?.buckets?.size?.toString() ?: "--",
                        label = "Buckets",
                        icon = Icons.Default.Cloud,
                        color = PurpleCol,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            // Quick actions
            SCard {
                SectionHeader("Quick Actions")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClickChip("SQL Editor", TealCol) { vm.navigateTo(NavScreen.SQL) }
                    ClickChip("Users", BlueCol) { vm.navigateTo(NavScreen.USERS) }
                    ClickChip("Logs", AmberCol) { vm.navigateTo(NavScreen.LOGS) }
                    ClickChip("Realtime", GreenCol) { vm.navigateTo(NavScreen.REALTIME) }
                }
            }
        }

        item {
            // Features & guide button
            var showFeatures by remember { mutableStateOf(false) }
            SCard {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showFeatures = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MenuBook, null, tint = TealCol, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Feature List & Session Guide", color = TextCol, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Full feature list, how to start, credentials guide — copyable", color = SubText, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = SubText, modifier = Modifier.size(16.dp))
                }
            }
            if (showFeatures) FeatureListDialog(onDismiss = { showFeatures = false })
        }

        if (overview?.errors?.isNotEmpty() == true) {
            item {
                SCard {
                    SectionHeader("Warnings", AmberCol)
                    Spacer(Modifier.height(6.dp))
                    overview.errors.forEach { err ->
                        Text("• $err", color = AmberCol, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            // Recent tables
            if (overview?.tables?.isNotEmpty() == true) {
                SCard {
                    SectionHeader("Tables")
                    Spacer(Modifier.height(8.dp))
                    overview.tables.take(5).forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.navigateTo(NavScreen.TABLES); vm.openTableDetail(t) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TableRows, null, tint = SubText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(t.name, color = TextCol, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(t.row_estimate + " rows", color = SubText, fontSize = 11.sp)
                        }
                        SDivider()
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { vm.navigateTo(NavScreen.TABLES) }) {
                        Text("View all tables →", color = TealCol, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            // Web Dashboard shortcut card
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OpenInBrowser, null, tint = BlueCol, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Supabase Web Dashboard", color = TextCol, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Open full dashboard in browser view", color = SubText, fontSize = 11.sp)
                    }
                    SecondaryButton("Open", onClick = { vm.navigateToWebDashboard() })
                }
            }
        }
    }
}

// ── OVERVIEW ──────────────────────────────────────────────────────────────────

@Composable
fun OverviewScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val cache = state.overviewCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadOverview() }
    if (cache == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }
        return
    }
    val overview = cache.payload
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            cache.error?.let {
                ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard() })
            }
        }
        if (overview != null) {
            item {
                StatGrid(
                    Triple(overview.tables.size.toString(), "Tables", TealCol),
                    Triple(overview.users.size.toString(), "Users", BlueCol),
                    Triple(overview.functions.size.toString(), "Functions", AmberCol),
                    Triple(overview.buckets.size.toString(), "Buckets", PurpleCol),
                    Triple(overview.secrets.size.toString(), "Secrets", RedCol),
                    Triple(overview.projects.size.toString(), "Projects", GreenCol)
                )
            }
            if (overview.currentProject.name.isNotBlank()) {
                item {
                    SCard {
                        SectionHeader("Current Project")
                        Spacer(Modifier.height(8.dp))
                        IconLabelRow(Icons.Default.Folder, "Name", overview.currentProject.name)
                        IconLabelRow(Icons.Default.Tag, "Ref", overview.currentProject.ref)
                        IconLabelRow(Icons.Default.Place, "Region", overview.currentProject.displayRegion)
                        IconLabelRow(Icons.Default.CreditCard, "Plan", overview.currentProject.planName)
                        IconLabelRow(Icons.Default.Info, "Status", overview.currentProject.status)
                    }
                }
            }
        }
    }
}

// ── PROJECTS ──────────────────────────────────────────────────────────────────

@Composable
fun ProjectsScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val cache = state.projectsCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadProjects() }
    if (cache == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cache.error?.let { item { ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard("/projects") }) } }
        items(cache.payload ?: emptyList()) { proj ->
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(proj.name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(proj.ref, color = SubText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            StatusChip(proj.status, if (proj.status == "ACTIVE_HEALTHY") GreenCol else AmberCol, small = true)
                            StatusChip(proj.planName, BlueCol, small = true)
                            Text(proj.displayRegion, color = SubText, fontSize = 10.sp)
                        }
                    }
                    PrimaryButton("Use", onClick = { vm.useProject(proj) }, modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}

// ── USAGE ─────────────────────────────────────────────────────────────────────

@Composable
fun UsageScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val cache = state.usageCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadUsage() }
    if (cache == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }
        return
    }
    val m = cache.payload?.metrics
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        cache.error?.let { item { ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard("/usage") }) } }
        item {
            StatGrid(
                Triple(m?.dbSizeBytes?.let { formatBytes(it) } ?: "--", "Database Size", TealCol),
                Triple(m?.storageSizeBytes?.let { formatBytes(it) } ?: "--", "Storage Used", BlueCol),
                Triple(m?.monthlyActiveUsers?.toString() ?: "--", "Monthly Active Users", AmberCol),
                Triple(m?.apiRequestsCount?.toString() ?: "--", "API Requests", PurpleCol),
                Triple(m?.edgeInvocations?.toString() ?: "--", "Edge Invocations", GreenCol),
                Triple(m?.thirdPartyMau?.toString() ?: "--", "3rd-Party MAU", RedCol)
            )
        }
        // Bar chart: static placeholder data using what we have
        if (m != null) {
            item {
                SCard {
                    SectionHeader("Resource Overview")
                    Spacer(Modifier.height(12.dp))
                    val chartEntries = listOfNotNull(
                        m.monthlyActiveUsers?.let { "MAU" to it.toFloat() },
                        m.thirdPartyMau?.let { "3rd Party" to it.toFloat() }
                    )
                    if (chartEntries.isNotEmpty()) {
                        BarChartView(entries = chartEntries, label = "Users", barColor = BlueCol)
                    } else {
                        Text("Load usage data to see charts.", color = SubText, fontSize = 12.sp)
                    }
                }
            }
            item {
                SCard {
                    SectionHeader("Plan & Region")
                    Spacer(Modifier.height(8.dp))
                    IconLabelRow(Icons.Default.CreditCard, "Plan", m.planName)
                    IconLabelRow(Icons.Default.Place, "Region", m.region)
                }
            }
        }
        cache.payload?.notes?.filter { it.isNotBlank() }?.let { notes ->
            if (notes.isNotEmpty()) {
                item {
                    SCard {
                        SectionHeader("Notes", AmberCol)
                        Spacer(Modifier.height(6.dp))
                        notes.forEach { Text("• $it", color = AmberCol, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}

// ── TABLES ────────────────────────────────────────────────────────────────────

@Composable
fun TablesScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val activeTable = state.activeTableForDetail
    if (activeTable != null) {
        TableDetailScreen(activeTable, vm, state)
        BackHandler { vm.closeTableDetail() }
        return
    }

    val cache = state.tablesCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadTables() }
    val tables = cache?.payload ?: emptyList()
    val schemas = (listOf("All") + tables.map { it.schema }.distinct().sorted())

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchField(state.tableSearch, vm::setTableSearch, "Search tables...")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(schemas) { schema ->
                    ClickChip(schema, if (state.tableSchemaFilter == schema) TealCol else SubText) {
                        vm.setTableSchemaFilter(schema)
                    }
                }
            }
        }

        if (cache == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }
            return@Column
        }

        cache.error?.let {
            Box(modifier = Modifier.padding(16.dp)) {
                ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard("/editor") })
            }
        }

        val filtered = tables
            .filter { state.tableSchemaFilter == "All" || it.schema == state.tableSchemaFilter }
            .filter { state.tableSearch.isBlank() || it.name.contains(state.tableSearch, true) || it.schema.contains(state.tableSearch, true) }

        if (filtered.isEmpty() && cache.error == null) {
            EmptyState("No tables found")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { table ->
                    TableRow(table, vm, state)
                }
            }
        }
    }
}

@Composable
fun TableRow(table: SupabaseTable, vm: SupabaseViewModel, state: SupabaseUiState) {
    var showMenu by remember { mutableStateOf(false) }
    SCard {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { vm.openTableDetail(table) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.TableRows, null, tint = TealCol, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(table.name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(table.schema, color = SubText, fontSize = 11.sp)
                    Text("${table.row_estimate} rows", color = SubText, fontSize = 11.sp)
                    table.rls_enabled?.let {
                        StatusChip(if (it) "RLS ON" else "RLS OFF", if (it) GreenCol else AmberCol, small = true)
                    }
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = SubText, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = CardBg2) {
                    DropdownMenuItem(text = { Text("Preview", color = TextCol, fontSize = 13.sp) }, onClick = { showMenu = false; vm.previewTable(table.name, table.schema) }, leadingIcon = { Icon(Icons.Default.Visibility, null, tint = SubText, modifier = Modifier.size(14.dp)) })
                    DropdownMenuItem(text = { Text("Export CSV", color = TextCol, fontSize = 13.sp) }, onClick = { showMenu = false; vm.showCsvExport(table.name) }, leadingIcon = { Icon(Icons.Default.Download, null, tint = SubText, modifier = Modifier.size(14.dp)) })
                    DropdownMenuItem(text = { Text("Query in SQL", color = TextCol, fontSize = 13.sp) }, onClick = { showMenu = false; vm.openInSql("SELECT * FROM ${table.schema}.${table.name} LIMIT 50;") }, leadingIcon = { Icon(Icons.Default.Code, null, tint = SubText, modifier = Modifier.size(14.dp)) })
                }
            }
        }
    }

    // Preview dialog
    state.activeTablePreview?.let { preview ->
        if (preview.key == "${table.schema}.${table.name}") {
            TablePreviewDialog(preview, vm)
        }
    }

    // CSV export dialog
    if (state.showCsvExportDialog && state.csvExportTableName == table.name) {
        CsvExportDialog(state, vm)
    }
}

@Composable
fun TableDetailScreen(table: SupabaseTable, vm: SupabaseViewModel, state: SupabaseUiState) {
    val sortedRows = if (state.tableSortColumn.isNotBlank()) {
        state.tableDataRows.sortedWith(compareBy { row ->
            row[state.tableSortColumn]?.toString() ?: ""
        }).let { if (!state.tableSortAsc) it.reversed() else it }
    } else state.tableDataRows

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(CardBg2).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.closeTableDetail() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = SubText)
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(table.name, color = TextCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${table.schema} · ${state.tableDataRows.size} rows loaded", color = SubText, fontSize = 11.sp)
            }
            // Insert row
            IconButton(onClick = {
                if (state.tableColumns.isNotEmpty()) vm.showInsertRow(table, state.tableColumns)
                else vm.showInsertRow(table, emptyList())
            }) {
                Icon(Icons.Default.Add, null, tint = TealCol)
            }
            IconButton(onClick = { vm.showCsvExport(table.name) }) {
                Icon(Icons.Default.Download, null, tint = SubText)
            }
        }

        if (state.tableColumnsLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }
            return@Column
        }

        // Column summary
        if (state.tableColumns.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.tableColumns) { col ->
                    val color = if (col.is_primary_key) AmberCol else if (!col.is_nullable) BlueCol else SubText
                    StatusChip("${col.name}: ${col.data_type}", color, small = true)
                }
            }
        }

        // Data grid
        if (sortedRows.isNotEmpty() && state.sqlResultColumns.isEmpty()) {
            val cols = state.tableColumns.map { it.name }.ifEmpty { sortedRows.firstOrNull()?.keys?.toList() ?: emptyList() }
            val pkCol = state.tableColumns.firstOrNull { it.is_primary_key }?.name ?: cols.firstOrNull()
            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                DataGrid(
                    columns = cols,
                    rows = sortedRows,
                    sortColumn = state.tableSortColumn,
                    sortAsc = state.tableSortAsc,
                    onSortClick = { vm.setTableSort(it) },
                    onDeleteRowClick = { row ->
                        val col = pkCol
                        val value = col?.let { row[it]?.toString() }
                        if (col != null && value != null) {
                            vm.requestDeleteRow(table.name, col, value)
                        }
                    }
                )
            }
        } else if (sortedRows.isEmpty() && !state.tableDataLoading) {
            EmptyState("No rows loaded. Check credentials or table permissions.")
        }

        // Load more
        if (state.tableDataHasMore) {
            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                if (state.tableDataLoading) CircularProgressIndicator(color = TealCol, modifier = Modifier.size(24.dp))
                else TextButton(onClick = { vm.loadMoreTableData() }) {
                    Text("Load more rows", color = TealCol, fontSize = 13.sp)
                }
            }
        }
    }

    if (state.showCsvExportDialog && state.csvExportTableName == table.name) {
        CsvExportDialog(state, vm)
    }

    if (state.showDeleteRowConfirm && state.deleteRowTarget?.first == table.name) {
        DeleteRowConfirmDialog(vm)
    }
}

@Composable
fun DeleteRowConfirmDialog(vm: SupabaseViewModel) {
    Dialog(onDismissRequest = { vm.dismissDeleteRow() }) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(CardBg2).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Delete row?", color = TextCol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "This will permanently delete this row from the table. This action cannot be undone.",
                color = SubText, fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Cancel", onClick = { vm.dismissDeleteRow() }, modifier = Modifier.weight(1f))
                DangerButton("Delete", onClick = { vm.confirmDeleteRow() }, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ── SQL EDITOR ────────────────────────────────────────────────────────────────

@Composable
fun SqlScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    var showSchemaDump by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // SQL / Schema Dump toggle
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ClickChip("SQL Editor", if (!showSchemaDump) TealCol else SubText) { showSchemaDump = false }
            ClickChip("Schema Dump", if (showSchemaDump) TealCol else SubText) { showSchemaDump = true }
        }

        if (showSchemaDump) {
            SchemaDumpPanel(state, vm)
            return@Column
        }
        // Query input
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBg2)
                    .border(1.dp, if (state.sqlRunning) TealCol else CardBg2, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                BasicTextField(
                    value = state.sqlQuery,
                    onValueChange = vm::setSqlQuery,
                    textStyle = TextStyle(color = TextCol, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(TealCol),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 180.dp),
                    decorationBox = { inner ->
                        if (state.sqlQuery.isEmpty()) Text("SELECT * FROM ...", color = SubText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        inner()
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    if (state.sqlRunning) "Running…" else "▶ Run",
                    onClick = { vm.runSql() },
                    enabled = !state.sqlRunning,
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton("Snippets", onClick = { vm.showSnippetsPanel() })
                SecondaryButton("Save", onClick = { vm.showSaveSnippetDialog() })
                if (state.sqlResult.isNotBlank()) {
                    IconButton(onClick = { vm.showSqlCsvExport() }) {
                        Icon(Icons.Default.Download, null, tint = SubText)
                    }
                }
            }
        }

        // History chips
        if (state.sqlHistory.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.sqlHistory.reversed().take(10)) { h ->
                    ClickChip(h.take(30), SubText) { vm.loadSqlHistoryItem(h) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Results
        if (state.sqlResultRows.isNotEmpty() && state.sqlResultColumns.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Results — ${state.sqlResultRows.size} rows", color = SubText, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text("Tap cell to copy", color = SubText, fontSize = 10.sp)
                }
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    DataGrid(
                        columns = state.sqlResultColumns,
                        rows = state.sqlResultRows
                    )
                }
            }
        } else if (state.sqlResult.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                CodeBlock(state.sqlResult)
            }
        }

        // Snippets panel
        if (state.showSnippetsPanel) {
            Dialog(onDismissRequest = { vm.dismissSnippetsPanel() }) {
                Surface(color = CardBg2, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Saved Snippets", color = TextCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(10.dp))
                        if (state.sqlSnippets.isEmpty()) {
                            Text("No saved snippets yet. Write a query and tap Save.", color = SubText, fontSize = 13.sp)
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(state.sqlSnippets) { s ->
                                    SCard2 {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f).clickable { vm.loadSnippet(s) }) {
                                                Text(s.name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text(s.sql.take(60), color = SubText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            IconButton(onClick = { vm.deleteSnippet(s.id) }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Default.Delete, null, tint = RedCol, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("Close", onClick = { vm.dismissSnippetsPanel() }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (state.showSqlCsvExportDialog) {
            AlertDialog(
                onDismissRequest = { vm.dismissSqlCsvExport() },
                containerColor = CardBg2,
                title = { Text("Export SQL Results as CSV", color = TextCol, fontWeight = FontWeight.Bold) },
                text = { Text("This will export the current query results as a CSV file.", color = SubText, fontSize = 13.sp) },
                confirmButton = { PrimaryButton("Export", onClick = { vm.exportSqlResultCsv(state.sqlQuery) }) },
                dismissButton = { SecondaryButton("Cancel", onClick = { vm.dismissSqlCsvExport() }) }
            )
        }
    }
}

// ── POLICIES ──────────────────────────────────────────────────────────────────

@Composable
fun PoliciesScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val cache = state.policiesCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadPolicies() }
    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(state.policySearch, vm::setPolicySearch, "Search policies...", modifier = Modifier.padding(16.dp))
        if (cache == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }; return@Column }
        cache.error?.let {
            Box(modifier = Modifier.padding(16.dp)) { ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard("/auth/policies") }) }
        }
        val policies = (cache.payload ?: emptyList()).filter { state.policySearch.isBlank() || it.name.contains(state.policySearch, true) || it.table.contains(state.policySearch, true) }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(policies) { p ->
                SCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${p.schema}.${p.table}", color = SubText, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusChip(p.command, BlueCol, small = true)
                            if (p.enabled) StatusChip("ON", GreenCol, small = true) else StatusChip("OFF", RedCol, small = true)
                        }
                    }
                    if (p.roles.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Roles: ${p.roles.joinToString(", ")}", color = SubText, fontSize = 11.sp)
                    }
                    p.definition?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("USING: $it", color = SubText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            if (policies.isEmpty()) item { EmptyState("No policies found") }
        }
    }
}

// ── MIGRATIONS ────────────────────────────────────────────────────────────────

@Composable
fun MigrationsScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val cache = state.migrationsCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadMigrations() }
    if (cache == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }; return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cache.error?.let { item { ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard("/database/migrations") }) } }
        items(cache.payload ?: emptyList()) { m ->
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(m.name.ifBlank { m.version }, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("v${m.version}", color = SubText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        m.inserted_at?.let { Text(shortTime(it), color = SubText, fontSize = 10.sp) }
                    }
                    StatusChip(m.status, if (m.status == "applied" || m.status == "--") GreenCol else AmberCol, small = true)
                }
            }
        }
        if (cache.payload?.isEmpty() == true) item { EmptyState("No migrations found") }
    }
}

// ── CRON JOBS ─────────────────────────────────────────────────────────────────

@Composable
fun CronScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val cache = state.cronCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadCronJobs() }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(state.cronSearch, vm::setCronSearch, "Search cron jobs...", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { vm.showCreateCronJob() }) {
                Icon(Icons.Default.Add, null, tint = TealCol)
            }
        }
        if (cache == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }; return@Column }
        cache.error?.let { Box(modifier = Modifier.padding(16.dp)) { ErrorBanner(it) } }
        val jobs = (cache.payload ?: emptyList()).filter { state.cronSearch.isBlank() || it.jobname.contains(state.cronSearch, true) }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(jobs) { job ->
                SCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(job.jobname.ifBlank { "Job ${job.jobid}" }, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(job.schedule, color = TealCol, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text(job.command.take(60), color = SubText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusChip(if (job.active) "Active" else "Disabled", if (job.active) GreenCol else AmberCol, small = true)
                            Row {
                                IconButton(onClick = { vm.toggleCronJob(job.jobid, !job.active) }, modifier = Modifier.size(28.dp)) {
                                    Icon(if (job.active) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = BlueCol, modifier = Modifier.size(14.dp))
                                }
                                IconButton(onClick = { vm.deleteCronJob(job.jobname) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = RedCol, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            if (jobs.isEmpty() && cache.error == null) item { EmptyState("No cron jobs found. pg_cron extension must be enabled.") }
        }
    }
    if (state.showCreateCronJob) CreateCronJobDialog(vm)
}

@Composable
fun CreateCronJobDialog(vm: SupabaseViewModel) {
    var name by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("0 * * * *") }
    var command by remember { mutableStateOf("SELECT 1;") }
    Dialog(onDismissRequest = { vm.dismissCreateCronJob() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardBg2, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("New Cron Job", color = TextCol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                STextField(name, { name = it }, "Job Name", hint = "my_job")
                STextField(schedule, { schedule = it }, "Schedule (cron expression)", hint = "0 * * * *")
                STextField(command, { command = it }, "SQL Command", hint = "SELECT clean_old_data();", singleLine = false)
                InfoBanner("Schedule format: minute hour day month weekday. Example: '0 2 * * *' = daily at 2am")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Cancel", { vm.dismissCreateCronJob() }, modifier = Modifier.weight(1f))
                    PrimaryButton("Create", { vm.createCronJob(name, schedule, command) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── WEBHOOKS ──────────────────────────────────────────────────────────────────

@Composable
fun WebhooksScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val cache = state.webhooksCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadWebhooks() }
    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(state.webhookSearch, vm::setWebhookSearch, "Search webhooks...", modifier = Modifier.padding(16.dp))
        if (cache == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }; return@Column }
        cache.error?.let {
            Box(modifier = Modifier.padding(16.dp)) { ErrorBanner(it) }
            InfoBanner("Webhooks require supabase_functions.hooks table. Ensure Database Webhooks are enabled in your project.", modifier = Modifier.padding(horizontal = 16.dp))
        }
        val hooks = (cache.payload ?: emptyList()).filter { state.webhookSearch.isBlank() || it.name.contains(state.webhookSearch, true) || it.table_name.contains(state.webhookSearch, true) }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(hooks) { hook ->
                SCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(hook.name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${hook.schema_name}.${hook.table_name}", color = SubText, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                                hook.events.forEach { ev -> StatusChip(ev, BlueCol, small = true) }
                            }
                            Text(hook.service_url.take(50), color = SubText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusChip(if (hook.active) "Active" else "Off", if (hook.active) GreenCol else AmberCol, small = true)
                            IconButton(onClick = { vm.deleteWebhook(hook.id, hook.name) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, null, tint = RedCol, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            if (hooks.isEmpty() && cache.error == null) item { EmptyState("No webhooks found.", Icons.Default.Webhook) }
        }
    }
}

// ── USERS ─────────────────────────────────────────────────────────────────────

@Composable
fun UsersScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val cache = state.usersCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadUsers() }
    val allUsers = cache?.payload ?: emptyList()
    val filtered = allUsers.filter {
        state.userSearch.isBlank() ||
        (it.email ?: "").contains(state.userSearch, true) ||
        (it.phone ?: "").contains(state.userSearch, true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(state.userSearch, vm::setUserSearch, "Search users...", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { vm.showCreateUser() }) { Icon(Icons.Default.PersonAdd, null, tint = TealCol) }
        }

        if (cache == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }; return@Column }
        cache.error?.let { Box(Modifier.padding(horizontal = 16.dp)) { ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard("/auth/users") }) } }

        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("${filtered.size} of ${allUsers.size} users", color = SubText, fontSize = 11.sp)
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { user ->
                UserRow(user, vm)
            }
            if (filtered.isEmpty() && cache.error == null) item { EmptyState("No users found") }
        }
    }
}

@Composable
fun UserRow(user: SupabaseUser, vm: SupabaseViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    SCard {
        Row(modifier = Modifier.clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(BlueCol.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(user.displayName.take(1).uppercase(), color = BlueCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(user.providerStr, PurpleCol, small = true)
                    if (user.isBanned) StatusChip("Banned", RedCol, small = true)
                    if (!user.isConfirmed) StatusChip("Unconfirmed", AmberCol, small = true)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = SubText, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = CardBg2) {
                    if (user.isBanned) {
                        DropdownMenuItem(text = { Text("Unban", color = GreenCol, fontSize = 13.sp) }, onClick = { showMenu = false; vm.unbanUser(user.id, user.displayName) })
                    } else {
                        DropdownMenuItem(text = { Text("Ban", color = AmberCol, fontSize = 13.sp) }, onClick = { showMenu = false; vm.banUser(user.id, user.displayName) })
                    }
                    user.email?.let { em ->
                        DropdownMenuItem(text = { Text("Reset Password", color = TextCol, fontSize = 13.sp) }, onClick = { showMenu = false; vm.sendPasswordReset(em) })
                    }
                    DropdownMenuItem(text = { Text("Delete", color = RedCol, fontSize = 13.sp) }, onClick = { showMenu = false; vm.deleteUser(user.id, user.displayName) })
                }
            }
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            SDivider()
            Spacer(Modifier.height(8.dp))
            Text("ID: ${user.id}", color = SubText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            user.created_at?.let { Text("Created: ${shortTime(it)}", color = SubText, fontSize = 10.sp) }
            user.last_sign_in_at?.let { Text("Last sign-in: ${shortTime(it)}", color = SubText, fontSize = 10.sp) }
        }
    }
}

// ── SECRETS ───────────────────────────────────────────────────────────────────

@Composable
fun SecretsScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val cache = state.secretsCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadSecrets() }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(state.secretSearch, vm::setSecretSearch, "Search secrets...", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { vm.showAddSecret() }) { Icon(Icons.Default.Add, null, tint = TealCol) }
        }
        if (cache == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }; return@Column }
        cache.error?.let { Box(Modifier.padding(horizontal = 16.dp)) { ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard("/settings/vault") }) } }
        val secrets = (cache.payload ?: emptyList()).filter { state.secretSearch.isBlank() || it.name.contains(state.secretSearch, true) }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(secrets) { s ->
                SCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = PurpleCol, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            s.updated_at?.let { Text("Updated: ${shortTime(it)}", color = SubText, fontSize = 10.sp) }
                        }
                        IconButton(onClick = { vm.deleteSecret(s.name) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Delete, null, tint = RedCol, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            if (secrets.isEmpty() && cache.error == null) item { EmptyState("No secrets found") }
        }
    }
}

// ── STORAGE ───────────────────────────────────────────────────────────────────

@Composable
fun StorageScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val activeBucket = state.activeBucket
    if (activeBucket != null) {
        BucketBrowserScreen(activeBucket, vm, state)
        BackHandler { vm.closeBucket() }
        return
    }
    val cache = state.storageCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadStorage() }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(state.bucketSearch, vm::setBucketSearch, "Search buckets...", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { vm.showCreateBucket() }) { Icon(Icons.Default.Add, null, tint = TealCol) }
        }
        if (cache == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }; return@Column }
        cache.error?.let { Box(Modifier.padding(horizontal = 16.dp)) { ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard("/storage/buckets") }) } }
        val buckets = (cache.payload ?: emptyList()).filter { state.bucketSearch.isBlank() || it.name.contains(state.bucketSearch, true) }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(buckets) { b ->
                SCard {
                    Row(modifier = Modifier.clickable { vm.openBucket(b) }, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = AmberCol, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(b.name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusChip(if (b.public) "Public" else "Private", if (b.public) AmberCol else BlueCol, small = true)
                                Text(b.sizeLimitDisplay, color = SubText, fontSize = 11.sp)
                            }
                        }
                        Row {
                            IconButton(onClick = { vm.emptyBucket(b.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.DeleteSweep, null, tint = AmberCol, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { vm.deleteBucket(b.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Delete, null, tint = RedCol, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            if (buckets.isEmpty() && cache.error == null) item { EmptyState("No buckets found") }
        }
    }
    if (state.showCreateBucket) CreateBucketDialog(vm)
}

@Composable
fun BucketBrowserScreen(bucket: SupabaseBucket, vm: SupabaseViewModel, state: SupabaseUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().background(CardBg2).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.closeBucket() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ArrowBack, null, tint = SubText) }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bucket.name, color = TextCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(state.bucketPath.ifBlank { "/" }, color = SubText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            // Upload button
            IconButton(onClick = {
                // In AndroidIDE, file picker via ActivityResult — trigger snack note for now
                // Full file picker integration requires Activity context; placeholder shown
                vm.snack("Pick a file from your device to upload. (Implement file picker in MainActivity for full upload support.)")
            }) {
                Icon(Icons.Default.Upload, null, tint = TealCol)
            }
        }
        if (state.bucketObjectsLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }
            return@Column
        }
        if (state.isUploading) {
            LinearProgressIndicator(progress = state.uploadProgress, color = TealCol, trackColor = CardBg2, modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.bucketObjects) { obj ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBg)
                        .clickable {
                            if (obj.isFolder) vm.loadBucketObjects(bucket.id, "${state.bucketPath}${obj.name}")
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (obj.isFolder) Icons.Default.Folder else Icons.Default.InsertDriveFile, null, tint = if (obj.isFolder) AmberCol else SubText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(obj.name, color = TextCol, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!obj.isFolder) Text(obj.displaySize, color = SubText, fontSize = 11.sp)
                    }
                    if (!obj.isFolder) {
                        IconButton(onClick = { vm.deleteStorageObject(bucket.id, "${state.bucketPath}${obj.name}") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, null, tint = RedCol, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            if (state.bucketObjects.isEmpty()) item { EmptyState("Bucket is empty") }
        }
    }
}

// ── FUNCTIONS ─────────────────────────────────────────────────────────────────

@Composable
fun FunctionsScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val cache = state.functionsCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadFunctions() }
    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(state.functionSearch, vm::setFunctionSearch, "Search functions...", modifier = Modifier.padding(16.dp))
        if (cache == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }; return@Column }
        cache.error?.let { Box(Modifier.padding(horizontal = 16.dp)) { ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard("/functions") }) } }
        val fns = (cache.payload ?: emptyList()).filter { state.functionSearch.isBlank() || it.displayName.contains(state.functionSearch, true) }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(fns) { fn ->
                SCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fn.displayName, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusChip(fn.status, if (fn.isActive) GreenCol else AmberCol, small = true)
                                StatusChip(if (fn.verify_jwt) "JWT" else "Public", BlueCol, small = true)
                                Text("v${fn.version}", color = SubText, fontSize = 11.sp)
                            }
                        }
                        PrimaryButton("Invoke", onClick = { vm.showInvokeFunction(fn.slug) }, modifier = Modifier.height(34.dp))
                    }
                    fn.updated_at?.let { Spacer(Modifier.height(4.dp)); Text("Updated: ${shortTime(it)}", color = SubText, fontSize = 10.sp) }
                }
            }
            if (fns.isEmpty() && cache.error == null) item { EmptyState("No functions found") }
        }
    }
}

// ── LOGS ──────────────────────────────────────────────────────────────────────

@Composable
fun LogsScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val services = listOf("API Gateway", "Postgres", "PostgREST", "Pooler", "Auth", "Storage", "Realtime", "Edge Functions", "Cron")
    val ranges = listOf("1 Hour", "24 Hours", "7 Days", "30 Days")
    val cache = state.logsCache
    LaunchedEffect(Unit) { if (cache == null) vm.loadLogs() }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(services) { svc ->
                    ClickChip(svc, if (state.logService == svc) TealCol else SubText) {
                        vm.setLogService(svc); vm.loadLogs()
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ranges.forEach { r ->
                    ClickChip(r, if (state.logRange == r) BlueCol else SubText) {
                        vm.setLogRange(r); vm.loadLogs()
                    }
                }
                Spacer(Modifier.weight(1f))
                SecondaryButton("Load", onClick = { vm.loadLogs() })
            }
        }

        if (cache == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TealCol) }; return@Column }

        cache.error?.let {
            Box(Modifier.padding(horizontal = 16.dp)) {
                ErrorBanner(it, onWebFallback = { vm.navigateToWebDashboard("/logs/explorer") })
            }
        }

        cache.payload?.let { payload ->
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("${payload.count ?: payload.records.size} events · ${payload.serviceName} · ${payload.selectedRange}", color = SubText, fontSize = 11.sp)
            }
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(payload.records) { record ->
                    LogRow(record)
                }
                if (payload.records.isEmpty() && cache.error == null) item { EmptyState("No log entries found for this range") }
            }
        }
    }
}

// ── REALTIME ──────────────────────────────────────────────────────────────────

@Composable
fun RealtimeScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Controls
        SCard(modifier = Modifier.padding(16.dp)) {
            SectionHeader("Realtime Listener")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                STextField(state.realtimeSchema, vm::setRealtimeSchema, "Schema", modifier = Modifier.weight(1f), hint = "public")
                STextField(state.realtimeTable, vm::setRealtimeTable, "Table", modifier = Modifier.weight(1f), hint = "my_table")
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.realtimeActive) {
                    LiveDot(GreenCol)
                    Text("Listening…", color = GreenCol, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    DangerButton("Stop", onClick = { vm.stopRealtime() })
                } else {
                    PrimaryButton("▶ Start Listening", onClick = { vm.startRealtime() }, modifier = Modifier.weight(1f))
                    if (state.realtimeEvents.isNotEmpty()) {
                        SecondaryButton("Clear", onClick = { vm.clearRealtimeEvents() })
                    }
                }
            }
            state.realtimeError?.let {
                Spacer(Modifier.height(6.dp))
                ErrorBanner("Connection error: $it")
            }
        }

        InfoBanner("Events appear here in real time. INSERT, UPDATE and DELETE events are captured.", modifier = Modifier.padding(horizontal = 16.dp))

        if (state.realtimeEvents.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                EmptyState("No events yet. Start listening and make changes to your table.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                items(state.realtimeEvents) { event ->
                    RealtimeEventRow(event)
                }
            }
        }
    }
}

// ── CONNECTIONS ───────────────────────────────────────────────────────────────

@Composable
fun ConnectionsScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PrimaryButton("Save Current as Connection", onClick = { vm.showSaveConnection() }, modifier = Modifier.fillMaxWidth())
        }
        if (state.savedConnections.isEmpty()) {
            item { EmptyState("No saved connections. Save your current credentials as a connection.") }
        }
        items(state.savedConnections) { conn ->
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(conn.nickname, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(conn.project_ref.ifBlank { conn.project_url }, color = SubText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Saved: ${conn.savedAt}", color = SubText, fontSize = 10.sp)
                    }
                    Row {
                        PrimaryButton("Load", onClick = { vm.loadSavedConnection(conn) }, modifier = Modifier.height(34.dp))
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = { vm.deleteSavedConnection(conn.id) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Delete, null, tint = RedCol, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── CREDENTIALS ───────────────────────────────────────────────────────────────

@Composable
fun CredentialsScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val d = state.credDraft
    val clipboard = LocalClipboardManager.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    fun paste(field: String) {
        val text = clipboard.getText()?.text?.trim() ?: return
        vm.updateCredDraft(field, text)
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Project Connection", modifier = Modifier.weight(1f))
                    IconButton(onClick = { uriHandler.openUri("https://supabase.com/dashboard/projects") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.OpenInNew, null, tint = SubText, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    STextField(d.project_url, { vm.updateCredDraft("project_url", it) }, "Project URL", modifier = Modifier.weight(1f), hint = "https://xxxx.supabase.co")
                    IconButton(onClick = { paste("project_url") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentPaste, null, tint = TealCol, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    STextField(d.project_ref, { vm.updateCredDraft("project_ref", it) }, "Project Ref", modifier = Modifier.weight(1f), hint = "xxxx (auto-detected)")
                    SecondaryButton("Detect", onClick = { vm.inferRefFromUrl() })
                }
            }
        }
        item {
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("API Keys", modifier = Modifier.weight(1f))
                    IconButton(onClick = { uriHandler.openUri("https://supabase.com/dashboard/project/_/settings/api") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.OpenInNew, null, tint = SubText, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Personal Access Token (PAT)", color = SubText, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { uriHandler.openUri("https://supabase.com/dashboard/account/tokens") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.OpenInNew, null, tint = TealCol, modifier = Modifier.size(12.dp))
                    }
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    STextField(d.personal_access_token, { vm.updateCredDraft("personal_access_token", it) }, "", modifier = Modifier.weight(1f), hint = "sbp_...", isPassword = true)
                    IconButton(onClick = { paste("personal_access_token") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentPaste, null, tint = TealCol, modifier = Modifier.size(16.dp))
                    }
                }
                Text("Required for Management API (projects, functions, secrets, logs)", color = SubText, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Text("Service Role / Admin Key", color = SubText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    STextField(d.project_admin_key, { vm.updateCredDraft("project_admin_key", it) }, "", modifier = Modifier.weight(1f), hint = "eyJ...", isPassword = true)
                    IconButton(onClick = { paste("project_admin_key") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentPaste, null, tint = TealCol, modifier = Modifier.size(16.dp))
                    }
                }
                Text("Required for users, tables, storage, RLS bypass", color = SubText, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Text("Anon Key (optional)", color = SubText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    STextField(d.anon_key, { vm.updateCredDraft("anon_key", it) }, "", modifier = Modifier.weight(1f), hint = "eyJ...", isPassword = true)
                    IconButton(onClick = { paste("anon_key") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentPaste, null, tint = TealCol, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item {
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Auth Credentials (optional)", modifier = Modifier.weight(1f))
                    IconButton(onClick = { uriHandler.openUri("https://supabase.com/dashboard/project/_/auth/users") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.OpenInNew, null, tint = SubText, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                STextField(d.email, { vm.updateCredDraft("email", it) }, "Email", hint = "admin@example.com")
                Spacer(Modifier.height(8.dp))
                STextField(d.password, { vm.updateCredDraft("password", it) }, "Password", isPassword = true)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Save Credentials", onClick = { vm.saveCredentials() }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Test PAT", onClick = { vm.testPat() }, modifier = Modifier.weight(1f), color = BlueCol)
                    SecondaryButton("Test Key", onClick = { vm.testProjectKey() }, modifier = Modifier.weight(1f), color = BlueCol)
                    SecondaryButton("Test Auth", onClick = { vm.testCloudAuth() }, modifier = Modifier.weight(1f), color = BlueCol)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Clear Form", onClick = { vm.clearCredentialDraft() }, modifier = Modifier.weight(1f))
                    DangerButton("Delete All", onClick = { vm.deleteCredentials() }, modifier = Modifier.weight(1f))
                }
            }
        }
        item { InfoBanner("PAT = Management API (projects, secrets, logs). Service key = project data. Never share these keys.", AmberCol) }
    }
}

// ── WEB DASHBOARD ─────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebDashboardScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    val wvCreds = state.webViewCredentials
    var currentUrl by remember { mutableStateOf(state.webViewUrl.ifBlank { "https://supabase.com/dashboard" }) }
    var canGoBack by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // WebView toolbar
        Row(
            modifier = Modifier.fillMaxWidth().background(CardBg2).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { webViewRef?.goBack() }, enabled = canGoBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = if (canGoBack) TextCol else SubText, modifier = Modifier.size(18.dp))
            }
            Text(
                currentUrl.removePrefix("https://"),
                color = SubText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            IconButton(onClick = { webViewRef?.reload() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, null, tint = SubText, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { vm.showWebViewCredDialog() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.AccountCircle, null, tint = if (wvCreds.rememberMe) TealCol else SubText, modifier = Modifier.size(18.dp))
            }
        }

        // Credential hint banner
        if (!wvCreds.rememberMe || wvCreds.email.isBlank()) {
            InfoBanner("Tap the account icon to save Web Dashboard credentials (separate from API credentials).")
        }

        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            currentUrl = url ?: ""
                            canGoBack = view?.canGoBack() ?: false

                            // Auto-fill credentials on Supabase sign-in page if Remember Me is on
                            if (wvCreds.rememberMe && wvCreds.email.isNotBlank() && url?.contains("supabase.com/dashboard/sign-in") == true) {
                                val js = """
                                    (function() {
                                        function setNativeValue(el, value) {
                                            var proto = Object.getPrototypeOf(el);
                                            var descriptor = Object.getOwnPropertyDescriptor(proto, 'value')
                                                || Object.getOwnPropertyDescriptor(el, 'value');
                                            var setter = descriptor && descriptor.set;
                                            if (setter) { setter.call(el, value); } else { el.value = value; }
                                            el.dispatchEvent(new Event('input', { bubbles: true }));
                                            el.dispatchEvent(new Event('change', { bubbles: true }));
                                        }
                                        function fillField(el, value) {
                                            if (!el) return;
                                            el.focus();
                                            try {
                                                document.execCommand('selectAll', false, null);
                                                var ok = document.execCommand('insertText', false, value);
                                                if (!ok || el.value !== value) { setNativeValue(el, value); }
                                            } catch (e) {
                                                setNativeValue(el, value);
                                            }
                                        }
                                        function fillCreds() {
                                            var emailField = document.querySelector('input[type="email"]');
                                            var passField = document.querySelector('input[type="password"]');
                                            if (!emailField || !passField) return false;
                                            if (emailField.value !== '${wvCreds.email.replace("'", "\\'")}') {
                                                fillField(emailField, '${wvCreds.email.replace("'", "\\'")}');
                                            }
                                            if (passField.value !== '${wvCreds.password.replace("'", "\\'")}') {
                                                fillField(passField, '${wvCreds.password.replace("'", "\\'")}');
                                            }
                                            return emailField.value === '${wvCreds.email.replace("'", "\\'")}' &&
                                                   passField.value === '${wvCreds.password.replace("'", "\\'")}';
                                        }
                                        // Keep re-checking for a few seconds in case the form hydrates late
                                        // or React resets an unsynced field after its own validation pass.
                                        var attempts = 0;
                                        var timer = setInterval(function() {
                                            attempts++;
                                            var done = fillCreds();
                                            if ((done && attempts > 3) || attempts > 20) clearInterval(timer);
                                        }, 200);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(js, null)
                            }
                        }
                    }
                    loadUrl(state.webViewUrl.ifBlank { "https://supabase.com/dashboard" })
                    webViewRef = this
                }
            },
            update = { wv ->
                val targetUrl = state.webViewUrl
                if (targetUrl.isNotBlank() && targetUrl != currentUrl) {
                    wv.loadUrl(targetUrl)
                }
                webViewRef = wv
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    BackHandler { webViewRef?.let { if (it.canGoBack()) it.goBack() } }
}

// ── SETTINGS ──────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(vm: SupabaseViewModel, state: SupabaseUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Appearance") }
        item {
            SCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Dark Mode", color = TextCol, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = state.settings.dark_mode, onCheckedChange = { vm.toggleDarkMode() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TealCol))
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Compact Mode", color = TextCol, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = state.settings.compact_mode, onCheckedChange = { vm.toggleCompactMode() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TealCol))
                }
            }
        }
        item { SectionHeader("Performance") }
        item {
            SCard {
                STextField(state.settingsTimeoutDraft, vm::updateTimeoutDraft, "API Timeout (seconds)", hint = "40")
                Spacer(Modifier.height(8.dp))
                STextField(state.settingsPreviewRowsDraft, vm::updatePreviewRowsDraft, "Table Preview Rows", hint = "5")
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Save Settings", onClick = { vm.saveSettings() }, modifier = Modifier.fillMaxWidth())
            }
        }
        item { SectionHeader("Security") }
        item {
            SCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App PIN", color = TextCol, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Currently: ${if (state.settings.app_pin.isNotBlank()) "Set (${state.settings.app_pin.length} chars)" else "Not set"}", color = SubText, fontSize = 11.sp)
                    }
                    SecondaryButton("Change", onClick = { vm.showSetPin() })
                }
            }
        }
        item { SectionHeader("Data") }
        item {
            SCard {
                DangerButton("Clear Session Cache", onClick = { vm.clearCache() }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Clears all cached data without removing credentials.", color = SubText, fontSize = 11.sp)
            }
        }
        item { SectionHeader("About") }
        item {
            SCard {
                Text("Supabase Admin", color = TealCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Module v2.0.0 · Slackline Admin Panel", color = SubText, fontSize = 12.sp)
                Text("Integrated Supabase module for Slackline Admin.", color = SubText, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("Files stored at: Downloads/SLACKLINE ADMIN FILES/SupaBase Data/", color = SubText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ── DIALOGS ───────────────────────────────────────────────────────────────────

@Composable
fun InsertRowDialog(ctx: InsertRowContext, vm: SupabaseViewModel) {
    val insertableCols = ctx.columns.filter { col ->
        col.name !in listOf("id", "created_at", "updated_at") &&
        col.default_value?.contains("gen_random_uuid") != true &&
        col.default_value?.contains("now()") != true
    }.ifEmpty { ctx.columns }

    val values = remember { mutableStateMapOf<String, String>() }

    Dialog(onDismissRequest = { vm.dismissInsertRow() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardBg2, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Insert Row — ${ctx.tableName}", color = TextCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (insertableCols.isEmpty()) {
                    Text("No editable columns detected. All columns appear auto-generated.", color = SubText, fontSize = 13.sp)
                } else {
                    insertableCols.forEach { col ->
                        STextField(
                            value = values[col.name] ?: "",
                            onValueChange = { values[col.name] = it },
                            label = "${col.name} (${col.data_type})${if (!col.is_nullable) " *" else ""}",
                            hint = col.default_value?.let { "Default: $it" } ?: ""
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Cancel", { vm.dismissInsertRow() }, modifier = Modifier.weight(1f))
                    PrimaryButton("Insert", modifier = Modifier.weight(1f), onClick = {
                        val payload = values.filter { it.value.isNotBlank() }
                        vm.executeInsertRow(ctx.tableName, payload)
                    })
                }
            }
        }
    }
}

@Composable
fun UpdateRowDialog(ctx: UpdateRowContext, vm: SupabaseViewModel) {
    var matchCol by remember { mutableStateOf("id") }
    var matchVal by remember { mutableStateOf("") }
    var payload by remember { mutableStateOf("{}") }

    Dialog(onDismissRequest = { vm.dismissUpdateRow() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardBg2, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Update Row — ${ctx.tableName}", color = TextCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                InfoBanner("Match column: ${ctx.pkGuess.ifBlank { "id" }}")
                STextField(matchCol, { matchCol = it }, "Match Column", hint = ctx.pkGuess.ifBlank { "id" })
                STextField(matchVal, { matchVal = it }, "Match Value", hint = "The value to match on")
                STextField(payload, { payload = it }, "JSON Payload", hint = "{\"name\": \"new value\"}", singleLine = false)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Cancel", { vm.dismissUpdateRow() }, modifier = Modifier.weight(1f))
                    PrimaryButton("Update", modifier = Modifier.weight(1f), onClick = {
                        val parsed = try { com.google.gson.Gson().fromJson(payload, Map::class.java) as? Map<String, Any> ?: emptyMap() } catch (_: Exception) { emptyMap<String, Any>() }
                        vm.executeUpdateRow(ctx.tableName, matchCol.ifBlank { ctx.pkGuess.ifBlank { "id" } }, matchVal, parsed)
                    })
                }
            }
        }
    }
}

@Composable
fun CreateUserDialog(vm: SupabaseViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { vm.dismissCreateUser() },
        containerColor = CardBg2,
        title = { Text("Create User", color = TextCol, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                STextField(email, { email = it }, "Email", hint = "user@example.com")
                STextField(password, { password = it }, "Password", isPassword = true, hint = "Min 6 characters")
            }
        },
        confirmButton = { PrimaryButton("Create", onClick = { vm.createUser(email, password) }) },
        dismissButton = { SecondaryButton("Cancel", onClick = { vm.dismissCreateUser() }) }
    )
}

@Composable
fun AddSecretDialog(vm: SupabaseViewModel) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { vm.dismissAddSecret() },
        containerColor = CardBg2,
        title = { Text("Add Secret", color = TextCol, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                STextField(name, { name = it }, "Secret Name", hint = "MY_SECRET")
                STextField(value, { value = it }, "Secret Value", isPassword = true)
            }
        },
        confirmButton = { PrimaryButton("Save", onClick = { vm.createSecret(name, value) }) },
        dismissButton = { SecondaryButton("Cancel", onClick = { vm.dismissAddSecret() }) }
    )
}

@Composable
fun FunctionInvokerDialog(state: SupabaseUiState, vm: SupabaseViewModel) {
    var body by remember { mutableStateOf("{}") }
    AlertDialog(
        onDismissRequest = { vm.dismissFunctionInvoker() },
        containerColor = CardBg2,
        title = { Text("Invoke: ${state.activeFunctionSlug}", color = TextCol, fontWeight = FontWeight.Bold) },
        text = { STextField(body, { body = it }, "Request Body (JSON)", hint = "{}", singleLine = false) },
        confirmButton = { PrimaryButton("Invoke", onClick = { vm.invokeFunction(state.activeFunctionSlug, body) }) },
        dismissButton = { SecondaryButton("Cancel", onClick = { vm.dismissFunctionInvoker() }) }
    )
}

@Composable
fun SaveSnippetDialog(vm: SupabaseViewModel, state: SupabaseUiState) {
    AlertDialog(
        onDismissRequest = { vm.dismissSaveSnippetDialog() },
        containerColor = CardBg2,
        title = { Text("Save Query as Snippet", color = TextCol, fontWeight = FontWeight.Bold) },
        text = { STextField(state.snippetNameDraft, vm::updateSnippetNameDraft, "Snippet Name", hint = "e.g. Active Users") },
        confirmButton = { PrimaryButton("Save", onClick = { vm.saveCurrentQueryAsSnippet() }) },
        dismissButton = { SecondaryButton("Cancel", onClick = { vm.dismissSaveSnippetDialog() }) }
    )
}

@Composable
fun ActionDialog(action: PendingAction, vm: SupabaseViewModel) {
    ConfirmDialog(
        title = action.title,
        message = "Are you sure you want to ${action.title.lowercase()}?",
        confirmText = action.title,
        onConfirm = { vm.executeAction(action, action.payloadTemplate) },
        onDismiss = { vm.dismissActionDialog() }
    )
}

@Composable
fun CsvExportDialog(state: SupabaseUiState, vm: SupabaseViewModel) {
    AlertDialog(
        onDismissRequest = { vm.dismissCsvExport() },
        containerColor = CardBg2,
        title = { Text("Export CSV — ${state.csvExportTableName}", color = TextCol, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Row limit:", color = SubText, fontSize = 12.sp)
                listOf(1000, 10000, 50000).forEach { limit ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { vm.setCsvExportLimit(limit) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = state.csvExportLimit == limit, onClick = { vm.setCsvExportLimit(limit) }, colors = RadioButtonDefaults.colors(selectedColor = TealCol))
                        Text("$limit rows", color = TextCol, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { PrimaryButton("Export", onClick = { vm.exportTableCsv(state.csvExportTableName, state.csvExportLimit) }) },
        dismissButton = { SecondaryButton("Cancel", onClick = { vm.dismissCsvExport() }) }
    )
}

@Composable
fun TablePreviewDialog(preview: TablePreviewDialog, vm: SupabaseViewModel) {
    Dialog(onDismissRequest = { vm.dismissTablePreview() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardBg2, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Preview — ${preview.key}", color = TextCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                val rows = preview.rows.filterIsInstance<Map<String, Any>>()
                if (rows.isNotEmpty()) {
                    val cols = rows.first().keys.toList()
                    Box(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                        DataGrid(columns = cols, rows = rows)
                    }
                } else {
                    Text("No rows to preview.", color = SubText, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Close", onClick = { vm.dismissTablePreview() }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun SaveConnectionDialog(state: SupabaseUiState, vm: SupabaseViewModel) {
    AlertDialog(
        onDismissRequest = { vm.dismissSaveConnection() },
        containerColor = CardBg2,
        title = { Text("Save Connection", color = TextCol, fontWeight = FontWeight.Bold) },
        text = { STextField(state.connectionNicknameDraft, vm::updateConnectionNickname, "Nickname", hint = state.cfg.currentRef.ifBlank { "My Project" }) },
        confirmButton = { PrimaryButton("Save", onClick = { vm.saveCurrentAsConnection() }) },
        dismissButton = { SecondaryButton("Cancel", onClick = { vm.dismissSaveConnection() }) }
    )
}

@Composable
fun SetPinDialog(vm: SupabaseViewModel) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { vm.dismissSetPin() },
        containerColor = CardBg2,
        title = { Text("Change PIN", color = TextCol, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                STextField(pin, { pin = it }, "New PIN (leave blank to remove)", isPassword = true)
                Text("Min 4 characters to set a PIN.", color = SubText, fontSize = 11.sp)
            }
        },
        confirmButton = { PrimaryButton("Set", onClick = { if (pin.isBlank() || pin.length >= 4) { vm.setPin(pin); vm.dismissSetPin() } }) },
        dismissButton = { SecondaryButton("Cancel", onClick = { vm.dismissSetPin() }) }
    )
}

@Composable
fun WebViewCredDialog(vm: SupabaseViewModel, state: SupabaseUiState) {
    val d = state.webViewCredDraft
    Dialog(onDismissRequest = { vm.dismissWebViewCredDialog() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardBg2, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Web Dashboard Credentials", color = TextCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                InfoBanner("These are only for the Supabase web dashboard login. Completely separate from your API credentials.")
                STextField(d.email, { vm.updateWebViewCredDraft("email", it) }, "Email", hint = "admin@example.com")
                STextField(d.password, { vm.updateWebViewCredDraft("password", it) }, "Password", isPassword = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = d.rememberMe,
                        onCheckedChange = { vm.updateWebViewCredDraft("rememberMe", it.toString()) },
                        colors = CheckboxDefaults.colors(checkedColor = TealCol)
                    )
                    Text("Remember Me (auto-fill on login page)", color = TextCol, fontSize = 13.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DangerButton("Clear", onClick = { vm.clearWebViewCredentials() }, modifier = Modifier.weight(1f))
                    SecondaryButton("Cancel", onClick = { vm.dismissWebViewCredDialog() }, modifier = Modifier.weight(1f))
                    PrimaryButton("Save", onClick = { vm.saveWebViewCredentials() }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GlobalSearchDialog(vm: SupabaseViewModel, state: SupabaseUiState) {
    Dialog(onDismissRequest = { vm.dismissGlobalSearch() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardBg2, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SearchField(state.globalSearchQuery, vm::updateGlobalSearch, "Search tables, users, functions...")
                Spacer(Modifier.height(10.dp))
                if (state.globalSearchResults.isEmpty() && state.globalSearchQuery.length >= 2) {
                    Text("No results found.", color = SubText, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.globalSearchResults) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { vm.navigateFromSearch(result) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, color) = when (result.type) {
                                "table"    -> Icons.Default.TableRows to TealCol
                                "user"     -> Icons.Default.Person to BlueCol
                                "function" -> Icons.Default.Bolt to AmberCol
                                "secret"   -> Icons.Default.Lock to PurpleCol
                                "bucket"   -> Icons.Default.Cloud to GreenCol
                                else       -> Icons.Default.Search to SubText
                            }
                            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(result.label, color = TextCol, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(result.subtitle, color = SubText, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = SubText, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (state.globalSearchQuery.length < 2) {
                    Text("Type at least 2 characters to search across all entities.", color = SubText, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                Spacer(Modifier.height(6.dp))
                SecondaryButton("Close", onClick = { vm.dismissGlobalSearch() }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun CreateBucketDialog(vm: SupabaseViewModel) {
    var name by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { vm.dismissCreateBucket() },
        containerColor = CardBg2,
        title = { Text("Create Bucket", color = TextCol, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                STextField(name, { name = it }, "Bucket Name", hint = "my-bucket")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it }, colors = CheckboxDefaults.colors(checkedColor = AmberCol))
                    Text("Public bucket (anyone can read)", color = TextCol, fontSize = 13.sp)
                }
            }
        },
        confirmButton = { PrimaryButton("Create", onClick = { vm.createBucket(name, isPublic) }) },
        dismissButton = { SecondaryButton("Cancel", onClick = { vm.dismissCreateBucket() }) }
    )
}


// ── Schema Dump Panel ─────────────────────────────────────────────────────────
@Composable
fun SchemaDumpPanel(state: SupabaseUiState, vm: SupabaseViewModel) {
    val clipboard = LocalClipboardManager.current
    val sections = listOf(
        "All","Database Info","Schemas","Tables","Columns","Primary Keys","Foreign Keys",
        "Indexes","Views","Functions & Procedures","Triggers","RLS Policies",
        "Row Level Security Status","Enums (Custom Types)","Extensions",
        "Auth Users Summary","Storage Buckets","Cron Jobs","Table Sizes"
    )
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SCard {
                SectionHeader("Schema Dump")
                Spacer(Modifier.height(4.dp))
                Text("Pulls complete project metadata — tables, columns, keys, indexes, views, functions, triggers, RLS policies, enums, extensions, auth, storage and more.", color = SubText, fontSize = 12.sp)
            }
        }
        item {
            SCard2 {
                Text("Section", color = SubText, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sections) { s ->
                        val active = s == state.schemaDumpSection
                        Surface(onClick = { vm.setSchemaDumpSection(s) }, color = if (active) TealCol else CardBg2, shape = RoundedCornerShape(6.dp)) {
                            Text(s, color = if (active) Color.Black else SubText, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(if (state.schemaDumpRunning) "Running…" else "▶  Run Dump", { vm.runSchemaDump() }, Modifier.weight(1f), enabled = !state.schemaDumpRunning)
                    SecondaryButton("Clear", { vm.clearSchemaDump() }, Modifier.weight(1f))
                }
                if (state.schemaDump.isNotBlank() && !state.schemaDumpRunning) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton("Copy All", { clipboard.setText(AnnotatedString(state.schemaDump)) }, Modifier.weight(1f), color = BlueCol, textColor = Color.White)
                        PrimaryButton("Export .sql", { vm.exportSchemaDump() }, Modifier.weight(1f), color = PurpleCol, textColor = Color.White)
                    }
                    Spacer(Modifier.height(6.dp))
                    PrimaryButton(
                        if (state.csvExportRunning) "Exporting CSV…" else "Export CSV (per section)",
                        onClick = { vm.exportSchemaDumpCsv() },
                        modifier = Modifier.fillMaxWidth(),
                        color = GreenCol,
                        textColor = Color.White,
                        enabled = !state.csvExportRunning
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${state.schemaDump.length} chars · ${state.schemaDump.lines().size} lines · Saved to Downloads/SLACKLINE ADMIN FILES/SupaBase Data/Schema Dump Files/", color = SubText, fontSize = 10.sp)
                }
            }
        }
        if (state.schemaDumpRunning) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = TealCol, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Running dump queries…", color = SubText, fontSize = 13.sp)
                }
            }
        }
        if (state.schemaDump.isNotBlank() && !state.schemaDumpRunning) {
            item {
                SCard2 {
                    SectionHeader("Dump Output")
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 600.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF030303)).padding(12.dp).verticalScroll(rememberScrollState())) {
                        Text(state.schemaDump, color = TealCol, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

// ── Feature list dialog ───────────────────────────────────────────────────────
@Composable
fun FeatureListDialog(onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg2,
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, null, tint = TealCol, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Feature List & Guide", color = TealCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { clipboard.setText(AnnotatedString(FEATURE_LIST_TEXT)); copied = true },
                        color = if (copied) GreenCol.copy(0.15f) else BlueCol.copy(0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, null, tint = if (copied) GreenCol else BlueCol, modifier = Modifier.size(14.dp))
                            Text(if (copied) "Copied!" else "Copy All", color = if (copied) GreenCol else BlueCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text("${FEATURE_LIST_TEXT.lines().size} lines", color = SubText, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
                }
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 520.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF030303)).padding(12.dp).verticalScroll(rememberScrollState())) {
                    Text(FEATURE_LIST_TEXT, color = TealCol, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 17.sp)
                }
            }
        },
        confirmButton = { PrimaryButton("Close", onDismiss) }
    )
}

private val FEATURE_LIST_TEXT = """
SupaAdmin Module — Slackline Admin Panel
Supabase Admin module integrated into Slackline.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  HOW TO START A SESSION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Step 1 — Add credentials (More → Credentials)
  • Project URL  → Your project's Supabase URL
  • PAT          → Settings > Access Tokens on supabase.com
  • Service Key  → Project Settings > API (service_role key)
  • Anon Key     → Project Settings > API (anon key — optional)
  Tap "Detect" to fill Project Ref from URL automatically.
  Tap paste icons to paste from clipboard.
  Tap "Save Credentials" when done.

Step 2 — Test your keys
  • Tap "Test PAT"  → should return project count
  • Tap "Test Key"  → should return user count
  Both green snacks = you are good to go.

Step 3 — Navigate
  Bottom nav: Home · Database · Auth · DevTools · More
  Sub-tabs appear at top of each section.
  Data auto-loads on first visit to each screen.
  Tap Refresh (↻) to reload at any time.
  Android back from any section → Home.
  Android back on Home → Exit prompt.

Step 4 — Save your connection
  More → Connections → "Save Current as Connection"
  Give it a nickname. Switch projects anytime by loading a saved connection.

Step 5 — Set a PIN (optional)
  More → Settings → Change PIN → 4-digit numpad lock on every launch.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  CREDENTIALS REQUIRED PER FEATURE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  PAT only:
    Projects, Functions, Secrets, Migrations, Logs, Schema Dump

  Service Key only:
    Users, Tables, Storage, SQL Editor, RLS Policies, Cron Jobs, Webhooks

  Both PAT + Service Key:
    Overview (full), Usage metrics, Dashboard stats

  Anon Key + Email/Password (optional):
    Cloud Auth test only · Web Dashboard auto-fill

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ALL FEATURES — SCREEN BY SCREEN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[ HOME → DASHBOARD ]
  • Project stat cards (Tables, Users, Functions, Buckets)
  • Quick action chips (SQL, Users, Logs, Realtime)
  • Feature list & session guide (this screen)
  • Recent tables list with tap-to-open
  • Web Dashboard shortcut card

[ HOME → OVERVIEW ]
  • Live project name, status, region, plan
  • Stat grid: tables, users, functions, buckets, secrets, projects
  • Load warnings shown inline

[ HOME → PROJECTS ]
  • All Supabase projects on your PAT
  • "Use" button sets active project instantly

[ HOME → USAGE ]
  • DB size, Storage size, MAU, 3rd-party MAU
  • API requests, Edge invocations
  • Bar charts for user metrics (MPAndroidChart)
  • Plan name and region

[ DATABASE → TABLES ]
  • Full table list, filter by schema, search
  • Tap table → full data grid view
    — Sortable columns (tap header)
    — Paginated rows (50 at a time, load more)
    — Insert Row button (NEW)
    — Export CSV button
  • Preview dialog, Export CSV, Open in SQL

[ DATABASE → SQL EDITOR ]
  • Multi-line SQL editor, green-on-black
  • Run any SQL, results shown as data grid
  • Tap any cell to copy value
  • Query history (last 20, tap to reload)
  • Named saved snippets (save + load + delete)
  • Export results as CSV
  • Toggle to Schema Dump mode (see below)

[ DATABASE → SQL → SCHEMA DUMP ]
  • 19 selectable sections or run ALL at once
  • Full output in scrollable code viewer
  • Copy All to clipboard in one tap
  • Export as .sql file to
    Downloads/SLACKLINE ADMIN FILES/SupaBase Data/Schema Dump Files/

[ DATABASE → RLS POLICIES ]
  • Full policy list: schema, table, command, roles
  • USING and WITH CHECK clauses shown
  • Enabled/disabled status chip

[ DATABASE → MIGRATIONS ]
  • Full migration history via Management API
  • Version, name, timestamp, status chip

[ DATABASE → CRON JOBS ]
  • pg_cron job list: name, schedule, command
  • Enable/disable toggle per job
  • Create new job (name + cron expression + SQL)
  • Delete job

[ DATABASE → WEBHOOKS ]
  • supabase_functions.hooks list
  • Table, schema, events (INSERT/UPDATE/DELETE)
  • Service URL shown, active/inactive chip
  • Delete webhook

[ AUTH → USERS ]
  • Full paginated user list (up to 500)
  • Search by email or phone
  • Create / Delete / Ban / Unban / Reset password
  • Provider chip, confirmed status, banned status
  • Expand card for full detail + timestamps

[ AUTH → SECRETS ]
  • Full secret list (names + digests)
  • Add new secret · Delete secret
  • Updated-at timestamp per secret

[ DEVTOOLS → STORAGE ]
  • Full bucket list, search, create, empty, delete
  • Browse bucket: files, folders, sizes
  • Delete individual objects
  • Upload file (requires file picker integration)
  • Navigate into folders

[ DEVTOOLS → EDGE FUNCTIONS ]
  • Full function list, search
  • Status chip, JWT verification chip, version
  • Invoke with custom JSON body

[ DEVTOOLS → LOGS ]
  • 9 services: API Gateway, Postgres, PostgREST,
    Pooler, Auth, Storage, Realtime, Edge Functions, Cron
  • 4 time ranges: 1h, 24h, 7d, 30d
  • Structured rows: timestamp + level badge + message
  • Color-coded: INFO (blue), WARN (orange), ERROR (red)
  • Tap any log row to copy message

[ DEVTOOLS → REALTIME ]
  • WebSocket listener for any table
  • Live INSERT / UPDATE / DELETE event feed
  • Event type chip, timestamp, JSON payload
  • Start / Stop / Clear events
  • Last 100 events kept in session

[ MORE → CONNECTIONS ]
  • Save current credentials with a nickname
  • One-tap load to switch projects instantly
  • Delete individual saved connections
  • Unlimited saved connections

[ MORE → CREDENTIALS ]
  • Enter Project URL, PAT, Service Key, Anon Key, Email, Password
  • Paste button per field (from clipboard)
  • Redirect links → open Supabase dashboard pages directly
  • Auto-detect Project Ref from URL
  • Test PAT / Test Key / Test Auth buttons
  • Save, clear form, delete all

[ MORE → WEB DASHBOARD ]
  • Full Supabase dashboard in WebView
  • Desktop-mode user agent for proper rendering
  • Remember Me: auto-fills login on supabase.com/dashboard/sign-in
  • Web credentials stored separately from API credentials
  • Back button navigates browser history
  • Per-screen deep links from error banners

[ MORE → SETTINGS ]
  • Request timeout (10–120 seconds)
  • Table preview row count (1–20)
  • Dark / Light mode toggle
  • Compact mode toggle
  • 4-digit PIN numpad lock (set/change/remove)
  • Clear session cache

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  NAVIGATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  • Bottom nav: Home · Database · Auth · DevTools · More
  • Sub-nav chips switch screens within each section
  • Android back → Home section from any section
  • Android back on Home → exit confirmation dialog
  • Table detail + bucket browser have their own ← button
  • Global search: tap 🔍 in header or tap Home tab again
  • All data cached in session memory

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  DATA & SECURITY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  Files in Downloads/SLACKLINE ADMIN FILES/SupaBase Data/:
    supabase_config.json    — API credentials (SLACKLINE ADMIN FILES/SupaBase Data/)
    supabase_settings.json  — App settings + PIN
    saved_connections.json  — Saved project connections
    webview_credentials.json — Web Dashboard login (separate)
    sql_snippets.json       — Saved SQL queries
    Schema Dump Files/      — Exported .sql dumps (SLACKLINE ADMIN FILES/SupaBase Data/Schema Dump Files/)

  All API calls go directly to Supabase REST + Management APIs.
  No third-party servers. No telemetry.
  Built with Kotlin + Jetpack Compose + OkHttp + MPAndroidChart.
  Developed with AndroidIDE on-device.
""".trimIndent()
