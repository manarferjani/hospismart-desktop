# 🎤 GUIDE DE TESTS - RECONNAISSANCE VOCALE

## 🧪 Tests Préliminaires (Pas de Microphone Nécessaire)

### Test 1: Disponibilité Vosk
```bash
# Compiler
.\mvnw clean compile -DskipTests=false

# Résultat attendu
# ✅ Vosk est disponible
# ✅ Modèle français trouvé
# ✅ Prêt pour reconnaissance vocale
```

### Test 2: Génération de Bilan IA
```bash
# Le service IA doit générer un bilan avec du texte de test
# Mode démo active si API OpenAI non configurée

# Résultat attendu
# ═════════════════════════════════════════
# ║ BILAN DE SANTÉ - ANALYSE BASÉE SUR IA ║
# ═════════════════════════════════════════
# 🔍 SYMPTÔMES IDENTIFIÉS:
# • Maux de tête / Migraine
# • Fatigue
# • Fièvre
# ...
```

## 🎯 Tests Fonctionnels Complets (Avec Microphone)

### Prérequis
- ✅ Microphone branché et testé dans les paramètres Windows
- ✅ Application compilée et prête
- ✅ MySQL lancé avec la base 'hospismart'

### Scénario de Test 1: Reconnaissance Simple
```
1. Lancer l'application
2. Se connecter avec un compte patient
3. Cliquer sur "🎤 Consultation Vocale - Bilan de Santé par IA"
4. Clicker "🎤 Démarrer Enregistrement"
5. Parler simplement: "J'ai mal à la tête"
6. Attendre 3-5 secondes
7. Clicker "⏹️ Arrêter" (ou attendre 10 secondes)

✅ Vérifier:
- Texte reconnu s'affiche dans "Texte Reconnu"
- Bilan généré s'affiche dans le champ IA
- Pas d'erreur dans la console
```

### Scénario de Test 2: Description Complète
```
1. Clicker "🎤 Démarrer Enregistrement"
2. Parler pendant 5-10 secondes:
   "Bonjour, j'ai mal à la tête depuis ce matin, 
    j'ai aussi de la fièvre et une légère toux.
    Je me sens très fatigué."
3. Clicker "⏹️ Arrêter"

✅ Vérifier:
- Texte complet reconnu
- Symptômes identifiés dans le bilan
- Recommandations appropriées
```

### Scénario de Test 3: Sauvegarde en Base de Données
```
1. Générer un bilan (voir Test 1 ou 2)
2. Clicker "💾 Sauvegarder"
3. Message "Succès" doit apparaître

✅ Vérifier en Base de Données:
SELECT * FROM health_summaries 
WHERE user_id = [patient_id] 
ORDER BY created_at DESC LIMIT 1;

Doit contenir:
- voice_text: le texte reconnu
- ai_summary: le bilan généré
- created_at: timestamp actuel
```

### Scénario de Test 4: Gestion des Erreurs
```
Test 4A: Microphone Désactivé
- Désactiver le microphone dans les paramètres Windows
- Clicker "🎤 Démarrer Enregistrement"
✅ Doit afficher: "Microphone non disponible"

Test 4B: Aucune Parole
- Lancer l'enregistrement sans parler
- Attendre 10 secondes
✅ Doit afficher: "Aucun texte reconnu"

Test 4C: Bruit de Fond Excessif
- Enregistrer avec bruit fort (TV, etc.)
✅ Peut donner un résultat incorrect (normal avec bruit)
```

## 📊 Checklist Avant Déploiement

### Compilation
- [ ] `.\mvnw clean compile` sans erreurs
- [ ] Pas de warnings non-critiques
- [ ] Toutes les dépendances résolues

### Vosk et Audio
- [ ] Dossier `vosk-model-small-fr-0.22` existe
- [ ] Structure correcte (am/, conf/, graph/)
- [ ] Microphone détecté par Windows
- [ ] Format audio 16kHz supporté

### Interface Utilisateur
- [ ] Boutons activé/désactivé correctement
- [ ] Labels de statut s'actualisent
- [ ] TextArea affiche le texte reconnu
- [ ] TextArea affiche le bilan

### Base de Données
- [ ] MySQL lancé
- [ ] Base 'hospismart' existe
- [ ] Table 'health_summaries' créée
- [ ] Foreign key correcte avec 'users'

### Logs et Débogage
- [ ] Console affiche [Voice] messages
- [ ] Console affiche [AI] messages
- [ ] Console affiche [VoiceHealth] messages
- [ ] Pas d'exceptions non-capturées

## 🐛 Débogage

### Éditer les Niveaux de Log

Dans `VoiceRecognitionService`:
```java
// Augmenter le niveau de log
LibVosk.setLogLevel(LogLevel.DEBUG);  // Plus verbeux
```

### Vérifier le Chemin du Modèle

```bash
# Affiche le chemin cherché
$modelPath = ".\vosk-model-small-fr-0.22"
Test-Path $modelPath
Get-Item $modelPath -Recurse
```

### Tester le Microphone

```bash
# Test rapide audio Windows
Get-AudioDevice -List
```

## 📈 Métriques de Performance

| Opération | Temps Attendu | Seuil Alerte |
|-----------|---------------|------------|
| Initialisation Vosk | < 2s | > 5s |
| Reconnaissance (5s parole) | 5-7s | > 10s |
| Génération IA (mode démo) | < 1s | > 3s |
| Génération IA (OpenAI) | 2-5s | > 10s |
| Sauvegarde BD | < 1s | > 2s |

## 🔍 Vérification des Logs

### Logs Normaux (Succès)
```
[VoiceHealth] Initialisation du contrôleur vocal
[Voice] Initialisation Vosk...
[Voice] ✅ Modèle trouvé
[Voice] ✅ Microphone activé
[Voice] 🎤 Parlez maintenant
[Voice] 📝 Phrase 1: j'ai mal à la tête
[Voice] ✅ Enregistrement terminé
[AI] Génération du bilan IA
[VoiceHealth] ✅ Bilan sauvegardé
```

### Logs d'Erreur (Diagnostic)
```
[Voice] ❌ Modèle Vosk non trouvé
  → Vérifier le chemin du modèle

[Voice] ❌ Microphone non disponible
  → Vérifier les paramètres audio Windows

[Voice] ⚠️ Format audio non supporté
  → Vérifier le driver audio

[HealthSummary] ❌ Erreur sauvegarde bilan
  → Vérifier MySQL et la base de données
```

## 📱 Cas Limites à Tester

1. **Entrée vide**: Ne rien dire pendant 10 secondes
2. **Entrée courte**: "Oui" ou "Non" uniquement
3. **Entrée longue**: Parler jusqu'à 10 secondes complet
4. **Bruits**: Parler avec bruit de fond
5. **Langue mélangée**: Français avec mots anglais
6. **Pauses**: Longues pauses entre les phrases
7. **Vitesse**: Parler très rapidement ou lentement

## 🎬 Enregistrement de Session de Test

```bash
# Rediriger la sortie console
.\mvnw clean compile > test_output.log 2>&1

# Vérifier le résultat
Get-Content test_output.log
```

---

**Astuce**: Tester d'abord les scénarios simples avant les complexes!
