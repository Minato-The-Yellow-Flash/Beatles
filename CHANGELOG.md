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
- **Browse Exit Action Button Flicker**: Action buttons (Play, Shuffle) immediately fade to zero alpha upon dismissal, completely eliminating button flicker when returning from category views.
- **Dynamic Genre-Specific Albums**: Replaced static album fallbacks in browse genre pages with real-time dynamic album queries powered by iTunes Search API.
- **Next Up Queue Spacing**: Lowered auto-current and queue suggestions by 10px and removed any rogue divider banners between consecutive tracks.
- **Onboarding Sticky Notes & Keyboard**: Vertically centered the story sticky notes in the middle of the screen and ensured the Next CTA button retains a crisp, non-collapsing 54dp height when the soft keyboard appears.
- **Hero Banner Progressive Blur**: Replaced static bottom blur with smooth multi-layer progressive blur and vertical gradient scrim, eliminating hard cutoff lines and separating the album artwork from the controls.
- **Trending & Featured Deduplication**: Guaranteed 100% mutual exclusivity between hero banner albums and trending albums carousel.
- **Bottom Navigation Clearance**: Added a clean 5px clearance between scrollable page content and the floating translucent navigation bar.
- **Home Genre Filtering**: Selecting any genre pill dynamically filters the hero albums, trending albums, and top tracks across the entire home feed.
- **Search Popularity Ranking**: Search algorithm now incorporates logarithmic view count weighting and verified artist boosting to prioritize original and popular tracks over low-quality re-uploads.
- **Now Playing 3-Dots Menu Background**: Replaced transparent container with a solid frosted card background and subtle glass refraction for flawless contrast.
- **Seamless Card Dismissal**: Expanded player sheet closes toward the mini-player layer with a 60px swipe threshold and single back gesture minimization.
- **Harmonic AutoMix Beatmatching**: Implemented dynamic tempo and pitch synchronization during DJ crossfades with gentle tempo glide restoration.

### ✨ New Features & Improvements
- **Card Popup Modal Player**: Replaced the previous mini-player capsule container with a smooth, fluid card expand and dismiss animation featuring background dim scrim and blur.
- **Seamless In-Place Song Playback**: Tapping any song plays instantly via the mini-player without forcing the full-screen player open; album track sets retain the expanded view.
- **Home Category Filter Pills**: Relocated category filters ("All", "Radio", "Podcasts", "Pop", "Hip-Hop", "Punjabi", "Bollywood", "Lo-Fi", "Chill", "Workout", etc.) directly beneath the bold "Home" header to dynamically filter the hero banner, trending albums, and top tracks.
- **Trending Albums Section**: Added a dedicated trending albums section tailored to listening habits and spotlight singers.
- **Onboarding Story Slides**: Redesigned Page 1 of onboarding with elevated Beatles branding, Apple SF Pro typography, and an interactive animated carousel of sticky note slides with 3D wrap-up/fold physics:
  - *“Designed with Love”* by Aashirvad Shukla
  - *“It All Started with a Java Project”*
  - *“Zero Ads. High Fidelity.”* (320kbps Lossless)
  - *“Intelligent Harmonic Mix”*
- **Apple SF Pro Typography**: Standardized letter spacing, tracking, and font weights across all headers, cards, and modal sheets.
