/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.nntp

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/*
 * 1.6.5 / 193 (richiesta di un utente, via Marco, 20/11) — IL PROXY SOCKS5.
 *
 * Per usare NeReO con Orbot/Tor (127.0.0.1:9050) o con il client SOCKS5 della
 * rete Nym. Opzione PER SERVER, spenta di default: chi non la accende non passa
 * mai di qui. NeReO resta solo testo: cambia solo il trasporto.
 *
 * La stretta di mano e' scritta qui invece di usare java.net.Proxy: si
 * comporta uguale su ogni Android (la versione di libcore verificata manda il
 * nome al proxy, ma Android 8 e 9 non sono stati verificati), non chiede MAI il
 * DNS del server al telefono, e gli errori si distinguono per codice.
 *
 * RFC 1928 (verificata alla fonte il 20/11):
 *  - §3 saluto: VER=05, NMETHODS, METHODS; risposta VER, METHOD. 00 = nessuna
 *    autenticazione, FF = nessun metodo accettabile (il client chiude);
 *  - §4 richiesta: VER=05, CMD=01 (CONNECT), RSV=00, ATYP=03 (nome di dominio:
 *    primo ottetto la lunghezza, niente NUL finale), DST.PORT su due ottetti;
 *  - §6 risposta: VER, REP, RSV, ATYP, BND.ADDR, BND.PORT; REP 00 = riuscito,
 *    01-08 errori.
 * Niente autenticazione del proxy: non chiesta.
 */

data class ProxySocks(val host: String, val porta: Int) {
    override fun toString() = "$host:$porta"
}

class ProxySocksException(val tipo: Tipo, val codice: Int, messaggio: String) : IOException(messaggio) {
    enum class Tipo { NON_RAGGIUNGIBILE, RICHIEDE_PASSWORD, NON_SOCKS5, RIFIUTO }
}

/** Il motivo di un rifiuto (REP 01-08, RFC 1928 §6), per il messaggio. */
fun motivoRifiutoSocks(rep: Int): String = when (rep) {
    1 -> "general proxy failure"
    2 -> "connection not allowed by the proxy"
    3 -> "network unreachable"
    4 -> "host unreachable (check the server name)"
    5 -> "connection refused by the server"
    6 -> "the route expired (on Tor: try again)"
    7 -> "command not supported by the proxy"
    8 -> "address type not supported by the proxy"
    else -> "unknown reply"
}

/**
 * Apre il socket verso [proxy] e chiede la connessione a [host]:[porta].
 * Il nome del server va al proxy cosi' com'e': il telefono non lo risolve.
 * I messaggi sono in inglese come gli altri di NntpClient.
 */
fun apriTramiteSocks(proxy: ProxySocks, host: String, porta: Int, timeoutMs: Int): Socket {
    val s = Socket()
    try {
        s.connect(InetSocketAddress(proxy.host, proxy.porta), timeoutMs)
    } catch (e: IOException) {
        runCatching { s.close() }
        throw ProxySocksException(
            ProxySocksException.Tipo.NON_RAGGIUNGIBILE, 0,
            "SOCKS proxy $proxy not reachable: is Orbot (or the Nym client) running?",
        )
    }
    try {
        s.soTimeout = timeoutMs
        strettaSocks5(s.getInputStream(), s.getOutputStream(), proxy, host, porta)
        return s
    } catch (e: Exception) {
        runCatching { s.close() }
        throw e
    }
}

/** La stretta di mano SOCKS5, sugli stream gia' aperti verso il proxy. */
fun strettaSocks5(input: InputStream, output: OutputStream, proxy: ProxySocks, host: String, porta: Int) {
    val nome = host.toByteArray(Charsets.US_ASCII)
    require(nome.size in 1..255) { "Server name too long for SOCKS5" }
    output.write(byteArrayOf(5, 1, 0))
    output.flush()
    val ver = leggi(input, proxy)
    val metodo = leggi(input, proxy)
    if (ver != 5) throw nonSocks5(proxy)
    if (metodo == 0xFF) {
        throw ProxySocksException(
            ProxySocksException.Tipo.RICHIEDE_PASSWORD, 0xFF,
            "The SOCKS proxy $proxy asks for a password: not supported",
        )
    }
    if (metodo != 0) throw nonSocks5(proxy)
    val richiesta = byteArrayOf(5, 1, 0, 3, nome.size.toByte()) + nome +
        byteArrayOf((porta shr 8).toByte(), (porta and 0xFF).toByte())
    output.write(richiesta)
    output.flush()
    if (leggi(input, proxy) != 5) throw nonSocks5(proxy)
    val rep = leggi(input, proxy)
    leggi(input, proxy) // RSV
    val lunghezza = when (leggi(input, proxy)) {
        1 -> 4
        4 -> 16
        3 -> leggi(input, proxy)
        else -> throw nonSocks5(proxy)
    }
    repeat(lunghezza + 2) { leggi(input, proxy) } // BND.ADDR e BND.PORT
    if (rep != 0) {
        throw ProxySocksException(
            ProxySocksException.Tipo.RIFIUTO, rep,
            "The SOCKS proxy $proxy refused the connection to $host:$porta: ${motivoRifiutoSocks(rep)} (SOCKS code $rep)",
        )
    }
}

private fun leggi(input: InputStream, proxy: ProxySocks): Int {
    val b = input.read()
    if (b < 0) throw nonSocks5(proxy)
    return b
}

private fun nonSocks5(proxy: ProxySocks) = ProxySocksException(
    ProxySocksException.Tipo.NON_SOCKS5, 0,
    "The proxy $proxy did not answer as a SOCKS5 proxy",
)

/** Aggiunto ai rifiuti del server quando si passa da un proxy. */
const val NOTA_SERVER_E_TOR = " (some servers do not accept connections through Tor or proxies)"
