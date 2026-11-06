/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.4.6 (§B4) — SPOSTARSI FRA I MESSAGGI: la logica, una volta sola.
 *
 * PERCHE' ESISTE. Fino alla 1.4.5 «messaggio precedente», «successivo»,
 * «prossimo non letto» e i salti fra discussioni vivevano SOLO dentro
 * `ArticleScreen`, mescolati allo stato della schermata. Nella vista a pannelli
 * non c'erano affatto: per arrivare al messaggio dopo si scorreva l'elenco e si
 * toccava. Con un telecomando — il caso di Roxy, che legge su un box TV — quello
 * scorrimento e' la parte difficile della giornata.
 *
 * Portare quelle azioni nei pannelli COPIANDOLE avrebbe creato la dodicesima
 * logica in due copie di questo progetto. Qui sono funzioni PURE: prendono la
 * lista e una posizione, restituiscono l'id di destinazione oppure `null`.
 * Chi chiama decide cosa farne — ed e' l'unica parte che le due viste non
 * possono avere in comune.
 *
 * NIENTE COMPOSE, NIENTE STATO. Per questo si possono provare in `CoreTests`
 * senza un dispositivo, ed e' li' che stanno i casi limite.
 *
 * ⚠️ IL CASO LIMITE CHE CONTA, e il commento che lo spiegava in ArticleScreen:
 * se per qualunque ragione l'articolo corrente NON e' nella sequenza, la
 * posizione vale -1, e un `getOrNull(posizione + 1)` restituirebbe il PRIMO
 * elemento invece di niente — un salto silenzioso all'inizio del gruppo. E' il
 * sintomo peggiore, perche' sembra che il comando funzioni. Ogni funzione qui
 * sotto comincia proteggendosi da quello.
 */
package org.openusenet.reader.ui.screens

/**
 * Confini della discussione che contiene [posizione]: inizio incluso, fine
 * ESCLUSA. Con [posizione] negativa restituisce l'intera lista, che e' il
 * comportamento prudente: nessun salto.
 */
fun confiniDiscussione(flat: List<ThreadItem>, posizione: Int): Pair<Int, Int> {
    if (posizione < 0) return 0 to flat.size
    var inizio = posizione
    while (inizio > 0 && !flat[inizio].threadStart) inizio--
    var fine = posizione + 1
    while (fine < flat.size && !flat[fine].threadStart) fine++
    return inizio to fine
}

/**
 * Il messaggio dopo, DENTRO la stessa discussione.
 *
 * beta14 (Marco): ci si ferma all'ultimo messaggio della discussione invece di
 * proseguire nella successiva — per quella ci sono le doppie frecce.
 */
fun idMessaggioSuccessivo(flat: List<ThreadItem>, posizione: Int): Long? {
    if (posizione < 0) return null
    val (_, fine) = confiniDiscussione(flat, posizione)
    if (posizione + 1 >= fine) return null
    return flat.getOrNull(posizione + 1)?.article?.id
}

/** Il messaggio prima, DENTRO la stessa discussione (beta14, come sopra). */
fun idMessaggioPrecedente(flat: List<ThreadItem>, posizione: Int): Long? {
    if (posizione <= 0) return null
    val (inizio, _) = confiniDiscussione(flat, posizione)
    if (posizione - 1 < inizio) return null
    return flat.getOrNull(posizione - 1)?.article?.id
}

/**
 * Il primo messaggio DA LEGGERE dopo quello corrente, nell'ordine della vista.
 *
 * Si parte da `posizione + 1` e non da `posizione`: il messaggio che si sta
 * guardando risulta gia' letto appena aperto. `null` quando non ce ne sono
 * altri — e il chiamante deve AVVISARE invece di restare fermo in silenzio
 * (lezione della regressione beta7).
 */
fun idPrimoNonLettoDopo(flat: List<ThreadItem>, posizione: Int): Long? {
    if (posizione < 0) return null
    return flat.drop(posizione + 1).firstOrNull { !it.article.read }?.article?.id
}

/**
 * 1.6 (tasto Avanti) — il prossimo messaggio da leggere IN QUESTO GRUPPO: prima
 * quelli dopo [posizione], poi si riparte dall'inizio. Con le discussioni piu'
 * recenti in cima, i non letti stanno spesso SOPRA di dove ci si trova: senza il
 * giro, Avanti lascerebbe il gruppo con dei messaggi ancora da leggere.
 *
 * Il messaggio corrente non conta mai: appena aperto e' gia' letto.
 *
 * Qui [posizione] negativa non e' un pericolo, a differenza delle funzioni qui
 * sopra: un messaggio nascosto aperto da una ricerca non sta nella sequenza, e
 * «il primo non letto del gruppo» e' la risposta giusta anche da li'.
 */
fun idNonLettoNelGruppo(sequenza: List<ThreadItem>, posizione: Int): Long? {
    if (posizione < 0) return sequenza.firstOrNull { !it.article.read }?.article?.id
    return idPrimoNonLettoDopo(sequenza, posizione)
        ?: sequenza.take(posizione).firstOrNull { !it.article.read }?.article?.id
}

/** Un gruppo visto dal tasto Avanti: solo cio' che serve a sceglierlo. */
data class GruppoDaLeggere(val id: Long, val nonLetti: Int)

/**
 * 1.6 (tasto Avanti) — il gruppo successivo con dei non letti, nell'ordine
 * dell'elenco ([gruppi] e' quello: tutti i server), ripartendo dall'inizio dopo
 * l'ultimo. Il gruppo corrente non e' mai la risposta.
 *
 * [scartati]: gruppi gia' provati, il cui contatore prometteva messaggi che non
 * c'erano. Senza, un contatore sbagliato farebbe girare il tasto sempre li'.
 */
fun idGruppoConNonLetti(
    gruppi: List<GruppoDaLeggere>,
    correnteId: Long?,
    scartati: Set<Long> = emptySet(),
): Long? {
    val indice = gruppi.indexOfFirst { it.id == correnteId }
    val giro = gruppi.drop(indice + 1) + gruppi.take(indice.coerceAtLeast(0))
    return giro.firstOrNull { it.nonLetti > 0 && it.id !in scartati }?.id
}

/**
 * 1.6.1 (BIG Umberto) — il tasto «fumetto»: il prossimo messaggio da leggere
 * DENTRO la discussione, prima quelli dopo [correnteId] e poi quelli sopra.
 * `null` = pulsante grigio. Prima usava [idPrimoNonLettoDopo], che attraversa il
 * gruppo: «non ti accorgi» di aver cambiato discussione. Per passare alla
 * successiva c'e' Avanti.
 *
 * [discussione] e' la sola discussione corrente, comunque ordinata. Non si
 * ricava dalla lista dell'elenco: nella vista sequenziale non ha inizi di
 * discussione, e nei pannelli e' filtrata da «solo non letti».
 */
fun idNonLettoNellaDiscussione(discussione: List<ThreadItem>, correnteId: Long?): Long? {
    val i = discussione.indexOfFirst { it.article.id == correnteId }
    if (i < 0) return null
    return (discussione.drop(i + 1) + discussione.take(i))
        .firstOrNull { !it.article.read }?.article?.id
}

/**
 * L'inizio della discussione SEGUENTE. `null` se l'articolo non e' nella lista
 * (meglio fermi che a caso) o se non ce n'e' un'altra dopo.
 */
fun idDiscussioneSuccessiva(tree: List<ThreadItem>, correnteId: Long?): Long? {
    val idx = tree.indexOfFirst { it.article.id == correnteId }
    if (idx < 0) return null
    return tree.drop(idx + 1).firstOrNull { it.threadStart }?.article?.id
}

/**
 * L'inizio della discussione PRECEDENTE.
 *
 * Due passi, e servono tutti e due: prima si torna all'inizio della discussione
 * in cui ci si trova, poi si risale a quello della precedente. Saltare il primo
 * passo significherebbe, da meta' discussione, tornare alla sua testa e
 * chiamarla «precedente».
 */
fun idDiscussionePrecedente(tree: List<ThreadItem>, correnteId: Long?): Long? {
    val idx = tree.indexOfFirst { it.article.id == correnteId }
    if (idx < 0) return null
    var inizio = idx
    while (inizio > 0 && !tree[inizio].threadStart) inizio--
    if (inizio == 0) return null
    var prec = inizio - 1
    while (prec > 0 && !tree[prec].threadStart) prec--
    return tree[prec].article.id
}

/**
 * I comandi di navigazione, in un oggetto solo.
 *
 * 1.4.6 (§B4) — PERCHE' UN OGGETTO E NON OTTO PARAMETRI. Nella vista a pannelli
 * la lista dei messaggi e l'articolo selezionato vivono in `ThreePaneHome`,
 * mentre la barra la disegnano `PostContinuo` e `ArticlePaneSingolo`, due
 * composable diversi. Passare i comandi uno per uno avrebbe voluto dire otto
 * parametri in piu' su ogni livello e in ogni punto di chiamata: e' esattamente
 * l'accumulo che ha prodotto §B4. Nessuno decide di scrivere una firma da
 * diciannove parametri — ci si arriva un callback per volta.
 *
 * I `Boolean` non sono un lusso: servono a spegnere il pulsante invece di
 * lasciarlo premere a vuoto. Un comando che non fa niente e non lo dice e' il
 * difetto peggiore di tutti, perche' sembra che funzioni.
 *
 * ⚠️ CONTIENE SOLO CIO' CHE VIENE DALL'ALTO. «Inizio messaggio», «fine
 * messaggio» e «ricarica il corpo» NON sono qui, ed e' una distinzione di
 * sostanza: lavorano sullo scorrimento e sul corpo del post che si sta
 * guardando, cioe' su stato che vive nel riquadro. Farli scendere da
 * ThreePaneHome avrebbe voluto dire portare su dello stato che sta bene dove
 * sta — il difetto opposto, e altrettanto costoso.
 */
data class NavigazioneMessaggi(
    val vaiPrecedente: () -> Unit,
    val vaiSuccessivo: () -> Unit,
    val vaiPrimoNonLetto: () -> Unit,
    val vaiDiscussionePrecedente: () -> Unit,
    val vaiDiscussioneSuccessiva: () -> Unit,
    /** 1.6: sempre premibile, perche' quando non c'e' altro lo dice lui. */
    val vaiAvanti: () -> Unit,
    val puoPrecedente: Boolean = false,
    val puoSuccessivo: Boolean = false,
    val esistonoNonLetti: Boolean = false,
    val puoDiscussionePrecedente: Boolean = false,
    val puoDiscussioneSuccessiva: Boolean = false,
)

/** Le azioni di barra che [NavigazioneMessaggi] sa eseguire da sola. */
val AZIONI_NAVIGAZIONE = setOf(
    "prev_msg", "next_msg", "next_unread", "avanti", "prev_thread", "next_thread",
)

/**
 * Esegue l'azione [id] se e' di navigazione; restituisce false se non lo e',
 * cosi' il chiamante prosegue con le sue. Un `when` solo, condiviso dalle due
 * viste: e' il motivo per cui questo file esiste.
 */
fun NavigazioneMessaggi.esegui(id: String): Boolean {
    when (id) {
        "prev_msg" -> vaiPrecedente()
        "next_msg" -> vaiSuccessivo()
        "next_unread" -> vaiPrimoNonLetto()
        "avanti" -> vaiAvanti()
        "prev_thread" -> vaiDiscussionePrecedente()
        "next_thread" -> vaiDiscussioneSuccessiva()
        else -> return false
    }
    return true
}

/** true se il pulsante di [id] va mostrato acceso. */
fun NavigazioneMessaggi.abilitata(id: String): Boolean = when (id) {
    "prev_msg" -> puoPrecedente
    "next_msg" -> puoSuccessivo
    "next_unread" -> esistonoNonLetti
    "prev_thread" -> puoDiscussionePrecedente
    "next_thread" -> puoDiscussioneSuccessiva
    else -> true
}
