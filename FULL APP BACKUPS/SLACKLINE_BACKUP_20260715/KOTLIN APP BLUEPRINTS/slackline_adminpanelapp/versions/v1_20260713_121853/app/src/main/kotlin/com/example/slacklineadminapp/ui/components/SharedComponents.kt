package com.example.slacklineadminapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.ui.theme.*

@Composable
fun SectionLabel(text: String, color: Color = TealCol, size: Int = 18) {
    Text(
        text = text,
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun BodyText(text: String, color: Color = TextCol) {
    Text(text = text, color = color, fontSize = 13.sp, lineHeight = 18.sp)
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = color ?: appColors.card),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    color: Color = TealCol,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape    = RoundedCornerShape(10.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    readOnly: Boolean = false,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label, fontSize = 12.sp) },
        modifier             = modifier.fillMaxWidth(),
        singleLine           = singleLine,
        readOnly             = readOnly,
        visualTransformation = if (password) PasswordVisualTransformation()
                               else VisualTransformation.None,
        shape  = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = TealCol,
            unfocusedBorderColor = SubText.copy(alpha = 0.5f),
            focusedLabelColor    = TealCol,
            unfocusedLabelColor  = SubText,
            focusedTextColor     = TextCol,
            unfocusedTextColor   = TextCol,
            cursorColor          = TealCol
        )
    )
}

@Composable
fun StatCard(value: Int, label: String, color: Color) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.height(72.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = appColors.card2)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value.toString(), color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(label, color = SubText, fontSize = 11.sp)
        }
    }
}

@Composable
fun BottomNavBar(buttons: List<Pair<String, () -> Unit>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavBg)
            .navigationBarsPadding()                            // ← Fix: respects bottom nav bar
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { (label, action) ->
            Button(
                onClick  = action,
                modifier = Modifier.weight(1f).height(52.dp),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = NavCol)
            ) {
                Text(label, color = Color(0xFFC8C8C8), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "OK",
    confirmColor: Color = TealCol,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title  = { Text(title, fontWeight = FontWeight.Bold) },
        text   = { Text(message, color = SubText) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmText.uppercase(), color = confirmColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = SubText) }
        },
        containerColor    = CardBg,
        titleContentColor = TextCol,
        textContentColor  = SubText
    )
}

@Composable
fun NumberPad(
    enteredDigits: String,
    maxLen: Int,
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val buttons = listOf("1","2","3","4","5","6","7","8","9","C","0","<")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in buttons.chunked(3)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { lbl ->
                    val bg = when (lbl) { "C" -> RedCol; "<" -> OrangeCol; else -> Color(0xFF1E293B) }
                    Button(
                        onClick  = {
                            when (lbl) {
                                "C"  -> onClear()
                                "<"  -> onBack()
                                else -> if (enteredDigits.length < maxLen) onDigit(lbl)
                            }
                        },
                        modifier = Modifier.weight(1f).height(65.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = bg)
                    ) {
                        Text(
                            text       = if (lbl == "<") "⌫" else lbl,
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = TealCol, strokeWidth = 3.dp)
    }
}