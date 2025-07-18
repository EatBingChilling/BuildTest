package com.project.lumina.client.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.material.R as MaterialR // 解决Material资源冲突
import com.project.lumina.R // 关键导包！没有这个全完蛋
import com.project.lumina.client.ui.theme.LuminaClientTheme
import com.project.lumina.client.util.HashCat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

// 暴躁版本检查员 + 凤凰验证终极修复版
class VersionCheckerActivity : ComponentActivity() {

    // 声明个验证管理器 不给activity就耍赖
    private lateinit var verificationManager: AppVerificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 关键修复：在super前设置主题！！！
        setTheme(R.style.Theme_Phoenix) // 用你的Phoenix主题
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // 全面屏适配 不写你手机上下黑边别赖我
        
        // 直接开整验证 不搞花里胡哨加载动画了
        setContent {
            LuminaClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 空壳 等验证完直接跳主页
                    LoadingPlaceholder()
                }
            }
        }

        // 创建验证管理器 带暴躁回调的那种
        verificationManager = AppVerificationManager(this, object : AppVerificationManager.VerificationCallback {
            override fun onSuccess() {
                // 验证成功 赶紧初始化
                CoroutineScope(Dispatchers.Main).launch {
                    initializeApp()
                    startMainActivity()
                }
            }

            override fun onFailure() {
                // 验证失败 滚回桌面
                Toast.makeText(this@VersionCheckerActivity, "验证失败 请加QQ 2284257190续命", Toast.LENGTH_LONG).show()
                finish()
            }
        })

        // 开干验证流程
        verificationManager.startVerification()
    }

    // 初始化耗时操作 放子线程不然ANP弹你脸上
    private suspend fun initializeApp() = withContext(Dispatchers.IO) {
        // 这里没给HashCat实现？主播你是真滴狗
        val kson = HashCat.getInstance()
        kson.LintHashInit(this@VersionCheckerActivity)  // 这函数名起得跟屎一样
        
        // 假装在检查版本 其实毛都没干 主播赶紧补逻辑
        delay(666)  // 象征性等会儿 不然加载动画闪退
    }

    // 跳主页 简单到有手就行
    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // 清栈防乱跳
        })
        finish()  // 结束自己 不然后退键回来又是一波加载
    }

    override fun onDestroy() {
        super.onDestroy()
        verificationManager.onDestroy()  // 清理资源 防止内存泄露
    }

    // 简易加载占位符 就转个圈吧
    @Composable
    fun LoadingPlaceholder() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // 旋转动画 转慢了别BB
            val infiniteTransition = rememberInfiniteTransition()
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),  // 线性动画 嫌卡自己改曲线
                    repeatMode = RepeatMode.Restart
                )
            )
            
            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer { rotationZ = rotation },
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    }
}

// 凤凰验证管理器全Kotlin版 终极修复
class AppVerificationManager(
    private val activity: ComponentActivity,
    private val callback: VerificationCallback
) {
    // 服务器地址 欠费记得续租
    private val BASE_URL = "http://110.42.63.51:39078/d/apps"
    private val PREFS_NAME = "app_verification_prefs"
    private val KEY_NOTICE_HASH = "notice_content_hash"
    private val KEY_PRIVACY_HASH = "privacy_content_hash"

    // 系统组件
    private var verificationDialog: android.app.AlertDialog? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val prefs: android.content.SharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // UI组件
    private var blockerOverlay: android.view.View? = null  // 遮罩层

    // 验证回调接口
    interface VerificationCallback {
        fun onSuccess()
        fun onFailure()
    }

    // 开始验证大冒险！
    fun startVerification() {
        // 1. 禁用界面：盖个半透明"被子"（遮罩层）
        blockUIInteraction()
        
        // 3. 开始闯关！
        startStep1()
    }

    // 盖遮罩层：比递归禁用视图更高效（不会卡死UI）
    private fun blockUIInteraction() {
        val root = activity.window.decorView as android.view.ViewGroup
        blockerOverlay = android.view.View(activity).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0x22000000) // 半透明黑
            setOnTouchListener { _, _ -> true }  // 拦截所有触摸
        }
        root.addView(blockerOverlay)
    }

    // 移除遮罩层：掀开被子让用户嗨皮
    private fun unblockUIInteraction() {
        blockerOverlay?.let {
            val root = activity.window.decorView as android.view.ViewGroup
            root.removeView(it)
            blockerOverlay = null
        }
    }

    // 第一步：验证应用序列号（服务器说行才行）
    private fun startStep1() {
        executor.execute {
            try {
                val response = makeHttpRequest("$BASE_URL/appstatus/a.ini")
                mainHandler.post {
                    if (parseIniStatus(response)) {
                        startStep2()
                    } else {
                        showRetryDialog("序列号验证失败", "请联系开发，没交月租，月租20元。交钱请加QQ 2284257190。") {
                            startStep1()
                        }
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    showRetryDialog("网络死掉了。", "可能服务器一起爆了。") {
                        startStep1()
                    }
                }
            }
        }
    }

    // 第二步：公告（有新公告才弹窗）
    private fun startStep2() {
        executor.execute {
            try {
                val response = makeHttpRequest("$BASE_URL/title/a.json")
                mainHandler.post {
                    try {
                        val json = JSONObject(response)
                        val title = json.getString("title")
                        val subtitle = json.getString("subtitle")
                        val content = json.getString("content")

                        val contentHash = getSHA256Hash(response)
                        val savedHash = prefs.getString(KEY_NOTICE_HASH, "")

                        if (!contentHash.equals(savedHash)) {
                            showNoticeDialog(title, subtitle, content, contentHash)
                        } else {
                            startStep3()
                        }
                    } catch (e: Exception) {
                        startStep3() // 继续下一步
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    startStep3() // 心大，继续走
                }
            }
        }
    }

    // 第三步：隐私协议（拒绝就撒娇）
    private fun startStep3() {
        executor.execute {
            try {
                val response = makeHttpRequest("$BASE_URL/privary/a.txt")
                mainHandler.post {
                    val contentHash = getSHA256Hash(response)
                    val savedHash = prefs.getString(KEY_PRIVACY_HASH, "")

                    if (!contentHash.equals(savedHash)) {
                        showPrivacyDialog(response, contentHash)
                    } else {
                        startStep4()
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    startStep4() // 继续
                }
            }
        }
    }

    // 第四步：版本更新（使用版本代码进行整数比较）
    private fun startStep4() {
        executor.execute {
            try {
                val response = makeHttpRequest("$BASE_URL/update/a.json")
                mainHandler.post {
                    try {
                        val json = JSONObject(response)
                        // 获取云端版本代码（整数）
                        val cloudVersion = json.getLong("version")
                        // 获取本地版本代码
                        val localVersion = getLocalVersionCode()

                        // 直接比较整数版本代码
                        if (cloudVersion > localVersion) {
                            showUpdateDialog(
                                json.getString("name"),
                                cloudVersion.toString(), // 转换为字符串显示
                                json.getString("update_content")
                            )
                        } else {
                            completeVerification()
                        }
                    } catch (e: Exception) {
                        completeVerification()
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    completeVerification()
                }
            }
        }
    }

    // ===================== 对话框方法 =====================
    
    // 公告对话框（必须点"朕已阅"）
    private fun showNoticeDialog(title: String, subtitle: String, content: String, contentHash: String) {
        MaterialAlertDialogBuilder(activity, MaterialR.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(title)
            .setMessage("$subtitle\n\n$content")
            .setPositiveButton("朕已阅") { _, _ ->
                prefs.edit().putString(KEY_NOTICE_HASH, contentHash).apply()
                startStep3()
            }
            .setCancelable(false)
            .show()
    }

    // 隐私协议对话框（拒绝就撒娇）
    private fun showPrivacyDialog(privacyContent: String, contentHash: String) {
        MaterialAlertDialogBuilder(activity, MaterialR.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("隐私协议")
            .setMessage(privacyContent)
            .setPositiveButton("同意") { _, _ ->
                prefs.edit().putString(KEY_PRIVACY_HASH, contentHash).apply()
                startStep4()
            }
            .setNegativeButton("拒绝") { _, _ ->
                MaterialAlertDialogBuilder(activity, MaterialR.style.ThemeOverlay_Material3_MaterialAlertDialog)
                    .setTitle("牢大别肘。")
                    .setMessage("侃爷韦斯特特特特特你怎么回事呢呢呢呢呢我到你家去去去去去你脑子瓦特了了了了了")
                    .setPositiveButton("签。") { _, _ -> 
                        showPrivacyDialog(privacyContent, contentHash)
                    }
                    .setNegativeButton("不签。") { _, _ -> 
                        activity.finish()
                    }
                    .show()
            }
            .show()
    }

    // 更新对话框
    private fun showUpdateDialog(name: String, version: String, updateContent: String) {
        MaterialAlertDialogBuilder(activity, MaterialR.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("发现新版本")
            .setMessage("$name v$version\n\n更新内容：\n$updateContent")
            .setPositiveButton("立即更新") { _, _ ->
                // 这里可以添加实际更新逻辑
                Toast.makeText(activity, "正在打开凤凰云盘", Toast.LENGTH_SHORT).show()
                startWebviewDownloader("110.42.63.51:39078/apps/apks")
            }
            .setNegativeButton("下次再说") { _, _ ->
                completeVerification()
            }
            .show()
    }

    // 重试对话框（网络失败时卖萌）
    private fun showRetryDialog(title: String, message: String, retryAction: () -> Unit) {
        MaterialAlertDialogBuilder(activity, MaterialR.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(title)
            .setMessage("$message\n\n不一定是服务器的问题。")
            .setPositiveButton("重试") { _, _ -> retryAction() }
            .setNegativeButton("退出") { _, _ ->
                Toast.makeText(activity, "凤凰验证 - 验证失败", Toast.LENGTH_SHORT).show()
                callback.onFailure()
            }
            .show()
    }

    // 完成验证：放鞭炮庆祝！
    private fun completeVerification() {
        verificationDialog?.dismiss()
        unblockUIInteraction()
        Toast.makeText(activity, "凤凰验证 - 应用序列号验证完成", Toast.LENGTH_SHORT).show()
        callback.onSuccess()
    }

    // ===================== 工具方法 =====================
    
    // 网络请求（带超时，防止鸽子飞丢）
    @Throws(IOException::class)
    private fun makeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000 // 8秒超时（鸽子不能太慢）
        conn.readTimeout = 8000

        if (conn.responseCode != 200) {
            throw IOException("HTTP ${conn.responseCode}")
        }

        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    // 解析INI（只认status=true）
    private fun parseIniStatus(iniContent: String): Boolean {
        return iniContent.contains("status=true") // 粗暴但有效
    }

    // SHA256替代MD5（更安全）
    private fun getSHA256Hash(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(input.toByteArray())
            val hex = StringBuilder()
            for (b in hash) {
                val hexByte = Integer.toHexString(0xff and b.toInt())
                if (hexByte.length == 1) hex.append('0')
                hex.append(hexByte)
            }
            hex.toString()
        } catch (e: Exception) {
            "hash_failed_${System.currentTimeMillis()}" // 随便返回个值
        }
    }
    
    private fun startWebviewDownloader(urlPath: String) {
        val intent = Intent(activity, com.project.lumina.client.WebviewDownloader::class.java)
        intent.putExtra("url", "http://$urlPath")
        activity.startActivity(intent)
    }

    // 获取本地版本代码（整数）
    private fun getLocalVersionCode(): Long {
        return try {
            val pkg = activity.packageManager.getPackageInfo(activity.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            1 // 默认版本代码
        }
    }

    // 退出时清理后台线程（防止内存泄漏）
    fun onDestroy() {
        executor.shutdownNow()
        verificationDialog?.dismiss()
    }
}
