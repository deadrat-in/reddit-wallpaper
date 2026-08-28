package com.wallpaper.reddit.data.extractor

import com.wallpaper.reddit.data.model.RedditPost
import com.wallpaper.reddit.data.model.RedditSort

sealed class ExtractionResult {
    data class Success(val posts: List<RedditPost>, val after: String? = null) : ExtractionResult()
    data class RateLimited(val retryAfterSeconds: Int? = null, val message: String = "Reddit rate limit reached") : ExtractionResult()
    data class Error(val message: String, val throwable: Throwable? = null) : ExtractionResult()
}

interface RedditSource {
    suspend fun getPosts(
        subreddit: String,
        sort: RedditSort = RedditSort.HOT,
        limit: Int = 25,
        after: String? = null
    ): ExtractionResult
}
