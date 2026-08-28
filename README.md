# Reddit Wallpaper — Android MVP

A lightweight, personal-use Android application built with modern Kotlin, Jetpack Compose, Material 3, Room, and WorkManager to fetch high-resolution wallpapers from Reddit, cache them offline, and set/rotate device wallpapers.

---

## Features

- **Multi-Strategy Reddit Extractor**:
  - Unauthenticated `.json` extraction with custom User-Agent and exponential backoff.
  - Multi-image Reddit gallery parsing (`media_metadata`).
  - RSS feed parser fallback (`/r/<sub_name>/.rss`).
  - Unescapes preview URLs (`&amp;` -> `&`) and targets highest resolution originals.
- **Smart Filtering**:
  - Filter by orientation (Portrait, Landscape, All).
  - Minimum resolution threshold checking.
  - Optional NSFW filter (disabled by default).
- **Subreddit Manager**:
  - Add and delete custom subreddits (`r/wallpapers`, `r/EarthPorn`, `r/Animewallpaper`, etc.).
  - Instant chip switching with responsive caching.
- **Offline & Cache-First Architecture**:
  - Room database (`SubredditEntity`, `CachedPostEntity`, `WallpaperEntity`).
  - Browsed feeds and downloaded wallpapers are accessible offline.
- **Full-Screen Wallpaper Viewer**:
  - High-res pinch-to-zoom and pan gesture support.
  - Direct actions: **Set Wallpaper** (Home, Lock, or Both), **Download/Save**, **Favorite**, and **Open on Reddit**.
- **Background Auto-Rotation**:
  - Uses Android `WorkManager` + `WallpaperManager`.
  - Configurable rotation intervals (1h – 24h) and target screen selection.
  - Rotates strictly from locally cached wallpapers to preserve battery and prevent Reddit rate-limiting.

---

## Project Structure

```
reddit-wallpaper/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/wallpaper/reddit/
│   │   │   ├── data/
│   │   │   │   ├── model/         # RedditPost, GalleryImage, Enums
│   │   │   │   ├── extractor/     # RedditHttpExtractor, MediaResolver, RedditSource
│   │   │   │   ├── db/            # Room Database, DAOs, Entities
│   │   │   │   └── repository/    # WallpaperRepository (Network + Cache coordinator)
│   │   │   ├── service/           # WallpaperSetter (WallpaperManager integration)
│   │   │   ├── worker/            # WallpaperRotationWorker (WorkManager periodic task)
│   │   │   ├── ui/
│   │   │   │   ├── theme/         # Material 3 Dark theme, typography, colors
│   │   │   │   ├── components/    # WallpaperCard, SubredditChips, Dialogs, ErrorBanner
│   │   │   │   ├── viewmodel/     # HomeViewModel, ViewerViewModel, SettingsViewModel
│   │   │   │   └── screens/       # HomeScreen, WallpaperViewerScreen, FavoritesScreen, SettingsScreen
│   │   │   ├── RedditWallpaperApp.kt
│   │   │   └── MainActivity.kt
│   │   └── res/                   # Drawables, colors, strings, themes, adaptive icons
│   └── src/test/                  # Unit tests (RedditHttpExtractorTest, MediaResolverTest)
├── gradle/
│   ├── libs.versions.toml         # Version catalog (Compose BOM, Kotlin 2.0, Room, OkHttp)
│   └── wrapper/                   # Gradle wrapper configuration
├── build.gradle.kts
└── settings.gradle.kts
```

---

## How to Build & Install

### Option A: Using Android Studio
1. Open Android Studio.
2. Select **Open** and select `/home/anu/Workspace/data_hoard/reddit-wallpaper`.
3. Connect your Android phone via USB (with USB Debugging enabled).
4. Click **Run** (`Shift + F10`).

### Option B: Using Gradle & ADB CLI
1. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
2. Install directly onto your connected device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
