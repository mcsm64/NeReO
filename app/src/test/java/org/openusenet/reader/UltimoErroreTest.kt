/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.2 / 189 (HAL) — l'ultimo errore operativo e le righe del log copiate.
 */
package org.openusenet.reader

import java.io.File
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.nntp.NntpLog

class UltimoErroreTest {

    @Test
    fun `il caso di HAL - timeout sul POST, con le righe del log`() {
        val testo = UltimoErrore.componi(
            "2026-11-09 10:00:00", "Versione: 1.6.2 (versionCode 189)", "POST",
            "news.astraweb.com:119", "it.tlc.cellulari.android",
            SocketTimeoutException("Read timed out"),
            listOf("C: POST", "S: 340 Ok", "D: nessuna risposta dopo il punto finale"),
        )
        assertTrue(testo.contains("Operazione: POST"))
        assertTrue(testo.contains("Server: news.astraweb.com:119"))
        assertTrue(testo.contains("Gruppo: it.tlc.cellulari.android"))
        assertTrue(testo.contains("Eccezione: SocketTimeoutException"))
        assertTrue(testo.contains("Messaggio: Read timed out"))
        assertTrue(testo.contains("Ultime 3 righe del log NNTP"))
        assertTrue(testo.trimEnd().endsWith("D: nessuna risposta dopo il punto finale"))
    }

    @Test
    fun `log spento - lo si dice, e i campi mancanti diventano un trattino`() {
        val testo = UltimoErrore.componi("q", "", "aggiornamento (connessione)", null, null, IllegalStateException(), null)
        assertTrue(testo.contains("Log NNTP spento"))
        assertTrue(testo.contains("Server: -"))
        assertTrue(testo.contains("Gruppo: -"))
        assertTrue(testo.contains("Messaggio: -"))
        assertFalse(testo.contains("Causa:"))
    }

    @Test
    fun `la causa avvolta si vede`() {
        val e = RuntimeException("esterno", SocketTimeoutException("interno"))
        val testo = UltimoErrore.componi("q", "", "POST", "s", "g", e, emptyList())
        assertTrue(testo.contains("Causa: SocketTimeoutException: interno"))
    }

    @Test
    fun `ultimeRighe - le piu' recenti, in ordine`() {
        val prima = NntpLog.enabled
        try {
            NntpLog.enabled = true
            NntpLog.clear()
            for (i in 1..10) NntpLog.diag("riga $i")
            assertEquals(listOf("D: riga 8", "D: riga 9", "D: riga 10"), NntpLog.ultimeRighe(3))
            assertEquals(10, NntpLog.ultimeRighe(40).size)
        } finally {
            NntpLog.clear()
            NntpLog.enabled = prima
        }
    }

    @Test
    fun `registra scrive il file, e la copia non cambia con le righe dopo`() {
        val f = File.createTempFile("last_error", ".txt")
        val primaFile = UltimoErrore.file
        val primaLog = NntpLog.enabled
        try {
            UltimoErrore.file = f
            NntpLog.enabled = true
            NntpLog.clear()
            NntpLog.diag("prima dell'errore")
            UltimoErrore.registra("POST", "s:119", "g", SocketTimeoutException("Read timed out"))
            NntpLog.diag("dopo l'errore")
            val testo = UltimoErrore.snapshot()
            assertTrue(testo.contains("D: prima dell'errore"))
            assertFalse(testo.contains("dopo l'errore"))
            UltimoErrore.clear()
            assertEquals("", UltimoErrore.snapshot())
        } finally {
            NntpLog.clear()
            NntpLog.enabled = primaLog
            UltimoErrore.file = primaFile
            f.delete()
        }
    }
}
