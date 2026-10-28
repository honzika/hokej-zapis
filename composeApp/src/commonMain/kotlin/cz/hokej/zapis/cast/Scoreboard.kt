package cz.hokej.zapis.cast

import cz.hokej.zapis.model.MatchState
import cz.hokej.zapis.model.Phase
import cz.hokej.zapis.model.Side
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val CAST_NAMESPACE = "urn:x-cast:cz.hokej.zapis"

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
    val part: String = "třetina",
    val partGen: String = "třetiny",
    val countUp: Boolean = false,
    val periodLengthMs: Long = 0,
)

@Serializable
data class TeamSnapshot(val name: String, val color: String, val score: Int, val logo: String? = null)

fun MatchState.toSnapshot(): ScoreboardSnapshot {
    fun team(side: Side) = config.team(side).let {
        TeamSnapshot(it.name, it.color.toCssColor(), score(side), it.logo?.dataUrl)
    }
    val last = goals.lastOrNull()?.let { g ->
        listOfNotNull(config.team(g.side).name, g.scorer, config.goalTime(g.period, g.atSec))
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
        part = config.sport.part,
        partGen = config.sport.partGen,
        countUp = config.sport.countUp,
        periodLengthMs = config.periodLengthSec * 1000L,
    )
}

private fun Long.toCssColor() = "#" + (this and 0xFFFFFF).toString(16).padStart(6, '0')

val ScoreboardJson = Json { encodeDefaults = true }

fun ScoreboardSnapshot.toJson(): String = ScoreboardJson.encodeToString(ScoreboardSnapshot.serializer(), this)

/** Platformní implementace (Google Cast) přijde ve fázi 2. */
fun interface ScoreboardPublisher {
    fun publish(snapshot: ScoreboardSnapshot)
}

object NoopPublisher : ScoreboardPublisher {
    override fun publish(snapshot: ScoreboardSnapshot) = Unit
}
