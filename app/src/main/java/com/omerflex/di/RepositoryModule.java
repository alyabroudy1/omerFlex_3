package com.omerflex.di;

import com.omerflex.repository.MovieRepository;
import com.omerflex.repository.ServerConfigRepository;
import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.database.dao.IptvDao;
import com.omerflex.service.database.dao.MovieDao;
import com.omerflex.service.database.dao.MovieHistoryDao;
import com.omerflex.service.database.dao.ServerConfigDao;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides
    @Singleton
    public ServerConfigRepository provideServerConfigRepository(ServerConfigDao serverConfigDao, ThreadPoolManager threadPoolManager) {
        return new ServerConfigRepository(serverConfigDao, threadPoolManager);
    }

    @Provides
    @Singleton
    public MovieRepository provideMovieRepository(MovieDao movieDao, MovieHistoryDao movieHistoryDao, IptvDao iptvDao,
                                                ThreadPoolManager threadPoolManager) {
        return new MovieRepository(movieDao, movieHistoryDao, iptvDao, threadPoolManager);
    }
}
