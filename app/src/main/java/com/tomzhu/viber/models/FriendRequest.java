package com.tomzhu.viber.models;

public class FriendRequest extends NotificationItem {
    private String uid;
    private String photoUrl;

    public FriendRequest(String u, String url, String message, long time) {
        super(message, time);
        uid = u;
        photoUrl = url;
    }

    public String getUid() {
        return uid;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
