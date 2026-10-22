/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Gestione filtri: regole con campo, tipo match (contains/wildcard/regex),
 * punteggio, scope wildmat, scadenza. Import/export kill-file testuale.
 */
package org.openusenet.reader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.ArrowBack
import org.openusenet.reader.ui.icone.Add
import org.openusenet.reader.ui.icone.CleaningServices
import org.openusenet.reader.ui.icone.Delete
import org.openusenet.reader.ui.icone.FileDownload
import org.openusenet.reader.ui.icone.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.openusenet.reader.R
import org.openusenet.reader.data.FilterRuleEntity
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.data.toRule
import org.openusenet.reader.filter.FilterField
import org.openusenet.reader.filter.FollowupMode
import org.openusenet.reader.filter.campoIdentificaUnAutore
import org.openusenet.reader.filter.KillFile
import org.openusenet.reader.filter.MatchType
import org.openusenet.reader.filter.punteggioRisposte
import org.openusenet.reader.ui.indietroSicuro

@Composable
private fun fieldLabelIt(f: FilterField) = when (f) {
    FilterField.FROM -> stringResource(R.string.flt_field_from)
    FilterField.SUBJECT -> stringResource(R.string.flt_field_subject)
    FilterField.MESSAGE_ID -> stringResource(R.string.flt_field_id)
    FilterField.XPOST -> stringResource(R.string.flt_field_xpost)
    FilterField.BODY -> stringResource(R.string.flt_field_body)
    FilterField.REFERENCES -> stringResource(R.string.flt_field_references)
    FilterField.NEWSREADER -> stringResource(R.string.flt_field_newsreader)
    FilterField.HEADERS -> stringResource(R.string.flt_field_headers)
}

/**
 * M1: campi proposti come chip nell'editor.
 *
 * REFERENCES resta fuori di proposito: e' il primitivo di basso livello su cui
 * poggia la propagazione automatica, e usarlo a mano vuol dire incollare un
 * message-id preciso. Chi ne ha bisogno lo scrive nel kill-file; metterlo fra i
 * chip inviterebbe solo a creare regole che non fanno mai match.
 */
private val CAMPI_VISIBILI: List<FilterField> =
    FilterField.entries.filter { it != FilterField.REFERENCES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(repo: NewsRepository, nav: NavHostController) {
    val rules by repo.filterRules.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<FilterRuleEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }
    // beta20 (Roxy): applicazione delle regole ai messaggi gia' scaricati
    var showApplicaOra by remember { mutableStateOf(false) }
    var applicando by remember { mutableStateOf(false) }
    var esitoApplicazione by remember { mutableStateOf<NewsRepository.EsitoFiltriRetroattivi?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.flt_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.indietroSicuro() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    // beta20 (Roxy): applica le regole ai messaggi GIA' scaricati.
                    // Prima i filtri agivano solo sui nuovi in arrivo, quindi una
                    // regola "Elimina del tutto" lasciava i vecchi al loro posto.
                    IconButton(enabled = !applicando, onClick = { showApplicaOra = true }) {
                        Icon(Icons.Default.CleaningServices, contentDescription = stringResource(R.string.flt_apply_now_cd))
                    }
                    IconButton(onClick = { showImportExport = true }) {
                        Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.flt_importexport_cd))
                    }
                },
            )
        },
        floatingActionButton = {
            // beta16 (Roxy): FAB dentro l'area sicura in landscape (vedi
            // ServersScreen per la causa edge-to-edge).
            FloatingActionButton(
                onClick = { editing = null; showEditor = true },
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.flt_new_rule_cd))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(rules, key = { it.id }) { rule ->
                ListItem(
                    headlineContent = {
                        // 1.5.7: «nick = indirizzo» non ha un pattern da mostrare.
                        val nickIndirizzo = rule.matchType == MatchType.NICK_EMAIL.name
                        val etichettaNick = stringResource(R.string.flt_match_nick_email)
                        Text(rule.name.ifBlank { if (nickIndirizzo) "${rule.field}: $etichettaNick" else "${rule.field}: ${rule.pattern}" })
                    },
                    supportingContent = {
                        val nickIndirizzo = rule.matchType == MatchType.NICK_EMAIL.name
                        val etichettaNick = stringResource(R.string.flt_match_nick_email)
                        val desc = if (rule.field == FilterField.BODY.name)
                            stringResource(R.string.flt_body_rule, rule.matchType.lowercase(), rule.pattern)
                        else
                            "${rule.field.lowercase()} " +
                                (if (nickIndirizzo) etichettaNick else "${rule.matchType.lowercase()} \"${rule.pattern}\"") +
                                "  →  " + (if (rule.score > 0) "+${rule.score}" else "${rule.score}")
                        // M1: una regola estesa alle risposte si deve riconoscere
                        // dall'elenco, senza doverla riaprire.
                        val estesa = rule.followup != FollowupMode.NONE.name
                        Text(
                            desc +
                                (if (rule.groupWildmat != "*") "  [${rule.groupWildmat}]" else "") +
                                (if (estesa) "  ⛓ " + stringResource(R.string.flt_followup_badge) else ""),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { scope.launch { repo.deleteFilter(rule.id) } }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
                        }
                    },
                    modifier = Modifier.clickable { editing = rule; showEditor = true },
                )
            }
            if (rules.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.flt_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }

    if (showEditor) {
        FilterEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { r -> scope.launch { repo.saveFilter(r) }; showEditor = false },
        )
    }

    if (showApplicaOra) {
        AlertDialog(
            onDismissRequest = { showApplicaOra = false },
            title = { Text(stringResource(R.string.flt_apply_title)) },
            text = {
                Text(
                    stringResource(R.string.flt_apply_body),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showApplicaOra = false
                    applicando = true
                    scope.launch {
                        val esito = runCatching { repo.applicaFiltriRetroattivi() }
                            .getOrDefault(NewsRepository.EsitoFiltriRetroattivi(0, 0))
                        applicando = false
                        esitoApplicazione = esito
                    }
                }) { Text(stringResource(R.string.flt_apply_now)) }
            },
            dismissButton = { TextButton(onClick = { showApplicaOra = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    esitoApplicazione?.let { esito ->
        AlertDialog(
            onDismissRequest = { esitoApplicazione = null },
            title = { Text(stringResource(R.string.flt_applied_title)) },
            text = {
                // 1.4.8 (§A12-retro): la terza cifra. Il "nessun messaggio"
                // deve guardare TUTTE E TRE, altrimenti una regola
                // «Evidenzia» che ha fatto il suo lavoro si annuncia dicendo
                // che non ha trovato niente.
                // 1.5.3: e le due cifre dell'altra direzione — tornati visibili
                // e stelle tolte — in una riga sola, che compare se servono.
                val allentati = esito.rimessiInChiaro > 0 || esito.stelleTolte > 0
                Text(
                    if (esito.eliminati == 0 && esito.nascosti == 0 && esito.ripunteggiati == 0 && !allentati)
                        stringResource(R.string.flt_applied_none)
                    else
                        stringResource(
                            R.string.flt_applied_summary,
                            esito.eliminati, esito.nascosti, esito.ripunteggiati,
                        ) + (if (allentati) "\n" + stringResource(
                            R.string.flt_applied_relaxed, esito.rimessiInChiaro, esito.stelleTolte,
                        ) else "")
                )
            },
            confirmButton = { TextButton(onClick = { esitoApplicazione = null }) { Text("OK") } },
        )
    }

    if (showImportExport) {
        var text by remember {
            mutableStateOf(KillFile.export(rules.map { it.toRule() }))
        }
        AlertDialog(
            onDismissRequest = { showImportExport = false },
            title = { Text(stringResource(R.string.flt_killfile_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.flt_killfile_note),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.importKillFile(KillFile.parse(text)) }
                    showImportExport = false
                }) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Text(stringResource(R.string.flt_import))
                }
            },
            dismissButton = { TextButton(onClick = { showImportExport = false }) { Text(stringResource(R.string.common_close)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterEditorDialog(
    initial: FilterRuleEntity?,
    onDismiss: () -> Unit,
    onSave: (FilterRuleEntity) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var field by remember { mutableStateOf(initial?.field ?: FilterField.SUBJECT.name) }
    var matchType by remember { mutableStateOf(initial?.matchType ?: MatchType.CONTAINS.name) }
    var pattern by remember { mutableStateOf(initial?.pattern ?: "") }
    var score by remember { mutableStateOf((initial?.score ?: -100).toString()) }
    // beta18 (Piano B): azione selezionata, derivata dallo score iniziale. Se lo
    // score è un valore "noto" (soglia) mostra l'azione; se è custom (es. +50 o
    // frutto di cumulabilità) parte in modalità AVANZATA col numero visibile.
    var azione by remember {
        mutableStateOf(actionForScore(initial?.score ?: -100))
    }
    var wildmat by remember { mutableStateOf(initial?.groupWildmat ?: "*") }
    var caseSensitive by remember { mutableStateOf(initial?.caseSensitive ?: false) }
    // M1 (release personale Marco): estensione alle risposte.
    var estendiAlleRisposte by remember {
        mutableStateOf((initial?.followup ?: FollowupMode.NONE.name) != FollowupMode.NONE.name)
    }
    // 1.5.7 / 175 (Marco: «segnare chi scrive nei post del plonkato»): su una
    // regola che NASCONDE ed e' estesa alle risposte, le risposte si possono
    // EVIDENZIARE invece di nasconderle. Il motore lo sapeva gia' fare (un
    // followupScore positivo vince sempre, vedi effettoPropagazioneDi): mancava
    // solo la scelta qui. Parte da com'era salvata la regola.
    var risposteEvidenziate by remember { mutableStateOf((initial?.followupScore ?: 0) > 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) stringResource(R.string.flt_rule_new) else stringResource(R.string.flt_rule_edit)) },
        text = {
            // M1.1 (Marco, screenshot del 20/08): il contenuto di questo dialogo
            // NON scorreva. Un AlertDialog impone un'altezza massima, e un Column
            // senza verticalScroll non reagisce scorrendo ma COMPRIMENDO e
            // TAGLIANDO: su schermo stretto i chip vanno a capo (2 righe per i
            // campi, 2 per il tipo di match, 5 per le azioni) e tutto cio' che
            // sta sotto il punteggio finiva fuori dal dialogo, irraggiungibile.
            //
            // Il difetto e' PRECEDENTE a M1: gia' nella 1.1 "Ambito gruppi" e
            // "Case sensitive" erano tagliati sui telefoni piu' piccoli — non se
            // ne era accorto nessuno perche' nessuno li cercava. L'aggiunta della
            // casella "Estendi alle risposte" lo ha solo reso visibile.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.flt_name_opt)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CAMPI_VISIBILI.forEach { f ->
                        FilterChip(
                            selected = field == f.name,
                            onClick = {
                                field = f.name
                                // 1.5.7: «nick = indirizzo» esiste solo sul From.
                                if (f != FilterField.FROM && matchType == MatchType.NICK_EMAIL.name) {
                                    matchType = MatchType.CONTAINS.name
                                }
                            },
                            label = { Text(fieldLabelIt(f)) },
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MatchType.entries
                        .filter { it != MatchType.NICK_EMAIL || field == FilterField.FROM.name }
                        .forEach { m ->
                            FilterChip(
                                selected = matchType == m.name,
                                onClick = { matchType = m.name },
                                label = {
                                    Text(
                                        if (m == MatchType.NICK_EMAIL) stringResource(R.string.flt_match_nick_email)
                                        else m.name.lowercase()
                                    )
                                },
                            )
                        }
                }
                // 1.5.7: «nick = indirizzo» non ha uno schema da scrivere.
                if (matchType == MatchType.NICK_EMAIL.name) {
                    Text(
                        stringResource(R.string.flt_nick_email_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text(stringResource(R.string.flt_pattern)) }, singleLine = true, keyboardOptions = KeyboardOptions(autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth())
                }
                if (field == FilterField.HEADERS.name) {
                    Text(
                        stringResource(R.string.flt_headers_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 1.4.4: la nota sta QUI e non nel ramo del BODY qui sotto, che
                // nasconde il selettore dell'azione. Una regola sul lettore di
                // news l'azione la vuole eccome — e' il suo scopo.
                if (field == FilterField.NEWSREADER.name) {
                    Text(
                        stringResource(R.string.flt_newsreader_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (field == FilterField.BODY.name) {
                    Text(
                        stringResource(R.string.flt_body_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // beta18 (Piano B): azioni etichettate al posto del numero
                    // grezzo. L'utente sceglie "Ignora/Evidenzia/Segui..."; il
                    // numero appare solo in modalità Avanzata (per la cumulabilità).
                    Text(stringResource(R.string.flt_action), style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        FilterAction.entries.forEach { a ->
                            FilterChip(
                                selected = azione == a,
                                onClick = {
                                    azione = a
                                    // quando scelgo un'azione "umana", allineo lo
                                    // score al suo valore; in Avanzata lascio il
                                    // numero all'utente.
                                    if (a.score != null) score = a.score.toString()
                                },
                                // RC01: etichetta azione tradotta alla visualizzazione
                                // (l'enum non è @Composable e il suo label fa da default)
                                label = {
                                    Text(
                                        when (a) {
                                            FilterAction.IGNORA -> stringResource(R.string.flt_act_ignore)
                                            FilterAction.ELIMINA -> stringResource(R.string.flt_act_delete)
                                            FilterAction.EVIDENZIA -> stringResource(R.string.flt_act_highlight)
                                            FilterAction.SEGUI -> stringResource(R.string.flt_act_follow)
                                            FilterAction.AVANZATA -> stringResource(R.string.flt_act_advanced)
                                        }
                                    )
                                },
                            )
                        }
                    }
                    if (azione == FilterAction.AVANZATA) {
                        OutlinedTextField(
                            value = score,
                            onValueChange = { score = it.filter { c -> c.isDigit() || c == '-' } },
                            label = { Text(stringResource(R.string.flt_score_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                // M1: estensione alle risposte. Ha senso solo su regole che
                // identificano un AUTORE — su un oggetto o un crosspost la
                // discendenza non vuol dire nulla, e la casella confonderebbe.
                //
                // 1.4.4: il criterio NON è piu' scritto qui. Stava in due punti
                // di questo file — qui e nel salvataggio — e tutti e due
                // dicevano solo FROM: quando è arrivato il campo "lettore di
                // news" sono rimasti indietro entrambi. Ora la risposta la dà
                // l'enum, in un posto solo. Vedi FilterField.identificaUnAutore.
                if (campoIdentificaUnAutore(field)) {
                    Row {
                        Checkbox(
                            checked = estendiAlleRisposte,
                            onCheckedChange = { estendiAlleRisposte = it },
                        )
                        Text(
                            stringResource(R.string.flt_followup),
                            Modifier.padding(top = 12.dp),
                        )
                    }
                    if (estendiAlleRisposte) {
                        // 1.5.1 (§A12-retro meta' B): la nota segue l'azione,
                        // come ora la segue la propagazione. Era il rilievo di
                        // Cercaparole nella 1.4.7: «decisamente orientato solo
                        // al plonkaggio». Stesso criterio di
                        // effettoPropagazioneDi: positivo = non nasconde.
                        val positiva = (score.toIntOrNull() ?: 0) > 0
                        Text(
                            stringResource(
                                when {
                                    positiva -> R.string.flt_followup_note_positiva
                                    risposteEvidenziate -> R.string.flt_followup_note_evidenziate
                                    else -> R.string.flt_followup_note
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // 1.5.7 / 175: regola che nasconde → le risposte si
                        // possono nascondere (come sempre) o EVIDENZIARE.
                        if (!positiva) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = !risposteEvidenziate, onClick = { risposteEvidenziate = false })
                                Text(stringResource(R.string.flt_followup_hide))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = risposteEvidenziate, onClick = { risposteEvidenziate = true })
                                Text(stringResource(R.string.flt_followup_highlight))
                            }
                        }
                    }
                }
                OutlinedTextField(value = wildmat, onValueChange = { wildmat = it }, label = { Text(stringResource(R.string.flt_scope)) }, singleLine = true, keyboardOptions = KeyboardOptions(autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth())
                Row {
                    Checkbox(checked = caseSensitive, onCheckedChange = { caseSensitive = it })
                    Text(stringResource(R.string.flt_case), Modifier.padding(top = 12.dp))
                }

            }
        },
        confirmButton = {
            TextButton(
                enabled = (pattern.isNotBlank() || matchType == MatchType.NICK_EMAIL.name) &&
                    (field == FilterField.BODY.name || score.toIntOrNull() != null),
                onClick = {
                    onSave(
                        FilterRuleEntity(
                            id = initial?.id ?: 0,
                            name = name,
                            field = field,
                            matchType = matchType,
                            // 1.5.7: «*» per nick = indirizzo — il pattern non
                            // conta, ma il kill-file scarta le regole senza.
                            pattern = if (matchType == MatchType.NICK_EMAIL.name) "*" else pattern,
                            score = if (field == FilterField.BODY.name) -10000 else (score.toIntOrNull() ?: 0),
                            caseSensitive = caseSensitive,
                            groupWildmat = wildmat.ifBlank { "*" },
                            // 1.4.4 — IL SALVATAGGIO NON AZZERA PIU' IN SILENZIO.
                            //
                            // Prima questa riga forzava NONE per ogni campo che
                            // non fosse FROM. Sembra innocuo e non lo e': il
                            // kill-file accetta `Followup:` su qualunque campo,
                            // quindi una regola importata con la propagazione la
                            // perdeva alla PRIMA apertura dell'editor, senza un
                            // avviso e senza che l'utente avesse mai visto la
                            // casella. E' successo a Marco.
                            //
                            // Ora: sui campi che identificano un autore decide la
                            // casella (che li' si vede); su tutti gli altri si
                            // CONSERVA quello che c'era. Cancellare un dato che
                            // non si mostra non e' mai la scelta giusta.
                            followup = if (campoIdentificaUnAutore(field)) {
                                if (estendiAlleRisposte) FollowupMode.SUBTREE.name else FollowupMode.NONE.name
                            } else {
                                initial?.followup ?: FollowupMode.NONE.name
                            },
                            // 1.4.4 — GLI ALTRI TRE CAMPI CHE L'EDITOR PERDEVA.
                            // Trovati mentre si correggeva il `followup`, sono la
                            // stessa malattia: questo dialogo RICOSTRUISCE
                            // l'entita' da zero, quindi ogni campo che non mostra
                            // torna al proprio valore predefinito. Una regola con
                            // una scadenza la perdeva; una regola disattivata si
                            // riaccendeva da sola; il punteggio di propagazione
                            // spariva. Nessuno dei tre e' mai stato segnalato,
                            // il che dice soltanto che nessuno li aveva provati.
                            expiresEpochDay = initial?.expiresEpochDay,
                            enabled = initial?.enabled ?: true,
                            // 1.5.7 / 175: la scelta «evidenzia le risposte» si
                            // scrive qui; un punteggio di propagazione NEGATIVO
                            // scritto a mano nel kill-file si conserva, come prima.
                            followupScore = punteggioRisposte(
                                attiva = campoIdentificaUnAutore(field) && estendiAlleRisposte &&
                                    (score.toIntOrNull() ?: 0) <= 0,
                                evidenzia = risposteEvidenziate,
                                salvato = initial?.followupScore,
                            ),
                        )
                    )
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
