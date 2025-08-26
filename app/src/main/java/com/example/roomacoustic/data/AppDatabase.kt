package com.example.roomacoustic.data

import android.content.Context
import androidx.room.*

// data/AppDatabase.kt
@Database(entities = [RoomEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context, AppDatabase::class.java, "room-db"
                ).build().also { INSTANCE = it }
            }
    }
}
