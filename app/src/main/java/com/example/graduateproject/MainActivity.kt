package com.example.graduateproject

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    val db = Firebase.firestore
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth

    private var startMarker: Marker? = null
    private var endMarker: Marker? = null

    private lateinit var mapView: MapView
    private lateinit var mMap: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }

    private var isRecording = false
    private var startTime: Long = 0
    private val activityPath = mutableListOf<LatLng>()
    private var activityPolyline: Polyline? = null

    private val locationLiveData: MutableLiveData<LatLng> by lazy {
        MutableLiveData<LatLng>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync(this)

        supportActionBar?.title = ""

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        val actionBar = supportActionBar

        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar)

        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)
        imageView?.setImageResource(R.drawable.logo)

        val btn_user = findViewById<ImageButton>(R.id.btn_user)
        val btn_place = findViewById<ImageButton>(R.id.btn_place)
        val btn_leaf = findViewById<ImageButton>(R.id.btn_leaf)
        val btn_chart = findViewById<ImageButton>(R.id.btn_chart)
        val btn_start = findViewById<ImageButton>(R.id.btn_start)
        val btn_end = findViewById<ImageButton>(R.id.btn_end)
        val btn_add = findViewById<ImageButton>(R.id.btn_add)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btn_user.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
        }
        btn_place.setOnClickListener {
            val intent = Intent(this, RecordActivity::class.java)
            startActivity(intent)
        }
        btn_leaf.setOnClickListener {
            val intent = Intent(this, TreeActivity::class.java)
            startActivity(intent)
        }
        btn_chart.setOnClickListener {
            val intent = Intent(this, ChartActivity::class.java)
            startActivity(intent)
        }
        btn_start.setOnClickListener {
            btnStartActivity()
        }

        btn_end.setOnClickListener {
            btnEndActivity()
        }
        btn_add.setOnClickListener {
            showInputDialog()
        }

        // Get the current location
        getMyCurrentLocation()
    }

    private fun showInputDialog() {
        // 創建 EditText 用於輸入
        val input = EditText(this)
        input.hint = "輸入地點名稱"

        // 創建 AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("新增地點")
        builder.setView(input)
        builder.setPositiveButton("確定") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                // 若有輸入，則加入到資料庫
                addLocationToDatabase(name)
            } else {
                Toast.makeText(this@MainActivity, "請輸入地點名稱", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("取消", null)

        builder.show()
    }
    private fun addLocationToDatabase(name: String) {
        if (name.isNotEmpty()) {
            // 取得當前使用者 ID
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "未登入的用戶", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // 從 currentUser 中取得手機號碼
            val phoneNumber = currentUser.phoneNumber
            if(phoneNumber.isNullOrEmpty()) {
                Toast.makeText(this, "找不到手機號碼", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            // 移除 +886 前綴
            val sanitizedPhoneNumber = "0" + phoneNumber.replace("+886", "")
            // 使用 phoneNumber 替代 userId
            val userId = sanitizedPhoneNumber


            if (lastLocation != null) {
                // 建立地點的資料
                val locationData = hashMapOf(
                    "名稱" to name,
                    "位置" to GeoPoint(lastLocation.latitude, lastLocation.longitude)
                )

                // 將地點加入到 Firestore
                val name = locationData["名稱"] as String
                db.collection("users").document(userId).collection("地點").document(name)
                    .set(locationData, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(this@MainActivity, "地點已加入!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@MainActivity, "加入失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this@MainActivity, "當前位置未知", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "請輸入地點名稱", Toast.LENGTH_SHORT).show()
        }
    }

    private fun btnStartActivity() {

        val intent = Intent(this, LocationTrackingService::class.java)
        startService(intent)

        if (isRecording) {
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            Toast.makeText(this, "您已經開始運動！", Toast.LENGTH_SHORT).show()

            isRecording = true
            startTime = System.currentTimeMillis()
            activityPath.clear()
            activityPolyline?.remove()

            startMarker = placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude), true)

            val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(1000)

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun btnEndActivity() {

        stopService(Intent(this, LocationTrackingService::class.java))

        if (!isRecording) {
            return
        }
        Toast.makeText(this, "您已經結束運動！", Toast.LENGTH_SHORT).show()

        isRecording = false
        fusedLocationClient.removeLocationUpdates(locationCallback)

        endMarker = placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude), true)

        val endTime = System.currentTimeMillis()
        val durationInSeconds = (endTime - startTime) / 1000
        val distanceInMeters = calculateDistance(activityPath)

        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val seconds = durationInSeconds % 60

        val formattedTime = String.format("%d小時%d分%d秒", hours, minutes, seconds)
        val formattedDistance = "%.3f".format(distanceInMeters / 1000)

        // 1. 計算分數
        val scoreToAdd = calculateScore(minutes.toInt(), formattedDistance.toDouble())

        // 2. 將分數累加到Firebase的數據庫
        val currentUser = firebaseAuth.currentUser
        if(currentUser != null) {
            val phoneNumber = currentUser.phoneNumber
            if(!phoneNumber.isNullOrEmpty()) {
                val sanitizedPhoneNumber = "0" + phoneNumber.replace("+886", "")
                addScoreToLeaderboard(sanitizedPhoneNumber, minutes.toInt(), formattedDistance.toDouble())
            }
        }

        val infoDialog = AlertDialog.Builder(this)
            .setTitle("本次運動紀錄")
            .setMessage("運動時長： $formattedTime \n運動距離： $formattedDistance 公里")
            .setPositiveButton("OK") {_ ,_ ->
                activityPath.clear()
                activityPolyline?.remove()
                startMarker?.remove()
                endMarker?.remove()
            }
            .create()
        addExerciseRecordToDatabase(minutes, formattedDistance, startTime, endTime)
        infoDialog.show()
    }
    fun calculateScore(minutes: Int, kilometers: Double): Int {
        return (minutes * 1) + (kilometers * 10).toInt()
    }
    fun addScoreToLeaderboard(phoneNumber: String, minutes: Int, kilometers: Double) {
        // 2. 獲取當前使用者在Firebase中的分數
        firestore.collection("排行榜").document(phoneNumber).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentScore = document.getLong("分數")?.toInt() ?: 0
                    val newScore = currentScore + calculateScore(minutes, kilometers)
                    val newRank = determineRank(newScore)

                    // 3. 新的分數 = 現有的分數 + 剛計算出來的分數
                    val updatedData: MutableMap<String, Any> = hashMapOf(
                        "分數" to newScore as Any,
                        "階級" to newRank as Any
                    )

                    // 4. 將新的分數更新到Firebase中
                    firestore.collection("排行榜").document(phoneNumber).update(updatedData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "分數已更新", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "更新分數錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "找不到使用者資料", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "讀取分數錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    fun determineRank(score: Int): Int {
        return when {
            score < 500 -> 1
            score < 1500 -> 2
            score < 3000 -> 3
            score < 5000 -> 4
            else -> 5
        }
    }
    private fun convertMillisToTimeFormat(millis: Long): String { //轉換開始跟結束時間
        val date = Date(millis)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    private fun addExerciseRecordToDatabase(minutes: Long, formattedDistance: String, startTime: Long, endTime: Long) {
        val currentUser = firebaseAuth.currentUser
        val formattedStartTime = convertMillisToTimeFormat(startTime)
        val formattedEndTime = convertMillisToTimeFormat(endTime)
        if (currentUser == null) {
            Toast.makeText(this, "未登入的用戶", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val phoneNumber = currentUser.phoneNumber
        if(phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "找不到手機號碼", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val sanitizedPhoneNumber = "0" + phoneNumber.replace("+886", "")
        val userId = sanitizedPhoneNumber

        val calendar = Calendar.getInstance()
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val exerciseRecord = hashMapOf(
            "開始時間" to formattedStartTime,
            "結束時間" to formattedEndTime,
            "運動時長(分鐘)" to minutes,
            "運動距離(公里)" to formattedDistance,
            "日期" to currentDate
        )
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendar.time)
        val dateTime = "$currentDate $currentTime"
        db.collection("users").document(userId).collection("運動紀錄").document(dateTime)
            .set(exerciseRecord)
            .addOnSuccessListener {
                Toast.makeText(this@MainActivity, "運動紀錄已加入!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@MainActivity, "加入失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateDistance(path: List<LatLng>): Double {
        var distance = 0.0
        for (i in 1 until path.size) {
            val prev = path[i - 1]
            val current = path[i]
            distance += calculateDistance(prev, current)
        }
        return distance
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val radius = 6371 // Earth radius in km
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val deltaLat = Math.toRadians(end.latitude - start.latitude)
        val deltaLng = Math.toRadians(end.longitude - start.longitude)

        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return radius * c * 1000
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                lastLocation = location
                val currentLatLong = LatLng(location.latitude, location.longitude)
                locationLiveData.value = currentLatLong
                if (isRecording) {
                    activityPath.add(currentLatLong)

                    activityPolyline?.remove()

                    if (activityPath.size >= 2) {
                        val polylineOptions = PolylineOptions()
                            .color(ContextCompat.getColor(this@MainActivity, R.color.red))
                            .width(10f)
                            .addAll(activityPath)
                        activityPolyline = mMap.addPolyline(polylineOptions)
                    }
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 18f))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        }else {
            getMyCurrentLocation()
        }
        mMap.isMyLocationEnabled = true
        mMap.setOnMarkerClickListener(this)
    }

    private fun getMyCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.requestLocationUpdates(
                LocationRequest.create(),
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun placeMarkerOnMap(currentLatLong: LatLng, shouldPlace: Boolean = true): Marker? {
        val markerOptions = MarkerOptions().position(currentLatLong)
        markerOptions.title("$currentLatLong")

        if (shouldPlace) {
            val marker = mMap.addMarker(markerOptions)
            return marker
        }
        return null
    }

    override fun onMarkerClick(marker: Marker) = false

    override fun onBackPressed() {
        if (isRecording) {
            btnEndActivity()
            return
        }

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("退出APP")
            .setMessage("確定要退出應用程式？")
            .setPositiveButton("確定") { _, _ ->
                finishAffinity() // 關閉所有Activity並退出應用程式
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getMyCurrentLocation()

                    if (isRecording) {
                        startMarker = placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                    }
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("需要定位權限")
                        .setMessage("需要定位權限才能使用此程式，請點擊“允許”以授予定位權限。")
                        .setPositiveButton("允許") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                LOCATION_REQUEST_CODE
                            )
                        }
                        .setNegativeButton("取消") { _, _ ->
                            finish()
                        }
                        .create()
                        .show()
                }
            }
        }
    }
}