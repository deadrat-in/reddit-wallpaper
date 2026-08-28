package com.wallpaper.reddit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RedditPost(
    val id: String,
    val name: String, // e.g. "t3_xxxx"
    val subreddit: String,
    val title: String,
    val author: String,
    val permalink: String,
    val postUrl: String,
    val mediaUrl: String?,
    val thumbnailUrl: String?,
    val width: Int? = null,
    val height: Int? = null,
    val isNsfw: Boolean = false,
    val isGallery: Boolean = false,
    val galleryImages: List<GalleryImage> = emptyList(),
    val score: Int = 0,
    val createdUtc: Long = 0L,
    val fetchedAt: Long = System.currentTimeMillis()
)

@Serializable
data class GalleryImage(
    val id: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null
)

enum class RedditSort(val apiValue: String, val displayName: String) {
    HOT("hot", "Hot"),
    NEW("new", "New"),
    TOP_DAY("top", "Top Today"),
    TOP_WEEK("top", "Top Week"),
    TOP_MONTH("top", "Top Month"),
    TOP_YEAR("top", "Top Year"),
    TOP_ALL("top", "Top All-Time")
}

enum class OrientationFilter(val displayName: String) {
    ALL("All Orientations"),
    PORTRAIT("Portrait Only"),
    LANDSCAPE("Landscape Only")
}

enum class TargetScreen(val displayName: String) {
    BOTH("Home & Lock Screen"),
    HOME("Home Screen Only"),
    LOCK("Lock Screen Only")
}
