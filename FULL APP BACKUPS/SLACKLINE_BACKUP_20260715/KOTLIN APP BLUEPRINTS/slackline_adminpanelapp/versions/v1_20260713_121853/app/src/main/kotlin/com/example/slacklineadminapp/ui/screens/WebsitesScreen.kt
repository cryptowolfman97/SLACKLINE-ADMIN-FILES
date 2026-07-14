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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ── Navigation states ─────────────────────────────────────────────────────────

sealed class WebView {
    object Hub : WebView()
    data class Vault(val siteId: String) : WebView()
    data class Editor(val siteId: String, val pageId: String?) : WebView()
}

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun WebsitesRegistryScreen(onNavigateBack: () -> Unit) {
    var view by remember { mutableStateOf<WebView>(WebView.Hub) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun snack(msg: String) { scope.launch { snackbarHostState.showSnackbar(msg) } }

    BackHandler(enabled = view !is WebView.Hub) {
        view = when (val v = view) {
            is WebView.Editor -> WebView.Vault(v.siteId)
            is WebView.Vault  -> WebView.Hub
            else              -> WebView.Hub
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.bg).padding(padding)) {
            when (val v = view) {
                is WebView.Hub    -> HubView(onNavigateBack, onOpen = { view = WebView.Vault(it) }, ::snack)
                is WebView.Vault  -> VaultView(v.siteId,
                    onBack    = { view = WebView.Hub },
                    onEditor  = { pid -> view = WebView.Editor(v.siteId, pid) },
                    ::snack)
                is WebView.Editor -> EditorView(v.siteId, v.pageId,
                    onBack = { view = WebView.Vault(v.siteId) },
                    ::snack)
            }
        }
    }
}

// ── HUB ───────────────────────────────────────────────────────────────────────

@Composable
private fun HubView(onNavigateBack: () -> Unit, onOpen: (String) -> Unit, snack: (String) -> Unit) {
    var sites by remember { mutableStateOf(WebsitesStore.loadAll()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editSiteId by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) { sites = WebsitesStore.loadAll() }

    Column(modifier = Modifier.fillMaxSize()) {
        WebHeader("Websites Registry", BlueCol, Icons.Default.Language)
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Button(onClick = { editSiteId = null; showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                    Text("+ Add Website", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            item { BodyText("Total Websites: ${sites.size}", SubText) }
            if (sites.isEmpty()) item { AppCard { BodyText("No websites found. Tap Add Website to start.", SubText) } }
            items(sites.entries.sortedByDescending { it.value.updated }.map { it.toPair() }) { (sid, site) ->
                AppCard(color = LocalAppColors.current.card2) {
                    Text(site.name.ifBlank { sid }, color = TextCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(site.domain.ifBlank { "No domain set" }, color = SubText, fontSize = 12.sp)
                    Text("Pages: ${site.pages.size}  •  Updated: ${site.updated.take(10)}", color = TealCol, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpen(sid) }, modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                            Text("Open Vault", color = Color.White, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { editSiteId = sid; showAddDialog = true },
                            modifier = Modifier.height(36.dp), shape = RoundedCornerShape(8.dp)) {
                            Text("Settings", color = SubText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        BottomNavBar(listOf("← BACK" to onNavigateBack))
    }

    if (showAddDialog) {
        SiteDialog(editSiteId = editSiteId, onDismiss = { showAddDialog = false }, onSaved = { showAddDialog = false; refresh++ }, snack = snack)
    }
}

@Composable
private fun SiteDialog(editSiteId: String?, onDismiss: () -> Unit, onSaved: () -> Unit, snack: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val existing = remember(editSiteId) { if (editSiteId != null) WebsitesStore.loadAll()[editSiteId] else null }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var domain by remember { mutableStateOf(existing?.domain ?: "") }
    var repo by remember { mutableStateOf(existing?.repo ?: "") }
    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        ConfirmDialog("Delete Website", "Delete \"${existing?.name}\"? All pages will be lost.",
            "Delete", RedCol,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    val data = WebsitesStore.loadAll()
                    data.remove(editSiteId)
                    WebsitesStore.saveAll(data)
                    withContext(Dispatchers.Main) { snack("Website deleted."); onSaved() }
                }
            },
            onDismiss = { showDelete = false })
        return
    }

    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBg, titleContentColor = TextCol,
        title = { Text(if (editSiteId != null) "Edit Website" else "New Website", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(name, { name = it }, "Website Name")
                AppTextField(domain, { domain = it }, "Domain (e.g. example.com)")
                AppTextField(repo, { repo = it }, "Repository (optional)")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editSiteId != null) TextButton(onClick = { showDelete = true }) { Text("Delete", color = RedCol) }
                TextButton(onClick = {
                    if (name.isBlank()) { snack("Name is required."); return@TextButton }
                    scope.launch(Dispatchers.IO) {
                        val data = WebsitesStore.loadAll()
                        val sid = editSiteId ?: WebsitesStore.slugify(name).ifBlank { "site_${System.currentTimeMillis()}" }
                        val entry = data.getOrPut(sid) { WebsiteEntry() }
                        data[sid] = entry.copy(name = name.trim(), domain = domain.trim(), repo = repo.trim(), updated = WebsitesStore.utcNow())
                        WebsitesStore.saveAll(data)
                        withContext(Dispatchers.Main) { snack("Website saved."); onSaved() }
                    }
                }) { Text("Save", color = BlueCol, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) } }
    )
}

// ── VAULT ─────────────────────────────────────────────────────────────────────

@Composable
private fun VaultView(siteId: String, onBack: () -> Unit, onEditor: (String?) -> Unit, snack: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var site by remember { mutableStateOf(WebsitesStore.loadAll()[siteId]) }
    var refresh by remember { mutableIntStateOf(0) }
    var showDeletePage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refresh) { site = WebsitesStore.loadAll()[siteId] }

    // ── Single HTML file picker ───────────────────────────────────────────────
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                val filename = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    c.moveToFirst(); c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                } ?: "imported.html"
                val title = filename.removeSuffix(".html").removeSuffix(".htm").replaceFirstChar { it.uppercase() }
                val data = WebsitesStore.loadAll()
                data[siteId]?.pages?.set(filename, WebsitePage(title, filename, content, WebsitesStore.utcNow()))
                data[siteId]?.let { data[siteId] = it.copy(updated = WebsitesStore.utcNow()) }
                WebsitesStore.saveAll(data)
                withContext(Dispatchers.Main) { snack("Imported $filename."); refresh++ }
            } catch (e: Exception) { withContext(Dispatchers.Main) { snack("Import failed: ${e.message}") } }
        }
    }

    // ── Folder picker — imports ALL files recursively ─────────────────────────
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val rootDoc = DocumentFile.fromTreeUri(context, uri)
                    ?: throw Exception("Could not open folder.")
                var count = 0

                // Recursive traversal; relativePath is "" for the root level
                fun traverseAndImport(dir: DocumentFile, relativePath: String) {
                    for (child in dir.listFiles()) {
                        val childName = child.name ?: continue
                        val childRelPath = if (relativePath.isEmpty()) childName else "$relativePath/$childName"
                        if (child.isDirectory) {
                            traverseAndImport(child, childRelPath)
                        } else {
                            val content = context.contentResolver
                                .openInputStream(child.uri)
                                ?.bufferedReader()
                                ?.readText() ?: continue
                            val title = childName
                                .substringBeforeLast(".")
                                .replaceFirstChar { it.uppercase() }
                            val data = WebsitesStore.loadAll()
                            data[siteId]?.pages?.set(
                                childRelPath,
                                WebsitePage(title, childRelPath, content, WebsitesStore.utcNow())
                            )
                            data[siteId]?.let { data[siteId] = it.copy(updated = WebsitesStore.utcNow()) }
                            WebsitesStore.saveAll(data)
                            count++
                        }
                    }
                }

                traverseAndImport(rootDoc, "")
                withContext(Dispatchers.Main) {
                    snack("Imported $count file(s) from folder.")
                    refresh++
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { snack("Folder import failed: ${e.message}") }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Path header
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(site?.name ?: siteId, color = BlueCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Domain: ${site?.domain ?: "-"}  •  Repo: ${site?.repo ?: "-"}", color = SubText, fontSize = 11.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    SmallBtn("Export All", TealCol) {
                        scope.launch(Dispatchers.IO) {
                            val pages = WebsitesStore.loadAll()[siteId]?.pages ?: return@launch
                            if (pages.isEmpty()) { withContext(Dispatchers.Main) { snack("No pages to export.") }; return@launch }
                            val dir = File(WebsitesStore.exportDir(), WebsitesStore.slugify(site?.name ?: siteId))
                            dir.mkdirs()
                            pages.forEach { (id, p) ->
                                // Preserve sub-folder structure on export
                                val outFile = File(dir, id)
                                outFile.parentFile?.mkdirs()
                                outFile.writeText(p.html)
                            }
                            withContext(Dispatchers.Main) { snack("Exported ${pages.size} pages.") }
                        }
                    }
                    SmallBtn("Import File",   PurpleCol)  { fileLauncher.launch("*/*") }
                    SmallBtn("Import Folder", OrangeCol)  { folderLauncher.launch(null) }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Button(onClick = { onEditor(null) }, modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                    Text("+ Add Page", color = Color.White, fontSize = 13.sp)
                }
            }
            val pages = site?.pages?.entries?.sortedByDescending { it.value.updated } ?: emptyList()
            if (pages.isEmpty()) item { AppCard { BodyText("No pages found. Tap Add Page or Import.", SubText) } }
            items(pages) { (pageId, page) ->
                AppCard(color = LocalAppColors.current.card) {
                    Text(page.title.ifBlank { pageId }, color = TealCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("$pageId  •  Updated: ${page.updated.take(10)}", color = SubText, fontSize = 11.sp)
                    // HTML / content snippet
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF111111)).padding(8.dp)) {
                        Text(page.html.take(100).replace("\n", " ") + "…", color = Color(0xFF888888), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val clipboard = LocalClipboardManager.current
                        SmallChip("Edit",   TealCol)  { onEditor(pageId) }
                        SmallChip("Copy",   BlueCol)  { clipboard.setText(AnnotatedString(page.html)) }
                        SmallChip("Export", GreenCol) {
                            scope.launch(Dispatchers.IO) {
                                val f = File(WebsitesStore.exportDir(), "Exported_${pageId.replace("/", "_")}")
                                f.writeText(page.html)
                                withContext(Dispatchers.Main) { snack("Exported $pageId.") }
                            }
                        }
                        SmallChip("Delete", RedCol) { showDeletePage = pageId }
                    }
                }
            }
        }
        BottomNavBar(listOf("← HUB" to onBack, "REFRESH" to { refresh++ }))
    }

    showDeletePage?.let { pid ->
        ConfirmDialog("Delete Page", "Delete \"$pid\"?", "Delete", RedCol,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    val data = WebsitesStore.loadAll()
                    data[siteId]?.pages?.remove(pid)
                    WebsitesStore.saveAll(data)
                    withContext(Dispatchers.Main) { snack("Deleted $pid."); refresh++ }
                }
                showDeletePage = null
            },
            onDismiss = { showDeletePage = null })
    }
}

// ── EDITOR ────────────────────────────────────────────────────────────────────

@Composable
private fun EditorView(siteId: String, pageId: String?, onBack: () -> Unit, snack: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val existing = remember(pageId) {
        if (pageId != null) WebsitesStore.loadAll()[siteId]?.pages?.get(pageId) else null
    }
    var title    by remember { mutableStateOf(existing?.title ?: "") }
    var filename by remember { mutableStateOf(existing?.file ?: pageId ?: "") }
    var html     by remember { mutableStateOf(existing?.html ?: "") }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    c.moveToFirst(); c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                } ?: "file.html"
                withContext(Dispatchers.Main) {
                    title    = name.removeSuffix(".html").removeSuffix(".htm").replaceFirstChar { it.uppercase() }
                    filename = name
                    html     = content
                    snack("Loaded $name into editor.")
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { snack("Error: ${e.message}") } }
        }
    }

    fun save() {
        var fn = filename.trim()
        if (fn.isBlank()) { snack("Filename is required."); return }
        if (!fn.contains(".")) fn += ".html"
        scope.launch(Dispatchers.IO) {
            val data = WebsitesStore.loadAll()
            val site = data.getOrPut(siteId) { WebsiteEntry() }
            if (pageId != null && pageId != fn) site.pages.remove(pageId)
            site.pages[fn] = WebsitePage(title.ifBlank { fn }, fn, html, WebsitesStore.utcNow())
            data[siteId] = site.copy(updated = WebsitesStore.utcNow())
            WebsitesStore.saveAll(data)
            withContext(Dispatchers.Main) { snack("Saved $fn.") }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (pageId != null) "Edit: $pageId" else "New Page", color = TealCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                AppTextField(title,    { title = it },    "Page Title")
                AppTextField(filename, { filename = it }, "Filename (e.g. index.html)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { save() }, modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = GreenCol)) {
                        Text("Save Page", color = Color.White, fontSize = 12.sp)
                    }
                    Button(onClick = { fileLauncher.launch("*/*") }, modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = PurpleCol)) {
                        Text("Import", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).padding(14.dp).clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A0A0A)).padding(12.dp).verticalScroll(rememberScrollState())) {
            BasicTextField(
                value = html, onValueChange = { html = it },
                textStyle = TextStyle(color = Color(0xFFCDD3DE), fontSize = 13.sp, fontFamily = FontFamily.Monospace, lineHeight = 20.sp),
                cursorBrush = SolidColor(TealCol), modifier = Modifier.fillMaxWidth()
            )
        }

        BottomNavBar(listOf("← VAULT" to onBack, "SAVE" to { save() }))
    }
}

// ── Shared atoms ──────────────────────────────────────────────────────────────

@Composable
private fun WebHeader(title: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun SmallBtn(text: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 10.dp)) {
        Text(text, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun SmallChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = color.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}
