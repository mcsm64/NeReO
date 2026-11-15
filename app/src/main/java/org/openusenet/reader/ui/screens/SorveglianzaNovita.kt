/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui.screens

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * 1.5.5 / 173 — SE ARRIVANO NOVITA' E NON HAI ANCORA TOCCATO, IN CIMA.
 *
 * PERCHE' LA 1.5.4 NON BASTAVA (Marcoseba: «a me capita ancora»).
 *  1. La sorveglianza si accendeva SOLO dopo un ripristino di posizione. Al
 *     primo ingresso nel gruppo (la memoria della posizione vive solo in RAM)
 *     o quando la lista partiva gia' dall'alto, restava spenta. E la lista di
 *     Compose, con le righe a chiave fissa, quando compaiono righe NUOVE
 *     SOPRA quella visibile resta ferma su quella: i nuovi finivano sopra il
 *     bordo dello schermo, anche partendo «in cima».
 *  2. Si spegneva con `isScrollInProgress`, che e' vero anche per gli
 *     scorrimenti fatti dal programma (il ripristino stesso): poteva spegnersi
 *     da sola. Ora si spegne SOLO per un trascinamento del dito
 *     ([DragInteraction.Start]), che e' l'unico gesto davvero dell'utente.
 *
 * COME FUNZIONA. Quando la schermata ha preso la sua decisione iniziale
 * ([attiva]), la prima impronta diventa il riferimento. Da li', finche'
 * l'utente non trascina la lista, se [novitaInCima] dice si' → riga 0, una
 * volta sola. Usata da ThreadListScreen (telefono) e ThreePaneHome (pannelli):
 * un posto solo, non due copie.
 *
 * [chiave] e' il gruppo: cambiando gruppo si riparte da capo.
 *
 * [spegni]: un altro gesto dell'utente che vale come «ho scelto io». Serve ai
 * pannelli, dove si apre un messaggio SENZA lasciare la lista: da li' la lista
 * non deve piu' saltare da sola (il telefono non ne ha bisogno: aprendo un
 * messaggio la lista esce di scena).
 */
@Composable
fun InCimaSeArrivanoNovita(
    listState: LazyListState,
    chiave: Any?,
    attiva: Boolean,
    impronta: ImprontaLista?,
    vistaSequenziale: Boolean,
    ordinamento: String,
    spegni: Boolean = false,
    // 1.6.4 (Jeff): con [inFondo] le novita' si cercano in fondo e si scorre a
    // [ultimoIndice]. [saltaInFondoSubito]: alla prima impronta si va gia' in
    // fondo; lo usano i pannelli, che non hanno una posizione da riprendere.
    inFondo: Boolean = false,
    ultimoIndice: Int = -1,
    saltaInFondoSubito: Boolean = false,
) {
    val fondo by rememberUpdatedState(ultimoIndice)
    var riferimento by remember(chiave) { mutableStateOf<ImprontaLista?>(null) }
    var sorveglia by remember(chiave) { mutableStateOf(true) }
    // Solo dopo che c'e' un riferimento: cambiando gruppo nei pannelli il
    // messaggio scelto nel gruppo PRECEDENTE resta selezionato per un istante,
    // e non deve spegnere la sorveglianza del gruppo nuovo.
    LaunchedEffect(chiave, spegni) { if (spegni && riferimento != null) sorveglia = false }

    LaunchedEffect(listState, chiave) {
        listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start) sorveglia = false
        }
    }

    LaunchedEffect(chiave, attiva, impronta, vistaSequenziale, ordinamento) {
        if (!attiva || !sorveglia || impronta == null) return@LaunchedEffect
        val rif = riferimento
        if (rif == null) {
            riferimento = impronta
            if (inFondo && saltaInFondoSubito && fondo >= 0) listState.scrollToItem(fondo)
            return@LaunchedEffect
        }
        if (inFondo) {
            if (novitaInFondo(vistaSequenziale, ordinamento, rif, impronta) && fondo >= 0) {
                sorveglia = false
                listState.scrollToItem(fondo)
            }
        } else if (novitaInCima(vistaSequenziale, ordinamento, rif, impronta)) {
            sorveglia = false
            listState.scrollToItem(0)
        }
    }
}
