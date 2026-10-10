# 🎵 Beatles — Modern Android Music Experience

<p align="center">
  <img src="app/src/main/res/drawable/beatles_logo.xml" width="100" height="100" alt="Beatles Logo" />
</p>

<p align="center">
  <strong>Pure Sound. Liquid Aesthetics. Seamless Harmonic Mixing.</strong>
</p>

<p align="center">
  <a href="https://www.oracle.com/java/"><img src="https://img.shields.io/badge/Java-17%20%7C%20OOP%20%26%20JDBC-ED8B00.svg?style=flat&logo=openjdk" alt="Java 17" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=flat&logo=kotlin" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-2024.12.01-4285F4.svg?style=flat&logo=android" alt="Compose" /></a>
  <a href="https://developer.android.com/media/media3"><img src="https://img.shields.io/badge/AndroidX%20Media3-1.5.1-34A853.svg?style=flat" alt="Media3" /></a>
  <a href="https://github.com/TeamNewPipe/NewPipeExtractor"><img src="https://img.shields.io/badge/NewPipe%20Extractor-v0.26.5-FF0000.svg?style=flat" alt="NewPipe" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat" alt="License: GPL v3" /></a>
  <img src="https://img.shields.io/badge/API-26%2B%20(Android%208.0%2B)-success.svg?style=flat" alt="API 26+" />
</p>

---

## 🌟 Overview

**Beatles** is a flagship Android music streaming application combining enterprise-grade **Java** persistence and database architecture with a reactive **Jetpack Compose** frontend. Designed around Material 3 expressive UI principles, hardware-accelerated real-time blur, and the harmonic intelligence of professional audio software, Beatles delivers a pristine, continuous listening experience.

Beatles combines hardware-accelerated progressive blur, synchronized real-time lyrics with fluid spring physics, YouTube Music audio stream extraction, and an intelligent **AutoMix engine** that harmonically blends tracks without abrupt stops, silence, or awkward transitions.

---

## ✨ Key Features

### 🎛️ Intelligent Harmonic AutoMix Engine
- **Harmonic Compatibility Analysis**: Detects key and musical tempo to seamlessly pair consecutive tracks.
- **Dynamic Structural Scanning**: Scans up to 60 seconds of the outgoing song and 120 seconds of the incoming song to trigger transitions at musical downbeats and natural phrase boundaries.
- **Strict Genre & Language Alignment**: Guarantees transitions stay within complementary musical vibes and user-selected languages.
- **Equal-Power Crossfading**: Replaces abrupt track changes with smooth exponential volume fades and zero dead air.

### 🎤 Dynamic Synchronized Animated Lyrics
- **Spring-Physics Word Highlighting**: Active lyrics glow and scale dynamically in sync with the song’s vocal pace.
- **Smooth Viewport Tracking**: Viewport gently scrolls to keep current singing lines center-stage.
- **Progressive Depth**: Inactive past lines gracefully blur and fade, while upcoming verses stay subtly visible.
- **Multi-Source Fallback**: Real-time sync powered by [LRCLIB](https://lrclib.net/) with high-accuracy line timecodes.

### 💎 Liquid Glass Design & Expressive Micro-Interactions
- **Haze-Powered Liquid Glassmorphism**: High-performance real-time background blurring using [Haze](https://github.com/chrisbanes/haze).
- **Progressive Masked Gradients**: Header and navigation bars blend smoothly with underlying scrolling content using vertical gradient destination-in masks.
- **Full Theme Adaptability**: Complete Light and Dark theme palettes with dynamic surface tinting and contrast optimization.
- **Fluid Physics-Based Modal Sheets**: Continuous gesture drag-to-dismiss for full-screen playback with natural inertia and predictive back support.

### 🎧 Studio-Grade Audio Architecture
- **Official Studio Audio Priority**: Automatically extracts pristine YouTube Music audio streams with highest available bitrate (Opus/AAC), avoiding low-quality video tracks.
- **Media3 & ExoPlayer Foundation**: Robust background playback service with media session integration and lock-screen controls.
- **High-Definition Artwork**: Automatically fetches 1000×1000 ultra-high-definition artwork via iTunes Search & Deezer APIs with YouTube `hqdefault` fallbacks.
- **Offline Persistence & Favorites**: Instant local library caching for starred songs, playlists, recently played history, and artist preferences.

---

## ☕ Java Architecture & Core Concepts Implementation

The core data persistence and transactional storage layer of Beatles is built in **pure Java** (`com.beatles.player.data.db`), adhering to software engineering best practices:

### 1. Object-Oriented Programming (OOP) Implementation
- **Encapsulation**: Critical state within database components (`DatabaseManager`, `DatabaseHelper`, DAOs) is kept private with controlled public accessors and mutators.
- **Inheritance & Polymorphism**:
  - `DatabaseHelper extends SQLiteOpenHelper` demonstrates class inheritance, overriding polymorphic lifecycle hooks: `onCreate()`, `onUpgrade()`, and `onConfigure()`.
- **Interfaces & Design Patterns**:
  - **DAO (Data Access Object) Pattern**: `SongDao.java`, `PlaylistDao.java`, and `UserDao.java` decouple business logic from underlying SQL storage.
  - **Thread-Safe Singleton Pattern**: `DatabaseManager.java` employs the classic double-checked locking idiom with a `volatile` instance reference to ensure thread-safe single-instance management.
- **Robust Exception Handling**:
  - Defensive SQL execution wrapped in structured `try-catch-finally` blocks.
  - Guarantees transaction finalization via `endTransaction()` in `finally` clauses and proper closing of `Cursor` resources.

### 2. Multithreading & Synchronization
- **Thread Synchronization**: Database write and read operations utilize `synchronized` methods to prevent race conditions during concurrent playback logging and playlist updates.
- **ACID Transaction Management**: Explicit transaction boundaries (`beginTransactionNonExclusive()`, `setTransactionSuccessful()`, `endTransaction()`) ensure atomic multi-table operations (e.g., adding songs to playlists with order indexes).
- **Non-blocking Execution**: Java database routines are invoked seamlessly from background worker coroutine dispatchers (`Dispatchers.IO`), keeping the Compose UI thread at 60/120 FPS.

### 3. Collections & Generics
- Extensive usage of Java Collections Framework: `java.util.List`, `java.util.ArrayList`, `java.util.Map`, `java.util.Set`.
- Type-safe generic method signatures and object mapping algorithms (e.g., `mapCursorToSong(Cursor cursor)` transforming raw database rows into strongly-typed `Song` instances).

### 4. Database Operations & SQL Schema (JDBC / SQLite Architecture)
- **Parameterized SQL Statements**: All queries utilize parameterized bind arguments (`?`), preventing SQL injection attacks.
- **Relational Integrity**: Foreign keys with `ON DELETE CASCADE` support maintaining integrity across tables:
  - `songs`: Primary song catalogue cache.
  - `favorites`: User-starred tracks.
  - `playback_history`: Chronological playback log.
  - `playlists` & `playlist_songs`: Custom playlists with track order indexes.
  - `downloads`: Offline cache references.
  - `user_preferences`: Key-value configuration store.

### 5. Java & Kotlin Seamless Interoperability
- Demonstrates bidirectional cross-language communication: Modern Kotlin ViewModels and StateFlow architectures query and observe Java Data Access Objects with zero friction.

---

## 🛠️ Tech Stack & Architecture

| Layer | Technologies & Libraries |
| :--- | :--- |
| **Languages** | Java 17 (Database & Architecture), Kotlin 2.0.21 (UI & Media Pipeline) |
| **Database & Persistence**| SQLite, JDBC-style DAOs, `SQLiteOpenHelper`, SharedPreferences |
| **UI Framework** | Jetpack Compose (BOM 2024.12.01) + Material 3 |
| **Graphics & Blur** | Chris Banes Haze (`dev.chrisbanes.haze:haze:0.7.1`), Compose Offscreen Compositing |
| **Audio Engine** | AndroidX Media3 ExoPlayer 1.5.1, MediaSession |
| **Stream Extraction**| TeamNewPipe Extractor v0.26.5 |
| **Networking & HTTP**| OkHttp 4.12.0 |
| **Image Loading** | Coil Compose 2.7.0 |
| **Serialization** | Gson 2.11.0 |
| **Concurrency** | Java Thread Synchronization, Kotlin Coroutines & StateFlow |
| **Architecture** | MVVM (Model-View-ViewModel) + Single Activity Architecture |

---

## 📂 Project Structure

```
BeatlesAndroid/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/beatles/player/
│   │   │   ├── MainActivity.kt               # Single Activity lifecycle entry point
│   │   │   ├── data/
│   │   │   │   ├── Models.kt                 # Song, LyricLine, Album data entities
│   │   │   │   ├── MusicRepository.kt        # NewPipe stream extractor, iTunes artwork & LRCLib
│   │   │   │   ├── LocalStorage.kt           # SharedPreferences local cache & favorites
│   │   │   │   ├── Downloader.kt             # OkHttp networking provider
│   │   │   │   └── db/                       # Java Enterprise Persistence Layer
│   │   │   │       ├── DatabaseHelper.java   # SQLiteOpenHelper schema & table migrations
│   │   │   │       ├── DatabaseManager.java  # Thread-safe Singleton connection & transaction manager
│   │   │   │       ├── SongDao.java          # Song, favorites & history DAO
│   │   │   │       ├── PlaylistDao.java      # Playlist CRUD operations DAO
│   │   │   │       └── UserDao.java          # User preferences DAO
│   │   │   ├── player/
│   │   │   │   ├── AudioPlayerManager.kt     # Media3 ExoPlayer controller & queue management
│   │   │   │   ├── PlaybackService.kt        # Foreground Media3 session playback service
│   │   │   │   ├── BeatlesMediaCache.kt      # Disk cache manager
│   │   │   │   └── automix/                  # Harmonic AutoMix transition engine
│   │   │   │       ├── AutoMixEngine.kt      # Transition trigger & scheduling
│   │   │   │       ├── TrackAnalyzer.kt      # Harmonic & tempo detector
│   │   │   │       ├── TransitionPlanner.kt  # Downbeat & phrase alignment
│   │   │   │       └── WaveformAnalyzer.kt   # Volume & loudness envelope calculator
│   │   │   ├── ui/
│   │   │   │   ├── AppNavigation.kt          # Root navigation, liquid tabs, bottom sheet
│   │   │   │   ├── components/               # NowPlayingSheet, AlbumDetailSheet, EdgeFade
│   │   │   │   ├── screens/                  # Home, Explore, Search, Library, Onboarding
│   │   │   │   └── theme/                    # Theme, Color, Type definitions
│   │   └── res/                              # Drawables, mipmaps, themes, vector assets
│   └── build.gradle.kts                      # App dependencies & SDK configuration
├── gradle/wrapper/                           # Official Gradle Wrapper binaries
├── build.gradle.kts                          # Root project build configuration
├── settings.gradle.kts                       # Repository management & project includes
├── .gitignore                                # Comprehensive Android ignore rules
├── LICENSE                                   # GNU General Public License v3.0
└── README.md                                 # Project documentation
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Android Studio Ladybug (2024.2.1+) or Meerkat (2024.3.1+)
- **JDK**: Java Development Kit 17 or higher
- **Android SDK**: Compile SDK 35, Min SDK 26 (Android 8.0 Oreo+)

### Build & Run from Command Line

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Minato-The-Yellow-Flash/Beatles.git
   cd Beatles
   ```

2. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on connected device or emulator**:
   ```bash
   ./gradlew installDebug
   ```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤝 Contributing & Developer Guide

We welcome contributions from developers of all skill levels! Whether you want to improve audio processing, enhance the database layer, or design new UI interactions, here is how you can get started:

### 💡 High-Impact Contribution Areas

1. **Java Persistence & Database Layer (`data/db`)**:
   - Implement SQLite Full-Text Search (`FTS5`) for instant offline searching of cached songs and artists.
   - Add database migration tests and automated data export/backup utilities.
2. **Harmonic AutoMix Engine (`player/automix`)**:
   - Refine harmonic Camelot wheel compatibility transitions in `TrackAnalyzer.kt`.
   - Improve BPM tempo detection and dynamic phrase boundary alignment algorithms.
3. **Audio Features & Player Pipeline (`player/`)**:
   - Add an equalizer (parametric EQ / bass boost) interface using Android AudioEffect APIs.
   - Add support for gapless FLAC / local storage scanning alongside streaming.
4. **UI & Jetpack Compose (`ui/`)**:
   - Build custom playback visualizers with Compose Canvas shaders.
   - Add landscape / tablet / foldable responsive layouts.

### 🛠️ Contribution Workflow

1. **Fork the Repository**: Click the **Fork** button at the top-right of the GitHub repository page.
2. **Clone your fork**:
   ```bash
   git clone https://github.com/<your-username>/Beatles.git
   cd Beatles
   ```
3. **Create a descriptive feature branch**:
   ```bash
   git checkout -b feature/eq-support
   ```
4. **Make your changes & verify the build**:
   ```bash
   ./gradlew test assembleDebug
   ```
5. **Commit your changes**:
   ```bash
   git commit -m "feat(audio): add 5-band equalizer interface"
   ```
6. **Push to your fork and submit a Pull Request**:
   ```bash
   git push origin feature/eq-support
   ```
7. Open a **Pull Request (PR)** on the main repository describing your changes and test results.

---

Crafted with care by ~ Team beatles
Members : Aashirvad Shukla
Tarun Kumar 
Yash Kumar

## 📄 License & Legal Notice

This project is licensed under the **GNU General Public License v3.0 (GPLv3)** — see the [LICENSE](LICENSE) file for details.

> **Disclaimer**: This application is built for educational, research, and personal use. Stream extraction is powered by the open-source NewPipe Extractor project. All rights, trademarks, and audio copyrights belong to their respective owners and service providers.
