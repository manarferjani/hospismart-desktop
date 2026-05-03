package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Consultation;
import com.hospismart.hospismartdesktop.services.UserService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.io.File;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.text.Normalizer;

public class PdfService {

    private final UserService userService = new UserService();

    public String getCompiledHtml(Consultation consultation) throws Exception {
        com.hospismart.hospismartdesktop.models.User medecin = userService.findById(consultation.getMedecinId());
        com.hospismart.hospismartdesktop.models.User patient = userService.findById(consultation.getPatientId());

        String htmlContent;
        try (InputStream is = getClass().getResourceAsStream("/ordonnance_template.html") != null
                ? getClass().getResourceAsStream("/ordonnance_template.html")
                : getClass().getResourceAsStream("/com/hospismart/hospismartdesktop/views/ordonnance_template.html")) {
            if (is == null) {
                throw new java.io.IOException("Template HTML introuvable : ordonnance_template.html");
            }
            htmlContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

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

        htmlContent = htmlContent.replaceAll("(?s)\\{%.*?%\\}", "");
        htmlContent = htmlContent.replaceAll("(?s)\\{\\{.*?\\}\\}", "");

        if (!htmlContent.startsWith("<!DOCTYPE")) {
             htmlContent = "<!DOCTYPE html>\n" + htmlContent;
        }
        return htmlContent;
    }

    public String generateOrdonnancePdf(Consultation consultation) throws Exception {
        String outputFileName = "Ordonnance_" + consultation.getId() + "_" + System.currentTimeMillis() + ".pdf";
        String outputPath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + outputFileName;

        String htmlContent = getCompiledHtml(consultation);
        List<String> lines = buildPdfLines(htmlContent);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 11);
                contentStream.newLineAtOffset(50, 780);

                for (String line : lines) {
                    contentStream.showText(safePdfText(line));
                    contentStream.newLineAtOffset(0, -15);
                }

                contentStream.endText();
            }

            document.save(new File(outputPath));
        }

        return outputPath;
    }

    private List<String> buildPdfLines(String htmlContent) {
        String plainText = htmlContent.replaceAll("(?s)<[^>]+>", " ")
                                      .replaceAll("\\s+", " ")
                                      .trim();

        List<String> lines = new ArrayList<>();
        int maxChars = 90;
        for (int i = 0; i < plainText.length(); i += maxChars) {
            lines.add(plainText.substring(i, Math.min(i + maxChars, plainText.length())));
        }
        if (lines.isEmpty()) {
            lines.add("Ordonnance générée avec succès");
        }
        return lines;
    }

    private String safePdfText(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
