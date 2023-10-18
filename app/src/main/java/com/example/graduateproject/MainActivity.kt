package com.example.graduateproject

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    val db = Firebase.firestore
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync(this)

        supportActionBar?.title = ""

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

        // Get the current location
        getMyCurrentLocation()
    }

    private fun btnStartActivity() {
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
        if (!isRecording) {
            return
        }
        Toast.makeText(this, "您已經結束運動！", Toast.LENGTH_SHORT).show()

        isRecording = false
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val endTime = System.currentTimeMillis()
        val durationInSeconds = (endTime - startTime) / 1000
        val distanceInMeters = calculateDistance(activityPath)

        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val seconds = durationInSeconds % 60

        val formattedTime = String.format("%d小時%d分%d秒", hours, minutes, seconds)

        val infoDialog = AlertDialog.Builder(this)
            .setTitle("本次運動紀錄")
            .setMessage("運動時長： $formattedTime \n運動距離： ${"%.3f".format(distanceInMeters / 1000)} 公里")
            .setPositiveButton("OK") { _, _ ->
                activityPath.clear()
                activityPolyline?.remove()
            }
            .create()

        infoDialog.show()
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
                placeMarkerOnMap(currentLatLong)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 12f))
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

    private fun placeMarkerOnMap(currentLatLong: LatLng) {
        val markerOptions = MarkerOptions().position(currentLatLong)
        markerOptions.title("$currentLatLong")
        mMap.addMarker(markerOptions)

        if (isRecording) {
            activityPath.add(currentLatLong)
            if (activityPath.size >= 2) {
                val polylineOptions = PolylineOptions()
                    .color(ContextCompat.getColor(this, R.color.red))
                    .width(10f)
                    .addAll(activityPath)
                activityPolyline = mMap.addPolyline(polylineOptions)
            }
        }
    }

    override fun onMarkerClick(marker: Marker) = false

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