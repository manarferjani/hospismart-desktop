package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.dao.ReclamationDao;
import com.hospismart.hospismartdesktop.dao.ReponseDao;
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

import java.net.URL;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import javafx.util.Duration;

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

    private ReclamationDao dao = new ReclamationDao(); // DAO reclamation
    private ReponseDao reponseDao = new ReponseDao(); // DAO reponse
    private ObservableList<Reclamation> reclamationList = FXCollections.observableArrayList(); // Source de donnees de la table
    private int selectedId = -1; // Id selectionne, -1 si rien selectionne
    private Reponse currentReponse = null; // Reponse liee a la reclamation selectionnee

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
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        setupStatusBadgeCells();

        loadTable();
        setupAnimations();

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

            // Charge la reponse associee pour edition/suppression dans le meme formulaire.
            currentReponse = reponseDao.getReponseByReclamationId(selectedId);
            if (currentReponse != null) {
                txtReponse.setText(currentReponse.getContenu());
                txtAdminNom.setText(currentReponse.getAdminNom());
                txtAdminEmail.setText(currentReponse.getAdminEmail());
            } else {
                txtReponse.setText("");
                txtAdminNom.setText("");
                txtAdminEmail.setText("");
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
    void goToReponses(ActionEvent event) { // Navigation vers l'ecran de gestion des reponses
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/reponse_back.fxml"));
            Parent root = loader.load();
            root.setOpacity(0);
            stage.setScene(new javafx.scene.Scene(root, 1000, 700));
            FadeTransition fade = new FadeTransition(Duration.millis(280), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
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

    private void setupAnimations() { // Anime l'apparition de la table
        FadeTransition ftTable = new FadeTransition(Duration.millis(350), tableReclamation);
        ftTable.setFromValue(0.0);
        ftTable.setToValue(1.0);
        ftTable.play();
    }
}

