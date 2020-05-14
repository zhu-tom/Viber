package com.tomzhu.viber.models;

import java.util.BitSet;

public class ChatMessage {
    private String message;
    private String senderUser;
    private String senderUid;
    private String senderName;
    private String id;
    private Status status;
    private Type type;

    public enum Status {
        READ,
        SENT,
    }

    public Status getStatus() {
        return status;
    }

    public enum Type {
        TEXT,
        LEAVE,
    }

    public ChatMessage(String i, String m, String s, String u, String n, Type t, Status status1) {
        message = m;
        senderUser = s;
        senderUid = u;
        senderName = n;
        type = t;
        status = status1;
        id = i;
    }

    public String getId() {
        return id;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public Type getType() {
        return type;
    }
}
