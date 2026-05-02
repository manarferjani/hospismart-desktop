package com.hospismart.hospismartdesktop;

import com.hospismart.hospismartdesktop.services.UserService;
import com.hospismart.hospismartdesktop.models.User;

public class TestLogin {
    public static void main(String[] args) {
        UserService service = new UserService();
        System.out.println("Testing login...");
        User u = service.login("admin@hospismart.com", "admin");
        if (u != null) {
            System.out.println("Login success!");
        } else {
            System.out.println("Login failed!");
        }
    }
}
