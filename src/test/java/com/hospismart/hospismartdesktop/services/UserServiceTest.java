package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.User;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Test CRUD UserService")
public class UserServiceTest {

    private UserService userService;
    private static final String EMAIL_PREFIX = "test.user.";
    private static final String EMAIL_DOMAIN = "example.com";
    private static final String INITIAL_PASSWORD = "TestPassword123";
    private static final String UPDATED_PASSWORD = "UpdatedPassword456";
    private int insertedUserId;
    private String insertedUserEmail;

    @BeforeEach
    public void setup() throws Exception {
        System.out.println("\n📦 Initialisation du test...");
        try {
            userService = new UserService();
            insertedUserEmail = EMAIL_PREFIX + System.currentTimeMillis() + "@" + EMAIL_DOMAIN;
            System.out.println("✅ UserService initialisé");
            System.out.println("   Email de test: " + insertedUserEmail);
        } catch (Exception e) {
            System.err.println("❌ ERREUR lors de l'initialisation:");
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @Order(1)
    @DisplayName("Créer un utilisateur (Create)")
    public void testAjouterUtilisateur() throws Exception {
        System.out.println("\n=== TEST 1: CRÉER UN UTILISATEUR (CREATE) ===");
        
        User user = new User();
        user.setNom("TestNom");
        user.setPrenom("TestPrenom");
        user.setEmail(insertedUserEmail);
        user.setTelephone("0600000000");
        user.setPassword(INITIAL_PASSWORD);
        user.setType("ROLE_PATIENT");

        System.out.println("📝 Tentative d'ajout d'utilisateur...");
        System.out.println("   Email: " + insertedUserEmail);
        System.out.println("   Nom: " + user.getNom());
        System.out.println("   Prénom: " + user.getPrenom());
        
        boolean created = userService.ajouter(user);
        assertTrue(created, "L'utilisateur doit être créé avec succès");
        System.out.println("✅ Utilisateur créé avec succès!");

        User saved = findByEmail(insertedUserEmail);
        assertNotNull(saved, "L'utilisateur doit exister après création");
        assertEquals("TestNom", saved.getNom());
        assertEquals("TestPrenom", saved.getPrenom());
        assertEquals(insertedUserEmail, saved.getEmail());
        assertEquals("ROLE_PATIENT", saved.getType());
        assertTrue(saved.isActive(), "L'utilisateur doit être actif par défaut");

        insertedUserId = saved.getId();
        assertTrue(insertedUserId > 0, "L'identifiant utilisateur doit être positif");
        System.out.println("   ID généré: " + insertedUserId);
        System.out.println("   Statut: " + (saved.isActive() ? "ACTIF ✓" : "INACTIF ✗"));
    }

    @Test
    @Order(2)
    @DisplayName("Lire les utilisateurs (Read)")
    public void testAfficherUtilisateurs() {
        System.out.println("\n=== TEST 2: LIRE LES UTILISATEURS (READ) ===");
        System.out.println("📖 Lecture de la liste des utilisateurs...");
        
        List<User> users = userService.afficher();
        assertNotNull(users, "La liste d'utilisateurs ne doit pas être nulle");
        assertFalse(users.isEmpty(), "La liste d'utilisateurs ne doit pas être vide");

        System.out.println("✅ List des utilisateurs chargée!");
        System.out.println("   Total d'utilisateurs: " + users.size());

        User found = findByEmail(insertedUserEmail);
        assertNotNull(found, "L'utilisateur inséré doit apparaître dans la liste");
        assertEquals(insertedUserId, found.getId());
        System.out.println("✅ Utilisateur de test trouvé dans la liste!");
        System.out.println("   Email: " + found.getEmail());
        System.out.println("   Nom: " + found.getNom() + " " + found.getPrenom());
    }

    @Test
    @Order(3)
    @DisplayName("Mettre à jour un utilisateur (Update)")
    public void testModifierUtilisateur() {
        System.out.println("\n=== TEST 3: MODIFIER UN UTILISATEUR (UPDATE) ===");
        System.out.println("✏️  Tentative de modification de l'utilisateur ID: " + insertedUserId);
        
        User user = new User();
        user.setId(insertedUserId);
        user.setNom("TestNomModifie");
        user.setPrenom("TestPrenomModifie");
        user.setEmail(insertedUserEmail);
        user.setTelephone("0611111111");
        user.setPassword(UPDATED_PASSWORD);
        user.setType("ROLE_PATIENT");

        System.out.println("   Ancien: TestNom → Nouveau: TestNomModifie");
        System.out.println("   Ancien: TestPrenom → Nouveau: TestPrenomModifie");
        System.out.println("   Ancien: 0600000000 → Nouveau: 0611111111");

        boolean updated = userService.modifier(user);
        assertTrue(updated, "La modification doit réussir");
        System.out.println("✅ Utilisateur modifié avec succès!");

        User saved = findByEmail(insertedUserEmail);
        assertNotNull(saved, "L'utilisateur doit toujours exister après modification");
        assertEquals("TestNomModifie", saved.getNom());
        assertEquals("TestPrenomModifie", saved.getPrenom());
        assertEquals("0611111111", saved.getTelephone());
        System.out.println("✅ Modifications confirmées en base de données!");
    }

    @Test
    @Order(4)
    @DisplayName("Connexion avec mot de passe modifié")
    public void testLoginUtilisateur() {
        System.out.println("\n=== TEST 4: CONNEXION UTILISATEUR (LOGIN) ===");
        System.out.println("🔐 Tentative de connexion...");
        System.out.println("   Email: " + insertedUserEmail);
        System.out.println("   Mot de passe: [protégé]");
        
        User loginResult = userService.login(insertedUserEmail, UPDATED_PASSWORD);
        assertNotNull(loginResult, "La connexion doit réussir avec le nouveau mot de passe");
        assertEquals(insertedUserId, loginResult.getId());
        assertEquals(insertedUserEmail, loginResult.getEmail());
        System.out.println("✅ Connexion réussie!");
        System.out.println("   ID: " + loginResult.getId());
        System.out.println("   Nom: " + loginResult.getNom() + " " + loginResult.getPrenom());
    }

    @Test
    @Order(5)
    @DisplayName("Désactiver puis activer un compte")
    public void testActiverDesactiverCompte() {
        System.out.println("\n=== TEST 5: ACTIVATION/DÉSACTIVATION DU COMPTE ===");
        System.out.println("🔴 Tentative de désactivation du compte ID: " + insertedUserId);
        
        userService.desactiverCompte(insertedUserId);
        User deactivated = findByEmail(insertedUserEmail);
        assertNotNull(deactivated);
        assertFalse(deactivated.isActive(), "Le compte doit être désactivé");
        System.out.println("✅ Compte désactivé!");
        System.out.println("   Statut: DÉSACTIVÉ ✗");

        System.out.println("\n🟢 Tentative de réactivation du compte ID: " + insertedUserId);
        userService.activerCompte(insertedUserId);
        User activated = findByEmail(insertedUserEmail);
        assertNotNull(activated);
        assertTrue(activated.isActive(), "Le compte doit être réactivé");
        System.out.println("✅ Compte réactivé!");
        System.out.println("   Statut: ACTIF ✓");
    }

    @Test
    @Order(6)
    @DisplayName("Supprimer un utilisateur (Delete)")
    public void testSupprimerUtilisateur() {
        System.out.println("\n=== TEST 6: SUPPRIMER UN UTILISATEUR (DELETE) ===");
        System.out.println("🗑️  Tentative de suppression de l'utilisateur ID: " + insertedUserId);
        System.out.println("   Email: " + insertedUserEmail);
        
        boolean deleted = userService.supprimer(insertedUserId);
        assertTrue(deleted, "La suppression doit réussir");
        System.out.println("✅ Utilisateur supprimé avec succès!");

        User saved = findByEmail(insertedUserEmail);
        assertNull(saved, "L'utilisateur ne doit plus exister après suppression");
        System.out.println("✅ Vérification: Utilisateur introuvable en base ✓");

        User loginResult = userService.login(insertedUserEmail, UPDATED_PASSWORD);
        assertNull(loginResult, "La connexion doit échouer après suppression");
        System.out.println("✅ Vérification: Connexion impossible ✓");
    }

    private User findByEmail(String email) {
        return userService.afficher().stream()
                .filter(user -> email.equals(user.getEmail()))
                .findFirst()
                .orElse(null);
    }
}
