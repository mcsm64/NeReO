/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui.screens

/**
 * 1.5.5 / 173 — «CI SONO NOVITA' IN CIMA?», DECISO IN UN POSTO SOLO.
 *
 * LA SEGNALAZIONE (Marcoseba, 18 ottobre 2026, sulla 1.5.4): «Purtroppo a me
 * capita ancora» — aprendo un gruppo i messaggi nuovi restano fuori schermo.
 *
 * La 1.5.4 si chiedeva «il PRIMO della lista e' cambiato?». Non basta: nella
 * vista ad albero, ordinata per messaggio piu' recente, una risposta nuova
 * dentro la discussione GIA' in cima lascia il primo identico, e NeReO
 * concludeva «niente di nuovo».
 *
 * IL SEGNALE NUOVO: l'id interno degli articoli (chiave autoincrementale di
 * Room; l'inserimento usa IGNORE, quindi un articolo gia' presente non
 * riceve un id nuovo) cresce a ogni messaggio scaricato. «L'id piu' alto e'
 * salito» = e' arrivato almeno un messaggio.
 *
 * MA DOVE FINISCE IL NUOVO dipende dall'ordinamento, ed e' questo che decide:
 *  - ordinamenti «_asc»: le novita' stanno in FONDO. Andare in cima non le
 *    mostra: risposta sempre `false`, si resta dov'eri;
 *  - vista sequenziale (qualunque verso discendente) e «activity_desc»: ogni
 *    messaggio nuovo porta in cima la sua riga → conta l'id piu' alto;
 *  - «opened_desc» ad albero o raggruppata: in cima sale solo una discussione
 *    NUOVA, non una risposta a una vecchia → conta il primo della lista,
 *    come nella 1.5.4.
 *
 * 1.6.1 / 187: in quest'ultimo caso deve salire ANCHE l'id piu' alto. Il primo
 * cambia pure senza messaggi nuovi: una seguita appena letta scende, una riga
 * letta sparisce con «solo non letti». Quello non e' un arrivo.
 *
 * Kotlin puro, nessun import: si prova da solo con kotlinc.
 */
data class ImprontaLista(val primoId: Long, val ultimoId: Long, val ultimaRigaId: Long = 0)

/**
 * L'impronta di una lista: `null` se la lista e' vuota (niente da confrontare).
 * [ultimaRigaId]: l'ultima riga, per le novita' in fondo (1.6.4, Jeff).
 */
fun improntaLista(primoId: Long?, ids: Iterable<Long>, ultimaRigaId: Long? = null): ImprontaLista? {
    if (primoId == null) return null
    val massimo = ids.maxOrNull() ?: return null
    return ImprontaLista(primoId, massimo, ultimaRigaId ?: 0)
}

fun novitaInCima(
    vistaSequenziale: Boolean,
    ordinamento: String,
    prima: ImprontaLista,
    ora: ImprontaLista,
): Boolean = when {
    ordinamento.endsWith("_asc") -> false
    vistaSequenziale || ordinamento == "activity_desc" -> ora.ultimoId > prima.ultimoId
    else -> ora.primoId != prima.primoId && ora.ultimoId > prima.ultimoId
}

/*
 * 1.6.4 (Jeff, mail del 12/11) — IL FONDO, PER CHI LEGGE DAL PIU' VECCHIO.
 *
 * Jeff ordina dal piu' vecchio, come in Thunderbird, e vuole aprire il gruppo
 * in fondo, sui piu' recenti. Con gli ordini «_asc» le novita' stanno li', e
 * [novitaInCima] risponde sempre no. Con l'opzione `apriInFondo` (Aspetto,
 * spenta di default) si specchia la regola della cima:
 *  - vista sequenziale e «activity_asc»: ogni messaggio nuovo porta in fondo la
 *    sua riga → conta l'id piu' alto;
 *  - «opened_asc» ad albero o raggruppata: in fondo scende solo una discussione
 *    NUOVA → deve cambiare anche l'ultima riga.
 */
fun partenzaInFondo(ordinamento: String, apriInFondo: Boolean): Boolean =
    apriInFondo && ordinamento.endsWith("_asc")

fun novitaInFondo(
    vistaSequenziale: Boolean,
    ordinamento: String,
    prima: ImprontaLista,
    ora: ImprontaLista,
): Boolean = when {
    !ordinamento.endsWith("_asc") -> false
    vistaSequenziale || ordinamento == "activity_asc" -> ora.ultimoId > prima.ultimoId
    else -> ora.ultimaRigaId != prima.ultimaRigaId && ora.ultimoId > prima.ultimoId
}
