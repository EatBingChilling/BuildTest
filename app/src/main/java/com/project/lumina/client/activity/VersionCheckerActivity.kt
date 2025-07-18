package com.project.lumina.client.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
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
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

class VersionCheckerActivity : AppCompatActivity() {

    private lateinit var verificationManager: AppVerificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Phoenix) // Ensure Theme_Phoenix is defined in res/values/styles.xml
        super.onCreate(savedInstanceState)

        setContent {
            LoadingPlaceholder()
        }

        verificationManager = AppVerificationManager(
            activity = this,
            onSuccess = {
                CoroutineScope(Dispatchers.Main).launch {
                    initializeApp()
                    startMainActivity()
                }
            },
            onFailure = {
                Toast.makeText(this, "验证失败", Toast.LENGTH_LONG).show()
                finish()
            }
        )
        verificationManager.start()
    }

    private suspend fun initializeApp() = withContext(Dispatchers.IO) {
        val kson = HashCat.getInstance()
        kson.LintHashInit(this@VersionCheckerActivity)
        delay(666)
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        verificationManager.onDestroy()
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
    private val onFailure: () -> Unit
) {
    private val BASE_URL = "http://110.42.63.51:39078/d/apps"
    private val PREFS = activity.getSharedPreferences("verify_prefs", Context.MODE_PRIVATE)

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = android.os.Handler(activity.mainLooper)

    fun start() {
        executor.execute {
            try {
                val status = makeHttp("$BASE_URL/appstatus/a.ini")
                if (!status.contains("status=true")) {
                    handler.post { onFailure() }
                    return@execute
                }

                val notice = makeHttp("$BASE_URL/title/a.json")
                showNoticeIfNeeded(notice)

                val privacy = makeHttp("$BASE_URL/privary/a.txt")
                showPrivacyIfNeeded(privacy)

                val update = makeHttp("$BASE_URL/update/a.json")
                showUpdateIfNeeded(update)

                handler.post(onSuccess)
            } catch (e: IOException) {
                handler.post(onFailure)
            }
        }
    }

    private fun makeHttp(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        if (conn.responseCode != 200) throw IOException()
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun showNoticeIfNeeded(json: String) {
        val key = "notice_hash"
        val hash = sha256(json)
        if (hash != PREFS.getString(key, "")) {
            val obj = JSONObject(json)
            val title = obj.getString("title")
            val subtitle = obj.getString("subtitle")
            val content = obj.getString("content")
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage("$subtitle\n\n$content")
                .setCancelable(false)
                .setPositiveButton("朕已阅") { _, _ ->
                    PREFS.edit().putString(key, hash).apply()
                }
                .show()
        }
    }

    private fun showPrivacyIfNeeded(txt: String) {
        val key = "privacy_hash"
        val hash = sha256(txt)
        if (hash != PREFS.getString(key, "")) {
            AlertDialog.Builder(activity)
                .setTitle("隐私协议")
                .setMessage(txt)
                .setCancelable(false)
                .setPositiveButton("签") { _, _ ->
                    PREFS.edit().putString(key, hash).apply()
                }
                .setNegativeButton("不签") { _, _ ->
                    activity.finish()
                }
                .show()
        }
    }

    private fun showUpdateIfNeeded(json: String) {
        val obj = JSONObject(json)
        val cloud = obj.getLong("version")
        val local = activity.packageManager
            .getPackageInfo(activity.packageName, 0).longVersionCode
        if (cloud > local) {
            AlertDialog.Builder(activity)
                .setTitle("发现新版本")
                .setMessage("${obj.getString("name")}\n\n更新内容：\n${obj.getString("update_content")}")
                .setPositiveButton("立即更新") { _, _ ->
                    // Implement download page navigation here
                }
                .setNegativeButton("下次再说") { _, _ -> }
                .show()
        }
    }

    private fun sha256(s: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun onDestroy() {
        executor.shutdownNow()
    }
}