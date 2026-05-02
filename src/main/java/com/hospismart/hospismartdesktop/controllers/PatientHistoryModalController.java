package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.RendezVous;
import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.RendezVousService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PatientHistoryModalController {

    @FXML private Label lblPatientName;
    @FXML private TableView<RendezVous> tableHistory;
    @FXML private TableColumn<RendezVous, String> colDate;
    @FXML private TableColumn<RendezVous, String> colMotif;
    @FXML private TableColumn<RendezVous, String> colStatut;

    private final RendezVousService rdvService = new RendezVousService();

    @FXML
    public void initialize() {
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motif"));
        
        colDate.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getDatetime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            return new SimpleStringProperty(date);
        });

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
    }

    public void initData(User patient, int medecinId) {
        String fullName = "";
        if(patient.getNom() != null) fullName += patient.getNom().toUpperCase();
        if(patient.getPrenom() != null) fullName += " " + patient.getPrenom();
        lblPatientName.setText("Historique : " + fullName.trim());

        try {
            List<RendezVous> history = rdvService.findHistoryByPatientAndMedecin(patient.getId(), medecinId);
            tableHistory.setItems(FXCollections.observableArrayList(history));
        } catch (Exception e) {
            System.err.println("Erreur chargement de l'historique modal : " + e.getMessage());
        }
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) lblPatientName.getScene().getWindow();
        stage.close();
    }
}
