package com.polylab.thegallery.core.permissions

sealed class PermissionState {
    data object Granted : PermissionState()
    data object Denied : PermissionState()
    data object PermanentlyDenied : PermissionState()
    data object Unknown : PermissionState()
}