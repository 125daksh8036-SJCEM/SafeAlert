package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlertEntity::class, EmergencyContactEntity::class], version = 2, exportSchema = false)
abstract class SafeAlertDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        @Volatile
        private var INSTANCE: SafeAlertDatabase? = null

        fun getDatabase(context: Context): SafeAlertDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeAlertDatabase::class.java,
                    "safealert_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
