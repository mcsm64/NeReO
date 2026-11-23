/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader

import java.net.InetAddress
import java.net.ServerSocket
import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.nntp.NntpClient
import org.openusenet.reader.nntp.NntpException
import org.openusenet.reader.nntp.messageIdDi
import org.openusenet.reader.nntp.scaricaArticolo

/** 1.6.5 (BIG Umberto): dopo un 430 per Message-ID, un tentativo per numero, verificato. */
class ScaricaArticoloTest {
    private fun articolo(numero: Int, id: String) =
        listOf("220 $numero $id", "Message-ID: $id", "Subject: s", "", "corpo di $id", ".")

    /** Come il server di Umberto: per Message-ID 430, per numero si'. */
    private fun risposte(cmd: String): List<String> = when (cmd) {
        "GROUP it.test" -> listOf("211 9 1 9 it.test")
        "ARTICLE <diretto@x>" -> articolo(0, "<diretto@x>")
        "ARTICLE <a@x>", "ARTICLE <b@x>", "ARTICLE <c@x>", "ARTICLE <d@x>" -> listOf("430 No article with that message-id")
        "ARTICLE 7" -> articolo(7, "<a@x>")
        "ARTICLE 8" -> articolo(8, "<un-altro@x>")
        "ARTICLE 9" -> listOf("423 No article with that number")
        else -> listOf("500 sconosciuto")
    }

    private fun conServer(prova: (NntpClient, List<String>) -> Unit) {
        val comandi = Collections.synchronizedList(ArrayList<String>())
        val presa = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val filo = Thread {
            runCatching {
                presa.accept().use { s ->
                    val w = s.getOutputStream().bufferedWriter(Charsets.ISO_8859_1)
                    val r = s.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                    w.write("200 finto\r\n"); w.flush()
                    while (true) {
                        val cmd = r.readLine() ?: break
                        if (cmd.startsWith("QUIT")) { w.write("205 ciao\r\n"); w.flush(); break }
                        if (cmd.startsWith("ARTICLE")) comandi.add(cmd)
                        val righe = if (cmd.startsWith("CAPABILITIES")) listOf("101 elenco", "VERSION 2", "READER", ".") else risposte(cmd)
                        for (riga in righe) w.write(riga + "\r\n")
                        w.flush()
                    }
                }
            }
        }
        filo.isDaemon = true
        filo.start()
        try {
            NntpClient("127.0.0.1", presa.localPort).use { c ->
                c.connect()
                c.group("it.test")
                prova(c, comandi)
            }
        } finally {
            presa.close()
        }
    }

    private fun codice(blocco: () -> Unit): Int =
        (runCatching(blocco).exceptionOrNull() as NntpException).code

    @Test
    fun `trovato al primo colpo - un comando solo`() = conServer { c, comandi ->
        assertEquals("<diretto@x>", messageIdDi(scaricaArticolo(c, "<diretto@x>", 3)))
        assertEquals(listOf("ARTICLE <diretto@x>"), comandi.toList())
    }

    @Test
    fun `430 per Message-ID e 220 per numero con lo stesso Message-ID - arriva`() = conServer { c, comandi ->
        assertEquals("<a@x>", messageIdDi(scaricaArticolo(c, "<a@x>", 7)))
        assertEquals(listOf("ARTICLE <a@x>", "ARTICLE 7"), comandi.toList())
    }

    @Test
    fun `per numero arriva un altro messaggio - scartato, resta il 430`() = conServer { c, _ ->
        assertEquals(430, codice { scaricaArticolo(c, "<b@x>", 8) })
    }

    @Test
    fun `430 e poi 423 - il server non lo trova`() = conServer { c, _ ->
        assertEquals(423, codice { scaricaArticolo(c, "<c@x>", 9) })
    }

    @Test
    fun `numero negativo, un ricostruito - nessun secondo tentativo`() = conServer { c, comandi ->
        assertEquals(430, codice { scaricaArticolo(c, "<d@x>", -3) })
        assertEquals(listOf("ARTICLE <d@x>"), comandi.toList())
    }

    @Test
    fun `numero zero - nessun secondo tentativo`() = conServer { c, comandi ->
        assertEquals(430, codice { scaricaArticolo(c, "<d@x>", 0) })
        assertTrue(comandi.none { it == "ARTICLE 0" })
    }
}
