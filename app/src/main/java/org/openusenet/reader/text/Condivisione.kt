/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.text

/*
 * 1.6.4 (Halv Skjaldson, news.software.readers, 12/11) — IL TESTO CONDIVISO.
 *
 * Halv condivide da Feedly, YouTube, Google, app di IA «come manda un link agli
 * amici»: in NeReO vuole un messaggio nuovo con titolo e link, e il cursore
 * sotto il link per scrivere il suo commento. Con PiaoHong il link arrivava
 * spesso rotto.
 *
 * Android passa un oggetto (EXTRA_SUBJECT, non sempre) e un testo (EXTRA_TEXT).
 * Cosa ci sia dentro lo decide l'app che condivide. Qui la regola, in Kotlin puro:
 *  - l'oggetto diventa una riga sola;
 *  - ogni link va su una riga da sola, intero (l'invio non spezza le parole
 *    piu' lunghe del limite: vedi WrapBody), senza il punto o la virgola che
 *    lo seguono nel testo;
 *  - senza oggetto, la prima riga che non e' un link diventa l'oggetto;
 *  - una riga uguale all'oggetto non si ripete nel corpo;
 *  - righe vuote ripetute diventano una.
 * NeReO non scarica la pagina: il riassunto c'e' solo se l'app lo manda.
 */

data class Condiviso(val oggetto: String, val corpo: String)

private val LINK = Regex("""https?://\S+""")
private const val CODA_DEL_LINK = ".,;:!?"

/** Il corpo finisce con un a capo: il cursore va sulla riga sotto il link. */
fun testoCondiviso(oggetto: String?, testo: String?): Condiviso {
    var ogg = oggetto.orEmpty().replace(Regex("\\s+"), " ").trim()
    val separato = testo.orEmpty().replace("\r\n", "\n").replace(LINK) { m ->
        val link = m.value.trimEnd { it in CODA_DEL_LINK }
        "\n" + link + "\n" + m.value.substring(link.length)
    }
    val righe = ArrayList<String>()
    for (r in separato.split('\n').map { it.trim() }) {
        // il punto rimasto dopo un link staccato non e' una riga
        if (r.isNotEmpty() && r.all { it in CODA_DEL_LINK }) continue
        if (r.isEmpty() && (righe.isEmpty() || righe.last().isEmpty())) continue
        righe.add(r)
    }
    if (ogg.isEmpty()) {
        val i = righe.indexOfFirst { it.isNotEmpty() && !LINK.matches(it) }
        if (i >= 0) { ogg = righe[i]; righe.removeAt(i) }
    } else {
        val i = righe.indexOfFirst { it == ogg }
        if (i >= 0) righe.removeAt(i)
    }
    while (righe.isNotEmpty() && righe.first().isEmpty()) righe.removeAt(0)
    while (righe.isNotEmpty() && righe.last().isEmpty()) righe.removeAt(righe.lastIndex)
    val corpo = if (righe.isEmpty()) "" else righe.joinToString("\n") + "\n"
    return Condiviso(ogg, corpo)
}
