package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Evenement;
import com.hospismart.hospismartdesktop.models.Inscription;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF Export Service for Events — generates professional event reports.
 */
public class PdfExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    // Colors matching the app design
    private static final BaseColor PRIMARY = new BaseColor(74, 124, 247);      // #4a7cf7
    private static final BaseColor DARK = new BaseColor(30, 41, 59);           // #1e293b
    private static final BaseColor GRAY = new BaseColor(100, 116, 139);        // #64748b
    private static final BaseColor LIGHT_BG = new BaseColor(245, 247, 251);    // #f5f7fb
    private static final BaseColor GREEN = new BaseColor(34, 197, 94);         // #22c55e
    private static final BaseColor WHITE = BaseColor.WHITE;

    /**
     * Export a single event to a PDF file.
     * @return the generated File, or null on error
     */
    public File exportEvent(Evenement event, List<Inscription> participants) {
        try {
            // Save to user's Desktop
            String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
            String fileName = "Evenement_" + sanitize(event.getTitre()) + "_" + event.getId() + ".pdf";
            File outputFile = new File(desktopPath, fileName);

            Document document = new Document(PageSize.A4, 40, 40, 50, 40);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            // ── Header Banner ─────────────────────────────────────────
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(PRIMARY);
            headerCell.setPadding(20);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, WHITE);
            Font subFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(200, 220, 255));

            Paragraph headerTitle = new Paragraph("HospiSmart — Fiche Événement", titleFont);
            headerTitle.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(headerTitle);

            Paragraph headerSub = new Paragraph("Document généré le " + LocalDateTime.now().format(DATE_FMT), subFont);
            headerSub.setAlignment(Element.ALIGN_CENTER);
            headerSub.setSpacingBefore(4);
            headerCell.addElement(headerSub);

            headerTable.addCell(headerCell);
            document.add(headerTable);
            document.add(new Paragraph(" "));

            // ── Event Title Section ───────────────────────────────────
            Font eventTitleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, DARK);
            Paragraph eventTitle = new Paragraph(event.getTitre(), eventTitleFont);
            eventTitle.setSpacingAfter(8);
            document.add(eventTitle);

            // Status + Type badges
            Font badgeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, GRAY);
            Paragraph badges = new Paragraph();
            badges.add(new Chunk("Type: ", badgeFont));
            badges.add(new Chunk(nvl(event.getTypeEvenement()), new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, PRIMARY)));
            badges.add(new Chunk("     Statut: ", badgeFont));
            BaseColor statusColor = getStatusColor(event.getStatut());
            badges.add(new Chunk(nvl(event.getStatut()), new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, statusColor)));
            badges.setSpacingAfter(14);
            document.add(badges);

            // ── Details Table ─────────────────────────────────────────
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{1, 2.5f});
            detailsTable.setSpacingAfter(16);

            addDetailRow(detailsTable, "Date de début", event.getDateDebut() != null ? event.getDateDebut().format(FMT) : "-");
            addDetailRow(detailsTable, "Date de fin", event.getDateFin() != null ? event.getDateFin().format(FMT) : "-");
            addDetailRow(detailsTable, "Lieu", nvl(event.getLieu()));
            addDetailRow(detailsTable, "Budget", event.getBudgetAlloue() > 0 ? String.format("%.2f TND", event.getBudgetAlloue()) : "Non défini");

            if (event.getLatitude() != null && event.getLongitude() != null) {
                addDetailRow(detailsTable, "Coordonnées", String.format("%.5f, %.5f", event.getLatitude(), event.getLongitude()));
            }

            document.add(detailsTable);

            // ── Description ───────────────────────────────────────────
            if (event.getDescription() != null && !event.getDescription().isBlank()) {
                Font sectionFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, DARK);
                document.add(new Paragraph("Description", sectionFont));
                document.add(new Paragraph(" "));

                PdfPTable descBox = new PdfPTable(1);
                descBox.setWidthPercentage(100);
                PdfPCell descCell = new PdfPCell();
                descCell.setBackgroundColor(LIGHT_BG);
                descCell.setPadding(12);
                descCell.setBorderColor(new BaseColor(224, 229, 238));
                descCell.addElement(new Paragraph(event.getDescription(),
                        new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, DARK)));
                descBox.addCell(descCell);
                descBox.setSpacingAfter(16);
                document.add(descBox);
            }

            // ── Participants Table ────────────────────────────────────
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, DARK);
            document.add(new Paragraph("Participants (" + participants.size() + ")", sectionFont));
            document.add(new Paragraph(" "));

            if (participants.isEmpty()) {
                document.add(new Paragraph("Aucun participant inscrit pour cet événement.",
                        new Font(Font.FontFamily.HELVETICA, 11, Font.ITALIC, GRAY)));
            } else {
                PdfPTable partTable = new PdfPTable(4);
                partTable.setWidthPercentage(100);
                partTable.setWidths(new float[]{0.5f, 2f, 2.5f, 1.5f});

                // Table header
                addTableHeader(partTable, "#");
                addTableHeader(partTable, "Nom");
                addTableHeader(partTable, "Email");
                addTableHeader(partTable, "Téléphone");

                // Table rows
                int idx = 1;
                for (Inscription p : participants) {
                    addTableCell(partTable, String.valueOf(idx++), idx % 2 == 0);
                    addTableCell(partTable, nvl(p.getNomParticipant()), idx % 2 == 0);
                    addTableCell(partTable, nvl(p.getEmailParticipant()), idx % 2 == 0);
                    addTableCell(partTable, p.getTelephoneParticipant() != null ? p.getTelephoneParticipant() : "-", idx % 2 == 0);
                }

                partTable.setSpacingAfter(16);
                document.add(partTable);
            }

            // ── Footer ─────────────────────────────────────────────────
            document.add(new Paragraph(" "));
            LineSeparator line = new LineSeparator();
            line.setLineColor(new BaseColor(224, 229, 238));
            document.add(new Chunk(line));
            document.add(new Paragraph(" "));

            Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, GRAY);
            Paragraph footer = new Paragraph(
                    "Document généré automatiquement par HospiSmart Desktop — PIDEV 2025-2026", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return outputFile;

        } catch (Exception e) {
            System.err.println("PdfExportService error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ── Table Helpers ─────────────────────────────────────────────────────────

    private void addDetailRow(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, GRAY);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, DARK);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(new BaseColor(241, 245, 249));
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(LIGHT_BG);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(new BaseColor(241, 245, 249));
        valueCell.setPadding(8);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String text) {
        Font hFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, hFont));
        cell.setBackgroundColor(PRIMARY);
        cell.setPadding(8);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, boolean alternate) {
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, DARK);
        PdfPCell cell = new PdfPCell(new Phrase(text, cellFont));
        cell.setPadding(7);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new BaseColor(241, 245, 249));
        if (alternate) cell.setBackgroundColor(LIGHT_BG);
        table.addCell(cell);
    }

    private BaseColor getStatusColor(String statut) {
        if (statut == null) return GRAY;
        return switch (statut.toLowerCase()) {
            case "planifié", "planifie" -> PRIMARY;
            case "en_cours" -> new BaseColor(245, 158, 11);
            case "terminé", "termine" -> GREEN;
            case "annulé", "annule" -> new BaseColor(239, 68, 68);
            default -> GRAY;
        };
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private String sanitize(String s) {
        if (s == null) return "event";
        return s.replaceAll("[^a-zA-Z0-9À-ÿ]", "_").replaceAll("_+", "_");
    }
}
