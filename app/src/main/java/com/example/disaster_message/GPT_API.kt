package com.example.disaster_message

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GPT_API {
    // companion object는 해당 클래스의 정적멤버 역할을 하는 객체 = static 역할
    companion object {
        private val gson = Gson()
        private val GPT_API_KEY = BuildConfig.GPT_API_KEY
    }

    data class DisasterMessage(
        @SerializedName("Title") var title: String,
        @SerializedName("Body") var body: String
    )


    data class GptResponse(
        val choices: List<Choice>
    )

    data class Choice(
        val message: Message
    )

    data class Message(
        val content: String
    )

    fun callGPTAPI(userLanguage: String, title: String, body: String): DisasterMessage {

        val prompt = """
            다음 재난 메시지를 ${userLanguage}로 번역하세요.
            반드시 Title과 Body를 완전히 번역해야 하며, 숫자, 고유명사, 특수기호를 포함한 모든 문장을 그대로 번역하십시오.
            결과를 JSON 형식으로 반환하세요.
            예시: {"Title": "Translated Title", "Body": "Translated Body"}

            Title: $title
            Body: $body
        """.trimIndent()

        val response = makeGPTRequest(prompt)
        Log.d("SharedPreferences_test", "response : $response")
        Log.d("SharedPreferences_test", "유저랭귀지 : $userLanguage")

        if (response.contains("ㅇ") && userLanguage != "한국어") {
            callGPTAPI(userLanguage, title, body)
            Log.d("SharedPreferences_test", "한국어 번역 -> 다시 호출")
        }

        try {
            // JSON 응답에서 중괄호로 시작하는 부분만 추출
            val jsonStr = response.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            Log.d("SharedPreferences_test", "jsonStr : $jsonStr")
            val tmp = Gson().fromJson(jsonStr, DisasterMessage::class.java)
            Log.d("SharedPreferences_test", "tmp : $tmp")
            return tmp
        } catch (e: Exception) {
            Log.e("SharedPreferences_test", "JSON 파싱 오류", e)
            throw e
        }
    }

    private fun makeGPTRequest(prompt: String): String {
        Log.d("SharedPreferences_test", "prompt : $prompt")
        val url = URL("https://api.openai.com/v1/chat/completions")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $GPT_API_KEY")
            Log.d("SharedPreferences_test", GPT_API_KEY)
            doOutput = true
        }

        // Gson을 사용하여 JSON 생성
        val requestMap = mapOf(
            "model" to "gpt-4",
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            )
        )
        val requestBody = gson.toJson(requestMap)

        try {
            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            return when (val responseCode = connection.responseCode) {
                in 200..299 -> {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        val response = gson.fromJson(reader.readText(), GptResponse::class.java)
                        response.choices.firstOrNull()?.message?.content ?: "분석 결과 없음"
                    }
                }
                else -> {
                    val error = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    throw Exception("GPT API 오류 ($responseCode): $error")
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}