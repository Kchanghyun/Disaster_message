package com.example.disaster_message

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            // 1. 필요한 권한 체크
            val hasPermissions = checkPermissions()

            // 2. 필요한 초기 데이터 로딩
            val initialDataLoaded = loadInitialData()

            // 모든 체크가 완료되면
            if(hasPermissions && initialDataLoaded) {
                // 초기 설정 여부에 따라 다른 화면으로 이동
                val nextActivity = if (이미 설정이 완료되어있으면(권한 및 위치 정보)) {
                    MainActivity::class.java
                } else {
                    InitialSetupActivity::class.java
                }

                // 다음 화면으로 전환
                startActivity(Intent(this@SplashActivity, nextActivity))
                finish() // 현재 액티비티 종료
            } else {
                // 권한이 없거나 초기화 실패 시 처리
                showError()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == packageManager.PERMISSION_GRANTED
    }

    private suspend fun checkLoginStatus(): Boolean {
        // SharedPreferences나 DataStore에서 로그인 상태 확인
    }

    private suspend fun loadInitialData(): Boolean {
        return try {
            // 필요한 초기 데이터 로딩
            // API 호출이나 데이터베이스 작업 등
            delay(2000) // 최소 2초간 스플래시 화면 표시 (선택사항)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun showError() {
        Toast.makeText(this, "초기화 실패", Toast.LENGTH_LONG).show()
    }
}