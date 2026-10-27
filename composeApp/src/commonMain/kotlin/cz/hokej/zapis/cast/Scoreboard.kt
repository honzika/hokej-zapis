package cz.hokej.zapis.cast

import cz.hokej.zapis.model.MatchState
import cz.hokej.zapis.model.Phase
import cz.hokej.zapis.model.Side
import cz.hokej.zapis.model.formatSec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Data, která se posílají na TV receiver (receiver/index.html). */
@Serializable
data class ScoreboardSnapshot(
    val home: TeamSnapshot,
    val away: TeamSnapshot,
    val period: Int,
    val periods: Int,
    val remainingMs: Long,
    val running: Boolean,
    val phase: String,
    val lastGoal: String? = null,
)

@Serializable
data class TeamSnapshot(val name: String, val color: String, val score: Int)

fun MatchState.toSnapshot(): ScoreboardSnapshot {
    fun team(side: Side) = config.team(side).let {
        TeamSnapshot(it.name, it.color.toCssColor(), score(side))
    }
    val last = goals.lastOrNull()?.let { g ->
        listOfNotNull(config.team(g.side).name, g.scorer, "${g.period}. tř. ${formatSec(g.atSec)}")
            .joinToString(" · ")
    }
    return ScoreboardSnapshot(
        home = team(Side.HOME),
        away = team(Side.AWAY),
        period = period,
        periods = config.periods,
        remainingMs = remainingMs,
        running = phase == Phase.RUNNING,
        phase = phase.name,
        lastGoal = last,
    )
}

private fun Long.toCssColor() = "#" + (this and 0xFFFFFF).toString(16).padStart(6, '0')

val ScoreboardJson = Json { encodeDefaults = true }

fun ScoreboardSnapshot.toJson(): String = ScoreboardJson.encodeToString(ScoreboardSnapshot.serializer(), this)

/** Platformní implementace (Google Cast) přijde ve fázi 2. */
interface ScoreboardPublisher {
    fun publish(snapshot: ScoreboardSnapshot)
}

object NoopPublisher : ScoreboardPublisher {
    override fun publish(snapshot: ScoreboardSnapshot) = Unit
}
