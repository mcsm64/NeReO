// SPDX-License-Identifier: GPL-3.0-or-later
import java.time.LocalDate
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.openusenet.reader"
    // 1.3.4: alzato da 35 a 37 perche' Compose 1.12 lo pretende.
    //
    // compileSdk NON e' targetSdk, e la differenza qui e' tutto:
    //  - compileSdk = contro quale versione di Android si COMPILA. Cambia
    //    solo cosa il compilatore conosce, non come l'app si comporta.
    //  - targetSdk  = a quali regole del sistema l'app dichiara di essersi
    //    adeguata. Alzarlo fa scattare restrizioni nuove (permessi,
    //    notifiche, accesso ai file): QUELLO cambia il comportamento.
    // Percio' targetSdk resta 35, ed e' una scelta: se dopo questa release
    // qualcosa si comporta male, il colpevole e' Compose o AGP, non Android
    // che ha cambiato le regole sotto i piedi. Il targetSdk si alzera' da
    // solo, in una release sua, quando avremo voglia di collaudare quello.
    // Nessun obbligo esterno: NeReO sta su SourceForge, non sul Play Store.
    //
    // NB: l'AGP Upgrade Assistant ha messo in gradle.properties
    // `android.sdk.defaultTargetSdkToCompileSdkIfUnset=false`, quindi il
    // disaccoppiamento e' garantito dalla configurazione, non dalla memoria.
    compileSdk = 37

    defaultConfig {
        applicationId = "org.openusenet.reader"
        minSdk = 26 // java.util.Base64 richiede API 26+
        targetSdk = 35
        // PROMEMORIA (bug beta11, tester): ReleaseNotes.kt ha VERSION e
        // sections SEPARATI da qui apposta (testo curato a mano) — ad ogni
        // bump qui sotto, aggiornare SEMPRE anche ReleaseNotes.kt, altrimenti
        // la schermata "Novità versione" mostra la release sbagliata.
        // 1.2.8 (Marco, 1 settembre 2026): tappa intermedia fra la 1.2.7 e la
        // futura 1.3 "completa" — raccoglie tutto ciò che era stato preparato
        // come 1.3/1.3.1/1.3.2 durante il collaudo in solitaria, mai uscite
        // ufficialmente. versionCode NON riparte da dopo il 54 (1.2.7): resta
        // più alto di ogni APK di prova già generato in questa sessione
        // (fino al 57), così un eventuale aggiornamento sul device di Marco
        // non viene mai bloccato da Android per "versione più vecchia".
        // 1.2.9 (Marco, 2 settembre 2026): multi-identità vera + tutta la
        // roadmap NNTP rimasta (voce E), nella stessa release — la stessa
        // regola di sempre: versionCode monotono, mai riusato.
        // 1.2.9.b (Marco, 4 settembre 2026): a differenza delle correzioni
        // O-W (versione tenuta invariata perché la 1.2.9 non era mai stata
        // pubblicata agli utenti finali), questa volta Marco vuole
        // ricompilare e distribuire — quindi la versione DEVE cambiare
        // perché chi ha già la 1.2.9 possa vedere l'aggiornamento. "b" sta
        // per "bugfixing": tre correzioni dal collaudo pubblico (U, V, W) +
        // il nuovo log crash minimo (Piano C.1, NeReO_bug_1.2.9.md voce X).
        // 1.2.9.c (Marco, 4 settembre 2026): Piano B della stessa voce X —
        // il sync di testate (syncGroup/syncGroupList) non aveva NESSUNA
        // guardia di concorrenza contro il timer "Controllo automatico
        // nuovi messaggi": una corsa a tempo reale e dimostrata nel codice,
        // candidata concreta (non ancora confermata da uno stack trace) per
        // il crash. Aggiunto headerSyncMutex (Repository.kt), stesso
        // principio di bodyFetchMutex. Via libera esplicito di Marco: "vai
        // con B in una 1.2.9.c".
        // 1.2.10 (Marco, 5 settembre 2026): via libera "applichiamo A per
        // voce Y ed usciamo con una 1.2.10 da far testare agli utenti".
        // Piano A della voce Y (Big Umberto, NeReO_bug_1.2.9.md): nuovo
        // switch dedicato AppSettings.syncOnGroupOpen per disattivare il
        // sync incondizionato all'apertura di un gruppo. Insieme, prima
        // versione reale a portare anche il Piano B del Dato 5 (difesa
        // .catch/onEach in Repository.articles() contro il CursorWindow
        // IllegalStateException del 4-5 settembre), rimasto "a versione
        // invariata" nella 1.2.9.c in attesa di questo giro.
        // 1.2.11 (Marco, 5 settembre 2026): aggiornamento della libreria Room
        // 2.6.1 -> 2.8.4 (Piano C del Dato 5, NeReO_bug_1.2.9.md) - non 3.0
        // (pacchetto androidx.room3 separato, riscrittura KMP, scartato:
        // nessun vantaggio documentato su questo bug a fronte di un costo
        // di migrazione molto piu' alto). La 2.8.4 contiene due correzioni
        // ufficiali su corse Flow-contro-scrittura-concorrente (2.7.0-rc01,
        // 2.8.1), la stessa categoria del Dato 5 - ABDUZIONE, non conferma:
        // le difese applicative .catch (Repository.articles/searchArticles/
        // searchArticlesInGroup) restano, questo e' un livello aggiuntivo,
        // non un sostituto. Se il crash sparisce dopo un giro di collaudo,
        // Marco la convertira' in 1.3; altrimenti resta 1.2.11.
        // 1.2.12 (Marco, 6 settembre 2026): release di SICUREZZA, dall'audit
        // completo del codice (NeReO_sicurezza_audit.md). Chiude i punti che
        // non toccano la connettivita': sanificazione delle intestazioni in
        // invio, tetti su riga/blocco nelle risposte del server, tetto e
        // guardia sul threading (References ostile), ripristino di backup
        // validato e avvio a prova di preferenze corrotte, escape del plonk,
        // Face con limiti di decodifica, chiavi di ricerca univoche, criterio
        // "e' mio" stretto all'indirizzo, ReDoS arginata. Include anche le due
        // funzioni scritte il 6 settembre a versione invariata (Sostituisci
        // solo sui propri messaggi, Ritira/cancel). NON include la verifica
        // TLS del nome host: e' l'unica correzione che puo' far smettere di
        // connettersi cio' che oggi funziona, quindi va in una release
        // separata e sola, per poterla attribuire senza ambiguita'.
        // 1.2.13 (Marco, 6 settembre 2026): due cose, entrambe piccole e
        // indipendenti dalla connettivita'. (1) Ritentativo trasparente su
        // connessione riusata: chiude il "Errore: Software caused connection
        // abort" mostrato al posto del corpo, diagnosticato col log NNTP del 6
        // settembre - vedi NeReO_bug_connessione_riusata.md. (2) Richiesta di
        // Merovingio: lo User-Agent di una identita' NUOVA nasce compilato col
        // segnaposto %nereo%, invece che vuoto (anonimo per distrazione).
        // La verifica TLS del nome host slitta di conseguenza alla 1.2.14:
        // resta l'unica correzione che esce DA SOLA.
        // 1.2.14 (Marco, 6 settembre 2026): VERIFICA DEL NOME HOST TLS — la voce
        // piu' grave dell'audit di sicurezza (NeReO_sicurezza_audit.md §1.1),
        // rimandata due volte di proposito perche' e' l'UNICA correzione che
        // puo' far smettere di funzionare una configurazione che oggi funziona.
        // Esce percio' DA SOLA: se un tester non si connette piu', la causa e'
        // senza ambiguita' questa. Con interruttore di consenso GLOBALE
        // (Manutenzione -> Sicurezza), spento di default, sul modello gia' usato
        // dalla 1.2.6 per l'autenticazione non cifrata — Piano B scelto da
        // Marco fra tre: niente per-server per ora, che richiederebbe una
        // migrazione del database per una funzione di cui non sappiamo ancora
        // se qualcuno avra' bisogno.
        // 1.2.15 (Marco, 7 settembre 2026): giro di rifiniture, via libera
        // esplicito "io direi da fare tutto! anche D! Non voglio compilare
        // mille volte". Sei voci in una sola release, tutte gia' discusse:
        //  A) NntpException fuori dal ritentativo della 1.2.13 (difetto mio:
        //     NntpException estende IOException, quindi un 430 faceva buttare
        //     una connessione sana e ripetere la domanda);
        //  B) "430 No such article" con messaggio proprio + pulsante per
        //     togliere la copia locale rimasta orfana;
        //  C) verifica con STAT dopo un cancel: si dice all'utente se il
        //     ritiro e' avvenuto davvero (Astraweb ignora i control message,
        //     OVH li onora - provato sul campo il 6 settembre);
        //  D) Supersedes gestito in lettura: il messaggio sostituito viene
        //     NASCOSTO (non eliminato), e solo se ha lo stesso autore;
        //  E) SNIHostName non riceve piu' un indirizzo IP letterale (su IPv6
        //     lanciava IllegalArgumentException non catturata - misurato su
        //     JDK 21 il 7 settembre);
        //  F) la connessione dei corpi in cache non rileggeva gli interruttori
        //     di sicurezza: rispegnendo "Consenti certificati non
        //     verificabili" restava utilizzabile per un altro minuto una
        //     connessione aperta SENZA verifica. Emerso dalla sequenza stessa
        //     del collaudo della 1.2.14.
        // A differenza della 1.2.14 qui si puo' raggruppare: nessuna di queste
        // modifiche puo' impedire una connessione che oggi funziona, quindi
        // non serve poterle attribuire una per una.
        // 1.2.16 (Marco, 7 settembre 2026): "piano B e prepara la 1.2.16".
        // Nasce dalla QUARTA prova sul campo: ritiro iniettato da
        // eternal-september, rimosso li' e intatto su Astraweb e OVH anche dopo
        // un ricaricamento da remoto. Tre prove di ritiro su tre, e nessuna ha
        // mai tolto un articolo da un server diverso da quello di iniezione:
        // il ritiro e', in pratica, un'operazione LOCALE al proprio server.
        // La verifica STAT della 1.2.15 diceva il vero, ma su un server solo,
        // mentre la domanda che l'utente si pone e' "e sugli altri?" - quella a
        // cui Marco ha risposto tre volte a mano aprendo MesNews sul PC. Ora
        // l'app interroga anche gli altri server configurati (una connessione
        // ciascuno, solo in quel momento) e mostra un esito per server; il
        // dialogo di conferma dichiara il limite prima, invece di lasciarlo
        // scoprire dopo. Nessun rischio per la connettivita': si aggiungono
        // interrogazioni, non si cambia come ci si collega.
        // 1.2.17 (Marco, 7 settembre 2026): "facciamo subito A+B+C". Nasce dal
        // log del collaudo della 1.2.16: su news.usenet.ovh la password parte
        // IN CHIARO ad ogni collegamento (il server annuncia STARTTLS e
        // risponde 580 Error initializing TLS; Marco ha poi verificato che il
        // servizio non offre TLS su nessuna porta, e SASL non e' annunciato).
        // Non potendo cifrare, si fanno le due sole cose sensate:
        //  B) AUTENTICAZIONE PIGRA (opzione, SPENTA di default): la password
        //     parte al primo 480 del server invece che ad ogni connessione. Su
        //     un server INN, che lascia leggere senza credenziali, significa
        //     inviarla solo quando si pubblica. Spenta di default perche' tocca
        //     il modo in cui ci si collega a OGNI server - stessa cautela della
        //     1.2.14;
        //  C) SEGNO PERMANENTE nell'elenco server quando l'ultimo collegamento
        //     non era cifrato. Permanente e non un avviso a ogni sessione:
        //     quello stato non cambia mai, e un avviso ricorrente diventerebbe
        //     rumore da ignorare. Basato sulla cifratura OSSERVATA, non su
        //     quella configurata - e' l'unico dato onesto.
        // Piu' una voce minore dallo stesso log: il contatore di sessione non
        // conta piu' come errori 430/580/480, che oggi sono risposte ATTESE
        // (un ritiro riuscito chiudeva la sessione con "1 errori").
        // 1.3 (Marco, 7 settembre 2026): LA STABILE. Marco: "vorrei uscire in
        // giornata con la 1.3 finale". Rispetto alla 1.2.17 cambia UNA cosa, ed
        // e' la correzione di un difetto mio trovato nel log del suo collaudo:
        // l'autenticazione pigra non veniva applicata a postArticle ne' a
        // refreshAvailableGroups, perche' quel blocco di codice esisteva in
        // QUATTRO copie e ne avevo aggiornate due. Per questo il POST su OVH non
        // mostrava mai la riga "il server chiede l'autenticazione": non passava
        // dalla strada pigra. Ora i quattro siti sono unificati in
        // preparaConnessione(): la regola vale ovunque per costruzione, e la
        // prossima si scrivera' in un posto solo.
        // NOTA SULLA PUBBLICAZIONE: finche' questo APK non e' caricato su
        // SourceForge, versionName puo' restare "1.3" anche se servisse un altro
        // giro - basta alzare versionCode (mai riusato). E' lo stesso criterio
        // gia' applicato alla 1.2.9 con le correzioni O-W.
        // 1.3.1 (Marco, 8 settembre 2026): PRIMO RISCONTRO REALE sulla 1.3, da
        // Roxy. Nel layout a pannelli, con "Tocca l'OP per espandere" acceso,
        // il tocco sulla riga della discussione espandeva SOLTANTO: il ramo che
        // seleziona l'articolo non veniva mai eseguito. Nella 1.2.9 avevamo
        // dichiarato come via d'uscita il menu a pressione prolungata, ma sul
        // suo Box TV col telecomando IL TOCCO PROLUNGATO NON ESISTE: l'OP di
        // qualunque discussione con risposte era semplicemente irraggiungibile.
        // Ora fa "espandi E VAI al primo non letto" (ripiego sull'OP se sono
        // tutti letti), e chiudendo riporta la selezione sull'OP. Solo nei
        // pannelli: sul telefono il tocco naviga via, quindi "espandi e vai"
        // sarebbe una contraddizione, e li' il tocco prolungato c'e'.
        // Roxy ha confermato esplicitamente anche l'effetto collaterale (il
        // messaggio raggiunto viene marcato letto): "e' proprio cosi' che
        // funziona [...] Cosi' va benissimo".
        // 1.3.1, secondo giro (8 settembre 2026, "se serve ricompila tutto come
        // 1.3.1"): versionName INVARIATO perche' la 1.3.1 non e' ancora stata
        // pubblicata, versionCode alzato perche' e' monotono e non si riusa —
        // stesso criterio gia' applicato alla 1.2.9 con le correzioni O-W.
        // Si aggiunge il menu ⋮ sulle righe della lista nei pannelli: nasce
        // dalla risposta di Roxy alla domanda sul tocco prolungato. Non era
        // vero, come avevo ipotizzato, che quel menu per lui NON ESISTE: si
        // apre tenendo premuto il tasto sinistro del mini trackpad. Ma
        // "l'operazione sarebbe davvero lenta e poco immediata doverlo fare
        // ogni volta, passerebbe la voglia di usarlo". Non impossibile:
        // IMPRATICABILE. La pressione prolungata resta, il ⋮ si aggiunge.
        // 1.3.1, terzo giro (8 settembre 2026): Marco sceglie il Piano C —
        // "io pero' farei gia' C perche' quella richiesta me l'ha gia' fatta
        // Roxy". Suggerimento di Dave Royal (tablet in VERTICALE): mettere
        // ROT13 e Intestazioni nel menu ⋮ e tenere fuori le citazioni. Due
        // richieste indipendenti sullo stesso punto -> non si spostano due
        // icone, si rende la barra PERSONALIZZABILE come gia' quella del
        // telefono. Differenza voluta rispetto al telefono: qui "spento" non
        // e' "nascosto" ma "spostato nel menu ⋮" — nulla diventa
        // irraggiungibile, che su un dispositivo a puntatore conta.
        // 1.3.2 (9 settembre 2026): qui versionName CAMBIA, e non e' un
        // dettaglio. La 1.3.1 e' stata PUBBLICATA su SourceForge, quindi il
        // nome e' bruciato: due APK diversi non possono chiamarsi uguale una
        // volta usciti. E' l'altra meta' della regola applicata sopra (finche'
        // non e' pubblica il nome puo' restare, il codice no).
        // Contenuto: una sola voce, richiesta da Marco usando la 1.3.1 sul
        // tablet — "sarebbe bello che anche sui telefoni con visualizzazione ad
        // albero ci fosse un modo per andare dritto al nuovo messaggio non
        // letto". Realizzata come OPZIONE NUOVA e separata ("un'opzione in piu'
        // che si puo' anche NON attivare non fa mai male"), perche'
        // tapOpToExpand e' legata alla riga richiudibile della vista
        // raggruppata (total > 1), che nella vista ad albero non esiste: non e'
        // scelta di gusto, riusarla non avrebbe potuto funzionare.
        // Nota tecnica: prima di aggiungere il QUINTO punto che calcola "primo
        // non letto della discussione" la logica e' stata estratta una volta
        // sola in ThreadFlatten (radiceDi / membriDelThread / primoNonLetto).
        // La 1.3 e' uscita con un difetto proprio perche' una correzione era
        // stata applicata a due copie su quattro di un blocco duplicato.
        // 1.3.2, secondo giro (9 settembre 2026): nota tecnica di Dave Royal
        // sullo User-Agent. versionName INVARIATO perche' la 1.3.2 non e'
        // ancora stata pubblicata (l'APK non esiste), versionCode alzato
        // perche' e' monotono e non si riusa. Stesso criterio della 1.3.1.
        // 1.3.2, terzo giro (9 settembre 2026): il crash-test di BIG Umberto su
        // alt.test.a. Cancellato l'ULTIMO articolo di un gruppo, il water mark
        // alto non scende e NeReO chiedeva un intervallo overview vuoto,
        // trattando il 423 (risposta normale, RFC 3977 §8.3) come errore
        // fatale. Nome invariato: la 1.3.2 non e' ancora pubblicata.
        // 1.3.2, quarto giro (9 settembre 2026): solo TESTO, nessun cambio di
        // comportamento. La nota sotto "Tocca il messaggio per espandere il
        // thread" ora dice DOVE agisce: nel layout a pagina singola solo nella
        // vista raggruppata. Nasce dall'obiezione di cercaparole, che aveva
        // dedotto dal comportamento che sul telefono l'opzione "fa una cosa
        // inutile" — in realta' nell'albero non ne fa nessuna, perche' il ramo
        // e' agganciato a total>1 e treeList non popola total. Se un lettore
        // attento ci si sbaglia, il difetto non e' nella sua attenzione: e'
        // che la nota non lo diceva.
        //
        // Perche' NON si e' fatta la sua proposta (riusare quell'opzione sul
        // telefono cambiandone l'azione): "telefono" e "tablet" in NeReO non
        // esistono. Il layout si sceglie su maxWidth >= 600.dp — un telefono
        // in ORIZZONTALE e' gia' dalla parte larga — e paneMode e' forzabile
        // dall'utente. L'interruttore cambierebbe significato ruotando il
        // dispositivo in mano. Vedi NeReO_1.3.2_primo_non_letto_telefono.md.
        // 1.3.3 (10 settembre 2026): la 1.3.2 E' STATA PUBBLICATA, quindi il
        // nome cambia — e' l'altra meta' della regola. Contenuto: solo
        // chiarezza, nessun cambio di comportamento TLS.
        // Segnalazione di Bingo3331: su Paganini l'opzione "consenti
        // certificati non verificabili" non faceva passare la connessione, e
        // lui ne ha dedotto che fosse rotta. Verificato nel codice: NON e'
        // rotta, disattiva solo endpointIdentificationAlgorithm (il NOME
        // HOST). La catena di firma la valida il TrustManager dentro
        // SSLSocketFactory.getDefault(), che quell'opzione non sfiora: un
        // certificato autofirmato o scaduto viene rifiutato comunque.
        // Il difetto vero era NOSTRO: il messaggio d'errore suggeriva quella
        // opzione per QUALUNQUE SSLException, catena compresa, cioe' mandava
        // l'utente a cercare un rimedio inesistente. Ora l'errore viene
        // classificato (catena / nome host / indeterminato) in un solo punto
        // condiviso dai due percorsi cifrati, e l'opzione e' rinominata con
        // quello che fa davvero.
        // 1.3.3, secondo giro (10 settembre 2026): il video di Roxy chiude la
        // questione della barra copia/incolla. MISURATO: la selezione del
        // testo funziona (il blocco si evidenzia e si estende col
        // trascinamento) ma la barra flottante non compare MAI - nemmeno per
        // un fotogramma in 22 secondi, con la selezione ferma al centro dello
        // schermo. Quindi showMenu non viene proprio chiamata: non e' un
        // problema di posizionamento del popup, come avevo ipotizzato.
        // Indizio del meccanismo: mancano anche le MANIGLIE di selezione, che
        // Compose disegna sul percorso a tocco. E' il percorso MOUSE, che non
        // alza la barra - e su Android il tasto destro e' BACK, quindi niente
        // menu contestuale nemmeno da li'.
        // Rimedio: azioni che NON passano dalla selezione. "Copia il
        // messaggio" nelle due barre di lettura, "Incolla" nella barra di
        // scrittura. Un clic, identiche a tocco e a puntatore.
        // 1.3.3, terzo giro (10 settembre 2026): CORREZIONE DI UNA FRASE
        // SBAGLIATA, piu' due conferme a device.
        // Avevo scritto nelle note e nei README che Roxy "aveva una selezione
        // con cui non poteva fare nulla". FALSO, e lo ha smentito lui:
        // "gli shortcut da tastiera funzionano perfettamente (CTRL+C)/(CTRL+V)".
        // La funzione che gli serviva ESISTEVA GIA': mancava l'informazione.
        // Le scorciatoie sono ora documentate nelle note e nei README.
        // Seconda conferma, da uno screenshot di Marco: col DITO, sul telefono,
        // maniglie E barra ci sono entrambe, e la barra e' posizionata bene.
        // Quindi la barra non e' rotta, e il "rischio noto" sul posizionamento
        // dichiarato dalla 1.3 e' chiuso: commento aggiornato in
        // SelectionToolbar.kt. Due dispositivi, due percorsi di input, segnali
        // coerenti in direzioni opposte: l'abduzione mouse-contro-tocco resta
        // tale, ma e' da trattare come acquisita.
        // NON e' stato fatto il dialogo "Copia testo...": avrebbe duplicato con
        // una finestra intera quello che due tasti gia' fanno.
        // 1.3.4 (10 settembre 2026): RELEASE TECNICA, da sola.
        // Nessuna modifica al codice dell'app rispetto alla 1.3.3: cambia
        // solo cio' su cui l'app poggia. Toolchain migrata con l'AGP Upgrade
        // Assistant (AGP 9.3.0, Gradle 9.5.0, Kotlin 2.2.10, KSP2) e Compose
        // dalla 1.7.6 alla 1.12 — venti mesi in un colpo.
        //
        // PERCHE' DA SOLA: e' la regola della 1.2.14, applicata a una
        // migrazione invece che a una correzione. Due anni di Compose toccano
        // rendering, scroll, testo, IME, animazioni: TUTTA l'app. Se dopo
        // questa qualcosa va storto, dev'esserci un solo sospettato — e non
        // le cinque voci di contenuto della 1.3.3.
        //
        // PERCHE' SI FA: le note di Compose 1.12 contengono lavori espliciti
        // sulla selezione col mouse e sulla barra di selezione. E' l'unico
        // modo per sapere se il caso di Roxy sia gia' risolto a monte.
        // PREVISIONE, non certezza: se la barra col puntatore compare, ottimo;
        // se non compare, sapremo che quella strada non bastava — e il debito
        // di venti mesi l'avremo tolto comunque, che vale a prescindere.
        //
        // Il passo intermedio (toolchain nuova + Compose vecchia) e' stato
        // compilato e provato da Marco: APK verificato, AGP 9.3.0 con
        // Compose 1.7.6, tutto normale. AGP e Gradle sono gia' assolti.
        // 1.3.4, secondo giro (10 settembre 2026): la RICADUTA della
        // migrazione, trovata dal collaudo di Marco. In lettura il menu di
        // selezione mostrava Copia/Seleziona tutto/Leggi schermo — cioe'
        // quello di SISTEMA — invece del nostro con Cerca e Condividi.
        // CAUSA (verificata nel sorgente androidx, non dedotta): Compose 1.12
        // non passa piu' da LocalTextToolbar, il gancio che la 1.2.8 aveva
        // sostituito per la voce #5 di Dave Royal. Il nostro
        // SelectionToolbar.kt non era rotto: era diventato INERTE, e il suo
        // commento continuava ad affermare che funzionava. Cancellato.
        // NON RICOSTRUIBILE per ora: la nuova API
        // (appendTextContextMenuComponents / TextContextMenuBuilderScope.item)
        // permette di AGGIUNGERE voci al menu di piattaforma, ma
        // TextContextMenuSession espone solo close() e nulla nel pacchetto
        // contextmenu dice quale testo sia selezionato. Senza il testo,
        // "Cerca" e "Condividi" non sanno cosa cercare ne' cosa condividere.
        // Verificato anche a device che rememberSelectionState() non offre
        // membri utili in questa versione ("No suggestions" nell'IDE).
        // versionName INVARIATO: la 1.3.4 non e' ancora pubblicata.
        //
        // 1.3.5 (10 settembre 2026): i messaggi d'errore cablati nel livello
        // di rete passano dall'ITALIANO all'INGLESE. Nasce da un riscontro
        // reale: un utente inglese (Xiaomi M2103K19K, Android 13, NeReO
        // 1.3.2) con l'interfaccia in inglese ha ricevuto in ITALIANO il 483
        // ("Connessione non cifrata...") e il 481. Non era una traduzione
        // dimenticata: NntpClient e Repository sono senza Context e non
        // possono leggere le risorse, quindi quei testi non passano da
        // strings.xml e nessuna lingua di sistema li tocca.
        // Piano C dei tre valutati (A: vocabolario iniettato; B: solo codici
        // tipizzati tradotti nella UI; C: cambiare la lingua del cablato).
        // C non introduce debito: le stesse frasi che A prendera' in mano un
        // giorno, intanto smettono di parlare italiano a chi non lo capisce.
        // versionName CAMBIA: la 1.3.4 va pubblicata, quindi il nome e'
        // bruciato (due APK diversi non possono chiamarsi uguale).
        //
        // versionCode 84 (11 settembre 2026): alla 1.3.5 si aggiunge il
        // pacchetto CREDENZIALI, nato dal log NNTP dello stesso utente
        // inglese che aveva motivato il cambio di lingua. Il suo 481 su
        // Solani non era nostro — il server ha ricevuto e rifiutato — ma
        // guardando il codice e' emerso che salva() ripuliva l'host con
        // trim() e NON il nome utente. Ora lo ripulisce; la password no, di
        // proposito. Piu' l'occhio sul campo password (chiesto da lui), il
        // conteggio caratteri (che coglie lo spazio invisibile che l'occhio
        // non mostra) e la FORMA delle credenziali nel log diagnostico.
        //
        // versionCode 85 (11 settembre 2026): quattro aggiunte, scelte con un
        // criterio solo — nessuna di esse puo' NASCONDERE un difetto delle
        // altre, quindi possono viaggiare insieme.
        //  1. i preset dei tre server con account nascono su 563 cifrato
        //     (ServerPresets.kt): l'unica con un effetto visibile, e non puo'
        //     toccare un server gia' configurato;
        //  2. rememberTooltipPositionProvider al posto della versione
        //     deprecata (ArticleScreen);
        //  3. kotlinOptions -> kotlin { compilerOptions }: stesso jvmTarget 17,
        //     verificabile nel bytecode dell'APK;
        //  4. appunti estratti in ui/Appunti.kt — STESSA API di prima, un solo
        //     punto invece di sei. Nessun cambio di comportamento.
        // Restano fuori di proposito: fallbackToDestructiveMigration (tocca il
        // database, va con la migrazione Room 11->12 della 1.4) e i sette flag
        // android.* di gradle.properties (ognuno commuta un comportamento: uno
        // alla volta, con una prova in mezzo).
        // versionName INVARIATO: la 1.3.5 non e' ancora pubblicata.
        //
        // versionCode 86 (13 settembre 2026): TRE VOCI, e le prime due sono
        // lo stesso difetto visto da due lati.
        //  1+2. "apri il primo non letto" tornava al post iniziale gia' letto,
        //     e "marca la discussione letta" ogni tanto non la spegneva.
        //     Causa unica: l'app rispondeva alla domanda "quali messaggi stanno
        //     in questa discussione" in TRE modi — il threader (che disegna lo
        //     schermo), membriDelThread e threadIdsOf (duplicata in due
        //     schermate), questi ultimi due a confronto di stringhe sulle
        //     References. Divergono quando il post iniziale non e' mai stato
        //     scaricato o quando un messaggio elenca il solo padre diretto.
        //     Ora c'e' una fonte sola: mappaDiscussioni(), che chiede al
        //     THREADER. Vedi la nota lunga in ThreadFlatten.kt.
        //     E' la lezione della 1.3 applicata una seconda volta, dopo che la
        //     prima si era fermata a meta' (1.3.2: quattro copie ridotte a una,
        //     ma threadIdsOf non era stata contata).
        //  3. la firma puo' avere righe vuote fra le righe di testo: il limite
        //     di quattro conta ora le sole righe CON TESTO, con un tetto fisico
        //     a otto perche' non diventi spazio vuoto. Richiesta di un amico di
        //     Marco. Allentamento dichiarato della netiquette, non uno standard.
        // versionName INVARIATO: la 1.3.5 non e' ancora pubblicata.
        //
        // versionCode 87 (13 settembre 2026): la 86 NON COMPILAVA. Sostituendo
        // il blocco delle funzioni condivise in ThreadFlatten.kt avevo
        // cancellato per sbaglio `radiceDi`, che ThreePaneHome chiama in due
        // punti per la chiave di expandedRoots. La suite di verifica di allora
        // non se n'e' accorta: guardava parentesi, XML, chiavi delle stringhe e
        // i membri dei tipi NUOVI, non che ogni funzione CHIAMATA esistesse
        // ancora. L'ha trovata il compilatore sulla macchina di Marco.
        // Ripristinata, e aggiunto alla routine un controllo che confronta i
        // simboli con la release precedente: se qualcosa e' sparito e qualcuno
        // lo usa ancora, si vede qui e non a compilazione fallita.
        //
        // 1.3.6 (14 settembre 2026): due DIFETTI DI CONFORMITA' e una funzione
        // che mancava, tutti nati dalle domande di Adam H. Kerman su un gruppo
        // anglofono. Escono prima della 1.4 perche' riguardano cio' che NeReO
        // mette SULLA RETE — quello che finisce negli archivi altrui non si
        // corregge dopo — e perche' non dipendono da niente della 1.4: zero
        // database, zero TLS.
        //  1. encodeWord produceva UNA encoded-word senza limite: un oggetto
        //     italiano normale usciva a 140 caratteri contro i 75 della
        //     RFC 2047 §2. Ora si spezza e si piega, tagliando su confini di
        //     code point (le coppie surrogate lo esigono).
        //  2. Content-Type era sempre "UTF-8; 8bit" anche per un corpo di puro
        //     ASCII. Ora si dichiara us-ascii/7bit quando e' il caso, e il
        //     controllo sta DOPO che la firma e' entrata nel corpo.
        //  3. Esporta/condividi articolo: non e' mai esistita fuori dalla barra
        //     di selezione, morta con SelectionToolbar.kt nella 1.3.4.
        // versionName CAMBIA: la 1.3.5 e' pubblica.
        //
        // versionCode 89 (14 settembre 2026): UNA RIGA, e non cambia niente di
        // cio' che l'utente vede. La 88 compilava con due soli avvisi Kotlin,
        // tutti e due su un unico punto di ArticleScreen.kt: la 1.3.5 aveva
        // sostituito rememberPlainTooltipPositionProvider() (deprecata) con
        // rememberTooltipPositionProvider(), che nell'overload SENZA argomenti
        // e' deprecata a sua volta. Ora si passa TooltipAnchorPosition.Above,
        // che la deprecazione stessa indica come equivalente esatto.
        // versionName INVARIATO: la 1.3.6 non e' ancora pubblicata, l'APK 88
        // non e' uscito dalla macchina di Marco. versionCode alzato lo stesso,
        // perche' e' monotono e non si riusa MAI — ed e' l'unico modo per
        // distinguere a colpo d'occhio, in "Informazioni", il binario con la
        // correzione da quello senza (col parco macchine doppio, desktop e
        // portatile, la copia vecchia del progetto e' un rischio reale).
        //
        // versionCode 90 (14 settembre 2026): richiesta di ObiWan, accolta
        // dentro la 1.3.6 perche' la 1.3.6 non e' ancora pubblica — una
        // funzione in piu' in una release non uscita non e' una release in
        // piu'. Due azioni di barra: "link" copia l'URL permanente
        // dell'articolo (https://usenet.id/<message-id>), "msgid" il
        // Message-ID nella forma canonica <...>, che finora non si poteva
        // ottenere in nessun modo se non aprendo le intestazioni e
        // selezionandolo a mano. NeReO non contatta usenet.id: compone una
        // stringa. Vedi ui/LinkArticolo.kt (provata in CoreTests: 8 casi,
        // compresi i Message-ID con / ? # % dentro).
        // Corretta nella stessa occasione una dimenticanza della 89:
        // l'azione "esporta" non era stata registrata in CustomizeBarsScreen,
        // quindi nell'editor delle barre compariva con l'icona di ripiego e
        // con l'id nudo al posto dell'etichetta.
        // versionName INVARIATO: la 1.3.6 non e' ancora pubblicata.
        //
        // versionCode 91 (14 settembre 2026): CORREZIONE DI UN ERRORE MIO
        // della 1.3.5. Avevo spostato OVH su 563/SSL insieme a Eternal
        // September e Solani, trattando i tre server "con account" come un
        // gruppo omogeneo — ma NeReO_ovh_starttls_in_chiaro.md, nato dal log
        // di Marco per la 1.2.17, diceva che OVH non offre TLS su NESSUNA
        // porta. Chi sceglieva OVH dall'elenco riceveva una configurazione
        // impossibile: lo stesso difetto che la 1.3.5 doveva correggere,
        // ripresentato dall'altro lato.
        // Il 14 settembre l'amministratore ha abilitato STARTTLS sulla 119, e
        // Marco ha MISURATO che si collega. Il preset ora dice 119 +
        // STARTTLS, e ServerPreset ha il campo che prima gli mancava per
        // poterlo dire.
        // versionName INVARIATO: la 1.3.6 non e' ancora pubblicata.
        //
        // 1.3.7 (15 settembre 2026): due segnalazioni di Dave Royal sul
        // RIPRISTINO DEL BACKUP, una giusta e una da accertare.
        //
        // QUELLA GIUSTA: il ripristino DUPLICAVA i filtri. Verificato, ed era
        // peggio: duplicava anche le IDENTITA'. Ma il difetto vero non era
        // "aggiunge invece di sostituire" — era che nella stessa operazione
        // l'app faceva TRE cose diverse (dedup su server e sottoscrizioni,
        // inserimento cieco su filtri e identita', sovrascrittura sulle
        // impostazioni). Ora il ripristino e' IDEMPOTENTE e CHIEDE se unire o
        // sostituire, dichiarando il perimetro prima: filtri e identita' si',
        // server, sottoscrizioni e articoli mai (cancellare un server porta
        // via in cascata l'intera cache locale).
        // Terza correzione, trovata li' accanto: il dialogo della password
        // decifrava E importava nello stesso runCatching, quindi un errore di
        // IMPORT veniva annunciato come "password errata".
        //
        // L'ALTRA, da accertare: "non riesco a ripristinare un backup cifrato
        // se il file e' stato rinominato". Il sintomo e' suo, la causa che
        // ipotizza no: BackupCrypto deriva la chiave da password + salt
        // CASUALE contenuto nell'envelope, AES-GCM senza AAD, e il nome del
        // file non arriva mai ne' a isEncrypted ne' a decrypt. Nessuna
        // correzione inventata finche' non arriva il dato che manca (quale
        // errore vedeva). Vedi NeReO_bug_ripristino_backup.md.
        //
        // versionName CAMBIA, e NON e' una scelta: la 1.3.6 E' STATA
        // PUBBLICATA il 14 settembre 2026. Il nome e' bruciato, due APK
        // diversi non possono chiamarsi uguale una volta usciti — la meta'
        // vincolante della regola dei due numeri. Le voci della 1.3.6 restano
        // nelle note come sezione a se', che ora e' anche l'unica forma
        // corretta: sono una release pubblica, non un giro interno.
        //
        // CORREZIONE (15 settembre 2026): fino a poco fa questo commento
        // diceva che la 1.3.6 "non e' mai stata pubblicata". FALSO, e me lo ha
        // corretto Marco. Era l'unico presupposto su cui poggiava la scelta
        // del nome; la scelta resta giusta, la ragione era sbagliata. Un
        // commento che mente e' un commento che il prossimo lettore crede.
        //
        // versionCode 93 (15 settembre 2026): la 92 non cambia di una riga di
        // comportamento rispetto alla 93 — cambiano due COMMENTI che
        // affermavano il falso. Si alza lo stesso, perche' la regola dice
        // "monotono, mai riusato, anche per un cambio di sola virgola nei
        // commenti", e perche' non so se Marco abbia gia' compilato la 92.
        //
        // versionCode 94 (15 settembre 2026): l'A CAPO IN INVIO non lascia piu'
        // uno spazio in coda alle righe che spezza.
        // Domanda di Adam H. Kerman: uno spazio a fine riga e' il marcatore di
        // rottura DEBOLE del Format=Flowed (RFC 3676) — un lettore che conosce
        // f=f riunirebbe la riga con la successiva, disfacendo l'a-capo.
        // Misurato riproducendo wrapBody riga per riga: nel caso normale lo
        // spazio non c'e', ma con un DOPPIO SPAZIO nel punto di rottura
        // split(" ") produce un elemento vuoto che si porta dietro il proprio
        // separatore. Il rimedio NON e' scartare gli elementi vuoti (che
        // collasserebbe i doppi spazi dentro la riga, cioe' riscriverebbe il
        // testo dell'utente): si ripulisce la coda delle sole righe che
        // spezziamo noi. Sei prove nuove in CoreTests.
        //
        // versionCode 95 (17 settembre 2026): "File illeggibile" smette di
        // nascondere la causa. Dave Royal ha ripetuto la prova SEI volte su due
        // versioni dell'app senza poterci dire nulla di piu', perche' nulla di
        // piu' gli mostravamo: runCatching{...}.getOrNull() faceva collassare
        // FileNotFoundException, SecurityException e uno stream nullo nella
        // stessa frase. Ora il dettaglio compare in un dialogo (non in uno
        // snackbar: si troncherebbe) con un pulsante per copiarlo.
        // Terza volta che incontriamo questa forma di difetto: il messaggio TLS
        // della 1.3.3 che proponeva un rimedio inesistente, e il "password
        // errata" corretto in questa stessa release.
        // La CAUSA del caso di Dave resta ignota: l'ipotesi (rename via MTP che
        // lascia una riga stantia in MediaStore su Samsung) e' un'abduzione, e
        // questa modifica serve proprio a sostituirla con un dato.
        //
        // 1.4 (17 settembre 2026): APERTA. Marco: "la 1.4 e' ora di farla!".
        // Contratto completo in NeReO_1.4_specifica.md — TOFU su SPKI,
        // eccezione certificato scaduto, STARTTLS rigoroso, migrazione Room
        // 11->12, Followup-To, introduzione per gruppo, backup v6. Piu' le tre
        // voci decise il 17 settembre (§10.2): correzione del charset del
        // corpo, avviso "certificato in scadenza", "letto" per Message-ID.
        //
        // versionCode 96: la specifica diceva 88, bruciato. Fra il 14 e il 17
        // settembre sono usciti OTTO build (88-91 come 1.3.6, 92-95 come
        // 1.3.7) che quando quel documento e' stato scritto non esistevano.
        //
        // PRIMA VOCE — STARTTLS: i due casi della RFC 4642 non sono lo stesso
        // caso. Rifiuto PRIMA dell'handshake: lecito proseguire in chiaro,
        // invariato. 382 seguito da handshake FALLITO: la RFC impone di
        // CHIUDERE, e fino alla 1.3.7 proseguivamo in chiaro. Ora connect()
        // fallisce con un messaggio esplicito.
        // Questa build contiene SOLO quella modifica, di proposito: tocca il
        // percorso di connessione, e la regola della 1.2.14 vuole un solo
        // sospettato possibile. Il collaudo che conta e' il §9 della
        // specifica: i server che oggi funzionano devono continuare a
        // funzionare.
        //
        // versionCode 97 (17 settembre 2026) — MIGRAZIONE ROOM 11 -> 12, DA
        // SOLA. Tre modifiche allo schema in un colpo (tabella `tls_pins`,
        // colonna `servers.eccezioneScadenza`, colonna `groups.replyIntro`) e
        // NESSUN lettore: nessuna delle tre e' ancora usata da una riga di
        // codice, ed e' voluto.
        // Se i dati sopravvivono, la migrazione e' assolta e le funzioni che
        // useranno quelle colonne arrivano dopo, una alla volta, con un solo
        // sospettato ciascuna. E' la regola della 1.2.14 applicata al
        // database invece che alla connessione — e qui vale doppio: una
        // migrazione che va storta non costa un difetto, costa i dati di
        // qualcuno.
        // Il collaudo NON e' "si apre": e' aggiornare da una 1.3.7 con dati
        // REALI, su una COPIA, e contare server, gruppi, articoli, filtri,
        // identita' e bozze prima e dopo.
        // 98 (17 settembre 2026) — IL RIPRISTINO INVENTAVA UNO USERNAME.
        // Trovato in un log NNTP di Marco: Paganini, che e' un server aperto,
        // mandava "AUTHINFO USER null" e poi una password vuota, prendendosi un
        // 501 di sintassi. La parola "null" nasceva in lettura del backup:
        // esportiamo lo username assente come null JSON, e optString di Android
        // su JSONObject.NULL restituisce la stringa "null", non il fallback.
        // Era l'unico dei sette campi esportati come NULL letto senza la
        // guardia isNull, e stava li' almeno dalla 1.2.8.
        // Correzione (piano B): guardia in lettura, riparazione delle righe gia'
        // sporcate al primo ripristino, e rifiuto esplicito di mandare
        // AUTHINFO PASS vuota (nel ramo del 381: la RFC 4643 §2.3.1 prevede il
        // caso legittimo del solo nome utente, che risponde 281).
        // versionName INVARIATO: la 1.4 non e' ancora pubblicata.
        // 99 (17 settembre 2026) — DIAGNOSTICA DATABASE in Manutenzione.
        // Primo dei tre passi del blocco TrustManager (scelta "A" di Marco):
        // prima lo STRUMENTO DI MISURA, poi il TrustManager da solo, poi la
        // riparazione una-tantum all'avvio. Questa build e' INERTE: dieci
        // COUNT(*) e la lettura di due lunghezze di file, eseguiti solo quando
        // l'utente preme il pulsante. Non scrive niente e non tocca la rete.
        // Serve a rendere MISURA quello che sulla migrazione 11->12 e' stato
        // finora una deduzione dalla forma della migrazione.
        // versionName INVARIATO: la 1.4 non e' ancora pubblicata.
        // 100 (17 settembre 2026) — DUE DIFETTI SEGNALATI, PRIMA DEL TLS.
        // Scelta "A" di Marco: i difetti su funzioni che si usano ogni giorno
        // passano davanti alle funzioni nuove. Consegnati insieme perche' Marco
        // ha chiesto due sole compilazioni, ma sono due interventi distinti e
        // lo storico li tiene separati.
        //
        //  (1) Dave Royal — la risposta crosspostata non diceva dove andava.
        //      Tre difetti: la schermata mostrava UN gruppo su tre; riaprire il
        //      selettore azzerava la scelta e tornava al default; nella vista a
        //      pannelli l'azione non esisteva proprio.
        //  (2) Bingo3331 — "tutti i messaggi" su un server ad alta ritenzione
        //      sbatteva contro il nostro tetto di 64 MB (MAX_BLOCCO_BYTE) e non
        //      scaricava nulla. L'overview ora si chiede a fette da 10.000
        //      numeri, con il segnalibro che avanza a ogni fetta. Il tetto resta
        //      dov'e': alzarlo sarebbe stato un rinvio, non una correzione.
        // versionName INVARIATO: la 1.4 non e' ancora pubblicata.
        // 101 (17 settembre 2026) — IL BLOCCO TLS E IL RESTO DELLA 1.4.
        // Seconda e ultima compilazione del programma concordato con Marco
        // ("applica tutte le modifiche da 1 a 10, teniamo da parte solo la 11").
        // Dentro ci sono, in ordine di rischio:
        //   - TrustManager delegante + TOFU su SPKI (PENDING/TRUSTED), eccezione
        //     "certificato scaduto" per riga server, schermata "Identita' TLS"
        //     con sonda dedicata, avviso "scade fra N giorni";
        //   - correzione del charset del corpo (3 passi) + azione "Ricarica il
        //     messaggio", senza la quale la prima sarebbe invisibile;
        //   - "letto" che segue il Message-ID (predefinito acceso, come MesNews);
        //   - Followup-To letto e rispettato (l'unico criterio GNKSA che
        //     mancava), con avviso e caso "poster";
        //   - introduzione alla citazione per gruppo (Henry The Mole);
        //   - riparazione una tantum all'avvio degli username "null".
        // FUORI: backup v6 (voce 11), tenuta da parte da Marco.
        // versionName INVARIATO: la 1.4 non e' ancora pubblicata.
        // 102 (17 settembre 2026) — PENDING NON E' LO STATO NORMALE.
        // Rilievo di Marco sulla 101, prima ancora di compilarla: Paganini dopo
        // il rinnovo passa la validazione normale, e nella 101 ogni identita'
        // osservata diventava comunque PENDING. Tutti i suoi server sarebbero
        // comparsi come "VISTA, NON FIDATA" senza niente da approvare: un
        // allarme falso, cioe' addestramento a cliccare senza leggere — proprio
        // cio' che il disegno del TOFU cerca di evitare.
        // Terzo stato VALIDA (la colonna e' TEXT: nessuna migrazione), etichetta
        // e testo dedicati, e il pulsante che su un server regolare diventa
        // un'OFFERTA ("Appunta comunque") invece di una richiesta.
        // versionCode nuovo e non riusato anche se la 101 non e' stata
        // pubblicata: e' la regola dei due numeri, senza eccezioni.
        // 103 (17 settembre 2026) — LA 102 NON COMPILAVA. Tre errori miei:
        //  - clientCorpiPer era rimasta NON sospendibile mentre nuovoClient e
        //    annotaIdentitaTls, che chiama, lo erano diventate (leggono e
        //    scrivono il pin TLS sul database);
        //  - MainActivity usava TlsIdentityScreen senza importarla: e' in un
        //    altro package, ed e' il difetto tipico del FILE NUOVO.
        // La suite non poteva vederli — guardava parentesi, XML, chiavi delle
        // stringhe e simboli SPARITI. Da questa build ci sono due controlli in
        // piu', entrambi validati sul guasto vero prima di fidarsene:
        // controllo_sospensione.py e controllo_import.py.
        // 1.4.1 (Marco, 20 settembre 2026) — PRIMA BUILD (104) della release di
        // correzioni che segue la 1.4 pubblica. versionName CAMBIA per forza: la
        // 1.4 e' su SourceForge, e due APK diversi non possono chiamarsi uguale.
        // Questa build contiene UNA SOLA modifica di comportamento — l'unico
        // punto che marca "letto" (segnaLetti in Repository), da cui dipende "il
        // letto segue il Message-ID" segnalato da Dave Royal — piu' due righe di
        // log inerti per la segnalazione della lista che smette di aggiornarsi.
        // Un solo sospettato per build: regola della 1.2.14.
        // Build 105: le undici correzioni locali del piano, piu' il ritorno al
        // comportamento previsto per markAllUnread (la 104 ne aveva fatto
        // un'eccezione, e l'eccezione non stava in piedi: l'ANNULLA di "segna
        // tutto letto" propagava gia'). versionName invariato: la 1.4.1 non e'
        // ancora pubblica, quindi il nome non e' bruciato.
        // Build 106: l'Incolla in scrittura. La 105 rendeva l'icona sempre
        // visibile, ma non sarebbe bastato: NeReO leggeva gli appunti mentre
        // DISEGNAVA la barra, e da Android 10 la lettura richiede il focus di
        // input (la scrittura no). Due misure di Roxy lo hanno provato. Ora si
        // legge alla pressione, e con coerceToText. La 105 non e' mai stata
        // compilata, ma il versionCode non si riusa comunque (regola del par. 1,
        // come la 101 il 17 settembre).
        // Build 107: le ultime due voci del piano, entrambe di Dave Royal.
        // L'avviso sulle intestazioni riservate scartate (105/G) — la regola
        // SPOSTATA in IntestazioniPersonalizzate, cosi' l'editor e l'invio
        // leggono la stessa riga di codice — e lo scambio fra "segna tutto
        // letto" e "aggiorna" nella barra (Dave 08). Piu' la correzione del
        // testo su RFC 4642, che dice SHOULD dove le nostre note dicevano
        // "impone". versionName invariato: la 1.4.1 non e' ancora pubblica.
        // 1.4.5 (29 settembre 2026, versionCode 122) — «SEGUI LA DISCUSSIONE»
        // SEGUIVA SOLO IL PASSATO.
        //
        // IL versionName NON CAMBIA, ED E' UNA CORREZIONE A UN MIO ERRORE.
        // Questa build era stata preparata come "1.4.6", trattando la consegna
        // della 1.4.5/121 a Dave Royal come se fosse una pubblicazione. Non lo
        // era: il par. 1 della checklist lega il cambio di versionName alla
        // PUBBLICAZIONE SU SOURCEFORGE, non alla consegna a un collaudatore.
        // La 121 non e' mai uscita, quindi il nome 1.4.5 e' ancora libero e la
        // 122 se lo tiene. Marco: «se la chiamassi 1.4.6 poi avrei un buco su
        // SourceForge, che vorrei evitare».
        //
        // *La regola dice "pubblicata", e pubblicata vuol dire una cosa sola.
        // Averla letta come "consegnata" e' lo stesso errore del par. 5 — cosa
        // ha in mano chi — visto dal lato del nome invece che del numero.*
        //
        // La 122 esce quindi con le QUATTRO voci della 121 (mai uscite) piu' le
        // due di questo giro: sei in tutto, in una sezione sola.
        //
        // BIG Umberto: «cercando un post a mano ho notato che alcune stelle
        // mancano, solo questione estetica. Ma penso sia fatto apposta per non
        // degradare la lettura del soggetto, correggimi se sbaglio.»
        //
        // Sbagliava su tutti e tre i punti, e la parte che conta e' la terza.
        //
        // LA DIAGNOSI E' STATA SBAGLIATA UNA VOLTA, E VA REGISTRATO PERCHE'.
        // La prima spiegazione — verificata nel codice — diceva che la stella
        // sta sul MESSAGGIO mentre la riga chiusa mostra la RADICE, quindi una
        // stella messa su una risposta non si vede finche' non apri. E' un caso
        // reale. Non era il suo: lo screenshot di Marco mostra la discussione
        // APERTA, tutti i messaggi visibili, e le stelle che si fermano a una
        // data. Il codice diceva una cosa vera, io guardavo il pezzo che non
        // c'entrava. *Una diagnosi si prova contro cio' che l'utente vede, non
        // contro cio' che il codice potrebbe fare.*
        //
        // LA CAUSA VERA. `setWatchedBulk` e' un UPDATE sui messaggi PRESENTI
        // quando premi «segui». Al sync il flag nasceva solo da una regola dei
        // filtri con azione WATCH. Nessuna eredita' lungo le References: le
        // risposte che arrivavano dopo nascevano non seguite.
        //
        // E NON E' ESTETICA. `watched` protegge da tre cose: il filtro sul
        // lettore di news (applicaRegoleLettore), la propagazione dei filtri
        // alle risposte (propagaAlleRisposte) e la CONSERVAZIONE, che tiene i
        // seguiti e cancella gli altri. Quindi in una discussione seguita una
        // risposta arrivata dopo poteva essere nascosta da un filtro o
        // cancellata dalla retention mentre le sorelle piu' vecchie restavano:
        // la discussione si svuotava dal centro.
        //
        // LA FORMA DELLA CORREZIONE, e non e' quella che sembrava gratis.
        // Accanto c'e' gia' `replyToMe`, che per ogni articolo nuovo carica il
        // GENITORE DIRETTO: riusarlo costava zero query. Scartato per due
        // motivi verificati: (1) il genitore diretto si spezza se un anello
        // intermedio manca — mai scaricato, o tolto dalla conservazione — e
        // l'header References porta invece la catena INTERA, quindi un
        // pronipote trova la radice seguita da se'; (2) quel blocco e' saltato
        // quando l'utente non ha un'email configurata, e l'eredita' del seguito
        // non puo' dipendere da quello.
        //
        // Si guardano percio' TUTTE le References, che e' esattamente la regola
        // che il filtro «solo discussioni seguite» usa da sempre
        // (`inThreadSeguito`). Flag e filtro smettono di contraddirsi PER
        // COSTRUZIONE, non per manutenzione.
        //
        // COSTO: una query a blocco da 500 id (limite variabili SQLite, stessa
        // misura di `contaPerGruppi`), sull'indice di `messageId`, e solo sugli
        // id che il lotto nomina. MENO di quanto costi `replyToMe`, che
        // interroga una volta per articolo. Nessuna build di misura: qui il
        // costo non era un'incognita.
        //
        // SICUREZZA: gli id si leggono con `Threader.idNelleReferences`, nata
        // in questo giro per non ricopiare `Regex("<[^>]+>")` in un quinto
        // posto. Quella copia sarebbe stata sbagliata due volte — e' piu' larga
        // (accetta spazi e < annidati) e soprattutto NON TAGLIA: il tetto di
        // 100 dell'audit 1.2.12 esiste perche' un References da ~400 KB e' un
        // vettore reale, e adesso decide anche quanti parametri finiscono in una
        // query SQL. *Un limite di sicurezza vale quanto il posto peggiore in
        // cui e' scritto.* Il taglio, che era scritto due volte, ora e' uno.
        //
        // LIMITE DICHIARATO NELLE NOTE: vale per i messaggi FUTURI. Quelli gia'
        // in archivio senza flag restano tali; rimetterli in pari e' una
        // scrittura di massa sul database e si decide a parte. Rimedio manuale:
        // togliere e rimettere il «segui».
        //
        // RICONOSCIMENTO A DAVE ROYAL nei testi che escono (README, note,
        // cap. 7 dello storico). Non e' cortesia: dalla 1.4 in poi la maggior
        // parte delle voci porta il suo nome, e tre volte ha corretto noi.
        //
        // Il 29 settembre ha confermato sul campo le due voci della 1.4.5 che
        // erano sue: «121 fixes item 1 - reply to all groups by default in
        // phone mode» e «& the 'mark all read' icon in panel mode».
        //
        // 1.4.5 (28 settembre 2026, versionCode 121) — QUATTRO VOCI, DUE DELLE
        // QUALI ERANO RIMASTE INDIETRO.
        //
        // IL versionName CAMBIA, E NON E' UNA SCELTA: la 1.4.4 versionCode 120
        // e' PUBBLICA su SourceForge dal 28 settembre. Due APK diversi non
        // possono chiamarsi uguale (par. 1 della checklist). Chiavi rn_v145_*
        // verificate libere.
        //
        // COME SONO NATE. Marco ha chiesto "cosa e' rimasto indietro", e la
        // risposta non stava nel quaderno del da fare: stava nelle cose che il
        // quaderno NON diceva. Due richieste di Dave Royal erano state
        // parcheggiate "in wishlist, 1.5" prima che quella wishlist venisse
        // eliminata e sostituita dal quaderno, e nel trasloco non erano state
        // trasportate. Nove giorni di silenzio, e non se n'era accorto nessuno.
        // *E' lo stesso difetto del codice: cio' che si sposta si SPOSTA, e
        // quello che resta indietro non lo segnala nessuno.*
        //
        // 1. «SEGNA TUTTO LETTO» TORNA IN BARRA NEI PANNELLI (par. A11, Dave
        // Royal). Sue parole: «"Mark all as read" requires two taps versus one
        // previously». E' la stessa persona che aveva chiesto la modifica, e
        // non e' una contraddizione: la prima segnalazione riguardava la
        // POSIZIONE (accanto al filtro dei non letti, si premeva per sbaglio),
        // la seconda il COSTO IN TOCCHI. Siamo stati noi a confonderle in una.
        //
        // IL DIFETTO VERO E' CHE LO STESSO PROBLEMA HA AVUTO DUE CURE DIVERSE.
        // Nella 108: sul telefono l'icona e' stata SPOSTATA lontano dal filtro
        // (un tocco), nei pannelli e' stata TOLTA perche' li' la voce c'era
        // anche nel menu (due tocchi). Nessuna delle due e' sbagliata presa da
        // sola, ma la cura del telefono risolveva l'adiacenza SENZA costare un
        // tocco, e nei pannelli non era mai stata provata.
        //
        // *Due copie che divergono, prima o poi si notano. Due copie curate in
        // modo DIVERSO sembrano entrambe corrette guardando il codice, e si
        // scoprono solo quando qualcuno conta i tocchi.* Nessuno degli otto
        // controlli automatici puo' prendere questa forma di debito.
        // Azione e snackbar con ANNULLA estratti in
        // ui/screens/AzioneSegnaTuttoLetto.kt: erano due copie parola per
        // parola. Quinto mattone di par. B4.
        //
        // 1-bis. IL NOME DEL GRUPPO CHE SI ADATTA. Dave, accettando la sesta
        // icona in una colonna stretta: «the group name should preferably be
        // abbreviated in the same way as in column 1 then truncated to fit
        // without line-wrap». La logica c'era gia' in colonna 1 ed e' sua
        // (beta12): nome intero finche' ci sta, poi abbreviato, poi ellissi.
        // Estratta in NomeGruppoAdattivo invece di ricopiarla — sarebbe stata
        // la DECIMA copia su due, nella release che ne chiude altre quattro.
        // La differenza fra le due colonne e' un parametro (`righe`), non una
        // funzione nuova.
        //
        // 2. IL CROSSPOST SUL TELEFONO (par. A10, Dave Royal, segnalazione #05
        // del 19 settembre). Era descritta come "la piu' invasiva delle sei,
        // cambia un flusso che l'utente conosce". ANDANDO A GUARDARE NON ERA
        // QUELLO. L'impostazione «rispondi sempre a tutti i gruppi» esiste
        // dalla 1.2.7 (Dave #8) ed era consultata in UN SOLO POSTO:
        // l'inizializzazione di CrosspostReplyDialog. La vista a pagine quel
        // dialogo non lo apre (beta17: si va dritti in scrittura), quindi
        // `extraGroups` nasceva vuoto e l'impostazione, pur accesa, non faceva
        // niente.
        //
        // Non serviva portare il dialogo sul telefono: serviva far leggere
        // l'impostazione anche a quel percorso. La regola e' uscita in
        // `extraPredefinitiCrosspost` — pura, quattro prove in CoreTests — e la
        // chiamano il dialogo e la schermata di scrittura. `crosspostScelto`
        // resta FALSO, perche' un valore di partenza non e' una scelta: un
        // Followup-To dell'autore continua a vincere, e riaprendo il selettore
        // si rivede lo stesso insieme invece di una lista congelata.
        //
        // Rendeva FALSA una riga delle note della 1.4 gia' pubblicata —
        // «rispondendo a un crosspost ora si vede dove il messaggio andra'
        // davvero» era vera solo a pannelli.
        //
        // 3. SALVA COME .eml (par. B9, Adam H. Kerman). L'altra meta' della sua
        // richiesta del 13 settembre: la 1.3.6 aveva dato la CONDIVISIONE
        // (testo a un'altra app), questa da' il FILE.
        //
        // E QUI C'ERA UNA TRAPPOLA. NeReO salva il corpo GIA' DECODIFICATO
        // (`setBody(id, decodeTextBodyRaw(...))`), mentre le intestazioni
        // restano grezze: possono ancora dire `charset=ISO-8859-15` e
        // `Content-Transfer-Encoding: quoted-printable` mentre sotto c'e'
        // Unicode. Condividere quel testo non fa danno, perche' nessuna
        // intestazione viaggia. SCRIVERLO IN UN FILE si: un client di posta
        // legge quelle righe, ci crede, e ri-decodifica un testo gia'
        // decodificato. Ogni accento diventa spazzatura e la colpa e' nostra,
        // perche' il file lo abbiamo scritto noi. `articoloEml` riscrive quelle
        // due intestazioni (UTF-8, 8bit) e porta via anche le loro
        // continuazioni ripiegate (RFC 5322 par. 2.2.3), perche' una riga
        // orfana rende illeggibile il blocco. Otto prove in CoreTests: un file
        // che finisce in mano a un altro programma non si collauda a occhio.
        //
        // Azione di barra "eml", registrata nei QUATTRO posti che
        // controllo_barre.py sorveglia. Il selettore SAF sta in
        // ui/EsportaEml.kt: un ActivityResultLauncher vive nella composizione,
        // e registrarlo in ogni schermata sarebbe stata l'ennesima copia.
        //
        // 4. %oggetto% / %subject% NELL'INTRODUZIONE ALLA CITAZIONE (par. B1
        // passo 1, Ammammata). Era il quinto dei cinque dati che chiedeva di
        // portarsi dietro in una citazione, e l'unico che mancava. Normalizzato
        // con `Threader.normalizeSubject`, la stessa funzione che decide
        // l'appartenenza al thread: senza, un'introduzione su una discussione
        // lunga direbbe «Re: Re: Re: Re: oggetto». Migliora ANCHE le risposte
        // normali, ed e' la ragione per cui questo passo viene prima degli
        // altri cinque del pacchetto citazioni.
        //
        // PULIZIA MINUTA: chiave `pane_mark_all_as_read` tolta dalle tre
        // cartelle (non piu' referenziata dopo il punto 1), sei import orfani
        // — uno dei quali, `CircularProgressIndicator` in ThreadListScreen, era
        // gia' orfano nella 120 e non lo aveva notato nessuno.
        //
        // Due file nuovi: 97 invece di 95.
        //
        // 1.4.4 (28 settembre 2026, versionCode 120) — DUE GRUPPI OMONIMI SU
        // DUE SERVER, E NIENTE CHE LI DISTINGUESSE.
        //
        // Roxy: nella vista a PANNELLI la colonna dei gruppi e' una lista unica
        // che non separa i server, mentre la vista a pagine li separa. Marco
        // aveva la stessa nota sul proprio taccuino, e ha aggiunto il pezzo che
        // mancava: il problema non finisce nella colonna, perche' una volta
        // ENTRATI nel gruppo non lo dice piu' niente, in nessuna delle due
        // viste.
        //
        // NON ERA SOLO UN'ETICHETTA MANCANTE. `GroupDao.allSubscribed()` —
        // l'unica query che alimenta quella colonna, consumatore unico
        // verificato — ordinava `ORDER BY sortOrder, name` SENZA `serverId`. Ma
        // `sortOrder` e' per server (`moveGroup(serverId, ...)`), quindi i
        // gruppi dei due server venivano INTERCALATI: il primo di A, il primo
        // di B, il secondo di A. I due omonimi di Roxy finivano adiacenti
        // perche' avevano lo stesso `sortOrder`. `allSubscribedWithTotals`, che
        // alimenta la vista a pagine, ordinava gia' per `serverId` dal primo
        // giorno: era questa a essere sbagliata, non quella.
        //
        // COSA E' STATO FATTO (piano B scelto da Marco).
        //  - la query ordina per `serverId`;
        //  - la colonna dei pannelli ha le sezioni per server;
        //  - `ThreadListScreen` e la colonna 2 dicono da quale server stai
        //    leggendo, sotto al nome del gruppo — e SOLO con piu' di un server,
        //    stesso criterio del menu "aggiorna" di §A7: con un server solo non
        //    c'e' niente da distinguere e una riga in piu' costerebbe altezza
        //    utile senza dire nulla.
        //
        // ESTRATTA, NON RICOPIATA. L'intestazione sarebbe stata la seconda
        // copia, cioe' il nono caso della serie — quella che la settimana
        // scorsa ha gia' prodotto una regressione. Sta in
        // ui/screens/IntestazioneServer.kt, con dentro anche il
        // raggruppamento, e le due viste la chiamano. Unico file nuovo: 95
        // invece di 94.
        //
        // DUE EFFETTI COLLATERALI, ed entrambi sono migliorie:
        //  - l'etichetta ora e' il NOME del server e non l'host. Con due
        //    account sullo stesso provider l'host e' identico e non avrebbe
        //    distinto niente, che e' esattamente il difetto da correggere.
        //    Ripiega sull'host se il nome e' vuoto.
        //  - i GRUPPI ORFANI (serverId che non corrisponde piu' a nessun
        //    server: tre ne aveva BIG Umberto) non spariscono piu' dalla lista.
        //    Un `servers.forEach` scritto a mano li perde, ed e' quello che la
        //    vista a pagine faceva da sempre; ora finiscono in coda sotto "?",
        //    senza menu perche' non c'e' niente da sincronizzare.
        //
        // versionName resta 1.4.4: non e' mai stata pubblicata su SourceForge.
        // versionCode 120 perche' il 119 e' stato consegnato.
        //
        // 1.4.4 (25 settembre 2026, versionCode 119) — IL MENU DEI TRE PUNTINI
        // SI APRIVA A UN PALMO DI DISTANZA.
        //
        // Dave Royal: «In general when you tap a 3-dot icon the menu appears
        // underneath where you tapped. The exception is the phone mode group
        // page which lists the threads, where it appears on the extreme left —
        // about 10cm away on this tablet. All the panel mode ones are OK.»
        //
        // LA CAUSA, e non e' un caso particolare di quella schermata.
        // `DropdownMenu` e' un `Popup`, e un `Popup` si posiziona sul nodo di
        // layout GENITORE (`childCoordinates.parentLayoutCoordinates`), non
        // sull'`IconButton` che gli sta accanto. Se la coppia icona+menu non ha
        // un contenitore proprio, il genitore e' il contenitore dell'intera
        // barra: in `ThreadListScreen` le azioni stanno in una `Row` con
        // `fillMaxWidth()` sotto la `TopAppBar`, quindi il menu si misurava
        // sulla riga intera e partiva dal bordo sinistro dello schermo. Nelle
        // colonne strette della vista a pannelli lo scarto e' di pochi
        // millimetri: lo stesso difetto, invisibile. E' il motivo per cui Dave
        // ha visto sbagliata solo quella pagina.
        //
        // COSA E' STATO FATTO. Un censimento di tutte le coppie
        // IconButton+DropdownMenu: SETTE, e SEI erano senza contenitore
        // (ArticleScreen, ComposeScreen, GroupsScreen, ThreadListScreen,
        // ThreePaneHome x2) piu' il menu appena estratto in AzioneAggiorna.kt,
        // che il `Box` ce l'aveva in ThreePaneHome e l'ha perso nel trasloco
        // della 118 — errore mio, e la settima copia che si e' comportata da
        // settima copia. Una sola aveva il `Box` (ThreePaneHome:1615), ed e'
        // l'unica che si posizionava bene. Ora ce l'hanno tutte, con il
        // commento che dice perche' quel `Box` non e' decorativo: togliendolo,
        // il menu smette di seguire l'icona.
        //
        // versionName resta 1.4.4: non e' mai stata pubblicata su SourceForge.
        // versionCode 119 perche' il 118 era gia' stato consegnato a Marco —
        // monotono, mai riusato, anche se quel pacchetto non e' stato nemmeno
        // scaricato (par. 1 della checklist). Il 118 va ritirato.
        //
        // 1.4.4 (24 settembre 2026, versionCode 118) — §A7 E UNA CORREZIONE
        // CHE NON HA UNA VOCE NELLE NOTE.
        //
        // §A7, DAVE ROYAL: con un solo server l'icona "aggiorna" non apre piu'
        // il menu, aggiorna e basta. Il menu si apriva SEMPRE, e con un server
        // offriva due voci che fanno la stessa cosa piu' un dialogo in cui
        // spuntare l'unico server che si possiede. Piano B scelto da Marco: la
        // condizione PIU' l'estrazione — icona e menu vivevano in due copie
        // (GroupsScreen e ThreePaneHome) identiche salvo il nome della
        // variabile di stato, e ora stanno in ui/screens/AzioneAggiorna.kt.
        // Terzo mattone di §B4 dopo i dialoghi del ritiro e il pulsante
        // Incolla. E' l'unico file nuovo (94 invece di 93).
        //
        // LA CORREZIONE SENZA VOCE. L'editor delle regole disegnava la casella
        // "estendi alle risposte" solo sul campo FROM, e al salvataggio
        // FORZAVA a NONE il followup di ogni altro campo. Il criterio era
        // scritto due volte in quel file, e quando e' arrivato un campo nuovo
        // sono rimaste indietro entrambe: la casella non compariva, e una
        // regola importata da kill-file con la propagazione la perdeva alla
        // prima apertura dell'editor, in silenzio. Ora il criterio e' uno solo
        // e sta sull'enum (FilterField.identificaUnAutore), e il salvataggio
        // CONSERVA invece di azzerare.
        //
        // Nello stesso giro, tre campi che l'editor perdeva da sempre e che
        // nessuno aveva mai provato: la scadenza di una regola, lo stato
        // "disattivata" e il punteggio di propagazione. Stessa malattia — il
        // dialogo ricostruiva l'entita' da zero, quindi ogni campo non mostrato
        // tornava al predefinito.
        //
        // Perche' senza voce nelle note: vedi il blocco 117 qui sotto.
        //
        // 1.4.4 (24 settembre 2026, versionCode 117) — LA SEGNALAZIONE DI ROXY.
        //
        // Roxy: «nella versione "normale" e' presente sia il "cancel message"
        // del menu contestuale, ed e' presente anche l'icona "Incolla". Ma
        // nella versione a pannelli landscape continuano a non essere
        // presenti.» Aveva ragione, e la formulazione esatta e' piu' semplice
        // di "non funzionano": nei pannelli quelle due cose NON C'ERANO. Lui le
        // cercava e non le trovava, noi gli confermavamo che la correzione
        // c'era — avevamo ragione tutti e due, guardando schermate diverse.
        // Sesto caso della serie "presente in una copia su due".
        //
        // 1. L'ICONA INCOLLA nel pannello di scrittura dei pannelli. Tutto il
        //    lavoro fatto su quel pulsante (1.3.3 la richiesta, 105/H l'icona
        //    sempre visibile, build 106 la lettura degli appunti spostata dentro
        //    il clic) era stato fatto su una copia sola, perche' l'altra il
        //    pulsante non ce l'aveva. ESTRATTO in ui/Appunti.kt, non ricopiato.
        // 2. "RITIRA DAL SERVER" nel menu contestuale dei pannelli. Nella 110
        //    non era entrato, e la ragione era scritta nel codice: porta con se'
        //    tre dialoghi che vivevano in ThreadListScreen, e ricopiarli sarebbe
        //    stato il difetto stesso. Quella ragione e' stata ONORATA invece che
        //    aggirata: i tre dialoghi sono in ui/screens/RitiroArticolo.kt e le
        //    due viste ne chiamano uno solo. E' l'unico file nuovo del pacchetto
        //    (93 invece di 92).
        //
        // C'e' anche un campo nuovo nel motore filtri, che fa match sul lettore
        // di news dichiarato da chi scrive. NON ha una voce nelle note di
        // versione, e la scelta e' deliberata: un filtro serve a chi lo imposta,
        // e annunciarlo in un elenco che leggono tutti e' l'unico modo di
        // renderlo inutile. La funzione e' documentata dove va documentata,
        // cioe' nel codice e nella nota dell'editor delle regole.
        //
        // versionName resta 1.4.4: non e' mai stata pubblicata su SourceForge.
        // versionCode 117 perche' il 116 era gia' stato consegnato — monotono,
        // mai riusato, anche quando cambia solo del testo.
        //
        // 1.4.3 (23 settembre 2026, versionCode 114) — LA LISTA SORDA, PRESA.
        // Il log diagnostico di BIG Umberto sulla 113 contiene l'eccezione per
        // intero: "Couldn't read row 627, col 0 from CursorWindow" su una lista
        // di 2301 righe, subito dopo un sync che aveva inserito 4 articoli. La
        // diagnostica messa nella 104 ha fatto il suo mestiere, e cio' che da
        // settembre era un'abduzione ora e' un dato. Contenuto:
        //  1. il `.catch` che difendeva le liste era TERMINALE — dopo la sua
        //     emit la Flow e' completata e Room non ha piu' nessuno a cui
        //     notificare: e' quello, non il fallimento di lettura, a rendere la
        //     lista sorda fino alla ricostruzione della schermata. Sostituito
        //     con retryWhen a budget limitato, che ri-sottoscrive e la fa
        //     riparare da sola. La difesa era scritta in TRE copie: ora e' una;
        //  2. al primo fallimento il log scrive quanto PESA davvero la lista,
        //     separando le colonne mostrate da body/headersRaw/faceB64 che
        //     SELECT * si porta dietro. E' la misura che dira' se l'innesco e'
        //     il peso o la scrittura concorrente — e quindi se serva o no la
        //     proiezione della query, che tocca entrambe le viste;
        //  3. l'Annulla della diagnostica LISTGROUP annulla davvero (Umberto:
        //     "devi chiudere NeReO brutalmente per interrompere", davanti a
        //     un'attesa che il suo log misura in 97 secondi). Non basta
        //     cancellare la coroutine: una lettura bloccante su socket non ha
        //     punti di sospensione, quindi si chiude il socket. E il dialogo si
        //     chiude PRIMA dello snackbar, non dopo;
        //  4. voce A3 — la pulizia degli orfani, che la 113 si era limitata a
        //     contare "misurare prima". La misura e' arrivata: 3 gruppi e
        //     99.906 righe di catalogo senza server sul telefono di Umberto, su
        //     un database di 36 MB. Con conferma e preventivo, stampo della 113;
        //  5. pulizia minuta dell'audit: groupDao.delete(id), DELETE viva e
        //     senza chiamanti in un progetto senza CASCADE; sei chiavi stringa
        //     mai referenziate; un KDoc appeso alla funzione sbagliata.
        // versionName CAMBIA, e NON e' una scelta: LA 1.4.2 VERSIONCODE 113
        // E' GIA' IN MANO AGLI UTENTI. Due APK diversi non possono chiamarsi
        // uguale (par. 1 della checklist). Era stato scritto "1.4.2, versionName
        // invariato" per qualche ora, sulla premessa sbagliata che la 113 non
        // fosse ancora uscita: l'ha corretto Marco. E' la SECONDA volta in due
        // giorni che questo progetto attribuisce a una release pubblicata cose
        // che non contiene — la prima fu la 1.4.1/107 con le build 108-110 —
        // quindi le tre correzioni della 114 hanno una sezione di note propria
        // (rn_v143_*) e la sezione della 1.4.2 e' stata rimessa alle sette voci
        // che il 113 ha davvero spedito.
        // Build 113 (22 settembre 2026) — la 112 non l'ha provata nessuno,
        // nemmeno Marco: e' compilata e basta. Quindi invece di consegnarla
        // cosi' ci entrano le correzioni dell'audit che non richiedono una
        // rifattorizzazione, tutte della stessa famiglia ("il database dice la
        // verita'"):
        //  1. cancellare un server porta via i suoi gruppi, i suoi articoli,
        //     il suo catalogo e i suoi pin — prima restavano orfani per
        //     sempre, perche' non ci sono chiavi esterne e nessuno puliva;
        //  2. e lo chiede prima, dicendo quanti messaggi se ne vanno;
        //  3. la diagnostica database conta gli orfani gia' esistenti (solo
        //     conta: la pulizia avra' una build sua, misurare prima);
        //  4. eliminaDiscussione aggiorna il contatore dei non letti, unica
        //     cancellazione che non lo faceva;
        //  5. l'inizializzatore morto di paneBarInMenu allineato al valore
        //     vero, quello che init() scrive a ogni avvio.
        // 1.4.2 (22 settembre 2026, versionCode 112): la 1.4.1 PUBBLICA su
        // SourceForge e' il versionCode 107 — confermato da Marco. Le build
        // 108, 109 e 110 sono state scritte quando la 107 era gia' in mano
        // agli utenti, quindi NON sono mai uscite come 1.4.1 ed escono con
        // questa: la revoca della 105/B, le icone della barra nei due layout,
        // il crosspost su thread nuovo nei pannelli e "Sostituisci" nel menu
        // contestuale dei pannelli. Note in-app, README e storico sono stati
        // riportati a cio' che la 107 ha davvero spedito: per qualche ora
        // elencavano undici voci sotto la 1.4.1 invece delle nove vere.
        // Il 112 e non il 111 perche' il pacchetto 111 era gia' stato
        // consegnato: versionCode monotono, mai riusato (par. 1).
        // 1.4.2 (22 settembre 2026, versionCode 111): la 1.4.1 e' PUBBLICA su
        // SourceForge, quindi versionName DEVE cambiare — due APK diversi non
        // possono chiamarsi uguale (par. 1 della checklist).
        // Contenuto: UNA correzione, segnalata sul gruppo con l'analisi di HAL
        // e verificata riga per riga prima di toccare il codice. Il pulsante
        // "Ricarica il messaggio" della barra di lettura chiamava
        // eliminaCopiaLocale, che fa DELETE FROM articles: cancellava
        // l'articolo invece di riscaricarlo, e siccome syncGroup riparte da
        // lastFetchedArticleNumber+1 quell'articolo non tornava piu'. Il
        // difetto e' nato nella 1.4 insieme alla funzione stessa, quindi e'
        // anche nella 1.4 e nella 1.4.1 gia' pubblicate.
        // Insieme, il cambio di nome che toglie di mezzo la trappola:
        // eliminaCopiaLocale -> eliminaArticoloLocale.
        // Build 110: "Sostituisci (supersede)" nel menu contestuale della
        // vista a PANNELLI, dove non c'era. Segnalato da Roxy, che e' andato a
        // cercare una funzione che gli avevo detto esserci e non l'ha trovata:
        // aveva ragione lui. Quarto caso in tre giorni di una funzione
        // presente in una copia su due. "Ritira (cancel)" NON entra qui: i
        // suoi tre dialoghi vivono in ThreadListScreen e vanno estratti, non
        // ricopiati, e quel percorso manda messaggi di CONTROLLO sulla rete.
        // Corretta anche la nota della 1.3.1 (rn_v131b_l2), che elencava sei
        // voci di quel menu di cui quattro non sono mai esistite: e' la nota
        // che mi aveva fatto dare a Roxy una risposta sbagliata.
        // Build 109: la terza voce di Dave sulla modalita' a pannelli — il
        // crosspost su un thread NUOVO, che li' non si poteva fare (il
        // selettore compariva solo rispondendo a un articolo gia' crospostato,
        // mentre nella vista a pagine per un thread nuovo c'e' sempre).
        // Il numero e' 109 e non 108 anche se la 108 non e' mai stata
        // compilata: la regola dei due numeri non fa eccezioni, ed era gia'
        // stata applicata alla 102 il 17 settembre.
        // Build 108: due riscontri di Dave Royal sulla 107, prima che uscisse.
        // (1) REVOCA della 105/B: un proprio post non nasce piu' letto. La
        // regola era piu' larga della richiesta che l'aveva generata e nessun
        // altro lettore si comporta cosi' — PAN, Thunderbird, PH e (verificato
        // da Marco) MesNews, che di questo progetto e' il modello dichiarato.
        // (2) La barra della colonna 2 in modalita' a pannelli, che la 107
        // aveva lasciato indietro: "segna tutto letto" via dalla barra (e'
        // gia' nel menu, li' e' un doppione) e ordine allineato al telefono.
        // 1.4.7 (5 ottobre 2026, versionCode 134) — APERTURA DEL CICLO.
        // versionName CAMBIA, e NON e' una scelta: la 1.4.6 versionCode 133 E'
        // STATA PUBBLICATA su SourceForge il 2 ottobre (confermato da Marco),
        // e due APK diversi non possono chiamarsi uguale (par. 1 della
        // checklist). Finche' l'albero diceva 1.4.6/133 mentiva su se' stesso:
        // ogni controllo di coerenza confrontava i sorgenti con un numero che
        // appartiene gia' a un pacchetto pubblico.
        // Questa build non cambia una riga di comportamento: e' l'allineamento
        // che apre il cantiere della 1.4.7 (voce 1 di nove). Il versionCode si
        // alza lo stesso, perche' e' monotono e non si riusa nemmeno per una
        // virgola in un commento.
        // 1.4.7 (5 ottobre 2026, versionCode 135) — DUE VOCI DI JEFF.
        // §A17: il riscarico del gruppo compare anche nel foglio a pressione
        // prolungata della schermata gruppi, dove abita tutta la sua famiglia.
        // Il dialogo era in DUE copie gia' divergenti (`it.message` contro
        // `it.message ?: ""`): la terza non e' stata scritta, le due sono state
        // SPOSTATE in DialogoRiscaricaGruppo.kt.
        // §A18: l'elenco dei gruppi ricorda dove si era (GroupScrollMemory).
        // versionName INVARIATO: la 1.4.7 non e' ancora pubblicata, il 134 non
        // e' uscito dalla macchina di Marco. versionCode alzato lo stesso.
        // 1.4.7 (5 ottobre 2026, versionCode 136) — CORREZIONE DI UN MIO
        // ERRORE DELLA 135, trovato da Marco sul dispositivo col log NNTP.
        // Estraendo il dialogo del riscarico avevo messo dentro di esso il
        // `rememberCoroutineScope()`. Quel composable esiste solo mentre il
        // dialogo e' aperto: chiuderlo lo toglie dalla composizione e CANCELLA
        // LO SCOPE, cioe' la coroutine appena avviata. `reloadGroupFromScratch`
        // faceva in tempo a cancellare gli articoli ma non a risincronizzare:
        // gruppo svuotato (415 -> 0 righe) e sync successivi senza XOVER.
        // Ora lo scope arriva dalla SCHERMATA, che resta composta.
        // 1.4.7 (5 ottobre 2026, versionCode 137) — DUE COSE INDIPENDENTI.
        // (1) L'avviso che mancava nel dialogo del riscarico: dice quanti
        // messaggi tornano davvero (AppSettings.maxHeadersSync) e che i piu'
        // vecchi si perdono. Nasce da un numero vero: 415 -> 298 sul telefono
        // di Marco. Il corpo del dialogo avvertiva su stato di lettura e corpi,
        // non sul fatto che si puo' finire con MENO messaggi di prima.
        // (2) §A15 build 1: il rientro dell'albero, scritto in TRE file, e le
        // rotaie verticali vanno in RientroAlbero.kt. ⚠️ LA RESA NON CAMBIA:
        // le rotaie restano solo nei pannelli. Si accenderanno nella build 2.
        // 1.4.7 (5 ottobre 2026, versionCode 138) — §A15 BUILD 2.
        // Le rotaie verticali dell'albero si accendono anche nell'elenco
        // messaggi del telefono e nell'albero della discussione: erano solo
        // nei pannelli, e la 137 ha dimostrato che l'estrazione reggeva a
        // resa invariata. Risposta alla domanda di Dave Royal del 2 ottobre:
        // non erano una scelta di progetto, erano una copia rimasta indietro.
        // ⚠️ L'albero guadagna i 4.dp di scarto che telefono e pannelli hanno
        // sempre avuto: senza, l'ultima rotaia cadrebbe sotto la prima lettera.
        // 1.4.7 (5 ottobre 2026, versionCode 139) — LA CHIUSURA.
        // Nessuna modifica di comportamento: entrano le NOTE DI VERSIONE
        // (ReleaseNotes.sections + rn_v147_* nelle tre cartelle), che alla
        // build 134 avevo dimenticato alzando solo VERSION — l'app avrebbe
        // mostrato il titolo 1.4.7 con i contenuti della 1.2.
        // *Un numero di versione aggiornato senza le sue note non e' mezzo
        // lavoro: e' un'etichetta che mente.*
        // versionCode nuovo perche' ReleaseNotes.kt e strings.xml stanno
        // DENTRO lo zip: il par. 1 non fa eccezioni nemmeno per una virgola.
        // 1.4.7 (5 ottobre 2026, versionCode 140) — §A11-bis, DOVE MANCAVA.
        // `NomeGruppoAdattivo` (nome intero finche' ci sta, poi abbreviato,
        // poi ellissi) era usata in DUE punti su cinque, entrambi nei
        // pannelli. Dave l'aveva chiesta per la barra di colonna 2; il
        // titolo sul telefono fa lo stesso mestiere e aveva il troncamento
        // secco. Aggiunta li' e nel selettore dei gruppi.
        // Quarta volta della stessa forma in questa release: la logica
        // c'era gia', mancava in meta' dei posti dove serve.
        // 1.4.7 (5 ottobre 2026, versionCode 141) — CORREZIONE DELLA 140.
        // Avevo passato `righe = 2` al titolo del telefono «per non cambiare
        // il comportamento esistente». Ma la richiesta di Dave dice
        // «truncated to fit WITHOUT line-wrap»: il comportamento esistente
        // era quello da togliere. NomeGruppoAdattivo abbrevia solo quando il
        // testo trabocca oltre `righe`, quindi con due righe concesse non
        // scattava mai. Trovato da Marco sullo screenshot di
        // alt.comp.software.thunderbird, andato a capo invece che abbreviato.
        // 1.4.8 (5 ottobre 2026, versionCode 142) — §A12-retro, PRIMA META'.
        //
        // versionName CAMBIA, e non e' una scelta: la 1.4.7 versionCode 141 e'
        // PUBBLICA su SourceForge. Due APK diversi non possono chiamarsi uguale
        // (par. 1 della checklist).
        //
        // «APPLICA ORA» COPRIVA DUE AZIONI SU QUATTRO. Il motore ha cinque
        // esiti (KILL, HIDE, NORMAL, HIGHLIGHT, WATCH) e
        // `applicaFiltriRetroattivi` scriveva il punteggio in UN RAMO SOLO,
        // quello HIDE. Per gli altri la riga conservava il punteggio di prima,
        // cioe' zero nel caso tipico. Segnalazione di Cercaparole su Usenet: una
        // regola «Evidenzia» o «Segui» non vale sul pregresso.
        //
        // IL PUNTO CHE HA DECISO LA FORMA DELLA CORREZIONE: in NeReO
        // «evidenziato» NON E' UN FLAG. L'elenco discussioni colora la riga
        // leggendo `article.score >= 100`. Quindi non manca una funzione, manca
        // una UPDATE: il motore valutava correttamente e questa funzione
        // buttava via il risultato. Nessuna colonna nuova, nessuna migrazione.
        //
        // PERCHE' UN DAO NUOVO (`setScoreByIds`) invece di riusare
        // `setHiddenAndScoreByMessageIds` con `hidden = false`, che era gratis:
        // quella scrive ENTRAMBE le colonne, e un articolo nascosto per
        // PROPAGAZIONE puo' avere, sui propri campi, un punteggio positivo —
        // si sarebbero riaperti in silenzio rami che un plonk aveva chiuso, su
        // scrittura di massa. E per ID DI RIGA, non per message-id, perche' il
        // punteggio e' l'esito della valutazione di quel messaggio IN QUEL
        // GRUPPO (il wildmat decide quali regole si applicano): per message-id
        // due copie crosspostate si livellerebbero all'ultimo gruppo esaminato.
        //
        // DUE ESCLUSIONI VOLUTE: i messaggi gia' nascosti (non si vedono, e
        // riscriverli cancellerebbe il punteggio di propagazione, unica traccia
        // del perche' sono nascosti) e i seguiti (esclusi da sempre, a monte).
        //
        // LIMITE DICHIARATO, seconda meta' di §A12-retro, build a parte:
        // l'azione WATCH scrive il punteggio ma non mette la stella, e la
        // propagazione alle risposte continua a nascondere qualunque sia
        // l'azione della regola — quindi «estendi alle risposte» su una regola
        // «Segui» nasconde ancora i rami che volevi seguire. Sono tre scritture
        // di massa e si provano una per volta.
        // 1.4.8 (6 ottobre 2026, versionCode 143) — TRE VOCI DI BIG UMBERTO.
        //
        // versionName INVARIATO: la 1.4.8 non e' ancora pubblicata, l'APK 142
        // non esiste fuori di qui. versionCode alzato perche' e' monotono.
        //
        // par. U1 — IL BERSAGLIO NELLA VISTA RAGGRUPPATA. «Mostra nell'elenco»
        // evidenziava il messaggio solo se era il PRIMO POST di una
        // discussione; albero e sequenziale a posto. Gli articoli arrivano da
        // un Flow con `initial = emptyList()`, quindi alla prima composizione
        // la lista e' VUOTA: l'effetto partiva lo stesso, bruciava la guardia
        // `discussioneAperta` cercando in un elenco vuoto, e all'arrivo dei
        // dati rinunciava. Si vedeva in una vista sola perche' e' l'unica in
        // cui un messaggio non-radice ha bisogno della seconda passata.
        // *Una guardia che protegge dal caso «non c'e'» scatta anche sul caso
        // «non c'e' ANCORA».*
        //
        // par. U2 — `_` E `%` ERANO METACARATTERI NELLA RICERCA. Le due query
        // di ricerca sono `LIKE` senza clausola `ESCAPE`: cercare `_114`
        // significava «un carattere qualsiasi, poi 114». Non e' sicurezza
        // (il testo lo scrive l'utente per se'), e' correttezza: l'underscore
        // non si poteva cercare. ⚠️ La neutralizzazione esisteva gia' dentro
        // `plonkAuthorByFrom` dall'audit 1.2.12: alla seconda occorrenza e'
        // stata ESTRATTA in `testoPerLike`, non ricopiata.
        //
        // par. U3 — LA FRECCIA INDIETRO DOPO «MOSTRA NELL'ELENCO». La rotta
        // veniva aperta con un `navigate` nudo — in tutta la navigazione non
        // c'era un solo `popUpTo` — quindi l'elenco finiva SOPRA l'articolo e
        // la freccia ripercorreva tutto a ritroso. ⚠️ Si toglie l'ARTICOLO e
        // solo quello: `popUpTo` svuota tutto cio' che sta sopra la
        // destinazione nominata, quindi puntare all'elenco discussioni —
        // la prima forma pensata — avrebbe portato via anche la ricerca,
        // l'opposto dell'intenzione. Le due costanti `*_PATTERN` esistono
        // perche' `popUpTo` confronta il percorso REGISTRATO, query opzionale
        // compresa, e un percorso che non corrisponde viene ignorato in
        // silenzio.
        //
        // Fuori dall'app, nei file dello zip: lo storico versioni in coda ai
        // due README era UNA riga da ~3800 caratteri che ripeteva sette
        // versioni gia' descritte per esteso sopra (il Merovingio).
        // 1.4.8 (6 ottobre 2026, versionCode 144) - par. B6b, GENERARE f=f.
        //
        // versionName INVARIATO: la 1.4.8 non e' ancora pubblicata.
        //
        // L'IMPOSTAZIONE NASCE SPENTA, e non per prudenza generica: e'
        // l'unica di NeReO che cambia i byte che finiscono su Usenet, e un
        // articolo spedito non si ritira. Tutto il resto della 1.4.8 si
        // corregge sul telefono di Marco; questo si correggerebbe sugli
        // schermi degli altri. Il principio 1 vuole il metro prima della
        // modifica rischiosa: il metro e' la meta' che LEGGE f=f, dentro
        // dalla 1.4.6 (par. B6a), e l'interruttore serve a provare la meta'
        // che scrive su messaggi veri prima di accenderla a tutti. Marco la
        // verifica con MesNews dal PC.
        //
        // `avvolgiFlowed` e' `wrapBody` con UNA differenza: le righe che
        // spezziamo noi finiscono con uno spazio. ⚠️ E' il contrario esatto
        // del `trimEnd()` aggiunto nella 1.3.7 per la domanda di Kerman —
        // la stessa riga e' un difetto o la funzione, secondo cio' che il
        // messaggio dichiara di se'.
        //
        // ⚠️ IL `Content-Type` GUARDA IL CORPO, non l'impostazione, e usa lo
        // STESSO predicato che il lettore usa per fidarsi del formato altrui
        // (`almenoUnaRigaContinua`, estratto da `usaFlowed`): cosi' non
        // possiamo dichiarare un formato che il nostro stesso lettore
        // scarterebbe come vuoto. E' il difetto di par. B6a-bis — MesNews
        // che dichiara f=f e impagina con a-capo rigidi — reso impossibile
        // per costruzione invece che per attenzione.
        // 1.4.8 (6 ottobre 2026, versionCode 145) - CORREZIONE DELLA 144,
        // trovata dal messaggio di prova PRIMA della compilazione.
        //
        // `avvolgiFlowed` teneva da parte DUE colonne sempre e confrontava
        // il contenuto con `cols - prof - 2`. Conseguenza: una riga di 71 o
        // 72 colonne andata a capo A MANO veniva spezzata in due, 66 + 6,
        // e in un lettore che non ricompone si vedeva la scaletta. Cioe'
        // esattamente il difetto che f=f dovrebbe togliere, prodotto da noi
        // e su chiunque impagini a 72 colonne — su Usenet la maggioranza,
        // Marco compreso.
        //
        // Ora il conto si fa sulla riga che USCIREBBE: una riga che non
        // verra' spezzata non ha nessuno spazio di rottura, quindi il suo
        // margine e' uno solo e si conosce in anticipo. La promessa di
        // `wrapBody` — *chi va a capo a mano non vede cambiare nulla* —
        // vale di nuovo anche con f=f acceso, e i due rami producono adesso
        // lo STESSO numero di righe: l'unica differenza sono 30 spazi
        // invisibili in coda.
        //
        // La risposta sulla protezione sta ora in UN POSTO SOLO
        // (`protezioneServe`): serviva due volte — per misurare e per
        // comporre — e due risposte che divergono danno righe di 73 colonne
        // o righe spezzate per niente.
        //
        // *Un messaggio di prova rappresentativo e' un metro: 23 asserzioni
        // passavano, e il difetto stava in un caso che nessuna di quelle 19
        // nominava.*
        // 1.4.8 (6 ottobre 2026, versionCode 146) - DUE DIFETTI VISTI IN
        // MESNEWS, E UNA LEZIONE SUL BANCO DI PROVA.
        //
        // Marco ha postato il messaggio di prova su it.test e l'ha guardato
        // in MesNews. Il blocco 5 - una riga rientrata di tre spazi e lunga
        // - arrivava A FILO, mentre la stessa riga corta arrivava
        // rientrata. Controprova decisiva: lo stesso testo postato DA
        // MesNews arriva rientrato, quindi non era la resa del lettore.
        //
        // (1) IL RIENTRO PERSO. `split(" ")` su un contenuto che comincia
        // con spazi produce elementi VUOTI in testa, e il ramo «prima
        // parola» li riassorbiva uno dopo l'altro senza scrivere niente.
        // `wrapBody` non ne soffre perche' la sua regex di prefisso si
        // porta via anche gli spazi iniziali prima dello split; qui no,
        // perche' in f=f quel rientro e' CONTENUTO del capoverso. Ora si
        // mette da parte prima di spezzare, va sulla prima riga e non si
        // ripete sulle continuazioni.
        //
        // (2) IL GEMELLO, dalla stessa riga: la condizione era
        // `corrente.isEmpty()`. Un elemento vuoto che cade subito DOPO una
        // rottura lasciava `corrente` vuota, e la parola seguente entrava
        // dal ramo «prima parola» senza il suo spazio: un doppio spazio
        // dell'utente diventava singolo. *«La riga e' vuota» non vuol dire
        // «siamo all'inizio della riga».* Ora e' un flag esplicito.
        //
        // ⚠️⚠️ E LA LEZIONE CHE VALE PIU' DELLE DUE CORREZIONI. Le 23
        // asserzioni della 145 passavano perche' giravano su una
        // TRASCRIZIONE INFEDELE: in Python avevo scritto il ciclo con un
        // flag `primo`, mentre il Kotlin usa `corrente.isEmpty()`. Le due
        // forme si comportano in modo diverso ESATTAMENTE sugli elementi
        // vuoti, cioe' sui due casi qui sopra. Il banco di prova diceva il
        // vero su un programma che non era questo.
        // *Una trascrizione e' una prova solo finche' e' fedele, e la
        // fedelta' non si presume: si controlla riga per riga.*
        // 1.4.8 (6 ottobre 2026, versionCode 147) - CORREZIONE DELLA 146, E
        // LA FINE DELLE TRASCRIZIONI.
        //
        // IL DIFETTO, visto da Marco su it.test: a ogni rottura di riga due
        // parole si incollavano - "messaggioserve", "numeratoqui". Il ramo
        // `else` chiamava `chiudiRiga`, che rimette `primaParolaDellaRiga` a
        // `true`, e poi faceva `corrente.append(parola)` SENZA rimetterlo a
        // `false`: la parola dopo rientrava dal ramo "prima parola", che
        // appende senza spazio. Ora la prima parola di una riga si posa da
        // un posto solo (`posaPrimaParola`).
        // *Un flag che due rami devono ricordarsi di spegnere viene
        // dimenticato da uno dei due.*
        //
        // ⚠️⚠️ MA LA COSA CHE CONTA E' COME L'ABBIAMO SAPUTO. Le 25
        // asserzioni della 146 passavano, e passavano perche' giravano su
        // una TRASCRIZIONE in Python delle due funzioni: la trascrizione
        // metteva il flag a `false` dove il Kotlin non lo metteva. E' la
        // SECONDA volta in un'ora che la trascrizione diverge proprio nel
        // punto in cui sta il difetto.
        //
        // RIMEDIO STRUTTURALE: `WrapBody.kt` e `FormatFlowed.kt` sono Kotlin
        // puro, senza un solo import - si compilano DA SOLI, senza Android e
        // senza Gradle. Non c'era nessuna ragione di trascriverli. Da ora le
        // prove girano sul codice vero (`controlli/flowed/prova_flowed.sh`,
        // kotlinc + java). Verificato che lo strumento VEDE: sul codice
        // della 146 segnala 6 fallimenti, dove la trascrizione ne segnalava
        // zero.
        // *Una trascrizione non e' il programma. Se il programma si puo'
        // eseguire, si esegue.*
        // 1.4.9 (8 ottobre 2026, versionCode 148) - DUE VOCI DA JEFF, UNA
        // CHE TOGLIE E UNA CHE AGGIUNGE.
        //
        // versionName CAMBIA: la 1.4.8 e' pubblicata su SourceForge dal 6
        // ottobre, e due APK diversi non possono chiamarsi uguale (par. 1
        // della checklist).
        //
        // (par. J1) L'INTERRUTTORE PER LE ROTAIE DELL'ALBERO. Jeff aveva
        // scelto lo stile citazioni "solo i >" e vedeva comunque righe
        // verticali: erano le rotaie del rientro di par. A15, un ornamento
        // diverso che si somiglia. Verificato che non fosse una regressione -
        // ArticleBody.kt e' identico byte per byte dalla 1.4.6 - e che
        // l'indizio discriminante ce l'avesse davanti: le barre delle
        // citazioni prendono il COLORE DEL LIVELLO, le rotaie sono di un
        // grigio unico. *Un indizio visibile a tutti non e' un indizio notato
        // da qualcuno.*
        //
        // Non si corregge, si rende opzionale, e ACCESA di default: le rotaie
        // le ha chieste Dave Royal. ⚠️ La guardia sta DENTRO
        // disegnaRotaieAlbero, non nei tre drawBehind che la chiamano: tre
        // guardie sarebbero tre copie della stessa condizione, cioe' il guasto
        // da cui par. A15 e' nato. L'interruttore sta in Aspetto SOTTO lo stile
        // citazioni, cioe' dove Jeff ha cercato - par. A16.
        //
        // (par. J3-C1) "CONSERVA QUESTO", da una domanda di Jeff: "does NeReO
        // store posts?". La risposta era no, e la domanda ha fatto vedere
        // quello che mancava. Non un ARCHIVIO - il nome e' la promessa, e
        // "archivio" promette elenco, ricerca e permanenza - ma un file .eml
        // scritto in una cartella scelta UNA VOLTA, con permesso SAF
        // persistente. Meta' era gia' scritta dalla 1.4.5: articoloEml() e la
        // riscrittura del charset. Quello che mancava era passare da
        // CreateDocument, che chiede la destinazione a OGNI salvataggio, a un
        // albero scelto una volta - col selettore di mezzo nessuno conserva un
        // messaggio dopo l'altro.
        //
        // ⚠️ I TRE RISCHI, E IL TERZO E' QUELLO CHE NON SI VEDEVA.
        //  (1) il permesso SAF si puo' perdere, e NON solo cancellando la
        //      cartella: il backup di NeReO salva le impostazioni ITERANDO
        //      TUTTE le chiavi (#4b, "completo per costruzione"), quindi l'URI
        //      viaggia col backup mentre il permesso, legato
        //      all'installazione, no. Rimedio: non si crede al valore
        //      salvato, si chiede al sistema (persistedUriPermissions). Non si
        //      e' aggiunta un'eccezione al backup perche' avrebbe risolto un
        //      caso solo rompendo quell'invariante;
        //  (2) i nomi di file collidono: nomeFileConserva aggiunge la parte
        //      locale del Message-ID, unica per definizione (RFC 5322 3.6.4);
        //  (3) la ricerca non vede i conservati, e lo dice la nota ACCANTO al
        //      comando, non solo il README - lezione di par. A16.
        //
        // Costo dell'azione di barra: DUE posti dal par. B4 della 1.4.6
        // (catalogo + implementazione), piu' gli id nei cataloghi di
        // Settings. Nasce nel menu dei pannelli, applicando la regola scritta
        // nel 1.3.6 per le azioni future.
        //
        // nomeFileSicuro e' ESTRATTO da nomeFileEml invece di essere
        // ricopiato: prima di scrivere la seconda copia di una logica si
        // estrae la prima.
        // 1.4.9 (8 ottobre 2026, versionCode 149) - par. K1: IL MINIMO
        // DEL CAMPO MESSAGGIO NON ERA UN MINIMO.
        //
        // versionName RESTA "1.4.9": la 148 non e' mai stata pubblicata, e il
        // par. 1 della checklist lascia fermo il nome finche' la versione non
        // esce su SourceForge. Cambia solo il versionCode, come sempre.
        //
        // SEGNALAZIONE. Un utente su Samsung J6 (720x1480, 5,6", tastiera
        // Samsung con barra dei suggerimenti) non vedeva quello che scriveva:
        // lettere TAGLIATE A META' in altezza nel campo del messaggio.
        //
        // ⚠️ NON ERA LA TASTIERA A COPRIRE IL CAMPO. La barra con «Invia»
        // stava al suo posto sopra la tastiera, quindi l'ingombro era
        // dichiarato correttamente: era il campo a ricevere pochi pixel. La
        // conferma e' arrivata dallo stesso utente, che con una tastiera BASSA
        // (KeePass, senza suggerimenti ne' riga numeri) riesce a scrivere.
        // *La tastiera Samsung non e' la causa: e' la condizione che scopre il
        // difetto.* Cambiare tastiera o telefono lo nasconde, non lo risolve.
        //
        // CAUSA. La colonna di ComposeScreen non scorreva e il campo prendeva
        // `weight(1f)`: il padre lo misurava con un'altezza FISSA pari a cio'
        // che avanzava, e il `heightIn(min = 120.dp)` non poteva chiedere piu'
        // del massimo ricevuto. Il minimo veniva schiacciato in silenzio —
        // mentre il commento accanto dichiarava «altezza minima garantita».
        //
        // > Un commento che dichiara una garanzia non e' una garanzia.
        //
        // RIMEDIO. La colonna scorre, il campo perde il peso e tiene
        // `heightIn(min = 160.dp, max = 480.dp)`. ⚠️ Le due meta' si tengono:
        // in una colonna scorrevole un figlio con `weight` fa cadere la
        // schermata (altezza massima infinita). Il tetto evita che, con
        // altezza illimitata, il campo cresca fino a contenere un'intera
        // risposta lunga disegnandola tutta.
        //
        // ⚠️ NON si tocca ComposePane in ThreePaneHome.kt, che ha lo stesso
        // difetto in forma peggiore (nessun limite di altezza): sta sulla vista
        // larga, dove lo spazio non manca, e due schermate in una build
        // raddoppiano i sospettati. Resta nel quaderno.
        //
        // BANCO DI PROVA, e non costa una compilazione: caratteri molto
        // grandi, telefono in verticale, dettagli «Mittente e intestazioni»
        // aperti, tastiera alzata. Marco l'ha riprodotto cosi' sul proprio
        // Edge 50 Neo — «con quelli aperti non vedo nemmeno mezza riga».
        // 1.4.10 (10 ottobre 2026, versionCode 150) - TRE VOCI DI
        // MANUTENZIONE, NESSUNA CHIESTA DA FUORI.
        //
        // ⚠️ versionName CAMBIA PER OBBLIGO: la 1.4.9 e' pubblica su
        // SourceForge dall'8 ottobre, e due APK diversi non possono
        // chiamarsi uguale (par. 1 della checklist).
        //
        // ⚠️ E "1.4.10" E' IL PRIMO NUMERO A DUE CIFRE DEL PROGETTO.
        // Verificato in UpdateChecker.kt PRIMA di usarlo: il confronto e'
        // NUMERICO segmento per segmento (`segmenti()` piu' il ciclo che
        // confronta Int), quindi 1.4.10 risulta piu' recente di 1.4.9. Un
        // confronto testuale avrebbe detto il contrario, e il controllo
        // aggiornamenti avrebbe smesso di vedere le versioni nuove IN
        // SILENZIO. *Un rischio che non morde va verificato lo stesso: la
        // prossima volta il numero a due cifre arriva senza che nessuno ci
        // pensi.*
        //
        // (voce 9) `fallbackToDestructiveMigration(false)`. Il `false` non e'
        // una scelta nostra: e' l'equivalente che Room dichiara nel proprio
        // `ReplaceWith`. Comportamento invariato, un avviso in meno.
        //
        // (voce 12, par. A11-bis la coda) I nomi dei gruppi nell'elenco
        // passano da NomeGruppoAdattivo, come gia' facevano NELLO STESSO FILE
        // una sessantina di righe piu' sotto. Era rimasto fuori perche'
        // serviva il grassetto per i non letti e il componente non ha un
        // parametro `fontWeight` — ma ha `style`, e il grassetto e' un
        // attributo dello stile. *Prima di aggiungere un parametro a un
        // componente condiviso, guardare se quello che c'e' lo esprime gia'.*
        //
        // (voce 11) WrapBody.kt e FormatFlowed.kt SI SPOSTANO nel package
        // `text`. Chiudono il debito dichiarato nella 1.4.8: `Repository`
        // (livello data) importava da `ui.screens`. ⚠️ Zero comportamento, e
        // il banco di prova `controlli/flowed/prova_flowed.sh` compila i due
        // file VERI al percorso nuovo e passa tutte le asserzioni — che e' la
        // verifica piu' forte possibile per uno spostamento.
        //
        // Le tre viaggiano insieme perche' nessuna puo' produrre un difetto
        // silenzioso: due sono a comportamento invariato e verificate dal
        // compilatore, la terza cambia una resa che si vede a colpo d'occhio.
        // 1.4.10 (10 ottobre 2026, versionCode 151) - par. K3: IL MENU
        // CONTESTUALE CHE NON SCORREVA. versionName invariato: la 1.4.10 non
        // e' mai uscita su SourceForge.
        //
        // SEGNALAZIONE di Macchia: tenendo premuto su un messaggio il
        // pannello delle voci si apre, ma scorrendo verso l'alto «a un certo
        // punto si blocca» e le ultime voci restano tagliate — «Ritira dal
        // server» irraggiungibile.
        //
        // ⚠️ NON SI BLOCCAVA: NON SCORREVA. Il contenuto del ModalBottomSheet
        // era una Column senza verticalScroll. Oltre l'altezza massima del
        // foglio il resto veniva tagliato, senza modo di arrivarci.
        //
        // ⚠️ E IL COMMENTO ACCANTO DICEVA GIA' «le voci scorrono». E' il
        // SECONDO commento in tre giorni che dichiara una garanzia che non
        // c'e', dopo «altezza minima garantita» del campo messaggio (K1).
        // Non e' una coincidenza: e' il modo in cui questo progetto si fa
        // male. *Un commento che dichiara una garanzia non e' una garanzia.*
        //
        // LA CONDIZIONE L'HA ISOLATA MACCHIA DA SOLO: succede solo se nel
        // ramo ci sono suoi messaggi, perche' allora compare la voce in piu'
        // «Vai alla prima risposta ai miei messaggi» e le voci passano da
        // tredici a quattordici. *Una segnalazione che porta gia' la variabile
        // discriminante vale una diagnosi.*
        //
        // ⚠️ CORRETTO IN DUE POSTI: ThreadListScreen e ThreePaneHome hanno lo
        // stesso foglio. Correggerne uno solo sarebbe il guasto della 1.3.
        //
        // ⚠️ par. K1-bis (lo stesso difetto nel campo messaggio dei pannelli)
        // NON entra qui: aprendo ComposePane e' saltato fuori un commento di
        // beta15 che dice che li' il .verticalScroll() esterno era stato
        // TOLTO apposta, perche' con un campo scrollabile dentro produceva il
        // bug di Roxy «il testo sparisce su INCOLLA». Vedi il documento di
        // progetto: va verificato prima sulla 1.4.9 pubblicata, non ripetuto
        // al buio.
        // 1.4.10 (10 ottobre 2026, versionCode 152) - LA 151 NON COMPILAVA.
        //
        // ⚠️ SETTE riferimenti al vecchio package erano rimasti indietro dopo
        // lo spostamento della voce 11: due nomi pienamente qualificati
        // (`FormatoCorpo`) in ArticleBody.kt e CINQUE import in CoreTests.kt.
        // Il compilatore ne ha visti tre, perche' la compilazione di release
        // non tocca i test: gli altri cinque avrebbero rotto le prove dopo.
        //
        // ⚠️ E NESSUN CONTROLLO POTEVA VEDERLI. `controllo_import` verifica
        // che un simbolo usato ABBIA un import, non che quell'import PUNTI
        // DOVE DEVE: un import verso un package che non contiene piu' quel
        // nome e' un import a tutti gli effetti, e lo script lo contava.
        //
        // > Un controllo che verifica l'esistenza di un riferimento non
        // > verifica la sua destinazione.
        //
        // RIMEDIO STRUTTURALE: `controlli/controllo_pacchetti.py`, che
        // costruisce la mappa package -> simboli dai sorgenti e verifica ogni
        // nome qualificato. Verificato PRIMA di crederci, come vuole il
        // principio 1: sull'albero corretto tace, sui sorgenti della 151
        // segnala esattamente quei sette e nient'altro. La prima stesura ne
        // segnalava 115 sul codice sano — un controllo che urla a vuoto non
        // esiste, ed e' stata corretta prima di entrare.
        //
        // *E il censimento a mano aveva mancato il bersaglio DUE VOLTE sullo
        // stesso spostamento: prima `formatoCorpo`, poi `FormatoCorpo` — due
        // nomi che differiscono per una maiuscola.*
        // 1.4.10 (12 ottobre 2026, versionCode 153) - par. K4: IL `+` CHE
        // NON APRIVA NIENTE. La 152 non e' mai stata compilata.
        //
        // SEGNALAZIONE, cinque parole: «faccio tap sul + ma non mi apre
        // l'albero». Marco non l'aveva capita; era esatta.
        //
        // Nella vista a discussioni raggruppate il `+` compare quando
        // `total > 1`, e `total` conta tutti i membri; ma all'apertura, con
        // «solo non letti» acceso, le risposte gia' lette venivano filtrate.
        // Discussione col primo messaggio non letto e risposte tutte lette:
        // `+` presente, contatore che dice «3 risposte», e aprendola NULLA.
        //
        // > Un comando premibile che non fa niente e' peggio di un comando
        // > assente: il secondo informa, il primo fa dubitare l'utente di se'
        // > stesso. E infatti ha chiesto se sbagliava lui.
        //
        // RIMEDIO B su tre valutati: il `+` apre tutto. Il filtro dice che
        // cosa mostra l'ELENCO; toccare il `+` e' un gesto esplicito su UNA
        // discussione, e il gesto esplicito vince sulla preferenza generale.
        // Il contatore torna onesto senza toccarlo. UNA RIGA, in `rootsList`
        // (ThreadFlatten.kt), che e' il posto condiviso dalle due viste.
        //
        // ⚠️ Terza sorpresa in una settimana prodotta da «solo non letti»,
        // dopo par. A16 e par. U1. *Un filtro che nasce acceso e' una
        // premessa che l'utente non ricorda di avere.*
        // 1.4.11 (12 ottobre 2026, versionCode 154) - par. B3: L'ALLARME
        // ANTI-DECLASSAMENTO STARTTLS, CHIESTO DA OBIWAN.
        //
        // ⚠️ versionName CAMBIA PER OBBLIGO: la 1.4.10 versionCode 153 E'
        // PUBBLICA su SourceForge dal 12 ottobre 2026, confermata da Marco.
        // Due APK diversi non possono chiamarsi uguale (par. 1 della
        // checklist).
        //
        // RFC 4642 par. 5, verificata alla fonte: «An NNTP client can
        // PARTIALLY protect against these attacks by RECORDING THE FACT that
        // a particular NNTP server offers TLS during one session and
        // GENERATING AN ALARM if it does not appear in the CAPABILITIES
        // response for a later session.» E' un MAY, e protegge solo DOPO il
        // primo collegamento pulito: se l'attaccante c'e' gia' la prima
        // volta, questo meccanismo non lo vede mai. Lo dice la RFC stessa con
        // quel «partially», e la nota di versione lo ripete.
        //
        // Il par. 2.1 della stessa RFC sembra vietarlo - «cached results of
        // CAPABILITIES from a previous session MUST NOT be relied on» - e non
        // lo vieta:
        //
        // > Ricordare per allarmare non e' fidarsi per decidere.
        //
        // Il dato ricordato non cambia MAI il percorso di connessione: non fa
        // saltare un tentativo, non presume una capacita'. Confronta e segnala.
        //
        // Costo reale quasi nullo, e non per bravura: `connect()` GIA' legge
        // CAPABILITIES prima di `tentaStartTls()`, e la 1.2.18 ha GIA' ridotto
        // a UNO i punti di preparazione dopo il collegamento
        // (`preparaConnessione`). Nessun comando NNTP in piu', nessun giro di
        // rete, nessuna migrazione Room.
        //
        // ⚠️ MEMORIA PER host:porta, NON per `Server.id`. L'avevo progettata
        // per id, e BackupManager mi ha smentito: al ripristino i server
        // prendono id NUOVI (si rimappa per host:porta, commento riga 64). Una
        // memoria per id, dopo un ripristino, sarebbe finita sul server
        // SBAGLIATO - un falso allarme peggiore del silenzio. E host:porta non
        // e' solo un ripiego: due righe sullo stesso host:porta vedono per
        // forza la stessa CAPABILITIES, quindi e' anche la granularita' giusta.
        //
        // Le tre condizioni che tengono zitto l'allarme quando deve star zitto
        // (un allarme che suona a vuoto non esiste - vedi controllo_pacchetti,
        // che alla prima stesura urlava 115 volte sul codice sano):
        //  1. lista CAPABILITIES VUOTA non vuol dire «STARTTLS rimosso»: il
        //     server che non risponde 101 la lascia vuota. Si tace.
        //  2. `ssl = true` (TLS implicito) escluso da tutto: un server su 563
        //     non ha ragione di annunciare STARTTLS, e allarmare li' renderebbe
        //     ogni server cifrato un falso allarme permanente.
        //  3. allarme solo se quel host:porta STARTTLS lo aveva offerto
        //     davvero in passato. Mai alla prima visita.
        // 1.4.11 (12 ottobre 2026, versionCode 155) - CORREZIONE DELLA 154,
        // TROVATA DAL COLLAUDO DI MARCO PRIMA DELLA PUBBLICAZIONE.
        //
        // versionName INVARIATO: la 1.4.11 non e' mai stata pubblicata, la 154
        // non e' uscita dalla macchina di Marco. versionCode alzato lo stesso,
        // perche' e' monotono e non si riusa (par. 1 della checklist).
        //
        // ⚠️ IL DIFETTO. `tentaStartTls` rilegge CAPABILITIES dopo un upgrade
        // riuscito - cosa che la RFC raccomanda - e fino alla 154 SOVRASCRIVEVA
        // `advertisedCapabilities`. Ma dopo uno STARTTLS riuscito il server NON
        // DEVE PIU' annunciare STARTTLS (RFC 4642 par. 2.1), quindi il par. B3
        // leggeva una lista senza STARTTLS e gridava al declassamento PROPRIO
        // SULLE CONNESSIONI CIFRATE CON SUCCESSO.
        //
        // MISURATO, non dedotto. Log di Marco del 12 ottobre 2026, OVH:119,
        // tre sessioni di fila:
        //   6p1ix  STARTTLS acceso, upgrade riuscito -> nessuna registrazione
        //   3mksl  STARTTLS spento -> la lista non viene sovrascritta, registra
        //   4gz7i  STARTTLS riacceso, upgrade riuscito -> ALLARME FALSO
        // E l'allarme citava come prova la lista post-upgrade: nella prima riga
        // della stessa sessione c'era "OVER, READER, STARTTLS, XPAT", nella
        // riga dell'allarme "OVER, READER, XPAT".
        //
        // RIMEDIO B su tre valutati. La rilettura resta (e' cio' che la RFC
        // raccomanda) ma scrive in un campo suo, `capabilitiesDopoTls`; e
        // `advertisedCapabilities` torna a significare UNA COSA SOLA: cio' che
        // il server annunciava quando siamo arrivati. Verificato prima di
        // sceglierlo che nessuno leggesse quel campo dopo l'upgrade: fuori da
        // NntpClient lo legge solo `osservaStarttls`.
        //
        // ⚠️ Scartato A (un secondo campo "fotocopia", lascia due nomi simili
        // e fra sei mesi si legge quello sbagliato) e C (dedurre l'offerta dal
        // fatto che l'upgrade e' riuscito: la piu' corta, ma l'unica che si
        // romperebbe DI NUOVO IN SILENZIO se un domani si aggiungesse
        // un'altra rilettura altrove - cioe' esattamente come ci siamo
        // arrivati stavolta).
        //
        // > Un campo che risponde a due domande diverse risponde male alla
        // > seconda il giorno in cui qualcuno gliela fa.
        //
        // ⚠️ Il numero 155 era stato assegnato a una build USA-E-GETTA, mai
        // compilata, che serviva a far comparire l'allarme a comando. Non
        // serve piu': il difetto l'ha fatto comparire da solo, sul codice vero
        // e su un server vero. *Un banco di prova costruito per simulare un
        // guasto e' superfluo quando il guasto c'e' davvero.*
        // 1.4.11 (12 ottobre 2026, versionCode 156) - LA RIGA DI DIAGNOSTICA
        // CHE TAGLIAVA SENZA DIRLO. Rimedio D su quattro valutati.
        //
        // versionName INVARIATO: la 1.4.11 non e' ancora pubblicata.
        //
        // ⚠️ Non e' una voce chiesta da un utente: e' un difetto che il
        // collaudo del par. B3 ha fatto emergere su se stesso. La riga
        // `CAPABILITIES:` del log faceva `.take(300)` e non lo dichiarava, e
        // nello stesso pomeriggio ha portato fuori strada DUE VOLTE - la
        // seconda su una decisione di sicurezza: si era concluso che
        // `news.tcpreset.net:119` non offrisse STARTTLS, e invece lo offre
        // (risponde 382), col nome oltre il taglio.
        //
        // > Una troncatura senza un segno non e' un difetto cosmetico quando
        // > il dato tagliato e' quello che decide.
        //
        // RIMEDIO D: si accorciano gli ARGOMENTI, una capability per volta e
        // partendo dalla piu' ingombrante, finche' la riga ci sta. Nessun NOME
        // di capability si perde mai, e i nomi sono cio' che serve a chi legge.
        //
        // ⚠️ Scartato "alzare il tetto a 600": sposta il problema di qualche
        // server, non lo toglie. Scartato "marcare il taglio e basta": avrebbe
        // detto al lettore di essere cieco lasciandolo cieco - su tcpreset si
        // sarebbe letto "TRONCATA" e si sarebbe dovuto fare l'esperimento
        // comunque. Scartato "una capability per riga": il log tiene ~400
        // righe a rotazione, e il dialogo - la ragione per cui esiste - ne
        // verrebbe spinto fuori.
        //
        // ⚠️ IL FILE E' NUOVO E LO E' DI PROPOSITO: `nntp/CapabilitiesLog.kt`
        // e' Kotlin PURO, senza un solo import, quindi si compila da solo con
        // kotlinc e si PROVA invece di crederci - come WrapBody.kt e
        // FormatFlowed.kt. Banco: controlli/capabilities/prova_capabilities.sh,
        // 22 asserzioni sul codice vero, coi dati misurati nei log di Marco.
        // *Se il programma si puo' eseguire, si esegue.*
        //
        // ⚠️ E il banco ha corretto ME: avevo scritto l'asserzione dando per
        // scontato che si accorciasse SASL. Si accorcia LIST, e ha ragione -
        // 121 caratteri di argomenti contro 94. Esito migliore di quello che
        // mi aspettavo: i meccanismi SASL, utili a diagnosticare l'auth,
        // sopravvivono. *Un'aspettativa non e' una specifica.*
        //
        // Il conteggio file dello zip passa da 107 a 108, e il file in piu' e'
        // questo (par. 6 della checklist: una differenza si spiega).
        // 1.5 (12 ottobre 2026, versionCode 157) - APERTURA DEL CICLO.
        // PASSO 2 DEL PACCHETTO CITAZIONI: l'oggetto e il suo formattatore.
        //
        // ⚠️ versionName CAMBIA, E IL SALTO E' VOLUTO: la 1.4.11 versionCode
        // 156 e' PUBBLICA su SourceForge dal 12 ottobre (manifest binario
        // verificato). "Chiamare 1.5 la prossima release" era nell'elenco
        // VALUTATO E SCARTATO: e' stato RIAPERTO con Marco e deciso si',
        // perche' era stato scartato in un contesto di release piccole e il
        // pacchetto citazioni non lo e' - schermata nuova, raccoglitore,
        // inserimento da file. *Una decisione scartata si puo' riaprire, ma si
        // riapre dichiarandolo.*
        //
        // ⚠️ VERIFICATO ESEGUENDOLO prima di adottare il numero:
        // isNewer("1.5", "1.4.11") = true. `segmenti()` confronta Int per Int
        // e i segmenti mancanti valgono 0, quindi 1.5 == 1.5.0 e 1.5.1 > 1.5.
        // E' la stessa verifica fatta prima della 1.4.10, e per la stessa
        // ragione: un confronto TESTUALE avrebbe detto il contrario e il
        // controllo aggiornamenti avrebbe smesso di vedere le versioni nuove
        // senza un errore da nessuna parte.
        //
        // LA REGOLA DI QUESTA RELEASE, e vale piu' del numero: OGNI BUILD DEVE
        // LASCIARE L'ALBERO PUBBLICABILE. Una release grande rompe il ritmo
        // che ha trovato i difetti della 1.4.11; si para costruendo il
        // pacchetto in passi additivi e invisibili finche' non c'e' la
        // schermata. Se a meta' strada arriva una segnalazione urgente si
        // pubblica quello che c'e', invece di scegliere fra una release
        // incompleta e un rinvio.
        //
        // PERCHE' SI COMINCIA DAL PASSO 2 E NON DAL 3: il passo 3 (schermata
        // «Cita...») usa `SelectionState` e `disableSelectionClearOnTap()`,
        // firme non verificabili da qui. Il passo 2 e' LOGICA PURA, quindi va
        // in un file senza un solo import e si PROVA con kotlinc - ed e' anche
        // invisibile all'utente, quindi rispetta la regola qui sopra.
        //
        // Lo zip passa da 108 a 109 file: il file in piu' e' `ui/Citazione.kt`
        // (par. 6 della checklist: una differenza si spiega).
        // 1.5 (12 ottobre 2026, versionCode 158) - PASSO 3, PIANO C, PRIMA
        // META': la voce «Cita» nel menu di selezione della lettura a schermo
        // intero. Marco ha scelto il piano C: selezione + menu come strada
        // principale, schermata a righe come ripiego per il telecomando.
        //
        // ⚠️ IL DATO DEL 10 SETTEMBRE E' CAMBIATO. Compose 1.12.0 espone il
        // testo selezionato (`SelectionState.selectedTexts`): verificato sui
        // METADATI KOTLIN dell'APK 157 (classe e membri PUBLIC), e compilato
        // contro le classi dell'APK con kotlinc + plugin Compose - firme e
        // tipi passano. Manca invece `disableSelectionClearOnTap()`: per
        // questo e' una voce del menu e non un pulsante nella barra.
        //
        // ⚠️ IL TESTO SELEZIONATO NON E' IL TESTO DEL MESSAGGIO: barre al posto
        // dei `>`, numeri di livello, capoversi flowed riuniti. La selezione
        // dice solo QUALI righe; si citano le ORIGINALI (`righeOriginaliDi`,
        // logica pura in Citazione.kt, 35 asserzioni con kotlinc).
        //
        // versionName INVARIATO: la 1.5 non e' ancora pubblicata.
        // Lo zip passa da 109 a 110 file: il file in piu' e'
        // `ui/CitaSelezione.kt` (par. 6 della checklist).
        // 1.5 (12 ottobre 2026, versionCode 159) - PIANO D, DAL COLLAUDO DI
        // MARCO SULLA 158: citando righe che nel messaggio erano GIA' citate,
        // l'introduzione nominava l'autore del messaggio aperto («Roberto ha
        // scritto:») mentre il testo era di Giangi, sei livelli piu' in basso.
        // Ora la citazione si ri-radica sull'attribuzione interna che introduce
        // quelle righe, e i livelli in piu' si tolgono. `riradica` e
        // `sembraAttribuzione` in Citazione.kt (logica pura, 47 asserzioni,
        // il post vero di Marco e' una prova fissa). `sembraAttribuzione` e'
        // ESTRATTA da buildPrefill, dove stava in due copie.
        // versionName INVARIATO: la 1.5 non e' ancora pubblicata. Zip: 110 file.
        // 1.5 (12 ottobre 2026, versionCode 160) - LA 159 NON SCATTAVA SUL POST
        // VERO. Collaudo di Marco: stesso post di Roberto, ancora «Roberto ha
        // scritto:». Il testo GREZZO (dato da Marco) ha un `>>>> 8>< ----` al
        // livello 4 fra l'attribuzione di Giangi (5) e il testo (6): e' il
        // taglio di Roberto, al SUO livello, piu' basso del blocco. La 159 lo
        // prendeva per l'uscita dal blocco. Ora si scavalcano righe vuote e
        // segni di taglio a qualunque livello (`sembraTaglio`). Il banco
        // passava perche' il caso era ricostruito da uno screenshot: adesso
        // contiene il testo vero (51 asserzioni).
        // E, deciso da Marco: README.txt -> README_IT.txt, anche nello zip,
        // per avere un solo nome ovunque. Zip: 110 file, stesso numero.
        // versionName INVARIATO: la 1.5 non e' ancora pubblicata.
        // 1.5 (12 ottobre 2026, versionCode 161) - «CITA» ANCHE NELLA VISTA A
        // PANNELLI, deciso da Marco prima di pubblicare: chi usa i pannelli
        // (Dave, tablet) non avrebbe trovato la voce che le note annunciano.
        // Due chiamate a CorpoCitabile in ThreePaneHome (PostContinuo e
        // ArticlePaneSingolo): l'unico punto ha tre chiamanti, zero copie.
        // Le frasi «per ora solo a schermo intero» di note, README e manuale
        // sono CORRETTE invece di aggiungere voci: erano diventate false.
        // versionName INVARIATO: la 1.5 non e' ancora pubblicata. Zip: 110 file.
        // 1.5.1 (13 ottobre 2026, versionCode 162) - LA BOZZA SI SALVA DA SOLA.
        // ⚠️ versionName CAMBIA PER OBBLIGO: la 1.5 versionCode 161 e' PUBBLICA
        // su SourceForge dal 12 ottobre (manifest binario verificato).
        // isNewer("1.5.1", "1.5") = true, ESEGUITO prima di adottare il nome.
        //
        // Piano B di fine sviluppo, scelto da Marco: 1.5.1 = bozza autosalvata
        // + «Scarta» + ritiro della promessa della schermata a righe; 1.5.2 =
        // §A12-retro meta' B. Pochi giri di compilazione: la decisione sta in
        // GuardiaBozza.kt (Kotlin puro, 14 asserzioni con kotlinc: T1, T2, T3),
        // e ComposeScreen e' stato compilato contro le classi dell'APK 157 —
        // nessun errore sulle righe nuove.
        // Zip: da 110 a 111 file, il file in piu' e' ui/GuardiaBozza.kt.
        // 1.5.1 (13 ottobre 2026, versionCode 163) - §A12-retro META' B, la
        // seconda meta' del piano B di fine sviluppo, nella STESSA release
        // (Marco: pubblicare 1.5.1 e §A12 insieme). versionName INVARIATO: la
        // 1.5.1 non e' pubblicata.
        //
        // La propagazione alle risposte SEGUE L'AZIONE della regola: prima
        // nascondeva sempre (default -100), quindi «Segui»/«Evidenzia» estese
        // alle risposte nascondevano i rami da vedere. Decisione in
        // FilterEngine.effettoPropagazioneDi (Kotlin puro, 13 asserzioni con
        // kotlinc); per le regole negative NIENTE cambia. Revoca sdoppiata sul
        // segno della regola com'era salvata. Stella su «Segui il thread»
        // (propagazione e «Applica ora»), PER ULTIMA nel giro. Una build sola,
        // scelta da Marco: colore e stella si distinguono a occhio.
        // Compilato contro le classi dell'APK 157 (senza la stdlib duplicata):
        // nessun errore sulle righe nuove. Zip: 111 file, nessun file nuovo.
        // 1.5.2 (13 ottobre 2026, versionCode 164) - L'APK DIMAGRISCE, NIENTE
        // DI VISIBILE CAMBIA. versionName CAMBIA PER OBBLIGO: la 1.5.1
        // versionCode 163 e' PUBBLICA su SourceForge (manifest verificato).
        // isNewer("1.5.2", "1.5.1") = true, ESEGUITO prima di adottare il nome.
        //
        // Misura sull'APK 157 (dex2jar + conteggio classi): i dex erano 13,06 MB
        // su 14,35; le icone di material-icons-extended erano 11.400 classi su
        // 29.414 e il 55% del bytecode. NeReO ne usa 75. Con R8 spento (resta
        // spento: i nomi degli enum stanno nel database) entravano TUTTE.
        // Piano A, scelto da Marco: le 75 icone COPIATE in ui/icone/IconeNeReO.kt
        // con gli STESSI nomi (estensioni su Icons.Filled e
        // Icons.AutoMirrored.Filled), ricavate dal bytecode della libreria e
        // verificate IDENTICHE una per una (controlli/icone, prova del guasto
        // inclusa); la dipendenza passa a material-icons-core. Negli altri file
        // cambiano solo gli import (22 file, 167 righe).
        // Zip: da 111 a 112 file, il file in piu' e' ui/icone/IconeNeReO.kt.
        // 1.5.3 (14 ottobre 2026, versionCode 165) - TOLTA UNA REGOLA, IL SUO
        // EFFETTO SE NE VA. versionName CAMBIA PER OBBLIGO: la 1.5.2/164 e'
        // PUBBLICA (Marco, 13 ottobre 15:31). isNewer("1.5.3","1.5.2") = true,
        // ESEGUITO.
        //
        // Collaudo di Marco: tolta una regola «Ignora», l'autore restava
        // nascosto fino a un riscarico dal server; tolte «Segui il thread» ed
        // «Evidenzia», stella e colore restavano. Cancellare rimetteva in
        // chiaro le sole risposte propagate, e «Applica ora» andava in una
        // direzione sola. Piano A + A1 (Marco): dopo ogni salvataggio e
        // cancellazione, e in coda ad «Applica ora», una REVISIONE toglie
        // gli effetti che nessuna regola rimasta giustifica. Decisione in
        // FilterEngine.revisioneDopoCambioRegole + applicaVetoCrosspost
        // (Kotlin puro, 26 asserzioni nuove con kotlinc, prova del guasto
        // inclusa). Il plonk rapido scrive ora anche il punteggio. Righe
        // «Filtri:» nel log di diagnostica. Nessuna migrazione.
        // Compilato contro le classi dell'APK 157: nessun errore sulle righe
        // nuove (solo le R nuove, che il banco non ha). Zip: 112 file.
        // 1.5.3 (14 ottobre 2026, versionCode 166) - IL LOG HA TROVATO UN
        // DIFETTO DELLA 165. Salvata una «Segui il thread» estesa alle risposte
        // e premuto «Applica ora», la revisione in coda ha tolto 88 stelle: le
        // COPIE delle risposte appena seguite sugli ALTRI SERVER (it.test su
        // cinque server = cinque righe). Stella e punteggio si scrivono per
        // message-id, il registro di propagazione e' per gruppo: le copie non
        // erano protette. Il veto sulle copie, che valeva solo per il
        // «rimetti in chiaro», ora vale per tutto: una copia giustificata
        // trattiene le altre. 4 asserzioni nuove (43), prova del guasto.
        // versionName INVARIATO: la 1.5.3 non e' pubblicata (Marco l'ha in
        // collaudo; lo stato non si deduce - da confermare).
        // 1.5.3 (14 ottobre 2026, versionCode 167) - LA FOTO DI MARCO: tolta
        // una «Segui il thread» su un autore, due suoi post su Astraweb tenevano
        // la stella. «Applica ora» stellava per message-id (tutte le copie) ma
        // scriveva il 9999 per riga: la copia degli altri server restava con
        // stella e punteggio 0, cioe' con l'aspetto di una stella manuale, e
        // col veto in blocco della 166 tratteneva anche la copia giusta. Ora:
        // punteggio e stella insieme per message-id; veto PER EFFETTO (il
        // nascosto trattiene il nascosto, la stella di regola la stella, il
        // punteggio il punteggio); tolta la stella della regola da una copia,
        // si toglie anche dalle copie che sembrano manuali. 5 asserzioni nuove
        // (48), prova del guasto. versionName INVARIATO: 1.5.3 non pubblicata.
        // 1.5.4 (15 ottobre 2026, versionCode 168) - TRE RICHIESTE DI UTENTI.
        // versionName CAMBIA PER OBBLIGO: la 1.5.3/167 e' PUBBLICA (pagina di
        // SourceForge vista da Marco, download predefinito sull'APK nuovo).
        // isNewer("1.5.4","1.5.3") = true, ESEGUITO.
        //  1. «Cita e rispondi» nel menu di selezione (piano A): la citazione
        //     scelta passa alla risposta da CitazioneInAttesa (ui/Citazione.kt,
        //     presa una volta, scade in 2 minuti, 8 asserzioni) e buildPrefill
        //     la mette al posto di quella automatica. Le due scritture (pagina
        //     e pannello) e le tre viste di lettura.
        //  2. all'apertura del gruppo i nuovi non si vedevano (piano A): il
        //     ripristino decideva sulla lista VECCHIA, prima del sync
        //     dell'apertura. Ora, se dopo il ripristino la cima cambia e
        //     l'utente non ha toccato la lista, si va in cima.
        //  3. «una volta al giorno» = 24 ore OPPURE giorno di calendario nuovo
        //     (piano B): update/FinestraControllo.kt, Kotlin puro, 11 asserzioni.
        // Zip: da 112 a 113 file, il file in piu' e' update/FinestraControllo.kt.
        // 1.5.5 (17 ottobre 2026, versionCode 169) - TRE VOCI DEL QUADERNO
        // (A + B, scelte di Marco). versionName CAMBIA PER OBBLIGO: la
        // 1.5.4/168 e' PUBBLICA dal 15 ottobre (confermato da Marco, APK
        // pubblicato = APK del pacchetto, SHA-256 identico).
        // isNewer("1.5.5","1.5.4") = true, ESEGUITO.
        //  A. §K1-bis: il campo del messaggio nella scrittura a PANNELLI aveva
        //     ancora weight(1f) + heightIn(min): il minimo schiacciato della
        //     1.4.9. Stessa correzione della vista a pagine: colonna
        //     scorrevole, niente peso, heightIn(min = 160, max = 480).
        //  B1. la ricerca dice DOVE ha trovato: se la parola sta solo nel
        //      corpo, sotto la riga compare un estratto col giallo
        //      (ui/screens/DoveCorrisponde.kt, Kotlin puro).
        //  B2. il 441 spiegato: al testo del server si aggiunge una riga
        //      (nntp/RifiutoInvio.kt, Kotlin puro; ui/SpiegaRifiuto.kt per le
        //      due scritture).
        // Zip: da 113 a 118 file: i tre .kt nuovi, TRADEMARK.txt e .gitignore.
        // versionCode 170 (17 ottobre 2026, sera) - G1 + G2 + G3 del
        // brainstorming, scelte di Marco PRIMA di compilare la 169: versionName
        // INVARIATO (la 1.5.5 non e' pubblicata), versionCode alzato perche'
        // la 169 e' stata consegnata (monotono, mai riusato).
        //  G1. iniziali colorate al posto della Face quando manca (colore
        //      fisso per indirizzo); G3. intestazione dell'autore a scheda,
        //      data «oggi / ieri / giorno»: un componente solo per le TRE viste
        //      (ui/screens/IntestazioneAutore.kt; logica pura in
        //      ui/AutoreVisivo.kt). La decodifica della Face e' SPOSTATA li'
        //      da ArticleScreen, difese della 1.2.12 comprese.
        //  G2. icona monocromatica per le icone a tema (mipmap-anydpi-v33).
        // Zip: da 118 a 121 file (i due .kt e l'icona v33).
        // versionCode 171 (18 ottobre 2026) - SEGNALAZIONE SU USENET: rispondere
        // a un post con un References lunghissimo (nl.politiek, 27 id) falliva
        // all'invio. Il References usciva senza tetto. Ora un punto solo, in
        // postArticle: doppioni tolti e, oltre i 998 caratteri, accorciato
        // tenendo la radice e gli antenati piu' recenti (RFC 5537 §3.4.4,
        // RFC 5536 §2.2, lette alla fonte). thread/ReferencesInUscita.kt,
        // Kotlin puro, 17 asserzioni. versionName INVARIATO (1.5.5 non
        // pubblicata, 170 mai compilata). Zip: da 121 a 122 file.
        // versionCode 172 (18 ottobre 2026) - LE X-FACE SI VEDONO (richiesta
        // di Marco, piano A: solo visualizzazione). Prima la avevo chiamata
        // 1.5.6; Marco: la 1.5.5 non e' mai stata pubblicata, quindi resta
        // 1.5.5 (regola dei due numeri: il nome cambia solo dopo la
        // pubblicazione). La 171 compilata non si pubblica.
        // Decodificatore tradotto da FFmpeg (libavcodec/xface.c + xfacedec.c,
        // LGPL 2.1+, da compface di James Ashton) in ui/XFace.kt, Kotlin puro;
        // accanto all'autore: Face, poi X-Face, poi iniziali. Nessuna modifica
        // al database: la X-Face si legge dalle intestazioni gia' salvate.
        // Zip: da 122 a 123 file.
        // versionCode 173 (18 ottobre 2026) - I MESSAGGI NUOVI NON SFUGGONO
        // PIU', davvero (Marcoseba sulla 1.5.4: «a me capita ancora»). La
        // sorveglianza della 1.5.4 si accendeva solo dopo un ripristino di
        // posizione e guardava solo il primo della lista; ora vale a ogni
        // ingresso, si spegne solo per un trascinamento del dito, e il segnale
        // di novita' dipende dall'ordinamento (ui/screens/NovitaInCima.kt,
        // SorveglianzaNovita.kt). Estesa ai pannelli (piano B con estensione,
        // scelto da Marco). versionName INVARIATO: 1.5.5 non pubblicata.
        // Zip: da 123 a 125 file.
        // 1.5.5 / 173 PUBBLICATA il 19 ottobre 2026 (SourceForge + GitHub).
        // versionCode 174 (19 ottobre 2026) - versionName CAMBIA: la 1.5.5 e'
        // pubblica. NIENTE PIU' CHIUSURE SENZA SELETTORE DI FILE (Roxy, box TV
        // Beelink GT King, Android 9: «Esporta configurazione» chiudeva l'app;
        // log crash: ActivityNotFoundException su CREATE_DOCUMENT). Nove punti
        // aprivano il selettore di sistema senza protezione: ora passano tutti
        // da ui/SelettoreSicuro.kt. Piano B (Marco): backup e ripristino della
        // configurazione hanno un ripiego nella cartella dell'app
        // (backup/BackupLocale.kt), senza permessi nuovi.
        // Zip: da 125 a 128 file.
        // 1.5.6 / 174 PUBBLICATA il 20 ottobre 2026 (SourceForge + GitHub v1.5.6).
        // versionCode 175 (20 ottobre 2026) - versionName CAMBIA (1.5.6 pubblica;
        // nome 1.5.7 PROVVISORIO, lo decide Marco). LE VIRGOLETTE SI VEDONO: 38
        // stringhe EN e 17 IT avevano " (o &quot;) non protette, che Android
        // tratta come delimitatori e toglie a schermo. Ora \" ovunque; nuovo
        // controllo 5-bis nella suite. Scoperto provando controlli/traduzioni.py.
        versionCode = 175
        versionName = "1.5.7"
    }

    buildTypes {
        release {
            // Beta1: R8/minify DISATTIVATO di proposito — mai collaudato su
            // quest'app; un crash solo-in-release è il peggior regalo per i
            // tester. Si riattiva (con regole complete e collaudo dedicato)
            // verso la 1.0 finale, a funzionalità congelate.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // buildConfig serve alla schermata Informazioni per leggere versione e
    // pacchetto: da AGP 8 non e' piu' attivo di default e va chiesto
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// 1.3.5 — era `android { kotlinOptions { jvmTarget = "17" } }`.
//
// Sia `kotlinOptions` sia `jvmTarget: String` sono deprecati: la DSL nuova
// vuole un blocco `kotlin { }` di primo livello (fuori da `android { }`) e un
// valore TIPIZZATO invece di una stringa. Il vantaggio non e' estetico: con la
// stringa, "17 " o "1.7" si scoprivano a compilazione fallita; con l'enum non
// esistono proprio.
//
// Il risultato non cambia: 17, come prima e come compileOptions qui sopra, che
// resta sincronizzato a mano (Java e Kotlin hanno due impostazioni separate e
// devono dire la stessa cosa).
//
// VERIFICABILE senza fidarsi: la versione del bytecode dentro l'APK. Se dopo
// questa modifica i .class dicessero qualcosa di diverso da 17, il colpevole e'
// questa riga.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// --------------------------------------------------------------------------
// VERSIONE.txt generato, non scritto a mano.
//
// Storia: il file esisteva come promemoria manuale ("controlla che il numero
// combaci con build.gradle.kts"). Trascinandolo invariato di zip in zip era
// finito per essere l'UNICO dato incoerente del progetto — il guardiano che
// mentiva. Ora lo genera Gradle leggendo gli stessi valori che finiscono
// nell'APK: non e' piu' una copia da tenere allineata, e' un derivato che non
// puo' divergere per costruzione.
//
// Gira a ogni build (dipendenza di preBuild), quindi il file nella cartella
// riflette sempre l'ultimo versionName/versionCode compilato.
// --------------------------------------------------------------------------
val generaVersioneTxt by tasks.registering {
    val versionNameCorrente = android.defaultConfig.versionName
    val versionCodeCorrente = android.defaultConfig.versionCode
    val applicationIdCorrente = android.defaultConfig.applicationId
    // in rootDir/VERSIONE.txt, accanto agli altri file di progetto
    val destinazione = rootProject.file("VERSIONE.txt")
    // input espliciti: Gradle rigenera solo se la versione cambia
    inputs.property("versionName", versionNameCorrente ?: "")
    inputs.property("versionCode", versionCodeCorrente ?: 0)
    outputs.file(destinazione)
    doLast {
        val data = LocalDate.now()
        destinazione.writeText(
            buildString {
                appendLine("NeReO (Ne(ws) Re(ader) O(pen-source))")
                appendLine("(package interno $applicationIdCorrente: invariato di proposito)")
                appendLine("Versione sorgenti: $versionNameCorrente (versionCode $versionCodeCorrente)")
                appendLine("Data build: $data")
                appendLine()
                appendLine("Questo file e' GENERATO automaticamente a ogni build da")
                appendLine("app/build.gradle.kts: non modificarlo a mano, le modifiche")
                appendLine("verrebbero sovrascritte. La versione qui sopra e' sempre")
                appendLine("quella realmente compilata nell'APK.")
            }
        )
    }
}
tasks.named("preBuild") { dependsOn(generaVersioneTxt) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    // Room 2.8.4 (dal 2.6.1): dalla 2.7 le API room-ktx sono state
    // assorbite in room-runtime (nessuna dipendenza room-ktx separata da
    // dichiarare - vedi NeReO_bug_1.2.9.md, Piano C del Dato 5, per il
    // perche' di questo aggiornamento).
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
}
