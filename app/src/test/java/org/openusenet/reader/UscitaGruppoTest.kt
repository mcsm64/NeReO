/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.ui.screens.eUscitaDalGruppo
import org.openusenet.reader.ui.screens.soloCambiati
import org.openusenet.reader.ui.screens.totaleSegnati

/** 1.6.3 (Dave Royal): cosa conta come uscita dal gruppo, e l'ANNULLA su piu' gruppi. */
class UscitaGruppoTest {
    @Test
    fun `un altro gruppo e' un'uscita`() = assertTrue(eUscitaDalGruppo(1, 2))

    @Test
    fun `lo stesso gruppo non e' un'uscita`() = assertFalse(eUscitaDalGruppo(1, 1))

    @Test
    fun `nessun gruppo aperto prima - niente da lasciare`() = assertFalse(eUscitaDalGruppo(null, 2))

    @Test
    fun `gruppo tolto, per esempio disiscritto - non e' un'uscita`() = assertFalse(eUscitaDalGruppo(1, null))

    @Test
    fun `l'annulla tocca solo i gruppi cambiati`() {
        val segnati = mapOf(1L to listOf(10L, 11L), 2L to emptyList(), 3L to listOf(30L))
        assertEquals(mapOf(1L to listOf(10L, 11L), 3L to listOf(30L)), soloCambiati(segnati))
        assertEquals(3, totaleSegnati(segnati))
    }

    @Test
    fun `niente di nuovo da segnare - nessun avviso`() {
        assertEquals(0, totaleSegnati(soloCambiati(mapOf(1L to emptyList()))))
        assertEquals(0, totaleSegnati(emptyMap()))
    }
}
