package com.wallpaper.reddit.worker

import android.content.Context
import androidx.work.*
import com.wallpaper.reddit.data.model.TargetScreen
import java.util.concurrent.TimeUnit

class RotationScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleRotation(
        intervalHours: Long = 3L,
        targetScreen: TargetScreen = TargetScreen.BOTH,
        onlyFavorites: Boolean = false
    ) {
        val inputData = workDataOf(
            WallpaperRotationWorker.KEY_TARGET_SCREEN to targetScreen.name,
            WallpaperRotationWorker.KEY_ONLY_FAVORITES to onlyFavorites
        )

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<WallpaperRotationWorker>(
            intervalHours.coerceAtLeast(1L),
            TimeUnit.HOURS
        )
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WallpaperRotationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun cancelRotation() {
        workManager.cancelUniqueWork(WallpaperRotationWorker.WORK_NAME)
    }
}
