package com.phoenix.luminacn.shiyi

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.phoenix.luminacn.overlay.manager.OverlayWindow
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.constructors.GameManager
import kotlinx.coroutines.delay

class NameTagOverlayService : Service() {

    private var nameTagOverlay: NameTagOverlay? = null
    private var isOverlayShown = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        nameTagOverlay = NameTagOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> showNameTagOverlay()
            ACTION_HIDE_OVERLAY -> hideNameTagOverlay()
            ACTION_STOP_SERVICE -> {
                hideNameTagOverlay()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun showNameTagOverlay() {
        if (!isOverlayShown) {
            nameTagOverlay?.let { overlay ->
                try {
                    OverlayManager.showOverlayWindow(overlay)
                    isOverlayShown = true
                    android.util.Log.d("NameTagService", "NameTag overlay shown")
                } catch (e: Exception) {
                    android.util.Log.e("NameTagService", "Failed to show overlay", e)
                }
            }
        }
    }

    private fun hideNameTagOverlay() {
        if (isOverlayShown) {
            nameTagOverlay?.let { overlay ->
                try {
                    OverlayManager.dismissOverlayWindow(overlay)
                    isOverlayShown = false
                    android.util.Log.d("NameTagService", "NameTag overlay hidden")
                } catch (e: Exception) {
                    android.util.Log.e("NameTagService", "Failed to hide overlay", e)
                }
            }
        }
    }

    override fun onDestroy() {
        hideNameTagOverlay()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.phoenix.luminacn.SHOW_NAMETAG_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.phoenix.luminacn.HIDE_NAMETAG_OVERLAY"
        const val ACTION_STOP_SERVICE = "com.phoenix.luminacn.STOP_NAMETAG_SERVICE"
    }
}

class NameTagOverlay : OverlayWindow() {

    // --- 重构后的安全 layoutParams ---
    private val _layoutParams by lazy {
        WindowManager.LayoutParams().apply {
            // 使用安全的窗口类型
            type = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // 关键修改：只使用安全的 flags
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

            // 如果需要全屏绘制，使用确切像素值而不是 MATCH_PARENT
            val metrics = OverlayManager.currentContext!!.resources.displayMetrics
            width = metrics.widthPixels
            height = metrics.heightPixels

            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            format = PixelFormat.TRANSLUCENT

            // 适配刘海屏
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        val context = LocalContext.current

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    try {
                        NameTagRenderView(ctx).apply {
                            setBackgroundColor(Color.TRANSPARENT)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            // 确保不拦截触摸
                            isClickable = false
                            isFocusable = false
                            isFocusableInTouchMode = false
                            setOnTouchListener { _, _ -> false }
                            
                            // 使用软件渲染避免某些设备问题
                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            
                            systemUiVisibility = (
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NameTagOverlay", "Failed to create NameTagRenderView", e)
                        // 返回一个空的 View 作为备选
                        View(ctx).apply {
                            setBackgroundColor(Color.TRANSPARENT)
                            isClickable = false
                            isFocusable = false
                            setOnTouchListener { _, _ -> false }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    if (view is NameTagRenderView) {
                        // 定期检查并更新 session
                        view.updateSession(GameManager.netBound)
                        view.requestLayout()
                    }
                }
            )
        }

        // 定期更新 session
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000) // 每秒检查一次
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                android.util.Log.d("NameTagOverlay", "NameTag overlay disposed")
            }
        }
    }
}