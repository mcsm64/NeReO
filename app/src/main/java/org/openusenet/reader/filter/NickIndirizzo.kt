/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.filter

/**
 * 1.5.7 / 175 — IL CONFRONTO «NICK = INDIRIZZO» (anti-sockpuppet).
 *
 * LA RICHIESTA (Marco, 20 ottobre 2026): un troll che si crea un'identita'
 * nuova per ogni post, costruendo l'indirizzo spezzando il nick:
 *
 *     Coltivatore Diretto <colti@vatorediret.to>   -> coltivatorediretto = coltivatorediretto
 *     Mondozoccola <mondo@zocco.la>                -> mondozoccola = mondozoccola
 *     Gino Pontieri <gino@pontie.it>               -> ginopontieri != ginopontieit
 *
 * Il terzo NON scatta, e va detto: lo schema non e' costante. Per quello c'e'
 * il campo «Intestazioni», e il punteggio cumulativo del motore (piu' indizi
 * deboli sommati) — vedi NeReO_1.5.7_175_virgolette.md.
 *
 * PERCHE' NON UNA REGEX: una regex sa ripetere un pezzo di testo (\1), non sa
 * ripeterlo TOGLIENDOGLI i punti. Serve un confronto dopo la normalizzazione.
 *
 * NORMALIZZARE = minuscole e solo lettere e cifre: nel nick cadono spazi,
 * virgolette e trattini; nell'indirizzo la chiocciola e i punti. Il nick deve
 * avere almeno [MIN_LUNGHEZZA] lettere o cifre: sotto, un'uguaglianza puo'
 * essere un caso (un nick «ab» e un indirizzo «a@b»).
 *
 * Kotlin puro, nessun import: si prova da solo con kotlinc.
 */
private const val MIN_LUNGHEZZA = 6

private fun normalizza(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }

/**
 * Nick e indirizzo di una riga From, nelle due forme d'uso:
 * `Nick <indirizzo>` e `indirizzo (Nick)`. Null se manca uno dei due.
 */
fun nickEIndirizzo(from: String): Pair<String, String>? {
    val f = from.trim()
    val apre = f.lastIndexOf('<')
    val chiude = f.lastIndexOf('>')
    if (apre >= 0 && chiude > apre) {
        val indirizzo = f.substring(apre + 1, chiude).trim()
        val nick = f.substring(0, apre).trim().trim('"').trim()
        return if (nick.isNotEmpty() && '@' in indirizzo) nick to indirizzo else null
    }
    val pa = f.indexOf('(')
    val pc = f.lastIndexOf(')')
    if (pa >= 0 && pc > pa) {
        val indirizzo = f.substring(0, pa).trim()
        val nick = f.substring(pa + 1, pc).trim()
        return if (nick.isNotEmpty() && '@' in indirizzo) nick to indirizzo else null
    }
    return null
}

/** true se il nick, normalizzato, coincide con l'indirizzo senza chiocciola e punti. */
fun nickUgualeIndirizzo(from: String): Boolean {
    val (nick, indirizzo) = nickEIndirizzo(from) ?: return false
    val n = normalizza(nick)
    return n.length >= MIN_LUNGHEZZA && n == normalizza(indirizzo)
}
