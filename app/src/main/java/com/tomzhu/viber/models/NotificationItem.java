package com.tomzhu.viber.models;

public class NotificationItem {
    private String message;
    private long time;

    public NotificationItem(String m, long t) {
        message = m;
        time = t;
    }

    public String getMessage() {
        return message;
    }

    public long getTime() {
        return time;
    }
}
