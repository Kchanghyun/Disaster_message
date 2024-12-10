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
import okhttp3.OkHttpClient
import okhttp3.Request
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

//        user_language = prefs.getString("user_language", "영어") ?: "영어"
//
//        val locationList: List<String> = listOf(*location.split(",").toTypedArray())
//        val locations: Map<String, Pair<Double, Double>> = getCoordinatesForMultipleAddresses(locationList, google_map_api_key)
//
//        val userLat = prefs.getFloat("user_latitude", 37.821769F).toDouble()
//        Log.d("SharedPreferences_test", "userLat : $userLat")
//        val userLon = prefs.getFloat("user_longitude", 128.155389F).toDouble()
//        Log.d("SharedPreferences_test", "userLon : $userLon")
//
//        Log.d("SharedPreferences_test", "FCM에서의 user_language : $user_language")
//        Log.d("SharedPreferences_test", "메시지 받음")
//        scope.launch {
//            try {
//                Log.d("SharedPreferences_test", "launch에서의 user_language : $user_language")
//                response_list = gpt_API.callGPTAPI(user_language, title, body)
//                Log.d("SharedPreferences_test", "response_list : $response_list")
//                Log.d("SharedPreferences_test", "gpt_api 호출함")
//                Log.d("SharedPreferences_test", "response : ${response_list.title}")
//                Log.d("SharedPreferences_test", "response : ${response_list.body}")
//                saveMessageToLog(response_list.title, response_list.body)
//                sendNotification(response_list.title, response_list.body)
//                Log.d("SharedPreferences_test", "알림 보냄")
//            } catch (e: Exception) {
//                Log.e("SharedPreferences_test", "Error during translation", e)
//                saveMessageToLog(title, body)
//                sendNotification(title, body) // 번역 실패 시 원문 알림
//            }
//        }

        val locationList: List<String> = listOf(*location.split(",").toTypedArray())

        Log.d("location_test", "FCM으로 받은 위치 : $location")

        val locations: Map<String, Pair<Double, Double>> = getCoordinatesForMultipleAddresses(locationList, google_map_api_key)
        Log.d("location_test", ", 단위로 나눈 문자열 리스트 : $locations")
        val userLat = prefs.getFloat("user_latitude", 37.82177F).toDouble()
        Log.d("location_test", "prefs.Lat : $userLat")
        val userLon = prefs.getFloat("user_longitude", 128.1554F).toDouble()
        Log.d("location_test", "prefs.Lon : $userLon")
        for ((address, coordinates) in locations) {
            val locationType = getLocationType(address)
            Log.d("location_test", "문자열 리스트 순회(주소) : $address")
            Log.d("location_test", "수신 지역 범위 : $locationType")
            val latitude = coordinates.first
            val longitude = coordinates.second
            Log.d("location_test", "수신범위 내? : ${isUserWithinDisasterArea(userLat, userLon, latitude, longitude, locationType)}")
            if(isUserWithinDisasterArea(userLat, userLon, latitude, longitude, locationType)) {
                user_language = prefs.getString("user_language", "영어") ?: "영어"
                Log.d("SharedPreferences_test", "FCM에서의 user_language : $user_language")
                Log.d("SharedPreferences_test", "메시지 받음")
                scope.launch {
                    try {
                        Log.d("SharedPreferences_test", "launch에서의 user_language : $user_language")
                        response_list = gpt_API.callGPTAPI(user_language, title, body)
                        Log.d("SharedPreferences_test", "response_list : $response_list")
                        Log.d("SharedPreferences_test", "gpt_api 호출함")
                        Log.d("SharedPreferences_test", "response : ${response_list.title}")
                        Log.d("SharedPreferences_test", "response : ${response_list.body}")
                        saveMessageToLog(response_list.title, response_list.body)
                        sendNotification(response_list.title, response_list.body)
                        Log.d("SharedPreferences_test", "알림 보냄")
                    } catch (e: Exception) {
                        Log.e("SharedPreferences_test", "Error during translation", e)
                        saveMessageToLog(title, body)
                        sendNotification(title, body) // 번역 실패 시 원문 알림
                    }
                }
                break
            }
        }
        // ex : locations - "강원도 원주시" : (35, 127)
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
            Log.d("SharedPreferences_test", "메시지 저장 성공")
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
            notificationManager.createNotificationChannel(channel)
        }

//        // 전체화면 인텐트 설정
        val Intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("title", title)
            putExtra("body", body)
        }

        val PendingIntent = PendingIntent.getActivity(
            this, 0, Intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val uuidString = UUID.randomUUID().toString()
        val notificationId = uuidString.hashCode()

        // 알람 빌더
        val builder = NotificationCompat.Builder(this, "emergency")
            .setSmallIcon(R.drawable.ic_warning) // 알림 아이콘
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX) // 최대 우선순위
            .setCategory(NotificationCompat.CATEGORY_CALL) // 알람 카테고리 ( 우선순위 : CALL > ALARM )
            .setFullScreenIntent(PendingIntent, true) // 전체화면 인텐트
            .setAutoCancel(false) // 탭하면 알림이 사라지는지 여부
//            .setTimeoutAfter(20000) // 20초 후 자동으로 사라짐
            .setOngoing(false) // 사용자가 스와이프로 제거할 수 없게 설정
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 1000, 500, 1000)) // 진동 패턴
            .setLights(Color.RED, 3000, 3000) // LED 설정
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 잠금화면에서 전체 내용 표시
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(body)) // 긴 텍스트 표시



        // Wake Lock 획득 (화면이 꺼져있을 때도 알림)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "FCM:WakeLock"
        ).apply {
            acquire(10000)
        }

//        wakeLock.acquire(10000) // 10초 동안 화면 켜기
        notificationManager.notify(
            notificationId,
            builder.build()
        )

        Handler(Looper.getMainLooper()).postDelayed({
            notificationManager.cancel(notificationId)
        }, 20000)

        // 진동 실행
        startVibration()

        // WakeLock 해제
        wakeLock.release()
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
    override fun onCreate() {
        super.onCreate()
        //지정된 토픽을 구독한다.
        prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        gpt_API = GPT_API() // gpt_API 객체 초기화
        Log.d("SharedPreferences_test", "google map api key : $google_map_api_key")
        FirebaseMessaging.getInstance().subscribeToTopic("FCMMessage")
    }
}