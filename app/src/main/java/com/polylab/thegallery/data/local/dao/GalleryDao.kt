package com.polylab.thegallery.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.polylab.thegallery.data.local.entity.AlbumEntity
import com.polylab.thegallery.data.local.entity.AlbumItemCrossRef
import com.polylab.thegallery.data.local.entity.FavoriteEntity
import com.polylab.thegallery.data.local.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    // Favorites
    @Query("SELECT * FROM favorites WHERE mediaId = :id")
    suspend fun getFavorite(id: Long): FavoriteEntity?

    @Query("SELECT mediaId FROM favorites")
    fun getFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :id")
    suspend fun removeFavorite(id: Long)

    // Albums
    @Insert
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Query("""
        SELECT albums.*, COUNT(album_items.mediaId) as itemCount 
        FROM albums 
        LEFT JOIN album_items ON albums.id = album_items.albumId 
        GROUP BY albums.id 
        ORDER BY albums.createdAt DESC
    """)
    fun getAlbumsWithCount(): Flow<List<AlbumWithItemCount>>

    @Query("""
        SELECT mediaId FROM album_items 
        WHERE albumId = :albumId 
        ORDER BY addedAt DESC
    """)
    suspend fun getAlbumMediaIds(albumId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToAlbum(item: AlbumItemCrossRef)

    @Query("DELETE FROM album_items WHERE albumId = :albumId AND mediaId = :mediaId")
    suspend fun removeFromAlbum(albumId: Long, mediaId: Long)

    // Media cache for better performance
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaItemEntity>)

    @Query("SELECT * FROM media_cache WHERE lastScanned > :since ORDER BY dateTaken DESC")
    suspend fun getCachedMediaItems(since: Long): List<MediaItemEntity>
}

data class AlbumWithItemCount(
    @Embedded val album: AlbumEntity,
    val itemCount: Int
)