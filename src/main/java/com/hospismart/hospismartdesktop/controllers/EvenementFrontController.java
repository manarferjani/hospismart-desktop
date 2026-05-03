package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.models.Evenement;
import com.hospismart.hospismartdesktop.models.Inscription;
import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.services.EvenementService;
import com.hospismart.hospismartdesktop.services.InscriptionService;
import com.hospismart.hospismartdesktop.services.WeatherService;
import com.hospismart.hospismartdesktop.utils.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class EvenementFrontController implements Initializable {

    @FXML private FlowPane         cardsContainer;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Label            totalLabel;

    private final EvenementService   service     = new EvenementService();
    private final InscriptionService inscService = new InscriptionService();
    private final WeatherService     weatherService = new WeatherService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

    // Weather cache
    private Map<Integer, Map<String, Object>> weatherCache = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeFilter.getItems().addAll("Tous", "reunion", "formation", "visite", "maintenance", "autre");
        typeFilter.setValue("Tous");
        loadData();
    }

    public void loadData() {
        renderCards("", "");
    }

    @FXML private void onSearch() {
        String kw   = searchField.getText();
        String type = "Tous".equals(typeFilter.getValue()) ? "" : typeFilter.getValue();
        renderCards(kw, type);
    }

    @FXML private void onRefresh() {
        searchField.clear();
        typeFilter.setValue("Tous");
        weatherCache = null; // force refresh
        loadData();
    }

    @FXML private void onRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/welcome-view.fxml"));
            Scene scene = new Scene(loader.load(), 900, 620);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setTitle("HospiSmart — Bienvenue");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Card rendering ────────────────────────────────────────────────────────

    private void renderCards(String keyword, String type) {
        List<Evenement> list = service.search(keyword, type, "");
        cardsContainer.getChildren().clear();
        for (Evenement e : list) cardsContainer.getChildren().add(buildCard(e));
        if (totalLabel != null) totalLabel.setText(list.size() + " evenements disponibles");

        // Load weather in background
        loadWeatherAsync(list);
    }

    private void loadWeatherAsync(List<Evenement> events) {
        Thread thread = new Thread(() -> {
            try {
                Map<Integer, Map<String, Object>> forecasts = weatherService.getForecastsForEvents(events);
                Platform.runLater(() -> {
                    weatherCache = forecasts;
                    // Re-render cards with weather data
                    List<Node> cards = cardsContainer.getChildren().stream().toList();
                    for (int i = 0; i < cards.size() && i < events.size(); i++) {
                        Evenement ev = events.get(i);
                        Map<String, Object> weather = forecasts.get(ev.getId());
                        if (weather != null && cards.get(i) instanceof VBox card) {
                            addWeatherToCard(card, weather);
                        }
                    }
                });
            } catch (Exception ex) {
                System.err.println("Weather loading error: " + ex.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void addWeatherToCard(VBox card, Map<String, Object> weather) {
        // Find the card-body-front VBox inside the card
        for (Node child : card.getChildren()) {
            if (child instanceof VBox body && body.getStyleClass().contains("card-body-front")) {
                // Check if weather already added
                if (body.getChildren().stream().anyMatch(n -> "weather-box".equals(n.getId()))) return;

                HBox weatherBox = new HBox(6);
                weatherBox.setId("weather-box");
                weatherBox.setAlignment(Pos.CENTER);
                weatherBox.setPadding(new Insets(6, 8, 6, 8));
                weatherBox.setStyle(
                        "-fx-background-color: linear-gradient(to right, #eff6ff, #f0fdf4);" +
                        "-fx-background-radius: 8; -fx-border-color: #e0e7ff; " +
                        "-fx-border-radius: 8; -fx-border-width: 1;");

                // Weather icon
                String iconUrl = (String) weather.get("condition_icon");
                if (iconUrl != null) {
                    try {
                        ImageView iconView = new ImageView(new Image(iconUrl, 28, 28, true, true, true));
                        weatherBox.getChildren().add(iconView);
                    } catch (Exception ignored) {}
                }

                // Temperature
                Double temp = (Double) weather.get("temp_c");
                if (temp != null) {
                    Label tempLabel = new Label(String.format("%.0f°C", temp));
                    tempLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e40af;");
                    weatherBox.getChildren().add(tempLabel);
                }

                // Rain chance
                Double rain = (Double) weather.get("chance_of_rain");
                if (rain != null && rain > 0) {
                    Label rainLabel = new Label(String.format("🌧 %.0f%%", rain));
                    rainLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
                    weatherBox.getChildren().add(rainLabel);
                }

                // Condition text
                String condText = (String) weather.get("condition_text");
                if (condText != null) {
                    Label condLabel = new Label(condText);
                    condLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
                    condLabel.setMaxWidth(80);
                    condLabel.setWrapText(true);
                    weatherBox.getChildren().add(condLabel);
                }

                // Insert before the button (last child)
                int insertIdx = Math.max(0, body.getChildren().size() - 1);
                body.getChildren().add(insertIdx, weatherBox);
                break;
            }
        }
    }

    /**
     * Card matching web front template (evenements.html.twig)
     * Structure: blue header (title + status badge) → body (type, desc, details, button)
     */
    private VBox buildCard(Evenement e) {
        VBox card = new VBox(0);
        card.getStyleClass().add("event-card-front");

        // ── Blue Header ── matching .front-card-header ───────────
        HBox header = new HBox(8);
        header.getStyleClass().add("card-header-front");
        header.setAlignment(Pos.CENTER_LEFT);

        Label titre = new Label(nvl(e.getTitre()));
        titre.getStyleClass().add("card-header-title");
        titre.setWrapText(true);
        titre.setMaxWidth(170);
        HBox.setHgrow(titre, Priority.ALWAYS);
        header.getChildren().add(titre);

        // Status badge in header
        if (e.getStatut() != null) {
            Label statusBadge = new Label(e.getStatut().substring(0, 1).toUpperCase() + e.getStatut().substring(1));
            String pillClass = switch (e.getStatut()) {
                case "planifié", "planifie" -> "pill-planifie";
                case "en_cours" -> "pill-en-cours";
                case "terminé", "termine" -> "pill-termine";
                case "annulé", "annule" -> "pill-annule";
                default -> "pill-planifie";
            };
            statusBadge.getStyleClass().add(pillClass);
            header.getChildren().add(statusBadge);
        }

        card.getChildren().add(header);

        // ── Card Body ── matching .front-card-body ───────────────
        VBox body = new VBox(6);
        body.getStyleClass().add("card-body-front");

        // Type badge
        if (e.getTypeEvenement() != null && !e.getTypeEvenement().isBlank()) {
            Label typeLabel = new Label(e.getTypeEvenement().substring(0, 1).toUpperCase() + e.getTypeEvenement().substring(1));
            typeLabel.getStyleClass().add("type-badge");
            body.getChildren().add(typeLabel);
        }

        // Description (truncated)
        String desc = nvl(e.getDescription());
        if (!desc.isBlank()) {
            String truncated = desc.length() > 100 ? desc.substring(0, 100) + "..." : desc;
            Label descLabel = new Label(truncated);
            descLabel.getStyleClass().add("card-desc-front");
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(250);
            body.getChildren().add(descLabel);
        }

        // Separator
        Region sep = new Region();
        sep.setStyle("-fx-background-color: #e2e8f0; -fx-min-height: 1; -fx-pref-height: 1;");
        sep.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(sep, new Insets(4, 0, 4, 0));
        body.getChildren().add(sep);

        // Detail lines — matching web template structure
        if (e.getDateDebut() != null) {
            Label dateLabel = new Label("Debut: " + e.getDateDebut().format(FMT));
            dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            body.getChildren().add(dateLabel);
        }
        if (e.getDateFin() != null) {
            Label dateFinLabel = new Label("Fin: " + e.getDateFin().format(FMT));
            dateFinLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            body.getChildren().add(dateFinLabel);
        }
        if (e.getLieu() != null && !e.getLieu().isBlank()) {
            Label lieuLabel = new Label("Lieu: " + e.getLieu());
            lieuLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            lieuLabel.setMaxWidth(250);
            lieuLabel.setWrapText(true);
            body.getChildren().add(lieuLabel);
        }
        if (e.getBudgetAlloue() > 0) {
            Label budgetLabel = new Label(String.format("Budget: %.0f TND", e.getBudgetAlloue()));
            budgetLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            body.getChildren().add(budgetLabel);
        }

        // Participants
        int participantCount = inscService.countByEvenement(e.getId());
        Label partLabel = new Label("Participants: " + participantCount);
        partLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        body.getChildren().add(partLabel);

        // ── M'inscrire button ── full width like web ─────────────
        Button inscrireBtn = new Button("M'inscrire");
        inscrireBtn.getStyleClass().add("btn-inscrire");
        inscrireBtn.setMaxWidth(Double.MAX_VALUE);
        inscrireBtn.setOnAction(ev -> {
            try {
                showInscriptionDialog(e);
            } catch (Throwable ex) {
                ex.printStackTrace();
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                a.setTitle("Erreur");
                a.setHeaderText("Erreur lors de l'ouverture");
                a.setContentText(ex.getMessage());
                a.showAndWait();
            }
        });

        if ("annule".equals(e.getStatut()) || "termine".equals(e.getStatut())
                || "annulé".equals(e.getStatut()) || "terminé".equals(e.getStatut())) {
            inscrireBtn.setDisable(true);
            inscrireBtn.setText("Inscriptions fermees");
        }

        VBox.setMargin(inscrireBtn, new Insets(6, 0, 0, 0));
        body.getChildren().add(inscrireBtn);

        card.getChildren().add(body);
        return card;
    }

    // ── Inscription Dialog with Map + Weather ───────────────────────────────────

    private void showInscriptionDialog(Evenement event) {
        Dialog<Inscription> dialog = new Dialog<>();
        dialog.setTitle("S'inscrire a : " + event.getTitre());
        dialog.setHeaderText(null);
        dialog.setResizable(true);

        ButtonType inscrireType = new ButtonType("S'inscrire", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(inscrireType, ButtonType.CANCEL);

        // ── Main layout: left = event info + map + weather, right = form ──
        VBox mainBox = new VBox(14);
        mainBox.setPrefWidth(620);

        // ── Event header ──────────────────────────────────────────
        Label titleLabel = new Label(event.getTitre());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        titleLabel.setWrapText(true);

        HBox infoRow = new HBox(16);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        if (event.getDateDebut() != null) {
            Label dateL = new Label("📅 " + event.getDateDebut().format(FMT));
            dateL.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            infoRow.getChildren().add(dateL);
        }
        if (event.getLieu() != null && !event.getLieu().isBlank()) {
            Label lieuL = new Label("📍 " + event.getLieu());
            lieuL.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            infoRow.getChildren().add(lieuL);
        }
        if (event.getStatut() != null) {
            Label statusL = new Label(event.getStatut().toUpperCase());
            String badgeColor = switch (event.getStatut()) {
                case "planifié", "planifie" -> "#4a7cf7";
                case "en_cours" -> "#f59e0b";
                case "terminé", "termine" -> "#22c55e";
                case "annulé", "annule" -> "#ef4444";
                default -> "#94a3b8";
            };
            statusL.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 9px;" +
                "-fx-font-weight: 700; -fx-padding: 2 8; -fx-background-radius: 10;", badgeColor));
            infoRow.getChildren().add(statusL);
        }

        mainBox.getChildren().addAll(titleLabel, infoRow);

        // ── Map + Weather side by side ────────────────────────────
        HBox mapWeatherRow = new HBox(12);

        // Map WebView (read-only, shows event location)
        if (event.getLatitude() != null && event.getLongitude() != null) {
            javafx.scene.web.WebView mapView = new javafx.scene.web.WebView();
            mapView.setPrefSize(340, 180);
            mapView.setMaxSize(340, 180);

            String mapHtml = String.format("""
                <!DOCTYPE html><html><head>
                <meta charset="utf-8"/>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                  html, body { margin:0; padding:0; width:100%%; height:100%%; overflow:hidden; }
                  #map { width:100%%; height:100%%; }
                </style>
                </head><body><div id="map"></div><script>
                var map = L.map('map').setView([%s,%s], 14);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
                  attribution:'&copy; OSM', maxZoom:19
                }).addTo(map);
                L.marker([%s,%s]).addTo(map).bindPopup('%s').openPopup();
                setTimeout(function(){ map.invalidateSize(); }, 200);
                setTimeout(function(){ map.invalidateSize(); }, 600);
                setTimeout(function(){ map.invalidateSize(); }, 1500);
                </script></body></html>
                """,
                event.getLatitude(), event.getLongitude(),
                event.getLatitude(), event.getLongitude(),
                event.getLieu() != null ? event.getLieu().replace("'", "\\'") : "Lieu"
            );
            mapView.getEngine().loadContent(mapHtml);

            VBox mapBox = new VBox(4);
            Label mapLabel = new Label("Carte de Localisation");
            mapLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            mapBox.getChildren().addAll(mapLabel, mapView);
            mapWeatherRow.getChildren().add(mapBox);
        } else {
            // No coordinates — show placeholder
            VBox mapPlaceholder = new VBox(8);
            mapPlaceholder.setPrefSize(340, 180);
            mapPlaceholder.setAlignment(Pos.CENTER);
            mapPlaceholder.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
            Label noMapLabel = new Label("📍 Carte non disponible\n(pas de coordonnées)");
            noMapLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-text-alignment: center;");
            noMapLabel.setWrapText(true);
            mapPlaceholder.getChildren().add(noMapLabel);

            VBox mapBox = new VBox(4);
            Label mapLabel = new Label("Carte de Localisation");
            mapLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            mapBox.getChildren().addAll(mapLabel, mapPlaceholder);
            mapWeatherRow.getChildren().add(mapBox);
        }

        // Weather panel
        VBox weatherPanel = new VBox(8);
        weatherPanel.setPrefWidth(240);
        weatherPanel.setPadding(new Insets(10, 12, 10, 12));
        weatherPanel.setStyle("-fx-background-color: linear-gradient(to bottom, #eff6ff, #f0fdf4);" +
                "-fx-background-radius: 10; -fx-border-color: #e0e7ff; -fx-border-radius: 10;");

        Label weatherTitle = new Label("Météo Prévue");
        weatherTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        weatherPanel.getChildren().add(weatherTitle);

        // Try to get weather from cache or show loading
        if (weatherCache != null && weatherCache.containsKey(event.getId()) && weatherCache.get(event.getId()) != null) {
            Map<String, Object> w = weatherCache.get(event.getId());

            Double temp = (Double) w.get("temp_c");
            if (temp != null) {
                Label tempL = new Label(String.format("🌡️ %.0f°C", temp));
                tempL.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e40af;");
                weatherPanel.getChildren().add(tempL);
            }

            String condText = (String) w.get("condition_text");
            if (condText != null) {
                Label condL = new Label(condText);
                condL.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
                condL.setWrapText(true);
                weatherPanel.getChildren().add(condL);
            }

            Double rain = (Double) w.get("chance_of_rain");
            if (rain != null) {
                Label rainL = new Label(String.format("Pluie : %.0f%%", rain));
                rainL.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                weatherPanel.getChildren().add(rainL);
            }

            Double humidity = (Double) w.get("humidity");
            if (humidity != null) {
                Label humL = new Label(String.format("Humidité : %.0f%%", humidity));
                humL.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                weatherPanel.getChildren().add(humL);
            }

            Double wind = (Double) w.get("wind_kph");
            if (wind != null) {
                Label windL = new Label(String.format("Vent : %.0f km/h", wind));
                windL.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                weatherPanel.getChildren().add(windL);
            }

            // Weather icon
            String iconUrl = (String) w.get("condition_icon");
            if (iconUrl != null) {
                try {
                    javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView(
                            new javafx.scene.image.Image(iconUrl, 48, 48, true, true, true));
                    weatherPanel.getChildren().add(0, iconView); // Add at top after title
                } catch (Exception ignored) {}
            }
        } else {
            Label noWeather = new Label("Données météo non disponibles\n(Prévision impossible > 14 jours)");
            noWeather.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            noWeather.setWrapText(true);
            weatherPanel.getChildren().add(noWeather);
        }

        VBox weatherBox = new VBox(4);
        weatherBox.getChildren().add(weatherPanel);
        mapWeatherRow.getChildren().add(weatherBox);
        HBox.setHgrow(weatherBox, Priority.ALWAYS);

        mainBox.getChildren().add(mapWeatherRow);

        // ── Separator ─────────────────────────────────────────────
        Region sep = new Region();
        sep.setStyle("-fx-background-color:#e2e8f0; -fx-min-height:1; -fx-pref-height:1;");
        mainBox.getChildren().add(sep);

        // ── Registration form section ─────────────────────────────
        Label formHeader = new Label("Formulaire d'inscription");
        formHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        mainBox.getChildren().add(formHeader);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        TextField nomField = new TextField();
        nomField.setPromptText("Votre nom complet");
        nomField.setPrefWidth(320);

        TextField emailField = new TextField();
        emailField.setPromptText("votre.email@exemple.com");
        emailField.setPrefWidth(320);

        TextField telField = new TextField();
        telField.setPromptText("+216 XX XXX XXX (optionnel)");
        telField.setPrefWidth(320);

        // Autofill with logged-in user data
        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null) {
            String fullName = (currentUser.getNom() != null ? currentUser.getNom() : "") + " " + 
                              (currentUser.getPrenom() != null ? currentUser.getPrenom() : "");
            nomField.setText(fullName.trim());
            
            if (currentUser.getEmail() != null) {
                emailField.setText(currentUser.getEmail());
            }
            if (currentUser.getTelephone() != null) {
                telField.setText(currentUser.getTelephone());
            }
        }

        grid.add(new Label("Nom *:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Email *:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Telephone :"), 0, 2);
        grid.add(telField, 1, 2);

        mainBox.getChildren().add(grid);

        ScrollPane scrollPane = new ScrollPane(mainBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white;");
        scrollPane.setPrefHeight(520);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-pref-width: 660;");
        dialog.getDialogPane().setPrefHeight(580);

        Node inscrireButton = dialog.getDialogPane().lookupButton(inscrireType);
        inscrireButton.setDisable(true);

        Runnable validate = () -> {
            String nom = nomField.getText().trim();
            String email = emailField.getText().trim();
            String tel = telField.getText().trim();

            // Contrôle de saisie par Java (Regex)
            boolean nomValide = nom.length() >= 3 && nom.matches("^[a-zA-ZÀ-ÿ\\s-]+$");
            boolean emailValide = email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");
            boolean telValide = tel.isEmpty() || tel.matches("^[0-9]{8}$");

            boolean isValid = nomValide && emailValide && telValide;
            
            // Visual feedback
            nomField.setStyle(nom.isEmpty() || nomValide ? "" : "-fx-border-color: red;");
            emailField.setStyle(email.isEmpty() || emailValide ? "" : "-fx-border-color: red;");
            telField.setStyle(tel.isEmpty() || telValide ? "" : "-fx-border-color: red;");

            inscrireButton.setDisable(!isValid);
        };
        nomField.textProperty().addListener((obs, o, n) -> validate.run());
        emailField.textProperty().addListener((obs, o, n) -> validate.run());
        telField.textProperty().addListener((obs, o, n) -> validate.run());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == inscrireType) {
                Inscription insc = new Inscription();
                insc.setEvenementId(event.getId());
                insc.setNomParticipant(nomField.getText().trim());
                insc.setEmailParticipant(emailField.getText().trim());
                insc.setTelephoneParticipant(telField.getText().trim().isEmpty() ? null : telField.getText().trim());
                return insc;
            }
            return null;
        });

        Optional<Inscription> result = dialog.showAndWait();
        result.ifPresent(inscription -> {
            if (inscService.isDejaInscrit(event.getId(), inscription.getEmailParticipant())) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Deja inscrit");
                alert.setHeaderText(null);
                alert.setContentText("Vous etes deja inscrit(e) avec l'email : "
                        + inscription.getEmailParticipant());
                alert.showAndWait();
                return;
            }

            boolean ok = inscService.inscrire(inscription);
            if (ok) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Inscription reussie");
                alert.setHeaderText(null);
                alert.setContentText("Vous etes inscrit(e) a \"" + event.getTitre() + "\".\n\nTotal: "
                        + inscService.countByEvenement(event.getId()) + " participant(s).");
                alert.showAndWait();
                loadData();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText(null);
                alert.setContentText("Impossible de finaliser l'inscription.");
                alert.showAndWait();
            }
        });
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
