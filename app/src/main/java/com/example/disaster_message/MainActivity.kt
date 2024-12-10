package com.example.disaster_message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: CustomAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var language: TextView
    private val scope = CoroutineScope(Dispatchers.IO)
//    private lateinit var prefs_l : SharedPreferences
    private lateinit var languagesData: SetupActivity.Languages
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.rv)
        recyclerView.layoutManager = LinearLayoutManager(this)
        language = findViewById(R.id.language)

        val prefs_l = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        val messageList = loadMessages()
        adapter = CustomAdapter(messageList)
        recyclerView.adapter = adapter

        val json = assets.open("languages.json").bufferedReader().use { it.readText() }
        languagesData = Json.decodeFromString<SetupActivity.Languages>(json)

        val targetValue = prefs_l.getString("user_language", "English")
        Log.d("SharedPreferences_test", "선택한 언어 : $targetValue")

        language.text = "Language : ${languagesData.findByValue(targetValue)}"
        val settings = findViewById<ImageView>(R.id.settings)
        settings.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun SetupActivity.Languages.findByValue(value: String?): String? {
        return this.languages.entries.firstOrNull { it.value == value }?.key
    }

    private fun loadMessages(): ArrayList<Profile> {
        val prefs = getSharedPreferences("disaster_log", Context.MODE_PRIVATE)
        val logJson = prefs.getString("messages", "[]")
        val messageList = ArrayList<Profile>()

        try {
            val type = object : TypeToken<ArrayList<Profile>>() {}.type
            val savedMessages = Gson().fromJson<ArrayList<Profile>>(logJson, type)

            val currentTime = System.currentTimeMillis()
            savedMessages.forEach { profile ->
                val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                val profileTime = formatter.parse(profile.date)?.time ?: 0

                // 일주일 이내의 메시지만 추가 ( 밀리초 계산 )
                if (currentTime - profileTime <= 7 * 24 * 60 * 60 * 1000) // 7일 * 24시간 * 60분 * 60초 * 1000밀리초
                {
                    messageList.add(profile)
                }
            }

            messageList.sortByDescending { profile ->
                val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                formatter.parse(profile.date)?.time ?: 0
            }
        } catch (e: Exception) {
            Log.e("SharedPreferences_test", "메시지 로드 실패", e)
        }

        return messageList
    }
}