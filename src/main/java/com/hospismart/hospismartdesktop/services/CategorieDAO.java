package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Categorie;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO CRUD complet pour la table categorie
 */
public class CategorieDAO {
    private Connection connection;

    public CategorieDAO() {
        this.connection = MyDbConnexion.getInstance().getCnx();
    }

    public List<Categorie> findAll() {
        List<Categorie> list = new ArrayList<>();
        String sql = "SELECT * FROM categorie ORDER BY nom";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Categorie c = new Categorie();
                c.setId(rs.getInt("id"));
                c.setNom(rs.getString("nom"));
                c.setDescription(rs.getString("description"));
                list.add(c);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur findAll catégories : " + e.getMessage());
        }
        return list;
    }

    public boolean add(Categorie c) {
        String sql = "INSERT INTO categorie (nom, description) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getDescription());
            ps.executeUpdate();
            System.out.println("✅ Catégorie ajoutée !");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur add catégorie : " + e.getMessage());
            return false;
        }
    }

    public boolean update(Categorie c) {
        String sql = "UPDATE categorie SET nom=?, description=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getId());
            ps.executeUpdate();
            System.out.println("✅ Catégorie modifiée !");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur update catégorie : " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM categorie WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Catégorie supprimée !");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur delete catégorie : " + e.getMessage());
            return false;
        }
    }
    public boolean existsByNom(String nom, int excludeId) {
        String sql = "SELECT COUNT(*) FROM categorie WHERE LOWER(nom) = LOWER(?) AND id != ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nom.trim());
            ps.setInt(2, excludeId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur existsByNom catégorie : " + e.getMessage());
            return false;
        }
    }
}
