package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.RendezVous;
import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.RendezVousService;
import com.hospismart.hospismartdesktop.utils.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import com.hospismart.hospismartdesktop.models.Medicament;
import com.hospismart.hospismartdesktop.models.MouvementStock;
import com.hospismart.hospismartdesktop.services.MedicamentDAO;
import com.hospismart.hospismartdesktop.services.CategorieDAO;
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
import javafx.scene.shape.Circle;
import javafx.collections.FXCollections;
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
    @FXML private Label statRdvJour, statAttente, statPatients;
    @FXML private Label lblNextPatientName, lblNextPatientTime, lblNextPatientMotif;
    @FXML private ImageView imgNextPatient;
    @FXML private TableView<RendezVous> tableRdv;
    @FXML private TableColumn<RendezVous, String> colPatient, colDate, colStatut;
    @FXML private TableColumn<RendezVous, Void> colAction;
    // === KPI Labels ===
    @FXML private Label lblDate;
    @FXML private Label lblTotalMed;
    @FXML private Label lblTotalCat;
    @FXML private Label lblValeurStock;
    @FXML private Label lblAlertes;
    @FXML private Label lblTotalMouv;

    public StackPane getContentArea() { return contentArea; }
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

    @FXML
    public void initialize() {
        // 1. Initialisation Session (Uniquement si vide)
        if (UserSession.getUser() == null) {
            User user = new User();
            user.setId(4);
            user.setNom("Ferjani");
            user.setPrenom("Nour");
            UserSession.login(user);
        }
        // Date du jour
        lblDate.setText("📅 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy  —  HH:mm")));

        // Charger les données
        List<Medicament> medicaments = medicamentDAO.findAll();
        List<MouvementStock> mouvements = mouvementDAO.findAll();

        // 1. CARTES KPI
        loadKPIs(medicaments, mouvements);

        // 2. Sidebar (Éléments statiques)
        if (lblSidebarName != null) {
            lblSidebarName.setText("Dr. " + UserSession.getUser().getPrenom() + " " + UserSession.getUser().getNom());
        }
        // 2. GRAPHIQUES
        loadPieChart(medicaments);
        loadBarStock(medicaments);
        loadBarEntreesSorties(medicaments, mouvements);

        // 3. Charger l'accueil par défaut au premier lancement
        // On vérifie si contentArea est vide pour ne pas recharger si on est déjà dedans
        Platform.runLater(() -> {
            if (contentArea != null && contentArea.getChildren().isEmpty()) {
                loadPage("HomeDashboard.fxml");
            }
        });
        // 3. TABLE ALERTES
        loadTableAlertes(medicaments);
    }

    // ===================================================================
    // 1. CARTES KPI
    // ===================================================================
    private void loadKPIs(List<Medicament> medicaments, List<MouvementStock> mouvements) {
        // Total médicaments
        lblTotalMed.setText(String.valueOf(medicaments.size()));
    private void loadPage(String fxmlFileName) {
        if (isNavigating) return; // Bloque si une navigation est déjà en cours

        // Total catégories
        int nbCat = categorieDAO.findAll().size();
        lblTotalCat.setText(String.valueOf(nbCat));
        try {
            isNavigating = true;
            System.out.println("🔄 Tentative de navigation vers : " + fxmlFileName);

        // Valeur totale du stock = Σ (quantité × prix)
        double valeurTotale = medicaments.stream()
            .mapToDouble(m -> m.getQuantite() * m.getPrixUnitaire())
            .sum();
        lblValeurStock.setText(String.format("%.2f TND", valeurTotale));
            URL fxmlLocation = getClass().getResource("/com/hospismart/hospismartdesktop/views/" + fxmlFileName);
            if (fxmlLocation == null) fxmlLocation = getClass().getResource("/" + fxmlFileName);
            if (fxmlLocation == null) throw new IOException("FXML introuvable : " + fxmlFileName);

        // Nombre en alerte (stock ≤ seuil)
        long nbAlertes = medicaments.stream()
            .filter(m -> m.getQuantite() <= m.getSeuilAlerte())
            .count();
        lblAlertes.setText(String.valueOf(nbAlertes));
            FXMLLoader loader = new FXMLLoader(fxmlLocation);

        // Total mouvements
        lblTotalMouv.setText(String.valueOf(mouvements.size()));
    }
            // On ne force le contrôleur QUE pour le HomeDashboard
            if (fxmlFileName.equals("HomeDashboard.fxml")) {
                loader.setController(this);
            }

            Parent root = loader.load();
            contentArea.getChildren().setAll(root);
    // ===================================================================
    // 2. PIE CHART : Répartition par catégorie
    // ===================================================================
    private void loadPieChart(List<Medicament> medicaments) {
        // Grouper par catégorie
        Map<String, Long> parCategorie = medicaments.stream()
            .collect(Collectors.groupingBy(
                m -> m.getCategorieNom() != null && !m.getCategorieNom().isEmpty()
                    ? m.getCategorieNom() : "Sans catégorie",
                Collectors.counting()
            ));

            if (fxmlFileName.equals("HomeDashboard.fxml")) {
                refreshDashboardData();
            }
        List<PieChart.Data> pieData = parCategorie.entrySet().stream()
            .map(e -> new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()))
            .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        } finally {
            isNavigating = false; // Libère le verrou
        }
    }
        pieCategories.setData(FXCollections.observableArrayList(pieData));
        pieCategories.setLabelsVisible(true);
    }

    // ===================================================================
    // 3. BAR CHART : Top 6 stock par médicament
    // ===================================================================
    private void loadBarStock(List<Medicament> medicaments) {
        // Trier par quantité décroissante, prendre top 6
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

    private void refreshDashboardData() {
        if (lblWelcome == null) return; // Sécurité si le FXML n'est pas bien chargé
        barStock.getData().clear();
        barStock.getData().add(series);
        barStock.setLegendVisible(false);
        barStock.setCategoryGap(20);
        barStock.setBarGap(0);
        barStock.setAnimated(false);

        lblWelcome.setText("Bienvenue, Dr. " + UserSession.getUser().getPrenom());
        lblDate.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));

        try {
            int medecinId = UserSession.getUser().getId();

            // 1. Prochain Patient
            RendezVous nextRdv = rdvService.findNextByMedecin(medecinId);
            this.currentNextRdv = nextRdv;
            if (nextRdv != null) {
                lblNextPatientName.setText(nextRdv.getPatientName());
                lblNextPatientTime.setText(nextRdv.getDatetime().format(DateTimeFormatter.ofPattern("HH:mm")));
                lblNextPatientMotif.setText(nextRdv.getMotif());
            } else {
                lblNextPatientName.setText("Aucun patient");
                lblNextPatientTime.setText("--:--");
                lblNextPatientMotif.setText("Aucun RDV accepté");
            }
        // Coloriser les barres + Ajouter Tooltip
        for (XYChart.Data<String, Number> d : series.getData()) {
            d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    int val = d.getYValue().intValue();
                    // Trouver le médicament original pour le tooltip complet
                    Medicament med = top6.stream()
                        .filter(m -> (m.getNom().length() > 12 ? m.getNom().substring(0, 10) + ".." : m.getNom()).equals(d.getXValue()))
                        .findFirst().orElse(null);

                    if (med != null) {
                        javafx.scene.control.Tooltip.install(newNode,
                            new javafx.scene.control.Tooltip(med.getNom() + " : " + val + " unités"));

                        if (val <= med.getSeuilAlerte()) {
                            newNode.setStyle("-fx-bar-fill: #dc3545;"); // rouge si alerte
                        } else if (val < 50) {
                            newNode.setStyle("-fx-bar-fill: #ffc107;"); // jaune si bas
                        } else {
                            newNode.setStyle("-fx-bar-fill: #0d6efd;"); // bleu normal
                        }
                    }
                }
            });
        }
    }

            // 2. Stats (Dynamique pour RDV du jour et En Attente)
            List<RendezVous> allRdv = rdvService.findALL(); // Idéalement, créer des méthodes count directes
            long rdvJour = allRdv.stream()
                .filter(r -> r.getMedecinId() == medecinId && r.getDatetime().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();
            long enAttente = allRdv.stream()
                .filter(r -> r.getMedecinId() == medecinId && "En attente".equals(r.getStatut()))
                .count();

            statRdvJour.setText(String.valueOf(rdvJour));
            statAttente.setText(String.valueOf(enAttente));
            statPatients.setText("150"); // Placeholder pour le moment
    // ===================================================================
    // 4. BAR CHART : Entrées vs Sorties par médicament
    // ===================================================================
    private void loadBarEntreesSorties(List<Medicament> medicaments, List<MouvementStock> mouvements) {
        // Grouper les mouvements par médicament
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
        } catch (Exception e) {
            System.err.println("Erreur chargement Dashboard : " + e.getMessage());
        }

        setupTable();
    }

    private void setupTable() {
        if (tableRdv == null) return;
        // Prendre les médicaments qui ont le plus de mouvements (top 6)
        Set<String> allNames = new LinkedHashSet<>();
        allNames.addAll(entrees.keySet());
        allNames.addAll(sorties.keySet());

        // Trier par total mouvements
        List<String> sorted = allNames.stream()
            .sorted((a, b) -> {
                int totalA = entrees.getOrDefault(a, 0) + sorties.getOrDefault(a, 0);
                int totalB = entrees.getOrDefault(b, 0) + sorties.getOrDefault(b, 0);
                return Integer.compare(totalB, totalA);
            })
            .limit(6)
            .collect(Collectors.toList());
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
        XYChart.Series<String, Number> seriesEntrees = new XYChart.Series<>();
        seriesEntrees.setName("📥 Entrées");
        XYChart.Series<String, Number> seriesSorties = new XYChart.Series<>();
        seriesSorties.setName("📤 Sorties");

            {
                btnAccept.getStyleClass().add("btn-action-accept");
                btnRefuse.getStyleClass().add("btn-action-refuse");
        for (String nom : sorted) {
            String shortName = nom.length() > 12 ? nom.substring(0, 10) + ".." : nom;
            seriesEntrees.getData().add(new XYChart.Data<>(shortName, entrees.getOrDefault(nom, 0)));
            seriesSorties.getData().add(new XYChart.Data<>(shortName, sorties.getOrDefault(nom, 0)));
        }

                btnAccept.setOnAction(event -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    handleUpdateStatut(rdv.getId(), "Accepté");
                });
        barEntreesSorties.getData().clear();
        barEntreesSorties.getData().addAll(seriesEntrees, seriesSorties);
        barEntreesSorties.setCategoryGap(20);
        barEntreesSorties.setBarGap(4);
        barEntreesSorties.setAnimated(false);

                btnRefuse.setOnAction(event -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    handleUpdateStatut(rdv.getId(), "Annulé");
                });
                container.setAlignment(javafx.geometry.Pos.CENTER);
            }
        // Coloriser entrées en vert, sorties en rouge et ajouter tooltips
        for (XYChart.Data<String, Number> d : seriesEntrees.getData()) {
            d.nodeProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    n.setStyle("-fx-bar-fill: #198754;");
                    javafx.scene.control.Tooltip.install(n, new javafx.scene.control.Tooltip("Entrées : " + d.getYValue()));
                }
            });
        }
        for (XYChart.Data<String, Number> d : seriesSorties.getData()) {
            d.nodeProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    n.setStyle("-fx-bar-fill: #dc3545;");
                    javafx.scene.control.Tooltip.install(n, new javafx.scene.control.Tooltip("Sorties : " + d.getYValue()));
                }
            });
        }
    }

    // ===================================================================
    // 5. TABLE ALERTES
    // ===================================================================
    private void loadTableAlertes(List<Medicament> medicaments) {
        colAlNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colAlStock.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colAlSeuil.setCellValueFactory(new PropertyValueFactory<>("seuilAlerte"));
        colAlPeremption.setCellValueFactory(new PropertyValueFactory<>("datePeremption"));

        // Colorer le stock en rouge
        colAlStock.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setGraphic(container);
                    setText(item.toString());
                    setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                }
            }
        });

        try {
            int medecinId = UserSession.getUser().getId();
            List<RendezVous> data = rdvService.findPendingByMedecin(medecinId);
            tableRdv.setItems(FXCollections.observableArrayList(data));
        } catch (Exception e) {
            System.err.println("Erreur SQL Table : " + e.getMessage());
        }
    }

    @FXML public void showDashboard(ActionEvent e) { loadPage("HomeDashboard.fxml"); }
    @FXML public void showDemandesRdv(ActionEvent e) { loadPage("mesDemandesRDV.fxml"); }
    @FXML public void showDisponibilites(ActionEvent e) { loadPage("MedecinDisponibilites.fxml"); }
    @FXML public void showMesPatients(ActionEvent e) { loadPage("MesPatients.fxml"); }
        List<Medicament> enAlerte = medicaments.stream()
            .filter(m -> m.getQuantite() <= m.getSeuilAlerte())
            .sorted(Comparator.comparingInt(Medicament::getQuantite))
            .collect(Collectors.toList());

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
        tableAlertes.setItems(FXCollections.observableArrayList(enAlerte));
    }
}

    private void handleUpdateStatut(int rdvId, String nouveauStatut) {
        try {
            rdvService.updateStatut(rdvId, nouveauStatut);
            refreshDashboardData(); // Rafraîchir pour enlever le RDV traité
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du statut : " + e.getMessage());
        }
    }
}