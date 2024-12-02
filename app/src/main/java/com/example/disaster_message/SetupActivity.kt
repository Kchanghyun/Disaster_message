package com.example.disaster_message

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.serialization.json.Json
import java.io.File
import android.Manifest
import kotlinx.serialization.Serializable

class SetupActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var languageSpinner: Spinner
    private lateinit var searchLocationEdit: EditText
    private lateinit var nextButton: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)

        // UI 초기화
        initializeUI()

        // 다음 버튼 클릭 처리
        nextButton.setOnClickListener {
            // 설정 저장
            saveSettings()

            // 메인 화면으로 이동
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if ( checkLocationPermission() ) {
            mMap.isMyLocationEnabled = true
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1)
            return false
        }
        return true
    }

    private fun initializeUI() {
        languageSpinner = findViewById(R.id.languageSpinner)
        searchLocationEdit = findViewById(R.id.searchLocationEdit)
        nextButton = findViewById(R.id.nextButton)

        // 스피너 설정
        @Serializable
        data class Languages(val languages: Map<String, String>)

        val json = assets.open("languages.json").bufferedReader().use { it.readText() }
        val languagesData = Json.decodeFromString<Languages>(json)

        val languageKeys = languagesData.languages.keys.toList()
        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, languageKeys)
        languageSpinner.adapter = adapter
    }

    private fun saveSettings() {
        prefs.edit().apply {
            putString("selected_language",
                languageSpinner.selectedItem.toString())
            putString("location", searchLocationEdit.text.toString())
            putBoolean("is_first_run", false)
            apply()
        }
    }
}