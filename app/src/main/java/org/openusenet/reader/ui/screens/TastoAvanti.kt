/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6 (sondaggio sui gruppi) — IL TASTO AVANTI: un solo comando per leggere
 * tutto. Prossimo non letto del gruppo, poi il gruppo successivo con dei non
 * letti, su tutti i server e nell'ordine dell'elenco. Quando non resta niente
 * lo dice, invece di restare fermo.
 *
 * Le decisioni pure stanno in NavigazioneLettura.kt, provate in CoreTests. Qui
 * c'e' solo la parte che tocca il database, in comune fra le due viste; a loro
 * resta il COSA FARNE, che e' diverso (a pagine si naviga, a pannelli si cambia
 * selezione).
 *
 * ⚠️ LA SEQUENZA VA COSTRUITA SENZA IL FILTRO «SOLO NON LETTI» E MAI SULLA VISTA
 * RAGGRUPPATA. Con il filtro il messaggio appena letto esce dalla lista e la
 * posizione diventa -1; la vista raggruppata contiene le sole radici. E' la
 * regressione beta7: vedi il commento su `navView` in ArticleScreen.
 */
package org.openusenet.reader.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.openusenet.reader.AppSettings
import org.openusenet.reader.data.NewsRepository

/** Dove porta il tasto Avanti. */
sealed interface EsitoAvanti {
    data class Messaggio(val id: Long) : EsitoAvanti
    data class AltroGruppo(val gruppoId: Long, val articoloId: Long, val nome: String) : EsitoAvanti
    /** Non c'e' piu' niente da leggere, in nessun gruppo. */
    data object Fine : EsitoAvanti
}

/** La vista con cui si cammina fra i messaggi: mai quella raggruppata. */
fun vistaDiNavigazione(): String =
    if (AppSettings.threadView == ThreadViews.ROOTS) ThreadViews.TREE else AppSettings.threadView

/**
 * null = la lista non e' ancora arrivata: niente da fare, e soprattutto nessun
 * salto al gruppo dopo solo perche' questo sembra vuoto.
 */
suspend fun esitoAvanti(
    repo: NewsRepository,
    sequenza: List<ThreadItem>,
    posizione: Int,
    gruppoCorrente: Long?,
): EsitoAvanti? {
    if (sequenza.isEmpty()) return null
    idNonLettoNelGruppo(sequenza, posizione)?.let { return EsitoAvanti.Messaggio(it) }

    val gruppi = repo.subscribedGroups.first()
    val elenco = gruppi.map { GruppoDaLeggere(it.id, it.unreadCount) }
    val scartati = HashSet<Long>()
    while (true) {
        val id = idGruppoConNonLetti(elenco, gruppoCorrente, scartati) ?: return EsitoAvanti.Fine
        val articoli = repo.articles(id, AppSettings.mostraFiltrati, unreadOnly = false).first()
        val primo = withContext(Dispatchers.Default) {
            buildThreadList(articoli, unreadOnly = false, view = vistaDiNavigazione())
                .firstOrNull { !it.article.read }?.article?.id
        }
        if (primo != null) {
            return EsitoAvanti.AltroGruppo(id, primo, gruppi.firstOrNull { it.id == id }?.name.orEmpty())
        }
        scartati.add(id)
    }
}

/**
 * Cio' che succede lasciando un gruppo nella vista a pagine: i messaggi
 * contrassegnati si cancellano davvero e, se l'opzione e' attiva, il resto si
 * segna letto. Era dentro `leaveGroup` di ThreadListScreen; ora lo usa anche
 * Avanti, che dal gruppo esce senza passare dall'elenco.
 *
 * 1.6.3 (Dave Royal): la usano anche i pannelli, che non l'avevano mai chiamata,
 * e restituisce gli id segnati letti: chi la chiama li passa a
 * [offriAnnullaLetti], cosi' l'uscita ha lo stesso avviso con ANNULLA di
 * «Segna tutto letto».
 */
suspend fun applicaUscitaGruppo(repo: NewsRepository, groupId: Long): List<Long> {
    val daCancellare = PendingDeletions.drain(groupId)
    if (daCancellare.isNotEmpty()) repo.deleteLocal(daCancellare)
    return if (AppSettings.markReadOnExit) repo.markAllRead(groupId) else emptyList()
}
