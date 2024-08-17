// CropImageActivity.kt
package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageView
import java.io.File
import java.io.FileOutputStream

class CropImageActivity : AppCompatActivity() {

    private lateinit var cropImageView: CropImageView
    private lateinit var okButton: Button
    private lateinit var retryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_image)

        cropImageView = findViewById(R.id.cropImageView)
        okButton = findViewById(R.id.okButton)
        retryButton = findViewById(R.id.retryButton)

        val imageUri = Uri.parse(intent.getStringExtra("imageUri"))
        cropImageView.setImageUriAsync(imageUri)

        okButton.setOnClickListener {
            val croppedImage = cropImageView.croppedImage
            croppedImage?.let {
                val imagePath = saveCroppedImage(it) // Save cropped image to a file
                val resultIntent = Intent().apply {
                    putExtra("imagePath", imagePath) // Pass the file path back to AddCardActivity
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        retryButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish() // User can retry capturing the image
        }
    }

    private fun saveCroppedImage(bitmap: Bitmap): String {
        val fileName = "cropped_${System.currentTimeMillis()}.png"
        val file = File(filesDir, fileName)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
        }
        return file.absolutePath // Return the file path
    }
}
