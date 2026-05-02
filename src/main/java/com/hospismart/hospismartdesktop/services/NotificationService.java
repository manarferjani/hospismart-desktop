package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Notification;
import com.hospismart.hospismartdesktop.utils.MyDbConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService implements CRUD<Notification> {
    private Connection cnx;

    public NotificationService() {
        cnx = MyDbConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Notification n) throws SQLException {
        String req = "INSERT INTO `notification` (content, created_at, is_read, user_id, type, link_url) VALUES (?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, n.getContent());
        ps.setTimestamp(2, Timestamp.valueOf(n.getCreatedAt()));
        ps.setBoolean(3, n.isRead());
        ps.setInt(4, n.getUserId());
        ps.setString(5, n.getType());
        ps.setString(6, n.getLinkUrl());
        ps.executeUpdate();
    }

    @Override
    public void updateOne(Notification n) throws SQLException {
        String req = "UPDATE `notification` SET is_read=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setBoolean(1, n.isRead());
        ps.setInt(2, n.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("DELETE FROM `notification` WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Notification> findALL() throws SQLException {
        List<Notification> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM `notification` ORDER BY created_at DESC");
        while (rs.next()) {
            Notification n = new Notification();
            n.setId(rs.getInt("id"));
            n.setContent(rs.getString("content"));
            n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            n.setRead(rs.getBoolean("is_read"));
            n.setUserId(rs.getInt("user_id"));
            list.add(n);
        }
        return list;
    }

    /** Récupère toutes les notifications non lues d'un utilisateur, triées par date décroissante */
    public List<Notification> findUnreadByUser(int userId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String req = "SELECT * FROM `notification` WHERE user_id = ? AND is_read = 0 ORDER BY created_at DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Notification n = new Notification();
            n.setId(rs.getInt("id"));
            n.setContent(rs.getString("content"));
            n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            n.setRead(rs.getBoolean("is_read"));
            n.setUserId(rs.getInt("user_id"));
            n.setType(rs.getString("type"));
            list.add(n);
        }
        return list;
    }

    /** Compte les notifications non lues d'un utilisateur */
    public int countUnread(int userId) throws SQLException {
        String req = "SELECT COUNT(*) FROM `notification` WHERE user_id = ? AND is_read = 0";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    /** Marque toutes les notifications d'un user comme lues */
    public void markAllAsRead(int userId) throws SQLException {
        String req = "UPDATE `notification` SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    /** Récupère toutes les notifications d'un utilisateur (lues + non lues), triées par date décroissante, max 50 */
    public List<Notification> findAllByUser(int userId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String req = "SELECT * FROM `notification` WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Notification n = new Notification();
            n.setId(rs.getInt("id"));
            n.setContent(rs.getString("content"));
            n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            n.setRead(rs.getBoolean("is_read"));
            n.setUserId(rs.getInt("user_id"));
            n.setType(rs.getString("type"));
            list.add(n);
        }
        return list;
    }
}