package com.example.disaster_message

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = "http://3.21.25.59:8080"  // 서버 URL로 변경

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

//    val fcmApi: FCMApi = retrofit.create(FCMApi::class.java)
}