package com.project.lumina.client.activity

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.project.lumina.client.R
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

class VersionCheckerActivity : AppCompatActivity() {

    private lateinit var verificationManager: AppVerificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight)
        super.onCreate(savedInstanceState)

        // 沉浸式边距
        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom
            )
            insets
        }

        setContentView(R.layout.activity_loading_md3)

        verificationManager = AppVerificationManager(this) {
            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.IO) { delay(666) } // 模拟初始化
                startActivity(Intent(this@VersionCheckerActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        }
        verificationManager.startVerification()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::verificationManager.isInitialized) verificationManager.onDestroy()
    }
}

/* -------------------------------------------------------------------------- */

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

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 控件
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

    private var step1Passed = false
    private var step2Passed = false
    private var step3Passed = false
    private var step4Passed = false

    fun startVerification() {
        bindViews()
        initializeStepUI()
        startStep1()
    }

    private fun bindViews() {
        progressIndicator = activity.findViewById(R.id.progress_indicator)
        statusText        = activity.findViewById(R.id.status_text)
        step1Card         = activity.findViewById(R.id.step1_card)
        step2Card         = activity.findViewById(R.id.step2_card)
        step3Card         = activity.findViewById(R.id.step3_card)
        step4Card         = activity.findViewById(R.id.step4_card)
        step1Text         = activity.findViewById(R.id.step1_text)
        step2Text         = activity.findViewById(R.id.step2_text)
        step3Text         = activity.findViewById(R.id.step3_text)
        step4Text         = activity.findViewById(R.id.step4_text)
        step1Progress     = activity.findViewById(R.id.step1_progress)
        step2Progress     = activity.findViewById(R.id.step2_progress)
        step3Progress     = activity.findViewById(R.id.step3_progress)
        step4Progress     = activity.findViewById(R.id.step4_progress)
    }

    private fun initializeStepUI() {
        progressIndicator.progress = 0
        statusText.text = "应用验证中…"
        setStepStatus(1, StepStatus.IN_PROGRESS, "正在连接服务器")
        setStepStatus(2, StepStatus.WAITING, "等待公告")
        setStepStatus(3, StepStatus.WAITING, "等待隐私协议")
        setStepStatus(4, StepStatus.WAITING, "检查版本")
    }

    private enum class StepStatus { WAITING, IN_PROGRESS, SUCCESS, ERROR }

    private fun setStepStatus(step: Int, status: StepStatus, text: String) = handler.post {
        val txt: TextView = when (step) {
            1 -> step1Text
            2 -> step2Text
            3 -> step3Text
            4 -> step4Text
            else -> return@post
        }
        val progress: CircularProgressIndicator = when (step) {
            1 -> step1Progress
            2 -> step2Progress
            3 -> step3Progress
            4 -> step4Progress
            else -> return@post
        }

        txt.text = text
        when (status) {
            StepStatus.WAITING -> {
                progress.visibility = View.GONE
                txt.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            StepStatus.IN_PROGRESS -> {
                progress.visibility = View.VISIBLE
                progress.isIndeterminate = true
                txt.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            StepStatus.SUCCESS -> {
                progress.visibility = View.GONE
                txt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_24, 0, 0, 0)
            }
            StepStatus.ERROR -> {
                progress.visibility = View.GONE
                txt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_24, 0, 0, 0)
            }
        }
    }

    private fun updateProgress() {
        val done = listOf(step1Passed, step2Passed, step3Passed, step4Passed).count { it }
        handler.post {
            progressIndicator.setProgress(done * 100 / 4, true)
            if (done == 4) statusText.text = "验证完成，启动中…"
        }
    }

    /* ----------------- 4 步流程 ----------------- */
    private fun startStep1() {
        executor.execute {
            try {
                val resp = makeHttpRequest("$BASE_URL/appstatus/a.ini")
                handler.post {
                    if (parseIniStatus(resp)) {
                        step1Passed = true
                        setStepStatus(1, StepStatus.SUCCESS, "服务器连接成功")
                        updateProgress()
                        startStep2()
                    } else {
                        setStepStatus(1, StepStatus.ERROR, "应用状态验证失败")
                        showRetryDialog("状态验证失败", "应用当前不可用，请联系开发者", ::startStep1)
                    }
                }
            } catch (e: IOException) {
                handler.post {
                    setStepStatus(1, StepStatus.ERROR, "网络连接失败")
                    showRetryDialog("网络错误", "无法连接服务器，请检查网络", ::startStep1)
                }
            }
        }
    }

    private fun startStep2() {
        setStepStatus(2, StepStatus.IN_PROGRESS, "获取公告…")
        executor.execute {
            try {
                val resp = makeHttpRequest("$BASE_URL/title/a.json")
                handler.post {
                    try {
                        val json = JSONObject(resp)
                        val title = json.getString("title")
                        val subtitle = json.getString("subtitle")
                        val content = json.getString("content")
                        val hash = getSHA256Hash(resp)
                        if (prefs.getString(KEY_NOTICE_HASH, "") != hash) {
                            setStepStatus(2, StepStatus.IN_PROGRESS, "请阅读新公告")
                            showNoticeDialog(title, subtitle, content, hash)
                        } else {
                            step2Passed = true
                            setStepStatus(2, StepStatus.SUCCESS, "公告已读")
                            updateProgress()
                            startStep3()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "公告解析失败", e)
                        step2Passed = true
                        setStepStatus(2, StepStatus.ERROR, "公告解析失败，跳过")
                        updateProgress()
                        startStep3()
                    }
                }
            } catch (e: IOException) {
                handler.post {
                    step2Passed = true
                    setStepStatus(2, StepStatus.ERROR, "获取公告失败，跳过")
                    updateProgress()
                    startStep3()
                }
            }
        }
    }

    private fun startStep3() {
        setStepStatus(3, StepStatus.IN_PROGRESS, "获取隐私协议…")
        executor.execute {
            try {
                val resp = makeHttpRequest("$BASE_URL/privary/a.txt")
                handler.post {
                    val hash = getSHA256Hash(resp)
                    if (prefs.getString(KEY_PRIVACY_HASH, "") != hash) {
                        setStepStatus(3, StepStatus.IN_PROGRESS, "请同意隐私协议")
                        showPrivacyDialog(resp, hash)
                    } else {
                        step3Passed = true
                        setStepStatus(3, StepStatus.SUCCESS, "隐私协议已同意")
                        updateProgress()
                        startStep4()
                    }
                }
            } catch (e: IOException) {
                handler.post {
                    setStepStatus(3, StepStatus.ERROR, "获取协议失败")
                    showRetryDialog("隐私协议获取失败", "无法获取隐私协议，这是必需的步骤", ::startStep3)
                }
            }
        }
    }

    private fun startStep4() {
        setStepStatus(4, StepStatus.IN_PROGRESS, "检查版本…")
        executor.execute {
            try {
                val resp = makeHttpRequest("$BASE_URL/update/a.json")
                handler.post {
                    try {
                        val json = JSONObject(resp)
                        val cloud = json.getLong("version")
                        val local = getLocalVersionCode()
                        if (cloud > local) {
                            setStepStatus(4, StepStatus.IN_PROGRESS, "发现新版本")
                            showUpdateDialog(
                                json.getString("name"),
                                cloud.toString(),
                                json.getString("update_content"),
                                local,
                                cloud
                            )
                        } else {
                            step4Passed = true
                            setStepStatus(4, StepStatus.SUCCESS, "已是最新版本")
                            updateProgress()
                            checkAllStepsComplete()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "版本检查失败", e)
                        step4Passed = true
                        setStepStatus(4, StepStatus.ERROR, "版本检查失败，跳过")
                        updateProgress()
                        checkAllStepsComplete()
                    }
                }
            } catch (e: IOException) {
                handler.post {
                    step4Passed = true
                    setStepStatus(4, StepStatus.ERROR, "无法获取版本信息，跳过")
                    updateProgress()
                    checkAllStepsComplete()
                }
            }
        }
    }

    private fun checkAllStepsComplete() {
        if (step1Passed && step2Passed && step3Passed && step4Passed) {
            handler.postDelayed({ onVerificationComplete() }, 800)

        }
    }

    /* ---------- Dialog 工具 ---------- */
    private fun showNoticeDialog(title: String, subtitle: String, content: String, hash: String) =
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage("$subtitle\n\n$content")
            .setPositiveButton("我已阅读") { _, _ ->
                prefs.edit().putString(KEY_NOTICE_HASH, hash).apply()
                step2Passed = true
                setStepStatus(2, StepStatus.SUCCESS, "公告已读")
                updateProgress()
                startStep3()
            }
            .setCancelable(false)
            .show()

    private fun showPrivacyDialog(content: String, hash: String) =
        MaterialAlertDialogBuilder(activity)
            .setTitle("隐私协议")
            .setMessage(content)
            .setPositiveButton("同意") { _, _ ->
                prefs.edit().putString(KEY_PRIVACY_HASH, hash).apply()
                step3Passed = true
                setStepStatus(3, StepStatus.SUCCESS, "隐私协议已同意")
                updateProgress()
                startStep4()
            }
            .setNegativeButton("拒绝") { _, _ ->
                MaterialAlertDialogBuilder(activity)
                    .setTitle("无法继续")
                    .setMessage("必须同意隐私协议才能继续使用")
                    .setPositiveButton("重新阅读") { _, _ -> showPrivacyDialog(content, hash) }
                    .setNegativeButton("退出应用") { _, _ -> activity.finish() }
                    .setCancelable(false)
                    .show()
            }
            .setCancelable(false)
            .show()

    private fun showUpdateDialog(name: String, ver: String, content: String, local: Long, cloud: Long) =
        MaterialAlertDialogBuilder(activity)
            .setTitle("发现新版本")
            .setMessage("$name v$ver\n\n当前版本: $local\n最新版本: $cloud\n\n更新内容：\n$content")
            .setPositiveButton("立即更新") { _, _ ->
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://110.42.63.51:39078/apps/apks")))
                activity.finish()
            }
            .setNegativeButton("跳过更新") { _, _ ->
                step4Passed = true
                setStepStatus(4, StepStatus.SUCCESS, "跳过更新")
                updateProgress()
                checkAllStepsComplete()
            }
            .setCancelable(false)
            .show()

    private fun showRetryDialog(title: String, msg: String, action: () -> Unit) =
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("重试") { _, _ -> action() }
            .setNegativeButton("退出") { _, _ -> activity.finish() }
            .setCancelable(false)
            .show()

    /* ---------- 网络 & 工具 ---------- */
    private fun makeHttpRequest(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "Lumina Android Client")
        if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseIniStatus(s: String) = s.contains("status=true", ignoreCase = true)

    private fun getSHA256Hash(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    @Suppress("DEPRECATION")
    private fun getLocalVersionCode(): Long =
        activity.packageManager.getPackageInfo(activity.packageName, 0).let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode
            else it.versionCode.toLong()
        }

    fun onDestroy() {
        executor.shutdownNow()
    }
}
