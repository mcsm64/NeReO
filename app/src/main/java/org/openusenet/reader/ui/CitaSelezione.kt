/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.openusenet.reader.R
import org.openusenet.reader.data.Article
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.ui.screens.introLine

/*
 * 1.5 / 158 — PASSO 3 DEL PACCHETTO CITAZIONI, PIANO C, PRIMA META':
 * «SELEZIONO E POI CITA», come l'aveva chiesto Ammammata il 14 settembre.
 *
 * PERCHE' ADESSO SI PUO' E A SETTEMBRE NO. Il 10 settembre la nuova API del
 * menu contestuale permetteva di AGGIUNGERE una voce, ma non diceva quale testo
 * fosse selezionato: una voce «Cita» che non sa cosa citare non serve. Compose
 * 1.12 ha `SelectionState`, che lo espone (`selectedTexts`). Verificato il 12
 * ottobre sui METADATI KOTLIN dell'APK 157 — non sulla documentazione, che si
 * contraddice: le note di rilascio lo danno nella 1.13, il blog di agosto nella
 * 1.12. Il binario ha dato ragione al blog: classe, `selectedTexts`,
 * `rememberSelectionState()` e `SelectionContainer(state = ...)` sono PUBLIC.
 *
 * ⚠️ PERCHE' UNA VOCE DEL MENU E NON UN PULSANTE NELLA BARRA. Nella 1.12.0
 * manca `Modifier.disableSelectionClearOnTap()` (verificato: non c'e' nel dex).
 * Senza, toccare un pulsante FUORI dal testo cancella la selezione prima che il
 * clic arrivi, e il pulsante troverebbe una selezione vuota. La voce del menu
 * contestuale compare sopra la selezione e non la tocca.
 *
 * ⚠️ UN SOLO PUNTO, DI PROPOSITO. Il corpo del messaggio e' selezionabile in
 * TRE posti (lettura a schermo intero e due varianti della vista a pannelli).
 * Questo file e' l'unico che sa come si cita una selezione; i tre posti lo
 * chiamano. La 158 lo collega alla sola lettura a schermo intero — una
 * schermata per volta, compilando in mezzo.
 *
 * Il RIPIEGO col telecomando (Roxy, TV box: trascinare maniglie non si fa) e'
 * la schermata a righe del piano B, e arriva in una build successiva.
 */

/** Chiave della voce nel menu contestuale: basta che sia unica e stabile. */
private object ChiaveCita

/** 1.5.4: chiave della voce «Cita e rispondi». */
private object ChiaveCitaERispondi

/**
 * Avvolge il corpo di un messaggio in un `SelectionContainer` che, oltre alle
 * voci di sistema (Copia, Seleziona tutto...), offre **«Cita»**.
 *
 * «Cita» mette negli appunti una citazione completa: l'introduzione
 * dell'identita' in uso (la stessa di Rispondi, comprese quella per gruppo e i
 * segnaposto), le righe ORIGINALI coperte dalla selezione coi loro `>`, e in
 * coda Message-ID e link permanente. Il testo si costruisce con le funzioni
 * pure di `Citazione.kt`: qui non c'e' una riga di formattazione.
 *
 * [corpo] e' il testo del messaggio COSI' COME E' IN ARCHIVIO. [riconverti]
 * porta la selezione nella stessa forma: serve al ROT13, dove a schermo c'e' il
 * testo decodificato e in archivio quello codificato (il ROT13 e' un'involuzione,
 * quindi la stessa funzione va e torna).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CorpoCitabile(
    repo: NewsRepository,
    articolo: Article?,
    corpo: String?,
    riconverti: (String) -> String = { it },
    /**
     * 1.5.4: come si apre la RISPOSTA a [articolo] in questa vista (pagina
     * intera o pannello, con i suoi controlli sul crosspost). null = la voce
     * «Cita e rispondi» non compare.
     */
    onCitaERispondi: (() -> Unit)? = null,
    contenuto: @Composable () -> Unit,
) {
    val stato = rememberSelectionState()
    val scope = rememberCoroutineScope()
    val appunti = rememberAppunti()
    val ctx = LocalContext.current
    val etichetta = stringResource(R.string.sel_cita)
    val avviso = stringResource(R.string.sel_cita_fatto)
    val introPredefinita = stringResource(R.string.cmp_default_intro)
    val etichettaRispondi = stringResource(R.string.sel_cita_rispondi)
    val rispondiAttuale by rememberUpdatedState(onCitaERispondi)
    // il menu si costruisce quando compare, non quando si compone: deve vedere
    // l'articolo di ADESSO, anche se nel frattempo ci si e' spostati nel thread.
    val articoloAttuale by rememberUpdatedState(articolo)
    val corpoAttuale by rememberUpdatedState(corpo)
    val riconvertiAttuale by rememberUpdatedState(riconverti)

    /*
     * Il testo della citazione, IN UN PUNTO SOLO per le due voci: «Cita» lo
     * mette negli appunti con la coda Message-ID/link, «Cita e rispondi» lo
     * consegna alla risposta SENZA coda (la risposta ha gia' le References:
     * un link al messaggio a cui si sta rispondendo sarebbe un doppione).
     */
    suspend fun testoCitazione(a: Article, scelta: CitazioneRiradicata, conCoda: Boolean): String {
        val gruppo = repo.groupById(a.groupId)
        val intro = scelta.introduzione ?: introLine(
            repo.identitaPerGruppo(a.groupId), a, gruppo?.name.orEmpty(),
            introPredefinita, gruppo?.replyIntro,
        )
        return formattaCitazione(
            citazioneDa(
                intro, scelta.righe, if (conCoda) a.messageId else null,
                gruppo?.name.orEmpty(), a.subject,
            )
        )
    }

    /*
     * La selezione ATTUALE, ri-radicata sulle righe originali. Va letta PRIMA
     * di chiudere il menu: e' l'unico momento in cui la selezione e' sicuramente
     * ancora quella che l'utente vede. null = niente da citare.
     */
    fun sceltaAttuale(): Pair<Article, CitazioneRiradicata>? {
        val selezionato = stato.selectedTexts.joinToString("\n") { it.text }
        val a = articoloAttuale ?: return null
        // 1.5 / 159 (piano D, collaudo di Marco): se le righe scelte erano GIA'
        // citate nel messaggio, il testo e' di un altro, e l'introduzione
        // diventa la sua attribuzione — «Giangi ha scritto:», non «Roberto ha
        // scritto:». Vedi Citazione.kt.
        val scelta = riradica(corpoAttuale.orEmpty(), riconvertiAttuale(selezionato))
        return if (scelta.righe.isEmpty()) null else a to scelta
    }

    SelectionContainer(
        state = stato,
        modifier = Modifier.appendTextContextMenuComponents {
            item(key = ChiaveCita, label = etichetta) {
                val presa = sceltaAttuale()
                close()
                val (a, scelta) = presa ?: return@item
                scope.launch {
                    appunti.copia(testoCitazione(a, scelta, conCoda = true))
                    stato.clear()
                    // Android 13+ mostra da se' la conferma di copia; sotto no.
                    if (android.os.Build.VERSION.SDK_INT < 33) {
                        android.widget.Toast.makeText(ctx, avviso, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            // 1.5.4: «Cita e rispondi». Stessa selezione, stessa citazione; al
            // posto degli appunti, la risposta si apre gia' pronta.
            if (rispondiAttuale != null) {
                item(key = ChiaveCitaERispondi, label = etichettaRispondi) {
                    val presa = sceltaAttuale()
                    close()
                    val (a, scelta) = presa ?: return@item
                    scope.launch {
                        CitazioneInAttesa.metti(a.id, testoCitazione(a, scelta, conCoda = false))
                        stato.clear()
                        rispondiAttuale?.invoke()
                    }
                }
            }
        },
    ) {
        contenuto()
    }
}
