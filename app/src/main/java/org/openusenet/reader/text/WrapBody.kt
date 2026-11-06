/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.4.6 (§B12.1) — `wrapBody` E IL SUO LIMITE, FUORI DA `ComposeLogic.kt`.
 *
 * PERCHE' ESISTE QUESTO FILE, ED E' UN MOTIVO DI COLLAUDO, NON DI ORDINE.
 * `ComposeLogic.kt` nomina tipi Compose (`TextRange`, `TextFieldValue`) nelle
 * firme di ALTRE funzioni. Le funzioni di primo livello di un file Kotlin
 * finiscono tutte nella stessa classe — `ComposeLogicKt` — quindi una prova
 * JVM che voleva soltanto `wrapBody` doveva caricare una classe che nomina
 * tipi Android assenti sul banco di prova. Il commento di `WrapBodyTest` in
 * `CoreTests.kt` avvertiva da tempo: se quella classe non si carica non e' un
 * difetto dell'app, e' un difetto della prova. Ora non puo' piu' succedere.
 *
 * IL PACCHETTO NON CAMBIA, quindi nessun chiamante e nessun import si muove:
 * lo spostamento e' invisibile a tutto il resto del progetto.
 *
 * `BODY_MAX_COLS` viaggia con la funzione perche' e' il suo limite e nessun
 * altro lo usa — quando si estrae un componente si porta via il suo
 * contenitore.
 *
 * ⚠️ QUESTO FILE DEVE RESTARE DI KOTLIN PURO. Non ha import e non deve
 * averne di Compose o di Android: il giorno che ne compare uno, il motivo per
 * cui il file esiste sparisce senza che nessuno se ne accorga. Vale in
 * particolare per §B6 (Format=Flowed), che riscrivera' proprio qui dentro.
 */
package org.openusenet.reader.text

/**
 * Larghezza massima delle righe del corpo prima dell'invio.
 *
 * Non è solo buona educazione: molti server NNTP rifiutano l'articolo con
 * "441 Line too long" quando una riga supera il loro limite. Chi scrive un
 * paragrafo di getto senza andare a capo produce una riga unica lunghissima,
 * e il messaggio viene respinto (segnalazione di un tester).
 */
const val BODY_MAX_COLS = 72

/**
 * 1.6 — IL CORPO COME VA SULLA RETE, in un posto solo.
 *
 * ⚠️ Il difetto che chiude: la scelta fra [avvolgiFlowed] e [wrapBody] stava
 * dentro `ComposeScreen`, e `ComposePane` (vista a pannelli) spediva il testo
 * tale e quale: nessun a capo a 72 colonne (rischio «441 Line too long») e
 * interruttore f=f ignorato. `postArticle` non manda a capo: decide solo il
 * `Content-Type`, guardando il corpo che riceve.
 */
fun corpoDaInviare(testo: String, flowed: Boolean): String =
    if (flowed) avvolgiFlowed(testo) else wrapBody(testo)

/**
 * Righe che iniziano cosi' sono elenchi o enumerazioni: mai unirle alla
 * precedente. La regola dei capoversi sta in Riflusso.kt.
 */
val MARCA_ELENCO = Regex("""^([-*•·>]|\d{1,3}[.)])\s""")

/** Sotto questa larghezza non e' testo andato a capo: sono righe corte per scelta. */
const val LARGHEZZA_MIN_IMPAGINATA = 30

/**
 * Manda a capo il corpo del messaggio rispettando le convenzioni Usenet.
 *
 * Regole applicate, in ordine di importanza:
 *  - non spezza le parole a metà: va a capo negli spazi;
 *  - conserva il prefisso di citazione (">", "> >", ">>") sulle righe che
 *    continuano, altrimenti la citazione si sfalda e non si capisce più chi ha
 *    detto cosa;
 *  - non spezza una parola più lunga del limite (tipicamente un indirizzo web):
 *    la lascia sforare, perché un link tagliato in due non funziona più;
 *  - non tocca la firma dopo il delimitatore "-- ", già limitata per conto suo;
 *  - lascia intatte le righe che stanno già entro il limite, quindi chi va a
 *    capo a mano non vede cambiare nulla.
 */
fun wrapBody(text: String, cols: Int = BODY_MAX_COLS): String {
    val out = ArrayList<String>()
    var nellaFirma = false
    for (riga in text.lines()) {
        if (riga.trimEnd() == "--" || riga == "-- ") nellaFirma = true
        if (nellaFirma || riga.length <= cols) { out.add(riga); continue }

        // prefisso di citazione da ripetere sulle righe di continuazione
        val prefisso = Regex("^[ \t]*(>[ >\t]*)?").find(riga)?.value ?: ""
        val contenuto = riga.substring(prefisso.length)
        val larghezza = (cols - prefisso.length).coerceAtLeast(20)

        /*
         * 1.3.7 — NESSUNA RIGA PRODOTTA DAL WRAP FINISCE CON UNO SPAZIO.
         *
         * Domanda di Adam H. Kerman, 15 settembre 2026: «when you break a long
         * line at the whitespace character, does the now-broken line end with a
         * trailing space character that counts toward line length?».
         *
         * Non e' pignoleria: uno spazio in fondo a una riga e' il marcatore di
         * ROTTURA DEBOLE del Format=Flowed (RFC 3676). Un lettore che conosce
         * f=f riunirebbe quella riga con la successiva — disfacendo esattamente
         * l'a-capo che avevamo appena messo.
         *
         * MISURATO riproducendo questa funzione riga per riga
         * (scratchpad/proto-wrap/prova.py): nel caso normale lo spazio NON c'e'
         * e il separatore conta gia' nella lunghezza. Ma con un DOPPIO SPAZIO
         * nel punto di rottura — due spazi dopo il punto, abitudine da macchina
         * da scrivere tutt'altro che rara — `split(" ")` produce un elemento
         * VUOTO, quello si porta dietro il proprio separatore, e la riga esce
         * con lo spazio in coda:
         *
         *     |parola parola parola parola parola parola parola  doppio |
         *
         * Il rimedio NON e' scartare gli elementi vuoti: quello collasserebbe i
         * doppi spazi dentro la riga, cioe' riscriverebbe il testo dell'utente,
         * ed e' proprio cio' che questo progetto si vieta (non tagliamo gli
         * spazi a una password, non riflussiamo una firma). Si ripulisce solo
         * la CODA delle righe che spezziamo NOI: e' residuo nostro, non testo
         * suo.
         *
         * La riga "-- " non passa di qui: il ramo della firma la emette tale e
         * quale, e il suo spazio finale e' prescritto dalla convenzione.
         */
        var corrente = StringBuilder()
        for (parola in contenuto.split(" ")) {
            when {
                corrente.isEmpty() -> corrente.append(parola)
                corrente.length + 1 + parola.length <= larghezza -> corrente.append(' ').append(parola)
                else -> { out.add((prefisso + corrente).trimEnd()); corrente = StringBuilder(parola) }
            }
        }
        if (corrente.isNotEmpty()) out.add((prefisso + corrente).trimEnd())
    }
    return out.joinToString("\n")
}

/**
 * 1.6.1 (BIG Umberto) — le righe della firma, ognuna al massimo [cols] colonne.
 *
 * [wrapBody] non guarda la firma nel corpo, quindi le righe vanno spezzate
 * QUI, prima di entrare nel corpo. Stesse regole del corpo: alle parole, e una
 * parola piu' lunga del limite (un indirizzo web) resta intera.
 */
fun spezzaRigheFirma(righe: List<String>, cols: Int = BODY_MAX_COLS): List<String> =
    righe.flatMap { wrapBody(it, cols).lines() }

/**
 * 1.4.8 (par. B6b) — GENERARE Format=Flowed (RFC 3676).
 *
 * E' [wrapBody] con un'unica differenza di sostanza: **le righe che spezziamo
 * NOI finiscono con uno spazio**, e quello spazio e' il marcatore di rottura
 * debole. Chi legge con un lettore che conosce f=f ricompone il capoverso alla
 * larghezza del proprio schermo; chi legge con uno che non lo conosce vede
 * righe andate a capo a 72 colonne, come oggi.
 *
 * ⚠️ E' ESATTAMENTE IL CONTRARIO DI UNA DECISIONE DEL 1.3.7, e va detto qui o
 * sembrera' una svista. Alla domanda di Adam H. Kerman — «la riga spezzata
 * finisce con uno spazio?» — `wrapBody` risponde NO, e lo fa con un `trimEnd()`
 * aggiunto apposta: senza `format=flowed` dichiarato, quello spazio sarebbe un
 * marcatore che nessuno ha chiesto, e un lettore f=f disferebbe l'a-capo appena
 * messo. Qui il formato **viene dichiarato**, quindi lo stesso spazio passa da
 * effetto collaterale a informazione. *La stessa riga di codice e' un difetto o
 * la funzione, secondo cio' che il messaggio dichiara di se'.*
 *
 * LE REGOLE, verificate alla fonte e non ricordate — e sono il rovescio esatto
 * di quelle che [srotolaFlowed] applica in lettura:
 *
 * 1. **La firma non si tocca** (§4.3). Dal separatore `"-- "` in giu' le righe
 *    escono tali e quali, come fa `wrapBody`: la firma e' gia' limitata per
 *    conto suo (4 righe da 72) e la RFC dice che quella riga non e' ne' fissa
 *    ne' flowed. Se la spezzassimo, si incollerebbe all'ultimo capoverso di
 *    ogni messaggio che mandiamo.
 * 2. **I `>` si contano e si riemettono** su ogni riga di continuazione (§4.5),
 *    cosi' la citazione non si sfalda. Due righe di profondita' diversa non si
 *    uniscono mai, quindi la profondita' va ripetuta per forza.
 * 3. **Lo spazio di protezione** (§4.4) va davanti al contenuto che comincia
 *    con uno spazio, con `>` o con `From ` — altrimenti chi legge lo
 *    scambierebbe per indentazione mangiata, per un livello di citazione in
 *    piu', o per l'inizio di un messaggio in una mailbox.
 * 4. **L'ultima riga di un capoverso NON finisce con uno spazio**: e' «fissa»,
 *    ed e' lei a chiudere il capoverso.
 *
 * ⚠️ E UNA CONSEGUENZA CHE TOCCA IL TESTO DELL'UTENTE: le righe che NON
 * spezziamo vengono ripulite in coda. `wrapBody` le lascia intatte — *«chi va a
 * capo a mano non vede cambiare nulla»* — ma con il formato dichiarato uno
 * spazio finale battuto per sbaglio non e' piu' spazio: e' sintassi, e
 * unirebbe due righe che l'utente ha voluto separate. Si tocca solo la CODA, e
 * solo quando si dichiara f=f.
 *
 * Nessun `delsp`: per le lingue europee lo spazio di rottura e' anche il
 * separatore fra le parole, quindi va conservato (ed e' il caso che
 * [srotolaFlowed] tratta con `delSp = false`).
 */
/**
 * 1.4.8 (par. B6b): lo spazio di protezione va messo? (RFC 3676 §4.4 e §5)
 *
 * Un posto solo, perche' la risposta serve DUE volte in [avvolgiFlowed] — per
 * misurare se una riga ci sta senza essere spezzata, e per comporla — e le due
 * risposte non possono divergere senza produrre righe di 73 colonne o righe
 * spezzate per niente.
 */
private fun protezioneServe(prof: Int, contenuto: String): Boolean =
    prof > 0 ||                        // §4.4+§5: concesso, e tiene la resa di "> testo"
        contenuto.startsWith(" ") ||   // §4.4: obbligatorio (rientro voluto)
        contenuto.startsWith(">") ||   // §4.4: obbligatorio (riga non citata che comincia con >)
        contenuto.startsWith("From ")  // §4.4: obbligatorio (From-munging)

/**
 * 1.6 — DOVE FINISCONO I `>` DI TESTA, anche nella forma `> > testo`.
 *
 * `> > testo` e' cio' che NeReO stessa produce rispondendo a un messaggio gia'
 * citato. Per la RFC la profondita' conta i soli `>` attaccati
 * ([profonditaCitazione], che resta cosi' in LETTURA: e' la norma per i
 * messaggi altrui), ma qui i `>` li stiamo SCRIVENDO noi: contarne uno solo
 * lasciava l'altro nel contenuto, e la riga spezzata proseguiva a livello 1.
 * I `>` si riconoscono separati da UNO spazio e si riemettono attaccati, nella
 * forma canonica del §4.5.
 */
private fun fineTestaCitazione(riga: String): Int {
    var fine = 0
    var i = 0
    while (i < riga.length && riga[i] == '>') {
        fine = i + 1
        i = fine
        if (i + 1 < riga.length && riga[i] == ' ' && riga[i + 1] == '>') i++
    }
    return fine
}

fun avvolgiFlowed(text: String, cols: Int = BODY_MAX_COLS): String {
    val out = ArrayList<String>()
    var nellaFirma = false

    fun emetti(prof: Int, contenuto: String, continua: Boolean) {
        // §4.4 — lo spazio di protezione, nei due casi in cui la norma lo vuole
        // e in uno in cui lo concede.
        //
        // OBBLIGATORIO: *«On generation, any unquoted lines which start with
        // ">", and any lines which start with a space or "From " MUST be
        // space-stuffed»*. Vale su QUALUNQUE riga, non solo sulla prima di un
        // capoverso: una continuazione puo' cominciare con "From " se la
        // rottura cade proprio li'.
        //
        // CONCESSO, e lo usiamo su tutte le righe citate: *«Other lines MAY be
        // space-stuffed as desired»* (§4.4), col motivo scritto nel §5 —
        // *«the generating agent MAY space-stuff all lines, to maintain the
        // relative indentation of the lines when viewed by clients which are
        // not aware of Format=Flowed»*. Senza, le righe di continuazione di una
        // citazione escono come `>group being posted to`, attaccate al segno,
        // e chi legge con un programma che non ricompone vede quello. Per la
        // norma `>> Exit` e `>>Exit` sono identici (§4.5), quindi lo spazio
        // non costa nessuna informazione: costa una colonna e compra la resa
        // di sempre.
        val serve = protezioneServe(prof, contenuto)
        out.add(">".repeat(prof) + (if (serve) " $contenuto" else contenuto) + if (continua) " " else "")
    }

    for (riga in text.lines()) {
        if (riga.trimEnd() == "--" || riga == "-- ") nellaFirma = true
        if (nellaFirma) { out.add(riga); continue }

        val fineTesta = fineTestaCitazione(riga)
        val prof = riga.substring(0, fineTesta).count { it == '>' }
        var contenuto = riga.substring(fineTesta)
        // ⚠️ LO SPAZIO DOPO I `>` SI TOGLIE, QUELLO A INIZIO RIGA NO, e la
        // differenza non e' un capriccio.
        //
        // Dopo i `>` quello spazio e' la spaziatura convenzionale della
        // citazione, e il lettore la tratta come spazio di protezione: lo
        // toglie (§4.4). Se lo lasciassimo dentro il contenuto e poi
        // aggiungessimo il nostro, uscirebbe `>  testo` con due spazi, e la
        // citazione si allontanerebbe dal segno di una colonna per livello.
        //
        // A inizio di una riga NON citata, invece, quello spazio e' il RIENTRO
        // VOLUTO da chi scrive — un elenco, un blocco di codice — e va
        // conservato: lo protegge il ramo obbligatorio di `emetti`, che ne
        // aggiunge uno in piu' cosi' il lettore ne toglie uno e il rientro
        // resta. *La stessa riga di spazio e' convenzione in un posto e
        // contenuto nell'altro, e distinguerli e' tutto il mestiere qui.*
        if (prof > 0 && contenuto.startsWith(" ")) contenuto = contenuto.substring(1)
        // vedi la nota in testa: in f=f lo spazio in coda e' sintassi
        contenuto = contenuto.trimEnd()

        // ⚠️ IL CONTO DI «CI STA» SI FA SULLA RIGA CHE USCIREBBE, non su un
        // margine prudenziale, e la differenza l'ha trovata il messaggio di
        // prova di Marco.
        //
        // La prima stesura teneva da parte DUE colonne sempre — una per lo
        // spazio di protezione (§4.4) e una per lo spazio di rottura — e
        // confrontava il contenuto con `cols - prof - 2`. Conseguenza: una
        // riga di 71 o 72 colonne andata a capo A MANO veniva spezzata in
        // due, 66 + 6, e in un lettore che non ricompone si vedeva la
        // scaletta. Cioe' esattamente il difetto che f=f dovrebbe togliere,
        // prodotto da noi, e **su chiunque impagini a 72 colonne**, che su
        // Usenet e' la maggioranza.
        //
        // Una riga che NON verra' spezzata non ha nessuno spazio di rottura,
        // quindi il suo margine e' uno solo (la protezione, se serve) e si
        // sa in anticipo. Si misura dunque la riga vera: se ci sta, esce
        // tale e quale — e la promessa di `wrapBody` *«chi va a capo a mano
        // non vede cambiare nulla»* resta valida anche qui.
        //
        // Le righe vuote passano da questo ramo e restano vuote, cioe'
        // separatori di capoverso.
        val protezione = if (protezioneServe(prof, contenuto)) 1 else 0
        if (prof + protezione + contenuto.length <= cols) {
            emetti(prof, contenuto, continua = false); continue
        }

        // Da qui si spezza: ora le due colonne di margine servono entrambe —
        // la protezione e lo spazio di rottura in coda, che conta nella
        // lunghezza della riga come qualunque altro carattere (era la
        // sostanza della domanda di Kerman). Una riga troppo lunga costa un
        // "441 Line too long" e il messaggio respinto dal server.
        val larghezza = (cols - prof - 2).coerceAtLeast(20)

        // ⚠️ IL RIENTRO SI METTE DA PARTE PRIMA DI SPEZZARE, e questo difetto
        // l'ha trovato Marco guardando il messaggio di prova in MesNews: una
        // riga rientrata e LUNGA arrivava a filo, mentre la stessa riga corta
        // arrivava rientrata.
        //
        // `split(" ")` su un contenuto che comincia con spazi produce elementi
        // VUOTI in testa, e il ramo `corrente.isEmpty()` li riassorbe uno dopo
        // l'altro senza mai scrivere niente: il rientro spariva. `wrapBody` non
        // ha il problema perche' la sua regex di prefisso si porta via anche
        // gli spazi iniziali prima dello `split` — qui no, perche' in f=f quel
        // rientro e' CONTENUTO e deve restare dentro il capoverso.
        //
        // Il rientro va sulla PRIMA riga e non si ripete: le righe successive
        // sono la continuazione dello stesso capoverso, e ripeterlo vorrebbe
        // dire infilare spazi in mezzo al testo quando chi legge lo ricompone.
        val rientro = contenuto.takeWhile { it == ' ' }
        val testo = contenuto.substring(rientro.length)

        // ⚠️ «PRIMA PAROLA DELLA RIGA» E' UN FLAG, NON `corrente.isEmpty()`, e
        // anche questo l'ha scoperto il messaggio di prova.
        //
        // Su un doppio spazio `split(" ")` produce un elemento VUOTO. Con
        // `isEmpty()` come condizione, un elemento vuoto che cade subito dopo
        // una rottura di riga lascia `corrente` ancora vuota, e la parola
        // successiva entra dal ramo «prima parola» senza il suo spazio: il
        // doppio spazio dell'utente diventa singolo. `wrapBody` non ne
        // soffre perche' li' ogni riga finisce con un `trimEnd()` e le righe
        // sono indipendenti — uno spazio perso sul punto di rottura non si
        // vede. Qui invece chi legge ricompone, e la perdita resta nel testo.
        //
        // *Due difetti nati dalla stessa riga: «la riga e' vuota» non vuol
        // dire «siamo all'inizio della riga».*
        var primaRigaDelCapoverso = true
        var primaParolaDellaRiga = true
        var corrente = StringBuilder()
        fun chiudiRiga(continua: Boolean) {
            emetti(prof, (if (primaRigaDelCapoverso) rientro else "") + corrente.toString(), continua)
            primaRigaDelCapoverso = false
            primaParolaDellaRiga = true
            corrente = StringBuilder()
        }
        // ⚠️ LA PRIMA PAROLA DI UNA RIGA SI POSA DA UN POSTO SOLO.
        //
        // Nella 146 il ramo `else` chiamava `chiudiRiga` — che rimette il flag
        // a `true` — e poi faceva `corrente.append(parola)` SENZA rimetterlo a
        // `false`. La parola successiva rientrava percio' dal ramo «prima
        // parola», che appende senza spazio: due parole incollate a ogni
        // rottura di riga. *Un flag che due rami devono ricordarsi di spegnere
        // viene dimenticato da uno dei due.*
        fun posaPrimaParola(parola: String) {
            corrente.append(parola)
            primaParolaDellaRiga = false
        }
        for (parola in testo.split(" ")) {
            val disponibile =
                (larghezza - if (primaRigaDelCapoverso) rientro.length else 0).coerceAtLeast(20)
            when {
                primaParolaDellaRiga -> posaPrimaParola(parola)
                corrente.length + 1 + parola.length <= disponibile -> corrente.append(' ').append(parola)
                // ⚠️ QUI NON SI FA `trimEnd()`, al contrario di `wrapBody`. Se
                // la rottura cade su un doppio spazio, `split(" ")` ha prodotto
                // un elemento vuoto e `corrente` finisce con uno spazio: quello
                // e' testo dell'utente, e lo spazio che aggiungiamo noi gli si
                // affianca come marcatore. In lettura il marcatore viene
                // consumato e il doppio spazio torna intatto — provato nel
                // giro completo di `CoreTests`.
                else -> { chiudiRiga(continua = true); posaPrimaParola(parola) }
            }
        }
        if (corrente.isNotEmpty()) chiudiRiga(continua = false)
    }
    return out.joinToString("\n")
}
