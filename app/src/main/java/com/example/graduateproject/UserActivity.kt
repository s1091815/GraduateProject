package com.example.graduateproject

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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