/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui

import android.content.ActivityNotFoundException
import androidx.activity.result.ActivityResultLauncher
import org.openusenet.reader.nntp.NntpLog

/**
 * 1.5.6 / 174 — APRIRE IL SELETTORE DI SISTEMA SENZA CHIUDERE L'APP.
 *
 * LA SEGNALAZIONE (Roxy, 19 ottobre 2026, box TV Beelink GT King, Android 9):
 * «Esporta configurazione» chiudeva NeReO all'istante, senza messaggio. Il log
 * crash confermava: ActivityNotFoundException su CREATE_DOCUMENT. Molti box TV
 * non hanno il selettore di file di Android (DocumentsUI), e `launch()` di un
 * ActivityResultLauncher, se nessuna app risponde all'Intent, LANCIA: nessuno
 * la intercettava, in OTTO punti (backup, ripristino, log NNTP, log crash,
 * .eml, tre in «Conserva»).
 *
 * Restituisce `false` se il selettore non c'e': il chiamante decide se avvisare
 * o usare un ripiego. Una riga nel log di diagnostica, perche' il prossimo box
 * che non ha il selettore lo dica da se'.
 *
 * Stessa famiglia di [condividiTesto] (EsportaArticolo.kt), che il caso «box TV
 * spoglio» l'aveva gia' previsto per la condivisione.
 */
fun <I> ActivityResultLauncher<I>.apriSeEsiste(input: I): Boolean =
    try {
        launch(input)
        true
    } catch (e: ActivityNotFoundException) {
        NntpLog.diag("Selettore di sistema assente su questo dispositivo: ${e.message ?: "ActivityNotFoundException"}")
        false
    }
