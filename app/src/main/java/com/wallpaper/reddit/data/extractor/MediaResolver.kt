package com.wallpaper.reddit.data.extractor

import com.wallpaper.reddit.data.model.OrientationFilter
import com.wallpaper.reddit.data.model.RedditPost

data class ResolvedMedia(
    val directImageUrl: String,
    val displayThumbnailUrl: String,
    val width: Int?,
    val height: Int?,
    val isPortrait: Boolean?,
    val isHighRes: Boolean
)

class MediaResolver(
    private val defaultMinWidth: Int = 1080,
    private val defaultMinHeight: Int = 1920
) {

    fun resolve(post: RedditPost): ResolvedMedia? {
        val mediaUrl = post.mediaUrl ?: post.galleryImages.firstOrNull()?.url ?: return null
        val w = post.width ?: post.galleryImages.firstOrNull()?.width
        val h = post.height ?: post.galleryImages.firstOrNull()?.height

        val isPortrait = if (w != null && h != null) h >= w else null
        val isHighRes = if (w != null && h != null) (w >= 1080 && h >= 1080) else true

        val thumb = post.thumbnailUrl ?: mediaUrl

        return ResolvedMedia(
            directImageUrl = mediaUrl,
            displayThumbnailUrl = thumb,
            width = w,
            height = h,
            isPortrait = isPortrait,
            isHighRes = isHighRes
        )
    }

    fun matchesFilter(
        post: RedditPost,
        orientationFilter: OrientationFilter,
        allowNsfw: Boolean,
        minWidth: Int = defaultMinWidth,
        minHeight: Int = defaultMinHeight
    ): Boolean {
        if (post.isNsfw && !allowNsfw) {
            return false
        }

        val w = post.width
        val h = post.height

        // If dimensions are unknown, allow it for preview
        if (w == null || h == null) {
            return true
        }

        // Check orientation
        when (orientationFilter) {
            OrientationFilter.PORTRAIT -> {
                if (h < w) return false
            }
            OrientationFilter.LANDSCAPE -> {
                if (w < h) return false
            }
            OrientationFilter.ALL -> {}
        }

        // Check dimension threshold if specified
        val maxDim = maxOf(w, h)
        val minDim = minOf(w, h)
        val requiredMax = maxOf(minWidth, minHeight)
        val requiredMin = minOf(minWidth, minHeight)

        if (maxDim < requiredMax * 0.7 || minDim < requiredMin * 0.7) {
            // Tolerant scaling check (allow 70% of min required to avoid over-filtering)
            return false
        }

        return true
    }
}
