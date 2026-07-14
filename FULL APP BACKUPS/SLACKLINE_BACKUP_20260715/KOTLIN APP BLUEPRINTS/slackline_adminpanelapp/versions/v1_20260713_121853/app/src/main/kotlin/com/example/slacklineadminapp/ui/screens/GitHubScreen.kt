package com.example.slacklineadminapp.ui.screens

import androidx.activity.compose.BackHandler
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.data.CloudPresetsStore
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GitHubManagerScreen(
    onNavigateBack: () -> Unit,
    vm: GitHubViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Boot
    LaunchedEffect(Unit) { vm.loadAccounts() }
    
    // Intercept Android back button
    BackHandler(enabled = state.view != GitHubView.Hub) {
        when (state.view) {
            is GitHubView.Repos       -> vm.navigateTo(GitHubView.Hub)
            is GitHubView.Vault       -> {
                val path = (state.view as GitHubView.Vault).path
                if (path.isBlank()) vm.navigateTo(GitHubView.Repos)
                else vm.navigateUp()
            }
            is GitHubView.Editor      -> vm.navigateTo(GitHubView.Vault(""))
            is GitHubView.Releases -> vm.navigateTo(GitHubView.Repos)
            is GitHubView.EditRelease -> vm.navigateTo(GitHubView.Releases(state.activeRepo))
            else -> { /* Hub — let system handle it */ }
        }
    }
    
    // Snackbar host
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackMessage) {
        state.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalAppColors.current.bg)
                .padding(padding)
        ) {
            // Content
            AnimatedContent(
                targetState = state.view,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
                },
                label = "github_nav"
            ) { view ->
                when (view) {
                    is GitHubView.Hub         -> HubView(state, vm, onNavigateBack)
                    is GitHubView.Repos       -> ReposView(state, vm, context)
                    is GitHubView.Vault       -> VaultView(state, vm, context)
                    is GitHubView.Editor      -> EditorView(state, vm)
                    is GitHubView.Releases    -> ReleasesView(state, vm, context)
                    is GitHubView.EditRelease -> EditReleaseView(state, vm, context)
                }
            }

            // Loading overlay
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CircularProgressIndicator(color = TealCol, strokeWidth = 3.dp)
                        Text(state.loadingMessage, color = TextCol, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HUB – Account Management
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HubView(state: GitHubUiState, vm: GitHubViewModel, onNavigateBack: () -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GhScreenHeader(
                    title = "GitHub Manager",
                    subtitle = "Connect Personal Access Tokens (PATs) to manage repositories.",
                    icon = Icons.Default.Hub,
                    iconColor = PurpleCol
                )
            }
            item {
                ActionButton("+ Add Account", PurpleCol) { showAddDialog = true }
            }

            if (state.accounts.isEmpty()) {
                item {
                    AppCard {
                        BodyText("No accounts connected. Tap '+ Add Account' to get started.", SubText)
                    }
                }
            } else {
                items(state.accounts.entries.toList(), key = { it.key }) { (alias, acc) ->
                    AccountCard(alias = alias, account = acc, vm = vm)
                }
            }
        }

        BottomNavBar(listOf("BACK" to onNavigateBack))
    }

    if (showAddDialog) {
        AddAccountDialog(onDismiss = { showAddDialog = false }, onSave = { alias, user, token ->
            vm.saveAccount(alias, user, token)
            showAddDialog = false
        })
    }
}

@Composable
private fun AccountCard(alias: String, account: GitHubAccount, vm: GitHubViewModel) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    val appColors = LocalAppColors.current

    AppCard(color = appColors.card2) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, null, tint = PurpleCol, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(alias, color = TextCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("@${account.username}  •  Token: ••••••••",
                    color = SubText, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.connectAccount(alias) },
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleCol)
            ) { Text("Connect", color = Color.White, fontSize = 13.sp) }

            OutlinedButton(
                onClick = { showConfirmDelete = true },
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, RedCol)
            ) { Text("Remove", color = RedCol, fontSize = 13.sp) }
        }
    }

    if (showConfirmDelete) {
        ConfirmDialog(
            title = "Remove Account",
            message = "Remove \"$alias\"? You can re-add it at any time.",
            confirmText = "Remove",
            confirmColor = RedCol,
            onConfirm = { vm.deleteAccount(alias) },
            onDismiss = { showConfirmDelete = false }
        )
    }
}

@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var alias by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var showPresetMenu by remember { mutableStateOf(false) }

    val adminPresets = remember {
        CloudPresetsStore.loadAll().filter { it.type == "github_admin" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        titleContentColor = TextCol,
        title = { Text("Add GitHub Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(alias, { alias = it }, "Account Alias (e.g. Work)")
                AppTextField(username, { username = it }, "GitHub Username")
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Personal Access Token (PAT)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, tint = SubText
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleCol, unfocusedBorderColor = SubText.copy(alpha = 0.4f),
                        focusedLabelColor = PurpleCol, unfocusedLabelColor = SubText,
                        focusedTextColor = TextCol, unfocusedTextColor = TextCol, cursorColor = PurpleCol
                    )
                )

                Box {
                    Button(
                        onClick = { showPresetMenu = true },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanCol)
                    ) {
                        Icon(Icons.Default.CloudDownload, null,
                            modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Load from Cloud Preset", color = Color.White, fontSize = 13.sp)
                    }
                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { showPresetMenu = false },
                        modifier = Modifier.background(CardBg2)
                    ) {
                        if (adminPresets.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No GitHub Admin presets found", color = SubText, fontSize = 13.sp) },
                                onClick = { showPresetMenu = false }
                            )
                        } else {
                            adminPresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.name, color = TextCol, fontSize = 13.sp) },
                                    onClick = {
                                        alias    = preset.alias.ifBlank { preset.name }
                                        username = preset.username
                                        token    = preset.token
                                        showPresetMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    "PATs need: repo, delete_repo (optional) scopes.",
                    color = SubText, fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(alias.trim(), username.trim(), token.trim()) }) {
                Text("SAVE", color = PurpleCol, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = SubText) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// REPOS – Repository List
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReposView(state: GitHubUiState, vm: GitHubViewModel, context: android.content.Context) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<GitHubRepo?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRepos = remember(state.repos, searchQuery) {
        if (searchQuery.isBlank()) state.repos
        else state.repos.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GhScreenHeader(
                    title = "Repositories",
                    subtitle = "@${state.activeOwner}",
                    icon = Icons.Default.FolderSpecial,
                    iconColor = PurpleCol
                )
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search repositories...", color = SubText, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = SubText) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = SubText)
                        }}
                    } else null,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleCol, unfocusedBorderColor = SubText.copy(0.3f),
                        focusedTextColor = TextCol, unfocusedTextColor = TextCol, cursorColor = PurpleCol
                    )
                )
            }
            item {
                ActionButton("+ New Repository", GreenCol) { showCreateDialog = true }
            }

            if (state.error != null) {
                item { ErrorBanner(state.error) { vm.clearError() } }
            }

            if (filteredRepos.isEmpty() && !state.isLoading) {
                item {
                    AppCard { BodyText("No repositories found.", SubText) }
                }
            }

            items(filteredRepos, key = { it.id }) { repo ->
                RepoCard(
                    repo = repo, 
                    onOpen = { vm.openRepo(repo) },
                    onOpenReleases = { vm.openReleases(repo.name) },
                    onDelete = { showDeleteConfirm = repo }
                )
            }
        }

        BottomNavBar(listOf(
            "← BACK" to { vm.navigateTo(GitHubView.Hub) },
            "REFRESH" to { vm.loadRepos() }
        ))
    }

    if (showCreateDialog) {
        CreateRepoDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, priv ->
                vm.createRepo(name, desc, priv)
                showCreateDialog = false
            }
        )
    }

    showDeleteConfirm?.let { repo ->
        ConfirmDialog(
            title = "Delete Repository",
            message = "Delete \"${repo.name}\" permanently? This cannot be undone.",
            confirmText = "Delete",
            confirmColor = RedCol,
            onConfirm = { vm.deleteRepo(repo); showDeleteConfirm = null },
            onDismiss = { showDeleteConfirm = null }
        )
    }
}

@Composable
private fun RepoCard(repo: GitHubRepo, onOpen: () -> Unit, onOpenReleases: () -> Unit, onDelete: () -> Unit) {
    val appColors = LocalAppColors.current
    AppCard(color = appColors.card) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                if (repo.private) Icons.Default.Lock else Icons.Default.LockOpen,
                null, tint = if (repo.private) OrangeCol else GreenCol,
                modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(repo.name, color = TealCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (!repo.description.isNullOrBlank()) {
                    Text(repo.description, color = SubText, fontSize = 12.sp,
                        maxLines = 2, modifier = Modifier.padding(top = 2.dp))
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!repo.language.isNullOrBlank())
                        GhChip(repo.language, BlueCol)
                    GhChip(if (repo.private) "Private" else "Public",
                        if (repo.private) OrangeCol else GreenCol)
                    GhChip("★ ${repo.stargazers_count}", YellowCol)
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onOpen, modifier = Modifier.weight(1.2f).height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleCol)
            ) { Text("Open", color = Color.White, fontSize = 13.sp) }

            Button(
                onClick = onOpenReleases, modifier = Modifier.weight(1.2f).height(38.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanCol)
            ) { 
                Icon(Icons.Default.NewReleases, null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Releases", color = Color.White, fontSize = 12.sp) 
            }

            OutlinedButton(
                onClick = onDelete, modifier = Modifier.height(38.dp),
                shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, RedCol)
            ) { Text("Delete", color = RedCol, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun CreateRepoDialog(onDismiss: () -> Unit, onCreate: (String, String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg, titleContentColor = TextCol,
        title = { Text("Create Repository", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(name, { name = it }, "Repository Name")
                AppTextField(description, { description = it }, "Description (optional)")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PurpleCol, checkedTrackColor = PurpleCol.copy(0.4f))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(if (isPrivate) "Private repository" else "Public repository", color = TextCol, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.trim(), description.trim(), isPrivate) }) {
                Text("CREATE", color = GreenCol, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = SubText) } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// VAULT – File Browser
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VaultView(state: GitHubUiState, vm: GitHubViewModel, context: android.content.Context) {
    var showNewDirDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<GitHubItem?>(null) }
    var showDownloadConfirm by remember { mutableStateOf<GitHubItem?>(null) }
    var showBranchMenu by remember { mutableStateOf(false) }

    val currentPath = (state.view as? GitHubView.Vault)?.path ?: ""

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val cr = context.contentResolver
            val fileName = cr.query(selectedUri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (idx >= 0) cursor.getString(idx) else "upload"
            } ?: "upload"
            uploadFileFromUri(context, selectedUri, fileName, vm)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = CardBg2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    "${state.activeOwner}/${state.activeRepo}",
                    color = TealCol, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "/ ${if (currentPath.isBlank()) "(root)" else currentPath}",
                        color = SubText, fontSize = 12.sp, modifier = Modifier.weight(1f)
                    )
                    Box {
                        TextButton(onClick = { showBranchMenu = true }) {
                            Icon(Icons.Default.AccountTree, null, tint = PurpleCol, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(state.activeBranch, color = PurpleCol, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, null, tint = PurpleCol)
                        }
                        DropdownMenu(
                            expanded = showBranchMenu,
                            onDismissRequest = { showBranchMenu = false },
                            modifier = Modifier.background(CardBg2)
                        ) {
                            state.branches.forEach { branch ->
                                DropdownMenuItem(
                                    text = { Text(branch, color = TextCol, fontSize = 13.sp) },
                                    onClick = { vm.switchBranch(branch); showBranchMenu = false },
                                    leadingIcon = {
                                        if (branch == state.activeBranch)
                                            Icon(Icons.Default.Check, null, tint = TealCol, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { vm.searchItems(it) },
            placeholder = { Text("Filter files...", color = SubText, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = SubText) },
            trailingIcon = if (state.searchQuery.isNotEmpty()) {
                { IconButton(onClick = { vm.searchItems("") }) {
                    Icon(Icons.Default.Clear, null, tint = SubText)
                }}
            } else null,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurpleCol, unfocusedBorderColor = SubText.copy(0.3f),
                focusedTextColor = TextCol, unfocusedTextColor = TextCol, cursorColor = PurpleCol
            )
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallActionBtn("+ File", GreenCol, Modifier.weight(1f)) { vm.openNewFile() }
            SmallActionBtn("+ Folder", OrangeCol, Modifier.weight(1f)) { showNewDirDialog = true }
            SmallActionBtn("Upload", PurpleCol, Modifier.weight(1f)) { fileLauncher.launch("*/*") }
        }
        Spacer(Modifier.height(8.dp))

        if (state.error != null) {
            ErrorBanner(state.error, modifier = Modifier.padding(horizontal = 16.dp)) { vm.clearError() }
            Spacer(Modifier.height(8.dp))
        }

        val displayItems = if (state.searchQuery.isNotEmpty()) state.filteredItems else state.items
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (displayItems.isEmpty() && !state.isLoading) {
                item { AppCard { BodyText("Directory is empty.", SubText) } }
            }
            items(displayItems, key = { it.path }) { item ->
                if (item.isDirectory) {
                    DirectoryCard(item, onOpen = { vm.openDirectory(item) })
                } else {
                    FileCard(
                        item,
                        onEdit    = { vm.openEditor(item) },
                        onDownload = { showDownloadConfirm = item },
                        onDelete  = { showDeleteConfirm = item }
                    )
                }
            }
        }

        BottomNavBar(listOf(
            "← REPOS"   to { vm.navigateTo(GitHubView.Repos) },
            "↑ UP"      to { vm.navigateUp() },
            "REFRESH"   to {
                val path = (state.view as? GitHubView.Vault)?.path ?: ""
                vm.loadVault(path)
            }
        ))
    }

    if (showNewDirDialog) {
        SimpleInputDialog(
            title = "New Directory",
            label = "Directory name",
            confirmText = "Create",
            confirmColor = GreenCol,
            onDismiss = { showNewDirDialog = false },
            onConfirm = { name -> vm.createDirectory(name); showNewDirDialog = false }
        )
    }

    showDeleteConfirm?.let { item ->
        ConfirmDialog(
            title = "Delete File",
            message = "Delete \"${item.name}\" permanently?",
            confirmText = "Delete", confirmColor = RedCol,
            onConfirm = { vm.deleteFile(item); showDeleteConfirm = null },
            onDismiss = { showDeleteConfirm = null }
        )
    }

    showDownloadConfirm?.let { item ->
        ConfirmDialog(
            title = "Download File",
            message = "Save \"${item.name}\" to Downloads/SLACKLINE ADMIN FILES/GitHub Downloads?",
            confirmText = "Download", confirmColor = PurpleCol,
            onConfirm = { vm.downloadFile(item); showDownloadConfirm = null },
            onDismiss = { showDownloadConfirm = null }
        )
    }
}

@Composable
private fun DirectoryCard(item: GitHubItem, onOpen: () -> Unit) {
    val appColors = LocalAppColors.current
    AppCard(color = appColors.card) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, null, tint = OrangeCol, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(item.name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                modifier = Modifier.weight(1f))
            IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, null, tint = OrangeCol)
            }
        }
    }
}

@Composable
private fun FileCard(item: GitHubItem, onEdit: () -> Unit, onDownload: () -> Unit, onDelete: () -> Unit) {
    val appColors = LocalAppColors.current
    val ext = item.name.substringAfterLast('.', "").lowercase()
    val iconTint = when (ext) {
        "json"               -> TealCol
        "kt", "java"        -> PurpleCol
        "py"                 -> BlueCol
        "md", "txt"          -> TextCol
        "png", "jpg", "jpeg" -> GreenCol
        else                 -> SubText
    }
    AppCard(color = appColors.card) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(fileIcon(ext), null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(item.sizeFormatted, color = SubText, fontSize = 11.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GhActionChip("Edit", TealCol)   { onEdit() }
            GhActionChip("↓", PurpleCol)    { onDownload() }
            GhActionChip("✕", RedCol)       { onDelete() }
        }
    }
}

private fun fileIcon(ext: String) = when (ext) {
    "json"              -> Icons.Default.DataObject
    "kt", "java", "py" -> Icons.Default.Code
    "md"               -> Icons.Default.Description
    "png", "jpg", "jpeg", "gif", "webp" -> Icons.Default.Image
    "zip", "tar", "gz" -> Icons.Default.Archive
    else               -> Icons.Default.InsertDriveFile
}

// ─────────────────────────────────────────────────────────────────────────────
// EDITOR – File Create / Edit / Rename
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditorView(state: GitHubUiState, vm: GitHubViewModel) {
    val editorState = state.view as? GitHubView.Editor
    var fileName by remember(editorState?.fileName) {
        mutableStateOf(editorState?.fileName ?: "")
    }
    var content by remember { mutableStateOf("") }
    var commitMsg by remember { mutableStateOf("") }
    var showCommitDialog by remember { mutableStateOf(false) }
    var wordWrap by remember { mutableStateOf(true) }
    val isNewFile = editorState?.sha == null

    LaunchedEffect(state.fileContent) {
        if (state.fileContent.isNotEmpty() && !state.fileContent.startsWith("Error")) {
            content = state.fileContent
        }
    }

    LaunchedEffect(fileName) {
        commitMsg = if (isNewFile) "Add $fileName"
        else if (fileName != editorState?.fileName) "Rename ${editorState?.fileName} → $fileName"
        else "Update $fileName"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = CardBg2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isNewFile) "New File" else "Edit: ${editorState?.fileName}",
                    color = TealCol, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Filename (edit to rename)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealCol, unfocusedBorderColor = SubText.copy(0.4f),
                        focusedLabelColor = TealCol, focusedTextColor = TextCol, unfocusedTextColor = TextCol,
                        cursorColor = TealCol
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallActionBtn("Commit & Push", GreenCol, Modifier.weight(1f)) {
                        showCommitDialog = true
                    }
                    IconButton(onClick = { wordWrap = !wordWrap }, modifier = Modifier
                        .size(40.dp)
                        .background(if (wordWrap) PurpleCol.copy(0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.WrapText, null, tint = if (wordWrap) PurpleCol else SubText, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { content = "" }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Clear, null, tint = RedCol, modifier = Modifier.size(20.dp))
                    }
                }

                val lines = content.lines().size
                val chars = content.length
                Text("$lines lines  •  $chars chars",
                    color = SubText, fontSize = 11.sp)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0A0A0A))
                .padding(4.dp)
                .verticalScroll(rememberScrollState())
        ) {
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                textStyle = TextStyle(
                    color = Color(0xFFCDD3DE),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(TealCol),
                maxLines = Int.MAX_VALUE,
                singleLine = false
            )
        }

        BottomNavBar(listOf(
            "← BACK" to { vm.navigateTo(GitHubView.Vault((state.view as? GitHubView.Editor)?.let { "" } ?: "")) },
            "COMMIT" to { showCommitDialog = true }
        ))
    }

    if (showCommitDialog) {
        CommitDialog(
            suggestedMessage = commitMsg,
            onDismiss = { showCommitDialog = false },
            onCommit = { msg ->
                vm.commitFile(
                    oldFileName = editorState?.fileName ?: "",
                    newFileName = fileName.trim(),
                    content = content,
                    commitMessage = msg.ifBlank { commitMsg }
                )
                showCommitDialog = false
            }
        )
    }
}

@Composable
private fun CommitDialog(
    suggestedMessage: String,
    onDismiss: () -> Unit,
    onCommit: (String) -> Unit
) {
    var msg by remember { mutableStateOf(suggestedMessage) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg, titleContentColor = TextCol,
        title = { Text("Commit & Push", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter a commit message:", color = SubText, fontSize = 13.sp)
                AppTextField(msg, { msg = it }, "Commit message", singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = { onCommit(msg.trim()) }) {
                Text("COMMIT", color = GreenCol, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = SubText) } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// RELEASES – App Deployment and Binary Assets Engine
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReleasesView(state: GitHubUiState, vm: GitHubViewModel, context: android.content.Context) {
    var showCreateReleaseDialog by remember { mutableStateOf(false) }
    var selectedAssetForDownload by remember { mutableStateOf<GitHubReleaseAsset?>(null) }
    var selectedReleaseForDeletion by remember { mutableStateOf<GitHubRelease?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = CardBg2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    "${state.activeOwner}/${state.activeRepo}",
                    color = TealCol, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
                Text(
                    "App Releases & Production Binaries",
                    color = SubText, fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ActionButton("+ Create New Release", CyanCol) { showCreateReleaseDialog = true }
            }

            if (state.error != null) {
                item { ErrorBanner(state.error) { vm.clearError() } }
            }

            if (state.releases.isEmpty() && !state.isLoading) {
                item {
                    AppCard {
                        BodyText("No app releases found for this repository.", SubText)
                    }
                }
            }

            items(state.releases, key = { it.id }) { release ->
                ReleaseCard(
                    release = release,
                    onDownloadAsset = { selectedAssetForDownload = it },
                    onEditRelease = { vm.openEditRelease(release) },
                    onDeleteRelease = { selectedReleaseForDeletion = release },
                    vm = vm
                )
            }
        }

        BottomNavBar(listOf(
            "← REPOS" to { vm.navigateTo(GitHubView.Repos) },
            "REFRESH" to { vm.loadReleases(state.activeRepo) }
        ))
    }

    if (showCreateReleaseDialog) {
        CreateReleaseDialog(
            context = context,
            onDismiss = { showCreateReleaseDialog = false },
            onCreate = { tagName, title, body, isPrerelease, isLatest, attachedFile ->
                vm.createRelease(tagName, title, body, isPrerelease, isLatest, attachedFile)
                showCreateReleaseDialog = false
            }
        )
    }

    selectedAssetForDownload?.let { asset ->
        ConfirmDialog(
            title = "Download Production Binary",
            message = "Save asset \"${asset.name}\" (${asset.sizeFormatted}) into your secure Downloads directory?",
            confirmText = "Download Asset",
            confirmColor = PurpleCol,
            onConfirm = { 
                vm.downloadReleaseAsset(asset)
                selectedAssetForDownload = null 
            },
            onDismiss = { selectedAssetForDownload = null }
        )
    }

    selectedReleaseForDeletion?.let { release ->
        ConfirmDialog(
            title = "Delete Release Tag",
            message = "Permanently wipe out release \"${release.name.ifBlank { release.tagName }}\"? This action cannot be reversed.",
            confirmText = "Delete Release",
            confirmColor = RedCol,
            onConfirm = {
                vm.deleteRelease(release)
                selectedReleaseForDeletion = null
            },
            onDismiss = { selectedReleaseForDeletion = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReleaseCard(
    release: GitHubRelease,
    onDownloadAsset: (GitHubReleaseAsset) -> Unit,
    onEditRelease: () -> Unit,
    onDeleteRelease: () -> Unit,
    vm: GitHubViewModel
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current

    AppCard(color = appColors.card) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = release.name.ifBlank { "Release ${release.tagName}" },
                        color = TextCol, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GhChip(release.tagName, TealCol)
                        if (release.prerelease) {
                            GhChip("Pre-release", OrangeCol)
                        } else if (release.latest) {
                            GhChip("Latest", GreenCol)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditRelease, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, tint = TealCol, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteRelease, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = RedCol, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (!release.body.isNullOrBlank()) {
                Text(
                    text = release.body,
                    color = SubText,
                    fontSize = 12.sp,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
            }

            if (release.assets.isNotEmpty()) {
                Text("Attached Production Artifacts:", color = TextCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    release.assets.forEach { asset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(appColors.card2)
                                .combinedClickable(
                                    onClick = { onDownloadAsset(asset) },
                                    onLongClick = { vm.copyAssetLinkToClipboard(context, asset.browser_download_url) }
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Android, null, tint = GreenCol, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(asset.name, color = TextCol, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(asset.sizeFormatted, color = SubText, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Default.ContentCopy, null, tint = SubText.copy(alpha = 0.6f), modifier = Modifier.size(14.dp).padding(end = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateReleaseDialog(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onCreate: (tagName: String, title: String, body: String, isPrerelease: Boolean, isLatest: Boolean, file: File?) -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var isPrerelease by remember { mutableStateOf(false) }
    var isLatest by remember { mutableStateOf(true) }
    
    var attachedFile by remember { mutableStateOf<File?>(null) }
    var attachedFileName by remember { mutableStateOf("") }

    val assetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            try {
                val name = context.contentResolver.query(targetUri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    if (index >= 0) cursor.getString(index) else "application-binary.apk"
                } ?: "application-binary.apk"
                
                val tempFile = File(context.cacheDir, name)
                context.contentResolver.openInputStream(targetUri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                attachedFile = tempFile
                attachedFileName = name
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        titleContentColor = TextCol,
        title = { Text("Publish App Release", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                AppTextField(tagName, { tagName = it }, "Tag version (e.g., v11.0)")
                AppTextField(title, { title = it }, "Release title (e.g., SHV Store Download)")
                AppTextField(body, { body = it }, "Release changelog / description", singleLine = false)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isPrerelease,
                        onCheckedChange = { 
                            isPrerelease = it 
                            if (it) isLatest = false
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = PurpleCol, checkedTrackColor = PurpleCol.copy(0.4f))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Set as a pre-release binary", color = TextCol, fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isLatest,
                        onCheckedChange = { 
                            isLatest = it
                            if (it) isPrerelease = false
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = PurpleCol, checkedTrackColor = PurpleCol.copy(0.4f)),
                        enabled = !isPrerelease
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Set as latest production version", color = TextCol, fontSize = 13.sp)
                }

                Spacer(Modifier.height(4.dp))
                Text("Release Binary Asset (.apk, .zip)", color = TextCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                
                if (attachedFile != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Android, null, tint = TealCol)
                            Spacer(Modifier.width(8.dp))
                            Text(attachedFileName, color = TextCol, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = { attachedFile = null; attachedFileName = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = RedCol, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { assetLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SubText.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.AttachFile, null, tint = SubText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Attach Binary Package File", color = TextCol, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    onCreate(tagName.trim(), title.trim(), body.trim(), isPrerelease, isLatest, attachedFile) 
                },
                enabled = tagName.isNotBlank()
            ) {
                Text("PUBLISH", color = CyanCol, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = SubText) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// EDIT RELEASE – Advanced Configuration Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditReleaseView(state: GitHubUiState, vm: GitHubViewModel, context: android.content.Context) {
    val release = state.selectedRelease ?: return

    var tagName by remember(release.tagName) { mutableStateOf(release.tagName) }
    var title by remember(release.name) { mutableStateOf(release.name) }
    var body by remember(release.body) { mutableStateOf(release.body ?: "") }
    var isPrerelease by remember(release.prerelease) { mutableStateOf(release.prerelease) }
    var isLatest by remember(release.latest) { mutableStateOf(release.latest) }
    
    var replacementFile by remember { mutableStateOf<File?>(null) }
    var replacementFileName by remember { mutableStateOf("") }
    
    var assetIdForDeletion by remember { mutableStateOf<Long?>(null) }

    val replacementLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            try {
                val name = context.contentResolver.query(targetUri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    if (index >= 0) cursor.getString(index) else "replaced-asset.apk"
                } ?: "replaced-asset.apk"
                
                val tempFile = File(context.cacheDir, name)
                context.contentResolver.openInputStream(targetUri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                replacementFile = tempFile
                replacementFileName = name
            } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = CardBg2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    "Configure Workspace: ${release.tagName}",
                    color = TealCol, fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
                Text(
                    "Patch release tags, manifests, and binary nodes",
                    color = SubText, fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) {
                item { ErrorBanner(state.error) { vm.clearError() } }
            }

            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Metadata Configuration", color = TealCol, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        
                        AppTextField(tagName, { tagName = it }, "Tag version identifier")
                        AppTextField(title, { title = it }, "Release header banner title")
                        AppTextField(body, { body = it }, "Changelog manifest note details", singleLine = false)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isPrerelease,
                                onCheckedChange = { 
                                    isPrerelease = it 
                                    if (it) isLatest = false
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = PurpleCol, checkedTrackColor = PurpleCol.copy(0.4f))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Mark variant as Pre-release beta", color = TextCol, fontSize = 13.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isLatest,
                                onCheckedChange = { 
                                    isLatest = it
                                    if (it) isPrerelease = false
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = PurpleCol, checkedTrackColor = PurpleCol.copy(0.4f)),
                                enabled = !isPrerelease
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Force set as primary head deployment pointer", color = TextCol, fontSize = 13.sp)
                        }
                    }
                }
            }

            if (release.assets.isNotEmpty()) {
                item {
                    Text("Currently Mapped Remote Binaries:", color = TextCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                items(release.assets, key = { it.id }) { asset ->
                    val appColors = LocalAppColors.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(appColors.card)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Inventory, null, tint = OrangeCol, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(asset.name, color = TextCol, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(asset.sizeFormatted, color = SubText, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { assetIdForDeletion = asset.id }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = RedCol, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stage Replacement Target Binary", color = TextCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (replacementFile != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.CloudUpload, null, tint = TealCol)
                                Spacer(Modifier.width(8.dp))
                                Text(replacementFileName, color = TextCol, fontSize = 12.sp, maxLines = 1)
                            }
                            IconButton(onClick = { replacementFile = null; replacementFileName = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = RedCol, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { replacementLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SubText.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.CloudSync, null, tint = SubText, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Upload New Replacement APK", color = TextCol, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        BottomNavBar(listOf(
            "CANCEL" to { vm.navigateTo(GitHubView.Releases(state.activeRepo)) },
            "APPLY PATCH" to {
                vm.updateRelease(
                    releaseId = release.id,
                    tagName = tagName.trim(),
                    title = title.trim(),
                    body = body.trim(),
                    isPrerelease = isPrerelease,
                    isLatest = isLatest,
                    newAttachedFile = replacementFile
                )
            }
        ))
    }

    assetIdForDeletion?.let { assetId ->
        ConfirmDialog(
            title = "Purge Binary Node Asset",
            message = "Are you absolutely sure you want to remove this binary package from your GitHub remote node mapping completely?",
            confirmText = "Delete Binary Node",
            confirmColor = RedCol,
            onConfirm = {
                vm.deleteReleaseAsset(assetId)
                assetIdForDeletion = null
            },
            onDismiss = { assetIdForDeletion = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Upload helper (system file picker → URI → upload)
// ─────────────────────────────────────────────────────────────────────────────

private fun uploadFileFromUri(
    context: android.content.Context,
    uri: Uri,
    suggestedName: String,
    vm: GitHubViewModel
) {
    try {
        val tmp = File(context.cacheDir, suggestedName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { out -> input.copyTo(out) }
        }
        vm.uploadFile(tmp, suggestedName, "Upload $suggestedName")
    } catch (e: Exception) {
        // ViewModel handles snack trace error
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared small UI atoms
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GhScreenHeader(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = TextCol, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = SubText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GhChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun GhActionChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
private fun SmallActionBtn(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ErrorBanner(message: String?, modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    if (message == null) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RedCol.copy(0.15f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ErrorOutline, null, tint = RedCol, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = RedCol, fontSize = 12.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, null, tint = RedCol, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SimpleInputDialog(
    title: String,
    label: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg, titleContentColor = TextCol,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { AppTextField(text, { text = it }, label) },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text(confirmText.uppercase(), color = confirmColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = SubText) } }
    )
}
