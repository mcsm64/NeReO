# Traduire NeReO

*English version: [README.md](README.md)*

Merci de votre aide ! L'interface de NeReO existe en anglais et en italien ; les autres langues sont des **traductions de la communauté** : vous traduisez, l'auteur vérifie et intègre votre travail dans la version suivante, et votre nom figure dans les remerciements.

Il n'est **pas** nécessaire de connaître Android ni la programmation : on modifie un seul fichier texte.

## Une nouvelle langue

Copiez `template.txt` (toujours vide) et nommez la copie d'après votre langue, par ex. `is_IS.txt` pour l'islandais ou `nl_NL.txt` pour le néerlandais. Sur GitHub : ouvrez `template.txt`, copiez son texte, puis dans votre fork utilisez **Add file → Create new file**, nommez-le `translations/is_IS.txt`, collez, traduisez. La suite de ce guide s'applique telle quelle.

## Le fichier

Chaque langue a un fichier dans ce dossier ; pour le français, `fr_FR.txt`. Il se présente ainsi :

```
# en: Export error: %s
mnt_export_err = Erreur d'exportation : %s
```

- La ligne qui commence par `# en:` est l'original anglais. Ne la modifiez pas.
- La ligne en dessous est `clé = traduction`. **Écrivez uniquement après le `=`.** Ne modifiez pas la clé.
- **Une traduction laissée vide** affiche le texte anglais. Vous pouvez donc traduire en plusieurs fois, et une traduction partielle est acceptée.
- Écrivez les apostrophes et les guillemets normalement (`l'exportation`, `"texte"`).
- **Gardez les marqueurs exactement comme en anglais** : `%s`, `%d`, `%1$s`, `%2$s`… ainsi que les mots entre signes `%`, comme `%author%` (ne les traduisez pas). L'application les remplace par des noms, des nombres ou des dates ; un marqueur erroné peut faire planter l'application, c'est pourquoi chaque fichier est vérifié avant d'être intégré. Les marqueurs numérotés (`%1$s`, `%2$s`) peuvent changer de place dans la phrase si le français l'exige ; les autres (`%s`, `%d`) doivent rester dans le même ordre.
- `\n` signifie un retour à la ligne : gardez-le là où l'anglais en a un.
- Les lignes marquées `# CHANGED` ont un nouveau texte anglais depuis votre dernière traduction : vérifiez-les.
- Les **notes de version** (clés commençant par `rn_`, en fin de fichier) sont facultatives.

Enregistrez le fichier en **UTF-8** (tout éditeur qui gère les accents convient ; sous Windows, Notepad++ ou le Bloc-notes).

## Travailler sur GitHub, pas à pas

Il faut un compte GitHub (gratuit). Les boutons de GitHub sont en anglais : leurs noms sont indiqués **en gras**.

1. Ouvrez le fichier : <https://github.com/mcsm64/NeReO/blob/main/translations/fr_FR.txt>.
2. Cliquez sur le **crayon** (*Edit this file*). GitHub propose de créer un **fork**, c'est-à-dire votre propre copie du projet : acceptez (**Fork this repository**).
3. Traduisez dans l'éditeur du navigateur. *(Si vous préférez un éditeur sur votre ordinateur : copiez tout le texte, traduisez-le, puis recollez-le dans l'éditeur du navigateur.)*
4. Cliquez sur **Commit changes...**, écrivez une courte description (par ex. « Français : 200 premières chaînes ») et confirmez avec **Propose changes**.
5. Sur la page suivante, cliquez sur **Create pull request**, puis encore sur **Create pull request**. L'auteur la reçoit.

**Pour continuer plus tard :** ouvrez le même fichier **dans votre fork** (github.com/VOTRE-NOM/NeReO), modifiez et validez à nouveau (**Commit changes**). Tant que la *pull request* est ouverte, vos nouveaux commits s'y ajoutent automatiquement.

**Avant de reprendre après une nouvelle version de NeReO :** sur la page principale de votre fork, cliquez sur **Sync fork → Update branch** pour travailler sur le fichier le plus récent (les nouveaux textes apparaissent avec une traduction vide, ceux qui ont changé sont marqués `# CHANGED`).

## Pas de compte GitHub ?

Téléchargez le fichier, traduisez-le et envoyez-le par e-mail, ou joignez-le à une nouvelle **Issue** sur GitHub. Le résultat est le même.

## Et ensuite

L'auteur accepte votre *pull request*. Lors de la préparation de la version suivante, le fichier est vérifié (marqueurs, clés) puis converti dans le format utilisé par Android. Les textes non traduits restent en anglais. Votre nom figurera dans les remerciements : dites-nous comment vous souhaitez apparaître.

## Un conseil de vocabulaire

NeReO est un lecteur de nouvelles Usenet : gardez les termes habituels des lecteurs francophones (« groupe », « article », « fil de discussion », « suivi », « en-têtes »...). En cas de doute, le groupe fr.comp.usenet.lecteurs-de-news est le bon endroit pour demander.
