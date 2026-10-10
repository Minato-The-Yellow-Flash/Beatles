package com.beatles.player.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Android SQLite / JDBC-compliant Database Helper.
 * Manages database creation, table schemas, and version upgrades for the Beatles Player.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "beatles_player.db";
    public static final int DATABASE_VERSION = 2;

    // Table names
    public static final String TABLE_SONGS = "songs";
    public static final String TABLE_FAVORITES = "favorites";
    public static final String TABLE_HISTORY = "playback_history";
    public static final String TABLE_PLAYLISTS = "playlists";
    public static final String TABLE_PLAYLIST_SONGS = "playlist_songs";
    public static final String TABLE_DOWNLOADS = "downloads";
    public static final String TABLE_USER_PREFS = "user_preferences";

    // SQL table creation statements
    private static final String CREATE_TABLE_SONGS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_SONGS + " (" +
            "id TEXT PRIMARY KEY, " +
            "title TEXT NOT NULL, " +
            "artist TEXT NOT NULL, " +
            "album TEXT, " +
            "cover_url TEXT, " +
            "duration_seconds INTEGER DEFAULT 0, " +
            "stream_url TEXT, " +
            "genre TEXT DEFAULT 'Various', " +
            "is_album INTEGER DEFAULT 0, " +
            "created_at INTEGER DEFAULT (strftime('%s', 'now'))" +
            ");";

    private static final String CREATE_TABLE_FAVORITES =
            "CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITES + " (" +
            "song_id TEXT PRIMARY KEY, " +
            "added_at INTEGER NOT NULL, " +
            "FOREIGN KEY(song_id) REFERENCES " + TABLE_SONGS + "(id) ON DELETE CASCADE" +
            ");";

    private static final String CREATE_TABLE_HISTORY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_HISTORY + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "song_id TEXT NOT NULL, " +
            "played_at INTEGER NOT NULL, " +
            "FOREIGN KEY(song_id) REFERENCES " + TABLE_SONGS + "(id) ON DELETE CASCADE" +
            ");";

    private static final String CREATE_TABLE_PLAYLISTS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLISTS + " (" +
            "id TEXT PRIMARY KEY, " +
            "name TEXT NOT NULL, " +
            "created_at INTEGER NOT NULL" +
            ");";

    private static final String CREATE_TABLE_PLAYLIST_SONGS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLIST_SONGS + " (" +
            "playlist_id TEXT NOT NULL, " +
            "song_id TEXT NOT NULL, " +
            "position INTEGER NOT NULL, " +
            "PRIMARY KEY(playlist_id, song_id), " +
            "FOREIGN KEY(playlist_id) REFERENCES " + TABLE_PLAYLISTS + "(id) ON DELETE CASCADE, " +
            "FOREIGN KEY(song_id) REFERENCES " + TABLE_SONGS + "(id) ON DELETE CASCADE" +
            ");";

    private static final String CREATE_TABLE_DOWNLOADS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_DOWNLOADS + " (" +
            "song_id TEXT PRIMARY KEY, " +
            "local_path TEXT NOT NULL, " +
            "downloaded_at INTEGER NOT NULL, " +
            "FOREIGN KEY(song_id) REFERENCES " + TABLE_SONGS + "(id) ON DELETE CASCADE" +
            ");";

    private static final String CREATE_TABLE_USER_PREFS =
            "CREATE TABLE IF NOT EXISTS " + TABLE_USER_PREFS + " (" +
            "pref_key TEXT PRIMARY KEY, " +
            "pref_value TEXT NOT NULL" +
            ");";

    // Indexes for fast lookups
    private static final String CREATE_INDEX_HISTORY =
            "CREATE INDEX IF NOT EXISTS idx_history_played_at ON " + TABLE_HISTORY + " (played_at DESC);";
    private static final String CREATE_INDEX_FAVORITES =
            "CREATE INDEX IF NOT EXISTS idx_fav_added_at ON " + TABLE_FAVORITES + " (added_at DESC);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
        db.enableWriteAheadLogging();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SONGS);
        db.execSQL(CREATE_TABLE_FAVORITES);
        db.execSQL(CREATE_TABLE_HISTORY);
        db.execSQL(CREATE_TABLE_PLAYLISTS);
        db.execSQL(CREATE_TABLE_PLAYLIST_SONGS);
        db.execSQL(CREATE_TABLE_DOWNLOADS);
        db.execSQL(CREATE_TABLE_USER_PREFS);
        db.execSQL(CREATE_INDEX_HISTORY);
        db.execSQL(CREATE_INDEX_FAVORITES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Safe additive migration
        if (oldVersion < 2) {
            onCreate(db);
        }
    }
}
