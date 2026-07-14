package com.example.slacklineadminapp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.slacklineadminapp.data.KeyRestorer
import com.example.slacklineadminapp.data.SecurityConfig
import com.example.slacklineadminapp.data.RequestSyncWorker
import com.example.slacklineadminapp.navigation.Routes
import com.example.slacklineadminapp.ui.components.ActionButton
import com.example.slacklineadminapp.ui.components.BodyText
import com.example.slacklineadminapp.ui.components.SectionLabel
import com.example.slacklineadminapp.ui.screens.*
import com.example.slacklineadminapp.ui.screens.shvstore.*
import com.example.slacklineadminapp.ui.theme.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ComposeEmptyActivity)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestStorage()
        Thread { KeyRestorer.restoreAllIfNeeded() }.start()

        // ── Initialize Background Sync for Push Notifications ──
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<RequestSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "SupabaseRequestSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        val initialCfg = SecurityConfig.get(this)

        setContent {
            val cfg by SecurityConfig.getFlow(this).collectAsState(initial = initialCfg)
            SlackLineTheme(darkTheme = cfg.theme != "Light") {
                SlackLineApp(
                    startLocked = cfg.appPin.isNotEmpty(),
                    companyName = cfg.companyName,
                    onExit      = { finish() }
                )
            }
        }
    }

    private fun requestStorage() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        // Request Notification permission for Android 13+ along with storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
            // Even if we have MANAGE_APP_ALL_FILES, we still need to ask for Notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        } else {
            permLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

@Composable
fun SlackLineApp(startLocked: Boolean, companyName: String, onExit: () -> Unit) {
    val nav = rememberNavController()

    val startDest = if (startLocked) Routes.LOCK_SCREEN else Routes.DASHBOARD

    NavHost(navController = nav, startDestination = startDest) {

        composable(Routes.LOCK_SCREEN) {
            LockScreen {
                nav.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.LOCK_SCREEN) { inclusive = true }
                }
            }
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                companyName = companyName,
                onNavigate  = { route ->
                    when (route) {
                        "legacy"               -> nav.navigate(Routes.LEGACY_LICENSE)
                        "new_license"          -> nav.navigate(Routes.NEW_LICENSE)
                        "license_tools"        -> nav.navigate(Routes.LICENSE_TOOLS)
                        "cloud_settings"       -> nav.navigate(Routes.CLOUD_SETTINGS)
                        "settings"             -> nav.navigate(Routes.SETTINGS)
                        "github"               -> nav.navigate(Routes.GITHUB_MANAGER)
                        "supabase"             -> nav.navigate(Routes.SUPABASE_ADMIN)
                        "activity_log"         -> nav.navigate(Routes.ACTIVITY_LOG)
                        "customers"            -> nav.navigate(Routes.CUSTOMERS)
                        "workflow"             -> nav.navigate(Routes.WORKFLOW_GUIDE)
                        "websites"             -> nav.navigate(Routes.WEBSITES)
                        "documents"            -> nav.navigate(Routes.DOCUMENTS)
                        "blueprints"           -> nav.navigate(Routes.APP_BLUEPRINTS)
                        "kotlin_tool"          -> nav.navigate(Routes.KOTLIN_LICENSE_TOOL)
                        "invoice_maker"        -> nav.navigate(Routes.INVOICE_MAKER)
                        "website_admin"        -> nav.navigate(Routes.COMPANY_WEBSITE_ADMIN)
                        "domain_management"    -> nav.navigate(Routes.DOMAIN_MANAGEMENT)
                        "project_management"   -> nav.navigate(Routes.PROJECT_MANAGEMENT)
                        "business_calendar"    -> nav.navigate("business_calendar")
                        "client_service_requests" -> nav.navigate("client_service_requests") 
                        "kotlin_apps_manager"  -> nav.navigate("kotlin_apps_manager")
                        "kotlin_app_injector"  -> nav.navigate("kotlin_app_injector")
                        "kotlin_app_generator" -> nav.navigate("kotlin_app_generator")
                        "kotlin_app_blueprints" -> nav.navigate(Routes.KOTLIN_APP_BLUEPRINTS)
                        "shv_store_admin"      -> nav.navigate("shv_store_admin")
                    }
                },
                onExit = onExit
            )
        }

        composable(Routes.LEGACY_LICENSE) {
            LegacyLicenseScreen(
                onNavigateBack = { nav.popBackStack() },
                onOpenProduct  = { id -> nav.navigate(Routes.productManager(id)) },
                onAddProduct   = { nav.navigate(Routes.ADD_PRODUCT) }
            )
        }

        composable(Routes.ADD_PRODUCT) {
            AddProductScreen(
                onNavigateBack = { nav.popBackStack() },
                onDone         = { nav.popBackStack(Routes.LEGACY_LICENSE, inclusive = false) }
            )
        }

        composable(
            route     = Routes.PRODUCT_MANAGER,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { back ->
            val pid = back.arguments?.getString("productId") ?: return@composable
            ProductManagerScreen(productId = pid, onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.NEW_LICENSE) {
            NewLicenseManagerScreen(
                onNavigateBack = { nav.popBackStack() },
                onOpenProduct  = { id -> nav.navigate(Routes.newProductManager(id)) }
            )
        }

        composable(
            route     = Routes.NEW_PRODUCT_MANAGER,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { back ->
            val pid = back.arguments?.getString("productId") ?: return@composable
            NewProductManagerScreen(productId = pid, onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.LICENSE_TOOLS) {
            LicenseToolsScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { nav.popBackStack() },
                onNavigate     = { route -> nav.navigate(route) }
            )
        }

        composable(Routes.CLOUD_SETTINGS) {
            CloudSettingsScreen(
                onNavigateBack        = { nav.popBackStack() },
                onOpenMainBackup      = { nav.navigate(Routes.MAIN_BACKUP) },
                onOpenUniversalBackup = { nav.navigate(Routes.UNIVERSAL_BACKUP) },
                onOpenAppBackup       = { nav.navigate(Routes.APP_BACKUP) }
            )
        }

        composable(Routes.MAIN_BACKUP) {
            PlaceholderScreen("Main Universal Backup") { nav.popBackStack() }
        }

        composable(Routes.UNIVERSAL_BACKUP) {
            PlaceholderScreen("Universal Backup (Non-Licensing)") { nav.popBackStack() }
        }

        composable(Routes.APP_BACKUP) {
            AppBackupScreen(
                onNavigateBack  = { nav.popBackStack() },
                onOpenRecycleBin = { nav.navigate(Routes.RECYCLE_BIN) }
            )
        }

        composable(Routes.RECYCLE_BIN) {
            RecycleBinScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.GITHUB_MANAGER) {
            GitHubManagerScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.SUPABASE_ADMIN) {
            SupabaseAdminScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.ACTIVITY_LOG) {
            ActivityLogScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.CUSTOMERS) {
            CustomersDirectoryScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.WORKFLOW_GUIDE) {
            WorkflowGuideScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.WEBSITES) {
            WebsitesRegistryScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.DOCUMENTS) {
            DocumentsScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.APP_BLUEPRINTS) {
            AppBlueprintsScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable("kotlin_app_generator") {
            KotlinAppGeneratorScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.KOTLIN_LICENSE_TOOL) {
            KotlinLicenseToolScreen(onNavigateBack = { nav.popBackStack() })
        }

        // Updated Route to point to the new AdvancedInvoiceScreen
        composable(Routes.INVOICE_MAKER) {
            AdvancedInvoiceScreen(onBackClicked = { nav.popBackStack() })
        }

        composable(Routes.COMPANY_WEBSITE_ADMIN) {
            CompanyWebsiteAdminScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.DOMAIN_MANAGEMENT) {
            DomainManagementScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(Routes.KOTLIN_APP_BLUEPRINTS) {
            KotlinAppBlueprintsScreen(onNavigateBack = { nav.popBackStack() })
        }        
        
        composable(Routes.PROJECT_MANAGEMENT) {
            ProjectManagementScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable("business_calendar") {
            BusinessCalendarScreen(
                onNavigateBack = { nav.popBackStack() },
                onEditTaskRequested = { taskId ->
                    nav.navigate("edit_task_route/$taskId")
                }
            )
        }

        composable(
            route = "edit_task_route/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("taskId").orEmpty()
            PlaceholderScreen("Edit Task Node (ID: $id)") { nav.popBackStack() }
        }

        composable("kotlin_apps_manager") {
            KotlinLicenseHubScreen(
                onNavigateToProduct = { productId ->
                    nav.navigate("kotlin_product_manager/$productId")
                },
                onNavigateBack = { nav.popBackStack() }
            )
        }

        composable(
            route = "kotlin_product_manager/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { back ->
            val pid = back.arguments?.getString("productId") ?: return@composable
            KotlinProductManagerScreen(
                productId = pid,
                onNavigateBack = { nav.popBackStack() }
            )
        }

        composable("kotlin_app_injector") {
            KotlinAppInjectorScreen(onNavigateBack = { nav.popBackStack() })
        }

        // ── SHV Store Admin Module ────────────────────────────────────────────
        composable("shv_store_admin") {
            StoreAdminHubScreen(
                onNavigate     = { route -> nav.navigate("store_$route") },
                onNavigateBack = { nav.popBackStack() }
            )
        }
        composable("store_store_apps") {
            AdminAppsScreen(
                onEdit = { id -> nav.navigate("store_app_form/$id") },
                onAdd  = { nav.navigate("store_app_form/new") }
            )
        }
        composable("store_store_news") {
            AdminNewsScreen(
                onEdit = { id -> nav.navigate("store_news_form/$id") },
                onAdd  = { nav.navigate("store_news_form/new") }
            )
        }
        composable("store_store_broadcasts") {
            AdminBroadcastsScreen()
        }
        composable("store_store_updates") {
            AdminUpdatesScreen(
                onEdit = { id -> nav.navigate("store_update_form/$id") },
                onAdd  = { nav.navigate("store_update_form/new") }
            )
        }
        composable("store_store_users") {
            AdminUsersScreen(onBack = { nav.popBackStack() })
        }
        composable("store_store_contact") {
            AdminContactScreen(onDone = { nav.popBackStack() })
        }
        composable("store_store_download_links") {
            AdminDownloadLinksScreen(onBack = { nav.popBackStack() })
        }
        composable("store_store_update_form") {
            AdminStoreUpdateFormScreen(onDone = { nav.popBackStack() })
        }
        composable(
            route = "store_app_form/{appId}",
            arguments = listOf(navArgument("appId") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("appId") ?: return@composable
            AdminAppFormScreen(
                appId  = id,
                onDone = { nav.popBackStack() }
            )
        }
        composable(
            route = "store_news_form/{newsId}",
            arguments = listOf(navArgument("newsId") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("newsId") ?: return@composable
            AdminNewsFormScreen(
                newsId = id,
                onDone = { nav.popBackStack() }
            )
        }
        composable("store_broadcast_form/new") {
            AdminBroadcastFormScreen(onDone = { nav.popBackStack() })
        }
        
        // ── New Module Route ──
        composable("client_service_requests") {
            ClientServiceRequestsScreen(onNavigateBack = { nav.popBackStack() })
        }

        composable(
            route = "store_update_form/{updateId}",
            arguments = listOf(navArgument("updateId") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("updateId") ?: return@composable
            AdminUpdateFormScreen(
                updateId = id,
                onDone   = { nav.popBackStack() }
            )
        }
        composable("store_config") {
            StoreConfigScreen(onNavigateBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionLabel(title)
        BodyText("Coming in Phase 2.")
        ActionButton("Back") { onBack() }
    }
}
