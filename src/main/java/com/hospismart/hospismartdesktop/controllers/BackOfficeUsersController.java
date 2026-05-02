package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.UserService;
import com.hospismart.hospismartdesktop.utils.Session;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BackOfficeUsersController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colType;
    @FXML private TableColumn<User, String> colStatut;
    @FXML private TableColumn<User, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;

    private UserService userService = new UserService();
    private ObservableList<User> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize columns
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getId())));
        colNom.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNom() + " " + cellData.getValue().getPrenom()));
        colEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType()));
        colStatut.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().isActive() ? "Actif" : "Inactif"));

        setupActionColumn();
        
        filterComboBox.setValue("TOUS");
        loadData();
    }

    private void loadData() {
        masterData.clear();
        masterData.addAll(userService.afficher());
        usersTable.setItems(masterData);
    }

    private void setupActionColumn() {
        colActions.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button btnEditer = new Button("Éditer");
            private final Button btnActiver = new Button("Activer");
            private final Button btnDesactiver = new Button("Désactiv.");
            private final Button btnSupprimer = new Button("Suppr.");
            private final HBox pane = new HBox(5, btnEditer, btnActiver, btnDesactiver, btnSupprimer);

            {
                btnEditer.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                btnActiver.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                btnDesactiver.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                btnSupprimer.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");

                btnEditer.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    openEditDialog(user);
                });

                btnActiver.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    userService.activerCompte(user.getId());
                    loadData();
                });

                btnDesactiver.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    userService.desactiverCompte(user.getId());
                    loadData();
                });

                btnSupprimer.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Confirmation");
                    alert.setHeaderText("Supprimer l'utilisateur ?");
                    alert.setContentText("Êtes-vous sûr de vouloir supprimer " + user.getNom() + " ?");
                    Optional<ButtonType> result = alert.showAndWait();
                        boolean deleted = userService.supprimer(user.getId());
                        if (!deleted) {
                            Alert err = new Alert(Alert.AlertType.ERROR, "La suppression a échoué. Le compte est potentiellement lié à d'autres données.");
                            err.showAndWait();
                        }
                        loadData();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    btnActiver.setVisible(!user.isActive());
                    btnActiver.setManaged(!user.isActive());
                    btnDesactiver.setVisible(user.isActive());
                    btnDesactiver.setManaged(user.isActive());
                    setGraphic(pane);
                }
            }
        });
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().toLowerCase();
        String typeFilter = filterComboBox.getValue();

        List<User> filteredList = masterData.stream().filter(u -> {
            boolean matchesKeyword = keyword.isEmpty() || 
                u.getNom().toLowerCase().contains(keyword) || 
                u.getPrenom().toLowerCase().contains(keyword) || 
                u.getEmail().toLowerCase().contains(keyword);
            
            boolean matchesType = "TOUS".equals(typeFilter) || (u.getType() != null && u.getType().toUpperCase().contains(typeFilter.toUpperCase()));
            
            return matchesKeyword && matchesType;
        }).collect(Collectors.toList());

        usersTable.setItems(FXCollections.observableArrayList(filteredList));
    }

    @FXML
    void handleExportPDF(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.setInitialFileName("Liste_Utilisateurs_Hospismart.pdf");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf"));
        
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File targetFile = fileChooser.showSaveDialog(stage);

        if (targetFile == null) return; // L'utilisateur a annulé

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(targetFile));
            document.open();
            
            com.itextpdf.text.Font titleFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 22, new com.itextpdf.text.BaseColor(21, 101, 192)); // #1565C0
            com.itextpdf.text.Font headFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 12, com.itextpdf.text.BaseColor.WHITE);
            com.itextpdf.text.Font dataFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 11, com.itextpdf.text.BaseColor.DARK_GRAY);

            Paragraph title = new Paragraph("Annuaire des Utilisateurs - HospiSmart\n\n", titleFont);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(title);
            
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(15f);

            // En-têtes stylisés
            String[] headers = {"ID", "Utilisateur", "Email", "Rôle", "Statut"};
            for (String header : headers) {
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(header, headFont));
                cell.setBackgroundColor(new com.itextpdf.text.BaseColor(33, 150, 243));
                cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                cell.setPadding(10f);
                cell.setBorderColor(com.itextpdf.text.BaseColor.WHITE);
                table.addCell(cell);
            }

            // Remplissage des données
            for (User u : usersTable.getItems()) {
                com.itextpdf.text.pdf.PdfPCell[] cells = {
                    new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(String.valueOf(u.getId()), dataFont)),
                    new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(u.getNom() + " " + u.getPrenom(), dataFont)),
                    new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(u.getEmail(), dataFont)),
                    new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(u.getType(), dataFont)),
                    new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(u.isActive() ? "Actif" : "Inactif", dataFont))
                };
                
                for(int i=0; i<cells.length; i++) {
                    cells[i].setPadding(8f);
                    cells[i].setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    cells[i].setBorderColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
                    if(i == 4 && !u.isActive()) {
                        cells[i].setBackgroundColor(new com.itextpdf.text.BaseColor(253, 237, 237)); // fond rouge léger pour inactif
                    }
                    table.addCell(cells[i]);
                }
            }
            
            document.add(table);
            document.close();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export PDF");
            alert.setHeaderText("Succès");
            alert.setContentText("PDF généré et sauvegardé sur votre Bureau :\n" + targetFile.getAbsolutePath());
            alert.showAndWait();

        } catch (Throwable e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur fatale lors de l'export PDF :\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    void handleShowStats(ActionEvent event) {
        Stage stage = new Stage();
        stage.setTitle("Statistiques des Utilisateurs");

        long adminCount = masterData.stream().filter(u -> u.getType() != null && u.getType().toUpperCase().contains("ADMIN")).count();
        long patientCount = masterData.stream().filter(u -> u.getType() != null && u.getType().toUpperCase().contains("PATIENT")).count();
        long medecinCount = masterData.stream().filter(u -> u.getType() != null && u.getType().toUpperCase().contains("MEDECIN")).count();

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Admins (" + adminCount + ")", adminCount),
                new PieChart.Data("Patients (" + patientCount + ")", patientCount),
                new PieChart.Data("Médecins (" + medecinCount + ")", medecinCount)
        );
        PieChart chart = new PieChart(pieChartData);

        Scene scene = new Scene(chart, 400, 400);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    void handleOpenAddDialog(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/AddUserBack.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Ajouter un Nouvel Utilisateur Administratif");
            stage.setScene(scene);
            stage.showAndWait();
            loadData(); // actualiser après fermeture
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur au chargement du formulaire.");
            alert.show();
        }
    }

    private void openEditDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/EditUserBack.fxml"));
            Scene scene = new Scene(loader.load());
            
            com.hospismart.hospismartdesktop.controllers.EditUserBackController controller = loader.getController();
            controller.setUser(user);
            
            Stage stage = new Stage();
            stage.setTitle("Éditer l'utilisateur");
            stage.setScene(scene);
            stage.showAndWait();
            loadData(); // refresh on close
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur au chargement du formulaire d'édition.");
            alert.show();
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Session.getInstance().cleanUserSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hospismart/hospismartdesktop/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
