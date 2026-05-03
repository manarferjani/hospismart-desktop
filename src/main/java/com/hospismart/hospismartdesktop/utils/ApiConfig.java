package com.hospismart.hospismartdesktop.utils;

/**
 * Central configuration for all external API keys.
 * Replace placeholder values with your actual API keys.
 */
public final class ApiConfig {

    private ApiConfig() {
    }

    // ── WeatherAPI.com ──────────────────────────────────────────
    // Free tier: 1M calls/month — https://www.weatherapi.com/
    public static final String WEATHER_API_KEY = "b2e4aa95be7e4b6d92ce4507099ccdad";

    // ── Google Gemini (AI Description) ──────────────────────────
    // Free tier available — https://aistudio.google.com/apikey
    public static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE";

    // ── Geoapify (Geocoding / Map Search) ───────────────────────
    // Free tier: 3000 req/day — https://www.geoapify.com/
    public static final String GEOAPIFY_API_KEY = "89f000efcae2411a893182409262604";

    /**
     * Check if an API key is configured (not placeholder).
     */
    public static boolean isConfigured(String key) {
        return key != null && !key.isBlank()
                && !key.startsWith("YOUR_") && !key.endsWith("_HERE");
    }
}
