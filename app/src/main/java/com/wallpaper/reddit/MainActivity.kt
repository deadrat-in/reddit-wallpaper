package com.wallpaper.reddit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wallpaper.reddit.data.model.RedditPost
import com.wallpaper.reddit.ui.screens.FavoritesScreen
import com.wallpaper.reddit.ui.screens.HomeScreen
import com.wallpaper.reddit.ui.screens.SettingsScreen
import com.wallpaper.reddit.ui.screens.WallpaperViewerScreen
import com.wallpaper.reddit.ui.theme.RedditWallpaperTheme
import com.wallpaper.reddit.ui.viewmodel.HomeViewModel
import com.wallpaper.reddit.ui.viewmodel.HomeViewModelFactory
import com.wallpaper.reddit.ui.viewmodel.SettingsViewModel
import com.wallpaper.reddit.ui.viewmodel.SettingsViewModelFactory
import com.wallpaper.reddit.ui.viewmodel.ViewerViewModel
import com.wallpaper.reddit.ui.viewmodel.ViewerViewModelFactory

class MainActivity : ComponentActivity() {

    private var selectedPostForViewer: RedditPost? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RedditWallpaperApp
        val repository = app.repository

        setContent {
            RedditWallpaperTheme {
                val navController = rememberNavController()

                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(repository)
                )
                val viewerViewModel: ViewerViewModel = viewModel(
                    factory = ViewerViewModelFactory(applicationContext, repository)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(applicationContext)
                )

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToViewer = { post ->
                                selectedPostForViewer = post
                                navController.navigate("viewer")
                            },
                            onNavigateToFavorites = {
                                navController.navigate("favorites")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    composable("viewer") {
                        val post = selectedPostForViewer
                        if (post != null) {
                            WallpaperViewerScreen(
                                post = post,
                                viewModel = viewerViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            navController.popBackStack()
                        }
                    }

                    composable("favorites") {
                        FavoritesScreen(
                            repository = repository,
                            onNavigateToViewer = { post ->
                                selectedPostForViewer = post
                                navController.navigate("viewer")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
