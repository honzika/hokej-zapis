package cz.hokej.zapis

import cz.hokej.zapis.cast.toJson
import cz.hokej.zapis.cast.toSnapshot
import cz.hokej.zapis.history.InMemoryTextStore
import cz.hokej.zapis.history.MatchHistory
import cz.hokej.zapis.history.toRecord
import cz.hokej.zapis.history.toState
import cz.hokej.zapis.logic.MatchController
import cz.hokej.zapis.model.MatchConfig
import cz.hokej.zapis.model.Side
import cz.hokej.zapis.model.Sport
import cz.hokej.zapis.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class FootballTest {
    private val time = TestTimeSource()
    private val config = MatchConfig(Team("Sokol", 0xFF2E7D32), Team("Slavoj", 0xFFF9A825), 45 * 60, Sport.FOOTBALL)
    private val c = MatchController(config, time)

    @Test
    fun twoHalvesAndClockCountsUp() {
        assertEquals(2, config.periods)
        assertEquals("0:00", c.state.value.clockText)
        c.start(); time += 12.minutes + 30.seconds; c.tick()
        assertEquals("12:30", c.state.value.clockText)
        time += 33.minutes; c.tick()
        assertEquals("45:00", c.state.value.clockText)
        c.nextPeriod()
        assertEquals("45:00", c.state.value.clockText) // 2. poločas začíná na 45:00
        c.start(); time += 8.minutes; c.tick()
        assertEquals("53:00", c.state.value.clockText)
        c.addGoal(Side.HOME, "Hráč")
        assertEquals("54'", config.goalTime(2, c.state.value.goals.single().atSec))
    }

    @Test
    fun goalMinuteFormat() {
        assertEquals("1'", config.goalTime(1, 0))
        assertEquals("23'", config.goalTime(1, 22 * 60 + 10))
        assertEquals("46'", config.goalTime(2, 5))
        val hockey = config.copy(sport = Sport.HOCKEY, periods = 3, periodLengthSec = 600)
        assertEquals("2. tř. 3:15", hockey.goalTime(2, 195))
    }

    @Test
    fun sportSurvivesHistoryAndGoesToTv() {
        val history = MatchHistory(InMemoryTextStore())
        history.save(c.state.value.toRecord("f", 1))
        assertEquals(Sport.FOOTBALL, history.matches.value.single().toState().config.sport)
        val json = c.state.value.toSnapshot().toJson()
        assertTrue("\"countUp\":true" in json && "\"part\":\"poločas\"" in json, json)
    }
}
