package com.hospismart.hospismartdesktop.utils;

import com.hospismart.hospismartdesktop.models.User;

public class UserSession {
    // Instance unique de l'utilisateur connecté
    private static User instance;

    // Cette méthode sera appelée juste après la connexion (ou ton mock au démarrage)
    public static void login(User user) {
        instance = user;
    }

    public static void logout() {
        instance = null;
    }

    public static User getUser() {
        return instance;
    }

    public static boolean isIsLoggedIn() {
        return instance != null;
    }
}