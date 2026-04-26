package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service pour gérer les bilans de santé vocaux
 * Sauvegarde, récupère et gère les bilans générés par l'IA
 */
public class HealthSummaryService {
    private static final String TABLE_NAME = "health_summaries";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Sauvegarde un bilan de santé en base de données
     */
    public boolean saveHealthSummary(User patient, String voiceText, String aiSummary) {
        try {
            System.out.println("[HealthSummary] Sauvegarde du bilan pour: " + patient.getEmail());
            
            // Vérifier si la table existe, sinon la créer
            createTableIfNotExists();
            
            String sql = "INSERT INTO " + TABLE_NAME + 
                " (user_id, voice_text, ai_summary, created_at, updated_at) " +
                " VALUES (?, ?, ?, ?, ?)";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                LocalDateTime now = LocalDateTime.now();
                String timestamp = now.format(DATE_FORMATTER);
                
                pstmt.setInt(1, patient.getId());
                pstmt.setString(2, voiceText);
                pstmt.setString(3, aiSummary);
                pstmt.setString(4, timestamp);
                pstmt.setString(5, timestamp);
                
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("[HealthSummary] ✅ Bilan sauvegardé avec succès");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[HealthSummary] ❌ Erreur sauvegarde bilan: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupère tous les bilans d'un patient
     */
    public List<Map<String, Object>> getPatientSummaries(User patient) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        
        try {
            String sql = "SELECT id, voice_text, ai_summary, created_at FROM " + TABLE_NAME + 
                        " WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, patient.getId());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> summary = new HashMap<>();
                        summary.put("id", rs.getInt("id"));
                        summary.put("voiceText", rs.getString("voice_text"));
                        summary.put("aiSummary", rs.getString("ai_summary"));
                        summary.put("createdAt", rs.getString("created_at"));
                        summaries.add(summary);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[HealthSummary] Erreur récupération bilans: " + e.getMessage());
        }
        
        return summaries;
    }

    /**
     * Récupère le dernier bilan d'un patient
     */
    public Map<String, Object> getLatestSummary(User patient) {
        try {
            String sql = "SELECT id, voice_text, ai_summary, created_at FROM " + TABLE_NAME + 
                        " WHERE user_id = ? ORDER BY created_at DESC LIMIT 1";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, patient.getId());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> summary = new HashMap<>();
                        summary.put("id", rs.getInt("id"));
                        summary.put("voiceText", rs.getString("voice_text"));
                        summary.put("aiSummary", rs.getString("ai_summary"));
                        summary.put("createdAt", rs.getString("created_at"));
                        return summary;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[HealthSummary] Erreur récupération dernier bilan: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Supprime un bilan
     */
    public boolean deleteSummary(int summaryId) {
        try {
            String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, summaryId);
                int rowsAffected = pstmt.executeUpdate();
                
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("[HealthSummary] Erreur suppression bilan: " + e.getMessage());
        }
        return false;
    }

    /**
     * Crée la table si elle n'existe pas
     */
    private void createTableIfNotExists() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "voice_text LONGTEXT NOT NULL, " +
                "ai_summary LONGTEXT NOT NULL, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")";
            
            stmt.executeUpdate(sql);
            System.out.println("[HealthSummary] Table " + TABLE_NAME + " vérifiée/créée");
        } catch (SQLException e) {
            System.err.println("[HealthSummary] Erreur création table: " + e.getMessage());
        }
    }

    /**
     * Obtient une connexion à la base de données
     */
    private Connection getConnection() throws SQLException {
        // Configuration MySQL locale
        String url = "jdbc:mysql://localhost:3306/hospismart?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String password = "";
        
        return DriverManager.getConnection(url, user, password);
    }
}
