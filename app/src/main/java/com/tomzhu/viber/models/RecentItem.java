package com.tomzhu.viber.models;

public class RecentItem {
    private ChatType type;
    private String uid;
    private String chatId;
    private long datetime;

    public enum ChatType {
        VIDEO,
        TEXT
    }

    public RecentItem(ChatType type, String uid, String chatId, long datetime) {
        this.type = type;
        this.uid = uid;
        this.chatId = chatId;
        this.datetime = datetime;
    }

    public String getUid() {
        return uid;
    }

    public long getDatetime() {
        return datetime;
    }

    public String getChatId() {
        return chatId;
    }

    public ChatType getName() {
        return type;
    }

    public ChatType getType() {
        return type;
    }
}
