package com.example.mediagallery

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class FullScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        val imagePath = intent.getStringExtra("imagePath")
        val imageView = findViewById<ImageView>(R.id.fullScreenImage)

        val bitmap = BitmapFactory.decodeFile(imagePath)
        imageView.setImageBitmap(bitmap)
    }
}
