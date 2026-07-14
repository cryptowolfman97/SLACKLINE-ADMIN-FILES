package com.example.slacklineadminapp.ui.screens

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.ui.components.*
import com.example.slacklineadminapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.zip.Inflater

// Data class representing either a legacy or new product for unified display
data class AnyProduct(
    val id: String,
    val displayName: String,
    val activationPrefix: String,
    val bundleApp: String,
    val githubOwner: String = "",
    val githubRepo: String = "",
    val githubBranch: String = "main",
    val githubPath: String = "",
    val color: String = "#00A383",
    val isLegacy: Boolean
) {
    val revoUrl: String get() {
        if (githubOwner.isBlank() || githubRepo.isBlank() || githubPath.isBlank()) return ""
        return "https://raw.githubusercontent.com/$githubOwner/$githubRepo/${githubBranch.ifBlank{"main"}}/$githubPath"
    }
    fun publicKeyPem(): String = if (isLegacy) {
        val cfg = ProductRegistry.get(id)
        if (cfg != null) EngineCache.get(cfg).publicKeyPem() else ""
    } else {
        NewLicenseStore.publicKeyPem(id)
    }
    fun hasAuthority(): Boolean = if (isLegacy) {
        val cfg = ProductRegistry.get(id)
        if (cfg != null) EngineCache.get(cfg).hasAuthority() else false
    } else {
        NewLicenseStore.hasAuthority(id)
    }
}

class LicenseToolsViewModel : ViewModel() {

    private val _toast   = MutableStateFlow("")
    val toast: StateFlow<String> = _toast

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // ── Template generator ────────────────────────────────────────────────
    var selectedProduct by mutableStateOf<AnyProduct?>(null)
    var tplAppClass     by mutableStateOf("MyNewApp")
    var tplOutputName   by mutableStateOf("")
    var tplRevoUrl      by mutableStateOf("")
    var tplDemoHours    by mutableStateOf("24")
    var tplResult       by mutableStateOf("")

    // ── License verifier ──────────────────────────────────────────────────
    var verifyCode      by mutableStateOf("")
    var verifyResult    by mutableStateOf("")
    var verifyIsOk      by mutableStateOf<Boolean?>(null)
    var verifyPayload   by mutableStateOf<Map<String, Any?>?>(null)

    fun consumeToast() { _toast.value = "" }

    fun allProducts(): List<AnyProduct> {
        val legacy = ProductRegistry.all().map { cfg ->
            AnyProduct(
                id = cfg.id, displayName = cfg.displayName,
                activationPrefix = cfg.activationPrefix, bundleApp = cfg.bundleApp,
                githubOwner = cfg.githubOwner, githubRepo = cfg.githubRepo,
                githubBranch = cfg.githubBranch, githubPath = cfg.githubPath,
                color = cfg.color, isLegacy = true
            )
        }
        val newProds = NewLicenseStore.allProducts().map { p ->
            AnyProduct(
                id = p.id, displayName = p.displayName,
                activationPrefix = p.activationPrefix, bundleApp = p.bundleApp,
                githubOwner = p.githubOwner, githubRepo = p.githubRepo,
                githubBranch = p.githubBranch, githubPath = p.githubPath,
                color = p.color, isLegacy = false
            )
        }
        return legacy + newProds
    }

    fun selectProduct(p: AnyProduct) {
        selectedProduct = p
        tplRevoUrl = p.revoUrl
        tplAppClass = p.displayName.replace(" ", "") + "App"
    }

    // ── Template generation ───────────────────────────────────────────────

    fun generateTemplate() = viewModelScope.launch(Dispatchers.IO) {
        val prod = selectedProduct ?: run { _toast.value = "Select a product first."; return@launch }
        if (!prod.hasAuthority()) { _toast.value = "Product has no authority keys."; return@launch }
        if (tplAppClass.isBlank()) { _toast.value = "App class name required."; return@launch }
        _loading.value = true
        try {
            val pubPem  = prod.publicKeyPem()
            val appCls  = tplAppClass.trim().let { if (it.endsWith("App")) it else "${it}App" }
            val outName = tplOutputName.trim().ifBlank { "${appCls}_licensed.py" }
            val revo    = tplRevoUrl.trim()
            val hours   = tplDemoHours.trim().toIntOrNull() ?: 24

            val template = buildPyTemplate(
                appClass = appCls, pubPem = pubPem,
                actPrefix = prod.activationPrefix, bundleApp = prod.bundleApp,
                revoUrl = revo, productName = prod.displayName, demoHours = hours
            )

            val dir  = AppStorage.injectedAppsDir().also { it.mkdirs() }
            val file = File(dir, outName)
            file.writeText(template)

            tplResult = "✓ Saved to:\n${file.absolutePath}"
            AppStorage.logActivity("Template Generated", "App: $appCls  Product: ${prod.displayName}", "LicenseTools")
            _toast.value = "Template saved: $outName"
        } catch (e: Exception) {
            tplResult = "Error: ${e.message}"
            _toast.value = "Generation failed: ${e.message}"
        }
        _loading.value = false
    }

    // ── License verifier ─────────────────────────────────────────────────

    fun verifyLicense(products: List<AnyProduct>) = viewModelScope.launch(Dispatchers.IO) {
        val code = verifyCode.trim()
        if (code.isBlank()) { _toast.value = "Paste a license code first."; return@launch }
        _loading.value = true
        verifyResult   = "Verifying..."
        verifyIsOk     = null
        verifyPayload  = null
        try {
            // Decode the code
            val idx     = code.indexOf('-')
            val cleaned = code.substring(idx + 1).replace(".", "").let {
                it + "=".repeat((4 - it.length % 4) % 4)
            }
            val compressed = Base64.decode(
                cleaned.replace('-','+').replace('_','/'), Base64.DEFAULT
            )
            // Inflate
            val inf = Inflater()
            inf.setInput(compressed)
            val out = ByteArray(compressed.size * 10)
            val n   = inf.inflate(out)
            inf.end()
            val json    = JSONObject(String(out, 0, n, Charsets.UTF_8))
            val payload = json.getJSONObject("p")
            val sigB64  = json.getString("s")

            val payMap = mutableMapOf<String, Any?>()
            payload.keys().forEach { k -> payMap[k] = payload.get(k) }
            verifyPayload = payMap

            val bundleApp = payMap["app"]?.toString() ?: ""

            // Find matching product
            val matchProd = products.find {
                it.bundleApp.equals(bundleApp, ignoreCase = true) ||
                it.activationPrefix == code.substringBefore("-")
            }

            if (matchProd == null) {
                verifyIsOk   = false
                verifyResult = "✗ No product found matching bundle_app='$bundleApp'.\nCheck that this license belongs to one of your products."
                _loading.value = false
                return@launch
            }

            if (!matchProd.hasAuthority()) {
                verifyIsOk   = false
                verifyResult = "✗ Product '${matchProd.displayName}' has no authority keys loaded."
                _loading.value = false
                return@launch
            }

            val pubPem = matchProd.publicKeyPem()

            // Verify signature using BouncyCastle (same as LicenseEngine)
            val canonical  = AppStorage.canonicalJson(payMap)
            val sig        = Base64.decode(
                sigB64.replace('-','+').replace('_','/') + "==", Base64.DEFAULT
            )
            val pubKeyBytes = pubPem.lines()
                .filter { !it.startsWith("-----") }.joinToString("").let {
                    Base64.decode(it, Base64.DEFAULT)
                }

            val algId = org.bouncycastle.asn1.x509.AlgorithmIdentifier(
                org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.rsaEncryption,
                org.bouncycastle.asn1.DERNull.INSTANCE
            )
            val spki = org.bouncycastle.asn1.x509.SubjectPublicKeyInfo(
                algId,
                org.bouncycastle.asn1.pkcs.RSAPublicKey.getInstance(pubKeyBytes)
            ).encoded
            val pubKey = java.security.KeyFactory.getInstance("RSA", "BC")
                .generatePublic(java.security.spec.X509EncodedKeySpec(spki))

            val verifier = java.security.Signature.getInstance("SHA256withRSA", "BC")
            verifier.initVerify(pubKey)
            verifier.update(canonical)
            val valid = verifier.verify(sig)

            if (valid) {
                verifyIsOk   = true
                verifyResult = "✓ VALID — Signature verified against '${matchProd.displayName}'"
            } else {
                verifyIsOk   = false
                verifyResult = "✗ INVALID — Signature check failed against '${matchProd.displayName}'"
            }
        } catch (e: Exception) {
            verifyIsOk   = false
            verifyResult = "✗ Error: ${e.message}"
        }
        _loading.value = false
    }

    // ── Python template builder ───────────────────────────────────────────

    private fun buildPyTemplate(
        appClass: String, pubPem: String, actPrefix: String,
        bundleApp: String, revoUrl: String, productName: String, demoHours: Int
    ): String {
        val lines = mutableListOf<String>()
        fun L(s: String = "") { lines.add(s) }

        L("# SlackLine License Gate — Generated by SHV Admin Panel")
        L("# Product: $productName  |  Generated: ${AppStorage.utcNow()}")
        L("# Activation prefix: $actPrefix-  |  Bundle app: $bundleApp")
        L("import base64, hashlib, json, os, zlib")
        L("from datetime import datetime, timezone")
        L("from kivy.clock import Clock")
        L("from kivy.metrics import dp")
        L("from kivy.core.clipboard import Clipboard")
        L("from kivy.utils import get_color_from_hex")
        L("from kivy.uix.screenmanager import ScreenManager, Screen, NoTransition")
        L("from kivymd.app import MDApp")
        L("from kivymd.uix.button import MDRaisedButton")
        L("from kivymd.uix.textfield import MDTextField")
        L("from kivymd.uix.boxlayout import MDBoxLayout")
        L("from kivymd.uix.label import MDLabel")
        L("try:")
        L("    import rsa as _rsa")
        L("    _SHV_RSA_OK = True")
        L("except ImportError:")
        L("    _SHV_RSA_OK = False")
        L()
        L("_SHV_BUNDLE_APP  = '${bundleApp}'")
        L("_SHV_ACT_PREFIX  = '${actPrefix}'")
        L("_SHV_DEMO_HOURS  = ${demoHours}")
        L("_SHV_REVO_URL    = '${revoUrl}'")
        L("_SHV_PUBLIC_KEY_PEM = b\"\"\"${pubPem}\"\"\"")
        L("_SHV_LIC_FILE    = 'shv_license_${bundleApp}.json'")
        L()
        L("def _shv_data_dir():")
        L("    try:")
        L("        app = MDApp.get_running_app()")
        L("        if app and getattr(app, 'user_data_dir', None):")
        L("            return app.user_data_dir")
        L("    except Exception:")
        L("        pass")
        L("    return os.path.join(os.path.expanduser('~'), '.shv_app_data')")
        L()
        L("def _shv_get_device_code():")
        L("    raw = ''")
        L("    try:")
        L("        from jnius import autoclass")
        L("        S  = autoclass('android.provider.Settings\$Secure')")
        L("        PA = autoclass('org.kivy.android.PythonActivity')")
        L("        raw = str(S.getString(PA.mActivity.getContentResolver(), S.ANDROID_ID) or '')")
        L("    except Exception:")
        L("        pass")
        L("    if not raw:")
        L("        try:")
        L("            import uuid; raw = str(uuid.getnode())")
        L("        except Exception:")
        L("            raw = 'fallback'")
        L("    return hashlib.sha256(raw.encode('utf-8')).hexdigest()[:8].upper()")
        L()
        L("def _shv_decode_code(code):")
        L("    prefix = _SHV_ACT_PREFIX + '-'")
        L("    s = code.strip().replace('\\n','').replace(' ','')")
        L("    if s.startswith(prefix): s = s[len(prefix):]")
        L("    s = s.replace('.','') + '=' * ((4 - len(s.replace('.','')) % 4) % 4)")
        L("    raw = base64.urlsafe_b64decode(s.encode('ascii'))")
        L("    data = json.loads(zlib.decompress(raw).decode('utf-8'))")
        L("    return data['p'], data['s']")
        L()
        L("def _shv_verify(payload, sig_b64):")
        L("    if not _SHV_RSA_OK: return False")
        L("    try:")
        L("        pub    = _rsa.PublicKey.load_pkcs1(_SHV_PUBLIC_KEY_PEM)")
        L("        canon  = json.dumps(payload, sort_keys=True, separators=(',',':')).encode('utf-8')")
        L("        sig    = base64.urlsafe_b64decode(sig_b64.encode('ascii') + b'==')")
        L("        _rsa.verify(canon, sig, pub)")
        L("        return True")
        L("    except Exception:")
        L("        return False")
        L()
        L("def _shv_check_license(code, device_code=None):")
        L("    if not code or not code.strip(): return False, '', 'No code.', ''")
        L("    if device_code is None: device_code = _shv_get_device_code()")
        L("    try: payload, sig_b64 = _shv_decode_code(code.strip())")
        L("    except Exception as e: return False, '', f'Decode error: {e}', ''")
        L("    if not _shv_verify(payload, sig_b64): return False, '', 'Signature invalid.', ''")
        L("    if str(payload.get('app','')).lower() != _SHV_BUNDLE_APP.lower():")
        L("        return False, '', 'Wrong product.', ''")
        L("    bound = str(payload.get('device_code','')).strip().upper()")
        L("    if bound and bound != device_code.upper():")
        L("        return False, '', f'Device mismatch. Yours: {device_code.upper()}  Bound: {bound}', ''")
        L("    expiry = str(payload.get('expires_at','') or payload.get('expiry','')).strip()")
        L("    if expiry:")
        L("        try:")
        L("            from datetime import timezone")
        L("            exp = datetime.fromisoformat(expiry.replace('Z','+00:00'))")
        L("            if datetime.now(timezone.utc) > exp:")
        L("                return False, '', f'Expired {expiry[:10]}.', ''")
        L("        except Exception: pass")
        L("    return True, str(payload.get('tier','pro')).lower(), 'Verified.', payload.get('license_id','')")
        L()
        L("def _shv_load_license():")
        L("    try:")
        L("        with open(os.path.join(_shv_data_dir(), _SHV_LIC_FILE), 'r') as f:")
        L("            return json.load(f)")
        L("    except Exception: return None")
        L()
        L("def _shv_save_license(code, payload):")
        L("    os.makedirs(_shv_data_dir(), exist_ok=True)")
        L("    with open(os.path.join(_shv_data_dir(), _SHV_LIC_FILE), 'w') as f:")
        L("        json.dump({'activation_code': code, 'payload': payload,")
        L("            'license_id': payload.get('license_id',''),")
        L("            'tier': payload.get('tier','pro')}, f)")
        L()
        L("# ── Replace this with your real app UI ──────────────────────────────")
        L("def _shv_original_build(tier='pro'):")
        L("    root = MDBoxLayout(orientation='vertical', padding=dp(20), spacing=dp(12))")
        L("    root.add_widget(MDLabel(")
        L("        text=f'Welcome to ${productName}!  Tier: {tier.upper()}',")
        L("        font_style='H5', halign='center',")
        L("        theme_text_color='Custom', text_color=get_color_from_hex('#00a383'),")
        L("        size_hint_y=None, height=dp(60)")
        L("    ))")
        L("    return root")
        L()
        L("class SHVActivationGate:")
        L("    def __init__(self, on_activated):")
        L("        self._on_activated = on_activated")
        L("        self._device_code  = _shv_get_device_code()")
        L("        self._widget       = self._build()")
        L("    def get_widget(self): return self._widget")
        L("    def _build(self):")
        L("        root = MDBoxLayout(orientation='vertical', padding=dp(28), spacing=dp(14),")
        L("                           md_bg_color=get_color_from_hex('#000000'))")
        L("        def _lbl(t, s='14sp', c='#b1bad3', b=False):")
        L("            lb = MDLabel(text=t, font_size=s, halign='center',")
        L("                         theme_text_color='Custom', text_color=get_color_from_hex(c),")
        L("                         size_hint_y=None, bold=b)")
        L("            lb.bind(texture_size=lambda i,v: setattr(i,'height',max(v[1],dp(22))))")
        L("            return lb")
        L("        def _btn(t, c='#00a383'):")
        L("            return MDRaisedButton(text=t, md_bg_color=get_color_from_hex(c),")
        L("                                  size_hint=(1,None), height=dp(48))")
        L("        root.add_widget(_lbl('${productName}','26sp','#00a383',b=True))")
        L("        root.add_widget(_lbl('Activate Your License','18sp','#b1bad3',b=True))")
        L("        root.add_widget(_lbl(f'Device: {self._device_code}','13sp','#8f9bb3'))")
        L("        copy_btn = _btn('Copy Device Code','#2b82ba')")
        L("        copy_btn.bind(on_release=lambda *_: Clipboard.copy(self._device_code))")
        L("        root.add_widget(copy_btn)")
        L("        self._inp = MDTextField(hint_text='${actPrefix}- activation code',")
        L("                               multiline=True, mode='rectangle',")
        L("                               size_hint_y=None, height=dp(110))")
        L("        root.add_widget(self._inp)")
        L("        paste_btn = _btn('Paste from Clipboard','#82429e')")
        L("        paste_btn.bind(on_release=lambda *_: setattr(self._inp,'text',Clipboard.paste() or ''))")
        L("        root.add_widget(paste_btn)")
        L("        self._status = _lbl('','13sp','#d93838')")
        L("        root.add_widget(self._status)")
        L("        act_btn = _btn('Activate License','#1b9e46')")
        L("        act_btn.bind(on_release=lambda *_: self._activate())")
        L("        root.add_widget(act_btn)")
        L("        return root")
        L("    def _activate(self):")
        L("        code = (self._inp.text or '').strip()")
        L("        if not code: return")
        L("        ok, tier, msg, lid = _shv_check_license(code, self._device_code)")
        L("        if ok:")
        L("            try: p, _ = _shv_decode_code(code); _shv_save_license(code, p)")
        L("            except Exception: pass")
        L("            self._status.text = '✓ Activated!'")
        L("            self._status.text_color = get_color_from_hex('#1b9e46')")
        L("            Clock.schedule_once(lambda dt: self._on_activated(tier), 0.5)")
        L("        else:")
        L("            self._status.text = msg")
        L("            self._status.text_color = get_color_from_hex('#d93838')")
        L()
        L("class ${appClass}(MDApp):")
        L("    def build(self):")
        L("        self.theme_cls.theme_style = 'Dark'")
        L("        lic = _shv_load_license()")
        L("        if lic:")
        L("            ok, tier, msg, lid = _shv_check_license(lic.get('activation_code',''))")
        L("            if ok: return _shv_original_build(tier)")
        L("        self._gate = SHVActivationGate(self._on_activated)")
        L("        return self._gate.get_widget()")
        L("    def _on_activated(self, tier):")
        L("        self.root.clear_widgets()")
        L("        self.root.add_widget(_shv_original_build(tier))")
        L()
        L("if __name__ == '__main__': ${appClass}().run()")

        return lines.joinToString("\n")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun LicenseToolsScreen(
    onNavigateBack: () -> Unit,
    vm: LicenseToolsViewModel = viewModel()
) {
    val clipboard  = LocalClipboardManager.current
    val toast      by vm.toast.collectAsState()
    val loading    by vm.loading.collectAsState()
    val appColors   = LocalAppColors.current
    val products    = remember { vm.allProducts() }

    LaunchedEffect(toast) { if (toast.isNotEmpty()) vm.consumeToast() }

    Column(modifier = Modifier.fillMaxSize().background(appColors.bg)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("License Tools", PinkCol, 20)
            BodyText("Generate licensed app templates and verify activation codes.", SubText)

            if (toast.isNotEmpty()) AppCard { BodyText(toast, TealCol) }

            // ── SECTION 1: Template Generator ─────────────────────────────────
            AppCard {
                SectionLabel("Generate App Template", GreenCol)
                BodyText("Creates a ready-to-run KivyMD Python app with the SHV license gate pre-installed.", SubText)

                if (products.isEmpty()) {
                    BodyText("No products available. Add one in Legacy or New License Manager first.", SubText)
                } else {
                    // Product selector
                    BodyText("Select Product:", SubText)
                    var prodExpanded by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { prodExpanded = true }, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (vm.selectedProduct != null) {
                                    runCatching { Color(android.graphics.Color.parseColor(vm.selectedProduct!!.color)) }
                                        .getOrDefault(CardBg2)
                                } else CardBg2
                            )) {
                            Text(vm.selectedProduct?.displayName ?: "-- Choose a product --",
                                color = Color.White)
                        }
                        DropdownMenu(expanded = prodExpanded, onDismissRequest = { prodExpanded = false },
                            containerColor = CardBg) {
                            // Group: Legacy
                            val legacyProds = products.filter { it.isLegacy }
                            val newProds    = products.filter { !it.isLegacy }
                            if (legacyProds.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("── Legacy Products ──", color = SubText, fontSize = 11.sp) },
                                    onClick = {}, enabled = false
                                )
                                legacyProds.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.displayName, color = Color.White) },
                                        onClick = { vm.selectProduct(p); prodExpanded = false }
                                    )
                                }
                            }
                            if (newProds.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("── New Products ──", color = SubText, fontSize = 11.sp) },
                                    onClick = {}, enabled = false
                                )
                                newProds.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.displayName, color = Color.White) },
                                        onClick = { vm.selectProduct(p); prodExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // Show public key preview if product selected
                    vm.selectedProduct?.let { prod ->
                        val pubKey = remember(prod.id) { prod.publicKeyPem() }
                        if (pubKey.isNotBlank()) {
                            BodyText("Public Key: ✓ Loaded (${prod.activationPrefix}-)", GreenCol)
                        } else {
                            BodyText("⚠ No authority keys for this product.", OrangeCol)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    BodyText("App Class Name", SubText)
                    AppTextField(vm.tplAppClass, { vm.tplAppClass = it }, "e.g. MyNewApp")
                    BodyText("Output Filename (optional — leave blank to auto-generate)", SubText)
                    AppTextField(vm.tplOutputName, { vm.tplOutputName = it }, "e.g. my_app.py")
                    BodyText("GitHub Revocation URL (auto-filled from product)", SubText)
                    AppTextField(vm.tplRevoUrl, { vm.tplRevoUrl = it }, "https://raw.githubusercontent.com/...")

                    // Cloud preset loader for revo URL
                    val presets = remember { CloudPresetsStore.loadAll().filter { it.type == "github_path" } }
                    if (presets.isNotEmpty()) {
                        var presetExpanded by remember { mutableStateOf(false) }
                        Box {
                            ActionButton("Load Revo URL from Cloud Preset", CyanCol) { presetExpanded = true }
                            DropdownMenu(expanded = presetExpanded, onDismissRequest = { presetExpanded = false },
                                containerColor = CardBg) {
                                presets.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name, color = Color.White) },
                                        onClick = {
                                            vm.tplRevoUrl = "https://raw.githubusercontent.com/${p.owner}/${p.repo}/${p.branch.ifBlank{"main"}}/${p.path}"
                                            presetExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    BodyText("Demo Hours (default 24)", SubText)
                    AppTextField(vm.tplDemoHours, { vm.tplDemoHours = it }, "e.g. 24, 48, 168")

                    if (loading) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GreenCol)
                        }
                    } else {
                        ActionButton("Generate Template File", GreenCol) { vm.generateTemplate() }
                    }

                    if (vm.tplResult.isNotEmpty()) {
                        BodyText(vm.tplResult, if (vm.tplResult.startsWith("✓")) GreenCol else RedCol)
                    }
                }
            }

            // ── SECTION 2: License Verifier ───────────────────────────────────
            AppCard {
                SectionLabel("License Verifier", BlueCol)
                BodyText("Paste any activation code to verify its signature against your products.", SubText)

                AppTextField(vm.verifyCode, { vm.verifyCode = it }, "Paste activation code here")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            vm.verifyCode = clipboard.getText()?.text ?: ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CardBg2)
                    ) { Text("Paste", color = TealCol) }
                    Button(
                        onClick = { vm.verifyCode = "" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CardBg2)
                    ) { Text("Clear", color = RedCol) }
                }

                ActionButton("Verify License", BlueCol) { vm.verifyLicense(products) }

                if (vm.verifyResult.isNotEmpty()) {
                    val resultColor = when (vm.verifyIsOk) {
                        true  -> GreenCol
                        false -> RedCol
                        null  -> SubText
                    }
                    AppCard(color = CardBg2) {
                        Text(vm.verifyResult, color = resultColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                        vm.verifyPayload?.let { p ->
                            Spacer(Modifier.height(8.dp))
                            SectionLabel("Payload", SubText)
                            listOf(
                                "License ID" to p["license_id"],
                                "App" to p["app"],
                                "Tier" to p["tier"],
                                "Source" to p["source"],
                                "Device Code" to p["device_code"],
                                "Customer" to p["customer_name"],
                                "Issued" to p["issued_at"]?.toString()?.take(10),
                                "Expiry" to p["expires_at"],
                                "Schema" to p["schema"]
                            ).forEach { (k, v) ->
                                if (!v?.toString().isNullOrBlank()) {
                                    BodyText("$k: $v", SubText)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { clipboard.setText(AnnotatedString(AppStorage.gson.toJson(p))) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = TealCol)
                            ) { Text("Copy Full Payload JSON", color = Color.White) }
                        }
                    }
                }
            }
        }
        BottomNavBar(listOf("BACK" to onNavigateBack, "HOME" to onNavigateBack))
    }
}
