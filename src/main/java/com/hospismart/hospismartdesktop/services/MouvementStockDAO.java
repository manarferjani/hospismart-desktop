package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.MouvementStock;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des mouvements de stock
 * Logique transactionnelle : chaque mouvement met à jour automatiquement la quantité du médicament
 */
public class MouvementStockDAO {
    private Connection connection;

    public MouvementStockDAO() {
        this.connection = MyDbConnexion.getInstance().getCnx();
    }

    // LISTER TOUS LES MOUVEMENTS (avec jointure pour récupérer le nom du médicament)
    public List<MouvementStock> findAll() {
        List<MouvementStock> list = new ArrayList<>();
        String sql = "SELECT ms.*, m.nom AS medicament_nom FROM mouvement_stock ms " +
                     "JOIN medicament m ON ms.medicament_id = m.id " +
                     "ORDER BY ms.date_mouvement DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                MouvementStock ms = new MouvementStock();
                ms.setId(rs.getInt("id"));
                ms.setType(rs.getString("type"));
                ms.setQuantite(rs.getInt("quantite"));
                ms.setDateMouvement(rs.getTimestamp("date_mouvement").toLocalDateTime());
                ms.setCommentaire(rs.getString("commentaire"));
                ms.setMedicamentId(rs.getInt("medicament_id"));
                ms.setMedicamentNom(rs.getString("medicament_nom"));
                list.add(ms);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findAll mouvements : " + e.getMessage());
        }
        return list;
    }

    // AJOUTER UN MOUVEMENT + MISE À JOUR ATOMIQUE DU STOCK (transaction)
    public boolean add(MouvementStock m) {
        String insertSql = "INSERT INTO mouvement_stock (type, quantite, date_mouvement, commentaire, medicament_id) VALUES (?, ?, NOW(), ?, ?)";
        String updateSql = m.getType().equalsIgnoreCase("ENTREE")
                ? "UPDATE medicament SET quantite = quantite + ? WHERE id = ?"
                : "UPDATE medicament SET quantite = quantite - ? WHERE id = ?";
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement psInsert = connection.prepareStatement(insertSql)) {
                psInsert.setString(1, m.getType());
                psInsert.setInt(2, m.getQuantite());
                psInsert.setString(3, m.getCommentaire());
                psInsert.setInt(4, m.getMedicamentId());
                psInsert.executeUpdate();
            }

            try (PreparedStatement psUpdate = connection.prepareStatement(updateSql)) {
                psUpdate.setInt(1, m.getQuantite());
                psUpdate.setInt(2, m.getMedicamentId());
                psUpdate.executeUpdate();
            }

            connection.commit();
            System.out.println("✅ Mouvement enregistré et stock mis à jour !");
            return true;

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { /* ignore */ }
            System.err.println("❌ Erreur transaction mouvement : " + e.getMessage());
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ex) { /* ignore */ }
        }
    }

    // SUPPRIMER UN MOUVEMENT
    public boolean delete(int id) {
        String sql = "DELETE FROM mouvement_stock WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Mouvement supprimé !");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression mouvement : " + e.getMessage());
            return false;
        }
    }

    // MOUVEMENTS D'UN MÉDICAMENT PRÉCIS
    public List<MouvementStock> findByMedicamentId(int medicamentId) {
        List<MouvementStock> list = new ArrayList<>();
        String sql = "SELECT ms.*, m.nom AS medicament_nom FROM mouvement_stock ms " +
                     "JOIN medicament m ON ms.medicament_id = m.id " +
                     "WHERE ms.medicament_id = ? ORDER BY ms.date_mouvement DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, medicamentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MouvementStock ms = new MouvementStock();
                ms.setId(rs.getInt("id"));
                ms.setType(rs.getString("type"));
                ms.setQuantite(rs.getInt("quantite"));
                ms.setDateMouvement(rs.getTimestamp("date_mouvement").toLocalDateTime());
                ms.setCommentaire(rs.getString("commentaire"));
                ms.setMedicamentId(rs.getInt("medicament_id"));
                ms.setMedicamentNom(rs.getString("medicament_nom"));
                list.add(ms);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findByMedicament : " + e.getMessage());
        }
        return list;
    }
}
