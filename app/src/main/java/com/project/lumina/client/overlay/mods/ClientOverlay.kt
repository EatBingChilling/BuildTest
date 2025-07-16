package com.project.lumina.client.overlay.mods

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.project.lumina.client.R
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay

class ClientOverlay : OverlayWindow(), LifecycleOwner {

    /* -------------------- 配置持久化 -------------------- */
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

    private var watermarkText by mutableStateOf(prefs.getString("text", "") ?: "")
    private var textColor by mutableStateOf(prefs.getInt("color", Color.WHITE))
    private var shadowEnabled by mutableStateOf(prefs.getBoolean("shadow", false))
    private var fontSize by mutableStateOf(prefs.getInt("size", 28).coerceIn(5, 300))
    private var rainbowEnabled by mutableStateOf(prefs.getBoolean("rainbow", false))
    private var alphaValue by mutableStateOf(prefs.getInt("alpha", 25).coerceIn(0, 100))
    private var useUnifont by mutableStateOf(prefs.getBoolean("use_unifont", true))

    /* -------------------- 生命周期 -------------------- */
    private val _lifecycleRegistry = LifecycleRegistry(this).apply {
        currentState = Lifecycle.State.STARTED
    }
    override val lifecycle: Lifecycle = _lifecycleRegistry

    /* -------------------- 布局参数 -------------------- */
    override val layoutParams: WindowManager.LayoutParams =
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

    /* -------------------- 伴生对象 -------------------- */
    companion object {
        private var overlayInstance: ClientOverlay? = null
        private var shouldShowOverlay = true

        private val appContext: Context by lazy {
            val activityThread = Class.forName("android.app.ActivityThread")
            val method = activityThread.getMethod("currentApplication")
            method.invoke(null) as Application
        }

        fun showOverlay() {
            if (!Settings.canDrawOverlays(appContext)) return
            if (shouldShowOverlay) {
                overlayInstance = ClientOverlay()
                runCatching { OverlayManager.showOverlayWindow(overlayInstance!!) }
            }
        }

        fun dismissOverlay() {
            runCatching {
                overlayInstance?.let {
                    it._lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
                    OverlayManager.dismissOverlayWindow(it)
                }
            }
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (!enabled) dismissOverlay()
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        /* 配置弹窗 */
        private var configWindow: ConfigDialogWindow? = null
        fun showConfigDialog() {
            if (!Settings.canDrawOverlays(appContext)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${appContext.packageName}")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                appContext.startActivity(intent)
                return
            }
            if (configWindow == null) configWindow = ConfigDialogWindow()
            OverlayManager.showOverlayWindow(configWindow!!)
        }
    }

    /* -------------------- 水印内容 -------------------- */
    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val text = "LuminaCN${if (watermarkText.isNotBlank()) "\n$watermarkText" else ""}"
        val unifontFamily = FontFamily(Font(resId = R.font.packet))
        val defaultFamily = FontFamily.Default

        var rainbowColor by remember { mutableStateOf(ComposeColor.White) }
        LaunchedEffect(rainbowEnabled) {
            if (rainbowEnabled) while (true) {
                val hue = (System.currentTimeMillis() % 3600L) / 10f
                rainbowColor = ComposeColor.hsv(hue, 1f, 1f)
                delay(50L)
            }
        }

        val baseColor = if (rainbowEnabled) rainbowColor else ComposeColor(textColor)
        val finalColor = baseColor.copy(alpha = alphaValue / 100f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (shadowEnabled) {
                Text(
                    text = text,
                    fontSize = fontSize.sp,
                    color = ComposeColor.Black.copy(alpha = 0.15f),
                    textAlign = TextAlign.Center,
                    lineHeight = (fontSize * 1.5).sp,
                    fontFamily = if (useUnifont) unifontFamily else defaultFamily,
                    modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                )
            }
            Text(
                text = text,
                fontSize = fontSize.sp,
                color = finalColor,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize * 1.2).sp,
                fontFamily = if (useUnifont) unifontFamily else defaultFamily
            )
        }
    }
}

/* =========================================================================
 *  以下为跨类共享的 @Composable 函数与弹窗实现
 * ========================================================================= */

/**
 * 通用颜色滑条组件（顶层函数，任何地方都可调用）
 */
@Composable
fun ColorSlider(color: Int, onColorChanged: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R")
            Slider(
                value = Color.red(color) / 255f,
                onValueChange = {
                    onColorChanged(
                        Color.rgb(
                            (it * 255).toInt(),
                            Color.green(color),
                            Color.blue(color)
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                valueRange = 0f..1f
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G")
            Slider(
                value = Color.green(color) / 255f,
                onValueChange = {
                    onColorChanged(
                        Color.rgb(
                            Color.red(color),
                            (it * 255).toInt(),
                            Color.blue(color)
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                valueRange = 0f..1f
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B")
            Slider(
                value = Color.blue(color) / 255f,
                onValueChange = {
                    onColorChanged(
                        Color.rgb(
                            Color.red(color),
                            Color.green(color),
                            (it * 255).toInt()
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                valueRange = 0f..1f
            )
        }
    }
}

/**
 * 配置弹窗悬浮窗（独立 OverlayWindow）
 */
private class ConfigDialogWindow : OverlayWindow() {
    override val layoutParams: WindowManager.LayoutParams =
        WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = android.graphics.PixelFormat.TRANSLUCENT
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER
        }

    @Composable
    override fun Content() {
        val ctx = LocalContext.current
        val prefs = ctx.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

        var watermarkText by remember {
            mutableStateOf(prefs.getString("text", "") ?: "")
        }
        var textColor by remember {
            mutableStateOf(prefs.getInt("color", Color.WHITE))
        }
        var shadowEnabled by remember {
            mutableStateOf(prefs.getBoolean("shadow", false))
        }
        var fontSize by remember {
            mutableStateOf(prefs.getInt("size", 28).coerceIn(5, 300))
        }
        var rainbowEnabled by remember {
            mutableStateOf(prefs.getBoolean("rainbow", false))
        }
        var alphaValue by remember {
            mutableStateOf(prefs.getInt("alpha", 25).coerceIn(0, 100))
        }
        var useUnifont by remember {
            mutableStateOf(prefs.getBoolean("use_unifont", true))
        }

        MaterialTheme {
            AlertDialog(
                onDismissRequest = { OverlayManager.dismissOverlayWindow(this) },
                title = { Text("配置水印", style = MaterialTheme.typography.headlineSmall) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("水印文字") },
                            singleLine = true
                        )

                        Text("文字颜色")
                        ColorSlider(color = textColor, onColorChanged = { textColor = it })

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("阴影")
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = shadowEnabled,
                                onCheckedChange = { shadowEnabled = it }
                            )
                        }

                        Text("字体大小：$fontSize")
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = { fontSize = it.toInt() },
                            valueRange = 5f..300f
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("彩虹")
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = rainbowEnabled,
                                onCheckedChange = { rainbowEnabled = it }
                            )
                        }

                        Text("透明度：$alphaValue%")
                        Slider(
                            value = alphaValue.toFloat(),
                            onValueChange = { alphaValue = it.toInt() },
                            valueRange = 0f..100f
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Unifont")
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = useUnifont,
                                onCheckedChange = { useUnifont = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        prefs.edit()
                            .putString("text", watermarkText)
                            .putInt("color", textColor)
                            .putBoolean("shadow", shadowEnabled)
                            .putInt("size", fontSize)
                            .putBoolean("rainbow", rainbowEnabled)
                            .putInt("alpha", alphaValue)
                            .putBoolean("use_unifont", useUnifont)
                            .apply()
                        OverlayManager.dismissOverlayWindow(this)
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { OverlayManager.dismissOverlayWindow(this) }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
