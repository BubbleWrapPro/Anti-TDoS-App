# 📱 Anti-TDoS-App  
**Protégez votre appareil contre les attaques de type Telephony Denial of Service (TDoS).**  

## 🚀 Description  
**Anti-TDoS-App** est une application Android conçue pour détecter et bloquer les attaques TDoS (Telephony Denial of Service). Ces attaques visent à saturer un téléphone en appels entrants, rendant impossible toute communication légitime.  

## 🎯 Fonctionnalités  
✅ Détection des appels suspects en masse  
✅ Blocage automatique des appels indésirables  
✅ Journal des appels bloqués  
✅ Configuration personnalisable via un fichier JSON  

## 📂 Contenu du dépôt  
- **📂 `Les fichier .kt`** : Code source Kotlin des classes principales :  
  - `CallReceiver.kt` : Gestion des appels entrants  
  - `MainActivity.kt` : Interface principale de l'application  
- **📂 `app-debug.apk`** : APK de l'application (nécessaire pour l'installation en attendant la sortie sur le Play Store)  
- **📂 `config.json`** *(optionnel mais recommandé)* : Fichier de configuration personnalisable  

## 🛠️ Installation  
1. **Télécharger et installer l'APK**  
   - Récupérez l'APK depuis ce dépôt  
   - Installez-le sur votre appareil Android  
   - Activez les permissions requises  

2. **(Optionnel) Configurer le fichier JSON**  
   - Placez `config.json` dans le répertoire de l'application  
   - Modifiez-le selon vos préférences  

## ⚠️ Permissions requises  
L'application nécessite les permissions suivantes :  
📞 *Gérer les appels* (détection et blocage des appels suspects)  

## 🏗️ Améliorations futures  
🔹 Interface plus intuitive  
🔹 Mode apprentissage pour affiner le filtrage des appels  
🔹 Journal exportable des appels bloqués  

## 📜 Licence  
Aucune licence ne protège ce projet, vous êtes libre d'utiliser ce code et de l'améliorer si besoin. N'hésitez pas à me créditer pour toute utilisation dans un cadre publique.
