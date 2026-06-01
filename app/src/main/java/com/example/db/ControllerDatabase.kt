package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LayoutEntity::class], version = 1, exportSchema = false)
abstract class ControllerDatabase : RoomDatabase() {
    abstract fun layoutDao(): LayoutDao

    companion object {
        @Volatile
        private var INSTANCE: ControllerDatabase? = null

        fun getDatabase(context: Context): ControllerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ControllerDatabase::class.java,
                    "controller_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
