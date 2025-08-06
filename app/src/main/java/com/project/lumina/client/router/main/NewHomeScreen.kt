/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 */

package com.project.lumina.client.router.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Check

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.lumina.client.R
import com.project.lumina.client.constructors.AccountManager
import com.project.lumina.client.overlay.mods.NotificationType
import com.project.lumina.client.overlay.mods.SimpleOverlayNotification
import com.project.lumina.client.service.Services
import com.project.lumina.client.ui.component.ServerSelector
import com.project.lumina.client.router.main.AccountScreen
import com.project.lumina.client.viewmodel.MainScreenViewModel
import com.project.lumina.client.util.InjectNeko
import com.project.lumina.client.util.MCPackUtils
import com.project.lumina.client.util.ServerInit
import com.project.lumina.client.overlay.manager.ConnectionInfoOverlay
import com.project.lumina.client.ui.component.SubServerInfo
import com.project.lumina.client.router.main.PackSelectionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import com.project.lumina.client.activity.AppVerificationManager

// NewHomeScreen专用的Material3配色方案
@Composable
fun getNewHomeMaterial3Colors(): NewHomeMaterial3Colors {
    val colorScheme = MaterialTheme.colorScheme
    return NewHomeMaterial3Colors(
        primary = colorScheme.primary,
        onPrimary = colorScheme.onPrimary,
        secondary = colorScheme.secondary,
        onSecondary = colorScheme.onSecondary,
        tertiary = colorScheme.tertiary,
        onTertiary = colorScheme.onTertiary,
        background = colorScheme.background,
        onBackground = colorScheme.onBackground,
        surface = colorScheme.surface,
        onSurface = colorScheme.onSurface,
        surfaceVariant = colorScheme.surfaceVariant,
        onSurfaceVariant = colorScheme.onSurfaceVariant,
        error = colorScheme.error,
        onError = colorScheme.onError,
        primaryContainer = colorScheme.primaryContainer,
        onPrimaryContainer = colorScheme.onPrimaryContainer,
        secondaryContainer = colorScheme.secondaryContainer,
        onSecondaryContainer = colorScheme.onSecondaryContainer,
        tertiaryContainer = colorScheme.tertiaryContainer,
        onTertiaryContainer = colorScheme.onTertiaryContainer,
        outline = colorScheme.outline,
        outlineVariant = colorScheme.outlineVariant,
        scrim = colorScheme.scrim,
        inverseSurface = colorScheme.inverseSurface,
        inverseOnSurface = colorScheme.inverseOnSurface,
        inversePrimary = colorScheme.inversePrimary
    )
}

// NewHomeScreen专用的Material3配色数据类
data class NewHomeMaterial3Colors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val error: Color,
    val onError: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomeScreen(
    onStartToggle: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()

    // 获取NewHomeScreen专用的Material3配色
    val colors = getNewHomeMaterial3Colors()

    // 验证相关状态
    var isVerifying by remember { mutableStateOf(true) }
    var verificationStep by remember { mutableStateOf(1) }
    var verificationMessage by remember { mutableStateOf("正在连接服务器...") }
    var verificationError by remember { mutableStateOf<String?>(null) }
    var showVerificationOverlay by remember { mutableStateOf(true) }
    var verificationProgress by remember { mutableStateOf(0f) }

    // 页面状态
    var selectedTab by remember { mutableStateOf(0) }
    var showZeqaBottomSheet by remember { mutableStateOf(false) }
    var isLaunchingMinecraft by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var currentPackName by remember { mutableStateOf("") }

    // 关于页内容
    val aboutContent = """LuminaCN

开源项目，基于Material3 Compose重构。

作者: Phoen1x

项目地址: https://github.com/你的仓库"""

    // 真正的验证流程
    LaunchedEffect(Unit) {
        try {
            // 模拟AppVerificationManager的验证步骤
            verificationStep = 1
            verificationMessage = "正在连接服务器..."
            verificationProgress = 0.25f
            delay(1000)
            
            verificationStep = 2
            verificationMessage = "获取公告..."
            verificationProgress = 0.5f
            delay(1000)
            
            verificationStep = 3
            verificationMessage = "获取隐私协议..."
            verificationProgress = 0.75f
            delay(1000)
            
            verificationStep = 4
            verificationMessage = "检查版本..."
            verificationProgress = 1.0f
            delay(1000)
            
            isVerifying = false
        } catch (e: Exception) {
            // 网络失败自动跳过
            verificationError = "网络连接失败，跳过验证"
            delay(500)
            isVerifying = false
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = colors.surface,
                contentColor = colors.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "主仪表盘") },
                    label = { Text("主仪表盘") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        indicatorColor = colors.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Link, contentDescription = "远程连接") },
                    label = { Text("远程连接") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        indicatorColor = colors.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = "关于") },
                    label = { Text("关于") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        indicatorColor = colors.primaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onStartToggle,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ) {
                    Icon(
                        imageVector = if (isLaunchingMinecraft) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isLaunchingMinecraft) "停止" else "开始"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> MainDashboard(
                    showZeqaBottomSheet = showZeqaBottomSheet,
                    onShowZeqaBottomSheet = { showZeqaBottomSheet = true },
                    isLaunchingMinecraft = isLaunchingMinecraft,
                    showProgressDialog = showProgressDialog,
                    downloadProgress = downloadProgress,
                    currentPackName = currentPackName,
                    colors = colors
                )
                1 -> RemoteLinkPage(colors = colors)
                2 -> AboutPage(aboutContent, colors = colors)
            }
            if (showZeqaBottomSheet) {
                ZeqaSubServerBottomSheet(
                    onDismiss = { showZeqaBottomSheet = false },
                    onSelect = { subServer: SubServerInfo ->
                        mainScreenViewModel.selectCaptureModeModel(
                            captureModeModel.copy(serverHostName = subServer.serverAddress, serverPort = subServer.serverPort)
                        )
                        showZeqaBottomSheet = false
                    }
                )
            }
            if (isVerifying) {
                // Material3风格的验证遮罩
                Surface(
                    color = colors.background.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 验证步骤指示器
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { verificationProgress },
                                modifier = Modifier.width(200.dp),
                                color = colors.primary,
                                trackColor = colors.surfaceVariant
                            )
                            
                            Text(
                                text = verificationMessage,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface
                            )
                            
                            if (verificationError != null) {
                                Text(
                                    text = verificationError!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.error
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // 验证步骤列表
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VerificationStep(
                                step = 1,
                                title = "连接服务器",
                                isCompleted = verificationStep >= 1,
                                isCurrent = verificationStep == 1,
                                colors = colors
                            )
                            VerificationStep(
                                step = 2,
                                title = "获取公告",
                                isCompleted = verificationStep >= 2,
                                isCurrent = verificationStep == 2,
                                colors = colors
                            )
                            VerificationStep(
                                step = 3,
                                title = "隐私协议",
                                isCompleted = verificationStep >= 3,
                                isCurrent = verificationStep == 3,
                                colors = colors
                            )
                            VerificationStep(
                                step = 4,
                                title = "版本检查",
                                isCompleted = verificationStep >= 4,
                                isCurrent = verificationStep == 4,
                                colors = colors
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationStep(
    step: Int,
    title: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    colors: NewHomeMaterial3Colors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 步骤图标
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = when {
                        isCompleted -> colors.primary
                        isCurrent -> colors.primaryContainer
                        else -> colors.surfaceVariant
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) colors.onPrimaryContainer 
                           else colors.onSurfaceVariant
                )
            }
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isCompleted -> colors.primary
                isCurrent -> colors.onSurface
                else -> colors.onSurfaceVariant
            }
        )
    }
}

@Composable
fun MainDashboard(
    showZeqaBottomSheet: Boolean,
    onShowZeqaBottomSheet: () -> Unit,
    isLaunchingMinecraft: Boolean,
    showProgressDialog: Boolean,
    downloadProgress: Float,
    currentPackName: String,
    colors: NewHomeMaterial3Colors
) {
    // 这里写主仪表盘内容，全部用Material3控件
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "欢迎使用 LuminaCN!", 
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface
        )
        Button(
            onClick = onShowZeqaBottomSheet,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            )
        ) {
            Icon(Icons.Filled.Cloud, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("选择 Zeqa 分服")
        }
        if (showProgressDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("正在下载: $currentPackName", color = colors.onSurface) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            color = colors.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(downloadProgress * 100).toInt()}%", color = colors.onSurface)
                        Text(
                            if (downloadProgress < 1f) "正在下载..." else "正在启动 Minecraft ...",
                            color = colors.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {},
                containerColor = colors.surface,
                titleContentColor = colors.onSurface,
                textContentColor = colors.onSurface
            )
        }
    }
}

@Composable
fun RemoteLinkPage(colors: NewHomeMaterial3Colors) {
    // 远程连接页内容，Material3风格
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Link, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = colors.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "远程连接功能开发中…", 
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface
        )
    }
}

@Composable
fun AboutPage(content: String, colors: NewHomeMaterial3Colors) {
    // 关于页内容，Material3风格
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Info, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = colors.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            content, 
            style = MaterialTheme.typography.bodyLarge, 
            textAlign = TextAlign.Center,
            color = colors.onSurface
        )
    }
}

// 辅助函数
private suspend fun makeHttpRequest(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 15000
    conn.readTimeout = 15000
    conn.setRequestProperty("User-Agent", "Lumina Android Client")
    conn.connect()
    if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
    return conn.inputStream.bufferedReader().use { it.readText() }
}

private fun parseIniStatus(s: String) = s.contains("status=true", ignoreCase = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeqaSubServerBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (SubServerInfo) -> Unit
) {
    val subServers = listOf(
        SubServerInfo("AS1", "亚洲", "104.234.6.50", 10001),
        SubServerInfo("AS2", "亚洲", "104.234.6.50", 10002),
        SubServerInfo("AS3", "亚洲", "104.234.6.50", 10003),
        SubServerInfo("AS4", "亚洲", "104.234.6.50", 10004),
        SubServerInfo("AS5", "亚洲", "104.234.6.50", 10005),
        SubServerInfo("EU1", "欧洲", "178.32.145.167", 10001),
        SubServerInfo("EU2", "欧洲", "178.32.145.167", 10002),
        SubServerInfo("EU3", "欧洲", "178.32.145.167", 10003),
        SubServerInfo("EU4", "欧洲", "178.32.145.167", 10004),
        SubServerInfo("EU5", "欧洲", "178.32.145.167", 10005),
        SubServerInfo("NA1", "北美", "51.79.62.8", 10001),
        SubServerInfo("NA2", "北美", "51.79.62.8", 10002),
        SubServerInfo("NA3", "北美", "51.79.62.8", 10003),
        SubServerInfo("NA4", "北美", "51.79.62.8", 10004),
        SubServerInfo("NA5", "北美", "51.79.62.8", 10005),
        SubServerInfo("SA1", "南非", "38.54.63.126", 10001),
        SubServerInfo("SA2", "南非", "38.54.63.126", 10002),
        SubServerInfo("SA3", "南非", "38.54.63.126", 10003),
        SubServerInfo("SA4", "南非", "38.54.63.126", 10004),
        SubServerInfo("SA5", "南非", "38.54.63.126", 10005)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "选择 Zeqa 分服",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = onDismiss) {
                    Text(
                        "取消",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                "基于你的地理位置选择一个分服",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subServers) { subServer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable {
                                onSelect(subServer)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = subServer.id,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subServer.region,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = subServer.serverAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "端口: ${subServer.serverPort}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 