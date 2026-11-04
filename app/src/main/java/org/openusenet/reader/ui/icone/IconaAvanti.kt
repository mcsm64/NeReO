/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6: l'icona del tasto Avanti, due triangoli verso destra. Sta fuori da
 * IconeNeReO.kt perche' quello e' un file generato dalla libreria Material, e
 * questa e' disegnata qui: stessa griglia da 24 e stessi attributi delle altre.
 */
package org.openusenet.reader.ui.icone

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.Dp

private var _avanti: ImageVector? = null

val Icons.Filled.Avanti: ImageVector
    get() = _avanti ?: ImageVector.Builder(
        name = "Filled.Avanti",
        defaultWidth = Dp(24f),
        defaultHeight = Dp(24f),
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = false,
    )
    .addPath(
        pathData = PathBuilder().apply {
            moveTo(4f, 6f)
            lineTo(12.5f, 12f)
            lineTo(4f, 18f)
            close()
            moveTo(13f, 6f)
            lineTo(21.5f, 12f)
            lineTo(13f, 18f)
            close()
        }.nodes,
        fill = SolidColor(Color.Black),
    )
    .build().also { _avanti = it }
