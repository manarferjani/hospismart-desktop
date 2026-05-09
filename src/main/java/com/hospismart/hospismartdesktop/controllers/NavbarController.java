package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.main.JavaFxMain;
import com.hospismart.hospismartdesktop.models.Notification;
import com.hospismart.hospismartdesktop.services.NotificationService;
import com.hospismart.hospismartdesktop.utils.UserSession;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NavbarController {

    @FXML private Hyperlink linkAccueil;
    @FXML private Hyperlink linkServices;
    @FXML private Hyperlink linkRdvs;
    @FXML private Hyperlink linkEvenements;
    @FXML private Hyperlink linkReclamations;
    @FXML private Hyperlink linkMedicaments;

    // Notification bell elements
    @FXML private StackPane bellContainer;
    @FXML private Label bellIcon;
    @FXML private StackPane badgePane;
    @FXML private Label badgeCount;

    private Popup notifPopup;
    private boolean popupVisible = false;
    private final NotificationService notifService = new NotificationService();
    private Timer refreshTimer;

    private static final String STYLE_ACTIVE = "-fx-text-fill: #0d6efd; -fx-font-weight: bold; -fx-underline: false; -fx-border-color: transparent;";
    private static final String STYLE_NORMAL = "-fx-text-fill: #555; -fx-font-weight: normal; -fx-underline: false; -fx-border-color: transparent;";
    private static final String STYLE_HOVER = "-fx-text-fill: #4178F5; -fx-underline: false; -fx-border-color: transparent;";

    @FXML
    public void initialize() {
        resetAllLinks();

        // On gère le survol intelligemment
        setupHoverEffect(linkAccueil);
        setupHoverEffect(linkServices);
        setupHoverEffect(linkRdvs);
        setupHoverEffect(linkEvenements);
        setupHoverEffect(linkReclamations);
        if (linkMedicaments != null) {
            setupHoverEffect(linkMedicaments);
            linkMedicaments.setVisible(true);
            linkMedicaments.setManaged(true);
        }

        javafx.application.Platform.runLater(() -> {
            // Détection dynamique de la page active via le contenu de la scène
            if (bellContainer != null && bellContainer.getScene() != null && bellContainer.getScene().getRoot() != null) {
                Parent root = bellContainer.getScene().getRoot();
                if (root.lookup("#gridRdv") != null) {
                    setActive(linkRdvs);
                } else if (root.lookup("#flowPaneMedicaments") != null) {
                    if (linkMedicaments != null) setActive(linkMedicaments);
                } else if (root.lookup("#tableReclamation") != null) {
                    setActive(linkReclamations);
                } else if (root.lookup("#typeFilter") != null && root.lookup("#cardsContainer") != null) {
                    // evenement-front.fxml a un #typeFilter
                    setActive(linkEvenements);
                } else if (root.lookup("#servicesContainer") != null) {
                    // TrouverMedecin.fxml a un #servicesContainer
                    setActive(linkAccueil); // Ou linkServices selon ce qu'on préfère (Accueil par defaut)
                } else if (linkAccueil != null) {
                    setActive(linkAccueil);
                }
            }

            // Initialize notification system
            refreshNotifBadge();
            setupBellHoverAnimation();
            startAutoRefresh();
        });
    }

    // ========================
    // NOTIFICATION BELL LOGIC
    // ========================

    /** Refresh the badge count from DB */
    private void refreshNotifBadge() {
        if (UserSession.getUser() == null) return;
        try {
            int count = notifService.countUnread(UserSession.getUser().getId());
            javafx.application.Platform.runLater(() -> {
                if (count > 0) {
                    badgeCount.setText(count > 99 ? "99+" : String.valueOf(count));
                    badgePane.setVisible(true);
                    badgePane.setManaged(true);
                    // Pulse animation on badge
                    pulseAnimation(badgePane);
                } else {
                    badgePane.setVisible(false);
                    badgePane.setManaged(false);
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Add hover animation to bell icon */
    private void setupBellHoverAnimation() {
        if (bellContainer == null) return;

        bellContainer.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), bellIcon);
            st.setToX(1.2);
            st.setToY(1.2);
            st.play();

            // Swing animation (like a real bell)
            RotateTransition rt = new RotateTransition(Duration.millis(100), bellIcon);
            rt.setByAngle(15);
            rt.setCycleCount(4);
            rt.setAutoReverse(true);
            rt.play();
        });

        bellContainer.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), bellIcon);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();

            bellIcon.setRotate(0);
        });
    }

    /** Pulse animation for badge */
    private void pulseAnimation(StackPane badge) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(600), badge);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.25);
        pulse.setToY(1.25);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    /** Auto-refresh every 30 seconds */
    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshNotifBadge();
            }
        }, 30000, 30000);
    }

    /** Toggle notification dropdown popup */
    @FXML
    private void toggleNotifications(MouseEvent event) {
        if (popupVisible && notifPopup != null) {
            notifPopup.hide();
            popupVisible = false;
            return;
        }

        // Marquer toutes les notifications comme lues dès l'ouverture de la cloche
        if (UserSession.getUser() != null) {
            try {
                notifService.markAllAsRead(UserSession.getUser().getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Cacher le badge immédiatement
        badgePane.setVisible(false);
        badgePane.setManaged(false);

        showNotifPopup();
    }

    /** Build & show the notification popup */
    private void showNotifPopup() {
        if (UserSession.getUser() == null) return;

        notifPopup = new Popup();
        notifPopup.setAutoHide(true);
        notifPopup.setOnHidden(e -> popupVisible = false);

        VBox popupContent = new VBox(0);
        popupContent.setPrefWidth(380);
        popupContent.setMaxHeight(500);
        popupContent.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 16;" +
            "-fx-border-width: 1;"
        );
        popupContent.setEffect(new DropShadow(25, Color.rgb(0, 0, 0, 0.15)));

        // ---- Header ----
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 14, 20));
        header.setStyle("-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("\uD83D\uDD14  Notifications");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        header.getChildren().add(titleLabel);

        // ---- Notification List ----
        VBox notifList = new VBox(0);
        ScrollPane scrollPane = new ScrollPane(notifList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(380);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        try {
            // Afficher toutes les notifications récentes (lues + non lues)
            List<Notification> notifications = notifService.findAllByUser(UserSession.getUser().getId());

            if (notifications.isEmpty()) {
                // Empty state
                VBox emptyBox = new VBox(10);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(40, 20, 40, 20));

                Label emptyIcon = new Label("\uD83C\uDF89");
                emptyIcon.setStyle("-fx-font-size: 40;");

                Label emptyText = new Label("Aucune notification !");
                emptyText.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

                Label emptySubtext = new Label("Vous \u00eates \u00e0 jour. Nous vous notifierons\nlorsqu'il y aura du nouveau.");
                emptySubtext.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");
                emptySubtext.setWrapText(true);

                emptyBox.getChildren().addAll(emptyIcon, emptyText, emptySubtext);
                notifList.getChildren().add(emptyBox);
            } else {
                for (int i = 0; i < notifications.size(); i++) {
                    Notification n = notifications.get(i);
                    notifList.getChildren().add(createNotifCard(n, i == notifications.size() - 1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Erreur de chargement des notifications");
            errorLabel.setStyle("-fx-text-fill: #dc3545; -fx-padding: 20;");
            notifList.getChildren().add(errorLabel);
        }

        popupContent.getChildren().addAll(header, scrollPane);

        notifPopup.getContent().add(popupContent);

        // Position popup below the bell icon
        javafx.application.Platform.runLater(() -> {
            var bounds = bellContainer.localToScreen(bellContainer.getBoundsInLocal());
            if (bounds != null) {
                notifPopup.show(bellContainer.getScene().getWindow(),
                    bounds.getMaxX() - 380,
                    bounds.getMaxY() + 8);
                popupVisible = true;

                // Slide-in animation
                popupContent.setTranslateY(-10);
                popupContent.setOpacity(0);
                TranslateTransition tt = new TranslateTransition(Duration.millis(200), popupContent);
                tt.setFromY(-10);
                tt.setToY(0);
                FadeTransition ft = new FadeTransition(Duration.millis(200), popupContent);
                ft.setFromValue(0);
                ft.setToValue(1);
                new ParallelTransition(tt, ft).play();
            }
        });
    }

    /** Create a single notification card in the dropdown */
    private HBox createNotifCard(Notification n, boolean isLast) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
            "-fx-background-color: " + (n.isRead() ? "white" : "#f0f7ff") + ";" +
            (isLast ? "" : "-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;") +
            "-fx-cursor: hand;"
        );

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #f8fafc;" +
            (isLast ? "" : "-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;") +
            "-fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: " + (n.isRead() ? "white" : "#f0f7ff") + ";" +
            (isLast ? "" : "-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;") +
            "-fx-cursor: hand;"
        ));

        // Icon circle
        String iconText;
        String iconBg;

        if ("rdv_accepte".equals(n.getType())) {
            iconText = "✅";
            iconBg = "#dcfce7";
        } else if ("rdv_refuse".equals(n.getType())) {
            iconText = "❌";
            iconBg = "#fee2e2";
        } else {
            iconText = "🔔";
            iconBg = "#e0e7ff";
        }

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(40, 40);
        iconCircle.setMinSize(40, 40);
        iconCircle.setMaxSize(40, 40);
        iconCircle.setStyle(
            "-fx-background-color: " + iconBg + ";" +
            "-fx-background-radius: 50;"
        );
        Label iconLabel = new Label(iconText);
        iconLabel.setStyle("-fx-font-size: 16;");
        iconCircle.getChildren().add(iconLabel);

        // Text container
        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label contentLabel = new Label(n.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(280);
        contentLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #1e293b;" +
            (n.isRead() ? "" : " -fx-font-weight: bold;"));

        Label timeLabel = new Label(formatTimeAgo(n.getCreatedAt()));
        timeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");

        textBox.getChildren().addAll(contentLabel, timeLabel);

        // Unread dot indicator
        if (!n.isRead()) {
            Circle dot = new Circle(4, Color.web("#4178F5"));
            StackPane dotPane = new StackPane(dot);
            dotPane.setPadding(new Insets(6, 0, 0, 0));
            card.getChildren().addAll(iconCircle, textBox, dotPane);
        } else {
            card.getChildren().addAll(iconCircle, textBox);
        }

        return card;
    }

    /** Format createdAt into "Il y a X minutes", "Il y a X heures" etc. */
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);

        if (minutes < 1) return "À l'instant";
        if (minutes < 60) return "Il y a " + minutes + " min";
        if (hours < 24) return "Il y a " + hours + "h";
        if (days < 7) return "Il y a " + days + " jour" + (days > 1 ? "s" : "");

        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }



    // ========================
    // ORIGINAL NAVBAR LOGIC
    // ========================

    private void setActive(Hyperlink link) {
        link.setStyle(STYLE_ACTIVE);
        // On peut utiliser l'ID pour marquer qu'il est actif
        link.setUserData("active");
    }

    private void setupHoverEffect(Hyperlink link) {
        if (link != null) {
            // Enlever le cadre pointillé au clic/focus
            link.setFocusTraversable(false);

            link.setOnMouseEntered(e -> {
                if (!"active".equals(link.getUserData())) {
                    link.setStyle(STYLE_HOVER);
                }
            });
            link.setOnMouseExited(e -> {
                if (!"active".equals(link.getUserData())) {
                    link.setStyle(STYLE_NORMAL);
                } else {
                    link.setStyle(STYLE_ACTIVE);
                }
            });
        }
    }

    private void resetAllLinks() {
        linkAccueil.setUserData(null);
        linkServices.setUserData(null);
        if (linkRdvs != null) linkRdvs.setUserData(null);
        linkEvenements.setUserData(null);
        if (linkReclamations != null) linkReclamations.setUserData(null);
        if (linkMedicaments != null) linkMedicaments.setUserData(null);

        linkAccueil.setStyle(STYLE_NORMAL);
        linkServices.setStyle(STYLE_NORMAL);
        if (linkRdvs != null) linkRdvs.setStyle(STYLE_NORMAL);
        linkEvenements.setStyle(STYLE_NORMAL);
        if (linkReclamations != null) linkReclamations.setStyle(STYLE_NORMAL);
        if (linkMedicaments != null) linkMedicaments.setStyle(STYLE_NORMAL);
    }

    @FXML
    private void goToServices(ActionEvent event) {
        switchScene(event, "/TrouverMedecin.fxml");
    }

    @FXML
    private void goToAccueil(ActionEvent event) {
        switchScene(event, "/TrouverMedecin.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        JavaFxMain.setRoot(fxmlPath, null);
    }

    public void goToRdvs(ActionEvent event) {
        switchScene(event, "/MesRdv.fxml");
    }

    @FXML
    public void goToReclamations(ActionEvent event) {
        switchScene(event, "/com/hospismart/hospismartdesktop/reclamation_front.fxml");
    }
    @FXML
    public void goToEvenements(ActionEvent event) {
        switchScene(event, "/com/hospismart/hospismartdesktop/evenement-front.fxml");
    }

    @FXML
    public void goToMedicaments(ActionEvent event) {
        switchScene(event, "/com/hospismart/hospismartdesktop/medicament_front.fxml");
    }

    @FXML
    public void goToMonCompte(ActionEvent event) {
        switchScene(event, "/com/hospismart/hospismartdesktop/UserProfile.fxml");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        com.hospismart.hospismartdesktop.utils.Session.getInstance().cleanUserSession();
        switchScene(event, "/com/hospismart/hospismartdesktop/Login.fxml");
    }
}