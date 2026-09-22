package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPageEntity

@Database(
    entities = [DocumentEntity::class, DocumentPageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun scannerDao(): ScannerDao

    companion object {
        @Volatile
        private var INSTANCE: ScannerDatabase? = null

        fun getDatabase(context: Context): ScannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScannerDatabase::class.java,
                    "scanner_pro.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
