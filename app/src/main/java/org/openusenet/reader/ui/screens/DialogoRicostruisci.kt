/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6 (sondaggio sui gruppi) — «RICOSTRUISCI DISCUSSIONE», la parte che si vede,
 * in comune fra elenco, pannelli e lettura.
 *
 * ANNULLARE E' CHIUDERE: il lavoro vive in un LaunchedEffect del dialogo, e
 * uscendo dalla composizione si interrompe da solo. Cio' che e' gia' arrivato
 * resta (vedi `ricostruisciDiscussione`). L'esito e' un Toast perche' il dialogo
 * nasce in schermate diverse, e in lettura non c'e' sempre uno snackbar.
 */
package org.openusenet.reader.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openusenet.reader.R
import org.openusenet.reader.data.Article
import org.openusenet.reader.data.EsitoRicostruzione
import org.openusenet.reader.data.NewsRepository

/** L'esito a parole: «trovati X, non disponibili Y sul server Z», piu' cio' che serve. */
private fun testoEsito(ctx: Context, e: EsitoRicostruzione): String = buildString {
    append(ctx.getString(R.string.rb_esito, e.trovati, e.nonDisponibili, e.server))
    if (e.scartati > 0) append(' ').append(ctx.getString(R.string.rb_scartati, e.scartati))
    if (e.restanti > 0) append(' ').append(ctx.getString(R.string.rb_restanti, e.restanti))
    e.errore?.let { append(' ').append(ctx.getString(R.string.rb_errore, it)) }
}

/**
 * [membri]: i messaggi da cui partire. Basta anche uno solo: le sue References
 * nominano gia' i suoi antenati.
 */
@Composable
fun DialogoRicostruisci(repo: NewsRepository, membri: List<Article>, onChiudi: () -> Unit) {
    val ctx = LocalContext.current
    var fatti by remember { mutableIntStateOf(0) }
    var totale by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val esito = repo.ricostruisciDiscussione(membri) { f, t -> fatti = f; totale = t }
        Toast.makeText(ctx, testoEsito(ctx, esito), Toast.LENGTH_LONG).show()
        onChiudi()
    }
    AlertDialog(
        onDismissRequest = onChiudi,
        title = { Text(stringResource(R.string.rb_title)) },
        text = {
            Column {
                Text(stringResource(R.string.rb_avanzamento, fatti, totale))
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onChiudi) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

/**
 * Per la lettura: l'azione se il messaggio aperto ha il padre mancante, altrimenti
 * null. Disegna da se' il dialogo, cosi' ai tre riquadri basta una riga.
 */
@Composable
fun ricostruisciDaLettura(repo: NewsRepository, articolo: Article?): (() -> Unit)? {
    var aperto by remember(articolo?.id) { mutableStateOf(false) }
    val manca by produceState(false, articolo?.id, articolo?.references, aperto) {
        value = articolo != null && repo.padreMancante(articolo)
    }
    if (aperto && articolo != null) {
        DialogoRicostruisci(repo, listOf(articolo)) { aperto = false }
    }
    return if (manca) ({ aperto = true }) else null
}
