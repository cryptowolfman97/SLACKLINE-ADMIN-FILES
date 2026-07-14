package com.example.interstellarcalc.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon    = { Icon(Icons.Default.RocketLaunch, contentDescription = null) },
        title   = { Text("Abort Mission?") },
        text    = { Text("Are you sure you want to exit Interstellar Calc+?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Exit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Stay") } }
    )
}
