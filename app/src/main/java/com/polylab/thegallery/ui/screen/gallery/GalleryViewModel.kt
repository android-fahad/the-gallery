package com.polylab.thegallery.ui.screen.gallery

import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.polylab.thegallery.core.permissions.PermissionState
import com.polylab.thegallery.core.result.onError
import com.polylab.thegallery.core.result.onSuccess
import com.polylab.thegallery.data.repo.GalleryRepository
import com.polylab.thegallery.domain.model.GalleryFilter
import com.polylab.thegallery.domain.model.MediaItem
import com.polylab.thegallery.ui.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository
) : MviViewModel<GalleryIntent, GalleryState, GalleryEffect>(GalleryState()) {

    private var imagesFlow: Flow<PagingData<MediaItem>>? = null

    val pagedImages: Flow<PagingData<MediaItem>>
        get() = imagesFlow ?: createPagedFlow()

    private val refreshTrigger = MutableStateFlow(Unit)

    init {
        viewModelScope.launch {
            handleIntent(GalleryIntent.LoadImages)
        }
    }

    val galleryItems = repository.getGalleryItems().cachedIn(viewModelScope)

    override suspend fun handleIntent(intent: GalleryIntent) {
        when (intent) {
            GalleryIntent.LoadImages -> loadImages()
            is GalleryIntent.ToggleFavorite -> toggleFavorite(intent.mediaId)
            is GalleryIntent.SearchImages -> searchImages(intent.query)
            is GalleryIntent.FilterImages -> filterImages(intent.filter)
            GalleryIntent.RequestPermission -> requestPermission()
            is GalleryIntent.NavigateToDetail -> navigateToDetail(intent.mediaId, intent.index)
        }
    }

    private suspend fun loadImages() {
        setState { copy(isLoading = true, error = null) }

        val permissionState = repository.getPermissionState()
        setState { copy(permissionState = permissionState) }

        if (permissionState == PermissionState.Granted) {
            // Start collecting favorite IDs
            repository.getFavoriteIds().collect { favoriteIds ->
                setState { copy(favoriteIds = favoriteIds, isLoading = false) }
            }
        } else {
            setState { copy(isLoading = false) }
        }
    }

    private fun createPagedFlow(): Flow<PagingData<MediaItem>> {
        val flow = repository.getPagedImages(currentState.filter)
            .cachedIn(viewModelScope)
        imagesFlow = flow
        return flow
    }

    private suspend fun toggleFavorite(mediaId: Long) {
        repository.toggleFavorite(mediaId)
            .onSuccess { isFavorite ->
                val message = if (isFavorite) "Added to favorites" else "Removed from favorites"
                setEffect { GalleryEffect.ShowSuccess(message) }
            }
            .onError { error ->
                setEffect { GalleryEffect.ShowError("Failed to update favorite: ${error.message}") }
            }
    }

    private suspend fun searchImages(query: String) {
        val newFilter = currentState.filter.copy(searchQuery = query.takeIf { it.isNotBlank() })
        setState { copy(filter = newFilter) }
        refreshImages()
    }

    private suspend fun filterImages(filter: GalleryFilter) {
        setState { copy(filter = filter) }
        refreshImages()
    }

    private fun refreshImages() {
        imagesFlow = null // Force recreation of flow
    }

    private suspend fun requestPermission() {
        setEffect { GalleryEffect.RequestPermission }
    }

    private suspend fun navigateToDetail(mediaId: Long, index: Int) {
        setEffect { GalleryEffect.NavigateToDetail(mediaId, index) }
    }



    fun refreshGallery() {
        refreshTrigger.value = Unit
    }

    fun addImage(uri: Uri) {
        viewModelScope.launch {
            repository.insertImage(uri)
            delay(1000)
            createPagedFlow()
        }
    }

}