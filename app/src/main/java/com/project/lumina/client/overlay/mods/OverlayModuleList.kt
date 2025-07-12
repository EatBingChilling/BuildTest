package com.project.lumina.client.overlay.mods

import android.graphics.BlurMaskFilter
import android.graphics.Color as NativeColor
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.overlay.manager.OverlayWindow
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class OverlayModuleList : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.END
            x = 3
            y = 2
        }
    }

    override val layoutParams: WindowManager.LayoutParams get() = _layoutParams

    companion object {
        private val moduleState = ModuleState()
        private val overlayInstance by lazy { OverlayModuleList() }
        private var shouldShowOverlay = false

        fun showText(name: String) {
            if (shouldShowOverlay) {
                moduleState.addModule(name)
                runCatching { OverlayManager.showOverlayWindow(overlayInstance) }
            }
        }

        fun removeText(name: String) {
            moduleState.markForRemoval(name)
        }

        fun setOverlayEnabled(enabled: Boolean) {
            shouldShowOverlay = enabled
            if (!enabled) runCatching { OverlayManager.dismissOverlayWindow(overlayInstance) }
        }

        fun isOverlayEnabled() = shouldShowOverlay
    }

    @Composable
    override fun Content() {
        if (!isOverlayEnabled()) return

        val sorted = moduleState.modules
            .sortedByDescending { it.name.length }

        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(top = 8.dp, end = 3.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(0.dp) // 零间距
        ) {
            sorted.forEachIndexed { idx, mod ->
                key(mod.id) {
                    NeonModuleRow(mod, idx, sorted.size)
                }
            }
        }
    }

    @Composable
    private fun NeonModuleRow(
        module: ModuleItem,
        index: Int,
        total: Int
    ) {
        val text = module.name
        val isRemoving = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            snapshotFlow { moduleState.modulesToRemove }
                .collect { isRemoving.value = it.contains(module.name) }
        }

        val infinite = rememberInfiniteTransition()
        val phase by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing)
            )
        )

        val textMeasurer = rememberTextMeasurer()
        val style = TextStyle(fontSize = 13.sp)
        val measured = remember(text) { textMeasurer.measure(text, style) }

        val density = LocalDensity.current
        val glowRadius = with(density) { 10.dp.toPx() }

        val paddingH = with(density) { 8.dp.toPx() }
        val paddingV = with(density) { 4.dp.toPx() }

        val targetAlpha = if (isRemoving.value) 0f else 1f
        val alpha by animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = tween(300)
        )

        Spacer(Modifier.height(2.dp)) // 细间距

        Box(
            modifier = Modifier
                .alpha(alpha)
                .wrapContentSize()
                .drawWithCache {
                    val colorPos = (phase + index * 0.7f / total) % 1f
                    val color = smoothGradient(colorPos)

                    onDrawBehind {
                        // 发光：先画 3 次模糊底
                        val glowPaint = Paint().apply {
                            this.color = color
                            blurRadius = glowRadius
                        }
                        repeat(3) {
                            drawIntoCanvas { canvas ->
                                val nativePaint = (glowPaint.asFrameworkPaint()).apply {
                                    maskFilter = BlurMaskFilter(
                                        glowRadius,
                                        BlurMaskFilter.Blur.SOLID
                                    )
                                }
                                canvas.nativeCanvas.drawText(
                                    text,
                                    paddingH,
                                    paddingV + measured.size.height / 2f + measured.firstBaseline,
                                    nativePaint
                                )
                            }
                        }

                        // 实体文字
                        drawText(
                            textMeasurer,
                            text,
                            topLeft = Offset(paddingH, paddingV),
                            style = style.copy(color = color)
                        )
                    }
                }
        ) {
            // 占位尺寸
            Spacer(
                Modifier.size(
                    width = (measured.size.width + paddingH * 2).toDp(density),
                    height = (measured.size.height + paddingV * 2).toDp(density)
                )
            )
        }

        if (isRemoving.value && alpha == 0f) {
            moduleState.removeModule(module.name)
        }
    }

    /* ---------------- 工具函数 ---------------- */

    private fun smoothGradient(position: Float): Color {
        val colors = listOf(
            Color.Red,
            Color(0xFFFF4500),
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color(0xFFFF69B4)
        )
        val len = colors.size
        val pos = (position % 1f).coerceIn(0f, 1f)
        val scaled = pos * (len - 1)
        val idx = scaled.toInt()
        val fract = scaled - idx
        val c1 = colors[idx % len]
        val c2 = colors[(idx + 1) % len]
        return lerp(c1, c2, fract)
    }

    private fun lerp(a: Color, b: Color, t: Float): Color {
        return Color(
            red = a.red + t * (b.red - a.red),
            green = a.green + t * (b.green - a.green),
            blue = a.blue + t * (b.blue - a.blue),
            alpha = 1f
        )
    }

    private fun Float.toDp(density: Density): Dp = (this / density.density).dp
}
