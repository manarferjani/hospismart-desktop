# 🔐 Guide Complet: Authentification à Deux Facteurs (2FA) avec Google Authenticator

## 📋 Vue d'ensemble

L'authentification à deux facteurs (2FA) a été ajoutée au système HospiSmart. Elle utilise **Google Authenticator** basé sur l'algorithme **TOTP** (Time-based One-Time Password).

---

## ✅ Implémentation Complète

### 1. **Dépendances Maven Ajoutées**
```xml
<!-- TOTP Library for Google Authenticator -->
<dependency>
    <groupId>com.warrenstrange</groupId>
    <artifactId>googleauth</artifactId>
    <version>1.5.0</version>
</dependency>

<!-- QR Code Generation -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.1</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.1</version>
</dependency>
```

### 2. **Services Créés**

#### `TwoFactorAuthService.java`
Service principal pour la gestion 2FA:
- ✅ `generateSecret()` - Génère une clé secrète TOTP
- ✅ `generateQRCodeImage()` - Crée une image QR code
- ✅ `generateQRCodeBase64()` - QR code en Base64 (pour UI)
- ✅ `verifyCode()` - Vérifie un code TOTP
- ✅ `generateBackupCodes()` - Génère des codes de secours (16 codes)
- ✅ `getCurrentCode()` - Obtient le code actuel (tests)

**Localisation:** `src/main/java/com/hospismart/hospismartdesktop/services/TwoFactorAuthService.java`

### 3. **Modifications du Modèle User**

Deux nouveaux champs ajoutés:
```java
private boolean twoFactorEnabled = false;    // Active/Inactive
private String twoFactorSecret = null;       // Clé secrète TOTP
```

**Getters/Setters:**
- `isTwoFactorEnabled()` / `setTwoFactorEnabled()`
- `getTwoFactorSecret()` / `setTwoFactorSecret()`

### 4. **Modifications de UserService**

**Colonnes Base de Données Créées Automatiquement:**
```sql
ALTER TABLE user ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE user ADD COLUMN two_factor_secret VARCHAR(255) NULL;
```

**Méthodes Ajoutées:**
- ✅ `saveTwoFactorSecret(userId, secret)` - Sauvegarde et active 2FA
- ✅ `disableTwoFactor(userId)` - Désactive 2FA
- ✅ `getTwoFactorSecret(userId)` - Récupère la clé secrète
- ✅ `isTwoFactorEnabled(userId)` - Vérifie si 2FA est activée

### 5. **Contrôleurs Créés**

#### **TwoFactorAuthController**
Gère la vérification du code 2FA à la connexion
- Fichier: `src/main/java/com/hospismart/hospismartdesktop/controllers/TwoFactorAuthController.java`
- FXML: `src/main/resources/com/hospismart/hospismartdesktop/TwoFactorVerification.fxml`

**Fonctionnalités:**
- ✅ Saisie du code à 6 chiffres
- ✅ Vérification du code TOTP
- ✅ Limite de 5 tentatives
- ✅ Support des codes de secours (à venir)
- ✅ Redirection vers le profil après vérification

#### **SetupTwoFactorController**
Gère la configuration initiale de 2FA
- Fichier: `src/main/java/com/hospismart/hospismartdesktop/controllers/SetupTwoFactorController.java`
- FXML: `src/main/resources/com/hospismart/hospismartdesktop/Setup2FA.fxml`

**Fonctionnalités:**
- ✅ Génération d'une nouvelle clé secrète
- ✅ Affichage du QR code
- ✅ Affichage de la clé secrète en clair
- ✅ Copie de la clé dans le presse-papiers
- ✅ Vérification du code avant activation
- ✅ Sauvegarde en base de données

### 6. **Modification de LoginController**

Le flux de connexion a été modifié:

```
1. Saisie email/mot de passe
   ↓
2. Vérification email/mot de passe
   ↓
3. Vérification si 2FA est activée?
   ├─ OUI → Redirection vers TwoFactorVerification.fxml
   │         ↓
   │         Saisie du code 2FA
   │         ↓
   │         Vérification du code
   │         ↓
   │         Redirection vers le profil
   │
   └─ NON → Redirection directe vers le profil
```

---

## 🚀 Flux Complet d'Utilisation

### **Phase 1: Configuration Initiale (Par l'utilisateur)**

1. **Connexion** → Profil utilisateur
2. **Bouton "Activer 2FA"** → Page Setup2FA.fxml
3. **Télécharger Google Authenticator** (iOS/Android)
4. **Scanner le QR code** ou entrer la clé manuellement
5. **Entrer le code 6 chiffres** généré par l'app
6. **Cliquer "Activer 2FA"** → 2FA activée ✅

### **Phase 2: Connexion avec 2FA Activée**

1. **Écran de connexion** → Saisir email/mot de passe
2. **Vérification réussie** → Redirection vers TwoFactorVerification.fxml
3. **Saisir le code 2FA** généré par Google Authenticator
4. **Vérification du code** → Profil utilisateur ✅

### **Phase 3: Désactivation de 2FA**

1. **Profil utilisateur** → Bouton "Désactiver 2FA"
2. **Confirmation** → 2FA désactivée
3. **Prochaine connexion** → Pas de vérification 2FA

---

## 📱 Installation Google Authenticator

### Android
- Ouvrir Google Play Store
- Rechercher "Google Authenticator"
- Télécharger et installer

### iOS
- Ouvrir App Store
- Rechercher "Google Authenticator"
- Télécharger et installer

### Alternatives (Compatibles TOTP)
- Microsoft Authenticator
- Authy
- FreeOTP
- Bitwarden

---

## 🛡️ Sécurité

### Chiffrage
- ✅ Les clés secrètes sont stockées en **base de données**
- ✅ Les mots de passe utilisateur sont **hashés avec BCrypt**
- ⚠️ À améliorer: Chiffrer les clés secrètes dans la DB

### Limitation des Tentatives
- ✅ Maximum **5 tentatives** de codes incorrects
- ✅ Après 5 tentatives, le code est demandé à nouveau

### Codes de Secours
- 📋 8 codes de 8 chiffres généré pour chaque utilisateur
- 📝 À implémenter: Stockage sécurisé et utilisation

---

## 📝 Structure des Fichiers

```
src/main/java/com/hospismart/hospismartdesktop/
├── services/
│   ├── TwoFactorAuthService.java          ✅ Service 2FA
│   └── UserService.java                   ✅ Modifié pour 2FA
│
├── controllers/
│   ├── TwoFactorAuthController.java       ✅ Vérification 2FA
│   ├── SetupTwoFactorController.java      ✅ Configuration 2FA
│   └── LoginController.java               ✅ Modifié pour 2FA
│
└── models/
    └── User.java                          ✅ Modifié pour 2FA

src/main/resources/com/hospismart/hospismartdesktop/
├── TwoFactorVerification.fxml             ✅ Interface vérification
├── Setup2FA.fxml                          ✅ Interface configuration
└── style-admin.css                        ✅ Styles
```

---

## 🔧 Configuration Nécessaire

### 1. Créer le répertoire QR Codes
```bash
mkdir qr_codes/
```

### 2. Recompiler le projet
```bash
mvn clean compile
# ou
./mvnw.cmd clean compile
```

### 3. Tester
```bash
mvn javafx:run
# ou
./mvnw.cmd javafx:run
```

---

## ✨ Fonctionnalités Implémentées

| Fonctionnalité | Status | Détails |
|---|---|---|
| Génération de clé secrète | ✅ | TOTP standard |
| QR Code | ✅ | Généré dynamiquement |
| Vérification TOTP | ✅ | Code à 6 chiffres |
| Activation 2FA | ✅ | Sauvegarde en DB |
| Désactivation 2FA | ✅ | Suppression de la clé |
| Codes de secours | 📋 | À implémenter |
| Récupération compte | 📋 | À implémenter |
| Historique connexions 2FA | 📋 | À implémenter |

---

## 🧪 Tests Manuels

### Test 1: Activation 2FA
```
1. Se connecter normalement
2. Aller dans Profil → Activer 2FA
3. Scanner le QR code avec Google Authenticator
4. Entrer le code 6 chiffres
5. Cliquer "Activer 2FA"
✅ Résultat: 2FA activée
```

### Test 2: Connexion avec 2FA
```
1. Se déconnecter
2. Essayer de se connecter avec email/mot de passe
3. Page de vérification 2FA s'affiche
4. Entrer le code généré par Google Authenticator
5. Cliquer "Vérifier"
✅ Résultat: Redirection vers profil
```

### Test 3: Code Incorrect
```
1. Connexion → Saisir un code incorrect
2. Cliquer "Vérifier"
✅ Résultat: Message d'erreur + tentatives restantes
```

---

## 🐛 Dépannage

### "Impossible de charger le QR code"
- Vérifier que les dépendances Zxing sont bien installées
- Vérifier que le répertoire `qr_codes/` existe

### "Code incorrect même avec le bon code"
- Vérifier la synchronisation horaire du serveur et du téléphone
- Attendre le code suivant (délai de 30 secondes entre chaque code)

### "2FA activée mais pas demandée à la connexion"
- Vérifier que la colonne `two_factor_enabled` est bien `TRUE` en DB
- Vérifier que le LoginController charge les données 2FA

---

## 📞 Support

Pour tout problème ou amélioration suggérée concernant la 2FA:
1. Vérifier les logs de la console
2. Consulter la documentation de Google Authenticator
3. Contacter l'équipe de développement

---

## 🔜 Améliorations Futures

- [ ] Chiffrement des clés secrètes en base de données
- [ ] Implémentation des codes de secours
- [ ] Historique des connexions 2FA
- [ ] Option de récupération de compte
- [ ] Notification d'ajout de 2FA par email
- [ ] Support WebAuthn/FIDO2
- [ ] Authentification par biométrie + 2FA

---

## 📚 Références

- [Google Authenticator API](https://github.com/google/google-authenticator)
- [TOTP (RFC 6238)](https://tools.ietf.org/html/rfc6238)
- [Zxing QR Code Library](https://github.com/zxing/zxing)

