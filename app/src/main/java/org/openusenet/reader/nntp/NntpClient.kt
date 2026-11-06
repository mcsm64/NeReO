/*
 * OpenUsenet — text-only Usenet newsreader for Android
 * Copyright (C) 2026  OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Client NNTP minimale conforme a RFC 3977 (+ AUTHINFO RFC 4643, OVER/XOVER).
 * Puro Kotlin/JVM: nessuna dipendenza Android, testabile da riga di comando.
 */
package org.openusenet.reader.nntp

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager

class NntpException(val code: Int, message: String) : IOException("$code $message")

/** 1.6.1: quanto si aspetta la risposta al QUIT, chiudendo. Vedi [NntpClient.close]. */
private const val ATTESA_QUIT_MS = 1_000

/**
 * 1.3.3 (segnalazione di Bingo3331) — che COSA è andato storto nell'handshake.
 *
 * Serve perché i due casi hanno rimedi DIVERSI e fino alla 1.3.2 li trattavamo
 * come uno solo, suggerendo sempre lo stesso interruttore:
 *
 *  - CATENA: il certificato non è firmato da una CA che Android riconosce
 *    (autofirmato, CA privata), oppure è scaduto o non ancora valido. Lo
 *    rifiuta il TrustManager dentro SSLSocketFactory.getDefault(), e
 *    `consentiCertificatoNonVerificabile` NON LO TOCCA: quell'opzione agisce
 *    solo su endpointIdentificationAlgorithm, cioè sul nome host. Suggerirla
 *    qui manda l'utente in un vicolo cieco — è esattamente quello che è
 *    successo a Bingo3331 su Paganini: ha fatto quello che il messaggio gli
 *    diceva, non ha funzionato, e ne ha dedotto (ragionevolmente) che
 *    l'opzione fosse rotta. Non è rotta: fa un'altra cosa.
 *
 *  - NOME HOST: il certificato è valido ma non è emesso per QUESTO host.
 *    Qui l'opzione funziona davvero, e va suggerita.
 *
 *  - INDETERMINATO: non riconosciamo la firma dell'errore. Non si inventa una
 *    diagnosi: si dice cosa non torna e si mostra il messaggio grezzo, che è
 *    il dato che serve a chi segnala.
 */
internal enum class EsitoTls { CATENA, NOME_HOST, INDETERMINATO }

/**
 * Classifica una [javax.net.ssl.SSLException] percorrendo TUTTA la catena delle
 * cause: su Android l'eccezione utile (CertificateException →
 * CertPathValidatorException) sta quasi sempre annidata sotto una
 * SSLHandshakeException generica, quindi guardare solo il livello esterno non
 * basta.
 *
 * L'ordine dei test non è casuale: la CATENA si riconosce per TIPO, che è un
 * segnale forte; il NOME HOST anche per testo del messaggio, che è più debole
 * e quindi viene dopo.
 */
internal fun classificaErroreTls(e: Throwable): EsitoTls {
    var t: Throwable? = e
    val visti = HashSet<Throwable>()
    val testo = StringBuilder()
    while (t != null && visti.add(t)) {
        if (t is java.security.cert.CertificateException ||
            t is java.security.cert.CertPathValidatorException ||
            t is java.security.cert.CertificateExpiredException ||
            t is java.security.cert.CertificateNotYetValidException
        ) {
            return EsitoTls.CATENA
        }
        t.message?.let { testo.append(it).append(' ') }
        t = t.cause
    }
    val msg = testo.toString().lowercase()
    // firme testuali della catena (alcune ROM riportano solo il messaggio)
    if (msg.contains("trust anchor") || msg.contains("certpath") ||
        msg.contains("unable to find valid certification path") ||
        msg.contains("certificate expired") || msg.contains("certificate has expired")
    ) {
        return EsitoTls.CATENA
    }
    if (e is javax.net.ssl.SSLPeerUnverifiedException ||
        msg.contains("hostname") || msg.contains("not verified") ||
        msg.contains("subject alternative")
    ) {
        return EsitoTls.NOME_HOST
    }
    return EsitoTls.INDETERMINATO
}

/**
 * 1.3.3: spiegazione in chiaro dell'errore TLS, UNA sola volta per tutti e due
 * i percorsi cifrati (TLS implicito in connect(), STARTTLS in tentaStartTls()).
 *
 * Perché condivisa e non scritta due volte: fino alla 1.3.2 i due punti
 * avevano due testi diversi che dicevano la stessa cosa sbagliata, e correggerne
 * uno solo sarebbe stato il difetto della 1.3 daccapo.
 *
 * Il messaggio grezzo del server ([e]) c'è SEMPRE, in fondo: è il dato che
 * serve a chi segnala un caso che non abbiamo previsto, e senza il quale una
 * diagnosi a distanza diventa un indovinello.
 *
 * NOTA (1.3.5): testo in INGLESE cablato, come tutti i messaggi di questa
 * classe. NntpClient è volutamente privo di dipendenze Android (niente
 * stringResource) per restare provabile da riga di comando: non può leggere le
 * risorse, quindi i suoi messaggi non sono traducibili.
 *
 * Fino alla 1.3.4 erano in italiano, e un utente inglese (Xiaomi M2103K19K,
 * 1.3.2) si è visto rispondere in italiano da un'app con l'interfaccia in
 * inglese. Da qui la scelta: se una lingua sola deve valere per tutti, che sia
 * quella di `values/` (il default dell'app) e la lingua franca di Usenet.
 *
 * NON è la localizzazione: è il ripiego dichiarato. Il prezzo lo paga ora
 * l'utente italiano, che legge queste frasi in inglese pur avendo l'interfaccia
 * in italiano — e che sono meno di dieci. La soluzione vera resta passare i
 * testi dall'esterno (piano A in wishlist), da fare quando questa classe si
 * toccherà per altri motivi.
 *
 * Le etichette citate nel testo sono quelle di values/ (inglese), non le
 * italiane: devono corrispondere a ciò che l'utente vede sui pulsanti.
 */
internal fun spiegazioneErroreTls(host: String, e: Throwable): String =
    when (classificaErroreTls(e)) {
        EsitoTls.CATENA ->
            "The certificate of $host is not signed by an authority Android trusts, " +
                "or it has expired. NeReO does not accept certificates like this, and " +
                "the option \"Do not check the host name on the certificate\" does NOT " +
                "help here: it acts on the name only, not on who signed it. " +
                "If your provider also offers an unencrypted port you can use that, " +
                "knowing the traffic travels in the clear. (${e.message})"
        EsitoTls.NOME_HOST ->
            "The certificate of $host is valid, but it was not issued for this address. " +
                "This happens with providers whose certificate is misconfigured. " +
                "If you know this is your case and accept the risk, turn on \"Do not " +
                "check the host name on the certificate\" in Maintenance → Security. " +
                "(${e.message})"
        EsitoTls.INDETERMINATO ->
            "Encrypted connection to $host failed: the TLS handshake did not succeed, " +
                "for a reason NeReO cannot classify. If you can, report the text in " +
                "brackets here to the developers: that is the part that matters. " +
                "(${e.message})"
    }

data class NntpResponse(val code: Int, val text: String) {
    val isPositive get() = code in 200..299
}

/** 1.6: le intestazioni di un articolo e il numero dichiarato dal server (0 = nessuno). */
class TestaArticolo(val numero: Long, val righe: List<ByteArray>)

data class GroupEntry(val name: String, val high: Long, val low: Long, val status: String)

data class GroupStats(val estimatedCount: Long, val low: Long, val high: Long, val name: String)

/** Una riga di overview (OVER/XOVER): i campi standard RFC 2980/3977. */
data class OverviewLine(
    val number: Long,
    val subject: String,
    val from: String,
    val date: String,
    val messageId: String,
    val references: String,
    val bytes: Long,
    val lines: Int,
    val extra: List<String> = emptyList(),
)

/**
 * Connessione NNTP sincrona. Usarla da un dispatcher IO (mai dal main thread).
 * Uso tipico:
 *   NntpClient("news.example.org", 563, ssl = true).use { c ->
 *       c.connect(); c.authenticate(user, pass)
 *       val g = c.group("it.hobby.modellismo")
 *       val over = c.over(g.high - 200, g.high)
 *   }
 */
class NntpClient(
    private val host: String,
    private val port: Int = 119,
    private val ssl: Boolean = false,
    /**
     * 1.2.9 (voce E, roadmap NNTP — RFC 4642): STARTTLS, l'upgrade a cifrato
     * DOPO la connessione, alternativo al TLS implicito ([ssl]). Ignorato se
     * [ssl] è già true. Se il server non lo supporta o lo rifiuta, la
     * connessione prosegue in chiaro — vedi [effectivelySsl] per come questo
     * si ripercuote su authenticate().
     */
    private val startTls: Boolean = false,
    /**
     * 1.2.14 (audit di sicurezza, voce §1.1) — se true SALTA la verifica che il
     * certificato appartenga davvero all'host a cui ci stiamo collegando.
     *
     * Di norma è false, e la verifica è attiva. Va acceso solo consapevolmente
     * (Manutenzione → Sicurezza), e solo per un provider che presenta un
     * certificato con nome non combaciante: capita, specie sui piani economici
     * e sugli host raggiunti tramite un alias. Con la verifica disattivata la
     * connessione resta CIFRATA ma non più AUTENTICATA: chiunque si trovi in
     * mezzo può presentare un certificato valido per un dominio suo qualunque e
     * leggere tutto, credenziali comprese.
     */
    private val consentiCertificatoNonVerificabile: Boolean = false,
    /**
     * 1.4 (TOFU) — lo SPKI che l'utente ha APPROVATO per questo endpoint, o
     * null se non ce n'e' uno.
     *
     * Quando c'e', quella chiave **e'** l'identita' di riferimento per questo
     * percorso (identity binding): la si confronta a ogni connessione, e se non
     * coincide la connessione fallisce — senza nessun pulsante "accetta il
     * nuovo" nell'avviso. L'attrito e' la funzione, non un difetto.
     */
    private val pinApprovato: String? = null,
    /**
     * 1.4 — l'utente ha acceso l'eccezione "certificato scaduto" per QUESTA
     * riga server. Vale solo per le date: catena PKIX e nome host restano
     * pretesi. Vedi [FiduciaTls].
     */
    private val eccezioneScadenza: Boolean = false,
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
    private val charset: Charset = Charsets.UTF_8,
) : Closeable {

    /**
     * 1.4 — il nostro TrustManager. UNO per client, costruito qui: osserva la
     * catena a ogni handshake (anche fallito) e delega la decisione a quello di
     * sistema, ribaltandola solo nei due casi deliberati.
     */
    private val fiducia = TrustManagerNeReO(pinApprovato, eccezioneScadenza)

    /**
     * L'identita' TLS osservata sull'ultimo handshake, riuscito o fallito.
     * Il Repository la legge DOPO connect() e la scrive nel database: qui
     * dentro non si potrebbe, l'osservazione avviene nel mezzo dell'handshake.
     */
    val identitaTls: IdentitaTls? get() = fiducia.ultimaIdentita

    /**
     * La fabbrica di socket TLS di NeReO, al posto di `SSLSocketFactory
     * .getDefault()`.
     *
     * UN SOLO punto: fino alla 1.3.7 `getDefault()` compariva in DUE posti
     * (connect e tentaStartTls) e sarebbero diventati due posti da cui
     * dimenticarsi la fiducia nuova. E' la regola che questo progetto ha gia'
     * pagato cinque volte.
     *
     * `init(null, ..., null)`: nessuna identita' cliente, nessun generatore
     * casuale nostro — solo il TrustManager cambia rispetto al comportamento
     * predefinito.
     */
    private fun fabbricaTls(): SSLSocketFactory {
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, arrayOf<TrustManager>(fiducia), null)
        return ctx.socketFactory
    }

    /**
     * 1.4 — se va verificato che il certificato appartenga all'host.
     *
     * Due modi di dire "no", e sono diversi:
     *  - [consentiCertificatoNonVerificabile]: l'utente ha spento la verifica
     *    (Manutenzione -> Sicurezza). La connessione resta cifrata ma NON
     *    autenticata. E' la scelta della 1.2.14, invariata.
     *  - [pinApprovato] presente: la verifica del nome host non serve perche'
     *    c'e' qualcosa di PIU' forte — l'uguaglianza esatta con una chiave
     *    pubblica che l'utente ha approvato per questo endpoint. Non e' un
     *    allentamento: e' la prova di identita' SOSTITUITA, non tolta. Il pin
     *    e' legato a host e porta osservati all'approvazione, quindi non puo'
     *    essere riusato su un endpoint diverso.
     */
    private fun verificaNomeHost(): Boolean =
        !consentiCertificatoNonVerificabile && pinApprovato == null

    private var socket: Socket? = null
    private var input: BufferedInputStream? = null
    private var output: BufferedOutputStream? = null

    /**
     * true se la connessione è EFFETTIVAMENTE cifrata in questo momento: [ssl]
     * (TLS implicito, vero da subito) OPPURE un upgrade STARTTLS riuscito.
     * authenticate() guarda QUESTO flag, non il parametro [ssl] del
     * costruttore: così la guardia anti-chiaro (1.2.6, RFC 4643) protegge
     * anche chi si connette in chiaro e ottiene la cifratura via STARTTLS,
     * e — simmetricamente — resta attiva se lo STARTTLS richiesto fallisce
     * (nessun downgrade silenzioso della protezione sulle credenziali).
     */
    private var effectivelySsl: Boolean = ssl

    /**
     * 1.2.17 (voce C): true se questa connessione è EFFETTIVAMENTE cifrata in
     * questo momento. Serve al Repository per annotare, dopo il collegamento,
     * se un server sta parlando in chiaro — informazione che finisce accanto al
     * suo nome nell'elenco dei server. È l'unico modo onesto di saperlo: la
     * configurazione dice solo cosa si è CHIESTO, non cosa si è ottenuto (OVH
     * annuncia STARTTLS e poi fallisce).
     */
    val connessioneCifrata: Boolean get() = effectivelySsl

    /**
     * CAPABILITIES **COME IL SERVER LA DICHIARA ALL'ARRIVO**, letta una volta
     * dopo il saluto e **mai piu' riscritta per il resto della sessione**
     * (roadmap NNTP, Tier 1 punto 2): prima `capabilities()` esisteva ma non
     * veniva mai chiamata — codice morto. Serve alla diagnosi (log) e a
     * decidere, in modo "morbido" (mai bloccante: resta il fallback reattivo
     * di sempre), se tentare STARTTLS ha senso. Vuota se il server non
     * risponde 101 o se l'interrogazione fallisce — mai un errore fatale.
     *
     * ### ⚠️ 1.4.11 / 155 — PERCHE' «ALL'ARRIVO» E' IN GRASSETTO
     *
     * Fino alla build 154 questo campo veniva **sovrascritto** dalla rilettura
     * che [tentaStartTls] fa dopo un upgrade riuscito. Nessuno leggeva quel
     * secondo valore, quindi per anni non e' costato niente — finche' il
     * par. B3 non ha chiesto a questo campo una domanda diversa: *che cosa
     * annunciava il server quando siamo arrivati?* Dopo uno STARTTLS riuscito
     * il server **non deve piu'** annunciare STARTTLS (RFC 4642 par. 2.1),
     * quindi il par. B3 leggeva una lista senza STARTTLS e gridava al
     * declassamento **proprio sulle connessioni cifrate con successo**.
     * Misurato sul dispositivo di Marco, OVH, sessione `4gz7i` del 12 ottobre
     * 2026: allarme falso su una connessione che si era appena cifrata.
     *
     * > Un campo che risponde a due domande diverse risponde male alla
     * > seconda il giorno in cui qualcuno gliela fa.
     *
     * La rilettura dopo la cifratura **non e' stata tolta** — e' quello che la
     * RFC raccomanda — ma ha un campo suo: [capabilitiesDopoTls].
     */
    var advertisedCapabilities: List<String> = emptyList()
        private set

    /**
     * CAPABILITIES **riletta dopo un upgrade STARTTLS riuscito**, perche' il
     * server puo' dichiarare cose diverse una volta cifrata la connessione
     * (tipicamente AUTHINFO in piu'). Vuota se non c'e' stato nessun upgrade,
     * o se la rilettura e' fallita.
     *
     * ⚠️ **Non va MAI usata per sapere se il server offre STARTTLS**: dopo un
     * upgrade riuscito quella capability non c'e' per definizione. Per quella
     * domanda esiste [advertisedCapabilities], che e' la fotografia d'arrivo.
     */
    var capabilitiesDopoTls: List<String> = emptyList()
        private set

    /**
     * Ordine dei campi OVER/XOVER dichiarato dal server, interrogato una sola
     * volta per connessione (bug beta11: prima si assumeva sempre l'ordine
     * RFC di default, ma il protocollo NON lo garantisce — un server con
     * overview.fmt non-standard rompeva silenziosamente il parsing di
     * References, quindi il prefetch delle risposte). null finché non
     * ancora richiesto.
     */
    private var overviewFormatCache: List<String>? = null

    /**
     * Alcuni server (es. Astraweb) NON conoscono OVER e rispondono con un codice
     * di "comando ignoto"; una volta scoperto su questa connessione, andiamo
     * dritti a XOVER per non sprecare un comando fallito a ogni intervallo.
     */
    private var overNonSupportato = false

    var serverGreeting: String = ""
        private set
    var postingAllowed: Boolean = false
        private set

    /**
     * Identificatore breve di QUESTA connessione, solo per la diagnostica
     * (intestazione/piè di sessione in NntpLog — punto 3 della proposta
     * HAL/Bingo3331, 1 settembre 2026): fa corrispondere il "chiusa dopo…" al
     * "sessione avviata…" giusto quando più sessioni si susseguono nello
     * stesso buffer (es. un server per volta durante syncAllSubscribed). Non
     * ha alcun ruolo nel protocollo NNTP.
     */
    private var sessionId: String = ""
    private var sessionStartNs: Long = 0
    private var comandiSessione = 0
    private var erroriSessione = 0

    fun connect(): NntpResponse {
        // Connetto sempre un socket TCP normale col timeout richiesto…
        //
        // beta17 (segnalazione operatore tcpreset/Gabx): FALLBACK DUAL-STACK.
        // Prima si faceva `Socket().connect(InetSocketAddress(host, port))`, che
        // con un host che pubblica sia IPv6 (AAAA) sia IPv4 (A) prova UN SOLO
        // indirizzo (spesso l'IPv6) e, se quello fallisce, si arrende — anche se
        // l'altro indirizzo funziona. Caso reale: news.tcpreset.net aveva la 563
        // TLS in ascolto solo su IPv4, NeReO provava l'IPv6, riceveva RST e non
        // ripiegava. Ora risolvo TUTTI gli indirizzi e li provo in sequenza
        // finché uno si connette (Piano A: fallback sequenziale, non Happy
        // Eyeballs parallelo — rispetta l'architettura sincrona del client).
        // Timeout PER-TENTATIVO ridotto: con più indirizzi morti, l'utente non
        // deve aspettare connectTimeoutMs pieno moltiplicato per ognuno.
        val indirizzi = InetAddress.getAllByName(host)
        val perAttemptMs =
            if (indirizzi.size > 1) (connectTimeoutMs / indirizzi.size).coerceAtLeast(2000)
            else connectTimeoutMs
        var plain: Socket? = null
        var ultimoErrore: IOException? = null
        for (addr in indirizzi) {
            val tentativo = Socket()
            try {
                tentativo.connect(InetSocketAddress(addr, port), perAttemptMs)
                plain = tentativo
                break
            } catch (e: IOException) {
                ultimoErrore = e
                runCatching { tentativo.close() }
            }
        }
        // Se nessun indirizzo ha risposto, rilancio l'ultimo errore (comportamento
        // osservabile invariato per il chiamante: resta una IOException).
        // NB: uso una `val` locale perché Kotlin NON fa smart-cast a non-null su
        // una `var` mutabile — senza questa, `plain` resterebbe Socket? più sotto
        // (createSocket/return) e non compilerebbe.
        val plainOk: Socket = plain ?: throw (ultimoErrore ?: IOException("No address for $host"))
        plainOk.soTimeout = readTimeoutMs
        // …poi, se SSL, ci sovrappongo il TLS PASSANDO l'hostname: così parte
        // l'SNI, indispensabile per i provider in hosting condiviso (molti
        // server — es. dietro OVH — senza SNI rifiutano l'handshake o
        // presentano il certificato sbagliato). eternal-september funziona in
        // entrambi i casi; usenet.ovh richiede l'SNI.
        val s: Socket = if (ssl) {
            val factory = fabbricaTls()
            (factory.createSocket(plainOk, host, port, true) as SSLSocket).apply {
                sslParameters = sslParameters.apply {
                    // 1.2.15 (voce E) — l'SNI si manda solo se l'host è un NOME.
                    // Vedi [eIndirizzoIp]: con un IPv6 letterale SNIHostName
                    // lanciava IllegalArgumentException, che qui NON è catturata
                    // (peschiamo IOException) — l'utente vedeva un errore
                    // incomprensibile invece di un messaggio. Con un IP l'SNI non
                    // serve comunque a nulla: RFC 6066 §3 vieta esplicitamente di
                    // metterci un indirizzo letterale.
                    if (!eIndirizzoIp(host)) {
                        serverNames = listOf(SNIHostName(host))
                    }
                    // 1.2.14 (audit di sicurezza §1.1) — LA riga che mancava.
                    // L'SNI qui sopra dice al server CHI stiamo cercando, ma non
                    // verifica niente: su una SSLSocket grezza Android controlla
                    // che il certificato sia firmato da una CA valida e basta.
                    // Senza questa impostazione, chiunque si trovi in mezzo
                    // (Wi-Fi pubblico, DNS dirottato) poteva presentare un
                    // certificato valido per un dominio SUO, l'handshake
                    // riusciva, effectivelySsl diventava true, la guardia
                    // anti-credenziali-in-chiaro si riteneva soddisfatta e
                    // AUTHINFO USER/PASS partiva verso l'attaccante — senza un
                    // avviso, senza un lucchetto rotto, senza nulla.
                    // "HTTPS" è il nome storico dell'algoritmo: identifica la
                    // regola di confronto fra nome host e certificato (RFC 2818
                    // / RFC 6125), non il protocollo applicativo — è quello
                    // giusto anche per NNTP.
                    // 1.4: la condizione sta ora in verificaNomeHost() —
                    // un pin approvato SOSTITUISCE questa prova, non la toglie.
                    if (verificaNomeHost()) {
                        endpointIdentificationAlgorithm = "HTTPS"
                    }
                }
                try {
                    startHandshake()
                } catch (e: javax.net.ssl.SSLException) {
                    // 1.3.3: la diagnosi (catena vs nome host) e il testo stanno
                    // ora in un posto solo — vedi spiegazioneErroreTls.
                    val causa = classificaErroreTls(e)
                    NntpLog.diag("handshake TLS fallito verso $host [$causa]: ${e.message}")
                    runCatching { close() }
                    throw IOException(spiegazioneErroreTls(host, e))
                } catch (e: IOException) {
                    // 1.6.1: un timeout durante l'handshake non e' una SSLException:
                    // `socket` non e' ancora assegnato, e close() non lo vedrebbe.
                    runCatching { close() }
                    throw e
                }
            }
        } else {
            plainOk
        }
        // Timeout di lettura sul socket da cui leggiamo DAVVERO. Il plain aveva
        // già il suo (serve durante l'handshake TLS, che legge dal plain), ma
        // l'SSLSocket sovrapposto NON eredita il soTimeout del socket sottostante:
        // le read applicative passano da `s`, quindi il timeout va (ri)messo qui,
        // altrimenti una connessione TLS che si pianta a metà risposta resterebbe
        // appesa senza scadenza. (Segnalazione tester esperto NNTP.) Per il caso
        // non-SSL `s` è lo stesso plainOk: riscriverlo è idempotente.
        s.soTimeout = readTimeoutMs
        socket = s
        input = BufferedInputStream(s.getInputStream())
        output = BufferedOutputStream(s.getOutputStream())
        val greeting = readResponse()
        if (greeting.code !in intArrayOf(200, 201)) {
            throw NntpException(greeting.code, greeting.text)
        }
        serverGreeting = greeting.text
        postingAllowed = greeting.code == 200
        // Diagnostica (punto 3, HAL/Bingo3331): la "sessione" nel senso del log
        // inizia qui, dopo il saluto — comandi/errori/durata contati da questo
        // istante, QUIT/close() ne scrivono il piè corrispondente.
        sessionId = java.lang.Long.toString(System.nanoTime() and 0xFFFFFFL, 36)
        sessionStartNs = System.nanoTime()
        comandiSessione = 0
        erroriSessione = 0
        NntpLog.sessionStart(host, port, ssl, sessionId)
        // CAPABILITIES (roadmap NNTP, Tier 1 punto 2): attiva un metodo che
        // esisteva ma non era mai stato chiamato. Solo diagnostica per ora —
        // AFFIANCA il fallback reattivo esistente (OVER→XOVER, LIST ACTIVE→LIST
        // ecc.), non lo sostituisce: un server con CAPABILITIES parziali o
        // assenti si comporta esattamente come prima. Mai un errore fatale qui.
        runCatching { advertisedCapabilities = capabilities() }
        if (advertisedCapabilities.isNotEmpty()) {
            // 1.4.11 / 156: la resa sta in `CapabilitiesLog.kt`, Kotlin puro e
            // provabile con kotlinc. Prima qui c'era `.take(300)`, che tagliava
            // SENZA DIRLO: il 12 ottobre 2026 ha fatto concludere che tcpreset
            // non offrisse STARTTLS, mentre lo offre e il nome stava oltre il
            // taglio. Vedi l'intestazione di quel file.
            NntpLog.diag("CAPABILITIES: " + capabilitiesPerIlLog(advertisedCapabilities))
        }
        // STARTTLS (voce E, roadmap NNTP — RFC 4642): upgrade a cifrato DOPO la
        // connessione, per i server rimasti su una porta in chiaro. Solo se
        // richiesto e se non si è già in TLS implicito.
        if (!ssl && startTls) tentaStartTls()
        // MODE READER: obbligatorio su alcuni server prima dei comandi da
        // lettore (LIST, GROUP...). Se il server non lo conosce (500/502)
        // proseguiamo senza: era già in modalità lettore.
        try {
            val mr = command("MODE READER")
            if (mr.code == 200) postingAllowed = true
            if (mr.code == 201) postingAllowed = false
        } catch (_: IOException) { /* server minimale: ignora */ }
        return greeting
    }

    /**
     * Esegue l'upgrade STARTTLS su una connessione già stabilita in chiaro
     * (chiamata solo da [connect] quando richiesto).
     *
     * 1.4 — I DUE CASI DELLA RFC 4642 NON SONO LO STESSO CASO.
     *
     * Fino alla 1.3.7 questa funzione era "best-effort per costruzione":
     * qualunque rifiuto O errore lasciava [effectivelySsl] a false e la
     * sessione proseguiva in chiaro. Per uno dei due casi era lecito; per
     * l'altro la RFC dice il contrario.
     *
     *  - **Rifiuto PRIMA dell'handshake** (risposta diversa da 382): il client
     *    "may continue in cleartext according to its own policy". Lecito, e
     *    resta com'era — si annota nel log e si prosegue. La guardia della
     *    1.2.6 protegge comunque le credenziali.
     *
     *  - **382 seguito da handshake FALLITO**: RFC 4642 §2.2 — client e server
     *    **DEVONO chiudere la connessione**. Non e' una preferenza: dopo il 382
     *    entrambe le parti considerano il flusso passato a TLS, e il nostro
     *    ClientHello e' gia' stato scritto su quel socket. Il "ripiego in
     *    chiaro" che facevamo probabilmente non funzionava nemmeno — quel
     *    flusso contiene byte TLS — ma soprattutto non doveva essere tentato.
     *
     * Conseguenza voluta: nel secondo caso [connect] FALLISCE. Un server che
     * annuncia STARTTLS e poi non lo porta a termine smette di collegarsi, e
     * l'utente lo scopre da un errore invece che da una sessione silenziosamente
     * in chiaro. E' la stessa scelta della 1.2.14 (verifica del nome host): si
     * preferisce un errore visibile a una falsa sicurezza.
     *
     * @throws NntpException se l'handshake fallisce dopo un 382.
     */
    private fun tentaStartTls() {
        // Distingue i due casi della RFC: diventa true solo DOPO il 382, cioe'
        // quando entrambe le parti si aspettano TLS su quel flusso.
        var dopoIl382 = false
        try {
            val r = command("STARTTLS")
            if (r.code != 382) {
                NntpLog.diag("STARTTLS rifiutato dal server (${r.code} ${r.text}): la connessione resta in chiaro.")
                return
            }
            dopoIl382 = true
            val plain = socket ?: return
            val factory = fabbricaTls()
            val tls = (factory.createSocket(plain, host, port, true) as SSLSocket).apply {
                sslParameters = sslParameters.apply {
                    // 1.2.15 (voce E): SNI solo su un nome, vedi connect().
                    if (!eIndirizzoIp(host)) {
                        serverNames = listOf(SNIHostName(host))
                    }
                    // 1.2.14: stessa verifica del TLS implicito, vedi connect().
                    // 1.4: la condizione sta ora in verificaNomeHost() —
                    // un pin approvato SOSTITUISCE questa prova, non la toglie.
                    if (verificaNomeHost()) {
                        endpointIdentificationAlgorithm = "HTTPS"
                    }
                }
                startHandshake()
            }
            tls.soTimeout = readTimeoutMs
            socket = tls
            input = BufferedInputStream(tls.getInputStream())
            output = BufferedOutputStream(tls.getOutputStream())
            effectivelySsl = true
            NntpLog.diag("STARTTLS riuscito: connessione ora cifrata.")
            // Il server può dichiarare CAPABILITIES diverse una volta cifrata la
            // connessione (RFC 4642 §2.3, es. AUTHINFO in più): le rileggo.
            //
            // ⚠️ 1.4.11 / 155: il risultato va in un campo SUO. Fino alla 154
            // sovrascriveva `advertisedCapabilities`, e siccome qui dentro
            // STARTTLS non c'e' piu' (il server non deve piu' annunciarlo), il
            // par. B3 leggeva quella lista e dava l'allarme sulle connessioni
            // RIUSCITE. Vedi il commento esteso su `advertisedCapabilities`.
            runCatching { capabilitiesDopoTls = capabilities() }
        } catch (e: IOException) {
            // 1.2.14: distinguo il fallimento del CERTIFICATO da un guasto
            // qualunque. Questa riga nel log NNTP dice esattamente cosa è
            // successo e dove sta l'interruttore.
            //
            // 1.3.3: stessa diagnosi del TLS implicito, stesso testo. Prima
            // qui c'era una seconda formulazione, che suggeriva l'interruttore
            // anche quando non poteva servire a nulla.
            val diagnosi = if (e is javax.net.ssl.SSLException) {
                "STARTTLS [${classificaErroreTls(e)}]: ${spiegazioneErroreTls(host, e)}"
            } else {
                "STARTTLS fallito (${e.message})"
            }

            if (!dopoIl382) {
                // Guasto PRIMA del 382: nessun impegno reciproco a passare a
                // TLS. Comportamento invariato rispetto alla 1.3.7.
                NntpLog.diag("$diagnosi La connessione resta in chiaro.")
                return
            }

            // Dopo il 382: RFC 4642 §2.2 — si chiude, e si dice perche'.
            NntpLog.diag("$diagnosi Il server aveva accettato (382): connessione CHIUSA, come impone la RFC 4642.")
            runCatching { socket?.close() }
            socket = null
            input = null
            output = null
            effectivelySsl = false
            // Messaggio in inglese come tutto questo livello (1.3.5): NntpClient
            // non ha un Context e non puo' leggere strings.xml.
            throw NntpException(
                580,
                "STARTTLS handshake failed after the server accepted it (382). " +
                    "RFC 4642 requires both sides to close: the connection was NOT " +
                    "continued in cleartext.",
            )
        }
    }

    /**
     * 1.2.6 (roadmap NNTP, priorità 1 — sicurezza): su una connessione NON cifrata
     * `AUTHINFO PASS` manderebbe la password in CHIARO sulla rete (RFC 4643 lo vieta).
     * Di default rifiutiamo prima di inviare qualunque credenziale. [allowPlaintext]
     * (override, spento di default, da Impostazioni) consente l'eccezione consapevole
     * per chi ha un server legacy solo-testo che richiede comunque l'auth.
     */
    /**
     * 1.2.17 (voce B) — CREDENZIALI PER L'AUTENTICAZIONE PIGRA.
     *
     * Quando è valorizzata, [command] sa autenticarsi da sé al primo comando
     * che il server rifiuta perché non siamo autenticati, e ripetere il
     * comando. Il chiamante non chiama più [authenticate] appena connesso: la
     * password parte solo se e quando serve davvero.
     *
     * Perché conta: su un server senza cifratura (`news.usenet.ovh`, che
     * annuncia STARTTLS ma risponde `580 Error initializing TLS` — vedi
     * NeReO_ovh_starttls_in_chiaro.md) la password viaggia IN CHIARO ad ogni
     * collegamento. INN lascia leggere senza autenticazione e chiede le
     * credenziali solo per pubblicare: con questa strada la password passa
     * dalla rete qualche volta al mese invece che decine di volte al giorno.
     * Non risolve il problema — lo riduce di due ordini di grandezza.
     *
     * `terzo` = allowPlaintext, cioè il consenso già dato dall'utente.
     */
    var credenzialiPigre: Triple<String, String, Boolean>? = null

    /** true quando ci siamo già autenticati su questa connessione. */
    private var autenticato = false

    /** Guardia anti-ricorsione: [authenticate] usa [command], che non deve richiamarla. */
    private var autenticazioneInCorso = false

    fun authenticate(user: String, pass: String, allowPlaintext: Boolean = false) {
        // 1.2.9: guardo la cifratura EFFETTIVA (TLS implicito o STARTTLS
        // riuscito), non solo il parametro [ssl] del costruttore — altrimenti
        // chi si connette in chiaro e ottiene la cifratura via STARTTLS si
        // vedrebbe bloccare l'auth per errore, e chi la richiede senza
        // ottenerla la manderebbe comunque in chiaro.
        if (!effectivelySsl && !allowPlaintext) {
            // 1.3.5: testo in inglese — vedi la nota su spiegazioneErroreTls.
            // È il messaggio più letto di tutta la classe: lo incontra chiunque
            // aggiunga un server con account partendo dai preset, che
            // compilano porta 119 senza SSL.
            throw NntpException(
                483,
                "Unencrypted connection: your password will not be sent in the clear. " +
                    "Use the SSL/TLS port (usually 563), or — only if you know what you " +
                    "are doing — turn on \"Allow unencrypted authentication\" in Maintenance.",
            )
        }
        autenticazioneInCorso = true
        try {
            var r = command("AUTHINFO USER $user")
            if (r.code == 381) {
                /*
                 * 1.4: UNA PASSWORD VUOTA NON E' UNA CREDENZIALE.
                 *
                 * Prima mandavamo "AUTHINFO PASS" senza argomento, e il server
                 * rispondeva con un errore di SINTASSI:
                 *   501 Syntax is: AUTHINFO USER name|PASS password|SASL ...
                 * che non dice niente a nessuno. Visto il 17 settembre 2026 nel
                 * log di Marco, su tre sessioni Paganini di fila.
                 *
                 * La guardia sta QUI dentro, nel ramo del 381, e non prima
                 * dell'AUTHINFO USER: la RFC 4643 §2.3.1 prevede che un server
                 * che richiede il SOLO nome utente risponda 281 direttamente e
                 * "MUST NOT give a 381 response to AUTHINFO USER". Uscire prima
                 * per password vuota romperebbe quel caso, che e' legittimo.
                 * Se siamo qui, il 381 l'ha chiesto il server: la password
                 * serve davvero, e non ce l'abbiamo.
                 *
                 * Codice 581: locale, come il 580 dello STARTTLS. Non e' una
                 * risposta del server e non deve sembrarlo.
                 */
                if (pass.isEmpty()) {
                    // 1.3.5: testo in inglese — vedi la nota su spiegazioneErroreTls.
                    throw NntpException(
                        581,
                        "The server asked for a password for user \"$user\", but no password " +
                            "is saved for this server. Open Settings > Servers and enter it - " +
                            "or clear the user name, if this server does not need an account.",
                    )
                }
                r = command("AUTHINFO PASS $pass")
            }
            if (r.code != 281) throw NntpException(r.code, "Authentication failed: ${r.text}")
            autenticato = true
        } finally {
            autenticazioneInCorso = false
        }
    }

    fun capabilities(): List<String> {
        val r = command("CAPABILITIES")
        if (r.code != 101) return emptyList()
        return readMultilineText()
    }

    /** LIST ACTIVE — l'elenco completo dei gruppi. Sui server grossi sono centinaia di migliaia di righe. */
    fun listActive(wildmat: String? = null): Sequence<GroupEntry> {
        val cmd = if (wildmat != null) "LIST ACTIVE $wildmat" else "LIST ACTIVE"
        var r = command(cmd)
        // fallback per server datati che conoscono solo il LIST semplice
        if (r.code in intArrayOf(500, 501) && wildmat == null) r = command("LIST")
        if (r.code != 215) throw NntpException(r.code, r.text)
        return readMultilineText().asSequence().mapNotNull { line ->
            val p = line.split(' ')
            if (p.size >= 4) GroupEntry(p[0], p[1].toLongOrNull() ?: 0, p[2].toLongOrNull() ?: 0, p[3])
            else null
        }
    }

    fun group(name: String): GroupStats {
        val r = command("GROUP $name")
        if (r.code != 211) throw NntpException(r.code, r.text)
        val p = r.text.trim().split(' ')
        return GroupStats(
            estimatedCount = p.getOrNull(0)?.toLongOrNull() ?: 0,
            low = p.getOrNull(1)?.toLongOrNull() ?: 0,
            high = p.getOrNull(2)?.toLongOrNull() ?: 0,
            name = p.getOrNull(3) ?: name,
        )
    }

    /**
     * Ordine reale dei campi OVER/XOVER per QUESTO server, in minuscolo,
     * senza i due punti (es. "subject", "from", ":bytes" → "bytes"). La
     * colonna 0 della riga OVER (numero articolo) non è mai inclusa qui:
     * va sempre sommato +1 all'indice trovato in questa lista.
     *
     * Se il server non supporta LIST OVERVIEW.FMT (risposta 500/501),
     * ripiega sull'ordine RFC 3977 "di default" — che è comunque solo una
     * convenzione, non una garanzia; da qui il bug che stiamo correggendo.
     */
    fun overviewFormat(): List<String> {
        overviewFormatCache?.let { return it }
        val default = listOf("subject", "from", "date", "message-id", "references", "bytes", "lines")
        val r = command("LIST OVERVIEW.FMT")
        val result = if (r.code != 215) {
            default
        } else {
            readMultilineText().mapNotNull { raw ->
                val t = raw.trim()
                when {
                    t.isEmpty() -> null
                    t.startsWith(":") -> t.removePrefix(":").substringBefore(':').lowercase().ifBlank { null }
                    else -> t.removeSuffix(":").substringBefore(':').lowercase().ifBlank { null }
                }
            }.ifEmpty { default }
        }
        overviewFormatCache = result
        return result
    }

    /**
     * OVER (RFC 3977) con XOVER (bozza storica) come fallback.
     *
     * Alcuni server — Astraweb in primis — non conoscono OVER e rispondono
     * "400 Unrecognized command". Nota: 400 è NON-standard qui (per RFC 3977
     * sarebbe "servizio non disponibile"; "comando ignoto" è 500) — perciò il
     * vecchio fallback tarato solo su 500/501 non lo intercettava e l'app
     * lanciava l'errore. Ora ripieghiamo su XOVER a QUALUNQUE codice di
     * "comando ignoto" (400/500/501/502) e ricordiamo l'esito per la sessione.
     */
    fun over(low: Long, high: Long): List<OverviewLine> {
        val formato = overviewFormat()
        fun comandoIgnoto(code: Int) = code == 400 || code == 500 || code == 501 || code == 502
        var r = if (overNonSupportato) {
            command("XOVER $low-$high")
        } else {
            val ro = command("OVER $low-$high")
            if (comandoIgnoto(ro.code)) {
                overNonSupportato = true // su questa connessione OVER non c'è: da ora XOVER diretto
                command("XOVER $low-$high")
            } else {
                ro
            }
        }
        // 1.3.2 (crash-test di BIG Umberto su alt.test.a): il 423 NON è un
        // errore, è una risposta normale.
        //
        // RFC 3977 §8.3, OVER: 423 = "No articles in that range". Diverso dal
        // 423 di ARTICLE (§6.2.1, "No article with that number"): lì il
        // soggetto è un numero, qui un INTERVALLO. La forma "2162-2162" nel
        // messaggio del server è la firma di questo, non di ARTICLE.
        //
        // Come ci si arriva: l'ultimo articolo del gruppo viene cancellato. Il
        // water mark alto NON scende (RFC §6.1.1: "not all article numbers
        // between the reported low and high water marks necessarily correspond
        // to existing articles"), quindi GROUP continua a dichiarare 2162,
        // l'intervallo da chiedere diventa 2162-2162 e lì dentro non c'è più
        // nulla. Il buco è previsto dal protocollo, non è un'anomalia.
        //
        // Cosa faceva prima: lanciava. L'errore arrivava all'utente, la
        // connessione condivisa veniva buttata e riaperta per i gruppi
        // rimanenti, e il segnalibro NON avanzava — quindi l'aggiornamento
        // dopo richiedeva lo stesso intervallo morto, all'infinito, finché
        // qualcuno non postava. Riprodotto tre volte su tre server diversi
        // (Solani, eternal-september, Paganini): il codice è standard, il
        // difetto era nostro.
        //
        // Ora: lista vuota. Il chiamante prosegue e porta il segnalibro a
        // stats.high, così l'intervallo morto viene consumato una volta sola.
        // Non è una politica nuova: il segnalibro avanza già a stats.high
        // quando il 224 torna con MENO righe di quelle chieste (il caso
        // Astraweb). Qui si applica la stessa regola a una risposta che, per
        // giunta, è un'affermazione esplicita del server e non un silenzio.
        if (r.code == 423) {
            NntpLog.diag(
                "423 su $low-$high: nessun articolo nell'intervallo " +
                    "(RFC 3977 §8.3) — non è un errore, il segnalibro avanza",
            )
            return emptyList()
        }
        if (r.code != 224) throw NntpException(r.code, r.text)
        // righe raw ⇒ decodifica intelligente (UTF-8 stretto → ISO-8859-15) prima del parse
        val parsed = readMultilineRaw().mapNotNull { parseOverviewLine(MimeUtil.smartDecodeHeader(it), formato) }
        // Diagnostica (punto 1, HAL/Bingo3331 — quello che risolve davvero il
        // problema del backfill Astraweb): quante righe sono arrivate rispetto
        // all'intervallo richiesto, e quali numeri mancano.
        NntpLog.overSummary(low, high, parsed.map { it.number })
        return parsed
    }

    /**
     * LISTGROUP (RFC 3977 §6.1.2): l'elenco VERO dei numeri articolo presenti
     * sul gruppo in questo momento — un segnale indipendente dal database
     * overview interrogato da OVER/XOVER. Un buco in XOVER non prova che
     * l'articolo non esista: potrebbe essere solo il database overview del
     * server ad avere il buco, non gli articoli (distinzione di HAL/Bingo3331,
     * 1 settembre 2026, confermata tecnicamente corretta).
     *
     * Comando SOLO diagnostico manuale (Fase 2): mai usato nella sync normale
     * — su un gruppo grosso la risposta può contenere centinaia di migliaia
     * di numeri, un costo che ha senso solo quando l'utente lo chiede
     * esplicitamente per indagare un caso preciso.
     */
    fun listGroup(name: String): List<Long> {
        val r = command("LISTGROUP $name")
        if (r.code != 211) throw NntpException(r.code, r.text)
        return readMultilineText().mapNotNull { it.trim().toLongOrNull() }
    }

    /**
     * STAT (RFC 3977 §6.2.4): verifica se un numero di articolo esiste
     * DAVVERO sul server, senza scaricarlo. Usato solo dalla diagnostica
     * manuale (Fase 2 + aggiunta di HAL): sui numeri presenti in LISTGROUP ma
     * assenti dal riepilogo OVER/XOVER, distingue un buco nel database
     * overview del server (STAT trova l'articolo, code 223) da un articolo
     * davvero scaduto/cancellato (qualunque altro codice — su un gruppo già
     * selezionato con GROUP/LISTGROUP l'unica risposta negativa plausibile è
     * 423 "nessun articolo con quel numero").
     */
    fun stat(number: Long): Boolean {
        val r = command("STAT $number")
        return r.code == 223
    }

    /**
     * 1.2.15 (voce C) — STAT per Message-ID invece che per numero.
     *
     * A differenza di [stat], NON richiede che un gruppo sia selezionato: per
     * Message-ID la ricerca è sull'intero server (RFC 3977 §6.2.4). Serve a
     * verificare, dopo un `cancel`, se il server ha davvero ritirato
     * l'articolo — 223 = c'è ancora, 430 = non c'è più.
     *
     * Restituisce null se il server risponde in un modo che non sappiamo
     * interpretare: "non lo so" è un esito legittimo e va distinto da "c'è" e
     * da "non c'è". Un errore di rete si propaga come eccezione: non è
     * compito di questa funzione decidere cosa farne.
     */
    fun statPerMessageId(messageId: String): Boolean? {
        val id = if (messageId.startsWith("<")) messageId else "<$messageId>"
        val r = command("STAT $id")
        return when (r.code) {
            223 -> true
            430 -> false
            else -> null
        }
    }

    /** Corpo + header di un articolo, come righe di BYTE grezzi (charset deciso a valle da MimeUtil). */
    fun article(numberOrId: String): List<ByteArray> = fetchMultilineRaw("ARTICLE $numberOrId", 220)

    fun head(numberOrId: String): List<ByteArray> = fetchMultilineRaw("HEAD $numberOrId", 221)

    fun body(numberOrId: String): List<ByteArray> = fetchMultilineRaw("BODY $numberOrId", 222)

    /**
     * 1.6: come [head], ma tiene il numero della riga di risposta (`221 n <id>`),
     * che [head] butta via. 0 = il server non lo dice. Vedi RicercaMessageId.kt.
     */
    fun headConNumero(numberOrId: String): TestaArticolo {
        val r = command("HEAD $numberOrId")
        if (r.code != 221) throw NntpException(r.code, r.text)
        return TestaArticolo(r.text.substringBefore(' ').toLongOrNull() ?: 0L, readMultilineRaw())
    }

    /** 1.6: il Message-ID dell'articolo [number] del gruppo selezionato, o null. */
    fun messageIdDelNumero(number: Long): String? {
        val r = command("STAT $number")
        return if (r.code == 223) r.text.split(' ').getOrNull(1) else null
    }

    /** POST di un articolo già formattato (header + riga vuota + corpo). */
    fun post(rawArticleLines: List<String>) {
        val r = command("POST")
        if (r.code != 340) throw NntpException(r.code, r.text)
        val out = output ?: error("Not connected")
        for (line in rawArticleLines) {
            // dot-stuffing RFC 3977 §3.1.1
            val stuffed = if (line.startsWith(".")) ".$line" else line
            out.write(stuffed.toByteArray(charset))
            out.write(CRLF)
        }
        out.write(DOT_CRLF)
        out.flush()
        val res = readResponse()
        if (res.code != 240) throw NntpException(res.code, res.text)
    }

    fun quit() {
        try { command("QUIT") } catch (_: IOException) { }
    }

    override fun close() {
        try {
            // 1.6.1 (BIG Umberto): connessione mai aperta, QUIT non ha senso e
            // command() lancerebbe. Connessione aperta: il saluto al server non
            // vale l'attesa di lettura intera, che su un server muto si sommava al
            // timeout appena scaduto, a mutex del sync occupato.
            if (output != null) {
                runCatching { socket?.soTimeout = ATTESA_QUIT_MS }
                quit()
            }
        } finally {
            // Piè di sessione (punto 3, HAL/Bingo3331): solo se una sessione è
            // davvero partita (sessionStartNs > 0) — connect() potrebbe non
            // essere mai stato chiamato con successo (es. errore prima del
            // saluto), nel qual caso non c'è nulla da chiudere nel log.
            if (sessionStartNs > 0) {
                val durataMs = (System.nanoTime() - sessionStartNs) / 1_000_000
                NntpLog.sessionEnd(sessionId, durataMs, comandiSessione, erroriSessione)
            }
            try { socket?.close() } catch (_: IOException) { }
            socket = null; input = null; output = null
        }
    }

    // ------------------------------------------------------------------ interni

    private fun command(cmd: String): NntpResponse {
        val out = output ?: error("Not connected: call connect() first")
        // 1.2.12 (audit di sicurezza): difesa in profondità contro l'iniezione
        // di comandi. Un a-capo dentro un nome di gruppo, un Message-ID o un
        // nome utente diventerebbe un COMANDO NNTP in più, eseguito dentro la
        // sessione già autenticata. I chiamanti dovrebbero già passare valori
        // puliti: qui si rifiuta comunque, perché è l'unico punto da cui passa
        // ogni comando e quindi l'unico posto dove la garanzia è completa.
        if (cmd.any { it == '\r' || it == '\n' }) {
            throw IOException("Invalid NNTP command: it contains a line break")
        }
        NntpLog.client(cmd) // diagnostica: registra il comando (credenziali oscurate alla fonte)
        comandiSessione++
        val inizioNs = System.nanoTime()
        out.write(cmd.toByteArray(charset))
        out.write(CRLF)
        out.flush()
        // 1.2.13 (diagnostica): se la RISPOSTA non arriva, il log finora
        // mostrava solo la riga "C: ..." e poi il vuoto — capire che il comando
        // era partito senza risposta ha richiesto di confrontare due log a
        // mano. Ora la caduta lascia una riga esplicita, con la causa.
        val risposta = try {
            readResponse(inizioNs)
        } catch (e: IOException) {
            NntpLog.diag("nessuna risposta al comando (${redactPerLog(cmd)}): ${e.message}")
            throw e
        }

        // 1.2.17 (voce B) — AUTENTICAZIONE PIGRA: ci si autentica QUI, al primo
        // comando che il server rifiuta perché non sa chi siamo, e il comando
        // viene ripetuto. Per il chiamante è invisibile: riceve la risposta al
        // secondo tentativo come se fosse la prima.
        //
        // 480 è il codice previsto da RFC 4643 §2 ("authentication required").
        // Accettiamo anche 502, che alcuni server usano per la stessa cosa —
        // costa al massimo un giro di rete sprecato, e solo finché non ci siamo
        // ancora autenticati.
        //
        // Sicuro da ripetere: 480/502 sono risposte NEGATIVE, quindi nessun
        // blocco multilinea le segue e non c'è nulla da consumare prima di
        // riprovare. La guardia [autenticazioneInCorso] impedisce che sia
        // authenticate() stessa a innescare il giro.
        if ((risposta.code == 480 || risposta.code == 502) &&
            !autenticato && !autenticazioneInCorso
        ) {
            val cred = credenzialiPigre
            if (cred != null) {
                NntpLog.diag("il server chiede l'autenticazione (${risposta.code}): mi autentico ora e ripeto ${redactPerLog(cmd)}")
                authenticate(cred.first, cred.second, cred.third)
                return command(cmd)
            }
        }
        return risposta
    }

    /** Nome del comando senza argomenti: nel log diagnostico non serve altro, e non può portarsi dietro credenziali. */
    private fun redactPerLog(cmd: String): String = cmd.substringBefore(' ')

    private fun fetchMultilineRaw(cmd: String, expectCode: Int): List<ByteArray> {
        val r = command(cmd)
        if (r.code != expectCode) throw NntpException(r.code, r.text)
        return readMultilineRaw()
    }

    /**
     * [inizioNs], se noto, è l'istante di invio del comando (System.nanoTime):
     * la differenza va nella riga di log come durata del comando (punto 2,
     * HAL/Bingo3331). null per il saluto iniziale in connect(), che non è la
     * risposta a un comando inviato da noi.
     */
    private fun readResponse(inizioNs: Long? = null): NntpResponse {
        val line = readLine() ?: throw IOException("Connection closed by the server")
        val code = line.take(3).toIntOrNull() ?: throw IOException("Malformed NNTP response: $line")
        val text = line.drop(4)
        // Conteggio errori di sessione per il piè (punto 3): 4xx/5xx presi come
        // "errore" nel senso ampio della diagnostica, non nel senso stretto di
        // ogni singola funzione (che decide da sé cosa considerare un errore
        // per il chiamante — es. STAT usa 223 vs tutto il resto).
        //
        // 1.2.17 — TRE ECCEZIONI, tutte risposte che oggi ci ASPETTIAMO:
        //  430 "no such article": dalla 1.2.15 è la risposta cercata dalla
        //      verifica dopo un ritiro, e dalla 1.2.16 lo è per costruzione —
        //      un ritiro RIUSCITO faceva scrivere "1 errori" nel riepilogo;
        //  580 STARTTLS non avviabile: guasto del server, gestito e loggato a
        //      parte con la sua riga di diagnostica;
        //  480 autenticazione richiesta: con l'autenticazione pigra è il segnale
        //      normale che innesca l'invio delle credenziali, non un guasto.
        // Contarle confondeva chi legge il log mesi dopo — cioè noi.
        if (code >= 400 && code != 430 && code != 580 && code != 480) erroriSessione++
        val durataMs = inizioNs?.let { (System.nanoTime() - it) / 1_000_000 }
        NntpLog.server(code, text, durataMs) // diagnostica: registra la riga di stato (anche il saluto iniziale)
        return NntpResponse(code, text)
    }

    /** Blocco multilinea come TESTO (per comandi ASCII: LIST, CAPABILITIES, LISTGROUP). */
    private fun readMultilineText(): List<String> {
        val lines = ArrayList<String>(256)
        var totale = 0L
        try {
            while (true) {
                val line = readLine() ?: throw IOException("Connection closed in the middle of a data block")
                if (line == ".") break
                lines.add(if (line.startsWith("..")) line.substring(1) else line)
                // 1.2.12: tetto complessivo, vedi readMultilineRaw
                totale += line.length.toLong() + 2
                if (totale > MAX_BLOCCO_BYTE) {
                    throw IOException("Response block exceeded the $MAX_BLOCCO_BYTE byte limit: aborted")
                }
            }
        } catch (e: IOException) {
            // Diagnostica: un blocco multilinea troncato a metà (timeout, reset)
            // è esattamente il genere di sintomo che il vecchio log non poteva
            // mostrare — si vedeva solo "S: 224/215 ..." e poi il buco.
            NntpLog.diag("blocco multilinea interrotto dopo ${lines.size} righe: ${e.message}")
            throw e
        }
        return lines
    }

    /**
     * Blocco multilinea come BYTE grezzi (ARTICLE/HEAD/BODY/OVER: charset deciso
     * a valle).
     *
     * 1.2.12 (audit di sicurezza): tetto complessivo di [MAX_BLOCCO_BYTE] su
     * quanto si accumula in memoria. NeReO è un lettore SOLO TESTO: un articolo
     * legittimo sta in poche decine di kilobyte, e anche un OVER di un gruppo
     * molto popoloso resta ordini di grandezza sotto questo limite. Senza il
     * tetto, un solo articolo enorme (o un blocco che non finisce mai) fa
     * morire l'app con un `OutOfMemoryError` che nessun `catch (Exception)`
     * intercetta — ed è un articolo che chiunque, al mondo, può pubblicare.
     */
    private fun readMultilineRaw(): List<ByteArray> {
        val lines = ArrayList<ByteArray>(256)
        val dot = '.'.code.toByte()
        var totale = 0L
        try {
            while (true) {
                val line = readLineBytes() ?: throw IOException("Connection closed in the middle of a data block")
                if (line.size == 1 && line[0] == dot) break
                // de-dot-stuffing: ".." iniziale ⇒ "."
                lines.add(if (line.size >= 2 && line[0] == dot && line[1] == dot) line.copyOfRange(1, line.size) else line)
                totale += line.size.toLong() + 2
                if (totale > MAX_BLOCCO_BYTE) {
                    throw IOException("Response block exceeded the $MAX_BLOCCO_BYTE byte limit: aborted")
                }
            }
        } catch (e: IOException) {
            NntpLog.diag("blocco multilinea interrotto dopo ${lines.size} righe: ${e.message}")
            throw e
        }
        return lines
    }

    private val lineBuf = java.io.ByteArrayOutputStream(256)

    /**
     * Legge una riga come byte grezzi (fino a \n, togliendo l'eventuale \r finale).
     *
     * 1.2.12 (audit di sicurezza): tetto di [MAX_RIGA_BYTE] sulla singola riga.
     * Senza, un server ostile (o un intermediario) che invii byte senza mai un
     * a-capo fa crescere il buffer fino a `OutOfMemoryError` — che è un `Error`,
     * non una `Exception`, quindi NESSUNO dei `catch` dell'app lo intercetta:
     * l'app muore e basta. Il tetto è larghissimo rispetto a qualunque riga
     * legittima di Usenet (le righe di testo stanno sotto il kilobyte, quelle
     * di un Face ripiegato sotto il centinaio di byte).
     */
    private fun readLineBytes(): ByteArray? {
        val inp = input ?: error("Not connected")
        lineBuf.reset()
        while (true) {
            val b = inp.read()
            if (b == -1) return if (lineBuf.size() == 0) null else lineBuf.toByteArray()
            if (b == '\n'.code) {
                var bytes = lineBuf.toByteArray()
                if (bytes.isNotEmpty() && bytes.last() == '\r'.code.toByte()) bytes = bytes.copyOf(bytes.size - 1)
                return bytes
            }
            lineBuf.write(b)
            if (lineBuf.size() > MAX_RIGA_BYTE) {
                lineBuf.reset() // non tenersi in memoria ciò che si sta rifiutando
                throw IOException("Response line exceeded the $MAX_RIGA_BYTE byte limit: server response ignored")
            }
        }
    }

    /** Riga di protocollo (ASCII) decodificata col charset di trasporto. */
    private fun readLine(): String? = readLineBytes()?.let { String(it, charset) }

    companion object {
        private val CRLF = byteArrayOf(13, 10)
        private val DOT_CRLF = byteArrayOf(46, 13, 10)

        /**
         * 1.2.15 (voce E) — true se [h] è un indirizzo IP LETTERALE, non un
         * nome di host.
         *
         * Misurato su JDK 21 (7 settembre 2026): `SNIHostName` accetta senza
         * batter ciglio un IPv4 in forma "157.180.91.226", mentre su un IPv6
         * ("2a01:4f9:c012:f55a::1") lancia IllegalArgumentException
         * ("Contains non-LDH ASCII characters"). Quella eccezione, in
         * connect(), non era catturata da nessuno: chi inseriva un IPv6 come
         * host vedeva un errore incomprensibile invece di un messaggio.
         *
         * Il criterio è volutamente grossolano — due soli casi da riconoscere,
         * entrambi impossibili in un nome di host valido:
         * - i due punti compaiono SOLO in un IPv6 (nel campo host la porta è
         *   separata, non incollata);
         * - una stringa di sole cifre e punti non può essere un nome DNS
         *   registrabile.
         * Un falso positivo costerebbe soltanto un SNI non inviato; un falso
         * negativo, un'eccezione. L'asimmetria giustifica la severità.
         */
        internal fun eIndirizzoIp(h: String): Boolean =
            h.contains(':') || h.matches(Regex("^[0-9]{1,3}(\\.[0-9]{1,3}){3}$"))

        /**
         * 1.2.12 (audit di sicurezza) — tetti di sanità sulle risposte del
         * server. Non sono limiti "di prodotto": sono la rete che impedisce a
         * una risposta ostile o malformata di consumare memoria senza fine.
         *
         * Sono volutamente MOLTO larghi rispetto a Usenet reale, così da non
         * poter mai colpire un contenuto legittimo: una riga di testo sta sotto
         * il kilobyte (1 MB = mille volte tanto) e un articolo solo-testo sta in
         * decine di kilobyte, contro i 64 MB concessi al blocco.
         *
         * Il blocco più grande che l'app scarica davvero non è un articolo ma
         * l'elenco gruppi (LIST) o un riepilogo overview (XOVER): il primo resta
         * ampiamente sotto anche sui provider con feed enorme, il secondo è già
         * limitato a monte dall'impostazione "quanti messaggi per gruppo".
         * L'unico caso che potrebbe avvicinarsi è "tutti" impostato su un gruppo
         * gigantesco — che oggi, senza tetto, non finisce comunque bene: almeno
         * ora dà un errore leggibile invece di un arresto anomalo.
         *
         * Se un tetto dovesse mai scattare su contenuto legittimo, il sintomo è
         * chiaro e diagnosticabile (errore esplicito, visibile anche nel log
         * NNTP) e il numero si alza in una riga.
         */
        private const val MAX_RIGA_BYTE = 1L * 1024 * 1024
        private const val MAX_BLOCCO_BYTE = 64L * 1024 * 1024

        private val DEFAULT_OVERVIEW_FORMAT =
            listOf("subject", "from", "date", "message-id", "references", "bytes", "lines")

        /**
         * [formato] = ordine reale dei campi (da [NntpClient.overviewFormat]),
         * SENZA la colonna 0 (numero articolo, sempre implicita e sempre
         * prima). Se un campo atteso non è nell'elenco del server, ripiega
         * sulla sua posizione RFC di default invece di fallire — meglio un
         * dato probabilmente giusto che nessun dato.
         */
        fun parseOverviewLine(line: String, formato: List<String> = DEFAULT_OVERVIEW_FORMAT): OverviewLine? {
            val f = line.split('\t')
            if (f.size < 8) return null

            fun colonna(nomeCampo: String): Int {
                val i = formato.indexOf(nomeCampo)
                val idx = if (i >= 0) i + 1 else (DEFAULT_OVERVIEW_FORMAT.indexOf(nomeCampo) + 1)
                return idx.coerceIn(0, f.size - 1)
            }

            val subjectC = colonna("subject")
            val fromC = colonna("from")
            val dateC = colonna("date")
            val messageIdC = colonna("message-id")
            val referencesC = colonna("references")
            val bytesC = colonna("bytes")
            val linesC = colonna("lines")

            return OverviewLine(
                number = f[0].toLongOrNull() ?: return null,
                subject = MimeUtil.decodeWords(f[subjectC]),
                from = MimeUtil.decodeWords(f[fromC]),
                date = f[dateC],
                messageId = f[messageIdC],
                references = f[referencesC],
                bytes = f[bytesC].trim().toLongOrNull() ?: 0,
                lines = f[linesC].trim().toIntOrNull() ?: 0,
                extra = if (f.size > 8) f.subList(8, f.size) else emptyList(),
            )
        }
    }
}
