package com.example.disaster_message

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


fun getCoordinatesForMultipleAddresses(addresses: List<String>, apiKey: String): Map<String, Pair<Double, Double>> {
    val results = mutableMapOf<String, Pair<Double, Double>>()
    addresses.forEach { address ->
        val coordinates = getCoordinates(address, apiKey)
        if (coordinates != null) {
            results[address] = coordinates
        } else {
            Log.d("Coordinate_Error", "Failed to get coordinates for address: $address")
        }
    }
    return results
}

fun getLocationType(location: String): String {
    val splitArray = location.trim().split(" ")
    val lastWord = splitArray.last()
    val secondLastWord = splitArray.getOrNull(splitArray.size -2)

    return when {
        splitArray.size >= 2 -> {
            when {
                lastWord == "전체" -> {
                    when {
                        secondLastWord?.endsWith("도") == true -> "도" // 130
                        secondLastWord?.endsWith("시") == true -> "시" // 30
                        else -> "군/구" // 기본값 처리
                    }
                }
                lastWord.endsWith("시") -> "시" // 30
                lastWord.endsWith("군") -> "군/구" // 15
                lastWord.endsWith("구") -> "군/구" // 15
                lastWord.endsWith("읍") -> "읍/면/동" // 5
                lastWord.endsWith("면") -> "읍/면/동" // 5
                lastWord.endsWith("동") -> "읍/면/동" // 5
                else -> "시설" // 2
            }
        }
        else -> "군/구"
    }
}
fun getCoordinates(address: String, apiKey: String): Pair<Double, Double>? {
    val client = OkHttpClient()
    val encodedAddress = URLEncoder.encode(address, "UTF-8")
    val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${encodedAddress}&key=$apiKey"
    val request = Request.Builder().url(url).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            Log.d("SharedPreferences_test", "API 요청 실패: ${response.code}")
            return null
        }
        val responseBody = response.body?.string() ?: return null
        val jsonObject = JSONObject(responseBody)
        val resultsArray = jsonObject.getJSONArray("results")

        if (resultsArray.length() > 0) {
            val location = resultsArray.getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
            val lat = location.getDouble("lat")
            val lng = location.getDouble("lng")
            Log.d("location_test", "위도, 경도 : $lat, $lng")
            return Pair(lat, lng)
        } else {
            Log.d("SharedPreferences_test", "결과 없음")
            return null
        }
    }
}

fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // 지구 반지름(km)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon /2 ) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c // 거리 (km)
}

fun getRadiusForLocationType(locationType: String): Double {
    return when (locationType) {
        "도" -> 130.0
        "시" -> 30.0
        "군/구" -> 15.0
        "읍/면/동" -> 5.0
        "시설" -> 2.0
        else -> 20.0 // 애매하니까 20km
    }
}

// 사용자 위치가 수신 지역 범위 안에 있는지 확인
fun isUserWithinDisasterArea(
    userLat: Double,
    userLon: Double,
    disasterLat: Double,
    disasterLon: Double,
    locationType: String
): Boolean {
    val radius = getRadiusForLocationType(locationType) // 동적 거리 계산
    Log.d("location_test", "radius : $radius")
    val distance = haversine(userLat, userLon, disasterLat, disasterLon)
    Log.d("location_test", "distance : $distance")
    return distance <= radius
}