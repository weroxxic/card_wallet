package com.example.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CardEntity::class], version = 2) // Update version as necessary
abstract class CardDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile
        private var INSTANCE: CardDatabase? = null

        // Define migration from version 1 to version 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: If you added a new column called "description" to the "cards" table
                // Uncomment the following line if you have actually added a new column:
                // database.execSQL("ALTER TABLE cards ADD COLUMN description TEXT")
            }
        }

        fun getDatabase(context: Context): CardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CardDatabase::class.java,
                    "card_database"
                )
                    .addMigrations(MIGRATION_1_2) // Add migration path
                    .fallbackToDestructiveMigration() // This ensures a fallback to destructive migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
