/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.data

import kotlinx.coroutines.delay

/*
 * 1.6.5 / 193 (segnalazione del 20/11, log su Astraweb, free.it.ai.general) —
 * IL PROPRIO POST CHE ARRIVA QUALCHE SECONDO DOPO.
 *
 * Dopo il 240 il server non aveva ancora alzato nemmeno il numero piu' alto del
 * gruppo: l'aggiornamento subito dopo l'invio non trovava niente, e NeReO non
 * riprovava. RFC 3977 §6.3.1.2: il 240 dice che l'articolo SARA' disponibile,
 * non quando. Non e' il vecchio #3282 (overview in ritardo, corretto nella 1.6).
 *
 * Regola, in Kotlin puro: un aggiornamento subito; se non e' arrivato niente,
 * di nuovo dopo [ATTESE_DOPO_INVIO_MS]. Ci si ferma al primo arrivo. Oltre, il
 * post compare al prossimo aggiornamento, come prima.
 */
val ATTESE_DOPO_INVIO_MS: List<Long> = listOf(2_000L, 5_000L, 10_000L)

suspend fun aggiornaFinoAdArrivo(
    attese: List<Long> = ATTESE_DOPO_INVIO_MS,
    attendi: suspend (Long) -> Unit = { delay(it) },
    prima: (Long) -> Unit = {},
    aggiorna: suspend () -> Int,
): Int {
    var arrivati = aggiorna()
    for (ms in attese) {
        if (arrivati > 0) break
        prima(ms)
        attendi(ms)
        arrivati = aggiorna()
    }
    return arrivati
}

/** I gruppi di destinazione iscritti sullo stesso server; almeno quello di partenza. */
fun gruppiDaAggiornareDopoInvio(partenza: Group, iscrittiDelServer: List<Group>, destinazioni: List<String>): List<Long> {
    val nomi = destinazioni.map { it.trim() }.toSet() + partenza.name
    return iscrittiDelServer.filter { it.name in nomi }.map { it.id }.ifEmpty { listOf(partenza.id) }
}
