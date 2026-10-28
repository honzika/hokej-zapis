package cz.hokej.zapis.cast

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Stav se mění každých 100 ms, ale receiver si běžící čas dopočítává sám.
 * Posíláme proto jen skutečné změny (skóre, start/stop, korekce času)
 * a občas synchronizační zprávu, aby se hodiny na TV nerozjely.
 */
class ThrottledPublisher(
    private val delegate: ScoreboardPublisher,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val heartbeat: Duration = 10.seconds,
    private val tolerance: Duration = 500.milliseconds,
) : ScoreboardPublisher {
    private var lastSent: ScoreboardSnapshot? = null
    private var sentAt: TimeMark? = null

    override fun publish(snapshot: ScoreboardSnapshot) {
        val last = lastSent
        val since = sentAt?.elapsedNow()
        if (last != null && since != null &&
            snapshot.copy(remainingMs = 0) == last.copy(remainingMs = 0) &&
            since < heartbeat
        ) {
            val predicted = if (last.running) last.remainingMs - since.inWholeMilliseconds else last.remainingMs
            if (kotlin.math.abs(predicted - snapshot.remainingMs) <= tolerance.inWholeMilliseconds) return
        }
        lastSent = snapshot
        sentAt = timeSource.markNow()
        delegate.publish(snapshot)
    }
}
