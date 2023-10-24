package com.example.graduateproject

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

open class BaseActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var deviceListener: ListenerRegistration? = null // 增加一個實例變量以持有我們的監聽器

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onStart() {
        super.onStart()

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val localFormatNumber = convertToLocalFormat(currentUser.phoneNumber ?: "")
            monitorDeviceChange(localFormatNumber)
        }
    }

    override fun onStop() {
        super.onStop()
        deviceListener?.remove()  // 確保在活動停止時移除監聽器
    }

    private fun convertToLocalFormat(phoneNumber: String): String {
        return if (phoneNumber.startsWith("+886")) {
            "0" + phoneNumber.drop(4)
        } else {
            phoneNumber
        }
    }

    private fun monitorDeviceChange(phoneNumber: String) {
        deviceListener = firestore.collection("users").document(phoneNumber)
            .collection("userInfo").document("deviceInfo")
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "資料庫錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val lastLoggedInDevice = documentSnapshot.getString("last_logged_in_device")
                    val currentDeviceId = getCurrentDeviceId()
                    if (lastLoggedInDevice != currentDeviceId) {
                        firebaseAuth.signOut()
                        val intent = Intent(this@BaseActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
    }

    private fun getCurrentDeviceId(): String {
        return android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
    }
}