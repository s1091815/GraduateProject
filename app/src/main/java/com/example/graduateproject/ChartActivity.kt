package com.example.graduateproject

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class ChartActivity: BaseActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var listView: ListView
    private lateinit var recordAdapter: RecordAdapter
    private var exerciseRecordList = mutableListOf<Record>()
    private var sanitizedPhoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chart)

        supportActionBar?.title = ""

        firestore = FirebaseFirestore.getInstance()

        listView = findViewById(R.id.listview)

        recordAdapter = RecordAdapter(this, exerciseRecordList)
        listView.adapter = recordAdapter

        fetchRecordFromDatabase()
        val actionBar = supportActionBar

        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar_withhome)

        val bnt_backhome = actionBar?.customView?.findViewById<ImageButton>(R.id.bnt_backhome)
        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)

        imageView?.setImageResource(R.drawable.logo)

        bnt_backhome?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun fetchRecordFromDatabase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "未登入的用戶", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val phoneNumber = currentUser.phoneNumber
        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "找不到手機號碼", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        sanitizedPhoneNumber = "0" + phoneNumber.replace("+886", "")

        firestore.collection("users").document(sanitizedPhoneNumber).collection("運動紀錄")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val record = Record(
                        date = document.data["日期"] as? String ?: "",
                        distance = document.data["運動距離(公里)"] as? String ?: "",
                        time = (document.data["運動時長(分鐘)"] as? Long)?.toString() ?: ""
                    )
                    exerciseRecordList.add(record)
                }

                exerciseRecordList.reverse()  // 反轉列表

                recordAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "錯誤: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        finish()
    }
}
