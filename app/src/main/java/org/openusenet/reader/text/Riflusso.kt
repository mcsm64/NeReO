/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.1 (BIG Umberto) — IL RIFLUSSO: QUALI RIGHE SONO LO STESSO CAPOVERSO.
 *
 * Una decisione sola, [siUnisceAllaPrecedente], per la lettura ([riflussoBlocco],
 * prima `reflowLines` in ArticleBody.kt, privata e senza test) e per «Riformatta»
 * in scrittura (ui/RiformattaCitazione.kt). Sostituisce `interruzioneVoluta`.
 *
 * LA STORIA, perche' ogni regola qui sotto ha un post che la giustifica:
 *
 * - v1: soglia fissa a 60. Chi impaginava piu' stretto non veniva riconosciuto.
 * - v2: larghezza stimata per blocco (la riga piu' lunga) e riga «piena» quando
 *   la prima parola della riga dopo non ci sarebbe stata. E' il modello vero
 *   dell'a-capo automatico.
 * - 1.2.3 (Bingo3331, Dave Royal): soglia minima da 40 a 30, per chi impagina
 *   a 38-40 colonne.
 * - 1.2.4 (Bingo3331): le orfane in mezzo al capoverso («e'», «sono», «delle
 *   altre») lo spezzavano. Da allora si univa SEMPRE, salvo riga corta chiusa
 *   da punteggiatura.
 * - 1.6.1 (BIG Umberto, it.tlc.cellulari.android): quel «sempre» univa una
 *   colonna di numeri di telefono (tre per riga a schermo), attaccava alla frase
 *   un link che stava da solo, e ignorava gli a capo voluti dopo un punto.
 *
 * ⚠️ IL CRITERIO SEMPLICE NON BASTA, ed e' stato provato. «Si unisce solo una
 * riga lunga, una riga breve chiude il capoverso» riporta il difetto della 1.2.4
 * (il pettine dopo una citazione) e attacca alla riga piu' lunga del blocco la
 * prima riga breve che segue, perche' quella risulta piena per definizione. Da
 * qui le quattro eccezioni. Il prototipo e' stato provato sul post vero di
 * Umberto: i casi sono in `RiflussoTest`.
 *
 * Il limite noto, scritto nei test: un pettine con DUE righe corte di fila
 * («...fa quasi la / stessa / funzione.») resta spezzato, perche' due righe corte
 * sono indistinguibili da una colonna. Nel dubbio non si unisce: una riga corta
 * rimasta al suo posto e' solo meno bella, un elenco unito e' illeggibile.
 *
 * Kotlin puro, come WrapBody.kt: si prova in `CoreTests`.
 */
package org.openusenet.reader.text

/** Tetto alla stima: una riga mai andata a capo non deve falsarla. */
private const val LARGHEZZA_MAX_STIMA = 90

private val SOLO_URL = Regex("""<?(?:https?://|www\.)\S+>?""", RegexOption.IGNORE_CASE)

/** Punteggiatura che chiude una frase. */
private val FINE_FRASE = setOf('.', '!', '?', '…')

/** Punteggiatura che chiude una riga corta: etichette, saluti, frasi brevi. */
private val CHIUSURA = FINE_FRASE + setOf(':', ';')

/** Punteggiatura che esclude l'orfana: la riga finiva li' per scelta. */
private val PUNTEGGIATURA_TERMINALE = CHIUSURA + ','

/** Una riga fatta solo di un indirizzo web: non si unisce mai, ne' prima ne' dopo. */
fun rigaSoloUrl(riga: String): Boolean = SOLO_URL.matches(riga.trim())

/**
 * Dove andava a capo il mittente: la riga piu' lunga, senza i soli URL (un
 * indirizzo lungo farebbe credere a un margine che non c'e', e non si unirebbe
 * piu' niente).
 */
fun stimaLarghezza(righe: List<String>): Int =
    (righe.filter { it.isNotBlank() && !rigaSoloUrl(it) }.maxOfOrNull { it.trimEnd().length } ?: 0)
        .coerceAtMost(LARGHEZZA_MAX_STIMA)

/**
 * La riga [i] di [righe] (testo senza segni di citazione) continua la
 * precedente? [larghezza] e' quella di [stimaLarghezza]. Le righe vuote e i
 * limiti del blocco li gestisce chi chiama: qui la precedente esiste.
 */
fun siUnisceAllaPrecedente(righe: List<String>, i: Int, larghezza: Int): Boolean {
    val riga = righe[i].trim()
    val prec = righe[i - 1].trim()
    if (riga.isEmpty() || prec.isEmpty()) return false
    if (rigaSoloUrl(riga) || rigaSoloUrl(prec) || MARCA_ELENCO.containsMatchIn(riga)) return false

    fun testo(k: Int) = righe.getOrNull(k)?.trim().orEmpty()
    fun corta(k: Int) = testo(k).let { it.isNotEmpty() && !rigaSoloUrl(it) && it.length * 10 < larghezza * 6 }
    fun lunga(k: Int) = testo(k).let { !rigaSoloUrl(it) && it.length * 4 >= larghezza * 3 }
    // coda «a pettine»: corta, fra due righe lunghe, senza punteggiatura finale
    fun orfana(k: Int) = testo(k).let {
        k > 0 && lunga(k - 1) && lunga(k + 1) && it.isNotEmpty() &&
            it.length * 5 <= larghezza * 2 && it.last() !in PUNTEGGIATURA_TERMINALE
    }

    // due righe corte di fila sono una colonna, non la coda di un capoverso
    if (corta(i) && corta(i + 1) && !orfana(i)) return false
    // riga corta dopo una riga chiusa: la frase era finita, la corta sta da sola
    if (corta(i) && prec.last() in CHIUSURA) return false
    // fine frase e maiuscola: il «punto a capo», anche a fine riga piena
    if (prec.last() in FINE_FRASE && riga.first().isUpperCase()) return false

    // il margine stimato puo' essere un po' largo: 9/10 vale gia' riga piena
    val piena = prec.length * 10 >= larghezza * 9 ||
        prec.length + 1 + riga.substringBefore(' ').length > larghezza
    // un'orfana si riattacca alla riga dopo solo se quella continua la frase:
    // «'pecorazzi et similia'» seguita da «Questo lo fa...» finiva li'
    return piena || orfana(i) || (orfana(i - 1) && riga.first().isLowerCase())
}

/**
 * Ricompone i capoversi di UN blocco (stesso livello di citazione, mai la firma).
 * L'a-capo forzato mostrato su schermo stretto lascia righe con una-due parole:
 * qui si ricuciono. Sotto [LARGHEZZA_MIN_IMPAGINATA] non e' testo impaginato
 * (elenchi, versi, messaggi brevi) e il blocco resta intatto. Il prefisso di
 * citazione viene tolto e rimesso uniforme, cosi' il render funziona come prima.
 */
fun riflussoBlocco(lines: List<String>, depth: Int): List<String> {
    val prefix = if (depth > 0) ">".repeat(depth) + " " else ""
    val bare = lines.map { if (depth > 0) it.dropWhile { c -> c == '>' || c == ' ' } else it }
    val w = stimaLarghezza(bare)
    if (w < LARGHEZZA_MIN_IMPAGINATA) return lines

    val out = ArrayList<String>()
    val para = StringBuilder()
    fun flush() {
        if (para.isNotEmpty()) { out.add(prefix + para.toString()); para.setLength(0) }
    }
    for ((i, content) in bare.withIndex()) {
        if (content.isBlank()) {
            flush()
            out.add(lines[i]) // riga vuota: separa i capoversi, mantenuta com'e'
            continue
        }
        val trimmed = content.trim()
        when {
            para.isEmpty() -> para.append(trimmed)
            siUnisceAllaPrecedente(bare, i, w) -> para.append(' ').append(trimmed)
            else -> { flush(); para.append(trimmed) }
        }
    }
    flush()
    return out
}
