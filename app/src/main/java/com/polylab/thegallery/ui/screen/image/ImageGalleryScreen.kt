package com.polylab.thegallery.ui.screen.image

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ImageGalleryScreen(
    imageUris: List<String>, // List of image URIs from storage
    initialSelectedIndex: Int = 0,
    onBackPressed: () -> Unit = {},
    onImageClick: ((Int) -> Unit)? = null // Add callback for image click
) {
    var selectedIndex by remember { mutableStateOf(initialSelectedIndex) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Animate to selected item when it changes - center it in the carousel
    LaunchedEffect(selectedIndex) {
        coroutineScope.launch {
            // Calculate the offset to center the selected item
            val itemWidth = with(density) { (80 + 8).dp.toPx() } // item size + padding
            val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
            val centerOffset = (screenWidth / 2 - itemWidth / 2).toInt()

            listState.animateScrollToItem(
                index = selectedIndex,
                scrollOffset = -centerOffset
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Blurred background image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUris.getOrNull(selectedIndex))
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(25.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.6f
        )

        // Gradient overlay for better contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.2f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main image card with enhanced gestures
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                ZoomableImageCard(
                    imageUri = imageUris.getOrNull(selectedIndex) ?: "",
                    onDismiss = onBackPressed,
                    onSwipeLeft = {
                        if (selectedIndex < imageUris.size - 1) {
                            selectedIndex++
                        }
                    },
                    onSwipeRight = {
                        if (selectedIndex > 0) {
                            selectedIndex--
                        }
                    },
                    onClick = {
                        // Navigate to fullscreen when image is clicked
                        onImageClick?.invoke(selectedIndex)
                    }
                )
            }

            ImageCarousel(
                imageUris = imageUris,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = { selectedIndex = it }
            )

/*            // Bottom image carousel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.0f),
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = LocalConfiguration.current.screenWidthDp.dp / 2)
                ) {
                    itemsIndexed(imageUris) { index, imageUri ->
                        ImageCarouselItem(
                            imageUri = imageUri,
                            isSelected = index == selectedIndex,
                            index = index,
                            selectedIndex = selectedIndex,
                            onClick = { selectedIndex = index }
                        )
                    }
                }
            }*/
        }
    }
}

@Composable
fun ImageCarousel(
    imageUris: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.0f),
                        Color.Black.copy(alpha = 0.1f)
                    )
                )
            )
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = LocalConfiguration.current.screenWidthDp.dp / 2)
        ) {
            itemsIndexed(imageUris) { index, imageUri ->
                ImageCarouselItem(
                    imageUri = imageUri,
                    isSelected = index == selectedIndex,
                    index = index,
                    selectedIndex = selectedIndex,
                    onClick = {
                        onSelectedIndexChange(index)
                        // Scroll to center the selected item
                        coroutineScope.launch {
                            listState.animateScrollToItem(
                                index = index,
                                scrollOffset = -10 // This centers the item due to contentPadding
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ZoomableImageCard(
    imageUri: String,
    onDismiss: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onClick: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var dismissProgress by remember { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetX"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetY"
    )

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(0.5f, 4f)
        scale = newScale

        // Apply pan only if zoomed in
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y

            // Constrain pan to image bounds
            val maxOffsetX = (scale - 1f) * 200f
            val maxOffsetY = (scale - 1f) * 200f
            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            // Reset offsets when zoomed out
            offsetX = 0f
            offsetY = 0f
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .graphicsLayer {
                scaleX = animatedScale * (1f - dismissProgress * 0.2f)
                scaleY = animatedScale * (1f - dismissProgress * 0.2f)
                translationX = animatedOffsetX
                translationY = animatedOffsetY + (dismissProgress * 100f)
                alpha = 1f - dismissProgress * 0.7f
            }
            .transformable(state = transformableState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Single tap to open fullscreen
                        onClick()
                    },
                    onDoubleTap = {
                        // Double tap to zoom toggle
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        // Handle drag to dismiss (both up and down)
                        if (dismissProgress > 0.3f) {
                            onDismiss()
                        } else {
                            dismissProgress = 0f
                        }

                        // Handle horizontal swipe for navigation
                        if (scale <= 1f && abs(offsetX) > 100f) {
                            if (offsetX > 0) {
                                onSwipeRight()
                            } else {
                                onSwipeLeft()
                            }
                        }

                        // Reset offsets after swipe
                        if (scale <= 1f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                ) { _,change ->
                    if (scale <= 1f) {
                        // Handle dismiss gesture (drag up or down)
                        val verticalDrag = abs(change.y)
                        if (verticalDrag > abs(change.x)) {
                            dismissProgress = (verticalDrag / size.height).coerceIn(0.3f, 0.5f)
                            offsetY = change.y
                        } else {
                            // Handle horizontal swipe for navigation
                            offsetX += change.x
                        }
                    }
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .crossfade(true)
                .build(),
            contentDescription = "Selected image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun ImageCarouselItem(
    imageUri: String,
    isSelected: Boolean,
    index: Int,
    selectedIndex: Int,
    onClick: () -> Unit
) {
    val distance = abs(index - selectedIndex)

    // Calculate scale based on distance from selected item
    val scale by animateFloatAsState(
        targetValue = when (distance) {
            0 -> 1.2f
            1 -> 1.05f
            2 -> 1f
            else -> 0.9f
        },
        animationSpec = tween(300),
        label = "scale"
    )

    // Calculate alpha based on distance
    val alpha by animateFloatAsState(
        targetValue = when (distance) {
            0 -> 1f
            1 -> 0.9f
            2 -> 0.8f
            else -> 0.7f
        },
        animationSpec = tween(300),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(if (isSelected) 80.dp else 65.dp)
            .scale(scale)
            .graphicsLayer {
                this.alpha = alpha
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
                .then(
                    if (isSelected) {
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else Modifier
                ),
            shape = RoundedCornerShape(12.dp),
            border = if (isSelected) {
                androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = Color.White
                )
            } else null
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}