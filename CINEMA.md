# Version cinéma : utilisation et limites

## Personnaliser

Toucher « TON UNIVERS. » ou **Réglages > Choisir une image**.
Un PNG détouré avec fond transparent donne le résultat le plus proche de la vidéo.
Le projet ne contient ni portrait John Wick ni musique sous licence.
Il ne retire pas l'arrière-plan des photos et ne corrige pas leur orientation EXIF.
Choisis une image dont tu as le droit d'usage.

**Réglages** permet de changer le nom, la couleur (rouge, ambre ou violet),
la grille de 3 à 5 colonnes et le launcher par défaut.
L'interface est défilante pour les petits écrans.

## Panneaux

- **INFOS** : modèle, version Android, RAM disponible/totale, stockage libre et batterie.
  Les valeurs sont relues quand l'accueil revient au premier plan ou quand on ouvre INFOS.
- **CALENDRIER** : calendrier mensuel réel avec précédent/suivant et jour courant marqué.
  « Ouvrir l'agenda » lance une application calendrier compatible.
  Aucun accès aux événements privés, aucune permission calendrier demandée.
- **MUSIQUE** : choisir un fichier audio local, puis LIRE ou PAUSE.
  La lecture s'arrête quand l'accueil n'est plus visible (application ouverte,
  écran verrouillé, sélecteur de fichiers, etc.). Pas de service en arrière-plan.
  Formats pris en charge selon l'appareil. Pas de streaming Spotify ou YouTube,
  pas de contrôle des autres lecteurs.

Les fichiers sont sélectionnés par le sélecteur Android, sans accès global aux médias.
Les autorisations d'accès sont conservées lorsque le fournisseur de fichiers le permet.
Les favoris se règlent par appui long sur une icône.
Les icônes restent celles des applications installées, pas les pictogrammes blancs
de la vidéo. Pas de widgets tiers, dossiers ou météo connectée dans cette version.

## GitHub et installation

Suivre les étapes du README. La configuration GitHub Actions compile cette version
cinéma et publie **Dabou-Launcher-APK** contenant `app-debug.apk`.
Le même identifiant d'application est utilisé que dans le premier ZIP.
La signature debug peut changer entre exécutions GitHub : une désinstallation
peut être nécessaire avant réinstallation, avec perte des préférences.
Le nouveau thème utilise des préférences séparées de celles de l'ancien.

Le fichier `MainActivity.java` conserve l'ancienne interface comme référence de code,
mais seule `CinemaActivity` est déclarée comme accueil et ouverte à l'exécution.

## État des vérifications

Projet source préparé sans SDK Android disponible dans l'environnement de génération.
Aucun APK compilé et aucun test sur téléphone effectué ici.
La compilation et Android Lint sont exécutés par GitHub Actions.
Tester sur téléphone avant de choisir ce launcher comme accueil principal.
Conserver le launcher système installé et accessible dans les paramètres Android.
