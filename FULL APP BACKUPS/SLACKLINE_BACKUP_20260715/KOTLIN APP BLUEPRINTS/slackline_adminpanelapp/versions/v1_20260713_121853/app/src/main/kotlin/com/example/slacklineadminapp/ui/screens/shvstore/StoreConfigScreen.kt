package com.example.slacklineadminapp.ui.screens.shvstore

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.SecurityConfig
import com.example.slacklineadminapp.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@Composable
fun StoreConfigScreen(onNavigateBack: () -> Unit) {
    val appColors = LocalAppColors.current
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()

    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isSaving     by remember { mutableStateOf(false) }
    var savedOk      by remember { mutableStateOf(false) }

    // Load current saved values on entry
    LaunchedEffect(Unit) {
        val cfg = SecurityConfig.get(context)
        email    = cfg.storeEmail
        password = cfg.storePassword
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bg)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.card)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF00E5CC))
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "STORE CONFIG",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5CC),
                    letterSpacing = 1.5.sp
                )
                Text(
                    "SHV Store Admin Credentials",
                    fontSize = 11.sp,
                    color = appColors.subtext
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1C1C1C), thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E5CC).copy(alpha = 0.06f))
                    .border(1.dp, Color(0xFF00E5CC).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = Color(0xFF00E5CC),
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                    "These credentials are used to authenticate with Supabase when you open the SHV Store Admin section. They are stored encrypted on this device only.",
                    color = appColors.subtext,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; savedOk = false },
                label = { Text("Store Admin Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, null, tint = Color(0xFF00E5CC), modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Color(0xFF00E5CC),
                    unfocusedBorderColor    = Color(0xFF1C1C1C),
                    focusedTextColor        = appColors.text,
                    unfocusedTextColor      = appColors.text,
                    cursorColor             = Color(0xFF00E5CC),
                    focusedLabelColor       = Color(0xFF00E5CC),
                    unfocusedLabelColor     = appColors.subtext,
                    focusedContainerColor   = appColors.card,
                    unfocusedContainerColor = appColors.card
                )
            )

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; savedOk = false },
                label = { Text("Store Admin Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, null, tint = Color(0xFF00E5CC), modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null,
                            tint = appColors.subtext,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Color(0xFF00E5CC),
                    unfocusedBorderColor    = Color(0xFF1C1C1C),
                    focusedTextColor        = appColors.text,
                    unfocusedTextColor      = appColors.text,
                    cursorColor             = Color(0xFF00E5CC),
                    focusedLabelColor       = Color(0xFF00E5CC),
                    unfocusedLabelColor     = appColors.subtext,
                    focusedContainerColor   = appColors.card,
                    unfocusedContainerColor = appColors.card
                )
            )

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        SecurityConfig.setStoreEmail(context, email.trim())
                        SecurityConfig.setStorePassword(context, password)
                        isSaving = false
                        savedOk  = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isSaving && email.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5CC))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        "Save Credentials",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Success confirmation
            if (savedOk) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF00E5CC).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF00E5CC).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00E5CC), modifier = Modifier.size(16.dp))
                    Text("Credentials saved successfully.", color = Color(0xFF00E5CC), fontSize = 13.sp)
                }
            }
        }
    }
}
