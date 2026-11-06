/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Lista messaggi di un gruppo. Tre viste (albero / sequenziale / solo
 * discussioni) con separazione grafica tra i thread, filtro non-letti,
 * watched in testa, sync manuale e conservazione (retention) per gruppo.
 */
package org.openusenet.reader.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import org.openusenet.reader.ui.icone.DeleteSweep
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import org.openusenet.reader.ui.icone.Block
import org.openusenet.reader.ui.icone.VisibilityOff
import org.openusenet.reader.ui.icone.Cancel
import org.openusenet.reader.ui.icone.CheckCircle
import org.openusenet.reader.ui.icone.Checklist
import org.openusenet.reader.ui.icone.Close
import org.openusenet.reader.ui.icone.Delete
import org.openusenet.reader.ui.icone.RadioButtonUnchecked
import org.openusenet.reader.ui.icone.RestoreFromTrash
import org.openusenet.reader.ui.icone.SelectAll
import org.openusenet.reader.ui.icone.OpenInNew
import org.openusenet.reader.ui.icone.Undo
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.ArrowBack
import org.openusenet.reader.ui.icone.Add
import org.openusenet.reader.ui.icone.DoneAll
import org.openusenet.reader.ui.icone.Download
import org.openusenet.reader.ui.icone.Drafts
import org.openusenet.reader.ui.icone.Edit
import org.openusenet.reader.ui.icone.Markunread
import org.openusenet.reader.ui.icone.MoreVert
import org.openusenet.reader.ui.icone.Remove
import org.openusenet.reader.ui.icone.Search
import org.openusenet.reader.ui.icone.Person
import org.openusenet.reader.ui.icone.Star
import org.openusenet.reader.ui.icone.StarBorder
import org.openusenet.reader.ui.icone.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
import org.openusenet.reader.data.Article
import org.openusenet.reader.data.Identity
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.ui.Routes
import org.openusenet.reader.ui.indietroSicuro
import org.openusenet.reader.ui.playDing
import org.openusenet.reader.ui.components.readStateColor

// wishlist beta11: non più private, riusata anche da ThreePaneHome
@Composable
fun viewLabel(v: String) = when (v) {
    ThreadViews.SEQUENTIAL -> stringResource(R.string.tl_view_sequential)
    ThreadViews.ROOTS -> stringResource(R.string.tl_view_grouped_threads)
    else -> stringResource(R.string.tl_view_tree)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadListScreen(
    repo: NewsRepository,
    nav: NavHostController,
    groupId: Long,
    /**
     * 1.4.6 (par. B13, BIG Umberto): il messaggio su cui posizionarsi entrando,
     * oppure `null` per l'ingresso normale. Arriva dalla ricerca attraverso
     * l'azione «mostra nell'elenco».
     */
    evidenziaArticolo: Long? = null,
    /**
     * 1.5.9 / 177 (vort3): id della voce di navigazione, chiave di
     * [DiscussioniAperte]. Uguale al ritorno dall'articolo, nuova a ogni
     * ingresso nel gruppo.
     */
    chiaveVoce: String = "",
    /** 1.5.9 / 177 (vort3): l'ultimo articolo letto partendo da qui, o null. */
    ultimoLetto: Long? = null,
) {
    // 1.4.7 (§A16) — UNA SOLA FONTE, e il perche' viene da Jeff.
    //
    // Fino alla 1.4.6 qui c'era `var unreadOnly by remember { ... }`: una COPIA
    // locale che si scriveva da sola e in piu' scriveva AppSettings. Finche'
    // l'unico modo di cambiare il valore era l'icona in questa stessa barra, le
    // due copie non potevano divergere. Dalla 1.4.7 il valore si cambia anche da
    // Impostazioni → Aspetto, e una copia catturata all'ingresso nella schermata
    // avrebbe continuato a mostrare lo stato vecchio.
    //
    // `AppSettings.unreadOnly` e' `by mutableStateOf`: leggerlo qui iscrive la
    // composizione ai suoi cambiamenti, quindi non serve nessuno stato locale e
    // i punti di lettura qui sotto restano identici.
    val unreadOnly = AppSettings.unreadOnly
    // mostra solo le discussioni con almeno un messaggio seguito (stellato)
    var watchedOnly by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf(AppSettings.threadView) }
    var overflow by remember { mutableStateOf(false) }
    var retentionDialog by remember { mutableStateOf(false) }
    var groupPurge by remember { mutableStateOf<Int?>(null) }
    // beta19 (Roxy): conservazione a numero di discussioni (ha precedenza sui giorni)
    var groupKeepThreads by remember { mutableStateOf<Int?>(null) }
    // 1.4 (Henry The Mole): introduzione alla citazione di QUESTO gruppo.
    var groupReplyIntro by remember { mutableStateOf<String?>(null) }
    // 1.6 (Dave Royal): identità di QUESTO gruppo, e quelle fra cui scegliere.
    var groupIdentityId by remember { mutableStateOf<Long?>(null) }
    // 1.6: la discussione da ricostruire, se il dialogo e' aperto.
    var ricostruisciMembri by remember { mutableStateOf<List<Article>?>(null) }
    val identita by repo.identities.collectAsState(initial = emptyList())
    var contextItem by remember { mutableStateOf<ThreadItem?>(null) }
    // Modalità selezione multipla: attiva, il tocco seleziona invece di aprire.
    // Serve a cancellare più copie locali in un colpo (richiesta dei tester).
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    // Messaggi contrassegnati per la cancellazione: restano visibili barrati
    // finché si è nel gruppo, poi vengono eliminati davvero (vedi leaveGroup).
    var marcatiVersione by remember(groupId) { mutableStateOf(0) }
    val marcati = remember(groupId, marcatiVersione) { PendingDeletions.of(groupId) }
    var reloadDialog by remember { mutableStateOf(false) }
    var bodiesDialog by remember { mutableStateOf(false) }
    // 1.2.11: messaggio (proprio) per cui si sta chiedendo conferma del ritiro
    // dal server con un messaggio di controllo "cancel". null = nessun dialog.
    var cancelTarget by remember { mutableStateOf<Article?>(null) }

    // 1.4.4: l'esito per server e l'attesa NON stanno piu' qui — sono
    // meccanica del ritiro, non di questa schermata, e vivono dentro
    // DialoghiRitiro insieme ai dialoghi che le mostrano.
    // discussioni aperte con il +, per messageId della radice. Vive finche' si
    // resta nel gruppo: uscendo si riparte tutto richiuso, che e' il colpo
    // d'occhio che questa vista deve dare.
    // 1.5.9 / 177 (vort3): sopravvive all'andata e ritorno nell'articolo, vedi
    // DiscussioniAperte. Letta QUI, prima della prima lista: e' quella su cui
    // il ripristino della posizione cerca la sua ancora.
    var expandedRoots by remember { mutableStateOf(DiscussioniAperte.get(chiaveVoce)) }
    LaunchedEffect(expandedRoots) { DiscussioniAperte.put(chiaveVoce, expandedRoots) }

    // Posizione di lettura ricordata per gruppo (vedi ThreadScrollMemory).
    // Lo stato parte da zero: il ripristino avviene più sotto, quando la lista
    // è davvero popolata — prima non c'è niente su cui posizionarsi.
    val listState = rememberLazyListState()
    // true quando il ripristino per questo gruppo è già stato fatto: senza
    // questo, ogni aggiornamento della lista (un messaggio segnato letto, una
    // sincronizzazione) rimanderebbe l'utente indietro mentre sta scorrendo.
    var posizioneRipristinata by remember(groupId) { mutableStateOf(false) }
    /*
     * 1.5.4 — I NUOVI ARRIVANO DOPO IL RIPRISTINO (segnalazione di un utente,
     * via Marco, 15 ottobre 2026: «all'apertura del gruppo, se ci sono nuovi
     * messaggi, la vista non scorre verso l'alto ma rimane bloccata sul
     * precedente... e ripartiva da quel punto anche chiudendo e riaprendo»).
     *
     * Il ripristino della beta14 decide «ci sono novita'?» alla PRIMA lista non
     * vuota — ma all'apertura parte anche il sync del gruppo, e quella prima
     * lista e' la VECCHIA: niente novita', si torna giu'. I nuovi arrivano in
     * cima un attimo dopo, a decisione presa. Intanto il salvataggio continuo
     * registra la nuova cima, e al rientro successivo il confronto dice di nuovo
     * «niente di nuovo»: lo stesso punto, ogni volta.
     *
     * Piano A (Marco): se, DOPO aver riportato l'utente dov'era, la cima della
     * lista cambia e lui non ha ancora toccato la lista, si va in cima.
     *
     * 1.5.5 / 173 (Marcoseba: «a me capita ancora»): la sorveglianza si
     * accendeva solo dopo un ripristino, e «novita'» voleva dire solo «il primo
     * e' cambiato». Ora vale in ogni ingresso, e il segnale tiene conto
     * dell'ordinamento: vedi SorveglianzaNovita.kt e NovitaInCima.kt.
     */

    val articles by repo.articles(groupId, AppSettings.mostraFiltrati)
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var syncing by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    // 1.4.4 (§A9, Roxy + Marco): DA QUALE SERVER stai leggendo. Con due gruppi
    // omonimi su server diversi il solo nome del gruppo non lo dice, e una
    // volta entrati non lo diceva niente — ne' qui ne' nella vista a pannelli.
    var groupServerId by remember { mutableStateOf(0L) }
    val servers by repo.servers.collectAsState(initial = emptyList())
    // Con UN SOLO server l'etichetta resta vuota e la barra non cambia: non
    // c'e' niente da distinguere, e una riga in piu' costerebbe altezza utile
    // senza dire nulla. E' lo stesso criterio del menu "aggiorna" di §A7.
    // 1.6 / 180: la regola sta in serverDaMostrare (IntestazioneServer.kt).
    val serverEtichetta = remember(groupServerId, servers) { serverDaMostrare(servers, groupServerId) }

    LaunchedEffect(groupId) {
        val g = repo.groupById(groupId)
        groupName = g?.name ?: ""
        groupServerId = g?.serverId ?: 0L
        groupPurge = g?.purgeDays
        groupKeepThreads = g?.purgeKeepThreads
        groupReplyIntro = g?.replyIntro
        groupIdentityId = g?.identityId
        syncing = true
        // beta20 (Roxy): RIPRISTINATO il sync all'apertura del gruppo (vedi
        // SyncGuard in ThreadFlatten.kt per la spiegazione completa dell'errore
        // di beta19). La guardia evita solo il ri-sync ravvicinato sullo stesso
        // gruppo, tipico del rientro dalla lettura di una discussione.
        //
        // 1.2.10 (voce Y, Big Umberto): aggiunta la guardia esplicita
        // AppSettings.syncOnGroupOpen — prima questo sync era incondizionato,
        // senza alcun interruttore dedicato (vedi NeReO_bug_1.2.9.md). Chi lo
        // disattiva mantiene comunque il sync manuale (pulsante/pull-to-refresh
        // più sotto in questo file), mai gated da questa impostazione.
        if (AppSettings.syncOnGroupOpen && SyncGuard.deveSincronizzare(groupId)) {
            runCatching { repo.syncGroup(groupId) }
                .onFailure { snackbar.showSnackbar(ctx.getString(R.string.tl_sync_failed, it.message)) }
        }
        // wishlist beta11 (Roxy): scaricamento automatico dei corpi, dietro
        // impostazione esplicita — vedi Manutenzione e backup. Silenzioso di
        // proposito: è un extra sul sync, non deve interrompere la lettura
        // con un altro snackbar se fallisce.
        if (AppSettings.scaricaCorpiAutomatico) {
            runCatching { repo.downloadAllBodies(groupId, limit = AppSettings.maxCorpiPerApertura) }
        }
        syncing = false
    }

    // Filtro "solo seguite": limito gli articoli ai thread che contengono almeno
    // un messaggio watched. Il legame di thread è dato dalle References, quindi
    // raccolgo i messageId dei watched e dei loro discendenti seguendo il padre.
    val articoliVisibili = remember(articles, watchedOnly) {
        if (!watchedOnly) articles
        else {
            val watchedMsgIds = articles.filter { it.watched }.map { it.messageId }.toSet()
            if (watchedMsgIds.isEmpty()) emptyList()
            else {
                // un articolo appartiene a un thread seguito se lui o un suo
                // antenato (via References) è watched
                fun inThreadSeguito(a: Article): Boolean {
                    if (a.messageId in watchedMsgIds) return true
                    return Regex("<[^>]+>").findAll(a.references).any { it.value in watchedMsgIds }
                }
                articles.filter { inThreadSeguito(it) }
            }
        }
    }

    val items = remember(articoliVisibili, unreadOnly, view, AppSettings.threadSort, expandedRoots) {
        buildThreadList(articoliVisibili, unreadOnly, view, expandedRoots = expandedRoots)
    }
    // 1.5.5 / 173: primo della lista e id piu' alto, per riconoscere le novita'.
    val impronta = remember(items, articoliVisibili) {
        improntaLista(items.firstOrNull()?.article?.id, articoliVisibili.map { it.id })
    }

    // Ripristino (beta14 best-of-both): appena la lista ha contenuto, decido.
    // Se il primo articolo della lista è cambiato rispetto a quando sono
    // uscito, significa che sono arrivati articoli NUOVI in cima: resto in
    // cima a mostrarli. Altrimenti torno esattamente dove avevo lasciato.
    // Una volta sola per ingresso nel gruppo.
    LaunchedEffect(items, groupId) {
        if (posizioneRipristinata || items.isEmpty()) return@LaunchedEffect
        /*
         * 1.4.6 (par. B13): con un messaggio da evidenziare questo ripristino
         * NON si fa. L'ancora significa «dov'ero rimasto»; l'evidenziazione
         * significa «portami qui», e sono due intenzioni diverse sullo stesso
         * scorrimento. Lasciarle correre insieme vuol dire due salti in sequenza
         * e l'ultimo che vince a caso.
         *
         * ⚠️ E' il motivo per cui l'ancora NON e' stata riusata per questo: ha
         * anche una guardia che la annulla quando sono arrivati messaggi nuovi
         * in cima — giusta per riprendere, sbagliata per un comando esplicito,
         * che deve funzionare sempre.
         */
        if (evidenziaArticolo != null) { posizioneRipristinata = true; return@LaunchedEffect }
        val improntaOra = impronta
        ThreadScrollMemory.get(groupId)?.let { ancora ->
            val ciSonoNovita = improntaOra != null && novitaInCima(
                view == ThreadViews.SEQUENTIAL, AppSettings.threadSort, ancora.impronta, improntaOra,
            )
            if (!ciSonoNovita) {
                // nessun articolo nuovo in cima → riprendi da dove eri
                // se il messaggio non c'è più (cancellato, filtrato, scaduto) si
                // resta in cima: meglio un punto noto che uno arbitrario. Idem
                // se era la prima riga ed è sceso (1.6.1: seguita appena letta).
                indiceDiRipristino(ancora, items.map { it.article.id })?.let {
                    listState.scrollToItem(it, ancora.offset)
                }
            }
            // se ci sono novità non faccio nulla: la lista è già in cima (0,0),
            // dove i nuovi articoli sono visibili
        }
        posizioneRipristinata = true
    }

    // 1.5.5 / 173: da qui, in OGNI caso (ripristino o partenza dall'alto), le
    // novita' che arrivano col sync dell'apertura portano in cima finche'
    // l'utente non trascina la lista. Non con un messaggio da evidenziare:
    // quello e' un «portami qui» esplicito (par. B13) e non va scavalcato.
    InCimaSeArrivanoNovita(
        listState = listState,
        chiave = groupId,
        attiva = posizioneRipristinata && evidenziaArticolo == null,
        impronta = impronta,
        vistaSequenziale = view == ThreadViews.SEQUENTIAL,
        ordinamento = AppSettings.threadSort,
    )

    /*
     * 1.4.6 (par. B13, BIG Umberto) — «MOSTRA NELL'ELENCO».
     *
     * *«La ricerca funziona, ma ti apre il post, non ti fa vedere dove e'.»*
     * Da un risultato si finiva dritti nel messaggio, senza alcun modo di
     * sapere in quale discussione stesse ne' chi gli stava intorno.
     *
     * DUE PASSATE, e servono tutte e due. Alla prima il messaggio spesso NON
     * e' una riga dell'elenco: la sua discussione e' chiusa, e una discussione
     * chiusa e' una riga sola. Allora si cerca in quale discussione sta —
     * guardando l'elenco completamente aperto, l'unico in cui ogni messaggio
     * compare di sicuro — la si apre, e al giro dopo la riga c'e' e ci si
     * scorre sopra.
     *
     * ⚠️ [discussioneAperta] non e' una comodita': e' la GUARDIA che impedisce
     * il ciclo. Se il messaggio non si trova nemmeno nell'elenco aperto
     * (cancellato, filtrato, scaduto) senza di lei si riproverebbe a ogni
     * ricomposizione, per sempre.
     *
     * [giaScorso] serve a scorrere UNA volta sola: l'evidenziazione resta
     * accesa finche' l'utente non tocca qualcosa, ma riportarlo li' a ogni
     * aggiornamento della lista vorrebbe dire inchiodarcelo.
     *
     * ⚠️⚠️ CORREZIONE 1.4.8 (par. U1, BIG Umberto, 6 ottobre) — LA GUARDIA
     * SCATTAVA ANCHE SUL CASO «NON C'E' ANCORA».
     *
     * Segnalazione: nella vista raggruppata il bersaglio evidenzia il
     * messaggio solo se e' il PRIMO POST di una discussione; albero e
     * sequenziale a posto.
     *
     * Gli articoli arrivano da un Flow con `collectAsState(initial =
     * emptyList())`, quindi alla PRIMA composizione la lista e' vuota.
     * Questo effetto partiva lo stesso, non trovava l'id — ovvio, non c'era
     * niente — bruciava [discussioneAperta], cercava la discussione in un
     * elenco VUOTO (`discussioneDi` torna null) e non espandeva nulla.
     * All'arrivo dei dati l'effetto rigirava, l'id ancora non c'era perche'
     * la discussione era chiusa, e incontrava `if (discussioneAperta)
     * return`: rinuncia definitiva.
     *
     * PERCHE' SI VEDEVA IN UNA VISTA SOLA. Nella raggruppata una discussione
     * chiusa e' UNA riga: un messaggio che non sia la radice ha bisogno della
     * seconda passata, cioe' proprio di quella che la guardia uccideva. Una
     * RADICE, invece, e' gia' una riga appena i dati arrivano, e la seconda
     * passata non le serve mai — come non serve mai ad albero e sequenziale,
     * dove ogni messaggio e' gia' una riga. Non era la vista a essere
     * speciale: era l'unica in cui il difetto poteva manifestarsi.
     *
     * *Una guardia che protegge dal caso «non c'e'» scatta anche sul caso
     * «non c'e' ANCORA», se non si distingue la lista vuota dalla ricerca
     * fallita.* La protezione resta intera: a dati presenti si tenta sempre
     * una volta sola.
     */
    var daEvidenziare by remember(evidenziaArticolo) { mutableStateOf(evidenziaArticolo) }
    var discussioneAperta by remember(evidenziaArticolo) { mutableStateOf(false) }
    var giaScorso by remember(evidenziaArticolo) { mutableStateOf(false) }
    // Le chiavi restano le tre di prima: `items` deriva da `articoliVisibili`,
    // quindi all'arrivo dei dati cambia da se' e l'effetto rigira. Aggiungerne
    // una quarta avrebbe voluto dire solo piu' riavvii per lo stesso risultato.
    LaunchedEffect(items, daEvidenziare, discussioneAperta) {
        val id = daEvidenziare ?: return@LaunchedEffect
        val idx = items.indexOfFirst { it.article.id == id }
        if (idx >= 0) {
            if (!giaScorso) { listState.scrollToItem(idx); giaScorso = true }
            return@LaunchedEffect
        }
        // 1.4.8 (par. U1): senza articoli non c'e' NIENTE da cercare, e
        // tentare qui vorrebbe dire soltanto consumare la guardia prima che
        // i dati esistano. Vedi la nota qui sopra.
        if (articoliVisibili.isEmpty()) return@LaunchedEffect
        if (discussioneAperta) return@LaunchedEffect
        discussioneAperta = true
        val completo = buildThreadList(
            articoliVisibili, unreadOnly, view,
            expandedRoots = allRootKeys(articoliVisibili, unreadOnly),
        )
        discussioneDi(completo, id)?.let { expandedRoots = expandedRoots + it }
    }

    // Salvataggio: registra QUALE messaggio è in cima, non quale riga. Se la
    // lista è vuota getOrNull dà null e non si salva nulla — è la condizione
    // che impediva alla memoria di azzerarsi da sola. Registra anche l'id del
    // PRIMO articolo della lista (beta14): serve al rientro per capire se sono
    // arrivati articoli nuovi in cima.
    LaunchedEffect(listState, groupId, items, impronta) {
        val improntaSalvata = impronta ?: return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (idx, off) ->
                items.getOrNull(idx)?.article?.id?.let { id ->
                    ThreadScrollMemory.put(groupId, id, off, improntaSalvata, inCima = idx == 0)
                }
            }
    }

    // 1.3.5 — UNICA FONTE DI VERITA' sull'appartenenza a una discussione.
    // Calcolata dal threader sull'elenco COMPLETO (non su quello filtrato) e
    // memorizzata: cambia solo quando cambia la lista degli articoli.
    // Vedi la nota lunga in ThreadFlatten.kt per i due difetti che questa
    // mappa chiude.
    val mappaThread = remember(articles) { mappaDiscussioni(articles) }

    // 1.5.9 / 177 (vort3): la riga in tinta tenue. Non fa scorrere niente: la
    // posizione resta quella ripristinata, e la regola «novita' in cima» non
    // ne sa nulla.
    val rigaLetta = remember(items, articles, mappaThread, ultimoLetto) {
        rigaUltimoLetto(items, articles, mappaThread, ultimoLetto)
    }

    /**
     * Tutti gli id della discussione che contiene [articleId].
     *
     * 1.3.5: passa da [mappaThread], cioe' dal THREADER, invece di confrontare
     * stringhe di References. Prima bastava un messaggio con le References
     * ridotte al solo padre diretto perche' restasse fuori dalla selezione:
     * "marca la discussione letta" ne lasciava indietro uno e la discussione
     * restava visibile. Vedi la nota lunga in ThreadFlatten.kt.
     */
    fun threadIdsOf(articleId: Long): List<Long> {
        val a = articles.firstOrNull { it.id == articleId } ?: return listOf(articleId)
        val ids = membriDelThread(articles, mappaThread, a).map { it.id }
        return ids.ifEmpty { listOf(articleId) }
    }

    /**
     * beta20 (Big Umberto): espande la selezione multipla a TUTTI i messaggi
     * delle discussioni selezionate.
     *
     * Nella vista raggruppata una riga selezionata rappresenta un'intera
     * discussione: le azioni "segna letto/non letto" devono valere su tutti i
     * suoi messaggi, non solo sulla riga di testa. Prima si marcava il solo id
     * selezionato, quindi una discussione da 3 messaggi restava con 2 non letti
     * (e ripetendo l'operazione i contatori scendevano di uno per volta).
     *
     * Per le righe che NON sono radici (viste albero/sequenziale) resta valido
     * il singolo messaggio: `threadIdsOf` viene usata solo sulle radici.
     */
    fun idsDelleDiscussioniSelezionate(): List<Long> {
        val perRiga = items.associate { it.article.id to it.isRoot }
        return selectedIds.flatMap { id ->
            if (perRiga[id] == true) threadIdsOf(id) else listOf(id)
        }.distinct()
    }

    // uscita dal gruppo: se l'opzione è attiva, segna tutto letto PRIMA di uscire
    fun leaveGroup() {
        // I messaggi contrassegnati per la cancellazione vengono eliminati ORA:
        // finché si resta nel gruppo restano barrati e ripristinabili, uscendo
        // la scelta diventa definitiva (rientrando non compaiono più).
        // 1.6: il COSA sta in applicaUscitaGruppo, che lo usa anche il tasto Avanti.
        // 1.5.9 / 177: uscendo dal gruppo si riparte tutto chiuso.
        DiscussioniAperte.clear(chiaveVoce)
        if (AppSettings.markReadOnExit || PendingDeletions.of(groupId).isNotEmpty()) {
            scope.launch {
                applicaUscitaGruppo(repo, groupId)
                nav.indietroSicuro()
            }
        } else {
            nav.indietroSicuro()
        }
    }
    // In selezione il tasto Indietro annulla la selezione invece di uscire dal
    // gruppo: uscire perdendo quanto selezionato sarebbe una sorpresa sgradita.
    BackHandler {
        if (selectionMode) {
            selectionMode = false
            selectedIds = emptySet()
        } else leaveGroup()
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                // Barra della modalità selezione: sostituisce quella normale per
                // rendere evidente che i tocchi ora selezionano, e per tenere le
                // azioni distruttive lontane dai comandi di lettura.
                TopAppBar(
                    title = { Text(stringResource(R.string.tl_selected_count, selectedIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = { selectionMode = false; selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.tl_exit_selection))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            // seleziona/deseleziona tutto quello che è a schermo
                            val tutti = items.map { it.article.id }.toSet()
                            selectedIds = if (selectedIds.containsAll(tutti)) emptySet() else tutti
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.tl_select_all))
                        }
                        // beta15 (Big Umberto): segna i SELEZIONATI letti / non
                        // letti. setReadBulk esiste già ed è usato altrove nel
                        // file (stesso pattern). Azioni non distruttive: stanno
                        // prima di elimina/ripristina.
                        //
                        // beta20 (Big Umberto): ESPANDO la selezione a tutta la
                        // discussione. Nella vista raggruppata una riga
                        // rappresenta un'INTERA discussione, non un singolo
                        // messaggio: marcando solo gli id selezionati veniva
                        // segnato letto il solo primo post, e la discussione
                        // restava colorata con i contatori che scendevano di uno
                        // a ogni ripetizione (3/3 → 2/3 → …), il bug segnalato.
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                val ids = idsDelleDiscussioniSelezionate()
                                scope.launch { repo.setReadBulk(ids, true, groupId) }
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = stringResource(R.string.tl_mark_selected_read))
                        }
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                // beta20 (Big Umberto): come sopra, l'azione vale
                                // sull'intera discussione selezionata
                                val ids = idsDelleDiscussioniSelezionate()
                                scope.launch { repo.setReadBulk(ids, false, groupId) }
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                        ) {
                            Icon(Icons.Default.Markunread, contentDescription = stringResource(R.string.tl_mark_selected_unread))
                        }
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                // contrassegna: restano barrati e ripristinabili
                                // finché non si esce dal gruppo
                                PendingDeletions.mark(groupId, selectedIds)
                                marcatiVersione++
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.tl_mark_deletion))
                        }
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                PendingDeletions.unmark(groupId, selectedIds)
                                marcatiVersione++
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.tl_restore_selected))
                        }
                    },
                )
            } else
            Column {
            // wishlist beta11 (richiesto da Marco): intestazione a due righe.
            // Prima nome gruppo e icone condividevano un'unica riga e un nome
            // lungo veniva tagliato — vedi screenshot "it-alt.sport.c...".
            // Riga 1: solo freccia indietro + nome completo, fino a 2 righe.
            // Riga 2: le stesse azioni di prima, ora con tutto lo spazio loro.
            TopAppBar(
                title = {
                        // 1.6 / 180: nome e server passano da TitoloGruppo
                        // (IntestazioneServer.kt), lo stesso della colonna 2 e
                        // della lettura. Le note qui sotto restano valide.
                        //
                        // 1.4.7 (§A11-bis, Dave Royal) — ANCHE QUI.
                        //
                        // La richiesta era: «the group name should preferably be
                        // abbreviated in the same way as in column 1 then truncated
                        // to fit without line-wrap». Nella 1.4.5 l'avevamo
                        // applicata ai due punti della vista a PANNELLI e
                        // `NomeGruppoAdattivo` e' nata li'. Questo titolo — che sul
                        // telefono fa esattamente il mestiere della barra di
                        // colonna 2 — era rimasto col troncamento secco.
                        //
                        // *Quarta volta della stessa forma in questa release: la
                        // logica esisteva gia' ed era usata in due punti su cinque.*
                        //
                        // ⚠️ `righe = 1`, E NON E' UN DETTAGLIO — errore della 140.
                        // Li' avevo messo 2 «per non cambiare il comportamento
                        // esistente», ma il comportamento esistente era proprio
                        // quello da togliere: la richiesta dice «truncated to fit
                        // WITHOUT LINE-WRAP». `NomeGruppoAdattivo` abbrevia solo
                        // quando il testo trabocca oltre `righe`: con due righe
                        // concesse `alt.comp.software.thunderbird` ci sta, non
                        // trabocca, e l'abbreviazione non scatta mai.
                        //
                        // *Una cautela che conserva proprio cio' che si era chiesto
                        // di cambiare non e' prudenza: e' la modifica non fatta.*
                        //
                        // 1.4.4 (§A9): il server sotto al nome, e solo quando i
                        // server sono piu' di uno. Sottotitolo e non seconda
                        // riga del titolo: e' un'informazione di contesto, non
                        // il nome di cio' che stai guardando.
                        TitoloGruppo(
                            nome = groupName.ifEmpty { stringResource(R.string.tl_default_title) },
                            server = serverEtichetta,
                        )
                },
                navigationIcon = {
                    IconButton(onClick = { leaveGroup() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(end = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                    IconButton(onClick = {
                        // §A16: si scrive la preferenza, non una copia locale.
                        AppSettings.unreadOnly = !AppSettings.unreadOnly
                        AppSettings.save()
                    }) {
                        Icon(
                            if (unreadOnly) Icons.Default.Markunread else Icons.Default.Drafts,
                            contentDescription = if (unreadOnly) stringResource(R.string.common_showing_unread) else stringResource(R.string.tl_showing_all),
                            tint = if (unreadOnly) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        enabled = !syncing,
                        onClick = {
                            scope.launch {
                                syncing = true
                                runCatching { repo.syncGroup(groupId) }
                                    .onSuccess { n ->
                                        snackbar.showSnackbar(testoEsitoSync(ctx, n, repo.lastSyncFiltrati))
                                    }
                                    .onFailure { snackbar.showSnackbar(ctx.getString(R.string.msg_error, it.message)) }
                                syncing = false
                            }
                        },
                    ) { Icon(Icons.Default.Sync, contentDescription = stringResource(R.string.tl_sync)) }
                    // 1.4.1 (Dave 08) — ORDINE SCAMBIATO, e non e' estetica.
                    // "It's very easy to accidentally mark the group read,
                    // especially because it's next to the envelope icon which I
                    // use a lot": un'azione RARA e con conseguenze appoggiata a
                    // una FREQUENTE e innocua (il filtro non-letti). Spostarla
                    // nel menu pero' costerebbe un tocco al giorno a chi la usa
                    // ogni sera. Scambiandola con Aggiorna nessuno perde tocchi
                    // e i due pulsanti che si confondono non si toccano piu'.
                    // 1.4.5 (§A11): l'azione e lo snackbar con ANNULLA sono usciti
                    // di qui e stanno in ui/screens/AzioneSegnaTuttoLetto.kt, perche'
                    // ora li chiama anche la barra dei pannelli. Erano ricopiati
                    // parola per parola in due punti; il terzo non si fa.
                    AzioneSegnaTuttoLetto(
                        repo = repo,
                        groupId = groupId,
                        scope = scope,
                        snackbar = snackbar,
                    )
                    // 1.4.4 (Dave Royal): il Box E' L'ANCORA. Senza, DropdownMenu si
                    // aggancia al nodo GENITORE (Popup usa parentLayoutCoordinates),
                    // e in una Row a tutta larghezza il menu parte dal bordo dello
                    // schermo invece che da sotto l'icona.
                    Box {
                        IconButton(onClick = { overflow = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.art_more_cd))
                        }
                        DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                            // scese qui dalla barra: azioni occasionali, e con
                            // l'etichetta scritta si capiscono senza indovinare l'icona
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.tl_search_group)) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = {
                                    overflow = false
                                    nav.navigate(Routes.searchGroup(groupId, groupName))
                                },
                            )
                            // 1.2.9 (richiesta di Marco, Piano C): "Bozze" spostata
                            // qui, dentro il gruppo a cui si scrive — non più nel
                            // menu principale. Parte filtrata su questo gruppo, con
                            // un interruttore dentro lo schermo per vedere tutte.
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.drafts_title)) },
                                leadingIcon = { Icon(Icons.Default.Drafts, contentDescription = null) },
                                onClick = {
                                    overflow = false
                                    nav.navigate(Routes.draftsForGroup(groupId))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_watched_threads_only)) },
                                leadingIcon = {
                                    Icon(
                                        if (watchedOnly) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (watchedOnly) MaterialTheme.colorScheme.tertiary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                trailingIcon = { Checkbox(checked = watchedOnly, onCheckedChange = null) },
                                onClick = { watchedOnly = !watchedOnly; overflow = false },
                            )
                            HorizontalDivider()
                            Text(
                                stringResource(R.string.common_view),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                            listOf(ThreadViews.TREE, ThreadViews.SEQUENTIAL, ThreadViews.ROOTS).forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(viewLabel(v)) },
                                    leadingIcon = { RadioButton(selected = view == v, onClick = null) },
                                    onClick = {
                                        view = v
                                        AppSettings.threadView = v
                                        AppSettings.save()
                                        overflow = false
                                    },
                                )
                            }
                            HorizontalDivider()
                            Text(
                                stringResource(R.string.common_thread_sort),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                            AppSettings.threadSorts.forEach { (chiave, etichetta) ->
                                DropdownMenuItem(
                                    // RC01: etichetta ordinamento tradotta alla visualizzazione
                                    text = { Text(when (chiave) {
                                        "activity_desc" -> stringResource(R.string.sort_activity_desc)
                                        "activity_asc" -> stringResource(R.string.sort_activity_asc)
                                        "opened_desc" -> stringResource(R.string.sort_opened_desc)
                                        "opened_asc" -> stringResource(R.string.sort_opened_asc)
                                        else -> etichetta
                                    }) },
                                    leadingIcon = {
                                        RadioButton(selected = AppSettings.threadSort == chiave, onClick = null)
                                    },
                                    onClick = {
                                        AppSettings.threadSort = chiave
                                        AppSettings.save()
                                        overflow = false
                                    },
                                )
                            }
                            if (view == ThreadViews.ROOTS) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.common_expand_all_threads)) },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    onClick = {
                                        expandedRoots = allRootKeys(articles, unreadOnly)
                                        overflow = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.common_collapse_all_threads)) },
                                    leadingIcon = { Icon(Icons.Default.Remove, contentDescription = null) },
                                    onClick = { expandedRoots = emptySet(); overflow = false },
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_mark_all_unread)) },
                                leadingIcon = { Icon(Icons.Default.Markunread, contentDescription = null) },
                                onClick = {
                                    scope.launch { repo.markAllUnread(groupId) }
                                    overflow = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_redownload_group_menu)) },
                                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                                onClick = { reloadDialog = true; overflow = false },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_download_all_bodies_menu)) },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = { overflow = false; bodiesDialog = true },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (AppSettings.mostraFiltrati) stringResource(R.string.common_hide_filtered) else stringResource(R.string.common_show_filtered)) },
                                onClick = { AppSettings.mostraFiltrati = !AppSettings.mostraFiltrati; overflow = false },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_group_retention_menu)) },
                                onClick = { retentionDialog = true; overflow = false },
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            // beta16 (Roxy): FAB dentro l'area sicura in landscape (vedi
            // ServersScreen per la causa edge-to-edge).
            FloatingActionButton(
                onClick = { nav.navigate(Routes.composeNew(groupId)) },
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cmp_new_thread))
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
            PullToRefreshBox(
                isRefreshing = syncing,
                onRefresh = {
                    scope.launch {
                        syncing = true
                        runCatching { repo.syncGroup(groupId) }
                            .onSuccess { n ->
                                if (AppSettings.dingOnReply && repo.lastSyncNewToMe > 0) playDing(ctx)
                                snackbar.showSnackbar(testoEsitoSync(ctx, n, repo.lastSyncFiltrati))
                            }
                            .onFailure { snackbar.showSnackbar(ctx.getString(R.string.msg_error, it.message)) }
                        syncing = false
                    }
                },
            ) {
                LazyColumn(state = listState) {
                itemsIndexed(items, key = { _, it -> it.article.id }) { idx, item ->
                    if (item.threadStart && idx != 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    // 1.2.9 (Piano B, Roxy): stesso lambda usato sia dal +/-
                    // sia, quando l'opzione è attiva, dal tocco sul testo
                    // dell'OP — un solo posto dove vive la logica di
                    // espandi/comprimi, niente duplicazione fra i due punti
                    // che la scatenano.
                    val toggleAction: (() -> Unit)? = if (item.isRoot && item.total > 1) {
                        {
                            expandedRoots = if (item.rootKey in expandedRoots)
                                expandedRoots - item.rootKey else expandedRoots + item.rootKey
                        }
                    } else null
                    ThreadRow(
                        item = item,
                        evidenziato = item.article.id == daEvidenziare,
                        ultimoLetto = item.article.id == rigaLetta,
                        selezionato = item.article.id in selectedIds,
                        selectionMode = selectionMode,
                        marcato = item.article.id in marcati,
                        onClick = {
                            val id = item.article.id
                            // 1.4.6 (par. B13): il primo tocco spegne
                            // l'evidenziazione. Ha gia' fatto il suo lavoro —
                            // dirti dove sta — e lasciarla accesa la
                            // trasformerebbe in una macchia permanente.
                            daEvidenziare = null
                            when {
                                selectionMode ->
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                // toccare un messaggio barrato non lo apre: offre
                                // di ripristinarlo, che è l'unica cosa sensata da
                                // fare su qualcosa che sta per essere eliminato
                                id in marcati -> contextItem = item
                                // Piano B: tocco sul testo dell'OP espande/
                                // comprime invece di aprire (il messaggio resta
                                // raggiungibile dal menu → "Apri"). Il +/-
                                // (ingrandito, correzione P) resta identico e
                                // continua a funzionare comunque.
                                AppSettings.tapOpToExpand && toggleAction != null -> toggleAction()
                                // 1.3.2 (Marco): "andare dritto al primo non
                                // letto" anche sul telefono, dove il tocco
                                // NAVIGA VIA e quindi non ha senso espandere.
                                //
                                // Scatta su `isRoot`, non su `total > 1`: nella
                                // vista ad albero — quella per cui è stata
                                // chiesta — `total` resta 0, mentre `isRoot` è
                                // valorizzato sulla riga di testa di ogni
                                // discussione (e MAI nella vista sequenziale,
                                // che è un flusso piatto dove "discussione" non
                                // significa nulla).
                                //
                                // Sta DOPO il ramo tapOpToExpand di proposito:
                                // nella vista raggruppata, con entrambe accese,
                                // espandere ha la precedenza.
                                AppSettings.apriPrimoNonLetto && item.isRoot -> {
                                    val target = primoNonLetto(
                                        // 1.3.5: appartenenza decisa dal threader
                                        // (mappaThread), non piu' da un confronto
                                        // di stringhe sulle References — vedi la
                                        // nota lunga in ThreadFlatten.kt.
                                        membriDelThread(articoliVisibili, mappaThread, item.article)
                                    )
                                    nav.navigate(Routes.article(target?.id ?: id))
                                }
                                else -> nav.navigate(Routes.article(id))
                            }
                        },
                        onLongClick = {
                            // la pressione prolungata entra in selezione portandosi
                            // dietro l'elemento toccato: un gesto solo per iniziare
                            if (selectionMode) {
                                val id = item.article.id
                                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                            } else contextItem = item
                        },
                        onToggle = toggleAction,
                    )
                }
                if (items.isEmpty() && !syncing) {
                    item {
                        Text(
                            if (unreadOnly && articles.isNotEmpty())
                                stringResource(R.string.tl_all_read_empty)
                            else
                                stringResource(R.string.tl_no_articles),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                }
            }
        }
    }

    // Menu contestuale come pannello che sale dal basso.
    //
    // Prima era un AlertDialog: le voci stavano impilate dentro confirmButton,
    // e il pulsante "Annulla" (dismissButton) veniva affiancato a quella colonna
    // finendo sovrapposto alle voci, a metà altezza. Un dialogo di conferma è
    // fatto per chiedere sì/no, non per ospitare un elenco di azioni. Il
    // pannello dal basso è il contenitore giusto: le voci scorrono, si chiude
    // scorrendo in giù o toccando fuori, e "Annulla" non serve più.
    ricostruisciMembri?.let { membri ->
        DialogoRicostruisci(repo, membri) { ricostruisciMembri = null }
    }
    contextItem?.let { ctxItem ->
        val a = ctxItem.article
        val marcato = PendingDeletions.isMarked(groupId, a.id)
        ModalBottomSheet(onDismissRequest = { contextItem = null }) {
                // 1.4.10 (par. K3, Macchia) — IL FOGLIO SCORRE DAVVERO.
                //
                // ⚠️ IL COMMENTO QUI SOPRA DICEVA GIA' «le voci scorrono».
                // NON ERA VERO: il contenuto era una Column senza
                // verticalScroll, e quando le voci superavano l'altezza
                // massima del foglio l'ultima veniva TAGLIATA, senza modo di
                // raggiungerla.
                //
                // Segnalato da Macchia, che ha anche isolato la condizione da
                // solo: succede SOLO quando nel ramo ci sono suoi messaggi,
                // perche' allora compare la voce in piu' «Vai alla prima
                // risposta ai miei messaggi» e le voci passano da tredici a
                // quattordici. Senza quella, ci stanno.
                //
                // *E' il secondo commento in tre giorni che dichiarava una
                // garanzia inesistente, dopo «altezza minima garantita» del
                // campo messaggio (par. K1). Un commento che dichiara una
                // garanzia non e' una garanzia.*
                //
                // ⚠️ La correzione e' in DUE posti, questo e il gemello in
                // ThreePaneHome: lo stesso foglio senza scorrimento esiste
                // anche nella vista a pannelli. Correggerne uno solo sarebbe
                // il guasto della 1.3, quello che ha fondato la regola piu'
                // citata del quaderno.
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    a.subject,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                HorizontalDivider()

                if (marcato) {
                    // Messaggio già contrassegnato: l'azione utile è una sola.
                    VoceMenu(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.tl_restore_message)) {
                        PendingDeletions.unmark(groupId, listOf(a.id))
                        marcatiVersione++
                        contextItem = null
                    }
                    val altri = PendingDeletions.of(groupId)
                    if (altri.size > 1) {
                        VoceMenu(Icons.Default.RestoreFromTrash, stringResource(R.string.tl_restore_all, altri.size)) {
                            PendingDeletions.unmark(groupId, altri.toList())
                            marcatiVersione++
                            contextItem = null
                        }
                    }
                } else {
                    // 1.2.9 (Piano B, Roxy): con l'opzione "tocca l'OP per
                    // espandere" attiva, il tocco sulla riga non apre più
                    // l'articolo — questa voce resta il percorso sempre
                    // disponibile per farlo (utile anche a chi l'opzione non
                    // ce l'ha: è un secondo modo, ordinario, di aprire da qui).
                    VoceMenu(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.common_open_message)) {
                        contextItem = null
                        nav.navigate(Routes.article(a.id))
                    }
                    HorizontalDivider()
                    // beta20 (Bingo3331): salto alle risposte ai propri messaggi
                    // anche da qui, non solo dall'omino (vedi ThreePaneHome per
                    // il ragionamento: il menu è un percorso più affidabile e
                    // rende la funzione scopribile). Nella vista a pagine
                    // selezionare un articolo significa APRIRLO, quindi si
                    // naviga direttamente alla lettura.
                    // 1.3.5: la radice non serve piu' qui — questa schermata
                    // non ha thread da espandere, e l'appartenenza la decide
                    // ora mappaThread. La variabile sarebbe rimasta inutilizzata.
                    val risposteAMeMenu = membriDelThread(articles, mappaThread, ctxItem.article, soloRisposteAMe = true)
                    if (risposteAMeMenu.isNotEmpty()) {
                        val nonLette = risposteAMeMenu.count { !it.read }
                        VoceMenu(
                            Icons.Default.Person,
                            if (nonLette > 0) stringResource(R.string.common_go_first_unread_reply, nonLette)
                            else stringResource(R.string.common_go_first_reply, risposteAMeMenu.size),
                        ) {
                            val target = primoNonLetto(risposteAMeMenu)
                            contextItem = null
                            if (target != null) nav.navigate(Routes.article(target.id))
                        }
                        HorizontalDivider()
                    }
                    VoceMenu(
                        Icons.Default.Markunread,
                        if (a.read) stringResource(R.string.common_mark_message_unread) else stringResource(R.string.common_mark_message_read),
                    ) {
                        scope.launch { repo.setRead(a.id, !a.read) }
                        contextItem = null
                    }
                    VoceMenu(Icons.Default.Drafts, stringResource(R.string.common_mark_thread_read)) {
                        val ids = threadIdsOf(a.id)
                        scope.launch { repo.setReadBulk(ids, true, groupId) }
                        contextItem = null
                    }
                    VoceMenu(Icons.Default.Markunread, stringResource(R.string.common_mark_thread_unread)) {
                        val ids = threadIdsOf(a.id)
                        scope.launch { repo.setReadBulk(ids, false, groupId) }
                        contextItem = null
                    }
                    // 1.6 (sondaggio sui gruppi): vedi filter/IgnoraDiscussione.kt.
                    val ignorata by produceState(false, a.id) { value = repo.eIgnorata(a.messageId) }
                    VoceMenu(
                        Icons.Default.VisibilityOff,
                        stringResource(if (ignorata) R.string.common_unignore_thread else R.string.common_ignore_thread),
                    ) {
                        val membri = membriDelThread(articles, mappaThread, a)
                        contextItem = null
                        scope.launch {
                            if (ignorata) repo.smettiDiIgnorare(membri) else repo.ignoraDiscussione(membri)
                            snackbar.showSnackbar(
                                ctx.getString(if (ignorata) R.string.common_unignore_thread_done else R.string.common_ignore_thread_done),
                            )
                        }
                    }
                    // 1.6 (sondaggio sui gruppi): solo se manca davvero qualcosa.
                    val mancanti by produceState(0, a.id, articles.size) {
                        value = repo.quantiMancanti(membriDelThread(articles, mappaThread, a))
                    }
                    if (mancanti > 0) {
                        VoceMenu(Icons.Default.Download, stringResource(R.string.common_rebuild_thread, mancanti)) {
                            ricostruisciMembri = membriDelThread(articles, mappaThread, a)
                            contextItem = null
                        }
                    }
                    HorizontalDivider()
                    VoceMenu(Icons.Default.Block, stringResource(R.string.common_ignore_author)) {
                        val id = a.id
                        contextItem = null
                        scope.launch {
                            val autore = repo.plonkAuthor(id)
                            snackbar.showSnackbar(
                                if (autore != null) ctx.getString(R.string.common_ignore_author_success, autore)
                                else ctx.getString(R.string.common_author_unidentified)
                            )
                        }
                    }
                    HorizontalDivider()
                    // beta20 (Roxy): elimina l'INTERA discussione dal dispositivo.
                    VoceMenu(Icons.Default.DeleteSweep, stringResource(R.string.common_delete_thread)) {
                        // 1.3.5: gli id li decide threadIdsOf (threader).
                        val idsThread = threadIdsOf(a.id)
                        contextItem = null
                        scope.launch {
                            val n = repo.eliminaDiscussione(idsThread)
                            snackbar.showSnackbar(
                                if (n > 0) ctx.getString(R.string.common_thread_deleted, n) else ctx.getString(R.string.common_no_message_deleted)
                            )
                        }
                    }
                    // 1.2.11 (Marco, 6 settembre 2026): Sostituisci e Ritira
                    // compaiono SOLO sui propri messaggi. Su un messaggio
                    // altrui il server rifiuterebbe comunque l'operazione:
                    // offrire il comando sarebbe solo fuorviante. Il criterio
                    // "è mio?" è lo stesso, centralizzato, già usato dall'omino
                    // nella lista discussioni e dalla rilevazione replyToMe.
                    if (AppSettings.eMioPost(a.fromField)) {
                        HorizontalDivider()
                        VoceMenu(Icons.Default.Edit, stringResource(R.string.tl_supersede)) {
                            val id = a.id
                            contextItem = null
                            nav.navigate(Routes.composeSupersede(id))
                        }
                        VoceMenu(Icons.Default.Cancel, stringResource(R.string.tl_cancel_post)) {
                            contextItem = null
                            cancelTarget = a
                        }
                    }
                    VoceMenu(Icons.Default.Delete, stringResource(R.string.tl_delete_local_copy)) {
                        PendingDeletions.mark(groupId, listOf(a.id))
                        marcatiVersione++
                        contextItem = null
                    }
                    VoceMenu(Icons.Default.Checklist, stringResource(R.string.tl_select_multiple)) {
                        selectedIds = setOf(a.id)
                        selectionMode = true
                        contextItem = null
                    }
                }
            }
        }
    }

    /*
     * 1.4.4 (Roxy, 24 settembre 2026) — I TRE DIALOGHI DEL RITIRO SONO USCITI
     * DA QUI.
     *
     * Vivevano in questo file, e per questo "Ritira dal server" esisteva solo
     * nella vista a pagine: il menu contestuale dei pannelli non poteva averlo
     * senza ricopiarli, cioe' senza commettere il difetto che il progetto ha
     * gia' pagato sei volte. Ora stanno in RitiroArticolo.kt e le due viste ne
     * chiamano uno solo.
     *
     * Si chiama SEMPRE, non dentro un `if`: dopo la conferma `cancelTarget`
     * torna null ma l'attesa e l'esito devono continuare a vivere.
     */
    DialoghiRitiro(
        repo = repo,
        bersaglio = cancelTarget,
        onFine = { cancelTarget = null },
        snackbar = snackbar,
    )

    if (bodiesDialog) {
        AlertDialog(
            onDismissRequest = { bodiesDialog = false },
            title = { Text(stringResource(R.string.common_download_bodies_title)) },
            text = {
                Text(
                    stringResource(R.string.common_download_bodies_body),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    bodiesDialog = false
                    scope.launch {
                        syncing = true
                        runCatching { repo.downloadAllBodies(groupId) }
                            .onSuccess { n -> snackbar.showSnackbar(ctx.getString(R.string.msg_bodies_downloaded, n)) }
                            .onFailure { snackbar.showSnackbar(ctx.getString(R.string.msg_error, it.message)) }
                        syncing = false
                    }
                }) { Text(stringResource(R.string.common_download)) }
            },
            dismissButton = { TextButton(onClick = { bodiesDialog = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    if (reloadDialog) {
        // 1.4.7 (§A17): il dialogo e' SPOSTATO in DialogoRiscaricaGruppo.kt,
        // non ricopiato. Era in due esemplari gia' divergenti; con la richiesta
        // di Jeff ne sarebbe servito un terzo.
        DialogoRiscaricaGruppo(
            groupId = groupId,
            repo = repo,
            // Lo scope e' QUELLO DELLA SCHERMATA, non del dialogo:
            // vedi il blocco di commento in DialogoRiscaricaGruppo.kt
            // — e' il difetto della build 135.
            scope = scope,
            snackbar = snackbar,
            onChiudi = { reloadDialog = false },
            onSyncing = { syncing = it },
        )
    }

    if (retentionDialog) {
        RetentionDialog(
            current = groupPurge,
            onDismiss = { retentionDialog = false },
            // beta20 (Paul): il dialog si CHIUDE PRIMA di avviare l'operazione.
            // Prima l'aggiornamento di stato e la coroutine di cancellazione si
            // sovrapponevano alla composizione del dialog ancora aperto: su
            // Android 10 questo causava un crash toccando Annulla dopo aver
            // spuntato l'eliminazione.
            onPick = { days ->
                retentionDialog = false
                groupPurge = days
                groupKeepThreads = null
                scope.launch {
                    // il criterio a tempo esclude quello a numero: lo azzero qui,
                    // in un'unica azione invece di due callback concorrenti
                    repo.setGroupPurgeKeepThreads(groupId, null)
                    repo.setGroupPurgeDays(groupId, days)
                }
            },
            currentKeepThreads = groupKeepThreads,
            onPickKeepThreads = { n ->
                retentionDialog = false
                groupKeepThreads = n
                scope.launch {
                    repo.setGroupPurgeKeepThreads(groupId, n)
                    if (n != null && n > 0) repo.purgeGroupKeepThreads(groupId, n)
                }
            },
            // 1.4 (Henry The Mole): introduzione alla citazione per gruppo.
            currentIntro = groupReplyIntro,
            onPickIntro = { intro ->
                groupReplyIntro = intro
                scope.launch { repo.setGroupReplyIntro(groupId, intro) }
            },
            // 1.6 (Dave Royal): identità per gruppo.
            identita = identita,
            currentIdentityId = groupIdentityId,
            onPickIdentity = { id ->
                groupIdentityId = id
                scope.launch { repo.assignIdentityToGroup(groupId, id) }
            },
        )
    }
}

// wishlist beta11: non più private — riusata anche da ThreePaneHome per
// parità del menu ⋮ nel layout a tre pannelli.
// beta19 (Roxy): ora offre DUE criteri di conservazione — a tempo (giorni) o a
// NUMERO DI DISCUSSIONI. Motivo di Roxy: con la sola scadenza a tempo un gruppo
// poco frequentato si svuota, mentre spesso servono discussioni vecchie ma
// ancora utili. Il criterio a numero, se impostato, ha la precedenza.
@Composable
fun RetentionDialog(
    current: Int?,
    onDismiss: () -> Unit,
    onPick: (Int?) -> Unit,
    currentKeepThreads: Int? = null,
    onPickKeepThreads: ((Int?) -> Unit)? = null,
    /**
     * 1.4 (Henry The Mole) — introduzione alla citazione per QUESTO gruppo.
     * null/vuota = si usa quella dell'identita'.
     *
     * Sta QUI e non dietro una voce di menu nuova per una ragione pratica: il
     * menu del gruppo esiste in DUE schermate (lista discussioni e vista a
     * pannelli), mentre questo dialogo e' gia' condiviso da entrambe. Una voce
     * nuova avrebbe voluto dire due registrazioni, cioe' due occasioni di
     * dimenticarne una — il difetto di `esporta` (tre posti su quattro).
     */
    currentIntro: String? = null,
    onPickIntro: ((String?) -> Unit)? = null,
    /**
     * 1.6 (Dave Royal) — identità per QUESTO gruppo, null = quella del server.
     * Sta qui per la stessa ragione dell'introduzione qui sopra. Con una sola
     * identità non c'è niente da scegliere e la sezione non compare.
     */
    identita: List<Identity> = emptyList(),
    currentIdentityId: Long? = null,
    onPickIdentity: ((Long?) -> Unit)? = null,
) {
    // Bozza locale: si scrive liberamente e si salva alla conferma. Salvare a
    // ogni tasto vorrebbe dire una scrittura sul database per carattere.
    var introBozza by remember(currentIntro) { mutableStateOf(currentIntro.orEmpty()) }
    // null = usa predefinito globale; 0 = mai; N = giorni
    val options: List<Pair<Int?, String>> = listOf(
        null to stringResource(R.string.tl_use_default),
        0 to stringResource(R.string.tl_never_keep_all),
        30 to stringResource(R.string.mnt_days_30),
        90 to stringResource(R.string.mnt_days_90),
        180 to stringResource(R.string.mnt_days_180),
        365 to stringResource(R.string.mnt_year_1),
    )
    val opzioniThread: List<Pair<Int?, String>> = listOf(
        50 to stringResource(R.string.tl_keep_latest_threads, 50),
        100 to stringResource(R.string.tl_keep_latest_threads, 100),
        200 to stringResource(R.string.tl_keep_latest_threads, 200),
        500 to stringResource(R.string.tl_keep_latest_threads, 500),
    )
    val aNumero = (currentKeepThreads ?: 0) > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tl_group_retention)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.tl_retention_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (onPickKeepThreads != null) {
                    Text(
                        stringResource(R.string.tl_by_thread_count),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        stringResource(R.string.tl_thread_count_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    opzioniThread.forEach { (n, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPickKeepThreads(n) },
                        ) {
                            RadioButton(selected = aNumero && currentKeepThreads == n, onClick = null)
                            Text(label)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        stringResource(R.string.tl_by_time),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                options.forEach { (days, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // beta20 (Paul): PRIMA qui partivano DUE callback
                                // in sequenza — `onPickKeepThreads(null)` e
                                // `onPick(days)` — ognuno dei quali aggiornava lo
                                // stato e poteva chiudere il dialog, con una
                                // coroutine di cancellazione avviata nel mezzo
                                // mentre il dialog era ancora composto. Su
                                // Android 10 (ricomposizione piu' lenta) questo
                                // portava al crash. Ora e' UNA sola azione: il
                                // chiamante azzera il criterio a numero dentro
                                // onPick, senza doppio aggiornamento.
                                onPick(days)
                            },
                    ) {
                        RadioButton(selected = !aNumero && current == days, onClick = null)
                        Text(label)
                    }
                }
                // 1.6 (Dave Royal): identità per gruppo. Vale subito, come le
                // righe qui sopra, ma senza chiudere: sotto c'è l'introduzione.
                if (onPickIdentity != null && identita.size > 1) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text(
                        stringResource(R.string.tl_group_identity),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        stringResource(R.string.tl_group_identity_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    val scelte: List<Pair<Long?, String>> =
                        listOf<Pair<Long?, String>>(null to stringResource(R.string.tl_group_identity_inherit)) +
                            identita.map { it.id to it.nickname.ifBlank { it.name } }
                    var scelta by remember(currentIdentityId) { mutableStateOf(currentIdentityId) }
                    scelte.forEach { (id, nome) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scelta = id; onPickIdentity(id) },
                        ) {
                            RadioButton(selected = scelta == id, onClick = null)
                            Text(nome)
                        }
                    }
                }
                // 1.4 (Henry The Mole): introduzione alla citazione per gruppo.
                if (onPickIntro != null) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text(
                        stringResource(R.string.tl_group_intro),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        stringResource(R.string.tl_group_intro_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    OutlinedTextField(
                        value = introBozza,
                        onValueChange = { introBozza = it },
                        label = { Text(stringResource(R.string.tl_group_intro_label)) },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // 1.4: l'introduzione si salva alla chiusura, non a ogni tasto.
                onPickIntro?.invoke(introBozza.trim().takeIf { it.isNotBlank() })
                onDismiss()
            }) { Text(stringResource(R.string.common_close)) }
        },
    )
}

/**
 * Una riga della lista. Nella vista raggruppata le righe radice ricevono
 * [onToggle] (non nullo) e mostrano il +/- a sinistra piu' il contatore dei
 * messaggi da leggere a destra; le risposte gia' aperte ricevono null e uno
 * spazio della stessa larghezza, cosi' restano incolonnate sotto il titolo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadRow(
    item: ThreadItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (() -> Unit)? = null,
    /** 1.4.6 (par. B13): la riga raggiunta da «mostra nell'elenco». */
    evidenziato: Boolean = false,
    /** 1.5.9 / 177 (vort3): l'ultimo articolo letto, o la testa della sua discussione chiusa. */
    ultimoLetto: Boolean = false,
    selezionato: Boolean = false,
    selectionMode: Boolean = false,
    marcato: Boolean = false,
) {
    val article = item.article
    // 1.4.7 (§A15 build 1): formula estratta in RientroAlbero.kt. Era una
    // delle TRE copie identiche sparse in tre file.
    val indent = rientroAlbero(item.depth)
    // larghezza riservata al +/- : le risposte la lasciano vuota per allinearsi.
    // Era 36.dp (post-1.2.9, Roxy sul TV Box): sotto il minimo Material di
    // 48dp per un bersaglio toccabile, e a distanza con un telecomando la
    // mira ne risente ancora di più che col dito su un telefono in mano.
    val gutter = 48.dp
    // 1.4.7 (§A15 build 2, Dave Royal): le rotaie verticali arrivano anche qui.
    // Catturato come val perché drawBehind non è @Composable e non può leggere
    // il tema — stessa ragione per cui lo fa ThreePaneHome da sempre.
    val railColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                when {
                    selezionato -> MaterialTheme.colorScheme.secondaryContainer
                    // 1.4.6 (par. B13): tinta DIVERSA da quella della selezione.
                    // Sono due stati che possono coesistere e vogliono dire cose
                    // diverse — «questa l'hai scelta tu» e «questa e' quella che
                    // cercavi» — e un colore solo per entrambi le confonderebbe.
                    evidenziato -> MaterialTheme.colorScheme.tertiaryContainer
                    // 1.5.9 / 177 (vort3): «in modo leggero». Tinta neutra,
                    // sotto le altre due: e' un promemoria, non uno stato.
                    ultimoLetto -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            // beta18 (Bingo3331): barretta sul bordo sinistro se questa riga è una
            // risposta a un mio post (Piano A). Usa il campo esistente
            // article.replyToMe. Teal, come in ThreePaneHome. Sostituisce il
            // vecchio prefisso "↩ " nel titolo (che rubava spazio al testo).
            .drawBehind {
                // 1.4.7 (§A15 build 2): prima le rotaie, poi la barretta teal —
                // lo stesso ordine dei pannelli, così dove coesistono la teal
                // resta sopra. La geometria combacia perché anche qui il testo
                // parte da `4.dp + indent`, identico a ThreePaneHome.
                disegnaRotaieAlbero(item.depth, passoRientro(), railColor)
                if (item.article.replyToMe) {
                    // sfondo teal semi-trasparente solo se non letto (vedi
                    // ThreePaneHome per la logica completa: barretta = sempre,
                    // sfondo = solo non letto).
                    if (!item.article.read) {
                        drawRect(RispostaAMeColor.copy(alpha = 0.12f))
                    }
                    drawLine(
                        RispostaAMeColor,
                        Offset(1.5.dp.toPx(), 0f),
                        Offset(1.5.dp.toPx(), size.height),
                        strokeWidth = 3.dp.toPx(),
                    )
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 4.dp + indent, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            // In selezione la spunta prende il posto del +/-: le due cose non
            // servono mai insieme e affiancarle stringerebbe troppo il titolo.
            Icon(
                if (selezionato) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (selezionato) stringResource(R.string.tl_selected) else stringResource(R.string.tl_not_selected),
                tint = if (selezionato) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(end = 8.dp),
            )
        } else when {
            onToggle != null -> IconButton(onClick = onToggle, modifier = Modifier.size(gutter)) {
                Icon(
                    if (item.expanded) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = if (item.expanded) stringResource(R.string.common_collapse_thread)
                                         else stringResource(R.string.common_expand_thread),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            // riga dentro una discussione aperta, oppure radice senza risposte
            item.rootKey.isNotEmpty() -> Spacer(Modifier.width(gutter))
            else -> Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (article.watched) {
                    Icon(
                        Icons.Default.Star, contentDescription = stringResource(R.string.tl_watched),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                // beta14 (Big Umberto): marca le discussioni in cui ho scritto,
                // per ritrovarle e continuarle senza lasciarle a metà. Solo sulle
                // radici (la discussione), con icona discreta.
                if (item.isRoot && item.hasMyPost) {
                    Icon(
                        // 1.4.1 (105/I): il numero accanto all'omino sono le
                        // RISPOSTE ai miei post, non i miei post — BIG Umberto
                        // l'ha letto come "2 post miei", e senza etichetta era
                        // una lettura ragionevole.
                        Icons.Default.Person,
                        contentDescription = if (item.repliesToMe > 0)
                            stringResource(R.string.tl_my_post_with_replies, item.repliesToMe)
                        else stringResource(R.string.common_contains_my_message),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 4.dp).size(18.dp),
                    )
                    // beta18 (Bingo3331): contatore risposte dirette ai miei post
                    // (Piano B). Solo se > 0. Teal come la barretta.
                    if (item.repliesToMe > 0) {
                        Text(
                            "${item.repliesToMe}",
                            style = MaterialTheme.typography.labelMedium,
                            color = RispostaAMeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
                Text(
                    article.subject,
                    style = MaterialTheme.typography.bodyLarge,
                    // beta14 (Big Umberto): per le RADICI, lo stato letto/non-letto
                    // guarda l'intera discussione (item.unread), non solo il primo
                    // post: una discussione con nuove risposte deve tornare
                    // "non letta" anche se la radice era già letta. Per le righe
                    // interne (non radici) vale il read del singolo messaggio.
                    fontWeight = if (righeNonLette(item, article)) FontWeight.Bold else FontWeight.Normal,
                    // barrato = contrassegnato per la cancellazione: sparirà uscendo dal gruppo
                    textDecoration = if (marcato) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    color = when {
                        marcato -> MaterialTheme.colorScheme.outline
                        article.hidden -> MaterialTheme.colorScheme.outline
                        article.score >= 100 -> MaterialTheme.colorScheme.primary
                        AppSettings.colorReadState -> readStateColor(!righeNonLette(item, article))
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Text(
                // beta13 (Marco, Piano B — coerenza con ThreePaneHome): nome
                // autore senza <email> + data e ora insieme al posto della sola
                // data compatta. Stessa scelta applicata a entrambe le viste.
                "${authorDisplayName(article.fromField)}" +
                    (if (article.score != 0) "  ·  score ${article.score}" else "") +
                    "  ·  ${formatThreadDateTime(article.dateEpoch)}",
                style = MaterialTheme.typography.bodySmall,
                textDecoration = if (marcato) TextDecoration.LineThrough else null,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        // Contatore della discussione: "da leggere / totale" quando c'è
        // qualcosa da leggere, solo il totale quando è tutta letta. Sapere
        // quanto è lunga una discussione aiuta a decidere se aprirla.
        if (item.isRoot && item.total > 0) {
            val tuttoLetto = item.unread == 0
            Badge(
                // beta14 (Big Umberto): quando ci sono non letti, badge ROSSO
                // (error) invece di primary — più evidente e non smorzato da
                // Material You. Coerente col badge dei gruppi.
                containerColor = if (tuttoLetto) MaterialTheme.colorScheme.surfaceVariant
                                 else MaterialTheme.colorScheme.error,
                contentColor = if (tuttoLetto) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(start = 8.dp),
            ) { Text(if (tuttoLetto) "${item.total}" else "${item.unread}/${item.total}") }
        }
    }
}

/**
 * Voce del menu contestuale a pannello: icona + testo, tutta la larghezza
 * cliccabile. Uniforma spaziatura e altezza fra le voci, che con i TextButton
 * del vecchio dialogo risultavano irregolari.
 */
@Composable
private fun VoceMenu(icona: androidx.compose.ui.graphics.vector.ImageVector, testo: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icona, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(testo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 20.dp))
    }
}
