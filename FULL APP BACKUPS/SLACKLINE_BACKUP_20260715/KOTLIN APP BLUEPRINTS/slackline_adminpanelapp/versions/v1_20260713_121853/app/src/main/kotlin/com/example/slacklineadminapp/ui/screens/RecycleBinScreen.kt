package com.example.slacklineadminapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slacklineadminapp.data.AppStorage
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class RecycledFile(val moduleLabel: String, val file: File)

class RecycleBinViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<RecycledFile>>(emptyList())
    val items: StateFlow<List<RecycledFile>> get() = _items

    private val _toast = MutableStateFlow("")
    val toast: StateFlow<String> = _toast

    private val _confirmEmpty = MutableStateFlow(false)
    val confirmEmpty: StateFlow<Boolean> = _confirmEmpty

    fun load() {
        val root = AppStorage.recycleBinDir()
        val out = mutableListOf<RecycledFile>()
        root.listFiles()?.filter { it.isDirectory }?.forEach { moduleDir ->
            moduleDir.listFiles()?.filter { it.isFile }?.forEach { f ->
                out.add(RecycledFile(moduleDir.name, f))
            }
        }
        _items.value = out.sortedByDescending { it.file.lastModified() }
    }

    fun consumeToast() { _toast.value = "" }
    fun askConfirmEmpty(show: Boolean) { _confirmEmpty.value = show }

    /** Moves a recycled file back into its original module folder, stripping the timestamp prefix we added. */
    fun restoreFile(item: RecycledFile) = viewModelScope.launch {
        val moduleDir = AppStorage.backupModuleDir(item.moduleLabel)
        if (moduleDir == null) { _toast.value = "Original module folder not found."; return@launch }
        val originalName = item.file.name.split("_", limit = 3).let { parts ->
            if (parts.size == 3) parts[2] else item.file.name
        }
        try {
            item.file.copyTo(File(moduleDir, originalName), overwrite = true)
            item.file.delete()
            _toast.value = "Restored \"$originalName\" to ${item.moduleLabel}."
            load()
        } catch (e: Exception) {
            _toast.value = "Restore failed: ${e.message}"
        }
    }

    fun deleteForever(item: RecycledFile) = viewModelScope.launch {
        item.file.delete()
        _toast.value = "Permanently deleted."
        load()
    }

    fun emptyBin() = viewModelScope.launch {
        AppStorage.recycleBinDir().listFiles()?.forEach { it.deleteRecursively() }
        _confirmEmpty.value = false
        _toast.value = "Recycle Bin emptied."
        load()
    }
}

@Composable
fun RecycleBinScreen(
    onNavigateBack: () -> Unit,
    vm: RecycleBinViewModel = viewModel()
) {
    val appColors = LocalAppColors.current
    val items         by vm.items.collectAsState()
    val toast         by vm.toast.collectAsState()
    val confirmEmpty  by vm.confirmEmpty.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Column(modifier = Modifier.fillMaxSize().background(appColors.bg)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("Recycle Bin", PurpleCol, 20)
            BodyText(
                "Files overwritten by App Backup restores land here instead of being deleted. " +
                    "Currently scoped to backup/restore only.",
                SubText
            )

            if (toast.isNotEmpty()) {
                AppCard(color = CardBg2) { Text(toast, color = TealCol, fontWeight = FontWeight.SemiBold) }
            }

            if (items.isEmpty()) {
                AppCard { BodyText("Recycle Bin is empty.", SubText) }
            } else {
                items.forEach { item ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.file.name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(item.moduleLabel, color = SubText, fontSize = 11.sp)
                            }
                            IconButton(onClick = { vm.restoreFile(item) }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = TealCol)
                            }
                            IconButton(onClick = { vm.deleteForever(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Forever", tint = RedCol)
                            }
                        }
                    }
                }
                ActionButton("Empty Recycle Bin", RedCol, onClick = { vm.askConfirmEmpty(true) })
            }
        }
        BottomNavBar(listOf("BACK" to onNavigateBack, "HOME" to onNavigateBack))
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = "Empty Recycle Bin?",
            message = "This permanently deletes every file currently in the Recycle Bin. This cannot be undone.",
            confirmText = "Empty It",
            confirmColor = RedCol,
            onConfirm = { vm.emptyBin() },
            onDismiss = { vm.askConfirmEmpty(false) }
        )
    }
}
