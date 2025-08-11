package com.omerflex.entity;

public class SyncData {
    private long timestamp;
    private String deviceMac;
    private int resultCount;

    // Required empty constructor for Firebase
    public SyncData() {
    }

    public SyncData(long timestamp, String deviceMac, int resultCount) {
        this.timestamp = timestamp;
        this.deviceMac = deviceMac;
        this.resultCount = resultCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }
}
