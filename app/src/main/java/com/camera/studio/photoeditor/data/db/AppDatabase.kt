package com.camera.studio.photoeditor.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SavedMedia::class],
    version = 2, // +is_favorite (M7) — fallbackToDestructiveMigration() ở AppModule tự tạo lại DB.
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedMediaDao(): SavedMediaDao
}
