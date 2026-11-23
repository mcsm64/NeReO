/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.5 / 194 (BIG Umberto): un messaggio che arriva nasce letto se una sua
 * copia e' gia' letta altrove.
 */
package org.openusenet.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.data.Article
import org.openusenet.reader.data.ereditaLetto

class LettoEreditatoTest {
    private fun art(mid: String, letto: Boolean = false) = Article(
        groupId = 2, number = 1, messageId = mid, subject = "prova2",
        fromField = "Castor <c@x>", dateEpoch = 1, read = letto,
    )

    @Test
    fun `il caso di Umberto - letto su paganini, arriva su corradoroberto`() {
        val arrivati = listOf(art("<prova2@castor>"), art("<altro@x>"))
        val esito = ereditaLetto(arrivati, setOf("<prova2@castor>"))
        assertTrue(esito[0].read)
        assertFalse(esito[1].read)
    }

    @Test
    fun `nessuna copia letta - la lista resta quella`() {
        val arrivati = listOf(art("<a@x>"))
        assertSame(arrivati, ereditaLetto(arrivati, emptySet()))
        assertFalse(ereditaLetto(arrivati, setOf("<b@x>"))[0].read)
    }

    @Test
    fun `gia' letto resta letto, gli altri campi non cambiano`() {
        val a = art("<a@x>", letto = true)
        assertEquals(a, ereditaLetto(listOf(a), setOf("<a@x>"))[0])
        val b = art("<b@x>")
        assertEquals(b.copy(read = true), ereditaLetto(listOf(b), setOf("<b@x>"))[0])
    }
}
