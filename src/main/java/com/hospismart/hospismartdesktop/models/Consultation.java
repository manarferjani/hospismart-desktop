package com.hospismart.hospismartdesktop.models;


import java.time.LocalDateTime;

public class Consultation {
    public static final int PRIORITE_URGENT = 5;
    public static final int PRIORITE_ELEVE = 4;
    public static final int PRIORITE_MOYEN = 3;
    public static final int PRIORITE_STANDARD = 2;
    public static final int PRIORITE_ADMIN = 1;

    private int id;
    private int priorite;
    private LocalDateTime dateHeure;
    private String statut; // EN_ATTENTE, TERMINEE, ANNULEE
    private String motif;
    private String observations;
    private int patientId;
    private int medecinId;
    private int rendezVousId;
    private String examenClinique;
    private String diagnostic;
    private String traitement;
    private String examensComplementaires;
    private String recommandations;

    public Consultation() {
        this.dateHeure = LocalDateTime.now();
        this.priorite = PRIORITE_STANDARD;
        this.statut = "EN_ATTENTE";
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPriorite() { return priorite; }
    public void setPriorite(int priorite) { this.priorite = priorite; }

    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }

    public int getRendezVousId() { return rendezVousId; }
    public void setRendezVousId(int rendezVousId) { this.rendezVousId = rendezVousId; }

    public String getExamenClinique() { return examenClinique; }
    public void setExamenClinique(String examenClinique) { this.examenClinique = examenClinique; }

    public String getDiagnostic() { return diagnostic; }
    public void setDiagnostic(String diagnostic) { this.diagnostic = diagnostic; }

    public String getTraitement() { return traitement; }
    public void setTraitement(String traitement) { this.traitement = traitement; }

    public String getExamensComplementaires() { return examensComplementaires; }
    public void setExamensComplementaires(String examensComplementaires) { this.examensComplementaires = examensComplementaires; }

    public String getRecommandations() { return recommandations; }
    public void setRecommandations(String recommandations) { this.recommandations = recommandations; }

    @Override
    public String toString() {
        return "Consultation #" + id + " - " + motif;
    }
}
