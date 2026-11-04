/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * LA RIGA CHE DICE DI CHI SONO I GRUPPI QUI SOTTO, IN UN POSTO SOLO.
 *
 * PERCHE' ESISTE QUESTO FILE (1.4.4, §A9).
 * Roxy, 28 settembre: nella vista a pannelli la colonna dei gruppi e' una lista
 * unica, e con due gruppi OMONIMI su server diversi non c'e' modo di sapere
 * quale sia quale. Marco aveva la stessa nota sul suo taccuino.
 *
 * La vista a pagine l'intestazione ce l'aveva da sempre. Aggiungerne una
 * seconda copia nei pannelli sarebbe stato il nono caso della serie "presente
 * in una copia su due" — quella che questa settimana ha gia' prodotto una
 * regressione (il Box perso nell'estrazione della 118). Quindi la riga vive
 * qui, e le due viste la chiamano.
 *
 * LO SLOT `azione` E' IL MOTIVO PER CUI L'ESTRAZIONE FUNZIONA. Nella vista a
 * pagine l'intestazione porta anche il menu di aggiornamento selettivo di quel
 * server; nei pannelli, per ora, no. Senza lo slot le due intestazioni
 * sarebbero rimaste due funzioni diverse, cioe' due copie con un altro nome.
 */
package org.openusenet.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openusenet.reader.data.Server

/**
 * Intestazione di un server dentro un elenco di gruppi.
 *
 * @param etichetta come si chiama il server. **Usare [etichettaServer]** invece
 *        di passare `host` o `name` a mano: la scelta fra i due non e'
 *        indifferente e sta scritta li'.
 * @param azione contenuto opzionale in coda alla riga (il menu di
 *        aggiornamento selettivo, nella vista a pagine). Chi lo passa si porta
 *        dietro anche il proprio `Box` di ancoraggio, se contiene un menu:
 *        vedi `controllo_ancore.py` e la lezione della 119.
 */
@Composable
fun IntestazioneServer(
    etichetta: String,
    modifier: Modifier = Modifier,
    azione: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Text(
            etichetta,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
        azione()
    }
}

/**
 * Il nome di un gruppo che si adatta allo spazio che ha.
 *
 * PERCHE' ESISTE (1.4.5, §A11-bis).
 * Richiesta di Dave Royal, 28 settembre, quando ha accettato la sesta icona
 * nella barra dei pannelli: *«the group name should preferably be abbreviated
 * in the same way as in column 1 then truncated to fit the available space
 * without line-wrap. If the result is incomprehensible then I can refer to
 * column 1 where the group is highlighted.»*
 *
 * LA LOGICA C'ERA GIA', IN COLONNA 1, ED E' SUA: dalla wishlist beta12, prima
 * il nome INTERO (fino a [righe] righe), e solo se anche cosi' va in overflow
 * si passa alla forma abbreviata — mai troncare a meta' parola quando c'e'
 * spazio per il nome vero. Ricopiarla in colonna 2 sarebbe stata la **decima**
 * copia su due del progetto, e per giunta nella release che ne chiude altre
 * quattro.
 *
 * LA DIFFERENZA FRA LE DUE COLONNE E' UN PARAMETRO, NON UNA FUNZIONE NUOVA.
 * La colonna 1 e' una lista e puo' spendere due righe; la colonna 2 e' una
 * barra alta una riga e sta accanto alle icone, quindi [righe] = 1. E' la
 * stessa lezione dello slot di [IntestazioneServer]: *quando due copie
 * divergono su un dettaglio, il dettaglio diventa un parametro.*
 *
 * L'ellissi resta come ultima difesa: se nemmeno l'abbreviato ci sta, meglio
 * un nome tagliato che una barra sfondata — ed e' il caso in cui Dave dice che
 * guardera' la colonna 1.
 */
@Composable
fun NomeGruppoAdattivo(
    nome: String,
    modifier: Modifier = Modifier,
    righe: Int = 2,
    style: TextStyle = LocalTextStyle.current,
) {
    var abbreviato by remember(nome) { mutableStateOf(false) }
    Text(
        if (abbreviato) abbreviateGroupName(nome) else nome,
        style = style,
        maxLines = if (abbreviato) 1 else righe,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (it.hasVisualOverflow) abbreviato = true },
        modifier = modifier,
    )
}

/**
 * Il nome di un gruppo e, sotto, una riga di contesto in piccolo: i [dettagli]
 * (in lettura il contatore `[5/8]` e il segno `+N` dei crosspost) e il
 * [server], separati da « · ». Senza niente da dire, la riga piccola non c'e'.
 *
 * PERCHE' ESISTE (1.6 / 180, RPoggi via Usenet: «il nome del gruppo mentre si
 * legge»). La forma «nome sopra, server sotto solo con piu' server» e' della
 * 1.4.4 (§A9, Roxy), ma era scritta a mano in DUE posti — la barra
 * dell'elenco discussioni e l'intestazione della colonna 2 dei pannelli. La
 * lettura sarebbe stata la terza copia, e il tutto schermo dei pannelli la
 * quarta: ora i quattro posti chiamano questa, e la regola del server sta in
 * [serverDaMostrare].
 *
 * @param style lo stile del NOME. Il resto della riga e' sempre `labelSmall`.
 */
@Composable
fun TitoloGruppo(
    nome: String,
    server: String,
    modifier: Modifier = Modifier,
    dettagli: String = "",
    style: TextStyle = LocalTextStyle.current,
) {
    val sotto = listOf(dettagli, server).filter { it.isNotEmpty() }.joinToString(" · ")
    Column(modifier) {
        NomeGruppoAdattivo(nome = nome, righe = 1, style = style)
        if (sotto.isNotEmpty()) {
            Text(
                sotto,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Quale server nominare accanto a un gruppo: **nessuno** (stringa vuota) se i
 * server configurati sono al massimo uno — non c'e' niente da distinguere, e
 * una riga in piu' costerebbe altezza senza dire nulla (1.4.4, §A9). Vuota
 * anche per un gruppo orfano, il cui server non esiste piu'.
 */
fun serverDaMostrare(servers: List<Server>, serverId: Long?): String =
    if (servers.size <= 1) ""
    else servers.firstOrNull { it.id == serverId }
        ?.let { etichettaServer(it.name, it.host) }.orEmpty()

/**
 * Quanti ALTRI gruppi oltre a [corrente] compaiono in [gruppi] (la riga
 * `Newsgroups:`, gia' spezzata da `crosspostGroups`). Doppioni e maiuscole non
 * contano due volte. E' il numero del segno `+N` nella barra di lettura (1.6,
 * voluto da Marco).
 */
fun altriGruppiCrosspost(gruppi: List<String>, corrente: String): Int =
    gruppi.map { it.trim().lowercase() }
        .filter { it.isNotEmpty() && it != corrente.trim().lowercase() }
        .distinct().size

/**
 * Come si chiama un server quando lo si mostra all'utente.
 *
 * **Il nome, non l'host, e la scelta e' motivata.** Fino alla 1.4.4 la vista a
 * pagine mostrava `host`: con due account sullo STESSO provider — due host
 * identici — quell'intestazione non avrebbe distinto niente, che e' esattamente
 * il difetto che §A9 corregge. Il `name` e' l'etichetta che l'utente decide lui
 * quando crea il server, quindi e' l'unica che puo' essere diversa per forza.
 *
 * Ripiego su `host` se il nome e' vuoto: i server creati dai preset nascono con
 * un nome, ma un campo di testo si puo' sempre svuotare a mano.
 */
fun etichettaServer(nome: String, host: String): String = nome.ifBlank { host }

/**
 * Spezza un elenco di gruppi nelle sezioni da mostrare, una per server, e
 * restituisce `(etichetta, gruppi)` nell'ordine in cui vanno disegnate.
 *
 * Esiste per due ragioni, e la seconda e' quella che conta.
 *
 * 1. Le due viste raggruppavano (o non raggruppavano) per conto proprio.
 *
 * 2. **I GRUPPI ORFANI.** Un `serverId` puo' non corrispondere piu' a nessun
 *    server: non e' teorico, nella diagnostica LISTGROUP di BIG Umberto
 *    comparivano tre righe `? — nomegruppo`, ed e' il difetto che ha prodotto
 *    la pulizia degli orfani della 1.4.3. Un `servers.forEach` scritto a mano
 *    li fa SPARIRE dall'elenco — la vista a pagine li perde cosi' da sempre, e
 *    i pannelli li avrebbero persi passando dalla lista piatta alle sezioni.
 *    Qui finiscono in una sezione di coda etichettata `?`, la stessa
 *    convenzione che usa gia' `subscribedGroupsWithServerNames`. Un gruppo che
 *    c'e' nel database e non si vede nell'elenco e' peggio di un gruppo con
 *    un'intestazione brutta: il secondo si puo' cancellare, il primo no.
 *
 * @param serverDi come si ricava il `serverId` dall'elemento — le due viste
 *        portano tipi diversi (`Group` e `GroupWithTotal`).
 */
fun <T> raggruppaPerServer(
    elementi: List<T>,
    servers: List<Server>,
    serverDi: (T) -> Long,
): List<SezioneServer<T>> {
    val perServer = elementi.groupBy(serverDi)
    val sezioni = servers.mapNotNull { s ->
        perServer[s.id]?.takeIf { it.isNotEmpty() }
            ?.let { SezioneServer(s, etichettaServer(s.name, s.host), it) }
    }
    val idNoti = servers.map { it.id }.toSet()
    val orfani = elementi.filter { serverDi(it) !in idNoti }
    return if (orfani.isEmpty()) sezioni else sezioni + SezioneServer(null, "?", orfani)
}

/**
 * Una sezione dell'elenco gruppi.
 *
 * @param server il server di questa sezione, **`null` per la sezione degli
 *        orfani**. Chi disegna un'azione nell'intestazione (il menu di
 *        aggiornamento selettivo) la mette solo quando il server c'e': un
 *        server che non esiste piu' non si puo' sincronizzare, e offrirlo
 *        sarebbe un pulsante che non puo' funzionare.
 */
data class SezioneServer<T>(
    val server: Server?,
    val etichetta: String,
    val elementi: List<T>,
)
