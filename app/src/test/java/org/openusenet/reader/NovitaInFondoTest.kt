/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.ui.screens.ImprontaLista
import org.openusenet.reader.ui.screens.improntaLista
import org.openusenet.reader.ui.screens.novitaInCima
import org.openusenet.reader.ui.screens.novitaInFondo
import org.openusenet.reader.ui.screens.partenzaInFondo

/** 1.6.4 (Jeff): con l'ordine dal piu' vecchio si apre in fondo, sui piu' recenti. */
class NovitaInFondoTest {
    private val prima = ImprontaLista(primoId = 1, ultimoId = 50, ultimaRigaId = 40)

    @Test
    fun `solo con l'opzione e un ordine dal piu' vecchio`() {
        assertTrue(partenzaInFondo("activity_asc", true))
        assertTrue(partenzaInFondo("opened_asc", true))
        assertFalse(partenzaInFondo("activity_asc", false))
        assertFalse(partenzaInFondo("activity_desc", true))
        assertFalse(partenzaInFondo("opened_desc", true))
    }

    @Test
    fun `ordine dal piu' recente - in fondo non ci sono novita'`() =
        assertFalse(novitaInFondo(true, "activity_desc", prima, prima.copy(ultimoId = 60)))

    @Test
    fun `vista sequenziale - conta l'id piu' alto`() {
        assertTrue(novitaInFondo(true, "opened_asc", prima, prima.copy(ultimoId = 51)))
        assertFalse(novitaInFondo(true, "opened_asc", prima, prima))
    }

    @Test
    fun `per ultima attivita' - una risposta porta in fondo la sua discussione`() =
        assertTrue(novitaInFondo(false, "activity_asc", prima, prima.copy(ultimoId = 51)))

    @Test
    fun `per apertura ad albero - una risposta a una vecchia discussione non e' in fondo`() =
        assertFalse(novitaInFondo(false, "opened_asc", prima, prima.copy(ultimoId = 51)))

    @Test
    fun `per apertura ad albero - una discussione nuova scende in fondo`() =
        assertTrue(novitaInFondo(false, "opened_asc", prima, prima.copy(ultimoId = 51, ultimaRigaId = 51)))

    @Test
    fun `l'ultima riga cambiata senza messaggi nuovi non e' un arrivo`() =
        assertFalse(novitaInFondo(false, "opened_asc", prima, prima.copy(ultimaRigaId = 39)))

    @Test
    fun `la cima resta com'era - per gli ordini dal piu' vecchio dice sempre no`() =
        assertFalse(novitaInCima(true, "activity_asc", prima, prima.copy(ultimoId = 60)))

    @Test
    fun `l'impronta porta l'ultima riga, e senza resta zero`() {
        assertEquals(ImprontaLista(3, 9, 7), improntaLista(3, listOf(3L, 9L, 7L), 7))
        assertEquals(ImprontaLista(3, 9, 0), improntaLista(3, listOf(3L, 9L)))
    }
}
