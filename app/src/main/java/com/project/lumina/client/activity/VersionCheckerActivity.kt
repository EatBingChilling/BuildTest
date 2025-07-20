package com.project.lumina.client.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.project.lumina.client.R

class VersionCheckerActivity : AppCompatActivity() {

    private lateinit var verificationManager: AppVerificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_md3)

        verificationManager = AppVerificationManager(this) {
            // 验证完成后跳转 MainActivity
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
        if (::verificationManager.isInitialized) {
            verificationManager.onDestroy()
        }
    }
}