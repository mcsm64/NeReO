/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.ui

/**
 * 1.5.7 / 175 (brainstorming del 17/10, voce F8) — SFOGLIARE COI TASTI DEL VOLUME.
 *
 * Nella lettura a schermo intero: volume GIU' = messaggio successivo, volume
 * SU = precedente. Come in molti lettori di e-book e di news: si legge con una
 * mano sola, senza allungare il pollice fino alla barra in basso.
 *
 * SPENTO DI DEFAULT (Aspetto → lettura): chi non lo attiva non vede nessuna
 * differenza, e i tasti del volume fanno il loro lavoro. Anche attivo, i
 * tasti vengono «presi» SOLO mentre e' aperto un messaggio a schermo intero:
 * nell'elenco, nella scrittura e nelle altre schermate regolano il volume
 * come sempre.
 *
 * COME. MainActivity.dispatchKeyEvent chiede a [gestisci] se il tasto e' suo;
 * la schermata di lettura si registra in [gestore] finche' e' visibile
 * (DisposableEffect) e si toglie quando esce. Un posto solo, nessuno stato
 * condiviso oltre a questo.
 *
 * La pressione prolungata (ripetizione automatica) NON sfoglia: un tasto
 * tenuto giu' per sbaglio farebbe correre per tutta la discussione.
 */
object TastiVolume {

    enum class Verso { AVANTI, INDIETRO }

    /** La schermata di lettura attiva, o null. */
    @Volatile
    var gestore: ((Verso) -> Unit)? = null

    /** Codici Android: KeyEvent.KEYCODE_VOLUME_UP = 24, KEYCODE_VOLUME_DOWN = 25. */
    private const val VOLUME_SU = 24
    private const val VOLUME_GIU = 25
    /** KeyEvent.ACTION_DOWN = 0. */
    private const val PREMUTO = 0

    /** Il verso per un tasto, o null se il tasto non e' del volume. Pura: provata a banco. */
    fun verso(codice: Int): Verso? = when (codice) {
        VOLUME_GIU -> Verso.AVANTI
        VOLUME_SU -> Verso.INDIETRO
        else -> null
    }

    /**
     * true = il tasto e' stato consumato (non cambia il volume).
     * Si consumano sia la pressione sia il rilascio, altrimenti il rilascio
     * arriverebbe al sistema da solo; la ripetizione si consuma senza sfogliare.
     */
    fun gestisci(attivo: Boolean, codice: Int, azione: Int, ripetizione: Int): Boolean {
        if (!attivo) return false
        val v = verso(codice) ?: return false
        val g = gestore ?: return false
        if (azione == PREMUTO && ripetizione == 0) g(v)
        return true
    }
}
