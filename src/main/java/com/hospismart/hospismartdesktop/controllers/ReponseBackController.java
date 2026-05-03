package com.hospismart.hospismartdesktop.controllers; // Package des controllers JavaFX

import com.hospismart.hospismartdesktop.services.ReponseDao; // DAO pour les operations sur les reponses
import com.hospismart.hospismartdesktop.models.Reponse; // Modele Reponse
import javafx.animation.FadeTransition; // Animation de fondu
import javafx.collections.FXCollections; // Fabrique de listes observables
import javafx.collections.ObservableList; // Liste observable JavaFX
import javafx.event.ActionEvent; // Evenements d'action (boutons)
import javafx.fxml.FXML; // Annotation de liaison FXML
import javafx.fxml.FXMLLoader; // Chargeur FXML pour navigation entre vues
import javafx.fxml.Initializable; // Interface d'initialisation du controller
import javafx.scene.Node; // Noeud JavaFX
import javafx.scene.Parent; // Racine d'une scene
import javafx.scene.Scene; // Scene JavaFX
import javafx.scene.chart.PieChart; // Camembert statistiques
import javafx.scene.control.*; // Composants de controle JavaFX
import javafx.scene.control.cell.PropertyValueFactory; // Liaison colonne/propriete
import javafx.scene.input.MouseEvent; // Evenements souris
import javafx.stage.Stage; // Fenetre principale

import java.io.IOException; // Exception I/O pour chargement FXML
import java.net.URL; // URL de ressource
import java.time.LocalDateTime; // Type date/heure
import java.util.HashSet; // Ensemble sans doublons
import java.util.ResourceBundle; // Bundle de ressources
import javafx.util.Duration; // Duree des animations

public class ReponseBackController implements Initializable { // Controller Back Office des reponses

    @FXML // Table injectee depuis FXML
    private TableView<Reponse> tableReponse; // Tableau des reponses
    @FXML // Colonne contenu injectee
    private TableColumn<Reponse, String> colContenu; // Colonne texte reponse
    @FXML // Colonne admin nom injectee
    private TableColumn<Reponse, String> colAdminNom; // Colonne nom admin
    @FXML // Colonne admin email injectee
    private TableColumn<Reponse, String> colAdminEmail; // Colonne email admin
    @FXML // Colonne date injectee
    private TableColumn<Reponse, LocalDateTime> colDate; // Colonne date reponse

    @FXML // Zone contenu injectee
    private TextArea txtReponse; // Champ edition contenu
    @FXML // Champ nom admin injecte
    private TextField txtAdminNom; // Champ nom admin
    @FXML // Champ email admin injecte
    private TextField txtAdminEmail; // Champ email admin
    @FXML // Champ recherche injecte
    private TextField txtRecherche; // Barre de recherche
    @FXML // Pie chart injecte
    private PieChart chartReponses; // Graphique repartition des reponses
    @FXML // Label total injecte
    private Label lblTotalReponses; // KPI total reponses
    @FXML // Label admins actifs injecte
    private Label lblAdminsActifs; // KPI nombre d'admins actifs
    @FXML // Label reclamations repondues injecte
    private Label lblReclamationsRepondues; // KPI reclamations ayant une reponse

    private ReponseDao dao = new ReponseDao(); // Instance DAO reponse
    private ObservableList<Reponse> reponseList = FXCollections.observableArrayList(); // Source de donnees de la table
    private int selectedId = -1; // Id selectionne, -1 si rien selectionne

    @Override // Methode appelee apres chargement FXML
    public void initialize(URL url, ResourceBundle resourceBundle) { // Initialisation de la vue back reponse
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu")); // Lie colContenu a getContenu()
        colAdminNom.setCellValueFactory(new PropertyValueFactory<>("adminNom")); // Lie colAdminNom a getAdminNom()
        colAdminEmail.setCellValueFactory(new PropertyValueFactory<>("adminEmail")); // Lie colAdminEmail a getAdminEmail()
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateReponse")); // Lie colDate a getDateReponse()

        loadTable(); // Charge les donnees initiales
        setupAnimations(); // Lance animation d'apparition

        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> { // Ecoute les changements de texte recherche
            filterTable(newValue); // Filtre le tableau en temps reel
        });
    }

    private void loadTable() { // Recharge toutes les reponses depuis la base
        reponseList.clear(); // Vide ancienne liste
        reponseList.addAll(dao.getAllReponses()); // Charge depuis DAO
        tableReponse.setItems(reponseList); // Affecte la liste a la table
        refreshKpis(tableReponse.getItems()); // Recalcule les compteurs
        refreshChart(tableReponse.getItems()); // Recalcule le camembert
    }

    private void refreshKpis(ObservableList<Reponse> source) { // Met a jour les statistiques numeriques
        if (lblTotalReponses == null) return; // Garde-fou si UI non injectee
        if (source == null) { // Si source absente
            source = FXCollections.observableArrayList(); // Utilise une liste vide
        }
        lblTotalReponses.setText(String.valueOf(source.size())); // Affiche le total

        HashSet<String> admins = new HashSet<>(); // Ensemble d'emails admin uniques
        HashSet<Integer> reclamations = new HashSet<>(); // Ensemble d'ids reclamation uniques
        for (Reponse r : source) { // Parcourt les reponses visibles
            if (r.getAdminEmail() != null && !r.getAdminEmail().isBlank()) admins.add(r.getAdminEmail()); // Compte admin unique par email
            reclamations.add(r.getReclamationId()); // Compte reclamation unique repondue
        }
        lblAdminsActifs.setText(String.valueOf(admins.size())); // Affiche nb admins actifs
        lblReclamationsRepondues.setText(String.valueOf(reclamations.size())); // Affiche nb reclamations repondues
    }

    private void filterTable(String keyword) { // Filtre la table par mot-cle
        if (keyword == null || keyword.isEmpty()) { // Si pas de filtre
            tableReponse.setItems(reponseList); // Restaure la liste complete
            refreshKpis(tableReponse.getItems()); // Met a jour KPI
            refreshChart(tableReponse.getItems()); // Met a jour chart
            return; // Sort de la methode
        }
        String lowerCaseFilter = keyword.toLowerCase(); // Normalise le mot-cle en minuscule
        ObservableList<Reponse> filteredData = FXCollections.observableArrayList(); // Liste filtree resultat
        for (Reponse r : reponseList) { // Parcourt les reponses de base
            boolean match = (r.getContenu() != null && r.getContenu().toLowerCase().contains(lowerCaseFilter)) || // Match sur contenu
                    (r.getAdminNom() != null && r.getAdminNom().toLowerCase().contains(lowerCaseFilter)) || // Match sur nom admin
                    (r.getAdminEmail() != null && r.getAdminEmail().toLowerCase().contains(lowerCaseFilter)); // Match sur email admin
            if (match) { // Si au moins un champ correspond
                filteredData.add(r); // Ajoute a la liste filtree
            }
        }
        tableReponse.setItems(filteredData); // Affiche uniquement les resultats filtres
        refreshKpis(tableReponse.getItems()); // Met a jour KPI sur donnees filtrees
        refreshChart(tableReponse.getItems()); // Met a jour chart sur donnees filtrees
    }

    private void refreshChart(ObservableList<Reponse> source) { // Reconstruit le camembert par admin
        if (chartReponses == null) return; // Garde-fou si chart absent
        if (source == null) { // Si source nulle
            source = FXCollections.observableArrayList(); // Remplace par liste vide
        }

        java.util.Map<String, Integer> byAdmin = new java.util.LinkedHashMap<>(); // Compte reponses par admin
        for (Reponse r : source) { // Parcourt donnees source
            String key = (r.getAdminNom() == null || r.getAdminNom().isBlank()) ? "Non défini" : r.getAdminNom().trim(); // Cle admin affichee
            byAdmin.put(key, byAdmin.getOrDefault(key, 0) + 1); // Incremente compteur de l'admin
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(); // Donnees finales du chart
        for (java.util.Map.Entry<String, Integer> e : byAdmin.entrySet()) { // Parcourt les paires admin/compteur
            data.add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue())); // Cree une tranche par admin
        }
        if (data.isEmpty()) { // Si aucune donnee
            data.add(new PieChart.Data("Aucune donnée", 1)); // Ajoute tranche placeholder
        }
        chartReponses.setData(data); // Applique les donnees au chart
    }

    @FXML // Handler clique sur une ligne du tableau
    void handleTableClick(MouseEvent event) { // Charge la ligne selectionnee dans le formulaire
        Reponse selected = tableReponse.getSelectionModel().getSelectedItem(); // Recupere la ligne selectionnee
        if (selected != null) { // Verifie qu'une ligne existe
            selectedId = selected.getId(); // Memorise l'id selectionne
            txtReponse.setText(selected.getContenu()); // Remplit contenu
            txtAdminNom.setText(selected.getAdminNom()); // Remplit nom admin
            txtAdminEmail.setText(selected.getAdminEmail()); // Remplit email admin
        }
    }

    @FXML // Handler bouton modifier
    void modifierReponse(ActionEvent event) { // Met a jour une reponse existante
        if (selectedId == -1) { // Si aucune ligne selectionnee
            showAlert("Attention", "Veuillez sélectionner une réponse dans le tableau."); // Alerte utilisateur
            return; // Stoppe traitement
        }

        String error = validateReponseInputs(); // Lance la validation des champs
        if (error != null) { // Si invalide
            showAlert("Erreur de saisie", error); // Affiche l'erreur
            return; // Stoppe traitement
        }

        Reponse r = tableReponse.getSelectionModel().getSelectedItem(); // Recupere l'objet selectionne
        r.setContenu(txtReponse.getText().trim()); // Met a jour contenu
        r.setAdminNom(txtAdminNom.getText().trim()); // Met a jour nom admin
        r.setAdminEmail(txtAdminEmail.getText().trim()); // Met a jour email admin

        dao.updateReponse(r); // Persiste en base
        showAlert("Succès", "La réponse a été modifiée avec succès."); // Confirme succes
        loadTable(); // Recharge tableau
        clearFields(); // Reinitialise formulaire
    }

    @FXML // Handler bouton supprimer
    void supprimerReponse(ActionEvent event) { // Supprime la reponse selectionnee
        if (selectedId == -1) { // Si aucune ligne selectionnee
            showAlert("Attention", "Veuillez sélectionner une réponse à supprimer."); // Avertit utilisateur
            return; // Stoppe traitement
        }

        dao.deleteReponse(selectedId); // Supprime en base par id
        showAlert("Succès", "La réponse a été supprimée de la base de données."); // Confirme suppression
        loadTable(); // Recharge tableau
        clearFields(); // Reinitialise formulaire
    }

    @FXML // Handler bouton actualiser
    void actualiser(ActionEvent event) { // Recharge les donnees et reset formulaire
        loadTable(); // Recharge table/KPI/chart
        clearFields(); // Vide les champs
    }

    private void clearFields() { // Efface les champs du formulaire
        txtReponse.clear(); // Vide contenu
        txtAdminNom.clear(); // Vide nom admin
        txtAdminEmail.clear(); // Vide email admin
        selectedId = -1; // Reset selection
    }

    @FXML // Handler navigation vers ecran reclamations
    void goToReclamations(ActionEvent event) { // Ouvre la vue back reclamation avec fondu
        try { // Debut bloc navigation
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); // Recupere fenetre courante
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/reclamation_back.fxml")); // Charge FXML cible
            Parent root = loader.load(); // Construit l'arbre de noeuds
            root.setOpacity(0); // Rend la vue transparente au depart
            stage.setScene(new Scene(root, 1000, 700)); // Remplace la scene courante
            FadeTransition fade = new FadeTransition(Duration.millis(280), root); // Cree animation fondu
            fade.setFromValue(0); // Debut transparent
            fade.setToValue(1); // Fin opaque
            fade.play(); // Lance animation
        } catch (IOException e) { // Capture erreur de chargement FXML
            e.printStackTrace(); // Log erreur
        }
    }

    private String validateReponseInputs() { // Valide les donnees saisies, retourne message d'erreur ou null
        String contenu = txtReponse.getText() != null ? txtReponse.getText().trim() : ""; // Lit/normalise contenu
        String nom = txtAdminNom.getText() != null ? txtAdminNom.getText().trim() : ""; // Lit/normalise nom
        String email = txtAdminEmail.getText() != null ? txtAdminEmail.getText().trim() : ""; // Lit/normalise email

        if (contenu.length() < 10) { // Regle min longueur contenu
            return "Le contenu de la réponse doit contenir au moins 10 caracteres."; // Message erreur contenu
        }
        if (!nom.matches("^[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s'-]{2,49}$")) { // Regle nom valide
            return "Le nom admin est invalide (minimum 3 lettres, sans chiffres)."; // Message erreur nom
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) { // Regle format email
            return "L'email admin est invalide."; // Message erreur email
        }
        return null; // Validation OK
    }

    private void showAlert(String title, String content) { // Affiche une alerte simple
        Alert alert = new Alert(Alert.AlertType.INFORMATION); // Cree alerte de type information
        alert.setTitle(title); // Definit titre
        alert.setContentText(content); // Definit contenu
        alert.showAndWait(); // Affiche et attend fermeture
    }

    private void setupAnimations() { // Anime l'apparition du tableau
        FadeTransition ftTable = new FadeTransition(Duration.millis(360), tableReponse); // Cree animation fondu sur table
        ftTable.setFromValue(0.0); // Debut transparent
        ftTable.setToValue(1.0); // Fin opaque
        ftTable.play(); // Lance animation
    }
} // Fin du controller ReponseBackController
