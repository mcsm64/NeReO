/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Layout a TRE PANNELLI (gruppi | thread | articolo) ispirato a MesNews,
 * usato automaticamente su schermi larghi (landscape / tablet). Il flusso a
 * pagine per telefono resta invariato. Selezione di gruppo/articolo come stato
 * LOCALE; la composizione (nuovo thread / risposta) avviene IN-PANNELLO nel
 * riquadro destro, senza cambiare schermata.
 */
package org.openusenet.reader.ui.screens

import org.openusenet.reader.ui.CitazioneInAttesa
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openusenet.reader.ui.TastiVolume
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.Reply
import org.openusenet.reader.ui.icone.OpenInNew
import org.openusenet.reader.ui.icone.Add
import org.openusenet.reader.ui.icone.Block
import org.openusenet.reader.ui.icone.VisibilityOff
import org.openusenet.reader.ui.icone.Cancel
import org.openusenet.reader.ui.icone.ContentCut
import org.openusenet.reader.ui.icone.ContentCopy
// 1.3.6 (ObiWan). SE NON COMPILA: ripiegare su ContentCopy per entrambe.
import org.openusenet.reader.ui.icone.DeleteSweep
import org.openusenet.reader.ui.icone.Download
import org.openusenet.reader.ui.icone.MoreVert
import org.openusenet.reader.ui.icone.Star
import org.openusenet.reader.ui.icone.StarBorder
import org.openusenet.reader.ui.icone.Sync
import org.openusenet.reader.ui.icone.Search
import org.openusenet.reader.ui.icone.Dns
import org.openusenet.reader.ui.icone.Markunread
import org.openusenet.reader.ui.icone.Drafts
import org.openusenet.reader.ui.icone.Edit
import org.openusenet.reader.ui.icone.FilterAlt
import org.openusenet.reader.ui.icone.Fullscreen
import org.openusenet.reader.ui.icone.FullscreenExit
import org.openusenet.reader.ui.icone.Info
import org.openusenet.reader.ui.icone.NewReleases
import org.openusenet.reader.ui.StrisciaAggiornato
import org.openusenet.reader.ui.icone.Update
import org.openusenet.reader.ui.icone.KeyboardArrowUp
import org.openusenet.reader.ui.icone.KeyboardArrowDown
import org.openusenet.reader.ui.icone.SwapVert
import org.openusenet.reader.ui.icone.FormatQuote
import org.openusenet.reader.ui.icone.Groups
import androidx.compose.foundation.layout.Arrangement
import org.openusenet.reader.ui.icone.Remove
import org.openusenet.reader.ui.icone.Palette
import org.openusenet.reader.ui.icone.Person
import org.openusenet.reader.ui.icone.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.openusenet.reader.ui.PulsanteIncolla
import org.openusenet.reader.ui.spiegazioneRifiutoInvio
import org.openusenet.reader.ui.CorpoCitabile
import org.openusenet.reader.ui.rememberAppunti
import org.openusenet.reader.ui.articoloEml
import org.openusenet.reader.ui.articoloGrezzo
import org.openusenet.reader.text.corpoDaInviare
import org.openusenet.reader.text.formatoCorpo
import org.openusenet.reader.ui.nomeFileConserva
import org.openusenet.reader.ui.nomeFileEml
import org.openusenet.reader.ui.rememberConserva
import org.openusenet.reader.ui.rememberSalvaEml
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.openusenet.reader.Branding
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
import org.openusenet.reader.ui.playDing
import org.openusenet.reader.data.Article
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.thread.Threader
import org.openusenet.reader.ui.Routes
import org.openusenet.reader.ui.components.UnsubscribeDialog
import org.openusenet.reader.ui.components.QuoteColoredBody
import org.openusenet.reader.ui.components.readStateColor
import org.openusenet.reader.ui.components.readingColors

/** Cosa si sta componendo nel pannello destro. */
private sealed interface ComposeTarget {
    data class NewThread(val groupId: Long) : ComposeTarget
    /**
     * `extraGroups` NULL e lista VUOTA non sono la stessa cosa, e 1.4.6
     * (§A13.2) e' la release in cui hanno smesso di esserlo:
     *  - `null`  = l'utente non ha scelto niente, perche' l'articolo non era un
     *              crosspost e nessun dialogo si e' aperto. Un `Followup-To`
     *              dell'autore puo' ancora decidere la destinazione.
     *  - lista   = l'utente HA scelto nel dialogo, **anche se e' vuota**
     *              («solo questo gruppo»), e la sua scelta non si scavalca.
     */
    data class Reply(val articleId: Long, val extraGroups: List<String>? = null) : ComposeTarget
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThreePaneHome(repo: NewsRepository, nav: NavHostController) {
    val groups by repo.subscribedGroups.collectAsState(initial = emptyList())
    val servers by repo.servers.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    // 1.6.3 (Dave Royal): nei pannelli «Segna tutto letto uscendo dal gruppo» non
    // era mai stato collegato. L'uscita sono i due gesti voluti, il tocco su un
    // altro gruppo e Avanti: passano tutti e due di qui, e nient'altro. La
    // rotazione, il ritorno da un'altra schermata o la disiscrizione cambiano il
    // gruppo selezionato senza passare di qui, e non segnano niente.
    val avvisi = LocalAvvisi.current
    fun lasciaGruppoPannelli(nuovo: Long) {
        val prima = selectedGroupId
        selectedGroupId = nuovo
        if (prima != null && eUscitaDalGruppo(prima, nuovo)) {
            avvisi.scope.launch {
                offriAnnullaLetti(repo, ctx, avvisi, mapOf(prima to applicaUscitaGruppo(repo, prima)))
            }
        }
    }
    // 1.4.1 (105/C): stesso difetto di ArticleScreen — vedi la nota li'.
    var selectedArticleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var compose by remember { mutableStateOf<ComposeTarget?>(null) }
    var syncing by remember { mutableStateOf(false) }
    // beta14 (Roxy): stato di refresh del PANNELLO CENTRALE (gruppo corrente),
    // separato da `syncing` che è il refresh della lista gruppi a sinistra.
    var syncingGroup by remember { mutableStateOf(false) }
    // discussioni aperte nella vista raggruppata (per messageId della radice)
    var expandedRoots by remember { mutableStateOf(emptySet<String>()) }
    // nasconde i messaggi gia' letti nel pannello discussioni. Condivide la
    // preferenza con la lista a schermo intero, cosi' la scelta e' una sola.
    // 1.4.7 (§A16): letta direttamente, senza copia locale — stessa ragione e
    // stesso rimedio di ThreadListScreen, dove il commento esteso spiega il
    // perche'. `AppSettings.unreadOnly` e' uno stato osservabile.
    val unreadOnly = AppSettings.unreadOnly
    // riga su cui è stato aperto il menu contestuale (pressione prolungata)
    var contextItem by remember { mutableStateOf<ThreadItem?>(null) }
    // 1.4.4 (Roxy): il messaggio (proprio) per cui si sta chiedendo conferma
    // del ritiro dal server. Unica riga di stato che resta al chiamante: i tre
    // dialoghi e la loro meccanica vivono in RitiroArticolo.kt, condivisi con
    // la vista a pagine.
    var cancelTarget by remember { mutableStateOf<Article?>(null) }
    val snackbar = remember { SnackbarHostState() }
    // 1.2.6 (Marco): controllo aggiornamenti manuale dalla barra principale.
    var checkAggiornamenti by remember { mutableStateOf(false) }
    org.openusenet.reader.ui.ControlloAggiornamentiManuale(checkAggiornamenti) { checkAggiornamenti = false }
    // 1.2.6 (Marco/Dave): modalità riordino gruppi nella colonna a pannelli (prima
    // il riordino era solo in phone mode). Attivabile per non affollare la colonna
    // stretta: quando ON, ogni gruppo mostra frecce su/giù al posto del badge.
    var riordinaGruppi by remember { mutableStateOf(false) }
    // 1.6.3 (Dave Royal): menu della colonna dei gruppi e «Segna letti tutti i gruppi».
    var menuGruppi by remember { mutableStateOf(false) }
    var dialogoTuttiLetti by remember { mutableStateOf(false) }
    // 1.3 (D — lettura a tutto schermo nei pannelli, richiesta di PiaoHong/
    // Dave). Nasconde temporaneamente le colonne gruppi e discussioni,
    // lasciando solo la lettura a piena larghezza — utile su tablet in
    // verticale o quando si vuole concentrarsi sul testo senza rifare il
    // layout dell'app. Ha senso solo con un articolo aperto: si azzera da
    // sola quando l'articolo si chiude, così non si resta "intrappolati".
    var fullScreenReading by remember { mutableStateOf(false) }
    LaunchedEffect(selectedArticleId) {
        if (selectedArticleId == null) fullScreenReading = false
    }
    // 1.3 (C — aggiornamento selettivo, vedi NeReO_feature_refresh_selettivo.md).
    // Qui, in tablet mode, solo il livello GLOBALE (scegli i server): la
    // colonna gruppi non ha intestazioni per server come in GroupsScreen (è
    // una lista piatta, stretta apposta per lo schermo diviso), quindi il
    // livello "scegli i gruppi di questo server" non ha un posto naturale
    // senza un redesign della colonna — rimandato, non urgente: chi usa i
    // pannelli (Roxy, TV Box) ha comunque "aggiorna questo gruppo" (icona fissa
    // nel pannello centrale, già esistente) come livello più fine.
    // 1.4.4: lo stato del menu dell'icona aggiorna non sta piu' qui — vive
    // dentro AzioneAggiorna, insieme al menu che lo usa.
    var pickServersDialogPannello by remember { mutableStateOf(false) }
    var selectedServerIdsPannello by remember { mutableStateOf<Set<Long>>(emptySet()) }
    suspend fun eseguiSyncPannello(azione: suspend () -> Int) =
        aggiornaConEsito(repo, ctx, snackbar, { syncing = it }, azione)
    // beta14 (Bingo3331): sync automatico all'apertura, se attivo. Stessa
    // guardia di sessione di GroupsScreen: una volta sola per avvio.
    LaunchedEffect(Unit) {
        if (AppSettings.syncOnStart && !AppSettings.syncOnStartDone) {
            AppSettings.syncOnStartDone = true
            syncing = true
            runCatching { repo.syncAllSubscribed() }
                .onSuccess { if (AppSettings.dingOnReply && repo.lastSyncNewToMe > 0) playDing(ctx) }
                .onFailure { }
            syncing = false
        }
    }
    // gruppo su cui e' stata chiesta la disiscrizione (pressione prolungata)
    var daDisiscrivere by remember { mutableStateOf<org.openusenet.reader.data.Group?>(null) }
    // wishlist beta11: parità col menu ⋮ di ThreadListScreen, che nel layout
    // a tre pannelli non era mai stato portato — mancavano interamente
    // "mostra filtrati", "solo seguite" e le azioni di manutenzione gruppo
    var showHidden by remember { mutableStateOf(false) }
    var watchedOnly by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    var reloadDialog by remember { mutableStateOf(false) }
    var bodiesDialog by remember { mutableStateOf(false) }
    var retentionDialog by remember { mutableStateOf(false) }
    // 1.6 (tasto Avanti): il messaggio da aprire dopo il cambio di gruppo, e il
    // fermo ai tocchi ripetuti. Vedi TastoAvanti.kt.
    var articoloInArrivo by remember { mutableStateOf<Long?>(null) }
    // 1.6: la discussione da ricostruire, se il dialogo e' aperto.
    var ricostruisciMembri by remember { mutableStateOf<List<Article>?>(null) }
    var avantiInCorso by remember { mutableStateOf(false) }
    // 1.6 (Dave Royal): le identità fra cui scegliere nel dialogo del gruppo.
    val identita by repo.identities.collectAsState(initial = emptyList())

    val articles by remember(selectedGroupId, showHidden) {
        selectedGroupId?.let { repo.articles(it, showHidden = showHidden, unreadOnly = false) }
            ?: flowOf(emptyList<Article>())
    }.collectAsState(initial = emptyList())

    // 1.3.5 — UNICA FONTE DI VERITA' sull'appartenenza a una discussione.
    // Calcolata dal threader sull'elenco COMPLETO (non su quello filtrato) e
    // memorizzata: cambia solo quando cambia la lista degli articoli.
    // Vedi la nota lunga in ThreadFlatten.kt per i due difetti che questa
    // mappa chiude.
    val mappaThread = remember(articles) { mappaDiscussioni(articles) }

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

    // Filtro "solo seguite" (stessa logica di ThreadListScreen): un articolo
    // resta visibile se lui o un suo antenato via References è watched.
    val articoliVisibili = remember(articles, watchedOnly) {
        if (!watchedOnly) articles
        else {
            val watchedMsgIds = articles.filter { it.watched }.map { it.messageId }.toSet()
            if (watchedMsgIds.isEmpty()) emptyList()
            else {
                fun inThreadSeguito(a: Article): Boolean {
                    if (a.messageId in watchedMsgIds) return true
                    return Regex("<[^>]+>").findAll(a.references).any { it.value in watchedMsgIds }
                }
                articles.filter { inThreadSeguito(it) }
            }
        }
    }

    val flat = remember(articoliVisibili, AppSettings.threadView, AppSettings.threadSort, AppSettings.seguiteInCima, expandedRoots, unreadOnly) {
        buildThreadList(
            articoliVisibili,
            unreadOnly = unreadOnly,
            view = AppSettings.threadView,
            expandedRoots = expandedRoots,
        )
    }

    LaunchedEffect(selectedGroupId) {
        // 1.6: arrivando col tasto Avanti il messaggio da aprire c'e' gia'.
        selectedArticleId = articoloInArrivo
        articoloInArrivo = null
        compose = null
        expandedRoots = emptySet()
        val gid = selectedGroupId ?: return@LaunchedEffect
        syncing = true
        // beta20 (Roxy): RIPRISTINATO il sync all'apertura del gruppo. In beta19
        // l'avevo legato a `syncOnStart` (= "all'avvio dell'app sincronizza TUTTI
        // i gruppi"), che e' un'altra cosa: cosi' aprendo un gruppo non scaricava
        // piu' le intestazioni. Ora scarica sempre, ma la guardia evita il
        // ri-sync se lo stesso gruppo e' stato aggiornato pochi istanti prima
        // (era il caso segnalato da Marco: rientro dall'elenco discussioni).
        //
        // 1.2.10 (voce Y, Big Umberto): stessa guardia esplicita
        // AppSettings.syncOnGroupOpen di ThreadListScreen.kt — vedi lì per i
        // dettagli. Il sync manuale (icona/pull-to-refresh, altrove in questo
        // file) resta sempre disponibile a prescindere.
        if (AppSettings.syncOnGroupOpen && SyncGuard.deveSincronizzare(gid)) {
            runCatching { repo.syncGroup(gid) }
        }
        // wishlist beta11 (Roxy): stesso comportamento di ThreadListScreen
        if (AppSettings.scaricaCorpiAutomatico) {
            runCatching { repo.downloadAllBodies(gid, limit = AppSettings.maxCorpiPerApertura) }
        }
        syncing = false
    }

    Scaffold(
        snackbarHost = { SnackbarConAvvisi(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    // 1.6 / 180 (RPoggi): nella lettura a tutto schermo le
                    // colonne spariscono, e con loro il nome del gruppo della
                    // colonna 2. Li' la barra mostra il gruppo DELL'ARTICOLO
                    // come sul telefono (stesso TitoloGruppo, stesso +N); senza
                    // contatore, che i pannelli non hanno. Fuori dal tutto
                    // schermo resta «NeReO»: il gruppo lo dice gia' la colonna 2.
                    // `articles` e' un Flow di Room: quando il corpo arriva,
                    // headersRaw si aggiorna e il +N compare da se'.
                    val letto = if (fullScreenReading) articles.firstOrNull { it.id == selectedArticleId } else null
                    val gruppoLetto = letto?.let { a -> groups.firstOrNull { it.id == a.groupId } }
                    if (letto == null || gruppoLetto == null) {
                        Text(Branding.APP_NAME)
                    } else {
                        val altri = altriGruppiCrosspost(crosspostGroups(letto.headersRaw), gruppoLetto.name)
                        TitoloGruppo(
                            nome = gruppoLetto.name,
                            server = serverDaMostrare(servers, gruppoLetto.serverId),
                            dettagli = if (altri > 0) "+$altri" else "",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
                actions = {
                    // 1.3 (D): visibile solo con un articolo aperto — è lì che ha senso.
                    if (selectedArticleId != null) {
                        IconButton(onClick = { fullScreenReading = !fullScreenReading }) {
                            Icon(
                                if (fullScreenReading) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = stringResource(
                                    if (fullScreenReading) R.string.pane_fullscreen_exit_cd else R.string.pane_fullscreen_cd
                                ),
                                tint = if (fullScreenReading) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { nav.navigate(Routes.SEARCH) }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.groups_search))
                    }
                    IconButton(onClick = {
                        if (servers.isEmpty()) nav.navigate(Routes.SERVERS) else nav.navigate(Routes.DIRECTORY)
                    }) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.groups_subscribe)) }
                    IconButton(onClick = { nav.navigate(Routes.MAINTENANCE) }) {
                        Icon(Icons.Default.Storage, contentDescription = stringResource(R.string.menu_maintenance))
                    }
                    IconButton(onClick = { nav.navigate(Routes.APPEARANCE) }) {
                        Icon(Icons.Default.Palette, contentDescription = stringResource(R.string.menu_appearance))
                    }
                    IconButton(onClick = { nav.navigate(Routes.IDENTITY) }) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(R.string.menu_identity))
                    }
                    // #7 (parità, 1.2.8): qui c'era l'icona EditNote per le Bozze
                    // di TUTTI i gruppi. 1.2.9 (Piano C, richiesta di Marco): tolta
                    // da qui per lo stesso motivo per cui è sparita dal menu di
                    // GroupsScreen — le bozze sono sempre di UN gruppo, l'ingresso
                    // naturale è nel menu ⋮ del gruppo (vedi sopra in questo stesso
                    // file, dentro il DropdownMenu del pannello thread), dove parte
                    // già filtrata. "Tutte le bozze" resta raggiungibile da lì con
                    // l'interruttore dentro DraftsScreen.
                    IconButton(onClick = { nav.navigate(Routes.FILTERS) }) {
                        Icon(Icons.Default.FilterAlt, contentDescription = stringResource(R.string.menu_filters))
                    }
                    IconButton(onClick = { nav.navigate(Routes.SERVERS) }) {
                        Icon(Icons.Default.Dns, contentDescription = stringResource(R.string.menu_servers))
                    }
                    IconButton(onClick = { nav.navigate(Routes.RELEASE_NOTES) }) {
                        Icon(Icons.Default.NewReleases, contentDescription = stringResource(R.string.releasenotes_title))
                    }
                    IconButton(onClick = { checkAggiornamenti = true }) {
                        Icon(Icons.Default.Update, contentDescription = stringResource(R.string.upd_check_now))
                    }
                    IconButton(onClick = { nav.navigate(Routes.ABOUT) }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.about_title))
                    }
                },
            )
        },
    ) { padding ->
        // i tre pannelli come contenuti riusabili: la disposizione (colonne o
        // righe) e le proporzioni arrivano dalle impostazioni Aspetto
        val groupsPane: @Composable () -> Unit = {
            Column {
                    // 1.5.8: «Aggiornato a <versione> — Novita'», una volta per
                    // versione. Stesso componente di GroupsScreen: ui/StrisciaAggiornato.kt.
                    StrisciaAggiornato(nav)
                    // 1.2.6 (Marco/Dave): toggle "riordina" per la colonna gruppi in
                    // tablet mode. Mostrato solo se ci sono gruppi.
                    if (groups.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                        ) {
                            // 1.3 (C): livello globale anche in tablet mode — accanto al
                            // riordino, stesso posto naturale (icone di servizio della
                            // colonna gruppi).
                            // 1.4.4 (§A7, Dave Royal): icona e menu vivono ora in
                            // AzioneAggiorna.kt, condivisi con la vista a pagine.
                            // Con un server solo il menu non si apre nemmeno.
                            AzioneAggiorna(
                                abilitata = !syncing,
                                numeroServer = servers.size,
                                onAggiornaTutto = {
                                    scope.launch { eseguiSyncPannello { repo.syncAllSubscribed() } }
                                },
                                onScegliServer = {
                                    selectedServerIdsPannello = servers.map { it.id }.toSet()
                                    pickServersDialogPannello = true
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // 1.6.3 (Dave Royal): sul suo schermo qui ci stanno due
                            // icone. Riordina entra nel ⋮ insieme a «Segna letti tutti
                            // i gruppi»; mentre si riordina, al posto del ⋮ c'e' la sua
                            // icona accesa, che chiude il riordino con un tocco.
                            if (riordinaGruppi) {
                                IconButton(onClick = { riordinaGruppi = false }) {
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = stringResource(R.string.groups_reorder_toggle),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                Box {
                                    IconButton(onClick = { menuGruppi = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.menu_more),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    DropdownMenu(expanded = menuGruppi, onDismissRequest = { menuGruppi = false }) {
                                        VoceSegnaLettiTutti { menuGruppi = false; dialogoTuttiLetti = true }
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.groups_reorder_toggle)) },
                                            leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null) },
                                            onClick = { menuGruppi = false; riordinaGruppi = true },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (dialogoTuttiLetti) {
                        DialogoSegnaLettiTutti(repo, servers) { dialogoTuttiLetti = false }
                    }
                    if (pickServersDialogPannello) {
                        AlertDialog(
                            onDismissRequest = { pickServersDialogPannello = false },
                            title = { Text(stringResource(R.string.sel_pick_servers_title)) },
                            text = {
                                Column {
                                    servers.forEach { server ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedServerIdsPannello = if (server.id in selectedServerIdsPannello)
                                                        selectedServerIdsPannello - server.id else selectedServerIdsPannello + server.id
                                                },
                                        ) {
                                            Checkbox(
                                                checked = server.id in selectedServerIdsPannello,
                                                onCheckedChange = {
                                                    selectedServerIdsPannello = if (it) selectedServerIdsPannello + server.id else selectedServerIdsPannello - server.id
                                                },
                                            )
                                            Text(server.host)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    enabled = selectedServerIdsPannello.isNotEmpty(),
                                    onClick = {
                                        pickServersDialogPannello = false
                                        val ids = selectedServerIdsPannello
                                        scope.launch { eseguiSyncPannello { repo.syncServers(ids) } }
                                    },
                                ) { Text(stringResource(R.string.common_ok)) }
                            },
                            dismissButton = { TextButton(onClick = { pickServersDialogPannello = false }) { Text(stringResource(R.string.common_cancel)) } },
                        )
                    }
                    if (groups.isEmpty()) {
                        Text(
                            if (servers.isEmpty())
                                stringResource(R.string.pane_configure_server)
                            else
                                stringResource(R.string.groups_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    PullToRefreshBox(
                        isRefreshing = syncing,
                        onRefresh = {
                            scope.launch { eseguiSyncPannello { repo.syncAllSubscribed() } }
                        },
                    ) {
                    LazyColumn {
                        // 1.4.4 (§A9, Roxy): la colonna era una lista UNICA, e con
                        // due gruppi omonimi su server diversi non c'era modo di
                        // sapere quale fosse quale. Ora ha le sezioni per server
                        // come la vista a pagine, dalla stessa funzione — vedi
                        // IntestazioneServer.kt, che gestisce anche gli orfani.
                        // La query che alimenta questa lista e' stata corretta
                        // nello stesso giro: ordinava senza serverId, quindi i
                        // gruppi dei due server arrivavano gia' INTERCALATI.
                        raggruppaPerServer(groups, servers) { it.serverId }.forEach { sez ->
                        item(key = "srv-${sez.etichetta}") {
                            IntestazioneServer(sez.etichetta)
                        }
                        items(sez.elementi, key = { it.id }) { g ->
                            // wishlist beta12 (Dave Royal, colonna gruppi stretta su
                            // tablet in verticale): prima 2 righe intere, e solo se
                            // ANCHE COSÌ vanno in overflow si passa alla forma
                            // abbreviata — mai troncare a mezza parola.
                            // 1.4.5 (§A11-bis): la logica e' uscita di qui e sta in
                            // IntestazioneServer.kt, perche' Dave l'ha chiesta anche
                            // per la barra della colonna 2, dove le righe sono una.
                            ListItem(
                                headlineContent = { NomeGruppoAdattivo(g.name) },
                                trailingContent = {
                                    if (riordinaGruppi) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { scope.launch { repo.moveGroup(g.serverId, g.id, up = true) } },
                                                modifier = Modifier.size(32.dp),
                                            ) {
                                                Icon(
                                                    Icons.Default.KeyboardArrowUp,
                                                    contentDescription = stringResource(R.string.groups_move_up),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            IconButton(
                                                onClick = { scope.launch { repo.moveGroup(g.serverId, g.id, up = false) } },
                                                modifier = Modifier.size(32.dp),
                                            ) {
                                                Icon(
                                                    Icons.Default.KeyboardArrowDown,
                                                    contentDescription = stringResource(R.string.groups_move_down),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    } else if (g.unreadCount > 0) {
                                        Badge { Text(g.unreadCount.toString()) }
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = if (g.id == selectedGroupId)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                ),
                                modifier = Modifier.combinedClickable(
                                    onClick = { lasciaGruppoPannelli(g.id) },
                                    onLongClick = { daDisiscrivere = g },
                                ),
                            )
                        }
                        }
                        // beta14 (Dave Royal): il gesto pull-to-refresh (che aggiorna
                        // TUTTI i gruppi sottoscritti) non era scopribile — nessuna
                        // icona visibile. Suggerimento discreto, stesso stile di quello
                        // in GroupsScreen. Scelto invece di aggiungere un'icona per non
                        // affollare ancora la toolbar.
                        if (groups.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.pane_refresh_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                    }
            }
        }
        val threadsPane: @Composable () -> Unit = {
            Column {
                    if (selectedGroupId != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 4.dp),
                        ) {
                            // 1.4.4 (§A9): il nome del gruppo e, sotto, da quale
                            // server — ma solo con piu' di un server, che e' il
                            // solo caso in cui c'e' qualcosa da distinguere.
                            // Nella colonna 2, stretta, una riga di troppo si
                            // paga: per questo e' labelSmall e non un titolo.
                            val gruppoCorrente = groups.firstOrNull { it.id == selectedGroupId }
                            // 1.4.5 (§A11-bis, Dave Royal): con la sesta icona in
                            // barra il nome si stringe, e lui ha chiesto che si
                            // abbrevi come in colonna 1 invece di troncarsi e
                            // basta. Una riga sola: qui la barra non puo' crescere.
                            // 1.6 / 180: nome e server passano da TitoloGruppo.
                            TitoloGruppo(
                                nome = gruppoCorrente?.name.orEmpty(),
                                server = serverDaMostrare(servers, gruppoCorrente?.serverId),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            // 1.4.1 (build 108, Dave Royal) — LA BARRA DEI PANNELLI.
                            //
                            // La 107 aveva scambiato Aggiorna e "segna tutto
                            // letto" nella barra del TELEFONO e basta. Dave usa
                            // i pannelli, dove la barra e' un'altra: sei icone
                            // piu' vicine fra loro, e la correzione non c'era.
                            // E' la stessa forma di difetto di "esporta" e di
                            // radiceDi — una funzione applicata ad alcune copie
                            // e non a tutte.
                            //
                            // Qui pero' la soluzione e' diversa da quella del
                            // telefono, e apposta: "Segna tutto come letto" e'
                            // GIA' nel menu dei tre puntini, in questa stessa
                            // schermata. L'icona in barra era un doppione, e
                            // toglierla non leva niente a nessuno — rende il
                            // tocco per sbaglio impossibile invece che meno
                            // probabile. Sul telefono, dove quel menu non ha la
                            // voce, l'icona resta: la' l'unico rimedio a costo
                            // zero era lo scambio.
                            //
                            // Resta invece lo scambio con il filtro dei non
                            // letti, per dare ai due layout lo stesso ordine.
                            IconButton(onClick = {
                                // §A16: si scrive la preferenza, non una copia.
                                AppSettings.unreadOnly = !AppSettings.unreadOnly
                                AppSettings.save()
                            }) {
                                Icon(
                                    if (unreadOnly) Icons.Default.Markunread else Icons.Default.Drafts,
                                    contentDescription = if (unreadOnly) stringResource(R.string.common_showing_unread)
                                                         else stringResource(R.string.pane_hide_read_messages),
                                    tint = if (unreadOnly) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // beta16 (Roxy, TV Box): icona refresh FISSA come
                            // alternativa al gesto pull-to-refresh, che col
                            // touchpad/telecomando che emula il touch non parte
                            // (il trascinamento non viene riprodotto bene). Stessa
                            // logica di syncGroup del PullToRefreshBox sotto.
                            // Durante il refresh mostra lo spinner al posto
                            // dell'icona e si disabilita.
                            IconButton(
                                enabled = !syncingGroup,
                                onClick = {
                                    selectedGroupId?.let { gid ->
                                        scope.launch {
                                            syncingGroup = true
                                            runCatching { repo.syncGroup(gid) }
                                            syncingGroup = false
                                        }
                                    }
                                },
                            ) {
                                if (syncingGroup) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp),
                                    )
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = stringResource(R.string.pane_refresh_group))
                                }
                            }
                            IconButton(onClick = {
                                selectedGroupId?.let { gid ->
                                    val nome = groups.firstOrNull { it.id == gid }?.name ?: ""
                                    nav.navigate(Routes.searchGroup(gid, nome))
                                }
                            }) { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.pane_search_in_group)) }
                            IconButton(onClick = {
                                selectedGroupId?.let { compose = ComposeTarget.NewThread(it) }
                            }) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cmp_new_thread)) }
                            // 1.4.5 (§A11, Dave Royal): l'icona torna in barra, e QUI —
                            // subito prima dei tre puntini, lontana dal filtro dei non
                            // letti, che era il difetto della prima segnalazione. La
                            // 108 l'aveva tolta invece di spostarla come sul telefono,
                            // e costava un tocco in piu' da allora. La voce e' uscita
                            // dal menu qui sotto: una strada sola, come sul telefono.
                            AzioneSegnaTuttoLetto(
                                repo = repo,
                                groupId = selectedGroupId,
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
                                    // 1.2.9 (richiesta di Marco, Piano C): "Bozze"
                                    // spostata qui, dentro il gruppo a cui si scrive
                                    // — non più nel menu principale. Parte filtrata
                                    // su questo gruppo, con un interruttore dentro lo
                                    // schermo per vedere tutte (vedi DraftsScreen).
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.drafts_title)) },
                                        leadingIcon = { Icon(Icons.Default.Drafts, contentDescription = null) },
                                        onClick = {
                                            overflow = false
                                            selectedGroupId?.let { gid -> nav.navigate(Routes.draftsForGroup(gid)) }
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
                                            leadingIcon = {
                                                RadioButton(selected = AppSettings.threadView == v, onClick = null)
                                            },
                                            onClick = {
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
                                            // La mappa non è @Composable e conserva chiavi tecniche:
                                            // l'etichetta viene tradotta qui, al momento della visualizzazione.
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
                                    if (AppSettings.threadView == ThreadViews.ROOTS) {
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.common_expand_all_threads)) },
                                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                            onClick = {
                                                expandedRoots = allRootKeys(articoliVisibili, unreadOnly)
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
                                    // beta14 (Roxy, opzione 2): le due azioni gemelle
                                    // "segna tutto letto/da-leggere" stavano entrambe
                                    // qui, vicine e nell'ordine naturale.
                                    //
                                    // 1.4.1 (build 108): "segna tutto letto" era
                                    // diventata l'UNICA via nei pannelli — la doppia
                                    // spunta in barra era un doppione ed era stata
                                    // tolta perche' si premeva per sbaglio (Dave Royal).
                                    //
                                    // 1.4.5 (§A11, ANCORA Dave Royal): quella cura
                                    // costava un tocco in piu' a ogni uso, e non era
                                    // la stessa applicata al telefono — li' l'icona era
                                    // stata SPOSTATA lontano dal filtro, non tolta.
                                    // Ora la barra ce l'ha anche qui, nella stessa
                                    // posizione, e la voce esce da questo menu: una
                                    // strada sola per layout, e la stessa nei due.
                                    // L'azione vive in AzioneSegnaTuttoLetto.kt.
                                    //
                                    // "Segna tutto da leggere" RESTA nel menu: e' rara
                                    // davvero, e in barra non c'e' mai stata.
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.common_mark_all_unread)) },
                                        leadingIcon = { Icon(Icons.Default.Markunread, contentDescription = null) },
                                        onClick = {
                                            selectedGroupId?.let { gid -> scope.launch { repo.markAllUnread(gid) } }
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
                                        text = { Text(if (showHidden) stringResource(R.string.common_hide_filtered) else stringResource(R.string.common_show_filtered)) },
                                        onClick = { showHidden = !showHidden; overflow = false },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.common_group_retention_menu)) },
                                        onClick = { retentionDialog = true; overflow = false },
                                    )
                                }
                            }
                        }
                    }
                    if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
                    when {
                        selectedGroupId == null -> Text(
                            stringResource(R.string.pane_choose_group),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                        else -> {
                        // beta20 (Roxy): l'indicatore circolare compariva e restava
                        // BLOCCATO tornando in cima all'albero — "lo fa da quando
                        // torni di nuovo in alto, non alla prima apertura".
                        // Causa: risalendo, la lista raggiunge il bordo superiore
                        // con VELOCITÀ RESIDUA; il PullToRefreshBox interpreta
                        // quello slancio come inizio di trascinamento e mostra
                        // l'indicatore, ma il gesto non prosegue quanto basta per
                        // far scattare il refresh: l'indicatore resta a metà,
                        // né attivato né riportato indietro. Alla prima apertura
                        // non accade perché la lista è già ferma in cima.
                        // Rimedio: abilitare il gesto SOLO quando la lista è
                        // davvero ferma in cima (non in movimento inerziale).
                        val listaAlbero = rememberLazyListState()
                        // 1.5.5 / 173 (Marcoseba): anche qui, se il sync
                        // dell'apertura porta novita' e non hai ancora trascinato
                        // la lista, si va in cima a mostrarle. Stessa logica del
                        // telefono, stesso file: SorveglianzaNovita.kt.
                        val improntaPannello = remember(flat, articoliVisibili) {
                            improntaLista(flat.firstOrNull()?.article?.id, articoliVisibili.map { it.id })
                        }
                        InCimaSeArrivanoNovita(
                            listState = listaAlbero,
                            chiave = selectedGroupId,
                            attiva = true,
                            impronta = improntaPannello,
                            vistaSequenziale = AppSettings.threadView == ThreadViews.SEQUENTIAL,
                            ordinamento = AppSettings.threadSort,
                            spegni = selectedArticleId != null,
                        )
                        val fermaInCima by remember {
                            derivedStateOf {
                                !listaAlbero.isScrollInProgress &&
                                    listaAlbero.firstVisibleItemIndex == 0 &&
                                    listaAlbero.firstVisibleItemScrollOffset == 0
                            }
                        }
                        // R2 (Roxy): il cerchietto "aggiorna" restava appeso in cima
                        // all'albero e, per farlo sparire, bisognava scorrere giù più
                        // volte (il primo scroll veniva mangiato per scacciarlo).
                        //
                        // Il tentativo beta20 SCAMBIAVA il PullToRefreshBox dentro/
                        // fuori dalla composizione a seconda di `fermaInCima`. Ma
                        // quello swap strutturale attorno alla stessa lista è di per
                        // sé fragile: nella transizione (fling che si ferma in cima →
                        // box ricomposto) l'indicatore poteva comparire e restare
                        // appeso, e finché mostrato mangiava il primo scroll.
                        //
                        // Rimedio: NON scambiare più il box. Contenitore STABILE, e si
                        // controlla l'ABILITAZIONE del gesto con
                        // `Modifier.pullToRefresh(enabled = fermaInCima)`: quando la
                        // lista non è ferma in cima il gesto è inerte (nessun
                        // indicatore da trascinamento, lo scroll passa liscio), mentre
                        // l'indicatore di sync in corso resta guidato da isRefreshing.
                        val pullState = rememberPullToRefreshState()
                        val contenutoAlbero: @Composable () -> Unit = {
                        LazyColumn(state = listaAlbero) {
                            itemsIndexed(flat, key = { _, it -> it.article.id }) { idx, item ->
                                if (item.threadStart && idx != 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                // 1.2.9 (Piano B, Roxy): stesso lambda usato sia
                                // dal +/- sia, con l'opzione attiva, dal tocco
                                // sul testo dell'OP — vedi ThreadListScreen.
                                val toggleAction: (() -> Unit)? = if (item.isRoot && item.total > 1) {
                                    {
                                        expandedRoots = if (item.rootKey in expandedRoots)
                                            expandedRoots - item.rootKey
                                        else expandedRoots + item.rootKey
                                    }
                                } else null
                                ThreePaneThreadRow(
                                    item = item,
                                    selected = item.article.id == selectedArticleId,
                                    scala = AppSettings.paneListScale,
                                    onLongClick = { contextItem = item },
                                    // 1.3.1 (Roxy): stessa identica azione della
                                    // pressione prolungata, raggiungibile con un
                                    // clic. Un solo punto dove vive l'apertura
                                    // del menu, due modi di scatenarla.
                                    onMenu = { contextItem = item },
                                    onToggle = toggleAction,
                                    // beta20 (Bingo3331): espande il thread e
                                    // seleziona la prima risposta NON LETTA ai
                                    // miei post (o la prima, se lette tutte).
                                    onVaiAlleRisposte = { ti ->
                                        // beta20 (Bingo3331): la radice si ricava
                                        // dall'ARTICOLO, non da `ti.rootKey`.
                                        // Perché i tentativi precedenti fallivano:
                                        // `rootKey` è popolato SOLO dalla vista
                                        // raggruppata; nella vista ad albero e in
                                        // quella sequenziale resta stringa VUOTA.
                                        // Con rk vuoto il filtro impazziva —
                                        // `references.contains("")` è sempre vero,
                                        // quindi passavano tutti gli articoli del
                                        // gruppo — e finiva per non selezionare
                                        // nulla di sensato: restava il primo post
                                        // del thread, il sintomo segnalato.
                                        //
                                        // 1.3.2: la logica sta ora in
                                        // ThreadFlatten (radiceDi /
                                        // membriDelThread / primoNonLetto),
                                        // condivisa con gli altri quattro punti.
                                        // 1.3.5: l'appartenenza la decide il
                                        // THREADER via mappaThread, non piu' un
                                        // confronto di stringhe. radiceDi resta
                                        // solo per la chiave di expandedRoots.
                                        val rk = radiceDi(ti)
                                        if (rk.isNotEmpty()) expandedRoots = expandedRoots + rk
                                        val target = primoNonLetto(
                                            membriDelThread(articles, mappaThread, ti.article, soloRisposteAMe = true)
                                        )
                                        if (target != null) { selectedArticleId = target.id; compose = null }
                                    },
                                ) {
                                    // Piano B: tocco sul testo dell'OP espande/
                                    // comprime invece di selezionare (il messaggio
                                    // resta raggiungibile dal menu → "Apri"). Il
                                    // +/- (ingrandito, correzione P) resta
                                    // identico e continua a funzionare comunque.
                                    if (AppSettings.tapOpToExpand && toggleAction != null) {
                                        // 1.3.1 (Roxy, Box TV): "espandi E VAI",
                                        // non più "espandi e basta".
                                        //
                                        // Il difetto che chiude: con l'opzione
                                        // accesa questo ramo espandeva soltanto,
                                        // e il ramo `else` (che seleziona) non
                                        // veniva MAI eseguito — quindi l'OP di
                                        // una discussione con risposte non si
                                        // poteva aprire nel riquadro di lettura.
                                        // Restava il menu contestuale (pressione
                                        // prolungata), che nella 1.2.9 avevamo
                                        // indicato come via d'uscita: ma su un
                                        // Box TV col telecomando IL TOCCO
                                        // PROLUNGATO NON ESISTE. Per Roxy quel
                                        // messaggio era irraggiungibile, punto.
                                        //
                                        // Perché qui e non anche sul telefono:
                                        // nei pannelli selezionare aggiorna il
                                        // riquadro accanto e si resta nella
                                        // lista, quindi "espandi e vai" è
                                        // coerente. Sul telefono il tocco NAVIGA
                                        // VIA: espandere una lista che si sta per
                                        // abbandonare non avrebbe senso, e lì il
                                        // tocco prolungato c'è.
                                        val eraAperto = item.expanded
                                        toggleAction()
                                        selectedArticleId = if (eraAperto) {
                                            // stiamo CHIUDENDO: le risposte
                                            // spariscono dalla vista, quindi la
                                            // selezione deve restare su una riga
                                            // ancora visibile — l'OP.
                                            item.article.id
                                        } else {
                                            // stiamo APRENDO: "vai al primo NON
                                            // LETTO della discussione", scelta
                                            // esplicita di Roxy ("va in automatico
                                            // sul primo messaggio non letto e
                                            // ovviamente diventa letto, col thread
                                            // aperto. Così va benissimo").
                                            // Se sono tutti letti si ripiega
                                            // sull'OP, che è la richiesta
                                            // originale ("si posiziona sul primo
                                            // post della discussione").
                                            //
                                            // I membri si ricavano da
                                            // articoliVisibili e non da `flat`:
                                            // la lista appiattita verrà
                                            // ricostruita solo alla prossima
                                            // ricomposizione, qui le risposte non
                                            // ci sono ancora. Stesso criterio già
                                            // usato da onVaiAlleRisposte.
                                            primoNonLetto(
                                                // 1.3.5: appartenenza dal threader
                                                membriDelThread(articoliVisibili, mappaThread, item.article)
                                            )?.id ?: item.article.id
                                        }
                                        compose = null
                                    } else {
                                        selectedArticleId = item.article.id
                                        compose = null
                                    }
                                }
                            }
                            if (flat.isEmpty() && !syncing) {
                                item {
                                    // 1.2.5 (Dave Royal): messaggio consapevole del
                                    // filtro. Col filtro "solo non letti" attivo,
                                    // "nessun messaggio" era fuorviante (ce ne sono,
                                    // ma letti) → "nessun messaggio da leggere".
                                    Text(
                                        stringResource(
                                            if (unreadOnly) R.string.pane_no_unread
                                            else R.string.pane_no_articles
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            }
                        }
                        }
                        // Scelta utente (richiesta Marco/Roxy): di default il gesto
                        // "tira giù per aggiornare" resta attivo (a lista ferma in
                        // cima). Se l'utente attiva "solo icona" in Aspetto, il gesto
                        // viene DISATTIVATO del tutto — l'aggiornamento resta possibile
                        // con l'icona Aggiorna già presente in cima al pannello (beta16).
                        // Così in landscape (pannello basso, gesto perennemente armato)
                        // niente più cerchietto appeso né tocchi "mangiati".
                        if (AppSettings.soloIconaAggiornaPannelli) {
                            Box(Modifier.fillMaxSize()) { contenutoAlbero() }
                        } else {
                            Box(
                                Modifier.pullToRefresh(
                                    isRefreshing = syncingGroup,
                                    state = pullState,
                                    enabled = fermaInCima,
                                    onRefresh = {
                                        scope.launch {
                                            syncingGroup = true
                                            val gid = selectedGroupId
                                            if (gid != null) {
                                                SyncGuard.invalida(gid) // gesto esplicito: sincronizza comunque
                                                runCatching { repo.syncGroup(gid) }
                                            }
                                            syncingGroup = false
                                        }
                                    },
                                ),
                            ) {
                                contenutoAlbero()
                                PullToRefreshDefaults.Indicator(
                                    state = pullState,
                                    isRefreshing = syncingGroup,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                )
                            }
                        }
                        }
                    }
            }
        }
        val readingPane: @Composable () -> Unit = {
            Box {
                        // beta18 (Roxy): per la vista continua, la fonte degli
                        // articoli del thread è `articles` (lista completa del
                        // gruppo), NON `flat` (che contiene solo i thread espansi
                        // e solo le righe visibili nella lista UI — causa del bug
                        // segnalato da Roxy: mostrava tutti i post del gruppo).
                        // Il remember si invalida sul rootKey: così cambiando
                        // thread selezionato la LazyColumn si aggiorna.
                        // beta20 (Roxy): il rootKey NON si ricava piu' da `flat`,
                        // che contiene solo le righe VISIBILI (thread espansi):
                        // se il thread era chiuso il rootKey restava vuoto, la
                        // lista usciva vuota e la vista continua ripiegava su un
                        // post per volta. Ora lo ricavo dall'ARTICOLO selezionato:
                        // primo id delle References, o il proprio messageId se e'
                        // una radice. Indipendente da cosa e' espanso a video.
                        // 1.3.5: QUINTA copia della stessa regola a stringhe,
                        // trovata dalla verifica incrociata mentre si correggevano
                        // le altre. Stesso difetto: in vista continua, una
                        // discussione con l'originale mai scaricato mostrava solo
                        // una parte dei suoi messaggi. Nessuno l'aveva segnalata
                        // perche' la vista continua la usano in pochi. Ora passa
                        // da mappaThread come tutto il resto.
                        val selArticle = articles.firstOrNull { it.id == selectedArticleId }
                        val selectedRootKey = selArticle?.let { mappaThread[it.messageId] } ?: ""
                        val threadItems = remember(selectedRootKey, articles, mappaThread) {
                            if (selArticle == null || selectedRootKey.isEmpty()) emptyList()
                            else {
                                val subset = membriDelThread(articles, mappaThread, selArticle)
                                // UK-1 (Dave Royal): la vista continua (col.3) era
                                // ordinata per DATA, mentre l'albero (col.2) è in
                                // ordine JWZ → scorrendo le due colonne non
                                // corrispondevano. Ora la col.3 usa lo STESSO
                                // ordine-albero (padre prima dei figli), così
                                // combaciano. Tutti i post (unreadOnly=false): la
                                // vista continua serve a leggere l'intero thread.
                                buildThreadList(subset, unreadOnly = false, view = ThreadViews.TREE)
                            }
                        }
                        val threadArticles = remember(threadItems) { threadItems.map { it.article } }
                        // 1.2.7 (Dave #1): profondità di ogni post nell'albero, per
                        // marcare con un separatore più spesso l'inizio di una nuova
                        // sotto-discussione nella vista continua.
                        val threadDepths = remember(threadItems) { threadItems.map { it.depth } }
                        // 1.4.6 (§B4): i comandi per spostarsi fra i messaggi nascono
                        // QUI, l'unico posto che ha sia la lista piatta sia
                        // l'articolo selezionato, e scendono come UN parametro
                        // attraverso ArticlePane fino alla barra. Uno per otto: con
                        // un callback per comando sarebbero stati ventiquattro
                        // parametri in piu' su tre firme — l'accumulo che ha
                        // prodotto §B4. Il COSA cercare sta in NavigazioneLettura,
                        // provato in CoreTests; qui c'e' solo il cosa farne.
                        val posLettura = flat.indexOfFirst { it.article.id == selectedArticleId }
                        // 1.6.1 (BIG Umberto): da threadItems, che ha tutta la
                        // discussione. `flat` e' filtrata da «solo non letti»:
                        // il messaggio appena letto ne esce e il tasto si spegneva.
                        val nonLettoDiscussione = idNonLettoNellaDiscussione(threadItems, selectedArticleId)
                        val navigazione = NavigazioneMessaggi(
                            vaiPrecedente = {
                                idMessaggioPrecedente(flat, posLettura)?.let { selectedArticleId = it }
                            },
                            vaiSuccessivo = {
                                idMessaggioSuccessivo(flat, posLettura)?.let { selectedArticleId = it }
                            },
                            vaiPrimoNonLetto = {
                                nonLettoDiscussione?.let { id ->
                                    selectedArticleId = id
                                    // nella vista raggruppata la discussione va aperta
                                    threadItems.firstOrNull { it.article.id == id }
                                        ?.let { expandedRoots = expandedRoots + radiceDi(it) }
                                }
                            },
                            vaiDiscussionePrecedente = {
                                idDiscussionePrecedente(flat, selectedArticleId)?.let { selectedArticleId = it }
                            },
                            vaiDiscussioneSuccessiva = {
                                idDiscussioneSuccessiva(flat, selectedArticleId)?.let { selectedArticleId = it }
                            },
                            // 1.6: NON su `flat`. Con «solo non letti» il messaggio
                            // appena letto ne esce e la posizione diventa -1, e la
                            // vista raggruppata ha le sole radici. La sequenza si
                            // costruisce al tocco, cosi' non costa niente a ogni
                            // messaggio letto. Vedi TastoAvanti.kt.
                            vaiAvanti = vai@{
                                if (avantiInCorso) return@vai
                                avantiInCorso = true
                                scope.launch {
                                    try {
                                        val sequenza = withContext(Dispatchers.Default) {
                                            buildThreadList(articoliVisibili, unreadOnly = false, view = vistaDiNavigazione())
                                        }
                                        val pos = sequenza.indexOfFirst { it.article.id == selectedArticleId }
                                        when (val esito = esitoAvanti(repo, sequenza, pos, selectedGroupId)) {
                                            null -> Unit
                                            is EsitoAvanti.Messaggio -> {
                                                selectedArticleId = esito.id
                                                // nella vista raggruppata la discussione va aperta
                                                sequenza.firstOrNull { it.article.id == esito.id }
                                                    ?.let { expandedRoots = expandedRoots + radiceDi(it) }
                                            }
                                            EsitoAvanti.Fine -> android.widget.Toast.makeText(
                                                ctx, ctx.getString(R.string.art_avanti_fine), android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                            is EsitoAvanti.AltroGruppo -> {
                                                android.widget.Toast.makeText(
                                                    ctx, ctx.getString(R.string.art_avanti_gruppo, esito.nome), android.widget.Toast.LENGTH_SHORT,
                                                ).show()
                                                articoloInArrivo = esito.articoloId
                                                lasciaGruppoPannelli(esito.gruppoId)
                                            }
                                        }
                                    } finally {
                                        avantiInCorso = false
                                    }
                                }
                            },
                            // i flag spengono il pulsante invece di lasciarlo
                            // premere a vuoto: un comando che non fa niente e non
                            // lo dice sembra funzionante, ed e' il difetto peggiore.
                            puoPrecedente = idMessaggioPrecedente(flat, posLettura) != null,
                            puoSuccessivo = idMessaggioSuccessivo(flat, posLettura) != null,
                            esistonoNonLetti = nonLettoDiscussione != null,
                            puoDiscussionePrecedente = idDiscussionePrecedente(flat, selectedArticleId) != null,
                            puoDiscussioneSuccessiva = idDiscussioneSuccessiva(flat, selectedArticleId) != null,
                        )
                        // 1.6: i tasti del volume anche qui, ma mai mentre si
                        // scrive nel pannello. Stessa forma di ArticleScreen.
                        val navigazioneAttuale by rememberUpdatedState(navigazione)
                        val volumeAttivo = compose == null && selectedArticleId != null
                        DisposableEffect(volumeAttivo) {
                            val mio: (TastiVolume.Verso) -> Unit = { v ->
                                if (v == TastiVolume.Verso.AVANTI) navigazioneAttuale.vaiAvanti()
                                else navigazioneAttuale.vaiPrecedente()
                            }
                            if (volumeAttivo) TastiVolume.gestore = mio
                            onDispose { if (TastiVolume.gestore === mio) TastiVolume.gestore = null }
                        }
                        ArticlePane(
                            repo, selectedArticleId, threadArticles, threadDepths,
                            onReply = { id, extra -> compose = ComposeTarget.Reply(id, extra) },
                            onSelect = { id -> selectedArticleId = id; compose = null },
                            navigazione = navigazione,
                        )
            }
        }

        val wG = AppSettings.paneWeightGroups
        val wT = AppSettings.paneWeightThreads
        val wA = AppSettings.paneWeightArticle

        val composeTarget = compose
        if (composeTarget != null) {
            // UK-2 (Dave Royal): in modalità pannelli, scrivendo in landscape la
            // tastiera copriva l'editor confinato in un pannello (colonna stretta
            // o riga in basso). Ora, quando si scrive, l'editor prende TUTTO lo
            // schermo — come la vista a pagine, che non ha mai avuto il problema:
            // così imePadding ridimensiona correttamente e l'area di scrittura
            // resta sopra la tastiera. Alla chiusura si torna ai pannelli.
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                ComposePane(
                    repo = repo,
                    target = composeTarget,
                    onSent = {
                        compose = null
                        scope.launch { selectedGroupId?.let { runCatching { repo.syncGroup(it) } } }
                    },
                    onCancel = { compose = null },
                    snackbar = snackbar,
                )
            }
        } else if (fullScreenReading && selectedArticleId != null) {
            // 1.3 (D): solo readingPane, a piena larghezza — niente pesi/divisori.
            // L'articolo resta intatto: readingPane() è montato in TUTTI i rami
            // (qui, sotto e nella riga), non viene mai smontato durante il
            // toggle. groupsPane/threadsPane invece qui NON sono chiamati: si
            // smontano, e con loro lo scroll delle rispettive LazyColumn (non
            // hoisted) — si perde la posizione, si rimonta dall'alto uscendo dal
            // pieno schermo. Costo accettato: è una modalità che si attiva
            // proprio per allontanarsi da quelle colonne, non per tornarci subito.
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) { readingPane() }
        } else if (AppSettings.paneVertical) {
            // disposizione a TRE RIGHE (gruppi sopra, discussioni, lettura sotto)
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                Box(Modifier.weight(wG).fillMaxWidth()) { groupsPane() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(wT).fillMaxWidth()) { threadsPane() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(wA).fillMaxWidth()) { readingPane() }
            }
        } else {
            // disposizione a TRE COLONNE (classica, stile desktop)
            Row(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                Box(Modifier.weight(wG).fillMaxHeight()) { groupsPane() }
                PaneDivider()
                Box(Modifier.weight(wT).fillMaxHeight()) { threadsPane() }
                PaneDivider()
                Box(Modifier.weight(wA).fillMaxHeight()) { readingPane() }
            }
        }
    }

    // Menu contestuale a pannello dal basso (vedi nota in ThreadListScreen:
    // con l'AlertDialog il pulsante "Annulla" finiva sovrapposto alle voci).
    ricostruisciMembri?.let { membri ->
        DialogoRicostruisci(repo, membri) { ricostruisciMembri = null }
    }
    contextItem?.let { ctxItem ->
        val a = ctxItem.article
        ModalBottomSheet(onDismissRequest = { contextItem = null }) {
            // 1.4.10 (par. K3, Macchia) — IL GEMELLO. Vedi la nota per esteso
            // in ThreadListScreen: lo stesso foglio senza scorrimento, le
            // stesse voci che traboccano. Corretti insieme di proposito.
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
                // 1.2.9 (Piano B, Roxy): con l'opzione "tocca l'OP per
                // espandere" attiva, il tocco sulla riga non seleziona più
                // l'articolo nel pannello di lettura — questa voce resta il
                // percorso sempre disponibile per farlo (utile anche a chi
                // l'opzione non ce l'ha: secondo modo, ordinario, di aprire).
                VoceMenuPannello(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.common_open_message)) {
                    selectedArticleId = a.id
                    compose = null
                    contextItem = null
                }
                HorizontalDivider()
                // beta20 (Bingo3331): percorso ALTERNATIVO e affidabile per
                // saltare alle risposte ai propri messaggi. L'omino cliccabile
                // resta, ma è un `clickable` annidato dentro il
                // `combinedClickable` della riga: una struttura di gesti fragile,
                // perché il contenitore esterno deve prima distinguere tocco da
                // pressione lunga e può sottrarre l'evento al figlio. Qui invece
                // il menu è GIÀ aperto: nessuna competizione di gesti, area
                // ampia, meccanismo già collaudato dalle altre voci.
                // In più la voce rende la funzione SCOPRIBILE: chi non sa che
                // l'omino si tocca, la trova leggendo il menu.
                //
                // Le risposte si CALCOLANO qui dal thread invece di leggere
                // `ctxItem.repliesToMe`: quel campo è valorizzato solo sulle
                // righe di testa, mentre il menu si apre su QUALSIASI riga —
                // tenendo premuto su una risposta interna la voce sarebbe
                // sparita. La radice si ricava dall'articolo perché `rootKey`
                // non è popolato in tutte le viste.
                val rkMenu = radiceDi(ctxItem)
                val risposteAMeMenu = remember(ctxItem.article.id, articles, mappaThread) {
                    membriDelThread(articles, mappaThread, ctxItem.article, soloRisposteAMe = true)
                }
                if (risposteAMeMenu.isNotEmpty()) {
                    val nonLette = risposteAMeMenu.count { !it.read }
                    VoceMenuPannello(
                        Icons.Default.Person,
                        if (nonLette > 0) stringResource(R.string.common_go_first_unread_reply, nonLette)
                        else stringResource(R.string.common_go_first_reply, risposteAMeMenu.size),
                    ) {
                        if (rkMenu.isNotEmpty()) expandedRoots = expandedRoots + rkMenu
                        val target = primoNonLetto(risposteAMeMenu)
                        if (target != null) {
                            selectedArticleId = target.id
                            compose = null
                        }
                        contextItem = null
                    }
                    HorizontalDivider()
                }
                VoceMenuPannello(
                    Icons.Default.Markunread,
                    if (a.read) stringResource(R.string.common_mark_message_unread) else stringResource(R.string.common_mark_message_read),
                ) {
                    val id = a.id; val read = a.read
                    scope.launch { repo.setRead(id, !read) }
                    contextItem = null
                }
                VoceMenuPannello(Icons.Default.Drafts, stringResource(R.string.common_mark_thread_read)) {
                    val ids = threadIdsOf(a.id)
                    scope.launch { repo.setReadBulk(ids, true, a.groupId) }
                    contextItem = null
                }
                VoceMenuPannello(Icons.Default.Markunread, stringResource(R.string.common_mark_thread_unread)) {
                    val ids = threadIdsOf(a.id)
                    scope.launch { repo.setReadBulk(ids, false, a.groupId) }
                    contextItem = null
                }
                // 1.6 (sondaggio sui gruppi): vedi filter/IgnoraDiscussione.kt.
                val ignorata by produceState(false, a.id) { value = repo.eIgnorata(a.messageId) }
                VoceMenuPannello(
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
                    VoceMenuPannello(Icons.Default.Download, stringResource(R.string.common_rebuild_thread, mancanti)) {
                        ricostruisciMembri = membriDelThread(articles, mappaThread, a)
                        contextItem = null
                    }
                }
                HorizontalDivider()
                // beta19 (Roxy): nella vista a pannelli mancava del tutto il modo
                // di marcare una discussione come "seguita" — c'era solo nella
                // vista a pagine. Il filtro "solo seguite" esisteva già qui, ma
                // senza un modo per impostare il flag era inutilizzabile.
                VoceMenuPannello(
                    if (a.watched) Icons.Default.Star else Icons.Default.StarBorder,
                    if (a.watched) stringResource(R.string.art_unwatch) else stringResource(R.string.art_watch),
                ) {
                    // 2.1(a): segui/non-segui su TUTTA la discussione (thread-wide),
                    // come "segna discussione letta". Nuovo stato = opposto del post toccato.
                    val ids = threadIdsOf(a.id)
                    val nuovo = !a.watched
                    scope.launch { repo.setWatchedBulk(ids, nuovo) }
                    contextItem = null
                }
                HorizontalDivider()
                // beta20 (Roxy): elimina l'INTERA discussione dal dispositivo.
                // Diverso da "elimina copia locale", che lasciava le righe barrate.
                VoceMenuPannello(Icons.Default.DeleteSweep, stringResource(R.string.common_delete_thread)) {
                    // 1.3.5: gli id li decide threadIdsOf (threader), non piu'
                    // Repository con un confronto di stringhe sulle References.
                    val idsThread = threadIdsOf(a.id)
                    scope.launch {
                        val n = repo.eliminaDiscussione(idsThread)
                        snackbar.showSnackbar(
                            if (n > 0) ctx.getString(R.string.common_thread_deleted, n) else ctx.getString(R.string.common_no_message_deleted)
                        )
                    }
                    contextItem = null
                }
                HorizontalDivider()
                VoceMenuPannello(Icons.Default.Block, stringResource(R.string.common_ignore_author)) {
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
                /*
                 * 1.4.1 (build 110, Roxy) — "SOSTITUISCI" MANCAVA QUI.
                 *
                 * Roxy chiedeva una funzione di ritiro/sostituzione dal server,
                 * io gli avevo risposto che esiste dalla 1.2.11 e che sta nel
                 * menu contestuale "anche nel layout a pannelli". Lui e' andato
                 * a guardare e non l'ha trovata. Aveva ragione lui.
                 *
                 * NeReO ha DUE menu contestuali di riga: quello di
                 * ThreadListScreen (pagine) e questo (pannelli). "Sostituisci"
                 * e "Ritira" esistevano solo nel primo — quarto caso in tre
                 * giorni di una funzione presente in una copia su due, dopo le
                 * icone della barra, il crosspost su thread nuovo e, a suo
                 * tempo, "esporta".
                 *
                 * PERCHE' SOLO "SOSTITUISCI" E NON ANCHE "RITIRA". Sostituisci
                 * e' una navigazione, una riga, e nello stesso schermo c'e'
                 * gia' il precedente: anche "cerca nel gruppo" dalla barra dei
                 * pannelli naviga cosi'. Il ritiro invece porta con se' tre
                 * dialoghi (conferma, attesa, esito per server) che vivono
                 * dentro ThreadListScreen: ricopiarli qui sarebbe la seconda
                 * copia, cioe' il difetto stesso. Vanno ESTRATTI in un
                 * composable condiviso, e siccome quel percorso mette messaggi
                 * di CONTROLLO sulla rete merita un collaudo suo, non un posto
                 * in coda a una release gia' pronta.
                 *
                 * E' anche la voce giusta per il caso che Roxy aveva descritto
                 * — "un post con troppi errori" — ed e' l'unica delle due che,
                 * secondo le quattro prove sul campo del 6-7 settembre, agisce
                 * davvero fuori dal server su cui si e' pubblicato.
                 */
                if (AppSettings.eMioPost(a.fromField)) {
                    HorizontalDivider()
                    VoceMenuPannello(Icons.Default.Edit, stringResource(R.string.tl_supersede)) {
                        val id = a.id
                        contextItem = null
                        nav.navigate(Routes.composeSupersede(id))
                    }
                    // 1.4.4 (Roxy, 24 settembre 2026): e ORA ANCHE "Ritira".
                    // La ragione per cui nella 110 non era entrato e' stata
                    // onorata invece che aggirata: i tre dialoghi sono stati
                    // ESTRATTI in RitiroArticolo.kt, non ricopiati qui.
                    VoceMenuPannello(Icons.Default.Cancel, stringResource(R.string.tl_cancel_post)) {
                        cancelTarget = a
                        contextItem = null
                    }
                }
            }
        }
    }

    // 1.4.4: gli stessi tre dialoghi della vista a pagine, una volta sola.
    // Si chiama SEMPRE, non dentro un if — vedi il commento della funzione.
    DialoghiRitiro(
        repo = repo,
        bersaglio = cancelTarget,
        onFine = { cancelTarget = null },
        snackbar = snackbar,
    )

    daDisiscrivere?.let { g ->
        // beta20 (Paul): dati letti una volta fuori dai callback (vedi GroupsScreen)
        val id = g.id
        val nome = g.name
        UnsubscribeDialog(
            nomeGruppo = nome,
            messaggiInLocale = 0,
            onDismiss = { daDisiscrivere = null },
            onConferma = { elimina ->
                daDisiscrivere = null
                // se era il gruppo aperto, i pannelli a destra vanno svuotati:
                // resterebbero a mostrare messaggi di un gruppo non piu' in elenco
                if (selectedGroupId == id) {
                    selectedGroupId = null
                    selectedArticleId = null
                }
                scope.launch { repo.unsubscribeGroup(id, elimina) }
            },
        )
    }

    // wishlist beta11 — parità con ThreadListScreen: stessi tre dialog,
    // stesso testo, stesso comportamento. selectedGroupId è nullable qui
    // (nel layout a pagine è sempre valorizzato dalla navigazione), quindi
    // ogni azione controlla che un gruppo sia davvero selezionato.
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
                    val gid = selectedGroupId ?: return@TextButton
                    scope.launch {
                        syncing = true
                        runCatching { repo.downloadAllBodies(gid) }
                            .onSuccess { n -> snackbar.showSnackbar(ctx.getString(R.string.msg_bodies_downloaded, n)) }
                            .onFailure { snackbar.showSnackbar(ctx.getString(R.string.msg_error, it.message ?: "")) }
                        syncing = false
                    }
                }) { Text(stringResource(R.string.common_download)) }
            },
            dismissButton = { TextButton(onClick = { bodiesDialog = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    if (reloadDialog) {
        // 1.4.7 (§A17): spostato in DialogoRiscaricaGruppo.kt. Il `groupId`
        // nullable della funzione estratta copre il vecchio `?: return`.
        DialogoRiscaricaGruppo(
            groupId = selectedGroupId,
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
        val gid = selectedGroupId
        if (gid != null) {
            val g = groups.firstOrNull { it.id == gid }
            RetentionDialog(
                current = g?.purgeDays,
                onDismiss = { retentionDialog = false },
                // beta20 (Paul): chiusura del dialog PRIMA dell'operazione
                // (vedi ThreadListScreen per il dettaglio del crash).
                onPick = { days ->
                    retentionDialog = false
                    scope.launch {
                        repo.setGroupPurgeKeepThreads(gid, null)
                        repo.setGroupPurgeDays(gid, days)
                    }
                },
                currentKeepThreads = g?.purgeKeepThreads,
                onPickKeepThreads = { n ->
                    retentionDialog = false
                    scope.launch {
                        repo.setGroupPurgeKeepThreads(gid, n)
                        if (n != null && n > 0) repo.purgeGroupKeepThreads(gid, n)
                    }
                },
                // 1.4 (Henry The Mole): introduzione alla citazione per gruppo.
                currentIntro = g?.replyIntro,
                onPickIntro = { intro -> scope.launch { repo.setGroupReplyIntro(gid, intro) } },
                // 1.6 (Dave Royal): identità per gruppo.
                identita = identita,
                currentIdentityId = g?.identityId,
                onPickIdentity = { id -> scope.launch { repo.assignIdentityToGroup(gid, id) } },
            )
        } else {
            retentionDialog = false
        }
    }
}

@Composable
private fun PaneDivider() {
    Box(
        Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * 1.3.1 (Dave Royal + Roxy) — BARRA DEL RIQUADRO DI LETTURA NEI PANNELLI.
 *
 * Un solo posto per entrambi i riquadri (vista continua e pannello singolo):
 * prima erano due blocchi identici copiati, e la lezione della 1.3 e' appena
 * costata una release — quattro copie della stessa regola sono quattro
 * occasioni di aggiornarne tre.
 *
 * Le azioni in [AppSettings.paneBarOrder] che NON sono in
 * [AppSettings.paneBarInMenu] diventano icone; le altre finiscono nel menu ⋮,
 * che resta comunque presente perche' ospita anche il plonk. Nessuna funzione
 * sparisce mai: si sposta soltanto.
 */
@Composable
private fun BarraLetturaPannello(
    rot13on: Boolean,
    replyCd: String,
    readFg: Color,
    /**
     * 1.4.6 (§B4, fetta 3): l'UNICA via d'uscita. La barra non sa piu' che cosa
     * facciano le azioni — sa disegnarle e riferire quale e' stata premuta.
     * Chi chiama mette in fila `NavigazioneMessaggi.esegui` e
     * `AzioniLettura.esegui`, che sono definite una volta sola per tutti e due i
     * riquadri.
     */
    onAzione: (String) -> Unit,
    /**
     * Quali azioni sono utilizzabili ADESSO: il primo messaggio non ha un
     * precedente, la vista continua non ha un «inizio messaggio». Un pulsante
     * spento dice la verita'; uno acceso che non fa niente sembra rotto.
     */
    abilitata: (String) -> Boolean,
    /**
     * 1.2.6 (Dave Royal): il plonk sta nel menu da sempre e non e' un'azione di
     * barra — non ha un id nel catalogo e non si puo' spostare. Resta un
     * parametro suo.
     */
    onPlonk: () -> Unit,
) {
    var menuAperto by remember { mutableStateOf(false) }
    val inBarra = AppSettings.paneBarOrder.filter { it !in AppSettings.paneBarInMenu }
    val nelMenu = AppSettings.paneBarOrder.filter { it in AppSettings.paneBarInMenu }

    inBarra.forEach { id ->
        val voce = azioneBarra(id) ?: return@forEach
        IconButton(onClick = { onAzione(id) }, enabled = abilitata(id)) {
            val descrizione = etichettaAzionePannello(id, rot13on, replyCd)
            // L'UNICA azione con una tinta propria: acceso vuol dire «citazioni
            // mute». Le altre prendono il colore del contenuto, cosi' quando
            // sono spente il grigio arriva da solo.
            if (id == "mute_quotes") {
                Icon(
                    voce.icona,
                    contentDescription = descrizione,
                    tint = if (AppSettings.mutaCitazioni) MaterialTheme.colorScheme.primary else readFg,
                )
            } else {
                Icon(voce.icona, contentDescription = descrizione)
            }
        }
    }

    Box {
        IconButton(onClick = { menuAperto = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.art_more_cd))
        }
        DropdownMenu(expanded = menuAperto, onDismissRequest = { menuAperto = false }) {
            // 1.4.6 (§B4, fetta 3): QUI C'ERANO NOVE RAMI, gemelli di quelli qui
            // sopra e con le icone dichiarate a mano — la copia che era
            // sopravvissuta alla fetta 1. Adesso la voce di menu e' il pulsante
            // di barra scritto in un'altra forma, e non c'e' piu' nessun posto
            // dove un'azione possa esistere a meta'.
            nelMenu.forEach { id ->
                val voce = azioneBarra(id) ?: return@forEach
                DropdownMenuItem(
                    text = { Text(etichettaAzionePannello(id, rot13on, replyCd)) },
                    leadingIcon = { Icon(voce.icona, contentDescription = null) },
                    enabled = abilitata(id),
                    onClick = { menuAperto = false; onAzione(id) },
                )
            }
            // 1.2.6 (Dave Royal): il plonk sta nel menu da sempre, e ci resta.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.art_plonk_menu)) },
                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                onClick = { menuAperto = false; onPlonk() },
            )
        }
    }
}

/**
 * L'etichetta di un'azione della barra dei pannelli.
 *
 * Due azioni non possono prenderla dal catalogo e basta, ed e' il motivo per cui
 * questa funzione esiste invece di una lettura diretta: "rot13" cambia testo a
 * seconda di che cosa il tocco FARA' (decodifica o ripristino), e "reply" usa
 * una descrizione gia' pronta che cambia fra i due riquadri. Quando due copie
 * divergono su un dettaglio, il dettaglio diventa un parametro.
 *
 * Serve sia al pulsante (come `contentDescription`) sia alla voce di menu (come
 * testo): e' la stessa parola, e prima era scritta due volte.
 */
@Composable
private fun etichettaAzionePannello(id: String, rot13on: Boolean, replyCd: String): String =
    when (id) {
        "rot13" -> stringResource(if (rot13on) R.string.art_rot13_orig else R.string.art_rot13_dec)
        "reply" -> replyCd
        else -> azioneBarra(id)?.let { stringResource(it.etichetta) } ?: id
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ThreePaneThreadRow(
    item: ThreadItem,
    selected: Boolean,
    scala: Float = 1f,
    onToggle: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    /**
     * 1.3.1 (Roxy): apre il menu contestuale della riga con UN CLIC, invece
     * della sola pressione prolungata.
     *
     * Perché serve: Roxy legge da un Box TV con mini tastiera e trackpad. Il
     * menu a pressione prolungata tecnicamente si apre (tenendo premuto il
     * tasto sinistro), ma — parole sue — «l'operazione sarebbe davvero lenta e
     * poco immediata doverlo fare ogni volta, passerebbe la voglia di usarlo».
     * Su un puntatore la pressione prolungata è un gesto sbagliato: non è
     * impossibile, è impraticabile.
     *
     * Vale anche per chi tocca: la pressione prolungata è invisibile, un ⋮ si
     * vede. La pressione prolungata resta attiva com'era, per chi la usa già.
     */
    onMenu: (() -> Unit)? = null,
    /** beta20 (Bingo3331): tocco su omino+contatore -> vai alle risposte ai miei post. */
    onVaiAlleRisposte: ((ThreadItem) -> Unit)? = null,
    onClick: () -> Unit,
) {
    val article = item.article
    val depth = item.depth
    // a densita' alta si stringe anche il rientro, altrimenti su otto livelli
    // il testo finisce schiacciato contro il bordo destro
    // 1.4.7 (§A15 build 1): passo e rientro vengono da RientroAlbero.kt,
    // insieme alle altre due copie. La resa NON cambia.
    val passo = passoRientro(scala)
    val indent = rientroAlbero(depth, scala)
    val railColor = MaterialTheme.colorScheme.outlineVariant
    // beta18 (Bingo3331): colore dedicato per "risposta a un mio post". Teal:
    // distinto da viola (brand/identità) e giallo (ricerca). Usato sia per la
    // barretta laterale (Piano A) sia per il contatore (Piano B). Catturato qui
    // come val perché drawBehind non è @Composable e non può leggere il tema.
    val rispostaAMeBarColor = RispostaAMeColor
    // Bersaglio del +/-: a scala 1.0 era 32dp, sotto il minimo Material di
    // 48dp e più piccolo ancora del +/- del telefono (36dp) — proprio la
    // schermata che usa Roxy sul TV Box, dove la mira col telecomando a
    // distanza è meno precisa del dito su un telefono in mano. Non scende mai
    // sotto 48dp, qualunque sia la densità scelta in Aspetto → "Dimensione
    // elenco pannelli"; a densità alta (scala grande) continua comunque a
    // crescere con lei.
    val gutter = maxOf(48f, 32f * scala).dp
    // il guadagno di righe a schermo viene per meta' dal carattere e per meta'
    // dai margini: ridurre solo il primo lascia le righe alte come prima
    val padV = (6f * scala).dp
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .drawBehind {
                // 1.4.7 (§A15 build 1): rotaie verticali, una per livello di
                // profondità. Il disegno è estratto in RientroAlbero.kt — qui
                // resta l'UNICA chiamata, come prima: la build 1 non cambia la
                // resa, la build 2 accenderà le rotaie anche altrove.
                disegnaRotaieAlbero(depth, passo, railColor)
                // beta18 (Bingo3331): barretta sul bordo sinistro se questa riga
                // è una risposta diretta a un mio post (Piano A). Usa il campo
                // ESISTENTE article.replyToMe (calcolato al sync). Sobria: barra
                // spessa a sinistra, non sfondo pieno. Teal, distinto da viola
                // (brand) e giallo (ricerca).
                if (item.article.replyToMe) {
                    // beta18 rev.3 (Marco+Bingo3331, opzione 3): sfondo teal
                    // semi-trasparente SOLO se non ancora letto — dice "risposta
                    // a te che non hai ancora letto". Dopo la lettura lo sfondo
                    // scompare; la barretta resta sempre (dice "risposta a te",
                    // indipendentemente dallo stato di lettura). I due layer
                    // dicono cose diverse: barretta = identità, sfondo = urgenza.
                    if (!item.article.read) {
                        drawRect(rispostaAMeBarColor.copy(alpha = 0.12f))
                    }
                    drawLine(
                        rispostaAMeBarColor,
                        Offset(1.5.dp.toPx(), 0f),
                        Offset(1.5.dp.toPx(), size.height),
                        strokeWidth = 3.dp.toPx(),
                    )
                }
            }
            .padding(start = 4.dp + indent, end = 8.dp, top = padV, bottom = padV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            onToggle != null -> IconButton(onClick = onToggle, modifier = Modifier.size(gutter)) {
                Icon(
                    if (item.expanded) Icons.Default.Remove else Icons.Default.Add,
                    contentDescription = if (item.expanded) stringResource(R.string.common_collapse_thread)
                                         else stringResource(R.string.common_expand_thread),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            item.rootKey.isNotEmpty() -> Spacer(Modifier.width(gutter))
            else -> Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // beta14 (Big Umberto): marca le discussioni con un mio post
                if (item.isRoot && item.hasMyPost) {
                    // beta20 (Bingo3331): con risposte presenti, omino+contatore
                    // diventa cliccabile e porta alla prima risposta non letta.
                    val saltoAttivo = item.repliesToMe > 0 && onVaiAlleRisposte != null
                    // beta21 (Bingo3331): il tocco non funzionava perché l'area
                    // cliccabile era l'icona stessa, appena 16dp — troppo piccola
                    // rispetto al minimo tocco (48dp): il dito prendeva la riga
                    // sotto e si apriva il thread dal primo post. Ora l'area
                    // cliccabile è un contenitore di 40dp attorno all'icona, che
                    // resta disegnata a 16dp.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .then(
                                if (saltoAttivo)
                                    Modifier
                                        .clip(CircleShape)
                                        .clickable { onVaiAlleRisposte?.invoke(item) }
                                else Modifier
                            )
                            .size(if (saltoAttivo) 40.dp else 20.dp),
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = if (saltoAttivo) stringResource(R.string.pane_go_to_replies_to_my_messages)
                                                 else stringResource(R.string.common_contains_my_message),
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    // beta18 (Bingo3331): contatore discreto delle risposte dirette
                    // ai miei post in questa discussione (Piano B). Solo se > 0.
                    // 1.4.1 (105/I): l'etichetta sta sull'omino qui sopra, che
                    // dice se il numero sono risposte — vedi ThreadListScreen.
                    if (item.repliesToMe > 0) {
                        Text(
                            "${item.repliesToMe}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RispostaAMeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
                Text(
                    article.subject,
                    style = MaterialTheme.typography.bodyMedium.ridotto(scala),
                    // beta14 (Big Umberto): stato letto sull'intera discussione
                    fontWeight = if (righeNonLette(item, article)) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    color = when {
                        article.hidden -> MaterialTheme.colorScheme.outline
                        article.score >= 100 -> MaterialTheme.colorScheme.primary
                        AppSettings.colorReadState -> readStateColor(!righeNonLette(item, article))
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    // weight(1f): il titolo prende lo spazio RESIDUO della Row, così
                    // il contenitore da 40dp dell'omino non viene compresso dai
                    // soggetti lunghi su due righe. Questo è un beneficio di LAYOUT
                    // reale e va tenuto.
                    //
                    // RETTIFICA (ex beta20): un vecchio commento dichiarava questa la
                    // "causa vera" del tap sull'omino che non funzionava. NON è così:
                    // un test successivo ha smentito la diagnosi, il tap DIRETTO
                    // sull'omino resta inaffidabile. La via che funziona con certezza
                    // è il menu contestuale (pressione lunga sulla riga). Vedi
                    // "Debito tecnico noto" nel documento di stato del progetto.
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                // beta13 (Marco): nell'albero, ora+data piena al posto
                // dell'indirizzo e-mail. Mostro il solo NOME dell'autore
                // (senza <email>) e data+ora insieme (formatThreadDateTime).
                // 1.6.3 / 190 (BIG Umberto): vedi testoAutoreData in ThreadFlatten.
                testoAutoreData(item, article),
                style = MaterialTheme.typography.bodySmall.ridotto(scala),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        // "da leggere / totale" (vedi nota in ThreadListScreen)
        if (item.isRoot && item.total > 0) {
            val tuttoLetto = item.unread == 0
            Badge(
                // beta14 (Big Umberto): rosso (error) quando ci sono non letti
                containerColor = if (tuttoLetto) MaterialTheme.colorScheme.surfaceVariant
                                 else MaterialTheme.colorScheme.error,
                contentColor = if (tuttoLetto) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(start = 6.dp),
            ) { Text(if (tuttoLetto) "${item.total}" else "${item.unread}/${item.total}") }
        }
        // 1.3.1 (Roxy): il ⋮ della riga. Un clic invece di una pressione
        // prolungata. Stessa dimensione del +/- (mai sotto i 48dp Material),
        // e sull'altro bordo: i due comandi della riga stanno agli estremi,
        // il testo in mezzo.
        if (onMenu != null) {
            IconButton(onClick = onMenu, modifier = Modifier.size(gutter)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.art_more_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ArticlePane(
    repo: NewsRepository,
    articleId: Long?,
    threadArticles: List<Article>,
    threadDepths: List<Int>,
    /**
     * 1.4.6 (§B4): i comandi per spostarsi fra i messaggi, in un oggetto solo.
     * Questo composable non li usa: li passa a chi disegna la barra. Un
     * parametro invece di otto, su ognuno dei tre livelli che separano
     * ThreePaneHome dalla barra.
     */
    navigazione: NavigazioneMessaggi,
    onReply: (Long, List<String>?) -> Unit,
    onSelect: (Long) -> Unit,
) {
    if (articleId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.pane_select_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    // beta18 (Roxy): vista continua. Se attiva e il thread ha più di un post,
    // mostro TUTTI i post impilati (LazyColumn: renderizza solo il visibile, così
    // un thread lungo non carica tutto in memoria). Altrimenti la vista singola
    // storica, invariata.
    if (AppSettings.vistaContinua && threadArticles.size > 1) {
        val (readBg, readFg) = readingColors()
        // #5 + R1 (Roxy): la lista si apre posizionata sul messaggio selezionato,
        // con quel messaggio IN CIMA.
        //
        // STORIA. Il #5 impostava `LazyListState(firstVisibleItemIndex = idx)`: ma
        // quel valore vale SOLO alla costruzione dello stato, e lo stato si ricreava
        // su (articleId, size). I corpi però si caricano async cambiando le ALTEZZE
        // senza cambiare né articleId né size → lo stato non si ricreava e l'offset
        // restava stale → il messaggio scivolava a metà/fondo, a volte sull'item
        // sbagliato (indice ricalcolato su un thread ancora in arrivo). Regressione
        // segnalata da Roxy: in landscape il post finiva in posizione casuale.
        //
        // RIMEDIO v3 (unisce le due cose che Roxy voleva: posizione giusta + niente
        // "sussulto"). Un solo scroll (v2) NON bastava: i corpi si caricano async e
        // fanno CRESCERE gli item sopra il target dopo lo scroll, spingendolo giù →
        // posizione "a caso". Qui RI-ANCORIAMO il target in cima con scroll
        // ISTANTANEO (niente animazione) per una breve finestra, finché i corpi
        // vicini non hanno finito di caricare. Quando è già in cima lo scroll è un
        // no-op invisibile: si nota solo l'eventuale piccola correzione. Ci si ferma
        // appena l'utente tocca (isScrollInProgress), per non contrastare lo scroll
        // manuale. Keying su (articleId, threadArticles): ri-posiziona sia a nuova
        // selezione sia se il thread cambia/si riordina mentre arriva.
        val listState = rememberLazyListState()
        // 1.2.5 (Roxy, soluzione A): indice del messaggio selezionato = l'ancora.
        val selIdx = threadArticles.indexOfFirst { it.id == articleId }
        // Guardia anti-conflitto: una volta posizionato (o appena l'utente scorre)
        // NON riposizioniamo più. Serve perché marcare "letto" ri-emette la lista
        // (Flow) → l'effetto di posizionamento ri-scatterebbe e, se l'utente ha già
        // scorso, lo riporterebbe di colpo sull'ancora. Con questa guardia il
        // riposizionamento vive solo nella finestra iniziale.
        var ancorato by remember(articleId) { mutableStateOf(false) }
        LaunchedEffect(articleId, threadArticles) {
            val idx = threadArticles.indexOfFirst { it.id == articleId }
            if (idx < 0) { ancorato = true; return@LaunchedEffect }
            if (ancorato) return@LaunchedEffect
            repeat(15) {
                if (listState.isScrollInProgress) { ancorato = true; return@LaunchedEffect } // utente scorre: smetti e non ricombattere
                val inCima = listState.firstVisibleItemIndex == idx &&
                    listState.firstVisibleItemScrollOffset == 0
                if (!inCima) listState.scrollToItem(idx)
                delay(100)
            }
            ancorato = true // finestra iniziale esaurita: da qui non riposizionare più
        }
        // 1.2.5c (Roxy): marcatura "letto" = ogni messaggio che APPARE a schermo, anche
        // solo PARZIALMENTE. Roxy ha ragione: il criterio "cima del riquadro" lasciava
        // sempre fuori l'ultimo messaggio (non può raggiungere la cima, non ha nulla
        // sotto a spingerlo su). Ora si marcano i post presenti in
        // `layoutInfo.visibleItemsInfo` (che include i parzialmente visibili).
        //
        // La chiave per NON far tornare l'over-marking dei segnaposto: si marca solo
        // ciò che è visibile E ha il CORPO caricato (`body != null`). Un segnaposto in
        // attesa non è "un messaggio che appare", è uno spinner: quindi non conta. In
        // orizzontale, con 1-2 messaggi reali a video, si marcano 1-2 — come descritto.
        //
        // Gate `ancorato`: prima del posizionamento la lista è in cima al thread e
        // mostra i primi post (SOPRA l'ancora); senza gate quelli verrebbero marcati.
        // Dopo il posizionamento l'ancora è in cima e sopra non c'è nulla di visibile.
        //
        // Il download di un corpo (setBody) ri-emette la lista → questo effetto (keyed
        // su threadArticles) si riavvia e ri-valuta: così un post diventato da poco
        // "visibile + con corpo" viene marcato anche se la finestra visibile non è
        // cambiata. Lo scroll è invece catturato dal cambio degli indici visibili.
        val gidThread = threadArticles.getOrNull(selIdx.coerceAtLeast(0))?.groupId
        LaunchedEffect(articleId, threadArticles) {
            if (selIdx < 0 || gidThread == null) return@LaunchedEffect
            snapshotFlow {
                if (!ancorato) emptyList<Int>()
                else listState.layoutInfo.visibleItemsInfo.map { it.index }
            }.collect { visibili ->
                val ids = visibili.mapNotNull { threadArticles.getOrNull(it) }
                    .filter { it.body != null && !it.read }.map { it.id }
                if (ids.isNotEmpty()) repo.setReadBulk(ids, true, gidThread)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(readBg)
                .padding(horizontal = 16.dp),
        ) {
            itemsIndexed(threadArticles, key = { _, post -> post.id }) { index, post ->
                PostContinuo(repo, post, readBg, readFg, onReply, navigazione)
                // 1.2.7 (Dave #1): separatore più spesso quando il post SEGUENTE
                // apre una nuova sotto-discussione (depth <= 1 = risposta diretta
                // all'OP): marca visivamente la fine del sotto-thread precedente.
                // Fra le risposte più profonde resta il separatore sottile.
                val nuovoSottoThread = threadDepths.getOrNull(index + 1)?.let { it <= 1 } == true
                if (nuovoSottoThread) {
                    HorizontalDivider(
                        Modifier.padding(vertical = 10.dp),
                        thickness = 3.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    )
                } else {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
            }
        }
        return
    }
    ArticlePaneSingolo(repo, articleId, threadArticles, onReply, onSelect, navigazione)
}

/**
 * beta18 (Roxy): un singolo post nella vista continua. Carica il proprio corpo
 * in modo indipendente (progressivo: ogni post mostra il progress finché il suo
 * body non arriva), così l'apertura del thread non blocca sul download di tutti.
 */
@Composable
private fun PostContinuo(
    repo: NewsRepository,
    post: Article,
    readBg: androidx.compose.ui.graphics.Color,
    readFg: androidx.compose.ui.graphics.Color,
    onReply: (Long, List<String>?) -> Unit,
    /** 1.4.6 (§B4): vedi ArticlePane. Arriva da ThreePaneHome e va alla barra. */
    navigazione: NavigazioneMessaggi,
) {
    var body by remember(post.id) { mutableStateOf<String?>(null) }
    var error by remember(post.id) { mutableStateOf<String?>(null) }
    // 1.4.5 (par. B9): il selettore di sistema per il .eml, registrato qui
    // perche' un ActivityResultLauncher vive nella composizione.
    val salvaEml = rememberSalvaEml()
    // 1.4.9 (§J3-C1): il selettore di CARTELLA, registrato per la stessa
    // ragione. Si apre una volta sola nella vita dell'installazione.
    val conserva = rememberConserva()
    // #6/#6-ter (parità): header e ROT13 anche nel pannello di lettura.
    var headersRaw by remember(post.id) { mutableStateOf(post.headersRaw) }
    var showHeaders by remember(post.id) { mutableStateOf(false) }
    var rot13on by remember(post.id) { mutableStateOf(false) }
    // 1.2.3 (Dave Royal): la rilevazione crosspost mancava SOLO qui, nella vista
    // continua — il pulsante rispondi chiamava secco onReply(post.id, emptyList()),
    // quindi rispondendo a un post crosspostato NeReO non avvisava e pubblicava
    // solo nel primo gruppo. Portata qui la stessa logica di ArticlePaneSingolo.
    var crosspostWarning by remember(post.id) { mutableStateOf(false) }
    // 1.5.4: il «rispondi» di questo post in un punto solo — lo usano il
    // pulsante della barra e la voce «Cita e rispondi» del menu di selezione.
    val rispondiAlPost: () -> Unit = {
        val crossgroups = crosspostGroups(headersRaw)
        if (crossgroups.size > 1) crosspostWarning = true else onReply(post.id, null)
    }
    var currentGroupName by remember(post.id) { mutableStateOf("") }
    // 1.2.6 (Dave Royal): menu ⋮ per-post nella vista a pannelli (plonk).
    var showPlonk by remember(post.id) { mutableStateOf(false) }
    /*
     * 1.4.6 (§B4, fetta 3): «ricarica» arriva anche nei pannelli.
     *
     * Stesso meccanismo della vista a pagine, non una strada nuova: si azzera il
     * solo CORPO (`invalidaCorpo`) e si cambia questa chiave, che rilancia il
     * LaunchedEffect qui sotto esattamente come farebbe un cambio di articolo.
     *
     * ⚠️ NON si cancella la riga. Era il difetto della 1.4.2: `eliminaCopiaLocale`
     * toglieva l'articolo, e siccome il sync riparte dal segnalibro del gruppo
     * quell'articolo non tornava piu' senza riscaricare tutto il gruppo. Qui
     * nasce gia' corretto perche' copia la versione corretta, non quella
     * originale — ed e' il motivo per cui questa azione andava portata, non
     * riscritta.
     */
    var ricaricaChiave by remember(post.id) { mutableStateOf(0) }
    val scopeRicarica = rememberCoroutineScope()
    LaunchedEffect(post.id, ricaricaChiave) {
        currentGroupName = repo.groupNameById(post.groupId) ?: ""
        // #8: rilancia la cancellazione (scroll veloce), mostra solo errori veri.
        try {
            // 1.2.5 (soluzione A): in vista continua il download NON marca letto;
            // la marcatura è guidata dallo scroll in ArticlePane (solo ciò che
            // arriva davvero in cima al riquadro).
            body = repo.fetchBody(post.id, markRead = false)
            headersRaw = repo.articleById(post.id)?.headersRaw ?: post.headersRaw
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        // 1.4.6 (§A14): la barra non si contende piu' la riga con il testo.
        // Su un riquadro stretto scende sotto invece di schiacciare
        // l'intestazione a un carattere per riga. Vedi IntestazioneConBarra.kt.
        IntestazioneConBarra(
            azioniInBarra = AppSettings.paneBarOrder.count { it !in AppSettings.paneBarInMenu },
            intestazione = { mod ->
                // 1.5.5 (G1 + G3): avatar piccolo e data «oggi / ieri / giorno».
                IntestazioneAutoreCompatta(post.fromField, post.dateEpoch, post.faceB64, headersRaw, readFg, mod)
            },
            barra = {
                /*
                 * 1.4.6 (§B4, fetta 3): «inizio messaggio» e «fine messaggio»
                 * restano NULLI qui, e non e' una dimenticanza.
                 *
                 * Nella vista continua i messaggi scorrono tutti insieme in un
                 * unico elenco: non esiste uno scorrimento DEL SINGOLO post su
                 * cui quei due comandi possano agire. Passarli comunque
                 * significherebbe far saltare l'elenco intero — un'azione che
                 * porta l'utente altrove rispondendo al nome che legge.
                 *
                 * Dichiarandoli nulli, la barra li disegna SPENTI: l'utente vede
                 * che esistono, vede che qui non si usano, e la barra non cambia
                 * forma passando da una vista all'altra.
                 */
                val azioni = azioniLettura(
                    onRot13 = { rot13on = !rot13on },
                    onHeaders = { showHeaders = true },
                    onReply = rispondiAlPost,
                    ricostruisci = ricostruisciDaLettura(repo, post),
                    onSalvaEml = {
                        salvaEml(
                            nomeFileEml(post.subject, post.messageId),
                            articoloEml(headersRaw, if (rot13on) rot13(body.orEmpty()) else body),
                        )
                    },
                    // 1.4.9 (§J3-C1): stesso contenuto di "eml", altra
                    // destinazione — la cartella scelta una volta invece del
                    // selettore a ogni salvataggio. Il nome del file porta anche
                    // il Message-ID, perche' li' i file si accumulano.
                    onConserva = {
                        conserva.conserva(
                            nomeFileConserva(post.subject, post.messageId),
                            articoloEml(headersRaw, if (rot13on) rot13(body.orEmpty()) else body),
                        )
                    },
                    // stesso testo che l'utente ha davanti agli occhi, ROT13 compreso
                    testoMessaggio = { if (rot13on) rot13(body.orEmpty()) else body.orEmpty() },
                    articoloDaEsportare = {
                        articoloGrezzo(headersRaw, if (rot13on) rot13(body.orEmpty()) else body)
                    },
                    messageIdArticolo = { post.messageId },
                    ricaricaCorpo = {
                        scopeRicarica.launch {
                            runCatching { repo.invalidaCorpo(post.id) }
                            ricaricaChiave++
                        }
                    },
                )
                BarraLetturaPannello(
                    rot13on = rot13on,
                    replyCd = stringResource(R.string.pane_reply_to_post),
                    readFg = readFg,
                    onAzione = { id -> if (!navigazione.esegui(id)) azioni.esegui(id) },
                    abilitata = { id -> navigazione.abilitata(id) && azioni.disponibile(id) },
                    onPlonk = { showPlonk = true },
                )
            },
        )
        if (showPlonk) PlonkDialog(repo, post.id, post.fromField) { showPlonk = false }
        // 1.2.5 (Dave Royal): in vista continua mostra i gruppi del crosspost su
        // ogni post — prima li vedevi solo nella vista a singolo articolo. Per
        // preferenza di Dave niente dicitura "⚠ Cross-posted to N groups:": bastano
        // i gruppi in rosso. I nomi dei gruppi sono dati, non testo traducibile.
        RigaGruppiMessaggio(
            headersRaw, currentGroupName, Modifier.padding(top = 2.dp, bottom = 2.dp), soloNomi = true,
        )
        when {
            error != null -> Text(stringResource(R.string.msg_error, error.orEmpty()), color = MaterialTheme.colorScheme.error)
            body == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
            // 1.3.4: il corpo NON e' piu' avvolto da WithSelectionActions.
            // Quel meccanismo sostituiva LocalTextToolbar per aggiungere
            // "Cerca" e "Condividi" al menu di selezione (voce #5 di Dave
            // Royal, 1.2.8). Compose 1.12 non passa piu' da li': ora il menu
            // e' quello della piattaforma, e il nostro veniva ignorato in
            // silenzio. Il file SelectionToolbar.kt e' stato CANCELLATO
            // invece di lasciarlo li' inerte con un commento che diceva di
            // funzionare. Vedi NeReO_1.3.4_migrazione_toolchain.md.
            // 1.5 / 161: il SelectionContainer sta dentro CorpoCitabile
            // (ui/CitaSelezione.kt), che aggiunge «Cita» al menu di selezione:
            // lo stesso, unico punto della lettura a schermo intero.
            else -> CorpoCitabile(
                repo = repo,
                articolo = post,
                corpo = body,
                riconverti = { if (rot13on) rot13(it) else it },
                // 1.5.4: «Cita e rispondi» passa dallo STESSO rispondi del
                // pulsante, col suo avviso sul crosspost.
                onCitaERispondi = rispondiAlPost,
            ) {
                QuoteColoredBody(
                    if (rot13on) rot13(body.orEmpty()) else body.orEmpty(),
                    background = readBg,
                    baseTextColor = readFg,
                    formato = formatoCorpo(headersRaw),
                )
            }
        }
    }
    if (crosspostWarning) {
        val crossgroups = remember(headersRaw) { crosspostGroups(headersRaw) }
        // 1.4.6 (par. A1): il selettore parte dalla richiesta dell'autore, non
        // dal gruppo di lettura. Senza Followup-To i due valori sono identici a
        // prima — le due funzioni sono l'identita' su quel caso.
        val destinazioneAutore = destinazioneRisposta(headersRaw)
        val correntePerDialog = gruppoObbligatorio(
            currentGroupName.ifEmpty { crossgroups.firstOrNull() ?: "" }, destinazioneAutore)
        CrosspostReplyDialog(
            currentGroup = correntePerDialog,
            allGroups = gruppiOffertiNelSelettore(destinazioneAutore, crossgroups),
            gruppiAutore = gruppiChiestiDallAutore(destinazioneAutore),
            onConfirm = { extra ->
                crosspostWarning = false
                onReply(post.id, extra)
            },
            onDismiss = { crosspostWarning = false },
        )
    }
    if (showHeaders) HeaderDialogPannello(headersRaw) { showHeaders = false }
}

/** #6 (parità): dialogo header riutilizzabile nel pannello di lettura, gemello di
 *  quello di ArticleScreen (monospazio, scrollabile, selezionabile). */
@Composable
private fun HeaderDialogPannello(headersRaw: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.art_headers_title)) },
        text = {
            if (headersRaw.isNullOrBlank()) {
                Text(stringResource(R.string.art_headers_none), style = MaterialTheme.typography.bodySmall)
            } else {
                SelectionContainer {
                    Text(
                        nascondiFaceHeaders(headersRaw),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
    )
}

/**
 * 1.2.6 (Dave Royal): dialogo plonk riusabile nel riquadro di lettura a pannelli —
 * prima il plonk era raggiungibile solo dal menu ⋮ della vista telefono
 * (`ArticleScreen`), quindi in tablet mode bisognava cambiare vista. Stessa logica:
 * `repo.plonkAuthor(id, esteso)` con la casella "estendi alle risposte" (default ON).
 */
@Composable
private fun PlonkDialog(repo: NewsRepository, articleId: Long, fromField: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var plonkEsteso by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.art_plonk_title)) },
        text = {
            Column {
                Text(stringResource(R.string.art_plonk_body, fromField.ifBlank { stringResource(R.string.art_this_author) }))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = plonkEsteso, onCheckedChange = { plonkEsteso = it })
                    Text(stringResource(R.string.art_plonk_followup), style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    stringResource(
                        if (plonkEsteso) R.string.art_plonk_followup_on else R.string.art_plonk_followup_off,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val esteso = plonkEsteso
                scope.launch { repo.plonkAuthor(articleId, esteso) }
                onDismiss()
            }) { Text("*plonk*") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun ArticlePaneSingolo(
    repo: NewsRepository,
    articleId: Long?,
    threadArticles: List<Article>,
    onReply: (Long, List<String>?) -> Unit,
    onSelect: (Long) -> Unit,
    /** 1.4.6 (§B4): vedi ArticlePane. Arriva da ThreePaneHome e va alla barra. */
    navigazione: NavigazioneMessaggi,
) {
    if (articleId == null) return
    var article by remember(articleId) { mutableStateOf<Article?>(null) }
    var body by remember(articleId) { mutableStateOf<String?>(null) }
    var error by remember(articleId) { mutableStateOf<String?>(null) }
    // 1.4.5 (par. B9): vedi PostContinuo.
    val salvaEml = rememberSalvaEml()
    // 1.4.9 (§J3-C1): vedi PostContinuo.
    val conserva = rememberConserva()
    // #6/#6-ter (parità): header e ROT13 anche nel pannello singolo
    var showHeaders by remember(articleId) { mutableStateOf(false) }
    var rot13on by remember(articleId) { mutableStateOf(false) }
    // wishlist beta11: stesso avviso crosspost di ArticleScreen (telefono)
    var crosspostWarning by remember(articleId) { mutableStateOf(false) }
    // 1.5.4: il «rispondi» di questo articolo in un punto solo — lo usano il
    // pulsante della barra e la voce «Cita e rispondi» del menu di selezione.
    val rispondiAllArticolo: () -> Unit = {
        article?.let { a ->
            val crossgroups = crosspostGroups(a.headersRaw)
            if (crossgroups.size > 1) crosspostWarning = true else onReply(a.id, null)
        }
    }
    // beta17: nome del gruppo corrente (per il selettore crosspost). L'articolo
    // ha solo groupId; il nome serve a pre-selezionarlo e a marcarlo "questo".
    var currentGroupName by remember(articleId) { mutableStateOf("") }
    // 1.2.6 (Dave Royal): menu ⋮ per-post nella vista a pannelli (plonk).
    var showPlonk by remember(articleId) { mutableStateOf(false) }

    // 1.2.15 (voce B): parità con la vista a pagine (ArticleScreen) — anche nel
    // pannello di lettura, un articolo che sul server non esiste più offre di
    // rimuovere la copia locale rimasta orfana.
    var mancanteId by remember(articleId) { mutableStateOf<Long?>(null) }
    val scopePannello = rememberCoroutineScope()

    /** 1.4.6 (§B4, fetta 3): «ricarica». Vedi la nota estesa in PostContinuo. */
    var ricaricaChiave by remember(articleId) { mutableStateOf(0) }

    LaunchedEffect(articleId, ricaricaChiave) {
        article = repo.articleById(articleId)
        article?.groupId?.let { gid -> currentGroupName = repo.groupNameById(gid) ?: "" }
        // #8: rilancia la cancellazione (scroll veloce), mostra solo errori veri.
        mancanteId = null
        try {
            body = repo.fetchBody(articleId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: org.openusenet.reader.data.ArticoloMancanteException) {
            error = e.message
            mancanteId = e.articleId
        } catch (e: Exception) {
            error = e.message
        }
        article = repo.articleById(articleId) // riflette il flag "letto"
    }

    val (readBg, readFg) = readingColors()
    // 1.2.6 (Dave Royal, Tier1 #6): swipe verticale nel pannello di lettura per
    // saltare al post NON letto successivo/precedente della discussione (se non
    // ce ne sono, al post seguente/precedente). Lo scroll è issato qui perché il
    // rilevatore di gesti deve leggere scroll.value/maxValue per capire se si
    // parte dal bordo — stessa logica del telefono (ArticleScreen), ma senza la
    // navigazione orizzontale fra discussioni (nei pannelli il thread è già
    // selezionato a parte).
    val scroll = rememberScrollState()
    fun prossimoNonLetto(): Long? {
        val idx = threadArticles.indexOfFirst { it.id == articleId }
        if (idx < 0) return null
        val target = threadArticles.drop(idx + 1).firstOrNull { !it.read }
            ?: threadArticles.getOrNull(idx + 1)
        return target?.id
    }
    fun precedenteNonLetto(): Long? {
        val idx = threadArticles.indexOfFirst { it.id == articleId }
        if (idx <= 0) return null
        val target = threadArticles.take(idx).lastOrNull { !it.read }
            ?: threadArticles.getOrNull(idx - 1)
        return target?.id
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(readBg)
            .then(
                if (AppSettings.gesturesEnabled) {
                    Modifier.pointerInput(articleId, threadArticles) {
                        val soglia = 110.dp.toPx()
                        awaitEachGesture {
                            val giu = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val tDown = giu.uptimeMillis
                            var tUp = tDown
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
                            val rapido = (tUp - tDown) < 500
                            val orizzontale = kotlin.math.abs(dx) > kotlin.math.abs(dy)
                            when {
                                !rapido || orizzontale -> {}
                                // in fondo, scorri ancora verso l'alto → post successivo
                                dy < -soglia && partitoDaFondo -> prossimoNonLetto()?.let(onSelect)
                                // in cima, tira verso il basso → post precedente
                                dy > soglia && partitoDaCima -> precedenteNonLetto()?.let(onSelect)
                                else -> {}
                            }
                        }
                    }
                } else Modifier,
            ),
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
    ) {
        article?.let { a ->
            // 1.4.6 (§A14): la barra non si contende piu' la riga con il testo.
            // Su un riquadro stretto scende sotto invece di schiacciare
            // l'intestazione a un carattere per riga. Vedi IntestazioneConBarra.kt.
            IntestazioneConBarra(
                azioniInBarra = AppSettings.paneBarOrder.count { it !in AppSettings.paneBarInMenu },
                intestazione = { mod ->
                    Text(
                    a.subject,
                    style = MaterialTheme.typography.titleMedium,
                    color = readFg,
                    modifier = mod,
                )
                },
                barra = {
                    // 1.4.6 (§B4, fetta 3): qui «inizio» e «fine» ci sono
                    // davvero, perche' questo riquadro ha uno scorrimento suo.
                    // E' l'unica differenza fra i due fasci: tutto il resto e'
                    // la stessa definizione, non una seconda copia.
                    val azioni = azioniLettura(
                        onRot13 = { rot13on = !rot13on },
                        onHeaders = { showHeaders = true },
                        onReply = rispondiAllArticolo,
                        ricostruisci = ricostruisciDaLettura(repo, a),
                        onSalvaEml = {
                            salvaEml(
                                nomeFileEml(a.subject, a.messageId),
                                articoloEml(a.headersRaw, if (rot13on) rot13(body.orEmpty()) else body),
                            )
                        },
                        // 1.4.9 (§J3-C1): vedi la gemella in PostContinuo.
                        onConserva = {
                            conserva.conserva(
                                nomeFileConserva(a.subject, a.messageId),
                                articoloEml(a.headersRaw, if (rot13on) rot13(body.orEmpty()) else body),
                            )
                        },
                        testoMessaggio = { if (rot13on) rot13(body.orEmpty()) else body.orEmpty() },
                        articoloDaEsportare = {
                            articoloGrezzo(a.headersRaw, if (rot13on) rot13(body.orEmpty()) else body)
                        },
                        messageIdArticolo = { a.messageId },
                        vaiInCima = { scopePannello.launch { scroll.animateScrollTo(0) } },
                        vaiInFondo = { scopePannello.launch { scroll.animateScrollTo(scroll.maxValue) } },
                        ricaricaCorpo = {
                            scopePannello.launch {
                                runCatching { repo.invalidaCorpo(a.id) }
                                ricaricaChiave++
                            }
                        },
                    )
                    BarraLetturaPannello(
                        rot13on = rot13on,
                        replyCd = stringResource(R.string.cmp_reply),
                        readFg = readFg,
                        onAzione = { id -> if (!navigazione.esegui(id)) azioni.esegui(id) },
                        abilitata = { id -> navigazione.abilitata(id) && azioni.disponibile(id) },
                        onPlonk = { showPlonk = true },
                    )
                },
            )
            if (showPlonk) PlonkDialog(repo, a.id, a.fromField) { showPlonk = false }
            val crossgroups = remember(a.headersRaw) { crosspostGroups(a.headersRaw) }
            RigaGruppiMessaggio(a.headersRaw, currentGroupName, Modifier.padding(bottom = 4.dp))
            // 1.5.5 (G1 + G3): la stessa intestazione della lettura a schermo
            // intero — qui prima mancava la Face.
            IntestazioneAutore(a.fromField, a.dateEpoch, a.faceB64, a.headersRaw, readFg)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            if (crosspostWarning) {
                // beta17: selettore gruppi. Il gruppo corrente è sempre incluso;
                // l'utente può aggiungerne altri fino al cap. onReply riceve i
                // gruppi EXTRA scelti, che viaggiano nel ComposeTarget.Reply fino
                // a postArticle. Fallback: se il nome del gruppo corrente non è
                // ancora caricato, uso il primo dei crossgroups.
                // 1.4.6 (par. A1): vedi PostContinuo.
                val destinazioneAutore = destinazioneRisposta(a.headersRaw)
                val correntePerDialog = gruppoObbligatorio(
                    currentGroupName.ifEmpty { crossgroups.firstOrNull() ?: "" }, destinazioneAutore)
                CrosspostReplyDialog(
                    currentGroup = correntePerDialog,
                    allGroups = gruppiOffertiNelSelettore(destinazioneAutore, crossgroups),
                    gruppiAutore = gruppiChiestiDallAutore(destinazioneAutore),
                    onConfirm = { extra ->
                        crosspostWarning = false
                        onReply(a.id, extra)
                    },
                    onDismiss = { crosspostWarning = false },
                )
            }
        }
        when {
            error != null -> Column {
                Text(stringResource(R.string.msg_error, error.orEmpty()), color = MaterialTheme.colorScheme.error)
                mancanteId?.let { id ->
                    TextButton(onClick = {
                        scopePannello.launch {
                            runCatching { repo.eliminaArticoloLocale(id) }
                            // Nei pannelli non si "torna indietro": si sposta la
                            // selezione sul messaggio rimasto più vicino. Se la
                            // discussione era fatta solo da questo, il pannello
                            // resta con l'avviso ma senza più il pulsante — la
                            // riga è già sparita dall'elenco accanto.
                            mancanteId = null
                            threadArticles.firstOrNull { it.id != id }?.let { onSelect(it.id) }
                        }
                    }) { Text(stringResource(R.string.art_missing_remove)) }
                }
            }
            body == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
            // beta14 (Dave Royal): nel layout a pannelli il corpo NON era dentro
            // un SelectionContainer, quindi non si poteva selezionare/copiare il
            // testo — mentre nella vista a pagine (ArticleScreen) sì. Allineato.
            // 1.3.4: il corpo NON e' piu' avvolto da WithSelectionActions.
            // Quel meccanismo sostituiva LocalTextToolbar per aggiungere
            // "Cerca" e "Condividi" al menu di selezione (voce #5 di Dave
            // Royal, 1.2.8). Compose 1.12 non passa piu' da li': ora il menu
            // e' quello della piattaforma, e il nostro veniva ignorato in
            // silenzio. Il file SelectionToolbar.kt e' stato CANCELLATO
            // invece di lasciarlo li' inerte con un commento che diceva di
            // funzionare. Vedi NeReO_1.3.4_migrazione_toolchain.md.
            // 1.5 / 161: il SelectionContainer sta dentro CorpoCitabile
            // (ui/CitaSelezione.kt), che aggiunge «Cita» al menu di selezione:
            // lo stesso, unico punto della lettura a schermo intero.
            else -> CorpoCitabile(
                repo = repo,
                articolo = article,
                corpo = body,
                riconverti = { if (rot13on) rot13(it) else it },
                onCitaERispondi = rispondiAllArticolo,
            ) {
                QuoteColoredBody(
                    if (rot13on) rot13(body.orEmpty()) else body.orEmpty(),
                    background = readBg,
                    baseTextColor = readFg,
                    formato = formatoCorpo(article?.headersRaw),
                )
            }
        }
    }
    }
    if (showHeaders) HeaderDialogPannello(article?.headersRaw) { showHeaders = false }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComposePane(
    repo: NewsRepository,
    target: ComposeTarget,
    onSent: () -> Unit,
    onCancel: () -> Unit,
    // 1.4.4: il pannello non aveva modo di dire niente all'utente se non con
    // la riga rossa degli errori di invio, che e' un'altra cosa. Gli serve da
    // quando ha il pulsante Incolla: "appunti vuoti" e' un avviso, non un
    // errore. Lo snackbar e' quello della schermata, non uno suo.
    snackbar: SnackbarHostState,
) {
    val ctx = LocalContext.current
    // 1.4 (Followup-To: poster): copiare l'indirizzo dell'autore. Unico punto
    // appunti dell'app, vedi ui/Appunti.kt.
    val appunti = rememberAppunti()
    val prefs = remember { ctx.getSharedPreferences("identity", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    // RC01: introduzione predefinita alla citazione, letta qui perché
    // stringResource funziona solo in scope @Composable (i punti che la usano
    // sono dentro LaunchedEffect/onClick).
    val introPredefinita = stringResource(R.string.cmp_default_intro)

    // 1.2.9 (multi-identità): come in ComposeScreen, l'identità viene risolta
    // per il server di destinazione appena lo si conosce (LaunchedEffect qui
    // sotto). Prima della 1.2.9 il pannello di composizione NON guardava
    // affatto il server: usava sempre l'identità globale, a differenza della
    // vista a pagine (ComposeScreen) che già rispettava l'override per-server
    // — allineato qui.
    var identity by remember { mutableStateOf<org.openusenet.reader.data.Identity?>(null) }
    var fromName by remember { mutableStateOf("") }
    var fromEmail by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var bodyField by remember { mutableStateOf(TextFieldValue("")) }
    var parent by remember { mutableStateOf<Article?>(null) }
    var replyMode by remember { mutableStateOf(prefs.getString("reply_mode", "below") ?: "below") }
    var modeMenu by remember { mutableStateOf(false) }
    // beta15 (Bingo3331): dialog "Rifinisci citazione"
    var showTrim by remember { mutableStateOf(false) }
    var targetGroupId by remember { mutableStateOf<Long?>(null) }
    // 1.4 (Henry The Mole): introduzione alla citazione impostata su QUESTO
    // gruppo. null/vuota = si usa quella dell'identita'. Vedi introLine().
    var introGruppo by remember { mutableStateOf<String?>(null) }
    var referencesHeader by remember { mutableStateOf<String?>(null) }
    var groupName by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }
    // wishlist beta12 (Roxy, TV Box con minitastiera wifi + Marco: succede
    // anche altrove): il campo Messaggio qui non ha un'altezza massima —
    // cresce libero dentro la Column che scorre, affidandosi al
    // comportamento IMPLICITO di Compose per portare il cursore in vista.
    // Con digitazione veloce (tastiera fisica/wifi, non touch) quel
    // meccanismo resta indietro di un frame rispetto alla nuova altezza
    // del campo: il cursore "sprofonda" sotto lo schermo. Fix: richiesta
    // ESPLICITA di scroll-to-cursor ad ogni carattere digitato, dopo aver
    // atteso un frame perché la nuova altezza sia già stata misurata.
    val corpoInVista = remember { BringIntoViewRequester() }
    // beta17: gruppi extra del crosspost scelti nel selettore, da passare a
    // postArticle. Solo per le risposte; vuoto per i nuovi thread.
    var extraGroups by remember { mutableStateOf<List<String>>(emptyList()) }

    /*
     * 1.4.6 (§A13.2, prove 9B di Dave Royal del 29 settembre 2026) —
     * DICHIARATA QUI, ACCANTO A `extraGroups`, E NON PIU' SESSANTA RIGHE SOTTO.
     *
     * Sono due meta' dello stesso stato: QUALI gruppi extra, e SE quella lista
     * e' una decisione dell'utente o solo un valore di partenza. Stavano
     * lontane, e il difetto nasceva esattamente da li': il `LaunchedEffect` che
     * riceve i gruppi scelti nel pannello dell'articolo non poteva toccare
     * `crosspostScelto`, che ancora non esisteva in quel punto. Cosi' restava
     * falso, `gruppiFollowup` sopravviveva al `takeIf`, e il messaggio usciva
     * dove diceva l'intestazione invece che dove aveva scelto l'utente.
     *
     * Il principio era gia' scritto nella vista a pagine — «una decisione umana
     * non si scavalca con un'intestazione» — ed era implementato a meta'.
     */
    var crosspostScelto by remember { mutableStateOf(false) }

    LaunchedEffect(target) {
        when (target) {
            is ComposeTarget.NewThread -> {
                targetGroupId = target.groupId
                parent = null
                extraGroups = emptyList()
                crosspostScelto = false
            }
            is ComposeTarget.Reply -> {
                // 1.4.6 (§A13.2): la scelta fatta nel pannello dell'articolo
                // arriva fin qui, e da qui in poi VALE. Vedi ComposeTarget.Reply
                // per la differenza fra null e lista vuota.
                extraGroups = target.extraGroups ?: emptyList()
                crosspostScelto = target.extraGroups != null
                val p = repo.articleById(target.articleId)
                if (p != null) {
                    parent = p
                    targetGroupId = p.groupId
                    subject = "Re: " + Threader.normalizeSubject(p.subject)
                    referencesHeader = (p.references + " " + p.messageId).trim()
                }
            }
        }
        targetGroupId?.let {
            val g = repo.groupById(it)
            groupName = g?.name ?: ""
            introGruppo = g?.replyIntro
        }
        val ident = repo.identitaPerGruppo(targetGroupId)
        identity = ident
        fromName = ident.name
        fromEmail = ident.email
        bodyField = buildPrefill(
            ident, if (parent != null) replyMode else "none", parent,
            groupName = groupName,
            defaultIntro = introPredefinita,
            introGruppo = introGruppo,
            // 1.5.4: «Cita e rispondi» dal menu di selezione (ui/Citazione.kt)
            citazioneScelta = parent?.let { CitazioneInAttesa.prendi(it.id) },
        )
        ready = true
    }

    val canSend = !sending && ready && fromName.isNotBlank() && fromEmail.contains("@") &&
        subject.isNotBlank() && bodyField.text.isNotBlank() && targetGroupId != null
    /*
     * 1.4 (Dave Royal, 17 settembre 2026) — LA VISTA A PANNELLI ERA UN VICOLO
     * CIECO SUI GRUPPI DI RISPOSTA.
     *
     *   «I can't tap that x-post icon in panel mode, it's not present. So if I
     *    want to change the replied-to groups I have to abandon the reply and
     *    restart.»
     *
     * Non era una svista di posizionamento: qui l'azione non è mai esistita. In
     * questa vista la scelta dei gruppi si fa PRIMA, nel pannello dell'articolo,
     * e arriva dentro `ComposeTarget.Reply(articleId, extraGroups)`; una volta
     * entrati nel pannello di composizione non c'era modo di tornarci.
     *
     * È la stessa forma di difetto di `esporta` (registrata in tre posti su
     * quattro) e di `radiceDi` (§3-bis della checklist): una funzione applicata
     * ad ALCUNE copie e non a tutte. `ComposePane` è nato dopo `ComposeScreen`,
     * e l'azione non l'ha seguito.
     *
     * `giaScelti = extraGroups` SENZA il null: qui, a differenza della vista a
     * pagine, una scelta c'è sempre già — l'ha fatta il pannello dell'articolo
     * prima di arrivare qui.
     */
    val gruppiOriginale = remember(parent?.headersRaw) { crosspostGroups(parent?.headersRaw) }
    var showCrosspostPane by remember { mutableStateOf(false) }

    /*
     * 1.4.1 (build 108, Dave Royal) — E IL CROSSPOST SU UN THREAD NUOVO?
     *
     *   «Some actions, eg starting a x-posted thread, setting follow-up,
     *    cannot be done in panel mode.»
     *
     * Vero, e verificato nel codice prima di credergli: il selettore qui sopra
     * compare solo `if (gruppiOriginale.size > 1)`, cioe' solo RISPONDENDO a un
     * articolo gia' crospostato. Aprendo una discussione NUOVA quella
     * condizione e' sempre falsa — non c'e' un originale — quindi il pulsante
     * non nasceva mai. Nella vista a pagine invece, dalla beta18, per un thread
     * nuovo il pulsante c'e' SEMPRE.
     *
     * E' la stessa forma di difetto che la 1.4 aveva corretto qui per le
     * RISPOSTE (vedi il commento sopra), tornata sull'altro ramo dello stesso
     * `if`: una funzione applicata ad alcune copie e non a tutte. Sistemata la
     * meta' che Dave aveva segnalato allora, l'altra e' rimasta scoperta.
     *
     * I gruppi offerti sono i SOTTOSCRITTI, come nella vista a pagine: stesso
     * dialogo, stessa sorgente, nessuna terza regola. (Dave ha chiesto anche di
     * poter indicare un gruppo NON sottoscritto: e' un'altra cosa, riguarda il
     * Followup-To, e sta in wishlist — vedi NeReO_dave_followup_to.md.)
     */
    val gruppiSottoscritti by repo.subscribedGroupsWithTotals.collectAsState(initial = emptyList())
    var showCrosspostNuovoPane by remember { mutableStateOf(false) }
    // 1.4 (Followup-To): come in ComposeScreen — la richiesta dell'autore vince
    // sul gruppo di lettura, finche' l'utente non sceglie esplicitamente.
    val destinazioneAutore = remember(parent?.headersRaw) { destinazioneRisposta(parent?.headersRaw) }
    // 1.4.6 (par. A1, punto 4): UNA fonte sola per l'avviso e per il
    // comportamento. Prima il `takeIf` stava qui e l'avviso leggeva il valore
    // grezzo: due righe della stessa schermata potevano smentirsi. Ora la
    // richiesta dell'autore si ritira in UN posto, e tutto cio' che sta a valle
    // legge il risultato.
    val destinazioneInVigore = destinazioneAncoraInVigore(destinazioneAutore, crosspostScelto)
    val gruppiFollowup = (destinazioneInVigore as? DestinazioneRisposta.Gruppi)?.gruppi
    val destinazioni = gruppiFollowup
        ?: gruppiDestinazione(gruppoObbligatorio(groupName, destinazioneAutore), extraGroups)
    val indirizzoAutore = remember(parent?.fromField) {
        parent?.fromField?.let { AppSettings.indirizzoDelFrom(it) }
    }

    if (showCrosspostPane) {
        CrosspostReplyDialog(
            currentGroup = gruppoObbligatorio(groupName, destinazioneAutore),
            allGroups = gruppiOffertiNelSelettore(destinazioneAutore, gruppiOriginale),
            giaScelti = extraGroups,
            gruppiAutore = gruppiChiestiDallAutore(destinazioneAutore),
            onConfirm = { extra -> extraGroups = extra; crosspostScelto = true; showCrosspostPane = false },
            onDismiss = { showCrosspostPane = false },
        )
    }

    if (showCrosspostNuovoPane) {
        CrosspostNuovoThreadDialog(
            currentGroup = groupName,
            subscribedGroups = gruppiSottoscritti.map { it.group.name },
            // Come nella vista a pagine: se una scelta c'e' gia', il selettore
            // riparte da QUELLA. Riaprirlo per controllare non deve cambiare
            // cio' che si sta controllando — difetto trovato da Dave nella 1.4.
            giaScelti = if (crosspostScelto) extraGroups else null,
            onConfirm = { extra ->
                extraGroups = extra
                crosspostScelto = true
                showCrosspostNuovoPane = false
            },
            onDismiss = { showCrosspostNuovoPane = false },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            // beta14 (lamentele tester tastiera invadente + fix Android 10):
            // ComposePane (layout a PANNELLI) NON aveva imePadding(), che invece
            // ComposeScreen (layout a PAGINE) ha da sempre. Per questo un tester
            // notava "a pagine funziona, a pannelli il testo resta sotto la
            // tastiera": mancava qui la gestione che ridimensiona per la
            // tastiera. Allineato a ComposeScreen. Va prima del contenuto così
            // il padding inferiore riduce l'area invece di essere scrollato via.
            //
            // beta15 (Roxy, bug "il testo sparisce su INCOLLA / la selezione si
            // ferma a inizio pagina"): RIMOSSO il .verticalScroll() esterno che
            // stava qui. Causa vera: la Column scrollabile conteneva un campo
            // corpo ANCH'ESSO scrollabile e SENZA weight (cresceva illimitato) —
            // due scroll annidati sullo stesso asse che si contendevano i gesti.
            // Con testo corto non si notava; superata l'altezza visibile (o di
            // colpo con un incolla) entravano in conflitto e il contenuto finiva
            // sotto. Ora la Column NON scorre: è il campo corpo a scorrere dentro
            // di sé grazie a weight(1f), esattamente come in ComposeScreen (che
            // non ha mai avuto questo bug proprio perché strutturata così).
            //
            // 1.5.5 (§K1-bis) — E ADESSO LA COLONNA SCORRE DI NUOVO, MA IL
            // CAMPO HA UN TETTO. E' la correzione della 1.4.9 (§K1) portata
            // nella seconda copia, la stessa struttura verificata sul campo
            // dall'utente del J6 nella vista a pagine. Il campo qui sotto aveva
            // `weight(1f)` + `heightIn(min = 200.dp)`: lo stesso minimo che non
            // e' un minimo, perche' il peso impone un'altezza fissa e il minimo
            // non puo' chiedere piu' del massimo che riceve. E qui nome, email
            // e oggetto sono SEMPRE visibili (non c'e' «Mittente e
            // intestazioni» da chiudere), quindi lo spazio residuo era ancora
            // meno.
            // Il conflitto della beta15 (Roxy) nasceva da un campo SENZA
            // tetto dentro una colonna scorrevole: cresceva illimitato e i due
            // scorrimenti si contendevano i gesti. Con `heightIn(max = 480.dp)`
            // il campo resta limitato e scorre al proprio interno, come nella
            // vista a pagine dalla 1.4.9.
            // ⚠️ Niente `weight` fra i figli di questa colonna: in una colonna
            // scorrevole l'altezza disponibile e' infinita (vedi ComposeScreen).
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (parent != null) stringResource(R.string.cmp_reply) else stringResource(R.string.cmp_new_thread),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            // 1.4.4 (Roxy, 24 settembre 2026) — IL PULSANTE INCOLLA, CHE QUI
            // NON C'ERA MAI STATO. Fuori dal ramo `parent != null` come nella
            // vista a pagine: serve anche aprendo una discussione nuova.
            // La sua storia e il suo perche' stanno in ui/Appunti.kt.
            PulsanteIncolla(
                testo = bodyField,
                onTesto = { bodyField = it },
                appunti = appunti,
                snackbar = snackbar,
            )
            if (parent != null) {
                // beta15 (Bingo3331): rifinisci citazione — solo se ci sono righe
                // citate da togliere nel corpo.
                if (quotedLineIndices(bodyField.text).isNotEmpty()) {
                    IconButton(onClick = { showTrim = true }) {
                        Icon(Icons.Default.ContentCut, contentDescription = stringResource(R.string.cmp_trim_cd))
                    }
                }
                // 1.4 (Dave Royal): l'azione crosspost che in questa vista non
                // c'era. Stessa condizione della vista a pagine — compare solo
                // se l'originale stava davvero su più gruppi.
                // 1.4.6 (par. A1): vedi ComposeScreen — la condizione deve
                // tenere conto anche dei gruppi chiesti dall'autore, altrimenti
                // su un messaggio non crospostato il selettore non compare e
                // l'avviso rimanda a un pulsante che non esiste.
                if (selettoreDisponibile(destinazioneAutore, gruppiOriginale, groupName)) {
                    IconButton(onClick = { showCrosspostPane = true }) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = stringResource(R.string.cmp_crosspost_reply_cd),
                        )
                    }
                }
                // 1.4.4 (Dave Royal): il Box E' L'ANCORA. Senza, DropdownMenu si
                // aggancia al nodo GENITORE (Popup usa parentLayoutCoordinates),
                // e in una Row a tutta larghezza il menu parte dal bordo dello
                // schermo invece che da sotto l'icona.
                Box {
                    IconButton(onClick = { modeMenu = true }) {
                        Icon(Icons.Default.FormatQuote, contentDescription = stringResource(R.string.cmp_quotemode_cd))
                    }
                    DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
                        REPLY_MODES.forEach { (mode, _) ->
                            DropdownMenuItem(
                                text = { Text(etichettaModalitaCitazione(mode)) },
                                leadingIcon = { RadioButton(selected = replyMode == mode, onClick = null) },
                                onClick = {
                                    replyMode = mode
                                    prefs.edit().putString("reply_mode", mode).apply()
                                    identity?.let { ident ->
                                        bodyField = buildPrefill(ident, mode, parent, groupName = groupName, defaultIntro = introPredefinita, introGruppo = introGruppo)
                                    }
                                    modeMenu = false
                                },
                            )
                        }
                    }
                }
            } else {
                // 1.4.1 (build 108, Dave Royal): crosspost anche per un thread
                // NUOVO, come nella vista a pagine dalla beta18. Pulsante sempre
                // visibile, colorato di primario quando dei gruppi extra sono
                // gia' stati scelti. Vedi il commento esteso piu' sopra.
                IconButton(onClick = { showCrosspostNuovoPane = true }) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = stringResource(R.string.cmp_crosspost_new_cd),
                        tint = if (extraGroups.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            LocalContentColor.current,
                    )
                }
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
            Button(
                enabled = canSend,
                onClick = {
                    val gid = targetGroupId
                    if (gid != null) {
                        scope.launch {
                            // 1.2.9: come in ComposeScreen, l'ultimo mittente si
                            // ricorda solo nell'identità DEFAULT, e solo se è
                            // quella in uso (server senza identità dedicata).
                            identity?.let { ident ->
                                if (ident.isDefault && (ident.name != fromName.trim() || ident.email != fromEmail.trim())) {
                                    runCatching { repo.saveIdentity(ident.copy(name = fromName.trim(), email = fromEmail.trim())) }
                                }
                            }
                            sending = true; error = null
                            runCatching {
                                val ident = identity ?: repo.identitaPerGruppo(targetGroupId)
                                repo.postArticle(
                                    gid, fromName.trim(), fromEmail.trim(), subject.trim(),
                                    // 1.6: come la vista a pagine. Qui il corpo
                                    // partiva senza nessun a capo a 72 colonne.
                                    corpoDaInviare(bodyField.text, AppSettings.formatFlowedInUscita),
                                    referencesHeader,
                                    // Correzione 1.2.9 (segnalazione di Marco): stesso fix di
                                    // ComposeScreen.kt — vuoto deve restare vuoto (= anonimo),
                                    // non essere sostituito col default. Vedi il commento
                                    // esteso in ComposeScreen.kt.
                                    userAgent = ident.userAgent,
                                    xNoArchive = ident.xNoArchive,
                                    customHeaders = ident.customHeaders,
                                    faceB64 = ident.faceB64,
                                    extraGroups = extraGroups,
                                    // 1.4: vedi ComposeScreen.
                                    gruppiEspliciti = gruppiFollowup,
                                )
                            }.onSuccess { onSent() }
                                .onFailure { error = it.message }
                            sending = false
                        }
                    }
                },
            ) { Text(stringResource(R.string.cmp_send)) }
        }

        if (sending) LinearProgressIndicator(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        error?.let {
            Text(
                stringResource(R.string.pane_send_failed, it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            // 1.5.5 (voce 15): la stessa riga di spiegazione della vista a
            // pagine, dallo stesso posto (ui/SpiegaRifiuto.kt).
            spiegazioneRifiutoInvio(it)?.let { spiegazione ->
                Text(
                    stringResource(spiegazione),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text(
            // 1.4 (Dave Royal): con più gruppi di destinazione si mostrano tutti.
            // 1.4.6 (§A13.1): stessa etichetta della vista a pagine, da un
            // posto solo - vedi etichettaDestinazioni in CrosspostReply.kt.
            etichettaDestinazioni(destinazioni) +
                (if (parent != null) "  ·  " + etichettaModalitaCitazione(replyMode) else ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        RigaIdentita(repo, identity, fromName, fromEmail)
        // 1.4: l'avviso del Followup-To, identico alla vista a pagine.
        AvvisoFollowupTo(
            destinazione = destinazioneInVigore,
            gruppoCorrente = groupName,
            indirizzoAutore = indirizzoAutore,
            onCopiaIndirizzo = { appunti.copia(it) },
            onApriPosta = { apriAppDiPosta(ctx, it) },
        )
        AvvisoPubblicatoAltrove(
            gruppiDaSegnalare(gruppiOriginale, groupName, destinazioneAutore, destinazioni), destinazioni,
        )
        OutlinedTextField(
            value = fromName, onValueChange = { fromName = it },
            label = { Text(stringResource(R.string.srv_name)) }, singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = fromEmail, onValueChange = { fromEmail = it },
            label = { Text(stringResource(R.string.cmp_email)) }, singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = subject, onValueChange = { subject = it },
            label = { Text(stringResource(R.string.cmp_subject)) }, singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = bodyField,
            onValueChange = { nuovo ->
                bodyField = nuovo
                scope.launch {
                    withFrameNanos { } // aspetta che la nuova altezza sia misurata
                    runCatching { corpoInVista.bringIntoView() }
                }
            },
            label = { Text(stringResource(R.string.cmp_message)) },
            // beta19 (Marco): maiuscola automatica come in ComposeScreen.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = false,
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            // beta15 (Roxy): il corpo scorre DENTRO di sé; niente crescita
            // illimitata dentro una colonna scorrevole (vedi commento in cima).
            // 1.5.5 (§K1-bis): tolto `weight(1f)`, che schiacciava il minimo;
            // minimo e tetto identici alla vista a pagine (1.4.9, §K1).
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 480.dp)
                .bringIntoViewRequester(corpoInVista),
        )
    }

    if (showTrim) {
        TrimQuoteDialog(
            body = bodyField.text,
            onResult = { nuovo -> bodyField = nuovo; showTrim = false },
            onDismiss = { showTrim = false },
        )
    }
}

/**
 * Riduce uno stile di testo di un fattore, lasciando stare i valori non
 * specificati. Moltiplicare direttamente un TextUnit "Unspecified" solleva
 * un'eccezione a tempo di esecuzione, e non tutti gli stili Material
 * dichiarano lineHeight.
 */
private fun TextStyle.ridotto(fattore: Float): TextStyle =
    if (fattore >= 0.999f) this else copy(
        fontSize = if (fontSize.isSpecified) fontSize * fattore else fontSize,
        lineHeight = if (lineHeight.isSpecified) lineHeight * fattore else lineHeight,
    )

/** Voce del menu contestuale a pannello (gemella di quella in ThreadListScreen). */
@Composable
private fun VoceMenuPannello(
    icona: androidx.compose.ui.graphics.vector.ImageVector,
    testo: String,
    onClick: () -> Unit,
) {
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
