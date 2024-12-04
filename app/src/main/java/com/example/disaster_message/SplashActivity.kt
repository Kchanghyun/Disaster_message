package com.example.disaster_message

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        lifecycleScope.launch {
            delay(2000) // 최소 로딩 시간

            // 최초 실행 여부 확인
            val isFirstRun = prefs.getBoolean("is_first_run", true)

            val nextActivity = if (isFirstRun) {
                // 최초 실행이면 설정 화면으로
                Initial_SetupActivity::class.java
            } else {
                // 아니면 메인 화면으로
                MainActivity::class.java
            }

            startActivity(Intent(this@SplashActivity, nextActivity))
            finish()
        }
    }
}