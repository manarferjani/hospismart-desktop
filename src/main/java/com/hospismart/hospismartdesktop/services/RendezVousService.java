package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.RendezVous;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RendezVousService implements CRUD<RendezVous> {

    private Connection cnx;

    public RendezVousService() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    private void checkConnection() throws SQLException {
        if (cnx == null || cnx.isClosed()) {
            System.out.println("WARN: Reconnecting to DB...");
            cnx = MyDbConnexion.getInstance().getCnx();
            if (cnx == null || cnx.isClosed()) {
                cnx = java.sql.DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/hospismart", "root", "");
            }
        }
        if (cnx == null) {
            throw new SQLException("Impossible de rétablir la connexion à la base de données. cnx est null.");
        }
    }

    @Override
    public void insertOne(RendezVous rv) throws SQLException {
        String req = "INSERT INTO `rendez_vous` (`datetime`, `statut`, `motif`, `patient_id`, `medecin_id`, `priorite`, `disponibilite_id`) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setTimestamp(1, Timestamp.valueOf(rv.getDatetime()));
        ps.setString(2, rv.getStatut());
        ps.setString(3, rv.getMotif());
        ps.setInt(4, rv.getPatientId());
        ps.setInt(5, rv.getMedecinId());
        ps.setInt(6, rv.getPriorite());
        ps.setInt(7, rv.getDisponibiliteId()); // Lien avec le créneau choisi

        ps.executeUpdate();
    }

    @Override
    public void updateOne(RendezVous rv) throws SQLException {
        String req = "UPDATE `rendez_vous` SET `datetime`=?, `statut`=?, `motif`=?, `priorite`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setTimestamp(1, Timestamp.valueOf(rv.getDatetime()));
        ps.setString(2, rv.getStatut());
        ps.setString(3, rv.getMotif());
        ps.setInt(4, rv.getPriorite());
        ps.setInt(5, rv.getId());

        ps.executeUpdate();
    }

    @Override
    public void deleteOne(int id) throws SQLException {
        String req = "DELETE FROM `rendez_vous` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<RendezVous> findALL() throws SQLException {
        List<RendezVous> list = new ArrayList<>();
        String req = "SELECT * FROM `rendez_vous`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            RendezVous rv = new RendezVous();
            rv.setId(rs.getInt("id"));
            // Conversion inverse : Timestamp vers LocalDateTime
            rv.setDatetime(rs.getTimestamp("datetime").toLocalDateTime());
            rv.setStatut(rs.getString("statut"));
            rv.setMotif(rs.getString("motif"));
            rv.setPatientId(rs.getInt("patient_id"));
            rv.setMedecinId(rs.getInt("medecin_id"));
            rv.setPriorite(rs.getInt("priorite"));

            list.add(rv);
        }
        return list;
    }
    public List<RendezVous> findPendingByMedecin(int medecinId) throws SQLException {
        checkConnection();
        List<RendezVous> list = new ArrayList<>();
        // Jointure avec la table user pour récupérer le nom/prénom du patient
        String req = "SELECT r.*, u.nom, u.prenom FROM `rendez_vous` r " +
                "JOIN `user` u ON r.patient_id = u.id " +
                "WHERE r.medecin_id = ? AND r.statut = 'En attente'";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, medecinId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            RendezVous rv = new RendezVous();
            rv.setId(rs.getInt("id"));
            rv.setDatetime(rs.getTimestamp("datetime").toLocalDateTime());
            rv.setStatut(rs.getString("statut"));
            rv.setMotif(rs.getString("motif"));
            rv.setPatientId(rs.getInt("patient_id"));
            rv.setMedecinId(rs.getInt("medecin_id"));
            rv.setPriorite(rs.getInt("priorite"));
            
            // On concatène nom et prénom pour l'affichage
            String fullName = rs.getString("nom").toUpperCase() + " " + rs.getString("prenom");
            rv.setPatientName(fullName);
            
            list.add(rv);
        }
        return list;
    }

    public RendezVous findNextByMedecin(int medecinId) throws SQLException {
        checkConnection();
        String req = "SELECT r.*, u.nom, u.prenom FROM `rendez_vous` r " +
                    "JOIN `user` u ON r.patient_id = u.id " +
                    "WHERE r.medecin_id = ? AND r.statut = 'Accepté' AND (r.datetime >= DATE_SUB(NOW(), INTERVAL 2 HOUR) OR DATE(r.datetime) = CURDATE()) " +
                    "ORDER BY r.datetime ASC LIMIT 1";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, medecinId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            RendezVous rv = new RendezVous();
            rv.setId(rs.getInt("id"));
            rv.setDatetime(rs.getTimestamp("datetime").toLocalDateTime());
            rv.setStatut(rs.getString("statut"));
            rv.setMotif(rs.getString("motif"));
            rv.setPatientId(rs.getInt("patient_id"));
            rv.setMedecinId(rs.getInt("medecin_id"));
            
            String fullName = rs.getString("nom").toUpperCase() + " " + rs.getString("prenom");
            rv.setPatientName(fullName);
            return rv;
        }
        return null;
    }

    // Méthodes pour Accepter/Refuser (équivalent des actions Symfony)
    public void updateStatut(int id, String nouveauStatut) throws SQLException {
        String req = "UPDATE `rendez_vous` SET `statut` = ? WHERE `id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, nouveauStatut);
        ps.setInt(2, id);
        ps.executeUpdate();
    }

    /** Récupère les rendez-vous d'un patient avec le nom du médecin */
    public List<RendezVous> findByPatient(int patientId) throws SQLException {
        checkConnection();
        List<RendezVous> list = new ArrayList<>();
        String req = "SELECT r.*, u.nom AS medecin_nom, u.prenom AS medecin_prenom, u.specialite " +
                     "FROM `rendez_vous` r " +
                     "LEFT JOIN `user` u ON r.medecin_id = u.id " +
                     "WHERE r.patient_id = ? " +
                     "ORDER BY r.datetime DESC";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, patientId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            RendezVous rv = new RendezVous();
            rv.setId(rs.getInt("id"));
            java.sql.Timestamp ts = rs.getTimestamp("datetime");
            if (ts != null) {
                rv.setDatetime(ts.toLocalDateTime());
            } else {
                rv.setDatetime(java.time.LocalDateTime.now());
            }
            rv.setStatut(rs.getString("statut"));
            rv.setMotif(rs.getString("motif"));
            rv.setPatientId(rs.getInt("patient_id"));
            rv.setMedecinId(rs.getInt("medecin_id"));
            rv.setPriorite(rs.getInt("priorite"));

            // Nom du médecin pour l'affichage (sécurisé via LEFT JOIN)
            String nom = rs.getString("medecin_nom");
            String prenom = rs.getString("medecin_prenom");
            String spec = rs.getString("specialite");

            if (nom != null && prenom != null) {
                rv.setMedecinName("Dr. " + nom.toUpperCase() + " " + prenom);
            } else {
                rv.setMedecinName("Dr. Medecin ID " + rv.getMedecinId());
            }

            rv.setSpecialite(spec != null ? spec : "Spécialité inconnue");

            list.add(rv);
        }
        return list;
    }

    /**
     * Récupère l'historique des rendez-vous pour un patient spécifique et un médecin spécifique.
     * Trie par date décroissante pour avoir les plus récents en premier.
     */
    public List<RendezVous> findHistoryByPatientAndMedecin(int patientId, int medecinId) throws SQLException {
        checkConnection();
        List<RendezVous> list = new ArrayList<>();
        String req = "SELECT * FROM `rendez_vous` WHERE `patient_id` = ? AND `medecin_id` = ? ORDER BY `datetime` DESC";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, patientId);
        ps.setInt(2, medecinId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            RendezVous rv = new RendezVous();
            rv.setId(rs.getInt("id"));
            java.sql.Timestamp ts = rs.getTimestamp("datetime");
            if (ts != null) {
                rv.setDatetime(ts.toLocalDateTime());
            } else {
                rv.setDatetime(java.time.LocalDateTime.now());
            }
            rv.setStatut(rs.getString("statut"));
            rv.setMotif(rs.getString("motif"));
            rv.setPatientId(rs.getInt("patient_id"));
            rv.setMedecinId(rs.getInt("medecin_id"));
            rv.setPriorite(rs.getInt("priorite"));
            list.add(rv);
        }
        return list;
    }
}