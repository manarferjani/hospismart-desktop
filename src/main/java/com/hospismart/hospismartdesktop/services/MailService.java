package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.Medicament;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

/**
 * Service d'envoi d'emails d'alerte pour les ruptures de stock.
 * Utilise l'API JavaMail (Jakarta Mail) avec le serveur SMTP de Gmail.
 *
 * Lorsqu'un mouvement de sortie fait passer la quantité d'un médicament
 * en dessous de son seuil d'alerte, ce service envoie automatiquement
 * un email HTML professionnel au responsable pharmacie.
 */
public class MailService {

    // ===== CONFIGURATION SMTP =====
    // ⚠️ Remplacez par votre adresse Gmail et votre mot de passe d'application
    // Pour générer un mot de passe d'application : https://myaccount.google.com/apppasswords
    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final String SMTP_PORT     = "587";
    private static final String EMAIL_FROM    = "arfaouimahmoud62@gmail.com";
    private static final String EMAIL_PASSWORD = "qese qvib bxst fvsn";
    private static final String EMAIL_TO       = "arfaouimahmoud62@gmail.com";

    /**
     * Envoie un email d'alerte de rupture de stock pour un médicament donné.
     *
     * @param medicament Le médicament dont le stock est critique.
     * @return true si l'email a été envoyé avec succès, false sinon.
     */
    public boolean envoyerAlerteRuptureStock(Medicament medicament) {
        try {
            // 1. Configuration des propriétés SMTP (connexion sécurisée TLS)
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");

            // 2. Création de la session authentifiée
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });

            // 3. Construction du message email
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "Hospismart Alerte"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_TO));
            message.setSubject("⚠️ Alerte Stock Critique — " + medicament.getNom());

            // 4. Construction du corps HTML professionnel
            String htmlContent = construireCorpsEmail(medicament);
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            // 5. Envoi du mail via le serveur SMTP
            Transport.send(message);

            System.out.println("📧 Email d'alerte envoyé avec succès pour : " + medicament.getNom());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Construit le corps HTML de l'email d'alerte avec un design professionnel.
     *
     * @param med Le médicament en alerte.
     * @return Le contenu HTML formaté du mail.
     */
    private String construireCorpsEmail(Medicament med) {
        String statut = med.getQuantite() == 0 ? "RUPTURE TOTALE" : "STOCK CRITIQUE";
        String couleurStatut = med.getQuantite() == 0 ? "#dc3545" : "#f59e0b";

        return "<!DOCTYPE html>" +
            "<html><head><meta charset='UTF-8'></head>" +
            "<body style='margin:0;padding:0;font-family:Segoe UI,Arial,sans-serif;background:#f1f5f9;'>" +
            "<div style='max-width:600px;margin:30px auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);'>" +

            // ===== EN-TÊTE =====
            "  <div style='background:linear-gradient(135deg,#1e293b,#334155);padding:30px 35px;'>" +
            "    <h1 style='color:white;margin:0;font-size:22px;'>⚠️ Alerte Stock — Hospismart</h1>" +
            "    <p style='color:#94a3b8;margin:8px 0 0;font-size:14px;'>Notification automatique de rupture de stock</p>" +
            "  </div>" +

            // ===== BADGE STATUT =====
            "  <div style='padding:25px 35px 0;'>" +
            "    <span style='background:" + couleurStatut + ";color:white;padding:6px 18px;border-radius:20px;font-weight:bold;font-size:13px;'>" +
            statut +
            "    </span>" +
            "  </div>" +

            // ===== DÉTAILS DU MÉDICAMENT =====
            "  <div style='padding:20px 35px;'>" +
            "    <h2 style='color:#1e293b;margin:0 0 15px;font-size:20px;'>💊 " + med.getNom() + "</h2>" +
            "    <table style='width:100%;border-collapse:collapse;'>" +
            "      <tr style='border-bottom:1px solid #f1f5f9;'>" +
            "        <td style='padding:12px 0;color:#64748b;font-weight:bold;width:180px;'>Quantité en stock</td>" +
            "        <td style='padding:12px 0;color:" + couleurStatut + ";font-weight:bold;font-size:18px;'>" + med.getQuantite() + " unité(s)</td>" +
            "      </tr>" +
            "      <tr style='border-bottom:1px solid #f1f5f9;'>" +
            "        <td style='padding:12px 0;color:#64748b;font-weight:bold;'>Seuil d'alerte</td>" +
            "        <td style='padding:12px 0;color:#1e293b;'>" + med.getSeuilAlerte() + " unité(s)</td>" +
            "      </tr>" +
            "      <tr style='border-bottom:1px solid #f1f5f9;'>" +
            "        <td style='padding:12px 0;color:#64748b;font-weight:bold;'>Prix unitaire</td>" +
            "        <td style='padding:12px 0;color:#1e293b;'>" + String.format("%.2f", med.getPrixUnitaire()) + " TND</td>" +
            "      </tr>" +
            "      <tr>" +
            "        <td style='padding:12px 0;color:#64748b;font-weight:bold;'>Date de péremption</td>" +
            "        <td style='padding:12px 0;color:#1e293b;'>" + (med.getDatePeremption() != null ? med.getDatePeremption().toString() : "Non définie") + "</td>" +
            "      </tr>" +
            "    </table>" +
            "  </div>" +

            // ===== MESSAGE D'ACTION =====
            "  <div style='margin:0 35px 25px;padding:15px 20px;background:#fef2f2;border-left:4px solid #dc3545;border-radius:8px;'>" +
            "    <p style='margin:0;color:#991b1b;font-weight:bold;'>🔴 Action requise</p>" +
            "    <p style='margin:5px 0 0;color:#7f1d1d;font-size:14px;'>Veuillez procéder au réapprovisionnement de ce médicament dans les plus brefs délais.</p>" +
            "  </div>" +

            // ===== PIED DE PAGE =====
            "  <div style='background:#f8fafc;padding:20px 35px;text-align:center;border-top:1px solid #e2e8f0;'>" +
            "    <p style='margin:0;color:#94a3b8;font-size:12px;'>📧 Email généré automatiquement par Hospismart — PIDEV 2025-2026</p>" +
            "  </div>" +

            "</div>" +
            "</body></html>";
    }
}
