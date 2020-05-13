package com.tomzhu.viber.models;


public class FriendItem {
    private String uid;
    private String chatId;

    public FriendItem(String u, String c) {
        uid = u;
        chatId = c;
    }

    public String getUid() {
        return uid;
    }

    public String getChatId() {
        return chatId;
    }
}
