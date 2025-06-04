package com.omerflex.service.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.omerflex.entity.IptvEntry;
import com.omerflex.entity.Movie;
import com.omerflex.entity.MovieHistory;
import com.omerflex.entity.ServerConfig;
import com.omerflex.service.database.dao.IptvDao;
import com.omerflex.service.database.dao.MovieDao;
import com.omerflex.service.database.dao.MovieHistoryDao;
import com.omerflex.service.database.dao.ServerConfigDao;

@Database(entities = {Movie.class, ServerConfig.class, MovieHistory.class, IptvEntry.class}, version = 1, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class OmerFlexDatabase extends RoomDatabase {

    public abstract MovieDao movieDao();
    public abstract ServerConfigDao serverConfigDao();
    public abstract MovieHistoryDao movieHistoryDao();
    public abstract IptvDao iptvDao();

    private static volatile OmerFlexDatabase INSTANCE;

    public static OmerFlexDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (OmerFlexDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    OmerFlexDatabase.class, "omerflex_database")
                            .fallbackToDestructiveMigration()
                            // Add other configurations like .addCallback(), .setQueryExecutor(), etc. if needed
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
