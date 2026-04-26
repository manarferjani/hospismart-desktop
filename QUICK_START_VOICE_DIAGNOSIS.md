# 🚀 QUICK START - RECONNAISSANCE VOCALE & DIAGNOSTIC IA

**Durée**: 5 minutes pour démarrer  
**Complexité**: ⭐ Facile  
**Version**: 1.5

---

## 📝 Prérequis Rapides

✅ Java 11+  
✅ Maven  
✅ Microphone  
✅ MySQL (optionnel pour tests)  
✅ Modèle Vosk dans `model/`  

---

## ⚡ 3 Étapes pour Démarrer

### Étape 1: Vérifier le Modèle Vosk

```bash
# Depuis la racine du projet, vérifier que ces fichiers existent:
ls model/am/final.mdl
ls model/conf/mfcc.conf
ls model/graph/Gr.fst
# Tous doivent exister ✅
```

### Étape 2: Compiler

```bash
cd hospismart-desktop
.\mvnw clean compile
```

### Étape 3: Exécuter l'Application

```bash
# Via IDE JavaFX (VS Code, IntelliJ, Eclipse):
# - Ouvrir le projet
# - Exécuter com.hospismart.hospismartdesktop.main.Main
# - Ou: .\mvnw javafx:run
```

---

## 🎯 Utilisation

### 1️⃣ Se Connecter Comme Patient
```
Email: patient@example.com
Mot de passe: patient123
```

### 2️⃣ Accéder à "Consultation Vocale"
```
Menu Patient → 🎤 Consultation Vocale
```

### 3️⃣ Enregistrer Votre Voix
```
1. Cliquer [🎤 Démarrer Enregistrement]
2. Parler pendant max 10 secondes
3. Exemple: "J'ai mal à la tête et de la fièvre depuis ce matin"
4. Cliquer [⏹️ Arrêter] ou attendre 10 secondes
```

### 4️⃣ Voir le Diagnostic
```
L'IA affiche automatiquement:
• Symptômes identifiés
• Diagnostic probable
• Recommandations
• Niveau d'urgence
```

### 5️⃣ Sauvegarder
```
Cliquer [💾 Sauvegarder]
✅ Bilan sauvegardé en base de données
```

---

## 🧪 Test Rapide (Sans Voix)

Si vous n'avez pas de microphone, tester le diagnostic:

```bash
# Exécuter le test de diagnostic
.\mvnw test -Dtest=AIHealthDiagnosisTest
```

Cela affichera 5 cas de diagnostic différents ✅

---

## 🔧 Configuration Optionnelle

### OpenAI API (Pour meilleur diagnostic)

```powershell
# Windows PowerShell
$env:OPENAI_API_KEY = "sk-YOUR_API_KEY"
```

```bash
# Linux/Mac
export OPENAI_API_KEY="sk-YOUR_API_KEY"
```

---

## 🎤 Exemples de Symptômes Reconnus

| Symptôme | Phrase d'exemple |
|----------|-----------------|
| Fièvre | "J'ai de la fièvre" |
| Toux | "Je tousse beaucoup" |
| Mal de gorge | "J'ai mal à la gorge" |
| Mal de tête | "J'ai une migraine" |
| Fatigue | "Je suis très fatigué" |
| Nausée | "J'ai des nausées" |
| Diarrhée | "J'ai la diarrhée" |
| Allergie | "Je suis allergique" |
| Respiration difficile | "J'ai du mal à respirer" |

---

## 🩺 Diagnostics Possibles

### 🔴 URGENCE (Appeler SAMU 15)
- Difficultés respiratoires sévères
- Perte de conscience
- Convulsions

### 🟡 MODÉRÉE (Consulter 24-48h)
- Infection respiratoire (fièvre + toux + gorge)
- Gastroentérite (nausées + diarrhée)
- Syndrome grippal

### 🟢 BASSE (Observation)
- Céphalée/Migraine
- Allergie
- Symptômes légers

---

## 📊 Flux Complet

```
┌─────────────────────────────────────────┐
│  PATIENT SE CONNECTE                    │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│  VA À "CONSULTATION VOCALE"             │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│  CLIQUE "DÉMARRER ENREGISTREMENT"       │
│  - Microphone s'active                  │
│  - Message: "Parlez maintenant"         │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│  PATIENT PARLE 10 SECONDES               │
│  "J'ai mal à la tête et de la fièvre"  │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│  VOSK RECONNAÎT LE TEXTE                │
│  Résultat: "J'ai mal à la tête et de   │
│            la fièvre"                   │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│  IA ANALYSE ET GÉNÈRE DIAGNOSTIC        │
│  • Symptômes: Mal de tête, Fièvre      │
│  • Diagnostic: Syndrome grippal         │
│  • Urgence: 🟡 MODÉRÉE                 │
│  • Recommandations: Repos, hydratation  │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│  AFFICHE LE BILAN COMPLET               │
│  • Texte reconnu: "J'ai mal à la..."   │
│  • Bilan détaillé avec recommandations  │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│  PATIENT CLIQUE "SAUVEGARDER"           │
│  ✅ Bilan sauvegardé en base de données│
└─────────────────────────────────────────┘
```

---

## 🐛 Problèmes Courants

### ❌ "Modèle Vosk non trouvé"
```bash
# Solution: Vérifier le dossier model/
ls -R model/
# Doit afficher: am/, conf/, graph/, ivector/
```

### ❌ "Aucun texte reconnu"
```bash
# Solution: 
# 1. Parler plus fort et clairement
# 2. Vérifier le microphone avec: Volume dans Windows
# 3. Mode démo s'active automatiquement
```

### ❌ "Impossible de sauvegarder"
```bash
# Solution: 
# 1. Vérifier que MySQL est lancé
# 2. Vérifier les credentials dans HealthSummaryService
# 3. Créer la base: CREATE DATABASE hospismart;
```

---

## 🎯 Cas d'Usage Clés

### 👨‍⚕️ Médecin Veut Tester
1. Se connecter comme médecin
2. Voir l'historique des bilans patients
3. Consulter les diagnostics générés

### 👨‍🦲 Patient Veut Obtenir Diagnostic
1. Se connecter comme patient
2. Aller à "Consultation Vocale"
3. Décrire les symptômes
4. Obtenir le bilan instantanément
5. Sauvegarder pour suivi

### 🤖 Développeur Veut Améliorer IA
1. Éditer `AIHealthSummaryService.java`
2. Améliorer les diagnostics
3. Ajouter de nouveaux symptômes
4. Tester avec `AIHealthDiagnosisTest.java`

---

## 📚 Ressources Utiles

| Ressource | Localisation |
|-----------|--------------|
| Guide Complet | `VOICE_DIAGNOSIS_GUIDE.md` |
| Améliorations | `VOICE_DIAGNOSIS_IMPROVEMENTS.md` |
| Service IA | `src/main/java/.../AIHealthSummaryService.java` |
| Service Voix | `src/main/java/.../VoiceRecognitionService.java` |
| Test IA | `src/test/java/.../AIHealthDiagnosisTest.java` |
| Interface | `src/main/resources/.../VoiceHealth.fxml` |

---

## ✅ Checklist Démarrage Rapide

- [ ] Vosk dans `model/` ✅
- [ ] Java 11+ installé ✅
- [ ] Maven compilé sans erreur ✅
- [ ] Microphone branché et testé ✅
- [ ] MySQL configuré (optionnel) ✅
- [ ] Application lancée ✅
- [ ] Patient connecté ✅
- [ ] Interface Consultation Vocale affichée ✅
- [ ] Test microphone réussi ✅
- [ ] Diagnostic généré ✅
- [ ] Bilan sauvegardé ✅

---

## 🎉 Vous êtes Prêt!

Vous pouvez maintenant:
- ✅ Utiliser la reconnaissance vocale en français
- ✅ Générer des diagnostics par IA
- ✅ Tester différents scénarios
- ✅ Sauvegarder les bilans

**Pour plus de détails**: Voir `VOICE_DIAGNOSIS_GUIDE.md`

---

**Version**: 1.5  
**Statut**: ✅ Opérationnel  
**Support**: Voir les fichiers documentation
