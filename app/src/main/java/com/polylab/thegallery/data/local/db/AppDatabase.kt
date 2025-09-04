package com.polylab.thegallery.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.polylab.thegallery.data.local.dao.GalleryDao
import com.polylab.thegallery.data.local.entity.AlbumEntity
import com.polylab.thegallery.data.local.entity.AlbumItemCrossRef
import com.polylab.thegallery.data.local.entity.FavoriteEntity
import com.polylab.thegallery.data.local.entity.MediaItemEntity

@Database(
    entities = [FavoriteEntity::class, AlbumEntity::class, AlbumItemCrossRef::class, MediaItemEntity::class],
    version = 1, exportSchema = true
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun galleryDao(): GalleryDao
}