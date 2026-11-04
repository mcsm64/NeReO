/*
 * "Rifinisci citazione" (Piano A, beta15 — richiesta Bingo3331).
 *
 * Bingo chiedeva una X accanto a OGNI riga quotata, per cancellarla al volo e
 * velocizzare il trimming del quote (netiquette). Un widget-per-riga INLINE
 * dentro il campo di testo non è fattibile in Compose (il corpo è un unico
 * OutlinedTextField, non una lista di righe), e reintrodurrebbe la complessità
 * di scroll/selezione che abbiamo appena tolto in ComposePane (bug Roxy).
 *
 * Soluzione additiva e a basso rischio: un pannello dedicato che mostra le sole
 * righe citate, ognuna con la sua X. L'utente toglie quelle che vuole, conferma,
 * e il corpo viene ricostruito togliendo ESATTAMENTE quelle righe (per indice,
 * non per contenuto: righe citate identiche non si confondono). Il resto del
 * messaggio — intro, testo proprio, firma — non viene toccato. Funziona a
 * pulsanti, quindi comodo anche col controller esterno (Roxy).
 *
 * Condiviso fra ComposeScreen (a pagine) e ComposePane (a pannelli): un solo
 * punto di manutenzione.
 */
package org.openusenet.reader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import org.openusenet.reader.ui.icone.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openusenet.reader.R
import org.openusenet.reader.ui.riformattaCitazioni

/** Riga "citata" = comincia con ">" (eventuali spazi iniziali ammessi). */
fun isQuotedLine(line: String): Boolean = line.trimStart().startsWith(">")

/** Indici (nel testo completo) delle righe citate. */
fun quotedLineIndices(body: String): List<Int> =
    body.lines().mapIndexedNotNull { i, line -> if (isQuotedLine(line)) i else null }

/** Ricostruisce il corpo togliendo le righe agli indici indicati. */
fun removeLinesByIndex(body: String, indices: Set<Int>): String =
    body.lines().filterIndexed { i, _ -> i !in indices }.joinToString("\n")

/**
 * Dialog "Rifinisci citazione". Mostra le sole righe citate del [body]; l'utente
 * ne toglie a piacere con la X. Alla conferma richiama [onResult] col corpo
 * ricostruito (cursore riportato a inizio testo, coerente con buildPrefill).
 * Se non ci sono righe citate, [onResult] non fa nulla di distruttivo.
 */
@Composable
fun TrimQuoteDialog(
    body: String,
    onResult: (TextFieldValue) -> Unit,
    onDismiss: () -> Unit,
) {
    // indici delle righe citate ancora "vive" (non ancora tolte in questa sessione)
    val citate = remember(body) { quotedLineIndices(body) }
    val righe = remember(body) { body.lines() }
    var rimosse by remember(body) { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trim_title)) },
        text = {
            if (citate.isEmpty()) {
                Text(
                    stringResource(R.string.trim_none),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                androidx.compose.foundation.layout.Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        stringResource(R.string.trim_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    // 1.6 (sondaggio sui gruppi): sta QUI perche' questo dialogo e'
                    // gia' condiviso dalle due viste, e cosi' si taglia e si
                    // riformatta nello stesso gesto: vale sulle righe rimaste.
                    TextButton(
                        onClick = {
                            val nuovo = riformattaCitazioni(removeLinesByIndex(body, rimosse))
                            onResult(TextFieldValue(nuovo, androidx.compose.ui.text.TextRange(nuovo.length)))
                        },
                    ) { Text(stringResource(R.string.trim_reflow)) }
                    citate.forEach { idx ->
                        val tolta = idx in rimosse
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                righe[idx],
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (tolta)
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (tolta) {
                                TextButton(onClick = { rimosse = rimosse - idx }) {
                                    Text(stringResource(R.string.common_cancel))
                                }
                            } else {
                                IconButton(onClick = { rimosse = rimosse + idx }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.trim_remove_cd),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = rimosse.isNotEmpty(),
                onClick = {
                    val nuovo = removeLinesByIndex(body, rimosse)
                    onResult(TextFieldValue(nuovo, androidx.compose.ui.text.TextRange(nuovo.length)))
                },
            ) { Text(stringResource(R.string.trim_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
