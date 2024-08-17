package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Primary key for the database
    val cardName: String,            // Corresponds to title in CardItem
    val frontImagePath: String,      // Path to the front image of the card
    val backImagePath: String        // Path to the back image of the card
    // Add any additional fields if necessary, such as a description or other metadata
)
