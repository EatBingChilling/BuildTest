package com.project.lumina.client.overlay.mods

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay

class ClientOverlay : OverlayWindow() {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

    // State for the watermark properties
    private var watermarkText by mutableStateOf(prefs.getString("text", "") ?: "")
    private var textColor by mutableStateOf(prefs.getInt("color", Color.WHITE))
    private var shadowEnabled by mutableStateOf(prefs.getBoolean("shadow", false))
    private var fontSize by mutableStateOf(prefs.getInt("size", 28).coerceIn(5, 300))
    private var rainbowEnabled by mutableStateOf(prefs.getBoolean("rainbow", false))
    private var alphaValue by mutableStateOf(prefs.getInt("alpha", 25).coerceIn(0, 100))
    private var useUnifont by mutableStateOf(prefs.getBoolean("use_unifont", true))

    // State to control dialog visibility
    private var showDialog by mutableStateOf(false)


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

    fun showConfigDialog() {
        showDialog = true
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        // --- Watermark Display Logic (Unchanged) ---
        val text = "LuminaCN${if (watermarkText.isNotBlank()) "\n$watermarkText" else ""}"

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
                    modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                )
            }
            Text(
                text = text,
                fontSize = fontSize.sp,
                color = finalColor,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize * 1.2).sp
            )
        }

        // --- Material Design 3 AlertDialog ---
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("配置水印") },
                text = {
                    DialogConfigurationUI(
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
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Save preferences and dismiss
                            prefs.edit()
                                .putString("text", watermarkText)
                                .putInt("color", textColor)
                                .putBoolean("shadow", shadowEnabled)
                                .putInt("size", fontSize)
                                .putBoolean("rainbow", rainbowEnabled)
                                .putInt("alpha", alphaValue)
                                .putBoolean("use_unifont", useUnifont)
                                .apply()
                            showDialog = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun DialogConfigurationUI(
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
    onUseUnifontChanged: (Boolean) -> Unit,
) {
    // Add vertical scroll for small screens
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = watermarkText,
            onValueChange = onWatermarkTextChanged,
            label = { Text("水印文字") },
            modifier = Modifier.fillMaxWidth()
        )

        ColorSlider(color = textColor, onColorChanged = onTextColorChanged)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("阴影效果", modifier = Modifier.weight(1f))
            Switch(checked = shadowEnabled, onCheckedChange = onShadowEnabledChanged)
        }

        Column {
            Text("字体大小: $fontSize")
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { onFontSizeChanged(it.toInt()) },
                valueRange = 5f..300f,
                steps = 294 // (300 - 5) - 1
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("彩虹效果", modifier = Modifier.weight(1f))
            Switch(checked = rainbowEnabled, onCheckedChange = onRainbowEnabledChanged)
        }

        Column {
            Text("透明度: $alphaValue%")
            Slider(
                value = alphaValue.toFloat(),
                onValueChange = { onAlphaValueChanged(it.toInt()) },
                valueRange = 0f..100f,
                steps = 99 // 101 steps
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("使用Unifont字体", modifier = Modifier.weight(1f))
            Switch(checked = useUnifont, onCheckedChange = onUseUnifontChanged)
        }
    }
}


@Composable
fun ColorSlider(color: Int, onColorChanged: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("文字颜色")
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R", color = ComposeColor.Red)
            Slider(
                value = red.toFloat(),
                onValueChange = { onColorChanged(Color.rgb(it.toInt(), green, blue)) },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G", color = ComposeColor.Green)
            Slider(
                value = green.toFloat(),
                onValueChange = { onColorChanged(Color.rgb(red, it.toInt(), blue)) },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B", color = ComposeColor.Blue)
            Slider(
                value = blue.toFloat(),
                onValueChange = { onColorChanged(Color.rgb(red, green, it.toInt())) },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
        }
    }
}
