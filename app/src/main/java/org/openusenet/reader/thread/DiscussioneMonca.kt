/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6 (sondaggio sui gruppi) — QUALI MESSAGGI MANCANO A UNA DISCUSSIONE.
 *
 * Solo gli ANTENATI: ogni messaggio nomina i suoi nelle References, mentre
 * nessuno dice chi gli ha risposto. Una risposta scaduta non si puo' cercare.
 *
 * Kotlin puro. La lettura delle References e' quella unica, col suo tetto
 * ([Threader.idNelleReferences]).
 */
package org.openusenet.reader.thread

/** Tutti gli id nominati da queste righe References, senza ripetizioni. */
fun idNominati(references: List<String>): List<String> =
    references.flatMap { Threader.idNelleReferences(it) }.distinct()

/**
 * Gli id da cercare: nominati, non [presenti] in questo gruppo e non ancora
 * [giaCercati]. Prima i piu' vicini a ogni messaggio (il padre, poi il nonno):
 * se il tetto taglia, resta fuori la parte piu' lontana della discussione.
 */
fun idDaCercare(
    references: List<String>,
    presenti: Set<String>,
    giaCercati: Set<String> = emptySet(),
): List<String> {
    val esito = LinkedHashSet<String>()
    for (r in references) {
        for (id in Threader.idNelleReferences(r).asReversed()) {
            if (id !in presenti && id !in giaCercati) esito.add(id)
        }
    }
    return esito.toList()
}
