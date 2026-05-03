package com.hospismart.hospismartdesktop.main;

import com.hospismart.hospismartdesktop.models.User;
import com.hospismart.hospismartdesktop.utils.Session;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

/**
 * Point d'entrée principal de l'application Hospismart.
 * Gère la navigation centralisée selon les rôles des utilisateurs.
 */
public class JavaFxMain extends Application {

    private static Stage primaryStage;

    public static void showFrontoffice() {

    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // 1. Chargement des polices globales
        loadGlobalFonts();

        // Ajout automatique du compte Medecin s'il n'existe pas
        try {
            com.hospismart.hospismartdesktop.services.UserService userService = new com.hospismart.hospismartdesktop.services.UserService();
            User medecinUser = userService.login("manarferjani@gmail.com", "medecin");
            if (medecinUser == null) {
                User newMedecin = new User();
                newMedecin.setNom("Ferjani");
                newMedecin.setPrenom("Manar");
                newMedecin.setEmail("manarferjani@gmail.com");
                newMedecin.setPassword("medecin");
                newMedecin.setType("ROLE_MEDECIN");
                userService.ajouter(newMedecin);
                System.out.println("[INFO] Le compte médecin 'manarferjani@gmail.com' a été créé avec succès dans la base de données.");
            }
        } catch (Exception e) {
            System.out.println("[Erreur] Impossible de créer le compte médecin (Manar): " + e.getMessage());
        }

        // Ajout automatique du compte Taher Ben Alaya (Pédiatrie)
        try {
            com.hospismart.hospismartdesktop.services.UserService userService = new com.hospismart.hospismartdesktop.services.UserService();
            com.hospismart.hospismartdesktop.services.ServiceService sService = new com.hospismart.hospismartdesktop.services.ServiceService();

            int pediatrieId = -1;
            for (com.hospismart.hospismartdesktop.models.Service s : sService.findALL()) {
                if (s.getNom().toLowerCase().contains("pediatrie") || s.getNom().toLowerCase().contains("pédiatrie")) {
                    pediatrieId = s.getId();
                    break;
                }
            }

            User taherUser = userService.login("taher@medecin.com", "medecin");
            if (taherUser == null) {
                User newTaher = new User();
                newTaher.setNom("Ben Alaya");
                newTaher.setPrenom("Taher");
                newTaher.setEmail("taher@medecin.com");
                newTaher.setPassword("medecin");
                newTaher.setType("ROLE_MEDECIN");
                newTaher.setSpecialite("Pédiatre");
                if (pediatrieId != -1) {
                    newTaher.setServiceId(pediatrieId);
                } else {
                    System.out.println("[Avertissement] Service 'Pédiatrie' introuvable ! Utilisateur Taher créé sans service.");
                }
                userService.ajouter(newTaher);
                System.out.println("[INFO] Le compte médecin 'taher@medecin.com' a été créé avec succès dans la base de données.");
            } else {
                // If the user was previously created without a service or specialite, update them
                if (taherUser.getServiceId() <= 0 && pediatrieId != -1) {
                    taherUser.setServiceId(pediatrieId);
                    taherUser.setSpecialite("Pédiatre");
                    userService.modifier(taherUser);
                    System.out.println("[INFO] Le service du Dr. Taher a été mis à jour.");
                }
            }
        } catch (Exception e) {
            System.out.println("[Erreur] Impossible de créer le compte médecin (Taher): " + e.getMessage());
        }

        // 2. Vérification de la session (si l'utilisateur est déjà connecté)
        User currentUser = Session.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Redirection automatique si déjà connecté
            showDashboard(currentUser);
        } else {
            // Sinon, afficher la page de connexion
            setRoot("/com/hospismart/hospismartdesktop/Login.fxml", "Hospismart - Connexion");
        }

        stage.show();
    }

    /**
     * Redirige l'utilisateur vers le bon tableau de bord selon son rôle.
     */
    public static void showDashboard(User user) {
        String role = (user.getType() != null) ? user.getType().toUpperCase() : "";

        System.out.println("[Navigation] Rôle détecté : " + role);
        if (role.contains("ADMIN")) {
            System.out.println("[Navigation] Redirection vers le Back-Office (Admin)");
            setRoot("/com/hospismart/hospismartdesktop/main.fxml", "Hospismart - Administration");
        }
        else if (role.contains("MEDECIN") || role.contains("PRATICIEN")) {
            System.out.println("[Navigation] Redirection vers le Back-Office (Médecin)");
            setRoot("/BackDashboard.fxml", "Hospismart - Tableau de bord Praticien");
        }
        else {
            System.out.println("[Navigation] Redirection vers le Front-Office (Patient)");
            setRoot("/TrouverMedecin.fxml", "Hospismart - Espace Patient");
        }
    }

    /**
     * Change la racine de la scène principale.
     */
    public static void setRoot(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(JavaFxMain.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene;
            if (primaryStage.getScene() == null) {
                scene = new Scene(root, 1200, 800);
                primaryStage.setScene(scene);
            } else {
                scene = primaryStage.getScene();
                scene.setRoot(root);
            }

            // Appliquer le CSS global
            applyGlobalStyles(scene);

            if (title != null) {
                primaryStage.setTitle(title);
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur lors du chargement de la vue : " + fxmlPath);
            e.printStackTrace();
        }
    }

    private static void loadGlobalFonts() {
        Font.loadFont(JavaFxMain.class.getResourceAsStream("/fonts/Montserrat-Bold.ttf"), 14);
        Font.loadFont(JavaFxMain.class.getResourceAsStream("/fonts/Montserrat-Regular.ttf"), 14);
    }

    private static void applyGlobalStyles(Scene scene) {
        URL cssURL = JavaFxMain.class.getResource("/css/style.css");
        if (cssURL != null) {
            String css = cssURL.toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
