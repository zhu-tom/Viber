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

    public RecentItem(String type, String uid, String chatId, long datetime) {
        if (type.equals("video")) {
            this.type = ChatType.VIDEO;
        } else if (type.equals("text")) {
            this.type = ChatType.TEXT;
        }
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
}
