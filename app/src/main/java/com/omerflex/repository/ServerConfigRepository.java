package com.omerflex.repository;

import androidx.lifecycle.LiveData;
import com.omerflex.entity.ServerConfig;
import com.omerflex.service.database.dao.ServerConfigDao;
import com.omerflex.service.concurrent.ThreadPoolManager;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ServerConfigRepository {
    private final ServerConfigDao serverConfigDao;
    private final Executor diskIoExecutor;

    @Inject
    public ServerConfigRepository(ServerConfigDao serverConfigDao, ThreadPoolManager threadPoolManager) {
        this.serverConfigDao = serverConfigDao;
        this.diskIoExecutor = threadPoolManager.getDiskExecutor();
    }

    public LiveData<List<ServerConfig>> getAllServerConfigs() {
        return serverConfigDao.getAllServerConfigsLiveData();
    }

    public LiveData<ServerConfig> getServerConfigByName(String name) {
        return serverConfigDao.getServerConfigByNameLiveData(name);
    }

    public void insert(ServerConfig serverConfig) {
        diskIoExecutor.execute(() -> {
            serverConfigDao.insert(serverConfig);
        });
    }

    public void update(ServerConfig serverConfig) {
        diskIoExecutor.execute(() -> {
            serverConfigDao.update(serverConfig);
        });
    }
}
