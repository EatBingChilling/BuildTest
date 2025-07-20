package com.project.lumina.client.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.lumina.client.R

class VersionCheckerActivity : AppCompatActivity() {

    private lateinit var verificationManager: AppVerificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Material3 主题（NoActionBar）
        setTheme(com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight_NoActionBar)
        // 2. 开启 Edge-to-Edge
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_md3)

        // 3. 处理系统栏 padding，避免遮挡
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        verificationManager = AppVerificationManager(this) {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }, 800)
        }
        verificationManager.startVerification()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::verificationManager.isInitialized) verificationManager.onDestroy()
    }
}
