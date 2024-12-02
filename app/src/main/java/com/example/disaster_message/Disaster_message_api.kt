package com.example.disaster_message

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess
import com.example.disaster_message.GPT_API
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 기본 언어 세팅
var user_language = "영어"

class Disaster_message_api {
    companion object {
        private val logger = LoggerFactory.getLogger(Disaster_message_api::class.java)
        private val gson = Gson()
        private const val BASE_URL = "https://www.safetydata.go.kr/V2/api/DSSP-IF-00247"
        private const val SERVICE_KEY = "1I7ILC5YHZ5MJ6ML"  // 본인의 서비스 키로 변경
        private const val POLLING_INTERVAL = 90000L  // 144분 (하루 10회 호출 기준)
    }
    private val gpt = GPT_API()
    private var lastMessageSN: Long = 0

    // API 응답 구조를 위한 데이터 클래스들
    data class ApiResponse(
        val header: Header,
        val body: List<DisasterMessage>,
        val numOfRows: Int,
        val pageNo: Int,
        val totalCount: Int
    )

    data class Header(
        val resultMsg: String,
        val resultCode: String,
        val errorMsg: String?
    )

    data class DisasterMessage(
        var MSG_CN: String,          // 메시지 내용
        val RCPTN_RGN_NM: String,    // 수신 지역
        val CRT_DT: String,          // 생성 일시
        val EMRG_STEP_NM: String,    // 긴급 단계
        val DST_SE_NM: String,       // 재해 구분
        val SN: String,              // 일련번호
        val REG_YMD: String?,        // 등록 일자
        val MDFCN_YMD: String?       // 수정 일자
    )


    private fun formatMessage(message: DisasterMessage): String {
        return """
            |━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            |[재난문자 #${message.SN}]
            |${message.DST_SE_NM} - ${message.EMRG_STEP_NM}
            |━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            |• 발생시각: ${message.CRT_DT}
            |• 발생지역: ${message.RCPTN_RGN_NM}
            |• 내용: ${message.MSG_CN}
            |• 등록일시: ${message.REG_YMD ?: "없음"}
            |• 수정일시: ${message.MDFCN_YMD ?: "없음"}
            |━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            |""".trimMargin()
    }

    private fun fetchAlerts(): List<DisasterMessage> {
        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val urlBuilder = StringBuilder(BASE_URL).apply {
            append("?serviceKey=${URLEncoder.encode(SERVICE_KEY, "UTF-8")}")
            append("&returnType=json")
            append("&pageNo=1")
            append("&numOfRows=20")  // 한 번에 가져올 메시지 수
            append("&crtDt=$currentDate")
        }

        val url = URL(urlBuilder.toString())
        logger.debug("요청 URL: $url")

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Charset", "UTF-8")
            connectTimeout = 5000  // 5초
            readTimeout = 5000     // 5초
        }

        return try {
            when (val responseCode = connection.responseCode) {
                in 200..299 -> {
                    val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use {
                        it.readText()
                    }

                    val response = gson.fromJson(responseText, ApiResponse::class.java)
                    if (response.header.resultCode == "00") {
                        // 새로운 메시지만 필터링
                        val newMessages = response.body.filter { message ->
                            message.SN.toLong() > lastMessageSN
                        }

                        // 새 메시지가 있다면 마지막 메시지 SN 업데이트
                        if (newMessages.isNotEmpty()) {
                            lastMessageSN = newMessages.maxOf { it.SN.toLong() }
                            logger.info("마지막 메시지 SN 업데이트: $lastMessageSN")
                        }

                        newMessages
                    } else {
                        logger.error("API 오류: ${response.header.errorMsg}")
                        emptyList()
                    }
                }
                else -> {
                    logger.error("API 호출 실패: $responseCode")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("API 호출 중 오류 발생: ${e.message}", e)
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    fun startMonitoring() {
        logger.info("""
            |재난문자 모니터링을 시작합니다...
            |• API 주소: $BASE_URL
            |• 호출 간격: ${POLLING_INTERVAL / 1000}초
            |• 예상 일일 호출 횟수: ${86400000 / POLLING_INTERVAL}회
            |━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        """.trimMargin())

        while (true) {
            try {
                val messages = fetchAlerts() // List<DisasterMessage>
                // DisasterMessage => String 타입 변수들의 데이터 클래스
                if (messages.isNotEmpty()) {
                    logger.info("\n=== 새로운 재난문자 ${messages.size}건이 수신되었습니다 ===")
                    messages.forEach { message ->
//                        logger.info("\n${formatMessage(message)}")
                        logger.info("\n${gpt.callGPTAPI(message)}")
                    }
                } else {
                    logger.debug("수신된 재난문자가 없습니다.")
                }
                Thread.sleep(POLLING_INTERVAL)
            } catch (e: Exception) {
                logger.error("모니터링 중 오류 발생", e)
                Thread.sleep(POLLING_INTERVAL)
            }
        }
    }
}