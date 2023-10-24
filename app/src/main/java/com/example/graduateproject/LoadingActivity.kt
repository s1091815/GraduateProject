package com.example.graduateproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.graduateproject.MainActivity
import com.example.graduateproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoadingActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth

    private val loadingTime: Long = 3500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loading)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        supportActionBar?.title = ""

        val actionBar = supportActionBar

        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar)

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val localFormatNumber = convertToLocalFormat(currentUser.phoneNumber ?: "")
            checkIfPhoneIsRegistered(localFormatNumber) { isRegistered ->
                if (isRegistered) {
                    goToMainActivity()
                }
            }
        }else {
            Handler().postDelayed({
                val intent = Intent(this@LoadingActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }, loadingTime)
        }

        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)
        imageView?.setImageResource(R.drawable.logo)

    }

    private fun getCurrentDeviceId(): String {
        return android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
    }
    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    private fun convertToLocalFormat(phoneNumber: String): String {
        return if (phoneNumber.startsWith("+886")) {
            "0" + phoneNumber.drop(4)
        } else {
            phoneNumber
        }
    }
    private fun checkIfPhoneIsRegistered(phoneNumber: String, callback: (Boolean) -> Unit) {
        firestore.collection("users").document(phoneNumber)
            .collection("userInfo").document("deviceInfo")
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val lastLoggedInDevice = documentSnapshot.getString("last_logged_in_device")
                    if (lastLoggedInDevice == getCurrentDeviceId()) {
                        callback(true)
                    } else {
                        firebaseAuth.signOut()
                        Toast.makeText(this, "您已在其他裝置上登入", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoadingActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "資料庫錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
