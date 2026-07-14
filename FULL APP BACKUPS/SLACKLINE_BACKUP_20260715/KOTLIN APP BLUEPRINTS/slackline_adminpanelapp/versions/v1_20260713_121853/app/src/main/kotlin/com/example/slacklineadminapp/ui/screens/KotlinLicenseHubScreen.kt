package com.example.slacklineadminapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.slacklineadminapp.data.KotlinLicenseEngine
import com.example.slacklineadminapp.data.KotlinLicenseRepository
import com.example.slacklineadminapp.data.KotlinProduct
import com.example.slacklineadminapp.data.ProductStatus
import com.example.slacklineadminapp.data.SecurityConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KotlinLicenseHubScreen(
    onNavigateToProduct: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var products by remember { mutableStateOf(emptyList<KotlinProduct>()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Delete flow state
    var productPendingDelete by remember { mutableStateOf<KotlinProduct?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        products = KotlinLicenseRepository.getAllProducts()
    }

    // Called when user long-presses a product card
    fun requestDelete(product: KotlinProduct) {
        productPendingDelete = product
        val cfg = SecurityConfig.get(context)
        if (cfg.advPin.isNotEmpty()) {
            // Advanced PIN is set — require it before showing confirm
            showPinDialog = true
        } else {
            // No PIN set — go straight to confirmation
            showConfirmDialog = true
        }
    }

    fun executeDelete() {
        productPendingDelete?.let { prod ->
            KotlinLicenseRepository.deleteProduct(prod.id)
            products = KotlinLicenseRepository.getAllProducts()
        }
        productPendingDelete = null
        showConfirmDialog = false
        showPinDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "KOTLIN APPS LICENSE MANAGER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No Kotlin products found.\nTap '+' to create your first product authority.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products) { product ->
                        ProductItemCard(
                            product = product,
                            onClick = { onNavigateToProduct(product.id) },
                            onLongClick = { requestDelete(product) }
                        )
                    }
                }
            }
        }

        // ── ADD PRODUCT DIALOG ─────────────────────────────────────────
        if (showAddDialog) {
            AddProductDialog(
                onDismiss = { showAddDialog = false },
                onProductCreated = {
                    products = KotlinLicenseRepository.getAllProducts()
                    showAddDialog = false
                }
            )
        }

        // ── ADVANCED PIN VERIFICATION DIALOG ──────────────────────────
        if (showPinDialog && productPendingDelete != null) {
            AdvancedPinVerifyDialog(
                productName = productPendingDelete!!.name,
                onDismiss = {
                    showPinDialog = false
                    productPendingDelete = null
                },
                onPinVerified = {
                    showPinDialog = false
                    showConfirmDialog = true
                }
            )
        }

        // ── FINAL DELETE CONFIRMATION DIALOG ──────────────────────────
        if (showConfirmDialog && productPendingDelete != null) {
            DeleteConfirmDialog(
                productName = productPendingDelete!!.name,
                onDismiss = {
                    showConfirmDialog = false
                    productPendingDelete = null
                },
                onConfirm = { executeDelete() }
            )
        }
    }
}

// ── PRODUCT CARD WITH LONG-PRESS ──────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductItemCard(
    product: KotlinProduct,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val statusColor = when (product.status) {
        ProductStatus.ACTIVE -> Color(0xFF00E701)
        ProductStatus.WARNING -> Color(0xFFFFB300)
        ProductStatus.REVOKED -> Color(0xFFFF4E4E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Prefix Root: ${product.prefix}  |  Code: ${product.appCode}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hold to delete",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = product.status.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Manage",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── ADVANCED PIN VERIFICATION DIALOG ─────────────────────────────────
@Composable
private fun AdvancedPinVerifyDialog(
    productName: String,
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFF4E4E),
                    modifier = Modifier.size(36.dp)
                )

                Text(
                    "Delete Product",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF4E4E)
                )

                Text(
                    "Enter your Advanced PIN to delete \"$productName\".",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { pinInput = it; pinError = "" } },
                    label = { Text("Advanced PIN (6 digits)") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = pinError.isNotEmpty(),
                    supportingText = { if (pinError.isNotEmpty()) Text(pinError, color = Color(0xFFFF4E4E)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val cfg = SecurityConfig.get(context)
                            if (pinInput == cfg.advPin) {
                                onPinVerified()
                            } else {
                                pinError = "Incorrect PIN. Try again."
                                pinInput = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4E4E))
                    ) {
                        Text("Verify", color = Color.White)
                    }
                }
            }
        }
    }
}

// ── DELETE CONFIRMATION DIALOG ────────────────────────────────────────
@Composable
private fun DeleteConfirmDialog(
    productName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = Color(0xFFFF4E4E)
            )
        },
        title = {
            Text("Delete \"$productName\"?", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "This will permanently delete this product authority and cannot be undone. " +
                "All associated license records will also be removed.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4E4E))
            ) {
                Text("Delete Forever", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ── ADD PRODUCT DIALOG ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(onDismiss: () -> Unit, onProductCreated: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("") }
    var appCode by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Create New Product Authority",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Prefix Root (e.g., CTP-)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = appCode,
                    onValueChange = { appCode = it },
                    label = { Text("Unique App Code (Internal Unique Key)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isGenerating) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && prefix.isNotBlank() && appCode.isNotBlank()) {
                                isGenerating = true
                                val keys = KotlinLicenseEngine.generateKeyPair()
                                val newProduct = KotlinProduct(
                                    name = name.trim(),
                                    prefix = prefix.trim().uppercase(),
                                    appCode = appCode.trim().lowercase(),
                                    publicKeyPem = keys.publicKeyPem,
                                    privateKeyPem = keys.privateKeyPem
                                )
                                KotlinLicenseRepository.saveProduct(newProduct)
                                onProductCreated()
                            }
                        },
                        enabled = !isGenerating && name.isNotBlank() && prefix.isNotBlank() && appCode.isNotBlank()
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Generate Authority")
                        }
                    }
                }
            }
        }
    }
}