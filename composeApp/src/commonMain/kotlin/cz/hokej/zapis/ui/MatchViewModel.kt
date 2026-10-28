package cz.hokej.zapis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.hokej.zapis.cast.NoopPublisher
import cz.hokej.zapis.cast.ScoreboardPublisher
import cz.hokej.zapis.cast.toSnapshot
import cz.hokej.zapis.logic.MatchController
import cz.hokej.zapis.model.MatchConfig
import cz.hokej.zapis.model.Team
import cz.hokej.zapis.model.TeamColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import cz.hokej.zapis.history.InMemoryTextStore
import cz.hokej.zapis.history.MatchHistory
import cz.hokej.zapis.history.MatchRecord
import cz.hokej.zapis.history.toRecord
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class Screen { SETUP, MATCH, SUMMARY, HISTORY, HISTORY_DETAIL }

class MatchViewModel(
    private val publisher: ScoreboardPublisher = NoopPublisher,
    val history: MatchHistory = MatchHistory(InMemoryTextStore()),
) : ViewModel() {
    private val _screen = MutableStateFlow(Screen.SETUP)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    /** Poslední nastavení se předvyplní pro další zápas. */
    var lastConfig = MatchConfig(
        home = Team("Domácí", TeamColors[0]),
        away = Team("Hosté", TeamColors[1]),
        periodLengthSec = 10 * 60,
    )
        private set

    var controller: MatchController? = null
        private set

    private var jobs: List<Job> = emptyList()
    private var matchId = ""
    private var startedAtMs = 0L

    /** Zápas otevřený ze seznamu. */
    var selectedRecord: MatchRecord? = null
        private set

    fun startMatch(config: MatchConfig) {
        lastConfig = config
        jobs.forEach { it.cancel() }
        val c = MatchController(config)
        controller = c
        startedAtMs = nowMs()
        matchId = startedAtMs.toString()
        jobs = listOf(
            viewModelScope.launch {
                while (true) {
                    c.tick()
                    delay(100)
                }
            },
            viewModelScope.launch {
                c.state.collectLatest { publisher.publish(it.toSnapshot()) }
            },
            // Ukládat průběžně (start, góly, konec), ať se zápas neztratí ani při pádu appky.
            viewModelScope.launch {
                c.state.map { it.goals to it.phase }.distinctUntilChanged().collect { saveCurrent() }
            },
        )
        _screen.value = Screen.MATCH
    }

    fun finishMatch() {
        controller?.finish()
        saveCurrent()
        _screen.value = Screen.SUMMARY
    }

    fun openHistory() {
        _screen.value = Screen.HISTORY
    }

    fun openRecord(record: MatchRecord) {
        selectedRecord = record
        _screen.value = Screen.HISTORY_DETAIL
    }

    fun deleteRecord(record: MatchRecord) = history.delete(record.id)

    fun closeHistory() {
        _screen.value = if (_screen.value == Screen.HISTORY_DETAIL) Screen.HISTORY else Screen.SETUP
    }

    private fun saveCurrent() {
        val c = controller ?: return
        history.save(c.state.value.toRecord(matchId, startedAtMs))
    }

    fun newMatch() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        controller = null
        _screen.value = Screen.SETUP
    }
}

@OptIn(ExperimentalTime::class)
private fun nowMs() = Clock.System.now().toEpochMilliseconds()
