/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader

import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.data.EsitoGiro
import org.openusenet.reader.data.ServerNonAggiornato
import org.openusenet.reader.data.elencoNonAggiornati
import org.openusenet.reader.data.giroDelServer
import org.openusenet.reader.data.nonRisponde
import org.openusenet.reader.data.serverNonAggiornato
import org.openusenet.reader.nntp.NntpClient
import org.openusenet.reader.nntp.NntpException

/** 1.6.1 (BIG Umberto): un server che non risponde non ferma gli altri. */
class GiroServerTest {
    /** Una sessione finta: [apertura] conta i tentativi e puo' lanciare. */
    private class Finta(val apertura: (Int) -> Unit = {}) {
        var tentativi = 0
        val fatti = ArrayList<String>()
        suspend fun giro(gruppi: List<String>, lavoro: (String) -> Int = { 1 }): EsitoGiro =
            giroDelServer<Unit, String>(
                gruppi,
                sessione = { blocco ->
                    tentativi++
                    apertura(tentativi)
                    blocco(Unit)
                },
                sincronizza = { _, g -> lavoro(g).also { fatti.add(g) } },
            )
    }

    @Test
    fun `tutto bene - una sessione sola per tutti i gruppi`() = runBlocking {
        val f = Finta()
        val e = f.giro(listOf("a", "b", "c"))
        assertEquals(1, f.tentativi)
        assertEquals(3, e.scaricati)
        assertEquals(3, e.riusciti)
        assertNull(e.errore)
        assertFalse(e.daSegnalare)
    }

    @Test
    fun `server spento - un tentativo solo, non uno per gruppo`() = runBlocking {
        val spento = SocketTimeoutException("after 15000ms")
        val f = Finta { throw spento }
        val e = f.giro(List(10) { "g$it" })
        assertEquals(1, f.tentativi)
        assertTrue(f.fatti.isEmpty())
        assertTrue(e.sessioneMancata)
        assertSame(spento, e.errore)
        assertTrue(e.daSegnalare)
    }

    @Test
    fun `login rifiutato - non si ripete per ogni gruppo`() = runBlocking {
        val f = Finta { throw NntpException(481, "rifiutato") }
        f.giro(List(10) { "g$it" })
        assertEquals(1, f.tentativi)
    }

    @Test
    fun `un gruppo in errore si salta e gli altri proseguono`() = runBlocking {
        val f = Finta()
        val e = f.giro(listOf("a", "sparito", "c")) { g ->
            if (g == "sparito") throw NntpException(411, "no such group")
            1
        }
        assertEquals(listOf("a", "c"), f.fatti)
        // Terza sessione: dopo un gruppo riuscito il guasto puo' essere la connessione,
        // quindi quel gruppo si riprova una volta prima di saltarlo. Come prima.
        assertEquals(3, f.tentativi)
        assertFalse(e.sessioneMancata)
        assertFalse(e.daSegnalare) // un gruppo sparito non e' un server fuori
    }

    @Test
    fun `tutti i gruppi in errore - il server si segnala`() = runBlocking {
        val f = Finta()
        val e = f.giro(listOf("a", "b")) { throw NntpException(502, "basta") }
        assertEquals(2, f.tentativi)
        assertTrue(e.daSegnalare)
    }

    @Test
    fun `caduta a meta' - riapre, e se non riapre esce con quanto ha`() = runBlocking {
        val f = Finta { if (it == 2) throw ConnectException("refused") }
        val e = f.giro(listOf("a", "cade", "c", "d")) { g ->
            if (g == "cade") throw IOException("connessione caduta")
            1
        }
        assertEquals(listOf("a"), f.fatti)
        assertEquals(2, f.tentativi)
        assertEquals(1, e.scaricati)
        assertTrue(e.sessioneMancata)
        assertTrue(e.daSegnalare)
    }

    @Test
    fun `caduta a meta' - se riapre riprende dal gruppo dopo`() = runBlocking {
        val f = Finta()
        f.giro(listOf("a", "cade", "c")) { g ->
            if (g == "cade") throw IOException("connessione caduta")
            1
        }
        assertEquals(listOf("a", "c"), f.fatti)
    }

    @Test
    fun `il primo server spento non ferma i successivi`() = runBlocking {
        // Il caso di Umberto, come lo gira syncGroupList.
        val esiti = listOf(
            Finta { throw SocketTimeoutException("after 15000ms") }.giro(listOf("x", "y")),
            Finta().giro(listOf("z")),
            Finta().giro(listOf("u", "v")),
        )
        assertEquals(listOf(0, 1, 2), esiti.map { it.scaricati })
        assertEquals(listOf(true, false, false), esiti.map { it.daSegnalare })
    }

    @Test
    fun `l'annullamento risale e non e' un errore del server`() {
        val e = runCatching {
            runBlocking { Finta { throw CancellationException("basta") }.giro(listOf("a", "b")) }
        }.exceptionOrNull()
        assertTrue(e is CancellationException)
    }

    @Test
    fun `il mutex si libera dopo un errore`() = runBlocking {
        val m = Mutex()
        m.withLock { Finta { throw SocketTimeoutException("x") }.giro(listOf("a")) }
        assertFalse(m.isLocked)
    }

    @Test
    fun `il mutex si libera dopo un annullamento`() = runBlocking {
        val m = Mutex()
        val dentro = CompletableDeferred<Unit>()
        val lavoro = launch(start = CoroutineStart.UNDISPATCHED) {
            m.withLock {
                giroDelServer<Unit, String>(
                    listOf("a", "b"),
                    sessione = { blocco -> blocco(Unit) },
                    sincronizza = { _, _ ->
                        dentro.complete(Unit)
                        awaitCancellation()
                    },
                )
            }
        }
        dentro.await()
        assertTrue(m.isLocked)
        lavoro.cancel()
        lavoro.join()
        assertFalse(m.isLocked)
    }

    @Test
    fun `il riepilogo nomina i server fuori e il motivo`() {
        val lista = listOf(
            serverNonAggiornato("robertocorrado", SocketTimeoutException("after 15000ms")),
            serverNonAggiornato("solani", NntpException(481, "rifiutato")),
        )
        assertEquals(ServerNonAggiornato("robertocorrado", null), lista[0])
        assertEquals(
            "robertocorrado (non risponde), solani (481 rifiutato)",
            elencoNonAggiornati(lista, "non risponde"),
        )
    }

    @Test
    fun `non risponde - timeout e rifiuto si, il resto no`() {
        assertTrue(nonRisponde(SocketTimeoutException()))
        assertTrue(nonRisponde(ConnectException()))
        assertFalse(nonRisponde(NntpException(481, "x")))
        assertFalse(nonRisponde(IOException("x")))
    }

    // ---------------------------------------------------- con un server vero, finto

    /** Una porta su cui non ascolta nessuno. */
    private fun portaChiusa(): Int = ServerSocket(0).use { it.localPort }

    @Test
    fun `porta chiusa - errore di rete, e close non ci mette del suo`() {
        val c = NntpClient("127.0.0.1", portaChiusa())
        val e = runCatching { c.connect() }.exceptionOrNull()!!
        assertTrue(nonRisponde(e))
        c.close() // prima lanciava "Not connected"
    }

    @Test
    fun `server che accetta e tace - la chiusura non aspetta un altro timeout`() {
        val presa = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        try {
            val c = NntpClient("127.0.0.1", presa.localPort, readTimeoutMs = 2_500)
            val e = runCatching { c.connect() }.exceptionOrNull()!!
            assertTrue(nonRisponde(e))
            val inizio = System.nanoTime()
            c.close()
            val ms = (System.nanoTime() - inizio) / 1_000_000
            assertTrue("chiusura in $ms ms", ms < 2_000)
        } finally {
            presa.close()
        }
    }

    @Test
    fun `il server spento non ferma quello buono, su socket veri`() = runBlocking(Dispatchers.IO) {
        val presa = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val filo = Thread {
            runCatching {
                presa.accept().use { s ->
                    val w = s.getOutputStream().bufferedWriter(Charsets.ISO_8859_1)
                    val r = s.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                    w.write("200 finto\r\n"); w.flush()
                    while (true) {
                        val cmd = r.readLine() ?: break
                        val risposta = when {
                            cmd.startsWith("GROUP") -> "211 3 1 3 it.test"
                            cmd.startsWith("QUIT") -> "205 ciao"
                            else -> "500 sconosciuto"
                        }
                        w.write("$risposta\r\n"); w.flush()
                        if (cmd.startsWith("QUIT")) break
                    }
                }
            }
        }
        filo.isDaemon = true
        filo.start()
        try {
            val porte = listOf(portaChiusa(), presa.localPort)
            val esiti = porte.map { porta ->
                giroDelServer<NntpClient, String>(
                    listOf("it.test", "it.test"),
                    sessione = { blocco ->
                        NntpClient("127.0.0.1", porta).use { c ->
                            c.connect()
                            blocco(c)
                        }
                    },
                    sincronizza = { c, g -> c.group(g); 1 },
                )
            }
            assertTrue(esiti[0].daSegnalare)
            assertEquals(0, esiti[0].scaricati)
            assertEquals(2, esiti[1].scaricati)
            assertFalse(esiti[1].daSegnalare)
        } finally {
            presa.close()
        }
    }
}
