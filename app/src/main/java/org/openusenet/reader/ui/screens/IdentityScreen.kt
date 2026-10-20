/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.2.9 (multi-identità, richiesta di Marco: "un multi utente reale, come in
 * MesNews e/o NR MOD17 di Piao Hong"): due schermate al posto della vecchia
 * IdentityScreen unica.
 *
 * IdentityScreen (questo file, elenco): mostra tutte le identità salvate,
 * quale sia la predefinita, e porta all'editor di una di esse (o a crearne
 * una nuova). L'assegnazione di un'identità a un server specifico NON avviene
 * più qui: si fa da ServersScreen, editando il server — è lì che ha senso
 * decidere "MCSM scrive su astraweb", non qui dove si edita l'identità in sé.
 *
 * IdentityEditScreen (sotto, stesso file): il form completo di UNA identità —
 * nome, email, firma, User-Agent, intestazioni personalizzate, introduzione
 * alla citazione, FACE. È lo stesso contenuto che prima viveva nell'unica
 * IdentityScreen (sezione generale), ora applicato a una riga qualunque della
 * tabella `identities` invece che alle preferenze globali.
 */
package org.openusenet.reader.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.apriSeEsiste
import org.openusenet.reader.ui.icone.ArrowBack
import org.openusenet.reader.ui.icone.Add
import org.openusenet.reader.ui.icone.Delete
import org.openusenet.reader.ui.icone.Save
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.openusenet.reader.R
import org.openusenet.reader.data.Identity
import org.openusenet.reader.data.IntestazioniPersonalizzate
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.ui.Routes
import org.openusenet.reader.ui.indietroSicuro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityScreen(repo: NewsRepository, nav: NavHostController) {
    val identities by repo.identities.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.idl_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.indietroSicuro() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate(Routes.identityEdit(0)) },
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.idl_add_cd))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(identities, key = { it.id }) { identity ->
                ListItem(
                    headlineContent = {
                        Text(identity.nickname.ifBlank { identity.name.ifBlank { identity.email } })
                    },
                    supportingContent = {
                        val riga = listOfNotNull(
                            identity.name.takeIf { it.isNotBlank() },
                            identity.email.takeIf { it.isNotBlank() }?.let { "<$it>" },
                        ).joinToString(" ")
                        Text(
                            if (identity.isDefault) {
                                stringResource(R.string.idl_default_suffix, riga)
                            } else riga,
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            scope.launch {
                                repo.deleteIdentity(identity.id)
                                org.openusenet.reader.AppSettings.reloadIdentityEmails(repo)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { nav.navigate(Routes.identityEdit(identity.id)) },
                )
            }
            if (identities.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.idl_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityEditScreen(repo: NewsRepository, nav: NavHostController, identityId: Long) {
    val ctx = LocalContext.current
    val introPredefinita = stringResource(R.string.cmp_default_intro)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var caricata by remember { mutableStateOf(identityId <= 0) }
    var nickname by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    var sigEnabled by remember { mutableStateOf(true) }
    var sigRandom by remember { mutableStateOf(false) }
    // 1.2.13 (Merovingio): identità NUOVA → campo già compilato col segnaposto
    // (vedi Branding.USER_AGENT_PREDEFINITO). Chi vuole restare anonimo lo
    // svuota: l'anonimato resta possibile, ma diventa una scelta invece che il
    // risultato di un campo mai compilato. Un'identità ESISTENTE non viene
    // toccata: poco sotto, il LaunchedEffect sovrascrive con il valore salvato,
    // vuoto compreso.
    var userAgent by remember { mutableStateOf(org.openusenet.reader.Branding.USER_AGENT_PREDEFINITO) }
    var xNoArchive by remember { mutableStateOf(false) }
    var customHeaders by remember { mutableStateOf("") }
    var replyIntro by remember { mutableStateOf("") }
    var faceB64 by remember { mutableStateOf<String?>(null) }
    var faceSize by remember { mutableStateOf(48) }
    var isDefault by remember { mutableStateOf(false) }

    LaunchedEffect(identityId) {
        if (identityId > 0) {
            repo.identityById(identityId)?.let { i ->
                nickname = i.nickname
                name = i.name
                email = i.email
                signature = i.signature
                sigEnabled = i.sigEnabled
                sigRandom = i.sigRandom
                userAgent = i.userAgent
                xNoArchive = i.xNoArchive
                customHeaders = i.customHeaders
                replyIntro = i.replyIntro
                faceB64 = i.faceB64
                faceSize = i.faceSize
                isDefault = i.isDefault
            }
        } else {
            // 1.2.9: introduzione predefinita già tradotta (RC01) come valore
            // di partenza per una nuova identità — coerente con quella che
            // vedeva chi creava l'identità generale prima della 1.2.9.
            replyIntro = ""
        }
        caricata = true
    }

    val facePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val src = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    ?: error(ctx.getString(R.string.id_img_unreadable))
                val scaled = Bitmap.createScaledBitmap(src, faceSize, faceSize, true)
                val out = java.io.ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                faceB64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            }.onFailure {
                scope.launch { snackbar.showSnackbar(ctx.getString(R.string.id_img_invalid, it.message ?: "")) }
            }
        }
    }

    fun salva(onDone: () -> Unit) {
        scope.launch {
            repo.saveIdentity(
                Identity(
                    id = identityId,
                    nickname = nickname.trim().ifBlank { name.trim().ifBlank { email.trim() } },
                    name = name.trim(),
                    email = email.trim(),
                    sigEnabled = sigEnabled,
                    sigRandom = sigRandom,
                    signature = signature.trimEnd(),
                    userAgent = userAgent.trim(),
                    xNoArchive = xNoArchive,
                    customHeaders = customHeaders.trim(),
                    replyIntro = replyIntro.trim(),
                    faceB64 = faceB64,
                    faceSize = faceSize,
                    isDefault = isDefault,
                ),
            )
            // 2.1(b), esteso 1.2.9: myEmail/myEmails devono riflettere subito
            // l'identità appena salvata (nuova email, cambio di predefinita, ecc.).
            org.openusenet.reader.AppSettings.reloadIdentityEmails(repo)
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (identityId > 0) stringResource(R.string.id_title) else stringResource(R.string.id_new_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.indietroSicuro() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_cancel))
                    }
                },
                actions = {
                    if (identityId > 0) {
                        IconButton(onClick = {
                            scope.launch {
                                repo.deleteIdentity(identityId)
                                org.openusenet.reader.AppSettings.reloadIdentityEmails(repo)
                                nav.indietroSicuro()
                            }
                        }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete)) }
                    }
                    IconButton(onClick = {
                        salva {
                            scope.launch { snackbar.showSnackbar(ctx.getString(R.string.id_saved)) }
                            nav.indietroSicuro()
                        }
                    }) { Icon(Icons.Default.Save, contentDescription = stringResource(R.string.common_save)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!caricata) return@Scaffold
        Box(Modifier.padding(padding).fillMaxSize().imePadding()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = nickname, onValueChange = { nickname = it },
                    label = { Text(stringResource(R.string.id_nickname)) },
                    supportingText = { Text(stringResource(R.string.id_nickname_note)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.id_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text(stringResource(R.string.id_email)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { isDefault = !isDefault },
                ) {
                    Switch(checked = isDefault, onCheckedChange = { isDefault = it })
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(stringResource(R.string.id_default_switch), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.id_default_switch_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Switch(checked = sigEnabled, onCheckedChange = { sigEnabled = it })
                    Text(
                        stringResource(R.string.id_add_sig),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp, top = 12.dp),
                    )
                }
                Row {
                    Switch(checked = sigRandom, onCheckedChange = { sigRandom = it })
                    Text(
                        stringResource(R.string.id_random_sig),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp, top = 12.dp),
                    )
                }
                OutlinedTextField(
                    value = signature, onValueChange = { signature = it },
                    label = { Text(stringResource(R.string.id_sig_label)) },
                    supportingText = {
                        Text(stringResource(R.string.id_sig_note, MULTI_SEPARATOR, SIG_MAX_LINES, SIG_MAX_COLS))
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                )
                Text(
                    stringResource(R.string.id_sig_note2),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )

                Text(
                    stringResource(R.string.id_privacy),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                OutlinedTextField(
                    value = userAgent, onValueChange = { userAgent = it },
                    label = { Text(stringResource(R.string.id_useragent)) },
                    // 1.2.13: il nome del segnaposto arriva come parametro, così
                    // la nota resta corretta anche se un giorno cambiasse.
                    // 1.3.2: il predefinito usa ora %versione% (vedi Branding),
                    // quindi è quello il segnaposto da spiegare qui.
                    supportingText = { Text(stringResource(R.string.id_useragent_note, "%versione%")) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                Row {
                    Switch(checked = xNoArchive, onCheckedChange = { xNoArchive = it })
                    Text(
                        stringResource(R.string.id_noarchive),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                    )
                }

                Text(
                    stringResource(R.string.id_intro),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                OutlinedTextField(
                    value = replyIntro, onValueChange = { replyIntro = it },
                    label = { Text(stringResource(R.string.id_intro_tpl, introPredefinita)) },
                    supportingText = { Text(stringResource(R.string.id_intro_note, MULTI_SEPARATOR)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                )

                Text(
                    stringResource(R.string.id_custom_headers),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                OutlinedTextField(
                    value = customHeaders, onValueChange = { customHeaders = it },
                    label = { Text(stringResource(R.string.id_custom_headers_lbl)) },
                    supportingText = { Text(stringResource(R.string.id_custom_headers_note)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
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

                Text(
                    stringResource(R.string.id_face),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = faceSize == 48, onClick = { faceSize = 48 }),
                ) {
                    RadioButton(selected = faceSize == 48, onClick = null)
                    Text(stringResource(R.string.id_face_48), modifier = Modifier.padding(start = 8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = faceSize == 96, onClick = { faceSize = 96 }),
                ) {
                    RadioButton(selected = faceSize == 96, onClick = null)
                    Text(stringResource(R.string.id_face_96), modifier = Modifier.padding(start = 8.dp))
                }
                if (faceSize == 96) {
                    Text(
                        stringResource(R.string.id_face_warn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    )
                }
                Text(
                    stringResource(R.string.id_face_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val bmp = remember(faceB64) {
                        faceB64?.let {
                            runCatching {
                                val bytes = Base64.decode(it, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }.getOrNull()
                        }
                    }
                    if (bmp != null) {
                        Image(
                            bmp.asImageBitmap(),
                            contentDescription = stringResource(R.string.id_face_current),
                            modifier = Modifier.size(bmp.width.dp),
                        )
                    } else {
                        Text(stringResource(R.string.id_face_none), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(
                        onClick = {
                            // 1.5.6 / 174: senza selettore (box TV) si avvisa, non ci si chiude.
                            if (!facePicker.apriSeEsiste("image/*")) {
                                scope.launch { snackbar.showSnackbar(ctx.getString(R.string.common_no_picker)) }
                            }
                        },
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Text(stringResource(R.string.id_face_choose))
                    }
                    if (faceB64 != null) {
                        TextButton(onClick = { faceB64 = null }) { Text(stringResource(R.string.id_face_remove)) }
                    }
                }
                Text(
                    stringResource(R.string.id_face_std_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
