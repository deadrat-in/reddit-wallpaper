package com.wallpaper.reddit.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wallpaper.reddit.data.extractor.RedditSessionManager
import com.wallpaper.reddit.data.model.OrientationFilter
import com.wallpaper.reddit.data.model.TargetScreen
import com.wallpaper.reddit.worker.RotationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val autoRotationEnabled: Boolean = false,
    val rotationIntervalHours: Long = 3L,
    val targetScreen: TargetScreen = TargetScreen.BOTH,
    val onlyFavoritesForRotation: Boolean = false,
    val defaultOrientation: OrientationFilter = OrientationFilter.ALL,
    val allowNsfw: Boolean = false,
    val hasActiveBrowserSession: Boolean = false
)

class SettingsViewModel(
    private val context: Context,
    val sessionManager: RedditSessionManager = RedditSessionManager.getInstance(context),
    private val rotationScheduler: RotationScheduler = RotationScheduler(context)
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "reddit_wallpaper_prefs"
        private const val KEY_AUTO_ROTATION = "pref_auto_rotation"
        private const val KEY_ROTATION_INTERVAL = "pref_rotation_interval"
        private const val KEY_TARGET_SCREEN = "pref_target_screen"
        private const val KEY_ONLY_FAVORITES = "pref_only_favorites"
        private const val KEY_ORIENTATION = "pref_orientation"
        private const val KEY_ALLOW_NSFW = "pref_allow_nsfw"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.hasActiveSession.collect { active ->
                _uiState.update { it.copy(hasActiveBrowserSession = active) }
            }
        }
    }

    private fun loadSettings(): SettingsUiState {
        val autoRot = prefs.getBoolean(KEY_AUTO_ROTATION, false)
        val interval = prefs.getLong(KEY_ROTATION_INTERVAL, 3L)
        val targetName = prefs.getString(KEY_TARGET_SCREEN, TargetScreen.BOTH.name) ?: TargetScreen.BOTH.name
        val onlyFavs = prefs.getBoolean(KEY_ONLY_FAVORITES, false)
        val orientName = prefs.getString(KEY_ORIENTATION, OrientationFilter.ALL.name) ?: OrientationFilter.ALL.name
        val nsfw = prefs.getBoolean(KEY_ALLOW_NSFW, false)

        val target = try { TargetScreen.valueOf(targetName) } catch (e: Exception) { TargetScreen.BOTH }
        val orientation = try { OrientationFilter.valueOf(orientName) } catch (e: Exception) { OrientationFilter.ALL }

        return SettingsUiState(
            autoRotationEnabled = autoRot,
            rotationIntervalHours = interval,
            targetScreen = target,
            onlyFavoritesForRotation = onlyFavs,
            defaultOrientation = orientation,
            allowNsfw = nsfw,
            hasActiveBrowserSession = !sessionManager.getCookies().isNullOrBlank()
        )
    }

    fun setAutoRotation(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ROTATION, enabled).apply()
        _uiState.update { it.copy(autoRotationEnabled = enabled) }
        syncRotationWorker()
    }

    fun setRotationInterval(hours: Long) {
        prefs.edit().putLong(KEY_ROTATION_INTERVAL, hours).apply()
        _uiState.update { it.copy(rotationIntervalHours = hours) }
        syncRotationWorker()
    }

    fun setTargetScreen(target: TargetScreen) {
        prefs.edit().putString(KEY_TARGET_SCREEN, target.name).apply()
        _uiState.update { it.copy(targetScreen = target) }
        syncRotationWorker()
    }

    fun setOnlyFavoritesForRotation(onlyFavs: Boolean) {
        prefs.edit().putBoolean(KEY_ONLY_FAVORITES, onlyFavs).apply()
        _uiState.update { it.copy(onlyFavoritesForRotation = onlyFavs) }
        syncRotationWorker()
    }

    fun setDefaultOrientation(orientation: OrientationFilter) {
        prefs.edit().putString(KEY_ORIENTATION, orientation.name).apply()
        _uiState.update { it.copy(defaultOrientation = orientation) }
    }

    fun setAllowNsfw(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_NSFW, allow).apply()
        _uiState.update { it.copy(allowNsfw = allow) }
    }

    fun clearBrowserSession() {
        sessionManager.clearSession()
        _uiState.update { it.copy(hasActiveBrowserSession = false) }
    }

    private fun syncRotationWorker() {
        val state = _uiState.value
        if (state.autoRotationEnabled) {
            rotationScheduler.scheduleRotation(
                intervalHours = state.rotationIntervalHours,
                targetScreen = state.targetScreen,
                onlyFavorites = state.onlyFavoritesForRotation
            )
        } else {
            rotationScheduler.cancelRotation()
        }
    }
}

class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(context) as T
    }
}
