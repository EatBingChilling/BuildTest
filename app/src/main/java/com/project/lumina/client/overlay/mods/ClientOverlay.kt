package com.project.lumina.client.overlay.mods

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.*
import android.widget.*
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.*

/**
 * 水印悬浮窗，完全移除 Compose。
 */
class ClientOverlay : OverlayWindow() {

    /* ---------------- 水印参数 ---------------- */
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

    private var watermarkText  = prefs.getString("text", "") ?: ""
    private var textColor      = prefs.getInt("color", Color.WHITE)
    private var shadowEnabled  = prefs.getBoolean("shadow", false)
    private var fontSize       = prefs.getInt("size", 28).coerceIn(5, 300)
    private var rainbowEnabled = prefs.getBoolean("rainbow", false)
    private var alphaValue     = prefs.getInt("alpha", 25).coerceIn(0, 100)
    private var useUnifont     = prefs.getBoolean("use_unifont", true)

    /* ---------------- 布局参数 ---------------- */
    override val layoutParams: WindowManager.LayoutParams =
        WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width  = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
            format = PixelFormat.TRANSLUCENT
        }

    /* ---------------- 实际 View ---------------- */
    private lateinit var textView: TextView

    /**
     * 必须实现的抽象方法：返回要添加到窗口的 View。
     * 这里我们返回一个 TextView 作为水印。
     */
    override fun onCreateView(): View {
        textView = TextView(context).apply {
            text = watermarkText
            setTextColor(textColor)
            textSize = fontSize.toFloat()
            alpha = alphaValue / 100f
            gravity = Gravity.CENTER
            setShadowLayer(if (shadowEnabled) 4f else 0f, 2f, 2f, Color.BLACK)
        }

        /* 彩虹循环 */
        if (rainbowEnabled) {
            CoroutineScope(Dispatchers.Main).launch {
                while (isAttachedToWindow) {
                    textView.setTextColor(hsvRainbow())
                    delay(200)
                }
            }
        }
        return textView
    }

    /* ---------------- 工具函数 ---------------- */
    private fun hsvRainbow(): Int {
        val hue = (System.currentTimeMillis() / 40) % 360
        return Color.HSVToColor(floatArrayOf(hue.toFloat(), 1f, 1f))
    }

    /* ---------------- 静态接口 ---------------- */
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
                kotlin.runCatching { OverlayManager.showOverlayWindow(overlayInstance!!) }
            }
        }

        fun dismissOverlay() {
            kotlin.runCatching {
                overlayInstance?.let { OverlayManager.dismissOverlayWindow(it) }
            }
            overlayInstance = null
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (!enabled) dismissOverlay()
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        /* ------------ 配置对话框（传统 AlertDialog） ------------ */
        private var configDialog: AlertDialog? = null

        fun showConfigDialog() {
            if (!Settings.canDrawOverlays(appContext)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${appContext.packageName}")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                appContext.startActivity(intent)
                return
            }

            if (configDialog?.isShowing == true) return

            val prefs = appContext.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

            /* 当前值 */
            var watermarkText  = prefs.getString("text", "") ?: ""
            var textColor      = prefs.getInt("color", Color.WHITE)
            var shadowEnabled  = prefs.getBoolean("shadow", false)
            var fontSize       = prefs.getInt("size", 28).coerceIn(5, 300)
            var rainbowEnabled = prefs.getBoolean("rainbow", false)
            var alphaValue     = prefs.getInt("alpha", 25).coerceIn(0, 100)
            var useUnifont     = prefs.getBoolean("use_unifont", true)

            val ctx = appContext
            val scroll = ScrollView(ctx)
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
            }

            /* 水印文字 */
            val etText = EditText(ctx).apply { setText(watermarkText); hint = "水印文字" }
            col.addView(etText)

            /* 颜色滑条 */
            fun addSeek(label: String, init: Int, max: Int, set: (Int) -> Unit) {
                col.addView(TextView(ctx).apply { text = label })
                col.addView(SeekBar(ctx).apply {
                    this.max = max
                    progress = init
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) = set(p)
                        override fun onStartTrackingTouch(sb: SeekBar?) {}
                        override fun onStopTrackingTouch(sb: SeekBar?) {}
                    })
                })
            }
            addSeek("R", Color.red(textColor), 255) { textColor = Color.rgb(it, Color.green(textColor), Color.blue(textColor)) }
            addSeek("G", Color.green(textColor), 255) { textColor = Color.rgb(Color.red(textColor), it, Color.blue(textColor)) }
            addSeek("B", Color.blue(textColor), 255) { textColor = Color.rgb(Color.red(textColor), Color.green(textColor), it) }

            /* 开关 */
            fun addSwitch(label: String, init: Boolean, set: (Boolean) -> Unit) {
                val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                row.addView(TextView(ctx).apply { text = label })
                row.addView(Space(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
                row.addView(Switch(ctx).apply { isChecked = init; setOnCheckedChangeListener { _, b -> set(b) } })
                col.addView(row)
            }
            addSwitch("阴影", shadowEnabled) { shadowEnabled = it }
            addSwitch("彩虹", rainbowEnabled) { rainbowEnabled = it }
            addSwitch("Unifont", useUnifont) { useUnifont = it }

            /* 滑条：字体大小 & 透明度 */
            addSeek("字体大小", fontSize, 300) { fontSize = it.coerceIn(5, 300) }
            addSeek("透明度 %", alphaValue, 100) { alphaValue = it.coerceIn(0, 100) }

            scroll.addView(col)

            configDialog = AlertDialog.Builder(ctx)
                .setTitle("配置水印")
                .setView(scroll)
                .setPositiveButton("保存") { _, _ ->
                    prefs.edit()
                        .putString("text", etText.text.toString())
                        .putInt("color", textColor)
                        .putBoolean("shadow", shadowEnabled)
                        .putInt("size", fontSize)
                        .putBoolean("rainbow", rainbowEnabled)
                        .putInt("alpha", alphaValue)
                        .putBoolean("use_unifont", useUnifont)
                        .apply()
                    configDialog = null
                    /* 重启水印以应用配置 */
                    dismissOverlay()
                    showOverlay()
                }
                .setNegativeButton("取消") { _, _ -> configDialog = null }
                .setOnDismissListener { configDialog = null }
                .create()

            /* 关键点：把 Dialog 的窗口声明成悬浮窗类型 */
            configDialog?.window?.setType(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE
            )
            configDialog?.show()
        }
    }
}
