package com.tomzhu.viber.models;

public class ProtectedResult {
    private String photoUrl;
    private String username;
    private String uid;

    public ProtectedResult(String username, String uid, String url) {
        this.username = username;
        this.uid = uid;
        this.photoUrl = url;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }
}
