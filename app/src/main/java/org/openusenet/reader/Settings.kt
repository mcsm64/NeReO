/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Impostazioni di aspetto, osservabili da Compose: quando cambiano,
 * il tema si aggiorna immediatamente in tutta l'app.
 */
package org.openusenet.reader

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.openusenet.reader.nntp.NntpLog

object AppSettings {
    private lateinit var prefs: SharedPreferences

    /** "system" | "light" | "dark" */
    var themeMode by mutableStateOf("system")

    /** Material You (colori dal wallpaper, Android 12+) */
    var dynamicColor by mutableStateOf(true)

    /** chiave di ACCENT_COLORS */
    var accent by mutableStateOf("indigo")

    /** Corpo articolo in monospazio (stile lettore classico / MesNews). */
    var bodyMonospace by mutableStateOf(true)

    /**
     * Scala del testo APPLICATA A TUTTA L'INTERFACCIA (tema):
     * 0.8 / 0.9 / 1.15 / 1.3 / 1.5 / 1.7, corrispondenti a
     * Piccolissimo / Molto piccolo / Piccolo / Normale / Grande / Molto grande.
     */
    var bodyFontScale by mutableStateOf(1.3f)

    /** Etichette delle dimensioni testo, dalla più piccola. */
    val fontScales: LinkedHashMap<String, Float> = linkedMapOf(
        "Piccolissimo" to 0.8f,
        "Molto piccolo" to 0.9f,
        "Piccolo" to 1.15f,
        "Normale" to 1.3f,
        "Grande" to 1.5f,
        "Molto grande" to 1.7f,
    )

    /** Housekeeping: elimina i letti più vecchi di N giorni dopo ogni sync. 0 = disattivato. */
    var autoPurgeDays by mutableStateOf(0)

    /** Vista thread predefinita: "tree" (albero) | "sequential" (per data) | "roots" (solo discussioni). */
    var threadView by mutableStateOf("tree")

    /** Sfondo di lettura del corpo: chiave di readingBackgrounds. */
    var readingBg by mutableStateOf("default")

    /** Tinta di sfondo dell'INTERA interfaccia: chiave di uiPalettes. */
    var uiPalette by mutableStateOf("default")

    /**
     * Fattore di riduzione del SOLO pannello centrale (elenco discussioni) nel
     * layout a pannelli. 1.0 = come il resto dell'app.
     *
     * E' separato da bodyFontScale di proposito: su uno schermo grande e
     * lontano — TV box, monitor — servono tante intestazioni a colpo d'occhio
     * ma un corpo del messaggio che resti leggibile da tre metri. Un unico
     * moltiplicatore globale non puo' soddisfare due esigenze opposte.
     */
    var paneListScale by mutableStateOf(1.0f)

    /** Fusione linee intelligente: collassa le righe vuote consecutive nel corpo. */
    var collapseBlankLines by mutableStateOf(false)

    /**
     * 1.5.7 / 175 (F8): nella lettura a schermo intero, volume giu' = messaggio
     * successivo, volume su = precedente. Spento di default. Vedi ui/TastiVolume.kt.
     */
    var tastiVolume by mutableStateOf(false)

    /**
     * 1.5.7 / 175: dissolvenza tra le schermate (quella predefinita di
     * Navigation Compose, 700 ms). Acceso di default: spento, le schermate
     * cambiano di colpo. Vedi il NavHost in MainActivity.
     */
    var animazioni by mutableStateOf(true)

    /**
     * Riflusso testo (de-wrap): ricompone i capoversi spezzati dall'a-capo
     * forzato a ~72 caratteri, così su schermo stretto non restano righe con
     * una-due parole orfane. Solo in LETTURA; le righe corte intenzionali
     * (elenchi, versi) restano. Spento di default. (Richiesta Bingo3331.)
     */
    var reflowText by mutableStateOf(false)

    /**
     * 1.4.8 (par. B6b) — IMPAGINARE IN Format=Flowed CIO' CHE SI SPEDISCE.
     *
     * Acceso, i capoversi lunghi escono con la rottura debole della RFC 3676
     * (uno spazio in fondo alle righe che spezziamo noi) e il messaggio
     * dichiara `format=flowed`: chi legge con un lettore moderno vede il
     * capoverso ricomposto alla larghezza del proprio schermo invece delle
     * righe a 72 colonne.
     *
     * ⚠️ SPENTO DI DEFAULT, e il motivo non e' prudenza generica: questa e'
     * l'unica impostazione di NeReO che cambia **i byte che finiscono su
     * Usenet**, e un articolo spedito non si ritira. Tutto il resto si
     * corregge sul proprio telefono; questo si corregge sugli schermi degli
     * altri. Il principio 1 del quaderno vuole il metro prima della modifica
     * rischiosa: il metro e' la meta' che LEGGE f=f, dentro dalla 1.4.6, e
     * questo interruttore serve a provare la meta' che scrive su messaggi veri
     * prima di accenderla a tutti.
     *
     * Resta un'impostazione anche dopo: chi pubblica in gruppi letti con
     * programmi d'epoca puo' preferire l'a-capo rigido, e in un lettore che
     * vuole somigliare a MesNews «come impagino cio' che spedisco» e' una
     * scelta legittima da lasciare a chi scrive.
     */
    var formatFlowedInUscita by mutableStateOf(false)

    /**
     * Vista a pannelli: disattiva lo swipe "tira giù per aggiornare" nella
     * colonna albero, lasciando solo l'icona Aggiorna (già sempre presente,
     * beta16). Utile in landscape, dove il pannello è basso e il gesto resta
     * armato competendo coi tocchi / lasciando il cerchietto appeso.
     *
     * 1.2.3 (Marco/Roxy): default invertito a TRUE nella sola vista a pannelli.
     * Il pull-to-refresh è nato per liste a tutto schermo, non per una colonna
     * alta pochi cm: nel pannello ruba il primo scroll verso l'alto e lascia il
     * cerchietto appeso (terzo tester a inciamparci). Ora nei pannelli si usa di
     * default l'icona; chi preferisce il gesto lo riattiva togliendo la spunta.
     * Il telefono (lista a tutto schermo) NON è toccato da questa opzione: lì il
     * gesto resta com'è.
     */
    var soloIconaAggiornaPannelli by mutableStateOf(true)

    /** Accorcia il testo citato: i blocchi quote lunghi si comprimono (tocca per espandere). */
    var shortenQuotes by mutableStateOf(false)

    /**
     * 1.2.6 (Dave Royal, il "Q" di PAN): muta TUTTE le citazioni sostituendo ogni
     * blocco citato con un segnaposto ("> testo citato nascosto"). Toggle rapido da
     * un'icona in barra, così scorrendo un thread leggi solo le risposte; toccando un
     * blocco lo si riespande singolarmente. Più deciso di [shortenQuotes] (che accorcia
     * solo i lunghi). Default off; solo in lettura.
     */
    var mutaCitazioni by mutableStateOf(false)

    /**
     * 1.2.7 (Dave #8, opzione accolta da Marco): default dei gruppi quando si
     * risponde a un crosspost. false = solo il gruppo corrente (comportamento
     * storico, "niente crosspost per distrazione"); true = pre-seleziona tutti i
     * gruppi dell'originale fino al cap. In entrambi i casi il dialog resta e
     * l'utente può aggiustare. Il default imposto NON cambia: è una scelta di chi
     * (come Dave) preferisce rispondere sempre a tutti.
     */
    var rispostaCrosspostTuttiIGruppi by mutableStateOf(false)

    /**
     * 1.2.5 (Bingo3331): controllo automatico di nuove versioni su SourceForge.
     * È una sola GET a un URL pubblico, senza dati utente; l'app avvisa soltanto,
     * non scarica né installa nulla. Vedi UpdateChecker.
     *
     * 1.2.8 (Bingo3331 + HAL): da interruttore booleano a modalità —
     * "never" | "daily" | "4h" | "always". Default "daily" (comportamento
     * storico), ma la finestra ora è di 24h REALI dall'ultimo controllo
     * riuscito, non più "stesso giorno di calendario": quel confronto per
     * data solare poteva bruciare lo slot di giornata prima ancora che una
     * release fosse pubblicata, lasciando l'utente "bloccato" fino al giorno
     * dopo anche riaprendo l'app cento volte — il bug segnalato da Bingo3331
     * il 1 settembre 2026. Vedi `AGGIORNAMENTI_INTERVALLI` per i valori.
     *
     * 1.5.4: per "daily" vale ANCHE il giorno di calendario nuovo, come seconda
     * via per controllare (mai come ragione per non farlo): la prima apertura
     * di ogni giorno controlla sempre. Vedi update/FinestraControllo.kt.
     */
    var autoCheckMode by mutableStateOf("daily")

    /** 1.2.5: epoch ms dell'ultimo controllo aggiornamenti RIUSCITO (throttle). */
    var lastUpdateCheckMs by mutableStateOf(0L)

    /**
     * 1.2.8: esito dell'ultimo controllo riuscito, per il testo "Ultimo
     * controllo: …" sotto il selettore. "" = nessun aggiornamento trovato,
     * altrimenti la versione trovata (es. "1.2.8").
     */
    var lastUpdateCheckFound by mutableStateOf("")

    /**
     * 1.2.6 (sicurezza, roadmap NNTP): consente l'autenticazione su connessione NON
     * cifrata (AUTHINFO PASS in chiaro). Default FALSE: la password non viaggia mai in
     * chiaro senza un'esplicita scelta dell'utente (RFC 4643). Override consapevole per
     * server legacy solo-testo. Vedi NntpClient.authenticate.
     */
    var consentiAuthNonCifrata by mutableStateOf(false)

    /**
     * 1.2.17 (voce B) — invia la password SOLO quando il server la chiede.
     *
     * Comportamento storico (spento): NeReO si autentica appena collegato, ad
     * ogni connessione, se il server ha un nome utente. Acceso: aspetta che sia
     * il server a rispondere "480 authentication required" (RFC 4643 §2), si
     * autentica in quel momento e ripete il comando.
     *
     * A cosa serve: su un server che NON riesce a cifrare la connessione la
     * password viaggia in chiaro ogni volta. INN lascia leggere senza
     * autenticazione e chiede le credenziali solo per pubblicare, quindi con
     * questa opzione la password passa dalla rete qualche volta al mese invece
     * che decine di volte al giorno.
     *
     * Default SPENTO di proposito: tocca il modo in cui ci si collega a OGNI
     * server, e un provider che pretenda l'autenticazione senza dirlo con un
     * 480 smetterebbe di funzionare. Chi la accende e vede un server rompersi
     * la rispegne e torna esattamente a prima — nessun dato viene perso.
     */
    var autenticazionePigra by mutableStateOf(false)

    /**
     * 1.2.14 (audit di sicurezza §1.1): consente di collegarsi anche quando il
     * certificato del server NON risulta valido per l'host richiesto.
     *
     * Default FALSE, cioè la verifica è attiva. Fino alla 1.2.13 non esisteva
     * alcuna verifica del nome host: bastava un certificato valido per un
     * dominio qualsiasi — cosa alla portata di chiunque — perché un
     * intercettatore su una rete non fidata leggesse tutto il traffico e si
     * prendesse le credenziali del provider, senza che l'utente vedesse nulla.
     *
     * L'override esiste perché capita che un provider abbia il certificato
     * configurato male (nome non combaciante, host raggiunto tramite un alias),
     * e senza via d'uscita l'utente resterebbe fuori dal suo server in attesa
     * di una nuova versione dell'app. È GLOBALE, non per-server, per la stessa
     * ragione per cui lo era [consentiAuthNonCifrata] nella 1.2.6: il
     * per-server richiede una colonna in più e una migrazione del database, e
     * si fa quando si dimostra che serve davvero (per l'auth è successo nella
     * 1.2.9). Acceso, la connessione resta CIFRATA ma non AUTENTICATA.
     */
    var consentiCertificatoNonVerificabile by mutableStateOf(false)

    /**
     * beta19 (Roxy): soglia in righe oltre la quale un blocco citato viene
     * accorciato (se [shortenQuotes] è attivo). Prima era fissa a 6, per questo
     * la funzione "sembrava non fare nulla": su Usenet molte citazioni sono di
     * 3-6 righe e non superavano mai la soglia. Configurabile perché la soglia
     * utile dipende da come si scrive nei gruppi che si frequentano.
     * Il blocco accorciato mostra sempre 2 righe in meno della soglia (min 2).
     */
    var sogliaAccorciaCitazioni by mutableStateOf(6)

    /**
     * beta14 (Dave Royal): stile di visualizzazione delle citazioni.
     *  - "bars"  : solo barre verticali colorate (i ">" del testo vengono tolti)
     *  - "chevron": solo i ">" originali stile Usenet classico (niente barre)
     *  - "both"  : entrambi (comportamento storico)
     * Default "bars": più pulito e moderno, ma l'utente sceglie.
     */
    var quoteStyle by mutableStateOf("bars")

    /**
     * 1.2.9 (Marco): oltre una certa profondità di citazione, i ">" ripetuti
     * (stile "chevron"/"both") diventano illeggibili — soprattutto su
     * telefono in verticale, dove lo spazio orizzontale è già poco ("si
     * perde il filo"). Oltre [sogliaCitazioniANumeri], il prefisso mostra il
     * NUMERO del livello invece dei ">" ripetuti (es. oltre 8: "9", "10",
     * "11"…), come fa chi scrive a mano citazioni molto annidate su Usenet.
     * Solo resa VISIVA in lettura: il testo che arriva e quello che parte
     * restano con i ">" standard — NeReO stesso e ogni altro client
     * continuano a leggere correttamente la profondità (calcolata sempre
     * contando i ">" originali, mai i numeri). Nessun effetto in stile
     * "bars" (i ">" non si vedono comunque). Default ON: sotto soglia il
     * rendering è identico a prima, quindi non toglie nulla a chi non ne ha
     * bisogno.
     */
    var numericQuoteDepth by mutableStateOf(true)

    /** Soglia di profondità oltre la quale scattano i numeri (vedi [numericQuoteDepth]). */
    var sogliaCitazioniANumeri by mutableStateOf(8)

    /**
     * 1.4.9 (§J1, Jeff) — le ROTAIE verticali del rientro negli elenchi.
     *
     * Sono le righe grigie che segnano un livello di profondità ciascuna negli
     * elenchi di discussioni (telefono, pannelli e albero della discussione):
     * dalla 1.4.7 §A15 le disegna [disegnaRotaieAlbero] in un posto solo.
     *
     * Non sono le barre delle citazioni, e non c'entrano con [quoteStyle] — ma
     * si somigliano abbastanza perché Jeff, che aveva scelto «solo i >», le
     * abbia prese per quelle e abbia segnalato una regressione che non c'era.
     * L'opzione nasce da lì: due ornamenti diversi che si somigliano vogliono
     * due interruttori, non uno.
     *
     * ACCESO di default: le rotaie le ha chieste Dave Royal, e spegnerle per
     * tutti per rispondere a chi le vuole via sarebbe il rimedio peggiore del
     * male. Chi non le vuole le toglie in un tocco, in Aspetto, dove l'opzione
     * sta subito sotto lo stile delle citazioni — cioè dove Jeff ha cercato.
     *
     * Non tocca il RIENTRO: le righe restano rientrate secondo la profondità,
     * spariscono solo le linee verticali.
     */
    var rotaieAlbero by mutableStateOf(true)

    /**
     * 1.4.9 (§J3-C1) — la cartella di «Conserva questo», come URI di albero SAF
     * (`content://…/tree/…`), oppure vuota se non è ancora stata scelta.
     *
     * PERCHÉ UNA CARTELLA RICORDATA E NON IL SELETTORE DI SISTEMA. Il salvataggio
     * .eml della 1.4.5 chiede la destinazione A OGNI SALVATAGGIO: va bene per il
     * gesto occasionale («questo lo giro al mio client di posta»), non per
     * mettere da parte un messaggio dopo l'altro. Qui la cartella si sceglie una
     * volta e poi «Conserva» è un tocco.
     *
     * ⚠️ IL PERMESSO SU QUESTO URI SI PUÒ PERDERE: l'utente sposta o cancella la
     * cartella, o un ripristino non riporta i permessi persistenti. Quando
     * succede, la scrittura deve fallire AD ALTA VOCE e questo campo si svuota,
     * così il tocco successivo richiede la cartella invece di fallire di nuovo
     * in silenzio. Un salvataggio che non avviene senza dirlo è peggio di un
     * errore: vedi ui/Conserva.kt.
     */
    var cartellaConserva by mutableStateOf("")

    /** Segna tutto come letto uscendo dal gruppo. */
    var markReadOnExit by mutableStateOf(false)

    /**
     * beta18 (richiesta di un tester via Marco): mostra "Albero della
     * conversazione" ANCHE nella barra in basso di ArticleScreen, oltre che in
     * alto. Utile a chi trova scomodo raggiungere la barra alta (dita corte,
     * mancini, controller). Default off: chi non ne ha bisogno tiene la barra
     * bassa snella.
     */
    var alberoInBasso by mutableStateOf(false)

    /**
     * 1.3 (B — barre personalizzabili, PILOTA sulla barra bassa di ArticleScreen,
     * vedi NeReO_feature_barre.md): [articleBarOrder] è l'ordine COMPLETO di
     * tutte le azioni note della barra (comprese quelle nascoste, che così
     * mantengono una posizione stabile quando si riattivano); [articleBarHidden]
     * è l'insieme delle nascoste. La barra viva (ArticleScreen) filtra l'ordine
     * escludendo le nascoste. L'editor è [CustomizeBarsScreen]; il riordino usa
     * frecce su/giù (non drag): stesso approccio già validato per il riordino
     * gruppi in tablet mode, funziona anche col telecomando dei TV box — un
     * drag in stile "barra viva" non ci riesce (vedi NeReO_feature_barre.md).
     * Migrazione: se non è mai stato salvato un ordine, [alberoInBasso] decide
     * il seme (nascosto se era off); dopo la prima apertura dell'editor questo
     * insieme diventa la fonte autorevole e [alberoInBasso] resta un residuo
     * innocuo (letto solo qui, non più scritto altrove dalla 1.3).
     */
    val articleBarActionIds = listOf(
        "prev_msg", "next_msg", "next_unread", "prev_thread", "next_thread",
        "back_index", "top", "bottom", "mute_quotes", "tree_bottom",
        // 1.3.3 (Roxy): copia l'intero messaggio negli appunti, un clic.
        "copia",
        // 1.3.6 (Adam H. Kerman): consegna l'articolo GREZZO — intestazioni
        // comprese — a un'altra app. Nasce da "NeReO non sa mandare posta":
        // cosi' chi vuole rispondere in privato passa la palla al suo client di
        // posta invece di restare senza strade. Vedi ui/EsportaArticolo.kt.
        "esporta",
        // 1.4.5 (Adam H. Kerman, par. B9): l'altra meta' della sua richiesta
        // del 13 settembre. La 1.3.6 aveva dato la CONDIVISIONE (testo a
        // un'altra app); questa da' il FILE, che un client di posta apre e
        // riconosce come messaggio. Vedi ui/EsportaEml.kt, che riscrive
        // Content-Type e Content-Transfer-Encoding perche' il file non
        // dichiari un charset diverso da quello che contiene.
        "eml",
        // 1.4.9 (§J3-C1): «conserva questo». Sorella di "eml" e non suo
        // doppione: "eml" chiede dove ogni volta, questa scrive nella cartella
        // scelta una volta sola. Vedi ui/Conserva.kt.
        "conserva",
        // 1.3.6 (ObiWan): "link" copia l'URL permanente dell'articolo
        // (https://usenet.id/<message-id>), "msgid" il Message-ID nella forma
        // canonica. Vedi ui/LinkArticolo.kt. Chi ha gia' un ordine salvato se
        // le ritrova IN CODA alla barra: il caricamento riaccoda sempre gli id
        // nuovi (vedi il blocco "article_bar_order" in load()), quindi nessuna
        // azione resta invisibile dopo un aggiornamento.
        "link",
        "msgid",
        // 1.4: "ricarica" butta la copia locale e riscarica il corpo dal
        // server. Serve alla correzione del charset (il testo DECODIFICATO e'
        // quello che salviamo: senza un ri-scarico, gli articoli gia' in
        // archivio resterebbero rotti per sempre).
        "ricarica",
        // 1.4.6 (par. B13, BIG Umberto): «la ricerca ti apre il post, non ti fa
        // vedere dove e'». Porta all'elenco del gruppo sul messaggio aperto.
        // Non c'e' in paneBarActionIds: nei pannelli l'elenco e' gia' accanto.
        "mostra_in_elenco",
    )
    var articleBarOrder by mutableStateOf(articleBarActionIds)
    var articleBarHidden by mutableStateOf(emptySet<String>())

    /** Sposta l'azione [id] di [delta] posizioni nell'ordine (clampato ai bordi). */
    fun moveArticleBarAction(id: String, delta: Int) {
        val idx = articleBarOrder.indexOf(id)
        if (idx < 0) return
        val newIdx = (idx + delta).coerceIn(0, articleBarOrder.size - 1)
        if (newIdx == idx) return
        val mut = articleBarOrder.toMutableList()
        mut.removeAt(idx)
        mut.add(newIdx, id)
        articleBarOrder = mut
        save()
    }

    /** Alterna mostra/nascondi per l'azione [id] della barra di lettura. */
    fun toggleArticleBarActionHidden(id: String) {
        articleBarHidden = if (id in articleBarHidden) articleBarHidden - id else articleBarHidden + id
        save()
    }

    /** Ripristina l'ordine e la visibilità predefiniti della barra di lettura. */
    fun resetArticleBar() {
        articleBarOrder = articleBarActionIds
        articleBarHidden = emptySet()
        save()
    }

    /**
     * 1.3.1 (Dave Royal + Roxy) — BARRA DEL RIQUADRO DI LETTURA NEI PANNELLI,
     * personalizzabile come quella del telefono.
     *
     * Da dove nasce: nel layout a tre pannelli su tablet in VERTICALE il
     * riquadro di lettura è strettissimo, e cinque pulsanti da 48dp (il minimo
     * Material) si prendono ~240dp — all'oggetto del messaggio resta quel che
     * avanza, e va a capo cinque o sei volte. Dave Royal: «put ROT13 and
     * headers icons inside the 3-dots - but keep quotes outside». Roxy aveva
     * chiesto la stessa cosa in precedenza. Due richieste indipendenti sullo
     * stesso punto: la risposta giusta non è spostare due icone al posto loro,
     * è lasciar scegliere.
     *
     * DIFFERENZA IMPORTANTE rispetto alla barra del telefono: lì "nascosto"
     * vuol dire SPARITO. Qui no: ciò che si toglie dalla barra finisce nel menu
     * ⋮, che c'è sempre. Nessuna funzione diventa irraggiungibile — e non è un
     * dettaglio, visto che su un Box TV guidato da un puntatore la pressione
     * prolungata è impraticabile (vedi 1.3.1, prima parte).
     */
    /**
     * 1.3.3 (Roxy) — perche' esiste l'azione "copia".
     *
     * Il video del 10 settembre chiude la questione: sul suo Box TV la
     * SELEZIONE del testo FUNZIONA (il blocco si evidenzia e si estende col
     * trascinamento), ma la barra flottante Copia/Cerca/Condividi non compare
     * MAI — nemmeno per un fotogramma, in 22 secondi di registrazione, con la
     * selezione ferma al centro dello schermo. Non e' un problema di
     * posizionamento del popup: `showMenu` non viene proprio chiamata.
     *
     * Firma dell'indizio: mancano anche le MANIGLIE di selezione, che Compose
     * disegna sul percorso a tocco. E' il percorso MOUSE, che non alza la
     * barra flottante — e su Android il tasto destro e' BACK, quindi niente
     * menu contestuale nemmeno da li'. Roxy ha una selezione con cui non puo'
     * fare nulla. (ABDUZIONE sul meccanismo, coerente con tutto il video e con
     * le maniglie assenti; MISURATO che showMenu non viene mai chiamata.)
     *
     * Rimedio: un'azione che non passa DAL TUTTO per la selezione. Un clic,
     * niente pressione prolungata, niente trascinamento — funziona identica a
     * tocco e a puntatore. Copia il messaggio INTERO: per copiarne un pezzo
     * serve ancora selezionare, ed e' un limite dichiarato.
     */
    val paneBarActionIds = listOf(
        "rot13", "mute_quotes", "headers", "reply", "copia", "esporta", "eml", "conserva",
        "link", "msgid",
        /*
         * 1.4.6 (§B4, fetta 3) — LE OTTO CHE MANCAVANO AI PANNELLI.
         *
         * Il censimento del 22 settembre le aveva contate: sedici azioni nella
         * barra della vista a pagine, NOVE in questa. Le sette di differenza non
         * erano una scelta di prodotto — nei pannelli per arrivare al messaggio
         * dopo bisognava scorrere l'elenco e toccare. Col telecomando di un box
         * TV quello scorrimento e' la parte difficile della giornata, ed e' la
         * ragione per cui questa voce esisteva.
         *
         * Non ci sono `back_index` (nei pannelli l'elenco e' gia' a schermo,
         * accanto) e `tree_bottom` (l'albero e' l'altro riquadro): sono le due
         * che nella vista a pannelli non vogliono dire niente. Otto su dieci, e
         * le due mancanti per una ragione scritta.
         */
        "prev_msg", "next_msg", "next_unread", "prev_thread", "next_thread",
        "top", "bottom", "ricarica",
    )
    var paneBarOrder by mutableStateOf(paneBarActionIds)

    /**
     * La disposizione iniziale: quali azioni nascono nel menu ⋮ invece che in
     * barra.
     *
     * ⚠️ DEVE STARE SOPRA [paneBarInMenu]. In un `object` Kotlin gli
     * inizializzatori girano NELL'ORDINE IN CUI SONO SCRITTI: dichiararlo piu'
     * in basso e leggerlo piu' in alto darebbe un riferimento non ancora
     * inizializzato, cioe' un guasto all'avvio e non un errore di compilazione.
     *
     * 1.4.6 (§B4, fetta 3): le otto azioni nuove nascono NEL MENU, applicando la
     * regola gia' scritta nel 1.3.6 per le azioni future. Vale per le
     * installazioni nuove esattamente come per gli aggiornamenti (vedi `+ nuoveP`
     * in load()): prima i due rami potevano divergere, perche' l'uno riaccodava
     * al menu e l'altro ripartiva da un insieme fisso che nessuno aggiornava.
     */
    val paneBarInMenuIniziale = setOf(
        "rot13", "headers", "link", "msgid",
        "prev_msg", "next_msg", "next_unread", "prev_thread", "next_thread",
        "top", "bottom", "ricarica",
        // 1.4.9 (§J3-C1): nasce nel menu, applicando la regola scritta nel
        // 1.3.6 per le azioni future e seguita dalle otto del §B4. Nel riquadro
        // di lettura dei pannelli lo spazio è la risorsa scarsa, e conservare
        // un messaggio non è un gesto che si fa ogni due righe. Resta a un
        // tocco di distanza: nei pannelli «nel menu» non vuol dire sparito.
        "conserva",
    )

    /**
     * Azioni spostate nel menu ⋮ invece che nella barra. Il default NON è
     * l'insieme vuoto: nasce già con ROT13 e Intestazioni nel menu, cioè
     * esattamente la disposizione chiesta da Dave. È un cambio di aspetto
     * deliberato per tutti, non solo per chi lo configura: il riquadro stretto
     * in verticale è un problema reale a prescindere da chi lo segnala, e chi
     * preferisce le icone di prima le rimette in barra in due tocchi.
     */
    // 1.4.2 (build 113): questo valore iniziale era "rot13, headers" e non
    // corrispondeva a niente — init() lo sovrascrive a OGNI avvio, in entrambi
    // i rami, e il ramo "nessuna preferenza salvata" mette gli stessi quattro
    // id di resetPaneBar(). Era un inizializzatore morto che faceva sembrare
    // incoerente il "ripristina predefiniti", che invece e' corretto. Allineato
    // al valore vero, cosi' chi lo legge non deve andare a controllare.
    //
    // 1.4.6 (§B4, fetta 3): questo insieme era scritto in TRE punti — qui, in
    // resetPaneBar() e nel ramo "nessuna preferenza salvata" di load(). Tre
    // copie di un valore che deve essere lo stesso, e la terza era gia' quella
    // che rendeva possibile l'incoerenza descritta qui sopra. Adesso e' uno.
    var paneBarInMenu by mutableStateOf(paneBarInMenuIniziale)

    /** Sposta l'azione [id] di [delta] posizioni nella barra dei pannelli. */
    fun movePaneBarAction(id: String, delta: Int) {
        val idx = paneBarOrder.indexOf(id)
        if (idx < 0) return
        val newIdx = (idx + delta).coerceIn(0, paneBarOrder.size - 1)
        if (newIdx == idx) return
        val mut = paneBarOrder.toMutableList()
        mut.removeAt(idx)
        mut.add(newIdx, id)
        paneBarOrder = mut
        save()
    }

    /** Alterna "in barra" / "nel menu ⋮" per l'azione [id]. */
    fun togglePaneBarInMenu(id: String) {
        paneBarInMenu = if (id in paneBarInMenu) paneBarInMenu - id else paneBarInMenu + id
        save()
    }

    /** Ripristina la disposizione predefinita della barra dei pannelli. */
    fun resetPaneBar() {
        paneBarOrder = paneBarActionIds
        paneBarInMenu = paneBarInMenuIniziale
        save()
    }

    /**
     * beta18 (Roxy): vista "continua" del thread nel pannello di lettura —
     * tutti i post della discussione impilati uno sotto l'altro (stile
     * NewsGroupMod/newsreader da PC), invece di uno per volta. Su TV Box
     * landscape è accessibilità: col controller non si può fare swipe verso
     * l'alto per cambiare post. Opzionale, off di default.
     */
    var vistaContinua by mutableStateOf(false)

    /** Massimo intestazioni per sincronizzazione. 0 = nessun limite (scarica tutto). */
    var maxHeadersSync by mutableStateOf(1000)

    /**
     * Wishlist beta11 (richiesto da Roxy, caso TV Box dove "scarica tutti i
     * corpi" manuale è l'unico metodo praticabile). Se attivo, dopo il sync
     * automatico all'apertura di un gruppo scarica anche i corpi mancanti,
     * fino al tetto [maxCorpiPerApertura]. Disattivato di default: può
     * essere pesante su gruppi grandi o connessioni lente — vedi Piano C
     * della wishlist.
     */
    var scaricaCorpiAutomatico by mutableStateOf(false)

    /**
     * beta14 (Bingo3331): all'apertura di NeReO scarica automaticamente i nuovi
     * messaggi di tutti i gruppi sottoscritti (equivale allo swipe-refresh, ma
     * automatico). Default OFF di proposito: un'app che si connette da sola
     * all'avvio è una sorpresa per chi ha dati a consumo o connessione lenta
     * (il caso che Bingo stesso cita). Chi la vuole la attiva in Impostazioni.
     */
    var syncOnStart by mutableStateOf(false)

    /**
     * Flag di SESSIONE (non persistente): true dopo che il sync automatico
     * all'avvio è stato eseguito una volta in questo avvio dell'app. Evita che
     * riparta ogni volta che si torna alla lista gruppi navigando. Si azzera
     * naturalmente alla chiusura del processo.
     */
    var syncOnStartDone: Boolean = false

    /**
     * 1.2.10 (voce Y di NeReO_bug_1.2.9.md, richiesta di Big Umberto via
     * Marco): governa il sync incondizionato di UN gruppo alla sua apertura
     * (`LaunchedEffect(groupId)` in ThreadListScreen.kt/ThreePaneHome.kt,
     * introdotto in beta20 per correggere la regressione opposta — vedi
     * NeReO_stato_progetto.md §5). Non è lo stesso interruttore di
     * [syncOnStart] (che sincronizza TUTTI i gruppi al solo avvio del
     * processo): questo agisce ogni volta che SI ENTRA in un gruppo, incluso
     * il rientro da un articolo letto (Compose smonta e rimonta la
     * schermata a ogni navigazione — vedi Dato 5 in NeReO_bug_1.2.9.md).
     * Default ON: preserva il comportamento per chi non ha mai segnalato
     * il problema (incluso il collaudo di Marco). Chi lo disattiva mantiene
     * comunque il sync manuale (pulsante/pull-to-refresh), MAI toccato da
     * questo interruttore.
     */
    var syncOnGroupOpen by mutableStateOf(true)

    /**
     * 1.4 — un messaggio crospostato letto in un gruppo risulta letto anche
     * negli altri (stesso Message-ID).
     *
     * PREDEFINITO ACCESO, e la ragione e' una misura, non un gusto: Marco usa
     * MesNews da PC tutti i giorni ed e' la fonte primaria su come si comporta.
     * «SE il mio messaggio crospostato lo leggo su un gruppo specifico, poi lo
     * trovo marcato letto anche sugli altri». NeReO nasce per assomigliare a
     * MesNews, e questa e' una delle cose che MesNews fa.
     *
     * Chiude anche un'incoerenza NOSTRA: "seguito" (watched) e i filtri
     * lavorano gia' per Message-ID. Era il "letto" a fare eccezione.
     */
    var lettoSegueMessageId by mutableStateOf(true)

    /**
     * 1.4.1 (105/D, Dave Royal) — "mostra i filtrati".
     *
     * Era una variabile LOCALE della lista discussioni, e la schermata di
     * lettura chiedeva sempre `showHidden = false`. Da quella riga scritta
     * fissa discendevano tre sintomi: nell'albero a comparsa i messaggi
     * filtrati non comparivano mai in grigio come invece fanno nella lista;
     * aprendo l'albero DA un post filtrato usciva "impossibile ricostruire la
     * discussione" (quell'articolo non era nell'elenco, quindi la sua
     * posizione era -1); e su un post filtrato avanti/indietro non facevano
     * niente, per la stessa ragione.
     *
     * Vive qui per essere condivisa fra le due schermate, ma NON viene salvata
     * nelle preferenze di proposito: resta una scelta temporanea come prima, e
     * al riavvio si riparte da "filtrati nascosti".
     */
    var mostraFiltrati by mutableStateOf(false)

    /** Tetto per [scaricaCorpiAutomatico]: valore libero, a discrezione dell'utente. */
    var maxCorpiPerApertura by mutableStateOf(50)

    /** Ultimo stato del filtro "solo non letti" nella lista messaggi (ricordato). */
    var unreadOnly by mutableStateOf(true)

    /** Ordinamento: false = dal più recente al più vecchio; true = dal più vecchio. */
    var sortAscending by mutableStateOf(false)

    /**
     * Criterio di ordinamento delle discussioni. Chiave di [threadSorts].
     *
     * Sostituisce di fatto il vecchio sortAscending, che distingueva solo
     * recente/vecchio ma SEMPRE per ultima attivita' del thread. Ci sono due
     * assi indipendenti: quale data guardare (ultimo messaggio del thread vs
     * apertura del thread) e in che verso. Un solo booleano non li copriva.
     *
     * Retro-compatibilita': all'avvio, se manca "thread_sort" ma c'e' il vecchio
     * "sort_ascending", si traduce il vecchio valore nel criterio "activity".
     */
    var threadSort by mutableStateOf("activity_desc")

    /**
     * Aprendo un messaggio, scarica in anticipo i corpi delle sue risposte
     * dirette, cosi' scorrere il thread è immediato. Attivo di default: costa
     * poco (poche richieste) e migliora sensibilmente la lettura. Disattivabile
     * per chi ha traffico dati limitato.
     */
    var prefetchReplies by mutableStateOf(true)

    /** Colora nelle liste i messaggi da leggere (rosso) e quelli letti (verde). */
    var colorReadState by mutableStateOf(true)

    /**
     * 1.2.9 (Piano B, Roxy): in alternativa al +/- (ingrandito nella
     * correzione P), il tocco sul testo dell'OP di una discussione con
     * risposte espande/comprime il thread invece di aprirlo. Facoltativo e
     * spento di default: chi è abituato al tocco-apre-sempre non nota nulla;
     * chi mira da lontano (TV box, telecomando) lo accende in Aspetto. Con
     * l'opzione attiva l'apertura del messaggio resta comunque disponibile
     * dal menu (tocco prolungato) → "Apri".
     */
    var tapOpToExpand by mutableStateOf(false)

    /**
     * 1.3.2 (Marco): sul TELEFONO, toccare la testa di una discussione apre
     * direttamente il primo messaggio non letto della discussione invece
     * dell'OP. Facoltativa e spenta di default.
     *
     * Perché un'opzione NUOVA e non il riuso di [tapOpToExpand]: quella agisce
     * solo dove la riga rappresenta l'intera discussione richiudibile (vista
     * raggruppata, `total > 1`), e nella vista AD ALBERO — quella usata da chi
     * ha chiesto questa funzione — `total` non è mai valorizzato, quindi non
     * potrebbe scattare mai. Sono due comportamenti diversi in due viste
     * diverse: due interruttori.
     *
     * Nella vista raggruppata, se sono accese entrambe, "espandi" ha la
     * precedenza (là il thread si apre sul posto e restare nella lista è la
     * cosa sensata).
     */
    var apriPrimoNonLetto by mutableStateOf(false)

    /** Gesture di navigazione fra messaggi (swipe) nella lettura. */
    var gesturesEnabled by mutableStateOf(true)

    /** Diagnostica gesture: mostra i valori misurati a fine gesto (per il collaudo). */
    var gestureDebug by mutableStateOf(false)

    /**
     * Diagnostica NNTP ("scatola nera"): registra in memoria il dialogo
     * client/server per capire su quale comando un server risponde male.
     * Spento di default; l'interruttore sta in Manutenzione → Diagnostica.
     */
    var nntpLogEnabled by mutableStateOf(false)

    /** Layout multi-pannello disposto in righe invece che in colonne. */
    var paneVertical by mutableStateOf(false)

    /** Quando usare il layout a pannelli: "auto" (schermi larghi) | "always" | "never". */
    var paneMode by mutableStateOf("auto")

    /** Proporzioni dei tre pannelli (gruppi / discussioni / messaggio). */
    var paneWeightGroups by mutableStateOf(1.0f)
    var paneWeightThreads by mutableStateOf(1.3f)
    var paneWeightArticle by mutableStateOf(1.6f)

    /** Segnale sonoro quando arriva una risposta a un mio messaggio. */
    var dingOnReply by mutableStateOf(false)

    /** Auto-controllo nuovi messaggi ogni N minuti (0 = disabilitato). */
    var autoCheckMinutes by mutableStateOf(0)

    /** La mia email (letta dalle impostazioni Identità): serve a riconoscere le
     *  risposte ai miei messaggi. Aggiornata da IdentityScreen al salvataggio. */
    var myEmail by mutableStateOf("")

    /** Tutte le mie email (globale + per-server), minuscole e non vuote: serve a
     *  riconoscere i "miei" post anche con identità per-server (2.1b). */
    var myEmails by mutableStateOf<Set<String>>(emptySet())

    /**
     * True se l'header From di un articolo corrisponde a una delle MIE
     * identità. Stesso identico criterio già in uso da tempo in due punti
     * (ThreadFlatten, per l'omino "c'è un mio post nella discussione", e
     * Repository, per la rilevazione di `replyToMe` al sync): qui è
     * centralizzato perché dalla 1.2.11 lo usano anche le azioni Sostituisci
     * (supersede) e Annulla (cancel), che hanno senso SOLO sui propri
     * messaggi — e un criterio duplicato in quattro punti è esattamente il
     * modo in cui, storicamente, i quattro punti finiscono per divergere
     * (vedi la lezione della voce V in NeReO_storico_completo.md).
     *
     * Confronto per sottostringa sull'indirizzo minuscolo, come già altrove:
     * il campo From arriva come "Nome <indirizzo>" (col nome a volte in
     * codifica MIME), quindi cercare l'indirizzo dentro la riga è il criterio
     * robusto già collaudato. Con nessuna identità configurata restituisce
     * sempre false: meglio non offrire l'azione che offrirla a vuoto.
     */
    fun eMioPost(fromField: String): Boolean {
        val emails = myEmails
        if (emails.isEmpty()) return false
        val mio = indirizzoDelFrom(fromField) ?: return false
        return emails.any { it.isNotBlank() && it == mio }
    }

    /**
     * Estrae l'indirizzo vero da un campo From, minuscolo. null se non se ne
     * riconosce uno.
     *
     * 1.2.12 (audit di sicurezza): prima [eMioPost] cercava l'indirizzo come
     * SOTTOSTRINGA dell'intera riga From. Bastava quindi che un terzo
     * pubblicasse come `From: "tuo@indirizzo.it" <troll@evil.net>` perché le
     * azioni Sostituisci e Ritira comparissero sul SUO messaggio come se fosse
     * tuo — e il ritiro sarebbe partito a tuo nome, verso i gruppi scelti da
     * lui. Ora si confronta l'indirizzo estratto, per uguaglianza.
     *
     * Si riconoscono entrambe le forme ammesse dallo standard, perché su Usenet
     * si incontrano davvero tutte e due:
     *   Nome Cognome <tizio@example.org>      → si prende ciò che sta fra <>
     *   tizio@example.org (Nome Cognome)      → si prende ciò che precede la (
     * Nel primo caso si usa l'ULTIMA coppia di parentesi angolari: un nome
     * visualizzato può contenerne una finta, quella vera è sempre in coda.
     */
    fun indirizzoDelFrom(fromField: String): String? {
        val f = fromField.trim()
        val apre = f.lastIndexOf('<')
        val chiude = f.lastIndexOf('>')
        val grezzo = if (apre >= 0 && chiude > apre) {
            f.substring(apre + 1, chiude)
        } else {
            f.substringBefore('(').trim()
        }
        val addr = grezzo.trim().lowercase()
        return addr.takeIf { it.contains('@') && !it.any { c -> c.isWhitespace() } }
    }

    /**
     * Presets sfondo lettura → coppia (sfondo, testo) in ARGB.
     * "default" = null ⇒ usa i colori del tema. Le coppie garantiscono contrasto.
     */
    val readingBackgrounds: LinkedHashMap<String, Pair<Long, Long>?> = linkedMapOf(
        "default" to null,
        "sepia" to (0xFFF5ECD9L to 0xFF3A2F1BL),
        "crema" to (0xFFFBF7ECL to 0xFF33302AL),
        "grigio" to (0xFF2B2B2BL to 0xFFE6E6E6L),
        "nero" to (0xFF000000L to 0xFFD8D8D8L),
    )

    /**
     * Tinta di sfondo applicata a tutta l'interfaccia, non solo al corpo del
     * messaggio. [bg] e' lo sfondo delle pagine, [surface] quello delle barre
     * e delle schede (un gradino diverso, altrimenti spariscono), [on] il
     * colore del testo. [dark] dice se la tinta e' scura: da quel flag
     * dipendono tutti gli altri colori derivati del tema.
     *
     * Le coppie sono scelte con contrasto ampiamente sopra il minimo
     * leggibile, cosi' nessuna combinazione risulta faticosa.
     */
    data class UiPalette(
        val label: String,
        val bg: Long,
        val surface: Long,
        val on: Long,
        val dark: Boolean,
    )

    /** "default" = null ⇒ nessuna tinta, valgono i colori normali del tema. */
    val uiPalettes: LinkedHashMap<String, UiPalette?> = linkedMapOf(
        "default" to null,
        "sepia" to UiPalette("Sepia", 0xFFF4EAD5L, 0xFFEADFC6L, 0xFF3A2F1BL, false),
        "crema" to UiPalette("Crema", 0xFFFBF6E9L, 0xFFF2ECDAL, 0xFF33302AL, false),
        "menta" to UiPalette("Verde menta", 0xFFE7F0E7L, 0xFFDAE7DAL, 0xFF1F2E1FL, false),
        "cielo" to UiPalette("Azzurro chiaro", 0xFFE7EFF7L, 0xFFD9E5F1L, 0xFF1D2A36L, false),
        "lavanda" to UiPalette("Lavanda", 0xFFEDE9F5L, 0xFFE1DBEEL, 0xFF2A2440L, false),
        "ardesia" to UiPalette("Ardesia", 0xFF262B31L, 0xFF313841L, 0xFFDCE3EAL, true),
        "notte" to UiPalette("Blu notte", 0xFF121A2AL, 0xFF1B2436L, 0xFFCDD7EAL, true),
        "bosco" to UiPalette("Verde bosco", 0xFF15221AL, 0xFF1E2E24L, 0xFFCFE0D4L, true),
        "carbone" to UiPalette("Carbone", 0xFF171717L, 0xFF222222L, 0xFFE0E0E0L, true),
        // Purple, rivisto nella beta10.
        //
        // La prima versione usava il viola del riferimento (#502E92) come
        // sfondo, con le barre in #5E3AA8: fra i due c'era 1,23:1 di
        // separazione, cioè le barre annegavano nel fondo. E il rosso dei
        // messaggi da leggere si fermava a 4,28:1, sotto la soglia leggibile.
        //
        // Ora lo sfondo è un viola più profondo e le barre riprendono la tinta
        // del riferimento, così si staccano. Verificate TUTTE le combinazioni
        // in uso (testo, non letti, letti, testo tenue, barrato) su entrambi i
        // fondi: la peggiore è 4,6:1, tutte sopra il minimo.
        "purple" to UiPalette("Purple", 0xFF331C60L, 0xFF4A2C86L, 0xFFEDE7F6L, true),
    )

    /** Livelli di densita' del pannello discussioni, dal piu' largo al piu' fitto. */
    val paneListScales: LinkedHashMap<String, Float> = linkedMapOf(
        "Normale" to 1.0f,
        "Compatta" to 0.88f,
        "Fitta" to 0.76f,
        "Molto fitta" to 0.66f,
        "Massima" to 0.58f,
    )

    /**
     * I quattro criteri d'ordine, con etichetta esplicita.
     *   activity_* : per data del messaggio piu' recente del thread (il thread
     *                che "si muove" sale in cima). Comportamento storico.
     *   opened_*   : per data di APERTURA del thread, cioe' del messaggio
     *                radice. Ordine classico dei newsreader da PC.
     */
    val threadSorts: LinkedHashMap<String, String> = linkedMapOf(
        "activity_desc" to "Per messaggio piu' recente del thread",
        "activity_asc" to "Per messaggio piu' vecchio del thread",
        "opened_desc" to "Per apertura del thread, dai piu' recenti",
        "opened_asc" to "Per apertura del thread, dai piu' vecchi",
    )

    val accentColors = linkedMapOf(
        "indigo" to 0xFF3F51B5,
        "teal" to 0xFF00695C,
        "green" to 0xFF2E7D32,
        "amber" to 0xFFFF8F00,
        "red" to 0xFFC62828,
        "purple" to 0xFF6A1B9A,
    )

    /**
     * 1.2.12 (audit di sicurezza) — l'avvio non può più essere bloccato da una
     * preferenza col tipo sbagliato.
     *
     * `init` è la seconda istruzione di `App.onCreate`: se lancia, l'app non
     * parte, e l'unico rimedio per l'utente è "cancella dati", cioè perdere
     * tutto. Un valore col tipo sbagliato (per esempio la stringa "x" dove ci
     * si aspetta un booleano) fa lanciare `ClassCastException` a
     * `SharedPreferences`, e questa è esattamente la situazione che un file di
     * backup malformato — o ostile — poteva produrre, visto che il ripristino
     * scrive le preferenze col tipo dichiarato nel file.
     *
     * Ora un guasto del genere costa le sole PREFERENZE (tema, dimensioni,
     * interruttori: si riparte dai valori predefiniti), non i dati: server,
     * identità, filtri, gruppi e messaggi vivono nel database e non vengono
     * toccati.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        try {
            caricaPreferenze()
        } catch (_: ClassCastException) {
            prefs.edit().clear().commit()
            caricaPreferenze()
        }
    }

    private fun caricaPreferenze() {
        themeMode = prefs.getString("theme_mode", "system") ?: "system"
        dynamicColor = prefs.getBoolean("dynamic_color", true)
        accent = prefs.getString("accent", "indigo") ?: "indigo"
        bodyMonospace = prefs.getBoolean("body_monospace", true)
        // le vecchie scale (0.85 / 1.0) erano troppo piccole: portate al minimo attuale
        // beta15: il minimo consentito scende a 0.8 (nuove taglie Piccolissimo/
        // Molto piccolo). Prima era coerceAtLeast(1.15f) — che ora risalirebbe
        // le nuove taglie all'ex-minimo, vanificandole.
        bodyFontScale = prefs.getFloat("body_font_scale", 1.3f).coerceAtLeast(0.8f)
        autoPurgeDays = prefs.getInt("auto_purge_days", 0)
        threadView = prefs.getString("thread_view", "tree") ?: "tree"
        readingBg = prefs.getString("reading_bg", "default") ?: "default"
        uiPalette = prefs.getString("ui_palette", "default") ?: "default"
        paneListScale = prefs.getFloat("pane_list_scale", 1.0f)
        collapseBlankLines = prefs.getBoolean("collapse_blank_lines", false)
        tastiVolume = prefs.getBoolean("volume_keys", false)
        animazioni = prefs.getBoolean("screen_animations", true)
        reflowText = prefs.getBoolean("reflow_text", false)
        // 1.4.8 (par. B6b): spento di default, vedi la nota sulla proprieta'
        formatFlowedInUscita = prefs.getBoolean("format_flowed_out", false)
        soloIconaAggiornaPannelli = prefs.getBoolean("solo_icona_aggiorna_pannelli", true)
        shortenQuotes = prefs.getBoolean("shorten_quotes", false)
        mutaCitazioni = prefs.getBoolean("muta_citazioni", false)
        rispostaCrosspostTuttiIGruppi = prefs.getBoolean("risposta_crosspost_tutti", false)
        // 1.2.8: migrazione silenziosa dal vecchio booleano "auto_check_updates"
        // (1.2.5-1.3) — se non è mai stata salvata una modalità, la si deriva
        // da lì: true→"daily" (comportamento storico), false→"never".
        autoCheckMode = prefs.getString("auto_check_mode", null)
            ?: if (prefs.getBoolean("auto_check_updates", true)) "daily" else "never"
        lastUpdateCheckMs = prefs.getLong("last_update_check_ms", 0L)
        lastUpdateCheckFound = prefs.getString("last_update_check_found", "") ?: ""
        consentiAuthNonCifrata = prefs.getBoolean("consenti_auth_non_cifrata", false)
        autenticazionePigra = prefs.getBoolean("autenticazione_pigra", false)
        consentiCertificatoNonVerificabile = prefs.getBoolean("consenti_cert_non_verificabile", false)
        tapOpToExpand = prefs.getBoolean("tap_op_to_expand", false)
        apriPrimoNonLetto = prefs.getBoolean("apri_primo_non_letto", false)
        sogliaAccorciaCitazioni = prefs.getInt("soglia_accorcia_citazioni", 6)
        quoteStyle = prefs.getString("quote_style", "bars") ?: "bars"
        numericQuoteDepth = prefs.getBoolean("numeric_quote_depth", true)
        sogliaCitazioniANumeri = prefs.getInt("soglia_citazioni_a_numeri", 8)
        // 1.4.9 (§J1): acceso di default — vedi la dichiarazione.
        rotaieAlbero = prefs.getBoolean("rotaie_albero", true)
        // 1.4.9 (§J3-C1): vuota = cartella non ancora scelta, o permesso perso.
        cartellaConserva = prefs.getString("cartella_conserva", "") ?: ""
        markReadOnExit = prefs.getBoolean("mark_read_on_exit", false)
        alberoInBasso = prefs.getBoolean("albero_in_basso", false)
        // 1.3 (B): se non è mai stato salvato un ordine, semina da alberoInBasso
        // (migrazione silenziosa — vedi commento sulla dichiarazione).
        if (prefs.contains("article_bar_order")) {
            val salvato = (prefs.getString("article_bar_order", "") ?: "")
                .split(",").filter { it.isNotBlank() }
            val noti = articleBarActionIds.toSet()
            val ripulito = salvato.filter { it in noti }.distinct()
            val mancanti = articleBarActionIds.filter { it !in ripulito }
            articleBarOrder = ripulito + mancanti
            articleBarHidden = (prefs.getString("article_bar_hidden", "") ?: "")
                .split(",").filter { it.isNotBlank() }.toSet()
        } else {
            articleBarOrder = articleBarActionIds
            articleBarHidden = if (!alberoInBasso) setOf("tree_bottom") else emptySet()
        }
        // 1.3.1: barra del riquadro di lettura nei pannelli. Stessa forma della
        // barra del telefono qui sopra: si ripuliscono gli id ignoti e si
        // riaccodano quelli mancanti, così un ordine salvato da una versione
        // futura con più azioni non lascia mai buchi.
        if (prefs.contains("pane_bar_order")) {
            val salvatoP = (prefs.getString("pane_bar_order", "") ?: "")
                .split(",").filter { it.isNotBlank() }
            val notiP = paneBarActionIds.toSet()
            val ripulitoP = salvatoP.filter { it in notiP }.distinct()
            val nuoveP = paneBarActionIds.filter { it !in ripulitoP }
            paneBarOrder = ripulitoP + nuoveP
            /*
             * 1.3.6: un'azione che il salvataggio NON conosceva nasce NEL MENU,
             * non in barra.
             *
             * La barra del riquadro divide la riga con l'oggetto del messaggio:
             * ogni icona in piu' e' spazio tolto al titolo, ed e' il motivo per
             * cui Dave aveva chiesto di spostarne due nel menu. Facendo comparire
             * d'ufficio in barra ogni azione nuova, un aggiornamento
             * restringerebbe l'oggetto a ogni versione senza che nessuno lo
             * abbia chiesto.
             *
             * Non nasconde niente: l'azione c'e' ed e' nel menu ⋮, e due tocchi
             * nell'editor la portano in barra. La regola vale da qui in avanti
             * per QUALUNQUE azione futura, non solo per queste due.
             */
            paneBarInMenu = (prefs.getString("pane_bar_in_menu", "") ?: "")
                .split(",").filter { it.isNotBlank() }.toSet() + nuoveP
        } else {
            paneBarOrder = paneBarActionIds
            paneBarInMenu = paneBarInMenuIniziale
        }
        vistaContinua = prefs.getBoolean("vista_continua", false)
        maxHeadersSync = prefs.getInt("max_headers_sync", 1000)
        scaricaCorpiAutomatico = prefs.getBoolean("scarica_corpi_automatico", false)
        syncOnStart = prefs.getBoolean("sync_on_start", false)
        syncOnGroupOpen = prefs.getBoolean("sync_on_group_open", true)
        lettoSegueMessageId = prefs.getBoolean("letto_segue_messageid", true)
        maxCorpiPerApertura = prefs.getInt("max_corpi_per_apertura", 50)
        unreadOnly = prefs.getBoolean("unread_only", true)
        sortAscending = prefs.getBoolean("sort_ascending", false)
        threadSort = prefs.getString("thread_sort", null)
            ?: if (sortAscending) "activity_asc" else "activity_desc"
        prefetchReplies = prefs.getBoolean("prefetch_replies", true)
        colorReadState = prefs.getBoolean("color_read_state", true)
        gesturesEnabled = prefs.getBoolean("gestures_enabled", true)
        gestureDebug = prefs.getBoolean("gesture_debug", false)
        nntpLogEnabled = prefs.getBoolean("nntp_log_enabled", false)
        NntpLog.enabled = nntpLogEnabled // sincronizza la scatola nera col valore salvato
        paneVertical = prefs.getBoolean("pane_vertical", false)
        paneMode = prefs.getString("pane_mode", "auto") ?: "auto"
        paneWeightGroups = prefs.getFloat("pane_w_groups", 1.0f)
        paneWeightThreads = prefs.getFloat("pane_w_threads", 1.3f)
        paneWeightArticle = prefs.getFloat("pane_w_article", 1.6f)
        dingOnReply = prefs.getBoolean("ding_on_reply", false)
        autoCheckMinutes = prefs.getInt("auto_check_minutes", 0)
        // 1.2.9: myEmail/myEmails vivono ora nella tabella `identities` (DB,
        // accesso sospendibile) e non più nelle SharedPreferences "identity"
        // lette qui sopra: init() resta sincrona, quindi NON può più caricarle
        // direttamente. Le ricarica App.onCreate(), in una coroutine, DOPO
        // l'eventuale migrazione delle identità legacy — vedi reloadIdentityEmails
        // sotto.
    }

    /**
     * Ricarica [myEmail] (l'email dell'identità predefinita) e [myEmails]
     * (l'unione delle email di TUTTE le identità salvate, minuscole e non
     * vuote) dalla tabella `identities`. Sostituisce, dalla 1.2.9, la vecchia
     * lettura dalle SharedPreferences "identity" (email globale + per-server):
     * con le identità reali ogni email vive nel DB, non più nelle prefs.
     *
     * Da chiamare all'avvio (dopo l'eventuale migrazione delle identità
     * legacy), dopo il salvataggio di un'identità e dopo un ripristino da
     * backup. (2.1b, esteso 1.2.9)
     */
    suspend fun reloadIdentityEmails(repo: org.openusenet.reader.data.NewsRepository) {
        val tutte = repo.identitiesOnce()
        val predefinita = tutte.firstOrNull { it.isDefault } ?: tutte.firstOrNull()
        myEmail = predefinita?.email?.trim() ?: ""
        val set = LinkedHashSet<String>()
        for (ident in tutte) {
            ident.email.trim().lowercase().takeIf { it.isNotBlank() }?.let { set.add(it) }
        }
        myEmails = set
    }

    fun save() {
        prefs.edit()
            .putString("theme_mode", themeMode)
            .putBoolean("dynamic_color", dynamicColor)
            .putString("accent", accent)
            .putBoolean("body_monospace", bodyMonospace)
            .putFloat("body_font_scale", bodyFontScale)
            .putInt("auto_purge_days", autoPurgeDays)
            .putString("thread_view", threadView)
            .putString("reading_bg", readingBg)
            .putString("ui_palette", uiPalette)
            .putFloat("pane_list_scale", paneListScale)
            .putBoolean("collapse_blank_lines", collapseBlankLines)
            .putBoolean("volume_keys", tastiVolume)
            .putBoolean("screen_animations", animazioni)
            .putBoolean("reflow_text", reflowText)
            .putBoolean("format_flowed_out", formatFlowedInUscita)
            .putBoolean("solo_icona_aggiorna_pannelli", soloIconaAggiornaPannelli)
            .putBoolean("shorten_quotes", shortenQuotes)
            .putBoolean("muta_citazioni", mutaCitazioni)
            .putBoolean("risposta_crosspost_tutti", rispostaCrosspostTuttiIGruppi)
            .putString("auto_check_mode", autoCheckMode)
            .putLong("last_update_check_ms", lastUpdateCheckMs)
            .putString("last_update_check_found", lastUpdateCheckFound)
            .putBoolean("consenti_auth_non_cifrata", consentiAuthNonCifrata)
            .putBoolean("autenticazione_pigra", autenticazionePigra)
            .putBoolean("consenti_cert_non_verificabile", consentiCertificatoNonVerificabile)
            .putBoolean("tap_op_to_expand", tapOpToExpand)
            .putBoolean("apri_primo_non_letto", apriPrimoNonLetto)
            .putInt("soglia_accorcia_citazioni", sogliaAccorciaCitazioni)
            .putString("quote_style", quoteStyle)
            .putBoolean("numeric_quote_depth", numericQuoteDepth)
            .putInt("soglia_citazioni_a_numeri", sogliaCitazioniANumeri)
            .putBoolean("rotaie_albero", rotaieAlbero)
            .putString("cartella_conserva", cartellaConserva)
            .putBoolean("mark_read_on_exit", markReadOnExit)
            .putBoolean("albero_in_basso", alberoInBasso)
            .putString("article_bar_order", articleBarOrder.joinToString(","))
            .putString("article_bar_hidden", articleBarHidden.joinToString(","))
            .putString("pane_bar_order", paneBarOrder.joinToString(","))
            .putString("pane_bar_in_menu", paneBarInMenu.joinToString(","))
            .putBoolean("vista_continua", vistaContinua)
            .putInt("max_headers_sync", maxHeadersSync)
            .putBoolean("scarica_corpi_automatico", scaricaCorpiAutomatico)
            .putBoolean("sync_on_start", syncOnStart)
            .putBoolean("sync_on_group_open", syncOnGroupOpen)
            .putBoolean("letto_segue_messageid", lettoSegueMessageId)
            .putInt("max_corpi_per_apertura", maxCorpiPerApertura)
            .putBoolean("unread_only", unreadOnly)
            .putBoolean("sort_ascending", sortAscending)
            .putString("thread_sort", threadSort)
            .putBoolean("prefetch_replies", prefetchReplies)
            .putBoolean("color_read_state", colorReadState)
            .putBoolean("gestures_enabled", gesturesEnabled)
            .putBoolean("gesture_debug", gestureDebug)
            .putBoolean("nntp_log_enabled", nntpLogEnabled)
            .putBoolean("pane_vertical", paneVertical)
            .putString("pane_mode", paneMode)
            .putFloat("pane_w_groups", paneWeightGroups)
            .putFloat("pane_w_threads", paneWeightThreads)
            .putFloat("pane_w_article", paneWeightArticle)
            .putBoolean("ding_on_reply", dingOnReply)
            .putInt("auto_check_minutes", autoCheckMinutes)
            .apply()
    }

    // ------------------------------------------------------------------
    // par. B3 (1.4.11) — MEMORIA «QUESTO SERVER OFFRIVA STARTTLS»
    // ------------------------------------------------------------------
    //
    // Richiesta di ObiWan. RFC 4642 par. 5 suggerisce di REGISTRARE che un
    // server offre TLS e di dare un ALLARME se in una sessione successiva la
    // capability non c'e' piu'. Serve una memoria che sopravviva alla chiusura
    // dell'app, quindi sta nelle preferenze e non in RAM.
    //
    // ⚠️ NON e' un'impostazione dell'utente: non ha un interruttore, non
    // compare in nessuna schermata e non entra in [load]/[save]. Vive qui
    // perche' qui c'e' [prefs], che e' privato. Si legge e si scrive al volo,
    // cosi' un ripristino da backup (che riscrive le prefs e richiama [init])
    // la ritrova senza passare da uno stato in memoria.
    //
    // ⚠️ CHIAVE host:porta, NON l'id di riga del server. Al ripristino i
    // server prendono id nuovi — BackupManager rimappa per host:porta — e una
    // memoria per id finirebbe sul server sbagliato. E' anche la granularita'
    // giusta: due righe sullo stesso host:porta vedono la stessa CAPABILITIES.
    //
    // ⚠️ UNA STRINGA, non un Set. Il backup par. 4b itera `prefs.all` e
    // riconosce b/i/l/f/s: un `StringSet` finirebbe nel suo `else -> continue`
    // e sparirebbe dal backup in silenzio. Separatore `\n` perche' un nome
    // host non puo' contenerlo.
    private const val CHIAVE_STARTTLS_VISTO = "starttls_visto"

    private fun chiaveServerTls(host: String, port: Int): String =
        host.trim().lowercase() + ":" + port

    private fun insiemeStarttlsVisto(): Set<String> =
        (prefs.getString(CHIAVE_STARTTLS_VISTO, "") ?: "")
            .split('\n')
            .filter { it.isNotBlank() }
            .toSet()

    /**
     * true se questo host:porta ha annunciato STARTTLS in una sessione
     * precedente. Vale SOLO per dare l'allarme: non deve cambiare il percorso
     * di connessione (RFC 4642 par. 2.1 — «MUST NOT be relied on»).
     *
     * [runCatching] perche' [prefs] e' `lateinit`: un collegamento prima di
     * [init] e' impossibile nell'app reale, ma qui un'eccezione non deve
     * nemmeno potersi affacciare — questa funzione gira dentro la
     * preparazione di OGNI connessione.
     */
    fun starttlsGiaVisto(host: String, port: Int): Boolean =
        runCatching { chiaveServerTls(host, port) in insiemeStarttlsVisto() }
            .getOrDefault(false)

    /** Registra che questo host:porta offre STARTTLS. Idempotente. */
    fun ricordaStarttlsVisto(host: String, port: Int) {
        runCatching {
            val k = chiaveServerTls(host, port)
            val attuale = insiemeStarttlsVisto()
            if (k in attuale) return@runCatching
            prefs.edit()
                .putString(CHIAVE_STARTTLS_VISTO, (attuale + k).joinToString("\n"))
                .apply()
        }
    }
}
