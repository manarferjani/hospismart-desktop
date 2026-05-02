# 🎤 GUIDE DE RECONNAISSANCE VOCALE ET DIAGNOSTIC PAR IA

## 📋 Vue d'ensemble

Le système **HospiSmart Voice Health Diagnosis** permet aux patients de :
1. **Décrire leurs symptômes vocalement** via microphone
2. **Obtenir un bilan de santé instantané** généré par IA
3. **Recevoir des recommandations médicales** basées sur les symptômes
4. **Sauvegarder les bilans** en base de données pour suivi

---

## 🏗️ Architecture Technique

### Composants Principaux

#### 1️⃣ **VoiceRecognitionService**
- **Fonction**: Capture audio du microphone et reconnaissance vocale
- **Modèle**: Vosk (français - `vosk-model-small-fr-0.22`)
- **Durée d'enregistrement**: 10 secondes maximum
- **Format audio**: 16-bit mono, 16kHz PCM

**Classe**: `com.hospismart.hospismartdesktop.services.VoiceRecognitionService`

```java
// Utilisation
VoiceRecognitionService voiceService = new VoiceRecognitionService();
String recognizedText = voiceService.startListening();
voiceService.stopListening();
voiceService.cleanup();
```

#### 2️⃣ **AIHealthSummaryService**
- **Fonction**: Analyse le texte et génère un diagnostic par IA
- **Modes**: 
  - Mode OpenAI (si clé API configurée)
  - Mode Démo (analyse locale intelligente)
- **Langue**: Français
- **Génère**:
  - Symptômes identifiés
  - Analyse préliminaire
  - Diagnostic probable
  - Recommandations médicales
  - Niveau d'urgence

**Classe**: `com.hospismart.hospismartdesktop.services.AIHealthSummaryService`

```java
// Utilisation
AIHealthSummaryService aiService = new AIHealthSummaryService();
String bilan = aiService.generateHealthSummary(voiceText);
```

#### 3️⃣ **HealthSummaryService**
- **Fonction**: Persiste les bilans en base de données
- **Base de données**: MySQL - table `health_summaries`
- **Fonctionnalités**: CRUD complet

**Classe**: `com.hospismart.hospismartdesktop.services.HealthSummaryService`

```java
// Utilisation
HealthSummaryService healthService = new HealthSummaryService();
boolean saved = healthService.saveHealthSummary(patient, voiceText, aiSummary);
```

---

## 🎯 Interface Utilisateur

### Écran: VoiceHealth.fxml

**Localisation**: 
```
src/main/resources/com/hospismart/hospismartdesktop/VoiceHealth.fxml
```

**Éléments de l'interface**:

```
┌─────────────────────────────────────────────────────────────────┐
│  🎤 Consultation Vocale - Bilan de Santé                ⬅️ Retour│
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  📋 INSTRUCTIONS                                                  │
│  ├─ Cliquez sur "Démarrer Enregistrement"                       │
│  ├─ Décrivez vos symptômes pendant 10 secondes                  │
│  └─ L'IA générera automatiquement un bilan professionnel        │
│                                                                   │
│  🎤 ENREGISTREMENT VOCAL                                         │
│  ├─ [🎤 Démarrer]  [⏹️ Arrêter]  Status: ✅ Prêt               │
│                                                                   │
│  📝 TEXTE RECONNU                                                │
│  ├─ (Texte vocalisé reconverti en texte)                       │
│                                                                   │
│  🤖 BILAN DE SANTÉ (par IA)                                     │
│  ├─ [Résultat du diagnostic avec:                              │
│  │  - Symptômes identifiés                                      │
│  │  - Analyse préliminaire                                      │
│  │  - Diagnostic probable                                       │
│  │  - Recommandations                                           │
│  │  - Niveau d'urgence]                                         │
│  └─ [💾 Sauvegarder]                                            │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Installation et Configuration

### 1️⃣ Prérequis

- ✅ **Java 11+** installé et `JAVA_HOME` configuré
- ✅ **Maven** (via `mvnw.cmd`)
- ✅ **Microphone** connecté et activé
- ✅ **Model Vosk** extrait dans le dossier `model/`
- ✅ **MySQL 5.7+** configurée avec base `hospismart`

### 2️⃣ Vérifier le Modèle Vosk

```bash
# Vérifier la structure (depuis la racine du projet)
# model/
# ├── am/
# │   └── final.mdl
# ├── conf/
# │   ├── mfcc.conf
# │   └── model.conf
# ├── graph/
# │   └── Gr.fst
# ├── ivector/
# └── README
```

### 3️⃣ Configuration Optionnelle: OpenAI API

Pour utiliser OpenAI au lieu du mode démo:

**Windows PowerShell**:
```powershell
$env:OPENAI_API_KEY = "sk-YOUR_OPENAI_API_KEY_HERE"
```

**Linux/Mac**:
```bash
export OPENAI_API_KEY="sk-YOUR_OPENAI_API_KEY_HERE"
```

### 4️⃣ Compiler et Exécuter

```bash
# Compiler
cd hospismart-desktop
.\mvnw clean compile

# Exécuter (via IDE JavaFX ou)
# Exécuter directement via VS Code ou un IDE JavaFX comme IntelliJ/Eclipse
```

---

## 📊 Flux d'Utilisation Complet

```
┌──────────────────────────────────────────────────────────────┐
│  1. PATIENT SE CONNECTE                                      │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  2. ACCÈDE AU MENU "CONSULTATION VOCALE"                     │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  3. INTERFACE VoiceHealth.fxml S'AFFICHE                     │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  4. PATIENT CLIQUE "🎤 DÉMARRER ENREGISTREMENT"             │
│     • Microphone s'active                                    │
│     • VoiceRecognitionService lance capture audio          │
│     • "🎤 Parlez maintenant (max 10 secondes)..."          │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  5. PATIENT DÉCRIT SES SYMPTÔMES                            │
│     Exemple: "J'ai une fièvre depuis hier, j'ai mal à       │
│              la gorge et je tousse beaucoup"                │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  6. VOSK CONVERTIT L'AUDIO EN TEXTE                         │
│     Résultat: "j'ai une fièvre depuis hier, j'ai mal à      │
│               la gorge et je tousse beaucoup"               │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  7. TEXTE AFFICHÉ EN "TEXTE RECONNU"                        │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  8. AIHealthSummaryService ANALYSE LE TEXTE:               │
│     • Détecte symptômes: Fièvre, mal de gorge, toux        │
│     • Génère diagnostic probable                            │
│     • Détermine niveau d'urgence                            │
│     • Crée recommandations médicales                        │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  9. BILAN AFFICHÉ DANS "BILAN DE SANTÉ (PAR IA)"           │
│                                                              │
│  ╔════════════════════════════════════════╗                │
│  ║  BILAN DE SANTÉ - ANALYSE PAR IA      ║                │
│  ╠════════════════════════════════════════╣                │
│  ║ 📋 DESCRIPTION:                        ║                │
│  ║ (Texte original du patient)            ║                │
│  ║                                        ║                │
│  ║ 🔍 SYMPTÔMES IDENTIFIÉS:              ║                │
│  ║ • Fièvre                               ║                │
│  ║ • Mal de gorge                         ║                │
│  ║ • Toux                                 ║                │
│  ║                                        ║                │
│  ║ 🩺 ANALYSE PRÉLIMINAIRE:              ║                │
│  ║ Infection respiratoire probable...     ║                │
│  ║                                        ║                │
│  ║ 💊 DIAGNOSTIC PROBABLE:               ║                │
│  ║ INFECTION RESPIRATOIRE MODÉRÉE        ║                │
│  ║ • Grippe (influenza)                   ║                │
│  ║ • Pharyngite virale                    ║                │
│  ║ • Bronchite aiguë                      ║                │
│  ║ Action: Consultation médicale...       ║                │
│  ║                                        ║                │
│  ║ ✅ RECOMMANDATIONS:                    ║                │
│  ║ 🏥 Consulter un médecin...            ║                │
│  ║ 🩺 Gargariser avec eau salée...       ║                │
│  ║ 💊 Prendre du paracétamol...          ║                │
│  ║ 📊 Mesurer la température...          ║                │
│  ║ ☎️ Appeler SAMU si aggravation...     ║                │
│  ║                                        ║                │
│  ║ ⚠️ NIVEAU D'URGENCE:                   ║                │
│  ║ 🟡 MODÉRÉE - Consultation 24-48h     ║                │
│  ║                                        ║                │
│  ╚════════════════════════════════════════╝                │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  10. PATIENT CLIQUE "💾 SAUVEGARDER"                        │
│      • HealthSummaryService sauvegarde en BDD               │
│      • Message: "Bilan sauvegardé avec succès"            │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  11. BILAN DISPONIBLE DANS LE PROFIL PATIENT               │
│      • Historique de tous les bilans                        │
│      • Dates et heures de consultation                      │
│      • Possibilité d'exporter ou imprimer                   │
└──────────────────────────────────────────────────────────────┘
```

---

## 🎯 Caractéristiques du Diagnostic

### 📊 Symptômes Détectés

Le système détecte automatiquement ces symptômes (en français):

| Symptôme | Détecteurs |
|----------|-----------|
| 🤒 Fièvre | "fièvre", "fiévreux", "température élevée" |
| 🤧 Rhume | "rhume", "rhinite", "nez bouché" |
| 😷 Toux | "toux", "tousse", "toussotement" |
| 😷 Grippe | "grippe", "grippal", "syndrome grippal" |
| 🤢 Nausée | "nausée", "nausées", "vomir", "vomissement" |
| 🤕 Mal de tête | "mal de tête", "migraine", "céphalée" |
| 😩 Fatigue | "fatigue", "fatigué", "épuisement" |
| 😵 Vertiges | "vertige", "vertiges", "étourdissement" |
| 🫁 Difficultés respiratoires | "respiration difficile", "essoufflement" |
| 🚽 Troubles digestifs | "diarrhée", "constipation", "mal d'estomac" |
| 😤 Mal de gorge | "mal à la gorge", "angine", "pharyngite" |
| 🤧 Allergies | "allergie", "allergique" |

### 🩺 Diagnostics Générés

Le système génère un diagnostic probable basé sur les combinaisons:

1. **INFECTION RESPIRATOIRE GRAVE**
   - Condition: Fièvre + Toux + Gorge + Difficultés respiratoires
   - Diagnostics: Pneumonie, Bronchite sévère
   - Urgence: 🔴 ÉLEVÉE

2. **INFECTION RESPIRATOIRE MODÉRÉE**
   - Condition: Fièvre + Toux + Gorge
   - Diagnostics: Grippe, Pharyngite, Bronchite
   - Urgence: 🟡 MODÉRÉE

3. **GASTROENTÉRITE**
   - Condition: Nausée + Diarrhée + pas de fièvre
   - Urgence: 🟡 MODÉRÉE

4. **SYNDROME GRIPPAL**
   - Condition: Fièvre + Fatigue + Mal de tête
   - Urgence: 🟡 MODÉRÉE

5. **CÉPHALÉE/MIGRAINE**
   - Condition: Mal de tête sans fièvre
   - Urgence: 🟢 BASSE

6. **RÉACTION ALLERGIQUE**
   - Condition: Allergie détectée
   - Urgence: 🟢 BASSE

### ⚠️ Niveaux d'Urgence

| Niveau | Couleur | Condition | Action |
|--------|---------|-----------|--------|
| **ÉLEVÉE** | 🔴 | Symptômes graves: difficultés respiratoires, perte conscience, convulsion | ☎️ Appeler SAMU (15) IMMÉDIATEMENT |
| **MODÉRÉE** | 🟡 | Fièvre + infection probable | Consultation 24-48 heures |
| **BASSE** | 🟢 | Symptômes légers à modérés | Observation ou pharmacien |
| **NORMALE** | 🟢 | Pas de symptômes spécifiques | Observation attentive |

---

## 🧪 Tests et Validation

### Test Basique

```bash
# Lancer les tests unitaires
.\mvnw test -Dtest=VoiceRecognitionTest
```

### Test Manuel

1. **Démarrer l'application**
   - Exécuter depuis IDE JavaFX
   - Ou: `.\mvnw javafx:run`

2. **Se connecter comme patient**

3. **Accéder à "Consultation Vocale"**

4. **Cliquer "Démarrer Enregistrement"**

5. **Parler pendant 10 secondes**
   - Exemple: "Bonjour, j'ai mal à la tête depuis ce matin et je me sens très fatigué"

6. **Vérifier les résultats**
   - Texte reconnu s'affiche
   - Bilan généré automatiquement
   - Cliquer "Sauvegarder" pour persister

---

## 🔐 Sécurité et Confidentialité

### Données Sensibles

- 📝 Les textes vocaux et bilans sont **stockés en base de données**
- 🔒 Accès limité au **patient propriétaire du bilan**
- 🏥 Les médecins peuvent **accéder au dossier complet** du patient
- 🔐 Chiffrement recommandé en production

### Configuration MySQL

```sql
-- Vérifier les permissions
USE hospismart;
SELECT * FROM health_summaries WHERE user_id = ?;

-- Supprimer les bilans sensibles
DELETE FROM health_summaries WHERE id = ?;
```

---

## 🐛 Dépannage

### Problème 1: Vosk non détecté
```
❌ "Modèle Vosk non trouvé"
```
**Solution**:
- ✅ Vérifier que le dossier `model/` existe à la racine du projet
- ✅ Vérifier que `model/am/final.mdl` existe
- ✅ Redémarrer l'application

### Problème 2: Microphone non activé
```
❌ "Microphone non disponible"
```
**Solution**:
- ✅ Vérifier les paramètres son de Windows
- ✅ Vérifier les permissions d'accès au microphone
- ✅ Tester le microphone avec Audacity ou autre outil

### Problème 3: Pas de texte reconnu
```
❌ "Aucun texte reconnu"
```
**Solution**:
- ✅ Parler plus fort et clairement
- ✅ Vérifier que le microphone fonctionne (niveau audio)
- ✅ Éviter les bruits de fond
- ✅ Mode démo s'active automatiquement

### Problème 4: Base de données non accessible
```
❌ "Impossible de sauvegarder le bilan"
```
**Solution**:
- ✅ Vérifier que MySQL est en cours d'exécution
- ✅ Vérifier les credentials dans `HealthSummaryService`
- ✅ Vérifier que la base `hospismart` existe

---

## 📈 Performance et Optimisation

### Temps de Traitement

- ⏱️ Enregistrement audio: **10 secondes**
- ⏱️ Reconnaissance vocale (Vosk): **2-3 secondes**
- ⏱️ Génération du bilan (IA local): **< 1 seconde**
- ⏱️ Génération via OpenAI: **2-5 secondes**
- ⏱️ **Temps total**: ~13-18 secondes

### Optimisations Possibles

1. **Utiliser un GPU** pour Vosk (accélération)
2. **Cache des modèles IA** (réduire temps chargement)
3. **Compression des audio** (réduire taille BDD)
4. **Batch processing** (traiter plusieurs bilans)

---

## 📚 Ressources

### Documentation
- 📖 [Vosk Documentation](https://github.com/alphacep/vosk)
- 📖 [OpenAI API Docs](https://platform.openai.com/docs)
- 📖 [JavaFX Docs](https://openjfx.io/)

### Fichiers Clés

```
src/main/java/com/hospismart/hospismartdesktop/
├── services/
│   ├── VoiceRecognitionService.java (880 lines)
│   ├── AIHealthSummaryService.java (450+ lines) ⭐ AMÉLIORÉ
│   └── HealthSummaryService.java (150 lines)
├── controllers/
│   └── VoiceHealthController.java (250+ lines)
└── models/
    ├── Consultation.java
    ├── FicheMedicale.java
    └── Diagnostic.java

src/main/resources/com/hospismart/hospismartdesktop/
└── VoiceHealth.fxml (80 lines) ⭐ INTERFACE

src/test/java/com/hospismart/hospismartdesktop/
└── VoiceRecognitionTest.java (100+ lines)
```

---

## ✅ Checklist de Déploiement

- [ ] Java 11+ installé et `JAVA_HOME` configuré
- [ ] Modèle Vosk (`model/`) extrait et vérifié
- [ ] Maven compilé sans erreur
- [ ] MySQL configuré avec base `hospismart`
- [ ] Table `health_summaries` créée
- [ ] Microphone testé et fonctionnel
- [ ] OpenAI API key configurée (optionnel)
- [ ] Tests unitaires réussis
- [ ] Interface VoiceHealth.fxml accessible
- [ ] Sauvegardes des bilans vérifiées

---

## 📞 Support

Pour toute question ou problème:

1. **Vérifier les logs** dans la console
2. **Consulter ce guide** (section Dépannage)
3. **Tester avec le mode démo** (sans OpenAI)
4. **Vérifier la configuration MySQL** (`HealthSummaryService`)

---

**Dernière mise à jour**: 26 avril 2026  
**Version**: 1.5 (avec amélioration du diagnostic par IA)  
**Status**: ✅ Production Ready
