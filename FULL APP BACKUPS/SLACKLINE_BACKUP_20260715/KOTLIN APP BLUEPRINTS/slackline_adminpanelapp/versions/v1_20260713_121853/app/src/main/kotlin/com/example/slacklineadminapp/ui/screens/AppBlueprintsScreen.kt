package com.example.slacklineadminapp.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// ── Navigation ────────────────────────────────────────────────────────────────

sealed class BpView {
    object Hub : BpView()
    data class Vault(val bpId: String, val path: String = "") : BpView()
    data class Editor(val bpId: String, val path: String, val filePath: String?) : BpView()
}

// ── Entry ─────────────────────────────────────────────────────────────────────

@Composable
fun AppBlueprintsScreen(onNavigateBack: () -> Unit) {
    var view by remember { mutableStateOf<BpView>(BpView.Hub) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun snack(msg: String) { scope.launch { snackbarHostState.showSnackbar(msg) } }

    BackHandler(enabled = view !is BpView.Hub) {
        view = when (val v = view) {
            is BpView.Editor -> BpView.Vault(v.bpId, v.path)
            is BpView.Vault  -> if (v.path.isBlank()) BpView.Hub
                                else BpView.Vault(v.bpId, v.path.substringBeforeLast("/", ""))
            else             -> BpView.Hub
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.bg).padding(padding)) {
            when (val v = view) {
                is BpView.Hub    -> BpHubView(onNavigateBack, onOpen = { view = BpView.Vault(it) }, ::snack)
                is BpView.Vault  -> BpVaultView(v.bpId, v.path,
                    onBack = {
                        view = if (v.path.isBlank()) BpView.Hub
                               else BpView.Vault(v.bpId, v.path.substringBeforeLast("/", ""))
                    },
                    onNavigate = { path -> view = BpView.Vault(v.bpId, path) },
                    onEditor   = { fp -> view = BpView.Editor(v.bpId, v.path, fp) },
                    ::snack)
                is BpView.Editor -> BpEditorView(v.bpId, v.path, v.filePath,
                    onBack = { view = BpView.Vault(v.bpId, v.path) }, ::snack)
            }
        }
    }
}

// ── HUB ───────────────────────────────────────────────────────────────────────

@Composable
private fun BpHubView(onNavigateBack: () -> Unit, onOpen: (String) -> Unit, snack: (String) -> Unit) {
    var bps by remember { mutableStateOf(AppBlueprintsStore.loadAll()) }
    var showDialog by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) { bps = AppBlueprintsStore.loadAll() }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GridView, null, tint = TealCol, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("App Blueprints", color = TealCol, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { BodyText("Store complete app source trees for emergencies.", SubText) }
            item {
                Button(onClick = { editId = null; showDialog = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                    Text("+ New Blueprint", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            item { BodyText("Total Blueprints: ${bps.size}", SubText) }
            if (bps.isEmpty()) item { AppCard { BodyText("No blueprints yet. Tap + New Blueprint.", SubText) } }
            items(bps.entries.sortedByDescending { it.value.updated_at }.map { it.toPair() }) { (bid, bp) ->
                AppCard(color = LocalAppColors.current.card2) {
                    Text(bp.name.ifBlank { bid }, color = TextCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Version: ${bp.version}  •  Files: ${bp.files.size}  •  Updated: ${bp.updated_at.take(10)}", color = SubText, fontSize = 12.sp)
                    if (bp.description.isNotBlank()) Text(bp.description, color = SubText, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpen(bid) }, modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                            Text("Open Vault", color = Color.White, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { editId = bid; showDialog = true }, modifier = Modifier.height(36.dp), shape = RoundedCornerShape(8.dp)) {
                            Text("Settings", color = SubText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        BottomNavBar(listOf("← BACK" to onNavigateBack))
    }

    if (showDialog) {
        BpDialog(editId, onDismiss = { showDialog = false }, onSaved = { showDialog = false; refresh++ }, snack = snack)
    }
}

@Composable
private fun BpDialog(editId: String?, onDismiss: () -> Unit, onSaved: () -> Unit, snack: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val existing = remember(editId) { if (editId != null) AppBlueprintsStore.loadAll()[editId] else null }
    var name    by remember { mutableStateOf(existing?.name ?: "") }
    var version by remember { mutableStateOf(existing?.version ?: "1.0.0") }
    var desc    by remember { mutableStateOf(existing?.description ?: "") }
    var showDel by remember { mutableStateOf(false) }

    if (showDel) {
        ConfirmDialog("Delete Blueprint", "Delete \"${existing?.name}\" and all its files?", "Delete", RedCol,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    val data = AppBlueprintsStore.loadAll(); data.remove(editId); AppBlueprintsStore.saveAll(data)
                    withContext(Dispatchers.Main) { snack("Blueprint deleted."); onSaved() }
                }
            }, onDismiss = { showDel = false })
        return
    }

    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBg, titleContentColor = TextCol,
        title = { Text(if (editId != null) "Edit Blueprint" else "New Blueprint", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(name,    { name = it },    "Blueprint Name")
                AppTextField(version, { version = it }, "Version (e.g. 1.0.0)")
                AppTextField(desc,    { desc = it },    "Description (optional)")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editId != null) TextButton(onClick = { showDel = true }) { Text("Delete", color = RedCol) }
                TextButton(onClick = {
                    if (name.isBlank()) { snack("Name is required."); return@TextButton }
                    scope.launch(Dispatchers.IO) {
                        val data = AppBlueprintsStore.loadAll()
                        val now  = AppBlueprintsStore.utcNow()
                        val bid  = editId ?: AppBlueprintsStore.slugify(name).ifBlank { "bp_${System.currentTimeMillis()}" }
                        if (bid !in data) {
                            val safeName = AppBlueprintsStore.slugify(name)
                            data[bid] = AppBlueprint(
                                name = name.trim(), version = version.trim().ifBlank { "1.0.0" },
                                description = desc.trim(), created_at = now, updated_at = now,
                                files = mutableMapOf(
                                    ".github/workflows/build.yml" to "name: Buildozer CI\n\non:\n  push:\n    branches: [ main ]\n\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v3\n      - name: Build APK\n        run: docker run --rm -v \${{ github.workspace }}:/app kivy/buildozer\n",
                                    "buildozer.spec" to "[app]\ntitle = ${name.trim()}\npackage.name = $safeName\npackage.domain = org.shvertex\nsource.dir = .\nsource.include_exts = py,png,jpg,kv,atlas\nversion = 0.1\nrequirements = python3,kivy,kivymd\norientation = portrait\n\n[buildozer]\nlog_level = 2\n",
                                    "main.py" to "from kivymd.app import MDApp\nfrom kivymd.uix.label import MDLabel\n\nclass ${name.trim().replace(" ", "")}App(MDApp):\n    def build(self):\n        return MDLabel(text='Hello from Blueprint!', halign='center')\n\nif __name__ == '__main__':\n    ${name.trim().replace(" ", "")}App().run()\n"
                                )
                            )
                        } else {
                            data[bid] = data[bid]!!.copy(name = name.trim(), version = version.trim().ifBlank { data[bid]!!.version }, description = desc.trim(), updated_at = now)
                        }
                        AppBlueprintsStore.saveAll(data)
                        withContext(Dispatchers.Main) { snack("Blueprint saved."); onSaved() }
                    }
                }) { Text("Save", color = TealCol, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) } }
    )
}

// ── VAULT ─────────────────────────────────────────────────────────────────────

@Composable
private fun BpVaultView(bpId: String, currentPath: String, onBack: () -> Unit,
                        onNavigate: (String) -> Unit, onEditor: (String?) -> Unit, snack: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bp by remember { mutableStateOf(AppBlueprintsStore.loadAll()[bpId]) }
    var items by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    var showNewDir by remember { mutableStateOf(false) }
    var showDelFile by remember { mutableStateOf<String?>(null) }
    var importMode by remember { mutableStateOf("single") }

    LaunchedEffect(refresh, currentPath) {
        isLoading = true
        withContext(Dispatchers.IO) {
            bp = AppBlueprintsStore.loadAll()[bpId]
            items = AppBlueprintsStore.listPath(bp?.files ?: emptyMap(), currentPath)
        }
        isLoading = false
    }

    // File picker
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                val content = try { bytes.toString(Charsets.UTF_8) } catch (_: Exception) { snack("Binary files not supported."); return@launch }
                val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    c.moveToFirst(); c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                } ?: "file.txt"

                if (importMode == "single") {
                    val remotePath = if (currentPath.isNotBlank()) "$currentPath/$name" else name
                    val data = AppBlueprintsStore.loadAll()
                    data[bpId]?.files?.set(remotePath, content)
                    data[bpId]?.let { data[bpId] = it.copy(updated_at = AppBlueprintsStore.utcNow()) }
                    AppBlueprintsStore.saveAll(data)
                    withContext(Dispatchers.Main) { snack("Imported $name."); refresh++ }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { snack("Error: ${e.message}") } }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${bp?.name ?: bpId}  (${bp?.version ?: ""})", color = TealCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Path: /${currentPath.ifBlank { "(root)" }}", color = SubText, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    BpBtn("New File",   GreenCol)   { onEditor(null) }
                    BpBtn("New Folder", OrangeCol)  { showNewDir = true }
                    BpBtn("Import",     PurpleCol)  { importMode = "single"; fileLauncher.launch("*/*") }
                    BpBtn("Export ZIP", TealCol)    {
                        scope.launch(Dispatchers.IO) {
                            val files = AppBlueprintsStore.loadAll()[bpId]?.files ?: return@launch
                            if (files.isEmpty()) { withContext(Dispatchers.Main) { snack("No files to export.") }; return@launch }
                            val dir = AppBlueprintsStore.exportDir()
                            val zip = File(dir, "${bp?.name ?: bpId}.zip")
                            ZipOutputStream(zip.outputStream()).use { zos ->
                                files.forEach { (path, content) ->
                                    zos.putNextEntry(ZipEntry(path))
                                    zos.write(content.toByteArray(Charsets.UTF_8))
                                    zos.closeEntry()
                                }
                            }
                            withContext(Dispatchers.Main) { snack("Exported to ${zip.name}.") }
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealCol)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (items.isEmpty()) item { AppCard { BodyText("Directory is empty.", SubText) } }
                items(items) { (type, name) ->
                    val fullPath = if (currentPath.isNotBlank()) "$currentPath/$name" else name
                    if (type == "dir") {
                        AppCard(color = LocalAppColors.current.card2) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, null, tint = OrangeCol, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onNavigate(fullPath) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ChevronRight, null, tint = OrangeCol)
                                }
                            }
                        }
                    } else {
                        val size = AppBlueprintsStore.humanBytes(bp?.files?.get(fullPath)?.toByteArray()?.size?.toLong() ?: 0L)
                        AppCard(color = LocalAppColors.current.card) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, null, tint = TealCol, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(size, color = SubText, fontSize = 11.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                BpChip("Edit",   TealCol)   { onEditor(fullPath) }
                                BpChip("Export", BlueCol)   {
                                    scope.launch(Dispatchers.IO) {
                                        val content = AppBlueprintsStore.loadAll()[bpId]?.files?.get(fullPath) ?: ""
                                        val out = File(AppBlueprintsStore.exportDir(), name)
                                        out.writeText(content)
                                        withContext(Dispatchers.Main) { snack("Exported $name.") }
                                    }
                                }
                                BpChip("Delete", RedCol)    { showDelFile = fullPath }
                            }
                        }
                    }
                }
            }
        }

        BottomNavBar(listOf("← BACK" to onBack, "REFRESH" to { refresh++ }))
    }

    if (showNewDir) {
        var dirName by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showNewDir = false }, containerColor = CardBg, titleContentColor = TextCol,
            title = { Text("New Directory", fontWeight = FontWeight.Bold) },
            text = { AppTextField(dirName, { dirName = it }, "Directory name") },
            confirmButton = {
                TextButton(onClick = {
                    if (dirName.isBlank()) { snack("Name required."); return@TextButton }
                    scope.launch(Dispatchers.IO) {
                        val placeholder = if (currentPath.isNotBlank()) "$currentPath/$dirName/.gitkeep" else "$dirName/.gitkeep"
                        val data = AppBlueprintsStore.loadAll()
                        data[bpId]?.files?.set(placeholder, "")
                        data[bpId]?.let { data[bpId] = it.copy(updated_at = AppBlueprintsStore.utcNow()) }
                        AppBlueprintsStore.saveAll(data)
                        withContext(Dispatchers.Main) { snack("Directory \"$dirName\" created."); refresh++; showNewDir = false }
                    }
                }) { Text("Create", color = GreenCol, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showNewDir = false }) { Text("Cancel", color = SubText) } }
        )
    }

    showDelFile?.let { fp ->
        ConfirmDialog("Delete", "Delete \"${fp.substringAfterLast('/')}\"?", "Delete", RedCol,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    val data = AppBlueprintsStore.loadAll()
                    val toRemove = data[bpId]?.files?.keys?.filter { it == fp || it.startsWith("$fp/") } ?: emptyList()
                    toRemove.forEach { data[bpId]?.files?.remove(it) }
                    data[bpId]?.let { data[bpId] = it.copy(updated_at = AppBlueprintsStore.utcNow()) }
                    AppBlueprintsStore.saveAll(data)
                    withContext(Dispatchers.Main) { snack("Deleted."); refresh++ }
                }
                showDelFile = null
            }, onDismiss = { showDelFile = null })
    }
}

// ── EDITOR ────────────────────────────────────────────────────────────────────

@Composable
private fun BpEditorView(bpId: String, currentPath: String, filePath: String?, onBack: () -> Unit, snack: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existing = remember(filePath) {
        if (filePath != null) AppBlueprintsStore.loadAll()[bpId]?.files?.get(filePath) else null
    }
    val defaultFilename = filePath?.substringAfterLast('/') ?: ""
    var filename by remember { mutableStateOf(defaultFilename) }
    var content  by remember { mutableStateOf(existing ?: "") }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                val text = try { bytes.toString(Charsets.UTF_8) } catch (_: Exception) { snack("Binary files not supported."); return@launch }
                val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    c.moveToFirst(); c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                } ?: "file.txt"
                withContext(Dispatchers.Main) { filename = name; content = text; snack("Loaded $name.") }
            } catch (e: Exception) { withContext(Dispatchers.Main) { snack("Error: ${e.message}") } }
        }
    }

    fun save() {
        val fn = filename.trim()
        if (fn.isBlank()) { snack("Filename required."); return }
        scope.launch(Dispatchers.IO) {
            val data = AppBlueprintsStore.loadAll()
            val fullPath = if (filePath != null) filePath
                           else if (currentPath.isNotBlank()) "$currentPath/$fn" else fn
            data[bpId]?.files?.set(fullPath, content)
            data[bpId]?.let { data[bpId] = it.copy(updated_at = AppBlueprintsStore.utcNow()) }
            AppBlueprintsStore.saveAll(data)
            withContext(Dispatchers.Main) { snack("Saved $fn.") }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (filePath != null) "Edit: ${filePath.substringAfterLast('/')}" else "New File", color = TealCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                AppTextField(filename, { if (filePath == null) filename = it }, "Filename",
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { save() }, modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = GreenCol)) {
                        Text("Save", color = Color.White, fontSize = 12.sp)
                    }
                    Button(onClick = { fileLauncher.launch("*/*") }, modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = PurpleCol)) {
                        Text("Import", color = Color.White, fontSize = 12.sp)
                    }
                    Button(onClick = { content = "" }, modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = RedCol)) {
                        Text("Clear", color = Color.White, fontSize = 12.sp)
                    }
                }
                Text("${content.lines().size} lines  •  ${content.length} chars", color = SubText, fontSize = 11.sp)
            }
        }

        Box(modifier = Modifier.weight(1f).padding(14.dp).clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A0A0A)).padding(12.dp).verticalScroll(rememberScrollState())) {
            BasicTextField(
                value = content, onValueChange = { content = it },
                textStyle = TextStyle(color = Color(0xFFCDD3DE), fontSize = 13.sp, fontFamily = FontFamily.Monospace, lineHeight = 20.sp),
                cursorBrush = SolidColor(TealCol), modifier = Modifier.fillMaxWidth()
            )
        }

        BottomNavBar(listOf("← VAULT" to onBack, "SAVE" to { save() }))
    }
}

// ── Atoms ─────────────────────────────────────────────────────────────────────

@Composable
private fun BpBtn(text: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color), contentPadding = PaddingValues(horizontal = 10.dp)) {
        Text(text, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun BpChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = color.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}
