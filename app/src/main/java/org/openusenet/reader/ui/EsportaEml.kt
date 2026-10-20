/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * SALVARE UN ARTICOLO COME FILE .eml.
 *
 * PERCHE' ESISTE (1.4.5, §B9).
 * Adam H. Kerman, 13 settembre 2026: «allow the user to save the article to an
 * archived messages folder to allow him to reply with an email client». La
 * 1.3.6 aveva risposto con la CONDIVISIONE (ui/EsportaArticolo.kt): l'articolo
 * grezzo consegnato a un'altra app come testo. Risolve meta' del bisogno —
 * l'altra meta' e' un FILE, che un client di posta apre e riconosce come
 * messaggio invece di trovarselo incollato dentro.
 *
 * ==========================================================================
 *  PERCHE' NON BASTA SCRIVERE SU FILE QUELLO CHE GIA' SI CONDIVIDE
 * ==========================================================================
 *
 * Questo e' il punto tecnico della voce, e se si sbaglia il file e' rotto.
 *
 * NeReO salva il corpo GIA' DECODIFICATO: `setBody(id,
 * MimeUtil.decodeTextBodyRaw(headers, bodyLines))`. Il charset originale e il
 * Content-Transfer-Encoding sono gia' stati risolti, e in archivio resta testo
 * Kotlin, cioe' Unicode.
 *
 * Le INTESTAZIONI invece sono salvate grezze, com'erano. Quindi possono ancora
 * dire `charset=ISO-8859-15` e `Content-Transfer-Encoding: quoted-printable`,
 * mentre il corpo che ci sta sotto non e' piu' ne' l'uno ne' l'altro.
 *
 * Nella CONDIVISIONE questo non fa danno: passa del testo a un'altra app, e
 * nessuna intestazione viaggia insieme. In un FILE .eml fa danno eccome: il
 * client di posta legge quelle righe, ci crede, e ri-decodifica un testo che
 * era gia' decodificato. Ogni accento diventa spazzatura, e l'utente
 * incolpera' NeReO — con ragione, perche' il file lo abbiamo scritto noi.
 *
 * Quindi [articoloEml] RISCRIVE quelle due intestazioni per farle corrispondere
 * a cio' che il file contiene davvero: UTF-8, 8bit. Tutte le altre restano
 * intatte — From, Subject, Date, Message-ID, References sono l'articolo, e non
 * si toccano.
 *
 * *Un file che dichiara una cosa e ne contiene un'altra e' peggio di un file
 * che non si puo' scrivere: il secondo si nota subito.*
 *
 * RIPIEGAMENTO (RFC 5322 par. 2.2.3): un'intestazione puo' continuare sulle
 * righe seguenti, che iniziano con spazio o tabulazione. Togliendo una
 * `Content-Type:` lunga bisogna portarsi via anche le sue continuazioni,
 * altrimenti restano nel file come righe orfane e lo rendono illeggibile a
 * meta'.
 */
package org.openusenet.reader.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.openusenet.reader.R

/** Le due intestazioni che descrivono la CODIFICA, non il messaggio. */
private val INTESTAZIONI_DI_CODIFICA = listOf("content-type", "content-transfer-encoding")

/**
 * L'articolo nella forma di un messaggio RFC 5322 coerente con se' stesso:
 * intestazioni (con la codifica riscritta), riga vuota, corpo.
 *
 * Kotlin puro e senza Android: provata in `CoreTests`. Un file che finisce in
 * mano a un altro programma non si collauda a occhio.
 */
fun articoloEml(headersRaw: String?, corpo: String?): String {
    val corpoTesto = corpo.orEmpty()
    val righe = headersRaw?.trimEnd()?.lines().orEmpty()
    val tenute = ArrayList<String>(righe.size)
    var scartando = false
    for (r in righe) {
        val continuazione = r.startsWith(" ") || r.startsWith("\t")
        if (continuazione) {
            // Segue la sorte dell'intestazione a cui appartiene.
            if (!scartando) tenute.add(r)
            continue
        }
        val nome = r.substringBefore(':', "").trim().lowercase()
        scartando = nome in INTESTAZIONI_DI_CODIFICA
        if (!scartando) tenute.add(r)
    }
    // In coda, dove non possono essere scambiate per una continuazione di
    // qualcos'altro.
    tenute.add("Content-Type: text/plain; charset=UTF-8")
    tenute.add("Content-Transfer-Encoding: 8bit")
    return tenute.joinToString("\n") + "\n\n" + corpoTesto
}

/**
 * Rende [grezzo] utilizzabile come nome di file su qualunque filesystem,
 * troncato a [max] caratteri.
 *
 * Caratteri vietati: quelli che i filesystem rifiutano. Si sostituiscono invece
 * di toglierli, cosi' le parole non si incollano fra loro. Il punto e lo spazio
 * finali si tolgono perche' Windows li rifiuta in coda a un nome.
 *
 * 1.4.9 (§J3-C1): estratto da [nomeFileEml] perche' serve anche a
 * [nomeFileConserva] — ed e' la regola del quaderno, prima di scrivere la
 * seconda copia di una logica si estrae la prima. Il comportamento di
 * [nomeFileEml] non cambia di un carattere: le prove che lo fissano sono in
 * `CoreTests` da prima di questa estrazione.
 */
fun nomeFileSicuro(grezzo: String, max: Int = 80): String = grezzo
    .replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "_")
    .replace(Regex("\\s+"), " ")
    .trim()
    .take(max)
    .trimEnd('.', ' ')

/**
 * Nome di file proposto nel selettore di sistema.
 *
 * L'utente puo' cambiarlo: questo e' solo il punto di partenza, e deve essere
 * riconoscibile in una cartella di archivio fra altri cento. Si parte
 * dall'oggetto perche' e' l'unica cosa che l'utente ricorda; il Message-ID e'
 * la riserva quando l'oggetto e' vuoto, e non e' mai vuoto lui.
 */
fun nomeFileEml(oggetto: String, messageId: String): String {
    val grezzo = oggetto.ifBlank { messageId.trim('<', '>') }
    return (nomeFileSicuro(grezzo).ifBlank { "articolo" }) + ".eml"
}

/**
 * Restituisce la funzione «salva questo articolo come .eml», con il selettore
 * di sistema gia' allestito.
 *
 * Perche' un composable che restituisce una lambda invece di una funzione
 * normale: il selettore SAF e' un `ActivityResultLauncher`, che va registrato
 * nella composizione. Chiamarlo da due schermate senza questo giro avrebbe
 * significato registrarlo due volte, cioe' l'undicesima copia su due del
 * progetto — nella release che ne chiude altre quattro.
 *
 * Nessun permesso sul disco: il file lo apre l'utente col selettore di sistema,
 * ed e' la stessa strada gia' usata per il log e il kill-file.
 *
 * L'esito e' un avviso a comparsa e non uno snackbar per una ragione pratica:
 * le due colonne di lettura della vista a pannelli non hanno uno snackbar
 * proprio, ed e' lo stesso motivo per cui li' anche la conferma di copia e' un
 * avviso. Un messaggio che compare in una vista e non nell'altra sarebbe la
 * prossima riga di questo quaderno.
 */
@Composable
fun rememberSalvaEml(): (String, String) -> Unit {
    val ctx: Context = LocalContext.current
    // Il contenuto va ricordato fra il lancio del selettore e il suo ritorno:
    // in mezzo l'utente naviga nelle cartelle, e l'app puo' anche essere
    // ricomposta.
    var daScrivere by remember { mutableStateOf("") }
    val esitoOk = stringResource(R.string.art_eml_saved)
    val esitoKo = stringResource(R.string.art_eml_failed)
    val senzaSelettore = stringResource(R.string.common_no_picker)
    val selettore = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("message/rfc822"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val riuscito = runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { flusso ->
                flusso.write(daScrivere.toByteArray(Charsets.UTF_8))
            } ?: error("nessun flusso in scrittura")
        }.isSuccess
        android.widget.Toast
            .makeText(ctx, if (riuscito) esitoOk else esitoKo, android.widget.Toast.LENGTH_SHORT)
            .show()
    }
    return { nomeProposto, testo ->
        daScrivere = testo
        // 1.5.6 / 174: un box TV senza selettore di file avvisa, non si chiude.
        if (!selettore.apriSeEsiste(nomeProposto)) {
            android.widget.Toast.makeText(ctx, senzaSelettore, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
