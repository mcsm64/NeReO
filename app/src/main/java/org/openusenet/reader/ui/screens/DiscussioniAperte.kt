/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Discussioni aperte con il + nella lista discussioni, conservate mentre si
 * legge un articolo.
 *
 * 1.5.9 / 177 (vort3, via Marco il 30 ottobre 2026): «tornando dall'articolo
 * voglio ritrovare aperte le discussioni che avevo aperto».
 *
 * PERCHE' SI PERDEVANO. Navigation Compose toglie l'elenco dalla composizione
 * mentre l'articolo e' sullo schermo: ogni `remember` di ThreadListScreen
 * riparte da capo al ritorno. Si ripartiva tutto chiuso, e con le discussioni
 * chiuse spariva anche la riga a cui era ancorata [ThreadScrollMemory]: per
 * questo sembrava persa pure la posizione.
 *
 * LA CHIAVE E' LA VOCE DI NAVIGAZIONE, NON IL GRUPPO. Il ritorno dall'articolo
 * ritrova la stessa voce (stesso `NavBackStackEntry.id`); un nuovo ingresso nel
 * gruppo ne crea una nuova e quindi parte tutto chiuso, come vuole la regola di
 * sempre («uscendo si riparte tutto richiuso»). Si svuota anche in leaveGroup,
 * per non lasciare indietro voci che non torneranno.
 *
 * ⚠️ PERCHE' NON `rememberSaveable` (piano B, scartato da Marco): l'insieme
 * finirebbe nel Bundle dell'attivita', e con «espandi tutto» in un gruppo da
 * migliaia di discussioni potrebbe superare il limite del binder — crash
 * premendo Home. Qui resta in memoria: se Android chiude l'app in background,
 * al ritorno si riparte chiuso, che e' il comportamento di prima.
 */
package org.openusenet.reader.ui.screens

object DiscussioniAperte {

    private val perVoce = HashMap<String, Set<String>>()

    fun get(voce: String): Set<String> = perVoce[voce].orEmpty()

    fun put(voce: String, radici: Set<String>) {
        if (radici.isEmpty()) perVoce.remove(voce) else perVoce[voce] = radici
    }

    fun clear(voce: String) { perVoce.remove(voce) }
}
