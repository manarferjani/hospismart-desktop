package com.hospismart.hospismartdesktop.models;


import java.time.LocalDateTime;

public class ParametreVital {
    private int id;
    private String tension;
    private double temperature;
    private int frequenceCardiaque;
    private LocalDateTime datePrise;

    public ParametreVital() {
        this.datePrise = LocalDateTime.now();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTension() { return tension; }
    public void setTension(String tension) { this.tension = tension; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getFrequenceCardiaque() { return frequenceCardiaque; }
    public void setFrequenceCardiaque(int frequenceCardiaque) { this.frequenceCardiaque = frequenceCardiaque; }

    public LocalDateTime getDatePrise() { return datePrise; }
    public void setDatePrise(LocalDateTime datePrise) { this.datePrise = datePrise; }

    @Override
    public String toString() {
        return "Tension: " + tension + ", Temp: " + temperature + "°C";
    }
}
