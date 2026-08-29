package com.wallpaper.reddit.data.repository

import android.content.Context
import android.util.Log
import com.wallpaper.reddit.data.db.AppDatabase
import com.wallpaper.reddit.data.db.entities.CachedPostEntity
import com.wallpaper.reddit.data.db.entities.SubredditEntity
import com.wallpaper.reddit.data.db.entities.WallpaperEntity
import com.wallpaper.reddit.data.extractor.ExtractionResult
import com.wallpaper.reddit.data.extractor.MediaResolver
import com.wallpaper.reddit.data.extractor.RedditHttpExtractor
import com.wallpaper.reddit.data.extractor.RedditSessionManager
import com.wallpaper.reddit.data.extractor.RedditSource
import com.wallpaper.reddit.data.model.OrientationFilter
import com.wallpaper.reddit.data.model.RedditPost
import com.wallpaper.reddit.data.model.RedditSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class WallpaperRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    val sessionManager: RedditSessionManager = RedditSessionManager.getInstance(context),
    private val redditSource: RedditSource = RedditHttpExtractor(sessionManager),
    private val mediaResolver: MediaResolver = MediaResolver(),
    private val httpClient: OkHttpClient = RedditHttpExtractor.defaultClient(sessionManager)
) {

    companion object {
        private const val TAG = "WallpaperRepo"
    }

    private val subredditDao = database.subredditDao()
    private val cachedPostDao = database.cachedPostDao()
    private val wallpaperDao = database.wallpaperDao()

    val subreddits: Flow<List<SubredditEntity>> = subredditDao.getAllSubreddits()
    val favorites: Flow<List<WallpaperEntity>> = wallpaperDao.getFavoriteWallpapers()
    val allSavedWallpapers: Flow<List<WallpaperEntity>> = wallpaperDao.getAllWallpapers()

    suspend fun addSubreddit(name: String) = withContext(Dispatchers.IO) {
        val cleanName = name.trim().removePrefix("r/").removePrefix("/")
        if (cleanName.isNotBlank()) {
            subredditDao.insertSubreddit(
                SubredditEntity(
                    name = cleanName,
                    displayName = "r/$cleanName"
                )
            )
        }
    }

    suspend fun removeSubreddit(name: String) = withContext(Dispatchers.IO) {
        subredditDao.deleteByName(name)
        cachedPostDao.clearSubreddit(name)
    }

    suspend fun fetchPosts(
        subreddit: String,
        sort: RedditSort = RedditSort.HOT,
        after: String? = null,
        orientationFilter: OrientationFilter = OrientationFilter.ALL,
        allowNsfw: Boolean = false,
        minWidth: Int = 1080,
        minHeight: Int = 1920
    ): ExtractionResult = withContext(Dispatchers.IO) {
        val cleanSub = subreddit.trim().removePrefix("r/").removePrefix("/")
        val result = redditSource.getPosts(cleanSub, sort, limit = 25, after = after)

        if (result is ExtractionResult.Success) {
            val filteredPosts = result.posts.filter { post ->
                mediaResolver.matchesFilter(
                    post = post,
                    orientationFilter = orientationFilter,
                    allowNsfw = allowNsfw,
                    minWidth = minWidth,
                    minHeight = minHeight
                )
            }

            // Cache posts to database for offline use
            val entities = filteredPosts.map { post ->
                CachedPostEntity(
                    id = post.id,
                    subreddit = post.subreddit,
                    title = post.title,
                    author = post.author,
                    permalink = post.permalink,
                    postUrl = post.postUrl,
                    mediaUrl = post.mediaUrl ?: post.galleryImages.firstOrNull()?.url ?: "",
                    thumbnailUrl = post.thumbnailUrl,
                    width = post.width,
                    height = post.height,
                    isNsfw = post.isNsfw,
                    score = post.score,
                    createdUtc = post.createdUtc
                )
            }
            if (entities.isNotEmpty()) {
                cachedPostDao.insertPosts(entities)
            }

            return@withContext ExtractionResult.Success(
                posts = filteredPosts,
                after = result.after
            )
        }

        result
    }

    fun getCachedPosts(subreddit: String): Flow<List<RedditPost>> {
        val cleanSub = subreddit.trim().removePrefix("r/").removePrefix("/")
        return cachedPostDao.getCachedPosts(cleanSub).map { entities ->
            entities.map { entity ->
                RedditPost(
                    id = entity.id,
                    name = "t3_${entity.id}",
                    subreddit = entity.subreddit,
                    title = entity.title,
                    author = entity.author,
                    permalink = entity.permalink,
                    postUrl = entity.postUrl,
                    mediaUrl = entity.mediaUrl,
                    thumbnailUrl = entity.thumbnailUrl,
                    width = entity.width,
                    height = entity.height,
                    isNsfw = entity.isNsfw,
                    score = entity.score,
                    createdUtc = entity.createdUtc,
                    fetchedAt = entity.fetchedAt
                )
            }
        }
    }

    suspend fun downloadWallpaperFile(post: RedditPost): File? = withContext(Dispatchers.IO) {
        val mediaUrl = post.mediaUrl ?: post.galleryImages.firstOrNull()?.url ?: return@withContext null
        val wallpaperDir = File(context.filesDir, "wallpapers").apply { mkdirs() }
        val extension = when {
            mediaUrl.contains(".png", ignoreCase = true) -> "png"
            mediaUrl.contains(".webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
        val targetFile = File(wallpaperDir, "${post.id}.$extension")

        // Return immediately if already cached on disk
        if (targetFile.exists() && targetFile.length() > 0) {
            Log.d(TAG, "Wallpaper already cached on disk: ${targetFile.absolutePath}")
            return@withContext targetFile
        }

        val request = Request.Builder()
            .url(mediaUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed downloading image: HTTP ${response.code}")
                    return@withContext null
                }
                val body = response.body ?: return@withContext null
                FileOutputStream(targetFile).use { output ->
                    body.byteStream().copyTo(output)
                }
            }

            // Save to database
            val entity = WallpaperEntity(
                id = post.id,
                subreddit = post.subreddit,
                title = post.title,
                postUrl = post.postUrl,
                mediaUrl = mediaUrl,
                localFilePath = targetFile.absolutePath,
                width = post.width,
                height = post.height,
                isDownloaded = true,
                downloadedAt = System.currentTimeMillis()
            )
            wallpaperDao.insertOrUpdate(entity)

            Log.d(TAG, "Successfully downloaded wallpaper to: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            targetFile
        } catch (e: IOException) {
            Log.e(TAG, "Error downloading wallpaper: ${e.message}", e)
            null
        }
    }

    suspend fun toggleFavorite(post: RedditPost, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val existing = wallpaperDao.getWallpaperById(post.id)
        if (existing != null) {
            wallpaperDao.updateFavorite(post.id, isFavorite)
        } else {
            val entity = WallpaperEntity(
                id = post.id,
                subreddit = post.subreddit,
                title = post.title,
                postUrl = post.postUrl,
                mediaUrl = post.mediaUrl ?: "",
                localFilePath = null,
                width = post.width,
                height = post.height,
                isFavorite = isFavorite,
                isDownloaded = false
            )
            wallpaperDao.insertOrUpdate(entity)
        }
    }

    suspend fun getWallpaperEntity(id: String): WallpaperEntity? = withContext(Dispatchers.IO) {
        wallpaperDao.getWallpaperById(id)
    }

    suspend fun markWallpaperApplied(id: String) = withContext(Dispatchers.IO) {
        wallpaperDao.updateLastSetTimestamp(id, System.currentTimeMillis())
    }
}
