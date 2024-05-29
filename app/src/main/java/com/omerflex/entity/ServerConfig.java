package com.omerflex.entity;

import com.omerflex.server.Util;

import java.util.HashMap;
import java.util.Map;

public class ServerConfig {

    private String name;
    private String displayName;
    private  String url;
    private  String webName;
    private boolean isActive;
    private String description;
    private String date;
    private String stringCookies;
    private String referer;
    private Map<String, String> headers;
    private Map<String, String> mappedCookies;

    public ServerConfig (){
        headers = new HashMap<>();
        mappedCookies = new HashMap<>();
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "name='" + name + '\'' +
                ", referer='" + referer + '\'' +
                ", displayName='" + displayName + '\'' +
                ", url='" + url + '\'' +
                ", webName='" + webName + '\'' +
                ", isActive=" + isActive +
                ", description='" + description + '\'' +
                ", date='" + date + '\'' +
                ", headers=" + headers +
                ", stringCookies='" + stringCookies + '\'' +
                ", mappedCookies=" + mappedCookies +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getWebName() {
        return webName;
    }

    public void setWebName(String webName) {
        this.webName = webName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStringCookies() {
        return stringCookies;
    }

    public void setStringCookies(String stringCookies) {
        if (stringCookies != null && !stringCookies.isEmpty()){
            this.stringCookies = stringCookies;
            this.mappedCookies = Util.getMapCookies(stringCookies);
        }
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getMappedCookies() {
        return mappedCookies;
    }

    public void setMappedCookies(Map<String, String> mappedCookies) {
        this.mappedCookies = mappedCookies;
    }
}
