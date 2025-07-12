package com.project.lumina.client.overlay.mods

import android.view.Gravity
import android.view.WindowManager
import android.graphics.PixelFormat
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.ui.theme.*
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ===============  入口：对外暴露的两个静态方法  =============== */
class OverlayNotification : OverlayWindow() {

    private val _layoutParams by lazy {
        WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.BOTTOM or Gravity.END
            x = 20
            y = 20
            format = PixelFormat.TRANSLUCENT
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private val notificationState = NotificationState()
        private var isOverlayShowing = false

        /* 开启 */
        fun onModuleEnabled(moduleName: String) {
            notificationState.addNotification(moduleName, ModuleAction.ENABLE)
            ensureOverlayVisible()
        }

        /* 关闭 */
        fun onModuleDisabled(moduleName: String) {
            notificationState.addNotification(moduleName, ModuleAction.DISABLE)
            ensureOverlayVisible()
        }

        private fun ensureOverlayVisible() {
            if (isOverlayShowing) return
            try {
                OverlayManager.showOverlayWindow(OverlayNotification())
                isOverlayShowing = true
            } catch (_: Exception) {
            }
        }

        fun onOverlayDismissed() {
            isOverlayShowing = false
        }
    }

    /* -------------------- Compose UI -------------------- */
    @Composable
    override fun Content() {
        val notifications = notificationState.notifications

        LaunchedEffect(notifications.size) {
            if (notifications.isEmpty()) {
                delay(100)
                OverlayManager.dismissOverlayWindow(this@OverlayNotification)
                onOverlayDismissed()
            }
        }

        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(0.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                notifications.forEach { item ->
                    key(item.id) {
                        NotificationCard(item, notificationState)
                    }
                }
            }
        }
    }
}

/* ===============  单张卡片  =============== */
@Composable
private fun NotificationCard(
    item: NotificationItem,
    state: NotificationState
) {
    var visible by remember { mutableStateOf(false) }
    var exitState by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(item.id) {
        delay(50)
        visible = true
        delay(2000)
        exitState = true
        delay(300)
        state.removeNotification(item.id)
    }

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val offsetX by animateFloatAsState(
        if (exitState) 200f else if (visible) 0f else -200f,
        springSpec
    )
    val scale by animateFloatAsState(
        if (exitState) 0.8f else 1f,
        springSpec
    )
    val alpha by animateFloatAsState(
        if (visible && !exitState) 1f else 0f,
        tween(300, easing = FastOutSlowInEasing)
    )

    val progressAnimation = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        scope.launch {
            progressAnimation.animateTo(0f, tween(2500))
        }
    }

    // 根据开启/关闭选择颜色
    val baseColor = when (item.action) {
        ModuleAction.ENABLE -> ONotifBase
        ModuleAction.DISABLE -> ONotifDisableBase
    }
    val accentColor = when (item.action) {
        ModuleAction.ENABLE -> ONotifAccent
        ModuleAction.DISABLE -> ONotifDisableAccent
    }

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp)
            .alpha(alpha)
            .scale(scale)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.8f),
                ambientColor = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(12.dp))
            .width(140.dp)
            .height(75.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(baseColor.copy(alpha = 0.95f))
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.moduleName,
                    color = ONotifText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.7f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(accentColor, RoundedCornerShape(3.dp))
                )
            }

            Text(
                text = when (item.action) {
                    ModuleAction.ENABLE -> "Enabled"
                    ModuleAction.DISABLE -> "Disabled"
                },
                color = ONotifText.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(0f, 0.5f),
                        blurRadius = 1f
                    )
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        ONotifProgressbar.copy(alpha = 0.3f),
                        RoundedCornerShape(1.5.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnimation.value)
                        .height(3.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.8f))
                            ),
                            shape = RoundedCornerShape(1.5.dp)
                        )
                )
            }
        }
    }
}

/* ===============  状态管理  =============== */
private enum class ModuleAction { ENABLE, DISABLE }

data class NotificationItem(
    val id: Int,
    val moduleName: String,
    val action: ModuleAction
)


private class NotificationState {
    private val _notifications = mutableStateListOf<NotificationItem>()
    val notifications: List<NotificationItem> get() = _notifications

    private var nextId = 0
    private val activeKeys = mutableSetOf<String>()

    fun addNotification(moduleName: String, action: ModuleAction) {
        val key = "$moduleName-${action.name}"
        if (key in activeKeys) return
        activeKeys.add(key)

        if (_notifications.size >= 3) {
            val oldest = _notifications.removeFirst()
            activeKeys.remove("${oldest.moduleName}-${oldest.action.name}")
        }
        _notifications.add(NotificationItem(nextId++, moduleName, action))
    }

    fun removeNotification(id: Int) {
        val item = _notifications.find { it.id == id } ?: return
        _notifications.remove(item)
        activeKeys.remove("${item.moduleName}-${item.action.name}")
    }
}

/* ===============  预览  =============== */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewCards() {
    val state = NotificationState().apply {
        addNotification("Test Module", ModuleAction.ENABLE)
        addNotification("Another", ModuleAction.DISABLE)
    }
    Box(Modifier.background(Color.Black).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.notifications.forEach {
                NotificationCard(it, state)
            }
        }
    }
}
