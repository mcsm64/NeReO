/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Repository: unico punto di contatto tra UI, database e rete NNTP.
 */
package org.openusenet.reader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.openusenet.reader.AppSettings
import org.openusenet.reader.Branding
import org.openusenet.reader.filter.richiedeIntestazioni
import org.openusenet.reader.filter.FilterEngine
import org.openusenet.reader.filter.FilterField
import org.openusenet.reader.filter.FilterRule
import org.openusenet.reader.filter.effettoPropagazioneDi
import org.openusenet.reader.filter.DecisioneRiga
import org.openusenet.reader.filter.StatoMessaggio
import org.openusenet.reader.filter.applicaVetoCrosspost
import org.openusenet.reader.filter.revisioneDopoCambioRegole
import org.openusenet.reader.filter.FollowupMode
import org.openusenet.reader.filter.FollowupPropagator
import org.openusenet.reader.filter.MatchType
import org.openusenet.reader.filter.PropagationInput
import org.openusenet.reader.filter.ScorableArticle
import org.openusenet.reader.thread.Threader
import org.openusenet.reader.thread.referencesInUscita
import org.openusenet.reader.filter.ScoreResult
import org.openusenet.reader.nntp.MimeUtil
import org.openusenet.reader.nntp.NntpClient
import org.openusenet.reader.nntp.NntpException
import org.openusenet.reader.nntp.NntpLog
import org.openusenet.reader.nntp.OverviewLine
import org.openusenet.reader.nntp.IdentitaTls
import org.openusenet.reader.nntp.EsitoFiducia
import org.openusenet.reader.security.PasswordCipher
// 1.4.8 (par. B6b): vedi il "DEBITO DICHIARATO" nel punto d'uso — questo
// simbolo e' Kotlin puro e la sua casa giusta non e' `ui.screens`.
import org.openusenet.reader.text.almenoUnaRigaContinua
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 1.4.2 (build 114) — quanti fallimenti di lettura CONSECUTIVI si perdonano a
 * una lista prima di rinunciare a ri-sottoscriverla.
 *
 * Tre e' un compromesso dichiarato, non un numero magico: abbastanza per
 * superare una scrittura concorrente che dura un battito, poco abbastanza da
 * non trasformare una query rotta sul serio in un giro a vuoto perpetuo. Il
 * contatore si azzera a ogni emissione riuscita — vedi
 * `NewsRepository.difesoDalCursorWindow`.
 */
private const val MAX_TENTATIVI_CURSOR = 3

/**
 * 1.4.2 (build 114) — LA MANIGLIA PER FERMARE UNA DIAGNOSTICA IN CORSO.
 *
 * BIG Umberto, 23 settembre: *«Se tappi su un gruppo, il tasto annulla è in
 * grigio e non funziona, devi chiudere NeReO brutalmente per interrompere»*.
 * Aveva ragione due volte: il pulsante era disabilitato per costruzione, e
 * l'attesa non e' breve — la sua diagnostica LISTGROUP su `it.test` ha
 * impiegato **97.586 ms**, quasi tutti dentro un solo `OVER 1-169588`.
 *
 * PERCHE' NON BASTA `job.cancel()`, ed e' il punto interessante. La
 * cancellazione di una coroutine si nota ai punti di sospensione; una lettura
 * bloccante su socket non ne ha nessuno. Il `cancel()` resterebbe li' ad
 * aspettare che il server finisca di parlare — cioe' i novantasette secondi
 * che si voleva evitare. L'unico modo di interrompere davvero una lettura
 * bloccante e' togliere di sotto cio' che si sta leggendo: si chiude il
 * socket, la `read` fallisce all'istante, e lo srotolamento normale delle
 * eccezioni fa il resto.
 *
 * Da qui la forma: un oggetto che l'interfaccia crea, passa al Repository e
 * tiene in mano. `@Volatile` perche' i due lati vivono su thread diversi —
 * chi presidia sta sul thread di rete, chi annulla arriva dall'interfaccia.
 *
 * L'ordine inverso e' previsto: se l'annullamento arriva mentre la
 * connessione si sta ancora aprendo, [presidia] trova [annullato] gia' vero e
 * chiude subito, invece di lasciar partire un lavoro che nessuno aspetta piu'.
 */
class AnnullamentoDiagnostica {
    @Volatile
    private var client: NntpClient? = null

    /** Vero da quando l'utente ha chiesto di interrompere. Non torna piu' falso. */
    @Volatile
    var annullato: Boolean = false
        private set

    /** Chiamata dal Repository appena la connessione esiste. */
    internal fun presidia(c: NntpClient) {
        client = c
        if (annullato) chiudiOra()
    }

    /** Chiamata dal Repository a lavoro finito: il socket non e' piu' suo. */
    internal fun lasciaIlPresidio() {
        client = null
    }

    /**
     * Chiamata dall'interfaccia. Fuori dal thread principale di proposito:
     * chiudere un socket TLS puo' scrivere un `close_notify`, cioe' rete, e la
     * rete sul thread principale su Android e' un'eccezione in faccia.
     */
    suspend fun annulla() {
        annullato = true
        withContext(Dispatchers.IO) { chiudiOra() }
    }

    private fun chiudiOra() {
        runCatching { client?.close() }
    }
}

/**
 * 1.2.7 (feature A, Marco): segnale per il dialog "connessione non sicura".
 * Quando un sync fallisce perché il server rifiuta l'autenticazione su
 * connessione non cifrata (NntpException 483, la guardia introdotta in 1.2.6),
 * il Repository scrive qui l'id del server; un dialog montato una volta sola in
 * `AppNav` lo osserva e offre le due vie d'uscita (passa a SSL / consenti non
 * cifrata). Un solo punto di stato invece di duplicare il dialog negli ~10 punti
 * che lanciano un sync. `mutableStateOf` è scrivibile da qualunque thread
 * (snapshot), quindi va bene dal thread di rete; la lettura/ricomposizione
 * avviene sul main come per ogni stato Compose.
 */
object AvvisoConnessioneNonSicura {
    val serverId = androidx.compose.runtime.mutableStateOf<Long?>(null)
}

/**
 * 1.2.17 (voce C) — CIFRATURA REALMENTE OTTENUTA, server per server.
 *
 * La configurazione dice solo cosa si è **chiesto**, non cosa si è **ottenuto**:
 * `news.usenet.ovh` ha STARTTLS acceso nelle impostazioni, lo annuncia pure fra
 * le proprie capability, e poi risponde `580 Error initializing TLS` — quindi
 * risulterebbe "cifrato" a chiunque guardi la sola configurazione, mentre parla
 * in chiaro (vedi `NeReO_ovh_starttls_in_chiaro.md`). L'unico dato onesto è
 * quello osservato dopo il collegamento.
 *
 * Da qui esce il segno accanto al nome nell'elenco dei server. Perché un segno
 * **permanente** e non un avviso a ogni sessione: con un provider senza TLS
 * quello stato non cambia mai, e un avviso ricorrente diventerebbe rumore che si
 * impara a ignorare — cioè peggio di niente.
 *
 * In memoria e non nel database di proposito: è un'osservazione, non una
 * configurazione. Dopo un riavvio dell'app resta ignota finché non ci si
 * ricollega, ed è giusto così — "non lo so ancora" è meglio di un dato vecchio.
 */
object StatoCifratura {
    val cifrata = androidx.compose.runtime.mutableStateMapOf<Long, Boolean>()

    /**
     * par. B3 (1.4.11) — STARTTLS SPARITO DA UN SERVER CHE LO OFFRIVA.
     *
     * Richiesta di ObiWan. RFC 4642 par. 5 descrive l'attacco: chi sta in
     * mezzo cancella la riga STARTTLS dalla risposta CAPABILITIES, e il client
     * non prova nemmeno a cifrare. La difesa suggerita dalla RFC e' ricordare
     * che quel server lo offriva e dare un allarme quando non lo offre piu'.
     *
     * true = quel server STARTTLS lo aveva offerto in passato e adesso non lo
     * annuncia. false = tutto regolare (o non si sa ancora). Da qui esce la
     * riga rossa accanto al nome nell'elenco dei server.
     *
     * In memoria come [cifrata], e per la stessa ragione: e' un'osservazione
     * sulla sessione corrente, non una configurazione. La memoria DURATURA
     * («lo offriva») sta invece nelle preferenze, per host:porta, in
     * [org.openusenet.reader.AppSettings.starttlsGiaVisto] — serve che
     * sopravviva alla chiusura dell'app, altrimenti l'allarme non potrebbe
     * mai scattare.
     */
    val starttlsSparito = androidx.compose.runtime.mutableStateMapOf<Long, Boolean>()
}

/**
 * 1.2.15 (voce B) — l'articolo esiste nel nostro database ma NON esiste più sul
 * server (risposta 430 per Message-ID, 423 per numero).
 *
 * Ha un tipo suo, invece di essere una IOException generica con dentro un
 * testo, perché l'interfaccia deve poterlo riconoscere per offrire la via
 * d'uscita giusta — eliminare la copia locale rimasta orfana — senza leggere
 * il messaggio d'errore e sperare che la frase non cambi mai.
 *
 * Non è un guasto: è la situazione normale dopo che un articolo è scaduto sul
 * server o è stato ritirato con successo (dall'autore o da noi stessi, con la
 * funzione Ritira). La riga di riepilogo era già stata scaricata prima, e senza
 * un nuovo sync del gruppo nessuno l'ha ancora tolta di mezzo.
 */
class ArticoloMancanteException(
    val articleId: Long,
    val messageId: String,
) : java.io.IOException(
    "Questo messaggio non è più sul server: probabilmente è scaduto oppure è " +
        "stato ritirato dal suo autore. Nell'elenco resta solo la riga di " +
        "riepilogo scaricata in precedenza.",
)

/**
 * 1.2.15 (voce C) — esito della verifica dopo un `cancel`.
 *
 * Tre valori e non due, di proposito: sul campo abbiamo già visto entrambi i
 * comportamenti (OVH onora i messaggi di controllo, Astraweb li ignora del
 * tutto), e un server che non risponde in modo interpretabile è un terzo caso
 * reale che non va spacciato per uno degli altri due.
 */
enum class EsitoRitiro {
    /** Il server non trova più l'articolo: il ritiro ha funzionato. */
    RITIRATO,

    /** L'articolo è ancora lì: il server ha ignorato la richiesta, o non l'ha ancora elaborata. */
    ANCORA_PRESENTE,

    /** Il messaggio di controllo è partito, ma la verifica non è stata possibile. */
    NON_VERIFICABILE,
}

/**
 * 1.2.16 (Piano B) — esito del ritiro **su un singolo server**.
 *
 * [iniezione] distingue il server su cui il messaggio di controllo è stato
 * effettivamente inviato dagli altri, che vengono soltanto interrogati. È una
 * differenza sostanziale e non cosmetica: sul server di iniezione il ritiro può
 * agire, sugli altri praticamente mai (vedi NeReO_cancel_supersede_campo.md
 * §2.1), quindi un "ancora presente" altrove è la normalità e non un guasto.
 */
data class EsitoRitiroServer(
    val server: String,
    val esito: EsitoRitiro,
    val iniezione: Boolean,
)

/**
 * 1.4.1 — LE INTESTAZIONI PERSONALIZZATE, in un posto solo.
 *
 * Dave Royal, 19 settembre: ha provato a impostare un `Followup-To:` dalle
 * intestazioni personalizzate, in due modi diversi, e non e' mai comparso nel
 * messaggio. Non era un difetto: `followup-to` e' fra le riservate e la riga
 * veniva scartata **di proposito**. Il difetto era il SILENZIO — si scrive una
 * riga valida, si invia, e quella riga non c'e', senza un avviso e senza modo
 * di accorgersene se non ispezionando le intestazioni di un proprio post
 * pubblicato.
 *
 * Perche' un oggetto e non due controlli separati: l'editor deve dire in
 * anticipo **esattamente** cosa l'invio scartera'. Se la regola fosse scritta
 * due volte, il giorno in cui una delle due cambia l'avviso comincia a mentire
 * — ed e' il difetto che questo progetto ha gia' pagato quattro volte.
 * Emettitore e avviso leggono la stessa riga di codice.
 */
object IntestazioniPersonalizzate {

    /**
     * Intestazioni che NeReO scrive da se' e che un testo personalizzato non
     * puo' sovrascrivere.
     *
     * 1.2.12 (audit): aggiunte quelle di CONTROLLO — `control` ritira o crea
     * gruppi, `approved` scavalca la moderazione, `sender`/`followup-to`/
     * `expires` alterano identita' e propagazione. Un testo incollato da fonte
     * esterna non deve poter diventare un messaggio di controllo per
     * distrazione.
     */
    val RISERVATE = setOf(
        "from", "newsgroups", "subject", "references", "message-id", "date",
        "path", "mime-version", "content-type", "content-transfer-encoding",
        "user-agent", "x-no-archive", "face", "supersedes",
        "control", "approved", "sender", "followup-to", "expires",
        "distribution", "injection-info", "injection-date",
    )

    /** "Nome: valore", una per riga. Le righe che non hanno questa forma si ignorano. */
    private val RIGA = Regex("^([A-Za-z][A-Za-z0-9-]*):\\s*(.+)$")

    /** Le coppie (nome, valore) che finiranno davvero nel messaggio. */
    fun valide(testo: String?): List<Pair<String, String>> =
        righe(testo).filter { it.first.lowercase() !in RISERVATE }

    /** I nomi delle intestazioni che verranno SCARTATE perche' riservate. */
    fun scartate(testo: String?): List<String> =
        righe(testo).map { it.first }.filter { it.lowercase() in RISERVATE }

    private fun righe(testo: String?): List<Pair<String, String>> =
        testo?.lines().orEmpty().mapNotNull { riga ->
            RIGA.find(riga.trim())?.destructured?.let { (nome, valore) -> nome to valore }
        }
}

/**
 * 1.4.8 (par. U2) — TESTO LIBERO DENTRO UNA `LIKE`.
 *
 * In SQL `%` vuol dire «qualunque sequenza» e `_` «un carattere qualsiasi».
 * Chi mette testo non controllato dentro una `LIKE` deve neutralizzarli, e la
 * query deve dichiarare `ESCAPE '\'`: le due cose vanno sempre insieme, una
 * senza l'altra non fa niente o fa danno.
 *
 * ERA SCRITTA DENTRO `plonkAuthorByFrom`, dove l'audit 1.2.12 l'aveva messa
 * perche' li' il testo arriva dal campo From di un ESTRANEO e un `Troll <%>`
 * produceva `LIKE '%%%'`, vero su ogni riga, che nascondeva l'intero database.
 * Alla seconda occorrenza — la ricerca, dove il testo lo scrive l'utente per
 * se' e il danno e' solo di correttezza — la si estrae invece di ricopiarla:
 * *prima di aggiungere la N-esima copia di una logica, estrarla.*
 *
 * ⚠️ La barra rovesciata va sostituita per PRIMA, altrimenti si
 * ri-scapperebbero le barre appena inserite.
 */
internal fun testoPerLike(testo: String): String = testo
    .replace("\\", "\\\\")
    .replace("%", "\\%")
    .replace("_", "\\_")

class NewsRepository(
    private val db: AppDatabase,
    private val cipher: PasswordCipher = PasswordCipher.NoOp,
) {

    /** Numero di nuove risposte a miei messaggi trovate nell'ultimo syncGroup. */
    @Volatile
    var lastSyncNewToMe: Int = 0

    /**
     * 1.4.1 (105/F, Dave Royal) — quanti articoli dell'ultimo sync sono stati
     * NASCOSTI dai filtri.
     *
     * Serve perche' "3 nuovi" e la lista rispondono a due domande diverse: il
     * messaggio conta gli articoli INSERITI, la lista e il pallino del gruppo
     * contano i VISIBILI e non letti. Con un filtro attivo divergono, e dire
     * "3 nuovi" quando non ne comparira' nessuno e' un'affermazione falsa, non
     * un'approssimazione. Stesso meccanismo di [lastSyncNewToMe].
     */
    var lastSyncFiltrati: Int = 0

    /** Appoggio fra [elaboraLottoOverview] e il ciclo dei lotti in syncGroup. */
    private var ultimoLottoFiltrati: Int = 0
        private set

    /**
     * 1.2.3 (Dave Royal): "single-flight" sui download dei corpi. Nella vista
     * continua ogni post visibile lancia il proprio fetchBody: con più post NON
     * letti (corpo non in cache) si aprivano più connessioni NNTP simultanee allo
     * stesso server, che molti provider limitano (2-4) → le eccedenti venivano
     * RESETTATE e a schermo compariva "Error: connection reset" al posto del testo
     * (segnalato da Dave). Serializzando i fetch di rete a UNO per volta la
     * concorrenza scende a 1 e il reset sparisce. Il percorso da cache resta fuori
     * dal lock (nessuna attesa per i corpi già scaricati).
     */
    private val bodyFetchMutex = Mutex()

    /**
     * 1.2.9.c (Piano B — NeReO_bug_1.2.9.md, voce X, Dato 4): serializza il
     * sync delle TESTATE ([syncGroup] singolo e [syncGroupList], il cuore
     * comune di syncAllSubscribed/syncServer/syncGroups). Prima di questa
     * versione NESSUNA guardia impediva al sync automatico periodico
     * (MainActivity, timer "Controllo automatico nuovi messaggi") di partire
     * mentre un sync avviato dall'utente era già in corso — entrambi
     * scrivevano sulle stesse tabelle Room senza alcuna serializzazione,
     * una corsa a tempo reale e dimostrata, candidata concreta (abduzione,
     * non ancora confermata da uno stack trace) per il crash "si chiude
     * spesso improvvisamente" segnalato dal collaudo pubblico. Stesso
     * principio di [bodyFetchMutex] (già in uso dal 1.2.3 per i corpi):
     * nel caso peggiore un sync aspetta l'altro invece di accavallarsi —
     * mai un errore IN PIÙ rispetto a prima, solo meno concorrenza.
     */
    private val headerSyncMutex = Mutex()

    /**
     * 1.5.3: salvare, cancellare e «Applica ora» riscrivono in blocco gli
     * stessi messaggi. Due giri sovrapposti (si cancellano due regole di fila)
     * leggerebbero ognuno lo stato a meta' dell'altro: uno alla volta.
     */
    private val filtriMutex = Mutex()

    /**
     * 1.2.9 (voce E, roadmap NNTP, punto 4 — pool leggero di connessioni).
     * fetchBody riapriva una connessione NNTP per OGNI messaggio, anche
     * leggendo lo stesso gruppo di fila — il caso più comune, si scorre un
     * thread messaggio per messaggio. Qui si tiene viva una connessione per
     * server fra chiamate consecutive (già serializzate da [bodyFetchMutex],
     * quindi senza bisogno di altra sincronizzazione). Non è un pool vero e
     * proprio (una sola connessione, non N): il sync (syncGroupList) fa già
     * "una connessione per server" per conto suo, questo estende la stessa
     * idea alla lettura dei corpi. Su qualunque errore la connessione viene
     * scartata SUBITO (mai riusata al giro dopo): nel caso peggiore si torna
     * al comportamento di sempre, una connessione per fetch — mai un errore
     * IN PIÙ rispetto a prima.
     */
    private var corpiClientCache: Pair<Long, NntpClient>? = null
    private var corpiClientUltimoUsoNs: Long = 0
    private val corpiClientIdleMaxMs = 60_000L

    /**
     * 1.2.15 (voce F) — impronta degli interruttori di sicurezza con cui la
     * connessione in cache è stata APERTA.
     *
     * Il difetto che chiude, emerso dal collaudo della 1.2.14: la connessione
     * riusata veniva restituita senza ricostruire l'[NntpClient], che riceve i
     * flag SOLO nel costruttore. Quindi, dopo aver acceso «Consenti certificati
     * non verificabili», letto un messaggio e rispento l'interruttore, la
     * connessione NON verificata restava utilizzabile per un altro minuto: la
     * protezione risultava riattivata nell'interfaccia ma non nei fatti.
     * (Stesso ragionamento per «Consenti autenticazione non cifrata».)
     *
     * Confrontare l'impronta qui, invece di chiudere la connessione dallo
     * schermo di Manutenzione, tiene la correzione tutta dentro il Repository:
     * l'interfaccia non deve sapere che esiste una connessione in cache, e la
     * garanzia vale per QUALUNQUE strada cambi quei flag — ripristino di un
     * backup compreso.
     */
    private data class ImprontaSicurezza(
        val certNonVerificabile: Boolean,
        val authInChiaro: Boolean,
        // 1.2.17 (voce B): anche questa decide COME ci si è collegati, quindi
        // cambiarla deve buttare la connessione aperta con la regola vecchia.
        val autenticazionePigra: Boolean,
    )

    private var corpiClientImpronta: ImprontaSicurezza? = null

    private fun improntaCorrente() = ImprontaSicurezza(
        certNonVerificabile = AppSettings.consentiCertificatoNonVerificabile,
        authInChiaro = AppSettings.consentiAuthNonCifrata,
        autenticazionePigra = AppSettings.autenticazionePigra,
    )

    private fun chiudiClientCorpi() {
        corpiClientCache?.second?.let { runCatching { it.close() } }
        corpiClientCache = null
        corpiClientImpronta = null
    }

    /**
     * Riusa la connessione aperta per fetchBody su [server] se ancora valida,
     * altrimenti ne apre una nuova.
     *
     * 1.2.13: restituisce anche SE la connessione è stata riusata. Serve a
     * [fetchBody] per decidere se un errore di rete meriti un ritentativo
     * (connessione riusata: probabilmente il server l'ha lasciata cadere
     * mentre l'utente leggeva) oppure vada mostrato subito (connessione appena
     * aperta: l'errore è reale). Vedi il commento esteso in fetchBody.
     */
    /**
     * 1.2.18 — PREPARAZIONE UNICA DOPO connect(), per TUTTE le connessioni.
     *
     * Nasce da un difetto mio nella 1.2.17: lo stesso blocco (annota la
     * cifratura ottenuta, decifra la password, autentica) era ripetuto in
     * QUATTRO punti, e l'autenticazione pigra l'ho applicata solo a due —
     * openClient e clientCorpiPer. Restavano ansiosi [postArticle] e
     * [refreshAvailableGroups], cioè proprio l'invio di un messaggio e lo
     * scaricamento dell'elenco gruppi. Il collaudo di Marco su OVH non poteva
     * mostrare la riga "il server chiede l'autenticazione", perché il POST non
     * passava affatto dalla strada pigra.
     *
     * La causa vera non è la svista: è che quel blocco esisteva in quattro
     * copie. Adesso ne esiste UNA, e la prossima regola che riguarda il
     * collegamento si scrive in un posto solo.
     */
    private fun preparaConnessione(client: NntpClient, server: Server) {
        // voce C (1.2.17): la cifratura REALMENTE ottenuta, non quella richiesta.
        StatoCifratura.cifrata[server.id] = client.connessioneCifrata
        // par. B3 (1.4.11): STARTTLS offerto ieri e non piu' oggi — RFC 4642 par. 5.
        osservaStarttls(client, server)
        if (server.username.isNullOrEmpty()) return
        val stored = server.password.orEmpty()
        val pw = cipher.decrypt(stored)
        if (stored.isNotEmpty() && pw.isEmpty()) {
            throw java.io.IOException("Password del server non leggibile: probabilmente persa dopo una reinstallazione o un ripristino. Aprila in Impostazioni → Server e reinseriscila.")
        }
        val allowPlaintext = server.authOverrideNonCifrata ?: AppSettings.consentiAuthNonCifrata
        // voce B (1.2.17): la decifratura resta QUI (locale, e un errore va detto
        // subito); ciò che slitta è solo l'INVIO delle credenziali sulla rete.
        if (AppSettings.autenticazionePigra) {
            client.credenzialiPigre = Triple(server.username, pw, allowPlaintext)
        } else {
            client.authenticate(server.username, pw, allowPlaintext)
        }
    }

    /**
     * par. B3 (1.4.11) — ALLARME ANTI-DECLASSAMENTO STARTTLS, RFC 4642 par. 5.
     *
     * Richiesta di ObiWan. La RFC, alla lettera: «An NNTP client can PARTIALLY
     * protect against these attacks by RECORDING THE FACT that a particular
     * NNTP server offers TLS during one session and GENERATING AN ALARM if it
     * does not appear in the CAPABILITIES response for a later session.»
     *
     * Non costa niente perche' il dato c'e' gia': [NntpClient.connect] legge
     * CAPABILITIES PRIMA di tentare STARTTLS, e la 1.2.18 ha ridotto a UNO i
     * punti di preparazione dopo il collegamento. Nessun comando in piu'.
     *
     * ⚠️ RICORDARE PER ALLARMARE NON E' FIDARSI PER DECIDERE. Il par. 2.1
     * della stessa RFC vieta di FIDARSI di una CAPABILITIES memorizzata
     * («MUST NOT be relied on»), e questa funzione non se ne fida: non fa
     * saltare un tentativo, non presume niente, non tocca il percorso di
     * connessione. Legge, confronta, segnala.
     *
     * ### ⚠️ 1.4.11 / 155 — LA TRAPPOLA NON ERA FUTURA, ERA GIA' SCATTATA
     *
     * Nella build 154 questo commento diceva «per chi verra' dopo: se un
     * giorno si rileggesse CAPABILITIES anche dopo un upgrade riuscito...».
     * Lo si faceva **gia'**: `tentaStartTls` rileggeva e SOVRASCRIVEVA
     * `advertisedCapabilities`. Risultato misurato sul dispositivo di Marco
     * (OVH, sessione `4gz7i` del 12 ottobre 2026): allarme falso su una
     * connessione che si era appena cifrata con successo — il caso peggiore
     * che questa voce potesse produrre.
     *
     * Dalla 155 `advertisedCapabilities` e' la fotografia D'ARRIVO e non viene
     * piu' sovrascritta; la rilettura dopo la cifratura vive in
     * `capabilitiesDopoTls`. ⚠️ **Quel campo non va mai usato qui**: dopo un
     * upgrade riuscito STARTTLS non c'e' per definizione.
     *
     * > Un avvertimento scritto al futuro non controlla il presente.
     *
     * Le tre uscite silenziose sono il cuore della voce. *Un allarme che suona
     * a vuoto non esiste*: `controllo_pacchetti.py`, alla prima stesura,
     * segnalava 115 volte su codice sano, e andava buttato.
     */
    private fun osservaStarttls(client: NntpClient, server: Server) {
        // 1. TLS implicito: la connessione e' cifrata da subito e il server non
        //    ha ragione di annunciare STARTTLS. Allarmare qui farebbe di ogni
        //    server su 563 un falso allarme permanente.
        if (server.ssl) return
        val caps = client.advertisedCapabilities
        // 2. Lista vuota NON vuol dire «STARTTLS rimosso»: il server che non
        //    risponde 101, o l'interrogazione fallita, la lasciano vuota — lo
        //    dice il commento di `advertisedCapabilities`. Assenza di dato.
        if (caps.isEmpty()) return
        // La capability e' l'etichetta «STARTTLS» da sola (RFC 4642 par. 2.1);
        // si guarda il PRIMO token per tollerare un eventuale argomento.
        val offerto = caps.any {
            it.trim().substringBefore(' ').equals("STARTTLS", ignoreCase = true)
        }
        if (offerto) {
            AppSettings.ricordaStarttlsVisto(server.host, server.port)
            StatoCifratura.starttlsSparito[server.id] = false
            return
        }
        // 3. Mai alla prima visita: senza un «lo offriva» registrato non c'e'
        //    niente da confrontare. E' il «partially» della RFC, ed e' un
        //    limite del metodo, non una svista.
        val giaVisto = AppSettings.starttlsGiaVisto(server.host, server.port)
        StatoCifratura.starttlsSparito[server.id] = giaVisto
        if (giaVisto) {
            NntpLog.diag(
                "ALLARME RFC 4642 par. 5: ${server.host}:${server.port} aveva offerto " +
                    "STARTTLS in una sessione precedente e adesso non lo annuncia piu'. " +
                    "Capability ricevute: " + caps.joinToString(", ").take(300)
            )
        }
    }

    // 1.4: SOSPENDIBILE. Da quando nuovoClient legge il pin TLS dal database e
    // annotaIdentitaTls ci scrive l'identita' osservata, questa funzione non
    // puo' piu' essere sincrona. Entrambi i chiamanti erano gia' dentro un
    // withLock/withContext, quindi il cambio non li tocca.
    private suspend fun clientCorpiPer(server: Server): Pair<NntpClient, Boolean> {
        val ora = System.nanoTime()
        val esistente = corpiClientCache
        val impronta = improntaCorrente()
        // 1.2.15 (voce F): l'impronta degli interruttori di sicurezza fa parte
        // delle condizioni di riuso quanto il server e il tempo di inattività.
        // Se l'utente ha cambiato idea nel frattempo, la connessione aperta con
        // le regole vecchie va chiusa, non riusata.
        if (esistente != null && esistente.first == server.id &&
            corpiClientImpronta == impronta &&
            (ora - corpiClientUltimoUsoNs) / 1_000_000 < corpiClientIdleMaxMs
        ) {
            corpiClientUltimoUsoNs = ora
            return esistente.second to true
        }
        if (esistente != null && corpiClientImpronta != impronta) {
            NntpLog.diag("interruttori di sicurezza cambiati: chiudo la connessione aperta con le impostazioni precedenti")
        }
        chiudiClientCorpi()
        val client = nuovoClient(server)
        try {
            client.connect()
        } finally {
            // Anche se l'handshake e' FALLITO: osservare non e' fidarsi, e
            // l'identita' vista serve proprio quando la connessione non passa.
            annotaIdentitaTls(server, client)
        }
        // Qui la connessione NON è dentro un use{}: se la preparazione fallisce
        // va chiusa a mano, altrimenti resta appesa (in cache non ci è ancora
        // entrata, quindi nessun altro la chiuderebbe).
        try {
            preparaConnessione(client, server)
        } catch (e: Exception) {
            runCatching { client.close() }
            throw e
        }
        corpiClientCache = server.id to client
        corpiClientImpronta = impronta
        corpiClientUltimoUsoNs = ora
        return client to false
    }

    val servers get() = db.serverDao().all()
    val subscribedGroups get() = db.groupDao().allSubscribed()
    val subscribedGroupsWithTotals get() = db.groupDao().allSubscribedWithTotals()
    val filterRules get() = db.filterDao().all()

    suspend fun saveServer(server: Server): Long {
        val stored = server.password?.takeIf { it.isNotEmpty() }?.let { cipher.encrypt(it) }
        return db.serverDao().upsert(server.copy(password = stored))
    }
    /**
     * 1.4.2 (build 113) — QUANTO COSTA CANCELLARE QUESTO SERVER.
     *
     * Gruppi e messaggi che se ne andrebbero. Non tocca niente: serve al
     * dialogo di conferma, perche' un numero davanti agli occhi e' l'unica
     * cosa che distingue "cancello questo server di prova" da "cancello sei
     * mesi di archivio".
     */
    suspend fun costoCancellazioneServer(serverId: Long): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val gruppi = db.groupDao().idsPerServer(serverId)
        val articoli = if (gruppi.isEmpty()) 0
                       else gruppi.chunked(500).sumOf { db.articleDao().contaPerGruppi(it) }
        gruppi.size to articoli
    }

    /**
     * 1.4.2 (build 113) — CANCELLARE UN SERVER PORTA VIA CIO' CHE GLI
     * APPARTIENE. Prima non lo faceva, e nessuno se n'era accorto.
     *
     * Fino alla 112 questa funzione era una riga sola, `serverDao().delete(id)`.
     * Le entita' non hanno chiavi esterne — zero `ForeignKey` in tutto il
     * progetto — quindi non c'e' nessun `ON DELETE CASCADE`, e nessuna pulizia
     * esisteva da nessuna parte. Restavano indietro, per sempre: i `groups` di
     * quel server, tutti i loro `articles`, le righe di `available_groups` e i
     * `tls_pins`. Invisibili, irraggiungibili, e contati dalla diagnostica
     * database — che percio' rispondeva "hai 40.000 articoli" a chi non
     * riusciva a vederne uno.
     *
     * Non e' teorico: nella diagnostica LISTGROUP di BIG Umberto compaiono tre
     * righe "? — nomegruppo", e quel `?` e' esattamente un gruppo il cui
     * `serverId` non corrisponde piu' a nessun server (vedi
     * [subscribedGroupsWithServerNames]).
     *
     * ORDINE DELLE CANCELLAZIONI, e non e' indifferente: prima gli articoli,
     * poi i gruppi, poi il resto, infine il server. Cosi' in ogni istante
     * intermedio il database e' in uno stato che il resto dell'app sa leggere;
     * cancellando il server per primo, un sync che partisse nel frattempo
     * troverebbe gruppi senza server, cioe' la condizione che stiamo
     * eliminando.
     *
     * Il modello di come si scrive era gia' nel progetto: [deleteIdentity]
     * sgancia i riferimenti, cancella, e promuove un nuovo predefinito.
     */
    suspend fun eliminaServerEContenuti(serverId: Long) = withContext(Dispatchers.IO) {
        val gruppi = db.groupDao().idsPerServer(serverId)
        if (gruppi.isNotEmpty()) {
            gruppi.chunked(500).forEach { db.articleDao().deleteByGroups(it) }
        }
        db.groupDao().deleteByServer(serverId)
        db.availableGroupDao().clear(serverId)
        db.tlsPinDao().cancellaPerServer(serverId)
        db.serverDao().delete(serverId)
    }

    // ------------------------------------------------------------- identità

    /** 1.2.9 (multi-identità): vedi [Identity] per il perché di questa tabella. */
    val identities get() = db.identityDao().all()
    suspend fun identitiesOnce(): List<Identity> = db.identityDao().allOnce()
    suspend fun identityById(id: Long): Identity? = db.identityDao().byId(id)

    /**
     * 1.3.2 (Dave Royal, RFC 5536): riscrive lo User-Agent delle identità che
     * hanno ancora ESATTAMENTE un nostro vecchio predefinito. Restituisce
     * quante righe sono state aggiornate. Vedi App.migraUserAgentIdentita.
     */
    suspend fun sostituisciUserAgent(vecchio: String, nuovo: String): Int =
        db.identityDao().sostituisciUserAgent(vecchio, nuovo)

    /**
     * Salva un'identità. Se [Identity.isDefault] è true, toglie prima il flag
     * a tutte le altre: al più UNA può essere la ricaduta dei server senza
     * assegnazione esplicita (vedi [resolvedIdentity]).
     */
    suspend fun saveIdentity(identity: Identity): Long {
        if (identity.isDefault) db.identityDao().clearDefault()
        return db.identityDao().upsert(identity)
    }

    /**
     * Cancella un'identità e slega i server che la usavano (ricadranno sulla
     * default). Se era LEI la default, ne promuove un'altra a caso — non
     * lasciare la app senza nessuna identità di ricaduta.
     */
    suspend fun deleteIdentity(id: Long) {
        val era = db.identityDao().byId(id) ?: return
        db.identityDao().unassignFromServers(id)
        db.identityDao().delete(id)
        if (era.isDefault) {
            db.identityDao().allOnce().firstOrNull()?.let { saveIdentity(it.copy(isDefault = true)) }
        }
    }

    suspend fun assignIdentityToServer(serverId: Long, identityId: Long?) =
        db.serverDao().setIdentityId(serverId, identityId)

    /**
     * L'identità da usare per pubblicare su [serverId]: quella assegnata
     * esplicitamente al server, altrimenti quella marcata default, altrimenti
     * (caso limite: nessuna identità esiste ancora, prima che la migrazione
     * al primo avvio sia passata) un'identità vuota "di cortesia" — mai un
     * crash in ComposeScreen per questo.
     */
    suspend fun resolvedIdentity(serverId: Long?): Identity {
        val viaServer = serverId?.let { db.serverDao().byId(it)?.identityId }?.let { db.identityDao().byId(it) }
        return viaServer ?: db.identityDao().defaultOnce() ?: Identity(nickname = "", isDefault = true)
    }

    /**
     * Apre una connessione NNTP autenticata al server e la passa al blocco,
     * chiudendola al termine. Centralizza connect + authenticate + gestione
     * della password illeggibile, prima duplicati in fetchBody e postArticle.
     *
     * 1.2.9 (voce E, roadmap NNTP): STARTTLS e l'override per-server dell'auth
     * non cifrata vengono da [server] stesso, non più solo dall'impostazione
     * globale — [server.authOverrideNonCifrata] vince quando valorizzato,
     * altrimenti si ricade sul comportamento globale di sempre.
     */
    private suspend fun <T> openClient(server: Server, blocco: suspend (NntpClient) -> T): T =
        nuovoClient(server).use { client ->
            try {
                client.connect()
            } finally {
                annotaIdentitaTls(server, client)
            }
            preparaConnessione(client, server)
            blocco(client)
        }

    // ------------------------------------------------- fiducia TLS (1.4, TOFU)

    /**
     * L'UNICO punto in cui nasce un [NntpClient].
     *
     * Fino alla 1.3.7 quella chiamata era scritta in QUATTRO posti, con gli
     * stessi cinque argomenti copiati a mano. Aggiungere qui la fiducia TLS
     * avrebbe significato quattro occasioni di dimenticarsene — ed e'
     * esattamente la forma di difetto che questo progetto ha gia' pagato con
     * l'autenticazione pigra (1.2.17: applicata a due copie su quattro) e con
     * `esporta` (registrata in tre posti su quattro).
     *
     * Il pin si applica solo se e' TRUSTED **e** host e porta coincidono con
     * quelli osservati all'approvazione: un pin non si eredita cambiando
     * endpoint, e due righe server che puntano allo stesso host restano
     * distinte.
     */
    private suspend fun nuovoClient(server: Server, readTimeoutMs: Int = 30_000): NntpClient {
        val pin = db.tlsPinDao().perServer(server.id)
        val approvato = pin
            ?.takeIf { it.state == TlsPinStato.TRUSTED && it.host == server.host && it.port == server.port }
            ?.spkiSha256
        return NntpClient(
            server.host, server.port, server.ssl,
            startTls = server.startTls,
            consentiCertificatoNonVerificabile = AppSettings.consentiCertificatoNonVerificabile,
            pinApprovato = approvato,
            eccezioneScadenza = server.eccezioneScadenza,
            readTimeoutMs = readTimeoutMs,
        )
    }

    /**
     * Annota l'identita' TLS vista sull'ultimo handshake — RIUSCITO O NO.
     *
     * Tre casi, e la differenza fra loro e' tutta la funzione:
     *
     * 1. **Pin TRUSTED che coincide** → si aggiornano solo i dati del
     *    certificato osservato (date, subject, issuer). Serve all'avviso di
     *    scadenza: al rinnovo, la chiave puo' restare la stessa e cambiare solo
     *    le date.
     * 2. **Pin TRUSTED che NON coincide** → non si tocca NIENTE. Il blocco
     *    l'ha gia' fatto il TrustManager; qui sovrascrivere significherebbe
     *    cancellare cio' che l'utente aveva approvato, cioe' il solo dato che
     *    gli permette di capire cosa e' cambiato. **Il pin non si aggiorna mai
     *    da solo.**
     * 3. **Nessun pin, o PENDING** → si registra (o si aggiorna) l'osservazione
     *    come PENDING. PENDING **non e' fiducia**: e' una fotografia in attesa
     *    che un umano la guardi.
     */
    private suspend fun annotaIdentitaTls(server: Server, client: NntpClient) {
        val ident = client.identitaTls ?: return
        val esistente = db.tlsPinDao().perServer(server.id)
        val ora = System.currentTimeMillis()

        if (esistente != null && esistente.state == TlsPinStato.TRUSTED) {
            val stessoEndpoint = esistente.host == server.host && esistente.port == server.port
            if (!stessoEndpoint || esistente.spkiSha256 != ident.spkiSha256) return // caso 2
            db.tlsPinDao().upsert(                                                  // caso 1
                esistente.copy(
                    certificateSha256 = ident.certificatoSha256,
                    subject = ident.subject,
                    issuer = ident.issuer,
                    notBefore = ident.notBefore,
                    notAfter = ident.notAfter,
                    observedAt = ora,
                )
            )
            return
        }

        db.tlsPinDao().upsert(                                                      // caso 3
            TlsPin(
                id = esistente?.id ?: 0,
                serverId = server.id,
                host = server.host,
                port = server.port,
                spkiSha256 = ident.spkiSha256,
                certificateSha256 = ident.certificatoSha256,
                subject = ident.subject,
                issuer = ident.issuer,
                notBefore = ident.notBefore,
                notAfter = ident.notAfter,
                // la PRIMA volta che questa identita' e' stata vista
                observedAt = if (esistente?.spkiSha256 == ident.spkiSha256) esistente.observedAt else ora,
                approvedAt = null,
                // 1.4: PENDING solo se la validazione normale NON e' passata.
                // Un server in regola si registra come VALIDA: i suoi dati
                // servono all'avviso di scadenza, ma non c'e' niente da
                // approvare e non va chiesto (rilievo di Marco su Paganini,
                // che e' tornato regolare dopo il rinnovo di Ivo Gandolfo).
                state = if (ident.esito == EsitoFiducia.OK) TlsPinStato.VALIDA else TlsPinStato.PENDING,
            )
        )
    }

    /**
     * 1.4 — SONDA TLS DEDICATA: che identita' presenta QUESTO server adesso.
     *
     * Connessione a se' stante, e le due cose che NON fa sono la ragione per
     * cui esiste:
     *  - **non autentica**: non passa da `preparaConnessione`, quindi nessuna
     *    credenziale entra in gioco per una semplice occhiata;
     *  - **non scrive niente**: l'osservazione non diventa un PENDING nuovo. La
     *    sonda INFORMA; a registrare e' il collegamento vero. Se sovrascrivesse,
     *    guardare cambierebbe cio' che si guarda — lo stesso difetto del
     *    selettore crosspost che Dave Royal ha trovato.
     *
     * L'identita' si legge anche quando `connect()` FALLISCE: l'handshake e'
     * gia' avvenuto, la catena e' gia' stata vista, ed e' proprio il caso in cui
     * serve di piu'.
     */
    suspend fun sondaTls(serverId: Long): IdentitaTls? = withContext(Dispatchers.IO) {
        val server = db.serverDao().byId(serverId) ?: return@withContext null
        val client = nuovoClient(server)
        try {
            runCatching { client.connect() }
            client.identitaTls
        } finally {
            runCatching { client.close() }
        }
    }

    /** L'identita' TLS registrata per un server (PENDING o TRUSTED), se c'e'. */
    suspend fun pinTls(serverId: Long): TlsPin? = db.tlsPinDao().perServer(serverId)

    /** 1.4: tutte le identita' registrate — l'elenco server mostra le scadenze vicine. */
    suspend fun pinTlsTutti(): List<TlsPin> = db.tlsPinDao().tuttiOnce()

    /** L'identita' TLS di un server, come flusso per l'interfaccia. */
    fun pinTlsFlow(serverId: Long) = db.tlsPinDao().perServerFlow(serverId)

    /**
     * Approva l'identita' attualmente registrata: PENDING → TRUSTED.
     *
     * Non accetta uno SPKI dall'esterno di proposito: si approva **quello che e'
     * stato osservato e mostrato**, non un valore che qualcuno passa per
     * argomento. Restituisce false se non c'e' nulla da approvare.
     */
    suspend fun approvaPinTls(serverId: Long): Boolean {
        val pin = db.tlsPinDao().perServer(serverId) ?: return false
        db.tlsPinDao().upsert(pin.copy(state = TlsPinStato.TRUSTED, approvedAt = System.currentTimeMillis()))
        return true
    }

    /** Revoca: la riga sparisce e si torna alla validazione normale. */
    suspend fun revocaPinTls(serverId: Long) = db.tlsPinDao().cancellaPerServer(serverId)

    /** Ultimo Message-ID nell'header References (il padre diretto). "" se assente. */
    private fun lastReference(references: String): String =
        Regex("<[^>]+>").findAll(references).lastOrNull()?.value ?: ""

    /**
     * 1.4.6 (§A12): marca «seguito» ogni articolo del lotto che risponda, a
     * qualunque profondita', a un messaggio gia' seguito.
     *
     * Chi era gia' seguito per conto suo (una regola dei filtri con azione
     * WATCH) resta tale: qui si aggiunge, non si sovrascrive.
     *
     * I blocchi da 500 sono il limite di variabili di SQLite, stessa misura
     * gia' usata da `contaPerGruppi`. Gli id vengono da
     * `Threader.idNelleReferences`, che applica il tetto di sicurezza del
     * 1.2.12: un References da 400 KB non diventa una lista di parametri da
     * 400 KB.
     */
    private suspend fun ereditaSeguitoDalThread(articles: List<Article>): List<Article> {
        val riferiti = articles
            .filterNot { it.watched }
            .flatMap { Threader.idNelleReferences(it.references) }
            .distinct()
        if (riferiti.isEmpty()) return articles
        val seguiti = riferiti.chunked(500)
            .flatMap { db.articleDao().messageIdSeguitiFra(it) }
            .toHashSet()
        if (seguiti.isEmpty()) return articles
        return articles.map { a ->
            if (a.watched) a
            else if (Threader.idNelleReferences(a.references).any { it in seguiti }) a.copy(watched = true)
            else a
        }
    }

    // ---------------------------------------------------------------- bozze
    val drafts get() = db.draftDao().all()
    val draftCount get() = db.draftDao().count()

    suspend fun draftById(id: Long): Draft? = db.draftDao().byId(id)

    /** Salva (o aggiorna) una bozza e ne restituisce l'id. */
    suspend fun saveDraft(draft: Draft): Long = withContext(Dispatchers.IO) {
        db.draftDao().upsert(draft.copy(updatedEpoch = System.currentTimeMillis() / 1000))
    }

    suspend fun deleteDraft(id: Long) = withContext(Dispatchers.IO) {
        if (id > 0) db.draftDao().delete(id)
    }

    /**
     * 1.4.2 (build 114) — LA DIFESA CONTRO IL CursorWindow, IN UN POSTO SOLO.
     *
     * Fino alla 113 questa logica era scritta in TRE copie (`articles`,
     * `searchArticles`, `searchArticlesInGroup`), e la storia di come ci sia
     * arrivata e' istruttiva: nata in `byGroup` nella 1.2.10, e ricopiata
     * nelle altre due nella 1.2.11 quando ci si accorse che erano scoperte.
     * Tre copie della stessa riga sono tre occasioni di correggerne due —
     * esattamente il difetto che la 1.3 ha pagato in pubblico. Ora e' una.
     *
     * COSA CAMBIA RISPETTO ALLA 113, ed e' il motivo di questa build.
     * Il log di BIG Umberto del 23 settembre ha fatto quello che la
     * diagnostica della 104 doveva fargli fare, e ha mostrato il difetto per
     * intero:
     *
     *     D: lista gruppo 36: ricevute 2301 righe
     *     D: lista gruppo 36: ricevute 2301 righe
     *     ... sync: 4 articoli nuovi, sessione chiusa senza errori ...
     *     D: lista gruppo 36: LETTURA FALLITA (IllegalStateException:
     *        Couldn't read row 627, col 0 from CursorWindow...)
     *
     * Il guaio non era il fallimento della lettura — quello e' raro e la
     * lista aveva gia' dati buoni da mostrare. Il guaio era il `.catch`:
     * l'operatore `catch` di Flow e' TERMINALE. Dopo la sua `emit`, la Flow
     * e' COMPLETATA, e Room non ha piu' nessuno a cui notificare le
     * invalidazioni successive. Da quel momento la schermata era cieca fino
     * a quando non veniva ricostruita: la "lista sorda" che Umberto segnalava
     * da settembre, con i messaggi nuovi nel database, il pallino del gruppo
     * che li contava, e la lista aperta che non li mostrava mai.
     *
     * La cura e' `retryWhen`, che invece RI-SOTTOSCRIVE il flusso di monte.
     * Room riesegue la query con un cursore nuovo ed emette subito lo stato
     * corrente: la lista si ripara da sola entro un istante, invece di
     * restare sorda per sempre. Il `.catch` resta in coda, ma solo come rete
     * d'emergenza dopo [MAX_TENTATIVI_CURSOR] fallimenti consecutivi — senza
     * un tetto, una query che fallisse sempre girerebbe in tondo a vuoto.
     *
     * Il contatore si azzera a ogni emissione riuscita: il budget e' "tre
     * fallimenti di fila", non "tre fallimenti in tutta la vita della
     * schermata".
     *
     * @param etichetta come questa lista si chiama nel log diagnostico.
     * @param misura eseguita UNA volta, al primo fallimento, per scrivere nel
     *        log quanto pesa davvero cio' che si stava leggendo (vedi
     *        [PesoLista]). Null dove non ha senso — la ricerca globale non ha
     *        un gruppo di cui misurare il peso.
     */
    private fun Flow<List<Article>>.difesoDalCursorWindow(
        etichetta: String,
        misura: (suspend () -> String)? = null,
    ): Flow<List<Article>> {
        var ultimaListaValida: List<Article> = emptyList()
        var falliteDiFila = 0
        return this
            .onEach {
                ultimaListaValida = it
                falliteDiFila = 0
                // 1.4.1 (build 104) — DIAGNOSTICA: dice se la Flow smette di
                // emettere, e se prima di smettere e' passata di qui. E' la
                // riga che ha permesso di vedere il difetto invece di dedurlo.
                NntpLog.diag("$etichetta: ricevute ${it.size} righe")
            }
            .retryWhen { e, _ ->
                if (e !is IllegalStateException || falliteDiFila >= MAX_TENTATIVI_CURSOR) return@retryWhen false
                falliteDiFila++
                // La misura si paga UNA volta sola, al primo fallimento: tre
                // volte la stessa query, con lo stesso risultato, sarebbe
                // rumore nel log e lavoro sul database proprio mentre il
                // database sta gia' dando segni di fatica.
                val peso = if (falliteDiFila == 1) misura?.invoke()?.let { " $it" }.orEmpty() else ""
                NntpLog.diag(
                    "$etichetta: LETTURA FALLITA (${e.javaClass.simpleName}: ${e.message}). " +
                        "Ri-sottoscrizione, tentativo $falliteDiFila di $MAX_TENTATIVI_CURSOR.$peso",
                )
                // Una pausa crescente: se l'innesco e' una scrittura concorrente,
                // il tempo e' l'unico ingrediente che manca.
                delay(120L * falliteDiFila)
                true
            }
            .catch { e ->
                if (e is IllegalStateException) {
                    NntpLog.diag(
                        "$etichetta: LETTURA FALLITA $MAX_TENTATIVI_CURSOR volte di fila, si rinuncia. " +
                            "ATTENZIONE: da qui in poi questa lista NON ricevera' " +
                            "piu' aggiornamenti finche' la schermata non viene ricreata.",
                    )
                    emit(ultimaListaValida)
                } else {
                    throw e
                }
            }
    }

    /**
     * La misura di [PesoLista] gia' pronta da scrivere nel log, in kB.
     *
     * La finestra di `SQLiteCursor` e' da 2048 kB: e' il numero rispetto al
     * quale va letto il totale, ed e' scritto nella riga apposta perche' chi
     * legge il log non debba ricordarselo.
     */
    private suspend fun misuraPesoLista(groupId: Long): String = runCatching {
        val p = db.diagnosticaDao().pesoListaGruppo(groupId)
        val kb = { n: Long -> (n + 512) / 1024 }
        "Peso misurato: ${p.righe} righe, colonne di lista ~${kb(p.leggere)} kB, " +
            "colonne mai mostrate (corpo+intestazioni+faccia) ~${kb(p.pesanti)} kB, " +
            "totale ~${kb(p.leggere + p.pesanti)} kB su una finestra da 2048 kB."
    }.getOrElse { "Peso non misurabile (${it.javaClass.simpleName})." }

    /**
     * Ricerca testuale in tutta la cache locale (tutti i gruppi).
     *
     * Piano B esteso (NeReO_bug_1.2.9.md, Dato 5, 5 settembre 2026): questa
     * Flow condivide con `articles(...)` qui sotto lo stesso rischio di
     * `IllegalStateException` da CursorWindow — `ArticleDao` ha tre metodi
     * `Flow<List<Article>>` (`byGroup`, `searchArticles`,
     * `searchArticlesInGroup`), e dalla 114 tutti e tre passano dalla stessa
     * difesa, [difesoDalCursorWindow].
     *
     * Niente misura del peso: la ricerca e' globale e per giunta gia' limitata
     * a 500 righe, quindi non c'e' un gruppo di cui misurare alcunche'.
     *
     * 1.4.8 (par. U2, BIG Umberto): il testo digitato passa da [testoPerLike].
     * Prima finiva grezzo dentro una `LIKE`, quindi `_` e `%` si comportavano
     * da metacaratteri: cercare `_114` trovava anche « 114», e un underscore
     * non si poteva cercare affatto. La clausola `ESCAPE` sta nella query.
     */
    fun searchArticles(query: String): Flow<List<Article>> =
        db.articleDao().searchArticles(testoPerLike(query))
            .difesoDalCursorWindow("ricerca globale")

    /** Come [searchArticles] ma ristretta a un solo gruppo — stessa difesa. */
    fun searchArticlesInGroup(groupId: Long, query: String): Flow<List<Article>> =
        db.articleDao().searchArticlesInGroup(groupId, testoPerLike(query))
            .difesoDalCursorWindow("ricerca nel gruppo $groupId")

    /** Wishlist beta12 (Bingo3331): serve per la ricerca "per thread" in SearchScreen. */
    suspend fun articleByMessageId(messageId: String) = db.articleDao().byMessageId(messageId)

    /** Nome del gruppo per id (per mostrare la provenienza nei risultati). */
    suspend fun groupNameById(id: Long): String? = db.groupDao().byId(id)?.name

    /** Nome del server per id (1.2.7: lo mostra il dialog "connessione non sicura"). */
    suspend fun serverNameById(id: Long): String? = db.serverDao().byId(id)?.name

    /**
     * Piano B, voce X (NeReO_bug_1.2.9.md, Dato 5, 5 settembre 2026): la Flow di Room
     * dietro questa query può, in casi rari, sollevare una
     * `IllegalStateException` del CursorWindow quando la sua raccolta viene
     * cancellata (uscita dal gruppo, Compose smonta la schermata) proprio
     * mentre un sync concorrente scrive nuove righe nella stessa tabella
     * `articles` (es. "aggiorna tutti" dalla schermata Server) — un crash
     * reale, capitato a un utente del collaudo pubblico (traccia con
     * `ArticleDao_Impl$29.call` dentro `CoroutinesRoom.createFlow`, con la
     * coroutine collezionatrice già in stato "Cancelling").
     *
     * 1.4.2 (build 114) — QUESTA PARTE NON È PIÙ UN'IPOTESI. Il log di BIG
     * Umberto del 23 settembre 2026 contiene l'eccezione per intero
     * (`Couldn't read row 627, col 0 from CursorWindow`, su una lista di 2301
     * righe, subito dopo un sync che aveva inserito 4 articoli): che la
     * "lista sorda" **sia** questo fallimento è ora un dato, non un sospetto.
     *
     * Resta abduttivo soltanto l'INNESCO — peso delle righe oppure scrittura
     * concorrente — e a scioglierlo è la misura che [misuraPesoLista] scrive
     * nel log al primo fallimento, invece di un'altra induzione.
     *
     * La difesa vive in [difesoDalCursorWindow], dove sta anche il motivo per
     * cui dalla 114 è `retryWhen` e non più `catch`.
     */
    fun articles(groupId: Long, showHidden: Boolean, unreadOnly: Boolean = false): Flow<List<Article>> =
        db.articleDao().byGroup(groupId, showHidden, unreadOnly)
            .difesoDalCursorWindow(
                etichetta = "lista gruppo $groupId",
                misura = { misuraPesoLista(groupId) },
            )

    suspend fun articleById(id: Long): Article? = db.articleDao().byId(id)

    suspend fun groupById(id: Long): Group? = db.groupDao().byId(id)

    // ---------------------------------------------------------------- sync

    /**
     * Scarica le nuove headline di un gruppo (sync incrementale su numeri
     * articolo), applica i filtri e salva. Ritorna quanti articoli nuovi.
     */
    suspend fun syncGroup(groupId: Long, maxHeadlines: Int = AppSettings.maxHeadersSync): Int = withContext(Dispatchers.IO) {
        // 1.2.9.c (Piano B, vedi headerSyncMutex): un solo sync di testate per
        // volta, che sia questo gruppo singolo, un syncGroupList in corso, o il
        // timer automatico — mai due in corsa sullo stesso database.
        headerSyncMutex.withLock {
            val group = db.groupDao().byId(groupId) ?: return@withLock 0
            val server = db.serverDao().byId(group.serverId) ?: return@withLock 0
            val engine = buildEngine()
            // 1.2.7 (Marco): se il server rifiuta l'auth su connessione non cifrata
            // (483, la guardia di sicurezza della 1.2.6), non rilancio un errore
            // secco: segnalo al dialog dedicato (feature A) che offre "passa a SSL"
            // o "consenti non cifrata", e restituisco 0. Gli altri errori risalgono
            // come prima.
            try {
                openClient(server) { client -> syncGruppoConClient(client, group, engine, maxHeadlines) }
            } catch (e: NntpException) {
                if (e.code == 483) {
                    AvvisoConnessioneNonSicura.serverId.value = server.id
                    0
                } else throw e
            }
        }
    }

    /**
     * Cuore del sync di UN gruppo su un client GIÀ connesso e autenticato.
     *
     * Estratto da syncGroup (richiesta Marco/"A"): così syncAllSubscribed può
     * riusare UNA sola connessione per server per tutti i suoi gruppi, invece
     * di riaprire socket + handshake TLS + AUTHINFO per ognuno — era la lentezza
     * che il log ha reso evidente (28 gruppi = 28 login). Qui NON si apre né
     * chiude la connessione: se ne occupa il chiamante (openClient).
     */
    private suspend fun syncGruppoConClient(
        client: NntpClient,
        group: Group,
        engine: FilterEngine,
        maxHeadlines: Int,
    ): Int {
            lastSyncNewToMe = 0
            lastSyncFiltrati = 0
            val stats = client.group(group.name)
            // maxHeadlines <= 0 ⇒ nessun limite: si parte dal primo articolo disponibile
            val from = if (maxHeadlines <= 0) {
                maxOf(group.lastFetchedArticleNumber + 1, stats.low)
            } else {
                maxOf(group.lastFetchedArticleNumber + 1, stats.low, stats.high - maxHeadlines + 1)
            }
            if (from > stats.high) return 0

            /*
             * 1.4 (Bingo3331, 17 settembre 2026) — L'OVERVIEW SI CHIEDE A FETTE.
             *
             * La sua segnalazione: con "tutti i messaggi" su un server ad alta
             * ritenzione (Astraweb, alt.free.newservers) non si scaricava NULLA
             * e usciva
             *     "Response block exceeded the 67108864 byte limit: aborted"
             * cioe' il nostro tetto di MAX_BLOCCO_BYTE (NntpClient), nato
             * dall'audit della 1.2.12 per non morire di OutOfMemoryError.
             *
             * Il tetto ha fatto il suo mestiere — il commento sopra la costante
             * prevedeva testualmente proprio questo caso — ma la funzione
             * "tutti i messaggi", che offriamo noi in Manutenzione, su quei
             * server non funzionava.
             *
             * LA CAUSA NON ERA IL NUMERO. Era che chiedevamo
             *     client.over(from, stats.high)
             * cioe' l'INTERO intervallo in un colpo solo, e la risposta veniva
             * letta tutta in memoria. Col limite impostato l'intervallo e'
             * corto (300 righe); con "tutti" e' il gruppo intero, milioni di
             * righe di overview.
             *
             * ALZARE IL TETTO NON E' UNA CORREZIONE, E' UN RINVIO: sposta il
             * muro e rimette in gioco l'OutOfMemoryError che il tetto esiste per
             * impedire. Si chiede a fette, si scrive ogni fetta, si avanza il
             * segnalibro. Tre effetti invece di uno: memoria costante, ripresa
             * dopo un'interruzione, e — il giorno che si vorra' — un
             * avanzamento da mostrare.
             *
             * PERCHE' 10.000. E' un intervallo di NUMERI d'articolo, non di
             * articoli: quanti ne tornino davvero dipende dai buchi del gruppo.
             * Nel caso peggiore (nessun buco, righe di overview grasse da ~1 kB)
             * sono ~10 MB per blocco, comodamente sotto i 64 MB del tetto, che
             * resta dov'e' a fare la guardia al caso patologico vero. Piu'
             * piccolo moltiplicherebbe i tempi di andata e ritorno su gruppi
             * enormi; piu' grande riavvicinerebbe il muro.
             */
            val regolePropaganti = engine.poisoningRulesForGroup(group.name)
            var totaleArticoli = 0
            var totaleNuoviPerMe = 0
            var lo = from
            while (lo <= stats.high) {
                val hi = minOf(lo + PASSO_OVERVIEW - 1, stats.high)
                val (inseriti, nuoviPerMe) = elaboraLottoOverview(
                    group, engine, client.over(lo, hi), regolePropaganti, hi,
                )
                totaleArticoli += inseriti
                totaleNuoviPerMe += nuoviPerMe
                lastSyncFiltrati += ultimoLottoFiltrati
                lo = hi + 1
            }
            lastSyncNewToMe = totaleNuoviPerMe
            db.groupDao().refreshUnreadCount(group.id)
            // housekeeping: override del gruppo se presente, altrimenti valore globale
            // beta19 (Roxy): se il gruppo ha una conservazione a NUMERO DI
            // DISCUSSIONI, quella ha la precedenza sui giorni — così un gruppo
            // poco frequentato non si svuota col passare del tempo.
            val keepThreads = group.purgeKeepThreads ?: 0
            if (keepThreads > 0) {
                db.articleDao().keepOnlyLastThreadsInGroup(group.id, keepThreads)
            } else {
                val purgeDays = group.purgeDays ?: AppSettings.autoPurgeDays
                if (purgeDays > 0) {
                    val cutoff = System.currentTimeMillis() / 1000 - purgeDays.toLong() * 86_400
                    db.articleDao().deleteReadOlderThanInGroup(group.id, cutoff)
                }
            }
            return totaleArticoli
    }


    /**
     * Un LOTTO di overview: lo valuta coi filtri, lo inserisce, propaga le
     * regole M1 e porta il segnalibro del gruppo a [finoA].
     *
     * Estratta da [syncGruppoConClient] il 17 settembre 2026, quando la
     * sincronizzazione e' passata a chiedere l'overview A FETTE (vedi la nota
     * lunga la' sopra). Il corpo e' lo stesso di prima, parola per parola: qui
     * cambia CHI lo chiama e QUANTE volte, non cosa fa.
     *
     * Ritorna (articoli inseriti, risposte a messaggi miei) — il chiamante li
     * somma su tutti i lotti.
     */
    private suspend fun elaboraLottoOverview(
        group: Group,
        engine: FilterEngine,
        over: List<OverviewLine>,
        regolePropaganti: List<FilterRule>,
        finoA: Long,
    ): Pair<Int, Int> {
        val semiNuovi = ArrayList<PoisonedId>()

        val articles = over.mapNotNull { o ->
            val scorable = ScorableArticle(
                o.from, o.subject, o.messageId,
                newsgroups = group.name,
                references = o.references,
            )
            val result = engine.evaluate(scorable, group.name)

            // M1: l'articolo e' un "seme"? Va registrato PRIMA dell'eventuale
            // KILL: se lo si registrasse dopo, il post del troll sarebbe gia'
            // stato scartato e la sua discendenza futura non avrebbe piu'
            // nulla a cui agganciarsi. E' il bug che rende la funzione
            // apparentemente corretta al primo sync e rotta dal secondo.
            for (regola in regolePropaganti) {
                if (regola.matchesArticle(scorable)) {
                    semiNuovi.add(
                        PoisonedId(
                            messageId = o.messageId,
                            groupId = group.id,
                            epoch = parseDate(o.date),
                            ruleId = regola.id,
                            seed = true,
                        )
                    )
                }
            }

            if (result.action == ScoreResult.Action.KILL) return@mapNotNull null
            Article(
                groupId = group.id,
                number = o.number,
                messageId = o.messageId,
                subject = o.subject,
                fromField = o.from,
                dateEpoch = parseDate(o.date),
                references = o.references,
                bytes = o.bytes,
                lineCount = o.lines,
                score = result.score,
                hidden = result.action == ScoreResult.Action.HIDE,
                watched = result.action == ScoreResult.Action.WATCH,
            )
        }
        // rileva le risposte ai miei messaggi: il genitore (ultimo id in
        // References) è un articolo scritto dal mio indirizzo
        // 2.1(b): confronto con TUTTE le mie email (globale + per-server), non
        // con la sola globale — altrimenti i post fatti con un'identità
        // per-server non venivano riconosciuti come miei.
        val mieEmail = AppSettings.myEmails
        var newToMe = 0
        // 1.4.1 (build 108) — LA 105/B E' STATA REVOCATA, E VA SPIEGATO PERCHE'.
        //
        // Nella 105 un articolo con il From mio nasceva gia' letto. Nasceva da
        // una segnalazione di BIG Umberto dopo un supersede, ma la regola
        // scritta qui era molto piu' larga della sua richiesta: non toccava
        // "il messaggio appena spedito", toccava OGNI proprio post che entra
        // per la prima volta nel database — quindi anche tutto il proprio
        // storico alla prima sincronizzazione di un gruppo, e (dalla 104, che
        // fa seguire il letto al Message-ID) in tutti i gruppi insieme.
        //
        // Dave Royal: "I've never met a newsreader that automatically marks my
        // own posts read. PAN, Thunderbird and PH don't"; Marco ha verificato
        // che nemmeno MesNews lo fa — ed e' il modello dichiarato del progetto,
        // citato come autorita' per il "letto che segue il Message-ID". La
        // ragione di merito e' la sua: dopo aver pubblicato si vuole RITROVARE
        // il proprio messaggio per controllare come e' venuto, e con il filtro
        // "solo non letti" acceso un post nato letto semplicemente non c'e'.
        //
        // Prova sul campo, non opinione: il 21 settembre il collaudo del
        // crosspost e' fallito proprio cosi'. Il messaggio era stato pubblicato
        // (240 Article Posted, entrambi i gruppi +1) ma non si vedeva, perche'
        // era nato verde.
        // ------------------------------------------------------------ §A12
        // «SEGUI LA DISCUSSIONE» DEVE SEGUIRE ANCHE IL FUTURO.
        //
        // Segnalazione di BIG Umberto (29 settembre): in una discussione seguita
        // le stelle c'erano solo fino a una certa data. Diagnosi completa in
        // NeReO_stelle_discussione_seguita.md; in breve: `setWatchedBulk` e' un
        // UPDATE sui messaggi PRESENTI quando premi, e qui sotto il flag nasceva
        // solo da una regola dei filtri (ScoreResult.Action.WATCH). Nessuna
        // eredita': le risposte che arrivavano dopo nascevano non seguite.
        //
        // E NON ERA ESTETICA, che e' il punto. `watched` protegge da tre cose —
        // il filtro sul lettore di news (applicaRegoleLettore), la propagazione
        // dei filtri alle risposte (propagaAlleRisposte) e la CONSERVAZIONE, che
        // tiene i seguiti e cancella gli altri. Una discussione seguita si
        // svuotava dal centro: restavano i messaggi del giorno in cui l'avevi
        // seguita, sparivano quelli per cui la seguivi.
        //
        // PERCHE' LE REFERENCES E NON IL GENITORE DIRETTO, che sarebbe stato
        // gratis visto che `flagged` qui sotto il genitore lo carica gia':
        //
        //  - il genitore diretto SI SPEZZA. Se un anello intermedio non c'e'
        //    (mai scaricato, o tolto dalla conservazione) i discendenti non
        //    ereditano piu'. L'header References porta l'INTERA catena, quindi
        //    un pronipote trova la radice seguita da se', a qualunque
        //    profondita';
        //  - quel blocco e' saltato quando `mieEmail` e' vuoto, e l'eredita' del
        //    seguito non puo' dipendere dall'aver configurato un'email;
        //  - ed e' la stessa regola che il FILTRO «solo discussioni seguite» usa
        //    gia' (`inThreadSeguito` in ThreadListScreen). Finora flag e filtro
        //    si contraddicevano: ora dicono la stessa cosa per costruzione,
        //    non per manutenzione.
        //
        // Costo: una query a blocco, sull'indice di `messageId`, e solo sugli id
        // che il lotto nomina davvero. Meno di quanto costi `replyToMe` qui
        // sotto, che invece interroga una volta per articolo.
        //
        // LIMITE DICHIARATO: vale per i messaggi FUTURI. Quelli gia' in archivio
        // senza flag restano tali — rimetterli in pari e' una scrittura di massa
        // e si decide a parte. Rimedio manuale: togliere e rimettere il «segui».
        val articoliConEredita = ereditaSeguitoDalThread(articles)

        val flagged = if (mieEmail.isEmpty()) articoliConEredita else articoliConEredita.map { a ->
            val parentId = a.references.trim().split(Regex("\\s+")).lastOrNull()?.takeIf { it.isNotBlank() }
            val parent = parentId?.let { db.articleDao().byMessageId(it) }
            // 1.4.1 (105/A): il criterio "e' mio" e' uno solo, quello
            // irrobustito dall'audit della 1.2.12. Qui era rimasta la ricerca
            // per sottostringa sulla riga From intera. Questa resta.
            if (parent != null && AppSettings.eMioPost(parent.fromField)) {
                newToMe++; a.copy(replyToMe = true)
            } else a
        }
        // (il totale su TUTTI i lotti lo somma il chiamante)
        ultimoLottoFiltrati = flagged.count { it.hidden }
        db.articleDao().insertAll(flagged)

        // ---------------------------------------------------------- M1
        // Seconda passata: propagazione alle risposte. Gira DOPO l'insert,
        // perche' lavora sugli articoli come stanno nel database.
        if (regolePropaganti.isNotEmpty()) {
            if (semiNuovi.isNotEmpty()) db.poisonedIdDao().insertAll(semiNuovi)

            // Di norma basta valutare il lotto appena arrivato: il contesto
            // storico e' gia' nel registro degli avvelenati.
            //
            // Eccezione: se in questo lotto sono comparsi semi NUOVI, puo'
            // esistere in archivio una risposta arrivata PRIMA del post che
            // la avvelena (numeri d'articolo fuori ordine, capita con certi
            // feed). Quella non verrebbe mai piu' rivalutata, e resterebbe
            // visibile per sempre. In quel caso soltanto si allarga l'esame
            // a tutto il gruppo: e' una passata O(n) con lookup su insieme,
            // non il costo quadratico che aveva fatto crashare beta20.
            val candidati =
                if (semiNuovi.isEmpty()) flagged
                else db.articleDao().byGroupOnce(group.id)

            val nascosti = propagaAlleRisposte(group, engine, candidati)
            if (nascosti > 0) db.groupDao().refreshUnreadCount(group.id)
        }

        // Il segnalibro avanza A OGNI LOTTO, non solo alla fine: una
        // sincronizzazione interrotta a meta' (rete che cade, app chiusa)
        // riprende da qui invece di ricominciare da capo. Su un gruppo da
        // milioni di articoli e' la differenza fra un riavvio e mezz'ora
        // buttata.
        db.groupDao().update(
            group.copy(
                // [finoA]: la fine di QUESTO lotto, non del gruppo.
                lastFetchedArticleNumber = finoA,
                lastSyncEpoch = System.currentTimeMillis() / 1000,
            )
        )
        return articles.size to newToMe
    }


    /** Scarica il corpo di un articolo (se non già in cache) e lo ritorna decodificato. */
    /**
     * Salva intestazioni complete e Face a partire dai dati grezzi appena
     * scaricati. Estratta come funzione condivisa (beta15) perché la stessa
     * logica era duplicata in tre punti indipendenti — [fetchBody],
     * [prefetchReplies], [downloadAllBodies] — e nel tempo due di questi tre
     * punti sono rimasti indietro rispetto ai fix applicati al primo: bug
     * segnalato da Bingo3331 come "persistente" dopo il fix di beta13/14,
     * quando in realtà il fix c'era ma non copriva gli altri due percorsi di
     * scaricamento. Un solo punto di manutenzione da qui in poi.
     */
    private suspend fun salvaHeaderEFace(articleId: Long, headers: Map<String, String>, raw: List<ByteArray>) {
        // intestazioni complete (decodificate) per la vista "Intestazioni".
        //
        // BUG beta12/13 (Bingo3331 + Big Umberto, pattern diversi ma stessa
        // causa): "Intestazioni non disponibili" compariva in modo
        // apparentemente casuale (Bingo) o solo dopo il primo messaggio del
        // thread (Umberto). Causa: il vecchio codice faceva
        // `raw.takeWhile { it.isNotEmpty() }` sui byte grezzi — fragile: se la
        // prima riga di `raw` era vuota o anomala, takeWhile si fermava subito
        // e NON salvava alcun header → headersRaw null → messaggio di errore.
        // Fix: uso la stessa separazione ROBUSTA di splitArticleRaw (la prima
        // riga vuota separa header e corpo), invece di fidarmi che raw inizi
        // pulito. `raw` qui è List<ByteArray>.
        run {
            var sep = raw.indexOfFirst { it.isEmpty() }
            if (sep < 0) sep = raw.size
            raw.subList(0, sep)
                .joinToString("\n") { MimeUtil.smartDecodeHeader(it) }
                .takeIf { it.isNotBlank() }
                ?.let { db.articleDao().setHeaders(articleId, it) }
        }
        // header "Face:" (PNG 48x48 base64): tolgo spazi/fold e salvo per la
        // visualizzazione.
        //
        // BUG beta13 (Marco, con test: la propria Face 48x48 A COLORI non
        // compariva affatto): il tetto era 12000 caratteri, tarato su Face
        // piccole in stile X-Face. Ma un PNG 48x48 A COLORI dettagliato
        // supera facilmente i 12000 base64 (~9KB PNG → ~12600 char) e veniva
        // scartato QUI in silenzio, senza mai arrivare a setFace: in lettura
        // non c'era nulla da mostrare. Alzato a 32000 (copre Face a colori
        // ricche, ~23KB PNG) mantenendo comunque un tetto anti-abuso contro
        // header abnormi.
        headers["face"]?.replace(Regex("\\s+"), "")?.takeIf { it.length in 40..32000 }?.let {
            db.articleDao().setFace(articleId, it)
        }
        // 1.4.4: le intestazioni complete sono appena arrivate — e' l'unico
        // istante in cui una regola sul lettore di news puo' essere valutata.
        applicaRegoleLettore(articleId, headers)
    }

    /**
     * 1.4.4 — LA REGOLA SUL LETTORE DI NEWS, VALUTATA NELL'ISTANTE IN CUI
     * DIVENTA VALUTABILE.
     *
     * Senza questo aggancio la funzione ci sarebbe ma sembrerebbe rotta: le
     * regole su `NEWSREADER` scatterebbero solo premendo «Applica ora» nella
     * schermata Filtri, e nessuno lo farebbe dopo ogni messaggio. Qui invece il
     * post sparisce da solo poco dopo essere arrivato — con «prefetch
     * risposte» acceso (predefinito) o con «scarica i corpi», senza nemmeno
     * aprirlo.
     *
     * TRE SCELTE DA DICHIARARE, perche' nessuna delle tre e' ovvia.
     *
     * 1. SI NASCONDE, NON SI CANCELLA. Anche con un punteggio da KILL, qui si
     *    arriva al massimo a HIDE. Cancellare dentro il percorso di scarico
     *    significherebbe far sparire l'articolo mentre lo stai leggendo — il
     *    corpo e' appena arrivato *perche' l'hai aperto*. La cancellazione
     *    resta a «Applica ora», che e' un gesto deliberato.
     * 2. IL SEME SI REGISTRA PRIMA. Stesso ordine del sync e di
     *    `applicaFiltriRetroattivi`, e per la stessa ragione: e' il message-id
     *    a cui si aggancera' la discendenza futura. Registrarlo dopo aver
     *    nascosto funziona al primo giro e smette al secondo.
     * 3. LA PROPAGAZIONE NON SI FA QUI. Il ramo cade al sync successivo (o
     *    subito, premendo «Applica ora»): propagare a ogni corpo scaricato
     *    vorrebbe dire rileggere tutto il gruppo per ogni messaggio.
     *
     * COSTO PER CHI NON USA LA FUNZIONE: una `SELECT` su `filter_rules`, che
     * ha una manciata di righe, accanto a una richiesta di rete che e' appena
     * costata mille volte tanto. Se nessuna regola guarda il lettore, si esce
     * prima di toccare qualunque altra cosa.
     */
    private suspend fun applicaRegoleLettore(articleId: Long, headers: Map<String, String>) {
        // 1.5.7 / 175: non solo il lettore di news — anche il campo HEADERS
        // (tutte le intestazioni) diventa valutabile qui. Si esce subito se
        // nessuna regola attiva guarda le intestazioni complete.
        val hf = MimeUtil.intestazioniPerFiltri(headers)
        if (hf.lettore == null && hf.tutte == null) return
        val tutte = db.filterDao().allOnce().map { it.toRule() }
        if (tutte.none { it.enabled && it.field.richiedeIntestazioni }) return

        val a = db.articleDao().byId(articleId) ?: return
        if (a.watched) return // una discussione seguita non si nasconde mai
        val g = db.groupDao().byId(a.groupId) ?: return

        val engine = FilterEngine(tutte)
        val scorable = ScorableArticle(
            a.fromField, a.subject, a.messageId,
            newsgroups = g.name, body = a.body, references = a.references,
            newsreader = hf.lettore, intestazioni = hf.tutte,
        )

        // (2) il seme prima di tutto il resto
        val regolaSeme = engine.poisoningRulesForGroup(g.name).firstOrNull { it.matchesArticle(scorable) }
        if (regolaSeme != null) {
            db.poisonedIdDao().insertAll(
                listOf(PoisonedId(a.messageId, a.groupId, a.dateEpoch, regolaSeme.id, seed = true)),
            )
        }

        // (1) nascondere, mai cancellare
        val res = engine.evaluate(scorable, g.name)
        val daNascondere = res.action == ScoreResult.Action.HIDE || res.action == ScoreResult.Action.KILL
        if (daNascondere && !a.hidden) {
            // Per messageId e non per id: un crosspost ha una copia per gruppo,
            // e nasconderne una sola lascerebbe le altre a video (1.2.15, voce D).
            db.articleDao().setHiddenAndScoreByMessageIds(listOf(a.messageId), hidden = true, score = res.score)
            db.articleDao().groupIdsByMessageId(a.messageId).forEach { db.groupDao().refreshUnreadCount(it) }
        }
    }

    /**
     * 1.2.15 (voce D) — SUPERSEDES IN LETTURA.
     *
     * Osservazione di Marco (6 settembre, con prova sul campo): dopo un
     * "Sostituisci" su MesNews restavano leggibili DUE messaggi, l'originale e
     * quello nuovo, su tutti e tre i server. NeReO ne mostrava uno solo — ma
     * per effetto collaterale di come ricostruisce il thread, non per una
     * scelta. Questa funzione la rende una scelta.
     *
     * `Supersedes:` (RFC 5536 §3.2.12) è un'intestazione dell'articolo NUOVO
     * che nomina quello che rimpiazza. Il server dovrebbe cancellare il vecchio
     * per conto suo; molti non lo fanno, e in ogni caso la nostra copia locale
     * era già stata scaricata. Qui il vecchio viene NASCOSTO, non eliminato:
     * resta visibile con "Mostra nascosti", esattamente come un messaggio
     * filtrato. Cancellarlo sarebbe una perdita di dati decisa da un'altra
     * persona.
     *
     * LA GUARDIA DI PROPRIETÀ È IL PUNTO DELICATO. Senza, chiunque potrebbe
     * pubblicare un articolo con `Supersedes: <messaggio-di-qualcun-altro>` e
     * farlo sparire dalla vista di chi legge NeReO: una censura a costo zero,
     * proprio la classe di problemi che l'audit di sicurezza ha chiuso
     * altrove. I server seri confrontano il From: prima di onorare un
     * supersede; lo facciamo anche noi, con lo stesso identico criterio usato
     * da "Sostituisci" e "Ritira" ([AppSettings.indirizzoDelFrom], confronto
     * sull'indirizzo e non sulla riga intera).
     *
     * L'aggiornamento colpisce TUTTE le copie con quel Message-ID (un crosspost
     * ne ha una per gruppo), quindi i conteggi vanno rinfrescati per ognuno dei
     * gruppi coinvolti.
     */
    private suspend fun applicaSupersedes(nuovo: Article, headers: Map<String, String>) {
        val vecchioId = headers["supersedes"]
            ?.trim()
            ?.takeIf { it.startsWith("<") && it.endsWith(">") && it.length in 3..998 }
            ?: return
        if (vecchioId == nuovo.messageId) return
        val vecchio = db.articleDao().byMessageId(vecchioId) ?: return
        if (vecchio.id == nuovo.id) return
        if (vecchio.hidden) return

        val autoreVecchio = AppSettings.indirizzoDelFrom(vecchio.fromField)
        val autoreNuovo = AppSettings.indirizzoDelFrom(nuovo.fromField)
        if (autoreVecchio == null || autoreNuovo == null ||
            !autoreVecchio.equals(autoreNuovo, ignoreCase = true)
        ) {
            NntpLog.diag(
                "Supersedes ignorato: $vecchioId non è dello stesso autore di ${nuovo.messageId}",
            )
            return
        }

        // score conservato: il supersede riguarda la visibilità, non il
        // punteggio assegnato dal motore filtri.
        db.articleDao().setHiddenAndScoreByMessageIds(listOf(vecchioId), true, vecchio.score)
        for (gid in db.articleDao().groupIdsByMessageId(vecchioId)) {
            db.groupDao().refreshUnreadCount(gid)
        }
        NntpLog.diag("Supersedes: nascosto $vecchioId, sostituito da ${nuovo.messageId}")
    }

    /**
     * 1.2.15 (voce B) — elimina l'ARTICOLO locale, riga compresa, quando sul
     * server non esiste più e l'utente lo chiede esplicitamente.
     *
     * Non è una sincronizzazione né una pulizia automatica: è la via d'uscita
     * offerta accanto al messaggio di [ArticoloMancanteException]. Al prossimo
     * sync del gruppo la riga non tornerà, perché non c'è più nulla da cui
     * possa tornare.
     *
     * 1.4.2 — SI CHIAMAVA `eliminaCopiaLocale`, ED E' STATO UN GUAIO.
     * Quel nome suonava come "togli la copia in cache", cioè il corpo. Fa
     * invece `DELETE FROM articles`. Sulla fede di quel nome, la 1.4 ci ha
     * cablato sopra il pulsante "Ricarica il messaggio": premerlo cancellava
     * l'articolo per sempre invece di riscaricarlo. Il nome ora dice quello
     * che la funzione fa — vedi [invalidaCorpo] per ciò che il pulsante di
     * ricarica voleva davvero.
     */
    suspend fun eliminaArticoloLocale(articleId: Long) = withContext(Dispatchers.IO) {
        val article = db.articleDao().byId(articleId) ?: return@withContext
        db.articleDao().deleteByIds(listOf(articleId))
        db.groupDao().refreshUnreadCount(article.groupId)
    }

    /**
     * 1.4.2 — quello che "Ricarica il messaggio" avrebbe dovuto fare dalla 1.4.
     *
     * Azzera il SOLO corpo. [fetchBody] non trova più la cache, scende fino a
     * GROUP + ARTICLE e riscrive corpo, intestazioni e `Face`. Restano in
     * piedi riga, Message-ID, numero, stato letto, "seguita", punteggio dei
     * filtri e marcatore delle risposte a me.
     *
     * Perché il difetto era grave e non solo fastidioso: `syncGroup` riparte
     * da `lastFetchedArticleNumber + 1`, quindi un articolo già superato dal
     * segnalibro NON viene più richiesto. Cancellarlo non lo faceva sparire
     * fino al prossimo aggiornamento: lo faceva sparire finché non si
     * riscaricava l'intero gruppo.
     *
     * Segnalato sul gruppo da un utente con l'analisi di HAL, verificata qui
     * riga per riga prima di correggere.
     */
    suspend fun invalidaCorpo(articleId: Long) = withContext(Dispatchers.IO) {
        db.articleDao().svuotaCorpo(articleId)
    }

    /**
     * [markRead]: se false, scarica/ritorna il corpo SENZA marcare "letto".
     *
     * 1.2.3 (Roxy): la vista continua marcava "letto" troppi messaggi. Ogni post
     * composto chiamava fetchBody, che marcava letto; ma all'apertura la lista
     * nasce in cima al thread e compone i primi messaggi PRIMA di saltare a quello
     * selezionato → i messaggi in testa, solo attraversati, finivano "letti".
     * Sganciando la marcatura dal download, la vista continua può scaricare il
     * corpo per mostrarlo (markRead=false) e decidere altrove QUALI marcare
     * letti (solo dall'ancora in giù). Default true: gli altri chiamanti (vista
     * singola, telefono) non cambiano comportamento.
     */
    suspend fun fetchBody(articleId: Long, markRead: Boolean = true): String = withContext(Dispatchers.IO) {
        val article = db.articleDao().byId(articleId) ?: return@withContext ""

        // Corpo già in cache: niente rete, ma la marcatura "letto" va fatta
        // COMUNQUE. Prima della beta9 il caso era innocuo (un corpo finiva in
        // cache solo dopo essere stato letto, quindi era già read=true); con il
        // prefetch delle risposte i corpi arrivano in cache SENZA essere letti,
        // e uscire di qui senza marcare lasciava tutto il thread "da leggere"
        // tranne il primo messaggio.
        article.body?.let { cached ->
            if (markRead && !article.read) segnaLetto(article.id, article.groupId, true)
            return@withContext cached
        }

        val group = db.groupDao().byId(article.groupId) ?: return@withContext ""
        val server = db.serverDao().byId(group.serverId) ?: return@withContext ""

        // 1.2.3: un solo fetch di rete per volta (vedi bodyFetchMutex).
        bodyFetchMutex.withLock {
            // Ri-controllo la cache DENTRO il lock: mentre eravamo in coda un altro
            // fetch potrebbe aver già scaricato questo corpo. Evita di riaprire una
            // connessione e di ripetere il lavoro.
            val fresh = db.articleDao().byId(article.id)
            fresh?.body?.let { cached ->
                if (markRead && !fresh.read) segnaLetto(fresh.id, fresh.groupId, true)
                return@withContext cached
            }
            // 1.2.9: connessione riusata fra chiamate consecutive (vedi
            // clientCorpiPer sopra), invece di una nuova per ogni corpo.
            //
            // 1.2.13 — RITENTATIVO TRASPARENTE SU CONNESSIONE RIUSATA.
            // Diagnosi dal log NNTP del 6 settembre 2026 (sessione 3whpd e,
            // a ritroso, tre sessioni della 1.2.11): il comando parte, la
            // risposta non arriva MAI, e l'errore di rete finisce a schermo al
            // posto del corpo ("Errore: Software caused connection abort").
            // Ricorre sempre con la stessa forma: connessione RIUSATA, viva da
            // decine di secondi, primo comando dopo una pausa di lettura. Le
            // sessioni brevi non falliscono mai, e una connessione allo stesso
            // server è sopravvissuta quasi cinque minuti: non è il riuso ad
            // essere sbagliato, è la nostra finestra di 60 secondi ad essere
            // ottimista per QUEL server. Ogni provider ha la sua soglia di
            // inattività e non la conosceremo mai per tutti.
            //
            // Quindi non si insegue il numero giusto: se il passo di rete
            // fallisce con un errore di I/O SU UNA CONNESSIONE RIUSATA, la si
            // butta e si riprova UNA volta con una connessione nuova. È sicuro
            // perché a quel punto non è stato scritto nulla nel database e
            // rileggere un articolo è idempotente. Una connessione APPENA
            // aperta che fallisce, invece, non si ritenta: lì l'errore è vero e
            // va mostrato.
            var ultimoErrore: Exception? = null
            var tentativi = 0
            while (tentativi < 2) {
                tentativi++
                val (client, riusata) = clientCorpiPer(server)
                try {
                    client.group(group.name)
                    val raw = client.article(article.messageId.ifEmpty { article.number.toString() })
                    val (headers, bodyLines) = MimeUtil.splitArticleRaw(raw)
                    val text = MimeUtil.decodeTextBodyRaw(headers, bodyLines)
                    db.articleDao().setBody(article.id, text)
                    salvaHeaderEFace(article.id, headers, raw)
                    // 1.2.15 (voce D): se questo articolo ne sostituisce un
                    // altro, l'originale sparisce dalla vista locale.
                    // runCatching di proposito: è un effetto collaterale
                    // gradito, non parte del contratto di questa funzione. Se
                    // fallisse, propagando finirebbe nel catch qui sotto e
                    // farebbe RISCARICARE un corpo già salvato — il rimedio
                    // peggiore del male.
                    runCatching { applicaSupersedes(article, headers) }
                    // `group.id` e `article.groupId` sono lo stesso valore
                    // (il gruppo e' stato letto proprio da article.groupId).
                    if (markRead) segnaLetto(article.id, article.groupId, true)
                    maybeAutoPlonkByBody(article, group.name, text)
                    return@withContext text
                } catch (e: NntpException) {
                    // 1.2.15 (voci A e B) — UN ERRORE DI PROTOCOLLO NON È UN
                    // GUASTO DI RETE, e va trattato diversamente da entrambi i
                    // punti di vista.
                    //
                    // Difetto introdotto da me nella 1.2.13 e trovato leggendo
                    // il log di un cancel: NntpException ESTENDE IOException,
                    // quindi finiva dritto nel ramo del ritentativo qui sotto.
                    // Un "430 No such article" — il server ha risposto, in
                    // perfetta salute, che quell'articolo non esiste — faceva
                    // buttare una connessione sana, aprirne un'altra e
                    // ripetere la domanda per ottenere la stessa risposta.
                    // Lavoro inutile e un secondo di attesa regalato all'utente.
                    //
                    // Qui la connessione NON viene chiusa: ha appena parlato,
                    // sta benissimo, e il prossimo articolo la riuserà.
                    //
                    // 430 (per Message-ID) e 423 (per numero) sono lo stesso
                    // fatto detto in due modi: l'articolo è scaduto o è stato
                    // ritirato sul server, ma la riga di riepilogo è rimasta
                    // nel nostro database. È il caso NORMALE di un cancel
                    // andato a buon fine — nostro o di qualcun altro — e
                    // merita un messaggio che lo dica, invece di un codice
                    // numerico che non significa niente per chi legge.
                    if (e.code == 430 || e.code == 423) {
                        NntpLog.diag("articolo non più sul server (${e.code}): ${article.messageId}")
                        throw ArticoloMancanteException(article.id, article.messageId)
                    }
                    throw e
                } catch (e: Exception) {
                    // connessione probabilmente caduta/in errore: non riusarla al prossimo giro
                    chiudiClientCorpi()
                    // Si ritenta SOLO l'errore di rete su connessione riusata.
                    // La CancellationException (scorrere veloce fra i messaggi)
                    // non è un errore di I/O e continua a propagarsi subito,
                    // come deve: è cooperazione normale, non un guasto.
                    // Sono inclusi di proposito anche i timeout di lettura: su
                    // rete mobile una connessione inattiva viene spesso lasciata
                    // cadere SENZA che nessuno ce lo dica, e in quel caso il
                    // sintomo è proprio un timeout. Costo nel caso peggiore: una
                    // sola attesa in più, e solo su connessione già riusata.
                    // 1.2.15 (voce A): gli errori di PROTOCOLLO non arrivano più
                    // fin qui — li intercetta il catch (NntpException) sopra.
                    // Quello che resta è ciò per cui il ritentativo era stato
                    // pensato: la connessione che è caduta davvero.
                    if (riusata && tentativi == 1 && e is java.io.IOException) {
                        ultimoErrore = e
                        NntpLog.diag("connessione riusata caduta (${e.message}): riprovo con una connessione nuova")
                    } else {
                        throw e
                    }
                }
            }
            throw ultimoErrore ?: java.io.IOException("Scaricamento del corpo non riuscito")
        }
    }

    /**
     * Scarica in background i corpi delle RISPOSTE DIRETTE al messaggio dato,
     * cosi' scorrendo il thread sono gia' pronte. Le risposte gia' con corpo
     * vengono saltate. Silenzioso: gli errori di rete non devono disturbare
     * la lettura in corso.
     *
     * "Risposta diretta" = articolo il cui ultimo Message-ID in References è il
     * Message-ID del messaggio corrente.
     *
     * IMPORTANTE (bug segnalato da un tester su 1.2.11, non ancora pubblicata,
     * corretto "a versione invariata"): prima di questa correzione, questa
     * funzione apriva una PROPRIA connessione via `openClient`, indipendente
     * e non coordinata con quella condivisa di [fetchBody] ([clientCorpiPer]/
     * [bodyFetchMutex]). Leggendo articoli in rapida successione (ognuno con
     * risposte non ancora scaricate), ogni apertura lanciava un nuovo
     * prefetch = una nuova connessione, che potevano accumularsi in parallelo
     * verso lo stesso server. Un provider che limita le connessioni
     * simultanee per account (confermato dal log Diagnostica NNTP: molte
     * sessioni sovrapposte verso lo stesso host) può abortire quelle in
     * eccesso — da cui l'errore di rete mostrato al posto del corpo. Corretto
     * riusando la STESSA connessione condivisa e serializzata di [fetchBody]:
     * ora il prefetch non apre mai una seconda connessione verso il server,
     * mette in coda dietro (o si intervalla con) le letture dell'utente.
     */
    suspend fun prefetchReplies(articleId: Long) = withContext(Dispatchers.IO) {
        val parent = db.articleDao().byId(articleId) ?: return@withContext
        if (parent.messageId.isBlank()) return@withContext
        val group = db.groupDao().byId(parent.groupId) ?: return@withContext
        val server = db.serverDao().byId(group.serverId) ?: return@withContext

        val figli = db.articleDao().byGroupOnce(parent.groupId)
            .filter { it.body == null && lastReference(it.references) == parent.messageId }
        if (figli.isEmpty()) return@withContext

        for (figlio in figli) {
            runCatching {
                bodyFetchMutex.withLock {
                    // Ri-controllo dentro il lock: un'altra fetch (l'utente
                    // stesso, o un prefetch precedente) potrebbe averlo già
                    // scaricato nel frattempo.
                    val fresh = db.articleDao().byId(figlio.id)
                    if (fresh?.body != null) return@withLock
                    try {
                        // 1.2.13: il prefetch NON ritenta (a differenza di
                        // fetchBody). È lavoro di anticipo e silenzioso: se la
                        // connessione è caduta, viene scartata qui e la lettura
                        // vera dell'utente ne aprirà una nuova, con il proprio
                        // ritentativo. Aggiungerne uno anche qui raddoppierebbe
                        // il lavoro in background senza che nessuno lo veda.
                        val (client, _) = clientCorpiPer(server)
                        client.group(group.name)
                        val raw = client.article(figlio.messageId.ifEmpty { figlio.number.toString() })
                        val (headers, bodyLines) = MimeUtil.splitArticleRaw(raw)
                        val text = MimeUtil.decodeTextBodyRaw(headers, bodyLines)
                        db.articleDao().setBody(figlio.id, text)
                        salvaHeaderEFace(figlio.id, headers, raw)
                    } catch (e: Exception) {
                        // stessa cautela di fetchBody: connessione probabilmente
                        // caduta/in errore, non riusarla al prossimo giro.
                        chiudiClientCorpi()
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Scarica i corpi di TUTTI i messaggi del gruppo ancora senza corpo, fino a
     * [limit]. Operazione esplicita e potenzialmente lunga (una richiesta per
     * messaggio): la si offre disattivata di default. Restituisce quanti corpi
     * sono stati effettivamente scaricati. NON marca i messaggi come letti:
     * scaricare non è leggere.
     */
    suspend fun downloadAllBodies(groupId: Long, limit: Int = 5000): Int = withContext(Dispatchers.IO) {
        val group = db.groupDao().byId(groupId) ?: return@withContext 0
        val server = db.serverDao().byId(group.serverId) ?: return@withContext 0
        val mancanti = db.articleDao().withoutBodyInGroup(groupId, limit)
        if (mancanti.isEmpty()) return@withContext 0
        var fatti = 0
        openClient(server) { client ->
            client.group(group.name)
            for (a in mancanti) {
                runCatching {
                    val raw = client.article(a.messageId.ifEmpty { a.number.toString() })
                    val (headers, bodyLines) = MimeUtil.splitArticleRaw(raw)
                    db.articleDao().setBody(a.id, MimeUtil.decodeTextBodyRaw(headers, bodyLines))
                    salvaHeaderEFace(a.id, headers, raw)
                    fatti++
                }
            }
        }
        fatti
    }

    suspend fun missingBodyCount(groupId: Long): Int =
        db.articleDao().missingBodyCount(groupId)

    /**
     * Cancella la COPIA LOCALE dei messaggi indicati. Non invia nulla al
     * server: risincronizzando il gruppo i messaggi tornano (se ancora
     * presenti sul server). Restituisce i messaggi cancellati, cosi' il
     * chiamante puo' offrire un ANNULLA reinserendoli tali e quali.
     */
    /**
     * Sposta un gruppo sottoscritto su/giù di una posizione fra quelli dello
     * stesso server. Ad ogni chiamata rinumera TUTTA la lista in ordine
     * attuale (0,1,2,...) prima di scambiare i due vicini: così non ci si
     * pianta quando più gruppi condividono ancora sortOrder=0 (default,
     * mai riordinati prima). Lista tipicamente corta: costo trascurabile.
     */
    suspend fun moveGroup(serverId: Long, groupId: Long, up: Boolean) = withContext(Dispatchers.IO) {
        val lista = db.groupDao().subscribedOnceForServer(serverId).toMutableList()
        val i = lista.indexOfFirst { it.id == groupId }
        if (i < 0) return@withContext
        val j = if (up) i - 1 else i + 1
        if (j < 0 || j >= lista.size) return@withContext
        val tmp = lista[i]; lista[i] = lista[j]; lista[j] = tmp
        lista.forEachIndexed { idx, g -> db.groupDao().setSortOrder(g.id, idx) }
    }

    /** Stesso meccanismo di [moveGroup], applicato ai server (elenco intero, non per-server). */
    suspend fun moveServer(serverId: Long, up: Boolean) = withContext(Dispatchers.IO) {
        val lista = db.serverDao().allOnce().toMutableList()
        val i = lista.indexOfFirst { it.id == serverId }
        if (i < 0) return@withContext
        val j = if (up) i - 1 else i + 1
        if (j < 0 || j >= lista.size) return@withContext
        val tmp = lista[i]; lista[i] = lista[j]; lista[j] = tmp
        lista.forEachIndexed { idx, s -> db.serverDao().setSortOrder(s.id, idx) }
    }

    suspend fun deleteLocal(ids: List<Long>): List<Article> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val salvati = ids.mapNotNull { db.articleDao().byId(it) }
        db.articleDao().deleteByIds(ids)
        salvati.map { it.groupId }.distinct().forEach { db.groupDao().refreshUnreadCount(it) }
        salvati
    }

    /** Ripristina messaggi cancellati localmente (undo di deleteLocal). */
    suspend fun restoreLocal(articles: List<Article>) = withContext(Dispatchers.IO) {
        if (articles.isEmpty()) return@withContext
        db.articleDao().insertAll(articles)
        articles.map { it.groupId }.distinct().forEach { db.groupDao().refreshUnreadCount(it) }
    }

    // ------------------------------------------------- directory gruppi

    /**
     * Scarica l'elenco COMPLETO dei gruppi del server (LIST ACTIVE) e lo
     * salva in cache locale. Da quel momento la ricerca è offline e
     * istantanea. Ritorna il numero di gruppi in cache.
     */
    /** Sincronizza tutti i gruppi sottoscritti; ritorna quante nuove risposte a me. */
    /**
     * Ritorna il numero totale di nuovi articoli SCARICATI su tutti i gruppi.
     *
     * Bug beta13 (segnalato da Marco con conteggi precisi): prima questa
     * funzione sommava `lastSyncNewToMe` — che è tutt'altra cosa, cioè i
     * nuovi articoli *indirizzati all'utente* (usati per il ding), non i
     * nuovi articoli scaricati. Risultato: la barra mostrava numeri
     * scorrelati (es. 1+2 scaricati ma "nessuno" perché nessuno era diretto
     * a me; oppure "5" quando ne erano arrivati 22). Il valore giusto c'era
     * già: syncGroup() ritorna il conteggio corretto, veniva solo buttato
     * via dentro il runCatching. Ora si somma quello. `newToMe` resta contato
     * a parte, solo per decidere se suonare il ding.
     */
    suspend fun syncAllSubscribed(segnalaAuthNonCifrata: Boolean = true): Int = withContext(Dispatchers.IO) {
        syncGroupList(db.groupDao().allSubscribedOnce(), segnalaAuthNonCifrata)
    }

    /**
     * 1.3 (C — aggiornamento selettivo per server/gruppo, richiesta di
     * Bingo3331: vedi NeReO_feature_refresh_selettivo.md). Aggiorna SOLO i
     * gruppi sottoscritti di UN server — stesso motore di [syncAllSubscribed],
     * ristretto. Non tenta alcun "dedup" fra server che condividono un gruppo:
     * ognuno ha la propria retention/i propri articoli, restano indipendenti
     * (scelta deliberata, vedi il doc).
     */
    suspend fun syncServer(serverId: Long, segnalaAuthNonCifrata: Boolean = true): Int = withContext(Dispatchers.IO) {
        syncGroupList(db.groupDao().subscribedOnceForServer(serverId), segnalaAuthNonCifrata)
    }

    /**
     * 1.3 (C): aggiorna un sottoinsieme di gruppi scelto dall'utente (checkbox
     * nella UI di selezione), a prescindere dal server di appartenenza —
     * [syncGroupList] li raggruppa comunque per server, una connessione per
     * server come sempre.
     */
    suspend fun syncGroups(groupIds: List<Long>, segnalaAuthNonCifrata: Boolean = true): Int = withContext(Dispatchers.IO) {
        val gruppi = groupIds.mapNotNull { db.groupDao().byId(it) }
        syncGroupList(gruppi, segnalaAuthNonCifrata)
    }

    /**
     * Cuore comune di [syncAllSubscribed], [syncServer] e [syncGroups]
     * (estratto nella 1.3 — prima viveva solo dentro syncAllSubscribed):
     * raggruppa [gruppi] per server e sincronizza con UNA connessione per
     * server, riusando lo stesso client per tutti i gruppi di quel server.
     * Vedi il commento storico su syncGruppoConClient per il perché.
     */
    private suspend fun syncGroupList(gruppi: List<Group>, segnalaAuthNonCifrata: Boolean): Int = headerSyncMutex.withLock {
        // 1.2.9.c (Piano B, vedi headerSyncMutex): stessa guardia di syncGroup,
        // qui per il percorso "molti gruppi" (syncAllSubscribed/syncServer/
        // syncGroups, incluso il timer automatico in MainActivity).
        var scaricati = 0
        var newToMe = 0
        val engine = buildEngine() // costruito UNA volta e riusato per tutti i gruppi
        // "A" (richiesta Marco): UNA connessione per SERVER, non una per gruppo.
        // Prima ogni gruppo apriva socket + handshake TLS + AUTHINFO e chiudeva:
        // con 28 gruppi su 3 server = 28 login (la lentezza segnalata dal log).
        // Ora raggruppo per server e riuso lo stesso client per tutti i suoi
        // gruppi. È anche più gentile col server (una sessione, non 28).
        // 1.2.6 (Marco): raccolgo gli errori per server. Prima il runCatching
        // inghiottiva l'eccezione in silenzio (robustezza: un server/gruppo
        // ostile non deve abortire l'intero giro), ma buttava via anche il
        // SEGNALE: con l'auth rifiutata su connessione non cifrata (NntpException
        // 483) ogni gruppo falliva, veniva saltato, scaricati restava 0 e la UI
        // mostrava "nessun nuovo messaggio" invece dell'errore. Il singolo gruppo
        // (syncGroup) invece l'errore lo mostrava, perché non ha il runCatching.
        // Il fallimento è a livello di CONNESSIONE, non di gruppo: tengo un errore
        // per server (l'ultimo), poi — se non ho scaricato NULLA e c'è stato
        // almeno un errore — lo rilancio così la UI lo mostra.
        val erroriPerServer = LinkedHashMap<Long, Throwable>()
        val perServer = gruppi.groupBy { it.serverId }
        for ((serverId, gruppiServer) in perServer) {
            val server = db.serverDao().byId(serverId) ?: continue
            // Se la connessione condivisa cade a metà, si riapre per i gruppi
            // ancora da fare. Guardia anti-loop: se un tentativo non fa progressi
            // (il gruppo corrente fa fallire subito la connessione), quel gruppo
            // si salta invece di ritentarlo all'infinito.
            var idx = 0
            while (idx < gruppiServer.size) {
                val partenza = idx
                runCatching {
                    openClient(server) { client ->
                        while (idx < gruppiServer.size) {
                            val n = syncGruppoConClient(client, gruppiServer[idx], engine, AppSettings.maxHeadersSync)
                            scaricati += n
                            newToMe += lastSyncNewToMe
                            idx++ // avanza SOLO a gruppo riuscito
                        }
                    }
                }.onFailure { erroriPerServer[serverId] = it }
                if (idx == partenza) idx++ // nessun progresso: salta il gruppo ostile
            }
        }
        // lastSyncNewToMe riflette l'ultimo gruppo; lo riallineo al totale
        // cumulativo così il ding in GroupsScreen vede il conteggio giusto.
        lastSyncNewToMe = newToMe
        // Fallimento totale = errore visibile. Con un solo server rilancio
        // l'eccezione originale (preserva il messaggio 483 esatto); con più
        // server, un'eccezione col riassunto dei messaggi distinti. Se invece
        // qualcosa è stato scaricato, resto in silenzio (limite noto della via A:
        // un fallimento parziale su un altro server non emerge).
        // 1.2.7 (Marco, feature A): separo il caso "connessione non cifrata" (483)
        // dagli altri errori. Il 483 va al dialog dedicato (che offre "passa a
        // SSL" / "consenti"), non al messaggio secco. `segnalaAuthNonCifrata` è
        // false per il sync automatico periodico: un avviso modale a intervalli,
        // mentre l'utente legge, sarebbe invadente — lo mostriamo solo sui sync
        // avviati dall'utente.
        val e483 = erroriPerServer.filterValues { it is NntpException && it.code == 483 }
        val altriErrori = erroriPerServer.filterValues { !(it is NntpException && it.code == 483) }
        if (segnalaAuthNonCifrata && e483.isNotEmpty()) {
            AvvisoConnessioneNonSicura.serverId.value = e483.keys.first()
        }
        if (scaricati == 0 && altriErrori.isNotEmpty()) {
            if (altriErrori.size == 1) {
                throw altriErrori.values.first()
            }
            val messaggi = altriErrori.values.mapNotNull { it.message }.distinct()
                .joinToString("; ")
            // 1.3.5: in inglese come i messaggi di NntpClient — anche Repository
            // e' senza Context e non puo' leggere le risorse. Vedi la nota
            // su spiegazioneErroreTls in NntpClient.kt.
            throw Exception(messaggi.ifEmpty { "synchronisation failed" })
        }
        return scaricati
    }

    suspend fun refreshAvailableGroups(
        serverId: Long,
        wildmat: String? = null,
        onProgress: ((Int) -> Unit)? = null,
    ): Int = withContext(Dispatchers.IO) {
        val server = db.serverDao().byId(serverId) ?: return@withContext 0
        // timeout di lettura più lungo: su feed grossi il LIST ACTIVE è lento a partire
        nuovoClient(server, readTimeoutMs = 120_000).use { client ->
            try {
                client.connect()
            } finally {
                annotaIdentitaTls(server, client)
            }
            preparaConnessione(client, server)
            val names = client.listActive(wildmat?.takeIf { it.isNotBlank() }).map { it.name }.toList()
            db.availableGroupDao().clear(serverId)
            // insert a blocchi: sui server grossi sono centinaia di migliaia di righe
            var done = 0
            names.chunked(2000).forEach { chunk ->
                db.availableGroupDao().insertAll(chunk.map { AvailableGroup(serverId, it) })
                done += chunk.size
                onProgress?.invoke(done)
            }
            names.size
        }
    }

    suspend fun availableGroupCount(serverId: Long): Int =
        db.availableGroupDao().count(serverId)

    /**
     * Gruppi sottoscritti con il nome del proprio server, per il selettore
     * della diagnostica LISTGROUP manuale (Manutenzione → Diagnostica, Fase 2
     * della proposta HAL/Bingo3331, 1 settembre 2026).
     */
    suspend fun subscribedGroupsWithServerNames(): List<Pair<Group, String>> = withContext(Dispatchers.IO) {
        val servers = db.serverDao().allOnce().associateBy { it.id }
        db.groupDao().allSubscribedOnce().map { g -> g to (servers[g.serverId]?.name ?: "?") }
    }

    /**
     * Diagnostica manuale (Fase 2): confronta GROUP/LISTGROUP/OVER per un
     * gruppo e scrive il riepilogo nel log NNTP diagnostico — non tocca il
     * database locale né la sync normale, è "sola lettura" verso il server.
     * Richiede NntpLog.enabled = true (come tutta la diagnostica), altrimenti
     * fa comunque le richieste di rete ma non registra nulla: chi chiama
     * dovrebbe avvisare l'utente PRIMA di lanciarla, non è compito di questo
     * metodo deciderlo.
     *
     * Aggiunta di HAL (1 settembre 2026): per i numeri presenti in LISTGROUP
     * ma assenti dal riepilogo overview appena ricevuto, un STAT diagnostico
     * per numero distingue un buco nel database overview del server (STAT
     * trova l'articolo) da un articolo davvero scaduto/cancellato (STAT non
     * lo trova). Limitato a [statCap] numeri per non intasare il server su
     * gruppi con buchi enormi — un caso reale probabilmente ne ha pochi.
     */
    suspend fun diagnosticaListgroup(
        groupId: Long,
        statCap: Int = 30,
        annullamento: AnnullamentoDiagnostica? = null,
    ): Unit = withContext(Dispatchers.IO) {
        val group = db.groupDao().byId(groupId) ?: return@withContext
        val server = db.serverDao().byId(group.serverId) ?: return@withContext
        try {
            openClient(server) { client ->
                annullamento?.presidia(client)
                val stats = client.group(group.name)
                val presenti = client.listGroup(group.name)
                val over = client.over(stats.low, stats.high) // registra già il proprio riepilogo (punto 1)
                val overNumeri = over.map { it.number }.toHashSet()
                val mancantiDaOverview = presenti.filter { it !in overNumeri }
                NntpLog.diag(
                    "LISTGROUP ${group.name}: ${presenti.size} articoli presenti sul server " +
                        "(GROUP: low=${stats.low} high=${stats.high} count=${stats.estimatedCount}), " +
                        "${mancantiDaOverview.size} assenti dal riepilogo overview appena ricevuto",
                )
                if (mancantiDaOverview.isNotEmpty()) {
                    val daVerificare = mancantiDaOverview.take(statCap)
                    var esistono = 0
                    var assenti = 0
                    for (n in daVerificare) {
                        // Il ciclo di STAT e' l'unico punto della diagnostica fatto
                        // di tante richieste brevi: qui l'annullamento si nota da
                        // solo, senza bisogno di chiudere niente.
                        if (annullamento?.annullato == true) break
                        if (client.stat(n)) esistono++ else assenti++
                    }
                    val nota = if (mancantiDaOverview.size > statCap) {
                        " (STAT eseguito solo sui primi $statCap di ${mancantiDaOverview.size}, per non intasare il server)"
                    } else ""
                    NntpLog.diag(
                        "STAT sui ${daVerificare.size} numeri assenti dall'overview: " +
                            "$esistono esistono davvero (buco nel database overview del server), " +
                            "$assenti non esistono (probabile scadenza/cancellazione)$nota",
                    )
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Se e' l'utente ad aver chiuso il socket, l'eccezione che ne esce
            // NON e' un guasto: e' l'annullamento che arriva a destinazione.
            // Qualunque altro errore continua a propagarsi al chiamante.
            if (annullamento?.annullato != true) throw e
            NntpLog.diag(
                "diagnostica LISTGROUP ${group.name}: ANNULLATA dall'utente " +
                    "(${e.javaClass.simpleName}). I dati gia' registrati qui sopra restano validi.",
            )
        } finally {
            annullamento?.lasciaIlPresidio()
        }
    }

    /** Ricerca locale istantanea (prefissi prima, poi contains). */
    suspend fun searchAvailableGroups(serverId: Long, query: String): List<String> =
        db.availableGroupDao().search(serverId, query.trim())

    suspend fun subscribedNames(serverId: Long): Set<String> =
        db.groupDao().subscribedNames(serverId).toSet()

    suspend fun unsubscribeByName(serverId: Long, groupName: String) {
        db.groupDao().byName(serverId, groupName)?.let {
            db.groupDao().update(it.copy(subscribed = false))
        }
    }

    /**
     * [sortOrder]: se non null, imposta l'ordine del gruppo (1.2.6 — ripristino
     * dell'ordine dai backup, richiesta Dave Royal). Null = comportamento storico
     * (nuovo gruppo → 0; gruppo esistente → ordine invariato).
     */
    suspend fun subscribe(serverId: Long, groupName: String, sortOrder: Int? = null) {
        val existing = db.groupDao().byName(serverId, groupName)
        if (existing == null) {
            db.groupDao().insert(
                Group(serverId = serverId, name = groupName, subscribed = true, sortOrder = sortOrder ?: 0),
            )
        } else {
            db.groupDao().update(
                existing.copy(subscribed = true, sortOrder = sortOrder ?: existing.sortOrder),
            )
        }
    }

    suspend fun unsubscribe(groupId: Long) {
        db.groupDao().byId(groupId)?.let { db.groupDao().update(it.copy(subscribed = false)) }
    }

    /**
     * Disiscrizione da una schermata, con scelta su cosa fare della copia
     * locale.
     *
     * La riga del gruppo NON viene cancellata dal database: resta con
     * subscribed = false. Cosi' impostazioni come la conservazione
     * personalizzata sopravvivono a una risottoscrizione, ed e' anche il
     * comportamento che [subscribe] gia' si aspetta, visto che riusa la riga
     * esistente invece di crearne una nuova.
     *
     * Se [eliminaMessaggi] e' true si azzera anche l'avanzamento del sync:
     * senza quel passo una risottoscrizione futura non riscaricherebbe nulla,
     * perche' il server verrebbe interrogato solo per i numeri d'articolo
     * successivi all'ultimo gia' visto.
     */
    suspend fun unsubscribeGroup(groupId: Long, eliminaMessaggi: Boolean) {
        if (eliminaMessaggi) {
            db.articleDao().deleteByGroup(groupId)
            db.groupDao().resetSyncProgress(groupId)
            db.groupDao().refreshUnreadCount(groupId)
        }
        unsubscribe(groupId)
    }

    /**
     * Segna letto tutto il gruppo e RESTITUISCE gli id che prima erano da
     * leggere. Serve all'ANNULLA: rimettere semplicemente tutto a "non letto"
     * cancellerebbe anche le letture fatte davvero prima del tocco. Con la
     * fotografia si ripristina lo stato esatto di un istante prima.
     */
    suspend fun markAllRead(groupId: Long): List<Long> {
        val wereUnread = db.articleDao().unreadIds(groupId)
        // 1.4.1: passa da [segnaLetti] invece di `markAllRead` del DAO, cosi'
        // anche questo gesto propaga per Message-ID. L'effetto sul database e'
        // identico: `markAllRead` scrive read=1 su TUTTO il gruppo, ma le sole
        // righe che cambiano davvero sono quelle che erano a 0 — cioe'
        // esattamente [wereUnread].
        segnaLetti(wereUnread, listOf(groupId), true)
        return wereUnread
    }

    /**
     * Riporta l'intero gruppo a "da leggere" (nessun traffico di rete).
     *
     * 1.4.1 — PROPAGA, e la prima stesura sbagliava.
     *
     * Nella build 104 questo era l'unico punto che non passava da [segnaLetti],
     * per non far riapparire come non letti messaggi di altri gruppi. Dave
     * Royal ha chiesto "a quale problema serve?", e guardando meglio l'eccezione
     * non stava nemmeno in piedi da sola: l'ANNULLA di "segna tutto letto"
     * chiama `setReadBulk(..., false, ...)`, che passa da [segnaLetti] e quindi
     * propaga gia' — e deve, altrimenti annullerebbe a meta'. C'erano cosi' due
     * strade per rimettere a non letto, con due comportamenti diversi: proprio
     * la divergenza che [segnaLetti] esiste per eliminare.
     *
     * Ora sono una sola. La conseguenza e' reale e sta nelle note di versione:
     * "segna tutto da leggere" puo' far tornare non letti messaggi in altri
     * gruppi. E' il prezzo dichiarato della coerenza.
     */
    suspend fun markAllUnread(groupId: Long) {
        // solo le righe che cambiano davvero (le gia' lette), simmetrico di
        // `unreadIds` in [markAllRead]: evita di caricare gli id dell'intero
        // gruppo per toccarne una parte.
        segnaLetti(db.articleDao().readIds(groupId), listOf(groupId), false)
    }

    /**
     * Svuota la copia locale del gruppo e riparte da zero dal server.
     *
     * Da non confondere con "segna tutto da leggere": quella agisce solo sui
     * messaggi che hai gia' in memoria, questa li ributta via e li riprende
     * dal server. Serve quando i messaggi sono stati eliminati dalla
     * conservazione automatica, oppure quando la copia locale e' incoerente.
     *
     * Azzera anche l'avanzamento del sync incrementale: senza quel passo il
     * server verrebbe interrogato solo per i numeri d'articolo successivi
     * all'ultimo gia' visto, e non tornerebbe indietro nulla.
     */
    suspend fun reloadGroupFromScratch(groupId: Long): Int {
        db.articleDao().deleteByGroup(groupId)
        db.groupDao().resetSyncProgress(groupId)
        db.groupDao().refreshUnreadCount(groupId)
        return syncGroup(groupId)
    }

    /** Segna letti/non letti più articoli in blocco (es. un'intera discussione). */
    suspend fun setReadBulk(articleIds: List<Long>, read: Boolean, groupId: Long) {
        if (articleIds.isEmpty()) return
        // Gli articoli si leggono PRIMA di cambiarli: servono i loro Message-ID
        // per propagare alle copie negli altri gruppi (1.4).
        segnaLetti(articleIds, listOf(groupId), read)
    }

    suspend fun setRead(articleId: Long, read: Boolean) {
        val a = db.articleDao().byId(articleId) ?: return
        segnaLetto(a.id, a.groupId, read)
    }

    /**
     * 1.4.1 — L'UNICO PUNTO CHE MARCA "LETTO", e perche' esiste.
     *
     * Segnalazione di Dave Royal del 19 settembre, confermata da Marco: "il
     * letto segue il Message-ID" non funzionava. Non era rotto: era
     * **collegato nei posti sbagliati**. Lo stato `read` si scriveva in SETTE
     * punti del Repository e la propagazione era agganciata a DUE — quelli del
     * menu. I tre dentro [fetchBody], cioe' quelli che scattano quando si
     * LEGGE davvero un messaggio, non la chiamavano: la funzione non poteva
     * funzionare per nessuno.
     *
     * La correzione non e' stata aggiungere la chiamata nei punti mancanti —
     * sarebbe stata la settima copia della stessa regola, ed e' il difetto che
     * questo progetto ha gia' pagato quattro volte (NntpClient in quattro
     * punti, "e' mio post" in sei, gli appunti in cinque file, e questo). Qui
     * passano TUTTI, e chi un domani aggiungera' l'ottavo punto trovera' una
     * funzione da chiamare invece di tre righe da ricopiare.
     *
     * Fa le tre cose che devono stare insieme, nell'ordine che conta:
     *  1. legge id e Message-ID PRIMA di scrivere (dopo, il dato per propagare
     *     sarebbe gia' cambiato);
     *  2. scrive lo stato;
     *  3. aggiorna i contatori e propaga alle copie negli altri gruppi.
     *
     * Legge `id` e `messageId` e NON gli articoli interi: "segna tutto letto"
     * su un gruppo da migliaia di messaggi caricherebbe in memoria anche tutti
     * i corpi gia' scaricati.
     */
    private suspend fun segnaLetti(ids: List<Long>, gruppi: List<Long>, letto: Boolean) {
        if (ids.isEmpty()) return
        val origine =
            if (AppSettings.lettoSegueMessageId) db.articleDao().idEMessageId(ids) else emptyList()
        db.articleDao().setReadBulk(ids, letto)
        gruppi.distinct().forEach { db.groupDao().refreshUnreadCount(it) }
        propagaLettoPerMessageId(origine, letto)
    }

    /** Comodita' per il caso di gran lunga piu' frequente: un articolo solo. */
    private suspend fun segnaLetto(id: Long, gruppo: Long, letto: Boolean) =
        segnaLetti(listOf(id), listOf(gruppo), letto)

    /**
     * 1.4 — IL "LETTO" SEGUE IL MESSAGE-ID.
     *
     * Un messaggio crospostato e' UN messaggio, anche se noi lo teniamo in N
     * righe (una per gruppo). Leggerlo in un gruppo e ritrovarlo non letto
     * negli altri due significa rileggerlo — o, peggio, non capire perche' il
     * contatore non scende.
     *
     * DA DOVE VIENE LA DECISIONE. Non da una nostra idea: MesNews fa cosi', e
     * la fonte e' Marco, che lo usa da PC tutti i giorni. NeReO nasce per
     * assomigliare a MesNews. Chiude anche un'incoerenza nostra: "seguito"
     * (watched) e i filtri ragionano gia' per Message-ID — faceva eccezione
     * solo il letto.
     *
     * COSTO: una query su un indice che esiste gia', e solo quando un articolo
     * cambia stato. Le copie tipiche sono due o tre; se non ce ne sono, la
     * funzione esce subito e non tocca niente.
     *
     * Si aggiornano i contatori SOLO dei gruppi davvero toccati, non di tutti.
     */
    private suspend fun propagaLettoPerMessageId(origine: List<IdMessageId>, read: Boolean) {
        if (!AppSettings.lettoSegueMessageId) return
        val idOrigine = origine.map { it.id }.toHashSet()
        val mids = origine.map { it.messageId }.filter { it.isNotBlank() }.distinct()
        if (mids.isEmpty()) return
        val copie = mids.flatMap { db.articleDao().tutteLeCopie(it) }.filter { it.id !in idOrigine }
        if (copie.isEmpty()) return
        db.articleDao().setReadBulk(copie.map { it.id }, read)
        copie.map { it.groupId }.distinct().forEach { db.groupDao().refreshUnreadCount(it) }
    }


    suspend fun toggleWatched(articleId: Long) {
        val a = db.articleDao().byId(articleId) ?: return
        db.articleDao().setWatched(articleId, !a.watched)
    }

    /** 2.1(a): "segui la discussione" agisce su TUTTO il thread, non sul singolo
     *  articolo — così la stella compare sulla radice (la discussione) qualunque
     *  sia il post da cui la si attiva. */
    suspend fun setWatchedBulk(ids: List<Long>, watched: Boolean) {
        db.articleDao().setWatchedBulk(ids, watched)
    }

    /** #4c: per il backup — i Message-ID delle discussioni seguite. */
    suspend fun watchedMessageIdsOnce(): List<String> = db.articleDao().watchedMessageIds()

    /** #4c: al ripristino — marca "seguito" gli articoli PRESENTI coi MID dati
     *  (best-effort: quelli non ancora riscaricati non ci sono ancora). */
    suspend fun markWatchedByMessageIds(mids: List<String>) =
        db.articleDao().setWatchedByMessageIds(mids)

    // ------------------------------------------ M1: propagazione alle risposte

    /**
     * M1 (release personale Marco): applica ai [candidati] le regole che
     * estendono il proprio effetto alle risposte, nascondendo chi discende da un
     * post filtrato. Ritorna quanti articoli sono stati nascosti.
     *
     * Cosa NON fa, di proposito:
     *  - non tocca i semi (li ha gia' gestiti FilterEngine col suo punteggio);
     *  - non tocca i messaggi seguiti (watched): una discussione che hai
     *    marcato esplicitamente vince sempre sul filtro automatico;
     *  - non cancella nulla. Chi risponde a un troll non ha colpe: viene reso
     *    invisibile, non distrutto. In NeReO `hidden` esclude gia' l'articolo da
     *    elenco, conteggi non letti, totali di gruppo e ricerca, quindi a schermo
     *    il risultato e' identico alla cancellazione — ma resta reversibile e,
     *    soprattutto, la catena locale resta intatta per la chiusura transitiva.
     */
    private suspend fun propagaAlleRisposte(
        group: Group,
        engine: FilterEngine,
        candidati: List<Article>,
    ): Int {
        val regole = engine.poisoningRulesForGroup(group.name)
        if (regole.isEmpty() || candidati.isEmpty()) return 0

        // I seguiti sono intoccabili: li tolgo subito dai candidati.
        val valutabili = candidati.filter { !it.watched }
        if (valutabili.isEmpty()) return 0

        val input = valutabili.map {
            PropagationInput(
                messageId = it.messageId,
                references = it.references,
                dateEpoch = it.dateEpoch,
            )
        }

        var totale = 0
        // 1.5.1: le risposte da SEGUIRE si marcano tutte INSIEME, DOPO il giro
        // su tutte le regole. Non per efficienza: `watched` e' protettivo (i
        // seguiti escono da `valutabili` qui sopra, e da applicaRegoleLettore e
        // dalla conservazione), quindi marcarle a meta' giro cambierebbe l'esito
        // delle regole successive. Vedi NeReO_1.4.8_142_filtri_che_aggiungono.md.
        val daSeguire = LinkedHashSet<String>()
        for (regola in regole) {
            // semi presenti fra i candidati di questo giro
            val semi = valutabili.asSequence()
                .filter { a ->
                    regola.matchesArticle(
                        ScorableArticle(
                            a.fromField, a.subject, a.messageId,
                            newsgroups = group.name,
                            body = a.body,
                            references = a.references,
                        )
                    )
                }
                .map { it.messageId }
                .toSet()

            // contesto storico: cio' che questa regola ha gia' avvelenato
            val registro = db.poisonedIdDao().idsByGroupAndRule(group.id, regola.id).toSet()
            if (semi.isEmpty() && registro.isEmpty()) continue

            // Registra i semi SOPRAVVISSUTI (quelli non cancellati, tipici del
            // plonk rapido che nasconde invece di eliminare). I semi eliminati
            // vengono registrati dai chiamanti PRIMA della cancellazione, quando
            // esistono ancora: qui sarebbe troppo tardi.
            val semiDaRegistrare = semi - registro
            if (semiDaRegistrare.isNotEmpty()) {
                val perSeme = valutabili.associateBy { it.messageId }
                db.poisonedIdDao().insertAll(
                    semiDaRegistrare.map { mid ->
                        PoisonedId(
                            messageId = mid,
                            groupId = group.id,
                            epoch = perSeme[mid]?.dateEpoch ?: 0L,
                            ruleId = regola.id,
                            seed = true,
                        )
                    }
                )
            }

            val colpiti = FollowupPropagator.propagate(
                articles = input.map { it.copy(seedPoisoned = it.messageId in semi) },
                alreadyPoisoned = registro,
                mode = regola.followup,
            )
            if (colpiti.isEmpty()) continue

            // 1.5.1 (§A12-retro meta' B): la propagazione SEGUE L'AZIONE della
            // regola. Prima qui c'era `hidden = true` fisso col punteggio -100
            // di default: una regola «Segui» o «Evidenzia» estesa alle risposte
            // NASCONDEVA i rami che si volevano vedere. La decisione sta in
            // FilterEngine.effettoPropagazioneDi (Kotlin puro, provata con
            // kotlinc): per le regole negative non cambia NIENTE.
            val effetto = regola.effettoPropagazione
            val elenco = colpiti.toList()
            elenco.chunked(500).forEach {
                if (effetto.nascondi) {
                    db.articleDao().setHiddenAndScoreByMessageIds(it, hidden = true, score = effetto.punteggio)
                } else {
                    db.articleDao().setScoreVisibiliByMessageIds(it, effetto.punteggio)
                }
            }
            if (effetto.segui) daSeguire.addAll(elenco)

            val perId = valutabili.associateBy { it.messageId }
            db.poisonedIdDao().insertAll(
                elenco.map { mid ->
                    PoisonedId(
                        messageId = mid,
                        groupId = group.id,
                        epoch = perId[mid]?.dateEpoch ?: 0L,
                        ruleId = regola.id,
                        seed = false,
                    )
                }
            )
            // il chiamante legge questo numero come «nascosti» (aggiorna i
            // contatori dei non letti): le risposte evidenziate o seguite non
            // ci vanno.
            if (effetto.nascondi) totale += elenco.size
        }
        // 1.5.1: la stella, PER ULTIMA (vedi `daSeguire` sopra).
        daSeguire.toList().chunked(500).forEach { db.articleDao().setWatchedByMessageIds(it) }
        return totale
    }

    /**
     * M1: revoca di una regola propagante — rimette in chiaro le risposte che
     * aveva nascosto e ne dimentica il registro.
     *
     * Ri-mostra SOLO le righe con seed = 0, cioe' quelle colpite per
     * propagazione: i post dell'autore filtrato non vanno certo riesumati (e
     * comunque, se erano in KILL, non esistono piu' in locale).
     *
     * ⚠️ 1.5.3: «non vanno certo riesumati» valeva per il plonk, ed era il
     * difetto per «Ignora»: tolta la regola, l'autore restava nascosto fino a
     * un riscarico. I post dell'autore ora li decide la revisione che segue
     * ogni salvataggio e cancellazione (rivediDopoCambioRegole): tornano in
     * chiaro se NESSUNA regola rimasta li nasconde. Questa funzione resta per
     * le sole risposte, che hanno un registro proprio.
     */
    private suspend fun revocaPropagazione(ruleId: Long) {
        val daRimostrare = db.poisonedIdDao().propagatedIdsExclusiveToRule(ruleId)
        // 1.5.1 (§A12-retro meta' B): la revoca e' SDOPPIATA. Si guarda la
        // regola COM'ERA SALVATA (la si legge prima che saveFilter la
        // sovrascriva o deleteFilter la cancelli): se nascondeva, si rimette
        // in chiaro come sempre; se era positiva, `unhideByMessageIds` sarebbe
        // un no-op sbagliato — le risposte non erano nascoste — e si azzera
        // invece il punteggio che aveva scritto.
        // ⚠️ Le STELLE NON si tolgono: possono essere state messe a mano, e
        // toglierle in blocco sarebbe peggio del difetto. Limite dichiarato.
        val vecchia = db.filterDao().allOnce().firstOrNull { it.id == ruleId }
        val nascondeva = vecchia == null ||
            effettoPropagazioneDi(vecchia.score, vecchia.followupScore).nascondi
        // 1.5.3: il ramo POSITIVO non scrive piu' niente qui. Azzerare il
        // punteggio PRIMA della revisione cancellava l'unica traccia che dice
        // «questa stella l'ha messa la regola» (punteggio >= soglia WATCH), e
        // le risposte seguite tenevano la stella per sempre. Ora tolta la
        // riga dal registro, la revisione che segue (rivediDopoCambioRegole)
        // le tratta come i match diretti: colore via, stella della regola via.
        if (nascondeva) {
            daRimostrare.chunked(500).forEach { db.articleDao().unhideByMessageIds(it) }
        }
        db.poisonedIdDao().deleteByRule(ruleId)
        NntpLog.diag(
            "Filtri: regola #$ruleId, revoca della propagazione: ${daRimostrare.size} risposte " +
                (if (nascondeva) "rimesse in chiaro" else "lasciate alla revisione (regola positiva)"),
        )
    }

    // ---------------------------------------------------------------- plonk

    /**
     * PLONK: mette l'autore dell'articolo nel killfile (score -9999, quindi
     * i suoi futuri articoli non vengono nemmeno salvati) e nasconde
     * retroattivamente quelli già scaricati. Ritorna il pattern usato.
     */
    suspend fun plonkAuthor(articleId: Long, estendiAlleRisposte: Boolean = true): String? {
        val article = db.articleDao().byId(articleId) ?: return null
        return plonkAuthorByFrom(article.fromField, article.groupId, estendiAlleRisposte)
    }

    /** Mette nel killfile l'autore [fromField] (idempotente) e nasconde i suoi messaggi. */
    private suspend fun plonkAuthorByFrom(
        fromField: String,
        groupId: Long,
        estendiAlleRisposte: Boolean = true,
    ): String? {
        // se c'è un'email nel From uso quella (più stabile del nome visualizzato)
        val pattern = Regex("<([^>]+)>").find(fromField)?.groupValues?.get(1)
            ?: fromField.trim().ifEmpty { return null }
        // M1: il plonk rapido puo' essere secco o esteso alle risposte. La
        // scelta arriva dalla casella del dialogo di conferma; i menu
        // contestuali, che non hanno dialogo, usano il default (esteso).
        val modo = if (estendiAlleRisposte) FollowupMode.SUBTREE.name else FollowupMode.NONE.name

        val esistente = db.filterDao().allOnce().firstOrNull {
            it.field == FilterField.FROM.name && it.matchType == MatchType.CONTAINS.name &&
                it.pattern == pattern && it.score <= -9999
        }
        if (esistente == null) {
            db.filterDao().upsert(
                FilterRuleEntity(
                    name = "Plonk: $fromField",
                    field = FilterField.FROM.name,
                    matchType = MatchType.CONTAINS.name,
                    pattern = pattern,
                    score = -9999,
                    followup = modo,
                )
            )
        } else if (estendiAlleRisposte && esistente.followup == FollowupMode.NONE.name) {
            // Autore gia' plonkato in modo secco e ora lo si vuole esteso:
            // si promuove la regola invece di crearne una seconda identica.
            // Il caso opposto (esteso -> secco) NON viene gestito qui: sarebbe
            // un declassamento silenzioso, e si fa dalla schermata filtri
            // togliendo la spunta, dove si vede cosa si sta facendo.
            db.filterDao().upsert(esistente.copy(followup = modo))
        }
        // 1.2.12 (audit di sicurezza): `pattern` viene dal campo From di un
        // articolo, cioè da chi lo ha pubblicato. In un LIKE, `%` e `_` sono
        // metacaratteri: un From come `Troll <%>` produceva `LIKE '%%%'`, vero
        // su ogni riga, e nascondeva l'INTERO database in tutti i gruppi (senza
        // alcun modo comodo per tornare indietro). Peggio: `maybeAutoPlonkByBody`
        // può innescare questa strada da sola, senza che l'utente tocchi nulla,
        // se il corpo contiene una parola che l'utente filtra. Qui si
        // neutralizzano i metacaratteri; la clausola ESCAPE sta nella query.
        // 1.4.8 (par. U2): la neutralizzazione, che era scritta qui, ora sta in
        // [testoPerLike] — serviva anche alla ricerca, ed e' il codice che si
        // SPOSTA, non si ricopia.
        db.articleDao().hideByAuthorLike("%${testoPerLike(pattern)}%", score = -9999)

        // M1: il plonk esteso deve agire SUBITO anche sulle risposte gia'
        // scaricate, altrimenti l'utente plonka il troll e si ritrova ancora
        // sotto gli occhi mezzo thread che lo cita.
        val gruppo = if (estendiAlleRisposte) db.groupDao().byId(groupId) else null
        if (gruppo != null) {
            val engine = buildEngine()
            propagaAlleRisposte(gruppo, engine, db.articleDao().byGroupOnce(groupId))
        }

        db.groupDao().refreshUnreadCount(groupId)
        return pattern
    }

    /**
     * Regole "corpo" (auto-plonk per contenuto): se il corpo appena scaricato
     * contiene il pattern di una regola BODY nello scope del gruppo, l'autore
     * finisce nel killfile — blocco per CONTENUTO, non per nick.
     */
    private suspend fun maybeAutoPlonkByBody(article: Article, group: String, body: String) {
        val bodyRules = db.filterDao().allOnce().map { it.toRule() }
            .filter { it.enabled && it.field == FilterField.BODY }
        if (bodyRules.isEmpty()) return
        val today = java.time.LocalDate.now().toEpochDay()
        for (r in bodyRules) {
            if (r.expiresEpochDay != null && r.expiresEpochDay < today) continue
            val inScope = r.groupWildmat == "*" ||
                Regex(FilterRule.wildcardToRegex(r.groupWildmat)).matches(group)
            if (inScope && r.matches(body)) {
                plonkAuthorByFrom(article.fromField, article.groupId)
                break
            }
        }
    }

    // ---------------------------------------------------------------- posting

    /**
     * 1.2.12 (audit di sicurezza) — riduce un valore a UNA SOLA RIGA, prima che
     * finisca dentro un'intestazione di un articolo da inviare.
     *
     * Un articolo si compone come righe di testo: `Nome: valore`. Se `valore`
     * contiene un a-capo, quello che segue diventa una RIGA DI INTESTAZIONE IN
     * PIÙ, scritta da qualcun altro dentro un messaggio firmato dall'utente
     * (per esempio un `Control: cancel` che ritira l'articolo di un terzo). Se
     * poi contiene anche una riga fatta di un solo punto, chiude il POST in
     * anticipo e il resto viene letto dal server come COMANDI NNTP, dentro una
     * sessione già autenticata.
     *
     * Non tutti questi valori vengono dall'utente: References e Supersedes
     * derivano da articoli altrui, i nomi dei gruppi del crosspost pure, e
     * indirizzo/User-Agent/intestazioni personalizzate possono arrivare da un
     * file di backup ripristinato. Nessuno di questi è dato fidato: si ripulisce
     * qui, in un punto solo, invece di sperare che ogni sorgente sia pulita.
     */
    private fun unaRiga(s: String): String = s.replace('\r', ' ').replace('\n', ' ').trim()

    /**
     * Invia un articolo (nuovo thread o risposta) sul gruppo indicato.
     * [referencesHeader] va valorizzato solo per le risposte:
     * References del padre + Message-ID del padre.
     */
    suspend fun postArticle(
        groupId: Long,
        fromName: String,
        fromEmail: String,
        subject: String,
        body: String,
        referencesHeader: String? = null,
        // 1.3.2: era "OpenUsenet (Android; GPLv3)" — un valore predefinito
        // rimasto indietro di due cambi di nome, e per giunta nella forma
        // sbagliata secondo la RFC 5536. Non si vedeva perché entrambi i
        // chiamanti passano sempre `ident.userAgent`: era un valore morto che
        // aspettava solo un terzo chiamante distratto. Ora il valore di
        // ricaduta è "nessuno", che è anche la scelta più prudente.
        userAgent: String? = null,
        xNoArchive: Boolean = false,
        customHeaders: String? = null,
        faceB64: String? = null,
        /** Message-ID da sostituire: se valorizzato aggiunge l'header Supersedes. */
        supersedesId: String? = null,
        /**
         * beta17 (Bingo3331): gruppi AGGIUNTIVI su cui crospostare la risposta,
         * oltre al gruppo corrente (`groupId`). Nomi di gruppo, non id: i gruppi
         * del crosspost possono non essere sottoscritti localmente. Vuoto (default)
         * = comportamento storico, risposta al solo gruppo corrente. Cap a 3 TOTALI
         * imposto qui come rete di sicurezza (l'UI già limita la selezione).
         */
        extraGroups: List<String> = emptyList(),
        /**
         * 1.4 (`Followup-To`) — la riga `Newsgroups:` DECISA DALL'AUTORE
         * dell'articolo a cui si risponde. Quando e' valorizzata sostituisce
         * del tutto "gruppo corrente + [extraGroups]": e' il solo modo di
         * rispettare un header che significa "la discussione prosegue altrove".
         *
         * null (default) = comportamento storico, invariato.
         */
        gruppiEspliciti: List<String>? = null,
    ): Unit = withContext(Dispatchers.IO) {
        val group = db.groupDao().byId(groupId) ?: error("Group not found")
        val server = db.serverDao().byId(group.serverId) ?: error("Server not found")

        // 1.2.12 (audit di sicurezza): un Supersedes può ritirare l'articolo
        // che nomina, quindi vale solo sui PROPRI. Il controllo esisteva solo
        // nell'interfaccia (la voce di menu, nascosta sugli articoli altrui):
        // qui si ripete a livello di dato, perché il Repository non deve
        // fidarsi di chi lo chiama — stessa scelta già fatta in cancelArticle.
        supersedesId?.trim()?.takeIf { it.isNotEmpty() }?.let { sid ->
            val originale = db.articleDao().byMessageId(unaRiga(sid))
            if (originale != null && !AppSettings.eMioPost(originale.fromField)) {
                error("Si possono sostituire solo i propri messaggi")
            }
        }

        nuovoClient(server).use { client ->
            try {
                client.connect()
            } finally {
                annotaIdentitaTls(server, client)
            }
            preparaConnessione(client, server)
            val headers = buildList {
                // 1.2.12: ogni valore che entra in un'intestazione passa da
                // unaRiga(). fromName e subject erano già al sicuro per effetto
                // collaterale (encodeWord codifica in base64 appena compare un
                // carattere non stampabile, quindi anche un a-capo) — ma per
                // effetto collaterale, non per scelta: ora è esplicito.
                // 1.3.6: encodeWord puo' ora restituire PIU' encoded-word
                // separate da CRLF+spazio (RFC 2047 §2: tetto di 75 caratteri).
                // NntpClient.post scrive un elemento per riga, quindi il valore
                // va spezzato qui: le righe di continuazione cominciano gia'
                // con uno spazio e sono a norma cosi' come sono.
                fun intestazione(nome: String, valore: String) {
                    ("$nome: $valore").split("\r\n").forEach { add(it) }
                }
                intestazione("From", "${MimeUtil.encodeWord(unaRiga(fromName))} <${unaRiga(fromEmail)}>")
                // beta17: Newsgroups = gruppo corrente + eventuali gruppi extra
                // (crosspost legittimo). Dedup preservando l'ordine (corrente
                // primo), pulizia degli spazi, e cap di sicurezza a 3 gruppi
                // TOTALI: oltre, si troncano — MAI si cancella il testo (il blocco
                // "duro" oltre-cap è nell'UI; qui è solo la rete di sicurezza).
                //
                // 1.4 (Followup-To): [gruppiEspliciti], quando c'e', SOSTITUISCE
                // "gruppo corrente + extra". E' l'unico modo di rispettare
                // davvero un Followup-To, che significa "la discussione prosegue
                // ALTROVE": se il gruppo di lettura restasse sempre in testa,
                // staremmo dicendo di aver rispettato l'header mentre
                // continuiamo a pubblicare dove l'autore ha chiesto di non
                // farlo. Il server di destinazione resta quello del gruppo
                // aperto — cambia dove esce il messaggio, non da dove parte.
                val gruppiRisposta = (gruppiEspliciti ?: (listOf(group.name) + extraGroups))
                    .map { unaRiga(it) }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .take(3)
                add("Newsgroups: ${gruppiRisposta.joinToString(",")}")
                intestazione("Subject", MimeUtil.encodeWord(unaRiga(subject)))
                // 1.5.5 / 171 (segnalazione su Usenet, nl.politiek): il References
                // passava COSI' COM'ERA. Ora un punto solo, per tutte le strade che
                // arrivano qui (risposta a pagine e a pannelli, supersede, bozza
                // ripresa): doppioni tolti e, oltre i 998 caratteri, accorciato
                // tenendo la radice e gli antenati piu' recenti (RFC 5537 §3.4.4).
                // Vedi thread/ReferencesInUscita.kt.
                referencesHeader?.takeIf { it.isNotBlank() }?.let { grezzo ->
                    val prima = Threader.idNelleReferences(grezzo).size
                    val pronto = referencesInUscita(grezzo)
                    val dopo = if (pronto.isEmpty()) 0 else pronto.split(' ').size
                    if (dopo != prima) {
                        NntpLog.diag("References: $prima identificativi -> $dopo (doppioni e tetto di 998 caratteri)")
                    }
                    if (pronto.isNotEmpty()) add("References: $pronto")
                }
                add("MIME-Version: 1.0")
                // 1.3.6 — SI DICHIARA CIO' CHE SI MANDA DAVVERO.
                //
                // Fino alla 1.3.5 era sempre "charset=UTF-8; 8bit", anche per un
                // corpo di puro ASCII. Non e' falso (l'ASCII e' un
                // sottoinsieme dell'UTF-8) ma e' meno preciso del necessario, e
                // un lettore d'epoca ha diritto a sapere che puo' trattare il
                // messaggio come 7 bit. Richiesta di Adam H. Kerman: «If it's
                // pure ASCII, mark it as such».
                //
                // IL CONTROLLO VA FATTO QUI, NON PRIMA: a questo punto il corpo
                // contiene GIA' la firma, che viene aggiunta in composizione.
                // Deciderlo un passo prima significherebbe dichiarare us-ascii e
                // spedire una firma con una lettera accentata dentro — la
                // trappola che Kerman ha segnalato nello stesso messaggio
                // («don't forget to parse after appending the .sigfile, a
                // failing of many newsreaders»).
                val soloAscii = body.all { it.code in 0..127 }
                // 1.4.8 (par. B6b) — SI DICHIARA f=f SOLO SE C'E' DAVVERO.
                //
                // Il predicato e' lo STESSO che il lettore usa per decidere se
                // fidarsi del formato dichiarato da un altro
                // (`almenoUnaRigaContinua`, estratto da `usaFlowed`): cosi' non
                // possiamo dichiarare un formato che il nostro stesso lettore
                // scarterebbe come vuoto — che e' esattamente quello che fa
                // MesNews e che ci e' costato il difetto di par. B6a-bis.
                //
                // E si guarda IL CORPO, non l'impostazione: a questo punto il
                // corpo e' definitivo, firma compresa (stesso argomento del
                // controllo ASCII qui sopra). Un messaggio tutto di righe corte
                // non ha nessuna rottura debole, quindi non dichiara niente,
                // qualunque sia l'interruttore.
                //
                // ⚠️ DEBITO DICHIARATO: questo import viene da `ui.screens`,
                // cioe' un livello piu' in su. `WrapBody.kt` e
                // `FormatFlowed.kt` sono Kotlin puro e la loro casa giusta e'
                // un package `text`; ci stanno in `ui.screens` per ragioni
                // storiche. Spostarli tocca molti import e non si fa dentro
                // una release per il resto chiusa — e' una voce del quaderno.
                val corpoFlowed = almenoUnaRigaContinua(body.lines())
                add(
                    "Content-Type: text/plain; charset=" +
                        (if (soloAscii) "us-ascii" else "UTF-8") +
                        (if (corpoFlowed) "; format=flowed" else "")
                )
                add("Content-Transfer-Encoding: " + if (soloAscii) "7bit" else "8bit")
                // privacy: User-Agent solo se dichiarato (vuoto/null = anonimo)
                // 1.2.13: i segnaposto di versione (%nereo%, %versione%) si
                // risolvono QUI, al momento dell'invio — non alla creazione
                // dell'identità. Così lo User-Agent dichiarato dice sempre la
                // versione con cui il messaggio è stato scritto davvero, invece
                // di congelare per sempre quella del giorno in cui l'identità è
                // stata creata. Vuoto resta vuoto: nessun User-Agent, come
                // dalla correzione W della 1.2.9.
                userAgent?.let { unaRiga(Branding.espandiVersione(it)) }
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { add("User-Agent: $it") }
                if (xNoArchive) add("X-No-Archive: yes")
                // Supersedes: sostituisce un proprio articolo con questa versione.
                // Il server accetta il nuovo articolo e, se rispetta il meccanismo,
                // ritira il precedente. Non tutti i server lo onorano.
                supersedesId?.let { unaRiga(it) }?.takeIf { it.isNotEmpty() }?.let { add("Supersedes: $it") }
                // header personalizzati (stile MesNews): "Nome: valore", uno per
                // riga. 1.4.1: la regola vive in [IntestazioniPersonalizzate], un
                // posto solo — perche' l'editor deve poter dire IN ANTICIPO cosa
                // verra' scartato, e due copie della stessa regola divergono.
                IntestazioniPersonalizzate.valide(customHeaders).forEach { (nome, valore) ->
                    add("${unaRiga(nome)}: ${unaRiga(valore)}")
                }
                // Face (PNG 48x48 base64) con folding: prima riga + continuazioni indentate
                faceB64?.filterNot { it.isWhitespace() }?.takeIf { it.isNotBlank() }?.let { face ->
                    // 1.2.12: via ogni spaziatura (a-capo compresi) prima di
                    // ripiegare la riga — il base64 non ne contiene per natura,
                    // e un Face arrivato da un backup ripristinato potrebbe.
                    val chunks = face.chunked(72)
                    add("Face: ${chunks.first()}")
                    chunks.drop(1).forEach { add(" $it") }
                }
            }
            client.post(headers + "" + body.lines())
        }
    }

    /**
     * Invia un MESSAGGIO DI CONTROLLO "cancel" (RFC 5537 §5.3) per ritirare un
     * PROPRIO articolo già pubblicato. 1.2.11 (richiesta di Marco, 6 settembre
     * 2026), complemento di Supersedes (che invece pubblica una versione
     * sostitutiva).
     *
     * Non è un comando NNTP a sé: è un normale POST di un articolo che porta
     * l'header `Control: cancel <Message-ID>` — stessa strada già percorsa da
     * [postArticle], nessuna capacità nuova richiesta al server.
     *
     * ONESTÀ SUI LIMITI (da dire all'utente, non da scoprire dopo):
     * - il server decide se onorarlo. Molti provider moderni ignorano o
     *   filtrano i cancel (storicamente abusati da terzi per cancellare i
     *   messaggi altrui); un 240 in risposta significa "articolo di controllo
     *   accettato", NON "articolo cancellato".
     * - anche quando onorato, la propagazione agli altri server della rete non
     *   è garantita: una copia può sopravvivere altrove.
     * - la copia LOCALE non viene toccata di proposito: cancellarla darebbe
     *   l'impressione di un successo che non possiamo verificare. Sparirà da
     *   sola alla prossima risincronizzazione del gruppo, se il server ha
     *   davvero ritirato l'articolo.
     *
     * Difesa a livello di dato (non solo di interfaccia): la stessa verifica
     * "è mio?" che nasconde la voce di menu viene ripetuta qui, perché il
     * Repository non deve fidarsi di chi lo chiama.
     */
    suspend fun cancelArticle(articleId: Long): List<EsitoRitiroServer> = withContext(Dispatchers.IO) {
        val article = db.articleDao().byId(articleId) ?: error("Message not found")
        if (article.messageId.isBlank()) error("Message without a Message-ID: it cannot be cancelled")
        if (!AppSettings.eMioPost(article.fromField)) error("Si possono annullare solo i propri messaggi")
        val group = db.groupDao().byId(article.groupId) ?: error("Group not found")
        val server = db.serverDao().byId(group.serverId) ?: error("Server not found")

        // From: deve coincidere con quello dell'articolo originale, altrimenti
        // un server che pure onora i cancel lo rifiuta. Prendo l'INDIRIZZO
        // dall'originale (è già stato verificato come mio) e il nome
        // dall'identità che lo possiede, così la riga viene formata e
        // codificata dalle stesse funzioni usate per postare normalmente.
        // 1.2.12: stessa funzione che decide "è mio" (AppSettings.eMioPost), non
        // una regex a parte. Prima si prendeva la PRIMA email che compariva
        // nella riga From: su un From costruito ad arte quella poteva essere
        // l'indirizzo della vittima messo lì apposta dentro il nome
        // visualizzato, mentre il mittente vero era un altro. Chi verifica e
        // chi usa devono guardare lo stesso identico dato.
        val indirizzo = AppSettings.indirizzoDelFrom(article.fromField)
            ?: error("Sender address not recognised in the original article")
        val identita = identitiesOnce()
            .firstOrNull { it.email.trim().equals(indirizzo, ignoreCase = true) }
            ?: resolvedIdentity(group.serverId)
        val nome = identita.name.ifBlank { indirizzo.substringBefore("@") }

        // Newsgroups: quelli dell'ORIGINALE quando li conosciamo (un crosspost
        // va ritirato in TUTTI i gruppi in cui è comparso, non solo in quello
        // da cui lo stiamo guardando); le intestazioni complete sono già in
        // locale se il corpo è stato scaricato almeno una volta.
        val gruppiOriginale = article.headersRaw
            ?.lineSequence()
            ?.firstOrNull { it.startsWith("Newsgroups:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: group.name

        val messageIdPulito = unaRiga(article.messageId)
        val gruppiPuliti = unaRiga(gruppiOriginale)

        val esitoIniezione = openClient(server) { client ->
            val headers = listOf(
                "From: ${MimeUtil.encodeWord(nome)} <$indirizzo>",
                "Newsgroups: $gruppiPuliti",
                // convenzione storica ("cmsg"): l'oggetto ripete il comando di
                // controllo, così resta leggibile anche ai lettori che mostrano
                // gli articoli di controllo invece di eseguirli.
                "Subject: cmsg cancel $messageIdPulito",
                "Control: cancel $messageIdPulito",
                "MIME-Version: 1.0",
                "Content-Type: text/plain; charset=UTF-8",
                "Content-Transfer-Encoding: 8bit",
            )
            client.post(headers + "" + listOf("Article cancelled by its author."))

            // 1.2.15 (voce C) — VERIFICA CHE IL RITIRO SIA AVVENUTO.
            //
            // Fino alla 1.2.14 dicevamo "richiesta inviata, il server potrebbe
            // ignorarla" e lì finiva: chi ritirava un messaggio su Astraweb
            // restava convinto di averlo tolto, e l'articolo era ancora lì.
            // Non è un'ipotesi, è la prova sul campo del 6 settembre — OVH
            // (INN 2.7.3) onora e propaga i messaggi di controllo, Astraweb li
            // ignora completamente. STAT per Message-ID costa un comando e
            // trasforma una promessa in un riscontro.
            //
            // Due tentativi: molti server elaborano la coda dei messaggi di
            // controllo in modo asincrono, e chiedere nello stesso istante in
            // cui si è inviato darebbe un "ancora presente" ingiusto. Due
            // secondi sono un compromesso — abbastanza per un INN scarico,
            // abbastanza poco da non far sembrare l'app bloccata. Se dopo la
            // pausa l'articolo c'è ancora, la risposta onesta è "ancora
            // presente", NON "non ha funzionato": magari la coda è solo lenta,
            // e il testo mostrato all'utente lo dice.
            var presente = runCatching { client.statPerMessageId(messageIdPulito) }.getOrNull()
            if (presente == true) {
                kotlinx.coroutines.delay(2_000)
                presente = runCatching { client.statPerMessageId(messageIdPulito) }.getOrNull()
            }
            esitoDa(presente)
        }

        // 1.2.16 (Piano B) — E SUGLI ALTRI SERVER?
        //
        // La verifica della 1.2.15 diceva la verità, ma su UN server solo. La
        // domanda che l'utente si pone davvero è un'altra, ed è quella a cui
        // Marco ha risposto tre volte a mano, aprendo MesNews sul PC: "sugli
        // altri è sparito?". Le prove del 6-7 settembre dicono quasi sempre di
        // no (il ritiro agisce, in pratica, solo sul server di iniezione), ma
        // "quasi sempre" non è "sempre", e comunque saperlo vale più che
        // dedurlo.
        //
        // Ogni server è interrogato con UNA connessione, solo qui e solo ora:
        // nessun lavoro in sottofondo, nessuna connessione tenuta aperta. Un
        // server irraggiungibile diventa NON_VERIFICABILE e non fa fallire il
        // resto — l'esito del ritiro vero e proprio è già stato ottenuto.
        val altri = db.serverDao().allOnce().filter { it.id != server.id }
        val esitiAltri = altri.map { altro ->
            val presente = runCatching {
                openClient(altro) { c -> c.statPerMessageId(messageIdPulito) }
            }.getOrNull()
            EsitoRitiroServer(
                server = altro.name.ifBlank { altro.host },
                esito = esitoDa(presente),
                iniezione = false,
            )
        }

        listOf(
            EsitoRitiroServer(
                server = server.name.ifBlank { server.host },
                esito = esitoIniezione,
                iniezione = true,
            ),
        ) + esitiAltri
    }

    /**
     * 1.2.16: traduzione unica da "risposta di STAT" a esito. `null` significa
     * "non lo so" e arriva da due strade diverse — il server ha risposto in un
     * modo che non sappiamo leggere, oppure non siamo riusciti a parlargli —
     * che per l'utente sono la stessa cosa: non possiamo dirglielo.
     */
    private fun esitoDa(presente: Boolean?): EsitoRitiro = when (presente) {
        false -> EsitoRitiro.RITIRATO
        true -> EsitoRitiro.ANCORA_PRESENTE
        null -> EsitoRitiro.NON_VERIFICABILE
    }

    // ---------------------------------------------------------------- filtri

    /**
     * M1: salvando una regola gia' esistente se ne butta via il registro di
     * propagazione. Il pattern potrebbe essere cambiato: tenere gli avvelenati
     * vecchi lascerebbe nascosti messaggi che la regola nuova non rivendica
     * piu', senza alcun modo per l'utente di capire perche'.
     */
    // 1.5.3: NonCancellable — lo scope della schermata Filtri muore se si esce
    // a meta' del giro, e una regola cancellata con i suoi messaggi ancora
    // nascosti sarebbe esattamente il difetto che questa versione corregge.
    suspend fun saveFilter(rule: FilterRuleEntity): Long =
        withContext(NonCancellable + Dispatchers.IO) {
            filtriMutex.withLock {
                if (rule.id != 0L) revocaPropagazione(rule.id)
                val id = db.filterDao().upsert(rule)
                NntpLog.diag(
                    "Filtri: regola #${if (rule.id != 0L) rule.id else id} salvata " +
                        "(${rule.field} ${rule.matchType} punteggio ${rule.score}, " +
                        "propagazione ${rule.followup}, ambito ${rule.groupWildmat}, " +
                        "attiva ${rule.enabled})",
                )
                rivediDopoCambioRegole(db.groupDao().allSubscribedOnce(), buildEngine(), "salvataggio")
                id
            }
        }

    /**
     * M1: cancellando la regola si ri-mostrano le risposte che aveva nascosto.
     * 1.5.3: e anche i messaggi che nascondeva, colorava o seguiva DIRETTAMENTE
     * (vedi [rivediDopoCambioRegole]): prima restavano com'erano, e per
     * rivedere un autore tolto da «Ignora» bisognava riscaricare dal server.
     */
    suspend fun deleteFilter(id: Long) {
        withContext(NonCancellable + Dispatchers.IO) {
            filtriMutex.withLock {
                revocaPropagazione(id)
                db.filterDao().delete(id)
                NntpLog.diag("Filtri: regola #$id eliminata")
                rivediDopoCambioRegole(db.groupDao().allSubscribedOnce(), buildEngine(), "eliminazione")
            }
        }
    }

    /** 1.5.3: cosa ha cambiato una revisione, per il log e per «Applica ora». */
    data class EsitoRevisione(val rimessiInChiaro: Int, val stelleTolte: Int, val ripunteggiati: Int)

    /**
     * 1.5.3 — TOGLIE gli effetti che le regole attuali non giustificano piu':
     * rimette in chiaro, toglie la stella della regola, riporta il punteggio
     * verso zero. Non nasconde e non aggiunge niente (quello e' del sync e di
     * «Applica ora»). La decisione per messaggio e' in
     * `revisioneDopoCambioRegole` (FilterEngine.kt, Kotlin puro, provata con
     * kotlinc); qui c'e' solo il giro sul database.
     *
     * Due passate: prima si DECIDE su tutti i gruppi, poi si SCRIVE. Serve al
     * veto sulle copie (crosspost, e lo stesso gruppo su piu' server): una copia
     * il cui effetto e' ancora giustificato trattiene tutte le altre.
     *
     * Il corpo e le intestazioni si passano al motore (a differenza del giro
     * di «Applica ora»): una regola sul corpo che nasconde ancora deve vedersi.
     * Dove mancano, e una regola rimasta li guarda, il messaggio non si tocca.
     */
    private suspend fun rivediDopoCambioRegole(
        gruppi: List<Group>,
        engine: FilterEngine,
        perche: String,
    ): EsitoRevisione {
        val decisioni = ArrayList<DecisioneRiga>()
        val gruppoDi = HashMap<Long, Long>()
        for (g in gruppi) {
            val protetti = db.poisonedIdDao().idsByGroup(g.id).toHashSet()
            val regole = engine.rulesForGroup(g.name)
            val guardaCorpo = regole.any { it.field == FilterField.BODY }
            val guardaLettore = regole.any { it.field == FilterField.NEWSREADER }
            val guardaIntestazioni = regole.any { it.field == FilterField.HEADERS }
            for (a in db.articleDao().byGroupOnce(g.id)) {
                val hf = MimeUtil.intestazioniPerFiltri(a.headersRaw)
                val stato = StatoMessaggio(
                    nascosto = a.hidden,
                    seguito = a.watched,
                    punteggio = a.score,
                    protetto = a.messageId in protetti,
                    valutabile = !(guardaCorpo && a.body == null) &&
                        !(guardaLettore && hf.lettore == null) &&
                        !(guardaIntestazioni && hf.tutte == null),
                )
                val esito = engine.evaluate(
                    ScorableArticle(
                        a.fromField, a.subject, a.messageId,
                        newsgroups = g.name, body = a.body, references = a.references,
                        newsreader = hf.lettore, intestazioni = hf.tutte,
                    ),
                    g.name,
                )
                decisioni.add(
                    DecisioneRiga(
                        a.id, a.messageId, a.hidden, revisioneDopoCambioRegole(stato, esito),
                        seguito = a.watched, punteggio = a.score,
                    ),
                )
                gruppoDi[a.id] = g.id
            }
        }
        val dopoVeto = applicaVetoCrosspost(decisioni)
        // stesso ordine in entrata e in uscita: si confrontano riga per riga
        val trattenuti = decisioni.indices.count { decisioni[it].revisione != null && dopoVeto[it].revisione == null }
        val finali = dopoVeto.filter { it.revisione != null }

        val inChiaro = finali.filter { it.revisione!!.rimettiInChiaro }
        inChiaro.groupBy { it.revisione!!.punteggio ?: 0 }.forEach { (score, lotto) ->
            lotto.map { it.id }.chunked(500).forEach { db.articleDao().rimettiInChiaroByIds(it, score) }
        }
        val senzaStella = finali.filter { it.revisione!!.togliStella }
        senzaStella.map { it.id }.chunked(500).forEach { db.articleDao().setWatchedBulk(it, false) }
        val ripunteggio = finali.filter { !it.revisione!!.rimettiInChiaro && it.revisione!!.punteggio != null }
        ripunteggio.groupBy { it.revisione!!.punteggio!! }.forEach { (score, lotto) ->
            lotto.map { it.id }.chunked(500).forEach { db.articleDao().setScoreByIds(it, score) }
        }
        finali.mapNotNull { gruppoDi[it.id] }.distinct().forEach { db.groupDao().refreshUnreadCount(it) }

        val esito = EsitoRevisione(inChiaro.size, senzaStella.size, ripunteggio.size)
        NntpLog.diag(
            "Filtri: revisione dopo $perche su ${gruppi.size} gruppi, ${decisioni.size} messaggi - " +
                "tornati visibili ${esito.rimessiInChiaro}, stelle tolte ${esito.stelleTolte}, " +
                "punteggi riportati verso zero ${esito.ripunteggiati}" +
                (if (trattenuti > 0) ", lasciati com'erano perche' un'altra copia e' giustificata $trattenuti" else ""),
        )
        return esito
    }

    // ---------------------------------------------------------------- backup & housekeeping
    suspend fun allServersOnce(): List<Server> = db.serverDao().allOnce()
    suspend fun allFiltersOnce(): List<FilterRuleEntity> = db.filterDao().allOnce()
    suspend fun allSubscribedOnce(): List<Group> = db.groupDao().allSubscribedOnce()

    /** Server per l'export: password decifrata solo se [includePasswords], altrimenti null. */
    suspend fun exportServers(includePasswords: Boolean): List<Server> =
        db.serverDao().allOnce().map { s ->
            s.copy(
                password = if (includePasswords) cipher.decrypt(s.password.orEmpty()).ifEmpty { null } else null,
            )
        }

    /**
     * Ripristina un server dal backup SENZA creare duplicati: se esiste già lo
     * stesso host:porta lo aggiorna mantenendo l'id (e la password esistente,
     * se il backup non ne porta una). Ritorna l'id del server.
     */
    /**
     * 1.4 — Diagnostica database: le righe di ogni tabella, nell'ordine in cui
     * vanno lette (dalla configurazione ai dati, poi le tabelle di servizio).
     *
     * I nomi restano quelli VERI delle tabelle e non si traducono: questo
     * elenco esiste per finire dentro una segnalazione, e una segnalazione che
     * dice "gruppi disponibili" invece di `available_groups` costringe chi
     * legge a indovinare di quale tabella si stia parlando.
     *
     * A cosa serve, per esteso, in `DiagnosticaDao`.
     */
    suspend fun conteggiDatabase(): List<Pair<String, Int>> {
        val d = db.diagnosticaDao()
        return listOf(
            "servers" to d.servers(),
            "identities" to d.identities(),
            "groups" to d.groups(),
            "available_groups" to d.availableGroups(),
            "articles" to d.articles(),
            "articles (con corpo)" to d.articoliConCorpo(),
            "filter_rules" to d.filterRules(),
            "drafts" to d.drafts(),
            "poisoned_ids" to d.poisonedIds(),
            "tls_pins" to d.tlsPins(),
        )
    }

    /**
     * 1.4.2 (build 113) — righe rimaste senza il loro server o il loro gruppo.
     *
     * Le lascia indietro chi ha cancellato un server con una versione fino
     * alla 112 compresa: da quella dopo non se ne creano piu'. Restituisce solo
     * le voci DIVERSE DA ZERO, cosi' su un'installazione sana la diagnostica
     * non guadagna quattro righe di rumore, e il giorno in cui ne compare una
     * quella riga significa davvero qualcosa.
     *
     * Nella 113 qui si contava e basta: misurare prima e cancellare poi e' la
     * stessa doctrina con cui la 1.4 ha messo la diagnostica database davanti
     * al TrustManager. La misura e' arrivata (vedi [eliminaOrfani]), e dalla
     * 114 questi conteggi servono a due cose: mostrarli, e dire all'utente
     * quanto sta per buttare PRIMA che lo butti.
     */
    suspend fun conteggiOrfani(): List<Pair<String, Int>> {
        val d = db.diagnosticaDao()
        return listOf(
            "groups senza server" to d.gruppiOrfani(),
            "articles senza gruppo" to d.articoliOrfani(),
            "available_groups senza server" to d.catalogoOrfano(),
            "tls_pins senza server" to d.pinOrfani(),
        ).filter { it.second > 0 }
    }

    /**
     * 1.4.2 (build 114) — LA PULIZIA DEGLI ORFANI (voce A3).
     *
     * Il permesso di scriverla e' un numero, non un'idea: la diagnostica della
     * 113 sul telefono di BIG Umberto, 23 settembre 2026, ha riportato 3
     * `groups` e 99.906 righe di `available_groups` senza server — quasi meta'
     * delle 206.833 totali, su un file da 36 MB. Quei tre gruppi sono le
     * stesse righe `? — nomegruppo` che gli compaiono nel selettore della
     * diagnostica LISTGROUP.
     *
     * L'ORDINE E' OBBLIGATO e non e' quello di [eliminaServerEContenuti]: li'
     * si va dagli articoli ai gruppi perche' gli id dei gruppi servono ancora;
     * qui si va dai gruppi agli articoli, perche' finche' il gruppo orfano
     * esiste i suoi articoli non sono orfani e la seconda DELETE non li
     * vedrebbe. Invertire le due righe significherebbe lasciare indietro
     * proprio gli articoli che si vogliono togliere, e accorgersene solo alla
     * passata successiva.
     *
     * Nessuna transazione, ed e' una scelta: se la cancellazione si
     * interrompesse a meta', il risultato sarebbero *meno* orfani di prima e
     * una seconda passata finirebbe il lavoro. Una transazione su centomila
     * righe, su un telefono di fascia bassa, costerebbe piu' di cio' che
     * protegge.
     *
     * Restituisce quante righe sono state tolte davvero, voce per voce e solo
     * quelle diverse da zero: e' un consuntivo, non il preventivo che
     * l'utente ha gia' visto nel dialogo di conferma.
     */
    suspend fun eliminaOrfani(): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        val d = db.diagnosticaDao()
        val gruppi = d.eliminaGruppiOrfani()
        val articoli = d.eliminaArticoliOrfani()
        val catalogo = d.eliminaCatalogoOrfano()
        val pin = d.eliminaPinOrfani()
        listOf(
            "groups senza server" to gruppi,
            "articles senza gruppo" to articoli,
            "available_groups senza server" to catalogo,
            "tls_pins senza server" to pin,
        ).filter { it.second > 0 }
    }

    /**
     * 1.4 — la versione di schema scritta NEL FILE, non quella dichiarata nel
     * codice. Dopo un'apertura riuscita coincidono; il giorno in cui non
     * coincidessero, e' il primo numero da guardare.
     *
     * `withContext(IO)`: qui non c'e' un DAO generato da Room a spostare il
     * lavoro fuori dal thread principale, e `readableDatabase` puo' aprire il
     * file davvero.
     */
    suspend fun versioneSchemaSuDisco(): Int = withContext(Dispatchers.IO) {
        db.openHelper.readableDatabase.version
    }

    /**
     * 1.4 — ripara i nomi utente FINTI lasciati dai ripristini di backup fatti
     * con le versioni fino alla 1.3.7 comprese, e restituisce quante righe ha
     * corretto (0 in un'installazione sana).
     *
     * Il guasto: in esportazione lo username assente viaggiava come `null` JSON
     * e in lettura `optString` di Android ne restituiva la PAROLA "null" invece
     * del fallback (la spiegazione per esteso sta in `BackupManager`, sul punto
     * in cui la guardia `isNull` e' stata aggiunta). Un server anonimo si
     * ritrovava percio' un nome utente, e NeReO tentava l'autenticazione dove
     * non serviva.
     *
     * Due condizioni, entrambe necessarie: la stringa deve essere ESATTAMENTE
     * "null" e il server non deve avere una password. Con una password salvata
     * quello username, per quanto improbabile, e' stato scritto da qualcuno che
     * sapeva cosa stava facendo, e non tocca a noi cancellarlo.
     */
    suspend fun riparaUsernameFinti(): Int {
        var riparati = 0
        for (s in db.serverDao().allOnce()) {
            if (s.username == "null" && s.password.isNullOrEmpty()) {
                db.serverDao().upsert(s.copy(username = null))
                riparati++
            }
        }
        return riparati
    }

    suspend fun restoreServer(incoming: Server): Long {
        val existing = db.serverDao().byHostPort(incoming.host, incoming.port)
        val newPw = incoming.password?.takeIf { it.isNotEmpty() }?.let { cipher.encrypt(it) }
        return if (existing != null) {
            // 1.2.12 (audit di sicurezza): UN RIPRISTINO NON PEGGIORA MAI LA
            // SICUREZZA DI UN SERVER GIÀ CONFIGURATO CON UNA PASSWORD.
            //
            // Prima, un backup che dichiarasse lo stesso host:porta di un
            // server già presente ne riscriveva ssl/startTls/override
            // CONSERVANDONE la password: un file ostile ("configurazione già
            // pronta") poteva quindi spegnere la cifratura E disattivare la
            // guardia anti-credenziali-in-chiaro, e alla prima
            // sincronizzazione la password dell'utente partiva in chiaro,
            // senza che lui vedesse nulla.
            //
            // Ora, su un server che HA già una password salvata: la cifratura
            // si può solo ACCENDERE, mai spegnere, e l'override
            // dell'autenticazione non cifrata si può solo TOGLIERE, mai
            // mettere. Su un server senza password (o nuovo) i campi arrivano
            // integri: lì non c'è nulla da proteggere e va rispettata la
            // configurazione scelta da chi ha fatto il backup.
            val haPassword = !existing.password.isNullOrEmpty() || newPw != null
            db.serverDao().upsert(
                existing.copy(
                    name = incoming.name,
                    ssl = if (haPassword) (existing.ssl || incoming.ssl) else incoming.ssl,
                    username = incoming.username,
                    password = newPw ?: existing.password,
                    // 1.2.9: STARTTLS e override auth viaggiano col backup come gli
                    // altri campi del server. L'assegnazione dell'identità NO — la
                    // gestisce BackupManager a parte, per nickname (gli id di riga
                    // non hanno senso fra installazioni diverse).
                    startTls = if (haPassword) (existing.startTls || incoming.startTls) else incoming.startTls,
                    authOverrideNonCifrata = if (haPassword) {
                        // true = "consenti credenziali in chiaro": il valore più
                        // permissivo non può arrivare da un file. null/false sì.
                        if (incoming.authOverrideNonCifrata == true) existing.authOverrideNonCifrata
                        else incoming.authOverrideNonCifrata
                    } else incoming.authOverrideNonCifrata,
                )
            )
            existing.id
        } else {
            db.serverDao().upsert(incoming.copy(id = 0, password = newPw))
        }
    }

    /** Elimina i messaggi LETTI più vecchi di [days] giorni (tiene non letti e watched). */
    suspend fun purgeOld(days: Int): Int {
        if (days <= 0) return 0
        val cutoff = System.currentTimeMillis() / 1000 - days.toLong() * 86_400
        // M1: il registro degli avvelenati segue la stessa finestra degli
        // articoli. Se restasse indietro crescerebbe all'infinito; se corresse
        // avanti perderebbe il contesto di thread ancora vivi.
        db.poisonedIdDao().deleteOlderThan(cutoff)
        return db.articleDao().deleteReadOlderThan(cutoff)
    }

    /** Come purgeOld ma su un solo gruppo. */
    suspend fun purgeGroup(groupId: Long, days: Int): Int {
        if (days <= 0) return 0
        val cutoff = System.currentTimeMillis() / 1000 - days.toLong() * 86_400
        db.poisonedIdDao().deleteOlderThan(cutoff)
        return db.articleDao().deleteReadOlderThanInGroup(groupId, cutoff)
    }

    /** Imposta l'override di retention del gruppo (null = usa il globale). */
    suspend fun setGroupPurgeDays(groupId: Long, days: Int?) = db.groupDao().setPurgeDays(groupId, days)

    /**
     * 1.4 (Henry The Mole) — introduzione alla citazione per un gruppo.
     * null = torna a quella dell'identita'.
     */
    suspend fun setGroupReplyIntro(groupId: Long, intro: String?) =
        db.groupDao().setReplyIntro(groupId, intro)

    /**
     * Esito di [applicaFiltriRetroattivi]: quanti articoli già scaricati sono
     * stati eliminati (regole KILL, es. "Elimina del tutto"), quanti sono
     * stati invece nascosti (regole HIDE, es. "Ignora (nascondi)") e quanti
     * hanno cambiato punteggio senza sparire (1.4.8, §A12-retro: le regole che
     * AGGIUNGONO — "Evidenzia" e i punteggi della modalità avanzata).
     *
     * Tre cifre perché le azioni sono quattro e due di esse non rimuovono
     * niente: un esito a due cifre era, da solo, la dichiarazione del difetto.
     */
    data class EsitoFiltriRetroattivi(
        val eliminati: Int,
        val nascosti: Int,
        val ripunteggiati: Int = 0,
        // 1.5.3: «Applica ora» va anche nell'altra direzione (vedi
        // rivediDopoCambioRegole). Due cifre in piu', per la stessa ragione
        // della terza: un esito che non le conta dichiara il difetto da solo.
        val rimessiInChiaro: Int = 0,
        val stelleTolte: Int = 0,
    )

    /**
     * beta20 (Roxy): applica le regole di filtro agli articoli GIA' SCARICATI.
     * Il KILL agiva solo durante il sync (gli articoli che matchavano non
     * entravano nel DB), quindi creando un filtro "Elimina del tutto" i messaggi
     * gia' scaricati restavano visibili. Qui rivaluto quelli presenti e cancello
     * chi finirebbe in KILL. NON tocca i messaggi seguiti (watched).
     *
     * Correzione 1.2.9 (segnalazione di Marco, "mostra filtrati" mostrava solo
     * i messaggi precedenti alla creazione del filtro — un audit incompleto per
     * costruzione): questa funzione, nonostante il nome e lo scopo dichiarato
     * ("applica le regole ai messaggi gia' scaricati"), rivalutava SOLO l'esito
     * KILL. Un messaggio gia' scaricato che ricadeva in una regola "Ignora
     * (nascondi)" (HIDE, non KILL) non veniva MAI marcato `hidden`, quindi
     * restava visibile per sempre, invisibile all'audit di "mostra filtrati" —
     * che invece funzionava correttamente per i messaggi arrivati DOPO la
     * creazione del filtro (valutati dal motore al momento del sync, vedi
     * `syncGroup`). Da qui l'asimmetria osservata: vecchi
     * invisibili all'audit, nuovi visibili. Ora la funzione gestisce ENTRAMBI
     * gli esiti nello stesso giro: KILL -> eliminazione (comportamento
     * storico, invariato), HIDE -> marcatura `hidden = true` (nuovo), cosi'
     * "mostra filtrati" e' un audit completo per qualunque regola, vecchia o
     * nuova che sia.
     *
     * Correzione 1.4.8, §A12-retro (segnalazione di Cercaparole su Usenet): la
     * stessa malattia della 1.2.9, dall'altro capo. Rivalutati tutti e quattro
     * gli esiti, la funzione scriveva il punteggio SOLO nel ramo HIDE, quindi
     * le due azioni che AGGIUNGONO — "Evidenzia" e i punteggi della modalita'
     * avanzata — restavano senza effetto sul pregresso. Ora il punteggio si
     * scrive anche quando non nasconde; il dettaglio e le due esclusioni volute
     * stanno nel commento al blocco, piu' sotto.
     *
     * [groupId] null = tutti i gruppi sottoscritti.
     */
    suspend fun applicaFiltriRetroattivi(groupId: Long? = null): EsitoFiltriRetroattivi =
        withContext(Dispatchers.IO) { filtriMutex.withLock { applicaFiltriRetroattiviBloccato(groupId) } }

    private suspend fun applicaFiltriRetroattiviBloccato(groupId: Long?): EsitoFiltriRetroattivi {
        val engine = buildEngine()
        val gruppi = if (groupId != null) listOfNotNull(db.groupDao().byId(groupId))
                     else db.groupDao().allSubscribedOnce()
        var eliminati = 0
        var nascosti = 0
        var ripunteggiati = 0
        for (g in gruppi) {
            val tutti = db.articleDao().byGroupOnce(g.id)
            val regolePropaganti = engine.poisoningRulesForGroup(g.name)

            // M1: registra i semi PRIMA di cancellarli, come al sync. Qui e'
            // ancora piu' importante: dopo la cancellazione questi message-id
            // non esisterebbero da nessuna parte, e le risposte che arriveranno
            // domani non avrebbero modo di sapere da chi discendono.
            if (regolePropaganti.isNotEmpty()) {
                val semi = ArrayList<PoisonedId>()
                for (a in tutti) {
                    val hf = MimeUtil.intestazioniPerFiltri(a.headersRaw)
                    val scorable = ScorableArticle(
                        a.fromField, a.subject, a.messageId,
                        newsgroups = g.name, body = a.body, references = a.references,
                        // 1.4.4: null per la stragrande maggioranza degli articoli
                        // (intestazioni complete solo per quelli scaricati), e
                        // allora le regole NEWSREADER e HEADERS si saltano da sole.
                        newsreader = hf.lettore, intestazioni = hf.tutte,
                    )
                    for (regola in regolePropaganti) {
                        if (regola.matchesArticle(scorable)) {
                            semi.add(PoisonedId(a.messageId, g.id, a.dateEpoch, regola.id, seed = true))
                        }
                    }
                }
                if (semi.isNotEmpty()) db.poisonedIdDao().insertAll(semi)
            }

            // Un solo giro di valutazione per articolo, riusato sia per il
            // KILL (elimina) sia per l'HIDE (nascondi) — prima erano due
            // valutazioni separate solo per il KILL; l'HIDE non era valutato
            // affatto qui.
            val valutati = tutti.filter { !it.watched }.map { a ->
                val hf = MimeUtil.intestazioniPerFiltri(a.headersRaw)
                a to engine.evaluate(
                    ScorableArticle(
                        a.fromField, a.subject, a.messageId,
                        newsgroups = g.name, references = a.references,
                        newsreader = hf.lettore, intestazioni = hf.tutte,
                    ),
                    g.name,
                )
            }

            val daEliminare = valutati.filter { it.second.action == ScoreResult.Action.KILL }.map { it.first.id }
            daEliminare.chunked(500).forEach { db.articleDao().deleteByIds(it) }
            eliminati += daEliminare.size

            // HIDE: solo chi non e' gia' marcato hidden (non ha senso
            // ri-scrivere quello che gia' c'e', e conta solo i cambiamenti
            // reali nel totale mostrato a Marco).
            val daNascondere = valutati.filter { (a, res) -> res.action == ScoreResult.Action.HIDE && !a.hidden }
            if (daNascondere.isNotEmpty()) {
                daNascondere.groupBy { it.second.score }.forEach { (score, gruppo) ->
                    gruppo.map { it.first.messageId }.chunked(500).forEach { ids ->
                        db.articleDao().setHiddenAndScoreByMessageIds(ids, hidden = true, score = score)
                    }
                }
                nascosti += daNascondere.size
            }

            // --------------------------------------------- 1.4.8 §A12-retro
            // IL PUNTEGGIO SI SCRIVE ANCHE QUANDO NON NASCONDE.
            //
            // Segnalazione di Cercaparole su Usenet (5 ottobre): una regola
            // «Evidenzia» o «Segui» non si applicava al pregresso. La diagnosi
            // ha mostrato che il motore la valutava correttamente e che era
            // QUESTA funzione a buttare via l'esito: il punteggio veniva
            // scritto nel solo ramo HIDE, qui sopra. Per gli altri tre esiti
            // la riga conservava il punteggio di prima, cioe' zero nel caso
            // tipico — e in NeReO «evidenziato» non e' un flag, e' la lettura
            // di `score >= 100` fatta dall'elenco discussioni. Nessun
            // punteggio scritto, nessuna evidenziazione: non una funzione
            // mancante, una UPDATE mancante.
            //
            // Copre di conseguenza anche la MODALITA' AVANZATA, che era muta
            // allo stesso modo e che nessuno aveva segnalato: una regola a +50
            // o a -30 ora vale sul pregresso come vale sul nuovo.
            //
            // DUE ESCLUSIONI VOLUTE, ed e' qui che sta tutta la prudenza di
            // questa modifica:
            //
            //  - gli articoli GIA' NASCOSTI si saltano. Non si vedono, quindi
            //    un punteggio aggiornato non cambierebbe nulla a schermo; e
            //    riscriverlo vorrebbe dire sovrascrivere il punteggio di
            //    propagazione (`followupScoreEffettivo`), che e' l'unica
            //    traccia del perche' quell'articolo e' nascosto. Si perderebbe
            //    un dato per non guadagnare niente.
            //  - i SEGUITI non arrivano nemmeno qui: `valutati` li ha gia'
            //    esclusi (`tutti.filter { !it.watched }`), come da sempre.
            //
            // 1.5.1: il «LIMITE DICHIARATO» che stava qui — WATCH senza stella,
            // propagazione che nasconde qualunque sia l'azione — e' la seconda
            // meta' di §A12-retro, ed e' chiuso. La stella si mette PIU' SOTTO,
            // dopo la propagazione; la propagazione segue l'azione in
            // propagaAlleRisposte.
            val daRipunteggiare = valutati.filter { (a, res) ->
                !a.hidden &&
                    res.action != ScoreResult.Action.KILL &&
                    res.action != ScoreResult.Action.HIDE &&
                    res.score != a.score
            }
            if (daRipunteggiare.isNotEmpty()) {
                daRipunteggiare.groupBy { it.second.score }.forEach { (score, lotto) ->
                    lotto.map { it.first.id }.chunked(500).forEach { ids ->
                        db.articleDao().setScoreByIds(ids, score)
                    }
                }
                ripunteggiati += daRipunteggiare.size
            }

            // M1: propagazione sui superstiti. Gli eliminati non servono piu' —
            // il loro contributo all'albero e' gia' nel registro dei semi.
            if (regolePropaganti.isNotEmpty()) {
                val idEliminati = daEliminare.toSet()
                val superstiti = tutti.filter { it.id !in idEliminati }
                propagaAlleRisposte(g, engine, superstiti)
            }

            // 1.5.1 (§A12-retro meta' B): la STELLA sui match diretti di una
            // regola «Segui», come fa il sync sui messaggi nuovi
            // (`watched = result.action == WATCH`). PER ULTIMA: `watched` e'
            // protettivo, e marcarla prima della propagazione avrebbe tolto
            // questi messaggi dai suoi candidati. I nascosti si saltano, come
            // per il punteggio qui sopra; i gia' seguiti non sono in `valutati`.
            //
            // ⚠️ 1.5.3/167 (foto di Marco, 14/10): la stella si scrive per
            // message-id, cioe' su TUTTE le copie (lo stesso gruppo su piu'
            // server); il punteggio invece era scritto per riga, solo qui. Le
            // copie degli altri server, quando arrivava il loro turno, erano gia'
            // seguite, uscivano da `valutati` e restavano con stella e
            // punteggio 0 — cioe' con l'aspetto di una stella MESSA A MANO, che
            // la revisione non toglie. Ora punteggio e stella vanno insieme, per
            // message-id, come fa gia' la propagazione.
            val daSeguire = valutati
                .filter { (a, res) -> !a.hidden && res.action == ScoreResult.Action.WATCH }
            daSeguire.groupBy { it.second.score }.forEach { (score, lotto) ->
                lotto.map { it.first.messageId }.chunked(500)
                    .forEach { db.articleDao().setScoreVisibiliByMessageIds(it, score) }
            }
            daSeguire.map { it.first.messageId }.chunked(500)
                .forEach { db.articleDao().setWatchedByMessageIds(it) }

            db.groupDao().refreshUnreadCount(g.id)
        }
        // 1.5.3: e poi l'altra direzione, sugli stessi gruppi. DOPO tutto il
        // resto: i messaggi appena nascosti, seguiti o propagati qui sopra
        // risultano gia' giustificati dalle regole, e la revisione li lascia.
        val rev = rivediDopoCambioRegole(gruppi, engine, "Applica ora")
        NntpLog.diag("Filtri: Applica ora - eliminati $eliminati, nascosti $nascosti, ripunteggiati $ripunteggiati")
        return EsitoFiltriRetroattivi(
            eliminati, nascosti, ripunteggiati + rev.ripunteggiati, rev.rimessiInChiaro, rev.stelleTolte,
        )
    }

    /**
     * beta20 (Roxy): elimina TUTTI i messaggi di una discussione dal database
     * locale. Prima esisteva solo "elimina copia locale", che svuotava il corpo
     * lasciando la riga barrata nell'elenco.
     *
     * 1.3.5 — prende ora gli ID GIA' DECISI dal chiamante invece di un rootKey
     * da cui ricalcolarsi i membri.
     *
     * Era la quinta copia della regola a stringhe
     * (`a.messageId == rootKey || a.references.contains(rootKey)`), trovata
     * dalla verifica incrociata mentre si correggevano le altre. Qui la
     * divergenza era la piu' sgradevole di tutte, perche' questa funzione
     * CANCELLA: su una discussione il cui post iniziale non e' mai stato
     * scaricato ne toglieva una parte e lasciava i frammenti a video.
     *
     * L'appartenenza la decide ora chi ha davanti la discussione disegnata —
     * `threadIdsOf`, che passa dal threader. Repository non ha (e non deve
     * avere) una sua idea di che cosa sia un thread.
     */
    suspend fun eliminaDiscussione(ids: List<Long>): Int = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext 0
        // 1.4.2 (build 113): i gruppi si leggono PRIMA di cancellare, perche'
        // dopo le righe non ci sono piu' e non c'e' da dove ricavarli.
        val gruppi = ids.chunked(500)
            .flatMap { db.articleDao().byIds(it) }
            .map { it.groupId }
            .distinct()
        ids.chunked(500).forEach { db.articleDao().deleteByIds(it) }
        // 1.4.2 (build 113) — QUI MANCAVA, ed era l'unica cancellazione a non
        // farlo: deleteLocal e eliminaArticoloLocale aggiornano entrambe il
        // contatore. Eliminando una discussione con messaggi non letti il
        // pallino del gruppo restava piu' alto del vero fino al sync
        // successivo — lo stesso sintomo che stiamo inseguendo con la "lista
        // sorda", e quindi il modo migliore per confondere le acque.
        gruppi.forEach { db.groupDao().refreshUnreadCount(it) }
        ids.size
    }

    /**
     * beta19 (Roxy): imposta la conservazione a NUMERO DI DISCUSSIONI per il
     * gruppo (null/0 = usa la conservazione a giorni). Quando è valorizzata ha
     * la precedenza sui giorni: si tengono le N discussioni più recenti.
     */
    suspend fun setGroupPurgeKeepThreads(groupId: Long, keep: Int?) =
        db.groupDao().setPurgeKeepThreads(groupId, keep)

    /** Applica subito la conservazione a numero di discussioni su un gruppo. */
    suspend fun purgeGroupKeepThreads(groupId: Long, keep: Int): Int =
        db.articleDao().keepOnlyLastThreadsInGroup(groupId, keep)

    suspend fun importKillFile(rules: List<FilterRule>) {
        db.filterDao().upsertAll(rules.map { it.toEntity() })
    }

    suspend fun buildEngine(): FilterEngine =
        FilterEngine(db.filterDao().allOnce().map { it.toRule() })

    companion object {
        /**
         * Quanti NUMERI d'articolo per ogni richiesta di overview (1.4,
         * segnalazione Bingo3331). Non sono articoli: quanti ne tornino
         * dipende dai buchi del gruppo. Vedi la nota in syncGruppoConClient
         * per il perche' di questo valore.
         */
        private const val PASSO_OVERVIEW = 10_000L

        private val RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME
        // Alcuni server omettono il giorno della settimana o usano 2 cifre d'anno
        private val NO_DOW = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss Z", java.util.Locale.US)

        /** Parse tollerante delle date Usenet (RFC 5322 è un far west). */
        fun parseDate(raw: String): Long {
            val cleaned = raw.trim().substringBefore(" (").trim()
            runCatching { return ZonedDateTime.parse(cleaned, RFC1123).toEpochSecond() }
            runCatching { return ZonedDateTime.parse(cleaned, NO_DOW).toEpochSecond() }
            // ultima spiaggia: butto via il giorno della settimana e riprovo
            val noDow = cleaned.substringAfter(", ")
            runCatching { return ZonedDateTime.parse(noDow, NO_DOW).toEpochSecond() }
            return 0L
        }
    }
}

fun FilterRuleEntity.toRule() = FilterRule(
    id = id, name = name,
    field = runCatching { FilterField.valueOf(field) }.getOrDefault(FilterField.SUBJECT),
    matchType = runCatching { MatchType.valueOf(matchType) }.getOrDefault(MatchType.CONTAINS),
    pattern = pattern, score = score, caseSensitive = caseSensitive,
    groupWildmat = groupWildmat, expiresEpochDay = expiresEpochDay, enabled = enabled,
    // M1: valore ignoto (kill-file scritto a mano, o database piu' recente del
    // codice) ⇒ regola classica. Mai un crash per un enum sconosciuto.
    followup = runCatching { FollowupMode.valueOf(followup) }.getOrDefault(FollowupMode.NONE),
    followupScore = followupScore,
)

fun FilterRule.toEntity() = FilterRuleEntity(
    id = id, name = name, field = field.name, matchType = matchType.name,
    pattern = pattern, score = score, caseSensitive = caseSensitive,
    groupWildmat = groupWildmat, expiresEpochDay = expiresEpochDay, enabled = enabled,
    followup = followup.name, followupScore = followupScore,
)
