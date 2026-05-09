package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.services.ReclamationDao;
import com.hospismart.hospismartdesktop.services.ReponseDao;
import com.hospismart.hospismartdesktop.models.Reclamation;
import com.hospismart.hospismartdesktop.models.Reponse;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReclamationBackController implements Initializable { // Controller Back Office pour gerer reclamations + reponses

    @FXML
    private TableView<Reclamation> tableReclamation; // Tableau principal des reclamations
    @FXML
    private TableColumn<Reclamation, Integer> colId;
    @FXML
    private TableColumn<Reclamation, String> colTitre;
    @FXML
    private TableColumn<Reclamation, String> colPatient;
    @FXML
    private TableColumn<Reclamation, String> colEmail;
    @FXML
    private TableColumn<Reclamation, String> colStatut;
    @FXML
    private TableColumn<Reclamation, String> colCategorie;
    @FXML
    private TableColumn<Reclamation, String> colPriorite;
    @FXML
    private TableColumn<Reclamation, String> colEtatMental;
    @FXML
    private TableColumn<Reclamation, LocalDateTime> colDate;

    @FXML
    private TextField txtTitre;
    @FXML
    private TextField txtPatient;
    @FXML
    private TextField txtEmail;
    @FXML
    private ComboBox<String> cmbStatut;
    @FXML
    private ComboBox<String> cmbCategorie;
    @FXML
    private ComboBox<String> cmbPriorite;
    @FXML
    private TextArea txtDescription;
    @FXML
    private TextField txtEtatMental;
    @FXML
    private TextArea txtReponse;
    @FXML
    private TextField txtAdminNom;
    @FXML
    private TextField txtAdminEmail;
    @FXML
    private TextField txtRecherche;
    @FXML
    private Label lblTotalReclamations;
    @FXML
    private Label lblEnAttente;
    @FXML
    private Label lblTraitees;
    @FXML
    private PieChart chartReclamations;

    @FXML
    private VBox pnlTraitement;

    @FXML
    private Label notifIcon; // Icône notification (à ajouter dans le FXML)

    private ReclamationDao dao = new ReclamationDao(); // DAO reclamation
    private ReponseDao reponseDao = new ReponseDao(); // DAO reponse
    private ObservableList<Reclamation> reclamationList = FXCollections.observableArrayList(); // Source de donnees de la table
    private int selectedId = -1; // Id selectionne, -1 si rien selectionne
    private Reponse currentReponse = null; // Reponse liee a la reclamation selectionnee
    private int lastMaxId = 0; // Pour le tracking des nouvelles reclamations
    private int unreadCount = 0; // Nombre de notifications non lues
    private boolean isFirstCheck = true; // Permet de gerer proprement l'etat initial du polling

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) { // Initialise combos, colonnes, donnees et ecouteurs
        cmbStatut.setItems(FXCollections.observableArrayList("En attente", "En cours", "Résolu", "Fermé"));
        cmbCategorie.setItems(FXCollections.observableArrayList("Service", "Propreté", "Personnel", "Autre"));
        cmbPriorite.setItems(FXCollections.observableArrayList("Basse", "Moyenne", "Haute"));

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("nomPatient"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colPriorite.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        if (colEtatMental != null) {
            colEtatMental.setCellValueFactory(new PropertyValueFactory<>("etatMental"));
        }
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        setupStatusBadgeCells();

        loadTable();
        setupAnimations();
        startRealTimeListener(); // Lancement de l'ecoute en temps reel

        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            filterTable(newValue);
        });
    }

    private void loadTable() { // Recharge reclamations depuis la base puis met a jour KPIs/charts
        reclamationList.clear();
        reclamationList.addAll(dao.getAllReclamations());
        tableReclamation.setItems(reclamationList);
        refreshKpis(tableReclamation.getItems());
        refreshChart(tableReclamation.getItems());
    }

    // ==========================================
    // NOTIFICATIONS POP-UP STANDALONE
    // ==========================================
    private void startRealTimeListener() {
        // En l'absence d'un serveur tiers (ex: Firebase), nous creons un ecouteur local
        // qui "poll" (interroge) la BD toutes les 3 secondes pour detecter les ajouts.
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.seconds(3), e -> checkForNewReclamations())
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void checkForNewReclamations() {
        List<Reclamation> currentList = dao.getAllReclamations();
        int currentMaxId = currentList.stream().mapToInt(Reclamation::getId).max().orElse(0);

        if (isFirstCheck) {
            lastMaxId = currentMaxId;
            isFirstCheck = false;
            return; // Ignore la premiere verification
        }

        if (currentMaxId > lastMaxId) {
            // Nouvelles reclamations detectees !

            // Emet un son systeme
            java.awt.Toolkit.getDefaultToolkit().beep();

            int newCount = 0;
            for (Reclamation r : currentList) {
                if (r.getId() > lastMaxId) {
                    newCount++;
                }
            }
            unreadCount += newCount;
            updateNotifIcon();
            lastMaxId = currentMaxId;

            // On met a jour le tableau en silence
            String search = txtRecherche.getText();
            if (search == null || search.isEmpty()) {
                loadTable();
            } else {
                reclamationList.clear();
                reclamationList.addAll(currentList);
                filterTable(search);
            }
        }
    }

    private void updateNotifIcon() {
        System.out.println("[DEBUG] updateNotifIcon appelé, unreadCount=" + unreadCount);
        if (notifIcon != null) {
            if (unreadCount > 0) {
                notifIcon.setText("🔔 " + unreadCount);
                notifIcon.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 20px;");
                notifIcon.setVisible(true);
            } else {
                notifIcon.setText("🔔");
                notifIcon.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
            }
        } else {
            System.out.println("[DEBUG] notifIcon est null");
        }
    }

    @FXML
    private void handleNotifClick() {
        unreadCount = 0;
        updateNotifIcon();

        try {
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.setTitle("Dernières Notifications");

            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
            vbox.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-border-color: #dbe3ed; -fx-border-width: 1; -fx-border-radius: 5;");

            javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Les 3 dernières réclamations :");
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2f6ecf;");
            vbox.getChildren().add(titleLabel);

            java.util.List<Reclamation> allRecs = dao.getAllReclamations();
            allRecs.sort((r1, r2) -> Integer.compare(r2.getId(), r1.getId()));

            int count = 0;
            for (Reclamation r : allRecs) {
                if (count >= 3) break;
                // Formater la date en String si elle existe
                String dateStr = r.getDateCreation() != null ? r.getDateCreation().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Date inconnue";

                javafx.scene.layout.VBox itemBox = new javafx.scene.layout.VBox(2);
                itemBox.setStyle("-fx-padding: 8; -fx-background-color: #f7f9fc; -fx-background-radius: 5;");

                javafx.scene.control.Label lblTitre = new javafx.scene.control.Label("• " + r.getTitre());
                lblTitre.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

                javafx.scene.control.Label lblDetails = new javafx.scene.control.Label("  Par: " + r.getNomPatient() + " - " + dateStr);
                lblDetails.setStyle("-fx-text-fill: #6b7785; -fx-font-size: 11px;");

                itemBox.getChildren().addAll(lblTitre, lblDetails);
                vbox.getChildren().add(itemBox);
                count++;
            }

            if (count == 0) {
                vbox.getChildren().add(new javafx.scene.control.Label("Aucune réclamation trouvée."));
            }

            javafx.scene.Scene scene = new javafx.scene.Scene(vbox, 350, 250);
            popupStage.setScene(scene);
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ==========================================

    private void setupStatusBadgeCells() { // Configure le rendu visuel des colonnes statut/priorite
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-badge-attente", "status-badge-cours", "status-badge-resolu", "status-badge-ferme", "status-badge-traite");
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String low = normalizeStatus(item);
                    if (low.contains("attente")) getStyleClass().add("status-badge-attente");
                    else if (low.contains("cours")) getStyleClass().add("status-badge-cours");
                    else if (low.contains("resolu")) getStyleClass().add("status-badge-resolu");
                    else if (low.contains("ferme") || low.contains("traite")) getStyleClass().add("status-badge-traite");
                }
            }
        });

        colPriorite.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String low = item.toLowerCase();
                    if (low.contains("haute") || low.contains("urgente")) {
                        setStyle("-fx-text-fill:#c0392b; -fx-font-weight:700;");
                    } else if (low.contains("moyenne") || low.contains("normale")) {
                        setStyle("-fx-text-fill:#d58c1f; -fx-font-weight:700;");
                    } else {
                        setStyle("-fx-text-fill:#2e9b5f; -fx-font-weight:700;");
                    }
                }
            }
        });
    }

    private void refreshKpis(ObservableList<Reclamation> source) { // Recalcule les compteurs dashboard
        if (lblTotalReclamations == null) return;
        int total = source != null ? source.size() : 0;
        int attente = 0;
        int traite = 0;
        if (source == null) {
            source = FXCollections.observableArrayList();
        }
        for (Reclamation r : source) {
            String s = normalizeStatus(r.getStatut());
            if (s.contains("attente")) attente++;
            if (s.contains("resolu") || s.contains("ferme") || s.contains("traite")) traite++;
        }
        lblTotalReclamations.setText(String.valueOf(total));
        lblEnAttente.setText(String.valueOf(attente));
        lblTraitees.setText(String.valueOf(traite));
    }

    private void filterTable(String keyword) { // Filtre en temps reel selon titre/patient/statut
        if (keyword == null || keyword.isEmpty()) {
            tableReclamation.setItems(reclamationList);
            refreshKpis(tableReclamation.getItems());
            refreshChart(tableReclamation.getItems());
            return;
        }
        String lowerCaseFilter = keyword.toLowerCase();
        ObservableList<Reclamation> filteredData = FXCollections.observableArrayList();
        for (Reclamation r : reclamationList) {
            boolean match = (r.getTitre() != null && r.getTitre().toLowerCase().contains(lowerCaseFilter)) ||
                (r.getNomPatient() != null && r.getNomPatient().toLowerCase().contains(lowerCaseFilter)) ||
                (r.getStatut() != null && r.getStatut().toLowerCase().contains(lowerCaseFilter));
            if (match) {
                filteredData.add(r);
            }
        }
        tableReclamation.setItems(filteredData);
        refreshKpis(tableReclamation.getItems());
        refreshChart(tableReclamation.getItems());
    }

    private void refreshChart(ObservableList<Reclamation> source) { // Construit le camembert de repartition des statuts
        if (chartReclamations == null) return;
        if (source == null) {
            source = FXCollections.observableArrayList();
        }

        int attente = 0;
        int enCours = 0;
        int traite = 0;

        for (Reclamation r : source) {
            String s = normalizeStatus(r.getStatut());
            if (s.contains("attente")) attente++;
            else if (s.contains("cours")) enCours++;
            else if (s.contains("resolu") || s.contains("ferme") || s.contains("traite")) traite++;
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
            new PieChart.Data("En attente (" + attente + ")", attente),
            new PieChart.Data("En cours (" + enCours + ")", enCours),
            new PieChart.Data("Traitées (" + traite + ")", traite)
        );

        // Evite un camembert vide
        boolean allZero = attente == 0 && enCours == 0 && traite == 0;
        if (allZero) {
            data = FXCollections.observableArrayList(new PieChart.Data("Aucune donnée", 1));
        }
        chartReclamations.setData(data);
    }

    @FXML
    void handleTableClick(MouseEvent event) { // Charge la reclamation selectionnee dans le formulaire admin
        Reclamation selected = tableReclamation.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedId = selected.getId();
            txtTitre.setText(selected.getTitre());
            txtPatient.setText(selected.getNomPatient());
            txtEmail.setText(selected.getEmail());
            cmbStatut.setValue(selected.getStatut());
            cmbCategorie.setValue(selected.getCategorie());
            cmbPriorite.setValue(selected.getPriorite());
            txtDescription.setText(selected.getDescription());
            if (txtEtatMental != null) {
                txtEtatMental.setText(selected.getEtatMental() != null ? selected.getEtatMental() : "Non évalué");
            }

            // Charge la reponse associee pour edition/suppression dans le meme formulaire.
            currentReponse = reponseDao.getReponseByReclamationId(selectedId);
            if (currentReponse != null) {
                txtReponse.setText(currentReponse.getContenu());
                txtAdminNom.setText(currentReponse.getAdminNom());
                txtAdminEmail.setText(currentReponse.getAdminEmail());
            } else {
                txtReponse.setText("");
                
                // Auto-fill admin data from session when creating a new reply
                com.hospismart.hospismartdesktop.models.User currentUser = com.hospismart.hospismartdesktop.utils.Session.getInstance().getCurrentUser();
                if (currentUser != null) {
                    txtAdminNom.setText(currentUser.getPrenom() + " " + currentUser.getNom());
                    txtAdminEmail.setText(currentUser.getEmail());
                } else {
                    txtAdminNom.setText("");
                    txtAdminEmail.setText("");
                }
            }
        }
    }

    @FXML
    void modifierReclamation(ActionEvent event) { // Met a jour statut reclamation et cree/modifie la reponse admin
        if (selectedId == -1) {
            showAlert("Attention", "Veuillez sélectionner une réclamation à traiter.");
            return;
        }

        String error = validateAdminInputs();
        if (error != null) {
            showAlert("Erreur de saisie", error);
            return;
        }

        Reclamation r = tableReclamation.getSelectionModel().getSelectedItem();
        // L'admin modifie d'abord le statut de la reclamation.
        r.setStatut(cmbStatut.getValue());
        dao.updateReclamation(r);

        // Si une reponse est saisie, on gere soit la creation soit la mise a jour.
        if (txtReponse.getText() != null && !txtReponse.getText().trim().isEmpty()) {
            if (currentReponse == null) {
                currentReponse = new Reponse();
                currentReponse.setReclamationId(selectedId);
                currentReponse.setContenu(txtReponse.getText().trim());
                currentReponse.setAdminNom(txtAdminNom.getText().trim());
                currentReponse.setAdminEmail(txtAdminEmail.getText().trim());
                currentReponse.setDateReponse(LocalDateTime.now());
                reponseDao.addReponse(currentReponse);
            } else {
                currentReponse.setContenu(txtReponse.getText().trim());
                currentReponse.setAdminNom(txtAdminNom.getText().trim());
                currentReponse.setAdminEmail(txtAdminEmail.getText().trim());
                reponseDao.updateReponse(currentReponse);
            }
        }

        showAlert("Succès", "La réponse et le statut ont été enregistrés.");
        loadTable();
        clearFields();
    }

    @FXML
    void supprimerReponse(ActionEvent event) { // Supprime uniquement la reponse liee a la reclamation selectionnee
        if (currentReponse == null) {
            showAlert("Attention", "Aucune réponse à supprimer pour cette réclamation.");
            return;
        }
        reponseDao.deleteReponse(currentReponse.getId());
        currentReponse = null;
        txtReponse.clear();
        txtAdminNom.clear();
        txtAdminEmail.clear();
        showAlert("Succès", "La réponse a été supprimée avec succès.");
    }

    @FXML
    void supprimerReclamation(ActionEvent event) { // Supprime reclamation (+ reponse associee si presente)
        if (selectedId == -1) {
            showAlert("Attention", "Veuillez sélectionner une réclamation à supprimer.");
            return;
        }

        // Supprime d'abord la reponse associee pour garder une coherence metier.
        if (currentReponse != null) {
            reponseDao.deleteReponse(currentReponse.getId());
        }

        dao.deleteReclamation(selectedId);
        loadTable();
        clearFields();
    }

    @FXML
    void actualiser(ActionEvent event) { // Recharge completement la vue
        loadTable();
        clearFields();
    }

    @FXML
    void basculerPanneauTraitement(ActionEvent event) { // Affiche ou masque le panneau de traitement/reponse
        if (pnlTraitement == null) return;
        boolean show = !pnlTraitement.isVisible();
        pnlTraitement.setVisible(show);
        pnlTraitement.setManaged(show);
    }

    @FXML
    void goToReponses(ActionEvent event) { // Navigation vers l'ecran de gestion des reponses
        try {
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.Scene scene = source.getScene();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/reponse_back.fxml"));
            Parent root = loader.load();
            root.setOpacity(0);

            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) scene.lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            } else {
                javafx.stage.Stage stage = (javafx.stage.Stage) scene.getWindow();
                stage.setScene(new javafx.scene.Scene(root, 1000, 700));
            }

            FadeTransition fade = new FadeTransition(Duration.millis(280), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void actualiser() {
        txtRecherche.clear();
        loadTable();
        System.out.println("Donnees actualisees");
    }

    @FXML
    void exporterPDF() {
        if (tableReclamation.getItems().isEmpty()) {
            showAlert("Attention", "Aucune donnée à exporter.");
            return;
        }
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Rapport_Reclamations_Complet.pdf");
        java.io.File file = fileChooser.showSaveDialog(tableReclamation.getScene().getWindow());

        if (file != null) {
            try (org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument()) {
                // Format Paysage (Landscape) pour faire tenir toutes les colonnes
                org.apache.pdfbox.pdmodel.common.PDRectangle formatPaysage = new org.apache.pdfbox.pdmodel.common.PDRectangle(842f, 595f);
                org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(formatPaysage);
                document.addPage(page);

                org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page);

                // --- En-tête du document ---
                contentStream.setNonStrokingColor(41, 128, 185); // Bleu Hôpital
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 24);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 540);
                contentStream.showText("Donnees Completes des Reclamations");
                contentStream.endText();

                // Ligne de séparation
                contentStream.setStrokingColor(41, 128, 185);
                contentStream.setLineWidth(2f);
                contentStream.moveTo(50, 525);
                contentStream.lineTo(790, 525);
                contentStream.stroke();

                // --- Statistiques interactives (Icônes carrées) ---
                int total = tableReclamation.getItems().size();
                int attente = 0, enCours = 0, traite = 0;
                for (Reclamation r : tableReclamation.getItems()) {
                    String s = normalizeStatus(r.getStatut());
                    if (s.contains("attente")) attente++;
                    else if (s.contains("cours")) enCours++;
                    else traite++;
                }

                drawStatItem(contentStream, 50, 490, 41, 128, 185, "Total : " + total);           // Bleu
                drawStatItem(contentStream, 160, 490, 211, 84, 0, "En attente : " + attente);     // Orange
                drawStatItem(contentStream, 310, 490, 52, 152, 219, "En cours : " + enCours);     // Bleu clair
                drawStatItem(contentStream, 460, 490, 39, 174, 96, "Traitees : " + traite);       // Vert

                // --- En-tête du tableau ---
                int headerY = 440;
                contentStream.setNonStrokingColor(41, 128, 185);
                contentStream.addRect(40, headerY - 7, 760, 25);
                contentStream.fill();

                contentStream.setNonStrokingColor(255, 255, 255); // Texte Blanc
                contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 11);

                // Dispositions des colonnes (Larges)
                int xTitre = 50, xPatient = 190, xEmail = 300, xCategorie = 450, xPriorite = 535, xStatut = 615, xDate = 685;

                drawTextAbsolute(contentStream, "Titre", xTitre, headerY);
                drawTextAbsolute(contentStream, "Patient", xPatient, headerY);
                drawTextAbsolute(contentStream, "Email", xEmail, headerY);
                drawTextAbsolute(contentStream, "Categorie", xCategorie, headerY);
                drawTextAbsolute(contentStream, "Priorite", xPriorite, headerY);
                drawTextAbsolute(contentStream, "Statut", xStatut, headerY);
                drawTextAbsolute(contentStream, "Date", xDate, headerY);

                // --- Contenu du tableau ---
                int yPosition = headerY - 25;
                boolean isAlternate = false;
                java.time.format.DateTimeFormatter dtFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                for (Reclamation r : tableReclamation.getItems()) {
                    if (yPosition < 50) { // Nouvelle page si débordement
                        contentStream.close();
                        page = new org.apache.pdfbox.pdmodel.PDPage(formatPaysage);
                        document.addPage(page);
                        contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page);
                        yPosition = 530;

                        // Ré-afficher l'en-tête du tableau
                        contentStream.setNonStrokingColor(41, 128, 185);
                        contentStream.addRect(40, yPosition - 7, 760, 25);
                        contentStream.fill();

                        contentStream.setNonStrokingColor(255, 255, 255);
                        contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 11);
                        drawTextAbsolute(contentStream, "Titre", xTitre, yPosition);
                        drawTextAbsolute(contentStream, "Patient", xPatient, yPosition);
                        drawTextAbsolute(contentStream, "Email", xEmail, yPosition);
                        drawTextAbsolute(contentStream, "Categorie", xCategorie, yPosition);
                        drawTextAbsolute(contentStream, "Priorite", xPriorite, yPosition);
                        drawTextAbsolute(contentStream, "Statut", xStatut, yPosition);
                        drawTextAbsolute(contentStream, "Date", xDate, yPosition);

                        yPosition -= 25;
                    }

                    // Fond alterné pour lisibilité
                    if (isAlternate) {
                        contentStream.setNonStrokingColor(240, 245, 250);
                        contentStream.addRect(40, yPosition - 7, 760, 20);
                        contentStream.fill();
                    }
                    isAlternate = !isAlternate;

                    // Bordure inférieure
                    contentStream.setStrokingColor(220, 220, 220);
                    contentStream.setLineWidth(0.5f);
                    contentStream.moveTo(40, yPosition - 7);
                    contentStream.lineTo(800, yPosition - 7);
                    contentStream.stroke();

                    contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    contentStream.setNonStrokingColor(40, 40, 40);

                    // Données (avec méthode truncate pour éviter chevauchement)
                    drawTextAbsolute(contentStream, truncate(formatPdfText(safe(r.getTitre())), 24), xTitre, yPosition);
                    drawTextAbsolute(contentStream, truncate(formatPdfText(safe(r.getNomPatient())), 15), xPatient, yPosition);
                    drawTextAbsolute(contentStream, truncate(formatPdfText(safe(r.getEmail())), 25), xEmail, yPosition);
                    drawTextAbsolute(contentStream, truncate(formatPdfText(safe(r.getCategorie())), 14), xCategorie, yPosition);

                    // Date
                    String dateStr = r.getDateCreation() != null ? r.getDateCreation().format(dtFormatter) : "N/A";
                    drawTextAbsolute(contentStream, dateStr, xDate, yPosition);

                    // Priorité (Couleur et police)
                    String prioStr = formatPdfText(safe(r.getPriorite()));
                    String checkPrio = prioStr.toLowerCase();
                    if (checkPrio.contains("haute") || checkPrio.contains("urgente")) {
                        contentStream.setNonStrokingColor(192, 57, 43);
                        contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10);
                    } else if (checkPrio.contains("moyenne") || checkPrio.contains("normale")) {
                        contentStream.setNonStrokingColor(211, 84, 0);
                        contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    } else {
                        contentStream.setNonStrokingColor(39, 174, 96);
                        contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    }
                    drawTextAbsolute(contentStream, prioStr, xPriorite, yPosition);

                    // Statut (Couleurs dynamiques)
                    contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10);
                    String statutStr = formatPdfText(safe(r.getStatut()));
                    String checkStat = statutStr.toLowerCase();
                    if (checkStat.contains("attente")) {
                        contentStream.setNonStrokingColor(211, 84, 0);
                    } else if (checkStat.contains("cours")) {
                        contentStream.setNonStrokingColor(41, 128, 185);
                    } else {
                        contentStream.setNonStrokingColor(39, 174, 96);
                    }
                    drawTextAbsolute(contentStream, statutStr, xStatut, yPosition);

                    yPosition -= 20;
                }
                contentStream.close();
                document.save(file);
                showAlert("Succès", "L'export PDF détaillé a été généré avec succès en mode Paysage !");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur", "Une erreur est survenue lors de l'export : " + e.getMessage());
            }
        }
    }

    @FXML
    void exporterExcel() {
        if (tableReclamation.getItems().isEmpty()) {
            showAlert("Attention", "Aucune donnée à exporter.");
            return;
        }
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer le rapport Excel");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx"));
        fileChooser.setInitialFileName("Rapport_Reclamations.xlsx");
        java.io.File file = fileChooser.showSaveDialog(tableReclamation.getScene().getWindow());

        if (file != null) {
            try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("Réclamations");

                // En-tête
                org.apache.poi.xssf.usermodel.XSSFRow headerRow = sheet.createRow(0);
                String[] columns = {"ID", "Titre", "Patient", "Email", "Catégorie", "Priorité", "État Mental", "Statut", "Date"};
                for (int i = 0; i < columns.length; i++) {
                    org.apache.poi.xssf.usermodel.XSSFCell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                }

                // Lignes
                java.time.format.DateTimeFormatter dtFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                int rowNum = 1;
                for (Reclamation r : tableReclamation.getItems()) {
                    org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(r.getId());
                    row.createCell(1).setCellValue(r.getTitre());
                    row.createCell(2).setCellValue(r.getNomPatient());
                    row.createCell(3).setCellValue(r.getEmail());
                    row.createCell(4).setCellValue(r.getCategorie());
                    row.createCell(5).setCellValue(r.getPriorite());
                    row.createCell(6).setCellValue(r.getEtatMental() != null ? r.getEtatMental() : "Non évalué");
                    row.createCell(7).setCellValue(r.getStatut());
                    row.createCell(8).setCellValue(r.getDateCreation() != null ? r.getDateCreation().format(dtFormatter) : "N/A");
                }

                try (java.io.FileOutputStream fileOut = new java.io.FileOutputStream(file)) {
                    workbook.write(fileOut);
                }
                showAlert("Succès", "L'export Excel a été généré avec succès !");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur", "Une erreur est survenue lors de l'export : " + e.getMessage());
            }
        }
    }

    private void drawStatItem(org.apache.pdfbox.pdmodel.PDPageContentStream stream, float x, float y, int rColor, int gColor, int bColor, String text) throws java.io.IOException {
        // Dessiner le petit carré de couleur ("l'icône")
        stream.setNonStrokingColor(rColor, gColor, bColor);
        stream.addRect(x, y - 1, 10, 10);
        stream.fill();

        // Dessiner le texte associé
        stream.setNonStrokingColor(60, 60, 60); // Gris
        stream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 12);
        stream.beginText();
        stream.newLineAtOffset(x + 18, y);
        stream.showText(text);
        stream.endText();
    }

    private String truncate(String text, int length) { // Coupe les textes trop longs pour éviter qu'ils se chevauchent
        if (text == null) return "";
        if (text.length() <= length) return text;
        return text.substring(0, length - 3) + "...";
    }

    private void drawTextAbsolute(org.apache.pdfbox.pdmodel.PDPageContentStream stream, String text, int x, int y) throws java.io.IOException {
        stream.beginText();
        stream.newLineAtOffset(x, y);
        stream.showText(text != null ? text : "");
        stream.endText();
    }

    private String formatPdfText(String text) {
        if (text == null) return "";
        // Remplacer les caractères accentués par leur équivalent non accentué
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Supprimer autres caractères non supportés par la police standard
        return normalized.replaceAll("[^\\x00-\\x7F]", "");
    }

    private String validateAdminInputs() { // Valide statut et champs admin si une reponse est saisie
        if (cmbStatut.getValue() == null || cmbStatut.getValue().trim().isEmpty()) {
            return "Veuillez choisir un statut.";
        }

        String reponse = txtReponse.getText() != null ? txtReponse.getText().trim() : "";
        String nom = txtAdminNom.getText() != null ? txtAdminNom.getText().trim() : "";
        String email = txtAdminEmail.getText() != null ? txtAdminEmail.getText().trim() : "";

        // Nom/email deviennent obligatoires seulement quand le texte de reponse est renseigne.
        if (!reponse.isEmpty()) {
            if (reponse.length() < 10) {
                return "La réponse doit contenir au moins 10 caracteres.";
            }
            if (!nom.matches("^[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s'-]{2,49}$")) {
                return "Le nom admin est invalide (minimum 3 lettres, sans chiffres).";
            }
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return "L'email admin est invalide.";
            }
        }

        return null;
    }

    private String normalizeStatus(String value) { // Supprime accents + minuscule pour comparaisons robustes
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase();
    }

    private void clearFields() { // Reinitialise tous les champs du formulaire
        txtTitre.clear();
        txtPatient.clear();
        txtEmail.clear();
        cmbStatut.setValue(null);
        cmbCategorie.setValue(null);
        cmbPriorite.setValue(null);
        txtDescription.clear();
        if (txtEtatMental != null) txtEtatMental.clear();
        if (txtReponse != null) txtReponse.clear();
        if (txtAdminNom != null) txtAdminNom.clear();
        if (txtAdminEmail != null) txtAdminEmail.clear();
        selectedId = -1;
        currentReponse = null;
    }

    private void showAlert(String title, String content) { // Affiche une boite d'information
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Safe string to avoid nulls in PDF output
    private String safe(String s) {
        return s == null ? "" : s.replaceAll("\t", " ").replaceAll("\n", " ");
    }


    private void setupAnimations() { // Anime l'apparition de la table
        FadeTransition ftTable = new FadeTransition(Duration.millis(350), tableReclamation);
        ftTable.setFromValue(0.0);
        ftTable.setToValue(1.0);
        ftTable.play();
    }
}
