package com.project.lumina.client.overlay.mods

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay

class OverlayModuleList : OverlayWindow() {
    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.END
            x = 10
            y = 5
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private val moduleState = ModuleState()
        private val overlayInstance by lazy { OverlayModuleList() }
        private var shouldShowOverlay = false

        val moduleColors = listOf(
            Color(0xFFff5555),
            Color(0xFF55ff55),
            Color(0xFF5555ff),
            Color(0xFFffff55)
        )

        fun showText(moduleName: String) {
            if (shouldShowOverlay) {
                moduleState.addModule(moduleName)
                try {
                    OverlayManager.showOverlayWindow(overlayInstance)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        fun removeText(moduleName: String) {
            moduleState.removeModule(moduleName)
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (!enabled) {
                try {
                    OverlayManager.dismissOverlayWindow(overlayInstance)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        fun isOverlayEnabled(): Boolean = shouldShowOverlay
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        var overlayVisible by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            overlayVisible = true
        }

        val overlayAlpha by animateFloatAsState(
            targetValue = if (overlayVisible) 1f else 0f,
            animationSpec = tween(500)
        )

        val sortedModules = moduleState.modules.sortedByDescending { it.name.length }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .padding(8.dp)
                .alpha(overlayAlpha),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sortedModules.forEachIndexed { index, module ->
                key(module.id) {
                    ModuleItem(
                        module = module,
                        moduleState = moduleState,
                        index = index,
                        entryDelay = index * 50
                    )
                }
            }
        }
    }

    @Composable
    fun ModuleItem(
        module: ModuleItem,
        moduleState: ModuleState,
        index: Int,
        entryDelay: Int = 0
    ) {
        var visible by remember { mutableStateOf(false) }
        var exitState by remember { mutableStateOf(false) }
        val isEnabled by remember { derivedStateOf { moduleState.isModuleEnabled(module.name) } }
        val isMarkedForRemoval by remember {
            derivedStateOf { moduleState.modulesToRemove.contains(module.name) }
        }

        LaunchedEffect(Unit) {
            delay(entryDelay.toLong())
            visible = true
        }

        LaunchedEffect(isMarkedForRemoval) {
            if (isMarkedForRemoval) {
                exitState = true
                delay(300)
                moduleState.removeModule(module.name)
            }
        }

        val offsetX by animateFloatAsState(
            targetValue = when {
                exitState -> 200f
                visible -> 0f
                else -> -200f
            },
            animationSpec = tween(300)
        )

        val alpha by animateFloatAsState(
            targetValue = if (visible && !exitState) 1f else 0f,
            animationSpec = tween(300)
        )

        val scale by animateFloatAsState(
            targetValue = when {
                exitState -> 0.8f
                visible -> 1f
                else -> 0.8f
            },
            animationSpec = tween(300)
        )

        val textColor = moduleColors[index % moduleColors.size]
        val barColor = textColor

        Row(
            modifier = Modifier
                .offset(x = offsetX.dp)
                .alpha(alpha)
                .scale(scale)
                .wrapContentWidth()
                .height(28.dp)
                .padding(horizontal = 4.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isEnabled) {
                        moduleState.markForRemoval(module.name)
                    }
                }
        ) {
            Text(
                text = module.name,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .weight(1f)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(5.dp) // ← 修复单位错误
                    .background(color = barColor)
            )
        }
    }
}

class ModuleState {
    private val _modules = mutableStateListOf<ModuleItem>()
    val modules: List<ModuleItem> get() = _modules.toList()
    private var nextId = 0
    private val _modulesToRemove = mutableStateListOf<String>()
    val modulesToRemove: List<String> get() = _modulesToRemove.toList()

    fun addModule(moduleName: String) {
        if (_modules.none { it.name == moduleName }) {
            _modules.add(ModuleItem(nextId++, moduleName))
            _modulesToRemove.remove(moduleName)
        }
    }

    fun markForRemoval(moduleName: String) {
        if (!_modulesToRemove.contains(moduleName)) {
            _modulesToRemove.add(moduleName)
        }
    }

    fun removeModule(moduleName: String) {
        _modules.removeAll { it.name == moduleName }
        _modulesToRemove.remove(moduleName)
    }

    fun isModuleEnabled(moduleName: String): Boolean {
        return _modules.any { it.name == moduleName } && !_modulesToRemove.contains(moduleName)
    }

    fun toggleModule(moduleName: String) {
        if (isModuleEnabled(moduleName)) {
            markForRemoval(moduleName)
        } else {
            addModule(moduleName)
        }
    }
}

data class ModuleItem(val id: Int, val name: String)
