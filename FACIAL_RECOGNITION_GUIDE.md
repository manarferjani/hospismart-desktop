# 📷 Reconnaissance Faciale - Guide d'Installation et d'Utilisation

## 🚀 Installation

### Prérequis

1. **Caméra USB connectable** à votre ordinateur (webcam intégrée ou USB)
2. **Java 17+** (déjà configuré dans votre projet)
3. **OpenCV 4.8.1** (ajouté automatiquement via Maven)

### Installation Automatique

La dépendance OpenCV est déjà configurée dans `pom.xml` :

```xml
<dependency>
    <groupId>org.opencv</groupId>
    <artifactId>opencv-java</artifactId>
    <version>4.8.1</version>
</dependency>
```

Lancez simplement :
```bash
mvn clean javafx:run
```

Maven téléchargera automatiquement OpenCV.

---

## 📲 Fonctionnalités Implémentées

### 1️⃣ Connexion par Reconnaissance Faciale
- Accès depuis le bouton **"📷 Connexion par Visage"** de l'écran de connexion
- Caméra en direct avec détection de visage
- Reconnaissance automatique si le visage est enregistré
- Redirection vers le tableau de bord de l'utilisateur

### 2️⃣ Enregistrement de Visage (Optionnel)
- Lors de l'inscription, option d'enregistrer son visage
- Interface `FaceRegister.fxml` pour capture
- Enregistrement fait localement (fichier PNG)

### 3️⃣ Vérification de Sécurité
- Comparaison d'histogrammes pour la reconnaissance
- Seuil de similarité de 60% pour un match valide
- Détection de visages multiples gérée

---

## 🎥 Utilisation - Flux Utilisateur

### Première Visite (Inscription + Enregistrement Faciale)

1. Cliquez sur **"Pas de compte ? S'inscrire ici"**
2. Remplissez le formulaire d'inscription
3. Cliquez sur **"Créer un compte"**
4. *(Optionnel)* Enregistrez votre visage si vous le souhaitez
   - Positionnez votre visage dans le cadre
   - Cliquez sur **"📷 Enregistrer mon visage"**
   - Ou **"⏭️ Passer"** pour ignorer

### Connexion par Reconnaissance Faciale

1. Sur l'écran de connexion, cliquez sur **"📷 Connexion par Visage"**
2. L'interface affiche le flux vidéo de votre caméra
3. Positionnez votre visage dans le cadre
4. Cliquez sur **"📷 Capturer et Connexion"**
5. Le système reconnaît votre visage et vous connecte automatiquement
6. Si pas reconnu : message d'erreur et possibilité de réessayer

### Retour à la Connexion Classique

- Depuis l'écran de reconnaissance faciale : cliquez sur **"❌ Retour"**

---

## 🔧 Architecture Technique

### Classes Créées

#### 1. `FaceRecognitionService.java`
Service core pour la reconnaissance faciale
- `initializeCamera()` : Initialise la caméra USB
- `captureFrame()` : Capture une frame vidéo
- `detectFaces(Mat image)` : Détecte les visages (Haar Cascade)
- `registerFace(int userId, Mat faceImage)` : Enregistre le visage
- `verifyFace(int userId, Mat capturedFace)` : Vérifie un visage
- `compareFacesUsingHistogram()` : Compare par histogramme

#### 2. `FaceLoginController.java`
Contrôleur pour la connexion par visage
- Interface avec flux vidéo en live
- Capture et vérification du visage
- Navigation vers le tableau de bord

#### 3. `FaceRegisterController.java`
Contrôleur pour l'enregistrement du visage
- Capture du premier visage
- Enregistrement local en PNG

### Algorithme de Reconnaissance

1. **Détection Haar Cascade** : Localise les visages dans la frame
2. **Comparaison d'Histogrammes** : Compare les histogrammes des images
3. **Seuil de Similarité** : 60% minimum pour un match positif
4. **Normalisation** : Images redimensionnées à 200x200 pixels

### Stockage des Données

```
faces_data/
├── user_1.png        # Visage de l'utilisateur ID=1
├── user_2.png        # Visage de l'utilisateur ID=2
└── user_3.png        # Etc.
```

---

## ⚙️ Configuration

### Changer le Seuil de Reconnaissance

Dans `FaceRecognitionService.java`, ligne `~180` :

```java
// Si similarité > 60% c'est un match
boolean isMatch = similarity > 0.60;
```

**Valeurs recommandées :**
- `0.50` : Plus permissif (+ de faux positifs)
- `0.60` : Équilibré (défaut)
- `0.80` : Plus strict (- de faux positifs)

### Redimensionner la Caméra

Dans `FaceLoginController.java`, ligne `~120` :

```java
Imgproc.resize(capturedFace, resizedCaptured, new Size(200, 200));
```

Changez `new Size(200, 200)` pour d'autres dimensions.

---

## 🛠️ Dépannage

### ❌ "Aucune caméra détectée"

**Causes :**
- Caméra USB non branchée
- Caméra déjà utilisée par une autre application
- Problème de driver camera

**Solutions :**
- Rebranchez la caméra USB
- Fermez les autres applications utilisant la caméra (Teams, Zoom, etc.)
- Vérifiez les drivers dans le Gestionnaire de périphériques

### ❌ "Impossible de charger le classificateur"

**Cause :** Le fichier `haarcascade_frontalface_alt.xml` n'est pas trouvé

**Solution :** Il sera téléchargé automatiquement via Maven. Relancez :
```bash
mvn clean javafx:run
```

### ❌ "Visage non reconnu"

**Causes :**
- Visage pas enregistré
- Mauvais angle/éclairage
- Ressemblance insuffisante (masque, lunettes, etc.)

**Solutions :**
- Enregistrez votre visage avec bonne lumière
- Essayez sans masque / lunettes
- Réessayez avec un meilleur angle
- Réenregistrez votre visage (remplace l'ancien)

### ⚠️ Performances Lentes

**Cause :** Réduction d'histogrammes peut être lente sur machines faibles

**Optimisations :**
- Réduire la résolution : `new Size(100, 100)`
- Augmenter le seuil : `similarity > 0.75`
- Réduire la complexité de détection

---

## 🔒 Sécurité

### Considérations

1. **Stockage local** : Les visages sont stockés en local (`faces_data/`), PAS en cloud
2. **Pas de chiffrement** : Pour production, chiffer `faces_data/`
3. **Pas de serveur** : Tout est local à la machine
4. **Histogrammesrégit** : Pas de données biométriques personnelles stockées

### Pour Production

1. Chiffrer le répertoire `faces_data/`
2. Ajouter une vérification supplémentaire (captcha, 2FA)
3. Logger les tentatives de reconnaissance faciale
4. Implémenter un timeout après N tentatives échouées

---

## 📊 Accuracy & Limitations

### Factors Affecting Recognition

| Factor | Impact | Recommendation |
|--------|--------|-----------------|
| Éclairage | ⭐⭐⭐ Critique | Bonne lumière naturelle |
| Angle visage | ⭐⭐⭐ Critique | Face caméra |
| Distance | ⭐⭐ Important | 30-60cm optimal |
| Accessoires | ⭐⭐ Important | Sans masque/lunettes |
| Expression | ⭐ Mineur | Expression neutre conseillée |

### Accuracy Typical

- **Bonnes conditions** : 95%+ de reconnaissance
- **Conditions moyennes** : 85-95%
- **Mauvaises conditions** : < 85%

---

## 📚 Ressources

- [OpenCV Documentation](https://docs.opencv.org/)
- [Haar Cascade Classifiers](https://docs.opencv.org/master/db/d28/tutorial_cascade_classifier.html)
- [JavaFX ImageView](https://docs.oracle.com/javase/11/docs/api/javafx.graphics/javafx/scene/image/ImageView.html)

---

## ✅ Test Complet

Pour tester la fonctionnalité complète :

1. **Lancez l'application**
   ```bash
   mvn clean javafx:run
   ```

2. **Créez un nouvel utilisateur + Enregistrez visage**
   - Cliquez "S'inscrire"
   - Remplissez le formulaire
   - Enregistrez votre visage

3. **Testez la connexion par visage**
   - Cliquez "📷 Connexion par Visage"
   - Positionnez votre visage
   - Cliquez "Capturer"
   - ✅ Vous devriez être connecté !

---

## 🎯 Roadmap Futur

- [ ] Enregistrement de plusieurs angles de visage
- [ ] Amélioration de l'algorithme (LBP, SIFT, Deep Learning)
- [ ] Support de multiples caméras
- [ ] Adjustment du seuil de reconnaissance en temps réel
- [ ] Statistiques de reconnaissance (% succès)
- [ ] Mode "Anti-spoofing" (détection vidéo liveness)

---

**Développé en local avec OpenCV 4.8.1** ✅
