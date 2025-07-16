package com.project.lumina.client.overlay.mods

import android.app.AlertDialog
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
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

    /* -------------------- 配置对话框 -------------------- */
    fun showConfigDialog() {
        if (!Settings.canDrawOverlays(appContext)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${appContext.packageName}")
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            appContext.startActivity(intent)
            return
        }

        val dialogContext = android.view.ContextThemeWrapper(
            appContext,
            android.R.style.Theme_Material_Dialog_Alert
        )

        val dialog = AlertDialog.Builder(dialogContext)
            .setTitle("配置水印")
            .setView(ComposeView(dialogContext).apply {
                setContent {
                    MaterialTheme {
                        ConfigDialogContent(
                            watermarkText = watermarkText,
                            textColor = textColor,
                            shadowEnabled = shadowEnabled,
                            fontSize = fontSize,
                            rainbowEnabled = rainbowEnabled,
                            alphaValue = alphaValue,
                            useUnifont = useUnifont,
                            onWatermarkTextChanged = { watermarkText = it },
                            onTextColorChanged = { textColor = it },
                            onShadowEnabledChanged = { shadowEnabled = it },
                            onFontSizeChanged = { fontSize = it },
                            onRainbowEnabledChanged = { rainbowEnabled = it },
                            onAlphaValueChanged = { alphaValue = it },
                            onUseUnifontChanged = { useUnifont = it }
                        )
                    }
                }
            })
            .setPositiveButton("确定") { _, _ ->
                prefs.edit()
                    .putString("text", watermarkText)
                    .putInt("color", textColor)
                    .putBoolean("shadow", shadowEnabled)
                    .putInt("size", fontSize)
                    .putBoolean("rainbow", rainbowEnabled)
                    .putInt("alpha", alphaValue)
                    .putBoolean("use_unifont", useUnifont)
                    .apply()
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.window?.apply {
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            addFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            )
            clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            attributes = attributes.apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.CENTER
            }
        }
        dialog.show()
    }

    /* -------------------- 水印内容 -------------------- */
    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val text = "LuminaCN${if (watermarkText.isNotBlank()) "\n$watermarkText" else ""}"

        val unifontFamily = FontFamily(Font(com.project.lumina.R.font.unifont))
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

/* -------------------- 配置对话框 Composable -------------------- */
@Composable
fun ConfigDialogContent(
    watermarkText: String,
    textColor: Int,
    shadowEnabled: Boolean,
    fontSize: Int,
    rainbowEnabled: Boolean,
    alphaValue: Int,
    useUnifont: Boolean,
    onWatermarkTextChanged: (String) -> Unit,
    onTextColorChanged: (Int) -> Unit,
    onShadowEnabledChanged: (Boolean) -> Unit,
    onFontSizeChanged: (Int) -> Unit,
    onRainbowEnabledChanged: (Boolean) -> Unit,
    onAlphaValueChanged: (Int) -> Unit,
    onUseUnifontChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = watermarkText,
            onValueChange = onWatermarkTextChanged,
            label = { Text("水印文字") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("文字颜色")
        ColorSlider(
            color = textColor,
            onColorChanged = onTextColorChanged
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("阴影效果")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = shadowEnabled, onCheckedChange = onShadowEnabledChanged)
        }

        Text("字体大小: $fontSize")
        Slider(
            value = fontSize.toFloat(),
            onValueChange = { onFontSizeChanged(it.toInt()) },
            valueRange = 5f..300f,
            steps = 295
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("彩虹效果")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = rainbowEnabled, onCheckedChange = onRainbowEnabledChanged)
        }

        Text("透明度: $alphaValue%")
        Slider(
            value = alphaValue.toFloat(),
            onValueChange = { onAlphaValueChanged(it.toInt()) },
            valueRange = 0f..100f,
            steps = 100
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("使用Unifont字体")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = useUnifont, onCheckedChange = onUseUnifontChanged)
        }
    }
}

/* -------------------- RGB 滑条 -------------------- */
@Composable
fun ColorSlider(color: Int, onColorChanged: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R:")
            Slider(
                value = Color.red(color).toFloat() / 255f,
                onValueChange = {
                    val newColor = Color.rgb(
                        (it * 255).toInt(),
                        Color.green(color),
                        Color.blue(color)
                    )
                    onColorChanged(newColor)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G:")
            Slider(
                value = Color.green(color).toFloat() / 255f,
                onValueChange = {
                    val newColor = Color.rgb(
                        Color.red(color),
                        (it * 255).toInt(),
                        Color.blue(color)
                    )
                    onColorChanged(newColor)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B:")
            Slider(
                value = Color.blue(color).toFloat() / 255f,
                onValueChange = {
                    val newColor = Color.rgb(
                        Color.red(color),
                        Color.green(color),
                        (it * 255).toInt()
                    )
                    onColorChanged(newColor)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
