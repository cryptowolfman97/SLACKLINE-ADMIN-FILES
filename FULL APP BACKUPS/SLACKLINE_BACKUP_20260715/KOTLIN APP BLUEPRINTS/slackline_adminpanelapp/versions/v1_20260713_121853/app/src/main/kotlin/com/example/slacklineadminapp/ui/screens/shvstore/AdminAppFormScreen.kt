@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.slacklineadminapp.ui.screens.shvstore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.StoreSupabaseApi
import com.example.slacklineadminapp.ui.theme.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

// ─── Pricing tier model ───────────────────────────────────────────────────────
private data class PricingTier(
    val tier: String,           // "Demo" | "Pro" | "Pro+"
    val price: String,          // "Free" | "9.99" etc.
    val type: String,           // "free" | "one_time" | "monthly" | "yearly"
    val enabled: Boolean = false
)

private val FIXED_TIERS = listOf("Demo", "Pro", "Pro+")
private val PRICE_TYPES  = listOf("free", "one_time", "monthly", "yearly")
private val PRICE_TYPE_LABELS = mapOf(
    "free"     to "Free",
    "one_time" to "One-time",
    "monthly"  to "Monthly",
    "yearly"   to "Yearly"
)

// ─── Colours ──────────────────────────────────────────────────────────────────
private val BG        = Color(0xFF000000)
private val CARD      = Color(0xFF0E0E0E)
private val BORDER    = Color(0xFF1C1C1C)
private val ACCENT    = Color(0xFF00E5CC)
private val GOLD      = Color(0xFFFFB300)
private val AMBER     = Color(0xFFFF8C00)
private val ERROR     = Color(0xFFFF4D6A)
private val TXT       = Color(0xFFF2F4F6)
private val TXT_MUTED = Color(0xFF4A5260)
private val TXT_SUB   = Color(0xFF8A929C)

@Composable
fun AdminAppFormScreen(appId: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()

    // ── Basic fields ──────────────────────────────────────────────────────────
    var name              by remember { mutableStateOf("") }
    var tagline           by remember { mutableStateOf("") }
    var version           by remember { mutableStateOf("") }
    var category          by remember { mutableStateOf("") }
    var description       by remember { mutableStateOf("") }
    var features           by remember { mutableStateOf("") } // one feature per line
    var apkUrl            by remember { mutableStateOf("") }
    var iconUrl           by remember { mutableStateOf("") }
    var screenshots       by remember { mutableStateOf("") }
    var requiresLicense   by remember { mutableStateOf(false) }
    var isFeatured        by remember { mutableStateOf(false) }
    var hasUpdate         by remember { mutableStateOf(false) }
    var isUnderMaintenance by remember { mutableStateOf(false) }
    var syncToWebsite      by remember { mutableStateOf(false) }
    var existingIsPublished by remember { mutableStateOf(false) }
    var existingSortOrder   by remember { mutableStateOf(0) }

    // ── Pricing tiers — one entry per fixed tier ──────────────────────────────
    // Each entry: enabled flag + price string + type string
    data class TierState(
        var enabled: Boolean = false,
        var price:   String  = "",
        var type:    String  = "free"   // free | one_time | monthly | yearly
    )
    val tiers = remember {
        mutableStateListOf(
            TierState(), // Demo
            TierState(), // Pro
            TierState()  // Pro+
        )
    }

    var isLoading    by remember { mutableStateOf(false) }
    var hasChanges   by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    val isEditing = appId != "new"
    val markDirty: () -> Unit = { if (!hasChanges) hasChanges = true }

    // ── Load existing data ────────────────────────────────────────────────────
    LaunchedEffect(appId) {
        if (isEditing) {
            try {
                val resp = StoreSupabaseApi.client.get("${StoreSupabaseApi.BASE_URL}/rest/v1/apps") {
                    header("apikey", StoreSupabaseApi.ANON_KEY)
                    header("Authorization", "Bearer ${StoreSupabaseApi.accessToken ?: StoreSupabaseApi.ANON_KEY}")
                    parameter("id", "eq.$appId")
                }
                if (resp.status == HttpStatusCode.OK) {
                    val item = resp.body<List<JsonObject>>().firstOrNull()
                    item?.let {
                        name               = it["name"]?.jsonPrimitive?.content ?: ""
                        tagline            = it["tagline"]?.jsonPrimitive?.content ?: ""
                        version            = it["version"]?.jsonPrimitive?.content ?: ""
                        category           = it["category"]?.jsonPrimitive?.content ?: ""
                        description        = it["description"]?.jsonPrimitive?.content ?: ""
                        features           = it["features"]?.jsonArray
                            ?.mapNotNull { f -> f.jsonPrimitive.contentOrNull }
                            ?.joinToString("\n") ?: ""
                        apkUrl             = it["apk_url"]?.jsonPrimitive?.content ?: ""
                        iconUrl            = it["icon_url"]?.jsonPrimitive?.content ?: ""
                        screenshots        = it["screenshots"]?.jsonPrimitive?.content ?: ""
                        requiresLicense    = it["requires_license"]?.jsonPrimitive?.boolean ?: false
                        isFeatured         = it["is_featured"]?.jsonPrimitive?.boolean ?: false
                        hasUpdate          = it["has_update"]?.jsonPrimitive?.boolean ?: false
                        isUnderMaintenance = it["is_under_maintenance"]?.jsonPrimitive?.boolean ?: false
                        syncToWebsite      = it["sync_to_website"]?.jsonPrimitive?.boolean ?: false
                        existingIsPublished = it["is_published"]?.jsonPrimitive?.boolean ?: false
                        existingSortOrder   = it["sort_order"]?.jsonPrimitive?.intOrNull ?: 0

                        // Parse saved pricing JSON back into tier states
                        val pricingJson = it["pricing"]?.jsonArray
                        pricingJson?.forEach { el ->
                            val obj  = el.jsonObject
                            val tier = obj["tier"]?.jsonPrimitive?.content ?: return@forEach
                            val idx  = FIXED_TIERS.indexOf(tier)
                            if (idx >= 0) {
                                tiers[idx] = TierState(
                                    enabled = true,
                                    price   = obj["price"]?.jsonPrimitive?.content ?: "",
                                    type    = obj["type"]?.jsonPrimitive?.content ?: "free"
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        hasChanges = false
    }

    BackHandler(enabled = hasChanges) { showExitDialog = true }

    // ── Exit dialog ───────────────────────────────────────────────────────────
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = CARD, shape = RoundedCornerShape(20.dp),
            title = { Text("Discard changes?", color = TXT, fontWeight = FontWeight.Bold) },
            text  = { Text("You have unsaved changes. Are you sure you want to leave?", color = TXT_SUB) },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; hasChanges = false; onDone() }) {
                    Text("Discard", color = ERROR)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Keep Editing", color = ACCENT)
                }
            }
        )
    }

    // ── Main form ─────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // ── Basic info fields ─────────────────────────────────────────────────
        SectionLabel("APP INFO")
        Spacer(Modifier.height(10.dp))

        FormField(name, { name = it; markDirty() }, "Name")
        FormField(tagline, { tagline = it; markDirty() }, "Tagline")
        FormField(version, { version = it; markDirty() }, "Version")
        FormField(category, { category = it; markDirty() }, "Category")
        OutlinedTextField(
            value = description, onValueChange = { description = it; markDirty() },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )
        Spacer(Modifier.height(10.dp))

        // ── App Features ─────────────────────────────────────────────────────
        SectionLabel("APP FEATURES")
        Spacer(Modifier.height(4.dp))
        Text(
            "One feature per line. Shown to customers as a bullet list.",
            fontSize = 11.sp, color = TXT_MUTED, lineHeight = 16.sp
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = features, onValueChange = { features = it; markDirty() },
            label = { Text("Features") },
            placeholder = { Text("Live currency conversion\nFloating overlay calculator\nOffline mode") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )
        Spacer(Modifier.height(20.dp))

        // ── URLs ─────────────────────────────────────────────────────────────
        SectionLabel("FILES & MEDIA")
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = apkUrl, onValueChange = { apkUrl = it; markDirty() },
            label = { Text("APK URL") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), colors = fieldColors()
        )
        if (apkUrl.contains("dropbox.com"))
            DropboxHint("→ Will save as dl=1")
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = iconUrl, onValueChange = { iconUrl = it; markDirty() },
            label = { Text("Icon URL") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), colors = fieldColors()
        )
        if (iconUrl.contains("dropbox.com"))
            DropboxHint("→ Will save as dl=1")
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = screenshots, onValueChange = { screenshots = it; markDirty() },
            label = { Text("Screenshots (comma-separated URLs)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), colors = fieldColors()
        )
        if (screenshots.contains("dropbox.com"))
            DropboxHint("→ Will save each as raw=1")
        Spacer(Modifier.height(20.dp))

        // ── Pricing tiers ─────────────────────────────────────────────────────
        SectionLabel("PRICING TIERS")
        Spacer(Modifier.height(4.dp))
        Text(
            "Enable the tiers available for this app. Leave disabled to hide that tier.",
            fontSize = 11.sp, color = TXT_MUTED, lineHeight = 16.sp
        )
        Spacer(Modifier.height(12.dp))

        FIXED_TIERS.forEachIndexed { idx, tierName ->
            val tier = tiers[idx]

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (tier.enabled) Color(0xFF0E1F1D) else CARD)
                    .border(
                        1.dp,
                        if (tier.enabled) ACCENT.copy(alpha = 0.35f) else BORDER,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    // Tier header row with enable toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        when (tierName) {
                                            "Demo" -> TXT_MUTED.copy(alpha = 0.12f)
                                            "Pro"  -> ACCENT.copy(alpha = 0.12f)
                                            else   -> GOLD.copy(alpha = 0.12f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    when (tierName) {
                                        "Demo" -> Icons.Default.Preview
                                        "Pro"  -> Icons.Default.Star
                                        else   -> Icons.Default.WorkspacePremium
                                    },
                                    contentDescription = null,
                                    tint = when (tierName) {
                                        "Demo" -> TXT_MUTED
                                        "Pro"  -> ACCENT
                                        else   -> GOLD
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    tierName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tier.enabled) TXT else TXT_MUTED
                                )
                                if (!tier.enabled)
                                    Text("Disabled", fontSize = 10.sp, color = TXT_MUTED)
                            }
                        }
                        Switch(
                            checked = tier.enabled,
                            onCheckedChange = {
                                tiers[idx] = tier.copy(enabled = it)
                                markDirty()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ACCENT
                            )
                        )
                    }

                    // Price + type fields — only shown when enabled
                    if (tier.enabled) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = BORDER)
                        Spacer(Modifier.height(12.dp))

                        // Price field
                        OutlinedTextField(
                            value = tier.price,
                            onValueChange = { tiers[idx] = tier.copy(price = it); markDirty() },
                            label = { Text("Price (e.g. Free, 9.99, 4.99)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = fieldColors()
                        )

                        Spacer(Modifier.height(10.dp))

                        // Pricing type selector
                        Text("Pricing Type", fontSize = 11.sp, color = TXT_MUTED, letterSpacing = 0.3.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PRICE_TYPES.forEach { typeKey ->
                                val selected = tier.type == typeKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected)
                                                Brush.horizontalGradient(listOf(ACCENT, Color(0xFF00BFA8)))
                                            else
                                                Brush.horizontalGradient(listOf(CARD, CARD))
                                        )
                                        .border(1.dp, if (selected) Color.Transparent else BORDER, RoundedCornerShape(8.dp))
                                        .clickable { tiers[idx] = tier.copy(type = typeKey); markDirty() }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        PRICE_TYPE_LABELS[typeKey] ?: typeKey,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.Black else TXT_SUB
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(10.dp))

        // ── Toggles ───────────────────────────────────────────────────────────
        SectionLabel("VISIBILITY & STATUS")
        Spacer(Modifier.height(10.dp))

        ToggleRow(
            label = "Featured App",
            sub   = "Shows in the featured banner on the Store tab",
            checked = isFeatured,
            trackColor = GOLD,
            onChange = { isFeatured = it; markDirty() }
        )
        Spacer(Modifier.height(10.dp))
        ToggleRow(
            label = "Update Available",
            sub   = "Shows an update banner on the app detail page",
            checked = hasUpdate,
            trackColor = ACCENT,
            onChange = { hasUpdate = it; markDirty() }
        )
        Spacer(Modifier.height(10.dp))
        ToggleRow(
            label = "Under Maintenance",
            sub   = "App stays visible but download is blocked",
            checked = isUnderMaintenance,
            trackColor = AMBER,
            onChange = { isUnderMaintenance = it; markDirty() }
        )
        Spacer(Modifier.height(10.dp))
        ToggleRow(
            label = "Requires License",
            sub   = "User must have a license to use this app",
            checked = requiresLicense,
            trackColor = ACCENT,
            onChange = { requiresLicense = it; markDirty() }
        )
        Spacer(Modifier.height(10.dp))
        ToggleRow(
            label = "Add/Update App Details on Website",
            sub   = "Syncs name, tagline, description, icon, screenshots, features & pricing to the Products page on shvertex.online (Store APK link is never sent)",
            checked = syncToWebsite,
            trackColor = ACCENT,
            onChange = { syncToWebsite = it; markDirty() }
        )

        Spacer(Modifier.height(24.dp))

        // ── Save button ───────────────────────────────────────────────────────
        Button(
            onClick = {
                scope.launch {
                    isLoading = true

                    // Build pricing JSON array from enabled tiers
                    val pricingArray = buildJsonArray {
                        tiers.forEachIndexed { idx, t ->
                            if (t.enabled) {
                                add(buildJsonObject {
                                    put("tier",  FIXED_TIERS[idx])
                                    put("price", t.price.ifBlank { "Free" })
                                    put("type",  t.type)
                                })
                            }
                        }
                    }

                    val data = mapOf(
                        "name"               to name,
                        "tagline"            to tagline,
                        "version"            to version,
                        "category"           to category,
                        "description"        to description,
                        "features"           to buildJsonArray {
                            features.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                .forEach { add(it) }
                        },
                        "apk_url"            to normalizeDropboxDl(apkUrl.trim()),
                        "icon_url"           to normalizeDropboxDl(iconUrl.trim()),
                        "screenshots"        to normalizeScreenshots(screenshots),
                        "requires_license"   to requiresLicense,
                        "is_featured"        to isFeatured,
                        "has_update"         to hasUpdate,
                        "is_under_maintenance" to isUnderMaintenance,
                        "pricing"            to pricingArray,
                        "sync_to_website"    to syncToWebsite
                    )
                    val savedId = if (isEditing) {
                        StoreSupabaseApi.updateApp(appId, data); appId
                    } else {
                        StoreSupabaseApi.insertApp(data)
                    }

                    if (savedId != null) {
                    // ── Sync (or archive) the matching website product ──
                    if (syncToWebsite) {
                        val websitePricingTiers = buildJsonArray {
                            tiers.forEachIndexed { idx, t ->
                                if (t.enabled) {
                                    val tierName = FIXED_TIERS[idx]
                                    val priceLabel = if (t.price.ifBlank { "Free" }.equals("Free", true)) "Free"
                                        else "$${t.price}" + when (t.type) { "monthly" -> "/mo"; "yearly" -> "/yr"; else -> "" }
                                    add(buildJsonObject {
                                        put("name", tierName)
                                        put("price", priceLabel)
                                        put("features", buildJsonArray {})
                                        put("highlighted", tierName == "Pro+")
                                    })
                                }
                            }
                        }
                        val enabledTiers = tiers.filterIndexed { _, t -> t.enabled }
                        val pricingType = when {
                            enabledTiers.isEmpty() || enabledTiers.all { it.price.ifBlank { "Free" }.equals("Free", true) } -> "free"
                            enabledTiers.any { it.type == "monthly" || it.type == "yearly" } -> "subscription"
                            else -> "paid"
                        }
                        val priceLabel = enabledTiers.firstOrNull { !it.price.ifBlank { "Free" }.equals("Free", true) }?.let {
                            "$${it.price}" + when (it.type) { "monthly" -> "/mo"; "yearly" -> "/yr"; else -> "" }
                        } ?: "Free"
                        val slug = name.trim().lowercase()
                            .replace(Regex("[^a-z0-9]+"), "-")
                            .trim('-')
                            .ifBlank { savedId }

                        val productData = mapOf(
                            "name"          to name,
                            "slug"          to slug,
                            "tagline"       to tagline,
                            "description"   to description,
                            "icon_url"      to normalizeDropboxRaw(iconUrl.trim()),
                            "screenshots"   to buildJsonArray {
                                normalizeScreenshots(screenshots).split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    .forEach { url -> add(buildJsonObject { put("url", url); put("caption", "") }) }
                            },
                            "features"      to buildJsonArray {
                                features.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                    .forEach { f -> add(buildJsonObject { put("title", f) }) }
                            },
                            "pricing_tiers" to websitePricingTiers,
                            "pricing_type"  to pricingType,
                            "price_label"   to priceLabel,
                            "app_version"   to version,
                            "tags"          to buildJsonArray { if (category.isNotBlank()) add(category) },
                            "is_featured"   to isFeatured,
                            "sort_order"    to existingSortOrder,
                            "status"        to if (existingIsPublished) "live" else "archived"
                        )
                        StoreSupabaseApi.upsertWebsiteProduct(savedId, productData)
                    } else {
                        StoreSupabaseApi.archiveWebsiteProductIfExists(savedId)
                    }
                    }
                    isLoading = false; hasChanges = false; onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled  = !isLoading,
            colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (!isLoading) Brush.horizontalGradient(listOf(ACCENT, Color(0xFF00BFA8)))
                        else            Brush.horizontalGradient(listOf(ACCENT.copy(alpha = 0.4f), Color(0xFF00BFA8).copy(alpha = 0.4f))),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading)
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else
                    Text(if (isEditing) "Update App" else "Create App", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }

        if (isEditing) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { scope.launch { StoreSupabaseApi.deleteApp(appId); hasChanges = false; onDone() } },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ERROR.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(ERROR.copy(alpha = 0.4f), ERROR.copy(alpha = 0.4f)))
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, null, tint = ERROR, modifier = Modifier.size(16.dp))
                    Text("Delete App", color = ERROR, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Small composables ─────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.width(3.dp).height(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF00E5CC)))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E5CC), letterSpacing = 2.sp)
    }
}

@Composable
private fun FormField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors()
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun DropboxHint(text: String) {
    Text(text, fontSize = 10.sp, color = Color(0xFF00E5CC).copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 6.dp))
}

@Composable
private fun ToggleRow(label: String, sub: String, checked: Boolean, trackColor: Color, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E0E0E))
            .border(1.dp, Color(0xFF1C1C1C), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color(0xFFF2F4F6), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, fontSize = 11.sp, color = Color(0xFF4A5260), lineHeight = 15.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = trackColor)
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Color(0xFF00E5CC).copy(alpha = 0.7f),
    unfocusedBorderColor    = Color(0xFF1C1C1C),
    focusedTextColor        = Color(0xFFF2F4F6),
    unfocusedTextColor      = Color(0xFFF2F4F6),
    cursorColor             = Color(0xFF00E5CC),
    focusedLabelColor       = Color(0xFF00E5CC),
    unfocusedLabelColor     = Color(0xFF4A5260),
    focusedContainerColor   = Color(0xFF0A0A0A),
    unfocusedContainerColor = Color(0xFF0A0A0A)
)

// ─── Dropbox URL helpers ──────────────────────────────────────────────────────

private fun normalizeDropboxDl(url: String): String {
    if (!url.contains("dropbox.com")) return url
    return url
        .replace("&dl=0", "&dl=1").replace("&raw=1", "&dl=1").replace("&dl=1", "&dl=1")
        .let { if (!it.contains("&dl=")) "$it&dl=1" else it }
}

private fun normalizeDropboxRaw(url: String): String {
    if (!url.contains("dropbox.com")) return url
    return url
        .replace("&dl=0", "&raw=1").replace("&dl=1", "&raw=1").replace("&raw=1", "&raw=1")
        .let { if (!it.contains("&raw=")) "$it&raw=1" else it }
}

private fun normalizeScreenshots(raw: String): String =
    raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ") { normalizeDropboxRaw(it) }
