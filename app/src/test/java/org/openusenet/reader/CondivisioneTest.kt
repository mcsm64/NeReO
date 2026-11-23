/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader

import org.junit.Assert.assertEquals
import org.junit.Test
import org.openusenet.reader.text.Condiviso
import org.openusenet.reader.text.testoCondiviso

/** 1.6.4 (Halv), 1.6.5 (Dave Royal): oggetto e corpo da cio' che manda l'app che condivide. */
class CondivisioneTest {
    @Test
    fun `browser - titolo nell'oggetto e link fra parentesi nel corpo`() = assertEquals(
        Condiviso("Una pagina", "<https://esempio.it/a/b?c=1>\n"),
        testoCondiviso("Una pagina", "https://esempio.it/a/b?c=1"),
    )

    @Test
    fun `senza oggetto - la prima riga che non e' un link diventa l'oggetto`() = assertEquals(
        Condiviso("Un video da vedere", "<https://youtu.be/abc123>\n"),
        testoCondiviso(null, "Un video da vedere\nhttps://youtu.be/abc123"),
    )

    @Test
    fun `titolo e link attaccati - il link va su una riga da solo`() = assertEquals(
        Condiviso("Notizia del giorno", "<https://feedly.example/n/42>\n"),
        testoCondiviso("", "Notizia del giorno https://feedly.example/n/42"),
    )

    @Test
    fun `il titolo ripetuto nel testo non si ripete nel corpo`() = assertEquals(
        Condiviso("Titolo", "Due righe di riassunto.\n\n<https://x.org/p>\n"),
        testoCondiviso("Titolo", "Titolo\n\nDue righe di riassunto.\n\n\n\nhttps://x.org/p"),
    )

    @Test
    fun `il punto dopo il link resta fuori dal link`() = assertEquals(
        Condiviso("Guarda", "<https://x.org/p>\n"),
        testoCondiviso(null, "Guarda https://x.org/p."),
    )

    @Test
    fun `un link lungo resta intero`() {
        val link = "https://www.example.com/" + "segmento/".repeat(20) + "fine?a=1&b=2"
        assertEquals("<$link>\n", testoCondiviso("T", link).corpo)
    }

    @Test
    fun `un link gia' fra parentesi non si raddoppia`() = assertEquals(
        Condiviso("T", "<https://x.org/p>\n"),
        testoCondiviso("T", "<https://x.org/p>"),
    )

    @Test
    fun `oggetto su piu' righe diventa una riga`() =
        assertEquals("Uno due", testoCondiviso("Uno\n  due ", "https://x.org").oggetto)

    @Test
    fun `solo un link, nessun oggetto`() =
        assertEquals(Condiviso("", "<https://x.org/p>\n"), testoCondiviso(null, "https://x.org/p"))

    @Test
    fun `testo selezionato senza link - oggetto vuoto e tutto nel corpo`() = assertEquals(
        Condiviso("", "Prima riga\nseconda riga\n\nterza\n"),
        testoCondiviso(null, "Prima riga\nseconda riga\n\nterza"),
    )

    @Test
    fun `l'oggetto dell'app non vale senza link - Share via`() = assertEquals(
        Condiviso("", "Un paragrafo intero, senza a capo, preso da una pagina web.\n"),
        testoCondiviso("Share via", "Un paragrafo intero, senza a capo, preso da una pagina web."),
    )

    @Test
    fun `niente da condividere`() = assertEquals(Condiviso("", ""), testoCondiviso(null, null))
}
