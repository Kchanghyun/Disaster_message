package com.example.disaster_message


import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var prefs: SharedPreferences
    private lateinit var user_language: String
    private lateinit var gpt_API: GPT_API
    private val google_map_api_key = BuildConfig.GOOGLE_MAP_API_KEY

    private lateinit var response_list: GPT_API.DisasterMessage

    override fun onMessageReceived(message: RemoteMessage) {
        // message 형태
        super.onMessageReceived(message)
        // FCM 메시지를 받았을 때 호출
        Log.d("SharedPreferences_test", "메시지 받음")

        val title = message.data["title"] ?: "no title"
        val body = message.data["body"] ?: "no body"
        val location = message.data["location"] ?: "no location"

        val locationList: List<String> = listOf(*location.split(",").toTypedArray())


        val locations: Map<String, Pair<Double, Double>> = getCoordinatesForMultipleAddresses(locationList, google_map_api_key)
        val userLat = prefs.getFloat("user_latitude", 37.82177F).toDouble()
        val userLon = prefs.getFloat("user_longitude", 128.1554F).toDouble()
        for ((address, coordinates) in locations) {
            val locationType = getLocationType(address)
            val latitude = coordinates.first
            val longitude = coordinates.second
            if(isUserWithinDisasterArea(userLat, userLon, latitude, longitude, locationType)) {
                user_language = prefs.getString("user_language", "영어") ?: "영어"
                scope.launch {
                    try {
                        response_list = gpt_API.callGPTAPI(user_language, title, body)
                        saveMessageToLog(response_list.title, response_list.body)
                        sendNotification(response_list.title, response_list.body)
                    } catch (e: Exception) {
                        Log.e("SharedPreferences_test", "Error during translation", e)
                        saveMessageToLog(title, body)
                        sendNotification(title, body) // 번역 실패 시 원문 알림
                    }
                }
                break
            }
        }
    }

    private fun saveMessageToLog(title: String, body: String) {
        val currentTime = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())

        val prefs = getSharedPreferences("disaster_log", Context.MODE_PRIVATE)

        // 기존 로그 가져오기 (JSON 문자열 형태로 저장)
        val logJson = prefs.getString("messages", "[]")
        val messageList = ArrayList<Profile>()

        try {
            val type = object : TypeToken<ArrayList<Profile>>() {}.type
            val existingMessages = Gson().fromJson<ArrayList<Profile>>(logJson, type)

            // 기존 메시지들을 messageList에 추가
            messageList.addAll(existingMessages)

            messageList.add(Profile(title, body, currentTime))
            // 리스트를 JSON으로 변환하여 저장
            val updateJson = Gson().toJson(messageList)
            prefs.edit().putString("messages", updateJson).apply()
        } catch(e: Exception) {
            Log.e("SharedPreferences_test", "메시지 저장 실패", e)
        }
    }

    private fun sendNotification(title: String, body: String) {
        // 알림 생성
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()

        // 전체화면 인텐트 설정
        val Intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("title", title)
            putExtra("body", body)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationId = UUID.randomUUID().toString().hashCode()

        // Wake Lock 획득 (화면이 꺼져있을 때도 알림)
        // 시스템의 전원 관리 서비스에 접근하기 위한 PowerManager 객체 저장
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            // 화면, 키보드, 백라이트 모두 ON
            PowerManager.FULL_WAKE_LOCK or
                    // WakeLock 획득 시 기기를 즉시 깨움
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    // WakeLock이 해제된 후에도 화면을 켜진 상태로 유지
                    PowerManager.ON_AFTER_RELEASE,
            "FCM:WakeLock"
        ).apply {
            // 15초(15000밀리초)동안 WakeLock 유지
            acquire(15000)
        }

        scope.launch { // 비동기 실행
            startVibration() // 진동 실행
        }

        try {
            notificationManager.notify(
                notificationId,
                createHeadUpNotification(title, body, pendingIntent).build()
            )

            Handler(Looper.getMainLooper()).postDelayed({
                val notification = createPermanentNotification(title, body, pendingIntent)
                    .setOnlyAlertOnce(true)
                    .setVibrate(longArrayOf(0))
                    .build()

                notification.flags = notification.flags or Notification.FLAG_ONLY_ALERT_ONCE
                notificationManager.notify(notificationId, notification)
            }, 20000)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun startVibration() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibratePattern = longArrayOf(0, 1000, 500, 1000)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(vibratePattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 500, 1000), -1)
            }
        } catch (e: Exception) {
            Log.e("SharedPreferences_test", "진동 실행 실패", e)
        }
    }

    private fun createNotificationChannel() {
        // 안드로이드 8.0 이상에서는 채널이 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "emergency",
                "재난 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "DISASTER_MESSAGE"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000) // 진동 패턴
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                importance = NotificationManager.IMPORTANCE_HIGH
                setBypassDnd(true) // 방해금지 모드 무시
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // 잠금화면에서 전체 내용 표시
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createHeadUpNotification(title: String, body: String, pendingIntent: PendingIntent) : NotificationCompat.Builder {
        return NotificationCompat.Builder(this, "emergency")
            .setSmallIcon(R.drawable.ic_warning) // 알림에 나올 아이콘
            .setContentTitle(title) // 알림 타이틀
            .setContentText(body) // 알림 내용
            .setPriority(NotificationCompat.PRIORITY_MAX) // 최대 우선순위
            .setCategory(NotificationCompat.CATEGORY_CALL) // 알람 카테고리 ( 우선순위 : CALL > ALARM )
            // 1. 헤드업 알림을 표시하는 트리거, 2. 알림을 탭했을 때의 동작도 정의(ContentIntent와 비슷한 역할)
            .setFullScreenIntent(pendingIntent, true) // 전체화면 인텐트
            .setAutoCancel(true) // 탭하면 알림이 사라지는지 여부
            .setOngoing(false) // 사용자가 스와이프로 제거할 수 없게 설정
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 1000, 500, 1000)) // 진동 패턴
            .setLights(Color.RED, 3000, 3000) // LED 설정
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 잠금화면에서 전체 내용 표시
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(body)) // 긴 텍스트 표시
            .setOnlyAlertOnce(true) // 동일한 알림 id에 대해서 처음 한 번만 알림 효과를 발생
    }

    private fun createPermanentNotification(title: String, body: String, pendingIntent: PendingIntent): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, "emergency")
            .setSmallIcon(R.drawable.ic_warning) // 알림 아이콘
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 최대 우선순위
//            .setCategory(NotificationCompat.CATEGORY_CALL) // 알람 카테고리 ( 우선순위 : CALL > ALARM )
            .setContentIntent(pendingIntent) // (헤드업이 아닌) 일반 알림을 탭했을 때의 동작
            .setFullScreenIntent(null, false)
            .setAutoCancel(true) // 탭하면 알림이 사라지는지 여부
            .setOngoing(false) // 사용자가 스와이프로 제거할 수 없게 설정
            .setVibrate(null) // 진동 비활성화
            .setSound(null) // 소리 비활성화
            .setDefaults(0)
            .setFullScreenIntent(null, false)
            .setAllowSystemGeneratedContextualActions(false)
            .setLights(Color.RED, 3000, 3000) // LED 설정
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 잠금화면에서 전체 내용 표시
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(body)) // 긴 텍스트 표시
            .setOnlyAlertOnce(true) // 동일한 알림 ID에 대해 소리, 진동 등의 알림 효과를 처음 한 번만 발생시키도록 하는 메서드
    }

    override fun onCreate() {
        super.onCreate()
        //지정된 토픽을 구독한다.
        prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        gpt_API = GPT_API() // gpt_API 객체 초기화
        Log.d("SharedPreferences_test", "google map api key : $google_map_api_key")
        FirebaseMessaging.getInstance().subscribeToTopic("FCMMessage")
    }
}