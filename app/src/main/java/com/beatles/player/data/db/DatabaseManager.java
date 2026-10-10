package com.beatles.player.data.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/**
 * Singleton Database Manager providing thread-safe SQLite connection management,
 * ACID transaction control, and JDBC-style parameterized statement execution.
 */
public class DatabaseManager {

    private static volatile DatabaseManager instance;
    private final DatabaseHelper dbHelper;

    private DatabaseManager(Context context) {
        this.dbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public static DatabaseManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager(context);
                }
            }
        }
        return instance;
    }

    public SQLiteDatabase getWritableDatabase() {
        return dbHelper.getWritableDatabase();
    }

    public SQLiteDatabase getReadableDatabase() {
        return dbHelper.getReadableDatabase();
    }

    public void beginTransaction() {
        getWritableDatabase().beginTransactionNonExclusive();
    }

    public void setTransactionSuccessful() {
        getWritableDatabase().setTransactionSuccessful();
    }

    public void endTransaction() {
        getWritableDatabase().endTransaction();
    }

    /**
     * Executes a parameterized DDL or DML statement with protection against SQL injection.
     */
    public void execute(String sql, Object[] bindArgs) {
        SQLiteDatabase db = getWritableDatabase();
        if (bindArgs == null || bindArgs.length == 0) {
            db.execSQL(sql);
            return;
        }

        SQLiteStatement stmt = db.compileStatement(sql);
        try {
            for (int i = 0; i < bindArgs.length; i++) {
                Object arg = bindArgs[i];
                int index = i + 1; // 1-based indexing in JDBC / SQLiteStatement
                if (arg == null) {
                    stmt.bindNull(index);
                } else if (arg instanceof Long || arg instanceof Integer) {
                    stmt.bindLong(index, ((Number) arg).longValue());
                } else if (arg instanceof Double || arg instanceof Float) {
                    stmt.bindDouble(index, ((Number) arg).doubleValue());
                } else if (arg instanceof byte[]) {
                    stmt.bindBlob(index, (byte[]) arg);
                } else {
                    stmt.bindString(index, arg.toString());
                }
            }
            stmt.execute();
        } finally {
            stmt.close();
        }
    }

    /**
     * Executes a raw parameterized query returning a Cursor.
     */
    public Cursor query(String sql, String[] selectionArgs) {
        return getReadableDatabase().rawQuery(sql, selectionArgs);
    }
}
