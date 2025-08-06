package com.project.lumina.client.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.project.lumina.client.R

// Material3 配色变量定义
val primary get() = MaterialTheme.colorScheme.primary
val BackgroundColor get() = MaterialTheme.colorScheme.background
val SurfaceColor get() = MaterialTheme.colorScheme.surface
val OnBackgroundColor get() = MaterialTheme.colorScheme.onBackground
val OnSurfaceColor get() = MaterialTheme.colorScheme.onSurface
val TheBackgroundColorForOverlayUi get() = MaterialTheme.colorScheme.surface
val TheBackgroundColorForOverlayUi2 get() = MaterialTheme.colorScheme.surfaceVariant
val TheNotBackgroundColorForOverlayUi get() = MaterialTheme.colorScheme.onSurface
val TextColorForModules get() = MaterialTheme.colorScheme.onSurface

// Launcher 相关配色
val LauncherRadialColor get() = MaterialTheme.colorScheme.primary
val LAnimationColor get() = MaterialTheme.colorScheme.primary
val LTextColor get() = MaterialTheme.colorScheme.onSurface
val LBlobColor1 get() = MaterialTheme.colorScheme.primary
val LBlobColor2 get() = MaterialTheme.colorScheme.secondary
val LBg1 get() = MaterialTheme.colorScheme.background
val LBg2 get() = MaterialTheme.colorScheme.surface

// MiniMap 相关配色
val Mbg get() = MaterialTheme.colorScheme.surface
val MgridColor get() = MaterialTheme.colorScheme.outline
val MCrosshair get() = MaterialTheme.colorScheme.primary
val MPlayerMarker get() = MaterialTheme.colorScheme.primary
val MNorth get() = MaterialTheme.colorScheme.primary
val MEntityClose get() = MaterialTheme.colorScheme.error
val MEntityFar get() = MaterialTheme.colorScheme.tertiary

// ArrayList 相关配色
val OArrayList1 get() = MaterialTheme.colorScheme.primary
val OArrayList2 get() = MaterialTheme.colorScheme.secondary
val OArrayBase get() = MaterialTheme.colorScheme.surface

// Notification 相关配色
val ONotifAccent get() = MaterialTheme.colorScheme.primary
val ONotifBase get() = MaterialTheme.colorScheme.surface
val ONotifText get() = MaterialTheme.colorScheme.onSurface
val ONotifProgressbar get() = MaterialTheme.colorScheme.primary

// Packet 相关配色
val PColorGradient1 get() = MaterialTheme.colorScheme.primary
val PColorGradient2 get() = MaterialTheme.colorScheme.secondary
val PBackground get() = MaterialTheme.colorScheme.surface

// Speedometer 相关配色
val SBaseColor get() = MaterialTheme.colorScheme.primary
val SAccentColor get() = MaterialTheme.colorScheme.secondary
val SBAckgroundGradient1 get() = MaterialTheme.colorScheme.surface
val SBAckgroundGradient2 get() = MaterialTheme.colorScheme.surfaceVariant

val SMiniLineGrpah get() = MaterialTheme.colorScheme.primary
val SMeterBg get() = MaterialTheme.colorScheme.surface
val SMeterAccent get() = MaterialTheme.colorScheme.primary
val SMeterBase get() = MaterialTheme.colorScheme.secondary

// TopCenterOverlay 相关配色
val TCOGradient1 get() = MaterialTheme.colorScheme.primary
val TCOGradient2 get() = MaterialTheme.colorScheme.secondary
val TCOBackground get() = MaterialTheme.colorScheme.surface

// ElevatedCard 相关配色
val EColorCard1 get() = MaterialTheme.colorScheme.primary
val EColorCard2 get() = MaterialTheme.colorScheme.secondary
val EColorCard3 get() = MaterialTheme.colorScheme.tertiary

// ModuleCard 相关配色
val MColorCard1 get() = MaterialTheme.colorScheme.primary
val MColorCard2 get() = MaterialTheme.colorScheme.secondary
val MColorCard3 get() = MaterialTheme.colorScheme.tertiary
val MColorScreen1 get() = MaterialTheme.colorScheme.surface
val MColorScreen2 get() = MaterialTheme.colorScheme.surfaceVariant

// Navigation 相关配色
val NColorItem1 get() = MaterialTheme.colorScheme.primary
val NColorItem2 get() = MaterialTheme.colorScheme.secondary
val NColorItem3 get() = MaterialTheme.colorScheme.tertiary
val NColorItem4 get() = MaterialTheme.colorScheme.primary
val NColorItem5 get() = MaterialTheme.colorScheme.secondary
val NColorItem6 get() = MaterialTheme.colorScheme.tertiary
val NColorItem7 get() = MaterialTheme.colorScheme.primary

// PackItem 相关配色
val PColorItem1 get() = MaterialTheme.colorScheme.primary

// 启用/禁用状态配色
val EnabledBackgroundColor get() = MaterialTheme.colorScheme.primary
val DisabledBackgroundColor get() = MaterialTheme.colorScheme.surfaceVariant
val EnabledGlowColor get() = MaterialTheme.colorScheme.primary
val EnabledTextColor get() = MaterialTheme.colorScheme.onPrimary
val DisabledTextColor get() = MaterialTheme.colorScheme.onSurfaceVariant
val EnabledIconColor get() = MaterialTheme.colorScheme.onPrimary
val DisabledIconColor get() = MaterialTheme.colorScheme.onSurfaceVariant

// 进度指示器配色
val ProgressIndicatorColor get() = MaterialTheme.colorScheme.primary

// Slider 相关配色
val SliderTrackColor get() = MaterialTheme.colorScheme.surfaceVariant
val SliderActiveTrackColor get() = MaterialTheme.colorScheme.primary
val SliderThumbColor get() = MaterialTheme.colorScheme.primary

// Checkbox 相关配色
val CheckboxUncheckedColor get() = MaterialTheme.colorScheme.outline
val CheckboxCheckedColor get() = MaterialTheme.colorScheme.primary
val CheckboxCheckmarkColor get() = MaterialTheme.colorScheme.onPrimary

// Choice 相关配色
val ChoiceSelectedColor get() = MaterialTheme.colorScheme.primary
val ChoiceUnselectedColor get() = MaterialTheme.colorScheme.surfaceVariant

// KitsuGUI 相关配色
val KitsuPrimary get() = MaterialTheme.colorScheme.primary
val KitsuSecondary get() = MaterialTheme.colorScheme.secondary
val KitsuSurface get() = MaterialTheme.colorScheme.surface
val KitsuSurfaceVariant get() = MaterialTheme.colorScheme.surfaceVariant
val KitsuOnSurface get() = MaterialTheme.colorScheme.onSurface
val KitsuOnSurfaceVariant get() = MaterialTheme.colorScheme.onSurfaceVariant
val KitsuBackground get() = MaterialTheme.colorScheme.background
val KitsuSelected get() = MaterialTheme.colorScheme.primary
val KitsuUnselected get() = MaterialTheme.colorScheme.surfaceVariant
val KitsuHover get() = MaterialTheme.colorScheme.primaryContainer

// Keystrokes Overlay 相关配色
val baseColor get() = MaterialTheme.colorScheme.surface
val borderColor get() = MaterialTheme.colorScheme.outline
val pressedColor get() = MaterialTheme.colorScheme.primary
val textColor get() = MaterialTheme.colorScheme.onSurface

val MyFontFamily = FontFamily(
    Font(R.font.fredoka_light)
)

val MyTypography = Typography(
    displayLarge = TextStyle(fontFamily = MyFontFamily, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = MyFontFamily, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = MyFontFamily, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = MyFontFamily, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = MyFontFamily, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = MyFontFamily, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = MyFontFamily, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = MyFontFamily, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = MyFontFamily, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = MyFontFamily, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = MyFontFamily, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = MyFontFamily, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = MyFontFamily, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = MyFontFamily, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = MyFontFamily, fontSize = 11.sp),
)

@Composable
fun LuminaClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MyTypography,
        content = content
    )
}