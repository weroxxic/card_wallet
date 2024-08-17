package com.example.myapplication

data class CardItem(
    val title: String,            // Corresponds to cardName in CardEntity
    val frontImagePath: String,   // Path to the front image of the card
    val backImagePath: String     // Path to the back image of the card
)
