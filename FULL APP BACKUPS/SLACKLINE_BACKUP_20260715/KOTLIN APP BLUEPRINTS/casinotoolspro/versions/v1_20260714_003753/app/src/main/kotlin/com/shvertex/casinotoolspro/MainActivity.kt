package com.shvertex.casinotoolspro

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shvertex.casinotoolspro.license.CTPAccess
import com.shvertex.casinotoolspro.license.LicenseGateScreen
import com.shvertex.casinotoolspro.navigation.AppNavigation
import com.shvertex.casinotoolspro.navigation.Routes
import com.shvertex.casinotoolspro.theme.*

// ── Global State & Terminology Dictionary ─────────────────────────────────────

val LocalPresentationMode = compositionLocalOf<MutableState<Boolean>> {
    error("Presentation Mode state not provided")
}

val LocalPipMode = compositionLocalOf { false }

object AppStrings {
    fun appName(isPres: Boolean) = if (isPres) "Strategy Suite Pro" else "Casino Tools Pro"
    fun appSubtitle(isPres: Boolean) = if (isPres) "Advanced risk analytics toolkit" else "Private gambling analytics toolkit"
    fun profitLabel(isPres: Boolean) = if (isPres) "SESSION UNITS" else "SESSION PROFIT"
    fun inputPlaceholder(isPres: Boolean) = if (isPres) "Enter Result (+/-)" else "Enter Win/Loss (+/-)"
    fun statusText(isPres: Boolean) = if (isPres) "PRO+ ACTIVE | PRESENTATION MODE" else "PRO+ ACTIVE | CTP-8E170416"
    
    fun disclaimerTitle(isPres: Boolean) = if (isPres) "Academic & Research Notice" else "Responsible Gambling Disclaimer"
    fun disclaimerText(isPres: Boolean) = if (isPres) {
        """VANTAGE - Strategy Suite Pro is a premium presentation layer built on top of the same analytical engine inside Casino Tools Pro. It is designed for clean demos, structured walkthroughs, and educational showcase videos where risk, variance, progression logic, and simulated outcomes need to be explained clearly.
           Presentation Mode refines selected wording and visuals for marketing and demonstration purposes only. It does not change the underlying calculations, licensing controls, or safety principles of the app.
           
           Important Disclaimer
                • This software is an analysis, simulation, and education tool — not a gambling product.
                • It does not encourage gambling, guarantee profit, or claim predictive certainty.
                • Nothing shown inside the app should be treated as betting advice, financial advice, or a promise of results.
                • Any real-world decisions remain the sole responsibility of the user.
            
           Created by SH VERTEX TECH"""
    } else {
        """VATAGE - Casino Tools Pro is an analytics and simulation toolkit built to help users study bankroll behavior, variance, progression logic, discipline, and betting structures more clearly. It was created to explore risk mathematically, not to glamorize gambling.
         
           Important Disclaimer
                • Casino Tools Pro does not promote gambling or encourage irresponsible betting.
                • It is not a promise of income and must never be treated as a guaranteed way to make money.
                • All results shown by the app are mathematical simulations, estimates, or scenario outputs — not certainties.
                • Gambling always carries real financial risk, and losses can happen quickly.
                • This tool is intended for analysis, education, strategy review, and awareness of risk.
                • Users are responsible for complying with local laws, platform rules, and age restrictions.
           Use the app to understand probability and decision quality better — not as a shortcut to profit.
         
           Created by SH VERTEX TECH"""
    }
}

// ── Main Activity ─────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    
    private var isPipState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isPresentationMode = remember { mutableStateOf(false) }
            var gateCleared by remember { mutableStateOf(false) }

            CompositionLocalProvider(
                LocalPresentationMode provides isPresentationMode,
                LocalPipMode provides isPipState
            ) {
                CasinoToolsProTheme {
                    if (!gateCleared) {
                        LicenseGateScreen(
                            context = this,
                            onAccessGranted = { tier ->
                                CTPAccess.setTier(tier)
                                gateCleared = true
                            }
                        )
                    } else {
                        val navController = rememberNavController()
                        val currentEntry  by navController.currentBackStackEntryAsState()
                        val currentRoute  = currentEntry?.destination?.route
                        var showExitDialog by remember { mutableStateOf(false) }

                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CTPColors.Black)
                                .systemBarsPadding()
                                .blur(radius = if (showExitDialog) 12.dp else 0.dp)
                        ) {
                            BackHandler(enabled = currentRoute == Routes.HOME) {
                                showExitDialog = true
                            }
                            AppNavigation(navController = navController)
                        }

                        if (showExitDialog) {
                            ExitConfirmationDialog(
                                isPresentation = isPresentationMode.value,
                                onConfirm = { finish() },
                                onDismiss = { showExitDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipState = isInPictureInPictureMode
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }
}

// ── Exit Dialog ───────────────────────────────────────────────────────────────

@Composable
fun ExitConfirmationDialog(isPresentation: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(CTPColors.Card.copy(alpha = 0.85f))
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(CTPColors.RedDim.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) { Text("⚡", fontSize = 28.sp) }

                Text(
                    text      = if (isPresentation) "Exit Strategy Suite Pro?" else "Exit Casino Tools Pro?",
                    style     = CTPType.HeadlineLarge,
                    color     = CTPColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text      = "Your session data is saved automatically.\nYou can resume anytime.",
                    style     = CTPType.BodyMedium,
                    color     = CTPColors.TextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = CTPColors.TextSecondary),
                        border  = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CTPColors.Border)),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("STAY", style = CTPType.LabelLarge) }

                    Button(
                        onClick = onConfirm,
                        colors  = ButtonDefaults.buttonColors(containerColor = CTPColors.Red),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("EXIT", style = CTPType.LabelLarge, color = Color.White) }
                }
            }
        }
    }
}
