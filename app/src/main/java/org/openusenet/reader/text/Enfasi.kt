/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.1 / 187 — *GRASSETTO*, /CORSIVO/ E _SOTTOLINEATO_, SOLO A SCHERMO.
 *
 * Richiesta di Marco (5/11/2026), dopo una prova in alt.test.test.jojo: MesNews
 * nasconde i segni e applica lo stile, NeReO li mostrava. `-testo-` e `\testo\`
 * restano come sono, come in MesNews (Marco: `\` si usa nelle URL, `--` in firma).
 *
 * Qui si dice soltanto DOVE sono i segni. Il disegno sta in ArticleBody.kt, e
 * come per le faccine il TESTO NON CAMBIA: i segni restano nella stringa, a
 * grandezza zero. Cosi' «Copia», «Cita», la ricerca e i filtri li vedono ancora.
 *
 * QUANDO DUE SEGNI SONO UNA COPPIA, su una riga sola:
 *  - quello che apre ha davanti inizio riga, uno spazio o un segno di apertura;
 *  - quello che chiude ha dopo fine riga, uno spazio, un segno di chiusura o
 *    punteggiatura;
 *  - il testo in mezzo non comincia e non finisce con uno spazio, contiene
 *    almeno una lettera o una cifra e non contiene lo stesso segno.
 * Restano cosi' come sono `2*3*4`, `3 * 4 * 5`, `/usr/bin/`, `km/h`, `5/11/2026`,
 * `nome_file_`, `**doppio**` e `*;-)*`, che resta una faccina. Dentro un URL non
 * si guarda: il chiamante passa gli intervalli dei link.
 *
 * ⚠️ E' UNA REGOLA NOSTRA, ricavata dalle prove di Marco e dai post visti in
 * MesNews, non quella di MesNews letta alla fonte. Un `/etc/` isolato diventa
 * corsivo: e' il prezzo di riconoscere `/TEST/`.
 *
 * ⚠️ QUESTO FILE DEVE RESTARE DI KOTLIN PURO, come Faccine.kt: le prove lo
 * caricano sulla JVM, senza Android.
 */
package org.openusenet.reader.text

enum class TipoEnfasi { GRASSETTO, CORSIVO, SOTTOLINEATO }

/** Una coppia di segni: [apertura] e [chiusura] sono le loro posizioni nella riga. */
data class Enfasi(val apertura: Int, val chiusura: Int, val tipo: TipoEnfasi)

private val SEGNI = mapOf(
    '*' to TipoEnfasi.GRASSETTO,
    '/' to TipoEnfasi.CORSIVO,
    '_' to TipoEnfasi.SOTTOLINEATO,
)

/** Possono stare davanti al segno che apre. `>` e' il prefisso di citazione. */
private const val PRIMA = "([{<>\"'«“‘"

/** Possono stare dopo il segno che chiude. */
private const val DOPO = ")]}>\"'»”’.,;:!?…"

private fun puoAprire(riga: String, i: Int) =
    i == 0 || riga[i - 1].isWhitespace() || riga[i - 1] in PRIMA

private fun puoChiudere(riga: String, j: Int) =
    j == riga.lastIndex || riga[j + 1].isWhitespace() || riga[j + 1] in DOPO

/** Dove si chiude la coppia aperta in [i], o -1. Conta il primo segno uguale: mai altri. */
private fun chiusura(riga: String, i: Int, esclusi: List<IntRange>): Int {
    val j = riga.indexOf(riga[i], i + 1)
    if (j <= i + 1 || !puoChiudere(riga, j)) return -1
    if (riga[i + 1].isWhitespace() || riga[j - 1].isWhitespace()) return -1
    if (esclusi.any { it.first <= j && it.last >= i }) return -1
    return if (riga.substring(i + 1, j).any { it.isLetterOrDigit() }) j else -1
}

/** Le coppie di [riga], in ordine. [esclusi]: gli intervalli dei link. Niente annidamenti. */
fun trovaEnfasi(riga: String, esclusi: List<IntRange> = emptyList()): List<Enfasi> {
    val out = ArrayList<Enfasi>()
    var i = 0
    while (i < riga.length) {
        val tipo = SEGNI[riga[i]]
        val fine = if (tipo != null && puoAprire(riga, i)) chiusura(riga, i, esclusi) else -1
        if (tipo != null && fine >= 0) {
            out.add(Enfasi(i, fine, tipo))
            i = fine + 1
        } else {
            i++
        }
    }
    return out
}
