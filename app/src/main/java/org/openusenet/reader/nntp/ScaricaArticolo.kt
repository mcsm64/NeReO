/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.nntp

/*
 * 1.6.5 (BIG Umberto, 18/11, log su Solani, INN 2.7.1) — L'ARTICOLO PER NUMERO
 * QUANDO IL MESSAGE-ID NON BASTA.
 *
 * Nel gruppo, overview e LISTGROUP avevano i messaggi; `ARTICLE <id>` rispondeva
 * 430, `ARTICLE <numero>` (da un altro lettore) li dava. NeReO chiedeva solo per
 * Message-ID e diceva «probabilmente scaduto»: falso.
 *
 * RFC 3977 §6.2.1.1: per Message-ID `220 0|n message-id` o `430`; per numero, a
 * gruppo selezionato, `220 n message-id`, `412` o `423`.
 *
 * La regola, in un posto solo per le tre strade del corpo (lettura, risposte in
 * anticipo, «scarica corpi»):
 *  - si chiede per Message-ID, come prima;
 *  - solo dopo un 430, e solo se il numero e' positivo (i ricostruiti hanno
 *    numeri negativi, inventati da noi), UN tentativo per numero;
 *  - la risposta vale solo se il suo Message-ID coincide con quello atteso: un
 *    server che ha rinumerato darebbe un ALTRO articolo. Se non coincide, vale
 *    come il 430 di prima.
 * Il gruppo lo seleziona il chiamante, come gia' faceva. Niente memoria per
 * server: il costo e' un comando, e solo dopo un 430.
 */
fun scaricaArticolo(client: NntpClient, messageId: String, numero: Long): List<ByteArray> {
    if (messageId.isEmpty()) return client.article(numero.toString())
    try {
        return client.article(messageId)
    } catch (e: NntpException) {
        if (e.code != 430 || numero <= 0) throw e
        NntpLog.diag("430 per Message-ID $messageId, riprovo per numero $numero")
        val raw = try {
            client.article(numero.toString())
        } catch (e2: NntpException) {
            NntpLog.diag("per numero $numero: ${e2.code}, il server non trova l'articolo")
            throw e2
        }
        val trovato = messageIdDi(raw)
        if (trovato != messageId.trim()) {
            NntpLog.diag("per numero $numero: Message-ID diverso ($trovato), scartato")
            throw e
        }
        NntpLog.diag("per numero $numero: trovato")
        return raw
    }
}

/** Il Message-ID dell'intestazione di un articolo grezzo, o "" se manca. */
fun messageIdDi(raw: List<ByteArray>): String =
    MimeUtil.splitArticleRaw(raw).first["message-id"].orEmpty().trim()
