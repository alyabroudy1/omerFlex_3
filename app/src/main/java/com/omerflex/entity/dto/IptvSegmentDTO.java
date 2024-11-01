package com.omerflex.entity.dto;

public class IptvSegmentDTO {
    public String id;
    public String name;
    public String tvgName;
    public String groupTitle;
    public String tvgLogo;
    public String url;
    public String fileName;
    public String credentialUrl;

    @Override
    public String toString() {
        return "IptvSegmentDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", tvgName='" + tvgName + '\'' +
                ", groupTitle='" + groupTitle + '\'' +
                ", tvgLogo='" + tvgLogo + '\'' +
                ", url='" + url + '\'' +
                ", fileName='" + fileName + '\'' +
                ", credentialUrl='" + credentialUrl + '\'' +
                '}';
    }
}
