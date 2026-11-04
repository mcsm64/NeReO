/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.4.6 (§B4) — IL CATALOGO UNICO DELLE AZIONI DI BARRA.
 *
 * PERCHE' ESISTE. Fino alla 1.4.5 un'azione di barra viveva in QUATTRO posti:
 * l'elenco degli id in `Settings.kt`, l'icona e l'etichetta in due tabelle
 * `when` separate dentro `CustomizeBarsScreen.kt`, e l'implementazione nella
 * schermata che la esegue. Quattro posti vuol dire quattro occasioni di
 * dimenticarsene, e il progetto ne ha gia' pagata una: «esporta», aggiunta il
 * 14 settembre 2026 e registrata solo in tre, funzionava benissimo ma
 * nell'editor delle barre compariva con l'icona di ripiego e con l'ID NUDO al
 * posto del nome. Da quel guasto e' nato `controllo_barre.py`, che da allora
 * verifica a mano cio' che qui adesso non si puo' piu' sbagliare.
 *
 * COSA CAMBIA. Icona ed etichetta stanno insieme all'id, in una riga sola: i
 * quattro posti diventano DUE — il descrittore (qui) e l'implementazione, che
 * per forza di cose resta in ogni vista. Le due tabelle `when` di
 * `CustomizeBarsScreen` non spariscono: diventano una lettura di questo
 * catalogo, ripieghi compresi, identici a prima.
 *
 * L'ETICHETTA E' UN `Int`, NON UNA `String`. Cosi' il catalogo non e'
 * composable e non ha bisogno di `stringResource`: lo risolve chi disegna.
 * Un elenco di dati resta un elenco di dati.
 *
 * QUESTO FILE NON DECIDE CHI OFFRE COSA. Quali azioni siano disponibili nella
 * vista a pagine e quali nei pannelli resta in `Settings.articleBarActionIds` e
 * `Settings.paneBarActionIds`: sono due SELEZIONI da questo catalogo, ed e'
 * proprio la loro differenza — 16 contro 9 al 30 settembre 2026 — la misura di
 * §B4. Averla come dato, e non sparsa nel codice, e' meta' del lavoro.
 */
package org.openusenet.reader.ui.screens

import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.Avanti
import org.openusenet.reader.ui.icone.Download
import org.openusenet.reader.ui.icone.NavigateBefore
import org.openusenet.reader.ui.icone.NavigateNext
import org.openusenet.reader.ui.icone.Reply
import org.openusenet.reader.ui.icone.Subject
import org.openusenet.reader.ui.icone.Toc
import org.openusenet.reader.ui.icone.AccountTree
import org.openusenet.reader.ui.icone.AlternateEmail
import org.openusenet.reader.ui.icone.ContentCopy
import org.openusenet.reader.ui.icone.EnhancedEncryption
import org.openusenet.reader.ui.icone.FormatQuote
import org.openusenet.reader.ui.icone.KeyboardArrowDown
import org.openusenet.reader.ui.icone.KeyboardArrowUp
import org.openusenet.reader.ui.icone.KeyboardDoubleArrowLeft
import org.openusenet.reader.ui.icone.KeyboardDoubleArrowRight
import org.openusenet.reader.ui.icone.Link
import org.openusenet.reader.ui.icone.MarkChatUnread
import org.openusenet.reader.ui.icone.MyLocation
import org.openusenet.reader.ui.icone.Refresh
import org.openusenet.reader.ui.icone.SaveAlt
import org.openusenet.reader.ui.icone.Share
import org.openusenet.reader.ui.icone.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import org.openusenet.reader.R

/** Un'azione di barra: il suo id stabile, la sua icona, la sua etichetta. */
data class AzioneBarra(
    val id: String,
    val icona: ImageVector,
    /** id di risorsa: lo risolve chi disegna, cosi' questo file resta dati. */
    val etichetta: Int,
)

/**
 * Tutte le azioni di barra del progetto, in un posto solo.
 *
 * L'ordine e' quello storico di `articleBarActionIds` seguito dalle tre nate
 * nella 1.3.1 per il riquadro di lettura dei pannelli (`rot13`, `headers`,
 * `reply`). Non e' l'ordine in cui compaiono all'utente: quello lo decidono
 * `articleBarOrder` e `paneBarOrder`, che l'utente puo' cambiare.
 */
val CATALOGO_BARRE: List<AzioneBarra> = listOf(
    AzioneBarra("prev_msg", Icons.AutoMirrored.Filled.NavigateBefore, R.string.art_prev_msg),
    AzioneBarra("next_msg", Icons.AutoMirrored.Filled.NavigateNext, R.string.art_next_msg),
    AzioneBarra("next_unread", Icons.Default.MarkChatUnread, R.string.art_next_unread),
    // 1.6 (sondaggio sui gruppi): prossimo non letto, poi il gruppo successivo.
    AzioneBarra("avanti", Icons.Default.Avanti, R.string.art_avanti),
    AzioneBarra("prev_thread", Icons.Default.KeyboardDoubleArrowLeft, R.string.art_prev_thread),
    AzioneBarra("next_thread", Icons.Default.KeyboardDoubleArrowRight, R.string.art_next_thread),
    AzioneBarra("back_index", Icons.AutoMirrored.Filled.Toc, R.string.art_back_index),
    AzioneBarra("top", Icons.Default.KeyboardArrowUp, R.string.art_top),
    AzioneBarra("bottom", Icons.Default.KeyboardArrowDown, R.string.art_bottom),
    AzioneBarra("mute_quotes", Icons.Default.FormatQuote, R.string.art_mute_quotes_cd),
    AzioneBarra("tree_bottom", Icons.Default.AccountTree, R.string.art_tree_cd),
    // 1.3.1: nate per la barra del riquadro di lettura nei pannelli. Le
    // etichette esistevano gia' come descrizioni delle icone e funzionano tali
    // e quali come voci di elenco — nessuna stringa nuova.
    AzioneBarra("rot13", Icons.Default.EnhancedEncryption, R.string.art_rot13_dec),
    AzioneBarra("headers", Icons.AutoMirrored.Filled.Subject, R.string.art_headers_cd),
    AzioneBarra("reply", Icons.AutoMirrored.Filled.Reply, R.string.cmp_reply),
    // 1.3.3 (Roxy): copia il messaggio intero, un clic, senza selezione.
    AzioneBarra("copia", Icons.Default.ContentCopy, R.string.art_copy_message),
    // 1.3.6: esporta/condividi (Kerman), link permanente e Message-ID (ObiWan).
    AzioneBarra("esporta", Icons.Default.Share, R.string.art_export_message),
    // 1.4.5 (Kerman, §B9): il FILE, che un client di posta riconosce.
    AzioneBarra("eml", Icons.Default.SaveAlt, R.string.art_export_eml),
    // 1.4.9 (§J3-C1, da una domanda di Jeff): «conserva questo» — scrive il
    // messaggio nella cartella scelta UNA volta, senza passare dal selettore a
    // ogni salvataggio.
    //
    // ⚠️ L'icona NON e' un secondo dischetto. `SaveAlt` e' di "eml", e due
    // icone di salvataggio accanto nella stessa barra sarebbero due comandi che
    // l'utente deve provare per capire quale fa cosa. `Storage` dice «messo da
    // parte», che e' quello che questa fa davvero. Scelta anche fra quelle che
    // il progetto usa gia' altrove: un'icona inesistente nel pacchetto
    // material-icons-extended si scopre solo compilando, e compilare costa a
    // Marco un giro.
    AzioneBarra("conserva", Icons.Default.Storage, R.string.art_conserva),
    // 1.4: butta la copia locale e riscarica il corpo dal server.
    AzioneBarra("ricarica", Icons.Default.Refresh, R.string.art_reload),
    // 1.6 (sondaggio sui gruppi): solo nei pannelli; al telefono sta nel menu ⋮.
    AzioneBarra("ricostruisci", Icons.Default.Download, R.string.rb_title),
    AzioneBarra("link", Icons.Default.Link, R.string.art_copy_link),
    // 1.4.6 (par. B13, BIG Umberto): porta all'elenco del gruppo, posizionato
    // sul messaggio e con la sua discussione aperta. SOLO nella vista a pagine:
    // nei pannelli l'elenco e' gia' accanto, come back_index e tree_bottom.
    AzioneBarra("mostra_in_elenco", Icons.Default.MyLocation, R.string.art_show_in_list),
    AzioneBarra("msgid", Icons.Default.AlternateEmail, R.string.art_copy_msgid),
)

private val PER_ID: Map<String, AzioneBarra> = CATALOGO_BARRE.associateBy { it.id }

/** L'azione con quell'id, oppure null se l'id non e' del catalogo. */
fun azioneBarra(id: String): AzioneBarra? = PER_ID[id]

/**
 * Icona di ripiego per un id che non dovrebbe esistere. Era gia' questa prima
 * dell'estrazione e resta identica: un id ignoto non deve far cadere la
 * schermata, deve farsi notare.
 */
val ICONA_RIPIEGO: ImageVector = Icons.Default.AccountTree
