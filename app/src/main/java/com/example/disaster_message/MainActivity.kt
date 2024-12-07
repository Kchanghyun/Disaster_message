package com.example.disaster_message

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.IO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

//        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                val token = task.result
//                Log.d("FCM_TOKEN", "토큰 : $token")
//                sendTokenToServer(token)
//            }
//        }
        val settings = findViewById<ImageView>(R.id.settings)
        settings.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

//    private fun sendTokenToServer(token: String) {
//        scope.launch {
//            try {
//                val response = RetrofitClient.fcmApi.sendToken(TokenRequest(token))
//                if (response.isSuccessful) {
//                    Log.d("FCM_TOKEN", "토큰 서버 전송 성공")
//                } else {
//                    Log.e("FCM_TOKEN", "토큰 서버 전송 실패: ${response.code()}")
//                }
//            } catch (e: Exception) {
//                Log.e("FCM_TOKEN", "토큰 서버 전송 오류", e)
//            }
//        }
//    }
}