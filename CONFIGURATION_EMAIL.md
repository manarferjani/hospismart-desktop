# Configuration du Service Email - "Oublier le Mot de Passe"

Ce document explique comment configurer le service email pour permettre à vos utilisateurs de réinitialiser leur mot de passe par email.

## 🔧 Configuration Requise

### Étape 1 : Activer l'accès "App Passwords" sur Gmail

1. Allez sur https://myaccount.google.com/
2. Cliquez sur **Sécurité** dans le menu de gauche
3. Descendez jusqu'à **Mots de passe des applications**
4. Sélectionnez **Mail** et **Windows Computer**
5. Google vous générera un mot de passe de 16 caractères (ex: `abcd efgh ijkl mnop`)
6. Copiez ce mot de passe

> ⚠️ **IMPORTANT** : Ce mot de passe d'application est DIFFÉRENT de votre mot de passe Gmail normal

### Étape 2 : Configurer les Identifiants dans le Code

Ouvrez le fichier : `src/main/java/com/hospismart/hospismartdesktop/services/EmailService.java`

Localisez ces lignes (autour de la ligne 11-15) :

```java
private static final String SMTP_USER = "votre_email@gmail.com";
private static final String SMTP_PASSWORD = "votre_mot_de_passe_application";
```

Remplacez-les par :

```java
private static final String SMTP_USER = "votre.email@gmail.com";  // Votre adresse Gmail
private static final String SMTP_PASSWORD = "votre mot de passe application";  // Les 16 caractères de Google
```

**Exemples :**
```java
private static final String SMTP_USER = "hospismart.app@gmail.com";
private static final String SMTP_PASSWORD = "abcd efgh ijkl mnop";
```

### Étape 3 : Construire et Tester

1. Open le terminal et exécutez :
```bash
mvn clean javafx:run
```

2. Sur l'écran de connexion, cliquez sur **"Mot de passe oublié ?"**

3. Entrez une adresse email valide dans votre base de données

4. Vous devriez recevoir un email avec le nouveau mot de passe

## ✅ Vérification du Fonctionnement

### Console Output

Vérifiez la console pour ces messages :

**✓ Succès :**
```
[EmailService] Email de réinitialisation envoyé avec succès à: utilisateur@gmail.com
```

**✗ Erreurs Courantes :**

| Message | Cause | Solution |
|---------|-------|----------|
| `Identifiants SMTP invalides` | Les identifiants ne sont pas configurés | Vérifiez l'étape 2 |
| `535 - Authentication failed` | Mot de passe d'application incorrect | Régénérez le mot de passe dans votre compte Google |
| `Connection timeout` | Pas de connexion internet | Vérifiez votre connexion réseau |
| `Mail server not initialized` | Configuration Gmail incomplète | Vérifiez les identifiants Gmail |

## 🛡️ Sécurité

- **Ne pas** committer vos identifiants réels dans le code
- Utilisez toujours un **mot de passe d'application**, pas votre mot de passe Gmail principal
- Considérez un fichier de configuration externe pour les environnements de production

## 🚀 Fonctionnalités Implémentées

✅ **Réinitialisation de mot de passe par email**
- Génère un mot de passe aléatoire sécurisé
- Envoie le nouveau mot de passe par email
- Mise à jour hash du mot de passe en base de données

✅ **Gestion des erreurs améliorée**
- Validation d'adresse email
- Messages d'erreur détaillés
- Support pour les spams Gmail

✅ **Interface utilisateur conviviale**
- Dialog box pour entrer l'email
- Messages de confirmation/erreur clairs
- Lien "Mot de passe oublié ?" sur la page de connexion

## 📧 Format de l'Email

L'email envoyé contient :
- Nouveau mot de passe en HTML stylisé
- Recommandation de changer le mot de passe
- En-tête et footer Hospismart

## ❓ Dépannage Avancé

Si vous continuez à avoir des erreurs :

1. **Vérifiez que 2FA est activé** (requiert les mots de passe d'application)
2. **Autorisez les applications "moins sécurisées"** (ancienne méthode, moins recommandée)
3. **Utilisez une adresse Gmail créée récemment** (gmail.com, pas votre domaine personnalisé)
4. **Testez avec un email différent** (ex: Gmail au lieu de Outlook)

## 📝 Exemple de Configuration Testée

```java
// Configuration qui fonctionne :
SMTP_HOST = "smtp.gmail.com"
SMTP_PORT = "587"
SMTP_USER = "hospismart.app@gmail.com"
SMTP_PASSWORD = "xyzt abcd efgh ijkl"  // Mot de passe d'application Google
```

---

**Questions ?** Consultez la documentation officielle Google :
https://support.google.com/accounts/answer/185833
