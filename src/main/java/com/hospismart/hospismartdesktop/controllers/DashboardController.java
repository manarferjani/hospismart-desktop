package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.RendezVous;
import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.CategorieDAO;
import com.hospismart.hospismartdesktop.services.RendezVousService;
import com.hospismart.hospismartdesktop.utils.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import com.hospismart.hospismartdesktop.models.Medicament;
import com.hospismart.hospismartdesktop.models.MouvementStock;
import com.hospismart.hospismartdesktop.services.MedicamentDAO;
import com.hospismart.hospismartdesktop.services.MouvementStockDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur du Tableau de Bord.
 * Affiche les statistiques globales, graphiques et alertes de stock.
 */
public class DashboardController {

    @FXML private Label lblSidebarName, lblWelcome, lblDate;
    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard, btnDispo, btnMesPatients, btnPharmacie;
    @FXML private Label statRdvJour, statAttente, statPatients;
    @FXML private Label lblNextPatientName, lblNextPatientTime, lblNextPatientMotif;
    @FXML private ImageView imgNextPatient;
    @FXML private TableView<RendezVous> tableRdv;
    @FXML private TableColumn<RendezVous, String> colPatient, colDate, colStatut;
    @FXML private TableColumn<RendezVous, Void> colAction;

    // === KPI Labels ===
    @FXML private Label lblTotalMed;
    @FXML private Label lblTotalCat;
    @FXML private Label lblValeurStock;
    @FXML private Label lblAlertes;
    @FXML private Label lblTotalMouv;

    // === Charts ===
    @FXML private PieChart pieCategories;
    @FXML private BarChart<String, Number> barStock;
    @FXML private BarChart<String, Number> barEntreesSorties;

    // === Table Alertes ===
    @FXML private TableView<Medicament> tableAlertes;
    @FXML private TableColumn<Medicament, String> colAlNom;
    @FXML private TableColumn<Medicament, Integer> colAlStock;
    @FXML private TableColumn<Medicament, Integer> colAlSeuil;
    @FXML private TableColumn<Medicament, LocalDate> colAlPeremption;

    private final RendezVousService rdvService = new RendezVousService();
    private RendezVous currentNextRdv;
    private boolean isNavigating = false; // Sécurité anti-boucle
    private MedicamentDAO medicamentDAO = new MedicamentDAO();
    private CategorieDAO categorieDAO = new CategorieDAO();
    private MouvementStockDAO mouvementDAO = new MouvementStockDAO();

    public StackPane getContentArea() { return contentArea; }

    @FXML
    public void initialize() {
        // 2. Sidebar (Éléments statiques)
        if (lblSidebarName != null) {
            User u = Session.getInstance().getCurrentUser();
            if (u != null) {
                lblSidebarName.setText("Dr. " + u.getPrenom() + " " + u.getNom());
            } else {
                lblSidebarName.setText("Dr. Inconnu");
            }
        }

        // Date du jour
        if (lblDate != null) {
            lblDate.setText("📅 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy  —  HH:mm")));
        }

        // 3. Charger l'accueil par défaut au premier lancement (si applicable)
        Platform.runLater(() -> {
            if (contentArea != null && contentArea.getChildren().isEmpty()) {
                loadPage("HomeDashboard.fxml");
            } else {
                // Si on est dans le dashboard admin direct (dashboard.fxml)
                refreshDashboardData();
            }
        });
    }

    private void loadPage(String fxmlFileName) {
        if (isNavigating) return;

        try {
            isNavigating = true;
            System.out.println("🔄 Tentative de navigation vers : " + fxmlFileName);

            URL fxmlLocation = getClass().getResource("/com/hospismart/hospismartdesktop/views/" + fxmlFileName);
            if (fxmlLocation == null) fxmlLocation = getClass().getResource("/com/hospismart/hospismartdesktop/" + fxmlFileName);
            if (fxmlLocation == null) fxmlLocation = getClass().getResource("/" + fxmlFileName);
            if (fxmlLocation == null) throw new IOException("FXML introuvable : " + fxmlFileName);

            FXMLLoader loader = new FXMLLoader(fxmlLocation);

            // On ne force le contrôleur QUE pour le Dashboard Principal (Home ou Back)
            if (fxmlFileName.equals("HomeDashboard.fxml") || fxmlFileName.equals("BackDashboard.fxml")) {
                loader.setController(this);
            }

            Parent root = loader.load();
            contentArea.getChildren().setAll(root);

            // Rafraîchir les données si on est sur le dashboard
            if (fxmlFileName.equals("HomeDashboard.fxml") || fxmlFileName.equals("BackDashboard.fxml")) {
                refreshDashboardData();
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        } finally {
            isNavigating = false;
        }
    }

    private void refreshDashboardData() {
        // 1. Données de Bienvenue
        if (lblWelcome != null) {
            User u = Session.getInstance().getCurrentUser();
            lblWelcome.setText("Bienvenue, " + (u != null ? "Dr. " + u.getPrenom() : "Docteur"));
        }
        if (lblDate != null) {
            lblDate.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        }

        try {
            User u = Session.getInstance().getCurrentUser();
            int medecinId = (u != null) ? u.getId() : -1;

            // 2. Prochain Patient
            RendezVous nextRdv = rdvService.findNextByMedecin(medecinId);
            this.currentNextRdv = nextRdv;
            if (nextRdv != null && lblNextPatientName != null) {
                lblNextPatientName.setText(nextRdv.getPatientName());
                lblNextPatientTime.setText(nextRdv.getDatetime().format(DateTimeFormatter.ofPattern("HH:mm")));
                lblNextPatientMotif.setText(nextRdv.getMotif());
            } else if (lblNextPatientName != null) {
                lblNextPatientName.setText("Aucun patient");
                lblNextPatientTime.setText("--:--");
                lblNextPatientMotif.setText("Aucun RDV accepté");
            }

            // 3. Statistiques RDV
            List<RendezVous> allRdv = rdvService.findALL();
            long rdvJour = allRdv.stream()
                .filter(r -> r.getMedecinId() == medecinId && r.getDatetime().toLocalDate().equals(LocalDate.now()))
                .count();
            long enAttente = allRdv.stream()
                .filter(r -> r.getMedecinId() == medecinId && "En attente".equals(r.getStatut()))
                .count();

            if (statRdvJour != null) statRdvJour.setText(String.valueOf(rdvJour));
            if (statAttente != null) statAttente.setText(String.valueOf(enAttente));
            if (statPatients != null) statPatients.setText("150"); // Placeholder

            // 4. Charger les données Stock & Graphiques
            List<Medicament> medicaments = medicamentDAO.findAll();
            List<MouvementStock> mouvements = mouvementDAO.findAll();

            loadKPIs(medicaments, mouvements);
            loadPieChart(medicaments);
            loadBarStock(medicaments);
            loadBarEntreesSorties(medicaments, mouvements);
            loadTableAlertes(medicaments);

            // 5. Charger la table des RDV en attente
            setupTable();

        } catch (Exception e) {
            System.err.println("Erreur chargement Dashboard : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadKPIs(List<Medicament> medicaments, List<MouvementStock> mouvements) {
        if (lblTotalMed != null) lblTotalMed.setText(String.valueOf(medicaments.size()));

        if (lblTotalCat != null) {
            int nbCat = categorieDAO.findAll().size();
            lblTotalCat.setText(String.valueOf(nbCat));
        }

        if (lblValeurStock != null) {
            double valeurTotale = medicaments.stream()
                .mapToDouble(m -> m.getQuantite() * m.getPrixUnitaire())
                .sum();
            lblValeurStock.setText(String.format("%.2f TND", valeurTotale));
        }

        if (lblAlertes != null) {
            long nbAlertes = medicaments.stream()
                .filter(m -> m.getQuantite() <= m.getSeuilAlerte())
                .count();
            lblAlertes.setText(String.valueOf(nbAlertes));
        }

        if (lblTotalMouv != null) lblTotalMouv.setText(String.valueOf(mouvements.size()));
    }

    private void loadPieChart(List<Medicament> medicaments) {
        if (pieCategories == null) return;

        Map<String, Long> parCategorie = medicaments.stream()
            .collect(Collectors.groupingBy(
                m -> m.getCategorieNom() != null && !m.getCategorieNom().isEmpty()
                    ? m.getCategorieNom() : "Sans catégorie",
                Collectors.counting()
            ));

        List<PieChart.Data> pieData = parCategorie.entrySet().stream()
            .map(e -> new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()))
            .collect(Collectors.toList());

        pieCategories.setData(FXCollections.observableArrayList(pieData));
        pieCategories.setLabelsVisible(true);
    }

    private void loadBarStock(List<Medicament> medicaments) {
        if (barStock == null) return;

        List<Medicament> top6 = medicaments.stream()
            .sorted(Comparator.comparingInt(Medicament::getQuantite).reversed())
            .limit(6)
            .collect(Collectors.toList());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Quantité en stock");

        for (Medicament m : top6) {
            String shortName = m.getNom() != null && m.getNom().length() > 12
                ? m.getNom().substring(0, 10) + ".." : m.getNom();
            XYChart.Data<String, Number> data = new XYChart.Data<>(shortName, m.getQuantite());
            series.getData().add(data);
        }

        barStock.getData().clear();
        barStock.getData().add(series);
        barStock.setLegendVisible(false);
        barStock.setCategoryGap(20);
        barStock.setBarGap(0);
        barStock.setAnimated(false);

        // Coloriser les barres + Tooltips
        for (XYChart.Data<String, Number> d : series.getData()) {
            d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    int val = d.getYValue().intValue();
                    Medicament med = top6.stream()
                        .filter(m -> (m.getNom().length() > 12 ? m.getNom().substring(0, 10) + ".." : m.getNom()).equals(d.getXValue()))
                        .findFirst().orElse(null);

                    if (med != null) {
                        Tooltip.install(newNode, new Tooltip(med.getNom() + " : " + val + " unités"));
                        if (val <= med.getSeuilAlerte()) newNode.setStyle("-fx-bar-fill: #dc3545;");
                        else if (val < 50) newNode.setStyle("-fx-bar-fill: #ffc107;");
                        else newNode.setStyle("-fx-bar-fill: #0d6efd;");
                    }
                }
            });
        }
    }

    private void loadBarEntreesSorties(List<Medicament> medicaments, List<MouvementStock> mouvements) {
        if (barEntreesSorties == null) return;

        Map<String, Integer> entrees = new LinkedHashMap<>();
        Map<String, Integer> sorties = new LinkedHashMap<>();

        for (MouvementStock mv : mouvements) {
            String nom = mv.getMedicamentNom() != null ? mv.getMedicamentNom() : "ID:" + mv.getMedicamentId();
            if ("ENTREE".equalsIgnoreCase(mv.getType())) {
                entrees.merge(nom, mv.getQuantite(), Integer::sum);
            } else {
                sorties.merge(nom, mv.getQuantite(), Integer::sum);
            }
        }

        Set<String> allNames = new LinkedHashSet<>();
        allNames.addAll(entrees.keySet());
        allNames.addAll(sorties.keySet());

        List<String> sorted = allNames.stream()
            .sorted((a, b) -> {
                int totalA = entrees.getOrDefault(a, 0) + sorties.getOrDefault(a, 0);
                int totalB = entrees.getOrDefault(b, 0) + sorties.getOrDefault(b, 0);
                return Integer.compare(totalB, totalA);
            })
            .limit(6)
            .collect(Collectors.toList());

        XYChart.Series<String, Number> seriesEntrees = new XYChart.Series<>();
        seriesEntrees.setName("📥 Entrées");
        XYChart.Series<String, Number> seriesSorties = new XYChart.Series<>();
        seriesSorties.setName("📤 Sorties");

        for (String nom : sorted) {
            String shortName = nom.length() > 12 ? nom.substring(0, 10) + ".." : nom;
            seriesEntrees.getData().add(new XYChart.Data<>(shortName, entrees.getOrDefault(nom, 0)));
            seriesSorties.getData().add(new XYChart.Data<>(shortName, sorties.getOrDefault(nom, 0)));
        }

        barEntreesSorties.getData().clear();
        barEntreesSorties.getData().addAll(seriesEntrees, seriesSorties);
        barEntreesSorties.setCategoryGap(20);
        barEntreesSorties.setBarGap(4);
        barEntreesSorties.setAnimated(false);

        // Tooltips
        for (XYChart.Data<String, Number> d : seriesEntrees.getData()) {
            d.nodeProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    n.setStyle("-fx-bar-fill: #198754;");
                    Tooltip.install(n, new Tooltip("Entrées : " + d.getYValue()));
                }
            });
        }
        for (XYChart.Data<String, Number> d : seriesSorties.getData()) {
            d.nodeProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    n.setStyle("-fx-bar-fill: #dc3545;");
                    Tooltip.install(n, new Tooltip("Sorties : " + d.getYValue()));
                }
            });
        }
    }

    private void setupTable() {
        if (tableRdv == null) return;

        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(cellData -> {
            String date = cellData.getValue().getDatetime().format(DateTimeFormatter.ofPattern("HH:mm"));
            return new javafx.beans.property.SimpleStringProperty(date);
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnAccept = new Button("✅");
            private final Button btnRefuse = new Button("❌");
            private final HBox container = new HBox(8, btnAccept, btnRefuse);

            {
                btnAccept.getStyleClass().add("btn-action-accept");
                btnRefuse.getStyleClass().add("btn-action-refuse");
                btnAccept.setOnAction(event -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    handleUpdateStatut(rdv.getId(), "Accepté");
                });
                btnRefuse.setOnAction(event -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    handleUpdateStatut(rdv.getId(), "Annulé");
                });
                container.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });

        try {
            User u = Session.getInstance().getCurrentUser();
            int medecinId = (u != null) ? u.getId() : -1;
            List<RendezVous> data = rdvService.findPendingByMedecin(medecinId);
            tableRdv.setItems(FXCollections.observableArrayList(data));
        } catch (Exception e) {
            System.err.println("Erreur SQL Table : " + e.getMessage());
        }
    }

    private void loadTableAlertes(List<Medicament> medicaments) {
        if (tableAlertes == null) return;

        colAlNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colAlStock.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colAlSeuil.setCellValueFactory(new PropertyValueFactory<>("seuilAlerte"));
        colAlPeremption.setCellValueFactory(new PropertyValueFactory<>("datePeremption"));

        colAlStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                }
            }
        });

        List<Medicament> enAlerte = medicaments.stream()
            .filter(m -> m.getQuantite() <= m.getSeuilAlerte())
            .sorted(Comparator.comparingInt(Medicament::getQuantite))
            .collect(Collectors.toList());

        tableAlertes.setItems(FXCollections.observableArrayList(enAlerte));
    }

    private void handleUpdateStatut(int rdvId, String nouveauStatut) {
        try {
            rdvService.updateStatut(rdvId, nouveauStatut);
            refreshDashboardData();
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du statut : " + e.getMessage());
        }
    }

    @FXML public void showDashboard(ActionEvent e) { loadPage("HomeDashboard.fxml"); }
    @FXML public void showDemandesRdv(ActionEvent e) { loadPage("mesDemandesRDV.fxml"); }
    @FXML public void showDisponibilites(ActionEvent e) { loadPage("MedecinDisponibilites.fxml"); }
    @FXML
    public void showMesPatients(ActionEvent event) {
        setActiveMenu(btnMesPatients);
        loadPage("MesPatients.fxml");
    }

    @FXML
    public void showPharmacie(ActionEvent event) {
        setActiveMenu(btnPharmacie);
        loadPage("medicament_medecin.fxml");
    }

    private void setActiveMenu(Button activeBtn) {
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("menu-button-active");
        if (btnDispo != null) btnDispo.getStyleClass().remove("menu-button-active");
        if (btnMesPatients != null) btnMesPatients.getStyleClass().remove("menu-button-active");
        if (btnPharmacie != null) btnPharmacie.getStyleClass().remove("menu-button-active");

        if (btnDashboard != null && !btnDashboard.getStyleClass().contains("menu-button")) btnDashboard.getStyleClass().add("menu-button");
        if (btnDispo != null && !btnDispo.getStyleClass().contains("menu-button")) btnDispo.getStyleClass().add("menu-button");
        if (btnMesPatients != null && !btnMesPatients.getStyleClass().contains("menu-button")) btnMesPatients.getStyleClass().add("menu-button");
        if (btnPharmacie != null && !btnPharmacie.getStyleClass().contains("menu-button")) btnPharmacie.getStyleClass().add("menu-button");

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("menu-button-active");
        }
    }

    @FXML
    public void handleStartConsultation(ActionEvent event) {
        if (currentNextRdv == null) {
            System.err.println("Aucun patient à consulter");
            return;
        }

        try {
            URL fxmlLocation = getClass().getResource("/com/hospismart/hospismartdesktop/views/NewConsultation.fxml");
            if (fxmlLocation == null) fxmlLocation = getClass().getResource("/NewConsultation.fxml");

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            NewConsultationController controller = loader.getController();
            controller.setRendezVous(currentNextRdv);
            controller.setDashboardController(this);

            contentArea.getChildren().setAll(root);
        } catch (IOException e) {
            System.err.println("Erreur chargement NewConsultation.fxml : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            com.hospismart.hospismartdesktop.utils.Session.getInstance().cleanUserSession();
            
            URL fxmlLocation = getClass().getResource("/com/hospismart/hospismartdesktop/Login.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root, 1200, 768));
            stage.centerOnScreen();
        } catch (Exception e) {
            System.err.println("Erreur de déconnexion : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
