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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.serialization.Serializable

class SetupActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentAddress: String = "" // 현재 위치 주소 저장용

    private lateinit var languageSpinner: Spinner
    private lateinit var searchLocationEdit: EditText
    private lateinit var nextButton: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // getSharedPreferences는 SharedPreferences 객체를 생성하거나 가져오는 함수
        prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // UI 초기화
        initializeUI()

        // 다음 버튼 클릭 처리
        nextButton.setOnClickListener {
            // 설정 저장
//            saveSettings()
//
//            // 메인 화면으로 이동
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
            val selectedLanguage = languageSpinner.selectedItem.toString()

            // User 객체 생성
            val user = User(
                id = User.generateUserId(),
                userLanguage = selectedLanguage,
                address = currentAddress
            )

            saveUserData(user)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 위치 권한 확인 및 요청
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            // 권한이 있으면 위치 가져오기
            getCurrentLocation()
        } else {
            // 권한 요청
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    // 현재 위치로 카메라 이동
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // 현재 위치 마커 추가
                    mMap.addMarker(MarkerOptions().position(currentLatLng))

                    // 내 위치 버튼 활성화
                    mMap.isMyLocationEnabled = true
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // 하나의 SharedPreferences 파일에 데이터를 저장 -> 새로운 값 -> 이전 값에 덮어쓰기
    // 여러 사용자의 데이터를 따로 저장하는 구조가 아님
    // 한 기기에서 한 사용자만 사용한다고 가정하면 현재 코드로도 충분함.
    // -> 앱의 내부 저장소에 생성되는 파일이라서 해당 기기에서만 접근 가능
    /*
    1. 한 기기는 한 사람이 사용
    2. 앱의 설정은 기기별로 따로 관리
    3. SharedPreferences는 그 기기의 앱 설정을 저장하는 용도로 사용
     */
    private fun saveUserData(user: User) {
        prefs.edit().apply {
            putString("user_language", user.userLanguage)
            putString("user_address", user.address)
            putBoolean("is_first_run", false)
            apply()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    getCurrentLocation()
                }
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
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

//    private fun saveSettings() {
//        prefs.edit().apply {
//            putString("selected_language",
//                languageSpinner.selectedItem.toString())
//            putString("location", searchLocationEdit.text.toString())
//            putBoolean("is_first_run", false)
//            apply()
//        }
//    }

}

/*
여기서 이제 next를 누르면
위치 정보 확인 후 이상 없으면 그 정보를 User 객체의 address, language로 설정
 */