# Translating NeReO

*Version française : [README_fr.md](README_fr.md)*

Thank you for helping! NeReO's interface is in English and Italian; other languages are **community translations**: you translate, the author checks and includes your work in the next release, and you are credited.

You do **not** need to know Android or programming. You edit one plain text file.

## A new language

Copy `template.txt` (it is always empty) and name the copy after your language, e.g. `is_IS.txt` for Icelandic or `nl_NL.txt` for Dutch. On GitHub: open `template.txt`, copy its text, then in your fork use **Add file → Create new file**, name it `translations/is_IS.txt`, paste, translate. The rest of this guide applies unchanged.

## The file

Each language has one file in this folder, for example `fr_FR.txt` for French. It looks like this:

```
# en: Export error: %s
mnt_export_err = Erreur d'exportation : %s
```

- The line starting with `# en:` is the English original. Do not change it.
- The line below it is `key = translation`. **Write only after the `=`.** Do not change the key.
- **Leave a translation empty** and NeReO shows the English text. So you can translate in several sessions, and a partial translation is fine.
- Write apostrophes and quotes normally (`l'exportation`, `"texte"`).
- **Keep placeholders exactly as in English**: `%s`, `%d`, `%1$s`, `%2$s`... and words between `%` signs such as `%author%` (do not translate those). They are replaced by names, numbers or dates when the app runs; a wrong placeholder can crash the app, so every file is checked before it goes in.
- `\n` means a new line: keep it where the English has it.
- Lines marked `# CHANGED` have a new English text since your last translation: please check them.
- The **release notes** (keys starting with `rn_`, at the end of the file) are optional.

Save the file as **UTF-8** (any editor that supports accents does; on Windows, Notepad++ or the Windows Notepad both work).

## Working on GitHub, step by step

You need a free GitHub account.

1. Open the file, e.g. <https://github.com/mcsm64/NeReO/blob/main/translations/fr_FR.txt>.
2. Click the **pencil** (Edit this file). GitHub offers to **fork** the project, that is, to make your own copy: accept.
3. Translate in the browser editor. *(Or, if you prefer an editor on your computer: copy the whole text, translate it there, then paste it back into the browser editor.)*
4. Click **Commit changes...**, write a short description (e.g. "French: first 200 strings") and confirm with **Propose changes**.
5. On the next page click **Create pull request**, then **Create pull request** again. The author receives it.

**To continue later:** open the same file **in your fork** (github.com/YOUR-NAME/NeReO), edit and commit again. While the pull request is open, your new commits are added to it automatically.

**Before starting again after a new NeReO release:** on your fork's main page click **Sync fork → Update branch**, so you work on the latest file (new texts appear with an empty translation, changed ones are marked `# CHANGED`).

## No GitHub account?

Download the file, translate it, and send it by e-mail or attach it to a new **Issue** on GitHub. It ends up in the same place.

## What happens next

The author merges your pull request. When the next version is prepared, the file is checked (placeholders, keys) and converted into the format Android uses. Untranslated texts stay in English. Your name goes in the credits: tell us how you want to appear.
