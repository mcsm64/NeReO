/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.data

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * 1.6.1 (BIG Umberto, 4/11) — IL GIRO SUI GRUPPI DI UN SERVER.
 *
 * Il suo caso: un server non risponde e gli altri non si aggiornano. Il dialogo
 * «Scegli i server» chiamava syncServer uno per volta dentro un `sumOf`, e il primo
 * errore fermava tutto; intanto il server spento veniva ritentato per ogni gruppo,
 * 15 secondi a gruppo, tenendo occupato il mutex del sync.
 *
 * Qui la regola, in Kotlin puro per poterla provare:
 *  - se la sessione NON si apre (rete, saluto, login), il server esce dal giro al
 *    primo tentativo: riprovare gruppo per gruppo non cambierebbe nulla;
 *  - se si apre e un gruppo fallisce, quel gruppo si salta e si riapre per i
 *    successivi, come prima: la guardia sta in `riusciti`, che avanza solo a
 *    gruppo finito;
 *  - l'annullamento risale sempre, non e' un errore del server.
 *
 * [sessione] apre la connessione e chiama il blocco; [sincronizza] lavora un gruppo.
 */
suspend fun <C, G> giroDelServer(
    gruppi: List<G>,
    sessione: suspend (blocco: suspend (C) -> Unit) -> Unit,
    sincronizza: suspend (C, G) -> Int,
): EsitoGiro {
    var scaricati = 0
    var riusciti = 0
    var indice = 0
    var errore: Throwable? = null
    var sessioneMancata = false
    while (indice < gruppi.size && !sessioneMancata) {
        currentCoroutineContext().ensureActive()
        val partenza = indice
        var aperta = false
        try {
            sessione { client ->
                aperta = true
                while (indice < gruppi.size) {
                    currentCoroutineContext().ensureActive()
                    scaricati += sincronizza(client, gruppi[indice])
                    riusciti++
                    indice++
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, come il runCatching di prima: un gruppo ostile puo' far
            // saltare anche la memoria, e non deve fermare il giro.
            errore = e
            if (!aperta) sessioneMancata = true
            else if (indice == partenza) indice++
        }
    }
    return EsitoGiro(scaricati, riusciti, errore, sessioneMancata)
}

data class EsitoGiro(
    val scaricati: Int,
    /** Gruppi completati. */
    val riusciti: Int,
    /** L'ultimo errore, se c'e' stato. */
    val errore: Throwable?,
    /** La sessione non si e' aperta, e i gruppi rimasti non sono stati provati. */
    val sessioneMancata: Boolean,
) {
    /**
     * Da dire all'utente. Un solo gruppo in errore, con gli altri riusciti, resta
     * fuori: un gruppo sparito dal server darebbe un avviso a ogni aggiornamento.
     */
    val daSegnalare: Boolean get() = errore != null && (sessioneMancata || riusciti == 0)
}

/** Il server fuori dal riepilogo finale. [motivo] null: non ha risposto. */
data class ServerNonAggiornato(val nome: String, val motivo: String?)

/** Vero se il server non ha risposto: niente da spiegare oltre a questo. */
fun nonRisponde(e: Throwable): Boolean =
    e is SocketTimeoutException || e is ConnectException || e is NoRouteToHostException

fun serverNonAggiornato(nome: String, errore: Throwable): ServerNonAggiornato =
    ServerNonAggiornato(nome, if (nonRisponde(errore)) null else errore.message)

/** «solani (non risponde), e+s (481 rifiutato)». */
fun elencoNonAggiornati(lista: List<ServerNonAggiornato>, nonRispondeTesto: String): String =
    lista.joinToString(", ") { "${it.nome} (${it.motivo ?: nonRispondeTesto})" }
