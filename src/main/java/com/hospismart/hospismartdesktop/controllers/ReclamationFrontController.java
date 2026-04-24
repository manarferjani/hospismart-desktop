package com.hospismart.hospismartdesktop.controllers; // Package des controllers JavaFX

import com.hospismart.hospismartdesktop.Services.ReclamationDao; // DAO des reclamations
import com.hospismart.hospismartdesktop.Services.ReponseDao; // DAO des reponses
import com.hospismart.hospismartdesktop.models.Reclamation; // Modele Reclamation
import com.hospismart.hospismartdesktop.models.Reponse; // Modele Reponse
import javafx.animation.FadeTransition; // Animation de fondu
import javafx.animation.ScaleTransition; // Animation de zoom
import javafx.collections.FXCollections; // Fabrique de listes observables
import javafx.collections.ObservableList; // Liste observable JavaFX
import javafx.event.ActionEvent; // Evenement d'action (boutons)
import javafx.fxml.FXML; // Annotation de liaison FXML
import javafx.fxml.Initializable; // Interface d'initialisation du controller
import javafx.scene.control.*; // Composants UI JavaFX
import javafx.scene.control.cell.PropertyValueFactory; // Liaison simple colonnes <-> proprietes
import javafx.scene.input.MouseEvent; // Evenement souris

import java.net.URL; // URL de ressource FXML
import java.text.Normalizer; // Normalisation des accents
import java.time.LocalDateTime; // Date/heure locale
import java.time.format.DateTimeFormatter; // Formatage de date
import java.util.ResourceBundle; // Bundle de ressources
import javafx.util.Duration; // Duree des animations

public class ReclamationFrontController implements Initializable { // Controller principal du front office reclamations

    private final ReclamationDao dao = new ReclamationDao(); // DAO des reclamations
    private final ReponseDao reponseDao = new ReponseDao(); // DAO des reponses
    private final ObservableList<Reclamation> reclamationList = FXCollections.observableArrayList(); // Liste observable de la table
    private int selectedId = -1; // Id de la reclamation selectionnee

    @FXML // Injecte la table depuis le FXML
    private TableView<Reclamation> tableReclamation; // Table affichant les reclamations
    @FXML // Injecte la colonne titre
    private TableColumn<Reclamation, String> colTitre; // Colonne du titre
    @FXML // Injecte la colonne statut
    private TableColumn<Reclamation, String> colStatut; // Colonne du statut
    @FXML // Injecte la colonne categorie
    private TableColumn<Reclamation, String> colCategorie; // Colonne de la categorie
    @FXML // Injecte la colonne date
    private TableColumn<Reclamation, LocalDateTime> colDate; // Colonne date de creation

    @FXML // Injecte champ titre
    private TextField txtTitre; // Saisie du titre
    @FXML // Injecte champ nom patient
    private TextField txtPatient; // Saisie du nom patient
    @FXML // Injecte champ email
    private TextField txtEmail; // Saisie de l'email
    @FXML // Injecte combo categorie
    private ComboBox<String> cmbCategorie; // Choix categorie
    @FXML // Injecte combo priorite
    private ComboBox<String> cmbPriorite; // Choix priorite
    @FXML // Injecte zone description
    private TextArea txtDescription; // Saisie description
    @FXML
    private TextArea txtReponse; // Reponse affichee au client

    // ---- Composants Chatbot Gemini ----
    @FXML
    private TextArea txtChatbot; // Zone d'affichage des reponses du chatbot
    @FXML
    private TextField txtChatInput; // Zone de saisie pour parler au chatbot
    @FXML
    private Button btnChatSendMessage;
    @FXML
    private Button btnAutoFill; // Bouton pour remplir auto
    @FXML
    private Button btnAjouter; // Bouton ajouter/sauvegarder
    @FXML
    private Button btnModifier; // Bouton modifier

    @FXML
    private Label lblTotalFront; // KPI total
    @FXML
    private Label lblFrontAttente; // KPI attente
    @FXML
    private Label lblFrontTraite; // KPI traitées

    private String lastPredictedMentalState = "Non défini"; // Stockage de l'état mental prédit par l'IA
    private String lastUserMessage = ""; // Dernier message utilisateur pour le fallback local

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) { // Setup initial
        cmbCategorie.setItems(FXCollections.observableArrayList("Service", "Propreté", "Personnel", "Autre")); // Charge les categories possibles
        cmbPriorite.setItems(FXCollections.observableArrayList("Basse", "Moyenne", "Haute")); // Charge les priorites possibles

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre")); // Lie colonne titre a getTitre()
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut")); // Lie colonne statut a getStatut()
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie")); // Lie colonne categorie a getCategorie()
        setupStatusBadgeCell(); // Applique un rendu colore sur la colonne statut

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation")); // Lie colonne date a getDateCreation()

        // Formate visuellement la date dans la table.
        colDate.setCellFactory(column -> { // Definit un rendu personnalise pour chaque cellule date
            return new TableCell<Reclamation, LocalDateTime>() { // Cellule personnalisee pour LocalDateTime
                @Override // Surcharge du rafraichissement d'une cellule
                protected void updateItem(LocalDateTime item, boolean empty) { // Met a jour le texte de la cellule
                    super.updateItem(item, empty); // Appel parent obligatoire
                    if (item == null || empty) { // Si cellule vide ou valeur absente
                        setText(null); // N'affiche rien
                    } else { // Sinon
                        setText(item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))); // Affiche la date formatee
                    }
                }
            };
        });

        loadTable(); // Charge les reclamations existantes
        activerModeAjout(); // Met l'UI en mode ajout par defaut
        setupAnimations(); // Lance les animations legeres

        // Bloc debug temporaire: ecrit les colonnes BD dans un fichier local.
        try { // Debut tentative de lecture schema table reclamation
            java.sql.Connection cx = com.hospismart.hospismartdesktop.utils.MyDbConnexion.getInstance().getCnx(); // Recupere la connexion BD
            java.sql.Statement s = cx.createStatement(); // Cree un statement SQL
            java.sql.ResultSet res = s.executeQuery("SELECT * FROM reclamation LIMIT 1"); // Interroge la table reclamation
            java.sql.ResultSetMetaData md = res.getMetaData(); // Recupere les metadonnees colonnes
            StringBuilder sb = new StringBuilder("COLONNES:\n"); // Prepare le texte de sortie
            for (int i = 1; i <= md.getColumnCount(); i++) { // Parcourt toutes les colonnes
                sb.append(md.getColumnName(i)).append("\n"); // Ajoute le nom de colonne dans le fichier
            }
            java.nio.file.Files.writeString(java.nio.file.Paths.get("debug_db.txt"), sb.toString()); // Ecrit le fichier debug
        } catch (Exception e) { // Si erreur de debug
            try { // Tentative d'ecrire le message d'erreur
                java.nio.file.Files.writeString(java.nio.file.Paths.get("debug_db.txt"), "ERROR: " + e.getMessage()); // Ecrit l'erreur debug
            } catch (Exception ex) { // Ignore une erreur secondaire d'ecriture
            }
        }
    }

    private void loadTable() { // Recharge les donnees de la table UI
        reclamationList.clear(); // Vide la liste locale
        reclamationList.addAll(dao.getAllReclamations()); // Recharge depuis la base
        tableReclamation.setItems(reclamationList); // Rebranche la liste dans la table
        refreshKpis(); // Met a jour les compteurs
    }

    private void setupStatusBadgeCell() { // Configure les badges de couleur selon statut
        colStatut.setCellFactory(col -> new TableCell<>() { // Defini une cellule personnalisee
            @Override // Surcharge update d'une cellule de statut
            protected void updateItem(String item, boolean empty) { // Met a jour contenu visuel
                super.updateItem(item, empty); // Appel parent
                getStyleClass().removeAll("status-badge-attente", "status-badge-cours", "status-badge-resolu", "status-badge-traite"); // Nettoie anciens styles
                if (empty || item == null) { // Si cellule vide
                    setText(null); // Vide le texte
                    setStyle(""); // Reset style inline
                } else { // Sinon
                    setText(item); // Affiche le statut brut
                    String low = normalizeStatus(item); // Normalise pour comparaison robuste
                    if (low.contains("attente")) getStyleClass().add("status-badge-attente"); // Style attente
                    else if (low.contains("cours")) getStyleClass().add("status-badge-cours"); // Style en cours
                    else if (low.contains("resolu")) getStyleClass().add("status-badge-resolu"); // Style resolu
                    else if (low.contains("ferme") || low.contains("traite")) getStyleClass().add("status-badge-traite"); // Style traite/ferme
                }
            }
        });
    }

    private void refreshKpis() { // Recalcule les compteurs du front
        int total = reclamationList.size(); // Total reclamations chargees
        int attente = 0; // Compteur attente
        int traite = 0; // Compteur traite/resolu/ferme
        for (Reclamation r : reclamationList) { // Parcourt toutes les reclamations
            String s = normalizeStatus(r.getStatut()); // Normalise le statut
            if (s.contains("attente")) attente++; // Incremente attente
            if (s.contains("traite") || s.contains("resolu") || s.contains("ferme")) traite++; // Incremente traite
        }
        if (lblTotalFront != null) lblTotalFront.setText(String.valueOf(total)); // Affiche total
        if (lblFrontAttente != null) lblFrontAttente.setText(String.valueOf(attente)); // Affiche attente
        if (lblFrontTraite != null) lblFrontTraite.setText(String.valueOf(traite)); // Affiche traite
    }

    @FXML // Handler clic sur une ligne de table
    void handleTableClick(MouseEvent event) { // Charge la reclamation selectionnee dans le formulaire
        Reclamation selected = tableReclamation.getSelectionModel().getSelectedItem(); // Lit la ligne selectionnee
        if (selected != null) { // Verifie qu'une ligne est bien selectionnee
            selectedId = selected.getId(); // Memorise l'id selectionne
            txtTitre.setText(selected.getTitre()); // Remplit titre
            txtPatient.setText(selected.getNomPatient()); // Remplit nom patient
            txtEmail.setText(selected.getEmail()); // Remplit email
            cmbCategorie.setValue(selected.getCategorie()); // Remplit categorie
            cmbPriorite.setValue(selected.getPriorite()); // Remplit priorite
            txtDescription.setText(selected.getDescription()); // Remplit description

            if (txtReponse != null) { // Verifie que la zone reponse existe dans cette vue
                Reponse rep = reponseDao.getReponseByReclamationId(selectedId); // Charge la reponse liee
                if (rep != null) { // Si une reponse existe
                    txtReponse.setText(rep.getContenu() + "\n- Par l'admin " + rep.getAdminNom()); // Affiche contenu + auteur
                } else { // Si aucune reponse
                    txtReponse.setText(""); // Vide la zone
                }
            }

            activerModeEdition(); // Bascule l'UI en mode edition
        }
    }

    @FXML // Handler bouton vider
    void viderFormulaire(ActionEvent event) { // Nettoie formulaire et revient mode ajout
        clearFields(); // Efface toutes les saisies
        activerModeAjout(); // Affiche bouton ajouter
    }

    @FXML // Handler bouton soumettre
    void soumettreReclamation(ActionEvent event) { // Cree puis enregistre une nouvelle reclamation
        Reclamation r = new Reclamation(); // Instancie un nouvel objet reclamation
        r.setTitre(txtTitre.getText()); // Affecte titre depuis champ
        r.setNomPatient(txtPatient.getText()); // Affecte nom patient
        r.setEmail(txtEmail.getText()); // Affecte email
        r.setStatut("En attente"); // Force statut initial
        r.setCategorie(cmbCategorie.getValue()); // Affecte categorie
        r.setPriorite(cmbPriorite.getValue()); // Affecte priorite
        r.setDescription(txtDescription.getText()); // Affecte description
        r.setEtatMental(lastPredictedMentalState); // Sauvegarde l'etat mental predit par Gemini
        r.setDateCreation(LocalDateTime.now()); // Affecte date de creation actuelle

        String error = validateReclamationInputs(); // Valide les champs saisis
        if (error != null) { // Si une erreur existe
            showAlert("Erreur de saisie", error, Alert.AlertType.ERROR); // Affiche message erreur
            return; // Stoppe le traitement
        }

        checkProfanityApi(r, false); // Verification via API avant sauvegarde
    }

    @FXML // Handler bouton modifier
    void modifierReclamation(ActionEvent event) { // Met a jour la reclamation selectionnee
        if (selectedId == -1) { // Si aucune selection
            showAlert("Attention", "Veuillez sélectionner une réclamation à modifier.", Alert.AlertType.WARNING); // Alerte utilisateur
            return; // Stoppe
        }

        Reclamation r = tableReclamation.getSelectionModel().getSelectedItem(); // Recupere l'objet selectionne
        String statut = normalizeStatus(r.getStatut()); // Normalise son statut
        // Le client ne peut plus modifier une reclamation deja traitee.
        if (statut.contains("traite") || statut.contains("resolu") || statut.contains("ferme")) { // Verifie statut bloque
            showAlert( // Affiche une alerte de blocage
                    "Modification bloquée", // Titre alerte
                    "Cette réclamation est déjà traitée (statut: " + r.getStatut() + ") et ne peut plus être modifiée.", // Message detaille
                    Alert.AlertType.WARNING // Type warning
            );
            return; // Stoppe la modification
        }

        String error = validateReclamationInputs(); // Revalide les champs
        if (error != null) { // Si invalide
            showAlert("Erreur de saisie", error, Alert.AlertType.ERROR); // Affiche erreur
            return; // Stoppe
        }

        r.setTitre(txtTitre.getText().trim()); // Met a jour titre
        r.setNomPatient(txtPatient.getText().trim()); // Met a jour nom
        r.setEmail(txtEmail.getText().trim()); // Met a jour email
        r.setCategorie(cmbCategorie.getValue()); // Met a jour categorie
        r.setPriorite(cmbPriorite.getValue()); // Met a jour priorite
        r.setDescription(txtDescription.getText().trim()); // Met a jour description

        checkProfanityApi(r, true); // Verification via API avant modification
    }

    private void checkProfanityApi(Reclamation r, boolean isUpdate) {
        // Validation locale et appel à l'API PurgoMalum pour vérifier le texte
        String textToCheck = r.getTitre() + " " + r.getDescription();
        String urlStr = "https://www.purgomalum.com/service/containsprofanity?text=" + java.net.URLEncoder.encode(textToCheck, java.nio.charset.StandardCharsets.UTF_8);

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(urlStr))
                .GET()
                .build();

        client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> javafx.application.Platform.runLater(() -> {
                    String body = response.body();
                    String textLow = textToCheck.toLowerCase();
                    // L'API filtre surtout l'anglais, donc on ajoute un filtre local pour le francais
                    boolean hasFrenchBadWords = textLow.contains("putain") || textLow.contains("merde")
                            || textLow.contains("connard") || textLow.contains("enculé")
                            || textLow.contains("salope") || textLow.contains("fuck");

                    if ("true".equals(body) || hasFrenchBadWords) {
                        showAlert("Langage inapproprié", "Votre texte contient des mots offensants et ne peut pas être soumis.", Alert.AlertType.ERROR);
                    } else {
                        if (isUpdate) {
                            dao.updateReclamation(r);
                            showAlert("Modifiée", "Réclamation modifiée avec succès.", Alert.AlertType.INFORMATION);
                        } else {
                            dao.addReclamation(r);
                            showAlert("Succès", "Votre réclamation a été soumise avec succès.", Alert.AlertType.INFORMATION);
                        }
                        loadTable();
                        viderFormulaire(null);
                    }
                }))
                .exceptionally(e -> {
                    javafx.application.Platform.runLater(() -> showAlert("Erreur de connexion", "Impossible d'analyser le texte de la réclamation. Veuillez vérifier votre connexion.", Alert.AlertType.ERROR));
                    return null;
                });
    }

    @FXML // Handler bouton supprimer
    void supprimerReclamation(ActionEvent event) { // Supprime la reclamation selectionnee
        if (selectedId == -1) { // Si rien selectionne
            showAlert("Attention", "Veuillez sélectionner une réclamation à supprimer.", Alert.AlertType.WARNING); // Avertit utilisateur
            return; // Stoppe
        }

        dao.deleteReclamation(selectedId); // Supprime en base via id
        showAlert("Suppression", "La réclamation a été retirée.", Alert.AlertType.INFORMATION); // Confirme suppression
        loadTable(); // Recharge tableau
        viderFormulaire(null); // Nettoie formulaire
    }

    @FXML // Handler bouton actualiser
    void actualiser(ActionEvent event) { // Recharge juste les donnees
        loadTable(); // Recharge tableau et KPI
    }

    private void clearFields() { // Efface tous les champs du formulaire
        txtTitre.clear(); // Vide titre
        txtPatient.clear(); // Vide nom patient
        txtEmail.clear(); // Vide email
        cmbCategorie.setValue(null); // Reinitialise categorie
        cmbPriorite.setValue(null); // Reinitialise priorite
        txtDescription.clear(); // Vide description
        if (txtReponse != null) txtReponse.clear(); // Vide reponse si presente
        selectedId = -1; // Reinitialise la selection
        lastPredictedMentalState = "Non défini";
    }

    private void activerModeAjout() { // Configure boutons pour mode ajout
        btnAjouter.setVisible(true); // Affiche ajouter
        btnAjouter.setManaged(true); // Garde sa place dans layout
        btnModifier.setVisible(false); // Cache modifier
        btnModifier.setManaged(false); // Retire sa place dans layout
    }

    private void activerModeEdition() { // Configure boutons pour mode edition
        btnAjouter.setVisible(false); // Cache ajouter
        btnAjouter.setManaged(false); // Retire sa place dans layout
        btnModifier.setVisible(true); // Affiche modifier
        btnModifier.setManaged(true); // Garde sa place dans layout
    }

    private String validateReclamationInputs() { // Valide les donnees saisies et retourne un message d'erreur ou null
        String titre = txtTitre.getText() != null ? txtTitre.getText().trim() : ""; // Lit/normalise titre
        String nom = txtPatient.getText() != null ? txtPatient.getText().trim() : ""; // Lit/normalise nom
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : ""; // Lit/normalise email
        String description = txtDescription.getText() != null ? txtDescription.getText().trim() : ""; // Lit/normalise description

        if (titre.length() < 5) { // Regle min longueur titre
            return "Le titre doit contenir au moins 5 caracteres."; // Message erreur titre
        }
        if (!nom.matches("^[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\s'-]{2,49}$")) { // Regle nom alphabetique min 3
            return "Le nom est invalide (minimum 3 lettres, sans chiffres)."; // Message erreur nom
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) { // Regle format email
            return "L'adresse email est invalide."; // Message erreur email
        }
        if (cmbCategorie.getValue() == null || cmbCategorie.getValue().trim().isEmpty()) { // Verifie categorie
            return "Veuillez choisir une categorie."; // Message erreur categorie
        }
        if (cmbPriorite.getValue() == null || cmbPriorite.getValue().trim().isEmpty()) { // Verifie priorite
            return "Veuillez choisir une priorite."; // Message erreur priorite
        }
        if (description.length() < 10) { // Regle min longueur description
            return "La description doit contenir au moins 10 caracteres."; // Message erreur description
        }
        return null; // Aucune erreur
    }

    private String normalizeStatus(String value) { // Normalise statut pour comparaisons robustes
        if (value == null) return ""; // Protege contre null
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD); // Separe lettres et accents
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(); // Supprime accents et passe en minuscule
    }

    private void showAlert(String title, String content, Alert.AlertType type) { // Affiche une boite de dialogue
        Alert alert = new Alert(type); // Cree alerte du type demande
        alert.setTitle(title); // Definit titre fenetre
        alert.setHeaderText(null); // Supprime en-tete
        alert.setContentText(content); // Definit message
        alert.showAndWait(); // Affiche et attend fermeture
    }

    private void setupAnimations() { // Installe les animations d'entree/hover
        FadeTransition ftTable = new FadeTransition(Duration.millis(380), tableReclamation); // Animation fondu table
        ftTable.setFromValue(0.0); // Debut transparent
        ftTable.setToValue(1.0); // Fin opaque
        ftTable.play(); // Lance animation table

        FadeTransition ftForm = new FadeTransition(Duration.millis(520), txtTitre); // Animation fondu sur zone formulaire
        ftForm.setFromValue(0.0); // Debut transparent
        ftForm.setToValue(1.0); // Fin opaque
        ftForm.play(); // Lance animation formulaire

        addHoverScale(btnAjouter); // Active hover sur bouton ajouter
        addHoverScale(btnModifier); // Active hover sur bouton modifier
    }

    private void addHoverScale(Button button) { // Ajoute effet de zoom au survol
        if (button == null) return; // Sort si bouton absent
        ScaleTransition onEnter = new ScaleTransition(Duration.millis(120), button); // Animation a l'entree souris
        onEnter.setToX(1.03); // Agrandit en X
        onEnter.setToY(1.03); // Agrandit en Y

        ScaleTransition onExit = new ScaleTransition(Duration.millis(120), button); // Animation a la sortie souris
        onExit.setToX(1.0); // Revient taille normale X
        onExit.setToY(1.0); // Revient taille normale Y

        button.setOnMouseEntered(e -> onEnter.playFromStart()); // Lance zoom a l'entree
        button.setOnMouseExited(e -> onExit.playFromStart()); // Lance retour a la sortie
    }

    // =============================================
    // SECTION CHATBOT GEMINI (Appel API Reel)
    // =============================================

    private void callGeminiAPI(String prompt, java.util.function.Consumer<String> callback) {
        String apiKey = "AIzaSyBeockjPx07_gt35XIaSQG1GLx5Riz22a0";
        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        String escapedPrompt = escapeJson(prompt);
        String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]}";

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(urlStr))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body, java.nio.charset.StandardCharsets.UTF_8))
                .build();

        client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> javafx.application.Platform.runLater(() -> {
                    String responseBody = response.body();
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        callback.accept(parseGeminiErrorResponse(response.statusCode(), responseBody));
                        return;
                    }

                    String reply = parseGeminiResponse(responseBody);
                    if (reply.trim().isEmpty()) {
                        reply = "Réponse Gemini vide ou non reconnue.";
                    }
                    callback.accept(reply);
                }))
                .exceptionally(e -> {
                    javafx.application.Platform.runLater(() -> callback.accept("Erreur réseau: " + e.getMessage()));
                    return null;
                });
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String parseGeminiResponse(String json) {
        if (json == null || json.isBlank()) return "";

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", java.util.regex.Pattern.DOTALL)
                .matcher(json);

        if (matcher.find()) {
            return unescapeJsonString(matcher.group(1));
        }

        return "";
    }

    private String parseGeminiErrorResponse(int statusCode, String json) {
        if (json != null && !json.isBlank()) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", java.util.regex.Pattern.DOTALL)
                    .matcher(json);
            if (matcher.find()) {
                return "Erreur Gemini HTTP " + statusCode + ": " + unescapeJsonString(matcher.group(1));
            }
        }
        return "Erreur Gemini HTTP " + statusCode + ": " + (json == null ? "réponse vide" : json);
    }

    private String unescapeJsonString(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @FXML
    void sendChatMessage() {
        String msg = txtChatInput.getText();
        if (msg == null || msg.trim().isEmpty()) return;

        lastUserMessage = msg;
        txtChatbot.appendText("Vous : " + msg + "\n");
        txtChatInput.clear();
        btnChatSendMessage.setDisable(true);
        txtChatbot.appendText("Bot AI : ... (réflexion en cours)\n");

        String prompt = "Tu es un assistant IA chaleureux d'une clinique. Ton rôle est d'aider le patient à formuler son besoin de réclamation de manière concise, et de le rassurer. Ne donne pas la réclamation formatée. Dis-lui juste que tu peux l'aider à remplir le formulaire avec le bouton de Remplissage Rapide. Voici le chat: " + txtChatbot.getText();

        callGeminiAPI(prompt, reply -> {
            txtChatbot.setText(txtChatbot.getText().replace("Bot AI : ... (réflexion en cours)\n", ""));
            txtChatbot.appendText("Bot Gemini : " + reply + "\n\n");
            btnChatSendMessage.setDisable(false);
        });
    }

    @FXML
    void autoFillForm() {
        btnAutoFill.setDisable(true);
        txtChatbot.appendText("Bot Gemini : (Analyse de notre discussion pour remplir les champs...)\n");

        String conversationSnapshot = txtChatbot.getText();
        String prompt = "Analyse cette conversation: \n" + conversationSnapshot + "\n" +
                "Réponds avec EXACTEMENT ces 5 lignes, sans markdown, sans explication, et avec les libellés en début de ligne :\n" +
                "TITRE: <titre court>\n" +
                "CATEGORIE: <doit être Service, Propreté, Personnel ou Autre>\n" +
                "PRIORITE: <doit être Basse, Moyenne ou Haute>\n" +
                "DESCRIPTION: <une phrase de description claire du souci>\n" +
                "ETAT_MENTAL: <Calme, Frustré, Paniqué, En Colère, Inquiet>";

        callGeminiAPI(prompt, reply -> {
            txtChatbot.setText(txtChatbot.getText().replace("Bot Gemini : (Analyse de notre discussion pour remplir les champs...)\n", ""));

            boolean filled = reply != null && !reply.startsWith("Erreur Gemini HTTP") && applyAutoFillFromGemini(reply);
            if (!filled) {
                filled = applyLocalAutoFillFallback(lastUserMessage + "\n" + conversationSnapshot + "\n" + reply);
            }

            if (filled) {
                txtChatbot.appendText("Bot Gemini : ✅ Voilà ! J'ai pré-rempli vos champs. Vous pouvez les vérifier et Sauvegarder.\n\n");
            } else {
                txtChatbot.appendText("Bot Gemini : ⚠️ Je n'ai pas pu extraire les champs automatiquement. Essayez avec plus de détails.\n\n");
            }

            btnAutoFill.setDisable(false);
        });
    }

    private boolean applyAutoFillFromGemini(String reply) {
        if (reply == null || reply.trim().isEmpty()) {
            return false;
        }

        String titre = extractField(reply, "TITRE");
        String categorie = extractField(reply, "CATEGORIE");
        String priorite = extractField(reply, "PRIORITE");
        String description = extractField(reply, "DESCRIPTION");
        String etatMental = extractField(reply, "ETAT_MENTAL");
        if (etatMental.isEmpty()) {
            etatMental = extractField(reply, "ETAT MENTAL");
        }

        boolean hasAny = false;

        if (!titre.isEmpty()) {
            txtTitre.setText(titre);
            hasAny = true;
        }

        if (!categorie.isEmpty()) {
            cmbCategorie.setValue(normalizeCategory(categorie));
            hasAny = true;
        }

        if (!priorite.isEmpty()) {
            cmbPriorite.setValue(normalizePriority(priorite));
            hasAny = true;
        }

        if (!description.isEmpty()) {
            txtDescription.setText(description);
            hasAny = true;
        }

        if (!etatMental.isEmpty()) {
            lastPredictedMentalState = etatMental;
            hasAny = true;
        }

        return hasAny;
    }

    private boolean applyLocalAutoFillFallback(String text) {
        String source = text == null ? "" : text.toLowerCase();
        if (source.isBlank()) return false;

        if ((source.contains("rendez") || source.contains("rdv") || source.contains("rendez-vous")) &&
                (source.contains("urgent") || source.contains("critique") || source.contains("sang") || source.contains("douleur") || source.contains("mortel") || source.contains("mortelle") || source.contains("personne n'est disponible") || source.contains("personne n est disponible") || source.contains("impossible") || source.contains("n'arrive pas") || source.contains("n arrive pas"))) {
            txtTitre.setText("Demande de rendez-vous urgent");
            cmbCategorie.setValue("Service");
            cmbPriorite.setValue("Haute");
            txtDescription.setText("Le patient signale une situation critique nécessitant un rendez-vous urgent.");
            lastPredictedMentalState = "Paniqué / Stressé";
            return true;
        }

        if (source.contains("retard") || source.contains("attente") || source.contains("patienter") || source.contains("pas de rendez") || source.contains("pas de rdv")) {
            txtTitre.setText("Retard de prise en charge");
            cmbCategorie.setValue("Service");
            cmbPriorite.setValue("Moyenne");
            txtDescription.setText("Le patient signale un retard de prise en charge ou d'obtention de rendez-vous.");
            lastPredictedMentalState = "Frustré";
            return true;
        }

        if (source.contains("propret") || source.contains("sale") || source.contains("nettoyage")) {
            txtTitre.setText("Problème de propreté");
            cmbCategorie.setValue("Propreté");
            cmbPriorite.setValue("Basse");
            txtDescription.setText("Le patient signale un problème de propreté ou d'hygiène.");
            lastPredictedMentalState = "Mécontent";
            return true;
        }

        txtTitre.setText("Demande d'information");
        cmbCategorie.setValue("Autre");
        cmbPriorite.setValue("Basse");
        txtDescription.setText("Demande générale de renseignements concernant le service.");
        lastPredictedMentalState = "Calme";
        return true;
    }

    private String extractField(String text, String label) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?im)^\\s*[-*•]?\\s*" + java.util.regex.Pattern.quote(label) + "\\s*:\\s*(.+?)\\s*$")
                .matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String normalizeCategory(String value) {
        String v = value == null ? "" : value.toLowerCase();
        if (v.contains("service")) return "Service";
        if (v.contains("propret")) return "Propreté";
        if (v.contains("personnel")) return "Personnel";
        return "Autre";
    }

    private String normalizePriority(String value) {
        String v = value == null ? "" : value.toLowerCase();
        if (v.contains("haute") || v.contains("urgent")) return "Haute";
        if (v.contains("moyenne") || v.contains("normal")) return "Moyenne";
        return "Basse";
    }

} // Fin du controller Front Office

