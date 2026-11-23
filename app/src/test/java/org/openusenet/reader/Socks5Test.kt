/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader

import java.io.DataInputStream
import java.net.InetAddress
import java.net.ServerSocket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.nntp.NntpClient
import org.openusenet.reader.nntp.ProxySocks
import org.openusenet.reader.nntp.ProxySocksException

/** 1.6.5 / 193: proxy SOCKS5 finto su socket locale, RFC 1928. */
class Socks5Test {
    /** Cio' che il proxy finto ha ricevuto nella richiesta CONNECT. */
    private class Ricevuto { @Volatile var host: String? = null; @Volatile var porta: Int = -1 }

    /**
     * Un proxy SOCKS5 finto. [metodo]: la risposta al saluto (00 o FF); [rep]: la
     * risposta al CONNECT. Riuscito, fa anche da server NNTP (saluto 200).
     * [nonSocks]: risponde come un server qualunque, non SOCKS.
     */
    private fun conProxy(metodo: Int = 0, rep: Int = 0, nonSocks: Boolean = false, prova: (ProxySocks, Ricevuto) -> Unit) {
        val ricevuto = Ricevuto()
        val presa = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val filo = Thread {
            runCatching {
                presa.accept().use { s ->
                    val inp = DataInputStream(s.getInputStream())
                    val out = s.getOutputStream()
                    if (nonSocks) { out.write("HTTP/1.1 400 Bad Request\r\n\r\n".toByteArray()); out.flush(); return@use }
                    val ver = inp.readUnsignedByte(); val n = inp.readUnsignedByte(); repeat(n) { inp.readUnsignedByte() }
                    check(ver == 5)
                    out.write(byteArrayOf(5, metodo.toByte())); out.flush()
                    if (metodo != 0) return@use
                    inp.readUnsignedByte(); inp.readUnsignedByte(); inp.readUnsignedByte() // VER CMD RSV
                    check(inp.readUnsignedByte() == 3) // ATYP: nome di dominio
                    val nome = ByteArray(inp.readUnsignedByte()).also { inp.readFully(it) }
                    ricevuto.host = String(nome, Charsets.US_ASCII)
                    ricevuto.porta = inp.readUnsignedShort()
                    out.write(byteArrayOf(5, rep.toByte(), 0, 1, 0, 0, 0, 0, 0, 0)); out.flush()
                    if (rep != 0) return@use
                    val w = out.bufferedWriter(Charsets.ISO_8859_1)
                    val r = inp.bufferedReader(Charsets.ISO_8859_1)
                    w.write("200 finto\r\n"); w.flush()
                    while (true) {
                        val cmd = r.readLine() ?: break
                        if (cmd.startsWith("QUIT")) { w.write("205 ciao\r\n"); w.flush(); break }
                        w.write(if (cmd.startsWith("CAPABILITIES")) "101 elenco\r\nVERSION 2\r\nREADER\r\n.\r\n" else "500 no\r\n"); w.flush()
                    }
                }
            }
        }
        filo.isDaemon = true
        filo.start()
        try {
            prova(ProxySocks("127.0.0.1", presa.localPort), ricevuto)
        } finally {
            presa.close()
        }
    }

    private fun errore(blocco: () -> Unit): ProxySocksException =
        runCatching(blocco).exceptionOrNull() as ProxySocksException

    @Test
    fun `il nome del server va al proxy, e il telefono non lo risolve`() = conProxy { proxy, ricevuto ->
        // Un .onion non si risolve con nessun DNS: se NeReO lo chiedesse, fallirebbe qui.
        NntpClient("abcdefghijklmnop.onion", 119, proxy = proxy).use { it.connect() }
        assertEquals("abcdefghijklmnop.onion", ricevuto.host)
        assertEquals(119, ricevuto.porta)
    }

    @Test
    fun `il proxy rifiuta - codice e motivo`() = conProxy(rep = 5) { proxy, _ ->
        val e = errore { NntpClient("news.example.org", 563, proxy = proxy).use { it.connect() } }
        assertEquals(ProxySocksException.Tipo.RIFIUTO, e.tipo)
        assertEquals(5, e.codice)
        assertTrue(e.message!!.contains("refused"))
    }

    @Test
    fun `il proxy chiede una password - non supportato, detto chiaro`() = conProxy(metodo = 0xFF) { proxy, _ ->
        val e = errore { NntpClient("news.example.org", 119, proxy = proxy).use { it.connect() } }
        assertEquals(ProxySocksException.Tipo.RICHIEDE_PASSWORD, e.tipo)
    }

    @Test
    fun `dall'altra parte non c'e' un proxy SOCKS5`() = conProxy(nonSocks = true) { proxy, _ ->
        val e = errore { NntpClient("news.example.org", 119, proxy = proxy).use { it.connect() } }
        assertEquals(ProxySocksException.Tipo.NON_SOCKS5, e.tipo)
    }

    @Test
    fun `proxy spento - non raggiungibile, con il suggerimento`() {
        val porta = ServerSocket(0).use { it.localPort }
        val e = errore { NntpClient("news.example.org", 119, proxy = ProxySocks("127.0.0.1", porta)).use { it.connect() } }
        assertEquals(ProxySocksException.Tipo.NON_RAGGIUNGIBILE, e.tipo)
        assertTrue(e.message!!.contains("Orbot"))
    }
}
