package com.beatles.player.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.beatles.player.data.Playlist;
import com.beatles.player.data.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for custom Playlists and their associated tracks.
 */
public class PlaylistDao {

    private final DatabaseManager dbManager;
    private final SongDao songDao;

    public PlaylistDao(Context context) {
        this.dbManager = DatabaseManager.getInstance(context);
        this.songDao = new SongDao(context);
    }

    public synchronized List<Playlist> getPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        String sql = "SELECT id, name FROM " + DatabaseHelper.TABLE_PLAYLISTS + " ORDER BY created_at ASC";

        Cursor cursor = dbManager.query(sql, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    String name = cursor.getString(1);
                    List<Song> songs = getSongsForPlaylist(id);
                    playlists.add(new Playlist(id, name, songs));
                }
            } finally {
                cursor.close();
            }
        }
        return playlists;
    }

    public synchronized List<Song> getSongsForPlaylist(String playlistId) {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT s.id, s.title, s.artist, s.album, s.cover_url, s.duration_seconds, s.stream_url, s.genre, s.is_album " +
                     "FROM " + DatabaseHelper.TABLE_PLAYLIST_SONGS + " ps " +
                     "JOIN " + DatabaseHelper.TABLE_SONGS + " s ON ps.song_id = s.id " +
                     "WHERE ps.playlist_id = ? " +
                     "ORDER BY ps.position ASC";

        Cursor cursor = dbManager.query(sql, new String[]{playlistId});
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    String title = cursor.getString(1);
                    String artist = cursor.getString(2);
                    String album = cursor.getString(3);
                    String coverUrl = cursor.getString(4);
                    int duration = cursor.getInt(5);
                    String streamUrl = cursor.getString(6);
                    String genre = cursor.getString(7);
                    boolean isAlbum = cursor.getInt(8) == 1;

                    songs.add(new Song(id, title, artist, album, coverUrl, duration, streamUrl, genre, isAlbum));
                }
            } finally {
                cursor.close();
            }
        }
        return songs;
    }

    public synchronized void savePlaylist(Playlist playlist) {
        if (playlist == null || playlist.getId() == null) return;

        dbManager.beginTransaction();
        try {
            SQLiteDatabase db = dbManager.getWritableDatabase();

            ContentValues plValues = new ContentValues();
            plValues.put("id", playlist.getId());
            plValues.put("name", playlist.getName() != null ? playlist.getName() : "Untitled Playlist");
            plValues.put("created_at", System.currentTimeMillis());
            db.insertWithOnConflict(DatabaseHelper.TABLE_PLAYLISTS, null, plValues, SQLiteDatabase.CONFLICT_REPLACE);

            // Replace associated playlist songs
            db.delete(DatabaseHelper.TABLE_PLAYLIST_SONGS, "playlist_id = ?", new String[]{playlist.getId()});

            List<Song> songs = playlist.getSongs();
            if (songs != null) {
                for (int i = 0; i < songs.size(); i++) {
                    Song s = songs.get(i);
                    songDao.upsertSong(s);

                    ContentValues linkValues = new ContentValues();
                    linkValues.put("playlist_id", playlist.getId());
                    linkValues.put("song_id", s.getId());
                    linkValues.put("position", i);
                    db.insert(DatabaseHelper.TABLE_PLAYLIST_SONGS, null, linkValues);
                }
            }

            dbManager.setTransactionSuccessful();
        } finally {
            dbManager.endTransaction();
        }
    }

    public synchronized void deletePlaylist(String playlistId) {
        if (playlistId == null) return;
        SQLiteDatabase db = dbManager.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_PLAYLISTS, "id = ?", new String[]{playlistId});
    }
}
