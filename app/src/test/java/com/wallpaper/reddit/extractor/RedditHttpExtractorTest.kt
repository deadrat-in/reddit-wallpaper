package com.wallpaper.reddit.extractor

import com.wallpaper.reddit.data.extractor.ExtractionResult
import com.wallpaper.reddit.data.extractor.RedditHttpExtractor
import org.junit.Assert.*
import org.junit.Test

class RedditHttpExtractorTest {

    private val extractor = RedditHttpExtractor()

    @Test
    fun testParseRedditJsonSingleImage() {
        val sampleJson = """
        {
            "kind": "Listing",
            "data": {
                "after": "t3_sample_after",
                "children": [
                    {
                        "kind": "t3",
                        "data": {
                            "id": "post123",
                            "name": "t3_post123",
                            "subreddit": "wallpapers",
                            "title": "Neon Cyberpunk City [3840x2160]",
                            "author": "pixel_artist",
                            "permalink": "/r/wallpapers/comments/post123/neon_cyberpunk_city/",
                            "url": "https://i.redd.it/abc123xyz.jpg",
                            "score": 450,
                            "over_18": false,
                            "created_utc": 1700000000,
                            "preview": {
                                "images": [
                                    {
                                        "source": {
                                            "url": "https://preview.redd.it/abc123xyz.jpg?width=3840&amp;crop=smart&amp;auto=webp",
                                            "width": 3840,
                                            "height": 2160
                                        }
                                    }
                                ]
                            }
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val result = extractor.parseRedditJson(sampleJson, "wallpapers")
        assertTrue(result is ExtractionResult.Success)
        val success = result as ExtractionResult.Success
        assertEquals(1, success.posts.size)
        assertEquals("t3_sample_after", success.after)

        val post = success.posts[0]
        assertEquals("post123", post.id)
        assertEquals("Neon Cyberpunk City [3840x2160]", post.title)
        assertEquals("https://i.redd.it/abc123xyz.jpg", post.mediaUrl)
        assertEquals(3840, post.width)
        assertEquals(2160, post.height)
        assertFalse(post.isNsfw)
    }

    @Test
    fun testParseRedditJsonGallery() {
        val sampleGalleryJson = """
        {
            "kind": "Listing",
            "data": {
                "children": [
                    {
                        "kind": "t3",
                        "data": {
                            "id": "gallery99",
                            "name": "t3_gallery99",
                            "subreddit": "wallpapers",
                            "title": "Minimal Mountain Collection",
                            "author": "nature_lover",
                            "permalink": "/r/wallpapers/comments/gallery99/collection/",
                            "url": "https://www.reddit.com/gallery/gallery99",
                            "is_gallery": true,
                            "media_metadata": {
                                "item_1": {
                                    "status": "valid",
                                    "s": {
                                        "u": "https://preview.redd.it/item1.jpg?width=1920&amp;format=pjpg&amp;auto=webp&amp;s=abc",
                                        "x": 1920,
                                        "y": 1080
                                    }
                                },
                                "item_2": {
                                    "status": "valid",
                                    "s": {
                                        "u": "https://preview.redd.it/item2.jpg?width=2560&amp;format=pjpg&amp;auto=webp&amp;s=def",
                                        "x": 2560,
                                        "y": 1440
                                    }
                                }
                            }
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val result = extractor.parseRedditJson(sampleGalleryJson, "wallpapers")
        assertTrue(result is ExtractionResult.Success)
        val success = result as ExtractionResult.Success
        assertEquals(1, success.posts.size)

        val post = success.posts[0]
        assertTrue(post.isGallery)
        assertEquals(2, post.galleryImages.size)
        assertTrue(post.galleryImages[0].url.contains("&format=pjpg"))
        assertFalse(post.galleryImages[0].url.contains("&amp;")) // Verifies unescaping
    }

    @Test
    fun testParseRssXml() {
        val sampleRss = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
            <entry>
                <title>Serene Forest [2160x3840]</title>
                <link href="https://www.reddit.com/r/EarthPorn/comments/forest1/" />
                <id>https://www.reddit.com/r/EarthPorn/comments/forest1/</id>
                <content type="html">&lt;img src="https://i.redd.it/forest.jpg" alt="test" /&gt;</content>
            </entry>
        </feed>
        """.trimIndent()

        val result = extractor.parseRssXml(sampleRss, "EarthPorn")
        assertTrue(result is ExtractionResult.Success)
        val success = result as ExtractionResult.Success
        assertEquals(1, success.posts.size)
        assertEquals("Serene Forest [2160x3840]", success.posts[0].title)
        assertEquals("https://i.redd.it/forest.jpg", success.posts[0].mediaUrl)
    }
}
