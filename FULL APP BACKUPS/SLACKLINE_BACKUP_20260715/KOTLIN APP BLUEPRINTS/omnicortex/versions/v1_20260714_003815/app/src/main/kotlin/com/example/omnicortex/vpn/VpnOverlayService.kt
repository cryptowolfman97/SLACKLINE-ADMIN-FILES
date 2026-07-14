package com.example.omnicortex.vpn

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * VpnOverlayService
 *
 * A foreground-free background service that draws a small draggable
 * floating bubble over other apps (requires SYSTEM_ALERT_WINDOW).
 *
 * The bubble shows:
 *  - Shield icon (green = connected, grey = disconnected)
 *  - "Protected" / "Off" label
 *  - Tap to return to the app
 *  - × button to dismiss the bubble (VPN keeps running)
 */
class VpnOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_SHOW = "com.example.omnicortex.overlay.SHOW"
        const val ACTION_HIDE = "com.example.omnicortex.overlay.HIDE"

        fun show(context: Context) {
            context.startService(Intent(context, VpnOverlayService::class.java).setAction(ACTION_SHOW))
        }

        fun hide(context: Context) {
            context.startService(Intent(context, VpnOverlayService::class.java).setAction(ACTION_HIDE))
        }
    }

    // ── Lifecycle / SavedState boilerplate required for ComposeView in a Service ──

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> removeOverlay()
            else        -> showOverlay()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlay()
        scope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    // ── Overlay ───────────────────────────────────────────────────────────────

    private fun showOverlay() {
        if (overlayView != null) return
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 200
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VpnOverlayService)
            setViewTreeSavedStateRegistryOwner(this@VpnOverlayService)
            setContent {
                VpnBubble(
                    params      = params,
                    wm          = windowManager!!,
                    onDismiss   = { removeOverlay() },
                    onTap       = { launchMainActivity() }
                )
            }
        }

        overlayView = view
        windowManager?.addView(view, params)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
        stopSelf()
    }

    private fun launchMainActivity() {
        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent?.let { startActivity(it) }
    }
}

// ── Bubble Composable ──────────────────────────────────────────────────────────

@Composable
private fun VpnBubble(
    params: WindowManager.LayoutParams,
    wm: WindowManager,
    onDismiss: () -> Unit,
    onTap: () -> Unit
) {
    val vpnState by WarpVpnEngine.state.collectAsState(initial = VpnState.DISCONNECTED)
    val isConnected = vpnState is VpnState.CONNECTED

    val AegisCyan  = Color(0xFF00FFD1)
    val BgDark     = Color(0xCC111318)

    // Pulse animation when connected
    val pulse = rememberInfiniteTransition(label = "bubblePulse")
    val pulseScale by pulse.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.08f,
        animationSpec  = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label          = "scale"
    )

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .scale(if (isConnected) pulseScale else 1f)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    params.x += dragAmount.x.toInt()
                    params.y += dragAmount.y.toInt()
                    try { wm.updateViewLayout(
                        // view reference not needed — WM tracks it
                        null, params
                    ) } catch (_: Exception) {}
                }
            }
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(BgDark)
                .border(
                    width  = 1.dp,
                    color  = if (isConnected) AegisCyan.copy(0.5f) else Color(0xFF2A2D35),
                    shape  = RoundedCornerShape(50.dp)
                )
                .clickable { onTap() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint     = if (isConnected) AegisCyan else Color(0xFF4B5563),
                modifier = Modifier.size(16.dp)
            )
            Text(
                if (isConnected) "Protected" else "VPN Off",
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (isConnected) AegisCyan else Color(0xFF6B7280)
            )
            Spacer(Modifier.width(2.dp))
            // Dismiss button
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F2128))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint     = Color(0xFF6B7280),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
