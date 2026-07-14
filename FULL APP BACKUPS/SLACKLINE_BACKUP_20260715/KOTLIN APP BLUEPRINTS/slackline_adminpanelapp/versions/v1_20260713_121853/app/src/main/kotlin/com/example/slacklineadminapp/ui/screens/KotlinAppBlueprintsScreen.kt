package com.example.slacklineadminapp.ui.screens

// ─────────────────────────────────────────────────────────────────────────────
//  KotlinAppBlueprintsScreen.kt
//  Module: KOTLIN APP BLUEPRINTS  (Development Vaults)
//  Author: SH Vertex Technologies
//
//  Storage root: Downloads/SLACKLINE ADMIN FILES/Kotlin App Blueprints/
//    <AppSlug>/
//      meta.json          ← KabMeta (name, packageName, kotlinVersion, etc.)
//      versions/
//        v1_20260101_120000/   ← snapshot timestamp-named dirs
//          <original zip contents, actual files on disk>
//        v2_20260627_094721/
//          ...
//      notes.txt          ← freetext notes per app
//
//  Features
//  ────────
//  • Import app from .zip  → analysed for meta (build.gradle / AndroidManifest)
//  • Version history       → re-import = new snapshot, old kept
//  • File tree browser     → dirs + files with icons, breadcrumb nav
//  • File viewer/editor    → full-screen code editor, monospace, editable
//  • Syntax tinting        → .kt / .xml / .gradle / .json / .py colour hints
//  • File search           → search bar inside any app's file tree
//  • Quick stats           → file count, total lines, last updated
//  • Favourites / pin      → starred apps float to top
//  • Notes per app         → freetext notepad per blueprint
//  • Export single .txt    → all files concatenated with headers + indentation
//  • Re-export as .zip     → rebuilds original zip from stored files
//  • Delete version        → remove a snapshot
//  • Delete app            → remove everything
// ─────────────────────────────────────────────────────────────────────────────

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slacklineadminapp.data.AppStorage
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// ══════════════════════════════════════════════════════════════════════════════
//  DATA MODELS
// ══════════════════════════════════════════════════════════════════════════════

data class KabMeta(
    val name:          String = "",
    val packageName:   String = "",
    val kotlinVersion: String = "",
    val agpVersion:    String = "",
    val composeBom:    String = "",
    val minSdk:        String = "",
    val targetSdk:     String = "",
    val importedAt:    String = "",
    val updatedAt:     String = "",
    val isFavourite:   Boolean = false,
    val totalFiles:    Int = 0,
    val totalLines:    Int = 0
)

data class KabVersion(
    val tag:       String = "",   // e.g. "v1_20260627_094721"
    val label:     String = "",   // human label e.g. "v1"
    val timestamp: String = "",
    val fileCount: Int = 0,
    val lineCount: Int = 0
)

// ══════════════════════════════════════════════════════════════════════════════
//  STORAGE ENGINE
// ══════════════════════════════════════════════════════════════════════════════

object KabStore {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** Root: SLACKLINE ADMIN FILES/KOTLIN APP BLUEPRINTS/ */
    fun rootDir(): File = AppStorage.kotlinAppBlueprintsDir()

    fun appDir(slug: String): File = File(rootDir(), slug).also { it.mkdirs() }
    fun metaFile(slug: String): File = File(appDir(slug), "meta.json")
    fun notesFile(slug: String): File = File(appDir(slug), "notes.txt")
    fun versionsDir(slug: String): File = File(appDir(slug), "versions").also { it.mkdirs() }
    fun versionDir(slug: String, tag: String): File = File(versionsDir(slug), tag).also { it.mkdirs() }
    fun versionsIndexFile(slug: String): File = File(appDir(slug), "versions_index.json")

    // ── Meta ─────────────────────────────────────────────────────────────────

    fun loadMeta(slug: String): KabMeta? = try {
        val f = metaFile(slug)
        if (f.exists()) gson.fromJson(f.readText(), KabMeta::class.java) else null
    } catch (_: Exception) { null }

    fun saveMeta(slug: String, meta: KabMeta) {
        metaFile(slug).writeText(gson.toJson(meta), Charsets.UTF_8)
    }

    // ── Versions index ────────────────────────────────────────────────────────

    fun loadVersions(slug: String): MutableList<KabVersion> = try {
        val f = versionsIndexFile(slug)
        if (!f.exists()) mutableListOf()
        else {
            val type = object : TypeToken<MutableList<KabVersion>>() {}.type
            gson.fromJson<MutableList<KabVersion>>(f.readText(), type) ?: mutableListOf()
        }
    } catch (_: Exception) { mutableListOf() }

    fun saveVersions(slug: String, versions: List<KabVersion>) {
        versionsIndexFile(slug).writeText(gson.toJson(versions), Charsets.UTF_8)
    }

    // ── Notes ─────────────────────────────────────────────────────────────────

    fun loadNotes(slug: String): String = try {
        notesFile(slug).takeIf { it.exists() }?.readText(Charsets.UTF_8) ?: ""
    } catch (_: Exception) { "" }

    fun saveNotes(slug: String, notes: String) {
        notesFile(slug).writeText(notes, Charsets.UTF_8)
    }

    // ── App list ──────────────────────────────────────────────────────────────

    fun listApps(): List<Pair<String, KabMeta>> {
        val dir = rootDir()
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isDirectory && File(it, "meta.json").exists() }
            ?.mapNotNull { d ->
                val meta = loadMeta(d.name) ?: return@mapNotNull null
                d.name to meta
            }
            ?.sortedWith(compareByDescending<Pair<String, KabMeta>> { it.second.isFavourite }
                .thenByDescending { it.second.updatedAt })
            ?: emptyList()
    }

    // ── File tree helpers ─────────────────────────────────────────────────────

    /** List entries at [path] inside [versionDir], dirs first */
    fun listPath(vDir: File, path: String): List<Pair<String, String>> {
        val base = if (path.isBlank()) vDir else File(vDir, path)
        if (!base.exists() || !base.isDirectory) return emptyList()
        return base.listFiles()
            ?.map { f -> (if (f.isDirectory) "dir" else "file") to f.name }
            ?.sortedWith(compareBy({ it.first != "dir" }, { it.second }))
            ?: emptyList()
    }

    /** Recursively collect all files under [dir] relative to [root] */
    fun collectAllFiles(dir: File, root: File = dir): List<Pair<String, File>> {
        val result = mutableListOf<Pair<String, File>>()
        dir.walkTopDown().filter { it.isFile }.forEach { f ->
            result.add(f.relativeTo(root).path to f)
        }
        return result.sortedBy { it.first }
    }

    // ── Import from zip ───────────────────────────────────────────────────────

    data class ImportResult(
        val slug:      String,
        val meta:      KabMeta,
        val version:   KabVersion,
        val fileCount: Int,
        val lineCount: Int
    )

    fun importZip(zipBytes: ByteArray, suggestedName: String): ImportResult {
        // Extract zip entries into memory first
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Parse meta from build.gradle / settings.gradle / AndroidManifest
        val meta = parseMeta(entries, suggestedName)
        val slug = slugify(meta.name).ifBlank { "app_${System.currentTimeMillis()}" }

        // Determine version number
        val existingVersions = loadVersions(slug)
        val vNum  = existingVersions.size + 1
        val ts    = AppStorage.timestamp()
        val tag   = "v${vNum}_$ts"
        val vLabel = "v$vNum"

        // Write files to version dir
        val vDir = versionDir(slug, tag)
        var lineCount = 0
        entries.forEach { (path, bytes) ->
            val outFile = File(vDir, path)
            outFile.parentFile?.mkdirs()
            outFile.writeBytes(bytes)
            // Count lines for text files
            if (isTextFile(path)) {
                try { lineCount += bytes.toString(Charsets.UTF_8).lines().size } catch (_: Exception) {}
            }
        }

        val version = KabVersion(
            tag       = tag,
            label     = vLabel,
            timestamp = utcNow(),
            fileCount = entries.size,
            lineCount = lineCount
        )
        existingVersions.add(version)
        saveVersions(slug, existingVersions)

        val fullMeta = meta.copy(
            importedAt  = if (existingVersions.size == 1) utcNow() else (loadMeta(slug)?.importedAt ?: utcNow()),
            updatedAt   = utcNow(),
            isFavourite = loadMeta(slug)?.isFavourite ?: false,
            totalFiles  = entries.size,
            totalLines  = lineCount
        )
        saveMeta(slug, fullMeta)

        return ImportResult(slug, fullMeta, version, entries.size, lineCount)
    }

    private fun parseMeta(entries: Map<String, ByteArray>, fallbackName: String): KabMeta {
        var name          = fallbackName.removeSuffix(".zip").replace("_", " ")
        var packageName   = ""
        var kotlinVersion = ""
        var agpVersion    = ""
        var composeBom    = ""
        var minSdk        = ""
        var targetSdk     = ""

        // settings.gradle / settings.gradle.kts — app name
        entries.entries.firstOrNull { it.key.endsWith("settings.gradle") || it.key.endsWith("settings.gradle.kts") }
            ?.value?.let { bytes ->
                val text = bytes.toString(Charsets.UTF_8)
                Regex("""rootProject\.name\s*=\s*["']([^"']+)["']""").find(text)?.groupValues?.getOrNull(1)
                    ?.let { name = it }
            }

        // build.gradle (root) — kotlin, AGP, compose BOM versions
        entries.entries.firstOrNull {
            (it.key == "build.gradle" || it.key == "build.gradle.kts") &&
                    !it.key.startsWith("app/")
        }?.value?.let { bytes ->
            val text = bytes.toString(Charsets.UTF_8)
            Regex("""kotlin[^\d]*([\d.]+)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)
                ?.let { kotlinVersion = it }
            Regex("""com\.android\.tools\.build:gradle:([\d.]+)""").find(text)?.groupValues?.getOrNull(1)
                ?.let { agpVersion = it }
            Regex("""agp\s*=\s*["']([\d.]+)["']""").find(text)?.groupValues?.getOrNull(1)
                ?.let { if (agpVersion.isBlank()) agpVersion = it }
        }

        // app/build.gradle — package, minSdk, targetSdk, compose BOM
        entries.entries.firstOrNull {
            it.key.endsWith("app/build.gradle") || it.key.endsWith("app/build.gradle.kts")
        }?.value?.let { bytes ->
            val text = bytes.toString(Charsets.UTF_8)
            Regex("""applicationId\s*[=\s]*["']([^"']+)["']""").find(text)?.groupValues?.getOrNull(1)
                ?.let { packageName = it }
            Regex("""minSdk\s*[=\s]*([\d]+)""").find(text)?.groupValues?.getOrNull(1)
                ?.let { minSdk = it }
            Regex("""targetSdk\s*[=\s]*([\d]+)""").find(text)?.groupValues?.getOrNull(1)
                ?.let { targetSdk = it }
            Regex("""compose[._-]bom[^\d]*([\d.]+)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)
                ?.let { composeBom = it }
        }

        // AndroidManifest.xml — package fallback
        if (packageName.isBlank()) {
            entries.entries.firstOrNull { it.key.endsWith("AndroidManifest.xml") }
                ?.value?.let { bytes ->
                    val text = bytes.toString(Charsets.UTF_8)
                    Regex("""package\s*=\s*["']([^"']+)["']""").find(text)?.groupValues?.getOrNull(1)
                        ?.let { packageName = it }
                }
        }

        // libs.versions.toml — kotlin, agp, compose BOM
        entries.entries.firstOrNull { it.key.endsWith("libs.versions.toml") }
            ?.value?.let { bytes ->
                val text = bytes.toString(Charsets.UTF_8)
                Regex("""kotlin\s*=\s*["']([\d.]+)["']""").find(text)?.groupValues?.getOrNull(1)
                    ?.let { if (kotlinVersion.isBlank()) kotlinVersion = it }
                Regex("""agp\s*=\s*["']([\d.]+)["']""").find(text)?.groupValues?.getOrNull(1)
                    ?.let { if (agpVersion.isBlank()) agpVersion = it }
                Regex("""compose-bom\s*=\s*["']([\d.]+)["']""").find(text)?.groupValues?.getOrNull(1)
                    ?.let { if (composeBom.isBlank()) composeBom = it }
            }

        return KabMeta(
            name          = name.trim().ifBlank { fallbackName },
            packageName   = packageName,
            kotlinVersion = kotlinVersion,
            agpVersion    = agpVersion,
            composeBom    = composeBom,
            minSdk        = minSdk,
            targetSdk     = targetSdk
        )
    }

    // ── Export single TXT ─────────────────────────────────────────────────────

    fun exportAsTxt(slug: String, versionTag: String): File {
        val vDir  = versionDir(slug, versionTag)
        val meta  = loadMeta(slug)
        val files = collectAllFiles(vDir)
        val sb    = StringBuilder()

        sb.appendLine("=" .repeat(80))
        sb.appendLine("  KOTLIN APP BLUEPRINT EXPORT")
        sb.appendLine("  App     : ${meta?.name ?: slug}")
        sb.appendLine("  Package : ${meta?.packageName ?: "-"}")
        sb.appendLine("  Kotlin  : ${meta?.kotlinVersion ?: "-"}   AGP: ${meta?.agpVersion ?: "-"}")
        sb.appendLine("  Version : $versionTag")
        sb.appendLine("  Exported: ${utcNow()}")
        sb.appendLine("=" .repeat(80))
        sb.appendLine()

        var exportedCount = 0
        var skippedCount  = 0
        files.forEach { (relPath, file) ->
            if (!isTextFile(relPath)) {
                skippedCount++
                return@forEach  // silently skip binary/IDE files
            }
            exportedCount++
            sb.appendLine()
            sb.appendLine("─".repeat(80))
            sb.appendLine("FILE: $relPath")
            sb.appendLine("─".repeat(80))
            try {
                val fileContent = file.readText(Charsets.UTF_8)
                fileContent.lines().forEach { line ->
                    sb.appendLine("    $line")
                }
            } catch (_: Exception) {
                sb.appendLine("    [Could not read file — skipped]")
            }
        }

        // Append export summary at bottom
        sb.appendLine()
        sb.appendLine("=".repeat(80))
        sb.appendLine("  EXPORT SUMMARY")
        sb.appendLine("  Files exported : $exportedCount")
        sb.appendLine("  Files skipped  : $skippedCount (binary / IDE metadata)")
        sb.appendLine("=".repeat(80))

        val outDir = AppStorage.kotlinAppBlueprintsExportDir()
        val outFile = File(outDir, "${slug}_${versionTag}_export.txt")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }

    // ── Export ZIP ────────────────────────────────────────────────────────────

    fun exportAsZip(slug: String, versionTag: String): File {
        val vDir  = versionDir(slug, versionTag)
        val files = collectAllFiles(vDir)
        val outDir = AppStorage.kotlinAppBlueprintsExportDir()
        val outFile = File(outDir, "${slug}_${versionTag}.zip")
        ZipOutputStream(FileOutputStream(outFile)).use { zos ->
            files.forEach { (relPath, file) ->
                zos.putNextEntry(ZipEntry(relPath))
                zos.write(file.readBytes())
                zos.closeEntry()
            }
        }
        return outFile
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteVersion(slug: String, tag: String) {
        versionDir(slug, tag).deleteRecursively()
        val versions = loadVersions(slug).filter { it.tag != tag }.toMutableList()
        saveVersions(slug, versions)
        // update meta stats from latest version
        val latest = versions.lastOrNull()
        loadMeta(slug)?.let { m ->
            saveMeta(slug, m.copy(
                totalFiles = latest?.fileCount ?: 0,
                totalLines = latest?.lineCount ?: 0,
                updatedAt  = latest?.timestamp ?: m.updatedAt
            ))
        }
    }

    fun deleteApp(slug: String) {
        appDir(slug).deleteRecursively()
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    fun slugify(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    fun utcNow(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .also { it.timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

    fun humanBytes(bytes: Long): String = when {
        bytes < 1024L        -> "${bytes} B"
        bytes < 1_048_576L   -> "${"%.1f".format(bytes / 1024.0)} KB"
        else                 -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
    }

    fun isTextFile(path: String): Boolean {
        // Skip hidden IDE/tool/build directories entirely — never treat as text
        val normalised = path.replace('\\', '/')
        val skipDirs = setOf(
            ".acside", ".gradle", ".idea", ".git", ".kotlin",
            ".cxx", ".externalNativeBuild", "build", "__pycache__",
            ".androidide", ".cache"
        )
        if (normalised.split('/').any { it in skipDirs }) return false

        // Whitelist of known plain-text extensions only
        val ext = normalised.substringAfterLast('.', "").lowercase()
        return ext in setOf(
            "kt", "kts", "java", "xml", "gradle", "json", "toml",
            "txt", "md", "yaml", "yml", "properties", "py", "sh",
            "bat", "html", "css", "js", "ts", "sql", "pro",
            "gitignore", "editorconfig", "cfg", "ini", "spec",
            "iml", "pbxproj", "plist"
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  NAVIGATION STATE
// ══════════════════════════════════════════════════════════════════════════════

sealed class KabView {
    object Hub                                                            : KabView()
    data class AppDetail(val slug: String)                                : KabView()
    data class VersionBrowser(val slug: String, val versionTag: String,
                               val path: String = "")                    : KabView()
    data class FileEditor(val slug: String, val versionTag: String,
                          val filePath: String)                           : KabView()
    data class Notes(val slug: String)                                    : KabView()
}

// ══════════════════════════════════════════════════════════════════════════════
//  ENTRY COMPOSABLE
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun KotlinAppBlueprintsScreen(onNavigateBack: () -> Unit) {
    var view by remember { mutableStateOf<KabView>(KabView.Hub) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun snack(msg: String) { scope.launch { snackbarHostState.showSnackbar(msg) } }

    BackHandler(enabled = view !is KabView.Hub) {
        view = when (val v = view) {
            is KabView.FileEditor     -> KabView.VersionBrowser(v.slug, v.versionTag,
                v.filePath.substringBeforeLast('/', "").let { p ->
                    val vDir = KabStore.versionDir(v.slug, v.versionTag)
                    val rel  = File(v.filePath).parent ?: ""
                    rel
                })
            is KabView.VersionBrowser -> if (v.path.isBlank()) KabView.AppDetail(v.slug)
                                         else KabView.VersionBrowser(v.slug, v.versionTag,
                                             v.path.substringBeforeLast('/', ""))
            is KabView.Notes          -> KabView.AppDetail(v.slug)
            is KabView.AppDetail      -> KabView.Hub
            else                      -> KabView.Hub
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalAppColors.current.bg)
                .padding(padding)
        ) {
            when (val v = view) {
                is KabView.Hub ->
                    KabHubView(
                        onNavigateBack = onNavigateBack,
                        onOpenApp      = { slug -> view = KabView.AppDetail(slug) },
                        snack          = ::snack
                    )
                is KabView.AppDetail ->
                    KabAppDetailView(
                        slug           = v.slug,
                        onBack         = { view = KabView.Hub },
                        onBrowse       = { tag -> view = KabView.VersionBrowser(v.slug, tag) },
                        onNotes        = { view = KabView.Notes(v.slug) },
                        snack          = ::snack
                    )
                is KabView.VersionBrowser ->
                    KabBrowserView(
                        slug           = v.slug,
                        versionTag     = v.versionTag,
                        currentPath    = v.path,
                        onBack         = {
                            view = if (v.path.isBlank()) KabView.AppDetail(v.slug)
                                   else KabView.VersionBrowser(v.slug, v.versionTag,
                                       v.path.substringBeforeLast('/', ""))
                        },
                        onNavigate     = { p -> view = KabView.VersionBrowser(v.slug, v.versionTag, p) },
                        onEditFile     = { fp -> view = KabView.FileEditor(v.slug, v.versionTag, fp) },
                        snack          = ::snack
                    )
                is KabView.FileEditor ->
                    KabEditorView(
                        slug           = v.slug,
                        versionTag     = v.versionTag,
                        filePath       = v.filePath,
                        onBack         = {
                            val parentPath = v.filePath.substringBeforeLast('/', "")
                            view = KabView.VersionBrowser(v.slug, v.versionTag, parentPath)
                        },
                        snack          = ::snack
                    )
                is KabView.Notes ->
                    KabNotesView(
                        slug   = v.slug,
                        onBack = { view = KabView.AppDetail(v.slug) },
                        snack  = ::snack
                    )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  HUB  – app list
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KabHubView(
    onNavigateBack: () -> Unit,
    onOpenApp:      (String) -> Unit,
    snack:          (String) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var apps    by remember { mutableStateOf<List<Pair<String, KabMeta>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    var search  by remember { mutableStateOf("") }

    LaunchedEffect(refresh) {
        loading = true
        withContext(Dispatchers.IO) { apps = KabStore.listApps() }
        loading = false
    }

    // ZIP file picker
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: run { withContext(Dispatchers.Main) { snack("Could not read file.") }; return@launch }
                val name  = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    c.moveToFirst()
                    c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                } ?: "app.zip"
                val result = KabStore.importZip(bytes, name)
                withContext(Dispatchers.Main) {
                    snack("✓ Imported \"${result.meta.name}\" — ${result.fileCount} files, ${result.lineCount} lines.")
                    refresh++
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { snack("Import failed: ${e.message}") }
            }
        }
    }

    val displayed = if (search.isBlank()) apps
                    else apps.filter {
                        it.second.name.contains(search, ignoreCase = true) ||
                        it.second.packageName.contains(search, ignoreCase = true)
                    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ─────────────────────────────────────────────────────────
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, null, tint = GreenCol, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kotlin App Blueprints", color = GreenCol,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Development Vaults  •  SH Vertex Technologies",
                            color = SubText, fontSize = 11.sp)
                    }
                }
                // Stats row
                if (apps.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KabStatChip("${apps.size}", "Apps",  GreenCol, modifier = Modifier.weight(1f))
                        KabStatChip("${apps.sumOf { it.second.totalFiles }}", "Files", BlueCol,  modifier = Modifier.weight(1f))
                        KabStatChip("${apps.count { it.second.isFavourite }}", "Faves", AmberCol, modifier = Modifier.weight(1f))
                    }
                }
                // Search
                OutlinedTextField(
                    value         = search,
                    onValueChange = { search = it },
                    label         = { Text("Search apps…", fontSize = 12.sp) },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = SubText, modifier = Modifier.size(18.dp)) },
                    trailingIcon  = if (search.isNotBlank()) {{ IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Close, null, tint = SubText, modifier = Modifier.size(16.dp))
                    }}} else null,
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(8.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GreenCol,
                        unfocusedBorderColor = SubText.copy(alpha = 0.4f),
                        focusedLabelColor    = GreenCol,
                        unfocusedLabelColor  = SubText,
                        focusedTextColor     = TextCol,
                        unfocusedTextColor   = TextCol,
                        cursorColor          = GreenCol
                    )
                )
            }
        }

        // ── List ───────────────────────────────────────────────────────────
        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenCol)
            }
        } else {
            LazyColumn(
                modifier        = Modifier.weight(1f),
                contentPadding  = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ActionButton(
                        text    = "+ Add Kotlin App (Import ZIP)",
                        color   = GreenCol,
                        onClick = { zipPicker.launch("application/zip") }
                    )
                }

                if (displayed.isEmpty()) {
                    item {
                        AppCard {
                            BodyText(
                                if (search.isNotBlank()) "No apps match \"$search\"."
                                else "No Kotlin app blueprints yet.\nTap '+ Add Kotlin App' to import a ZIP.",
                                SubText
                            )
                        }
                    }
                }

                items(displayed) { (slug, meta) ->
                    KabAppCard(slug = slug, meta = meta,
                        onOpen      = { onOpenApp(slug) },
                        onToggleFav = {
                            scope.launch(Dispatchers.IO) {
                                KabStore.saveMeta(slug, meta.copy(isFavourite = !meta.isFavourite))
                                withContext(Dispatchers.Main) { refresh++ }
                            }
                        }
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        BottomNavBar(listOf(
            "← BACK"   to onNavigateBack,
            "REFRESH"  to { refresh++ }
        ))
    }
}

// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun KabAppCard(
    slug:        String,
    meta:        KabMeta,
    onOpen:      () -> Unit,
    onToggleFav: () -> Unit
) {
    AppCard(color = LocalAppColors.current.card2) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(meta.name, color = TextCol, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (meta.isFavourite) {
                        Icon(Icons.Default.Star, null, tint = AmberCol, modifier = Modifier.size(16.dp))
                    }
                }
                if (meta.packageName.isNotBlank())
                    Text(meta.packageName, color = SubText, fontSize = 11.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
            }
        }

        // Tech badges
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())) {
            if (meta.kotlinVersion.isNotBlank())
                KabBadge("Kotlin ${meta.kotlinVersion}", Color(0xFF7F52FF))
            if (meta.agpVersion.isNotBlank())
                KabBadge("AGP ${meta.agpVersion}", TealCol)
            if (meta.composeBom.isNotBlank())
                KabBadge("BOM ${meta.composeBom}", BlueCol)
            if (meta.minSdk.isNotBlank())
                KabBadge("API ${meta.minSdk}+", OrangeCol)
        }

        // Stats
        Text("${meta.totalFiles} files  •  ${meta.totalLines} lines  •  ${meta.updatedAt.take(10)}",
            color = SubText, fontSize = 11.sp)

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick  = onOpen,
                modifier = Modifier.weight(1f).height(36.dp),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenCol)
            ) {
                Text("Open", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick  = onToggleFav,
                modifier = Modifier.height(36.dp),
                shape    = RoundedCornerShape(8.dp),
                border   = BorderStroke(1.dp, if (meta.isFavourite) AmberCol else SubText.copy(0.4f))
            ) {
                Icon(
                    if (meta.isFavourite) Icons.Default.Star else Icons.Default.StarBorder,
                    null, tint = if (meta.isFavourite) AmberCol else SubText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  APP DETAIL  – version list + meta + actions
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KabAppDetailView(
    slug:    String,
    onBack:  () -> Unit,
    onBrowse:(String) -> Unit,
    onNotes: () -> Unit,
    snack:   (String) -> Unit
) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    var meta     by remember { mutableStateOf(KabStore.loadMeta(slug)) }
    var versions by remember { mutableStateOf(KabStore.loadVersions(slug)) }
    var refresh  by remember { mutableIntStateOf(0) }
    var showDelApp by remember { mutableStateOf(false) }
    var delVersionTag by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refresh) {
        withContext(Dispatchers.IO) {
            meta     = KabStore.loadMeta(slug)
            versions = KabStore.loadVersions(slug)
        }
    }

    // ZIP picker for adding a new version
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: run { withContext(Dispatchers.Main) { snack("Could not read file.") }; return@launch }
                val existingMeta = KabStore.loadMeta(slug)
                val result = KabStore.importZip(bytes, existingMeta?.name ?: slug)
                withContext(Dispatchers.Main) {
                    snack("✓ New version imported — ${result.fileCount} files.")
                    refresh++
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { snack("Import failed: ${e.message}") }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ─────────────────────────────────────────────────────────
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, null, tint = GreenCol, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(meta?.name ?: slug, color = GreenCol,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(meta?.packageName ?: "", color = SubText, fontSize = 11.sp)
                    }
                    if (meta?.isFavourite == true)
                        Icon(Icons.Default.Star, null, tint = AmberCol, modifier = Modifier.size(18.dp))
                }

                // Tech meta grid
                meta?.let { m ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (m.kotlinVersion.isNotBlank())
                            KabMetaRow(Icons.Default.Code,        "Kotlin",  m.kotlinVersion, Color(0xFF7F52FF))
                        if (m.agpVersion.isNotBlank())
                            KabMetaRow(Icons.Default.Build,       "AGP",     m.agpVersion,    TealCol)
                        if (m.composeBom.isNotBlank())
                            KabMetaRow(Icons.Default.Layers,      "BOM",     m.composeBom,    BlueCol)
                        if (m.minSdk.isNotBlank() || m.targetSdk.isNotBlank())
                            KabMetaRow(Icons.Default.PhoneAndroid,"SDK",
                                listOfNotNull(
                                    m.minSdk.takeIf { it.isNotBlank() }?.let { "min $it" },
                                    m.targetSdk.takeIf { it.isNotBlank() }?.let { "target $it" }
                                ).joinToString("  •  "), OrangeCol)
                        KabMetaRow(Icons.Default.FolderOpen,  "Files",   "${m.totalFiles}", SubText)
                        KabMetaRow(Icons.Default.TextSnippet, "Lines",   "${m.totalLines}", SubText)
                    }
                }

                // Quick action row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    KabActionChip("+ New Version", GreenCol)  { zipPicker.launch("application/zip") }
                    KabActionChip("Notes",         BlueCol)   { onNotes() }
                    KabActionChip("Delete App",    RedCol)    { showDelApp = true }
                }
            }
        }

        // ── Version list ───────────────────────────────────────────────────
        LazyColumn(
            modifier        = Modifier.weight(1f),
            contentPadding  = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Version History", color = SubText, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
            }

            if (versions.isEmpty()) {
                item { AppCard { BodyText("No versions yet.", SubText) } }
            }

            items(versions.sortedByDescending { it.timestamp }) { ver ->
                AppCard(color = LocalAppColors.current.card) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GreenCol.copy(0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ver.label, color = GreenCol,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ver.tag, color = TextCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${ver.fileCount} files  •  ${ver.lineCount} lines",
                                color = SubText, fontSize = 11.sp)
                            Text(ver.timestamp.take(19).replace("T", "  "), color = SubText, fontSize = 10.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        KabActionChip("Browse",     GreenCol)  { onBrowse(ver.tag) }
                        KabActionChip("Export TXT", AmberCol)  {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val f = KabStore.exportAsTxt(slug, ver.tag)
                                    withContext(Dispatchers.Main) { snack("✓ Exported to ${f.name}") }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { snack("Export failed: ${e.message}") }
                                }
                            }
                        }
                        KabActionChip("Export ZIP", TealCol)   {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val f = KabStore.exportAsZip(slug, ver.tag)
                                    withContext(Dispatchers.Main) { snack("✓ Exported to ${f.name}") }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { snack("Export failed: ${e.message}") }
                                }
                            }
                        }
                        KabActionChip("Delete",     RedCol)    { delVersionTag = ver.tag }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        BottomNavBar(listOf(
            "← BACK"  to onBack,
            "REFRESH" to { refresh++ }
        ))
    }

    // Delete version dialog
    delVersionTag?.let { tag ->
        ConfirmDialog(
            title       = "Delete Version",
            message     = "Delete snapshot \"$tag\"? This cannot be undone.",
            confirmText = "Delete",
            confirmColor = RedCol,
            onConfirm   = {
                scope.launch(Dispatchers.IO) {
                    KabStore.deleteVersion(slug, tag)
                    withContext(Dispatchers.Main) { snack("Version deleted."); refresh++ }
                }
            },
            onDismiss   = { delVersionTag = null }
        )
    }

    // Delete app dialog
    if (showDelApp) {
        ConfirmDialog(
            title       = "Delete App",
            message     = "Delete \"${meta?.name ?: slug}\" and ALL its versions? This cannot be undone.",
            confirmText = "Delete",
            confirmColor = RedCol,
            onConfirm   = {
                scope.launch(Dispatchers.IO) {
                    KabStore.deleteApp(slug)
                    withContext(Dispatchers.Main) { snack("App deleted."); onBack() }
                }
            },
            onDismiss   = { showDelApp = false }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  VERSION BROWSER  – file tree
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KabBrowserView(
    slug:        String,
    versionTag:  String,
    currentPath: String,
    onBack:      () -> Unit,
    onNavigate:  (String) -> Unit,
    onEditFile:  (String) -> Unit,
    snack:       (String) -> Unit
) {
    val scope   = rememberCoroutineScope()
    val vDir    = remember(slug, versionTag) { KabStore.versionDir(slug, versionTag) }
    var items   by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var search  by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var delPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refresh, currentPath) {
        loading = true
        withContext(Dispatchers.IO) { items = KabStore.listPath(vDir, currentPath) }
        loading = false
    }

    val displayed = if (search.isBlank()) items
                    else items.filter { it.second.contains(search, ignoreCase = true) }

    // Breadcrumb parts
    val breadcrumbs = if (currentPath.isBlank()) listOf("root")
                      else listOf("root") + currentPath.split("/")

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ─────────────────────────────────────────────────────────
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, null, tint = GreenCol, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("${KabStore.loadMeta(slug)?.name ?: slug}  /  $versionTag",
                            color = GreenCol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${items.size} items  •  ${items.count { it.first == "dir" }} dirs  •  ${items.count { it.first == "file" }} files",
                            color = SubText, fontSize = 10.sp)
                    }
                }

                // Breadcrumb nav
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    breadcrumbs.forEachIndexed { idx, crumb ->
                        val isLast = idx == breadcrumbs.lastIndex
                        Text(
                            text  = if (idx == 0) "⌂ root" else crumb,
                            color = if (isLast) GreenCol else SubText,
                            fontSize = 11.sp,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            modifier = if (!isLast) Modifier.clickable {
                                val targetPath = if (idx == 0) ""
                                                 else breadcrumbs.subList(1, idx + 1).joinToString("/")
                                onNavigate(targetPath)
                            } else Modifier
                        )
                        if (!isLast) Text(" / ", color = SubText.copy(0.5f), fontSize = 11.sp)
                    }
                }

                // Search bar
                OutlinedTextField(
                    value         = search,
                    onValueChange = { search = it },
                    label         = { Text("Search in this folder…", fontSize = 11.sp) },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = SubText, modifier = Modifier.size(16.dp)) },
                    trailingIcon  = if (search.isNotBlank()) {{ IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Close, null, tint = SubText, modifier = Modifier.size(14.dp))
                    }}} else null,
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(8.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = GreenCol,
                        unfocusedBorderColor = SubText.copy(0.3f),
                        focusedLabelColor    = GreenCol,
                        unfocusedLabelColor  = SubText,
                        focusedTextColor     = TextCol,
                        unfocusedTextColor   = TextCol,
                        cursorColor          = GreenCol
                    )
                )
            }
        }

        // ── Tree ───────────────────────────────────────────────────────────
        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenCol)
            }
        } else {
            LazyColumn(
                modifier        = Modifier.weight(1f),
                contentPadding  = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (displayed.isEmpty()) {
                    item { AppCard { BodyText("No items here.", SubText) } }
                }

                items(displayed) { (type, name) ->
                    val fullPath = if (currentPath.isBlank()) name else "$currentPath/$name"
                    if (type == "dir") {
                        // Directory row
                        Surface(
                            onClick = { onNavigate(fullPath) },
                            color   = LocalAppColors.current.card2,
                            shape   = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, null, tint = AmberCol, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(name, color = TextCol, fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = SubText, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else {
                        // File row
                        val actualFile = File(vDir, fullPath)
                        val fileSize   = KabStore.humanBytes(actualFile.length())
                        val ext        = name.substringAfterLast('.', "").lowercase()

                        Surface(
                            color  = LocalAppColors.current.card,
                            shape  = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = fileTypeIcon(ext),
                                        contentDescription = null,
                                        tint        = fileTypeColor(ext),
                                        modifier    = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, color = TextCol, fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp)
                                        Text("$fileSize  •  .$ext", color = SubText, fontSize = 10.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    KabActionChip("View / Edit", GreenCol) { onEditFile(fullPath) }
                                    KabActionChip("Delete",      RedCol)   { delPath = fullPath }
                                }
                            }
                        }
                    }
                }
            }
        }

        BottomNavBar(listOf(
            "← BACK"  to onBack,
            "REFRESH" to { refresh++ }
        ))
    }

    // Delete file confirm
    delPath?.let { fp ->
        ConfirmDialog(
            title       = "Delete File",
            message     = "Delete \"${fp.substringAfterLast('/')}\"?",
            confirmText = "Delete",
            confirmColor = RedCol,
            onConfirm   = {
                scope.launch(Dispatchers.IO) {
                    File(vDir, fp).delete()
                    withContext(Dispatchers.Main) { snack("Deleted."); refresh++ }
                }
                delPath = null
            },
            onDismiss = { delPath = null }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  FILE EDITOR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KabEditorView(
    slug:       String,
    versionTag: String,
    filePath:   String,
    onBack:     () -> Unit,
    snack:      (String) -> Unit
) {
    val scope    = rememberCoroutineScope()
    val vDir     = remember(slug, versionTag) { KabStore.versionDir(slug, versionTag) }
    val file     = remember(filePath) { File(vDir, filePath) }
    val ext      = filePath.substringAfterLast('.', "").lowercase()
    val isBinary = !KabStore.isTextFile(filePath)

    var content  by remember { mutableStateOf("") }
    var loading  by remember { mutableStateOf(true) }
    var dirty    by remember { mutableStateOf(false) }

    LaunchedEffect(filePath) {
        loading = true
        withContext(Dispatchers.IO) {
            content = if (isBinary) "[Binary file — not editable as text]"
                      else try { file.readText(Charsets.UTF_8) } catch (_: Exception) { "[Read error]" }
        }
        loading = false
    }

    fun save() {
        if (isBinary) { snack("Cannot save binary file."); return }
        scope.launch(Dispatchers.IO) {
            try {
                file.writeText(content, Charsets.UTF_8)
                withContext(Dispatchers.Main) { snack("✓ Saved."); dirty = false }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { snack("Save failed: ${e.message}") }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ─────────────────────────────────────────────────────────
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(fileTypeIcon(ext), null, tint = fileTypeColor(ext), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(filePath.substringAfterLast('/'), color = fileTypeColor(ext),
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(filePath, color = SubText, fontSize = 10.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (dirty) {
                        Surface(color = AmberCol.copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("UNSAVED", color = AmberCol, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                if (!isBinary) {
                    Text("${content.lines().size} lines  •  ${content.length} chars  •  .$ext",
                        color = SubText, fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { save() }, modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenCol)) {
                            Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { content = ""; dirty = true },
                            modifier = Modifier.height(36.dp), shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedCol)) {
                            Text("Clear", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ── Code area ──────────────────────────────────────────────────────
        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenCol)
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF050808))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                if (isBinary) {
                    Text(content, color = SubText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                } else {
                    BasicTextField(
                        value         = content,
                        onValueChange = { content = it; dirty = true },
                        textStyle     = TextStyle(
                            color      = Color(0xFFCDD3DE),
                            fontSize   = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight  = 20.sp
                        ),
                        cursorBrush   = SolidColor(GreenCol),
                        modifier      = Modifier.widthIn(min = 600.dp)
                    )
                }
            }
        }

        BottomNavBar(listOf(
            "← BACK"         to onBack,
            if (!isBinary) "SAVE" to { save() } else "READ-ONLY" to {}
        ))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  NOTES VIEW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KabNotesView(
    slug:   String,
    onBack: () -> Unit,
    snack:  (String) -> Unit
) {
    val scope  = rememberCoroutineScope()
    var notes  by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var dirty   by remember { mutableStateOf(false) }

    LaunchedEffect(slug) {
        withContext(Dispatchers.IO) { notes = KabStore.loadNotes(slug) }
        loading = false
    }

    fun save() {
        scope.launch(Dispatchers.IO) {
            KabStore.saveNotes(slug, notes)
            withContext(Dispatchers.Main) { snack("✓ Notes saved."); dirty = false }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.StickyNote2, null, tint = BlueCol, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Notes", color = BlueCol, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        modifier = Modifier.weight(1f))
                    if (dirty) {
                        Surface(color = AmberCol.copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text("UNSAVED", color = AmberCol, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("${KabStore.loadMeta(slug)?.name ?: slug}", color = SubText, fontSize = 11.sp)
                Button(onClick = { save() }, modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueCol)) {
                    Text("Save Notes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BlueCol)
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070A0D))
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                BasicTextField(
                    value         = notes,
                    onValueChange = { notes = it; dirty = true },
                    textStyle     = TextStyle(
                        color      = Color(0xFFCDD3DE),
                        fontSize   = 14.sp,
                        lineHeight  = 22.sp
                    ),
                    cursorBrush   = SolidColor(BlueCol),
                    modifier      = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (notes.isEmpty()) {
                            Text("Write notes about this app here…\n\nExample:\n• What APIs are used\n• Known issues\n• Next planned features\n• Special build instructions",
                                color = SubText.copy(0.5f), fontSize = 14.sp, lineHeight = 22.sp)
                        }
                        inner()
                    }
                )
            }
        }

        BottomNavBar(listOf(
            "← BACK" to onBack,
            "SAVE"   to { save() }
        ))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ATOM COMPOSABLES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun KabBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text     = text,
            color    = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun KabActionChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color   = color.copy(alpha = 0.15f),
        shape   = RoundedCornerShape(6.dp)
    ) {
        Text(
            text     = text,
            color    = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun KabStatChip(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = appColors.card)
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(label, color = SubText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun KabMetaRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text("$label:", color = SubText, fontSize = 11.sp, modifier = Modifier.width(60.dp))
        Text(value,  color = TextCol, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SYNTAX COLOUR HELPERS
// ══════════════════════════════════════════════════════════════════════════════

private fun fileTypeColor(ext: String): Color = when (ext) {
    "kt", "kts"                -> Color(0xFF7F52FF)  // Kotlin purple
    "java"                     -> Color(0xFFE76F00)  // Java orange
    "xml"                      -> Color(0xFF4FC3F7)  // XML cyan
    "gradle"                   -> Color(0xFF02BF7A)  // Gradle green
    "toml"                     -> Color(0xFF9CCC65)  // TOML lime
    "json"                     -> Color(0xFFFFD54F)  // JSON yellow
    "md"                       -> Color(0xFF90A4AE)  // Markdown grey
    "yaml", "yml"              -> Color(0xFFEF5350)  // YAML red
    "properties"               -> Color(0xFF78909C)  // props slate
    "py"                       -> Color(0xFF3776AB)  // Python blue
    "sh", "bat"                -> Color(0xFF66BB6A)  // Script green
    "txt", "gitignore", "pro"  -> SubText
    else                       -> TextCol
}

private fun fileTypeIcon(ext: String): ImageVector = when (ext) {
    "kt", "kts", "java"   -> Icons.Default.Code
    "xml"                  -> Icons.Default.DataObject
    "gradle", "toml"       -> Icons.Default.Build
    "json"                 -> Icons.Default.DataObject
    "md"                   -> Icons.Default.Description
    "yaml", "yml"          -> Icons.Default.Settings
    "png", "jpg", "webp"   -> Icons.Default.Image
    "so"                   -> Icons.Default.Memory
    else                   -> Icons.Default.InsertDriveFile
}
