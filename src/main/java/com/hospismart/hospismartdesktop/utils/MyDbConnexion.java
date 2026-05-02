package com.hospismart.hospismartdesktop.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class
MyDbConnexion {

    // On garde les mêmes noms que le prof pour rester cohérente avec ses exemples
    private String USER_NAME = "root";
    private String PASSWORD = "";

    private String URL = "jdbc:mysql://127.0.0.1:3306/hospismart";

    private Connection cnx;

    private static MyDbConnexion instance;

    // Constructeur privé (on ne peut pas faire 'new' en dehors de cette classe)
    private MyDbConnexion() {
        connect();
    }

    private void connect() {
        try {
            cnx = DriverManager.getConnection(URL, USER_NAME, PASSWORD);
            System.out.println("Connexion au projet Hospismart : OK");
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
            cnx = null;
        }
    }

    public static MyDbConnexion getInstance() {
        if (instance == null) {
            instance = new MyDbConnexion();
        }
        return instance;
    }

    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                connect(); // Force la reconnexion si la connexion a été perdue ou a échoué initialement
            }
        } catch (SQLException e) {
            System.err.println("Erreur de vérification de la connexion : " + e.getMessage());
        }
        return cnx;
    }
}
