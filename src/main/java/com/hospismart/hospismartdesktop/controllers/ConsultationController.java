package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Consultation;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;

public class ConsultationController {

    @FXML
    private TableView<Consultation> tableConsultations;

    @FXML
    private TableColumn<Consultation, Integer> colId;

    @FXML
    private TableColumn<Consultation, LocalDateTime> colDate;

    @FXML
    private TableColumn<Consultation, String> colStatut;

    @FXML
    private TableColumn<Consultation, String> colMotif;

    @FXML
    private TableColumn<Consultation, String> colObservations;

    @FXML
    public void initialize() {
        // Liaison des colonnes avec les attributs de la classe Consultation
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateHeure"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motif"));
        colObservations.setCellValueFactory(new PropertyValueFactory<>("observations"));

        // Pour les actions (show/edit), on utilise souvent une cellule personnalisée (Button)
        // ou on gère la sélection de la ligne.
    }
}