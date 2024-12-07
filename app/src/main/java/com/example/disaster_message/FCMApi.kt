//package com.example.disaster_message
//
//import retrofit2.Response
//import retrofit2.http.Body
//import retrofit2.http.POST
//
//interface FCMApi {
//    @POST("/api/fcm/token/register")
//    suspend fun sendToken(@Body tokenRequest: TokenRequest): Response<Unit>
//}
//
//data class TokenRequest(
//    val token: String
//)