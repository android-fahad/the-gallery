package com.polylab.thegallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import com.polylab.thegallery.ui.screen.gallery.GalleryScreen
import com.polylab.thegallery.ui.screen.gallery.GalleryViewModel
import com.polylab.thegallery.ui.screen.image.ImageGalleryScreen
import com.polylab.thegallery.ui.screen.image.fullscreen.FullscreenImageScreen

sealed class Screen(val route: String) {
    object Gallery : Screen("gallery")
    object ImageGallery : Screen("image_gallery/{mediaId}/{selectedIndex}") {
        fun createRoute(mediaId: Long, selectedIndex: Int): String {
            return "image_gallery/$mediaId/$selectedIndex"
        }
    }
    object FullscreenImage : Screen("fullscreen_image/{mediaId}/{initialIndex}") {
        fun createRoute(mediaId: Long, initialIndex: Int = 0): String {
            return "fullscreen_image/$mediaId/$initialIndex"
        }
    }
}

@Composable
fun GalleryNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Gallery.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onNavigateToDetail = { mediaId, selectedIndex ->
                    // Navigate to ImageGalleryScreen first
                    navController.navigate(Screen.ImageGallery.createRoute(
                        mediaId,
                        selectedIndex = selectedIndex
                    ))
                }
            )
        }
        
        composable(
            route = Screen.ImageGallery.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
            val viewModel: GalleryViewModel = hiltViewModel()
            val items = viewModel.galleryItems.collectAsLazyPagingItems()
            
            // Get list of URIs and find selected index
            val imageUris = remember(items.itemCount) {
                items.itemSnapshotList.items.filterNotNull().map { it.uri.toString() }
            }
            val selectedIndex = remember(mediaId, items.itemCount) {
                items.itemSnapshotList.items.filterNotNull().indexOfFirst { it.id == mediaId }.coerceAtLeast(0)
            }
            
            ImageGalleryScreen(
                imageUris = imageUris,
                initialSelectedIndex = selectedIndex,
                onBackPressed = {
                    navController.popBackStack()
                },
                onImageClick = { clickedIndex ->
                    // Get the mediaId of clicked image
                    val clickedMediaId = items.itemSnapshotList.items.filterNotNull().getOrNull(clickedIndex)?.id ?: 0L
                    // Navigate to fullscreen view
                    navController.navigate(Screen.FullscreenImage.createRoute(clickedMediaId, clickedIndex))
                }
            )
        }
        
        composable(
            route = Screen.FullscreenImage.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.LongType },
                navArgument("initialIndex") { 
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
            val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0
            
            FullscreenImageScreen(
                mediaId = mediaId,
                initialIndex = initialIndex,
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}
