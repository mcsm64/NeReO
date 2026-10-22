/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Motore filtri con scoring stile slrn/Gnus.
 *
 * Idea di fondo (per chi viene dall'oleodinamica: pensala come una centralina
 * con più valvole in serie — ogni regola aggiunge o toglie "pressione" cioè
 * punteggio, e a valle due pressostati decidono: sotto la soglia kill
 * l'articolo viene scartato, sopra la soglia watch viene evidenziato).
 *
 * Puro Kotlin/JVM, testato in app/src/test.
 */
package org.openusenet.reader.filter

/**
 * Campo dell'articolo su cui la regola fa match.
 *
 * REFERENCES (M1, release personale Marco): match sulla catena degli antenati.
 * Permette una regola manuale "tutto ciò che discende da <questo message-id>",
 * ed è il primitivo su cui poggia la propagazione automatica di
 * [FollowupPropagator]. Non compare fra i chip della schermata filtri: è un
 * campo da kill-file, per chi sa cosa sta facendo.
 */
/**
 * Campi su cui una regola puo' fare match.
 *
 * 1.4.4 — NEWSREADER.
 *
 * A cosa serve: un autore puo' cambiare nickname quando vuole, ma il programma
 * con cui pubblica lo dichiara lui stesso e cambia di rado. Una regola su From
 * va rifatta a ogni nick nuovo; una sul lettore no.
 *
 * NON E' UN CAMPO COME GLI ALTRI, e va detto qui perche' e' l'unica cosa da
 * ricordare usandolo: `User-Agent` NON sta nel riepilogo overview (RFC 3977
 * §8.3), che porta solo Subject, From, Date, Message-ID, References, bytes e
 * righe. Sta nelle intestazioni COMPLETE, che arrivano insieme al corpo.
 * Quindi una regola su questo campo scatta **dopo lo scarico**, non al sync
 * come le altre: e' un plonk a posteriori, non un filtro preventivo.
 *
 * Con "prefetch risposte" acceso (predefinito) o con "scarica i corpi" la
 * differenza si sente poco; a corpi non scaricati, la regola semplicemente non
 * si valuta — [valoreCampo] torna null e il motore la salta senza alterare il
 * punteggio, esattamente come fa da sempre con BODY.
 */
/**
 * 1.5.7 / 175 — HEADERS («Intestazioni»): cerca in TUTTE le intestazioni del
 * messaggio, una riga «nome: valore» per intestazione, nomi in minuscolo.
 * Serve alle impronte che un autore si porta dietro senza saperlo (Marco, 20
 * ottobre: «X-Antivirus: Norton ... Outbound message»). Come NEWSREADER, si
 * valuta solo sugli articoli gia' scaricati: le intestazioni complete arrivano
 * col corpo, non col riepilogo overview.
 */
enum class FilterField { FROM, SUBJECT, MESSAGE_ID, XPOST, BODY, REFERENCES, NEWSREADER, HEADERS }

/**
 * 1.4.4 — QUESTO CAMPO IDENTIFICA UN AUTORE?
 *
 * È il criterio che decide se a una regola abbia senso offrire l'estensione
 * alle risposte: la discendenza di un post vuol dire qualcosa solo se la regola
 * individua *chi* l'ha scritto. Su un oggetto o su un crosspost non vuol dire
 * niente, ed è la ragione per cui il vincolo esiste dalla M1.
 *
 * PERCHÉ È QUI E NON DENTRO L'EDITOR. Il criterio era scritto **due volte** in
 * `FiltersScreen`, una per disegnare la casella e una per salvarla, e tutte e
 * due dicevano solo `FROM`. Quando il campo `NEWSREADER` è stato aggiunto al
 * motore, quelle due righe sono rimaste indietro: la casella non compariva, e
 * il salvataggio azzerava in silenzio una propagazione che l'utente non poteva
 * nemmeno vedere. Segnalato da Marco il 24 settembre sulla sua regola, che
 * aveva perso la riga `Followup: subtree` senza che nessuno glielo dicesse.
 *
 * Ora la risposta sta in un posto solo, ed è l'enum stessa a darla.
 *
 * NEWSREADER identifica un autore quanto FROM, e in un senso più forte: FROM
 * cambia a ogni nickname nuovo, il programma con cui si pubblica no.
 */
val FilterField.identificaUnAutore: Boolean
    get() = this == FilterField.FROM || this == FilterField.NEWSREADER || this == FilterField.HEADERS

/**
 * 1.5.7 / 175: il campo si valuta solo con le intestazioni COMPLETE (che
 * arrivano col corpo). Un posto solo per le due regole che ne hanno bisogno,
 * cosi' chi decide «valutabile o no» non deve elencarle a mano.
 */
val FilterField.richiedeIntestazioni: Boolean
    get() = this == FilterField.NEWSREADER || this == FilterField.HEADERS

/**
 * Come sopra, partendo dal nome memorizzato in `FilterRuleEntity.field`.
 * Un valore che non corrisponde a nessun campo noto risponde `false` invece di
 * far cadere la schermata.
 */
fun campoIdentificaUnAutore(nomeCampo: String): Boolean =
    FilterField.entries.any { it.name == nomeCampo && it.identificaUnAutore }

/**
 * Tipo di match: contains semplice, wildcard (* e ?) o regex completa.
 *
 * 1.5.7 / 175 — NICK_EMAIL: «nick = indirizzo», il confronto anti-sockpuppet
 * (vedi NickIndirizzo.kt). Ha senso sul campo FROM; il pattern non si usa
 * (l'editor salva «*», perche' il kill-file scarta le regole senza pattern).
 */
enum class MatchType { CONTAINS, WILDCARD, REGEX, NICK_EMAIL }

/**
 * M1 (release personale Marco): estensione di una regola alle RISPOSTE.
 *
 * Il motore di scoring valuta un articolo per volta e non sa nulla di thread:
 * questa enum non cambia [FilterEngine], dichiara soltanto l'INTENZIONE della
 * regola. Chi la esegue è [FollowupPropagator], che lavora sulla catena
 * References dopo che il motore ha girato.
 *
 *  - NONE    : comportamento storico. Colpisce solo l'autore che matcha.
 *  - DIRECT  : colpisce anche chi risponde DIRETTAMENTE (un solo livello).
 *              Non esposto nella UI — vedi nota sotto — ma supportato dal
 *              motore e dal formato kill-file.
 *  - SUBTREE : colpisce TUTTA la discendenza del post che matcha.
 *
 * Perché DIRECT non è nella UI: nascondere chi risponde al troll lasciando
 * visibili le contro-risposte è il peggiore dei due mondi, perché quelle
 * tipicamente CITANO il testo del troll — che quindi ti arriva lo stesso, di
 * rimbalzo e senza contesto. Resta nel modello perché costa zero e cambiare
 * idea non deve voler dire rifare la migrazione del database.
 */
enum class FollowupMode { NONE, DIRECT, SUBTREE }

/**
 * 1.5.1 — §A12-retro META' B: LA PROPAGAZIONE SEGUE L'AZIONE DELLA REGOLA.
 *
 * IL DIFETTO (aperto nella 1.4.8, rilievo originale di Cercaparole nella
 * 1.4.7): le regole create dalla schermata Filtri non hanno un `followupScore`
 * proprio, quindi le risposte prendevano SEMPRE il default -100 ed erano
 * NASCOSTE, qualunque fosse l'azione. Una regola «Segui la discussione» o
 * «Evidenzia» con «estendi alle risposte» nascondeva proprio i rami che si
 * volevano vedere.
 *
 * LA REGOLA, in un posto solo:
 *  - un `followupScore` scritto a mano (kill-file, modalita' avanzata) VINCE,
 *    sempre: chi l'ha scritto sapeva cosa voleva;
 *  - altrimenti le risposte prendono il SEGNO della regola madre: positiva ->
 *    lo STESSO punteggio (evidenziate, o seguite se arrivano alla soglia
 *    WATCH); negativa o nulla -> [FilterRule.DEFAULT_FOLLOWUP_SCORE], come
 *    sempre.
 *  - si NASCONDE solo con un punteggio negativo. ⚠️ Negativo, non «sotto la
 *    soglia HIDE»: prima della 1.5.1 si nascondeva con QUALUNQUE punteggio, e
 *    un kill-file con un followupscore di -30 continua a nascondere come prima.
 *    Per tutte le regole negative il comportamento e' IDENTICO a prima.
 *
 * Induzione dichiarata (1.4.8): non dovrebbero esistere regole propaganti
 * positive gia' salvate, perche' fino alla 1.4.4 la casella compariva solo su
 * FROM e prometteva di nascondere. Se qualcuno ne ha, al primo «Applica ora» le
 * sue risposte smettono di sparire — che e' esattamente la correzione.
 *
 * Kotlin puro: provato con kotlinc (controlli/filtri/prova_filtri.sh).
 */
data class EffettoPropagazione(
    /** Il punteggio da scrivere sulle risposte colpite. */
    val punteggio: Int,
    /** true = le risposte si nascondono (punteggio negativo). */
    val nascondi: Boolean,
    /** true = le risposte si seguono: stella (punteggio >= soglia WATCH). */
    val segui: Boolean,
)

fun effettoPropagazioneDi(
    score: Int,
    followupScore: Int?,
    soglie: Thresholds = Thresholds(),
): EffettoPropagazione {
    val p = followupScore ?: if (score > 0) score else FilterRule.DEFAULT_FOLLOWUP_SCORE
    return EffettoPropagazione(punteggio = p, nascondi = p < 0, segui = p >= soglie.watch)
}

/**
 * 1.5.7 / 175 — «SEGNARE CHI SCRIVE NEI POST DEL PLONKATO» (Marco, 20/10,
 * piano A): il punteggio da salvare per le RISPOSTE di una regola che nasconde.
 *
 *  - [attiva] = la scelta si vede nell'editor (campo che identifica un autore,
 *    «estendi alle risposte» acceso, regola non positiva). Se non si vede, si
 *    conserva cio' che c'era: l'editor non cancella un dato che non mostra
 *    (lezione 1.4.4).
 *  - [evidenzia] -> un punteggio positivo: quello gia' salvato se positivo,
 *    altrimenti la soglia HIGHLIGHT. Per [effettoPropagazioneDi] un
 *    followupScore positivo non nasconde: le risposte restano visibili e
 *    colorate, il plonkato sparisce.
 *  - altrimenti -> null (default: nascoste, come sempre), ma un punteggio
 *    NEGATIVO scritto a mano nel kill-file (es. -30) si conserva.
 *
 * Kotlin puro: provato con kotlinc (controlli/filtri).
 */
fun punteggioRisposte(attiva: Boolean, evidenzia: Boolean, salvato: Int?): Int? = when {
    !attiva -> salvato
    evidenzia -> if (salvato != null && salvato > 0) salvato else Thresholds().highlight
    else -> if (salvato != null && salvato < 0) salvato else null
}

data class FilterRule(
    val id: Long = 0,
    val name: String = "",
    val field: FilterField,
    val matchType: MatchType,
    val pattern: String,
    val score: Int,                    // delta punteggio: negativo = penalizza, positivo = promuove
    val caseSensitive: Boolean = false,
    val groupWildmat: String = "*",    // scope: "*" = globale, "it.*", "it.hobby.modellismo", ...
    val expiresEpochDay: Long? = null, // scadenza opzionale (giorno epoch); null = mai
    val enabled: Boolean = true,
    /** M1: estensione alle risposte. NONE = regola classica, invariata. */
    val followup: FollowupMode = FollowupMode.NONE,
    /**
     * M1: punteggio da applicare a chi viene colpito per PROPAGAZIONE (non per
     * match diretto). null ⇒ [DEFAULT_FOLLOWUP_SCORE].
     *
     * È volutamente separato da [score]: l'autore filtrato va eliminato, ma chi
     * gli risponde non ha colpe e va soltanto reso invisibile — in modo
     * reversibile, spegnendo la regola, senza riscaricare nulla dal server.
     */
    val followupScore: Int? = null,
) {
    /** Regex compilata (lazy, una volta sola per regola). */
    val compiled: Regex? by lazy {
        try {
            val opts = if (caseSensitive) setOf() else setOf(RegexOption.IGNORE_CASE)
            when (matchType) {
                MatchType.REGEX -> Regex(pattern, opts)
                MatchType.WILDCARD -> Regex(wildcardToRegex(pattern), opts)
                MatchType.CONTAINS, MatchType.NICK_EMAIL -> null
            }
        } catch (_: Exception) {
            null // pattern invalido ⇒ regola inerte, mai un crash
        }
    }

    fun matches(value: String): Boolean = when (matchType) {
        MatchType.NICK_EMAIL -> nickUgualeIndirizzo(value)
        MatchType.CONTAINS ->
            if (caseSensitive) value.contains(pattern) else value.contains(pattern, ignoreCase = true)
        // 1.2.12 (audit di sicurezza): il testo su cui gira la regex viene
        // troncato. Una regex può avere costo esponenziale nella lunghezza del
        // testo ("backtracking catastrofico"): con una regola scritta in modo
        // sfortunato dall'utente, chi pubblica su Usenet può scegliere un corpo
        // fatto apposta per bloccare il thread di sincronizzazione a tempo
        // indeterminato. La compilazione del pattern era già protetta (un
        // pattern invalido rende la regola inerte), l'ESECUZIONE no.
        // Il taglio non cambia nulla nell'uso reale: una regola che deve
        // pescare una parola la trova ben prima del quarto di megabyte, e i
        // messaggi di un lettore solo-testo stanno molto sotto.
        else -> compiled?.containsMatchIn(value.take(MAX_TESTO_REGEX)) ?: false
    }

    /**
     * M1: la regola scatta su questo articolo? Estrae da sola il campo giusto.
     * Serve a chi deve valutare UNA regola isolata (la propagazione alle
     * risposte) invece dell'intero punteggio cumulativo.
     */
    fun matchesArticle(article: ScorableArticle): Boolean {
        val value = valoreCampo(article, field) ?: return false
        return matches(value)
    }

    /** M1: la regola estende il proprio effetto alle risposte? */
    val propaga: Boolean get() = followup != FollowupMode.NONE

    /**
     * M1: punteggio effettivo per gli articoli colpiti per propagazione.
     *
     * 1.5.1: non e' piu' sempre [DEFAULT_FOLLOWUP_SCORE]. Lo decide
     * [effettoPropagazione], in un posto solo — questo campo ne e' una lettura,
     * cosi' chi lo usava (FollowupPropagator.propagateMulti) resta coerente.
     */
    val followupScoreEffettivo: Int get() = effettoPropagazione.punteggio

    /** 1.5.1 (§A12-retro meta' B): che cosa fa questa regola alle RISPOSTE. */
    val effettoPropagazione: EffettoPropagazione
        get() = effettoPropagazioneDi(score, followupScore)

    companion object {
        /**
         * 1.2.12 (audit di sicurezza): lunghezza massima del testo su cui si fa
         * girare una regex. Vedi il commento in [matches].
         */
        const val MAX_TESTO_REGEX = 256 * 1024

        /**
         * M1: default per [followupScore]. Vale -100, cioè esattamente la soglia
         * HIDE di [Thresholds]: chi risponde al troll sparisce dalla vista ma
         * resta nel database.
         *
         * In NeReO `hidden = true` significa già invisibile OVUNQUE — escluso dal
         * conteggio dei non letti (GroupDao.refreshUnreadCount), dal totale del
         * gruppo (allSubscribedWithTotals), dalla ricerca (searchArticles) e
         * dall'elenco discussioni (ArticleDao.byGroup). A schermo il risultato è
         * identico alla cancellazione; cambia solo che è reversibile.
         *
         * Non è una scelta "morbida": è quella che tiene in piedi la riparazione
         * del References troncato. La catena locale è ciò su cui gira la chiusura
         * transitiva di FollowupPropagator — cancellarla significherebbe dover
         * tombare ogni singola risposta per non perdere i discendenti profondi.
         */
        const val DEFAULT_FOLLOWUP_SCORE: Int = -100

        fun wildcardToRegex(w: String): String = buildString {
            for (c in w) when (c) {
                '*' -> append(".*")
                '?' -> append('.')
                in "\\^$.|+()[]{}" -> { append('\\'); append(c) }
                else -> append(c)
            }
        }
    }
}

/**
 * Estrae dall'articolo il valore del campo su cui una regola fa match.
 * null ⇒ campo non valutabile (tipicamente BODY con il corpo non ancora
 * scaricato): la regola va saltata senza alterare il punteggio.
 */
internal fun valoreCampo(article: ScorableArticle, field: FilterField): String? = when (field) {
    FilterField.FROM -> article.from
    FilterField.SUBJECT -> article.subject
    FilterField.MESSAGE_ID -> article.messageId
    FilterField.XPOST -> article.newsgroups
    FilterField.BODY -> article.body
    FilterField.REFERENCES -> article.references
    FilterField.NEWSREADER -> article.newsreader
    FilterField.HEADERS -> article.intestazioni
}

/** Articolo "visto" dal motore filtri (indipendente dal DB). */
data class ScorableArticle(
    val from: String,
    val subject: String,
    val messageId: String,
    val newsgroups: String,  // header Newsgroups: per il match xpost
    val body: String? = null,
    /**
     * M1: header References: (catena degli antenati, message-id separati da
     * spazi). Default "" per non rompere i chiamanti esistenti né i test.
     */
    val references: String = "",
    /**
     * 1.4.4: il lettore di news dichiarato dall'autore — `User-Agent`,
     * `X-Newsreader` o `X-Mailer`, uniti in una riga sola da
     * `MimeUtil.lettoreDiNews`.
     *
     * Volutamente il VALORE GIA' ESTRATTO e non le intestazioni grezze: questo
     * package e' puro Kotlin/JVM, senza dipendenze da Room, da Android ne' dal
     * package `nntp`, ed e' quello che lo rende provabile in `app/src/test`.
     * Estrarre qui avrebbe voluto dire scrivere il TERZO parser di
     * intestazioni del progetto (dopo `crosspostGroups` e `followupTo`, che
     * passano entrambi da `MimeUtil.splitArticle`) — vedi la regola: prima di
     * aggiungere la N-esima copia di una logica, estrarla.
     *
     * Default null = intestazioni non disponibili (corpo non ancora
     * scaricato), quindi regola non valutabile. Stessa semantica di [body].
     */
    val newsreader: String? = null,
    /**
     * 1.5.7 / 175: tutte le intestazioni, «nome: valore» una per riga, nomi in
     * minuscolo (MimeUtil.intestazioniPerFiltri). Null = non disponibili:
     * regola HEADERS non valutabile, stessa semantica di [newsreader].
     */
    val intestazioni: String? = null,
)

/** Esito della valutazione. */
data class ScoreResult(
    val score: Int,
    val matchedRules: List<FilterRule>,
    val action: Action,
) {
    enum class Action { KILL, HIDE, NORMAL, HIGHLIGHT, WATCH }
}

/**
 * Soglie stile slrn:
 *   score <= killThreshold   → KILL   (mai mostrato, mai scaricato il corpo)
 *   score <= hideThreshold   → HIDE   (nascosto ma recuperabile con "mostra tutto")
 *   score >= watchThreshold  → WATCH  (thread seguito, notifiche/ordinamento in testa)
 *   score >= highlightThreshold → HIGHLIGHT (evidenziato)
 */
data class Thresholds(
    val kill: Int = -9999,
    val hide: Int = -100,
    val highlight: Int = 100,
    val watch: Int = 9999,
)

class FilterEngine(
    rules: List<FilterRule>,
    private val thresholds: Thresholds = Thresholds(),
    private val todayEpochDay: Long = java.time.LocalDate.now().toEpochDay(),
) {
    private val active = rules.filter { r ->
        r.enabled && (r.expiresEpochDay == null || r.expiresEpochDay >= todayEpochDay)
    }

    /** Regole applicabili a un dato gruppo (wildmat semplice con *). */
    fun rulesForGroup(group: String): List<FilterRule> = active.filter { r ->
        r.groupWildmat == "*" || Regex(FilterRule.wildcardToRegex(r.groupWildmat)).matches(group)
    }

    /**
     * M1: regole "avvelenanti" attive su [group], cioè quelle che estendono il
     * proprio effetto alle risposte. Se la lista è vuota il Repository salta del
     * tutto la seconda passata: costo zero per chi non usa la funzione.
     */
    fun poisoningRulesForGroup(group: String): List<FilterRule> =
        rulesForGroup(group).filter { it.propaga }

    /**
     * M1: true se [article] fa scattare almeno una regola avvelenante su
     * [group]. È il "seme" della propagazione: da qui in poi lavora
     * [FollowupPropagator] sulla catena References.
     */
    fun isPoisonSeed(article: ScorableArticle, group: String): Boolean =
        poisoningRulesForGroup(group).any { it.matchesArticle(article) }

    fun evaluate(article: ScorableArticle, group: String): ScoreResult {
        var score = 0
        val matched = ArrayList<FilterRule>(4)
        for (rule in rulesForGroup(group)) {
            // BODY assente (corpo non ancora scaricato) ⇒ regola non valutabile,
            // si passa oltre senza alterare il punteggio.
            val value = valoreCampo(article, rule.field) ?: continue
            if (rule.matches(value)) {
                score += rule.score
                matched.add(rule)
            }
        }
        val action = when {
            score <= thresholds.kill -> ScoreResult.Action.KILL
            score <= thresholds.hide -> ScoreResult.Action.HIDE
            score >= thresholds.watch -> ScoreResult.Action.WATCH
            score >= thresholds.highlight -> ScoreResult.Action.HIGHLIGHT
            else -> ScoreResult.Action.NORMAL
        }
        return ScoreResult(score, matched, action)
    }
}

/*
 * 1.5.3 — QUANDO UNA REGOLA SPARISCE, IL SUO EFFETTO SPARISCE CON LEI.
 *
 * IL DIFETTO (collaudo di Marco, 13 ottobre 2026): tolta una regola «Ignora»,
 * i messaggi dell'autore restavano nascosti e tornavano solo RISCARICANDOLI dal
 * server. Cancellare una regola rimetteva in chiaro le sole risposte propagate
 * (registro `poisoned_ids`), e «Applica ora» andava in una direzione sola:
 * nascondeva, ripunteggiava, metteva la stella — non rimetteva mai in chiaro.
 * Lo stesso dall'altra parte: tolta una «Segui il thread», stella E colore
 * restavano, perche' «Applica ora» salta i seguiti e il loro 9999 non si
 * ricalcolava mai; tolta una «Evidenzia», il colore restava fino ad «Applica ora».
 *
 * LA DECISIONE, per ogni messaggio gia' scaricato, sta QUI (Kotlin puro,
 * provata con kotlinc in controlli/filtri). La revisione TOGLIE effetti, non ne
 * aggiunge: aggiungere resta del sync e di «Applica ora».
 *
 *  - NASCOSTO: si rimette in chiaro solo se l'ha nascosto un FILTRO, cioe' se
 *    il punteggio scritto e' <= soglia HIDE, e se nessuna regola rimasta lo
 *    nasconderebbe ancora. ⚠️ Il criterio del punteggio e' un'INFERENZA sul
 *    perche' la riga e' nascosta, verificata sul censimento dei punti che
 *    scrivono `hidden` (NeReO_filtri_revoca_match_diretti.md): esclude i
 *    messaggi sostituiti da un Supersedes, che tengono il punteggio vecchio.
 *  - STELLA: si toglie se e' DELLA REGOLA — punteggio >= soglia WATCH, perche'
 *    la stella messa a mano non scrive il punteggio — e nessuna regola rimasta
 *    darebbe WATCH. Piano A1 scelto da Marco, col limite dichiarato: un
 *    messaggio stellato a mano che ricadeva ANCHE nella regola perde la stella.
 *  - PUNTEGGIO dei visibili: si avvicina a zero (mai si allontana, mai cambia
 *    segno): cosi' tolta una «Evidenzia» il colore se ne va, ma una regola
 *    nuova non agisce di nascosto al salvataggio.
 *  - PROTETTO (nel registro di propagazione di una regola rimasta) o NON
 *    VALUTABILE (una regola rimasta guarda il corpo o le intestazioni, e qui
 *    non ci sono): non si tocca niente.
 */

/** Lo stato di un messaggio gia' scaricato, per [revisioneDopoCambioRegole]. */
data class StatoMessaggio(
    val nascosto: Boolean,
    val seguito: Boolean,
    val punteggio: Int,
    /** Nel registro di propagazione di una regola ancora presente. */
    val protetto: Boolean = false,
    /** false = una regola rimasta guarda un campo che qui manca (corpo, intestazioni). */
    val valutabile: Boolean = true,
)

/** Cosa cambiare. [punteggio] null = il punteggio resta com'e'. */
data class Revisione(
    val rimettiInChiaro: Boolean = false,
    val togliStella: Boolean = false,
    val punteggio: Int? = null,
)

/** null = niente da cambiare. Vedi il commento qui sopra. */
fun revisioneDopoCambioRegole(
    stato: StatoMessaggio,
    esito: ScoreResult,
    soglie: Thresholds = Thresholds(),
): Revisione? {
    if (stato.protetto || !stato.valutabile) return null
    val nascondeAncora = esito.action == ScoreResult.Action.HIDE || esito.action == ScoreResult.Action.KILL
    if (stato.nascosto) {
        if (stato.punteggio > soglie.hide || nascondeAncora) return null
        return Revisione(rimettiInChiaro = true, punteggio = esito.score)
    }
    if (nascondeAncora) return null
    if (stato.seguito && stato.punteggio >= soglie.watch && esito.action != ScoreResult.Action.WATCH) {
        return Revisione(togliStella = true, punteggio = esito.score)
    }
    val p = stato.punteggio
    val n = esito.score
    val versoZero = (p > 0 && n >= 0 && n < p) || (p < 0 && n <= 0 && n > p)
    return if (versoZero) Revisione(punteggio = n) else null
}

/**
 * LE COPIE DELLO STESSO MESSAGGIO SI DECIDONO INSIEME.
 *
 * Uno stesso message-id ha una riga per gruppo: per un crosspost, e — il caso
 * comune, che la 1.5.3/165 non aveva contato — per lo STESSO gruppo letto da
 * piu' server (it.test su cinque server = cinque righe). Nascondere,
 * punteggio e stella si SCRIVONO per message-id, quindi su tutte le copie; ma
 * il registro di propagazione e' PER GRUPPO, e la copia di un altro server
 * spesso non ci sta.
 *
 * ⚠️ Il guasto, misurato sul log di Marco del 14 ottobre: salvata una regola
 * «Segui il thread» estesa alle risposte e premuto «Applica ora», la
 * revisione in coda allo stesso «Applica ora» ha tolto 88 stelle — le copie,
 * sugli altri server, delle risposte appena seguite. Nel gruppo dove la
 * propagazione era passata erano protette dal registro; nelle copie no.
 *
 * Quindi: se UNA copia ha un effetto ancora giustificato, quello STESSO
 * effetto non si toglie dalle altre — nascosto trattiene il nascosto, stella
 * di regola la stella, punteggio il punteggio (167; nella 166 il veto era in
 * blocco, vedi sotto). Fino alla 165 valeva per il solo «rimetti in chiaro».
 */
data class DecisioneRiga(
    val id: Long,
    val messageId: String,
    val nascosto: Boolean,
    val revisione: Revisione?,
    val seguito: Boolean = false,
    val punteggio: Int = 0,
)

fun applicaVetoCrosspost(
    righe: List<DecisioneRiga>,
    soglie: Thresholds = Thresholds(),
): List<DecisioneRiga> {
    // 1.5.3/167: il veto e' PER EFFETTO, non in blocco. Nella 166 una copia
    // con un punteggio qualunque tratteneva tutto delle altre: un +100 ancora
    // giustificato su un server teneva NASCOSTA la copia di un altro, e una
    // stella dall'aspetto manuale (punteggio 0, vedi sotto) teneva la stella
    // della regola su tutte le copie — la foto di Marco.
    val vetoChiaro = HashSet<String>()     // una copia deve restare nascosta
    val vetoStella = HashSet<String>()     // una copia ha una stella DI REGOLA ancora giustificata
    val vetoPunteggio = HashSet<String>()  // una copia visibile ha un punteggio ancora giustificato
    for (r in righe) {
        val rev = r.revisione
        if (r.nascosto && rev?.rimettiInChiaro != true) vetoChiaro.add(r.messageId)
        if (r.seguito && rev == null && r.punteggio >= soglie.watch) vetoStella.add(r.messageId)
        if (!r.nascosto && rev == null && r.punteggio != 0) vetoPunteggio.add(r.messageId)
    }
    val dopoVeto = righe.map { r ->
        val rev = r.revisione ?: return@map r
        val bloccata = when {
            rev.rimettiInChiaro -> r.messageId in vetoChiaro
            rev.togliStella -> r.messageId in vetoStella
            else -> r.messageId in vetoPunteggio
        }
        if (bloccata) r.copy(revisione = null) else r
    }
    // La stella si scrive per message-id: se la regola la toglie da una copia,
    // la toglie anche da quelle che hanno solo l'ASPETTO di una stella manuale
    // (seguite con punteggio sotto la soglia: la copia di un altro server, che
    // fino alla 166 «Applica ora» stellava senza scriverle il punteggio). Il
    // punteggio di quelle copie resta com'e'. Stesso limite del piano A1: una
    // stella messa a mano proprio su una copia di un messaggio della regola
    // se ne va con le altre.
    val stellaTolta = dopoVeto.filter { it.revisione?.togliStella == true }.mapTo(HashSet()) { it.messageId }
    return dopoVeto.map { r ->
        if (r.revisione == null && r.seguito && !r.nascosto && r.punteggio < soglie.watch &&
            r.messageId in stellaTolta
        ) r.copy(revisione = Revisione(togliStella = true)) else r
    }
}
