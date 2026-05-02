package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Diagnostic;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiagnosticService implements CRUD<Diagnostic> {
    private Connection cnx;

    public DiagnosticService() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Diagnostic d) throws SQLException {
        String req = "INSERT INTO `diagnostic` (contenu, probabilite_ia) VALUES (?,?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, d.getContenu());
        ps.setDouble(2, d.getProbabiliteIa());
        ps.executeUpdate();
    }

    @Override
    public void updateOne(Diagnostic d) throws SQLException {
        String req = "UPDATE `diagnostic` SET contenu=?, probabilite_ia=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, d.getContenu());
        ps.setDouble(2, d.getProbabiliteIa());
        ps.setInt(3, d.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("DELETE FROM `diagnostic` WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Diagnostic> findALL() throws SQLException {
        List<Diagnostic> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM `diagnostic`");
        while (rs.next()) {
            Diagnostic d = new Diagnostic();
            d.setId(rs.getInt("id"));
            d.setContenu(rs.getString("contenu"));
            d.setProbabiliteIa(rs.getDouble("probabilite_ia"));
            list.add(d);
        }
        return list;
    }
}