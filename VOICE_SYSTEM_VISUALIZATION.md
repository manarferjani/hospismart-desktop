# 🎨 VISUALISATION - SYSTÈME DE DIAGNOSTIC PAR IA

---

## 🏗️ ARCHITECTURE GLOBALE

```
┌─────────────────────────────────────────────────────────────┐
│                                                               │
│                    APPLICATION HOSPISERVE                    │
│                        Patient/Médecin                       │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  VoiceHealthController (JavaFX)                      │   │
│  │  ├─ Interface Consultation Vocale                    │   │
│  │  ├─ Gestion threads                                 │   │
│  │  └─ Affichage résultats                             │   │
│  └──────────────────────────────────────────────────────┘   │
│                         ↑ ↓                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  VoiceRecognitionService                             │   │
│  │  ├─ Capture audio microphone                         │   │
│  │  ├─ Vosk (français)                                 │   │
│  │  ├─ 10 secondes max                                 │   │
│  │  └─ Mode simulation fallback                         │   │
│  └──────────────────────────────────────────────────────┘   │
│                         ↓                                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  AIHealthSummaryService ⭐ AMÉLIORÉ                 │   │
│  │  ├─ Analyse symptômes                               │   │
│  │  ├─ Diagnostic différencié                          │   │
│  │  ├─ Évaluation gravité (scoring)                    │   │
│  │  ├─ Recommandations personnalisées                  │   │
│  │  ├─ Support OpenAI API                              │   │
│  │  └─ Mode démo (analyse locale)                       │   │
│  └──────────────────────────────────────────────────────┘   │
│                         ↓                                     │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  HealthSummaryService                                │   │
│  │  ├─ Persistance MySQL                               │   │
│  │  ├─ CRUD bilans                                     │   │
│  │  ├─ Historique patient                              │   │
│  │  └─ Table: health_summaries                          │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                        RESSOURCES                            │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  🎙️ Microphone           → VoiceRecognitionService         │
│                                                               │
│  🧠 Modèle Vosk          → model/am/final.mdl              │
│                                                               │
│  🤖 OpenAI API (opt)     → AIHealthSummaryService          │
│                                                               │
│  💾 MySQL DB             → HealthSummaryService            │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 FLUX D'EXÉCUTION COMPLET

```
┌──────────────────────────────────────────────────────────────┐
│                                                                │
│     1️⃣ PATIENT SE CONNECTE À L'APPLICATION                 │
│        Email: patient@example.com                            │
│        Mot de passe: ****                                    │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     2️⃣ NAVIGATION VERS "CONSULTATION VOCALE"               │
│        Menu Patient → 🎤 Consultation Vocale               │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     3️⃣ INTERFACE VoiceHealth.fxml S'AFFICHE                │
│        • Bouton "Démarrer Enregistrement"                   │
│        • Zones d'affichage texte et diagnostic              │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     4️⃣ PATIENT CLIQUE "DÉMARRER ENREGISTREMENT"           │
│        • VoiceRecognitionService.startListening() appelé    │
│        • Microphone s'active                                │
│        • Message: "🎤 Parlez maintenant (max 10s)"         │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     5️⃣ PATIENT DÉCRIT SES SYMPTÔMES                        │
│        Exemple:                                              │
│        "Bonjour, j'ai une fièvre depuis ce matin,           │
│         j'ai mal à la gorge et je tousse beaucoup"          │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     6️⃣ VOSK RECONNAÎT LE TEXTE                             │
│        • Capture audio (10 sec)                             │
│        • Traitement Vosk                                    │
│        • Résultat texte:                                    │
│          "J'ai une fièvre depuis ce matin, j'ai mal à       │
│           la gorge et je tousse beaucoup"                   │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     7️⃣ AFFICHAGE TEXTE RECONNU                            │
│        Zone "Texte Reconnu":                                │
│        "J'ai une fièvre depuis ce matin, j'ai mal à        │
│         la gorge et je tousse beaucoup"                     │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     8️⃣ IA ANALYSE ET GÉNÈRE DIAGNOSTIC                   │
│        AIHealthSummaryService.generateHealthSummary()      │
│                                                                │
│        Étapes:                                               │
│        a) Détecte symptômes: Fièvre ✓, Gorge ✓, Toux ✓    │
│        b) Analyse combinaison                               │
│        c) Génère diagnostic: "Infection respiratoire"      │
│        d) Évalue gravité (score)                            │
│        e) Émet recommandations                              │
│        f) Détermine urgence: 🟡 MODÉRÉE                    │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     9️⃣ BILAN COMPLET AFFICHÉ                              │
│                                                                │
│  ╔══════════════════════════════════════════════════╗      │
│  ║    BILAN DE SANTÉ - ANALYSE PAR IA              ║      │
│  ╠══════════════════════════════════════════════════╣      │
│  ║                                                  ║      │
│  ║  📋 DESCRIPTION:                                ║      │
│  ║  J'ai une fièvre depuis ce matin...            ║      │
│  ║                                                  ║      │
│  ║  🔍 SYMPTÔMES IDENTIFIÉS:                      ║      │
│  ║  • Fièvre                                       ║      │
│  ║  • Mal de gorge                                 ║      │
│  ║  • Toux                                         ║      │
│  ║                                                  ║      │
│  ║  💊 DIAGNOSTIC PROBABLE:                        ║      │
│  ║  INFECTION RESPIRATOIRE MODÉRÉE                ║      │
│  ║  • Grippe (influenza)                           ║      │
│  ║  • Pharyngite virale                            ║      │
│  ║  • Bronchite aiguë                              ║      │
│  ║                                                  ║      │
│  ║  ✅ RECOMMANDATIONS:                            ║      │
│  ║  • Mesurer température régulièrement            ║      │
│  ║  • Prendre du paracétamol si nécessaire        ║      │
│  ║  • Rester hydraté                               ║      │
│  ║  • Gargariser avec eau salée                    ║      │
│  ║  • Consulter un médecin dans 24-48h            ║      │
│  ║                                                  ║      │
│  ║  ⚠️  NIVEAU D'URGENCE:                          ║      │
│  ║  🟡 MODÉRÉE - Consultation 24-48h             ║      │
│  ║                                                  ║      │
│  ╚══════════════════════════════════════════════════╝      │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     🔟 PATIENT CLIQUE "SAUVEGARDER"                       │
│        • HealthSummaryService.saveHealthSummary()          │
│        • Sauvegarde en MySQL                               │
│        • Message de confirmation                            │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     1️⃣1️⃣ BILAN SAUVEGARDÉ EN BASE DE DONNÉES              │
│        Table: health_summaries                              │
│        Colonnes:                                            │
│        ├─ id: Auto-incrementé                              │
│        ├─ user_id: ID patient                              │
│        ├─ voice_text: Texte original                       │
│        ├─ ai_summary: Diagnostic généré                    │
│        ├─ created_at: Timestamp                            │
│        └─ updated_at: Timestamp                            │
│                                                                │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ↓
┌──────────────────────────────────────────────────────────────┐
│     1️⃣2️⃣ BILAN DISPONIBLE DANS LE PROFIL PATIENT         │
│        • Historique de tous les bilans                     │
│        • Dates et heures d'enregistrement                  │
│        • Possibilité de consulter, exporter, imprimer      │
│                                                                │
└──────────────────────────────────────────────────────────────┘
```

---

## 🧠 LOGIQUE DE DÉTECTION DE SYMPTÔMES

```
TEXTE VOCAL:
"J'ai une fièvre depuis ce matin, j'ai mal à la gorge et je tousse"

↓ ANALYSE TEXTUELLE ↓

SYMPTÔMES DÉTECTÉS:
├─ "fièvre" → Fièvre ✅
├─ "mal à la gorge" → Mal de gorge ✅
└─ "tousse" → Toux ✅

↓ COMBINAISON ANALYSÉE ↓

CONDITIONS VÉRIFIÉES:
├─ hasFever = true
├─ hasThroat = true
├─ hasCough = true
└─ hasDifficulty = false

↓ DIAGNOSTIC DÉDUIT ↓

if (hasFever && hasCough && hasThroat) {
    if (hasDifficulty) {
        return GRAVE (pneumonie)
    } else {
        return MODÉRÉE (grippe)  ← CAS ACTUEL
    }
}

RÉSULTAT:
Diagnostic: "Infection respiratoire modérée"
Causes probables:
  • Grippe (influenza)
  • Pharyngite virale
  • Bronchite aiguë
```

---

## 📊 SYSTÈME DE SCORING D'URGENCE

```
SYMPTÔME GRAVE (100 points):
├─ Difficultés respiratoires sévères
├─ Perte de conscience
├─ Convulsions
└─ Saignement
    ↓
    → SCORE >= 100 → 🔴 URGENCE (SAMU)

SYMPTÔME MODÉRÉ-GRAVE (25-30 points):
├─ Fièvre + Toux
├─ Douleur intense
└─ Nausées + Vomissements
    ↓
    → SCORE >= 40 → 🟡 MODÉRÉE (24-48h)

SYMPTÔME MODÉRÉ (10-15 points):
├─ Fièvre seule
├─ Mal de tête
└─ Nombreux symptômes (>= 4)
    ↓
    → SCORE >= 15 → 🟡 BASSE-MODÉRÉE

AUCUN SYMPTÔME SPÉCIFIQUE:
    ↓
    → SCORE < 15 → 🟢 NORMALE
```

---

## 🎯 NIVEAUX D'URGENCE

```
┌────────────────────────────────────────────────────────┐
│                                                        │
│  🔴 URGENCE ÉLEVÉE                                    │
│  ├─ Définition: Symptômes graves                      │
│  ├─ Exemples:                                         │
│  │  • Difficulté à respirer                           │
│  │  • Perte de conscience                             │
│  │  • Douleur thoracique                              │
│  │  • Convulsions                                     │
│  ├─ Score: >= 100                                     │
│  ├─ Action: 🚨 APPELER SAMU 15 IMMÉDIATEMENT        │
│  └─ Temps: IMMÉDIAT                                  │
│                                                        │
├────────────────────────────────────────────────────────┤
│                                                        │
│  🟡 URGENCE MODÉRÉE                                   │
│  ├─ Définition: Infections probables                  │
│  ├─ Exemples:                                         │
│  │  • Grippe (fièvre + toux)                         │
│  │  • Gastroentérite                                 │
│  │  • Syndrome grippal                               │
│  ├─ Score: 40-99                                     │
│  ├─ Action: 📞 Consulter un médecin                 │
│  └─ Temps: 24-48 heures                             │
│                                                        │
├────────────────────────────────────────────────────────┤
│                                                        │
│  🟡 URGENCE BASSE-MODÉRÉE                             │
│  ├─ Définition: Symptômes légers                      │
│  ├─ Exemples:                                         │
│  │  • Mal de tête                                     │
│  │  • Allergie                                        │
│  ├─ Score: 15-39                                     │
│  ├─ Action: 💊 Pharmacien ou repos                   │
│  └─ Temps: Flexible, si persistant → médecin         │
│                                                        │
├────────────────────────────────────────────────────────┤
│                                                        │
│  🟢 NORMAL                                             │
│  ├─ Définition: Symptômes peu spécifiques            │
│  ├─ Exemples:                                         │
│  │  • Pas de diagnostic clair                         │
│  ├─ Score: < 15                                      │
│  ├─ Action: 👁️ Observation attentive                │
│  └─ Temps: Appeler si aggravation                     │
│                                                        │
└────────────────────────────────────────────────────────┘
```

---

## 📦 COMPOSANTS PRINCIPAUX

```
┌────────────────────────────┐
│  VoiceHealthController     │
│  ├─ initialize()           │
│  ├─ handleStartRecording() │
│  ├─ handleStopRecording()  │
│  ├─ generateHealthSummary()│
│  ├─ handleSaveSummary()    │
│  └─ showAlert()            │
└─────────────┬──────────────┘
              │
    ┌─────────┼─────────┐
    ↓         ↓         ↓
    │         │         │
┌───────────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│  VoiceRecognition         │  │  AIHealthSummary     │  │  HealthSummary       │
│  Service                  │  │  Service ⭐ AMÉLIORÉ  │  │  Service             │
├───────────────────────────┤  ├──────────────────────┤  ├──────────────────────┤
│ startListening()          │  │ generateHealthSum()  │  │ saveHealthSummary()  │
│ stopListening()           │  │ generateAnalysis()   │  │ getPatientSum()      │
│ cleanup()                 │  │ generateDiagnosis()  │  │ getLatestSummary()   │
│ isVoskAvailable()         │  │ assessUrgency()      │  │ deleteSummary()      │
│ captureAudioWithVosk()    │  │ generateRecommend()  │  │ createTableIfNot()   │
│ simulateRecognition()     │  │ analyzeSymptoms()    │  │ getConnection()      │
└───────────────────────────┘  └──────────────────────┘  └──────────────────────┘
```

---

## 🗄️ SCHÉMA BASE DE DONNÉES

```
health_summaries TABLE:
┌──────────────────────────────────────┐
│ id (PK)                              │
│ user_id (FK → users.id)              │
│ voice_text (LONGTEXT)                │
│ ai_summary (LONGTEXT)                │
│ created_at (DATETIME)                │
│ updated_at (DATETIME)                │
│ INDEX (user_id)                      │
│ FOREIGN KEY (user_id)                │
└──────────────────────────────────────┘

Exemple de données:
┌─────┬─────────┬──────────────────────┬──────────────────┐
│ id  │ user_id │ voice_text           │ ai_summary       │
├─────┼─────────┼──────────────────────┼──────────────────┤
│ 1   │ 5       │ "J'ai mal à la tête" │ "Migraine pro.." │
│ 2   │ 5       │ "Fièvre et toux"     │ "Grippe probab.."│
└─────┴─────────┴──────────────────────┴──────────────────┘
```

---

## 🎬 CAS D'USAGE PRINCIPAL

```
ACTEURS:
├─ Patient (utilisateur final)
├─ Médecin (consultation)
└─ Système IA (diagnostic automatique)

SCENARIO PRINCIPAL:
1. Patient se connecte
2. Accède à "Consultation Vocale"
3. Enregistre description vocale (10 sec)
4. Vosk reconnaît le texte
5. IA génère diagnostic
6. Patient voit le bilan formaté
7. Patient sauvegarde le bilan
8. Médecin peut consulter le bilan

RÉSULTAT:
✅ Diagnostic rapide
✅ Recommandations claires
✅ Historique persiste
✅ Prêt pour suivi médical
```

---

**Voilà! Le système complet de reconnaissance vocale et diagnostic par IA est maintenant documenté visuellement et techniquement! 🎉**
