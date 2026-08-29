package com.wallpaper.reddit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wallpaper.reddit.data.model.RedditPost
import com.wallpaper.reddit.ui.components.SetWallpaperDialog
import com.wallpaper.reddit.ui.theme.HeartRed
import com.wallpaper.reddit.ui.theme.RedditOrange
import com.wallpaper.reddit.ui.viewmodel.ViewerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperViewerScreen(
    post: RedditPost,
    viewModel: ViewerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(post) {
        viewModel.setPost(post)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Fullscreen Zoomable/Pannable Image
            val imageUrl = post.mediaUrl ?: post.galleryImages.firstOrNull()?.url ?: post.thumbnailUrl
            AsyncImage(
                model = imageUrl,
                contentDescription = post.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = maxOf(1f, minOf(4f, scale)),
                        scaleY = maxOf(1f, minOf(4f, scale)),
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
            )

            // Top Bar Scrim and Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Open in Reddit Browser
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.postUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open Reddit", tint = Color.White)
                        }

                        // Favorite Button
                        IconButton(
                            onClick = { viewModel.toggleFavorite() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (uiState.isFavorite) HeartRed else Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Action Bar Scrim & Buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    // Post title & metadata
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "r/${post.subreddit} • by ${post.author}" +
                                if (post.width != null && post.height != null) " • ${post.width}×${post.height}" else "",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.75f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Set Wallpaper Button
                        Button(
                            onClick = { viewModel.openSetDialog() },
                            enabled = !uiState.isApplying,
                            colors = ButtonDefaults.buttonColors(containerColor = RedditOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isApplying) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Set Wallpaper")
                            }
                        }

                        // Download Button
                        OutlinedButton(
                            onClick = { viewModel.downloadImage() },
                            enabled = !uiState.isDownloading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            if (uiState.isDownloading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download")
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showSetDialog) {
        SetWallpaperDialog(
            onDismiss = { viewModel.dismissSetDialog() },
            onSelectTarget = { target -> viewModel.applyWallpaper(target) }
        )
    }
}
