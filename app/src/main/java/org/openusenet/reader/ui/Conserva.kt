/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * 1.4.9 (§J3-C1) — «CONSERVA QUESTO»: UNA CARTELLA SCELTA UNA VOLTA.
 *
 * DA DOVE NASCE. Jeff, 7 ottobre 2026, chiedendo se NeReO tenga una copia dei
 * messaggi: «does NeReO store posts?». La risposta era no — e la domanda ha
 * fatto vedere a Marco quello che gli mancava: in MesNews, da PC, un messaggio
 * che vale la pena si mette da parte e resta li' anche quando il server lo
 * scade e la pulizia automatica fa il suo giro.
 *
 * ==========================================================================
 *  IL NOME E' LA PROMESSA, E PERCIO' NON SI CHIAMA «ARCHIVIO»
 * ==========================================================================
 *
 * Un «archivio» promette permanenza, ricerca e un elenco da sfogliare. Questa
 * voce non da' nessuna delle tre, e chiamarla archivio sarebbe una promessa
 * che il codice non mantiene. «Conserva questo» promette una cosa sola, quella
 * che fa davvero: quel messaggio ESCE dal database e diventa un file
 * dell'utente, quindi non lo porta piu' via niente di cio' che accade dentro
 * NeReO — ne' la pulizia a giorni, ne' una disiscrizione, ne' un riscarico.
 *
 * ⚠️ E IL ROVESCIO VA DETTO ACCANTO AL COMANDO, NON SOLO NEL README. Un
 * messaggio conservato NeReO non lo cerca piu': la ricerca legge il database, e
 * quel file non ci sta. E' la lezione di §A16 — un'impostazione spiegata solo in
 * un documento non e' spiegata — e qui vale doppio, perche' chi conserva lo fa
 * proprio per ritrovare.
 *
 * ==========================================================================
 *  PERCHE' NON BASTAVA IL SALVATAGGIO .eml CHE C'E' DALLA 1.4.5
 * ==========================================================================
 *
 * `rememberSalvaEml` (ui/EsportaEml.kt) usa `CreateDocument`, che chiede la
 * destinazione A OGNI SALVATAGGIO. Per il gesto occasionale — «questo lo giro al
 * mio client di posta», la richiesta di Kerman — e' la forma giusta. Per mettere
 * da parte un messaggio dopo l'altro e' un selettore di file da attraversare
 * ogni volta, e nessuno conserva niente a quel prezzo.
 *
 * Qui si usa `OpenDocumentTree` UNA volta, si prende il permesso PERSISTENTE su
 * quella cartella, e da li' in poi «Conserva» e' un tocco. Il contenuto del file
 * e' lo stesso: [articoloEml], con la riscrittura del charset che la 1.4.5 ha
 * reso necessaria. Le due voci convivono e non si sovrappongono.
 *
 * ==========================================================================
 *  I TRE RISCHI DICHIARATI, E COME SONO CHIUSI
 * ==========================================================================
 *
 * (1) IL PERMESSO SI PUO' PERDERE. L'utente sposta o cancella la cartella,
 *     revoca l'accesso, svuota i dati dell'app; oppure — ed e' il caso meno
 *     ovvio — RIPRISTINA UN BACKUP SU UN ALTRO TELEFONO. Il backup di NeReO
 *     salva le impostazioni iterando TUTTE le chiavi delle preferenze
 *     (backup/BackupManager.kt, #4b: «completo per costruzione»), quindi l'URI
 *     della cartella viaggia col backup mentre il permesso, che e' legato
 *     all'installazione, no. Ripristinando altrove la cartella SEMBREREBBE
 *     impostata e ogni scrittura fallirebbe.
 *
 *     ⚠️ Rimedio: il fallimento si CLASSIFICA invece di contarlo. Un permesso
 *     che non c'e' si manifesta come `SecurityException`, e quello e' il solo
 *     caso in cui l'impostazione si svuota, si dice all'utente che la cartella
 *     non c'e' piu', e il selettore si riapre col messaggio ancora in mano —
 *     cosi' il gesto si conclude invece di andare perduto. Un fallimento di
 *     altra natura (spazio esaurito, supporto staccato, nome rifiutato dal
 *     provider) si riferisce e basta: la cartella non e' il colpevole e
 *     dimenticarla sarebbe un danno in piu'.
 *
 *     ⚠️ Non si e' aggiunta un'eccezione al backup: avrebbe risolto un caso
 *     solo e rotto l'invariante «completo per costruzione» di quel file,
 *     mentre classificare il fallimento risolve tutti i casi — backup
 *     compreso, ma anche la revoca manuale e la cartella cancellata — e tocca
 *     solo codice nuovo.
 *
 *     ⚠️ E non si interroga il sistema in anticipo. `persistedUriPermissions`
 *     direbbe subito se il permesso c'e', ma sarebbe una chiamata binder a
 *     ogni ricomposizione della schermata di Manutenzione per anticipare una
 *     risposta che la scrittura da' comunque. *Un controllo preventivo che
 *     duplica quello che l'operazione stessa rileva e' un costo senza una
 *     informazione in piu'.*
 *
 * (2) I NOMI DI FILE COLLIDONO. In un'unica cartella due «Re: ciao» si
 *     sovrascriverebbero. [nomeFileConserva] aggiunge in coda la parte locale
 *     del Message-ID, che e' unica per definizione: due messaggi diversi non
 *     possono produrre lo stesso nome. (`createDocument` numera da se' i
 *     duplicati, ma un nome che si distingue A OCCHIO vale piu' di un «(1)».)
 *
 * (3) LA RICERCA NON LI VEDE. Detto nella nota accanto all'interruttore in
 *     Manutenzione e nel manuale — vedi sopra.
 *
 * NESSUN PERMESSO NUOVO NEL MANIFEST: la cartella la concede l'utente col
 * selettore di sistema, ed e' la stessa strada del log, del kill-file e del
 * salvataggio .eml.
 *
 * ==========================================================================
 *  ⚠️ INCERTEZZA DICHIARATA: QUATTRO FIRME ANDROID NON VERIFICATE ALLA FONTE
 * ==========================================================================
 *
 * Le pagine di riferimento di developer.android.com sono gusci JavaScript e non
 * si leggono come testo; l'ambiente in cui questo file e' stato scritto non ha
 * l'SDK Android e la politica di rete gli nega Maven Central, quindi non c'e'
 * stato modo di leggere le firme nel sorgente. Sono quattro, tutte del nucleo
 * SAF, e sono scritte a memoria:
 *
 *   DocumentsContract.getTreeDocumentId(Uri): String
 *   DocumentsContract.buildDocumentUriUsingTree(Uri, String): Uri
 *   DocumentsContract.createDocument(ContentResolver, Uri, String, String): Uri?
 *   ContentResolver.takePersistableUriPermission(Uri, Int)
 *
 * Se la compilazione segnala un riferimento non risolto, e' una di queste
 * quattro righe e non altro: si corregge sulla firma vera, non si indovina.
 * *Un dato tecnico sbagliato e' peggio di un'incertezza dichiarata* — e questa
 * incertezza costa un messaggio del compilatore, non un difetto pubblicato.
 */
package org.openusenet.reader.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R

/** Il tipo MIME di un messaggio di posta: lo stesso del salvataggio .eml. */
private const val MIME_MESSAGGIO = "message/rfc822"

/**
 * Nome del file conservato: l'oggetto per leggerlo, il Message-ID per
 * distinguerlo.
 *
 * In una cartella sola l'oggetto non basta — «Re: ciao» ce ne sono dieci — e il
 * Message-ID da solo non si legge. Quindi entrambi: oggetto ripulito, due
 * sottolineature, e la parte locale del Message-ID, che e' unica per
 * definizione (RFC 5322 par. 3.6.4: «globally unique»).
 *
 * Kotlin puro e senza Android, come [nomeFileEml]: si prova in `CoreTests`.
 */
fun nomeFileConserva(oggetto: String, messageId: String): String {
    val titolo = nomeFileSicuro(oggetto, 60)
    // La parte locale, non quella del dominio: il dominio e' lo stesso per tutti
    // i messaggi di quel server e non distinguerebbe nulla.
    val coda = nomeFileSicuro(messageId.trim('<', '>').substringBefore('@'), 24)
        .replace(' ', '_')
    return when {
        titolo.isBlank() && coda.isBlank() -> "articolo.eml"
        titolo.isBlank() -> "$coda.eml"
        coda.isBlank() -> "$titolo.eml"
        else -> titolo + "__" + coda + ".eml"
    }
}

/**
 * La cartella di «Conserva» come la conosce l'impostazione, o `null` se non e'
 * ancora stata scelta.
 *
 * ⚠️ Dice che cosa e' SCRITTO, non che sia ancora utilizzabile: il permesso si
 * verifica scrivendo — vedi il rischio (1) in testa al file.
 */
fun cartellaConservaSalvata(): Uri? {
    val salvata = AppSettings.cartellaConserva
    if (salvata.isBlank()) return null
    return runCatching { Uri.parse(salvata) }.getOrNull()
}

/**
 * Descrizione leggibile della cartella scelta, per mostrarla in Manutenzione.
 *
 * Si ricava dall'identificativo dell'albero (es. `primary:Documents/NeReO`)
 * senza aprire niente: e' un'etichetta, non un accesso, e deve poter comparire
 * in una riga di impostazioni senza costare una lettura su disco.
 */
fun descriviCartellaConserva(uri: Uri): String? = runCatching {
    val id = DocumentsContract.getTreeDocumentId(uri)
    id.substringAfter(':').ifBlank { id }
}.getOrNull()

/**
 * Come e' andata una scrittura. I tre casi vogliono tre rimedi diversi, ed e'
 * per questo che non e' un `Boolean`: vedi il rischio (1).
 */
private enum class EsitoScrittura { OK, PERMESSO_PERSO, FALLITA }

/** I due comandi di «Conserva»: conservare un messaggio, e scegliere la cartella. */
class ControlliConserva(
    /** Conserva un messaggio: nome di file proposto, contenuto .eml. */
    val conserva: (String, String) -> Unit,
    /** Apre il selettore di cartella, senza conservare nulla. */
    val scegliCartella: () -> Unit,
)

/**
 * Allestisce «Conserva»: il selettore di cartella registrato nella
 * composizione, e le due funzioni che lo usano.
 *
 * Perche' un composable che restituisce delle lambda invece di funzioni normali:
 * `OpenDocumentTree` e' un `ActivityResultLauncher` e va registrato nella
 * composizione — la stessa ragione per cui esiste [rememberSalvaEml], e la
 * stessa forma, cosi' chi conosce una conosce l'altra.
 */
@Composable
fun rememberConserva(): ControlliConserva {
    val ctx: Context = LocalContext.current
    // Il messaggio va ricordato fra il lancio del selettore e il suo ritorno: in
    // mezzo l'utente naviga nelle cartelle e l'app puo' essere ricomposta. E'
    // lo stesso motivo del `daScrivere` di rememberSalvaEml.
    var inAttesa by remember { mutableStateOf<Pair<String, String>?>(null) }
    val esitoOk = stringResource(R.string.cns_saved)
    val esitoKo = stringResource(R.string.cns_failed)
    val cartellaPersa = stringResource(R.string.cns_folder_lost)
    val cartellaScelta = stringResource(R.string.cns_folder_set)
    val senzaSelettore = stringResource(R.string.common_no_picker)

    fun avvisa(testo: String) {
        android.widget.Toast.makeText(ctx, testo, android.widget.Toast.LENGTH_LONG).show()
    }

    fun scrivi(albero: Uri, nome: String, testo: String): EsitoScrittura {
        val esito = runCatching {
            val cartella = DocumentsContract.buildDocumentUriUsingTree(
                albero,
                DocumentsContract.getTreeDocumentId(albero),
            )
            val file = DocumentsContract
                .createDocument(ctx.contentResolver, cartella, MIME_MESSAGGIO, nome)
                ?: error("createDocument ha restituito null")
            ctx.contentResolver.openOutputStream(file)?.use { flusso ->
                flusso.write(testo.toByteArray(Charsets.UTF_8))
            } ?: error("nessun flusso in scrittura")
        }
        return when {
            esito.isSuccess -> EsitoScrittura.OK
            // ⚠️ L'UNICO caso in cui si dimentica la cartella: un permesso che
            // non c'e' piu'. Tutto il resto e' un guasto del momento, e
            // dimenticare la cartella per uno spazio esaurito farebbe pagare
            // all'utente una scelta che aveva fatto bene.
            esito.exceptionOrNull() is SecurityException -> EsitoScrittura.PERMESSO_PERSO
            else -> EsitoScrittura.FALLITA
        }
    }

    fun dimenticaCartella() {
        AppSettings.cartellaConserva = ""
        AppSettings.save()
    }

    val selettore = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { albero ->
        val pendente = inAttesa
        inAttesa = null
        if (albero == null) return@rememberLauncherForActivityResult
        // Il permesso PERSISTENTE e' il punto di tutta la voce: senza questa
        // chiamata la cartella sarebbe valida solo fino al prossimo riavvio, e
        // «scelta una volta» diventerebbe «scelta fino a stasera».
        val preso = runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                albero,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.isSuccess
        if (!preso) {
            avvisa(esitoKo)
            return@rememberLauncherForActivityResult
        }
        AppSettings.cartellaConserva = albero.toString()
        AppSettings.save()
        // Se il selettore si era aperto PER conservare qualcosa, quel qualcosa si
        // conserva adesso: il gesto dell'utente era «conserva», non «scegli una
        // cartella», e deve concludersi.
        if (pendente == null) {
            avvisa(cartellaScelta)
        } else {
            avvisa(if (scrivi(albero, pendente.first, pendente.second) == EsitoScrittura.OK) esitoOk else esitoKo)
        }
    }

    // 1.5.6 / 174: un box TV senza selettore di file avvisa, non si chiude.
    // Le tre aperture del selettore qui sotto passano tutte di qui.
    fun apriSelettore() {
        if (!selettore.apriSeEsiste(null)) {
            inAttesa = null
            avvisa(senzaSelettore)
        }
    }

    return ControlliConserva(
        conserva = { nome, testo ->
            val albero = cartellaConservaSalvata()
            if (albero == null) {
                inAttesa = nome to testo
                apriSelettore()
            } else {
                when (scrivi(albero, nome, testo)) {
                    EsitoScrittura.OK -> avvisa(esitoOk)
                    EsitoScrittura.PERMESSO_PERSO -> {
                        // Si dice, si dimentica, e si riapre il selettore col
                        // messaggio ancora in mano. Un salvataggio che non
                        // avviene in silenzio e' peggio di un errore.
                        dimenticaCartella()
                        avvisa(cartellaPersa)
                        inAttesa = nome to testo
                        apriSelettore()
                    }
                    EsitoScrittura.FALLITA -> avvisa(esitoKo)
                }
            }
        },
        scegliCartella = {
            inAttesa = null
            apriSelettore()
        },
    )
}
