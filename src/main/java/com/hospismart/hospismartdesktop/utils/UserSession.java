package com.hospismart.hospismartdesktop.utils;

import com.hospismart.hospismartdesktop.models.User;

public class UserSession {

    public static void login(User user) {
        Session.getInstance().setCurrentUser(user);
    }

    public static void logout() {
        Session.getInstance().cleanUserSession();
    }

    public static User getUser() {
        return Session.getInstance().getCurrentUser();
    }

    public static boolean isIsLoggedIn() {
        return Session.getInstance().getCurrentUser() != null;
    }
}