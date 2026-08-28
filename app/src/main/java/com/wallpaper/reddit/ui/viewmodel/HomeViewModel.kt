package com.wallpaper.reddit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wallpaper.reddit.data.db.entities.SubredditEntity
import com.wallpaper.reddit.data.extractor.ExtractionResult
import com.wallpaper.reddit.data.model.OrientationFilter
import com.wallpaper.reddit.data.model.RedditPost
import com.wallpaper.reddit.data.model.RedditSort
import com.wallpaper.reddit.data.repository.WallpaperRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val subreddits: List<SubredditEntity> = emptyList(),
    val selectedSubreddit: String = "wallpapers",
    val selectedSort: RedditSort = RedditSort.HOT,
    val orientationFilter: OrientationFilter = OrientationFilter.ALL,
    val posts: List<RedditPost> = emptyList(),
    val favoritePostIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val rateLimitMessage: String? = null,
    val errorMessage: String? = null,
    val afterToken: String? = null,
    val isOfflineFeed: Boolean = false
)

class HomeViewModel(
    private val repository: WallpaperRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Collect subreddits
        viewModelScope.launch {
            repository.subreddits.collect { subs ->
                _uiState.update { state ->
                    val selected = if (subs.any { it.name == state.selectedSubreddit }) {
                        state.selectedSubreddit
                    } else {
                        subs.firstOrNull()?.name ?: "wallpapers"
                    }
                    state.copy(subreddits = subs, selectedSubreddit = selected)
                }
            }
        }

        // Collect favorite IDs
        viewModelScope.launch {
            repository.favorites.collect { favs ->
                _uiState.update { it.copy(favoritePostIds = favs.map { f -> f.id }.toSet()) }
            }
        }

        // Initial load
        refreshPosts()
    }

    fun selectSubreddit(subreddit: String) {
        if (_uiState.value.selectedSubreddit != subreddit) {
            _uiState.update { it.copy(selectedSubreddit = subreddit, afterToken = null, posts = emptyList()) }
            loadPosts(isRefresh = true)
        }
    }

    fun selectSort(sort: RedditSort) {
        if (_uiState.value.selectedSort != sort) {
            _uiState.update { it.copy(selectedSort = sort, afterToken = null, posts = emptyList()) }
            loadPosts(isRefresh = true)
        }
    }

    fun setOrientationFilter(filter: OrientationFilter) {
        _uiState.update { it.copy(orientationFilter = filter) }
        loadPosts(isRefresh = true)
    }

    fun addSubreddit(name: String) {
        viewModelScope.launch {
            repository.addSubreddit(name)
            selectSubreddit(name.trim().removePrefix("r/").removePrefix("/"))
        }
    }

    fun deleteSubreddit(name: String) {
        viewModelScope.launch {
            repository.removeSubreddit(name)
        }
    }

    fun toggleFavorite(post: RedditPost) {
        viewModelScope.launch {
            val isFav = _uiState.value.favoritePostIds.contains(post.id)
            repository.toggleFavorite(post, !isFav)
        }
    }

    fun refreshPosts() {
        loadPosts(isRefresh = true)
    }

    fun loadMore() {
        if (!_uiState.value.isLoading && _uiState.value.afterToken != null) {
            loadPosts(isRefresh = false)
        }
    }

    private fun loadPosts(isRefresh: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    rateLimitMessage = null,
                    errorMessage = null
                )
            }

            val result = repository.fetchPosts(
                subreddit = current.selectedSubreddit,
                sort = current.selectedSort,
                after = if (isRefresh) null else current.afterToken,
                orientationFilter = current.orientationFilter
            )

            when (result) {
                is ExtractionResult.Success -> {
                    val newPosts = if (isRefresh) result.posts else current.posts + result.posts
                    _uiState.update {
                        it.copy(
                            posts = newPosts,
                            afterToken = result.after,
                            isLoading = false,
                            isRefreshing = false,
                            isOfflineFeed = false
                        )
                    }
                }
                is ExtractionResult.RateLimited -> {
                    // Fall back to cached posts from database
                    loadCachedFallback(result.message, isRateLimit = true)
                }
                is ExtractionResult.Error -> {
                    // Fall back to cached posts from database
                    loadCachedFallback(result.message, isRateLimit = false)
                }
            }
        }
    }

    private suspend fun loadCachedFallback(message: String, isRateLimit: Boolean) {
        val sub = _uiState.value.selectedSubreddit
        repository.getCachedPosts(sub).firstOrNull()?.let { cached ->
            if (cached.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        posts = cached,
                        isLoading = false,
                        isRefreshing = false,
                        rateLimitMessage = if (isRateLimit) message else null,
                        errorMessage = if (!isRateLimit) message else null,
                        isOfflineFeed = true
                    )
                }
                return
            }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                rateLimitMessage = if (isRateLimit) message else null,
                errorMessage = if (!isRateLimit) message else null
            )
        }
    }
}

class HomeViewModelFactory(
    private val repository: WallpaperRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository) as T
    }
}
