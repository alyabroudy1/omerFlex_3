package com.omerflex.entity;

import com.omerflex.server.Util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ServerConfig {

    private String name;
    private String label;
    private boolean isActive;
    private  String url;
    private String referer;
    private Date createdAt;
    private String stringCookies;
    private Map<String, String> headers;

    public ServerConfig (){
        headers = new HashMap<>();
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "name='" + name + '\'' +
                ", referer='" + referer + '\'' +
                ", displayName='" + label + '\'' +
                ", url='" + url + '\'' +
//                ", webName='" + webName + '\'' +
                ", isActive=" + isActive +
//                ", description='" + description + '\'' +
                ", date='" + createdAt + '\'' +
                ", headers=" + headers +
                ", stringCookies='" + stringCookies + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getStringCookies() {
        return stringCookies;
    }

    public void setStringCookies(String stringCookies) {
        if (stringCookies != null && !stringCookies.isEmpty()){
            this.stringCookies = stringCookies;
//            this.mappedCookies = Util.getMapCookies(stringCookies);
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
        return Util.getMapCookies(stringCookies);
//        try {
//            return Util.convertJsonToHashMap(stringCookies);
//        } catch (JSONException e) {
//            return Util.getMapCookies(stringCookies);
//        }
    }
}
