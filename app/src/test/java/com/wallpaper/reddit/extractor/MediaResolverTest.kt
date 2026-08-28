package com.wallpaper.reddit.extractor

import com.wallpaper.reddit.data.extractor.MediaResolver
import com.wallpaper.reddit.data.model.OrientationFilter
import com.wallpaper.reddit.data.model.RedditPost
import org.junit.Assert.*
import org.junit.Test

class MediaResolverTest {

    private val resolver = MediaResolver()

    @Test
    fun testOrientationFiltering() {
        val portraitPost = RedditPost(
            id = "p1",
            name = "t3_p1",
            subreddit = "wallpapers",
            title = "Phone Wallpaper",
            author = "author1",
            permalink = "",
            postUrl = "",
            mediaUrl = "https://i.redd.it/p1.jpg",
            thumbnailUrl = null,
            width = 1080,
            height = 2400,
            isNsfw = false
        )

        val landscapePost = RedditPost(
            id = "p2",
            name = "t3_p2",
            subreddit = "wallpapers",
            title = "Desktop Wallpaper",
            author = "author2",
            permalink = "",
            postUrl = "",
            mediaUrl = "https://i.redd.it/p2.jpg",
            thumbnailUrl = null,
            width = 3840,
            height = 2160,
            isNsfw = false
        )

        // Test PORTRAIT filter
        assertTrue(resolver.matchesFilter(portraitPost, OrientationFilter.PORTRAIT, allowNsfw = false))
        assertFalse(resolver.matchesFilter(landscapePost, OrientationFilter.PORTRAIT, allowNsfw = false))

        // Test LANDSCAPE filter
        assertFalse(resolver.matchesFilter(portraitPost, OrientationFilter.LANDSCAPE, allowNsfw = false))
        assertTrue(resolver.matchesFilter(landscapePost, OrientationFilter.LANDSCAPE, allowNsfw = false))

        // Test ALL filter
        assertTrue(resolver.matchesFilter(portraitPost, OrientationFilter.ALL, allowNsfw = false))
        assertTrue(resolver.matchesFilter(landscapePost, OrientationFilter.ALL, allowNsfw = false))
    }

    @Test
    fun testNsfwFiltering() {
        val nsfwPost = RedditPost(
            id = "p3",
            name = "t3_p3",
            subreddit = "wallpapers",
            title = "NSFW Art",
            author = "author3",
            permalink = "",
            postUrl = "",
            mediaUrl = "https://i.redd.it/p3.jpg",
            thumbnailUrl = null,
            width = 1920,
            height = 1080,
            isNsfw = true
        )

        assertFalse(resolver.matchesFilter(nsfwPost, OrientationFilter.ALL, allowNsfw = false))
        assertTrue(resolver.matchesFilter(nsfwPost, OrientationFilter.ALL, allowNsfw = true))
    }

    @Test
    fun testResolutionResolution() {
        val post = RedditPost(
            id = "p4",
            name = "t3_p4",
            subreddit = "wallpapers",
            title = "High Res Wallpaper",
            author = "author4",
            permalink = "",
            postUrl = "",
            mediaUrl = "https://i.redd.it/p4.jpg",
            thumbnailUrl = "https://preview.redd.it/p4_thumb.jpg",
            width = 3840,
            height = 2160,
            isNsfw = false
        )

        val resolved = resolver.resolve(post)
        assertNotNull(resolved)
        assertEquals("https://i.redd.it/p4.jpg", resolved!!.directImageUrl)
        assertEquals(3840, resolved.width)
        assertEquals(2160, resolved.height)
        assertFalse(resolved.isPortrait!!)
        assertTrue(resolved.isHighRes)
    }
}
