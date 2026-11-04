/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Gruppo sottoscritto + totale articoli visibili (per la lista stile Piao Hong). */
data class GroupWithTotal(
    @Embedded val group: Group,
    val totalCount: Int,
)

/**
 * 1.4.1 — proiezione minima di un articolo: l'id e il suo Message-ID.
 *
 * Esiste per propagare il "letto" alle copie crospostate senza caricare gli
 * articoli interi, corpi compresi. Due campi invece di venti, e su un "segna
 * tutto letto" la differenza non e' teorica.
 */
data class IdMessageId(
    val id: Long,
    val messageId: String,
)

/**
 * 1.4.2 (build 114) — quanto PESA la lista di un gruppo, in byte.
 *
 * Nasce dal log di BIG Umberto del 23 settembre: la lettura della lista
 * fallisce con "Couldn't read row 627, col 0 from CursorWindow" su un gruppo
 * di 2301 righe. `SQLiteCursor` tiene una finestra da 2 MB e, per una riga
 * fuori finestra, RI-ESEGUE la query da capo — quindi il numero che conta e'
 * se il risultato ci sta o no in una finestra sola.
 *
 * Due totali separati di proposito:
 *  - [leggere] sono le colonne che la lista MOSTRA davvero;
 *  - [pesanti] sono `body`, `headersRaw` e `faceB64`, che la lista non mostra
 *    mai ma che `SELECT *` si porta dietro a ogni riga.
 *
 * Se [pesanti] e' la fetta grossa, la cura e' proiettare la query; se e'
 * trascurabile e il totale sta sotto i 2 MB, allora la finestra non c'entra e
 * l'innesco e' la scrittura concorrente. E' una misura, non un'ipotesi: serve
 * a decidere con un numero invece che con un'induzione.
 */
data class PesoLista(
    val righe: Int,
    val leggere: Long,
    val pesanti: Long,
)

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY sortOrder, name")
    fun all(): Flow<List<Server>>

    @Query("SELECT * FROM servers ORDER BY sortOrder, name")
    suspend fun allOnce(): List<Server>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun byId(id: Long): Server?

    @Query("SELECT * FROM servers WHERE host = :host AND port = :port LIMIT 1")
    suspend fun byHostPort(host: String, port: Int): Server?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(server: Server): Long

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE servers SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int)

    /** 1.2.9: assegna (o toglie, con null) l'identità dedicata di questo server. */
    @Query("UPDATE servers SET identityId = :identityId WHERE id = :id")
    suspend fun setIdentityId(id: Long, identityId: Long?)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE serverId = :serverId AND subscribed = 1 ORDER BY sortOrder, name")
    fun subscribed(serverId: Long): Flow<List<Group>>

    /**
     * 1.4.4 (§A9, segnalazione di Roxy): aggiunto `serverId` in testa
     * all'ORDER BY. **Non e' un dettaglio estetico.** `sortOrder` e' PER
     * SERVER (vedi `moveGroup(serverId, ...)`), quindi senza `serverId` i
     * gruppi dei vari server venivano INTERCALATI: il primo di A, il primo di
     * B, il secondo di A... Con due gruppi omonimi su server diversi — il caso
     * di Roxy — finivano adiacenti e indistinguibili nella colonna dei
     * pannelli, che e' l'unico consumatore di questa query.
     *
     * `allSubscribedWithTotals` qui sotto ordinava gia' cosi' dal principio:
     * era la vista a pagine a essere giusta, non questa.
     */
    @Query("SELECT * FROM groups WHERE subscribed = 1 ORDER BY serverId, sortOrder, name")
    fun allSubscribed(): Flow<List<Group>>

    @Query(
        "SELECT groups.*, (SELECT COUNT(*) FROM articles WHERE articles.groupId = groups.id AND articles.hidden = 0) AS totalCount " +
            "FROM groups WHERE subscribed = 1 ORDER BY serverId, sortOrder, name"
    )
    fun allSubscribedWithTotals(): Flow<List<GroupWithTotal>>

    @Query("SELECT * FROM groups WHERE subscribed = 1 ORDER BY sortOrder, name")
    suspend fun allSubscribedOnce(): List<Group>

    /** Gruppi sottoscritti di UN server, nell'ordine attuale (sortOrder, poi nome). Snapshot, non Flow. */
    @Query("SELECT * FROM groups WHERE serverId = :serverId AND subscribed = 1 ORDER BY sortOrder, name")
    suspend fun subscribedOnceForServer(serverId: Long): List<Group>

    @Query("UPDATE groups SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int)

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun byId(id: Long): Group?

    @Query("SELECT * FROM groups WHERE serverId = :serverId AND name = :name")
    suspend fun byName(serverId: Long, name: String): Group?

    @Query("SELECT name FROM groups WHERE serverId = :serverId AND subscribed = 1")
    suspend fun subscribedNames(serverId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(group: Group): Long

    @Update
    suspend fun update(group: Group)

    @Query("UPDATE groups SET unreadCount = (SELECT COUNT(*) FROM articles WHERE groupId = :groupId AND read = 0 AND hidden = 0) WHERE id = :groupId")
    suspend fun refreshUnreadCount(groupId: Long)

    @Query("UPDATE groups SET purgeDays = :days WHERE id = :id")
    suspend fun setPurgeDays(id: Long, days: Int?)

    /** 1.4 (Henry The Mole): introduzione alla citazione per questo gruppo. */
    @Query("UPDATE groups SET replyIntro = :intro WHERE id = :id")
    suspend fun setReplyIntro(id: Long, intro: String?)

    /** 1.6 (Dave Royal): identità per questo gruppo. null = eredita dal server. */
    @Query("UPDATE groups SET identityId = :identityId WHERE id = :id")
    suspend fun setIdentityId(id: Long, identityId: Long?)

    /** 1.6: il sync scrive solo le sue colonne, vedi elaboraLottoOverview. */
    @Query("UPDATE groups SET lastFetchedArticleNumber = :numero, lastSyncEpoch = :epoch WHERE id = :id")
    suspend fun setAvanzamentoSync(id: Long, numero: Long, epoch: Long)

    /** beta19 (Roxy): conservazione per numero di discussioni (null = usa i giorni). */
    @Query("UPDATE groups SET purgeKeepThreads = :keep WHERE id = :id")
    suspend fun setPurgeKeepThreads(id: Long, keep: Int?)

    /** Azzera l'avanzamento del sync incrementale: la prossima sync riparte dall'inizio. */
    @Query("UPDATE groups SET lastFetchedArticleNumber = 0 WHERE id = :id")
    suspend fun resetSyncProgress(id: Long)

    /*
     * 1.4.2 (build 114) — qui c'era `delete(id)`, cancellata perche' NON E'
     * MAI STATA CHIAMATA da nessuno. Un gruppo si toglie disiscrivendolo
     * (`subscribed = 0`, che ne conserva gli articoli) oppure insieme al suo
     * server con [deleteByServer]. Una DELETE viva e senza chiamanti, in un
     * progetto senza chiavi esterne e senza CASCADE, e' un'arma carica
     * lasciata sul tavolo: chi la trovasse domani cancellerebbe il gruppo
     * lasciandone indietro tutti gli articoli. Vedi l'audit del 22 settembre.
     */

    /**
     * 1.4.2 (build 113): gli id dei gruppi di un server — TUTTI, sottoscritti
     * o no. Serve a [NewsRepository.eliminaServerEContenuti] per sapere quali
     * articoli portare via insieme al server, e a contare cosa si sta per
     * buttare PRIMA di chiederlo all'utente.
     */
    @Query("SELECT id FROM groups WHERE serverId = :serverId")
    suspend fun idsPerServer(serverId: Long): List<Long>

    @Query("DELETE FROM groups WHERE serverId = :serverId")
    suspend fun deleteByServer(serverId: Long)
}

@Dao
interface ArticleDao {
    @Query(
        """SELECT * FROM articles WHERE groupId = :groupId
           AND (:showHidden OR hidden = 0)
           AND (:unreadOnly = 0 OR read = 0)
           ORDER BY dateEpoch DESC"""
    )
    fun byGroup(groupId: Long, showHidden: Boolean = false, unreadOnly: Boolean = false): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun byId(id: Long): Article?

    @Query("SELECT * FROM articles WHERE messageId = :messageId LIMIT 1")
    suspend fun byMessageId(messageId: String): Article?

    /**
     * 1.4 — TUTTE le copie di un articolo: lo stesso Message-ID nei gruppi
     * diversi in cui e' stato crospostato.
     *
     * L'indice su `messageId` esiste gia' (entita' `Article`): nessuna
     * migrazione, nessuna scansione.
     */
    @Query("SELECT * FROM articles WHERE messageId = :messageId")
    suspend fun tutteLeCopie(messageId: String): List<Article>

    /** 1.6: quali di questi id ci sono in QUESTO gruppo, nascosti compresi. Massimo 500. */
    @Query("SELECT messageId FROM articles WHERE groupId = :groupId AND messageId IN (:ids)")
    suspend fun presentiNelGruppo(groupId: Long, ids: List<String>): List<String>

    /**
     * 1.6: il numero piu' basso del gruppo. I ricostruiti che non hanno un numero
     * in questo gruppo ne prendono uno negativo: l'indice unico (groupId, number)
     * resta rispettato, e i numeri negativi non esistono sul server.
     */
    @Query("SELECT MIN(number) FROM articles WHERE groupId = :groupId")
    suspend fun numeroMinimo(groupId: Long): Long?

    /** 1.6: toglie i ricostruiti a numero negativo che ora arrivano col numero vero. */
    @Query("DELETE FROM articles WHERE groupId = :groupId AND number < 0 AND messageId IN (:ids)")
    suspend fun togliRicostruiti(groupId: Long, ids: List<String>)

    /** 1.4: gli articoli di un elenco di id (per leggerne i Message-ID prima di cambiarli). */
    @Query("SELECT * FROM articles WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<Article>

    /**
     * 1.4.1 — SOLO `id` e `messageId`, senza i corpi.
     *
     * Serve a [org.openusenet.reader.data.NewsRepository] per propagare il
     * "letto" per Message-ID. Con `byIds` un "segna tutto letto" su un gruppo
     * da migliaia di articoli caricherebbe in memoria anche tutti i testi gia'
     * scaricati, per usarne due campi.
     */
    @Query("SELECT id, messageId FROM articles WHERE id IN (:ids)")
    suspend fun idEMessageId(ids: List<Long>): List<IdMessageId>

    /**
     * 1.2.15 (voce D): un crosspost ha una copia per ogni gruppo, tutte con lo
     * stesso Message-ID. Nascondendone una si nascondono tutte, quindi i
     * conteggi "da leggere" vanno rinfrescati per ognuno dei gruppi toccati.
     */
    @Query("SELECT DISTINCT groupId FROM articles WHERE messageId = :messageId")
    suspend fun groupIdsByMessageId(messageId: String): List<Long>

    /** Snapshot (non-Flow) di tutti i messaggi del gruppo, per operazioni una-tantum. */
    @Query("SELECT * FROM articles WHERE groupId = :groupId")
    suspend fun byGroupOnce(groupId: Long): List<Article>

    /**
     * Ricerca nella cache: oggetto, autore e (per i messaggi già scaricati) corpo.
     *
     * 1.4.8 (par. U2, BIG Umberto): clausola `ESCAPE` esplicita su tutte e tre
     * le `LIKE`. Senza, `_` e `%` digitati dall'utente restano METACARATTERI
     * SQL: cercare `_114` vuol dire «un carattere qualsiasi, poi 114», e trova
     * anche « 114». Qui non è un problema di sicurezza come in
     * [hideByAuthorLike] — il testo lo scrive l'utente per sé — ma di
     * correttezza: l'underscore non si può cercare, ed è un carattere che nelle
     * sigle di versione e nei nomi di file compare di continuo. Chi chiama
     * passa il testo già neutralizzato da `testoPerLike` (Repository).
     */
    @Query(
        """SELECT * FROM articles
           WHERE hidden = 0 AND (
               subject LIKE '%' || :q || '%' ESCAPE '\' OR
               fromField LIKE '%' || :q || '%' ESCAPE '\' OR
               (body IS NOT NULL AND body LIKE '%' || :q || '%' ESCAPE '\')
           )
           ORDER BY dateEpoch DESC
           LIMIT 500"""
    )
    fun searchArticles(q: String): Flow<List<Article>>

    /** Come searchArticles ma ristretta a un solo gruppo — stesso `ESCAPE`. */
    @Query(
        """SELECT * FROM articles
           WHERE groupId = :groupId AND hidden = 0 AND (
               subject LIKE '%' || :q || '%' ESCAPE '\' OR
               fromField LIKE '%' || :q || '%' ESCAPE '\' OR
               (body IS NOT NULL AND body LIKE '%' || :q || '%' ESCAPE '\')
           )
           ORDER BY dateEpoch DESC
           LIMIT 500"""
    )
    fun searchArticlesInGroup(groupId: Long, q: String): Flow<List<Article>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<Article>)

    @Update
    suspend fun update(article: Article)

    @Query("UPDATE articles SET read = 1 WHERE groupId = :groupId")
    suspend fun markAllRead(groupId: Long)

    /** Id dei messaggi ancora da leggere: fotografia usata per l'ANNULLA di "segna tutto letto". */
    @Query("SELECT id FROM articles WHERE groupId = :groupId AND read = 0")
    suspend fun unreadIds(groupId: Long): List<Long>

    /** 1.4.1: simmetrico di [unreadIds] — gli id GIA' letti, per "segna tutto da leggere". */
    @Query("SELECT id FROM articles WHERE groupId = :groupId AND read = 1")
    suspend fun readIds(groupId: Long): List<Long>

    @Query("UPDATE articles SET read = 0 WHERE groupId = :groupId")
    suspend fun markAllUnread(groupId: Long)

    @Query("UPDATE articles SET body = :body WHERE id = :id")
    suspend fun setBody(id: Long, body: String)

    /**
     * 1.4.2 — svuota SOLO il corpo, lasciando in piedi la riga dell'articolo.
     *
     * Serve a "Ricarica il messaggio": azzerando il corpo, [fetchBody] non
     * trova piu' la cache e riscende fino a GROUP + ARTICLE. Restano id,
     * Message-ID, numero, stato letto, "seguita", punteggio dei filtri e il
     * marcatore delle risposte a me — cioe' tutto cio' che il DELETE della
     * 1.4 buttava via. NON e' un doppione di [setBody]: quella prende una
     * String non annullabile, e passarle "" lascerebbe una cache vuota che
     * fetchBody restituirebbe come corpo valido.
     */
    @Query("UPDATE articles SET body = NULL WHERE id = :id")
    suspend fun svuotaCorpo(id: Long)

    @Query("UPDATE articles SET faceB64 = :face WHERE id = :id")
    suspend fun setFace(id: Long, face: String)

    @Query("UPDATE articles SET headersRaw = :headers WHERE id = :id")
    suspend fun setHeaders(id: Long, headers: String)

    @Query("UPDATE articles SET read = :read WHERE id IN (:ids)")
    suspend fun setReadBulk(ids: List<Long>, read: Boolean)

    @Query("UPDATE articles SET read = :read WHERE id = :id")
    suspend fun setRead(id: Long, read: Boolean)

    @Query("UPDATE articles SET watched = :watched WHERE id = :id")
    suspend fun setWatched(id: Long, watched: Boolean)

    @Query("UPDATE articles SET watched = :watched WHERE id IN (:ids)")
    suspend fun setWatchedBulk(ids: List<Long>, watched: Boolean)

    // #4c: backup delle discussioni seguite (per Message-ID, stabile fra ripristini)
    @Query("SELECT messageId FROM articles WHERE watched = 1 AND messageId != ''")
    suspend fun watchedMessageIds(): List<String>

    /**
     * 1.4.6 (§A12): quali fra i Message-ID dati appartengono a un messaggio
     * SEGUITO. Serve al sync per far ereditare il «seguito» alle risposte che
     * arrivano dentro una discussione gia' seguita.
     *
     * **Non e' [watchedMessageIds] con un filtro in memoria**, ed e' una scelta
     * di costo: quella carica TUTTI i seguiti del database a ogni lotto, questa
     * si limita a quelli che il lotto nomina davvero e passa dall'indice su
     * `messageId`, lo stesso che usa `byMessageId`. Il chiamante spezza la
     * lista a blocchi (il limite di variabili di SQLite), come gia' si fa per
     * `contaPerGruppi`.
     */
    @Query("SELECT messageId FROM articles WHERE watched = 1 AND messageId IN (:messageIds)")
    suspend fun messageIdSeguitiFra(messageIds: List<String>): List<String>

    @Query("UPDATE articles SET watched = 1 WHERE messageId IN (:messageIds)")
    suspend fun setWatchedByMessageIds(messageIds: List<String>)

    /**
     * Plonk retroattivo: nasconde gli articoli già scaricati di un autore.
     *
     * 1.2.12 (audit di sicurezza): clausola `ESCAPE` esplicita. Il pattern
     * arriva dal campo From di un articolo, cioè da chi lo ha pubblicato:
     * senza escape, un From costruito ad arte (`Troll <%>`) trasformava la
     * condizione in `LIKE '%%%'`, che è vera per QUALUNQUE riga — l'intero
     * database veniva marcato nascosto, in tutti i gruppi. Chi chiama deve
     * neutralizzare `%`, `_` e `\` con la barra rovesciata (vedi
     * Repository.plonkAuthorByFrom).
     *
     * NOTA: che il plonk agisca su tutti i gruppi e non solo su quello corrente
     * NON è un difetto ed è stato lasciato com'è — ignorare un autore vale
     * ovunque, è il senso stesso del plonk.
     */
    // 1.5.3: scrive anche il PUNTEGGIO del plonk (-9999). Prima nascondeva e
    // basta, lasciando il punteggio vecchio: tolto il plonk, la revisione non
    // poteva distinguere quelle righe da un messaggio sostituito da un
    // Supersedes (che resta nascosto di proposito), e le lasciava nascoste.
    @Query("UPDATE articles SET hidden = 1, score = :score WHERE fromField LIKE :like ESCAPE '\\'")
    suspend fun hideByAuthorLike(like: String, score: Int)

    /**
     * 1.5.3: rimette in chiaro per ID DI RIGA, non per message-id. Ogni copia
     * di un crosspost la decide la revisione del SUO gruppo (le regole possono
     * avere un ambito), e il veto di `applicaVetoCrosspost` ha gia' trattenuto
     * quelle che in un altro gruppo devono restare nascoste.
     */
    @Query("UPDATE articles SET hidden = 0, score = :score WHERE id IN (:ids)")
    suspend fun rimettiInChiaroByIds(ids: List<Long>, score: Int)

    /**
     * M1: nasconde per message-id invece che per id di riga. La propagazione
     * ragiona in message-id (sono loro a formare l'albero) e cosi' colpisce
     * anche le copie crosspostate in altri gruppi, che e' il comportamento
     * voluto: se il ramo e' avvelenato lo e' ovunque.
     */
    @Query("UPDATE articles SET hidden = :hidden, score = :score WHERE messageId IN (:messageIds)")
    suspend fun setHiddenAndScoreByMessageIds(messageIds: List<String>, hidden: Boolean, score: Int)

    /** M1: rimette in chiaro (revoca di una regola). Non tocca il punteggio. */
    @Query("UPDATE articles SET hidden = 0 WHERE messageId IN (:messageIds)")
    suspend fun unhideByMessageIds(messageIds: List<String>)

    /**
     * 1.5.1 (§A12-retro meta' B): il punteggio delle risposte colpite da una
     * regola propagante POSITIVA («Segui», «Evidenzia»), senza toccare `hidden`.
     * Serve anche alla revoca (score = 0).
     *
     * ⚠️ `AND hidden = 0` di proposito, come nella 1.4.8: un messaggio nascosto
     * non si vede, quindi un punteggio nuovo non cambierebbe niente a schermo —
     * e riscriverlo cancellerebbe il punteggio di propagazione NEGATIVA, che e'
     * l'unica traccia del perche' e' nascosto.
     *
     * Per message-id, come la propagazione negativa: un ramo seguito lo e'
     * ovunque, cosi' come un ramo avvelenato.
     */
    @Query("UPDATE articles SET score = :score WHERE messageId IN (:messageIds) AND hidden = 0")
    suspend fun setScoreVisibiliByMessageIds(messageIds: List<String>, score: Int)

    /**
     * 1.4.8 (§A12-retro) — SCRIVE SOLO IL PUNTEGGIO.
     *
     * A cosa serve: in NeReO «evidenziato» non e' un flag memorizzato, e'
     * una LETTURA del punteggio (`ThreadListScreen`: colora la riga quando
     * `article.score >= 100`). Quindi per rendere retroattiva una regola
     * «Evidenzia» non serve nessuna colonna nuova: basta che il punteggio
     * che il motore ha gia' calcolato venga scritto. Prima della 1.4.8
     * `applicaFiltriRetroattivi` lo scriveva in un ramo solo, quello HIDE —
     * valutava correttamente e poi buttava via il risultato.
     *
     * PERCHE' NON SI RIUSA [setHiddenAndScoreByMessageIds] CON `hidden = false`,
     * che sarebbe stato gratis. Perche' quella scrive ENTRAMBE le colonne, e un
     * articolo puo' essere nascosto per PROPAGAZIONE (ramo avvelenato da una
     * regola su un altro autore) pur avendo, sui propri campi, un punteggio
     * positivo. Passandogli `hidden = false` si riaprirebbe in silenzio un ramo
     * che un plonk aveva chiuso, su una scrittura di massa, e la cosa si
     * disferebbe solo a mano articolo per articolo. Le due colonne hanno due
     * padroni diversi — il punteggio e' dell'articolo, `hidden` e' anche
     * dell'albero — e vanno scritte da due query diverse.
     *
     * PERCHE' PER ID DI RIGA E NON PER MESSAGE-ID, al contrario delle due query
     * qui sopra. Perche' il punteggio non e' una proprieta' del messaggio, e'
     * l'esito della valutazione DI QUEL MESSAGGIO IN QUEL GRUPPO: le regole
     * applicate sono quelle che il wildmat ammette per il gruppo
     * (`FilterEngine.rulesForGroup`), quindi due copie crosspostate dello
     * stesso articolo possono legittimamente avere punteggi diversi. Scrivere
     * per message-id le livellerebbe tutte all'ultimo gruppo esaminato dal
     * giro, con un esito che dipende dall'ordine dei gruppi. `hidden`, al
     * contrario, e' per message-id proprio di proposito: un ramo avvelenato lo
     * e' ovunque. Stessa chiave di `deleteByIds`, che e' l'altro ramo che
     * ragiona per riga.
     */
    @Query("UPDATE articles SET score = :score WHERE id IN (:ids)")
    suspend fun setScoreByIds(ids: List<Long>, score: Int)

    /** Housekeeping sicuro: elimina i letti più vecchi, ma tiene non letti e watched. */
    @Query("DELETE FROM articles WHERE dateEpoch < :olderThanEpoch AND read = 1 AND watched = 0")
    suspend fun deleteReadOlderThan(olderThanEpoch: Long): Int

    /** Come sopra, ma solo per un gruppo (retention per-gruppo). */
    @Query("DELETE FROM articles WHERE groupId = :groupId AND dateEpoch < :olderThanEpoch AND read = 1 AND watched = 0")
    suspend fun deleteReadOlderThanInGroup(groupId: Long, olderThanEpoch: Long): Int

    /**
     * beta19 (Roxy): conservazione per NUMERO di discussioni invece che per data.
     * Con la scadenza a tempo un gruppo poco frequentato si svuota; con un tetto
     * a numero di discussioni resta sempre contenuto utile.
     * Tiene le [keep] discussioni più recenti (per data del messaggio più recente
     * di ogni thread, identificato dal rootKey = primo messageId nelle References
     * oppure il proprio messageId se è una radice) ed elimina gli articoli delle
     * discussioni più vecchie. Stesse protezioni del purge a tempo: non tocca i
     * NON letti né i thread seguiti (watched).
     */
    @Query(
        """
        DELETE FROM articles
        WHERE groupId = :groupId
          AND read = 1
          AND watched = 0
          AND COALESCE(NULLIF(SUBSTR(`references`, 1, INSTR(`references` || ' ', ' ') - 1), ''), messageId) NOT IN (
              SELECT rk FROM (
                  SELECT COALESCE(NULLIF(SUBSTR(`references`, 1, INSTR(`references` || ' ', ' ') - 1), ''), messageId) AS rk,
                         MAX(dateEpoch) AS ultimo
                  FROM articles
                  WHERE groupId = :groupId
                  GROUP BY rk
                  ORDER BY ultimo DESC
                  LIMIT :keep
              )
          )
        """
    )
    suspend fun keepOnlyLastThreadsInGroup(groupId: Long, keep: Int): Int

    @Query("DELETE FROM articles WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: Long)

    /** 1.4.2 (build 113): quanti articoli stanno in questi gruppi. Serve a dire
     *  all'utente cosa sta per buttare via cancellando un server. */
    @Query("SELECT COUNT(*) FROM articles WHERE groupId IN (:groupIds)")
    suspend fun contaPerGruppi(groupIds: List<Long>): Int

    /** 1.4.2 (build 113): tutti gli articoli di questi gruppi, in un colpo. */
    @Query("DELETE FROM articles WHERE groupId IN (:groupIds)")
    suspend fun deleteByGroups(groupIds: List<Long>)

    /** Cancellazione locale di messaggi selezionati (non tocca il server). */
    @Query("DELETE FROM articles WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** Messaggi del gruppo ancora senza corpo scaricato, dai piu' recenti. */
    @Query("SELECT * FROM articles WHERE groupId = :groupId AND body IS NULL ORDER BY dateEpoch DESC LIMIT :limit")
    suspend fun withoutBodyInGroup(groupId: Long, limit: Int): List<Article>

    /** Numero di messaggi del gruppo ancora senza corpo. */
    @Query("SELECT COUNT(*) FROM articles WHERE groupId = :groupId AND body IS NULL")
    suspend fun missingBodyCount(groupId: Long): Int
}

@Dao
interface AvailableGroupDao {
    @Query("SELECT COUNT(*) FROM available_groups WHERE serverId = :serverId")
    suspend fun count(serverId: Long): Int

    /**
     * Ricerca incrementale: i match di prefisso vengono prima (digitando
     * "it.tlc.c" la gerarchia it.tlc.cellulari.* appare in testa),
     * poi i match "contains" ovunque nel nome.
     */
    @Query(
        """SELECT name FROM available_groups
           WHERE serverId = :serverId AND name LIKE '%' || :query || '%'
           ORDER BY CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END, name
           LIMIT 500"""
    )
    suspend fun search(serverId: Long, query: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<AvailableGroup>)

    @Query("DELETE FROM available_groups WHERE serverId = :serverId")
    suspend fun clear(serverId: Long)
}

@Dao
interface FilterDao {
    @Query("SELECT * FROM filter_rules ORDER BY id")
    fun all(): Flow<List<FilterRuleEntity>>

    @Query("SELECT * FROM filter_rules")
    suspend fun allOnce(): List<FilterRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: FilterRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<FilterRuleEntity>)

    @Query("DELETE FROM filter_rules WHERE id = :id")
    suspend fun delete(id: Long)
}

/** 1.6: registro delle discussioni ignorate (vedi [IgnoredId]). Solo identificatori. */
@Dao
interface IgnoredIdDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(righe: List<IgnoredId>)

    /** Quali di questi id sono nel registro. Al massimo 500 per volta (variabili SQLite). */
    @Query("SELECT messageId FROM ignored_ids WHERE messageId IN (:ids)")
    suspend fun fra(ids: List<String>): List<String>

    @Query("SELECT COUNT(*) FROM ignored_ids")
    suspend fun conta(): Int

    @Query("SELECT * FROM ignored_ids")
    suspend fun tutti(): List<IgnoredId>

    @Query("DELETE FROM ignored_ids WHERE messageId IN (:ids)")
    suspend fun togli(ids: List<String>)

    @Query("DELETE FROM ignored_ids WHERE epoch < :olderThanEpoch")
    suspend fun deleteOlderThan(olderThanEpoch: Long)
}

/**
 * M1 (release personale Marco): registro degli avvelenati (vedi [PoisonedId]).
 * Tutte le query sono per identificatore, mai per contenuto: la tabella non
 * conserva nulla dei messaggi oltre al loro message-id.
 */
@Dao
interface PoisonedIdDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<PoisonedId>)

    /** Message-id avvelenati di UNA regola dentro un gruppo: il contesto storico della propagazione. */
    @Query("SELECT messageId FROM poisoned_ids WHERE groupId = :groupId AND ruleId = :ruleId")
    suspend fun idsByGroupAndRule(groupId: Long, ruleId: Long): List<String>

    /** Tutti gli avvelenati del gruppo, indipendentemente dalla regola. */
    @Query("SELECT messageId FROM poisoned_ids WHERE groupId = :groupId")
    suspend fun idsByGroup(groupId: Long): List<String>

    /**
     * Message-id da ri-mostrare quando si revoca [ruleId]: colpiti PER
     * PROPAGAZIONE (seed = 0) e rivendicati SOLO da questa regola.
     *
     * La seconda condizione non e' un dettaglio: se due regole avvelenano lo
     * stesso ramo, disattivarne una sola non deve rimettere in chiaro messaggi
     * che l'altra vuole ancora nascosti.
     */
    @Query(
        """SELECT messageId FROM poisoned_ids
           WHERE ruleId = :ruleId AND seed = 0
             AND messageId NOT IN (SELECT messageId FROM poisoned_ids WHERE ruleId != :ruleId)"""
    )
    suspend fun propagatedIdsExclusiveToRule(ruleId: Long): List<String>

    /**
     * Revoca selettiva: cancellando o disattivando una regola si buttano via
     * anche le sue righe, altrimenti resterebbero a filtrare per sempre
     * messaggi che nessuna regola viva rivendica piu'.
     */
    @Query("DELETE FROM poisoned_ids WHERE ruleId = :ruleId")
    suspend fun deleteByRule(ruleId: Long)

    @Query("DELETE FROM poisoned_ids WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: Long)

    /** Purge allineato a quello degli articoli: il registro non deve crescere all'infinito. */
    @Query("DELETE FROM poisoned_ids WHERE epoch < :olderThanEpoch")
    suspend fun deleteOlderThan(olderThanEpoch: Long): Int

    @Query("SELECT COUNT(*) FROM poisoned_ids")
    suspend fun count(): Int
}

/**
 * 1.2.9 (multi-identità): profili mittente riutilizzabili fra più server.
 * Vedi [Identity] per il perché di questa tabella.
 */
@Dao
interface IdentityDao {
    @Query("SELECT * FROM identities ORDER BY sortOrder, nickname")
    fun all(): Flow<List<Identity>>

    @Query("SELECT * FROM identities ORDER BY sortOrder, nickname")
    suspend fun allOnce(): List<Identity>

    @Query("SELECT * FROM identities WHERE id = :id")
    suspend fun byId(id: Long): Identity?

    /** L'identità di ricaduta per i server senza assegnazione esplicita. Al più una riga. */
    @Query("SELECT * FROM identities WHERE isDefault = 1 LIMIT 1")
    suspend fun defaultOnce(): Identity?

    @Query("SELECT COUNT(*) FROM identities")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(identity: Identity): Long

    /** Toglie il flag di default a TUTTE le righe (usato prima di impostarne una nuova: al più una alla volta). */
    @Query("UPDATE identities SET isDefault = 0")
    suspend fun clearDefault()

    @Query("DELETE FROM identities WHERE id = :id")
    suspend fun delete(id: Long)

    /** Slega i server che puntavano a un'identità appena cancellata (ricadranno sulla default). */
    @Query("UPDATE servers SET identityId = NULL WHERE identityId = :id")
    suspend fun unassignFromServers(id: Long)

    /** 1.6: lo stesso per i gruppi (ricadranno sull'identità del server). */
    @Query("UPDATE groups SET identityId = NULL WHERE identityId = :id")
    suspend fun unassignFromGroups(id: Long)

    /**
     * 1.3.2: sostituisce uno User-Agent SOLO dove è ESATTAMENTE [vecchio].
     * Serve alla correzione RFC 5536 (vedi Branding.LEGACY_USER_AGENTS e
     * App.migraUserAgentIdentita). Restituisce quante righe ha toccato.
     *
     * Il confronto esatto è la garanzia: un valore scritto dall'utente non
     * coincide con un nostro predefinito, quindi non viene mai riscritto.
     */
    @Query("UPDATE identities SET userAgent = :nuovo WHERE userAgent = :vecchio")
    suspend fun sostituisciUserAgent(vecchio: String, nuovo: String): Int
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts ORDER BY updatedEpoch DESC")
    fun all(): Flow<List<Draft>>

    @Query("SELECT COUNT(*) FROM drafts")
    fun count(): Flow<Int>

    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun byId(id: Long): Draft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: Draft): Long

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun delete(id: Long)
}

/**
 * Conteggi riga per riga di TUTTE le tabelle — la Diagnostica database della
 * schermata Manutenzione (1.4, versionCode 99).
 *
 * PERCHE' ESISTE.
 * Una migrazione Room che SI APRE non e' una migrazione che HA FUNZIONATO.
 * All'apertura Room confronta lo schema dichiarato con quello trovato: se
 * coincidono apre e non dice altro. Ma una migrazione puo' essere formalmente
 * corretta e aver svuotato una tabella — o essere fallita a meta' e aver fatto
 * scattare `fallbackToDestructiveMigration`. In entrambi i casi l'app si apre
 * benissimo: e' l'archivio dell'utente a non esserci piu'.
 *
 * Il conteggio distingue "si apre" da "ci sono ancora tutti i dati". Si prende
 * prima dell'aggiornamento, si riprende dopo, e i numeri devono coincidere.
 *
 * E NON E' UN PONTEGGIO DA TOGLIERE. Alla prossima segnalazione del tipo "ho
 * perso i messaggi" questo riquadro e' la prima cosa da chiedere all'utente,
 * invece di fargli ricostruire tutto a parole — come il log NNTP e il registro
 * dei crash qui accanto. Una misura che l'utente puo' copiare e incollare vale
 * piu' di dieci messaggi di descrizione.
 *
 * NB: sono nove COUNT(*) su un database locale, eseguiti solo quando l'utente
 * preme il pulsante. Nessuno di questi conteggi gira durante l'uso normale.
 */
@Dao
interface DiagnosticaDao {
    @Query("SELECT COUNT(*) FROM servers")
    suspend fun servers(): Int

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun groups(): Int

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun articles(): Int

    @Query("SELECT COUNT(*) FROM available_groups")
    suspend fun availableGroups(): Int

    @Query("SELECT COUNT(*) FROM filter_rules")
    suspend fun filterRules(): Int

    @Query("SELECT COUNT(*) FROM identities")
    suspend fun identities(): Int

    @Query("SELECT COUNT(*) FROM drafts")
    suspend fun drafts(): Int

    @Query("SELECT COUNT(*) FROM poisoned_ids")
    suspend fun poisonedIds(): Int

    @Query("SELECT COUNT(*) FROM tls_pins")
    suspend fun tlsPins(): Int

    /** 1.6: e' anche la prova, in Diagnostica database, che la 12 -> 13 e' passata. */
    @Query("SELECT COUNT(*) FROM ignored_ids")
    suspend fun ignoredIds(): Int

    /** Articoli di cui e' stato scaricato anche il corpo: dice quanto pesa la cache. */
    @Query("SELECT COUNT(*) FROM articles WHERE body IS NOT NULL")
    suspend fun articoliConCorpo(): Int

    /*
     * 1.4.2 (build 113) — GLI ORFANI, CONTATI E BASTA.
     *
     * Fino alla 112 cancellare un server toglieva la sola riga `servers`:
     * niente chiavi esterne, niente CASCADE, nessuna pulizia da nessuna parte.
     * I suoi gruppi, i suoi articoli, il suo catalogo e i suoi pin restavano
     * li' per sempre — invisibili, irraggiungibili, e contati da questa stessa
     * diagnostica, che percio' mostrava numeri piu' alti di cio' che l'utente
     * vede. Nello screenshot della diagnostica LISTGROUP di BIG Umberto se ne
     * vedono tre, come righe "? — nomegruppo".
     *
     * Dalla 113 [NewsRepository.eliminaServerEContenuti] non ne crea piu'. Qui
     * si contano solo quelli GIA' esistenti: misurare prima, cancellare poi —
     * la stessa doctrina con cui la 1.4 ha messo la diagnostica database prima
     * del TrustManager. La pulizia arrivera' in una build sua, quando sapremo
     * di quali numeri stiamo parlando.
     */
    @Query("SELECT COUNT(*) FROM groups WHERE serverId NOT IN (SELECT id FROM servers)")
    suspend fun gruppiOrfani(): Int

    @Query("SELECT COUNT(*) FROM articles WHERE groupId NOT IN (SELECT id FROM groups)")
    suspend fun articoliOrfani(): Int

    @Query("SELECT COUNT(*) FROM available_groups WHERE serverId NOT IN (SELECT id FROM servers)")
    suspend fun catalogoOrfano(): Int

    @Query("SELECT COUNT(*) FROM tls_pins WHERE serverId NOT IN (SELECT id FROM servers)")
    suspend fun pinOrfani(): Int

    /*
     * 1.4.2 (build 114) — GLI ORFANI, ORA ANCHE CANCELLATI.
     *
     * La 113 li contava soltanto, di proposito: misurare prima. La misura e'
     * arrivata il 23 settembre dal telefono di BIG Umberto — 3 gruppi e 99.906
     * righe di catalogo senza server, su un database di 36 MB — e con un
     * numero vero davanti la pulizia ha smesso di essere un'idea nostra.
     *
     * L'ORDINE DI CHIAMATA NON E' LIBERO, e il chiamante lo rispetta: prima i
     * gruppi, poi gli articoli. Al contrario, gli articoli dei gruppi orfani
     * sarebbero ancora agganciati al loro gruppo nel momento in cui si
     * cancellano, resterebbero li' e diventerebbero orfani un istante dopo —
     * servirebbe una seconda passata. Stessa trappola gia' vista nella 113 con
     * [NewsRepository.eliminaServerEContenuti], solo al rovescio.
     *
     * Ognuna restituisce quante righe ha tolto: e' il numero che l'utente
     * vede a cose fatte, non una stima calcolata prima.
     */
    @Query("DELETE FROM groups WHERE serverId NOT IN (SELECT id FROM servers)")
    suspend fun eliminaGruppiOrfani(): Int

    @Query("DELETE FROM articles WHERE groupId NOT IN (SELECT id FROM groups)")
    suspend fun eliminaArticoliOrfani(): Int

    @Query("DELETE FROM available_groups WHERE serverId NOT IN (SELECT id FROM servers)")
    suspend fun eliminaCatalogoOrfano(): Int

    @Query("DELETE FROM tls_pins WHERE serverId NOT IN (SELECT id FROM servers)")
    suspend fun eliminaPinOrfani(): Int

    /**
     * 1.4.2 (build 114) — il peso in byte della lista di un gruppo.
     * Vedi [PesoLista] per il perche'.
     *
     * `LENGTH` su una colonna di testo conta i CARATTERI, non i byte del file:
     * per un corpo in solo ASCII i due numeri coincidono, per uno con accenti
     * il peso vero e' un filo piu' alto. Va benissimo: qui serve capire se
     * siamo a 500 kB o a 5 MB, non il byte esatto. `IFNULL` perche' `SUM` su
     * un gruppo vuoto torna NULL, e `references` e' una parola riservata di
     * SQL — da qui gli apici inversi.
     */
    @Query(
        """SELECT COUNT(*) AS righe,
                  IFNULL(SUM(LENGTH(subject) + LENGTH(fromField) +
                             LENGTH(`references`) + LENGTH(messageId)), 0) AS leggere,
                  IFNULL(SUM(LENGTH(IFNULL(body, '')) +
                             LENGTH(IFNULL(headersRaw, '')) +
                             LENGTH(IFNULL(faceB64, ''))), 0) AS pesanti
           FROM articles WHERE groupId = :groupId"""
    )
    suspend fun pesoListaGruppo(groupId: Long): PesoLista
}

/**
 * Le identita' TLS osservate e approvate (1.4, TOFU su SPKI).
 *
 * Al piu' UNA riga per server — l'indice unico su `serverId` lo garantisce a
 * livello di database, non di buone intenzioni.
 */
@Dao
interface TlsPinDao {
    @Query("SELECT * FROM tls_pins WHERE serverId = :serverId LIMIT 1")
    suspend fun perServer(serverId: Long): TlsPin?

    @Query("SELECT * FROM tls_pins WHERE serverId = :serverId LIMIT 1")
    fun perServerFlow(serverId: Long): Flow<TlsPin?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pin: TlsPin): Long

    @Query("DELETE FROM tls_pins WHERE serverId = :serverId")
    suspend fun cancellaPerServer(serverId: Long)

    @Query("SELECT * FROM tls_pins")
    suspend fun tuttiOnce(): List<TlsPin>
}
