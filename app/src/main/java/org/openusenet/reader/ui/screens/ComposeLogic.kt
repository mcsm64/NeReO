/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Logica di composizione condivisa fra la schermata a pagina intera e la
 * composizione in-pannello: modalità di quoting, firme (anche multiple e
 * casuali, stile MesNews), introduzione alla citazione personalizzabile,
 * ROT13. Unica fonte di verità: evita che le due UI divergano.
 */
package org.openusenet.reader.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.openusenet.reader.data.Article
import org.openusenet.reader.data.Identity
import org.openusenet.reader.thread.Threader
import org.openusenet.reader.ui.righeCitate
import org.openusenet.reader.ui.sembraAttribuzione
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val REPLY_MODES = linkedMapOf(
    "below" to "Scrivi sotto la citazione (netiquette)",
    "above" to "Scrivi sopra la citazione",
    "trimmed" to "Citazione ridotta",
    "none" to "Senza citazione",
)

/**
 * RC01: etichetta tradotta della modalità di citazione.
 * La mappa REPLY_MODES sopra tiene le CHIAVI tecniche (below/above/trimmed/none)
 * e un'etichetta italiana di riserva mai mostrata; il testo che l'utente legge
 * si prende SEMPRE da qui, perché le risorse richiedono un contesto @Composable.
 */
@androidx.compose.runtime.Composable
fun etichettaModalitaCitazione(mode: String): String = when (mode) {
    "below" -> androidx.compose.ui.res.stringResource(org.openusenet.reader.R.string.quote_below)
    "above" -> androidx.compose.ui.res.stringResource(org.openusenet.reader.R.string.quote_above)
    "trimmed" -> androidx.compose.ui.res.stringResource(org.openusenet.reader.R.string.quote_trimmed)
    "none" -> androidx.compose.ui.res.stringResource(org.openusenet.reader.R.string.quote_none)
    // Nessuna delle quattro chiavi note: mostro la chiave stessa, non
    // l'etichetta italiana di riserva (evita italiano in interfaccia inglese).
    else -> mode
}


/** Separatore fra voci multiple (firme, introduzioni) nei campi di testo. */
const val MULTI_SEPARATOR = "%%"

/** Netiquette: la firma vale al massimo 4 righe da 72 colonne. */
const val SIG_MAX_LINES = 4
const val SIG_MAX_COLS = 72

/**
 * 1.3.5 — tetto alle righe FISICHE della firma, righe vuote comprese.
 *
 * Serve perche' da questa versione le righe vuote non consumano piu' il limite
 * di quattro (vedi [trimSignatureLines]): senza un secondo tetto, una firma di
 * quattro righe imbottita di trenta righe vuote sarebbe "valida". Otto e' il
 * minimo che lascia passare il caso chiesto — quattro righe di testo
 * intercalate da una riga vuota ciascuna, cioe' sette righe fisiche.
 */
const val SIG_MAX_PHYSICAL_LINES = 8

/** Introduzione predefinita alla citazione. */
const val DEFAULT_INTRO = "%autore% ha scritto:"

/**
 * ROT13: ruota di 13 posizioni le sole lettere A-Z/a-z. È la sua stessa
 * inversa, quindi la funzione serve sia a codificare sia a decodificare.
 * Su Usenet si usa per spoiler e battute, non è cifratura.
 */
fun rot13(text: String): String = buildString(text.length) {
    for (c in text) {
        append(
            when {
                c in 'a'..'z' -> 'a' + (c - 'a' + 13) % 26
                c in 'A'..'Z' -> 'A' + (c - 'A' + 13) % 26
                else -> c
            }
        )
    }
}

/**
 * Applica i limiti di netiquette alle righe di firma (esclusa la riga "-- ").
 *
 * 1.3.5 — LE RIGHE VUOTE NON CONTANO PIU'.
 *
 * Prima era `lines.take(4)`, cioe' quattro righe FISICHE. Chi scrive una firma
 * spaziata cosi':
 *
 *     -- ⎵
 *     prima riga
 *     (vuota)
 *     seconda riga
 *     (vuota)
 *     terza riga
 *     (vuota)
 *     quarta riga
 *
 * ne vedeva sopravvivere due: la terza e la quarta sparivano, mangiate dalle
 * righe vuote. Richiesta arrivata da un amico di Marco il 13 settembre 2026.
 *
 * Ora il limite di [SIG_MAX_LINES] conta le sole righe CON TESTO, mentre
 * [SIG_MAX_PHYSICAL_LINES] tiene il totale sotto controllo. Le righe vuote in
 * coda si buttano: non aggiungono nulla e allungano il messaggio.
 *
 * NOTA DA DIRE A CHI LA USA: la netiquette classica (son-of-RFC1036) conta
 * quattro righe FISICHE, righe vuote incluse. Questa e' una tolleranza di
 * NeReO: la firma resta valida qui, ma un altro lettore puo' comunque
 * troncarla. E' un allentamento deliberato, non uno standard.
 *
 * Vale in entrambi i sensi, perche' la funzione e' una sola: quando la firma
 * si COMPONE (invio) e quando si LEGGE quella altrui (ArticleBody).
 */
fun trimSignatureLines(lines: List<String>): List<String> {
    val out = ArrayList<String>(lines.size.coerceAtMost(SIG_MAX_PHYSICAL_LINES))
    var conTesto = 0
    for (riga in lines) {
        if (out.size >= SIG_MAX_PHYSICAL_LINES) break
        if (riga.isBlank()) {
            // una riga vuota non consuma il limite delle quattro, ma consuma
            // quello fisico: e' li' che si ferma chi ne mette trenta
            out.add(riga)
            continue
        }
        if (conTesto >= SIG_MAX_LINES) break
        conTesto++
        out.add(if (riga.length > SIG_MAX_COLS) riga.take(SIG_MAX_COLS) else riga)
    }
    while (out.isNotEmpty() && out.last().isBlank()) out.removeAt(out.size - 1)
    return out
}

/** Spezza un campo multi-voce sul separatore, scartando le voci vuote. */
fun splitMulti(text: String): List<String> =
    text.split(MULTI_SEPARATOR).map { it.trim('\n', '\r', ' ') }.filter { it.isNotBlank() }

/**
 * Blocco firma con delimitatore standard "-- ".
 *
 * 1.2.9 (multi-identità): [identity] arriva già RISOLTA per il server di
 * destinazione (Repository.identitaPerGruppo — quella assegnata al server, o
 * la default se il server non ne ha una propria). Prima della 1.2.9 c'era un
 * solo campo firma con un override per-server letto qui dentro; ora
 * un'identità è un oggetto intero, non serve più questa distinzione qui.
 *
 * Se sono definite più firme nello stesso campo (separate da "%%") e l'opzione
 * casuale è attiva, ne viene estratta una a sorte a ogni composizione.
 */
fun signatureBlock(identity: Identity): String {
    if (!identity.sigEnabled) return ""
    val all = splitMulti(identity.signature)
    if (all.isEmpty()) return ""
    val chosen = if (identity.sigRandom && all.size > 1) all.random() else all.first()
    // Segnaposto versione, sostituiti al momento della composizione (come per
    // %autore% ecc. in introLine): così la firma resta aggiornata da sola a
    // ogni release, senza doverla correggere a mano.
    //   %versione% → solo la versione, es. "1.0.0-beta14"
    //   %nereo%    → nome + versione, es. "NeReO 1.0.0-beta14"
    // 1.2.13: la sostituzione vive ora in Branding.espandiVersione, condivisa
    // con lo User-Agent — una sola definizione, così le due non divergono.
    val conVersione = org.openusenet.reader.Branding.espandiVersione(chosen)
    return "-- \n" + trimSignatureLines(conVersione.lines()).joinToString("\n")
}

/**
 * Riga di introduzione alla citazione, da modello personalizzabile.
 * Segnaposto: %autore%/%author% %email% %data%/%date% %ora%/%time%
 * %gruppo%/%group% %oggetto%/%subject% %messageid%
 * Se sono definiti più modelli (separati da "%%") ne viene scelto uno a sorte.
 *
 * RC01: [defaultIntro] permette a chi chiama di passare l'introduzione
 * predefinita GIÀ TRADOTTA (letta da R.string.cmp_default_intro nello scope
 * @Composable). Il valore di riserva resta la costante italiana
 * [DEFAULT_INTRO]: il parametro ha un default, così nessun chiamante
 * esistente va aggiornato per forza.
 */
fun introLine(
    identity: Identity,
    article: Article,
    groupName: String = "",
    defaultIntro: String = DEFAULT_INTRO,
    /**
     * 1.4 (richiesta di Henry The Mole) — l'introduzione impostata su QUESTO
     * gruppo, se c'e'. **SOSTITUISCE** quella dell'identita', non si aggiunge
     * al sorteggio.
     *
     * La ragione sta nella richiesta: lui vuole differenziare per gruppo,
     * tipicamente per LINGUA. Oggi piu' introduzioni danno solo il sorteggio —
     * cioe' l'esatto contrario: una scelta casuale dove serve una scelta
     * mirata. Un'introduzione italiana sorteggiata su un gruppo inglese e' il
     * difetto, non la funzione.
     *
     * null o vuota = si usa quella dell'identita', come sempre. Lo schema
     * "null = usa il globale" e' lo stesso gia' usato da `purgeDays` e
     * `purgeKeepThreads`: non si inventa una convenzione nuova.
     */
    introGruppo: String? = null,
): String {
    val sorgente = introGruppo?.takeIf { it.isNotBlank() } ?: identity.replyIntro
    val templates = splitMulti(sorgente).ifEmpty { listOf(defaultIntro) }
    val template = if (templates.size > 1) templates.random() else templates.first()
    val zoned = Instant.ofEpochSecond(article.dateEpoch).atZone(ZoneId.systemDefault())
    val email = Regex("<([^>]+)>").find(article.fromField)?.groupValues?.get(1) ?: article.fromField
    val name = article.fromField.substringBefore('<').trim().ifEmpty { email }
    val data = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(zoned)
    val ora = DateTimeFormatter.ofPattern("HH:mm").format(zoned)
    return template
        .replace("%autore%", name)
        // Gli alias inglesi convivono con quelli italiani per non rompere firme già salvate.
        .replace("%author%", name)
        .replace("%email%", email)
        .replace("%data%", data)
        .replace("%date%", data)
        .replace("%ora%", ora)
        .replace("%time%", ora)
        .replace("%gruppo%", groupName)
        .replace("%group%", groupName)
        // 1.4.5 (§B1 passo 1, richiesta di Ammammata): l'OGGETTO dell'articolo
        // citato. Era il quinto dei cinque dati che lui chiedeva di portarsi
        // dietro in una citazione, e l'unico che mancava — gruppo, data, ora e
        // autore c'erano gia' tutti.
        //
        // NORMALIZZATO, e non e' un dettaglio: senza, un'introduzione su una
        // discussione lunga diventerebbe «... ha scritto in "Re: Re: Re: Re:
        // oggetto"». `normalizeSubject` e' la stessa funzione che decide
        // l'appartenenza al thread, quindi l'oggetto citato e quello su cui il
        // programma ragiona sono lo stesso — non due idee simili.
        //
        // Migliora ANCHE le risposte normali, che e' la ragione per cui questo
        // passo viene prima degli altri cinque del pacchetto citazioni: non e'
        // codice speso solo per una funzione futura.
        .replace("%oggetto%", Threader.normalizeSubject(article.subject))
        .replace("%subject%", Threader.normalizeSubject(article.subject))
        .replace("%messageid%", article.messageId)
}

/**
 * Costruisce corpo + posizione cursore secondo la modalità di quoting.
 * La firma del citato viene sempre tagliata al "-- ".
 */
fun buildPrefill(
    identity: Identity,
    mode: String,
    p: Article?,
    groupName: String = "",
    defaultIntro: String = DEFAULT_INTRO,
    /** 1.4: introduzione del gruppo — vedi [introLine]. */
    introGruppo: String? = null,
    /**
     * 1.5.4 («Cita e rispondi»): la citazione GIA' PRONTA scelta dal lettore,
     * introduzione compresa (vedi `CitazioneInAttesa`). Se c'e', prende il
     * posto della citazione automatica, qualunque sia la modalita', e si
     * presenta come «sotto»: citazione, riga vuota, cursore, firma.
     */
    citazioneScelta: String? = null,
): TextFieldValue {
    val sig = signatureBlock(identity)
    if (p != null && !citazioneScelta.isNullOrBlank()) {
        val pre = citazioneScelta.trimEnd() + "\n\n"
        return TextFieldValue(pre + (if (sig.isEmpty()) "" else "\n$sig"), TextRange(pre.length))
    }
    if (p == null || mode == "none") {
        val text = if (sig.isEmpty()) "" else "\n\n$sig"
        return TextFieldValue(text, TextRange(0))
    }
    // beta19 (Roxy): "citazione ridotta" ora isola l'ULTIMO LIVELLO di citazione,
    // cioè il testo scritto dall'autore a cui sto rispondendo, scartando i livelli
    // più vecchi (le righe che nell'originale erano già citate con ">").
    // PRIMA tagliava le prime 6 righe a caso — spesso proprio la parte vecchia,
    // inutile: "lo scopo dovrebbe essere lasciare esattamente solo il testo a cui
    // si sta rispondendo direttamente, per rendere fluida la discussione" (Roxy).
    val corpoOriginale = (p.body ?: "")
        .lines()
        .takeWhile { it.trim() != "--" } // taglia la firma del citato
    val corpoDaCitare = if (mode == "trimmed") {
        // tengo solo le righe NON già citate nell'originale (testo nuovo
        // dell'autore). Se dopo il filtro non resta nulla (messaggio fatto di
        // sola citazione), ripiego sul corpo intero per non perdere il contesto.
        //
        // beta21 (Roxy): tolgo anche la riga di ATTRIBUZIONE del livello che sto
        // scartando. L'intro tipo «On 10/08/2026, Tizio wrote:» NON inizia con
        // ">", quindi sopravviveva al filtro e restava appiccicata alla mia
        // risposta pur riferendosi a una citazione che non c'è più: "c'è citata
        // anche la tua intro che in realtà non c'entra nulla e crea
        // fraintendimenti". Riconosco come attribuzione una riga che precede
        // (anche a distanza di righe vuote) un blocco citato e che termina con
        // ":" — è la forma usata da tutti i newsreader, in qualunque lingua.
        val righe = corpoOriginale.toMutableList()
        for (i in righe.indices) {
            val r = righe[i].trim()
            if (r.isEmpty() || r.startsWith(">")) continue
            // la riga introduce una citazione? guardo la prima riga non vuota che
            // segue: se è citata, questa è un'attribuzione da togliere.
            val seguente = righe.drop(i + 1).firstOrNull { it.isNotBlank() }
            if (seguente == null || !seguente.trimStart().startsWith(">")) continue
            // Riconosco l'attribuzione in modo tollerante: la forma canonica
            // finisce con ":" ma esistono varianti senza. Accetto quindi anche
            // righe brevi che contengono un marcatore tipico ("ha scritto",
            // "wrote", "scriveva", "said") — così funziona in ITA e ENG.
            // 1.5 / 159: la regola e' ESTRATTA in ui/Citazione.kt
            // (sembraAttribuzione), perche' «Cita» ne e' il terzo utente.
            // Prima stava qui in due copie con due elenchi identici.
            if (sembraAttribuzione(r)) righe[i] = ""
        }
        val soloNuove = righe.filter { !it.trimStart().startsWith(">") }
        var ripulite = soloNuove.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
        // ultima rete: se dopo la pulizia resta in coda un'attribuzione orfana
        // (introduceva un blocco citato che ora non c'è più) la tolgo.
        while (ripulite.isNotEmpty()) {
            if (!sembraAttribuzione(ripulite.last())) break
            ripulite = ripulite.dropLast(1).dropLastWhile { it.isBlank() }
        }
        if (ripulite.isEmpty()) corpoOriginale else ripulite
    } else corpoOriginale
    // 1.5 (passo 2 del pacchetto citazioni): ESTRATTA, non duplicata. Qui
    // c'era `corpoDaCitare.map { "> $it" }`, ed era l'unico posto dove si
    // metteva il prefisso; la schermata «Cita...» sarebbe stata il secondo.
    // Adesso il prefisso vive in `ui/Citazione.kt` e questo e' un chiamante.
    // Comportamento identico, verificato dal banco.
    val quoted = righeCitate(corpoDaCitare)
    val quoteBlock = introLine(identity, p, groupName, defaultIntro, introGruppo) + "\n" + quoted.joinToString("\n")

    return if (mode == "above") {
        val text = "\n\n$quoteBlock" + (if (sig.isEmpty()) "" else "\n\n$sig")
        TextFieldValue(text, TextRange(0))
    } else { // below | trimmed
        val pre = "$quoteBlock\n\n"
        val text = pre + (if (sig.isEmpty()) "" else "\n$sig")
        TextFieldValue(text, TextRange(pre.length))
    }
}
