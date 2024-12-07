package com.example.disaster_message

data class User(
//    val id: Int, // 사용자 고유 ID(기본키)
    var userLanguage: String  = "한국어", // 한국어 기본값
    var address: String // 위치 주소
) //{
//    companion object {
//        private var lastId = 0

//        fun generateUserId(): Int {
//            return ++lastId
//        }
//    }
//}