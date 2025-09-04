package com.polylab.thegallery.data.repo

import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingData
import com.polylab.thegallery.core.permissions.PermissionState
import com.polylab.thegallery.core.result.GResult
import com.polylab.thegallery.data.local.entity.AlbumEntity
import com.polylab.thegallery.domain.model.Album
import com.polylab.thegallery.domain.model.GalleryFilter
import com.polylab.thegallery.domain.model.MediaItem
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
interface GalleryRepository {
    fun getPagedImages(filter: GalleryFilter = GalleryFilter()): Flow<PagingData<MediaItem>>
    fun getFavoriteIds(): Flow<List<Long>>
    suspend fun toggleFavorite(id: Long): GResult<Boolean>
    fun getAlbums(): Flow<GResult<List<Album>>>
    suspend fun createAlbum(name: String): GResult<Long>
    suspend fun addToAlbum(albumId: Long, mediaId: Long): GResult<Unit>
    suspend fun removeFromAlbum(albumId: Long, mediaId: Long): GResult<Unit>
    suspend fun getPermissionState(): PermissionState

    suspend fun insertImage(uri: Uri)
    fun getGalleryItems(): Flow<PagingData<MediaItem>>
}