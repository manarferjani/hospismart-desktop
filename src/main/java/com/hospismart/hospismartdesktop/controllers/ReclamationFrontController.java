package com.hospismart.hospismartdesktop.controllers; // Package des controllers JavaFX

import com.hospismart.hospismartdesktop.dao.ReclamationDao; // DAO des reclamations
import com.hospismart.hospismartdesktop.dao.ReponseDao; // DAO des reponses
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
    @FXML // Injecte zone de reponse admin
    private TextArea txtReponse; // Affichage reponse admin
    @FXML // Injecte bouton ajouter
    private Button btnAjouter; // Bouton soumettre
    @FXML // Injecte bouton modifier
    private Button btnModifier; // Bouton modifier
    @FXML // Injecte label total
    private Label lblTotalFront; // KPI total
    @FXML // Injecte label attente
    private Label lblFrontAttente; // KPI en attente
    @FXML // Injecte label traite
    private Label lblFrontTraite; // KPI traite

    private ReclamationDao dao = new ReclamationDao(); // Acces BD reclamations
    private ReponseDao reponseDao = new ReponseDao(); // Acces BD reponses
    private ObservableList<Reclamation> reclamationList = FXCollections.observableArrayList(); // Source de donnees de la table
    private int selectedId = -1; // ID selectionne, -1 = rien selectionne

    @Override // Methode appelee automatiquement apres chargement FXML
    public void initialize(URL url, ResourceBundle resourceBundle) { // Initialisation de l'ecran
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
        if (lblTotalFront == null) return; // Garde-fou si labels non injectes
        int total = reclamationList.size(); // Total reclamations chargees
        int attente = 0; // Compteur attente
        int traite = 0; // Compteur traite/resolu/ferme
        for (Reclamation r : reclamationList) { // Parcourt toutes les reclamations
            String s = normalizeStatus(r.getStatut()); // Normalise le statut
            if (s.contains("attente")) attente++; // Incremente attente
            if (s.contains("traite") || s.contains("resolu") || s.contains("ferme")) traite++; // Incremente traite
        }
        lblTotalFront.setText(String.valueOf(total)); // Affiche total
        lblFrontAttente.setText(String.valueOf(attente)); // Affiche attente
        lblFrontTraite.setText(String.valueOf(traite)); // Affiche traite
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
        r.setDateCreation(LocalDateTime.now()); // Affecte date de creation actuelle

        String error = validateReclamationInputs(); // Valide les champs saisis
        if (error != null) { // Si une erreur existe
            showAlert("Erreur de saisie", error, Alert.AlertType.ERROR); // Affiche message erreur
            return; // Stoppe le traitement
        }

        dao.addReclamation(r); // Insere en base
        showAlert("Succès", "Votre réclamation a été soumise avec succès.", Alert.AlertType.INFORMATION); // Message succes
        loadTable(); // Recharge table
        viderFormulaire(null); // Reinitialise formulaire
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

        dao.updateReclamation(r); // Persiste la mise a jour en base
        showAlert("Modifiée", "Réclamation modifiée avec succès.", Alert.AlertType.INFORMATION); // Alerte succes
        loadTable(); // Recharge tableau
        viderFormulaire(null); // Reset formulaire
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
}
