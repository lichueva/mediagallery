package com.example.mediagallery

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val IMAGE_LIST = mutableListOf<String>()
    private lateinit var adapter: ImageAdapter
    private lateinit var notificationManager: NotificationManager

    private val takePhotoResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                val savedPath = saveImageToGallery(imageBitmap)
                if (savedPath != null) {
                    IMAGE_LIST.add(savedPath)
                    adapter.notifyDataSetChanged()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            showNotification(savedPath)
                        } else {
                            requestNotificationPermission()
                        }
                    } else {
                        showNotification(savedPath)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = ImageAdapter(IMAGE_LIST)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val takePhotoButton = findViewById<Button>(R.id.btnTakePhoto)
        takePhotoButton.setOnClickListener {
            if (checkCameraPermission()) {
                takePhoto()
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoResultLauncher.launch(intent)
    }

    private fun saveImageToGallery(bitmap: Bitmap): String? {
        val folder = getExternalFilesDir("MediaGallery")
        folder?.mkdirs()
        val file = File(folder, "IMG_${System.currentTimeMillis()}.jpg")
        return try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showNotification(imagePath: String) {
        val file = File(imagePath)
        if (!file.exists()) {
            Toast.makeText(this, "Файл не існує", Toast.LENGTH_SHORT).show()
            return
        }

        val fullScreenIntent = Intent(this, FullScreenActivity::class.java).apply {
            putExtra("imagePath", imagePath)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bitmap = BitmapFactory.decodeFile(imagePath)

        val notification = NotificationCompat.Builder(this, "MediaGalleryChannel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Фото збережено!")
            .setContentText("Натисніть, щоб переглянути на повний екран")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap))
            .build()

        notificationManager.notify(1, notification)
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "MediaGalleryChannel",
                "Media Gallery",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
            }
        }
    }
}
