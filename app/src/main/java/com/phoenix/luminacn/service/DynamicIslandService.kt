package com.phoenix.luminacn.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.phoenix.luminacn.music.MusicObserver
import com.phoenix.luminacn.phoenix.DynamicIslandState
import com.phoenix.luminacn.phoenix.DynamicIslandView
import com.phoenix.luminacn.phoenix.rememberDynamicIslandState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.math.roundToInt

class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    fun performRestore(savedState: Bundle?) { savedStateRegistryController.performRestore(savedState) }
    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
}

class DynamicIslandService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private var dynamicIslandState: DynamicIslandState? = null
    private lateinit var windowParams: WindowManager.LayoutParams
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lifecycleOwner = ServiceLifecycleOwner()

    private var isWarmedUp = mutableStateOf(false)
    private var musicModeEnabled = mutableStateOf(true)

    companion object {
        const val ACTION_UPDATE_TEXT = "com.phoenix.luminacn.ACTION_UPDATE_TEXT"
        const val ACTION_UPDATE_Y_OFFSET = "com.phoenix.luminacn.ACTION_UPDATE_Y_OFFSET"
        const val ACTION_UPDATE_SCALE = "com.phoenix.luminacn.ACTION_UPDATE_SCALE"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_Y_OFFSET_DP = "extra_y_offset_dp"
        const val EXTRA_SCALE = "extra_scale"
        const val ACTION_SHOW_NOTIFICATION_SWITCH = "com.phoenix.luminacn.ACTION_SHOW_NOTIFICATION_SWITCH"
        const val EXTRA_MODULE_NAME = "extra_module_name"
        const val EXTRA_MODULE_STATE = "extra_module_state"
        const val ACTION_SHOW_OR_UPDATE_PROGRESS = "com.phoenix.luminacn.ACTION_SHOW_OR_UPDATE_PROGRESS"
        const val EXTRA_IDENTIFIER = "extra_identifier"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_ICON_RES_ID = "extra_icon_res_id"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_PROGRESS_VALUE = "extra_progress_value"
        const val ACTION_SHOW_OR_UPDATE_MUSIC = "com.phoenix.luminacn.ACTION_SHOW_OR_UPDATE_MUSIC"
        const val ACTION_REMOVE_TASK = "com.phoenix.luminacn.ACTION_REMOVE_TASK"
        const val EXTRA_PROGRESS_TEXT = "extra_progress_text"
        const val EXTRA_IS_MAJOR_UPDATE = "extra_is_major_update"
        const val EXTRA_IMAGE_DATA = "extra_image_data"
        const val EXTRA_ALBUM_ART_DATA = "extra_album_art_data"
        const val ACTION_SET_MUSIC_MODE = "com.phoenix.luminacn.ACTION_SET_MUSIC_MODE"
        const val EXTRA_MUSIC_MODE_ENABLED = "extra_music_mode_enabled"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 读取Y轴初始位置
        val prefs = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        val initialYOffset = prefs.getFloat("dynamicIslandYOffset", 20f)

        // 检测是否是问题机型
        val isProblematicDevice = isProblematicROM()
        
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            
            // 对Android 12+和问题机型使用软件渲染
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || isProblematicDevice) {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                Log.d("DynamicIslandService", "Using software rendering for Android 12+")
            }
            
            // 确保View本身不处理任何触摸事件
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            isLongClickable = false
            
            setContent {
                val isDarkTheme = isSystemInDarkTheme()
                val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (isDarkTheme) dynamicDarkColorScheme(this@DynamicIslandService) else dynamicLightColorScheme(this@DynamicIslandService)
                } else {
                    if (isDarkTheme) darkColorScheme() else lightColorScheme()
                }

                val alpha by animateFloatAsState(targetValue = if (isWarmedUp.value) 1.0f else 0.0f, label = "warmup")

                MaterialTheme(colorScheme = colorScheme) {
                    val state = rememberDynamicIslandState()

                    LaunchedEffect(state) {
                        this@DynamicIslandService.dynamicIslandState = state
                    }

                    DynamicIslandView(
                        state = state,
                        modifier = Modifier.alpha(alpha)
                    )
                }
            }
        }

        // 关键修复：正确的flag设置
        // FLAG_NOT_FOCUSABLE - 不获取焦点
        // FLAG_NOT_TOUCHABLE - 不接收触摸，触摸会穿透到下层
        // 不要加 FLAG_NOT_TOUCH_MODAL！它是给可触摸窗口用的
        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,  // 改回MATCH_PARENT
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,  // 只用这两个flag！
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(initialYOffset)
            
            // 对于Android 12+的额外处理
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12引入了新的触摸限制，尝试通过反射绕过
                try {
                    val paramsClass = this::class.java
                    // 尝试设置私有标志
                    paramsClass.getDeclaredField("privateFlags")?.let { field ->
                        field.isAccessible = true
                        var privateFlags = field.getInt(this)
                        // 清除可能阻止触摸穿透的私有标志
                        // PRIVATE_FLAG_TRUSTED_OVERLAY = 0x20000000
                        privateFlags = privateFlags and 0x20000000.inv()
                        field.setInt(this, privateFlags)
                    }
                } catch (e: Exception) {
                    Log.w("DynamicIslandService", "Could not modify private flags: ${e.message}")
                }
            }
        }
        
        windowManager.addView(composeView, windowParams)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        loadSettings()
    }

    private fun isProblematicROM(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val miuiVersion = getSystemProperty("ro.miui.ui.version.name")
        val hyperosVersion = getSystemProperty("ro.mi.os.version.name")
        
        return when {
            // 小米/红米设备
            manufacturer.contains("xiaomi") || brand.contains("redmi") -> {
                Log.d("DynamicIslandService", "Detected Xiaomi/Redmi device")
                true
            }
            // MIUI
            !miuiVersion.isNullOrEmpty() -> {
                Log.d("DynamicIslandService", "Detected MIUI: $miuiVersion")
                true
            }
            // HyperOS/澎湃OS
            !hyperosVersion.isNullOrEmpty() -> {
                Log.d("DynamicIslandService", "Detected HyperOS: $hyperosVersion")
                true
            }
            // Android 15+
            Build.VERSION.SDK_INT >= 35 -> {
                Log.d("DynamicIslandService", "Detected Android 15+")
                true
            }
            else -> false
        }
    }
    
    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            process.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        musicModeEnabled.value = prefs.getBoolean("musicModeEnabled", true)
        if (musicModeEnabled.value) {
            startMusicObserver()
        }
    }

    private fun startMusicObserver() {
        try {
            MusicObserver.start(this)
            Log.d("DynamicIslandService", "Music observer started")
        } catch (e: Exception) {
            Log.e("DynamicIslandService", "Failed to start music observer", e)
        }
    }

    private fun stopMusicObserver() {
        try {
            MusicObserver.stop(this)
            Log.d("DynamicIslandService", "Music observer stopped")
        } catch (e: Exception) {
            Log.e("DynamicIslandService", "Failed to stop music observer", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isWarmedUp.value) {
            isWarmedUp.value = true
        }

        intent ?: return START_STICKY
        when (intent.action) {
            ACTION_UPDATE_TEXT -> intent.getStringExtra(EXTRA_TEXT)?.let { text ->
                dynamicIslandState?.updateConfig(dynamicIslandState?.scale ?: 1.0f, text)
            }

            ACTION_UPDATE_Y_OFFSET -> {
                val yOffsetDp = intent.getFloatExtra(EXTRA_Y_OFFSET_DP, 0f)
                windowParams.y = dpToPx(yOffsetDp)
                windowManager.updateViewLayout(composeView, windowParams)
            }

            ACTION_UPDATE_SCALE -> {
                val newScale = intent.getFloatExtra(EXTRA_SCALE, 1.0f)
                dynamicIslandState?.updateConfig(newScale, dynamicIslandState?.persistentText ?: "User")
            }

            ACTION_SHOW_NOTIFICATION_SWITCH -> intent.getStringExtra(EXTRA_MODULE_NAME)?.let { name ->
                val state = intent.getBooleanExtra(EXTRA_MODULE_STATE, false)
                dynamicIslandState?.addSwitch(name, name, state)
            }

            ACTION_SHOW_OR_UPDATE_PROGRESS -> handleShowOrUpdateProgress(intent)
            ACTION_SHOW_OR_UPDATE_MUSIC -> handleShowOrUpdateMusic(intent)
            ACTION_REMOVE_TASK -> intent.getStringExtra(EXTRA_IDENTIFIER)?.let { dynamicIslandState?.removeTask(it) }

            ACTION_SET_MUSIC_MODE -> {
                val enabled = intent.getBooleanExtra(EXTRA_MUSIC_MODE_ENABLED, true)
                musicModeEnabled.value = enabled
                if (enabled) startMusicObserver() else stopMusicObserver()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        stopMusicObserver()
        windowManager.removeView(composeView)
        serviceScope.cancel()
    }

    private fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density).roundToInt()

    private fun handleShowOrUpdateProgress(intent: Intent) {
        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE)
        val progress = intent.takeIf { it.hasExtra(EXTRA_PROGRESS_VALUE) }?.getFloatExtra(EXTRA_PROGRESS_VALUE, 0f)
        val duration = if (progress == null && intent.hasExtra(EXTRA_DURATION_MS)) {
            intent.getLongExtra(EXTRA_DURATION_MS, 5000L)
        } else {
            null
        }

        val iconDrawable = when {
            intent.hasExtra(EXTRA_IMAGE_DATA) -> intent.getByteArrayExtra(EXTRA_IMAGE_DATA)?.let { createDrawableFromByteArray(it) }
            intent.getIntExtra(EXTRA_ICON_RES_ID, -1) != -1 -> {
                val resId = intent.getIntExtra(EXTRA_ICON_RES_ID, -1)
                runCatching { ContextCompat.getDrawable(this, resId) }.getOrNull()
            }
            else -> null
        }

        dynamicIslandState?.addOrUpdateProgress(identifier, title, subtitle, iconDrawable, progress, duration)
    }

    private fun handleShowOrUpdateMusic(intent: Intent) {
        if (!musicModeEnabled.value) return

        val identifier = intent.getStringExtra(EXTRA_IDENTIFIER) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: ""
        val progressText = intent.getStringExtra(EXTRA_PROGRESS_TEXT) ?: ""
        val progress = intent.getFloatExtra(EXTRA_PROGRESS_VALUE, 0f)
        val isMajorUpdate = intent.getBooleanExtra(EXTRA_IS_MAJOR_UPDATE, true)

        val albumArt = intent.getByteArrayExtra(EXTRA_ALBUM_ART_DATA)?.let { createDrawableFromByteArray(it) }

        dynamicIslandState?.addOrUpdateMusic(identifier, title, subtitle, albumArt, progressText, progress, isMajorUpdate)
    }

    private fun createDrawableFromByteArray(byteArray: ByteArray): Drawable? {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            bitmap?.let { android.graphics.drawable.BitmapDrawable(resources, it) }
        } catch (e: Exception) {
            Log.e("DynamicIslandService", "Failed to decode image data", e)
            null
        }
    }
}