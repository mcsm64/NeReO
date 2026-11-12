/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * «SEGNA TUTTO LETTO», IN UN POSTO SOLO — AZIONE E ANNULLA.
 *
 * PERCHE' ESISTE QUESTO FILE (1.4.5, §A11).
 * Dave Royal, 28 settembre:
 *
 *   «I'd still like "Mark all as read" moved back to the menu bar from the
 *    three-dot menu. After that change "Mark all as read" requires two taps
 *    versus one previously. (Panel mode.)»
 *
 * E' LA STESSA PERSONA CHE AVEVA CHIESTO LA MODIFICA, e non e' una
 * contraddizione: le due segnalazioni parlano di due cose diverse. La prima
 * riguardava la POSIZIONE — un'azione rara e con conseguenze appoggiata a una
 * frequente e innocua (il filtro non-letti), quindi premuta per sbaglio. La
 * seconda riguarda il COSTO IN TOCCHI. Siamo stati noi a confonderle in una.
 *
 * IL DIFETTO VERO ERA CHE LO STESSO PROBLEMA HA AVUTO DUE CURE DIVERSE.
 * Nella 1.4.1 (build 108):
 *   - sul TELEFONO l'icona e' stata SPOSTATA, scambiata con «Aggiorna»: un
 *     tocco, e lontana dal filtro;
 *   - nei PANNELLI e' stata TOLTA, perche' li' la voce c'era gia' anche nel
 *     menu ed era un doppione: due tocchi.
 * Nessuna delle due e' sbagliata presa da sola. Ma la cura del telefono
 * risolveva l'adiacenza SENZA costare un tocco, ed e' quella che nei pannelli
 * non e' mai stata provata.
 *
 * *Due copie che divergono, prima o poi si notano. Due copie curate in modo
 * DIVERSO sembrano entrambe corrette guardando il codice, e si scoprono solo
 * quando qualcuno conta i tocchi.* Per questo l'azione ora vive qui: la
 * prossima volta che si sposta, si sposta una volta sola.
 *
 * COSA PORTA CON SE', e non e' ovvio: lo snackbar con ANNULLA. «Segna tutto
 * letto» cancella uno stato che l'utente non puo' ricostruire a mano — quali
 * messaggi non aveva ancora letto — quindi l'annulla non e' una cortesia, e'
 * la ragione per cui l'azione si puo' tenere in barra. Era ricopiato parola per
 * parola in due punti; ricopiarlo in un terzo sarebbe stato il difetto di cui
 * parla questa release.
 */
package org.openusenet.reader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.DoneAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.openusenet.reader.R
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.data.Server

/*
 * 1.6.3 (Dave Royal, 11/11) — GLI AVVISI CHE SOPRAVVIVONO AL CAMBIO DI SCHERMATA.
 *
 * Lo snackbar con ANNULLA era della schermata: uscendo dal gruppo sul telefono
 * la schermata si chiude, e snackbar e coroutine se ne andavano con lei. Ora
 * c'e' un host solo per l'app, con lo scope di AppNav: l'avviso resta mentre si
 * passa all'elenco dei gruppi o, con Avanti, al gruppo nuovo, e l'ANNULLA vale
 * per il gruppo lasciato. Le schermate lo mostrano con [SnackbarConAvvisi].
 */
class AvvisiApp(val host: SnackbarHostState, val scope: CoroutineScope)

val LocalAvvisi = staticCompositionLocalOf<AvvisiApp> { error("AvvisiApp non fornito: manca in AppNav") }

@Composable
fun rememberAvvisiApp(): AvvisiApp {
    val scope = rememberCoroutineScope()
    return remember(scope) { AvvisiApp(SnackbarHostState(), scope) }
}

/** Lo snackbar della schermata piu' quello dell'app, nello stesso posto. */
@Composable
fun SnackbarConAvvisi(snackbar: SnackbarHostState) {
    Box {
        SnackbarHost(snackbar)
        SnackbarHost(LocalAvvisi.current.host)
    }
}

/**
 * «N messaggi segnati come letti — ANNULLA», per uno o piu' gruppi. L'ANNULLA
 * rimette non letti esattamente quelli di prima, gruppo per gruppo. Un avviso
 * nuovo sostituisce quello vecchio: cambiando gruppo in fretta non si fa la coda.
 */
fun offriAnnullaLetti(
    repo: NewsRepository,
    ctx: android.content.Context,
    avvisi: AvvisiApp,
    segnati: Map<Long, List<Long>>,
) {
    val cambiati = soloCambiati(segnati)
    val totale = totaleSegnati(cambiati)
    if (totale == 0) return
    avvisi.scope.launch {
        avvisi.host.currentSnackbarData?.dismiss()
        val esito = avvisi.host.showSnackbar(
            message = ctx.getString(R.string.common_marked_read, totale),
            actionLabel = ctx.getString(R.string.common_undo_upper),
            duration = SnackbarDuration.Long,
        )
        if (esito == SnackbarResult.ActionPerformed) {
            cambiati.forEach { (gruppo, ids) -> repo.setReadBulk(ids, false, gruppo) }
        }
    }
}

/**
 * Segna letti tutti i messaggi di [groupId] e offre l'annulla.
 *
 * Non e' composable apposta: la chiamano sia un `IconButton` sia una voce di
 * menu, e nessuna delle due deve ricopiare il giro dello snackbar. 1.6.3: gira
 * nello scope dell'app, cosi' l'avviso resta anche lasciando la schermata.
 */
fun segnaTuttoLetto(
    repo: NewsRepository,
    groupId: Long,
    ctx: android.content.Context,
    avvisi: AvvisiApp,
) {
    avvisi.scope.launch {
        offriAnnullaLetti(repo, ctx, avvisi, mapOf(groupId to repo.markAllRead(groupId)))
    }
}

/**
 * L'icona di barra «segna tutto letto».
 *
 * **Dove va messa, e non e' indifferente:** MAI accanto al filtro dei non
 * letti. E' l'unica cosa che la prima segnalazione di Dave chiedeva, e vale in
 * tutti e due i layout. Nella barra sta dopo «Aggiorna» (telefono) e dopo
 * «Nuovo thread» (pannelli), cioe' subito prima dei tre puntini.
 */
@Composable
fun AzioneSegnaTuttoLetto(
    repo: NewsRepository,
    groupId: Long?,
    tint: Color = LocalContentColor.current,
) {
    val ctx = LocalContext.current
    val avvisi = LocalAvvisi.current
    IconButton(
        enabled = groupId != null,
        onClick = { groupId?.let { segnaTuttoLetto(repo, it, ctx, avvisi) } },
    ) {
        Icon(
            Icons.Default.DoneAll,
            contentDescription = stringResource(R.string.common_mark_all_read),
            tint = tint,
        )
    }
}

/*
 * 1.6.3 (Dave Royal) — «SEGNA LETTI TUTTI I GRUPPI».
 *
 * Al telefono nel menu principale, nei pannelli nel menu della colonna dei
 * gruppi. La conferma e' la scelta dei server, come «Scegli i server da
 * aggiornare»: tutti spuntati, si puo' toglierne qualcuno. Con un server solo
 * resta una domanda. Dopo, lo stesso avviso con ANNULLA di «Segna tutto letto».
 */

@Composable
fun VoceSegnaLettiTutti(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.mark_all_groups_read)) },
        leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
fun DialogoSegnaLettiTutti(repo: NewsRepository, servers: List<Server>, onChiudi: () -> Unit) {
    val ctx = LocalContext.current
    val avvisi = LocalAvvisi.current
    var scelti by remember(servers) { mutableStateOf(servers.map { it.id }.toSet()) }
    AlertDialog(
        onDismissRequest = onChiudi,
        title = { Text(stringResource(R.string.mark_all_groups_title)) },
        text = {
            Column {
                Text(
                    stringResource(
                        if (servers.size > 1) R.string.mark_all_groups_servers else R.string.mark_all_groups_one,
                    ),
                )
                if (servers.size > 1) {
                    servers.forEach { server ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clickable { scelti = if (server.id in scelti) scelti - server.id else scelti + server.id },
                        ) {
                            Checkbox(
                                checked = server.id in scelti,
                                onCheckedChange = { scelti = if (it) scelti + server.id else scelti - server.id },
                            )
                            Text(server.name.ifBlank { server.host })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = scelti.isNotEmpty(),
                onClick = {
                    onChiudi()
                    val ids = scelti
                    avvisi.scope.launch {
                        offriAnnullaLetti(repo, ctx, avvisi, repo.markAllReadServers(ids))
                    }
                },
            ) { Text(stringResource(R.string.mark_all_groups_confirm)) }
        },
        dismissButton = { TextButton(onClick = onChiudi) { Text(stringResource(R.string.common_cancel)) } },
    )
}
