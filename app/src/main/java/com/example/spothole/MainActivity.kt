package com.example.spothole

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var button: Button = findViewById(R.id.button123)
        button.setOnClickListener{
            startActivity(Intent(this, MapsActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out,)
        }
    }
}