package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ResetPINActivity : AppCompatActivity() {

    private lateinit var pinFields: Array<EditText>
    private var currentIndex = 0
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var savedPin: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_pin)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        savedPin = sharedPreferences.getString("PIN", "") ?: ""

        // Set up the "Enter" button click listener
        findViewById<Button>(R.id.enterButton).setOnClickListener {
            if (currentIndex == 4) {
                savePin()
            } else {
                Toast.makeText(this, "Please enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onNumberClick(view: View) {
        if (currentIndex < 4) {
            val button = view as Button
            pinFields[currentIndex].setText(button.text)
            currentIndex++
        }
    }

    fun onBackspaceClick(view: View) {
        if (currentIndex > 0) {
            currentIndex--
            pinFields[currentIndex].setText("")
        }
    }

    private fun savePin() {
        val enteredPin = pinFields.joinToString(separator = "") { it.text.toString() }

        if (enteredPin == savedPin) {
            Toast.makeText(this, "Enter a different PIN than the current one", Toast.LENGTH_SHORT).show()
            resetPinFields()
        } else {
            sharedPreferences.edit().putString("PIN", enteredPin).apply()
            Toast.makeText(this, "PIN saved successfully", Toast.LENGTH_SHORT).show()
            startMainActivity()
        }
    }

    private fun resetPinFields() {
        for (pinField in pinFields) {
            pinField.setText("")
        }
        currentIndex = 0
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
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
}
