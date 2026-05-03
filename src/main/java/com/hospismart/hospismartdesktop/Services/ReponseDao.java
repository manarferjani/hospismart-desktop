package com.hospismart.hospismartdesktop.services; // Package de la couche d'acces aux donnees

import com.hospismart.hospismartdesktop.models.Reponse; // Modele Reponse manipule par ce DAO
import com.hospismart.hospismartdesktop.utils.MyDbConnexion; // Utilitaire singleton de connexion BD

import java.sql.*; // API JDBC (Connection, PreparedStatement, ResultSet...)
import java.time.LocalDateTime; // Date/heure pour valeur par defaut

public class ReponseDao { // DAO responsable du CRUD sur la table reponse

    private Connection cnx; // Connexion JDBC reutilisee par les methodes

    public ReponseDao() { // Constructeur du DAO
        cnx = MyDbConnexion.getInstance().getCnx(); // Recupere la connexion partagee
    }

    public void addReponse(Reponse r) { // Inserer une nouvelle reponse en base
        if (cnx == null) return;
        String req = "INSERT INTO reponse (contenu, date_reponse, admin_nom, reclamation_id, admin_email) VALUES (?, ?, ?, ?, ?)"; // Requete SQL d'insertion
        try { // Debut bloc d'execution SQL
            PreparedStatement pst = cnx.prepareStatement(req); // Prepare la requete parametree
            pst.setString(1, r.getContenu()); // Parametre 1: contenu
            pst.setTimestamp(2, Timestamp.valueOf(r.getDateReponse() != null ? r.getDateReponse() : LocalDateTime.now())); // Parametre 2: date reponse
            pst.setString(3, r.getAdminNom()); // Parametre 3: nom admin
            pst.setInt(4, r.getReclamationId()); // Parametre 4: id reclamation liee
            pst.setString(5, r.getAdminEmail()); // Parametre 5: email admin
            pst.executeUpdate(); // Execute l'insertion
        } catch (SQLException e) { // Capture les erreurs SQL
            e.printStackTrace(); // Log simple de l'erreur
        }
    }

    public java.util.List<Reponse> getAllReponses() { // Recuperer toutes les reponses
        java.util.List<Reponse> list = new java.util.ArrayList<>(); // Liste resultat
        if (cnx == null) return list;
        String req = "SELECT * FROM reponse"; // Requete SQL de lecture complete
        try { // Debut bloc SQL
            Statement st = cnx.createStatement(); // Cree un statement simple
            ResultSet rs = st.executeQuery(req); // Execute le SELECT
            while (rs.next()) { // Parcourt chaque ligne retournee
                Reponse r = new Reponse(); // Cree un objet Reponse a mapper
                r.setId(rs.getInt("id")); // Map colonne id
                r.setContenu(rs.getString("contenu")); // Map colonne contenu
                if (rs.getTimestamp("date_reponse") != null) { // Verifie que la date existe
                    r.setDateReponse(rs.getTimestamp("date_reponse").toLocalDateTime()); // Map date_reponse vers LocalDateTime
                }
                r.setAdminNom(rs.getString("admin_nom")); // Map nom admin
                r.setReclamationId(rs.getInt("reclamation_id")); // Map id reclamation liee
                r.setAdminEmail(rs.getString("admin_email")); // Map email admin
                list.add(r); // Ajoute l'objet mappe a la liste resultat
            }
        } catch (SQLException e) { // Capture erreurs SQL
            e.printStackTrace(); // Log simple de l'erreur
        }
        return list; // Retourne la liste des reponses
    }

    public Reponse getReponseByReclamationId(int reclamationId) { // Recuperer la reponse associee a une reclamation
        if (cnx == null) return null;
        String req = "SELECT * FROM reponse WHERE reclamation_id = ?"; // Requete SQL filtree par id reclamation
        try { // Debut bloc SQL
            PreparedStatement pst = cnx.prepareStatement(req); // Prepare la requete parametree
            pst.setInt(1, reclamationId); // Affecte l'id reclamation au parametre 1
            ResultSet rs = pst.executeQuery(); // Execute le SELECT
            if (rs.next()) { // Si une ligne est trouvee
                Reponse r = new Reponse(); // Cree un objet Reponse
                r.setId(rs.getInt("id")); // Map id
                r.setContenu(rs.getString("contenu")); // Map contenu
                if (rs.getTimestamp("date_reponse") != null) { // Verifie presence date
                    r.setDateReponse(rs.getTimestamp("date_reponse").toLocalDateTime()); // Map date
                }
                r.setAdminNom(rs.getString("admin_nom")); // Map nom admin
                r.setReclamationId(rs.getInt("reclamation_id")); // Map id reclamation
                r.setAdminEmail(rs.getString("admin_email")); // Map email admin
                return r; // Retourne la reponse trouvee
            }
        } catch (SQLException e) { // Capture erreurs SQL
            e.printStackTrace(); // Log simple de l'erreur
        }
        return null; // Retourne null si aucune reponse n'est trouvee
    }

    public void updateReponse(Reponse r) { // Mettre a jour une reponse existante
        if (cnx == null) return;
        String req = "UPDATE reponse SET contenu=?, admin_nom=?, admin_email=?, date_reponse=? WHERE id=?"; // Requete SQL de mise a jour
        try { // Debut bloc SQL
            PreparedStatement pst = cnx.prepareStatement(req); // Prepare la requete parametree
            pst.setString(1, r.getContenu()); // Parametre 1: contenu
            pst.setString(2, r.getAdminNom()); // Parametre 2: nom admin
            pst.setString(3, r.getAdminEmail()); // Parametre 3: email admin
            pst.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now())); // Parametre 4: date de modification
            pst.setInt(5, r.getId()); // Parametre 5: id cible
            pst.executeUpdate(); // Execute la mise a jour
        } catch (SQLException e) { // Capture erreurs SQL
            e.printStackTrace(); // Log simple de l'erreur
        }
    }

    public void deleteReponse(int id) { // Supprimer une reponse par son id
        if (cnx == null) return;
        String req = "DELETE FROM reponse WHERE id=?"; // Requete SQL de suppression
        try { // Debut bloc SQL
            PreparedStatement pst = cnx.prepareStatement(req); // Prepare la requete parametree
            pst.setInt(1, id); // Affecte l'id cible
            pst.executeUpdate(); // Execute la suppression
        } catch (SQLException e) { // Capture erreurs SQL
            e.printStackTrace(); // Log simple de l'erreur
        }
    }
} // Fin du DAO Reponse
