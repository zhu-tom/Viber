package com.tomzhu.viber.models;

public class ChatMessage {
    private String message;
    private String senderUser;
    private String senderUid;

    public ChatMessage(String m, String s, String u) {
        message = m;
        senderUser = s;
        senderUid = u;
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
}
