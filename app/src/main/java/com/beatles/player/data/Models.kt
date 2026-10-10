package com.beatles.player.data

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val durationSeconds: Int,
    val streamUrl: String? = null,
    val genre: String = "Various",
    val isAlbum: Boolean = false
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)

enum class UserRole { LISTENER, ARTIST, ADMIN }
enum class ArtistTab { FEED, UPLOAD, ANALYTICS, MESSAGES }
enum class AdminTab { DASHBOARD, USERS, CONTENT, SETTINGS }

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val genre: String = "Various",
    val releaseYear: String = "2026",
    val releaseDate: String = "Jan 9, 2026",
    val type: String = "Album",
    val videoUrl: String? = null,
    val songs: List<Song> = emptyList()
)

data class AlbumSet(
    val id: String,
    val name: String,
    val description: String = "",
    val albums: List<Album> = emptyList()
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList()
)


data class MockUser(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val joinedDate: String
)

data class TrackSubmission(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val submittedDate: String,
    val status: String // "pending", "approved", "rejected"
)

data class FanMessage(
    val id: String,
    val fromUser: String,
    val text: String,
    val timeAgo: String,
    val avatarUrl: String = ""
)

data class FeedbackTicket(
    val id: String,
    val user: String,
    val msg: String,
    val timeAgo: String,
    val type: String // "bug", "praise", "feature"
)
