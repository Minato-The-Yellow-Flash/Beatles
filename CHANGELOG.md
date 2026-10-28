# 📝 Changelog

All notable changes to the **Beatles** music streaming application are documented in this file.

---

## [2.0] - 2026-10-29

### 🐛 Fixes
- **Search Bar Cursor Position**: Fixed cursor jumping back to index 0 after playing a search result; cursor now remains at the end of the query text.  
  *(Special shout-out to **Vimal Updhyaya** for reporting this issue!)*
- **App Update Dialog Bottom Cutting**: Re-architected the update prompt into an iOS-style centered modal card (`Alignment.Center`) with full `DialogWindowProvider` edge-to-edge support, preventing the dialog from getting cut off on gesture and 3-button navigation bars.
- **Album Art Corner Hairlines**: Eliminated the outer specular edge stroke causing thin hairlines around the top corners of the media player artwork.
- **Browse Detail Header Layering**: Decoupled genre banner title typography from the artwork's vertical gradient destination-in mask, ensuring title text remains 100% crisp and unmasked.
- **Browse Exit Action Button Flicker**: Fixed action buttons lingering or flickering during sub-page pop/dismiss transitions.
- **Dynamic Genre-Specific Albums**: Replaced the static 12-album fallback in browse genre pages with real-time dynamic album queries powered by iTunes Search API.
- **Next Up Queue Spacing**: Lowered auto-current and queue suggestions by 10px and removed the rogue banner between consecutive tracks.
- **Onboarding Permission Timing**: Removed premature permission prompting on the first screen of onboarding; media permissions are now requested contextually when performing actions (e.g., song downloads).

### ✨ New Features & Improvements
- **Card Popup Modal Player**: Replaced the previous mini-player capsule container with a smooth, fluid card expand and dismiss animation featuring background dim scrim and blur.
- **Seamless In-Place Song Playback**: Tapping any song plays instantly via the mini-player without forcing the full-screen player open; album track sets retain the expanded view.
- **Home Category Filter Pills**: Relocated category filters ("All", "Radio", "Podcasts", "Pop", "Hip-Hop", "Punjabi", "Bollywood", "Lo-Fi", "Chill", "Workout", etc.) directly beneath the bold "Home" header to dynamically filter the hero banner, trending albums, and top tracks.
- **Trending Albums Section**: Added a dedicated trending albums section tailored to listening habits and spotlight singers.
- **Onboarding Story Slides**: Redesigned Page 1 of onboarding with elevated Beatles branding, Apple SF Pro typography, and an interactive animated carousel of sticky note slides with 3D wrap-up/fold physics:
  - *“Designed with Love”* by Aashirvad Shukla
  - *“It All Started with a Java Project”*
  - *“Zero Ads. High Fidelity.”* (320kbps Lossless)
  - *“BitChord Dynamic Mix Engine”*
- **Apple SF Pro Typography**: Standardized letter spacing, tracking, and font weights across all headers, cards, and modal sheets.

---

## [1.9] - 2026-10-18

### 🐛 Fixes & Improvements
- **Queue Song Item Curves**: Updated queue item corner radius to pure `RoundedCornerShape(30)` for a balanced modern aesthetic.
- **Made For You 'Show More'**: Added fluid navbar liquid-glass 'Show More' card with arrow for seamless personalized discovery.
- **Free Download & User Analytics Engine**: Real-time APK download counts fetched live from GitHub Releases API.
- **User Geolocation & Hardware Telemetry**: Country, city, region, device model, and OS tracking via free GeoIP without paid subscriptions.
- **Interactive Admin Analytics Hub**: In-app real-time downloads dashboard with 1-click Google Apps Script webhook integration for free Google Sheets logging.

---

## [1.8] - 2026-10-10

### 🐛 Fixes & Improvements
- **Pill Queue Items**: Refined queue item layout without visual rendering artifacts.
- **Album Morph**: Fluid transition between album view and active playback.
- **Explore Genre Show-More**: Richer recommendations and deep query expansions for regional and international genres.
- **Top Charts Cooldown**: Added a 2-minute caching window to prevent redundant network requests.
- **Movie Albums**: Enabled movie soundtrack queries and distinct album set groupings.
