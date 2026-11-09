/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * "Verifica identita' TLS" (1.4) — la schermata dove la fiducia si CONCEDE.
 *
 * PERCHE' STA QUI E NON IN UN DIALOGO DURANTE IL SYNC.
 * Un avviso di sicurezza che interrompe un aggiornamento e' quello che si
 * clicca senza leggere. E in pratica bloccherebbe `headerSyncMutex` in attesa
 * di un umano che magari ha messo il telefono in tasca. Quindi: la connessione
 * FALLISCE, l'identita' osservata resta registrata come PENDING, e l'utente
 * approva QUANDO VUOLE, da qui, con i dati sotto gli occhi e nessuna fretta.
 *
 * PENDING NON E' FIDUCIA. E' una fotografia in attesa che qualcuno la guardi:
 * un "no" con memoria, non un mezzo "si'".
 *
 * MA PENDING NON E' NEMMENO LO STATO NORMALE. Un server la cui catena e il cui
 * nome host sono in regola si registra come VALIDA: i suoi dati servono
 * all'avviso di scadenza, non a chiedere un giudizio che non serve. Mostrare
 * "VISTA, NON FIDATA" su server perfettamente sani sarebbe un allarme falso —
 * cioe' addestramento a cliccare senza leggere, l'esatto contrario di quello
 * che questa schermata esiste per fare. (Rilievo di Marco, 17 settembre 2026,
 * su Paganini tornato regolare dopo il rinnovo di Ivo Gandolfo.)
 */
package org.openusenet.reader.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.openusenet.reader.R
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.data.TlsPin
import org.openusenet.reader.data.TlsPinStato
import org.openusenet.reader.nntp.IdentitaTls
import org.openusenet.reader.ui.indietroSicuro
import org.openusenet.reader.ui.rememberAppunti

/** Soglia dell'avviso "il certificato scade fra N giorni". */
const val TLS_GIORNI_AVVISO = 30

/** 1.6.2 / 189: la data completa di text/DataOra, con le scelte di Aspetto. */
private fun dataBreve(ms: Long): String {
    val p = org.openusenet.reader.AppSettings.preferenzeDataOra()
    return org.openusenet.reader.text.DataOra.dataCompleta(org.openusenet.reader.text.DataOra.inZona(ms / 1000, p), p)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TlsIdentityScreen(repo: NewsRepository, nav: NavHostController, serverId: Long) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val appunti = rememberAppunti()

    var pin by remember { mutableStateOf<TlsPin?>(null) }
    var nomeServer by remember { mutableStateOf("") }
    var sonda by remember { mutableStateOf<IdentitaTls?>(null) }
    var sondaErrore by remember { mutableStateOf<String?>(null) }
    var sondaInCorso by remember { mutableStateOf(false) }
    var ricarica by remember { mutableStateOf(0) }

    LaunchedEffect(serverId, ricarica) {
        pin = repo.pinTls(serverId)
        nomeServer = repo.serverNameById(serverId) ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tls_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.indietroSicuro() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(nomeServer, style = MaterialTheme.typography.titleMedium)

            val p = pin
            if (p == null) {
                Text(
                    stringResource(R.string.tls_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                /*
                 * TRE stati, non due — e la differenza fra VALIDA e PENDING e'
                 * tutta qui: un server in regola NON deve comparire come
                 * sospetto. Rilievo di Marco, 17 settembre: dopo il rinnovo di
                 * Paganini la validazione normale passa, e mostrare
                 * "VISTA, NON FIDATA" su quattro server su quattro sarebbe un
                 * allarme falso — cioe' addestramento a cliccare senza leggere,
                 * l'opposto di quello che questa schermata serve a fare.
                 */
                val fidata = p.state == TlsPinStato.TRUSTED
                val regolare = p.state == TlsPinStato.VALIDA
                val etichetta = when {
                    fidata -> R.string.tls_state_trusted
                    regolare -> R.string.tls_state_valid
                    else -> R.string.tls_state_pending
                }
                Text(
                    stringResource(etichetta),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (fidata || regolare) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(
                        when {
                            fidata -> R.string.tls_trusted_note
                            regolare -> R.string.tls_valid_note
                            else -> R.string.tls_pending_note
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                CampoTls(R.string.tls_endpoint, "${p.host}:${p.port}")
                CampoTls(R.string.tls_subject, p.subject)
                CampoTls(R.string.tls_issuer, p.issuer)
                CampoTls(R.string.tls_spki, p.spkiSha256)
                CampoTls(R.string.tls_cert, p.certificateSha256)
                CampoTls(R.string.tls_valid, "${dataBreve(p.notBefore)} → ${dataBreve(p.notAfter)}")

                /*
                 * 1.4 — L'AVVISO DI SCADENZA, che PREVIENE invece di attutire.
                 *
                 * Non era nel contratto del 13 settembre perche' allora non
                 * sapevamo una cosa che Ivo Gandolfo ci ha detto dopo: il
                 * rinnovo del certificato di Paganini e' MANUALE. Un certificato
                 * Let's Encrypt vive 90 giorni, quindi quel caso non e' chiuso,
                 * e' CICLICO. Con il TrustManager della 1.4 la catena la vediamo
                 * a ogni connessione e `notAfter` e' gratis: non usarlo sarebbe
                 * stato sprecare un dato che abbiamo gia' in mano.
                 */
                val giorni = (p.notAfter - System.currentTimeMillis()) / 86_400_000L
                when {
                    giorni < 0 -> Avviso(stringResource(R.string.tls_expired), errore = true)
                    giorni <= TLS_GIORNI_AVVISO ->
                        Avviso(stringResource(R.string.tls_expires_in, giorni), errore = true)
                    else -> Unit
                }

                CampoTls(R.string.tls_observed, dataBreve(p.observedAt))
                p.approvedAt?.let { CampoTls(R.string.tls_approved, dataBreve(it)) }

                Row(Modifier.padding(top = 12.dp)) {
                    if (!fidata) {
                        // Su un server REGOLARE il pulsante non e' una richiesta:
                        // e' un'offerta. "Appunta" invece di "Approva" — chi non
                        // lo tocca resta con la validazione normale, che va
                        // benissimo.
                        FilledTonalButton(onClick = {
                            scope.launch {
                                repo.approvaPinTls(serverId)
                                ricarica++
                            }
                        }) {
                            Text(
                                stringResource(
                                    if (regolare) R.string.tls_pin_anyway else R.string.tls_approve,
                                ),
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                repo.revocaPinTls(serverId)
                                ricarica++
                            }
                        },
                        modifier = Modifier.padding(start = if (fidata) 0.dp else 12.dp),
                    ) { Text(stringResource(R.string.tls_revoke)) }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            /*
             * LA SONDA — e il suo limite, scritto nell'interfaccia e non solo
             * nei documenti.
             *
             * Mostra l'identita' che il server presenta ADESSO, con una
             * connessione DEDICATA: nessun AUTHINFO, nessuna credenziale in
             * gioco. Serve a rispondere alla domanda giusta prima di approvare:
             * "quella fotografia di tre giorni fa vale ancora?".
             *
             * MA fra la sonda e l'handshake successivo l'identita' puo' cambiare
             * di nuovo: e' un TOCTOU, e nessuna sonda lo elimina. Il valore
             * della sonda e' ONESTA', non una garanzia crittografica — la
             * garanzia resta il confronto del pin a OGNI connessione. Questo
             * l'utente deve leggerlo qui, non scoprirlo dopo.
             */
            Text(stringResource(R.string.tls_probe_section), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.tls_probe_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            FilledTonalButton(
                enabled = !sondaInCorso,
                onClick = {
                    sondaInCorso = true
                    sondaErrore = null
                    scope.launch {
                        runCatching { repo.sondaTls(serverId) }
                            .onSuccess { sonda = it; if (it == null) sondaErrore = "—" }
                            .onFailure { sondaErrore = it.message ?: it.javaClass.simpleName }
                        sondaInCorso = false
                    }
                },
            ) { Text(stringResource(R.string.tls_probe)) }
            if (sondaInCorso) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))

            sondaErrore?.let {
                Avviso(stringResource(R.string.tls_probe_failed, it), errore = true)
            }
            sonda?.let { s ->
                CampoTls(R.string.tls_spki, s.spkiSha256)
                CampoTls(R.string.tls_subject, s.subject)
                CampoTls(R.string.tls_issuer, s.issuer)
                CampoTls(R.string.tls_valid, "${dataBreve(s.notBefore)} → ${dataBreve(s.notAfter)}")
                val registrato = pin?.spkiSha256
                if (registrato != null) {
                    Avviso(
                        stringResource(
                            if (registrato == s.spkiSha256) R.string.tls_probe_same else R.string.tls_probe_diff,
                        ),
                        errore = registrato != s.spkiSha256,
                    )
                }
                OutlinedButton(
                    onClick = { appunti.copia(s.spkiSha256) },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(stringResource(R.string.common_copy)) }
            }
        }
    }
}

@Composable
private fun CampoTls(etichetta: Int, valore: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            stringResource(etichetta),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Monospace: queste stringhe si CONFRONTANO a occhio, carattere per
        // carattere. Un font proporzionale rende quel confronto piu' difficile
        // proprio dove l'errore costa di piu'.
        Text(valore, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun Avviso(testo: String, errore: Boolean) {
    Text(
        testo,
        style = MaterialTheme.typography.bodySmall,
        color = if (errore) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}
