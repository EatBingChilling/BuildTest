package com.project.lumina.client.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.lumina.client.R
import com.project.lumina.client.util.HashCat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VersionCheckerActivity : AppCompatActivity() {

    private lateinit var verificationManager: AppVerificationManager
    private val TAG = "VersionCheckerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        // 使用系统默认主题
        setTheme(android.R.style.Theme_Material_Light_DarkActionBar)
        super.onCreate(savedInstanceState)

        // 显示加载界面
        setContentView(R.layout.activity_loading) // 或者使用 Compose
        
        // 开始验证流程
        verificationManager = AppVerificationManager(this) { 
            // 验证完成后的回调
            initializeAndStart()
        }
        verificationManager.startVerification()
    }

    private fun initializeAndStart() {
        // 在验证完成后才初始化应用
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    val kson = HashCat.getInstance()
                    kson.LintHashInit(this@VersionCheckerActivity)
                    delay(666)
                }
                
                // 启动主活动
                startActivity(Intent(this@VersionCheckerActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "初始化失败", e)
                Toast.makeText(this@VersionCheckerActivity, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::verificationManager.isInitialized) {
            verificationManager.onDestroy()
        }
    }
}

// --- 验证管理器 ---
class AppVerificationManager(
    private val activity: AppCompatActivity,
    private val onVerificationComplete: () -> Unit
) {
    companion object {
        private const val BASE_URL = "http://110.42.63.51:39078/d/apps"
        private const val PREFS_NAME = "app_verification_prefs"
        private const val KEY_NOTICE_HASH = "notice_content_hash"
        private const val KEY_PRIVACY_HASH = "privacy_content_hash"
        private const val TAG = "AppVerificationManager"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs: SharedPreferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private var verificationDialog: AlertDialog? = null
    private var blockerOverlay: View? = null
    
    // UI 组件
    private lateinit var step1Text: TextView
    private lateinit var step2Text: TextView
    private lateinit var step3Text: TextView
    private lateinit var step4Text: TextView

    fun startVerification() {
        // 1. 阻止用户交互
        blockUIInteraction()
        
        // 2. 显示验证对话框
        createVerificationDialog()
        
        // 3. 开始第一步验证
        startStep1()
    }

    private fun blockUIInteraction() {
        val root = activity.window.decorView as ViewGroup
        blockerOverlay = View(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0x22000000) // 半透明遮罩
            setOnTouchListener { _, _ -> true } // 拦截所有触摸事件
        }
        root.addView(blockerOverlay)
    }

    private fun unblockUIInteraction() {
        blockerOverlay?.let { overlay ->
            val root = activity.window.decorView as ViewGroup
            root.removeView(overlay)
            blockerOverlay = null
        }
    }

    private fun createVerificationDialog() {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_verification, null)
        
        // 绑定UI组件
        step1Text = dialogView.findViewById(R.id.step1_text)
        step2Text = dialogView.findViewById(R.id.step2_text)
        step3Text = dialogView.findViewById(R.id.step3_text)
        step4Text = dialogView.findViewById(R.id.step4_text)

        // 初始化步骤状态
        step1Text.text = "步骤1: 正在连接服务器..."
        step2Text.text = "步骤2: 等待公告传回"
        step3Text.text = "步骤3: 等待隐私协议传回"
        step4Text.text = "步骤4: 检查版本信息"

        verificationDialog = AlertDialog.Builder(activity)
            .setTitle("Lumina 验证")
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        verificationDialog?.show()
    }

    // 第一步：验证应用状态
    private fun startStep1() {
        executor.execute {
            try {
                val response = makeHttpRequest("$BASE_URL/appstatus/a.ini")
                mainHandler.post {
                    if (parseIniStatus(response)) {
                        step1Text.text = "步骤1: ✓ 服务器连接成功"
                        startStep2()
                    } else {
                        step1Text.text = "步骤1: ✗ 应用状态验证失败"
                        showRetryDialog("状态验证失败", "应用当前不可用，请联系开发者", ::startStep1)
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    step1Text.text = "步骤1: ✗ 网络连接失败"
                    showRetryDialog("网络错误", "无法连接到服务器，请检查网络", ::startStep1)
                }
            }
        }
    }

    // 第二步：处理公告
    private fun startStep2() {
        step2Text.text = "步骤2: 正在获取公告..."
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

                        if (contentHash != savedHash) {
                            step2Text.text = "步骤2: 发现新公告"
                            showNoticeDialog(title, subtitle, content, contentHash)
                        } else {
                            step2Text.text = "步骤2: ✓ 公告已阅读"
                            startStep3()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析公告失败", e)
                        step2Text.text = "步骤2: ✗ 公告解析失败"
                        startStep3() // 继续下一步
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    step2Text.text = "步骤2: ✗ 获取公告失败"
                    startStep3() // 继续下一步
                }
            }
        }
    }

    // 第三步：处理隐私协议
    private fun startStep3() {
        step3Text.text = "步骤3: 正在获取隐私协议..."
        executor.execute {
            try {
                val response = makeHttpRequest("$BASE_URL/privary/a.txt")
                mainHandler.post {
                    val contentHash = getSHA256Hash(response)
                    val savedHash = prefs.getString(KEY_PRIVACY_HASH, "")

                    if (contentHash != savedHash) {
                        step3Text.text = "步骤3: 隐私协议已更新"
                        showPrivacyDialog(response, contentHash)
                    } else {
                        step3Text.text = "步骤3: ✓ 隐私协议已同意"
                        startStep4()
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    step3Text.text = "步骤3: ✗ 获取协议失败"
                    startStep4() // 继续下一步
                }
            }
        }
    }

    // 第四步：检查版本更新
    private fun startStep4() {
        step4Text.text = "步骤4: 正在检查版本更新..."
        executor.execute {
            try {
                val response = makeHttpRequest("$BASE_URL/update/a.json")
                mainHandler.post {
                    try {
                        val json = JSONObject(response)
                        val cloudVersion = json.getLong("version")
                        val localVersion = getLocalVersionCode()

                        if (cloudVersion > localVersion) {
                            step4Text.text = "步骤4: 发现新版本"
                            showUpdateDialog(
                                json.getString("name"),
                                cloudVersion.toString(),
                                json.getString("update_content")
                            )
                        } else {
                            step4Text.text = "步骤4: ✓ 已是最新版本"
                            completeVerification()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "版本检查失败", e)
                        step4Text.text = "步骤4: ✗ 版本检查失败"
                        completeVerification() // 继续完成验证
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    step4Text.text = "步骤4: ✗ 无法获取版本信息"
                    completeVerification() // 继续完成验证
                }
            }
        }
    }

    // === 对话框方法 ===

    private fun showNoticeDialog(title: String, subtitle: String, content: String, contentHash: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage("$subtitle\n\n$content")
            .setPositiveButton("我已阅读") { _, _ ->
                prefs.edit().putString(KEY_NOTICE_HASH, contentHash).apply()
                step2Text.text = "步骤2: ✓ 公告已阅读"
                startStep3()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPrivacyDialog(privacyContent: String, contentHash: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        
        AlertDialog.Builder(activity)
            .setTitle("隐私协议")
            .setMessage(privacyContent)
            .setPositiveButton("同意") { _, _ ->
                prefs.edit().putString(KEY_PRIVACY_HASH, contentHash).apply()
                step3Text.text = "步骤3: ✓ 隐私协议已同意"
                startStep4()
            }
            .setNegativeButton("拒绝") { _, _ ->
                AlertDialog.Builder(activity)
                    .setTitle("无法继续")
                    .setMessage("必须同意隐私协议才能继续使用应用")
                    .setPositiveButton("重新阅读") { _, _ -> 
                        showPrivacyDialog(privacyContent, contentHash) 
                    }
                    .setNegativeButton("退出应用") { _, _ -> 
                        activity.finish() 
                    }
                    .setCancelable(false)
                    .show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showUpdateDialog(name: String, version: String, updateContent: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        
        AlertDialog.Builder(activity)
            .setTitle("发现新版本")
            .setMessage("$name v$version\n\n更新内容：\n$updateContent")
            .setPositiveButton("立即更新") { _, _ ->
                Toast.makeText(activity, "正在打开下载页面", Toast.LENGTH_SHORT).show()
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://110.42.63.51:39078/apps/apks"))
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "打开下载页面失败", e)
                    Toast.makeText(activity, "无法打开下载页面", Toast.LENGTH_SHORT).show()
                }
                step4Text.text = "步骤4: ✓ 已打开下载页面"
                completeVerification()
            }
            .setNegativeButton("稍后更新") { _, _ ->
                step4Text.text = "步骤4: ✓ 稍后更新"
                completeVerification()
            }
            .setCancelable(false)
            .show()
    }

    private fun showRetryDialog(title: String, message: String, retryAction: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage("$message\n\n请检查网络连接后重试")
            .setPositiveButton("重试") { _, _ -> retryAction() }
            .setNegativeButton("退出") { _, _ ->
                Toast.makeText(activity, "验证失败，应用退出", Toast.LENGTH_SHORT).show()
                activity.finish()
            }
            .setCancelable(false)
            .show()
    }

    // 完成验证
    private fun completeVerification() {
        verificationDialog?.dismiss()
        verificationDialog = null
        unblockUIInteraction()
        
        Toast.makeText(activity, "验证完成", Toast.LENGTH_SHORT).show()
        
        // 调用完成回调
        onVerificationComplete()
    }

    // === 工具方法 ===

    private fun makeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        if (conn.responseCode != 200) {
            throw IOException("HTTP ${conn.responseCode}")
        }

        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseIniStatus(iniContent: String): Boolean {
        return iniContent.contains("status=true")
    }

    private fun getSHA256Hash(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(input.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "SHA256计算失败", e)
            "hash_failed_${System.currentTimeMillis()}"
        }
    }

    private fun getLocalVersionCode(): Long {
        return try {
            val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取版本号失败", e)
            1L
        }
    }

    fun onDestroy() {
        executor.shutdownNow()
        verificationDialog?.dismiss()
        verificationDialog = null
        unblockUIInteraction()
    }
}