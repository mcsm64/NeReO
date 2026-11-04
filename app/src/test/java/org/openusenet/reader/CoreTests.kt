/*
 * OpenUsenet — Copyright (C) 2026 OpenUsenet contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.openusenet.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openusenet.reader.data.DbSafety
import org.openusenet.reader.filter.FilterEngine
import org.openusenet.reader.filter.FilterField
import org.openusenet.reader.filter.FilterRule
import org.openusenet.reader.filter.effettoPropagazioneDi
import org.openusenet.reader.filter.EffettoPropagazione
import org.openusenet.reader.filter.FollowupMode
import org.openusenet.reader.filter.FollowupPropagator
import org.openusenet.reader.filter.colpitiDaIgnorate
import org.openusenet.reader.filter.siSegnaLetto
import org.openusenet.reader.filter.campoIdentificaUnAutore
import org.openusenet.reader.filter.identificaUnAutore
import org.openusenet.reader.filter.PropagationInput
import org.openusenet.reader.filter.KillFile
import org.openusenet.reader.filter.MatchType
import org.openusenet.reader.filter.ScorableArticle
import org.openusenet.reader.filter.ScoreResult
import org.openusenet.reader.nntp.MimeUtil
import org.openusenet.reader.nntp.capabilitiesPerIlLog
import java.nio.charset.Charset
import org.openusenet.reader.nntp.NntpClient
import org.openusenet.reader.nntp.EsitoRicerca
import org.openusenet.reader.nntp.NntpException
import org.openusenet.reader.nntp.cercaPerMessageId
import org.openusenet.reader.nntp.intestazioniDaTesta
import org.openusenet.reader.nntp.numeroDaXref
import org.openusenet.reader.nntp.overviewDaIntestazioni
import org.openusenet.reader.thread.idDaCercare
import org.openusenet.reader.thread.idNominati
import org.openusenet.reader.thread.ThreadableArticle
import org.openusenet.reader.thread.Threader
import org.openusenet.reader.ui.articoloEml
import org.openusenet.reader.ui.linkUsenetId
import org.openusenet.reader.ui.Citazione
import org.openusenet.reader.ui.citazioneDa
import org.openusenet.reader.ui.formattaCitazione
import org.openusenet.reader.ui.formattaCitazioni
import org.openusenet.reader.ui.righeCitate
import org.openusenet.reader.ui.righeOriginaliDi
import org.openusenet.reader.ui.riradica
import org.openusenet.reader.ui.nomeFileConserva
import org.openusenet.reader.ui.nomeFileEml
import org.openusenet.reader.ui.messageIdCanonico
import org.openusenet.reader.text.corpoDaInviare
import org.openusenet.reader.text.wrapBody
import org.openusenet.reader.ui.riformattaCitazioni
import org.openusenet.reader.ui.screens.gruppiDestinazione
import org.openusenet.reader.ui.screens.extraPredefinitiCrosspost
import org.openusenet.reader.ui.screens.extraPredefinitiRisposta
import org.openusenet.reader.ui.screens.gruppiChiestiDallAutore
import org.openusenet.reader.ui.screens.DestinazioneRisposta
import org.openusenet.reader.ui.screens.gruppoObbligatorio
import org.openusenet.reader.ui.screens.gruppiOffertiNelSelettore
import org.openusenet.reader.ui.screens.destinazioneAncoraInVigore
import org.openusenet.reader.ui.screens.destinazioneRisposta
import org.openusenet.reader.text.formatoCorpo
import org.openusenet.reader.text.srotolaFlowed
import org.openusenet.reader.text.usaFlowed
import org.openusenet.reader.text.almenoUnaRigaContinua
import org.openusenet.reader.text.avvolgiFlowed
import org.openusenet.reader.text.FormatoCorpo
import org.openusenet.reader.text.profonditaCitazione
import org.openusenet.reader.text.trovaFaccine
import org.openusenet.reader.text.TABELLA_FACCINE
import org.openusenet.reader.ui.screens.followupTo
import org.openusenet.reader.ui.screens.crosspostGroups
import org.openusenet.reader.ui.screens.serverDaMostrare
import org.openusenet.reader.ui.screens.altriGruppiCrosspost
import org.openusenet.reader.data.Server
import org.openusenet.reader.ui.messageIdNudo
import org.openusenet.reader.data.Article
import org.openusenet.reader.ui.screens.ThreadItem
import org.openusenet.reader.ui.screens.discussioneDi
import org.openusenet.reader.ui.screens.rigaUltimoLetto
import org.openusenet.reader.ui.screens.confiniDiscussione
import org.openusenet.reader.ui.screens.esistonoNonLettiDopo
import org.openusenet.reader.ui.screens.idDiscussionePrecedente
import org.openusenet.reader.ui.screens.idDiscussioneSuccessiva
import org.openusenet.reader.ui.screens.idMessaggioPrecedente
import org.openusenet.reader.ui.screens.idMessaggioSuccessivo
import org.openusenet.reader.ui.screens.idPrimoNonLettoDopo
import org.openusenet.reader.ui.screens.idNonLettoNelGruppo
import org.openusenet.reader.ui.screens.idGruppoConNonLetti
import org.openusenet.reader.ui.screens.GruppoDaLeggere
import org.openusenet.reader.update.deveMostrareAggiornato
import org.openusenet.reader.data.segnalibroDopoLotto
import org.openusenet.reader.data.primaIdentita
import org.openusenet.reader.ui.screens.etichettaIdentita
import org.openusenet.reader.update.versionePiuNuova

class FilterEngineTest {

    private fun article(from: String = "mario@example.org", subject: String = "Ciao") =
        ScorableArticle(from, subject, "<x@y>", "it.test")

    @Test
    fun `contains rule matches case-insensitive by default`() {
        val engine = FilterEngine(
            listOf(FilterRule(field = FilterField.SUBJECT, matchType = MatchType.CONTAINS, pattern = "VIAGRA", score = -10000))
        )
        val r = engine.evaluate(article(subject = "Cheap viagra here"), "it.test")
        assertEquals(ScoreResult.Action.KILL, r.action)
    }

    @Test
    fun `regex rule works`() {
        val engine = FilterEngine(
            listOf(FilterRule(field = FilterField.FROM, matchType = MatchType.REGEX, pattern = """spam\d+@.*\.xyz""", score = -200))
        )
        assertEquals(ScoreResult.Action.HIDE, engine.evaluate(article(from = "spam42@junk.xyz"), "it.test").action)
        assertEquals(ScoreResult.Action.NORMAL, engine.evaluate(article(), "it.test").action)
    }

    @Test
    fun `wildcard scope limits rule to matching groups`() {
        val rule = FilterRule(
            field = FilterField.SUBJECT, matchType = MatchType.CONTAINS,
            pattern = "OT:", score = -150, groupWildmat = "it.hobby.*",
        )
        val engine = FilterEngine(listOf(rule))
        assertEquals(ScoreResult.Action.HIDE, engine.evaluate(article(subject = "OT: politica"), "it.hobby.modellismo").action)
        assertEquals(ScoreResult.Action.NORMAL, engine.evaluate(article(subject = "OT: politica"), "it.lavoro").action)
    }

    @Test
    fun `scores accumulate across rules`() {
        val engine = FilterEngine(
            listOf(
                FilterRule(field = FilterField.SUBJECT, matchType = MatchType.CONTAINS, pattern = "PET", score = 60),
                FilterRule(field = FilterField.FROM, matchType = MatchType.CONTAINS, pattern = "esperto", score = 60),
            )
        )
        val r = engine.evaluate(article(from = "esperto@food.it", subject = "Estrusori PET food"), "it.test")
        assertEquals(120, r.score)
        assertEquals(ScoreResult.Action.HIGHLIGHT, r.action)
        assertEquals(2, r.matchedRules.size)
    }

    @Test
    fun `expired rule is ignored`() {
        val engine = FilterEngine(
            listOf(
                FilterRule(
                    field = FilterField.SUBJECT, matchType = MatchType.CONTAINS,
                    pattern = "vendo", score = -10000, expiresEpochDay = 100,
                )
            ),
            todayEpochDay = 200,
        )
        assertEquals(ScoreResult.Action.NORMAL, engine.evaluate(article(subject = "vendo tornio"), "it.test").action)
    }

    @Test
    fun `invalid regex never crashes`() {
        val engine = FilterEngine(
            listOf(FilterRule(field = FilterField.SUBJECT, matchType = MatchType.REGEX, pattern = "([invalid", score = -10000))
        )
        assertEquals(ScoreResult.Action.NORMAL, engine.evaluate(article(subject = "([invalid"), "it.test").action)
    }
}

class KillFileTest {

    @Test
    fun `roundtrip export-parse preserves rules`() {
        val rules = listOf(
            FilterRule(name = "Spam xyz", field = FilterField.FROM, matchType = MatchType.REGEX, pattern = """.*@spam\.xyz""", score = -10000),
            FilterRule(field = FilterField.SUBJECT, matchType = MatchType.CONTAINS, pattern = "OT:", score = -150, groupWildmat = "it.hobby.*"),
            FilterRule(field = FilterField.XPOST, matchType = MatchType.WILDCARD, pattern = "*.binaries.*", score = -500, caseSensitive = true),
        )
        val parsed = KillFile.parse(KillFile.export(rules))
        assertEquals(3, parsed.size)
        assertEquals(rules[0].pattern, parsed[0].pattern)
        assertEquals(MatchType.REGEX, parsed[0].matchType)
        assertEquals("it.hobby.*", parsed[1].groupWildmat)
        assertTrue(parsed[2].caseSensitive)
        assertEquals(MatchType.WILDCARD, parsed[2].matchType)
    }

    @Test
    fun `comments and blank lines are tolerated`() {
        val text = """
            # commento
            Score: -100
            Subject: prova

            # altra regola
            [it.*]
            Score: 50
            From: regex:esperto@.*
        """.trimIndent()
        val parsed = KillFile.parse(text)
        assertEquals(2, parsed.size)
        assertEquals("*", parsed[0].groupWildmat)
        assertEquals("it.*", parsed[1].groupWildmat)
    }
}

class ThreaderTest {

    private fun art(id: String, refs: String = "", date: Long = 0) =
        ThreadableArticle("<$id>", refs, "test", date)

    @Test
    fun `builds simple thread tree`() {
        val roots = Threader.thread(
            listOf(
                art("root", date = 1),
                art("child1", refs = "<root>", date = 2),
                art("child2", refs = "<root> <child1>", date = 3),
            )
        )
        assertEquals(1, roots.size)
        val flat = roots[0].flatten()
        assertEquals(3, flat.size)
        assertEquals(0, flat[0].first) // root depth 0
        assertEquals(1, flat[1].first)
        assertEquals(2, flat[2].first)
    }

    @Test
    fun `orphan becomes root`() {
        val roots = Threader.thread(listOf(art("a"), art("b", refs = "<missing>")))
        assertEquals(2, roots.size)
    }

    @Test
    fun `subject normalization strips Re prefixes`() {
        assertEquals("foo", Threader.normalizeSubject("Re: Re: R: foo"))
        assertEquals("foo", Threader.normalizeSubject("re[2]: foo"))
        assertEquals("foo", Threader.normalizeSubject("foo"))
    }
}

class MimeUtilTest {

    @Test
    fun `decodes B-encoded UTF-8 word`() {
        assertEquals("Prova è", MimeUtil.decodeWords("=?UTF-8?B?UHJvdmEgw6g=?="))
    }

    @Test
    fun `decodes Q-encoded word with underscores`() {
        assertEquals("Città di Milano", MimeUtil.decodeWords("=?UTF-8?Q?Citt=C3=A0_di_Milano?="))
    }

    @Test
    fun `plain header passes through`() {
        assertEquals("Hello world", MimeUtil.decodeWords("Hello world"))
    }

    @Test
    fun `splitArticle unfolds headers`() {
        val (headers, body) = MimeUtil.splitArticle(
            listOf("Subject: parte uno", "\tparte due", "From: a@b", "", "corpo riga 1", "corpo riga 2")
        )
        assertEquals("parte uno parte due", headers["subject"])
        assertEquals(listOf("corpo riga 1", "corpo riga 2"), body)
    }
}

class OverviewParseTest {

    @Test
    fun `parses standard XOVER line`() {
        val line = "123\tOggetto =?UTF-8?B?w6g=?=\tMario <m@example.org>\tMon, 20 Jul 2026 10:00:00 +0200\t<id@srv>\t<parent@srv>\t2048\t42"
        val o = NntpClient.parseOverviewLine(line)
        assertNotNull(o)
        assertEquals(123L, o!!.number)
        assertEquals("Oggetto è", o.subject)
        assertEquals("<id@srv>", o.messageId)
        assertEquals(42, o.lines)
    }

    @Test
    fun `malformed line returns null`() {
        assertFalse(NntpClient.parseOverviewLine("garbage") != null)
    }
}

class DbSafetyTest {

    // Le migrazioni reali della 1.0: contigue da 2 a 9.
    private val passi = listOf(2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9)

    @Test
    fun `versioni migrabili coprono la catena fino a current`() {
        assertEquals(setOf(2, 3, 4, 5, 6, 7, 8, 9), DbSafety.versioniMigrabili(passi, 9))
    }

    @Test
    fun `versione 1 non e migrabile perche manca 1 a 2`() {
        assertFalse(1 in DbSafety.versioniMigrabili(passi, 9))
    }

    @Test
    fun `db nuovo zero non richiede backup`() {
        val m = DbSafety.versioniMigrabili(passi, 9)
        assertFalse(DbSafety.serveBackup(0, 9, m))
    }

    @Test
    fun `versione corrente non richiede backup`() {
        val m = DbSafety.versioniMigrabili(passi, 9)
        assertFalse(DbSafety.serveBackup(9, 9, m))
    }

    @Test
    fun `versione migrabile non richiede backup`() {
        val m = DbSafety.versioniMigrabili(passi, 9)
        assertFalse(DbSafety.serveBackup(5, 9, m))
    }

    @Test
    fun `versione 1 richiede backup`() {
        val m = DbSafety.versioniMigrabili(passi, 9)
        assertTrue(DbSafety.serveBackup(1, 9, m))
    }

    @Test
    fun `downgrade da versione futura richiede backup`() {
        val m = DbSafety.versioniMigrabili(passi, 9)
        assertTrue(DbSafety.serveBackup(11, 9, m))
    }

    @Test
    fun `file illeggibile sentinella meno uno richiede backup`() {
        val m = DbSafety.versioniMigrabili(passi, 9)
        assertTrue(DbSafety.serveBackup(-1, 9, m))
    }

    @Test
    fun `un buco nella catena taglia le versioni a monte`() {
        // manca 4 a 5: da 2,3,4 non si arriva piu' a 9
        val conBuco = listOf(2 to 3, 3 to 4, 5 to 6, 6 to 7, 7 to 8, 8 to 9)
        val m = DbSafety.versioniMigrabili(conBuco, 9)
        assertEquals(setOf(5, 6, 7, 8, 9), m)
        assertTrue(DbSafety.serveBackup(4, 9, m))
        assertFalse(DbSafety.serveBackup(6, 9, m))
    }
}

/**
 * M1 (release personale Marco) — propagazione di una regola alle risposte.
 *
 * Il caso che conta davvero e' `References troncato`: e' l'unico che distingue
 * un'implementazione corretta da una che funziona solo sui thread corti, e
 * corrisponde a un comportamento previsto da RFC 5536 (potatura degli elementi
 * CENTRALI della catena). Senza chiusura transitiva quel test fallisce e il
 * ramo "risorge" a meta' — il sintomo peggiore, perche' sembra casuale.
 */
class FollowupPropagatorTest {

    private fun art(
        id: String,
        refs: String = "",
        data: Long = 0,
        seme: Boolean = false,
    ) = PropagationInput(messageId = id, references = refs, dateEpoch = data, seedPoisoned = seme)

    private val baseThread = listOf(
        art("<troll>", "", 100, seme = true),
        art("<a>", "<troll>", 200),
        art("<b>", "<troll> <a>", 300),
    )

    @Test
    fun `subtree colpisce tutta la discendenza`() {
        val colpiti = FollowupPropagator.propagate(baseThread, emptySet(), FollowupMode.SUBTREE)
        assertEquals(setOf("<a>", "<b>"), colpiti)
    }

    @Test
    fun `direct si ferma al primo livello e non degenera in subtree`() {
        val catena = listOf(
            art("<troll>", "", 100, seme = true),
            art("<l1>", "<troll>", 200),
            art("<l2>", "<troll> <l1>", 300),
            art("<l3>", "<troll> <l1> <l2>", 400),
        )
        assertEquals(setOf("<l1>"), FollowupPropagator.propagate(catena, emptySet(), FollowupMode.DIRECT))
        assertEquals(
            setOf("<l1>", "<l2>", "<l3>"),
            FollowupPropagator.propagate(catena, emptySet(), FollowupMode.SUBTREE),
        )
    }

    @Test
    fun `References troncato - la chiusura transitiva ricostruisce il ramo`() {
        // <l3> ha perso <troll> dal CENTRO della catena (potatura RFC 5536):
        // l'intersezione secca col seme non lo troverebbe.
        val troncato = listOf(
            art("<radice>", "", 50),
            art("<troll>", "<radice>", 100, seme = true),
            art("<l1>", "<radice> <troll>", 200),
            art("<l2>", "<radice> <troll> <l1>", 300),
            art("<l3>", "<radice> <l1> <l2>", 400),
        )
        val colpiti = FollowupPropagator.propagate(troncato, emptySet(), FollowupMode.SUBTREE)
        assertTrue("<l3> deve essere colpito nonostante il References potato", "<l3>" in colpiti)
        assertEquals(setOf("<l1>", "<l2>", "<l3>"), colpiti)

        // controprova: l'approccio ingenuo (solo intersezione col seme) lo perde
        val ingenuo = troncato
            .filter { !it.seedPoisoned && "<troll>" in FollowupPropagator.parseReferences(it.references) }
            .map { it.messageId }
            .toSet()
        assertFalse("la controprova deve dimostrare che l'intersezione secca fallisce", "<l3>" in ingenuo)
    }

    @Test
    fun `il registro degli avvelenati tiene in piedi la propagazione dopo la cancellazione del seme`() {
        // il post del troll e' stato eliminato a un sync precedente: non e' nel lotto
        val soloRisposte = listOf(
            art("<a>", "<troll>", 200),
            art("<b>", "<troll> <a>", 300),
        )
        assertEquals(
            setOf("<a>", "<b>"),
            FollowupPropagator.propagate(soloRisposte, setOf("<troll>"), FollowupMode.SUBTREE),
        )
        // senza registro non colpirebbe nulla: e' esattamente il bug
        // "funziona al primo sync e non al secondo"
        assertTrue(FollowupPropagator.propagate(soloRisposte, emptySet(), FollowupMode.SUBTREE).isEmpty())
    }

    @Test
    fun `l'ordine di arrivo non conta`() {
        val mescolato = listOf(
            art("<l3>", "<radice> <l1> <l2>", 400),
            art("<l1>", "<radice> <troll>", 200),
            art("<radice>", "", 50),
            art("<l2>", "<radice> <troll> <l1>", 300),
            art("<troll>", "<radice>", 100, seme = true),
        )
        assertEquals(
            setOf("<l1>", "<l2>", "<l3>"),
            FollowupPropagator.propagate(mescolato, emptySet(), FollowupMode.SUBTREE),
        )
    }

    @Test
    fun `chi risponde al troll resta visibile altrove`() {
        // e' il requisito centrale: niente colpa per associazione fuori dal ramo
        val misto = listOf(
            art("<troll>", "", 100, seme = true),
            art("<pippo1>", "<troll>", 200),          // risposta al troll  -> colpito
            art("<altro>", "", 150),                   // thread di un altro
            art("<pippo2>", "<altro>", 250),           // pippo risponde altrove -> salvo
            art("<pippo3>", "", 260),                  // pippo apre un thread suo -> salvo
            art("<terzo>", "<altro> <pippo2>", 300),   // risposta a pippo altrove -> salvo
        )
        assertEquals(
            setOf("<pippo1>"),
            FollowupPropagator.propagate(misto, emptySet(), FollowupMode.SUBTREE),
        )
    }

    @Test
    fun `References corrotto o ciclico non manda in loop`() {
        val ciclico = listOf(
            art("<troll>", "", 100, seme = true),
            art("<x>", "<troll> <y>", 200),
            art("<y>", "<x> <troll>", 300),
            art("<z>", "<z>", 400),
            art("<w>", "spazzatura senza identificatori", 500),
        )
        assertEquals(
            setOf("<x>", "<y>"),
            FollowupPropagator.propagate(ciclico, emptySet(), FollowupMode.SUBTREE),
        )
    }

    @Test
    fun `NONE non fa nulla e la lista vuota non esplode`() {
        assertTrue(FollowupPropagator.propagate(baseThread, emptySet(), FollowupMode.NONE).isEmpty())
        assertTrue(FollowupPropagator.propagate(emptyList(), setOf("<troll>"), FollowupMode.SUBTREE).isEmpty())
    }

    @Test
    fun `un articolo gia' registrato non viene riportato una seconda volta`() {
        val gia = listOf(
            art("<a>", "<troll>", 200),
            art("<b>", "<troll> <a>", 300),
        )
        assertEquals(
            setOf("<b>"),
            FollowupPropagator.propagate(gia, setOf("<troll>", "<a>"), FollowupMode.SUBTREE),
        )
    }
}

/** M1: le due chiavi nuove del kill-file, e la retrocompatibilita' del formato. */
class KillFileFollowupTest {

    @Test
    fun `Followup e FollowupScore sopravvivono al giro export-import`() {
        val regola = FilterRule(
            name = "Plonk esteso",
            field = FilterField.FROM,
            matchType = MatchType.REGEX,
            pattern = "troll@.*\\.example",
            score = -9999,
            followup = FollowupMode.SUBTREE,
            followupScore = -100,
        )
        val riletta = KillFile.parse(KillFile.export(listOf(regola))).single()
        assertEquals(FollowupMode.SUBTREE, riletta.followup)
        assertEquals(-100, riletta.followupScore)
        assertEquals(FilterField.FROM, riletta.field)
        assertEquals(-9999, riletta.score)
    }

    @Test
    fun `un kill-file vecchio si rilegge come regola classica`() {
        val vecchio = """
            [it.test]
            Score: -100
            From: spammer@example.org
        """.trimIndent()
        val r = KillFile.parse(vecchio).single()
        assertEquals(FollowupMode.NONE, r.followup)
        assertEquals(FilterRule.DEFAULT_FOLLOWUP_SCORE, r.followupScoreEffettivo)
        assertFalse(r.propaga)
    }

    @Test
    fun `un valore di Followup sconosciuto degrada a regola classica`() {
        val conRefuso = """
            [*]
            Score: -9999
            From: troll@example.org
            Followup: subtrre
        """.trimIndent()
        assertEquals(FollowupMode.NONE, KillFile.parse(conRefuso).single().followup)
    }

    @Test
    fun `una regola senza Followup non emette le chiavi nuove`() {
        val classica = FilterRule(
            field = FilterField.SUBJECT, matchType = MatchType.CONTAINS,
            pattern = "spam", score = -100,
        )
        val testo = KillFile.export(listOf(classica))
        assertFalse(testo.contains("Followup"))
    }

    @Test
    fun `il campo References si esporta e reimporta`() {
        val r = FilterRule(
            field = FilterField.REFERENCES, matchType = MatchType.CONTAINS,
            pattern = "<radice@example.org>", score = -9999,
        )
        assertEquals(FilterField.REFERENCES, KillFile.parse(KillFile.export(listOf(r))).single().field)
    }
}

/**
 * 1.3.6 (secondo giro, richiesta di ObiWan): link permanente a un articolo.
 *
 * Il caso che conta e' l'ULTIMO: un Message-ID puo' contenere caratteri che in
 * un URL significano altro. Se questa classe passa, un link generato da NeReO
 * punta all'articolo giusto anche per quegli articoli.
 */
class LinkArticoloTest {

    @Test
    fun `le parentesi angolari spariscono`() {
        assertEquals("abc@example.org", messageIdNudo("<abc@example.org>"))
    }

    @Test
    fun `una sola parentesi non si tocca`() {
        // Dato malformato: mangiarne meta' in silenzio nasconderebbe il problema.
        assertEquals("<abc@example.org", messageIdNudo("<abc@example.org"))
    }

    @Test
    fun `spazi e a capo vengono tolti`() {
        assertEquals("abc@example.org", messageIdNudo(" <abc@ex\r\nample.org> "))
    }

    @Test
    fun `un Message-ID assente non produce un link monco`() {
        assertEquals("", linkUsenetId(null))
        assertEquals("", linkUsenetId("  "))
        assertEquals("", linkUsenetId("<>"))
    }

    @Test
    fun `il link dell'esempio di ObiWan e' esattamente quello atteso`() {
        assertEquals(
            "https://usenet.id/20260914102709.00000c5b@mvps.org",
            linkUsenetId("<20260914102709.00000c5b@mvps.org>"),
        )
    }

    @Test
    fun `la chiocciola resta leggibile e i caratteri pericolosi no`() {
        // '/' chiuderebbe il segmento, '?' aprirebbe la query, '#' il frammento,
        // '%' sarebbe letto come inizio di una sequenza codificata.
        assertEquals(
            "https://usenet.id/a%2Fb%3Fc%23d%25e@example.org",
            linkUsenetId("<a/b?c#d%e@example.org>"),
        )
    }

    @Test
    fun `la forma canonica rimette le parentesi anche se mancavano`() {
        assertEquals("<abc@example.org>", messageIdCanonico("abc@example.org"))
        assertEquals("<abc@example.org>", messageIdCanonico("<abc@example.org>"))
        assertEquals("", messageIdCanonico(null))
    }

    @Test
    fun `un Message-ID non ASCII si codifica in UTF-8`() {
        assertEquals("https://usenet.id/%C3%A8@example.org", linkUsenetId("<è@example.org>"))
    }
}

/**
 * 1.3.7 — a capo in invio: nessuna riga spezzata da noi finisce con uno spazio.
 *
 * Nasce dalla domanda di Adam H. Kerman del 15 settembre 2026. Uno spazio in
 * coda e' il marcatore di rottura debole del Format=Flowed (RFC 3676): un
 * lettore che conosce f=f riunirebbe la riga con la successiva, disfacendo
 * l'a-capo. Vedi il commento in ComposeLogic.wrapBody.
 *
 * 1.4.6 (§B12.1): wrapBody NON vive piu' in ComposeLogic.kt, ed e' per questa
 * prova che e' stato spostato. Stava in un file che nomina tipi Compose nelle
 * firme di ALTRE funzioni, e siccome le funzioni di primo livello finiscono
 * tutte nella stessa classe, caricare wrapBody voleva dire caricare tipi
 * Android assenti sul banco di prova: un errore di classe, non di asserzione,
 * e un difetto della prova, non dell'app. Ora sta in ui/screens/WrapBody.kt,
 * di Kotlin puro e senza un solo import. Se un giorno questa classe torna a
 * non caricarsi, il primo posto dove guardare e' un import comparso li'.
 */
class WrapBodyTest {

    private val lunga =
        "Traditionally one simply used the character set in common use in the " +
            "group being posted to but there were plenty of posters who used less " +
            "common character sets in those groups."

    private fun righe(s: String) = wrapBody(s).split("\n")

    @Test
    fun `nessuna riga spezzata finisce con uno spazio`() {
        assertTrue(righe(lunga).none { it.endsWith(" ") })
    }

    @Test
    fun `il doppio spazio nel punto di rottura non lascia code`() {
        // Il caso misurato: split(" ") produce un elemento VUOTO che si porta
        // dietro il proprio separatore.
        val conDoppio = "parola ".repeat(9) + " doppio  spazio proprio qui dove la riga si spezza davvero"
        assertTrue(righe(conDoppio).none { it.endsWith(" ") })
    }

    @Test
    fun `il doppio spazio DENTRO la riga sopravvive`() {
        // Non si scartano gli elementi vuoti: collasserebbero i doppi spazi,
        // cioe' riscriverebbero il testo dell'utente.
        assertTrue(wrapBody("due  spazi qui").contains("due  spazi"))
    }

    @Test
    fun `il prefisso di citazione si ripete e non lascia code`() {
        val r = righe("> $lunga")
        assertTrue(r.size > 1)
        assertTrue(r.all { it.startsWith("> ") })
        assertTrue(r.none { it.endsWith(" ") })
    }

    @Test
    fun `il delimitatore di firma conserva il suo spazio`() {
        // "-- " passa dal ramo della firma e non viene toccato: quello spazio
        // e' prescritto dalla convenzione.
        assertTrue(wrapBody("testo\n-- \nfirma").contains("-- "))
    }

    @Test
    fun `una riga gia' corta non viene toccata`() {
        assertEquals("corta", wrapBody("corta"))
    }
}

/**
 * 1.4.8 (par. B6b) — GENERARE Format=Flowed.
 *
 * LA PROVA CHE CONTA E' IL GIRO COMPLETO. `srotolaFlowed` esiste dalla 1.4.6 ed
 * e' stata scritta sulla RFC per leggere i messaggi DEGLI ALTRI: farle leggere i
 * nostri e' il collaudo piu' severo disponibile da qui, perche' le due meta'
 * sono state scritte in momenti diversi e nessuna delle due sa dell'altra.
 * Se il testo torna identico, le rotture debole sono marcate dove devono.
 *
 * *E' il motivo per cui par. B6a e' stato fatto prima: il metro precede di una
 * build la modifica rischiosa.*
 */
class FlowedGenerazioneTest {

    private val lunga =
        "Traditionally one simply used the character set in common use in the " +
            "group being posted to but there were plenty of posters who used less " +
            "common character sets in those groups."

    /** Il giro completo: genera, poi rileggi col lettore della 1.4.6. */
    private fun giro(t: String): List<String> = srotolaFlowed(avvolgiFlowed(t).split("\n"))

    @Test
    fun `un capoverso lungo torna identico dopo il giro completo`() {
        assertEquals(listOf(lunga), giro(lunga))
    }

    @Test
    fun `il doppio spazio nel punto di rottura sopravvive al giro completo`() {
        // Il caso misurato nella 1.3.7: split(" ") produce un elemento VUOTO
        // che si porta dietro il proprio separatore. Qui NON si ripulisce la
        // coda, quindi quel doppio spazio e il marcatore convivono.
        val t = "parola ".repeat(9) + " doppio  spazio proprio qui dove la riga si spezza davvero"
        assertEquals(listOf(t.trimEnd()), giro(t))
    }

    @Test
    fun `nessuna riga supera le 72 colonne, marcatore compreso`() {
        assertTrue(avvolgiFlowed(lunga).split("\n").all { it.length <= 72 })
        assertTrue(avvolgiFlowed("> > $lunga").split("\n").all { it.length <= 72 })
    }

    @Test
    fun `la citazione conserva la profondita'`() {
        // Il contenuto torna intero; la spaziatura dopo i '>' e' la forma
        // NORMALIZZATA, che per la RFC §4.5 e' semanticamente identica
        // ("`>> Exit` e `>>Exit` sono identici") ed e' quella che il lettore
        // ricava anche dall'originale.
        assertEquals(listOf(">$lunga"), giro("> $lunga"))
        assertEquals(listOf(">>$lunga"), giro(">>$lunga"))
        // ogni riga di continuazione riemette i propri '>' (§4.5): due righe di
        // profondita' diversa non si uniscono mai, quindi ometterli spezzerebbe
        // la citazione in due capoversi.
        assertTrue(avvolgiFlowed(">>$lunga").split("\n").all { it.startsWith(">>") })
    }

    @Test
    fun `la forma con gli spazi conserva i due livelli sulle continuazioni`() {
        // ⚠️ 1.6: `> > testo` e' la forma che NeReO stessa produce rispondendo a
        // un messaggio gia' citato. Si contava un solo `>` (quello in testa), e
        // l'altro finiva nel contenuto: la continuazione usciva a livello 1.
        val generato = avvolgiFlowed("> > $lunga").split("\n")
        assertTrue(generato.size > 1)
        assertTrue(generato.all { it.startsWith(">> ") })
        assertEquals(listOf(">>$lunga"), srotolaFlowed(generato))
        // anche a tre livelli, e su una riga corta: stessa profondita', stessa forma
        assertEquals(">>> corta", avvolgiFlowed("> > > corta"))
        // un `>` DOPO il testo non e' un livello
        assertEquals("> a > b", avvolgiFlowed("> a > b"))
    }

    @Test
    fun `le righe citate escono con lo spazio dopo il segno`() {
        // §4.4 permette di proteggere anche le righe che non lo richiedono, e
        // il §5 ne da' la ragione: la resa per chi NON conosce f=f. Senza, le
        // continuazioni uscivano come ">group being posted to".
        assertTrue(avvolgiFlowed("> $lunga").split("\n").all { it.startsWith("> ") })
    }

    @Test
    fun `il rientro voluto di una riga non citata si conserva`() {
        // Qui lo spazio iniziale NON e' convenzione di citazione: e' il rientro
        // di chi scrive, e va protetto perche' arrivi intatto.
        assertEquals(listOf("   elenco rientrato"), giro("   elenco rientrato"))
    }

    @Test
    fun `il rientro si conserva anche quando la riga va spezzata`() {
        // ⚠️ IL DIFETTO CHE QUESTA PROVA CHIUDE, visto da Marco in MesNews: la
        // riga corta arrivava rientrata, quella lunga a filo. `split(" ")` su
        // un contenuto che comincia con spazi produce elementi VUOTI in testa,
        // e il ramo «prima parola» li riassorbiva uno dopo l'altro senza
        // scrivere niente. Ora il rientro si mette da parte PRIMA di spezzare.
        val rientrata = "   " + "parola ".repeat(30)
        assertEquals(listOf(rientrata.trimEnd()), giro(rientrata))
        // e NON si ripete sulle continuazioni: sono lo stesso capoverso, e
        // ripeterlo infilerebbe spazi in mezzo al testo di chi legge
        assertTrue(!avvolgiFlowed(rientrata).split("\n")[1].startsWith(" "))
    }

    @Test
    fun `nessuna parola si incolla alla successiva al punto di rottura`() {
        // ⚠️ IL DIFETTO DELLA 146, visto da Marco al primo post su it.test:
        // "messaggioserve", "numeratoqui". Il ramo `else` chiamava
        // `chiudiRiga` — che rimette il flag «prima parola» a true — e poi
        // appendeva la parola senza rimetterlo a false, cosi' la parola
        // seguente entrava dal ramo «prima parola», che appende SENZA spazio.
        // Ora la prima parola di una riga si posa da un posto solo.
        val paragrafo = "Questo messaggio serve a mettere sotto sforzo il generatore: ogni blocco " +
            "numerato qui sotto esercita una regola diversa della RFC 3676, e va guardato uno per uno."
        assertEquals(listOf(paragrafo), giro(paragrafo))
    }

    @Test
    fun `il doppio spazio sopravvive anche se cade sul punto di rottura`() {
        // ⚠️ Il gemello del difetto qui sopra, e dalla stessa riga: con
        // `corrente.isEmpty()` come condizione, un elemento vuoto che cade
        // subito DOPO una rottura lasciava `corrente` ancora vuota e la parola
        // seguente entrava senza il suo spazio. «La riga e' vuota» non vuol
        // dire «siamo all'inizio della riga».
        val t = "parola ".repeat(9) + " doppio  spazio proprio qui dove la riga si spezza davvero"
        assertEquals(listOf(t.trimEnd()), giro(t))
    }

    @Test
    fun `la firma non viene mai spezzata e il suo spazio resta`() {
        val t = "$lunga\n-- \nMarco"
        val generato = avvolgiFlowed(t)
        assertTrue(generato.contains("\n-- \n"))
        // la riga PRIMA della firma non continua: altrimenti la firma si
        // incollerebbe all'ultimo capoverso di ogni messaggio spedito
        val righe = generato.split("\n")
        val iFirma = righe.indexOf("-- ")
        assertTrue(iFirma > 0)
        assertTrue(!righe[iFirma - 1].endsWith(" "))
    }

    @Test
    fun `lo spazio di protezione va e torna`() {
        // §4.4: una riga che comincia con uno spazio ne riceve un altro, e il
        // lettore ne toglie esattamente uno.
        assertEquals(listOf(" rientrata a mano"), giro(" rientrata a mano"))
        assertEquals(listOf("From qui comincia"), giro("From qui comincia"))
    }

    @Test
    fun `una riga che ci sta non viene toccata, nemmeno a 72 colonne`() {
        // ⚠️ IL DIFETTO CHE QUESTA PROVA CHIUDE, trovato dal messaggio di prova
        // di Marco prima della compilazione. La prima stesura teneva da parte
        // due colonne SEMPRE e confrontava il contenuto con `cols - prof - 2`:
        // una riga di 71 o 72 colonne andata a capo A MANO veniva spezzata in
        // due (66 + 6), e in un lettore che non ricompone si vedeva la
        // scaletta. Cioe' il difetto che f=f dovrebbe togliere, prodotto da
        // noi, e su chiunque impagini a 72 colonne — su Usenet, la
        // maggioranza. Ora il conto si fa sulla riga che USCIREBBE: una riga
        // che non verra' spezzata non ha nessuno spazio di rottura.
        val a72 = "0123456789 ".repeat(6) + "012345"
        assertEquals(72, a72.length)
        assertEquals(a72, avvolgiFlowed(a72))
        val a71 = "0123456789 ".repeat(6) + "01234"
        assertEquals(71, a71.length)
        assertEquals(a71, avvolgiFlowed(a71))
        // e una che NON ci sta si spezza ancora
        assertTrue(avvolgiFlowed("0123456789 ".repeat(6) + "0123456").split("\n").size == 2)
    }

    @Test
    fun `una riga citata che ci sta non viene toccata`() {
        // qui la colonna della protezione va contata: e' sempre presente sulle
        // righe citate, ed e' proprio il caso in cui un conto sbagliato di una
        // sola colonna spezzerebbe per niente.
        val q = "> " + "0123456789 ".repeat(6) + "012"
        assertTrue(q.length <= 72)
        assertEquals(q, avvolgiFlowed(q))
    }

    @Test
    fun `una riga corta resta fissa e non continua`() {
        assertEquals("corta", avvolgiFlowed("corta"))
        assertTrue(!almenoUnaRigaContinua(avvolgiFlowed("corta").split("\n")))
    }

    @Test
    fun `le righe vuote restano separatori di capoverso`() {
        assertEquals(listOf("uno", "", "due"), giro("uno\n\ndue"))
    }

    @Test
    fun `uno spazio finale battuto per sbaglio non unisce due righe`() {
        // Senza la ripulitura della coda queste due righe diventerebbero una.
        assertEquals(listOf("prima", "seconda"), giro("prima \nseconda"))
    }

    @Test
    fun `si dichiara f=f solo se c'e' davvero qualcosa da ricomporre`() {
        // E' il predicato che decide il Content-Type in postArticle, ed e' lo
        // stesso che il lettore usa per fidarsi del formato altrui.
        assertTrue(almenoUnaRigaContinua(avvolgiFlowed(lunga).split("\n")))
        assertTrue(!almenoUnaRigaContinua(avvolgiFlowed("tutte\nrighe\ncorte").split("\n")))
        // e il lettore, su quel corpo, lo userebbe
        assertTrue(usaFlowed(FormatoCorpo(flowed = true, delSp = false), avvolgiFlowed(lunga).split("\n")))
    }
}

/**
 * 1.4 (Dave Royal) — i gruppi di destinazione mostrati in scrittura.
 *
 * `gruppiDestinazione` vive in `CrosspostReply.kt`, che e' un file Compose, ma
 * la funzione e' Kotlin puro e non tocca un solo tipo di Compose: e' lo stesso
 * schema gia' collaudato con `wrapBody` in `ComposeLogic.kt`.
 */
class GruppiDestinazioneTests {

    /**
     * 1.4.5 (§A10) — i gruppi extra con cui una risposta NASCE.
     *
     * La regola stava dentro l'inizializzazione del dialogo crosspost, che la
     * vista a pagine non apre: l'impostazione «rispondi a tutti i gruppi» era
     * accesa e non faceva niente. Ora e' una funzione sola, ed e' qui perche'
     * un difetto che si vede solo con un'impostazione attiva e' esattamente
     * quello che nessuno prova a mano.
     */
    @Test
    fun `impostazione spenta - nessun gruppo extra`() {
        assertEquals(
            emptyList<String>(),
            extraPredefinitiCrosspost(
                allGroups = listOf("it.test", "alt.test", "misc.test"),
                currentGroup = "it.test",
                tuttiIGruppi = false,
            ),
        )
    }

    @Test
    fun `impostazione accesa - gli altri gruppi, senza il corrente`() {
        assertEquals(
            listOf("alt.test", "misc.test"),
            extraPredefinitiCrosspost(
                allGroups = listOf("it.test", "alt.test", "misc.test"),
                currentGroup = "it.test",
                tuttiIGruppi = true,
            ),
        )
    }

    @Test
    fun `il cap vale anche sul valore di partenza`() {
        // CROSSPOST_MAX_GROUPS = 3, quindi corrente + 2 extra al massimo.
        assertEquals(
            listOf("a.test", "b.test"),
            extraPredefinitiCrosspost(
                allGroups = listOf("it.test", "a.test", "b.test", "c.test", "d.test"),
                currentGroup = "it.test",
                tuttiIGruppi = true,
            ),
        )
    }

    @Test
    fun `un articolo non crospostato non aggiunge niente`() {
        assertEquals(
            emptyList<String>(),
            extraPredefinitiCrosspost(
                allGroups = listOf("it.test"),
                currentGroup = "it.test",
                tuttiIGruppi = true,
            ),
        )
    }

    @Test
    fun `un gruppo solo resta un gruppo solo`() {
        assertEquals(listOf("it.test"), gruppiDestinazione("it.test", emptyList()))
    }

    @Test
    fun `il gruppo corrente resta il primo`() {
        // Ordine non estetico: per convenzione Usenet il primo gruppo del
        // Newsgroups e' il riferimento della risposta, ed e' quello in cui si
        // sta leggendo.
        assertEquals(
            listOf("it.test", "alt.test", "misc.test"),
            gruppiDestinazione("it.test", listOf("alt.test", "misc.test")),
        )
    }

    @Test
    fun `un extra uguale al corrente non si ripete`() {
        assertEquals(
            listOf("it.test", "alt.test"),
            gruppiDestinazione("it.test", listOf("it.test", "alt.test")),
        )
    }

    @Test
    fun `i duplicati fra gli extra spariscono conservando l'ordine`() {
        assertEquals(
            listOf("it.test", "alt.test", "misc.test"),
            gruppiDestinazione("it.test", listOf("alt.test", "misc.test", "alt.test")),
        )
    }

    @Test
    fun `i vuoti non diventano virgole`() {
        // Una stringa vuota in mezzo produrrebbe "a, , b" sotto gli occhi
        // dell'utente: peggio che non mostrare nulla.
        assertEquals(
            listOf("it.test", "alt.test"),
            gruppiDestinazione("it.test", listOf("", "   ", "alt.test")),
        )
    }

    @Test
    fun `gli spazi ai bordi si tolgono`() {
        assertEquals(
            listOf("it.test", "alt.test"),
            gruppiDestinazione("  it.test ", listOf(" alt.test")),
        )
    }

    @Test
    fun `un gruppo corrente non ancora risolto non inventa una voce`() {
        // Durante il caricamento groupName e' "": la lista deve contenere solo
        // gli extra, non una riga vuota in testa.
        assertEquals(listOf("alt.test"), gruppiDestinazione("", listOf("alt.test")))
    }

    @Test
    fun `nessun gruppo da nessuna parte da' lista vuota`() {
        assertTrue(gruppiDestinazione("", emptyList()).isEmpty())
    }
}

/**
 * 1.4 — `Followup-To` (GNKSA, criterio che mancava).
 *
 * E' la regola che decide DOVE finisce un messaggio pubblico: si prova, non si
 * collauda tre volte a mano e poi si spera.
 */
class FollowupToTests {

    private fun art(vararg righe: String) = righe.joinToString("\n")

    @Test
    fun `senza header non si decide niente`() {
        val h = art("From: tizio@example.com", "Newsgroups: it.test", "Subject: prova")
        assertTrue(followupTo(h).isEmpty())
        assertTrue(destinazioneRisposta(h) is DestinazioneRisposta.Nessuna)
    }

    @Test
    fun `headersRaw nullo non esplode`() {
        assertTrue(followupTo(null).isEmpty())
        assertTrue(destinazioneRisposta(null) is DestinazioneRisposta.Nessuna)
    }

    @Test
    fun `un gruppo solo`() {
        val h = art("Newsgroups: it.test", "Followup-To: it.discussioni.altro")
        assertEquals(listOf("it.discussioni.altro"), followupTo(h))
        val d = destinazioneRisposta(h)
        assertTrue(d is DestinazioneRisposta.Gruppi)
        assertEquals(listOf("it.discussioni.altro"), (d as DestinazioneRisposta.Gruppi).gruppi)
    }

    @Test
    fun `piu' gruppi separati da virgola, spazi tolti`() {
        val h = art("Followup-To: it.uno, it.due ,it.tre")
        assertEquals(listOf("it.uno", "it.due", "it.tre"), followupTo(h))
    }

    @Test
    fun `poster chiede risposte private`() {
        val h = art("Newsgroups: it.test", "Followup-To: poster")
        assertTrue(destinazioneRisposta(h) is DestinazioneRisposta.SoloAutore)
    }

    @Test
    fun `poster non e' sensibile alle maiuscole`() {
        assertTrue(destinazioneRisposta(art("Followup-To: Poster")) is DestinazioneRisposta.SoloAutore)
        assertTrue(destinazioneRisposta(art("Followup-To: POSTER")) is DestinazioneRisposta.SoloAutore)
    }

    @Test
    fun `poster vince anche in una forma mista`() {
        // "poster,it.test" e' malformato (la RFC 5536 non lo prevede), ma se
        // l'autore ha scritto "poster" da qualche parte la sua intenzione e'
        // quella, e la lettura prudente e' rispettarla.
        val d = destinazioneRisposta(art("Followup-To: poster, it.test"))
        assertTrue(d is DestinazioneRisposta.SoloAutore)
    }

    // --- 1.4.6 (par. A1, punto 4): la richiesta dell'autore si ritira --------
    //
    // Nasce dal difetto A2 di Dave Royal: l'avviso leggeva il valore grezzo e la
    // riga dei gruppi quello filtrato, cosi' due righe della stessa schermata
    // potevano smentirsi. Ora la regola sta in un posto solo, ed e' pura: queste
    // sono le prove che prima non si potevano scrivere.

    @Test
    fun `senza scelta esplicita la richiesta dell'autore resta in vigore`() {
        val d = destinazioneRisposta(art("Newsgroups: it.test", "Followup-To: uk.test"))
        val v = destinazioneAncoraInVigore(d, sceltaEsplicita = false)
        assertTrue(v is DestinazioneRisposta.Gruppi)
        assertEquals(listOf("uk.test"), (v as DestinazioneRisposta.Gruppi).gruppi)
    }

    @Test
    fun `una scelta esplicita ritira la richiesta dell'autore`() {
        val d = destinazioneRisposta(art("Newsgroups: it.test", "Followup-To: uk.test"))
        assertTrue(destinazioneAncoraInVigore(d, sceltaEsplicita = true) is DestinazioneRisposta.Nessuna)
    }

    @Test
    fun `poster non si ritira mai`() {
        // Non indica gruppi alternativi: dice che l'autore vuole una risposta
        // PRIVATA. Scegliere dei gruppi non e' una risposta a quella richiesta,
        // e l'avviso porta con se' l'indirizzo, che resta utile.
        val d = destinazioneRisposta(art("Followup-To: poster"))
        assertTrue(destinazioneAncoraInVigore(d, sceltaEsplicita = true) is DestinazioneRisposta.SoloAutore)
    }

    @Test
    fun `senza Followup-To una scelta esplicita non cambia niente`() {
        // La non-regressione che conta: e' il caso di tutti i giorni.
        val d = destinazioneRisposta(art("Newsgroups: it.test"))
        assertTrue(destinazioneAncoraInVigore(d, sceltaEsplicita = true) is DestinazioneRisposta.Nessuna)
        assertTrue(destinazioneAncoraInVigore(d, sceltaEsplicita = false) is DestinazioneRisposta.Nessuna)
    }

    // --- 1.4.6 (par. A1, piano C): il selettore e la richiesta dell'autore ---
    //
    // Prova 9B di Dave Royal: crosspost it.test + uk.test con Followup-To a
    // uk.test, e il selettore «does not give the option of uk.test only».

    @Test
    fun `senza Followup-To il gruppo obbligatorio e' quello di lettura`() {
        // LA PROPRIETA' CHE RENDE SICURA LA MODIFICA: sul caso di tutti i giorni
        // le due funzioni sono l'IDENTITA'. Niente cambia, e non per fortuna.
        val d = destinazioneRisposta(art("Newsgroups: it.test"))
        assertEquals("it.test", gruppoObbligatorio("it.test", d))
        assertEquals(listOf("it.test", "uk.test"),
            gruppiOffertiNelSelettore(d, listOf("it.test", "uk.test")))
    }

    @Test
    fun `col Followup-To il gruppo obbligatorio e' quello chiesto dall'autore`() {
        val d = destinazioneRisposta(art("Newsgroups: it.test, uk.test", "Followup-To: uk.test"))
        assertEquals("uk.test", gruppoObbligatorio("it.test", d))
    }

    @Test
    fun `poster non sposta il gruppo obbligatorio`() {
        // `poster` non nomina gruppi: non c'e' niente da ancorare, e la risposta
        // pubblica resta dove si sta leggendo.
        val d = destinazioneRisposta(art("Newsgroups: it.test", "Followup-To: poster"))
        assertEquals("it.test", gruppoObbligatorio("it.test", d))
    }

    @Test
    fun `il selettore offre il followup per primo e senza ripetizioni`() {
        val d = destinazioneRisposta(art("Newsgroups: it.test, uk.test", "Followup-To: uk.test"))
        // uk.test e' in tutte e due le liste e deve comparire UNA volta sola, in
        // testa: il primo dell'elenco e' il gruppo obbligatorio.
        assertEquals(listOf("uk.test", "it.test"),
            gruppiOffertiNelSelettore(d, listOf("it.test", "uk.test")))
    }

    @Test
    fun `il selettore offre anche un gruppo di followup fuori dall'originale`() {
        // RFC 5536 par. 3.2.6: il Followup-To NON deve essere un sottoinsieme di
        // Newsgroups. Il caso 3 di Dave e' legittimo per norma, e prima questo
        // gruppo non compariva fra le opzioni.
        val d = destinazioneRisposta(art("Newsgroups: it.test", "Followup-To: uk.test"))
        assertEquals(listOf("uk.test", "it.test"), gruppiOffertiNelSelettore(d, listOf("it.test")))
    }

    @Test
    fun `una preferenza generica non scavalca la richiesta dell'autore`() {
        // IL DIFETTO, trovato da Marco il 1 ottobre 2026 su uno screenshot: con
        // «rispondi sempre a tutti i gruppi» attivo, il selettore nasceva con
        // it.test GIA' SPUNTATO su un Followup-To a uk.test — e confermando la
        // risposta sarebbe uscita su due gruppi invece che su quello chiesto.
        val autore = gruppiChiestiDallAutore(
            destinazioneRisposta(art("Newsgroups: it.test, uk.test", "Followup-To: uk.test")))
        assertEquals(listOf("uk.test"), autore)
        assertEquals(
            emptyList<String>(),
            extraPredefinitiRisposta(
                allGroups = listOf("uk.test", "it.test"),
                currentGroup = "uk.test",
                gruppiAutore = autore,
                tuttiIGruppi = true,
            ),
        )
    }

    @Test
    fun `senza richiesta dell'autore il predefinito e' quello di sempre`() {
        // Identita' sul caso di tutti i giorni: l'impostazione di Dave continua
        // a valere per i crosspost veri, che e' il caso per cui e' nata.
        val attesi = extraPredefinitiCrosspost(
            allGroups = listOf("it.test", "uk.test"), currentGroup = "it.test", tuttiIGruppi = true)
        assertEquals(
            attesi,
            extraPredefinitiRisposta(
                allGroups = listOf("it.test", "uk.test"),
                currentGroup = "it.test",
                gruppiAutore = emptyList(),
                tuttiIGruppi = true,
            ),
        )
        assertEquals(listOf("uk.test"), attesi)
    }

    @Test
    fun `prova 9A - il pulsante del selettore deve comparire`() {
        // LA CONDIZIONE DEL PULSANTE, non solo il contenuto del selettore.
        // Il Test 9A di Dave sta su UN gruppo solo con Followup-To altrove:
        // guardando i soli Newsgroups dell'originale si conta 1, il pulsante non
        // si disegna, e l'avviso rimanda a un selettore che non esiste.
        val d = destinazioneRisposta(art("Newsgroups: it.test", "Followup-To: uk.test"))
        assertEquals(1, crosspostGroups(art("Newsgroups: it.test")).size)
        assertTrue(gruppiOffertiNelSelettore(d, listOf("it.test")).size > 1)
    }

    @Test
    fun `senza Followup-To il pulsante compare esattamente come prima`() {
        // Non-regressione sulla condizione: un messaggio normale non deve
        // guadagnare un pulsante che prima non aveva.
        val d = destinazioneRisposta(art("Newsgroups: it.test"))
        assertEquals(1, gruppiOffertiNelSelettore(d, listOf("it.test")).size)
        assertEquals(2, gruppiOffertiNelSelettore(d, listOf("it.test", "uk.test")).size)
    }

    @Test
    fun `prova 9B - uk_test soltanto adesso e' esprimibile`() {
        // E' il difetto 3, per intero: obbligatorio uk.test, nessun extra
        // spuntato, destinazione finale uk.test e basta.
        val d = destinazioneRisposta(art("Newsgroups: it.test, uk.test", "Followup-To: uk.test"))
        val obbligatorio = gruppoObbligatorio("it.test", d)
        assertEquals(listOf("uk.test"), gruppiDestinazione(obbligatorio, emptyList()))
        // ...e «tutti e due» resta raggiungibile spuntando it.test.
        assertEquals(listOf("uk.test", "it.test"), gruppiDestinazione(obbligatorio, listOf("it.test")))
    }

    @Test
    fun `il nome dell'header non e' sensibile alle maiuscole`() {
        assertEquals(listOf("it.altro"), followupTo(art("FOLLOWUP-TO: it.altro")))
    }

    @Test
    fun `un header vuoto equivale ad assente`() {
        val h = art("Newsgroups: it.test", "Followup-To: ")
        assertTrue(followupTo(h).isEmpty())
        assertTrue(destinazioneRisposta(h) is DestinazioneRisposta.Nessuna)
    }

    @Test
    fun `Newsgroups e Followup-To non si confondono`() {
        val h = art("Newsgroups: it.uno,it.due", "Followup-To: it.tre")
        assertEquals(listOf("it.uno", "it.due"), crosspostGroups(h))
        assertEquals(listOf("it.tre"), followupTo(h))
    }
}

/**
 * 1.4 — la regola dei tre passi sul charset del corpo.
 *
 * I SETTE CASI SONO QUELLI MISURATI il 15 settembre 2026 dopo la domanda di
 * Adam H. Kerman (NeReO_bug_charset_corpo.md §2): allora cinque su sette
 * perdevano il testo. Entrano qui tali e quali, piu' due casi di CONTROLLO che
 * servono a dimostrare che la cura non e' peggiore del male — un charset non
 * europeo dichiarato correttamente, e l'ASCII puro.
 */
class CharsetCorpoTests {

    private val atteso = "perch\u00e9 \u00e8 cos\u00ec"
    private val latin9: Charset = Charset.forName("ISO-8859-15")
    private val latin1: Charset = Charset.forName("ISO-8859-1")
    private val byteLatin1 = atteso.toByteArray(latin9)
    private val byteUtf8 = atteso.toByteArray(Charsets.UTF_8)

    @Test
    fun `1 - nessun Content-Type (us-ascii implicito), byte Latin-1`() {
        assertEquals(atteso, MimeUtil.decodificaCorpoByte(byteLatin1, Charsets.US_ASCII))
    }

    @Test
    fun `2 - nessun Content-Type, byte UTF-8`() {
        assertEquals(atteso, MimeUtil.decodificaCorpoByte(byteUtf8, Charsets.US_ASCII))
    }

    @Test
    fun `3 - Content-Type senza charset, byte Latin-1`() {
        assertEquals(atteso, MimeUtil.decodificaCorpoByte(byteLatin1, Charsets.UTF_8))
    }

    @Test
    fun `4 - dichiara latin-1 ma e' UTF-8 (il mojibake che usciva timbrato)`() {
        assertEquals(atteso, MimeUtil.decodificaCorpoByte(byteUtf8, latin1))
    }

    @Test
    fun `5 - dichiara UTF-8 ma e' latin-1`() {
        assertEquals(atteso, MimeUtil.decodificaCorpoByte(byteLatin1, Charsets.UTF_8))
    }

    @Test
    fun `6 - dichiarazione corretta (latin-9) resta corretta`() {
        assertEquals(atteso, MimeUtil.decodificaCorpoByte(byteLatin1, latin9))
    }

    @Test
    fun `7 - dichiarazione corretta (UTF-8) resta corretta`() {
        assertEquals(atteso, MimeUtil.decodificaCorpoByte(byteUtf8, Charsets.UTF_8))
    }

    @Test
    fun `8 - un charset NON europeo dichiarato bene non viene scavalcato`() {
        // Il caso che vieta la scorciatoia "usa sempre l'euristica": qui la
        // dichiarazione e' giusta e Latin-9 produrrebbe spazzatura.
        val koi8 = Charset.forName("KOI8-R")
        val ru = "\u043f\u0440\u0438\u0432\u0435\u0442"
        assertEquals(ru, MimeUtil.decodificaCorpoByte(ru.toByteArray(koi8), koi8))
    }

    @Test
    fun `9 - ASCII puro non prende la strada dell'euristica`() {
        // Senza la richiesta di una sequenza MULTI-byte, l'ASCII entrerebbe nel
        // passo 1 e scavalcherebbe una dichiarazione giusta senza motivo.
        val asc = "plain ascii text"
        assertEquals(asc, MimeUtil.decodificaCorpoByte(asc.toByteArray(Charsets.US_ASCII), Charsets.US_ASCII))
    }

    @Test
    fun `senza dichiarazione si ripiega come fanno le intestazioni`() {
        assertEquals(atteso, MimeUtil.decodificaCorpoByte(byteLatin1, null))
    }
}

/**
 * 1.4.4 — il filtro sul lettore di news. Tre cose, e la seconda e' quella che
 * conta davvero.
 *
 * I valori di prova sono fittizi di proposito: un caso di prova non e' il posto
 * dove annotare contro chi si e' impostata una regola.
 */
class LettoreDiNewsTest {

    private fun intestazioni(vararg righe: String): String = righe.joinToString("\n")

    @Test
    fun `unisce le tre intestazioni storiche e ignora le altre`() {
        val h = intestazioni(
            "From: tizio@example.org",
            "Subject: prova",
            "User-Agent: EsempioReader/1.2 (Windows NT 10.0)",
        )
        assertEquals("EsempioReader/1.2 (Windows NT 10.0)", MimeUtil.lettoreDiNews(h))
        val vecchio = intestazioni("X-Newsreader: MesNews/1.08.06.00", "Subject: prova")
        assertEquals("MesNews/1.08.06.00", MimeUtil.lettoreDiNews(vecchio))
        val entrambe = intestazioni("User-Agent: Pan/0.146", "X-Mailer: Forte Agent")
        assertEquals("Pan/0.146 | Forte Agent", MimeUtil.lettoreDiNews(entrambe))
    }

    @Test
    fun `nessuna delle tre dichiarata da null, non stringa vuota`() {
        assertNull(MimeUtil.lettoreDiNews(intestazioni("From: x@y", "Subject: z")))
        assertNull(MimeUtil.lettoreDiNews(null))
        assertNull(MimeUtil.lettoreDiNews(""))
    }

    /**
     * IL CASO CHE CONTA. Le intestazioni complete arrivano col corpo, quindi
     * per la stragrande maggioranza degli articoli il valore e' null: la
     * regola deve essere SALTATA senza toccare il punteggio, non valutata
     * contro una stringa vuota. E' la stessa semantica di BODY, e se si
     * rompesse un giorno il sintomo sarebbe "il filtro colpisce tutti".
     */
    @Test
    fun `regola non valutabile quando il lettore non e' noto`() {
        val engine = FilterEngine(
            listOf(
                FilterRule(
                    field = FilterField.NEWSREADER,
                    matchType = MatchType.CONTAINS,
                    pattern = "EsempioReader/1.2",
                    score = -10000,
                ),
            ),
        )
        val senzaIntestazioni = ScorableArticle("tizio@example.org", "Ciao", "<x@y>", "it.test")
        assertEquals(ScoreResult.Action.NORMAL, engine.evaluate(senzaIntestazioni, "it.test").action)

        val conIntestazioni = senzaIntestazioni.copy(
            newsreader = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) EsempioReader/1.2",
        )
        assertEquals(ScoreResult.Action.KILL, engine.evaluate(conIntestazioni, "it.test").action)

        // Lo stesso programma, versione diversa: non deve cadere nella rete.
        val altroLettore = senzaIntestazioni.copy(newsreader = "Mozilla/5.0 EsempioReader/9.0")
        assertEquals(ScoreResult.Action.NORMAL, engine.evaluate(altroLettore, "it.test").action)
    }

    @Test
    fun `il kill-file conserva una regola sul lettore`() {
        val regole = listOf(
            FilterRule(
                name = "Esempio",
                field = FilterField.NEWSREADER,
                matchType = MatchType.CONTAINS,
                pattern = "EsempioReader/1.2",
                score = -9999,
                followup = FollowupMode.SUBTREE,
            ),
        )
        val riletto = KillFile.parse(KillFile.export(regole))
        assertEquals(1, riletto.size)
        assertEquals(FilterField.NEWSREADER, riletto[0].field)
        assertEquals("EsempioReader/1.2", riletto[0].pattern)
        assertEquals(FollowupMode.SUBTREE, riletto[0].followup)
    }
}

/**
 * 1.4.4 (§A7 e la correzione del 24 settembre) — il criterio «questo campo
 * identifica un autore».
 *
 * Esiste un test apposta perche' quel criterio era scritto DUE volte
 * nell'editor delle regole — una per disegnare la casella «estendi alle
 * risposte» e una per salvarla — e quando e' arrivato un campo nuovo sono
 * rimaste indietro tutte e due: la casella non compariva e il salvataggio
 * azzerava in silenzio la propagazione. Ora la risposta sta in un posto solo,
 * e questo test la inchioda.
 */
class CampoIdentificaAutoreTest {

    @Test
    fun `da e lettore identificano un autore, gli altri no`() {
        assertTrue(FilterField.FROM.identificaUnAutore)
        assertTrue(FilterField.NEWSREADER.identificaUnAutore)
        assertFalse(FilterField.SUBJECT.identificaUnAutore)
        assertFalse(FilterField.MESSAGE_ID.identificaUnAutore)
        assertFalse(FilterField.XPOST.identificaUnAutore)
        assertFalse(FilterField.BODY.identificaUnAutore)
        assertFalse(FilterField.REFERENCES.identificaUnAutore)
    }

    @Test
    fun `la versione per nome regge anche un campo sconosciuto`() {
        assertTrue(campoIdentificaUnAutore("FROM"))
        assertTrue(campoIdentificaUnAutore("NEWSREADER"))
        assertFalse(campoIdentificaUnAutore("SUBJECT"))
        // Un valore mai visto non deve far cadere la schermata delle regole:
        // risponde "no" e si va avanti.
        assertFalse(campoIdentificaUnAutore("CAMPO_CHE_NON_ESISTE"))
        assertFalse(campoIdentificaUnAutore(""))
    }
}

/**
 * 1.4.5 (par. B9, Adam H. Kerman) — il file .eml deve dire la verita' su se'
 * stesso.
 *
 * PERCHE' QUESTI TEST ESISTONO. NeReO salva il corpo GIA' DECODIFICATO, mentre
 * le intestazioni restano grezze: possono ancora dichiarare un charset e una
 * codifica di trasporto che al corpo non si applicano piu'. Condividere quel
 * testo non fa danno; SCRIVERLO IN UN FILE che un client di posta ri-decodifica
 * sì. Il difetto non si vedrebbe mai su un messaggio in inglese senza accenti,
 * ed e' esattamente il motivo per cui va provato qui invece che a occhio.
 */
class ArticoloEmlTests {

    @Test
    fun `il charset dichiarato e' quello che scriviamo davvero`() {
        val headers = """
            From: tizio@example.org
            Subject: prova
            Content-Type: text/plain; charset=ISO-8859-15
            Content-Transfer-Encoding: quoted-printable
        """.trimIndent()
        val eml = articoloEml(headers, "perche' e' cosi'")
        assertFalse("il charset originale non deve sopravvivere", eml.contains("ISO-8859-15"))
        assertFalse("la codifica originale non deve sopravvivere", eml.contains("quoted-printable"))
        assertTrue(eml.contains("Content-Type: text/plain; charset=UTF-8"))
        assertTrue(eml.contains("Content-Transfer-Encoding: 8bit"))
    }

    @Test
    fun `le altre intestazioni non si toccano`() {
        val headers = "From: tizio@example.org\nSubject: prova\nMessage-ID: <a@b>"
        val eml = articoloEml(headers, "corpo")
        assertTrue(eml.contains("From: tizio@example.org"))
        assertTrue(eml.contains("Subject: prova"))
        assertTrue(eml.contains("Message-ID: <a@b>"))
    }

    @Test
    fun `una intestazione ripiegata se ne va con le sue continuazioni`() {
        // RFC 5322 par. 2.2.3: le righe di continuazione iniziano con spazio o
        // tabulazione. Togliendo la Content-Type senza portarle via, resterebbe
        // nel file una riga orfana che rende illeggibile tutto il blocco.
        val headers = "Subject: prova\n" +
            "Content-Type: multipart/mixed;\n" +
            "\tboundary=\"qualcosa\";\n" +
            " charset=ISO-8859-1\n" +
            "Date: Sun, 28 Sep 2026 10:00:00 +0200"
        val eml = articoloEml(headers, "corpo")
        assertFalse(eml.contains("boundary"))
        assertFalse(eml.contains("ISO-8859-1"))
        assertTrue("una intestazione dopo quella tolta non deve sparire", eml.contains("Date: Sun"))
        assertTrue(eml.contains("Subject: prova"))
    }

    @Test
    fun `intestazioni e corpo restano separati da una riga vuota sola`() {
        val eml = articoloEml("Subject: prova", "prima riga\nseconda riga")
        val separatore = eml.indexOf("\n\n")
        assertTrue("manca la riga vuota di separazione", separatore > 0)
        assertEquals("prima riga\nseconda riga", eml.substring(separatore + 2))
    }

    @Test
    fun `senza intestazioni il file resta comunque un messaggio valido`() {
        // Corpo mai scaricato per intero: meglio un .eml con le sole
        // intestazioni di codifica che un file che non si apre.
        val eml = articoloEml(null, "solo corpo")
        assertTrue(eml.startsWith("Content-Type: text/plain; charset=UTF-8"))
        assertTrue(eml.endsWith("\n\nsolo corpo"))
    }

    @Test
    fun `il nome del file parte dall'oggetto`() {
        assertEquals("Re_ prova.eml", nomeFileEml("Re: prova", "<a@b>"))
    }

    @Test
    fun `senza oggetto si ripiega sul Message-ID`() {
        assertEquals("a@b.eml", nomeFileEml("", "<a@b>"))
    }

    @Test
    fun `i caratteri che i filesystem rifiutano non passano`() {
        val nome = nomeFileEml("""a/b\c:d*e?f"g<h>i|j""", "<x@y>")
        assertFalse(nome.dropLast(4).any { it in """\/:*?"<>|""" })
        assertTrue(nome.endsWith(".eml"))
    }

    // ===================================================================
    //  1.4.9 (§J3-C1) — IL NOME DI UN MESSAGGIO CONSERVATO
    // ===================================================================
    //
    // La differenza con `nomeFileEml` e' tutta qui: quello finisce in un
    // selettore dove l'utente vede e corregge, questo finisce in una cartella
    // dove i file si accumulano senza che nessuno guardi. Percio' deve essere
    // UNICO, e l'unica cosa unica per definizione e' il Message-ID.

    @Test
    fun `il nome conservato porta oggetto e Message-ID`() {
        assertEquals("Re_ prova__a.eml", nomeFileConserva("Re: prova", "<a@b.invalid>"))
    }

    @Test
    fun `due messaggi con lo stesso oggetto non si sovrascrivono`() {
        // ⚠️ IL RISCHIO (2) DICHIARATO: in una cartella sola, «Re: ciao» ce ne
        // sono dieci. Se questi due nomi coincidessero, conservare il secondo
        // cancellerebbe il primo — e sarebbe il peggior difetto possibile in una
        // funzione che si chiama «conserva».
        val uno = nomeFileConserva("Re: ciao", "<primo@server.invalid>")
        val due = nomeFileConserva("Re: ciao", "<secondo@server.invalid>")
        assertFalse(uno == due)
    }

    @Test
    fun `il dominio del Message-ID non entra nel nome`() {
        // Sarebbe lo stesso per tutti i messaggi di quel server: lunghezza in
        // piu' e zero potere di distinguere.
        assertFalse(nomeFileConserva("prova", "<x@news.example.invalid>").contains("example"))
    }

    @Test
    fun `anche il nome conservato e privo di caratteri vietati`() {
        val nome = nomeFileConserva("""a/b\c:d*e?f"g<h>i|j""", """<p/q:r@s>""")
        assertFalse(nome.dropLast(4).any { it in """\/:*?"<>|""" })
        assertTrue(nome.endsWith(".eml"))
    }

    @Test
    fun `senza oggetto resta il Message-ID, senza doppia sottolineatura`() {
        assertEquals("a.eml", nomeFileConserva("", "<a@b.invalid>"))
    }

    @Test
    fun `un oggetto lunghissimo non produce un nome impossibile`() {
        // I filesystem si fermano intorno ai 255 byte: un oggetto Usenet puo'
        // essere molto piu' lungo, e un nome rifiutato dal provider si vedrebbe
        // come un salvataggio che non avviene.
        val nome = nomeFileConserva("x".repeat(400), "<abcdefghijklmnopqrstuvwxyz0123456789@b.invalid>")
        assertTrue(nome.length <= 100)
        assertTrue(nome.endsWith(".eml"))
    }
}

/**
 * 1.4.6 (§A12) — gli id dell'header References, con il tetto del 1.2.12.
 *
 * PERCHE' SONO QUI. Questa funzione e' nata perche' servisse anche al sync, per
 * far ereditare il «seguito» alle risposte nuove. Il pezzo che va protetto con
 * un test non e' l'estrazione — e' il TAGLIO: il tetto di 100 nasce dall'audit
 * di sicurezza del 1.2.12 (un References da decine di migliaia di id fa saltare
 * il threading), e adesso decide anche quanti parametri finiscono in una query
 * SQL. Un tetto che si rompe in silenzio riapre due problemi invece di uno.
 */
class IdNelleReferencesTests {

    @Test
    fun `estrae gli id nell'ordine dichiarato`() {
        assertEquals(
            listOf("<a@x>", "<b@x>", "<c@x>"),
            Threader.idNelleReferences("<a@x> <b@x> <c@x>"),
        )
    }

    @Test
    fun `references vuoto non da' niente`() {
        assertEquals(emptyList<String>(), Threader.idNelleReferences(""))
        assertEquals(emptyList<String>(), Threader.idNelleReferences("   "))
    }

    @Test
    fun `il padre diretto e' sempre l'ultimo`() {
        // E' quello che decide dove si appende il messaggio: se il taglio lo
        // perdesse, il threading si romperebbe proprio sui thread lunghi.
        val refs = (1..500).joinToString(" ") { "<m$it@x>" }
        assertEquals("<m500@x>", Threader.idNelleReferences(refs).last())
    }

    @Test
    fun `oltre il tetto si tengono il primo e la coda`() {
        val refs = (1..500).joinToString(" ") { "<m$it@x>" }
        val tagliati = Threader.idNelleReferences(refs)
        assertEquals(100, tagliati.size)
        // la radice della discussione: e' lei a tenere insieme il thread
        assertEquals("<m1@x>", tagliati.first())
        // e poi gli ultimi 99, non i primi 99
        assertEquals("<m402@x>", tagliati[1])
        assertFalse(tagliati.contains("<m2@x>"))
    }

    @Test
    fun `sotto il tetto non si taglia niente`() {
        val refs = (1..100).joinToString(" ") { "<m$it@x>" }
        val tenuti = Threader.idNelleReferences(refs)
        assertEquals(100, tenuti.size)
        assertEquals("<m2@x>", tenuti[1])
    }

    @Test
    fun `un id malformato non inghiotte il resto della riga`() {
        // La regex stretta (niente spazi, niente < annidati) e' il motivo per
        // cui questa funzione sta in Threader e non e' una copia della
        // <[^>]+> usata altrove nell'app.
        assertEquals(
            listOf("<b@x>"),
            Threader.idNelleReferences("<a non chiuso <b@x>"),
        )
    }
}


/**
 * 1.4.6 (§B4) — SPOSTARSI FRA I MESSAGGI, senza un dispositivo.
 *
 * Queste funzioni vivevano dentro ArticleScreen, mescolate allo stato della
 * schermata, e per questo non erano provabili e nei pannelli non c'erano.
 * Estratte in `ui/screens/NavigazioneLettura.kt` come funzioni pure, i casi
 * limite si possono finalmente scrivere qui — a cominciare da quello che fa
 * piu' danno, l'articolo che non sta nella lista.
 */
class NavigazioneLetturaTest {

    private fun articolo(id: Long, letto: Boolean) = Article(
        id = id,
        groupId = 1,
        number = id,
        messageId = "<$id@example.org>",
        subject = "oggetto $id",
        fromField = "tizio@example.org",
        dateEpoch = id,
        read = letto,
    )

    private fun voce(id: Long, inizio: Boolean = false, letto: Boolean = true) =
        ThreadItem(depth = 0, article = articolo(id, letto), threadStart = inizio)

    /** Due discussioni: [1,2,3] e [4,5]. Tutti letti se non detto altrimenti. */
    private val lista = listOf(
        voce(1, inizio = true), voce(2), voce(3),
        voce(4, inizio = true), voce(5),
    )

    @Test
    fun `avanti resta dentro la discussione`() {
        assertEquals(2L, idMessaggioSuccessivo(lista, 0))
        assertNull(idMessaggioSuccessivo(lista, 2))   // ultimo della prima
        assertEquals(5L, idMessaggioSuccessivo(lista, 3))
    }

    @Test
    fun `indietro resta dentro la discussione`() {
        assertEquals(1L, idMessaggioPrecedente(lista, 1))
        assertNull(idMessaggioPrecedente(lista, 0))   // primo in assoluto
        assertNull(idMessaggioPrecedente(lista, 3))   // primo della seconda
    }

    /**
     * IL CASO CHE FA PIU' DANNO. Con l'articolo corrente fuori dalla lista la
     * posizione vale -1: un `getOrNull(posizione + 1)` darebbe il PRIMO
     * elemento, cioe' un salto silenzioso all'inizio del gruppo che sembra un
     * comando riuscito. Tutte le funzioni devono dire "niente".
     */
    @Test
    fun `posizione sconosciuta non fa saltare al primo`() {
        assertNull(idMessaggioSuccessivo(lista, -1))
        assertNull(idMessaggioPrecedente(lista, -1))
        assertNull(idPrimoNonLettoDopo(lista, -1))
        assertFalse(esistonoNonLettiDopo(lista, -1))
        assertNull(idDiscussioneSuccessiva(lista, 999L))
        assertNull(idDiscussionePrecedente(lista, 999L))
        assertNull(idDiscussioneSuccessiva(lista, null))
    }

    @Test
    fun `il primo non letto salta i letti e attraversa le discussioni`() {
        val misto = listOf(
            voce(1, inizio = true), voce(2), voce(3, letto = false),
            voce(4, inizio = true, letto = false),
        )
        assertEquals(3L, idPrimoNonLettoDopo(misto, 0))
        assertEquals(4L, idPrimoNonLettoDopo(misto, 2))
        assertTrue(esistonoNonLettiDopo(misto, 0))
        assertNull(idPrimoNonLettoDopo(misto, 3))     // dopo l'ultimo, niente
        assertFalse(esistonoNonLettiDopo(misto, 3))
    }

    @Test
    fun `i salti fra discussioni vanno all'inizio, non al messaggio accanto`() {
        assertEquals(4L, idDiscussioneSuccessiva(lista, 2L))
        assertNull(idDiscussioneSuccessiva(lista, 5L))
        // da meta' della seconda si torna all'inizio della PRIMA, non della
        // propria: il doppio passo esiste per questo.
        assertEquals(1L, idDiscussionePrecedente(lista, 5L))
        assertNull(idDiscussionePrecedente(lista, 2L))
    }

    // ---- 1.6: tasto Avanti ----

    private fun misto(vararg nonLetti: Long) = listOf(
        voce(1, inizio = true, letto = 1L !in nonLetti), voce(2, letto = 2L !in nonLetti),
        voce(3, letto = 3L !in nonLetti), voce(4, inizio = true, letto = 4L !in nonLetti),
        voce(5, letto = 5L !in nonLetti),
    )

    @Test
    fun `avanti - nella stessa discussione, poi nella successiva`() {
        assertEquals(3L, idNonLettoNelGruppo(misto(3, 5), 1))
        assertEquals(5L, idNonLettoNelGruppo(misto(3, 5), 2))
    }

    @Test
    fun `avanti - i non letti sopra la posizione non si saltano`() {
        assertEquals(1L, idNonLettoNelGruppo(misto(1), 4))
        // dopo vengono prima di quelli sopra
        assertEquals(5L, idNonLettoNelGruppo(misto(1, 5), 3))
    }

    @Test
    fun `avanti - il messaggio corrente non e' mai la destinazione`() {
        assertNull(idNonLettoNelGruppo(misto(3), 2))
    }

    @Test
    fun `avanti - gruppo finito`() {
        assertNull(idNonLettoNelGruppo(lista, 0))
    }

    @Test
    fun `avanti - messaggio fuori sequenza riparte dal primo non letto`() {
        assertEquals(2L, idNonLettoNelGruppo(misto(2, 4), -1))
        assertNull(idNonLettoNelGruppo(lista, -1))
    }

    private val gruppi = listOf(
        GruppoDaLeggere(10, 0), GruppoDaLeggere(20, 3), GruppoDaLeggere(30, 0), GruppoDaLeggere(40, 1),
    )

    @Test
    fun `avanti - il gruppo dopo con dei non letti, saltando i vuoti`() {
        assertEquals(20L, idGruppoConNonLetti(gruppi, 10))
        assertEquals(40L, idGruppoConNonLetti(gruppi, 20))
        assertEquals(40L, idGruppoConNonLetti(gruppi, 30))
    }

    @Test
    fun `avanti - dopo l'ultimo gruppo si riparte dall'inizio`() {
        assertEquals(20L, idGruppoConNonLetti(gruppi, 40))
    }

    @Test
    fun `avanti - il gruppo corrente non e' mai la destinazione`() {
        assertNull(idGruppoConNonLetti(listOf(GruppoDaLeggere(1, 5)), 1))
        assertNull(idGruppoConNonLetti(listOf(GruppoDaLeggere(1, 5), GruppoDaLeggere(2, 0)), 1))
    }

    @Test
    fun `avanti - un contatore falso non fa girare il tasto a vuoto`() {
        assertEquals(40L, idGruppoConNonLetti(gruppi, 10, scartati = setOf(20)))
        assertNull(idGruppoConNonLetti(gruppi, 10, scartati = setOf(20, 40)))
    }

    @Test
    fun `avanti - gruppo corrente fuori elenco, si parte dal primo`() {
        assertEquals(20L, idGruppoConNonLetti(gruppi, 999))
        assertEquals(20L, idGruppoConNonLetti(gruppi, null))
    }

    @Test
    fun `avanti - niente da leggere in nessun gruppo`() {
        assertNull(idGruppoConNonLetti(listOf(GruppoDaLeggere(1, 0), GruppoDaLeggere(2, 0)), 1))
        assertNull(idGruppoConNonLetti(emptyList(), 1))
    }

    @Test
    fun `i confini della discussione hanno la fine esclusiva`() {
        assertEquals(0 to 3, confiniDiscussione(lista, 1))
        assertEquals(3 to 5, confiniDiscussione(lista, 4))
        // posizione ignota: tutta la lista, cioe' nessun salto
        assertEquals(0 to 5, confiniDiscussione(lista, -1))
    }
}

/**
 * 1.4.6 (par. B6a) — Format=Flowed in LETTURA (RFC 3676).
 *
 * Regole verificate alla fonte: par. 4.1 interpretazione, 4.3 firma, 4.4
 * space-stuffing, 4.5 citazioni. Qui ci sono i casi limite, che e' il motivo
 * per cui `srotolaFlowed` e' una funzione pura senza un import.
 */
class FormatFlowedTests {

    @Test
    fun `una riga che finisce con uno spazio continua, le altre no`() {
        assertEquals(
            listOf("una frase spezzata in due", "e una riga a se'"),
            srotolaFlowed(listOf("una frase ", "spezzata in due", "e una riga a se'")),
        )
    }

    @Test
    fun `il separatore di firma non continua mai, nonostante lo spazio`() {
        // par. 4.3: «is neither fixed nor flowed». Finisce con uno spazio, ma se
        // lo si trattasse come flowed la firma si attaccherebbe al capoverso —
        // ed e' esattamente la trappola che questo paragrafo esiste per evitare.
        assertEquals(
            listOf("saluti", "-- ", "MCSM"),
            srotolaFlowed(listOf("saluti", "-- ", "MCSM")),
        )
    }

    @Test
    fun `la firma si riconosce anche dopo aver tolto le citazioni`() {
        // par. 4.3: il test va fatto prima E dopo aver contato i `>`.
        // Lo spazio dopo il `>` e' lo spazio di PROTEZIONE (par. 4.4) e si
        // toglie: il testo logico e' `>citazione`. La riga della firma viene
        // invece emessa com'era, perche' e' un delimitatore, non contenuto.
        assertEquals(
            listOf(">citazione", "> -- ", ">firma citata"),
            srotolaFlowed(listOf("> citazione", "> -- ", "> firma citata")),
        )
    }

    @Test
    fun `due profondita' diverse non si uniscono mai`() {
        // par. 4.5: anche se la riga precedente continuava, chi riceve «SHOULD
        // treat it as the last line of a paragraph».
        // ⚠️ Lo spazio in coda RESTA: con DelSp assente la RFC dice che «the
        // trailing space is not deleted», ed e' contenuto dell'autore. Il
        // capoverso finisce li' perche' cambia la profondita', non perche'
        // riscriviamo il suo testo — questo progetto non taglia gli spazi
        // altrui, nemmeno quelli invisibili.
        assertEquals(
            listOf(">>prima ", ">seconda"),
            srotolaFlowed(listOf(">>prima ", ">seconda")),
        )
    }

    @Test
    fun `lo spazio di protezione si toglie prima di guardare se la riga continua`() {
        // par. 4.4: «deleted before examining the line further». L'ordine conta:
        // una riga fatta del solo spazio di protezione diventa VUOTA, cioe'
        // fissa — guardandola prima sembrerebbe invece continuare.
        assertEquals(listOf("", "dopo"), srotolaFlowed(listOf(" ", "dopo")))
        // e una riga che comincia con `>` nel testo vero arriva protetta
        assertEquals(listOf(">non e' una citazione"), srotolaFlowed(listOf(" >non e' una citazione")))
    }

    @Test
    fun `DelSp sì toglie lo spazio mentre unisce, DelSp no lo tiene`() {
        assertEquals(listOf("unadue"), srotolaFlowed(listOf("una ", "due"), delSp = true))
        assertEquals(listOf("una due"), srotolaFlowed(listOf("una ", "due"), delSp = false))
    }

    @Test
    fun `il prefisso di citazione resta una volta sola`() {
        assertEquals(listOf(">una frase lunga citata"), srotolaFlowed(listOf(">una frase ", ">lunga citata")))
    }

    @Test
    fun `profondita' di citazione`() {
        assertEquals(0, profonditaCitazione("testo"))
        assertEquals(1, profonditaCitazione(">testo"))
        assertEquals(3, profonditaCitazione(">>>testo"))
    }

    @Test
    fun `il Content-Type dichiara il formato, in qualunque forma`() {
        assertTrue(formatoCorpo("Content-Type: text/plain; charset=UTF-8; format=flowed").flowed)
        assertTrue(formatoCorpo("content-type: TEXT/PLAIN; FORMAT=FLOWED").flowed)
        assertTrue(formatoCorpo("""Content-Type: text/plain; format="flowed"""").flowed)
        assertTrue(formatoCorpo("Content-Type: text/plain; format=flowed; delsp=yes").delSp)
        assertTrue(!formatoCorpo("Content-Type: text/plain; format=flowed").delSp)
    }

    @Test
    fun `un Content-Type andato a capo si legge lo stesso`() {
        // RFC 5322 folding: le continuazioni cominciano con spazio o tab. Senza
        // ricucirle, un `format=flowed` finito sulla riga dopo sembra assente.
        val h = "Subject: prova\nContent-Type: text/plain; charset=UTF-8;\n format=flowed\nFrom: tizio@example.com"
        assertTrue(formatoCorpo(h).flowed)
    }

    @Test
    fun `dichiarato ma non usato non e' flowed`() {
        // MesNews, 1 ottobre 2026: manda `format=flowed` e poi impagina con
        // a-capo rigidi. Per la norma e' valido — tutte righe «fisse» — ma non
        // porta nessuna informazione, e trattarlo come dato faceva buttare via
        // l'euristica che per quei messaggi funzionava.
        val rigide = listOf("Molto strano! io non ho avuto questo", "problema tra la 1.4.3 e la 1.4.4")
        assertTrue(!usaFlowed(FormatoCorpo(flowed = true, delSp = false), rigide))
    }

    @Test
    fun `basta una riga che continua perche' il formato serva`() {
        val miste = listOf("questa continua ", "e questa la chiude", "questa e' a se'")
        assertTrue(usaFlowed(FormatoCorpo(flowed = true, delSp = false), miste))
    }

    @Test
    fun `la firma da sola non fa un corpo flowed`() {
        // Il separatore finisce con uno spazio ma non e' una riga che continua:
        // contarlo farebbe passare per flowed qualunque messaggio firmato.
        val soloFirma = listOf("testo rigido", "-- ", "MCSM")
        assertTrue(!usaFlowed(FormatoCorpo(flowed = true, delSp = false), soloFirma))
    }

    @Test
    fun `senza la dichiarazione non si srotola, anche se qualche riga continua`() {
        // Uno spazio in coda puo' essere un refuso: senza il Content-Type che lo
        // dichiara, non e' un marcatore e non si tocca niente.
        val conSpazi = listOf("una riga con spazio ", "un'altra")
        assertTrue(!usaFlowed(FormatoCorpo(flowed = false, delSp = false), conSpazi))
    }

    @Test
    fun `senza dichiarazione non e' flowed, e il corpo non si tocca`() {
        // La non-regressione che conta: un messaggio che non dichiara niente
        // continua a passare per l'euristica di sempre.
        assertTrue(!formatoCorpo("Content-Type: text/plain; charset=UTF-8").flowed)
        assertTrue(!formatoCorpo(null).flowed)
        assertTrue(!formatoCorpo("From: tizio@example.com").flowed)
    }
}

/**
 * 1.4.6 (par. B13, BIG Umberto) — in quale discussione sta un messaggio.
 *
 * E' la sola parte DECIDENTE di «mostra nell'elenco»: il resto e' scorrimento.
 * Se sbaglia, l'utente viene portato nella discussione di qualcun altro — che
 * e' peggio di non essere portato da nessuna parte.
 */
class DiscussioneDiTests {

    private fun art(id: Long) = Article(
        id = id, groupId = 1L, number = id, messageId = "<m$id@x>", subject = "s",
        fromField = "a@b", dateEpoch = 0L,
    )

    private fun item(id: Long, root: String) =
        ThreadItem(depth = 0, article = art(id), rootKey = root)

    @Test
    fun `trova la discussione del messaggio`() {
        val flat = listOf(item(1, "<r1@x>"), item(2, "<r1@x>"), item(3, "<r2@x>"))
        assertEquals("<r1@x>", discussioneDi(flat, 2))
        assertEquals("<r2@x>", discussioneDi(flat, 3))
    }

    @Test
    fun `un messaggio che non c'e' non inventa una discussione`() {
        // Cancellato, filtrato, scaduto: meglio non portare da nessuna parte che
        // portare altrove. Il chiamante si ferma su `null`, e la guardia contro
        // il ciclo infinito dipende proprio da questo.
        val flat = listOf(item(1, "<r1@x>"))
        assertEquals(null, discussioneDi(flat, 99))
        assertEquals(null, discussioneDi(emptyList(), 1))
    }

    @Test
    fun `una rootKey vuota vale come assente`() {
        // Una riga senza radice non identifica nessuna discussione da aprire:
        // restituire "" farebbe aggiungere una chiave vuota all'insieme delle
        // discussioni aperte, cioe' un elemento che non apre niente e non si
        // toglie piu'.
        assertEquals(null, discussioneDi(listOf(item(1, "")), 1))
    }

    // 1.5.9 / 177 (vort3) — quale riga colorare come ultimo letto.
    private fun testa(id: Long, root: String) =
        ThreadItem(depth = 0, article = art(id), rootKey = root, isRoot = true)

    @Test
    fun `ultimo letto - la sua riga, se c'e'`() {
        val righe = listOf(testa(1, "<m1@x>"), item(2, "<m1@x>"))
        val mappa = mapOf("<m1@x>" to "<m1@x>", "<m2@x>" to "<m1@x>")
        assertEquals(2L, rigaUltimoLetto(righe, listOf(art(1), art(2)), mappa, 2L))
    }

    @Test
    fun `ultimo letto - discussione chiusa, la testa senza aprirla`() {
        // Ci si e' spostati con ▶ nella discussione 3, che nell'elenco e' chiusa.
        val righe = listOf(testa(1, "<m1@x>"), testa(3, "<m3@x>"))
        val mappa = mapOf("<m1@x>" to "<m1@x>", "<m3@x>" to "<m3@x>", "<m4@x>" to "<m3@x>")
        val articoli = listOf(art(1), art(3), art(4))
        assertEquals(3L, rigaUltimoLetto(righe, articoli, mappa, 4L))
    }

    @Test
    fun `ultimo letto - niente da colorare se manca`() {
        val righe = listOf(testa(1, "<m1@x>"))
        val mappa = mapOf("<m1@x>" to "<m1@x>")
        assertEquals(null, rigaUltimoLetto(righe, listOf(art(1)), mappa, null))
        // nascosto, filtrato o cancellato: niente, non una riga a caso
        assertEquals(null, rigaUltimoLetto(righe, listOf(art(1)), mappa, 99L))
        // vista sequenziale: nessuna riga e' una testa
        assertEquals(null, rigaUltimoLetto(listOf(item(1, "<m1@x>")), listOf(art(1), art(2)),
            mapOf("<m2@x>" to "<m1@x>"), 2L))
    }
    // ------------------------------------------------------------------
    // 1.4.11 / 156 — la riga `CAPABILITIES:` del log non taglia piu' in
    // silenzio. Il banco completo (22 asserzioni, coi dati misurati nei log di
    // Marco) e' in controlli/capabilities/prova_capabilities.sh; qui restano
    // le tre che contano, perche' girino anche sotto Gradle.
    // ------------------------------------------------------------------

    @Test
    fun `una lista corta di capability resta intera`() {
        val caps = listOf("VERSION 2", "READER", "STARTTLS", "XPAT")
        assertEquals("VERSION 2, READER, STARTTLS, XPAT", capabilitiesPerIlLog(caps))
    }

    @Test
    fun `il nome che decide non si perde dietro una lista di argomenti`() {
        // ⚠️ LA REGRESSIONE DEL 12 OTTOBRE 2026, in forma eseguibile. Con il
        // vecchio `.take(300)` STARTTLS cadeva oltre il taglio su
        // news.tcpreset.net, e si era concluso che quel server non lo offrisse.
        // Lo offre: risponde 382.
        val caps = listOf(
            "VERSION 2",
            "LIST " + (1..12).joinToString(" ") { "ARGOMENTO-LUNGO-NUMERO-$it" },
            "SASL " + (1..8).joinToString(" ") { "MECCANISMO-NUMERO-$it" },
            "STARTTLS",
        )
        assertTrue(caps.joinToString(", ").length > 300)
        assertFalse(caps.joinToString(", ").take(300).contains("STARTTLS"))

        val riga = capabilitiesPerIlLog(caps)
        assertTrue(riga.length <= 300)
        assertTrue(riga.contains("STARTTLS"))
        assertTrue(riga.contains("VERSION 2"))
        assertTrue(riga.contains("LIST"))
        assertTrue(riga.contains("SASL"))
    }

    @Test
    fun `se anche i soli nomi sforano il taglio viene dichiarato`() {
        val caps = (1..80).map { "CAPABILITY-NUMERO-$it" }
        val riga = capabilitiesPerIlLog(caps)
        assertTrue(riga.contains("TRONCATA"))
        assertTrue(riga.contains("80 capability"))
    }

    // ------------------------------------------------------------------
    // 1.5 passo 2 — l'oggetto «citazione» e il suo formattatore. Il banco
    // completo (21 asserzioni) e' in controlli/citazioni/prova_citazioni.sh;
    // qui restano quelle che fissano la FORMA e la separazione fra oggetto e
    // testo, perche' girino anche sotto Gradle.
    // ------------------------------------------------------------------

    @Test
    fun `la citazione ha la forma decisa nel documento di progetto`() {
        val intro = "In it.test, il 14/09/2026 alle 10:27, Tizio ha scritto in «oggetto»:"
        val c = citazioneDa(intro, listOf("testo scelto", "ancora testo"), "id@server")
        assertEquals(
            intro + "\n> testo scelto\n> ancora testo\n" +
                "[<id@server> — https://usenet.id/id@server]",
            formattaCitazione(c),
        )
    }

    @Test
    fun `l oggetto citazione porta i dati e non il testo gia fatto`() {
        // ⚠️ E' LA REGOLA DEL PASSO 2: «la B deve produrre un OGGETTO, non una
        // stringa». Se un giorno qualcuno facesse costruire la stringa alla
        // cattura, il raccoglitore dovrebbe smontarla — ed e' il modo in cui
        // in questo progetto nasce la sesta copia di una logica.
        val c = citazioneDa("Intro:", listOf("a", "b"), "<id@server>")
        assertEquals(listOf("a", "b"), c.righe)
        assertEquals("<id@server>", c.messageId)
        assertEquals("https://usenet.id/id@server", c.link)
    }

    @Test
    fun `un pezzo che manca accorcia il blocco e non lo buca`() {
        assertEquals("> a\n[<id@x>]", formattaCitazione(Citazione("", listOf("a"), "<id@x>")))
        assertEquals("Intro:\n> a", formattaCitazione(Citazione("Intro:", listOf("a"))))
        assertEquals("", formattaCitazione(Citazione("", emptyList())))
    }

    @Test
    fun `piu citazioni in fila sono separate da una riga vuota`() {
        val a = citazioneDa("A:", listOf("uno"))
        val b = citazioneDa("B:", listOf("due"))
        assertEquals("A:\n> uno\n\nB:\n> due", formattaCitazioni(listOf(a, b)))
        // una citazione vuota in mezzo non lascia un buco
        assertEquals(
            formattaCitazioni(listOf(a, b)),
            formattaCitazioni(listOf(a, Citazione("", emptyList()), b)),
        )
    }

    @Test
    fun `il prefisso di citazione si comporta come ha sempre fatto`() {
        assertEquals(listOf("> ciao"), righeCitate(listOf("ciao")))
        // riga gia' citata: «> >x», non «>>x». Comportamento storico di NeReO,
        // estratto da buildPrefill senza cambiarlo.
        assertEquals(listOf("> >x"), righeCitate(listOf(">x")))
        assertEquals(listOf("> "), righeCitate(listOf("")))
    }

    // 1.5 / 158 — la selezione dice QUALI righe; si citano le ORIGINALI.
    private val corpoCitabile = listOf(
        "Ciao a tutti,", "> Tizio ha scritto:", ">> vecchio livello",
        "> risposta di Tizio", "testo nuovo dell'autore ", "che continua qui",
    ).joinToString("\n")

    @Test
    fun `selezione in stile barre ritrova i livelli originali`() {
        assertEquals(
            listOf(">> vecchio livello", "> risposta di Tizio"),
            righeOriginaliDi(corpoCitabile, "vecchio livello\nrisposta di Tizio"),
        )
        // a meta' riga: si allarga alla riga intera
        assertEquals(
            listOf(">> vecchio livello", "> risposta di Tizio"),
            righeOriginaliDi(corpoCitabile, "livello\nrisposta di"),
        )
    }

    @Test
    fun `selezione in stile numeri e su capoverso flowed`() {
        assertEquals(listOf(">> vecchio livello"), righeOriginaliDi(corpoCitabile, "2 vecchio livello"))
        assertEquals(
            listOf("testo nuovo dell'autore ", "che continua qui"),
            righeOriginaliDi(corpoCitabile, "testo nuovo dell'autore che continua qui"),
        )
    }

    @Test
    fun `righe che l'originale non ha si saltano, e se manca tutto si cita il visto`() {
        assertEquals(
            listOf("> Tizio ha scritto:", ">> vecchio livello", "> risposta di Tizio"),
            righeOriginaliDi(corpoCitabile, "Tizio ha scritto:\n[nascosta]\nrisposta di Tizio"),
        )
        assertEquals(listOf("xyz", "abc"), righeOriginaliDi(corpoCitabile, "xyz\nabc"))
        assertEquals(emptyList<String>(), righeOriginaliDi(corpoCitabile, "  \n"))
    }

    // 1.5 / 159 (piano D) — collaudo di Marco: il testo citato dentro una
    // citazione appartiene a chi l'ha scritto, non all'autore del messaggio.
    @Test
    fun `citare dentro una citazione ri-radica sull'attribuzione interna`() {
        val corpo = listOf(
            "On 12/10, Giangi wrote:",
            ">>>>> Il 05/10/2026 14:30, Giangi ha scritto:",
            ">>>>>> 8>< ----",
            ">>>>>> Il costo puro del materiale",
            ">>>>>> 500€.",
        ).joinToString("\n")
        val r = riradica(corpo, "Il costo puro del materiale\n500€.")
        assertEquals("Il 05/10/2026 14:30, Giangi ha scritto:", r.introduzione)
        assertEquals(listOf("Il costo puro del materiale", "500€."), r.righe)
        // testo dell'autore del messaggio: introduzione invariata
        assertEquals(null, riradica(corpo, "Giangi wrote").introduzione)
    }

    // 1.5 / 160 — il post VERO di Roberto: il taglio «8>< ----» sta al livello
    // 4, PIU' BASSO del blocco, fra l'attribuzione e il testo. Si scavalca.
    @Test
    fun `il taglio a un livello piu' basso non ferma la ricerca dell'attribuzione`() {
        val corpo = listOf(
            ">>>>> Il 05/10/2026 14:30, Giangi ha scritto:",
            ">>>>",
            ">>>> 8>< ----",
            ">>>>",
            ">>>>>> Il costo puro del materiale",
        ).joinToString("\n")
        val r = riradica(corpo, "Il costo puro del materiale")
        assertEquals("Il 05/10/2026 14:30, Giangi ha scritto:", r.introduzione)
        assertEquals(listOf("Il costo puro del materiale"), r.righe)
    }


    // 1.5.1 / 163 — §A12-retro meta' B: la propagazione segue l'azione.
    @Test
    fun `la propagazione alle risposte segue il segno della regola`() {
        assertEquals(EffettoPropagazione(-100, nascondi = true, segui = false), effettoPropagazioneDi(-9999, null))
        assertEquals(EffettoPropagazione(9999, nascondi = false, segui = true), effettoPropagazioneDi(9999, null))
        assertEquals(EffettoPropagazione(100, nascondi = false, segui = false), effettoPropagazioneDi(100, null))
        // un followupScore scritto a mano vince, e un negativo nasconde come prima
        assertEquals(-30, effettoPropagazioneDi(-9999, -30).punteggio)
        assertTrue(effettoPropagazioneDi(-9999, -30).nascondi)
    }

}

/**
 * 1.5.8 — la striscia «Aggiornato a <versione> — Novita'» (piano B1 di Marco)
 * e il confronto di versioni spostato da UpdateChecker.isNewer.
 */
class StrisciaAggiornatoTest {

    @Test
    fun `confronto numerico, non testuale`() {
        assertTrue(versionePiuNuova("1.4.10", "1.4.9"))
        assertTrue(versionePiuNuova("1.5", "1.4.11"))
        assertTrue(versionePiuNuova("1.5.8", "1.5.7"))
        assertFalse(versionePiuNuova("1.5.7", "1.5.8"))
        assertFalse(versionePiuNuova("1.5.7", "1.5.7"))
        assertFalse(versionePiuNuova("1.2", "1.2.0"))
        assertFalse(versionePiuNuova(" 1.2.0 ", "1.2"))
    }

    @Test
    fun `mai alla prima installazione`() {
        assertFalse(deveMostrareAggiornato("1.5.8", null, primaInstallazione = true))
        assertFalse(deveMostrareAggiornato("1.5.8", "1.5.7", primaInstallazione = true))
    }

    @Test
    fun `chi aggiorna dalla 1_5_7 non ha la chiave e la vede`() {
        assertTrue(deveMostrareAggiornato("1.5.8", null, primaInstallazione = false))
        assertTrue(deveMostrareAggiornato("1.5.8", "", primaInstallazione = false))
    }

    @Test
    fun `una volta sola per versione`() {
        assertTrue(deveMostrareAggiornato("1.5.9", "1.5.8", primaInstallazione = false))
        assertFalse(deveMostrareAggiornato("1.5.8", "1.5.8", primaInstallazione = false))
    }

    @Test
    fun `tornare a una versione vecchia non la fa comparire`() {
        assertFalse(deveMostrareAggiornato("1.5.7", "1.5.8", primaInstallazione = false))
    }
}

/**
 * 1.6 — il segnalibro del sync non supera l'ultimo numero ricevuto
 * nell'ultimo lotto (Bingo3331 + HAL, articolo 3282 su Astraweb).
 */
class SegnalibroSyncTest {

    @Test
    fun `il caso di Bingo - overview vuota subito dopo l'invio, il segnalibro resta`() {
        assertEquals(3281L, segnalibroDopoLotto(3281, 3282, 3282, emptyList(), ultimoLotto = true))
    }

    @Test
    fun `overview completa - il segnalibro va alla fine come prima`() {
        assertEquals(3282L, segnalibroDopoLotto(3281, 3282, 3282, listOf(3282L), ultimoLotto = true))
        assertEquals(3290L, segnalibroDopoLotto(3279, 3280, 3290, listOf(3280L, 3285L, 3290L), ultimoLotto = true))
    }

    @Test
    fun `coda mancante - il segnalibro si ferma all'ultimo ricevuto`() {
        assertEquals(3285L, segnalibroDopoLotto(3279, 3280, 3290, listOf(3280L, 3283L, 3285L), ultimoLotto = true))
    }

    @Test
    fun `lotto intermedio - i buchi sono normali, va alla fine del lotto`() {
        assertEquals(19999L, segnalibroDopoLotto(0, 10000, 19999, emptyList(), ultimoLotto = false))
        assertEquals(19999L, segnalibroDopoLotto(0, 10000, 19999, listOf(10005L), ultimoLotto = false))
    }

    @Test
    fun `ultimo lotto dopo altri lotti - vuoto, resta alla fine del lotto precedente`() {
        // il segnalibro letto all'inizio e' vecchio: conta la fine del lotto prima
        assertEquals(19999L, segnalibroDopoLotto(0, 20000, 20005, emptyList(), ultimoLotto = true))
    }

    @Test
    fun `numeri scavalcati dal limite di testate non si richiedono`() {
        // segnalibro 0, limite 300 su high 10000: si parte da 9701
        assertEquals(9700L, segnalibroDopoLotto(0, 9701, 10000, emptyList(), ultimoLotto = true))
    }

    @Test
    fun `numeri fuori dall'intervallo chiesto non spostano il segnalibro`() {
        assertEquals(3281L, segnalibroDopoLotto(3281, 3282, 3282, listOf(9999L, 5L), ultimoLotto = true))
    }

    @Test
    fun `il segnalibro non torna mai indietro`() {
        assertEquals(3300L, segnalibroDopoLotto(3300, 3282, 3290, listOf(3285L), ultimoLotto = true))
    }
}

/**
 * 1.5.9 / 177 — faccine a caratteri mostrate come emoji (text/Faccine.kt).
 * Si prova solo DOVE sono le faccine: il testo non cambia mai.
 */
class FaccineTest {
    private fun sequenze(riga: String, escluse: List<IntRange> = emptyList()) =
        trovaFaccine(riga, escluse).map { it.sequenza }

    @Test
    fun `l'esempio di Marco - due faccine stupite`() {
        val f = trovaFaccine("8-o 8-o")
        assertEquals(listOf("8-o", "8-o"), f.map { it.sequenza })
        assertEquals(listOf(0 until 3, 4 until 7), f.map { it.inizio until it.fine })
        assertEquals(String(Character.toChars(0x1F632)), f[0].emoji)
    }

    @Test
    fun `riga di sola faccina`() {
        assertEquals(listOf(":-)"), sequenze(":-)"))
        assertEquals(listOf(":)"), sequenze("   :)   "))
    }

    @Test
    fun `isolata da spazi o punteggiatura finale`() {
        assertEquals(listOf(":-)"), sequenze("ciao :-) a tutti"))
        assertEquals(listOf(";-)"), sequenze("lo sai bene ;-)."))
        assertEquals(listOf(":-D"), sequenze("davvero :-D!"))
        assertEquals(listOf(":-("), sequenze("peccato :-(, pazienza"))
    }

    @Test
    fun `in mezzo a una parola resta testo`() {
        assertTrue(sequenze("a:-)b").isEmpty())
        assertTrue(sequenze("ciao:-)").isEmpty())
        assertTrue(sequenze(":-)ciao").isEmpty())
        assertTrue(sequenze(":-).x").isEmpty())  // punteggiatura non finale
    }

    // 1.5.9 / 178 — collaudo di Marco sulla 177: «"8-o 8-o"» restava testo.
    @Test
    fun `fra virgolette e parentesi si converte`() {
        assertEquals(listOf("8-o", "8-o"), sequenze("\"8-o 8-o\""))
        assertEquals(listOf("8-o"), sequenze("\"8-o\""))
        assertEquals(listOf("8-o"), sequenze("«8-o»"))
        assertEquals(listOf("8-o"), sequenze("(8-o)"))
        assertEquals(listOf("8-o"), sequenze("'8-o'"))
        assertEquals(listOf("8-o"), sequenze("“8-o”"))
        assertEquals(listOf("8-o"), sequenze("[8-o]"))
        assertEquals(listOf("8-o"), sequenze("disse \"8-o\". E poi"))
        assertEquals(listOf("8-o"), sequenze("> \"8-o\""))
        assertEquals(listOf("8-o"), sequenze(">\"8-o\""))
    }

    @Test
    fun `virgolette attaccate a una parola restano testo`() {
        assertTrue(sequenze("a8-ob").isEmpty())
        assertTrue(sequenze("l'8-o").isEmpty())
        assertTrue(sequenze("x\"8-o\"").isEmpty())
        assertTrue(sequenze("a(8-o)").isEmpty())
        assertTrue(sequenze("\"8-o\"x").isEmpty())
        assertTrue(sequenze("vai su http://example.com").isEmpty())
        assertTrue(sequenze("vinto \"8-0\"").isEmpty())
    }

    // 1.5.9 / 179 — come MesNews: l'enfasi Usenet intorno alla faccina.
    @Test
    fun `fra asterischi e trattini bassi si converte`() {
        assertEquals(listOf(";-)"), sequenze("*;-)*"))
        assertEquals(listOf(":-)"), sequenze("_:-)_"))
        assertEquals(listOf(":-)", ";-)"), sequenze("\":-)\" *;-)*"))
    }

    @Test
    fun `asterischi e trattini bassi attaccati a una parola restano testo`() {
        assertTrue(sequenze("a*:-)").isEmpty())
        assertTrue(sequenze("x_8-o").isEmpty())
        assertTrue(sequenze("*:-)*x").isEmpty())
    }

    @Test
    fun `faccina in fondo a una parentesi - piano A1`() {
        // `:-))` si legge `:-)` piu' una parentesi: e' lo stesso caso di
        // «(ciao :-))», che deve convertirsi.
        assertEquals(listOf(":-)"), sequenze(":-))"))
        assertEquals(listOf(":-)"), sequenze("(ciao :-))"))
    }

    @Test
    fun `numeri e punteggi restano testo`() {
        assertTrue(sequenze("28-o").isEmpty())
        assertTrue(sequenze("vinto 8-0 in casa").isEmpty())
        assertTrue(sequenze("ore 8-10").isEmpty())
    }

    @Test
    fun `URL, Message-ID e percorsi restano testo`() {
        assertTrue(sequenze("http://example.com/:-)").isEmpty())
        assertTrue(sequenze("<abc:-)@example.com>").isEmpty())
        assertTrue(sequenze("<:-)@example.com>").isEmpty())
        assertTrue(sequenze("guarda http://x.it/a :/").isEmpty()) // :/ esclusa
        assertTrue(sequenze("C:\\dati").isEmpty())
        // una faccina isolata DENTRO un intervallo escluso (un URL che il
        // chiamante ha riconosciuto) resta testo; fuori si converte
        val riga = "www.x.it/?q= :-) :-)"
        assertEquals(listOf(":-)"), sequenze(riga, listOf(0..15)))
    }

    @Test
    fun `testo citato`() {
        assertEquals(listOf(":-)"), sequenze("> :-)"))
        assertEquals(listOf(":-)"), sequenze(">:-)"))
        assertEquals(listOf("8-O"), sequenze(">> > 8-O"))
        assertEquals(listOf(":-P"), sequenze(">> scherzo :-P"))
        assertTrue(sequenze("a>:-)").isEmpty())
    }

    @Test
    fun `varianti con apostrofo e trattino`() {
        assertEquals(listOf(":'(", ":'-("), sequenze(":'( :'-("))
        assertEquals(listOf(":-|", ":-*"), sequenze(":-| :-*"))
    }

    @Test
    fun `tabella base senza le escluse`() {
        for (vietata in listOf(":/", ":-/", ":\\", "XD", "<3", ">:-(", "^_^", "o_O", "8-0"))
            assertFalse(vietata, vietata in TABELLA_FACCINE)
        // nessuna sequenza e' l'inizio di un'altra: il primo riscontro e' quello giusto
        for (a in TABELLA_FACCINE.keys) for (b in TABELLA_FACCINE.keys)
            if (a != b) assertFalse("$a / $b", b.startsWith(a))
        // tutte di Unicode <= 7.0 (Android 9): nessun code point oltre U+1F64F
        for (e in TABELLA_FACCINE.values) assertTrue(e.codePointAt(0) in 0x1F600..0x1F64F)
    }
}

/** 1.6 — Ricostruisci discussione: quali id cercare, e la ricerca contro un server finto. */
class RicostruisciDiscussioneTest {
    @Test
    fun `solo gli id assenti, i piu' vicini per primi, senza ripetizioni`() {
        val righe = listOf("<a@x> <b@x> <c@x>", "<a@x> <b@x>")
        assertEquals(listOf("<c@x>", "<a@x>"), idDaCercare(righe, presenti = setOf("<b@x>")))
    }

    @Test
    fun `gli id gia' cercati non si cercano due volte`() {
        assertEquals(listOf("<a@x>"), idDaCercare(listOf("<a@x> <b@x>"), emptySet(), giaCercati = setOf("<b@x>")))
    }

    @Test
    fun `discussione intera - niente da cercare`() {
        assertTrue(idDaCercare(listOf("<a@x> <b@x>", ""), setOf("<a@x>", "<b@x>")).isEmpty())
    }

    @Test
    fun `gli id nominati sono letti con la funzione unica, tetto compreso`() {
        val lunga = (1..500).joinToString(" ") { "<$it@x>" }
        assertEquals(Threader.idNelleReferences(lunga), idNominati(listOf(lunga)))
        assertTrue(idNominati(listOf(lunga)).size <= 100)
    }

    @Test
    fun `Xref - il numero del gruppo giusto`() {
        val x = "news.example.org it.test:3282 it.altro:17"
        assertEquals(3282L, numeroDaXref(x, "it.test"))
        assertEquals(17L, numeroDaXref(x, "it.altro"))
    }

    @Test
    fun `Xref - gruppo assente, intestazione assente`() {
        assertNull(numeroDaXref("news.example.org it.altro:17", "it.test"))
        assertNull(numeroDaXref(null, "it.test"))
        assertNull(numeroDaXref("", "it.test"))
    }

    @Test
    fun `Xref - il nome del server non e' una posizione`() {
        assertNull(numeroDaXref("it.test:5", "it.test"))
    }

    @Test
    fun `Xref - un locatore non numerico o zero non vale`() {
        assertNull(numeroDaXref("s it.test:abc", "it.test"))
        assertNull(numeroDaXref("s it.test:0", "it.test"))
    }

    @Test
    fun `Xref - gruppo col prefisso uguale non si confonde`() {
        assertNull(numeroDaXref("s it.test.altro:9", "it.test"))
    }

    @Test
    fun `dalle intestazioni alla riga che usa la sync`() {
        val righe = listOf(
            "Subject: =?UTF-8?Q?perch=C3=A9?=", "From: Marco <m@x.it>", "Date: Mon, 2 Nov 2026 10:00:00 +0100",
            "References: <a@x>", " <b@x>", "Lines: 12", "Xref: s it.test:5",
        ).map { it.toByteArray() }
        val o = overviewDaIntestazioni(intestazioniDaTesta(righe), "<c@x>", 5)
        assertEquals("perché", o.subject)
        assertEquals("<a@x> <b@x>", o.references)
        assertEquals("<c@x>", o.messageId)
        assertEquals(12, o.lines)
        assertEquals(5L, o.number)
    }

    @Test
    fun `un Subject a 8 bit passa dalla decodifica della sync`() {
        val righe = listOf("Subject: citt\u00e0".toByteArray(Charsets.UTF_8))
        assertEquals("città", overviewDaIntestazioni(intestazioniDaTesta(righe), "<c@x>", 0).subject)
    }

    /** Un server NNTP in una riga: risponde a ogni comando con [risposte]. */
    private fun conServerFinto(risposte: (String) -> List<String>, prova: (NntpClient) -> Unit) {
        val presa = java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
        val filo = Thread {
            runCatching {
                presa.accept().use { s ->
                    val w = s.getOutputStream().bufferedWriter(Charsets.ISO_8859_1)
                    val r = s.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                    w.write("200 finto\r\n"); w.flush()
                    while (true) {
                        val cmd = r.readLine() ?: break
                        for (riga in risposte(cmd)) w.write(riga + "\r\n")
                        w.flush()
                        if (cmd.startsWith("QUIT")) break
                    }
                }
            }
        }
        filo.isDaemon = true
        filo.start()
        try {
            NntpClient("127.0.0.1", presa.localPort).use { c -> c.connect(); prova(c) }
        } finally {
            presa.close()
        }
    }

    private fun serverBase(cmd: String): List<String> = when {
        cmd.startsWith("CAPABILITIES") -> listOf("101 elenco", "VERSION 2", "READER", ".")
        cmd.startsWith("HEAD <sparito@x>") -> listOf("430 No article with that message-id")
        cmd.startsWith("HEAD <zero@x>") ->
            listOf("221 0 <zero@x>", "Subject: s", "Xref: srv it.test:42", ".")
        cmd.startsWith("HEAD <numero@x>") -> listOf("221 7 <numero@x>", "Subject: s", ".")
        cmd.startsWith("HEAD <xreffalsa@x>") ->
            listOf("221 0 <xreffalsa@x>", "Subject: s", "Xref: srv it.test:43", ".")
        cmd.startsWith("HEAD <altrove@x>") ->
            listOf("221 0 <altrove@x>", "Subject: s", "Xref: srv it.altro:1", ".")
        cmd.startsWith("STAT 42") -> listOf("223 42 <zero@x>")
        cmd.startsWith("STAT 43") -> listOf("223 43 <un-altro@x>")
        cmd.startsWith("HEAD <rotto@x>") -> listOf("403 guasto del server")
        else -> listOf("500 sconosciuto")
    }

    private fun numero(e: EsitoRicerca) = (e as EsitoRicerca.Trovato).overview.number

    @Test
    fun `430 - non disponibile, e la connessione resta buona`() = conServerFinto(::serverBase) { c ->
        assertEquals(EsitoRicerca.NonDisponibile, cercaPerMessageId(c, "<sparito@x>", "it.test"))
        assertEquals(7L, numero(cercaPerMessageId(c, "<numero@x>", "it.test")))
    }

    @Test
    fun `il numero dichiarato dal server vince`() = conServerFinto(::serverBase) { c ->
        assertEquals(7L, numero(cercaPerMessageId(c, "<numero@x>", "it.test")))
    }

    @Test
    fun `numero zero - si prende da Xref, verificato con STAT`() = conServerFinto(::serverBase) { c ->
        assertEquals(42L, numero(cercaPerMessageId(c, "<zero@x>", "it.test")))
    }

    @Test
    fun `una Xref che porta a un altro articolo non si crede`() = conServerFinto(::serverBase) { c ->
        assertEquals(0L, numero(cercaPerMessageId(c, "<xreffalsa@x>", "it.test")))
    }

    @Test
    fun `articolo di un altro gruppo - numero 0, lo decide chi chiama`() = conServerFinto(::serverBase) { c ->
        assertEquals(0L, numero(cercaPerMessageId(c, "<altrove@x>", "it.test")))
    }

    @Test
    fun `senza gruppo non si chiede nessun numero`() = conServerFinto(::serverBase) { c ->
        assertEquals(0L, numero(cercaPerMessageId(c, "<zero@x>")))
    }

    @Test
    fun `un guasto del server non e' un 430 - resta un errore`() = conServerFinto(::serverBase) { c ->
        val e = runCatching { cercaPerMessageId(c, "<rotto@x>", "it.test") }.exceptionOrNull()
        assertEquals(403, (e as NntpException).code)
    }
}

/** 1.6 — Ignora discussione: chi e' colpito, e chi resta comunque non letto. */
class IgnoraDiscussioneTest {
    private fun art(id: String, refs: String = "", data: Long = 1) = PropagationInput(id, refs, data)

    @Test
    fun `registro vuoto - non cambia niente`() {
        assertTrue(colpitiDaIgnorate(listOf(art("<b@x>", "<a@x>")), emptySet()).isEmpty())
    }

    @Test
    fun `una risposta nella discussione ignorata e' colpita, le altre no`() {
        val c = colpitiDaIgnorate(
            listOf(art("<b@x>", "<a@x>"), art("<c@x>", "<d@x>"), art("<e@x>")), setOf("<a@x>"),
        )
        assertEquals(setOf("<b@x>"), c)
    }

    @Test
    fun `basta un antenato qualunque`() {
        assertEquals(setOf("<c@x>"), colpitiDaIgnorate(listOf(art("<c@x>", "<a@x> <b@x>")), setOf("<b@x>")))
    }

    @Test
    fun `References potate - la catena nello stesso lotto regge`() {
        // c nomina solo b, che non e' nel registro ma arriva nello stesso lotto
        val lotto = listOf(art("<c@x>", "<b@x>", 2), art("<b@x>", "<a@x>", 1))
        assertEquals(setOf("<b@x>", "<c@x>"), colpitiDaIgnorate(lotto, setOf("<a@x>")))
    }

    @Test
    fun `una copia gia' nel registro, da un altro gruppo o server, e' colpita`() {
        assertEquals(setOf("<a@x>"), colpitiDaIgnorate(listOf(art("<a@x>")), setOf("<a@x>")))
    }

    @Test
    fun `una risposta a me resta non letta`() {
        assertFalse(siSegnaLetto(rispostaAMe = true, mio = false, seguito = false))
    }

    @Test
    fun `un mio post resta non letto`() {
        assertFalse(siSegnaLetto(rispostaAMe = false, mio = true, seguito = false))
    }

    @Test
    fun `un messaggio seguito resta non letto`() {
        assertFalse(siSegnaLetto(rispostaAMe = false, mio = false, seguito = true))
    }

    @Test
    fun `tutto il resto si segna letto`() {
        assertTrue(siSegnaLetto(rispostaAMe = false, mio = false, seguito = false))
    }

    @Test
    fun `il registro segue la discussione anche attraverso un mio post`() {
        val c = colpitiDaIgnorate(
            listOf(art("<mio@x>", "<a@x>", 1), art("<dopo@x>", "<a@x> <mio@x>", 2)), setOf("<a@x>"),
        )
        assertEquals(setOf("<mio@x>", "<dopo@x>"), c)
    }
}

/** 1.6 (Dave Royal) — identita' per gruppo: gruppo, poi server, poi predefinita. */
class IdentitaPerGruppoTest {
    private val identita = mapOf(1L to "gruppo", 2L to "server")
    private fun scelta(g: Long?, s: Long?, predefinita: String? = "predefinita") =
        primaIdentita(g, s, trova = { identita[it] }, predefinita = { predefinita })

    @Test
    fun `il gruppo vince sul server`() = assertEquals("gruppo", scelta(1, 2))

    @Test
    fun `senza gruppo si eredita dal server`() = assertEquals("server", scelta(null, 2))

    @Test
    fun `senza gruppo e senza server, la predefinita`() = assertEquals("predefinita", scelta(null, null))

    @Test
    fun `identita' del gruppo cancellata - ricade sul server`() = assertEquals("server", scelta(99, 2))

    @Test
    fun `identita' cancellate tutte e due - ricade sulla predefinita`() =
        assertEquals("predefinita", scelta(99, 98))

    @Test
    fun `nessuna identita' - null, e al vuoto di cortesia ci pensa il Repository`() =
        assertNull(scelta(null, null, predefinita = null))

    @Test
    fun `la riga in scrittura compare con piu' identita' e il mittente giusto`() {
        assertEquals("MCSM", etichettaIdentita("MCSM", "Marco", "m@x.it", 2, " Marco ", "M@X.it"))
    }

    @Test
    fun `una sola identita' - niente riga`() {
        assertNull(etichettaIdentita("MCSM", "Marco", "m@x.it", 1, "Marco", "m@x.it"))
    }

    @Test
    fun `mittente cambiato a mano - la riga non dice il falso`() {
        assertNull(etichettaIdentita("MCSM", "Marco", "m@x.it", 2, "Altro", "m@x.it"))
        assertNull(etichettaIdentita("MCSM", "Marco", "m@x.it", 2, "Marco", "altro@x.it"))
        assertNull(etichettaIdentita("", "Marco", "m@x.it", 2, "Marco", "m@x.it"))
    }
}

/**
 * 1.6 — «Riformatta» in scrittura. Regola di prudenza: nel dubbio non si unisce.
 * Le ultime prove sono le invarianti: stesse parole, seconda passata identica,
 * giro completo con Format=Flowed.
 */
class RiformattaCitazioneTest {
    /** Una citazione «a pettine»: righe piene e code di una parola. */
    private val pettine = """
        > Questa e' una riga che arrivava quasi al margine delle settantadue
        > colonne
        > e che dopo la citazione e' stata spezzata lasciando una coda corta
        > corta
        > e poi il discorso prosegue ancora per un bel pezzo su questa riga
        > finale
    """.trimIndent()

    private fun parole(s: String) =
        s.lines().flatMap { it.split(' ') }.map { it.trimStart('>') }.filter { it.isNotEmpty() }

    @Test
    fun `il pettine si ricompone entro le 72 colonne`() {
        val r = riformattaCitazioni(pettine).lines()
        assertTrue(r.size < pettine.lines().size)
        assertTrue(r.all { it.length <= 72 && it.startsWith("> ") })
        assertTrue(r.dropLast(1).all { it.length > 50 })
    }

    @Test
    fun `una riga citata troppo lunga si spezza conservando il livello`() {
        val r = riformattaCitazioni("> > " + "parola ".repeat(20).trimEnd()).lines()
        assertTrue(r.size > 1)
        assertTrue(r.all { it.startsWith("> > ") && it.length <= 72 })
    }

    @Test
    fun `testo nostro, firma e righe vuote non si toccano`() {
        val testo = "Tizio ha scritto:\n$pettine\n\nIl mio testo resta qui,\nsu due righe.\n\n-- \nMarco\n> finta citazione nella firma\n> su due righe"
        val r = riformattaCitazioni(testo)
        assertTrue(r.startsWith("Tizio ha scritto:\n> "))
        assertTrue(r.endsWith("\n\nIl mio testo resta qui,\nsu due righe.\n\n-- \nMarco\n> finta citazione nella firma\n> su due righe"))
    }

    @Test
    fun `una riga non citata lunga non si tocca`() {
        val lunga = "parola ".repeat(20).trimEnd()
        assertEquals(lunga, riformattaCitazioni(lunga))
    }

    @Test
    fun `la riga vuota citata separa i capoversi`() {
        val t = "> primo capoverso lungo abbastanza da sembrare testo impaginato davvero\n> \n> secondo capoverso lungo abbastanza da sembrare testo impaginato pure"
        assertEquals(t, riformattaCitazioni(t))
        val s = t.replace("\n> \n", "\n>\n")
        assertEquals(s, riformattaCitazioni(s))
    }

    @Test
    fun `livelli diversi non si uniscono mai`() {
        val t = "> > riga di secondo livello lunga abbastanza da sembrare impaginata\n> riga di primo livello lunga abbastanza da sembrare impaginata"
        assertEquals(t, riformattaCitazioni(t))
    }

    @Test
    fun `l'attribuzione resta sulla sua riga`() {
        val t = "> Il 03/11/2026 alle 10:00, Sempronio ha scritto:\n> > riga citata lunga abbastanza da sembrare impaginata per bene"
        assertEquals(t, riformattaCitazioni(t))
        // senza i due punti, ma introduce un livello piu' profondo
        val s = "> On Monday Sempronio wrote\n> > riga citata lunga abbastanza da sembrare impaginata per bene"
        assertEquals(s, riformattaCitazioni(s))
    }

    @Test
    fun `said in mezzo a una frase non ferma il capoverso`() {
        val r = riformattaCitazioni(
            "> and then he said that the whole thing should be done again from the\n> start\n> because nobody had really understood what had been asked at first"
        ).lines()
        // la coda «start» si e' riunita alla riga dopo: «said» non ha fermato niente
        assertTrue(r[1].startsWith("> start because"))
    }

    @Test
    fun `i segni di taglio restano dove sono`() {
        val t = "> prima parte del discorso lunga abbastanza da sembrare impaginata\n> [...]\n> seconda parte del discorso lunga abbastanza da sembrare impaginata\n> 8<-------\n> terza parte del discorso lunga abbastanza da sembrare impaginata"
        assertEquals(t, riformattaCitazioni(t))
    }

    @Test
    fun `la firma citata non si tocca`() {
        val t = "> testo citato lungo abbastanza da sembrare impaginato per bene\n> -- \n> Tizio Caio\n> via Roma 1, una firma lunga che non deve essere ricomposta mai"
        assertEquals(t, riformattaCitazioni(t))
    }

    @Test
    fun `elenchi e rientri restano com'erano`() {
        val t = "> ecco cosa serve per fare la prova come si deve in tutti i casi:\n> - la prima cosa da fare\n> - la seconda cosa da fare\n> 1. numerata\n>    rientrata"
        assertEquals(t, riformattaCitazioni(t))
    }

    @Test
    fun `saluti e frasi brevi con punteggiatura restano separati`() {
        val t = "> Ciao,\n> questa e' una riga lunga abbastanza da sembrare impaginata davvero"
        assertEquals(t, riformattaCitazioni(t))
    }

    @Test
    fun `i versi a righe corte restano com'erano`() {
        val t = "> Nel mezzo del cammin\n> di nostra vita\n> mi ritrovai\n> per una selva"
        assertEquals(t, riformattaCitazioni(t))
    }

    @Test
    fun `un URL lungo non si tocca e non si unisce`() {
        val url = "https://example.org/" + "a".repeat(80)
        val t = "> vedi questo indirizzo qui sotto che spiega bene tutta la faccenda\n> $url\n> e poi il discorso prosegue con altre parole fino a riempire la riga"
        assertEquals(t, riformattaCitazioni(t))
    }

    @Test
    fun `prefisso profondissimo - niente crash e niente parole perse`() {
        val t = ">".repeat(60) + " " + "parola ".repeat(12).trimEnd()
        assertEquals(parole(t), parole(riformattaCitazioni(t)))
    }

    @Test
    fun `una faccina a inizio riga non e' testo da unire`() {
        val t = "> una riga citata lunga abbastanza da sembrare impaginata per bene\n>:-)\n> altra riga citata lunga abbastanza da sembrare impaginata"
        assertEquals(t, riformattaCitazioni(t))
    }

    @Test
    fun `i doppi spazi dentro la riga sopravvivono`() {
        val t = "> una riga con  doppio spazio lunga abbastanza da sembrare impaginata\n> coda"
        assertTrue(riformattaCitazioni(t).contains("con  doppio"))
    }

    @Test
    fun `gli spazi in coda di un messaggio flowed non restano`() {
        val t = "> riga che finiva con uno spazio perche' il messaggio era flowed \n> e continua qui sotto fino a riempire la riga di parole"
        assertTrue(riformattaCitazioni(t).lines().none { it.endsWith(" ") })
    }

    @Test
    fun `testo vuoto e senza citazioni restano uguali`() {
        assertEquals("", riformattaCitazioni(""))
        assertEquals("ciao\n\nmondo", riformattaCitazioni("ciao\n\nmondo"))
    }

    @Test
    fun `invariante - le parole restano le stesse`() {
        assertEquals(parole(pettine), parole(riformattaCitazioni(pettine)))
    }

    @Test
    fun `invariante - seconda passata identica alla prima`() {
        val casi = listOf(
            pettine,
            "> > " + "parola ".repeat(30).trimEnd(),
            "$pettine\n> \n> > altra riga di secondo livello lunga abbastanza da sembrare testo\n> > coda.\n> > seguito",
            "> Fine frase.\n> " + "parola ".repeat(10).trimEnd() + "\n> altro.\n> " + "testo ".repeat(14).trimEnd(),
        )
        for (c in casi) {
            val uno = riformattaCitazioni(c)
            assertEquals(uno, riformattaCitazioni(uno))
        }
    }

    @Test
    fun `giro completo con Format=Flowed - righe entro 72 e livelli intatti`() {
        val t = "> > " + "parola ".repeat(30).trimEnd() + "\n> " + "testo ".repeat(30).trimEnd()
        val riformattato = riformattaCitazioni(t)
        val inviato = avvolgiFlowed(riformattato).lines()
        assertEquals(riformattato.lines().size, inviato.size)   // niente di rispezzato
        assertTrue(inviato.all { it.length <= 72 })
        assertTrue(inviato.none { it.endsWith(" ") })           // righe fisse
        assertEquals(riformattato.lines().map { it.count { c -> c == '>' } }, inviato.map { it.count { c -> c == '>' } })
    }

    @Test
    fun `il corpo da inviare passa sempre dall'a capo`() {
        val lunga = "parola ".repeat(20).trimEnd()
        assertTrue(corpoDaInviare(lunga, flowed = false).lines().all { it.length <= 72 })
        assertTrue(corpoDaInviare(lunga, flowed = true).lines().all { it.length <= 72 })
        assertTrue(almenoUnaRigaContinua(corpoDaInviare(lunga, flowed = true).lines()))
        assertFalse(almenoUnaRigaContinua(corpoDaInviare(lunga, flowed = false).lines()))
    }
}

/**
 * 1.6 / 180 — il gruppo nella barra di lettura (RPoggi): quale server nominare
 * (regola della 1.4.4, ora in un posto solo) e il segno `+N` dei crosspost.
 */
class TitoloGruppoTest {
    private val es = Server(id = 1, name = "Eternal-September", host = "news.eternal-september.org")
    private val aw = Server(id = 2, name = "", host = "news.astraweb.com")

    @Test
    fun `un server solo - nessuna etichetta`() {
        assertEquals("", serverDaMostrare(listOf(es), 1))
        assertEquals("", serverDaMostrare(emptyList(), 1))
    }

    @Test
    fun `piu' server - il nome, o l'host se il nome e' vuoto`() {
        assertEquals("Eternal-September", serverDaMostrare(listOf(es, aw), 1))
        assertEquals("news.astraweb.com", serverDaMostrare(listOf(es, aw), 2))
    }

    @Test
    fun `gruppo orfano o senza server - nessuna etichetta`() {
        assertEquals("", serverDaMostrare(listOf(es, aw), 99))
        assertEquals("", serverDaMostrare(listOf(es, aw), null))
    }

    @Test
    fun `crosspost - si contano solo gli altri gruppi`() {
        assertEquals(2, altriGruppiCrosspost(listOf("it.test", "it.uno", "it.due"), "it.test"))
        assertEquals(0, altriGruppiCrosspost(listOf("it.test"), "it.test"))
        assertEquals(0, altriGruppiCrosspost(emptyList(), "it.test"))
    }

    @Test
    fun `crosspost - doppioni, maiuscole e spazi non contano due volte`() {
        assertEquals(1, altriGruppiCrosspost(listOf("it.test", " it.uno", "IT.UNO", "", "It.Test"), "it.test"))
    }

    @Test
    fun `crosspost - dalla riga Newsgroups vera`() {
        val h = "From: a@b.c\nNewsgroups: it.test, it.uno,it.due\nSubject: x"
        assertEquals(2, altriGruppiCrosspost(crosspostGroups(h), "it.uno"))
        // corpo non ancora scaricato: headersRaw null, nessun segno
        assertEquals(0, altriGruppiCrosspost(crosspostGroups(null), "it.uno"))
    }
}
