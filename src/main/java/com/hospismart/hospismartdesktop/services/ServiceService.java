package com.hospismart.hospismartdesktop.services;


import com.hospismart.hospismartdesktop.models.Service;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceService implements CRUD<Service> {

    private Connection cnx;

    public ServiceService() {
        // On récupère la connexion via ton Singleton
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Service s) throws SQLException {
        String req = "INSERT INTO `service` (`nom`, `description`, `icon`, `image`) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, s.getNom());
        ps.setString(2, s.getDescription());
        ps.setString(3, s.getIcon());
        ps.setString(4, s.getImage());
        ps.executeUpdate();
    }

    @Override
    public void updateOne(Service s) throws SQLException {
        String req = "UPDATE `service` SET `nom`=?, `description`=?, `icon`=?, `image`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, s.getNom());
        ps.setString(2, s.getDescription());
        ps.setString(3, s.getIcon());
        ps.setString(4, s.getImage());
        ps.setInt(5, s.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(int id) throws SQLException {
        String req = "DELETE FROM `service` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Service> findALL() throws SQLException {
        List<Service> services = new ArrayList<>();
        String req = "SELECT * FROM `service`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Service s = new Service();
            s.setId(rs.getInt("id"));
            s.setNom(rs.getString("nom"));
            s.setDescription(rs.getString("description"));
            s.setIcon(rs.getString("icon"));
            s.setImage(rs.getString("image"));

            services.add(s);
        }
        return services;
    }
}