/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
import org.openusenet.reader.data.Group
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.text.Condiviso
import org.openusenet.reader.text.testoCondiviso
import org.openusenet.reader.ui.screens.serverDaMostrare

/*
 * 1.6.4 (Halv Skjaldson) — «CONDIVIDI → NeReO».
 *
 * Da un'altra app si sceglie NeReO nel menu Condividi. MainActivity si apre
 * nell'attivita' di quell'app, come Signal o WhatsApp: chiede in quale gruppo
 * scrivere (l'ultimo usato per una condivisione in cima), apre un messaggio
 * nuovo con oggetto e link gia' scritti e il cursore sotto il link. Inviato o
 * abbandonato il messaggio, si torna all'app di partenza.
 */

/** Il contenuto di un «Condividi» di solo testo, o null se l'intent non lo e'. */
fun condivisoDa(intent: Intent?): Condiviso? {
    if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("text/") != true) return null
    val c = testoCondiviso(
        intent.getStringExtra(Intent.EXTRA_SUBJECT),
        intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
    )
    return c.takeIf { it.oggetto.isNotEmpty() || it.corpo.isNotEmpty() }
}

/**
 * Il contenuto in attesa della schermata di scrittura, per UN gruppo e per
 * poco tempo: stesso schema di CitazioneInAttesa.
 */
object CondivisioneInArrivo {
    private const val VALIDITA_MS = 2 * 60 * 1000L
    private var gruppo: Long = -1
    private var contenuto: Condiviso? = null
    private var quando: Long = 0

    fun metti(groupId: Long, c: Condiviso, ora: Long = System.currentTimeMillis()) {
        gruppo = groupId; contenuto = c; quando = ora
    }

    fun prendi(groupId: Long, ora: Long = System.currentTimeMillis()): Condiviso? {
        val c = contenuto.takeIf { groupId == gruppo && ora - quando <= VALIDITA_MS }
        contenuto = null; gruppo = -1
        return c
    }
}

/** Montato in AppNav solo quando l'app e' stata aperta da «Condividi». */
@Composable
fun GestoreCondivisione(repo: NewsRepository, nav: NavHostController, condiviso: Condiviso) {
    val attivita = LocalContext.current as? Activity
    var scelto by remember { mutableStateOf(false) }
    if (!scelto) {
        DialogoGruppoCondivisione(
            repo = repo,
            onScelto = { gruppo ->
                scelto = true
                AppSettings.ultimoGruppoCondivisione = gruppo.id
                AppSettings.save()
                CondivisioneInArrivo.metti(gruppo.id, condiviso)
                nav.navigate(Routes.composeNew(gruppo.id))
            },
            onAnnulla = { attivita?.finish() },
        )
    } else {
        // Tornati all'elenco dei gruppi dopo la scrittura: si torna all'app di partenza.
        LaunchedEffect(Unit) {
            var scrivendo = false
            nav.currentBackStackEntryFlow.collect { voce ->
                when (voce.destination.route) {
                    Routes.COMPOSE_NEW -> scrivendo = true
                    Routes.GROUPS -> if (scrivendo) attivita?.finish()
                }
            }
        }
    }
}

@Composable
private fun DialogoGruppoCondivisione(
    repo: NewsRepository,
    onScelto: (Group) -> Unit,
    onAnnulla: () -> Unit,
) {
    val gruppi by repo.subscribedGroups.collectAsState(initial = emptyList())
    val servers by repo.servers.collectAsState(initial = emptyList())
    val ultimo = gruppi.firstOrNull { it.id == AppSettings.ultimoGruppoCondivisione }
    val altri = gruppi.filter { it.id != ultimo?.id }
    fun etichetta(g: Group): String {
        val s = serverDaMostrare(servers, g.serverId)
        return if (s.isEmpty()) g.name else "${g.name} · $s"
    }
    AlertDialog(
        onDismissRequest = onAnnulla,
        title = { Text(stringResource(R.string.share_choose_group)) },
        text = {
            if (gruppi.isEmpty()) {
                Text(stringResource(R.string.share_no_groups))
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    if (ultimo != null) {
                        item(key = "ultimo") {
                            Text(
                                "★ " + etichetta(ultimo),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().clickable { onScelto(ultimo) }.padding(vertical = 12.dp),
                            )
                            HorizontalDivider()
                        }
                    }
                    items(altri, key = { it.id }) { g ->
                        Text(
                            etichetta(g),
                            modifier = Modifier.fillMaxWidth().clickable { onScelto(g) }.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onAnnulla) { Text(stringResource(R.string.common_cancel)) } },
    )
}
