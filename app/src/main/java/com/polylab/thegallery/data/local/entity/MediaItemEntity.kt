package com.polylab.thegallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_cache")
data class MediaItemEntity(
    @PrimaryKey val id: Long,
    val displayName: String?,
    val mimeType: String?,
    val dateTaken: Long?,
    val width: Int?,
    val height: Int?,
    val size: Long?,
    val bucketDisplayName: String?,
    val lastScanned: Long = System.currentTimeMillis()
)