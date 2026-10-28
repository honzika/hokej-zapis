package cz.hokej.zapis.logic

import cz.hokej.zapis.model.GoalEvent
import cz.hokej.zapis.model.GoalMoment
import cz.hokej.zapis.model.MatchConfig
import cz.hokej.zapis.model.MatchState
import cz.hokej.zapis.model.Phase
import cz.hokej.zapis.model.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Stav zápasu a časomíra. Zbývající čas se počítá z monotónních hodin
 * od posledního startu, takže nezáleží na tom, jak často se volá [tick].
 */
class MatchController(
    config: MatchConfig,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val clockTime: () -> String = ::localClockTime,
) {
    private val _state = MutableStateFlow(MatchState(config))
    val state: StateFlow<MatchState> = _state.asStateFlow()

    private var runningSince: TimeMark? = null
    private var remainingAtStart = 0L
    private var nextGoalId = 1

    private val periodMs get() = _state.value.config.periodLengthSec * 1000L

    fun start() {
        val s = _state.value
        if (s.phase != Phase.READY && s.phase != Phase.PAUSED) return
        if (s.remainingMs <= 0) return
        runningSince = timeSource.markNow()
        remainingAtStart = s.remainingMs
        _state.value = s.copy(phase = Phase.RUNNING)
    }

    fun pause() {
        tick()
        val s = _state.value
        if (s.phase != Phase.RUNNING) return
        runningSince = null
        _state.value = s.copy(phase = Phase.PAUSED)
    }

    fun toggle() = if (_state.value.phase == Phase.RUNNING) pause() else start()

    fun tick() {
        val s = _state.value
        val since = runningSince ?: return
        if (s.phase != Phase.RUNNING) return
        val remaining = (remainingAtStart - since.elapsedNow().inWholeMilliseconds).coerceAtLeast(0)
        _state.value = if (remaining == 0L) {
            runningSince = null
            s.copy(remainingMs = 0, phase = Phase.PERIOD_END)
        } else {
            s.copy(remainingMs = remaining)
        }
    }

    /** Ruční korekce času (např. když se zapomene zastavit). */
    fun adjust(deltaSec: Int) {
        tick()
        val s = _state.value
        if (s.phase == Phase.FINISHED) return
        val remaining = (s.remainingMs + deltaSec * 1000L).coerceIn(0, periodMs)
        if (s.phase == Phase.RUNNING) {
            runningSince = timeSource.markNow()
            remainingAtStart = remaining
        }
        val phase = when {
            remaining == 0L -> { runningSince = null; Phase.PERIOD_END }
            s.phase == Phase.PERIOD_END -> Phase.PAUSED
            else -> s.phase
        }
        _state.value = s.copy(remainingMs = remaining, phase = phase)
    }

    fun moment(): GoalMoment {
        tick()
        val s = _state.value
        return GoalMoment(s.period, ((periodMs - s.remainingMs) / 1000).toInt(), clockTime())
    }

    fun addGoal(side: Side, scorer: String?, at: GoalMoment = moment()) {
        val s = _state.value
        if (s.phase == Phase.FINISHED) return
        val atSec = at.atSec.coerceIn(0, s.config.periodLengthSec)
        val goal = GoalEvent(nextGoalId++, side, at.period, atSec, scorer?.trim()?.ifEmpty { null }, at.clockTime)
        _state.value = s.copy(goals = s.goals + goal)
    }

    fun removeGoal(id: Int) {
        val s = _state.value
        _state.value = s.copy(goals = s.goals.filterNot { it.id == id })
    }

    fun nextPeriod() {
        val s = _state.value
        if (s.phase != Phase.PERIOD_END || s.isLastPeriod) return
        _state.value = s.copy(period = s.period + 1, remainingMs = periodMs, phase = Phase.READY)
    }

    fun finish() {
        tick()
        runningSince = null
        _state.value = _state.value.copy(phase = Phase.FINISHED)
    }
}

@OptIn(ExperimentalTime::class)
fun localClockTime(): String {
    val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${t.hour}:${t.minute.toString().padStart(2, '0')}"
}
