package com.beatles.player.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Data Access Object for persisting key-value user configurations, selected language,
 * and user role in the relational database.
 */
public class UserDao {

    private final DatabaseManager dbManager;

    public UserDao(Context context) {
        this.dbManager = DatabaseManager.getInstance(context);
    }

    public synchronized String getPreference(String key, String defaultValue) {
        String sql = "SELECT pref_value FROM " + DatabaseHelper.TABLE_USER_PREFS + " WHERE pref_key = ? LIMIT 1";
        Cursor cursor = dbManager.query(sql, new String[]{key});
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return defaultValue;
    }

    public synchronized void setPreference(String key, String value) {
        if (key == null) return;
        SQLiteDatabase db = dbManager.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pref_key", key);
        values.put("pref_value", value != null ? value : "");
        db.insertWithOnConflict(DatabaseHelper.TABLE_USER_PREFS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
