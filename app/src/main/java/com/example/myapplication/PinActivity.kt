package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PinActivity : AppCompatActivity() {

    private lateinit var pinFields: Array<EditText>
    private var currentIndex = 0
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var backgroundImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        // Initialize EditText fields
        pinFields = arrayOf(
            findViewById(R.id.pin1),
            findViewById(R.id.pin2),
            findViewById(R.id.pin3),
            findViewById(R.id.pin4)
        )

        // Disable keyboard for EditText fields
        for (pinField in pinFields) {
            pinField.showSoftInputOnFocus = false
        }

        backgroundImageView = findViewById(R.id.backgroundImageView)
        val instructionTextView: TextView = findViewById(R.id.instructionTextView)

        // Load and resize the background image
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.blurred)
        val resizedBitmap = resizeBitmap(originalBitmap, 1024, 1024)  // Adjust the max width and height as needed
        backgroundImageView.setImageBitmap(resizedBitmap)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        val savedPin = sharedPreferences.getString("PIN", null)

        if (savedPin == null) {
            // No PIN set, show the first launch dialog
            showFirstLaunchDialog()
            instructionTextView.text = "Set up a new PIN and don't forget it."
        } else {
            // PIN is already set, prompt the user to enter it
            instructionTextView.text = "Enter your PIN"
        }
    }

    private fun showFirstLaunchDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_first_launch, null)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val understandButton = dialogView.findViewById<Button>(R.id.understandButton)
        understandButton.setOnClickListener {
            // Dismiss the dialog and allow the user to set the PIN
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    fun onNumberClick(view: View) {
        if (currentIndex < 4) {
            val button = view as Button
            pinFields[currentIndex].setText(button.text)
            currentIndex++

            if (currentIndex == 4) {
                verifyPin()
            }
        }
    }

    fun onBackspaceClick(view: View) {
        if (currentIndex > 0) {
            currentIndex--
            pinFields[currentIndex].setText("")
        }
    }

    private fun verifyPin() {
        val enteredPin = pinFields.joinToString(separator = "") { it.text.toString() }
        val savedPin = sharedPreferences.getString("PIN", null)

        if (savedPin == null) {
            // Set a new PIN
            sharedPreferences.edit().putString("PIN", enteredPin).apply()
            Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show()
            setPinVerified()
            startMainActivity()
        } else if (enteredPin == savedPin) {
            // Correct PIN entered
            setPinVerified()
            startMainActivity()
        } else {
            // Incorrect PIN entered
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            // Clear the fields for re-entry
            for (pinField in pinFields) {
                pinField.setText("")
            }
            currentIndex = 0
        }
    }

    private fun setPinVerified() {
        sharedPreferences.edit().putBoolean("PIN_VERIFIED", true).apply()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun resizeBitmap(image: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio: Float = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxWidth
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxHeight
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }
}
