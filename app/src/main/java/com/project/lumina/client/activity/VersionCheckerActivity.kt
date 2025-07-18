package com.project.lumina.client.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.lumina.R
import com.project.lumina.client.util.HashCat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class VersionCheckerActivity : AppCompatActivity() {

    private lateinit var verificationManager: AppVerificationManager
    private val TAG = "VersionCheckerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        // 使用系统默认主题
        setTheme(android.R.style.Theme_Material_Light_DarkActionBar)
        super.onCreate(savedInstanceState)

        setContent {
            LoadingPlaceholder()
        }

        verificationManager = AppVerificationManager(
            activity = this,
            onSuccess = {
                Log.d(TAG, "验证成功，正在初始化应用...")
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        initializeApp()
                        startMainActivity()
                    } catch (e: Exception) {
                        Log.e(TAG, "初始化应用失败", e)
                        Toast.makeText(this@VersionCheckerActivity, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            },
            onFailure = { errorMsg ->
                Log.e(TAG, "验证失败: $errorMsg")
                Toast.makeText(this, "验证失败: $errorMsg", Toast.LENGTH_LONG).show()
                finish()
            }
        )
        verificationManager.start()
    }

    private suspend fun initializeApp() = withContext(Dispatchers.IO) {
        try {
            val kson = HashCat.getInstance()
            kson.LintHashInit(this@VersionCheckerActivity)
            delay(666)
            Log.d(TAG, "应用初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "HashCat初始化失败", e)
            throw e
        }
    }

    private fun startMainActivity() {
        Log.d(TAG, "启动主活动")
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::verificationManager.isInitialized) {
            verificationManager.onDestroy()
        }
    }
}

// --- Loading Placeholder Composable ---

@Composable
fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
    }
}

// --- Verification Manager ---

class AppVerificationManager(
    private val activity: AppCompatActivity,
    private val onSuccess: () -> Unit,
    private val onFailure: (String) -> Unit
) {
    private val BASE_URL = "http://110.42.63.51:39078/d/apps"
    private val PREFS = activity.getSharedPreferences("verify_prefs", Context.MODE_PRIVATE)
    private val TAG = "AppVerificationManager"
    
    // 网络请求超时设置
    private val CONNECT_TIMEOUT = 10000 // 10秒
    private val READ_TIMEOUT = 15000 // 15秒
    
    // 使用协程替代线程池
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            try {
                Log.d(TAG, "开始验证流程...")
                
                // 1. 检查应用状态
                val status = makeHttpRequest("$BASE_URL/appstatus/a.ini")
                Log.d(TAG, "应用状态: $status")
                
                if (!status.contains("status=true")) {
                    withContext(Dispatchers.Main) {
                        onFailure("应用当前不可用")
                    }
                    return@launch
                }

                // 2. 检查并显示公告
                try {
                    val notice = makeHttpRequest("$BASE_URL/title/a.json")
                    withContext(Dispatchers.Main) {
                        showNoticeIfNeeded(notice)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "获取公告失败，继续执行", e)
                }

                // 3. 检查并显示隐私协议
                try {
                    val privacy = makeHttpRequest("$BASE_URL/privary/a.txt")
                    withContext(Dispatchers.Main) {
                        showPrivacyIfNeeded(privacy)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "获取隐私协议失败，继续执行", e)
                }

                // 4. 检查更新
                try {
                    val update = makeHttpRequest("$BASE_URL/update/a.json")
                    withContext(Dispatchers.Main) {
                        showUpdateIfNeeded(update)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "检查更新失败，继续执行", e)
                }

                // 5. 验证成功
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "验证流程完成")
                    onSuccess()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "验证过程中出现错误", e)
                withContext(Dispatchers.Main) {
                    when (e) {
                        is SocketTimeoutException -> onFailure("网络连接超时")
                        is IOException -> onFailure("网络连接失败")
                        else -> onFailure("未知错误: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun makeHttpRequest(url: String): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "请求URL: $url")
        
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "LuminaApp/1.0")
            
            val responseCode = conn.responseCode
            Log.d(TAG, "响应码: $responseCode")
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP错误: $responseCode")
            }
            
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "响应内容长度: ${response.length}")
            return@withContext response
            
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "请求超时: $url", e)
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "网络请求失败: $url", e)
            throw e
        } finally {
            conn.disconnect()
        }
    }

    private fun showNoticeIfNeeded(json: String) {
        try {
            val key = "notice_hash"
            val hash = sha256(json)
            val savedHash = PREFS.getString(key, "")
            
            Log.d(TAG, "公告哈希对比 - 当前: $hash, 已保存: $savedHash")
            
            if (hash != savedHash) {
                val obj = JSONObject(json)
                val title = obj.optString("title", "公告")
                val subtitle = obj.optString("subtitle", "")
                val content = obj.optString("content", "")
                
                if (activity.isFinishing || activity.isDestroyed) return
                
                AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setMessage(if (subtitle.isNotEmpty()) "$subtitle\n\n$content" else content)
                    .setCancelable(false)
                    .setPositiveButton("朕已阅") { _, _ ->
                        PREFS.edit().putString(key, hash).apply()
                        Log.d(TAG, "公告已阅读并保存")
                    }
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理公告时出错", e)
        }
    }

    private fun showPrivacyIfNeeded(txt: String) {
        try {
            val key = "privacy_hash"
            val hash = sha256(txt)
            val savedHash = PREFS.getString(key, "")
            
            Log.d(TAG, "隐私协议哈希对比 - 当前: $hash, 已保存: $savedHash")
            
            if (hash != savedHash) {
                if (activity.isFinishing || activity.isDestroyed) return
                
                AlertDialog.Builder(activity)
                    .setTitle("隐私协议")
                    .setMessage(txt)
                    .setCancelable(false)
                    .setPositiveButton("同意") { _, _ ->
                        PREFS.edit().putString(key, hash).apply()
                        Log.d(TAG, "隐私协议已同意并保存")
                    }
                    .setNegativeButton("不同意") { _, _ ->
                        Log.d(TAG, "用户拒绝隐私协议")
                        activity.finish()
                    }
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理隐私协议时出错", e)
        }
    }

    private fun showUpdateIfNeeded(json: String) {
        try {
            val obj = JSONObject(json)
            val cloudVersion = obj.optLong("version", 0)
            val localVersion = try {
                activity.packageManager
                    .getPackageInfo(activity.packageName, 0).longVersionCode
            } catch (e: Exception) {
                Log.e(TAG, "获取本地版本号失败", e)
                0L
            }
            
            Log.d(TAG, "版本对比 - 云端: $cloudVersion, 本地: $localVersion")
            
            if (cloudVersion > localVersion) {
                val appName = obj.optString("name", "新版本")
                val updateContent = obj.optString("update_content", "修复了一些问题")
                val downloadUrl = obj.optString("download_url", "")
                
                if (activity.isFinishing || activity.isDestroyed) return
                
                AlertDialog.Builder(activity)
                    .setTitle("发现新版本")
                    .setMessage("$appName\n\n更新内容：\n$updateContent")
                    .setPositiveButton("立即更新") { _, _ ->
                        if (downloadUrl.isNotEmpty()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                activity.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e(TAG, "打开下载链接失败", e)
                                Toast.makeText(activity, "无法打开下载链接", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(activity, "下载链接不可用", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("稍后更新") { _, _ ->
                        Log.d(TAG, "用户选择稍后更新")
                    }
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查更新时出错", e)
        }
    }

    private fun sha256(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "SHA256计算失败", e)
            input.hashCode().toString()
        }
    }

    fun onDestroy() {
        Log.d(TAG, "销毁验证管理器")
        scope.cancel()
    }
}