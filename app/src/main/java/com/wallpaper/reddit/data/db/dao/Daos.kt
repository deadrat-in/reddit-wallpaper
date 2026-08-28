package com.wallpaper.reddit.data.db.dao

import androidx.room.*
import com.wallpaper.reddit.data.db.entities.CachedPostEntity
import com.wallpaper.reddit.data.db.entities.SubredditEntity
import com.wallpaper.reddit.data.db.entities.WallpaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubredditDao {
    @Query("SELECT * FROM subreddits ORDER BY addedAt ASC")
    fun getAllSubreddits(): Flow<List<SubredditEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubreddit(subreddit: SubredditEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubreddits(subreddits: List<SubredditEntity>)

    @Delete
    suspend fun deleteSubreddit(subreddit: SubredditEntity)

    @Query("DELETE FROM subreddits WHERE name = :name")
    suspend fun deleteByName(name: String)
}

@Dao
interface CachedPostDao {
    @Query("SELECT * FROM cached_posts WHERE subreddit = :subreddit ORDER BY fetchedAt DESC")
    fun getCachedPosts(subreddit: String): Flow<List<CachedPostEntity>>

    @Query("SELECT * FROM cached_posts WHERE subreddit = :subreddit ORDER BY fetchedAt DESC")
    suspend fun getCachedPostsList(subreddit: String): List<CachedPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<CachedPostEntity>)

    @Query("DELETE FROM cached_posts WHERE subreddit = :subreddit")
    suspend fun clearSubreddit(subreddit: String)

    @Query("DELETE FROM cached_posts WHERE fetchedAt < :olderThanTimestamp")
    suspend fun cleanOldPosts(olderThanTimestamp: Long)
}

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpapers ORDER BY downloadedAt DESC")
    fun getAllWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY downloadedAt DESC")
    fun getFavoriteWallpapers(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE localFilePath IS NOT NULL")
    suspend fun getDownloadedWallpapersList(): List<WallpaperEntity>

    @Query("SELECT * FROM wallpapers WHERE id = :id LIMIT 1")
    suspend fun getWallpaperById(id: String): WallpaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(wallpaper: WallpaperEntity)

    @Query("UPDATE wallpapers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE wallpapers SET lastSetAsWallpaperAt = :timestamp WHERE id = :id")
    suspend fun updateLastSetTimestamp(id: String, timestamp: Long)

    @Delete
    suspend fun deleteWallpaper(wallpaper: WallpaperEntity)
}
