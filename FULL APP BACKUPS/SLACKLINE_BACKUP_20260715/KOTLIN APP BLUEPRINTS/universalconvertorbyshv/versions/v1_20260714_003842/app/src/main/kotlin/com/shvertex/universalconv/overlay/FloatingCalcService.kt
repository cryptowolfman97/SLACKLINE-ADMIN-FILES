package com.shvertex.universalconv.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.shvertex.universalconv.ui.components.BasicCalcGrid
import com.shvertex.universalconv.ui.components.CalcEngine
import com.shvertex.universalconv.ui.theme.*

/**
 * Floating overlay window that renders a fully touch-interactive calculator.
 * Uses [ComposeView] embedded in a [WindowManager] overlay layer
 * (TYPE_APPLICATION_OVERLAY, requires SYSTEM_ALERT_WINDOW permission).
 *
 * The window is draggable via the top handle bar, and dismisses via the X button.
 */
class FloatingCalcService : Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    // ── Lifecycle / SavedState boilerplate required by ComposeView ──
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val vmStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = vmStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ── Window / view refs ──────────────────────────────────────────
    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView

    private val calcEngine = CalcEngine()

    // ── Service lifecycle ───────────────────────────────────────────

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupOverlayWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (::composeView.isInitialized) {
            try { windowManager.removeView(composeView) } catch (_: Exception) {}
        }
        vmStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Overlay setup ───────────────────────────────────────────────

    private fun setupOverlayWindow() {
        val params = WindowManager.LayoutParams(
            dpToPx(280),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }

        composeView = ComposeView(this).apply {
            // ComposeView needs these owners to work outside an Activity
            setViewTreeLifecycleOwner(this@FloatingCalcService)
            setViewTreeViewModelStoreOwner(this@FloatingCalcService)
            setViewTreeSavedStateRegistryOwner(this@FloatingCalcService)

            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    FloatingCalcWindow(
                        engine   = calcEngine,
                        onClose  = { stopSelf() },
                        onDrag   = { dx, dy ->
                            params.x = (params.x + dx.toInt()).coerceAtLeast(0)
                            params.y = (params.y + dy.toInt()).coerceAtLeast(0)
                            try { windowManager.updateViewLayout(composeView, params) } catch (_: Exception) {}
                        },
                    )
                }
            }

            // While FLAG_NOT_FOCUSABLE is set the view still receives
            // touch events via this override — which makes calc buttons work.
            setOnTouchListener { v, event ->
                v.onTouchEvent(event)
                false
            }
        }

        windowManager.addView(composeView, params)
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}

// ── Floating window Composable ──────────────────────────────────────

@Composable
private fun FloatingCalcWindow(
    engine: CalcEngine,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D0D)),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {

            // ── Drag handle bar ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Rounded.DragHandle,
                        contentDescription = "Drag",
                        tint = Color(0xFF444444),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Calculator",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D9B8),
                        fontSize = 12.sp,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF8A8A8A),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            // ── Display ─────────────────────────────────────────────
            FloatingCalcDisplay(engine)

            // ── Keypad ──────────────────────────────────────────────
            BasicCalcGrid(
                engine   = engine,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                fontSize = 16.sp,
                spacing  = 5.dp,
            )
        }
    }
}

@Composable
private fun FloatingCalcDisplay(engine: CalcEngine) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141414))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            engine.expr.ifEmpty { "0" },
            style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color    = Color(0xFF8A8A8A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            fontSize  = 11.sp,
        )
        Text(
            engine.result.ifEmpty { "" },
            style      = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Bold,
            color      = Color(0xFFF0F0F0),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.fillMaxWidth(),
            textAlign  = TextAlign.End,
            fontSize   = 18.sp,
        )
    }
}
