/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.data.Article
import org.openusenet.reader.ui.screens.ImprontaLista
import org.openusenet.reader.ui.screens.ThreadItem
import org.openusenet.reader.ui.screens.ThreadScrollMemory
import org.openusenet.reader.ui.screens.idNonLettoNelGruppo
import org.openusenet.reader.ui.screens.indiceDiRipristino
import org.openusenet.reader.ui.screens.novitaInCima
import org.openusenet.reader.ui.screens.orderedRoots

/**
 * 1.6.1 / 187 (Cercaparole, Michele e Roxy su Usenet, 5/11): le discussioni
 * seguite stanno in cima solo finche' hanno un non letto. Si prova la funzione
 * vera, non una copia.
 */
class OrdinamentoSeguiteTest {

    private fun art(
        nome: String,
        data: Long,
        seguita: Boolean = false,
        letto: Boolean = true,
        padre: String? = null,
    ) = Article(
        id = data,
        groupId = 1,
        number = data,
        messageId = "<$nome@x>",
        subject = if (padre == null) "oggetto $nome" else "Re: oggetto $padre",
        fromField = "tizio@example.org",
        dateEpoch = data,
        references = padre?.let { "<$it@x>" }.orEmpty(),
        read = letto,
        watched = seguita,
    )

    private fun ordine(articoli: List<Article>, sort: String, soloNonLetti: Boolean = false): List<String> =
        orderedRoots(articoli, soloNonLetti, sort).first
            .map { it.flatten().first().second.messageId.removeSurrounding("<", "@x>") }

    @Test
    fun `seguita con non letti in cima, in tutti gli ordinamenti`() {
        val a = listOf(art("vecchia", 1), art("seguita", 2, seguita = true, letto = false), art("nuova", 3))
        assertEquals(listOf("seguita", "nuova", "vecchia"), ordine(a, "activity_desc"))
        assertEquals(listOf("seguita", "nuova", "vecchia"), ordine(a, "opened_desc"))
        assertEquals(listOf("seguita", "vecchia", "nuova"), ordine(a, "activity_asc"))
        assertEquals(listOf("seguita", "vecchia", "nuova"), ordine(a, "opened_asc"))
    }

    @Test
    fun `seguita tutta letta al suo posto, in tutti gli ordinamenti`() {
        val a = listOf(art("vecchia", 1), art("seguita", 2, seguita = true), art("nuova", 3))
        assertEquals(listOf("nuova", "seguita", "vecchia"), ordine(a, "activity_desc"))
        assertEquals(listOf("nuova", "seguita", "vecchia"), ordine(a, "opened_desc"))
        assertEquals(listOf("vecchia", "seguita", "nuova"), ordine(a, "activity_asc"))
        assertEquals(listOf("vecchia", "seguita", "nuova"), ordine(a, "opened_asc"))
    }

    @Test
    fun `piu' seguite con non letti stanno fra loro in ordine di data`() {
        val a = listOf(
            art("s1", 1, seguita = true, letto = false),
            art("s2", 2, seguita = true, letto = false),
            art("altra", 3),
        )
        assertEquals(listOf("s2", "s1", "altra"), ordine(a, "activity_desc"))
        assertEquals(listOf("s1", "s2", "altra"), ordine(a, "activity_asc"))
    }

    @Test
    fun `una seguita con non letti e una letta si separano`() {
        val a = listOf(
            art("letta", 1, seguita = true),
            art("da_leggere", 2, seguita = true, letto = false),
            art("altra", 3),
        )
        assertEquals(listOf("da_leggere", "altra", "letta"), ordine(a, "activity_desc"))
    }

    @Test
    fun `nessuna seguita, solo la data`() {
        val a = listOf(art("uno", 1, letto = false), art("due", 2), art("tre", 3, letto = false))
        assertEquals(listOf("tre", "due", "uno"), ordine(a, "activity_desc"))
        assertEquals(listOf("uno", "due", "tre"), ordine(a, "activity_asc"))
    }

    @Test
    fun `basta un non letto in qualunque punto della discussione`() {
        val a = listOf(
            art("seguita", 1, seguita = true),
            art("risposta", 3, letto = false, padre = "seguita"),
            art("nuova", 4),
        )
        assertEquals(listOf("seguita", "nuova"), ordine(a, "activity_desc"))
    }

    @Test
    fun `con solo non letti la seguita letta resta nascosta`() {
        val a = listOf(art("seguita", 1, seguita = true), art("altra", 2, letto = false))
        assertEquals(listOf("altra"), ordine(a, "activity_desc", soloNonLetti = true))
    }

    // --- «ci sono novita'?»: una seguita che scende non e' un arrivo ---

    private val prima = ImprontaLista(primoId = 10, ultimoId = 50)

    @Test
    fun `per apertura, la seguita scesa non e' una novita'`() {
        assertFalse(novitaInCima(false, "opened_desc", prima, ImprontaLista(20, 50)))
    }

    @Test
    fun `per apertura, una discussione nuova in cima e' una novita'`() {
        assertTrue(novitaInCima(false, "opened_desc", prima, ImprontaLista(51, 51)))
    }

    @Test
    fun `per apertura, una risposta a una vecchia non e' una novita' in cima`() {
        assertFalse(novitaInCima(false, "opened_desc", prima, ImprontaLista(10, 51)))
    }

    @Test
    fun `per attivita' e in sequenziale conta l'id piu' alto`() {
        assertTrue(novitaInCima(false, "activity_desc", prima, ImprontaLista(10, 51)))
        assertTrue(novitaInCima(true, "opened_desc", prima, ImprontaLista(10, 51)))
        assertFalse(novitaInCima(false, "activity_desc", prima, ImprontaLista(20, 50)))
    }

    @Test
    fun `con gli ordinamenti dai piu' vecchi mai`() {
        assertFalse(novitaInCima(false, "activity_asc", prima, ImprontaLista(51, 51)))
        assertFalse(novitaInCima(false, "opened_asc", prima, ImprontaLista(51, 51)))
    }

    // --- dove tornare dall'articolo ---

    private fun ancora(id: Long, inCima: Boolean) =
        ThreadScrollMemory.Ancora(id, 0, ImprontaLista(id, 99), inCima)

    @Test
    fun `chi era in cima resta in cima se la riga e' scesa`() {
        assertNull(indiceDiRipristino(ancora(1, inCima = true), listOf(2, 3, 1, 4)))
    }

    @Test
    fun `chi era in cima e la riga e' ancora prima torna li'`() {
        assertEquals(0, indiceDiRipristino(ancora(1, inCima = true), listOf(1, 2, 3)))
    }

    @Test
    fun `da un punto qualunque si segue la riga`() {
        assertEquals(2, indiceDiRipristino(ancora(1, inCima = false), listOf(2, 3, 1)))
    }

    @Test
    fun `riga sparita, si resta in cima`() {
        assertNull(indiceDiRipristino(ancora(9, inCima = false), listOf(1, 2)))
    }

    // --- lo swipe: i non letti rimasti sopra la seguita scesa ---

    @Test
    fun `lo swipe trova i non letti rimasti sopra`() {
        val lista = listOf(
            ThreadItem(depth = 0, article = art("sopra", 3, letto = false), threadStart = true),
            ThreadItem(depth = 0, article = art("seguita", 2, seguita = true), threadStart = true),
            ThreadItem(depth = 0, article = art("sotto", 1), threadStart = true),
        )
        assertEquals(3L, idNonLettoNelGruppo(lista, 1))
    }
}
