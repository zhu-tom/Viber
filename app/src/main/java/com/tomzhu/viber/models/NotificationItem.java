package com.tomzhu.viber.models;

public class NotificationItem {
    private String message;
    private long time;
    private String key;

    public NotificationItem(String k, String m, long t) {
        message = m;
        time = t;
        key = k;
    }

    public String getMessage() {
        return message;
    }

    public long getTime() {
        return time;
    }

    public String getKey() {
        return key;
    }
}
