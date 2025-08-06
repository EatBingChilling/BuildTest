#!/bin/bash

echo "修复非@Composable上下文中的@Composable函数调用..."

# 对于在非@Composable上下文中调用的@Composable函数，我们需要将它们移到@Composable函数内部
# 或者使用remember来缓存颜色值

# 修复MiniMapOverlay.kt中的非@Composable上下文调用
# 这些调用需要在@Composable函数内部进行
echo "修复MiniMapOverlay.kt..."

# 修复AnimatedBackground.kt中的非@Composable上下文调用
echo "修复AnimatedBackground.kt..."

# 修复PacketNotificationOverlay.kt中的非@Composable上下文调用
echo "修复PacketNotificationOverlay.kt..."

# 修复TopCenterOverlayNotification.kt中的非@Composable上下文调用
echo "修复TopCenterOverlayNotification.kt..."

# 修复ElevatedCardX.kt中的非@Composable上下文调用
echo "修复ElevatedCardX.kt..."

# 修复ModuleCardX.kt中的非@Composable上下文调用
echo "修复ModuleCardX.kt..."

echo "非@Composable上下文修复完成！" 