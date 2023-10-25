package com.example.graduateproject

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.util.concurrent.TimeUnit

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var verificationId: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        supportActionBar?.title = ""

        val actionBar = supportActionBar
        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar)

        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)
        imageView?.setImageResource(R.drawable.logo)

        val edit_Phone = findViewById<EditText>(R.id.edit_Phone)
        val edit_verificationCode = findViewById<EditText>(R.id.edit_verificationCode)
        val btn_register = findViewById<Button>(R.id.btn_register)
        val btn_sendverify = findViewById<Button>(R.id.btn_sendverify)

        btn_sendverify.setOnClickListener {
            val phoneNumber = edit_Phone.text.toString().trim()

            if (validatePhoneNumber(phoneNumber)) {
                checkIfPhoneIsRegistered(phoneNumber) { isRegistered ->
                    if (isRegistered) {
                        Toast.makeText(this, "此手機號碼已註冊!", Toast.LENGTH_SHORT).show()
                    } else {
                        sendVerificationCode("+886" + phoneNumber.substring(1))
                    }
                }
            } else {
                Toast.makeText(this, "無效的手機號碼", Toast.LENGTH_SHORT).show()
            }
        }

        btn_register.setOnClickListener {
            val code = edit_verificationCode.text.toString().trim()
            if (code.isEmpty()) {
                edit_verificationCode.error = "請輸入驗證碼"
                edit_verificationCode.requestFocus()
                return@setOnClickListener
            }

            verifyVerificationCode(code)
        }
    }

    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        return Patterns.PHONE.matcher(phoneNumber).matches() && phoneNumber.length == 10
    }

    private fun checkIfPhoneIsRegistered(phoneNumber: String, callback: (Boolean) -> Unit) {
        firestore.collection("users").document(phoneNumber)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                callback(documentSnapshot.exists())
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@RegisterActivity, "資料庫錯誤: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun sendVerificationCode(e164PhoneNumber: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            e164PhoneNumber,
            60,
            TimeUnit.SECONDS,
            this,
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "驗證碼發送失敗: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(verificationId, token)
                    this@RegisterActivity.verificationId = verificationId
                    Toast.makeText(this@RegisterActivity, "驗證碼已發送", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun verifyVerificationCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId ?: "", code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        val edit_Phone = findViewById<EditText>(R.id.edit_Phone)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "驗證成功", Toast.LENGTH_SHORT).show()
                    savePhoneNumberToDatabase(edit_Phone.text.toString().trim())
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(this, "驗證碼無效", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun savePhoneNumberToDatabase(phoneNumber: String) {
        val emptyMap = HashMap<String, Any>()
        firestore.collection("users").document(phoneNumber)
            .set(emptyMap)
            .addOnSuccessListener {
                addDefaultLocationsForUser(phoneNumber)
                addToLeaderboard(phoneNumber)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "儲存錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addDefaultLocationsForUser(phoneNumber: String) {
        val locations = listOf(
            mapOf("名稱" to "港區藝術中心", "位置" to GeoPoint( 24.2694974,  120.5576727)),
            mapOf("名稱" to "清水第二市場", "位置" to GeoPoint(24.2680596, 120.5607999)),
            mapOf("名稱" to "清水國中", "位置" to GeoPoint( 24.2679703, 120.5630871)),
            mapOf("名稱" to "清水南社社區活動中心", "位置" to GeoPoint(24.2648996,120.5639011)),
            mapOf("名稱" to "清水南社壽德宮", "位置" to GeoPoint(24.2658536, 120.5633261))
        )

        for (location in locations) {
            val name = location["名稱"] as String
            firestore.collection("users").document(phoneNumber).collection("地點").document(name)
                .set(location)
                .addOnFailureListener { e ->
                    Toast.makeText(this, "新增地點錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addToLeaderboard(phoneNumber: String) {
        val leaderboardData = mapOf(
            "姓名" to "",  // 您可以在此加入使用者的名稱或其他資料
            "分數" to 0,
            "階級" to 1
        )

        firestore.collection("排行榜").document(phoneNumber).set(leaderboardData)
            .addOnSuccessListener {
                Toast.makeText(this, "已新增到排行榜", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "新增到排行榜錯誤: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}