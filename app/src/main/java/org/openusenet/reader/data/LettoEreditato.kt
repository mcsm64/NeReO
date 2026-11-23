/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.5 / 194 (BIG Umberto, 20-21/11) — IL LETTO SEGUE IL MESSAGE-ID ANCHE
 * QUANDO IL MESSAGGIO ARRIVA.
 *
 * Il caso: «rossi isolati tra i verdi», in discussioni di giorni prima, anche
 * nello stesso gruppo su due server (corradoroberto e paganini). Un messaggio
 * letto su un server arrivava sull'altro giorni dopo, alla sua data vecchia, e
 * nasceva non letto. Dalla 1.4 il letto segue il Message-ID quando lo SEGNI;
 * una copia che arrivava dopo non guardava le altre.
 *
 * Stessa cosa per «Ricostruisci discussione» (1.6): i messaggi ricostruiti
 * nascono letti; quando arriva quello vero col suo numero, la riga ricostruita
 * si toglie e quella vera entrava non letta. La riga ricostruita e' una copia
 * letta come le altre: la si guarda PRIMA di toglierla.
 *
 * Solo con «il letto segue il Message-ID» acceso, come la propagazione.
 */
package org.openusenet.reader.data

/** [articoli] con `read = true` se il loro Message-ID e' fra [giaLetti]. */
fun ereditaLetto(articoli: List<Article>, giaLetti: Set<String>): List<Article> =
    if (giaLetti.isEmpty()) articoli
    else articoli.map { if (!it.read && it.messageId in giaLetti) it.copy(read = true) else it }
