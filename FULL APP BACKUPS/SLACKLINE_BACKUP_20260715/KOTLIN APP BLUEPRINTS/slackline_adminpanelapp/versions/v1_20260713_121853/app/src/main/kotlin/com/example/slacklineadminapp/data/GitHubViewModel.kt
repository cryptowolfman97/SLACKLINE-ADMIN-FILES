package com.example.slacklineadminapp.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ── Navigation ────────────────────────────────────────────────────────────────

sealed class GitHubView {
    object Hub    : GitHubView()
    object Repos  : GitHubView()
    data class Vault(val path: String = "") : GitHubView()
    data class Editor(val fileName: String, val sha: String?) : GitHubView()
    data class Releases(val repoName: String) : GitHubView()
    
    // ─── ADDED FOR EDIT MODAL/VIEW SCREEN MANAGEMENT ──────────────────────────
    data class EditRelease(val repoName: String, val releaseId: Long) : GitHubView()
}

// ── UI State ─────────────────────────────────────────────────────────────────

data class GitHubUiState(
    val view: GitHubView                     = GitHubView.Hub,
    val accounts: Map<String, GitHubAccount> = emptyMap(),
    val activeAlias: String                  = "",
    val activeOwner: String                  = "",
    val activeRepo: String                   = "",
    val activeBranch: String                 = "main",
    val repos: List<GitHubRepo>              = emptyList(),
    val items: List<GitHubItem>              = emptyList(),
    val branches: List<String>               = emptyList(),
    val fileContent: String                  = "",
    val fileSha: String?                     = null,
    val isLoading: Boolean                   = false,
    val loadingMessage: String               = "Loading...",
    val error: String?                       = null,
    val snackMessage: String?                = null,
    val snackIsError: Boolean                = false,
    val searchQuery: String                  = "",
    val filteredItems: List<GitHubItem>      = emptyList(),
    val releases: List<GitHubRelease>        = emptyList(),
    
    // ─── ADDED TO TRACK SELECTED MODAL DATA ──────────────────────────────────
    val selectedRelease: GitHubRelease?      = null
)

class GitHubViewModel : ViewModel() {

    private val _state = MutableStateFlow(GitHubUiState())
    val state: StateFlow<GitHubUiState> = _state.asStateFlow()

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun currentAccount(): GitHubAccount =
        _state.value.accounts[_state.value.activeAlias] ?: GitHubAccount()

    private fun api(): GitHubApi {
        val acc = currentAccount()
        return GitHubApi(acc.username, acc.token)
    }

    private fun ownerForCall(): String =
        _state.value.activeOwner.ifBlank { currentAccount().username }

    private fun loading(msg: String = "Loading...") =
        _state.update { it.copy(isLoading = true, loadingMessage = msg, error = null) }

    private fun done() = _state.update { it.copy(isLoading = false) }

    private fun snack(msg: String, isError: Boolean = false) =
        _state.update { it.copy(snackMessage = msg, snackIsError = isError) }

    fun clearSnack() = _state.update { it.copy(snackMessage = null) }
    fun clearError() = _state.update { it.copy(error = null) }

    // ── Accounts ──────────────────────────────────────────────────────────

    fun loadAccounts() {
        _state.update { it.copy(accounts = GitHubStorage.loadAccounts()) }
    }

    fun saveAccount(alias: String, username: String, token: String) {
        if (alias.isBlank() || username.isBlank() || token.isBlank()) {
            snack("All fields required.", true); return
        }
        viewModelScope.launch(Dispatchers.IO) {
            loading("Validating token...")
            try {
                val api = GitHubApi(username, token)
                val login = api.validateToken()
                GitHubStorage.saveAccount(alias, GitHubAccount(login, token))
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(accounts = GitHubStorage.loadAccounts()) }
                    done()
                    snack("Account saved. Authenticated as @$login")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Token validation failed: ${e.message}", true)
                }
            }
        }
    }

    fun deleteAccount(alias: String) {
        GitHubStorage.deleteAccount(alias)
        _state.update { it.copy(accounts = GitHubStorage.loadAccounts()) }
        snack("Account removed.")
    }

    // ── Navigation ────────────────────────────────────────────────────────

    fun navigateTo(view: GitHubView) {
        _state.update {
            it.copy(
                view          = view,
                error         = null,
                searchQuery   = "",
                filteredItems = emptyList()
            )
        }
        when (view) {
            is GitHubView.Hub         -> loadAccounts()
            is GitHubView.Repos       -> loadRepos()
            is GitHubView.Vault       -> loadVault(view.path)
            is GitHubView.Editor      -> if (view.sha != null) loadFileContent(view.fileName, view.sha)
            is GitHubView.Releases    -> loadReleases(view.repoName)
            is GitHubView.EditRelease -> loadSingleReleaseDetails(view.repoName, view.releaseId)
        }
    }

    fun connectAccount(alias: String) {
        val acc = _state.value.accounts[alias] ?: return
        _state.update {
            it.copy(
                activeAlias  = alias,
                activeOwner  = acc.username,
                activeBranch = "main"
            )
        }
        navigateTo(GitHubView.Repos)
    }

    fun navigateUp() {
        val current = (_state.value.view as? GitHubView.Vault)?.path ?: return
        val parent = current.substringBeforeLast('/', "")
        navigateTo(GitHubView.Vault(parent))
    }

    fun openDirectory(item: GitHubItem) {
        navigateTo(GitHubView.Vault(item.path))
    }

    fun openEditor(item: GitHubItem) {
        navigateTo(GitHubView.Editor(item.name, item.sha))
        _state.update { it.copy(fileSha = item.sha, fileContent = "") }
    }

    fun openNewFile() {
        navigateTo(GitHubView.Editor("", null))
        _state.update { it.copy(fileContent = "", fileSha = null) }
    }

    // ── Repositories ──────────────────────────────────────────────────────

    fun loadRepos() {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Loading repositories...")
            try {
                val repos = api().listRepos()
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(repos = repos) }
                    done()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    _state.update { it.copy(error = e.message) }
                }
            }
        }
    }

    fun createRepo(name: String, description: String, private: Boolean) {
        if (name.isBlank()) { snack("Repository name required.", true); return }
        viewModelScope.launch(Dispatchers.IO) {
            loading("Creating repository...")
            try {
                api().createRepo(name, description, private)
                withContext(Dispatchers.Main) {
                    done()
                    snack("Repository \"$name\" created.")
                    loadRepos()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Failed: ${e.message}", true)
                }
            }
        }
    }

    fun deleteRepo(repo: GitHubRepo) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Deleting repository...")
            try {
                api().deleteRepo(ownerForCall(), repo.name)
                withContext(Dispatchers.Main) {
                    done()
                    snack("Repository \"${repo.name}\" deleted.")
                    navigateTo(GitHubView.Repos)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Delete failed: ${e.message}", true)
                }
            }
        }
    }

    fun openRepo(repo: GitHubRepo) {
        val owner = repo.owner.login.ifBlank { currentAccount().username }
        _state.update {
            it.copy(
                activeOwner  = owner,
                activeRepo   = repo.name,
                activeBranch = repo.default_branch
            )
        }
        navigateTo(GitHubView.Vault(""))
        loadBranches(owner, repo.name)
    }

    fun switchBranch(branch: String) {
        val path = (_state.value.view as? GitHubView.Vault)?.path ?: ""
        _state.update { it.copy(activeBranch = branch) }
        loadVault(path)
    }

    private fun loadBranches(owner: String, repo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val branches = api().listBranches(owner, repo)
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(branches = branches) }
                }
            } catch (_: Exception) { /* non-fatal */ }
        }
    }

    // ── File Vault ────────────────────────────────────────────────────────

    fun loadVault(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Loading directory...")
            try {
                val items = api().listContents(
                    ownerForCall(), _state.value.activeRepo, path, _state.value.activeBranch
                ).sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(items = items, filteredItems = items) }
                    done()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    _state.update { it.copy(error = e.message, items = emptyList()) }
                }
            }
        }
    }

    fun searchItems(query: String) {
        _state.update { st ->
            val filtered = if (query.isBlank()) st.items
            else st.items.filter { it.name.contains(query, ignoreCase = true) }
            st.copy(searchQuery = query, filteredItems = filtered)
        }
    }

    fun createDirectory(name: String) {
        if (name.isBlank()) { snack("Name required.", true); return }
        val currentPath = (_state.value.view as? GitHubView.Vault)?.path ?: ""
        val placeholder = buildPath(currentPath, name, ".gitkeep")
        viewModelScope.launch(Dispatchers.IO) {
            loading("Creating directory...")
            try {
                api().putFile(
                    ownerForCall(), _state.value.activeRepo,
                    placeholder, "", "Create directory $name",
                    _state.value.activeBranch, null
                )
                withContext(Dispatchers.Main) {
                    done()
                    snack("Directory \"$name\" created.")
                    navigateTo(GitHubView.Vault(currentPath))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Failed: ${e.message}", true)
                }
            }
        }
    }

    fun deleteFile(item: GitHubItem) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Deleting file...")
            try {
                api().deleteFile(
                    ownerForCall(), _state.value.activeRepo,
                    item.path, item.sha,
                    "Delete ${item.name}", _state.value.activeBranch
                )
                withContext(Dispatchers.Main) {
                    done()
                    snack("${item.name} deleted.")
                    val vaultPath = (_state.value.view as? GitHubView.Vault)?.path ?: ""
                    navigateTo(GitHubView.Vault(vaultPath))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Delete failed: ${e.message}", true)
                }
            }
        }
    }

    // ── File Editor ───────────────────────────────────────────────────────

    fun loadFileContent(filePath: String, sha: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Loading file...")
            try {
                val (_, content) = api().getFileContent(
                    ownerForCall(), _state.value.activeRepo, filePath, _state.value.activeBranch
                )
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(fileContent = content, fileSha = sha) }
                    done()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    _state.update { it.copy(fileContent = "Error loading: ${e.message}") }
                }
            }
        }
    }

    fun commitFile(
        oldFileName: String,
        newFileName: String,
        content: String,
        commitMessage: String
    ) {
        if (newFileName.isBlank()) { snack("Filename required.", true); return }
        val currentPath = (_state.value.view as? GitHubView.Vault)?.path ?: run {
            (_state.value.view as? GitHubView.Editor)?.let { "" } ?: ""
        }
        val newPath = buildPath(currentPath, newFileName)
        val oldPath = if (oldFileName.isNotBlank()) buildPath(currentPath, oldFileName) else null
        val renaming = oldPath != null && oldFileName != newFileName
        val sha = _state.value.fileSha

        viewModelScope.launch(Dispatchers.IO) {
            loading("Committing...")
            try {
                val api = api()
                if (renaming && oldPath != null) {
                    api.putFile(ownerForCall(), _state.value.activeRepo, newPath, content,
                        commitMessage, _state.value.activeBranch, null)
                    if (sha != null) {
                        api.deleteFile(ownerForCall(), _state.value.activeRepo, oldPath, sha,
                            "Rename: $oldFileName → $newFileName", _state.value.activeBranch)
                    }
                } else {
                    val shaToUse = if (oldFileName == newFileName) sha else null
                    api.putFile(ownerForCall(), _state.value.activeRepo, newPath, content,
                        commitMessage, _state.value.activeBranch, shaToUse)
                }
                withContext(Dispatchers.Main) {
                    done()
                    snack("Committed successfully!")
                    AppStorage.logActivity("GitHub Commit", "Committed $newFileName", _state.value.activeRepo)
                    navigateTo(GitHubView.Vault(currentPath))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Commit failed: ${e.message}", true)
                }
            }
        }
    }

    // ── File Upload ───────────────────────────────────────────────────────

    fun uploadFile(localFile: File, remoteName: String, commitMessage: String) {
        if (remoteName.isBlank()) { snack("Remote filename required.", true); return }
        val currentPath = (_state.value.view as? GitHubView.Vault)?.path ?: ""
        val remotePath = buildPath(currentPath, remoteName)

        viewModelScope.launch(Dispatchers.IO) {
            loading("Uploading...")
            try {
                val bytes = localFile.readBytes()
                api().putBinaryFile(
                    ownerForCall(), _state.value.activeRepo, remotePath,
                    bytes, commitMessage, _state.value.activeBranch, null
                )
                withContext(Dispatchers.Main) {
                    done()
                    snack("\"$remoteName\" uploaded successfully!")
                    AppStorage.logActivity("GitHub Upload", "Uploaded $remoteName", _state.value.activeRepo)
                    navigateTo(GitHubView.Vault(currentPath))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Upload failed: ${e.message}", true)
                }
            }
        }
    }

    // ── Download ──────────────────────────────────────────────────────────

    fun downloadFile(item: GitHubItem) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Downloading...")
            try {
                val (_, content) = api().getFileContent(
                    ownerForCall(), _state.value.activeRepo, item.path, _state.value.activeBranch
                )
                val dir = AppStorage.githubDownloadsDir()
                val dest = File(dir, item.name)
                dest.writeText(content, Charsets.UTF_8)
                withContext(Dispatchers.Main) {
                    done()
                    snack("Downloaded to ${dest.absolutePath}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Download failed: ${e.message}", true)
                }
            }
        }
    }

    // ── Releases Operations Engine ────────────────────────────────────────

    fun openReleases(repoName: String) {
        _state.update { it.copy(activeRepo = repoName) }
        navigateTo(GitHubView.Releases(repoName))
    }

    fun loadReleases(repoName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Fetching deployment variants...")
            try {
                val fetched = api().listReleases(ownerForCall(), repoName)
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(releases = fetched) }
                    done()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    _state.update { it.copy(error = "Releases sync failed: ${e.message}") }
                }
            }
        }
    }

    fun openEditRelease(release: GitHubRelease) {
        _state.update { it.copy(selectedRelease = release) }
        navigateTo(GitHubView.EditRelease(_state.value.activeRepo, release.id))
    }

    private fun loadSingleReleaseDetails(repoName: String, releaseId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Syncing release updates...")
            try {
                val singleRelease = api().getSingleRelease(ownerForCall(), repoName, releaseId)
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(selectedRelease = singleRelease) }
                    done()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    _state.update { it.copy(error = "Failed to load release info: ${e.message}") }
                }
            }
        }
    }

    fun createRelease(
        tagName: String,
        title: String,
        body: String,
        isPrerelease: Boolean,
        isLatest: Boolean,
        attachedFile: File?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Publishing release configuration...")
            try {
                val api = api()
                val currentRepo = _state.value.activeRepo
                val owner = ownerForCall()
                
                val release = api.createRelease(owner, currentRepo, tagName, title, body, isPrerelease, isLatest)
                
                if (attachedFile != null && attachedFile.exists()) {
                    withContext(Dispatchers.Main) { loading("Streaming binary payload asset...") }
                    api.uploadReleaseAsset(owner, currentRepo, release.id, attachedFile)
                }
                
                withContext(Dispatchers.Main) {
                    done()
                    snack("Release $tagName deployed successfully.")
                    navigateTo(GitHubView.Releases(currentRepo))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Deployment Error: ${e.message}", true)
                }
            }
        }
    }

    // ─── ADDED: MODIFY/EDIT AN EXISTING RELEASE CONFIGURATION ─────────────────
    fun updateRelease(
        releaseId: Long,
        tagName: String,
        title: String,
        body: String,
        isPrerelease: Boolean,
        isLatest: Boolean,
        newAttachedFile: File?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Patching active release configuration...")
            try {
                val api = api()
                val currentRepo = _state.value.activeRepo
                val owner = ownerForCall()

                // 1. Update text metadata fields
                api.patchRelease(owner, currentRepo, releaseId, tagName, title, body, isPrerelease, isLatest)

                // 2. Upload optional package asset replacement
                if (newAttachedFile != null && newAttachedFile.exists()) {
                    withContext(Dispatchers.Main) { loading("Uploading new binary replacement package...") }
                    api.uploadReleaseAsset(owner, currentRepo, releaseId, newAttachedFile)
                }

                withContext(Dispatchers.Main) {
                    done()
                    snack("Release updated successfully.")
                    navigateTo(GitHubView.Releases(currentRepo))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Patch failed: ${e.message}", true)
                }
            }
        }
    }

    // ─── ADDED: DELETE AN ATTACHED APK DIRECTLY FROM A RELEASE ────────────────
    fun deleteReleaseAsset(assetId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Purging attached asset package...")
            try {
                val currentRepo = _state.value.activeRepo
                val currentReleaseId = _state.value.selectedRelease?.id ?: return@launch
                
                api().deleteReleaseAsset(ownerForCall(), currentRepo, assetId)
                
                withContext(Dispatchers.Main) {
                    snack("Asset deleted successfully.")
                    // Reload current window details to verify removal
                    loadSingleReleaseDetails(currentRepo, currentReleaseId)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Asset drop failed: ${e.message}", true)
                }
            }
        }
    }

    // ─── ADDED: FUNCTION TO COPY RAW DOWNLOAD URL STRINGS TO DEVICE CLIPBOARD ──
    fun copyAssetLinkToClipboard(context: Context, downloadUrl: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("APK Direct Link", downloadUrl)
            clipboard.setPrimaryClip(clip)
            snack("Link copied to clipboard!")
        } catch (e: Exception) {
            snack("Clipboard access failed.", true)
        }
    }

    fun downloadReleaseAsset(asset: GitHubReleaseAsset) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Streaming archive bundle...")
            try {
                val dir = AppStorage.githubDownloadsDir()
                val destFile = File(dir, asset.name)
                
                api().downloadReleaseAssetUrl(asset.browser_download_url, destFile)
                
                withContext(Dispatchers.Main) {
                    done()
                    snack("Binary verified & written to: ${destFile.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Download transmission failure: ${e.message}", true)
                }
            }
        }
    }

    fun deleteRelease(release: GitHubRelease) {
        viewModelScope.launch(Dispatchers.IO) {
            loading("Tearing down deployment tracking nodes...")
            try {
                val currentRepo = _state.value.activeRepo
                api().deleteRelease(ownerForCall(), currentRepo, release.id)
                withContext(Dispatchers.Main) {
                    done()
                    snack("Release cleared out safely.")
                    loadReleases(currentRepo)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    done()
                    snack("Wipe execution failed: ${e.message}", true)
                }
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private fun buildPath(vararg parts: String): String =
        parts.filter { it.isNotBlank() }.joinToString("/")
}
