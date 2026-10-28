package cz.hokej.zapis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import cz.hokej.zapis.logic.MatchController
import cz.hokej.zapis.model.GoalEvent
import cz.hokej.zapis.model.GoalMoment
import cz.hokej.zapis.model.MatchState
import cz.hokej.zapis.model.Phase
import cz.hokej.zapis.model.Side
import cz.hokej.zapis.model.formatClock
import cz.hokej.zapis.model.formatSec

@Composable
fun MatchScreen(controller: MatchController, onFinish: () -> Unit) {
    val state by controller.state.collectAsState()
    var goalFor by remember { mutableStateOf<Pair<Side, GoalMoment>?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<GoalEvent?>(null) }
    PlatformBackHandler { confirmFinish = true }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${state.period}. třetina z ${state.config.periods}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            CastButton()
            IconButton(onClick = { confirmFinish = true }) { Icon(Icons.Default.Flag, "Ukončit zápas") }
        }

        val onGoal = { side: Side -> goalFor = side to controller.moment() }
        BoxWithConstraints(Modifier.weight(1f)) {
            if (maxWidth > maxHeight) {
                // Na šířku: týmy po stranách, čas a události uprostřed
                Row(Modifier.fillMaxSize().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TeamPanel(state, Side.HOME, Modifier.weight(1f).fillMaxHeight()) { onGoal(Side.HOME) }
                    Column(Modifier.weight(1.3f)) {
                        Clock(state, controller::toggle, controller::adjust, clockSize = 56.sp)
                        PeriodAction(state, controller::nextPeriod, onFinish)
                        Events(state, { goalToDelete = it }, Modifier.weight(1f))
                    }
                    TeamPanel(state, Side.AWAY, Modifier.weight(1f).fillMaxHeight()) { onGoal(Side.AWAY) }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TeamPanel(state, Side.HOME, Modifier.weight(1f)) { onGoal(Side.HOME) }
                        TeamPanel(state, Side.AWAY, Modifier.weight(1f)) { onGoal(Side.AWAY) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Clock(state, controller::toggle, controller::adjust)
                    PeriodAction(state, controller::nextPeriod, onFinish)
                    Events(state, { goalToDelete = it }, Modifier.weight(1f))
                }
            }
        }
    }

    goalFor?.let { (side, at) ->
        ScorerDialog(
            title = "Gól – ${state.config.team(side).name} (${at.period}. tř. ${formatSec(at.atSec)})",
            onConfirm = { scorer -> controller.addGoal(side, scorer, at); goalFor = null },
            onDismiss = { goalFor = null },
        )
    }
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Smazat gól?") },
            text = { Text(goalLabel(state, goal)) },
            confirmButton = { TextButton(onClick = { controller.removeGoal(goal.id); goalToDelete = null }) { Text("Smazat") } },
            dismissButton = { TextButton(onClick = { goalToDelete = null }) { Text("Zrušit") } },
        )
    }
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Ukončit zápas?") },
            text = { Text("Zápas skončí se skóre ${state.score(Side.HOME)}:${state.score(Side.AWAY)}.") },
            confirmButton = { TextButton(onClick = { confirmFinish = false; onFinish() }) { Text("Ukončit") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Pokračovat") } },
        )
    }
}

@Composable
private fun PeriodAction(state: MatchState, onNextPeriod: () -> Unit, onFinish: () -> Unit) {
    if (state.phase != Phase.PERIOD_END) return
    Button(
        onClick = if (state.isLastPeriod) onFinish else onNextPeriod,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp),
    ) {
        Text(if (state.isLastPeriod) "Konec zápasu" else "Začít ${state.period + 1}. třetinu", fontSize = 17.sp)
    }
}

@Composable
private fun Events(state: MatchState, onDelete: (GoalEvent) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Text(
            "Události",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        if (state.goals.isEmpty()) {
            Text("Zatím žádný gól", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.weight(1f)) {
            items(state.goals.asReversed(), key = { it.id }) { goal ->
                GoalRow(state, goal, onDelete = { onDelete(goal) })
            }
        }
    }
}

@Composable
private fun TeamPanel(state: MatchState, side: Side, modifier: Modifier, onGoal: () -> Unit) {
    val team = state.config.team(side)
    val color = Color(team.color)
    val onColor = if (color.luminance() > 0.5f) Color.Black else Color.White
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.fillMaxWidth().height(8.dp).background(color))
        Text(
            team.name,
            Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            state.score(side).toString(),
            fontSize = 84.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 90.sp,
        )
        Button(
            onClick = onGoal,
            enabled = state.phase != Phase.FINISHED,
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = onColor),
            modifier = Modifier.fillMaxWidth().padding(10.dp).height(52.dp),
        ) { Text("+ Gól", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Clock(state: MatchState, onToggle: () -> Unit, onAdjust: (Int) -> Unit, clockSize: TextUnit = 72.sp) {
    val running = state.phase == Phase.RUNNING
    val canToggle = state.phase == Phase.READY || state.phase == Phase.PAUSED || running
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (running) Color(0xFF12351F) else MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = canToggle, onClick = onToggle)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatClock(state.remainingMs),
                    fontSize = clockSize,
                    fontWeight = FontWeight.Bold,
                    style = TabularNums,
                    color = if (running) Color(0xFF7CFFA4) else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    when (state.phase) {
                        Phase.READY -> "Klepnutím spustíš čas"
                        Phase.RUNNING -> "Běží – klepnutím zastavíš"
                        Phase.PAUSED -> "Zastaveno – klepnutím spustíš"
                        Phase.PERIOD_END -> "Konec třetiny"
                        Phase.FINISHED -> "Konec zápasu"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(-10, -1, +1, +10).forEach { d ->
                FilledTonalButton(onClick = { onAdjust(d) }, enabled = state.phase != Phase.FINISHED) {
                    Text(if (d > 0) "+${d}s" else "${d}s")
                }
            }
        }
    }
}

internal val TabularNums = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun GoalRow(state: MatchState, goal: GoalEvent, onDelete: () -> Unit) {
    val team = state.config.team(goal.side)
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(team.color)))
        Spacer(Modifier.width(10.dp))
        Text(
            "${goal.period}. tř. ${formatSec(goal.atSec)}",
            Modifier.width(100.dp),
            style = TabularNums,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            listOfNotNull(team.name, goal.scorer).joinToString(" – "),
            Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Smazat gól") }
    }
}

private fun goalLabel(state: MatchState, goal: GoalEvent) =
    listOfNotNull(state.config.team(goal.side).name, goal.scorer, "${goal.period}. tř. ${formatSec(goal.atSec)}")
        .joinToString(" · ")

@Composable
private fun ScorerDialog(title: String, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var scorer by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = scorer,
                onValueChange = { scorer = it.take(30) },
                label = { Text("Střelec (číslo nebo jméno)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm(scorer) }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(scorer) }) { Text("Uložit") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Zrušit") }
                TextButton(onClick = { onConfirm(null) }) { Text("Bez střelce") }
            }
        },
    )
}
