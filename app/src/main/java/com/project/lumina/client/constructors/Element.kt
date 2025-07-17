package com.project.lumina.client.constructors

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.project.lumina.client.game.InterruptiblePacketHandler
import kotlinx.serialization.json.*

abstract class Element(
    val name: String,                               // 内部名
    val category: CheatCategory,
    val iconResId: Int = 0,                        // 不再使用，可保留兼容
    defaultEnabled: Boolean = false,
    val private: Boolean = false,
    @StringRes open val displayNameResId: Int? = null  // 显示在按钮上的文字
) : InterruptiblePacketHandler, Configurable {

    open lateinit var session: NetBound
    private var _isEnabled by mutableStateOf(defaultEnabled)

    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {
            if (_isEnabled != value) {
                _isEnabled = value
                if (value) onEnabled() else onDisabled()
            }
        }

    val isSessionCreated: Boolean
        get() = ::session.isInitialized

    var isExpanded by mutableStateOf(false)
    var isShortcutDisplayed by mutableStateOf(false)
    var shortcutX = 0
    var shortcutY = 100

    val overlayShortcutButton by lazy { OverlayShortcutButton(this) }
    override val values: MutableList<Value<*>> = ArrayList()

    open fun onEnabled() {
        ArrayListManager.addModule(this)
        sendToggleMessage(true)
    }

    open fun onDisabled() {
        ArrayListManager.removeModule(this)
        sendToggleMessage(false)
    }

    /* ------------ 序列化 ------------ */
    override fun toJson() = buildJsonObject {
        put("state", isEnabled)
        put("values", buildJsonObject {
            values.forEach { value ->
                val key = if (value.name.isNotEmpty()) value.name else value.nameResId.toString()
                put(key, value.toJson())
            }
        })
        if (isShortcutDisplayed) {
            put("shortcut", buildJsonObject {
                put("x", shortcutX)
                put("y", shortcutY)
            })
        }
    }

    override fun fromJson(jsonElement: JsonElement) {
        if (jsonElement !is JsonObject) return
        isEnabled = (jsonElement["state"] as? JsonPrimitive)?.boolean ?: isEnabled
        (jsonElement["values"] as? JsonObject)?.forEach { (k, v) ->
            val value = getValue(k) ?: values.find { it.nameResId.toString() == k }
            value?.runCatching { fromJson(v) }?.onFailure { reset() }
        }
        (jsonElement["shortcut"] as? JsonObject)?.let {
            shortcutX = (it["x"] as? JsonPrimitive)?.int ?: shortcutX
            shortcutY = (it["y"] as? JsonPrimitive)?.int ?: shortcutY
            isShortcutDisplayed = true
        }
    }

    private fun sendToggleMessage(enabled: Boolean) {
        if (!isSessionCreated) return
        try {
            OverlayNotification.addNotification(name, enabled)
            if (enabled) OverlayModuleList.showText(name)
            else OverlayModuleList.removeText(name)
        } catch (e: Exception) {
            Log.w("AppCrashChan :3", "Failed to show module notification: ${e.message}")
        }
    }
}
