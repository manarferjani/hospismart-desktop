package com.hospismart.hospismartdesktop.tests;


import java.sql.*;

public class DbValidator {
    public static void checkTableStructure(String tableName) {
        // Paramètres de connexion (A vérifier avec ta config locale)
        String url = "jdbc:mysql://localhost:3306/hospismart";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData metaData = conn.getMetaData();

            // On demande les colonnes à la base de données
            ResultSet columns = metaData.getColumns(null, null, tableName, null);

            System.out.println("\n========================================================");
            System.out.println("   STRUCTURE DE LA TABLE : " + tableName.toUpperCase());
            System.out.println("========================================================");

            boolean tableExists = false;
            while (columns.next()) {
                tableExists = true;
                String columnName = columns.getString("COLUMN_NAME");
                String typeName = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                String isNullable = columns.getString("IS_NULLABLE");

                System.out.printf("| %-22s | %-10s | %-5d | Null: %-3s |%n",
                        columnName, typeName, columnSize, isNullable);
            }

            if (!tableExists) {
                System.err.println("ERREUR : La table '" + tableName + "' n'existe pas.");
                System.out.println("Vérifiez le nom (ex: consultation au lieu de consultations)");
            }
            System.out.println("========================================================\n");

        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Lance la vérification pour tes tables principales
        checkTableStructure("consultation");
        checkTableStructure("categorie");
        checkTableStructure("campagne");
        checkTableStructure("audit_log");
    }
}