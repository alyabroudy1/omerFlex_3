package com.omerflex.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.omerflex.dao.ServerConfigDao;
import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieHistory;
import com.omerflex.entity.ServerConfig;

@Database(entities = {Movie.class, MovieHistory.class, ServerConfig.class}, version = 7, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract MovieDao movieDao();
    public abstract MovieHistoryDao movieHistoryDao();
    public abstract ServerConfigDao serverConfigDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "omerflex_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}