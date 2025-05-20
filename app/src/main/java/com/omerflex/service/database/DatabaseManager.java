package com.omerflex.service.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.omerflex.service.concurrent.ThreadPoolManager;

import java.util.concurrent.Executor;

/**
 * Manager class for database operations.
 * Provides optimized access to the database with proper threading.
 */
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";

    // Singleton instance
    private static DatabaseManager instance;

    // Database helper
    private final MovieDbHelper dbHelper;

    // Thread pool for database operations
    private final Executor diskExecutor;

    private DatabaseManager(Context context) {
        dbHelper = MovieDbHelper.getInstance(context);
        diskExecutor = ThreadPoolManager.getInstance().getDiskExecutor();
    }

    /**
     * Get the singleton instance of DatabaseManager
     * @param context Application context
     * @return DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Execute a database read operation asynchronously
     * @param operation Database operation to execute
     */
    public void executeRead(final DatabaseOperation operation) {
        diskExecutor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                db = dbHelper.getReadableDatabase();
                operation.execute(db);
            } catch (Exception e) {
                Log.e(TAG, "Error executing database read operation", e);
                operation.onError(e);
            }
        });
    }

    /**
     * Execute a database write operation asynchronously
     * @param operation Database operation to execute
     */
    public void executeWrite(final DatabaseOperation operation) {
        diskExecutor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                operation.execute(db);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Error executing database write operation", e);
                operation.onError(e);
            } finally {
                if (db != null && db.inTransaction()) {
                    db.endTransaction();
                }
            }
        });
    }

    /**
     * Execute a database read operation synchronously
     * @param operation Database operation to execute
     */
    public void executeReadSync(final DatabaseOperation operation) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getReadableDatabase();
            operation.execute(db);
        } catch (Exception e) {
            Log.e(TAG, "Error executing database read operation", e);
            operation.onError(e);
        }
    }

    /**
     * Execute a database write operation synchronously
     * @param operation Database operation to execute
     */
    public void executeWriteSync(final DatabaseOperation operation) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            operation.execute(db);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error executing database write operation", e);
            operation.onError(e);
        } finally {
            if (db != null && db.inTransaction()) {
                db.endTransaction();
            }
        }
    }

    /**
     * Get the MovieDbHelper instance
     * @return MovieDbHelper instance
     */
    public MovieDbHelper getDbHelper() {
        return dbHelper;
    }

    /**
     * Reset the singleton instance (for testing purposes)
     */
    public static synchronized void reset() {
        instance = null;
    }

    /**
     * Interface for database operations
     */
    public interface DatabaseOperation {
        void execute(SQLiteDatabase db);
        default void onError(Exception e) {
            // Default implementation does nothing
        }
    }
}
