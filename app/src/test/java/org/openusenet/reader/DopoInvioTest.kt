/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.openusenet.reader.data.Group
import org.openusenet.reader.data.aggiornaFinoAdArrivo
import org.openusenet.reader.data.gruppiDaAggiornareDopoInvio

/** 1.6.5 / 193: dopo l'invio si riaggiorna finche' il proprio post arriva, per poco. */
class DopoInvioTest {
    private fun prova(esiti: List<Int>): Pair<Int, List<Long>> = runBlocking {
        val coda = ArrayDeque(esiti)
        val attese = ArrayList<Long>()
        val n = aggiornaFinoAdArrivo(attendi = { attese.add(it) }) { coda.removeFirstOrNull() ?: 0 }
        n to attese
    }

    @Test
    fun `arriva subito - nessuna attesa`() = assertEquals(2 to emptyList<Long>(), prova(listOf(2)))

    @Test
    fun `arriva al secondo tentativo - una attesa sola`() =
        assertEquals(1 to listOf(2_000L), prova(listOf(0, 1)))

    @Test
    fun `non arriva mai - tre attese e poi basta`() =
        assertEquals(0 to listOf(2_000L, 5_000L, 10_000L), prova(listOf(0, 0, 0, 0, 0)))

    private val lettura = Group(id = 1, serverId = 9, name = "free.it.ai.general", subscribed = true)
    private val altro = Group(id = 2, serverId = 9, name = "free.it.test", subscribed = true)
    private val estraneo = Group(id = 3, serverId = 9, name = "it.altro", subscribed = true)

    @Test
    fun `crosspost - i gruppi di destinazione iscritti su quel server`() = assertEquals(
        listOf(1L, 2L),
        gruppiDaAggiornareDopoInvio(lettura, listOf(lettura, altro, estraneo), listOf("free.it.ai.general", " free.it.test")),
    )

    @Test
    fun `followup altrove non iscritto - resta almeno il gruppo di partenza`() = assertEquals(
        listOf(1L),
        gruppiDaAggiornareDopoInvio(lettura, listOf(lettura, estraneo), listOf("it.non.iscritto")),
    )
}
