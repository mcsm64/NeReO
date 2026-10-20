/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Note di versione mostrate DENTRO l'app (schermata "Novità versione").
 *
 * Divisione dei compiti col README: il README_IT.txt (e README_EN.txt) è lo storico completo di
 * tutte le versioni, sta fuori dall'app e lo legge chi scarica il sorgente.
 * Questo invece è solo l'ULTIMA versione, sempre a portata di mano nell'app.
 *
 * Si aggiorna a mano a ogni release, come il README: è testo curato, non
 * generato. Tenerlo qui (una costante) evita di dover impacchettare un file
 * di risorse e leggerlo a runtime.
 */
package org.openusenet.reader

object ReleaseNotes {
    /**
     * Versione a cui si riferiscono le note sotto.
     *
     * ATTENZIONE (bug beta11, segnalato da un tester): questa costante è
     * SEPARATA da versionName/versionCode in app/build.gradle.kts, di
     * proposito (testo curato a mano, non generato) — ma questo significa
     * che va aggiornata A MANO ad ogni release, altrimenti la schermata
     * "Novità versione" mostra la versione sbagliata pur essendo l'APK
     * corretto. Ad ogni bump di versione in build.gradle.kts, aggiornare
     * SEMPRE anche questa riga e le [sections] sotto.
     */
    const val VERSION = "1.5.6"

    /**
     * Novità della versione corrente, in blocchi titolo → righe.
     *
     * RC01→1.0: i testi sono passati alle RISORSE (values/, values-en/,
     * values-it/) per essere tradotti come il resto dell'interfaccia. Qui
     * restano solo gli ID: essendo `object` non-composable, questa classe non
     * può risolvere le risorse da sé — lo fa ReleaseNotesScreen, che è
     * composable. Ad ogni release si aggiornano gli ID qui e i testi in
     * strings.xml (tutte e tre le cartelle).
     */
    val sections: List<Pair<Int, List<Int>>> = listOf(
        // 1.5.6 (19 ottobre 2026, versionCode 174). Chiavi rn_v156_* verificate
        // libere nelle tre cartelle prima di usarle.
        //  - l1  niente piu' chiusure senza selettore di file (Roxy, box TV)
        R.string.rn_v156_title to listOf(
            R.string.rn_v156_l1,
        ),
        // 1.5.5 (17 ottobre 2026, versionCode 169). Chiavi rn_v155_* verificate
        // libere nelle tre cartelle prima di usarle.
        //  - l1  la ricerca mostra l'estratto del testo
        //  - l2  il rifiuto 441 del server spiegato
        //  - l3  il campo del messaggio nella vista a pannelli
        //  - l4  (versionCode 170) intestazione dell'autore, iniziali colorate
        //  - l5  (versionCode 170) icona a tema di Android 13
        //  - l6  (versionCode 171) References oltre i 998 caratteri
        //  - l7  (versionCode 172) le X-Face si vedono
        //  - l8  (versionCode 173) i messaggi nuovi non sfuggono piu' (bis)
        R.string.rn_v155_title to listOf(
            R.string.rn_v155_l4,
            R.string.rn_v155_l7,
            R.string.rn_v155_l1,
            R.string.rn_v155_l2,
            R.string.rn_v155_l3,
            R.string.rn_v155_l8,
            R.string.rn_v155_l6,
            R.string.rn_v155_l5,
        ),
        // 1.5.4 (15 ottobre 2026, versionCode 168). Chiavi rn_v154_* verificate
        // libere nelle tre cartelle prima di usarle.
        //  - l1  «Cita e rispondi»
        //  - l2  aprendo un gruppo, i messaggi nuovi si vedono
        //  - l3  «una volta al giorno» controlla alla prima apertura del giorno
        R.string.rn_v154_title to listOf(
            R.string.rn_v154_l1,
            R.string.rn_v154_l2,
            R.string.rn_v154_l3,
        ),
        // 1.5.3 (14 ottobre 2026, versionCode 165). Chiavi rn_v153_* verificate
        // libere nelle tre cartelle prima di usarle.
        //  - l1  tolta una regola, il suo effetto se ne va (Ignora, Segui, Evidenzia)
        //  - l2  le righe «Filtri:» nel log
        //  (versionCode 166 e 167: correzioni interne sulle copie dello stesso
        //  messaggio su piu' server, nessuna voce nuova: l1 le copre gia')
        R.string.rn_v153_title to listOf(
            R.string.rn_v153_l1,
            R.string.rn_v153_l2,
        ),
        // 1.5.2 (13 ottobre 2026, versionCode 164). Chiavi rn_v152_* verificate
        // libere nelle tre cartelle prima di usarle.
        //  - l1  l'APK pesa meno (solo le 75 icone usate, non 11.400)
        R.string.rn_v152_title to listOf(
            R.string.rn_v152_l1,
        ),
        // 1.5.1 (13 ottobre 2026, versionCode 162). Chiavi rn_v151_* verificate
        // libere nelle tre cartelle prima di usarle.
        //  - l1  la bozza si salva da sola (T1: una risposta aperta per sbaglio
        //        non diventa bozza; T2: niente bozze risorte dopo l'invio)
        //  - l2  «Scarta» (T3)
        //  - l3  la schermata a righe annunciata nella 1.5 NON arriva per ora:
        //        si dice qui invece di riscrivere le note gia' pubblicate
        //  - l4  (versionCode 163) §A12-retro meta' B: la propagazione segue
        //        l'azione, e la stella su «Segui il thread»
        R.string.rn_v151_title to listOf(
            R.string.rn_v151_l1,
            R.string.rn_v151_l2,
            R.string.rn_v151_l3,
            R.string.rn_v151_l4,
        ),
        // 1.5 (12 ottobre 2026, versionCode 157). Chiavi rn_v15_* verificate
        // libere nelle tre cartelle prima di usarle (par. 2 della checklist:
        // rn_v13_*, rn_v131_* e rn_v132_* sono bruciate).
        //  - l1  passo 2 del pacchetto citazioni: lavoro interno, dichiarato
        //  - l2  (versionCode 158) «Cita» nel menu di selezione, lettura a
        //        schermo intero. Chiave rn_v15_l2 verificata libera.
        //  - l3  (versionCode 159, collaudo di Marco) «Cita» dentro una
        //        citazione ri-radica sull'attribuzione interna.
        //
        // ⚠️ IL BLOCCO NASCE INSIEME A `VERSION`, E NON DOPO. Alla build 134
        // della 1.4.7 `VERSION` era stata alzata senza aggiungere il blocco, e
        // l'app avrebbe mostrato il titolo nuovo coi contenuti della 1.2.
        // Questa versione apre un ciclo lungo: la sezione crescera' a ogni
        // passo, ma non parte vuota.
        R.string.rn_v15_title to listOf(
            R.string.rn_v15_l1,
            R.string.rn_v15_l2,
            R.string.rn_v15_l3,
        ),
        // 1.4.11 (12 ottobre 2026, versionCode 154). Chiavi rn_v1411_*
        // verificate libere nelle tre cartelle prima di usarle.
        //  - l1  (par. B3, ObiWan) l'allarme anti-declassamento STARTTLS
        //
        // ⚠️ La nota dice da sola il proprio limite: protegge solo DOPO un
        // collegamento pulito. Lo dice perche' la RFC 4642 lo dice
        // («partially»), e una difesa presentata per piu' di quello che e'
        // fa danno al posto di farne.
        R.string.rn_v1411_title to listOf(
            R.string.rn_v1411_l1,
        ),
        // 1.4.10 (10 ottobre 2026, versionCode 150). Chiavi rn_v1410_*
        // verificate libere nelle tre cartelle prima di usarle.
        //  - l1  i nomi di gruppo lunghi si abbreviano invece di troncarsi
        //  - l2  manutenzione interna, dichiarata per onesta' e non per vanto
        //  - l3  (par. K3, Macchia) il menu contestuale scorre davvero
        //  - l4  (par. K4) il `+` apre la discussione anche se le risposte
        //        sono gia' state lette
        //
        // ⚠️ Nessuna delle tre voci di questa versione e' stata chiesta da un
        // utente: sono debiti nostri. La nota lo dice, perche' chi legge
        // «novita'» e non trova niente di nuovo ha diritto di sapere perche'
        // il numero e' cambiato.
        R.string.rn_v1410_title to listOf(
            R.string.rn_v1410_l1,
            R.string.rn_v1410_l2,
            R.string.rn_v1410_l3,
            R.string.rn_v1410_l4,
        ),
        // 1.4.9 (8 ottobre 2026, versionCode 148). Chiavi rn_v149_* verificate
        // libere nelle tre cartelle prima di usarle (par. 2 della checklist:
        // rn_v13_*, rn_v131_* e rn_v132_* sono bruciate).
        //  - l1  (par. J1) interruttore per le rotaie verticali del rientro
        //  - l2  (par. J3-C1) «Conserva questo messaggio»
        //  - l3  (par. K1) il campo del messaggio non si schiaccia piu'
        //
        // Entrambe nascono da Jeff, e sono una che TOGLIE e una che AGGIUNGE:
        // la prima rende spegnibile un ornamento che aveva preso per un altro,
        // la seconda risponde alla sua domanda «NeReO conserva i post?».
        //
        // ⚠️ La seconda voce dichiara il proprio limite nel corpo del testo: un
        // messaggio conservato la ricerca non lo vede piu'. Chi conserva lo fa
        // per ritrovare, e una nota che tacesse quel rovescio venderebbe una
        // funzione diversa da quella che c'e'.
        R.string.rn_v149_title to listOf(
            R.string.rn_v149_l1,
            R.string.rn_v149_l2,
            R.string.rn_v149_l3,
        ),
        // 1.4.8 (5-6 ottobre 2026, versionCode 142 e 143). Chiavi rn_v148_*
        // verificate libere nelle tre cartelle prima di usarle (par. 2 della
        // checklist: rn_v13_*, rn_v131_* e rn_v132_* sono bruciate).
        //  - l1  (§A12-retro, prima meta') «Applica ora» vale anche per le
        //        regole che AGGIUNGONO: «Evidenzia» e i punteggi a mano
        //  - l2  il limite dichiarato: la stella e la propagazione no, non
        //        ancora
        //  - l3  (par. U1) il bersaglio funziona anche sulle risposte, non
        //        solo sul primo post della discussione
        //  - l4  (par. U2) `_` e `%` nella ricerca cercano se stessi
        //  - l5  (par. U3) la freccia indietro non ripercorre piu' tutto
        //  - l6  (par. B6b) Format=Flowed in uscita, SPENTO di default
        //
        // l1-l2 da Cercaparole su Usenet, l3-l5 da BIG Umberto sulla 1.4.7.
        // La seconda voce dice cosa NON fa: la correzione copre meta' del
        // difetto, e una nota di versione che tacesse l'altra meta' farebbe
        // credere all'utente di avere una funzione che non ha. *Un limite
        // dichiarato non e' una debolezza della nota: e' la nota che fa il suo
        // mestiere.*
        //
        // Le tre voci di Umberto hanno una parentela che vale notare: tutte e
        // tre stanno sulla strada «cerca -> apri -> guarda dove sta», cioe'
        // par. B13 della 1.4.6, che aveva chiesto lui. Sta collaudando la
        // propria funzione e ne ha trovato i tre buchi.
        R.string.rn_v148_title to listOf(
            R.string.rn_v148_l1,
            R.string.rn_v148_l2,
            R.string.rn_v148_l3,
            R.string.rn_v148_l4,
            R.string.rn_v148_l5,
            R.string.rn_v148_l6,
        ),
        // 1.4.7 (5 ottobre 2026, versionCode 134-138). Chiavi rn_v147_*
        // verificate libere nelle tre cartelle prima di usarle (par. 2 della
        // checklist: rn_v13_*, rn_v131_* e rn_v132_* sono bruciate).
        //  - l1  (§A15) le rotaie dell'albero in tutte e tre le viste
        //  - l2  (§B14) «albero della discussione», non «della conversazione»
        //  - l3  (§A16) l'interruttore «solo non letti» in Aspetto
        //  - l4  (§A17) il riscarico anche dall'elenco dei gruppi
        //  - l5  l'avviso sulla finestra di sync nel dialogo del riscarico
        //  - l6  (§A18) l'elenco dei gruppi ricorda dove eri
        //
        // ⚠️ QUESTA SEZIONE ERA LA MIA MANCANZA. Alla build 134 avevo alzato
        // `VERSION` a "1.4.7" senza aggiungere il blocco qui sotto: l'app
        // avrebbe mostrato il titolo nuovo con i contenuti della 1.2. Il par. 2
        // della checklist lo prescrive da sempre, e l'ho saltato bumpando solo
        // il numero. *Un numero di versione aggiornato senza le sue note non e'
        // mezzo lavoro: e' un'etichetta che mente.*
        //
        // Cinque voci su sei vengono da due collaudatori — Dave Royal (l1, l2)
        // e Jeff (l3, l4, l6) — e tutte e cinque sono GIUNZIONI: qualcosa che
        // esisteva per una schermata e non per la sua sorella. La sesta nasce
        // da una misura di Marco sul campo: 415 messaggi diventati 298.
        R.string.rn_v147_title to listOf(
            R.string.rn_v147_l1,
            R.string.rn_v147_l2,
            R.string.rn_v147_l3,
            R.string.rn_v147_l4,
            R.string.rn_v147_l5,
            R.string.rn_v147_l6,
        ),
        // 1.4.6 (30 settembre 2026, versionCode 123). Chiavi rn_v146_*
        // verificate libere: erano nate il 28 e sono state rinominate rn_v145_*
        // il 29 senza mai uscire, quindi non c'e' niente di bruciato.
        //  - l1  l'etichetta della scrittura diceva il gruppo di LETTURA
        //  - l2  nei pannelli la scelta del crosspost veniva scartata
        //  - l3  (§A14) nei pannelli stretti l'oggetto si riduceva a una
        //        colonna di lettere: la barra si prendeva tutta la riga
        //
        // Tutte e due dalle prove 9A/9B di Dave Royal del 29 settembre. Lui le
        // dava per non meritevoli di correzione: sul terzo difetto aveva
        // ragione (e' strutturale, sta in A1), su questi due la sua stima era
        // sul costo immaginato - una riga e un termine estratto.
        R.string.rn_v146_title to listOf(
            R.string.rn_v146_l1,
            R.string.rn_v146_l2,
            R.string.rn_v146_l3,
            R.string.rn_v146_l4,
            R.string.rn_v146_l5,
            R.string.rn_v146_l6,
            R.string.rn_v146_l7,
        ),
        // 1.4.5 (28-29 settembre 2026, versionCode 121 e 122). Chiavi rn_v145_*
        // verificate libere il 28 settembre (le occupate arrivano a rn_v144_*).
        //  - l1  «segna tutto letto» torna in barra nei pannelli
        //  - l2  il crosspost: l'impostazione «tutti i gruppi» vale anche sul
        //        telefono
        //  - l3  salva l'articolo come file .eml
        //  - l4  %oggetto% nell'introduzione alla citazione
        //
        // IL versionName CAMBIA, E NON E' UNA SCELTA: la 1.4.4 versionCode 120
        // E' PUBBLICA su SourceForge dal 28 settembre. Due APK diversi non
        // possono chiamarsi uguale (par. 1 della checklist).
        R.string.rn_v145_title to listOf(
            R.string.rn_v145_l1,
            R.string.rn_v145_l2,
            R.string.rn_v145_l3,
            R.string.rn_v145_l4,
            // 29 settembre, versionCode 122: la 121 non e' mai stata
            // pubblicata, quindi il nome resta 1.4.5 e le due voci nuove
            // entrano in QUESTA sezione invece di aprirne una seconda con lo
            // stesso titolo. Le chiavi erano nate rn_v146_* e sono state
            // rinominate: non essendo mai uscite, non si brucia niente.
            R.string.rn_v145_l5,
            R.string.rn_v145_l6,
        ),
        // 1.4.4 (24 settembre 2026, versionCode 117). Chiavi rn_v144_*
        // verificate libere.
        //  - l1  l'Incolla nel pannello di scrittura della vista a pannelli
        //  - l2  "Ritira dal server" nel menu contestuale dei pannelli
        //  - l3  (118) con un server solo, "aggiorna" non chiede piu' niente
        //  - l4  (119) il menu dei tre puntini si apre sotto l'icona
        //  - l5  (120) i gruppi omonimi su server diversi si distinguono
        //
        // NOTA DI MANUTENZIONE: le note di versione elencano cio' che l'utente
        // deve sapere, non tutto cio' che e' cambiato. Un campo dei filtri in
        // piu' non ha una voce qui di proposito — vedi la nota nel blocco 118
        // di build.gradle.kts — e con esso la correzione che gli ridava
        // l'estensione alle risposte.
        R.string.rn_v144_title to listOf(
            R.string.rn_v144_l1,
            R.string.rn_v144_l2,
            R.string.rn_v144_l3,
            R.string.rn_v144_l4,
            R.string.rn_v144_l5,
        ),
        // 1.4.3 (23 settembre 2026, versionCode 114). Chiavi rn_v143_*
        // verificate libere.
        //
        // PERCHE' ESISTE QUESTA SEZIONE, ed e' la seconda volta in due giorni:
        // la 1.4.2 versionCode 113 E' GIA' IN MANO AGLI UTENTI. Il versionName
        // percio' DEVE cambiare (par. 1 della checklist: due APK diversi non
        // possono chiamarsi uguale), e le tre correzioni della 114 NON possono
        // stare sotto la 1.4.2, che le attribuirebbe a una release che non le
        // ha. E' lo stesso errore fatto il 22 settembre con la 1.4.1/107, e
        // stavolta e' stato Marco ad accorgersene.
        //  - l1  (114) la lista sorda: il catch terminale diventa retryWhen
        //  - l2  (114) l'Annulla della diagnostica LISTGROUP annulla davvero
        //  - l3  (114) le righe orfane si possono finalmente eliminare
        R.string.rn_v143_title to listOf(
            R.string.rn_v143_l1,
            R.string.rn_v143_l2,
            R.string.rn_v143_l3,
        ),
        // 1.4.2 (22 settembre 2026). Chiavi rn_v142_* verificate libere.
        //
        // ATTENZIONE, e' la ragione per cui questa sezione ha sette voci e
        // non una: la 1.4.1 pubblicata su SourceForge e' il versionCode 107.
        // Le build 108, 109 e 110 sono state scritte DOPO, mentre la 107 era
        // gia' in mano agli utenti, e quindi NON sono mai uscite come 1.4.1:
        // escono qui. Tenerle elencate sotto la 1.4.1 avrebbe attribuito a
        // quella release cose che nessuno ha mai avuto.
        //  - l1  (111) "ricarica" non cancella piu' l'articolo
        //  - l2  (108) revoca della 105/B: i propri post tornano da leggere
        //  - l3  (108) "segna tutto letto" non si preme piu' per sbaglio
        //  - l4  (109) crosspost su thread nuovo anche nei pannelli
        //  - l5  (110) "Sostituisci" anche nel menu contestuale dei pannelli
        //  - l6  (113) cancellare un server porta via i suoi dati, e lo chiede
        //  - l7  (113) eliminaDiscussione aggiorna il contatore dei non letti
        //
        // NON TOCCARE PIU' QUESTE SETTE VOCI: descrivono cio' che il
        // versionCode 113 ha davvero spedito. Il 23 settembre la l6 era stata
        // riscritta per annunciare la pulizia degli orfani, che nella 113 NON
        // c'e': rimessa com'era, e quella voce e' passata alla 1.4.3.
        R.string.rn_v142_title to listOf(
            R.string.rn_v142_l1,
            R.string.rn_v142_l2,
            R.string.rn_v142_l3,
            R.string.rn_v142_l4,
            R.string.rn_v142_l5,
            R.string.rn_v142_l6, // 113: cancellare un server porta via i suoi dati, e lo chiede prima
            R.string.rn_v142_l7, // 113: eliminaDiscussione aggiorna il contatore dei non letti
        ),
        // 1.4.1 (21 settembre 2026, versionCode 107): release di sole
        // CORREZIONI dopo la 1.4 pubblica. Piano in NeReO_1.4.1_da_fare.md.
        // Chiavi rn_v141_* verificate libere (le occupate arrivavano a
        // rn_v14_*).
        //
        // NON TOCCARE PIU' QUESTE NOVE VOCI: descrivono cio' che il
        // versionCode 107 ha davvero spedito agli utenti. Il 22 settembre
        // erano state modificate (l2 e l9) e allungate a undici, per
        // comportamenti che stavano nelle build 108-110 mai pubblicate:
        // rimesse com'erano, e quelle voci sono passate alla 1.4.2.
        R.string.rn_v141_title to listOf(
            R.string.rn_v141_l1, // "letto per Message-ID": funziona leggendo, e propaga anche il "non letto"
            R.string.rn_v141_l2, // 105/A + 105/B: criterio "e' mio", e il post proprio che nasceva letto
            R.string.rn_v141_l3, // rispondere non fa piu' perdere il messaggio su cui eri
            R.string.rn_v141_l4, // albero e filtrati, OP nella discussione aperta, conteggio dei filtrati
            R.string.rn_v141_l5, // incolla sempre visibile, spento ad appunti vuoti
            R.string.rn_v141_l6, // voce arretrata della 1.4: password vuota
            R.string.rn_v141_l7, // due righe di diagnostica per la lista che non si aggiorna
            R.string.rn_v141_l8, // 105/G: le intestazioni riservate scartate si vedono PRIMA di inviare
            R.string.rn_v141_l9, // Dave 08: "segna tutto letto" scambiato con "aggiorna" (solo telefono)
        ),
        // 1.4 (17 settembre 2026): pubblicata il 17. Il commento diceva
        // "release APERTA" ed e' rimasto indietro: corretto il 20 settembre.
        // Contratto in NeReO_1.4_specifica.md.
        // NB sulle chiavi: rn_v14_* verificate libere (rn_v130_* era la 1.3
        // stabile, rn_v13_* erano gia' bruciate dalla 1.2.8).
        R.string.rn_v14_title to listOf(
            R.string.rn_v14_l1, // STARTTLS: dopo un 382 fallito si chiude (RFC 4642)
            R.string.rn_v14_l2, // database aggiornato alla versione 12
            R.string.rn_v14_l3, // il ripristino inventava lo username "null" (log di Marco)
            R.string.rn_v14_l4, // Diagnostica database in Manutenzione
            R.string.rn_v14_l5, // risposta crosspostata: si vede dove va (Dave Royal)
            R.string.rn_v14_l6, // overview a fette: "tutti i messaggi" funziona (Bingo3331)
            R.string.rn_v14_l7, // fiducia TLS: TOFU su SPKI, eccezione scadenza, schermata identita'
            R.string.rn_v14_l8, // avviso "il certificato scade fra N giorni"
            R.string.rn_v14_l9, // charset del corpo + "Ricarica il messaggio" (Kerman)
            R.string.rn_v14_l10, // "letto" segue il Message-ID (come MesNews)
            R.string.rn_v14_l11, // Followup-To letto e rispettato (GNKSA)
            R.string.rn_v14_l12, // introduzione alla citazione per gruppo (Henry The Mole)
        ),
        // 1.3.7 (15 settembre 2026): il ripristino del backup duplicava filtri
        // e identita' (Dave Royal), e non diceva mai cosa avrebbe fatto di
        // cio' che c'era gia'. Chiavi rn_v137_* verificate libere.
        R.string.rn_v137_title to listOf(
            R.string.rn_v137_l1, // niente piu' duplicati: ripristino idempotente
            R.string.rn_v137_l2, // ora chiede: unisci o sostituisci
            R.string.rn_v137_l3, // "password errata" non copre piu' gli errori di import
            R.string.rn_v137_l4, // a capo in invio: niente spazio in coda (Kerman)
            R.string.rn_v137_l5, // "file illeggibile" ora dice PERCHE' (Dave Royal)
        ),
        // 1.3.6 (14 settembre 2026): due difetti di conformita' RFC trovati
        // dalle domande di un lettore esperto su un gruppo anglofono, piu' la
        // funzione di esportazione che lui stesso ha chiesto — e che si e'
        // scoperto non essere mai esistita fuori dalla barra di selezione.
        // Chiavi rn_v136_* verificate libere.
        R.string.rn_v136_title to listOf(
            R.string.rn_v136_l1, // oggetti lunghi non ASCII: RFC 2047
            R.string.rn_v136_l2, // us-ascii dichiarato quando e' us-ascii
            R.string.rn_v136_l3, // esporta/condividi articolo
            R.string.rn_v136_l4, // link permanente e Message-ID negli appunti
            R.string.rn_v136_l5, // preset OVH: 119 + STARTTLS, correzione della 1.3.5
        ),
        // 1.3.5 (10 settembre 2026): i messaggi cablati nel livello di rete
        // passano dall'italiano all'inglese. Piano C: NON e' la
        // localizzazione, e le note lo dicono senza girarci intorno — un
        // utente italiano da qui in avanti legge queste frasi in inglese, ed
        // e' giusto che lo sappia dalla schermata delle novita' e non
        // scoprendolo davanti a un errore.
        // Chiavi rn_v135_* verificate libere.
        R.string.rn_v135_title to listOf(
            R.string.rn_v135_l1, // cosa cambia e da quale riscontro nasce
            R.string.rn_v135_l2, // cosa significa per chi usa l'app in italiano
            R.string.rn_v135_l3, // trim del nome utente, e perche' NON della password
            R.string.rn_v135_l4, // occhio + conteggio caratteri + avviso spazi
            R.string.rn_v135_l5, // la FORMA delle credenziali nel log diagnostico
            R.string.rn_v135_l6, // i preset dei tre server con account nascono su 563
            R.string.rn_v135_l7, // due segnalazioni, un difetto: appartenenza al thread
            R.string.rn_v135_l8, // nota al segnalatore: orfani si', oggetto no
            R.string.rn_v135_l9, // firma: le righe vuote non contano piu'
        ),
        // 1.3.4 (10 settembre 2026): RELEASE TECNICA, da sola. Zero righe di
        // codice dell'app cambiate rispetto alla 1.3.3: cambia la toolchain
        // (AGP 9.3.0, Gradle 9.5.0, Kotlin 2.2.10, KSP2) e Compose dalla
        // 1.7.6 alla 1.12. Regola della 1.2.14 applicata a una migrazione:
        // se qualcosa si rompe, un solo sospettato possibile.
        // Le note dicono all'utente anche COSA speriamo (selezione col mouse)
        // e che e' una speranza, non una promessa — e gli chiedono
        // esplicitamente di segnalare qualunque differenza, perche' e'
        // l'unico modo di collaudare venti mesi di framework.
        // Chiavi rn_v134_* verificate libere.
        R.string.rn_v134_title to listOf(
            R.string.rn_v134_l1, // cosa cambia: toolchain + Compose 1.7.6 -> 1.12
            R.string.rn_v134_l2, // perche' esce da sola
            R.string.rn_v134_l3, // cosa speriamo: la barra col mouse (previsione)
            R.string.rn_v134_l4, // invito a segnalare qualunque differenza
            R.string.rn_v134_l5, // cosa si e' perso: Cerca/Condividi sul testo selezionato
        ),
        // 1.3.3 (10 settembre 2026): solo VERITA'. Nessun cambio di
        // comportamento TLS — cio' che passava passa ancora, cio' che veniva
        // rifiutato viene rifiutato ancora. Cambia cosa DICIAMO quando
        // fallisce: fino alla 1.3.2 suggerivamo lo stesso interruttore per
        // qualunque errore di certificato, anche dove non poteva funzionare.
        // Bingo3331 ha fatto quello che il messaggio gli diceva, su Paganini,
        // e ne ha dedotto che l'opzione fosse rotta: non lo e', fa un'altra
        // cosa (nome host, non catena di firma). Il vicolo cieco era nostro.
        // Chiavi rn_v133_* verificate libere.
        R.string.rn_v133_title to listOf(
            R.string.rn_v133_l1, // errore TLS: catena vs nome host, rimedi diversi
            R.string.rn_v133_l2, // opzione rinominata con quello che fa davvero
            R.string.rn_v133_l3, // "Copia il messaggio" in barra + "Incolla" in scrittura (Roxy)
            R.string.rn_v133_l4, // limite: copia il messaggio intero, non la selezione
        ),
        // 1.3.2 (Marco, 9 settembre 2026): l'equivalente per TELEFONO di
        // "espandi e vai", nato dall'uso della 1.3.1 sul tablet. Opzione
        // separata e non riuso di tapOpToExpand: quella e' legata alla riga
        // richiudibile della vista raggruppata (total > 1), che nella vista ad
        // albero non esiste. Vedi AppSettings.apriPrimoNonLetto.
        // NB sulle chiavi: rn_v132_* erano GIA' occupate dalla "1.3.2" mai
        // pubblicata, confluita nella 1.2.8. Questa usa rn_v132b_*.
        R.string.rn_v132b_title to listOf(
            R.string.rn_v132b_l1, // telefono: tocco sulla testa -> primo non letto
            R.string.rn_v132b_l2, // User-Agent nella forma RFC 5536 (Dave Royal)
            R.string.rn_v132b_l3, // 423 su overview = intervallo vuoto, non errore (BIG Umberto)
        ),
        // 1.3.1 (Marco, 8 settembre 2026): primo riscontro reale sulla 1.3, da
        // Roxy (Box TV, telecomando, NESSUNO schermo tattile). Con l'opzione
        // "Tocca l'OP per espandere" accesa, nei pannelli il tocco espandeva
        // SOLTANTO: il ramo che seleziona non veniva mai eseguito. La via
        // d'uscita che avevamo dichiarato nella 1.2.9 (menu a pressione
        // prolungata) su quel dispositivo NON ESISTE — quindi l'OP di una
        // discussione con risposte era irraggiungibile.
        // NB sulle chiavi: rn_v131_* erano GIA' occupate dalla "1.3.1" mai
        // pubblicata di inizio settembre, confluita nella 1.2.8. Questa usa
        // rn_v131b_* — stessa convenzione dei suffissi 1.2.9.b / 1.2.9.c.
        R.string.rn_v131b_title to listOf(
            R.string.rn_v131b_l1, // espandi E VAI al primo non letto (pannelli)
            R.string.rn_v131b_l2, // menu ⋮ sulle righe: un clic invece di tenere premuto
            R.string.rn_v131b_l3, // barra del riquadro di lettura personalizzabile anche nei pannelli
        ),
        // 1.3 (Marco, 7 settembre 2026): la STABILE. Contiene una sola
        // correzione rispetto alla 1.2.17 - la lacuna sull'autenticazione pigra
        // in postArticle e refreshAvailableGroups, trovata leggendo il log del
        // collaudo - e per il resto e' la 1.2.17 provata sul campo.
        // NB: le chiavi rn_v13_* erano gia' occupate — sono quelle della 1.2.8,
        // riusate all'epoca da una 1.3 mai pubblicata (vedi il commento piu'
        // sotto). Questa 1.3, quella vera, usa rn_v130_*.
        R.string.rn_v130_title to listOf(
            R.string.rn_v130_l1, // lacuna dell'autenticazione pigra, chiusa unificando i 4 siti
            R.string.rn_v130_l2, // cosa raccoglie questa stabile
        ),
        // 1.2.17 (Marco, 7 settembre 2026): nasce dal log del collaudo della
        // 1.2.16, dove si e' visto che su news.usenet.ovh la password parte in
        // chiaro ad ogni collegamento (il server annuncia STARTTLS e risponde
        // 580 Error initializing TLS; Marco ha poi verificato che il servizio
        // non offre TLS su nessuna porta). Non potendo cifrare, si riduce
        // l'esposizione e la si rende visibile.
        R.string.rn_v1217_title to listOf(
            R.string.rn_v1217_l1, // autenticazione pigra (opzione, spenta di default)
            R.string.rn_v1217_l2, // segno "non cifrato" nell'elenco server
            R.string.rn_v1217_l3, // il log non conta piu' come errori le risposte attese
        ),
        // 1.2.16 (Marco, 7 settembre 2026): Piano B nato dalla quarta prova sul
        // campo (ritiro iniettato da eternal-september: rimosso li', intatto su
        // Astraweb e OVH). La verifica della 1.2.15 diceva il vero ma su un
        // server solo; la domanda vera dell'utente e' "e sugli altri?".
        R.string.rn_v1216_title to listOf(
            R.string.rn_v1216_l1, // esito del ritiro server per server
            R.string.rn_v1216_l2, // il dialogo di conferma dice cosa un ritiro puo' fare
        ),
        // 1.2.15 (Marco, 7 settembre 2026): giro di rifiniture nato dal
        // collaudo della 1.2.14 e dalla lettura del log di un cancel. Quattro
        // voci per l'utente; le due piu' tecniche (SNI su indirizzo IP,
        // connessione in cache che non rileggeva gli interruttori di
        // sicurezza) stanno insieme nell'ultima riga, perche' da sole non
        // direbbero nulla a chi legge.
        R.string.rn_v1215_title to listOf(
            R.string.rn_v1215_l1, // verifica STAT dopo un cancel
            R.string.rn_v1215_l2, // articolo non piu' sul server + rimozione copia locale
            R.string.rn_v1215_l3, // Supersedes in lettura
            R.string.rn_v1215_l4, // ritentativo, SNI su IP, cache e interruttori
        ),
        // 1.2.14 (Marco, 6 settembre 2026): verifica del nome host sui
        // certificati TLS, da sola. Una voce sola nelle note: e' l'unica
        // modifica della release, e l'utente deve poterla collegare
        // immediatamente a un'eventuale connessione che smette di funzionare.
        R.string.rn_v1214_title to listOf(
            R.string.rn_v1214_l1,
        ),
        // 1.2.13 (Marco, 6 settembre 2026): correzione del "Errore: Software
        // caused connection abort" che compariva al posto del corpo passando
        // in fretta da un messaggio all'altro, piu' la richiesta di Merovingio
        // sullo User-Agent precompilato nelle identita' nuove.
        R.string.rn_v1213_title to listOf(
            R.string.rn_v1213_l1, // niente piu' errore di rete al posto del corpo
            R.string.rn_v1213_l2, // User-Agent precompilato nelle identita' nuove
        ),
        // 1.2.12 (Marco, 6 settembre 2026): release di sicurezza, nata da un
        // audit completo del codice (NeReO_sicurezza_audit.md). Le voci per
        // l'utente restano DUE e generiche di proposito: le altre correzioni
        // sono invisibili (non cambiano nulla nell'uso normale) e descriverle
        // in dettaglio nelle note dell'app significherebbe solo spiegare a
        // chiunque come si attaccava la versione precedente, che è ancora
        // installata su tutti i dispositivi che non hanno ancora aggiornato.
        // Il dettaglio completo, quello sì, è nei documenti di progetto.
        R.string.rn_v1212_title to listOf(
            R.string.rn_v1212_l1, // irrobustimento generale contro contenuti malformati
            R.string.rn_v1212_l2, // Sostituisci solo sui propri + nuovo Ritira (cancel)
        ),
        // 1.2.11 (Marco, 5 settembre 2026): Piano C del Dato 5
        // (NeReO_bug_1.2.9.md) — aggiornamento della libreria Room da 2.6.1 a
        // 2.8.4 (non 3.0: pacchetto separato androidx.room3, riscrittura
        // KMP scartata per costo/beneficio). La 2.8.4 include due correzioni
        // ufficiali su corse Flow-contro-scrittura-concorrente, la stessa
        // categoria del crash CursorWindow del Dato 5 — ABDUZIONE, non
        // conferma: le difese .catch già spedite nella 1.2.10 restano,
        // questo è un livello aggiuntivo. Nessun cambio di comportamento
        // visibile; da confermare con un giro di collaudo se il sintomo
        // sparisce davvero.
        R.string.rn_v1211_title to listOf(
            R.string.rn_v1211_l1, // aggiornamento libreria database interna
            R.string.rn_v1211_l2, // stessa difesa anti-crash estesa alla ricerca
        ),
        // 1.2.10 (Marco, 5 settembre 2026): Piano A per la voce Y di
        // NeReO_bug_1.2.9.md (Big Umberto, tramite Marco) — nuovo switch
        // dedicato per disattivare il sync incondizionato di un gruppo alla
        // sua apertura, che finora non aveva alcun interruttore proprio
        // (era gated solo dal debounce SyncGuard, mai da un'impostazione).
        // Insieme, PRIMA versione reale a portare il Piano B del Dato 5
        // (difesa in Repository.articles() contro il CursorWindow
        // IllegalStateException capitato a Macchia): era stato implementato
        // a versione invariata il 5 settembre, in attesa del prossimo giro
        // di consegna — che è proprio questo.
        R.string.rn_v1210_title to listOf(
            R.string.rn_v1210_l1, // lista articoli più robusta (Dato 5/Piano B)
            R.string.rn_v1210_l2, // switch sync automatico all'apertura gruppo
        ),
        // 1.2.9.c (Marco, 4 settembre 2026): Piano B di NeReO_bug_1.2.9.md
        // voce X (Dato 4) — il sync di testate non aveva nessuna guardia di
        // concorrenza contro il timer "Controllo automatico nuovi
        // messaggi": corretto con un mutex (stesso principio già in uso per
        // lo scaricamento dei corpi). Non è una correzione CONFERMATA del
        // crash segnalato (manca ancora uno stack trace che lo dimostri),
        // ma un difetto di concorrenza reale e dimostrato nel codice, che
        // vale la pena chiudere a prescindere.
        R.string.rn_v129c_title to listOf(
            R.string.rn_v129c_l1, // sincronizzazione più robusta
        ),
        // 1.2.9.b (Marco, 4 settembre 2026): tre correzioni dal collaudo
        // pubblico della 1.2.9 (ricerca, "mostra filtrati", User-Agent —
        // vedi NeReO_bug_1.2.9.md voci U/V/W) più il nuovo log crash minimo
        // (Piano C.1, voce X: nessuno stack trace disponibile per la
        // segnalazione "si chiude spesso improvvisamente", quindi l'app
        // impara a raccoglierselo da sola). Prima volta che una release
        // "a versione invariata" diventa una vera nuova versione: Marco
        // vuole ricompilarla e distribuirla agli utenti.
        R.string.rn_v129b_title to listOf(
            R.string.rn_v129b_l1, // ricerca: risultati non si azzerano più
            R.string.rn_v129b_l2, // mostra filtrati: audit completo
            R.string.rn_v129b_l3, // User-Agent vuoto = anonimo vero
            R.string.rn_v129b_l4, // nuovo: log ultimo arresto anomalo
        ),
        // 1.2.9 (Marco, 2 settembre 2026): multi-identità VERA (una per
        // server, come MesNews/NR) + tutta la roadmap NNTP rimasta (voce E:
        // STARTTLS, CAPABILITIES, riuso connessione, override auth per
        // server) — la stessa release, per esplicita scelta di Marco
        // ("mi piace rischiare"). Chiude il gate verso la 1.3 definitiva.
        R.string.rn_v129_title to listOf(
            R.string.rn_v129_l1, // multi-identità per server
            R.string.rn_v129_l2, // STARTTLS
            R.string.rn_v129_l3, // override per-server auth non cifrata
            R.string.rn_v129_l4, // riuso connessione lettura corpi
            R.string.rn_v129_l5, // CAPABILITIES in diagnostica
            R.string.rn_v129_l6, // backup/ripristino identità
        ),
        // 1.2.8 (Marco, 1 settembre 2026): CONSOLIDA in un'unica sezione ciò
        // che durante lo sviluppo era stato numerato 1.3 → 1.3.1 → 1.3.2 —
        // nessuna di quelle è mai uscita ufficialmente, solo Marco le aveva
        // in collaudo. La vera 1.3 arriverà solo a roadmap NNTP (voce E)
        // completata o esplicitamente scartata — vedi NeReO_storico_completo.md.
        // Le chiavi stringa (rn_v13_*, rn_v131_*, rn_v132_*) restano quelle
        // già tradotte e verificate: qui si è unito solo l'elenco, il titolo
        // di sezione (rn_v13_title) è stato riscritto per dire "1.2.8".
        // "Traduci" (era nella wishlist 1.3, Tier 3) è stato scartato
        // DEFINITIVAMENTE da Marco, non solo rimandato: non compare da
        // nessuna parte qui perché non era mai stato implementato.
        R.string.rn_v13_title to listOf(
            R.string.rn_v13_l1, // B — barre personalizzabili
            R.string.rn_v13_l2, // C — aggiornamento selettivo
            R.string.rn_v13_l3, // D — lettura a tutto schermo nei pannelli
            R.string.rn_v13_l4, // #5 — Cerca/Condividi sul testo selezionato
            R.string.rn_v131_l1, // controllo aggiornamenti a 4 modalità
            R.string.rn_v131_l2, // fix throttle "una volta al giorno"
            R.string.rn_v132_l1, // diagnostica NNTP: riepilogo overview + timing + sessione
            R.string.rn_v132_l2, // diagnostica manuale LISTGROUP + STAT
        ),
        // 1.2.7: dialog "connessione non sicura" (SSL/consenti), default risposta
        // crosspost a tutti i gruppi, separatori sotto-thread in vista continua.
        R.string.rn_v127_title to listOf(
            R.string.rn_v127_l1,
            R.string.rn_v127_l2,
            R.string.rn_v127_l3,
        ),
        // 1.2.6: sicurezza (auth mai in chiaro), scorciatoia aggiornamenti, plonk e
        // riordino gruppi nei pannelli, ordine gruppi nel backup, Face nascosto.
        R.string.rn_v126_title to listOf(
            R.string.rn_v126_l1,
            R.string.rn_v126_l2,
            R.string.rn_v126_l3,
            R.string.rn_v126_l4,
            R.string.rn_v126_l5,
            R.string.rn_v126_l6,
            R.string.rn_v126_l7,
            R.string.rn_v126_l8,
        ),
        // 1.2.5: marcatura letti in vista continua (solo ciò che si vede),
        // gruppi crosspost su ogni post, testo stato-vuoto, controllo aggiornamenti.
        R.string.rn_v125_title to listOf(
            R.string.rn_v125_l1,
            R.string.rn_v125_l2,
            R.string.rn_v125_l3,
            R.string.rn_v125_l4,
        ),
        // 1.2.4: riflusso testo — unione delle righe orfane in mezzo ai capoversi.
        R.string.rn_v124_title to listOf(
            R.string.rn_v124_l1,
        ),
        // 1.2.3: correzioni in testa. Colore capi-thread (albero), crosspost
        // nella vista continua, niente più "connection reset" sui corpi, e
        // icona di aggiornamento come default nei pannelli.
        R.string.rn_v123_title to listOf(
            R.string.rn_v123_l1,
            R.string.rn_v123_l2,
            R.string.rn_v123_l3,
            R.string.rn_v123_l4,
            R.string.rn_v123_l5,
            R.string.rn_v123_l6,
        ),
        // 1.2.2: correzioni (regressioni landscape + timeout SSL).
        R.string.rn_v122_title to listOf(
            R.string.rn_v122_l4,
            R.string.rn_v122_l5,
            R.string.rn_v122_l1,
            R.string.rn_v122_l2,
            R.string.rn_v122_l3,
            R.string.rn_v122_l6,
            R.string.rn_v122_l7,
            R.string.rn_v122_l8,
        ),
        // 1.2: compatibilità server (Astraweb) in testa — chiude il caso più
        // sentito di questo ciclo. Poi plonk esteso, parità tablet, gruppi,
        // e la parte "cura" (backup + diagnostica + correzioni).
        R.string.rn_v12_compat_title to listOf(
            R.string.rn_v12_compat_l1,
        ),
        R.string.rn_m1_title to listOf(
            R.string.rn_m1_l1,
            R.string.rn_m1_l2,
            R.string.rn_m1_l3,
        ),
        R.string.rn_v12_tablet_title to listOf(
            R.string.rn_v12_tablet_l1,
            R.string.rn_v12_tablet_l2,
        ),
        R.string.rn_v12_groups_title to listOf(
            R.string.rn_v12_groups_l1,
            R.string.rn_v12_groups_l2,
        ),
        R.string.rn_v12_care_title to listOf(
            R.string.rn_v12_care_l1,
            R.string.rn_v12_care_l2,
            R.string.rn_v12_care_l3,
        ),
    )
}
