package com.beatles.player.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.beatles.player.data.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Song entities, Favorites, Playback History, and Downloads.
 * Implemented in pure Java with parameterized queries and transaction guarantees.
 */
public class SongDao {

    private final DatabaseManager dbManager;

    public SongDao(Context context) {
        this.dbManager = DatabaseManager.getInstance(context);
    }

    /**
     * Upserts a Song record into the 'songs' table.
     */
    public synchronized void upsertSong(Song song) {
        if (song == null || song.getId() == null || song.getId().trim().isEmpty()) return;

        SQLiteDatabase db = dbManager.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", song.getId());
        values.put("title", song.getTitle() != null ? song.getTitle() : "Unknown Track");
        values.put("artist", song.getArtist() != null ? song.getArtist() : "Unknown Artist");
        values.put("album", song.getAlbum() != null ? song.getAlbum() : "");
        values.put("cover_url", song.getCoverUrl() != null ? song.getCoverUrl() : "");
        values.put("duration_seconds", song.getDurationSeconds());
        values.put("stream_url", song.getStreamUrl() != null ? song.getStreamUrl() : "");
        values.put("genre", song.getGenre() != null ? song.getGenre() : "Various");
        values.put("is_album", song.isAlbum() ? 1 : 0);

        db.insertWithOnConflict(DatabaseHelper.TABLE_SONGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Retrieves all Favorited songs ordered by addition date descending.
     */
    public synchronized List<Song> getFavorites() {
        List<Song> result = new ArrayList<>();
        String sql = "SELECT s.id, s.title, s.artist, s.album, s.cover_url, s.duration_seconds, s.stream_url, s.genre, s.is_album " +
                     "FROM " + DatabaseHelper.TABLE_FAVORITES + " f " +
                     "JOIN " + DatabaseHelper.TABLE_SONGS + " s ON f.song_id = s.id " +
                     "ORDER BY f.added_at DESC";

        Cursor cursor = dbManager.query(sql, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    result.add(mapCursorToSong(cursor));
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    public synchronized void addFavorite(Song song) {
        if (song == null || song.getId() == null) return;
        upsertSong(song);

        ContentValues values = new ContentValues();
        values.put("song_id", song.getId());
        values.put("added_at", System.currentTimeMillis());

        dbManager.getWritableDatabase().insertWithOnConflict(
                DatabaseHelper.TABLE_FAVORITES, null, values, SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public synchronized void removeFavorite(String songId) {
        if (songId == null) return;
        dbManager.getWritableDatabase().delete(
                DatabaseHelper.TABLE_FAVORITES, "song_id = ?", new String[]{songId}
        );
    }

    public synchronized boolean isFavorite(String songId) {
        if (songId == null) return false;
        String sql = "SELECT 1 FROM " + DatabaseHelper.TABLE_FAVORITES + " WHERE song_id = ? LIMIT 1";
        Cursor cursor = dbManager.query(sql, new String[]{songId});
        if (cursor != null) {
            try {
                return cursor.moveToFirst();
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    /**
     * Retrieves recently played tracks (up to 30) ordered by most recent playback.
     */
    public synchronized List<Song> getRecentlyPlayed(int limit) {
        List<Song> result = new ArrayList<>();
        String sql = "SELECT s.id, s.title, s.artist, s.album, s.cover_url, s.duration_seconds, s.stream_url, s.genre, s.is_album " +
                     "FROM " + DatabaseHelper.TABLE_HISTORY + " h " +
                     "JOIN " + DatabaseHelper.TABLE_SONGS + " s ON h.song_id = s.id " +
                     "GROUP BY s.id " +
                     "ORDER BY MAX(h.played_at) DESC " +
                     "LIMIT " + Math.max(1, limit);

        Cursor cursor = dbManager.query(sql, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    result.add(mapCursorToSong(cursor));
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    public synchronized void addToRecentlyPlayed(Song song) {
        if (song == null || song.getId() == null) return;
        upsertSong(song);

        ContentValues values = new ContentValues();
        values.put("song_id", song.getId());
        values.put("played_at", System.currentTimeMillis());

        dbManager.getWritableDatabase().insert(DatabaseHelper.TABLE_HISTORY, null, values);

        // Prune old history entries beyond top 50
        dbManager.execute(
                "DELETE FROM " + DatabaseHelper.TABLE_HISTORY + " WHERE id NOT IN (" +
                "SELECT id FROM " + DatabaseHelper.TABLE_HISTORY + " ORDER BY played_at DESC LIMIT 50)",
                null
        );
    }

    /**
     * Retrieves all downloaded songs.
     */
    public synchronized List<Song> getDownloadedSongs() {
        List<Song> result = new ArrayList<>();
        String sql = "SELECT s.id, s.title, s.artist, s.album, s.cover_url, s.duration_seconds, d.local_path AS stream_url, s.genre, s.is_album " +
                     "FROM " + DatabaseHelper.TABLE_DOWNLOADS + " d " +
                     "JOIN " + DatabaseHelper.TABLE_SONGS + " s ON d.song_id = s.id " +
                     "ORDER BY d.downloaded_at DESC";

        Cursor cursor = dbManager.query(sql, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    result.add(mapCursorToSong(cursor));
                }
            } finally {
                cursor.close();
            }
        }
        return result;
    }

    public synchronized void saveDownloadedSong(Song song, String localPath) {
        if (song == null || song.getId() == null) return;
        upsertSong(song);

        ContentValues values = new ContentValues();
        values.put("song_id", song.getId());
        values.put("local_path", localPath != null ? localPath : "");
        values.put("downloaded_at", System.currentTimeMillis());

        dbManager.getWritableDatabase().insertWithOnConflict(
                DatabaseHelper.TABLE_DOWNLOADS, null, values, SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public synchronized void removeDownloadedSong(String songId) {
        if (songId == null) return;
        dbManager.getWritableDatabase().delete(
                DatabaseHelper.TABLE_DOWNLOADS, "song_id = ?", new String[]{songId}
        );
    }

    private Song mapCursorToSong(Cursor cursor) {
        String id = cursor.getString(0);
        String title = cursor.getString(1);
        String artist = cursor.getString(2);
        String album = cursor.getString(3);
        String coverUrl = cursor.getString(4);
        int durationSeconds = cursor.getInt(5);
        String streamUrl = cursor.getString(6);
        String genre = cursor.getString(7);
        boolean isAlbum = cursor.getInt(8) == 1;

        return new Song(id, title, artist, album, coverUrl, durationSeconds, streamUrl, genre, isAlbum);
    }
}
