/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6 (sondaggio sui gruppi) — CERCARE UN MESSAGGIO PER MESSAGE-ID.
 *
 * La usa «Ricostruisci discussione» e la usera' il Message-ID cliccabile (voce
 * F2 del quaderno): per questo non sa niente di discussioni ne' di database.
 * Si chiedono le sole INTESTAZIONI: e' cio' che fa la sync, e il corpo arriva
 * quando il messaggio si apre.
 *
 * IL NUMERO NEL GRUPPO, verificato alla fonte:
 *  - RFC 3977 §6.2.1: per Message-ID il numero nella risposta e' di norma 0. Un
 *    server puo' darne uno vero solo se l'articolo e' nel gruppo selezionato e
 *    quel numero riporta allo stesso articolo: se non e' zero, ci si puo' contare;
 *  - RFC 5536 §3.2.14: `Xref` e' facoltativa, la scrive l'ultimo server che ha
 *    trattato l'articolo, e la forma del locatore dipende dal server. Quindi
 *    vale solo se e' un numero, e solo dopo averlo verificato con STAT: un server
 *    che passa tale e quale la Xref di un altro darebbe il numero di un altro
 *    articolo. Una dichiarazione e' una promessa, non una prova.
 *  - numero 0 = non si sa, o l'articolo non e' in questo gruppo: decide chi
 *    chiama cosa farne.
 *
 * HEAD per Message-ID non cambia il gruppo selezionato (§6.2.1). Niente Android
 * qui: si prova in CoreTests, anche contro un server finto.
 */
package org.openusenet.reader.nntp

/** Esito di una ricerca. Un guasto di rete non e' un esito: resta un'eccezione. */
sealed interface EsitoRicerca {
    /** [overview] ha `number` 0 se il numero nel gruppo non e' noto. */
    data class Trovato(val overview: OverviewLine) : EsitoRicerca

    /** 430: il server non ce l'ha, scaduto o mai arrivato. */
    data object NonDisponibile : EsitoRicerca
}

/**
 * Il numero dell'articolo in [gruppo] secondo una riga
 * `Xref: server gruppo:numero gruppo:numero`, o null. La prima parola e' il nome
 * del server, e un locatore non numerico non vale.
 */
fun numeroDaXref(xref: String?, gruppo: String): Long? {
    val parole = xref?.trim()?.split(Regex("\\s+")) ?: return null
    return parole.drop(1).firstNotNullOfOrNull { locazione ->
        val i = locazione.lastIndexOf(':')
        if (i > 0 && locazione.substring(0, i) == gruppo) {
            locazione.substring(i + 1).toLongOrNull()?.takeIf { it > 0 }
        } else null
    }
}

/** Le intestazioni grezze, decodificate come fa la sync con l'overview. */
fun intestazioniDaTesta(righe: List<ByteArray>): Map<String, String> =
    MimeUtil.splitArticle(righe.map { MimeUtil.smartDecodeHeader(it) } + "").first

/** La stessa riga che la sync ricava da OVER: da qui in poi la strada e' quella. */
fun overviewDaIntestazioni(intestazioni: Map<String, String>, messageId: String, numero: Long) = OverviewLine(
    number = numero,
    subject = MimeUtil.decodeWords(intestazioni["subject"].orEmpty()),
    from = MimeUtil.decodeWords(intestazioni["from"].orEmpty()),
    date = intestazioni["date"].orEmpty(),
    messageId = messageId,
    references = intestazioni["references"].orEmpty(),
    bytes = intestazioni["bytes"]?.trim()?.toLongOrNull() ?: 0,
    lines = intestazioni["lines"]?.trim()?.toIntOrNull() ?: 0,
)

/**
 * Cerca [messageId] (con le parentesi angolari) sul server di [client].
 * [gruppo]: quello SELEZIONATO con GROUP, se serve il numero; null se non serve.
 */
fun cercaPerMessageId(client: NntpClient, messageId: String, gruppo: String? = null): EsitoRicerca {
    val testa = try {
        client.headConNumero(messageId)
    } catch (e: NntpException) {
        if (e.code == 430) return EsitoRicerca.NonDisponibile
        throw e
    }
    val intestazioni = intestazioniDaTesta(testa.righe)
    val numero = when {
        gruppo == null -> 0L
        testa.numero > 0 -> testa.numero
        else -> numeroDaXref(intestazioni["xref"], gruppo)
            ?.takeIf { client.messageIdDelNumero(it) == messageId } ?: 0L
    }
    return EsitoRicerca.Trovato(overviewDaIntestazioni(intestazioni, messageId, numero))
}
