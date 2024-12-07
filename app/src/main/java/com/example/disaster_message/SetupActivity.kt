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
import android.Manifest
import android.location.Geocoder
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.serialization.Serializable
import java.util.Locale

class SetupActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentAddress: String = "" // 현재 위치 주소 저장용

    private lateinit var languageSpinner: Spinner
    private lateinit var searchLocationEdit: EditText
    private lateinit var nextButton: Button
    private lateinit var prefs: SharedPreferences
    private lateinit var locationText: TextView
    private lateinit var refreshLocationButton: Button
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        locationText = findViewById(R.id.locationText)
        refreshLocationButton = findViewById(R.id.refreshLocationButton)
        backButton = findViewById(R.id.backButton)

        // getSharedPreferences는 SharedPreferences 객체를 생성하거나 가져오는 함수
        prefs = getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // UI 초기화
        initializeUI()

        refreshLocationButton.setOnClickListener {
            checkLocationPermission()
        }

        // 다음 버튼 클릭 처리
        nextButton.setOnClickListener {
            val selectedLanguage = languageSpinner.selectedItem.toString()

            // User 객체 생성
            val user = User(
//                id = User.generateUserId(),
                userLanguage = selectedLanguage,
                address = currentAddress
            )

            saveUserData(user)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        backButton.setOnClickListener {
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

                    val geocoder = Geocoder(this, Locale.getDefault())
                    try {
                        val address = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        address?.firstOrNull()?.let { address ->
                            // 주소를 TextView에 표시
                            val addressText = address.getAddressLine(0) ?: "주소를 찾을 수 없습니다"
                            locationText.text = addressText
                            currentAddress = addressText
                        }
                    } catch (e: Exception) {
                        locationText.text = "주소를 찾을 수 없습니다"
                    }

                    // 지도 업데이트
                    mMap.clear() // 기존 마커 제거
                    // 현재 위치 마커 추가
                    mMap.addMarker(MarkerOptions().position(currentLatLng))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
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

    // 사용자가 권한 요청 팝업(ex: 위치 권한 허용/거부 파업)에 대해 허용 또는 거부를 선택한 후 호출되는 콜백 메서드
    override fun onRequestPermissionsResult(
        requestCode: Int, // 요청 권한 - 요청을 보낼 때 사용한 코드
        permissions: Array<out String>, // 요청 권한의 배열(Manifest.permission.ACCESS_FINE_LOCATION)
        grantResults: IntArray // 사용자가 허용하거나 거부한 결과 배열(PackageManager.PERMISSION_GRANTED or PackageManager.PERMISSION_DENIED)
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) { // requestCode가 LOCATION_PERMISSION_REQUEST_CODE인 경우 실행
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && // 결과 배열이 비어있지 않고, 배열의 첫 번째 항목이 허용인지 확인
                    grantResults[0] == PackageManager.PERMISSION_GRANTED // 사용자가 권한을 허용하면
                ) {
                    getCurrentLocation()
                }
            }
        }
    }

    // static 역할 하는 블록
    companion object {
        // 위치 권한 요청을 식별하기 위해 사용되는 정수 코드
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    // 앱의 UI 구성 요소를 초기화하는 함수
    private fun initializeUI() {
        // view를 변수에 할당
        languageSpinner = findViewById(R.id.languageSpinner)
        locationText = findViewById(R.id.locationText)
        nextButton = findViewById(R.id.nextButton)

        // 스피너 설정
        @Serializable
        data class Languages(val languages: Map<String, String>)

        // assets 파일의 languages.json 파일 열어서 키 가져오기
        val json = assets.open("languages.json").bufferedReader().use { it.readText() }
        val languagesData = Json.decodeFromString<Languages>(json)

        val languageKeys = languagesData.languages.keys.toList()

        // android에서 UI 리스트를 관리하기 위한 기본 어댑터
        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, languageKeys)
        languageSpinner.adapter = adapter
    }

}

/*
여기서 이제 next를 누르면
위치 정보 확인 후 이상 없으면 그 정보를 User 객체의 address, language로 설정
 */