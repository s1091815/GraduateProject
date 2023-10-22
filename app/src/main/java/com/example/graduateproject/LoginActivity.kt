package com.example.graduateproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        supportActionBar?.title = ""

        val actionBar = supportActionBar

        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar)

        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)
        imageView?.setImageResource(R.drawable.logo)

        val btnQ = findViewById<Button>(R.id.btn_q)
        val btnlogin = findViewById<Button>(R.id.btn_login)
        val edit_phone = findViewById<EditText>(R.id.edit_Phone)

        firestore = FirebaseFirestore.getInstance()

        if (isLoggedIn()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnQ.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnlogin.setOnClickListener {
            val phoneNumber = edit_phone.text.toString().trim()

            // 檢查手機號碼是否為空
            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "手機號碼不可為空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkIfPhoneIsRegistered(phoneNumber) { isRegistered ->
                if (isRegistered) {
                    setLoggedInStatus(true)//登入狀態開啟
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // 關閉 LoginActivity
                } else {
                    // 顯示一條訊息說明此手機號碼未被註冊
                    Toast.makeText(this, "此手機號碼未註冊!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun checkIfPhoneIsRegistered(phoneNumber: String, callback: (Boolean) -> Unit) {
        val phoneDoc = firestore.collection("users").document(phoneNumber)
        phoneDoc.get().addOnSuccessListener { documentSnapshot ->
            callback(documentSnapshot.exists())
        }.addOnFailureListener { e ->
            Toast.makeText(this, "資料庫錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    //保存登入狀態
    private fun setLoggedInStatus(isLoggedIn: Boolean) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_logged_in", isLoggedIn)
        editor.apply()
    }

    //檢查登入狀態
    private fun isLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    override fun onBackPressed() {
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
}
