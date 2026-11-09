/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.openusenet.reader.data.AppDatabase
import org.openusenet.reader.data.IdentityMigrator
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.nntp.NntpLog
import org.openusenet.reader.security.KeystoreCipher
import org.openusenet.reader.ui.LocaleHelper

class App : Application() {
    val repository: NewsRepository by lazy { NewsRepository(AppDatabase.get(this), KeystoreCipher()) }

    /**
     * Scope legato al ciclo di vita dell'APPLICAZIONE (non di una schermata):
     * per lavori "spara e dimentica" che devono sopravvivere alla navigazione,
     * come il refresh automatico del gruppo dopo aver inviato un post (se
     * usassimo lo scope della schermata, chiudendola il refresh verrebbe
     * annullato a metà). SupervisorJob: un errore non abbatte gli altri lavori.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Su Android <= 12 applica la lingua scelta in-app anche al context
    // dell'Application (stringhe cercate fuori dalle schermate). Su 13+ e' un
    // no-op: al locale pensa il framework. Vedi LocaleHelper.
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applica(base))
    }

    override fun onCreate() {
        super.onCreate()
        // 1.2.9.b (Piano C.1, voce X di NeReO_bug_1.2.9.md): il PRIMO
        // passo, prima di qualunque altra inizializzazione — così anche un
        // crash durante l'avvio stesso finisce nel log.
        CrashLog.install(this)
        AppSettings.init(this)
        migrateUserAgent()
        // Diagnostica NNTP (punto 3, HAL/Bingo3331): la versione va impostata
        // qui, non dentro NntpLog/NntpClient, che restano volutamente senza
        // dipendenze Android per essere testabili da riga di comando.
        NntpLog.appVersion = BuildConfig.VERSION_NAME
        // 1.6.2 / 189 (HAL): ultimo errore operativo, stesso schema di NntpLog —
        // il lato Android lo imposta qui, l'oggetto resta provabile nei test.
        UltimoErrore.file = java.io.File(filesDir, "last_error.txt")
        UltimoErrore.intestazione =
            "Versione: ${BuildConfig.VERSION_NAME} (versionCode ${BuildConfig.VERSION_CODE})\n" +
            "Dispositivo: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, " +
            "Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})"
        // 1.2.9 (multi-identità): al primo avvio dopo l'update, migra le
        // identità legacy (SharedPreferences "identity" → tabella `identities`)
        // e SOLO DOPO ricarica myEmail/myEmails dal DB — l'ordine è
        // fondamentale: se reloadIdentityEmails girasse prima della
        // migrazione, al primo avvio non troverebbe ancora nessuna riga e
        // "i miei post" smetterebbe di essere riconosciuto finché non si
        // salva manualmente un'identità.
        appScope.launch {
            IdentityMigrator.migrateFromPrefsIfNeeded(repository, this@App)
            migraUserAgentIdentita()
            AppSettings.reloadIdentityEmails(repository)
            riparaUsernameFintiUnaVolta()
        }
    }

    /**
     * 1.4 — ripara UNA VOLTA SOLA i nomi utente finti lasciati dai ripristini
     * di backup fatti con le versioni fino alla 1.3.7 comprese.
     *
     * PERCHE' ANCHE QUI, visto che il ripristino li ripara gia'.
     * Perche' il ripristino li ripara solo a chi ne fa un altro. Chi ha
     * ripristinato una volta, si e' ritrovato `username = "null"` su un server
     * anonimo e non ripristinera' mai piu' resterebbe rotto per sempre, senza
     * sapere nemmeno cosa guardare: vedrebbe solo un server che non si collega
     * con un errore di protocollo incomprensibile.
     *
     * UNA VOLTA SOLA, e la bandierina si alza SOLO se la riparazione e'
     * riuscita: se il database non e' ancora pronto o la chiamata fallisce, il
     * prossimo avvio ci riprova invece di dare per fatto qualcosa che non e'
     * stato fatto.
     *
     * Non chiede niente all'utente, e la scelta e' deliberata: qui non si
     * cancella un dato SUO, si cancella un dato che NeReO si era inventato da
     * solo. Le note di versione lo dicono, e la riga finisce nel log
     * diagnostico per chi volesse verificarlo.
     */
    private suspend fun riparaUsernameFintiUnaVolta() {
        val p = getSharedPreferences("manutenzione", MODE_PRIVATE)
        if (p.getBoolean(RIPARAZIONE_USERNAME_FATTA, false)) return
        val n = runCatching { repository.riparaUsernameFinti() }.getOrNull() ?: return
        p.edit().putBoolean(RIPARAZIONE_USERNAME_FATTA, true).apply()
        if (n > 0) {
            NntpLog.diag("riparati $n nomi utente \"null\" lasciati da un ripristino precedente")
        }
    }

    /**
     * 1.3.2 (Dave Royal): allinea alla RFC 5536 §3.2.13 lo User-Agent delle
     * identità GIÀ SALVATE nel database.
     *
     * [migrateUserAgent] qui sotto fa la stessa cosa, ma solo sulle vecchie
     * preferenze "identity" — la strada pre-1.2.9. Dalla 1.2.9 le identità
     * sono righe della tabella `identities`, e quelle nessuno le aveva mai
     * riallineate: chi ha installato NeReO fra la 1.2.13 e la 1.3.1 ha nel
     * campo il modello `%nereo% (Android)`, che si espande con lo SPAZIO.
     *
     * Sostituisce solo le corrispondenze ESATTE con un predefinito che abbiamo
     * spedito noi (vedi [Branding.LEGACY_USER_AGENTS]): un valore scritto
     * dall'utente non compare in quella lista e resta intatto, incluso il
     * campo vuoto, che significa "non dichiarare nulla" ed è una scelta.
     *
     * Una sola volta, con flag proprio: se un domani l'utente rimettesse a
     * mano il vecchio modello, è affar suo e non glielo si tocca più.
     */
    private suspend fun migraUserAgentIdentita() {
        val prefs = getSharedPreferences("identity", Context.MODE_PRIVATE)
        if (prefs.getBoolean(FLAG_UA_RFC5536, false)) return
        var righe = 0
        for (vecchio in Branding.LEGACY_USER_AGENTS) {
            righe += repository.sostituisciUserAgent(vecchio, Branding.USER_AGENT_PREDEFINITO)
        }
        NntpLog.diag("User-Agent RFC 5536: identita' aggiornate = $righe")
        prefs.edit().putBoolean(FLAG_UA_RFC5536, true).apply()
    }

    /**
     * L'app ha cambiato nome (OpenUsenet -> MyNewsReader -> NeReO). Chi aveva
     * gia' salvato l'identita' si ritrova nelle preferenze il vecchio
     * User-Agent, che continuerebbe a comparire nelle intestazioni dei post.
     *
     * Qui lo aggiorno una volta sola, e SOLO se corrisponde esattamente a uno
     * dei valori predefiniti delle versioni precedenti: se il tester si e'
     * scritto uno User-Agent personalizzato, quello resta intoccato.
     */
    private fun migrateUserAgent() {
        val prefs = getSharedPreferences("identity", Context.MODE_PRIVATE)
        val current = prefs.getString("user_agent", null) ?: return
        if (current in Branding.LEGACY_USER_AGENTS) {
            prefs.edit().putString("user_agent", Branding.USER_AGENT_PREDEFINITO).apply()
        }
    }

    private companion object {
        /** Flag della migrazione User-Agent → RFC 5536 (1.3.2), una sola volta. */
        const val FLAG_UA_RFC5536 = "user_agent_rfc5536_v1"

        /** 1.4: riparazione degli username "null" da ripristino, una sola volta. */
        const val RIPARAZIONE_USERNAME_FATTA = "username_null_riparati_v1"
    }
}
