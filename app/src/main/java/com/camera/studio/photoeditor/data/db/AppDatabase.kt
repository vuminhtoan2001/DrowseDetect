package com.camera.studio.photoeditor.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedMedia::class],
    version = 3, // +content_uri (M9, save-to-gallery) — fallbackToDestructiveMigration() ở AppModule tự tạo lại DB.
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedMediaDao(): SavedMediaDao
}
