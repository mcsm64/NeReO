/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui.screens

/*
 * 1.6.3 (Dave Royal, 11/11) — LE REGOLE DELL'USCITA DAL GRUPPO, IN KOTLIN PURO.
 *
 * Nei pannelli «Segna tutto letto uscendo dal gruppo» non era mai stato
 * collegato: il cambio di gruppo in colonna 1 e Avanti cambiavano solo il gruppo
 * selezionato. Conta come uscita SOLO il passaggio a un altro gruppo voluto
 * dall'utente (tocco in colonna 1, Avanti). Non contano: lo stesso gruppo, il
 * tutto schermo, la rotazione, il ritorno da un'altra schermata, la
 * disiscrizione, l'app in background. Nel dubbio non si segna: chi lo chiama
 * sono solo i due gesti voluti, e questa funzione scarta il resto.
 */

/** Vero se lasciare [prima] per [dopo] e' un'uscita dal gruppo. */
fun eUscitaDalGruppo(prima: Long?, dopo: Long?): Boolean =
    prima != null && dopo != null && prima != dopo

/** Quanti messaggi sono diventati letti, su tutti i gruppi. */
fun totaleSegnati(segnati: Map<Long, List<Long>>): Int = segnati.values.sumOf { it.size }

/** Solo i gruppi in cui qualcosa e' cambiato: l'ANNULLA tocca quelli e basta. */
fun soloCambiati(segnati: Map<Long, List<Long>>): Map<Long, List<Long>> =
    segnati.filterValues { it.isNotEmpty() }
