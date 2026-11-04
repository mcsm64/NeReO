/*
 * Risposta ai crosspost (beta17 — richiesta Bingo3331, design deciso da Marco).
 *
 * Quando si risponde a un messaggio crospostato su più gruppi, l'utente può
 * scegliere in QUALI gruppi far uscire la risposta:
 *  - default: solo il gruppo corrente (mantiene la linea "niente crosspost per
 *    distrazione");
 *  - opzione: aggiungere altri gruppi dell'originale, fino a un massimo di 3
 *    TOTALI (cap deciso da Marco);
 *  - il testo NON viene mai cancellato: oltre il cap la spunta semplicemente non
 *    risponde più (il "pressostato ferma la pompa, non svuota il serbatoio").
 *
 * Questo file contiene: l'estrazione dei gruppi dall'header `Newsgroups:` e il
 * dialog-selettore condiviso fra ComposeScreen (a pagine) e ComposePane (a
 * pannelli), così la UI è in un solo punto di manutenzione.
 */
package org.openusenet.reader.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.openusenet.reader.AppSettings
import org.openusenet.reader.R
import org.openusenet.reader.data.Identity
import org.openusenet.reader.data.NewsRepository

/** Numero massimo di gruppi su cui una risposta può uscire (deciso da Marco). */
const val CROSSPOST_MAX_GROUPS = 3

// NB: `crosspostGroups(headersRaw)` è già definita in ThreadFlatten.kt (stesso
// package org.openusenet.reader.ui.screens) — la riuso, non la ridefinisco.

/**
 * I gruppi su cui questo messaggio uscirà davvero: il gruppo corrente più gli
 * extra scelti, senza vuoti e senza ripetizioni, nell'ordine in cui compariranno
 * nell'intestazione `Newsgroups:`.
 *
 * NASCE DA UNA SEGNALAZIONE DI DAVE ROYAL (17 settembre 2026):
 *   «When replying to multiple groups, the compose window only shows the group I
 *    am reading. […] this makes me think it's only going to one group or wonder
 *    whether I specified the groups correctly.»
 *
 * Fino alla 1.3.7 la schermata di scrittura mostrava `Gruppo: <uno>` — al
 * singolare, il solo gruppo che si stava leggendo — mentre la risposta poteva
 * uscire su tre. Non è un difetto estetico: riguarda **ciò che NeReO mette sulla
 * rete**, ed è la stessa categoria per cui la 1.3.6 è uscita prima della 1.4.
 * Quello che finisce negli archivi altrui non si corregge dopo.
 *
 * Funzione PURA e provata in `CoreTests`, non logica dentro una @Composable:
 * i punti che la usano sono due (vista a pagine e vista a pannelli), ed è
 * esattamente la soglia oltre la quale questo progetto estrae invece di
 * copiare.
 *
 * `LinkedHashSet`: toglie i duplicati SENZA riordinare. Il gruppo corrente resta
 * primo — è quello in cui si sta leggendo, e per convenzione Usenet è il
 * riferimento della risposta.
 */
fun gruppiDestinazione(corrente: String, extra: List<String>): List<String> {
    val out = LinkedHashSet<String>()
    if (corrente.isNotBlank()) out.add(corrente.trim())
    for (g in extra) if (g.isNotBlank()) out.add(g.trim())
    return out.toList()
}

/**
 * L'etichetta che dice DOVE uscira' il messaggio, in un posto solo.
 *
 * 1.4.6 - §A13.1, dalle prove 9A di Dave Royal del 29 settembre 2026. Le due
 * viste avevano ognuna la sua copia di questa scelta, e TUTTE E DUE, con un
 * `Followup-To` a un solo gruppo, mostravano il gruppo che si stava LEGGENDO
 * invece di quello in cui il messaggio sarebbe uscito: si leggeva
 * «Group: it.test» una riga sopra l'avviso che diceva uk.test.
 *
 * NON ERA UNA SVISTA, ed e' la lezione che vale oltre il caso. Fino alla 1.4
 * `destinazioni` con un elemento solo ERA SEMPRE il gruppo di lettura: i due
 * valori erano interscambiabili, e usare il secondo era comodo e innocuo. Il
 * `Followup-To` li ha fatti divergere, e da quel momento ogni punto che usava
 * il secondo per comodita' e' diventato un difetto - continuando a funzionare
 * in tutti i casi tranne quello nuovo, quindi senza annunciarsi.
 *
 * Estratta invece di corretta due volte: era un termine in due posti, e il
 * principio 2 del quaderno dice di estrarre prima di aggiungere la N-esima
 * copia. Il ripiego «…» copre il caso - in pratica irraggiungibile - di una
 * lista vuota: `gruppiDestinazione` restituisce vuoto solo se il gruppo
 * corrente e' in bianco e non ci sono extra. Il segnaposto tipografico e'
 * uguale in tutte le lingue, quindi non serve una risorsa: la nota arriva da
 * qui dalla `displayedGroupName` dei pannelli, sparita con l'estrazione -
 * quando si estrae un componente si porta via anche il suo contenitore.
 */
@Composable
fun etichettaDestinazioni(destinazioni: List<String>): String =
    if (destinazioni.size > 1) {
        stringResource(R.string.cmp_groups, destinazioni.joinToString(", "))
    } else {
        stringResource(
            R.string.cmp_group,
            destinazioni.firstOrNull()?.takeIf { it.isNotBlank() } ?: "…",
        )
    }

/**
 * 1.6 (Dave Royal) — il nome dell'identità da mostrare in scrittura, o null se
 * non va mostrata: con una sola identità non c'è niente da distinguere, e se
 * nome o indirizzo sono stati cambiati a mano (o vengono da una bozza) la riga
 * direbbe il falso. Il confronto sull'indirizzo ignora le maiuscole, come
 * [AppSettings.eMioPost].
 */
fun etichettaIdentita(
    nickname: String,
    nome: String,
    email: String,
    quanteIdentita: Int,
    fromName: String,
    fromEmail: String,
): String? {
    if (quanteIdentita < 2 || nickname.isBlank()) return null
    if (nome != fromName.trim() || !email.equals(fromEmail.trim(), ignoreCase = true)) return null
    return nickname
}

/** La riga «Identità: X» sopra il messaggio, nelle due viste. Vedi [etichettaIdentita]. */
@Composable
fun RigaIdentita(repo: NewsRepository, identita: Identity?, fromName: String, fromEmail: String) {
    val tutte by repo.identities.collectAsState(initial = emptyList())
    val i = identita ?: return
    val nick = etichettaIdentita(i.nickname, i.name, i.email, tutte.size, fromName, fromEmail) ?: return
    Text(
        stringResource(R.string.cmp_identity, nick),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/**
 * I gruppi EXTRA con cui una risposta a un crosspost deve NASCERE, prima che
 * l'utente scelga qualcosa.
 *
 * PERCHE' ESISTE, ED E' LA CORREZIONE DI §A10 (1.4.5).
 * L'impostazione «rispondi sempre a tutti i gruppi» esiste dalla 1.2.7 (Dave
 * #8), ma era consultata in UN SOLO POSTO: l'inizializzazione di
 * [CrosspostReplyDialog]. Nella vista a PANNELLI quel dialogo si apre sempre
 * prima della scrittura, quindi l'impostazione veniva onorata. Nella vista a
 * PAGINE, dalla beta17, si va dritti in scrittura — il dialogo c'e' ma si apre
 * solo se lo chiedi — e `extraGroups` nasceva vuoto: l'impostazione, pur
 * accesa, non aveva alcun effetto.
 *
 * Dave Royal, 19 settembre: *«with reply-to-all-groups on, replying to a
 * crosspost in phone mode the compose screen shows only the group I was
 * reading»*. Aveva ragione, e la voce delle note della 1.4 — «rispondendo a un
 * crosspost ora si vede dove il messaggio andra' davvero» — era vera **solo a
 * pannelli**.
 *
 * NON E' UNA COPIA DI LOGICA IN PIU', E' QUELLA DI PRIMA MESSA IN COMUNE: il
 * dialogo e la schermata di scrittura chiamano questa.
 *
 * [tuttiIGruppi] e' un parametro e non una lettura diretta di `AppSettings`
 * apposta: cosi' la funzione e' pura e sta in `CoreTests` come
 * [gruppiDestinazione].
 */
fun extraPredefinitiCrosspost(
    allGroups: List<String>,
    currentGroup: String,
    tuttiIGruppi: Boolean,
): List<String> =
    if (!tuttiIGruppi) emptyList()
    else allGroups.filter { it != currentGroup && it.isNotBlank() }
        .take(CROSSPOST_MAX_GROUPS - 1)

/*
 * ====================================================================
 * 1.4.6 (§A1, piano C) — IL SELETTORE SMETTE DI IGNORARE L'AUTORE
 * ====================================================================
 *
 * IL DIFETTO, Dave Royal, prova 9B del 29 settembre: *«the group selector does
 * not give the option of uk.test only»*. Un messaggio crospostato in `it.test` e
 * `uk.test` con `Followup-To: uk.test` va risposto su `uk.test` E BASTA — ma il
 * selettore era costruito sui `Newsgroups:` dell'ORIGINALE e teneva il gruppo di
 * lettura spuntato e non deselezionabile. La destinazione giusta non era
 * esprimibile.
 *
 * E il difetto gemello (A1, 22 settembre): siccome aprire il selettore impostava
 * `crosspostScelto`, il solo GESTO DI CONTROLLO buttava via il Followup-To. Non
 * era «l'utente ha deciso»: era «l'utente non aveva modo di decidere quello».
 *
 * LA CORREZIONE, e la sua forma conta. Non si tocca il contratto del dialogo —
 * resta «un gruppo obbligatorio piu' gli extra scelti» — si cambia COSA gli si
 * passa. Quando l'autore ha chiesto dei gruppi, il gruppo obbligatorio e' il
 * PRIMO DI QUELLI, e l'elenco offerto e' l'unione fra la sua richiesta e i
 * gruppi dell'originale.
 *
 * Cosi' «uk.test soltanto» si ottiene confermando senza extra, «tutti e due»
 * spuntando `it.test`, e aprire il selettore per guardare non distrugge piu'
 * niente.
 *
 * ⚠️ LA PROPRIETA' CHE RENDE SICURA QUESTA MODIFICA: tutte e due le funzioni qui
 * sotto sono l'IDENTITA' sul caso senza `Followup-To`. Un messaggio normale
 * passa esattamente per i valori di prima, byte per byte. La non-regressione non
 * e' una speranza da collaudare: e' una proprieta' da leggere.
 *
 * ⚠️ COSA QUESTA CORREZIONE NON FA, detto qui perche' non si creda coperto.
 * Non esiste un modo esplicito di IGNORARE la richiesta dell'autore: con un
 * `Followup-To: uk.test` non si puo' rispondere su `it.test` soltanto. Dave
 * aveva proposto un riquadro «Follow-ups requested to…. Accept or ignore?» al
 * tocco su «rispondi»; quel riquadro aggiungerebbe un passaggio al caso che lui
 * stesso ha verificato funzionare, quindi la decisione sul FLUSSO resta sua e
 * non e' stata presa qui. Questa correzione fa le tre cose che servono a
 * qualunque flusso lui scelga, e non ne inventa uno.
 */

/**
 * Il gruppo in cui la risposta esce COMUNQUE: quello che il selettore tiene
 * spuntato e non deselezionabile.
 *
 * Senza `Followup-To` e' il gruppo che si sta leggendo, come e' sempre stato.
 * Con un `Followup-To` a dei gruppi e' il PRIMO di quelli — perche' una risposta
 * deve pur uscire da qualche parte, e il posto che l'autore ha indicato e' una
 * scelta piu' difendibile del gruppo in cui ci si trova per caso.
 *
 * ⚠️ Si legge la destinazione GREZZA, non quella «ancora in vigore». E' voluto:
 * dopo una conferma nel selettore la richiesta dell'autore si ritira (vedi
 * [destinazioneAncoraInVigore]), ma l'ancora della scelta che l'utente ha appena
 * fatto resta quella. Leggere il valore ritirato farebbe tornare il gruppo di
 * lettura un istante dopo la conferma — cioe' il difetto A1 da capo.
 */
/** I gruppi che l'autore ha chiesto, oppure vuoto. Un solo punto che scarta l'involucro. */
fun gruppiChiestiDallAutore(destinazione: DestinazioneRisposta): List<String> =
    (destinazione as? DestinazioneRisposta.Gruppi)?.gruppi?.map { it.trim() }?.filter { it.isNotEmpty() }
        .orEmpty()

/**
 * 1.4.6 (par. A1) — LA SELEZIONE DI PARTENZA QUANDO L'AUTORE HA CHIESTO QUALCOSA.
 *
 * ⚠️ IL DIFETTO CHE CHIUDE, trovato da Marco il 1 ottobre 2026 su uno screenshot
 * del dialogo. Con «rispondi sempre a tutti i gruppi» attivo (l'impostazione di
 * Dave, 1.2.7) il selettore partiva con TUTTI gli altri gruppi spuntati. Da
 * quando l'elenco offerto e' l'unione fra la richiesta dell'autore e i gruppi
 * dell'originale, «tutti gli altri» ha cominciato a comprendere anche quelli che
 * l'autore aveva ESCLUSO: su un Followup-To a `uk.test`, il dialogo nasceva con
 * `it.test` gia' spuntato e confermando la risposta usciva su DUE gruppi.
 *
 * Avevo corretto una degradazione del Followup-To e ne avevo aperta un'altra
 * dalla porta accanto.
 *
 * LA REGOLA, e non e' un caso particolare: «tutti i gruppi» e' una preferenza su
 * COME trattare un crosspost, e un `Followup-To` non e' un crosspost — e' una
 * destinazione nominata. Una preferenza generica non raffina una richiesta
 * esplicita: la contraddice. Quando l'autore ha nominato dei gruppi, la
 * selezione di partenza e' la SUA, e l'impostazione resta per i crosspost veri.
 *
 * Identita' sul caso di tutti i giorni: senza `Followup-To` chiama esattamente
 * [extraPredefinitiCrosspost] come prima.
 */
fun extraPredefinitiRisposta(
    allGroups: List<String>,
    currentGroup: String,
    gruppiAutore: List<String>,
    tuttiIGruppi: Boolean,
): List<String> =
    if (gruppiAutore.isNotEmpty()) gruppiAutore.filter { it != currentGroup && it.isNotBlank() }
    else extraPredefinitiCrosspost(allGroups, currentGroup, tuttiIGruppi)

fun gruppoObbligatorio(gruppoDiLettura: String, destinazioneAutore: DestinazioneRisposta): String =
    gruppiChiestiDallAutore(destinazioneAutore).firstOrNull() ?: gruppoDiLettura

/**
 * I gruppi che il selettore mette in elenco: la richiesta dell'autore PRIMA,
 * poi quelli dell'originale, senza ripetizioni.
 *
 * L'ordine non e' estetico: il primo e' quello obbligatorio, e deve essere lo
 * stesso che restituisce [gruppoObbligatorio].
 *
 * Qui cade anche il limite «solo gruppi sottoscritti». Dave lo accetta per
 * SCRIVERE un `Followup-To` (li' i gruppi li sceglie lui e puo' sottoscriverli),
 * non per RISPETTARNE uno: *«theoretically none of the groups being replied to
 * will be subscribed to»* — li ha scelti qualcun altro.
 */
fun gruppiOffertiNelSelettore(
    destinazioneAutore: DestinazioneRisposta,
    gruppiOriginale: List<String>,
): List<String> {
    val out = LinkedHashSet<String>()
    out.addAll(gruppiChiestiDallAutore(destinazioneAutore))
    for (g in gruppiOriginale) if (g.isNotBlank()) out.add(g.trim())
    return out.toList()
}

/**
 * Dialog che avvisa del crosspost e lascia scegliere i gruppi di risposta.
 * [currentGroup] è pre-selezionato e NON deselezionabile (la risposta esce
 * sempre almeno lì). Alla conferma richiama [onConfirm] con i gruppi EXTRA
 * scelti (escluso quello obbligatorio), già limitati al cap.
 *
 * ⚠️ 1.4.6 (§A1): [currentGroup] NON è più per forza il gruppo che si sta
 * leggendo, e [allGroups] non è più per forza la riga `Newsgroups:`
 * dell'originale. Con un `Followup-To` sono [gruppoObbligatorio] e
 * [gruppiOffertiNelSelettore], cioè la richiesta dell'autore. *Il dialogo non se
 * ne accorge e non deve: il suo contratto è «un gruppo fisso più degli extra», e
 * quale sia il gruppo fisso è una decisione di chi lo apre.* È il motivo per cui
 * questa correzione non ha toccato nessuno dei quattro punti di chiamata più del
 * necessario.
 */
@Composable
fun CrosspostReplyDialog(
    currentGroup: String,
    allGroups: List<String>,
    /**
     * 1.4 (Dave Royal) — la scelta GIÀ fatta in questa composizione, oppure
     * `null` se non è ancora stata fatta. Vedi la nota sotto: la differenza fra
     * «non ho ancora scelto» e «ho scelto di non aggiungere niente» non si può
     * rappresentare con una lista vuota, e confonderle è il difetto.
     */
    giaScelti: List<String>? = null,
    /**
     * 1.4.6 (par. A1): i gruppi che l'AUTORE ha chiesto, vuoto se non ne ha
     * chiesti. Vuoto = il dialogo si comporta e si legge esattamente come prima,
     * parola per parola. Non vuoto cambia quattro cose, e tutte perche'
     * altrimenti direbbe il falso: la selezione di partenza (quella dell'autore,
     * non «tutti i gruppi»), il titolo, e le due frasi che parlano di «questo
     * gruppo» — che, con un Followup-To, questo gruppo non e'.
     */
    gruppiAutore: List<String> = emptyList(),
    onConfirm: (extraGroups: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val altri = remember(allGroups, currentGroup) { allGroups.filter { it != currentGroup } }
    /*
     * 1.2.7 (Dave #8): con "rispondi sempre a tutti i gruppi" attivo, la
     * selezione parte già con gli altri gruppi spuntati (fino al cap: corrente +
     * extra). Default (flag off): parte solo col gruppo corrente.
     *
     * 1.4 (Dave Royal, 17 settembre 2026) — CONTROLLARE NON DEVE MODIFICARE.
     *
     * Fino alla 1.3.7 questa inizializzazione guardava SOLO l'impostazione, e
     * `extraGroups` non veniva nemmeno passato qui dentro. Conseguenza: chi
     * apriva il dialogo per VERIFICARE di aver tolto due gruppi se li ritrovava
     * rimessi, perché la selezione ripartiva dal default. Le sue parole:
     *   «if I tap the x-post icon to check or change what groups I specified it
     *    reverts to the default - 'all' in my case. It should show what I
     *    actually specified IMO.»
     * Ha ragione, e il caso peggiore è proprio quello innocente: il gesto di
     * controllo distrugge ciò che controlla.
     *
     * Perché `null` e non una lista vuota: «ho scelto di non aggiungere nessun
     * gruppo» e «non ho ancora scelto» sono stati DIVERSI, e con
     * `rispostaCrosspostTuttiIGruppi` attivo portano a due schermate opposte.
     * Una lista vuota li confonderebbe, e il difetto tornerebbe identico sul
     * caso «ho tolto tutti gli extra».
     */
    var selezionati by remember {
        mutableStateOf(
            when {
                giaScelti != null -> giaScelti.toSet()
                // 1.4.5 (§A10): la regola sta in extraPredefinitiCrosspost, qui
                // sotto. Era scritta qui dentro, e il percorso a PAGINE — che
                // questo dialogo non lo apre — non la vedeva affatto.
                else -> extraPredefinitiRisposta(
                    allGroups = allGroups,
                    currentGroup = currentGroup,
                    gruppiAutore = gruppiAutore,
                    tuttiIGruppi = AppSettings.rispostaCrosspostTuttiIGruppi,
                ).toSet()
            },
        )
    }
    // gruppi totali selezionati = corrente (fisso) + extra scelti
    val totale = 1 + selezionati.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (gruppiAutore.isEmpty()) stringResource(R.string.xp_title)
                // «Messaggio crosspostato» sarebbe falso: il 9A sta su UN gruppo.
                else stringResource(R.string.xp_title_followup),
            )
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    if (gruppiAutore.isEmpty())
                        stringResource(R.string.xp_note, allGroups.size, CROSSPOST_MAX_GROUPS)
                    // allGroups e' l'UNIONE, non i gruppi dell'originale: dire
                    // «pubblicato in N gruppi» conterebbe una cosa per un'altra.
                    else stringResource(
                        R.string.xp_note_followup,
                        gruppiAutore.joinToString(", "), CROSSPOST_MAX_GROUPS,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                // gruppo obbligatorio: sempre attivo, non deselezionabile
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = true, enabled = false, onCheckedChange = null)
                    Text(
                        if (gruppiAutore.isEmpty()) stringResource(R.string.xp_this_group, currentGroup)
                        // Con un Followup-To il gruppo fisso NON e' quello che si
                        // sta leggendo, ed e' il punto di tutta la correzione.
                        else stringResource(R.string.xp_group_autore, currentGroup),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
                // altri gruppi dell'originale: spuntabili fino al cap
                altri.forEach { g ->
                    val checked = g in selezionati
                    // spuntabile se già scelto (per togliere) o se c'è ancora posto
                    val abilitato = checked || totale < CROSSPOST_MAX_GROUPS
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                enabled = abilitato,
                                onValueChange = { on ->
                                    selezionati = if (on) selezionati + g else selezionati - g
                                },
                            ),
                    ) {
                        Checkbox(checked = checked, enabled = abilitato, onCheckedChange = null)
                        Text(
                            g,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = if (abilitato) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                }
                if (totale >= CROSSPOST_MAX_GROUPS) {
                    Text(
                        stringResource(R.string.xp_max_reached, CROSSPOST_MAX_GROUPS),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selezionati.toList()) }) {
                Text(
                    when {
                        selezionati.isNotEmpty() -> stringResource(R.string.xp_reply_n, totale)
                        // «Rispondi solo qui» direbbe il falso: «qui» e' il gruppo
                        // che si legge, e la risposta esce dove ha chiesto l'autore.
                        gruppiAutore.isNotEmpty() ->
                            stringResource(R.string.xp_reply_only_in, currentGroup)
                        else -> stringResource(R.string.xp_reply_here)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

/**
 * beta18 (Marco): selettore gruppi extra per un NUOVO thread in crosspost.
 * A differenza di CrosspostReplyDialog (che mostra i gruppi dell'originale),
 * qui l'utente sceglie liberamente tra i propri gruppi sottoscritti.
 * Cap: max 2 gruppi extra + quello corrente = 3 totali (stesso cap del crosspost
 * in risposta). "Per evitare il multipost nei due newsgroup" — Bingo3331.
 *
 * [currentGroup] è il gruppo di partenza (già incluso, non toglibile).
 * [subscribedGroups] = nomi dei gruppi sottoscritti (escluso il corrente).
 * [onConfirm] riceve i gruppi EXTRA scelti (escluso il corrente).
 */
@Composable
fun CrosspostNuovoThreadDialog(
    currentGroup: String,
    subscribedGroups: List<String>,
    /** 1.4: come in [CrosspostReplyDialog] — la scelta già fatta, o `null`. */
    giaScelti: List<String>? = null,
    onConfirm: (extraGroups: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val altri = remember(subscribedGroups, currentGroup) {
        subscribedGroups.filter { it != currentGroup }.sorted()
    }
    // 1.4: stesso difetto del dialogo di risposta, stessa correzione. Qui non
    // c'e' un default da impostazione, ma riaprire il selettore azzerava
    // comunque i gruppi gia' scelti per il nuovo messaggio.
    var selezionati by remember { mutableStateOf(giaScelti?.toSet() ?: setOf()) }
    val totale = 1 + selezionati.size
    // campo di ricerca per trovare gruppi velocemente su liste lunghe
    var query by remember { mutableStateOf("") }
    val filtrati = remember(altri, query) {
        if (query.isBlank()) altri
        else altri.filter { it.contains(query.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.xp_new_title)) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
            ) {
                Text(
                    stringResource(R.string.xp_new_note, CROSSPOST_MAX_GROUPS - 1),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                // gruppo corrente: fisso, non deselezionabile
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = true, enabled = false, onCheckedChange = null)
                    Text(
                        stringResource(R.string.xp_this_group, currentGroup),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
                if (altri.isNotEmpty()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.xp_search_group)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        filtrati.forEach { g ->
                            val checked = g in selezionati
                            val abilitato = checked || totale < CROSSPOST_MAX_GROUPS
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = checked,
                                        enabled = abilitato,
                                        onValueChange = { on ->
                                            selezionati = if (on) selezionati + g else selezionati - g
                                        },
                                    ),
                            ) {
                                Checkbox(checked = checked, enabled = abilitato, onCheckedChange = null)
                                Text(
                                    g,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = if (abilitato) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                        if (totale >= CROSSPOST_MAX_GROUPS) {
                            Text(
                                stringResource(R.string.xp_max_reached, CROSSPOST_MAX_GROUPS),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selezionati.toList()) }) {
                Text(if (selezionati.isEmpty()) stringResource(R.string.xp_only_here) else stringResource(R.string.xp_send_n, totale))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

/**
 * 1.4 — L'AVVISO DEL `Followup-To`, condiviso fra la vista a pagine e quella a
 * pannelli.
 *
 * PERCHE' ESISTE, e perche' NON e' silenzioso.
 * Rispettare il `Followup-To` significa far uscire la risposta in un gruppo
 * DIVERSO da quello che l'utente sta guardando. Farlo senza dirlo sarebbe
 * sostituire una scortesia (ignorare l'header) con una sorpresa (pubblicare
 * dove l'utente non si aspetta). L'avviso e' la meta' della funzione.
 *
 * Condivisa dal primo giorno, non alla terza copia: i punti di uso sono gia'
 * due, ed e' la soglia oltre la quale questo progetto estrae.
 */
@Composable
fun AvvisoFollowupTo(
    destinazione: DestinazioneRisposta,
    gruppoCorrente: String,
    /** Indirizzo dell'autore, per il caso `poster`. null se non riconoscibile. */
    indirizzoAutore: String?,
    onCopiaIndirizzo: (String) -> Unit,
    onApriPosta: (String) -> Unit,
) {
    when (destinazione) {
        is DestinazioneRisposta.Nessuna -> Unit

        is DestinazioneRisposta.Gruppi -> Text(
            stringResource(
                R.string.cmp_followup_gruppi,
                destinazione.gruppi.joinToString(", "),
                gruppoCorrente,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        is DestinazioneRisposta.SoloAutore -> Column(Modifier.padding(bottom = 4.dp)) {
            Text(
                stringResource(R.string.cmp_followup_poster),
                style = MaterialTheme.typography.bodySmall,
                // error, non primary: qui NON stiamo facendo quello che
                // l'autore ha chiesto, e il colore deve dirlo.
                color = MaterialTheme.colorScheme.error,
            )
            if (indirizzoAutore != null) {
                Text(
                    indirizzoAutore,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row {
                    TextButton(onClick = { onCopiaIndirizzo(indirizzoAutore) }) {
                        Text(stringResource(R.string.common_copy))
                    }
                    TextButton(onClick = { onApriPosta(indirizzoAutore) }) {
                        Text(stringResource(R.string.cmp_followup_apri_posta))
                    }
                }
            }
        }
    }
}

/**
 * Apre l'app di posta del telefono su [indirizzo], per il caso
 * `Followup-To: poster`.
 *
 * `ACTION_SENDTO` con schema `mailto:` — nessun SMTP dentro NeReO, che resta un
 * lettore Usenet: si passa la richiesta a chi la posta la sa mandare. Se sul
 * dispositivo non c'e' nessuna app di posta non succede niente di rumoroso:
 * l'indirizzo resta scritto sopra e copiabile, che e' il ripiego giusto.
 */
fun apriAppDiPosta(ctx: android.content.Context, indirizzo: String) {
    runCatching {
        ctx.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_SENDTO,
                android.net.Uri.parse("mailto:" + indirizzo),
            ),
        )
    }
}
