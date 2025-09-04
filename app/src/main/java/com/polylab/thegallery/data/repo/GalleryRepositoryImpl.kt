package com.polylab.thegallery.data.repo

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.polylab.thegallery.core.permissions.PermissionState
import com.polylab.thegallery.core.result.GResult
import com.polylab.thegallery.data.local.db.AppDatabase
import com.polylab.thegallery.data.local.entity.AlbumEntity
import com.polylab.thegallery.data.local.entity.AlbumItemCrossRef
import com.polylab.thegallery.data.local.entity.FavoriteEntity
import com.polylab.thegallery.data.media.MediaStorePagingSource
import com.polylab.thegallery.di.IoDispatcher
import com.polylab.thegallery.domain.model.Album
import com.polylab.thegallery.domain.model.GalleryFilter
import com.polylab.thegallery.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : GalleryRepository {

    private val dao = database.galleryDao()

    override fun getPagedImages(filter: GalleryFilter): Flow<PagingData<MediaItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 60,
                prefetchDistance = 20,
                enablePlaceholders = false,
                initialLoadSize = 60
            ),
            pagingSourceFactory = {
                MediaStorePagingSource(context)
            }
        ).flow.flowOn(ioDispatcher)
    }

    override fun getFavoriteIds(): Flow<List<Long>> = dao.getFavoriteIds()

    override suspend fun toggleFavorite(id: Long): GResult<Boolean> = withContext(ioDispatcher) {
        try {
            val existing = dao.getFavorite(id)
            if (existing == null) {
                dao.upsertFavorite(FavoriteEntity(id))
                GResult.Success(true)
            } else {
                dao.removeFavorite(id)
                GResult.Success(false)
            }
        } catch (e: Exception) {
            GResult.Error(e)
        }
    }

    override fun getAlbums(): Flow<GResult<List<Album>>> = flow {
        emit(GResult.Loading)
        try {
            dao.getAlbumsWithCount().collect { albumsWithCount ->
                val albums = albumsWithCount.map { item ->
                    Album(
                        id = item.album.id,
                        name = item.album.name,
                        createdAt = item.album.createdAt,
                        itemCount = item.itemCount
                    )
                }
                emit(GResult.Success(albums))
            }
        } catch (e: Exception) {
            emit(GResult.Error(e))
        }
    }.flowOn(ioDispatcher)

    override suspend fun createAlbum(name: String): GResult<Long> = withContext(ioDispatcher) {
        try {
            val albumId = dao.insertAlbum(AlbumEntity(name = name))
            GResult.Success(albumId)
        } catch (e: Exception) {
            GResult.Error(e)
        }
    }

    override suspend fun addToAlbum(albumId: Long, mediaId: Long): GResult<Unit> =
        withContext(ioDispatcher) {
            try {
                dao.addToAlbum(AlbumItemCrossRef(albumId, mediaId))
                GResult.Success(Unit)
            } catch (e: Exception) {
                GResult.Error(e)
            }
        }

    override suspend fun removeFromAlbum(albumId: Long, mediaId: Long): GResult<Unit> =
        withContext(ioDispatcher) {
            try {
                dao.removeFromAlbum(albumId, mediaId)
                GResult.Success(Unit)
            } catch (e: Exception) {
                GResult.Error(e)
            }
        }

    override suspend fun getPermissionState(): PermissionState = withContext(ioDispatcher) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                checkPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
            else -> {
                checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    override fun getGalleryItems(): Flow<PagingData<MediaItem>> {
        return Pager(
            PagingConfig(pageSize = 50)
        ) { MediaStorePagingSource(context) }.flow
    }

    override suspend fun insertImage(uri: Uri) {
        // Copy the captured file into MediaStore
        val resolver = context.contentResolver
        val mimeType = "image/jpeg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TheGallery")
        }

        val newUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (newUri != null) {
            resolver.openOutputStream(newUri)?.use { outputStream ->
                resolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    private fun checkPermission(permission: String): PermissionState {
        return when (ContextCompat.checkSelfPermission(context, permission)) {
            PackageManager.PERMISSION_GRANTED -> PermissionState.Granted
            else -> {
                // Don't check for Activity here, as context is ApplicationContext
                // Return Unknown state for initial check, let UI handle rationale
                PermissionState.Denied
            }
        }
    }
}