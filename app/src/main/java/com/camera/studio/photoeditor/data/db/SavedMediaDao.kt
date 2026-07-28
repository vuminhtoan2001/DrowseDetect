package com.camera.studio.photoeditor.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Port `imagesService.js` (RN): getListImages/getInfoImages/getLatestImage/addImages/delImages. */
@Dao
interface SavedMediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: SavedMedia): Long

    @Query("SELECT * FROM saved_media ORDER BY id DESC")
    fun observeAll(): Flow<List<SavedMedia>>

    @Query("SELECT * FROM saved_media WHERE type = :type ORDER BY id DESC")
    fun observeByType(type: Int): Flow<List<SavedMedia>>

    @Query("SELECT * FROM saved_media ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): SavedMedia?

    @Query("SELECT * FROM saved_media WHERE type = :type ORDER BY id DESC LIMIT 1")
    suspend fun getLatestByType(type: Int): SavedMedia?

    @Query("SELECT * FROM saved_media WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): SavedMedia?

    @Query("SELECT * FROM saved_media WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): SavedMedia?

    @Query("SELECT COUNT(*) FROM saved_media WHERE type = :type")
    fun observeCountByType(type: Int): Flow<Int>

    @Query("DELETE FROM saved_media WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("UPDATE saved_media SET is_favorite = :isFavorite WHERE path = :path")
    suspend fun setFavorite(path: String, isFavorite: Boolean)

    @Query("UPDATE saved_media SET content_uri = :contentUri WHERE path = :path")
    suspend fun setContentUri(path: String, contentUri: String)

    @Query("SELECT * FROM saved_media WHERE is_favorite = 1 ORDER BY id DESC")
    fun observeFavorites(): Flow<List<SavedMedia>>
}
