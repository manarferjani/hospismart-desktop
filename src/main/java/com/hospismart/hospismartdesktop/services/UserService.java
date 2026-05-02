package com.hospismart.hospismartdesktop.services;


import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements CRUD<User> {

    private Connection cnx;

    public UserService() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(User u) throws SQLException {
        String req = "INSERT INTO `user` (`nom`, `prenom`, `email`, `telephone`, `password`, `type`, `image`, " +
                "`date_naissance`, `genre`, `groupe_sanguin`, `adresse`, `specialite`, `matricule`, `service_id`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getTelephone());
        ps.setString(5, u.getPassword());
        ps.setString(6, u.getType());
        ps.setString(7, u.getImage());

        // Gestion de la date (LocalDate -> Date SQL)
        ps.setDate(8, u.getDateNaissance() != null ? Date.valueOf(u.getDateNaissance()) : null);

        ps.setString(9, u.getGenre());
        ps.setString(10, u.getGroupeSanguin());
        ps.setString(11, u.getAdresse());
        ps.setString(12, u.getSpecialite());
        ps.setString(13, u.getMatricule());

        // Gestion du service_id (on met 0 ou null si ce n'est pas un médecin)
        if (u.getServiceId() > 0) ps.setInt(14, u.getServiceId());
        else ps.setNull(14, Types.INTEGER);

        ps.executeUpdate();
    }

    @Override
    public void updateOne(User u) throws SQLException {
        String req = "UPDATE `user` SET `nom`=?, `prenom`=?, `email`=?, `telephone`=?, `type`=?, `specialite`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getTelephone());
        ps.setString(5, u.getType());
        ps.setString(6, u.getSpecialite());
        ps.setInt(7, u.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(int id) throws SQLException {
        String req = "DELETE FROM `user` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<User> findALL() throws SQLException {
        List<User> list = new ArrayList<>();
        String req = "SELECT * FROM `user`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            User u = new User();
            u.setId(rs.getInt("id"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setTelephone(rs.getString("telephone"));
            u.setType(rs.getString("type"));

            // Conversion Date SQL -> LocalDate Java
            Date d = rs.getDate("date_naissance");
            if (d != null) u.setDateNaissance(d.toLocalDate());

            u.setSpecialite(rs.getString("specialite"));
            u.setServiceId(rs.getInt("service_id"));

            list.add(u);
        }
        return list;
    }

    public List<User> findMedecinsByService(int serviceId) throws SQLException {
        List<User> list = new ArrayList<>();
        // On filtre par type 'medecin' (ou le type que tu as défini) et par service_id
        String req = "SELECT * FROM `user` WHERE `type` = 'medecin' AND `service_entity_id` = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, serviceId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            User u = new User();
            u.setId(rs.getInt("id"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setImage(rs.getString("image"));
            u.setSpecialite(rs.getString("specialite"));
            u.setServiceId(rs.getInt("service_entity_id"));
            // Ajoute les autres champs dont tu as besoin pour la carte
            list.add(u);
        }
        return list;
    }

    public List<User> findPatientsByMedecin(int medecinId) throws SQLException {
        List<User> list = new ArrayList<>();
        // Select distinct patients who have rendez-vous with the doctor
        String req = "SELECT DISTINCT u.* FROM `user` u " +
                     "JOIN `rendez_vous` r ON u.id = r.patient_id " +
                     "WHERE r.medecin_id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, medecinId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            User u = new User();
            u.setId(rs.getInt("id"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setTelephone(rs.getString("telephone"));
            u.setType(rs.getString("type"));

            Date d = rs.getDate("date_naissance");
            if (d != null) u.setDateNaissance(d.toLocalDate());

            u.setGenre(rs.getString("genre"));
            u.setGroupeSanguin(rs.getString("groupe_sanguin"));
            u.setAdresse(rs.getString("adresse"));

            list.add(u);
        }
        return list;
    }
    public User findById(int id) throws SQLException {
        String req = "SELECT * FROM `user` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            User u = new User();
            u.setId(rs.getInt("id"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setTelephone(rs.getString("telephone"));
            u.setType(rs.getString("type"));
            u.setGenre(rs.getString("genre"));
            u.setGroupeSanguin(rs.getString("groupe_sanguin"));
            u.setAdresse(rs.getString("adresse"));
            u.setSpecialite(rs.getString("specialite"));
            u.setMatricule(rs.getString("matricule"));
            
            Date d = rs.getDate("date_naissance");
            if (d != null) u.setDateNaissance(d.toLocalDate());
            
            return u;
        }
        return null;
    }
}
