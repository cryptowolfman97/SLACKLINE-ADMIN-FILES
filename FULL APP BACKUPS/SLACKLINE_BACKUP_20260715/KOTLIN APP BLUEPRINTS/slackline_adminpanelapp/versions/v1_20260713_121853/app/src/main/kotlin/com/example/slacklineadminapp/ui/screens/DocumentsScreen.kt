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
import androidx.compose.foundation.lazy.itemsIndexed
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

sealed class DocView {
    object Hub : DocView()
    data class Vault(val folderId: String) : DocView()
    data class Editor(val folderId: String, val fileId: String?, val isList: Boolean = false) : DocView()
}

data class ChecklistItem(val text: String, val isChecked: Boolean)

@Composable
fun DocumentsScreen(onNavigateBack: () -> Unit) {
    var view by remember { mutableStateOf<DocView>(DocView.Hub) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun snack(msg: String) { scope.launch { snackbarHostState.showSnackbar(msg) } }

    BackHandler(enabled = view !is DocView.Hub) {
        view = when (val v = view) {
            is DocView.Editor -> DocView.Vault(v.folderId)
            is DocView.Vault  -> DocView.Hub
            else              -> DocView.Hub
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.bg).padding(padding)) {
            when (val v = view) {
                is DocView.Hub    -> DocHubView(onNavigateBack, onOpen = { view = DocView.Vault(it) }, ::snack)
                is DocView.Vault  -> DocVaultView(
                    v.folderId, 
                    onBack = { view = DocView.Hub },
                    onEditor = { fid, isList -> view = DocView.Editor(v.folderId, fid, isList) }, 
                    ::snack
                )
                is DocView.Editor -> DocEditorView(
                    v.folderId, 
                    v.fileId, 
                    v.isList,
                    onBack = { view = DocView.Vault(v.folderId) }, 
                    ::snack
                )
            }
        }
    }
}

// ── HUB ───────────────────────────────────────────────────────────────────────

@Composable
private fun DocHubView(onNavigateBack: () -> Unit, onOpen: (String) -> Unit, snack: (String) -> Unit) {
    var folders by remember { mutableStateOf(DocumentsStore.loadAll()) }
    var showDialog by remember { mutableStateOf(false) }
    var editFolderId by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) { folders = DocumentsStore.loadAll() }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = AmberCol, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Documents & Guides", color = AmberCol, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { BodyText("Organise your text files, notes, and documentation.", SubText) }
            item {
                Button(onClick = { editFolderId = null; showDialog = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberCol)) {
                    Text("+ Create Folder", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            item { BodyText("Total Folders: ${folders.size}", SubText) }
            if (folders.isEmpty()) item { AppCard { BodyText("No folders found. Tap Create Folder to start.", SubText) } }
            items(folders.entries.sortedByDescending { it.value.updated }.map { it.toPair() }) { (fid, folder) ->
                AppCard(color = LocalAppColors.current.card2) {
                    Text(folder.name.ifBlank { fid }, color = TextCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Items: ${folder.files.size}  •  Updated: ${folder.updated.take(10)}", color = OrangeCol, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpen(fid) }, modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = AmberCol)) {
                            Text("Open Vault", color = Color.Black, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { editFolderId = fid; showDialog = true },
                            modifier = Modifier.height(36.dp), shape = RoundedCornerShape(8.dp)) {
                            Text("Rename", color = SubText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        BottomNavBar(listOf("← BACK" to onNavigateBack))
    }

    if (showDialog) {
        FolderDialog(editFolderId, onDismiss = { showDialog = false },
            onSaved = { showDialog = false; refresh++ }, snack = snack)
    }
}

@Composable
private fun FolderDialog(editFolderId: String?, onDismiss: () -> Unit, onSaved: () -> Unit, snack: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val existing = remember(editFolderId) { if (editFolderId != null) DocumentsStore.loadAll()[editFolderId] else null }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        ConfirmDialog("Delete Folder", "Delete \"${existing?.name}\"? All files inside will be lost.", "Delete", RedCol,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    val data = DocumentsStore.loadAll(); data.remove(editFolderId); DocumentsStore.saveAll(data)
                    withContext(Dispatchers.Main) { snack("Folder deleted."); onSaved() }
                }
            }, onDismiss = { showDelete = false })
        return
    }

    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBg, titleContentColor = TextCol,
        title = { Text(if (editFolderId != null) "Edit Folder" else "New Folder", fontWeight = FontWeight.Bold) },
        text = { AppTextField(name, { name = it }, "Folder Name") },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editFolderId != null) TextButton(onClick = { showDelete = true }) { Text("Delete", color = RedCol) }
                TextButton(onClick = {
                    if (name.isBlank()) { snack("Name is required."); return@TextButton }
                    scope.launch(Dispatchers.IO) {
                        val data = DocumentsStore.loadAll()
                        val fid = editFolderId ?: DocumentsStore.slugify(name).ifBlank { "folder_${System.currentTimeMillis()}" }
                        val entry = data.getOrPut(fid) { DocumentFolder() }
                        data[fid] = entry.copy(name = name.trim(), updated = DocumentsStore.utcNow())
                        DocumentsStore.saveAll(data)
                        withContext(Dispatchers.Main) { snack("Folder saved."); onSaved() }
                    }
                }) { Text("Save", color = AmberCol, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) } }
    )
}

// ── VAULT ─────────────────────────────────────────────────────────────────────

@Composable
private fun DocVaultView(folderId: String, onBack: () -> Unit, onEditor: (String?, Boolean) -> Unit, snack: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var folder by remember { mutableStateOf(DocumentsStore.loadAll()[folderId]) }
    var refresh by remember { mutableIntStateOf(0) }
    var showDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refresh) { folder = DocumentsStore.loadAll()[folderId] }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                val filename = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    c.moveToFirst(); c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                } ?: "imported.md"
                val title = filename.removeSuffix(".txt").removeSuffix(".md").replaceFirstChar { it.uppercase() }
                val data = DocumentsStore.loadAll()
                data[folderId]?.files?.set(filename, DocumentFile(title, filename, content, DocumentsStore.utcNow()))
                DocumentsStore.saveAll(data)
                withContext(Dispatchers.Main) { snack("Imported $filename."); refresh++ }
            } catch (e: Exception) { withContext(Dispatchers.Main) { snack("Import failed: ${e.message}") } }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Folder: ${folder?.name ?: folderId}", color = AmberCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallDocBtn("Export All", AmberCol) {
                        scope.launch(Dispatchers.IO) {
                            val files = DocumentsStore.loadAll()[folderId]?.files ?: return@launch
                            if (files.isEmpty()) { withContext(Dispatchers.Main) { snack("No files to export.") }; return@launch }
                            val dir = File(DocumentsStore.exportDir(), DocumentsStore.slugify(folder?.name ?: folderId))
                            dir.mkdirs()
                            files.forEach { (id, f) -> File(dir, id).writeText(f.content) }
                            withContext(Dispatchers.Main) { snack("Exported ${files.size} files.") }
                        }
                    }
                    SmallDocBtn("Import File", OrangeCol) { fileLauncher.launch("text/*") }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { onEditor(null, false) }, modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = AmberCol)) {
                        Text("+ New Note", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { onEditor(null, true) }, modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = GreenCol)) {
                        Text("+ New List", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            val files = folder?.files?.entries?.sortedByDescending { it.value.updated } ?: emptyList()
            if (files.isEmpty()) item { AppCard { BodyText("Vault is empty. Create a note or list.", SubText) } }
            
            items(files) { (fileId, doc) ->
                val isChecklist = doc.content.lines().any { it.trim().startsWith("- [ ]") || it.trim().startsWith("- [x]") }
                
                AppCard(color = LocalAppColors.current.card) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(doc.title.ifBlank { "Untitled Note" }, color = OrangeCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Icon(
                            imageVector = if (isChecklist) Icons.Default.Checklist else Icons.Default.Notes,
                            contentDescription = null,
                            tint = SubText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text("Updated: ${doc.updated.take(10)}", color = SubText, fontSize = 11.sp)
                    
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF111111)).padding(8.dp)) {
                        Text(doc.content.take(100).replace("\n", " ") + "…", color = Color(0xFF888888), fontSize = 11.sp)
                    }
                    val clipboard = LocalClipboardManager.current
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        SmallDocChip("Edit",   OrangeCol) { onEditor(fileId, isChecklist) }
                        SmallDocChip("Copy",   AmberCol)  { clipboard.setText(AnnotatedString(doc.content)) }
                        SmallDocChip("Export", GreenCol)  {
                            scope.launch(Dispatchers.IO) {
                                File(DocumentsStore.exportDir(), "Exported_$fileId").writeText(doc.content)
                                withContext(Dispatchers.Main) { snack("Exported $fileId.") }
                            }
                        }
                        SmallDocChip("Delete", RedCol)    { showDelete = fileId }
                    }
                }
            }
        }
        BottomNavBar(listOf("← HUB" to onBack, "REFRESH" to { refresh++ }))
    }

    showDelete?.let { fid ->
        ConfirmDialog("Delete File", "Are you sure you want to delete this note?", "Delete", RedCol,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    val data = DocumentsStore.loadAll()
                    data[folderId]?.files?.remove(fid)
                    DocumentsStore.saveAll(data)
                    withContext(Dispatchers.Main) { snack("Note deleted."); refresh++ }
                }
                showDelete = null
            }, onDismiss = { showDelete = null })
    }
}

// ── EDITOR ────────────────────────────────────────────────────────────────────

@Composable
private fun DocEditorView(folderId: String, fileId: String?, initialIsList: Boolean, onBack: () -> Unit, snack: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    
    val existing = remember(fileId) {
        if (fileId != null) DocumentsStore.loadAll()[folderId]?.files?.get(fileId) else null
    }
    
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var content by remember { mutableStateOf(existing?.content ?: "") }
    var isListMode by remember { mutableStateOf(initialIsList) }

    // FIXED: Explicitly declared state type to prevent compiler fallback to List<Nothing>
    var checklistItems by remember { 
        mutableStateOf<List<ChecklistItem>>(
            if (initialIsList && existing != null) {
                existing.content.lines().filter { it.isNotBlank() }.map { line ->
                    val trimmed = line.trim()
                    val checked = trimmed.startsWith("- [x]", ignoreCase = true)
                    val text = trimmed.removePrefix("- [x] ").removePrefix("- [X] ").removePrefix("- [ ] ").trim()
                    ChecklistItem(text, checked)
                }
            } else {
                emptyList()
            }
        )
    }

    fun save() {
        if (title.isBlank() && content.isBlank() && checklistItems.isEmpty()) {
            snack("Cannot save an empty note."); return 
        }
        
        val safeTitle = title.ifBlank { "Untitled Note" }
        val generatedFilename = fileId ?: "${DocumentsStore.slugify(safeTitle)}_${System.currentTimeMillis()}.md"
        
        val finalContent = if (isListMode) {
            checklistItems.joinToString("\n") { if (it.isChecked) "- [x] ${it.text}" else "- [ ] ${it.text}" }
        } else {
            content
        }

        scope.launch(Dispatchers.IO) {
            val data = DocumentsStore.loadAll()
            val folder = data.getOrPut(folderId) { DocumentFolder() }
            folder.files[generatedFilename] = DocumentFile(safeTitle, generatedFilename, finalContent, DocumentsStore.utcNow())
            data[folderId] = folder.copy(updated = DocumentsStore.utcNow())
            DocumentsStore.saveAll(data)
            withContext(Dispatchers.Main) { snack("Note saved successfully.") }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (fileId != null) "Editing Note" else "New Note", color = AmberCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    TextButton(onClick = { 
                        if (isListMode) {
                            content = checklistItems.joinToString("\n") { if (it.isChecked) "- [x] ${it.text}" else "- [ ] ${it.text}" }
                        } else {
                            checklistItems = content.lines().filter { it.isNotBlank() }.map { ChecklistItem(it, false) }
                        }
                        isListMode = !isListMode 
                    }) {
                        Text(if (isListMode) "Switch to Text" else "Switch to List", color = SubText, fontSize = 12.sp)
                    }
                }
                
                AppTextField(title, { title = it }, "Note Title")
            }
        }

        Box(modifier = Modifier.weight(1f).padding(14.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A0A0A))) {
            if (isListMode) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    itemsIndexed(checklistItems) { index, item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { checked ->
                                    val newList = checklistItems.toMutableList()
                                    newList[index] = item.copy(isChecked = checked)
                                    checklistItems = newList
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AmberCol, uncheckedColor = SubText)
                            )
                            BasicTextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    val newList = checklistItems.toMutableList()
                                    newList[index] = item.copy(text = newText)
                                    checklistItems = newList
                                },
                                textStyle = TextStyle(color = if (item.isChecked) SubText else TextCol, fontSize = 14.sp),
                                cursorBrush = SolidColor(AmberCol),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = {
                                val newList = checklistItems.toMutableList()
                                newList.removeAt(index)
                                checklistItems = newList
                            }) {
                                Icon(Icons.Default.Close, "Remove", tint = RedCol, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    item {
                        TextButton(onClick = { 
                            val newList = checklistItems.toMutableList()
                            newList.add(ChecklistItem("", false))
                            checklistItems = newList
                        }) {
                            Text("+ Add Item", color = GreenCol)
                        }
                    }
                }
            } else {
                BasicTextField(
                    value = content, onValueChange = { content = it },
                    textStyle = TextStyle(color = TextCol, fontSize = 14.sp, lineHeight = 22.sp),
                    cursorBrush = SolidColor(AmberCol), 
                    modifier = Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())
                )
            }
        }

        BottomNavBar(listOf("← DISCARD" to onBack, "SAVE" to { save() }))
    }
}

// ── SMALL ATOMS ───────────────────────────────────────────────────────────────

@Composable
private fun SmallDocBtn(text: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color), contentPadding = PaddingValues(horizontal = 10.dp)) {
        Text(text, color = Color.Black, fontSize = 11.sp)
    }
}

@Composable
private fun SmallDocChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = color.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}
