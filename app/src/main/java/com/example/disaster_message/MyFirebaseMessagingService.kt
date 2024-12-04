package com.example.disaster_message

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService


class MyFirebaseMessagingService : FirebaseMessagingService() {
    //새로운 토큰을 생성할 시에 로그로 보냄
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("asdfqwer", token)
    }


    override fun onCreate() {
        super.onCreate()
        //지정된 토픽을 구독한다.
        FirebaseMessaging.getInstance().subscribeToTopic("testMessage")
    }
}