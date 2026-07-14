package com.example.slacklineadminapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.AppStorage
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

// ── Data ──────────────────────────────────────────────────────────────────────

data class GuideEntry(val key: String, val title: String, var content: String)

private val DEFAULT_GUIDES = listOf(
    GuideEntry("licensing",   "Licensing Workflow",        "1. Create a product in New License Manager.\n2. Generate a key pair.\n3. Issue licenses with device codes.\n4. Distribute the activation code to customers."),
    GuideEntry("github",      "GitHub Workflow",            "1. Connect a GitHub account via GitHub Manager.\n2. Navigate to the target repository.\n3. Edit, upload, or create files as needed.\n4. Commit changes with a meaningful message."),
    GuideEntry("supabase",    "Supabase Workflow",          "1. Save credentials in the Connection tab.\n2. Use Overview to check project status.\n3. Manage users, tables, and storage as needed.\n4. Run SQL queries via the SQL Editor."),
    GuideEntry("cloud",       "Cloud Settings Workflow",    "1. Configure cloud presets for GitHub and Supabase.\n2. Use presets in GitHub Manager and Supabase Admin for quick login.\n3. Keep presets updated when credentials change."),
    GuideEntry("blueprints",  "App Blueprints Workflow",    "1. Create a new blueprint entry.\n2. Import your app source files or create files manually.\n3. Use the vault to navigate and edit the file tree.\n4. Export individual files or the full ZIP."),
    GuideEntry("websites",    "Websites Registry Workflow", "1. Add a website with name, domain, and optional repo.\n2. Open the vault to manage HTML pages.\n3. Use the editor to write or paste HTML.\n4. Export pages to the Web_Exports folder."),
    GuideEntry("documents",   "Documents & Guides Workflow","1. Create a folder to organise your files.\n2. Add files with the editor or import .txt / .md files.\n3. Edit, copy, or export files as needed."),
    GuideEntry("kotlin_tool", "Kotlin License Tool Workflow","1. Select a product from New License Manager.\n2. Fill in package name and app details.\n3. Choose Patch Existing or Generate Template.\n4. Tap Generate to produce the Kotlin license files.")
)

object WorkflowGuideStore {
    private fun file() = AppStorage.workflowGuideFile()
    private val gson = Gson()

    fun load(): MutableList<GuideEntry> {
        return try {
            val text = file().readText()
            val type = object : TypeToken<MutableList<GuideEntry>>() {}.type
            gson.fromJson<MutableList<GuideEntry>>(text, type) ?: defaultMutable()
        } catch (_: Exception) { defaultMutable() }
    }

    fun save(guides: List<GuideEntry>) {
        file().writeText(gson.toJson(guides))
    }

    private fun defaultMutable() = DEFAULT_GUIDES.map { it.copy() }.toMutableList()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun WorkflowGuideScreen(onNavigateBack: () -> Unit) {
    var guides by remember { mutableStateOf(WorkflowGuideStore.load()) }
    var openGuide by remember { mutableStateOf<GuideEntry?>(null) }
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = openGuide != null) { openGuide = null }

    LaunchedEffect(openGuide) {}

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalAppColors.current.bg)
                .padding(padding)
        ) {
            // Header
            Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, null, tint = TealCol, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (openGuide != null) openGuide!!.title else "Workflow Guide",
                        color = TealCol, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (openGuide == null) {
                        TextButton(onClick = {
                            val all = guides.joinToString("\n\n") { "=== ${it.title} ===\n${it.content}" }
                            clipboard.setText(AnnotatedString(all))
                        }) { Text("Copy All", color = BlueCol, fontSize = 12.sp) }
                    }
                }
            }

            if (openGuide == null) {
                // Guide list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { BodyText("Tap any module to view, edit, and copy its guide.", SubText) }
                    items(guides) { guide ->
                        AppCard(color = LocalAppColors.current.card2) {
                            Text(guide.title, color = CyanCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                guide.content.lines().firstOrNull()?.take(80)?.plus("…") ?: "",
                                color = SubText, fontSize = 12.sp
                            )
                            Button(
                                onClick = { openGuide = guide },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealCol)
                            ) { Text("Open Guide", color = Color.White, fontSize = 13.sp) }
                        }
                    }
                }
                BottomNavBar(listOf("← BACK" to onNavigateBack))
            } else {
                // Guide editor
                val current = openGuide!!
                var editMode by remember { mutableStateOf(false) }
                var editText by remember(current.key) { mutableStateOf(current.content) }

                Column(modifier = Modifier.weight(1f)) {
                    // Action bar
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!editMode) {
                            Button(onClick = { editMode = true }, modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeCol)) {
                                Text("Edit", color = Color.White, fontSize = 12.sp)
                            }
                        } else {
                            Button(onClick = {
                                val idx = guides.indexOfFirst { it.key == current.key }
                                if (idx >= 0) {
                                    guides = guides.toMutableList().also { it[idx] = it[idx].copy(content = editText) }
                                    WorkflowGuideStore.save(guides)
                                    openGuide = guides[idx]
                                }
                                editMode = false
                            }, modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = GreenCol)) {
                                Text("Save", color = Color.White, fontSize = 12.sp)
                            }
                        }
                        Button(onClick = { clipboard.setText(AnnotatedString(editText)) },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                            Text("Copy", color = Color.White, fontSize = 12.sp)
                        }
                        Button(onClick = { openGuide = null }, modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = RedCol)) {
                            Text("Close", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0A0A0A))
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (editMode) {
                            BasicTextField(
                                value = editText,
                                onValueChange = { editText = it },
                                textStyle = TextStyle(color = TextCol, fontSize = 13.sp, lineHeight = 20.sp),
                                cursorBrush = SolidColor(TealCol),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(current.content, color = TextCol, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}
