package com.example.graduateproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var verificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(this@LoginActivity, "驗證失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
            verificationId = verId
            resendToken = token
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val btnQ = findViewById<Button>(R.id.btn_q)
        val btnlogin = findViewById<Button>(R.id.btn_login)
        val edit_phone = findViewById<EditText>(R.id.edit_Phone)
        val edit_verificationCode = findViewById<EditText>(R.id.edit_verificationCode)

        var isCodeSent = false

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val localFormatNumber = convertToLocalFormat(currentUser.phoneNumber ?: "")
            checkIfPhoneIsRegistered(localFormatNumber) { isRegistered ->
                if (isRegistered) {
                    goToMainActivity()
                }
            }
        }

        btnQ.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnlogin.setOnClickListener {
            if (!isCodeSent) {
                val phoneNumber = edit_phone.text.toString().trim()
                if (phoneNumber.isEmpty()) {
                    Toast.makeText(this, "手機號碼不可為空", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                checkIfPhoneIsRegistered(phoneNumber) { isRegistered ->
                    if (isRegistered) {
                        sendVerificationCode(convertToInternationalFormat(phoneNumber))
                        isCodeSent = true
                        btnlogin.text = "確認驗證碼"
                    } else {
                        Toast.makeText(this, "此手機號碼未註冊!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val code = edit_verificationCode.text.toString().trim()
                if (code.isEmpty()) {
                    Toast.makeText(this, "請輸入驗證碼", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                signInWithPhoneAuthCredential(credential)
            }
        }
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun checkIfPhoneIsRegistered(phoneNumber: String, callback: (Boolean) -> Unit) {
        firestore.collection("users").document(phoneNumber)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                callback(documentSnapshot.exists())
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "資料庫錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    goToMainActivity()
                } else {
                    Toast.makeText(this, "認證失敗", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun convertToLocalFormat(phoneNumber: String): String {
        return if (phoneNumber.startsWith("+886")) {
            "0" + phoneNumber.drop(4)
        } else {
            phoneNumber
        }
    }

    private fun convertToInternationalFormat(phoneNumber: String): String {
        return if (phoneNumber.startsWith("09")) {
            "+886" + phoneNumber.drop(1)
        } else {
            phoneNumber
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("退出APP")
            .setMessage("確定要退出應用程式？")
            .setPositiveButton("確定") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}