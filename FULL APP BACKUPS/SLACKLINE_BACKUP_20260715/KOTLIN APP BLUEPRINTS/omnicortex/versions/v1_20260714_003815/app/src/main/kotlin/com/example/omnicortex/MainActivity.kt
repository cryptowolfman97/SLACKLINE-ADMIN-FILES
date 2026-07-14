package com.example.omnicortex

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.omnicortex.data.prefs.AegisPreferences
import com.example.omnicortex.navigation.Routes
import com.example.omnicortex.ui.screens.*
import com.example.omnicortex.ui.theme.*
import com.example.omnicortex.viewmodel.HomeViewModel
import com.example.omnicortex.viewmodel.PrivacyShieldViewModel
import com.example.omnicortex.vpn.PermissionHelper
import com.example.omnicortex.vpn.WarpVpnEngine
import com.example.omnicortex.license.LicenseState
import androidx.lifecycle.lifecycleScope

class MainActivity : FragmentActivity() { // CHANGED: Must be FragmentActivity for BiometricPrompt

    private var privacyShieldViewModel: PrivacyShieldViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // License gate: startup check + recurring check every
        // LicenseGateConfig.PERIODIC_CHECK_MINUTES. Purely additive —
        // does not affect PIN lock, nav, or any existing screen.
        LicenseState.start(applicationContext, lifecycleScope)
        val initialPin       = AegisPreferences.getAppPin(this)
        val initialBiometric = AegisPreferences.getBiometric(this)
        setContent {
            ComposeEmptyActivityTheme {
                val psViewModel: PrivacyShieldViewModel = viewModel()
                val homeViewModel: HomeViewModel = viewModel()
                LaunchedEffect(psViewModel) { privacyShieldViewModel = psViewModel }

                OmniCortexNavHost(
                    startLocked            = initialPin.isNotEmpty(),
                    initialPin             = initialPin,
                    biometricEnabled       = initialBiometric,
                    privacyShieldViewModel = psViewModel,
                    homeViewModel          = homeViewModel
                )
            }
        }
    }

    @Deprecated("Required for VpnService.prepare() and Settings intent flows on all API levels")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            WarpVpnEngine.VPN_PERMISSION_REQUEST_CODE  -> privacyShieldViewModel?.onVpnPermissionResult(resultCode)
            PermissionHelper.REQUEST_OVERLAY,
            PermissionHelper.REQUEST_BATTERY           -> privacyShieldViewModel?.refreshPermissions(this)
        }
    }
}

@Composable
fun OmniCortexNavHost(
    startLocked: Boolean,
    initialPin: String,
    biometricEnabled: Boolean,
    privacyShieldViewModel: PrivacyShieldViewModel,
    homeViewModel: HomeViewModel
) {
    val nav           = rememberNavController()
    val ctx           = LocalContext.current
    val livePin       by AegisPreferences.appPinFlow(ctx).collectAsState(initial = initialPin)
    val liveBiometric by AegisPreferences.biometricFlow(ctx).collectAsState(initial = biometricEnabled)

    Scaffold(containerColor = BgAmoled) { padding ->
        NavHost(
            navController    = nav,
            startDestination = if (startLocked) Routes.LOCK_SCREEN else Routes.HOME,
            modifier         = Modifier.padding(padding)
        ) {
            composable(Routes.LOCK_SCREEN) {
                LockScreen(
                    correctPin       = livePin,
                    biometricEnabled = liveBiometric,
                    onUnlocked       = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.LOCK_SCREEN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onGoPosture        = { nav.navigate(Routes.DEVICE_POSTURE) },
                    onGoApps           = { nav.navigate(Routes.APP_PERMISSION) },
                    onGoNetwork        = { nav.navigate(Routes.NETWORK_INTEL) },
                    onGoBreach         = { nav.navigate(Routes.BREACH_MONITOR) },
                    onGoComms          = { nav.navigate(Routes.COMMS_VALIDATOR) },
                    onGoPrivacyShield  = { nav.navigate(Routes.PRIVACY_SHIELD) },
                    onGoHttpRecon      = { nav.navigate(Routes.HTTP_RECON) },
                    onGoPortScan       = { nav.navigate(Routes.PORT_SCAN) },
                    onGoDns            = { nav.navigate(Routes.DNS_INTEL) },
                    onGoApkAnalyser    = { nav.navigate(Routes.APK_ANALYSER) },
                    onGoShizukuFirewall    = { nav.navigate(Routes.shizukuModeRoute("firewall")) },
                    onGoShizukuPermissions = { nav.navigate(Routes.shizukuModeRoute("permissions")) },
                    onGoShizukuNetMonitor  = { nav.navigate(Routes.shizukuModeRoute("netmonitor")) },
                    onGoSettings       = { nav.navigate(Routes.SETTINGS) },
                    onExit             = { (ctx as? Activity)?.finish() },
                    homeViewModel      = homeViewModel
                )
            }
            composable(Routes.DEVICE_POSTURE)  { DevicePostureScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.APP_PERMISSION)  { AppPermissionScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.NETWORK_INTEL)   { NetworkIntelScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.BREACH_MONITOR)  { BreachMonitorScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.COMMS_VALIDATOR) { CommsValidatorScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.PRIVACY_SHIELD)  {
                PrivacyShieldScreen(
                    viewModel = privacyShieldViewModel,
                    onBack    = { nav.popBackStack() }
                )
            }
            composable(Routes.HTTP_RECON)      { HttpReconScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.PORT_SCAN)       { PortScanScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.DNS_INTEL)       { DnsIntelScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.APK_ANALYSER)    { ApkAnalyserScreen(onBack = { nav.popBackStack() }) }
            composable(
                Routes.SHIZUKU_MODE,
                arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "firewall" })
            ) { backStackEntry ->
                val tabKey = backStackEntry.arguments?.getString("tab")
                ShizukuModeScreen(onBack = { nav.popBackStack() }, initialTab = ShizukuTab.fromKey(tabKey))
            }
            composable(Routes.SETTINGS)        { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
