/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6 (sondaggio sui gruppi) — IGNORA DISCUSSIONE: le risposte future arrivano
 * gia' lette, tranne le risposte ai miei messaggi.
 *
 * NE' UN'AZIONE NUOVA DEL MOTORE NE' UNA REGOLA: il motore ragiona a punteggi e
 * una regola per discussione riempirebbe la schermata Filtri. C'e' un registro
 * di message-id ([org.openusenet.reader.data.IgnoredId]) e la stessa chiusura
 * transitiva di [FollowupPropagator], che regge gia' le References potate e le
 * catene arrivate nello stesso lotto.
 *
 * «E' MIO» NON SI RIDEFINISCE QUI: chi chiama passa `Article.replyToMe` e
 * `AppSettings.eMioPost`. Kotlin puro: si prova in CoreTests.
 */
package org.openusenet.reader.filter

/**
 * I message-id di [lotto] che appartengono a una discussione ignorata.
 *
 * Comprende anche i messaggi GIA' nel [registro]: [FollowupPropagator.propagate]
 * li salta come storia vecchia, ma qui una copia dello stesso messaggio puo'
 * arrivare da un altro gruppo o da un altro server, e vale come le altre.
 */
fun colpitiDaIgnorate(lotto: List<PropagationInput>, registro: Set<String>): Set<String> {
    if (registro.isEmpty()) return emptySet()
    return FollowupPropagator.propagate(lotto, registro, FollowupMode.SUBTREE) +
        lotto.map { it.messageId }.filter { it in registro }
}

/**
 * Un messaggio di una discussione ignorata si segna letto? No se risponde a me,
 * no se e' mio (dopo aver scritto lo si vuole ritrovare: Dave Royal, 1.4.1), no
 * se e' seguito: Segui e Ignora si escludono, e nel dubbio si mostra.
 */
fun siSegnaLetto(rispostaAMe: Boolean, mio: Boolean, seguito: Boolean): Boolean =
    !rispostaAMe && !mio && !seguito
