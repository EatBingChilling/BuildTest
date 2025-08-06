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
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.*
import androidx.compose.material3.icons.rounded.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomeScreen(
    onStartToggle: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsState()

    // 验证相关状态
    var isVerifying by remember { mutableStateOf(true) }
    var verificationStep by remember { mutableStateOf(1) }
    var verificationMessage by remember { mutableStateOf("正在连接服务器...") }
    var verificationError by remember { mutableStateOf<String?>(null) }
    var showVerificationOverlay by remember { mutableStateOf(true) }

    // 页面状态
    var selectedTab by remember { mutableStateOf(0) }
    var showZeqaBottomSheet by remember { mutableStateOf(false) }
    var isLaunchingMinecraft by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var currentPackName by remember { mutableStateOf("") }

    // 关于页内容
    val aboutContent = "LuminaCN

开源项目，基于Material3 Compose重构。\n\n作者: Phoen1x\n\n项目地址: https://github.com/你的仓库"

    // 验证流程（Material3风格，网络失败自动跳过）
    LaunchedEffect(Unit) {
        try {
            // 这里调用AppVerificationManager的验证逻辑，伪代码如下：
            // AppVerificationManager(context) { isVerifying = false }
            // 你可以用实际的Compose实现或Dialog实现验证UI
            // 这里只做演示，实际请用Material3控件
            delay(1500)
            isVerifying = false
        } catch (e: Exception) {
            // 网络失败自动跳过
            isVerifying = false
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "主仪表盘") },
                    label = { Text("主仪表盘") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Link, contentDescription = "远程连接") },
                    label = { Text("远程连接") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = "关于") },
                    label = { Text("关于") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onStartToggle,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                    currentPackName = currentPackName
                )
                1 -> RemoteLinkPage()
                2 -> AboutPage(aboutContent)
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
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("应用验证中…", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun MainDashboard(
    showZeqaBottomSheet: Boolean,
    onShowZeqaBottomSheet: () -> Unit,
    isLaunchingMinecraft: Boolean,
    showProgressDialog: Boolean,
    downloadProgress: Float,
    currentPackName: String
) {
    // 这里写主仪表盘内容，全部用Material3控件
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("欢迎使用 LuminaCN!", style = MaterialTheme.typography.headlineMedium)
        Button(
            onClick = onShowZeqaBottomSheet,
            colors = ButtonDefaults.buttonColors()
        ) {
            Icon(Icons.Filled.Cloud, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("选择 Zeqa 分服")
        }
        if (showProgressDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("正在下载: $currentPackName") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(progress = { downloadProgress })
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(downloadProgress * 100).toInt()}%")
                        Text(if (downloadProgress < 1f) "正在下载..." else "正在启动 Minecraft ...")
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun RemoteLinkPage() {
    // 远程连接页内容，Material3风格
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("远程连接功能开发中…", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun AboutPage(content: String) {
    // 关于页内容，Material3风格
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(content, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
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