package com.example.slacklineadminapp.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.toSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.platform.LocalContext
import com.example.slacklineadminapp.data.*
import com.example.slacklineadminapp.ui.theme.*
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.asComposeRenderEffect
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText

// ── Data classes ──────────────────────────────────────────────────────────────

private data class DashModule(
    val label: String,
    val color: Color,
    val route: String,
    val icon: ImageVector
)

private data class DashSection(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val modules: List<DashModule>
)

// A section's "real" modules (excludes placeholder slots) plus whether it has
// any placeholders at all — used to collapse 2-3 dead placeholder tiles into
// a single "+ Add Module" tile per section instead of showing them all.
private fun DashSection.realModules(): List<DashModule> =
    modules.filterNot { it.label.contains("PLACEHOLDER", ignoreCase = true) }

private fun DashSection.hasPlaceholders(): Boolean =
    modules.any { it.label.contains("PLACEHOLDER", ignoreCase = true) }

// ── All modules grouped by section ───────────────────────────────────────────

private val SECTION_PRODUCTS = DashSection(
    title   = "PRODUCTS",
    icon    = Icons.Default.Inventory2,
    color   = CyanCol,
    modules = listOf(
        DashModule("KOTLIN APPS\nMANAGER",        TealCol,   "kotlin_apps_manager", Icons.Default.Apps),
        DashModule("LEGACY LICENSE\nMANAGER",     PurpleCol, "legacy",              Icons.Default.AdminPanelSettings),
        DashModule("NEW LICENSE\nMANAGER",        CyanCol,   "new_license",         Icons.Default.LibraryAdd),
        DashModule("CUSTOMERS\nDIRECTORY",        GreenCol,  "customers",           Icons.Default.Groups),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_prod_1",           Icons.Default.Add),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_prod_2",           Icons.Default.Add)
    )
)

private val SECTION_LICENSING_TOOLS = DashSection(
    title   = "LICENSING TOOLS",
    icon    = Icons.Default.Science,
    color   = RedCol,
    modules = listOf(
        DashModule("KOTLIN APP\nINJECTOR",        RedCol,    "kotlin_app_injector", Icons.Default.Science),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_lict_1",           Icons.Default.Add),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_lict_2",           Icons.Default.Add)
    )
)

private val SECTION_SHV_STORE = DashSection(
    title   = "SHV STORE",
    icon    = Icons.Default.Storefront,
    color   = CyanCol,
    modules = listOf(
        DashModule("SHV STORE\nADMIN",            CyanCol,   "shv_store_admin",     Icons.Default.Storefront),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_store_1",          Icons.Default.Add),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_store_2",          Icons.Default.Add)
    )
)

private val SECTION_TOOLS = DashSection(
    title   = "TOOLS",
    icon    = Icons.Default.Construction,
    color   = GreenCol,
    modules = listOf(
        DashModule("KOTLIN APP\nGENERATOR",       GreenCol,  "kotlin_app_generator", Icons.Default.AutoAwesome),
        DashModule("PYTHON-KIVY\nLICENSE TOOLS",  PinkCol,   "license_tools",        Icons.Default.Construction),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_tools_1",           Icons.Default.Add)
    )
)

private val SECTION_CLOUD = DashSection(
    title   = "CLOUD SERVICES",
    icon    = Icons.Default.Cloud,
    color   = PurpleCol,
    modules = listOf(
        DashModule("GITHUB\nMANAGER",             PurpleCol, "github",    Icons.Default.Hub),
        DashModule("SUPA\nSTUDIO",                CyanCol,   "supabase",  Icons.Default.Storage),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_cld_1",  Icons.Default.Add)
    )
)

private val SECTION_TIME = DashSection(
    title   = "TIME MANAGEMNT",
    icon    = Icons.Default.DateRange,
    color   = BlueCol,
    modules = listOf(
        DashModule("BUSINESS\nCALENDAR",          GreenCol,  "business_calendar",  Icons.Default.DateRange),
        DashModule("PROJECT\nMANAGEMENT",         BlueCol,   "project_management", Icons.Default.Assignment),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_time_1",          Icons.Default.Add)
    )
)

private val SECTION_CLIENT_SERVICES = DashSection(
    title   = "CLIENT SERVICES",
    icon    = Icons.Default.SupportAgent,
    color   = CyanCol,
    modules = listOf(
        DashModule("CLIENT SERVICE\nREQUESTS",    CyanCol,   "client_service_requests", Icons.Default.SupportAgent),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_client_1",              Icons.Default.Add),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_client_2",              Icons.Default.Add)
    )
)

private val SECTION_ACCOUNTING = DashSection(
    title   = "ACCOUNTING",
    icon    = Icons.Default.Receipt,
    color   = AmberCol,
    modules = listOf(
        DashModule("INVOICE\nMAKER",              AmberCol,  "invoice_maker", Icons.Default.Receipt),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_acc_1",      Icons.Default.Add),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_acc_2",      Icons.Default.Add)
    )
)

private val SECTION_WEBSITE_TOOLS = DashSection(
    title   = "WEBSITE TOOLS",
    icon    = Icons.Default.Web,
    color   = TealCol,
    modules = listOf(
        DashModule("DOMAIN\nMANAGEMENT",          BlueCol,   "domain_management", Icons.Default.Language),
        DashModule("COMPANY\nWEBSITE ADMIN",      TealCol,   "website_admin", Icons.Default.Web),
        DashModule("WEBSITES\nREGISTRY",          BlueCol,   "websites",      Icons.Default.TravelExplore),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_web_1",      Icons.Default.Add)
    )
)

private val SECTION_APP_BLUEPRINTS = DashSection(
    title   = "APP BLUEPRINTS",
    icon    = Icons.Default.Code,
    color   = GreenCol,
    modules = listOf(
        DashModule("KOTLIN APP\nBLUEPRINTS",       GreenCol,  "kotlin_app_blueprints", Icons.Default.Code),
        DashModule("PYTHON-KIVY\nAPP BLUEPRINTS",  GreenCol,  "blueprints",            Icons.Default.DashboardCustomize),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",   SubText,   "ph_bp_1",               Icons.Default.Add)
    )
)

private val SECTION_NOTES = DashSection(
    title   = "NOTES",
    icon    = Icons.Default.MenuBook,
    color   = OrangeCol,
    modules = listOf(
        DashModule("DOCUMENTS\n& GUIDES",         OrangeCol, "documents",  Icons.Default.MenuBook),
        DashModule("WORKFLOW\nGUIDES",            GreenCol,  "workflow",   Icons.Default.AccountTree),
        DashModule("PLACEHOLDER\n(NEW FEATURE)",  SubText,   "ph_notes_1", Icons.Default.Add)
    )
)

private val SECTION_LEGACY = DashSection(
    title   = "LEGACY",
    icon    = Icons.Default.Inventory,
    color   = SubText,
    modules = listOf(
        DashModule("KOTLIN LICENSE\nTOOLS\n~Discontinued~", TealCol, "kotlin_tool", Icons.Default.Key),
        DashModule("PLACEHOLDER\n(FUTURE LEGACY)",          SubText, "ph_leg_1",    Icons.Default.Add),
        DashModule("PLACEHOLDER\n(FUTURE LEGACY)",          SubText, "ph_leg_2",    Icons.Default.Add)
    )
)

private val ALL_SECTIONS = listOf(
    SECTION_PRODUCTS,
    SECTION_LICENSING_TOOLS,
    SECTION_SHV_STORE,
    SECTION_TOOLS,
    SECTION_CLOUD,
    SECTION_TIME,
    SECTION_CLIENT_SERVICES,
    SECTION_ACCOUNTING,
    SECTION_WEBSITE_TOOLS,
    SECTION_APP_BLUEPRINTS,
    SECTION_NOTES,
    SECTION_LEGACY
)

// ── Settings modules ──────────────────────────────────────────────────────────

private val SETTINGS_MODULES = listOf(
    DashModule("ACTIVITY\nLOG",    OrangeCol, "activity_log",   Icons.Default.BarChart),
    DashModule("CLOUD\nSETTINGS",  TealCol,   "cloud_settings", Icons.Default.CloudSync),
    DashModule("SETTINGS",         OrangeCol, "settings",       Icons.Default.Tune)
)

// ── Routes ────────────────────────────────────────────────────────────────────

private val PHASE1_ROUTES = setOf(
    "legacy", "license_tools", "new_license", "cloud_settings", "settings",
    "github", "supabase", "activity_log", "customers", "workflow",
    "websites", "documents", "blueprints", "kotlin_tool", "invoice_maker",
    "website_admin", "domain_management", "kotlin_apps_manager", "kotlin_app_injector", "kotlin_app_generator",
    "project_management", "business_calendar",
    "shv_store_admin", "client_service_requests", "kotlin_app_blueprints"
)

// ── Matrix rain ───────────────────────────────────────────────────────────────

private val MATRIX_CHARS = "ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ0123456789ABCDEF"

private data class MatrixColumn(
    val x: Float,
    var headY: Float,
    var speed: Float,
    var length: Int,
    var chars: List<Char>,
    var opacity: Float
)

private fun randomChar() = MATRIX_CHARS[Random.nextInt(MATRIX_CHARS.length)]

@Composable
private fun MatrixRainBackground(modifier: Modifier = Modifier) {
    val charSize    = 14f
    val columnCount = 28

    val columns = remember {
        (0 until columnCount).map { i ->
            MatrixColumn(
                x       = i * (charSize + 2f),
                headY   = Random.nextFloat() * -800f,
                speed   = 3f + Random.nextFloat() * 5f,
                length  = 8 + Random.nextInt(16),
                chars   = (0..20).map { randomChar() },
                opacity = 0.35f + Random.nextFloat() * 0.35f
            )
        }.toMutableList()
    }

    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(40L)
            frame++
        }
    }

    Canvas(modifier = modifier) {
        val canvasHeight = size.height
        columns.forEachIndexed { idx, col ->
            if (frame > 0) {
                columns[idx] = col.copy(
                    headY = if (col.headY - col.length * charSize > canvasHeight) {
                        -col.length * charSize - Random.nextFloat() * canvasHeight * 0.5f
                    } else col.headY + col.speed,
                    chars = if (Random.nextInt(8) == 0)
                        col.chars.toMutableList().also { it[Random.nextInt(it.size)] = randomChar() }
                    else col.chars
                )
            }
            drawMatrixColumn(columns[idx], charSize)
        }
    }
}

private fun DrawScope.drawMatrixColumn(col: MatrixColumn, charSize: Float) {
    val paint = Paint().asFrameworkPaint()
    paint.isAntiAlias = true
    paint.textSize    = charSize
    val totalChars    = col.length
    for (i in 0 until totalChars) {
        val y = col.headY - i * charSize
        if (y < -charSize || y > size.height + charSize) continue
        val char      = col.chars[i % col.chars.size].toString()
        val trailFade = (1f - i.toFloat() / totalChars)
        val alpha     = (col.opacity * trailFade).coerceIn(0f, 1f)
        val color     = if (i == 0)
            Color(0xFF9FFFC8).copy(alpha = (col.opacity * 2.2f).coerceIn(0f, 0.95f))
        else
            TealCol.copy(alpha = (alpha * 1.4f).coerceIn(0f, 0.85f))
        drawIntoCanvas { canvas ->
            paint.color = color.toArgb()
            canvas.nativeCanvas.drawText(char, col.x, y, paint)
        }
    }
}

// ── Blur scrim overlay ────────────────────────────────────────────────────────

@Composable
private fun BlurScrimOverlay(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    renderEffect = android.graphics.RenderEffect
                        .createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
                .background(Color.Black.copy(alpha = 0.35f))
        )
    } else {
        Box(modifier = modifier.background(Color.Black.copy(alpha = 0.72f)))
    }
}

// ── Category popup ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryPopup(
    section: DashSection,
    pendingRequestsCount: Int,
    pinnedModules: List<DashModule>,
    onPinToggle: (DashModule) -> Unit,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // Collapse 2-3 dead "PLACEHOLDER" tiles into a single "+ Add Module" tile
    // per section — same functionality, far less visual noise.
    val realModules = remember(section) { section.realModules() }
    val showAddTile  = remember(section) { section.hasPlaceholders() }
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            BlurScrimOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss)
            )

            Card(
                modifier  = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Popup header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(section.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = section.icon,
                                contentDescription = null,
                                tint               = section.color,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            section.title,
                            color         = section.color,
                            fontSize      = 15.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }

                    HorizontalDivider(color = section.color.copy(alpha = 0.2f), thickness = 1.dp)

                    // Module grid — 3 columns. Real modules first, then a single
                    // "+ Add Module" tile standing in for whatever placeholder
                    // slots this section used to show individually.
                    val addModuleTile = DashModule(
                        "+ ADD\nMODULE", SubText, "__add_module__", Icons.Default.Add
                    )
                    val gridModules = if (showAddTile) realModules + addModuleTile else realModules
                    val rows = gridModules.chunked(3)
                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { mod ->
                                val isAddTile  = mod.route == "__add_module__"
                                val enabled    = mod.route in PHASE1_ROUTES
                                val badge      = if (mod.route == "client_service_requests") pendingRequestsCount else 0
                                val isDiscon   = mod.label.contains("Discontinued", ignoreCase = true)
                                val isPlaceholder = isAddTile
                                val tileColor  = when {
                                    isPlaceholder -> mod.color.copy(alpha = 0.18f)
                                    !enabled      -> mod.color.copy(alpha = 0.25f)
                                    else          -> mod.color
                                }

                                var pressed by remember { mutableStateOf(false) }
                                val scale by animateFloatAsState(
                                    targetValue   = if (pressed) 0.93f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness    = Spring.StiffnessHigh
                                    ),
                                    label = "popupTileScale_${mod.route}"
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    val isPinned   = pinnedModules.any { it.route == mod.route }
                                    val canPin     = pinnedModules.size < 9 || isPinned
                                    val isPlaceholder2 = isPlaceholder // local alias for lambda capture
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(82.dp)
                                            .scale(scale)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF111111))
                                            .border(
                                                width = 2.dp,
                                                color = tileColor.copy(alpha = if (isPlaceholder) 0.12f else if (isPinned) 1f else 0.55f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .then(
                                                if (enabled && !isPlaceholder2)
                                                    Modifier.combinedClickable(
                                                        onClick     = {
                                                            pressed = false
                                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            onDismiss()
                                                            onNavigate(mod.route)
                                                        },
                                                        onLongClick = {
                                                            if (canPin) onPinToggle(mod)
                                                        }
                                                    )
                                                else Modifier
                                            )
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector        = mod.icon,
                                            contentDescription = null,
                                            tint               = tileColor,
                                            modifier           = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.height(5.dp))
                                        Text(
                                            text           = mod.label,
                                            color          = if (isPlaceholder || !enabled)
                                                Color.White.copy(alpha = 0.22f)
                                            else Color.White.copy(alpha = 0.9f),
                                            fontSize       = 8.sp,
                                            fontWeight     = FontWeight.Bold,
                                            lineHeight     = 10.sp,
                                            textAlign      = TextAlign.Center,
                                            maxLines       = 3,
                                            textDecoration = if (isDiscon) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                    }

                                    // Pin indicator — top-start corner
                                    if (isPinned) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .offset(x = 4.dp, y = 4.dp)
                                                .size(14.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(tileColor.copy(alpha = 0.85f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector        = Icons.Default.PushPin,
                                                contentDescription = "Pinned",
                                                tint               = Color.Black,
                                                modifier           = Modifier.size(9.dp)
                                            )
                                        }
                                    }

                                    // Badge
                                    if (badge > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-4).dp)
                                                .background(RedCol, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text       = badge.toString(),
                                                color      = Color.White,
                                                fontSize   = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                            // Fill empty cells in last row
                            repeat(3 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    Text(
                        "Tap outside to close  •  Long-press to pin / unpin",
                        color     = SubText.copy(alpha = 0.4f),
                        fontSize  = 10.sp,
                        modifier  = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Exit dialog ───────────────────────────────────────────────────────────────

@Composable
private fun BlurredExitDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BlurScrimOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss)
            )
            Card(
                modifier  = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector        = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint               = RedCol,
                        modifier           = Modifier.size(32.dp)
                    )
                    Text(
                        "Exit App",
                        color      = Color.White,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Are you sure you want to exit?",
                        color     = SubText,
                        fontSize  = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick  = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = SubText)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick  = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = RedCol)
                        ) {
                            Text("Exit", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Pinned modules DataStore ──────────────────────────────────────────────────

private val Context.pinnedDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "slackline_pinned"
)
private val PINNED_KEY = stringPreferencesKey("pinned_routes")

// ── Category order DataStore (drag-to-reorder persistence) ───────────────────

private val Context.sectionOrderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "slackline_section_order"
)
private val SECTION_ORDER_KEY = stringPreferencesKey("section_order")

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    companyName: String,
    onNavigate: (String) -> Unit,
    onExit: () -> Unit
) {
    var pendingRequestsCount by remember { mutableStateOf(0) }

    // Loading flags — used to show a skeleton shimmer on the stat cards
    // instead of letting the counts flash 0 → real value while the
    // Supabase calls below are still in flight.
    var pendingLoading by remember { mutableStateOf(true) }
    var klLoading      by remember { mutableStateOf(true) }

    // Fetch pending count — unchanged
    LaunchedEffect(Unit) {
        try {
            val response = HttpClient(Android).get(
                "https://ovdxetyadfsxehwnbyuz.supabase.co/rest/v1/client_requests?status=eq.pending&select=id"
            ) {
                headers {
                    append("apikey",        "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz")
                    append("Authorization", "Bearer sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz")
                }
            }
            val dataText = response.bodyAsText()
            pendingRequestsCount = "\"id\"".toRegex().findAll(dataText).count()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pendingLoading = false
        }
    }

    // Refresh trigger: bumps every time the Dashboard re-enters composition
    // (e.g. navigating back from Kotlin Apps Manager / Legacy License Manager),
    // forcing the stats below to recompute instead of using stale cached
    // values from the first time the Dashboard was ever composed.
    var statsRefreshTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { statsRefreshTick++ }

    val legacyProducts = remember(statsRefreshTick) { ProductRegistry.all() }
    val newProducts    = remember(statsRefreshTick) { NewLicenseStore.allProducts() }

    // ── Kotlin Apps Manager counts ──────────────────────────────────────
    // The Kotlin Apps Manager module (kl_licenses table) is the source of
    // truth in Supabase, not the local isRevoked flag, so we pull the
    // status counts straight from the cloud here.
    var klTotal        by remember { mutableStateOf(0) }
    var klActive       by remember { mutableStateOf(0) }
    var klRevoked      by remember { mutableStateOf(0) }
    var klProductCount by remember { mutableStateOf(0) }

    LaunchedEffect(statsRefreshTick) {
        try {
            val response = HttpClient(Android).get(
                "https://ovdxetyadfsxehwnbyuz.supabase.co/rest/v1/kl_licenses?select=status"
            ) {
                headers {
                    append("apikey",        "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz")
                    append("Authorization", "Bearer sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz")
                }
            }
            val arr = org.json.JSONArray(response.bodyAsText())
            var active = 0
            var revoked = 0
            for (i in 0 until arr.length()) {
                when (arr.getJSONObject(i).optString("status")) {
                    "revoked" -> revoked++
                    else      -> active++   // treat anything not explicitly "revoked" as active
                }
            }
            klTotal   = arr.length()
            klActive  = active
            klRevoked = revoked
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            klLoading = false
        }
    }

    // ── Kotlin Apps Manager product count ───────────────────────────────
    // Products for this module live in Supabase (kl_products), not locally,
    // so they need their own fetch to feed the "Products" stat box.
    LaunchedEffect(statsRefreshTick) {
        try {
            val response = HttpClient(Android).get(
                "https://ovdxetyadfsxehwnbyuz.supabase.co/rest/v1/kl_products?select=id"
            ) {
                headers {
                    append("apikey",        "sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz")
                    append("Authorization", "Bearer sb_publishable_3J-H60daCgWdhSvpdXi0zw_QpPax3Dz")
                }
            }
            val arr = org.json.JSONArray(response.bodyAsText())
            klProductCount = arr.length()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // True until both Supabase calls that feed the stat row have settled.
    val statsLoading = pendingLoading || klLoading

    val totalProducts = legacyProducts.size + newProducts.size + klProductCount
    val totalLicenses = remember(statsRefreshTick, legacyProducts, newProducts, klTotal) {
        legacyProducts.sumOf { EngineCache.get(it).loadRecords().size } +
        newProducts.sumOf   { NewLicenseStore.loadLicenses(it.id).size } +
        klTotal
    }
    val totalActive = remember(statsRefreshTick, legacyProducts, newProducts, klActive) {
        legacyProducts.sumOf { EngineCache.get(it).loadRecords().count { r -> r.status == "active" } } +
        newProducts.sumOf   { NewLicenseStore.loadLicenses(it.id).count { r -> r.status == "active" } } +
        klActive
    }
    val totalRevoked = remember(statsRefreshTick, legacyProducts, newProducts, klRevoked) {
        legacyProducts.sumOf { EngineCache.get(it).loadRecords().count { r -> r.status == "revoked" } } +
        newProducts.sumOf   { NewLicenseStore.loadLicenses(it.id).count { r -> r.status == "revoked" } } +
        klRevoked
    }

    val appColors                                = LocalAppColors.current
    var showExitDialog                           by remember { mutableStateOf(false) }
    var openSection: DashSection?                by remember { mutableStateOf(null) }
    var settingsExpanded                         by remember { mutableStateOf(false) }

    // Pinned modules — persisted via DataStore
    val context      = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics      = LocalHapticFeedback.current

    // Background matrix animation toggle — persisted via SecurityConfig DataStore
    val securityCfg by SecurityConfig.getFlow(context)
        .collectAsStateWithLifecycle(initialValue = SecurityConfig.Cfg())

    val allModules   = remember { ALL_SECTIONS.flatMap { it.modules } }
    val pinnedRoutes by context.pinnedDataStore.data
        .map { prefs ->
            prefs[PINNED_KEY]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val pinnedModules = remember(pinnedRoutes) {
        pinnedRoutes.mapNotNull { route -> allModules.find { it.route == route } }
    }

    fun togglePin(mod: DashModule) {
        // Confirming vibration — this is a "hidden" long-press gesture, so the
        // haptic is the main signal the user gets that it actually registered.
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope.launch {
            context.pinnedDataStore.edit { prefs ->
                val current = prefs[PINNED_KEY]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.toMutableList()
                    ?: mutableListOf()
                if (current.contains(mod.route)) {
                    current.remove(mod.route)
                } else if (current.size < 9) {
                    current.add(mod.route)
                }
                prefs[PINNED_KEY] = current.joinToString(",")
            }
        }
    }

    // ── Section badge counts ─────────────────────────────────────────────
    // Small at-a-glance chips shown on the category buttons themselves —
    // simply how many real modules live inside each category.
    val sectionModuleCounts = remember {
        ALL_SECTIONS.associate { it.title to it.realModules().size }
    }

    // Sections with an urgent hidden count (e.g. pending client requests)
    // don't show that count as the category badge — the badge stays a plain
    // module count like every other category — instead the whole button
    // pulses to draw the eye. The actual number still shows on the module
    // tile itself inside the popup, unchanged.
    val sectionPulseFlags = remember(pendingRequestsCount) {
        mapOf(SECTION_CLIENT_SERVICES.title to (pendingRequestsCount > 0))
    }

    // ── Category reorder (drag to reorder) ───────────────────────────────
    var reorderMode by remember { mutableStateOf(false) }
    var draggingSectionTitle by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    val savedOrder by context.sectionOrderDataStore.data
        .map { prefs ->
            prefs[SECTION_ORDER_KEY]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Local, mutable working copy of the section order so drags feel instant;
    // re-derived whenever the persisted order changes underneath us (e.g. on
    // first load), but not on every recomposition after that.
    var orderedSections by remember { mutableStateOf(ALL_SECTIONS) }
    LaunchedEffect(savedOrder) {
        if (savedOrder.isNotEmpty()) {
            val byTitle = ALL_SECTIONS.associateBy { it.title }
            val restored = savedOrder.mapNotNull { byTitle[it] }.toMutableList()
            // Append any sections that weren't in the saved order yet
            // (e.g. new categories added in an app update).
            ALL_SECTIONS.forEach { if (it !in restored) restored.add(it) }
            orderedSections = restored
        }
    }

    fun persistSectionOrder(order: List<DashSection>) {
        coroutineScope.launch {
            context.sectionOrderDataStore.edit { prefs ->
                prefs[SECTION_ORDER_KEY] = order.joinToString(",") { it.title }
            }
        }
    }

    fun moveSection(from: Int, to: Int) {
        if (from == to || from !in orderedSections.indices || to !in orderedSections.indices) return
        val mutable = orderedSections.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        orderedSections = mutable
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        persistSectionOrder(mutable)
    }

    BackHandler { showExitDialog = true }

    // Dialogs
    if (showExitDialog) {
        BlurredExitDialog(
            onConfirm = { onExit() },
            onDismiss = { showExitDialog = false }
        )
    }

    openSection?.let { sec ->
        CategoryPopup(
            section              = sec,
            pendingRequestsCount = pendingRequestsCount,
            pinnedModules        = pinnedModules,
            onPinToggle          = { mod -> togglePin(mod) },
            onNavigate           = onNavigate,
            onDismiss            = { openSection = null }
        )
    }

    // ── Animations ────────────────────────────────────────────────────────────

    // Skeleton shimmer for the stat cards while Supabase calls are in flight
    val skeletonAnim = rememberInfiniteTransition(label = "skeleton")
    val skeletonAlpha by skeletonAnim.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.55f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 750, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    // Category button pulse — highlights a section with an urgent hidden
    // count (e.g. pending client requests) without putting a jarring red
    // number front-and-center on the category grid.
    val categoryPulseAnim = rememberInfiniteTransition(label = "categoryPulse")
    val categoryPulseAlpha by categoryPulseAnim.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "categoryPulseAlpha"
    )

    // Title shimmer sweep
    val shimmerAnim   = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerAnim.animateFloat(
        initialValue  = -1f,
        targetValue   = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // Title pulse
    val titlePulse = rememberInfiniteTransition(label = "titlePulse")
    val titleAlpha by titlePulse.animateFloat(
        initialValue  = 0.85f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleAlpha"
    )

    // Stats animated count-up + fade-in
    val statData = listOf(
        Triple(totalProducts, "Products", TealCol),
        Triple(totalLicenses, "Licenses", BlueCol),
        Triple(totalActive,   "Active",   GreenCol),
        Triple(totalRevoked,  "Revoked",  RedCol)
    )
    val cardVisible = statData.indices.map { idx ->
        val anim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(idx * 120L)
            anim.animateTo(1f, animationSpec = tween(400, easing = EaseOutCubic))
        }
        anim
    }
    val displayValues = statData.mapIndexed { idx, (target, _, _) ->
        val anim = remember { Animatable(0f) }
        LaunchedEffect(target) {
            kotlinx.coroutines.delay(idx * 120L + 80L)
            anim.animateTo(target.toFloat(), animationSpec = tween(750, easing = EaseOutCubic))
        }
        anim.value.toInt()
    }

    // Category buttons stagger in
    val btnVisible = ALL_SECTIONS.indices.map { idx ->
        val anim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(200L + idx * 80L)
            anim.animateTo(1f, animationSpec = tween(350, easing = EaseOutCubic))
        }
        anim
    }

    // Footer pulse
    val footerPulse = rememberInfiniteTransition(label = "footerPulse")
    val footerAlpha by footerPulse.animateFloat(
        initialValue  = 0.88f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "footerAlpha"
    )

    // ── Layout ────────────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bg)
    ) {
        if (securityCfg.matrixAnimationEnabled) {
            MatrixRainBackground(modifier = Modifier.fillMaxSize())
        }

        // Dim scrim over matrix so content reads clearly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            appColors.bg.copy(alpha = 0.45f),
                            appColors.bg.copy(alpha = 0.35f),
                            appColors.bg.copy(alpha = 0.45f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── ZONE 1: Hero ─────────────────────────────────────────────────

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = appColors.card.copy(alpha = 0.92f)),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title + shimmer
                    Box(
                        modifier         = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier            = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    companyName,
                                    color         = TealCol.copy(alpha = titleAlpha),
                                    fontWeight    = FontWeight.ExtraBold,
                                    fontSize      = 30.sp,
                                    textAlign     = TextAlign.Center,
                                    modifier      = Modifier.fillMaxWidth(),
                                    letterSpacing = 1.sp
                                )
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    val sweepWidth = size.width * 0.35f
                                    val x = shimmerOffset * (size.width + sweepWidth) - sweepWidth
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.10f),
                                                Color.White.copy(alpha = 0.18f),
                                                Color.White.copy(alpha = 0.10f),
                                                Color.Transparent
                                            ),
                                            startX = x,
                                            endX   = x + sweepWidth
                                        )
                                    )
                                }
                            }
                            Text(
                                "Dynamic Product Licensing  •  SH Vertex Technologies",
                                color     = SubText,
                                fontSize  = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    HorizontalDivider(color = TealCol.copy(alpha = 0.2f), thickness = 1.dp)

                    // Stats 1x4 row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        statData.forEachIndexed { globalIdx, (_, label, color) ->
                            val alpha = cardVisible[globalIdx].value
                            val value = displayValues[globalIdx]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .graphicsLayer {
                                        this.alpha        = alpha
                                        this.translationY = (1f - alpha) * 24f
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color.copy(alpha = 0.10f))
                            ) {
                                // Solid left accent bar
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(color)
                                )
                                Column(
                                    modifier            = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (statsLoading) {
                                        // Skeleton bars — signals "still loading" intentionally,
                                        // instead of showing a 0 that then jumps to the real count.
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(18.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(color.copy(alpha = skeletonAlpha))
                                        )
                                        Spacer(Modifier.height(5.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(40.dp)
                                                .height(9.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(color.copy(alpha = skeletonAlpha * 0.6f))
                                        )
                                    } else {
                                        Text(
                                            text       = value.toString(),
                                            color      = color,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize   = 23.sp,
                                            lineHeight = 23.sp,
                                            textAlign  = TextAlign.Center,
                                            style      = LocalTextStyle.current.copy(
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                lineHeightStyle = LineHeightStyle(
                                                    alignment = LineHeightStyle.Alignment.Center,
                                                    trim      = LineHeightStyle.Trim.Both
                                                )
                                            )
                                        )
                                        Spacer(Modifier.height(1.dp))
                                        Text(
                                            text       = label,
                                            color      = color.copy(alpha = 0.75f),
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign  = TextAlign.Center,
                                            style      = LocalTextStyle.current.copy(
                                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                lineHeightStyle = LineHeightStyle(
                                                    alignment = LineHeightStyle.Alignment.Center,
                                                    trim      = LineHeightStyle.Trim.Both
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Pinned Modules ────────────────────────────────────────
                    if (pinnedModules.isNotEmpty()) {
                        HorizontalDivider(color = TealCol.copy(alpha = 0.15f), thickness = 1.dp)
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier              = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector        = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint               = TealCol,
                                    modifier           = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text          = "PINNED",
                                    color         = TealCol,
                                    fontSize      = 10.sp,
                                    fontWeight    = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text       = "${pinnedModules.size}/9",
                                color      = TealCol.copy(alpha = 0.6f),
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        // Up to 9 pinned chips in three rows of 3
                        val pinnedRows = pinnedModules.toList().chunked(3)
                        pinnedRows.forEach { rowMods ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier              = Modifier.fillMaxWidth()
                            ) {
                                rowMods.forEach { mod ->
                                    var pressed by remember { mutableStateOf(false) }
                                    val chipScale by animateFloatAsState(
                                        targetValue   = if (pressed) 0.93f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness    = Spring.StiffnessHigh
                                        ),
                                        label = "pinnedChipScale_${mod.route}"
                                    )
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier              = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .scale(chipScale)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(mod.color.copy(alpha = 0.12f))
                                            .border(
                                                width = 1.dp,
                                                color = mod.color.copy(alpha = 0.45f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                pressed = false
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onNavigate(mod.route)
                                            }
                                            .padding(horizontal = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector        = mod.icon,
                                            contentDescription = null,
                                            tint               = mod.color,
                                            modifier           = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text       = mod.label.replace("\n", " "),
                                            color      = Color.White.copy(alpha = 0.85f),
                                            fontSize   = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines   = 2,
                                            lineHeight = 9.sp
                                        )
                                    }
                                }
                                // Fill empty slots in last row
                                repeat(3 - rowMods.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // ── ZONE 2: Category buttons — 2 column grid ───────────────────────

            // Header row: label + reorder toggle. Usage patterns settle after
            // a few weeks of real use, so let the person drag categories into
            // whatever order actually matches how they use the app.
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    text          = if (reorderMode) "DRAG TO REORDER" else "CATEGORIES",
                    color         = if (reorderMode) OrangeCol else SubText.copy(alpha = 0.6f),
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = { reorderMode = !reorderMode }) {
                    Icon(
                        imageVector        = if (reorderMode) Icons.Default.Check else Icons.Default.DragIndicator,
                        contentDescription = null,
                        tint               = if (reorderMode) GreenCol else SubText,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text       = if (reorderMode) "Done" else "Reorder",
                        color      = if (reorderMode) GreenCol else SubText,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            orderedSections.withIndex().toList().chunked(2).forEach { rowPair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    rowPair.forEach { (idx, section) ->
                      key(section.title) {
                        val alpha        = btnVisible.getOrNull(idx)?.value ?: 1f
                        val badgeCount   = sectionModuleCounts[section.title] ?: 0
                        val hasBadge     = !reorderMode && badgeCount > 0
                        val isPulsing    = !reorderMode && (sectionPulseFlags[section.title] == true)
                        val isDragging   = draggingSectionTitle == section.title
                        // Multi-word titles (e.g. "TIME MANAGEMNT") wrap onto 2 lines;
                        // single-word titles (e.g. "TOOLS") stay on 1 line.
                        val displayTitle = section.title.replace(" ", "\n")

                        var pressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue   = if (pressed) 0.97f else if (isDragging) 1.06f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness    = Spring.StiffnessHigh
                            ),
                            label = "catScale_${section.title}"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(47.dp)
                                .onGloballyPositioned { coords ->
                                    itemBounds[section.title] = Rect(coords.positionInRoot(), coords.size.toSize())
                                }
                                .zIndex(if (isDragging) 1f else 0f)
                                .scale(scale)
                                .graphicsLayer {
                                    this.alpha        = alpha
                                    this.translationX = (1f - alpha) * -40f + if (isDragging) dragOffset.x else 0f
                                    this.translationY = if (isDragging) dragOffset.y else 0f
                                    this.shadowElevation = if (isDragging) 12f else 0f
                                }
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDragging) appColors.card.copy(alpha = 0.98f) else appColors.card)
                                .border(
                                    width = if (isPulsing) 2.dp else if (isDragging) 1.5.dp else 0.dp,
                                    color = when {
                                        isPulsing  -> RedCol.copy(alpha = categoryPulseAlpha)
                                        isDragging -> section.color.copy(alpha = 0.7f)
                                        else       -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .then(
                                    if (reorderMode) {
                                        Modifier.pointerInput(section.title) {
                                            var dragStartCenter = Offset.Zero
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggingSectionTitle = section.title
                                                    dragOffset = Offset.Zero
                                                    // Anchor to the tile's position at the moment the
                                                    // drag begins. If we re-read live bounds every frame
                                                    // instead, a swap mid-drag shifts this tile into a new
                                                    // resting slot and the pointer math jumps/jitters.
                                                    dragStartCenter = itemBounds[section.title]?.center ?: Offset.Zero
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, delta ->
                                                    change.consume()
                                                    dragOffset += delta
                                                    val pointerPos = dragStartCenter + dragOffset
                                                    val targetTitle = itemBounds.entries
                                                        .firstOrNull { (t, r) -> t != section.title && r.contains(pointerPos) }
                                                        ?.key
                                                    if (targetTitle != null) {
                                                        val from = orderedSections.indexOfFirst { it.title == section.title }
                                                        val to   = orderedSections.indexOfFirst { it.title == targetTitle }
                                                        if (from != -1 && to != -1) moveSection(from, to)
                                                    }
                                                },
                                                onDragEnd    = { draggingSectionTitle = null; dragOffset = Offset.Zero },
                                                onDragCancel = { draggingSectionTitle = null; dragOffset = Offset.Zero }
                                            )
                                        }
                                    } else {
                                        Modifier.clickable {
                                            pressed = false
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            openSection = section
                                        }
                                    }
                                )
                        ) {
                            Row(
                                modifier          = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Coloured left accent bar
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(section.color)
                                )

                                Spacer(Modifier.width(10.dp))

                                // Icon — centered against the (possibly 2-line) title
                                Icon(
                                    imageVector        = section.icon,
                                    contentDescription = null,
                                    tint               = section.color,
                                    modifier           = Modifier.size(30.dp)
                                )

                                Spacer(Modifier.width(8.dp))

                                // Title — takes remaining space, wraps to 2 lines when needed
                                Text(
                                    text          = displayTitle,
                                    color         = Color.White,
                                    fontSize      = 15.sp,
                                    fontWeight    = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp,
                                    lineHeight    = 16.sp,
                                    maxLines      = 2,
                                    modifier      = Modifier.weight(1f)
                                )

                                // Drag handle (reorder mode) / badge / chevron
                                when {
                                    reorderMode -> {
                                        Icon(
                                            imageVector        = Icons.Default.DragIndicator,
                                            contentDescription = "Drag to reorder",
                                            tint               = section.color.copy(alpha = 0.8f),
                                            modifier           = Modifier
                                                .padding(end = 8.dp)
                                                .size(20.dp)
                                        )
                                    }
                                    hasBadge -> {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .background(section.color, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                badgeCount.toString(),
                                                color      = Color.White,
                                                fontSize   = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    else -> {
                                        Icon(
                                            imageVector        = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint               = section.color.copy(alpha = 0.6f),
                                            modifier           = Modifier
                                                .padding(end = 8.dp)
                                                .size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                      }
                    }
                    // Fill the empty slot if this is an odd last row
                    if (rowPair.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── ZONE 3: Footer ────────────────────────────────────────────────

            // Settings expand — 3 module chips animate upward
            AnimatedVisibility(
                visible = settingsExpanded,
                enter   = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    expandFrom = Alignment.Bottom
                ) + fadeIn(),
                exit    = shrinkVertically(
                    animationSpec = tween(200),
                    shrinkTowards = Alignment.Bottom
                ) + fadeOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    SETTINGS_MODULES.forEach { mod ->
                        val enabled = mod.route in PHASE1_ROUTES

                        var pressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue   = if (pressed) 0.93f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness    = Spring.StiffnessHigh
                            ),
                            label = "settingsTileScale_${mod.route}"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(76.dp)
                                .scale(scale)
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.card)
                                .border(
                                    width = 1.dp,
                                    color = mod.color.copy(alpha = if (enabled) 0.5f else 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .then(
                                    if (enabled) Modifier.clickable {
                                        pressed          = false
                                        settingsExpanded = false
                                        onNavigate(mod.route)
                                    } else Modifier
                                )
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector        = mod.icon,
                                contentDescription = null,
                                tint               = if (enabled) mod.color else mod.color.copy(alpha = 0.3f),
                                modifier           = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text       = mod.label,
                                color      = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 11.sp,
                                textAlign  = TextAlign.Center,
                                maxLines   = 2
                            )
                        }
                    }
                }
            }

            // Settings + Exit buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .graphicsLayer { alpha = footerAlpha }
            ) {
                // Settings button
                Button(
                    onClick  = { settingsExpanded = !settingsExpanded },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeCol),
                    shape  = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector        = if (settingsExpanded) Icons.Default.ExpandMore else Icons.Default.Tune,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Settings",
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 20.sp
                    )
                }

                // Exit button
                Button(
                    onClick  = { showExitDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = RedCol),
                    shape  = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Exit App",
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}
