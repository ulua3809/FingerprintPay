package com.surcumference.fingerprint.bean;

public class UpdateInfo {
    public String version;
    public String content;
    public String pageUrl;
    public String url;
    public int size;

    public UpdateInfo(String version, String content, String pageUrl, String url, int size) {
        this.version = version;
        this.content = content;
        this.pageUrl = pageUrl;
        this.url = url;
        this.size = size;
    }

    public UpdateInfo() {
    }
}
