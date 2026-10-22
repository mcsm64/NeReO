/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Aspetto: tema chiaro/scuro/sistema, Material You, Colore Tasti.
 * Le modifiche si applicano immediatamente.
 */
package org.openusenet.reader.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.ArrowBack
import org.openusenet.reader.ui.icone.Check
import org.openusenet.reader.ui.icone.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
import org.openusenet.reader.ui.LocaleHelper
import org.openusenet.reader.ui.Routes
import org.openusenet.reader.ui.indietroSicuro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(nav: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_appearance)) },
                navigationIcon = {
                    IconButton(onClick = { nav.indietroSicuro() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            val ctx = LocalContext.current

            // Lingua dell'app (interruttore in-app). Su Android 13+ e' lo stesso
            // setting di sistema; su <= 12 e' l'unico modo. Vedi LocaleHelper.
            Text(stringResource(R.string.app_language), style = MaterialTheme.typography.titleMedium)
            val etichetteLingua = mapOf(
                "" to stringResource(R.string.app_language_system),
                "it" to stringResource(R.string.app_language_it),
                "en" to stringResource(R.string.app_language_en),
            )
            val linguaCorrente = LocaleHelper.tagCorrente(ctx)
            LocaleHelper.tagSupportati.forEach { tag ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { LocaleHelper.impostaLingua(ctx, tag) },
                ) {
                    RadioButton(
                        selected = linguaCorrente == tag,
                        onClick = { LocaleHelper.impostaLingua(ctx, tag) },
                    )
                    Text(etichetteLingua[tag] ?: tag)
                }
            }

            Text(
                stringResource(R.string.app_theme),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            listOf(
                "system" to stringResource(R.string.app_theme_system),
                "light" to stringResource(R.string.app_theme_light),
                "dark" to stringResource(R.string.app_theme_dark),
            ).forEach { (value, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        AppSettings.themeMode = value
                        AppSettings.save()
                    },
                ) {
                    RadioButton(
                        selected = AppSettings.themeMode == value,
                        onClick = { AppSettings.themeMode = value; AppSettings.save() },
                    )
                    Text(label)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Switch(
                        checked = AppSettings.dynamicColor,
                        onCheckedChange = { AppSettings.dynamicColor = it; AppSettings.save() },
                    )
                    Text(
                        stringResource(R.string.app_material_you),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }

            Text(
                stringResource(R.string.app_button_colour) +
                    (if (AppSettings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        stringResource(R.string.app_button_colour_note) else ""),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSettings.accentColors.forEach { (key, argb) ->
                    val selected = AppSettings.accent == key
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            )
                            .clickable { AppSettings.accent = key; AppSettings.save() },
                    ) {
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = key, tint = Color.White)
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.app_reading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = AppSettings.bodyMonospace,
                    onCheckedChange = { AppSettings.bodyMonospace = it; AppSettings.save() },
                )
                Text(stringResource(R.string.app_monospace), modifier = Modifier.padding(start = 12.dp))
            }

            Text(
                stringResource(R.string.app_text_size),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            AppSettings.fontScales.forEach { (label, value) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        AppSettings.bodyFontScale = value; AppSettings.save()
                    },
                ) {
                    RadioButton(
                        selected = AppSettings.bodyFontScale == value,
                        onClick = { AppSettings.bodyFontScale = value; AppSettings.save() },
                    )
                    // RC01: l'etichetta della taglia va tradotta QUI, non nella
                    // mappa di Settings: quella non è @Composable (non può usare
                    // stringResource) e i suoi nomi fanno da chiave interna.
                    // Traducendo solo ciò che si mostra, la logica resta intatta.
                    Text(
                        when (label) {
                            "Piccolissimo" -> stringResource(R.string.app_size_xs)
                            "Molto piccolo" -> stringResource(R.string.app_size_s)
                            "Piccolo" -> stringResource(R.string.app_size_m)
                            "Normale" -> stringResource(R.string.app_size_l)
                            "Grande" -> stringResource(R.string.app_size_xl)
                            "Molto grande" -> stringResource(R.string.app_size_xxl)
                            else -> label
                        }
                    )
                }
            }
            Text(
                stringResource(R.string.app_text_size_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.collapseBlankLines,
                    onCheckedChange = { AppSettings.collapseBlankLines = it; AppSettings.save() },
                )
                Text(stringResource(R.string.app_collapse_blank), modifier = Modifier.padding(start = 12.dp))
            }
            // 1.5.7 / 175: dissolvenza tra le schermate. Acceso di default.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Switch(
                    checked = AppSettings.animazioni,
                    onCheckedChange = { AppSettings.animazioni = it; AppSettings.save() },
                )
                Text(stringResource(R.string.app_animations), modifier = Modifier.padding(start = 12.dp))
            }
            Text(
                stringResource(R.string.app_animations_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            )
            // 1.5.7 / 175 (F8): sfogliare coi tasti del volume. Spento di default.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Switch(
                    checked = AppSettings.tastiVolume,
                    onCheckedChange = { AppSettings.tastiVolume = it; AppSettings.save() },
                )
                Text(stringResource(R.string.app_volume_keys), modifier = Modifier.padding(start = 12.dp))
            }
            Text(
                stringResource(R.string.app_volume_keys_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Switch(
                    checked = AppSettings.reflowText,
                    onCheckedChange = { AppSettings.reflowText = it; AppSettings.save() },
                )
                Text(stringResource(R.string.app_reflow_text), modifier = Modifier.padding(start = 12.dp))
            }
            Text(
                stringResource(R.string.app_reflow_text_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Switch(
                    checked = AppSettings.shortenQuotes,
                    onCheckedChange = { AppSettings.shortenQuotes = it; AppSettings.save() },
                )
                Text(stringResource(R.string.app_shorten_quotes), modifier = Modifier.padding(start = 12.dp))
            }
            // beta19 (Roxy): soglia configurabile. Prima era fissa a 6 righe e la
            // funzione sembrava inerte, perché molte citazioni sono più corte.
            // Visibile solo quando l'accorciamento è attivo.
            if (AppSettings.shortenQuotes) {
                Text(
                    stringResource(R.string.app_shorten_threshold),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                ) {
                    listOf(4, 6, 8, 12).forEach { n ->
                        FilterChip(
                            selected = AppSettings.sogliaAccorciaCitazioni == n,
                            onClick = { AppSettings.sogliaAccorciaCitazioni = n; AppSettings.save() },
                            label = { Text(stringResource(R.string.app_lines_count, n)) },
                        )
                    }
                }
            }
            // beta14 (Dave Royal): scelta stile citazioni
            Text(
                stringResource(R.string.app_quote_style),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 2.dp),
            )
            Column(Modifier.padding(start = 12.dp)) {
                listOf(
                    "bars" to stringResource(R.string.app_quote_bars),
                    "chevron" to stringResource(R.string.app_quote_marks),
                    "both" to stringResource(R.string.app_quote_both),
                ).forEach { (valore, etichetta) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { AppSettings.quoteStyle = valore; AppSettings.save() }
                            .padding(vertical = 2.dp),
                    ) {
                        RadioButton(
                            selected = AppSettings.quoteStyle == valore,
                            onClick = { AppSettings.quoteStyle = valore; AppSettings.save() },
                        )
                        Text(etichetta, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            // 1.2.9 (Marco): oltre una certa profondità i ">" ripetuti
            // diventano illeggibili, soprattutto su telefono in verticale.
            // Visibile solo con "chevron"/"both" — in "bars" i ">" non si
            // vedono comunque, l'opzione non avrebbe effetto.
            if (AppSettings.quoteStyle != "bars") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Switch(
                        checked = AppSettings.numericQuoteDepth,
                        onCheckedChange = { AppSettings.numericQuoteDepth = it; AppSettings.save() },
                    )
                    Text(stringResource(R.string.app_numeric_quote_depth), modifier = Modifier.padding(start = 12.dp))
                }
                Text(
                    stringResource(R.string.app_numeric_quote_depth_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                )
                if (AppSettings.numericQuoteDepth) {
                    Text(
                        stringResource(R.string.app_quote_threshold),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                    ) {
                        listOf(6, 8, 10, 12).forEach { n ->
                            FilterChip(
                                selected = AppSettings.sogliaCitazioniANumeri == n,
                                onClick = { AppSettings.sogliaCitazioniANumeri = n; AppSettings.save() },
                                label = { Text(stringResource(R.string.app_quote_levels_count, n)) },
                            )
                        }
                    }
                }
            }
            // 1.4.9 (§J1, Jeff) — LE ROTAIE DELL'ALBERO, E PERCHE' STANNO QUI.
            //
            // Jeff aveva scelto «solo i >» e vedeva comunque righe verticali:
            // sono le rotaie del rientro negli elenchi, un ornamento diverso
            // che si somiglia. Ha cercato il rimedio nello stile citazioni —
            // cioe' esattamente qui sopra — e non l'ha trovato, perche' non
            // c'era. Adesso c'e', e sta dove l'ha cercato: non e' estetica
            // della schermata, e' il posto in cui l'utente arriva col sintomo.
            //
            // La nota nomina le barre delle citazioni di proposito, cosi' chi
            // arriva qui capisce quale dei due ornamenti sta spegnendo.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.rotaieAlbero,
                    onCheckedChange = { AppSettings.rotaieAlbero = it; AppSettings.save() },
                )
                Text(stringResource(R.string.app_tree_rails), modifier = Modifier.padding(start = 12.dp))
            }
            Text(
                stringResource(R.string.app_tree_rails_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Switch(
                    checked = AppSettings.markReadOnExit,
                    onCheckedChange = { AppSettings.markReadOnExit = it; AppSettings.save() },
                )
                Text(stringResource(R.string.app_mark_read_exit), modifier = Modifier.padding(start = 12.dp))
            }
            // 1.3 (B — barre personalizzabili): l'interruttore diretto "Albero
            // anche in basso" (beta18) è sostituito dall'editor dedicato, che fa
            // la stessa cosa (mostra/nascondi "tree_bottom") più il riordino di
            // TUTTA la barra bassa di lettura. AppSettings.alberoInBasso resta
            // solo come seme di migrazione (vedi Settings.kt), non più scritto
            // da qui.
            Text(
                stringResource(R.string.app_bars_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
            OutlinedButton(onClick = { nav.navigate(Routes.CUSTOMIZE_BARS) }) {
                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.app_bars_customize))
            }
            Text(
                stringResource(R.string.app_bars_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            // beta18 (Roxy): vista continua del thread — tutti i post uno sotto
            // l'altro nel pannello di lettura (utile su TV Box, dove non si può
            // fare swipe verso l'alto col controller).
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Switch(
                    checked = AppSettings.vistaContinua,
                    onCheckedChange = { AppSettings.vistaContinua = it; AppSettings.save() },
                )
                Text(stringResource(R.string.app_continuous_view), modifier = Modifier.padding(start = 12.dp))
            }

            // 1.4.7 (§A16, Jeff) — L'INTERRUTTORE DOVE SI CERCA.
            //
            // `unreadOnly` parte ACCESO, vale per tutti i gruppi e si ricorda
            // fra un avvio e l'altro, ma fino alla 1.4.6 si poteva cambiare
            // SOLO dall'icona a busta in cima a una lista: niente, qui dentro.
            // Jeff, al primo giorno d'uso, ha cercato in Impostazioni, non ha
            // trovato nulla e ha concluso che NeReO gli cancellava i messaggi
            // letti. Non e' una funzione nuova: e' la stessa preferenza, resa
            // raggiungibile da dove la si cerca.
            //
            // La nota nomina l'icona di proposito: chi arriva qui per il
            // sintomo deve uscirne sapendo anche dov'e' il comando rapido.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Switch(
                    checked = AppSettings.unreadOnly,
                    onCheckedChange = { AppSettings.unreadOnly = it; AppSettings.save() },
                )
                Text(stringResource(R.string.app_unread_only), modifier = Modifier.padding(start = 12.dp))
            }
            Text(
                stringResource(R.string.app_unread_only_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Text(
                stringResource(R.string.app_quote_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp),
            )

            Text(
                stringResource(R.string.app_reading_background),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSettings.readingBackgrounds.forEach { (key, pair) ->
                    val selected = AppSettings.readingBg == key
                    val swatch = pair?.let { Color(it.first) } ?: MaterialTheme.colorScheme.surface
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            )
                            .clickable { AppSettings.readingBg = key; AppSettings.save() },
                    ) {
                        if (selected) {
                            val tick = pair?.let { Color(it.second) } ?: MaterialTheme.colorScheme.onSurface
                            Icon(Icons.Default.Check, contentDescription = key, tint = tick)
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.app_reading_bg_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                stringResource(R.string.app_ui_tint),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
            Text(
                stringResource(R.string.app_ui_tint_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            PaletteGrid()

            Text(
                stringResource(R.string.app_pane_density),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
            Text(
                stringResource(R.string.app_pane_density_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            AppSettings.paneListScales.forEach { (etichetta, fattore) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { AppSettings.paneListScale = fattore; AppSettings.save() }
                        .padding(vertical = 2.dp),
                ) {
                    RadioButton(
                        selected = kotlin.math.abs(AppSettings.paneListScale - fattore) < 0.01f,
                        onClick = { AppSettings.paneListScale = fattore; AppSettings.save() },
                    )
                    // RC01: etichetta tradotta alla visualizzazione (la mappa in Settings
                    // non è @Composable e le sue chiavi restano tecniche)
                    Text(when (etichetta) {
                            "Normale" -> stringResource(R.string.app_density_normal)
                            "Compatta" -> stringResource(R.string.app_density_compact)
                            "Fitta" -> stringResource(R.string.app_density_dense)
                            "Molto fitta" -> stringResource(R.string.app_density_verydense)
                            "Massima" -> stringResource(R.string.app_density_max)
                            else -> etichetta
                        }, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Switch(
                    checked = AppSettings.colorReadState,
                    onCheckedChange = { AppSettings.colorReadState = it; AppSettings.save() },
                )
                Text(
                    stringResource(R.string.app_colour_read_state_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Switch(
                    checked = AppSettings.prefetchReplies,
                    onCheckedChange = { AppSettings.prefetchReplies = it; AppSettings.save() },
                )
                Text(
                    stringResource(R.string.app_prefetch_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            // 1.2.9 (Piano B, Roxy): alternativa al +/- (ingrandito nella
            // correzione P) per chi mira da lontano — es. TV box con
            // telecomando. Spento di default: comportamento invariato per chi
            // non lo tocca.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Switch(
                    checked = AppSettings.tapOpToExpand,
                    onCheckedChange = { AppSettings.tapOpToExpand = it; AppSettings.save() },
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text(stringResource(R.string.app_tap_op_expand), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.app_tap_op_expand_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 1.3.2 (Marco): l'equivalente per il telefono, dove il tocco
            // naviga via invece di espandere sul posto. Anche questa spenta di
            // default.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Switch(
                    checked = AppSettings.apriPrimoNonLetto,
                    onCheckedChange = { AppSettings.apriPrimoNonLetto = it; AppSettings.save() },
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text(stringResource(R.string.app_open_first_unread), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.app_open_first_unread_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                stringResource(R.string.app_navigation),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = AppSettings.gesturesEnabled,
                    onCheckedChange = { AppSettings.gesturesEnabled = it; AppSettings.save() },
                )
                Text(
                    stringResource(R.string.app_gesture_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Switch(
                    checked = AppSettings.gestureDebug,
                    onCheckedChange = { AppSettings.gestureDebug = it; AppSettings.save() },
                )
                Text(
                    stringResource(R.string.app_gesture_debug_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            Text(
                stringResource(R.string.app_pane_layout),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
            )
            listOf(
                "auto" to stringResource(R.string.app_pane_auto),
                "always" to stringResource(R.string.app_pane_always),
                "never" to stringResource(R.string.app_pane_never),
            ).forEach { (mode, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { AppSettings.paneMode = mode; AppSettings.save() },
                ) {
                    RadioButton(
                        selected = AppSettings.paneMode == mode,
                        onClick = { AppSettings.paneMode = mode; AppSettings.save() },
                    )
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = AppSettings.paneVertical,
                    onCheckedChange = { AppSettings.paneVertical = it; AppSettings.save() },
                )
                Text(
                    if (AppSettings.paneVertical) stringResource(R.string.app_pane_rows) else stringResource(R.string.app_pane_cols),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Switch(
                    checked = AppSettings.soloIconaAggiornaPannelli,
                    onCheckedChange = { AppSettings.soloIconaAggiornaPannelli = it; AppSettings.save() },
                )
                Text(
                    stringResource(R.string.app_pane_refresh_icon_only),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                stringResource(R.string.app_pane_refresh_icon_only_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            )
            Text(
                stringResource(R.string.app_pane_ratios),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            PaneSlider(stringResource(R.string.app_pane_groups), AppSettings.paneWeightGroups) {
                AppSettings.paneWeightGroups = it; AppSettings.save()
            }
            PaneSlider(stringResource(R.string.app_pane_threads), AppSettings.paneWeightThreads) {
                AppSettings.paneWeightThreads = it; AppSettings.save()
            }
            PaneSlider(stringResource(R.string.app_pane_reading), AppSettings.paneWeightArticle) {
                AppSettings.paneWeightArticle = it; AppSettings.save()
            }
            OutlinedButton(
                onClick = {
                    AppSettings.paneWeightGroups = 1.0f
                    AppSettings.paneWeightThreads = 1.3f
                    AppSettings.paneWeightArticle = 1.6f
                    AppSettings.save()
                },
                modifier = Modifier.padding(top = 4.dp),
            ) { Text(stringResource(R.string.app_reset_ratios)) }
        }
    }
}

@Composable
private fun PaneSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 8.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0.5f..3f,
            steps = 24,
            modifier = Modifier.weight(1f),
        )
        Text(
            String.format(java.util.Locale.US, "%.1f", value),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * Selettore delle tinte: ogni riquadro e' un'anteprima reale (sfondo, barra e
 * testo della tinta), cosi' si sceglie guardando il risultato invece di
 * indovinare da un nome.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteGrid() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppSettings.uiPalettes.forEach { (key, palette) ->
            val selected = AppSettings.uiPalette == key
            val bg = palette?.let { Color(it.bg) } ?: MaterialTheme.colorScheme.background
            val surface = palette?.let { Color(it.surface) } ?: MaterialTheme.colorScheme.surfaceVariant
            val on = palette?.let { Color(it.on) } ?: MaterialTheme.colorScheme.onBackground
            Column(
                modifier = Modifier
                    .size(width = 96.dp, height = 74.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { AppSettings.uiPalette = key; AppSettings.save() },
            ) {
                // finta barra del titolo, per far vedere il secondo gradino
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(surface)
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = on,
                                modifier = Modifier.size(14.dp).padding(end = 2.dp),
                            )
                        }
                        Text(
                            // RC01: nome della tinta tradotto alla visualizzazione;
                            // la chiave della mappa resta tecnica (sepia, menta…)
                            when (key) {
                                "sepia" -> stringResource(R.string.tint_sepia)
                                "crema" -> stringResource(R.string.tint_cream)
                                "menta" -> stringResource(R.string.tint_mint)
                                "cielo" -> stringResource(R.string.tint_sky)
                                "lavanda" -> stringResource(R.string.tint_lavender)
                                "ardesia" -> stringResource(R.string.tint_slate)
                                "notte" -> stringResource(R.string.tint_night)
                                "bosco" -> stringResource(R.string.tint_forest)
                                "carbone" -> stringResource(R.string.tint_charcoal)
                                "purple" -> stringResource(R.string.tint_purple)
                                else -> palette?.label ?: stringResource(R.string.app_default)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = on,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
