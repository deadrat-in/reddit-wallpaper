package com.wallpaper.reddit.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wallpaper.reddit.data.model.RedditPost
import com.wallpaper.reddit.data.model.TargetScreen
import com.wallpaper.reddit.data.repository.WallpaperRepository
import com.wallpaper.reddit.service.WallpaperSetter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ViewerUiState(
    val post: RedditPost? = null,
    val isDownloading: Boolean = false,
    val isApplying: Boolean = false,
    val isFavorite: Boolean = false,
    val downloadedFile: File? = null,
    val userMessage: String? = null,
    val showSetDialog: Boolean = false
)

class ViewerViewModel(
    private val context: Context,
    private val repository: WallpaperRepository,
    private val wallpaperSetter: WallpaperSetter = WallpaperSetter(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    fun setPost(post: RedditPost) {
        _uiState.update { it.copy(post = post) }
        checkLocalStatus(post.id)
    }

    private fun checkLocalStatus(id: String) {
        viewModelScope.launch {
            val entity = repository.getWallpaperEntity(id)
            if (entity != null) {
                val file = entity.localFilePath?.let { File(it) }?.takeIf { it.exists() }
                _uiState.update {
                    it.copy(
                        isFavorite = entity.isFavorite,
                        downloadedFile = file
                    )
                }
            }
        }
    }

    fun toggleFavorite() {
        val currentPost = _uiState.value.post ?: return
        val newFav = !_uiState.value.isFavorite
        viewModelScope.launch {
            repository.toggleFavorite(currentPost, newFav)
            _uiState.update { it.copy(isFavorite = newFav) }
        }
    }

    fun downloadImage() {
        val currentPost = _uiState.value.post ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, userMessage = null) }
            val file = repository.downloadWallpaperFile(currentPost)
            if (file != null) {
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadedFile = file,
                        userMessage = "Wallpaper saved to device cache!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        userMessage = "Failed to download image."
                    )
                }
            }
        }
    }

    fun openSetDialog() {
        _uiState.update { it.copy(showSetDialog = true) }
    }

    fun dismissSetDialog() {
        _uiState.update { it.copy(showSetDialog = false) }
    }

    fun applyWallpaper(targetScreen: TargetScreen) {
        val currentPost = _uiState.value.post ?: return
        dismissSetDialog()

        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true, userMessage = null) }

            // Ensure image is downloaded
            var file = _uiState.value.downloadedFile
            if (file == null || !file.exists()) {
                file = repository.downloadWallpaperFile(currentPost)
                _uiState.update { it.copy(downloadedFile = file) }
            }

            if (file == null) {
                _uiState.update { it.copy(isApplying = false, userMessage = "Could not download image to apply.") }
                return@launch
            }

            val success = wallpaperSetter.setWallpaperFromFile(file, targetScreen)
            if (success) {
                repository.markWallpaperApplied(currentPost.id)
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        userMessage = "Wallpaper successfully applied to ${targetScreen.displayName}!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        userMessage = "Failed to set wallpaper."
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}

class ViewerViewModelFactory(
    private val context: Context,
    private val repository: WallpaperRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ViewerViewModel(context, repository) as T
    }
}
