package com.polylab.thegallery.domain.model

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val itemCount: Int = 0,
    val coverUri: Uri? = null
)