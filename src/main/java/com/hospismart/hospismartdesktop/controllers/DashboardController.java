package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Medicament;
import com.hospismart.hospismartdesktop.models.MouvementStock;
import com.hospismart.hospismartdesktop.services.MedicamentDAO;
import com.hospismart.hospismartdesktop.services.CategorieDAO;
import com.hospismart.hospismartdesktop.services.MouvementStockDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur du Tableau de Bord.
 * Affiche les statistiques globales, graphiques et alertes de stock.
 */
public class DashboardController {

    // === KPI Labels ===
    @FXML private Label lblDate;
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

    private MedicamentDAO medicamentDAO = new MedicamentDAO();
    private CategorieDAO categorieDAO = new CategorieDAO();
    private MouvementStockDAO mouvementDAO = new MouvementStockDAO();

    @FXML
    public void initialize() {
        // Date du jour
        lblDate.setText("📅 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy  —  HH:mm")));

        // Charger les données
        List<Medicament> medicaments = medicamentDAO.findAll();
        List<MouvementStock> mouvements = mouvementDAO.findAll();

        // 1. CARTES KPI
        loadKPIs(medicaments, mouvements);

        // 2. GRAPHIQUES
        loadPieChart(medicaments);
        loadBarStock(medicaments);
        loadBarEntreesSorties(medicaments, mouvements);

        // 3. TABLE ALERTES
        loadTableAlertes(medicaments);
    }

    // ===================================================================
    // 1. CARTES KPI
    // ===================================================================
    private void loadKPIs(List<Medicament> medicaments, List<MouvementStock> mouvements) {
        // Total médicaments
        lblTotalMed.setText(String.valueOf(medicaments.size()));

        // Total catégories
        int nbCat = categorieDAO.findAll().size();
        lblTotalCat.setText(String.valueOf(nbCat));

        // Valeur totale du stock = Σ (quantité × prix)
        double valeurTotale = medicaments.stream()
            .mapToDouble(m -> m.getQuantite() * m.getPrixUnitaire())
            .sum();
        lblValeurStock.setText(String.format("%.2f TND", valeurTotale));

        // Nombre en alerte (stock ≤ seuil)
        long nbAlertes = medicaments.stream()
            .filter(m -> m.getQuantite() <= m.getSeuilAlerte())
            .count();
        lblAlertes.setText(String.valueOf(nbAlertes));

        // Total mouvements
        lblTotalMouv.setText(String.valueOf(mouvements.size()));
    }

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

        List<PieChart.Data> pieData = parCategorie.entrySet().stream()
            .map(e -> new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()))
            .collect(Collectors.toList());

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

        barStock.getData().clear();
        barStock.getData().add(series);
        barStock.setLegendVisible(false);
        barStock.setCategoryGap(20);
        barStock.setBarGap(0);
        barStock.setAnimated(false);

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
}
