/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Lettura articolo, stile Piao Hong: titolo con posizione [n/tot] nel gruppo,
 * barra inferiore con ◀ precedente / ▶ successivo (nell'ordine della vista
 * thread corrente), ritorno all'indice, salto a inizio/fine messaggio.
 * Il cambio messaggio è stato locale: niente pile di schermate nel back stack.
 */
package org.openusenet.reader.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.TastiVolume
import org.openusenet.reader.ui.icone.ArrowBack
import org.openusenet.reader.ui.icone.Reply
import org.openusenet.reader.ui.icone.AccountTree
// 1.3.6 (ObiWan): icone delle due azioni nuove. Sono in material-icons-extended,
// la dipendenza che il progetto usa gia' (libs.compose.material.icons).
// SE NON COMPILA: ripiegare su ContentCopy per entrambe — e' gia' importata e
// il significato lo porta l'etichetta, non l'icona.
import org.openusenet.reader.ui.icone.Block
import org.openusenet.reader.ui.icone.EnhancedEncryption
import org.openusenet.reader.ui.icone.MoreVert
import org.openusenet.reader.ui.icone.Subject
import org.openusenet.reader.ui.icone.Markunread
import org.openusenet.reader.ui.icone.Star
import org.openusenet.reader.ui.icone.StarBorder
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import org.openusenet.reader.ui.rememberAppunti
import org.openusenet.reader.ui.CorpoCitabile
import org.openusenet.reader.ui.articoloEml
import org.openusenet.reader.ui.articoloGrezzo
import org.openusenet.reader.ui.condividiTesto
import org.openusenet.reader.text.formatoCorpo
import org.openusenet.reader.ui.nomeFileConserva
import org.openusenet.reader.ui.nomeFileEml
import org.openusenet.reader.ui.rememberConserva
import org.openusenet.reader.ui.rememberSalvaEml
// 1.3.6 (ObiWan): link permanente e Message-ID — vedi ui/LinkArticolo.kt.
import org.openusenet.reader.ui.linkUsenetId
import org.openusenet.reader.ui.messageIdCanonico
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
import org.openusenet.reader.data.Article
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.ui.Routes
import org.openusenet.reader.ui.components.QuoteColoredBody
import org.openusenet.reader.ui.components.readStateColor
import org.openusenet.reader.ui.components.readingColors
import org.openusenet.reader.ui.indietroSicuro

// 1.5.5: MAX_FACE_LATO e la decodifica della Face sono in IntestazioneAutore.kt.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArticleScreen(repo: NewsRepository, nav: NavHostController, articleId: Long, searchQuery: String = "") {
    // 1.4.1 (105/C, BIG Umberto) — rememberSaveable, non remember.
    // Spostandosi dentro la discussione questo valore cambia, ma non e' una
    // nuova destinazione di navigazione: vive solo nella composizione. Andando
    // alla schermata di risposta la composizione se ne va, e al ritorno un
    // `remember` normale non viene ripristinato: si riparte dall'argomento di
    // rotta, cioe' dall'articolo aperto dall'elenco. Umberto: "avevo replicato
    // al 23esimo e mi ritrovo al primo". Confermato dalla sua prova A/B con
    // "Non conservare le attivita'": spento resta sul 15, acceso torna sul 1.
    var currentId by rememberSaveable { mutableStateOf(articleId) }
    var article by remember { mutableStateOf<Article?>(null) }
    var body by remember { mutableStateOf<String?>(null) }
    /*
     * 1.4 — "Ricarica il messaggio".
     *
     * PERCHE' SERVE, e perche' la correzione del charset SENZA questa sarebbe
     * stata invisibile: `fetchBody` decodifica e **SALVA il testo decodificato**
     * (`setBody`), i byte grezzi non si conservano. Correggere il decoder non
     * ripara quindi un solo articolo gia' in archivio: chi ha gli accenti
     * distrutti continuerebbe a vederli distrutti dopo l'aggiornamento, e il
     * primo collaudo direbbe "non e' cambiato niente".
     *
     * 1.4.2 — QUI LA 1.4 AVEVA SBAGLIATO FUNZIONE, e va detto.
     * Il pulsante chiamava `eliminaCopiaLocale`, che per il nome sembrava
     * "togli la copia in cache" e invece faceva `DELETE FROM articles`:
     * "ricarica" cancellava l'articolo. E siccome `syncGroup` riparte dal
     * segnalibro del gruppo, quell'articolo non tornava piu' senza riscaricare
     * l'intero gruppo. Ora si chiama `invalidaCorpo` e azzera il solo corpo;
     * la funzione che cancella la riga si chiama `eliminaArticoloLocale` e
     * resta dov'e' giusto che sia, nel dialogo del "430 No such article"
     * (1.2.15).
     *
     * Scartata l'alternativa "conservare anche i byte grezzi": raddoppierebbe
     * lo spazio della cache per riparare un caso che si ripara con un
     * ri-scarico.
     *
     * Cambiare questa chiave rilancia il LaunchedEffect che scarica il corpo:
     * e' lo stesso meccanismo del cambio di articolo, non una strada nuova.
     */
    var ricaricaChiave by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    /**
     * 1.2.15 (voce B): id dell'articolo che sul server non esiste più. Non
     * basta il testo dell'errore — serve sapere SU COSA agire per offrire il
     * pulsante che elimina la copia locale.
     */
    var mancanteId by remember { mutableStateOf<Long?>(null) }
    var watched by remember { mutableStateOf(false) }
    var showPlonkDialog by remember { mutableStateOf(false) }
    var showHeaders by remember { mutableStateOf(false) }
    var showTree by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    var rot13View by remember(articleId) { mutableStateOf(false) }
    // wishlist beta11: badge crosspost visibile nella lista (vedi ~riga 486).
    // beta17: il vecchio dialog-avviso "sola lettura" al tocco di Rispondi è
    // stato rimosso — la scelta dei gruppi ora avviene in ComposeScreen col
    // selettore a spunte.
    var diagnostica by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    // 1.3.3: appunti per l'azione "copia" della barra (vedi barActions).
    val appunti = rememberAppunti() // 1.3.5: unico punto, vedi ui/Appunti.kt
    val avvisoCopia = stringResource(R.string.art_copy_done)
    // 1.3.6: risolto qui perche' condividiTesto non e' composable.
    val titoloCondivisione = stringResource(R.string.art_export_chooser)

    /*
     * 1.3.6 (secondo giro): copia + avviso, estratta.
     *
     * Su Android 13+ il sistema mostra da se' la conferma di copia; sotto no, e
     * senza un riscontro l'utente non sa se e' successo qualcosa. Erano cinque
     * righe dentro l'azione "copia"; con le due azioni nuove di ObiWan sarebbero
     * diventate tre copie identiche. Regola del progetto: prima di aggiungere la
     * N-esima copia di una logica, estrarla — vale anche per cinque righe.
     */
    fun copiaNegliAppunti(testo: String) {
        if (testo.isBlank()) return
        appunti.copia(testo)
        if (android.os.Build.VERSION.SDK_INT < 33) {
            android.widget.Toast.makeText(ctx, avvisoCopia, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    // wishlist beta12 (Marco): lo swipe verso il messaggio successivo deve
    // rispettare "solo non letti", non camminare sull'intera lista come fa
    // oggi — e avvisare invece di restare zitto quando non c'è più nulla,
    // stessa lezione della regressione beta7 già documentata per goNext().
    val snackbar = remember { SnackbarHostState() }
    // 1.4.5 (§B9): il selettore di sistema per salvare l'articolo come .eml.
    // Va registrato nella composizione, non dentro un onClick — vedi
    // ui/EsportaEml.kt, dove sta anche il motivo per cui non lo registrano
    // tutte e due le schermate per conto proprio.
    val salvaEml = rememberSalvaEml()
    // 1.4.9 (§J3-C1): il selettore di CARTELLA, registrato per la stessa
    // ragione. Vedi ui/Conserva.kt.
    val conserva = rememberConserva()

    // articoli del gruppo, nell'ordine della vista corrente → posizione e prev/next
    val groupId = article?.groupId
    // 1.4.1 (105/D): `showHidden` era scritto fisso a false. Ora segue
    // l'impostazione condivisa, ed e' anche una CHIAVE del remember: senza,
    // cambiando l'opzione questa Flow non verrebbe ricreata.
    val groupArticles by remember(groupId, AppSettings.mostraFiltrati) {
        groupId?.let {
            repo.articles(it, showHidden = AppSettings.mostraFiltrati, unreadOnly = false)
        } ?: flowOf(emptyList<Article>())
    }.collectAsState(initial = emptyList())
    // Sequenza usata dallo swipe per prev/next messaggio.
    //
    // ATTENZIONE (regressione beta7): la vista "Discussioni raggruppate"
    // restituisce SOLO le radici, e per giunta il suo stato di apertura vive
    // nella schermata lista, non qui. Costruendo la sequenza con quella vista,
    // leggendo una risposta si otteneva position = -1: da li' goNext() cadeva
    // su flat[0] (salto alla prima discussione del gruppo) e goPrev() non
    // faceva nulla. Il raggruppamento e' un fatto di PRESENTAZIONE della lista;
    // dentro l'articolo la sequenza di lettura e' quella dell'albero completo.
    val navView = if (AppSettings.threadView == ThreadViews.ROOTS)
        ThreadViews.TREE else AppSettings.threadView
    val flat = remember(groupArticles, navView, AppSettings.threadSort) {
        buildThreadList(groupArticles, unreadOnly = false, view = navView)
    }
    val position = flat.indexOfFirst { it.article.id == currentId }
    val total = flat.size

    // beta14 (Marco): confini della DISCUSSIONE corrente dentro flat. Le frecce
    // singole (< >) devono muoversi solo dentro la discussione e fermarsi ai
    // suoi estremi; per cambiare discussione ci sono le doppie frecce (<< >>).
    // Il contatore in alto mostra posizione/totale DELLA DISCUSSIONE, non del
    // gruppo intero. Uso threadStart come marcatore di inizio discussione (lo
    // stesso usato per il dialogo albero).
    // 1.4.6 (§B4): i confini della discussione li calcola NavigazioneLettura,
    // che li condivide con la vista a pannelli invece di averne una copia.
    val confini = remember(flat, position) { confiniDiscussione(flat, position) }
    val threadStartIdx = confini.first
    val threadEndIdx = confini.second // esclusivo
    // posizione (1-based) e totale DENTRO la discussione corrente
    val posInThread = if (position < 0) 0 else position - threadStartIdx + 1
    val totalInThread = threadEndIdx - threadStartIdx

    // albero completo del gruppo: base per la navigazione fra messaggi e discussioni
    val tree = remember(groupArticles, AppSettings.threadSort) {
        buildThreadList(groupArticles, unreadOnly = false, view = ThreadViews.TREE)
    }
    // discussione corrente (per il dialogo "Albero della conversazione")
    val threadItems = remember(tree, currentId) {
        val idx = tree.indexOfFirst { it.article.id == currentId }
        if (idx < 0) emptyList() else {
            var start = idx
            while (start > 0 && !tree[start].threadStart) start--
            var end = idx + 1
            while (end < tree.size && !tree[end].threadStart) end++
            tree.subList(start, end).toList()
        }
    }

    // navigazione fra MESSAGGI (nell'ordine della vista corrente)
    //
    // Il controllo su position >= 0 e' una rete di sicurezza: se per qualunque
    // motivo l'articolo corrente non fosse nella sequenza, position vale -1 e
    // getOrNull(position + 1) restituirebbe il PRIMO elemento invece di niente
    // — un salto silenzioso all'inizio del gruppo, il sintomo peggiore perche'
    // sembra che il gesto funzioni.
    // 1.4.6 (§B4): il COSA cercare sta in NavigazioneLettura, provato in
    // CoreTests; qui resta il COSA FARNE, che e' l'unica parte non condivisibile.
    fun goNext() { idMessaggioSuccessivo(flat, position)?.let { currentId = it } }
    fun goPrev() { idMessaggioPrecedente(flat, position)?.let { currentId = it } }

    /**
     * Salta al primo messaggio DA LEGGERE dopo quello corrente, nell'ordine
     * della vista. Il messaggio attuale, appena aperto, risulta già letto:
     * per questo si parte da position+1 e non da position. Restituisce false
     * se non ce ne sono altri (il chiamante avvisa invece di restare fermo
     * in silenzio — vedi la lezione della regressione beta7).
     */
    fun goNextUnread(): Boolean {
        val next = idPrimoNonLettoDopo(flat, position) ?: return false
        currentId = next
        return true
    }
    val esistonoNonLetti = esistonoNonLettiDopo(flat, position)

    // navigazione fra DISCUSSIONI (salta all'inizio del thread seguente/precedente)
    fun goNextThread() { idDiscussioneSuccessiva(tree, currentId)?.let { currentId = it } }
    // 1.5.7 / 175 (F8): i tasti del volume sfogliano, finche' questa schermata e'
    // visibile. rememberUpdatedState: il gestore registrato una volta chiama
    // sempre le funzioni della composizione PIU' RECENTE (posizione attuale).
    val avantiAttuale by rememberUpdatedState({ goNext() })
    val indietroAttuale by rememberUpdatedState({ goPrev() })
    DisposableEffect(Unit) {
        val mio: (TastiVolume.Verso) -> Unit = { v ->
            if (v == TastiVolume.Verso.AVANTI) avantiAttuale() else indietroAttuale()
        }
        TastiVolume.gestore = mio
        onDispose { if (TastiVolume.gestore === mio) TastiVolume.gestore = null }
    }
    fun goPrevThread() { idDiscussionePrecedente(tree, currentId)?.let { currentId = it } }

    /**
     * Wrapper per lo swipe (wishlist beta12, segnalato da Marco): con "solo
     * non letti" attivo, avanzare deve saltare i già letti come fa il menu
     * "vai al primo non letto" — non camminare sull'intera lista come
     * [goNext] da solo farebbe. Se non c'è più nulla da leggere, avvisa
     * invece di restare zitto (stessa lezione della regressione beta7).
     *
     * 1.2.9 (Piano B, segnalazione di Marco — 2 settembre 2026): con "solo
     * non letti" SPENTO, [goNext] si fermava zitto all'ultimo messaggio
     * della discussione (fermata voluta fin da beta14, per le sole FRECCE
     * singole < >), mentre con "solo non letti" ACCESO [goNextUnread]
     * attraversa già le discussioni senza fermarsi — la differenza si
     * percepiva come "lo swipe non funziona". Corretto SOLO qui (swipe),
     * non in [goNext]: le frecce singole restano bloccate alla discussione
     * come deciso in beta14, il swipe ora si comporta come le doppie
     * frecce quando arriva al bordo, invece di restare zitto.
     */
    fun swipeNext() {
        if (AppSettings.unreadOnly) {
            if (!goNextUnread()) {
                scope.launch { snackbar.showSnackbar(ctx.getString(R.string.art_all_read)) }
            }
            return
        }
        if (position + 1 < threadEndIdx) {
            goNext()
            return
        }
        val prima = currentId
        goNextThread()
        if (currentId == prima) {
            scope.launch { snackbar.showSnackbar(ctx.getString(R.string.art_end_of_group)) }
        }
    }

    /**
     * Simmetrico di [swipeNext] per lo swipe indietro (1.2.9, stessa
     * correzione estesa per coerenza anche se la segnalazione di Marco
     * riguardava solo l'avanti): con [goPrev] fermo al primo messaggio
     * della discussione, salta all'inizio della discussione precedente
     * invece di restare zitto. Non tocca [goPrev], usato anche dalla
     * freccia singola "<".
     */
    fun swipePrev() {
        if (position - 1 >= threadStartIdx) {
            goPrev()
            return
        }
        val prima = currentId
        goPrevThread()
        if (currentId == prima) {
            scope.launch { snackbar.showSnackbar(ctx.getString(R.string.art_start_of_group)) }
        }
    }


    LaunchedEffect(currentId, ricaricaChiave) {
        body = null
        error = null
        article = repo.articleById(currentId)
        watched = article?.watched ?: false
        scroll.scrollTo(0)
        // beta13 nota (evidenziazione ricerca, livello 2): NON forziamo uno
        // scroll automatico alla prima occorrenza. Il corpo è un verticalScroll
        // semplice (non LazyColumn) reso da QuoteColoredBody a blocchi, quindi
        // non conosciamo la posizione in pixel di una parola senza misurare il
        // layout — soluzione fragile. L'evidenziazione gialla (già attiva) rende
        // la parola trovabile a colpo d'occhio scorrendo: è il grosso del valore,
        // senza il rischio di scroll che saltano nel posto sbagliato.
        // #8: NON catturare la CancellationException. Scorrendo veloce tra i
        // messaggi il LaunchedEffect precedente viene annullato a metà fetchBody:
        // è cooperazione normale, non un errore. runCatching la ingoiava e la
        // mostrava come "The coroutine scope left the composition". Ora la
        // cancellazione viene rilanciata; si mostrano solo gli errori veri.
        mancanteId = null
        try {
            body = repo.fetchBody(currentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: org.openusenet.reader.data.ArticoloMancanteException) {
            // 1.2.15 (voce B): l'articolo non è più sul server. Non è un guasto
            // da mostrare come tale: è una copia locale rimasta orfana, e
            // l'unica cosa sensata da offrire è toglierla di mezzo.
            error = e.message
            mancanteId = e.articleId
        } catch (e: Exception) {
            error = e.message
        }
        article = repo.articleById(currentId)
        // dopo aver mostrato questo messaggio, porta avanti i corpi delle
        // risposte dirette cosi' scorrere il thread e' immediato. In background,
        // senza bloccare: se fallisce, verranno scaricate alla lettura.
        if (AppSettings.prefetchReplies) {
            launch { runCatching { repo.prefetchReplies(currentId) } }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (totalInThread > 0 && position >= 0) "[$posInThread/$totalInThread]" else stringResource(R.string.art_title),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.indietroSicuro() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // beta17: la scelta dei gruppi crosspost avviene ORA dentro
                        // ComposeScreen (pulsante "Gruppi" in toolbar + selettore),
                        // che appare da sé se il messaggio è crospostato. Quindi qui
                        // navigo sempre diretto: il vecchio dialog di sola lettura
                        // (che diceva "la risposta andrà solo qui") era obsoleto e
                        // bloccava il nuovo flusso a spunte.
                        nav.navigate(Routes.composeReply(currentId))
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = stringResource(R.string.art_reply_cd))
                    }
                    IconButton(onClick = { showTree = true }) {
                        Icon(Icons.Default.AccountTree, contentDescription = stringResource(R.string.art_tree_cd))
                    }
                    IconButton(onClick = { showHeaders = true }) {
                        Icon(Icons.AutoMirrored.Filled.Subject, contentDescription = stringResource(R.string.art_headers_cd))
                    }
                    // 1.4.4 (Dave Royal): il Box E' L'ANCORA. Senza, DropdownMenu si
                    // aggancia al nodo GENITORE (Popup usa parentLayoutCoordinates),
                    // e in una Row a tutta larghezza il menu parte dal bordo dello
                    // schermo invece che da sotto l'icona.
                    Box {
                        IconButton(onClick = { overflow = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.art_more_cd))
                        }
                        DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                            DropdownMenuItem(
                                text = { Text(if (watched) stringResource(R.string.art_unwatch) else stringResource(R.string.art_watch)) },
                                leadingIcon = {
                                    Icon(if (watched) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null)
                                },
                                onClick = {
                                    watched = !watched
                                    // 2.1(a): segui/non-segui su TUTTA la discussione,
                                    // non solo sul messaggio aperto. Gli id del thread
                                    // vengono dal flat di navigazione. GUARDIA: con
                                    // position < 0 (messaggio non nel flat) threadStart/
                                    // End coprirebbero l'intero gruppo — in quel caso
                                    // ripiego sul solo messaggio corrente.
                                    val idsThread = if (position in flat.indices)
                                        flat.subList(threadStartIdx, threadEndIdx).map { it.article.id }
                                    else listOf(currentId)
                                    scope.launch { repo.setWatchedBulk(idsThread, watched) }
                                    overflow = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.art_mark_unread)) },
                                leadingIcon = { Icon(Icons.Default.Markunread, contentDescription = null) },
                                onClick = {
                                    scope.launch { repo.setRead(currentId, false) }
                                    overflow = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (rot13View) stringResource(R.string.art_rot13_orig) else stringResource(R.string.art_rot13_dec)) },
                                leadingIcon = { Icon(Icons.Default.EnhancedEncryption, contentDescription = null) },
                                onClick = { rot13View = !rot13View; overflow = false },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.art_plonk_menu)) },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = { showPlonkDialog = true; overflow = false },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            // 1.3 (B — barre personalizzabili, pilota su questa barra: vedi
            // NeReO_feature_barre.md e AppSettings.articleBarOrder/Hidden).
            // Ogni azione è un oggetto (id, icona, etichetta, enabled, onClick)
            // invece di un composable cablato: la barra si limita a scorrere
            // l'ordine salvato (filtrando le nascoste) e a renderizzare quello
            // che trova. L'editor è CustomizeBarsScreen; qui non cambia nulla
            // del comportamento di ciascuna azione, solo COME sono assemblate.
            val barActions = listOf(
                ArticleBarAction(
                    id = "prev_msg",
                    enabled = position > threadStartIdx,
                    onClick = { goPrev() },
                ),
                ArticleBarAction(
                    id = "next_msg",
                    enabled = position in 0 until (threadEndIdx - 1),
                    onClick = { goNext() },
                ),
                ArticleBarAction(
                    id = "next_unread",
                    enabled = esistonoNonLetti,
                    onClick = { goNextUnread() },
                ),
                ArticleBarAction(
                    id = "prev_thread",
                    onClick = { goPrevThread() },
                ),
                ArticleBarAction(
                    id = "next_thread",
                    onClick = { goNextThread() },
                ),
                ArticleBarAction(
                    id = "back_index",
                    onClick = { nav.indietroSicuro() },
                ),
                ArticleBarAction(
                    id = "top",
                    enabled = scroll.maxValue > 0,
                    onClick = { scope.launch { scroll.animateScrollTo(0) } },
                ),
                ArticleBarAction(
                    id = "bottom",
                    enabled = scroll.maxValue > 0,
                    onClick = { scope.launch { scroll.animateScrollTo(scroll.maxValue) } },
                ),
                // 1.2.6 (Dave Royal): muta/mostra citazioni (globale).
                ArticleBarAction(
                    id = "mute_quotes",
                    tinted = AppSettings.mutaCitazioni,
                    onClick = { AppSettings.mutaCitazioni = !AppSettings.mutaCitazioni; AppSettings.save() },
                ),
                // beta18 (richiesta tester): Albero della conversazione anche in
                // basso. Riusa lo stesso showTree della barra alta. Visibilità
                // ora governata dall'editor (AppSettings.articleBarHidden), non
                // più dal vecchio interruttore diretto alberoInBasso (migrato).
                ArticleBarAction(
                    id = "tree_bottom",
                    onClick = { showTree = true },
                ),
                // 1.3.3 (Roxy): copia l'INTERO messaggio con un clic, senza
                // dover prima selezionare. Il gemello di questa azione sta
                // nella barra dei pannelli — vedi BarraLetturaPannello. Qui il
                // testo si legge direttamente da `body`, che e' nello stesso
                // composable; nei pannelli arriva per lambda perche' la barra
                // e' una funzione separata.
                ArticleBarAction(
                    id = "copia",
                    enabled = !body.isNullOrBlank(),
                    onClick = {
                        copiaNegliAppunti(if (rot13View) rot13(body.orEmpty()) else body.orEmpty())
                    },
                ),
                // 1.3.6 (Adam H. Kerman): consegna l'articolo grezzo a un'altra
                // app — intestazioni comprese, cosi' un client di posta puo'
                // farne una risposta privata. Vedi ui/EsportaArticolo.kt.
                ArticleBarAction(
                    id = "esporta",
                    enabled = !body.isNullOrBlank(),
                    onClick = {
                        condividiTesto(
                            ctx,
                            articoloGrezzo(article?.headersRaw, if (rot13View) rot13(body.orEmpty()) else body),
                            titoloCondivisione,
                        )
                    },
                ),
                // 1.4.5 (§B9, Adam H. Kerman): l'altra meta' della richiesta del
                // 13 settembre — un FILE, non un testo condiviso. Il selettore
                // di sistema decide dove finisce, quindi nessun permesso nuovo.
                // La riscrittura del charset sta in ui/EsportaEml.kt e c'e' una
                // ragione precisa: il corpo in archivio e' gia' decodificato.
                ArticleBarAction(
                    id = "eml",
                    enabled = !body.isNullOrBlank(),
                    onClick = {
                        salvaEml(
                            nomeFileEml(article?.subject.orEmpty(), article?.messageId.orEmpty()),
                            articoloEml(article?.headersRaw, if (rot13View) rot13(body.orEmpty()) else body),
                        )
                    },
                ),
                /*
                 * 1.4.9 (§J3-C1) — «conserva questo», da una domanda di Jeff.
                 *
                 * Stesso contenuto di "eml" e altra destinazione: non il
                 * selettore di sistema a ogni salvataggio, ma la cartella scelta
                 * una volta. Serve a mettere da parte un messaggio dopo l'altro,
                 * che col selettore nessuno fa.
                 *
                 * Il nome del file porta anche la parte locale del Message-ID:
                 * in una cartella che si riempie, due «Re: ciao» si
                 * sovrascriverebbero. Vedi ui/Conserva.kt.
                 */
                ArticleBarAction(
                    id = "conserva",
                    enabled = !body.isNullOrBlank(),
                    onClick = {
                        conserva.conserva(
                            nomeFileConserva(article?.subject.orEmpty(), article?.messageId.orEmpty()),
                            articoloEml(article?.headersRaw, if (rot13View) rot13(body.orEmpty()) else body),
                        )
                    },
                ),
                /*
                 * 1.3.6 (secondo giro — ObiWan, 14 settembre 2026).
                 *
                 * Due azioni, non una, perche' rispondono a due bisogni diversi:
                 *  - il LINK serve a mandare qualcuno a leggere quel messaggio
                 *    (fuori da Usenet: una chat, una mail, un forum);
                 *  - il MESSAGE-ID serve dentro Usenet — citarlo in un
                 *    messaggio, metterlo in un References, allegarlo a una
                 *    segnalazione. Fino a ieri non c'era NESSUN modo di
                 *    ottenerlo senza aprire le intestazioni e selezionarlo a
                 *    mano.
                 *
                 * Entrambe si disattivano da sole se l'articolo non ha un
                 * Message-ID utilizzabile: vedi ui/LinkArticolo.kt, che
                 * restituisce stringa vuota invece di un link monco.
                 */
                ArticleBarAction(
                    id = "link",
                    enabled = linkUsenetId(article?.messageId).isNotEmpty(),
                    onClick = { copiaNegliAppunti(linkUsenetId(article?.messageId)) },
                ),
                // 1.4: forza il ri-scarico del corpo dal server. Vedi
                // ricaricaChiave qui sopra.
                ArticleBarAction(
                    id = "ricarica",
                    enabled = article != null,
                    onClick = {
                        val id = currentId
                        scope.launch {
                            // 1.4.2: qui c'era eliminaCopiaLocale, che cancella
                            // la RIGA. Il pulsante "ricarica" cancellava quindi
                            // l'articolo invece di riscaricarlo, e siccome il
                            // sync riparte dal segnalibro del gruppo, non
                            // tornava piu'. Ora si azzera il solo corpo.
                            runCatching { repo.invalidaCorpo(id) }
                            ricaricaChiave++
                        }
                    },
                ),
                // 1.4.6 (par. B13, BIG Umberto): porta all'elenco del gruppo
                // posizionato su questo messaggio, con la sua discussione
                // aperta. Spenta se il gruppo non e' noto — un comando che non
                // puo' agire si spegne, non finge.
                //
                // 1.4.8 (par. U3, BIG Umberto, 6 ottobre) — LA PILA NON SI
                // IMPILA PIU'. *«Per uscire con la freccia in alto fa un po'
                // di avanti e indietro fra messaggio e gruppo.»* La rotta
                // veniva aperta con un `navigate` nudo (in tutta la
                // navigazione non c'era un solo `popUpTo`), quindi l'elenco
                // finiva SOPRA l'articolo da cui si era partiti e la freccia
                // lo ripercorreva a ritroso.
                //
                // Si toglie l'ARTICOLO, e si toglie solo quello. E' il punto
                // da cui si viene e in cui non si vuole tornare — ci si e'
                // appena andati sopra, nell'elenco. Sotto resta la ricerca,
                // che e' dove si vuole poter tornare per saltare al risultato
                // successivo.
                //
                // ⚠️ NON si toglie l'elenco discussioni, che era la prima
                // forma pensata: `popUpTo` svuota tutto cio' che sta SOPRA la
                // destinazione nominata, quindi puntare all'elenco avrebbe
                // portato via anche la ricerca — l'opposto dell'intenzione.
                // *Verificata la semantica, non la memoria.*
                ArticleBarAction(
                    id = "mostra_in_elenco",
                    enabled = article?.groupId != null,
                    onClick = {
                        val gid = article?.groupId
                        val aid = article?.id
                        if (gid != null && aid != null) {
                            nav.navigate(Routes.threadsSu(gid, aid)) {
                                popUpTo(Routes.ARTICLE_PATTERN) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                ),
                ArticleBarAction(
                    id = "msgid",
                    enabled = messageIdCanonico(article?.messageId).isNotEmpty(),
                    onClick = { copiaNegliAppunti(messageIdCanonico(article?.messageId)) },
                ),
            ).associateBy { it.id }
            // #3 (Bingo3331): la barra a icone va a capo su due righe quando non
            // c'entrano tutte, invece di far sparire l'ultima icona oltre il
            // bordo. Icone a 48dp, niente weight.
            Surface(tonalElevation = 3.dp) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AppSettings.articleBarOrder.forEach { id ->
                        if (id in AppSettings.articleBarHidden) return@forEach
                        val a = barActions[id] ?: return@forEach
                        val voce = azioneBarra(id) ?: return@forEach
                        val etichetta = stringResource(voce.etichetta)
                        TooltipIconButton(
                            label = etichetta,
                            enabled = a.enabled,
                            onClick = a.onClick,
                        ) {
                            Icon(
                                voce.icona,
                                contentDescription = etichetta,
                                tint = if (a.tinted) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        val (readBg, readFg) = readingColors()
        // Il rilevatore di gesti sta sul contenitore ESTERNO e ascolta nella fase
        // Initial: gli eventi arrivano dal genitore verso i figli, quindi li vede
        // PRIMA che la zona di scorrimento li consumi. Non consuma nulla, così
        // scorrimento, selezione del testo e link continuano a funzionare.
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(readBg)
                .then(
                    if (AppSettings.gesturesEnabled) {
                        Modifier.pointerInput(currentId, flat, tree) {
                            val soglia = 110.dp.toPx()
                            awaitEachGesture {
                                val giu = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                val tDown = giu.uptimeMillis
                                var tUp = tDown
                                // posizione del testo all'INIZIO del gesto: le verticali
                                // navigano solo se si parte già dal bordo (niente scatti
                                // involontari mentre si scorre nel mezzo del messaggio)
                                val partitoDaCima = scroll.value <= 0
                                val partitoDaFondo = scroll.value >= scroll.maxValue
                                var dx = 0f
                                var dy = 0f
                                var attivo = true
                                while (attivo) {
                                    val evento = awaitPointerEvent(PointerEventPass.Initial)
                                    evento.changes.forEach { c ->
                                        val d = c.positionChangeIgnoreConsumed()
                                        dx += d.x
                                        dy += d.y
                                        tUp = c.uptimeMillis
                                    }
                                    attivo = evento.changes.any { it.pressed }
                                }
                                // un gesto lungo è una pressione per selezionare, non uno swipe: non naviga
                                val rapido = (tUp - tDown) < 500
                                val orizzontale = kotlin.math.abs(dx) > kotlin.math.abs(dy)
                                val esito = when {
                                    !rapido -> ctx.getString(R.string.gst_none_long)
                                    orizzontale && dx > soglia -> { goNextThread(); ctx.getString(R.string.gst_next_thread) }
                                    orizzontale && dx < -soglia -> { goPrevThread(); ctx.getString(R.string.gst_prev_thread) }
                                    // in fondo, scorri ancora verso l'alto → messaggio successivo
                                    !orizzontale && dy < -soglia && partitoDaFondo -> { swipeNext(); ctx.getString(R.string.gst_next_msg) }
                                    // in cima, tira verso il basso → messaggio precedente
                                    !orizzontale && dy > soglia && partitoDaCima -> { swipePrev(); ctx.getString(R.string.gst_prev_msg) }
                                    else -> ctx.getString(R.string.gst_none)
                                }
                                if (AppSettings.gestureDebug) {
                                    diagnostica = "dx=%.0f dy=%.0f | scroll=%d/%d | %s".format(
                                        dx, dy, scroll.value, scroll.maxValue, esito,
                                    )
                                }
                            }
                        }
                    } else Modifier
                ),
        ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
        ) {
            article?.let { a ->
                Text(a.subject, style = MaterialTheme.typography.titleMedium, color = readFg)
                // wishlist beta11 (Marco odia il crossposting): badge visibile già in
                // lettura, non solo al momento di rispondere. Nessuna nuova richiesta
                // di rete: legge Newsgroups: da headersRaw, già scaricato con fetchBody.
                val crossgroups = remember(a.headersRaw) { crosspostGroups(a.headersRaw) }
                if (crossgroups.size > 1) {
                    Text(
                        stringResource(R.string.art_crossposted, crossgroups.size, crossgroups.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                    )
                }
                // 1.5.5 (G1 + G3): Face o iniziali colorate, nome, indirizzo,
                // data «oggi / ieri / giorno». La decodifica della Face, con le
                // difese della 1.2.12, e' SPOSTATA in IntestazioneAutore.kt: le
                // tre viste di lettura ora la condividono.
                IntestazioneAutore(a.fromField, a.dateEpoch, a.faceB64, a.headersRaw, readFg)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
            when {
                error != null -> Column {
                    Text(stringResource(R.string.art_error, error ?: ""), color = MaterialTheme.colorScheme.error)
                    // 1.2.15 (voce B): la via d'uscita accanto al messaggio,
                    // non in un menu da cercare. Compare solo quando c'è
                    // davvero qualcosa da rimuovere.
                    mancanteId?.let { id ->
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { repo.eliminaArticoloLocale(id) }
                                nav.indietroSicuro()
                            }
                        }) { Text(stringResource(R.string.art_missing_remove)) }
                    }
                }
                body == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                // 1.3.4: il corpo NON e' piu' avvolto da WithSelectionActions.
                // Quel meccanismo sostituiva LocalTextToolbar per aggiungere
                // "Cerca" e "Condividi" al menu di selezione (voce #5 di Dave
                // Royal, 1.2.8). Compose 1.12 non passa piu' da li': ora il menu
                // e' quello della piattaforma, e il nostro veniva ignorato in
                // silenzio. Il file SelectionToolbar.kt e' stato CANCELLATO
                // invece di lasciarlo li' inerte con un commento che diceva di
                // funzionare. Vedi NeReO_1.3.4_migrazione_toolchain.md.
                //
                // 1.5 / 158: il SelectionContainer sta dentro CorpoCitabile
                // (ui/CitaSelezione.kt), che aggiunge «Cita» al menu di
                // selezione. Compose 1.12 dice finalmente COSA e' selezionato:
                // la stessa API che a settembre non lo diceva.
                else -> CorpoCitabile(
                    repo = repo,
                    articolo = article,
                    corpo = body,
                    riconverti = { if (rot13View) rot13(it) else it },
                    // 1.5.4: «Cita e rispondi» apre la stessa scrittura del
                    // pulsante Rispondi, con la citazione scelta gia' dentro.
                    onCitaERispondi = { article?.let { nav.navigate(Routes.composeReply(it.id)) } },
                ) {
                    QuoteColoredBody(
                        if (rot13View) rot13(body.orEmpty()) else body.orEmpty(),
                        background = readBg,
                        baseTextColor = readFg,
                        searchQuery = if (rot13View) "" else searchQuery,
                        formato = formatoCorpo(article?.headersRaw),
                    )
                }
            }
        }
        if (AppSettings.gestureDebug && diagnostica.isNotEmpty()) {
            Text(
                diagnostica,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        }
    }

    if (showPlonkDialog) {
        // M1: la casella riparte SPUNTATA a ogni apertura del dialogo (chiave
        // su showPlonkDialog). E' il comportamento voluto in questa build — il
        // plonk esteso e' la norma, quello secco l'eccezione consapevole — ed
        // evita che una deselezione fatta una volta resti appiccicata a
        // insaputa di chi la usa il mese dopo.
        var plonkEsteso by remember(showPlonkDialog) { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showPlonkDialog = false },
            title = { Text(stringResource(R.string.art_plonk_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.art_plonk_body, article?.fromField ?: stringResource(R.string.art_this_author))
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = plonkEsteso, onCheckedChange = { plonkEsteso = it })
                        Text(
                            stringResource(R.string.art_plonk_followup),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    // Il testo cambia con la casella: si legge cosa succede
                    // PRIMA di premere, non dopo.
                    Text(
                        stringResource(
                            if (plonkEsteso) R.string.art_plonk_followup_on
                            else R.string.art_plonk_followup_off
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPlonkDialog = false
                    val esteso = plonkEsteso
                    scope.launch {
                        repo.plonkAuthor(currentId, esteso)
                        nav.indietroSicuro()
                    }
                }) { Text("*plonk*") }
            },
            dismissButton = {
                TextButton(onClick = { showPlonkDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showHeaders) {
        AlertDialog(
            onDismissRequest = { showHeaders = false },
            title = { Text(stringResource(R.string.art_headers_title)) },
            text = {
                val h = article?.headersRaw
                if (h.isNullOrBlank()) {
                    Text(
                        stringResource(R.string.art_headers_none),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    SelectionContainer {
                        Text(
                            nascondiFaceHeaders(h),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHeaders = false }) { Text(stringResource(R.string.common_close)) } },
        )
    }

    if (showTree) {
        AlertDialog(
            onDismissRequest = { showTree = false },
            title = { Text(stringResource(R.string.art_tree_title)) },
            text = {
                if (threadItems.isEmpty()) {
                    Text(stringResource(R.string.art_tree_none), style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(threadItems, key = { it.article.id }) { item ->
                            val a = item.article
                            val current = a.id == currentId
                            // 1.4.7 (§A15 build 2): drawBehind non è @Composable
                            // e non legge il tema — il colore si cattura qui.
                            val railColor = MaterialTheme.colorScheme.outlineVariant
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentId = a.id
                                        showTree = false
                                    }
                                    // 1.4.7 (§A15 build 2, Dave Royal): le rotaie
                                    // verticali arrivano anche nell'albero. PRIMA
                                    // del padding, così disegnano sull'intera
                                    // larghezza della riga come nelle altre due
                                    // viste.
                                    .drawBehind {
                                        disegnaRotaieAlbero(item.depth, passoRientro(), railColor)
                                    }
                                    .padding(
                                        // 1.4.7 (§A15 build 2): i 4.dp di scarto
                                        // NON c'erano qui, mentre telefono e
                                        // pannelli li hanno sempre avuti. Senza,
                                        // l'ultima rotaia cadrebbe sotto la prima
                                        // lettera del titolo. Aggiunti per dare
                                        // alle tre viste la stessa geometria: è
                                        // lo scopo di §A15.
                                        start = 4.dp + rientroAlbero(item.depth),
                                        top = 4.dp,
                                        bottom = 4.dp,
                                    ),
                            ) {
                                Text(
                                    (if (current) "▶ " else "") + a.subject,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (current || !a.read) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        current -> MaterialTheme.colorScheme.primary
                                        AppSettings.colorReadState -> readStateColor(a.read)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 2,
                                )
                                Text(
                                    "${a.fromField}  ·  ${formatThreadDate(a.dateEpoch)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTree = false }) { Text(stringResource(R.string.common_close)) } },
        )
    }
}

/**
 * 1.3 (B — barre personalizzabili). Un'azione della barra bassa di lettura
 * modellata come DATO: [id] stabile (usato per ordine/visibilità salvati in
 * AppSettings), non un composable cablato. Vedi NeReO_feature_barre.md.
 */
/**
 * 1.4.6 (§B4): icona ed etichetta NON stanno piu' qui. Erano dichiarate tre
 * volte — in questa lista, nella gemella dei pannelli, e nelle due tabelle
 * dell'editor — e tre copie della stessa verita' sono tre occasioni di
 * divergere. Ora vengono da CATALOGO_BARRE (AzioniBarra.kt) e qui resta solo
 * cio' che NON puo' essere condiviso: se l'azione e' disponibile adesso, e
 * cosa fa. Il comportamento e' per forza di questa schermata; l'aspetto no.
 */
private data class ArticleBarAction(
    val id: String,
    val enabled: Boolean = true,
    val tinted: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * beta14 (segnalato da un tester che non capiva l'icona "primo non letto"):
 * IconButton con tooltip al tocco prolungato. Rende scopribile il significato
 * delle icone della barra inferiore di lettura — che sono molte e non tutte
 * ovvie — riusando le stesse etichette già presenti come contentDescription
 * (finora visibili solo ai lettori di schermo). È lo standard Material 3 per
 * barre di sole icone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        // 1.3.6 (secondo giro, versionCode 89): la 1.3.5 aveva sostituito
        // rememberPlainTooltipPositionProvider() — deprecata — con
        // rememberTooltipPositionProvider(). Nome giusto, OVERLOAD SBAGLIATO:
        // la forma senza argomenti e' a sua volta deprecata, ed erano gli
        // ultimi due avvisi Kotlin rimasti nel progetto (un solo punto di
        // chiamata, due avvisi: la funzione e la risoluzione del suo argomento
        // predefinito).
        //
        // La forma viva chiede DOVE mettere il tooltip:
        //   rememberTooltipPositionProvider(
        //       positioning: TooltipAnchorPosition,
        //       spacingBetweenTooltipAndAnchor: Dp = SpacingBetweenTooltipAndAnchor)
        // e la deprecazione stessa indica TooltipAnchorPosition.Above come
        // equivalente esatto del comportamento precedente: sopra l'icona, che
        // per una barra in fondo allo schermo e' anche l'unica posizione
        // sensata. Nessun cambio di aspetto, nessun cambio di spaziatura (il
        // secondo parametro resta al suo valore predefinito).
        //
        // TooltipAnchorPosition e' una value class con le costanti nel
        // companion (Above, Below, Left, Right, Start, End) — CamelCase, non
        // MAIUSCOLO.
        //
        // SE NON COMPILA: rimettere la riga commentata qui sotto e togliere
        // questa. La vecchia funziona ancora — e' deprecata, non rimossa, e un
        // avviso non vale un rilascio bloccato.
        //     positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(enabled = enabled, onClick = onClick, modifier = modifier, content = content)
    }
}
