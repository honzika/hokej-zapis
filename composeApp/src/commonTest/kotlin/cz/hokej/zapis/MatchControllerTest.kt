package cz.hokej.zapis

import cz.hokej.zapis.cast.toJson
import cz.hokej.zapis.cast.toSnapshot
import cz.hokej.zapis.logic.MatchController
import cz.hokej.zapis.model.MatchConfig
import cz.hokej.zapis.model.Phase
import cz.hokej.zapis.model.Side
import cz.hokej.zapis.model.Team
import cz.hokej.zapis.model.formatClock
import cz.hokej.zapis.model.GoalMoment
import cz.hokej.zapis.model.parseClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class MatchControllerTest {
    private val time = TestTimeSource()
    private val config = MatchConfig(Team("Lvi", 0xFFD32F2F), Team("Vlci", 0xFF1565C0), periodLengthSec = 60)
    private val c = MatchController(config, time, clockTime = { "20:15" })

    @Test
    fun clockCountsDownOnlyWhileRunning() {
        c.start()
        time += 10.seconds
        c.tick()
        assertEquals(50_000, c.state.value.remainingMs)
        c.pause()
        time += 30.seconds
        c.tick()
        assertEquals(50_000, c.state.value.remainingMs)
        c.start()
        time += 5.seconds
        c.tick()
        assertEquals(45_000, c.state.value.remainingMs)
    }

    @Test
    fun periodEndsAndNextStarts() {
        c.start()
        time += 61.seconds
        c.tick()
        assertEquals(Phase.PERIOD_END, c.state.value.phase)
        assertEquals(0, c.state.value.remainingMs)
        c.nextPeriod()
        assertEquals(2, c.state.value.period)
        assertEquals(60_000, c.state.value.remainingMs)
        assertEquals(Phase.READY, c.state.value.phase)
    }

    @Test
    fun noFourthPeriod() {
        repeat(3) {
            c.start(); time += 60.seconds; c.tick(); c.nextPeriod()
        }
        assertEquals(3, c.state.value.period)
        assertEquals(Phase.PERIOD_END, c.state.value.phase)
        c.finish()
        assertEquals(Phase.FINISHED, c.state.value.phase)
    }

    @Test
    fun goalsRecordTimeAndScorer() {
        c.start()
        time += 12.seconds
        c.addGoal(Side.HOME, " 17 Novák ")
        c.addGoal(Side.AWAY, "")
        val s = c.state.value
        assertEquals(1, s.score(Side.HOME))
        assertEquals(1, s.score(Side.AWAY))
        assertEquals(12, s.goals[0].atSec)
        assertEquals("17 Novák", s.goals[0].scorer)
        assertNull(s.goals[1].scorer)
        c.removeGoal(s.goals[0].id)
        assertEquals(0, c.state.value.score(Side.HOME))
    }

    @Test
    fun goalKeepsMomentFromButtonPress() {
        c.start()
        time += 12.seconds
        val at = c.moment()
        time += 8.seconds // vyplňování střelce
        c.addGoal(Side.HOME, "9", at)
        assertEquals(12, c.state.value.goals.single().atSec)
        assertEquals("20:15", c.state.value.goals.single().clockTime)
    }

    @Test
    fun manualGoalTimeIsClampedToPeriod() {
        c.addGoal(Side.AWAY, null, GoalMoment(1, 500))
        assertEquals(60, c.state.value.goals.single().atSec)
    }

    @Test
    fun parsesManualClock() {
        assertEquals(83, parseClock("1:23"))
        assertEquals(83, parseClock("123"))
        assertEquals(45, parseClock("45"))
        assertEquals(5, parseClock("5"))
        assertEquals(600, parseClock("10.00"))
        assertNull(parseClock("1:75"))
        assertNull(parseClock("abc"))
        assertNull(parseClock(""))
    }

    @Test
    fun adjustClampsAndReopensEndedPeriod() {
        c.adjust(+30)
        assertEquals(60_000, c.state.value.remainingMs)
        c.start(); time += 60.seconds; c.tick()
        assertEquals(Phase.PERIOD_END, c.state.value.phase)
        c.adjust(+10)
        assertEquals(Phase.PAUSED, c.state.value.phase)
        assertEquals(10_000, c.state.value.remainingMs)
        c.adjust(-20)
        assertEquals(Phase.PERIOD_END, c.state.value.phase)
    }

    @Test
    fun adjustWhileRunningKeepsCounting() {
        c.start(); time += 20.seconds
        c.adjust(+10)
        time += 5.seconds; c.tick()
        assertEquals(45_000, c.state.value.remainingMs)
    }

    @Test
    fun clockFormatRoundsUp() {
        assertEquals("10:00", formatClock(600_000))
        assertEquals("0:01", formatClock(1))
        assertEquals("0:00", formatClock(0))
        assertEquals("1:05", formatClock(64_100))
    }

    @Test
    fun snapshotJson() {
        c.addGoal(Side.HOME, "9")
        val json = c.state.value.toSnapshot().toJson()
        assertTrue("\"color\":\"#d32f2f\"" in json, json)
        assertTrue("\"score\":1" in json, json)
        assertTrue("Lvi · 9 · 1. tř. 0:00" in json, json)
    }
}
