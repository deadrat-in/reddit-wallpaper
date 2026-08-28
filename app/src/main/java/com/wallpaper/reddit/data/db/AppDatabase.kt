package com.wallpaper.reddit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wallpaper.reddit.data.db.dao.CachedPostDao
import com.wallpaper.reddit.data.db.dao.SubredditDao
import com.wallpaper.reddit.data.db.dao.WallpaperDao
import com.wallpaper.reddit.data.db.entities.CachedPostEntity
import com.wallpaper.reddit.data.db.entities.SubredditEntity
import com.wallpaper.reddit.data.db.entities.WallpaperEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubredditEntity::class,
        CachedPostEntity::class,
        WallpaperEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subredditDao(): SubredditDao
    abstract fun cachedPostDao(): CachedPostDao
    abstract fun wallpaperDao(): WallpaperDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val DEFAULT_SUBREDDITS = listOf(
            SubredditEntity(name = "wallpapers", displayName = "r/wallpapers", isDefault = true),
            SubredditEntity(name = "EarthPorn", displayName = "r/EarthPorn", isDefault = true),
            SubredditEntity(name = "Animewallpaper", displayName = "r/Animewallpaper", isDefault = true),
            SubredditEntity(name = "MinimalWallpaper", displayName = "r/MinimalWallpaper", isDefault = true),
            SubredditEntity(name = "spaceporn", displayName = "r/spaceporn", isDefault = true)
        )

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reddit_wallpapers.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default subreddits on database creation
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).subredditDao().insertSubreddits(DEFAULT_SUBREDDITS)
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
