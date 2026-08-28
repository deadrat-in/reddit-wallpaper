package com.wallpaper.reddit.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wallpaper.reddit.data.db.AppDatabase
import com.wallpaper.reddit.data.model.TargetScreen
import com.wallpaper.reddit.service.WallpaperSetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WallpaperRotationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "WallpaperRotationWorker"
        const val WORK_NAME = "periodic_wallpaper_rotation"
        const val KEY_TARGET_SCREEN = "key_target_screen"
        const val KEY_ONLY_FAVORITES = "key_only_favorites"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting automatic wallpaper rotation work...")

        val db = AppDatabase.getInstance(applicationContext)
        val wallpaperDao = db.wallpaperDao()
        val setter = WallpaperSetter(applicationContext)

        val targetScreenName = inputData.getString(KEY_TARGET_SCREEN) ?: TargetScreen.BOTH.name
        val targetScreen = try {
            TargetScreen.valueOf(targetScreenName)
        } catch (e: Exception) {
            TargetScreen.BOTH
        }
        val onlyFavorites = inputData.getBoolean(KEY_ONLY_FAVORITES, false)

        val availableWallpapers = wallpaperDao.getDownloadedWallpapersList().filter { wp ->
            val fileExists = wp.localFilePath != null && File(wp.localFilePath).exists()
            val matchesFav = if (onlyFavorites) wp.isFavorite else true
            fileExists && matchesFav
        }

        if (availableWallpapers.isEmpty()) {
            Log.w(TAG, "No cached wallpapers available for rotation. Skipping rotation.")
            return@withContext Result.success()
        }

        // Sort by least recently set to ensure good variety
        val candidate = availableWallpapers.sortedBy { it.lastSetAsWallpaperAt ?: 0L }.first()
        val file = File(candidate.localFilePath!!)

        Log.d(TAG, "Applying rotated wallpaper: ${candidate.title} from ${candidate.subreddit}")
        val success = setter.setWallpaperFromFile(file, targetScreen)

        if (success) {
            wallpaperDao.updateLastSetTimestamp(candidate.id, System.currentTimeMillis())
            Log.d(TAG, "Wallpaper successfully rotated!")
            Result.success()
        } else {
            Log.e(TAG, "Failed to apply rotated wallpaper file.")
            Result.retry()
        }
    }
}
