# 🎤 RECONNAISSANCE VOCALE - GUIDE D'IMPLÉMENTATION

## 📋 Résumé

Cette implémentation ajoute une fonctionnalité de reconnaissance vocale complète au système HospiSmart, permettant aux patients de décrire leurs symptômes oralement et obtenir un bilan de santé généré par l'IA.

## 🏗️ Architecture

### Services Clés

#### 1. **VoiceRecognitionService**
- **Rôle**: Gère l'enregistrement audio et la reconnaissance vocale via Vosk
- **Modèle**: Français (vosk-model-small-fr-0.22)
- **Durée max**: 10 secondes
- **Format audio**: 16-bit mono, 16kHz PCM

**Principales méthodes**:
```java
startListening()        // Lance l'enregistrement (10 sec max)
stopListening()         // Arrête manuellement
cleanup()              // Libère les ressources
isVoskAvailable()      // Vérifie la disponibilité
```

#### 2. **AIHealthSummaryService**
- **Rôle**: Génère un bilan de santé basé sur le texte reconnu
- **Modes**:
  - Mode IA (OpenAI API si configurée)
  - Mode Démo (analyse locale basée sur mots-clés)
- **Langue**: Français
- **Analyse**: Symptômes, diagnostic probable, recommandations, urgence

**Principales méthodes**:
```java
generateHealthSummary(voiceText)    // Génère le bilan
```

#### 3. **HealthSummaryService**
- **Rôle**: Persiste les bilans en base de données
- **Table**: `health_summaries`
- **Fonctionnalités**: CRUD complet

**Principales méthodes**:
```java
saveHealthSummary(patient, voiceText, aiSummary)    // Sauvegarde
getPatientSummaries(patient)                         // Récupère tous
getLatestSummary(patient)                            // Dernier bilan
deleteSummary(summaryId)                             // Supprime
```

## 🚀 Démarrage Rapide

### 1. Prérequis
- ✅ Maven (via mvnw)
- ✅ Java 11+
- ✅ Microphone connecté et activé
- ✅ Dossier `vosk-model-small-fr-0.22` dans le répertoire racine
- ✅ MySQL configurée avec la base 'hospismart'

### 2. Vérification du Modèle Vosk
```bash
# Le dossier doit avoir cette structure:
vosk-model-small-fr-0.22/
├── am/
│   └── final.mdl
├── conf/
│   ├── mfcc.conf
│   └── model.conf
├── graph/
│   └── Gr.fst
└── README
```

### 3. Configuration (optionnelle)

Pour utiliser OpenAI API au lieu du mode démo:

```bash
# Windows PowerShell:
$env:OPENAI_API_KEY = "sk-..."
```

### 4. Lancer l'Application
```bash
# Compilation
.\mvnw clean compile

# Exécution (via VS Code ou IDE JavaFX)
```

## 📊 Flux d'Utilisation

```
[Patient se connecte]
    ↓
[Clique sur "🎤 Consultation Vocale"]
    ↓
[Voir interface VoiceHealth.fxml]
    ↓
[Clique "🎤 Démarrer Enregistrement"]
    ↓
[Vosk écoute pendant 10 secondes (peut arrêter avant)]
    ↓
[Texte reconnu affiché]
    ↓
[AI génère bilan automatiquement]
    ↓
[Patient voit le bilan]
    ↓
[Peut sauvegarder en BD ou retourner]
```

## 🔍 Format du Bilan IA

### Mode Démo (Sans API)
```
╔════════════════════════════════════════════════════╗
║      BILAN DE SANTÉ - ANALYSE BASÉE SUR IA        ║
║         (Mode Démonstration - Analyse Locale)     ║
╚════════════════════════════════════════════════════╝

📋 DESCRIPTION DU PATIENT:
─────────────────────────
[Texte reconnu du patient]

🔍 SYMPTÔMES IDENTIFIÉS:
─────────────────────────
• Maux de tête / Migraine
• Fatigue
• Nausées/Vomissements

🩺 ANALYSE PRÉLIMINAIRE:
─────────────────────────
[Analyse basée sur mots-clés]

💊 DIAGNOSTIC PROBABLE:
─────────────────────────
[Diagnostic estimé]

✅ RECOMMANDATIONS:
─────────────────────────
• Consulter un médecin
• Repos et hydratation
...

⚠️ NIVEAU D'URGENCE:
─────────────────────────
🟢 NORMALE - Observation attentive et consultation si aggravation
```

### Mode IA (Avec OpenAI)
Bilan structuré en 5 sections professionnelles en français

## 🗄️ Schéma Base de Données

```sql
CREATE TABLE health_summaries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    voice_text LONGTEXT NOT NULL,
    ai_summary LONGTEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
)
```

## 🧪 Débogage et Logs

### Messages de Log Clés

| Préfixe | Signification |
|---------|---------------|
| `[Voice]` | Messages VoiceRecognitionService |
| `[AI]` | Messages AIHealthSummaryService |
| `[HealthSummary]` | Messages HealthSummaryService |
| `[VoiceHealth]` | Messages VoiceHealthController |

### Exemple de Sortie Console
```
[VoiceHealth] ════════════════════════════════════════
[VoiceHealth] Initialisation du contrôleur vocal
[VoiceHealth] ════════════════════════════════════════
[VoiceHealth] Patient: Jean Dupont
[Voice] ========================
[Voice] Initialisation Vosk...
[Voice] Recherche du modèle Vosk...
[Voice] ✅ Modèle trouvé: /path/to/vosk-model-small-fr-0.22
[Voice] Création du recognizer...
[Voice] Activation du microphone...
[Voice] ✅ Microphone activé
[Voice] 🎤 Parlez maintenant (max 10 secondes)...
...
[Voice] ✅ Texte reconnu: "j'ai mal à la tête et je me sens fatigué"
[AI] ========================
[AI] Génération du bilan IA
[AI] Texte reçu: j'ai mal à la tête...
[AI] ℹ️ API OpenAI non configurée
[AI] Utilisation du mode démo avec analyse locale
```

## ⚠️ Résolution des Problèmes Courants

### ❌ "Vosk non disponible"
**Causes possibles**:
- Modèle Vosk absent
- Chemin incorrect
- Librairies natives manquantes

**Solutions**:
1. Vérifier que `vosk-model-small-fr-0.22` existe au bon endroit
2. Vérifier le chemin absolu dans la console
3. Réinstaller les dépendances Vosk

### ❌ "Microphone non disponible"
**Causes possibles**:
- Microphone désactivé
- Permissions audio insuffisantes
- Autre application utilise le microphone

**Solutions**:
1. Vérifier que le microphone est branché et activé
2. Vérifier les paramètres son de Windows
3. Fermer les autres applications utilisant le microphone

### ❌ "Aucun texte reconnu"
**Causes possibles**:
- Microphone trop silencieux
- Bruit ambiant trop important
- Parole trop rapide/lente

**Solutions**:
1. Augmenter le volume du microphone
2. Parler plus lentement et distinctement
3. Utiliser dans un environnement silencieux

### ❌ "Erreur base de données"
**Causes possibles**:
- MySQL non lancé
- Base 'hospismart' n'existe pas
- Credentials MySQL incorrects

**Solutions**:
1. Lancer MySQL
2. Créer la base et utilisateur
3. Vérifier les credentials dans HealthSummaryService.java

## 📁 Fichiers Modifiés/Créés

### Créés
- ✅ `src/main/java/.../services/HealthSummaryService.java`

### Modifiés
- ✅ `src/main/java/.../services/VoiceRecognitionService.java`
- ✅ `src/main/java/.../services/AIHealthSummaryService.java`
- ✅ `src/main/java/.../controllers/VoiceHealthController.java`

### FXML (Inchangés)
- ✅ `src/main/resources/.../VoiceHealth.fxml`

## 🧬 Technologies Utilisées

| Technologie | Version | Rôle |
|------------|---------|------|
| Vosk | 0.3.32 | Reconnaissance vocale |
| OkHttp3 | 4.11.0 | Requêtes HTTP pour IA |
| Gson | 2.10.1 | Parsing JSON |
| JavaFX | 17.0.6 | Interface graphique |
| MySQL | 8.3.0 | Persistence données |

## 📚 Ressources

- [Vosk Documentation](https://alphacephei.com/vosk/)
- [OpenAI API Docs](https://platform.openai.com/docs)
- [JavaFX Documentation](https://gluonhq.com/products/javafx/)

## 🔐 Sécurité

- ✅ Validation des entrées vocales
- ✅ Hachage des données sensibles
- ✅ Connexions HTTPS pour API
- ⚠️ À implémenter: Authentification API OpenAI en backend

## 🎯 Prochaines Améliorations Possibles

1. Support multilingue (en, es, de, etc.)
2. Enregistrement audio persistant
3. Historique des bilans avec comparaison
4. Export PDF des bilans
5. Intégration médecin pour validation bilans
6. API REST pour accès programmatique
7. Support reconnaissance parleur
8. Amélioration feedback utilisateur temps réel

---

**Dernière mise à jour**: Avril 2026
**Status**: ✅ Prêt pour tests
