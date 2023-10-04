package com.example.graduateproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.graduateproject.RegisterActivity // 替换为正确的包名和类名
import com.example.graduateproject.R // 替换为你的R文件路径

class LoginActivity : AppCompatActivity() {

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

        btnQ.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnlogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
