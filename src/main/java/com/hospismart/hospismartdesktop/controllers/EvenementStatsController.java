package com.hospismart.hospismartdesktop.controllers;

import com.hospismart.hospismartdesktop.services.EventAnalyticsService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class EvenementStatsController implements Initializable {

    @FXML private FlowPane statsCardsPane;
    @FXML private PieChart typePieChart;
    @FXML private BarChart<String, Number> monthlyBarChart;
    @FXML private VBox insightsContainer;
    @FXML private VBox budgetContainer;

    private final EventAnalyticsService analytics = new EventAnalyticsService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadOverviewCards();
        loadTypePieChart();
        loadMonthlyBarChart();
        loadAiInsights();
        loadBudgetAnalysis();
    }

    // ── Overview Stats Cards ──────────────────────────────────────────────────

    private void loadOverviewCards() {
        Map<String, Object> overview = analytics.getOverview();

        statsCardsPane.getChildren().addAll(
                buildStatCard("📋", "Total Événements", String.valueOf(overview.get("total")), "#4a7cf7"),
                buildStatCard("📅", "À Venir", String.valueOf(overview.get("upcoming")), "#22c55e"),
                buildStatCard("✅", "Passés", String.valueOf(overview.get("past")), "#94a3b8"),
                buildStatCard("👥", "Total Participants", String.valueOf(overview.get("totalParticipants")), "#f59e0b"),
                buildStatCard("📊", "Moy. Participants", String.valueOf(overview.get("avgParticipants")), "#8b5cf6"),
                buildStatCard("💰", "Budget Total", String.format("%.0f TND", overview.get("totalBudget")), "#ef4444")
        );
    }

    private VBox buildStatCard(String icon, String title, String value, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setPrefWidth(165);
        card.setMinWidth(150);
        card.setStyle(String.format(
                "-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-border-color: #edf0f5; -fx-border-radius: 12; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);"));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle(String.format("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: %s;", color));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: 600;");

        card.getChildren().addAll(iconLabel, valueLabel, titleLabel);
        return card;
    }

    // ── Pie Chart ─────────────────────────────────────────────────────────────

    private void loadTypePieChart() {
        Map<String, Integer> dist = analytics.getTypeDistribution();
        typePieChart.getData().clear();
        for (Map.Entry<String, Integer> entry : dist.entrySet()) {
            typePieChart.getData().add(new PieChart.Data(
                    entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
        typePieChart.setLabelsVisible(true);
    }

    // ── Bar Chart ─────────────────────────────────────────────────────────────

    private void loadMonthlyBarChart() {
        List<Map<String, Object>> trend = analytics.getMonthlyTrend();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Événements");

        for (Map<String, Object> m : trend) {
            series.getData().add(new XYChart.Data<>((String) m.get("label"), (int) m.get("count")));
        }

        monthlyBarChart.getData().clear();
        monthlyBarChart.getData().add(series);
        monthlyBarChart.setCategoryGap(20);
        monthlyBarChart.setBarGap(3);
    }

    // ── AI Insights ───────────────────────────────────────────────────────────

    private void loadAiInsights() {
        List<Map<String, String>> insights = analytics.generateInsights();
        insightsContainer.getChildren().clear();

        for (Map<String, String> insight : insights) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));

            String type = insight.get("type");
            String bgColor = switch (type) {
                case "success" -> "#f0fdf4";
                case "warning" -> "#fffbeb";
                case "alert"   -> "#fef2f2";
                default        -> "#eff6ff";
            };
            String borderColor = switch (type) {
                case "success" -> "#bbf7d0";
                case "warning" -> "#fde68a";
                case "alert"   -> "#fecaca";
                default        -> "#bfdbfe";
            };

            row.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 8; " +
                    "-fx-border-color: %s; -fx-border-radius: 8; -fx-border-width: 1;",
                    bgColor, borderColor));

            Label iconLabel = new Label(insight.get("icon"));
            iconLabel.setStyle("-fx-font-size: 18px;");

            Label textLabel = new Label(insight.get("text"));
            textLabel.setWrapText(true);
            textLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
            HBox.setHgrow(textLabel, Priority.ALWAYS);

            row.getChildren().addAll(iconLabel, textLabel);
            insightsContainer.getChildren().add(row);
        }
    }

    // ── Budget Analysis ───────────────────────────────────────────────────────

    private void loadBudgetAnalysis() {
        Map<String, Map<String, Object>> budgetData = analytics.getBudgetAnalysis();
        budgetContainer.getChildren().clear();

        if (budgetData.isEmpty()) {
            budgetContainer.getChildren().add(new Label("Aucune donnée budgétaire disponible."));
            return;
        }

        for (Map.Entry<String, Map<String, Object>> entry : budgetData.entrySet()) {
            Map<String, Object> v = entry.getValue();
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 6;");

            Label typeLabel = new Label(entry.getKey());
            typeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-min-width: 100;");

            Label statsLabel = new Label(String.format(
                    "Total: %.0f TND  |  Moy: %.0f TND  |  Min: %.0f  |  Max: %.0f  |  %d événement(s)",
                    v.get("total"), v.get("avg"), v.get("min"), v.get("max"), v.get("count")));
            statsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

            row.getChildren().addAll(typeLabel, statsLabel);
            budgetContainer.getChildren().add(row);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void onRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/hospismart/hospismartdesktop/evenement-back.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 760);
            scene.getStylesheets().add(
                    getClass().getResource("/com/hospismart/hospismartdesktop/styles.css").toExternalForm());
            Stage stage = (Stage) statsCardsPane.getScene().getWindow();
            stage.setTitle("HospiSmart — Back Office");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
