/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.1 / 187 — grassetto, corsivo e sottolineato. Le prime righe sono il
 * «Formatting Test» di Marco in alt.test.test.jojo, confrontato con MesNews.
 */
package org.openusenet.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.text.Enfasi
import org.openusenet.reader.text.TipoEnfasi
import org.openusenet.reader.text.trovaEnfasi
import org.openusenet.reader.text.trovaFaccine

class EnfasiTest {
    private fun testi(riga: String) = trovaEnfasi(riga).map { riga.substring(it.apertura + 1, it.chiusura) }

    @Test
    fun `il Formatting Test di Marco`() {
        assertEquals(listOf(Enfasi(0, 5, TipoEnfasi.GRASSETTO)), trovaEnfasi("*TEST* -> GRASSETTO"))
        assertEquals(listOf(Enfasi(0, 5, TipoEnfasi.CORSIVO)), trovaEnfasi("/TEST/ -> CORSIVO"))
        assertEquals(listOf(Enfasi(0, 5, TipoEnfasi.SOTTOLINEATO)), trovaEnfasi("_TEST_ -> SOTTOLINEATO"))
    }

    @Test
    fun `barrato e rovescia restano testo, come in MesNews`() {
        assertTrue(trovaEnfasi("-TEST DI PROVA- -> BARRATO").isEmpty())
        assertTrue(trovaEnfasi("\\TEST DI PROVA\\ -> VEDIAMO COSA ESCE").isEmpty())
    }

    @Test
    fun `piu' parole, piu' coppie, come nel post di Umberto`() {
        assertEquals(
            listOf("NON TI ACCORGI", "NON LETTO"),
            testi("Quindi *NON TI ACCORGI* di essere all'ultimo messaggio *NON LETTO* in"),
        )
        assertEquals(listOf("solo", "tre"), testi("agire *solo* e /tre/ volte"))
    }

    @Test
    fun `accanto a punteggiatura, parentesi e citazione`() {
        assertEquals(listOf("vero"), testi("(*vero*)"))
        assertEquals(listOf("vero"), testi("e' *vero*, dico"))
        assertEquals(listOf("vero"), testi("«_vero_»"))
        assertEquals(listOf("vero"), testi(">*vero*"))
    }

    @Test
    fun `i falsi positivi restano testo`() {
        for (r in listOf(
            "2*3*4", "3 * 4 * 5", "/usr/bin/", "/usr/bin/env", "km/h e m/s", "il 5/11/2026",
            "nome_file_", "_nome_file", "a_b_c", "* voce di elenco *", "**doppio**", "__doppio__",
            "//commento//", "* * *", "___", "*", "", "/ /", "_ _",
        )) {
            assertTrue(r, trovaEnfasi(r).isEmpty())
        }
    }

    @Test
    fun `un link non si tocca`() {
        val riga = "vedi /qui/ https://example.org/a_b_c/d e _anche_ qui"
        val url = listOf(Regex("""https://\S+""").find(riga)!!.range)
        assertEquals(listOf("qui", "anche"), trovaEnfasi(riga, url).map { riga.substring(it.apertura + 1, it.chiusura) })
        // una coppia a cavallo del link non vale
        val cavallo = "*vedi https://example.org qui*"
        assertTrue(trovaEnfasi(cavallo, listOf(Regex("""https://\S+""").find(cavallo)!!.range)).isEmpty())
    }

    @Test
    fun `la faccina fra asterischi resta una faccina`() {
        assertTrue(trovaEnfasi("*;-)*").isEmpty())
        assertEquals(1, trovaFaccine("*;-)*", emptyList()).size)
    }

    @Test
    fun `una faccina dentro il grassetto non lo rompe`() {
        assertEquals(listOf("ciao :-) a tutti"), testi("*ciao :-) a tutti*"))
    }

    @Test
    fun `niente annidamenti - vale la coppia esterna`() {
        assertEquals(listOf("uno _due_ tre"), testi("*uno _due_ tre*"))
    }
}
