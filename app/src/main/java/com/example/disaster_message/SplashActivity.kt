package com.example.disaster_message

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private val NOTIFICATION_PERMISSION_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        lifecycleScope.launch {
            delay(2000)
            checkPermissions()
        }
    }

    private fun checkPermissions() {
        Log.d("Splash_test", "checkPermissions")
        // 안드로이드 버전 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 권한 확인
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) { // 알림 권한이 허용이 아니라면
                requestPermissions( // requestPermissions() 메서드 호출 후엔 자동으로 onRequestPermissionsResult()가 호출됨
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            } else {
                Log.d("Splash_test", "allow notification")
                moveToNextScreen()
            }
        } else {
            Log.d("Splash_test", "no android version")
            moveToNextScreen()
        }
    }

    private fun moveToNextScreen() {
        if (!isFinishing) {
            val isFirstRun = prefs.getBoolean("is_first_run", true)
            val nextActivity = if (isFirstRun) {
                Initial_SetupActivity::class.java
            } else {
                MainActivity::class.java
            }
            startActivity(Intent(this@SplashActivity, nextActivity))
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Splash_test", "checkbattery")
                    moveToNextScreen()
                } else {
                    Log.d("Splash_test", "moveScreen")
                    moveToNextScreen()
                }
            }
        }
    }
}