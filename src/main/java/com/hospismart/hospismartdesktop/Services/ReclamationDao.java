package com.hospismart.hospismartdesktop.Services; // Package de la couche DAO

import com.hospismart.hospismartdesktop.models.Reclamation; // Modele Reclamation
import com.hospismart.hospismartdesktop.utils.MyDbConnexion; // Gestionnaire singleton de connexion BD

import java.sql.*; // API JDBC
import java.time.LocalDateTime; // Date/heure locale
import java.util.ArrayList; // Liste concrete pour stocker les resultats
import java.util.List; // Interface liste retournee

public class ReclamationDao { // DAO responsable du CRUD table reclamation

    private Connection cnx; // Connexion JDBC reutilisee

    public ReclamationDao() { // Constructeur du DAO
        cnx = MyDbConnexion.getInstance().getCnx(); // Recupere la connexion singleton
        verifierEtMettreAJourSchema(); // Verifie/ajuste schema de table si necessaire
    }

    private void verifierEtMettreAJourSchema() { // Verifie qu'une colonne legacy existe
        if (cnx == null) return;
        try { // Debut bloc SQL
            // Verifie si la colonne 'reponse' existe, sinon la cree.
            DatabaseMetaData metaData = cnx.getMetaData(); // Recupere meta-infos BD
            ResultSet rs = metaData.getColumns(null, null, "reclamation", "reponse"); // Cherche la colonne reponse
            if (!rs.next()) { // Si la colonne n'existe pas
                Statement st = cnx.createStatement(); // Cree statement SQL
                st.executeUpdate("ALTER TABLE reclamation ADD COLUMN reponse TEXT"); // Ajoute colonne manquante
                System.out.println("Colonne 'reponse' ajoutee avec succes a la base de donnees."); // Log console
            }
        } catch (Exception e) { // Capture erreur generale (SQL ou meta)
            System.err.println("Erreur verification schema : " + e.getMessage()); // Log erreur schema
        }
    }

    public void addReclamation(Reclamation r) { // Inserer une reclamation
        if (cnx == null) return;
        String req = "INSERT INTO reclamation (titre, description, date_creation, email, nom_patient, statut, categorie, priorite, reponse, etat_mental) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // Requete SQL d'insertion
        try { // Debut bloc SQL
            PreparedStatement pst = cnx.prepareStatement(req); // Prepare la requete parametree
            pst.setString(1, r.getTitre()); // Parametre 1: titre
            pst.setString(2, r.getDescription()); // Parametre 2: description
            pst.setTimestamp(3, Timestamp.valueOf(r.getDateCreation() != null ? r.getDateCreation() : LocalDateTime.now())); // Parametre 3: date creation
            pst.setString(4, r.getEmail()); // Parametre 4: email
            pst.setString(5, r.getNomPatient()); // Parametre 5: nom patient
            pst.setString(6, r.getStatut() != null ? r.getStatut() : "En attente"); // Parametre 6: statut (defaut si null)
            pst.setString(7, r.getCategorie()); // Parametre 7: categorie
            pst.setString(8, r.getPriorite()); // Parametre 8: priorite
            pst.setString(9, r.getReponse() != null ? r.getReponse() : ""); // Parametre 9: reponse texte legacy
            pst.setString(10, r.getEtatMental() != null ? r.getEtatMental() : ""); // Parametre 10: etat_mental
            pst.executeUpdate(); // Execute insertion
        } catch (SQLException e) { // Capture erreurs SQL
            e.printStackTrace(); // Log simple d'erreur
        }
    }

    public List<Reclamation> getAllReclamations() { // Recuperer toutes les reclamations
        List<Reclamation> list = new ArrayList<>(); // Liste resultat
        if (cnx == null) {
            System.err.println("Erreur: Connexion BD non initialisée.");
            return list;
        }
        String req = "SELECT * FROM reclamation"; // Requete de lecture complete
        try { // Debut bloc SQL
            Statement st = cnx.createStatement(); // Cree statement simple
            ResultSet rs = st.executeQuery(req); // Execute SELECT
            ResultSetMetaData meta = rs.getMetaData(); // Recupere metadata des colonnes
            int columnCount = meta.getColumnCount(); // Nombre total de colonnes

            // Logs debug pour visualiser le schema detecte.
            System.out.println("---- DEBUG DB COLUMNS ----"); // Debut log colonnes
            for (int j = 1; j <= columnCount; j++) { // Parcourt colonnes
                System.out.println(meta.getColumnName(j) + " type: " + meta.getColumnTypeName(j)); // Affiche nom + type
            }
            System.out.println("--------------------------"); // Fin log colonnes

            while (rs.next()) { // Parcourt chaque ligne resultat
                Reclamation r = new Reclamation(); // Cree objet a remplir

                // Boucle defensive sur toutes les colonnes detectees.
                for (int i = 1; i <= columnCount; i++) { // Parcourt les colonnes de la ligne
                    String colName = meta.getColumnName(i).toLowerCase(); // Normalise nom colonne en minuscule

                    try { // Map chaque colonne selon son nom
                        if (colName.equals("id")) r.setId(rs.getInt(i)); // Map id
                        else if (colName.equals("titre")) r.setTitre(rs.getString(i)); // Map titre
                        else if (colName.equals("description")) r.setDescription(rs.getString(i)); // Map description
                        else if (colName.equals("email")) r.setEmail(rs.getString(i)); // Map email
                        else if (colName.equals("statut")) r.setStatut(rs.getString(i)); // Map statut
                        else if (colName.equals("categorie") || colName.equals("category")) r.setCategorie(rs.getString(i)); // Map categorie (FR/EN)
                        else if (colName.equals("priorite") || colName.equals("priority")) r.setPriorite(rs.getString(i)); // Map priorite (FR/EN)
                        else if (colName.equals("etat_mental") || colName.equals("etatmental")) r.setEtatMental(rs.getString(i)); // Map etat_mental
                        else if (colName.equals("reponse")) r.setReponse(rs.getString(i)); // Map colonne reponse legacy
                        else if (colName.equals("nompatient") || colName.equals("nom_patient")) r.setNomPatient(rs.getString(i)); // Map nom patient (formats differents)
                        else if (colName.equals("datecreation") || colName.equals("date_creation")) { // Gere variantes nom date
                            if (rs.getTimestamp(i) != null) { // Verifie timestamp non null
                                r.setDateCreation(rs.getTimestamp(i).toLocalDateTime()); // Convertit Timestamp en LocalDateTime
                            }
                        }
                    } catch (Exception e) { // Capture erreur de mapping colonne par colonne
                        System.err.println("Erreur sur la colonne " + colName + " : " + e.getMessage()); // Log erreur detaillee
                    }
                }

                list.add(r); // Ajoute l'objet mappe dans la liste
            }
        } catch (SQLException e) { // Capture erreurs globales SQL
            e.printStackTrace(); // Log stacktrace
            System.err.println("Erreur globale lors de la recuperation : " + e.getMessage()); // Message contextualise
        }
        return list; // Retourne toutes les reclamations recuperees
    }

    public void updateReclamation(Reclamation r) { // Mettre a jour une reclamation existante
        if (cnx == null) return;
        String req = "UPDATE reclamation SET titre=?, description=?, email=?, nom_patient=?, statut=?, categorie=?, priorite=?, reponse=?, etat_mental=? WHERE id=?"; // Requete SQL d'update
        try { // Debut bloc SQL
            PreparedStatement pst = cnx.prepareStatement(req); // Prepare la requete
            pst.setString(1, r.getTitre()); // Parametre 1: titre
            pst.setString(2, r.getDescription()); // Parametre 2: description
            pst.setString(3, r.getEmail()); // Parametre 3: email
            pst.setString(4, r.getNomPatient()); // Parametre 4: nom patient
            pst.setString(5, r.getStatut()); // Parametre 5: statut
            pst.setString(6, r.getCategorie()); // Parametre 6: categorie
            pst.setString(7, r.getPriorite()); // Parametre 7: priorite
            pst.setString(8, r.getReponse() != null ? r.getReponse() : ""); // Parametre 8: reponse legacy
            pst.setString(9, r.getEtatMental() != null ? r.getEtatMental() : ""); // Parametre 9: etat_mental
            pst.setInt(10, r.getId()); // Parametre 10: id de la ligne a modifier
            pst.executeUpdate(); // Execute mise a jour
        } catch (SQLException e) { // Capture erreur SQL
            e.printStackTrace(); // Log simple d'erreur
        }
    }

    public void deleteReclamation(int id) { // Supprimer une reclamation par son id
        if (cnx == null) return;
        String req = "DELETE FROM reclamation WHERE id=?"; // Requete SQL de suppression
        try { // Debut bloc SQL
            PreparedStatement pst = cnx.prepareStatement(req); // Prepare la requete parametree
            pst.setInt(1, id); // Affecte l'id cible
            pst.executeUpdate(); // Execute suppression
        } catch (SQLException e) { // Capture erreur SQL
            e.printStackTrace(); // Log simple d'erreur
        }
    }
} // Fin du DAO Reclamation
