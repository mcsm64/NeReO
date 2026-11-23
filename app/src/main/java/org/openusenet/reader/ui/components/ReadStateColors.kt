/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Colori di stato lettura per le liste messaggi: da leggere in rosso
 * ("fermati e leggi"), già letti in verde. Le tinte sono due per famiglia,
 * scelte in base alla luminosità dello sfondo, così restano leggibili sia in
 * tema chiaro sia in tema scuro.
 *
 * Nota di accessibilità: rosso/verde è la coppia critica per il daltonismo,
 * perciò il colore NON è mai l'unico segnale — i non letti restano in
 * grassetto, che funziona anche per chi non distingue le due tinte.
 */
package org.openusenet.reader.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val UNREAD_LIGHT = Color(0xFFC62828) // rosso profondo su fondo chiaro
private val UNREAD_DARK = Color(0xFFFF8A80)  // rosso chiaro su fondo scuro
private val READ_LIGHT = Color(0xFF2E7D32)   // verde bosco su fondo chiaro
private val READ_DARK = Color(0xFF81C784)    // verde tenue su fondo scuro

/** Colore del testo secondo lo stato di lettura, adattato al tema in uso. */
@Composable
fun readStateColor(read: Boolean): Color = readStateColor(read, MaterialTheme.colorScheme.surface)

/**
 * 1.6.5 / 194: lo stesso colore, scelto sullo sfondo dato invece che sul tema.
 * Serve al titolo della vista continua, che sta sullo sfondo di lettura: con
 * una tinta di lettura scura su un tema chiaro il verde scuro non si leggerebbe.
 */
fun readStateColor(read: Boolean, sfondo: Color): Color {
    val dark = sfondo.luminance() < 0.5f
    return when {
        read && dark -> READ_DARK
        read -> READ_LIGHT
        dark -> UNREAD_DARK
        else -> UNREAD_LIGHT
    }
}
