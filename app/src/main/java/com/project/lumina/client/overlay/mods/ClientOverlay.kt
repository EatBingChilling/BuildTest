// ClientOverlay.kt  优化修复版
package com.project.lumina.client.overlay.mods

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.project.lumina.client.R
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// 生命周期管理改进
private class OverlayLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    
    init {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    
    fun destroy() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
    
    override val lifecycle: Lifecycle get() = registry
}

class ClientOverlay : OverlayWindow() {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

    private var watermarkText by mutableStateOf(prefs.getString("text", "") ?: "")
    private var textColor by mutableStateOf(prefs.getInt("color", Color.WHITE))
    private var shadowEnabled by mutableStateOf(prefs.getBoolean("shadow", false))
    private var fontSize by mutableStateOf(prefs.getInt("size", 28).coerceIn(5, 300))
    private var rainbowEnabled by mutableStateOf(prefs.getBoolean("rainbow", false))
    private var opacity by mutableStateOf(prefs.getInt("opacity", 100).coerceIn(0, 100))
    private var position by mutableStateOf(prefs.getString("position", "Center") ?: "Center")

    private var configDialogShown by mutableStateOf(false)
    private var currentConfigLifecycleOwner: OverlayLifecycleOwner? = null
    private var currentConfigComposeView: View? = null

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private var overlayInstance: ClientOverlay? = null
        private var shouldShowOverlay = true

        private val appContext: Context by lazy {
            val appClass = Class.forName("android.app.ActivityThread")
            val method = appClass.getMethod("currentApplication")
            method.invoke(null) as Application
        }

        fun showOverlay() {
            if (shouldShowOverlay) {
                overlayInstance = ClientOverlay()
                try {
                    OverlayManager.showOverlayWindow(overlayInstance!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun dismissOverlay() {
            try {
                overlayInstance?.let { OverlayManager.dismissOverlayWindow(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (!enabled) dismissOverlay()
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        fun showConfigDialog() {
            overlayInstance?.showConfigDialog()
        }
    }

    // 对话框关闭处理
    private fun closeConfigDialog() {
        try {
            (currentConfigComposeView?.parent as? WindowManager)?.removeView(currentConfigComposeView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        currentConfigLifecycleOwner?.destroy()
        currentConfigLifecycleOwner = null
        currentConfigComposeView = null
        configDialogShown = false
    }

    // Compose Material3 弹窗
    @Composable
    fun ConfigDialog(onDismiss: () -> Unit) {
        var localText by remember { mutableStateOf(watermarkText) }
        var localRed by remember { mutableStateOf(Color.red(textColor)) }
        var localGreen by remember { mutableStateOf(Color.green(textColor)) }
        var localBlue by remember { mutableStateOf(Color.blue(textColor)) }
        var localShadow by remember { mutableStateOf(shadowEnabled) }
        var localSize by remember { mutableStateOf(fontSize.toFloat()) }
        var localRain by remember { mutableStateOf(rainbowEnabled) }
        var localAlpha by remember { mutableStateOf(opacity.toFloat()) }
        var localPosition by remember { mutableStateOf(position) }

        val localColor = Color.rgb(localRed, localGreen, localBlue)

        Dialog(
            onDismissRequest = { onDismiss() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("水印配置", style = MaterialTheme.typography.headlineSmall)

                    // 水印文本输入
                    OutlinedTextField(
                        value = localText,
                        onValueChange = { 
                            if (it.length <= 20) localText = it 
                        },
                        label = { Text("水印文字") },
                        singleLine = true,
                        placeholder = { Text("输入水印文字") }
                    )

                    // 颜色预览
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("预览颜色", modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(ComposeColor(localColor), MaterialTheme.shapes.medium)
                        )
                    }

                    // 颜色滑动选择器
                    listOf("红" to { it: Float -> localRed = it.toInt() }, 
                           "绿" to { it: Float -> localGreen = it.toInt() },
                           "蓝" to { it: Float -> localBlue = it.toInt() }
                    ).forEachIndexed { idx, (label, setter) ->
                        Column {
                            Text("$label: ${when (idx) {
                                0 -> localRed
                                1 -> localGreen
                                else -> localBlue
                            }}")
                            Slider(
                                value = when (idx) {
                                    0 -> localRed.toFloat()
                                    1 -> localGreen.toFloat()
                                    else -> localBlue.toFloat()
                                },
                                onValueChange = setter,
                                valueRange = 0f..255f,
                                steps = 255
                            )
                        }
                    }

                    // 字体大小
                    Column {
                        Text("字体大小: ${localSize.roundToInt()} 像素")
                        Slider(
                            value = localSize,
                            onValueChange = { localSize = it },
                            valueRange = 5f..300f,
                            steps = 295
                        )
                    }

                    // 透明度
                    Column {
                        Text("透明度: ${localAlpha.roundToInt()}%")
                        Slider(
                            value = localAlpha,
                            onValueChange = { localAlpha = it },
                            valueRange = 0f..100f,
                            steps = 100
                        )
                    }

                    // 位置选择
                    Column {
                        Text("显示位置")
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            listOf("左上", "右上", "左下", "右下", "居中").forEach { pos ->
                                FilterChip(
                                    selected = localPosition == pos,
                                    onClick = { localPosition = pos },
                                    label = { Text(pos) }
                                )
                            }
                        }
                    }

                    // 特效开关
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("阴影效果", modifier = Modifier.weight(1f))
                        Switch(checked = localShadow, onCheckedChange = { localShadow = it })
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("彩虹特效", modifier = Modifier.weight(1f))
                        Switch(checked = localRain, onCheckedChange = { localRain = it })
                    }

                    // 底部按钮
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { 
                            onDismiss() 
                        }) { 
                            Text("取消") 
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            watermarkText = localText
                            textColor = localColor
                            shadowEnabled = localShadow
                            fontSize = localSize.roundToInt()
                            rainbowEnabled = localRain
                            opacity = localAlpha.roundToInt()
                            position = localPosition

                            prefs.edit()
                                .putString("text", watermarkText)
                                .putInt("color", textColor)
                                .putBoolean("shadow", shadowEnabled)
                                .putInt("size", fontSize)
                                .putBoolean("rainbow", rainbowEnabled)
                                .putInt("opacity", opacity)
                                .putString("position", position)
                                .apply()
                            onDismiss()
                        }) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }

    // 弹窗用 WindowManager 挂 ComposeView
    fun showConfigDialog() {
        if (configDialogShown) return
        configDialogShown = true
        
        val lifecycleOwner = OverlayLifecycleOwner().also {
            currentConfigLifecycleOwner = it
        }

        val composeView = ComposeView(appContext).apply {
            currentConfigComposeView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setContent {
                MaterialTheme {
                    ConfigDialog {
                        closeConfigDialog()
                    }
                }
            }
        }

        val winParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            format = android.graphics.PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            gravity = Gravity.CENTER
        }

        try {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.addView(composeView, winParams)
        } catch (e: Exception) {
            e.printStackTrace()
            closeConfigDialog()
        }
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val unifontFamily = FontFamily(Font(R.font.unifont))
        val text = "LuminaCN${if (watermarkText.isNotBlank()) "\n$watermarkText" else ""}"

        var rainbowColor by remember { mutableStateOf(ComposeColor.White) }

        LaunchedEffect(rainbowEnabled) {
            if (rainbowEnabled) {
                while (isActive) {
                    val hue = (System.currentTimeMillis() % 3600L) / 10f
                    rainbowColor = ComposeColor.hsv(hue, 1f, 1f)
                    delay(50L)
                }
            }
        }

        val baseColor = if (rainbowEnabled) rainbowColor else ComposeColor(textColor)
        val finalColor = baseColor.copy(alpha = opacity / 100f)

        // 根据位置设置不同的对齐方式
        val (alignment, shadowOffset) = when (position) {
            "左上" -> Pair(Alignment.TopStart, 3.dp)
            "右上" -> Pair(Alignment.TopEnd, 3.dp)
            "左下" -> Pair(Alignment.BottomStart, 3.dp)
            "右下" -> Pair(Alignment.BottomEnd, 3.dp)
            else -> Pair(Alignment.Center, 1.dp) // 居中
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = alignment
        ) {
            // 阴影效果
            if (shadowEnabled) {
                Text(
                    text = text,
                    fontSize = fontSize.sp,
                    fontFamily = unifontFamily,
                    color = ComposeColor.Black.copy(alpha = 0.15f),
                    textAlign = TextAlign.Center,
                    lineHeight = (fontSize * 1.5).sp,
                    letterSpacing = (fontSize * 0.1).sp,
                    modifier = Modifier.offset(x = shadowOffset, y = shadowOffset)
                )
            }

            // 主文本
            Text(
                text = text,
                fontSize = fontSize.sp,
                fontFamily = unifontFamily,
                color = finalColor,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize * 1.2).sp,
                letterSpacing = (fontSize * 0.1).sp
            )
        }
    }
}



//Deepseek版