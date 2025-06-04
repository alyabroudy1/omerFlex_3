package com.omerflex.di;

import android.content.Context;
import androidx.room.Room;
import com.omerflex.service.database.OmerFlexDatabase;
import com.omerflex.service.database.dao.MovieDao;
import com.omerflex.service.database.dao.ServerConfigDao;
import com.omerflex.service.database.dao.MovieHistoryDao;
import com.omerflex.service.database.dao.IptvDao;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public OmerFlexDatabase provideOmerFlexDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context.getApplicationContext(),
                OmerFlexDatabase.class,
                "omerflex_database"
        ).fallbackToDestructiveMigration().build(); // Match existing setup in OmerFlexDatabase
    }

    @Provides
    public MovieDao provideMovieDao(OmerFlexDatabase database) {
        return database.movieDao();
    }

    @Provides
    public ServerConfigDao provideServerConfigDao(OmerFlexDatabase database) {
        return database.serverConfigDao();
    }

    @Provides
    public MovieHistoryDao provideMovieHistoryDao(OmerFlexDatabase database) {
        return database.movieHistoryDao();
    }

    @Provides
    public IptvDao provideIptvDao(OmerFlexDatabase database) {
        return database.iptvDao();
    }
}
