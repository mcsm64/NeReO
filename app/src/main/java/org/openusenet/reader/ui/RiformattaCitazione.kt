/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6 (sondaggio sui gruppi) — «RIFORMATTA»: RIMETTERE IN ORDINE LE RIGHE CITATE.
 *
 * Il difetto che cura si chiama «a pettine»: una riga scritta a 72 colonne,
 * citata, diventa di 74; il sistema la spezza e resta una coda di una parola,
 * riga si' e riga no. Tagliando pezzi di citazione succede lo stesso. Qui si
 * uniscono le righe dello stesso capoverso e dello stesso livello, e poi le
 * manda a capo [wrapBody], che sa gia' conservare il prefisso e non spezzare
 * le parole lunghe: di nuovo c'e' solo la decisione su COSA unire.
 *
 * LA REGOLA DI PRUDENZA: nel dubbio non si unisce. Unire due cose che non
 * andavano unite cambia il senso di cio' che ha scritto un altro; una riga
 * corta rimasta al suo posto e' solo meno bella. Quindi fanno da barriera, e
 * restano identiche: riga vuota, cambio di livello, attribuzione, segno di
 * taglio, firma (anche citata), rientro, elenco, riga con una parola che non
 * sta nella larghezza (un URL lungo) e, dalla 1.6.1, riga di solo URL. La fine
 * di capoverso si riconosce con le regole della lettura
 * ([siUnisceAllaPrecedente]): una sola euristica in tutta l'app.
 *
 * Le righe NON citate e la firma dell'utente non si guardano nemmeno.
 *
 * Cambiano solo gli a capo: un test verifica che le parole restino identiche.
 * Per questo non serve un «annulla».
 *
 * Coerente con Format=Flowed in uscita: le righe escono entro [BODY_MAX_COLS]
 * e senza spazio in coda, cioe' fisse, quindi `avvolgiFlowed` non le spezza
 * piu' (il giro completo e' nei test).
 *
 * Kotlin puro, nessuna dipendenza da Compose: si prova in `CoreTests`.
 */
package org.openusenet.reader.ui

import org.openusenet.reader.text.BODY_MAX_COLS
import org.openusenet.reader.text.LARGHEZZA_MIN_IMPAGINATA
import org.openusenet.reader.text.MARCA_ELENCO
import org.openusenet.reader.text.rigaSoloUrl
import org.openusenet.reader.text.siUnisceAllaPrecedente
import org.openusenet.reader.text.wrapBody

/** Quante volte al massimo si ripassa il testo: vedi [riformattaCitazioni]. */
private const val MAX_PASSATE = 4

/** Una riga citata smontata. [testo] e' senza prefisso e senza lo spazio convenzionale. */
private class RigaCitata(val grezza: String, val prefisso: String, val livello: Int, val testo: String)

private fun leggiRigaCitata(riga: String): RigaCitata {
    var fine = 0
    var i = 0
    while (i < riga.length && (riga[i] == '>' || riga[i] == ' ' || riga[i] == '\t')) {
        if (riga[i] == '>') fine = i + 1
        i++
    }
    val prefisso = riga.substring(0, fine)
    return RigaCitata(riga, prefisso, livelloCitazione(riga), riga.substring(fine).removePrefix(" "))
}

/**
 * Riga da lasciare identica e da non unire a niente?
 * [seguePiuProfonda]: la riga dopo ha un livello piu' alto, cioe' questa la introduce.
 */
private fun eBarriera(r: RigaCitata, larghezza: Int, seguePiuProfonda: Boolean): Boolean {
    val t = r.testo
    if (t.startsWith(" ") || t.startsWith("\t")) return true           // rientro voluto
    if (MARCA_ELENCO.containsMatchIn(t)) return true
    if (rigaSoloUrl(t)) return true                                    // link da solo: resta da solo
    if (sembraTaglio(t)) return true                                   // comprende `>:-)`, senza lettere
    // sembraAttribuzione guarda anche i marcatori dentro la riga («said»): da
    // soli non bastano, altrimenti una frase qualunque fermerebbe il capoverso.
    if (sembraAttribuzione(t) && (t.trimEnd().endsWith(":") || seguePiuProfonda)) return true
    return t.split(' ').any { it.length > larghezza }                  // URL lungo: non si tocca
}

private fun unaPassata(testo: String, cols: Int): String {
    val righe = testo.lines()
    val out = ArrayList<String>(righe.size)
    var i = 0
    // Livello della firma citata in corso, 0 = nessuna. Valgono le righe dal
    // separatore in giu', finche' il livello non scende.
    var firmaCitata = 0
    while (i < righe.size) {
        val riga = righe[i]
        if (!riga.startsWith(">")) {
            if (riga.trimEnd() == "--") { out.addAll(righe.subList(i, righe.size)); break }
            firmaCitata = 0
            out.add(riga)
            i++
            continue
        }
        val r = leggiRigaCitata(riga)
        if (firmaCitata > 0 && r.livello >= firmaCitata) { out.add(riga); i++; continue }
        firmaCitata = 0
        if (r.testo.trimEnd() == "--") { firmaCitata = r.livello; out.add(riga); i++; continue }
        if (r.testo.isBlank()) { out.add(riga); i++; continue }

        // Un BLOCCO: righe consecutive con testo, allo stesso livello.
        val blocco = ArrayList<RigaCitata>()
        var fine = i
        while (fine < righe.size && righe[fine].startsWith(">")) {
            val c = leggiRigaCitata(righe[fine])
            if (c.livello != r.livello || c.testo.isBlank() || c.testo.trimEnd() == "--") break
            blocco.add(c)
            fine++
        }
        val dopoBlocco = righe.getOrNull(fine)
        val seguePiuProfonda = dopoBlocco != null && dopoBlocco.startsWith(">") &&
            livelloCitazione(dopoBlocco) > r.livello
        val larghezza = (cols - r.prefisso.length - 1).coerceAtLeast(20)
        val barriere = blocco.mapIndexed { k, c ->
            eBarriera(c, larghezza, seguePiuProfonda && k == blocco.lastIndex)
        }
        // Stessa stima della lettura: la riga piu' lunga dice dove andava a capo.
        val stima = blocco.filterIndexed { k, _ -> !barriere[k] }
            .maxOfOrNull { it.testo.trimEnd().length } ?: 0
        val w = stima.coerceAtMost(larghezza)
        val testi = blocco.map { it.testo }

        val capoverso = ArrayList<RigaCitata>()
        fun chiudi() {
            if (capoverso.isEmpty()) return
            val sola = capoverso.singleOrNull()
            if (sola != null && sola.grezza.length <= cols) {
                out.add(sola.grezza)
            } else {
                val unito = capoverso.joinToString(" ") { it.testo.trim() }
                out.addAll(wrapBody(capoverso.first().prefisso + " " + unito, cols).lines())
            }
            capoverso.clear()
        }
        for ((k, c) in blocco.withIndex()) {
            when {
                // Righe corte per scelta (versi, elenchi senza segno): si lascia tutto.
                barriere[k] || (w < LARGHEZZA_MIN_IMPAGINATA && c.grezza.length <= cols) -> {
                    chiudi()
                    out.add(c.grezza)
                }
                capoverso.isNotEmpty() && !siUnisceAllaPrecedente(testi, k, w) -> {
                    chiudi()
                    capoverso.add(c)
                }
                else -> capoverso.add(c)
            }
        }
        chiudi()
        i = fine
    }
    return out.joinToString("\n")
}

/**
 * Riformatta a [cols] colonne le sole righe citate di [testo].
 *
 * Si ripassa fino a che il testo non cambia piu', perche' la larghezza si stima
 * sul testo che c'e': dopo una passata puo' cambiare di poco e riunire altro.
 * Cosi' premere il comando due volte equivale a premerlo una.
 */
fun riformattaCitazioni(testo: String, cols: Int = BODY_MAX_COLS): String {
    var attuale = testo
    repeat(MAX_PASSATE) {
        val dopo = unaPassata(attuale, cols)
        if (dopo == attuale) return attuale
        attuale = dopo
    }
    return attuale
}
