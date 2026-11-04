/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.data

/**
 * 1.6 (Dave Royal, sondaggio sui gruppi) — QUALE IDENTITA' USARE, in un posto
 * solo: quella del gruppo, poi quella del server, poi la predefinita.
 *
 * Un'assegnazione che non porta a niente (identita' cancellata) ricade sul
 * livello sotto, come ha sempre fatto il server: le chiavi sono «morbide».
 *
 * `inline` perche' [trova] e [predefinita] leggono il database: le lambda
 * possono sospendere, e la regola resta pura e provabile in CoreTests.
 */
inline fun <T : Any> primaIdentita(
    idGruppo: Long?,
    idServer: Long?,
    trova: (Long) -> T?,
    predefinita: () -> T?,
): T? = idGruppo?.let { trova(it) } ?: idServer?.let { trova(it) } ?: predefinita()
