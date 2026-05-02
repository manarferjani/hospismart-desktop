package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Consultation;
import com.hospismart.hospismartdesktop.models.User;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;

public class PdfService {

    private final UserService userService = new UserService();

    public String getCompiledHtml(Consultation consultation) throws Exception {
        com.hospismart.hospismartdesktop.models.User medecin = userService.findById(consultation.getMedecinId());
        com.hospismart.hospismartdesktop.models.User patient = userService.findById(consultation.getPatientId());

        java.io.InputStream is = getClass().getResourceAsStream("/ordonnance_template.html");
        if (is == null) {
            is = getClass().getResourceAsStream("/com/hospismart/hospismartdesktop/views/ordonnance_template.html");
        }
        if (is == null) {
            throw new java.io.IOException("Template HTML introuvable : ordonnance_template.html");
        }
        
        String htmlContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        // Map data to template
        String medecinNom = (medecin != null) ? medecin.getNom().toUpperCase() : "DOCTEUR";
        String medecinPrenom = (medecin != null) ? medecin.getPrenom() : "";
        String medecinSpecialite = (medecin != null && medecin.getSpecialite() != null) ? medecin.getSpecialite() : "Médecin spécialiste";
        String medecinMatricule = (medecin != null && medecin.getMatricule() != null) ? medecin.getMatricule() : "N/A";

        String patientNom = (patient != null) ? patient.getNom().toUpperCase() : "PATIENT";
        String patientPrenom = (patient != null) ? patient.getPrenom() : "";
        String patientGroupe = (patient != null && patient.getGroupeSanguin() != null) ? patient.getGroupeSanguin() : "Non renseigné";
        
        String ageStr = "N/A";
        if (patient != null && patient.getDateNaissance() != null) {
            int age = java.time.Period.between(patient.getDateNaissance(), java.time.LocalDate.now()).getYears();
            ageStr = age + " ans";
        }

        htmlContent = htmlContent.replace("{{ consultation.medecin.nom|upper }}", medecinNom)
                                 .replace("{{ consultation.medecin.prenom }}", medecinPrenom)
                                 .replace("{{ consultation.medecin.specialite|default('Médecin Généraliste') }}", medecinSpecialite)
                                 .replace("{{ consultation.medecin.matricule }}", medecinMatricule)
                                 .replace("{{ consultation.patient.prenom }}", patientPrenom)
                                 .replace("{{ consultation.patient.nom|upper }}", patientNom)
                                 .replace("{{ consultation.patient.groupeSanguin|default('Non renseigné') }}", patientGroupe)
                                 .replace("{{ \"now\"|date('Y') - consultation.patient.dateNaissance|date('Y') }} ans", ageStr)
                                 .replace("{{ consultation.dateHeure|date('d/m/Y à H:i') }}", consultation.getDateHeure().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")))
                                 .replace("{{ consultation.dateHeure|date('d/m/Y') }}", consultation.getDateHeure().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                 .replace("{{ consultation.traitement|nl2br }}", (consultation.getTraitement() != null ? consultation.getTraitement() : "Aucun traitement prescrit").replace("\n", "<br/>"))
                                 .replace("{{ consultation.diagnostic }}", (consultation.getDiagnostic() != null ? consultation.getDiagnostic() : "Non spécifié"))
                                 .replace("{{ consultation.recommandations|nl2br }}", (consultation.getRecommandations() != null ? consultation.getRecommandations() : "Aucune").replace("\n", "<br/>"))
                                 .replace("{{ consultation.examensComplementaires|nl2br }}", (consultation.getExamensComplementaires() != null ? consultation.getExamensComplementaires() : "Aucun").replace("\n", "<br/>"));
        
        htmlContent = htmlContent.replaceAll("\\{%[\\s\\S]*?%\\}", "");
        htmlContent = htmlContent.replaceAll("\\{\\{[\\s\\S]*?\\}\\}", "");
        
        if (!htmlContent.startsWith("<!DOCTYPE")) {
             htmlContent = "<!DOCTYPE html>\n" + htmlContent;
        }
        return htmlContent;
    }

    public String generateOrdonnancePdf(Consultation consultation) throws Exception {
        String htmlContent = getCompiledHtml(consultation);
        String outputFileName = "Ordonnance_" + consultation.getId() + "_" + System.currentTimeMillis() + ".pdf";
        String outputPath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + outputFileName;

        try (OutputStream os = new FileOutputStream(outputPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            // On s'assure que le contenu est bien interprété comme du XML/XHTML
            builder.withHtmlContent(htmlContent, new File(".").toURI().toURL().toString());
            builder.toStream(os);
            builder.run();
        }

        return outputPath;
    }
}