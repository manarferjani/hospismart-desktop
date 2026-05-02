package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Categorie;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieService implements CRUD<Categorie> {
    private Connection cnx;

    public CategorieService() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Categorie c) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("INSERT INTO `categorie` (nom, description) VALUES (?,?)");
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.executeUpdate();
    }

    @Override
    public void updateOne(Categorie c) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("UPDATE `categorie` SET nom=?, description=? WHERE id=?");
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.setInt(3, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("DELETE FROM `categorie` WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Categorie> findALL() throws SQLException {
        List<Categorie> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM `categorie`");
        while (rs.next()) {
            Categorie c = new Categorie();
            c.setId(rs.getInt("id"));
            c.setNom(rs.getString("nom"));
            c.setDescription(rs.getString("description"));
            list.add(c);
        }
        return list;
    }
}