package cz.hokej.zapis.model

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

enum class Side { HOME, AWAY }

/** Barva je ARGB, aby šla použít v Compose i serializovat pro TV. */
data class Team(val name: String, val color: Long, val logo: Logo? = null)

/** Zmenšený obrázek loga (max ~160 px), aby se vešel do Cast zprávy. */
class Logo(val bytes: ByteArray, val mimeType: String) {
    @OptIn(ExperimentalEncodingApi::class)
    val dataUrl: String by lazy { "data:$mimeType;base64," + Base64.encode(bytes) }
}

/** Pravidla a texty podle sportu (čeština potřebuje i pády). */
enum class Sport(
    val label: String,
    val periods: Int,
    val defaultMinutes: Int,
    val maxMinutes: Int,
    /** Fotbal: čas běží nahoru přes celý zápas (2. poločas od 45:00). Hokej odpočítává třetinu. */
    val countUp: Boolean,
    val part: String,
    val partAcc: String,
    val partGen: String,
    val partShort: String,
    val partsPlural: String,
) {
    HOCKEY("Hokej", 3, 10, 20, false, "třetina", "třetinu", "třetiny", "tř.", "třetiny"),
    FOOTBALL("Fotbal", 2, 45, 45, true, "poločas", "poločas", "poločasu", "pol.", "poločasy"),
}

data class MatchConfig(
    val home: Team,
    val away: Team,
    val periodLengthSec: Int,
    val sport: Sport = Sport.HOCKEY,
    val periods: Int = sport.periods,
) {
    /** Čas gólu: hokej „2. tř. 3:15“, fotbal minuta zápasu „53'“. */
    fun goalTime(period: Int, atSec: Int): String =
        if (sport.countUp) "${(period - 1) * periodLengthSec / 60 + atSec / 60 + 1}'"
        else "$period. ${sport.partShort} ${formatSec(atSec)}"

    fun team(side: Side) = if (side == Side.HOME) home else away
}

data class GoalEvent(
    val id: Int,
    val side: Side,
    val period: Int,
    /** Čas od začátku třetiny, kdy gól padl. */
    val atSec: Int,
    val scorer: String?,
    /** Skutečný čas (HH:mm), kdy byl gól zapsán. */
    val clockTime: String? = null,
)

/** Okamžik ve hře – čas gólu se bere při stisku „+ Gól“, ne až po vyplnění střelce. */
data class GoalMoment(val period: Int, val atSec: Int, val clockTime: String? = null)

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

    /** Co ukazují hodiny – podle sportu odpočet třetiny, nebo čas zápasu nahoru. */
    val clockText: String
        get() = if (config.sport.countUp) {
            val lenMs = config.periodLengthSec * 1000L
            formatSec((((period - 1) * lenMs + lenMs - remainingMs) / 1000).toInt())
        } else formatClock(remainingMs)
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

/**
 * Čas ve třetině zadaný ručně: „1:23“, „1.23“ nebo jen číslice „123“ (= 1:23, poslední dvě jsou sekundy).
 */
fun parseClock(text: String): Int? {
    val t = text.trim()
    if (t.isEmpty()) return null
    val parts = t.split(':', '.', ',')
    val (min, sec) = when {
        parts.size == 2 -> parts[0].ifEmpty { "0" }.toIntOrNull() to parts[1].toIntOrNull()
        parts.size == 1 && t.all { it.isDigit() } -> t.dropLast(2).ifEmpty { "0" }.toIntOrNull() to t.takeLast(2).toIntOrNull()
        else -> return null
    }
    if (min == null || sec == null || sec > 59) return null
    return min * 60 + sec
}

fun formatSec(sec: Int): String = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"
