/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Schermata principale: gruppi sottoscritti, raggruppati per server con
 * intestazione (stile Piao Hong NewsReader): busta chiusa = messaggi non
 * letti, busta aperta = tutto letto; a destra il conteggio "non letti/totale".
 */
package org.openusenet.reader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.Add
import org.openusenet.reader.ui.icone.KeyboardArrowUp
import org.openusenet.reader.ui.icone.KeyboardArrowDown
import org.openusenet.reader.ui.icone.Dns
import org.openusenet.reader.ui.icone.Drafts
import org.openusenet.reader.ui.icone.Email
import org.openusenet.reader.ui.icone.FilterAlt
import org.openusenet.reader.ui.icone.Info
import org.openusenet.reader.ui.icone.NewReleases
import org.openusenet.reader.ui.StrisciaAggiornato
import org.openusenet.reader.ui.icone.Update
import org.openusenet.reader.ui.icone.Palette
import org.openusenet.reader.ui.icone.Person
import org.openusenet.reader.ui.icone.PowerSettingsNew
import org.openusenet.reader.ui.icone.Search
import org.openusenet.reader.ui.icone.Sync
import org.openusenet.reader.ui.icone.MoreVert
import org.openusenet.reader.ui.icone.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.openusenet.reader.Branding
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
import org.openusenet.reader.ui.playDing
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.ui.Routes
import org.openusenet.reader.ui.components.UnsubscribeDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupsScreen(repo: NewsRepository, nav: NavHostController) {
    val groups by repo.subscribedGroupsWithTotals.collectAsState(initial = emptyList())
    val servers by repo.servers.collectAsState(initial = emptyList())
    val byServer = groups.groupBy { it.group.serverId }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    // beta20 (Marco): conferma per la chiusura esplicita dell'applicazione
    var showChiudiApp by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    var syncing by remember { mutableStateOf(false) }
    // gruppo su cui e' stata chiesta la disiscrizione (pressione prolungata)
    var daDisiscrivere by remember { mutableStateOf<org.openusenet.reader.data.GroupWithTotal?>(null) }
    // gruppo su cui e' stato aperto il menu azioni (pressione prolungata):
    // scarica corpi / annulla sottoscrizione. (richiesta Marco)
    var azioniPer by remember { mutableStateOf<org.openusenet.reader.data.GroupWithTotal?>(null) }
    // 1.4.7 (§A17): gruppo per cui e' stato chiesto il riscarico da zero.
    var daRiscaricare by remember { mutableStateOf<org.openusenet.reader.data.GroupWithTotal?>(null) }

    // 1.4.7 (§A18, Jeff) — RICORDARE DOVE SI ERA NELL'ELENCO DEI GRUPPI.
    //
    // Le sezioni si calcolano UNA VOLTA e servono a due cose: disegnare la
    // lista e sapere che cosa c'e' a ogni riga. Prima `raggruppaPerServer` era
    // chiamata dentro la `LazyColumn`; chiamarla una seconda volta per le
    // chiavi avrebbe creato due risposte alla stessa domanda — la forma di
    // difetto che questo progetto ha gia' pagato undici volte.
    val sezioniGruppi = remember(groups, servers) {
        raggruppaPerServer(groups, servers) { it.group.serverId }
    }
    // L'elenco PIATTO delle chiavi, nello stesso ordine in cui la LazyColumn
    // emette le righe: un'intestazione di server seguita dai suoi gruppi.
    // Deve restare allineato a quello che il blocco `LazyColumn` qui sotto
    // disegna — per questo nasce dalle stesse `sezioniGruppi`.
    val chiaviRighe = remember(sezioniGruppi) {
        buildList {
            sezioniGruppi.forEach { sez ->
                add("srv-${sez.etichetta}")
                sez.elementi.forEach { add("grp-${it.group.id}") }
            }
        }
    }
    val listState = rememberLazyListState()
    var posizioneRipristinata by remember { mutableStateOf(false) }

    // Ripristino: UNA VOLTA SOLA, e solo quando la lista e' gia' popolata.
    // Se la chiave salvata non esiste piu' (gruppo disiscritto, server tolto)
    // si resta in cima: meglio un punto noto che uno arbitrario — e' la stessa
    // regola di ThreadScrollMemory.
    LaunchedEffect(chiaviRighe) {
        if (posizioneRipristinata || chiaviRighe.isEmpty()) return@LaunchedEffect
        GroupScrollMemory.get()?.let { ancora ->
            val idx = chiaviRighe.indexOf(ancora.chiave)
            if (idx >= 0) listState.scrollToItem(idx, ancora.offset)
        }
        posizioneRipristinata = true
    }

    // Salvataggio: si registra QUALE riga e' in cima, non a che indice si era.
    LaunchedEffect(listState, chiaviRighe) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (idx, off) ->
                chiaviRighe.getOrNull(idx)?.let { GroupScrollMemory.put(it, off) }
            }
    }

    // 1.3 (C — aggiornamento selettivo per server/gruppo, richiesta di
    // Bingo3331: vedi NeReO_feature_refresh_selettivo.md). Tre livelli:
    // gruppo (già c'era, long-press → "scarica corpi"), server (menu
    // sull'intestazione) e globale (menu sull'icona in alto). Qui gli stati
    // dei due nuovi livelli.
    // 1.4.4: lo stato del menu dell'icona globale non sta piu' qui — vive
    // dentro AzioneAggiorna, insieme al menu che lo usa.
    var pickServersDialog by remember { mutableStateOf(false) } // "scegli i server"
    var selectedServerIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var serverHeaderMenuFor by remember { mutableStateOf<Long?>(null) } // menu sull'intestazione
    var pickGroupsForServer by remember { mutableStateOf<Long?>(null) } // "scegli i gruppi" (di questo server)
    var selectedGroupIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Involucro comune per ogni sync (era duplicato in tre punti: sync
    // all'apertura, tira-giù, icona in alto — ora anche nei due nuovi livelli
    // selettivi). Stesso comportamento di sempre: indicatore, ding sulle
    // risposte ai miei post, snackbar di esito.
    suspend fun eseguiSync(azione: suspend () -> Int) =
        aggiornaConEsito(repo, ctx, snackbar, { syncing = it }, azione)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Branding.APP_NAME) },
                actions = {
                    // beta19 (Roxy): pulsante di aggiornamento FISSO, alternativa
                    // al gesto "tira giù" che con telecomando/minipad dei TV Box
                    // non funziona bene. Stessa logica dell'icona già aggiunta in
                    // beta16 nel pannello centrale di ThreePaneHome.
                    // 1.3 (C): non aggiorna più tutto al tocco diretto — apre un
                    // menu (tocco lungo sostituito da tocco singolo perché qui è
                    // l'unica azione dell'icona): "aggiorna tutto" (comportamento
                    // storico) oppure "scegli quali server aggiornare".
                    // 1.4.4 (§A7, Dave Royal): icona e menu vivono ora in
                    // AzioneAggiorna.kt, condivisi con la vista a pannelli. Con
                    // un server solo il menu non si apre nemmeno.
                    AzioneAggiorna(
                        abilitata = !syncing,
                        numeroServer = servers.size,
                        // stessa resa del "tira giù": conteggio nuovi articoli
                        // (e ding) anche premendo l'icona, prima mancava — la
                        // striscia compariva solo col gesto.
                        onAggiornaTutto = { scope.launch { eseguiSync { repo.syncAllSubscribed() } } },
                        onScegliServer = {
                            selectedServerIds = servers.map { it.id }.toSet() // comodo: parte da "tutti"
                            pickServersDialog = true
                        },
                    )
                    IconButton(onClick = { nav.navigate(Routes.SEARCH) }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.groups_search))
                    }
                    var menu by remember { mutableStateOf(false) }
                    // 1.2.6 (Marco): controllo aggiornamenti manuale dal menu principale.
                    var checkAggiornamenti by remember { mutableStateOf(false) }
                    org.openusenet.reader.ui.ControlloAggiornamentiManuale(checkAggiornamenti) { checkAggiornamenti = false }
                    // 1.4.4 (Dave Royal): il Box E' L'ANCORA. Senza, DropdownMenu si
                    // aggancia al nodo GENITORE (Popup usa parentLayoutCoordinates),
                    // e in una Row a tutta larghezza il menu parte dal bordo dello
                    // schermo invece che da sotto l'icona.
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_more))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_servers)) },
                                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                                onClick = { menu = false; nav.navigate(Routes.SERVERS) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_filters)) },
                                leadingIcon = { Icon(Icons.Default.FilterAlt, contentDescription = null) },
                                onClick = { menu = false; nav.navigate(Routes.FILTERS) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_identity)) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                onClick = { menu = false; nav.navigate(Routes.IDENTITY) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_appearance)) },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                onClick = { menu = false; nav.navigate(Routes.APPEARANCE) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_maintenance)) },
                                leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                                onClick = { menu = false; nav.navigate(Routes.MAINTENANCE) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about_title)) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = { menu = false; nav.navigate(Routes.ABOUT) },
                            )
                            // 1.2.9 (richiesta di Marco, Piano C): "Bozze" non sta più
                            // qui nel menu generale — le bozze sono sempre legate a UN
                            // gruppo (Draft.groupId), quindi l'ingresso naturale è
                            // dentro il gruppo a cui si sta scrivendo (menu ⋮ di
                            // ThreadListScreen/ThreePaneHome), dove la lista parte già
                            // filtrata su quel gruppo. Resta comunque possibile vedere
                            // TUTTE le bozze di tutti i gruppi: c'è un interruttore
                            // "tutti i gruppi" dentro la schermata stessa (vedi
                            // DraftsScreen) — nessuna funzionalità persa, solo un
                            // secondo punto d'accesso in meno nel menu principale.
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.releasenotes_title)) },
                                leadingIcon = { Icon(Icons.Default.NewReleases, contentDescription = null) },
                                onClick = { menu = false; nav.navigate(Routes.RELEASE_NOTES) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.upd_check_now)) },
                                leadingIcon = { Icon(Icons.Default.Update, contentDescription = null) },
                                onClick = { menu = false; checkAggiornamenti = true },
                            )
                            // beta20 (Marco): chiusura esplicita. Il tasto Indietro
                            // lascia l'app in background; qui si esce davvero e il
                            // task viene tolto dai recenti, liberando la memoria.
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.quit_menu)) },
                                leadingIcon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null) },
                                onClick = { menu = false; showChiudiApp = true },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            // beta16 (Roxy): FAB dentro l'area sicura in landscape (vedi
            // ServersScreen per il dettaglio della causa edge-to-edge).
            FloatingActionButton(
                onClick = {
                    if (servers.isEmpty()) nav.navigate(Routes.SERVERS)
                    else nav.navigate(Routes.DIRECTORY)
                },
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.groups_subscribe))
            }
        },
    ) { padding ->
      Column(Modifier.padding(padding)) {
        // beta14 (Bingo3331): sync automatico all'apertura dell'app, se attivo
        // in Impostazioni. La guardia syncOnStartDone (flag di sessione) fa sì
        // che avvenga UNA sola volta per avvio, non a ogni ritorno alla lista.
        LaunchedEffect(Unit) {
            if (AppSettings.syncOnStart && !AppSettings.syncOnStartDone) {
                AppSettings.syncOnStartDone = true
                eseguiSync { repo.syncAllSubscribed() }
            }
        }
        // 1.5.8: «Aggiornato a <versione> — Novita'», una volta per versione.
        // Stesso componente della colonna gruppi dei pannelli: ui/StrisciaAggiornato.kt.
        StrisciaAggiornato(nav)
        if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
        PullToRefreshBox(
            isRefreshing = syncing,
            onRefresh = { scope.launch { eseguiSync { repo.syncAllSubscribed() } } },
        ) {
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            sezioniGruppi.forEach { sez ->
                val serverGroups = sez.elementi
                item(key = "srv-${sez.etichetta}") {
                    // 1.3 (C): l'intestazione del server porta anche il suo menu
                    // di aggiornamento selettivo — stesso posto naturale dove il
                    // gruppo ha già "scarica corpi" al long-press.
                    //
                    // 1.4.4 (§A9): la riga in sé è uscita di qui e sta in
                    // ui/screens/IntestazioneServer.kt, perché serviva identica
                    // anche alla colonna dei pannelli. Qui resta solo ciò che è
                    // davvero di questa vista: il menu. E l'etichetta ora è il
                    // NOME del server, non l'host — vedi etichettaServer().
                    //
                    // Il raggruppamento passa per la stessa funzione: prima era
                    // un servers.forEach scritto qui, che faceva SPARIRE dalla
                    // lista i gruppi il cui server non esiste più. Ora finiscono
                    // in coda sotto "?", senza menu perché non c'è niente da
                    // sincronizzare — si tolgono dalla pulizia orfani.
                    IntestazioneServer(sez.etichetta) {
                        val server = sez.server ?: return@IntestazioneServer
                        Box {
                            IconButton(
                                enabled = !syncing,
                                onClick = { serverHeaderMenuFor = server.id },
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = stringResource(R.string.sel_refresh_server_cd, server.host),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            DropdownMenu(
                                expanded = serverHeaderMenuFor == server.id,
                                onDismissRequest = { serverHeaderMenuFor = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sel_refresh_server_all)) },
                                    leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                                    onClick = {
                                        serverHeaderMenuFor = null
                                        scope.launch { eseguiSync { repo.syncServer(server.id) } }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sel_refresh_server_choose)) },
                                    leadingIcon = { Icon(Icons.Default.FilterAlt, contentDescription = null) },
                                    onClick = {
                                        serverHeaderMenuFor = null
                                        selectedGroupIds = serverGroups.map { it.group.id }.toSet() // parte da "tutti quelli di questo server"
                                        pickGroupsForServer = server.id
                                    },
                                )
                            }
                        }
                    }
                }
                items(serverGroups, key = { it.group.id }) { gt ->
                    val g = gt.group
                    val hasUnread = g.unreadCount > 0
                    ListItem(
                        leadingContent = {
                            Icon(
                                if (hasUnread) Icons.Default.Email else Icons.Default.Drafts,
                                contentDescription = if (hasUnread) stringResource(R.string.groups_unread) else stringResource(R.string.groups_all_read),
                                tint = if (hasUnread) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.outline,
                            )
                        },
                        headlineContent = {
                            // 1.4.10 (par. A11-bis, la coda) — IL COMPONENTE CONDIVISO
                            // USATO ANCHE QUI.
                            //
                            // Questo era un `Text` nudo mentre a riga ~660, nello
                            // STESSO FILE, i nomi di gruppo passano gia' da
                            // NomeGruppoAdattivo. Era rimasto fuori per una ragione
                            // tecnica e non di prodotto: serviva il grassetto per i
                            // gruppi con messaggi non letti, e il componente non ha un
                            // parametro `fontWeight`.
                            //
                            // Non gliene serve uno: ha gia' `style`, e il grassetto e'
                            // un attributo dello stile. *Prima di aggiungere un
                            // parametro a un componente condiviso, guardare se quello
                            // che c'e' lo esprime gia'.*
                            //
                            // Cosa cambia per chi guarda: un nome di gruppo troppo
                            // lungo adesso si abbrevia invece di finire in «...», e
                            // SOLO quando davvero non ci sta.
                            NomeGruppoAdattivo(
                                nome = g.name,
                                style = LocalTextStyle.current.copy(
                                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                                ),
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // beta13 (robustezza segnale non-letti, indagato con
                                // Marco): con Material You attivo `primary` può venire
                                // smorto e il "non letto" si distingueva poco. Il conteggio
                                // dei NON letti ora è un Badge pieno (sfondo colorato +
                                // testo su colore garantito), canale che regge anche con
                                // palette generata dal sistema. Il totale resta testo
                                // tenue accanto. Grassetto sul nome + icona busta restano
                                // come ulteriore ridondanza.
                                if (hasUnread) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    ) { Text("${g.unreadCount}") }
                                    Text(
                                        "/${gt.totalCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 2.dp),
                                    )
                                } else {
                                    Text(
                                        "${g.unreadCount}/${gt.totalCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                // riordino manuale (wishlist beta11): nessun controllo sui bordi
                                // qui in UI, ci pensa Repository.moveGroup a non fare nulla se
                                // sei già primo/ultimo
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { nav.navigate(Routes.threads(g.id)) },
                                onLongClick = { azioniPer = gt },
                            ),
                    )
                }
            }
            if (groups.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.groups_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            if (groups.isEmpty()) {
                item {
                    Text(
                        if (servers.isEmpty())
                            stringResource(R.string.groups_welcome)
                        else
                            stringResource(R.string.groups_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
        }
      }
    }

    if (showChiudiApp) {
        AlertDialog(
            onDismissRequest = { showChiudiApp = false },
            title = { Text(stringResource(R.string.quit_title)) },
            text = {
                Text(
                    stringResource(R.string.quit_body),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showChiudiApp = false
                    (ctx as? android.app.Activity)?.finishAndRemoveTask()
                }) { Text(stringResource(R.string.quit_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showChiudiApp = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    // Menu azioni al long-press (richiesta Marco): scarica corpi degli ultimi
    // 300 messaggi (riempie i buchi: la query prende i piu' recenti senza
    // corpo) oppure prosegui verso la disiscrizione col dialogo di sempre.
    azioniPer?.let { gt ->
        val gid = gt.group.id
        val gnome = gt.group.name
        AlertDialog(
            onDismissRequest = { azioniPer = null },
            title = { Text(gnome) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            azioniPer = null
                            scope.launch {
                                val n = repo.downloadAllBodies(gid, limit = 300)
                                snackbar.showSnackbar(ctx.getString(R.string.groups_bodies_done, n))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.groups_download_bodies), modifier = Modifier.fillMaxWidth()) }
                    // 1.4.7 (§A17, Jeff) — IL COMANDO DOVE ABITA LA SUA FAMIGLIA.
                    //
                    // Jeff, 5 ottobre: «I should have been able to find the
                    // "Re-download group from server", although I expected that
                    // to be a setting in the list of server newsgroups». Si era
                    // scusato; non doveva. Qui c'erano gia' «scarica corpi» e
                    // «disiscrivi», e l'intestazione di ogni server ha gia'
                    // «aggiorna tutti i gruppi»: l'unico comando di quella
                    // famiglia che stava altrove — nel menu ⋮ DENTRO al gruppo —
                    // era proprio il riscarico. L'anomalia era il comando, non
                    // il posto in cui lui ha guardato.
                    //
                    // Sta in mezzo, e l'ordine e' voluto: «disiscrivi» resta
                    // l'ultima perche' e' la piu' drastica delle tre (toglie il
                    // gruppo, non solo i messaggi). Qui si va dal meno grave al
                    // piu' grave: scarica, riscarica, disiscrivi.
                    TextButton(
                        onClick = {
                            azioniPer = null
                            daRiscaricare = gt
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.common_redownload_group_menu), modifier = Modifier.fillMaxWidth()) }
                    TextButton(
                        onClick = {
                            azioniPer = null
                            daDisiscrivere = gt
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.groups_unsub_action), modifier = Modifier.fillMaxWidth()) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { azioniPer = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    // 1.4.7 (§A17): lo stesso dialogo delle altre due schermate, non una copia.
    daRiscaricare?.let { gt ->
        DialogoRiscaricaGruppo(
            groupId = gt.group.id,
            repo = repo,
            // Lo scope e' QUELLO DELLA SCHERMATA, non del dialogo:
            // vedi il blocco di commento in DialogoRiscaricaGruppo.kt
            // — e' il difetto della build 135.
            scope = scope,
            snackbar = snackbar,
            onChiudi = { daRiscaricare = null },
            onSyncing = { syncing = it },
        )
    }

    // 1.3 (C): "scegli quali server aggiornare" (livello globale). Selezione
    // multipla di server, un giro solo (1.6.1) — niente "dedup" fra server che
    // condividono un gruppo (scelta deliberata, vedi
    // NeReO_feature_refresh_selettivo.md).
    if (pickServersDialog) {
        AlertDialog(
            onDismissRequest = { pickServersDialog = false },
            title = { Text(stringResource(R.string.sel_pick_servers_title)) },
            text = {
                Column {
                    servers.forEach { server ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedServerIds = if (server.id in selectedServerIds)
                                        selectedServerIds - server.id else selectedServerIds + server.id
                                },
                        ) {
                            Checkbox(
                                checked = server.id in selectedServerIds,
                                onCheckedChange = {
                                    selectedServerIds = if (it) selectedServerIds + server.id else selectedServerIds - server.id
                                },
                            )
                            Text(server.host)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedServerIds.isNotEmpty(),
                    onClick = {
                        pickServersDialog = false
                        val ids = selectedServerIds
                        scope.launch { eseguiSync { repo.syncServers(ids) } }
                    },
                ) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickServersDialog = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    // 1.3 (C): "scegli quali gruppi aggiornare" (livello server, dal menu
    // sull'intestazione).
    pickGroupsForServer?.let { serverId ->
        val serverGroups = byServer[serverId].orEmpty()
        val serverHost = servers.firstOrNull { it.id == serverId }?.host ?: ""
        AlertDialog(
            onDismissRequest = { pickGroupsForServer = null },
            title = { Text(stringResource(R.string.sel_pick_groups_title, serverHost)) },
            text = {
                Column {
                    serverGroups.forEach { gt ->
                        val g = gt.group
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedGroupIds = if (g.id in selectedGroupIds)
                                        selectedGroupIds - g.id else selectedGroupIds + g.id
                                },
                        ) {
                            Checkbox(
                                checked = g.id in selectedGroupIds,
                                onCheckedChange = {
                                    selectedGroupIds = if (it) selectedGroupIds + g.id else selectedGroupIds - g.id
                                },
                            )
                            // 1.4.7 (§A11-bis): anche nel selettore, dove la riga
                            // e' una sola e accanto c'e' una casella di spunta.
                            NomeGruppoAdattivo(nome = g.name, righe = 1)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedGroupIds.isNotEmpty(),
                    onClick = {
                        pickGroupsForServer = null
                        val ids = selectedGroupIds.toList()
                        scope.launch { eseguiSync { repo.syncGroups(ids) } }
                    },
                ) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickGroupsForServer = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    daDisiscrivere?.let { gt ->
        // beta20 (Paul): i dati del gruppo si leggono UNA VOLTA fuori dai
        // callback. Prima `gt` veniva letto dentro onConferma: se nel frattempo
        // il Flow dei gruppi riemetteva (sync, cancellazione in corso), il
        // riferimento poteva riferirsi a un elemento non piu' valido. Insieme
        // alla chiusura del dialog prima dell'operazione, toglie la finestra in
        // cui si verificava il crash su Android 10.
        val gid = gt.group.id
        val gnome = gt.group.name
        val gtot = gt.totalCount
        UnsubscribeDialog(
            nomeGruppo = gnome,
            messaggiInLocale = gtot,
            onDismiss = { daDisiscrivere = null },
            onConferma = { elimina ->
                daDisiscrivere = null
                scope.launch { repo.unsubscribeGroup(gid, elimina) }
            },
        )
    }
}
