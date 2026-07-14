package com.example.slacklineadminapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CustomerEntry(
    val name: String,
    val email: String,
    val product: String,
    val licenseId: String,
    val status: String,
    val date: String
)

@Composable
fun CustomersDirectoryScreen(onNavigateBack: () -> Unit) {
    var customers by remember { mutableStateOf<List<CustomerEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<CustomerEntry>()

            // Legacy products via ProductRegistry
            for (cfg in ProductRegistry.all()) {
                val eng = EngineCache.get(cfg)
                eng.loadRecords().forEach { r ->
                    val name = r.customerName.ifBlank { return@forEach }
                    list.add(CustomerEntry(
                        name      = name,
                        email     = r.customerEmail.ifBlank { "-" },
                        product   = cfg.displayName,
                        licenseId = r.licenseId,
                        status    = r.status,
                        date      = r.issuedAt.take(10)
                    ))
                }
            }

            // New license products
            for (prod in NewLicenseStore.allProducts()) {
                NewLicenseStore.loadLicenses(prod.id).forEach { lic ->
                    val name = lic.customerName.ifBlank { return@forEach }
                    list.add(CustomerEntry(
                        name      = name,
                        email     = lic.customerEmail.ifBlank { "-" },
                        product   = prod.displayName,
                        licenseId = lic.licenseId,
                        status    = lic.status,
                        date      = lic.issuedAt.take(10)
                    ))
                }
            }

            customers = list.sortedByDescending { it.date }
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.bg)
    ) {
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, null, tint = GreenCol, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Global Customers", color = GreenCol, fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, modifier = Modifier.weight(1f))
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenCol)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { BodyText("Total Registered Customers: ${customers.size}", SubText) }
                if (customers.isEmpty()) {
                    item {
                        AppCard { BodyText("No customers found. Enter names when generating licenses.", SubText) }
                    }
                } else {
                    items(customers) { c ->
                        AppCard(color = LocalAppColors.current.card2) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.name, color = GreenCol, fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(c.status.uppercase(),
                                    color = if (c.status == "active") GreenCol else RedCol,
                                    fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Text("Email: ${c.email}", color = TextCol, fontSize = 12.sp)
                            Text("Product: ${c.product}", color = TextCol, fontSize = 12.sp)
                            Text("License: ${c.licenseId}", color = SubText, fontSize = 11.sp)
                            Text("Since: ${c.date}", color = SubText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        BottomNavBar(listOf("← BACK" to onNavigateBack))
    }
}
