/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.backup

import android.content.Context
import java.io.File

/**
 * 1.5.6 / 174 — IL BACKUP QUANDO IL DISPOSITIVO NON HA IL SELETTORE DI FILE.
 *
 * Sul box TV di Roxy (Android 9) il selettore di sistema non esiste: senza un
 * ripiego la configurazione non si poteva ne' esportare ne' importare (piano B,
 * scelto da Marco il 19 ottobre).
 *
 * DOVE: la cartella dell'app sulla memoria condivisa,
 * `Android/data/org.openusenet.reader/files/` ([Context.getExternalFilesDir]).
 * Nessun permesso, su nessuna versione di Android. Su Android 8-10 un gestore
 * file o il cavo USB ci arrivano; dalla 11 i gestori file non la vedono piu',
 * ma su quei dispositivi di solito il selettore c'e' e questo ripiego non
 * scatta.
 *
 * ⚠️ PERCHE' NON LA CARTELLA Download: su Android 9 servirebbe il permesso
 * WRITE_EXTERNAL_STORAGE (MediaStore.Downloads esiste solo dalla 10). Un
 * permesso nuovo per un ripiego era il piano C, scartato.
 *
 * ⚠️ LIMITE, detto all'utente nel messaggio: la cartella dell'app si cancella
 * disinstallando NeReO. Il backup va copiato altrove.
 */
object BackupLocale {

    fun cartella(ctx: Context): File? = ctx.getExternalFilesDir(null)

    /** Scrive [contenuto] con un nome libero vicino a [nomeProposto]; restituisce il file. */
    fun salva(ctx: Context, nomeProposto: String, contenuto: String): File {
        val dir = cartella(ctx) ?: error("memoria condivisa non disponibile")
        if (!dir.exists() && !dir.mkdirs()) error("impossibile creare ${dir.absolutePath}")
        val esistenti = dir.list()?.toSet() ?: emptySet()
        val file = File(dir, nomeLibero(nomeProposto, esistenti))
        file.writeText(contenuto, Charsets.UTF_8)
        return file
    }

    /** I backup presenti, il piu' recente per primo. */
    fun elenco(ctx: Context): List<File> =
        cartella(ctx)?.listFiles()
            ?.filter { it.isFile && eNomeBackup(it.name) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
