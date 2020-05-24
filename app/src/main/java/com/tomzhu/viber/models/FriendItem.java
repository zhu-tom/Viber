package com.tomzhu.viber.models;


public class FriendItem {
    private String uid;
    private String chatId;
    private String lastMessage;

    public FriendItem(String u, String c, String l) {
        uid = u;
        chatId = c;
        lastMessage = l;
    }

    public String getUid() {
        return uid;
    }

    public String getChatId() {
        return chatId;
    }

    public String getLastMessage() {
        return lastMessage;
    }
}
