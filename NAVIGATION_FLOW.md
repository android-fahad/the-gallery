# Gallery App Navigation Flow

## Navigation Structure

```
┌──────────────────┐
│  Gallery Screen  │ (Main Grid/List View)
└────────┬─────────┘
         │ Click on any image
         ▼
┌──────────────────────┐
│ ImageGalleryScreen   │ (Swipeable Gallery with Carousel)
└────────┬─────────────┘
         │ Single tap on image
         ▼
┌──────────────────────┐
│ FullscreenImageScreen│ (Zoomable Fullscreen View)
└──────────────────────┘
```

## User Flow

### 1. **Gallery Screen (Main Screen)**
- Shows grid/list/staggered view of all images
- User can:
  - Switch between layouts (Grid/List/Staggered)
  - Search images
  - Filter by categories
  - Mark favorites
  - **Click on any image** → Opens ImageGalleryScreen

### 2. **ImageGalleryScreen (Intermediate Gallery)**
- Shows selected image in a card view with blurred background
- Features:
  - **Swipe left/right** to navigate between images
  - **Double tap** to zoom in/out on current image
  - **Drag vertically** to dismiss and go back
  - Bottom carousel showing all images
  - **Single tap on the main image** → Opens FullscreenImageScreen
  - Click on carousel items to jump to specific images

### 3. **FullscreenImageScreen (Full Immersive View)**
- Shows image in complete fullscreen with black background
- Features:
  - **Pinch to zoom** with pan support
  - **Swipe horizontally** to navigate between images
  - **Tap** to show/hide controls (back, share, info buttons)
  - Auto-hiding controls after 3 seconds
  - Page indicator at bottom
  - **Back button** or gesture to return to ImageGalleryScreen

## Navigation Implementation Details

### Routes
- `gallery` - Main gallery screen
- `image_gallery/{mediaId}` - ImageGalleryScreen with selected image
- `fullscreen_image/{mediaId}/{initialIndex}` - FullscreenImageScreen

### Key Features
1. **Progressive Enhancement**: Each screen adds more functionality
   - Gallery → Browse all images
   - ImageGallery → Focus on single image with quick navigation
   - Fullscreen → Immersive viewing experience

2. **Consistent Navigation**: Back button/gesture always returns to previous screen

3. **State Preservation**: Selected image and scroll position maintained across navigation

## Testing Instructions

1. **Launch the app**
   - Grant media permissions when prompted
   - Gallery screen loads with all images

2. **Test Gallery → ImageGallery navigation**
   - Click on any image in the gallery
   - Should open ImageGalleryScreen with that image selected
   - Bottom carousel should show all images

3. **Test ImageGallery features**
   - Swipe left/right to navigate
   - Double tap to zoom
   - Drag up/down to dismiss
   - Click carousel items to jump

4. **Test ImageGallery → Fullscreen navigation**
   - Single tap on the main image
   - Should open fullscreen view
   - Controls should be visible initially

5. **Test Fullscreen features**
   - Pinch to zoom and pan
   - Tap to toggle controls
   - Swipe to navigate
   - Use back button to return

6. **Test back navigation**
   - From Fullscreen → Back to ImageGallery
   - From ImageGallery → Back to Gallery
   - State should be preserved

## Code Structure

```kotlin
// Navigation setup in GalleryNavigation.kt
Gallery Screen → onClick → navigate(ImageGallery.createRoute(mediaId))
ImageGallery → onImageClick → navigate(FullscreenImage.createRoute(mediaId, index))
FullscreenImage → onBackPressed → popBackStack()
```
