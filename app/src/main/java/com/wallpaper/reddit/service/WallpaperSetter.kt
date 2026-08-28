package com.wallpaper.reddit.service

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import com.wallpaper.reddit.data.model.TargetScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class WallpaperSetter(private val context: Context) {

    companion object {
        private const val TAG = "WallpaperSetter"
    }

    private val wallpaperManager = WallpaperManager.getInstance(context)

    suspend fun setWallpaperFromFile(file: File, target: TargetScreen): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "File does not exist or is empty: ${file.absolutePath}")
            return@withContext false
        }

        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
            setWallpaperBitmap(bitmap, target)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting wallpaper from file: ${e.message}", e)
            false
        }
    }

    suspend fun setWallpaperBitmap(bitmap: Bitmap, target: TargetScreen): Boolean = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val whichFlag = when (target) {
                    TargetScreen.HOME -> WallpaperManager.FLAG_SYSTEM
                    TargetScreen.LOCK -> WallpaperManager.FLAG_LOCK
                    TargetScreen.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wallpaperManager.setBitmap(bitmap, null, true, whichFlag)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            Log.d(TAG, "Successfully applied wallpaper to $target")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed setting wallpaper bitmap: ${e.message}", e)
            false
        }
    }
}
