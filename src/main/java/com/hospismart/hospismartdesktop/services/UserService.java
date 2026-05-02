package com.hospismart.hospismartdesktop.services;


import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements CRUD<User> {

public class UserService {
    private Connection cnx;
    public static String lastLoginError = "";

    public UserService() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(User u) throws SQLException {
        String req = "INSERT INTO `user` (`nom`, `prenom`, `email`, `telephone`, `password`, `type`, `image`, " +
                "`date_naissance`, `genre`, `groupe_sanguin`, `adresse`, `specialite`, `matricule`, `service_id`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Création automatique des colonnes si elles n'existent pas
        try {
            cnx.createStatement().execute("ALTER TABLE user ADD COLUMN is_active BOOLEAN DEFAULT TRUE");
        } catch (SQLException ignored) {
            // L'erreur est levée si la colonne existe déjà
        }

        // Ajouter les colonnes 2FA
        try {
            cnx.createStatement().execute("ALTER TABLE user ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE");
        } catch (SQLException ignored) {
            // Colonne existe déjà
        }

        try {
            cnx.createStatement().execute("ALTER TABLE user ADD COLUMN two_factor_secret VARCHAR(255) NULL");
        } catch (SQLException ignored) {
            // Colonne existe déjà
        }
    }

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getTelephone());
        ps.setString(5, u.getPassword());
        ps.setString(6, u.getType());
        ps.setString(7, u.getImage());
    public boolean ajouter(User user) throws SQLException {
        String query = "INSERT INTO user (nom, roles, password, prenom, email, telephone) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(query);
        pst.setString(1, user.getNom());
        // Default to "[\"ROLE_PATIENT\"]" to satisfy JSON constraint and business rules
        String t = user.getType();
        String roleStr = (t != null && !t.isEmpty()) ? (t.startsWith("[") ? t : "[\"" + t + "\"]") : "[\"ROLE_PATIENT\"]";
        pst.setString(2, roleStr);
        String hashedPass = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(13));
        pst.setString(3, hashedPass);
        pst.setString(4, user.getPrenom());
        pst.setString(5, user.getEmail());
        pst.setString(6, user.getTelephone());
        int rows = pst.executeUpdate();
        return rows > 0;
    }

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
    public boolean modifier(User user) {
        String query = "UPDATE user SET nom=?, roles=?, password=?, prenom=?, email=?, telephone=? WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, user.getNom());
            String t = user.getType();
            String roleStr = (t != null && !t.isEmpty()) ? (t.startsWith("[") ? t : "[\"" + t + "\"]") : "[\"ROLE_PATIENT\"]";
            pst.setString(2, roleStr);

            // Si on veut modifier le mot de passe, on doit aussi le hacher
            // MAIS si c'est déjà un hash (commence par $2y$ ou $2a$), on ne le rehache pas !
            String pass = user.getPassword();
            String finalPass = pass;
            if (pass != null && !pass.startsWith("$2y$") && !pass.startsWith("$2a$")) {
                finalPass = BCrypt.hashpw(pass, BCrypt.gensalt(13));
            }
            pst.setString(3, finalPass);

            pst.setString(4, user.getPrenom());
            pst.setString(5, user.getEmail());
            pst.setString(6, user.getTelephone());
            pst.setInt(7, user.getId());
            int rows = pst.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification (DB): " + e.getMessage());
            return false;
        }
    }

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
    public boolean supprimer(int id) {
        try {
            // Désactiver temporairement la vérification des clés étrangères pour forcer la suppression
            try (java.sql.Statement st = cnx.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS=0");
            }

            String query = "DELETE FROM user WHERE id=?";
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, id);
            int rows = pst.executeUpdate();

            // Réactiver impérativement la vérification
            try (java.sql.Statement st = cnx.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS=1");
            }

            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression (DB): " + e.getMessage());
            try {
                try (java.sql.Statement st = cnx.createStatement()) {
                    st.execute("SET FOREIGN_KEY_CHECKS=1");
                }
            } catch (SQLException ignored) {}
            return false;
        }
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
    public List<User> afficher() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM user";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                String typeStr = "ROLE_PATIENT";
                try { typeStr = rs.getString("roles"); } catch(SQLException ignored) {}
                if (typeStr == null || typeStr.isEmpty()) typeStr = "[\"ROLE_PATIENT\"]";

                User user = new User(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        typeStr
                );
                user.setTelephone(rs.getString("telephone"));
                user.setPassword(rs.getString("password"));
                // is_active avec fallback si la colonne n'est pas encore créée
                boolean isActive = true;
                try { isActive = rs.getBoolean("is_active"); } catch(SQLException ignored) {}
                user.setActive(isActive);

                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'affichage des utilisateurs : " + e.getMessage());
        }
        return users;
    }

    public User login(String email, String password) {
        lastLoginError = "";
        String query = "SELECT * FROM user WHERE email=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();

            if (rs.next()) {
                String dbHash = rs.getString("password");
                boolean passwordMatch = false;

                // Handle both straight plaintext (since user successfully tested with it) and BCrypt hashes
                if (dbHash != null && (dbHash.startsWith("$2y$") || dbHash.startsWith("$2a$"))) {
                    // jBcrypt ne comprend que le préfixe $2a$ car $2y$ est spécifique à PHP
                    String compatibleHash = dbHash;
                    if (dbHash.startsWith("$2y$")) {
                        compatibleHash = "$2a$" + dbHash.substring(4);
                    }
                    try {
                        passwordMatch = BCrypt.checkpw(password, compatibleHash);
                    } catch (Throwable ex) {
                        lastLoginError = "BCrypt Parse Exception: " + ex.getMessage();
                        System.err.println("Erreur BCrypt: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                } else if (dbHash != null && dbHash.equals(password)) {
                    passwordMatch = true;
                }

    public List<User> findPatientsByMedecin(int medecinId) throws SQLException {
        List<User> list = new ArrayList<>();
        // Select distinct patients who have rendez-vous with the doctor
        String req = "SELECT DISTINCT u.* FROM `user` u " +
                     "JOIN `rendez_vous` r ON u.id = r.patient_id " +
                     "WHERE r.medecin_id = ?";
                if (passwordMatch) {
                    String typeStr = "ROLE_PATIENT";
                    try { typeStr = rs.getString("roles"); } catch(SQLException ignored) {}
                    if (typeStr == null || typeStr.isEmpty()) typeStr = "[\"ROLE_PATIENT\"]";

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
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            typeStr
                    );
                    user.setTelephone(rs.getString("telephone"));
                    user.setPassword(dbHash);

                    boolean active = true;
                    try { active = rs.getBoolean("is_active"); } catch (SQLException ignored) {}
                    user.setActive(active);

                    return user;
                }
            } else {
                lastLoginError = "L'adresse email n'a pas été trouvée dans la base de données.";
            }
        } catch (Throwable e) {
            lastLoginError = "Erreur SQL/Système : " + e.toString() + " - " + e.getMessage();
            System.err.println("Erreur lors de la connexion : " + e.getMessage());
        }
        return null;
    }

            u.setGenre(rs.getString("genre"));
            u.setGroupeSanguin(rs.getString("groupe_sanguin"));
            u.setAdresse(rs.getString("adresse"));
    public void activerCompte(int id) {
        String query = "UPDATE user SET is_active=true WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur d'activation : " + e.getMessage());
        }
    }

            list.add(u);
    public void desactiverCompte(int id) {
        String query = "UPDATE user SET is_active=false WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur de désactivation : " + e.getMessage());
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

    public User findByEmail(String email) {
        String query = "SELECT * FROM user WHERE email=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String typeStr = "ROLE_PATIENT";
                try { typeStr = rs.getString("roles"); } catch(SQLException ignored) {}
                if (typeStr == null || typeStr.isEmpty()) typeStr = "[\"ROLE_PATIENT\"]";

                User user = new User(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        typeStr
                );
                user.setTelephone(rs.getString("telephone"));
                user.setPassword(rs.getString("password"));

                boolean active = true;
                try { active = rs.getBoolean("is_active"); } catch (SQLException ignored) {}
                user.setActive(active);

                return user;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par email : " + e.getMessage());
        }
        return null;
    }

    public boolean updatePassword(int id, String newPlainPassword) {
        String query = "UPDATE user SET password=? WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            String hashedPass = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(13));
            pst.setString(1, hashedPass);
            pst.setInt(2, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du mot de passe : " + e.getMessage());
            return false;
        }
    }

    // ==================== MÉTHODES 2FA ====================

    /**
     * Sauvegarde le secret 2FA pour un utilisateur
     */
    public boolean saveTwoFactorSecret(int userId, String secret) {
        String query = "UPDATE user SET two_factor_secret=?, two_factor_enabled=true WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setString(1, secret);
            pst.setInt(2, userId);
            boolean result = pst.executeUpdate() > 0;
            if (result) {
                System.out.println("[2FA] Secret 2FA sauvegardé pour l'utilisateur " + userId);
            }
            return result;
        } catch (SQLException e) {
            System.err.println("[2FA] Erreur lors de la sauvegarde du secret: " + e.getMessage());
            return false;
        }
    }

    /**
     * Désactive la 2FA pour un utilisateur
     */
    public boolean disableTwoFactor(int userId) {
        String query = "UPDATE user SET two_factor_enabled=false, two_factor_secret=NULL WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, userId);
            boolean result = pst.executeUpdate() > 0;
            if (result) {
                System.out.println("[2FA] 2FA désactivée pour l'utilisateur " + userId);
                TwoFactorAuthService.deleteQRCode(userId);
            }
            return result;
        } catch (SQLException e) {
            System.err.println("[2FA] Erreur lors de la désactivation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Récupère le secret 2FA d'un utilisateur
     */
    public String getTwoFactorSecret(int userId) {
        String query = "SELECT two_factor_secret FROM user WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getString("two_factor_secret");
            }
        } catch (SQLException e) {
            System.err.println("[2FA] Erreur lors de la récupération du secret: " + e.getMessage());
        }
        return null;
    }

    /**
     * Vérifie si la 2FA est activée pour un utilisateur
     */
    public boolean isTwoFactorEnabled(int userId) {
        String query = "SELECT two_factor_enabled FROM user WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("two_factor_enabled");
            }
        } catch (SQLException e) {
            System.err.println("[2FA] Erreur lors de la vérification: " + e.getMessage());
        }
        return false;
    }

    /**
     * Charge les données 2FA dans un objet User
     */
    private void load2FAData(User user, ResultSet rs) {
        try {
            boolean twoFactorEnabled = false;
            try {
                twoFactorEnabled = rs.getBoolean("two_factor_enabled");
            } catch (SQLException ignored) {}

            String twoFactorSecret = null;
            try {
                twoFactorSecret = rs.getString("two_factor_secret");
            } catch (SQLException ignored) {}

            user.setTwoFactorEnabled(twoFactorEnabled);
            user.setTwoFactorSecret(twoFactorSecret);
        } catch (Exception e) {
            System.err.println("[2FA] Erreur chargement données 2FA: " + e.getMessage());
        }
    }
    /**
     * Active ou désactive un utilisateur dans la base de données
     */
    public boolean setActive(int userId, boolean active) {
        String query = "UPDATE user SET is_active=? WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(query);
            pst.setBoolean(1, active);
            pst.setInt(2, userId);
            boolean result = pst.executeUpdate() > 0;
            if (result) {
                String status = active ? "activé" : "désactivé";
                System.out.println("[USER] Utilisateur " + userId + " " + status);
            }
            return result;
        } catch (SQLException e) {
            System.err.println("[USER] Erreur lors de la modification du statut actif: " + e.getMessage());
            return false;
        }
    }}
