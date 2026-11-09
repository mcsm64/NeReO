/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.2 / 189 — DATA E ORA, IN UN POSTO SOLO.
 *
 * Dalla discussione su Usenet di BillH, Dave Royal, Kerr-Mudd John e Sn!pe (con
 * s|b): giorno/mese e ore a 24 erano scritti a mano in sei funzioni (elenco,
 * riga autore, intestazione di lettura, introduzione alla citazione, identita'
 * TLS, ultimo controllo aggiornamenti). Qui le forme sono una volta sola, e le
 * scelte dell'utente (ordine della data, 12/24 ore, fuso) valgono per tutte.
 *
 * Valori predefiniti IDENTICI a prima: GG/MM/AA, 24 ore, fuso del telefono.
 *
 * NON passano di qui, di proposito: i nomi dei file (backup, log: restano
 * ordinabili), i tempi dentro CrashLog e UltimoErrore, il formato del kill-file.
 *
 * Kotlin puro: fuso del telefono, lingua e «adesso» sono parametri, cosi' i test
 * provano ogni combinazione senza toccare il telefono.
 *
 * Per aggiungere un giorno «Automatico» (regione del telefono) basta un valore
 * in [OrdineData] e un ramo in [dataCompleta], [dataBreve] e [giornoMese]
 * (`DateTimeFormatter.ofLocalizedDate`): nessun chiamante cambia.
 */
package org.openusenet.reader.text

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ordine di giorno, mese e anno. */
enum class OrdineData { GMA, MGA, ISO }

/** 24 ore, o 12 ore con AM/PM (sempre in inglese, qualunque sia la lingua). */
enum class Orologio { H24, H12 }

/** Fuso in cui si MOSTRANO le ore. */
enum class FusoOrario { TELEFONO, UTC }

data class PreferenzeDataOra(
    val ordine: OrdineData = OrdineData.GMA,
    val orologio: Orologio = Orologio.H24,
    val fuso: FusoOrario = FusoOrario.TELEFONO,
) {
    companion object {
        /** Da tre stringhe salvate; un valore ignoto torna al predefinito, mai un crash. */
        fun da(ordine: String?, orologio: String?, fuso: String?) = PreferenzeDataOra(
            OrdineData.entries.firstOrNull { it.name == ordine } ?: OrdineData.GMA,
            Orologio.entries.firstOrNull { it.name == orologio } ?: Orologio.H24,
            FusoOrario.entries.firstOrNull { it.name == fuso } ?: FusoOrario.TELEFONO,
        )
    }
}

object DataOra {
    // Le cifre sono quelle standard qualunque sia la lingua: Locale.ROOT per i
    // numeri, Locale.US per AM/PM (richiesta: indipendente dalla lingua dell'app).
    private fun f(schema: String, locale: Locale = Locale.ROOT) = DateTimeFormatter.ofPattern(schema, locale)

    private val ORA_24 = f("HH:mm")
    private val ORA_12 = f("h:mm a", Locale.US)
    private val COMPLETA_GMA = f("dd/MM/yyyy")
    private val COMPLETA_MGA = f("MM/dd/yyyy")
    private val ISO = f("yyyy-MM-dd")
    private val BREVE_GMA = f("dd/MM/yy")
    private val BREVE_MGA = f("MM/dd/yy")
    private val GM_GMA = f("dd/MM")
    private val GM_MGA = f("MM/dd")
    private val GM_ISO = f("MM-dd")
    private val SCARTO = f("xx")

    /** Il fuso in cui mostrare. */
    fun zona(p: PreferenzeDataOra, telefono: ZoneId = ZoneId.systemDefault()): ZoneId =
        if (p.fuso == FusoOrario.UTC) ZoneOffset.UTC else telefono

    fun inZona(epochSecondi: Long, p: PreferenzeDataOra, telefono: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
        Instant.ofEpochSecond(epochSecondi).atZone(zona(p, telefono))

    /** « UTC» se si mostra in UTC, altrimenti niente: dove un'ora nuda sarebbe un equivoco. */
    fun suffissoUtc(p: PreferenzeDataOra): String = if (p.fuso == FusoOrario.UTC) " UTC" else ""

    fun ora(t: ZonedDateTime, p: PreferenzeDataOra): String =
        (if (p.orologio == Orologio.H12) ORA_12 else ORA_24).format(t)

    /** 07/11/2026 · 11/07/2026 · 2026-11-07 */
    fun dataCompleta(t: ZonedDateTime, p: PreferenzeDataOra): String = when (p.ordine) {
        OrdineData.GMA -> COMPLETA_GMA
        OrdineData.MGA -> COMPLETA_MGA
        OrdineData.ISO -> ISO
    }.format(t)

    /** 07/11/26 · 11/07/26 · 2026-11-07 (ISO sempre completa: decisione di Marco, 9/11). */
    fun dataBreve(t: ZonedDateTime, p: PreferenzeDataOra): String = when (p.ordine) {
        OrdineData.GMA -> BREVE_GMA
        OrdineData.MGA -> BREVE_MGA
        OrdineData.ISO -> ISO
    }.format(t)

    /** 07/11 · 11/07 · 11-07: la data di quest'anno, senza anno. */
    fun giornoMese(t: ZonedDateTime, p: PreferenzeDataOra): String = when (p.ordine) {
        OrdineData.GMA -> GM_GMA
        OrdineData.MGA -> GM_MGA
        OrdineData.ISO -> GM_ISO
    }.format(t)

    /** Riga dell'autore nell'elenco: «07/11/26 14:45». Senza «UTC»: la colonna e' stretta. */
    fun dataOraBreve(t: ZonedDateTime, p: PreferenzeDataOra): String = dataBreve(t, p) + " " + ora(t, p)

    /** Data piena dell'intestazione: «07/11/2026 14:45», con « UTC» se serve. */
    fun dataOraCompleta(t: ZonedDateTime, p: PreferenzeDataOra): String =
        dataCompleta(t, p) + " " + ora(t, p) + suffissoUtc(p)

    /** «lunedì 14:45», nella lingua dell'app. Senza «UTC»: lo aggiunge chi chiama. */
    fun giornoSettimanaOra(t: ZonedDateTime, p: PreferenzeDataOra, locale: Locale): String =
        DateTimeFormatter.ofPattern("EEEE", locale).format(t) + " " + ora(t, p)

    /**
     * La data dell'elenco: oggi solo l'ora, quest'anno senza anno, prima la data
     * breve. «Oggi» si calcola nello STESSO fuso della data: in UTC, un messaggio
     * delle 00:30 locali non deve dirsi di ieri.
     */
    fun elenco(
        epochSecondi: Long,
        p: PreferenzeDataOra,
        telefono: ZoneId = ZoneId.systemDefault(),
        adesso: Instant = Instant.now(),
    ): String {
        val t = inZona(epochSecondi, p, telefono)
        // atZone e non LocalDate.ofInstant: quella c'e' solo da Android 14.
        val oggi: LocalDate = adesso.atZone(t.zone).toLocalDate()
        return when {
            t.toLocalDate() == oggi -> ora(t, p)
            t.year == oggi.year -> giornoMese(t, p)
            else -> dataBreve(t, p)
        }
    }

    /** Scarto numerico RFC 5322 dell'ora indicata: «+0100», «-0500», «+0530». */
    fun scarto(t: ZonedDateTime): String = SCARTO.format(t)

    /** 2026-11-07 (UTC). */
    fun dataUtc(epochSecondi: Long): String = ISO.format(Instant.ofEpochSecond(epochSecondi).atOffset(ZoneOffset.UTC))

    /** «14:45 UTC»: sempre 24 ore, e' la forma internazionale. */
    fun oraUtc(epochSecondi: Long): String =
        ORA_24.format(Instant.ofEpochSecond(epochSecondi).atOffset(ZoneOffset.UTC)) + " UTC"

    // ---------------------------------------------------------------- lettura

    private val RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME
    // Alcuni server omettono il giorno della settimana
    private val NO_DOW = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss Z", Locale.US)

    /**
     * Data Usenet con il SUO scarto (RFC 5322 è un far west), o null se non si
     * legge. Era il corpo di `NewsRepository.parseDate`, che ora la chiama:
     * stessa regola, ma lo scarto dell'autore non si butta piu' via.
     */
    fun leggiDataUsenet(raw: String): ZonedDateTime? {
        val cleaned = raw.trim().substringBefore(" (").trim()
        runCatching { return ZonedDateTime.parse(cleaned, RFC1123) }
        runCatching { return ZonedDateTime.parse(cleaned, NO_DOW) }
        // ultima spiaggia: butto via il giorno della settimana e riprovo
        val noDow = cleaned.substringAfter(", ")
        runCatching { return ZonedDateTime.parse(noDow, NO_DOW) }
        return null
    }

    /**
     * Il valore della riga `Date:` di un blocco di intestazioni (una per riga,
     * come `Article.headersRaw`), righe di continuazione comprese. null se manca.
     */
    fun rigaDate(intestazioni: String?): String? {
        if (intestazioni.isNullOrBlank()) return null
        val righe = intestazioni.lines()
        val i = righe.indexOfFirst { it.startsWith("Date:", ignoreCase = true) }
        if (i < 0) return null
        val valore = StringBuilder(righe[i].substring(5).trim())
        var j = i + 1
        while (j < righe.size && righe[j].isNotEmpty() && righe[j].first().isWhitespace()) {
            valore.append(' ').append(righe[j].trim())
            j++
        }
        return valore.toString().takeIf { it.isNotBlank() }
    }
}
