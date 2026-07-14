package com.example.slacklineadminapp.ui.screens

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.slacklineadminapp.data.AppStorage

// ── Data models ───────────────────────────────────────────────────────────────

private data class TabEntry(val label: String, val icon: String)

private enum class AppTemplate(val displayName: String) {
    BOTTOM_NAV("Bottom Navigation"),
    NAV_DRAWER("Navigation Drawer")
}

// Built-in vector icon set — real paths, no @android:drawable dependency
private val ICON_OPTIONS = listOf(
    "ic_nav_home", "ic_nav_dashboard", "ic_nav_notifications",
    "ic_nav_settings", "ic_nav_person", "ic_nav_star",
    "ic_nav_list", "ic_nav_search", "ic_nav_info"
)

private val ICON_LABELS = mapOf(
    "ic_nav_home"          to "Home",
    "ic_nav_dashboard"     to "Dashboard",
    "ic_nav_notifications" to "Notifications",
    "ic_nav_settings"      to "Settings",
    "ic_nav_person"        to "Person",
    "ic_nav_star"          to "Star",
    "ic_nav_list"          to "List",
    "ic_nav_search"        to "Search",
    "ic_nav_info"          to "Info"
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun KotlinAppGeneratorScreen(onNavigateBack: () -> Unit) {
    BackHandler { onNavigateBack() }

    val appColors   = LocalAppColors.current
    val scope       = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // ── Form state ────────────────────────────────────────────────────────────
    var appName      by remember { mutableStateOf("") }
    var packageName  by remember { mutableStateOf("") }
    var outputPath   by remember { mutableStateOf(
        AppStorage.generatedAppsDir().absolutePath
    ) }
    var selectedTemplate by remember { mutableStateOf(AppTemplate.BOTTOM_NAV) }

    var tabs by remember {
        mutableStateOf(listOf(
            TabEntry("Home",     "ic_nav_home"),
            TabEntry("Dashboard","ic_nav_dashboard"),
            TabEntry("More",     "ic_nav_notifications"),
        ))
    }

    // ── Output state ──────────────────────────────────────────────────────────
    var statusLog    by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var isDone       by remember { mutableStateOf(false) }
    var progress     by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GreenCol)
            }
            Text(
                "Kotlin App Generator",
                color      = GreenCol,
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier   = Modifier.padding(start = 4.dp)
            )
        }

        HorizontalDivider(color = SubText.copy(alpha = 0.15f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Template picker ───────────────────────────────────────────────
            SectionCard(title = "Template", appColors = appColors) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppTemplate.entries.forEach { template ->
                        val selected = selectedTemplate == template
                        OutlinedButton(
                            onClick  = { selectedTemplate = template },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) GreenCol.copy(alpha = 0.12f) else Color.Transparent,
                                contentColor   = if (selected) GreenCol else SubText
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) GreenCol else SubText.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(
                                if (template == AppTemplate.BOTTOM_NAV) Icons.Default.ViewQuilt
                                else Icons.Default.Menu,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(template.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (selectedTemplate == AppTemplate.BOTTOM_NAV) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Floating pill-style bottom nav · 2–4 tabs",
                        color    = SubText.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Side drawer with toolbar · unlimited items",
                        color    = SubText.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            // ── App Info card ─────────────────────────────────────────────────
            SectionCard(title = "App Info", appColors = appColors) {
                AppTextField(
                    value         = appName,
                    onValueChange = { appName = it },
                    label         = "App Name",
                    hint          = "e.g. My Awesome App",
                    appColors     = appColors
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value         = packageName,
                    onValueChange = { packageName = it },
                    label         = "Package Name",
                    hint          = "e.g. com.yourname.myapp",
                    keyboard      = KeyboardType.Uri,
                    appColors     = appColors
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value         = outputPath,
                    onValueChange = { outputPath = it },
                    label         = "Output Directory (paste an existing ACS project root)",
                    hint          = "/storage/emulated/0/AndroidCodeStudio/MyApp",
                    appColors     = appColors
                )
            }

            // ── Tabs / Drawer items card ──────────────────────────────────────
            val sectionTitle = if (selectedTemplate == AppTemplate.BOTTOM_NAV)
                "Bottom Nav Tabs (2–4)" else "Drawer Items (2–6)"
            val maxItems = if (selectedTemplate == AppTemplate.BOTTOM_NAV) 4 else 6

            SectionCard(title = sectionTitle, appColors = appColors) {
                tabs.forEachIndexed { index, tab ->
                    TabEditorRow(
                        index     = index,
                        tab       = tab,
                        canRemove = tabs.size > 2,
                        appColors = appColors,
                        onLabelChange = { newLabel ->
                            tabs = tabs.toMutableList().also { it[index] = tab.copy(label = newLabel) }
                        },
                        onIconChange = { newIcon ->
                            tabs = tabs.toMutableList().also { it[index] = tab.copy(icon = newIcon) }
                        },
                        onRemove = {
                            tabs = tabs.toMutableList().also { it.removeAt(index) }
                        }
                    )
                    if (index < tabs.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color    = SubText.copy(alpha = 0.1f)
                        )
                    }
                }
                if (tabs.size < maxItems) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick  = {
                            tabs = tabs + TabEntry("Tab ${tabs.size + 1}", "ic_nav_star")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = GreenCol),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, GreenCol.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (selectedTemplate == AppTemplate.BOTTOM_NAV) "Add Tab" else "Add Drawer Item",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Features info card ────────────────────────────────────────────
            SectionCard(title = "Included Features", appColors = appColors) {
                val features = listOf(
                    "AMOLED black + light theme toggle",
                    "Settings button on Home screen toolbar",
                    "Back → Home, Back on Home → Exit dialog",
                    "Floating pill bottom nav (Bottom Nav template)",
                    "NavController + Navigation component",
                    "ViewModel + LiveData per fragment",
                    "ViewBinding enabled"
                )
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint     = GreenCol.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(feature, color = SubText, fontSize = 11.sp)
                    }
                }
            }

            // ── Progress & status log ─────────────────────────────────────────
            if (isGenerating) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color    = GreenCol,
                        trackColor = SubText.copy(alpha = 0.15f)
                    )
                    Text(
                        "Generating… ${(progress * 100).toInt()}%",
                        color    = GreenCol,
                        fontSize = 11.sp
                    )
                }
            }

            if (statusLog.isNotEmpty()) {
                Card(
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDone) GreenCol.copy(alpha = 0.08f) else appColors.card
                    )
                ) {
                    Text(
                        text       = statusLog,
                        color      = if (isDone) GreenCol else SubText,
                        fontSize   = 11.sp,
                        modifier   = Modifier.padding(14.dp),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        // ── Generate button ───────────────────────────────────────────────────
        HorizontalDivider(color = SubText.copy(alpha = 0.15f))
        Button(
            onClick = {
                val errors = validate(appName, packageName, tabs, selectedTemplate)
                if (errors != null) {
                    statusLog = "⚠️ $errors"
                    isDone    = false
                    return@Button
                }
                isGenerating = true
                isDone       = false
                progress     = 0f
                statusLog    = ""
                scope.launch {
                    val log = StringBuilder()
                    val result = withContext(Dispatchers.IO) {
                        generateProject(
                            appName.trim(), packageName.trim(),
                            tabs, outputPath.trim(), selectedTemplate, log
                        ) { p -> progress = p }
                    }
                    statusLog    = log.toString()
                    isGenerating = false
                    isDone       = result
                }
            },
            enabled  = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(48.dp),
            shape  = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor          = GreenCol,
                disabledContainerColor  = GreenCol.copy(alpha = 0.4f)
            )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = Color.Black,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (isGenerating) "Generating…" else "Generate Project",
                color      = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 15.sp
            )
        }
    }
}

// ── Composable helpers ────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    appColors: AppColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            title,
            color      = SubText,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(bottom = 6.dp)
        )
        Card(
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = appColors.card)
        ) {
            Column(modifier = Modifier.padding(14.dp), content = content)
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    keyboard: KeyboardType = KeyboardType.Text,
    appColors: AppColors
) {
    Column {
        Text(
            label,
            color    = SubText,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(hint, color = SubText.copy(alpha = 0.5f), fontSize = 13.sp) },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(10.dp),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(
                keyboardType   = keyboard,
                capitalization = if (keyboard == KeyboardType.Text) KeyboardCapitalization.Words
                                 else KeyboardCapitalization.None
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GreenCol,
                unfocusedBorderColor = SubText.copy(alpha = 0.3f),
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = GreenCol
            )
        )
    }
}

@Composable
private fun TabEditorRow(
    index: Int,
    tab: TabEntry,
    canRemove: Boolean,
    appColors: AppColors,
    onLabelChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "${index + 1}.",
            color      = SubText,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.width(20.dp)
        )
        OutlinedTextField(
            value         = tab.label,
            onValueChange = onLabelChange,
            placeholder   = { Text("Label", color = SubText.copy(alpha = 0.4f), fontSize = 12.sp) },
            modifier      = Modifier.weight(1f),
            shape         = RoundedCornerShape(8.dp),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = GreenCol,
                unfocusedBorderColor = SubText.copy(alpha = 0.3f),
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = GreenCol
            )
        )
        Spacer(Modifier.width(6.dp))
        Box {
            OutlinedButton(
                onClick  = { expanded = true },
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SubText),
                border   = androidx.compose.foundation.BorderStroke(1.dp, SubText.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(ICON_LABELS[tab.icon] ?: tab.icon, fontSize = 10.sp)
            }
            DropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
                modifier         = Modifier.background(appColors.card)
            ) {
                ICON_OPTIONS.forEach { icon ->
                    DropdownMenuItem(
                        text = { Text(ICON_LABELS[icon] ?: icon, color = Color.White, fontSize = 12.sp) },
                        onClick = { onIconChange(icon); expanded = false },
                        leadingIcon = {
                            if (icon == tab.icon)
                                Icon(Icons.Default.Check, null, tint = GreenCol, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }
        }
        if (canRemove) {
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove",
                    tint     = RedCol,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Validation ────────────────────────────────────────────────────────────────

private fun validate(
    appName: String,
    packageName: String,
    tabs: List<TabEntry>,
    template: AppTemplate
): String? {
    if (appName.isBlank()) return "App name cannot be empty."
    if (packageName.isBlank()) return "Package name cannot be empty."
    if (!packageName.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\$")))
        return "Package name must be like com.yourname.myapp (lowercase, dots, underscores only)."
    if (tabs.any { it.label.isBlank() }) return "All labels must be filled in."
    if (template == AppTemplate.BOTTOM_NAV && tabs.size > 4)
        return "Bottom navigation supports a maximum of 4 tabs."
    return null
}

// ── File generation ───────────────────────────────────────────────────────────

private fun generateProject(
    appName: String,
    packageName: String,
    tabs: List<TabEntry>,
    outputPath: String,
    template: AppTemplate,
    log: StringBuilder,
    onProgress: (Float) -> Unit
): Boolean {
    return try {
        val root = File(outputPath)
        val src  = File(root, "app/src/main")
        val res  = File(src,  "res")
        val kt   = File(src,  "kotlin/${packageName.replace('.', '/')}")

        var step  = 0
        // Estimate total files: varies by template/tab count
        val totalSteps = when (template) {
            AppTemplate.BOTTOM_NAV   -> 10 + tabs.size * 3
            AppTemplate.NAV_DRAWER   -> 12 + tabs.size * 3
        }

        fun write(file: File, content: String) {
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            step++
            onProgress((step.toFloat() / totalSteps).coerceAtMost(0.95f))
            log.append("✓ ${file.name}\n")
        }

        // ── 1. colors.xml ─────────────────────────────────────────────────────
        write(File(res, "values/colors.xml"), colorsXml())

        // ── 2. themes.xml (day + night) ───────────────────────────────────────
        write(File(res, "values/themes.xml"),       themesXml(template, isNight = false))
        write(File(res, "values-night/themes.xml"), themesXml(template, isNight = true))

        // ── 3. strings.xml ────────────────────────────────────────────────────
        write(File(res, "values/strings.xml"), stringsXml(appName, tabs, template))

        // ── 4. Nav icons (vector drawables) ───────────────────────────────────
        NAV_ICON_VECTORS.forEach { (name, pathData) ->
            write(File(res, "drawable/$name.xml"), vectorXml(pathData))
        }

        // ── 5. Exit dialog drawable ───────────────────────────────────────────
        write(File(res, "drawable/rounded_dialog_bg.xml"), roundedDialogBg())

        // ── 6. Template-specific layouts & navigation ─────────────────────────
        when (template) {
            AppTemplate.BOTTOM_NAV -> generateBottomNavFiles(
                res, kt, packageName, appName, tabs, ::write
            )
            AppTemplate.NAV_DRAWER -> generateNavDrawerFiles(
                res, kt, packageName, appName, tabs, ::write
            )
        }

        // ── 7. Fragment files (shared by both templates) ──────────────────────
        tabs.forEachIndexed { i, tab ->
            val isHome     = i == 0
            val isSettings = tab.label.equals("settings", ignoreCase = true)
            val fragKey    = tab.label.toFragKey()
            val className  = "${tab.label.toPascal()}Fragment"
            val vmClass    = "${tab.label.toPascal()}ViewModel"
            val bindClass  = "Fragment${tab.label.toPascal()}Binding"
            val uiPkg      = "$packageName.ui.$fragKey"

            // ViewModel
            write(
                File(kt, "ui/$fragKey/${vmClass}.kt"),
                viewModelKt(uiPkg, vmClass, tab.label)
            )
            // Fragment
            write(
                File(kt, "ui/$fragKey/${className}.kt"),
                when {
                    isHome     -> homeFragmentKt(uiPkg, packageName, className, vmClass, bindClass)
                    isSettings -> settingsFragmentKt(uiPkg, packageName, className, bindClass)
                    else       -> genericFragmentKt(uiPkg, className, vmClass, bindClass)
                }
            )
            // Layout
            write(
                File(res, "layout/fragment_${fragKey}.xml"),
                when {
                    isHome     -> homeFragmentLayout(appName)
                    isSettings -> settingsFragmentLayout()
                    else       -> genericFragmentLayout(tab.label)
                }
            )
        }

        // ── 8. dialog_exit.xml layout ─────────────────────────────────────────
        write(File(res, "layout/dialog_exit.xml"), dialogExitXml())

        onProgress(1f)
        log.append("\n✅ Done! Open the project in ACS and build.\nPath: $outputPath")
        true
    } catch (e: Exception) {
        log.append("\n❌ Error: ${e.message}")
        false
    }
}

// ── Bottom Navigation template files ─────────────────────────────────────────

private fun generateBottomNavFiles(
    res: File, kt: File,
    packageName: String, appName: String,
    tabs: List<TabEntry>,
    write: (File, String) -> Unit
) {
    // activity_main.xml — floating pill style
    write(File(res, "layout/activity_main.xml"), bottomNavActivityMain())

    // bottom_nav_menu.xml
    write(File(res, "menu/bottom_nav_menu.xml"), bottomNavMenu(tabs))

    // options menu (settings toolbar button)
    write(File(res, "menu/menu_main.xml"), optionsMenu())

    // nav_item_color selector
    write(File(res, "color/nav_item_color.xml"), navItemColorSelector())

    // mobile_navigation.xml
    write(File(res, "navigation/mobile_navigation.xml"), mobileNavigation(packageName, tabs, startId = "navigation_${tabs[0].label.toFragKey()}"))

    // MainActivity.kt
    write(File(kt, "MainActivity.kt"), bottomNavMainActivity(packageName, appName, tabs))
}

// ── Navigation Drawer template files ─────────────────────────────────────────

private fun generateNavDrawerFiles(
    res: File, kt: File,
    packageName: String, appName: String,
    tabs: List<TabEntry>,
    write: (File, String) -> Unit
) {
    // activity_main.xml
    write(File(res, "layout/activity_main.xml"), drawerActivityMain())

    // app_bar_main.xml
    write(File(res, "layout/app_bar_main.xml"), drawerAppBarMain())

    // content_main.xml
    write(File(res, "layout/content_main.xml"), drawerContentMain())

    // nav_header_main.xml
    write(File(res, "layout/nav_header_main.xml"), navHeaderMain(appName))

    // side_nav_bar drawable
    write(File(res, "drawable/side_nav_bar.xml"), sideNavBar())

    // drawer menu
    write(File(res, "menu/activity_main_drawer.xml"), drawerMenu(tabs))

    // options menu (settings toolbar button)
    write(File(res, "menu/menu_main.xml"), optionsMenu())

    // navigation graph
    write(File(res, "navigation/mobile_navigation.xml"), mobileNavigation(packageName, tabs, startId = "nav_${tabs[0].label.toFragKey()}"))

    // MainActivity
    write(File(kt, "MainActivity.kt"), drawerMainActivity(packageName, appName, tabs))
}

// ── String/key helpers ────────────────────────────────────────────────────────

private fun String.toFragKey()  = lowercase().replace(" ", "_")
private fun String.toPascal()   = split(" ").joinToString("") { it.replaceFirstChar(Char::uppercase) }
private fun String.xmlEscape()  = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

// ── Resource templates ────────────────────────────────────────────────────────

private fun colorsXml() = """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- AMOLED dark palette -->
    <color name="amoled_black">#FF000000</color>
    <color name="surface_card">#FF1A1A1A</color>
    <color name="surface_card_2">#FF242424</color>
    <color name="accent">#FF00E5CC</color>
    <color name="accent_dark">#FF009980</color>
    <color name="text_primary">#FFFFFFFF</color>
    <color name="text_secondary">#FF9E9E9E</color>
    <!-- Light palette -->
    <color name="light_background">#FFF5F5F5</color>
    <color name="light_surface">#FFFFFFFF</color>
    <color name="light_accent">#FF007A6E</color>
    <color name="light_text_primary">#FF111111</color>
    <color name="light_text_secondary">#FF666666</color>
    <!-- Common -->
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    <color name="error">#FFCF6679</color>
</resources>""".trimIndent()

private fun themesXml(template: AppTemplate, isNight: Boolean) = buildString {
    val noActionBar = template == AppTemplate.BOTTOM_NAV
    val parentDay   = if (noActionBar) "Theme.MaterialComponents.DayNight.NoActionBar"
                      else "Theme.MaterialComponents.DayNight.DarkActionBar"
    if (isNight) {
        append("""
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.AppTheme" parent="$parentDay">
        <item name="android:colorBackground">@color/amoled_black</item>
        <item name="colorSurface">@color/surface_card</item>
        <item name="colorPrimary">@color/accent</item>
        <item name="colorPrimaryVariant">@color/accent_dark</item>
        <item name="colorOnPrimary">@color/amoled_black</item>
        <item name="android:textColorPrimary">@color/text_primary</item>
        <item name="android:textColorSecondary">@color/text_secondary</item>
        <item name="android:statusBarColor" tools:targetApi="21">@color/amoled_black</item>
        <item name="android:navigationBarColor">@color/amoled_black</item>
        <item name="android:windowLightStatusBar" tools:targetApi="23">false</item>
        ${if (noActionBar) "<item name=\"windowActionBar\">false</item>\n        <item name=\"windowNoTitle\">true</item>" else ""}
    </style>
</resources>""".trimIndent())
    } else {
        append("""
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.AppTheme" parent="$parentDay">
        <item name="android:colorBackground">@color/light_background</item>
        <item name="colorSurface">@color/light_surface</item>
        <item name="colorPrimary">@color/light_accent</item>
        <item name="colorPrimaryVariant">@color/light_accent</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="android:textColorPrimary">@color/light_text_primary</item>
        <item name="android:textColorSecondary">@color/light_text_secondary</item>
        <item name="android:statusBarColor" tools:targetApi="21">@color/light_background</item>
        <item name="android:navigationBarColor">@color/light_background</item>
        <item name="android:windowLightStatusBar" tools:targetApi="23">true</item>
        ${if (noActionBar) "<item name=\"windowActionBar\">false</item>\n        <item name=\"windowNoTitle\">true</item>" else ""}
    </style>
    ${if (!noActionBar) """
    <style name="Theme.AppTheme.AppBarOverlay" parent="ThemeOverlay.AppCompat.Dark.ActionBar" />
    <style name="Theme.AppTheme.PopupOverlay" parent="ThemeOverlay.AppCompat.Light" />""".trimIndent() else ""}
</resources>""".trimIndent())
    }
}

private fun stringsXml(appName: String, tabs: List<TabEntry>, template: AppTemplate) = buildString {
    append("""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${appName.xmlEscape()}</string>
    <string name="action_settings">Settings</string>
    <string name="exit_title">Exit App?</string>
    <string name="exit_message">Are you sure you want to exit?</string>
    <string name="exit_yes">Exit</string>
    <string name="exit_no">Cancel</string>
    <string name="toggle_theme">Dark Mode (AMOLED)</string>
    <string name="toggle_theme_desc">Switch between AMOLED black and light theme</string>
""")
    tabs.forEach { tab ->
        val key = when (template) {
            AppTemplate.BOTTOM_NAV -> "title_${tab.label.toFragKey()}"
            AppTemplate.NAV_DRAWER -> "menu_${tab.label.toFragKey()}"
        }
        append("    <string name=\"$key\">${tab.label.xmlEscape()}</string>\n")
    }
    if (template == AppTemplate.NAV_DRAWER) {
        append("""    <string name="navigation_drawer_open">Open navigation drawer</string>
    <string name="navigation_drawer_close">Close navigation drawer</string>
    <string name="nav_header_title">${appName.xmlEscape()}</string>
    <string name="nav_header_subtitle">Built with SHV Generator</string>
    <string name="nav_header_desc">Navigation header</string>
""")
    }
    append("</resources>")
}

private fun vectorXml(pathData: String) = """
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24.0"
    android:viewportHeight="24.0">
    <path
        android:fillColor="#FF000000"
        android:pathData="$pathData" />
</vector>""".trimIndent()

private val NAV_ICON_VECTORS = mapOf(
    "ic_nav_home"          to "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z",
    "ic_nav_dashboard"     to "M3,13h8L11,3L3,3v10zM3,21h8v-6L3,15v6zM13,21h8L21,11h-8v10zM13,3v6h8L21,3h-8z",
    "ic_nav_notifications" to "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.9,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z",
    "ic_nav_settings"      to "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6 3.6,1.62 3.6,3.6 -1.62,3.6 -3.6,3.6z",
    "ic_nav_person"        to "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z",
    "ic_nav_star"          to "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z",
    "ic_nav_list"          to "M3,13h2L5,11L3,11v2zM3,17h2v-2L3,15v2zM3,9h2L5,7L3,7v2zM7,13h14v-2L7,11v2zM7,17h14v-2L7,15v2zM7,7v2h14L21,7L7,7z",
    "ic_nav_search"        to "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
    "ic_nav_info"          to "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-6h2v6zM13,9h-2L11,7h2v2z"
)

private fun roundedDialogBg() = """
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/surface_card"/>
    <corners android:radius="24dp"/>
</shape>""".trimIndent()

private fun sideNavBar() = """
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient
        android:angle="135"
        android:startColor="@color/accent"
        android:centerColor="@color/accent_dark"
        android:endColor="@color/surface_card"
        android:type="linear"/>
</shape>""".trimIndent()

// ── activity_main.xml — floating pill bottom nav ───────────────────────────────

private fun bottomNavActivityMain() = """
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?android:colorBackground">

    <fragment
        android:id="@+id/nav_host_fragment_activity_main"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:defaultNavHost="true"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:navGraph="@navigation/mobile_navigation" />

    <!-- Floating pill-style bottom nav -->
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/nav_card"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="32dp"
        android:layout_marginEnd="32dp"
        android:layout_marginBottom="20dp"
        app:cardBackgroundColor="@color/surface_card"
        app:cardCornerRadius="40dp"
        app:cardElevation="12dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent">

        <com.google.android.material.bottomnavigation.BottomNavigationView
            android:id="@+id/nav_view"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@android:color/transparent"
            app:itemIconTint="@color/nav_item_color"
            app:itemTextColor="@color/nav_item_color"
            app:labelVisibilityMode="labeled"
            app:menu="@menu/bottom_nav_menu" />

    </com.google.android.material.card.MaterialCardView>

</androidx.constraintlayout.widget.ConstraintLayout>""".trimIndent()

private fun bottomNavMenu(tabs: List<TabEntry>) = buildString {
    append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n")
    tabs.forEach { tab ->
        val id     = "navigation_${tab.label.toFragKey()}"
        val strKey = "title_${tab.label.toFragKey()}"
        append("    <item\n        android:id=\"@+id/$id\"\n        android:icon=\"@drawable/${tab.icon}\"\n        android:title=\"@string/$strKey\" />\n")
    }
    append("</menu>")
}

private fun mobileNavigation(packageName: String, tabs: List<TabEntry>, startId: String) = buildString {
    append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
    append("<navigation xmlns:android=\"http://schemas.android.com/apk/res/android\"\n")
    append("    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n")
    append("    xmlns:tools=\"http://schemas.android.com/tools\"\n")
    append("    android:id=\"@+id/mobile_navigation\"\n")
    append("    app:startDestination=\"@+id/$startId\">\n\n")
    tabs.forEach { tab ->
        val fragKey   = tab.label.toFragKey()
        val fragClass = ".ui.$fragKey.${tab.label.toPascal()}Fragment"
        val navId     = if (startId.startsWith("navigation_")) "navigation_$fragKey" else "nav_$fragKey"
        val strKey    = if (startId.startsWith("navigation_")) "title_$fragKey" else "menu_$fragKey"
        append("    <fragment\n")
        append("        android:id=\"@+id/$navId\"\n")
        append("        android:name=\"$fragClass\"\n")
        append("        android:label=\"@string/$strKey\"\n")
        append("        tools:layout=\"@layout/fragment_$fragKey\" />\n\n")
    }
    append("</navigation>")
}

private fun bottomNavMainActivity(
    packageName: String, appName: String, tabs: List<TabEntry>
) = buildString {
    val topLevelIds = tabs.joinToString(", ") { "R.id.navigation_${it.label.toFragKey()}" }
    append("""
package $packageName

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import ${packageName}.databinding.ActivityMainBinding
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        applyTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(null)

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(setOf($topLevelIds))
        binding.navView.setupWithNavController(navController)

        // Back: go to Home if not there, else show exit dialog
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeId = R.id.navigation_${tabs[0].label.toFragKey()}
                if (navController.currentDestination?.id != homeId) {
                    navController.navigate(homeId)
                } else {
                    showExitDialog()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                navController.navigate(R.id.navigation_${getSettingsFragKey(tabs)})
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun isDarkMode() = prefs.getBoolean("dark_mode", true)

    fun toggleTheme(dark: Boolean) {
        prefs.edit().putBoolean("dark_mode", dark).apply()
        recreate()
    }

    private fun applyTheme() {
        val isDark = prefs.getBoolean("dark_mode", true)
        setTheme(if (isDark) R.style.Theme_AppTheme else R.style.Theme_AppTheme)
        // Theme.AppTheme in values-night/ handles AMOLED automatically via DayNight
        if (isDark) {
            delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        } else {
            delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_title))
            .setMessage(getString(R.string.exit_message))
            .setPositiveButton(getString(R.string.exit_yes)) { _, _ -> finish() }
            .setNegativeButton(getString(R.string.exit_no), null)
            .show()
    }
}
""".trimIndent())
}

private fun getSettingsFragKey(tabs: List<TabEntry>): String {
    val settingsTab = tabs.firstOrNull { it.label.equals("settings", ignoreCase = true) }
    return settingsTab?.label?.toFragKey() ?: tabs.last().label.toFragKey()
}

// ── Navigation Drawer layout files ────────────────────────────────────────────

private fun drawerActivityMain() = """
<?xml version="1.0" encoding="utf-8"?>
<androidx.drawerlayout.widget.DrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/drawer_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true">

    <include
        android:id="@+id/app_bar_main"
        layout="@layout/app_bar_main"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <com.google.android.material.navigation.NavigationView
        android:id="@+id/nav_view"
        android:layout_width="wrap_content"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        android:fitsSystemWindows="true"
        app:headerLayout="@layout/nav_header_main"
        app:menu="@menu/activity_main_drawer" />

</androidx.drawerlayout.widget.DrawerLayout>""".trimIndent()

private fun drawerAppBarMain() = """
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:theme="@style/Theme.AppTheme.AppBarOverlay">
        <androidx.appcompat.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="?attr/colorPrimary"
            app:popupTheme="@style/Theme.AppTheme.PopupOverlay" />
    </com.google.android.material.appbar.AppBarLayout>

    <include layout="@layout/content_main" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>""".trimIndent()

private fun drawerContentMain() = """
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:layout_marginTop="?attr/actionBarSize">

    <fragment
        android:id="@+id/nav_host_fragment_content_main"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:defaultNavHost="true"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:navGraph="@navigation/mobile_navigation" />

</androidx.constraintlayout.widget.ConstraintLayout>""".trimIndent()

private fun navHeaderMain(appName: String) = """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="176dp"
    android:background="@drawable/side_nav_bar"
    android:gravity="bottom"
    android:orientation="vertical"
    android:paddingStart="16dp"
    android:paddingEnd="16dp"
    android:paddingBottom="16dp">

    <ImageView
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:contentDescription="@string/nav_header_desc"
        android:layout_marginBottom="8dp"
        app:srcCompat="@mipmap/ic_launcher_round" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/nav_header_title"
        android:textColor="@color/white"
        android:textSize="18sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/nav_header_subtitle"
        android:textColor="@color/white"
        android:alpha="0.75"
        android:textSize="12sp" />

</LinearLayout>""".trimIndent()

private fun drawerMenu(tabs: List<TabEntry>) = buildString {
    append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    xmlns:tools=\"http://schemas.android.com/tools\"\n    tools:showIn=\"navigation_view\">\n    <group android:checkableBehavior=\"single\">\n")
    tabs.forEach { tab ->
        val id     = "nav_${tab.label.toFragKey()}"
        val strKey = "menu_${tab.label.toFragKey()}"
        append("        <item\n            android:id=\"@+id/$id\"\n            android:icon=\"@drawable/${tab.icon}\"\n            android:title=\"@string/$strKey\" />\n")
    }
    append("    </group>\n</menu>")
}

private fun drawerMainActivity(
    packageName: String, appName: String, tabs: List<TabEntry>
) = buildString {
    val topLevelIds = tabs.joinToString(", ") { "R.id.nav_${it.label.toFragKey()}" }
    append("""
package $packageName

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import ${packageName}.databinding.ActivityMainBinding
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        applyTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView    = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(setOf($topLevelIds), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Back: close drawer → go Home → exit dialog
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                    drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
                    return
                }
                val homeId = R.id.nav_${tabs[0].label.toFragKey()}
                if (navController.currentDestination?.id != homeId) {
                    navController.navigate(homeId)
                } else {
                    showExitDialog()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_${getSettingsFragKey(tabs)})
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun isDarkMode() = prefs.getBoolean("dark_mode", true)

    fun toggleTheme(dark: Boolean) {
        prefs.edit().putBoolean("dark_mode", dark).apply()
        recreate()
    }

    private fun applyTheme() {
        if (prefs.getBoolean("dark_mode", true)) {
            delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        } else {
            delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    private fun showExitDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_title))
            .setMessage(getString(R.string.exit_message))
            .setPositiveButton(getString(R.string.exit_yes)) { _, _ -> finish() }
            .setNegativeButton(getString(R.string.exit_no), null)
            .show()
    }
}
""".trimIndent())
}

private fun navItemColorSelector() = """
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="?attr/colorPrimary" android:state_checked="true" />
    <item android:color="?android:textColorSecondary" />
</selector>""".trimIndent()

private fun optionsMenu() = """
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <item
        android:id="@+id/action_settings"
        android:title="@string/action_settings"
        android:icon="@drawable/ic_nav_settings"
        app:showAsAction="ifRoom" />
</menu>""".trimIndent()

// ── Fragment Kotlin templates ─────────────────────────────────────────────────

private fun viewModelKt(pkg: String, vmClass: String, label: String) = """
package $pkg

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class $vmClass : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "$label"
    }
    val text: LiveData<String> = _text
}
""".trimIndent()

private fun genericFragmentKt(
    pkg: String, className: String, vmClass: String, bindClass: String
) = """
package $pkg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class $className : Fragment() {

    private var _binding: ${bindClass}Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get($vmClass::class.java)
        _binding = ${bindClass}Binding.inflate(inflater, container, false)
        viewModel.text.observe(viewLifecycleOwner) { binding.textContent.text = it }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
""".trimIndent()

private fun homeFragmentKt(
    pkg: String, rootPkg: String, className: String, vmClass: String, bindClass: String
) = """
package $pkg

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import $rootPkg.MainActivity

class $className : Fragment() {

    private var _binding: ${bindClass}Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get($vmClass::class.java)
        _binding = ${bindClass}Binding.inflate(inflater, container, false)
        viewModel.text.observe(viewLifecycleOwner) { binding.textContent.text = it }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
""".trimIndent()

private fun settingsFragmentKt(
    pkg: String, rootPkg: String, className: String, bindClass: String
) = """
package $pkg

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import $rootPkg.MainActivity

class $className : Fragment() {

    private var _binding: ${bindClass}Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ${bindClass}Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val main = requireActivity() as MainActivity
        binding.themeSwitch.isChecked = main.isDarkMode()
        binding.themeSwitch.setOnCheckedChangeListener { _, checked ->
            main.toggleTheme(checked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
""".trimIndent()

// ── Fragment layout templates ─────────────────────────────────────────────────

private fun homeFragmentLayout(appName: String) = """
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?android:colorBackground"
    android:fillViewport="true"
    android:clipToPadding="false"
    android:paddingBottom="100dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingStart="20dp"
        android:paddingEnd="20dp"
        android:paddingTop="32dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Good day 👋"
            android:textColor="?attr/colorPrimary"
            android:textSize="13sp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Welcome to ${appName.xmlEscape()}"
            android:textColor="?android:textColorPrimary"
            android:textSize="26sp"
            android:textStyle="bold"
            android:layout_marginTop="4dp"
            android:layout_marginBottom="28dp" />

        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            app:cardBackgroundColor="?attr/colorSurface"
            app:cardCornerRadius="20dp"
            app:cardElevation="0dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="22dp">

                <TextView
                    android:id="@+id/text_content"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="🚀  Getting Started"
                    android:textColor="?android:textColorPrimary"
                    android:textSize="16sp"
                    android:textStyle="bold" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="10dp"
                    android:text="Your app is ready. Tap the nav bar to switch tabs, or use the Settings button in the toolbar."
                    android:textColor="?android:textColorSecondary"
                    android:textSize="14sp"
                    android:lineSpacingExtra="5dp" />

            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

    </LinearLayout>
</ScrollView>""".trimIndent()

private fun genericFragmentLayout(label: String) = """
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?android:colorBackground"
    android:clipToPadding="false"
    android:paddingBottom="100dp"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingStart="20dp"
        android:paddingEnd="20dp"
        android:paddingTop="32dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="${label.xmlEscape()}"
            android:textColor="?android:textColorPrimary"
            android:textSize="26sp"
            android:textStyle="bold"
            android:layout_marginBottom="24dp" />

        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:cardBackgroundColor="?attr/colorSurface"
            app:cardCornerRadius="20dp"
            app:cardElevation="0dp">

            <TextView
                android:id="@+id/text_content"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:padding="22dp"
                android:text="Add your ${label.xmlEscape()} content here."
                android:textColor="?android:textColorSecondary"
                android:textSize="14sp"
                android:lineSpacingExtra="5dp" />

        </com.google.android.material.card.MaterialCardView>

    </LinearLayout>
</ScrollView>""".trimIndent()

private fun settingsFragmentLayout() = """
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?android:colorBackground"
    android:clipToPadding="false"
    android:paddingBottom="100dp"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingStart="20dp"
        android:paddingEnd="20dp"
        android:paddingTop="32dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Settings"
            android:textColor="?android:textColorPrimary"
            android:textSize="26sp"
            android:textStyle="bold"
            android:layout_marginBottom="28dp" />

        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            app:cardBackgroundColor="?attr/colorSurface"
            app:cardCornerRadius="20dp"
            app:cardElevation="0dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:padding="20dp"
                android:gravity="center_vertical">

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/toggle_theme"
                        android:textColor="?android:textColorPrimary"
                        android:textSize="15sp"
                        android:textStyle="bold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="4dp"
                        android:text="@string/toggle_theme_desc"
                        android:textColor="?android:textColorSecondary"
                        android:textSize="12sp" />

                </LinearLayout>

                <com.google.android.material.switchmaterial.SwitchMaterial
                    android:id="@+id/themeSwitch"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content" />

            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

    </LinearLayout>
</ScrollView>""".trimIndent()

private fun dialogExitXml() = """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp"
    android:background="@drawable/rounded_dialog_bg">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/exit_title"
        android:textColor="?android:textColorPrimary"
        android:textSize="18sp"
        android:textStyle="bold"
        android:layout_marginBottom="8dp" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/exit_message"
        android:textColor="?android:textColorSecondary"
        android:textSize="13sp"
        android:layout_marginBottom="20dp" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="end">

        <Button
            android:id="@+id/btnCancel"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:text="@string/exit_no"
            android:textColor="?android:textColorSecondary"
            android:layout_marginEnd="4dp" />

        <Button
            android:id="@+id/btnExit"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:text="@string/exit_yes"
            android:textColor="?attr/colorPrimary" />

    </LinearLayout>
</LinearLayout>""".trimIndent()
