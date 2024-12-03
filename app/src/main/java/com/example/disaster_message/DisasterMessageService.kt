package com.example.disaster_message

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.launch
import kotlinx.coroutines.*

// 백그라운드 서비스 상속 -> 앱이 종료되어도 계속 실행되며, 장기 작업을 처리하거나 알림 등을 수행할 수 있다.
class DisasterMessageService : Service() {
    private lateinit var prefs: SharedPreferences // 사용자 설정 데이터를 저장하고 불러오기 위해 사용되는 SharedPreferences 객체
    private lateinit var disasterApi: Disaster_message_api
    private var monitoringJob: Job? = null // 코루틴 작업을 추적하기 위한 변수로, 모니터링 작업을 중단하거나 상태를 확인할 때 사용
    override fun onBind(intent: Intent?): IBinder? = null // 바운드 서비스가 아닌 경우 null을 반환

    // 서비스가 생성될 때 호출
    // 생성자 느낌인가?
    override fun onCreate() {
        super.onCreate()
        // SharedPreferences 파일의 이름 - user_settings
        // 이 이름으로 앱의 내부 저장소에 파일이 생성된다.
        // SetupActivity에서 같은 이름("user_settings")으로 저장한 데이터를 읽어올 수 있다.
        prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        disasterApi = Disaster_message_api()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 서비스가 시작될 때 모니터링 시작
        startMonitoring()
        // 서비스가 강제로 종료되더라도 다시 시작되도록 설정
        return START_STICKY
    }

    private fun startMonitoring() {
        // 재난문자 수신 시
        // 1. 사용자 설정 언어 확인
        // user_language 라는 키로 저장된 값을 찾음
        // 만약 값이 없다면 두 번째 매개변수인 "영어"를 기본값으로 반환
        // ?: -> 앞의 결과가 null이면 "영어"를 사용
//        var userLanguage = prefs.getString("user_language", "영어") ?: "영어"

        // 백그라운드에서 disasterApi.startMonitoring() 메서드 실행.
        // Dispatchers.IO는 입출력 작업에 최적화된 코루틴 디스패처이다.
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            disasterApi.startMonitoring() // 재난문자 모니터링 시작
        }
    }


    // API 26 이상에서는 알림 채널(NotificationChannel)을 생성해야 함.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNotification(message: Disaster_message_api.DisasterMessage) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 알림 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "disaster_alerts",
                "재난문자 알림",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "disaster_alerts")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle(message.DST_SE_NM)
            .setContentText(message.MSG_CN)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(message.SN.toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()  // 모니터링 중지
    }
}