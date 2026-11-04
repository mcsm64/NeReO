/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.4.6 (§B4, fetta 3) — COSA FA UN'AZIONE DI LETTURA, detto una volta sola.
 *
 * IL PROBLEMA CHE RISOLVE. `BarraLetturaPannello` conteneva DUE tabelle `when`
 * gemelle sugli stessi nove id: una per i pulsanti in barra, una per le voci del
 * menu ⋮. Diciotto rami per nove azioni, e ogni azione nuova ne chiedeva due —
 * con la garanzia, prima o poi, di scriverne uno solo. Era gia' successo: la
 * fetta 1 ha portato la tabella della BARRA a leggere il catalogo unico e si e'
 * lasciata dietro quella del MENU, che ha continuato a dichiarare le icone a
 * mano. Una quinta copia della stessa mappa id->icona, sopravvissuta proprio
 * all'intervento che doveva eliminarla.
 *
 * ⚠️ LA RAGIONE PER CUI NON BASTAVA «spostare il codice nel chiamante». I punti
 * di chiamata della barra sono DUE — `PostContinuo` e `ArticlePaneSingolo` —
 * quindi portare su la logica avrebbe solo spostato la coppia di copie di un
 * piano. Qui invece la definizione e' UNA e le costruzioni sono due: ogni
 * riquadro porta la sua parte locale, tutto il resto e' condiviso per
 * costruzione, non per disciplina.
 *
 * COSA STA QUI E COSA NO. Stanno le azioni che agiscono sul MESSAGGIO mostrato:
 * copiarlo, esportarlo, i suoi identificativi, rispondere, ROT13, intestazioni.
 * NON sta la navigazione fra messaggi, che vive in `NavigazioneLettura.kt`
 * perche' ha bisogno della LISTA e non del singolo articolo — e i due fasci si
 * compongono nel chiamante invece di conoscersi a vicenda.
 *
 * LE TRE AZIONI CHE POSSONO MANCARE. «Inizio», «fine» e «ricarica» lavorano su
 * stato che vive nel riquadro, e il riquadro non e' sempre lo stesso: nella
 * vista CONTINUA non esiste uno scorrimento del singolo messaggio, perche' i
 * messaggi scorrono tutti insieme. Sono percio' lambda NULLABILI, e [disponibile]
 * lo dice a chi disegna.
 *
 * ⚠️ E qui la scelta che conta: un comando indisponibile si SPEGNE, non si
 * nasconde e non si lascia premere a vuoto. Nasconderlo farebbe cambiare la
 * barra sotto le dita passando da una vista all'altra; lasciarlo attivo lo
 * farebbe sembrare rotto. Un pulsante grigio, invece, dice la verita': questa
 * cosa esiste, qui non si puo' fare.
 */
package org.openusenet.reader.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
import org.openusenet.reader.ui.condividiTesto
import org.openusenet.reader.ui.linkUsenetId
import org.openusenet.reader.ui.messageIdCanonico
import org.openusenet.reader.ui.rememberAppunti

/**
 * Le azioni che agiscono sul messaggio mostrato, in un fascio solo.
 *
 * Le ultime tre sono nullabili perche' dipendono dal riquadro: vedi la nota in
 * testa al file.
 */
data class AzioniLettura(
    val rot13: () -> Unit,
    val intestazioni: () -> Unit,
    val rispondi: () -> Unit,
    val copiaMessaggio: () -> Unit,
    val esporta: () -> Unit,
    val salvaEml: () -> Unit,
    /** 1.4.9 (§J3-C1): scrive il messaggio nella cartella di «Conserva». */
    val conserva: () -> Unit,
    val copiaLink: () -> Unit,
    val copiaMessageId: () -> Unit,
    val vaiInCima: (() -> Unit)? = null,
    val vaiInFondo: (() -> Unit)? = null,
    val ricaricaCorpo: (() -> Unit)? = null,
    /** 1.6: null = il padre del messaggio c'e', quindi niente da cercare. */
    val ricostruisci: (() -> Unit)? = null,
)

/**
 * Costruisce il fascio, con dentro gli aiutanti che prima vivevano nel corpo
 * della barra.
 *
 * NON usa `remember`, ed e' voluto: le lambda devono vedere lo stato AGGIORNATO
 * del riquadro (il corpo che arriva dopo, ROT13 che si accende). Memorizzarle
 * congelerebbe la prima versione — che e' precisamente il difetto che
 * [testoMessaggio] evitava gia' prima essendo una lambda invece di una stringa.
 */
@Composable
fun azioniLettura(
    onRot13: () -> Unit,
    onHeaders: () -> Unit,
    onReply: () -> Unit,
    onSalvaEml: () -> Unit,
    /**
     * 1.4.9 (§J3-C1): come [onSalvaEml] serve il nome del file e il contenuto,
     * che dipendono dall'articolo del riquadro, questa resta una lambda del
     * chiamante e non si costruisce qui.
     */
    onConserva: () -> Unit,
    /**
     * 1.3.3 (Roxy): testo del messaggio COSI' COME E' VISUALIZZATO — ROT13 gia'
     * applicato se attivo. Lambda perche' il corpo puo' non essere ancora
     * arrivato: si valuta al clic, non a ogni ricomposizione.
     */
    testoMessaggio: () -> String,
    /** 1.3.6: l'articolo GREZZO, intestazioni comprese, per l'esportazione. */
    articoloDaEsportare: () -> String,
    /** 1.3.6 (ObiWan): il Message-ID come sta in archivio, parentesi comprese. */
    messageIdArticolo: () -> String?,
    vaiInCima: (() -> Unit)? = null,
    vaiInFondo: (() -> Unit)? = null,
    ricaricaCorpo: (() -> Unit)? = null,
    ricostruisci: (() -> Unit)? = null,
): AzioniLettura {
    // 1.3.5: un solo punto in tutta l'app legge gli appunti (ui/Appunti.kt).
    val appunti = rememberAppunti()
    val ctx = LocalContext.current
    val avvisoCopia = stringResource(R.string.art_copy_done)
    val titoloCondivisione = stringResource(R.string.art_export_chooser)

    // Su Android 13+ il sistema mostra da se' la conferma di copia; sotto no,
    // e senza un riscontro l'utente non sa se e' successo qualcosa.
    fun copiaNegliAppunti(testo: String) {
        if (testo.isBlank()) return
        appunti.copia(testo)
        if (android.os.Build.VERSION.SDK_INT < 33) {
            android.widget.Toast.makeText(ctx, avvisoCopia, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    return AzioniLettura(
        rot13 = onRot13,
        intestazioni = onHeaders,
        rispondi = onReply,
        copiaMessaggio = { copiaNegliAppunti(testoMessaggio()) },
        // 1.3.6 (Adam H. Kerman): consegna l'articolo grezzo a un'altra app.
        esporta = { condividiTesto(ctx, articoloDaEsportare(), titoloCondivisione) },
        salvaEml = onSalvaEml,
        conserva = onConserva,
        // 1.3.6 (ObiWan): link permanente e Message-ID — vedi ui/LinkArticolo.kt.
        copiaLink = { copiaNegliAppunti(linkUsenetId(messageIdArticolo())) },
        copiaMessageId = { copiaNegliAppunti(messageIdCanonico(messageIdArticolo())) },
        vaiInCima = vaiInCima,
        vaiInFondo = vaiInFondo,
        ricaricaCorpo = ricaricaCorpo,
        ricostruisci = ricostruisci,
    )
}

/**
 * Esegue l'azione [id] se e' di lettura; restituisce false se non lo e' o se in
 * questo riquadro non e' disponibile, cosi' il chiamante prosegue con le sue.
 *
 * E' il gemello di `NavigazioneMessaggi.esegui`, e i due si mettono in fila nel
 * chiamante: prima la navigazione, poi la lettura. L'ordine non conta — i due
 * insiemi di id sono disgiunti — ma metterli in fila invece di fonderli tiene
 * separato cio' che dipende dalla LISTA da cio' che dipende dal MESSAGGIO.
 */
fun AzioniLettura.esegui(id: String): Boolean {
    when (id) {
        "rot13" -> rot13()
        "headers" -> intestazioni()
        "reply" -> rispondi()
        "copia" -> copiaMessaggio()
        "esporta" -> esporta()
        "eml" -> salvaEml()
        "conserva" -> conserva()
        "link" -> copiaLink()
        "msgid" -> copiaMessageId()
        // 1.2.6 (Dave Royal): muta/mostra citazioni. E' una preferenza GLOBALE,
        // quindi si risolve qui e non risale al chiamante: farla passare per i
        // due riquadri avrebbe voluto dire due copie di due righe.
        "mute_quotes" -> {
            AppSettings.mutaCitazioni = !AppSettings.mutaCitazioni
            AppSettings.save()
        }
        // `?.invoke() ?: return false`: se la lambda manca, l'azione non e' di
        // questo riquadro e il chiamante lo deve sapere.
        "top" -> vaiInCima?.invoke() ?: return false
        "bottom" -> vaiInFondo?.invoke() ?: return false
        "ricarica" -> ricaricaCorpo?.invoke() ?: return false
        "ricostruisci" -> ricostruisci?.invoke() ?: return false
        else -> return false
    }
    return true
}

/** Le azioni di lettura che questo riquadro sa davvero fare. */
fun AzioniLettura.disponibile(id: String): Boolean = when (id) {
    "top" -> vaiInCima != null
    "bottom" -> vaiInFondo != null
    "ricarica" -> ricaricaCorpo != null
    "ricostruisci" -> ricostruisci != null
    else -> true
}
