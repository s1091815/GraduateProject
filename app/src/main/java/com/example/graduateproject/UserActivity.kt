package com.example.graduateproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.graduateproject.RegisterActivity // 替换为正确的包名和类名
import com.example.graduateproject.R // 替换为你的R文件路径

class UserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user)

        supportActionBar?.title = ""

        val actionBar = supportActionBar

        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar)

        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)
        imageView?.setImageResource(R.drawable.logo)

        val btnlogout = findViewById<Button>(R.id.btnlogout)

        btnlogout.setOnClickListener {
            logOut()
        }
    }
    //保存登入狀態
    private fun setLoggedInStatus(isLoggedIn: Boolean) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_logged_in", isLoggedIn)
        editor.apply()
    }
    private fun logOut() {
        setLoggedInStatus(false)
        // 這裡也可以加入其他登出的相關操作，例如清除資料等
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}