/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Manutenzione: backup/ripristino (con cifratura opzionale a password) e
 * pulizia automatica dei messaggi vecchi già letti.
 */
package org.openusenet.reader.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.ArrowBack
import org.openusenet.reader.ui.icone.Download
import org.openusenet.reader.ui.icone.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.openusenet.reader.ui.rememberAppunti
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.openusenet.reader.Branding
import org.openusenet.reader.BuildConfig
import org.openusenet.reader.AppSettings
import org.openusenet.reader.CrashLog
import org.openusenet.reader.R
import org.openusenet.reader.backup.BackupCrypto
import org.openusenet.reader.backup.BackupLocale
import org.openusenet.reader.backup.BackupManager
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.nntp.NntpLog
import org.openusenet.reader.ui.cartellaConservaSalvata
import org.openusenet.reader.ui.descriviCartellaConserva
import org.openusenet.reader.ui.apriSeEsiste
import org.openusenet.reader.ui.indietroSicuro
import org.openusenet.reader.ui.rememberConserva
import org.openusenet.reader.update.UpdateChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(repo: NewsRepository, nav: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    // 1.3.5: un solo punto in tutta l'app legge gli appunti (ui/Appunti.kt).
    val appunti = rememberAppunti()
    var backupPw by remember { mutableStateOf("") }
    var pendingEncrypted by remember { mutableStateOf<String?>(null) }
    /**
     * 1.3.7: il backup GIA' LEGGIBILE (in chiaro, o appena decifrato) in attesa
     * che l'utente scelga COME ripristinarlo. Vedi BackupManager.ModoRipristino.
     */
    var pendingImport by remember { mutableStateOf<String?>(null) }
    /**
     * 1.3.7 (Dave Royal, 17 settembre 2026): il DETTAGLIO TECNICO di una
     * lettura fallita, da mostrare invece di nasconderlo. Vedi il commento sul
     * launcher di importazione qui sotto.
     */
    var erroreLettura by remember { mutableStateOf<String?>(null) }
    // 1.4: Diagnostica database — il rapporto da mostrare, e la guardia contro
    // due calcoli in parallelo (dieci COUNT su una tabella articoli grande non
    // sono istantanei).
    var rapportoDb by remember { mutableStateOf<String?>(null) }
    var dbInCorso by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    /*
     * 1.5.6 / 174 (Roxy, box TV senza selettore di file): il percorso del
     * backup appena salvato nella cartella dell'app, da mostrare; e l'elenco dei
     * backup presenti li', quando il ripristino non puo' aprire il selettore.
     * Vedi BackupLocale.kt.
     */
    var backupLocaleSalvato by remember { mutableStateOf<String?>(null) }
    var backupLocali by remember { mutableStateOf<List<java.io.File>?>(null) }
    // 1.2.5 (Bingo3331): controllo aggiornamenti manuale.
    var aggiornamentoTrovato by remember { mutableStateOf<String?>(null) }
    var controlloInCorso by remember { mutableStateOf(false) }
    // 1.2.8 (Fase 2, HAL/Bingo3331): diagnostica LISTGROUP manuale — elenco
    // gruppi sottoscritti da mostrare nel dialog di scelta, e stato "in corso"
    // per non far partire due diagnostiche in parallelo sullo stesso server.
    var gruppiPerDiagnostica by remember { mutableStateOf<List<Pair<org.openusenet.reader.data.Group, String>>>(emptyList()) }
    var showListgroupDialog by remember { mutableStateOf(false) }
    var listgroupInCorso by remember { mutableStateOf(false) }
    // 1.4.2 (build 114): la maniglia per fermare la diagnostica in corso. Vive
    // qui, accanto allo stato "in corso", e non dentro il Repository: chi la
    // deve azionare e' questa schermata. Ne nasce una nuova a ogni avvio,
    // perche' [AnnullamentoDiagnostica.annullato] non torna mai falso.
    var annullaListgroup by remember { mutableStateOf<org.openusenet.reader.data.AnnullamentoDiagnostica?>(null) }
    // 1.4.2 (build 114), voce A3: la pulizia degli orfani. Il conteggio e' il
    // preventivo che si mostra PRIMA di cancellare, sullo stampo del dialogo
    // di cancellazione server della 113; null = nessun dialogo aperto.
    var orfaniDaConfermare by remember { mutableStateOf<List<Pair<String, Int>>?>(null) }
    var orfaniInCorso by remember { mutableStateOf(false) }

    /*
     * 1.4 — il testo della Diagnostica database.
     *
     * Volutamente in colonne a larghezza fissa e senza traduzione dei nomi di
     * tabella: questo testo esiste per essere COPIATO dentro una segnalazione.
     * Chi lo legge deve poter confrontare due rapporti a colpo d'occhio — prima
     * e dopo un aggiornamento — e sapere di quale tabella si parla senza
     * tradurre all'indietro.
     *
     * Il WAL si riporta a parte: un database appena sincronizzato puo' avere
     * megabyte di scritture ancora nel journal, e chi guarda solo il .db si
     * chiede dove siano finiti.
     */
    suspend fun costruisciRapportoDb(): String {
        val conteggi = repo.conteggiDatabase()
        // 1.4.2 (build 113): righe rimaste senza il loro server o il loro
        // gruppo. Vuoto su un'installazione sana — e allora il blocco non
        // compare affatto.
        val orfani = repo.conteggiOrfani()
        val dichiarata = org.openusenet.reader.data.AppDatabase.VERSIONE_SCHEMA_DICHIARATA
        val suDisco = repo.versioneSchemaSuDisco()
        val file = ctx.getDatabasePath(org.openusenet.reader.data.AppDatabase.NOME_FILE)
        val wal = java.io.File(file.path + "-wal")
        fun kb(b: Long) = "%,d kB".format(java.util.Locale.US, (b + 1023) / 1024)
        return buildString {
            append(Branding.APP_NAME).append(' ').append(BuildConfig.VERSION_NAME)
            append(" (build ").append(BuildConfig.VERSION_CODE).append(")\n")
            append("schema dichiarato: ").append(dichiarata).append('\n')
            append("schema sul file:   ").append(suDisco)
            if (suDisco != dichiarata) append("   <-- NON COINCIDONO")
            append('\n')
            append(org.openusenet.reader.data.AppDatabase.NOME_FILE).append(": ")
            append(if (file.exists()) kb(file.length()) else "assente")
            if (wal.exists()) append("   WAL: ").append(kb(wal.length()))
            append("\n\n")
            for ((nome, n) in conteggi) {
                append(nome.padEnd(22)).append("%,d".format(java.util.Locale.US, n)).append('\n')
            }
            // 1.4.2 (build 113): il blocco esiste solo se c'e' qualcosa da
            // dire. Nomi di tabella non tradotti come sopra, e per la stessa
            // ragione: questo testo nasce per finire dentro una segnalazione.
            if (orfani.isNotEmpty()) {
                append("\nORFANI (da un server cancellato prima della 1.4.2)\n")
                for ((nome, n) in orfani) {
                    append(nome.padEnd(30)).append("%,d".format(java.util.Locale.US, n)).append('\n')
                }
            }
        }
    }

    suspend fun runImport(json: String, modo: BackupManager.ModoRipristino) {
        runCatching { BackupManager.import(repo, ctx, json, modo) }
            .onSuccess {
                // 1.4: la coda sui nomi utente riparati compare SOLO se ce n'erano.
                // In un'installazione sana il messaggio resta quello di sempre.
                val base = ctx.getString(R.string.mnt_restored, it.servers, it.subscriptions, it.filters)
                snackbar.showSnackbar(
                    if (it.usernameRiparati > 0) {
                        base + " " + ctx.getString(R.string.mnt_username_riparati, it.usernameRiparati)
                    } else {
                        base
                    },
                )
            }
            .onFailure { snackbar.showSnackbar(ctx.getString(R.string.mnt_import_err, it.message ?: "")) }
    }

    // 1.5.6 / 174: il contenuto del backup, un posto solo per le due strade
    // (selettore di sistema, o cartella dell'app quando il selettore manca).
    suspend fun contenutoBackup(pw: String): String {
        val json = BackupManager.export(repo, ctx, includePasswords = pw.isNotBlank())
        return if (pw.isNotBlank()) BackupCrypto.encrypt(json, pw) else json
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            val pw = backupPw
            runCatching {
                val out = contenutoBackup(pw)
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(out.toByteArray()) }
            }.onSuccess {
                snackbar.showSnackbar(
                    if (pw.isNotBlank()) ctx.getString(R.string.mnt_backup_enc_ok) else ctx.getString(R.string.mnt_backup_ok),
                )
            }.onFailure { snackbar.showSnackbar(ctx.getString(R.string.mnt_export_err, it.message ?: "")) }
        }
    }
    // Diagnostica: salva il dialogo NNTP registrato come file di testo.
    val exportLogLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(NntpLog.snapshot().toByteArray()) }
            }.onSuccess {
                snackbar.showSnackbar(ctx.getString(R.string.mnt_diag_saved))
            }.onFailure { snackbar.showSnackbar(ctx.getString(R.string.mnt_export_err, it.message ?: "")) }
        }
    }
    // 1.2.9.b (Piano C.1): salva l'ultimo arresto anomalo registrato, stesso
    // schema copia/esporta della diagnostica NNTP qui sopra.
    val exportCrashLogLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(CrashLog.snapshot(ctx).toByteArray()) }
            }.onSuccess {
                snackbar.showSnackbar(ctx.getString(R.string.mnt_crash_saved))
            }.onFailure { snackbar.showSnackbar(ctx.getString(R.string.mnt_export_err, it.message ?: "")) }
        }
    }
    /*
     * 1.5.6 / 174: cosa fare col testo di un backup appena letto, un posto solo
     * per le due strade (selettore di sistema, o elenco della cartella
     * dell'app). La logica e' quella della 1.3.7, spostata qui non ricopiata:
     * [nulloSenzaEccezione] e' la frase per il caso «nessun testo, nessuna
     * eccezione», che dipende da come si e' letto.
     */
    fun instradaBackupLetto(esito: Result<String?>, nulloSenzaEccezione: String) {
        val text = esito.getOrNull()
        if (text == null) {
            erroreLettura = esito.exceptionOrNull()
                ?.let { "${it.javaClass.simpleName}: ${it.message ?: "(nessun messaggio)"}" }
                ?: nulloSenzaEccezione
        } else if (BackupCrypto.isEncrypted(text)) {
            pendingEncrypted = text
        } else {
            pendingImport = text
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            /*
             * 1.3.7 — "File illeggibile" SMETTE DI NASCONDERE LA CAUSA.
             *
             * Dave Royal, 17 settembre 2026: sei tentativi su due versioni
             * dell'app, un backup rinominato che il selettore mostra
             * regolarmente come "JSON document" ma che non si apre. Tutto cio'
             * che poteva dirci era "unreadable file", perche' e' tutto cio' che
             * gli mostravamo: `runCatching { ... }.getOrNull()` faceva
             * collassare FileNotFoundException, SecurityException e uno stream
             * nullo nella STESSA frase.
             *
             * Sono cause diverse con rimedi diversi, e chi paga la diagnosi
             * mancata e' il tester, in tempo suo. E' la terza volta che questo
             * progetto incontra la stessa forma di difetto: il messaggio TLS
             * della 1.3.3 che proponeva un rimedio inesistente, e il "password
             * errata" che in questa stessa release copriva anche gli errori di
             * importazione.
             *
             * Nota sul ramo "stream nullo": openInputStream puo' restituire
             * null SENZA lanciare nulla. E' un caso distinto e va detto, non
             * confuso con un'eccezione.
             */
            instradaBackupLetto(
                runCatching {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                },
                "openInputStream: null",
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mnt_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.indietroSicuro() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        // beta20: imePadding sul contenitore esterno (vedi ServersScreen).
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
        ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            // spaziatura di contenitore: evita che l'etichetta di un campo
            // finisca sopra l'elemento precedente (vedi IdentityScreen)
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.mnt_backup_section), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.mnt_backup_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { backupPw = ""; showExportDialog = true }) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.mnt_export))
                }
                FilledTonalButton(onClick = {
                    // 1.5.6 / 174: senza selettore, i backup della cartella dell'app.
                    if (!importLauncher.apriSeEsiste(arrayOf("application/json", "text/*", "*/*"))) {
                        backupLocali = BackupLocale.elenco(ctx)
                    }
                }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.mnt_restore))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            Text(stringResource(R.string.mnt_autoclean), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.mnt_autoclean_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            listOf(
                0 to stringResource(R.string.mnt_never),
                30 to stringResource(R.string.mnt_days_30),
                90 to stringResource(R.string.mnt_days_90),
                180 to stringResource(R.string.mnt_days_180),
                365 to stringResource(R.string.mnt_year_1),
            ).forEach { (days, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        AppSettings.autoPurgeDays = days; AppSettings.save()
                    },
                ) {
                    RadioButton(
                        selected = AppSettings.autoPurgeDays == days,
                        onClick = { AppSettings.autoPurgeDays = days; AppSettings.save() },
                    )
                    Text(label)
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val d = if (AppSettings.autoPurgeDays > 0) AppSettings.autoPurgeDays else 90
                        val n = repo.purgeOld(d)
                        snackbar.showSnackbar(ctx.getString(R.string.mnt_cleaned, n, d))
                    }
                },
                modifier = Modifier.padding(top = 12.dp),
            ) { Text(stringResource(R.string.mnt_clean_now)) }

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            Text(stringResource(R.string.mnt_headers_section), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.mnt_headers_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            listOf(
                100 to stringResource(R.string.mnt_headers_100),
                300 to stringResource(R.string.mnt_headers_300),
                500 to stringResource(R.string.mnt_headers_500),
                1000 to stringResource(R.string.mnt_headers_1000),
                2000 to stringResource(R.string.mnt_headers_2000),
                0 to stringResource(R.string.mnt_headers_all),
            ).forEach { (n, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        AppSettings.maxHeadersSync = n; AppSettings.save()
                    },
                ) {
                    RadioButton(
                        selected = AppSettings.maxHeadersSync == n,
                        onClick = { AppSettings.maxHeadersSync = n; AppSettings.save() },
                    )
                    Text(label)
                }
            }
            Text(
                stringResource(R.string.mnt_headers_all_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            // wishlist beta11 (richiesto da Roxy, caso TV Box dove "scarica tutti i
            // corpi" manuale è l'unico metodo praticabile): scaricamento automatico
            // dei corpi ad ogni apertura gruppo, con tetto a discrezione dell'utente
            // — non una lista di preset come sopra, un valore libero.
            Text(stringResource(R.string.mnt_bodies_section), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.scaricaCorpiAutomatico,
                    onCheckedChange = { AppSettings.scaricaCorpiAutomatico = it; AppSettings.save() },
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.mnt_bodies_switch))
            }
            Text(
                stringResource(R.string.mnt_bodies_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            var maxCorpiTesto by remember(AppSettings.maxCorpiPerApertura) {
                mutableStateOf(AppSettings.maxCorpiPerApertura.toString())
            }
            OutlinedTextField(
                value = maxCorpiTesto,
                onValueChange = { nuovo ->
                    maxCorpiTesto = nuovo.filter { it.isDigit() }
                    maxCorpiTesto.toIntOrNull()?.takeIf { it > 0 }?.let {
                        AppSettings.maxCorpiPerApertura = it
                        AppSettings.save()
                    }
                },
                label = { Text(stringResource(R.string.mnt_bodies_max)) },
                singleLine = true,
                enabled = AppSettings.scaricaCorpiAutomatico,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            Text(stringResource(R.string.mnt_alerts_section), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.syncOnStart,
                    onCheckedChange = { AppSettings.syncOnStart = it; AppSettings.save() },
                )
                Text(
                    stringResource(R.string.mnt_syncstart_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                stringResource(R.string.mnt_syncstart_note2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 8.dp),
            )
            // 1.2.10 (voce Y di NeReO_bug_1.2.9.md, Big Umberto tramite Marco):
            // switch dedicato per il sync incondizionato di UN gruppo alla sua
            // apertura — diverso da syncOnStart qui sopra (quello sincronizza
            // TUTTI i gruppi, solo all'avvio del processo). Default ON: chi non
            // ha mai segnalato il problema non nota alcun cambiamento.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.syncOnGroupOpen,
                    onCheckedChange = { AppSettings.syncOnGroupOpen = it; AppSettings.save() },
                )
                Text(
                    stringResource(R.string.mnt_syncgroupopen_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                stringResource(R.string.mnt_syncgroupopen_note2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 8.dp),
            )
            // 1.4: il "letto" segue il Message-ID. Predefinito ACCESO — e' il
            // comportamento di MesNews, misurato da Marco che lo usa da PC.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.lettoSegueMessageId,
                    onCheckedChange = { AppSettings.lettoSegueMessageId = it; AppSettings.save() },
                )
                Text(
                    stringResource(R.string.mnt_letto_messageid),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                stringResource(R.string.mnt_letto_messageid_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.dingOnReply,
                    onCheckedChange = { AppSettings.dingOnReply = it; AppSettings.save() },
                )
                Text(
                    stringResource(R.string.mnt_sound_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                stringResource(R.string.mnt_autocheck),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            listOf(
                0 to stringResource(R.string.mnt_off),
                3 to stringResource(R.string.mnt_every_3),
                5 to stringResource(R.string.mnt_every_5),
                10 to stringResource(R.string.mnt_every_10),
            ).forEach { (m, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { AppSettings.autoCheckMinutes = m; AppSettings.save() },
                ) {
                    RadioButton(
                        selected = AppSettings.autoCheckMinutes == m,
                        onClick = { AppSettings.autoCheckMinutes = m; AppSettings.save() },
                    )
                    Text(label)
                }
            }
            Text(
                stringResource(R.string.mnt_autocheck_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            // 1.2.5 (Bingo3331): controllo nuove versioni su SourceForge. Solo
            // avviso + link, mai download automatico; una GET a un URL
            // pubblico, senza dati utente.
            //
            // 1.2.8 (Bingo3331 + HAL): da interruttore ON/OFF a modalità a
            // quattro vie (Mai / Una volta al giorno / Ogni 4 ore / A ogni
            // apertura), più l'indicatore "Ultimo controllo…" sotto — così il
            // meccanismo si vede funzionare anche quando non trova nulla di
            // nuovo (nessun popup in quel caso, per non snervare l'utente:
            // solo questo testo).
            Text(stringResource(R.string.upd_section), style = MaterialTheme.typography.titleMedium)
            listOf(
                "never" to stringResource(R.string.upd_mode_never),
                "daily" to stringResource(R.string.upd_mode_daily),
                "4h" to stringResource(R.string.upd_mode_4h),
                "always" to stringResource(R.string.upd_mode_always),
            ).forEach { (mode, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { AppSettings.autoCheckMode = mode; AppSettings.save() },
                ) {
                    RadioButton(
                        selected = AppSettings.autoCheckMode == mode,
                        onClick = { AppSettings.autoCheckMode = mode; AppSettings.save() },
                    )
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                testoUltimoControllo(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                stringResource(R.string.upd_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            OutlinedButton(
                enabled = !controlloInCorso,
                onClick = {
                    scope.launch {
                        controlloInCorso = true
                        val current = runCatching {
                            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
                        }.getOrNull() ?: ""
                        val latest = UpdateChecker.latestVersion()
                        controlloInCorso = false
                        when {
                            latest == null -> snackbar.showSnackbar(ctx.getString(R.string.upd_check_failed))
                            UpdateChecker.isNewer(latest, current) -> {
                                AppSettings.lastUpdateCheckMs = System.currentTimeMillis()
                                AppSettings.lastUpdateCheckFound = latest
                                AppSettings.save()
                                aggiornamentoTrovato = latest
                            }
                            else -> {
                                AppSettings.lastUpdateCheckMs = System.currentTimeMillis()
                                AppSettings.lastUpdateCheckFound = ""
                                AppSettings.save()
                                snackbar.showSnackbar(ctx.getString(R.string.upd_uptodate))
                            }
                        }
                    }
                },
            ) { Text(stringResource(R.string.upd_check_now)) }

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            // 1.2.6 (sicurezza): di default NeReO non invia la password su una
            // connessione non cifrata (RFC 4643). Override consapevole per server
            // legacy solo-testo. Spento di default.
            Text(stringResource(R.string.sec_section), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.consentiAuthNonCifrata,
                    onCheckedChange = { AppSettings.consentiAuthNonCifrata = it; AppSettings.save() },
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.sec_plain_auth))
            }
            Text(
                stringResource(R.string.sec_plain_auth_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            // 1.2.14 (audit di sicurezza §1.1): la verifica del nome host sui
            // certificati è ora ATTIVA, e questo è l'unico modo per rinunciarvi.
            // Stesso schema dell'interruttore qui sopra — spento di default,
            // nota in rosso — perché è la stessa categoria di scelta: un
            // consenso consapevole a una connessione meno sicura, offerto solo
            // perché senza via d'uscita un provider dal certificato mal
            // configurato lascerebbe l'utente fuori dal proprio server.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.consentiCertificatoNonVerificabile,
                    onCheckedChange = { AppSettings.consentiCertificatoNonVerificabile = it; AppSettings.save() },
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.sec_cert_unverified))
            }
            Text(
                stringResource(R.string.sec_cert_unverified_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            // 1.2.17 (voce B): invia la password solo quando il server la
            // chiede. NON in rosso, a differenza dei due interruttori qui
            // sopra: quelli TOLGONO una protezione, questo ne aggiunge una. Il
            // colore, in questa schermata, distingue le due cose.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.autenticazionePigra,
                    onCheckedChange = { AppSettings.autenticazionePigra = it; AppSettings.save() },
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.sec_lazy_auth))
            }
            Text(
                stringResource(R.string.sec_lazy_auth_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            // 1.2.7 (Dave #8): default della risposta a un crosspost. Spento =
            // solo il gruppo corrente (comportamento storico). Acceso = tutti i
            // gruppi dell'originale già pre-selezionati (chi, come Dave, risponde
            // sempre a tutti). Il dialog di scelta resta comunque.
            Text(stringResource(R.string.xp_default_all_section), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.rispostaCrosspostTuttiIGruppi,
                    onCheckedChange = { AppSettings.rispostaCrosspostTuttiIGruppi = it; AppSettings.save() },
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.xp_default_all_switch))
            }
            Text(
                stringResource(R.string.xp_default_all_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            // 1.4.8 (par. B6b): Format=Flowed in uscita. Sta QUI, accanto alla
            // risposta ai crosspost, perche' e' la stessa categoria — cio' che
            // NeReO spedisce — e non in Aspetto, che riguarda come si vede.
            //
            // ⚠️ LA NOTA VA LETTA DOVE STA L'INTERRUTTORE, non nel README. E'
            // la lezione di par. A16: Jeff ha concluso che l'app gli
            // cancellasse i messaggi letti perche' un'impostazione esistente
            // non era spiegata dove lui l'avrebbe cercata. Un documento che
            // sta dentro lo zip non e' dove si cerca un interruttore.
            Text(stringResource(R.string.flw_section), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.formatFlowedInUscita,
                    onCheckedChange = { AppSettings.formatFlowedInUscita = it; AppSettings.save() },
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.flw_switch))
            }
            Text(
                stringResource(R.string.flw_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                stringResource(R.string.flw_note2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            /*
             * 1.4.9 (§J3-C1) — LA CARTELLA DI «CONSERVA QUESTO».
             *
             * Qui NON si conserva niente: si dice dove, e si vede dove. Il
             * gesto sta sul messaggio (azione «Conserva» nella barra di lettura
             * o nel menu ⋮), ed e' giusto che stia li': conservare e' una cosa
             * che si fa guardando un messaggio, non entrando nelle
             * impostazioni.
             *
             * ⚠️ LA RIGA CHE CONTA E' LA SECONDA NOTA: la ricerca di NeReO non
             * vede i messaggi conservati. E' il limite della voce, e va letto
             * ACCANTO al comando — lezione di §A16, dove un'impostazione
             * spiegata solo nel README ha prodotto una segnalazione di dati
             * cancellati che non erano cancellati.
             *
             * ⚠️ Questa riga dice che cosa e' SCRITTO nell'impostazione, non che
             * il permesso ci sia ancora: quello si verifica scrivendo, e se e'
             * perso lo dice il primo «Conserva» — ad alta voce, svuotando
             * l'impostazione e richiedendo la cartella. Interrogare qui
             * `persistedUriPermissions` sarebbe una chiamata binder a ogni
             * ricomposizione per anticipare una risposta che la scrittura da'
             * comunque. Vedi ui/Conserva.kt, rischio (1).
             */
            val conserva = rememberConserva()
            Text(stringResource(R.string.cns_section), style = MaterialTheme.typography.titleMedium)
            val cartellaAttuale = cartellaConservaSalvata()?.let { descriviCartellaConserva(it) }
            Text(
                cartellaAttuale?.let { stringResource(R.string.cns_folder_current, it) }
                    ?: stringResource(R.string.cns_folder_none),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            OutlinedButton(onClick = { conserva.scegliCartella() }) {
                Text(
                    if (cartellaAttuale == null) stringResource(R.string.cns_folder_choose)
                    else stringResource(R.string.cns_folder_change)
                )
            }
            Text(
                stringResource(R.string.cns_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                stringResource(R.string.cns_note_search),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            // Diagnostica NNTP ("scatola nera"): registra il dialogo client/server
            // per capire su quale comando un server risponde male (es. Astraweb).
            // Spento di default; credenziali oscurate alla fonte; solo in RAM.
            Text(stringResource(R.string.mnt_diag_section), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.nntpLogEnabled,
                    onCheckedChange = {
                        AppSettings.nntpLogEnabled = it
                        NntpLog.enabled = it
                        if (it) NntpLog.clear() // si parte puliti a ogni attivazione
                        AppSettings.save()
                    },
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.mnt_diag_switch))
            }
            Text(
                stringResource(R.string.mnt_diag_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = {
                    val log = NntpLog.snapshot()
                    if (log.isBlank()) {
                        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.mnt_diag_empty)) }
                    } else {
                        appunti.copia(log)
                        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.mnt_diag_copied)) }
                    }
                }) { Text(stringResource(R.string.mnt_diag_copy)) }
                OutlinedButton(onClick = {
                    if (NntpLog.snapshot().isBlank()) {
                        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.mnt_diag_empty)) }
                    } else {
                        if (!exportLogLauncher.apriSeEsiste("${Branding.APP_NAME}-NNTP-log.txt")) {
                            scope.launch { snackbar.showSnackbar(ctx.getString(R.string.common_no_picker)) }
                        }
                    }
                }) { Text(stringResource(R.string.mnt_diag_export)) }
            }

            // Fase 2 (HAL/Bingo3331): diagnostica LISTGROUP manuale, MAI usata
            // nella sync normale. Chiede al server l'elenco vero degli articoli
            // presenti e lo confronta con l'ultimo riepilogo overview: serve a
            // capire se un buco in XOVER è nel database overview del server o
            // se gli articoli non ci sono davvero (con STAT diagnostico sui
            // numeri sospetti). Richiede l'interruttore sopra acceso, altrimenti
            // le richieste partirebbero ma non finirebbe nulla nel log.
            Text(
                stringResource(R.string.mnt_diag_listgroup_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
            OutlinedButton(
                enabled = !listgroupInCorso,
                onClick = {
                    if (!AppSettings.nntpLogEnabled) {
                        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.mnt_diag_listgroup_need_switch)) }
                    } else {
                        scope.launch {
                            gruppiPerDiagnostica = repo.subscribedGroupsWithServerNames()
                            if (gruppiPerDiagnostica.isEmpty()) {
                                snackbar.showSnackbar(ctx.getString(R.string.mnt_diag_listgroup_no_groups))
                            } else {
                                showListgroupDialog = true
                            }
                        }
                    }
                },
            ) { Text(stringResource(R.string.mnt_diag_listgroup_btn)) }

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            // 1.4 (versionCode 99) — DIAGNOSTICA DATABASE.
            // Nasce come strumento di misura per il collaudo delle migrazioni
            // Room (una migrazione che si APRE non e' una migrazione che ha
            // FUNZIONATO), ma resta perche' serve anche dopo: alla prossima
            // segnalazione "ho perso i messaggi" questi numeri sono la prima
            // cosa da chiedere. Gira SOLO quando si preme il pulsante.
            Text(stringResource(R.string.mnt_db_section), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.mnt_db_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            FilledTonalButton(
                enabled = !dbInCorso,
                onClick = {
                    dbInCorso = true
                    scope.launch {
                        rapportoDb = runCatching { costruisciRapportoDb() }
                            .getOrElse { ctx.getString(R.string.mnt_db_err, it.message ?: "") }
                        dbInCorso = false
                    }
                },
            ) { Text(stringResource(R.string.mnt_db_btn)) }

            // 1.4.2 (build 114), voce A3 — LA PULIZIA DEGLI ORFANI.
            // Sta qui e non altrove perche' qui l'utente li ha appena VISTI: il
            // rapporto sopra e' cio' che ha trasformato "forse c'e' della roba
            // vecchia" nei tre gruppi e nelle 99.906 righe di catalogo del
            // telefono di BIG Umberto. Si conta, si chiede, si cancella — mai
            // in automatico, mai senza dire quanto.
            Text(
                stringResource(R.string.mnt_orphans_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
            OutlinedButton(
                enabled = !orfaniInCorso && !dbInCorso,
                onClick = {
                    orfaniInCorso = true
                    scope.launch {
                        val conto = runCatching { repo.conteggiOrfani() }.getOrDefault(emptyList())
                        orfaniInCorso = false
                        if (conto.isEmpty()) {
                            snackbar.showSnackbar(ctx.getString(R.string.mnt_orphans_none))
                        } else {
                            orfaniDaConfermare = conto
                        }
                    }
                },
            ) { Text(stringResource(R.string.mnt_orphans_btn)) }

            HorizontalDivider(Modifier.padding(vertical = 24.dp))

            // 1.2.9.b (Piano C.1 — NeReO_bug_1.2.9.md, voce X: segnalazione di
            // un utente, "si chiude spesso improvvisamente", nessuno stack
            // trace disponibile). Sempre attivo, a differenza della
            // diagnostica NNTP sopra: un crash non avvisa prima di arrivare,
            // quindi non ha senso un interruttore da accendere in anticipo.
            // Stesso schema copia/esporta, più un pulsante per svuotarlo dopo
            // averlo mandato a chi sviluppa.
            Text(stringResource(R.string.mnt_crash_section), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.mnt_crash_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = {
                    val log = CrashLog.snapshot(ctx)
                    if (log.isBlank()) {
                        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.mnt_crash_empty)) }
                    } else {
                        appunti.copia(log)
                        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.mnt_crash_copied)) }
                    }
                }) { Text(stringResource(R.string.mnt_crash_copy)) }
                OutlinedButton(onClick = {
                    if (CrashLog.snapshot(ctx).isBlank()) {
                        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.mnt_crash_empty)) }
                    } else {
                        if (!exportCrashLogLauncher.apriSeEsiste("${Branding.APP_NAME}-crash-log.txt")) {
                            scope.launch { snackbar.showSnackbar(ctx.getString(R.string.common_no_picker)) }
                        }
                    }
                }) { Text(stringResource(R.string.mnt_crash_export)) }
                TextButton(onClick = {
                    if (CrashLog.snapshot(ctx).isBlank()) {
                        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.mnt_crash_empty)) }
                    } else {
                        CrashLog.clear(ctx)
                        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.mnt_crash_cleared)) }
                    }
                }) { Text(stringResource(R.string.mnt_crash_clear)) }
            }

            Spacer(Modifier.height(24.dp))
        }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.mnt_backup_pass_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.mnt_backup_pass_note),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = backupPw,
                        onValueChange = { backupPw = it },
                        label = { Text(stringResource(R.string.mnt_pass_empty)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    // Nome proposto: NeReO-Backup-AA-MM-DD.json. Il marchio viene
                    // da Branding (fonte unica del nome, cambia con l'app) e la
                    // data rende i backup distinguibili e ordinabili tra loro.
                    val data = java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yy-MM-dd"))
                    val nome = "${Branding.APP_NAME}-Backup-$data.json"
                    // 1.5.6 / 174 (Roxy): senza selettore, nella cartella dell'app.
                    if (!exportLauncher.apriSeEsiste(nome)) {
                        val pw = backupPw
                        scope.launch {
                            runCatching { BackupLocale.salva(ctx, nome, contenutoBackup(pw)) }
                                .onSuccess { backupLocaleSalvato = it.absolutePath }
                                .onFailure { snackbar.showSnackbar(ctx.getString(R.string.mnt_export_err, it.message ?: "")) }
                        }
                    }
                }) { Text(stringResource(R.string.mnt_export)) }
            },
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    // 1.5.6 / 174: DOVE e' finito il backup, per esteso. Un avviso a comparsa
    // e' troppo breve per un percorso da ricopiare in un gestore file.
    backupLocaleSalvato?.let { percorso ->
        AlertDialog(
            onDismissRequest = { backupLocaleSalvato = null },
            title = { Text(stringResource(R.string.mnt_local_saved_title)) },
            text = { Text(stringResource(R.string.mnt_local_saved_text, percorso)) },
            confirmButton = { TextButton(onClick = { backupLocaleSalvato = null }) { Text(stringResource(R.string.common_ok)) } },
        )
    }

    // 1.5.6 / 174: il ripristino senza selettore — i backup della cartella
    // dell'app, il piu' recente per primo. Una riga per file: col telecomando
    // del box si scorre e si sceglie come con le dita.
    backupLocali?.let { files ->
        AlertDialog(
            onDismissRequest = { backupLocali = null },
            title = { Text(stringResource(R.string.mnt_local_import_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (files.isEmpty()) {
                        Text(
                            stringResource(
                                R.string.mnt_local_import_empty,
                                BackupLocale.cartella(ctx)?.absolutePath ?: "Android/data/${ctx.packageName}/files",
                            ),
                        )
                    } else {
                        Text(
                            stringResource(R.string.mnt_local_import_note),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        files.forEach { f ->
                            TextButton(
                                onClick = {
                                    backupLocali = null
                                    instradaBackupLetto(runCatching { f.readText(Charsets.UTF_8) }, f.absolutePath)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(f.name, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { backupLocali = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    val enc = pendingEncrypted
    if (enc != null) {
        var pw by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingEncrypted = null },
            title = { Text(stringResource(R.string.mnt_backup_protected)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.mnt_backup_protected_note),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = pw,
                        onValueChange = { pw = it },
                        label = { Text(stringResource(R.string.mnt_backup_pass_title)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pw.isNotBlank(),
                    onClick = {
                        scope.launch {
                            // 1.3.7: si DECIFRA soltanto. Prima qui dentro si
                            // decifrava E si importava nello stesso
                            // runCatching, quindi un errore di IMPORT veniva
                            // annunciato all'utente come "password errata" —
                            // un messaggio che mandava a cercare la causa
                            // sbagliata. Ora i due passi sono separati.
                            runCatching { BackupCrypto.decrypt(enc, pw) }
                                .onSuccess { json ->
                                    pendingEncrypted = null
                                    pendingImport = json
                                }
                                .onFailure {
                                    snackbar.showSnackbar(ctx.getString(R.string.mnt_wrong_pass))
                                }
                        }
                    },
                ) { Text(stringResource(R.string.mnt_restore)) }
            },
            dismissButton = { TextButton(onClick = { pendingEncrypted = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    /*
     * 1.3.7 (Dave Royal) — COME ripristinare, chiesto invece che presunto.
     *
     * "Unisci" e' la prima voce e la scelta prudente: non cancella niente ed e'
     * idempotente. "Sostituisci" e' quella distruttiva, e il testo dichiara il
     * suo perimetro PRIMA — filtri e identita' si', server, sottoscrizioni e
     * articoli no — invece di lasciarlo scoprire dopo.
     */
    /*
     * 1.3.7: il dettaglio tecnico in un DIALOGO e non in uno snackbar, con un
     * pulsante per copiarlo. Uno snackbar lungo viene troncato a due righe, e
     * un dettaglio troncato e' esattamente inutile quanto nessun dettaglio: chi
     * deve segnalarlo ha bisogno di leggerlo per intero e di poterlo incollare.
     */
    val errLettura = erroreLettura
    if (errLettura != null) {
        AlertDialog(
            onDismissRequest = { erroreLettura = null },
            title = { Text(stringResource(R.string.mnt_file_unreadable)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.mnt_file_unreadable_note),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        errLettura,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { erroreLettura = null }) { Text(stringResource(R.string.common_close)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    appunti.copia(errLettura)
                    erroreLettura = null
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.art_copy_done)) }
                }) { Text(stringResource(R.string.common_copy)) }
            },
        )
    }

    /*
     * 1.4 — il rapporto della Diagnostica database.
     *
     * Stessa forma del dialogo "File illeggibile" qui sopra, e per la stessa
     * ragione: un testo che serve a una segnalazione va potuto leggere per
     * intero E copiare. Scorrevole, perche' undici righe piu' l'intestazione su
     * un telefono stretto non ci stanno sempre.
     */
    val rapporto = rapportoDb
    if (rapporto != null) {
        AlertDialog(
            onDismissRequest = { rapportoDb = null },
            title = { Text(stringResource(R.string.mnt_db_section)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        rapporto,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { rapportoDb = null }) { Text(stringResource(R.string.common_close)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    appunti.copia(rapporto)
                    rapportoDb = null
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.art_copy_done)) }
                }) { Text(stringResource(R.string.common_copy)) }
            },
        )
    }

    val daImportare = pendingImport
    if (daImportare != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.mnt_restore_how)) },
            text = { Text(stringResource(R.string.mnt_restore_how_note)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch { runImport(daImportare, BackupManager.ModoRipristino.UNISCI) }
                }) { Text(stringResource(R.string.mnt_restore_merge)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch { runImport(daImportare, BackupManager.ModoRipristino.SOSTITUISCI) }
                }) { Text(stringResource(R.string.mnt_restore_replace)) }
            },
        )
    }

    // 1.2.5 (Bingo3331): esito del controllo manuale, con link alla pagina release.
    val aggV = aggiornamentoTrovato
    if (aggV != null) {
        AlertDialog(
            onDismissRequest = { aggiornamentoTrovato = null },
            title = { Text(stringResource(R.string.upd_available_title)) },
            text = { Text(stringResource(R.string.upd_available_msg, aggV)) },
            confirmButton = {
                TextButton(onClick = {
                    aggiornamentoTrovato = null
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
            dismissButton = { TextButton(onClick = { aggiornamentoTrovato = null }) { Text(stringResource(R.string.common_close)) } },
        )
    }

    // Fase 2 (HAL/Bingo3331): scelta del gruppo su cui lanciare la diagnostica
    // LISTGROUP. Un solo elenco cliccabile (stesso pattern di altre scelte
    // dell'app), "server — gruppo" per distinguere gruppi omonimi su server
    // diversi.
    if (showListgroupDialog) {
        AlertDialog(
            onDismissRequest = { if (!listgroupInCorso) showListgroupDialog = false },
            title = { Text(stringResource(R.string.mnt_diag_listgroup_btn)) },
            text = {
                if (listgroupInCorso) {
                    Text(stringResource(R.string.mnt_diag_listgroup_running))
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        gruppiPerDiagnostica.forEach { (gruppo, nomeServer) ->
                            Text(
                                "$nomeServer — ${gruppo.name}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // 1.4.2 (build 114): una maniglia nuova a ogni
                                        // avvio, e la si mette da parte PRIMA di partire —
                                        // se l'utente annulla mentre la connessione si sta
                                        // ancora aprendo, la maniglia c'e' gia'.
                                        val maniglia = org.openusenet.reader.data.AnnullamentoDiagnostica()
                                        annullaListgroup = maniglia
                                        listgroupInCorso = true
                                        scope.launch {
                                            val esito = runCatching {
                                                repo.diagnosticaListgroup(gruppo.id, annullamento = maniglia)
                                            }
                                            // Prima si chiude, poi si avvisa. Al rovescio —
                                            // com'era fino alla 113 — `showSnackbar` sospende
                                            // finche' l'avviso non sparisce, e per tutto quel
                                            // tempo il dialogo resta li' a dire "in corso" di
                                            // un lavoro gia' finito.
                                            listgroupInCorso = false
                                            showListgroupDialog = false
                                            annullaListgroup = null
                                            snackbar.showSnackbar(
                                                esito.fold(
                                                    onSuccess = {
                                                        ctx.getString(
                                                            if (maniglia.annullato) {
                                                                R.string.mnt_diag_listgroup_cancelled
                                                            } else {
                                                                R.string.mnt_diag_listgroup_done
                                                            },
                                                        )
                                                    },
                                                    onFailure = {
                                                        ctx.getString(R.string.mnt_export_err, it.message ?: "")
                                                    },
                                                ),
                                            )
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                // 1.4.2 (build 114) — BIG Umberto, 23 settembre: «il tasto
                // annulla è in grigio e non funziona, devi chiudere NeReO
                // brutalmente per interrompere». Aveva ragione: era
                // disabilitato per costruzione, davanti a un'attesa che il suo
                // log misura in 97 secondi. Ora e' sempre attivo, e durante
                // l'operazione ne interrompe una davvero — chiudendo il socket,
                // perche' una lettura bloccante non si ferma in altro modo.
                TextButton(
                    onClick = {
                        if (listgroupInCorso) {
                            annullaListgroup?.let { maniglia -> scope.launch { maniglia.annulla() } }
                        } else {
                            showListgroupDialog = false
                        }
                    },
                ) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    /*
     * 1.4.2 (build 114), voce A3 — la conferma prima di cancellare gli orfani.
     *
     * Stesso stampo del dialogo di cancellazione server della 113, e per la
     * stessa ragione: un'operazione distruttiva dice PRIMA quante righe si
     * porta via, voce per voce. Qui il preventivo e' gia' calcolato — il
     * pulsante non apre questo dialogo finche' non ha un numero da mostrare.
     */
    orfaniDaConfermare?.let { conto ->
        AlertDialog(
            onDismissRequest = { if (!orfaniInCorso) orfaniDaConfermare = null },
            title = { Text(stringResource(R.string.mnt_orphans_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.mnt_orphans_confirm,
                        conto.joinToString("\n") { (nome, n) ->
                            "• %s: %,d".format(java.util.Locale.US, nome, n)
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !orfaniInCorso,
                    onClick = {
                        orfaniInCorso = true
                        scope.launch {
                            val esito = runCatching { repo.eliminaOrfani() }
                            // Il rapporto a schermo conteneva il blocco ORFANI:
                            // lasciarlo com'era vorrebbe dire mostrare righe che
                            // non esistono piu'. Si ricalcola solo se c'era.
                            if (esito.isSuccess && rapportoDb != null) {
                                rapportoDb = runCatching { costruisciRapportoDb() }.getOrNull() ?: rapportoDb
                            }
                            orfaniInCorso = false
                            orfaniDaConfermare = null
                            snackbar.showSnackbar(
                                esito.fold(
                                    onSuccess = { tolte ->
                                        ctx.getString(
                                            R.string.mnt_orphans_done,
                                            "%,d".format(java.util.Locale.US, tolte.sumOf { it.second }),
                                        )
                                    },
                                    onFailure = { ctx.getString(R.string.mnt_db_err, it.message ?: "") },
                                ),
                            )
                        }
                    },
                ) { Text(stringResource(R.string.mnt_orphans_confirm_btn)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !orfaniInCorso,
                    onClick = { orfaniDaConfermare = null },
                ) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/**
 * 1.2.8 (Bingo3331 + HAL): testo "Ultimo controllo: …" sotto il selettore di
 * modalità — così il meccanismo si vede funzionare anche quando non c'è
 * nulla di nuovo (niente popup in quel caso, solo questo testo).
 */
@Composable
private fun testoUltimoControllo(): String {
    val ms = AppSettings.lastUpdateCheckMs
    if (ms <= 0L) return stringResource(R.string.upd_last_check_never)
    val istante = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
    val oraFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    val quando = if (istante.toLocalDate() == java.time.LocalDate.now()) {
        stringResource(R.string.upd_last_check_today_at, istante.format(oraFmt))
    } else {
        stringResource(
            R.string.upd_last_check_date_at,
            istante.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")),
            istante.format(oraFmt),
        )
    }
    val trovato = AppSettings.lastUpdateCheckFound
    val esito = if (trovato.isBlank()) {
        stringResource(R.string.upd_last_check_none_found)
    } else {
        stringResource(R.string.upd_last_check_version_found, trovato)
    }
    return stringResource(R.string.upd_last_check_prefix, quando, esito)
}
