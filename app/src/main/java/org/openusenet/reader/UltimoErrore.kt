/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.2 / 189 (proposta di HAL, 9 novembre 2026) — L'ULTIMO ERRORE OPERATIVO.
 *
 * Il caso: un invio su Astraweb fallito una volta, col log NNTP acceso. Quando
 * il log e' stato esportato, le righe del primo tentativo erano gia' uscite dal
 * buffer (400 righe, solo in RAM), spinte fuori dal reinvio riuscito e da cio'
 * che e' venuto dopo. L'app non era crashata, quindi nemmeno CrashLog aveva
 * niente da dire.
 *
 * Qui si conserva l'ultimo errore NON fatale di un invio o di un aggiornamento:
 * quando, operazione, server, gruppo, tipo e messaggio dell'errore, piu' le
 * ultime [RIGHE_LOG] righe del log NNTP COPIATE NEL MOMENTO DELL'ERRORE. E'
 * questa copia a risolvere il caso: quello che succede dopo non la tocca.
 *
 * Un file solo, sovrascritto a ogni errore: l'impatto e' di qualche KB.
 *
 * Kotlin senza Android, come NntpLog: il file e l'intestazione li imposta
 * App.onCreate, cosi' [componi] si prova nei test di unita'.
 *
 * PRIVACY: le righe copiate sono quelle del log NNTP, gia' oscurate alla fonte
 * (credenziali) e senza corpi degli articoli. Il messaggio d'errore e' quello
 * del server o di Java. Il file resta sul dispositivo: esce solo se l'utente lo
 * copia o lo esporta.
 */
package org.openusenet.reader

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.openusenet.reader.nntp.NntpLog

object UltimoErrore {
    /** Quante righe del log NNTP si copiano insieme all'errore. */
    const val RIGHE_LOG = 40

    /** Impostato da App.onCreate (filesDir/last_error.txt). null: non si salva nulla. */
    @Volatile
    var file: File? = null

    /** Versione e dispositivo, impostati da App.onCreate. */
    @Volatile
    var intestazione: String = ""

    /**
     * Registra [errore]. Non lancia mai: la diagnostica non deve cambiare cio'
     * che succede all'utente, che riceve l'errore esattamente come prima.
     */
    fun registra(operazione: String, server: String?, gruppo: String?, errore: Throwable) {
        val f = file ?: return
        runCatching {
            val quando = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val righe = if (NntpLog.enabled) NntpLog.ultimeRighe(RIGHE_LOG) else null
            f.writeText(componi(quando, intestazione, operazione, server, gruppo, errore, righe))
        }
    }

    /**
     * Il testo del file. [righeLog] null: log NNTP spento, e lo si dice invece
     * di lasciare il vuoto (chi legge deve sapere che non c'era, non che manca).
     */
    fun componi(
        quando: String,
        intestazione: String,
        operazione: String,
        server: String?,
        gruppo: String?,
        errore: Throwable,
        righeLog: List<String>?,
    ): String = buildString {
        appendLine("NeReO — ultimo errore operativo")
        appendLine("Quando: $quando")
        if (intestazione.isNotEmpty()) appendLine(intestazione)
        appendLine("Operazione: $operazione")
        appendLine("Server: ${server ?: "-"}")
        appendLine("Gruppo: ${gruppo ?: "-"}")
        appendLine("Eccezione: ${errore.javaClass.simpleName}")
        appendLine("Messaggio: ${errore.message ?: "-"}")
        // La causa, se c'e': un errore TLS o di rete arriva spesso avvolto.
        val causa = errore.cause
        if (causa != null && causa !== errore) {
            appendLine("Causa: ${causa.javaClass.simpleName}: ${causa.message ?: "-"}")
        }
        appendLine()
        if (righeLog == null) {
            appendLine("Log NNTP spento: nessuna riga copiata.")
        } else {
            appendLine("Ultime ${righeLog.size} righe del log NNTP al momento dell'errore:")
            righeLog.forEach { appendLine(it) }
        }
    }

    /** Testo dell'ultimo errore registrato, o stringa vuota se non ce n'e' uno. */
    fun snapshot(): String =
        runCatching { file?.takeIf { it.exists() }?.readText() }.getOrNull().orEmpty()

    /** Cancella l'errore salvato. */
    fun clear() {
        runCatching { file?.delete() }
    }
}
