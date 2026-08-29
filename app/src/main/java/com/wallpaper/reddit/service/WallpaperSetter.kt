package com.wallpaper.reddit.service

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.wallpaper.reddit.data.model.TargetScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
            val processedBitmap = resizeAndCenterForScreen(bitmap)
            setWallpaperBitmap(processedBitmap, target)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting wallpaper from file: ${e.message}", e)
            false
        }
    }

    suspend fun setWallpaperBitmap(bitmap: Bitmap, target: TargetScreen): Boolean = withContext(Dispatchers.IO) {
        try {
            val (width, height) = getScreenMetrics()
            wallpaperManager.suggestDesiredDimensions(width, height)

            if (!wallpaperManager.isWallpaperSupported) {
                Log.w(TAG, "Device reports wallpaper is not supported.")
                return@withContext false
            }

            val processedBitmap = resizeAndCenterForScreen(bitmap)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (target == TargetScreen.BOTH || target == TargetScreen.HOME) {
                    wallpaperManager.setBitmap(processedBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                }
                if (target == TargetScreen.BOTH || target == TargetScreen.LOCK) {
                    wallpaperManager.setBitmap(processedBitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
            } else {
                wallpaperManager.setBitmap(processedBitmap)
            }
            Log.d(TAG, "Successfully applied wallpaper to $target (Dimensions: ${processedBitmap.width}x${processedBitmap.height})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed setting wallpaper bitmap: ${e.message}", e)
            false
        }
    }

    /**
     * Reusable scaling algorithm (adapted from WallYou WallpaperHelper):
     * Scales and centers the wallpaper to match the device's exact aspect ratio without distortion.
     */
    private fun resizeAndCenterForScreen(bitmap: Bitmap): Bitmap {
        val (screenWidth, screenHeight) = getScreenMetrics()
        val bitmapRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val screenRatio = screenHeight.toFloat() / screenWidth.toFloat()
        val scaleRatio = (bitmapRatio / screenRatio)

        var newWidth = bitmap.width
        var newHeight = bitmap.height

        if (bitmapRatio > screenRatio) {
            newHeight = (bitmap.height / scaleRatio).toInt()
        } else {
            newWidth = (scaleRatio * bitmap.width).toInt()
        }

        val gapX = ((bitmap.width - newWidth) / 2).coerceAtLeast(0)
        val gapY = ((bitmap.height - newHeight) / 2).coerceAtLeast(0)
        val safeWidth = newWidth.coerceAtMost(bitmap.width - gapX)
        val safeHeight = newHeight.coerceAtMost(bitmap.height - gapY)

        val centeredBitmap = Bitmap.createBitmap(bitmap, gapX, gapY, safeWidth, safeHeight)
        return Bitmap.createScaledBitmap(centeredBitmap, screenWidth, screenHeight, true)
    }

    private fun getScreenMetrics(): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }
}
