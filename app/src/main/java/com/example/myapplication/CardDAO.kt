package com.example.myapplication

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CardDao {

    // Query to fetch all cards from the database
    @Query("SELECT * FROM cards")
    fun getAllCards(): List<CardEntity>

    // Query to fetch a specific card by its name
    @Query("SELECT * FROM cards WHERE cardName = :cardName LIMIT 1")
    fun getCardByName(cardName: String): CardEntity?

    // Insert a new card into the database
    @Insert
    suspend fun insertCard(card: CardEntity)

    // Update an existing card in the database
    @Update
    suspend fun updateCard(card: CardEntity)

    // Delete a specific card from the database
    @Delete
    suspend fun deleteCard(card: CardEntity)
}
