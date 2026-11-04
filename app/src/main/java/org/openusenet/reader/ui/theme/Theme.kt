/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Tema: colori (accento, Material You o tinta di sfondo) e TIPOGRAFIA SCALATA
 * A LIVELLO DI APP.
 * La dimensione scelta in Aspetto vale per l'intera interfaccia (liste, menù,
 * impostazioni, lettura), non solo per il corpo dei messaggi. L'interlinea è
 * volutamente compatta (stile lettore classico): righe più vicine, più testo
 * a schermo.
 */
package org.openusenet.reader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import org.openusenet.reader.AppSettings

/** Rapporto interlinea/dimensione: 1.20 = compatto (il default Material è ~1.45). */
private const val LINE_HEIGHT_RATIO = 1.20f

private fun TextStyle.scaled(scale: Float): TextStyle =
    if (fontSize.isSpecified) {
        copy(
            fontSize = fontSize * scale,
            lineHeight = fontSize * scale * LINE_HEIGHT_RATIO,
        )
    } else {
        this
    }

private fun Typography.scaled(scale: Float) = Typography(
    displayLarge = displayLarge.scaled(scale),
    displayMedium = displayMedium.scaled(scale),
    displaySmall = displaySmall.scaled(scale),
    headlineLarge = headlineLarge.scaled(scale),
    headlineMedium = headlineMedium.scaled(scale),
    headlineSmall = headlineSmall.scaled(scale),
    titleLarge = titleLarge.scaled(scale),
    titleMedium = titleMedium.scaled(scale),
    titleSmall = titleSmall.scaled(scale),
    bodyLarge = bodyLarge.scaled(scale),
    bodyMedium = bodyMedium.scaled(scale),
    bodySmall = bodySmall.scaled(scale),
    labelLarge = labelLarge.scaled(scale),
    labelMedium = labelMedium.scaled(scale),
    labelSmall = labelSmall.scaled(scale),
)

@Composable
fun OpenUsenetTheme(content: @Composable () -> Unit) {
    val palette = AppSettings.uiPalettes[AppSettings.uiPalette]

    // Una tinta di sfondo porta con se' la propria polarita': "Sepia" e' chiara
    // e "Blu notte" e' scura per costruzione. Quando una tinta e' attiva e'
    // lei a decidere chiaro/scuro, altrimenti si otterrebbero combinazioni
    // illeggibili (testo chiaro su fondo sepia).
    val darkTheme = palette?.dark ?: when (AppSettings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val seed = Color(AppSettings.accentColors[AppSettings.accent] ?: 0xFF3F51B5)

    val base = when {
        // con una tinta attiva il colore dinamico di sistema viene ignorato:
        // sceglierebbe sfondi propri e cancellerebbe la tinta
        palette == null && AppSettings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> AppSettings.accentColorsDark[AppSettings.accent]?.let { (colore, testo) ->
            darkColorScheme(primary = Color(colore), onPrimary = Color(testo), tertiary = Color(colore), onTertiary = Color(testo))
        } ?: darkColorScheme(primary = seed, tertiary = seed)
        else -> lightColorScheme(primary = seed, tertiary = seed)
    }

    val colorScheme = if (palette == null) base else base.copy(
        background = Color(palette.bg),
        onBackground = Color(palette.on),
        surface = Color(palette.bg),
        onSurface = Color(palette.on),
        surfaceContainer = Color(palette.surface),
        surfaceContainerLow = Color(palette.surface),
        surfaceContainerHigh = Color(palette.surface),
        surfaceContainerHighest = Color(palette.surface),
        surfaceVariant = Color(palette.surface),
        onSurfaceVariant = Color(palette.on).copy(alpha = 0.75f),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography().scaled(AppSettings.bodyFontScale),
        content = content,
    )
}
