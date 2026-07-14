package com.example.omnicortex.license

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omnicortex.ui.theme.*

/**
 * Shown when the user hasn't signed in to their SH Vertex account at all,
 * regardless of which module (even a Free one) they tapped.
 */
@Composable
fun LoginRequiredDialog(
    onDismiss: () -> Unit,
    onOpenLicenseDetails: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor    = BgCard,
        icon = {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                    .background(AegisAmber.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.LockPerson, null, tint = AegisAmber, modifier = Modifier.size(28.dp)) }
        },
        title = {
            Text("Sign In Required", color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text(
                "You need to sign in to your SH Vertex account to use ${LicenseGateConfig.APP_NAME}, " +
                "including Free tier tools. Tap \"License Details\" below to sign in or create an account.",
                color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = { onDismiss(); onOpenLicenseDetails() },
                colors  = ButtonDefaults.buttonColors(containerColor = AegisAmber),
                shape   = RoundedCornerShape(10.dp)
            ) { Text("Open License Details", color = BgAmoled, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

/**
 * Shown when a signed-in user taps a module above their current tier
 * (Free tapping Pro/Pro+, or Pro tapping Pro+).
 */
@Composable
fun UpgradeLockDialog(
    requiredTier: Tier,
    moduleName: String,
    onDismiss: () -> Unit,
    onOpenLicenseDetails: () -> Unit
) {
    val accent = if (requiredTier == Tier.PRO_PLUS) AegisPurple else AegisCyan
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor    = BgCard,
        icon = {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Lock, null, tint = accent, modifier = Modifier.size(28.dp)) }
        },
        title = {
            Text("${requiredTier.label} Required", color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text(
                "\"$moduleName\" is a ${requiredTier.label} tier feature. Upgrade your license to unlock it.",
                color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = { onDismiss(); onOpenLicenseDetails() },
                colors  = ButtonDefaults.buttonColors(containerColor = accent),
                shape   = RoundedCornerShape(10.dp)
            ) { Text("Upgrade to ${requiredTier.label}", color = BgAmoled, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Not Now", color = TextSecondary)
            }
        }
    )
}
