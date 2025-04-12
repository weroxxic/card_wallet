package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.pdf.EncryptionConstants
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class CardDetailsActivity : AppCompatActivity() {

    private lateinit var cardNameTextView: TextView
    private lateinit var frontImageView: ImageView
    private lateinit var backImageView: ImageView
    private lateinit var rotateFrontButton: Button
    private lateinit var rotateBackButton: Button
    private lateinit var shareButton: Button

    private var frontImageRotation = 0f
    private var backImageRotation = 0f

    private val preferences by lazy { getSharedPreferences("CardPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)

        // Enable the back button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Card Details"

        cardNameTextView = findViewById(R.id.cardNameTextView)
        frontImageView = findViewById(R.id.frontImageView)
        backImageView = findViewById(R.id.backImageView)
        rotateFrontButton = findViewById(R.id.rotateFrontButton)
        rotateBackButton = findViewById(R.id.rotateBackButton)
        shareButton = findViewById(R.id.shareButton)

        // Retrieve card details from the intent
        val cardName = intent.getStringExtra("CARD_NAME")
        val frontImagePath = intent.getStringExtra("FRONT_IMAGE_PATH")
        val backImagePath = intent.getStringExtra("BACK_IMAGE_PATH")

        cardNameTextView.text = cardName

        // Load images from paths and apply saved rotations
        frontImageRotation = preferences.getFloat("${cardName}_frontRotation", 0f)
        backImageRotation = preferences.getFloat("${cardName}_backRotation", 0f)

        frontImageView.setImageBitmap(loadAndRotateBitmap(frontImagePath, frontImageRotation))
        backImageView.setImageBitmap(loadAndRotateBitmap(backImagePath, backImageRotation))

        // Set up rotation buttons
        rotateFrontButton.setOnClickListener {
            frontImageRotation += 90f
            rotateImage(frontImageView, frontImageRotation)
            saveRotationState(cardName, "frontRotation", frontImageRotation)
        }

        rotateBackButton.setOnClickListener {
            backImageRotation += 90f
            rotateImage(backImageView, backImageRotation)
            saveRotationState(cardName, "backRotation", backImageRotation)
        }

        // Set up share button
        shareButton.setOnClickListener {
            showPasswordDialog(cardName, frontImagePath, backImagePath)
        }
    }

    // Function to rotate an image view
    private fun rotateImage(imageView: ImageView, rotation: Float) {
        imageView.rotation = rotation % 360
        imageView.requestLayout()
    }

    // Function to save the rotation state
    private fun saveRotationState(cardName: String?, key: String, rotation: Float) {
        preferences.edit().putFloat("${cardName}_$key", rotation).apply()
    }

    // Load and rotate bitmap with the specified rotation
    private fun loadAndRotateBitmap(imagePath: String?, rotation: Float): Bitmap {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Function to prompt the user for a password and then share the card as a PDF
    private fun showPasswordDialog(cardName: String?, frontImagePath: String?, backImagePath: String?) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_password, null)
        builder.setView(dialogView)

        val passwordEditText = dialogView.findViewById<EditText>(R.id.passwordEditText)

        builder.setTitle("Enter PDF Password")
            .setPositiveButton("OK") { _, _ ->
                val password = passwordEditText.text.toString()
                if (password.length >= 4) {
                    val pdfFile = createPdf(cardName, frontImagePath, backImagePath, password)
                    sharePdf(pdfFile)
                } else {
                    Toast.makeText(this, "Please enter a password of at least 4 letters", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Function to create a password-protected PDF with the correct layout
    private fun createPdf(cardName: String?, frontImagePath: String?, backImagePath: String?, password: String): File {
        val pdfFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "$cardName.pdf")

        val writerProperties = WriterProperties().setStandardEncryption(
            password.toByteArray(),
            password.toByteArray(),
            EncryptionConstants.ALLOW_PRINTING,
            EncryptionConstants.ENCRYPTION_AES_128
        )

        val pdfWriter = PdfWriter(FileOutputStream(pdfFile), writerProperties)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        // Load the font for the card name
        val fontPath = "res/font/times_new_roman.ttf" // Ensure the path is correct
        val pdfFont: PdfFont = PdfFontFactory.createFont(fontPath, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED)

        // Add the card name with the same font style
        val cardNameParagraph = Paragraph(cardName).setFont(pdfFont)
            .setFontSize(cardNameTextView.textSize / resources.displayMetrics.scaledDensity)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(cardNameParagraph)

        // Calculate the available space for the images
        val pageWidth = pdfDocument.defaultPageSize.width
        val pageHeight = pdfDocument.defaultPageSize.height

        // Assuming a typical height for the paragraph based on the font size
        val cardNameHeight = (cardNameTextView.textSize / resources.displayMetrics.scaledDensity) * 1.2f

        // Calculate the y-coordinate to place the images right below the card name
        val imagesYPosition = pageHeight - cardNameHeight - (pageHeight * 0.75f) // 75% below the card name

        // Calculate the width available for each image
        val imageWidth = (pageWidth - 60) / 2  // 30 units margin on both sides, and some space between the images

        // Add the front image on the left
        val frontBitmap = getRotatedBitmap(frontImageView)
        val frontImage = Image(ImageDataFactory.create(bitmapToByteArray(frontBitmap)))
            .setWidth(imageWidth)
            .setHeight(pageHeight * 0.75f) // 75% of the page height
            .setFixedPosition(30f, imagesYPosition) // Position front image on the left
        document.add(frontImage)

        // Add the back image on the right
        val backBitmap = getRotatedBitmap(backImageView)
        val backImage = Image(ImageDataFactory.create(bitmapToByteArray(backBitmap)))
            .setWidth(imageWidth)
            .setHeight(pageHeight * 0.75f) // 75% of the page height
            .setFixedPosition(pageWidth / 2 + 15f, imagesYPosition) // Position back image on the right
        document.add(backImage)

        document.close()

        return pdfFile
    }

    // Helper function to get rotated Bitmap from ImageView
    private fun getRotatedBitmap(imageView: ImageView): Bitmap {
        val originalBitmap = (imageView.drawable as BitmapDrawable).bitmap
        val matrix = Matrix()
        matrix.postRotate(imageView.rotation)
        return Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
    }

    // Helper function to convert Bitmap to ByteArray
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    // Function to share the generated PDF
    private fun sharePdf(pdfFile: File) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", pdfFile)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share PDF"))
    }

    // Handle the back button click in the action bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Navigate back to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
