/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.backup

/*
 * 1.5.6 / 174 — le due regole sui nomi dei backup salvati nella cartella
 * dell'app (vedi BackupLocale.kt). Kotlin puro, nessun import: si prova da solo
 * con kotlinc.
 */

/** Un backup e' un .json: lo stesso criterio del nome proposto all'esportazione. */
fun eNomeBackup(nome: String): Boolean = nome.endsWith(".json", ignoreCase = true)

/**
 * [nome] se non e' gia' preso, altrimenti «nome-2.json», «nome-3.json»...
 * Due backup nello stesso giorno hanno lo stesso nome proposto: senza questo il
 * secondo cancellerebbe il primo senza dirlo. Funzione pura, provata a banco.
 */
fun nomeLibero(nome: String, esistenti: Set<String>): String {
    if (nome !in esistenti) return nome
    val punto = nome.lastIndexOf('.')
    val base = if (punto > 0) nome.substring(0, punto) else nome
    val est = if (punto > 0) nome.substring(punto) else ""
    var n = 2
    while ("$base-$n$est" in esistenti) n++
    return "$base-$n$est"
}
