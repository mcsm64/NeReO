/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.5.9 / 177 — LE FACCINE A CARATTERI DIVENTANO EMOJI, SOLO A SCHERMO.
 *
 * Richiesta di Marco (27/10/2026): come fa MesNews, «8-o 8-o» si legge come
 * due faccine stupite. Piano A1 scelto da Marco: emoji Unicode, niente
 * immagini nell'APK, e il TESTO NON CAMBIA. Qui si dice soltanto DOVE sono le
 * faccine e QUALE emoji va al loro posto; il disegno lo fa ArticleBody.kt,
 * che lascia nel testo i caratteri originali e ci disegna sopra l'emoji
 * (`appendInlineContent`). Cosi' «Copia», «Cita», la ricerca, i filtri, la
 * risposta e l'esportazione continuano a vedere `8-o`.
 *
 * QUANDO UNA SEQUENZA E' UNA FACCINA: solo se e' ISOLATA.
 *  - prima: inizio riga, uno spazio, oppure soltanto il prefisso di
 *    citazione (`>:-)` nello stile chevron);
 *  - dopo: fine riga o uno spazio.
 * Fra la faccina e il suo confine possono stare segni con un VERSO
 * (1.5.9 / 178, collaudo di Marco: «"8-o 8-o"» restava testo, e MesNews la
 * converte): prima virgolette e parentesi APERTE ([APERTURA]), dopo
 * virgolette, parentesi CHIUSE e punteggiatura ([CHIUSURA]). Si convertono
 * `"8-o"`, `«8-o»`, `(8-o),`; restano testo `l'8-o` e `x"8-o"`, perche' le
 * virgolette stanno attaccate a una parola.
 * Cosi' restano come sono `a:-)b`, `<abc:-)@x>`, `28-o`, `vinto 8-0` (lo zero
 * non e' in tabella: si usa solo la lettera o/O), e le sequenze dentro un URL,
 * che il chiamante esclude passando gli intervalli dei link.
 *
 * ⚠️ `:-))` si legge `:-)` piu' una `)` (piano A1 di Marco): e' la sola
 * lettura che converte anche «(ciao :-))», cioe' la faccina in fondo a una
 * parentesi.
 *
 * LA TABELLA e' quella BASE approvata da Marco il 27/10/2026. Escluse di
 * proposito: `:/` e `:-/` (http://), `:\` (C:\), `XD`, `<3`, `>:-(` (si
 * confonde col prefisso di citazione), `^_^`, `o_O`. Tutte le emoji sono di
 * Unicode 6.0-7.0: le ha anche il carattere di sistema di Android 9 (box TV
 * di Roxy), senza scaricare niente.
 *
 * ⚠️ QUESTO FILE DEVE RESTARE DI KOTLIN PURO, come WrapBody.kt: le prove di
 * CoreTests.kt lo caricano sulla JVM, senza Android.
 */
package org.openusenet.reader.text

/** Una faccina trovata in una riga: caratteri [inizio, fine) da mostrare come [emoji]. */
data class Faccina(val inizio: Int, val fine: Int, val sequenza: String, val emoji: String)

private fun emoji(codice: Int) = String(Character.toChars(codice))

/** Sequenza a caratteri → emoji. Nessuna sequenza e' l'inizio di un'altra. */
val TABELLA_FACCINE: Map<String, String> = linkedMapOf(
    ":-)" to emoji(0x1F642), ":)" to emoji(0x1F642),   // 🙂
    ";-)" to emoji(0x1F609), ";)" to emoji(0x1F609),   // 😉
    ":-(" to emoji(0x1F641), ":(" to emoji(0x1F641),   // 🙁
    ":-D" to emoji(0x1F603), ":D" to emoji(0x1F603),   // 😃
    ":-P" to emoji(0x1F61B), ":P" to emoji(0x1F61B),   // 😛
    ":-p" to emoji(0x1F61B), ":p" to emoji(0x1F61B),
    ":-O" to emoji(0x1F62E), ":O" to emoji(0x1F62E),   // 😮
    ":-o" to emoji(0x1F62E), ":o" to emoji(0x1F62E),
    "8-o" to emoji(0x1F632), "8-O" to emoji(0x1F632),  // 😲 stupito
    ":'(" to emoji(0x1F622), ":'-(" to emoji(0x1F622), // 😢
    ":-|" to emoji(0x1F610),                           // 😐
    ":-*" to emoji(0x1F618),                           // 😘
)

/*
 * Segni che valgono da tutti e due i lati: virgolette, e dalla 1.5.9 / 179
 * gli asterischi e i trattini bassi dell'enfasi Usenet (`*;-)*`, `_:-)_`:
 * MesNews li converte, collaudo di Marco). La `/` del /corsivo/ resta fuori:
 * sta in ogni `http://`.
 */
private const val SIMMETRICI = "\"'«»“”‘’*_"

/** Possono stare fra il confine e la faccina, PRIMA: virgolette, enfasi, parentesi aperte. */
private val APERTURA = (SIMMETRICI + "([").toSet()

/** Possono stare fra la faccina e il confine, DOPO: virgolette, enfasi, parentesi chiuse, punteggiatura. */
private val CHIUSURA = (SIMMETRICI + ")].,;:!?").toSet()

/**
 * Le faccine isolate di [riga], in ordine. [escluse] sono intervalli di
 * caratteri da non toccare (gli URL riconosciuti): una sequenza che li tocca
 * anche solo in parte resta testo.
 */
fun trovaFaccine(riga: String, escluse: List<IntRange> = emptyList()): List<Faccina> {
    // fino a qui solo `>` e spazi: il prefisso di citazione
    val finePrefisso = riga.indexOfFirst { it != '>' && !it.isWhitespace() }
        .let { if (it < 0) riga.length else it }
    val out = ArrayList<Faccina>()
    var i = 0
    while (i < riga.length) {
        val trovata = if (!sinistraIsolata(riga, i, finePrefisso)) null
        else TABELLA_FACCINE.entries.firstOrNull { (seq, _) ->
            riga.startsWith(seq, i) && destraIsolata(riga, i + seq.length) &&
                escluse.none { it.first < i + seq.length && it.last >= i }
        }
        if (trovata != null) {
            val fine = i + trovata.key.length
            out.add(Faccina(i, fine, trovata.key, trovata.value))
            i = fine
        } else i++
    }
    return out
}

private fun sinistraIsolata(riga: String, inizio: Int, finePrefisso: Int): Boolean {
    var j = inizio - 1
    while (j >= 0 && riga[j] in APERTURA) j--
    return j < finePrefisso || riga[j].isWhitespace()
}

private fun destraIsolata(riga: String, fine: Int): Boolean {
    var k = fine
    while (k < riga.length && riga[k] in CHIUSURA) k++
    return k >= riga.length || riga[k].isWhitespace()
}
