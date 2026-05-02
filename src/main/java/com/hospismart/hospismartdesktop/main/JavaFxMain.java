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

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // 1. Chargement des polices globales
        loadGlobalFonts();

        // 2. Vérification de la session (si l'utilisateur est déjà connecté)
        User currentUser = Session.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            // Redirection automatique si déjà connecté
            showDashboard(currentUser);
        } else {
            // Sinon, afficher la page de connexion (ou le launcher selon votre choix)
            // Ici, on commence par le Launcher pour garder votre structure actuelle
            setRoot("/com/hospismart/hospismartdesktop/launcher.fxml", "Hospismart - Menu Principal");
        }

        stage.show();
    }

    /**
     * Redirige l'utilisateur vers le bon tableau de bord selon son rôle.
     */
    public static void showDashboard(User user) {
        String role = (user.getType() != null) ? user.getType().toUpperCase() : "";
        
        // Si medecin ou admin -> BackDashboard
        if (role.contains("ADMIN") || role.contains("MEDECIN")) {
            System.out.println("[Navigation] Redirection vers le Back-Office (Admin/Médecin)");
            setRoot("/BackDashboard.fxml", "Hospismart - Tableau de bord Praticien");
        } 
        // Si patient -> TrouverMedecin
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