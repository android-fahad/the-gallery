package com.polylab.thegallery.data.local.entity

import androidx.room.Entity
import androidx.room.Index


@Entity(
    tableName = "album_items",
    primaryKeys = ["albumId", "mediaId"],
    indices = [Index("mediaId")]
)
data class AlbumItemCrossRef(
    val albumId: Long,
    val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
