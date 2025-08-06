#!/bin/bash

# 批量修复配色函数调用问题
# 将所有配色变量调用改为函数调用形式

echo "开始修复配色函数调用..."

# 修复ModuleContentA.kt
sed -i 's/ProgressIndicatorColor/ProgressIndicatorColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/EnabledBackgroundColor/EnabledBackgroundColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/DisabledBackgroundColor/DisabledBackgroundColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/EnabledGlowColor/EnabledGlowColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/EnabledTextColor/EnabledTextColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/DisabledTextColor/DisabledTextColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/EnabledIconColor/EnabledIconColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/DisabledIconColor/DisabledIconColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/ChoiceSelectedColor/ChoiceSelectedColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/ChoiceUnselectedColor/ChoiceUnselectedColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/SliderTrackColor/SliderTrackColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/SliderActiveTrackColor/SliderActiveTrackColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/SliderThumbColor/SliderThumbColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/CheckboxCheckedColor/CheckboxCheckedColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt
sed -i 's/CheckboxUncheckedColor/CheckboxUncheckedColor()/g' app/src/main/java/com/project/lumina/client/overlay/clickgui/ModuleContentA.kt

# 修复HomeCategoryUi.kt
sed -i 's/KitsuPrimary/KitsuPrimary()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/HomeCategoryUi.kt

# 修复KitsuGUI.kt
sed -i 's/KitsuSurface/KitsuSurface()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/KitsuGUI.kt
sed -i 's/KitsuPrimary/KitsuPrimary()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/KitsuGUI.kt
sed -i 's/KitsuSecondary/KitsuSecondary()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/KitsuGUI.kt
sed -i 's/KitsuOnSurface/KitsuOnSurface()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/KitsuGUI.kt
sed -i 's/KitsuSelected/KitsuSelected()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/KitsuGUI.kt
sed -i 's/KitsuHover/KitsuHover()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/KitsuGUI.kt
sed -i 's/KitsuOnSurfaceVariant/KitsuOnSurfaceVariant()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/KitsuGUI.kt
sed -i 's/KitsuSurfaceVariant/KitsuSurfaceVariant()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/KitsuGUI.kt

# 修复PlayerListUI.kt
sed -i 's/TheNotBackgroundColorForOverlayUi/TheNotBackgroundColorForOverlayUi()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/PlayerListUI.kt

# 修复WorldStats.kt
sed -i 's/KitsuPrimary/KitsuPrimary()/g' app/src/main/java/com/project/lumina/client/overlay/kitsugui/WorldStats.kt

# 修复ConnectionInfoOverlay.kt
sed -i 's/KitsuPrimary/KitsuPrimary()/g' app/src/main/java/com/project/lumina/client/overlay/manager/ConnectionInfoOverlay.kt

# 修复KitsuSettingsOverlay.kt
sed -i 's/TheNotBackgroundColorForOverlayUi/TheNotBackgroundColorForOverlayUi()/g' app/src/main/java/com/project/lumina/client/overlay/manager/KitsuSettingsOverlay.kt
sed -i 's/TheBackgroundColorForOverlayUi/TheBackgroundColorForOverlayUi()/g' app/src/main/java/com/project/lumina/client/overlay/manager/KitsuSettingsOverlay.kt

# 修复OverlayClickGUI.kt
sed -i 's/TheBackgroundColorForOverlayUi/TheBackgroundColorForOverlayUi()/g' app/src/main/java/com/project/lumina/client/overlay/manager/OverlayClickGUI.kt
sed -i 's/TheNotBackgroundColorForOverlayUi/TheNotBackgroundColorForOverlayUi()/g' app/src/main/java/com/project/lumina/client/overlay/manager/OverlayClickGUI.kt
sed -i 's/TheBackgroundColorForOverlayUi2/TheBackgroundColorForOverlayUi2()/g' app/src/main/java/com/project/lumina/client/overlay/manager/OverlayClickGUI.kt

# 修复KeystrokesOverlay.kt
sed -i 's/pressedColor/pressedColor()/g' app/src/main/java/com/project/lumina/client/overlay/mods/KeystrokesOverlay.kt
sed -i 's/baseColor/baseColor()/g' app/src/main/java/com/project/lumina/client/overlay/mods/KeystrokesOverlay.kt
sed -i 's/borderColor/borderColor()/g' app/src/main/java/com/project/lumina/client/overlay/mods/KeystrokesOverlay.kt
sed -i 's/textColor/textColor()/g' app/src/main/java/com/project/lumina/client/overlay/mods/KeystrokesOverlay.kt

# 修复MiniMapOverlay.kt
sed -i 's/Mbg/Mbg()/g' app/src/main/java/com/project/lumina/client/overlay/mods/MiniMapOverlay.kt
sed -i 's/MgridColor/MgridColor()/g' app/src/main/java/com/project/lumina/client/overlay/mods/MiniMapOverlay.kt
sed -i 's/MCrosshair/MCrosshair()/g' app/src/main/java/com/project/lumina/client/overlay/mods/MiniMapOverlay.kt
sed -i 's/MPlayerMarker/MPlayerMarker()/g' app/src/main/java/com/project/lumina/client/overlay/mods/MiniMapOverlay.kt
sed -i 's/MEntityClose/MEntityClose()/g' app/src/main/java/com/project/lumina/client/overlay/mods/MiniMapOverlay.kt
sed -i 's/MEntityFar/MEntityFar()/g' app/src/main/java/com/project/lumina/client/overlay/mods/MiniMapOverlay.kt

# 修复OverlayNotification.kt
sed -i 's/ONotifAccent/ONotifAccent()/g' app/src/main/java/com/project/lumina/client/overlay/mods/OverlayNotification.kt
sed -i 's/ONotifBase/ONotifBase()/g' app/src/main/java/com/project/lumina/client/overlay/mods/OverlayNotification.kt
sed -i 's/ONotifText/ONotifText()/g' app/src/main/java/com/project/lumina/client/overlay/mods/OverlayNotification.kt
sed -i 's/ONotifProgressbar/ONotifProgressbar()/g' app/src/main/java/com/project/lumina/client/overlay/mods/OverlayNotification.kt

# 修复PacketNotificationOverlay.kt
sed -i 's/PColorGradient1/PColorGradient1()/g' app/src/main/java/com/project/lumina/client/overlay/mods/PacketNotificationOverlay.kt
sed -i 's/PColorGradient2/PColorGradient2()/g' app/src/main/java/com/project/lumina/client/overlay/mods/PacketNotificationOverlay.kt
sed -i 's/PBackground/PBackground()/g' app/src/main/java/com/project/lumina/client/overlay/mods/PacketNotificationOverlay.kt

# 修复SpeedometerOverlay.kt
sed -i 's/SMeterBase/SMeterBase()/g' app/src/main/java/com/project/lumina/client/overlay/mods/SpeedometerOverlay.kt
sed -i 's/SMeterAccent/SMeterAccent()/g' app/src/main/java/com/project/lumina/client/overlay/mods/SpeedometerOverlay.kt
sed -i 's/SMeterBg/SMeterBg()/g' app/src/main/java/com/project/lumina/client/overlay/mods/SpeedometerOverlay.kt
sed -i 's/SMiniLineGrpah/SMiniLineGrpah()/g' app/src/main/java/com/project/lumina/client/overlay/mods/SpeedometerOverlay.kt

# 修复TopCenterOverlayNotification.kt
sed -i 's/TCOGradient1/TCOGradient1()/g' app/src/main/java/com/project/lumina/client/overlay/mods/TopCenterOverlayNotification.kt
sed -i 's/TCOGradient2/TCOGradient2()/g' app/src/main/java/com/project/lumina/client/overlay/mods/TopCenterOverlayNotification.kt
sed -i 's/TCOBackground/TCOBackground()/g' app/src/main/java/com/project/lumina/client/overlay/mods/TopCenterOverlayNotification.kt
sed -i 's/TextColorForModules/TextColorForModules()/g' app/src/main/java/com/project/lumina/client/overlay/mods/TopCenterOverlayNotification.kt

# 修复AnimatedBackground.kt
sed -i 's/LBg1/LBg1()/g' app/src/main/java/com/project/lumina/client/router/launch/AnimatedBackground.kt
sed -i 's/LBg2/LBg2()/g' app/src/main/java/com/project/lumina/client/router/launch/AnimatedBackground.kt
sed -i 's/LBlobColor1/LBlobColor1()/g' app/src/main/java/com/project/lumina/client/router/launch/AnimatedBackground.kt
sed -i 's/LBlobColor2/LBlobColor2()/g' app/src/main/java/com/project/lumina/client/router/launch/AnimatedBackground.kt

# 修复ElevatedCardX.kt
sed -i 's/EColorCard1/EColorCard1()/g' app/src/main/java/com/project/lumina/client/ui/component/ElevatedCardX.kt
sed -i 's/EColorCard2/EColorCard2()/g' app/src/main/java/com/project/lumina/client/ui/component/ElevatedCardX.kt
sed -i 's/EColorCard3/EColorCard3()/g' app/src/main/java/com/project/lumina/client/ui/component/ElevatedCardX.kt

# 修复ModuleCardX.kt
sed -i 's/MColorCard1/MColorCard1()/g' app/src/main/java/com/project/lumina/client/ui/component/ModuleCardX.kt
sed -i 's/MColorCard2/MColorCard2()/g' app/src/main/java/com/project/lumina/client/ui/component/ModuleCardX.kt
sed -i 's/MColorCard3/MColorCard3()/g' app/src/main/java/com/project/lumina/client/ui/component/ModuleCardX.kt

# 修复ModuleSettingsScreen.kt
sed -i 's/MColorScreen1/MColorScreen1()/g' app/src/main/java/com/project/lumina/client/ui/component/ModuleSettingsScreen.kt
sed -i 's/MColorScreen2/MColorScreen2()/g' app/src/main/java/com/project/lumina/client/ui/component/ModuleSettingsScreen.kt

# 修复NavigationRailItemX.kt
sed -i 's/NColorItem1/NColorItem1()/g' app/src/main/java/com/project/lumina/client/ui/component/NavigationRailItemX.kt
sed -i 's/NColorItem2/NColorItem2()/g' app/src/main/java/com/project/lumina/client/ui/component/NavigationRailItemX.kt
sed -i 's/NColorItem3/NColorItem3()/g' app/src/main/java/com/project/lumina/client/ui/component/NavigationRailItemX.kt

# 修复NavigationRailItemY.kt
sed -i 's/NColorItem4/NColorItem4()/g' app/src/main/java/com/project/lumina/client/ui/component/NavigationRailItemY.kt
sed -i 's/NColorItem5/NColorItem5()/g' app/src/main/java/com/project/lumina/client/ui/component/NavigationRailItemY.kt
sed -i 's/NColorItem6/NColorItem6()/g' app/src/main/java/com/project/lumina/client/ui/component/NavigationRailItemY.kt

# 修复NavigationRailY.kt
sed -i 's/NColorItem7/NColorItem7()/g' app/src/main/java/com/project/lumina/client/ui/component/NavigationRailY.kt

# 修复PackItem.kt
sed -i 's/PColorItem1/PColorItem1()/g' app/src/main/java/com/project/lumina/client/ui/component/PackItem.kt

# 修复ServerSelector.kt
sed -i 's/PColorItem1/PColorItem1()/g' app/src/main/java/com/project/lumina/client/ui/component/ServerSelector.kt

echo "配色函数调用修复完成！" 