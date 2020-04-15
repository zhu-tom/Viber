package com.tomzhu.viber.models;

import android.net.Uri;

public class User extends ProtectedResult {
    private String name;

    public User(String aName, String aUID, String aUsername) {
        super(aUsername, aUID, null);
        name = aName;
    }

    public String getName() {
        return name;
    }
}
