package com.phoenix.luminacn.shiyi

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.overlay.manager.OverlayWindow

class RenderOverlay : OverlayWindow() {

    // --- 重构后的 layoutParams，避免触摸拦截问题 ---
    override val layoutParams by lazy {
        WindowManager.LayoutParams().apply {
            // 使用更安全的窗口类型
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // 关键修改：使用 WRAP_CONTENT 避免全屏覆盖
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            
            // 关键修改：移除所有可能导致触摸问题的 flags
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            
            // 保持在左上角，但实际绘制区域由内容决定
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            
            format = PixelFormat.TRANSLUCENT
            
            // 适配刘海屏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    companion object {
        val overlayInstance by lazy { RenderOverlay() }
        private var shouldShowOverlay = false
        private var currentSession: com.phoenix.luminacn.constructors.NetBound? = null

        fun showOverlay() {
            if (shouldShowOverlay) {
                try {
                    OverlayManager.showOverlayWindow(overlayInstance)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun dismissOverlay() {
            try {
                OverlayManager.dismissOverlayWindow(overlayInstance)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (enabled) showOverlay() else dismissOverlay()
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        fun setSession(session: com.phoenix.luminacn.constructors.NetBound?) {
            currentSession = session
        }
    }

    private fun setImmersiveMode(view: View) {
        @Suppress("DEPRECATION")
        view.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        // 使用 Box 包装，但不强制 fillMaxSize
        Box {
            AndroidView(
                factory = { ctx ->
                    RenderOverlayView(ctx).apply {
                        // 设置透明背景
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        // 使用 WRAP_CONTENT 让内容决定大小
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        
                        // 确保视图不拦截触摸
                        isClickable = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        setOnTouchListener { _, _ -> false }
                    }
                },
                update = { view ->
                    view.post { setImmersiveMode(view) }
                    view.invalidate()
                }
            )
        }
    }
}