package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Inscription;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InscriptionService {

    private final Connection cnx;

    public InscriptionService() {
        this.cnx = MyDbConnexion.getInstance().getCnx();
        createTableIfNotExists();
    }

    // ── Auto-create table ──────────────────────────────

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS inscription (
                id INT AUTO_INCREMENT PRIMARY KEY,
                evenement_id INT NOT NULL,
                nom_participant VARCHAR(255) NOT NULL,
                email_participant VARCHAR(255) NOT NULL,
                telephone_participant VARCHAR(50),
                date_inscription DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE,
                UNIQUE KEY unique_inscription (evenement_id, email_participant)
            )
            """;
        try (Statement stmt = cnx.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("InscriptionService.createTable: " + e.getMessage());
        }
    }

    // ── CREATE ─────────────────────────────────────────

    public boolean inscrire(Inscription i) {
        String sql = "INSERT INTO inscription (evenement_id, nom_participant, email_participant, telephone_participant, date_inscription) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, i.getEvenementId());
            ps.setString(2, i.getNomParticipant());
            ps.setString(3, i.getEmailParticipant());
            ps.setString(4, i.getTelephoneParticipant());
            ps.setTimestamp(5, Timestamp.valueOf(i.getDateInscription()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate")) {
                System.err.println("InscriptionService.inscrire: déjà inscrit.");
            } else {
                System.err.println("InscriptionService.inscrire: " + e.getMessage());
            }
            return false;
        }
    }

    // ── CHECK IF ALREADY REGISTERED ────────────────────

    public boolean isDejaInscrit(int evenementId, String email) {
        String sql = "SELECT COUNT(*) FROM inscription WHERE evenement_id = ? AND email_participant = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            ps.setString(2, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("InscriptionService.isDejaInscrit: " + e.getMessage());
        }
        return false;
    }

    // ── COUNT INSCRIPTIONS FOR AN EVENT ────────────────

    public int countByEvenement(int evenementId) {
        String sql = "SELECT COUNT(*) FROM inscription WHERE evenement_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("InscriptionService.countByEvenement: " + e.getMessage());
        }
        return 0;
    }

    // ── FIND ALL FOR AN EVENT ──────────────────────────

    public List<Inscription> findByEvenement(int evenementId) {
        List<Inscription> list = new ArrayList<>();
        String sql = "SELECT * FROM inscription WHERE evenement_id = ? ORDER BY date_inscription DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Inscription i = new Inscription();
                i.setId(rs.getInt("id"));
                i.setEvenementId(rs.getInt("evenement_id"));
                i.setNomParticipant(rs.getString("nom_participant"));
                i.setEmailParticipant(rs.getString("email_participant"));
                i.setTelephoneParticipant(rs.getString("telephone_participant"));
                Timestamp ts = rs.getTimestamp("date_inscription");
                if (ts != null) i.setDateInscription(ts.toLocalDateTime());
                list.add(i);
            }
        } catch (SQLException e) {
            System.err.println("InscriptionService.findByEvenement: " + e.getMessage());
        }
        return list;
    }

    // ── DELETE ─────────────────────────────────────────

    public boolean desinscrire(int evenementId, String email) {
        String sql = "DELETE FROM inscription WHERE evenement_id = ? AND email_participant = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("InscriptionService.desinscrire: " + e.getMessage());
        }
        return false;
    }
}
