package com.example.graduateproject

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.graduateproject.RegisterActivity // 替换为正确的包名和类名
import com.example.graduateproject.R // 替换为你的R文件路径

class RegisterActivity: AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        supportActionBar?.title = ""

        val actionBar = supportActionBar

        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar)

        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)
        imageView?.setImageResource(R.drawable.logo)

        val btnregister = findViewById<Button>(R.id.btn_register)

        btnregister.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}