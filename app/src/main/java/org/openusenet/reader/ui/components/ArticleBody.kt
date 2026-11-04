/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Corpo dell'articolo, reso a BLOCCHI (stile Piao Hong NewsReader):
 *  - i blocchi citati hanno una barra verticale colorata a sinistra, con
 *    colore e rientro secondo il livello di citazione;
 *  - opzione "fusione linee": collassa le righe vuote consecutive;
 *  - opzione "accorcia citazioni": i blocchi quote lunghi si comprimono
 *    (tocca per espandere/richiudere);
 *  - URL cliccabili che aprono il browser di sistema;
 *  - firma attenuata; font e dimensione dalle impostazioni.
 */
package org.openusenet.reader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
import org.openusenet.reader.text.Faccina
import org.openusenet.reader.text.LARGHEZZA_MIN_IMPAGINATA
import org.openusenet.reader.text.MARCA_ELENCO
import org.openusenet.reader.text.TABELLA_FACCINE
import org.openusenet.reader.text.interruzioneVoluta
import org.openusenet.reader.text.trovaFaccine

// Palette a gradini per i livelli di citazione: tinte nettamente diverse.
private val LIGHT_QUOTES = listOf(
    Color(0xFF1565C0), // blu (livello 1, come Piao Hong)
    Color(0xFF1B7A3D), // verde
    Color(0xFFB25E00), // arancio
    // 1.6: viola (prima #7B1FA2, troppo vicino al colore autore «porpora»
    // #8E24AA: ΔE 6). 7,3:1 sul bianco, ΔE ≥ 18 da tutti gli altri viola.
    Color(0xFF6A3D9A),
    Color(0xFF00757A), // teal
)
private val DARK_QUOTES = listOf(
    Color(0xFF7FB4F5),
    Color(0xFF6FD68C),
    Color(0xFFE0A85A),
    Color(0xFFC79AE0),
    Color(0xFF5FCBD1),
)

private val URL_REGEX = Regex("""(?:https?://|www\.)[^\s<>()\[\]"']+""", RegexOption.IGNORE_CASE)

/** Righe consecutive con lo stesso ruolo (stesso livello di quote, o firma). */
private data class BodyBlock(val depth: Int, val signature: Boolean, val lines: List<String>)

/**
 * Riflusso (de-wrap) delle righe di UN blocco (stesso livello di citazione, mai
 * la firma). L'a-capo forzato mostrato su schermo stretto lascia righe con
 * una-due parole orfane: qui ricompongo i capoversi.
 *
 * Euristica ADATTIVA (v2): il problema della v1 era una soglia FISSA a 60 —
 * chi impagina più stretto (55, 50…) non veniva riconosciuto come "andato a
 * capo" e il riflusso non faceva nulla. Ora stimo la larghezza di impaginazione
 * di QUESTO blocco (W = riga più lunga, con un tetto) e considero una riga
 * "piena" (quindi andata a capo per il margine) quando la PRIMA PAROLA della
 * riga successiva non ci sarebbe stata: `lunghezza_prec + 1 + prima_parola > W`.
 * È il modello reale dell'a-capo automatico, e si adatta a ogni autore.
 *
 * Salvaguardie: se il blocco non ha righe abbastanza lunghe (W < [LARGHEZZA_MIN_IMPAGINATA])
 * NON è testo impaginato (elenchi, versi, messaggi brevi) → lascio intatto. Le
 * righe che iniziano con un marcatore di elenco non vengono mai unite. Le righe
 * vuote separano i capoversi. Il prefisso di citazione viene tolto e rimesso
 * uniforme, così il render (barre/chevron) funziona come prima.
 *
 * 1.2.3 (Bingo3331, Dave Royal): soglia abbassata 40 → 30. La v2 a 40 si
 * arrendeva sull'INTERO blocco quando l'autore impaginava stretto (~38-40 col):
 * la riga più lunga era ~39 < 40, quindi il riflusso non univa NIENTE — proprio
 * gli autori che ne hanno più bisogno restavano fuori ("continua a non
 * funzionare"). Scendere è sicuro perché è il test di unione stesso a proteggere
 * i versi: una riga corta per scelta non raggiunge il margine, quindi la prima
 * parola della successiva "ci sta" e le due NON vengono unite. Sotto ~30, invece,
 * distinguere prosa impaginata da testo-a-righe-corte diventa inaffidabile:
 * lì restiamo prudenti e lasciamo intatto.
 *
 * 1.2.4 (Bingo3331): il solo test "riga precedente piena" non bastava. Un testo
 * ri-impaginato lascia righe ORFANE di una-due parole IN MEZZO al capoverso (es.
 * "è", "sono", "nelle", "delle altre"): unita l'orfana, `prevLen` scendeva al suo
 * valore minuscolo e la riga dopo veniva letta come "interruzione voluta" → il
 * capoverso si spezzava e l'orfana ricompariva al render. Ora si UNISCE sempre,
 * tranne quando la riga precedente — oltre a non essere piena — finisce con
 * punteggiatura terminale (. , : ; ! ? …): questo protegge etichette ("Bar:"),
 * saluti ("Ciao,") e frasi brevi, che restano su riga propria, mentre le orfane
 * (che finiscono con una parola, senza punteggiatura) vengono ricomposte.
 * Limite noto: un congedo su riga propria SENZA riga vuota prima (es. "Saluti"
 * subito sotto un capoverso) può attaccarsi al capoverso. Caso minore e raro (i
 * congedi hanno di norma una riga vuota o la firma "-- "); reflow è comunque
 * opt-in e solo in lettura.
 */
private const val WRAP_MAX_W = 90 // tetto: evita che un URL lunghissimo falsi la stima

// 1.2.4: una fine riga è "voluta" (da NON unire) solo quando la riga precedente,
// oltre a essere corta, termina con punteggiatura terminale. Serve a proteggere
// etichette ("Bar:"), saluti ("Ciao,") e frasi brevi, senza però lasciare orfane
// le righe di una-due parole in mezzo a un capoverso.
// 1.6: la regola, l'elenco e la soglia minima sono SPOSTATI in text/WrapBody.kt,
// perche' li usa anche «Riformatta» in scrittura.

private fun reflowLines(lines: List<String>, depth: Int): List<String> {
    val prefix = if (depth > 0) ">".repeat(depth) + " " else ""
    val bare = lines.map { if (depth > 0) it.dropWhile { c -> c == '>' || c == ' ' } else it }
    // larghezza di impaginazione stimata per questo blocco
    val w = (bare.filter { it.isNotBlank() }.maxOfOrNull { it.trimEnd().length } ?: 0)
        .coerceAtMost(WRAP_MAX_W)
    if (w < LARGHEZZA_MIN_IMPAGINATA) return lines // niente wrapping reale: lascio intatto (elenchi, versi, messaggi brevi)

    val out = ArrayList<String>()
    val para = StringBuilder()
    var precedente = ""
    fun flush() {
        if (para.isNotEmpty()) { out.add(prefix + para.toString()); para.setLength(0) }
        precedente = ""
    }
    for ((i, content) in bare.withIndex()) {
        if (content.isBlank()) {
            flush()
            out.add(lines[i]) // riga vuota: separa i capoversi, mantenuta com'è
            continue
        }
        val trimmed = content.trim()
        // 1.2.4: interruzione VOLUTA solo se la riga precedente NON era piena E
        // finiva con punteggiatura terminale. Prima si spezzava su OGNI riga
        // precedente non piena: bastava un'orfana cortissima ("è", "sono",
        // "nelle") a spezzare il capoverso, così l'orfana ricompariva al render.
        // Ora un'orfana (che non finisce con punteggiatura) viene unita; a
        // restare su riga propria sono solo etichette/saluti/frasi brevi che
        // chiudono con . , : ; ! ? …
        val voluta = interruzioneVoluta(precedente, trimmed, w)
        when {
            para.isEmpty() -> para.append(trimmed)
            MARCA_ELENCO.containsMatchIn(trimmed) -> { flush(); para.append(trimmed) } // elenco: mai unire
            voluta -> { flush(); para.append(trimmed) }                                 // interruzione voluta
            else -> para.append(' ').append(trimmed)                                    // continuazione del capoverso
        }
        precedente = content
    }
    flush()
    return out
}

private fun splitBlocks(body: String, collapseBlank: Boolean): List<BodyBlock> {
    val out = ArrayList<BodyBlock>()
    var cur = ArrayList<String>()
    var curDepth = -1
    var curSig = false
    var inSignature = false
    var lastBlank = false

    fun flush() {
        if (cur.isNotEmpty()) { out.add(BodyBlock(curDepth, curSig, cur)); cur = ArrayList() }
    }

    for (raw in body.lines()) {
        if (!inSignature && raw.trimEnd() == "--") inSignature = true
        val blank = raw.isBlank()
        if (collapseBlank && blank && lastBlank) continue // fusione linee intelligente
        lastBlank = blank
        val depth = if (inSignature) 0 else raw.takeWhile { it == '>' || it == ' ' }.count { it == '>' }
        if (depth != curDepth || inSignature != curSig) {
            flush(); curDepth = depth; curSig = inSignature
        }
        cur.add(raw)
    }
    flush()
    return out
}

/** Coppia (sfondo, testo) di lettura secondo l'impostazione readingBg, con fallback al tema. */
@Composable
fun readingColors(): Pair<Color, Color> {
    val pair = AppSettings.readingBackgrounds[AppSettings.readingBg]
    val bg = pair?.let { Color(it.first) } ?: MaterialTheme.colorScheme.surface
    val fg = pair?.let { Color(it.second) } ?: MaterialTheme.colorScheme.onSurface
    return bg to fg
}

@Composable
fun QuoteColoredBody(
    body: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surface,
    baseTextColor: Color = MaterialTheme.colorScheme.onSurface,
    searchQuery: String = "",  // beta13: evidenziazione termine di ricerca nel corpo (livello 2)
    /**
     * 1.4.6 (par. B6a): come il MITTENTE dichiara di aver impaginato il corpo.
     * Il valore predefinito — niente dichiarazione — lascia tutto esattamente
     * come prima, euristica di riflusso compresa.
     */
    formato: org.openusenet.reader.text.FormatoCorpo =
        org.openusenet.reader.text.FormatoCorpo(false, false),
) {
    val dark = background.luminance() < 0.5f
    val quoteColors = if (dark) DARK_QUOTES else LIGHT_QUOTES
    val signatureColor = baseTextColor.copy(alpha = 0.6f)
    val linkColor = if (dark) Color(0xFF8AB4F8) else Color(0xFF0B57D0)
    val highlightBg = Color(0x66FFEB3B)  // giallo trasparente, come nella lista risultati

    // la dimensione arriva dal tema (scalata a livello di app): qui solo il font
    val style = MaterialTheme.typography.bodyMedium.copy(
        fontFamily = if (AppSettings.bodyMonospace) FontFamily.Monospace else FontFamily.Default,
    )

    /*
     * 1.4.6 (par. B6a): se il mittente ha dichiarato `format=flowed`, i
     * capoversi non si indovinano — li ha marcati lui, riga per riga. Qui si
     * ricostruisce il testo logico PRIMA di dividerlo in blocchi; l'euristica
     * qui sotto viene saltata, perche' stimare sopra un dato esatto non lo
     * migliora: lo puo' solo rovinare.
     */
    /*
     * 1.4.6 (par. B6a-bis): «dichiarato» non basta, deve essere anche USATO.
     * MesNews dichiara format=flowed e poi impagina con a-capo rigidi: nessuna
     * riga continua, quindi il formato non dice niente che non sapessimo. In
     * quel caso si torna all'euristica invece di restare senza nulla.
     */
    val flowed = remember(body, formato) {
        org.openusenet.reader.text.usaFlowed(formato, body.lines())
    }
    val testo = remember(body, flowed, formato) {
        if (flowed)
            org.openusenet.reader.text.srotolaFlowed(body.lines(), formato.delSp)
                .joinToString("\n")
        else body
    }
    val blocks = remember(testo, flowed, AppSettings.collapseBlankLines, AppSettings.reflowText) {
        splitBlocks(testo, AppSettings.collapseBlankLines).map { b ->
            when {
                // firma: si mostrano il delimitatore e al massimo 4 righe da 72 colonne
                b.signature && b.lines.size > 1 -> b.copy(
                    lines = listOf(b.lines.first()) +
                        org.openusenet.reader.ui.screens.trimSignatureLines(b.lines.drop(1)),
                )
                // riflusso: ricompone i capoversi (mai la firma). Solo in lettura.
                !flowed && AppSettings.reflowText && !b.signature ->
                    b.copy(lines = reflowLines(b.lines, b.depth))
                else -> b
            }
        }
    }

    // 1.5.9 / 177: faccine come emoji, solo a schermo (vedi appendConFaccine)
    val conFaccine = AppSettings.faccineEmoji
    val faccine = inlineFaccine(style, AppSettings.bodyMonospace)

    Column(modifier.fillMaxWidth()) {
        blocks.forEachIndexed { i, block ->
            val color = when {
                block.signature -> signatureColor
                block.depth == 0 -> baseTextColor
                else -> quoteColors[(block.depth - 1) % quoteColors.size]
            }
            if (block.depth == 0) {
                Text(
                    annotate(block.lines, color, linkColor, searchQuery, highlightBg, conFaccine),
                    style = style,
                    inlineContent = faccine,
                )
            } else {
                QuotedBlock(block, i, color, linkColor, style, searchQuery, highlightBg, conFaccine, faccine)
            }
        }
    }
}

/** Blocco citato: barra verticale colorata + rientro per livello + compressione opzionale. */
@Composable
private fun QuotedBlock(
    block: BodyBlock,
    index: Int,
    color: Color,
    linkColor: Color,
    style: TextStyle,
    // beta16 (bug segnalato da Marco): l'evidenziazione ricerca non arrivava al
    // testo CITATO. Non era un conflitto di colori: era che QuotedBlock non
    // riceveva affatto searchQuery/highlightBg, quindi annotate cadeva sul ramo
    // "niente da evidenziare". Ora vengono propagati come nel testo normale.
    searchQuery: String = "",
    highlightBg: Color = Color.Unspecified,
    conFaccine: Boolean = false,
    faccine: Map<String, InlineTextContent> = emptyMap(),
) {
    var expanded by remember(index, block.lines.size) { mutableStateOf(false) }
    // beta19 (Roxy): soglia configurabile (prima fissa a 6 → la funzione
    // "sembrava non fare nulla" perché molte citazioni sono più corte).
    val soglia = AppSettings.sogliaAccorciaCitazioni.coerceAtLeast(3)
    val long = block.lines.size > soglia
    // 1.2.6 (Dave Royal): "muta citazioni" — ogni blocco citato collassa a un
    // segnaposto finché non lo si tocca. Ha priorità sull'"accorcia".
    val muted = AppSettings.mutaCitazioni && !expanded
    val shorten = AppSettings.shortenQuotes && long && !expanded && !muted
    val shownRaw = if (shorten) block.lines.take((soglia - 2).coerceAtLeast(2)) else block.lines

    // beta14 (Dave Royal): stile citazioni scelto dall'utente.
    //  bars    → barra verticale colorata, ">" tolti dal testo (default)
    //  chevron → niente barra, ">" originali mantenuti (Usenet classico)
    //  both    → barra + ">" (comportamento storico)
    val stile = AppSettings.quoteStyle
    val mostraBarra = stile == "bars" || stile == "both"
    val togliChevron = stile == "bars"
    // 1.2.9 (Marco): oltre soglia, i ">" ripetuti (stile chevron/both) diventano
    // illeggibili — soprattutto in verticale, dove lo spazio è già poco. Il
    // NUMERO del livello (es. "9") li sostituisce SOLO nella resa a schermo:
    // `block.depth` viene dal conteggio dei ">" originali in splitBlocks, che
    // qui non cambia — è solo un modo diverso di MOSTRARE lo stesso dato. Come
    // per "bars", il prefisso originale (">"/spazi, in qualunque forma l'abbia
    // scritto l'autore: ">>>>" o "> > > >") viene tolto e ricostruito uniforme.
    val usaNumeri = stile != "bars" &&
        AppSettings.numericQuoteDepth &&
        block.depth > AppSettings.sogliaCitazioniANumeri.coerceAtLeast(2)
    val shown = when {
        togliChevron -> shownRaw.map { it.dropWhile { c -> c == '>' || c == ' ' } }
        usaNumeri -> shownRaw.map { "${block.depth} " + it.dropWhile { c -> c == '>' || c == ' ' } }
        else -> shownRaw
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(start = ((block.depth - 1).coerceAtMost(6) * 8).dp, top = 2.dp, bottom = 2.dp)
            .then(
                if ((AppSettings.shortenQuotes && long) || AppSettings.mutaCitazioni)
                    Modifier.clickable { expanded = !expanded }
                else Modifier
            ),
    ) {
        if (mostraBarra) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(color),
            )
        }
        Column(Modifier.padding(start = if (mostraBarra) 8.dp else 0.dp)) {
            if (muted) {
                // segnaposto: una riga sola, tocca per riespandere questo blocco.
                Text(
                    stringResource(R.string.body_quote_muted),
                    style = style.copy(fontSize = style.fontSize * 0.85f),
                    color = color.copy(alpha = 0.6f),
                )
            } else {
                Text(
                    annotate(shown, color, linkColor, searchQuery, highlightBg, conFaccine),
                    style = style,
                    inlineContent = faccine,
                )
                if (shorten) {
                    Text(
                        stringResource(R.string.body_more_lines, block.lines.size - shown.size),
                        style = style.copy(fontSize = style.fontSize * 0.8f),
                        color = color.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

private fun annotate(
    lines: List<String>,
    textColor: Color,
    linkColor: Color,
    searchQuery: String = "",
    highlightBg: Color = Color.Unspecified,
    conFaccine: Boolean = false,
): AnnotatedString =
    buildAnnotatedString {
        lines.forEachIndexed { i, line ->
            appendLineWithLinks(line, textColor, linkColor, searchQuery, highlightBg, conFaccine)
            if (i < lines.size - 1) append('\n')
        }
    }

/** Aggiunge testo colorato, evidenziando le occorrenze di [q] con [highlightBg] se impostato. */
private fun AnnotatedString.Builder.appendHighlighted(
    text: String, textColor: Color, q: String, highlightBg: Color,
) {
    if (q.isBlank() || highlightBg == Color.Unspecified) {
        withStyle(SpanStyle(color = textColor)) { append(text) }
        return
    }
    val lower = text.lowercase(); val lq = q.lowercase()
    var i = 0
    while (i < text.length) {
        val hit = lower.indexOf(lq, i)
        if (hit < 0) { withStyle(SpanStyle(color = textColor)) { append(text.substring(i)) }; break }
        if (hit > i) withStyle(SpanStyle(color = textColor)) { append(text.substring(i, hit)) }
        withStyle(SpanStyle(color = textColor, background = highlightBg)) {
            append(text.substring(hit, hit + q.length))
        }
        i = hit + q.length
    }
}

/*
 * 1.5.9 / 177 (Marco, piano A1) — FACCINE A CARATTERI MOSTRATE COME EMOJI.
 *
 * Dove sono le faccine lo dice text/Faccine.kt (Kotlin puro, con le prove).
 * Qui le si DISEGNA, e il punto e' come: nel testo restano i caratteri
 * originali (`8-o`) come `alternateText` di un `appendInlineContent`, e sopra
 * il loro posto si disegna l'emoji. Il testo non cambia, quindi:
 *  - «Copia» e «Cita» (CitaSelezione.kt) prendono `8-o`, e il confronto con
 *    l'archivio di Citazione.kt funziona senza saperne niente;
 *  - le posizioni dei caratteri non si spostano: link ed evidenziazione
 *    della ricerca lavorano come prima;
 *  - TalkBack legge la sequenza originale.
 * Le faccine si cercano DOPO gli URL e fuori da essi: una sequenza dentro un
 * link resta link. La sostituzione nel testo (piano A2) e' stata scartata
 * perche' «Copia» della selezione avrebbe dato le emoji, e la copia del menu
 * di sistema non si puo' intercettare.
 */
private const val ID_FACCINA = "faccina:"

/**
 * Lo spazio di ogni faccina, una voce per sequenza della tabella. Col
 * carattere monospaziato lo spazio e' largo quanto i caratteri originali
 * (~0,6 em l'uno): le colonne di ASCII art e tabelle non si spostano.
 *
 * GRANDEZZA DELL'EMOJI, tre giri di collaudo di Marco:
 *  - 177: emoji grande come il testo dentro un segnaposto alto 1 em, ma il
 *    carattere emoji e' alto circa 1,2 em e il Text la tagliava sopra e sotto;
 *  - 178: emoji a 0,8 (piano B): intera, ma piu' piccola del testo e con
 *    vuoti larghi ai lati (segnaposto 1,2 em). MesNews la mostra grande e
 *    stretta fra le virgolette;
 *  - 179 (piano A): emoji di nuovo grande come il testo, misurata SENZA
 *    limiti (`wrapContentSize(unbounded = true)`) e centrata: esce dal
 *    segnaposto invece di essere tagliata. Segnaposto largo 1,0 em, alto 1
 *    em come sempre: l'interlinea non cambia, e il disegno (~1 em) sta
 *    dentro la riga (1,2 em). Il monospaziato tiene le sue larghezze.
 */
@Composable
private fun inlineFaccine(style: TextStyle, monospace: Boolean): Map<String, InlineTextContent> =
    remember(style, monospace) {
        val stileEmoji = style.copy(lineHeight = TextUnit.Unspecified)
        TABELLA_FACCINE.entries.associate { (seq, emoji) ->
            val larghezza = if (monospace) (seq.length * 0.6f).coerceAtLeast(1.2f) else 1.0f
            ID_FACCINA + seq to InlineTextContent(
                Placeholder(larghezza.em, 1.em, PlaceholderVerticalAlign.TextCenter),
            ) {
                // DisableSelection: l'emoji disegnata NON e' testo selezionabile,
                // altrimenti «Copia» prenderebbe lei insieme all'originale.
                DisableSelection {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text(
                            emoji, style = stileEmoji,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Visible,
                            modifier = Modifier.wrapContentSize(unbounded = true),
                        )
                    }
                }
            }
        }
    }

/**
 * Aggiunge il tratto [inizio, fine) di [line] (mai dentro un URL), mettendo al
 * posto delle [faccine] che vi cadono l'emoji disegnata sopra l'originale.
 */
private fun AnnotatedString.Builder.appendConFaccine(
    line: String, inizio: Int, fine: Int, faccine: List<Faccina>,
    textColor: Color, searchQuery: String, highlightBg: Color,
) {
    var i = inizio
    for (f in faccine) {
        if (f.inizio < inizio || f.fine > fine) continue
        if (f.inizio > i) appendHighlighted(line.substring(i, f.inizio), textColor, searchQuery, highlightBg)
        appendInlineContent(ID_FACCINA + f.sequenza, f.sequenza)
        i = f.fine
    }
    if (i < fine) appendHighlighted(line.substring(i, fine), textColor, searchQuery, highlightBg)
}

/** Aggiunge una riga, trasformando gli URL in link cliccabili (aprono il browser). */
private fun AnnotatedString.Builder.appendLineWithLinks(
    line: String, textColor: Color, linkColor: Color,
    searchQuery: String = "", highlightBg: Color = Color.Unspecified,
    conFaccine: Boolean = false,
) {
    val url = URL_REGEX.findAll(line).toList()
    val faccine = if (conFaccine) trovaFaccine(line, url.map { it.range }) else emptyList()
    var last = 0
    for (m in url) {
        if (m.range.first > last) {
            appendConFaccine(line, last, m.range.first, faccine, textColor, searchQuery, highlightBg)
        }
        val raw = m.value
        val url = raw.trimEnd('.', ',', ';', ':', '!', '?', ')', ']')
        val href = if (url.startsWith("www.", ignoreCase = true)) "https://$url" else url
        withLink(
            LinkAnnotation.Url(
                href,
                TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
            ),
        ) { append(url) }
        if (url.length < raw.length) {
            withStyle(SpanStyle(color = textColor)) { append(raw.substring(url.length)) }
        }
        last = m.range.last + 1
    }
    if (last < line.length) {
        appendConFaccine(line, last, line.length, faccine, textColor, searchQuery, highlightBg)
    }
}
