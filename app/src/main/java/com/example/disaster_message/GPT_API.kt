package com.example.disaster_message

import com.example.disaster_message.Disaster_message_api.Companion
import com.example.disaster_message.Disaster_message_api.DisasterMessage
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GPT_API {
    // companion object는 해당 클래스의 정적멤버 역할을 하는 객체 = static 역할
    companion object {
        private val gson = Gson()
        private const val GPT_API_KEY = "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"
    }

    data class GptResponse(
        val choices: List<Choice>
    )

    data class Choice(
        val message: Message
    )

    data class Message(
        val content: String
    )

    private var user_language = "태국어"

    fun callGPTAPI(message: Disaster_message_api.DisasterMessage): DisasterMessage {
        val prompt = """
            다음 재난문자를 ${user_language}로 그냥 번역만 해줘.
            "${message.MSG_CN}"
        """.trimIndent()

        message.MSG_CN = makeGPTRequest(prompt)
        return message
    }

    private fun makeGPTRequest(prompt: String): String {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $GPT_API_KEY")
            doOutput = true
        }

        // Gson을 사용하여 JSON 생성
        val requestMap = mapOf(
            "model" to "gpt-3.5-turbo",
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