package com.polylab.thegallery.ui.screen.gallery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.ui.graphics.vector.ImageVector

enum class GalleryLayout {
    GRID,
    LIST,
    STAGGERED;

    val icon: ImageVector
        get() = when (this) {
            GalleryLayout.GRID -> Icons.Default.GridView
            GalleryLayout.LIST -> Icons.Default.ViewList
            GalleryLayout.STAGGERED -> Icons.Default.ViewQuilt
        }

    val contentDescription: String
        get() = when (this) {
            GalleryLayout.GRID -> "Grid view"
            GalleryLayout.LIST -> "List view"
            GalleryLayout.STAGGERED -> "Staggered grid view"
        }
}
