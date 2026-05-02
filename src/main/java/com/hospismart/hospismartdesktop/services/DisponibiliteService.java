package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Disponibilite;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DisponibiliteService implements CRUD<Disponibilite> {

    private Connection cnx;

    public DisponibiliteService() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Disponibilite d) throws SQLException {
        // On insère les dates et l'ID du médecin (clé étrangère)
        String req = "INSERT INTO `disponibilite` (`date_debut`, `date_fin`, `est_reserve`, `medecin_id`) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setTimestamp(1, Timestamp.valueOf(d.getDateDebut()));
        ps.setTimestamp(2, Timestamp.valueOf(d.getDateFin()));
        ps.setBoolean(3, d.isReserve());
        ps.setInt(4, d.getMedecin().getId()); // On récupère l'ID de l'objet User

        ps.executeUpdate();
    }

    @Override
    public void updateOne(Disponibilite d) throws SQLException {
        String req = "UPDATE `disponibilite` SET `date_debut`=?, `date_fin`=?, `est_reserve`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setTimestamp(1, Timestamp.valueOf(d.getDateDebut()));
        ps.setTimestamp(2, Timestamp.valueOf(d.getDateFin()));
        ps.setBoolean(3, d.isReserve());
        ps.setInt(4, d.getId());

        ps.executeUpdate();
    }

    @Override
    public void deleteOne(int id) throws SQLException {
        String req = "DELETE FROM `disponibilite` WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Disponibilite> findALL() throws SQLException {
        List<Disponibilite> dispos = new ArrayList<>();
        // On ne récupère que les disponibilités futures
        String req = "SELECT * FROM `disponibilite` WHERE date_debut >= NOW() ORDER BY date_debut ASC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Disponibilite d = new Disponibilite();
            d.setId(rs.getInt("id"));

            // Conversion SQL Timestamp -> Java LocalDateTime
            d.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
            d.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());

            d.setEstReserve(rs.getBoolean("est_reserve"));

            // Note: Pour le médecin, tu pourrais charger l'objet User complet ici
            // ou simplement hydrater l'ID si nécessaire.
            dispos.add(d);
        }
        return dispos;
    }
    public List<Disponibilite> findAvailableByMedecin(int medecinId) throws SQLException {
        List<Disponibilite> dispos = new ArrayList<>();
        String req = "SELECT * FROM `disponibilite` WHERE medecin_id = ? AND date_debut >= NOW() AND est_reserve = 0 ORDER BY date_debut ASC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, medecinId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Disponibilite d = new Disponibilite();
            d.setId(rs.getInt("id"));
            d.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
            d.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
            d.setEstReserve(rs.getBoolean("est_reserve"));
            d.setMedecinId(rs.getInt("medecin_id"));
            dispos.add(d);
        }
        return dispos;
    }

    public boolean checkOverlap(int medecinId, LocalDateTime start, LocalDateTime end, Integer excludeId) throws SQLException {
        String req = "SELECT COUNT(*) FROM `disponibilite` WHERE medecin_id = ? AND date_debut < ? AND date_fin > ? AND est_reserve = 0";
        if (excludeId != null) {
            req += " AND id != ?";
        }

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, medecinId);
        ps.setTimestamp(2, Timestamp.valueOf(end));
        ps.setTimestamp(3, Timestamp.valueOf(start));
        if (excludeId != null) {
            ps.setInt(4, excludeId);
        }

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int count = rs.getInt(1);
            System.out.println("DEBUG checkOverlap: medecinId=" + medecinId + ", start=" + start + ", end=" + end + ", chevauchements=" + count);
            return count > 0;
        }
        return false;
    }

    public void updateStatut(int id, boolean isReserved) throws SQLException {
        String req = "UPDATE `disponibilite` SET `est_reserve` = ? WHERE `id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setBoolean(1, isReserved);
        ps.setInt(2, id);
        ps.executeUpdate();
    }
}