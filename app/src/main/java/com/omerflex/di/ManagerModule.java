package com.omerflex.di;

import android.content.Context;
import com.omerflex.service.concurrent.ThreadPoolManager;
import com.omerflex.service.config.ConfigManager;
import com.omerflex.service.database.DatabaseManager;
import com.omerflex.service.database.dao.MovieDao;
import com.omerflex.service.database.dao.ServerConfigDao;
import com.omerflex.service.database.dao.MovieHistoryDao;
import com.omerflex.service.database.dao.IptvDao;
import com.omerflex.service.network.HttpClientManager;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class ManagerModule {

    @Provides
    @Singleton
    public ConfigManager provideConfigManager(@ApplicationContext Context context) {
        // Assuming ConfigManager.getInstance(context) is how it's obtained.
        // If ConfigManager needs to be a Hilt-managed class itself, it would require its own @Inject constructor.
        return ConfigManager.getInstance(context);
    }

    @Provides
    @Singleton
    public ThreadPoolManager provideThreadPoolManager() {
        // Assuming ThreadPoolManager.getInstance() is a well-behaved singleton.
        return ThreadPoolManager.getInstance();
    }

    @Provides
    @Singleton
    public HttpClientManager provideHttpClientManager(@ApplicationContext Context context, ConfigManager configManager) {
        // Assuming HttpClientManager.getInstance(context) is appropriate.
        // If it can take ConfigManager in its constructor or an init method, Hilt could provide it.
        // For now, sticking to its existing getInstance structure.
        // The provided configManager can be used if HttpClientManager is refactored.
        return HttpClientManager.getInstance(context);
    }

    @Provides
    @Singleton
    public DatabaseManager provideDatabaseManager(
            @ApplicationContext Context context,
            ThreadPoolManager threadPoolManager,
            ConfigManager configManager,
            MovieDao movieDao,
            ServerConfigDao serverConfigDao,
            MovieHistoryDao movieHistoryDao,
            IptvDao iptvDao
    ) {
        // This constructor signature for DatabaseManager needs to be created in DatabaseManager.java
        return new DatabaseManager(context, threadPoolManager, configManager, movieDao, serverConfigDao, movieHistoryDao, iptvDao);
    }
}
