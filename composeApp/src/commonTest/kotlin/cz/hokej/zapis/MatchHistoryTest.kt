package cz.hokej.zapis

import cz.hokej.zapis.history.InMemoryTextStore
import cz.hokej.zapis.history.MatchHistory
import cz.hokej.zapis.history.toRecord
import cz.hokej.zapis.history.toState
import cz.hokej.zapis.logic.MatchController
import cz.hokej.zapis.model.MatchConfig
import cz.hokej.zapis.model.Side
import cz.hokej.zapis.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.TestTimeSource

class MatchHistoryTest {
    private val config = MatchConfig(Team("Lvi", 0xFFD32F2F), Team("Vlci", 0xFF1565C0), periodLengthSec = 60)

    @Test
    fun savesUpdatesAndReloads() {
        val store = InMemoryTextStore()
        val history = MatchHistory(store)
        val c = MatchController(config, TestTimeSource(), clockTime = { "20:15" })
        history.save(c.state.value.toRecord("a", 1000))
        c.addGoal(Side.HOME, "17 Novák")
        history.save(c.state.value.toRecord("a", 1000))
        assertEquals(1, history.matches.value.size)
        assertFalse(history.matches.value.single().finished)

        c.finish()
        history.save(c.state.value.toRecord("a", 1000))
        history.save(MatchController(config).state.value.toRecord("b", 2000))

        val reloaded = MatchHistory(store).matches.value
        assertEquals(listOf("b", "a"), reloaded.map { it.id }) // nejnovější první
        val a = reloaded.last()
        assertTrue(a.finished)
        assertEquals(1, a.score(Side.HOME))
        assertEquals("17 Novák", a.goals.single().scorer)
        assertEquals("20:15", a.goals.single().clockTime)
        assertEquals(1, a.toState().score(Side.HOME))
    }

    @Test
    fun deleteAndCorruptFile() {
        val store = InMemoryTextStore("tohle není JSON")
        val history = MatchHistory(store)
        assertTrue(history.matches.value.isEmpty())
        history.save(MatchController(config).state.value.toRecord("x", 1))
        history.delete("x")
        assertTrue(MatchHistory(store).matches.value.isEmpty())
    }
}
