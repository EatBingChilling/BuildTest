// ClientOverlay.kt  最终修复版
package com.project.lumina.client.overlay.mods

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment // 添加缺失的Alignment导入
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ViewTreeLifecycleOwner // 添加ViewTreeLifecycleOwner导入
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive // 添加isActive导入

private class OverlayLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
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

    @Composable
    fun ConfigDialog(onDismiss: () -> Unit) {
        var localText by remember { mutableStateOf(watermarkText) }
        var localRed by remember { mutableStateOf(Color.red(textColor)) }
        var localGreen by remember { mutableStateOf(Color.green(textColor)) }
        var localBlue by remember { mutableStateOf(Color.blue(textColor)) }
        var localShadow by remember { mutableStateOf(shadowEnabled) }
        var localSize by remember { mutableStateOf(fontSize - 5) }
        var localRain by remember { mutableStateOf(rainbowEnabled) }
        var localAlpha by remember { mutableStateOf(opacity) }

        val localColor = Color.rgb(localRed, localGreen, localBlue)

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("配置水印", style = MaterialTheme.typography.headlineSmall)

                    OutlinedTextField(
                        value = localText,
                        onValueChange = { localText = it },
                        label = { Text("水印文字") },
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("颜色预览")
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(ComposeColor(localColor), MaterialTheme.shapes.medium)
                        )
                    }

                    listOf("红" to localRed, "绿" to localGreen, "蓝" to localBlue).forEachIndexed { idx, (label, value) ->
                        Column {
                            Text("$label: $value")
                            Slider(
                                value = value.toFloat(),
                                onValueChange = {
                                    when (idx) {
                                        0 -> localRed = it.toInt()
                                        1 -> localGreen = it.toInt()
                                        2 -> localBlue = it.toInt()
                                    }
                                },
                                valueRange = 0f..255f,
                                steps = 255
                            )
                        }
                    }

                    Column {
                        Text("字体大小: ${localSize + 5}")
                        Slider(
                            value = localSize.toFloat(),
                            onValueChange = { localSize = it.toInt() },
                            valueRange = 0f..295f,
                            steps = 295
                        )
                    }

                    Column {
                        Text("透明度: $localAlpha%")
                        Slider(
                            value = localAlpha.toFloat(),
                            onValueChange = { localAlpha = it.toInt() },
                            valueRange = 0f..100f,
                            steps = 100
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("阴影")
                        Switch(checked = localShadow, onCheckedChange = { localShadow = it })
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("彩虹")
                        Switch(checked = localRain, onCheckedChange = { localRain = it })
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            watermarkText = localText
                            textColor = localColor
                            shadowEnabled = localShadow
                            fontSize = localSize + 5
                            rainbowEnabled = localRain
                            opacity = localAlpha

                            prefs.edit()
                                .putString("text", watermarkText)
                                .putInt("color", textColor)
                                .putBoolean("shadow", shadowEnabled)
                                .putInt("size", fontSize)
                                .putBoolean("rainbow", rainbowEnabled)
                                .putInt("opacity", opacity)
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

    fun showConfigDialog() {
        val lifecycleOwner = OverlayLifecycleOwner()
        val composeView = ComposeView(appContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            // 使用ViewTreeLifecycleOwner设置生命周期所有者
            ViewTreeLifecycleOwner.set(this, lifecycleOwner)
            
            setContent {
                MaterialTheme {
                    ConfigDialog {
                        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                        wm.removeView(this)
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
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            gravity = Gravity.CENTER
        }

        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.addView(composeView, winParams)
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val unifontFamily = FontFamily(Font(R.font.unifont))
        val text = "LuminaCN${if (watermarkText.isNotBlank()) "\n$watermarkText" else ""}"

        var rainbowColor by remember { mutableStateOf(ComposeColor.White) }

        LaunchedEffect(rainbowEnabled) {
            try {
                if (rainbowEnabled) {
                    // 使用导入了的isActive
                    while (isActive) {
                        val hue = (System.currentTimeMillis() % 3600L) / 10f
                        rainbowColor = ComposeColor.hsv(hue, 1f, 1f)
                        delay(50L)
                    }
                }
            } catch (e: CancellationException) {
                // 正常取消，无需处理
            }
        }

        val baseColor = if (rainbowEnabled) rainbowColor else ComposeColor(textColor)
        val finalColor = baseColor.copy(alpha = opacity / 100f)

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
                    fontFamily = unifontFamily,
                    color = ComposeColor.Black.copy(alpha = 0.15f),
                    textAlign = TextAlign.Center,
                    lineHeight = (fontSize * 1.5).sp,
                    letterSpacing = (fontSize * 0.1).sp,
                    modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                )
            }

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
