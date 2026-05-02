package com.hospismart.hospismartdesktop.services;


import com.hospismart.hospismartdesktop.models.Consultation;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationService implements CRUD<Consultation> {
    private Connection cnx;

    public ConsultationService() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Consultation c) throws SQLException {
        String req = "INSERT INTO `consultation` (priorite, date_heure, statut, motif, observations, patient_id, medecin_id, rendez_vous_id, examen_clinique, diagnostic, traitement, examens_complementaires, recommandations) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                     "ON DUPLICATE KEY UPDATE priorite=VALUES(priorite), statut=VALUES(statut), observations=VALUES(observations), " +
                     "diagnostic=VALUES(diagnostic), traitement=VALUES(traitement), examen_clinique=VALUES(examen_clinique), " +
                     "examens_complementaires=VALUES(examens_complementaires), recommandations=VALUES(recommandations)";
        
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, c.getPriorite());
        ps.setTimestamp(2, Timestamp.valueOf(c.getDateHeure()));
        ps.setString(3, c.getStatut());
        ps.setString(4, c.getMotif());
        ps.setString(5, c.getObservations());
        ps.setInt(6, c.getPatientId());
        ps.setInt(7, c.getMedecinId());
        ps.setInt(8, c.getRendezVousId());
        ps.setString(9, c.getExamenClinique());
        ps.setString(10, c.getDiagnostic());
        ps.setString(11, c.getTraitement());
        ps.setString(12, c.getExamensComplementaires());
        ps.setString(13, c.getRecommandations());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            c.setId(rs.getInt(1));
        }
    }

    @Override
    public void updateOne(Consultation c) throws SQLException {
        String req = "UPDATE `consultation` SET priorite=?, statut=?, observations=?, diagnostic=?, traitement=?, examens_complementaires=?, recommandations=?, examen_clinique=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, c.getPriorite());
        ps.setString(2, c.getStatut());
        ps.setString(3, c.getObservations());
        ps.setString(4, c.getDiagnostic());
        ps.setString(5, c.getTraitement());
        ps.setString(6, c.getExamensComplementaires());
        ps.setString(7, c.getRecommandations());
        ps.setString(8, c.getExamenClinique());
        ps.setInt(9, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(int id) throws SQLException {
        String req = "DELETE FROM `consultation` WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Consultation> findALL() throws SQLException {
        List<Consultation> list = new ArrayList<>();
        String req = "SELECT * FROM `consultation`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            Consultation c = new Consultation();
            c.setId(rs.getInt("id"));
            c.setPriorite(rs.getInt("priorite"));
            c.setDateHeure(rs.getTimestamp("date_heure").toLocalDateTime());
            c.setStatut(rs.getString("statut"));
            c.setMotif(rs.getString("motif"));
            c.setPatientId(rs.getInt("patient_id"));
            c.setDiagnostic(rs.getString("diagnostic"));
            list.add(c);
        }
        return list;
    }

    public Consultation findByRendezVousId(int rdvId) throws SQLException {
        String req = "SELECT * FROM `consultation` WHERE rendez_vous_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, rdvId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Consultation c = new Consultation();
            c.setId(rs.getInt("id"));
            c.setPriorite(rs.getInt("priorite"));
            c.setDateHeure(rs.getTimestamp("date_heure").toLocalDateTime());
            c.setStatut(rs.getString("statut"));
            c.setMotif(rs.getString("motif"));
            c.setObservations(rs.getString("observations"));
            c.setPatientId(rs.getInt("patient_id"));
            c.setMedecinId(rs.getInt("medecin_id"));
            c.setRendezVousId(rs.getInt("rendez_vous_id"));
            c.setExamenClinique(rs.getString("examen_clinique"));
            c.setDiagnostic(rs.getString("diagnostic"));
            c.setTraitement(rs.getString("traitement"));
            c.setExamensComplementaires(rs.getString("examens_complementaires"));
            c.setRecommandations(rs.getString("recommandations"));
            return c;
        }
        return null;
    }

    public void deleteByRendezVousId(int rdvId) throws SQLException {
        String req = "DELETE FROM `consultation` WHERE rendez_vous_id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, rdvId);
        ps.executeUpdate();
    }
}