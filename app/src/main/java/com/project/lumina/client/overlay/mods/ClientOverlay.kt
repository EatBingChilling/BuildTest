package com.project.lumina.client.overlay.mods

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.*
import android.widget.*
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.*

class ClientOverlay : OverlayWindow() {

    /* ------------ 水印参数 ------------ */
    private val prefs: SharedPreferences =
        OverlayManager.currentContext!!.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

    private var watermarkText  = prefs.getString("text", "") ?: ""
    private var textColor      = prefs.getInt("color", Color.WHITE)
    private var shadowEnabled  = prefs.getBoolean("shadow", false)
    private var fontSize       = prefs.getInt("size", 28).coerceIn(5, 300)
    private var rainbowEnabled = prefs.getBoolean("rainbow", false)
    private var alphaValue     = prefs.getInt("alpha", 25).coerceIn(0, 100)

    /* ------------ 真正的水印 View ------------ */
    private val textView: TextView by lazy {
        TextView(OverlayManager.currentContext).apply {
            text = watermarkText
            setTextColor(textColor)
            textSize = fontSize.toFloat()
            alpha = alphaValue / 100f
            gravity = Gravity.CENTER
            setShadowLayer(if (shadowEnabled) 4f else 0f, 2f, 2f, Color.BLACK)
        }
    }

    /* ------------ Compose 占位（什么也不画） ------------ */
    @androidx.compose.runtime.Composable
    override fun Content() { /* 空着，不用 Compose */ }

    /* ------------ 生命周期钩子 ------------ */
    override fun onCreate() {
        super.onCreate()
        // 把 TextView 塞进 composeView，这样框架会把它加进窗口
        composeView.addView(textView)

        if (rainbowEnabled) {
            CoroutineScope(Dispatchers.Main).launch {
                while (true) {
                    textView.setTextColor(hsvRainbow())
                    delay(200)
                }
            }
        }
    }

    private fun hsvRainbow(): Int {
        val hue = (System.currentTimeMillis() / 40) % 360
        return Color.HSVToColor(floatArrayOf(hue.toFloat(), 1f, 1f))
    }

    /* ------------ 静态接口 ------------ */
    companion object {
        private var overlayInstance: ClientOverlay? = null
        private var shouldShowOverlay = true

        fun showOverlay() {
            if (!Settings.canDrawOverlays(OverlayManager.currentContext)) return
            if (shouldShowOverlay) {
                overlayInstance = ClientOverlay()
                OverlayManager.showOverlayWindow(overlayInstance!!)
            }
        }

        fun dismissOverlay() {
            overlayInstance?.let { OverlayManager.dismissOverlayWindow(it) }
            overlayInstance = null
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (!enabled) dismissOverlay()
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay

        /* ------------ 传统 AlertDialog 配置 ------------ */
        private var configDialog: AlertDialog? = null

        fun showConfigDialog() {
            val ctx = OverlayManager.currentContext ?: return
            if (!Settings.canDrawOverlays(ctx)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${ctx.packageName}")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                ctx.startActivity(intent)
                return
            }

            if (configDialog?.isShowing == true) return

            val prefs = ctx.getSharedPreferences("lumina_overlay_prefs", Context.MODE_PRIVATE)

            var watermarkText  = prefs.getString("text", "") ?: ""
            var textColor      = prefs.getInt("color", Color.WHITE)
            var shadowEnabled  = prefs.getBoolean("shadow", false)
            var fontSize       = prefs.getInt("size", 28).coerceIn(5, 300)
            var rainbowEnabled = prefs.getBoolean("rainbow", false)
            var alphaValue     = prefs.getInt("alpha", 25).coerceIn(0, 100)

            val scroll = ScrollView(ctx)
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
            }

            val etText = EditText(ctx).apply { setText(watermarkText); hint = "水印文字" }
            col.addView(etText)

            fun addSeek(label: String, init: Int, max: Int, set: (Int) -> Unit) {
                col.addView(TextView(ctx).apply { text = label })
                col.addView(SeekBar(ctx).apply {
                    this.max = max
                    progress = init
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) =
                            set(p)
                        override fun onStartTrackingTouch(sb: SeekBar?) {}
                        override fun onStopTrackingTouch(sb: SeekBar?) {}
                    })
                })
            }

            addSeek("R", Color.red(textColor), 255) { textColor = Color.rgb(it, Color.green(textColor), Color.blue(textColor)) }
            addSeek("G", Color.green(textColor), 255) { textColor = Color.rgb(Color.red(textColor), it, Color.blue(textColor)) }
            addSeek("B", Color.blue(textColor), 255) { textColor = Color.rgb(Color.red(textColor), Color.green(textColor), it) }

            fun addSwitch(label: String, init: Boolean, set: (Boolean) -> Unit) {
                val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                row.addView(TextView(ctx).apply { text = label })
                row.addView(Space(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
                row.addView(Switch(ctx).apply { isChecked = init; setOnCheckedChangeListener { _, b -> set(b) } })
                col.addView(row)
            }

            addSwitch("阴影", shadowEnabled) { shadowEnabled = it }
            addSwitch("彩虹", rainbowEnabled) { rainbowEnabled = it }

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
                        .apply()
                    configDialog = null
                    dismissOverlay()
                    showOverlay()
                }
                .setNegativeButton("取消") { _, _ -> configDialog = null }
                .setOnDismissListener { configDialog = null }
                .create()
                .apply {
                    window?.setType(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        else
                            WindowManager.LayoutParams.TYPE_PHONE
                    )
                    show()
                }
        }
    }
}
