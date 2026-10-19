# Dabou Cinema Launcher

Launcher Android natif en Java, interface française. Android 8.0 ou plus récent.
Version 2 inspirée de l'ambiance rouge/noir de la vidéo, pas une reproduction exacte.
Voir aussi `CINEMA.md` pour les fonctions et limites de cette nouvelle version.

## Fonctionnalités

- Accueil rouge/noir avec grande horloge, jour et date.
- Applications accessibles dans le profil Android courant, tri alphabétique et recherche.
- Appui long sur une application : favoris et informations système.
- Nom de l'accueil, image personnalisable, trois couleurs et grille de 3 à 5 colonnes.
- Panneaux d'informations appareil, calendrier et lecteur de fichiers audio locaux.
- Choix comme application d'accueil via la boîte de dialogue Android.
- Aucun accès Internet, publicité, compte ou bibliothèque applicative tierce.
- Préférences conservées uniquement sur l'appareil.

Les applications des profils professionnels et les widgets Android ne sont pas pris en charge.
Cet accueil utilise son propre fond graphique, pas le fond d'écran système.
Pas d'image John Wick incluse : toucher « TON UNIVERS. » pour choisir ton image.

## Obtenir l'APK avec GitHub (pas besoin d'Android Studio)

1. Décompresser le ZIP. Créer un dépôt GitHub, par exemple `dabou-launcher`.
2. Envoyer **le contenu** du dossier `dabou-launcher` à la racine du dépôt.
   `settings.gradle`, `app/` et `.github/` doivent être directement à la racine.
   Attention : `.github` est un dossier caché sur certains appareils.
   Avec le site GitHub, utiliser **Add file > Upload files**.
   Si le dossier caché n'est pas importé, créer le fichier
   `.github/workflows/android.yml` avec **Add file > Create new file**
   et y coller le contenu du fichier fourni.
3. Ouvrir **Actions**, activer les workflows si GitHub le demande, puis choisir
   **Compiler APK > Run workflow > Run workflow**. Le workflow doit être
   présent dans la branche par défaut pour être lancé manuellement.
4. Attendre la coche verte. Ouvrir cette exécution, puis télécharger
   **Dabou-Launcher-APK** dans la section **Artifacts** (connexion GitHub nécessaire).
5. Décompresser l'archive téléchargée : elle contient `app-debug.apk`.
6. Copier l'APK sur le téléphone et l'ouvrir. Autoriser si nécessaire
   l'installation depuis cette source dans les réglages Android.
7. Ouvrir **Dabou Cinema > Réglages > Définir comme accueil**.

Pour revenir au launcher précédent : **Paramètres Android > Applications >
Applications par défaut > Application d'accueil** (le chemin dépend du fabricant).
Garder le launcher d'origine installé.

## Compilation locale facultative

JDK 17, Gradle 8.9 et Android SDK plateforme 35 requis :

```sh
gradle assembleDebug lintDebug
```

Le projet ne contient pas de wrapper Gradle ni de fichier `local.properties`.
GitHub Actions installe Gradle et le SDK. Une connexion Internet est nécessaire
sur la machine de compilation pour télécharger les outils et dépendances.

## Signature et limites

Le workflow produit un **APK debug signé automatiquement**, installable pour test
personnel, pas une version de publication Play Store.
La clé debug peut changer entre exécutions GitHub : en cas de signature incompatible,
désinstaller l'ancienne version avant l'installation (cela efface ses préférences).
Pour des mises à jour durables, configurer une signature release avec une clé privée
conservée dans GitHub Secrets, jamais dans le dépôt.

Le code source a été préparé sans SDK Android dans l'environnement de génération :
aucun APK n'a été compilé ou testé sur appareil à ce stade. GitHub Actions exécutera
la compilation et Android Lint ; son résultat est la vérification de référence.
L'accès à Actions dépend des droits, quotas et réglages de votre compte GitHub.
