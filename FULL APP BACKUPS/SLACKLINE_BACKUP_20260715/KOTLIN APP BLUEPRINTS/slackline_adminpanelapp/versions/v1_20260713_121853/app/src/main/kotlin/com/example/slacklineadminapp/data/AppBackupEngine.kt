package com.example.slacklineadminapp.data

import java.io.File

/**
 * Full App Backup & Restore engine — Cloud Settings module.
 *
 * Layout in the target repo, under the preset's path:
 *   {path}/{BACKUP_NAME}/_manifest.json
 *   {path}/{BACKUP_NAME}/{MODULE LABEL}/... (mirrors the local module folder exactly)
 *
 * Every local read/write goes through AppStorage.BACKUP_MODULES — nothing here
 * hardcodes a module's folder name or location.
 */

enum class RestoreMode { FULL_REPLACE, MERGE }

data class BackupManifest(
    val name: String = "",
    val createdAt: String = "",
    val modules: List<String> = emptyList(),
    val fileCount: Int = 0
)

data class BackupSummary(
    val manifest: BackupManifest,
    val folderPath: String
)

object AppBackupEngine {

    private fun api(preset: CloudPreset) = GitHubApi(preset.username, preset.token)

    private fun joinPath(vararg parts: String): String =
        parts.filter { it.isNotBlank() }.joinToString("/") { it.trim('/') }

    private fun isTextFile(name: String): Boolean =
        listOf(".json", ".txt", ".pem", ".md", ".xml", ".csv", ".kt", ".py", ".gradle", ".pro")
            .any { name.endsWith(it, ignoreCase = true) }

    /** Recursively lists every file under [root], returning paths relative to [root]. */
    private fun listLocalFilesRecursive(root: File): List<File> {
        if (!root.exists()) return emptyList()
        val out = mutableListOf<File>()
        root.walkTopDown().forEach { f -> if (f.isFile) out.add(f) }
        return out
    }

    // ── BACKUP ───────────────────────────────────────────────────────────────

    /**
     * Uploads every file in [selectedModules] to a new folder named [backupName]
     * inside the preset's repo/path, then writes a manifest describing the backup.
     * [onProgress] is called with a short human-readable line per step.
     */
    fun runBackup(
        preset: CloudPreset,
        backupName: String,
        selectedModules: List<String>,
        onProgress: (String) -> Unit
    ) {
        require(preset.owner.isNotBlank() && preset.repo.isNotBlank()) { "Preset is missing owner/repo." }
        val gh = api(preset)
        var totalFiles = 0

        for (moduleLabel in selectedModules) {
            val localDir = AppStorage.backupModuleDir(moduleLabel) ?: continue
            val files = listLocalFilesRecursive(localDir)
            onProgress("Backing up $moduleLabel (${files.size} file${if (files.size == 1) "" else "s"})…")

            for (file in files) {
                val relativePath = file.relativeTo(localDir).path.replace(File.separatorChar, '/')
                val remotePath = joinPath(preset.path, backupName, moduleLabel, relativePath)
                if (isTextFile(file.name)) {
                    gh.putFile(
                        preset.owner, preset.repo, remotePath,
                        file.readText(Charsets.UTF_8),
                        "Backup: $backupName / $moduleLabel / ${file.name}",
                        preset.branch, sha = null
                    )
                } else {
                    gh.putBinaryFile(
                        preset.owner, preset.repo, remotePath,
                        file.readBytes(),
                        "Backup: $backupName / $moduleLabel / ${file.name}",
                        preset.branch, sha = null
                    )
                }
                totalFiles++
                onProgress("Uploaded $moduleLabel/$relativePath")
            }
        }

        val manifest = BackupManifest(
            name = backupName,
            createdAt = AppStorage.timestamp(),
            modules = selectedModules,
            fileCount = totalFiles
        )
        val manifestPath = joinPath(preset.path, backupName, "_manifest.json")
        gh.putFile(
            preset.owner, preset.repo, manifestPath,
            AppStorage.gson.toJson(manifest),
            "Backup manifest: $backupName",
            preset.branch, sha = null
        )
        onProgress("Backup complete — $totalFiles file${if (totalFiles == 1) "" else "s"} across ${selectedModules.size} module${if (selectedModules.size == 1) "" else "s"}.")
        AppStorage.logActivity("Backup Created", "$backupName (${selectedModules.size} modules, $totalFiles files)", "Cloud Settings")
    }

    // ── LIST BACKUPS ─────────────────────────────────────────────────────────

    /** Lists every backup folder in the preset's path by reading each one's manifest. */
    fun listBackups(preset: CloudPreset): List<BackupSummary> {
        val gh = api(preset)
        val basePath = preset.path
        val items = try {
            gh.listContents(preset.owner, preset.repo, basePath, preset.branch)
        } catch (e: Exception) {
            return emptyList()
        }
        return items.filter { it.isDirectory }.mapNotNull { dir ->
            try {
                val manifestPath = joinPath(dir.path, "_manifest.json")
                val (_, json) = gh.getFileContent(preset.owner, preset.repo, manifestPath, preset.branch)
                val manifest = AppStorage.gson.fromJson(json, BackupManifest::class.java)
                BackupSummary(manifest, dir.path)
            } catch (e: Exception) {
                null // folder without a manifest — not one of our backups, skip it
            }
        }.sortedByDescending { it.manifest.createdAt }
    }

    // ── RESTORE ──────────────────────────────────────────────────────────────

    /**
     * Recursively lists every file under a GitHub folder (contents API only
     * returns one level at a time, so subfolders — e.g. per-product license
     * folders, per-blueprint folders — need to be walked manually).
     */
    private fun listRemoteFilesRecursive(gh: GitHubApi, preset: CloudPreset, remoteDir: String): List<GitHubItem> {
        val out = mutableListOf<GitHubItem>()
        val items = try {
            gh.listContents(preset.owner, preset.repo, remoteDir, preset.branch)
        } catch (e: Exception) {
            return out
        }
        for (item in items) {
            if (item.isDirectory) out.addAll(listRemoteFilesRecursive(gh, preset, item.path))
            else out.add(item)
        }
        return out
    }

    fun runRestore(
        preset: CloudPreset,
        backup: BackupSummary,
        mode: RestoreMode,
        onProgress: (String) -> Unit
    ) {
        val gh = api(preset)

        for (moduleLabel in backup.manifest.modules) {
            val localDir = AppStorage.backupModuleDir(moduleLabel) ?: continue
            val remoteModuleDir = joinPath(backup.folderPath, moduleLabel)
            val remoteFiles = listRemoteFilesRecursive(gh, preset, remoteModuleDir)
            onProgress("Restoring $moduleLabel (${remoteFiles.size} file${if (remoteFiles.size == 1) "" else "s"})…")

            if (mode == RestoreMode.FULL_REPLACE) {
                listLocalFilesRecursive(localDir).forEach { AppStorage.moveToRecycleBin(moduleLabel, it) }
            }

            for (item in remoteFiles) {
                val relativePath = item.path.removePrefix("$remoteModuleDir/")
                val destFile = File(localDir, relativePath)
                destFile.parentFile?.mkdirs()

                if (mode == RestoreMode.MERGE && destFile.exists()) {
                    AppStorage.moveToRecycleBin(moduleLabel, destFile)
                }

                if (isTextFile(item.name)) {
                    val (_, content) = gh.getFileContent(preset.owner, preset.repo, item.path, preset.branch)
                    destFile.writeText(content, Charsets.UTF_8)
                } else {
                    val (_, bytes) = gh.getFileBytes(preset.owner, preset.repo, item.path, preset.branch)
                    destFile.writeBytes(bytes)
                }
                onProgress("Restored $moduleLabel/$relativePath")
            }
        }
        onProgress("Restore complete.")
        AppStorage.logActivity("Backup Restored", "${backup.manifest.name} (${mode.name})", "Cloud Settings")
    }
}
