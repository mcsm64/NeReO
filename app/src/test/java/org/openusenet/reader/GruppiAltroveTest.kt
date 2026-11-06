/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.1 / 187 (Dave Royal) — il messaggio non e' del gruppo che si sta leggendo.
 */
package org.openusenet.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.ui.screens.DestinazioneRisposta
import org.openusenet.reader.ui.screens.crosspostGroups
import org.openusenet.reader.ui.screens.extraPredefinitiCrosspost
import org.openusenet.reader.ui.screens.extraPredefinitiRisposta
import org.openusenet.reader.ui.screens.gruppiAltrove
import org.openusenet.reader.ui.screens.gruppiDaSegnalare
import org.openusenet.reader.ui.screens.gruppiOffertiNelSelettore
import org.openusenet.reader.ui.screens.selettoreDisponibile

class GruppiAltroveTest {
    private val tb = "alt.comp.software.thunderbird"
    private val w10 = "alt.comp.windows-10"
    private val nessuna = DestinazioneRisposta.Nessuna
    private val followup = DestinazioneRisposta.Gruppi(listOf(w10))

    @Test
    fun `il caso di Dave - il gruppo non e' fra i destinatari`() {
        assertEquals(listOf(w10), gruppiAltrove(listOf(w10), tb))
        assertEquals(listOf(w10, "it.test"), gruppiAltrove(listOf(w10, "it.test"), tb))
    }

    @Test
    fun `il gruppo c'e' - niente avviso`() {
        assertTrue(gruppiAltrove(listOf(tb), tb).isEmpty())
        assertTrue(gruppiAltrove(listOf(w10, tb), tb).isEmpty())
    }

    @Test
    fun `maiuscole e spazi non contano`() {
        assertTrue(gruppiAltrove(listOf("  ALT.Comp.Software.Thunderbird "), tb).isEmpty())
        assertTrue(gruppiAltrove(listOf(tb), "  $tb ").isEmpty())
        assertEquals(listOf(w10), gruppiAltrove(listOf("  $w10  ", ""), tb))
    }

    @Test
    fun `se un dato manca non si avvisa`() {
        assertTrue(gruppiAltrove(emptyList(), tb).isEmpty())
        assertTrue(gruppiAltrove(listOf(w10), "").isEmpty())
        assertTrue(gruppiAltrove(listOf(w10), "   ").isEmpty())
    }

    @Test
    fun `dalle intestazioni, separatori e righe piegate compresi`() {
        assertTrue(gruppiAltrove(crosspostGroups("Newsgroups: a.b,$tb\n"), tb).isEmpty())
        assertTrue(gruppiAltrove(crosspostGroups("Newsgroups: a.b, $tb\n"), tb).isEmpty())
        assertTrue(gruppiAltrove(crosspostGroups("Newsgroups: a.b,\n $tb\nSubject: x\n"), tb).isEmpty())
        assertEquals(listOf(w10), gruppiAltrove(crosspostGroups("Subject: x\nNewsgroups: $w10\n"), tb))
        assertTrue(gruppiAltrove(crosspostGroups(null), tb).isEmpty())
    }

    @Test
    fun `il selettore compare anche con un gruppo solo altrove`() {
        assertTrue(selettoreDisponibile(nessuna, listOf(w10), tb))
    }

    @Test
    fun `sui messaggi normali il selettore e' come prima`() {
        assertFalse(selettoreDisponibile(nessuna, listOf(tb), tb))
        assertFalse(selettoreDisponibile(nessuna, emptyList(), tb))
        assertTrue(selettoreDisponibile(nessuna, listOf(tb, w10), tb))
        assertEquals(listOf(tb, w10), gruppiOffertiNelSelettore(nessuna, listOf(tb, w10)))
    }

    @Test
    fun `con un Followup-To decide lui`() {
        // un gruppo solo, lo stesso del Followup-To: niente da scegliere
        assertFalse(selettoreDisponibile(followup, listOf(w10), tb))
        assertTrue(gruppiDaSegnalare(listOf(w10), tb, followup, listOf(w10)).isEmpty())
    }

    @Test
    fun `l'avviso in scrittura sparisce quando il gruppo e' stato aggiunto`() {
        assertEquals(listOf(w10), gruppiDaSegnalare(listOf(w10), tb, nessuna, listOf(tb)))
        assertTrue(gruppiDaSegnalare(listOf(w10), tb, nessuna, listOf(tb, w10)).isEmpty())
        assertTrue(gruppiDaSegnalare(listOf(w10), tb, nessuna, listOf(tb, w10.uppercase())).isEmpty())
        assertTrue(gruppiDaSegnalare(listOf(tb), tb, nessuna, listOf(tb)).isEmpty())
    }

    @Test
    fun `«tutti i gruppi» non aggiunge niente da solo se il messaggio sta altrove`() {
        assertTrue(extraPredefinitiCrosspost(listOf(w10), tb, tuttiIGruppi = true).isEmpty())
        assertTrue(extraPredefinitiRisposta(listOf(w10), tb, emptyList(), tuttiIGruppi = true).isEmpty())
        // su un crosspost vero resta com'era
        assertEquals(listOf(w10), extraPredefinitiCrosspost(listOf(tb, w10), tb, tuttiIGruppi = true))
    }
}
