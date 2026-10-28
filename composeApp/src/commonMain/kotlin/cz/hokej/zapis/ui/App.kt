package cz.hokej.zapis.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.hokej.zapis.cast.NoopPublisher
import cz.hokej.zapis.cast.ScoreboardPublisher
import cz.hokej.zapis.history.InMemoryTextStore
import cz.hokej.zapis.history.MatchHistory
import cz.hokej.zapis.history.formatDateTime
import cz.hokej.zapis.history.toState

private val IceScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00222F),
    background = Color(0xFF0B1621),
    surface = Color(0xFF0B1621),
    surfaceVariant = Color(0xFF16263A),
    surfaceContainerHigh = Color(0xFF1B2D44),
)

@Composable
fun App(
    publisher: ScoreboardPublisher = NoopPublisher,
    history: MatchHistory = MatchHistory(InMemoryTextStore()),
    vm: MatchViewModel = viewModel { MatchViewModel(publisher, history) },
) {
    MaterialTheme(colorScheme = IceScheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val screen by vm.screen.collectAsState()
            val controller = vm.controller
            when {
                screen == Screen.MATCH && controller != null -> {
                    KeepScreenOn()
                    MatchScreen(controller, onFinish = vm::finishMatch)
                }
                screen == Screen.SUMMARY && controller != null -> {
                    PlatformBackHandler(vm::newMatch)
                    SummaryScreen(controller.state.collectAsState().value, onNewMatch = vm::newMatch)
                }
                screen == Screen.HISTORY -> {
                    PlatformBackHandler(vm::closeHistory)
                    HistoryScreen(
                        matches = vm.history.matches.collectAsState().value,
                        onOpen = vm::openRecord,
                        onDelete = vm::deleteRecord,
                        onBack = vm::closeHistory,
                    )
                }
                screen == Screen.HISTORY_DETAIL && vm.selectedRecord != null -> {
                    val record = vm.selectedRecord!!
                    PlatformBackHandler(vm::closeHistory)
                    SummaryScreen(
                        state = record.toState(),
                        title = formatDateTime(record.startedAtMs),
                        actionLabel = "Zpět na zápasy",
                        onNewMatch = vm::closeHistory,
                    )
                }
                else -> SetupScreen(vm.lastConfig, onStart = vm::startMatch, onHistory = vm::openHistory)
            }
        }
    }
}

/** Během zápasu nenechat zhasnout displej. */
@Composable
expect fun KeepScreenOn()

/** Tlačítko pro výběr TV (Google Cast). */
@Composable
expect fun CastButton()

/** Systémové „zpět“ (Android); na iOS nic nedělá. */
@Composable
expect fun PlatformBackHandler(onBack: () -> Unit)
