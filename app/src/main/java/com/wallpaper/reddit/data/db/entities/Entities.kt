package com.wallpaper.reddit.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subreddits")
data class SubredditEntity(
    @PrimaryKey val name: String, // e.g. "wallpapers"
    val displayName: String,      // e.g. "r/wallpapers"
    val isDefault: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_posts")
data class CachedPostEntity(
    @PrimaryKey val id: String,
    val subreddit: String,
    val title: String,
    val author: String,
    val permalink: String,
    val postUrl: String,
    val mediaUrl: String,
    val thumbnailUrl: String?,
    val width: Int?,
    val height: Int?,
    val isNsfw: Boolean,
    val score: Int,
    val createdUtc: Long,
    val fetchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey val id: String, // Reddit post ID or UUID
    val subreddit: String,
    val title: String,
    val postUrl: String,
    val mediaUrl: String,
    val localFilePath: String?, // Internal or external storage path
    val width: Int?,
    val height: Int?,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadedAt: Long? = null,
    val lastSetAsWallpaperAt: Long? = null
)
