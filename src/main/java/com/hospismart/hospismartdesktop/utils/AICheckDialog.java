package com.hospismart.hospismartdesktop.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AICheckDialog {

    public enum DialogType { ERROR, CORRECTION, SUCCESS, INCOHERENCE }
    public enum Result     { PRIMARY, SECONDARY, CLOSED }

    private Result result = Result.CLOSED;

    public Result show(DialogType type, String title, String subtitle,
                       String detail, String analyseText,
                       String primaryLabel, String secondaryLabel,
                       String avant, String apres) {

        // ── Récupérer la fenêtre principale ───────────────────────────────
        Stage ownerStage = null;
        try {
            ownerStage = (Stage) javafx.stage.Window.getWindows()
                    .stream()
                    .filter(w -> w instanceof Stage && w.isShowing())
                    .findFirst().orElse(null);
        } catch (Exception ignored) {}

        // ── Stage principal de la modal ───────────────────────────────────
        Stage stage = new Stage();
        if (ownerStage != null) stage.initOwner(ownerStage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        // ── Overlay sombre derrière la modal ──────────────────────────────
        Stage overlayStage = null;
        if (ownerStage != null) {
            overlayStage = new Stage();
            overlayStage.initOwner(ownerStage);
            overlayStage.initStyle(StageStyle.TRANSPARENT);
            overlayStage.initModality(Modality.NONE);

            javafx.scene.layout.StackPane overlayPane = new javafx.scene.layout.StackPane();
            overlayPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
            overlayPane.setPrefSize(ownerStage.getWidth(), ownerStage.getHeight());

            Scene overlayScene = new Scene(overlayPane,
                    ownerStage.getWidth(), ownerStage.getHeight());
            overlayScene.setFill(Color.TRANSPARENT);
            overlayStage.setScene(overlayScene);
            overlayStage.setX(ownerStage.getX());
            overlayStage.setY(ownerStage.getY());
            overlayStage.show();
        }

        // ── Couleurs par type ──────────────────────────────────────────────
        String headerBg, headerBorder, iconBg, titleColor, subtitleColor, btnPrimaryBg;
        switch (type) {
            case INCOHERENCE -> {
                headerBg = "#FAECE7"; headerBorder = "#F5C4B3";
                iconBg = "#D85A30";  titleColor = "#993C1D"; subtitleColor = "#D85A30";
                btnPrimaryBg = "#D85A30";
            }
            case CORRECTION -> {
                headerBg = "#E6F1FB"; headerBorder = "#B5D4F4";
                iconBg = "#378ADD";  titleColor = "#185FA5"; subtitleColor = "#378ADD";
                btnPrimaryBg = "#378ADD";
            }
            case SUCCESS -> {
                headerBg = "#EAF3DE"; headerBorder = "#C0DD97";
                iconBg = "#639922";  titleColor = "#3B6D11"; subtitleColor = "#639922";
                btnPrimaryBg = "#639922";
            }
            default -> {
                headerBg = "#FFF8E1"; headerBorder = "#FFE082";
                iconBg = "#F59E0B";  titleColor = "#92400E"; subtitleColor = "#D97706";
                btnPrimaryBg = "#F59E0B";
            }
        }

        // ── Root ──────────────────────────────────────────────────────────
        VBox root = new VBox();
        root.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 16;
                -fx-border-radius: 16;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 40, 0, 0, 12);
                """);
        root.setMinWidth(440);
        root.setMaxWidth(440);

        // ── Header ────────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: " + headerBg + ";"
                        + "-fx-border-color: " + headerBorder + ";"
                        + "-fx-border-width: 0 0 0.5 0;"
                        + "-fx-background-radius: 16 16 0 0;"
                        + "-fx-border-radius: 16 16 0 0;"
        );

        // Icône circulaire
        StackPane iconPane = new StackPane();
        Circle circle = new Circle(18);
        circle.setFill(Color.web(iconBg));
        Label iconLabel = new Label(getIcon(type));
        iconLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold;");
        iconPane.getChildren().addAll(circle, iconLabel);

        VBox headerText = new VBox(3);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
        Label subtitleLbl = new Label("HospiSmart AI-Check");
        subtitleLbl.setStyle("-fx-font-size: 11; -fx-text-fill: " + subtitleColor + ";");
        headerText.getChildren().addAll(titleLbl, subtitleLbl);

        header.getChildren().addAll(iconPane, headerText);

        // ── Body ──────────────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setPadding(new Insets(20, 24, 24, 24));

        if (subtitle != null && !subtitle.isEmpty()) {
            Label subLbl = new Label(subtitle);
            subLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #6B7280; -fx-wrap-text: true;");
            subLbl.setMaxWidth(392);
            body.getChildren().add(subLbl);
        }

        // Carte avant/après (correction orthographique)
        if (avant != null && apres != null) {
            VBox diffCard = new VBox(8);
            diffCard.setPadding(new Insets(10, 14, 10, 14));
            diffCard.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8;");

            HBox rowAvant = new HBox(8);
            Label lblAvantKey = new Label("Avant");
            lblAvantKey.setStyle("-fx-font-size: 12; -fx-text-fill: #9CA3AF; -fx-min-width: 50;");
            Label lblAvantVal = new Label(avant);
            lblAvantVal.setStyle("-fx-font-size: 13; -fx-font-weight: bold; "
                    + "-fx-text-fill: #E24B4A; -fx-strikethrough: true;");
            rowAvant.getChildren().addAll(lblAvantKey, lblAvantVal);

            HBox rowApres = new HBox(8);
            Label lblApresKey = new Label("Après");
            lblApresKey.setStyle("-fx-font-size: 12; -fx-text-fill: #9CA3AF; -fx-min-width: 50;");
            Label lblApresVal = new Label(apres);
            lblApresVal.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #3B6D11;");
            rowApres.getChildren().addAll(lblApresKey, lblApresVal);

            diffCard.getChildren().addAll(rowAvant, rowApres);
            body.getChildren().add(diffCard);
        }

        // Bloc d'analyse avec bordure gauche colorée
        if (detail != null && !detail.isEmpty()) {
            Label detailLbl = new Label(detail);
            detailLbl.setStyle("""
                    -fx-font-size: 12;
                    -fx-text-fill: #4B5563;
                    -fx-wrap-text: true;
                    -fx-padding: 10 14 10 14;
                    -fx-background-color: #FAECE7;
                    -fx-background-radius: 0 8 8 0;
                    -fx-border-color: #D85A30;
                    -fx-border-width: 0 0 0 3;
                    """);
            detailLbl.setMaxWidth(392);
            body.getChildren().add(detailLbl);
        }

        // Texte d'analyse en italique
        if (analyseText != null && !analyseText.isEmpty()
                && !analyseText.equals("Analyse terminée.")) {
            Label analyseLbl = new Label(analyseText);
            analyseLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #9CA3AF; "
                    + "-fx-wrap-text: true; -fx-font-style: italic;");
            analyseLbl.setMaxWidth(392);
            body.getChildren().add(analyseLbl);
        }

        // ── Boutons ───────────────────────────────────────────────────────
        HBox btnRow = new HBox(8);
        btnRow.setPadding(new Insets(8, 0, 0, 0));

        if (secondaryLabel != null) {
            Button btnSecondary = new Button(secondaryLabel);
            btnSecondary.setStyle("""
                    -fx-font-size: 13; -fx-padding: 10 16;
                    -fx-background-color: #F3F4F6;
                    -fx-background-radius: 10;
                    -fx-border-color: #E5E7EB;
                    -fx-border-radius: 10;
                    -fx-text-fill: #374151;
                    -fx-cursor: hand;
                    """);
            btnSecondary.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnSecondary, Priority.ALWAYS);
            btnSecondary.setOnAction(e -> { result = Result.SECONDARY; stage.close(); });
            btnRow.getChildren().add(btnSecondary);
        }

        final String finalBtnBg = btnPrimaryBg;
        Button btnPrimary = new Button(primaryLabel);
        btnPrimary.setStyle(
                "-fx-font-size: 13; -fx-padding: 10 16;"
                        + "-fx-background-color: " + finalBtnBg + ";"
                        + "-fx-background-radius: 10;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-cursor: hand;"
        );
        btnPrimary.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnPrimary, Priority.ALWAYS);
        btnPrimary.setOnAction(e -> { result = Result.PRIMARY; stage.close(); });
        btnRow.getChildren().add(btnPrimary);

        body.getChildren().add(btnRow);
        root.getChildren().addAll(header, body);

        // ── Scene & affichage ─────────────────────────────────────────────
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();

        // Fermer l'overlay quand la modal se ferme
        final Stage finalOverlay = overlayStage;
        stage.setOnHidden(e -> {
            if (finalOverlay != null) finalOverlay.close();
        });

        stage.showAndWait();
        return result;
    }

    private String getIcon(DialogType type) {
        return switch (type) {
            case INCOHERENCE -> "⚠";
            case CORRECTION  -> "✎";
            case SUCCESS     -> "✓";
            default          -> "!";
        };
    }
}