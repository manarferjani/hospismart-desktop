package com.hospismart.hospismartdesktop.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton de connexion JDBC à la base de données hospismart
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private final String url = "jdbc:mysql://localhost:3306/hospismart?useSSL=false&serverTimezone=UTC";
    private final String user = "root";
    private final String password = "";

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connexion à la base hospismart réussie !");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }

    public static DatabaseConnection getInstance() {
        try {
            if (instance == null || instance.getConnection().isClosed()) {
                instance = new DatabaseConnection();
            }
        } catch (SQLException e) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
