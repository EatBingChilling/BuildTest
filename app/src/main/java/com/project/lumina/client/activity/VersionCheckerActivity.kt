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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
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
        // 使用 Material Design 3 主题
        setTheme(android.R.style.Theme_Material3_DayNight);
        super.onCreate(savedInstanceState)

        // 设置边到边显示
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        // 显示 Material 3 风格的加载界面
        setContentView(R.layout.activity_loading_md3)
        
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

// --- Material 3 验证管理器 ---
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
    
    // UI 组件
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var statusText: TextView
    private lateinit var step1Card: MaterialCardView
    private lateinit var step2Card: MaterialCardView
    private lateinit var step3Card: MaterialCardView
    private lateinit var step4Card: MaterialCardView
    private lateinit var step1Text: TextView
    private lateinit var step2Text: TextView
    private lateinit var step3Text: TextView
    private lateinit var step4Text: TextView
    private lateinit var step1Progress: CircularProgressIndicator
    private lateinit var step2Progress: CircularProgressIndicator
    private lateinit var step3Progress: CircularProgressIndicator
    private lateinit var step4Progress: CircularProgressIndicator

    // 验证状态
    private var step1Passed = false
    private var step2Passed = false
    private var step3Passed = false
    private var step4Passed = false

    fun startVerification() {
        // 显示 Material 3 验证对话框
        createMaterial3VerificationDialog()
        // 开始第一步验证
        startStep1()
    }

    private fun createMaterial3VerificationDialog() {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_verification_md3, null)
        
        // 绑定UI组件
        progressIndicator = dialogView.findViewById(R.id.progress_indicator)
        statusText = dialogView.findViewById(R.id.status_text)
        
        step1Card = dialogView.findViewById(R.id.step1_card)
        step2Card = dialogView.findViewById(R.id.step2_card)
        step3Card = dialogView.findViewById(R.id.step3_card)
        step4Card = dialogView.findViewById(R.id.step4_card)
        
        step1Text = dialogView.findViewById(R.id.step1_text)
        step2Text = dialogView.findViewById(R.id.step2_text)
        step3Text = dialogView.findViewById(R.id.step3_text)
        step4Text = dialogView.findViewById(R.id.step4_text)
        
        step1Progress = dialogView.findViewById(R.id.step1_progress)
        step2Progress = dialogView.findViewById(R.id.step2_progress)
        step3Progress = dialogView.findViewById(R.id.step3_progress)
        step4Progress = dialogView.findViewById(R.id.step4_progress)

        // 初始化步骤状态
        initializeStepUI()

        verificationDialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Lumina 验证")
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        verificationDialog?.show()
    }

    private fun initializeStepUI() {
        // 设置初始状态
        progressIndicator.progress = 0
        statusText.text = "正在进行应用验证，请稍候..."
        
        // 初始化步骤卡片
        setStepStatus(1, StepStatus.IN_PROGRESS, "正在连接服务器...")
        setStepStatus(2, StepStatus.WAITING, "等待公告传回")
        setStepStatus(3, StepStatus.WAITING, "等待隐私协议传回")
        setStepStatus(4, StepStatus.WAITING, "检查版本信息")
    }

    private enum class StepStatus {
        WAITING, IN_PROGRESS, SUCCESS, ERROR
    }

    private fun setStepStatus(stepNumber: Int, status: StepStatus, text: String) {
        val card: MaterialCardView
        val textView: TextView
        val progress: CircularProgressIndicator
        
        when (stepNumber) {
            1 -> { card = step1Card; textView = step1Text; progress = step1Progress }
            2 -> { card = step2Card; textView = step2Text; progress = step2Progress }
            3 -> { card = step3Card; textView = step3Text; progress = step3Progress }
            4 -> { card = step4Card; textView = step4Text; progress = step4Progress }
            else -> return
        }

        // 设置文本（支持 \n 换行）
        textView.text = text.replace("\\n", "\n")
        
        // 根据状态设置UI
        when (status) {
            StepStatus.WAITING -> {
                card.strokeColor = ContextCompat.getColor(activity, R.color.md_theme_outline_variant)
                card.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.md_theme_surface_variant))
                progress.visibility = View.GONE
                textView.setTextColor(ContextCompat.getColor(activity, R.color.md_theme_on_surface_variant))
            }
            StepStatus.IN_PROGRESS -> {
                card.strokeColor = ContextCompat.getColor(activity, R.color.md_theme_primary)
                card.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.md_theme_primary_container))
                progress.visibility = View.VISIBLE
                progress.isIndeterminate = true
                textView.setTextColor(ContextCompat.getColor(activity, R.color.md_theme_on_primary_container))
            }
            StepStatus.SUCCESS -> {
                card.strokeColor = ContextCompat.getColor(activity, R.color.md_theme_tertiary)
                card.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.md_theme_tertiary_container))
                progress.visibility = View.GONE
                textView.setTextColor(ContextCompat.getColor(activity, R.color.md_theme_on_tertiary_container))
                
                // 添加成功图标
                textView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_check_circle_24, 0, 0, 0
                )
            }
            StepStatus.ERROR -> {
                card.strokeColor = ContextCompat.getColor(activity, R.color.md_theme_error)
                card.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.md_theme_error_container))
                progress.visibility = View.GONE
                textView.setTextColor(ContextCompat.getColor(activity, R.color.md_theme_on_error_container))
                
                // 添加错误图标
                textView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_error_24, 0, 0, 0
                )
            }
        }
    }

    private fun updateProgress() {
        var completedSteps = 0
        if (step1Passed) completedSteps++
        if (step2Passed) completedSteps++
        if (step3Passed) completedSteps++
        if (step4Passed) completedSteps++
        
        val progress = (completedSteps * 100) / 4
        progressIndicator.setProgress(progress, true)
        
        if (completedSteps == 4) {
            statusText.text = "验证完成！正在启动应用..."
        }
    }

    // 第一步：验证应用状态
    private fun startStep1() {
        executor.execute {
            try {
                val response = makeHttpRequest("$BASE_URL/appstatus/a.ini")
                mainHandler.post {
                    if (parseIniStatus(response)) {
                        step1Passed = true
                        setStepStatus(1, StepStatus.SUCCESS, "✓ 服务器连接成功")
                        updateProgress()
                        startStep2()
                    } else {
                        setStepStatus(1, StepStatus.ERROR, "✗ 应用状态验证失败\\n应用当前不可用")
                        showRetryDialog("状态验证失败", "应用当前不可用，请联系开发者", ::startStep1)
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    setStepStatus(1, StepStatus.ERROR, "✗ 网络连接失败\\n请检查网络设置")
                    showRetryDialog("网络错误", "无法连接到服务器，请检查网络", ::startStep1)
                }
            }
        }
    }

    // 第二步：处理公告
    private fun startStep2() {
        setStepStatus(2, StepStatus.IN_PROGRESS, "正在获取公告...")
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
                            setStepStatus(2, StepStatus.IN_PROGRESS, "发现新公告\\n等待用户阅读")
                            showNoticeDialog(title, subtitle, content, contentHash)
                        } else {
                            step2Passed = true
                            setStepStatus(2, StepStatus.SUCCESS, "✓ 公告已阅读")
                            updateProgress()
                            startStep3()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析公告失败", e)
                        setStepStatus(2, StepStatus.ERROR, "✗ 公告解析失败\\n将跳过此步骤")
                        // 公告不是必需的，继续下一步
                        step2Passed = true
                        updateProgress()
                        startStep3()
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    setStepStatus(2, StepStatus.ERROR, "✗ 获取公告失败\\n将跳过此步骤")
                    // 公告不是必需的，继续下一步
                    step2Passed = true
                    updateProgress()
                    startStep3()
                }
            }
        }
    }

    // 第三步：处理隐私协议（必需步骤）
    private fun startStep3() {
        setStepStatus(3, StepStatus.IN_PROGRESS, "正在获取隐私协议...")
        executor.execute {
            try {
                val response = makeHttpRequest("$BASE_URL/privary/a.txt")
                mainHandler.post {
                    val contentHash = getSHA256Hash(response)
                    val savedHash = prefs.getString(KEY_PRIVACY_HASH, "")

                    if (contentHash != savedHash) {
                        setStepStatus(3, StepStatus.IN_PROGRESS, "隐私协议已更新\\n必须同意才能继续")
                        showPrivacyDialog(response, contentHash)
                    } else {
                        step3Passed = true
                        setStepStatus(3, StepStatus.SUCCESS, "✓ 隐私协议已同意")
                        updateProgress()
                        startStep4()
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    setStepStatus(3, StepStatus.ERROR, "✗ 获取协议失败\\n无法继续使用")
                    showRetryDialog("隐私协议获取失败", "无法获取隐私协议，这是必需的步骤", ::startStep3)
                }
            }
        }
    }

    // 第四步：检查版本更新
    private fun startStep4() {
        setStepStatus(4, StepStatus.IN_PROGRESS, "正在检查版本更新...")
        executor.execute {
            try {
                val response = makeHttpRequest("$BASE_URL/update/a.json")
                mainHandler.post {
                    try {
                        val json = JSONObject(response)
                        val cloudVersion = json.getLong("version")
                        val localVersion = getLocalVersionCode()

                        if (cloudVersion > localVersion) {
                            setStepStatus(4, StepStatus.IN_PROGRESS, "发现新版本\\n等待用户选择")
                            showUpdateDialog(
                                json.getString("name"),
                                cloudVersion.toString(),
                                json.getString("update_content"),
                                localVersion,
                                cloudVersion
                            )
                        } else {
                            step4Passed = true
                            setStepStatus(4, StepStatus.SUCCESS, "✓ 已是最新版本\\n版本号: $localVersion")
                            updateProgress()
                            checkAllStepsComplete()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "版本检查失败", e)
                        setStepStatus(4, StepStatus.ERROR, "✗ 版本检查失败\\n将跳过此步骤")
                        // 版本检查不是致命错误，继续完成验证
                        step4Passed = true
                        updateProgress()
                        checkAllStepsComplete()
                    }
                }
            } catch (e: IOException) {
                mainHandler.post {
                    setStepStatus(4, StepStatus.ERROR, "✗ 无法获取版本信息\\n将跳过此步骤")
                    // 版本检查不是致命错误，继续完成验证
                    step4Passed = true
                    updateProgress()
                    checkAllStepsComplete()
                }
            }
        }
    }

    // 检查所有步骤是否完成
    private fun checkAllStepsComplete() {
        if (step1Passed && step2Passed && step3Passed && step4Passed) {
            Handler(Looper.getMainLooper()).postDelayed({
                completeVerification()
            }, 1000) // 延迟1秒让用户看到完成状态
        }
    }

    // === Material 3 对话框方法 ===

    private fun showNoticeDialog(title: String, subtitle: String, content: String, contentHash: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        
        val fullContent = "$subtitle\n\n$content".replace("\\n", "\n")
        
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(fullContent)
            .setPositiveButton("我已阅读") { _, _ ->
                prefs.edit().putString(KEY_NOTICE_HASH, contentHash).apply()
                step2Passed = true
                setStepStatus(2, StepStatus.SUCCESS, "✓ 公告已阅读")
                updateProgress()
                startStep3()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPrivacyDialog(privacyContent: String, contentHash: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        
        val formattedContent = privacyContent.replace("\\n", "\n")
        
        MaterialAlertDialogBuilder(activity)
            .setTitle("隐私协议")
            .setMessage(formattedContent)
            .setPositiveButton("同意") { _, _ ->
                prefs.edit().putString(KEY_PRIVACY_HASH, contentHash).apply()
                step3Passed = true
                setStepStatus(3, StepStatus.SUCCESS, "✓ 隐私协议已同意")
                updateProgress()
                startStep4()
            }
            .setNegativeButton("拒绝") { _, _ ->
                MaterialAlertDialogBuilder(activity)
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

    private fun showUpdateDialog(name: String, version: String, updateContent: String, localVersion: Long, cloudVersion: Long) {
        if (activity.isFinishing || activity.isDestroyed) return
        
        val formattedContent = "当前版本: $localVersion\n最新版本: $cloudVersion\n\n更新内容：\n$updateContent".replace("\\n", "\n")
        
        MaterialAlertDialogBuilder(activity)
            .setTitle("发现新版本")
            .setMessage("$name v$version\n\n$formattedContent")
            .setPositiveButton("立即更新") { _, _ ->
                Toast.makeText(activity, "正在打开下载页面", Toast.LENGTH_SHORT).show()
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://110.42.63.51:39078/apps/apks"))
                    activity.startActivity(intent)
                    // 用户选择更新时，直接退出应用，不进入主界面
                    activity.finish()
                } catch (e: Exception) {
                    Log.e(TAG, "打开下载页面失败", e)
                    Toast.makeText(activity, "无法打开下载页面", Toast.LENGTH_SHORT).show()
                    // 如果打开下载页面失败，允许继续使用旧版本
                    step4Passed = true
                    setStepStatus(4, StepStatus.SUCCESS, "✓ 跳过更新")
                    updateProgress()
                    checkAllStepsComplete()
                }
            }
            .setNegativeButton("跳过更新") { _, _ ->
                step4Passed = true
                setStepStatus(4, StepStatus.SUCCESS, "✓ 跳过更新\\n当前版本: $localVersion")
                updateProgress()
                checkAllStepsComplete()
            }
            .setCancelable(false)
            .show()
    }

    private fun showRetryDialog(title: String, message: String, retryAction: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        
        val fullMessage = "$message\n\n请检查网络连接后重试".replace("\\n", "\n")
        
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(fullMessage)
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
        
        Toast.makeText(activity, "验证完成，正在启动应用", Toast.LENGTH_SHORT).show()
        
        // 调用完成回调
        onVerificationComplete()
    }

    // === 工具方法 ===

    private fun makeHttpRequest(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "Lumina Android Client")

        if (conn.responseCode != 200) {
            throw IOException("HTTP ${conn.responseCode}: ${conn.responseMessage}")
        }

        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseIniStatus(iniContent: String): Boolean {
        return iniContent.contains("status=true", ignoreCase = true)
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
    }
}