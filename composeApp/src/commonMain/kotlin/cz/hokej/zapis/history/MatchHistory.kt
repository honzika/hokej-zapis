package cz.hokej.zapis.history

import cz.hokej.zapis.model.GoalEvent
import cz.hokej.zapis.model.MatchConfig
import cz.hokej.zapis.model.MatchState
import cz.hokej.zapis.model.Phase
import cz.hokej.zapis.model.Side
import cz.hokej.zapis.model.Team
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Uložený zápas. Loga se neukládají (jsou velká a patří k týmu, ne k výsledku). */
@Serializable
data class MatchRecord(
    val id: String,
    val startedAtMs: Long,
    val finished: Boolean,
    val home: TeamRecord,
    val away: TeamRecord,
    val periodLengthSec: Int,
    val periods: Int,
    val goals: List<GoalRecord>,
) {
    fun score(side: Side) = goals.count { it.side == side }
}

@Serializable
data class TeamRecord(val name: String, val color: Long)

@Serializable
data class GoalRecord(
    val side: Side,
    val period: Int,
    val atSec: Int,
    val scorer: String? = null,
    val clockTime: String? = null,
)

fun MatchState.toRecord(id: String, startedAtMs: Long) = MatchRecord(
    id = id,
    startedAtMs = startedAtMs,
    finished = phase == Phase.FINISHED,
    home = TeamRecord(config.home.name, config.home.color),
    away = TeamRecord(config.away.name, config.away.color),
    periodLengthSec = config.periodLengthSec,
    periods = config.periods,
    goals = goals.map { GoalRecord(it.side, it.period, it.atSec, it.scorer, it.clockTime) },
)

/** Pro zobrazení detailu stejnou obrazovkou jako souhrn po zápase. */
fun MatchRecord.toState() = MatchState(
    config = MatchConfig(
        home = Team(home.name, home.color),
        away = Team(away.name, away.color),
        periodLengthSec = periodLengthSec,
        periods = periods,
    ),
    period = goals.maxOfOrNull { it.period } ?: 1,
    remainingMs = 0,
    phase = Phase.FINISHED,
    goals = goals.mapIndexed { i, g -> GoalEvent(i + 1, g.side, g.period, g.atSec, g.scorer, g.clockTime) },
)

@OptIn(ExperimentalTime::class)
fun formatDateTime(epochMs: Long): String {
    val t = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${t.dayOfMonth}. ${t.monthNumber}. ${t.year} ${t.hour}:${t.minute.toString().padStart(2, '0')}"
}

/** Úložiště pro jeden textový soubor (platformní implementace). */
interface TextStore {
    fun read(): String?
    fun write(text: String)
}

class InMemoryTextStore(private var text: String? = null) : TextStore {
    override fun read() = text
    override fun write(text: String) {
        this.text = text
    }
}

/** Seznam všech zápasů, nejnovější první. Ukládá se celý při každé změně (jde o pár kB). */
class MatchHistory(private val store: TextStore) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = ListSerializer(MatchRecord.serializer())

    private val _matches = MutableStateFlow(load())
    val matches: StateFlow<List<MatchRecord>> = _matches.asStateFlow()

    fun save(record: MatchRecord) =
        update((_matches.value.filterNot { it.id == record.id } + record).sortedByDescending { it.startedAtMs })

    fun delete(id: String) = update(_matches.value.filterNot { it.id == id })

    private fun update(list: List<MatchRecord>) {
        _matches.value = list
        store.write(json.encodeToString(serializer, list))
    }

    private fun load(): List<MatchRecord> =
        runCatching { store.read()?.let { json.decodeFromString(serializer, it) } }.getOrNull() ?: emptyList()
}
