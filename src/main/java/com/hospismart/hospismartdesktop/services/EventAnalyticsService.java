package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Evenement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI-powered Event Analytics — mirrors EventAnalyticsService.php
 * Computes stats, distributions, trends, and generates smart insights.
 */
public class EventAnalyticsService {

    private final EvenementService evenementService;
    private final InscriptionService inscriptionService;

    public EventAnalyticsService() {
        this.evenementService = new EvenementService();
        this.inscriptionService = new InscriptionService();
    }

    // ── Overview ──────────────────────────────────────────────────────

    public Map<String, Object> getOverview() {
        List<Evenement> events = evenementService.findAll();
        int total = events.size();
        int upcoming = 0, past = 0;
        double totalBudget = 0;
        int totalParticipants = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Evenement e : events) {
            if (e.getDateDebut() != null && e.getDateDebut().isAfter(now)) {
                upcoming++;
            } else {
                past++;
            }
            totalBudget += e.getBudgetAlloue();
            totalParticipants += inscriptionService.countByEvenement(e.getId());
        }

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("total", total);
        overview.put("upcoming", upcoming);
        overview.put("past", past);
        overview.put("totalParticipants", totalParticipants);
        overview.put("avgParticipants", total > 0 ? Math.round((double) totalParticipants / total * 10.0) / 10.0 : 0.0);
        overview.put("totalBudget", totalBudget);
        overview.put("avgBudget", total > 0 ? Math.round(totalBudget / total * 100.0) / 100.0 : 0.0);
        return overview;
    }

    // ── Type Distribution ─────────────────────────────────────────────

    public Map<String, Integer> getTypeDistribution() {
        List<Evenement> events = evenementService.findAll();
        Map<String, Integer> dist = new LinkedHashMap<>();
        for (Evenement e : events) {
            String type = e.getTypeEvenement() != null ? e.getTypeEvenement() : "autre";
            dist.merge(type, 1, Integer::sum);
        }
        // Sort descending
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(dist.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        Map<String, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : entries) sorted.put(entry.getKey(), entry.getValue());
        return sorted;
    }

    // ── Monthly Trend (last 6 months) ─────────────────────────────────

    public List<Map<String, Object>> getMonthlyTrend() {
        List<Evenement> events = evenementService.findAll();
        String[] frenchMonths = {"", "Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};

        LinkedHashMap<String, Map<String, Object>> months = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusMonths(i);
            String key = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String label = frenchMonths[date.getMonthValue()] + " " + date.getYear();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", label);
            m.put("count", 0);
            m.put("budget", 0.0);
            months.put(key, m);
        }

        for (Evenement e : events) {
            if (e.getDateDebut() != null) {
                String key = e.getDateDebut().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                if (months.containsKey(key)) {
                    Map<String, Object> m = months.get(key);
                    m.put("count", (int) m.get("count") + 1);
                    m.put("budget", (double) m.get("budget") + e.getBudgetAlloue());
                }
            }
        }

        return new ArrayList<>(months.values());
    }

    // ── Budget Analysis by Type ───────────────────────────────────────

    public Map<String, Map<String, Object>> getBudgetAnalysis() {
        List<Evenement> events = evenementService.findAll();
        Map<String, Map<String, Object>> byType = new LinkedHashMap<>();

        for (Evenement e : events) {
            String type = e.getTypeEvenement() != null ? e.getTypeEvenement() : "autre";
            byType.putIfAbsent(type, new LinkedHashMap<>(Map.of(
                    "total", 0.0, "count", 0, "min", Double.MAX_VALUE, "max", 0.0
            )));
            Map<String, Object> v = byType.get(type);
            double b = e.getBudgetAlloue();
            v.put("total", (double) v.get("total") + b);
            v.put("count", (int) v.get("count") + 1);
            if (b > 0) {
                v.put("min", Math.min((double) v.get("min"), b));
                v.put("max", Math.max((double) v.get("max"), b));
            }
        }

        for (Map<String, Object> v : byType.values()) {
            int count = (int) v.get("count");
            v.put("avg", count > 0 ? Math.round((double) v.get("total") / count * 100.0) / 100.0 : 0.0);
            if ((double) v.get("min") == Double.MAX_VALUE) v.put("min", 0.0);
        }
        return byType;
    }

    // ── Status Breakdown ──────────────────────────────────────────────

    public Map<String, Integer> getStatusBreakdown() {
        List<Evenement> events = evenementService.findAll();
        Map<String, Integer> status = new LinkedHashMap<>();
        for (Evenement e : events) {
            String s = e.getStatut() != null ? e.getStatut() : "inconnu";
            status.merge(s, 1, Integer::sum);
        }
        return status;
    }

    // ── AI Insights ───────────────────────────────────────────────────

    public List<Map<String, String>> generateInsights() {
        List<Evenement> events = evenementService.findAll();
        List<Map<String, String>> insights = new ArrayList<>();
        int total = events.size();

        if (total == 0) {
            insights.add(insight("info", "📊", "Aucun événement enregistré. Créez votre premier événement pour activer les analyses IA."));
            return insights;
        }

        // Type dominance
        Map<String, Integer> types = getTypeDistribution();
        String topType = types.keySet().iterator().next();
        int topCount = types.get(topType);
        int pct = Math.round((float) topCount / total * 100);
        if (pct > 50) {
            insights.add(insight("warning", "📋",
                    String.format("Les événements de type \"%s\" représentent %d%% du total. Pensez à diversifier les types d'activités.", topType, pct)));
        } else {
            insights.add(insight("success", "✅",
                    String.format("Bonne diversité d'événements ! Le type le plus fréquent (\"%s\") ne représente que %d%% du total.", topType, pct)));
        }

        // Budget analysis
        Map<String, Object> overview = getOverview();
        double avgBudget = (double) overview.get("avgBudget");
        if (avgBudget > 0) {
            insights.add(insight("info", "💰",
                    String.format("Budget moyen par événement : %.2f TND. Budget total alloué : %.2f TND.", avgBudget, (double) overview.get("totalBudget"))));
        }

        // Participation rate
        double avgPart = (double) overview.get("avgParticipants");
        if (avgPart < 2) {
            insights.add(insight("warning", "👥",
                    String.format("Le taux de participation moyen est faible (%.1f participants/événement). Envisagez des actions de communication.", avgPart)));
        } else if (avgPart >= 5) {
            insights.add(insight("success", "🎉",
                    String.format("Excellent taux de participation ! Moyenne de %.1f participants par événement.", avgPart)));
        }

        // Upcoming events
        int upcoming = (int) overview.get("upcoming");
        int pastCount = (int) overview.get("past");
        if (upcoming == 0) {
            insights.add(insight("alert", "⚠️", "Aucun événement à venir n'est planifié. Il est recommandé de planifier les prochaines activités."));
        } else {
            insights.add(insight("info", "📅", String.format("%d événement(s) à venir. %d événement(s) passé(s).", upcoming, pastCount)));
        }

        // Cancellation rate
        Map<String, Integer> statuses = getStatusBreakdown();
        int cancelled = statuses.getOrDefault("annulé", 0) + statuses.getOrDefault("annule", 0);
        if (cancelled > 0 && (float) cancelled / total > 0.2) {
            int cancelPct = Math.round((float) cancelled / total * 100);
            insights.add(insight("alert", "🚫",
                    String.format("%d%% des événements ont été annulés. Analysez les causes d'annulation pour améliorer la planification.", cancelPct)));
        }

        return insights;
    }

    private Map<String, String> insight(String type, String icon, String text) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("icon", icon);
        m.put("text", text);
        return m;
    }
}
