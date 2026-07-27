package com.camera.studio.photoeditor.di

import android.content.Context
import androidx.room.Room
import com.camera.studio.photoeditor.core.UserSettingsStore
import com.camera.studio.photoeditor.data.db.AppDatabase
import com.camera.studio.photoeditor.data.db.SavedMediaDao
import com.camera.studio.photoeditor.data.removebg.BackgroundRemovalService
import com.camera.studio.photoeditor.data.removebg.StubBackgroundRemovalService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI. Repository (MediaRepository) đã @Inject constructor nên không cần @Provides;
 * chỉ cần cung cấp Room DB + DAO + DataStore settings.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "camera_studio.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSavedMediaDao(db: AppDatabase): SavedMediaDao = db.savedMediaDao()

    @Provides
    @Singleton
    fun provideUserSettingsStore(@ApplicationContext context: Context): UserSettingsStore =
        UserSettingsStore(context)

    // TODO(M8): thay bằng implementation gọi API thật khi có contract.
    @Provides
    @Singleton
    fun provideBackgroundRemovalService(): BackgroundRemovalService = StubBackgroundRemovalService()
}
