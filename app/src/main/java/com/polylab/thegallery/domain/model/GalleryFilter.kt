package com.polylab.thegallery.domain.model

data class GalleryFilter(
    val showFavoritesOnly: Boolean = false,
    val albumId: Long? = null,
    val searchQuery: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_TAKEN_DESC
)