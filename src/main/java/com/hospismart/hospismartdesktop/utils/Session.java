package com.hospismart.hospismartdesktop.utils;

import com.hospismart.hospismartdesktop.models.User;

public class Session {
    private static Session instance;
    private User currentUser;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void cleanUserSession() {
        this.currentUser = null;
    }
}
