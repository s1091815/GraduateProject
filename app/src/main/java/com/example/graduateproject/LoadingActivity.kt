package com.example.graduateproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.graduateproject.MainActivity
import com.example.graduateproject.R

class LoadingActivity : AppCompatActivity() {

    private val loadingTime: Long = 3500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loading)

        supportActionBar?.title = ""

        val actionBar = supportActionBar

        actionBar?.displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_CUSTOM
        actionBar?.setCustomView(R.layout.custom_actionbar)

        val imageView = actionBar?.customView?.findViewById<ImageView>(R.id.customImageView)
        imageView?.setImageResource(R.drawable.logo)

        Handler().postDelayed({
            val intent = Intent(this@LoadingActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, loadingTime)
    }
}
