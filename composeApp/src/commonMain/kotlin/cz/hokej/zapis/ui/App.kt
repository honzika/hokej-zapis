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

private val IceScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00222F),
    background = Color(0xFF0B1621),
    surface = Color(0xFF0B1621),
    surfaceVariant = Color(0xFF16263A),
    surfaceContainerHigh = Color(0xFF1B2D44),
)

@Composable
fun App(vm: MatchViewModel = viewModel { MatchViewModel() }) {
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
                else -> SetupScreen(vm.lastConfig, onStart = vm::startMatch)
            }
        }
    }
}

/** Během zápasu nenechat zhasnout displej. */
@Composable
expect fun KeepScreenOn()

/** Systémové „zpět“ (Android); na iOS nic nedělá. */
@Composable
expect fun PlatformBackHandler(onBack: () -> Unit)
