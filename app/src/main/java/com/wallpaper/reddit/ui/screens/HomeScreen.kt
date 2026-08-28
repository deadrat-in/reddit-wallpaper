package com.wallpaper.reddit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wallpaper.reddit.data.model.OrientationFilter
import com.wallpaper.reddit.data.model.RedditPost
import com.wallpaper.reddit.data.model.RedditSort
import com.wallpaper.reddit.ui.components.*
import com.wallpaper.reddit.ui.viewmodel.HomeUiState
import com.wallpaper.reddit.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToViewer: (RedditPost) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reddit Wallpapers") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    // Sort dropdown
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            RedditSort.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.displayName) },
                                    onClick = {
                                        viewModel.selectSort(sort)
                                        showSortMenu = false
                                    },
                                    leadingIcon = if (uiState.selectedSort == sort) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Orientation filter dropdown
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            OrientationFilter.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.displayName) },
                                    onClick = {
                                        viewModel.setOrientationFilter(filter)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = if (uiState.orientationFilter == filter) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Refresh Button
                    IconButton(onClick = { viewModel.refreshPosts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }

                    // Favorites Button
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favorites")
                    }

                    // Settings Button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Subreddit selector chips
            SubredditChips(
                subreddits = uiState.subreddits,
                selectedSubreddit = uiState.selectedSubreddit,
                onSubredditSelected = { viewModel.selectSubreddit(it) },
                onAddSubredditClick = { showAddDialog = true },
                onDeleteSubreddit = { viewModel.deleteSubreddit(it) }
            )

            // Rate limit / Error / Offline Banners
            if (uiState.rateLimitMessage != null) {
                ErrorBanner(
                    message = uiState.rateLimitMessage!!,
                    isRateLimit = true,
                    onRetry = { viewModel.refreshPosts() }
                )
            } else if (uiState.errorMessage != null) {
                ErrorBanner(
                    message = uiState.errorMessage!!,
                    isRateLimit = false,
                    onRetry = { viewModel.refreshPosts() }
                )
            }

            if (uiState.isOfflineFeed) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "📁 Showing cached offline wallpapers",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Main Content: Loading or Wallpaper Grid
            if (uiState.isRefreshing && uiState.posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.posts.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No wallpapers found in r/${uiState.selectedSubreddit}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.refreshPosts() }) {
                            Text("Refresh Feed")
                        }
                    }
                }
            } else {
                val gridState = rememberLazyGridState()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.posts, key = { it.id }) { post ->
                        WallpaperCard(
                            post = post,
                            isFavorite = uiState.favoritePostIds.contains(post.id),
                            onCardClick = { onNavigateToViewer(post) },
                            onFavoriteClick = { viewModel.toggleFavorite(post) }
                        )
                    }

                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubredditDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { subName ->
                viewModel.addSubreddit(subName)
                showAddDialog = false
            }
        )
    }
}
