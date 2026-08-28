package com.wallpaper.reddit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wallpaper.reddit.data.model.RedditPost
import com.wallpaper.reddit.data.repository.WallpaperRepository
import com.wallpaper.reddit.ui.components.WallpaperCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    repository: WallpaperRepository,
    onNavigateToViewer: (RedditPost) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by repository.favorites.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite Wallpapers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No favorite wallpapers yet.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(favorites, key = { it.id }) { fav ->
                    val post = RedditPost(
                        id = fav.id,
                        name = "t3_${fav.id}",
                        subreddit = fav.subreddit,
                        title = fav.title,
                        author = "",
                        permalink = fav.postUrl.removePrefix("https://www.reddit.com"),
                        postUrl = fav.postUrl,
                        mediaUrl = fav.mediaUrl,
                        thumbnailUrl = fav.mediaUrl,
                        width = fav.width,
                        height = fav.height,
                        isNsfw = false
                    )
                    WallpaperCard(
                        post = post,
                        isFavorite = true,
                        onCardClick = { onNavigateToViewer(post) },
                        onFavoriteClick = {}
                    )
                }
            }
        }
    }
}
