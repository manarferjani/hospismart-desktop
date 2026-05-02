package com.hospismart.hospismartdesktop.services;

import com.hospismart.hospismartdesktop.models.User;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceCRUDTest {

    private UserService userService;
    private static final String EMAIL_PREFIX = "test.user.";
    private static final String EMAIL_DOMAIN = "@example.com";
    private static final String INITIAL_PASSWORD = "TestPassword123";
    private static final String UPDATED_PASSWORD = "UpdatedPassword456";
    private int insertedUserId = -1;
    private String insertedUserEmail;

    @BeforeEach
    public void setup() {
        System.out.println("\n========================================");
        System.out.println("📦 SETUP: Initialisation du service...");
        userService = new UserService();
        long timestamp = System.currentTimeMillis();
        insertedUserEmail = EMAIL_PREFIX + timestamp + EMAIL_DOMAIN;
        System.out.println("✅ SETUP: UserService est prêt!");
        System.out.println("   Email de test: " + insertedUserEmail);
        System.out.println("========================================\n");
    }

    private String getUniqueUsername(String prefix) {
        return prefix + "_" + System.nanoTime();
    }

    @Test
    @DisplayName("TEST 1: Ajouter un utilisateur")
    public void test01Ajouter() throws Exception {
        assertNotNull(userService, "UserService ne doit pas être null!");
        
        System.out.println("\n[TEST 1: CREATE] 📝 Création d'utilisateur");
        System.out.println("  Email: " + insertedUserEmail);
        
        User user = new User();
        user.setNom(getUniqueUsername("TestNom"));
        user.setPrenom("TestPrenom");
        user.setEmail(insertedUserEmail);
        user.setTelephone("0600000000");
        user.setPassword(INITIAL_PASSWORD);
        user.setType("ROLE_PATIENT");

        boolean created = userService.ajouter(user);
        assertTrue(created, "L'utilisateur doit être créé");
        System.out.println("  ✅ Utilisateur créé avec succès!");

        User saved = findByEmail(insertedUserEmail);
        assertNotNull(saved, "L'utilisateur doit exister");
        insertedUserId = saved.getId();
        System.out.println("  ✅ Vérification confirmée - ID: " + insertedUserId);
        System.out.println("  ✅ Statut: " + (saved.isActive() ? "ACTIF" : "INACTIF"));
    }

    @Test
    @DisplayName("TEST 2: Lire tous les utilisateurs")
    public void test02Afficher() {
        assertNotNull(userService, "UserService ne doit pas être null!");
        
        System.out.println("\n[TEST 2: READ] 📖 Lecture de la liste");
        
        List<User> users = userService.afficher();
        assertNotNull(users, "La liste ne doit pas être null");
        assertFalse(users.isEmpty(), "La liste ne doit pas être vide");
        
        System.out.println("  ✅ Liste chargée - Total: " + users.size() + " utilisateurs");
    }

    @Test
    @DisplayName("TEST 3: Modifier un utilisateur")
    @Order(3)
    public void test03Modifier() throws Exception {
        assertNotNull(userService, "UserService ne doit pas être null!");
        
        System.out.println("\n[TEST 3: UPDATE] ✏️ Modification d'utilisateur");
        
        // D'abord créer l'utilisateur
        User newUser = new User();
        newUser.setNom(getUniqueUsername("OriginalNom"));
        newUser.setPrenom("OriginalPrenom");
        newUser.setEmail(insertedUserEmail);
        newUser.setTelephone("0600000000");
        newUser.setPassword(INITIAL_PASSWORD);
        newUser.setType("ROLE_PATIENT");
        
        boolean created = userService.ajouter(newUser);
        assertTrue(created);
        User created_user = findByEmail(insertedUserEmail);
        assertNotNull(created_user);
        insertedUserId = created_user.getId();
        System.out.println("  📝 Utilisateur créé - ID: " + insertedUserId);
        
        // Maintenant le modifier
        User modUser = new User();
        modUser.setId(insertedUserId);
        modUser.setNom(getUniqueUsername("NomModifie"));
        modUser.setPrenom("PrenomModifie");
        modUser.setEmail(insertedUserEmail);
        modUser.setTelephone("0611111111");
        modUser.setPassword(UPDATED_PASSWORD);
        modUser.setType("ROLE_PATIENT");
        
        boolean updated = userService.modifier(modUser);
        assertTrue(updated, "La modification doit réussir");
        System.out.println("  ✅ Utilisateur modifié!");
        System.out.println("     ✓ Nom: NomModifie");
        System.out.println("     ✓ Prénom: PrenomModifie");
        System.out.println("     ✓ Téléphone: 0611111111");
        
        User modified = findByEmail(insertedUserEmail);
        assertEquals("PrenomModifie", modified.getPrenom());
    }

    @Test
    @DisplayName("TEST 4: Login utilisateur")
    @Order(4)
    public void test04Login() throws Exception {
        assertNotNull(userService, "UserService ne doit pas être null!");
        
        System.out.println("\n[TEST 4: LOGIN] 🔐 Authentification");
        
        // D'abord créer
        User newUser = new User();
        newUser.setNom(getUniqueUsername("LoginTest"));
        newUser.setPrenom("User");
        newUser.setEmail(insertedUserEmail);
        newUser.setPassword(INITIAL_PASSWORD);
        newUser.setType("ROLE_PATIENT");
        userService.ajouter(newUser);
        
        System.out.println("  Email: " + insertedUserEmail);
        System.out.println("  Mot de passe: [protégé]");
        
        User loginResult = userService.login(insertedUserEmail, INITIAL_PASSWORD);
        assertNotNull(loginResult, "La connexion doit réussir");
        System.out.println("  ✅ Connexion réussie!");
        System.out.println("     ✓ ID: " + loginResult.getId());
        System.out.println("     ✓ Nom: " + loginResult.getNom());
    }

    @Test
    @DisplayName("TEST 5: Activer/Désactiver compte")
    @Order(5)
    public void test05ActivationDeactivation() throws Exception {
        assertNotNull(userService, "UserService ne doit pas être null!");
        
        System.out.println("\n[TEST 5: ACTIVATION] 🟢🔴 Gestion du statut");
        
        // Créer d'abord
        User newUser = new User();
        newUser.setNom(getUniqueUsername("StatusTest"));
        newUser.setPrenom("User");
        newUser.setEmail(insertedUserEmail);
        newUser.setPassword(INITIAL_PASSWORD);
        newUser.setType("ROLE_PATIENT");
        userService.ajouter(newUser);
        User created = findByEmail(insertedUserEmail);
        insertedUserId = created.getId();
        
        // Désactiver
        System.out.println("  🔴 Tentative de désactivation...");
        userService.desactiverCompte(insertedUserId);
        User deactivated = findByEmail(insertedUserEmail);
        assertFalse(deactivated.isActive(), "Le compte doit être désactivé");
        System.out.println("  ✅ Compte désactivé!");
        
        // Réactiver
        System.out.println("  🟢 Tentative de réactivation...");
        userService.activerCompte(insertedUserId);
        User activated = findByEmail(insertedUserEmail);
        assertTrue(activated.isActive(), "Le compte doit être réactivé");
        System.out.println("  ✅ Compte réactivé!");
    }

    @Test
    @DisplayName("TEST 6: Supprimer un utilisateur")
    @Order(6)
    public void test06Supprimer() throws Exception {
        assertNotNull(userService, "UserService ne doit pas être null!");
        
        System.out.println("\n[TEST 6: DELETE] 🗑️ Suppression d'utilisateur");
        
        // Créer d'abord
        User newUser = new User();
        newUser.setNom(getUniqueUsername("DeleteTest"));
        newUser.setPrenom("User");
        newUser.setEmail(insertedUserEmail);
        newUser.setPassword(INITIAL_PASSWORD);
        newUser.setType("ROLE_PATIENT");
        userService.ajouter(newUser);
        User created = findByEmail(insertedUserEmail);
        insertedUserId = created.getId();
        
        System.out.println("  Email: " + insertedUserEmail);
        System.out.println("  ID: " + insertedUserId);
        
        boolean deleted = userService.supprimer(insertedUserId);
        assertTrue(deleted, "La suppression doit réussir");
        System.out.println("  ✅ Utilisateur supprimé!");
        
        User found = findByEmail(insertedUserEmail);
        assertNull(found, "L'utilisateur ne doit plus exister");
        System.out.println("  ✅ Vérification: Utilisateur introuvable ✓");
    }

    private User findByEmail(String email) {
        return userService.afficher().stream()
                .filter(user -> email.equals(user.getEmail()))
                .findFirst()
                .orElse(null);
    }
}
