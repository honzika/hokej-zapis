package cz.hokej.zapis.model

enum class Side { HOME, AWAY }

/** Barva je ARGB, aby šla použít v Compose i serializovat pro TV. */
data class Team(val name: String, val color: Long)

data class MatchConfig(
    val home: Team,
    val away: Team,
    val periodLengthSec: Int,
    val periods: Int = 3,
) {
    fun team(side: Side) = if (side == Side.HOME) home else away
}

data class GoalEvent(
    val id: Int,
    val side: Side,
    val period: Int,
    /** Čas od začátku třetiny, kdy gól padl. */
    val atSec: Int,
    val scorer: String?,
)

/** Okamžik ve hře – čas gólu se bere při stisku „+ Gól“, ne až po vyplnění střelce. */
data class GoalMoment(val period: Int, val atSec: Int)

enum class Phase { READY, RUNNING, PAUSED, PERIOD_END, FINISHED }

data class MatchState(
    val config: MatchConfig,
    val period: Int = 1,
    val remainingMs: Long = config.periodLengthSec * 1000L,
    val phase: Phase = Phase.READY,
    val goals: List<GoalEvent> = emptyList(),
) {
    fun score(side: Side) = goals.count { it.side == side }
    val isLastPeriod get() = period >= config.periods
}

val TeamColors: List<Long> = listOf(
    0xFFD32F2F, // červená
    0xFF1565C0, // modrá
    0xFF2E7D32, // zelená
    0xFFF9A825, // žlutá
    0xFFEF6C00, // oranžová
    0xFF6A1B9A, // fialová
    0xFF212121, // černá
    0xFFECEFF1, // bílá
    0xFF00838F, // tyrkysová
    0xFF6D4C41, // hnědá
)

/** Herní čas se zaokrouhluje nahoru, aby 0:00 bylo vidět až po skutečném konci. */
fun formatClock(remainingMs: Long): String {
    val totalSec = ((remainingMs + 999) / 1000).coerceAtLeast(0)
    return "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}"
}

fun formatSec(sec: Int): String = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"
