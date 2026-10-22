/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Import/export dei filtri in un formato testuale ispirato allo scorefile
 * di slrn — leggibile, versionabile, condivisibile via ngroup o email.
 *
 * Formato:
 *   [it.hobby.*]                  <- scope wildmat (opzionale, default *)
 *   Score: -1000
 *   From: regex:spammer@.*\.xyz
 *   Expires: 2026-12-31           <- opzionale
 *   %Nome regola facoltativo
 *
 * M1 (release personale Marco) — estensione alle risposte:
 *   [*]
 *   %Plonk Pinco esteso alle risposte
 *   Score: -9999
 *   From: regex:pinco@.*\.example
 *   Followup: subtree             <- none | direct | subtree
 *   FollowupScore: -100           <- opzionale, default -100 (soglia HIDE)
 *
 * 1.4.4 — filtro sul lettore di news:
 *   [*]
 *   %Esempio: plonk per programma di scrittura
 *   Score: -9999
 *   Newsreader: regex:EsempioReader/1\\.[0-3]
 *   Followup: subtree
 *
 * 1.5.7 — anti-sockpuppet e intestazioni:
 *   From: nickaddr:*              <- nick = indirizzo (senza @ e .)
 *   Headers: x-antivirus: Norton  <- in tutte le intestazioni, nomi in minuscolo
 *
 * `Newsreader` guarda User-Agent, X-Newsreader e X-Mailer insieme. Una regola
 * cosi' si valuta solo sugli articoli gia' scaricati (quelle intestazioni non
 * stanno nel riepilogo overview) — vedi FilterField.NEWSREADER.
 *
 * Le chiavi nuove sono emesse solo quando servono, quindi un kill-file
 * esportato da una versione precedente si rilegge identico e uno esportato da
 * qui resta leggibile da chi non le conosce (le ignorera' come righe sconosciute).
 *
 * Righe che iniziano con '#' = commento. Una regola termina alla riga vuota.
 */
package org.openusenet.reader.filter

import java.time.LocalDate

object KillFile {

    fun export(rules: List<FilterRule>): String = buildString {
        appendLine("# OpenUsenet score file — generated ${LocalDate.now()}")
        appendLine()
        for (r in rules) {
            // lo scope va emesso sempre: altrimenti una regola globale che segue
            // una regola con scope specifico lo "erediterebbe" al re-import
            appendLine("[${r.groupWildmat}]")
            if (r.name.isNotBlank()) appendLine("%${r.name}")
            appendLine("Score: ${r.score}")
            val prefix = when (r.matchType) {
                MatchType.REGEX -> "regex:"
                MatchType.WILDCARD -> "wild:"
                MatchType.CONTAINS -> ""
                // 1.5.7: «From: nickaddr:*» — il pattern non conta, ma deve esserci.
                MatchType.NICK_EMAIL -> "nickaddr:"
            }
            val field = when (r.field) {
                FilterField.FROM -> "From"
                FilterField.SUBJECT -> "Subject"
                FilterField.MESSAGE_ID -> "Message-ID"
                FilterField.XPOST -> "Xpost"
                FilterField.BODY -> "Body"
                FilterField.REFERENCES -> "References"
                // 1.4.4: non "User-Agent", perche' il campo ne guarda TRE
                // (User-Agent, X-Newsreader, X-Mailer) e chiamarlo come una
                // sola farebbe credere che le altre due non contino.
                FilterField.NEWSREADER -> "Newsreader"
                // 1.5.7: tutte le intestazioni, «nome: valore» per riga.
                FilterField.HEADERS -> "Headers"
            }
            appendLine("$field: $prefix${r.pattern}")
            if (r.caseSensitive) appendLine("Case: sensitive")
            // M1: emesse solo se la regola le usa davvero, per non sporcare i
            // kill-file di chi non tocca questa funzione.
            if (r.followup != FollowupMode.NONE) {
                appendLine("Followup: ${r.followup.name.lowercase()}")
                r.followupScore?.let { appendLine("FollowupScore: $it") }
            }
            r.expiresEpochDay?.let { appendLine("Expires: ${LocalDate.ofEpochDay(it)}") }
            appendLine()
        }
    }

    fun parse(text: String): List<FilterRule> {
        val rules = ArrayList<FilterRule>()
        var scope = "*"
        var name = ""
        var score: Int? = null
        var field: FilterField? = null
        var matchType = MatchType.CONTAINS
        var pattern = ""
        var caseSensitive = false
        var expires: Long? = null
        var followup = FollowupMode.NONE
        var followupScore: Int? = null

        fun flush() {
            val f = field
            val s = score
            if (f != null && s != null && pattern.isNotEmpty()) {
                rules.add(
                    FilterRule(
                        name = name, field = f, matchType = matchType, pattern = pattern,
                        score = s, caseSensitive = caseSensitive,
                        groupWildmat = scope, expiresEpochDay = expires,
                        followup = followup, followupScore = followupScore,
                    )
                )
            }
            name = ""; score = null; field = null
            matchType = MatchType.CONTAINS; pattern = ""; caseSensitive = false; expires = null
            followup = FollowupMode.NONE; followupScore = null
        }

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            when {
                line.isEmpty() -> flush()
                line.startsWith("#") -> Unit
                line.startsWith("%") -> name = line.drop(1).trim()
                line.startsWith("[") && line.endsWith("]") -> {
                    flush()
                    scope = line.substring(1, line.length - 1).trim().ifEmpty { "*" }
                }
                else -> {
                    val idx = line.indexOf(':')
                    if (idx <= 0) continue
                    val key = line.substring(0, idx).trim().lowercase()
                    val value = line.substring(idx + 1).trim()
                    when (key) {
                        "score" -> score = value.toIntOrNull()
                        "case" -> caseSensitive = value.equals("sensitive", true)
                        "expires" -> expires = runCatching { LocalDate.parse(value).toEpochDay() }.getOrNull()
                        // M1: valore non riconosciuto ⇒ NONE. Un kill-file
                        // scritto a mano con un refuso degrada a regola classica
                        // invece di far esplodere l'import.
                        "followup" -> followup = when (value.trim().lowercase()) {
                            "subtree", "thread", "yes", "true", "1" -> FollowupMode.SUBTREE
                            "direct", "direttamente", "1level" -> FollowupMode.DIRECT
                            else -> FollowupMode.NONE
                        }
                        "followupscore" -> followupScore = value.toIntOrNull()
                        "from", "subject", "message-id", "xpost", "body", "references", "newsreader", "headers" -> {
                            field = when (key) {
                                "from" -> FilterField.FROM
                                "subject" -> FilterField.SUBJECT
                                "message-id" -> FilterField.MESSAGE_ID
                                "xpost" -> FilterField.XPOST
                                "references" -> FilterField.REFERENCES
                                "newsreader" -> FilterField.NEWSREADER
                                "headers" -> FilterField.HEADERS
                                else -> FilterField.BODY
                            }
                            when {
                                value.startsWith("regex:") -> { matchType = MatchType.REGEX; pattern = value.drop(6) }
                                value.startsWith("wild:") -> { matchType = MatchType.WILDCARD; pattern = value.drop(5) }
                                value.startsWith("nickaddr:") -> { matchType = MatchType.NICK_EMAIL; pattern = value.drop(9).ifEmpty { "*" } }
                                else -> { matchType = MatchType.CONTAINS; pattern = value }
                            }
                        }
                    }
                }
            }
        }
        flush()
        return rules
    }
}
