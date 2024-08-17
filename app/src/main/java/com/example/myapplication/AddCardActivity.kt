package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddCardActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var frontImageView: ImageView
    private lateinit var backImageView: ImageView
    private lateinit var saveButton: Button

    private val REQUEST_IMAGE_FRONT = 1
    private val REQUEST_IMAGE_BACK = 2
    private val REQUEST_CAMERA_FRONT = 3
    private val REQUEST_CAMERA_BACK = 4
    private val REQUEST_CROP_IMAGE = 5

    private var currentImagePath: String? = null
    private var isFrontImage: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_card)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        titleEditText = findViewById(R.id.titleEditText)
        frontImageView = findViewById(R.id.frontImageView)
        backImageView = findViewById(R.id.backImageView)
        saveButton = findViewById(R.id.saveButton)

        val frontTextView = findViewById<TextView>(R.id.frontTextView)
        frontTextView.bringToFront()

        val backTextView = findViewById<TextView>(R.id.backTextView)
        backTextView.bringToFront()

        frontImageView.setOnClickListener {
            isFrontImage = true
            showImageSourceDialog(frontImageView)
        }

        backImageView.setOnClickListener {
            isFrontImage = false
            showImageSourceDialog(backImageView)
        }

        saveButton.setOnClickListener {
            validateAndSaveCard()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle the back button press
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun showImageSourceDialog(imageView: ImageView) {
        val options = mutableListOf("Capture from Camera", "Choose from Gallery")

        // Add "Remove Image" option if the image view already has an image
        if (imageView.drawable != null) {
            options.add("Remove Image")
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image Source")
        builder.setItems(options.toTypedArray()) { _, which ->
            when (options[which]) {
                "Capture from Camera" -> openCamera()
                "Choose from Gallery" -> openGallery()
                "Remove Image" -> imageView.setImageDrawable(null) // Remove the image
            }
        }
        builder.show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile = createImageFile()
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.example.myapplication.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, if (isFrontImage) REQUEST_CAMERA_FRONT else REQUEST_CAMERA_BACK)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, if (isFrontImage) REQUEST_IMAGE_FRONT else REQUEST_IMAGE_BACK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_FRONT, REQUEST_IMAGE_BACK -> {
                    val imageUri = data?.data
                    if (imageUri != null) {
                        startCropActivity(imageUri)
                    }
                }
                REQUEST_CAMERA_FRONT, REQUEST_CAMERA_BACK -> {
                    currentImagePath?.let {
                        val imageUri = Uri.fromFile(File(it))
                        startCropActivity(imageUri)
                    }
                }
                REQUEST_CROP_IMAGE -> {
                    val imagePath = data?.getStringExtra("imagePath")
                    imagePath?.let {
                        val bitmap = BitmapFactory.decodeFile(it)
                        if (isFrontImage) {
                            frontImageView.setImageBitmap(bitmap)
                        } else {
                            backImageView.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }
    }

    private fun startCropActivity(imageUri: Uri) {
        val intent = Intent(this, CropImageActivity::class.java)
        intent.putExtra("imageUri", imageUri.toString())
        startActivityForResult(intent, REQUEST_CROP_IMAGE)
    }

    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(null)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentImagePath = absolutePath
        }
    }

    private fun validateAndSaveCard() {
        val title = titleEditText.text.toString().trim()

        // Validate the input fields
        if (title.isEmpty() && frontImageView.drawable == null) {
            Toast.makeText(this, "Please enter card name and at least front image of the card", Toast.LENGTH_SHORT).show()
            return
        } else if (frontImageView.drawable == null && title.isNotEmpty()) {
            Toast.makeText(this, "At least insert front image of the card", Toast.LENGTH_SHORT).show()
            return
        } else if (title.isEmpty() && frontImageView.drawable != null) {
            Toast.makeText(this, "Please enter card name", Toast.LENGTH_SHORT).show()
            return
        }

        // Save the card details asynchronously
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val frontBitmap = (frontImageView.drawable as BitmapDrawable).bitmap
                val backBitmap = if (backImageView.drawable != null) {
                    (backImageView.drawable as BitmapDrawable).bitmap
                } else {
                    createWhiteBitmap(800, 800)
                }

                // Save the images to files and get their paths
                val frontImagePath = saveImageToFile(frontBitmap, "front_${System.currentTimeMillis()}.png")
                val backImagePath = saveImageToFile(backBitmap, "back_${System.currentTimeMillis()}.png")

                // Create a new card entity with the file paths
                val newCard = CardEntity(cardName = title, frontImagePath = frontImagePath, backImagePath = backImagePath)

                // Insert the new card into the database
                val db = CardDatabase.getDatabase(applicationContext)
                val cardDao = db.cardDao()
                cardDao.insertCard(newCard)

                // Confirm the insertion
                val insertedCard = cardDao.getCardByName(title)
                if (insertedCard != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddCardActivity, "Card saved successfully!", Toast.LENGTH_SHORT).show()
                        // Start MainActivity
                        val intent = Intent(this@AddCardActivity, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddCardActivity, "Failed to save card. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddCardActivity, "Error saving card: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveImageToFile(bitmap: Bitmap, fileName: String): String {
        val file = File(filesDir, fileName)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
        }
        return file.absolutePath
    }

    private fun createWhiteBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
        }
    }
}
