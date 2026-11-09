/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.2 / 189 — data e ora in un posto solo (BillH, Dave Royal, Kerr-Mudd John,
 * Sn!pe su Usenet), e i segnaposto nuovi dell'introduzione (piano C di Marco).
 */
package org.openusenet.reader

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.openusenet.reader.data.Article
import org.openusenet.reader.data.Identity
import org.openusenet.reader.text.DataOra
import org.openusenet.reader.text.FusoOrario
import org.openusenet.reader.text.OrdineData
import org.openusenet.reader.text.Orologio
import org.openusenet.reader.text.PreferenzeDataOra
import org.openusenet.reader.ui.screens.introLine

class DataOraTest {
    private val roma = ZoneId.of("Europe/Rome")
    private fun utc(a: Int, m: Int, g: Int, h: Int, min: Int) =
        ZonedDateTime.of(a, m, g, h, min, 0, 0, ZoneOffset.UTC).toEpochSecond()

    /** 2026-11-07 14:45 UTC = 15:45 a Roma (ora solare). */
    private val e = utc(2026, 11, 7, 14, 45)
    private val predefinite = PreferenzeDataOra()

    @Test
    fun `i predefiniti sono quelli di prima, e un valore ignoto ci torna`() {
        assertEquals(PreferenzeDataOra(OrdineData.GMA, Orologio.H24, FusoOrario.TELEFONO), predefinite)
        assertEquals(predefinite, PreferenzeDataOra.da(null, null, null))
        assertEquals(predefinite, PreferenzeDataOra.da("XYZ", "13", "MARTE"))
        assertEquals(
            PreferenzeDataOra(OrdineData.ISO, Orologio.H12, FusoOrario.UTC),
            PreferenzeDataOra.da("ISO", "H12", "UTC"),
        )
    }

    @Test
    fun `tre formati per 24 e 12 ore per fuso del telefono e UTC`() {
        val completa = mapOf(
            OrdineData.GMA to listOf("07/11/2026", "07/11/26", "07/11"),
            OrdineData.MGA to listOf("11/07/2026", "11/07/26", "11/07"),
            OrdineData.ISO to listOf("2026-11-07", "2026-11-07", "11-07"),
        )
        val ore = mapOf(
            (Orologio.H24 to FusoOrario.TELEFONO) to "15:45",
            (Orologio.H24 to FusoOrario.UTC) to "14:45",
            (Orologio.H12 to FusoOrario.TELEFONO) to "3:45 PM",
            (Orologio.H12 to FusoOrario.UTC) to "2:45 PM",
        )
        for (ordine in OrdineData.entries) for (orologio in Orologio.entries) for (fuso in FusoOrario.entries) {
            val p = PreferenzeDataOra(ordine, orologio, fuso)
            val t = DataOra.inZona(e, p, roma)
            val (c, b, gm) = completa.getValue(ordine)
            val o = ore.getValue(orologio to fuso)
            val utc = if (fuso == FusoOrario.UTC) " UTC" else ""
            val caso = "$ordine $orologio $fuso"
            assertEquals(caso, c, DataOra.dataCompleta(t, p))
            assertEquals(caso, b, DataOra.dataBreve(t, p))
            assertEquals(caso, gm, DataOra.giornoMese(t, p))
            assertEquals(caso, o, DataOra.ora(t, p))
            assertEquals(caso, "$b $o", DataOra.dataOraBreve(t, p))
            assertEquals(caso, "$c $o$utc", DataOra.dataOraCompleta(t, p))
        }
    }

    @Test
    fun `elenco - oggi l'ora, quest'anno senza anno, prima la data breve`() {
        val adesso = Instant.ofEpochSecond(utc(2026, 11, 7, 20, 0))
        assertEquals("15:45", DataOra.elenco(e, predefinite, roma, adesso))
        assertEquals("01/03", DataOra.elenco(utc(2026, 3, 1, 10, 0), predefinite, roma, adesso))
        assertEquals("03/05/24", DataOra.elenco(utc(2024, 5, 3, 10, 0), predefinite, roma, adesso))
        val mga = PreferenzeDataOra(OrdineData.MGA)
        assertEquals("03/01", DataOra.elenco(utc(2026, 3, 1, 10, 0), mga, roma, adesso))
        val iso = PreferenzeDataOra(OrdineData.ISO)
        assertEquals("03-01", DataOra.elenco(utc(2026, 3, 1, 10, 0), iso, roma, adesso))
        assertEquals("2024-05-03", DataOra.elenco(utc(2024, 5, 3, 10, 0), iso, roma, adesso))
    }

    @Test
    fun `oggi si decide nel fuso mostrato`() {
        // 23:30 UTC del 6 = 00:30 a Roma del 7
        val m = utc(2026, 11, 6, 23, 30)
        val adesso = Instant.ofEpochSecond(utc(2026, 11, 7, 10, 0))
        assertEquals("00:30", DataOra.elenco(m, predefinite, roma, adesso))
        assertEquals("06/11", DataOra.elenco(m, PreferenzeDataOra(fuso = FusoOrario.UTC), roma, adesso))
    }

    @Test
    fun `12 ore - mezzanotte e mezzogiorno`() {
        val p = PreferenzeDataOra(orologio = Orologio.H12, fuso = FusoOrario.UTC)
        assertEquals("12:00 AM", DataOra.ora(DataOra.inZona(utc(2026, 11, 7, 0, 0), p, roma), p))
        assertEquals("12:00 PM", DataOra.ora(DataOra.inZona(utc(2026, 11, 7, 12, 0), p, roma), p))
        assertEquals("12:59 AM", DataOra.ora(DataOra.inZona(utc(2026, 11, 7, 0, 59), p, roma), p))
        assertEquals("1:00 PM", DataOra.ora(DataOra.inZona(utc(2026, 11, 7, 13, 0), p, roma), p))
    }

    @Test
    fun `AM e PM in inglese qualunque sia la lingua`() {
        val prima = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            val p = PreferenzeDataOra(orologio = Orologio.H12, fuso = FusoOrario.UTC)
            assertEquals("2:45 PM", DataOra.ora(DataOra.inZona(e, p, roma), p))
        } finally {
            Locale.setDefault(prima)
        }
    }

    @Test
    fun `scarti +0100 -0500 +0530 - stesso istante, ore diverse`() {
        val casi = mapOf(
            "Sat, 7 Nov 2026 15:45:00 +0100" to ("+0100" to "15:45"),
            "Sat, 7 Nov 2026 09:45:00 -0500" to ("-0500" to "09:45"),
            "Sat, 7 Nov 2026 20:15:00 +0530" to ("+0530" to "20:15"),
        )
        for ((riga, atteso) in casi) {
            val t = DataOra.leggiDataUsenet(riga)!!
            assertEquals(riga, e, t.toEpochSecond())
            assertEquals(riga, atteso.first, DataOra.scarto(t))
            assertEquals(riga, atteso.second, DataOra.ora(t, predefinite))
        }
    }

    @Test
    fun `cambio dell'ora legale a Roma`() {
        fun scarto(s: Long) = DataOra.scarto(DataOra.inZona(s, predefinite, roma))
        fun ora(s: Long) = DataOra.ora(DataOra.inZona(s, predefinite, roma), predefinite)
        // 29 marzo 2026: alle 01:00 UTC le 02:00 diventano 03:00
        assertEquals("+0100", scarto(utc(2026, 3, 29, 0, 30)))
        assertEquals("01:30", ora(utc(2026, 3, 29, 0, 30)))
        assertEquals("+0200", scarto(utc(2026, 3, 29, 1, 30)))
        assertEquals("03:30", ora(utc(2026, 3, 29, 1, 30)))
        // 25 ottobre 2026: alle 01:00 UTC le 03:00 tornano 02:00
        assertEquals("+0200", scarto(utc(2026, 10, 25, 0, 30)))
        assertEquals("02:30", ora(utc(2026, 10, 25, 0, 30)))
        assertEquals("+0100", scarto(utc(2026, 10, 25, 1, 30)))
        assertEquals("02:30", ora(utc(2026, 10, 25, 1, 30)))
    }

    @Test
    fun `riga Date - senza giorno della settimana, con commento, illeggibile`() {
        assertEquals(e, DataOra.leggiDataUsenet("7 Nov 2026 15:45:00 +0100")!!.toEpochSecond())
        assertEquals(e, DataOra.leggiDataUsenet("Sat, 7 Nov 2026 15:45:00 +0100 (CET)")!!.toEpochSecond())
        assertEquals(e, DataOra.leggiDataUsenet("Sat, 7 Nov 2026 14:45:00 GMT")!!.toEpochSecond())
        assertNull(DataOra.leggiDataUsenet("ieri pomeriggio"))
        assertNull(DataOra.leggiDataUsenet(""))
    }

    @Test
    fun `rigaDate - continuazione, assente, altre righe con Date nel nome`() {
        assertEquals(
            "Sat, 7 Nov 2026 15:45:00 +0100",
            DataOra.rigaDate("From: a <a@b>\nDate: Sat, 7 Nov 2026\n 15:45:00 +0100\nSubject: x"),
        )
        assertNull(DataOra.rigaDate("From: a\nNNTP-Posting-Date: Sat, 7 Nov 2026 15:45:00 +0100"))
        assertNull(DataOra.rigaDate(null))
        assertEquals("x", DataOra.rigaDate("date: x"))
    }

    // ------------------------------------------------------------ introduzione

    private fun articolo(intestazioni: String?) = Article(
        groupId = 1, number = 1, messageId = "<a@b>", subject = "Re: prova",
        fromField = "Mario Rossi <m@x.it>", dateEpoch = e, headersRaw = intestazioni,
    )
    private val conDate = articolo("From: Mario Rossi <m@x.it>\nDate: Sat, 7 Nov 2026 09:45:00 -0500")
    private fun intro(modello: String, a: Article = conDate, p: PreferenzeDataOra = predefinite) =
        introLine(Identity(nickname = "t", replyIntro = modello), a, "it.test", prefs = p, zonaTelefono = roma)

    @Test
    fun `introduzione - la mia ora, come prima`() {
        assertEquals("Il 07/11/2026 alle 15:45 Mario Rossi ha scritto:", intro("Il %data% alle %ora% %autore% ha scritto:"))
        assertEquals("On 07/11/2026 at 15:45", intro("On %date% at %time%"))
        assertEquals("15:45 +0100", intro("%ora% %fuso%"))
        assertEquals("14:45 UTC +0000", intro("%ora% %fuso%", p = PreferenzeDataOra(fuso = FusoOrario.UTC)))
        assertEquals("11/07/2026 3:45 PM", intro("%data% %ora%", p = PreferenzeDataOra(OrdineData.MGA, Orologio.H12)))
    }

    @Test
    fun `introduzione - l'ora dell'autore`() {
        assertEquals("07/11/2026 09:45 -0500", intro("%dataautore% %oraautore% %fusoautore%"))
        assertEquals("07/11/2026 09:45 -0500", intro("%authordate% %authortime% %authorzone%"))
        // senza riga Date, o illeggibile: la mia ora e il suo scarto, non un'ora inventata
        assertEquals("15:45 +0100", intro("%oraautore% %fusoautore%", articolo(null)))
        assertEquals("15:45 +0100", intro("%oraautore% %fusoautore%", articolo("Date: domani")))
    }

    @Test
    fun `introduzione - internazionale e riga originale`() {
        assertEquals("2026-11-07 14:45 UTC", intro("%datautc% %orautc%"))
        assertEquals("2026-11-07 14:45 UTC", intro("%utcdate% %utctime%"))
        assertEquals("Sat, 7 Nov 2026 09:45:00 -0500", intro("%dataoriginale%"))
        assertEquals("Sat, 7 Nov 2026 09:45:00 -0500", intro("%origdate%"))
        assertEquals("2026-11-07 14:45 UTC", intro("%dataoriginale%", articolo(null)))
        assertEquals("domani", intro("%dataoriginale%", articolo("Date: domani")))
    }

    @Test
    fun `introduzione - i segnaposto di prima non cambiano`() {
        assertEquals(
            "Mario Rossi <m@x.it> it.test prova <a@b>",
            intro("%autore% <%email%> %gruppo% %oggetto% %messageid%"),
        )
    }
}
