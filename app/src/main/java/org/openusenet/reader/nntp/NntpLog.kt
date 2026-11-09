/*
 * OpenUsenet — text-only Usenet newsreader for Android
 * Copyright (C) 2026  OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Registratore diagnostico del dialogo NNTP ("scatola nera").
 *
 * Normalmente SPENTO. Quando attivato dall'utente (Manutenzione →
 * Diagnostica), tiene in memoria le ultime [MAX_LINES] righe del dialogo
 * client/server, ciascuna marcata "C: " (comando inviato), "S: " (risposta
 * ricevuta), "D: " (dato diagnostico CALCOLATO da NeReO, non un byte del
 * protocollo) o "==" (intestazione/piè di sessione). Serve a capire su quale
 * comando un server risponde in modo anomalo (es. il "400 Unrecognized
 * command" di Astraweb su OVER) — e, dalla 1.2.8, anche a capire QUANTE righe
 * di overview siano davvero arrivate: proposta di Bingo3331 + HAL/ChatGPT
 * (1 settembre 2026) per diagnosticare i buchi nel backfill di Astraweb.
 *
 * PRIVACY: le credenziali sono oscurate QUI, alla fonte, non lasciate
 * all'utente: AUTHINFO PASS e AUTHINFO USER escono sempre come "****".
 * Dalla 1.3.5 al "****" segue la sola FORMA del valore — lunghezza, spazi ai
 * bordi, presenza di caratteri non ASCII — che serve a diagnosticare un 481
 * senza rivelare il contenuto. Vedi [forma] per il compromesso, dichiarato.
 * Nulla viene scritto su disco: il buffer vive solo in RAM finché è attivo
 * e sparisce quando l'interruttore si spegne o l'app viene chiusa.
 *
 * Solo le RIGHE DI STATO delle risposte vengono registrate, non i blocchi
 * multilinea (LIST ACTIVE, OVER, ARTICLE…): così il buffer resta piccolo e
 * leggibile anche dopo una sincronizzazione intera. Le righe "D: " sono
 * RIEPILOGHI numerici calcolati sui blocchi multilinea (quante righe, quali
 * numeri mancano) — mai il contenuto vero degli articoli: nessuna nuova
 * superficie di rischio privacy rispetto a prima.
 */
package org.openusenet.reader.nntp

object NntpLog {

    /** Interruttore globale. Volatile: letto da thread di rete, scritto dalla UI. */
    @Volatile
    var enabled: Boolean = false

    /**
     * Versione dell'app mostrata nell'intestazione di sessione. NntpLog (come
     * NntpClient) non dipende da Android per restare testabile da riga di
     * comando: questo campo va impostato UNA VOLTA SOLA dal lato Android
     * (App.onCreate, da BuildConfig.VERSION_NAME) invece di leggerlo qui
     * direttamente. Stringa vuota finché nessuno lo imposta: l'intestazione
     * allora omette semplicemente la versione.
     */
    @Volatile
    var appVersion: String = ""

    // 1.6.2 / 189 (HAL): da 400 a 1500. Con 400 righe, un invio fallito notato
    // qualche minuto dopo era gia' uscito dal buffer al momento di esportare.
    // Non e' la soluzione (alzare il tetto sposta il problema): la soluzione e'
    // UltimoErrore, che copia le ultime righe nel momento dell'errore. Questo
    // e' il margine per chi esporta il log intero. Stima, non misura: qualche
    // centinaio di KB di RAM al massimo.
    private const val MAX_LINES = 1500
    private val buffer = ArrayDeque<String>(MAX_LINES)

    /** Registra un comando inviato dal client (credenziali oscurate). */
    fun client(cmd: String) {
        if (!enabled) return
        add("C: " + redact(cmd))
    }

    /**
     * Registra la riga di stato di una risposta del server. [durataMs], se
     * nota, è il tempo trascorso dall'invio del comando a questa risposta
     * (punto 2 della proposta HAL/Bingo3331): utile anche per capire quando
     * un server è solo LENTO, non guasto.
     */
    fun server(code: Int, text: String, durataMs: Long? = null) {
        if (!enabled) return
        val suffisso = if (durataMs != null) " [${durataMs} ms]" else ""
        add("S: $code $text$suffisso")
    }

    /**
     * Riga diagnostica CALCOLATA da NeReO (non un dato grezzo del protocollo):
     * riepiloghi, esiti di comandi diagnostici manuali (LISTGROUP/STAT), o
     * un blocco multilinea interrotto a metà.
     */
    fun diag(text: String) {
        if (!enabled) return
        add("D: $text")
    }

    /**
     * Riepilogo di una risposta OVER/XOVER multilinea appena ricevuta: quante
     * righe sono arrivate rispetto all'intervallo richiesto, quali numeri
     * mancano (compressi in intervalli — un buco di 5000 articoli non deve
     * riempire il buffer una riga alla volta) e se ci sono duplicati. Questo è
     * il punto 1, il più importante secondo HAL stesso: prima si vedeva solo
     * "S: 224 Overview Information Follows" e nulla diceva quante righe fossero
     * DAVVERO arrivate — il problema che nascondeva i buchi nel backfill.
     */
    fun overSummary(low: Long, high: Long, numeri: List<Long>) {
        if (!enabled) return
        val distinti = numeri.distinct().sorted()
        val duplicati = numeri.size - distinti.size
        val mancanti = ArrayList<LongRange>()
        var atteso = low
        for (n in distinti) {
            if (n > atteso) mancanti.add(atteso until n)
            atteso = maxOf(atteso, n + 1)
        }
        if (atteso <= high) mancanti.add(atteso..high)
        val missingCount = mancanti.sumOf { it.last - it.first + 1 }
        val intervalloRicevuto = if (distinti.isNotEmpty()) " (${distinti.first()}-${distinti.last()})" else ""
        val notaDuplicati = if (duplicati > 0) ", $duplicati duplicati" else ""
        add(
            "D: overview $low-$high: ${numeri.size} righe ricevute$intervalloRicevuto, " +
                "$missingCount mancanti [${formattaRanges(mancanti)}]$notaDuplicati",
        )
    }

    /** Comprime una lista di intervalli in una stringa leggibile, troncando quelli in eccesso. */
    private fun formattaRanges(ranges: List<LongRange>, maxMostrati: Int = 20): String {
        if (ranges.isEmpty()) return "nessuno"
        val mostrati = ranges.take(maxMostrati).joinToString(", ") { r ->
            if (r.first == r.last) "${r.first}" else "${r.first}-${r.last}"
        }
        return if (ranges.size > maxMostrati) "$mostrati, +${ranges.size - maxMostrati} altri" else mostrati
    }

    /**
     * Intestazione di sessione: una riga scritta una volta a ogni connessione
     * NNTP riuscita (punto 3 della proposta HAL/Bingo3331). [sessionId] è un
     * identificatore breve generato da NntpClient, utile solo per far
     * corrispondere questa riga al piè corrispondente quando più sessioni si
     * susseguono nello stesso buffer (es. syncAllSubscribed su più server).
     */
    fun sessionStart(host: String, port: Int, ssl: Boolean, sessionId: String) {
        if (!enabled) return
        val versione = if (appVersion.isNotEmpty()) " — NeReO $appVersion" else ""
        add("== sessione $sessionId avviata: $host:$port ${if (ssl) "TLS" else "in chiaro"}$versione ==")
    }

    /** Piè di sessione: scritto una volta alla chiusura della connessione (anche dopo un errore). */
    fun sessionEnd(sessionId: String, durataMs: Long, comandi: Int, errori: Int) {
        if (!enabled) return
        add("== sessione $sessionId chiusa dopo ${durataMs} ms — $comandi comandi, $errori errori ==")
    }

    @Synchronized
    private fun add(line: String) {
        if (buffer.size >= MAX_LINES) buffer.removeFirst()
        buffer.addLast(line)
    }

    /** Il dialogo registrato finora, una riga per elemento. */
    @Synchronized
    fun snapshot(): String = buffer.joinToString("\n")

    /** 1.6.2: le ultime [n] righe, dalla piu' vecchia alla piu' recente (per UltimoErrore). */
    @Synchronized
    fun ultimeRighe(n: Int): List<String> = buffer.toList().takeLast(n)

    /** Numero di righe attualmente in buffer (per l'interfaccia). */
    @Synchronized
    fun size(): Int = buffer.size

    /** Svuota il buffer (all'attivazione, per partire puliti). */
    @Synchronized
    fun clear() {
        buffer.clear()
    }

    /**
     * Oscura le credenziali. Il confronto è su maiuscolo perché i verbi NNTP
     * non sono sensibili al maiuscolo/minuscolo (RFC 3977 §3.1).
     */
    private fun redact(cmd: String): String {
        val up = cmd.uppercase()
        return when {
            up.startsWith("AUTHINFO PASS") -> "AUTHINFO PASS ****" + forma(cmd, "AUTHINFO PASS")
            up.startsWith("AUTHINFO USER") -> "AUTHINFO USER ****" + forma(cmd, "AUTHINFO USER")
            // 1.2.12 (audit di sicurezza): rete di sicurezza per il FUTURO.
            // Prima erano coperte solo le due forme sopra; oggi sono le uniche
            // che il client emette, quindi non si perdeva nulla — ma il giorno
            // in cui la roadmap NNTP portasse AUTHINFO SASL o GENERIC (RFC 4643
            // §2.3-2.4), le credenziali sarebbero finite nel log in chiaro
            // SENZA che nulla lo segnalasse. Meglio oscurare per prefisso: se
            // un comando comincia per AUTHINFO, tutto ciò che segue il verbo è
            // trattato come segreto.
            up.startsWith("AUTHINFO ") -> "AUTHINFO ****"
            else -> cmd
        }
    }

    /**
     * 1.3.5 — la FORMA di una credenziale, senza il suo contenuto.
     *
     * Nasce da un caso reale: un utente su Solani riceveva 481 con credenziali
     * che gli funzionavano da anni su un altro client. Il log diceva
     * "AUTHINFO PASS ****", cioe' tutto e niente: il server le aveva ricevute
     * e rifiutate, e per capire PERCHE' si poteva solo tirare a indovinare —
     * uno spazio di troppo da un incolla? una lettera accentata codificata
     * diversamente? Nessuno dei due si vede, nemmeno scrivendo la password in
     * chiaro sullo schermo.
     *
     * Queste tre informazioni si vedono invece subito, e non permettono di
     * ricostruire la credenziale:
     *  - la LUNGHEZZA in caratteri (chi la conosce si accorge se non torna);
     *  - gli SPAZI AI BORDI, che sono il difetto piu' probabile e il piu'
     *    invisibile;
     *  - la presenza di caratteri NON ASCII, che dipendono dalla codifica di
     *    trasporto e possono partire diversi da come sono stati digitati.
     *
     * Compromesso dichiarato: la lunghezza e' un'informazione in piu' rispetto
     * a prima. E' accettabile perche' il log e' spento per impostazione
     * predefinita, vive solo in RAM, e lo condivide l'utente quando decide lui
     * — mentre il beneficio e' chiudere un'intera classe di segnalazioni al
     * primo log ricevuto invece che dopo tre scambi di messaggi.
     */
    private fun forma(cmd: String, verbo: String): String {
        val valore = cmd.substring(minOf(verbo.length, cmd.length)).removePrefix(" ")
        if (valore.isEmpty()) return " (vuoto)"
        val note = ArrayList<String>(3)
        note.add("${valore.length} caratteri")
        if (valore.first().isWhitespace()) note.add("SPAZIO INIZIALE")
        if (valore.last().isWhitespace()) note.add("SPAZIO FINALE")
        if (valore.any { it.code < 32 || it.code > 126 }) note.add("caratteri non ASCII")
        return note.joinToString(", ", prefix = " (", postfix = ")")
    }
}
