/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Composizione: nuovo thread (da un gruppo) o risposta (da un articolo).
 *
 * Modalità di quoting (selezionabili dall'icona virgolette, ricordate
 * come default per le prossime risposte):
 *  - below   : citazione sopra, si scrive sotto (netiquette classica)
 *  - above   : si scrive sopra, citazione sotto (top-posting)
 *  - trimmed : citazione ridotta al solo testo dell'autore (scarta i livelli
 *              di citazione più vecchi)
 *  - none    : nessuna citazione
 * In tutte, la firma del mittente citato viene tagliata al "-- ".
 * Il cursore viene posizionato nel punto in cui si deve scrivere.
 */
package org.openusenet.reader.ui.screens

import org.openusenet.reader.ui.CitazioneInAttesa
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.ArrowBack
import org.openusenet.reader.ui.icone.Send
import org.openusenet.reader.ui.icone.EnhancedEncryption
import org.openusenet.reader.ui.icone.Groups
import org.openusenet.reader.ui.icone.ContentCut
import org.openusenet.reader.ui.icone.Save
import org.openusenet.reader.ui.icone.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.openusenet.reader.ui.GuardiaBozza
import org.openusenet.reader.ui.icone.FormatQuote
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.openusenet.reader.ui.PulsanteIncolla
import org.openusenet.reader.ui.rememberAppunti
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.openusenet.reader.App
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
// 1.4.10 (voce 11): wrapBody e avvolgiFlowed vivono nel package `text`.
// Erano in `ui.screens` e ComposeScreen li vedeva senza import perche' era
// nello stesso package: era proprio quella vicinanza accidentale a tenere
// in piedi il debito.
import org.openusenet.reader.text.corpoDaInviare
import org.openusenet.reader.data.Article
import org.openusenet.reader.data.Draft
import org.openusenet.reader.data.Identity
import org.openusenet.reader.data.IntestazioniPersonalizzate
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.thread.Threader
import org.openusenet.reader.ui.indietroSicuro
import org.openusenet.reader.ui.spiegazioneRifiutoInvio

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ComposeScreen(
    repo: NewsRepository,
    nav: NavHostController,
    groupId: Long? = null,
    replyToArticleId: Long? = null,
    supersedeArticleId: Long? = null,
    draftId: Long? = null,
) {
    val ctx = LocalContext.current
    // 1.3.3 (Roxy): appunti per il pulsante "Incolla" in barra.
    val appunti = rememberAppunti() // 1.3.5: unico punto, vedi ui/Appunti.kt
    val prefs = remember { ctx.getSharedPreferences("identity", Context.MODE_PRIVATE) }
    // RC01: introduzione predefinita alla citazione, letta qui perché
    // stringResource funziona solo in scope @Composable (i punti che la usano
    // sono dentro LaunchedEffect/onClick).
    val introPredefinita = stringResource(R.string.cmp_default_intro)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // 1.2.9 (multi-identità), 1.6: l'identità viene risolta per il gruppo di
    // destinazione (Repository.identitaPerGruppo) appena lo si conosce, dentro
    // la LaunchedEffect qui sotto — non più letta subito da preferenze sciolte
    // globali. fromName/fromEmail/customHeaders restano campi modificabili
    // dall'utente (come sempre), solo pre-compilati dall'identità risolta.
    var identity by remember { mutableStateOf<Identity?>(null) }
    var fromName by remember { mutableStateOf("") }
    var fromEmail by remember { mutableStateOf("") }
    var customHeaders by remember { mutableStateOf("") }
    // dettagli SEMPRE chiusi all'apertura: aperti toglievano tutta l'altezza al
    // corpo del messaggio (nei nuovi thread la tastiera copriva l'area di scrittura)
    var showDetails by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var bodyField by remember { mutableStateOf(TextFieldValue("")) }
    var parent by remember { mutableStateOf<Article?>(null) }
    var replyMode by remember { mutableStateOf(prefs.getString("reply_mode", "below") ?: "below") }
    var modeMenu by remember { mutableStateOf(false) }
    // beta15 (Bingo3331): dialog "Rifinisci citazione" per togliere righe citate
    var showTrim by remember { mutableStateOf(false) }
    // beta17 (Bingo3331): gruppi extra del crosspost scelti dall'utente + dialog
    // selettore. Vuoto = risposta al solo gruppo corrente (comportamento storico).
    var extraGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    /*
     * 1.4 (Dave Royal) — «ho già scelto» è uno stato, non una lista vuota.
     *
     * Serve a distinguere «non ho ancora aperto il selettore» da «l'ho aperto e
     * ho deciso di NON aggiungere gruppi»: con `rispostaCrosspostTuttiIGruppi`
     * attivo i due casi portano a due schermate opposte, e confonderli è il
     * difetto che Dave ha trovato — riaprire il selettore per controllare
     * rimetteva i gruppi che lui aveva tolto.
     */
    var crosspostScelto by remember { mutableStateOf(false) }
    var showCrosspost by remember { mutableStateOf(false) }
    // beta18 (Marco/Bingo3331): crosspost anche all'apertura di un nuovo thread.
    var showCrosspostNuovo by remember { mutableStateOf(false) }
    val gruppiSottoscritti by repo.subscribedGroupsWithTotals.collectAsState(initial = emptyList())
    var targetGroupId by remember { mutableStateOf(groupId) }
    // 1.4 (Henry The Mole): introduzione alla citazione impostata su QUESTO
    // gruppo. null/vuota = si usa quella dell'identita'. Vedi introLine().
    var introGruppo by remember { mutableStateOf<String?>(null) }
    var referencesHeader by remember { mutableStateOf<String?>(null) }
    // valorizzato in modalità supersede: Message-ID del proprio articolo da sostituire
    var supersedesId by remember { mutableStateOf<String?>(null) }
    // id della bozza corrispondente a questa composizione: vale 0 finché non ne
    // esiste una. Serve a non creare duplicati salvando più volte.
    var bozzaId by remember { mutableStateOf(draftId ?: 0L) }
    var groupName by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }

    // prefill iniziale
    LaunchedEffect(groupId, replyToArticleId, supersedeArticleId, draftId) {
        // Ripresa di una bozza: rimette in piedi anche il CONTESTO (a chi si
        // stava rispondendo, eventuale supersede, identità), non solo il testo.
        if (draftId != null && draftId > 0) {
            repo.draftById(draftId)?.let { d ->
                targetGroupId = d.groupId
                repo.groupById(d.groupId)?.let { g -> groupName = g.name }
                subject = d.subject
                bodyField = TextFieldValue(d.body)
                // Il mittente della bozza resta quello salvato CON la bozza (non
                // viene ricalcolato dall'identità del server): riprendere a
                // scrivere non deve cambiare a chi nome si stava già rispondendo.
                fromName = d.fromName
                fromEmail = d.fromEmail
                referencesHeader = d.references.ifBlank { null }
                supersedesId = d.supersedesId.ifBlank { null }
                if (d.replyToArticleId > 0) parent = repo.articleById(d.replyToArticleId)
                // Le intestazioni personalizzate non fanno parte della bozza salvata
                // (Draft non le conserva): si ripescano dall'identità del server.
                identity = repo.identitaPerGruppo(targetGroupId).also { customHeaders = it.customHeaders }
                ready = true
                return@LaunchedEffect
            }
        }
        if (replyToArticleId != null) {
            val p = repo.articleById(replyToArticleId)
            if (p != null) {
                parent = p
                targetGroupId = p.groupId
                subject = "Re: " + Threader.normalizeSubject(p.subject)
                referencesHeader = (p.references + " " + p.messageId).trim()
            }
        }
        targetGroupId?.let {
            val g = repo.groupById(it)
            groupName = g?.name ?: ""
            introGruppo = g?.replyIntro
        }
        // 1.4.5 (§A10, Dave Royal): con «rispondi sempre a tutti i gruppi»
        // attivo, la risposta a un crosspost nasce con TUTTI i gruppi
        // dell'originale — come gia' faceva la vista a pannelli, che passa dal
        // dialogo. Qui l'impostazione non veniva guardata da nessuno e
        // `extraGroups` nasceva vuoto: si vedeva il solo gruppo di lettura
        // finche' non si apriva il selettore a mano.
        //
        // `crosspostScelto` resta FALSO: questo non e' una scelta dell'utente,
        // e' un valore di partenza. La differenza conta in due punti — un
        // Followup-To dell'autore continua a vincere, e riaprendo il selettore
        // si rivede lo stesso insieme invece di una lista congelata.
        if (replyToArticleId != null) {
            parent?.let { p ->
                extraGroups = extraPredefinitiCrosspost(
                    allGroups = crosspostGroups(p.headersRaw),
                    currentGroup = groupName,
                    tuttiIGruppi = AppSettings.rispostaCrosspostTuttiIGruppi,
                )
            }
        }
        // 1.2.9 (multi-identità): l'identità assegnata al server di
        // destinazione (o quella predefinita, se il server non ne ha una
        // propria) — mittente, indirizzo e intestazioni personalizzate
        // diventano i suoi. Tutto resta modificabile a mano.
        identity = repo.identitaPerGruppo(targetGroupId).also { ident ->
            fromName = ident.name
            fromEmail = ident.email
            customHeaders = ident.customHeaders
        }
        if (supersedeArticleId != null) {
            // Supersede: riparto dall'originale (oggetto, corpo, references) e ne
            // dichiaro il Message-ID come sostituito. Il corpo va scaricato se
            // non è già in cache.
            val orig = repo.articleById(supersedeArticleId)
            if (orig != null) {
                targetGroupId = orig.groupId
                repo.groupById(orig.groupId)?.let { g ->
                    groupName = g.name
                }
                // il gruppo/server dell'originale potrebbe differire da quello
                // risolto sopra (qui targetGroupId era ancora null): ri-risolvo.
                identity = repo.identitaPerGruppo(targetGroupId).also { ident ->
                    fromName = ident.name
                    fromEmail = ident.email
                    customHeaders = ident.customHeaders
                }
                subject = orig.subject
                referencesHeader = orig.references.ifBlank { null }
                supersedesId = orig.messageId
                val corpo = orig.body ?: runCatching { repo.fetchBody(orig.id) }.getOrNull().orEmpty()
                bodyField = TextFieldValue(corpo)
                ready = true
                return@LaunchedEffect
            }
        }
        bodyField = buildPrefill(
            identity ?: repo.identitaPerGruppo(targetGroupId), if (parent != null) replyMode else "none", parent,
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

    // 1.4 (Dave Royal): i gruppi su cui il messaggio uscirà DAVVERO — vedi
    // gruppiDestinazione in CrosspostReply.kt. Calcolato qui una volta sola e
    // mostrato sotto: non è una decorazione, è l'unico posto in cui l'utente
    // può accorgersi di dove sta scrivendo prima di premere Invia.
    //
    // 1.4 (Followup-To, GNKSA): se l'autore ha chiesto che la discussione
    // prosegua altrove, quella richiesta VINCE sul gruppo di lettura — è
    // esattamente ciò che quell'header significa. Ma vince finché l'utente non
    // decide diversamente: appena apre il selettore dei gruppi e conferma, la
    // sua scelta esplicita ha la precedenza su quella dell'autore. Una
    // decisione umana non si scavalca con un'intestazione.
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

    /**
     * Salva (o aggiorna) la bozza con lo stato attuale della composizione,
     * contesto di conversazione compreso. Restituisce l'id, così il chiamante
     * può cancellarla a invio riuscito.
     */
    suspend fun salvaBozza(): Long {
        val gid = targetGroupId ?: return 0L
        val id = repo.saveDraft(
            Draft(
                id = bozzaId,
                groupId = gid,
                subject = subject,
                body = bodyField.text,
                fromName = fromName,
                fromEmail = fromEmail,
                references = referencesHeader.orEmpty(),
                supersedesId = supersedesId.orEmpty(),
                replyToArticleId = parent?.id ?: 0L,
            )
        )
        bozzaId = id
        return id
    }

    /*
     * 1.5.1 — LA BOZZA SI SALVA DA SOLA (piano A del 14 settembre, scelto da
     * Marco). La DECISIONE sta in ui/GuardiaBozza.kt, provata con kotlinc
     * (T1, T2, T3); qui c'e' solo il meccanismo:
     *
     *  - DEBOUNCE: 1,5 s dopo l'ultima modifica. `collectLatest` annulla
     *    l'attesa precedente a ogni nuovo carattere: e' il debounce senza
     *    l'API sperimentale `debounce`.
     *  - USCITA: su ON_STOP del ciclo di vita della schermata (l'app va in
     *    secondo piano, o la schermata viene chiusa) si salva quello che c'e',
     *    lanciando su `appScope` — lo scope della schermata muore con lei, e
     *    una scrittura sospesa li' potrebbe non arrivare mai (§7.2).
     *  - MUTEX: salvataggio, invio e «Scarta» passano dallo stesso lucchetto.
     *    Senza, un salvataggio gia' in volo potrebbe finire DOPO la
     *    cancellazione della bozza e farla risorgere (T2).
     *
     * ⚠️ `salvaBozza()` esce subito se non c'e' un gruppo di destinazione: in
     * un messaggio nuovo, prima della scelta del gruppo, l'autosalvataggio non
     * fa nulla. E' corretto (§7.3), ma va saputo.
     */
    val guardia = remember { GuardiaBozza() }
    val lucchettoBozza = remember { Mutex() }

    suspend fun salvaSeServe() {
        lucchettoBozza.withLock {
            if (!guardia.deveSalvare(subject, bodyField.text)) return@withLock
            val id = runCatching { salvaBozza() }.getOrDefault(0L)
            if (id > 0) guardia.segnaSalvata()
        }
    }

    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        // T1: il riferimento e' cio' che l'apertura ha generato, PRIMA di
        // qualunque modifica. Una bozza ripresa esiste gia' (esisteGia).
        guardia.fissaIniziale(subject, bodyField.text, esisteGia = bozzaId > 0)
        snapshotFlow { subject to bodyField.text }.collectLatest {
            delay(1500)
            salvaSeServe()
        }
    }

    val cicloDiVita = LocalLifecycleOwner.current
    DisposableEffect(cicloDiVita) {
        val osservatore = LifecycleEventObserver { _, evento ->
            if (evento != Lifecycle.Event.ON_STOP) return@LifecycleEventObserver
            (ctx.applicationContext as? App)?.appScope?.launch { salvaSeServe() }
        }
        cicloDiVita.lifecycle.addObserver(osservatore)
        onDispose { cicloDiVita.lifecycle.removeObserver(osservatore) }
    }

    // T3: «Scarta» — chiede conferma, poi cancella la bozza ed esce.
    var chiediScarta by remember { mutableStateOf(false) }

    fun doSend() {
        val gid = targetGroupId ?: return
        scope.launch {
            // 1.2.9 (multi-identità): l'ultimo mittente usato si ricorda SOLO
            // nell'identità DEFAULT, e solo se è quella che si stava davvero
            // usando (server senza identità dedicata) — altrimenti postare come
            // "MCSM" su un server cambierebbe silenziosamente il nome/indirizzo
            // usato di default ovunque.
            identity?.let { ident ->
                if (ident.isDefault && (ident.name != fromName.trim() || ident.email != fromEmail.trim())) {
                    runCatching { repo.saveIdentity(ident.copy(name = fromName.trim(), email = fromEmail.trim())) }
                }
            }
            sending = true
            // 1.5.1 (T2): da qui l'autosalvataggio si ferma, e si aspetta che
            // un eventuale salvataggio in volo finisca prima di andare avanti:
            // altrimenti potrebbe arrivare DOPO la cancellazione qui sotto.
            guardia.chiudi()
            lucchettoBozza.withLock { }
            // Rete di sicurezza: la bozza viene salvata PRIMA di tentare
            // l'invio e cancellata solo a invio riuscito. Se il server è
            // irraggiungibile o la rete cade, il testo resta in Bozze invece
            // di andare perso uscendo dalla schermata.
            runCatching { salvaBozza() }
            runCatching {
                val ident = identity ?: repo.identitaPerGruppo(targetGroupId)
                repo.postArticle(
                    gid, fromName.trim(), fromEmail.trim(), subject.trim(),
                    // a capo a 72 colonne: senza, un paragrafo scritto di getto
                    // diventa una riga unica e il server rifiuta con "441 Line too long"
                    //
                    // 1.4.8 (par. B6b): con l'impostazione accesa si manda a capo
                    // in Format=Flowed invece che con a-capo rigidi. E' l'UNICA
                    // differenza fra i due rami, ed e' uno spazio in fondo alle
                    // righe che spezziamo noi. Il `Content-Type` lo dichiara da
                    // se' in `postArticle`, guardando il corpo: non si passa
                    // nessuna bandiera, cosi' l'intestazione non puo' dire una
                    // cosa diversa da quella che spediamo.
                    corpoDaInviare(bodyField.text, AppSettings.formatFlowedInUscita),
                    referencesHeader,
                    // Correzione 1.2.9 (segnalazione di Marco): campo User-Agent
                    // vuoto nell'identità doveva significare "nessun header
                    // dichiarato" (è quello che dice il testo di aiuto sotto il
                    // campo, ed è quello che postArticle sa già fare — vedi il
                    // commento "privacy: User-Agent solo se dichiarato" in
                    // Repository.kt). Il fallback ifBlank{predefinito} qui
                    // lo tradiva SEMPRE, rendendo l'anonimato impossibile da
                    // ottenere pur svuotando esplicitamente il campo e salvando.
                    // Ora si passa l'identità così com'è: vuoto resta vuoto.
                    userAgent = ident.userAgent,
                    xNoArchive = ident.xNoArchive,
                    customHeaders = customHeaders,
                    faceB64 = ident.faceB64,
                    supersedesId = supersedesId,
                    extraGroups = extraGroups,
                    // 1.4: se l'autore ha chiesto che si prosegua altrove e
                    // l'utente non ha deciso diversamente, e' la sua lista a
                    // finire in Newsgroups — non il gruppo che stiamo leggendo.
                    gruppiEspliciti = gruppiFollowup,
                )
            }.onSuccess {
                // spedito: la bozza non serve più
                runCatching { repo.deleteDraft(bozzaId) }
                // A (richiesta Bingo3331): conferma esplicita "inviato" + refresh
                // automatico del gruppo, così il proprio post compare senza
                // refresh manuale. Il Toast sopravvive alla navigazione (a
                // differenza dello snackbar della schermata, che sparirebbe col
                // rientro). Il sync gira sull'appScope: se usassimo lo scope
                // della schermata, chiudendola verrebbe annullato. Best-effort:
                // la propagazione lato server (es. Astraweb) può ritardare la
                // comparsa; quando l'articolo arriva, la lista (Flow da Room) si
                // aggiorna da sola.
                Toast.makeText(ctx, ctx.getString(R.string.cmp_sent), Toast.LENGTH_SHORT).show()
                (ctx.applicationContext as? App)?.appScope?.launch {
                    runCatching { repo.syncGroup(gid) }
                }
                nav.indietroSicuro()
            }.onFailure {
                // 1.5.1 (T2): l'invio non e' andato, la bozza torna a proteggere il testo.
                guardia.riapri()
                // 1.5.5 (voce 15, il 441 di Ammammata): il testo del server
                // resta intero; se e' un 441 si aggiunge una riga che dice cosa
                // fare, e l'avviso resta a schermo piu' a lungo per leggerla.
                val spiegazione = spiegazioneRifiutoInvio(it.message)
                val base = ctx.getString(R.string.cmp_send_failed, it.message ?: "")
                if (spiegazione != null) {
                    snackbar.showSnackbar(
                        base + "\n" + ctx.getString(spiegazione),
                        duration = SnackbarDuration.Long,
                    )
                } else {
                    snackbar.showSnackbar(base)
                }
            }
            sending = false
        }
    }

    if (chiediScarta) {
        AlertDialog(
            onDismissRequest = { chiediScarta = false },
            title = { Text(stringResource(R.string.cmp_discard_title)) },
            text = { Text(stringResource(R.string.cmp_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    chiediScarta = false
                    scope.launch {
                        guardia.chiudi()
                        lucchettoBozza.withLock { runCatching { repo.deleteDraft(bozzaId) } }
                        nav.indietroSicuro()
                    }
                }) { Text(stringResource(R.string.cmp_discard_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { chiediScarta = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    Scaffold(
        // CORREZIONE beta12 (regressione grave, segnalata da Marco: testo che
        // spariva sotto la tastiera anche in PORTRAIT, non solo landscape).
        //
        // In beta11 avevo tolto imePadding() partendo dalla teoria che
        // adjustResize (manifest) da solo bastasse, e che i due insieme
        // sottraessero spazio due volte. Teoria plausibile ma MAI VERIFICATA
        // su device reale (nessun ambiente Android nel sandbox) — e questa
        // segnalazione dice che era sbagliata, o comunque che rimuoverlo ha
        // peggiorato le cose. Ripristinato.
        //
        // Sospetto per la prossima volta, se dovesse ripresentarsi: targetSdk
        // 35 (Android 15) cambia il comportamento di adjustResize verso
        // l'edge-to-edge obbligatorio — su quelle versioni il resize
        // "automatico" del manifest potrebbe non bastare più da solo, ed è
        // Compose (imePadding) a doversene occupare esplicitamente. Non è una
        // certezza, è un'ipotesi da tenere a mente se il problema torna anche
        // CON imePadding presente.
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(if (replyToArticleId != null) stringResource(R.string.cmp_reply) else stringResource(R.string.cmp_new_thread), maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = { nav.indietroSicuro() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_cancel))
                    }
                },
                actions = {
                    // 1.3.3 (Roxy): INCOLLA con un clic.
                    //
                    // In lettura il problema era che la barra di selezione non
                    // compare col puntatore; qui e' l'altra meta' della stessa
                    // richiesta ("COPIA/INCOLLA").
                    //
                    // 1.4.4: il pulsante e tutta la sua storia stanno ora in
                    // ui/Appunti.kt, perche' da questa versione lo usa anche il
                    // pannello di scrittura della vista a pannelli, che non
                    // l'aveva mai avuto. Estratto, non ricopiato.
                    PulsanteIncolla(
                        testo = bodyField,
                        onTesto = { bodyField = it },
                        appunti = appunti,
                        snackbar = snackbar,
                    )
                    if (parent != null) {
                        // beta17 (Bingo3331): se il messaggio a cui rispondo è
                        // crospostato, mostro il pulsante per scegliere i gruppi
                        // di risposta. crosspostGroups da ThreadFlatten.kt.
                        // 1.4.6 (par. A1): la condizione guardava SOLO i
                        // Newsgroups dell'originale, quindi su un messaggio NON
                        // crospostato con Followup-To altrove il pulsante non
                        // compariva — e l'avviso sopra diceva «apri il selettore
                        // dei gruppi se vuoi decidere tu» indicando una cosa che
                        // non c'era. Trovato da Marco il 1 ottobre 2026
                        // chiedendo, davanti al Test 9A, dove fosse quel
                        // selettore: non lo cercava male, non c'era.
                        //
                        // E' la TERZA volta per questa stessa forma: le note
                        // della 1.4.2 raccontano il selettore assente aprendo
                        // una discussione nuova, «stessa forma di difetto
                        // corretta nella 1.4 per le risposte, rimasta scoperta
                        // sull'altro ramo». Il ramo scoperto, stavolta, era il
                        // messaggio con un solo gruppo.
                        //
                        // Senza Followup-To la condizione e' identica a prima:
                        // gruppiOffertiNelSelettore e' l'identita' su quel caso.
                        if (gruppiOffertiNelSelettore(
                                destinazioneAutore, crosspostGroups(parent?.headersRaw)).size > 1) {
                            IconButton(onClick = { showCrosspost = true }) {
                                Icon(Icons.Default.Groups, contentDescription = stringResource(R.string.cmp_crosspost_reply_cd))
                            }
                        }
                    } else {
                        // beta18 (Marco/Bingo3331): crosspost anche per NUOVO
                        // thread. Pulsante sempre visibile; si colora di primario
                        // quando sono già stati scelti gruppi extra.
                        IconButton(onClick = { showCrosspostNuovo = true }) {
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
                    if (parent != null) {
                        // se nel corpo ci sono righe citate da togliere.
                        if (quotedLineIndices(bodyField.text).isNotEmpty()) {
                            IconButton(onClick = { showTrim = true }) {
                                Icon(Icons.Default.ContentCut, contentDescription = stringResource(R.string.cmp_trim_cd))
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
                                Text(
                                    stringResource(R.string.cmp_quotemode_note),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                                REPLY_MODES.forEach { (mode, _) ->
                                    DropdownMenuItem(
                                        text = { Text(etichettaModalitaCitazione(mode)) },
                                        leadingIcon = {
                                            RadioButton(selected = replyMode == mode, onClick = null)
                                        },
                                        onClick = {
                                            replyMode = mode
                                            prefs.edit().putString("reply_mode", mode).apply()
                                            identity?.let { ident ->
                                                bodyField = buildPrefill(
                                                    ident, mode, parent,
                                                    groupName = groupName,
                                                    defaultIntro = introPredefinita,
                                                    introGruppo = introGruppo,
                                                )
                                            }
                                            modeMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            // barra in basso: con adjustResize resta SEMPRE sopra la tastiera,
            // così il pulsante Invia è raggiungibile senza chiudere la tastiera
            BottomAppBar {
                IconButton(
                    enabled = bodyField.text.isNotBlank() || subject.isNotBlank(),
                    onClick = {
                        scope.launch {
                            salvaBozza()
                            snackbar.showSnackbar(ctx.getString(R.string.cmp_draft_saved))
                        }
                    },
                ) {
                    Icon(Icons.Default.Save, contentDescription = stringResource(R.string.cmp_save_draft_cd))
                }
                // 1.5.1 (T3): con l'autosalvataggio, uscire LASCIA una bozza.
                // Per uscire senza lasciare traccia serve un gesto esplicito.
                IconButton(onClick = { chiediScarta = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cmp_discard_cd))
                }
                IconButton(onClick = {
                    // ROT13 sulla SELEZIONE se c'è, altrimenti su tutto il testo.
                    // Serve a nascondere una battuta o uno spoiler lasciando
                    // leggibile il resto, come si è sempre fatto su Usenet.
                    val sel = bodyField.selection
                    bodyField = if (!sel.collapsed) {
                        val inizio = minOf(sel.start, sel.end)
                        val fine = maxOf(sel.start, sel.end)
                        val testo = bodyField.text
                        bodyField.copy(
                            text = testo.substring(0, inizio) +
                                rot13(testo.substring(inizio, fine)) +
                                testo.substring(fine),
                            // la selezione resta dov'era: si può ripremere per tornare indietro
                            selection = TextRange(inizio, fine),
                        )
                    } else {
                        bodyField.copy(text = rot13(bodyField.text))
                    }
                }) {
                    Icon(
                        Icons.Default.EnhancedEncryption,
                        contentDescription = if (bodyField.selection.collapsed)
                            stringResource(R.string.cmp_rot13_all) else stringResource(R.string.cmp_rot13_sel),
                    )
                }
                Text(
                    stringResource(R.string.cmp_chars, bodyField.text.length),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Button(
                    enabled = canSend,
                    onClick = { doSend() },
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(R.string.cmp_send))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                // 1.4.9 (§K1) — LA COLONNA SCORRE, E IL CAMPO SMETTE DI ESSERE
                // SCHIACCIATO.
                //
                // Prima questa colonna non scorreva e il campo del messaggio
                // prendeva `weight(1f)`: quello che avanzava, qualunque cosa
                // fosse. Su uno schermo piccolo, con i caratteri grandi e una
                // tastiera alta, quello che avanzava era mezza riga — e il
                // `heightIn(min = 120.dp)` del campo NON lo proteggeva, perche'
                // un minimo non puo' chiedere piu' del massimo che riceve, e il
                // peso gli imponeva un'altezza fissa.
                //
                // ⚠️ Percio' adesso niente ha piu' un peso qui dentro: in una
                // colonna scorrevole l'altezza disponibile e' infinita, e un
                // figlio con `weight` la farebbe cadere con «Vertically
                // scrollable component was measured with an infinity maximum
                // height». E' il vincolo che tiene insieme le due modifiche,
                // non un dettaglio: togliere lo scorrimento senza rimettere il
                // peso, o viceversa, rompe la schermata.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            // spaziatura di contenitore: evita che l'etichetta di un campo
            // finisca sopra l'elemento precedente (vedi IdentityScreen)
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sending) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    // beta13 (segnalato da Marco): tolta l'etichetta descrittiva
                    // della modalità di risposta ("Scrivi sotto la citazione
                    // (netiquette)" ecc.) — su smartphone in verticale con la
                    // tastiera aperta rubava tre righe di spazio prezioso alla
                    // scrittura. Resta solo il gruppo; la modalità di risposta
                    // si vede/cambia comunque in "Mittente e intestazioni".
                    // 1.4 (Dave Royal): quando i gruppi di destinazione sono
                    // PIÙ DI UNO si mostrano TUTTI. Prima compariva solo quello
                    // che si stava leggendo, e chi rispondeva a un crosspost non
                    // aveva modo di sapere dove sarebbe uscito il messaggio.
                    // Con un gruppo solo il testo resta identico a prima.
                    // 1.4.6 (§A13.1): la scelta e' estratta in etichettaDestinazioni
                    // (CrosspostReply.kt) perche' era identica nelle due viste, e
                    // in tutte e due mostrava il gruppo di LETTURA invece della
                    // destinazione quando il Followup-To ne indicava uno solo.
                    etichettaDestinazioni(destinazioni),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showDetails = !showDetails }) {
                    Text(if (showDetails) stringResource(R.string.cmp_hide_details) else stringResource(R.string.cmp_show_details))
                }
            }

            RigaIdentita(repo, identity, fromName, fromEmail)
            // 1.4: l'avviso del Followup-To. Non è un dettaglio ripiegabile:
            // dice che il messaggio esce in un posto diverso da quello che
            // l'utente sta guardando, e deve vedersi senza aprire nulla.
            AvvisoFollowupTo(
                destinazione = destinazioneInVigore,
                gruppoCorrente = groupName,
                indirizzoAutore = indirizzoAutore,
                onCopiaIndirizzo = { appunti.copia(it) },
                onApriPosta = { apriAppDiPosta(ctx, it) },
            )

            // dettagli ripiegabili: lasciano tutto lo spazio all'editor del messaggio
            if (showDetails) {
                OutlinedTextField(
                    value = fromName, onValueChange = { fromName = it },
                    label = { Text(stringResource(R.string.cmp_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = fromEmail, onValueChange = { fromEmail = it },
                    label = { Text(stringResource(R.string.cmp_email)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = customHeaders, onValueChange = { customHeaders = it },
                    label = { Text(stringResource(R.string.cmp_headers)) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                // 1.4.1 (105/G, Dave Royal): dire IN ANTICIPO cosa verra'
                // scartato. La regola e' la stessa che usa l'invio — vedi
                // data/IntestazioniPersonalizzate — quindi l'avviso non puo'
                // divergere da cio' che succede davvero.
                val scartate = IntestazioniPersonalizzate.scartate(customHeaders)
                if (scartate.isNotEmpty()) {
                    Text(
                        stringResource(R.string.hdr_riservate_scartate, scartate.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            OutlinedTextField(
                value = subject, onValueChange = { subject = it },
                label = { Text(stringResource(R.string.cmp_subject)) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )

            // il corpo prende tutta l'altezza residua e scorre AL SUO INTERNO:
            // il cursore resta sempre in vista e la citazione sopra è editabile
            //
            // wishlist beta12 (Roxy/Marco): richiesta ESPLICITA di scroll-to-
            // cursor ad ogni digitazione, oltre al comportamento implicito di
            // Compose — che con digitazione veloce (tastiera fisica/wifi, non
            // touch) può restare indietro di un frame. Vedi stesso fix su
            // ComposePane in ThreePaneHome.kt, dove il problema era più grave
            // perché il campo lì non ha nessun limite di altezza.
            val corpoInVista = remember { BringIntoViewRequester() }
            OutlinedTextField(
                value = bodyField,
                onValueChange = { nuovo ->
                    bodyField = nuovo
                    scope.launch {
                        withFrameNanos { }
                        runCatching { corpoInVista.bringIntoView() }
                    }
                },
                label = { Text(stringResource(R.string.cmp_message)) },
                // beta19 (Marco): maiuscola automatica a inizio frase e dopo il
                // punto, come nelle app di messaggistica. autoCorrect resta OFF:
                // su Usenet si scrivono spesso sigle, nomi di gruppi e comandi
                // che la correzione automatica rovinerebbe.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = false,
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .fillMaxWidth()
                    // 1.4.9 (§K1) — IL MINIMO ADESSO E' UN MINIMO.
                    //
                    // PERCHE' ESISTE. Un utente su Samsung J6 (720x1480, 5,6",
                    // tastiera Samsung con barra dei suggerimenti) non vedeva
                    // quello che scriveva: le lettere arrivavano TAGLIATE A META'
                    // in altezza. Non era la tastiera a coprire il campo — la
                    // barra con «Invia» stava al suo posto sopra di essa, quindi
                    // l'ingombro era dichiarato bene — era il campo a ricevere
                    // pochi pixel.
                    //
                    // ⚠️ E il commento che stava qui diceva «altezza minima
                    // garantita». Non lo era: con `weight(1f)` il padre misurava
                    // questo campo con un'altezza FISSA pari allo spazio
                    // distribuito, e `heightIn(min = 120.dp)` non puo' chiedere
                    // piu' del massimo che riceve. Il minimo veniva schiacciato
                    // in silenzio, e nessuno dei due lo sapeva.
                    //
                    // *Un commento che dichiara una garanzia non e' una
                    // garanzia.* E' il parente stretto di §A16: un'impostazione
                    // spiegata solo in un documento non e' spiegata.
                    //
                    // DIAGNOSI CONFERMATA DUE VOLTE DA FUORI. (1) Lo stesso
                    // utente, con una tastiera BASSA (quella di KeePass, senza
                    // suggerimenti ne' riga numeri), riesce a scrivere: quindi
                    // e' una quantita' di spazio verticale, non un difetto della
                    // tastiera Samsung — quella e' solo la condizione che lo
                    // scopre. (2) Marco l'ha riprodotto sul proprio Motorola
                    // Edge 50 Neo portando i caratteri al massimo e aprendo i
                    // dettagli: «con quelli aperti non vedo nemmeno mezza riga».
                    //
                    // IL MASSIMO NON E' UN CAPRICCIO. Dentro una colonna
                    // scorrevole l'altezza e' illimitata, e un campo di testo
                    // senza tetto cresce fino a contenere TUTTO: rispondere a un
                    // messaggio lungo vorrebbe dire disegnare per intero un
                    // campo alto migliaia di pixel, che su un J6 e' un problema
                    // peggiore di quello che stiamo togliendo. Col tetto il
                    // campo resta limitato e continua a scorrere al proprio
                    // interno, come ha sempre fatto.
                    .heightIn(min = 160.dp, max = 480.dp)
                    .padding(bottom = 8.dp)
                    // ⚠️ Da questa versione questa richiesta ha finalmente un
                    // antenato scorrevole su cui agire: prima la colonna non
                    // scorreva, e «portami in vista» aveva ben poco da muovere.
                    .bringIntoViewRequester(corpoInVista),
            )
        }
    }

    if (showTrim) {
        TrimQuoteDialog(
            body = bodyField.text,
            onResult = { nuovo -> bodyField = nuovo; showTrim = false },
            onDismiss = { showTrim = false },
        )
    }

    if (showCrosspost) {
        // beta17: selettore gruppi di risposta. Salva la scelta in extraGroups,
        // che verrà passato a postArticle all'invio. Non invia da qui.
        CrosspostReplyDialog(
            // 1.4.6 (par. A1): il selettore parte dalla richiesta dell'autore.
            // Senza Followup-To questi due valori sono identici a prima.
            currentGroup = gruppoObbligatorio(groupName, destinazioneAutore),
            allGroups = gruppiOffertiNelSelettore(
                destinazioneAutore, crosspostGroups(parent?.headersRaw)),
            // 1.4: se una scelta c'è già, il selettore riparte da QUELLA.
            giaScelti = if (crosspostScelto) extraGroups else null,
            gruppiAutore = gruppiChiestiDallAutore(destinazioneAutore),
            onConfirm = { extra ->
                extraGroups = extra
                crosspostScelto = true
                showCrosspost = false
            },
            onDismiss = { showCrosspost = false },
        )
    }

    if (showCrosspostNuovo) {
        // beta18 (Marco/Bingo3331): selettore gruppi extra per NUOVO thread.
        CrosspostNuovoThreadDialog(
            currentGroup = groupName,
            subscribedGroups = gruppiSottoscritti.map { it.group.name },
            giaScelti = if (crosspostScelto) extraGroups else null,
            onConfirm = { extra ->
                extraGroups = extra
                crosspostScelto = true
                showCrosspostNuovo = false
            },
            onDismiss = { showCrosspostNuovo = false },
        )
    }
}
