package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Medicament;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO CRUD complet pour la table medicament
 */
public class MedicamentDAO {
    private Connection connection;

    public MedicamentDAO() {
        this.connection = MyDbConnexion.getInstance().getCnx();
    }

    public List<Medicament> findAll() {
        List<Medicament> list = new ArrayList<>();
        String sql = "SELECT m.*, c.nom AS categorie_nom " +
                     "FROM medicament m " +
                     "LEFT JOIN categorie c ON m.categorie_id = c.id " +
                     "ORDER BY m.nom";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findAll médicaments : " + e.getMessage());
        }
        return list;
    }

    public Medicament findById(int id) {
        String sql = "SELECT * FROM medicament WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            System.err.println("❌ Erreur findById : " + e.getMessage());
        }
        return null;
    }

    public boolean add(Medicament m) {
        String sql = "INSERT INTO medicament (nom, quantite, seuil_alerte, prix_unitaire, date_peremption, categorie_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, m.getNom());
            ps.setInt(2, m.getQuantite());
            ps.setInt(3, m.getSeuilAlerte());
            ps.setDouble(4, m.getPrixUnitaire());
            ps.setDate(5, m.getDatePeremption() != null ? Date.valueOf(m.getDatePeremption()) : null);
            if (m.getCategorieId() > 0) ps.setInt(6, m.getCategorieId());
            else ps.setNull(6, Types.INTEGER);
            ps.executeUpdate();
            System.out.println("✅ Médicament ajouté !");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur add médicament : " + e.getMessage());
            return false;
        }
    }

    public boolean update(Medicament m) {
        String sql = "UPDATE medicament SET nom=?, quantite=?, seuil_alerte=?, prix_unitaire=?, date_peremption=?, categorie_id=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, m.getNom());
            ps.setInt(2, m.getQuantite());
            ps.setInt(3, m.getSeuilAlerte());
            ps.setDouble(4, m.getPrixUnitaire());
            ps.setDate(5, m.getDatePeremption() != null ? Date.valueOf(m.getDatePeremption()) : null);
            if (m.getCategorieId() > 0) ps.setInt(6, m.getCategorieId());
            else ps.setNull(6, Types.INTEGER);
            ps.setInt(7, m.getId());
            ps.executeUpdate();
            System.out.println("✅ Médicament modifié !");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur update médicament : " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        // Supprimer d'abord les mouvements liés (contrainte FK)
        String deleteMouvements = "DELETE FROM mouvement_stock WHERE medicament_id = ?";
        String deleteMed        = "DELETE FROM medicament WHERE id = ?";
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps1 = connection.prepareStatement(deleteMouvements)) {
                ps1.setInt(1, id);
                int nb = ps1.executeUpdate();
                if (nb > 0)
                    System.out.println("🔗 " + nb + " mouvement(s) liés supprimés en cascade.");
            }

            try (PreparedStatement ps2 = connection.prepareStatement(deleteMed)) {
                ps2.setInt(1, id);
                ps2.executeUpdate();
            }

            connection.commit();
            System.out.println("✅ Médicament supprimé !");
            return true;

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            System.err.println("❌ Erreur delete médicament : " + e.getMessage());
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    /**
     * Vérifie si un médicament avec ce nom existe déjà.
     * En mode édition, on exclut l'id courant pour permettre de garder le même nom.
     * @param nom   le nom à vérifier
     * @param excludeId  l'id à exclure (0 si nouveau)
     */
    public boolean existsByNom(String nom, int excludeId) {
        String sql = "SELECT COUNT(*) FROM medicament WHERE LOWER(nom) = LOWER(?) AND id != ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nom.trim());
            ps.setInt(2, excludeId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur existsByNom : " + e.getMessage());
            return false;
        }
    }

    // Médicaments dont le stock est en dessous du seuil d'alerte
    public List<Medicament> findEnAlerte() {
        List<Medicament> list = new ArrayList<>();
        String sql = "SELECT * FROM medicament WHERE quantite <= seuil_alerte ORDER BY nom";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("❌ Erreur findEnAlerte : " + e.getMessage());
        }
        return list;
    }

    private Medicament mapResultSet(ResultSet rs) throws SQLException {
        Medicament m = new Medicament();
        m.setId(rs.getInt("id"));
        m.setNom(rs.getString("nom"));
        m.setQuantite(rs.getInt("quantite"));
        m.setSeuilAlerte(rs.getInt("seuil_alerte"));
        m.setPrixUnitaire(rs.getDouble("prix_unitaire"));
        Date d = rs.getDate("date_peremption");
        if (d != null) m.setDatePeremption(d.toLocalDate());
        m.setCategorieId(rs.getInt("categorie_id"));
        m.setImageFilename(rs.getString("image_filename"));
        // Récupérer le nom de la catégorie depuis le JOIN
        try { m.setCategorieNom(rs.getString("categorie_nom")); } catch (SQLException ignored) {}
        return m;
    }
}
