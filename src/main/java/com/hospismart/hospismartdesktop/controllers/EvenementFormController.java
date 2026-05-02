package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Evenement;
import com.hospismart.hospismartdesktop.services.AiDescriptionService;
import com.hospismart.hospismartdesktop.services.EvenementService;
import com.hospismart.hospismartdesktop.utils.ApiConfig;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class EvenementFormController implements Initializable {

    @FXML private Label      formTitle;
    @FXML private Label      errorLabel;

    // ── Form fields ───────────────────────────────────
    @FXML private TextField        titreField;
    @FXML private TextArea         descriptionArea;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> statutCombo;
    @FXML private DatePicker       dateDebutPicker;
    @FXML private TextField        heureDebutField;
    @FXML private DatePicker       dateFinPicker;
    @FXML private TextField        heureFinField;
    @FXML private TextField        lieuField;
    @FXML private TextField        budgetField;

    // ── AI Description ────────────────────────────────
    @FXML private Button            aiGenerateBtn;
    @FXML private ProgressIndicator aiSpinner;
    @FXML private Label             aiStatusLabel;

    // ── Map ───────────────────────────────────────────
    @FXML private WebView mapWebView;

    private final EvenementService service = new EvenementService();
    private final AiDescriptionService aiService = new AiDescriptionService();
    private Evenement evenementToEdit = null;
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm");

    // Coordinates from map
    private Double selectedLat = null;
    private Double selectedLng = null;
    private final JavaBridge javaBridge = new JavaBridge();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeCombo.getItems().addAll("réunion", "formation", "visite", "maintenance", "autre");
        statutCombo.getItems().addAll("planifié", "en_cours", "terminé", "annulé");
        typeCombo.setValue("réunion");
        statutCombo.setValue("planifié");
        dateDebutPicker.setValue(LocalDate.now());
        dateFinPicker.setValue(LocalDate.now());
        heureDebutField.setText("09:00");
        heureFinField.setText("11:00");
        hideError();

        // ── Contrôle de Saisie (Live Validation) ──────────────────────────
        setupLiveValidation();

        // ── Initialize Map ────────────────────────────────────────────────
        initMap();
    }

    private void setupLiveValidation() {
        titreField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() < 3 || !newVal.matches("^[a-zA-Z0-9À-ÿ\\s\\-_']+$")) {
                titreField.setStyle("-fx-border-color: red;");
            } else {
                titreField.setStyle("");
            }
        });

        lieuField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty() || !newVal.matches("^[a-zA-Z0-9À-ÿ\\s\\-_',\\.]+$")) {
                lieuField.setStyle("-fx-border-color: red;");
            } else {
                lieuField.setStyle("");
            }
        });

        budgetField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("^[0-9]+(\\.[0-9]{1,2})?$")) {
                budgetField.setStyle("-fx-border-color: red;");
            } else {
                budgetField.setStyle("");
            }
        });
    }

    // ── Map Initialization ──────────────────────────────────────────────────

    private void initMap() {
        if (mapWebView == null) return;

        WebEngine engine = mapWebView.getEngine();
        engine.setJavaScriptEnabled(true);

        String geoapifyKey = ApiConfig.GEOAPIFY_API_KEY;
        boolean hasGeoKey = ApiConfig.isConfigured(geoapifyKey);

        String html = """
            <!DOCTYPE html>
            <html><head>
            <meta charset="utf-8"/>
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body { margin: 0; padding: 0; width: 100%%; height: 100%%; overflow: hidden; }
                #map { width: 100%%; height: 100%%; }
                #search-box {
                    position: absolute; top: 8px; left: 50px; z-index: 1000;
                    padding: 6px 12px; border: 1px solid #ccc; border-radius: 6px;
                    font-size: 13px; width: 220px; background: white;
                    box-shadow: 0 2px 6px rgba(0,0,0,0.15);
                }
            </style>
            </head><body>
            %s
            <div id="map"></div>
            <script>
                var map = L.map('map').setView([36.8, 10.18], 12);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap'
                }).addTo(map);
                setTimeout(function(){ map.invalidateSize(); }, 300);
                setTimeout(function(){ map.invalidateSize(); }, 1000);

                var marker = null;

                map.on('click', function(e) {
                    var lat = e.latlng.lat;
                    var lng = e.latlng.lng;
                    if (marker) map.removeLayer(marker);
                    marker = L.marker([lat, lng]).addTo(map);

                    // Reverse geocode with Nominatim (free)
                    fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng)
                        .then(r => r.json())
                        .then(data => {
                            var name = data.display_name || (lat.toFixed(4) + ', ' + lng.toFixed(4));
                            if (window.javaApp) {
                                window.javaApp.onLocationSelected(lat, lng, name);
                            }
                        })
                        .catch(() => {
                            if (window.javaApp) {
                                window.javaApp.onLocationSelected(lat, lng, lat.toFixed(4) + ', ' + lng.toFixed(4));
                            }
                        });
                });

                %s
            </script>
            </body></html>
            """.formatted(
                hasGeoKey ? "<input id='search-box' type='text' placeholder='Rechercher un lieu...' />" : "",
                hasGeoKey ? getSearchScript(geoapifyKey) : ""
        );

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaApp", javaBridge);
            }
        });

        engine.loadContent(html);
    }

    private String getSearchScript(String apiKey) {
        return """
            var searchBox = document.getElementById('search-box');
            var searchTimeout = null;
            searchBox.addEventListener('input', function() {
                clearTimeout(searchTimeout);
                var q = this.value.trim();
                if (q.length < 3) return;
                searchTimeout = setTimeout(function() {
                    fetch('https://api.geoapify.com/v1/geocode/autocomplete?text=' + encodeURIComponent(q) + '&apiKey=%s&limit=1')
                        .then(r => r.json())
                        .then(data => {
                            if (data.features && data.features.length > 0) {
                                var f = data.features[0];
                                var lat = f.geometry.coordinates[1];
                                var lng = f.geometry.coordinates[0];
                                var name = f.properties.formatted || q;
                                map.setView([lat, lng], 15);
                                if (marker) map.removeLayer(marker);
                                marker = L.marker([lat, lng]).addTo(map);
                                if (window.javaApp) {
                                    window.javaApp.onLocationSelected(lat, lng, name);
                                }
                            }
                        });
                }, 400);
            });
            """.formatted(apiKey);
    }

    /**
     * JavaScript bridge — receives clicked location from map.
     */
    public class JavaBridge {
        public void onLocationSelected(double lat, double lng, String name) {
            Platform.runLater(() -> {
                selectedLat = lat;
                selectedLng = lng;
                lieuField.setText(name);
            });
        }
    }

    /** Called by EvenementBackController before showing the stage. */
    public void setEvenement(Evenement evenement) {
        this.evenementToEdit = evenement;
        if (evenement == null) {
            formTitle.setText("➕  Ajouter un Événement");
            return;
        }
        formTitle.setText("✏  Modifier l'Événement");
        titreField.setText(nvl(evenement.getTitre()));
        descriptionArea.setText(nvl(evenement.getDescription()));
        lieuField.setText(nvl(evenement.getLieu()));
        budgetField.setText(evenement.getBudgetAlloue() > 0
                ? String.format("%.2f", evenement.getBudgetAlloue())
                : "");
        if (evenement.getTypeEvenement() != null) typeCombo.setValue(evenement.getTypeEvenement());
        if (evenement.getStatut()        != null) statutCombo.setValue(evenement.getStatut());
        if (evenement.getDateDebut() != null) {
            dateDebutPicker.setValue(evenement.getDateDebut().toLocalDate());
            heureDebutField.setText(evenement.getDateDebut().format(TF));
        }
        if (evenement.getDateFin() != null) {
            dateFinPicker.setValue(evenement.getDateFin().toLocalDate());
            heureFinField.setText(evenement.getDateFin().format(TF));
        }
        selectedLat = evenement.getLatitude();
        selectedLng = evenement.getLongitude();

        // Center map on existing location
        if (selectedLat != null && selectedLng != null && mapWebView != null) {
            WebEngine engine = mapWebView.getEngine();
            engine.getLoadWorker().stateProperty().addListener((obs, oldS, newS) -> {
                if (newS == Worker.State.SUCCEEDED) {
                    engine.executeScript(String.format(
                            "map.setView([%s, %s], 15); if(marker) map.removeLayer(marker); marker = L.marker([%s, %s]).addTo(map);",
                            selectedLat, selectedLng, selectedLat, selectedLng));
                }
            });
        }
    }

    // ── AI Description Generation ─────────────────────────────────────────────

    @FXML private void onGenerateAiDescription() {
        aiGenerateBtn.setDisable(true);
        aiSpinner.setVisible(true);
        aiSpinner.setManaged(true);
        aiStatusLabel.setText("Génération en cours...");

        String titre = trimOf(titreField.getText());
        String type = typeCombo.getValue();
        String lieu = trimOf(lieuField.getText());
        String dateDebut = dateDebutPicker.getValue() != null
                ? dateDebutPicker.getValue().toString() + "T" + trimOf(heureDebutField.getText())
                : "";
        String dateFin = dateFinPicker.getValue() != null
                ? dateFinPicker.getValue().toString() + "T" + trimOf(heureFinField.getText())
                : "";
        String budget = trimOf(budgetField.getText());

        Thread thread = new Thread(() -> {
            try {
                String description = aiService.generateDescription(titre, type, lieu, dateDebut, dateFin, budget);
                Platform.runLater(() -> {
                    descriptionArea.setText(description);
                    aiStatusLabel.setText("✅ Description générée !");
                    aiGenerateBtn.setDisable(false);
                    aiSpinner.setVisible(false);
                    aiSpinner.setManaged(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    aiStatusLabel.setText("❌ Erreur: " + e.getMessage());
                    aiGenerateBtn.setDisable(false);
                    aiSpinner.setVisible(false);
                    aiSpinner.setManaged(false);
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @FXML private void onSave() {
        hideError();

        // ── Validation avec Regex (Contrôle de saisie strict) ────────────────
        String titre = trimOf(titreField.getText());
        if (titre.isEmpty() || titre.length() < 3) { 
            showError("Le titre doit contenir au moins 3 caractères."); 
            return; 
        }
        if (!titre.matches("^[a-zA-Z0-9À-ÿ\\s\\-_']+$")) {
            showError("Le titre contient des caractères non autorisés.");
            return;
        }

        String lieu = trimOf(lieuField.getText());
        if (lieu.isEmpty()) { 
            showError("Le lieu est obligatoire."); 
            return; 
        }

        if (typeCombo.getValue()   == null) { showError("Sélectionnez un type d'événement."); return; }
        if (statutCombo.getValue() == null) { showError("Sélectionnez un statut."); return; }
        if (dateDebutPicker.getValue() == null) { showError("La date de début est obligatoire."); return; }
        if (dateFinPicker.getValue()   == null) { showError("La date de fin est obligatoire."); return; }

        // ── Parse times ───────────────────────────────
        LocalDateTime dateDebut, dateFin;
        try {
            dateDebut = LocalDateTime.of(dateDebutPicker.getValue(),
                    LocalTime.parse(trimOf(heureDebutField.getText()), TF));
        } catch (DateTimeParseException ex) {
            showError("Heure de début invalide — utilisez HH:mm (ex: 09:00).");
            return;
        }
        try {
            dateFin = LocalDateTime.of(dateFinPicker.getValue(),
                    LocalTime.parse(trimOf(heureFinField.getText()), TF));
        } catch (DateTimeParseException ex) {
            showError("Heure de fin invalide — utilisez HH:mm (ex: 11:00).");
            return;
        }
        if (!dateFin.isAfter(dateDebut)) {
            showError("La date de fin doit être postérieure à la date de début.");
            return;
        }

        // ── Parse budget ──────────────────────────────
        double budget = 0;
        String budgetText = trimOf(budgetField.getText()).replace(",", ".");
        if (!budgetText.isEmpty()) {
            try {
                budget = Double.parseDouble(budgetText);
                if (budget < 0) { showError("Le budget ne peut pas être négatif."); return; }
            } catch (NumberFormatException ex) {
                showError("Le budget doit être un nombre valide (ex: 500.00).");
                return;
            }
        }

        // ── Build & persist ───────────────────────────
        Evenement e = (evenementToEdit != null) ? evenementToEdit : new Evenement();
        e.setTitre(titre);
        e.setDescription(descriptionArea.getText());
        e.setTypeEvenement(typeCombo.getValue());
        e.setStatut(statutCombo.getValue());
        e.setLieu(lieu);
        e.setDateDebut(dateDebut);
        e.setDateFin(dateFin);
        e.setBudgetAlloue(budget);
        e.setLatitude(selectedLat);
        e.setLongitude(selectedLng);

        boolean ok = (evenementToEdit != null) ? service.update(e) : service.create(e);
        if (ok) {
            getStage().close();
        } else {
            showError("Erreur lors de l'enregistrement. Vérifiez la connexion à la base de données.");
        }
    }

    @FXML private void onCancel() {
        getStage().close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private Stage getStage() {
        return (Stage) titreField.getScene().getWindow();
    }

    private String trimOf(String s) { return s != null ? s.trim() : ""; }
    private String nvl(String s)    { return s != null ? s : ""; }
}
