/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.6.1 — le segnalazioni di BIG Umberto del 5/11: riflusso e firma.
 * I testi «veri» sono ricopiati dalla schermata di MesNews del suo post.
 */
package org.openusenet.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.data.Identity
import org.openusenet.reader.text.BODY_MAX_COLS
import org.openusenet.reader.text.avvolgiFlowed
import org.openusenet.reader.text.corpoDaInviare
import org.openusenet.reader.text.riflussoBlocco
import org.openusenet.reader.text.rigaSoloUrl
import org.openusenet.reader.text.spezzaRigheFirma
import org.openusenet.reader.text.stimaLarghezza
import org.openusenet.reader.ui.riformattaCitazioni
import org.openusenet.reader.ui.screens.signatureBlock
import org.openusenet.reader.ui.screens.trimSignatureLines

class RiflussoTest {
    private fun riflussa(t: String) = riflussoBlocco(t.split("\n"), 0)

    private val postVero = """
        Ci può stare, ma il tasto 'fumetto' erroneamente, già fa quasi la stessa
        funzione.
        Il tasto fumetto dovrebbe agire *solo* nella discussione che sto
        leggendo, invece se premuto alla fine o all'ultimo non letto della
        discussione, passa al primo non letto fra le discussioni successive.
        Rimane sempre illuminato e diventa grigio solo se hai letto tutte le
        discussioni successive nel gruppo.
        Quindi *NON TI ACCORGI* di essere all'ultimo messaggio *NON LETTO* in
        quella discussione, e ti trovi in una discussione che non ti interessa o
        in un 'pecorazzi et similia'
        Questo lo fa sia se è selezionato 'vai al primo non letto quando apri
        una discussione' sia se quello switch è disabilitato.
        Questo, non sono sicuro, mi sembra di averlo visto anche nella 1.5.9.

        Altra cosa, ma lo vidi diverse versioni fa...
        https://postimg.cc/f3dqHP0R
        È una colonna singola di numeri telefonici, ma viene messa in 3 colonne.
        Questo solo con lo switch 'riflusso testo' attivato.

        Ultima cosa la firma.
        È una frase tratta da un libro che su 72 colonne occupa 3 righe, ma che
        è scritta tutta su una riga, lasciando al programma di lettura come
        spezzarla.
        https://postimg.cc/5Hk936L8
        Come vedi è mozzata in partenza.
        È impostata nella pagina del server, comune per tutti i suoi gruppi.
    """.trimIndent()

    @Test
    fun `post vero - i capoversi come li ha scritti Umberto`() {
        assertEquals(
            listOf(
                "Ci può stare, ma il tasto 'fumetto' erroneamente, già fa quasi la stessa funzione.",
                "Il tasto fumetto dovrebbe agire *solo* nella discussione che sto leggendo, invece se premuto alla fine o all'ultimo non letto della discussione, passa al primo non letto fra le discussioni successive.",
                "Rimane sempre illuminato e diventa grigio solo se hai letto tutte le discussioni successive nel gruppo.",
                "Quindi *NON TI ACCORGI* di essere all'ultimo messaggio *NON LETTO* in quella discussione, e ti trovi in una discussione che non ti interessa o in un 'pecorazzi et similia'",
                "Questo lo fa sia se è selezionato 'vai al primo non letto quando apri una discussione' sia se quello switch è disabilitato.",
                "Questo, non sono sicuro, mi sembra di averlo visto anche nella 1.5.9.",
                "",
                "Altra cosa, ma lo vidi diverse versioni fa...",
                "https://postimg.cc/f3dqHP0R",
                "È una colonna singola di numeri telefonici, ma viene messa in 3 colonne.",
                "Questo solo con lo switch 'riflusso testo' attivato.",
                "",
                "Ultima cosa la firma.",
                "È una frase tratta da un libro che su 72 colonne occupa 3 righe, ma che è scritta tutta su una riga, lasciando al programma di lettura come spezzarla.",
                "https://postimg.cc/5Hk936L8",
                "Come vedi è mozzata in partenza.",
                "È impostata nella pagina del server, comune per tutti i suoi gruppi.",
            ),
            riflussa(postVero),
        )
    }

    @Test
    fun `una colonna di numeri resta una colonna`() {
        val numeri = listOf("+0003901155500190", "+090619258776", "+12018577757", "+13155785288", "+13376237501")
        val t = "Questa è la lista aggiornata di tutti i numeri di call center e\n" +
            "spammer/truffatori telefonici vari per i vostri filtri di blocco\n" +
            "antispam, versione di novembre 2026:\n\n" + numeri.joinToString("\n")
        assertEquals(numeri, riflussa(t).takeLast(numeri.size))
    }

    @Test
    fun `una colonna subito sotto il testo non si attacca alla riga lunga`() {
        val r = riflussa(
            "Ecco la lista dei numeri che mi chiamano da settimane, qualcuno li conosce?\n" +
                "0212345678\n0298765432\n0645678901\nGrazie a chi risponderà.",
        )
        assertEquals("0212345678", r[1])
        assertEquals(5, r.size)
    }

    @Test
    fun `la prosa a 72 colonne si ricompone`() {
        val r = riflussa(
            "Questo è un capoverso normale andato a capo a settantadue colonne come\n" +
                "fanno quasi tutti i programmi di posta e di news, e la riga finisce dove\n" +
                "finisce perché la parola dopo non ci stava più dentro il margine scelto\n" +
                "dal mittente, che resta la stessa per tutto il messaggio.",
        )
        assertEquals(1, r.size)
    }

    @Test
    fun `il pettine si ricuce - la 1_2_4 di Bingo3331 non torna`() {
        val r = riflussa(
            "Questa è una riga che arrivava quasi al margine delle settantadue\n" +
                "colonne\n" +
                "e che dopo la citazione è stata spezzata lasciando una coda corta\n" +
                "corta\n" +
                "e poi il discorso prosegue ancora per un bel pezzo su questa riga\n" +
                "finale",
        )
        assertEquals(1, r.size)
    }

    @Test
    fun `chi impagina a 40 colonne viene ricomposto, orfana compresa`() {
        val r = riflussa(
            "Autore che impagina molto stretto a circa\n" +
                "quaranta colonne e lascia qualche orfana\n" +
                "come\n" +
                "questa in mezzo a un capoverso normale\n" +
                "senza nessuna punteggiatura particolare",
        )
        assertEquals(1, r.size)
    }

    /** Il limite noto, scritto: due righe corte di fila sembrano una colonna. */
    @Test
    fun `pettine vero con due righe corte - resta spezzato`() {
        val r = riflussa(
            "Ci può stare, ma il tasto 'fumetto' erroneamente, già fa quasi la\n" +
                "stessa\nfunzione.\n" +
                "Il tasto fumetto dovrebbe agire *solo* nella discussione che sto\n" +
                "leggendo, invece se premuto alla fine o all'ultimo non letto della",
        )
        assertEquals("stessa", r[1])
        assertEquals("funzione.", r[2])
    }

    @Test
    fun `un URL lungo non falsa la stima`() {
        val url = "https://example.com/" + "a".repeat(70)
        val t = listOf(
            "Questo capoverso è impaginato a sessanta colonne e dovrebbe",
            "ricomporsi comunque anche se più sotto compare un indirizzo",
            url,
        )
        assertEquals(59, stimaLarghezza(t))
        assertEquals(url, riflussoBlocco(t, 0).last())
        assertEquals(2, riflussoBlocco(t, 0).size)
    }

    @Test
    fun `etichette, saluti e versi restano al loro posto`() {
        val r = riflussa(
            "Ciao a tutti, volevo raccontarvi quello che mi è successo oggi al mare.\n" +
                "Nome: Mario\nCittà: Udine\nSaluti\nMarco",
        )
        assertEquals(5, r.size)
        val versi = riflussa(
            "Vi riporto una poesia che ho trovato ieri sera in un vecchio libro di casa:\n" +
                "Nel mezzo del cammin di nostra vita\nmi ritrovai per una selva oscura\n" +
                "che la diritta via era smarrita",
        )
        assertEquals(4, versi.size)
    }

    @Test
    fun `sotto la larghezza minima il blocco resta intatto`() {
        val t = "riga corta\naltra riga corta\nterza"
        assertEquals(t.split("\n"), riflussa(t))
    }

    @Test
    fun `nel citato si rimette il prefisso, e i link restano soli`() {
        val r = riflussoBlocco(
            listOf(
                "> Questa è una riga che arrivava quasi al margine delle settantadue",
                "> colonne di testo",
                "> https://postimg.cc/f3dqHP0R",
                "> E poi il discorso riprende da qui.",
            ),
            1,
        )
        assertEquals("> https://postimg.cc/f3dqHP0R", r[1])
        assertEquals(3, r.size)
    }

    @Test
    fun `riconosce la riga di solo URL`() {
        assertTrue(rigaSoloUrl("https://postimg.cc/f3dqHP0R"))
        assertTrue(rigaSoloUrl("  www.example.org  "))
        assertTrue(rigaSoloUrl("<https://example.org/a>"))
        assertFalse(rigaSoloUrl("vedi https://postimg.cc/f3dqHP0R"))
        assertFalse(rigaSoloUrl(""))
    }

    /** La stessa regola in scrittura: «Riformatta» non attacca piu' i link. */
    @Test
    fun `riformatta - link corto e colonna restano, la frase si ricuce`() {
        val link = "> vedi questo indirizzo qui sotto che spiega bene tutta la faccenda\n" +
            "> https://postimg.cc/f3dqHP0R\n" +
            "> e poi il discorso prosegue con altre parole fino a riempire la riga"
        assertEquals(link, riformattaCitazioni(link))
        val colonna = "> Ecco la lista dei numeri che mi chiamano da settimane, chi li conosce?\n" +
            "> 0212345678\n> 0298765432\n> 0645678901"
        assertEquals(colonna, riformattaCitazioni(colonna))
    }
}

class FirmaTest {
    private val frase = "Signori, un annuncio importante. Questo e' il volo 121 per Los Angeles, " +
        "e il comandante vi informa che il viaggio sara' lungo ma tranquillo, " +
        "salvo imprevisti che per ora nessuno e' in grado di immaginare."

    private fun parole(s: String) = s.split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun firma(testo: String, casuale: Boolean = false) =
        signatureBlock(Identity(nickname = "roby", signature = testo, sigRandom = casuale))

    @Test
    fun `una riga lunga si spezza alle parole e non perde niente`() {
        val r = firma(frase).lines()
        assertEquals("-- ", r.first())
        assertEquals(parole(frase), parole(r.drop(1).joinToString(" ")))
        assertTrue(r.drop(1).all { it.length <= BODY_MAX_COLS })
        assertEquals(3, r.size - 1)
    }

    @Test
    fun `oltre i 288 caratteri non si perde niente`() {
        val lunghissima = "parola ".repeat(90).trimEnd()
        assertEquals(parole(lunghissima), parole(firma(lunghissima).lines().drop(1).joinToString(" ")))
    }

    @Test
    fun `le righe che stanno nel limite restano identiche`() {
        assertEquals("-- \nMarco\n\nUdine", firma("Marco\n\nUdine"))
    }

    @Test
    fun `il limite delle quattro righe scritte resta`() {
        assertEquals("-- \nuno\ndue\ntre\nquattro", firma("uno\ndue\ntre\nquattro\ncinque"))
    }

    @Test
    fun `un indirizzo piu' lungo del limite resta intero`() {
        val url = "https://example.org/" + "a".repeat(80)
        assertEquals(listOf(url), spezzaRigheFirma(listOf(url)))
    }

    @Test
    fun `firme multiple - anche quella estratta si spezza`() {
        val r = firma("$frase\n%%\n$frase", casuale = true).lines()
        assertTrue(r.drop(1).all { it.length <= BODY_MAX_COLS })
        assertEquals(parole(frase), parole(r.drop(1).joinToString(" ")))
    }

    @Test
    fun `in lettura la riga lunga altrui non si taglia`() {
        assertEquals(listOf(frase), trimSignatureLines(listOf(frase)))
    }

    @Test
    fun `all'invio la firma spezzata non cambia, con e senza flowed`() {
        val corpo = "Testo del messaggio.\n\n" + firma(frase)
        assertEquals(corpo, corpoDaInviare(corpo, flowed = false))
        assertEquals(corpo, avvolgiFlowed(corpo))
    }
}
