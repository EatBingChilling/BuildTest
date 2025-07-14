package com.project.lumina.client.overlay.mods

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.R
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay

class ClientOverlay : OverlayWindow() {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

    private var watermarkText   by mutableStateOf(prefs.getString("text", "") ?: "")
    private var textColor       by mutableStateOf(prefs.getInt("color", Color.WHITE))
    private var shadowEnabled   by mutableStateOf(prefs.getBoolean("shadow", false))
    private var fontSize        by mutableStateOf(prefs.getInt("size", 28).coerceIn(5, 300))
    private var rainbowEnabled  by mutableStateOf(prefs.getBoolean("rainbow", false))
    // 新增：透明度 0-100
    private var alphaValue      by mutableStateOf(prefs.getInt("alpha", 25).coerceIn(0, 100))
    // 新增：是否使用 Unifont
    private var useUnifont      by mutableStateOf(prefs.getBoolean("use_unifont", true))

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width  = WindowManager.LayoutParams.MATCH_PARENT
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
        try {
            val dialogView = LayoutInflater.from(appContext)
                .inflate(R.layout.overlay_config_dialog, null)

            val editText   = dialogView.findViewById<EditText>(R.id.editText)
            val seekRed    = dialogView.findViewById<SeekBar>(R.id.seekRed)
            val seekGreen  = dialogView.findViewById<SeekBar>(R.id.seekGreen)
            val seekBlue   = dialogView.findViewById<SeekBar>(R.id.seekBlue)
            val switchShadow = dialogView.findViewById<Switch>(R.id.switchShadow)
            val seekSize   = dialogView.findViewById<SeekBar>(R.id.seekSize).apply { max = 295 }

            val switchRainbow = dialogView.findViewById<Switch>(R.id.switchRainbow)
            val colorPreview  = dialogView.findViewById<TextView>(R.id.colorPreview)

            // 新增
            val seekAlpha     = dialogView.findViewById<SeekBar>(R.id.seekAlpha).apply { max = 100 }
            val textAlpha     = dialogView.findViewById<TextView>(R.id.textAlpha)
            val switchUnifont = dialogView.findViewById<Switch>(R.id.switchUseUnifont)

            // 原读取
            editText.setText(watermarkText)
            seekRed.progress   = Color.red(textColor)
            seekGreen.progress = Color.green(textColor)
            seekBlue.progress  = Color.blue(textColor)
            switchShadow.isChecked = shadowEnabled
            seekSize.progress  = fontSize - 5
            switchRainbow.isChecked = rainbowEnabled

            // 新增读取
            seekAlpha.progress = alphaValue
            textAlpha.text     = "透明度: $alphaValue%"
            switchUnifont.isChecked = useUnifont

            fun updateColorPreview() {
                val color = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
                colorPreview.setBackgroundColor(color)
            }
            updateColorPreview()

            seekRed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, from: Boolean) = updateColorPreview()
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
            seekGreen.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, from: Boolean) = updateColorPreview()
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
            seekBlue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, from: Boolean) = updateColorPreview()
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
            seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, from: Boolean) {
                    textAlpha.text = "透明度: $p%"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            // 创建包装上下文以支持 Dialog 主题
            val dialogContext = android.view.ContextThemeWrapper(
                appContext, 
                android.R.style.Theme_Material_Dialog_Alert
            )

            val dialog = AlertDialog.Builder(dialogContext)
                .setTitle("配置水印")
                .setView(dialogView)
                .setPositiveButton("确定") { _, _ ->
                    watermarkText = editText.text.toString()
                    textColor = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
                    shadowEnabled = switchShadow.isChecked
                    fontSize       = seekSize.progress + 5
                    rainbowEnabled = switchRainbow.isChecked
                    alphaValue     = seekAlpha.progress
                    useUnifont     = switchUnifont.isChecked

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

            // 设置窗口参数
            dialog.window?.let { window ->
                window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                
                // 设置窗口标志，确保可以获得焦点和触摸
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                )
                
                // 清除可能阻止显示的标志
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
                
                // 设置窗口属性
                val params = window.attributes
                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.CENTER
                window.attributes = params
            }

            dialog.show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            
            // 如果上述方法失败，尝试简化版本
            try {
                val builder = AlertDialog.Builder(appContext)
                builder.setTitle("配置水印")
                builder.setMessage("配置对话框加载失败，请检查布局文件")
                builder.setPositiveButton("确定", null)
                
                val fallbackDialog = builder.create()
                fallbackDialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                fallbackDialog.show()
            } catch (fallbackException: Exception) {
                fallbackException.printStackTrace()
            }
        }
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val fontFamily = if (useUnifont)
            FontFamily(Font(R.font.unifont))
        else
            FontFamily.Default

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
                    fontFamily = fontFamily,
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
                fontFamily = fontFamily,
                color = finalColor,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize * 1.2).sp,
                letterSpacing = (fontSize * 0.1).sp
            )
        }
    }
}