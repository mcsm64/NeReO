/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        Server::class, Group::class, Article::class, FilterRuleEntity::class,
        AvailableGroup::class, Draft::class,
        // M1 (release personale Marco): registro degli avvelenati (propagazione alle risposte)
        PoisonedId::class,
        // 1.2.9 (multi-identità + voce E roadmap NNTP): profili mittente riutilizzabili
        Identity::class,
        // 1.4 (TOFU): identità TLS osservate e approvate, una per riga server.
        TlsPin::class,
        // 1.6 (sondaggio sui gruppi): registro delle discussioni ignorate.
        IgnoredId::class,
    ],
    version = 13,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun groupDao(): GroupDao
    abstract fun articleDao(): ArticleDao
    abstract fun filterDao(): FilterDao
    abstract fun availableGroupDao(): AvailableGroupDao
    abstract fun draftDao(): DraftDao
    abstract fun poisonedIdDao(): PoisonedIdDao
    abstract fun ignoredIdDao(): IgnoredIdDao
    abstract fun identityDao(): IdentityDao

    /** 1.4: conteggi di tutte le tabelle — Diagnostica database in Manutenzione. */
    abstract fun diagnosticaDao(): DiagnosticaDao

    /** 1.4: identita' TLS osservate e approvate (TOFU). */
    abstract fun tlsPinDao(): TlsPinDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v2→v3: aggiunge Group.purgeDays (retention per gruppo). Preserva i dati esistenti. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE groups ADD COLUMN purgeDays INTEGER")
            }
        }

        /** v3→v4: aggiunge Article.faceB64 (header Face). Preserva i dati esistenti. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN faceB64 TEXT")
            }
        }

        /** v4→v5: aggiunge Article.headersRaw (vista intestazioni). Preserva i dati esistenti. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN headersRaw TEXT")
            }
        }

        /** v5→v6: aggiunge Article.replyToMe (risposte ai miei messaggi). Preserva i dati. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN replyToMe INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v6→v7: aggiunge la tabella delle bozze.
         *
         * È una CREATE TABLE, quindi non tocca nessun dato esistente: a
         * differenza di un ALTER TABLE non riscrive righe già presenti.
         * I tipi devono corrispondere esattamente a quelli che Room deduce
         * dall'entità Draft, altrimenti alla prima apertura scatta
         * fallbackToDestructiveMigration e il database viene azzerato.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `drafts` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`groupId` INTEGER NOT NULL, " +
                        "`subject` TEXT NOT NULL, " +
                        "`body` TEXT NOT NULL, " +
                        "`fromName` TEXT NOT NULL, " +
                        "`fromEmail` TEXT NOT NULL, " +
                        "`references` TEXT NOT NULL, " +
                        "`supersedesId` TEXT NOT NULL, " +
                        "`replyToArticleId` INTEGER NOT NULL, " +
                        "`updatedEpoch` INTEGER NOT NULL)"
                )
            }
        }

        /** v7→v8: aggiunge Group.sortOrder (riordino manuale gruppi). Preserva i dati esistenti. */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE groups ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v8→v9: aggiunge Group.purgeKeepThreads (conservazione a numero di discussioni). Preserva i dati. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE groups ADD COLUMN purgeKeepThreads INTEGER")
            }
        }

        /**
         * v9→v10 (M1, release personale Marco): estensione di una regola alle
         * RISPOSTE.
         *
         * Due ALTER TABLE con DEFAULT sulle regole esistenti — che restano
         * quindi regole classiche, followup = 'NONE' — e una CREATE TABLE per
         * il registro degli avvelenati.
         *
         * ATTENZIONE ai tipi: devono corrispondere ESATTAMENTE a quelli che Room
         * deduce dalle entita', altrimenti alla prima apertura scatta
         * fallbackToDestructiveMigration e il database viene azzerato. In
         * particolare `followup` e' TEXT NOT NULL con default, `followupScore`
         * e' INTEGER nullable (niente NOT NULL), e poisoned_ids ha una
         * PRIMARY KEY COMPOSTA (messageId, ruleId), non un rowid autogenerato.
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE filter_rules ADD COLUMN followup TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE filter_rules ADD COLUMN followupScore INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `poisoned_ids` (" +
                        "`messageId` TEXT NOT NULL, " +
                        "`groupId` INTEGER NOT NULL, " +
                        "`epoch` INTEGER NOT NULL, " +
                        "`ruleId` INTEGER NOT NULL, " +
                        "`seed` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`messageId`, `ruleId`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_poisoned_ids_ruleId` ON `poisoned_ids` (`ruleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_poisoned_ids_groupId` ON `poisoned_ids` (`groupId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_poisoned_ids_epoch` ON `poisoned_ids` (`epoch`)")

                // Le regole nate dal PLONK RAPIDO (menu contestuale) vengono
                // promosse a regole estese. E' una scelta deliberata di questa
                // release personale, non un effetto collaterale: "plonk" ha
                // sempre voluto dire "non voglio piu' vedere costui", e finora
                // si fermava a meta' lasciando in piedi mezzo thread che lo
                // citava.
                //
                // Tocca SOLO le regole col nome generato da plonkAuthor
                // ("Plonk: ..."), mai quelle scritte a mano dall'utente, che
                // restano esattamente come sono. L'effetto si vede al primo
                // sync o applicando i filtri ai messaggi gia' scaricati.
                db.execSQL(
                    "UPDATE filter_rules SET followup = 'SUBTREE' " +
                        "WHERE name LIKE 'Plonk: %' AND field = 'FROM'"
                )
            }
        }

        /**
         * v10→v11 (1.2.9): tabella `identities` (multi-identità, vedi
         * [Identity]) + tre colonne nuove su `servers` (assegnazione identità,
         * STARTTLS, override per-server dell'auth non cifrata — voce E della
         * roadmap NNTP). Preserva i dati esistenti: CREATE TABLE per la parte
         * nuova, tre ALTER TABLE nullable/con default per `servers` — nessuna
         * riga esistente cambia significato (identityId nullo = "usa la
         * generale", esattamente il comportamento di oggi).
         *
         * ATTENZIONE ai tipi (lezione di MIGRATION_9_10): devono combaciare
         * ESATTAMENTE con quelli dedotti da Room dall'entità, altrimenti alla
         * prima apertura scatta fallbackToDestructiveMigration e si perde tutto.
         * `isDefault`/`sigEnabled`/`sigRandom`/`xNoArchive` sono INTEGER (Boolean
         * Room); `faceB64` e `authOverrideNonCifrata` sono nullable (niente
         * NOT NULL); `identityId` è nullable (Long? Room, FK "morbida": nessun
         * vincolo FOREIGN KEY dichiarato, per restare tollerante a un'identità
         * cancellata — la risoluzione a runtime ricade sulla default).
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `identities` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`nickname` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`email` TEXT NOT NULL, " +
                        "`sigEnabled` INTEGER NOT NULL, " +
                        "`sigRandom` INTEGER NOT NULL, " +
                        "`signature` TEXT NOT NULL, " +
                        "`userAgent` TEXT NOT NULL, " +
                        "`xNoArchive` INTEGER NOT NULL, " +
                        "`customHeaders` TEXT NOT NULL, " +
                        "`replyIntro` TEXT NOT NULL, " +
                        "`faceB64` TEXT, " +
                        "`faceSize` INTEGER NOT NULL, " +
                        "`isDefault` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE servers ADD COLUMN identityId INTEGER")
                db.execSQL("ALTER TABLE servers ADD COLUMN startTls INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE servers ADD COLUMN authOverrideNonCifrata INTEGER")
            }
        }

        /**
         * 1.4 — UNA MIGRAZIONE, TRE MODIFICHE, NESSUN LETTORE.
         *
         * Le tre modifiche della 1.4 allo schema si fanno in un colpo solo:
         * due migrazioni in due release sarebbero il doppio delle occasioni di
         * sbagliare, e qui una migrazione che va storta non costa un difetto,
         * costa i dati di qualcuno.
         *
         * MA NESSUNA DELLE TRE E' ANCORA LETTA DA NIENTE, ed e' voluto. Questa
         * build contiene la SOLA migrazione: se i dati sopravvivono, la
         * migrazione e' assolta, e le funzioni che useranno queste colonne
         * arrivano dopo con un solo sospettato ciascuna. E' la regola della
         * 1.2.14 applicata al database invece che alla connessione.
         *
         * 1. `tls_pins`: identita' TLS osservate/approvate (vedi TlsPin).
         * 2. `servers.eccezioneScadenza`: eccezione "certificato scaduto" per
         *    riga server.
         * 3. `groups.replyIntro`: introduzione alla citazione per gruppo
         *    (Henry The Mole).
         *
         * ATTENZIONE alla forma del CREATE TABLE: Room confronta lo schema
         * DICHIARATO con quello TROVATO a ogni apertura, e uno scarto — anche
         * un NOT NULL di troppo — fa fallire l'apertura del database. La forma
         * qui sotto ricalca quella di MIGRATION_10_11, che e' in produzione
         * dalla 1.2.9 e quindi e' gia' stata verificata sul campo.
         */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tls_pins` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`serverId` INTEGER NOT NULL, " +
                        "`host` TEXT NOT NULL, " +
                        "`port` INTEGER NOT NULL, " +
                        "`spkiSha256` TEXT NOT NULL, " +
                        "`certificateSha256` TEXT NOT NULL, " +
                        "`subject` TEXT NOT NULL, " +
                        "`issuer` TEXT NOT NULL, " +
                        "`notBefore` INTEGER NOT NULL, " +
                        "`notAfter` INTEGER NOT NULL, " +
                        "`observedAt` INTEGER NOT NULL, " +
                        "`approvedAt` INTEGER, " +
                        "`state` TEXT NOT NULL)"
                )
                // Indice UNICO: al piu' un'identita' per riga server. Il nome
                // deve essere ESATTAMENTE quello che Room si aspetta —
                // index_<tabella>_<colonna> — altrimenti la verifica fallisce.
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_tls_pins_serverId` ON `tls_pins` (`serverId`)"
                )
                db.execSQL("ALTER TABLE servers ADD COLUMN eccezioneScadenza INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE groups ADD COLUMN replyIntro TEXT")
            }
        }

        /**
         * 1.6 — UNA MIGRAZIONE, DUE MODIFICHE, NESSUN LETTORE. Stessa regola
         * della 11 -> 12: viaggia da sola in una build, e le funzioni che la
         * useranno (Identita' per gruppo, Ignora discussione) arrivano dopo.
         *
         * 1. `groups.identityId`: nullable, nessun default, stessa forma di
         *    `groups.replyIntro`. Le righe esistenti restano NULL, cioe'
         *    «eredita»: nessun gruppo cambia comportamento.
         * 2. `ignored_ids`: tabella nuova, non tocca niente che esista.
         *
         * Una CREATE TABLE e una ALTER TABLE ADD COLUMN senza default non
         * riscrivono le righe: durata istantanea anche su archivi grandi.
         *
         * ATTENZIONE alla forma: chiave primaria su `messageId` (TEXT NOT NULL)
         * e nome dell'indice ESATTAMENTE index_<tabella>_<colonna>. Provata su
         * SQLite contro lo schema generato da Room, vedi NeReO_1.6_180.md.
         */
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE groups ADD COLUMN identityId INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ignored_ids` (" +
                        "`messageId` TEXT NOT NULL, " +
                        "`epoch` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`messageId`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ignored_ids_epoch` ON `ignored_ids` (`epoch`)")
            }
        }

        private const val DB_NAME = "openusenet.db"

        /**
         * 1.4: lo stesso nome, ma leggibile da fuori. Serve alla Diagnostica
         * database per misurare quanto occupa il file su disco — e va letto da
         * QUI, non riscritto a mano nella schermata: due copie della stessa
         * stringa sono due occasioni di divergere.
         */
        const val NOME_FILE = DB_NAME

        /**
         * DEVE combaciare con @Database(version = ...) qui sopra, e PASSI_MIGRAZIONE
         * con la lista addMigrations(). Se qualcuno li lascia disallineati il
         * caso peggiore e' un backup di scorta inutile (vedi DbSafety): mai una
         * perdita di dati. Restano comunque nella checklist di rilascio.
         */
        private const val SCHEMA_VERSION = 13

        /**
         * 1.4: la versione DICHIARATA, leggibile da fuori per la Diagnostica
         * database. La si confronta con quella TROVATA sul file
         * ([NewsRepository.versioneSchemaSuDisco]): dopo un'apertura riuscita
         * devono coincidere, e se un giorno non coincidessero quello e' il
         * primo numero da guardare.
         */
        const val VERSIONE_SCHEMA_DICHIARATA = SCHEMA_VERSION
        private val PASSI_MIGRAZIONE = listOf(
            2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9, 9 to 10, 10 to 11,
            11 to 12, 12 to 13,
        )

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: run {
                    // Rete di sicurezza: se sta per scattare un wipe distruttivo,
                    // copia prima il database e lascia un flag per avvisare l'utente.
                    proteggiPrimaDelWipe(context.applicationContext)
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME,
                    )
                        .addMigrations(
                            MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
                            MIGRATION_11_12, MIGRATION_12_13,
                        )
                        // backstop: se compare uno scarto di schema non previsto, riparte pulito
                        // (ma solo DOPO che proteggiPrimaDelWipe ha messo in salvo i dati).
                        // 1.4.10 (voce 9): `false` NON e' una scelta nostra, e'
                        // l'equivalente che Room dichiara nel `ReplaceWith` della
                        // propria deprecazione — comportamento invariato rispetto
                        // alla vecchia chiamata senza argomenti. Il quaderno e'
                        // rimasto fermo settimane su «la documentazione non dice
                        // cosa faccia dropAllTables»: la risposta l'ha data la
                        // correzione rapida dell'IDE, non una ricerca.
                        .fallbackToDestructiveMigration(false)
                        .build().also { instance = it }
                }
            }

        /**
         * Se il database su disco ha una versione che Room non sa migrare (e che
         * quindi verrebbe azzerata), ne fa una COPIA fisica con timestamp e
         * registra un flag in SharedPreferences perche' l'interfaccia possa
         * avvisare l'utente al primo avvio. Non fa nulla se il database e'
         * assente, nuovo o gia' migrabile: costo zero nel caso normale.
         */
        private fun proteggiPrimaDelWipe(context: Context) {
            val dbFile = context.getDatabasePath(DB_NAME)
            val onDisk = leggiVersioneSuDisco(dbFile) ?: return
            val migrabili = DbSafety.versioniMigrabili(PASSI_MIGRAZIONE, SCHEMA_VERSION)
            if (!DbSafety.serveBackup(onDisk, SCHEMA_VERSION, migrabili)) return

            val backupPath = backupFisico(dbFile)
            Log.w(
                "DbSafety",
                "Schema su disco v$onDisk non migrabile verso v$SCHEMA_VERSION: " +
                    "il database sara' reinizializzato. Backup: ${backupPath ?: "FALLITO"}",
            )
            context.getSharedPreferences(DbSafety.PREFS, Context.MODE_PRIVATE).edit()
                .putString(DbSafety.KEY_BACKUP_PATH, backupPath)
                .putInt(DbSafety.KEY_FROM_VERSION, onDisk)
                .apply()
        }

        /**
         * Legge la user_version del file SQLite senza passare da Room (che
         * scatenerebbe la migrazione). Ritorna null se il file non esiste,
         * -1 se esiste ma e' illeggibile/corrotto (trattato come non migrabile,
         * quindi da mettere comunque al sicuro prima di ricrearlo).
         */
        private fun leggiVersioneSuDisco(dbFile: File): Int? {
            if (!dbFile.exists()) return null
            return try {
                SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                    .use { it.version }
            } catch (e: Exception) {
                Log.w("DbSafety", "Versione del database illeggibile: ${e.message}")
                -1
            }
        }

        /**
         * Copia il database e i suoi file ausiliari (-wal, -shm) accanto
         * all'originale, con un nome a timestamp. Ritorna il percorso del file
         * principale copiato, o null se la copia fallisce.
         */
        private fun backupFisico(dbFile: File): String? {
            return try {
                val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val base = File(dbFile.parentFile, "openusenet-backup-$ts.db")
                for (suffix in listOf("", "-wal", "-shm")) {
                    val src = File(dbFile.path + suffix)
                    if (src.exists()) src.copyTo(File(base.path + suffix), overwrite = true)
                }
                base.path
            } catch (e: Exception) {
                Log.w("DbSafety", "Backup del database fallito: ${e.message}")
                null
            }
        }
    }
}
