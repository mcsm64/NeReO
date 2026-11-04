/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class Server(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 119,
    val ssl: Boolean = false,
    val username: String? = null,
    val password: String? = null,   // cifrata (Android Keystore AES/GCM, marcatore "enc:1:") — v0.8
    val sortOrder: Int = 0,
    /**
     * 1.2.9 (voce E, roadmap NNTP): identità da usare per pubblicare su questo
     * server — chiave esterna verso [Identity]. null = usa l'identità marcata
     * come predefinita (isDefault=true). Server esistenti (pre-1.2.9) restano
     * tutti null dopo la migrazione automatica: comportamento invariato finché
     * non si assegna esplicitamente un'identità diversa dalla generale.
     */
    val identityId: Long? = null,
    /**
     * 1.2.9 (voce E, roadmap NNTP — RFC 4642): STARTTLS, alternativa al TLS
     * implicito ([ssl]) per i server che restano su una porta in chiaro (di
     * solito 119) e offrono l'upgrade a cifrato DOPO la connessione. Ignorato
     * se [ssl] è già true (il TLS implicito non ha bisogno di upgrade). Se il
     * server non supporta STARTTLS la connessione prosegue in chiaro — la
     * stessa protezione già esistente per AUTHINFO (guardia 483) resta comunque
     * attiva, perché guarda la cifratura EFFETTIVA della connessione, non solo
     * questa richiesta.
     */
    val startTls: Boolean = false,
    /**
     * 1.2.9 (voce E, roadmap NNTP): override PER SERVER dell'impostazione
     * globale "Consenti autenticazione non cifrata" (Manutenzione → Sicurezza).
     * null (default) = usa il valore globale, comportamento invariato. true =
     * consenti SEMPRE su questo server, anche se il globale è spento. false =
     * blocca SEMPRE su questo server, anche se il globale è acceso — utile per
     * chi vuole restare severo su un server specifico pur avendo acconsentito
     * altrove.
     */
    val authOverrideNonCifrata: Boolean? = null,
    /**
     * 1.4 — ECCEZIONE "CERTIFICATO SCADUTO" PER QUESTA RIGA SERVER.
     *
     * Quando e' true, e SOLO per questo server, NeReO ignora la FINESTRA DI
     * VALIDITA' del certificato. Continua a pretendere tutto il resto: catena
     * PKIX valida fino a un'ancora del trust store di Android, e verifica del
     * NOME HOST — quest'ultima provata nel prototipo, dove con l'eccezione
     * accesa e un nome host sbagliato l'handshake fallisce comunque.
     *
     * Perche' esiste: Paganini presentava una catena Let's Encrypt regolare e
     * completa con la sola foglia scaduta. Per collegarsi non serviva
     * disattivare la validazione, serviva rilassare UNA condizione. E il
     * server e' stato riparato, ma a RINNOVO MANUALE: il caso tornera'.
     *
     * Si disinnesca da sola: il giorno in cui il server rinnova, la
     * validazione normale passa e questa eccezione diventa inerte. Nessun
     * avviso, nessuna riapprovazione, nessun utente addestrato a cliccare.
     *
     * NB: la colonna esiste dalla migrazione 11 -> 12, ma NON e' ancora letta
     * da nessuno. E' voluto — la migrazione viaggia da sola (vedi Db.kt).
     */
    val eccezioneScadenza: Boolean = false,
)

/**
 * 1.2.9 (multi-identità, richiesta di Marco: "un multi utente reale, come in
 * MesNews e/o NR MOD17 di Piao Hong"): un profilo mittente completo — nome,
 * email, firma, FACE, User-Agent, intestazioni personalizzate, introduzione
 * alla citazione — assegnabile a uno o più server (vedi [Server.identityId]).
 *
 * Prima della 1.2.9 esisteva solo UN'identità generale più, per server, un
 * override libero di soli nome/email/firma (preferenze sciolte, non
 * riutilizzabile fra più server): chi scriveva come "MCSM" su due server
 * doveva copiare gli stessi dati due volte. Qui invece un'identità è un
 * oggetto con un proprio id, assegnabile a quanti server si vuole.
 *
 * [isDefault]: al più UNA riga ha isDefault=true — è l'identità di ricaduta
 * per i server con [Server.identityId] nullo (compresi tutti quelli esistenti
 * prima della migrazione automatica). Vedi IdentityMigrator per la migrazione
 * dai dati pre-1.2.9.
 */
@Entity(tableName = "identities")
data class Identity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nome breve per riconoscerla negli elenchi (es. "MCSM"). Non finisce nei post. */
    val nickname: String,
    val name: String = "",
    val email: String = "",
    val sigEnabled: Boolean = true,
    val sigRandom: Boolean = false,
    val signature: String = "",
    val userAgent: String = "",
    val xNoArchive: Boolean = false,
    val customHeaders: String = "",
    val replyIntro: String = "",
    val faceB64: String? = null,
    val faceSize: Int = 48,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "groups",
    indices = [Index(value = ["serverId", "name"], unique = true)],
)
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: Long,
    val name: String,
    val subscribed: Boolean = false,
    val lastFetchedArticleNumber: Long = 0, // avanzamento sync incrementale
    val lastSyncEpoch: Long = 0,
    val unreadCount: Int = 0,
    /**
     * 1.4 — INTRODUZIONE ALLA CITAZIONE PER QUESTO GRUPPO. null = usa quella
     * dell'identita' (comportamento di sempre).
     *
     * Richiesta di Henry The Mole: differenziare l'introduzione in base al
     * gruppo, tipicamente per LINGUA. Piu' introduzioni sull'identita' danno
     * solo il sorteggio, che e' un'altra cosa: lui vuole una scelta mirata, non
     * un'altra faccia del dado.
     *
     * Stesso schema "null = usa il globale" gia' usato da [purgeDays] e
     * [purgeKeepThreads]: non si inventa una convenzione nuova.
     *
     * NB: la colonna esiste dalla migrazione 11 -> 12, ma NON e' ancora letta
     * da nessuno. E' voluto — la migrazione viaggia da sola (vedi Db.kt).
     */
    val replyIntro: String? = null,
    /**
     * 1.6 (Dave Royal, sondaggio sui gruppi) — IDENTITA' PER QUESTO GRUPPO.
     * null = quella del server, e se manca la predefinita. Precede
     * [Server.identityId]; [replyIntro] continua a vincere sull'introduzione
     * dell'identita'. Chiave «morbida» come quella del server: nessun vincolo
     * FOREIGN KEY, un'identita' cancellata ricade sul livello sotto. La regola
     * sta in [primaIdentita]. Colonna dalla migrazione 12 -> 13.
     */
    val identityId: Long? = null,
    /** Override retention per gruppo: giorni oltre cui eliminare i letti. null = usa il valore globale. */
    val purgeDays: Int? = null,
    /**
     * beta19 (Roxy): conservazione per NUMERO di discussioni invece che per data.
     * Se valorizzato (> 0) ha la PRECEDENZA su purgeDays per questo gruppo: si
     * tengono le N discussioni più recenti e si eliminano le più vecchie (sempre
     * solo lette e non seguite). null/0 = usa la conservazione a giorni.
     * Motivo: con la sola scadenza a tempo un gruppo poco frequentato si svuota,
     * mentre spesso servono discussioni vecchie ma ancora utili.
     */
    val purgeKeepThreads: Int? = null,
    /** Ordine di visualizzazione scelto dall'utente (frecce su/giù). 0 = non ancora riordinato. */
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["groupId", "number"], unique = true),
        Index(value = ["groupId", "read"]),
        Index(value = ["messageId"]),
    ],
)
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val number: Long,
    val messageId: String,
    val subject: String,
    val fromField: String,
    val dateEpoch: Long,
    val references: String = "",
    val bytes: Long = 0,
    val lineCount: Int = 0,
    val body: String? = null,       // null = corpo non ancora scaricato
    val read: Boolean = false,
    val watched: Boolean = false,
    val score: Int = 0,
    val hidden: Boolean = false,    // esito HIDE del motore filtri
    val faceB64: String? = null,    // header "Face:" (PNG 48x48 in Base64), estratto alla lettura
    val headersRaw: String? = null, // intestazioni complete del messaggio, per la vista "Intestazioni"
    val replyToMe: Boolean = false, // risposta a un mio messaggio (rilevata al sync)
)

/**
 * Cache locale dell'elenco completo dei gruppi di un server (LIST ACTIVE).
 * Scaricata una volta, poi la ricerca è istantanea e offline.
 */
@Entity(
    tableName = "available_groups",
    primaryKeys = ["serverId", "name"],
    indices = [Index(value = ["serverId"])],
)
data class AvailableGroup(
    val serverId: Long,
    val name: String,
)

@Entity(tableName = "filter_rules")
data class FilterRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val field: String,        // FilterField.name
    val matchType: String,    // MatchType.name
    val pattern: String,
    val score: Int,
    val caseSensitive: Boolean = false,
    val groupWildmat: String = "*",
    val expiresEpochDay: Long? = null,
    val enabled: Boolean = true,
    /** M1: FollowupMode.name — "NONE" = regola classica (default, invariata). */
    val followup: String = "NONE",
    /** M1: punteggio per gli articoli colpiti per propagazione. null = -100 (soglia HIDE). */
    val followupScore: Int? = null,
)

/**
 * M1 (release personale Marco) — REGISTRO DEGLI AVVELENATI.
 *
 * Contiene i message-id gia' noti come colpiti da una regola che si estende
 * alle risposte: sia i "semi" (i post dell'autore filtrato) sia le risposte
 * raggiunte per propagazione. Solo identificatori, nessun contenuto.
 *
 * Serve a DUE cose distinte, e vale la pena tenerle separate in testa.
 *
 * 1. SOPRAVVIVERE ALLA CANCELLAZIONE. Il KILL in syncGroup scarta l'articolo
 *    PRIMA di inserirlo nel database: al sync successivo il post del troll non
 *    esiste piu' in locale, l'insieme avvelenato nascerebbe vuoto e le sue
 *    risposte passerebbero indisturbate. Sintomo tipico e insidioso: la
 *    funzione sembra corretta al primo sync e smette di funzionare dal secondo.
 *
 * 2. EVITARE LA RISCANSIONE DEL GRUPPO. Registrando anche le risposte gia'
 *    nascoste, la propagazione al sync lavora SOLO sul lotto in arrivo: il
 *    contesto storico e' una query indicizzata invece di un byGroupOnce() su
 *    tutto il gruppo. Senza questo, ogni sync ricadrebbe nel costo quadratico
 *    che aveva fatto crashare beta20 su it.test (~47.000 articoli).
 *
 * Chiave (messageId, ruleId), non il solo messageId: lo stesso messaggio puo'
 * essere avvelenato da due regole diverse, e schiacciare le due righe in una
 * romperebbe la revoca selettiva — disattivando una regola si ri-mostrerebbero
 * messaggi che l'altra vuole ancora nascosti. Il groupId resta fuori dalla
 * chiave di proposito: un crosspost avvelenato in un gruppo resta avvelenato
 * ovunque, coerentemente col filtro XPOST esistente.
 */
@Entity(
    tableName = "poisoned_ids",
    primaryKeys = ["messageId", "ruleId"],
    indices = [Index(value = ["ruleId"]), Index(value = ["groupId"]), Index(value = ["epoch"])],
)
data class PoisonedId(
    val messageId: String,
    val groupId: Long,
    /** data dell'articolo (epoch secondi): serve al purge. */
    val epoch: Long,
    /** regola che l'ha avvelenato: permette la revoca selettiva. */
    val ruleId: Long,
    /** true = post dell'autore filtrato; false = risposta colpita per propagazione. */
    val seed: Boolean = false,
)

/**
 * 1.6 (sondaggio sui gruppi) — REGISTRO DELLE DISCUSSIONI IGNORATE: i message-id
 * dei messaggi di ogni discussione ignorata. Solo identificatori, nessun
 * contenuto.
 *
 * PERCHE' NON `poisoned_ids`: quelle righe significano «colpito da una regola» e
 * proteggono il messaggio dalla revisione dopo un cambio di regole. Riusarle con
 * un ruleId riservato farebbe rispondere lo stesso campo a due domande.
 *
 * PERCHE' NON UNA COLONNA SU `articles`: gli articoli se ne vanno con la
 * conservazione, e con loro il ricordo; una risposta tardiva tornerebbe non
 * letta in una discussione ignorata. Qui la chiave e' il solo message-id, quindi
 * vale per un crosspost e per lo stesso gruppo su piu' server.
 *
 * [epoch]: data dell'articolo (epoch secondi), serve alla pulizia.
 *
 * Tabella dalla migrazione 12 -> 13. La logica sta in filter/IgnoraDiscussione.kt.
 */
@Entity(tableName = "ignored_ids", indices = [Index(value = ["epoch"])])
data class IgnoredId(
    @PrimaryKey val messageId: String,
    val epoch: Long,
)

/**
 * Bozza di messaggio: un messaggio scritto ma non ancora spedito.
 *
 * Conserva anche il CONTESTO della conversazione, non solo oggetto e corpo:
 * senza la catena References una risposta ripresa dopo giorni tornerebbe a
 * essere un messaggio isolato, fuori dalla discussione a cui apparteneva.
 *
 * A differenza dei contrassegni di cancellazione (che vivono in memoria), una
 * bozza deve sopravvivere alla chiusura dell'app: se sparisse al riavvio non
 * servirebbe a nulla. Da qui la tabella dedicata.
 */
@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** gruppo di destinazione. */
    val groupId: Long,
    val subject: String = "",
    val body: String = "",
    /** identità con cui si stava scrivendo (può differire da quella generale). */
    val fromName: String = "",
    val fromEmail: String = "",
    /** catena References: è ciò che tiene la risposta dentro la sua discussione. */
    val references: String = "",
    /** Message-ID da sostituire, se la bozza è un supersede. */
    val supersedesId: String = "",
    /** articolo a cui si stava rispondendo, per ricostruire il contesto. */
    val replyToArticleId: Long = 0,
    val updatedEpoch: Long = 0,
)

/**
 * 1.4 — IDENTITA' TLS OSSERVATA E (forse) APPROVATA per una riga server.
 *
 * Il cuore del TOFU: `PENDING` NON e' fiducia, e' memoria. Una catena che la
 * validazione standard rifiuta viene REGISTRATA qui e la connessione fallisce
 * lo stesso; l'utente approva quando vuole, da Server -> Verifica identita'
 * TLS. Solo allora lo stato passa a `TRUSTED` e il pin comincia a valere.
 *
 * PERCHE' UNA TABELLA E NON COLONNE SU `servers`: sono dati di natura diversa
 * (un'osservazione con la sua data, non una configurazione), e una tabella
 * separata lascia spazio a una storia futura senza allargare `servers`.
 *
 * [spkiSha256] E' IL PIN, ed e' lo SHA-256 dello SubjectPublicKeyInfo del
 * certificato FOGLIA. Mai di un intermedio: appuntare un intermedio
 * significherebbe fidarsi di tutto cio' che quella CA firmera' in futuro,
 * cioe' quasi il contrario di quel che il TOFU serve a fare. E' la stessa
 * definizione che Android usa per la Network Security Configuration.
 *
 * [certificateSha256] e' l'impronta del certificato INTERO: serve solo a
 * mostrarla all'utente e alla diagnosi. Non si confronta mai — cambia a ogni
 * rinnovo anche quando la chiave resta la stessa.
 *
 * [host] e [port] sono quelli OSSERVATI AL MOMENTO DELL'APPROVAZIONE, e non
 * una ripetizione decorativa di cio' che sta in `servers`: se l'utente cambia
 * host o porta di quella riga, il pin DECADE e va riapprovato. Evita che
 * un'approvazione data per un endpoint valga per un altro.
 *
 * NB: la tabella esiste dalla migrazione 11 -> 12, ma NESSUNO la legge ancora.
 * E' voluto: la migrazione viaggia da sola (vedi Db.kt).
 */
/**
 * Gli unici due stati possibili di una riga [TlsPin]. Costanti e non enum
 * perche' il valore finisce TEXT nel database e in un backup JSON: una stringa
 * che si legge a occhio vale piu' di un ordinale che cambia se si riordina.
 */
object TlsPinStato {
    /**
     * VALIDATA DALLE REGOLE NORMALI: catena PKIX fino a un'ancora fidata e nome
     * host corretto. Non c'e' niente da approvare, e dirlo e' importante.
     *
     * PERCHE' QUESTO STATO ESISTE (17 settembre 2026, rilievo di Marco).
     * Nella prima stesura ogni identita' osservata diventava PENDING, anche
     * quella di un server perfettamente in regola. Marco l'ha visto subito:
     * tutti i suoi server sarebbero comparsi come "VISTA, NON FIDATA" senza che
     * ci fosse nulla da fare. E' un allarme falso, ed e' il danno peggiore
     * proprio su questo percorso: un avviso di sicurezza che compare quando non
     * serve addestra a cliccarlo senza leggere — esattamente cio' che il resto
     * di questo disegno cerca di evitare.
     *
     * La riga si scrive lo stesso, perche' i dati del certificato servono
     * all'avviso di scadenza. E' lo STATO a dire la verita': osservata e
     * regolare, non in attesa di un giudizio.
     */
    const val VALIDA = "VALIDA"

    /**
     * Osservata, mostrata, NON fidata: la validazione normale NON e' passata.
     * Non e' un mezzo si': e' un no con memoria.
     */
    const val PENDING = "PENDING"

    /** Approvata esplicitamente da un umano. */
    const val TRUSTED = "TRUSTED"
}

@Entity(
    tableName = "tls_pins",
    indices = [Index(value = ["serverId"], unique = true)],
)
data class TlsPin(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Riga server a cui questa identita' appartiene. Al piu' una per server. */
    val serverId: Long,
    /** Host osservato all'approvazione: se cambia, il pin decade. */
    val host: String,
    /** Porta osservata all'approvazione: se cambia, il pin decade. */
    val port: Int,
    /** IL PIN: SHA-256 dello SubjectPublicKeyInfo della foglia, Base64. */
    val spkiSha256: String,
    /** Impronta del certificato intero: solo per diagnosi e visualizzazione. */
    val certificateSha256: String,
    val subject: String,
    val issuer: String,
    /** Validita' del certificato osservato, in millisecondi epoch. */
    val notBefore: Long,
    val notAfter: Long,
    /** Quando la catena e' stata osservata la prima volta. */
    val observedAt: Long,
    /** Quando l'utente ha approvato. null finche' lo stato e' PENDING. */
    val approvedAt: Long? = null,
    /** "PENDING" (osservata, NON fidata) oppure "TRUSTED" (approvata). */
    val state: String,
)
