/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Decodifica RFC 2047 (encoded-words negli header), quoted-printable e
 * parsing minimale degli header di un articolo. Puro Kotlin/JVM.
 */
package org.openusenet.reader.nntp

import java.nio.charset.Charset
import java.util.Base64

object MimeUtil {

    private val ENCODED_WORD = Regex("""=\?([^?]+)\?([bBqQ])\?([^?]*)\?=""")

    /** Decodifica tutte le encoded-word RFC 2047 in una stringa header. */
    fun decodeWords(raw: String): String {
        if (!raw.contains("=?")) return raw
        // encoded-word adiacenti separate da spazi vanno fuse (RFC 2047 §6.2)
        val collapsed = raw.replace(Regex("""\?=\s+=\?"""), "?==?")
        return ENCODED_WORD.replace(collapsed) { m ->
            val cs = safeCharset(m.groupValues[1])
            val enc = m.groupValues[2]
            val payload = m.groupValues[3]
            try {
                when (enc.lowercase()) {
                    "b" -> String(Base64.getMimeDecoder().decode(payload), cs)
                    "q" -> decodeQ(payload, cs)
                    else -> m.value
                }
            } catch (_: Exception) {
                m.value
            }
        }
    }

    /** Q-encoding: come quoted-printable ma '_' = spazio. */
    private fun decodeQ(s: String, cs: Charset): String {
        val bytes = ArrayList<Byte>(s.length)
        var i = 0
        while (i < s.length) {
            when (val c = s[i]) {
                '_' -> { bytes.add(0x20); i++ }
                '=' -> {
                    if (i + 2 < s.length) {
                        val hex = s.substring(i + 1, i + 3).toIntOrNull(16)
                        if (hex != null) { bytes.add(hex.toByte()); i += 3 }
                        else { bytes.add(c.code.toByte()); i++ }
                    } else { bytes.add(c.code.toByte()); i++ }
                }
                else -> { bytes.add(c.code.toByte()); i++ }
            }
        }
        return String(bytes.toByteArray(), cs)
    }

    /** Quoted-printable per il corpo (RFC 2045 §6.7). */
    fun decodeQuotedPrintable(lines: List<String>, cs: Charset): String {
        val sb = StringBuilder()
        for (line in lines) {
            var l = line
            val soft = l.endsWith("=")
            if (soft) l = l.dropLast(1)
            val bytes = ArrayList<Byte>(l.length)
            var i = 0
            while (i < l.length) {
                val c = l[i]
                if (c == '=' && i + 2 < l.length + 1 && i + 3 <= l.length) {
                    val hex = l.substring(i + 1, i + 3).toIntOrNull(16)
                    if (hex != null) { bytes.add(hex.toByte()); i += 3; continue }
                }
                bytes.add(c.code.toByte()); i++
            }
            sb.append(String(bytes.toByteArray(), cs))
            if (!soft) sb.append('\n')
        }
        return sb.toString()
    }

    /**
     * Codifica una stringa header in encoded-word RFC 2047 (B-encoding UTF-8)
     * solo se contiene caratteri non-ASCII; altrimenti la lascia com'è.
     *
     * 1.3.6 — SPEZZATA E PIEGATA, come vuole la RFC.
     *
     * Fino alla 1.3.5 produceva UNA SOLA encoded-word, senza limite di
     * lunghezza. La RFC 2047 §2 dice che una encoded-word non puo' superare
     * **75 caratteri**, delimitatori compresi. Un oggetto italiano di lunghezza
     * normale sfondava il limite abbondantemente: misurato, «Re: Non è
     * possibile scaricare l'elenco dei gruppi dal server perché il certificato
     * è scaduto» usciva come una riga da **140 caratteri**.
     *
     * Non e' un dettaglio formale: un decodificatore ha il diritto di non
     * ricomporre una encoded-word fuori misura, e quello che resta negli
     * archivi altrui e' irrecuperabile. Segnalato da **Adam H. Kerman** su un
     * gruppo anglofono il 13 settembre 2026, con la domanda giusta: «at what
     * point do you parse for the need to encode?».
     *
     * COME SI SPEZZA. Più encoded-word si separano con CRLF + spazio (folding,
     * RFC 5322 §2.2.3) e il decodificatore NON inserisce spazio fra due
     * encoded-word adiacenti: per questo si puo' tagliare in mezzo a una parola
     * senza alterare il testo.
     *
     * DOVE SI TAGLIA. Il base64 codifica BYTE, ma il taglio va fatto su confini
     * di CODE POINT: spezzare una sequenza UTF-8 multi-byte produrrebbe
     * mojibake. Le coppie surrogate (emoji) sono gestite da `codePointAt` +
     * `charCount` — provate nel prototipo, dove un taglio ingenuo su `char`
     * avrebbe rotto proprio quelle.
     *
     * IL VALORE RESTITUITO PUO' CONTENERE CRLF. Chi lo mette in un header deve
     * spezzarlo in righe separate prima di darlo a [NntpClient.post], che
     * scrive un elemento per riga: le righe di continuazione cominciano gia'
     * con uno spazio, quindi sono a norma cosi' come sono.
     */
    fun encodeWord(s: String): String {
        if (s.all { it.code in 32..126 }) return s
        val apri = "=?UTF-8?B?"
        val chiudi = "?="
        // 75 caratteri totali meno i delimitatori; il base64 va a multipli di 4
        val maxB64 = ((75 - apri.length - chiudi.length) / 4) * 4
        val maxByte = (maxB64 / 4) * 3
        val parole = ArrayList<String>()
        val pezzo = StringBuilder()
        var byteCorrenti = 0

        fun chiudiPezzo() {
            if (pezzo.isEmpty()) return
            parole.add(apri + Base64.getEncoder().encodeToString(pezzo.toString().toByteArray(Charsets.UTF_8)) + chiudi)
            pezzo.setLength(0)
            byteCorrenti = 0
        }

        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            val larghezza = Character.charCount(cp)
            val n = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8).size
            if (byteCorrenti + n > maxByte) chiudiPezzo()
            pezzo.appendCodePoint(cp)
            byteCorrenti += n
            i += larghezza
        }
        chiudiPezzo()
        return parole.joinToString("\r\n ")
    }

    fun safeCharset(name: String): Charset = try {
        // "utf-8*it" → tolgo l'eventuale language tag RFC 2231
        Charset.forName(name.substringBefore('*').trim())
    } catch (_: Exception) {
        Charsets.UTF_8
    }

    /**
     * 1.4.4 — IL LETTORE DI NEWS DICHIARATO DALL'AUTORE, in una riga sola.
     *
     * Serve al campo filtro `NEWSREADER`.
     *
     * PERCHE' TRE INTESTAZIONI E NON UNA. Non esiste un'unica convenzione:
     * `User-Agent` e' quella moderna (RFC 5536 §3.2.13 la elenca fra le
     * intestazioni non obbligatorie), `X-Newsreader` e `X-Mailer` sono quelle
     * storiche e sono ancora in giro — un lettore vecchio, che e' esattamente
     * il caso d'uso, e' il piu' probabile a usarle. Guardarne una sola
     * significherebbe far mancare il match proprio a chi si vuole prendere.
     *
     * Si uniscono con " | " invece di sceglierne una: una regola `contains`
     * cerca comunque la sua sottostringa, e chi scrive una regex sa che le
     * alternative sono separate. Cosi' non c'e' un ordine di precedenza da
     * ricordare, e un articolo che le dichiara entrambe non ne perde nessuna.
     *
     * Null (e non stringa vuota) quando nessuna delle tre c'e': il motore
     * filtri distingue "regola non valutabile" da "valore vuoto" e nel primo
     * caso salta la regola senza toccare il punteggio.
     */
    fun lettoreDiNews(headers: Map<String, String>): String? =
        listOfNotNull(
            headers["user-agent"],
            headers["x-newsreader"],
            headers["x-mailer"],
        ).map { it.trim() }
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" | ")

    /**
     * Come sopra, partendo dalle intestazioni gia' memorizzate come testo
     * (`Article.headersRaw`). UNA SOLA definizione della regola, due modi di
     * entrarci: chi ha gia' la mappa non la ri-analizza, chi ha il testo passa
     * da [splitArticle] come fanno `crosspostGroups` e `followupTo`.
     */
    fun lettoreDiNews(headersRaw: String?): String? {
        if (headersRaw.isNullOrBlank()) return null
        val (headers, _) = splitArticle(headersRaw.lines() + "")
        return lettoreDiNews(headers)
    }

    /**
     * 1.5.7 / 175 — CIO' CHE I FILTRI LEGGONO DALLE INTESTAZIONI COMPLETE, in
     * una passata sola: il lettore di news (campo NEWSREADER) e tutte le
     * intestazioni, «nome: valore» una per riga, nomi in minuscolo (campo
     * HEADERS). Due campi, un'analisi: chi ha il testo non lo divide due volte.
     */
    data class IntestazioniFiltri(val lettore: String?, val tutte: String?)

    fun intestazioniPerFiltri(headers: Map<String, String>): IntestazioniFiltri =
        IntestazioniFiltri(
            lettore = lettoreDiNews(headers),
            tutte = headers.entries
                .takeIf { it.isNotEmpty() }
                ?.joinToString("\n") { (nome, valore) -> "${nome.lowercase()}: ${valore.trim()}" },
        )

    fun intestazioniPerFiltri(headersRaw: String?): IntestazioniFiltri {
        if (headersRaw.isNullOrBlank()) return IntestazioniFiltri(null, null)
        val (headers, _) = splitArticle(headersRaw.lines() + "")
        return intestazioniPerFiltri(headers)
    }

    /**
     * Divide un articolo grezzo in (headers, body) e "unfolda" gli header
     * su più righe (continuazione con spazio/tab, RFC 5322 §2.2.3).
     */
    fun splitArticle(rawLines: List<String>): Pair<Map<String, String>, List<String>> {
        val headers = LinkedHashMap<String, String>()
        var i = 0
        var currentKey: String? = null
        while (i < rawLines.size) {
            val line = rawLines[i]
            if (line.isEmpty()) { i++; break }
            if ((line.startsWith(" ") || line.startsWith("\t")) && currentKey != null) {
                // Unfolding delle righe di continuazione. ATTENZIONE al tipo:
                // - Face:/X-Face: sono base64 spezzato a 72 colonne. Vanno
                //   ricuciti SENZA spazio in mezzo, altrimenti la stringa base64
                //   si corrompe (in modo intermittente, a seconda di dove cade
                //   lo spazio rispetto ai gruppi di 4 → era il bug "alcune Face
                //   si vedono e altre no").
                // - Tutti gli altri header (Subject, References, ecc.) sono
                //   testo: qui lo spazio di separazione RFC ci vuole, altrimenti
                //   "Re: [Beta" + "12]" diventerebbe "Re: [Beta12]".
                val sep = if (currentKey == "face" || currentKey == "x-face") "" else " "
                headers[currentKey] = headers[currentKey] + sep + line.trim()
            } else {
                val idx = line.indexOf(':')
                if (idx > 0) {
                    currentKey = line.substring(0, idx).lowercase()
                    headers[currentKey] = line.substring(idx + 1).trim()
                }
            }
            i++
        }
        return headers to rawLines.subList(i, rawLines.size)
    }

    /**
     * Decodifica il corpo secondo Content-Type / Content-Transfer-Encoding
     * dichiarati negli header. Solo testo: qualunque cosa non-testo viene
     * segnalata come non supportata (siamo un reader NON binario, per scelta).
     */
    fun decodeTextBody(headers: Map<String, String>, bodyLines: List<String>): String {
        val ct = headers["content-type"] ?: "text/plain; charset=us-ascii"
        if (!ct.trim().lowercase().startsWith("text/") && !ct.trim().lowercase().startsWith("multipart/")) {
            return "[Contenuto non testuale: $ct — non supportato per scelta di progetto]"
        }
        val cs = Regex("""charset="?([^";\s]+)"?""", RegexOption.IGNORE_CASE)
            .find(ct)?.groupValues?.get(1)?.let { safeCharset(it) } ?: Charsets.UTF_8
        return when ((headers["content-transfer-encoding"] ?: "").trim().lowercase()) {
            "quoted-printable" -> decodeQuotedPrintable(bodyLines, cs)
            "base64" -> try {
                String(Base64.getMimeDecoder().decode(bodyLines.joinToString("")), cs)
            } catch (_: Exception) { bodyLines.joinToString("\n") }
            else -> bodyLines.joinToString("\n")
        }
    }

    // ------------------------------------------------------------------ byte-level (fix charset v0.8)

    /** ISO-8859-1: mappa 1:1 ogni byte 0x00..0xFF su un codepoint — decodifica "di trasporto" byte-preserving. */
    private val LATIN1: Charset = Charsets.ISO_8859_1

    /** ISO-8859-15 (Latin-9): come Latin-1 ma con €, œ, Š… fallback tipico dei gruppi europei legacy. */
    private val LATIN9: Charset = try { Charset.forName("ISO-8859-15") } catch (_: Exception) { Charsets.ISO_8859_1 }

    /**
     * Decodifica un header/overview line grezzo con euristica: prova UTF-8
     * STRETTO (segnala i byte malformati); se fallisce ⇒ ISO-8859-15.
     * Copre sia i subject moderni UTF-8 sia gli 8-bit Latin-9 dei gruppi it.*.
     * (Le encoded-word RFC 2047 restano gestite a valle da decodeWords.)
     */
    fun smartDecodeHeader(bytes: ByteArray): String = try {
        val dec = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        dec.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    } catch (_: Exception) {
        String(bytes, LATIN9)
    }

    /**
     * Come [splitArticle] ma partendo dai BYTE grezzi dell'articolo: gli
     * header vengono decodificati Latin-1 (byte-preserving, così ':' e i
     * fold ' '/'\t' restano riconoscibili) mentre il corpo resta in byte
     * per essere decodificato col charset giusto da [decodeTextBodyRaw].
     */
    fun splitArticleRaw(rawLines: List<ByteArray>): Pair<Map<String, String>, List<ByteArray>> {
        var sep = rawLines.indexOfFirst { it.isEmpty() }
        if (sep < 0) sep = rawLines.size
        val headerText = rawLines.subList(0, sep).map { String(it, LATIN1) }
        val (headers, _) = splitArticle(headerText + "")
        val body = if (sep < rawLines.size) rawLines.subList(sep + 1, rawLines.size) else emptyList()
        return headers to body
    }

    /**
     * Decodifica il corpo (byte grezzi) secondo Content-Type/CTE dichiarati.
     * Il caso "8bit/7bit/nessuno" ora decodifica i BYTE reali col charset
     * dichiarato: è questo che salva gli accenti nei gruppi Latin-9.
     */

    /**
     * 1.4 — UNA SOLA POLITICA DI DECODIFICA, non due.
     *
     * DA DOVE NASCE. Adam H. Kerman, 15 settembre 2026: *«What do you do in
     * followup if the quoted text is an 8-bit character set? […] We won't even
     * discuss what to do if the declaration is a mismatch.»* Quella seconda
     * meta', che lui aveva messo fra parentesi come troppo spinosa, conteneva un
     * difetto vero. Misurato su JDK 21 con il testo "perche' e' cosi'":
     * **cinque casi su sette perdevano il testo**, e in tutti e cinque
     * l'euristica che gia' usavamo PER LE INTESTAZIONI lo avrebbe recuperato.
     *
     * L'asimmetria che nessuno aveva messo a confronto:
     *  - intestazioni e overview -> [smartDecodeHeader]: UTF-8 stretto, e se
     *    fallisce Latin-9. Guarda i BYTE, non si fida della dichiarazione;
     *  - corpo -> prendeva la dichiarazione per buona, e in mancanza assumeva
     *    us-ascii. Un articolo senza Content-Type con byte a 8 bit — cioe' mezzo
     *    Usenet storico, e il pane dei gruppi italiani di vent'anni fa — aveva
     *    OGNI accento trasformato in U+FFFD.
     *
     * Il caso peggiore non e' quello: e' la dichiarazione SBAGLIATA (dichiara
     * latin-1, i byte sono UTF-8). Li' non compare nemmeno un U+FFFD: esce
     * mojibake formalmente valido, e noi lo ri-emettevamo in UTF-8 impeccabile.
     * Riciclaggio: il testo rotto usciva dalla nostra app col timbro di
     * conformita'.
     *
     * LA REGOLA, in tre passi:
     *
     *  1. byte UTF-8 validi in senso STRETTO **e** con almeno una sequenza
     *     multi-byte -> e' UTF-8, qualunque cosa dica la dichiarazione. E'
     *     un'euristica, non una legge: un testo Latin-1 puo' per caso essere
     *     UTF-8 valido. Nella prosa reale e' raro — una lettera accentata
     *     isolata non forma quasi mai una sequenza UTF-8 valida — ma va detto.
     *     La richiesta della sequenza multi-byte e' cio' che impedisce all'ASCII
     *     puro di prendere questa strada e scavalcare una dichiarazione giusta.
     *  2. altrimenti il charset DICHIARATO, se c'e'. Copre KOI8-R, Shift-JIS,
     *     tutto cio' che non e' europeo occidentale: li' la dichiarazione e'
     *     giusta e Latin-9 produrrebbe spazzatura.
     *  3. se non c'e' dichiarazione, o se quella dichiarata produce caratteri di
     *     sostituzione (U+FFFD), -> Latin-9. E' lo stesso ripiego di
     *     [smartDecodeHeader]: da qui in avanti l'app ha UNA politica sola.
     *
     * COSA NON C'E' QUI, DI PROPOSITO. Kerman ha aggiunto il pezzo di storia che
     * ci mancava: *«Traditionally, one simply used the character set in common
     * use in the group being posted to»* — cioe' al passo 3 il ripiego migliore
     * sarebbe il charset abituale della GERARCHIA (`ru.*` -> KOI8-R, `fj.*` ->
     * ISO-2022-JP, `pl.*` -> ISO-8859-2...). La tabella e' disegnata in
     * NeReO_bug_charset_corpo.md §3-bis e **non entra qui finche' non e'
     * MISURATA su articoli veri almeno per `it.*` e `ru.*`**: e' esattamente il
     * genere di tabella che questo progetto ha deciso di non costruire per
     * inferenza. Una riga sbagliata fa danni silenziosi come la dichiarazione
     * sbagliata che stiamo aggirando.
     */
    fun decodificaCorpoByte(bytes: ByteArray, dichiarato: Charset?): String {
        // Passo 1: UTF-8 stretto, ma solo se c'e' davvero una sequenza multi-byte.
        if (bytes.any { (it.toInt() and 0x80) != 0 }) {
            val stretto = runCatching {
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes))
                    .toString()
            }.getOrNull()
            if (stretto != null) return stretto
        }
        // Passo 2: il charset dichiarato, se non produce sostituzioni.
        if (dichiarato != null) {
            val testo = String(bytes, dichiarato)
            if (!testo.contains('\uFFFD')) return testo
        }
        // Passo 3: il ripiego, lo stesso delle intestazioni.
        return String(bytes, LATIN9)
    }

    fun decodeTextBodyRaw(headers: Map<String, String>, bodyLines: List<ByteArray>): String {
        val ct = headers["content-type"] ?: "text/plain; charset=us-ascii"
        val ctl = ct.trim().lowercase()
        if (!ctl.startsWith("text/") && !ctl.startsWith("multipart/")) {
            return "[Contenuto non testuale: $ct — non supportato per scelta di progetto]"
        }
        val cs = Regex("""charset="?([^";\s]+)"?""", RegexOption.IGNORE_CASE)
            .find(ct)?.groupValues?.get(1)?.let { safeCharset(it) } ?: Charsets.UTF_8
        return when ((headers["content-transfer-encoding"] ?: "").trim().lowercase()) {
            // QP e base64 sono ASCII-safe ⇒ leggo i byte come Latin-1 (byte-preserving) e riuso i decoder esistenti
            "quoted-printable" -> decodeQuotedPrintable(bodyLines.map { String(it, LATIN1) }, cs)
            // 1.4: anche qui i byte ci sono, quindi vale la regola dei tre passi.
            "base64" -> try {
                decodificaCorpoByte(
                    Base64.getMimeDecoder().decode(bodyLines.joinToString("") { String(it, LATIN1) }),
                    cs,
                )
            } catch (_: Exception) { bodyLines.joinToString("\n") { smartDecodeHeader(it) } }
            else -> {
                val out = java.io.ByteArrayOutputStream()
                bodyLines.forEachIndexed { i, b -> if (i > 0) out.write('\n'.code); out.write(b) }
                // 1.4: era `String(out.toByteArray(), cs)` — la dichiarazione presa
                // per buona. Vedi decodificaCorpoByte.
                decodificaCorpoByte(out.toByteArray(), cs)
            }
        }
    }
}
