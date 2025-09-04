package com.polylab.thegallery.domain.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String?,
    val mimeType: String?,
    val dateTaken: Long?,
    val dateModified: Long?,
    val width: Int?,
    val height: Int?,
    val size: Long?,
    val bucketDisplayName: String?, // Folder name
    val bucketId: String?,
) {
    val aspectRatio: Float
        get() = if (width != null && height != null && height > 0) {
            width.toFloat() / height.toFloat()
        } else 1f

    val isPortrait: Boolean get() = aspectRatio < 1f
    val isLandscape: Boolean get() = aspectRatio > 1f
}
