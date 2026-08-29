package com.wallpaper.reddit.data.extractor

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.Response

class RedditSessionManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "reddit_browser_session"
        private const val KEY_COOKIES = "saved_cookies"
        const val FIREFOX_DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0"

        @Volatile
        private var INSTANCE: RedditSessionManager? = null

        fun getInstance(context: Context): RedditSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RedditSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hasActiveSession = MutableStateFlow(hasSavedCookies())
    val hasActiveSession: StateFlow<Boolean> = _hasActiveSession.asStateFlow()

    fun getCookies(): String? {
        val webViewCookies = try {
            CookieManager.getInstance().getCookie("https://www.reddit.com")
        } catch (_: Throwable) {
            null
        }
        return if (!webViewCookies.isNullOrBlank()) {
            saveCookies(webViewCookies)
            webViewCookies
        } else {
            prefs.getString(KEY_COOKIES, null)
        }
    }

    fun syncFromCookieManager(): Boolean {
        return try {
            val cookies = CookieManager.getInstance().getCookie("https://www.reddit.com")
            if (!cookies.isNullOrBlank()) {
                saveCookies(cookies)
                _hasActiveSession.value = true
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun saveCookies(cookies: String) {
        prefs.edit().putString(KEY_COOKIES, cookies).apply()
        _hasActiveSession.value = true
    }

    fun clearSession() {
        prefs.edit().remove(KEY_COOKIES).apply()
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (_: Throwable) {}
        _hasActiveSession.value = false
    }

    private fun hasSavedCookies(): Boolean {
        return !prefs.getString(KEY_COOKIES, null).isNullOrBlank()
    }

    fun createBrowserInterceptor(): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val cookies = getCookies()

            val builder = original.newBuilder()
                .header("User-Agent", FIREFOX_DESKTOP_UA)
                .header("Accept", "application/json, text/html, application/xhtml+xml, */*")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("DNT", "1")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Referer", "https://www.reddit.com/")

            if (!cookies.isNullOrBlank()) {
                builder.header("Cookie", cookies)
            }

            chain.proceed(builder.build())
        }
    }
}
