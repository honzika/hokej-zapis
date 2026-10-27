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
import kotlinx.coroutines.launch

enum class Screen { SETUP, MATCH, SUMMARY }

class MatchViewModel(
    private val publisher: ScoreboardPublisher = NoopPublisher,
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

    fun startMatch(config: MatchConfig) {
        lastConfig = config
        jobs.forEach { it.cancel() }
        val c = MatchController(config)
        controller = c
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
        )
        _screen.value = Screen.MATCH
    }

    fun finishMatch() {
        controller?.finish()
        _screen.value = Screen.SUMMARY
    }

    fun newMatch() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        controller = null
        _screen.value = Screen.SETUP
    }
}
