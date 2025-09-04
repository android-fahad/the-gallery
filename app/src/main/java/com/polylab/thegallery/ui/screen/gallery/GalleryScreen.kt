package com.polylab.thegallery.ui.screen.gallery

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.polylab.thegallery.core.permissions.PermissionState
import com.polylab.thegallery.domain.model.MediaItem
import com.polylab.thegallery.ui.screen.image.ImageGalleryScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import java.util.Date
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onNavigateToDetail: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lazyPagingItems = viewModel.pagedImages.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    val items = viewModel.galleryItems.collectAsLazyPagingItems()
    val context = LocalContext.current

    // Animation states
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedLayout by remember { mutableStateOf(GalleryLayout.GRID) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            scope.launch {
                viewModel.handleIntent(GalleryIntent.LoadImages)
            }
        }
    }
    
    // Check for required permissions
    val requiredPermissions = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is GalleryEffect.NavigateToDetail -> {
                    onNavigateToDetail(effect.mediaId, effect.index)
                }
                is GalleryEffect.ShowError -> {
                    // Handle error (show snackbar, etc.)
                }
                is GalleryEffect.RequestPermission -> {
                    // Request permissions using the launcher
                    permissionLauncher.launch(requiredPermissions)
                }
                is GalleryEffect.ShowSuccess -> {
                    // Handle success message
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            // Modern App Bar with animations
            AnimatedAppBar(
                isSearchExpanded = isSearchExpanded,
                onSearchExpandedChange = { isSearchExpanded = it },
                selectedLayout = selectedLayout,
                onLayoutChange = { selectedLayout = it },
                searchQuery = state.filter.searchQuery ?: "",
                onSearchQueryChange = { query ->
                    scope.launch {
                        viewModel.handleIntent(GalleryIntent.SearchImages(query))
                    }
                },
                itemCount = lazyPagingItems.itemCount,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Filter Chips
            CategoryFilterRow(
                selectedCategory = state.filter.albumId?.toString() ?: "All",
                onCategorySelected = { category ->
                    scope.launch {
                        val albumId = if (category == "All") null else category.toLongOrNull()
                        viewModel.handleIntent(
                            GalleryIntent.FilterImages(
                                state.filter.copy(albumId = albumId)
                            )
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )


            // Gallery Content
            when {
                state.permissionState != PermissionState.Granted -> {
                    PermissionScreen(
                        permissionState = state.permissionState,
                        onRequestPermission = {
                            // Directly launch permission request
                            permissionLauncher.launch(requiredPermissions)
                        }
                    )
                }
                lazyPagingItems.loadState.refresh is LoadState.Loading -> {
                    LoadingGalleryGrid(selectedLayout)
                }
                lazyPagingItems.itemCount == 0 -> {
                    EmptyGalleryState(viewModel)
                }
                else -> {
                    AnimatedGalleryContent(
                        lazyPagingItems = lazyPagingItems,
                        items,
                        layout = selectedLayout,
                        favoriteIds = state.favoriteIds,
                        onImageClick = { mediaItem ->
                            // Navigate to fullscreen view using the navigation callback
                            onNavigateToDetail(mediaItem.id, items.itemSnapshotList.items.indexOf(mediaItem))
                        },
                        onToggleFavorite = { mediaId ->
                            scope.launch {
                                viewModel.handleIntent(GalleryIntent.ToggleFavorite(mediaId))
                            }
                        }
                    )


                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedAppBar(
    isSearchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    selectedLayout: GalleryLayout,
    onLayoutChange: (GalleryLayout) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row with title and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated title section
                AnimatedContent(
                    targetState = isSearchExpanded,
                    transitionSpec = {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "title_animation"
                ) { expanded ->
                    if (!expanded) {
                        Column {
                            Text(
                                text = "Gallery",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$itemCount items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.height(56.dp))
                    }
                }

                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search toggle button
                    IconButton(
                        onClick = { onSearchExpandedChange(!isSearchExpanded) },
                        modifier = Modifier
                            .background(
                                color = if (isSearchExpanded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchExpanded) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Layout toggle
                    LayoutToggleButton(
                        selectedLayout = selectedLayout,
                        onLayoutChange = onLayoutChange
                    )
                }
            }

            // Animated search bar
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Search your photos...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = modifier
    )
}

@Composable
private fun LayoutToggleButton(
    selectedLayout: GalleryLayout,
    onLayoutChange: (GalleryLayout) -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GalleryLayout.values().forEach { layout ->
            val isSelected = selectedLayout == layout
            IconButton(
                onClick = { onLayoutChange(layout) },
                modifier = Modifier
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = when (layout) {
                        GalleryLayout.GRID -> Icons.Default.GridView
                        GalleryLayout.LIST -> Icons.Default.ViewList
                        GalleryLayout.STAGGERED -> Icons.Default.ViewQuilt
                    },
                    contentDescription = layout.name,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Recent", "Favorites", "Videos", "Screenshots")

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(categories) { index, category ->
            val isSelected = selectedCategory == category

            FilterChip(
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                selected = isSelected,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.animateItem(
                    /*animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )*/
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedGalleryContent(
    lazyPagingItems: LazyPagingItems<MediaItem>,
    items: LazyPagingItems<MediaItem>,
    layout: GalleryLayout,
    favoriteIds: List<Long>,
    onImageClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    when (layout) {
        GalleryLayout.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 90.dp),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(0.5.dp),
                verticalArrangement = Arrangement.spacedBy(0.5.dp)
            ) {
                items(
                    count = items.itemCount,
                    key = { index -> items[index]?.id ?: index }
                ) { index ->
                    items[index]?.let { mediaItem ->
                        AnimatedGridItem(
                            mediaItem = mediaItem,
                            isFavorite = favoriteIds.contains(mediaItem.id),
                            onClick = { onImageClick(mediaItem) },
                            onToggleFavorite = { onToggleFavorite(mediaItem.id) },
                            animationDelay = (index % 12) * 50,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        GalleryLayout.LIST -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = { index -> lazyPagingItems[index]?.id ?: index }
                ) { index ->
                    lazyPagingItems[index]?.let { mediaItem ->
                        AnimatedListItem(
                            mediaItem = mediaItem,
                            isFavorite = favoriteIds.contains(mediaItem.id),
                            onClick = { onImageClick(mediaItem) },
                            onToggleFavorite = { onToggleFavorite(mediaItem.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        GalleryLayout.STAGGERED -> {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = { index -> lazyPagingItems[index]?.id ?: index }
                ) { index ->
                    lazyPagingItems[index]?.let { mediaItem ->
                        AnimatedStaggeredItem(
                            mediaItem = mediaItem,
                            isFavorite = favoriteIds.contains(mediaItem.id),
                            onClick = { onImageClick(mediaItem) },
                            onToggleFavorite = { onToggleFavorite(mediaItem.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedGridItem(
    mediaItem: MediaItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    animationDelay: Int = 0,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var isImageLoaded by remember { mutableStateOf(false) }

    // Animation states
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale_animation"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevation_animation"
    )

    // Entry animation
    LaunchedEffect(mediaItem.id) {
        delay(animationDelay.toLong())
        isImageLoaded = true
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(if (mediaItem.aspectRatio > 0) mediaItem.aspectRatio else 1f)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box {
            // Image with loading animation
            AsyncImage(
                model = mediaItem.uri,
                contentDescription = mediaItem.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize()
                    .graphicsLayer {
                        alpha = if (isImageLoaded) 1f else 0f
                    },
                contentScale = ContentScale.Crop,
                onSuccess = { isImageLoaded = true }
            )

            // Shimmer loading effect
            if (!isImageLoaded) {
                ShimmerEffect(
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Video indicator
            if (mediaItem.mimeType?.startsWith("video") == true) {
                VideoIndicator(
                    duration = "2:45", // You'd get this from MediaStore
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }

            // Favorite button with animation
/*            FavoriteButton(
                isFavorite = isFavorite,
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(0.dp)
            )*/

            // Selection overlay
            SelectionOverlay(
                isSelected = false, // You'd manage this state
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay for text
/*            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Image info overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = mediaItem.displayName ?: "Unknown",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = formatDate(mediaItem.dateTaken),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }*/
        }
    }
}

@Composable
private fun AnimatedListItem(
    mediaItem: MediaItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "list_scale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = mediaItem.uri,
                    contentDescription = mediaItem.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (mediaItem.mimeType?.startsWith("video") == true) {
                    VideoIndicator(
                        duration = "2:45",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = mediaItem.displayName ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = formatDate(mediaItem.dateTaken),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${mediaItem.width} × ${mediaItem.height}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    FavoriteButton(
                        isFavorite = isFavorite,
                        onClick = onToggleFavorite,
                        size = 20.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedStaggeredItem(
    mediaItem: MediaItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectRatio = if (mediaItem.aspectRatio > 0) mediaItem.aspectRatio else 1f
    val height = remember { 150.dp + (50.dp * Random.nextFloat()) }

    AnimatedGridItem(
        mediaItem = mediaItem,
        isFavorite = isFavorite,
        onClick = onClick,
        onToggleFavorite = onToggleFavorite,
        modifier = modifier.height(height)
    )
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "favorite_scale"
    )

    val color by animateColorAsState(
        targetValue = if (isFavorite) Color.Red else Color.White,
        animationSpec = tween(200),
        label = "favorite_color"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size + 16.dp)
            .background(
                color = Color.Black.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .scale(scale)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = color,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun VideoIndicator(
    duration: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SelectionOverlay(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isSelected,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = modifier
                .background(
                    Color.Blue.copy(alpha = 0.3f)
                )
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = Color.Blue,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    )
                    .padding(2.dp)
            )
        }
    }
}

@Composable
private fun ShimmerEffect(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
            )
    )
}

@Composable
private fun LoadingGalleryGrid(
    layout: GalleryLayout,
    modifier: Modifier = Modifier
) {
    when (layout) {
        GalleryLayout.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = modifier
            ) {
                items(20) { index ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .animateItem(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier
            ) {
                items(10) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row {
                            ShimmerEffect(
                                modifier = Modifier
                                    .width(120.dp)
                                    .fillMaxHeight()
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ShimmerEffect(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(16.dp)
                                )
                                ShimmerEffect(
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .height(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CameraLauncher(
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for camera
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            onImageCaptured(photoUri!!)
        }
    }

    fun createImageUri(): Uri? {
        val imageFile = File.createTempFile(
            "IMG_${System.currentTimeMillis()}",
            ".jpg",
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    Button(
        onClick = {
            val uri = createImageUri()
            photoUri = uri
            uri?.let { launcher.launch(it) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Open Camera",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }

}


@Composable
private fun EmptyGalleryState(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated empty state icon
        val infiniteTransition = rememberInfiniteTransition(label = "empty_animation")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                )
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No photos found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Your gallery is empty. Take some photos to get started!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        CameraLauncher {
                uri -> viewModel.addImage(uri)
        }
    }
}

@Composable
private fun PermissionScreen(
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated permission icon
        val infiniteTransition = rememberInfiniteTransition(label = "permission_animation")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "permission_scale"
        )

        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape
                )
                .padding(24.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = when (permissionState) {
                PermissionState.Denied -> "Gallery Access Required"
                PermissionState.PermanentlyDenied -> "Permission Denied"
                else -> "Grant Gallery Permission"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = when (permissionState) {
                PermissionState.Denied -> "We need access to your photos to show your gallery. This permission is required for the app to function."
                PermissionState.PermanentlyDenied -> "Gallery permission has been permanently denied. Please enable it in your device settings to use this feature."
                else -> "Allow access to view and organize your photos"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (permissionState == PermissionState.PermanentlyDenied) {
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                } else {
                    onRequestPermission()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (permissionState == PermissionState.PermanentlyDenied)
                    "Open Settings" else "Grant Permission",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}



// Extension functions for formatting
private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return "Unknown"

    val date = Date(timestamp)
    val now = Date()
    val diffTime = Math.abs(now.time - date.time)
    val diffDays = (diffTime / (1000 * 60 * 60 * 24)).toInt()

    return when {
        diffDays == 0 -> "Today"
        diffDays == 1 -> "Yesterday"
        diffDays < 7 -> "$diffDays days ago"
        diffDays < 30 -> "${diffDays / 7} weeks ago"
        diffDays < 365 -> "${diffDays / 30} months ago"
        else -> "${diffDays / 365} years ago"
    }
}

private fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes == 0L) return "0 B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()

    return DecimalFormat("#,##0.#").format(
        bytes / Math.pow(1024.0, digitGroups.toDouble())
    ) + " " + units[digitGroups]
}

