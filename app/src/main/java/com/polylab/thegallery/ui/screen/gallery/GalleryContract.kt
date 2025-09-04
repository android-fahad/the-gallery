package com.polylab.thegallery.ui.screen.gallery

import com.polylab.thegallery.core.permissions.PermissionState
import com.polylab.thegallery.domain.model.GalleryFilter
import com.polylab.thegallery.ui.base.UiEffect
import com.polylab.thegallery.ui.base.UiIntent
import com.polylab.thegallery.ui.base.UiState

sealed class GalleryIntent : UiIntent {
    data object LoadImages : GalleryIntent()
    data class ToggleFavorite(val mediaId: Long) : GalleryIntent()
    data class SearchImages(val query: String) : GalleryIntent()
    data class FilterImages(val filter: GalleryFilter) : GalleryIntent()
    data object RequestPermission : GalleryIntent()
    data class NavigateToDetail(val mediaId: Long, val index: Int) : GalleryIntent()
}

data class GalleryState(
    val permissionState: PermissionState = PermissionState.Unknown,
    val filter: GalleryFilter = GalleryFilter(),
    val favoriteIds: List<Long> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) : UiState

sealed class GalleryEffect : UiEffect {
    data class ShowError(val message: String) : GalleryEffect()
    data class NavigateToDetail(val mediaId: Long, val index: Int) : GalleryEffect()
    data object RequestPermission : GalleryEffect()
    data class ShowSuccess(val message: String) : GalleryEffect()
}