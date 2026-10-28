package cz.hokej.zapis

import cz.hokej.zapis.cast.ScoreboardPublisher
import cz.hokej.zapis.cast.ScoreboardSnapshot
import cz.hokej.zapis.cast.TeamSnapshot
import cz.hokej.zapis.cast.ThrottledPublisher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class ThrottledPublisherTest {
    private val time = TestTimeSource()
    private val sent = mutableListOf<ScoreboardSnapshot>()
    private val p = ThrottledPublisher(ScoreboardPublisher { sent += it }, time)
    private val base = ScoreboardSnapshot(
        home = TeamSnapshot("A", "#ff0000", 0),
        away = TeamSnapshot("B", "#0000ff", 0),
        period = 1, periods = 3, remainingMs = 60_000, running = true, phase = "RUNNING",
    )

    /** Simuluje ticky z hodin po 100 ms. */
    private fun runFor(ms: Long, from: Long): Long {
        var remaining = from
        repeat((ms / 100).toInt()) {
            time += 100.milliseconds
            remaining -= 100
            p.publish(base.copy(remainingMs = remaining))
        }
        return remaining
    }

    @Test
    fun runningClockSendsOnlyHeartbeats() {
        p.publish(base)
        runFor(25_000, 60_000)
        assertEquals(3, sent.size) // start + 2 heartbeaty (10 s, 20 s)
    }

    @Test
    fun scoreChangeIsSentImmediately() {
        p.publish(base)
        time += 100.milliseconds
        p.publish(base.copy(remainingMs = 59_900, home = base.home.copy(score = 1)))
        assertEquals(1, sent.last().home.score)
        assertEquals(2, sent.size)
    }

    @Test
    fun clockAdjustIsSentWhileRunningAndPaused() {
        p.publish(base)
        time += 1.seconds
        p.publish(base.copy(remainingMs = 69_000)) // +10 s za běhu
        assertEquals(2, sent.size)
        val paused = base.copy(remainingMs = 69_000, running = false, phase = "PAUSED")
        p.publish(paused)
        p.publish(paused.copy(remainingMs = 68_000)) // −1 s v pauze
        assertEquals(4, sent.size)
        time += 3.seconds
        p.publish(paused.copy(remainingMs = 68_000))
        assertEquals(4, sent.size)
    }
}
