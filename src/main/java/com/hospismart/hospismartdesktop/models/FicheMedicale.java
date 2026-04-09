package com.hospismart.hospismartdesktop.models;

import java.time.LocalDateTime;

public class FicheMedicale {
    private int id;
    private int patientId;
    private String mainSymptom;
    private String location;
    private String duration;
    private String intensity;
    private String summary;
    private String alertLevel;
    private boolean isConfirmed;
    private LocalDateTime createdAt;

    public FicheMedicale() {
        this.createdAt = LocalDateTime.now();
        this.isConfirmed = false;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getMainSymptom() { return mainSymptom; }
    public void setMainSymptom(String mainSymptom) { this.mainSymptom = mainSymptom; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getIntensity() { return intensity; }
    public void setIntensity(String intensity) { this.intensity = intensity; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }

    public boolean isConfirmed() { return isConfirmed; }
    public void setConfirmed(boolean confirmed) { isConfirmed = confirmed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}