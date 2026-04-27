package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Evenement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weather service using Open-Meteo API (100% free, no API key required).
 * https://open-meteo.com/
 */
public class WeatherService {

    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Get weather forecast for a single event based on its coordinates and date.
     * Uses Open-Meteo free API — no key needed.
     *
     * @return Map with keys: temp_c, temp_min_c, temp_max_c, condition_text,
     *         condition_icon, humidity, wind_kph, chance_of_rain — or null
     */
    public Map<String, Object> getForecastForEvent(Evenement event) {
        Double lat = event.getLatitude();
        Double lng = event.getLongitude();
        if (lat == null || lng == null || event.getDateDebut() == null) return null;

        LocalDate eventDate = event.getDateDebut().toLocalDate();
        long daysFromNow = ChronoUnit.DAYS.between(LocalDate.now(), eventDate);

        // Open-Meteo forecast supports up to 16 days ahead
        // For past dates or dates > 16 days, use current weather as approximation
        boolean useForecast = daysFromNow >= 0 && daysFromNow <= 16;

        try {
            String url;
            if (useForecast) {
                url = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max," +
                    "weathercode,windspeed_10m_max&current=temperature_2m,relative_humidity_2m" +
                    "&start_date=%s&end_date=%s&timezone=auto",
                    lat, lng, eventDate, eventDate
                );
            } else {
                // For dates outside forecast range, get current weather
                url = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s" +
                    "&current=temperature_2m,relative_humidity_2m,weathercode,windspeed_10m" +
                    "&timezone=auto",
                    lat, lng
                );
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Open-Meteo HTTP " + response.statusCode() + ": " + response.body());
                return null;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            Map<String, Object> result = new HashMap<>();

            if (useForecast && root.has("daily")) {
                JsonObject daily = root.getAsJsonObject("daily");

                JsonArray maxTemps = daily.getAsJsonArray("temperature_2m_max");
                JsonArray minTemps = daily.getAsJsonArray("temperature_2m_min");
                JsonArray rainChance = daily.getAsJsonArray("precipitation_probability_max");
                JsonArray weatherCodes = daily.getAsJsonArray("weathercode");
                JsonArray windMax = daily.getAsJsonArray("windspeed_10m_max");

                double tMax = maxTemps != null && maxTemps.size() > 0 ? maxTemps.get(0).getAsDouble() : 0;
                double tMin = minTemps != null && minTemps.size() > 0 ? minTemps.get(0).getAsDouble() : 0;
                double avgTemp = (tMax + tMin) / 2.0;

                result.put("temp_c", avgTemp);
                result.put("temp_min_c", tMin);
                result.put("temp_max_c", tMax);
                result.put("chance_of_rain", rainChance != null && rainChance.size() > 0 ? rainChance.get(0).getAsDouble() : 0.0);
                result.put("wind_kph", windMax != null && windMax.size() > 0 ? windMax.get(0).getAsDouble() : 0.0);

                int wmoCode = weatherCodes != null && weatherCodes.size() > 0 ? weatherCodes.get(0).getAsInt() : 0;
                result.put("condition_text", wmoToDescription(wmoCode));
                result.put("condition_icon", wmoToIconUrl(wmoCode));

                // Get humidity from current if available
                if (root.has("current")) {
                    JsonObject current = root.getAsJsonObject("current");
                    if (current.has("relative_humidity_2m")) {
                        result.put("humidity", current.get("relative_humidity_2m").getAsDouble());
                    }
                }
            } else if (root.has("current")) {
                JsonObject current = root.getAsJsonObject("current");
                double temp = current.has("temperature_2m") ? current.get("temperature_2m").getAsDouble() : 0;
                result.put("temp_c", temp);
                result.put("temp_min_c", temp);
                result.put("temp_max_c", temp);
                result.put("humidity", current.has("relative_humidity_2m") ? current.get("relative_humidity_2m").getAsDouble() : null);
                result.put("wind_kph", current.has("windspeed_10m") ? current.get("windspeed_10m").getAsDouble() : null);
                result.put("chance_of_rain", 0.0);

                int wmoCode = current.has("weathercode") ? current.get("weathercode").getAsInt() : 0;
                result.put("condition_text", wmoToDescription(wmoCode) + " (actuel)");
                result.put("condition_icon", wmoToIconUrl(wmoCode));
            } else {
                return null;
            }

            return result;

        } catch (Exception e) {
            System.err.println("WeatherService error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get weather forecasts for multiple events.
     * @return Map indexed by event ID
     */
    public Map<Integer, Map<String, Object>> getForecastsForEvents(List<Evenement> events) {
        Map<Integer, Map<String, Object>> results = new HashMap<>();
        for (Evenement event : events) {
            Map<String, Object> forecast = getForecastForEvent(event);
            results.put(event.getId(), forecast);
        }
        return results;
    }

    // ── WMO Weather Code mappings ────────────────────────────────────────────

    /**
     * Convert WMO weather code to human-readable French description.
     * https://open-meteo.com/en/docs#weathervariables
     */
    private String wmoToDescription(int code) {
        return switch (code) {
            case 0 -> "Ciel dégagé";
            case 1 -> "Principalement dégagé";
            case 2 -> "Partiellement nuageux";
            case 3 -> "Couvert";
            case 45, 48 -> "Brouillard";
            case 51 -> "Bruine légère";
            case 53 -> "Bruine modérée";
            case 55 -> "Bruine dense";
            case 56, 57 -> "Bruine verglaçante";
            case 61 -> "Pluie légère";
            case 63 -> "Pluie modérée";
            case 65 -> "Pluie forte";
            case 66, 67 -> "Pluie verglaçante";
            case 71 -> "Neige légère";
            case 73 -> "Neige modérée";
            case 75 -> "Neige forte";
            case 77 -> "Grains de neige";
            case 80 -> "Averses légères";
            case 81 -> "Averses modérées";
            case 82 -> "Averses violentes";
            case 85, 86 -> "Averses de neige";
            case 95 -> "Orage";
            case 96, 99 -> "Orage avec grêle";
            default -> "Variable";
        };
    }

    /**
     * Convert WMO weather code to an icon URL (using Open-Meteo's WMO standard icons).
     */
    private String wmoToIconUrl(int code) {
        // Use wttr.in icons as a reliable free source
        String icon = switch (code) {
            case 0, 1 -> "113"; // Sunny / Clear
            case 2 -> "116";    // Partly cloudy
            case 3 -> "119";    // Cloudy
            case 45, 48 -> "143"; // Fog
            case 51, 53, 55, 56, 57 -> "266"; // Drizzle
            case 61, 63, 80, 81 -> "296"; // Light/moderate rain
            case 65, 82 -> "308"; // Heavy rain
            case 66, 67 -> "311"; // Freezing rain
            case 71, 73, 77, 85 -> "326"; // Light snow
            case 75, 86 -> "338"; // Heavy snow
            case 95 -> "389"; // Thunderstorm
            case 96, 99 -> "395"; // Thunderstorm with hail
            default -> "116";
        };
        return "https://cdn.worldweatheronline.com/images/wsymbols01_png_64/wsymbol_0" +
               (icon.equals("113") ? "001" : icon.equals("116") ? "002" : icon.equals("119") ? "003" :
                icon.equals("143") ? "007" : icon.equals("266") ? "017" : icon.equals("296") ? "017" :
                icon.equals("308") ? "018" : icon.equals("311") ? "021" : icon.equals("326") ? "011" :
                icon.equals("338") ? "012" : icon.equals("389") ? "024" : icon.equals("395") ? "024" : "002") +
               "_night.png";
    }
}
