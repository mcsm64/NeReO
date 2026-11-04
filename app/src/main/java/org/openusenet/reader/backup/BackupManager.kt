/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Backup/ripristino della configurazione in JSON (org.json). Salva: server
 * (password opzionale), SOTTOSCRIZIONI ai gruppi, regole filtro, impostazioni
 * di aspetto e identità/firma.
 *
 * Ripristino robusto: i server non vengono duplicati (match per host:porta) e
 * le sottoscrizioni vengono riagganciate al server giusto tramite quella
 * chiave. Le password sono incluse SOLO se richiesto esplicitamente (il file
 * le conterrebbe in chiaro).
 */
package org.openusenet.reader.backup

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.openusenet.reader.AppSettings
import org.openusenet.reader.data.FilterRuleEntity
import org.openusenet.reader.data.IgnoredId
import org.openusenet.reader.data.Identity
import org.openusenet.reader.data.IdentityMigrator
import org.openusenet.reader.data.LegacyIdentityFields
import org.openusenet.reader.data.LegacyServerOverride
import org.openusenet.reader.data.NewsRepository
import org.openusenet.reader.data.Server

/**
 * [usernameRiparati] (1.4): righe server il cui nome utente valeva la PAROLA
 * "null" e che sono state rimesse a posto. Vale 0 in un'installazione sana:
 * si mostra all'utente solo quando e' maggiore di zero. Vedi la nota lunga
 * sulla guardia `isNull` piu' sotto in questo file.
 */
data class ImportResult(
    val servers: Int,
    val subscriptions: Int,
    val filters: Int,
    val usernameRiparati: Int = 0,
)

object BackupManager {

    // 1.2.9: v5 aggiunge l'array "identities" (multi-identità vera) e, per ogni
    // server, "identity_nickname"/"startTls"/"authOverrideNonCifrata". "identity"
    // e "server_identities" restano in export, INVARIATI nel formato, solo per
    // compatibilità con NeReO <1.2.9 che aprisse questo stesso backup — la
    // fonte però ora è il DB (righe Identity), non più le SharedPreferences.
    private const val VERSION = 5
    private const val IDENTITY_PREFS = "identity"

    private fun key(host: String, port: Int) = "$host:$port"

    suspend fun export(repo: NewsRepository, ctx: Context, includePasswords: Boolean): String {
        val idp = ctx.getSharedPreferences(IDENTITY_PREFS, Context.MODE_PRIVATE)
        val root = JSONObject()
        root.put("openusenet_backup", VERSION)
        root.put("exported_at", System.currentTimeMillis() / 1000)
        root.put("has_passwords", includePasswords)

        val serverList = repo.exportServers(includePasswords)
        val serversById = serverList.associateBy { it.id }
        // 1.2.9: elenco identità, servono sia per l'array "identities" vero e
        // proprio sia per risolvere identityId → nickname nei server sotto
        // (l'id di riga non ha senso fra installazioni diverse: chi ripristina
        // rimappa per nickname, come già fa per host:porta sui server).
        val identities = repo.identitiesOnce()
        val identityNickById = identities.associate { it.id to it.nickname }

        val servers = JSONArray()
        for (s in serverList) {
            val o = JSONObject()
                .put("name", s.name)
                .put("host", s.host)
                .put("port", s.port)
                .put("ssl", s.ssl)
                .put("username", s.username ?: JSONObject.NULL)
                // 1.2.9 (voce E, roadmap NNTP)
                .put("startTls", s.startTls)
                .put("authOverrideNonCifrata", (s.authOverrideNonCifrata as Any?) ?: JSONObject.NULL)
                // 1.2.9 (multi-identità)
                .put("identity_nickname", s.identityId?.let { identityNickById[it] } ?: JSONObject.NULL)
            if (includePasswords && !s.password.isNullOrEmpty()) o.put("password", s.password)
            servers.put(o)
        }
        root.put("servers", servers)

        val subs = JSONArray()
        for (g in repo.allSubscribedOnce()) {
            val srv = serversById[g.serverId] ?: continue
            // 1.2.6 (Dave Royal): includo l'ordine del gruppo, così il ripristino
            // non rimette tutto in ordine alfabetico perdendo il riordino manuale.
            subs.put(
                JSONObject()
                    .put("server", key(srv.host, srv.port))
                    .put("name", g.name)
                    .put("sortOrder", g.sortOrder)
                    // 1.6 (Dave Royal): per nickname, come sui server: l'id di
                    // riga non ha senso fra installazioni diverse. Campi nuovi
                    // e facoltativi: VERSION resta 5, le versioni vecchie li
                    // ignorano. L'introduzione del gruppo non era mai entrata
                    // nel backup: l'identita' sarebbe tornata e lei no.
                    .put("identity_nickname", g.identityId?.let { identityNickById[it] } ?: JSONObject.NULL)
                    .put("replyIntro", g.replyIntro ?: JSONObject.NULL)
                    // 1.6: anche la conservazione del gruppo. Vale 0 = «mai»,
                    // null = «usa il valore globale»: sono due cose diverse.
                    .put("purgeDays", (g.purgeDays as Any?) ?: JSONObject.NULL)
                    .put("purgeKeepThreads", (g.purgeKeepThreads as Any?) ?: JSONObject.NULL),
            )
        }
        root.put("subscriptions", subs)

        val filters = JSONArray()
        for (f in repo.allFiltersOnce()) {
            filters.put(
                JSONObject()
                    .put("name", f.name)
                    .put("field", f.field)
                    .put("matchType", f.matchType)
                    .put("pattern", f.pattern)
                    .put("score", f.score)
                    .put("caseSensitive", f.caseSensitive)
                    .put("groupWildmat", f.groupWildmat)
                    .put("expiresEpochDay", f.expiresEpochDay ?: JSONObject.NULL)
                    .put("enabled", f.enabled)
                    // M1: senza queste due, un giro di backup/ripristino
                    // declasserebbe in silenzio ogni regola estesa a regola
                    // classica, e il troll tornerebbe a farsi citare da mezzo
                    // thread senza che nulla lo segnali.
                    .put("followup", f.followup)
                    .put("followupScore", f.followupScore ?: JSONObject.NULL)
            )
        }
        root.put("filters", filters)

        // #4b: backup COMPLETO delle impostazioni per costruzione — itera tutte le
        // chiavi delle preferenze "settings" con un tag di tipo (b/i/l/f/s), così
        // ogni nuova impostazione entra nel backup senza aggiungerla a mano, e il
        // tipo si conserva (SharedPreferences è tipizzato: sbagliarlo = crash in lettura).
        val sp = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val settingsJson = JSONObject()
        for ((k, v) in sp.all) {
            val o = JSONObject()
            when (v) {
                is Boolean -> o.put("t", "b").put("v", v)
                is Int -> o.put("t", "i").put("v", v)
                is Long -> o.put("t", "l").put("v", v)
                is Float -> o.put("t", "f").put("v", v.toDouble())
                is String -> o.put("t", "s").put("v", v)
                else -> continue
            }
            settingsJson.put(k, o)
        }
        root.put("settings", settingsJson)

        // 1.2.9 (multi-identità): TUTTE le identità salvate, complete di ogni
        // campo — questo È il formato principale da qui in avanti. "identity"
        // e "server_identities" sotto restano SOLO perché NeReO <1.2.9, se
        // aprisse questo stesso backup, saprebbe ancora recuperare
        // un'identità generale e gli eventuali override per-server (senza
        // multi-identità vera, ma senza perdere tutto).
        val identitiesJson = JSONArray()
        for (i in identities) {
            identitiesJson.put(
                JSONObject()
                    .put("nickname", i.nickname)
                    .put("name", i.name)
                    .put("email", i.email)
                    .put("sigEnabled", i.sigEnabled)
                    .put("sigRandom", i.sigRandom)
                    .put("signature", i.signature)
                    .put("userAgent", i.userAgent)
                    .put("xNoArchive", i.xNoArchive)
                    .put("customHeaders", i.customHeaders)
                    .put("replyIntro", i.replyIntro)
                    .put("faceB64", i.faceB64 ?: JSONObject.NULL)
                    .put("faceSize", i.faceSize)
                    .put("isDefault", i.isDefault)
                    .put("sortOrder", i.sortOrder)
            )
        }
        root.put("identities", identitiesJson)

        // Formato legacy (compatibilità <1.2.9): l'identità DEFAULT come
        // "identity" generale. Fonte: il DB (fresco); le SharedPreferences
        // "identity" restano come ultima ruota di scorta se per qualche
        // motivo non esistesse ancora nessuna identità (non dovrebbe succedere:
        // la migrazione gira all'avvio, prima che l'utente possa arrivare qui).
        val defaultIdentity = identities.firstOrNull { it.isDefault } ?: identities.firstOrNull()
        root.put(
            "identity",
            JSONObject()
                .put("name", defaultIdentity?.name ?: idp.getString("name", "") ?: "")
                .put("email", defaultIdentity?.email ?: idp.getString("email", "") ?: "")
                .put("signature", defaultIdentity?.signature ?: idp.getString("signature", "") ?: "")
                .put("sig_enabled", defaultIdentity?.sigEnabled ?: idp.getBoolean("sig_enabled", true))
                .put("user_agent", defaultIdentity?.userAgent ?: idp.getString("user_agent", "") ?: "")
                .put("x_no_archive", defaultIdentity?.xNoArchive ?: idp.getBoolean("x_no_archive", false))
                .put("reply_mode", idp.getString("reply_mode", "below") ?: "below")
                .put("custom_headers", defaultIdentity?.customHeaders ?: idp.getString("custom_headers", "") ?: "")
                .put("reply_intro", defaultIdentity?.replyIntro ?: idp.getString("reply_intro", "") ?: "")
                .put("face_b64", defaultIdentity?.faceB64 ?: idp.getString("face_b64", null) ?: JSONObject.NULL),
        )

        // Formato legacy (compatibilità <1.2.9): identità PER-SERVER (nick/mail/
        // firma), rimappate per host:porta come le sottoscrizioni. Fonte: la
        // vera identità assegnata a ciascun server (server.identityId), se c'è
        // — salto i server che usano semplicemente la predefinita.
        val serverIdentities = JSONArray()
        for (s in serverList) {
            val ident = s.identityId?.let { id -> identities.firstOrNull { it.id == id } } ?: continue
            serverIdentities.put(
                JSONObject()
                    .put("server", key(s.host, s.port))
                    .put("name", ident.name)
                    .put("email", ident.email)
                    .put("signature", ident.signature)
            )
        }
        root.put("server_identities", serverIdentities)

        // #4c: discussioni seguite (Message-ID). Al ripristino si rimarcano gli
        // articoli presenti; quelli non ancora riscaricati restano fuori (best-effort).
        val watched = JSONArray()
        for (mid in repo.watchedMessageIdsOnce()) watched.put(mid)
        root.put("watched_threads", watched)

        // 1.6: discussioni ignorate. Campo nuovo e facoltativo: VERSION resta 5.
        val ignorati = JSONArray()
        for (r in repo.ignoratiOnce()) {
            ignorati.put(JSONObject().put("id", r.messageId).put("epoch", r.epoch))
        }
        root.put("ignored_threads", ignorati)

        return root.toString(2)
    }

    /**
     * 1.2.12 (audit di sicurezza) — ripulisce un valore di testo che arriva da
     * un file di backup.
     *
     * Il ripristino è, oggi, la via più comoda per attaccare un utente di
     * NeReO: basta convincerlo ad aprire un file ("ecco la configurazione già
     * pronta per il server X"). I campi dell'interfaccia sono a riga singola,
     * ma il ripristino li scavalca e scrive direttamente nel database, quindi
     * un a-capo infilato qui riemerge dove fa danno: dentro un comando NNTP
     * (`GROUP <nome>`, `AUTHINFO USER <nome>`) o dentro le intestazioni di un
     * articolo in partenza. Si tagliano anche i valori assurdamente lunghi:
     * nessun campo legittimo si avvicina a questi limiti, e un JSON gonfiato
     * non deve poter riempire la memoria.
     */
    private fun pulisci(s: String, max: Int = 4096): String =
        s.replace('\r', ' ').replace('\n', ' ').trim().take(max)

    /**
     * 1.3.7 — COME un ripristino si comporta verso cio' che c'e' gia'.
     *
     * Segnalazione di Dave Royal (15 settembre 2026): «I assumed that restoring
     * the backup REPLACED the existing configuration but I see that it ADDS the
     * filters rather than replacing them - so each is duplicated-ish».
     *
     * Aveva ragione, e il difetto era piu' esteso di quanto avesse visto: anche
     * le IDENTITA' venivano duplicate. Ma il difetto vero non era "aggiunge
     * invece di sostituire": era che nella STESSA operazione l'app faceva TRE
     * cose diverse — dedup su server e sottoscrizioni, inserimento cieco su
     * filtri e identita', sovrascrittura sulle impostazioni. Nessun utente puo'
     * farsi un'aspettativa corretta, perche' non ce n'era una sola da avere.
     */
    enum class ModoRipristino {
        /**
         * Unisce con cio' che c'e'. IDEMPOTENTE: ripristinare due volte lo
         * stesso file lascia esattamente lo stesso stato. E' il predefinito
         * perche' non distrugge niente.
         */
        UNISCI,

        /**
         * Azzera PRIMA filtri e identita', poi importa quelli del backup.
         *
         * Perche' NON tocca server, sottoscrizioni e articoli: cancellare un
         * server porta con se' gruppi e articoli in cascata, cioe' l'intera
         * cache locale. Un ripristino non deve poter distruggere mesi di
         * messaggi scaricati per un tocco sbagliato. Il dialogo lo dice
         * esplicitamente all'utente invece di lasciarlo scoprire dopo.
         */
        SOSTITUISCI,
    }

    /** Chiave di identita' di una regola di filtro: se coincide, e' la stessa regola. */
    private fun chiaveFiltro(
        name: String,
        field: String,
        matchType: String,
        pattern: String,
        groupWildmat: String,
    ): String = listOf(name, field, matchType, pattern, groupWildmat).joinToString("\u0000")

    suspend fun import(
        repo: NewsRepository,
        ctx: Context,
        json: String,
        modo: ModoRipristino = ModoRipristino.UNISCI,
    ): ImportResult {
        val root = JSONObject(json)
        val backupVer = root.optInt("openusenet_backup", 0)

        // SOSTITUISCI: si azzera prima, cosi' il percorso sotto trova le mappe
        // vuote e inserisce tutto come nuovo. Nessun ramo in piu' piu' avanti.
        if (modo == ModoRipristino.SOSTITUISCI) {
            repo.allFiltersOnce().forEach { repo.deleteFilter(it.id) }
            repo.identitiesOnce().forEach { repo.deleteIdentity(it.id) }
        }

        /*
         * Le chiavi con cui si riconosce "questa riga c'e' gia'".
         *
         *  - IDENTITA' -> nickname. Non e' un'invenzione: il formato di backup
         *    USA GIA' il nickname come chiave (nicknameToId, per rilegare i
         *    server alle identita'). La coerenza c'era gia' a meta'.
         *    Un nickname VUOTO non identifica nulla: quelle righe si inseriscono
         *    come nuove, ed e' giusto cosi'.
         *  - FILTRI -> (nome, campo, tipo, pattern, wildmat). Se coincidono e'
         *    la stessa regola e si aggiornano punteggio, scadenza, enabled e
         *    followup. Due regole identiche in tutto TRANNE il punteggio
         *    restano due regole: e' legittimo averle, e non tocca a noi
         *    decidere che l'utente si sia sbagliato.
         */
        val identitaEsistenti = repo.identitiesOnce()
            .filter { it.nickname.isNotBlank() }
            .associateBy { it.nickname }
        val filtriEsistenti = repo.allFiltersOnce()
            .associateBy { chiaveFiltro(it.name, it.field, it.matchType, it.pattern, it.groupWildmat) }

        // 1.2.9 (multi-identità, formato ≥5): le identità vanno create PRIMA
        // dei server, per poter risolvere subito "identity_nickname" → id
        // reale nella stessa passata. Mappa nickname→nuovo id: i nickname non
        // sono garantiti unici in astratto, ma lo sono nella pratica (li
        // abbiamo esportati noi da righe reali) — in caso di doppione vince
        // l'ultimo, innocuo.
        val nicknameToId = HashMap<String, Long>()
        if (backupVer >= 5) {
            root.optJSONArray("identities")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val nick = pulisci(o.optString("nickname", ""), 128)
                    val newId = repo.saveIdentity(
                        Identity(
                            // 1.3.7: se un'identita' con questo nickname c'e'
                            // gia', si AGGIORNA quella invece di crearne una
                            // seconda identica.
                            id = identitaEsistenti[nick]?.id ?: 0L,
                            // 1.2.12: i campi che finiscono in un'intestazione
                            // di un articolo (nome, indirizzo, User-Agent,
                            // intestazioni personalizzate) vengono ripuliti qui.
                            // Firma e introduzione alla citazione restano
                            // MULTIRIGA di proposito: finiscono nel corpo, non
                            // fra le intestazioni, e lì un a-capo è legittimo.
                            nickname = nick,
                            name = pulisci(o.optString("name", ""), 256),
                            email = pulisci(o.optString("email", ""), 256),
                            sigEnabled = o.optBoolean("sigEnabled", true),
                            sigRandom = o.optBoolean("sigRandom", false),
                            signature = o.optString("signature", "").take(8192),
                            userAgent = pulisci(o.optString("userAgent", ""), 256),
                            xNoArchive = o.optBoolean("xNoArchive", false),
                            customHeaders = o.optString("customHeaders", "").take(4096),
                            replyIntro = pulisci(o.optString("replyIntro", ""), 512),
                            faceB64 = if (o.isNull("faceB64")) null
                                      else o.optString("faceB64", "")
                                          .filterNot { c -> c.isWhitespace() }
                                          .take(32000).ifBlank { null },
                            faceSize = o.optInt("faceSize", 48),
                            isDefault = o.optBoolean("isDefault", false),
                            sortOrder = o.optInt("sortOrder", 0),
                        ),
                    )
                    o.optString("nickname", "").takeIf { it.isNotBlank() }?.let { nicknameToId[it] = newId }
                }
            }
        }

        // server (dedupe per host:porta) + mappa chiave→id per le sottoscrizioni
        val hostPortToId = HashMap<String, Long>()
        var nServers = 0
        root.optJSONArray("servers")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                // 1.2.12: host e username finiscono dentro comandi NNTP
                // (l'host nella connessione, l'username in AUTHINFO USER):
                // ripuliti qui, oltre alla difesa in profondità che ora
                // rifiuta gli a-capo dentro NntpClient.command().
                val host = pulisci(o.optString("host"), 253)
                val port = o.optInt("port", 119).coerceIn(1, 65535)
                val id = repo.restoreServer(
                    Server(
                        name = pulisci(o.optString("name", host), 128),
                        host = host,
                        port = port,
                        ssl = o.optBoolean("ssl", false),
                        /*
                         * 1.4: LA GUARDIA isNull NON E' UN VEZZO — SENZA, QUI
                         * NASCE UNO USERNAME CHE NON ESISTE.
                         *
                         * In esportazione scriviamo `s.username ?: JSONObject.NULL`,
                         * cioe' un `null` JSON vero. In lettura, optString
                         * dell'implementazione Android NON restituisce il
                         * fallback su JSONObject.NULL: quel sentinella non e'
                         * un null Java, quindi il ramo del fallback non viene
                         * mai raggiunto e il metodo ne fa il toString(), che
                         * vale "null" — deve valerlo, perche' quell'oggetto
                         * esiste per riserializzarsi come JSON valido.
                         * Risultato: quattro caratteri, non vuoti, ifBlank non
                         * scatta, e un server ANONIMO si ritrova un nome utente
                         * dopo ogni ripristino.
                         *
                         * Trovato il 17 settembre 2026 in un log di Marco:
                         * Paganini (che nel nostro catalogo e' `aperto = true`,
                         * "Nessuna registrazione") mandava
                         *   AUTHINFO USER null   -> 381 Enter password
                         *   AUTHINFO PASS        -> 501 Syntax is: ...
                         * Presente in tutti gli archivi sorgente che abbiamo,
                         * dalla 1.2.8 in poi.
                         *
                         * Era l'UNICO dei sette campi esportati come
                         * JSONObject.NULL letto senza questa guardia: gli altri
                         * sei (authOverrideNonCifrata, identity_nickname,
                         * expiresEpochDay, followupScore, faceB64, face_b64)
                         * ce l'avevano gia'. Una dimenticanza, non una scelta.
                         *
                         * ATTENZIONE al collaudo: un test JUnit sul JVM qui
                         * MENTIREBBE. L'implementazione di riferimento org.json
                         * restituisce il fallback su NULL, quindi passerebbe
                         * anche col codice rotto; e con android.jar stub
                         * JSONObject lancia "not mocked". L'unica verifica che
                         * vale e' sul dispositivo.
                         */
                        username = if (o.isNull("username")) null
                        else pulisci(o.optString("username"), 256).ifBlank { null },
                        password = if (o.has("password")) o.optString("password").ifBlank { null } else null,
                        // 1.2.9 (voce E, roadmap NNTP)
                        startTls = o.optBoolean("startTls", false),
                        authOverrideNonCifrata = if (o.isNull("authOverrideNonCifrata")) null else o.optBoolean("authOverrideNonCifrata"),
                    )
                )
                hostPortToId[key(host, port)] = id
                nServers++
                // 1.2.9 (multi-identità): l'assegnazione arriva DOPO restoreServer
                // — serve l'id reale del server, e restoreServer la esclude di
                // proposito (vedi il commento in Repository.restoreServer).
                if (backupVer >= 5 && !o.isNull("identity_nickname")) {
                    o.optString("identity_nickname", "").takeIf { it.isNotBlank() }
                        ?.let { nicknameToId[it] }
                        ?.let { repo.assignIdentityToServer(id, it) }
                }
            }
        }

        /*
         * 1.4: riparazione dei danni gia' fatti.
         *
         * La guardia isNull qui sopra impedisce che il guasto si RIPETA, ma non
         * tocca le righe che un ripristino precedente ha gia' sporcato. Questo
         * passaggio le rimette a posto — e sta QUI, dopo il blocco dei server,
         * perche' e' il momento in cui l'utente sta gia' guardando l'esito di un
         * ripristino ed e' pronto a leggere che qualcosa e' cambiato.
         *
         * Il criterio e' stretto di proposito (stringa ESATTAMENTE "null" E
         * nessuna password): il falso positivo sarebbe un utente il cui nome
         * utente e' letteralmente "null" su un server senza password. Se mai
         * esistesse, e' un caso che si ripara riscrivendo un campo; il caso
         * opposto — lasciare l'app a tentare un'autenticazione inventata — non
         * si ripara, perche' l'utente non sa nemmeno cosa guardare.
         */
        val nUsernameRiparati = repo.riparaUsernameFinti()

        // sottoscrizioni ai gruppi
        var nSubs = 0
        root.optJSONArray("subscriptions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = hostPortToId[o.optString("server")] ?: continue
                // 1.2.6: backup vecchi senza "sortOrder" → null → ordine di default.
                val ordine = if (o.has("sortOrder")) o.optInt("sortOrder", 0) else null
                // 1.2.12: il nome del gruppo finisce dentro "GROUP <nome>".
                val nomeGruppo = pulisci(o.optString("name"), 512)
                if (nomeGruppo.isBlank()) continue
                repo.subscribe(id, nomeGruppo, ordine)
                // 1.6: un backup vecchio non ha queste chiavi: isNull vale anche
                // per la chiave assente, e il gruppo resta com'e'. La guardia e'
                // quella del commento su `username`, qui sopra.
                repo.ripristinaImpostazioniGruppo(
                    id, nomeGruppo,
                    identityId = if (o.isNull("identity_nickname")) null
                                 else nicknameToId[o.optString("identity_nickname")],
                    intro = if (o.isNull("replyIntro")) null
                            else o.optString("replyIntro").take(4096).ifBlank { null },
                    purgeDays = if (o.isNull("purgeDays")) null else o.optInt("purgeDays"),
                    purgeKeepThreads = if (o.isNull("purgeKeepThreads")) null else o.optInt("purgeKeepThreads"),
                )
                nSubs++
            }
        }

        var nFilters = 0
        root.optJSONArray("filters")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val fNome = o.optString("name", "")
                val fCampo = o.optString("field")
                val fTipo = o.optString("matchType")
                val fPattern = o.optString("pattern")
                val fWildmat = o.optString("groupWildmat", "*")
                repo.saveFilter(
                    FilterRuleEntity(
                        // 1.3.7: stessa regola gia' presente -> si aggiorna,
                        // non si duplica. Vedi chiaveFiltro().
                        id = filtriEsistenti[chiaveFiltro(fNome, fCampo, fTipo, fPattern, fWildmat)]?.id ?: 0L,
                        name = fNome,
                        field = fCampo,
                        matchType = fTipo,
                        pattern = fPattern,
                        score = o.optInt("score", 0),
                        caseSensitive = o.optBoolean("caseSensitive", false),
                        groupWildmat = fWildmat,
                        expiresEpochDay = if (o.isNull("expiresEpochDay")) null else o.optLong("expiresEpochDay"),
                        enabled = o.optBoolean("enabled", true),
                        // M1: un backup prodotto da una versione precedente non
                        // ha queste chiavi ⇒ regola classica, come deve essere.
                        followup = o.optString("followup", "NONE").ifBlank { "NONE" },
                        followupScore = if (o.isNull("followupScore")) null else o.optInt("followupScore"),
                    )
                )
                nFilters++
            }
        }

        root.optJSONObject("settings")?.let { s ->
            if (backupVer >= 4) {
                // #4b: formato completo con tag di tipo → scrivo nelle prefs e ricarico
                val ed = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                val keys = s.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val tv = s.optJSONObject(k) ?: continue
                    when (tv.optString("t")) {
                        "b" -> ed.putBoolean(k, tv.optBoolean("v"))
                        "i" -> ed.putInt(k, tv.optInt("v"))
                        "l" -> ed.putLong(k, tv.optLong("v"))
                        "f" -> ed.putFloat(k, tv.optDouble("v").toFloat())
                        "s" -> ed.putString(k, tv.optString("v"))
                    }
                }
                ed.apply()
                AppSettings.init(ctx)   // ricarica TUTTE le impostazioni in memoria
            } else {
                // backup vecchio (formato flat, <4): solo le impostazioni note dell'epoca
                AppSettings.themeMode = s.optString("theme_mode", AppSettings.themeMode)
                AppSettings.dynamicColor = s.optBoolean("dynamic_color", AppSettings.dynamicColor)
                AppSettings.accent = s.optString("accent", AppSettings.accent)
                AppSettings.bodyMonospace = s.optBoolean("body_monospace", AppSettings.bodyMonospace)
                AppSettings.bodyFontScale =
                    s.optDouble("body_font_scale", AppSettings.bodyFontScale.toDouble()).toFloat()
                AppSettings.autoPurgeDays = s.optInt("auto_purge_days", AppSettings.autoPurgeDays)
                AppSettings.threadView = s.optString("thread_view", AppSettings.threadView)
                AppSettings.readingBg = s.optString("reading_bg", AppSettings.readingBg)
                AppSettings.save()
            }
        }

        // 1.2.9: formato legacy (<5) — "identity" (generale) + "server_identities"
        // (override per-server nome/email/firma) di un backup pre-multi-identità.
        // Passano da IdentityMigrator.migrateFromValues, che crea righe Identity
        // VERE esattamente come farebbe la migrazione al primo avvio (stessa
        // logica, fonte diversa: il JSON del backup invece delle
        // SharedPreferences). Solo se il dispositivo non ha ancora nessuna
        // identità propria: ripristinare più volte lo stesso vecchio backup, o
        // farlo su un'installazione già migrata, non deve duplicarle.
        if (backupVer < 5 && repo.identitiesOnce().isEmpty()) {
            val idn = root.optJSONObject("identity")
            val overrides = ArrayList<LegacyServerOverride>()
            root.optJSONArray("server_identities")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = hostPortToId[o.optString("server")] ?: continue
                    val ov = LegacyServerOverride(
                        serverId = id,
                        name = o.optString("name", ""),
                        email = o.optString("email", ""),
                        signature = o.optString("signature", ""),
                    )
                    if (!ov.isBlank) overrides.add(ov)
                }
            }
            if (idn != null || overrides.isNotEmpty()) {
                val global = LegacyIdentityFields(
                    name = idn?.optString("name", "") ?: "",
                    email = idn?.optString("email", "") ?: "",
                    signature = idn?.optString("signature", "") ?: "",
                    sigEnabled = idn?.optBoolean("sig_enabled", true) ?: true,
                    sigRandom = false,
                    userAgent = idn?.optString("user_agent", "") ?: "",
                    xNoArchive = idn?.optBoolean("x_no_archive", false) ?: false,
                    customHeaders = idn?.optString("custom_headers", "") ?: "",
                    replyIntro = idn?.optString("reply_intro", "") ?: "",
                    faceB64 = idn?.let { if (it.isNull("face_b64")) null else it.optString("face_b64", "").ifBlank { null } },
                    faceSize = 48,
                )
                // Nomi leggibili per le nuove identità sintetizzate: quelli dei
                // server appena ripristinati, non gli id nudi.
                val nomiServer = HashMap<Long, String>()
                root.optJSONArray("servers")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val hp = key(o.optString("host"), o.optInt("port", 119))
                        hostPortToId[hp]?.let { id -> nomiServer[id] = o.optString("name", hp) }
                    }
                }
                IdentityMigrator.migrateFromValues(repo, global, overrides, nomiServer)
            }
        }

        // #4c: rimarca "seguito" gli articoli presenti coi Message-ID salvati.
        root.optJSONArray("watched_threads")?.let { arr ->
            val mids = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { mids.add(it) }
            if (mids.isNotEmpty()) repo.markWatchedByMessageIds(mids)
        }

        // 1.6: un backup vecchio non ha la chiave, e non cambia niente.
        root.optJSONArray("ignored_threads")?.let { arr ->
            val righe = ArrayList<IgnoredId>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = pulisci(o.optString("id"), 998)
                if (id.isNotBlank()) righe.add(IgnoredId(id, o.optLong("epoch", 0L)))
            }
            repo.ripristinaIgnorati(righe)
        }

        // 2.1(b): dopo il ripristino delle identità, ricarica le email note dal DB.
        AppSettings.reloadIdentityEmails(repo)

        return ImportResult(nServers, nSubs, nFilters, nUsernameRiparati)
    }
}
