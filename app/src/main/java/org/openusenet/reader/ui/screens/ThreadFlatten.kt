/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Costruzione della lista messaggi secondo la vista scelta:
 *  - tree       : albero indentato (JWZ), seguite con non letti in cima
 *  - sequential : tutti i messaggi in ordine di data, senza rientro
 *  - roots      : una riga per discussione, richiudibile con +/- (beta7),
 *                 con contatore dei messaggi da leggere
 * In tutte, il flag threadStart marca la prima riga di ogni discussione, per
 * disegnare una separazione grafica tra un thread e l'altro.
 * Condiviso tra ThreadListScreen (telefono) e il layout a tre pannelli.
 */
package org.openusenet.reader.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import org.openusenet.reader.AppSettings
import org.openusenet.reader.data.Article
import org.openusenet.reader.nntp.MimeUtil
import org.openusenet.reader.thread.ThreadNode
import org.openusenet.reader.thread.ThreadableArticle
import org.openusenet.reader.thread.Threader

object ThreadViews {
    const val TREE = "tree"
    const val SEQUENTIAL = "sequential"
    const val ROOTS = "roots"
}

/**
 * Data compatta per la lista discussioni: ora se di oggi (14:32),
 * giorno/mese se dell'anno corrente (12/07), completa altrimenti (03/11/24).
 */
/**
 * beta13 (Marco): variante di [formatThreadDate] che mostra SEMPRE data E
 * ora insieme, non solo l'una o l'altra. Usata nell'albero del layout a tre
 * pannelli, dove Marco ha chiesto ora+data al posto dell'indirizzo e-mail.
 * L'anno si mostra solo se diverso dall'anno corrente, per non allungare
 * troppo la riga.
 */

/**
 * beta18 (Bingo3331): colore "risposta a un mio post". Teal medio, leggibile su
 * sfondo chiaro e scuro, distinto da viola (brand) e giallo (ricerca). Condiviso
 * fra ThreePaneHome e ThreadListScreen (barretta laterale + contatore).
 */
val RispostaAMeColor = Color(0xFF1FA9A0)

/**
 * beta20: guardia per il sync all'apertura di un gruppo.
 *
 * Fino a beta18 aprire un gruppo ne scaricava sempre le novita' (comportamento
 * base di qualunque newsreader). In beta19 l'ho messo dietro
 * `AppSettings.syncOnStart` per risolvere un aggiornamento indesiderato quando
 * si rientra nell'elenco discussioni: ERRORE, perche' quell'opzione significa
 * "all'AVVIO DELL'APP sincronizza TUTTI i gruppi" — tutt'altra cosa. Risultato:
 * aprendo un gruppo non scaricava piu' le intestazioni (segnalato da Roxy).
 *
 * Qui il problema originale si risolve dove sta davvero: non impedendo il sync,
 * ma evitando di RIPETERLO se lo stesso gruppo e' stato sincronizzato pochi
 * istanti prima (tipico rientro dalla lettura di una discussione).
 */
object SyncGuard {
    private val ultimoSync = mutableMapOf<Long, Long>()
    private const val FINESTRA_MS = 60_000L

    /** true se il gruppo va sincronizzato ora; registra il momento. */
    fun deveSincronizzare(groupId: Long): Boolean {
        val ora = System.currentTimeMillis()
        if (ora - (ultimoSync[groupId] ?: 0L) < FINESTRA_MS) return false
        ultimoSync[groupId] = ora
        return true
    }

    /** azzera la guardia: il prossimo accesso sincronizza di sicuro. */
    fun invalida(groupId: Long) { ultimoSync.remove(groupId) }

    /**
     * beta20 (il Merovingio): azzera TUTTA la guardia. Va chiamata all'avvio
     * dell'app.
     *
     * Perché serve: questo è un `object` Kotlin, cioè un singleton legato al
     * PROCESSO, non all'Activity. Chiudendo l'app con "Chiudi applicazione"
     * (`finishAndRemoveTask`) l'Activity sparisce ma il processo può restare
     * vivo: alla riapertura i tempi registrati erano ancora qui, e se erano
     * passati meno di 60 secondi la guardia bloccava il download dei nuovi
     * articoli — esattamente il sintomo segnalato ("riapro e non scarica più
     * in automatico, benché la funzione sia attiva").
     *
     * La guardia deve valere solo DENTRO una sessione di lettura (per non
     * ri-sincronizzare rientrando nell'elenco discussioni), mai fra un avvio
     * e l'altro.
     */
    fun azzeraTutto() { ultimoSync.clear() }
}

fun formatThreadDateTime(epochSeconds: Long): String {
    // beta14 (Marco): anno SEMPRE visibile a 2 cifre nell'elenco discussioni,
    // anche per l'anno corrente (prima veniva omesso), così ogni riga ha la
    // data completa e compatta.
    // 1.6.2 / 189: le forme stanno in text/DataOra, con le scelte di Aspetto.
    val p = org.openusenet.reader.AppSettings.preferenzeDataOra()
    val dt = org.openusenet.reader.text.DataOra.inZona(epochSeconds, p)
    return org.openusenet.reader.text.DataOra.dataOraBreve(dt, p)
}

/** Nome dell'autore senza l'indirizzo e-mail (es. "Mario Rossi <m@x.it>" → "Mario Rossi"). */
fun authorDisplayName(fromField: String): String {
    val nome = fromField.substringBefore('<').trim()
    return nome.ifEmpty { fromField.substringAfter('<').substringBefore('>').trim().ifEmpty { fromField } }
}

/**
 * beta13 (Bingo3331): evidenzia tutte le occorrenze (case-insensitive) di
 * [query] dentro [text] con uno sfondo giallo trasparente, restituendo un
 * AnnotatedString pronto da passare a Text(). Se [query] è vuota o non
 * compare, restituisce il testo semplice senza evidenziazioni. Condivisa fra
 * lista risultati (SearchScreen) e corpo articolo (ArticleScreen).
 */
fun highlightMatches(
    text: String,
    query: String,
    highlightBg: Color,
): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var i = 0
        val lowerText = text.lowercase()
        val lowerQ = q.lowercase()
        while (i < text.length) {
            val hit = lowerText.indexOf(lowerQ, i)
            if (hit < 0) {
                append(text.substring(i))
                break
            }
            append(text.substring(i, hit))
            withStyle(SpanStyle(background = highlightBg)) {
                append(text.substring(hit, hit + q.length))
            }
            i = hit + q.length
        }
    }
}

/**
 * beta14 (Big Umberto): dato un ThreadItem, dice se va mostrato come "non
 * letto". Per una RADICE della VISTA RAGGRUPPATA guarda l'intera discussione
 * (`unread > 0`): così una discussione con nuove risposte torna non-letta anche
 * se il primo post era già letto. Per una riga interna (o vista non
 * raggruppata) vale lo stato del singolo messaggio.
 *
 * 1.2.3 (Roxy): il test `isRoot` da solo era sbagliato per la VISTA AD ALBERO.
 * Lì la riga di testa ha `isRoot=true` (per mostrare omino e contatore), ma è
 * solo il PRIMO messaggio del thread mostrato per conto suo — le risposte sono
 * righe separate sotto. `treeList` non popola `unread` (resta 0), così ogni
 * capo-thread risultava SEMPRE letto (verde, non grassetto) a prescindere dal
 * suo stato reale, mentre "vai al prossimo non letto" (che guarda `article.read`)
 * lo considerava non letto: spia visiva e navigazione in disaccordo.
 * Il campo `total` è >0 SOLO nella vista raggruppata (dove `unread` è valorizzato
 * davvero); nella vista ad albero resta 0. Usarlo come discriminante fa sì che il
 * capo-thread dell'albero torni a colorarsi in base al SUO stato, come ogni altra
 * riga, senza toccare la vista raggruppata.
 */
/**
 * 1.4.1 (105/E, Dave Royal) — e il perche' della condizione su [ThreadItem.expanded].
 *
 * Per una RADICE lo stato mostrato e' quello dell'intera discussione: una
 * discussione con risposte nuove deve tornare "non letta" anche se la radice
 * era gia' stata letta (beta14, BIG Umberto). Giusto finche' la discussione e'
 * CHIUSA, perche' quella riga e' l'unica cosa visibile e deve parlare per
 * tutti.
 *
 * Aperta, no: le risposte non lette si vedono da sole, riga per riga, e la
 * testa che continua a dirsi "non letta" sta dicendo una cosa falsa su se'
 * stessa. Dave: "it shows 1/3 unread, but *2* posts are red".
 */
fun righeNonLette(item: ThreadItem, article: Article): Boolean =
    if (item.isRoot && item.total > 0 && !item.expanded) item.unread > 0 else !article.read

/**
 * 1.4.1 (105/F, Dave Royal) — il testo dell'esito di un aggiornamento.
 *
 * "Sometimes it says it has found new messages but no group has any shown":
 * il numero del sync conta gli articoli INSERITI, la lista e il pallino del
 * gruppo contano i VISIBILI e non letti. Con un filtro attivo divergono, e
 * annunciare "3 nuovi" quando non ne comparira' nessuno e' un'affermazione
 * falsa, non un'approssimazione. La formula e' sua: "Found no new posts.
 * (3 filtered)" — vera, e per giunta dice che il filtro sta lavorando, cosa
 * che prima non si vedeva da nessuna parte.
 *
 * Sta qui, e non nei quattro punti che mostrano quel messaggio, per la regola
 * di sempre: prima di scrivere la quarta copia di una logica, estrarla.
 */
fun testoEsitoSync(
    ctx: android.content.Context,
    inseriti: Int,
    filtrati: Int,
    nonAggiornati: List<org.openusenet.reader.data.ServerNonAggiornato> = emptyList(),
): String {
    val visibili = (inseriti - filtrati).coerceAtLeast(0)
    val esito = when {
        filtrati <= 0 && visibili <= 0 -> ctx.getString(org.openusenet.reader.R.string.msg_no_new)
        filtrati <= 0 -> ctx.getString(org.openusenet.reader.R.string.msg_new_articles, visibili)
        visibili <= 0 -> ctx.getString(org.openusenet.reader.R.string.msg_no_new_filtered, filtrati)
        else -> ctx.getString(
            org.openusenet.reader.R.string.msg_new_articles_filtered, visibili, filtrati
        )
    }
    if (nonAggiornati.isEmpty()) return esito
    // 1.6.1: «Nessun nuovo articolo. Non aggiornato: robertocorrado (non risponde)»
    val elenco = org.openusenet.reader.data.elencoNonAggiornati(
        nonAggiornati, ctx.getString(org.openusenet.reader.R.string.msg_sync_no_answer),
    )
    return esito.trimEnd('.') + ". " +
        ctx.getString(org.openusenet.reader.R.string.msg_sync_not_updated, elenco)
}

/** Oggi solo l'ora, quest'anno senza anno, prima la data breve. 1.6.2: vedi text/DataOra.elenco. */
fun formatThreadDate(epochSeconds: Long): String =
    org.openusenet.reader.text.DataOra.elenco(epochSeconds, org.openusenet.reader.AppSettings.preferenzeDataOra())

/**
 * Wishlist beta12 (Dave Royal, tablet 10" in verticale — colonna gruppi
 * stretta): abbrevia un nome gruppo tenendo per intero solo l'ultimo
 * segmento, gli altri ridotti alla sola iniziale. Es.:
 * "it.tlc.cellulari.android" → "i.t.c.android". Usata come RIPIEGO quando
 * il nome intero (fino a 2 righe) va comunque in overflow — non al posto
 * del troncamento, ma per evitarlo del tutto quando possibile.
 */
fun abbreviateGroupName(name: String): String {
    val parts = name.split('.')
    if (parts.size <= 1) return name
    return parts.dropLast(1).joinToString(".") { it.take(1) } + "." + parts.last()
}

data class ThreadItem(
    val depth: Int,
    val article: Article,
    val threadStart: Boolean = false,
    val replies: Int = 0,
    /** messageId della radice: identifica la discussione di appartenenza. */
    val rootKey: String = "",
    /** true se questa riga rappresenta l'intera discussione (vista raggruppata). */
    val isRoot: Boolean = false,
    /** messaggi da leggere nella discussione (solo sulle righe radice). */
    val unread: Int = 0,
    /** messaggi totali nella discussione (solo sulle righe radice). */
    val total: Int = 0,
    /** stato del +/- (solo sulle righe radice). */
    val expanded: Boolean = false,
    /** beta14 (Big Umberto): true se nella discussione c'è almeno un mio post. */
    val hasMyPost: Boolean = false,
    /**
     * beta18 (Bingo3331): quante risposte dirette ai miei post nella discussione.
     * Solo sulle righe radice (contatore accanto all'omino, Piano B). Contato
     * leggendo il campo ESISTENTE `Article.replyToMe` (calcolato al sync), NON
     * ricalcolato qui: unica fonte di verità.
     */
    val repliesToMe: Int = 0,
    /**
     * 1.6.3 / 190 (BIG Umberto): data del messaggio piu' recente della
     * discussione. Solo sulle righe radice della vista raggruppata, 0 altrove.
     */
    val ultimaData: Long = 0L,
)

/**
 * 1.6.3 / 190 (BIG Umberto, 12/11) — QUALE DATA MOSTRA LA RIGA.
 *
 * Ordinando per messaggio piu' recente, la riga di una discussione intera
 * mostrava la data del PRIMO messaggio: «Riscaldamento 10/11 21:14» sopra
 * «Inaugurazione 11/11 21:05» sembrava un errore d'ordine. Ora, in quel caso,
 * la riga mostra la data che decide l'ordine, e lo dice («ultimo»), perche'
 * l'autore resta quello che ha aperto la discussione.
 *
 * Solo sulle righe che rappresentano l'intera discussione (vista raggruppata,
 * `total > 1`); nell'albero ogni riga e' un messaggio e la sua data e' la sua.
 * Per apertura la data che decide e' gia' quella mostrata. Restituisce la data
 * e se e' quella dell'ultimo messaggio.
 */
fun dataDellaRiga(item: ThreadItem, sort: String): Pair<Long, Boolean> =
    if (item.isRoot && item.total > 1 && !sort.startsWith("opened") && item.ultimaData > item.article.dateEpoch) {
        item.ultimaData to true
    } else {
        item.article.dateEpoch to false
    }

/**
 * 1.6.3 / 190: «autore · score · data» della riga, UNA volta per telefono e
 * pannelli (prima due copie identiche in ThreadListScreen e ThreePaneHome).
 */
@androidx.compose.runtime.Composable
fun testoAutoreData(item: ThreadItem, article: Article): String {
    val (quando, ultimo) = dataDellaRiga(item, AppSettings.threadSort)
    val data = formatThreadDateTime(quando)
    return authorDisplayName(article.fromField) +
        (if (article.score != 0) "  ·  score ${article.score}" else "") +
        "  ·  " +
        (if (ultimo) androidx.compose.ui.res.stringResource(org.openusenet.reader.R.string.tl_ultimo, data) else data)
}

/**
 * 1.3.2: le tre funzioni qui sotto sono l'UNICA implementazione di
 * "quali messaggi compongono questa discussione, e qual è il primo non letto".
 *
 * Perché nascono adesso: la stessa logica era ricopiata in QUATTRO punti
 * (menu contestuale del telefono e dei pannelli, tocco sull'omino, "espandi e
 * vai" della 1.3.1) e stava per diventare cinque con l'apertura diretta sul
 * telefono. La 1.3 ci ha già mostrato il costo di questa forma di
 * duplicazione: una correzione applicata a due copie su quattro e un difetto
 * uscito in pubblico. Prima di aggiungere il quinto punto, si riduce a uno.
 *
 * 1.3.5 — LA RIDUZIONE E' ARRIVATA FINO IN FONDO SOLO ADESSO.
 *
 * Fino alla 1.3.4 l'app rispondeva alla domanda "quali messaggi stanno in
 * questa discussione" in TRE modi diversi:
 *
 *  1. il THREADER JWZ (Threader.thread -> ThreadNode.flatten), che e' quello
 *     che disegna davvero lo schermo: segue la catena delle References con un
 *     regex sui message-id e crea CONTENITORI FANTASMA per gli anelli
 *     mancanti, cosi' una discussione il cui post iniziale non e' mai stato
 *     scaricato resta una sola discussione;
 *  2. `membriDelThread`, che confrontava stringhe:
 *         it.messageId == rootKey || it.references.contains(rootKey)
 *  3. `threadIdsOf`, DUPLICATA in ThreadListScreen e ThreePaneHome, con la
 *     stessa contains() ma una radice calcolata in modo ancora diverso.
 *
 * Le tre concordano quasi sempre. Quando divergono escono i due difetti
 * segnalati il 13 settembre 2026, che sono lo STESSO difetto:
 *
 *  - "apri il primo non letto riporta al primo post, gia' letto". Discussione
 *    con l'originale P mai scaricato e tre risposte A, B, C: il threader le
 *    appende a un fantasma, flatten() lo salta, e rootKey diventa A — una
 *    RISPOSTA promossa a radice. Ma B e C referenziano P, non A: la contains()
 *    non li trova, restano fuori, e se A e' letto primoNonLetto cade sul
 *    ripiego e riapre A.
 *  - "marco la discussione letta e resta visibile". Basta un messaggio il cui
 *    client scrive nelle References il solo padre diretto invece della catena
 *    intera: threadIdsOf non lo raccoglie, quello resta non letto e la
 *    discussione resta accesa. Il threader invece lo aveva agganciato, perche'
 *    lui la catena la percorre.
 *
 * RIMEDIO: una fonte di verita' sola, ed e' il threader. [mappaDiscussioni]
 * chiede a LUI a quale discussione appartenga ogni messaggio; [membriDelThread]
 * e threadIdsOf leggono quella mappa. Diventa impossibile PER COSTRUZIONE che
 * un menu veda una discussione diversa da quella disegnata.
 *
 * E' la lezione della 1.3 applicata una seconda volta, dopo che la prima si era
 * fermata a meta': nella 1.3.2 le quattro copie dentro "primo non letto" erano
 * state ridotte a una, ma threadIdsOf faceva la stessa cosa poco piu' in la'
 * e non era stata contata.
 */

/**
 * Radice di un [ThreadItem], come CHIAVE DI VISUALIZZAZIONE (expandedRoots).
 *
 * `rootKey` è popolato SOLO dalla vista raggruppata e da quella ad albero;
 * nella sequenziale resta vuoto, e il ripiego la ricava dall'articolo (primo
 * References, o il messageId stesso se è un capo-thread).
 *
 * ATTENZIONE (1.3.5): questa funzione NON decide piu' l'appartenenza a una
 * discussione — per quello c'e' [mappaDiscussioni], che chiede al threader.
 * Qui serve solo come chiave dell'insieme dei thread espansi, dove conta che
 * sia la STESSA stringa che [rootsList] mette nelle righe, non che sia
 * semanticamente la radice giusta. Non riusarla per filtrare articoli: e' la
 * strada da cui sono usciti i difetti del 13 settembre 2026.
 */
fun radiceDi(item: ThreadItem): String {
    if (item.rootKey.isNotEmpty()) return item.rootKey
    val a = item.article
    return a.references.trim().split(Regex("\\s+")).firstOrNull { it.isNotBlank() }
        ?: a.messageId
}

/**
 * A quale discussione appartiene ogni messaggio, SECONDO IL THREADER.
 * Chiave: messageId dell'articolo. Valore: messageId della riga di testa della
 * sua discussione — lo stesso `rootKey` che [rootsList] e [treeList] mettono
 * nelle righe, perche' calcolato allo stesso modo (`flatten().first()`).
 *
 * Va costruita sull'elenco COMPLETO degli articoli, non su quello filtrato: se
 * si costruisse sui soli non letti, i letti resterebbero senza discussione e
 * "marca tutto letto" tornerebbe a mancarne qualcuno.
 *
 * COSTO: una passata del threader in piu' rispetto a quella del disegno. E'
 * accettabile perche' il costo che aveva fatto crashare i gruppi da 47.000
 * articoli (beta20) non era la costruzione dell'albero ma `flatten()` ripetuto
 * migliaia di volte dentro gli ordinamenti; qui flatten() gira una volta per
 * radice. Da memorizzare comunque con `remember(articles)` nelle schermate.
 */
fun mappaDiscussioni(articoli: List<Article>): Map<String, String> {
    val radici = Threader.thread(
        articoli.map { ThreadableArticle(it.messageId, it.references, it.subject, it.dateEpoch) }
    )
    val out = HashMap<String, String>(articoli.size * 2)
    for (radice in radici) {
        val piatto = radice.flatten()
        val chiave = piatto.firstOrNull()?.second?.messageId ?: continue
        for ((_, ta) in piatto) out[ta.messageId] = chiave
    }
    return out
}

/**
 * Messaggi della discussione a cui appartiene [articolo], in ordine di data,
 * secondo [mappa] (vedi [mappaDiscussioni]).
 *
 * Con [soloRisposteAMe] restituisce le sole risposte dirette a un mio post
 * (campo `Article.replyToMe`, calcolato al sync: unica fonte di verità).
 *
 * Se [articolo] non compare nella mappa — non dovrebbe succedere, ma una
 * lista puo' essere cambiata sotto — si ripiega sul solo articolo invece che
 * sull'intero gruppo: sbagliare per difetto qui significa aprire il messaggio
 * su cui l'utente ha toccato, che e' un esito comprensibile; sbagliare per
 * eccesso significherebbe marcare letto un gruppo intero.
 */
fun membriDelThread(
    articoli: List<Article>,
    mappa: Map<String, String>,
    articolo: Article,
    soloRisposteAMe: Boolean = false,
): List<Article> {
    val chiave = mappa[articolo.messageId]
        ?: return if (soloRisposteAMe) emptyList() else listOf(articolo)
    return articoli
        .filter { (!soloRisposteAMe || it.replyToMe) && mappa[it.messageId] == chiave }
        .sortedBy { it.dateEpoch }
}

/**
 * Primo messaggio NON LETTO di [membri]; se sono tutti letti, il primo in
 * assoluto. È la regola scelta esplicitamente da Roxy per "espandi e vai"
 * ("va in automatico sul primo messaggio non letto"), con ripiego sull'OP
 * che era la richiesta originaria.
 */
fun primoNonLetto(membri: List<Article>): Article? =
    membri.firstOrNull { !it.read } ?: membri.firstOrNull()

/**
 * Punto di ingresso: costruisce la lista secondo [view] e [sort].
 * [sort] e' una chiave di AppSettings.threadSorts: distingue sia QUALE data
 * guardare (ultimo messaggio del thread vs apertura del thread) sia il verso.
 */
fun buildThreadList(
    articles: List<Article>,
    unreadOnly: Boolean,
    view: String,
    sort: String = AppSettings.threadSort,
    expandedRoots: Set<String> = emptySet(),
): List<ThreadItem> =
    when (view) {
        // la vista sequenziale e' un flusso piatto: conta solo il verso sulla
        // data del singolo messaggio
        ThreadViews.SEQUENTIAL -> sequentialList(articles, unreadOnly, ascending = sort.endsWith("_asc"))
        ThreadViews.ROOTS -> rootsList(articles, unreadOnly, sort, expandedRoots)
        else -> treeList(articles, unreadOnly, sort)
    }

/**
 * messageId delle radici di TUTTE le discussioni visibili: serve al comando
 * "espandi tutto" senza dover ricostruire l'albero nella schermata.
 */
/**
 * 1.4.6 (par. B13) — IN QUALE DISCUSSIONE STA UN MESSAGGIO.
 *
 * BIG Umberto: «la ricerca funziona, ma ti apre il post, non ti fa vedere dove
 * e'». Per portarlo nell'elenco bisogna prima sapere quale discussione aprire:
 * se e' chiusa, quel messaggio non e' nemmeno una riga.
 *
 * Si cerca nell'elenco COMPLETAMENTE APERTO, che e' l'unico in cui ogni
 * messaggio compare di sicuro. Funzione pura, provata in `CoreTests`: e' la
 * parte che decide, il resto e' scorrimento.
 */
fun discussioneDi(flatCompleto: List<ThreadItem>, articleId: Long): String? =
    flatCompleto.firstOrNull { it.article.id == articleId }?.rootKey?.takeIf { it.isNotEmpty() }

/**
 * 1.5.9 / 177 (vort3) — QUALE RIGA COLORARE COME «ULTIMO LETTO».
 *
 * La riga dell'articolo, se c'e'. Se non c'e' — la sua discussione e' chiusa,
 * perche' dentro l'articolo ci si e' spostati con ▶ in un'altra — la riga di
 * testa della sua discussione, che NON si apre: la richiesta era tenere aperte
 * quelle aperte dall'utente, non aprirne altre. Altrimenti nessuna (articolo
 * nascosto da «solo non letti», filtrato, cancellato).
 *
 * L'appartenenza la decide [mappa] (vedi [mappaDiscussioni]), cioe' il
 * threader: lo stesso `rootKey` che [rootsList] mette nelle righe.
 */
fun rigaUltimoLetto(
    righe: List<ThreadItem>,
    articoli: List<Article>,
    mappa: Map<String, String>,
    ultimoLetto: Long?,
): Long? {
    val id = ultimoLetto ?: return null
    if (righe.any { it.article.id == id }) return id
    val msgId = articoli.firstOrNull { it.id == id }?.messageId ?: return null
    val radice = mappa[msgId] ?: return null
    return righe.firstOrNull { it.isRoot && it.rootKey == radice }?.article?.id
}

fun allRootKeys(articles: List<Article>, unreadOnly: Boolean): Set<String> =
    orderedRoots(articles, unreadOnly, AppSettings.threadSort).first
        .mapNotNull { it.flatten().firstOrNull()?.second?.messageId }
        .toSet()

/**
 * Radici ordinate (filtro non-letti a livello di thread) + indice per messageId.
 *
 * 1.6.1 / 187 (Cercaparole, Michele e Roxy su Usenet, 5/11): in cima stanno le
 * discussioni seguite CON almeno un non letto. Prima ci stavano tutte, e una
 * seguita vecchia e gia' letta restava sopra le discussioni nuove. Una seguita
 * tutta letta torna al suo posto secondo l'ordinamento scelto, e risale se
 * arriva una risposta. Nessun interruttore.
 *
 * ⚠️ Una seguita letta ora SCENDE sotto gli occhi di chi legge: per questo
 * [novitaInCima] guarda anche l'id piu' alto, [ThreadScrollMemory] ricorda se si
 * era in cima, e lo swipe usa [idNonLettoNelGruppo].
 *
 * 1.6.3 / 190 (BIG Umberto, 12/11): chi segue molte discussioni le vuole SEMPRE
 * in cima, come fino alla 1.6.1 / 186, e le due richieste si escludono. Diventa
 * una scelta in Aspetto: [seguiteSoloConNonLetti] false = sempre in cima
 * (predefinito), true = il modo di Cercaparole. Le tre guardie qui sopra
 * restano giuste in tutti e due i modi e non dipendono dalla scelta.
 */
internal fun orderedRoots(
    articles: List<Article>,
    unreadOnly: Boolean,
    sort: String,
    seguiteSoloConNonLetti: Boolean = AppSettings.seguiteSoloConNonLetti(),
): Pair<List<ThreadNode>, Map<String, Article>> {
    val byMsgId = articles.associateBy { it.messageId }
    var roots = Threader.thread(
        articles.map { ThreadableArticle(it.messageId, it.references, it.subject, it.dateEpoch) }
    )

    // beta20 (rpoggi): con gruppi molto grandi (it.test, ~47.000 articoli) l'app
    // crashava all'arrivo di ogni nuovo messaggio; sotto le poche migliaia non
    // succedeva mai. CAUSA: `flatten()` ricostruisce l'intero sotto-albero di una
    // radice, e qui veniva richiamato PIU' VOLTE per ogni radice — nel filtro
    // non-letti, come chiave di ordinamento (e `sortedBy` la invoca a OGNI
    // confronto, quindi migliaia di volte), e nella partizione dei seguiti.
    // Il costo esplodeva col quadrato del numero di thread, e ogni articolo in
    // arrivo faceva riemettere l'intera lista rifacendo tutto da capo.
    // RIMEDIO: appiattire UNA volta sola per radice e riusare il risultato.
    val appiattiti: Map<ThreadNode, List<Pair<Int, ThreadableArticle>>> =
        roots.associateWith { it.flatten() }
    fun piatto(root: ThreadNode): List<Pair<Int, ThreadableArticle>> =
        appiattiti[root] ?: root.flatten()

    if (unreadOnly) {
        roots = roots.filter { root -> piatto(root).any { (_, ta) -> byMsgId[ta.messageId]?.read == false } }
    }

    // Due chiavi possibili:
    //  - attività: data del messaggio più recente del thread (il thread vivo sale)
    //  - apertura: data della radice, cioè del primo messaggio in ordine di data
    fun lastActivity(root: ThreadNode): Long =
        piatto(root).maxOfOrNull { (_, ta) -> ta.dateEpoch } ?: 0L
    fun opened(root: ThreadNode): Long =
        piatto(root).minOfOrNull { (_, ta) -> ta.dateEpoch } ?: 0L

    // le chiavi si calcolano una volta per radice, non a ogni confronto
    val chiaveFn: (ThreadNode) -> Long = if (sort.startsWith("opened")) ::opened else ::lastActivity
    val chiavi: Map<ThreadNode, Long> = roots.associateWith(chiaveFn)
    val chiave: (ThreadNode) -> Long = { chiavi[it] ?: 0L }
    roots = if (sort.endsWith("_asc")) roots.sortedBy(chiave) else roots.sortedByDescending(chiave)

    val watchedIds = articles.asSequence().filter { it.watched }.map { it.messageId }.toSet()
    val (w, n) = if (watchedIds.isEmpty()) roots to emptyList()
                 else roots.partition { root ->
                     piatto(root).any { (_, ta) -> ta.messageId in watchedIds } &&
                         (!seguiteSoloConNonLetti ||
                             piatto(root).any { (_, ta) -> byMsgId[ta.messageId]?.read == false })
                 }
    return (w + n) to byMsgId
}

private fun treeList(articles: List<Article>, unreadOnly: Boolean, sort: String): List<ThreadItem> {
    val (roots, byMsgId) = orderedRoots(articles, unreadOnly, sort)
    val mieEmail = AppSettings.myEmails   // globale + per-server (2.1b)
    val out = ArrayList<ThreadItem>()
    for (root in roots) {
        val rows = root.flatten().mapNotNull { (depth, ta) -> byMsgId[ta.messageId]?.let { depth to it } }
        // con "solo non letti" nasconde i messaggi già letti anche dentro l'albero,
        // mantenendo il rientro originale (le risposte restano annidate)
        val visible = if (unreadOnly) rows.filter { (_, a) -> !a.read } else rows
        // beta20 (Bingo3331): la vista ad albero non popolava rootKey, isRoot,
        // hasMyPost e repliesToMe — venivano lasciati ai valori di default.
        // Conseguenze: l'omino e il contatore delle risposte NON comparivano
        // affatto in questa vista, e dove comparivano il salto alle risposte
        // riceveva un rootKey VUOTO (con cui `references.contains("")` è sempre
        // vero e il filtro selezionava a caso). Ora la riga di testa della
        // discussione porta le stesse informazioni della vista raggruppata.
        val rootKey = rows.firstOrNull()?.second?.messageId ?: ""
        // 1.4.1 (105/A) — stesso criterio di AppSettings.eMioPost, che dalla
        // 1.2.12 ESTRAE l'indirizzo dal campo From e lo confronta, invece di
        // cercarlo come sottostringa della riga. Qui la vecchia forma era
        // sopravvissuta all'audit: bastava `From: "tuo@indirizzo.it"
        // <altro@evil.net>` per far comparire l'omino, e un indirizzo corto
        // scattava dentro uno piu' lungo (mario@tin.it in supermario@tin.it).
        val hasMyPost = rows.any { (_, a) -> AppSettings.eMioPost(a.fromField) }
        val risposteAMe = rows.count { (_, a) -> a.replyToMe }
        visible.forEachIndexed { i, (depth, a) ->
            out.add(
                ThreadItem(
                    depth = depth,
                    article = a,
                    threadStart = i == 0,
                    rootKey = rootKey,
                    // la riga di testa fa da "radice" ai fini di omino/contatore
                    isRoot = i == 0,
                    hasMyPost = if (i == 0) hasMyPost else false,
                    repliesToMe = if (i == 0) risposteAMe else 0,
                )
            )
        }
    }
    return out
}

private fun sequentialList(articles: List<Article>, unreadOnly: Boolean, ascending: Boolean): List<ThreadItem> {
    val visible = if (unreadOnly) articles.filter { !it.read } else articles
    val sorted = if (ascending) visible.sortedBy { it.dateEpoch } else visible.sortedByDescending { it.dateEpoch }
    // beta20: anche qui il rootKey era vuoto; lo ricavo dalle References così
    // le funzioni che ne dipendono (salto alle risposte, vista continua)
    // funzionano anche in vista sequenziale.
    return sorted.map { a ->
        val rk = a.references.trim().split(Regex("\\s+")).firstOrNull { it.isNotBlank() } ?: a.messageId
        ThreadItem(depth = 0, article = a, threadStart = false, rootKey = rk)
    }
}

/**
 * Vista raggruppata: una riga per discussione. Se la radice e' in
 * [expandedRoots] subito sotto compaiono le risposte, indentate come
 * nell'albero. La radice NON viene ripetuta fra le risposte, cosi' ogni
 * articolo produce al massimo una riga (le liste Compose usano l'id
 * dell'articolo come chiave e non tollerano duplicati).
 */
private fun rootsList(
    articles: List<Article>,
    unreadOnly: Boolean,
    sort: String,
    expandedRoots: Set<String>,
): List<ThreadItem> {
    val (roots, byMsgId) = orderedRoots(articles, unreadOnly, sort)
    val out = ArrayList<ThreadItem>()
    // beta14 (Big Umberto): email dell'identità, per marcare le discussioni che
    // contengono un mio post. Calcolata una volta qui.
    val mieEmail = AppSettings.myEmails   // globale + per-server (2.1b)
    for (root in roots) {
        val flat = root.flatten()
        val rootKey = flat.firstOrNull()?.second?.messageId ?: continue
        // membri effettivamente presenti in locale (un reference puo' puntare
        // a un messaggio mai scaricato: quello non ha riga)
        val members = flat.mapNotNull { (d, ta) -> byMsgId[ta.messageId]?.let { d to it } }
        val head = members.firstOrNull() ?: continue
        val isOpen = rootKey in expandedRoots
        // beta18 (Bingo3331): conto le risposte ai miei post leggendo il campo
        // ESISTENTE Article.replyToMe (calcolato al sync in Repository), invece
        // di ricalcolare l'algoritmo qui: unica fonte di verità, niente doppioni.
        val nRisposteAMe = members.count { it.second.replyToMe }
        out.add(
            ThreadItem(
                depth = 0,
                article = head.second,
                threadStart = true,
                replies = (members.size - 1).coerceAtLeast(0),
                rootKey = rootKey,
                isRoot = true,
                unread = members.count { !it.second.read },
                total = members.size,
                expanded = isOpen,
                // beta14 (Big Umberto): la discussione contiene un mio post?
                // 1.4.1 (105/A): vedi la nota nell'altra vista — un solo
                // criterio, quello irrobustito dall'audit della 1.2.12.
                hasMyPost = members.any { m -> AppSettings.eMioPost(m.second.fromField) },
                // beta18 (Bingo3331): quante risposte dirette ai miei post.
                repliesToMe = nRisposteAMe,
                // 1.6.3 / 190 (BIG Umberto): vedi dataDellaRiga.
                ultimaData = members.maxOf { it.second.dateEpoch },
            )
        )
        if (isOpen) {
            val replies = members.drop(1)
            // 1.4.10 (par. K4) — APRIRE UNA DISCUSSIONE LA APRE TUTTA.
            //
            // SEGNALAZIONE, cinque parole: «faccio tap sul + ma non mi apre
            // l'albero». Era esatta, e il difetto stava qui.
            //
            // ⚠️ IL `+` C'ERA E NON FACEVA NIENTE. Il comando compare quando
            // `item.total > 1`, e `total` conta TUTTI i membri della
            // discussione (riga sopra: `members.size`). Qui invece, con «solo
            // non letti» acceso, le risposte gia' lette venivano filtrate via.
            // Una discussione con il primo messaggio non letto e tutte le
            // risposte lette mostrava dunque il `+`, dichiarava «3 risposte»
            // nel contatore, e aprendola non compariva nulla.
            //
            // *Un comando premibile che non fa niente e' peggio di un comando
            // assente: il secondo informa, il primo fa dubitare l'utente di
            // se' stesso.* E infatti ha chiesto se sbagliava lui.
            //
            // LA SCELTA, fra tre. (A) far sparire il `+` quando non c'e'
            // niente da mostrare: avrebbe richiesto di cambiare ANCHE il
            // contatore, altrimenti dice «3 risposte» su una discussione che
            // non si apre — l'incoerenza si spostava invece di sparire.
            // (C) mostrare le lette in grigio: stato visivo nuovo in una vista
            // gia' densa. (B, scelta) il `+` apre tutto.
            //
            // IL PRINCIPIO: il filtro «solo non letti» dice che cosa mostra
            // l'ELENCO; toccare il `+` e' un gesto esplicito su UNA
            // discussione. *Un gesto esplicito su un oggetto vince su una
            // preferenza generale sulla lista.* Il contatore torna onesto da
            // se', senza toccarlo.
            //
            // ⚠️ `unreadOnly` serve ancora, ed e' qui sopra: decide quali
            // RADICI compaiono (orderedRoots). Non si tocca quello.
            //
            // Terza sorpresa in una settimana prodotta da «solo non letti»,
            // dopo par. A16 (Jeff: credeva che l'app gli cancellasse i
            // messaggi) e par. U1.
            val visible = replies
            for ((depth, a) in visible) {
                out.add(
                    ThreadItem(
                        depth = depth.coerceAtLeast(1),
                        article = a,
                        rootKey = rootKey,
                    )
                )
            }
        }
    }
    return out
}

/**
 * Wishlist beta11 (Marco, che odia il crossposting): elenco dei gruppi
 * nell'header Newsgroups: di un articolo, letto da [Article.headersRaw]
 * già salvato da fetchBody per la vista "Intestazioni complete" — NESSUNA
 * nuova richiesta di rete. Lista vuota se il corpo non è ancora stato
 * scaricato (headersRaw null) o se l'header manca per qualche motivo.
 *
 * Condivisa tra ArticleScreen (telefono) e ThreePaneHome, per il badge
 * "X-post" in lettura e l'avviso al tocco di "Rispondi".
 */
fun crosspostGroups(headersRaw: String?): List<String> {
    if (headersRaw.isNullOrBlank()) return emptyList()
    val (headers, _) = MimeUtil.splitArticle(headersRaw.lines() + "")
    val raw = headers["newsgroups"] ?: return emptyList()
    return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}

/**
 * 1.4 — dove l'AUTORE ha chiesto che vada la risposta (`Followup-To`,
 * RFC 5536 §3.2.6).
 *
 * NASCE DAL GNKSA. Due domande di un utente americano, il 13 settembre 2026,
 * hanno portato a verificare i nove criteri GNKSA 2.0 invece di rispondere a
 * memoria: su otto eravamo allineati, su uno no. `Followup-To` non era ne'
 * letto ne' rispettato — zero occorrenze in tutto il sorgente. Rispondendo a un
 * articolo che diceva "Followup-To: it.discussioni.altro", NeReO rispondeva nel
 * gruppo in cui ci si trovava. Su Usenet e' una scortesia vera, ed e' il tipo di
 * difetto che un lettore esperto nota subito.
 *
 * E' la stessa funzione di [crosspostGroups] con un'altra chiave: l'header e'
 * gia' in `headersRaw`, salvato da fetchBody. Nessuna richiesta di rete,
 * nessuna colonna nuova, nessuna migrazione.
 *
 * LIMITE DA DICHIARARE, NON DA SCOPRIRE DOPO: `Followup-To` NON e'
 * nell'overview (OVER/XOVER). Si conosce solo DOPO che l'articolo e' stato
 * scaricato — in pratica non e' un problema, non si risponde a qualcosa che non
 * si e' letto, ma significa che non se ne puo' mostrare un contrassegno
 * nell'elenco discussioni prima di aprire il messaggio.
 */
fun followupTo(headersRaw: String?): List<String> {
    if (headersRaw.isNullOrBlank()) return emptyList()
    val (headers, _) = MimeUtil.splitArticle(headersRaw.lines() + "")
    val raw = headers["followup-to"] ?: return emptyList()
    return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}

/** Dove deve uscire una risposta, secondo l'autore dell'articolo. */
sealed interface DestinazioneRisposta {
    /** Nessun `Followup-To`: decide il gruppo che si sta leggendo, come sempre. */
    data object Nessuna : DestinazioneRisposta

    /**
     * `Followup-To: poster` — l'autore chiede risposte PRIVATE, per posta.
     *
     * NeReO non sa mandare posta (e' un lettore Usenet, senza SMTP), quindi
     * non si blocca la pubblicazione: si avvisa in modo inequivocabile e si
     * offre l'indirizzo. Chi vuole rispondere sul gruppo lo fara' SAPENDO di
     * andare contro la richiesta dell'autore — che e' diverso dal farlo per
     * ignoranza.
     */
    data object SoloAutore : DestinazioneRisposta

    /** L'autore ha indicato dei gruppi: la risposta esce LI'. */
    data class Gruppi(val gruppi: List<String>) : DestinazioneRisposta
}

/**
 * Legge il `Followup-To` e dice dove va la risposta.
 *
 * Funzione PURA, provata in `CoreTests`: e' la regola su cui si decide dove
 * finisce un messaggio pubblico, ed e' esattamente il genere di cosa che non si
 * collauda a mano tre volte e poi si spera.
 *
 * `poster` VINCE su tutto il resto se compare: un `Followup-To: poster,
 * it.test` e' malformato, ma se l'autore ha scritto "poster" da qualche parte
 * la sua intenzione e' quella, e la lettura prudente e' rispettarla.
 * *(Deduzione dal significato dell'header, non da una prescrizione della RFC:
 * la RFC 5536 §3.2.6 non prevede quella forma mista.)*
 */
fun destinazioneRisposta(headersRaw: String?): DestinazioneRisposta {
    val f = followupTo(headersRaw)
    if (f.isEmpty()) return DestinazioneRisposta.Nessuna
    if (f.any { it.equals("poster", ignoreCase = true) }) return DestinazioneRisposta.SoloAutore
    return DestinazioneRisposta.Gruppi(f)
}

/**
 * 1.4.6 (§A1, punto 4) — LA RICHIESTA DELL'AUTORE E' ANCORA IN VIGORE?
 *
 * IL DIFETTO CHE CHIUDE (§A2, segnalato da Dave Royal il 22 settembre). Nella
 * schermata di scrittura due righe potevano smentirsi a una riga di distanza: la
 * riga dei gruppi leggeva il valore FILTRATO da `crosspostScelto`, l'avviso
 * leggeva quello GREZZO. Con una scelta esplicita gia' fatta, l'avviso
 * continuava a dire «l'autore chiede che le risposte vadano in uk.test» mentre
 * il messaggio sarebbe uscito in it.test.
 *
 * ⚠️ E DAL 30 SETTEMBRE IL CASO E' PIU' FACILE DA INCONTRARE, non meno. Fino a
 * ieri nella vista a PANNELLI `crosspostScelto` non veniva mai impostato — era
 * §A13.2 — quindi la contraddizione li' non si vedeva: non perche' fosse
 * corretta, ma perche' un SECONDO difetto la nascondeva. Correggendo §A13.2
 * abbiamo tolto il tappo. *Chiudere un difetto puo' rendere visibile quello che
 * gli stava sotto: e' una ragione per cercarlo, non per rimandare.*
 *
 * COSA FA. Una scelta esplicita dell'utente ritira la richiesta dell'autore —
 * «una decisione umana non si scavalca con un'intestazione», ma vale anche al
 * contrario: una volta scavalcata, l'intestazione non va piu' annunciata come se
 * comandasse ancora.
 *
 * ⚠️ [DestinazioneRisposta.SoloAutore] NON si ritira mai, ed e' voluto: `poster`
 * non indica dei gruppi alternativi fra cui scegliere, dice che l'autore vuole
 * una risposta PRIVATA. Scegliere dei gruppi nel selettore non e' una risposta a
 * quella richiesta — e l'avviso porta con se' l'indirizzo, che resta utile.
 *
 * PERCHE' E' UNA FUNZIONE E NON DUE RIGHE SUL POSTO: serve in tutte e due le
 * viste, ed e' precisamente il punto 4 del disegno di Dave — «avviso e
 * comportamento dalla stessa fonte». Scritta due volte sarebbe tornata a
 * divergere, che e' come il difetto e' nato.
 */
fun destinazioneAncoraInVigore(
    destinazione: DestinazioneRisposta,
    sceltaEsplicita: Boolean,
): DestinazioneRisposta =
    if (sceltaEsplicita && destinazione is DestinazioneRisposta.Gruppi) DestinazioneRisposta.Nessuna
    else destinazione

/**
 * 1.2.6 (Marco): toglie dalle intestazioni mostrate il blob base64 di `Face:` e
 * `X-Face:` — dispersivo. Salta la riga dell'header E le sue righe di continuazione
 * (rientrate con spazio/tab, RFC 5322 folding), riprendendo alla prima intestazione
 * vera. Solo visualizzazione: `headersRaw` sul disco resta intatto, e il Face continua
 * a comparire come faccina accanto all'autore.
 */
private val FACE_HEADER = Regex("^(x-face|face)\\s*:", RegexOption.IGNORE_CASE)

fun nascondiFaceHeaders(headersRaw: String?): String {
    val src = headersRaw ?: return ""
    val out = ArrayList<String>()
    var skipping = false
    for (line in src.split('\n')) {
        val continua = line.isNotEmpty() && (line[0] == ' ' || line[0] == '\t')
        if (skipping) {
            if (continua) continue else skipping = false
        }
        if (FACE_HEADER.containsMatchIn(line)) { skipping = true; continue }
        out.add(line)
    }
    return out.joinToString("\n")
}
