package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.UserService;
import com.hospismart.hospismartdesktop.utils.UserSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MesPatientsController implements Initializable {

    @FXML
    private Label lblCount;
    @FXML
    private VBox patientsTableContainer;
    @FXML
    private TableView<User> tablePatients;
    @FXML
    private TableColumn<User, String> colNomPrenom;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colTelephone;
    @FXML
    private TableColumn<User, String> colGenre;
    @FXML
    private TableColumn<User, String> colSang;
    @FXML
    private TableColumn<User, Void> colActions;
    @FXML
    private VBox emptyState;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colNomPrenom.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            String fullName = "";
            if (u.getNom() != null)
                fullName += u.getNom().toUpperCase();
            if (u.getPrenom() != null)
                fullName += " " + u.getPrenom();
            return new SimpleStringProperty(fullName.trim());
        });
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colGenre.setCellValueFactory(new PropertyValueFactory<>("genre"));
        colSang.setCellValueFactory(new PropertyValueFactory<>("groupeSanguin"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnHistory = new Button("📖 Historique");

            {
                btnHistory.getStyleClass().add("btn-outline-primary");
                btnHistory.setOnAction(event -> {
                    User patient = getTableView().getItems().get(getIndex());
                    handleShowHistory(patient);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnHistory);
                }
            }
        });
    }

    private void handleShowHistory(User patient) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PatientHistoryModal.fxml"));
            Parent root = loader.load();

            PatientHistoryModalController controller = loader.getController();
            controller.initData(patient, UserSession.getUser().getId());

            Stage stage = new Stage();
            stage.setTitle("Historique du Patient");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Impossible d'ouvrir l'historique : " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadData() {
        try {
            if (UserSession.getUser() == null)
                return;

            int medecinId = UserSession.getUser().getId();
            List<User> patients = userService.findPatientsByMedecin(medecinId);

            if (patients.isEmpty()) {
                tablePatients.setVisible(false);
                tablePatients.setManaged(false);
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                lblCount.setText("Aucun patient.");
            } else {
                tablePatients.setVisible(true);
                tablePatients.setManaged(true);
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                tablePatients.setItems(FXCollections.observableArrayList(patients));
                lblCount.setText(patients.size() + " patient(s) rattaché(s) à vos rendez-vous.");
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement des patients : " + e.getMessage());
        }
    }
}
