package com.hospismart.hospismartdesktop.services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    // Configuration SMTP
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    
    // Configuration Gmail
    private static final String SMTP_USER = "tabeagle947@gmail.com";
    private static final String SMTP_PASSWORD = "rcdf rczc tgbq wamw";

    /**
     * Envoie un email de réinitialisation de mot de passe
     * @param toEmail Email du destinataire
     * @param newPassword Nouveau mot de passe
     * @return true si l'email a été envoyé avec succès
     */
    public static boolean sendResetPasswordEmail(String toEmail, String newPassword) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Hospismart - Réinitialisation de mot de passe");
            
            String htmlContent = "<html><body style=\"font-family: Arial, sans-serif;\">" +
                    "<h3 style=\"color: #1565C0;\">Bonjour,</h3>" +
                    "<p>Suite à votre demande, nous avons réinitialisé votre mot de passe.</p>" +
                    "<p><strong>Votre nouveau mot de passe est :</strong></p>" +
                    "<p style=\"background-color: #f0f0f0; padding: 10px; border-left: 4px solid #1565C0;\">" +
                    "<code style=\"font-size: 14px;\">" + escapeHtml(newPassword) + "</code></p>" +
                    "<p style=\"color: #d32f2f;\"><strong>⚠️ Important :</strong> Nous vous recommandons fortement de changer ce mot de passe dès votre prochaine connexion.</p>" +
                    "<br><p>Cordialement,<br>L'équipe Hospismart</p>" +
                    "</body></html>";
                    
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("[EmailService] Email de réinitialisation envoyé avec succès à: " + toEmail);
            return true;
            
        } catch (AuthenticationFailedException e) {
            System.err.println("[EmailService] ERREUR d'authentification SMTP: " + e.getMessage());
            System.err.println("[EmailService] Vérifiez l'email et le mot de passe d'application Gmail");
            return false;
        } catch (MessagingException e) {
            System.err.println("[EmailService] ERREUR d'envoi d'email: " + e.getMessage());
            if (e.getMessage().contains("535")) {
                System.err.println("[EmailService] Erreur 535: Identifiants SMTP invalides");
            } else if (e.getMessage().contains("failure")) {
                System.err.println("[EmailService] Erreur de connexion SMTP");
            }
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("[EmailService] ERREUR inattendue: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Échappe les caractères spéciaux HTML
     */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Envoie un email de bienvenue (exemple)
     */
    public static boolean sendWelcomeEmail(String toEmail, String userName) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Bienvenue sur Hospismart");
            
            String htmlContent = "<html><body>" +
                    "<h3>Bienvenue " + escapeHtml(userName) + ",</h3>" +
                    "<p>Votre compte a été créé avec succès.</p>" +
                    "<p>Vous pouvez maintenant vous connecter à Hospismart.</p>" +
                    "</body></html>";
                    
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            System.err.println("[EmailService] Erreur d'envoi d'email de bienvenue: " + e.getMessage());
            return false;
        }
    }
}
