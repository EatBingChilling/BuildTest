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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
    
    // 主页状态
    var selectedTab by remember { mutableStateOf(0) }
    var showZeqaBottomSheet by remember { mutableStateOf(false) }
    var isLaunchingMinecraft by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var currentPackName by remember { mutableStateOf("") }
    
    val sharedPreferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
    var InjectNekoPack by remember {
        mutableStateOf(sharedPreferences.getBoolean("injectNekoPackEnabled", false))
    }
    
    val localIp = remember { ConnectionInfoOverlay.getLocalIpAddress(context) }
    val showNotification: (String, NotificationType) -> Unit = { message, type ->
        SimpleOverlayNotification.show(
            message = message,
            type = type,
            durationMs = 3000
        )
    }
    
    // 验证函数
    suspend fun performVerification() {
        try {
            // 步骤1: 连接服务器
            verificationStep = 1
            verificationMessage = "正在连接服务器..."
            
            val response = makeHttpRequest("http://110.42.63.51:39078/d/apps/appstatus/a.ini")
            if (!parseIniStatus(response)) {
                verificationError = "应用状态验证失败"
                return
            }
            
            // 步骤2: 获取公告
            verificationStep = 2
            verificationMessage = "获取公告..."
            try {
                val noticeResponse = makeHttpRequest("http://110.42.63.51:39078/d/apps/title/a.json")
                // 这里可以处理公告，暂时跳过
            } catch (e: Exception) {
                // 公告获取失败，继续
            }
            
            // 步骤3: 获取隐私协议
            verificationStep = 3
            verificationMessage = "获取隐私协议..."
            try {
                val privacyResponse = makeHttpRequest("http://110.42.63.51:39078/d/apps/privary/a.txt")
                // 这里可以处理隐私协议，暂时跳过
            } catch (e: Exception) {
                // 隐私协议获取失败，继续
            }
            
            // 步骤4: 检查版本
            verificationStep = 4
            verificationMessage = "检查版本..."
            try {
                val updateResponse = makeHttpRequest("http://110.42.63.51:39078/d/apps/update/a.json")
                // 这里可以处理版本检查，暂时跳过
            } catch (e: Exception) {
                // 版本检查失败，继续
            }
            
            // 验证完成
            verificationStep = 5
            verificationMessage = "验证完成"
            delay(500)
            isVerifying = false
            showVerificationOverlay = false
            
        } catch (e: Exception) {
            verificationError = "网络连接失败，跳过验证"
            delay(1000)
            isVerifying = false
            showVerificationOverlay = false
        }
    }
    
    // 启动验证
    LaunchedEffect(Unit) {
        performVerification()
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 主内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 顶部欢迎区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Column {
                        Text(
                            text = "你好!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = AccountManager.currentAccount?.remark ?: "请选择账户",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // 选项卡
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("服务器", "账户", "材质包").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 内容区域
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> ServerSelector(
                        onShowZeqaBottomSheet = { showZeqaBottomSheet = true }
                    )
                    1 -> AccountScreen(showNotification)
                    2 -> PacksScreen()
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 启动按钮
            AnimatedContent(
                targetState = Services.isActive,
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                },
                label = "startButton"
            ) { isActive ->
                if (isActive) {
                    Button(
                        onClick = {
                            isLaunchingMinecraft = false
                            onStartToggle()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = "停止",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.stop),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = {
                            scope.launch {
                                delay(100)
                                isLaunchingMinecraft = true
                                Services.isLaunchingMinecraft = true
                                onStartToggle()
                                
                                delay(2500)
                                if (!Services.isActive) {
                                    isLaunchingMinecraft = false
                                    Services.isLaunchingMinecraft = false
                                    return@launch
                                }
                                
                                val selectedGame = mainScreenViewModel.selectedGame.value
                                if (selectedGame != null) {
                                    val intent = context.packageManager.getLaunchIntentForPackage(selectedGame)
                                    if (intent != null && Services.isActive) {
                                        context.startActivity(intent)
                                        
                                        delay(3000)
                                        if (Services.isActive) {
                                            val disableConnectionInfoOverlay = sharedPreferences.getBoolean("disableConnectionInfoOverlay", false)
                                            if (!disableConnectionInfoOverlay) {
                                                ConnectionInfoOverlay.show(localIp)
                                            }
                                        }
                                        isLaunchingMinecraft = false
                                        Services.isLaunchingMinecraft = false
                                        
                                        try {
                                            when {
                                                InjectNekoPack == true && PackSelectionManager.selectedPack != null -> {
                                                    PackSelectionManager.selectedPack?.let { selectedPack ->
                                                        currentPackName = selectedPack.name
                                                        showProgressDialog = true
                                                        downloadProgress = 0f

                                                        try {
                                                            MCPackUtils.downloadAndOpenPack(
                                                                context,
                                                                selectedPack
                                                            ) { progress ->
                                                                downloadProgress = progress
                                                            }
                                                            showProgressDialog = false
                                                        } catch (e: Exception) {
                                                            showProgressDialog = false
                                                            showNotification(
                                                                "材质包下载失败: ${e.message}",
                                                                NotificationType.ERROR
                                                            )
                                                        }
                                                    }
                                                }

                                                InjectNekoPack == true -> {
                                                    try {
                                                        InjectNeko.injectNeko(
                                                            context = context,
                                                            onProgress = {
                                                                // 处理进度
                                                            }
                                                        )
                                                    } catch (e: Exception) {
                                                        showNotification(
                                                            "Neko 注入失败: ${e.message}",
                                                            NotificationType.ERROR
                                                        )
                                                    }
                                                }

                                                else -> {
                                                    if (selectedGame == "com.mojang.minecraftpe") {
                                                        try {
                                                            ServerInit.addMinecraftServer(
                                                                context,
                                                                localIp
                                                            )
                                                        } catch (e: Exception) {
                                                            showNotification(
                                                                "服务器初始化失败: ${e.message}",
                                                                NotificationType.ERROR
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            showNotification(
                                                "一个未预料的错误发生: ${e.message}",
                                                NotificationType.ERROR
                                            )
                                        }
                                    } else {
                                        showNotification(
                                            "游戏启动失败，请检查是否安装 Minecraft 或在 App 管理器中正确添加了客户端",
                                            NotificationType.ERROR
                                        )
                                    }
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.start),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }
        }
        
        // 验证覆盖层
        AnimatedVisibility(
            visible = showVerificationOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "应用验证",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = verificationMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (verificationError != null) {
                            Text(
                                text = verificationError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        // 验证步骤指示器
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(4) { step ->
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = CircleShape,
                                    color = if (step < verificationStep) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                ) { }
                            }
                        }
                    }
                }
            }
        }
        
        // 进度对话框
        if (showProgressDialog) {
            Dialog(onDismissRequest = { /* Prevent dismissal during download */ }) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentSize()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "正在下载: $currentPackName",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (downloadProgress < 1f) "正在下载..." else "正在启动 Minecraft ...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
    
    // Zeqa 子服务器底部表单
    if (showZeqaBottomSheet) {
        ZeqaSubServerBottomSheet(
            onDismiss = { showZeqaBottomSheet = false },
            onSelect = { subServer ->
                mainScreenViewModel.selectCaptureModeModel(
                    captureModeModel.copy(serverHostName = subServer.serverAddress, serverPort = subServer.serverPort)
                )
                showZeqaBottomSheet = false
            }
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