package com.example.slacklineadminapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

enum class Step3Mode { PATCH, TEMPLATE }

@Composable
fun KotlinLicenseToolScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    fun snack(msg: String) { scope.launch { snackbarHostState.showSnackbar(msg) } }

    var selectedProduct by remember { mutableStateOf<NewProduct?>(null) }
    var pemText         by remember { mutableStateOf("") }
    var packageName     by remember { mutableStateOf("com.example.myapp") }
    var appCode         by remember { mutableStateOf("my_app") }
    var actPrefix       by remember { mutableStateOf("MYAPP6A") }
    var licenseFile     by remember { mutableStateOf("shv_license_myapp.json") }
    var displayName     by remember { mutableStateOf("My App Pro") }
    var themeName       by remember { mutableStateOf("MyAppTheme") }
    var step3Mode       by remember { mutableStateOf(Step3Mode.PATCH) }
    var manifestPath    by remember { mutableStateOf("") }
    var manifestName    by remember { mutableStateOf("No file selected.") }
    var mainActPath     by remember { mutableStateOf("") }
    var mainActName     by remember { mutableStateOf("No file selected.") }
    var resultText      by remember { mutableStateOf("Select a product and fill in the package name, then generate.") }
    var resultIsError   by remember { mutableStateOf(false) }
    var isGenerating    by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }

    val manifestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            c.moveToFirst(); c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
        } ?: "AndroidManifest.xml"
        val tmp = File(context.cacheDir, name)
        context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
        manifestPath = tmp.absolutePath; manifestName = name
        snack("$name selected.")
    }

    val mainActLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            c.moveToFirst(); c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
        } ?: "MainActivity.kt"
        val tmp = File(context.cacheDir, name)
        context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
        mainActPath = tmp.absolutePath; mainActName = name
        snack("$name selected.")
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(LocalAppColors.current.bg).padding(padding)) {
            Surface(color = LocalAppColors.current.card2, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, null, tint = CyanCol, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Kotlin License Tool", color = CyanCol, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, modifier = Modifier.weight(1f))
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // STEP 1
                item {
                    AppCard {
                        Text("Step 1 — Select Product", color = TealCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        BodyText("Choose a product from New License Manager. Fields auto-fill.", SubText)
                        Text(
                            selectedProduct?.let {
                                "${it.displayName}  |  prefix: ${it.activationPrefix}  |  app: ${it.bundleApp}" +
                                if (pemText.isBlank()) "  ⚠ No key" else ""
                            } ?: "No product selected.",
                            color = if (selectedProduct != null && pemText.isNotBlank()) GreenCol
                                    else if (selectedProduct != null) RedCol else SubText,
                            fontSize = 12.sp
                        )
                        Button(onClick = { showProductPicker = true },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealCol)) {
                            Text("Select Product", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // STEP 2
                item {
                    AppCard {
                        Text("Step 2 — App Details", color = BlueCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        BodyText("Auto-filled when product is selected. Edit if needed.", SubText)
                        AppTextField(packageName,  { packageName = it },  "Kotlin Package Name (e.g. com.example.myapp)")
                        AppTextField(appCode,       { appCode = it },      "App Code / bundle_app")
                        AppTextField(actPrefix,     { actPrefix = it },    "Activation Prefix (e.g. MYAPP6A)")
                        AppTextField(licenseFile,   { licenseFile = it },  "License File Name (e.g. shv_license_myapp.json)")
                        AppTextField(displayName,   { displayName = it },  "Display / Product Name")
                    }
                }

                // STEP 3
                item {
                    AppCard {
                        Text("Step 3 — MainActivity & Manifest", color = OrangeCol,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { step3Mode = Step3Mode.PATCH },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (step3Mode == Step3Mode.PATCH) OrangeCol
                                                     else LocalAppColors.current.card2)) {
                                Text("Patch Existing", color = Color.White, fontSize = 13.sp)
                            }
                            Button(onClick = { step3Mode = Step3Mode.TEMPLATE },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (step3Mode == Step3Mode.TEMPLATE) PurpleCol
                                                     else LocalAppColors.current.card2)) {
                                Text("Generate Template", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        if (step3Mode == Step3Mode.PATCH) {
                            BodyText("Picks your existing files. Adds INTERNET permission and injects license gate.", SubText)
                            Text("AndroidManifest.xml", color = SubText, fontSize = 12.sp)
                            Text(manifestName, color = if (manifestPath.isNotBlank()) GreenCol else SubText, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { manifestLauncher.launch("*/*") },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangeCol)) {
                                    Text("Pick Manifest", color = Color.White, fontSize = 12.sp)
                                }
                                Button(onClick = { manifestPath = ""; manifestName = "No file selected." },
                                    modifier = Modifier.height(44.dp), shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RedCol)) {
                                    Text("Clear", color = Color.White, fontSize = 12.sp)
                                }
                            }
                            Text("MainActivity.kt", color = SubText, fontSize = 12.sp)
                            Text(mainActName, color = if (mainActPath.isNotBlank()) GreenCol else SubText, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { mainActLauncher.launch("*/*") },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberCol)) {
                                    Text("Pick MainActivity", color = Color.Black, fontSize = 12.sp)
                                }
                                Button(onClick = { mainActPath = ""; mainActName = "No file selected." },
                                    modifier = Modifier.height(44.dp), shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RedCol)) {
                                    Text("Clear", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        } else {
                            BodyText("Generates ready-to-use MainActivity.kt and AndroidManifest.xml with license gate wired in.", SubText)
                            AppTextField(themeName, { themeName = it }, "App Theme Name (e.g. MyAppTheme)")
                        }
                    }
                }

                // STEP 4
                item {
                    AppCard {
                        Text("Step 4 — Generate", color = GreenCol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        BodyText("Files saved to:\nDownloads/SLACKLINE ADMIN FILES/SHV_Admin_Data/Kotlin_License_Output/<app_code>/", SubText)
                        Text(resultText,
                            color = if (resultIsError) RedCol
                                    else if (resultText.startsWith("Generated")) GreenCol
                                    else SubText,
                            fontSize = 12.sp)
                        Button(
                            onClick = {
                                val errors = mutableListOf<String>()
                                if (selectedProduct == null) errors.add("Select a product first.")
                                if (packageName.isBlank()) errors.add("Package name is required.")
                                if (appCode.isBlank())     errors.add("App code is required.")
                                if (actPrefix.isBlank())   errors.add("Activation prefix is required.")
                                if (licenseFile.isBlank()) errors.add("License file name is required.")
                                if (displayName.isBlank()) errors.add("Display name is required.")
                                if (pemText.isBlank())     errors.add("No public key — select a product with a loaded key.")
                                if (errors.isNotEmpty()) { snack(errors.first()); return@Button }
                                isGenerating = true
                                resultText = "Generating…"; resultIsError = false
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val summary = KotlinLicenseGenerator.generate(
                                            packageName  = packageName.trim(),
                                            appCode      = appCode.trim(),
                                            actPrefix    = actPrefix.trim(),
                                            licenseFile  = licenseFile.trim(),
                                            displayName  = displayName.trim(),
                                            pem          = pemText.trim(),
                                            themeName    = themeName.trim().ifBlank { "MyAppTheme" },
                                            mode         = step3Mode,
                                            manifestPath = manifestPath,
                                            mainActPath  = mainActPath
                                        )
                                        AppStorage.logActivity("Kotlin License Tool", summary, displayName.trim())
                                        withContext(Dispatchers.Main) {
                                            resultText = summary; resultIsError = false; isGenerating = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            resultText = "Error: ${e.message?.take(120)}"
                                            resultIsError = true; isGenerating = false
                                        }
                                    }
                                }
                            },
                            enabled = !isGenerating,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenCol)
                        ) {
                            if (isGenerating)
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else
                                Text("Generate Kotlin License Files", color = Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            BottomNavBar(listOf("← BACK" to onNavigateBack))
        }
    }

    if (showProductPicker) {
        val products = remember { NewLicenseStore.allProducts() }
        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            containerColor = CardBg, titleContentColor = TextCol,
            title = { Text("Select Product", fontWeight = FontWeight.Bold) },
            text = {
                if (products.isEmpty()) {
                    Text("No products in New License Manager. Add one first.", color = SubText, fontSize = 13.sp)
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        products.forEach { prod ->
                            val keyFile = NewLicenseStore.pubKeyPath(prod.id)
                            val hasKey  = keyFile.exists()
                            Button(
                                onClick = {
                                    selectedProduct = prod
                                    appCode     = prod.bundleApp.ifBlank { prod.id }
                                    actPrefix   = prod.activationPrefix
                                    displayName = prod.displayName
                                    val safe    = appCode.replace(Regex("[^a-z0-9]"), "")
                                    licenseFile = "shv_license_$safe.json"
                                    themeName   = prod.displayName.replace(Regex("[^a-zA-Z0-9]"), "") + "Theme"
                                    pemText     = if (hasKey) try { keyFile.readText().trim() } catch (_: Exception) { "" } else ""
                                    showProductPicker = false
                                    snack(if (hasKey) "${prod.displayName} selected. Key loaded."
                                          else "${prod.displayName} selected — no key found.")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasKey) TealCol else OrangeCol)
                            ) {
                                Text(
                                    "${prod.displayName}  [${prod.activationPrefix}]  ${if (hasKey) "✓ Key" else "✗ No Key"}",
                                    color = Color.White, fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showProductPicker = false }) { Text("Cancel", color = SubText) } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Generator — writes .kt files to disk as plain text strings
// ─────────────────────────────────────────────────────────────────────────────

object KotlinLicenseGenerator {

    private val OUTPUT_BASE get() = AppStorage.kotlinLicenseOutputDir()

    fun generate(
        packageName: String, appCode: String, actPrefix: String,
        licenseFile: String, displayName: String, pem: String,
        themeName: String, mode: Step3Mode,
        manifestPath: String, mainActPath: String
    ): String {
        val safeCode = appCode.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        val outDir   = File(OUTPUT_BASE, safeCode).also { it.mkdirs() }
        val written  = mutableListOf<String>()

        File(outDir, "SHVLicense.kt").writeText(buildSHVLicense(packageName, appCode, actPrefix, licenseFile, pem))
        written.add("SHVLicense.kt")

        File(outDir, "SHVAccount.kt").writeText(buildSHVAccount(packageName, appCode, displayName))
        written.add("SHVAccount.kt")

        File(outDir, "LicenseGateScreen.kt").writeText(buildLicenseGate(packageName, displayName))
        written.add("LicenseGateScreen.kt")

        when (mode) {
            Step3Mode.PATCH -> {
                if (manifestPath.isNotBlank()) {
                    val f = File(manifestPath)
                    if (!f.exists()) throw java.io.FileNotFoundException("Manifest not found: $manifestPath")
                    written.add("AndroidManifest.xml (${patchManifest(f, outDir)})")
                }
                if (mainActPath.isNotBlank()) {
                    val f = File(mainActPath)
                    if (!f.exists()) throw java.io.FileNotFoundException("MainActivity not found: $mainActPath")
                    written.add("MainActivity.kt (${patchMainActivity(f, outDir, packageName)})")
                }
            }
            Step3Mode.TEMPLATE -> {
                File(outDir, "MainActivity.kt").writeText(buildTemplateMainActivity(packageName, displayName, themeName))
                written.add("MainActivity.kt (template)")
                File(outDir, "AndroidManifest.xml").writeText(buildTemplateManifest(packageName))
                written.add("AndroidManifest.xml (template)")
            }
        }

        return "Generated: ${written.joinToString(", ")}"
    }

    // ── Manifest patch ────────────────────────────────────────────────────

    private fun patchManifest(src: File, outDir: File): String {
        var text = src.readText()
        val hasInternet = "android.permission.INTERNET" in text
        if (!hasInternet) {
            val insertAfter = Regex("<manifest[^>]*>")
            val match = insertAfter.find(text)
            if (match != null) {
                text = text.substring(0, match.range.last + 1) +
                    "\n    <uses-permission android:name=\"android.permission.INTERNET\" />" +
                    text.substring(match.range.last + 1)
            }
        }
        File(outDir, "AndroidManifest.xml").writeText(text)
        return if (hasInternet) "already had INTERNET" else "INTERNET permission added"
    }

    // ── MainActivity patch ────────────────────────────────────────────────

    private fun patchMainActivity(src: File, outDir: File, pkg: String): String {
        var text = src.readText()
        if ("LicenseGateScreen" in text) {
            File(outDir, "MainActivity.kt").writeText(text)
            return "already patched"
        }
        if ("$pkg.ui.screens.LicenseGateScreen" !in text) {
            text = text.replaceFirst(
                Regex("(import androidx\\.activity\\.ComponentActivity)"),
                "import $pkg.ui.screens.LicenseGateScreen\nimport $pkg.license.SHVAccount\n$1"
            )
        }
        val match = Regex("""setContent\s*\{""").find(text)
        if (match != null) {
            val gate = "\n        var accessGranted by mutableStateOf(false)\n" +
                "        LaunchedEffect(Unit) {\n" +
                "            val status = withContext(Dispatchers.IO) { SHVAccount.getAccessStatus(applicationContext) }\n" +
                "            if (status.valid) accessGranted = true\n" +
                "        }\n" +
                "        if (!accessGranted) {\n" +
                "            LicenseGateScreen(context = applicationContext) { accessGranted = true }\n" +
                "            return@setContent\n" +
                "        }\n"
            text = text.substring(0, match.range.last + 1) + gate + text.substring(match.range.last + 1)
        }
        File(outDir, "MainActivity.kt").writeText(text)
        return "license gate injected"
    }

    // ── Template Manifest ─────────────────────────────────────────────────

    private fun buildTemplateManifest(pkg: String): String {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n\n" +
            "    <uses-permission android:name=\"android.permission.INTERNET\" />\n\n" +
            "    <application\n" +
            "        android:allowBackup=\"true\"\n" +
            "        android:label=\"@string/app_name\"\n" +
            "        android:theme=\"@style/Theme.AppCompat.DayNight.NoActionBar\">\n\n" +
            "        <activity\n" +
            "            android:name=\".MainActivity\"\n" +
            "            android:exported=\"true\"\n" +
            "            android:windowSoftInputMode=\"adjustResize\">\n" +
            "            <intent-filter>\n" +
            "                <action android:name=\"android.intent.action.MAIN\" />\n" +
            "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
            "            </intent-filter>\n" +
            "        </activity>\n\n" +
            "    </application>\n\n" +
            "</manifest>\n"
    }

    // ── Template MainActivity ─────────────────────────────────────────────

    private fun buildTemplateMainActivity(pkg: String, displayName: String, themeName: String): String {
        val S = "\$"
        return "package $pkg\n\n" +
            "import android.os.Bundle\n" +
            "import androidx.activity.ComponentActivity\n" +
            "import androidx.activity.OnBackPressedCallback\n" +
            "import androidx.activity.compose.setContent\n" +
            "import androidx.compose.runtime.*\n" +
            "import androidx.navigation.compose.rememberNavController\n" +
            "import $pkg.ui.screens.LicenseGateScreen\n" +
            "import $pkg.ui.theme.$themeName\n" +
            "import $pkg.license.SHVAccount\n" +
            "import kotlinx.coroutines.Dispatchers\n" +
            "import kotlinx.coroutines.withContext\n\n" +
            "class MainActivity : ComponentActivity() {\n" +
            "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
            "        super.onCreate(savedInstanceState)\n" +
            "        setContent {\n" +
            "            $themeName {\n" +
            "                val navController = rememberNavController()\n" +
            "                var accessGranted by remember { mutableStateOf(false) }\n" +
            "                var showExitDialog by remember { mutableStateOf(false) }\n\n" +
            "                LaunchedEffect(Unit) {\n" +
            "                    val status = withContext(Dispatchers.IO) {\n" +
            "                        SHVAccount.getAccessStatus(applicationContext)\n" +
            "                    }\n" +
            "                    if (status.valid) accessGranted = true\n" +
            "                }\n\n" +
            "                if (!accessGranted) {\n" +
            "                    LicenseGateScreen(context = applicationContext) { accessGranted = true }\n" +
            "                    return@$themeName\n" +
            "                }\n\n" +
            "                // TODO: Replace with your NavHost\n" +
            "                AppNavHost(navController = navController)\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n\n" +
            "@androidx.compose.runtime.Composable\n" +
            "fun AppNavHost(navController: androidx.navigation.NavHostController) {\n" +
            "    // TODO: implement your navigation\n" +
            "}\n"
    }

    // ── SHVLicense.kt ─────────────────────────────────────────────────────

    private fun buildSHVLicense(pkg: String, appCode: String, actPrefix: String,
                                licenseFile: String, pem: String): String {
        val S = "\$"
        val pemBlock = pem.trim().lines().joinToString("\n") { "    $it" }
        return "package $pkg.license\n\n" +
            "import android.content.Context\n" +
            "import android.provider.Settings\n" +
            "import org.json.JSONObject\n" +
            "import java.io.File\n" +
            "import java.security.KeyFactory\n" +
            "import java.security.MessageDigest\n" +
            "import java.security.Signature\n" +
            "import java.security.spec.X509EncodedKeySpec\n" +
            "import java.util.Base64\n" +
            "import java.util.zip.Inflater\n\n" +
            "object SHVLicense {\n\n" +
            "    private const val BUNDLE_APP   = \"$appCode\"\n" +
            "    private const val ACT_PREFIX   = \"$actPrefix\"\n" +
            "    private const val LICENSE_FILE = \"$licenseFile\"\n\n" +
            "    private val PUBLIC_KEY_PEM = \"\"\"\n" +
            "$pemBlock\n" +
            "    \"\"\".trimIndent()\n\n" +
            "    fun getDeviceCode(context: Context): String {\n" +
            "        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: \"fallback\"\n" +
            "        return MessageDigest.getInstance(\"SHA-256\").digest(androidId.toByteArray(Charsets.UTF_8))\n" +
            "            .joinToString(\"\") { \"%02x\".format(it) }.take(8).uppercase()\n" +
            "    }\n\n" +
            "    fun decodeTokenPublic(code: String): Pair<JSONObject, String> {\n" +
            "        val prefix  = \"${S}{ACT_PREFIX}-\"\n" +
            "        var cleaned = code.trim().replace(\"\\n\", \"\").replace(\" \", \"\")\n" +
            "        if (cleaned.startsWith(prefix)) cleaned = cleaned.removePrefix(prefix)\n" +
            "        cleaned = cleaned.replace(\".\", \"\")\n" +
            "        val padded     = cleaned + \"=\".repeat((4 - cleaned.length % 4) % 4)\n" +
            "        val compressed = Base64.getUrlDecoder().decode(padded)\n" +
            "        val inflater   = Inflater(); inflater.setInput(compressed)\n" +
            "        val output = ByteArray(65536); val len = inflater.inflate(output); inflater.end()\n" +
            "        val json = JSONObject(String(output, 0, len, Charsets.UTF_8))\n" +
            "        return Pair(json.getJSONObject(\"p\"), json.getString(\"s\"))\n" +
            "    }\n\n" +
            "    private fun verify(payload: JSONObject, sigB64: String): Boolean {\n" +
            "        return try {\n" +
            "            val canonical   = buildCanonicalJson(payload)\n" +
            "            val pemStripped = PUBLIC_KEY_PEM.replace(\"-----BEGIN RSA PUBLIC KEY-----\", \"\")\n" +
            "                .replace(\"-----END RSA PUBLIC KEY-----\", \"\").replace(\"\\n\", \"\").trim()\n" +
            "            val pkcs1Bytes = Base64.getDecoder().decode(pemStripped)\n" +
            "            val pubKey = KeyFactory.getInstance(\"RSA\").generatePublic(X509EncodedKeySpec(wrapPkcs1InX509(pkcs1Bytes)))\n" +
            "            val sig = Signature.getInstance(\"SHA256withRSA\")\n" +
            "            sig.initVerify(pubKey); sig.update(canonical.toByteArray(Charsets.UTF_8))\n" +
            "            sig.verify(Base64.getUrlDecoder().decode(sigB64))\n" +
            "        } catch (e: Exception) { false }\n" +
            "    }\n\n" +
            "    private fun wrapPkcs1InX509(pkcs1: ByteArray): ByteArray {\n" +
            "        val oid = byteArrayOf(0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86.toByte(), 0x48,\n" +
            "            0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00)\n" +
            "        return derEncode(0x30, oid + derEncode(0x03, byteArrayOf(0x00) + pkcs1))\n" +
            "    }\n\n" +
            "    private fun derEncode(tag: Int, content: ByteArray): ByteArray {\n" +
            "        val len = content.size\n" +
            "        val lb  = when { len < 128 -> byteArrayOf(len.toByte()); len < 256 -> byteArrayOf(0x81.toByte(), len.toByte())\n" +
            "            else -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), (len and 0xff).toByte()) }\n" +
            "        return byteArrayOf(tag.toByte()) + lb + content\n" +
            "    }\n\n" +
            "    private fun buildCanonicalJson(obj: JSONObject): String {\n" +
            "        val parts = obj.keys().asSequence().sorted()\n" +
            "            .map { k -> \"\\\"${S}k\\\":\${S}{canonicalValue(obj.get(k))}\" }.toList()\n" +
            "        return \"{${S}{parts.joinToString(\",\")}}\"\n" +
            "    }\n\n" +
            "    private fun canonicalValue(value: Any?): String = when (value) {\n" +
            "        is JSONObject -> buildCanonicalJson(value)\n" +
            "        is String     -> \"\\\"${S}value\\\"\"\n" +
            "        is Boolean    -> if (value) \"true\" else \"false\"\n" +
            "        null, JSONObject.NULL -> \"null\"\n" +
            "        else          -> value.toString()\n" +
            "    }\n\n" +
            "    data class LicenseResult(val valid: Boolean, val tier: String = \"\",\n" +
            "        val message: String = \"\", val licenseId: String = \"\")\n\n" +
            "    fun checkLicense(code: String, context: Context): LicenseResult {\n" +
            "        if (code.isBlank()) return LicenseResult(false, message = \"No activation code.\")\n" +
            "        val deviceCode = getDeviceCode(context)\n" +
            "        return try {\n" +
            "            val (payload, sigB64) = decodeTokenPublic(code)\n" +
            "            if (!verify(payload, sigB64)) return LicenseResult(false, message = \"Invalid signature.\")\n" +
            "            if (payload.optString(\"app\").lowercase() != BUNDLE_APP.lowercase())\n" +
            "                return LicenseResult(false, message = \"Wrong product.\")\n" +
            "            val bound = payload.optString(\"device_code\").trim().uppercase()\n" +
            "            if (bound.isNotEmpty() && bound != deviceCode.uppercase())\n" +
            "                return LicenseResult(false, message = \"Device mismatch. Yours: ${S}deviceCode\")\n" +
            "            val expiry = payload.optString(\"expires_at\").ifBlank { payload.optString(\"expiry\") }\n" +
            "            if (expiry.isNotBlank()) {\n" +
            "                try {\n" +
            "                    val exp = java.time.Instant.parse(expiry.replace(\" \", \"T\")\n" +
            "                        .let { if (!it.endsWith(\"Z\")) \"${S}{it}Z\" else it })\n" +
            "                    if (java.time.Instant.now().isAfter(exp))\n" +
            "                        return LicenseResult(false, message = \"License expired.\")\n" +
            "                } catch (e: Exception) { }\n" +
            "            }\n" +
            "            LicenseResult(true, payload.optString(\"tier\", \"pro\").lowercase(),\n" +
            "                \"License verified.\", payload.optString(\"license_id\"))\n" +
            "        } catch (e: Exception) { LicenseResult(false, message = \"Decode error: ${S}{e.message}\") }\n" +
            "    }\n\n" +
            "    fun saveLicense(context: Context, code: String, payload: JSONObject) {\n" +
            "        File(context.filesDir, LICENSE_FILE).writeText(JSONObject().apply {\n" +
            "            put(\"activation_code\", code); put(\"license_id\", payload.optString(\"license_id\"))\n" +
            "            put(\"tier\", payload.optString(\"tier\", \"pro\")); put(\"payload\", payload)\n" +
            "            put(\"saved_at\", java.time.Instant.now().toString())\n" +
            "        }.toString())\n" +
            "    }\n\n" +
            "    fun loadLicense(context: Context): JSONObject? = try {\n" +
            "        val f = File(context.filesDir, LICENSE_FILE)\n" +
            "        if (f.exists()) JSONObject(f.readText()) else null\n" +
            "    } catch (e: Exception) { null }\n\n" +
            "    fun deleteLicense(context: Context) = File(context.filesDir, LICENSE_FILE).delete()\n\n" +
            "    fun checkOnStartup(context: Context): LicenseResult {\n" +
            "        val saved = loadLicense(context) ?: return LicenseResult(false, message = \"No license.\")\n" +
            "        return checkLicense(saved.optString(\"activation_code\"), context)\n" +
            "    }\n" +
            "}\n"
    }

    // ── SHVAccount.kt ─────────────────────────────────────────────────────

    private fun buildSHVAccount(pkg: String, appCode: String, displayName: String): String {
        val S = "\$"
        return "package $pkg.license\n\n" +
            "import android.content.Context\n" +
            "import kotlinx.coroutines.Dispatchers\n" +
            "import kotlinx.coroutines.withContext\n" +
            "import org.json.JSONObject\n" +
            "import java.io.File\n" +
            "import java.net.HttpURLConnection\n" +
            "import java.net.URL\n" +
            "import java.security.MessageDigest\n\n" +
            "object SHVAccount {\n" +
            "    private const val SUPABASE_URL      = \"https://ovdxetyadfsxehwnbyuz.supabase.co\"\n" +
            "    private const val PUBLISHABLE_KEY   = \"sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz\"\n" +
            "    private const val DEMO_FUNCTION_URL = \"${S}{SUPABASE_URL}/functions/v1/shv-demo-v2\"\n" +
            "    const val APP_CODE   = \"$appCode\"\n" +
            "    const val DEMO_HOURS = 24\n" +
            "    private const val SESSION_FILE    = \"shv_cloud_session_${S}{APP_CODE}.json\"\n" +
            "    private const val DEMO_CACHE_FILE = \"shv_demo_cache_${S}{APP_CODE}.json\"\n\n" +
            "    data class Session(val accessToken: String, val refreshToken: String,\n" +
            "        val expiresAt: Long, val userId: String, val email: String)\n\n" +
            "    data class DemoState(val valid: Boolean, val signedIn: Boolean, val status: String,\n" +
            "        val startAllowed: Boolean, val message: String, val remainingText: String,\n" +
            "        val remainingSeconds: Long, val offline: Boolean)\n\n" +
            "    data class AccessStatus(val valid: Boolean, val mode: String, val message: String,\n" +
            "        val tier: String?, val licenseId: String?, val trialState: DemoState?)\n\n" +
            "    fun getDeviceFingerprint(context: Context): String {\n" +
            "        val raw = \"${S}{SHVLicense.getDeviceCode(context)}|${S}{APP_CODE}\"\n" +
            "        return MessageDigest.getInstance(\"SHA-256\").digest(raw.toByteArray(Charsets.UTF_8))\n" +
            "            .joinToString(\"\") { \"%02x\".format(it) }\n" +
            "    }\n\n" +
            "    private fun post(url: String, body: JSONObject, token: String? = null): JSONObject {\n" +
            "        val conn = URL(url).openConnection() as HttpURLConnection\n" +
            "        conn.requestMethod = \"POST\"\n" +
            "        conn.setRequestProperty(\"Content-Type\", \"application/json\")\n" +
            "        conn.setRequestProperty(\"apikey\", PUBLISHABLE_KEY)\n" +
            "        if (token != null) conn.setRequestProperty(\"Authorization\", \"Bearer ${S}token\")\n" +
            "        conn.doOutput = true; conn.connectTimeout = 14_000; conn.readTimeout = 14_000\n" +
            "        conn.outputStream.use { it.write(body.toString().toByteArray()) }\n" +
            "        val code   = conn.responseCode\n" +
            "        val stream = if (code in 200..299) conn.inputStream else conn.errorStream\n" +
            "        val resp   = stream?.bufferedReader()?.readText() ?: \"{}\"\n" +
            "        if (code !in 200..299) throw RuntimeException(\n" +
            "            try { JSONObject(resp).optString(\"message\", resp) } catch (e: Exception) { resp })\n" +
            "        return JSONObject(resp)\n" +
            "    }\n\n" +
            "    private fun sessionFile(ctx: Context) = File(ctx.filesDir, SESSION_FILE)\n\n" +
            "    fun loadSession(ctx: Context): Session? = try {\n" +
            "        val j = JSONObject(sessionFile(ctx).readText())\n" +
            "        val t = j.optString(\"access_token\")\n" +
            "        if (t.isBlank()) null\n" +
            "        else Session(t, j.optString(\"refresh_token\"), j.optLong(\"expires_at\"),\n" +
            "            j.optJSONObject(\"user\")?.optString(\"id\") ?: \"\",\n" +
            "            j.optJSONObject(\"user\")?.optString(\"email\") ?: \"\")\n" +
            "    } catch (e: Exception) { null }\n\n" +
            "    private fun saveSession(ctx: Context, j: JSONObject) = sessionFile(ctx).writeText(j.toString())\n\n" +
            "    fun clearSession(ctx: Context) { sessionFile(ctx).delete() }\n\n" +
            "    suspend fun signIn(ctx: Context, email: String, password: String): Session =\n" +
            "        withContext(Dispatchers.IO) {\n" +
            "            val result = post(\"${S}{SUPABASE_URL}/auth/v1/token?grant_type=password\",\n" +
            "                JSONObject().put(\"email\", email.trim()).put(\"password\", password))\n" +
            "            saveSession(ctx, result)\n" +
            "            Session(result.optString(\"access_token\"), result.optString(\"refresh_token\"),\n" +
            "                result.optLong(\"expires_at\"),\n" +
            "                result.optJSONObject(\"user\")?.optString(\"id\") ?: \"\",\n" +
            "                result.optJSONObject(\"user\")?.optString(\"email\") ?: \"\")\n" +
            "        }\n\n" +
            "    suspend fun signUp(ctx: Context, email: String, password: String): Pair<Boolean, String> =\n" +
            "        withContext(Dispatchers.IO) {\n" +
            "            val result = post(\"${S}{SUPABASE_URL}/auth/v1/signup\",\n" +
            "                JSONObject().put(\"email\", email.trim()).put(\"password\", password))\n" +
            "            val needsConfirm = result.optJSONObject(\"user\")\n" +
            "                ?.optString(\"confirmation_sent_at\")?.isNotBlank() ?: false\n" +
            "            Pair(needsConfirm, result.optJSONObject(\"user\")?.optString(\"id\") ?: \"\")\n" +
            "        }\n\n" +
            "    private suspend fun demoCall(ctx: Context, action: String, extra: JSONObject? = null): JSONObject =\n" +
            "        withContext(Dispatchers.IO) {\n" +
            "            val session = loadSession(ctx) ?: throw RuntimeException(\"Sign in first.\")\n" +
            "            val body = JSONObject().put(\"action\", action).put(\"app_code\", APP_CODE)\n" +
            "                .put(\"demo_hours\", DEMO_HOURS)\n" +
            "                .put(\"device_code\", SHVLicense.getDeviceCode(ctx))\n" +
            "                .put(\"device_fingerprint_hash\", getDeviceFingerprint(ctx))\n" +
            "            extra?.keys()?.forEach { k -> body.put(k, extra.get(k)) }\n" +
            "            post(DEMO_FUNCTION_URL, body, session.accessToken)\n" +
            "        }\n\n" +
            "    suspend fun getDemoStatus(ctx: Context): DemoState = withContext(Dispatchers.IO) {\n" +
            "        val session = loadSession(ctx) ?: return@withContext DemoState(false, false,\n" +
            "            \"none\", false, \"Sign in to your SH Vertex account.\", \"0m\", 0, false)\n" +
            "        try { parseDemoResponse(demoCall(ctx, \"status\"), true) }\n" +
            "        catch (e: Exception) { DemoState(false, true, \"error\", false,\n" +
            "            \"Cannot verify demo.\", \"0m\", 0, false) }\n" +
            "    }\n\n" +
            "    suspend fun startDemo(ctx: Context): DemoState =\n" +
            "        withContext(Dispatchers.IO) { parseDemoResponse(demoCall(ctx, \"start\"), true) }\n\n" +
            "    private fun parseDemoResponse(resp: JSONObject, signedIn: Boolean): DemoState {\n" +
            "        val status    = resp.optString(\"status\", \"none\").lowercase()\n" +
            "        val expiresAt = parseIso(resp.optString(\"demo_expires_at\"))\n" +
            "        val serverNow = parseIso(resp.optString(\"server_now\")) ?: java.time.Instant.now()\n" +
            "        val active    = resp.optBoolean(\"demo_allowed\") && status == \"active\"\n" +
            "            && expiresAt != null && expiresAt.isAfter(serverNow)\n" +
            "        val remSec = if (active && expiresAt != null)\n" +
            "            maxOf(0L, expiresAt.epochSecond - serverNow.epochSecond) else 0L\n" +
            "        return DemoState(active, signedIn, status, resp.optBoolean(\"start_allowed\"),\n" +
            "            resp.optString(\"message\").ifBlank {\n" +
            "                if (active) \"Demo active: ${S}{formatRemaining(remSec)} remaining.\" else \"Demo not available.\" },\n" +
            "            formatRemaining(remSec), remSec, false)\n" +
            "    }\n\n" +
            "    suspend fun getAccessStatus(ctx: Context): AccessStatus = withContext(Dispatchers.IO) {\n" +
            "        val lic = SHVLicense.loadLicense(ctx)\n" +
            "        if (lic != null) {\n" +
            "            val r = SHVLicense.checkLicense(lic.optString(\"activation_code\"), ctx)\n" +
            "            if (r.valid) return@withContext AccessStatus(true, \"licensed\", r.message, r.tier, r.licenseId, null)\n" +
            "        }\n" +
            "        val demo = getDemoStatus(ctx)\n" +
            "        AccessStatus(demo.valid, if (demo.valid) \"trial\" else \"none\", demo.message, null, null, demo)\n" +
            "    }\n\n" +
            "    private fun parseIso(s: String?): java.time.Instant? {\n" +
            "        if (s.isNullOrBlank()) return null\n" +
            "        return try { java.time.Instant.parse(s.trim().replace(\" \", \"T\")\n" +
            "            .let { if (!it.endsWith(\"Z\") && !it.contains(\"+\")) \"${S}{it}Z\" else it })\n" +
            "        } catch (e: Exception) { null }\n" +
            "    }\n\n" +
            "    fun formatRemaining(seconds: Long): String {\n" +
            "        if (seconds <= 0) return \"0m\"\n" +
            "        val d = seconds / 86400; val h = (seconds % 86400) / 3600; val m = (seconds % 3600) / 60\n" +
            "        return when { d > 0 -> \"${S}{d}d ${S}{h}h\"; h > 0 -> \"${S}{h}h ${S}{m}m\"; else -> \"${S}{m}m\" }\n" +
            "    }\n" +
            "}\n"
    }

    // ── LicenseGateScreen.kt ──────────────────────────────────────────────

    private fun buildLicenseGate(pkg: String, displayName: String): String {
        val S = "\$"
        return "package $pkg.ui.screens\n\n" +
            "import android.content.Context\n" +
            "import androidx.compose.foundation.layout.*\n" +
            "import androidx.compose.foundation.rememberScrollState\n" +
            "import androidx.compose.foundation.verticalScroll\n" +
            "import androidx.compose.material3.*\n" +
            "import androidx.compose.runtime.*\n" +
            "import androidx.compose.ui.Alignment\n" +
            "import androidx.compose.ui.Modifier\n" +
            "import androidx.compose.ui.graphics.Color\n" +
            "import androidx.compose.ui.platform.LocalClipboardManager\n" +
            "import androidx.compose.ui.text.AnnotatedString\n" +
            "import androidx.compose.ui.text.font.FontWeight\n" +
            "import androidx.compose.ui.text.input.PasswordVisualTransformation\n" +
            "import androidx.compose.ui.text.style.TextAlign\n" +
            "import androidx.compose.ui.unit.dp\n" +
            "import androidx.compose.ui.unit.sp\n" +
            "import kotlinx.coroutines.Dispatchers\n" +
            "import kotlinx.coroutines.launch\n" +
            "import kotlinx.coroutines.withContext\n" +
            "import $pkg.license.SHVAccount\n" +
            "import $pkg.license.SHVLicense\n\n" +
            "@Composable\n" +
            "fun LicenseGateScreen(context: Context, onAccessGranted: () -> Unit) {\n" +
            "    val scope = rememberCoroutineScope()\n" +
            "    val clipboard = LocalClipboardManager.current\n" +
            "    var statusMessage  by remember { mutableStateOf(\"Checking license…\") }\n" +
            "    var isLoading      by remember { mutableStateOf(true) }\n" +
            "    var showActivate   by remember { mutableStateOf(false) }\n" +
            "    var showSignIn     by remember { mutableStateOf(false) }\n" +
            "    var activationCode by remember { mutableStateOf(\"\") }\n" +
            "    var emailField     by remember { mutableStateOf(\"\") }\n" +
            "    var passwordField  by remember { mutableStateOf(\"\") }\n" +
            "    var fieldError     by remember { mutableStateOf(\"\") }\n" +
            "    val deviceCode     = remember { SHVLicense.getDeviceCode(context) }\n\n" +
            "    LaunchedEffect(Unit) {\n" +
            "        isLoading = true\n" +
            "        val access = withContext(Dispatchers.IO) { SHVAccount.getAccessStatus(context) }\n" +
            "        isLoading = false\n" +
            "        if (access.valid) onAccessGranted() else statusMessage = access.message\n" +
            "    }\n\n" +
            "    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF000000)) {\n" +
            "        Column(\n" +
            "            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())\n" +
            "                .padding(horizontal = 20.dp, vertical = 32.dp),\n" +
            "            horizontalAlignment = Alignment.CenterHorizontally,\n" +
            "            verticalArrangement = Arrangement.spacedBy(16.dp)\n" +
            "        ) {\n" +
            "            Text(\"$displayName\", fontSize = 26.sp, fontWeight = FontWeight.Bold,\n" +
            "                color = Color(0xFF00BCD4), textAlign = TextAlign.Center)\n" +
            "            Text(\"Activate your license or sign in to start the trial.\",\n" +
            "                fontSize = 13.sp, color = Color(0xFF8F9BB3), textAlign = TextAlign.Center)\n" +
            "            Card(modifier = Modifier.fillMaxWidth(),\n" +
            "                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {\n" +
            "                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {\n" +
            "                    Text(\"DEVICE CODE\", fontSize = 11.sp, color = Color(0xFF8F9BB3), fontWeight = FontWeight.Bold)\n" +
            "                    Text(deviceCode, fontSize = 18.sp, color = Color(0xFFB1BAD3), fontWeight = FontWeight.Bold)\n" +
            "                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF00BCD4), strokeWidth = 2.dp)\n" +
            "                    else Text(statusMessage, fontSize = 12.sp, color = Color(0xFFD9A838))\n" +
            "                }\n" +
            "            }\n\n" +
            "            if (showActivate) {\n" +
            "                Card(modifier = Modifier.fillMaxWidth(),\n" +
            "                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {\n" +
            "                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {\n" +
            "                        Text(\"Activate License\", fontSize = 15.sp, color = Color(0xFF1B9E46), fontWeight = FontWeight.Bold)\n" +
            "                        OutlinedTextField(value = activationCode, onValueChange = { activationCode = it; fieldError = \"\" },\n" +
            "                            label = { Text(\"Activation Code\") }, modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 3,\n" +
            "                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1B9E46),\n" +
            "                                unfocusedBorderColor = Color(0xFF444444), focusedTextColor = Color(0xFFB1BAD3),\n" +
            "                                unfocusedTextColor = Color(0xFFB1BAD3)))\n" +
            "                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n" +
            "                            Button(onClick = { val cb = clipboard.getText()?.text ?: \"\"; if (cb.isNotBlank()) activationCode = cb },\n" +
            "                                modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))) { Text(\"Paste\") }\n" +
            "                            Button(onClick = { activationCode = \"\" }, modifier = Modifier.weight(1f),\n" +
            "                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1818))) { Text(\"Clear\") }\n" +
            "                        }\n" +
            "                        if (fieldError.isNotBlank()) Text(fieldError, fontSize = 12.sp, color = Color(0xFFD93838))\n" +
            "                        Button(onClick = {\n" +
            "                            val code = activationCode.trim()\n" +
            "                            if (code.isBlank()) { fieldError = \"Paste your activation code.\"; return@Button }\n" +
            "                            isLoading = true; statusMessage = \"Verifying…\"\n" +
            "                            scope.launch {\n" +
            "                                val result = withContext(Dispatchers.IO) { SHVLicense.checkLicense(code, context) }\n" +
            "                                isLoading = false\n" +
            "                                if (result.valid) {\n" +
            "                                    withContext(Dispatchers.IO) {\n" +
            "                                        val (payload, _) = SHVLicense.decodeTokenPublic(code)\n" +
            "                                        SHVLicense.saveLicense(context, code, payload)\n" +
            "                                    }\n" +
            "                                    onAccessGranted()\n" +
            "                                } else { fieldError = result.message; statusMessage = result.message }\n" +
            "                            }\n" +
            "                        }, modifier = Modifier.fillMaxWidth(),\n" +
            "                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B9E46))) {\n" +
            "                            Text(\"Activate\", fontWeight = FontWeight.Bold)\n" +
            "                        }\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n\n" +
            "            if (showSignIn) {\n" +
            "                Card(modifier = Modifier.fillMaxWidth(),\n" +
            "                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {\n" +
            "                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {\n" +
            "                        Text(\"Sign In — SH Vertex Account\", fontSize = 15.sp, color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold)\n" +
            "                        OutlinedTextField(value = emailField, onValueChange = { emailField = it; fieldError = \"\" },\n" +
            "                            label = { Text(\"Email\") }, modifier = Modifier.fillMaxWidth(), singleLine = true,\n" +
            "                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00BCD4),\n" +
            "                                unfocusedBorderColor = Color(0xFF444444), focusedTextColor = Color(0xFFB1BAD3),\n" +
            "                                unfocusedTextColor = Color(0xFFB1BAD3)))\n" +
            "                        OutlinedTextField(value = passwordField, onValueChange = { passwordField = it; fieldError = \"\" },\n" +
            "                            label = { Text(\"Password\") }, modifier = Modifier.fillMaxWidth(), singleLine = true,\n" +
            "                            visualTransformation = PasswordVisualTransformation(),\n" +
            "                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00BCD4),\n" +
            "                                unfocusedBorderColor = Color(0xFF444444), focusedTextColor = Color(0xFFB1BAD3),\n" +
            "                                unfocusedTextColor = Color(0xFFB1BAD3)))\n" +
            "                        if (fieldError.isNotBlank()) Text(fieldError, fontSize = 12.sp, color = Color(0xFFD93838))\n" +
            "                        Button(onClick = {\n" +
            "                            if (emailField.isBlank() || passwordField.isBlank()) { fieldError = \"Email and password required.\"; return@Button }\n" +
            "                            isLoading = true; statusMessage = \"Signing in…\"\n" +
            "                            scope.launch {\n" +
            "                                try {\n" +
            "                                    withContext(Dispatchers.IO) { SHVAccount.signIn(context, emailField, passwordField) }\n" +
            "                                    val access = withContext(Dispatchers.IO) { SHVAccount.getAccessStatus(context) }\n" +
            "                                    isLoading = false\n" +
            "                                    if (access.valid) onAccessGranted()\n" +
            "                                    else { statusMessage = access.message; fieldError = access.message }\n" +
            "                                } catch (e: Exception) { isLoading = false; fieldError = e.message ?: \"Sign-in failed.\"; statusMessage = fieldError }\n" +
            "                            }\n" +
            "                        }, modifier = Modifier.fillMaxWidth(),\n" +
            "                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))) {\n" +
            "                            Text(\"Sign In\", fontWeight = FontWeight.Bold, color = Color.Black)\n" +
            "                        }\n" +
            "                        Button(onClick = {\n" +
            "                            isLoading = true; statusMessage = \"Starting demo…\"\n" +
            "                            scope.launch {\n" +
            "                                try {\n" +
            "                                    val demo = withContext(Dispatchers.IO) { SHVAccount.startDemo(context) }\n" +
            "                                    isLoading = false\n" +
            "                                    if (demo.valid) onAccessGranted()\n" +
            "                                    else { statusMessage = demo.message; fieldError = demo.message }\n" +
            "                                } catch (e: Exception) { isLoading = false; fieldError = e.message ?: \"Could not start demo.\"; statusMessage = fieldError }\n" +
            "                            }\n" +
            "                        }, modifier = Modifier.fillMaxWidth(),\n" +
            "                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B9E46))) {\n" +
            "                            Text(\"Start Demo\", fontWeight = FontWeight.Bold)\n" +
            "                        }\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n\n" +
            "            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {\n" +
            "                Button(onClick = { showActivate = !showActivate; showSignIn = false; fieldError = \"\" },\n" +
            "                    modifier = Modifier.fillMaxWidth(),\n" +
            "                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B9E46))) {\n" +
            "                    Text(if (showActivate) \"Hide Activation\" else \"Activate License\", fontWeight = FontWeight.Bold)\n" +
            "                }\n" +
            "                Button(onClick = { showSignIn = !showSignIn; showActivate = false; fieldError = \"\" },\n" +
            "                    modifier = Modifier.fillMaxWidth(),\n" +
            "                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))) {\n" +
            "                    Text(if (showSignIn) \"Hide Sign In\" else \"Sign In / Start Demo\", fontWeight = FontWeight.Bold, color = Color.Black)\n" +
            "                }\n" +
            "            }\n\n" +
            "            Spacer(Modifier.height(24.dp))\n" +
            "            Text(\"Powered by SH Vertex Technologies\", fontSize = 11.sp,\n" +
            "                color = Color(0xFF444444), textAlign = TextAlign.Center)\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
    }
}
