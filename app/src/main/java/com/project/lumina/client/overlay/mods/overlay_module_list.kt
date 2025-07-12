package com.project.lumina.client.overlay.mods

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
            x = 3 
            y = 2
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    companion object {
        private val moduleState = ModuleState()
        private val overlayInstance by lazy { OverlayModuleList() }
        private var shouldShowOverlay = false

        private var capitalizeAndMerge = false
        private var displayMode = "None"

        val rainbowColors = listOf(
            Color(0xFFFF0000), // 红色
            Color(0xFFFF8000), // 橙色
            Color(0xFFFFFF00), // 黄色
            Color(0xFF00FF00), // 绿色
            Color(0xFF00FFFF), // 青色
            Color(0xFF0000FF), // 蓝色
            Color(0xFF8000FF)  // 紫色
        )

        fun setCapitalizeAndMerge(enabled: Boolean) {
            capitalizeAndMerge = enabled
        }

        fun setDisplayMode(mode: String) {
            displayMode = mode
        }

        fun showText(moduleName: String) {
            if (shouldShowOverlay) {
                moduleState.addModule(moduleName)
                try {
                    OverlayManager.showOverlayWindow(overlayInstance)
                } catch (e: Exception) {
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
                .wrapContentHeight()
                .wrapContentWidth()
                .padding(top = 8.dp, bottom = 8.dp, end = 0.dp)  
                .alpha(overlayAlpha)
                .background(Color.Transparent),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp) // 缩小间距从4.dp到2.dp
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

        // 创建彩虹色动态渐变动画
        val infiniteTransition = rememberInfiniteTransition()
        val animatedOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000), // 2秒循环一次
                repeatMode = RepeatMode.Restart
            )
        )

        // 创建彩虹色动态渐变
        val rainbowGradient = remember(animatedOffset) {
            val totalColors = rainbowColors.size
            val adjustedOffset = (animatedOffset * totalColors) % totalColors
            
            val colorList = mutableListOf<Color>()
            for (i in 0 until 3) { // 取3个连续的颜色用于渐变
                val colorIndex = ((adjustedOffset + i) % totalColors).toInt()
                colorList.add(rainbowColors[colorIndex])
            }
            colorList
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

        val animationOffset by animateFloatAsState(
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

        Row(
            modifier = Modifier
                .offset(x = animationOffset.dp)
                .alpha(alpha)
                .scale(scale)
                .wrapContentWidth()
                .height(26.dp) // 稍微减小高度，让模块更紧凑
                .padding(horizontal = 4.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.7f), // 半透明黑色背景
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
            // 彩虹渐变文本
            Text(
                text = module.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .wrapContentWidth()
                    .drawWithCache {
                        val brush = Brush.linearGradient(
                            colors = rainbowGradient,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f)
                        )
                        onDrawBehind {
                            drawRect(
                                brush = brush,
                                size = size
                            )
                        }
                    },
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = rainbowGradient,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    )
                )
            )

            // 彩虹渐变竖条
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp) // 稍微减小竖条宽度
                    .background(
                        brush = Brush.linearGradient(
                            colors = rainbowGradient,
                            start = Offset(0f, 0f),
                            end = Offset(0f, 100f)
                        )
                    )
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