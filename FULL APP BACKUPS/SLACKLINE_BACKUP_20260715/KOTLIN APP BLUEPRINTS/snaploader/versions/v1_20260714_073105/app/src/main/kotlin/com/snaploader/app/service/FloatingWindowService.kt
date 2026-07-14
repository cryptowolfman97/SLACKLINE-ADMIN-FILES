package com.snaploader.app.service

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.view.WindowManager.LayoutParams.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.snaploader.app.MainActivity
import com.snaploader.app.SnapLoaderApp
import kotlin.math.abs

class FloatingWindowService : Service() {

    companion object {
        const val ACTION_UPDATE_DOWNLOADS       = "com.snaploader.app.FLOAT_UPDATE"
        const val ACTION_SHARE_DOWNLOAD_STARTED = "com.snaploader.app.SHARE_DOWNLOAD"
        const val EXTRA_COUNT                   = "active_count"
        const val EXTRA_DOWNLOAD_TITLES         = "download_titles"
        const val EXTRA_DOWNLOAD_PROGRESSES     = "download_progresses"
        const val EXTRA_SHARE_TITLE             = "share_title"
        const val EXTRA_ACCENT_DARK             = "accent_dark"
        const val EXTRA_ACCENT_CONTAINER        = "accent_container"

        private const val NOTIF_ID          = 9001
        private const val DEFAULT_ACCENT    = 0xFF00C853.toInt()
        private const val DEFAULT_CONTAINER = 0xFF003314.toInt()

        fun buildIntent(context: Context, activeCount: Int = 0): Intent =
            Intent(context, FloatingWindowService::class.java).apply {
                putExtra(EXTRA_COUNT, activeCount)
            }
    }

    private lateinit var windowManager : WindowManager
    private var bubbleView   : View? = null
    private var expandedView : View? = null
    private var isExpanded   = false

    private var activeCount        = 0
    private var downloadTitles     : Array<String> = emptyArray()
    private var downloadProgresses : IntArray      = IntArray(0)
    private var shareTitle         = ""
    private var showingSharePip    = false

    private var accentColor    = DEFAULT_ACCENT
    private var containerColor = DEFAULT_CONTAINER

    private var bubbleX      = 0
    private var bubbleY      = 200
    private var screenWidth  = 0
    private var screenHeight = 0

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val al = intent.getLongExtra(EXTRA_ACCENT_DARK,    DEFAULT_ACCENT.toLong())
            val cl = intent.getLongExtra(EXTRA_ACCENT_CONTAINER, DEFAULT_CONTAINER.toLong())
            accentColor    = al.toInt()
            containerColor = cl.toInt()

            when (intent.action) {
                ACTION_UPDATE_DOWNLOADS -> {
                    activeCount        = intent.getIntExtra(EXTRA_COUNT, 0)
                    downloadTitles     = intent.getStringArrayExtra(EXTRA_DOWNLOAD_TITLES) ?: emptyArray()
                    downloadProgresses = intent.getIntArrayExtra(EXTRA_DOWNLOAD_PROGRESSES) ?: IntArray(0)
                    refreshBubble()
                    if (isExpanded) rebuildExpanded()
                }
                ACTION_SHARE_DOWNLOAD_STARTED -> {
                    shareTitle      = intent.getStringExtra(EXTRA_SHARE_TITLE) ?: ""
                    showingSharePip = true
                    activeCount     = (activeCount + 1).coerceAtLeast(1)
                    refreshBubble()
                    if (!isExpanded) expandBubble()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        val metrics   = resources.displayMetrics
        screenWidth   = metrics.widthPixels
        screenHeight  = metrics.heightPixels
        bubbleX       = screenWidth - dpToPx(72)

        val filter = IntentFilter().apply {
            addAction(ACTION_UPDATE_DOWNLOADS)
            addAction(ACTION_SHARE_DOWNLOAD_STARTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(updateReceiver, filter, RECEIVER_NOT_EXPORTED)
        else
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(updateReceiver, filter)

        startForeground(NOTIF_ID, buildNotification())
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        intent?.let {
            activeCount        = it.getIntExtra(EXTRA_COUNT, 0)
            downloadTitles     = it.getStringArrayExtra(EXTRA_DOWNLOAD_TITLES) ?: emptyArray()
            downloadProgresses = it.getIntArrayExtra(EXTRA_DOWNLOAD_PROGRESSES) ?: IntArray(0)
            accentColor        = it.getLongExtra(EXTRA_ACCENT_DARK,    DEFAULT_ACCENT.toLong()).toInt()
            containerColor     = it.getLongExtra(EXTRA_ACCENT_CONTAINER, DEFAULT_CONTAINER.toLong()).toInt()
        }
        refreshBubble()
        return START_STICKY
    }

    override fun onDestroy() {
        try { unregisterReceiver(updateReceiver) } catch (_: Exception) {}
        removeBubble(); removeExpanded()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Bubble ────────────────────────────────────────────────────────────────
    private fun showBubble() {
        if (bubbleView != null) return
        val size = dpToPx(60)

        val params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") TYPE_PHONE,
            FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = bubbleX; y = bubbleY }

        val bubble = FrameLayout(this).apply {
            elevation  = 12f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentColor)
                setStroke(dpToPx(2), darken(accentColor, 0.6f))
            }

            addView(TextView(this@FloatingWindowService).apply {
                text     = "⬇"
                textSize = 22f
                gravity  = Gravity.CENTER
                setTextColor(0xFF000000.toInt())
            }, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

            addView(TextView(this@FloatingWindowService).apply {
                id         = android.R.id.text1
                textSize   = 9f
                setTextColor(0xFFFFFFFF.toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(0xFFFF5252.toInt())
                }
                gravity    = Gravity.CENTER
                setPadding(dpToPx(2), dpToPx(1), dpToPx(2), dpToPx(1))
                visibility = if (activeCount > 0) View.VISIBLE else View.GONE
                text       = "$activeCount"
            }, FrameLayout.LayoutParams(dpToPx(18), dpToPx(18)).apply {
                gravity = Gravity.TOP or Gravity.END; topMargin = dpToPx(2); rightMargin = dpToPx(2)
            })
        }

        var downRawX = 0f; var downRawY = 0f
        var downX    = 0;  var downY    = 0
        var dragging = false

        bubble.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX; downRawY = event.rawY
                    downX = params.x; downY = params.y; dragging = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX; val dy = event.rawY - downRawY
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        dragging = true
                        params.x = (downX + dx.toInt()).coerceIn(0, screenWidth - size)
                        params.y = (downY + dy.toInt()).coerceIn(0, screenHeight - size)
                        bubbleX = params.x; bubbleY = params.y
                        windowManager.updateViewLayout(v, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        if (isExpanded) collapseExpanded() else expandBubble()
                    } else {
                        params.x = if (params.x + size / 2 < screenWidth / 2) 0
                                   else screenWidth - size
                        bubbleX = params.x
                        windowManager.updateViewLayout(v, params)
                    }
                    true
                }
                else -> false
            }
        }

        bubbleView = bubble
        windowManager.addView(bubble, params)
    }

    private fun refreshBubble() {
        val badge = bubbleView?.findViewById<TextView>(android.R.id.text1) ?: return
        badge.text       = "$activeCount"
        badge.visibility = if (activeCount > 0) View.VISIBLE else View.GONE
        (bubbleView as? FrameLayout)?.let { frame ->
            (frame.background as? GradientDrawable)?.setColor(accentColor)
        }
    }

    private fun removeBubble() {
        bubbleView?.let { try { windowManager.removeViewImmediate(it) } catch (_: Exception) {} }
        bubbleView = null
    }

    // ── Expanded panel ────────────────────────────────────────────────────────
    private fun expandBubble() {
        isExpanded = true
        removeExpanded()

        val panelW = dpToPx(268)
        val px     = (bubbleX - panelW + dpToPx(60)).coerceIn(0, screenWidth - panelW)
        val py     = (bubbleY + dpToPx(68)).coerceIn(0, screenHeight - dpToPx(240))

        val wlp = WindowManager.LayoutParams(
            panelW, WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") TYPE_PHONE,
            FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = px; y = py }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(14))
            background  = GradientDrawable().apply {
                setColor(0xFF111111.toInt())
                cornerRadius = dpToPx(20).toFloat()
                setStroke(dpToPx(1), accentColor and 0x66FFFFFF.toInt())
            }
            elevation = 20f

            // Header row
            addView(LinearLayout(this@FloatingWindowService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL

                addView(View(this@FloatingWindowService).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL; setColor(accentColor)
                    }
                }, LinearLayout.LayoutParams(dpToPx(8), dpToPx(8)).apply { rightMargin = dpToPx(8) })

                addView(TextView(this@FloatingWindowService).apply {
                    text     = "SnapLoader"
                    textSize = 13f
                    setTextColor(0xFFFFFFFF.toInt())
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

                addView(TextView(this@FloatingWindowService).apply {
                    text = "✕"; textSize = 15f
                    setTextColor(0xFF9E9E9E.toInt())
                    setPadding(dpToPx(10), dpToPx(2), 0, dpToPx(2))
                    setOnClickListener { stopSelf() }
                })
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

            // Divider
            addView(View(this@FloatingWindowService).apply {
                setBackgroundColor(0xFF2A2A2A.toInt())
            }, LinearLayout.LayoutParams(MATCH_PARENT, dpToPx(1)).apply {
                topMargin = dpToPx(10); bottomMargin = dpToPx(10)
            })

            // Download rows
            val hasItems = downloadTitles.isNotEmpty() || (showingSharePip && shareTitle.isNotEmpty())
            if (!hasItems) {
                addView(TextView(this@FloatingWindowService).apply {
                    text = "✅  No active downloads"; textSize = 12f
                    setTextColor(0xFF9E9E9E.toInt())
                })
            } else {
                if (showingSharePip && shareTitle.isNotEmpty()) addDownloadRow(this, shareTitle, -1)
                downloadTitles.forEachIndexed { i, t ->
                    addDownloadRow(this, t, downloadProgresses.getOrElse(i) { 0 })
                }
            }

            // Divider
            addView(View(this@FloatingWindowService).apply {
                setBackgroundColor(0xFF2A2A2A.toInt())
            }, LinearLayout.LayoutParams(MATCH_PARENT, dpToPx(1)).apply {
                topMargin = dpToPx(10); bottomMargin = dpToPx(10)
            })

            // Open App button
            addView(TextView(this@FloatingWindowService).apply {
                text     = "Open App  →"; textSize = 12f
                setTextColor(accentColor)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity  = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(containerColor)
                    cornerRadius = dpToPx(10).toFloat()
                    setStroke(dpToPx(1), accentColor)
                }
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                setOnClickListener {
                    startActivity(Intent(this@FloatingWindowService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    collapseExpanded()
                }
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        expandedView = panel
        windowManager.addView(panel, wlp)
    }

    private fun addDownloadRow(container: LinearLayout, title: String, progress: Int) {
        container.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(2), 0, dpToPx(8))

            addView(TextView(this@FloatingWindowService).apply {
                text     = if (title.length > 30) "${title.take(28)}…" else title
                textSize = 11f
                setTextColor(0xFFEEEEEE.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

            val isIndeterminate = progress < 0
            val track = FrameLayout(this@FloatingWindowService).apply {
                background = GradientDrawable().apply {
                    setColor(0xFF2A2A2A.toInt()); cornerRadius = dpToPx(3).toFloat()
                }
            }
            if (!isIndeterminate && progress in 0..100) {
                track.addView(View(this@FloatingWindowService).apply {
                    background = GradientDrawable().apply {
                        setColor(accentColor); cornerRadius = dpToPx(3).toFloat()
                    }
                }, FrameLayout.LayoutParams(
                    (dpToPx(240) * progress / 100).coerceAtLeast(dpToPx(4)),
                    MATCH_PARENT
                ))
            }
            addView(track, LinearLayout.LayoutParams(MATCH_PARENT, dpToPx(6)).apply {
                topMargin = dpToPx(5)
            })

            addView(TextView(this@FloatingWindowService).apply {
                text     = if (isIndeterminate) "Starting…" else "$progress%"
                textSize = 9f; gravity = Gravity.END
                setTextColor(accentColor)
            }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dpToPx(2) })

        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private fun rebuildExpanded() { removeExpanded(); expandBubble() }
    private fun collapseExpanded() { isExpanded = false; removeExpanded() }
    private fun removeExpanded() {
        expandedView?.let { try { windowManager.removeViewImmediate(it) } catch (_: Exception) {} }
        expandedView = null
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingWindowService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, SnapLoaderApp.OVERLAY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("SnapLoader — Download Monitor")
            .setContentText("Floating window active")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", stopIntent)
            .setOngoing(true).setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun darken(color: Int, factor: Float): Int {
        val a = color ushr 24
        val r = ((color shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = ((color shr 8  and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((color         and 0xFF) * factor).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
