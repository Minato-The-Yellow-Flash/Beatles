# 🎵 Beatles — Modern Android Music Experience

<p align="center">
  <img src="app/src/main/res/drawable/beatles_logo.xml" width="100" height="100" alt="Beatles Logo" />
</p>

<p align="center">
  <strong>Pure Sound. Liquid Aesthetics. Seamless Harmonic Mixing.</strong>
</p>

<p align="center">
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=flat&logo=kotlin" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-2024.12.01-4285F4.svg?style=flat&logo=android" alt="Compose" /></a>
  <a href="https://developer.android.com/media/media3"><img src="https://img.shields.io/badge/AndroidX%20Media3-1.5.1-34A853.svg?style=flat" alt="Media3" /></a>
  <a href="https://github.com/TeamNewPipe/NewPipeExtractor"><img src="https://img.shields.io/badge/NewPipe%20Extractor-v0.26.5-FF0000.svg?style=flat" alt="NewPipe" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat" alt="License: GPL v3" /></a>
  <img src="https://img.shields.io/badge/API-26%2B%20(Android%208.0%2B)-success.svg?style=flat" alt="API 26+" />
</p>

---

## 🌟 Overview

**Beatles** is a flagship-grade Android streaming music player built from the ground up using **Kotlin** and **Jetpack Compose**. Inspired by the fluid glass aesthetics of modern iOS and the harmonic intelligence of professional DJ software, Beatles delivers an unparalleled listening experience.

Beatles combines native hardware-accelerated progressive blur, synchronized karaoke lyrics with fluid spring physics, YouTube Music audio stream extraction, and an intelligent **AutoMix engine** that harmonically blends tracks without abrupt stops or awkward cutoffs.

---

## ✨ Key Features

### 🎛️ Intelligent Harmonic AutoMix Engine
- **Harmonic Compatibility Analysis**: Detects key and musical tempo to seamlessly pair consecutive songs.
- **Dynamic Structural Scanning**: Scans up to 60 seconds of the outgoing song and 120 seconds of the incoming song to trigger transitions at musical downbeats and natural phrase boundaries.
- **Strict Genre & Language Alignment**: Guarantees transitions stay within complementary musical vibes and selected languages.
- **Equal-Power Crossfading**: Replaces abrupt track changes with smooth exponential volume fades and zero dead air.

### 🎤 Immersive Apple Music-Style Animated Lyrics
- **Spring-Physics Word Highlighting**: Active lyrics glow and scale dynamically in sync with the song’s vocal pace.
- **Smooth Viewport Tracking**: Viewport gently scrolls to keep current singing lines center-stage.
- **Progressive Depth**: Inactive past lines gracefully blur and fade, while upcoming verses stay subtly visible.
- **Multi-Source Fallback**: Real-time sync powered by [LRCLIB](https://lrclib.net/) with high-accuracy line timecodes.

### 💎 Liquid Glass Design & Native Micro-Interactions
- **Haze-Powered Liquid Glassmorphism**: High-performance real-time background blurring using [Haze](https://github.com/chrisbanes/haze).
- **Progressive Masked Gradients**: Header and navigation bars blend smoothly with underlying scrolling content using vertical gradient destination-in masks.
- **Full Theme Adaptability**: Complete Light and Dark theme palettes with dynamic surface tinting and contrast optimization.
- **Physical Gesture Drawer**: Continuous swipe-down gestures to dismiss full-screen playback with natural inertia.

### 🎧 Studio-Grade Audio Architecture
- **Official Studio Audio Priority**: Automatically extracts pristine YouTube Music audio streams with highest available bitrate (Opus/AAC), avoiding low-quality video tracks.
- **Media3 & ExoPlayer Foundation**: Robust background playback service with media session integration and lock-screen controls.
- **High-Definition Artwork**: Automatically fetches 1000×1000 ultra-high-definition artwork via iTunes Search & Deezer APIs with YouTube `hqdefault` fallbacks.
- **Offline Persistence & Favorites**: Instant local library caching for starred songs, playlists, recently played history, and artist preferences.

---

## 🛠️ Tech Stack & Architecture

| Layer | Technologies & Libraries |
| :--- | :--- |
| **Language** | Kotlin 2.0.21 |
| **UI Framework** | Jetpack Compose (BOM 2024.12.01) + Material 3 |
| **Graphics & Blur** | Chris Banes Haze (`dev.chrisbanes.haze:haze:0.7.1`), Compose Offscreen Compositing |
| **Audio Engine** | AndroidX Media3 ExoPlayer 1.5.1, MediaSession |
| **Stream Extraction**| TeamNewPipe Extractor v0.26.5 |
| **Networking & HTTP**| OkHttp 4.12.0 |
| **Image Loading** | Coil Compose 2.7.0 |
| **Serialization** | Gson 2.11.0 |
| **Concurrency** | Kotlin Coroutines & StateFlow (Lifecycle Runtime Compose 2.8.7) |
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
│   │   │   │   └── Downloader.kt             # OkHttp networking provider
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
   git clone https://github.com/<your-username>/beatles-android.git
   cd beatles-android
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

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License & Legal Notice

This project is licensed under the **GNU General Public License v3.0 (GPLv3)** — see the [LICENSE](LICENSE) file for details.

> **Disclaimer**: This application is built for educational, research, and personal use. Stream extraction is powered by the open-source NewPipe Extractor project. All rights, trademarks, and audio copyrights belong to their respective owners and service providers.
