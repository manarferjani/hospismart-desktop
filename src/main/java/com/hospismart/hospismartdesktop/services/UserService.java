package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements CRUD<User> {
    private Connection cnx;
    public static String lastLoginError = "";

    public UserService() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    // ==================== MÉTHODES CRUD ====================

    @Override
    public void insertOne(User u) throws SQLException {
        // Initialisation sécurisée des colonnes si manquantes
        checkAndPrepareTable();

        String req = "INSERT INTO `user` (`nom`, `prenom`, `email`, `telephone`, `password`, `type`, `image`, " +
                "`date_naissance`, `genre`, `groupe_sanguin`, `adresse`, `specialite`, `matricule`, `service_id`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getTelephone());

        // Hachage sécurisé
        String hashedPass = BCrypt.hashpw(u.getPassword(), BCrypt.gensalt(13));
        ps.setString(5, hashedPass);

        ps.setString(6, u.getType());
        ps.setString(7, u.getImage());
        ps.setDate(8, u.getDateNaissance() != null ? Date.valueOf(u.getDateNaissance()) : null);
        ps.setString(9, u.getGenre());
        ps.setString(10, u.getGroupeSanguin());
        ps.setString(11, u.getAdresse());
        ps.setString(12, u.getSpecialite());
        ps.setString(13, u.getMatricule());

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
        try {
            try (Statement st = cnx.createStatement()) { st.execute("SET FOREIGN_KEY_CHECKS=0"); }
            String req = "DELETE FROM `user` WHERE id = ?";
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
            try (Statement st = cnx.createStatement()) { st.execute("SET FOREIGN_KEY_CHECKS=1"); }
        } catch (SQLException e) {
            try (Statement st = cnx.createStatement()) { st.execute("SET FOREIGN_KEY_CHECKS=1"); }
            throw e;
        }
    }

    @Override
    public List<User> findALL() throws SQLException {
        List<User> list = new ArrayList<>();
        String req = "SELECT * FROM `user`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            list.add(mapResultSetToUser(rs));
        }
        return list;
    }

    // ==================== LOGIQUE DE CONNEXION ====================

    public User login(String email, String password) {
        lastLoginError = "";
        String query = "SELECT * FROM user WHERE email=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String dbHash = rs.getString("password");
                boolean passwordMatch = false;

                if (dbHash != null && (dbHash.startsWith("$2y$") || dbHash.startsWith("$2a$"))) {
                    String compatibleHash = dbHash.replace("$2y$", "$2a$");
                    passwordMatch = BCrypt.checkpw(password, compatibleHash);
                } else {
                    passwordMatch = password.equals(dbHash);
                }

                if (passwordMatch) {
                    return mapResultSetToUser(rs);
                } else {
                    lastLoginError = "Mot de passe incorrect.";
                }
            } else {
                lastLoginError = "Utilisateur non trouvé.";
            }
        } catch (SQLException e) {
            lastLoginError = "Erreur base de données : " + e.getMessage();
        }
        return null;
    }

    // ==================== RECHERCHES SPÉCIFIQUES ====================

    public List<User> findMedecinsByService(int serviceId) throws SQLException {
        List<User> list = new ArrayList<>();
        String req = "SELECT * FROM `user` WHERE `type` = 'medecin' AND `service_id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, serviceId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) { list.add(mapResultSetToUser(rs)); }
        return list;
    }

    public User findByEmail(String email) {
        try {
            String query = "SELECT * FROM user WHERE email=?";
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return mapResultSetToUser(rs);
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return null;
    }

    // ==================== UTILITAIRES ====================

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setTelephone(rs.getString("telephone"));
        u.setType(rs.getString("type"));
        u.setImage(rs.getString("image"));
        u.setSpecialite(rs.getString("specialite"));

        Date d = rs.getDate("date_naissance");
        if (d != null) u.setDateNaissance(d.toLocalDate());

        try { u.setActive(rs.getBoolean("is_active")); } catch (SQLException ignored) {}
        return u;
    }

    private void checkAndPrepareTable() {
        String[] columns = {
                "ALTER TABLE user ADD COLUMN is_active BOOLEAN DEFAULT TRUE",
                "ALTER TABLE user ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE",
                "ALTER TABLE user ADD COLUMN two_factor_secret VARCHAR(255) NULL"
        };
        for (String sql : columns) {
            try { cnx.createStatement().execute(sql); } catch (SQLException ignored) {}
        }
    }
}