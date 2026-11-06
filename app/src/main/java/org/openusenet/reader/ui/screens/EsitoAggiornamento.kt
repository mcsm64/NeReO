/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui.screens

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CancellationException
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.ui.playDing

/**
 * 1.6.1 — un aggiornamento avviato dall'utente, dall'inizio al messaggio finale.
 *
 * Erano tre copie, in GroupsScreen e in ThreePaneHome, e non uguali: dai pannelli
 * un errore non si vedeva affatto. [inCorso] si spegne in `finally` e PRIMA del
 * messaggio: tenerlo acceso finche' la snackbar e' a video bloccava il pulsante.
 */
suspend fun aggiornaConEsito(
    repo: NewsRepository,
    ctx: Context,
    snackbar: SnackbarHostState,
    inCorso: (Boolean) -> Unit,
    azione: suspend () -> Int,
) {
    inCorso(true)
    val esito = try {
        runCatching { azione() }
    } finally {
        inCorso(false)
    }
    esito.onSuccess { n ->
        if (AppSettings.dingOnReply && repo.lastSyncNewToMe > 0) playDing(ctx)
        val fuori = repo.lastSyncNonAggiornati
        snackbar.showSnackbar(
            testoEsitoSync(ctx, n, repo.lastSyncFiltrati, fuori),
            duration = if (fuori.isEmpty()) SnackbarDuration.Short else SnackbarDuration.Long,
        )
    }.onFailure {
        if (it is CancellationException) throw it
        snackbar.showSnackbar(ctx.getString(R.string.msg_error, it.message ?: ""))
    }
}
