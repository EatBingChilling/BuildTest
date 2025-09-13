package com.phoenix.luminacn.overlay.manager

import android.app.Activity
import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.input.InputManager
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CoroutineScope

@Suppress("MemberVisibilityCanBePrivate")
abstract class OverlayWindow {

    var minimapZoom: Float = 1.0f
    var minimapDotSize: Int = 5

    open val layoutParams by lazy {
        LayoutParams().apply {
            width = LayoutParams.WRAP_CONTENT
            height = LayoutParams.WRAP_CONTENT
            gravity = Gravity.START or Gravity.TOP
            x = 0
            y = 0
            type = LayoutParams.TYPE_APPLICATION_OVERLAY
            
            // 关键修复：只使用最基本的flag配置
            // 移除所有可能导致触摸问题的flag
            flags = LayoutParams.FLAG_NOT_FOCUSABLE
            // 移除的问题flag：
            // - FLAG_LAYOUT_IN_SCREEN (可能扩展触摸区域)
            // - FLAG_NOT_TOUCH_MODAL (与触摸穿透冲突)  
            // - FLAG_WATCH_OUTSIDE_TOUCH (会监听外部触摸，干扰其他窗口)
            // - FLAG_HARDWARE_ACCELERATED (可能在某些设备上有问题)
            // - FLAG_LAYOUT_NO_LIMITS (最大的问题flag，会扩展窗口到屏幕外)
            // - FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS (不必要的flag)
            
            format = PixelFormat.TRANSLUCENT
            
            // 移除alpha设置，避免可能的问题
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //     alpha = (OverlayManager.currentContext!!.getSystemService(Service.INPUT_SERVICE) as? InputManager)?.maximumObscuringOpacityForTouch ?: 1f
            // }
        }
    }

    open val composeView by lazy {
        ComposeView(OverlayManager.currentContext!!).apply {
            // 简化系统UI设置，移除可能有问题的flag
            systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            
            // 确保触摸属性正确设置
            isClickable = true  // 按钮需要能被点击
            isFocusable = false  // 但不要获取焦点
            isFocusableInTouchMode = false
        }
    }

    val windowManager: WindowManager
        get() = OverlayManager.currentContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val lifecycleOwner = OverlayLifecycleOwner()

    val viewModelStore = ViewModelStore()

    val composeScope: CoroutineScope

    val recomposer: Recomposer

    var firstRun = true

    /**
     * Get the activity context if available, or null if in overlay mode
     */
    fun getActivityContext(): Activity? {
        return OverlayManager.currentContext as? Activity
    }

    /**
     * Show a toast message safely from overlay context
     */
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        try {
            Toast.makeText(OverlayManager.currentContext, message, duration).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {
        lifecycleOwner.performRestore(null)

        val coroutineContext = AndroidUiDispatcher.CurrentThread
        composeScope = CoroutineScope(coroutineContext)
        recomposer = Recomposer(coroutineContext)
    }

    @Composable
    abstract fun Content()
}