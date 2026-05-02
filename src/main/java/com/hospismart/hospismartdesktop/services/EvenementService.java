package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Evenement;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EvenementService {

    private final Connection cnx;

    public EvenementService() {
        this.cnx = MyDbConnexion.getInstance().getCnx();
    }

    // ── READ ALL ───────────────────────────────────────

    public List<Evenement> findAll() {
        List<Evenement> list = new ArrayList<>();
        String sql = "SELECT * FROM evenement ORDER BY date_debut DESC";
        try (Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("EvenementService.findAll: " + e.getMessage());
        }
        return list;
    }

    // ── READ ONE ───────────────────────────────────────

    public Optional<Evenement> findById(int id) {
        String sql = "SELECT * FROM evenement WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("EvenementService.findById: " + e.getMessage());
        }
        return Optional.empty();
    }

    // ── CREATE ─────────────────────────────────────────

    public boolean create(Evenement e) {
        String sql = "INSERT INTO evenement " +
                "(titre, description, type_evenement, date_debut, date_fin, lieu, statut, budget_alloue, latitude, longitude) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            setParams(ps, e);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("EvenementService.create: " + ex.getMessage());
            return false;
        }
    }

    // ── UPDATE ─────────────────────────────────────────

    public boolean update(Evenement e) {
        String sql = "UPDATE evenement SET " +
                "titre=?, description=?, type_evenement=?, date_debut=?, date_fin=?, " +
                "lieu=?, statut=?, budget_alloue=?, latitude=?, longitude=? " +
                "WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            setParams(ps, e);
            ps.setInt(11, e.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("EvenementService.update: " + ex.getMessage());
            return false;
        }
    }

    // ── DELETE ─────────────────────────────────────────

    public boolean delete(int id) {
        String sql = "DELETE FROM evenement WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("EvenementService.delete: " + ex.getMessage());
            return false;
        }
    }

    // ── SEARCH ─────────────────────────────────────────

    public List<Evenement> search(String keyword, String type, String statut) {
        List<Evenement> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM evenement WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (titre LIKE ? OR lieu LIKE ? OR description LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw); params.add(kw); params.add(kw);
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND type_evenement = ?");
            params.add(type.trim());
        }
        if (statut != null && !statut.isBlank()) {
            sql.append(" AND statut = ?");
            params.add(statut.trim());
        }
        sql.append(" ORDER BY date_debut DESC");

        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("EvenementService.search: " + e.getMessage());
        }
        return list;
    }

    // ── HELPERS ────────────────────────────────────────

    private void setParams(PreparedStatement ps, Evenement e) throws SQLException {
        ps.setString(1, e.getTitre());
        ps.setString(2, e.getDescription());
        ps.setString(3, e.getTypeEvenement());
        ps.setTimestamp(4, e.getDateDebut() != null ? Timestamp.valueOf(e.getDateDebut()) : null);
        ps.setTimestamp(5, e.getDateFin()   != null ? Timestamp.valueOf(e.getDateFin())   : null);
        ps.setString(6, e.getLieu());
        ps.setString(7, e.getStatut());
        ps.setDouble(8, e.getBudgetAlloue());
        if (e.getLatitude()  != null) ps.setDouble(9,  e.getLatitude());
        else ps.setNull(9,  Types.DOUBLE);
        if (e.getLongitude() != null) ps.setDouble(10, e.getLongitude());
        else ps.setNull(10, Types.DOUBLE);
    }

    private Evenement mapRow(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setId(rs.getInt("id"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));
        e.setTypeEvenement(rs.getString("type_evenement"));
        Timestamp dd = rs.getTimestamp("date_debut");
        if (dd != null) e.setDateDebut(dd.toLocalDateTime());
        Timestamp df = rs.getTimestamp("date_fin");
        if (df != null) e.setDateFin(df.toLocalDateTime());
        e.setLieu(rs.getString("lieu"));
        e.setStatut(rs.getString("statut"));
        e.setBudgetAlloue(rs.getDouble("budget_alloue"));
        double lat = rs.getDouble("latitude");
        if (!rs.wasNull()) e.setLatitude(lat);
        double lng = rs.getDouble("longitude");
        if (!rs.wasNull()) e.setLongitude(lng);
        return e;
    }
}
