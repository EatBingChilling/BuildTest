package com.project.lumina.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material3 配色变量定义 - 所有函数都需要@Composable注解
@Composable
fun primary(): Color = MaterialTheme.colorScheme.primary

@Composable
fun BackgroundColor(): Color = MaterialTheme.colorScheme.background

@Composable
fun SurfaceColor(): Color = MaterialTheme.colorScheme.surface

@Composable
fun OnBackgroundColor(): Color = MaterialTheme.colorScheme.onBackground

@Composable
fun OnSurfaceColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun TheBackgroundColorForOverlayUi(): Color = MaterialTheme.colorScheme.surface

@Composable
fun TheBackgroundColorForOverlayUi2(): Color = MaterialTheme.colorScheme.surfaceVariant

@Composable
fun TheNotBackgroundColorForOverlayUi(): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun TextColorForModules(): Color = MaterialTheme.colorScheme.onSurface

// Launcher 相关配色
@Composable
fun LauncherRadialColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun LAnimationColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun LTextColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun LBlobColor1(): Color = MaterialTheme.colorScheme.primary

@Composable
fun LBlobColor2(): Color = MaterialTheme.colorScheme.secondary

@Composable
fun LBg1(): Color = MaterialTheme.colorScheme.background

@Composable
fun LBg2(): Color = MaterialTheme.colorScheme.surface

// MiniMap 相关配色
@Composable
fun Mbg(): Color = MaterialTheme.colorScheme.surface

@Composable
fun MgridColor(): Color = MaterialTheme.colorScheme.outline

@Composable
fun MCrosshair(): Color = MaterialTheme.colorScheme.primary

@Composable
fun MPlayerMarker(): Color = MaterialTheme.colorScheme.primary

@Composable
fun MNorth(): Color = MaterialTheme.colorScheme.primary

@Composable
fun MEntityClose(): Color = MaterialTheme.colorScheme.error

@Composable
fun MEntityFar(): Color = MaterialTheme.colorScheme.tertiary

// ArrayList 相关配色
@Composable
fun OArrayList1(): Color = MaterialTheme.colorScheme.primary

@Composable
fun OArrayList2(): Color = MaterialTheme.colorScheme.secondary

@Composable
fun OArrayBase(): Color = MaterialTheme.colorScheme.surface

// Notification 相关配色
@Composable
fun ONotifAccent(): Color = MaterialTheme.colorScheme.primary

@Composable
fun ONotifBase(): Color = MaterialTheme.colorScheme.surface

@Composable
fun ONotifText(): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun ONotifProgressbar(): Color = MaterialTheme.colorScheme.primary

// Packet 相关配色
@Composable
fun PColorGradient1(): Color = MaterialTheme.colorScheme.primary

@Composable
fun PColorGradient2(): Color = MaterialTheme.colorScheme.secondary

@Composable
fun PBackground(): Color = MaterialTheme.colorScheme.surface

// Speedometer 相关配色
@Composable
fun SBaseColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun SAccentColor(): Color = MaterialTheme.colorScheme.secondary

@Composable
fun SBAckgroundGradient1(): Color = MaterialTheme.colorScheme.surface

@Composable
fun SBAckgroundGradient2(): Color = MaterialTheme.colorScheme.surfaceVariant

@Composable
fun SMiniLineGrpah(): Color = MaterialTheme.colorScheme.primary

@Composable
fun SMeterBg(): Color = MaterialTheme.colorScheme.surface

@Composable
fun SMeterAccent(): Color = MaterialTheme.colorScheme.primary

@Composable
fun SMeterBase(): Color = MaterialTheme.colorScheme.secondary

// TopCenterOverlay 相关配色
@Composable
fun TCOGradient1(): Color = MaterialTheme.colorScheme.primary

@Composable
fun TCOGradient2(): Color = MaterialTheme.colorScheme.secondary

@Composable
fun TCOBackground(): Color = MaterialTheme.colorScheme.surface

// ElevatedCard 相关配色
@Composable
fun EColorCard1(): Color = MaterialTheme.colorScheme.primary

@Composable
fun EColorCard2(): Color = MaterialTheme.colorScheme.secondary

@Composable
fun EColorCard3(): Color = MaterialTheme.colorScheme.tertiary

// ModuleCard 相关配色
@Composable
fun MColorCard1(): Color = MaterialTheme.colorScheme.primary

@Composable
fun MColorCard2(): Color = MaterialTheme.colorScheme.secondary

@Composable
fun MColorCard3(): Color = MaterialTheme.colorScheme.tertiary

@Composable
fun MColorScreen1(): Color = MaterialTheme.colorScheme.surface

@Composable
fun MColorScreen2(): Color = MaterialTheme.colorScheme.surfaceVariant

// Navigation 相关配色
@Composable
fun NColorItem1(): Color = MaterialTheme.colorScheme.primary

@Composable
fun NColorItem2(): Color = MaterialTheme.colorScheme.secondary

@Composable
fun NColorItem3(): Color = MaterialTheme.colorScheme.tertiary

@Composable
fun NColorItem4(): Color = MaterialTheme.colorScheme.primary

@Composable
fun NColorItem5(): Color = MaterialTheme.colorScheme.secondary

@Composable
fun NColorItem6(): Color = MaterialTheme.colorScheme.tertiary

@Composable
fun NColorItem7(): Color = MaterialTheme.colorScheme.primary

// PackItem 相关配色
@Composable
fun PColorItem1(): Color = MaterialTheme.colorScheme.primary

// 启用/禁用状态配色
@Composable
fun EnabledBackgroundColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun DisabledBackgroundColor(): Color = MaterialTheme.colorScheme.surfaceVariant

@Composable
fun EnabledGlowColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun EnabledTextColor(): Color = MaterialTheme.colorScheme.onPrimary

@Composable
fun DisabledTextColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun EnabledIconColor(): Color = MaterialTheme.colorScheme.onPrimary

@Composable
fun DisabledIconColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

// 进度指示器配色
@Composable
fun ProgressIndicatorColor(): Color = MaterialTheme.colorScheme.primary

// Slider 相关配色
@Composable
fun SliderTrackColor(): Color = MaterialTheme.colorScheme.surfaceVariant

@Composable
fun SliderActiveTrackColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun SliderThumbColor(): Color = MaterialTheme.colorScheme.primary

// Checkbox 相关配色
@Composable
fun CheckboxUncheckedColor(): Color = MaterialTheme.colorScheme.outline

@Composable
fun CheckboxCheckedColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun CheckboxCheckmarkColor(): Color = MaterialTheme.colorScheme.onPrimary

// Choice 相关配色
@Composable
fun ChoiceSelectedColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun ChoiceUnselectedColor(): Color = MaterialTheme.colorScheme.surfaceVariant

// KitsuGUI 相关配色
@Composable
fun KitsuPrimary(): Color = MaterialTheme.colorScheme.primary

@Composable
fun KitsuSecondary(): Color = MaterialTheme.colorScheme.secondary

@Composable
fun KitsuSurface(): Color = MaterialTheme.colorScheme.surface

@Composable
fun KitsuSurfaceVariant(): Color = MaterialTheme.colorScheme.surfaceVariant

@Composable
fun KitsuOnSurface(): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun KitsuOnSurfaceVariant(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun KitsuBackground(): Color = MaterialTheme.colorScheme.background

@Composable
fun KitsuSelected(): Color = MaterialTheme.colorScheme.primary

@Composable
fun KitsuUnselected(): Color = MaterialTheme.colorScheme.surfaceVariant

@Composable
fun KitsuHover(): Color = MaterialTheme.colorScheme.primaryContainer

// Keystrokes Overlay 相关配色
@Composable
fun baseColor(): Color = MaterialTheme.colorScheme.surface

@Composable
fun borderColor(): Color = MaterialTheme.colorScheme.outline

@Composable
fun pressedColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun textColor(): Color = MaterialTheme.colorScheme.onSurface 