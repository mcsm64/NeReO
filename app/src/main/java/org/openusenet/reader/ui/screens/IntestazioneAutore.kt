/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.openusenet.reader.R
import org.openusenet.reader.ui.COLORI_AUTORE
import org.openusenet.reader.ui.XFACE_LATO
import org.openusenet.reader.ui.XFACE_PIXEL
import org.openusenet.reader.ui.decodificaXFace
import org.openusenet.reader.ui.estraiXFace
import org.openusenet.reader.ui.TipoData
import org.openusenet.reader.ui.chiaveColoreAutore
import org.openusenet.reader.ui.indiceColoreAutore
import org.openusenet.reader.ui.inizialiAutore
import org.openusenet.reader.ui.tipoData
import java.time.LocalDate
import org.openusenet.reader.AppSettings
import org.openusenet.reader.text.DataOra

/**
 * 1.5.5 (G1 + G3) — L'INTESTAZIONE DELL'AUTORE, UN POSTO SOLO.
 *
 * Prima l'autore si mostrava in TRE punti scritti a parte: la lettura a
 * schermo intero (con la Face), il messaggio singolo nei pannelli (senza Face)
 * e la vista continua (una riga). Tre copie della stessa cosa, e la Face c'era
 * in una sola. Ora le tre viste chiamano questo file: Face o iniziali
 * colorate, nome, indirizzo, data «oggi / ieri / giorno».
 *
 * La decodifica della Face e' SPOSTATA qui da ArticleScreen, con le sue difese
 * (vedi [decodificaFace]): non ricopiata.
 */

/**
 * Lato massimo accettato per una Face. Una Face e' un PNG 48x48: 512 lascia
 * margine a chi ne pubblica una piu' grande, e ferma la «bomba di
 * decompressione» (vedi sotto). Era in ArticleScreen dalla 1.2.12.
 */
private const val MAX_FACE_LATO = 512

/**
 * La Face (base64 di un PNG) come Bitmap, o `null`.
 *
 * Decodifica robusta: in invio usiamo NO_WRAP, ma le Face di altri client
 * possono avere padding o spazi diversi; si ripulisce e si prova NO_WRAP, poi
 * DEFAULT.
 *
 * 1.2.12 (audit di sicurezza): prima si leggono SOLO le dimensioni
 * (inJustDecodeBounds non alloca nulla), si rifiuta cio' che e' fuori scala, e
 * la decodifica vera sta dentro un runCatching. Senza, un PNG di pochi
 * kilobyte che si espande in decine di megabyte faceva morire l'app con un
 * OutOfMemoryError aprendo l'articolo.
 */
fun decodificaFace(raw: String?): Bitmap? {
    if (raw.isNullOrBlank()) return null
    val clean = raw.filter { !it.isWhitespace() }
    val bytes = runCatching {
        android.util.Base64.decode(clean, android.util.Base64.NO_WRAP)
    }.recoverCatching {
        android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
    }.getOrNull() ?: return null
    return runCatching {
        val opzioni = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opzioni)
        val l = opzioni.outWidth
        val h = opzioni.outHeight
        if (l !in 1..MAX_FACE_LATO || h !in 1..MAX_FACE_LATO) null
        else android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

/**
 * 1.5.5 / 172: la X-Face (48x48, bianco e nero) come Bitmap: nero su bianco,
 * opaca, cosi' si legge anche sul tema scuro. `null` se l'intestazione manca
 * o non contiene niente di decodificabile.
 */
fun bitmapXFace(headersRaw: String?): Bitmap? {
    val testo = estraiXFace(headersRaw) ?: return null
    val punti = runCatching { decodificaXFace(testo) }.getOrNull() ?: return null
    val colori = IntArray(XFACE_PIXEL) { if (punti[it] != 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    return runCatching {
        Bitmap.createBitmap(colori, XFACE_LATO, XFACE_LATO, Bitmap.Config.ARGB_8888)
    }.getOrNull()
}

/**
 * Accanto all'autore, in quest'ordine: la Face (PNG a colori), la X-Face
 * (bianco e nero, 1.5.5/172) e solo se non c'e' nessuna delle due le iniziali su
 * un cerchio colorato (G1). Face e X-Face si ingrandiscono al tocco (wishlist
 * beta12 di Bingo3331). La X-Face si disegna senza sfumare i punti
 * (FilterQuality.None): ingrandita resta nitida, a quadretti, com'e' nata.
 */
@Composable
fun AvatarAutore(fromField: String, faceB64: String?, headersRaw: String?, lato: Dp) {
    val face = remember(faceB64) { decodificaFace(faceB64) }
    val xface = remember(faceB64, headersRaw) { if (face == null) bitmapXFace(headersRaw) else null }
    val immagine = face ?: xface
    if (immagine != null) {
        val nitida = if (face == null) FilterQuality.None else FilterQuality.Low
        val descrizione = stringResource(if (face != null) R.string.art_face_cd else R.string.art_xface_cd)
        var ingrandita by remember(faceB64, headersRaw) { mutableStateOf(false) }
        Image(
            immagine.asImageBitmap(),
            contentDescription = descrizione,
            filterQuality = nitida,
            modifier = Modifier
                .size(lato)
                .clickable { ingrandita = true },
        )
        if (ingrandita) {
            Dialog(onDismissRequest = { ingrandita = false }) {
                Image(
                    immagine.asImageBitmap(),
                    contentDescription = stringResource(R.string.art_face_big_cd),
                    filterQuality = nitida,
                    modifier = Modifier
                        .size(288.dp)
                        .clickable { ingrandita = false },
                )
            }
        }
        return
    }
    val iniziali = remember(fromField) { inizialiAutore(authorDisplayName(fromField)) }
    val colore = remember(fromField) {
        Color(COLORI_AUTORE[indiceColoreAutore(chiaveColoreAutore(fromField), COLORI_AUTORE.size)])
    }
    val dimensioneTesto = with(LocalDensity.current) { (lato * 0.4f).toSp() }
    Box(
        Modifier
            .size(lato)
            .clip(CircleShape)
            .background(colore),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            iniziali,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = dimensioneTesto,
            maxLines = 1,
        )
    }
}

/**
 * La data dell'intestazione (G3): «oggi 14:30», «ieri 23:50», «lunedì 09:12»
 * nell'ultima settimana, la data piena altrimenti. Al tocco si alterna con la
 * data piena, che resta sempre a un tocco di distanza.
 */
@Composable
fun testoDataMessaggio(dateEpoch: Long, piena: Boolean): String {
    // 1.6.2 / 189: forme e fuso da text/DataOra (Aspetto → Data e ora). Qui
    // l'ora porta « UTC» quando si mostra in UTC: non e' una colonna stretta, e
    // un'ora nuda in UTC sarebbe un equivoco. «Oggi» nello stesso fuso.
    val p = AppSettings.preferenzeDataOra()
    val quando = DataOra.inZona(dateEpoch, p)
    val utc = DataOra.suffissoUtc(p)
    val ora = DataOra.ora(quando, p) + utc
    val completa = DataOra.dataOraCompleta(quando, p)
    if (piena) return completa
    val locale = LocalConfiguration.current.locales[0]
    return when (tipoData(quando.toLocalDate().toEpochDay(), LocalDate.now(quando.zone).toEpochDay())) {
        TipoData.OGGI -> stringResource(R.string.art_date_today, ora)
        TipoData.IERI -> stringResource(R.string.art_date_yesterday, ora)
        TipoData.SETTIMANA -> DataOra.giornoSettimanaOra(quando, p, locale) + utc
        TipoData.DATA -> completa
    }
}

/**
 * L'intestazione intera (G3), per la lettura a schermo intero e il messaggio
 * singolo nei pannelli: avatar, nome in evidenza, indirizzo, data.
 */
@Composable
fun IntestazioneAutore(
    fromField: String,
    dateEpoch: Long,
    faceB64: String?,
    headersRaw: String?,
    readFg: Color,
    modifier: Modifier = Modifier,
) {
    var dataPiena by remember(dateEpoch) { mutableStateOf(false) }
    val nome = remember(fromField) { authorDisplayName(fromField) }
    val indirizzo = remember(fromField) {
        fromField.substringAfter('<', "").substringBefore('>', "").trim()
            .takeIf { it.isNotEmpty() && it != nome }
    }
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AvatarAutore(fromField, faceB64, headersRaw, 44.dp)
        Column(Modifier.weight(1f)) {
            Text(
                nome,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = readFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            indirizzo?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = readFg.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                testoDataMessaggio(dateEpoch, dataPiena),
                style = MaterialTheme.typography.bodySmall,
                color = readFg.copy(alpha = 0.7f),
                modifier = Modifier.clickable { dataPiena = !dataPiena },
            )
        }
    }
}

/**
 * La versione su una riga, per la vista continua (G3): avatar piccolo,
 * «nome · data». La riga e' quella che [IntestazioneConBarra] puo' mettere
 * accanto alla barra: [modifier] arriva da li' e va rispettato.
 */
@Composable
fun IntestazioneAutoreCompatta(
    fromField: String,
    dateEpoch: Long,
    faceB64: String?,
    headersRaw: String?,
    readFg: Color,
    modifier: Modifier = Modifier,
) {
    var dataPiena by remember(dateEpoch) { mutableStateOf(false) }
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AvatarAutore(fromField, faceB64, headersRaw, 28.dp)
        Text(
            "${authorDisplayName(fromField)}  ·  " + testoDataMessaggio(dateEpoch, dataPiena),
            style = MaterialTheme.typography.bodySmall,
            color = readFg.copy(alpha = 0.7f),
            modifier = Modifier
                .weight(1f)
                .clickable { dataPiena = !dataPiena },
        )
    }
}
