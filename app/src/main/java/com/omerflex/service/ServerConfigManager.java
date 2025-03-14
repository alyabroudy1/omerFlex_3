package com.omerflex.service;

import com.omerflex.entity.ServerConfig;

import java.util.ArrayList;

public class ServerConfigManager {

    static ArrayList<ServerConfig> serversConfigs = new ArrayList<>();

    public static ServerConfig getConfig(String serverId){
        for (ServerConfig config : serversConfigs) {
            if (config.getName().equals(serverId)) {
                return config;
            }
        }
        return null;
    }

    public static ServerConfig updateConfig(ServerConfig newConfig){
        for (int i = 0; i < serversConfigs.size(); i++) {
            ServerConfig config = serversConfigs.get(i);
            if (config.getName().equals(newConfig.getName())) {
                serversConfigs.set(i, newConfig); // Replace the existing config with the new config
                return config; // Return the old config
            }
        }
        return null;
    }

    public static boolean addConfig(ServerConfig newConfig) {
        for (ServerConfig config : serversConfigs) {
            if (config.getName().equals(newConfig.getName())) {
                return false; // Return false if the config with the specified name already exists
            }
        }
        serversConfigs.add(newConfig); // Add the new config
        return true; // Return true indicating the config was added successfully
    }
}
