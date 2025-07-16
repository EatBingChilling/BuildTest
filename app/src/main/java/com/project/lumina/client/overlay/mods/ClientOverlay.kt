// 文件：app/src/main/java/com/project/lumina/client/overlay/mods/ClientOverlay.kt
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.project.lumina.client.R
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay

class ClientOverlay : OverlayWindow(), LifecycleOwner {

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
    override val layoutParams: WindowManager.LayoutParams get() = _layoutParams

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

        fun showConfigDialog() = overlayInstance?.showConfigDialog()
    }

    /* -------------------- 配置弹窗 -------------------- */
    fun showConfigDialog() {
        if (!Settings.canDrawOverlays(appContext)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${appContext.packageName}")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            appContext.startActivity(intent)
            return
        }

        DialogComposeLifecycleOwner().use { owner ->
            val dialog = ComposeView(appContext).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    MaterialTheme(
                        colorScheme = dynamicColorSchemeWithFallback()   // M3 动态配色
                    ) {
                        ConfigDialog()
                    }
                }
            }

            val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.CENTER
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            }
            windowManager.addView(dialog, params)
        }
    }

    /* -------------------- 水印内容 -------------------- */
    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val text = "LuminaCN${if (watermarkText.isNotBlank()) "\n$watermarkText" else ""}"
        val unifontFamily = FontFamily(Font(resId = R.font.unifont))
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

    /* -------------------- 配置内容 -------------------- */
    @Composable
    private fun ConfigDialog() {
        var open by remember { mutableStateOf(true) }
        if (!open) return

        AlertDialog(
            onDismissRequest = { open = false },
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
                        Switch(checked = shadowEnabled, onCheckedChange = { shadowEnabled = it })
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
                        Switch(checked = rainbowEnabled, onCheckedChange = { rainbowEnabled = it })
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
                        Switch(checked = useUnifont, onCheckedChange = { useUnifont = it })
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
                    open = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("取消") }
            }
        )
    }
}

/* -------------------- 颜色滑条 -------------------- */
@Composable
private fun ColorSlider(color: Int, onColorChanged: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R")
            Slider(
                value = Color.red(color).toFloat() / 255f,
                onValueChange = {
                    onColorChanged(
                        Color.rgb((it * 255).toInt(), Color.green(color), Color.blue(color))
                    )
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G")
            Slider(
                value = Color.green(color).toFloat() / 255f,
                onValueChange = {
                    onColorChanged(
                        Color.rgb(Color.red(color), (it * 255).toInt(), Color.blue(color))
                    )
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B")
            Slider(
                value = Color.blue(color).toFloat() / 255f,
                onValueChange = {
                    onColorChanged(
                        Color.rgb(Color.red(color), Color.green(color), (it * 255).toInt())
                    )
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/* -------------------- 动态配色 Fallback -------------------- */
@Composable
private fun dynamicColorSchemeWithFallback(): ColorScheme =
    dynamicDarkColorScheme(LocalContext.current)   // 或 dynamicLightColorScheme

/* -------------------- Compose 弹窗 LifecycleOwner -------------------- */
private class DialogComposeLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    fun attachToWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun detachFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = store
    override val savedStateRegistry: SavedStateRegistry = controller.savedStateRegistry
}
