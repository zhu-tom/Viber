package com.tomzhu.viber.models;

public class ChatMessage {
    private String message;
    private String senderUser;
    private String senderUid;
    private String senderName;

    public ChatMessage(String m, String s, String u, String n) {
        message = m;
        senderUser = s;
        senderUid = u;
        senderName = n;
    }

    public String getMessage() {
        return message;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public String getSenderUser() {
        return senderUser;
    }

    public String getSenderName() {
        return senderName;
    }
}
