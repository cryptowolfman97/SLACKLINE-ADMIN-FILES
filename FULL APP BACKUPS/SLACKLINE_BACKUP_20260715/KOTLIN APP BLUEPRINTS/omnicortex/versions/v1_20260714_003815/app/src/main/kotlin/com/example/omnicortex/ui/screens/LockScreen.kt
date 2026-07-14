package com.example.omnicortex.ui.screens

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.omnicortex.ui.theme.*

@Composable
fun LockScreen(
    correctPin: String,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val maxLen = correctPin.length.coerceAtLeast(4).coerceAtMost(6)

    val context = LocalContext.current as? FragmentActivity

    // Helper to launch biometric prompt
    val launchBiometric = {
        if (context != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(context, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
            })
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Omni-Cortex")
                .setSubtitle("Confirm your biometric to continue")
                .setNegativeButtonText("Use PIN")
                .build()
            prompt.authenticate(promptInfo)
        }
    }

    // Auto-launch on first composition if enabled
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            launchBiometric()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgAmoled)
            .navigationBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Lowered overall UI and provided space for manual Biometric trigger
        Spacer(Modifier.height(50.dp))

        if (biometricEnabled && context != null) {
            IconButton(
                onClick = launchBiometric,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AegisGreen.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Fingerprint, null, tint = AegisGreen, modifier = Modifier.size(32.dp))
            }
        } else {
            Spacer(Modifier.height(56.dp)) // Maintain spacing if no biometric
        }

        Spacer(Modifier.height(40.dp))

        // Logo
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AegisGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, null, tint = AegisGreen, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text("SHV Omni-Cortex", color = AegisGreen, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("Enter PIN to continue", color = TextMuted, fontSize = 13.sp)

        Spacer(Modifier.height(36.dp))

        // PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(maxLen) { i ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i < input.length) AegisGreen else BgCardBorder)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (error.isNotBlank()) {
            Text(
                error, color = AegisRed, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
            )
        } else {
            Spacer(Modifier.height(18.dp)) // Keep layout stable
        }

        Spacer(Modifier.height(28.dp))

        // Number pad
        val rows = listOf(
            listOf("1","2","3"),
            listOf("4","5","6"),
            listOf("7","8","9"),
            listOf("C","0","⌫")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        Button(
                            onClick = {
                                error = ""
                                when (key) {
                                    "C"  -> input = ""
                                    "⌫" -> if (input.isNotEmpty()) input = input.dropLast(1)
                                    else -> {
                                        if (input.length < maxLen) {
                                            input += key
                                            if (input.length == correctPin.length) {
                                                if (input == correctPin) onUnlocked()
                                                else { error = "Incorrect PIN"; input = "" }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(62.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = when (key) {
                                    "C"  -> AegisRed.copy(alpha = 0.15f)
                                    "⌫" -> AegisAmber.copy(alpha = 0.15f)
                                    else -> BgCard
                                }
                            )
                        ) {
                            Text(
                                key,
                                color      = when (key) { "C" -> AegisRed; "⌫" -> AegisAmber; else -> TextPrimary },
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
