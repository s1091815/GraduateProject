package com.example.graduateproject

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest

class UserActivity : BaseActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private var selectedPhotoResId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user)

        supportActionBar?.title = ""

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        val currentUser = firebaseAuth.currentUser
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

        val actionBar = supportActionBar
        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar_withhome)

        val bnt_backhome = actionBar?.customView?.findViewById<ImageButton>(R.id.bnt_backhome)
        val bnt_sos = actionBar?.customView?.findViewById<ImageButton>(R.id.bnt_sos)
        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)
        val editname = findViewById<EditText>(R.id.editName)
        val editage = findViewById<EditText>(R.id.editAge)
        val editW = findViewById<EditText>(R.id.editWeight)
        val editH = findViewById<EditText>(R.id.editHeight)
        val btnsave = findViewById<Button>(R.id.btn_save)
        val btnlogout = findViewById<Button>(R.id.btnlogout)
        val btn_photo = findViewById<ImageButton>(R.id.btn_photo)
        val editphone = findViewById<EditText>(R.id.editPhone)

        imageView?.setImageResource(R.drawable.logo)

        loadDataFromDatabase(userId, editname, editage, editW, editH, editphone)

        bnt_backhome?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        bnt_sos?.setOnClickListener {
            checkAndRequestPermissions()
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val emergencyPhone = document.getString("緊急連絡電話")
                        emergencyPhone?.let { phone ->
                            getCurrentLocation { location ->
                                location?.let {
                                    sendEmergencyMessage(phone, it)
                                    makeEmergencyCall(phone)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "獲取緊急連絡人失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btn_photo.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.dialog_photos, null)

            val gridView = view.findViewById<GridView>(R.id.gridViewPhotos)
            val photos = listOf(R.drawable.photo1, R.drawable.photo2, R.drawable.photo3, R.drawable.photo4, R.drawable.photo5)
            gridView.adapter = ImageAdapter(this, photos)

            builder.setTitle("請挑選一張作為大頭貼照片")
            val dialog = builder.setView(view).create()

            gridView.setOnItemClickListener { _, _, position, _ ->
                selectedPhotoResId = photos[position]
                val imageViewProfile = findViewById<ImageView>(R.id.imageViewProfile)
                imageViewProfile.setImageResource(selectedPhotoResId!!)
                dialog.dismiss()
            }

            val btnCancel = view.findViewById<Button>(R.id.btnCancel)
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }


        btnsave.setOnClickListener {
            val name = editname.text.toString().trim()
            val age = editage.text.toString().toIntOrNull()
            val weight = editW.text.toString().toDoubleOrNull()
            val height = editH.text.toString().toDoubleOrNull()
            val phone = editphone.text.toString().trim()

            if(name.isEmpty() || age == null || weight == null || height == null) {
                Toast.makeText(this, "請確保所有欄位都有填寫正確", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userData = hashMapOf(
                "姓名" to name,
                "年齡" to age,
                "體重" to weight,
                "身高" to height,
                "照片ID" to selectedPhotoResId,
                "緊急連絡電話" to phone
            )

            firestore.collection("users").document(userId).set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "資料已保存", Toast.LENGTH_SHORT).show()
                    loadDataFromDatabase(userId, editname, editage, editW, editH, editphone)

                    val leaderboardUpdate: HashMap<String, Any> = hashMapOf("姓名" to name)
                    firestore.collection("排行榜").document(userId).update(leaderboardUpdate)
                        .addOnSuccessListener {
                            Toast.makeText(this, "排行榜的姓名已更新", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "更新排行榜的姓名失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "儲存失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnlogout.setOnClickListener {
            logOut()
        }
    }

    private fun getCurrentLocation(callback: (Location?) -> Unit) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        callback(location ?: return)
    }

    private fun sendEmergencyMessage(phoneNumber: String, location: Location?) {
        val smsManager = SmsManager.getDefault()
        val message = if (location != null) {
            val formattedLatitude = String.format("%.6f", location.latitude)
            val formattedLongitude = String.format("%.6f", location.longitude)
            "緊急情况！我的位置是：緯度 $formattedLatitude, 經度 ${location.longitude}"
        } else {
            "緊急情况！我的位置是：未知"
        }
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
    }

    private fun makeEmergencyCall(phoneNumber: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // 顯示解釋為什麼需要權限的對話框，並提供一個按鈕讓用戶授予權限
            requestCallPermission()
            return
        }
        // 權限已被授予，進行撥號
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$phoneNumber")
        startActivity(intent)
    }

    // 添加一個新的方法來請求撥打電話的權限
    private fun requestCallPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PERMISSION)
    }

    private fun checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SEND_SMS_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_SEND_SMS_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "SMS發送權限已授予", Toast.LENGTH_SHORT).show()
                    // 檢查是否還需要請求撥打電話權限
                    checkAndRequestPhoneCallPermission()
                } else {
                    Toast.makeText(this, "需要發送短信權限來進行緊急求助", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CALL_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "撥打電話權限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "需要撥打電話權限來進行緊急求助", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkAndRequestPhoneCallPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PERMISSION)
        }
    }

    companion object {
        private const val REQUEST_CALL_PERMISSION = 1
        private val REQUEST_SEND_SMS_PERMISSION = 2
    }

    private fun loadDataFromDatabase(userId: String, nameET: EditText, ageET: EditText, weightET: EditText, heightET: EditText, phoneET: EditText) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    nameET.setText(document.getString("姓名"))
                    ageET.setText(document.getLong("年齡")?.toString())
                    weightET.setText(document.getDouble("體重")?.toString())
                    heightET.setText(document.getDouble("身高")?.toString())
                    phoneET.setText(document.getString("緊急連絡電話"))
                } else {
                    Toast.makeText(this, "找不到資料", Toast.LENGTH_SHORT).show()
                }
                val photoResId = document.getLong("照片ID")?.toInt()
                if (photoResId != null) {
                    val imageViewProfile = findViewById<ImageView>(R.id.imageViewProfile)
                    imageViewProfile.setImageResource(photoResId)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "載入失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setLoggedInStatus(isLoggedIn: Boolean) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_logged_in", isLoggedIn)
        editor.apply()
    }
    private fun logOut() {
        firebaseAuth.signOut()
        setLoggedInStatus(false)
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        finish()
    }
}