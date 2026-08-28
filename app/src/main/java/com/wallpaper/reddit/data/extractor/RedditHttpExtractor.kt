package com.wallpaper.reddit.data.extractor

import android.util.Log
import com.wallpaper.reddit.data.model.GalleryImage
import com.wallpaper.reddit.data.model.RedditPost
import com.wallpaper.reddit.data.model.RedditSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class RedditHttpExtractor(
    private val client: OkHttpClient = defaultClient(),
    private val userAgent: String = "android:com.wallpaper.reddit:v1.0.0 (by /u/reddit_wallpaper_app)"
) : RedditSource {

    companion object {
        private const val TAG = "RedditExtractor"
        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }

        private fun logDebug(tag: String, msg: String) {
            try { Log.d(tag, msg) } catch (_: Throwable) { println("[$tag] $msg") }
        }
        private fun logWarn(tag: String, msg: String) {
            try { Log.w(tag, msg) } catch (_: Throwable) { System.err.println("[$tag WARN] $msg") }
        }
        private fun logError(tag: String, msg: String, tr: Throwable? = null) {
            try { Log.e(tag, msg, tr) } catch (_: Throwable) { System.err.println("[$tag ERROR] $msg ${tr?.message ?: ""}") }
        }
    }

    override suspend fun getPosts(
        subreddit: String,
        sort: RedditSort,
        limit: Int,
        after: String?
    ): ExtractionResult = withContext(Dispatchers.IO) {
        val cleanSubreddit = subreddit.trim().removePrefix("r/").removePrefix("/")
        val url = buildUrl(cleanSubreddit, sort, limit, after)

        logDebug(TAG, "[Reddit] GET $url")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json, text/html, */*")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                logDebug(TAG, "[Reddit] HTTP $code from $url")

                if (code == 429) {
                    val retryAfter = response.header("Retry-After")?.toIntOrNull()
                    logWarn(TAG, "[Reddit] Rate limited (429). Retry-After: $retryAfter")
                    return@withContext ExtractionResult.RateLimited(
                        retryAfterSeconds = retryAfter,
                        message = "Reddit rate limit reached (HTTP 429). Please wait before refreshing."
                    )
                }

                if (code == 403) {
                    logWarn(TAG, "[Reddit] Access blocked (403).")
                    return@withContext tryRssFallback(cleanSubreddit)
                }

                if (!response.isSuccessful) {
                    return@withContext ExtractionResult.Error("HTTP error $code: ${response.message}")
                }

                val bodyString = response.body?.string() ?: return@withContext ExtractionResult.Error("Empty response body")
                parseRedditJson(bodyString, cleanSubreddit)
            }
        } catch (e: IOException) {
            logError(TAG, "[Reddit] Network failure: ${e.message}", e)
            ExtractionResult.Error("Network error: ${e.localizedMessage ?: e.message}", e)
        } catch (e: Exception) {
            logError(TAG, "[Reddit] Parsing failure: ${e.message}", e)
            ExtractionResult.Error("Failed to parse Reddit response: ${e.localizedMessage ?: e.message}", e)
        }
    }

    private fun buildUrl(subreddit: String, sort: RedditSort, limit: Int, after: String?): String {
        val sortPath = when (sort) {
            RedditSort.HOT -> "hot"
            RedditSort.NEW -> "new"
            RedditSort.TOP_DAY -> "top"
            RedditSort.TOP_WEEK -> "top"
            RedditSort.TOP_MONTH -> "top"
            RedditSort.TOP_YEAR -> "top"
            RedditSort.TOP_ALL -> "top"
        }
        val timeParam = when (sort) {
            RedditSort.TOP_DAY -> "&t=day"
            RedditSort.TOP_WEEK -> "&t=week"
            RedditSort.TOP_MONTH -> "&t=month"
            RedditSort.TOP_YEAR -> "&t=year"
            RedditSort.TOP_ALL -> "&t=all"
            else -> ""
        }
        val afterParam = if (!after.isNullOrBlank()) "&after=$after" else ""
        return "https://www.reddit.com/r/$subreddit/$sortPath.json?limit=$limit$timeParam$afterParam"
    }

    fun parseRedditJson(jsonString: String, subreddit: String): ExtractionResult {
        return try {
            val root = jsonParser.parseToJsonElement(jsonString).jsonObject
            val data = root["data"]?.jsonObject ?: return ExtractionResult.Error("Missing 'data' in JSON response")
            val children = data["children"]?.jsonArray ?: return ExtractionResult.Error("Missing 'children' array")
            val after = data["after"]?.jsonPrimitive?.contentOrNull

            val posts = mutableListOf<RedditPost>()

            for (item in children) {
                val postData = item.jsonObject["data"]?.jsonObject ?: continue
                val post = parsePostData(postData, subreddit)
                if (post != null) {
                    posts.add(post)
                }
            }

            logDebug(TAG, "[Reddit] Parsed ${children.size} posts, extracted ${posts.size} valid wallpaper items")
            ExtractionResult.Success(posts = posts, after = after)
        } catch (e: Exception) {
            ExtractionResult.Error("JSON parsing exception: ${e.message}", e)
        }
    }

    private fun parsePostData(data: JsonObject, defaultSubreddit: String): RedditPost? {
        val id = data["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val name = data["name"]?.jsonPrimitive?.contentOrNull ?: "t3_$id"
        val title = data["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled"
        val author = data["author"]?.jsonPrimitive?.contentOrNull ?: "[deleted]"
        val sub = data["subreddit"]?.jsonPrimitive?.contentOrNull ?: defaultSubreddit
        val permalink = data["permalink"]?.jsonPrimitive?.contentOrNull ?: "/r/$sub/comments/$id/"
        val fullPostUrl = "https://www.reddit.com$permalink"
        val url = data["url"]?.jsonPrimitive?.contentOrNull ?: ""
        val thumbnail = data["thumbnail"]?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("http") }
        val isNsfw = data["over_18"]?.jsonPrimitive?.booleanOrNull ?: false
        val score = data["score"]?.jsonPrimitive?.intOrNull ?: 0
        val createdUtc = data["created_utc"]?.jsonPrimitive?.longOrNull ?: 0L
        val isGallery = data["is_gallery"]?.jsonPrimitive?.booleanOrNull ?: false

        var mediaUrl: String? = null
        var width: Int? = null
        var height: Int? = null
        val galleryImages = mutableListOf<GalleryImage>()

        // 1. Check if gallery
        if (isGallery && data.containsKey("media_metadata")) {
            val mediaMetadata = data["media_metadata"]?.jsonObject
            mediaMetadata?.forEach { (key, elem) ->
                val obj = elem.jsonObject
                val status = obj["status"]?.jsonPrimitive?.contentOrNull
                if (status == "valid" || status == null) {
                    val s = obj["s"]?.jsonObject
                    val rawUrl = s?.get("u")?.jsonPrimitive?.contentOrNull
                        ?: s?.get("gif")?.jsonPrimitive?.contentOrNull
                    if (rawUrl != null) {
                        val unescapedUrl = unescapeUrl(rawUrl)
                        val w = s?.get("x")?.jsonPrimitive?.intOrNull
                        val h = s?.get("y")?.jsonPrimitive?.intOrNull
                        galleryImages.add(GalleryImage(id = key, url = unescapedUrl, width = w, height = h))
                    }
                }
            }
            if (galleryImages.isNotEmpty()) {
                mediaUrl = galleryImages.first().url
                width = galleryImages.first().width
                height = galleryImages.first().height
            }
        }

        // 2. Direct Reddit or external image
        if (mediaUrl == null && (url.endsWith(".jpg", true) || url.endsWith(".png", true) ||
                url.endsWith(".jpeg", true) || url.endsWith(".webp", true) || url.contains("i.redd.it"))) {
            mediaUrl = url
        }

        // 3. Check preview object for high-res source
        if (data.containsKey("preview")) {
            val preview = data["preview"]?.jsonObject
            val images = preview?.get("images")?.jsonArray
            if (!images.isNullOrEmpty()) {
                val source = images[0].jsonObject["source"]?.jsonObject
                val rawSourceUrl = source?.get("url")?.jsonPrimitive?.contentOrNull
                if (rawSourceUrl != null) {
                    val previewUrl = unescapeUrl(rawSourceUrl)
                    if (mediaUrl == null) {
                        mediaUrl = previewUrl
                    }
                    if (width == null) width = source?.get("width")?.jsonPrimitive?.intOrNull
                    if (height == null) height = source?.get("height")?.jsonPrimitive?.intOrNull
                }
            }
        }

        // If no media could be identified, this is a text post or unsupported link
        if (mediaUrl == null && galleryImages.isEmpty()) {
            return null
        }

        return RedditPost(
            id = id,
            name = name,
            subreddit = sub,
            title = title,
            author = author,
            permalink = permalink,
            postUrl = fullPostUrl,
            mediaUrl = mediaUrl,
            thumbnailUrl = thumbnail ?: mediaUrl,
            width = width,
            height = height,
            isNsfw = isNsfw,
            isGallery = isGallery,
            galleryImages = galleryImages,
            score = score,
            createdUtc = createdUtc
        )
    }

    private fun tryRssFallback(subreddit: String): ExtractionResult {
        val rssUrl = "https://www.reddit.com/r/$subreddit/.rss"
        logDebug(TAG, "[Reddit] Attempting RSS fallback: $rssUrl")
        val request = Request.Builder()
            .url(rssUrl)
            .header("User-Agent", userAgent)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ExtractionResult.Error("RSS fallback failed with code ${response.code}")
                }
                val body = response.body?.string() ?: return ExtractionResult.Error("Empty RSS body")
                parseRssXml(body, subreddit)
            }
        } catch (e: Exception) {
            ExtractionResult.Error("RSS fallback error: ${e.message}", e)
        }
    }

    fun parseRssXml(xmlString: String, subreddit: String): ExtractionResult {
        val posts = mutableListOf<RedditPost>()
        val entryRegex = Regex("<entry>([\\s\\S]*?)</entry>", RegexOption.MULTILINE)
        val titleRegex = Regex("<title>([\\s\\S]*?)</title>")
        val linkRegex = Regex("<link href=\"([^\"]+)\"")
        val idRegex = Regex("<id>([\\s\\S]*?)</id>")
        val contentRegex = Regex("<content type=\"html\">([\\s\\S]*?)</content>")
        val imgRegex = Regex("&lt;img src=\"([^\"]+)\"")

        entryRegex.findAll(xmlString).forEach { match ->
            val entry = match.groupValues[1]
            val title = titleRegex.find(entry)?.groupValues?.get(1)?.trim() ?: "Untitled"
            val link = linkRegex.find(entry)?.groupValues?.get(1) ?: ""
            val rawId = idRegex.find(entry)?.groupValues?.get(1) ?: ""
            val id = rawId.substringAfterLast("/").ifEmpty { java.util.UUID.randomUUID().toString() }
            val content = contentRegex.find(entry)?.groupValues?.get(1) ?: ""

            // Extract image link from embedded content
            var imgUrl = imgRegex.find(content)?.groupValues?.get(1)
            if (imgUrl != null) {
                imgUrl = unescapeUrl(imgUrl)
                posts.add(
                    RedditPost(
                        id = id,
                        name = "t3_$id",
                        subreddit = subreddit,
                        title = title,
                        author = "reddit_user",
                        permalink = link.removePrefix("https://www.reddit.com"),
                        postUrl = link,
                        mediaUrl = imgUrl,
                        thumbnailUrl = imgUrl,
                        width = null,
                        height = null,
                        isNsfw = false,
                        isGallery = false,
                        galleryImages = emptyList(),
                        score = 0,
                        createdUtc = System.currentTimeMillis() / 1000
                    )
                )
            }
        }

        return ExtractionResult.Success(posts)
    }

    private fun unescapeUrl(url: String): String {
        return url.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
    }
}
