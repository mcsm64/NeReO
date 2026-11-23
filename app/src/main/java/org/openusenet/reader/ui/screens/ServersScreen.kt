/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.ArrowBack
import org.openusenet.reader.ui.icone.Add
import org.openusenet.reader.ui.icone.Lock
import org.openusenet.reader.ui.icone.Delete
import org.openusenet.reader.ui.icone.Edit
import org.openusenet.reader.ui.icone.Visibility
import org.openusenet.reader.ui.icone.VisibilityOff
import org.openusenet.reader.ui.icone.ExpandMore
import org.openusenet.reader.ui.icone.KeyboardArrowUp
import org.openusenet.reader.ui.icone.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.openusenet.reader.R
import org.openusenet.reader.data.Identity
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.data.Server
import org.openusenet.reader.data.ServerPresets
import org.openusenet.reader.ui.indietroSicuro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(repo: NewsRepository, nav: NavHostController) {
    val servers by repo.servers.collectAsState(initial = emptyList())
    // 1.4: giorni alla scadenza per ciascun server, dall'ultima identita' TLS
    // osservata. Mappa vuota finche' non ci si e' mai collegati in cifrato.
    var scadenze by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    LaunchedEffect(servers) {
        scadenze = repo.pinTlsTutti().associate {
            it.serverId to (it.notAfter - System.currentTimeMillis()) / 86_400_000L
        }
    }
    // 1.2.9: elenco identità per il selettore nel dialog di modifica server.
    val identities by repo.identities.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Server?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    /**
     * 1.4.2 (build 113): il server che si sta per cancellare, con quanti
     * gruppi e quanti messaggi se ne andrebbero con lui. Null = nessuna
     * richiesta in corso. I due numeri si leggono PRIMA di chiedere, perche'
     * "vuoi cancellare?" e "vuoi cancellare 6.412 messaggi?" sono due domande
     * diverse.
     */
    var daCancellare by remember { mutableStateOf<Triple<Server, Int, Int>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.srv_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.indietroSicuro() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        floatingActionButton = {
            // beta16 (Roxy, TV Box + Redmi landscape): con edge-to-edge attivo
            // (beta14, setDecorFitsSystemWindows=false) e senza gestione insets,
            // in landscape la barra di navigazione va a destra e copriva il FAB,
            // rendendolo non toccabile → impossibile aggiungere server/gruppi.
            // navigationBarsPadding lo tiene dentro l'area sicura in ogni
            // orientamento.
            FloatingActionButton(
                onClick = { editing = null; showDialog = true },
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.srv_add))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(servers, key = { it.id }) { server ->
                ListItem(
                    headlineContent = { Text(server.name) },
                    supportingContent = {
                        Column {
                            Text(
                                "${server.host}:${server.port}" + (if (server.ssl) stringResource(R.string.srv_ssl_suffix) else "") +
                                    stringResource(R.string.srv_tap_edit)
                            )
                            // 1.2.17 (voce C) — SEGNO PERMANENTE, non un avviso
                            // ricorrente. Compare solo dopo che ci si è
                            // collegati almeno una volta, e dice la cifratura
                            // OTTENUTA, non quella richiesta: la configurazione
                            // di OVH dichiara STARTTLS, il server lo annuncia, e
                            // poi risponde "580 Error initializing TLS". Chi
                            // guardasse le impostazioni lo crederebbe cifrato.
                            // 1.4: la scadenza del certificato accanto al
                            // nome del server. Su un server a rinnovo MANUALE
                            // (Paganini) questo previene la caduta invece di
                            // attutirla: il dato ce l'abbiamo gia', l'ha
                            // registrato il TrustManager all'ultimo
                            // collegamento.
                            scadenze[server.id]?.let { giorni ->
                                if (giorni <= TLS_GIORNI_AVVISO) {
                                    Text(
                                        stringResource(R.string.srv_cert_expiring, giorni),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            if (org.openusenet.reader.data.StatoCifratura.cifrata[server.id] == false) {
                                Text(
                                    stringResource(R.string.srv_cleartext_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            // par. B3 (1.4.11, ObiWan) — RFC 4642 par. 5: questo
                            // server STARTTLS lo offriva, e adesso non lo
                            // annuncia piu'. Va accanto alla riga «non cifrato»
                            // e non al posto suo: quella dice lo STATO, questa
                            // dice il CAMBIAMENTO, e il cambiamento e' l'unica
                            // cosa che distingue un provider senza TLS da
                            // qualcuno che sta in mezzo.
                            if (org.openusenet.reader.data.StatoCifratura.starttlsSparito[server.id] == true) {
                                Text(
                                    stringResource(R.string.srv_starttls_alarm),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    },
                    trailingContent = {
                        Row {
                            // riordino manuale (wishlist beta11): Repository.moveServer non fa
                            // nulla se il server è già primo/ultimo, niente controlli qui in UI
                            IconButton(onClick = { scope.launch { repo.moveServer(server.id, up = true) } }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.groups_move_up))
                            }
                            IconButton(onClick = { scope.launch { repo.moveServer(server.id, up = false) } }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.groups_move_down))
                            }
                            // 1.4 (TOFU): l'identita' TLS di QUESTO server, dove
                            // la fiducia si concede — mai in mezzo a un sync.
                            IconButton(onClick = {
                                nav.navigate("tls_identity/${server.id}")
                            }) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.tls_title),
                                )
                            }
                            IconButton(onClick = { editing = server; showDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit))
                            }
                            // 1.4.2 (build 113): non si cancella piu' un server
                            // con un tocco. Vedi il dialogo in fondo al file.
                            IconButton(onClick = {
                                val s = server
                                scope.launch {
                                    val (g, a) = repo.costoCancellazioneServer(s.id)
                                    daCancellare = Triple(s, g, a)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { editing = server; showDialog = true },
                )
            }
            if (servers.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.srv_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }

    if (showDialog) {
        ServerDialog(
            initial = editing,
            identities = identities,
            onDismiss = { showDialog = false },
            onSave = { s, proxy ->
                scope.launch {
                    // upsert su un server esistente non ridà l'id: si usa quello noto
                    val nuovo = repo.saveServer(s)
                    org.openusenet.reader.AppSettings.salvaProxy(if (s.id != 0L) s.id else nuovo, proxy)
                }
                showDialog = false
            },
        )
    }

    /*
     * 1.4.2 (build 113) — CONFERMA PRIMA DI CANCELLARE UN SERVER.
     *
     * Fino alla 112: un tocco sul cestino e il server spariva, senza domande.
     * E spariva male, perche' [NewsRepository.deleteServer] toglieva la sola
     * riga `servers` e lasciava indietro gruppi, articoli, catalogo e pin —
     * invisibili e irraggiungibili per sempre. Ora la cancellazione porta via
     * cio' che le appartiene (ed e' irreversibile davvero), quindi la domanda
     * serve il doppio, e deve contenere i NUMERI: "cancellare questo server?"
     * e "cancellare questo server e 6.412 messaggi?" sono due domande diverse,
     * e solo la seconda permette di rispondere.
     */
    daCancellare?.let { (server, gruppi, articoli) ->
        AlertDialog(
            onDismissRequest = { daCancellare = null },
            title = { Text(stringResource(R.string.srv_delete_title, server.name.ifBlank { server.host })) },
            text = {
                Text(
                    stringResource(R.string.srv_delete_body, gruppi, articoli),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = server.id
                    daCancellare = null
                    scope.launch { repo.eliminaServerEContenuti(id) }
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { daCancellare = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ServerDialog(
    initial: Server?,
    identities: List<Identity>,
    onDismiss: () -> Unit,
    onSave: (Server, Triple<Boolean, String, Int>) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var port by remember { mutableStateOf((initial?.port ?: 119).toString()) }
    var ssl by remember { mutableStateOf(initial?.ssl ?: false) }
    var user by remember { mutableStateOf(initial?.username ?: "") }
    // in modifica NON pre-riempio la password (nel DB è cifrata): vuota = invariata
    var pass by remember { mutableStateOf("") }
    var presetMenu by remember { mutableStateOf(false) }
    // 1.2.9 (voce E, roadmap NNTP + multi-identità)
    var identityId by remember { mutableStateOf(initial?.identityId) }
    var identityMenu by remember { mutableStateOf(false) }
    var startTls by remember { mutableStateOf(initial?.startTls ?: false) }
    var authOverride by remember { mutableStateOf(initial?.authOverrideNonCifrata) }
    // 1.6.5 / 193: proxy SOCKS5 (Tor, Nym), spento di default. Vedi nntp/Socks5.kt.
    val proxySalvato = remember { org.openusenet.reader.AppSettings.proxyImpostato(initial?.id ?: -1L) }
    var proxyOn by remember { mutableStateOf(proxySalvato.first) }
    var proxyHost by remember { mutableStateOf(proxySalvato.second) }
    var proxyPort by remember { mutableStateOf(proxySalvato.third.toString()) }

    fun salva() {
        onSave(
            Server(
                id = initial?.id ?: 0,
                name = name.ifBlank { host },
                host = host.trim(),
                port = port.toIntOrNull() ?: 119,
                ssl = ssl,
                // 1.3.5: ripulito come gia' l'host qui sopra. Un nome utente
                // con spazi ai bordi non esiste, e uno spazio arrivato da un
                // incolla o da un suggerimento della tastiera veniva spedito
                // al server, che lo contava: 481 senza alcun indizio.
                // La password, invece, NON si tocca — vedi il commento sul
                // suo campo.
                username = user.trim().ifBlank { null },
                // vuota in modifica = mantieni il blob cifrato esistente (encrypt è idempotente)
                password = when {
                    pass.isNotBlank() -> pass
                    initial != null -> initial.password
                    else -> null
                },
                sortOrder = initial?.sortOrder ?: 0,
                identityId = identityId,
                startTls = startTls,
                authOverrideNonCifrata = authOverride,
            ),
            Triple(
                proxyOn,
                proxyHost.trim(),
                proxyPort.toIntOrNull() ?: org.openusenet.reader.AppSettings.PROXY_PORTA_PREDEFINITA,
            ),
        )
    }

    // beta18 (Roxy): il vecchio AlertDialog rompeva l'inserimento con la tastiera
    // (scatti in portrait, tastiera che cannibalizza tutto in landscape + conferma
    // fantasma). Un AlertDialog Material3 vive in finestra separata e non gestisce
    // l'IME, tantomeno in landscape. Piano C: Dialog a SCHERMO INTERO
    // (usePlatformDefaultWidth=false) con Scaffold + imePadding, che gestisce la
    // tastiera come una schermata normale. Stato e salvataggio invariati rispetto
    // a prima: cambia solo il contenitore.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // beta19 (Roxy): senza questo la finestra-dialog NON riceve gli insets
            // della tastiera e imePadding non ha effetto → campi tagliati.
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (initial == null) stringResource(R.string.srv_new) else stringResource(R.string.srv_edit)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_cancel))
                            }
                        },
                        actions = {
                            TextButton(enabled = host.isNotBlank(), onClick = { salva() }) {
                                Text(stringResource(R.string.common_save))
                            }
                        },
                    )
                },
            ) { padding ->
                // beta19 (Roxy): ORDINE DEI MODIFIER CRITICO. Prima era
                // .fillMaxSize().imePadding().verticalScroll() — sbagliato:
                // fillMaxSize fissa l'altezza a tutto lo schermo PRIMA che
                // imePadding possa ridurla, così la tastiera copre il fondo.
                // Ora: imePadding PRIMA, poi fillMaxWidth (non fillMaxSize), così
                // l'area disponibile è già ridotta dalla tastiera e lo scroll
                // lavora sullo spazio giusto.
                // beta20: `verticalScroll` scorre SOLO se il contenitore ha
                // un'altezza VINCOLATA (il contenuto viene misurato illimitato e
                // il contenitore mostra una finestra su di esso). In beta19,
                // togliendo `fillMaxSize` per far posto a `imePadding`, e' sparito
                // anche il vincolo: lo scroll restava inerte e la schermata non
                // scorreva. Struttura corretta: vincolo + imePadding sul
                // contenitore ESTERNO, scroll sul contenuto INTERNO.
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .imePadding(),
                ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // 1.2.9 (multi-identità): con quale identità pubblicare su
                    // questo server. Spostata in cima su richiesta di Marco
                    // (post-collaudo 1.2.9): è il campo che si tocca più
                    // spesso quando si modifica un server già configurato,
                    // molto più di nome/host/porta — in fondo al form passava
                    // facilmente inosservato. "Usa l'identità predefinita"
                    // (null) resta sempre la prima voce del menu — comportamento
                    // invariato per chi non tocca questo campo.
                    Text(
                        stringResource(R.string.srv_identity),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Box {
                        val etichetta = identities.firstOrNull { it.id == identityId }
                            ?.let { it.nickname.ifBlank { it.name.ifBlank { it.email } } }
                            ?: stringResource(R.string.srv_identity_default_option)
                        TextButton(onClick = { identityMenu = true }) {
                            Icon(Icons.Default.ExpandMore, contentDescription = null)
                            Text(etichetta)
                        }
                        DropdownMenu(expanded = identityMenu, onDismissRequest = { identityMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.srv_identity_default_option)) },
                                onClick = { identityId = null; identityMenu = false },
                            )
                            identities.forEach { ident ->
                                DropdownMenuItem(
                                    text = { Text(ident.nickname.ifBlank { ident.name.ifBlank { ident.email } }) },
                                    onClick = { identityId = ident.id; identityMenu = false },
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (initial == null) {
                        Box {
                            TextButton(onClick = { presetMenu = true }) {
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                                Text(stringResource(R.string.srv_preset))
                            }
                            DropdownMenu(expanded = presetMenu, onDismissRequest = { presetMenu = false }) {
                                ServerPresets.all.forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(preset.label)
                                                Text(
                                                    "${'$'}{preset.host}  ·  " + when (preset.nota) {
                                                        "Registrazione gratuita sul sito" -> stringResource(R.string.srv_preset_free_reg)
                                                        "Nessuna registrazione" -> stringResource(R.string.srv_preset_no_reg)
                                                        else -> preset.nota
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        onClick = {
                                            // compilo host/porta/SSL e nome; utente e
                                            // password restano all'utente
                                            name = preset.label
                                            host = preset.host
                                            port = preset.port.toString()
                                            ssl = preset.ssl
                                            // 1.3.6: i due sono alternativi e
                                            // il preset ne dichiara uno solo.
                                            startTls = preset.startTls
                                            presetMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.srv_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    // wishlist beta12 (SiMcarD): autoCorrect disattivato — su host e
                    // nomi puntati (es. news.eternal-september.org) alcune tastiere
                    // inserivano automaticamente uno spazio dopo ogni punto, pensando
                    // fosse fine frase.
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text(stringResource(R.string.srv_host)) }, singleLine = true, keyboardOptions = KeyboardOptions(autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = port, onValueChange = { port = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.srv_port)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Row {
                        Checkbox(checked = ssl, onCheckedChange = {
                            ssl = it
                            if (it && port == "119") port = "563"
                            if (!it && port == "563") port = "119"
                            // TLS implicito e STARTTLS sono alternativi: attivando
                            // l'uno non ha senso tenere acceso anche l'altro.
                            if (it) startTls = false
                        })
                        Text(stringResource(R.string.srv_use_ssl), Modifier.padding(top = 12.dp))
                    }
                    // 1.2.9 (voce E, roadmap NNTP — RFC 4642): STARTTLS, alternativa
                    // al TLS implicito per i server rimasti su una porta in chiaro
                    // (di solito 119) che offrono l'upgrade a cifrato dopo la
                    // connessione. Disattivato/nascosto quando SSL è già attivo:
                    // sarebbe ridondante (la connessione è già cifrata da subito).
                    if (!ssl) {
                        Row {
                            Checkbox(checked = startTls, onCheckedChange = { startTls = it })
                            Column(Modifier.padding(top = 10.dp)) {
                                Text(stringResource(R.string.srv_starttls))
                                Text(
                                    stringResource(R.string.srv_starttls_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    // 1.6.5 / 193: proxy SOCKS5, per Orbot/Tor o Nym. Solo se acceso.
                    Row {
                        Checkbox(checked = proxyOn, onCheckedChange = { proxyOn = it })
                        Column(Modifier.padding(top = 10.dp)) {
                            Text(stringResource(R.string.srv_proxy))
                            Text(
                                stringResource(R.string.srv_proxy_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (proxyOn) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = proxyHost,
                                onValueChange = { proxyHost = it },
                                label = { Text(stringResource(R.string.srv_proxy_host)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = proxyPort,
                                onValueChange = { v -> proxyPort = v.filter { it.isDigit() }.take(5) },
                                label = { Text(stringResource(R.string.srv_proxy_port)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.5f),
                            )
                        }
                    }
                    OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text(stringResource(R.string.srv_user)) }, singleLine = true, keyboardOptions = KeyboardOptions(autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth())
                    // 1.3.5 — tre aggiunte nate dallo stesso caso reale (481
                    // su Solani con credenziali valide altrove):
                    //  1. l'occhio per rivelare la password, chiesto
                    //     esplicitamente dal segnalatore: serve a vedere i
                    //     refusi veri;
                    //  2. il conteggio caratteri, che serve a vedere cio' che
                    //     l'occhio NON mostra — uno spazio in piu' e'
                    //     invisibile anche in chiaro. Chi sa quanti caratteri
                    //     ha la propria password se ne accorge subito;
                    //  3. l'avviso sugli spazi ai bordi.
                    // La password NON viene ripulita con trim(): puo'
                    // legittimamente contenere spazi ai bordi, e non abbiamo
                    // il diritto di modificarla in silenzio. Si MOSTRA il
                    // problema, non lo si corregge d'autorita'. (Il nome
                    // utente invece si ripulisce, in salva(): un nome utente
                    // con spazi ai bordi non esiste.)
                    // keyboardType = Password: alcune tastiere evitano cosi'
                    // suggerimenti e apprendimento — ed e' un suggerimento
                    // completato che infila lo spazio di troppo.
                    var passVisibile by remember { mutableStateOf(false) }
                    val bordiConSpazio = pass.isNotEmpty() &&
                        (pass.first().isWhitespace() || pass.last().isWhitespace())
                    // Tipo dichiarato per esteso: `supportingText` vuole
                    // `@Composable (() -> Unit)?`, e con un if/else che
                    // restituisce null da un ramo e una lambda dall'altro
                    // l'inferenza e' inutilmente in salita. Scritto cosi' non
                    // c'e' niente da inferire. null = nessuno spazio
                    // riservato sotto al campo finche' non si scrive nulla.
                    val notaPassword: @Composable (() -> Unit)? =
                        if (pass.isEmpty()) {
                            null
                        } else {
                            {
                                Text(
                                    if (bordiConSpazio) {
                                        stringResource(R.string.srv_pass_edge_space, pass.length)
                                    } else {
                                        stringResource(R.string.srv_pass_chars, pass.length)
                                    },
                                    color = if (bordiConSpazio) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text(if (initial == null) stringResource(R.string.srv_pass_new) else stringResource(R.string.srv_pass_edit)) },
                        singleLine = true,
                        visualTransformation = if (passVisibile) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passVisibile = !passVisibile }) {
                                Icon(
                                    imageVector = if (passVisibile) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = stringResource(
                                        if (passVisibile) R.string.srv_pass_hide else R.string.srv_pass_show,
                                    ),
                                )
                            }
                        },
                        supportingText = notaPassword,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // 1.2.9 (voce E, roadmap NNTP): override PER SERVER
                    // dell'impostazione globale "Consenti autenticazione non
                    // cifrata" (Manutenzione → Sicurezza). Tre stati, non due:
                    // "usa il globale" (default, comportamento invariato) è
                    // distinto sia da "consenti sempre" sia da "blocca sempre".
                    Text(
                        stringResource(R.string.srv_auth_override),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Column(Modifier.selectableGroup()) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .selectable(selected = authOverride == null, onClick = { authOverride = null }),
                        ) {
                            androidx.compose.material3.RadioButton(selected = authOverride == null, onClick = null)
                            Text(stringResource(R.string.srv_auth_override_global), Modifier.padding(start = 8.dp))
                        }
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .selectable(selected = authOverride == true, onClick = { authOverride = true }),
                        ) {
                            androidx.compose.material3.RadioButton(selected = authOverride == true, onClick = null)
                            Text(stringResource(R.string.srv_auth_override_allow), Modifier.padding(start = 8.dp))
                        }
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .selectable(selected = authOverride == false, onClick = { authOverride = false }),
                        ) {
                            androidx.compose.material3.RadioButton(selected = authOverride == false, onClick = null)
                            Text(stringResource(R.string.srv_auth_override_block), Modifier.padding(start = 8.dp))
                        }
                    }

                    // spazio finale: con la tastiera aperta si scorre oltre
                    // l'ultimo campo invece di trovarlo incollato al bordo
                    Spacer(Modifier.height(24.dp))
                }
                }
            }
        }
    }
}
