======================================================================
 NeReO 1.5.6 - Ne(ws) Re(ader) O(pen-source)
 Newsreader Usenet (NNTP) solo testo per Android
 NeReO 1.5.6 (versionCode 174) - By MCSM - 2026/10/19
======================================================================
 ***  VERSIONE 1.5.6  ***

 Una segnalazione di Roxy dal suo box TV, il giorno dopo la 1.5.5.

  * NIENTE PIU' CHIUSURE SUI BOX TV SENZA SELETTORE DI FILE. Molti box Android TV non hanno la schermata di sistema per scegliere dove salvare o cosa aprire, e NeReO si chiudeva all'istante cercando di mostrarla (per esempio con "Esporta configurazione"). Ora lo dice, in tutti e nove i punti che la usano. Esportare e ripristinare la configurazione funziona comunque: senza selettore il backup si salva nella cartella di NeReO (Android/data/org.openusenet.reader/files, il percorso viene mostrato) e il ripristino elenca i backup trovati li'. Copia il backup altrove: disinstallando NeReO quella cartella si cancella.

 ***  VERSIONE 1.5.5  ***

 Tre voci del quaderno: un difetto noto e due piccole confusioni incontrate dagli utenti. E un aspetto nuovo per l'autore del messaggio, piu' l'icona a tema; e una correzione della 1.5.4 segnalata da un utente.

  * L'AUTORE DEL MESSAGGIO HA UN ASPETTO NUOVO. Accanto al nome la sua Face o, quando manca, le sue iniziali su un cerchio colorato: il colore viene dall'indirizzo, quindi la stessa persona ha sempre lo stesso colore, in ogni gruppo. La data dice "oggi", "ieri" o il giorno della settimana per i messaggi recenti (toccala per la data completa). Anche nei pannelli, dove la Face finora mancava.

  * RISPONDERE NELLE DISCUSSIONI MOLTO PROFONDE FUNZIONA DI NUOVO. Quando un messaggio portava una riga References lunghissima (uno in nl.politiek ne aveva 27), la risposta superava il limite di 998 caratteri per riga di intestazione e il server la rifiutava. Ora NeReO toglie gli identificativi doppi e, oltre il limite, toglie i piu' vecchi dopo il primo, tenendo l'inizio della discussione e i messaggi piu' recenti (RFC 5537). Sotto il limite non cambia niente.

  * LE X-FACE SI VEDONO. La classica faccina in bianco e nero di Usenet (l'intestazione X-Face, mandata da MesNews e da molti lettori tradizionali) compare accanto al nome dell'autore, dove finora compariva solo la Face a colori. Prima la Face, poi la X-Face, poi le iniziali colorate. Toccandola si ingrandisce e resta nitida. NeReO le mostra, non le invia.

  * I MESSAGGI NUOVI RESTANO DAVVERO IN VISTA. La correzione della 1.5.4 funzionava solo se NeReO ti aveva appena riportato dov'eri rimasto: al primo ingresso nel gruppo dopo aver aperto l'app, o con la lista gia' in cima, i messaggi arrivati con la sincronizzazione potevano ancora finire sopra il bordo dello schermo. Ora, a ogni apertura del gruppo, se arrivano messaggi nuovi e non hai ancora spostato la lista col dito, sale in cima a mostrarli. Con gli ordinamenti "dai piu' recenti" si accorge anche di una risposta nella discussione gia' in cima; con quelli "dai piu' vecchi", dove i nuovi stanno in fondo, resti dove sei. Anche nella vista a pannelli.

  * ICONA A TEMA: su Android 13 e successivi, con le icone a tema attive, anche quella di NeReO prende i colori del tuo tema.

  * LA RICERCA DICE DOVE HA TROVATO LA PAROLA. Se la parola sta solo nel testo del messaggio, sotto il risultato compare un breve estratto con la parola in giallo. Prima un risultato cosi' non aveva nessun giallo e sembrava sbagliato.

  * IL RIFIUTO 441 DEL SERVER SPIEGATO. Alla risposta del server, che resta intera, segue una riga in italiano. Per il caso piu' comune, "Article must contain real content", dice cosa fare: scrivere qualcosa in piu'.

  * NELLA VISTA A PANNELLI IL CAMPO DEL MESSAGGIO NON VIENE PIU' SCHIACCIATO: l'area di scrittura scorre e il campo mantiene un'altezza utilizzabile, come nella vista a pagine dalla 1.4.9.

  * IL NOME E' RISERVATO. Il codice resta GPL, ma le versioni modificate devono usare un altro nome e un altro applicationId: vedi TRADEMARK.txt.

 ***  VERSIONE 1.5.4  ***

 Tre richieste di utenti, arrivate a Marco il giorno dopo la 1.5.3.

  * "CITA E RISPONDI". Selezionando del testo in un messaggio, il menu offre accanto a "Cita" un comando che apre direttamente la risposta con quella citazione gia' dentro, il cursore sotto e la firma in fondo. Prima servivano cinque passi.

  * APRENDO UN GRUPPO, I MESSAGGI NUOVI NON SFUGGONO PIU'. La lista tornava al punto in cui l'avevi lasciata prima che i nuovi arrivassero in cima, e anche riaprendo si tornava sempre li'. Ora, se arrivano messaggi nuovi e non hai ancora toccato la lista, sale in cima a mostrarli.

  * AGGIORNAMENTI "UNA VOLTA AL GIORNO" controlla alla prima apertura di ogni giorno, e comunque dopo 24 ore. Prima aspettava 24 ore dall'ultimo controllo, e una versione nuova poteva essere notata anche un giorno dopo.

 ***  VERSIONE 1.5.3  ***

 Dal collaudo di Marco: una regola tolta deve portarsi via quello che aveva fatto.

  * TOLTA UNA REGOLA, IL SUO EFFETTO SE NE VA. Cancellando o modificando una regola "Ignora", i messaggi che nascondeva tornano visibili SENZA RISCARICARLI dal server; tolta una "Evidenzia" se ne va il colore; tolta una "Segui il thread" se ne vanno stella e colore - a meno che un'altra regola attiva li nasconda, li evidenzi o li segua ancora. "Applica ora" lavora ora nei due sensi. Le stelle messe a mano restano. Limiti: un messaggio stellato a mano che ricadeva anche nella regola perde la stella; gli autori nascosti col plonk rapido prima della 1.5.3 restano nascosti.

  * IL LOG DI DIAGNOSTICA RACCONTA I FILTRI: righe "Filtri:" per ogni regola salvata o cancellata e per ogni "Applica ora", con quanti messaggi sono tornati visibili, hanno perso la stella o hanno cambiato punteggio.

 ***  VERSIONE 1.5.2  ***

 Nessuna funzione nuova: l'app e' la stessa, ma pesa molto meno.

  * L'APK DIMAGRISCE. NeReO si portava dietro l'intera libreria di icone Material - piu' di UNDICIMILA icone, oltre meta' del codice compilato - e ne usa 75. Ora porta solo quelle, copiate nei sorgenti con gli stessi nomi e verificate identiche una per una (forme, colori, specchiatura per le lingue da destra a sinistra). A schermo non cambia niente.

 ***  VERSIONE 1.5.1  ***

 Prima release di chiusura: protegge il testo che scrivi, e ritira una promessa che non serve mantenere.

  * LA BOZZA SI SALVA DA SOLA. Un secondo e mezzo dopo l'ultima modifica, e di nuovo quando esci dalla schermata o l'app va in secondo piano. Uscire senza toccare "Salva bozza" non fa piu' perdere il testo. Una risposta aperta e lasciata com'era NON diventa una bozza; dopo un invio riuscito la bozza NON ricompare; se l'invio fallisce, la bozza torna a proteggere il testo.

  * "SCARTA" nella barra in basso della scrittura: butta il testo e cancella la bozza, dopo una conferma. Con il salvataggio automatico e' il modo per uscire senza lasciare traccia.

  * FILTRI: LA PROPAGAZIONE ALLE RISPOSTE SEGUE L'AZIONE DELLA REGOLA (versionCode 163). "Estendi alle risposte" su una regola "Segui il thread" o "Evidenzia" NASCONDEVA le risposte, cioe' proprio i rami da vedere: ora le segue con la stella o le evidenzia. "Applica ora" mette la stella anche ai messaggi gia' scaricati di una regola "Segui il thread". Per le regole che nascondono il comportamento e' IDENTICO. Revocando una regola che evidenziava il colore si toglie; le stelle restano, perche' possono essere state messe a mano.

  * LA SCHERMATA A RIGHE PER IL TELECOMANDO, annunciata nella 1.5, per ora non arriva. "Cita" funziona con la selezione del testo; la schermata si fara' se servira' davvero a qualcuno.

 ***  VERSIONE 1.5  ***

 Apre un ciclo lungo: il PACCHETTO CITAZIONI, chiesto da Ammammata e allargato da Bingo3331. Questa prima pietra non si vede, e lo diciamo invece di tacerlo.

  * "CITA" NEL MENU DI SELEZIONE (versionCode 158). Selezioni una parte di un messaggio e, nel menu che compare sopra la selezione, c'e' "Cita": negli appunti finisce una citazione completa - la tua introduzione, le righe col "> " davanti e in coda il Message-ID col link permanente. Si citano sempre le RIGHE ORIGINALI, INTERE: il testo a schermo e' rielaborato (barre al posto dei ">", numeri di livello, capoversi flowed riuniti), quindi la selezione dice solo QUALI righe, e cio' che era gia' citato conserva i suoi livelli. Nella lettura a schermo intero e, dalla versionCode 161, anche nella vista a pannelli; la schermata a righe per il telecomando arriva in un passo successivo.

  * "CITA" DENTRO UNA CITAZIONE DICE CHI HA SCRITTO DAVVERO (versionCode 159). Se le righe scelte erano gia' citate nel messaggio - per esempio la frase di Giangi dentro la risposta di Roberto - la citazione non comincia piu' con "Roberto ha scritto:": usa l'attribuzione che nel messaggio introduce quelle righe ("Il 05/10, Giangi ha scritto:") e toglie i livelli in piu'. Se l'attribuzione non c'e' o non si riconosce, resta il comportamento di prima.

  * IL README ITALIANO SI CHIAMA ORA README_IT.txt (versionCode 160), accanto a README_EN.txt: un solo nome, uguale dentro lo zip dei sorgenti e nella cartella di SourceForge.

  * IL MOTORE DELLE CITAZIONI E' STATO DIVISO IN DUE. Da una parte un VALORE che descrive una citazione - chi l'ha scritta, dove, quando, con quale oggetto, il Message-ID col link permanente, e le righe scelte; dall'altra una FUNZIONE che da quel valore produce il testo. Non e' un dettaglio di stile: il raccoglitore che arrivera' dopo dovra' tenere una LISTA di citazioni e formattarle tutte in fila, e se la cattura scrivesse direttamente una stringa negli appunti il raccoglitore dovrebbe riaprirla e smontarla. E' esattamente il modo in cui in questo progetto e' nata la sesta copia di una logica.

  * L'INTESTAZIONE DELLA CITAZIONE NON SI INVENTA: e' la tua introduzione alla citazione, con tutti i suoi segnaposto compreso %oggetto%. Un modello solo in tutta l'app - chi ha personalizzato la propria se la ritrova anche qui.

  * LA RIGA CHE METTE IL "> " DAVANTI AL TESTO CITATO adesso esiste in un posto solo, invece che dentro la funzione che prepara le risposte. Comportamento identico, verificato riga per riga da un banco di prova.

  * PROVATO PRIMA DI ESSERE COMPILATO. Il file nuovo e' Kotlin puro, senza un solo import: si compila da solo con kotlinc ed esegue 21 asserzioni sul codice vero prima che l'APK esista. Nella 1.4.11 due difetti su due li aveva trovati l'esecuzione e zero la lettura - questa volta il banco nasce insieme al codice invece che dopo un guasto.

 ***  VERSIONE 1.4.11  ***

 Una voce sola, chiesta da ObiWan. Dichiara da se' il proprio limite, perche' lo dichiara la RFC che la descrive.

  * SE UN SERVER OFFRIVA STARTTLS E SMETTE DI ANNUNCIARLO, ADESSO TE LO DICE. STARTTLS e' il passaggio da una connessione in chiaro a una cifrata: il server lo annuncia fra le proprie capability, e chi si mette in mezzo al collegamento puo' cancellare quella sola riga dalla risposta - un lettore che non sa niente di meglio non prova nemmeno a cifrare, e tutto sembra normale. NeReO ora ricorda quali server lo offrono e segnala accanto al nome del server, nell'elenco dei server, quello che lo offriva e non lo offre piu'. E' esattamente la difesa che la RFC 4642 par. 5 suggerisce, con le sue stesse parole: "recording the fact" e "generating an alarm".

  * E IL LIMITE, DETTO SUBITO: la RFC scrive "partially protect", e quel "partially" non e' una sfumatura. Il confronto ha bisogno di un collegamento pulito PRECEDENTE: se qualcuno sta in mezzo gia' la primissima volta che parli con un server, questo meccanismo non lo vede mai. Protegge dal DECLASSAMENTO di un server che si era gia' visto cifrato, non dal primo incontro.

  * RESTA ZITTO NEI DUE CASI IN CUI GRIDEREBBE AL LUPO. Se il server non risponde affatto alla domanda sulle capability la lista arriva vuota, e lista vuota non vuol dire "STARTTLS rimosso": non si segnala niente. E sui server gia' cifrati dal primo byte (TLS implicito, di solito porta 563) non si segnala niente nemmeno a guardarli, perche' non hanno ragione di annunciare un passaggio che non serve - altrimenti ogni server cifrato diventerebbe un allarme permanente.

  * NIENTE TRAFFICO IN PIU', NESSUNA IMPOSTAZIONE NUOVA. Il dato era gia' in mano: NeReO legge le capability a ogni collegamento PRIMA di tentare STARTTLS, dalla 1.3. La memoria e' per INDIRIZZO E PORTA e non per riga della tabella server, perche' al ripristino di un backup i server prendono identificativi nuovi e una memoria per identificativo sarebbe finita sul server sbagliato - un falso allarme e' peggio del silenzio.

 ***  VERSIONE 1.4.10  ***

 Cinque voci: tre di MANUTENZIONE che non ha chiesto nessuno, e due segnalate da utenti. Lo diciamo invece di tacerlo, perche' chi apre le novita' e non trova niente di nuovo ha diritto di sapere perche' il numero e' cambiato.

  * NELL'ELENCO DEI GRUPPI, UN NOME LUNGO SI ACCORCIA invece di troncarsi. Toglie il mezzo della gerarchia anziche' tagliare la fine con i puntini, e solo quando davvero non ci sta; i nomi corti restano intatti e i gruppi con messaggi non letti restano in grassetto. Il componente condiviso che fa questo lavoro era gia' usato una sessantina di righe piu' sotto nella stessa schermata: era rimasto fuori solo questo punto, e per una ragione tecnica e non di prodotto - serviva il grassetto e il componente non ha un parametro per il peso del carattere. Non gliene serviva uno: ha gia' lo stile, e il grassetto e' un attributo dello stile.

  * SOTTO IL COFANO, COMPORTAMENTO IDENTICO. La chiamata deprecata fallbackToDestructiveMigration() e' sostituita dall'equivalente che Room stessa prescrive nel proprio ReplaceWith; e i due file che si occupano dell'andata a capo e del Format=Flowed si SPOSTANO in un package "text" loro, chiudendo il debito dichiarato nella 1.4.8 - stavano fra le schermate, e il livello dati andava a pescare li' dentro. Lo spostamento e' verificato dal banco di prova che compila quei due file VERI con kotlinc: passa tutte le asserzioni al percorso nuovo.

  * NOTA PER CHI TIENE D'OCCHIO I NUMERI: 1.4.10 e' il primo numero a due cifre del progetto. Il controllo aggiornamenti confronta le versioni segmento per segmento COME NUMERI, quindi 1.4.10 risulta piu' recente di 1.4.9 - verificato prima di adottarlo, perche' un confronto testuale avrebbe detto il contrario e avrebbe smesso di vedere le versioni nuove in silenzio.
  * IL PANNELLO DELLE AZIONI, tenendo premuto su un messaggio, adesso scorre. Quando nella discussione c'erano messaggi tuoi compariva una voce in piu' e le ultime azioni - fra cui "Ritira dal server" - restavano tagliate fuori in basso, senza modo di raggiungerle. Non si bloccava: non scorreva. Lo stesso pannello nella vista a pannelli aveva lo stesso difetto ed e' corretto anche quello, perche' una correzione applicata a una copia su due e' il modo in cui questo progetto si e' gia' fatto male. (Segnalato da Macchia, che ha isolato la condizione da solo)
  * IL "+" APRE LA DISCUSSIONE anche quando le risposte le hai gia' lette. Nella vista a discussioni raggruppate, con "solo non letti" acceso, una discussione le cui risposte erano tutte lette mostrava comunque il "+" e il numero delle risposte accanto - ma toccandolo non compariva niente. Il filtro decide che cosa mostra l'ELENCO; toccare il "+" e' una richiesta esplicita di vedere UNA discussione, e adesso il gesto esplicito vince sulla preferenza generale. Segnalato in cinque parole da un utente che pensava di sbagliare lui: non sbagliava. Un comando premibile che non fa niente e' peggio di un comando assente.

 ***  VERSIONE 1.4.9  ***

 Tre voci. Le prime due da Jeff, una che TOGLIE e una che AGGIUNGE; la terza da un utente con un telefono che nessun collaudatore ha. La prima rende spegnibile un ornamento che lui aveva preso per un altro; la seconda risponde a una sua domanda di una riga. La seconda voce dichiara anche il proprio rovescio nel corpo del testo: un messaggio conservato la ricerca non lo vede piu'. Chi conserva lo fa per ritrovare, e una nota che tacesse quel limite venderebbe una funzione diversa da quella che c'e'.

  * UN INTERRUTTORE PER LE ROTAIE VERTICALI DEL RIENTRO (Aspetto, subito sotto lo stile delle citazioni). Sono le righe grigie che negli elenchi segnano un livello di profondita' ciascuna, accese dappertutto dalla 1.4.7. Jeff aveva scelto lo stile citazioni "solo i >" e vedeva comunque righe verticali: NON era una regressione - il disegno del corpo del messaggio e' identico byte per byte dalla 1.4.6 - erano le rotaie, cioe' un ornamento diverso che si somiglia. L'indizio che li distingue ce l'aveva davanti: le barre delle citazioni prendono il COLORE DEL LIVELLO, le rotaie sono di un grigio unico. Un indizio visibile a tutti non e' un indizio notato da qualcuno. Quindi non si corregge: si rende opzionale, e ACCESA di default, perche' quelle rotaie le aveva chieste Dave Royal. L'interruttore sta sotto lo stile citazioni perche' e' li' che Jeff ha cercato. Spegnendole il rientro resta, spariscono solo le linee. (Segnalato da Jeff)

  * "CONSERVA QUESTO MESSAGGIO". Nuova azione nella barra di lettura, e nel menu a tre punti nella vista a pannelli: scrive il messaggio come file .eml in una cartella che scegli UNA VOLTA SOLA (Manutenzione -> Messaggi conservati). Da quel momento conservare e' un tocco. "Salva come file .eml" resta al suo posto e continua a chiedere ogni volta dove mettere il file: e' la forma giusta per il giro occasionale verso un client di posta, ed e' la ragione per cui con quella nessuno metteva da parte un messaggio dopo l'altro. Un messaggio conservato ESCE DALL'ARCHIVIO LOCALE e diventa un file tuo: non lo porta via la pulizia automatica, ne' una disiscrizione, ne' un riscarico. In cambio, LA RICERCA DI NEREO NON LO VEDE PIU' - la ricerca legge l'archivio locale, e quel messaggio ne e' uscito; per rileggerlo si apre il file con un'altra app. Non si chiama "archivio" di proposito: il nome e' la promessa, e un archivio promette elenco, ricerca e permanenza, cioe' tre cose che questa voce non da'. Nasce da una domanda di Jeff, "NeReO conserva i post?": la risposta era no, e adesso e' "se glielo chiedi". (Da una domanda di Jeff)

  * IL CAMPO DEL MESSAGGIO NON SI SCHIACCIA PIU'. Su uno schermo piccolo, con i caratteri grandi e una tastiera alta, le lettere appena scritte si vedevano TAGLIATE A META'. Segnalato da un utente su un Samsung J6, e riprodotto qui alzando la dimensione del testo e aprendo "Mittente e intestazioni". Non era la tastiera a coprire il campo - la barra con "Invia" stava al suo posto sopra di essa: era il campo a ricevere quasi nessuna altezza, perche' il modulo di scrittura non scorreva e il campo prendeva "quello che avanza". L'altezza minima c'era gia' nel codice, con accanto un commento che la dichiarava GARANTITA; non lo era, perche' un minimo non puo' chiedere piu' del massimo che riceve. Ora il modulo scorre e il minimo e' un minimo. Lo stesso utente, con una tastiera piu' bassa, riusciva a scrivere: la tastiera non era la causa, era la condizione che scopriva il difetto.

 ***  VERSIONE 1.4.8  ***

 Sette voci: due sui filtri, tre di BIG Umberto sulla strada "cerca -> apri -> guarda dove sta", che e' la funzione che aveva chiesto lui nella 1.4.6 - sta collaudando la propria richiesta e ne ha trovato i tre buchi. La sesta sta fuori dall'applicazione, e la settima e' la sola di questa versione che cambia QUELLO CHE NEREO SPEDISCE - per questo nasce spenta. La seconda voce dice che cosa NON fa: il comando "Applica ora" copriva due azioni su quattro, questa versione ne aggiunge una delle due mancanti, e tacere l'altra farebbe credere di avere una funzione che non c'e'.

  * "APPLICA ORA" VALE ANCHE PER LE REGOLE CHE AGGIUNGONO. Una regola "Evidenzia", o un punteggio messo a mano in modalita' avanzata, si applicava soltanto ai messaggi che arrivavano dopo: sul pregresso non si vedeva niente. La causa non era un filtro che valutava male - il motore calcolava il punteggio giusto e la funzione che applica le regole al pregresso LO BUTTAVA VIA, perche' lo salvava solo nel ramo che serve a nascondere. E in NeReO "evidenziato" non e' un contrassegno a se': e' il punteggio che l'elenco delle discussioni legge per colorare la riga. Nessuna colonna nuova, nessuna migrazione: mancava una scrittura. Il riepilogo finale ha ora una terza cifra accanto a eliminati e nascosti. (Segnalato da Cercaparole su Usenet)

  * LIMITE DICHIARATO, PER LA META' CHE MANCA. Una regola "Segui il thread" applicata al pregresso da' ora il punteggio - la riga si colora - ma NON assegna ancora la stella delle discussioni seguite; e la casella "estendi alle risposte" continua a NASCONDERE i rami qualunque sia l'azione della regola, anche quando l'azione e' "segui". Sono scritture che toccano molte righe del database e si collaudano una per volta. I messaggi gia' nascosti e le discussioni che segui non vengono toccati, come prima.

  * IL BERSAGLIO FUNZIONA ANCHE SULLE RISPOSTE. Dai risultati della ricerca, "Mostra nell'elenco" riportava alla discussione solo se il messaggio era il PRIMO POST; nella vista a discussioni raggruppate una risposta non veniva evidenziata. Albero e vista sequenziale erano a posto, e non era una differenza fra le viste: nella raggruppata una discussione chiusa e' UNA RIGA SOLA, quindi per una risposta bisogna prima aprire la discussione - ed era quel passaggio a saltare, perche' veniva tentato mentre l'elenco dei messaggi non era ancora arrivato dal database. (Segnalato da BIG Umberto)

  * NELLA RICERCA, IL TRATTINO BASSO E LA PERCENTUALE CERCANO SE' STESSI. Erano caratteri jolly del database: cercare _114 voleva dire "un carattere qualsiasi, poi 114", e fra i risultati comparivano messaggi senza nessun trattino basso. L'indizio che li tradiva: in quelle righe l'evidenziazione gialla non compariva, perche' nell'oggetto il testo cercato non c'era davvero. Vale la pena ricordare che la ricerca guarda OGGETTO, AUTORE E CORPO dei messaggi scaricati, quindi un risultato senza giallo nell'oggetto puo' aver trovato la parola nel testo. (Segnalato da BIG Umberto)

  * LA FRECCIA INDIETRO NON RIPERCORRE PIU' TUTTA LA STRADA. Dopo "Mostra nell'elenco" la lista si apriva SOPRA il messaggio da cui si era partiti, e tornare indietro lo faceva riapparire prima di arrivare dove si voleva. Ora il messaggio esce di scena e la freccia riporta ai RISULTATI DELLA RICERCA, da cui si salta al successivo. (Segnalato da BIG Umberto)

  * FUORI DALL'APPLICAZIONE: LO STORICO VERSIONI IN CODA A QUESTO FILE. Era UNA RIGA SOLA di quasi quattromila caratteri, con tutte le voci da 1.2.7 a beta1 incollate di seguito - una tabella letta come un paragrafo da una riformattazione di fine settembre. E ripartiva da 1.2.7, cioe' ripeteva in forma corta SETTE VERSIONI GIA' DESCRITTE PER ESTESO qui sopra. Ora e' una riga per voce e comincia dalla 1.1, la prima che le sezioni non descrivono. Dalla prossima pubblicazione, inoltre, anche questo README italiano viene caricato gia' estratto nella cartella su SourceForge, accanto a quello inglese. (Entrambe segnalate dal Merovingio)

  * FORMAT=FLOWED IN USCITA, SPENTO DI DEFAULT (Manutenzione -> Impaginazione dei messaggi che spedisci). Acceso, i capoversi lunghi escono con la rottura DEBOLE della RFC 3676 - uno spazio in fondo alle righe che NeReO spezza - invece che con un a-capo fisso a 72 colonne: chi legge con un programma che conosce il formato vede il capoverso ricomposto alla larghezza del proprio schermo, anche su un telefono stretto, e chi legge con un programma che non lo conosce vede le righe a 72 colonne come sempre. La firma non viene toccata, e lo spazio dopo il segno di citazione resta al suo posto - la norma lo permette "to maintain the relative indentation of the lines when viewed by clients which are not aware of Format=Flowed". E' L'UNICA IMPOSTAZIONE CHE CAMBIA QUELLO CHE ESCE DA NEREO, e un messaggio pubblicato non si ritira: per questo nasce spenta, e il formato viene dichiarato solo quando c'e' davvero qualcosa da ricomporre. NeReO sa LEGGERE f=f dalla 1.4.6, quindi puo' verificare la propria uscita da se'.

 ***  VERSIONE 1.4.7  ***

 Sei voci, e cinque vengono da due collaudatori. Hanno tutte la stessa forma: qualcosa che esisteva per una schermata e non per la sua sorella. E' la release delle GIUNZIONI - i punti dove due parti dell'app, nate in momenti diversi, avevano smesso di somigliarsi senza che nessuno lo decidesse.

  * LE ROTAIE VERTICALI DELL'ALBERO compaiono in tutte e tre le viste: elenco messaggi sul telefono, albero della discussione e vista a pannelli. Prima erano solo nei pannelli. Dave Royal ha chiesto se fosse una scelta di progetto: non lo era. La stessa formula del rientro era scritta in TRE file diversi e soltanto uno disegnava anche le rotaie, quindi quella differenza non l'aveva mai decisa nessuno - era una copia rimasta indietro. Unificata in un punto solo e accesa dappertutto in due build separate: prima l'estrazione a resa invariata, poi l'aspetto nuovo, cosi' che un guasto avrebbe avuto una causa sola da cercare. (Segnalato da Dave Royal)

  * QUELLA SCHERMATA SI CHIAMA "albero della discussione", in entrambe le lingue. Si chiamava "albero della conversazione" mentre il resto dell'applicazione dice "discussione" dappertutto, e si contraddiceva da sola: il titolo diceva conversazione, il messaggio di errore diceva discussione. Non e' una questione di stile: una conversazione e' un insieme di messaggi STIMATO dagli oggetti, che e' quello che fanno i programmi di posta; una discussione e' un GRAFO DI RIFERIMENTI DICHIARATI, che e' quello che NeReO costruisce davvero. (Segnalato da Dave Royal)

  * IN ASPETTO C'E' UN INTERRUTTORE per mostrare solo i messaggi non letti. L'impostazione e' sempre esistita e nasce accesa, ma si poteva cambiare soltanto dall'icona a forma di busta sopra una lista, e da nessuna parte nelle Impostazioni. Un collaudatore al primo giorno d'uso ha cercato dove l'impostazione dovrebbe stare, non l'ha trovata e ha concluso che l'applicazione gli CANCELLASSE i messaggi letti. Non e' mai stato cancellato niente: l'interruttore non era dove chiunque lo cercherebbe. (Segnalato da Jeff)

  * IL RISCARICO DI UN GRUPPO si avvia anche dall'elenco dei gruppi, tenendo premuto. Prima stava solo nel menu dentro al gruppo, mentre tutto il resto di quella famiglia - scarica corpi, annulla sottoscrizione, aggiorna tutti i gruppi di questo server - abitava gia' nell'elenco. Fuori posto era il comando, non il posto dove si era cercato. (Segnalato da Jeff)

  * IL DIALOGO DEL RISCARICO dice quanti messaggi torneranno davvero. Riscaricare NON ripristina: riprende dal server tante intestazioni quante ne prevedono le impostazioni di sincronizzazione, e quello che di piu' vecchio avevi in locale si perde per sempre. Un gruppo da 415 messaggi ne ha ripresi 298, perche' l'impostazione era 300. Il dialogo avvertiva su stato di lettura, contrassegni e corpi, ma non sul rischio di finire con MENO messaggi di prima. L'avviso nuovo porta il numero reale della tua configurazione.

  * L'ELENCO DEI GRUPPI RICORDA DOVE ERI. Scorrendo in basso per cercare i gruppi con messaggi nuovi, entrando in uno e tornando indietro la lista ripartiva dall'inizio: con decine di gruppi su due server voleva dire riscorrere tutto ogni volta. Non si registra la riga, ma CHE COSA c'era in cima, cosi' disiscriversi da un gruppo non manda la posizione in un punto a caso. (Segnalato da Jeff)

 ***  VERSIONE 1.4.6  ***

 Nove voci, e il cassetto e' vuoto. La prima voce di difetto aperta del progetto - il rispetto del Followup-To, segnalata da Dave Royal il 21 settembre - si chiude qui. Sei difetti su nove li ha trovati Marco in collaudo, dopo che gli era stato detto che era tutto a posto.

  * IL FOLLOWUP-TO non si perde piu' aprendo il selettore dei gruppi. Quando l'autore chiede che le risposte proseguano altrove, il selettore parte dai gruppi che ha indicato LUI invece che da quello che stai leggendo, e li mostra anche se non sei iscritto: li ha scelti qualcun altro, non puoi doverli sottoscrivere per rispondere dove ti e' stato chiesto.

    Cosi' una destinazione come "solo uk.test", che prima non era proprio esprimibile, si ottiene confermando senza aggiungere niente. E aprire il selettore per GUARDARE non butta piu' via la richiesta: era il caso peggiore, perche' il gesto di controllo distruggeva proprio cio' che si voleva controllare.

    Infine l'avviso in cima alla scrittura e la riga che dice dove uscira' il messaggio leggono adesso lo stesso valore: prima potevano smentirsi a una riga di distanza. (Segnalato da Dave Royal, con le prove 9A e 9B.)

  * LA VISTA A PANNELLI guadagna i comandi di spostamento, che fino a ieri esistevano solo nella vista a pagine: messaggio precedente e successivo, primo non letto, discussione precedente e successiva, inizio e fine del messaggio, "ricarica". Per arrivare al messaggio dopo bisognava scorrere l'elenco e toccare - e con un telecomando quello scorrimento e' la parte difficile della giornata.

    Le voci nuove nascono nel menu, per non cambiare la barra sotto le dita a chi se l'e' gia' sistemata; due tocchi nell'editor delle barre le portano fra le icone. Un comando che in un certo riquadro non si puo' usare si presenta spento, invece di non fare niente. (Nasce dal modo di leggere di Roxy, su un box TV.)

  * L'OGGETTO DEL MESSAGGIO non si riduce piu' a una colonna di lettere. Nella vista a pannelli su un riquadro stretto, l'intestazione e la barra si contendevano la riga: la barra veniva misurata per prima e si prendeva quello che chiedeva, al testo toccava cio' che avanzava - e su un riquadro piu' stretto della barra non avanzava niente.

    Il punto non era che la barra fosse lunga: era che a pagare fosse sempre l'unica cosa che devi poter leggere. Ora sotto una certa larghezza la barra scende sotto l'intestazione, e va a capo lei stessa invece di farsi tagliare l'ultima icona - che era il pulsante del menu, cioe' la via per arrivare a tutto il resto. (Trovato da Marco sulla configurazione che nessun collaudatore ha: i pannelli su un telefono tenuto in orizzontale.)

  * NeReO LEGGE IL FORMAT=FLOWED (RFC 3676), il modo in cui quasi tutti i lettori moderni dicono dove finisce davvero un capoverso. Fino a ieri lo indovinava: stimava la larghezza di impaginazione e univa le righe che sembravano appartenersi. Funzionava quasi sempre, e "sempre" e "quasi sempre" sono due cose diverse.

    Adesso, quando chi scrive lo dichiara e lo usa davvero, non si stima piu' niente: le righe che continuano le ha marcate lui, una per una. Per i messaggi che non lo dichiarano non cambia nulla.

  * "MOSTRA NELL'ELENCO", dalla lettura di un messaggio: porta all'elenco del gruppo, apre la discussione che lo contiene e lo evidenzia. Nasce da una frase esatta - "la ricerca funziona, ma ti apre il post, non ti fa vedere dove e'" - e aveva ragione alla lettera: da un risultato si finiva dentro il messaggio senza sapere in quale discussione fosse ne' chi gli stesse intorno. (Segnalato da BIG Umberto.)

  * L'ETICHETTA IN CIMA ALLA SCRITTURA dice dove andra' il messaggio, non piu' il gruppo che stavi leggendo. Con un Followup-To a un solo gruppo le due cose avevano smesso di coincidere, e la schermata dichiarava il falso a una riga di distanza dall'avviso che diceva il vero. (Prova 9A di Dave Royal.)

  * NEI PANNELLI, LA SCELTA DEI GRUPPI di risposta non viene piu' scartata. Il dialogo si apriva, sceglievi, e un istante dopo la richiesta dell'autore la scavalcava in silenzio: il messaggio usciva dove diceva l'intestazione, non dove l'avevi mandato tu. Non era un dialogo confuso: era una decisione dell'utente buttata via. (Prova 9B di Dave Royal.)

 ***  VERSIONE 1.4.5  ***

 Sei voci. Due erano rimaste indietro - parcheggiate in una lista che e' stata sostituita, e mai trasportate nella nuova - e una nasce da una segnalazione che sembrava estetica e non lo era.

  * SEGUIRE UNA DISCUSSIONE ora segue anche il suo futuro, non solo il passato. Fino a ora "segui la discussione" marcava i messaggi che esistevano nel momento in cui lo premevi: le risposte che arrivavano dopo nascevano non seguite, cioe' l'esatto contrario di quello che avevi chiesto.

    E non era solo la stella che mancava. Il marcatore protegge anche dai filtri e dalla cancellazione automatica, quindi i messaggi piu' nuovi di una discussione che stavi seguendo erano MENO protetti dei vecchi, e la discussione poteva svuotarsi dal centro senza dire niente.

    Ora una risposta eredita il marcatore da un qualsiasi antenato seguito, a qualunque profondita': regge anche se un messaggio in mezzo alla catena non e' mai stato scaricato.

    Vale per i messaggi che arrivano d'ora in poi. Le risposte gia' in archivio restano come sono: per rimettere in pari una vecchia discussione, togli e rimetti il "segui". (segnalato da BIG Umberto, che l'aveva definito "solo questione estetica" - non lo era)

  * UN GRAZIE A DAVE ROYAL, e non e' una formalita'. Dalla 1.4 in poi la maggior parte di cio' che e' stato corretto in NeReO nasce da una sua segnalazione: il Followup-To, il menu di aggiornamento, i menu dei tre puntini che si aprivano nel posto sbagliato, il "segna tutto letto" nella vista a pannelli, le risposte ai crosspost sul telefono.

    Quello che le rende preziose non e' il numero. Dice anche dove il difetto NON c'e', e fa risparmiare mezza indagine; ci corregge quando lo fraintendiamo invece di lasciar correre; accetta un compromesso e dice quale; manda disegni, non solo lamentele; e tiene casi di prova pronti su uk.test e it.test, cosi' la correzione dopo si verifica sullo stesso terreno.

  * "SEGNA TUTTO LETTO" torna nella barra della vista a pannelli, dove costa di nuovo un tocco invece di due. Era stata tolta nella 1.4.1 per una buona ragione - stava appiccicata al filtro dei non letti e si premeva per sbaglio - ma la vista a pagine aveva ricevuto una cura migliore per lo stesso problema: li' l'icona era stata SPOSTATA lontano dal filtro, non tolta. Ora i pannelli hanno la stessa cura, nella stessa posizione, e la voce esce dal menu: una strada sola per vista, e la stessa nelle due. "Segna tutto da leggere" resta nel menu, perche' quella e' rara davvero. (segnalato, due volte e su due cose diverse, da Dave Royal)

  * "RISPONDI SEMPRE A TUTTI I GRUPPI" funziona anche sul telefono. L'impostazione esiste dalla 1.2.7, ma veniva letta in un solo punto: l'apertura del dialogo del crosspost - e nella vista a pagine si va dritti in scrittura, senza passare da li'. Quindi era accesa e non faceva niente: la schermata mostrava il solo gruppo che stavi leggendo, finche' non aprivi il selettore a mano. Ora la risposta a un articolo crospostato nasce con tutti i gruppi dell'originale, fino al limite previsto. Se l'autore ha chiesto che si prosegua altrove, la sua richiesta continua a vincere. (segnalato da Dave Royal)

  * UN ARTICOLO SI PUO' SALVARE COME FILE .eml, che un client di posta apre come un messaggio invece di trovarselo incollato dentro. E' l'altra meta' di una richiesta della 1.3.6: allora la risposta fu la condivisione dell'articolo grezzo, che serve a leggerlo altrove ma non a conservarlo. Il file viene scritto con le intestazioni di codifica riscritte per corrispondere a quello che contiene davvero: NeReO salva i corpi gia' decodificati, quindi ricopiare il Content-Type originale avrebbe fatto mentire il file su se stesso e ogni accento sarebbe uscito sbagliato. E' un'azione di barra come le altre, quindi la puoi spostare nel menu o togliere da Aspetto -> Personalizza le barre. (richiesta da Adam H. Kerman)

  * L'INTRODUZIONE ALLA CITAZIONE puo' ora portare anche l'OGGETTO del messaggio citato, con %oggetto% (o %subject%). Si aggiunge ai segnaposto che c'erano gia' - autore, email, data, ora, gruppo e Message-ID - e vale anche nelle risposte normali, non solo citando da un altro gruppo. (richiesto da Ammammata)

 ***  VERSIONE 1.4.4  ***

 Cinque voci: tre chiudono segnalazioni di Roxy - due delle quali non erano "non funziona", erano "non c'e'" - e due arrivano da Dave Royal.

  * Il pulsante INCOLLA c'e' ora anche nel pannello di scrittura della vista a PANNELLI, dove non era mai stato. Tutto il lavoro fatto su quel pulsante - chiesto nella 1.3.3, reso sempre visibile nella 1.4.1, e insegnato nella build 106 a leggere gli appunti sul clic invece che mentre la barra si disegna - era stato fatto solo sulla vista a pagine, perche' nei pannelli quel pulsante non esisteva proprio e non c'era niente da correggere. (segnalato piu' volte da Roxy, che aveva ragione tutte le volte)

  * "RITIRA DAL SERVER (cancel)" compare ora anche nel menu contestuale della vista a pannelli. Nella 1.4.2 era stato lasciato fuori di proposito: si porta dietro tre dialoghi - conferma, attesa, esito per server - e ricopiarli sarebbe stato esattamente il difetto di cui parla questa versione. Sono stati spostati in un posto solo e ora le due viste eseguono lo stesso codice. Come prima compare solo sui tuoi messaggi, e come prima manda un messaggio di controllo su Usenet. (segnalato da Roxy)

  * Con un SOLO server configurato, l'icona di aggiornamento non chiede piu' quali server vuoi aggiornare: aggiorna e basta. Il menu continua a comparire quando i server sono piu' di uno, dove la scelta vuol dire qualcosa. Fino a ora si apriva comunque, offrendo due voci che facevano la stessa cosa e un dialogo in cui spuntare l'unico server che possiedi: non una scelta, una formalita'. (segnalato da Dave Royal)

  * IL MENU DEI TRE PUNTINI si apre sotto l'icona che hai toccato, anche nell'elenco delle discussioni della vista a pagine. Li' compariva all'estrema sinistra dello schermo: su un tablet, un palmo di distanza dall'icona. La causa: quelle azioni stanno in una riga tutta loro, larga quanto lo schermo, e un menu senza niente attorno si aggancia a chi lo contiene, quindi si misurava sulla riga intera. Nelle colonne strette della vista a pannelli lo stesso difetto e' invisibile, ed e' il motivo per cui nessuno l'aveva mai segnalato. Un censimento ha trovato dodici coppie icona+menu e otto senza un contenitore proprio: ora ce l'hanno tutte, non solo quella trovata. (segnalato da Dave Royal)

  * I GRUPPI OMONIMI SU SERVER DIVERSI tornano a distinguersi. Nella vista a PANNELLI la colonna dei gruppi era una lista unica che non diceva mai da quale server venisse un gruppo - e non era nemmeno ordinata per server, quindi i gruppi dei due server uscivano intercalati e due nomi identici potevano finire uno accanto all'altro. Ora ha un'intestazione per server, come la vista a pagine ha sempre avuto. E in ENTRAMBE le viste la barra sopra l'elenco delle discussioni dice il server sotto al nome del gruppo: fino a ora, una volta entrato nel gruppo, non c'era piu' niente a dirti quale dei due stavi leggendo. Con un solo server configurato non cambia nulla, perche' non c'e' niente da distinguere.

    Nello stesso giro due cose minori. L'intestazione mostra ora il NOME che hai dato tu al server e non il suo host: e' l'unico a essere diverso se hai due account sullo stesso provider. E i gruppi rimasti orfani di un server cancellato non spariscono piu' dalla lista - si raccolgono in fondo sotto "?", dove la pulizia del database puo' trovarli. (segnalato da Roxy)

 ***  VERSIONE 1.4.3  ***

 Tre correzioni, tutte e tre nate da un solo messaggio di un collaudatore che aveva acceso il registro diagnostico.

  * Le liste non diventano piu' SORDE. Poteva capitare - quasi sempre su un gruppo grande, e subito dopo un aggiornamento - che la lista aperta smettesse di mostrare i messaggi nuovi: il pallino del gruppo li contava, il database li conteneva, ma l'elenco restava fermo finche' non si usciva dal gruppo e si rientrava. Sotto c'era una lettura del database che falliva, e attorno una difesa scritta male: invece di rimettersi in ascolto, la lista si chiudeva per sempre. Ora si ripara da sola nel giro di un istante. Senza il registro diagnostico acceso e il log mandato da chi ci sbatteva contro, questo difetto sarebbe ancora un sospetto invece che una correzione. (segnalato e diagnosticato da BIG Umberto)

  * Il pulsante Annulla della diagnostica LISTGROUP funziona. Era disabilitato per tutto il tempo dell'operazione, e su un gruppo da centosettantamila articoli quell'attesa supera il minuto e mezzo: l'unico modo di uscirne era chiudere l'app di forza. Ora interrompe davvero, e quello che era gia' stato registrato resta nel log. (segnalato da BIG Umberto)

  * Le righe rimaste orfane di un server cancellato con la 1.4.1 o prima si possono finalmente eliminare. La 1.4.2 si limitava a contarle, in fondo al rapporto della Diagnostica database: la misura e' arrivata - tre gruppi e 99.906 righe di catalogo senza server, su un database da 36 MB - e con un numero vero davanti la pulizia ha smesso di essere un'idea mia. Il pulsante sta subito sotto il rapporto, e prima di cancellare ti dice quante righe se ne vanno, voce per voce. (misurato da BIG Umberto)

 ***  VERSIONE 1.4.2  ***

 Sette correzioni. Nessuna funzione nuova.

  * "Ricarica il messaggio" ricarica davvero. Nella 1.4 e nella 1.4.1 quel pulsante della barra di lettura CANCELLAVA l'articolo invece di riscaricarlo: spariva dall'elenco insieme allo stato letto, al "seguita", al punteggio dei filtri e al marcatore delle risposte a te, e non tornava piu' nemmeno aggiornando il gruppo, perche' l'aggiornamento riparte dall'ultimo articolo gia' scaricato. La causa era il nome di una funzione: eliminaCopiaLocale suonava come "togli la copia in cache" e invece faceva DELETE FROM articles. Ora il pulsante azzera il SOLO testo del messaggio e lo richiede al server, e quella funzione si chiama eliminaArticoloLocale, che e' quello che fa. (segnalato sul gruppo, con analisi tecnica allegata)

  * I tuoi post tornano "da leggere" come qualunque altro messaggio. La 1.4.1 li faceva nascere gia' letti: sembrava comodo e invece non lo e', perche' dopo aver pubblicato si vuole RITROVARE il proprio messaggio, per vedere com'e' venuto e dove si e' agganciato nella discussione, e con il filtro "solo non letti" acceso semplicemente non c'era. Nessun altro lettore si comporta cosi': ne' PAN, ne' Thunderbird, ne' Piao Hong, ne' MesNews. In piu' la regola era piu' larga di quanto sembrasse: alla prima sincronizzazione di un gruppo marcava letto TUTTO il tuo storico li' dentro. (segnalato da Dave Royal)

  * "Segna tutto letto" non si preme piu' per sbaglio. Un'azione rara e con conseguenze stava accanto a una frequente e innocua, il filtro dei non letti. Sul TELEFONO si scambia di posto con "Aggiorna"; nella VISTA A PANNELLI, dove la barra e' piu' fitta e la stessa azione era gia' nel menu dei tre puntini, l'icona in barra e' stata TOLTA: era un doppione, e cosi' il tocco per sbaglio diventa impossibile invece che meno probabile. Nei due layout le icone hanno ora lo stesso ordine. (segnalato da Dave Royal)

  * Il crosspost funziona anche aprendo una discussione NUOVA dalla vista a pannelli. Il selettore dei gruppi c'era solo rispondendo a un messaggio gia' crospostato: iniziando una discussione da zero non compariva, mentre nella vista a pagine c'e' sempre. E' la stessa forma di difetto che la 1.4 aveva corretto qui per le RISPOSTE, rimasta scoperta sull'altro ramo dello stesso if. (segnalato da Dave Royal)

  * "Sostituisci (supersede)" compare ora anche nel menu contestuale della vista a PANNELLI, dove non c'era mai stata: esisteva solo nella vista a pagine. Come prima appare solo sui tuoi messaggi. "Ritira dal server (cancel)" resta per ora nella sola vista a pagine: quella parte va prima riorganizzata per non averne due copie, e manda messaggi di controllo sulla rete, quindi merita un collaudo suo. (segnalato da Roxy)

  * Cancellare un server porta via davvero quello che gli appartiene, e prima te lo chiede, dicendo quanti gruppi e quanti messaggi se ne andranno. Fino alla 1.4.1 bastava un tocco sul cestino, senza domande, e spariva la sola riga del server: i suoi gruppi e tutti i suoi messaggi restavano nel database PER SEMPRE, invisibili e irraggiungibili, a occupare spazio e a gonfiare i numeri della diagnostica. Se ti e' gia' successo, la Diagnostica database in Manutenzione ora te lo dice: in fondo al rapporto compare un blocco "ORFANI" con i conteggi. Per ora li CONTA soltanto: toglierli sara' il passo successivo, e voglio prima sapere di quali numeri parliamo.

  * Eliminando un'intera discussione il pallino dei non letti del gruppo scende subito. Prima restava fermo al numero di prima fino al successivo aggiornamento: era l'unica cancellazione che si dimenticava di ricalcolarlo.

 ***  VERSIONE 1.4.1  ***

 Release di sole CORREZIONI dopo la 1.4 pubblica. Nessuna funzione nuova.

  * Il "letto" che segue il Message-ID ora funziona davvero. Era annunciato nella 1.4 e non funzionava per nessuno: la regola era collegata solo alle voci di menu "segna letto", non al fatto di leggere un messaggio. Ora ogni punto passa da un posto solo. Vale anche al contrario: "segna tutto da leggere" propaga a sua volta, e puo' riportare a non letti messaggi in altri gruppi. (Dave Royal)
  * I tuoi post non tornano piu' "da leggere" dopo l'invio. Nello stesso giro e' stato irrobustito il criterio "e' mio?" nei tre punti rimasti indietro rispetto all'audit della 1.2.12: l'indirizzo si estrae dalla riga From e si confronta, invece di cercarlo dentro la riga. (BIG Umberto)
  * Rispondere non fa piu' perdere il segno: rispondevi al 23esimo messaggio e tornavi al primo post. (BIG Umberto)
  * L'albero a comparsa segue "mostra i filtrati" come la lista del gruppo. Corregge anche il falso "impossibile ricostruire la discussione" e l'avanti/indietro che su un post filtrato non faceva niente. (Dave Royal)
  * In una discussione aperta il primo messaggio mostra il proprio stato invece di quello dell'intera discussione: niente piu' due righe rosse quando il non letto e' uno solo. (Dave Royal)
  * Il conteggio dopo un aggiornamento dice quanti dei nuovi sono stati filtrati, invece di annunciare arrivi che non vedrai mai. (Dave Royal)
  * Il pulsante Incolla in scrittura c'e' sempre, e ora funziona dove prima no. Compariva solo con gli appunti gia' pieni, e NeReO li leggeva mentre DISEGNAVA la schermata invece che alla pressione: da Android 10 leggere gli appunti richiede il focus di input, scriverli no. Sul telefono non si vedeva, perche' la comparsa della tastiera fa ridisegnare la barra e la seconda lettura riesce; senza tastiera a schermo quella seconda lettura non avviene mai. Ora si legge alla pressione, e nel modo tollerante che usano le altre app, cosi' arriva anche il testo copiato come HTML o come link. (diagnosi da due misure di Roxy)

  * Le intestazioni personalizzate che NeReO si riserva vengono dette PRIMA di inviare. Alcune non sono sovrascrivibili: quelle vitali (From, Subject...) e quelle di controllo (Followup-To, Control, Sender, Expires...), che esistono perche' un testo incollato da fonte esterna non diventi per distrazione un messaggio di controllo. Finora venivano scartate IN SILENZIO - si scriveva una riga valida, si inviava, e quella riga non c'era - e la nota sotto il campo portava proprio Followup-To come esempio. Ora l'editor le elenca mentre scrivi, e la stessa regola alimenta l'avviso e l'invio, cosi' i due non possono divergere. (segnalato da Dave Royal)

  * "Segna tutto letto" e "Aggiorna" si scambiano di posto nella barra della lista messaggi. Un'azione rara e con conseguenze stava accanto a una frequente e innocua - il filtro dei non letti - e veniva premuta per sbaglio. Spostarla nel menu sarebbe costato un tocco al giorno a chi la usa ogni sera; scambiarle non costa niente a nessuno. (segnalato da Dave Royal)


 ***  VERSIONE 1.4  ***

 Pubblicata il 17 settembre 2026.

  * Lo STARTTLS ora rispetta entrambe le meta' della RFC 4642, invece di una sola. Se un server RIFIUTA lo STARTTLS prima dell'handshake, NeReO puo' proseguire in chiaro come lo standard consente, e questo non cambia. Ma se il server ACCETTA (382) e poi l'handshake fallisce, la RFC 4642 (par. 2.2) chiede a entrambe le parti di CHIUDERE - SHOULD, cioe' una raccomandazione forte e non un obbligo assoluto - mentre fino alla 1.3.7 proseguivamo in chiaro. Quel ripiego probabilmente non funzionava nemmeno, perche' il ClientHello TLS era gia' stato scritto sul socket; ma il punto e' che non doveva essere tentato. Una connessione cosi' ora fallisce con un messaggio esplicito invece di proseguire in silenzio senza cifratura.
  * Il database e' stato aggiornato (schema versione 12). Server, gruppi, articoli, filtri, identita' e bozze vengono migrati sul posto e non viene cancellato nulla: e se mai NeReO trovasse una versione che non sa migrare, ne fa prima una copia fisica e te lo dice. Le strutture nuove non sono ancora usate da niente: ci sono per il lavoro su fiducia e certificati che arriva in questa stessa release. Come sempre prima di una versione che tocca il database, un backup da Manutenzione non costa nulla.
  * Un ripristino poteva assegnare a un server un nome utente che non aveva mai avuto. I server senza account finivano nel backup correttamente, come valore vuoto, ma venivano riletti come la parola "null": quattro caratteri, che NeReO usava poi diligentemente per autenticarsi su server che non vogliono alcuna autenticazione. Il server rispondeva "381 Enter password", NeReO mandava una password vuota, e il server replicava con un errore di sintassi che non diceva nulla a nessuno. Era un solo controllo mancante su sette campi dello stesso tipo, ed era li' almeno dalla 1.2.8. Ora il ripristino e' corretto, e i server che si portano ancora dietro quel nome utente finto vengono riparati al primo ripristino di un backup: il messaggio dice quanti.
  * NeReO non manda piu' una password vuota quando un server la chiede. Dice chiaramente che per quel server non ne e' salvata nessuna, e suggerisce di cancellare il nome utente se il server non richiede un account. Il controllo sta dove il server ha davvero chiesto la password (risposta 381): un server che si accontenta del solo nome utente risponde 281 e continua a funzionare, come vuole la RFC 4643.
  * In Manutenzione c'e' un nuovo riquadro "Diagnostica database". Conta le righe di ogni tabella e mostra la versione dello schema e lo spazio occupato, con un pulsante Copia. Esiste perche' una migrazione del database che SI APRE non e' la stessa cosa di una migrazione che ha FUNZIONATO: puo' essere formalmente corretta e aver comunque svuotato una tabella, e l'app si aprirebbe benissimo - sarebbe l'archivio a non esserci piu'. Prendi i numeri prima di un aggiornamento e di nuovo dopo: se coincidono, non si e' perso niente. E' anche la cosa piu' rapida da allegare a una segnalazione. Non viene calcolato niente finche' non premi il pulsante.
  * Rispondendo a un crosspost ora si vede dove il messaggio andra' davvero. Fino a ora la schermata di scrittura mostrava solo il gruppo che stavi leggendo, anche quando la risposta era impostata per uscire su tre. Con questo sono caduti altri due difetti: riaprire il selettore dei gruppi per CONTROLLARE la scelta la riportava in silenzio al valore predefinito (il caso peggiore possibile, perche' il gesto di controllo distruggeva cio' che controllava), e nella vista a pannelli il pulsante del crosspost non esisteva affatto, quindi per cambiare i gruppi bisognava abbandonare la risposta e ricominciare. Tutti e tre segnalati da Dave Royal.
  * "Tutti i messaggi" ora funziona anche sui server ad alta ritenzione. Chiedere l'intero storico di un gruppo molto grande faceva richiedere tutto l'intervallo in una volta sola, e la risposta poteva superare il tetto di 64 MB che protegge l'app dall'esaurire la memoria: errore, e nessun messaggio scaricato. Ora l'elenco si chiede a fette, e ogni fetta viene salvata prima di chiedere la successiva. Il tetto resta dov'era (alzarlo avrebbe spostato il muro, non tolto) e in piu' si guadagnano due cose: una sincronizzazione interrotta a meta' riprende invece di ricominciare, e la memoria usata non dipende piu' da quanto e' grande il gruppo. Segnalato da Bingo3331.
  * La fiducia TLS ora si concede, non si subisce. Il percorso normale non cambia. Cambia cosa succede quando FALLISCE: invece di un vicolo cieco, NeReO annota l'identita' che il server ha presentato e la segna VISTA, NON FIDATA, e la connessione fallisce lo stesso. L'approvi dopo, con calma, da Server -> Identita' TLS, mai in un dialogo in mezzo a un aggiornamento: un avviso di sicurezza che ti interrompe e' quello che si clicca senza leggere. Si appunta lo SHA-256 della chiave pubblica della foglia, mai di un intermedio. Da li' in poi quella chiave si confronta a OGNI connessione, e se cambia NeReO si ferma e te le mostra entrambe, senza nessun pulsante "accetta la nuova". L'attrito e' la funzione. C'e' anche un'eccezione "certificato scaduto" per riga server, che rilassa le date e nient'altro, e che si disinnesca da sola il giorno del rinnovo.
  * Avviso "il certificato scade fra N giorni". Il TrustManager vede il certificato a ogni connessione: la data di scadenza e' gratis, e non usarla sarebbe stato sprecare un dato gia' in mano. Nasce da un caso vero: un amministratore che rinnova A MANO, e un certificato Let's Encrypt che vive 90 giorni. Quella situazione non e' chiusa, e' ciclica.
  * Gli accenti non si rompono piu' negli articoli senza dichiarazione di charset o con una dichiarazione sbagliata. Su sette casi misurati, cinque perdevano il testo; il peggiore era la dichiarazione SBAGLIATA, che non produceva nemmeno un carattere d'errore ma spazzatura plausibile, che NeReO ripubblicava in UTF-8 impeccabile. Regola a tre passi, dieci casi verificati a ogni compilazione. E siccome NeReO salva il testo GIA' DECODIFICATO, c'e' una nuova azione "Ricarica il messaggio" per la barra dell'articolo: senza, la correzione sarebbe invisibile su tutto cio' che e' gia' in archivio.
  * Il "letto" segue il Message-ID. Un crosspost letto in un gruppo risulta letto anche negli altri: e' un messaggio solo, ed e' quello che fa MesNews. Discussioni seguite e filtri ragionavano gia' cosi', faceva eccezione solo il letto. Predefinito acceso.
  * Followup-To letto e rispettato. Era l'unico criterio GNKSA che NeReO non rispettava. Se l'autore chiede che le risposte proseguano altrove, la risposta va LI', e la schermata di scrittura lo DICE: dirottare senza avvisare sostituirebbe una scortesia con una sorpresa. Se apri il selettore dei gruppi e scegli tu, vince la tua scelta. Il caso "poster" non blocca la pubblicazione ma avvisa e offre l'indirizzo dell'autore.
  * Introduzione alla citazione per gruppo (richiesta di Henry The Mole). SOSTITUISCE quella dell'identita' invece di entrare nel sorteggio: serve una scelta mirata, tipicamente la lingua del gruppo.


 ***  VERSIONE 1.3.7  ***

 Due segnalazioni di Dave Royal sul ripristino del backup. Una era giusta, e il difetto era piu' esteso di quanto avesse visto.

  * Il ripristino non duplica piu' filtri e identita'. Lui aveva notato che i filtri venivano AGGIUNTI invece che sostituiti; guardando il codice e' emerso che le IDENTITA' avevano lo stesso difetto. Il ripristino e' ora IDEMPOTENTE: ripristinare due volte lo stesso file lascia esattamente lo stesso stato. I filtri si riconoscono da nome, campo, tipo di confronto, pattern e ambito dei gruppi; le identita' dal nickname, che il formato di backup usava gia' come chiave per rilegare i server.
  * Il ripristino ora chiede come procedere: Unisci o Sostituisci. Il difetto vero non era "aggiunge invece di sostituire": era che nella stessa operazione l'app faceva TRE cose diverse (dedup su server e sottoscrizioni, inserimento cieco su filtri e identita', sovrascrittura sulle impostazioni). Nessuna delle due opzioni tocca server, sottoscrizioni o articoli scaricati: cancellare un server porterebbe via con se' i suoi gruppi e i suoi messaggi.
  * "Password errata" non copre piu' gli errori di importazione. Il dialogo decifrava E importava in un unico blocco protetto: un guasto nel secondo passo veniva annunciato come un problema del primo, mandando a cercare la causa sbagliata.
  * L'a capo in invio non lascia piu' uno spazio in coda alle righe che spezza. Uno spazio a fine riga e' il marcatore di rottura debole del Format=Flowed (RFC 3676): un lettore che lo interpreta avrebbe riunito quella riga con la successiva, disfacendo proprio l'a-capo appena messo. Succedeva solo quando la rottura cadeva su un doppio spazio, tutt'altro che raro dopo un punto. I doppi spazi DENTRO la riga restano come li hai scritti, e il delimitatore della firma conserva lo spazio che la convenzione richiede. Domanda di Adam H. Kerman.
  * "File illeggibile" ora dice anche perche'. Quando un backup non si apriva, il messaggio era quelle due parole e nient'altro: un file mancante, un permesso negato e uno stream vuoto sembravano la stessa cosa. Ora il dettaglio tecnico compare in un dialogo con un pulsante per copiarlo, perche' e' quello che serve a una segnalazione. Lo ha fatto emergere Dave Royal, che ha ripetuto la stessa prova sei volte su due versioni dell'app senza poterci dire nulla di piu' di "file illeggibile" - che era tutto quello che gli mostravamo.

 ***  VERSIONE 1.3.6  ***

 Due difetti di conformita' e una funzione che mancava, tutti nati dalle domande di Adam H. Kerman su un gruppo anglofono. Escono prima della 1.4 perche' riguardano cio' che NeReO mette SULLA RETE: quello che finisce negli archivi altrui non si corregge dopo.

  * Oggetti lunghi con caratteri accentati o non latini: codifica corretta. La RFC 2047 ammette al massimo 75 caratteri per parola codificata; NeReO ne produceva UNA SOLA di lunghezza qualunque - misurato, un oggetto italiano normale usciva a 140 caratteri. Ora la codifica si spezza e si piega come vuole lo standard, tagliando solo fra caratteri interi perche' emoji e simboli multi-byte restino intatti.
  * us-ascii dichiarato quando il corpo e' davvero us-ascii (con 7bit), invece di dichiarare sempre UTF-8. Il controllo avviene DOPO che la firma e' entrata nel corpo: deciderlo un passo prima significherebbe dichiarare ASCII puro e poi spedire una firma con dentro un accento.
  * Nuova azione di barra: Esporta / condividi. Consegna l'articolo - intestazioni complete comprese - a un'altra app, cosi' puoi archiviarlo o rispondere all'autore per posta, cosa che NeReO non sa fare da se'. Sostituisce una condivisione sparita in sordina con la 1.3.4 insieme al vecchio menu di selezione.
  * Due nuove azioni di barra: Copia link (usenet.id) e Copia Message-ID. La prima costruisce un indirizzo web che punta all'articolo aperto - https://usenet.id/ seguito dal suo Message-ID - cosi' puoi mandare qualcuno a leggere proprio quel messaggio da una chat, una mail o un forum. La seconda mette negli appunti il Message-ID stesso, nella forma canonica, che finora si poteva ottenere solo aprendo le intestazioni complete e selezionandolo a mano. NeReO NON CONTATTA MAI usenet.id: compone il testo e lo copia - nessuna connessione, nessun permesso nuovo. Richiesta di ObiWan.
  * Preimpostazione corretta: OVH torna sulla porta 119 con STARTTLS, invece della 563 con TLS implicito. La 1.3.5 aveva spostato sulla 563 tutti e tre i server che richiedono un account, ma OVH non offre TLS su quella porta: sceglierlo dall'elenco integrato produceva una configurazione che non poteva funzionare. Il suo amministratore ha abilitato STARTTLS sulla 119 il 14 settembre 2026, e quella combinazione e' stata verificata su un dispositivo reale. Per i server gia' configurati non cambia nulla.

 ***  VERSIONE 1.3.5  ***

 Una sola cosa, e non e' una funzione: i messaggi d'errore del livello di rete ora sono in INGLESE per tutti.

  * Da dove nasce: un utente che usa NeReO con l'interfaccia in inglese si e' visto rispondere IN ITALIANO quando il suo server ha rifiutato la connessione ("483 Connessione non cifrata...") e si e' ragionevolmente chiesto cosa avesse sbagliato. Non aveva sbagliato niente.
  * Perche' succedeva: NntpClient e' volutamente privo di dipendenze Android - e' la scelta che lo rende provabile da riga di comando - e quindi non ha un Context e non puo' leggere strings.xml. I suoi messaggi sono scritti nel codice, e nessuna lingua di sistema li tocca. Non era una traduzione dimenticata: i tre strings.xml hanno parita' completa di chiavi.
  * Cosa si e' scelto, e cosa costa: se una lingua sola deve valere per tutti, tanto vale sia quella di values/ (il predefinito dell'app) e la lingua franca di Usenet. NON e' la localizzazione: e' un ripiego dichiarato, e il prezzo lo paga ora chi usa l'app in italiano, che leggera' in inglese meno di dieci messaggi su un'app per il resto tradotta. La soluzione vera - passare i testi dall'esterno di quel livello - si fara' quando quella classe verra' riaperta per altri motivi.
  * Il nome utente viene ripulito dagli spazi ai bordi, come gia' l'indirizzo del server. Uno spazio arrivato da un incolla o da un suggerimento della tastiera veniva spedito al server, che lo contava - e l'unico riscontro era "autenticazione fallita". La password NO, di proposito: puo' legittimamente iniziare o finire con uno spazio, e l'app non ha il diritto di modificarla in silenzio.
  * Campo password: pulsante per rivelarla e conteggio dei caratteri. Il conteggio c'e' perche' coglie cio' che rivelare la password non puo': uno spazio finale e' invisibile anche in chiaro. Se sai che la tua ha undici caratteri e il campo ne segna dodici, il problema e' trovato. E se davvero ha spazi ai bordi, l'app te lo dice in rosso invece di correggerti d'autorita'.
  * Il registro diagnostico NNTP annota la FORMA delle credenziali - quanti caratteri, spazi ai bordi, presenza di caratteri non ASCII - e MAI il valore. E' la differenza fra diagnosticare un accesso rifiutato leggendo un log e tirare a indovinare.
  * Scegliendo un server dall'elenco integrato, i tre che richiedono un account (Eternal September, Solani, OVH) nascono su porta 563 cifrata. Finora l'elenco proponeva a tutti la 119 in chiaro, quei tre compresi - e siccome NeReO si rifiuta di mettere una password su una connessione non cifrata, il primo tentativo falliva SEMPRE con un 483. Non era un caso limite: era il percorso normale, e chi sceglie da un elenco lo fa proprio per non dover decidere la porta. I due server aperti restano sulla 119. Per i server gia' configurati non cambia nulla.
  * Corretto un difetto che si presentava in due modi diversi. "Apri il primo non letto" a volte tornava al post iniziale - GIA' LETTO - pur essendoci messaggi da leggere; e marcare una discussione come letta ogni tanto la lasciava visibile. Erano lo STESSO difetto: NeReO rispondeva alla domanda "quali messaggi stanno in questa discussione" in TRE modi, e uno solo dei tre era il motore di threading che disegna davvero lo schermo. Gli altri due confrontavano il testo dell'intestazione References, e divergevano ogni volta che il post iniziale non era mai stato scaricato o che un messaggio elencava il solo padre diretto. Ora tutta l'app chiede al motore di threading.
  * Nota per chi ha segnalato: le risposte orfane erano esattamente la causa. I cambi di oggetto no - il motore di threading di NeReO non guarda mai l'oggetto, quindi rinominare un messaggio non puo' influenzare il raggruppamento.
  * La firma puo' contenere righe vuote fra le righe di testo. Quattro righe intercalate perdevano le ultime due, perche' il limite contava le righe FISICHE; ora conta le righe DI TESTO, con un tetto complessivo perche' una firma non diventi spazio vuoto. Da sapere: la netiquette classica conta quattro righe fisiche, vuote comprese - questa e' una tolleranza di NeReO, e un altro lettore potrebbe comunque troncarla.

 ***  VERSIONE 1.3.4 - RELEASE TECNICA  ***

 Del codice dell'app non e' cambiata una riga rispetto alla 1.3.3. E' cambiato tutto cio' su cui NeReO poggia.

  * Catena di compilazione migrata: Android Gradle plugin 8.7.3 -> 9.3.0, Gradle 8.9 -> 9.5.0, Kotlin 2.0.21 -> 2.2.10, KSP alla numerazione KSP2. Fatta con l'AGP Upgrade Assistant di Android Studio, non a mano: le versioni le ha scelte lo strumento in base a cio' che quella installazione regge davvero.
  * Jetpack Compose dalla 1.7.6 alla 1.12 - venti mesi in un colpo solo. compileSdk sale a 37 perche' Compose 1.12 lo pretende; targetSdk RESTA a 35, deliberatamente: alzarlo farebbe scattare restrizioni nuove del sistema operativo, e non vogliamo mescolare QUELLE con questo salto.
  * Perche' esce da sola, senza nient'altro dentro: venti mesi di Compose toccano il disegno del testo, lo scorrimento, la tastiera e le animazioni, cioe' tutta l'app. Se dopo questa qualcosa si comporta male, dev'esserci UN SOLO sospettato possibile. E' la stessa regola con cui e' uscita la 1.2.14.
  * Il passo intermedio e' stato provato sul campo: toolchain nuova con Compose ancora vecchia, compilata e installata - APK verificato, AGP 9.3.0 con Compose 1.7.6, tutto normale. Quindi AGP e Gradle sono gia' assolti: se qualcosa si rompe adesso, e' Compose.
  * Cosa speriamo, dichiarato per quello che e': nelle note di Compose 1.12 ci sono lavori espliciti sulla selezione col mouse e sulla barra di selezione. Su un dispositivo senza schermo tattile quella barra non e' mai comparsa. Questo e' l'unico modo per sapere se il problema fosse gia' risolto a monte. E' una PREVISIONE, non una promessa: se non si risolve, sapremo che quella strada non bastava - e il debito di venti mesi sara' tolto comunque.
  * Una cosa si e' persa, ed e' giusto dirlo: selezionando il testo di un messaggio non compaiono piu' "Cerca sul web" e "Condividi". Erano state aggiunte nella 1.2.8 su richiesta di Dave Royal, sostituendo il menu di selezione con uno nostro. Compose 1.12 non passa piu' da quel meccanismo: il menu ora e' quello della piattaforma, e il nostro veniva ignorato in silenzio.
  * Perche' non si possono ricostruire: l'API nuova permette di AGGIUNGERE voci a quel menu, ma non dice quale testo sia selezionato - verificato nel sorgente di androidx, non dedotto. Senza il testo, "Cerca" e "Condividi" non sanno cosa cercare ne' cosa condividere. Copia e Seleziona tutto non sono toccate, e l'azione "Copia il messaggio" nella barra di lettura resta.
  * SelectionToolbar.kt e' stato cancellato, non lasciato li' inerte: il suo commento continuava ad affermare che funzionava, e un file che mente e' peggio di un file che manca.
  * Se noti qualcosa di diverso dalla 1.3.3, segnalalo anche se sembra una sciocchezza. E' esattamente a questo che serve questa versione.

 ---

 ***  VERSIONE 1.3.3  ***

 Novita' di questa versione:
  * Quando una connessione cifrata fallisce, NeReO dice ora QUALE dei due problemi possibili sia: i rimedi sono diversi, e fino alla 1.3.2 li trattavamo come uno solo.
  * CATENA DI FIRMA: se il certificato non e' firmato da un'autorita' che Android riconosce (autofirmato, CA privata) o e' scaduto, il messaggio lo dice e avverte esplicitamente che l'opzione di sicurezza NON PUO' SERVIRE. Fino alla 1.3.2 la suggerivamo in ogni caso, mandando l'utente a cercare una via d'uscita inesistente.
  * NOME HOST: se invece il certificato e' valido ma emesso per un altro indirizzo, l'opzione serve davvero e il messaggio ci indirizza.
  * INDETERMINATO: se l'errore non corrisponde a nessuno dei due, NeReO lo dice invece di inventarsi una diagnosi, e mostra il testo grezzo dell'eccezione - il dato che serve a chi segnala.
  * L'opzione e' stata rinominata con quello che fa davvero: "Non verificare il nome host sul certificato". Il vecchio nome, "Consenti certificati non verificabili", prometteva piu' di quanto mantenesse: non ha MAI fatto accettare a NeReO un certificato autofirmato, di autorita' sconosciuta o scaduto - quello lo rifiuta il TrustManager di Android, che quell'interruttore non sfiora. Ora la nota sotto l'opzione lo dice esplicitamente.
  * Nessun cambiamento di comportamento: cio' che si collegava prima si collega ancora, cio' che veniva rifiutato viene rifiutato ancora. Cambia solo quello che NeReO DICE.
  * Segnalato da Bingo3331, che su Paganini ha fatto esattamente quello che il messaggio d'errore gli diceva di fare, ha visto che non funzionava e ne ha dedotto - ragionevolmente - che l'opzione fosse rotta. Non e' rotta: fa un'altra cosa. Il vicolo cieco l'avevamo costruito noi.

 ---

  * Nuova azione "Copia il messaggio" nella barra di lettura, sia sul telefono sia nel riquadro dei pannelli: UN CLIC copia l'intero messaggio negli appunti, senza dover prima selezionare nulla. E' personalizzabile come tutte le altre azioni (si puo' spostare nel menu tre puntini o spegnere). Insieme, un pulsante "Incolla" nella barra della schermata di scrittura.
  * Perche' serviva, e non era quello che pensavamo: il video di collaudo di Roxy chiude la questione. Sul suo Box TV la SELEZIONE del testo FUNZIONA - il blocco si evidenzia e si estende col trascinamento - ma la barra flottante Copia/Cerca/Condividi NON COMPARE MAI, nemmeno per un fotogramma in 22 secondi con la selezione ferma al centro dello schermo. Non e' un problema di posizionamento del popup, come avevamo ipotizzato: la barra non viene proprio chiamata.
  * L'indizio del meccanismo: mancano anche le MANIGLIE di selezione, che Compose disegna sul percorso a tocco - mentre sul telefono, col dito, maniglie e barra ci sono entrambe. Due dispositivi, due percorsi di input, segnali coerenti in direzioni opposte: e' il percorso MOUSE a non alzare la barra. E su Android il tasto destro e' BACK, quindi non c'e' nemmeno un menu contestuale su cui ripiegare.
  * Se hai una tastiera collegata, Ctrl+C e Ctrl+V funzionano gia', anche dove la barra flottante non compare - confermato da Roxy sul suo Box TV. Per copiare UNA PARTE di messaggio resta la via piu' rapida, e vale la pena saperlo: la funzione c'era gia', mancava l'informazione.
  * Il rimedio gira intorno al problema invece di combatterlo: azioni che non passano dalla selezione. Un clic, identiche a tocco e a puntatore.
  * Limite dichiarato dell'azione: copia il messaggio INTERO. Per copiarne un pezzo bisogna ancora selezionarlo - col dito e la barra, oppure con Ctrl+C.

 ---

 ***  VERSIONE 1.3.2  ***

 Novita' di questa versione:
  * Anche sul TELEFONO si puo' andare dritti al primo messaggio non letto. Nuova opzione facoltativa in Aspetto -> "Tocca la discussione per aprire il primo non letto": toccando la testa di una discussione si apre il primo messaggio non ancora letto, invece del primo post (se sono gia' tutti letti, si apre il primo post). Vale nella vista AD ALBERO e in quella RAGGRUPPATA.
  * Spenta di default: chi non la accende non nota alcuna differenza. Nella vista raggruppata, se e' accesa anche "Tocca il messaggio per espandere il thread", ESPANDERE HA LA PRECEDENZA - li' il thread si apre sul posto e restare nella lista e' la cosa sensata.
  * Perche' un'opzione nuova e non la stessa della 1.3.1: quella agisce solo dove una riga rappresenta l'intera discussione richiudibile (vista raggruppata), e nella vista ad albero - proprio quella per cui la funzione e' stata chiesta - quel presupposto non esiste. Riusare l'interruttore esistente non avrebbe potuto funzionare: sono due comportamenti diversi in due viste diverse, quindi due interruttori.
  * User-Agent nella forma prevista dalla RFC 5536 (nota tecnica di Dave Royal): ora NeReO/1.3.2 (Android), con la versione attaccata da una BARRA. Fino alla 1.3.1 era "NeReO 1.3.1 (Android)", con uno spazio.
  * Perche' non era solo estetica: la RFC 5536 par. 3.2.13 definisce product = [CFWS] token [ [CFWS] "/" product-version ], cioe' la versione si attacca al prodotto con la barra. Con lo spazio l'intestazione non era ILLEGALE - la grammatica ammette piu' prodotti e "1.3.1" e' un token valido - ma diceva un'altra cosa: DUE PRODOTTI, uno di nome "NeReO" e uno di nome "1.3.1". Chi raccoglie statistiche sui newsreader leggeva un software inesistente. Il commento "(Android)" e' CFWS ed era gia' corretto.
  * Le identita' gia' salvate vengono riallineate una volta sola, e SOLO se contengono ancora esattamente uno dei predefiniti spediti da noi. Uno User-Agent scritto dall'utente non viene mai toccato - campo vuoto compreso, che dalla 1.2.9 significa "non dichiarare nulla" ed e' una scelta, non una dimenticanza.
  * Corretto: se l'ultimo articolo di un gruppo veniva cancellato, quel gruppo non si aggiornava piu'. Ogni tentativo rispondeva "423 No articles in NNNN-NNNN" e si sbloccava da solo soltanto quando qualcuno postava di nuovo.
  * Perche' succedeva: cancellato l'ultimo articolo, il numero alto dichiarato dal gruppo NON scende (RFC 3977 par. 6.1.1: non tutti i numeri fra il water mark basso e quello alto corrispondono a articoli esistenti), quindi NeReO chiedeva l'intervallo dal proprio segnalibro fino a quel numero - ormai vuoto. Il 423 che il server restituisce NON e' un errore: la RFC 3977 par. 8.3 lo definisce "nessun articolo in quell'intervallo", ed e' la risposta giusta. NeReO lo trattava come fatale.
  * Cosa costava davvero, oltre all'errore a schermo: la connessione condivisa veniva buttata e riaperta per i gruppi rimanenti (socket + TLS + autenticazione da capo, a ogni aggiornamento), il gruppo veniva saltato dalla guardia anti-loop, e il segnalibro non avanzava mai - quindi l'aggiornamento dopo richiedeva lo stesso intervallo morto. Ora il 423 produce una lista vuota, il segnalibro avanza e l'intervallo morto si consuma una volta sola.
  * Non era un caso di laboratorio: il buco fra numero basso e numero alto e' previsto dal protocollo, e i cancel di spam sull'ultimo articolo di un gruppo poco trafficato sono ordinaria amministrazione. Il test deliberato di BIG Umberto su alt.test.a, riprodotto tre volte su tre server diversi (Solani, eternal-september, Paganini), non ha inventato uno scenario impossibile: ha reso riproducibile una cosa che capita da sola e che finora poteva essere archiviata come "errore di connessione".
  * Sotto il cofano: prima di aggiungere il QUINTO punto del codice che calcola "primo non letto della discussione", la logica e' stata estratta una volta sola e i cinque punti ora la condividono. La 1.3 e' uscita con un difetto proprio perche' una correzione era stata applicata a due copie su quattro di un blocco duplicato.

 ---

 ***  VERSIONE 1.3.1  ***

 Novita' di questa versione:
  * Nel layout a pannelli, toccare una discussione ora la espande E ci va: con l'opzione "Tocca l'OP per espandere" attiva, il tocco apre nel riquadro di lettura il primo messaggio NON LETTO di quella discussione (o il primo post, se sono gia' tutti letti). Chiudendo la discussione la selezione torna sull'OP, l'unica riga rimasta visibile.
  * Perche' era un problema serio e non un fastidio: prima il tocco espandeva soltanto, e il primo post di una discussione con risposte non si poteva aprire affatto. Nella 1.2.9 avevamo indicato come via d'uscita il menu a pressione prolungata, ma su un Box TV col telecomando quel gesto NON ESISTE: per chi legge da li' quel messaggio era irraggiungibile. Segnalato da Roxy, che quell'opzione l'aveva chiesta lui stesso nella 1.2.9.
  * Ogni riga dell'elenco discussioni ha ora un pulsante "..." (tre puntini verticali) che apre il suo menu contestuale - apri, plonk, sostituisci, ritira, conservazione, selezione multipla - con UN SOLO CLIC. La pressione prolungata continua a funzionare esattamente come prima: e' una via in piu', non una sostituzione. Nasce dalla stessa segnalazione: su un dispositivo guidato da un puntatore (mini tastiera con trackpad) tenere premuto un tasto e' tecnicamente possibile, ma cosi' lento che - parole di Roxy - "passerebbe la voglia di usarlo".
  * La barra del riquadro di lettura nei pannelli e' ora personalizzabile, come gia' quella del telefono (Aspetto -> Personalizza barra di lettura). Con una differenza voluta: qui "spento" non vuol dire NASCOSTO ma SPOSTATO NEL MENU tre puntini - nulla diventa irraggiungibile. Nasce gia' con ROT13 e Intestazioni nel menu, perche' su un tablet tenuto in verticale il riquadro e' stretto e cinque pulsanti non lasciavano quasi spazio all'oggetto del messaggio. Suggerimento di Dave Royal, gia' chiesto in precedenza da Roxy.
  * Solo nel layout a pannelli, di proposito: sul telefono il tocco naviga via, quindi "espandi e vai" sarebbe una contraddizione, e li' la pressione prolungata e' un gesto naturale, non un ostacolo.

 ---

 ***  VERSIONE 1.3  ***

 Novita' di questa versione:
  * Corretta una lacuna della 1.2.17: "Invia la password solo quando il server la chiede" NON veniva applicata all'invio di un messaggio ne' allo scaricamento dell'elenco gruppi, che continuavano ad autenticarsi subito. Causa: lo stesso blocco di codice esisteva in quattro copie e ne erano state aggiornate due. I quattro punti che aprivano una connessione sono ora unificati in uno solo, cosi' la regola vale ovunque per costruzione.
  * Per il resto la 1.3 e' la 1.2.17 provata sul campo: e' la versione stabile che raccoglie tutta la linea 1.2.12-1.2.17 - il giro di irrobustimento nato dall'audit di sicurezza, la verifica del nome host TLS, ritiro e sostituzione con esito server per server, e tutto cio' che e' emerso da due giorni di prove reali su tre provider.
  * La 1.3 e' la PRIMA VERSIONE PUBBLICA dopo la 1.2.11: chi aggiorna da li' riceve in un colpo solo anche tutto il contenuto delle versioni 1.2.12-1.2.17, che non sono mai state distribuite (vedi la nota subito sotto lo storico).

 ---

 ======================================================================
  ATTENZIONE - LE VERSIONI DA 1.2.12 A 1.2.17 NON SONO MAI STATE
  PUBBLICATE
 ======================================================================

 L'ultima versione distribuita pubblicamente su SourceForge e' stata la 1.2.11.

 Tutto quello che segue qui sotto, dalla 1.2.12 alla 1.2.17, e' stato compilato e collaudato SOLO all'interno del progetto: su tre server reali (Astraweb, usenet.ovh, eternal-september) e su un solo dispositivo. Nessuna di quelle versioni e' mai arrivata agli utenti, e non esiste alcun APK pubblico che le contenga.

 Sono elencate perche' il loro contenuto e' TUTTO dentro la 1.3: servono a capire cosa e' cambiato e perche', non a cercare un aggiornamento che non e' mai esistito. Chi viene dalla 1.2.11 passa direttamente alla 1.3 e riceve tutto in un colpo solo.

 ---

 ***  VERSIONE 1.2.17  ***

 Novita' di questa versione:
  * Nuova opzione "Invia la password solo quando il server la chiede" (Manutenzione -> Sicurezza), SPENTA di default. Di norma NeReO si autentica appena si collega, ad ogni connessione; con questa accesa aspetta che sia il server a chiederlo, si autentica in quel momento e ripete il comando. Su un server che non riesce a cifrare la connessione cambia molto: i server INN lasciano leggere senza credenziali e le pretendono solo per pubblicare, quindi la password attraversa la rete qualche volta al mese invece che decine di volte al giorno. Spenta di default perche' tocca il modo in cui si contatta OGNI server: se uno smette di funzionare, si rispegne e si torna esattamente a prima.
  * Nell'elenco dei server compare l'avviso "ultimo collegamento NON cifrato" sotto i server che stanno parlando in chiaro. Si basa su cio' che e' realmente successo, non sulle impostazioni: un server puo' essere configurato per cifrare, dichiararlo, e non riuscirci comunque. E' un segno permanente e non un avviso ricorrente, perche' quello stato non cambia da solo e un avviso che torna ogni volta si impara a ignorare.
  * Il registro diagnostico non conta piu' come errori le risposte attese: un ritiro riuscito chiudeva la sessione con "1 errori", perche' il 430 "no such article" - che e' esattamente la risposta cercata - veniva contato come guasto.

 ---

 ***  VERSIONE 1.2.16  ***

 Novita' di questa versione:
  * Il ritiro (cancel) ora dice com'e' andata su OGNI server configurato, non solo su quello da cui hai pubblicato. Al termine compare un elenco - ritirato, ancora presente, non verificabile - con il server a cui la richiesta e' stata effettivamente inviata marcato come tale. E' la risposta che finora si poteva ottenere solo controllando a mano, un server alla volta, da un altro programma.
  * Il dialogo di conferma dice prima quello che si scopriva dopo. Quattro prove sul campo fra il 6 e il 7 settembre hanno stabilito che il ritiro agisce, in pratica, solo sul server su cui hai pubblicato: sugli altri il messaggio resta, e molti provider ignorano del tutto le richieste di ritiro. Ne discende un consiglio, ora scritto nella conferma: se una correzione deve essere vista da tutti, lo strumento giusto e' "Sostituisci" e non "Ritira" - il supersede e' un articolo normale e si propaga sempre, il ritiro e' un messaggio di controllo e quasi nessuno lo inoltra.
  * Nessun lavoro in sottofondo: i server vengono interrogati solo in quel momento, una connessione ciascuno, e uno irraggiungibile diventa "non verificabile" senza far fallire il resto.

 ---

 ***  VERSIONE 1.2.15  ***

 Novita' di questa versione:
  * "Ritira dal server (cancel)" ora dice SE HA FUNZIONATO. Prima l'app diceva soltanto "richiesta inviata, il server potrebbe ignorarla": chi ritirava un messaggio su un server che ignora i messaggi di controllo restava convinto di averlo tolto. Ora, subito dopo l'invio, NeReO chiede al server se quell'articolo c'e' ancora e riferisce la risposta: ritirato, ancora presente, oppure non verificabile. Tre esiti e non due, perche' sul campo li abbiamo visti tutti e tre.
  * Un messaggio che sul server non esiste piu' (scaduto, o ritirato dal suo autore) viene detto per quello che e', invece di comparire come un errore di rete qualunque. Accanto alla spiegazione c'e' il pulsante "Rimuovi la copia locale", per togliere la riga di riepilogo rimasta orfana. Sia nella vista a pagine sia nel pannello di lettura.
  * Supersedes gestito in lettura: quando un messaggio ne sostituisce un altro, il sostituito viene NASCOSTO (non eliminato: "Mostra nascosti" lo riporta), e solo se i due vengono dallo stesso indirizzo. Senza quel controllo chiunque potrebbe far sparire il messaggio di un altro dalla vista di chi legge.
  * Correzioni tecniche invisibili: una normale risposta "articolo non trovato" non viene piu' scambiata per una connessione caduta (difetto della 1.2.13, costava una riconnessione inutile); un server indicato con l'indirizzo IP non produce piu' un errore incomprensibile; cambiando gli interruttori di sicurezza viene ora chiusa subito la connessione aperta con le impostazioni precedenti.

 ---

 ***  VERSIONE 1.2.14  ***

 Novita' di questa versione:
  * VERIFICA DEL NOME HOST NELLE CONNESSIONI CIFRATE (TLS). Fino alla 1.2.13 il traffico verso il server era cifrato, ma nessuno controllava che il certificato presentato appartenesse davvero all'indirizzo richiesto: una rete ostile poteva mettersi in mezzo e leggere tutto, credenziali comprese, senza che l'app se ne accorgesse. Ora il controllo c'e', su entrambe le strade (TLS implicito e STARTTLS), come lo fa un browser.
  * Se un server ha il certificato configurato male, la connessione fallisce con un messaggio esplicito che dice cosa e' successo e dove intervenire, non con un errore di rete generico.
  * Nuovo interruttore "Consenti certificati non verificabili" (Manutenzione -> Sicurezza), SPENTO di default e con avviso in rosso: chi sa che il proprio provider ha il certificato messo male puo' collegarsi lo stesso. E' globale, non per-server, sul modello gia' usato dalla 1.2.6 per l'autenticazione non cifrata.
  * Questa versione esce DA SOLA, di proposito: e' l'unica correzione dell'audit di sicurezza che puo' far smettere di funzionare una configurazione che oggi funziona. Se dopo l'aggiornamento un server non si connette piu', la causa e' senza ambiguita' questa e la cura e' l'interruttore qui sopra.

 ---

 ***  VERSIONE 1.2.13  ***

 Novita' di questa versione:
  * Niente piu' errore di rete al posto del corpo di un messaggio passando in fretta da uno all'altro. La connessione tenuta aperta per la lettura veniva lasciata cadere dal server mentre si leggeva, e il primo comando dopo la pausa finiva nel vuoto: ora, se un errore di rete capita su una connessione RIUSATA, questa viene buttata e il messaggio riletto una volta con una connessione nuova, in silenzio. Diagnosticato col log della Diagnostica NNTP.
  * User-Agent precompilato nelle identita' NUOVE (richiesta di Merovingio): il campo nasceva vuoto su un'app appena installata, quindi chi non lo compilava postava anonimo senza averlo scelto. Ora contiene %nereo%, che all'invio diventa nome e versione in uso (e resta aggiornato da solo ad ogni release). Chi vuole restare anonimo lo svuota: le identita' GIA' ESISTENTI non vengono toccate.
  * La verifica del nome host nelle connessioni cifrate slitta alla 1.2.14: resta l'unica correzione che esce da sola.

 ---

 ***  VERSIONE 1.2.12  ***

 Novita' di questa versione:
  * Giro di irrobustimento generale, nato da un audit di sicurezza su tutto il codice. NeReO regge senza scomporsi articoli e risposte del server malformati o costruiti apposta: messaggi fuori misura, intestazioni con a-capo infilati dentro, catene References abnormi, immagini Face anomale, ripristini da file di backup manomessi. Nell'uso normale non cambia nulla - sono difese che si notano solo quando qualcosa prova ad approfittarne.
  * "Sostituisci (supersede)..." compare ora solo sui PROPRI messaggi, e accanto c'e' il nuovo "Ritira dal server (cancel)...": invia un messaggio di controllo che chiede al server di ritirare un articolo che hai pubblicato. Molti server ignorano queste richieste e la propagazione agli altri server non e' mai garantita: il dialogo di conferma lo dice chiaramente, invece di lasciartelo scoprire dopo.
  * NON inclusa qui: la verifica del nome host nelle connessioni cifrate. E' l'unica correzione che puo' far smettere di funzionare una configurazione che oggi funziona (capita che un provider presenti un certificato con nome non combaciante), quindi esce da sola nella versione successiva - cosi', se qualcosa non si connette piu', si sa subito di chi e' la colpa.

 ---

 ***  VERSIONE 1.2.11  ***

 Novita' di questa versione:
  * Aggiornata la libreria del database interno (Room) da 2.6.1 a 2.8.4: tentativo di correzione a monte del raro crash CursorWindow del Dato 5 (vedi versione 1.2.10 qui sotto). La 2.8.4 include due correzioni ufficiali su corse fra Flow di lettura e scritture concorrenti, la stessa categoria di quel crash - non e' una conferma della causa esatta, resta un'ipotesi verificata solo in parte, e le difese gia' introdotte nella 1.2.10 restano attive comunque, come rete aggiuntiva. Nessun cambiamento visibile all'utente.
  * Estesa la stessa difesa (elenco precedente al posto del crash) anche alla ricerca testuale, che condivideva lo stesso rischio ma non era ancora protetta.

 ---

 ***  VERSIONE 1.2.10  ***

 Novita' di questa versione:
  * Elenco articoli piu' robusto: un raro errore di lettura dal database (IllegalStateException del CursorWindow di Room), che poteva capitare uscendo da un gruppo mentre un sync scriveva ancora nuovi messaggi, non manda piu' in crash l'app - ora, se capita, mostra l'ultimo elenco valido invece di chiudersi. Prima versione reale a portare questa difesa: era pronta dal 5 settembre, in attesa di un giro di consegna.
  * Nuovo interruttore "Aggiorna automaticamente aprendo un gruppo" (Manutenzione, segnalazione di un tester tramite Marco): entrare in un gruppo (anche solo rientrando da un articolo appena letto) lo sincronizzava sempre, senza un modo per disattivarlo - chi si distraeva leggendo si ritrovava le discussioni lasciate a meta' mescolate a quelle nuove. Ora si puo' spegnere, tenendo tutto fermo finche' non si lancia il sync a mano (pulsante/tira giu' per aggiornare, sempre disponibili). Default acceso: nessun cambiamento per chi non ne ha bisogno.

 ---

 ***  VERSIONE 1.2.9.c  ***

 Novita' di questa versione:
  * Sincronizzazione piu' robusta: eliminata una possibile corsa a tempo fra il controllo automatico dei nuovi messaggi e altre sincronizzazioni in corso (manuali o del timer), che poteva contribuire a un arresto anomalo occasionale - segnalazione dal collaudo pubblico ancora in indagine (vedi la 1.2.9.b sotto per il log diagnostico). Non e' una correzione CONFERMATA del crash specifico (manca ancora una prova diretta), ma un difetto di concorrenza reale e dimostrato nel codice.

 ---

 ***  VERSIONE 1.2.9.b  ***

 Tre correzioni emerse dal collaudo pubblico della 1.2.9, piu' un nuovo strumento diagnostico: prima vera nuova versione da compilare e distribuire agli utenti (le correzioni precedenti erano rimaste "a versione invariata" perche' la 1.2.9 non era ancora stata pubblicata).

 Novita' di questa versione:
  * Ricerca: la lista dei risultati non si azzera piu' riaprendo un risultato (segnalazione di Marco): tornando da un messaggio aperto dalla ricerca, la query e i risultati restano quelli di prima invece di ripartire vuoti - prima bisognava rifare la stessa ricerca ogni volta per raggiungere il risultato successivo.
  * "Mostra filtrati" e' ora un audit completo (segnalazione di Marco: "se sono filtrati perche' mai dovremmo vederli?" -> "per me e' un audit rotto"): applicando i filtri ai messaggi gia' scaricati, anche quelli che corrispondono a una regola "Ignora" (non solo "Elimina del tutto") vengono ora marcati e mostrati in grigio - prima venivano solo eliminati i secondi, i primi restavano invisibili all'audit.
  * User-Agent vuoto = post davvero anonimo (segnalazione di Marco): lasciando vuoto il campo User-Agent nell'identita', ora il messaggio inviato non porta piu' nessuna intestazione User-Agent, come il testo di aiuto ha sempre promesso - prima veniva comunque sostituito in silenzio col valore predefinito dell'app.
  * Nuovo: registro dell'ultimo arresto anomalo (Manutenzione): se l'app dovesse mai chiudersi in modo improvviso, l'ultimo crash viene salvato da solo sul dispositivo - nulla parte automaticamente. Se capita, copialo o esportalo dalla nuova sezione "Ultimo arresto anomalo" e mandalo a chi sviluppa: senza questo, un crash segnalato senza altri dettagli non e' diagnosticabile.

 ---

 ***  VERSIONE 1.2.9  ***

 Multi-identita' vera (richiesta di Marco: "un multi utente reale, come in MesNews e/o NR MOD17 di Piao Hong") piu' il resto della roadmap NNTP rimasta (voce E), nella stessa release - piano completo scelto esplicitamente da Marco invece che per gradi. Chiude il gate verso la 1.3 definitiva, in attesa solo del collaudo su device.

 Novita' di questa versione:
  * Multi-identita': crea quante identita' vuoi (nome, email, firma, User-Agent, X-No-Archive, intestazioni personalizzate, introduzione alla citazione, Face) e assegna ciascuna a un server preciso - scrivi come "MCSM" su un server e come "TAF" su un altro, ognuna col proprio indirizzo e la propria Face. Nuove schermate Identita' (elenco + editor); l'assegnazione a un server si fa modificando il server. Chi aveva gia' un'identita' generale o override per-server nelle versioni precedenti se li ritrova migrati automaticamente, senza dover rifare nulla.
  * STARTTLS (RFC 4642): i server rimasti su una porta in chiaro (di solito 119) possono ora passare a una connessione cifrata subito dopo la connessione, in alternativa al TLS implicito.
  * CAPABILITIES: letto e registrato in diagnostica alla connessione.
  * Lettura piu' veloce: leggendo un thread, il corpo degli articoli riusa la stessa connessione invece di riconnettersi ad ogni singolo articolo.
  * Override per server dell'autenticazione non cifrata: per un singolo server puoi forzarla sempre consentita o sempre bloccata, indipendentemente dall'impostazione globale (Manutenzione -> Sicurezza).
  * Backup e ripristino portano ora anche le identita' e le nuove opzioni per server; i vecchi backup restano leggibili come prima.
  * Citazioni molto annidate - soglia numerica (Aspetto, richiesta di Marco): oltre una certa profondita' (default 8, configurabile fra 6/8/10/12) i ">" ripetuti diventano il numero del livello ("9", "10", "11"...) invece di allungarsi all'infinito - solo nella resa a schermo, con stile citazioni "chevron"/"entrambi": il testo che arriva e quello che parte resta sempre con i ">" standard, quindi la profondita' si legge correttamente ovunque, anche in un altro client. Attivo di default.
  * Tocca il messaggio per espandere il thread (Aspetto, opzionale, richiesta di Roxy): in alternativa al pulsante +/- (ingrandito a 48dp minimo per chi mira da lontano, es. TV box col telecomando), il tocco sul testo del primo messaggio di una discussione con risposte puo' espanderla/comprimerla invece di aprirla; il messaggio resta comunque raggiungibile dal menu (tocco prolungato) -> "Apri". Spento di default.
  * Bozze spostate dentro il gruppo (richiesta di Marco): "Bozze" non e' piu' nel menu principale - si apre dal menu del gruppo a cui si sta scrivendo, gia' filtrata sulle bozze di quel gruppo (ogni bozza sa gia' a quale gruppo appartiene). Un interruttore "Questo gruppo"/"Tutti i gruppi" dentro lo schermo permette di vedere comunque tutte le bozze insieme, quando serve.
  * Swipe verticale: ora passa anche da una discussione all'altra (segnalazione di Marco): nella lettura, con "solo non letti" spento lo swipe in fondo/inizio messaggio si fermava zitto all'ultimo/primo post della discussione invece di proseguire - diverso da quando "solo non letti" e' acceso, che gia' saltava alla discussione successiva. Ora lo swipe verticale si comporta allo stesso modo in entrambi i casi (le frecce singole < > restano bloccate dentro la discussione corrente, come sempre - per quello ci sono le doppie frecce).

 ---

 ***  VERSIONE 1.2.8  ***

 Tappa intermedia fra la 1.2.7 e la futura 1.3 "completa": raccoglie tutto cio' che era stato preparato come 1.3/1.3.1/1.3.2 durante il collaudo in solitaria, rinumerato prima di qualunque diffusione piu' larga - nessuna di quelle versioni e' mai uscita ufficialmente (quei numeri sono poi stati riusati dalle versioni reali descritte piu' in alto: sono omonimi, non le stesse build). La 1.3 vera arrivera' solo quando anche il resto della roadmap NNTP (CAPABILITIES, STARTTLS, pool di connessioni, override auth-non-cifrata per-server) sara' completato o scartato.

 Novita' di questa versione:
  * Barra di lettura personalizzabile: in Aspetto -> "Personalizza barra di lettura" scegli quali icone mostrare nella barra bassa della lettura e in che ordine (interruttore mostra/nascondi + frecce su/giu', comode anche col telecomando dei TV box).
  * Aggiornamento selettivo: l'icona di aggiornamento ora apre un menu - "Aggiorna tutto" (come prima) oppure "Scegli quali server aggiornare". Sull'intestazione di ogni server un menu simile permette di aggiornare solo quel server o solo alcuni suoi gruppi, invece di scaricare sempre tutto. In tablet/pannelli e' disponibile il livello server.
  * Lettura a tutto schermo nei pannelli (tablet): mentre leggi, una nuova icona nasconde temporaneamente le colonne gruppi e discussioni per concentrarti sul testo; si richiude da sola uscendo dall'articolo.
  * Cerca/Condividi sul testo selezionato: selezionando del testo in un articolo, oltre a Copia e Seleziona tutto compaiono anche Cerca (ricerca web) e Condividi. "Traduci" e' stato scartato DEFINITIVAMENTE (dipenderebbe da un gestore non garantito su ogni device).
  * Controllo aggiornamenti a quattro modalita': in Manutenzione -> Aggiornamenti scegli fra Mai / Una volta al giorno / Ogni 4 ore / Ad ogni apertura. Sotto il selettore compare sempre "Ultimo controllo: ..." cosi' si vede che il controllo funziona anche quando non trova nulla di nuovo (nessun avviso in quel caso, solo quel testo).
  * Corretto un bug: il vecchio limite "una volta al giorno" era per giorno di calendario, non per tempo trascorso - chi apriva l'app anche una sola volta prima che una nuova versione fosse pubblicata restava "bloccato" fino al giorno dopo, qualunque numero di volte riaprisse l'app nel frattempo.
  * Diagnostica NNTP potenziata (Bingo3331 + HAL): col log NNTP acceso (Manutenzione -> Diagnostica), ora si registra anche un riepilogo di ogni risposta OVER/XOVER (righe ricevute, numeri mancanti, duplicati), i tempi di ogni comando, e un'intestazione/piede per ogni sessione NNTP - pensato per capire i buchi nel backfill di provider come Astraweb.
  * Nuova diagnostica manuale "Diagnostica LISTGROUP...": chiede al server l'elenco vero degli articoli di un gruppo e lo confronta con l'ultimo riepilogo overview; sui numeri assenti, verifica con STAT se esistono davvero - distingue un buco nel database overview del server da un articolo davvero scaduto o cancellato.

 ---

 ***  VERSIONE 1.2.7  ***

 Novita' di questa versione:
  * Connessione non sicura: se un server rifiuta la password su una connessione non cifrata, ora compare un avviso chiaro con due scelte - "Passa a SSL" (consigliato) oppure "Consenti non cifrata" (sconsigliato). Niente piu' fallimento muto per chi e' alle prime armi. L'avviso non compare durante il controllo automatico a intervalli, solo quando aggiorni tu.
  * Risposta ai crosspost: nuova opzione in Manutenzione per rispondere di default a TUTTI i gruppi dell'originale invece del solo gruppo corrente (spenta di default; puoi comunque scegliere caso per caso).
  * Vista continua: un separatore piu' spesso segna ora l'inizio di ogni nuova sotto-discussione, cosi' si distinguono a colpo d'occhio.

 ---

 ***  VERSIONE 1.2.6  ***

 Novita' di questa versione:
  * Sicurezza: la password non viene piu' inviata su una connessione non cifrata (viaggerebbe in chiaro - RFC 4643 lo vieta). Se hai davvero un vecchio server che lo richiede, puoi consentirlo consapevolmente in Manutenzione -> Sicurezza. Preferisci sempre la porta SSL (di solito 563).
  * "Controlla aggiornamenti" ora e' anche nel menu principale, accanto a Informazioni e Novita' - niente piu' trafila in Manutenzione.
  * Vista a pannelli: menu a tre punti su ogni post, con il plonk dell'autore. Prima bisognava passare alla vista a schermo intero.
  * Vista a pannelli: si possono riordinare i gruppi anche qui (icona "riordina" in cima alla colonna gruppi), non piu' solo a schermo intero.
  * Il backup ora salva l'ordine dei gruppi: al ripristino non torna piu' tutto in ordine alfabetico.
  * Vista Intestazioni piu' pulita: il lungo codice base64 di Face/X-Face non viene piu' mostrato (la faccina resta accanto all'autore).
  * Muta citazioni: un nuovo pulsante nasconde il testo citato con un tocco, cosi' resta in evidenza solo il messaggio nuovo (un tocco sul segnaposto lo rimostra). Icona "virgolette" nella barra di lettura e nelle intestazioni dei pannelli.
  * Vista a pannelli: nel pannello di lettura lo swipe su/giu' salta al post non letto successivo/precedente della discussione (se non ce ne sono, al post seguente/precedente) - come sul telefono.

 ---

 ***  VERSIONE 1.2.5 (correzioni)  ***

 Novita' e correzioni di questa versione:
  * Vista continua: aprendo un thread lungo non vengono piu' segnati come letti in blocco i messaggi successivi al selezionato. Ora si marca letto solo cio' che appare davvero sotto gli occhi: il messaggio scelto all'apertura, e poi ogni messaggio man mano che lo porti in cima scorrendo.
  * Vista continua: i gruppi del crosspost sono ora mostrati (in rosso) su ogni post, non solo nella vista a singolo articolo.
  * Con il filtro "solo non letti" attivo, se non c'e' nulla da leggere l'elenco ora dice "Nessun messaggio da leggere" invece di "Nessun messaggio".
  * Controllo aggiornamenti: NeReO puo' avvisare quando c'e' una versione piu' recente su SourceForge (automatico una volta al giorno, disattivabile in Manutenzione, piu' un pulsante manuale). Solo avviso e link: l'app non scarica ne' installa nulla da se'.

 ---

 ***  VERSIONE 1.2.4 (correzioni)  ***

 Novita' e correzioni di questa versione:
  * Riflusso testo: ora unisce anche le righe "orfane" di una-due parole rimaste in mezzo a un capoverso. Prima, anche col riflusso attivo, qualche riga corta (una "e" o una "o" da sola) restava isolata perche' l'euristica precedente la scambiava per un a-capo voluto. Ora restano su riga propria solo etichette, saluti e frasi brevi che finiscono con punteggiatura (. , : ; ! ? ...). Spento di default; solo in lettura.

 ---

 ***  VERSIONE 1.2.3 (correzioni)  ***

 Novita' e correzioni di questa versione:
  * Vista ad albero: i messaggi che aprono una discussione non risultano piu' "gia' letti" quando non lo sono. Il colore segue ora lo stato reale del post, in accordo con "vai al prossimo non letto".
  * Vista continua: rispondendo a un messaggio crosspostato ora compare l'avviso per scegliere in quali gruppi rispondere, come nella vista a singolo articolo. Prima la risposta finiva solo nel primo gruppo, senza avviso.
  * Vista continua: aprendo piu' messaggi non letti non compare piu' "Error: connection reset" al posto del testo. I corpi si scaricano una richiesta per volta, invece di aprire tante connessioni insieme.
  * Vista a pannelli: l'aggiornamento usa di default l'icona in cima alla colonna; lo swipe "tira giu'" non ruba piu' il primo scorrimento verso l'alto. Lo swipe resta riattivabile in Aspetto. Sul telefono nulla cambia.
  * Riflusso testo: ora agisce anche sui post che vanno a capo stretto (intorno alle 38-40 colonne), che prima venivano ignorati del tutto e restavano con parole orfane. Sempre spento di default; solo in lettura.
  * Vista continua: aprendo un thread su un messaggio in mezzo, i messaggi in testa che vengono solo attraversati all'apertura non vengono piu' segnati come "letti". La marcatura vale dal messaggio scelto in giu', non su cio' che scorre via all'apertura.

 ---

 ***  VERSIONE 1.2.2 (correzioni)  ***

 Novita' e correzioni di questa versione:
  * Nuova opzione "Riflusso testo" (Aspetto): ricompone i capoversi spezzati dall'a-capo classico a 72 colonne, cosi' i post si leggono ordinati su schermo stretto invece di mostrare righe orfane di una
    o due parole. Spenta di default; solo in lettura.
  * Aggiornamento molto piu' veloce: una sola connessione per server invece di una per ogni gruppo (con molti gruppi la differenza si sente). La striscia col numero di nuovi messaggi ora compare anche premendo l'icona di aggiornamento, non solo col "tira giu'".
  * Vista a pannelli (tablet): nella vista continua la terza colonna segue ora lo stesso ordine-albero della colonna centrale (non piu' per data). Scrivendo un post l'editor si apre a tutto schermo, cosi' la tastiera non copre piu' l'area di scrittura in orizzontale.
  * Vista a pannelli (tablet/orizzontale): selezionando un messaggio dall'albero ora si posiziona di nuovo correttamente in cima e ci resta (nella 1.2 poteva finire in un punto casuale).
  * Il cerchietto "aggiorna" non resta piu' appeso in cima all'albero: lo scorrimento torna immediato.
  * Connessioni SSL/TLS: aggiunto il timeout di lettura che mancava, cosi' una connessione cifrata che si blocca non lascia l'app in attesa infinita.

 ---

 ***  VERSIONE 1.2  ***

 Novita' di questa versione:
  * Compatibilita' server: alcuni provider (Astraweb tra questi) rifiutano il comando OVER usato per scaricare le intestazioni e la sincronizzazione si fermava. Ora NeReO ripiega automaticamente su XOVER, cosi' quei server tornano a funzionare.
  * Plonk esteso alle risposte: una regola su "da" puo' nascondere anche le risposte al post filtrato e l'intero ramo sottostante, chiunque le abbia scritte (reversibile: togli la regola e tornano).
  * Parita' su tablet: nella vista a pannelli ci sono ora le intestazioni complete e il ROT13, e si raggiungono anche le Bozze. La vista continua si apre gia' posizionata sul messaggio scelto.
  * Gruppi: tieni premuto un gruppo per scaricare in un colpo solo i corpi mancanti degli ultimi 300 messaggi. "Segui la discussione" ora vale per l'intero thread; i tuoi post sono riconosciuti anche con le identita' per-server.
  * Dopo l'invio di un post: conferma "Messaggio inviato" e refresh automatico del gruppo, cosi' il proprio messaggio compare senza ricaricare a mano (appena il server lo rende disponibile).
  * Backup completo (identita' per-server, tutte le impostazioni e le discussioni seguite; ripristino compatibile coi backup vecchi).
  * Diagnostica NNTP in Manutenzione: registra il dialogo col server (password sempre oscurata) per risolvere piu' in fretta i casi strani. Spenta di default.
  * Correzioni: barra icone in basso a due righe sugli schermi stretti; via un errore spurio che compariva scorrendo veloce.

 Per il resto restano le funzioni complete della 1.1. L'interfaccia e' disponibile in italiano e in inglese.

======================================================================


=============
 COS'E' NeReO
=============

NeReO e' un lettore di newsgroup Usenet per Android, dedicato alle discussioni testuali.

Niente gruppi binari, niente allegati, niente pubblicita', niente telemetria: solo messaggi e conversazioni, come Usenet e' sempre stata.

E' software libero, rilasciato con licenza GNU GPL v3 o successiva. Il nome "NeReO" e il logo sono riservati all'autore: le versioni modificate devono usare un altro nome (vedi TRADEMARK.txt). Sorgenti anche su GitHub: https://github.com/mcsm64/NeReO


===================
 LINGUA INTERFACCIA
===================

NeReO segue automaticamente la lingua del telefono:

  * telefono in italiano   -> interfaccia in italiano
  * qualunque altra lingua -> interfaccia in inglese

Se preferisci una lingua diversa da quella del telefono, puoi impostarla solo per NeReO, senza toccare il resto del sistema, in due modi:

  * dentro l'app:  Aspetto -> Lingua
  * dal sistema:   Impostazioni -> App -> NeReO -> Lingua (solo Android 13 e successivi)

Su Android 13 e successivi le due strade comandano la stessa scelta. Su Android 12 e precedenti la voce di sistema non esiste: in quel caso si cambia lingua dall'interruttore dentro l'app.

Sono previste soltanto queste due lingue.


==============
 COSA SA FARE
==============

LETTURA
  * Tre modi di vedere le discussioni: ad albero, sequenziale, oppure raggruppate per thread.
  * Vista continua: tutti i messaggi di una discussione uno sotto l'altro, con salto diretto al messaggio scelto nell'albero.
  * Citazioni a colori per livello, con possibilita' di accorciarle.
  * Face (l'avatar storico di Usenet) mostrata accanto all'autore.
  * ROT13, intestazioni complete, albero della conversazione.
  * Navigazione con gesti: scorri per passare al messaggio o alla discussione successiva.
  * Indicazione delle risposte ai tuoi messaggi, con salto diretto alla prima non letta.

SCRITTURA
  * Risposte con citazione in quattro modalita', compresa la citazione ridotta e la rifinitura riga per riga.
  * Crosspost: risposte e nuovi thread su piu' gruppi.
  * Firma con varianti a rotazione e segnaposto automatici.
  * Identita' diversa per ogni server, se serve.
  * Bozze salvate localmente.

ORGANIZZAZIONE
  * Filtri con azioni chiare (Ignora, Elimina, Evidenzia, Segui il thread) oppure a punteggio per chi vuole il controllo fine. Applicabili anche ai messaggi gia' scaricati.
  * Plonk autore con un tocco.
  * Ricerca per messaggio o per discussione.
  * Conservazione automatica: per tempo o per numero di discussioni, impostabile gruppo per gruppo.
  * Backup e ripristino della configurazione, con cifratura opzionale.

ASPETTO
  * Tema chiaro, scuro o automatico; colori dinamici Material You.
  * Dieci tinte per l'interfaccia, sei dimensioni di testo.
  * Layout a pannelli per tablet e schermi larghi, con proporzioni regolabili.

CONNESSIONE
  * NNTP con autenticazione, SSL/TLS su porta dedicata.
  * Fallback automatico IPv6/IPv4.


======================
 PRIMA CONFIGURAZIONE
======================

1. Apri il menu (tre puntini in alto a destra) e scegli "Server", poi il "+" in basso. Puoi partire da un server noto o inserirne uno a mano. Esempio gratuito: news.eternal-september.org, porta 563, SSL (richiede registrazione gratuita sul loro sito).

2. Sempre dal menu, apri "Identita' e firma" e inserisci nome ed e-mail con cui vuoi comparire nei gruppi.

3. Torna all'elenco e tocca il "+" in basso per sfogliare i gruppi disponibili sul server e sottoscrivere quelli che ti interessano.

4. Tocca un gruppo per scaricarne i messaggi.


======================
 NOTE DI INSTALLAZIONE
======================

* Se stai aggiornando da una versione precedente, installa l'APK sopra quella esistente: NON disinstallare prima, o perderai gruppi, letture e impostazioni.
* Se il telefono blocca l'installazione, autorizza le origini sconosciute per l'app da cui apri il file.
* Requisiti: Android 8.0 o successivo.


==============
 SEGNALAZIONI
==============

Per una segnalazione utile bastano tre cose:
  * cosa stavi facendo quando e' successo;
  * uno screenshot, se il problema si vede;
  * il blocco da Informazioni -> Dati tecnici (c'e' un tasto Copia).


================
 STORICO VERSIONI
================

 Le versioni descritte qui sopra per esteso non si ripetono: questa e' la
 coda, cioe' tutto quello che viene prima della 1.2.

1.1        Cambio lingua dentro l'app (Aspetto -> Lingua). Rete di sicurezza del database: copia automatica e avviso prima di un eventuale azzeramento per schema incompatibile.
1.0        Prima versione stabile. Interfaccia in italiano e inglese.
1.0-RC01   Release candidate: traduzione completa dell'interfaccia.
beta20     Risolti tre crash (gruppi molto grandi, schermata vuota col tasto indietro, finestre di conferma). Filtri applicabili ai messaggi gia' scaricati; elimina discussione; salto alle risposte ai propri post; chiudi applicazione; correzioni a vista continua, tastiera e citazione ridotta.
beta19     Citazione ridotta corretta; maiuscola automatica; elenco gruppi scaricato per intero; conservazione per numero di discussioni.
beta18     Risposte ai propri post evidenziate; vista continua del thread; filtri con azioni al posto dei punteggi; crosspost su nuovo thread.
beta17     Risposta ai crosspost scegliendo i gruppi; fallback IPv6/IPv4.
beta16     Correzioni al layout orizzontale e all'interfaccia.
beta14-15  Intestazioni complete; Face affidabili; rifinisci citazione.
beta11-13  Ricerca; riordino gruppi; crosspost; splashscreen.
beta7-10   Da MyNewsReader a NeReO; bozze; identita' multiple.
beta1      Primo rilascio.


===============
 RINGRAZIAMENTI
===============

NeReO e' arrivato fin qui grazie a chi lo ha provato pazientemente versione dopo versione, segnalando difetti, proponendo funzioni e insistendo quando qualcosa non convinceva.

Un grazie a Roxy, Bingo3331, Big Umberto, rpoggi, Paul, Dave Royal, Alessandro, John (n2n), Macchia, il Merovingio, Adam H. Kerman, ObiWan, Ammammata, Henry The Mole, SiMcarD, cercaparole e Gabx: molte delle cose che oggi funzionano bene esistono perche' qualcuno si e' preso la briga di spiegare con precisione cosa non andava.

UN RINGRAZIAMENTO PARTICOLARE ad ALLEN, per un supporto sempre utile e TRASVERSALE, dietro le quinte. Non compare qui sopra accanto a una funzione, e la ragione e' proprio quella: il suo contributo non e' stato una segnalazione su un punto, e' stato esserci dove serviva, release dopo release. E' il genere di aiuto che non lascia traccia in un changelog e senza il quale un changelog come questo non esisterebbe.

Grazie a IVO GANDOLFO, amministratore di paganini.bofh.team, che ha rinnovato e ridistribuito il certificato del server chiudendo un caso che avevamo aperto noi: un intervento su un server che non e' nostro, a beneficio di tutti i client che si collegano li'.

Il progetto nasce dal MOD 17 di Newsreader di Piao Hong, di cui riprende l'impostazione dell'interfaccia.

Usenet Still Lives!
===================
MCSM (aka Marco)
