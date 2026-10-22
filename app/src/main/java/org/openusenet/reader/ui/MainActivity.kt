/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Context
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import org.openusenet.reader.R
import org.openusenet.reader.data.DbSafety
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.openusenet.reader.App
import org.openusenet.reader.AppSettings
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.data.AvvisoConnessioneNonSicura
import org.openusenet.reader.update.UpdateChecker
import org.openusenet.reader.ui.screens.AboutScreen
import org.openusenet.reader.ui.screens.TlsIdentityScreen
import org.openusenet.reader.ui.screens.DraftsScreen
import org.openusenet.reader.ui.screens.ReleaseNotesScreen
import org.openusenet.reader.ui.screens.AppearanceScreen
import org.openusenet.reader.ui.screens.ArticleScreen
import org.openusenet.reader.ui.screens.ComposeScreen
import org.openusenet.reader.ui.screens.CustomizeBarsScreen
import org.openusenet.reader.ui.screens.FiltersScreen
import org.openusenet.reader.ui.screens.GroupDirectoryScreen
import org.openusenet.reader.ui.screens.GroupsScreen
import org.openusenet.reader.ui.screens.IdentityEditScreen
import org.openusenet.reader.ui.screens.IdentityScreen
import org.openusenet.reader.ui.screens.MaintenanceScreen
import org.openusenet.reader.ui.screens.SearchScreen
import org.openusenet.reader.ui.screens.ServersScreen
import org.openusenet.reader.ui.screens.ThreadListScreen
import org.openusenet.reader.ui.screens.ThreePaneHome
import org.openusenet.reader.ui.theme.OpenUsenetTheme

/** 1.5.7: la durata predefinita di Navigation Compose 2.8, mantenuta identica. */
private const val DURATA_DISSOLVENZA = 700

class MainActivity : ComponentActivity() {
    // Su Android <= 12 applica la lingua scelta in-app avvolgendo il context
    // dell'Activity: da qui stringResource e getString seguono la lingua scelta.
    // Su 13+ e' un no-op (ci pensa il framework). Vedi LocaleHelper.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applica(newBase))
    }

    /**
     * 1.5.7 / 175 (F8): i tasti del volume sfogliano i messaggi, se l'opzione e'
     * attiva E se c'e' un messaggio aperto a schermo intero. In tutti gli altri
     * casi l'evento prosegue come sempre. Vedi ui/TastiVolume.kt.
     */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (org.openusenet.reader.ui.TastiVolume.gestisci(
                AppSettings.tastiVolume, event.keyCode, event.action, event.repeatCount,
            )
        ) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // beta13: splashscreen "vero" — installato PRIMA di super.onCreate,
        // come richiede l'API. Appare durante il caricamento reale e sparisce
        // da sé quando l'app è pronta (nessun ritardo artificiale).
        // installSplashScreen è una funzione di ESTENSIONE su Activity
        // (androidx.core.splashscreen.SplashScreenViewProvider), va importata
        // e chiamata direttamente, non come metodo statico di SplashScreen.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // beta14 (schermo nero in scrittura, segnalato su Android 10 E 15 —
        // quindi NON un problema di una singola versione): NeReO aveva solo
        // windowSoftInputMode="adjustResize" nel manifest e nessuna gestione
        // esplicita degli insets finestra. Con targetSdk 35 l'edge-to-edge
        // moderno cambia le regole e adjustResize da solo non basta più su
        // alcune piattaforme/dispositivi: l'area del composer poteva restare
        // NON disegnata (nera) all'apertura della tastiera, e non tornava
        // scrollando (contenuto non disegnato, non spostato). Con questa riga
        // diciamo al sistema che gestiamo noi gli insets via Compose: gli
        // imePadding() presenti nei composer ora ricevono gli insets corretti
        // della tastiera. Fix mirato e reversibile.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        // beta20 (il Merovingio): azzero la guardia anti-risync a ogni avvio.
        // SyncGuard è un singleton di PROCESSO: chiudendo l'app con "Chiudi
        // applicazione" l'Activity sparisce ma il processo può sopravvivere, e i
        // tempi registrati restavano validi. Riaprendo entro un minuto la
        // guardia bloccava il download dei nuovi articoli, benché l'opzione
        // fosse attiva. La guardia deve valere solo dentro una sessione, mai
        // fra un avvio e l'altro.
        org.openusenet.reader.ui.screens.SyncGuard.azzeraTutto()
        // Stesso identico problema per il flag del sync all'avvio: serve a farlo
        // avvenire UNA volta per sessione, ma vive anch'esso nel processo. Se il
        // processo sopravvive alla chiusura, alla riapertura risultava "già
        // fatto" e il sync automatico di tutti i gruppi non partiva più.
        AppSettings.syncOnStartDone = false
        val repo = (application as App).repository
        setContent {
            OpenUsenetTheme {
                AppNav(repo)
            }
        }
    }
}

object Routes {
    const val GROUPS = "groups"
    const val SERVERS = "servers"
    const val FILTERS = "filters"
    const val DIRECTORY = "directory"
    /** 1.2.9: ora l'ELENCO delle identità (vedi IDENTITY_EDIT per l'editor di una). */
    const val IDENTITY = "identity"
    const val IDENTITY_EDIT = "identity_edit/{identityId}"

    /** 1.4 (TOFU): "Verifica identita' TLS" di UN server. */
    const val TLS_IDENTITY = "tls_identity/{serverId}"
    const val APPEARANCE = "appearance"
    const val CUSTOMIZE_BARS = "customize_bars" // 1.3 (B): editor barre personalizzabili
    const val MAINTENANCE = "maintenance"
    const val ABOUT = "about"
    const val RELEASE_NOTES = "release_notes"
    const val SEARCH = "search"
    const val SEARCH_GROUP = "search/{groupId}/{groupName}"
    const val THREADS = "threads/{groupId}"
    const val ARTICLE = "article/{articleId}"
    const val COMPOSE_NEW = "compose/new/{groupId}"
    const val COMPOSE_REPLY = "compose/reply/{articleId}"
    const val COMPOSE_SUPERSEDE = "compose/supersede/{articleId}"
    const val COMPOSE_DRAFT = "compose/draft/{draftId}"
    const val DRAFTS = "drafts"
    // 1.2.9 (Piano C): bozze filtrate per gruppo, aperte dal menu ⋮ del
    // gruppo stesso invece che dal menu generale — vedi draftsForGroup().
    const val DRAFTS_GROUP = "drafts/{groupId}"

    /**
     * 1.4.8 (par. U3) — I PERCORSI COME SONO REGISTRATI DAVVERO.
     *
     * `popUpTo` confronta il percorso del DESTINATARIO, che e' la stringa
     * passata a `composable(...)`, query opzionale compresa: `Routes.THREADS`
     * da solo non corrisponde a niente e il `popUpTo` verrebbe semplicemente
     * ignorato, in silenzio e senza errori. Le due costanti stanno qui perche'
     * il modello sia scritto UNA volta sola: registrazione e `popUpTo` leggono
     * la stessa riga, e non possono divergere.
     */
    const val THREADS_PATTERN = "$THREADS?evidenzia={evidenzia}"
    const val ARTICLE_PATTERN = "$ARTICLE?q={q}"

    fun threads(groupId: Long) = "threads/$groupId"

    /**
     * 1.4.6 (par. B13): l'elenco del gruppo, posizionato su UN messaggio.
     * Query opzionale come il `?q=` di ARTICLE — una rotta sola, nessuna doppia
     * registrazione sullo stesso percorso.
     */
    fun threadsSu(groupId: Long, articleId: Long) = "threads/$groupId?evidenzia=$articleId"
    fun searchGroup(groupId: Long, groupName: String) =
        "search/$groupId/" + java.net.URLEncoder.encode(groupName, "UTF-8")
    fun article(articleId: Long) = "article/$articleId"
    fun composeNew(groupId: Long) = "compose/new/$groupId"
    fun composeReply(articleId: Long) = "compose/reply/$articleId"
    fun composeSupersede(articleId: Long) = "compose/supersede/$articleId"
    fun composeDraft(draftId: Long) = "compose/draft/$draftId"
    fun draftsForGroup(groupId: Long) = "drafts/$groupId"
    /** 1.2.9: 0 = nuova identità (l'editor lo riconosce e parte vuoto). */
    fun identityEdit(identityId: Long) = "identity_edit/$identityId"
}

@Composable
fun AppNav(repo: NewsRepository, nav: NavHostController = rememberNavController()) {
    val ctx = LocalContext.current
    // controllo automatico dei nuovi messaggi mentre l'app è aperta
    LaunchedEffect(AppSettings.autoCheckMinutes) {
        val minuti = AppSettings.autoCheckMinutes
        if (minuti > 0) {
            while (true) {
                delay(minuti * 60_000L)
                // 1.2.7: il sync periodico NON deve far comparire il dialog
                // "connessione non sicura" mentre l'utente legge (sarebbe modale
                // e a intervalli). Lo mostriamo solo sui sync avviati a mano.
                val nuoveAMe = runCatching { repo.syncAllSubscribed(segnalaAuthNonCifrata = false) }.getOrDefault(0)
                if (AppSettings.dingOnReply && nuoveAMe > 0) playDing(ctx)
            }
        }
    }
    // Avviso una-tantum se la rete di sicurezza ha reinizializzato il database
    // (schema incompatibile) mettendone da parte una copia. Vedi DbSafety/Db.kt.
    AvvisoResetDatabase()
    // 1.2.5 (Bingo3331): controllo automatico nuove versioni su SourceForge.
    AvvisoAggiornamento()
    // 1.2.7 (feature A, Marco): dialog "connessione non sicura" quando un sync
    // fallisce per auth rifiutata su connessione non cifrata (483). Montato qui
    // una volta sola; osserva il segnale del Repository.
    DialogoConnessioneNonSicura(repo, nav)
    /*
     * 1.5.7 / 175 — LE ANIMAZIONI TRA LE SCHERMATE, CON UN INTERRUTTORE.
     *
     * Un utente: «Is it possible to disable the "screen flashing" animation
     * when opening messages and menus?». Il «lampeggio» e' la dissolvenza
     * PREDEFINITA di Navigation Compose: 700 ms in entrata e in uscita
     * (verificato nell'APK: `sipush 700` in NavHostKt). NeReO non l'aveva mai
     * scelta ne' toccata. Acceso (predefinito) resta IDENTICA a prima — stessa
     * dissolvenza, stessa durata; spento, le schermate cambiano di colpo.
     * Le lambda leggono AppSettings al momento del cambio: l'interruttore vale
     * subito, senza riavviare. I menu a tendina hanno un'animazione propria di
     * Material, brevissima, che qui non si tocca.
     */
    NavHost(
        navController = nav,
        startDestination = Routes.GROUPS,
        enterTransition = {
            if (AppSettings.animazioni) fadeIn(animationSpec = tween(DURATA_DISSOLVENZA)) else EnterTransition.None
        },
        exitTransition = {
            if (AppSettings.animazioni) fadeOut(animationSpec = tween(DURATA_DISSOLVENZA)) else ExitTransition.None
        },
        popEnterTransition = {
            if (AppSettings.animazioni) fadeIn(animationSpec = tween(DURATA_DISSOLVENZA)) else EnterTransition.None
        },
        popExitTransition = {
            if (AppSettings.animazioni) fadeOut(animationSpec = tween(DURATA_DISSOLVENZA)) else ExitTransition.None
        },
    ) {
        composable(Routes.SEARCH) { SearchScreen(repo, nav) }
        composable(Routes.SEARCH_GROUP) { backStack ->
            val gid = backStack.arguments?.getString("groupId")?.toLongOrNull() ?: return@composable
            val gname = backStack.arguments?.getString("groupName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            SearchScreen(repo, nav, groupId = gid, groupName = gname)
        }
        composable(Routes.GROUPS) {
            // adattivo: tre pannelli su schermi larghi (landscape/tablet), pagine su telefono
            BoxWithConstraints {
                // "auto" = schermi larghi (600 dp: telefono in orizzontale compreso);
                // l'utente può comunque forzarlo o escluderlo da Aspetto
                val multiPannello = when (AppSettings.paneMode) {
                    "always" -> true
                    "never" -> false
                    else -> maxWidth >= 600.dp
                }
                if (multiPannello) ThreePaneHome(repo, nav) else GroupsScreen(repo, nav)
            }
        }
        composable(Routes.SERVERS) { ServersScreen(repo, nav) }
        composable(Routes.FILTERS) { FiltersScreen(repo, nav) }
        composable(Routes.DIRECTORY) { GroupDirectoryScreen(repo, nav) }
        composable(Routes.IDENTITY) { IdentityScreen(repo, nav) }
        composable(Routes.IDENTITY_EDIT) { backStack ->
            val identityId = backStack.arguments?.getString("identityId")?.toLongOrNull() ?: 0L
            IdentityEditScreen(repo, nav, identityId)
        }
        composable(Routes.TLS_IDENTITY) { backStack ->
            val sid = backStack.arguments?.getString("serverId")?.toLongOrNull() ?: 0L
            TlsIdentityScreen(repo, nav, sid)
        }
        composable(Routes.APPEARANCE) { AppearanceScreen(nav) }
        composable(Routes.CUSTOMIZE_BARS) { CustomizeBarsScreen(nav) } // 1.3 (B)
        composable(Routes.MAINTENANCE) { MaintenanceScreen(repo, nav) }
        composable(Routes.ABOUT) { AboutScreen(nav) }
        composable(Routes.RELEASE_NOTES) { ReleaseNotesScreen(nav) }
        composable(
            Routes.THREADS_PATTERN,
            arguments = listOf(
                androidx.navigation.navArgument("evidenzia") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStack ->
            val groupId = backStack.arguments?.getString("groupId")?.toLongOrNull() ?: return@composable
            // 1.4.6 (par. B13): senza il parametro si entra nel gruppo come
            // sempre, e il ripristino della posizione resta quello di prima.
            val evidenzia = backStack.arguments?.getString("evidenzia")?.toLongOrNull()
            ThreadListScreen(repo, nav, groupId, evidenzia)
        }
        // Un'unica rotta con query opzionale ?q= (termine di ricerca da
        // evidenziare, beta13). Senza ?q= il defaultValue "" copre la
        // navigazione normale dalla lista thread — niente doppia
        // registrazione sullo stesso path (eviterebbe ambiguità di routing).
        composable(
            Routes.ARTICLE_PATTERN,
            arguments = listOf(
                androidx.navigation.navArgument("q") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStack ->
            val articleId = backStack.arguments?.getString("articleId")?.toLongOrNull() ?: return@composable
            val q = backStack.arguments?.getString("q").orEmpty()
            ArticleScreen(repo, nav, articleId, searchQuery = q)
        }
        composable(Routes.COMPOSE_NEW) { backStack ->
            val groupId = backStack.arguments?.getString("groupId")?.toLongOrNull() ?: return@composable
            ComposeScreen(repo, nav, groupId = groupId)
        }
        composable(Routes.COMPOSE_REPLY) { backStack ->
            val articleId = backStack.arguments?.getString("articleId")?.toLongOrNull() ?: return@composable
            ComposeScreen(repo, nav, replyToArticleId = articleId)
        }
        composable(Routes.COMPOSE_SUPERSEDE) { backStack ->
            val articleId = backStack.arguments?.getString("articleId")?.toLongOrNull() ?: return@composable
            ComposeScreen(repo, nav, supersedeArticleId = articleId)
        }
        composable(Routes.COMPOSE_DRAFT) { backStack ->
            val draftId = backStack.arguments?.getString("draftId")?.toLongOrNull() ?: return@composable
            ComposeScreen(repo, nav, draftId = draftId)
        }
        composable(Routes.DRAFTS) { DraftsScreen(repo, nav, groupId = null) }
        composable(Routes.DRAFTS_GROUP) { backStack ->
            val gid = backStack.arguments?.getString("groupId")?.toLongOrNull()
            DraftsScreen(repo, nav, groupId = gid)
        }
    }
}

/**
 * 1.2.5 (Bingo3331): all'avvio, secondo l'intervallo scelto in Manutenzione,
 * chiede a SourceForge (best_release.json) qual è l'ultima versione e, se è
 * più recente di quella installata, mostra un avviso con il link alla pagina
 * delle release. Silenzioso su rete assente/errore E quando non trova nulla
 * di nuovo (per non snervare l'utente, richiesta di Marco/Bingo3331): nessun
 * popup, solo l'indicatore "Ultimo controllo…" in Manutenzione. L'app NON
 * scarica né installa nulla: apre il browser e l'utente scarica a mano.
 */
@Composable
private fun AvvisoAggiornamento() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var nuova by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // 1.2.6 (Marco): il controllo va rifatto a OGNI ritorno in primo piano, non
    // solo alla prima composizione. Con `LaunchedEffect(Unit)` l'effetto partiva
    // una volta sola, all'avvio a freddo: se Android teneva il processo caldo,
    // riaprendo l'app il giorno dopo NON ri-scattava e il "primo utilizzo di
    // giornata" non controllava nulla (il manuale sì, perché legato al tocco).
    // Con un osservatore ON_START il controllo segue le aperture reali.
    //
    // 1.2.8 (Bingo3331 + HAL, era "1.3.1" prima della rinumerazione): throttle per FINESTRA MOBILE in millisecondi
    // reali (UpdateChecker.intervalMs), non più "stesso giorno di calendario".
    // Col vecchio confronto per data solare, chi apriva l'app anche una sola
    // volta prima che una release fosse pubblicata bruciava lo slot di
    // giornata — e restava "bloccato" fino al giorno dopo, qualunque numero
    // di volte riaprisse l'app nel frattempo (il sintomo esatto segnalato da
    // Bingo3331 il 1 settembre 2026). Il timestamp si scrive SOLO a controllo
    // riuscito, così un buco di rete al risveglio non brucia lo slot.
    DisposableEffect(lifecycleOwner) {
        val osservatore = LifecycleEventObserver { _, evento ->
            if (evento != Lifecycle.Event.ON_START) return@LifecycleEventObserver
            // 1.5.4: la decisione sta in UpdateChecker.deveControllare (Kotlin
            // puro, provata): 24 ore OPPURE un nuovo giorno di calendario, per
            // «una volta al giorno». «Mai» resta mai.
            val ultimo = AppSettings.lastUpdateCheckMs
            val ora = System.currentTimeMillis()
            val fuso = java.time.ZoneId.systemDefault()
            fun giorno(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(fuso).toLocalDate().toEpochDay()
            if (!UpdateChecker.deveControllare(
                    AppSettings.autoCheckMode, ultimo, ora, giorno(ultimo), giorno(ora),
                )
            ) {
                return@LifecycleEventObserver // ancora dentro la finestra: salta
            }
            scope.launch {
                val current = runCatching {
                    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
                }.getOrNull() ?: return@launch
                // rete assente/errore ⇒ latest null ⇒ NON si tocca il timestamp:
                // il controllo si ritenta alla prossima apertura.
                val latest = UpdateChecker.latestVersion() ?: return@launch
                val trovato = UpdateChecker.isNewer(latest, current)
                AppSettings.lastUpdateCheckMs = System.currentTimeMillis()
                AppSettings.lastUpdateCheckFound = if (trovato) latest else ""
                AppSettings.save()
                if (trovato) nuova = latest
            }
        }
        lifecycleOwner.lifecycle.addObserver(osservatore)
        onDispose { lifecycleOwner.lifecycle.removeObserver(osservatore) }
    }
    val v = nuova ?: return
    AlertDialog(
        onDismissRequest = { nuova = null },
        title = { Text(stringResource(R.string.upd_available_title)) },
        text = { Text(stringResource(R.string.upd_available_msg, v)) },
        confirmButton = {
            TextButton(onClick = {
                nuova = null
                runCatching {
                    ctx.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(UpdateChecker.RELEASES_URL),
                        ),
                    )
                }
            }) { Text(stringResource(R.string.upd_open_page)) }
        },
        dismissButton = { TextButton(onClick = { nuova = null }) { Text(stringResource(R.string.upd_later)) } },
    )
}

/**
 * 1.2.7 (feature A, Marco): quando un sync fallisce perché il server rifiuta la
 * password su connessione non cifrata (483), invece di un errore secco il neofita
 * vede questo dialog con due vie d'uscita chiare. Le due opzioni NON sono pari: la
 * strada giusta è SSL, "consenti non cifrata" è un ripiego deprecato — perciò SSL è
 * il pulsante d'azione in evidenza, mentre "consenti" è un pulsante piccolo in rosso
 * dentro il testo (richiede una lettura e un tocco deliberato, non si preme per
 * sbaglio pensando di chiudere). Il pulsante qui attiva sempre l'override GLOBALE
 * (vale per tutti i server): detto a chiare lettere nel messaggio. Dalla 1.2.9
 * esiste anche un override PER SERVER (Server → modifica → "Autenticazione non
 * cifrata"), ma quello si imposta consapevolmente da lì, non da questo dialog
 * d'emergenza — che resta la via rapida "sblocca e vai avanti".
 */
@Composable
private fun DialogoConnessioneNonSicura(repo: NewsRepository, nav: NavHostController) {
    val serverId = AvvisoConnessioneNonSicura.serverId.value ?: return
    var nomeServer by remember(serverId) { mutableStateOf("") }
    LaunchedEffect(serverId) {
        nomeServer = runCatching { repo.serverNameById(serverId) }.getOrNull().orEmpty()
    }
    AlertDialog(
        onDismissRequest = { AvvisoConnessioneNonSicura.serverId.value = null },
        title = { Text(stringResource(R.string.sec_unsafe_title)) },
        text = {
            Column {
                Text(stringResource(R.string.sec_unsafe_msg, nomeServer))
                Spacer(Modifier.height(12.dp))
                // ripiego deprecato: piccolo, rosso, esplicito. Attiva il flag
                // GLOBALE e chiude; l'utente potrà poi ripremere aggiorna.
                TextButton(onClick = {
                    AppSettings.consentiAuthNonCifrata = true
                    AppSettings.save()
                    AvvisoConnessioneNonSicura.serverId.value = null
                }) {
                    Text(
                        stringResource(R.string.sec_unsafe_allow),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AvvisoConnessioneNonSicura.serverId.value = null
                nav.navigate(Routes.SERVERS)
            }) { Text(stringResource(R.string.sec_unsafe_use_ssl)) }
        },
        dismissButton = {
            TextButton(onClick = { AvvisoConnessioneNonSicura.serverId.value = null }) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

/**
 * 1.2.6 (Marco): controllo aggiornamenti MANUALE, richiamabile dal menu principale
 * (scorciatoia: prima era solo in Manutenzione, trafila troppo lunga). Auto-contenuto:
 * quando [richiesto] passa a true interroga SourceForge e mostra un dialog con l'esito
 * (nuova versione + link / già aggiornato / rete non disponibile). `onFine()` resetta il
 * flag della voce di menu a controllo concluso. Stessa logica dell'avviso automatico.
 */
@Composable
fun ControlloAggiornamentiManuale(richiesto: Boolean, onFine: () -> Unit) {
    val ctx = LocalContext.current
    var nuovaVersione by remember { mutableStateOf<String?>(null) }
    var messaggio by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(richiesto) {
        if (!richiesto) return@LaunchedEffect
        val current = runCatching {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        }.getOrNull() ?: ""
        val latest = UpdateChecker.latestVersion()
        if (latest != null) {
            val trovato = UpdateChecker.isNewer(latest, current)
            AppSettings.lastUpdateCheckMs = System.currentTimeMillis()
            AppSettings.lastUpdateCheckFound = if (trovato) latest else ""
            AppSettings.save()
        }
        when {
            latest == null -> messaggio = ctx.getString(R.string.upd_check_failed)
            UpdateChecker.isNewer(latest, current) -> nuovaVersione = latest
            else -> messaggio = ctx.getString(R.string.upd_uptodate)
        }
        onFine() // reset del flag a controllo concluso (rilancia l'effetto con false → no-op)
    }
    val nv = nuovaVersione
    if (nv != null) {
        AlertDialog(
            onDismissRequest = { nuovaVersione = null },
            title = { Text(stringResource(R.string.upd_available_title)) },
            text = { Text(stringResource(R.string.upd_available_msg, nv)) },
            confirmButton = {
                TextButton(onClick = {
                    nuovaVersione = null
                    runCatching {
                        ctx.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(UpdateChecker.RELEASES_URL),
                            ),
                        )
                    }
                }) { Text(stringResource(R.string.upd_open_page)) }
            },
            dismissButton = { TextButton(onClick = { nuovaVersione = null }) { Text(stringResource(R.string.upd_later)) } },
        )
    }
    val m = messaggio
    if (m != null) {
        AlertDialog(
            onDismissRequest = { messaggio = null },
            title = { Text(stringResource(R.string.upd_section)) },
            text = { Text(m) },
            confirmButton = { TextButton(onClick = { messaggio = null }) { Text(stringResource(R.string.common_ok)) } },
        )
    }
}

/**
 * Mostra UNA volta un avviso se la rete di sicurezza del database ha dovuto
 * reinizializzarlo (schema incompatibile: migrazione mancante o downgrade),
 * salvandone prima una copia. Legge il flag lasciato da Db.proteggiPrimaDelWipe
 * e lo cancella dopo averlo mostrato. Nel caso normale non fa nulla.
 */
@Composable
private fun AvvisoResetDatabase() {
    val ctx = LocalContext.current
    var messaggio by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val prefs = ctx.getSharedPreferences(DbSafety.PREFS, Context.MODE_PRIVATE)
        // La PRESENZA della chiave versione = c'e' un avviso da mostrare.
        if (prefs.contains(DbSafety.KEY_FROM_VERSION)) {
            val backup = prefs.getString(DbSafety.KEY_BACKUP_PATH, null)
            // In coroutine si usa ctx.getString, non stringResource (@Composable).
            messaggio = if (backup != null)
                ctx.getString(R.string.db_reset_with_backup, backup)
            else
                ctx.getString(R.string.db_reset_no_backup)
            prefs.edit()
                .remove(DbSafety.KEY_BACKUP_PATH)
                .remove(DbSafety.KEY_FROM_VERSION)
                .apply()
        }
    }
    val testo = messaggio ?: return
    AlertDialog(
        onDismissRequest = { messaggio = null },
        confirmButton = {
            TextButton(onClick = { messaggio = null }) {
                Text(stringResource(R.string.common_ok))
            }
        },
        title = { Text(stringResource(R.string.db_reset_title)) },
        text = { Text(testo) },
    )
}
