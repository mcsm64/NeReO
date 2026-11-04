/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.data

/*
 * 1.6 — IL PROPRIO POST CHE NON COMPARIVA DOPO L'INVIO (Bingo3331 + HAL, post
 * del 2 novembre 2026; piano A del 3/11). Kotlin puro, senza un solo import:
 * la decisione si prova in CoreTests.
 *
 * Il caso: risposta in free.it.ai.general via Astraweb, articolo 3282. Sul
 * server c'era (XOVER, STAT, HEAD, altri lettori), in NeReO no, e capitava
 * «a caso».
 *
 * La causa, letta nel codice: alla fine di ogni lotto il segnalibro andava
 * alla fine dell'intervallo CHIESTO, qualunque cosa fosse arrivata. Subito
 * dopo l'invio ComposeScreen lancia il sync; GROUP dice gia' high=3282 ma
 * l'overview del 3282 puo' non esserci ancora (abduzione: il server assegna
 * il numero prima di avere pronta l'overview). XOVER 3282-3282 torna vuoto, il
 * segnalibro va comunque a 3282, e al giro dopo from=3283 > high: il 3282 non
 * si chiede piu'.
 *
 * La regola nuova vale SOLO per l'ultimo lotto, quello che arriva a high:
 * il segnalibro si ferma all'ultimo numero davvero ricevuto. Nei lotti
 * intermedi i buchi sono normali (articoli cancellati o scaduti) e il
 * segnalibro va alla fine del lotto come prima.
 *
 * LIMITE DICHIARATO (previsione): se due messaggi arrivano a pochi secondi e
 * solo il secondo ha gia' l'overview, il primo si perde ancora.
 *
 * COSTO: se la coda manca per sempre (un articolo cancellato in fondo al
 * gruppo) ogni sync richiede quel pezzetto finale, pochi byte. Non ricrea
 * messaggi cancellati in locale: quei numeri non sono mai entrati.
 */

/**
 * Dove mettere il segnalibro dopo il lotto [lo]..[hi].
 *
 * [attuale] e' il segnalibro letto all'inizio del sync; [numeriRicevuti] sono
 * i numeri delle righe di overview arrivate, compresi gli articoli che i
 * filtri scarteranno (sono stati visti: non vanno richiesti di nuovo).
 */
fun segnalibroDopoLotto(
    attuale: Long,
    lo: Long,
    hi: Long,
    numeriRicevuti: List<Long>,
    ultimoLotto: Boolean,
): Long {
    if (!ultimoLotto) return hi
    // tutto quello che sta prima del lotto e' gia' passato: o da un lotto
    // precedente di questo sync, o scavalcato di proposito (limite di testate,
    // numeri sotto il low del server)
    val base = maxOf(attuale, lo - 1)
    // numeri fuori dall'intervallo chiesto non contano: un server che li
    // mandasse non deve spostare il segnalibro oltre quello che si e' chiesto
    val ultimoRicevuto = numeriRicevuti.filter { it in lo..hi }.maxOrNull() ?: return base
    return maxOf(base, ultimoRicevuto)
}
