package com.example.graduateproject

import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecordActivity: BaseActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var listView: ListView
    private lateinit var listAdapter: LocationListAdapter
    private var locationList = mutableListOf<String>()
    private var sanitizedPhoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record)

        firestore = FirebaseFirestore.getInstance()

        listView = findViewById(R.id.list_view)

        fetchLocationsFromDatabase()

        listAdapter = LocationListAdapter(this, locationList, firestore, sanitizedPhoneNumber)
        listView.adapter = listAdapter

        supportActionBar?.title = ""

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

    private fun fetchLocationsFromDatabase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
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

        sanitizedPhoneNumber = "0" + phoneNumber.replace("+886", "")

        firestore.collection("users").document(sanitizedPhoneNumber).collection("地點")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    locationList.add(document.id)
                }
                listAdapter.notifyDataSetChanged()
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
